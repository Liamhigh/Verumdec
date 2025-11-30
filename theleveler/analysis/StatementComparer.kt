package ai.verum.theleveler.analysis

import ai.verum.theleveler.core.*

/**
 * Compares statements made by the SAME actor to detect contradictions.
 * Speaker-aware comparison ensures we only flag same-person contradictions.
 */
object StatementComparer {

    /**
     * Compare all statements and find contradictions.
     * Only compares statements from the same actor.
     */
    fun compareStatements(statements: List<Statement>): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        
        // Group statements by actor
        val byActor = statements.groupBy { it.actor.normalized }
        
        for ((_, actorStatements) in byActor) {
            if (actorStatements.size < 2) continue
            
            // Compare each pair of statements from same actor
            for (i in actorStatements.indices) {
                for (j in i + 1 until actorStatements.size) {
                    val s1 = actorStatements[i]
                    val s2 = actorStatements[j]
                    
                    val contradiction = detectContradiction(s1, s2)
                    if (contradiction != null) {
                        contradictions.add(contradiction)
                    }
                }
            }
        }
        
        return contradictions
    }

    /**
     * Detect if two statements contradict each other.
     */
    private fun detectContradiction(s1: Statement, s2: Statement): Contradiction? {
        // Check for direct contradiction (lexical opposition)
        if (Similarity.isDirectContradiction(s1.text, s2.text)) {
            return Contradiction(
                actor = s1.actor,
                firstStatement = s1,
                secondStatement = s2,
                type = ContradictionType.DIRECT_CONTRADICTION,
                confidence = calculateConfidence(s1.text, s2.text, ContradictionType.DIRECT_CONTRADICTION),
                explanation = generateExplanation(s1, s2, ContradictionType.DIRECT_CONTRADICTION)
            )
        }
        
        // Check for implicit contradiction (semantic drift)
        if (Similarity.isImplicitContradiction(s1.text, s2.text)) {
            val similarity = Similarity.semanticScore(s1.text, s2.text)
            
            // Only flag if similarity is in the "suspicious" range
            if (similarity >= 0.25 && similarity < 0.45) {
                return Contradiction(
                    actor = s1.actor,
                    firstStatement = s1,
                    secondStatement = s2,
                    type = ContradictionType.IMPLICIT_CONTRADICTION,
                    confidence = calculateConfidence(s1.text, s2.text, ContradictionType.IMPLICIT_CONTRADICTION),
                    explanation = generateExplanation(s1, s2, ContradictionType.IMPLICIT_CONTRADICTION)
                )
            }
        }
        
        // Check for factual inconsistency (numbers, dates, amounts differ)
        if (hasFactualInconsistency(s1.text, s2.text)) {
            return Contradiction(
                actor = s1.actor,
                firstStatement = s1,
                secondStatement = s2,
                type = ContradictionType.FACTUAL_INCONSISTENCY,
                confidence = calculateConfidence(s1.text, s2.text, ContradictionType.FACTUAL_INCONSISTENCY),
                explanation = generateExplanation(s1, s2, ContradictionType.FACTUAL_INCONSISTENCY)
            )
        }
        
        return null
    }

    /**
     * Check for factual inconsistencies (different numbers, amounts, dates).
     */
    private fun hasFactualInconsistency(text1: String, text2: String): Boolean {
        // Extract numbers from both texts
        val numbers1 = extractNumbers(text1)
        val numbers2 = extractNumbers(text2)
        
        // If both have numbers on related topics, check if they differ
        if (numbers1.isNotEmpty() && numbers2.isNotEmpty()) {
            val similarity = Similarity.semanticScore(text1, text2)
            
            // Related texts with different numbers
            if (similarity > 0.4 && numbers1.intersect(numbers2).isEmpty()) {
                return true
            }
        }
        
        return false
    }

    /**
     * Extract numbers from text.
     */
    private fun extractNumbers(text: String): Set<String> {
        val pattern = Regex("\\b\\d+(?:[.,]\\d+)?\\b")
        return pattern.findAll(text).map { it.value }.toSet()
    }

    /**
     * Calculate confidence score for a contradiction.
     */
    private fun calculateConfidence(text1: String, text2: String, type: ContradictionType): Double {
        val baseSimilarity = Similarity.semanticScore(text1, text2)
        
        return when (type) {
            ContradictionType.DIRECT_CONTRADICTION -> {
                // Higher similarity + opposition = higher confidence
                0.6 + (baseSimilarity * 0.4)
            }
            ContradictionType.IMPLICIT_CONTRADICTION -> {
                // Medium confidence for drift
                0.4 + ((0.45 - baseSimilarity) * 0.5)
            }
            ContradictionType.FACTUAL_INCONSISTENCY -> {
                // High confidence for number mismatches
                0.7 + (baseSimilarity * 0.2)
            }
            else -> 0.5
        }
    }

    /**
     * Generate human-readable explanation for contradiction.
     */
    private fun generateExplanation(s1: Statement, s2: Statement, type: ContradictionType): String {
        val actor = s1.actor.rawName
        
        return when (type) {
            ContradictionType.DIRECT_CONTRADICTION ->
                "$actor made directly opposing statements: first stating '${s1.text.take(50)}...' and later '${s2.text.take(50)}...'"
            
            ContradictionType.IMPLICIT_CONTRADICTION ->
                "$actor's statements show semantic drift on the same topic, suggesting inconsistency."
            
            ContradictionType.FACTUAL_INCONSISTENCY ->
                "$actor provided different factual details (numbers, dates, or amounts) in related statements."
            
            ContradictionType.TIMELINE_CONTRADICTION ->
                "$actor's timeline of events is internally inconsistent."
            
            ContradictionType.BEHAVIOURAL_SHIFT ->
                "$actor's communication pattern shifted significantly between statements."
        }
    }
}

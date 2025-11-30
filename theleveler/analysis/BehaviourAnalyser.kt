package ai.verum.theleveler.analysis

import ai.verum.theleveler.core.*

/**
 * Analyses behaviour patterns and detects shifts in communication style.
 * Detects certainty changes, evasion, defensive tone, etc.
 */
object BehaviourAnalyser {

    // Certainty patterns
    private val highCertaintyPatterns = listOf(
        "definitely", "certainly", "absolutely", "always", "never",
        "100%", "guaranteed", "positive", "sure", "certain",
        "undoubtedly", "without doubt", "clearly", "obviously",
        "i know for a fact", "i saw", "i witnessed", "i confirm"
    )

    private val lowCertaintyPatterns = listOf(
        "maybe", "perhaps", "possibly", "might", "could be",
        "i think", "i believe", "i guess", "i assume", "i suppose",
        "not sure", "uncertain", "unsure", "probably", "likely",
        "don't remember", "can't recall", "don't know", "hard to say"
    )

    // Tone patterns
    private val defensivePatterns = listOf(
        "i didn't", "it wasn't me", "i never", "why would i",
        "that's not true", "you're wrong", "i'm not lying",
        "i swear", "i promise", "believe me", "trust me"
    )

    private val aggressivePatterns = listOf(
        "you always", "you never", "it's your fault", "blame yourself",
        "how dare you", "you're the one", "stop accusing", "back off"
    )

    private val evasivePatterns = listOf(
        "i don't recall", "i can't remember", "it's hard to say",
        "i'm not certain", "that was a long time ago", "i'd have to check",
        "let me think", "i need to review", "off the top of my head"
    )

    /**
     * Analyse all statements from all actors and detect behaviour shifts.
     */
    fun analyseStatements(statements: List<Statement>): List<BehaviourShift> {
        val shifts = mutableListOf<BehaviourShift>()
        
        // Group by actor
        val byActor = statements.groupBy { it.actor.normalized }
        
        for ((_, actorStatements) in byActor) {
            if (actorStatements.size < 2) continue
            
            // Compare consecutive statements
            for (i in 0 until actorStatements.size - 1) {
                val current = actorStatements[i]
                val next = actorStatements[i + 1]
                
                val detected = detectBehaviourShifts(current, next)
                shifts.addAll(detected)
            }
        }
        
        return shifts
    }

    /**
     * Detect behaviour shifts between two consecutive statements.
     */
    private fun detectBehaviourShifts(from: Statement, to: Statement): List<BehaviourShift> {
        val shifts = mutableListOf<BehaviourShift>()
        
        // Check for certainty drop
        if (hasCertaintyDrop(from.text, to.text)) {
            shifts.add(BehaviourShift(
                actor = from.actor,
                shiftType = BehaviourShiftType.CERTAINTY_DROP,
                description = "Certainty level dropped significantly between statements. Earlier statement was more definitive.",
                fromStatement = from,
                toStatement = to
            ))
        }
        
        // Check for certainty rise
        if (hasCertaintyRise(from.text, to.text)) {
            shifts.add(BehaviourShift(
                actor = from.actor,
                shiftType = BehaviourShiftType.CERTAINTY_RISE,
                description = "Certainty level increased significantly. Later statement is more definitive than earlier.",
                fromStatement = from,
                toStatement = to
            ))
        }
        
        // Check for defensive tone emergence
        if (hasDefensiveToneShift(from.text, to.text)) {
            shifts.add(BehaviourShift(
                actor = from.actor,
                shiftType = BehaviourShiftType.DEFENSIVE_TONE,
                description = "Communication shifted to a defensive tone, possibly indicating pressure or scrutiny.",
                fromStatement = from,
                toStatement = to
            ))
        }
        
        // Check for aggressive tone emergence
        if (hasAggressiveToneShift(from.text, to.text)) {
            shifts.add(BehaviourShift(
                actor = from.actor,
                shiftType = BehaviourShiftType.AGGRESSIVE_TONE,
                description = "Communication shifted to an aggressive or accusatory tone.",
                fromStatement = from,
                toStatement = to
            ))
        }
        
        // Check for evasive tone emergence
        if (hasEvasiveToneShift(from.text, to.text)) {
            shifts.add(BehaviourShift(
                actor = from.actor,
                shiftType = BehaviourShiftType.EVASIVE_TONE,
                description = "Communication became more evasive, with increased hedging or memory claims.",
                fromStatement = from,
                toStatement = to
            ))
        }
        
        // Check for linguistic drift
        if (hasLinguisticDrift(from.text, to.text)) {
            shifts.add(BehaviourShift(
                actor = from.actor,
                shiftType = BehaviourShiftType.LINGUISTIC_DRIFT,
                description = "Significant change in communication style or vocabulary detected.",
                fromStatement = from,
                toStatement = to
            ))
        }
        
        return shifts
    }

    /**
     * Check for certainty drop.
     */
    private fun hasCertaintyDrop(fromText: String, toText: String): Boolean {
        val fromCertainty = countPatternMatches(fromText, highCertaintyPatterns)
        val toCertainty = countPatternMatches(toText, highCertaintyPatterns)
        val toUncertainty = countPatternMatches(toText, lowCertaintyPatterns)
        
        return fromCertainty > 0 && (toCertainty == 0 || toUncertainty > toCertainty)
    }

    /**
     * Check for certainty rise.
     */
    private fun hasCertaintyRise(fromText: String, toText: String): Boolean {
        val fromUncertainty = countPatternMatches(fromText, lowCertaintyPatterns)
        val toCertainty = countPatternMatches(toText, highCertaintyPatterns)
        
        return fromUncertainty > 0 && toCertainty > 0
    }

    /**
     * Check for defensive tone shift.
     */
    private fun hasDefensiveToneShift(fromText: String, toText: String): Boolean {
        val fromDefensive = countPatternMatches(fromText, defensivePatterns)
        val toDefensive = countPatternMatches(toText, defensivePatterns)
        
        return fromDefensive == 0 && toDefensive >= 2
    }

    /**
     * Check for aggressive tone shift.
     */
    private fun hasAggressiveToneShift(fromText: String, toText: String): Boolean {
        val fromAggressive = countPatternMatches(fromText, aggressivePatterns)
        val toAggressive = countPatternMatches(toText, aggressivePatterns)
        
        return fromAggressive == 0 && toAggressive >= 1
    }

    /**
     * Check for evasive tone shift.
     */
    private fun hasEvasiveToneShift(fromText: String, toText: String): Boolean {
        val fromEvasive = countPatternMatches(fromText, evasivePatterns)
        val toEvasive = countPatternMatches(toText, evasivePatterns)
        
        return fromEvasive == 0 && toEvasive >= 2
    }

    /**
     * Check for linguistic drift using vocabulary analysis.
     */
    private fun hasLinguisticDrift(fromText: String, toText: String): Boolean {
        val similarity = SemanticEmbedder.similarity(fromText, toText)
        
        // Low similarity on related topics = drift
        // Check if they're discussing similar things with very different language
        val fromWords = extractSignificantWords(fromText)
        val toWords = extractSignificantWords(toText)
        
        val overlap = fromWords.intersect(toWords)
        val vocabularyChange = 1.0 - (overlap.size.toDouble() / maxOf(fromWords.size, toWords.size, 1))
        
        return vocabularyChange > 0.7 && similarity < 0.4
    }

    /**
     * Count pattern matches in text.
     */
    private fun countPatternMatches(text: String, patterns: List<String>): Int {
        val lower = text.lowercase()
        return patterns.count { lower.contains(it) }
    }

    /**
     * Extract significant words from text.
     */
    private fun extractSignificantWords(text: String): Set<String> {
        return text.lowercase()
            .split(Regex("[^a-z]+"))
            .filter { it.length > 4 }
            .toSet()
    }
}

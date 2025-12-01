package com.verumdec.core.analysis

import com.verumdec.core.model.Contradiction
import com.verumdec.core.model.ContradictionType
import com.verumdec.core.model.LegalTrigger
import com.verumdec.core.model.Statement
import java.util.UUID
import kotlin.math.abs

/**
 * StatementComparer performs direct comparison between statements to detect contradictions.
 * 
 * This is the core contradiction detection logic that:
 * - Compares statements from the same speaker (direct contradictions)
 * - Compares statements across different documents (cross-document contradictions)
 * - Uses lexical opposition patterns
 * - Detects negation-based contradictions
 * - Identifies semantic contradictions
 */
object StatementComparer {

    // Lexical opposition patterns - pairs of opposing terms
    private val lexicalOppositions = mapOf(
        "agree" to listOf("disagree", "oppose", "reject"),
        "accept" to listOf("refuse", "reject", "decline", "deny"),
        "true" to listOf("false", "untrue", "incorrect"),
        "yes" to listOf("no", "never", "nope"),
        "always" to listOf("never", "rarely", "seldom"),
        "did" to listOf("didn't", "did not", "never"),
        "was" to listOf("wasn't", "was not", "never was"),
        "is" to listOf("isn't", "is not", "not"),
        "have" to listOf("haven't", "have not", "don't have"),
        "will" to listOf("won't", "will not", "refuse to"),
        "can" to listOf("can't", "cannot", "unable"),
        "paid" to listOf("never paid", "didn't pay", "refused to pay"),
        "received" to listOf("never received", "didn't receive", "denied"),
        "sent" to listOf("never sent", "didn't send", "withheld"),
        "promised" to listOf("never promised", "broke promise", "refused"),
        "agreed" to listOf("disagreed", "never agreed", "refused"),
        "signed" to listOf("never signed", "didn't sign", "refused to sign"),
        "said" to listOf("never said", "didn't say", "denies saying"),
        "knew" to listOf("didn't know", "never knew", "was unaware"),
        "told" to listOf("never told", "didn't tell", "withheld"),
        "confirmed" to listOf("denied", "never confirmed", "disputed"),
        "admitted" to listOf("denied", "never admitted", "refused to admit"),
        "complete" to listOf("incomplete", "unfinished", "partial"),
        "full" to listOf("empty", "partial", "incomplete"),
        "all" to listOf("none", "nothing", "zero"),
        "everything" to listOf("nothing", "none"),
        "before" to listOf("after"),
        "early" to listOf("late"),
        "first" to listOf("last"),
        "more" to listOf("less", "fewer"),
        "increase" to listOf("decrease", "reduce"),
        "gain" to listOf("loss", "lost"),
        "profit" to listOf("loss"),
        "guilty" to listOf("innocent", "not guilty"),
        "responsible" to listOf("not responsible", "irresponsible"),
        "fault" to listOf("no fault", "blameless")
    )

    // Contradiction trigger phrases that explicitly indicate contradiction
    private val contradictionTriggers = listOf(
        "that's not what i said",
        "i never said that",
        "contrary to",
        "that's false",
        "that's incorrect",
        "i deny",
        "that's a lie",
        "you're wrong",
        "actually",
        "in fact",
        "to correct",
        "to clarify",
        "the truth is",
        "what really happened"
    )

    /**
     * Compare all statements and detect contradictions.
     *
     * @param statements List of all statements to compare
     * @return List of detected contradictions
     */
    fun compareAllStatements(statements: List<Statement>): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()

        // Group by speaker for intra-speaker comparison
        val bySpeaker = statements.groupBy { it.speaker.lowercase() }

        // Pass 1: Intra-speaker contradictions (same person contradicting themselves)
        for ((speaker, speakerStatements) in bySpeaker) {
            if (speakerStatements.size < 2) continue

            for (i in speakerStatements.indices) {
                for (j in i + 1 until speakerStatements.size) {
                    val contradiction = compareStatements(
                        speakerStatements[i],
                        speakerStatements[j],
                        ContradictionType.DIRECT
                    )
                    contradiction?.let { contradictions.add(it) }
                }
            }
        }

        // Pass 2: Cross-document contradictions (same speaker, different documents)
        for ((speaker, speakerStatements) in bySpeaker) {
            val byDocument = speakerStatements.groupBy { it.documentId }
            if (byDocument.size < 2) continue

            val documents = byDocument.keys.toList()
            for (i in documents.indices) {
                for (j in i + 1 until documents.size) {
                    val docAStatements = byDocument[documents[i]] ?: continue
                    val docBStatements = byDocument[documents[j]] ?: continue

                    for (stmtA in docAStatements) {
                        for (stmtB in docBStatements) {
                            val contradiction = compareStatements(
                                stmtA,
                                stmtB,
                                ContradictionType.CROSS_DOCUMENT
                            )
                            contradiction?.let { contradictions.add(it) }
                        }
                    }
                }
            }
        }

        // Pass 3: Cross-speaker contradictions on the same topic
        val speakers = bySpeaker.keys.toList()
        for (i in speakers.indices) {
            for (j in i + 1 until speakers.size) {
                val statementsA = bySpeaker[speakers[i]] ?: continue
                val statementsB = bySpeaker[speakers[j]] ?: continue

                for (stmtA in statementsA) {
                    for (stmtB in statementsB) {
                        // Only compare if they seem to be about the same topic
                        if (areSameSubject(stmtA.text, stmtB.text)) {
                            val contradiction = compareStatements(
                                stmtA,
                                stmtB,
                                ContradictionType.ENTITY
                            )
                            contradiction?.let { contradictions.add(it) }
                        }
                    }
                }
            }
        }

        return contradictions.distinctBy { "${it.sourceStatement.id}-${it.targetStatement.id}" }
    }

    /**
     * Compare two statements for contradictions.
     *
     * @param stmtA First statement
     * @param stmtB Second statement
     * @param type Type of contradiction to classify as
     * @return Contradiction if detected, null otherwise
     */
    fun compareStatements(
        stmtA: Statement,
        stmtB: Statement,
        type: ContradictionType
    ): Contradiction? {
        val textA = stmtA.text.lowercase()
        val textB = stmtB.text.lowercase()

        // Check for lexical oppositions
        val oppositionResult = detectLexicalOpposition(textA, textB)
        if (oppositionResult != null) {
            return createContradiction(
                stmtA, stmtB, type,
                "Opposing claims: '${oppositionResult.first}' vs '${oppositionResult.second}'",
                calculateSeverity(stmtA, stmtB, 0.8),
                0.8
            )
        }

        // Check for negation patterns
        val negationResult = detectNegation(textA, textB)
        if (negationResult != null) {
            return createContradiction(
                stmtA, stmtB, type,
                negationResult,
                calculateSeverity(stmtA, stmtB, 0.9),
                0.9
            )
        }

        // Check for explicit contradiction triggers
        val triggerResult = detectContradictionTrigger(textA, textB)
        if (triggerResult != null) {
            return createContradiction(
                stmtA, stmtB, type,
                triggerResult,
                calculateSeverity(stmtA, stmtB, 0.95),
                0.95
            )
        }

        // Check for sentiment opposition with similar content
        if (areSameSubject(textA, textB)) {
            val sentimentDiff = abs(stmtA.sentiment - stmtB.sentiment)
            if (sentimentDiff > 1.0) {
                return createContradiction(
                    stmtA, stmtB, type,
                    "Opposing sentiment on same subject: positive vs negative",
                    calculateSeverity(stmtA, stmtB, sentimentDiff / 2.0),
                    sentimentDiff / 2.0
                )
            }
        }

        // Check for claim vs denial pattern
        if (isClaimDenialPair(stmtA, stmtB)) {
            return createContradiction(
                stmtA, stmtB, type,
                "Claim directly contradicted by denial",
                calculateSeverity(stmtA, stmtB, 0.85),
                0.85
            )
        }

        return null
    }

    /**
     * Detect lexical opposition between two texts.
     */
    private fun detectLexicalOpposition(textA: String, textB: String): Pair<String, String>? {
        for ((term, opposites) in lexicalOppositions) {
            val hasTermA = textA.contains("\\b$term\\b".toRegex())
            val hasTermB = textB.contains("\\b$term\\b".toRegex())

            for (opposite in opposites) {
                val hasOppositeA = textA.contains("\\b${Regex.escape(opposite)}\\b".toRegex())
                val hasOppositeB = textB.contains("\\b${Regex.escape(opposite)}\\b".toRegex())

                // A has positive, B has negative
                if (hasTermA && hasOppositeB) {
                    return Pair(term, opposite)
                }
                // A has negative, B has positive
                if (hasOppositeA && hasTermB) {
                    return Pair(opposite, term)
                }
            }
        }
        return null
    }

    /**
     * Detect negation-based contradiction.
     */
    private fun detectNegation(textA: String, textB: String): String? {
        // Extract key phrases (simple approach - first verb and object)
        val keywordsA = extractKeywords(textA)
        val keywordsB = extractKeywords(textB)

        // Check for common keywords with opposite polarity
        val commonKeywords = keywordsA.intersect(keywordsB)
        if (commonKeywords.size >= 2) {
            val hasNegationA = hasNegation(textA)
            val hasNegationB = hasNegation(textB)

            if (hasNegationA != hasNegationB) {
                return "Contradictory statements about: ${commonKeywords.joinToString(", ")}"
            }
        }

        return null
    }

    /**
     * Check if text contains a negation.
     */
    private fun hasNegation(text: String): Boolean {
        val negationWords = listOf(
            "not", "never", "no", "none", "nobody", "nothing", "neither",
            "nowhere", "n't", "cannot", "can't", "won't", "wouldn't",
            "couldn't", "shouldn't", "haven't", "hasn't", "hadn't",
            "don't", "doesn't", "didn't", "isn't", "aren't", "wasn't", "weren't"
        )
        return negationWords.any { text.contains("\\b$it\\b".toRegex()) }
    }

    /**
     * Extract keywords from text.
     */
    private fun extractKeywords(text: String): Set<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "must", "shall", "can", "to", "of", "in",
            "for", "on", "with", "at", "by", "from", "as", "into", "through",
            "i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them",
            "my", "your", "his", "its", "our", "their", "this", "that", "these", "those"
        )

        return text.lowercase()
            .replace(Regex("[^a-z\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
    }

    /**
     * Detect explicit contradiction triggers.
     */
    private fun detectContradictionTrigger(textA: String, textB: String): String? {
        for (trigger in contradictionTriggers) {
            if (textB.contains(trigger)) {
                return "Explicit contradiction: '$trigger' in response to earlier statement"
            }
        }
        return null
    }

    /**
     * Check if two texts are about the same subject.
     */
    private fun areSameSubject(textA: String, textB: String): Boolean {
        val keywordsA = extractKeywords(textA)
        val keywordsB = extractKeywords(textB)

        if (keywordsA.isEmpty() || keywordsB.isEmpty()) return false

        val intersection = keywordsA.intersect(keywordsB)
        val union = keywordsA.union(keywordsB)

        // Jaccard similarity threshold
        val similarity = intersection.size.toDouble() / union.size.toDouble()
        return similarity > 0.3
    }

    /**
     * Check if this is a claim-denial pair.
     */
    private fun isClaimDenialPair(stmtA: Statement, stmtB: Statement): Boolean {
        val claimCategories = listOf(
            com.verumdec.core.model.LegalCategory.ASSERTION,
            com.verumdec.core.model.LegalCategory.PROMISE,
            com.verumdec.core.model.LegalCategory.ADMISSION
        )
        val denialCategories = listOf(
            com.verumdec.core.model.LegalCategory.DENIAL
        )

        val isAClaimBDenial = stmtA.legalCategory in claimCategories && stmtB.legalCategory in denialCategories
        val isBClaimADenial = stmtB.legalCategory in claimCategories && stmtA.legalCategory in denialCategories

        if (!isAClaimBDenial && !isBClaimADenial) return false

        // Also check they're about the same subject
        return areSameSubject(stmtA.text.lowercase(), stmtB.text.lowercase())
    }

    /**
     * Create a Contradiction object.
     */
    private fun createContradiction(
        stmtA: Statement,
        stmtB: Statement,
        type: ContradictionType,
        description: String,
        severity: Int,
        similarityScore: Double
    ): Contradiction {
        return Contradiction(
            id = UUID.randomUUID().toString(),
            type = type,
            sourceStatement = stmtA,
            targetStatement = stmtB,
            sourceDocument = stmtA.documentId,
            sourceLineNumber = stmtA.lineNumber,
            severity = severity,
            description = "$description: '${stmtA.text.take(50)}...' vs '${stmtB.text.take(50)}...'",
            legalTrigger = determineLegalTrigger(stmtA, stmtB, type),
            affectedEntities = listOf(stmtA.speaker, stmtB.speaker).distinct(),
            similarityScore = similarityScore
        )
    }

    /**
     * Calculate severity based on statement types and content.
     */
    private fun calculateSeverity(stmtA: Statement, stmtB: Statement, confidenceScore: Double): Int {
        var baseSeverity = (confidenceScore * 10).toInt().coerceIn(1, 10)

        // Increase for admission followed by denial (critical)
        if (stmtA.legalCategory == com.verumdec.core.model.LegalCategory.ADMISSION &&
            stmtB.legalCategory == com.verumdec.core.model.LegalCategory.DENIAL) {
            baseSeverity = minOf(baseSeverity + 2, 10)
        }

        // Increase for same speaker (self-contradiction)
        if (stmtA.speaker.equals(stmtB.speaker, ignoreCase = true)) {
            baseSeverity = minOf(baseSeverity + 1, 10)
        }

        // Increase for different documents (cross-document)
        if (stmtA.documentId != stmtB.documentId) {
            baseSeverity = minOf(baseSeverity + 1, 10)
        }

        return baseSeverity
    }

    /**
     * Determine the appropriate legal trigger.
     */
    private fun determineLegalTrigger(
        stmtA: Statement,
        stmtB: Statement,
        type: ContradictionType
    ): LegalTrigger {
        return when {
            // Denial after admission suggests unreliable testimony
            stmtA.legalCategory == com.verumdec.core.model.LegalCategory.ADMISSION &&
            stmtB.legalCategory == com.verumdec.core.model.LegalCategory.DENIAL ->
                LegalTrigger.UNRELIABLE_TESTIMONY

            // Same speaker contradicting themselves
            stmtA.speaker.equals(stmtB.speaker, ignoreCase = true) ->
                LegalTrigger.UNRELIABLE_TESTIMONY

            // Cross-document contradiction
            type == ContradictionType.CROSS_DOCUMENT ->
                LegalTrigger.MISREPRESENTATION

            // Financial statements
            stmtA.legalCategory == com.verumdec.core.model.LegalCategory.FINANCIAL ||
            stmtB.legalCategory == com.verumdec.core.model.LegalCategory.FINANCIAL ->
                LegalTrigger.FINANCIAL_DISCREPANCY

            // Entity contradiction (different speakers)
            type == ContradictionType.ENTITY ->
                LegalTrigger.MISREPRESENTATION

            else -> LegalTrigger.CONCEALMENT
        }
    }
}

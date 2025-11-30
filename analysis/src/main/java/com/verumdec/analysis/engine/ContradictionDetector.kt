package com.verumdec.analysis.engine

import java.util.*

/**
 * ContradictionDetector - Detects contradictions between statements.
 */
class ContradictionDetector {

    // Negation patterns
    private val negationPatterns = listOf(
        "never" to "always",
        "didn't" to "did",
        "did not" to "did",
        "wasn't" to "was",
        "was not" to "was",
        "haven't" to "have",
        "have not" to "have",
        "won't" to "will",
        "will not" to "will",
        "can't" to "can",
        "cannot" to "can",
        "don't" to "do",
        "do not" to "do",
        "no" to "yes",
        "false" to "true",
        "deny" to "admit"
    )

    /**
     * Detect contradictions between two statements.
     */
    fun detectContradiction(
        statement1: StatementInput,
        statement2: StatementInput
    ): ContradictionResult? {
        // Check for direct negation contradiction
        val directContradiction = checkDirectContradiction(statement1, statement2)
        if (directContradiction != null) return directContradiction

        // Check for semantic contradiction
        val semanticContradiction = checkSemanticContradiction(statement1, statement2)
        if (semanticContradiction != null) return semanticContradiction

        // Check for temporal contradiction
        val temporalContradiction = checkTemporalContradiction(statement1, statement2)
        if (temporalContradiction != null) return temporalContradiction

        return null
    }

    /**
     * Detect all contradictions in a list of statements.
     */
    fun detectAllContradictions(statements: List<StatementInput>): List<ContradictionResult> {
        val contradictions = mutableListOf<ContradictionResult>()

        for (i in statements.indices) {
            for (j in i + 1 until statements.size) {
                val contradiction = detectContradiction(statements[i], statements[j])
                if (contradiction != null) {
                    contradictions.add(contradiction)
                }
            }
        }

        return contradictions
    }

    /**
     * Check for direct negation pattern.
     */
    private fun checkDirectContradiction(
        s1: StatementInput,
        s2: StatementInput
    ): ContradictionResult? {
        val text1 = s1.text.lowercase()
        val text2 = s2.text.lowercase()

        // Check if same entity made both statements
        if (s1.entityId != s2.entityId) return null

        // Look for negation patterns
        for ((negative, positive) in negationPatterns) {
            if ((text1.contains(negative) && text2.contains(positive)) ||
                (text1.contains(positive) && text2.contains(negative))) {
                
                // Check for similar subject matter
                val commonWords = extractSignificantWords(text1).intersect(extractSignificantWords(text2).toSet())
                if (commonWords.size >= 2) {
                    return ContradictionResult(
                        type = ContradictionType.DIRECT,
                        statement1 = s1,
                        statement2 = s2,
                        severity = calculateSeverity(s1, s2, ContradictionType.DIRECT),
                        description = "Direct contradiction: Statement contains '$negative' while another contains '$positive' on same subject",
                        confidence = 0.85f
                    )
                }
            }
        }

        return null
    }

    /**
     * Check for semantic contradiction.
     */
    private fun checkSemanticContradiction(
        s1: StatementInput,
        s2: StatementInput
    ): ContradictionResult? {
        val text1 = s1.text.lowercase()
        val text2 = s2.text.lowercase()

        // Check for amount contradictions
        val amounts1 = extractAmounts(text1)
        val amounts2 = extractAmounts(text2)
        
        if (amounts1.isNotEmpty() && amounts2.isNotEmpty()) {
            // Check if discussing same thing but different amounts
            val commonWords = extractSignificantWords(text1).intersect(extractSignificantWords(text2).toSet())
            if (commonWords.size >= 2 && amounts1 != amounts2) {
                return ContradictionResult(
                    type = ContradictionType.CROSS_DOCUMENT,
                    statement1 = s1,
                    statement2 = s2,
                    severity = ContradictionSeverity.HIGH,
                    description = "Amount contradiction: Different amounts mentioned for same subject ($amounts1 vs $amounts2)",
                    confidence = 0.75f
                )
            }
        }

        // Check for date contradictions
        val dates1 = extractDates(text1)
        val dates2 = extractDates(text2)
        
        if (dates1.isNotEmpty() && dates2.isNotEmpty() && dates1 != dates2) {
            val commonWords = extractSignificantWords(text1).intersect(extractSignificantWords(text2).toSet())
            if (commonWords.size >= 2) {
                return ContradictionResult(
                    type = ContradictionType.TEMPORAL,
                    statement1 = s1,
                    statement2 = s2,
                    severity = ContradictionSeverity.MEDIUM,
                    description = "Date contradiction: Different dates mentioned for same event",
                    confidence = 0.70f
                )
            }
        }

        return null
    }

    /**
     * Check for temporal consistency issues.
     */
    private fun checkTemporalContradiction(
        s1: StatementInput,
        s2: StatementInput
    ): ContradictionResult? {
        if (s1.date == null || s2.date == null) return null
        if (s1.entityId != s2.entityId) return null

        val text1 = s1.text.lowercase()
        val text2 = s2.text.lowercase()

        // Check if story changed significantly over time
        val significantWords1 = extractSignificantWords(text1)
        val significantWords2 = extractSignificantWords(text2)
        
        val commonSubject = significantWords1.intersect(significantWords2.toSet())
        val differentWords = (significantWords1 + significantWords2) - commonSubject
        
        if (commonSubject.size >= 2 && differentWords.size > commonSubject.size * 2) {
            // Story seems to have evolved significantly
            val daysBetween = (s2.date.time - s1.date.time) / (1000 * 60 * 60 * 24)
            
            if (daysBetween.toInt() in 1..30) {
                return ContradictionResult(
                    type = ContradictionType.BEHAVIORAL,
                    statement1 = s1,
                    statement2 = s2,
                    severity = ContradictionSeverity.MEDIUM,
                    description = "Story shift: Account changed significantly within $daysBetween days",
                    confidence = 0.60f
                )
            }
        }

        return null
    }

    /**
     * Extract significant words from text.
     */
    private fun extractSignificantWords(text: String): Set<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "i", "you", "he", "she", "it", "we", "they", "my", "your", "his", "her",
            "this", "that", "these", "those", "and", "or", "but", "if", "then",
            "to", "of", "in", "for", "on", "with", "at", "by", "from", "about"
        )

        return text.split(Regex("[\\s,.!?;:]+"))
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
    }

    /**
     * Extract monetary amounts from text.
     */
    private fun extractAmounts(text: String): Set<String> {
        val pattern = Regex("[R$€£¥]\\s*[0-9,]+\\.?[0-9]*|[0-9,]+\\.?[0-9]*\\s*(?:rand|dollars?|euros?|pounds?)")
        return pattern.findAll(text).map { it.value }.toSet()
    }

    /**
     * Extract dates from text.
     */
    private fun extractDates(text: String): Set<String> {
        val patterns = listOf(
            Regex("\\d{1,2}/\\d{1,2}/\\d{2,4}"),
            Regex("\\d{4}-\\d{2}-\\d{2}"),
            Regex("\\d{1,2}\\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s+\\d{4}", RegexOption.IGNORE_CASE)
        )
        
        return patterns.flatMap { pattern -> 
            pattern.findAll(text).map { it.value } 
        }.toSet()
    }

    /**
     * Calculate severity based on statement properties.
     */
    private fun calculateSeverity(
        s1: StatementInput,
        s2: StatementInput,
        type: ContradictionType
    ): ContradictionSeverity {
        // Critical if involves money or legal matters
        val combinedText = "${s1.text} ${s2.text}".lowercase()
        if (combinedText.contains("payment") || combinedText.contains("money") ||
            combinedText.contains("contract") || combinedText.contains("agree")) {
            return ContradictionSeverity.CRITICAL
        }

        // High for direct contradictions
        if (type == ContradictionType.DIRECT) {
            return ContradictionSeverity.HIGH
        }

        // Medium for cross-document
        if (type == ContradictionType.CROSS_DOCUMENT) {
            return ContradictionSeverity.MEDIUM
        }

        return ContradictionSeverity.LOW
    }
}

// Data classes

data class StatementInput(
    val id: String,
    val entityId: String,
    val text: String,
    val date: Date?,
    val sourceEvidenceId: String
)

enum class ContradictionType {
    DIRECT, CROSS_DOCUMENT, BEHAVIORAL, TEMPORAL, MISSING_EVIDENCE, THIRD_PARTY
}

enum class ContradictionSeverity {
    CRITICAL, HIGH, MEDIUM, LOW
}

data class ContradictionResult(
    val type: ContradictionType,
    val statement1: StatementInput,
    val statement2: StatementInput,
    val severity: ContradictionSeverity,
    val description: String,
    val confidence: Float
)

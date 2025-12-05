package com.verumdec.leveler

import java.time.Instant
import java.util.regex.Pattern

/**
 * Leveler Engine for Verum Omnis
 *
 * Implements the contradiction_engine from verum-constitution.json:
 * - timeline analysis
 * - statement comparison
 * - behavioural inconsistencies
 * - document metadata mismatches
 * - coercion indicators
 * - intent vs action mismatch
 *
 * The Leveler Engine analyzes documents for:
 * 1. Contradictory statements
 * 2. Timeline anomalies
 * 3. Evasion patterns
 * 4. Integrity scoring
 */
class LevelerEngine {

    companion object {
        // Evasion pattern keywords - indicates possible dishonesty
        private val EVASION_KEYWORDS = listOf(
            "i don't recall",
            "i don't remember",
            "i can't remember",
            "not sure",
            "maybe",
            "i think",
            "perhaps",
            "possibly",
            "i'm not certain",
            "to the best of my knowledge",
            "as far as i know",
            "i believe"
        )

        // Contradiction indicator words
        private val CONTRADICTION_KEYWORDS = listOf(
            "never", "always", "no", "yes", "did", "didn't",
            "received", "not received", "paid", "unpaid",
            "present", "absent", "attended", "missed"
        )

        // Financial keywords for detecting financial contradictions
        private val FINANCIAL_KEYWORDS = listOf(
            "\\$[0-9,]+", "usd", "eur", "aed", "zar",
            "payment", "invoice", "received", "transferred",
            "deposit", "withdrawal"
        )

        /**
         * Threshold for detecting suspicious date ordering in document lines.
         * If dates appear more than this many lines apart in reverse order,
         * it may indicate timeline manipulation.
         */
        private const val DATE_ORDER_THRESHOLD_LINES = 10
    }

    /**
     * Analyzes a document for contradictions, evasion patterns, and integrity
     */
    fun analyzeDocument(content: String): LevelerAnalysis {
        val normalizedContent = content.lowercase()
        val lines = content.split("\n").filter { it.isNotBlank() }

        val contradictions = detectContradictions(lines)
        val evasionPatterns = detectEvasionPatterns(normalizedContent)
        val timelineIssues = detectTimelineAnomalies(lines)
        val financialContradictions = detectFinancialContradictions(lines)

        val totalIssues = contradictions.size + evasionPatterns.size +
                timelineIssues.size + financialContradictions.size

        // Calculate integrity score (100% = no issues, decreases with each issue)
        val integrityScore = calculateIntegrityScore(
            contradictionCount = contradictions.size,
            evasionCount = evasionPatterns.size,
            timelineIssueCount = timelineIssues.size,
            financialIssueCount = financialContradictions.size
        )

        // Calculate suspicion score (0.0 = no suspicion, 1.0 = high suspicion)
        val suspicionScore = calculateSuspicionScore(totalIssues, lines.size)

        return LevelerAnalysis(
            documentHash = hashCode().toString(),
            timestamp = Instant.now(),
            contradictions = contradictions,
            evasionPatterns = evasionPatterns,
            timelineIssues = timelineIssues,
            financialContradictions = financialContradictions,
            integrityScore = integrityScore,
            suspicionScore = suspicionScore,
            overallAssessment = generateAssessment(integrityScore, suspicionScore)
        )
    }

    /**
     * Analyzes multiple documents together for cross-document contradictions
     */
    fun analyzeCrossDocument(documents: List<DocumentEntry>): CrossDocumentAnalysis {
        val allContradictions = mutableListOf<Contradiction>()
        val timelineIssues = mutableListOf<TimelineIssue>()

        // Compare each document pair
        for (i in documents.indices) {
            for (j in i + 1 until documents.size) {
                val crossContradictions = findCrossDocumentContradictions(
                    documents[i], documents[j]
                )
                allContradictions.addAll(crossContradictions)

                val crossTimeline = findTimelineInconsistencies(
                    documents[i], documents[j]
                )
                timelineIssues.addAll(crossTimeline)
            }
        }

        val integrityScores = documents.associateWith { doc ->
            calculateIndividualIntegrity(doc.content)
        }

        return CrossDocumentAnalysis(
            documentCount = documents.size,
            crossContradictions = allContradictions,
            timelineInconsistencies = timelineIssues,
            integrityScores = integrityScores,
            overallIntegrity = integrityScores.values.average()
        )
    }

    /**
     * Detects contradictory statements within a document
     */
    private fun detectContradictions(lines: List<String>): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()
        val statements = mutableMapOf<String, MutableList<Pair<Int, String>>>()

        // Extract key statements and group by topic
        lines.forEachIndexed { index, line ->
            val lowerLine = line.lowercase()

            // Check for money-related statements
            if (lowerLine.contains("received") || lowerLine.contains("paid") ||
                lowerLine.contains("money") || lowerLine.matches(Regex(".*\\$[0-9,]+.*"))
            ) {
                statements.getOrPut("financial") { mutableListOf() }.add(index to line)
            }

            // Check for meeting/event statements
            if (lowerLine.contains("meeting") || lowerLine.contains("attended") ||
                lowerLine.contains("present") || lowerLine.contains("absent")
            ) {
                statements.getOrPut("events") { mutableListOf() }.add(index to line)
            }

            // Check for denial/confirmation statements
            val hasNegation = lowerLine.contains("never") || lowerLine.contains("always") ||
                lowerLine.contains("did not") || lowerLine.contains("didn't") ||
                lowerLine.contains("not") || lowerLine.contains("no ")
            val hasAffirmation = lowerLine.contains("yes") || lowerLine.contains("did ") ||
                lowerLine.contains("received") || lowerLine.contains("paid") ||
                lowerLine.contains("confirmed") || lowerLine.contains("attended")

            if (hasNegation || hasAffirmation) {
                statements.getOrPut("denials") { mutableListOf() }.add(index to line)
            }
        }

        // Analyze financial statements for contradictions
        statements["financial"]?.let { financialStatements ->
            for (i in financialStatements.indices) {
                for (j in i + 1 until financialStatements.size) {
                    val (idx1, stmt1) = financialStatements[i]
                    val (idx2, stmt2) = financialStatements[j]

                    if (isContradictory(stmt1, stmt2)) {
                        contradictions.add(
                            Contradiction(
                                statement1 = stmt1,
                                statement1Line = idx1 + 1,
                                statement2 = stmt2,
                                statement2Line = idx2 + 1,
                                category = "financial",
                                severity = Severity.HIGH,
                                explanation = "Potential contradiction in financial statements"
                            )
                        )
                    }
                }
            }
        }

        return contradictions
    }

    /**
     * Detects evasion patterns in text
     */
    private fun detectEvasionPatterns(content: String): List<EvasionPattern> {
        val patterns = mutableListOf<EvasionPattern>()
        var totalEvasionScore = 0

        EVASION_KEYWORDS.forEach { keyword ->
            val regex = Regex("\\b${Pattern.quote(keyword)}\\b", RegexOption.IGNORE_CASE)
            val matches = regex.findAll(content).toList()

            if (matches.isNotEmpty()) {
                totalEvasionScore += matches.size
                patterns.add(
                    EvasionPattern(
                        keyword = keyword,
                        occurrences = matches.size,
                        positions = matches.map { it.range.first },
                        severity = when {
                            matches.size >= 5 -> Severity.HIGH
                            matches.size >= 3 -> Severity.MEDIUM
                            else -> Severity.LOW
                        }
                    )
                )
            }
        }

        // Upgrade severity of highest-occurring pattern if total evasion count is high
        if (totalEvasionScore >= 5 && patterns.isNotEmpty() && patterns.none { it.severity == Severity.HIGH }) {
            // Find index and pattern with max occurrences in single pass
            val (maxIndex, _) = patterns.withIndex().maxByOrNull { it.value.occurrences } ?: return patterns
            patterns[maxIndex] = patterns[maxIndex].copy(severity = Severity.HIGH)
        }

        return patterns
    }

    /**
     * Detects timeline anomalies
     */
    private fun detectTimelineAnomalies(lines: List<String>): List<TimelineIssue> {
        val issues = mutableListOf<TimelineIssue>()
        val datePattern = Regex(
            "\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|" +
                    "\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}|" +
                    "(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s+\\d{1,2})",
            RegexOption.IGNORE_CASE
        )

        val datesFound = mutableListOf<Pair<Int, String>>()

        lines.forEachIndexed { index, line ->
            datePattern.findAll(line).forEach { match ->
                datesFound.add(index to match.value)
            }
        }

        // Check for suspicious patterns - dates mentioned before they could have happened
        for (i in datesFound.indices) {
            for (j in i + 1 until datesFound.size) {
                val (lineIdx1, _) = datesFound[i]
                val (lineIdx2, _) = datesFound[j]

                // If a later document line references an earlier date, it might be suspicious
                if (lineIdx1 > lineIdx2 + DATE_ORDER_THRESHOLD_LINES) {
                    issues.add(
                        TimelineIssue(
                            description = "Date reference order may indicate timeline manipulation",
                            lineNumber = lineIdx1 + 1,
                            severity = Severity.MEDIUM,
                            details = "Date referenced at line ${lineIdx1 + 1} may be inconsistent with document flow"
                        )
                    )
                }
            }
        }

        return issues
    }

    /**
     * Detects financial contradictions (amounts that don't add up)
     */
    private fun detectFinancialContradictions(lines: List<String>): List<FinancialContradiction> {
        val contradictions = mutableListOf<FinancialContradiction>()
        val amountPattern = Regex("\\$([0-9,]+\\.?[0-9]*)")

        val amountsFound = mutableMapOf<Double, MutableList<Pair<Int, String>>>()

        lines.forEachIndexed { index, line ->
            amountPattern.findAll(line).forEach { match ->
                val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
                if (amount != null) {
                    amountsFound.getOrPut(amount) { mutableListOf() }.add(index to line)
                }
            }
        }

        // Check for conflicting statements about the same amount
        amountsFound.forEach { (amount, occurrences) ->
            if (occurrences.size > 1) {
                val hasContradiction = occurrences.any { (_, line) ->
                    line.lowercase().contains("never") ||
                            line.lowercase().contains("not") ||
                            line.lowercase().contains("didn't")
                } && occurrences.any { (_, line) ->
                    line.lowercase().contains("received") ||
                            line.lowercase().contains("paid") ||
                            line.lowercase().contains("transferred")
                }

                if (hasContradiction) {
                    contradictions.add(
                        FinancialContradiction(
                            amount = amount,
                            occurrences = occurrences.map { it.second },
                            lineNumbers = occurrences.map { it.first + 1 },
                            severity = Severity.HIGH,
                            explanation = "Contradictory statements about \$${amount}"
                        )
                    )
                }
            }
        }

        return contradictions
    }

    /**
     * Checks if two statements are contradictory
     */
    private fun isContradictory(stmt1: String, stmt2: String): Boolean {
        val lower1 = stmt1.lowercase()
        val lower2 = stmt2.lowercase()

        // Check for opposite assertions
        val negationWords = listOf("never", "not", "no", "didn't", "don't", "haven't")
        val affirmationWords = listOf("yes", "did", "received", "paid", "have", "always")

        val stmt1HasNegation = negationWords.any { lower1.contains(it) }
        val stmt2HasNegation = negationWords.any { lower2.contains(it) }
        val stmt1HasAffirmation = affirmationWords.any { lower1.contains(it) }
        val stmt2HasAffirmation = affirmationWords.any { lower2.contains(it) }

        // Contradiction if one negates and one affirms
        return (stmt1HasNegation && stmt2HasAffirmation) ||
                (stmt1HasAffirmation && stmt2HasNegation)
    }

    /**
     * Finds contradictions between two documents
     */
    private fun findCrossDocumentContradictions(
        doc1: DocumentEntry,
        doc2: DocumentEntry
    ): List<Contradiction> {
        val contradictions = mutableListOf<Contradiction>()

        val lines1 = doc1.content.split("\n")
        val lines2 = doc2.content.split("\n")

        // Simple cross-document comparison for key statements
        lines1.forEachIndexed { idx1, line1 ->
            lines2.forEachIndexed { idx2, line2 ->
                if (isContradictory(line1, line2)) {
                    contradictions.add(
                        Contradiction(
                            statement1 = line1,
                            statement1Line = idx1 + 1,
                            statement2 = line2,
                            statement2Line = idx2 + 1,
                            category = "cross-document",
                            severity = Severity.HIGH,
                            explanation = "Contradiction between ${doc1.name} and ${doc2.name}"
                        )
                    )
                }
            }
        }

        return contradictions
    }

    /**
     * Finds timeline inconsistencies between documents
     */
    private fun findTimelineInconsistencies(
        doc1: DocumentEntry,
        doc2: DocumentEntry
    ): List<TimelineIssue> {
        val issues = mutableListOf<TimelineIssue>()

        // Check if document creation dates are suspicious
        if (doc1.createdAt != null && doc2.createdAt != null) {
            if (doc1.createdAt.isAfter(doc2.createdAt) &&
                doc1.content.contains("before") &&
                doc1.content.contains(doc2.name)
            ) {
                issues.add(
                    TimelineIssue(
                        description = "Document ${doc1.name} created after ${doc2.name} but references prior events",
                        lineNumber = 0,
                        severity = Severity.HIGH,
                        details = "Timeline inconsistency detected"
                    )
                )
            }
        }

        return issues
    }

    /**
     * Calculates integrity score (0-100)
     */
    private fun calculateIntegrityScore(
        contradictionCount: Int,
        evasionCount: Int,
        timelineIssueCount: Int,
        financialIssueCount: Int
    ): Double {
        val baseScore = 100.0
        val contradictionPenalty = contradictionCount * 15.0
        val evasionPenalty = evasionCount * 5.0
        val timelinePenalty = timelineIssueCount * 10.0
        val financialPenalty = financialIssueCount * 20.0

        val score = baseScore - contradictionPenalty - evasionPenalty -
                timelinePenalty - financialPenalty

        return score.coerceIn(0.0, 100.0)
    }

    /**
     * Calculates individual document integrity
     */
    private fun calculateIndividualIntegrity(content: String): Double {
        val analysis = analyzeDocument(content)
        return analysis.integrityScore
    }

    /**
     * Calculates suspicion score (0.0 - 1.0)
     */
    private fun calculateSuspicionScore(totalIssues: Int, totalLines: Int): Double {
        if (totalLines == 0) return 0.0

        val issueRatio = totalIssues.toDouble() / totalLines.coerceAtLeast(1)
        val suspicionScore = (issueRatio * 10).coerceIn(0.0, 1.0)

        return suspicionScore
    }

    /**
     * Generates an overall assessment
     */
    private fun generateAssessment(integrityScore: Double, suspicionScore: Double): Assessment {
        return when {
            integrityScore >= 90 && suspicionScore < 0.1 -> Assessment.HIGHLY_RELIABLE
            integrityScore >= 70 && suspicionScore < 0.3 -> Assessment.GENERALLY_RELIABLE
            integrityScore >= 50 && suspicionScore < 0.5 -> Assessment.NEEDS_VERIFICATION
            integrityScore >= 30 && suspicionScore < 0.7 -> Assessment.QUESTIONABLE
            else -> Assessment.HIGHLY_SUSPECT
        }
    }
}

/**
 * Severity levels for issues
 */
enum class Severity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Overall assessment categories
 */
enum class Assessment {
    HIGHLY_RELIABLE,
    GENERALLY_RELIABLE,
    NEEDS_VERIFICATION,
    QUESTIONABLE,
    HIGHLY_SUSPECT
}

/**
 * Represents a contradiction found in analysis
 */
data class Contradiction(
    val statement1: String,
    val statement1Line: Int,
    val statement2: String,
    val statement2Line: Int,
    val category: String,
    val severity: Severity,
    val explanation: String
)

/**
 * Represents an evasion pattern detected
 */
data class EvasionPattern(
    val keyword: String,
    val occurrences: Int,
    val positions: List<Int>,
    val severity: Severity
)

/**
 * Represents a timeline issue
 */
data class TimelineIssue(
    val description: String,
    val lineNumber: Int,
    val severity: Severity,
    val details: String
)

/**
 * Represents a financial contradiction
 */
data class FinancialContradiction(
    val amount: Double,
    val occurrences: List<String>,
    val lineNumbers: List<Int>,
    val severity: Severity,
    val explanation: String
)

/**
 * Complete analysis result from the Leveler Engine
 */
data class LevelerAnalysis(
    val documentHash: String,
    val timestamp: Instant,
    val contradictions: List<Contradiction>,
    val evasionPatterns: List<EvasionPattern>,
    val timelineIssues: List<TimelineIssue>,
    val financialContradictions: List<FinancialContradiction>,
    val integrityScore: Double,
    val suspicionScore: Double,
    val overallAssessment: Assessment
) {
    /**
     * Generates a human-readable summary
     */
    fun generateSummary(): String = buildString {
        appendLine("=" .repeat(60))
        appendLine("LEVELER ENGINE ANALYSIS REPORT")
        appendLine("=".repeat(60))
        appendLine()
        appendLine("Analysis Timestamp: $timestamp")
        appendLine()
        appendLine("-".repeat(40))
        appendLine("INTEGRITY ASSESSMENT")
        appendLine("-".repeat(40))
        appendLine("Integrity Score: ${String.format("%.1f", integrityScore)}%")
        appendLine("Suspicion Score: ${String.format("%.2f", suspicionScore)}")
        appendLine("Overall Assessment: ${overallAssessment.name.replace("_", " ")}")
        appendLine()

        if (contradictions.isNotEmpty()) {
            appendLine("-".repeat(40))
            appendLine("CONTRADICTIONS DETECTED: ${contradictions.size}")
            appendLine("-".repeat(40))
            contradictions.forEach { c ->
                appendLine("• [${c.severity}] ${c.explanation}")
                appendLine("  Line ${c.statement1Line}: ${c.statement1.take(50)}...")
                appendLine("  Line ${c.statement2Line}: ${c.statement2.take(50)}...")
                appendLine()
            }
        }

        if (evasionPatterns.isNotEmpty()) {
            appendLine("-".repeat(40))
            appendLine("EVASION PATTERNS DETECTED: ${evasionPatterns.size}")
            appendLine("-".repeat(40))
            evasionPatterns.forEach { e ->
                appendLine("• [${e.severity}] \"${e.keyword}\" - ${e.occurrences} occurrence(s)")
            }
            appendLine()
        }

        if (timelineIssues.isNotEmpty()) {
            appendLine("-".repeat(40))
            appendLine("TIMELINE ISSUES: ${timelineIssues.size}")
            appendLine("-".repeat(40))
            timelineIssues.forEach { t ->
                appendLine("• [${t.severity}] ${t.description}")
            }
            appendLine()
        }

        if (financialContradictions.isNotEmpty()) {
            appendLine("-".repeat(40))
            appendLine("FINANCIAL CONTRADICTIONS: ${financialContradictions.size}")
            appendLine("-".repeat(40))
            financialContradictions.forEach { f ->
                appendLine("• [${f.severity}] ${f.explanation}")
            }
            appendLine()
        }

        appendLine("=".repeat(60))
        appendLine("END OF LEVELER ENGINE REPORT")
        appendLine("=".repeat(60))
    }
}

/**
 * Represents a document entry for cross-document analysis
 */
data class DocumentEntry(
    val name: String,
    val content: String,
    val createdAt: Instant? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Cross-document analysis result
 */
data class CrossDocumentAnalysis(
    val documentCount: Int,
    val crossContradictions: List<Contradiction>,
    val timelineInconsistencies: List<TimelineIssue>,
    val integrityScores: Map<DocumentEntry, Double>,
    val overallIntegrity: Double
)

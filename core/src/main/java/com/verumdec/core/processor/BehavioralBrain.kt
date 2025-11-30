package com.verumdec.core.processor

import java.util.Date
import java.util.UUID

/**
 * BehavioralBrain - Contradiction detection between documents
 *
 * Analyzes documents and communications to detect behavioral patterns,
 * contradictions, and inconsistencies between statements.
 *
 * Operates fully offline without external dependencies.
 */
class BehavioralBrain {

    companion object {
        // Behavioral pattern indicators
        private val GASLIGHTING_PATTERNS = listOf(
            "you're imagining", "that never happened", "you're crazy",
            "i never said that", "you're confused", "you misunderstood",
            "you're overreacting", "you're being paranoid", "you're making things up",
            "you're too sensitive", "you always", "you never"
        )

        private val DEFLECTION_PATTERNS = listOf(
            "what about", "but you", "that's not the point",
            "let's focus on", "the real issue is", "you're changing the subject",
            "that's irrelevant", "we're not talking about that"
        )

        private val PRESSURE_PATTERNS = listOf(
            "you need to decide now", "this offer expires", "take it or leave it",
            "everyone else agrees", "don't miss out", "act fast", "limited time",
            "last chance", "final offer", "now or never", "urgently"
        )

        private val MANIPULATION_PATTERNS = listOf(
            "just this once", "i'll pay you back", "trust me", "it's an investment",
            "you'll make it back", "guaranteed return", "no risk",
            "if you loved me", "after all i've done", "you owe me",
            "don't you trust me", "i thought we were friends"
        )

        private val ADMISSION_PATTERNS = listOf(
            "i thought i was in the clear", "i didn't think anyone would notice",
            "i assumed it would be fine", "technically", "in a way",
            "i admit", "yes i did", "honestly", "to be fair", "actually"
        )

        private val NEGATION_WORDS = setOf(
            "not", "never", "no", "didn't", "didn't", "wasn't", "won't",
            "don't", "can't", "couldn't", "wouldn't", "shouldn't",
            "haven't", "hasn't", "hadn't", "none", "nothing", "nobody"
        )
    }

    /**
     * Analyze documents for behavioral patterns and contradictions.
     *
     * @param entityId The entity being analyzed
     * @param documents List of document contents with metadata
     * @return BehavioralBrainResult containing analysis or error
     */
    fun analyze(entityId: String, documents: List<DocumentInput>): BehavioralBrainResult {
        return try {
            if (documents.isEmpty()) {
                return BehavioralBrainResult.Failure(
                    error = "No documents provided for analysis",
                    errorCode = BehavioralErrorCode.INSUFFICIENT_DATA
                )
            }

            val statements = extractStatements(documents)
            if (statements.size < 2) {
                return BehavioralBrainResult.Failure(
                    error = "Insufficient statements for contradiction analysis",
                    errorCode = BehavioralErrorCode.INSUFFICIENT_DATA
                )
            }

            val contradictions = detectContradictions(statements, documents)
            val behavioralPatterns = detectBehavioralPatterns(documents)
            val storyConsistency = analyzeStoryConsistency(statements)
            val riskScore = calculateRiskScore(contradictions, behavioralPatterns, storyConsistency)
            val warnings = generateWarnings(contradictions, behavioralPatterns)

            BehavioralBrainResult.Success(
                entityId = entityId,
                contradictions = contradictions,
                behavioralPatterns = behavioralPatterns,
                storyConsistency = storyConsistency,
                riskScore = riskScore,
                warnings = warnings
            )
        } catch (e: Exception) {
            BehavioralBrainResult.Failure(
                error = "Analysis error: ${e.message}",
                errorCode = BehavioralErrorCode.PROCESSING_ERROR
            )
        }
    }

    /**
     * Document input for analysis.
     */
    data class DocumentInput(
        val id: String,
        val name: String,
        val content: String,
        val date: Date? = null,
        val type: String = "unknown"
    )

    /**
     * Internal statement representation.
     */
    private data class Statement(
        val id: String = UUID.randomUUID().toString(),
        val text: String,
        val documentId: String,
        val documentName: String,
        val date: Date?,
        val keywords: Set<String>,
        val hasNegation: Boolean,
        val sentenceIndex: Int
    )

    /**
     * Extract statements from documents.
     */
    private fun extractStatements(documents: List<DocumentInput>): List<Statement> {
        val statements = mutableListOf<Statement>()

        for (document in documents) {
            val sentences = splitIntoSentences(document.content)
            for ((index, sentence) in sentences.withIndex()) {
                val trimmed = sentence.trim()
                if (trimmed.length < 10) continue

                val keywords = extractKeywords(trimmed)
                val hasNegation = containsNegation(trimmed)

                statements.add(Statement(
                    text = trimmed,
                    documentId = document.id,
                    documentName = document.name,
                    date = document.date,
                    keywords = keywords,
                    hasNegation = hasNegation,
                    sentenceIndex = index
                ))
            }
        }

        return statements
    }

    /**
     * Split text into sentences.
     */
    private fun splitIntoSentences(text: String): List<String> {
        return text.split(Regex("[.!?\\n]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Extract keywords from a sentence.
     */
    private fun extractKeywords(text: String): Set<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "may", "might", "must", "shall", "can", "to", "of", "in", "for", "on", "with",
            "at", "by", "from", "as", "into", "through", "during", "before", "after",
            "above", "below", "between", "under", "again", "further", "then", "once",
            "here", "there", "when", "where", "why", "how", "all", "each", "few", "more",
            "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same",
            "so", "than", "too", "very", "just", "and", "but", "if", "or", "because",
            "until", "while", "this", "that", "these", "those", "it", "its", "i", "my", "me",
            "we", "you", "he", "she", "they", "them", "his", "her", "your", "our", "their"
        )

        return text.lowercase()
            .replace(Regex("[^a-z\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
    }

    /**
     * Check if sentence contains negation.
     */
    private fun containsNegation(text: String): Boolean {
        val words = text.lowercase().split(Regex("\\s+"))
        return words.any { it in NEGATION_WORDS }
    }

    /**
     * Detect contradictions between statements.
     */
    private fun detectContradictions(
        statements: List<Statement>,
        documents: List<DocumentInput>
    ): List<DocumentContradiction> {
        val contradictions = mutableListOf<DocumentContradiction>()

        for (i in statements.indices) {
            for (j in (i + 1) until statements.size) {
                val stmtA = statements[i]
                val stmtB = statements[j]

                // Skip statements from the same document unless they're far apart
                if (stmtA.documentId == stmtB.documentId &&
                    kotlin.math.abs(stmtA.sentenceIndex - stmtB.sentenceIndex) < 5) {
                    continue
                }

                val contradiction = checkContradiction(stmtA, stmtB)
                if (contradiction != null) {
                    contradictions.add(contradiction)
                }
            }
        }

        return contradictions.distinctBy { "${it.statementA}-${it.statementB}" }
    }

    /**
     * Check if two statements contradict each other.
     */
    private fun checkContradiction(stmtA: Statement, stmtB: Statement): DocumentContradiction? {
        // Check for common keywords
        val commonKeywords = stmtA.keywords.intersect(stmtB.keywords)
        if (commonKeywords.size < 2) return null

        // Check for negation mismatch
        if (stmtA.hasNegation != stmtB.hasNegation) {
            val type = determineContradictionType(stmtA, stmtB)
            val severity = determineSeverity(stmtA, stmtB, commonKeywords)

            return DocumentContradiction(
                documentA = stmtA.documentName,
                documentB = stmtB.documentName,
                statementA = stmtA.text,
                statementB = stmtB.text,
                type = type,
                severity = severity,
                explanation = "Statements about '${commonKeywords.take(3).joinToString(", ")}' have contradictory assertions",
                legalImplication = generateLegalImplication(type, severity)
            )
        }

        // Check for semantic contradiction using keyword patterns
        val textA = stmtA.text.lowercase()
        val textB = stmtB.text.lowercase()

        // Check for opposing terms
        val opposingPairs = listOf(
            "yes" to "no", "true" to "false", "agree" to "disagree",
            "accept" to "reject", "approve" to "deny", "confirm" to "deny",
            "sent" to "never sent", "paid" to "never paid", "received" to "never received"
        )

        for ((positive, negative) in opposingPairs) {
            if ((textA.contains(positive) && textB.contains(negative)) ||
                (textA.contains(negative) && textB.contains(positive))) {
                return DocumentContradiction(
                    documentA = stmtA.documentName,
                    documentB = stmtB.documentName,
                    statementA = stmtA.text,
                    statementB = stmtB.text,
                    type = ContradictionType.DIRECT,
                    severity = AnomalySeverity.HIGH,
                    explanation = "Direct contradiction detected: '$positive' vs '$negative'",
                    legalImplication = "Direct contradictions strongly indicate deceptive intent or unreliable testimony."
                )
            }
        }

        return null
    }

    /**
     * Determine the type of contradiction.
     */
    private fun determineContradictionType(stmtA: Statement, stmtB: Statement): ContradictionType {
        // Check if from different documents
        if (stmtA.documentId != stmtB.documentId) {
            return ContradictionType.DIRECT
        }

        // Check if temporal (different dates)
        if (stmtA.date != null && stmtB.date != null && stmtA.date != stmtB.date) {
            return ContradictionType.TEMPORAL
        }

        return ContradictionType.SEMANTIC
    }

    /**
     * Determine severity of contradiction.
     */
    private fun determineSeverity(
        stmtA: Statement,
        stmtB: Statement,
        commonKeywords: Set<String>
    ): AnomalySeverity {
        // High-impact keywords
        val criticalKeywords = setOf(
            "money", "payment", "paid", "signed", "agreed", "contract",
            "promised", "guarantee", "confirm", "deny", "admit"
        )

        val hasCriticalKeyword = commonKeywords.any { it in criticalKeywords }

        return when {
            hasCriticalKeyword && stmtA.documentId != stmtB.documentId -> AnomalySeverity.CRITICAL
            hasCriticalKeyword -> AnomalySeverity.HIGH
            commonKeywords.size >= 4 -> AnomalySeverity.HIGH
            commonKeywords.size >= 3 -> AnomalySeverity.MEDIUM
            else -> AnomalySeverity.LOW
        }
    }

    /**
     * Generate legal implication text.
     */
    private fun generateLegalImplication(type: ContradictionType, severity: AnomalySeverity): String {
        return when (type) {
            ContradictionType.DIRECT -> when (severity) {
                AnomalySeverity.CRITICAL -> "This direct contradiction is a critical indicator of potential perjury or fraud."
                AnomalySeverity.HIGH -> "This direct contradiction undermines credibility and suggests deliberate misrepresentation."
                else -> "This contradiction may indicate confusion or inconsistent record-keeping."
            }
            ContradictionType.TEMPORAL -> "Changing statements over time may indicate evolving narratives or attempted deception."
            ContradictionType.SEMANTIC -> "Semantic inconsistencies suggest unclear communication or potential evasion."
            ContradictionType.FACTUAL -> "Factual contradictions require verification against objective evidence."
            ContradictionType.OMISSION -> "Selective omission of information may constitute material misrepresentation."
        }
    }

    /**
     * Detect behavioral patterns in documents.
     */
    private fun detectBehavioralPatterns(documents: List<DocumentInput>): List<BehavioralPatternResult> {
        val patterns = mutableListOf<BehavioralPatternResult>()
        val allContent = documents.joinToString("\n") { it.content }
        val lowerContent = allContent.lowercase()

        // Check for gaslighting
        val gaslightingInstances = findPatternInstances(lowerContent, GASLIGHTING_PATTERNS)
        if (gaslightingInstances.isNotEmpty()) {
            patterns.add(BehavioralPatternResult(
                patternType = BehavioralPatternType.GASLIGHTING,
                instances = gaslightingInstances,
                frequency = gaslightingInstances.size,
                severity = calculatePatternSeverity(gaslightingInstances.size),
                confidence = calculateConfidence(gaslightingInstances.size, GASLIGHTING_PATTERNS.size)
            ))
        }

        // Check for deflection
        val deflectionInstances = findPatternInstances(lowerContent, DEFLECTION_PATTERNS)
        if (deflectionInstances.isNotEmpty()) {
            patterns.add(BehavioralPatternResult(
                patternType = BehavioralPatternType.DEFLECTION,
                instances = deflectionInstances,
                frequency = deflectionInstances.size,
                severity = calculatePatternSeverity(deflectionInstances.size),
                confidence = calculateConfidence(deflectionInstances.size, DEFLECTION_PATTERNS.size)
            ))
        }

        // Check for pressure tactics
        val pressureInstances = findPatternInstances(lowerContent, PRESSURE_PATTERNS)
        if (pressureInstances.isNotEmpty()) {
            patterns.add(BehavioralPatternResult(
                patternType = BehavioralPatternType.PRESSURE_TACTICS,
                instances = pressureInstances,
                frequency = pressureInstances.size,
                severity = calculatePatternSeverity(pressureInstances.size),
                confidence = calculateConfidence(pressureInstances.size, PRESSURE_PATTERNS.size)
            ))
        }

        // Check for manipulation
        val manipulationInstances = findPatternInstances(lowerContent, MANIPULATION_PATTERNS)
        if (manipulationInstances.isNotEmpty()) {
            patterns.add(BehavioralPatternResult(
                patternType = BehavioralPatternType.FINANCIAL_MANIPULATION,
                instances = manipulationInstances,
                frequency = manipulationInstances.size,
                severity = calculatePatternSeverity(manipulationInstances.size),
                confidence = calculateConfidence(manipulationInstances.size, MANIPULATION_PATTERNS.size)
            ))
        }

        // Check for admissions
        val admissionInstances = findPatternInstances(lowerContent, ADMISSION_PATTERNS)
        if (admissionInstances.isNotEmpty()) {
            patterns.add(BehavioralPatternResult(
                patternType = BehavioralPatternType.SLIP_UP_ADMISSION,
                instances = admissionInstances,
                frequency = admissionInstances.size,
                severity = AnomalySeverity.HIGH, // Admissions are always significant
                confidence = calculateConfidence(admissionInstances.size, ADMISSION_PATTERNS.size)
            ))
        }

        return patterns
    }

    /**
     * Find instances of pattern phrases in content.
     */
    private fun findPatternInstances(content: String, patterns: List<String>): List<String> {
        val instances = mutableListOf<String>()
        val sentences = splitIntoSentences(content)

        for (sentence in sentences) {
            val lowerSentence = sentence.lowercase()
            for (pattern in patterns) {
                if (lowerSentence.contains(pattern)) {
                    instances.add(sentence.take(200))
                    break
                }
            }
        }

        return instances.distinct().take(10)
    }

    /**
     * Calculate severity based on pattern frequency.
     */
    private fun calculatePatternSeverity(count: Int): AnomalySeverity {
        return when {
            count >= 5 -> AnomalySeverity.CRITICAL
            count >= 3 -> AnomalySeverity.HIGH
            count >= 2 -> AnomalySeverity.MEDIUM
            else -> AnomalySeverity.LOW
        }
    }

    /**
     * Calculate confidence score for pattern detection.
     */
    private fun calculateConfidence(foundCount: Int, totalPatterns: Int): Float {
        val baseConfidence = (foundCount.toFloat() / totalPatterns).coerceIn(0f, 1f)
        return (0.5f + baseConfidence * 0.5f) // Range: 0.5 to 1.0
    }

    /**
     * Analyze overall story consistency.
     */
    private fun analyzeStoryConsistency(statements: List<Statement>): StoryConsistency {
        val majorShifts = mutableListOf<StoryShift>()

        // Group statements by keywords to find narrative threads
        val keywordGroups = statements.groupBy { it.keywords.firstOrNull() ?: "" }
            .filter { it.key.isNotEmpty() && it.value.size > 1 }

        for ((keyword, stmts) in keywordGroups) {
            val sorted = stmts.sortedBy { it.date ?: Date(0) }
            for (i in 0 until sorted.size - 1) {
                val current = sorted[i]
                val next = sorted[i + 1]

                // Check for significant changes
                if (current.hasNegation != next.hasNegation) {
                    majorShifts.add(StoryShift(
                        date = next.date,
                        description = "Position on '$keyword' changed between documents",
                        fromStatement = current.text.take(100),
                        toStatement = next.text.take(100),
                        significance = AnomalySeverity.HIGH
                    ))
                }
            }
        }

        // Calculate consistency score
        val totalPossibleShifts = statements.size.coerceAtLeast(1)
        val consistencyScore = 1f - (majorShifts.size.toFloat() / totalPossibleShifts)

        // Count story versions (unique combinations of key assertions)
        val storyVersions = statements
            .map { "${it.keywords.sorted().take(3).joinToString(",")}-${it.hasNegation}" }
            .distinct()
            .size

        return StoryConsistency(
            overallConsistent = majorShifts.isEmpty(),
            consistencyScore = consistencyScore.coerceIn(0f, 1f),
            storyVersions = storyVersions,
            majorShifts = majorShifts
        )
    }

    /**
     * Calculate overall risk score.
     */
    private fun calculateRiskScore(
        contradictions: List<DocumentContradiction>,
        patterns: List<BehavioralPatternResult>,
        storyConsistency: StoryConsistency
    ): Float {
        var riskScore = 0f

        // Add risk from contradictions
        for (contradiction in contradictions) {
            riskScore += when (contradiction.severity) {
                AnomalySeverity.CRITICAL -> 25f
                AnomalySeverity.HIGH -> 15f
                AnomalySeverity.MEDIUM -> 10f
                AnomalySeverity.LOW -> 5f
                AnomalySeverity.INFO -> 2f
            }
        }

        // Add risk from behavioral patterns
        for (pattern in patterns) {
            riskScore += when (pattern.severity) {
                AnomalySeverity.CRITICAL -> 20f
                AnomalySeverity.HIGH -> 12f
                AnomalySeverity.MEDIUM -> 8f
                AnomalySeverity.LOW -> 4f
                AnomalySeverity.INFO -> 1f
            }
        }

        // Add risk from story inconsistency
        riskScore += (1f - storyConsistency.consistencyScore) * 20f

        return riskScore.coerceIn(0f, 100f)
    }

    /**
     * Generate warnings based on analysis.
     */
    private fun generateWarnings(
        contradictions: List<DocumentContradiction>,
        patterns: List<BehavioralPatternResult>
    ): List<String> {
        val warnings = mutableListOf<String>()

        val criticalContradictions = contradictions.count { it.severity == AnomalySeverity.CRITICAL }
        if (criticalContradictions > 0) {
            warnings.add("$criticalContradictions critical contradiction(s) detected - requires immediate attention")
        }

        val highPatterns = patterns.filter { it.severity in listOf(AnomalySeverity.CRITICAL, AnomalySeverity.HIGH) }
        for (pattern in highPatterns) {
            warnings.add("High-severity ${pattern.patternType.name.lowercase().replace("_", " ")} detected (${pattern.frequency} instances)")
        }

        if (patterns.any { it.patternType == BehavioralPatternType.GASLIGHTING }) {
            warnings.add("Gaslighting patterns detected - psychological manipulation indicators present")
        }

        if (patterns.any { it.patternType == BehavioralPatternType.SLIP_UP_ADMISSION }) {
            warnings.add("Potential admissions or slip-ups detected - review carefully")
        }

        return warnings
    }

    /**
     * Convert result to JSON string.
     */
    fun toJson(result: BehavioralBrainResult): String {
        return when (result) {
            is BehavioralBrainResult.Success -> buildSuccessJson(result)
            is BehavioralBrainResult.Failure -> buildFailureJson(result)
        }
    }

    private fun buildSuccessJson(result: BehavioralBrainResult.Success): String {
        val contradictionsJson = result.contradictions.joinToString(",") { c ->
            """{"id":"${c.id}","documentA":"${escapeJson(c.documentA)}","documentB":"${escapeJson(c.documentB)}","statementA":"${escapeJson(c.statementA.take(200))}","statementB":"${escapeJson(c.statementB.take(200))}","type":"${c.type}","severity":"${c.severity}","explanation":"${escapeJson(c.explanation)}","legalImplication":"${escapeJson(c.legalImplication)}"}"""
        }
        val patternsJson = result.behavioralPatterns.joinToString(",") { p ->
            val instancesJson = p.instances.joinToString(",") { "\"${escapeJson(it.take(100))}\"" }
            """{"patternType":"${p.patternType}","instances":[$instancesJson],"frequency":${p.frequency},"severity":"${p.severity}","confidence":${p.confidence}}"""
        }
        val shiftsJson = result.storyConsistency.majorShifts.joinToString(",") { s ->
            """{"date":${s.date?.time ?: "null"},"description":"${escapeJson(s.description)}","fromStatement":"${escapeJson(s.fromStatement.take(100))}","toStatement":"${escapeJson(s.toStatement.take(100))}","significance":"${s.significance}"}"""
        }
        val warningsJson = result.warnings.joinToString(",") { "\"${escapeJson(it)}\"" }

        return """
        {
            "success": true,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "analysisId": "${result.analysisId}",
            "entityId": "${result.entityId}",
            "contradictions": [$contradictionsJson],
            "behavioralPatterns": [$patternsJson],
            "storyConsistency": {
                "overallConsistent": ${result.storyConsistency.overallConsistent},
                "consistencyScore": ${result.storyConsistency.consistencyScore},
                "storyVersions": ${result.storyConsistency.storyVersions},
                "majorShifts": [$shiftsJson]
            },
            "riskScore": ${result.riskScore},
            "warnings": [$warningsJson]
        }
        """.trimIndent()
    }

    private fun buildFailureJson(result: BehavioralBrainResult.Failure): String {
        return """
        {
            "success": false,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "error": "${escapeJson(result.error)}",
            "errorCode": "${result.errorCode}"
        }
        """.trimIndent()
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

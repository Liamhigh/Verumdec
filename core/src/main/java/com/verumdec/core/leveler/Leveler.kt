package com.verumdec.core.leveler

import android.content.Context
import android.net.Uri
import com.verumdec.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

/**
 * Leveler - Unified Contradiction Engine
 * 
 * This is the new single source of truth for all contradiction detection and analysis.
 * It orchestrates the full forensic analysis pipeline using the new modular architecture:
 * - Core module for shared data models
 * - Entity module for entity discovery
 * - Timeline module for chronological analysis
 * - Analysis module for contradiction detection
 *
 * Replaces the old ContradictionEngine class.
 */
class Leveler(private val context: Context) {

    /**
     * Analysis progress listener for UI updates.
     */
    interface ProgressListener {
        fun onProgressUpdate(stage: AnalysisStage, progress: Int, message: String)
        fun onComplete(output: LevelerOutput)
        fun onError(error: String)
    }

    /**
     * Analysis stages for the Leveler pipeline.
     */
    enum class AnalysisStage {
        PROCESSING_DOCUMENTS,
        EXTRACTING_STATEMENTS,
        DISCOVERING_ENTITIES,
        BUILDING_TIMELINE,
        DETECTING_CONTRADICTIONS,
        ANALYZING_BEHAVIOR,
        CALCULATING_LIABILITY,
        GENERATING_REPORT,
        COMPLETE
    }

    private val statementIndex = StatementIndex()
    private var lastOutput: LevelerOutput? = null

    /**
     * Run the full Leveler analysis pipeline on a case file.
     *
     * @param caseFile The case to analyze
     * @param documentUris Map of document IDs to URIs
     * @param listener Progress listener for UI updates
     * @return LevelerOutput containing all analysis results
     */
    suspend fun run(
        caseFile: CaseFile,
        documentUris: Map<String, Uri>,
        listener: ProgressListener
    ): LevelerOutput = withContext(Dispatchers.IO) {
        try {
            val currentCase = caseFile

            // Stage 1: Process Documents
            listener.onProgressUpdate(AnalysisStage.PROCESSING_DOCUMENTS, 0, "Processing documents...")
            val processedDocuments = mutableListOf<ProcessedDocument>()
            val totalDocs = currentCase.documents.size

            for ((index, document) in currentCase.documents.withIndex()) {
                val uri = documentUris[document.id]
                val processed = processDocument(document, uri)
                processedDocuments.add(processed)

                val progress = ((index + 1) * 100 / totalDocs.coerceAtLeast(1))
                listener.onProgressUpdate(
                    AnalysisStage.PROCESSING_DOCUMENTS,
                    progress,
                    "Processed ${index + 1}/$totalDocs documents"
                )
            }

            // Stage 2: Extract Statements
            listener.onProgressUpdate(AnalysisStage.EXTRACTING_STATEMENTS, 0, "Extracting statements...")
            statementIndex.clear()
            val extractedStatements = extractStatements(processedDocuments)
            statementIndex.addStatements(extractedStatements)
            listener.onProgressUpdate(
                AnalysisStage.EXTRACTING_STATEMENTS,
                100,
                "Extracted ${extractedStatements.size} statements"
            )

            // Stage 3: Discover Entities
            listener.onProgressUpdate(AnalysisStage.DISCOVERING_ENTITIES, 0, "Discovering entities...")
            val speakerMap = buildSpeakerMap(extractedStatements)
            listener.onProgressUpdate(
                AnalysisStage.DISCOVERING_ENTITIES,
                100,
                "Found ${speakerMap.speakers.size} entities"
            )

            // Stage 4: Build Timeline
            listener.onProgressUpdate(AnalysisStage.BUILDING_TIMELINE, 0, "Building timeline...")
            val normalizedTimeline = buildNormalizedTimeline(extractedStatements)
            listener.onProgressUpdate(
                AnalysisStage.BUILDING_TIMELINE,
                100,
                "Generated ${normalizedTimeline.events.size} timeline events"
            )

            // Stage 5: Detect Contradictions
            listener.onProgressUpdate(AnalysisStage.DETECTING_CONTRADICTIONS, 0, "Detecting contradictions...")
            val contradictionSet = detectContradictions(extractedStatements, speakerMap, normalizedTimeline)
            listener.onProgressUpdate(
                AnalysisStage.DETECTING_CONTRADICTIONS,
                100,
                "Found ${contradictionSet.totalCount} contradictions"
            )

            // Stage 6: Analyze Behavior
            listener.onProgressUpdate(AnalysisStage.ANALYZING_BEHAVIOR, 0, "Analyzing behavioral patterns...")
            val behaviorShiftReport = analyzeBehavior(extractedStatements, speakerMap)
            listener.onProgressUpdate(
                AnalysisStage.ANALYZING_BEHAVIOR,
                100,
                "Detected ${behaviorShiftReport.shifts.size} behavioral shifts"
            )

            // Stage 7: Calculate Liability
            listener.onProgressUpdate(AnalysisStage.CALCULATING_LIABILITY, 0, "Calculating liability scores...")
            val liabilityScores = calculateLiabilityScores(speakerMap, contradictionSet, behaviorShiftReport)
            listener.onProgressUpdate(
                AnalysisStage.CALCULATING_LIABILITY,
                100,
                "Calculated scores for ${liabilityScores.size} entities"
            )

            // Stage 8: Generate Report
            listener.onProgressUpdate(AnalysisStage.GENERATING_REPORT, 0, "Generating report...")
            val extractionSummary = ExtractionSummary(
                totalDocuments = processedDocuments.size,
                processedDocuments = processedDocuments.count { it.processed },
                totalStatements = extractedStatements.size,
                speakersIdentified = speakerMap.speakers.size,
                timelineEvents = normalizedTimeline.events.size,
                processingDurationMs = System.currentTimeMillis() - currentCase.createdAt.time
            )

            val output = LevelerOutput(
                caseId = currentCase.id,
                caseName = currentCase.name,
                generatedAt = Date(),
                speakerMap = speakerMap,
                normalizedTimeline = normalizedTimeline,
                contradictionSet = contradictionSet,
                behaviorShiftReport = behaviorShiftReport,
                liabilityScores = liabilityScores,
                extractionSummary = extractionSummary,
                processedDocuments = processedDocuments,
                engineVersion = "2.0.0-leveler"
            )

            lastOutput = output

            // Complete
            listener.onProgressUpdate(AnalysisStage.COMPLETE, 100, "Analysis complete!")
            listener.onComplete(output)

            output
        } catch (e: Exception) {
            listener.onError(e.message ?: "Unknown error occurred")
            throw e
        }
    }

    /**
     * Get the last analysis output.
     */
    fun getLastOutput(): LevelerOutput? = lastOutput

    /**
     * Get the statement index for direct access.
     */
    fun getStatementIndex(): StatementIndex = statementIndex

    /**
     * Reset the engine for a new analysis.
     */
    fun reset() {
        statementIndex.clear()
        lastOutput = null
    }

    /**
     * Process a single document.
     */
    private fun processDocument(document: DocumentInfo, uri: Uri?): ProcessedDocument {
        // Simple text extraction - in production this would use PDF/OCR processors
        val extractedText = if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    // For now, just read as text - PDF processing would go here
                    stream.bufferedReader().readText().take(50000)
                } ?: ""
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }

        val metadata = DocumentMetadata(
            creationDate = document.addedAt,
            author = null,
            pageCount = 1
        )

        return ProcessedDocument(
            id = document.id,
            fileName = document.fileName,
            documentType = document.type,
            extractedText = extractedText,
            metadata = metadata,
            processed = extractedText.isNotEmpty()
        )
    }

    /**
     * Extract statements from processed documents.
     */
    private fun extractStatements(documents: List<ProcessedDocument>): List<Statement> {
        val statements = mutableListOf<Statement>()

        for (document in documents) {
            if (document.extractedText.isEmpty()) continue

            val sentences = document.extractedText.split(Regex("[.!?\\n]"))
                .filter { it.trim().length > 10 }

            var lineNumber = 0
            for (sentence in sentences) {
                lineNumber++
                val trimmed = sentence.trim()
                if (trimmed.length < 10) continue

                // Extract speaker from chat format or use document as speaker
                val (speaker, text) = extractSpeakerAndText(trimmed, document.fileName)

                val statement = Statement(
                    speaker = speaker,
                    text = text,
                    documentId = document.id,
                    documentName = document.fileName,
                    lineNumber = lineNumber,
                    timestamp = document.metadata.creationDate?.time,
                    sentiment = calculateSentiment(text),
                    certainty = calculateCertainty(text),
                    legalCategory = classifyLegalCategory(text)
                )
                statements.add(statement)
            }
        }

        return statements
    }

    /**
     * Extract speaker from text if in chat format.
     */
    private fun extractSpeakerAndText(text: String, defaultSpeaker: String): Pair<String, String> {
        val chatMatch = Regex("^([^:]+):\\s*(.+)").find(text)
        return if (chatMatch != null) {
            Pair(chatMatch.groupValues[1].trim(), chatMatch.groupValues[2].trim())
        } else {
            Pair(defaultSpeaker, text)
        }
    }

    /**
     * Build speaker map from statements.
     */
    private fun buildSpeakerMap(statements: List<Statement>): SpeakerMap {
        val speakerProfiles = mutableMapOf<String, SpeakerProfile>()

        val statementsBySpeaker = statements.groupBy { it.speaker }

        for ((speaker, speakerStatements) in statementsBySpeaker) {
            val profile = SpeakerProfile(
                id = UUID.randomUUID().toString(),
                name = speaker,
                aliases = mutableListOf(),
                statementCount = speakerStatements.size,
                firstAppearance = speakerStatements.minOfOrNull { it.timestamp ?: Long.MAX_VALUE },
                lastAppearance = speakerStatements.maxOfOrNull { it.timestamp ?: 0L },
                documentIds = speakerStatements.map { it.documentId }.distinct(),
                averageSentiment = speakerStatements.map { it.sentiment }.average(),
                averageCertainty = speakerStatements.map { it.certainty }.average()
            )
            speakerProfiles[speaker] = profile
        }

        return SpeakerMap(
            speakers = speakerProfiles,
            totalSpeakers = speakerProfiles.size,
            crossDocumentSpeakers = speakerProfiles.values.count { it.documentIds.size > 1 }
        )
    }

    /**
     * Build normalized timeline from statements.
     */
    private fun buildNormalizedTimeline(statements: List<Statement>): NormalizedTimeline {
        val events = statements
            .filter { it.timestamp != null }
            .map { statement ->
                TimelineEventEntry(
                    id = UUID.randomUUID().toString(),
                    timestamp = statement.timestamp!!,
                    description = statement.text.take(200),
                    speaker = statement.speaker,
                    documentId = statement.documentId,
                    statementId = statement.id,
                    eventType = classifyEventType(statement.text),
                    confidence = 1.0
                )
            }
            .sortedBy { it.timestamp }

        return NormalizedTimeline(
            events = events,
            totalEvents = events.size,
            normalizationMode = TimestampNormalizationMode.DOCUMENT_RELATIVE,
            earliestTimestamp = events.minOfOrNull { it.timestamp },
            latestTimestamp = events.maxOfOrNull { it.timestamp }
        )
    }

    /**
     * Detect contradictions across all statements.
     */
    private fun detectContradictions(
        statements: List<Statement>,
        speakerMap: SpeakerMap,
        timeline: NormalizedTimeline
    ): ContradictionSet {
        val contradictions = mutableListOf<ContradictionEntry>()
        val contradictionTypes = mutableMapOf<String, Int>()

        // Compare statements for contradictions
        for (i in statements.indices) {
            for (j in i + 1 until statements.size) {
                val stmtA = statements[i]
                val stmtB = statements[j]

                val contradiction = checkForContradiction(stmtA, stmtB)
                if (contradiction != null) {
                    contradictions.add(contradiction)
                    val typeName = contradiction.type.name
                    contradictionTypes[typeName] = (contradictionTypes[typeName] ?: 0) + 1
                }
            }
        }

        // Group by severity
        val bySeverity = contradictions.groupBy {
            when {
                it.severity >= 9 -> "critical"
                it.severity >= 7 -> "high"
                it.severity >= 5 -> "medium"
                else -> "low"
            }
        }

        return ContradictionSet(
            contradictions = contradictions,
            totalCount = contradictions.size,
            criticalCount = bySeverity["critical"]?.size ?: 0,
            highCount = bySeverity["high"]?.size ?: 0,
            mediumCount = bySeverity["medium"]?.size ?: 0,
            lowCount = bySeverity["low"]?.size ?: 0,
            contradictionTypes = contradictionTypes
        )
    }

    /**
     * Check if two statements contradict each other.
     */
    private fun checkForContradiction(stmtA: Statement, stmtB: Statement): ContradictionEntry? {
        val textA = stmtA.text.lowercase()
        val textB = stmtB.text.lowercase()

        // Check for negation patterns
        val negationPairs = listOf(
            "did" to "did not",
            "did" to "didn't",
            "was" to "was not",
            "was" to "wasn't",
            "have" to "have not",
            "have" to "haven't",
            "paid" to "never paid",
            "agreed" to "never agreed",
            "true" to "false",
            "yes" to "no"
        )

        for ((positive, negative) in negationPairs) {
            if ((textA.contains(positive) && textB.contains(negative)) ||
                (textA.contains(negative) && textB.contains(positive))) {
                
                // Check for topic similarity
                val wordsA = textA.split(Regex("\\W+")).filter { it.length > 3 }.toSet()
                val wordsB = textB.split(Regex("\\W+")).filter { it.length > 3 }.toSet()
                val commonWords = wordsA.intersect(wordsB)
                
                if (commonWords.size >= 2) {
                    val type = if (stmtA.speaker == stmtB.speaker) {
                        ContradictionType.DIRECT
                    } else if (stmtA.documentId != stmtB.documentId) {
                        ContradictionType.CROSS_DOCUMENT
                    } else {
                        ContradictionType.SEMANTIC
                    }

                    val severity = when (type) {
                        ContradictionType.DIRECT -> 8
                        ContradictionType.CROSS_DOCUMENT -> 7
                        else -> 5
                    }

                    return ContradictionEntry(
                        id = UUID.randomUUID().toString(),
                        type = type,
                        sourceStatementId = stmtA.id,
                        targetStatementId = stmtB.id,
                        sourceText = stmtA.text.take(100),
                        targetText = stmtB.text.take(100),
                        sourceSpeaker = stmtA.speaker,
                        targetSpeaker = stmtB.speaker,
                        sourceDocument = stmtA.documentId,
                        targetDocument = stmtB.documentId,
                        severity = severity,
                        description = "Contradictory statements about: ${commonWords.take(3).joinToString(", ")}",
                        legalTrigger = LegalTrigger.MISREPRESENTATION.name
                    )
                }
            }
        }

        return null
    }

    /**
     * Analyze behavioral patterns in statements.
     */
    private fun analyzeBehavior(
        statements: List<Statement>,
        speakerMap: SpeakerMap
    ): BehaviorShiftReport {
        val shifts = mutableListOf<BehaviorShift>()

        val statementsBySpeaker = statements.groupBy { it.speaker }

        for ((speaker, speakerStatements) in statementsBySpeaker) {
            if (speakerStatements.size < 2) continue

            val sorted = speakerStatements.sortedBy { it.timestamp ?: 0L }

            // Check for sentiment shifts
            for (i in 1 until sorted.size) {
                val prev = sorted[i - 1]
                val curr = sorted[i]

                val sentimentChange = curr.sentiment - prev.sentiment
                if (kotlin.math.abs(sentimentChange) > 0.5) {
                    shifts.add(
                        BehaviorShift(
                            id = UUID.randomUUID().toString(),
                            speakerId = speaker,
                            shiftType = if (sentimentChange < 0) ShiftType.TONE_SHIFT_NEGATIVE else ShiftType.TONE_SHIFT_POSITIVE,
                            beforeStatementId = prev.id,
                            afterStatementId = curr.id,
                            beforeState = describeSentiment(prev.sentiment),
                            afterState = describeSentiment(curr.sentiment),
                            severity = calculateShiftSeverity(sentimentChange),
                            description = "$speaker's tone shifted from ${describeSentiment(prev.sentiment)} to ${describeSentiment(curr.sentiment)}"
                        )
                    )
                }

                // Check for certainty decline
                val certaintyChange = prev.certainty - curr.certainty
                if (certaintyChange > 0.3) {
                    shifts.add(
                        BehaviorShift(
                            id = UUID.randomUUID().toString(),
                            speakerId = speaker,
                            shiftType = ShiftType.CERTAINTY_DECLINE,
                            beforeStatementId = prev.id,
                            afterStatementId = curr.id,
                            beforeState = describeCertainty(prev.certainty),
                            afterState = describeCertainty(curr.certainty),
                            severity = calculateDeclineSeverity(certaintyChange),
                            description = "$speaker's certainty declined from ${describeCertainty(prev.certainty)} to ${describeCertainty(curr.certainty)}"
                        )
                    )
                }
            }

            // Check for behavioral patterns
            shifts.addAll(detectBehavioralPatterns(speaker, sorted))
        }

        return BehaviorShiftReport(
            shifts = shifts,
            totalShifts = shifts.size,
            affectedSpeakers = shifts.map { it.speakerId }.distinct(),
            patternBreakdown = shifts.groupBy { it.shiftType.name }.mapValues { it.value.size }
        )
    }

    /**
     * Detect behavioral patterns in statements.
     */
    private fun detectBehavioralPatterns(speaker: String, statements: List<Statement>): List<BehaviorShift> {
        val patterns = mutableListOf<BehaviorShift>()

        val gaslightingKeywords = listOf("you're imagining", "that never happened", "you're confused", "you're crazy")
        val deflectionKeywords = listOf("but", "however", "that's not the point", "you should ask")
        val overExplainingKeywords = listOf("let me explain", "the reason is", "you see", "to clarify")

        for (statement in statements) {
            val text = statement.text.lowercase()

            // Check for gaslighting
            if (gaslightingKeywords.any { text.contains(it) }) {
                patterns.add(
                    BehaviorShift(
                        id = UUID.randomUUID().toString(),
                        speakerId = speaker,
                        shiftType = ShiftType.GASLIGHTING,
                        beforeStatementId = statement.id,
                        afterStatementId = statement.id,
                        beforeState = "neutral",
                        afterState = "gaslighting",
                        severity = 8,
                        description = "$speaker uses gaslighting language"
                    )
                )
            }

            // Check for deflection
            val deflectionCount = deflectionKeywords.count { text.contains(it) }
            if (deflectionCount >= 2) {
                patterns.add(
                    BehaviorShift(
                        id = UUID.randomUUID().toString(),
                        speakerId = speaker,
                        shiftType = ShiftType.DEFLECTION,
                        beforeStatementId = statement.id,
                        afterStatementId = statement.id,
                        beforeState = "direct",
                        afterState = "deflecting",
                        severity = 5,
                        description = "$speaker shows deflection behavior"
                    )
                )
            }

            // Check for over-explaining
            val overExplainingCount = overExplainingKeywords.count { text.contains(it) }
            if (overExplainingCount >= 2 || statement.text.length > 500) {
                patterns.add(
                    BehaviorShift(
                        id = UUID.randomUUID().toString(),
                        speakerId = speaker,
                        shiftType = ShiftType.OVER_EXPLAINING,
                        beforeStatementId = statement.id,
                        afterStatementId = statement.id,
                        beforeState = "concise",
                        afterState = "over-explaining",
                        severity = 6,
                        description = "$speaker shows over-explaining pattern (potential fraud indicator)"
                    )
                )
            }
        }

        return patterns
    }

    /**
     * Calculate liability scores for each speaker.
     */
    private fun calculateLiabilityScores(
        speakerMap: SpeakerMap,
        contradictionSet: ContradictionSet,
        behaviorReport: BehaviorShiftReport
    ): Map<String, LiabilityScoreEntry> {
        val scores = mutableMapOf<String, LiabilityScoreEntry>()

        for ((speakerId, profile) in speakerMap.speakers) {
            // Count contradictions involving this speaker
            val speakerContradictions = contradictionSet.contradictions.count {
                it.sourceSpeaker == speakerId || it.targetSpeaker == speakerId
            }

            val avgContradictionSeverity = contradictionSet.contradictions
                .filter { it.sourceSpeaker == speakerId || it.targetSpeaker == speakerId }
                .map { it.severity }
                .average()
                .let { if (it.isNaN()) 0.0 else it }

            // Count behavioral shifts for this speaker
            val speakerShifts = behaviorReport.shifts.count { it.speakerId == speakerId }

            val avgShiftSeverity = behaviorReport.shifts
                .filter { it.speakerId == speakerId }
                .map { it.severity }
                .average()
                .let { if (it.isNaN()) 0.0 else it }

            // Calculate component scores
            val contradictionScore = (speakerContradictions * 5 + avgContradictionSeverity * 3).toFloat().coerceIn(0f, 40f)
            val behavioralScore = (speakerShifts * 4 + avgShiftSeverity * 2).toFloat().coerceIn(0f, 30f)
            val consistencyScore = ((1 - profile.averageCertainty) * 15).toFloat().coerceIn(0f, 15f)
            val evidenceScore = (profile.statementCount / 10.0 * 5).toFloat().coerceIn(0f, 15f)

            val overallScore = (contradictionScore + behavioralScore + consistencyScore + evidenceScore).coerceIn(0f, 100f)

            scores[speakerId] = LiabilityScoreEntry(
                entityId = speakerId,
                entityName = profile.name,
                overallScore = overallScore,
                contradictionScore = contradictionScore,
                behavioralScore = behavioralScore,
                consistencyScore = consistencyScore,
                evidenceContributionScore = evidenceScore,
                contradictionCount = speakerContradictions,
                behavioralShiftCount = speakerShifts,
                breakdown = LiabilityBreakdownEntry(
                    totalContradictions = speakerContradictions,
                    criticalContradictions = contradictionSet.contradictions.count {
                        (it.sourceSpeaker == speakerId || it.targetSpeaker == speakerId) && it.severity >= 9
                    },
                    behavioralFlags = behaviorReport.shifts.filter { it.speakerId == speakerId }.map { it.shiftType.name },
                    storyChanges = speakerShifts
                )
            )
        }

        return scores
    }

    /**
     * Calculate simple sentiment from text.
     */
    private fun calculateSentiment(text: String): Double {
        val lower = text.lowercase()
        val positiveWords = listOf("good", "great", "excellent", "happy", "agree", "yes", "correct", "true")
        val negativeWords = listOf("bad", "terrible", "wrong", "disagree", "no", "false", "deny", "never", "not")

        val positiveCount = positiveWords.count { lower.contains(it) }
        val negativeCount = negativeWords.count { lower.contains(it) }

        return if (positiveCount + negativeCount > 0) {
            (positiveCount - negativeCount).toDouble() / (positiveCount + negativeCount)
        } else 0.0
    }

    /**
     * Calculate certainty from text.
     */
    private fun calculateCertainty(text: String): Double {
        val lower = text.lowercase()
        val certainWords = listOf("definitely", "certainly", "absolutely", "clearly", "always", "never")
        val uncertainWords = listOf("maybe", "perhaps", "possibly", "might", "could", "uncertain", "think")

        val certainCount = certainWords.count { lower.contains(it) }
        val uncertainCount = uncertainWords.count { lower.contains(it) }

        val base = 0.5
        val adjustment = (certainCount - uncertainCount) * 0.1
        return (base + adjustment).coerceIn(0.0, 1.0)
    }

    /**
     * Classify legal category from text.
     */
    private fun classifyLegalCategory(text: String): LegalCategory {
        val lower = text.lowercase()
        return when {
            lower.contains("admit") || lower.contains("confession") -> LegalCategory.ADMISSION
            lower.contains("deny") || lower.contains("never") || lower.contains("didn't") -> LegalCategory.DENIAL
            lower.contains("promise") || lower.contains("will") || lower.contains("shall") -> LegalCategory.PROMISE
            lower.contains("paid") || lower.contains("payment") || lower.contains("$") -> LegalCategory.FINANCIAL
            lower.contains("contract") || lower.contains("agreement") -> LegalCategory.CONTRACTUAL
            else -> LegalCategory.GENERAL
        }
    }

    /**
     * Classify event type from text.
     */
    private fun classifyEventType(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("paid") || lower.contains("payment") -> "PAYMENT"
            lower.contains("promised") || lower.contains("will") -> "PROMISE"
            lower.contains("agreed") || lower.contains("contract") -> "AGREEMENT"
            lower.contains("said") || lower.contains("stated") -> "STATEMENT"
            else -> "OTHER"
        }
    }

    private fun describeSentiment(sentiment: Double): String {
        return when {
            sentiment > 0.5 -> "positive"
            sentiment > 0.1 -> "slightly positive"
            sentiment > -0.1 -> "neutral"
            sentiment > -0.5 -> "slightly negative"
            else -> "negative"
        }
    }

    private fun describeCertainty(certainty: Double): String {
        return when {
            certainty > 0.8 -> "very certain"
            certainty > 0.6 -> "moderately certain"
            certainty > 0.4 -> "somewhat uncertain"
            else -> "uncertain"
        }
    }

    private fun calculateShiftSeverity(shift: Double): Int {
        return when {
            kotlin.math.abs(shift) > 1.5 -> 9
            kotlin.math.abs(shift) > 1.0 -> 7
            kotlin.math.abs(shift) > 0.5 -> 5
            else -> 3
        }
    }

    private fun calculateDeclineSeverity(decline: Double): Int {
        return when {
            decline > 0.7 -> 8
            decline > 0.5 -> 6
            decline > 0.3 -> 4
            else -> 2
        }
    }
}

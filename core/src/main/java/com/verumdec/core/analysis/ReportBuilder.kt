package com.verumdec.core.analysis

import com.verumdec.core.extraction.SpeakerExtractor
import com.verumdec.core.extraction.StatementExtractor
import com.verumdec.core.model.BehavioralAnomaly
import com.verumdec.core.model.Contradiction
import com.verumdec.core.model.ContradictionReport
import com.verumdec.core.model.Statement
import com.verumdec.core.model.TimelineEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * ReportBuilder is the entry point for generating forensic reports from raw text.
 * 
 * This is the main interface for the Verum Omnis contradiction engine.
 * It orchestrates the full analysis pipeline:
 * 1. Text extraction and normalization
 * 2. Speaker/actor identification
 * 3. Statement extraction
 * 4. Timeline construction
 * 5. Contradiction detection
 * 6. Narrative generation
 * 7. Report assembly
 */
class ReportBuilder {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    /**
     * Result of the report generation process.
     */
    data class ReportResult(
        val narrative: String,
        val contradictionsCount: Int,
        val behaviourShiftsCount: Int,
        val actorsCount: Int,
        val report: ContradictionReport?,
        val statements: List<Statement>,
        val timeline: List<TimelineEvent>,
        val actors: Set<String>,
        val contradictions: List<Contradiction>,
        val behaviourShifts: List<BehavioralAnomaly>,
        val generatedAt: String
    )

    /**
     * Generate a complete forensic report from raw text.
     * This is the main entry point for the contradiction engine.
     *
     * @param rawText The raw document text to analyze
     * @param documentId Optional document identifier
     * @param documentName Optional document name
     * @param documentAuthor Optional document author
     * @return ReportResult containing all analysis outputs
     */
    fun generate(
        rawText: String,
        documentId: String = UUID.randomUUID().toString(),
        documentName: String = "Document",
        documentAuthor: String? = null
    ): ReportResult {
        // Step 1: Extract statements from raw text
        val statements = StatementExtractor.extractStatements(
            text = rawText,
            documentId = documentId,
            documentName = documentName,
            documentAuthor = documentAuthor
        )

        // Step 2: Identify actors (speakers)
        val actors = SpeakerExtractor.extractUniqueSpeakers(rawText, documentAuthor)

        // Step 3: Build timeline
        val timeline = TimelineBuilder.build(statements)

        // Step 4: Detect contradictions using StatementComparer
        val contradictions = StatementComparer.compareAllStatements(statements)

        // Step 5: Detect behaviour shifts
        val behaviourShifts = detectBehaviourShifts(statements, actors)

        // Step 6: Generate narrative
        val narrative = generateNarrative(
            statements = statements,
            timeline = timeline,
            contradictions = contradictions,
            behaviourShifts = behaviourShifts,
            actors = actors
        )

        // Step 7: Build the contradiction report
        val report = buildContradictionReport(
            caseId = documentId,
            contradictions = contradictions,
            behaviourShifts = behaviourShifts,
            actors = actors
        )

        return ReportResult(
            narrative = narrative,
            contradictionsCount = contradictions.size,
            behaviourShiftsCount = behaviourShifts.size,
            actorsCount = actors.size,
            report = report,
            statements = statements,
            timeline = timeline,
            actors = actors,
            contradictions = contradictions,
            behaviourShifts = behaviourShifts,
            generatedAt = dateFormat.format(Date())
        )
    }

    /**
     * Generate a report from multiple documents.
     *
     * @param documents Map of document ID to (document name, document text, optional author)
     * @return Combined ReportResult
     */
    fun generateFromMultipleDocuments(
        documents: Map<String, Triple<String, String, String?>>
    ): ReportResult {
        // Combine all document texts
        val allStatements = mutableListOf<Statement>()
        val allActors = mutableSetOf<String>()

        for ((docId, docData) in documents) {
            val (docName, docText, docAuthor) = docData
            
            val statements = StatementExtractor.extractStatements(
                text = docText,
                documentId = docId,
                documentName = docName,
                documentAuthor = docAuthor
            )
            allStatements.addAll(statements)

            val actors = SpeakerExtractor.extractUniqueSpeakers(docText, docAuthor)
            allActors.addAll(actors)
        }

        // Build combined timeline
        val timeline = TimelineBuilder.build(allStatements)

        // Detect contradictions (including cross-document)
        val contradictions = StatementComparer.compareAllStatements(allStatements)

        // Detect behaviour shifts
        val behaviourShifts = detectBehaviourShifts(allStatements, allActors)

        // Generate narrative
        val narrative = generateNarrative(
            statements = allStatements,
            timeline = timeline,
            contradictions = contradictions,
            behaviourShifts = behaviourShifts,
            actors = allActors
        )

        // Build report
        val caseId = UUID.randomUUID().toString()
        val report = buildContradictionReport(
            caseId = caseId,
            contradictions = contradictions,
            behaviourShifts = behaviourShifts,
            actors = allActors
        )

        return ReportResult(
            narrative = narrative,
            contradictionsCount = contradictions.size,
            behaviourShiftsCount = behaviourShifts.size,
            actorsCount = allActors.size,
            report = report,
            statements = allStatements,
            timeline = timeline,
            actors = allActors,
            contradictions = contradictions,
            behaviourShifts = behaviourShifts,
            generatedAt = dateFormat.format(Date())
        )
    }

    /**
     * Detect behaviour shifts for each actor.
     */
    private fun detectBehaviourShifts(
        statements: List<Statement>,
        actors: Set<String>
    ): List<BehavioralAnomaly> {
        val shifts = mutableListOf<BehavioralAnomaly>()

        for (actor in actors) {
            val actorStatements = statements
                .filter { it.speaker.equals(actor, ignoreCase = true) }
                .sortedBy { it.timestampMillis ?: 0L }

            if (actorStatements.size < 2) continue

            // Detect sentiment shifts
            for (i in 1 until actorStatements.size) {
                val prev = actorStatements[i - 1]
                val curr = actorStatements[i]

                val sentimentShift = curr.sentiment - prev.sentiment
                val certaintyShift = curr.certainty - prev.certainty

                // Significant negative sentiment shift
                if (sentimentShift < -0.5) {
                    shifts.add(
                        BehavioralAnomaly(
                            id = UUID.randomUUID().toString(),
                            entityId = actor,
                            type = com.verumdec.core.model.BehavioralAnomalyType.TONE_SHIFT,
                            description = "$actor's tone shifted from ${describeSentiment(prev.sentiment)} to ${describeSentiment(curr.sentiment)}",
                            severity = calculateShiftSeverity(sentimentShift),
                            statementIds = listOf(prev.id, curr.id),
                            beforeState = describeSentiment(prev.sentiment),
                            afterState = describeSentiment(curr.sentiment)
                        )
                    )
                }

                // Significant certainty decline
                if (certaintyShift < -0.3) {
                    shifts.add(
                        BehavioralAnomaly(
                            id = UUID.randomUUID().toString(),
                            entityId = actor,
                            type = com.verumdec.core.model.BehavioralAnomalyType.CONFIDENCE_DECLINE,
                            description = "$actor's confidence declined from ${describeCertainty(prev.certainty)} to ${describeCertainty(curr.certainty)}",
                            severity = calculateShiftSeverity(certaintyShift),
                            statementIds = listOf(prev.id, curr.id),
                            beforeState = describeCertainty(prev.certainty),
                            afterState = describeCertainty(curr.certainty)
                        )
                    )
                }

                // Position change (denial after admission)
                if (StatementExtractor.isPositionChange(curr.text, prev.text)) {
                    shifts.add(
                        BehavioralAnomaly(
                            id = UUID.randomUUID().toString(),
                            entityId = actor,
                            type = com.verumdec.core.model.BehavioralAnomalyType.SUDDEN_DENIAL,
                            description = "$actor changed their position between statements",
                            severity = 8,
                            statementIds = listOf(prev.id, curr.id),
                            beforeState = "previous claim",
                            afterState = "contradicting position"
                        )
                    )
                }
            }
        }

        return shifts
    }

    /**
     * Generate the narrative summary.
     */
    private fun generateNarrative(
        statements: List<Statement>,
        timeline: List<TimelineEvent>,
        contradictions: List<Contradiction>,
        behaviourShifts: List<BehavioralAnomaly>,
        actors: Set<String>
    ): String {
        val builder = StringBuilder()

        builder.appendLine("FORENSIC ANALYSIS SUMMARY")
        builder.appendLine("=".repeat(50))
        builder.appendLine()

        // Statistics
        builder.appendLine("Analysis Statistics:")
        builder.appendLine("  - Total statements analyzed: ${statements.size}")
        builder.appendLine("  - Actors identified: ${actors.size}")
        builder.appendLine("  - Timeline events: ${timeline.size}")
        builder.appendLine("  - Contradictions detected: ${contradictions.size}")
        builder.appendLine("  - Behaviour shifts detected: ${behaviourShifts.size}")
        builder.appendLine()

        // Actors summary
        if (actors.isNotEmpty()) {
            builder.appendLine("Actors Identified:")
            actors.forEach { actor ->
                val actorStatements = statements.count { it.speaker.equals(actor, ignoreCase = true) }
                val actorContradictions = contradictions.count { it.affectedEntities.contains(actor) }
                builder.appendLine("  - $actor: $actorStatements statements, $actorContradictions contradictions")
            }
            builder.appendLine()
        }

        // Contradictions summary
        if (contradictions.isNotEmpty()) {
            builder.appendLine("Contradictions Summary:")
            val bySeverity = contradictions.groupBy { 
                when {
                    it.severity >= 8 -> "CRITICAL"
                    it.severity >= 6 -> "HIGH"
                    it.severity >= 4 -> "MEDIUM"
                    else -> "LOW"
                }
            }
            bySeverity.forEach { (level, list) ->
                builder.appendLine("  - $level: ${list.size}")
            }
            builder.appendLine()

            // Top contradictions
            builder.appendLine("Top Contradictions:")
            contradictions.sortedByDescending { it.severity }.take(5).forEach { c ->
                builder.appendLine("  [Severity ${c.severity}] ${c.description.take(100)}...")
            }
            builder.appendLine()
        }

        // Behaviour shifts summary
        if (behaviourShifts.isNotEmpty()) {
            builder.appendLine("Behaviour Shifts Detected:")
            behaviourShifts.take(5).forEach { shift ->
                builder.appendLine("  - ${shift.description}")
            }
            builder.appendLine()
        }

        // Timeline summary
        if (timeline.isNotEmpty()) {
            val dateRange = TimelineBuilder.getDateRange(timeline)
            if (dateRange != null) {
                builder.appendLine("Timeline Range: ${dateRange.first} to ${dateRange.second}")
            }
            
            val gaps = TimelineBuilder.findGaps(timeline, minGapDays = 30)
            if (gaps.isNotEmpty()) {
                builder.appendLine("Timeline Gaps (>30 days):")
                gaps.take(3).forEach { (start, end, days) ->
                    builder.appendLine("  - $start to $end ($days days)")
                }
            }
        }

        builder.appendLine()
        builder.appendLine("-".repeat(50))
        builder.appendLine("Generated by Verum Omnis Contradiction Engine")

        return builder.toString()
    }

    /**
     * Build the full ContradictionReport.
     */
    private fun buildContradictionReport(
        caseId: String,
        contradictions: List<Contradiction>,
        behaviourShifts: List<BehavioralAnomaly>,
        actors: Set<String>
    ): ContradictionReport {
        return ContradictionReport(
            caseId = caseId,
            totalContradictions = contradictions.size,
            contradictions = contradictions,
            timelineConflicts = emptyList(), // Would be populated by TimelineComparer
            behavioralAnomalies = behaviourShifts,
            affectedEntities = buildAffectedEntities(contradictions, actors),
            documentLinks = buildDocumentLinks(contradictions),
            severityBreakdown = contradictions.groupingBy { it.severity }.eachCount(),
            legalTriggers = buildLegalTriggers(contradictions),
            summary = "Analysis complete. Found ${contradictions.size} contradictions and ${behaviourShifts.size} behaviour shifts.",
            verificationStatus = com.verumdec.core.model.VerificationStatus(
                statementsIndexed = true,
                embeddingsGenerated = false,
                timelineObjectsExist = true,
                entityProfilesExist = true,
                contradictionPassRan = true,
                warnings = emptyList(),
                autoCorrections = emptyList()
            )
        )
    }

    private fun buildAffectedEntities(
        contradictions: List<Contradiction>,
        actors: Set<String>
    ): Map<String, com.verumdec.core.model.EntityInvolvement> {
        val result = mutableMapOf<String, com.verumdec.core.model.EntityInvolvement>()

        for (actor in actors) {
            val actorContradictions = contradictions.filter { it.affectedEntities.contains(actor) }
            result[actor] = com.verumdec.core.model.EntityInvolvement(
                entityId = actor,
                entityName = actor,
                contradictionCount = actorContradictions.size,
                contradictionIds = actorContradictions.map { it.id },
                liabilityScore = calculateLiabilityScore(actorContradictions),
                primaryRole = "Subject"
            )
        }

        return result
    }

    private fun buildDocumentLinks(contradictions: List<Contradiction>): Map<String, List<String>> {
        return contradictions.groupBy { it.sourceDocument }.mapValues { entry ->
            entry.value.map { it.id }
        }
    }

    private fun buildLegalTriggers(
        contradictions: List<Contradiction>
    ): List<com.verumdec.core.model.LegalTriggerEvidence> {
        return contradictions
            .filter { it.legalTrigger != null }
            .groupBy { it.legalTrigger!! }
            .map { (trigger, triggerContradictions) ->
                com.verumdec.core.model.LegalTriggerEvidence(
                    trigger = trigger,
                    description = trigger.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    supportingContradictionIds = triggerContradictions.map { it.id },
                    confidence = triggerContradictions.map { it.severity }.average() / 10.0,
                    recommendation = "Review the ${triggerContradictions.size} supporting contradictions"
                )
            }
    }

    private fun calculateLiabilityScore(contradictions: List<Contradiction>): Int {
        if (contradictions.isEmpty()) return 0
        val avgSeverity = contradictions.map { it.severity }.average()
        val count = contradictions.size
        return ((avgSeverity * 5 + count * 3).toInt()).coerceIn(0, 100)
    }

    private fun describeSentiment(sentiment: Double): String = when {
        sentiment > 0.5 -> "positive"
        sentiment > 0.1 -> "slightly positive"
        sentiment > -0.1 -> "neutral"
        sentiment > -0.5 -> "slightly negative"
        else -> "negative"
    }

    private fun describeCertainty(certainty: Double): String = when {
        certainty > 0.8 -> "very certain"
        certainty > 0.6 -> "moderately certain"
        certainty > 0.4 -> "somewhat uncertain"
        certainty > 0.2 -> "uncertain"
        else -> "very uncertain"
    }

    private fun calculateShiftSeverity(shift: Double): Int = when {
        kotlin.math.abs(shift) > 1.0 -> 9
        kotlin.math.abs(shift) > 0.7 -> 7
        kotlin.math.abs(shift) > 0.5 -> 5
        else -> 3
    }
}

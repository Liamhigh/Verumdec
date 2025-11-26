package com.verumomnis.contradiction.pdf

import com.verumomnis.contradiction.engine.*
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * PDF Seal Engine
 *
 * Creates sealed, tamper-evident PDF reports with SHA-512 hashing,
 * Verum watermarking, and complete forensic documentation.
 */
class PdfSealEngine {

    companion object {
        private const val VERUM_WATERMARK = "VERUM OMNIS - SEALED FORENSIC REPORT"
        private const val PATENT_NOTICE = "Patent Pending Verum Omnis Contradiction Engine"
    }

    /**
     * Generates a complete forensic report.
     */
    fun generateReport(
        title: String,
        entities: Map<String, Entity>,
        timeline: List<TimelineEvent>,
        contradictions: List<Contradiction>,
        behavioralPatterns: List<BehavioralPattern>,
        liabilityMatrix: Map<String, LiabilityEntry>
    ): ForensicReport {
        val reportId = generateReportId()
        val generatedAt = LocalDateTime.now()

        // Build narrative sections
        val narrativeContent = buildNarrative(
            entities = entities,
            timeline = timeline,
            contradictions = contradictions,
            behavioralPatterns = behavioralPatterns,
            liabilityMatrix = liabilityMatrix
        )

        // Calculate content hash
        val contentHash = calculateSha512(narrativeContent)

        return ForensicReport(
            id = reportId,
            title = title,
            generatedAt = generatedAt,
            entities = entities.values.toList(),
            timeline = timeline,
            contradictions = contradictions,
            behavioralPatterns = behavioralPatterns,
            liabilityMatrix = liabilityMatrix.values.toList(),
            narrativeContent = narrativeContent,
            sha512Hash = contentHash,
            watermark = VERUM_WATERMARK,
            patentNotice = PATENT_NOTICE
        )
    }

    /**
     * Builds the complete narrative from all analysis data.
     */
    private fun buildNarrative(
        entities: Map<String, Entity>,
        timeline: List<TimelineEvent>,
        contradictions: List<Contradiction>,
        behavioralPatterns: List<BehavioralPattern>,
        liabilityMatrix: Map<String, LiabilityEntry>
    ): String {
        return buildString {
            // Section A: Objective Narration Layer
            appendLine("=" .repeat(80))
            appendLine("SECTION A: OBJECTIVE CHRONOLOGICAL ACCOUNT")
            appendLine("=".repeat(80))
            appendLine()
            appendLine(buildObjectiveNarration(timeline))
            appendLine()

            // Section B: Entity Summary
            appendLine("=".repeat(80))
            appendLine("SECTION B: ENTITY PROFILES")
            appendLine("=".repeat(80))
            appendLine()
            appendLine(buildEntitySummary(entities))
            appendLine()

            // Section C: Contradiction Analysis
            appendLine("=".repeat(80))
            appendLine("SECTION C: CONTRADICTION ANALYSIS")
            appendLine("=".repeat(80))
            appendLine()
            appendLine(buildContradictionSummary(contradictions))
            appendLine()

            // Section D: Behavioral Pattern Analysis
            appendLine("=".repeat(80))
            appendLine("SECTION D: BEHAVIORAL PATTERN ANALYSIS")
            appendLine("=".repeat(80))
            appendLine()
            appendLine(buildBehavioralSummary(behavioralPatterns))
            appendLine()

            // Section E: Liability Matrix
            appendLine("=".repeat(80))
            appendLine("SECTION E: LIABILITY MATRIX")
            appendLine("=".repeat(80))
            appendLine()
            appendLine(buildLiabilityMatrix(liabilityMatrix))
            appendLine()

            // Section F: Deductive Logic Layer
            appendLine("=".repeat(80))
            appendLine("SECTION F: DEDUCTIVE CONCLUSIONS")
            appendLine("=".repeat(80))
            appendLine()
            appendLine(buildDeductiveConclusions(contradictions, behavioralPatterns, liabilityMatrix))
        }
    }

    /**
     * Builds objective narration from timeline events.
     */
    private fun buildObjectiveNarration(timeline: List<TimelineEvent>): String {
        if (timeline.isEmpty()) {
            return "No events recorded."
        }

        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm")

        return timeline
            .sortedBy { it.timestamp }
            .joinToString("\n\n") { event ->
                val dateStr = event.timestamp.format(formatter)
                val entityPrefix = event.entityId?.let { "[$it] " } ?: ""
                "On $dateStr: $entityPrefix${event.description} (Source: ${event.sourceDocument})"
            }
    }

    /**
     * Builds entity summary section.
     */
    private fun buildEntitySummary(entities: Map<String, Entity>): String {
        if (entities.isEmpty()) {
            return "No entities identified."
        }

        return entities.values.joinToString("\n\n") { entity ->
            buildString {
                appendLine("ENTITY: ${entity.primaryName}")
                appendLine("-".repeat(40))
                if (entity.aliases.isNotEmpty()) {
                    appendLine("Aliases: ${entity.aliases.joinToString(", ")}")
                }
                entity.email?.let { appendLine("Email: $it") }
                entity.phone?.let { appendLine("Phone: $it") }
                appendLine("Total statements: ${entity.claims.size}")
                appendLine("Liability score: ${String.format("%.1f", entity.liabilityScore * 100)}%")
            }
        }
    }

    /**
     * Builds contradiction summary section.
     */
    private fun buildContradictionSummary(contradictions: List<Contradiction>): String {
        if (contradictions.isEmpty()) {
            return "No contradictions detected."
        }

        val grouped = contradictions.groupBy { it.severity }

        return buildString {
            appendLine("Total contradictions detected: ${contradictions.size}")
            appendLine()

            for (severity in Contradiction.Severity.entries) {
                val group = grouped[severity] ?: continue
                appendLine("${severity.name} SEVERITY (${group.size}):")
                appendLine("-".repeat(40))

                group.forEachIndexed { index, contradiction ->
                    appendLine("${index + 1}. ${contradiction.explanation}")
                    appendLine("   Type: ${contradiction.type.name}")
                    appendLine()
                }
            }
        }
    }

    /**
     * Builds behavioral summary section.
     */
    private fun buildBehavioralSummary(patterns: List<BehavioralPattern>): String {
        if (patterns.isEmpty()) {
            return "No significant behavioral patterns detected."
        }

        return patterns.joinToString("\n\n") { pattern ->
            buildString {
                appendLine("PATTERN: ${pattern.patternType.name}")
                appendLine("Entity: ${pattern.entityId}")
                appendLine("Confidence: ${String.format("%.0f", pattern.confidence * 100)}%")
                appendLine("Description: ${pattern.description}")
                appendLine("Instances: ${pattern.instances.size}")
            }
        }
    }

    /**
     * Builds liability matrix section.
     */
    private fun buildLiabilityMatrix(matrix: Map<String, LiabilityEntry>): String {
        if (matrix.isEmpty()) {
            return "No liability scores calculated."
        }

        val sorted = matrix.values.sortedByDescending { it.totalLiabilityPercent }

        return buildString {
            appendLine("ENTITY LIABILITY RANKING:")
            appendLine("=".repeat(60))
            appendLine()

            sorted.forEachIndexed { index, entry ->
                appendLine("${index + 1}. ${entry.entityId}: ${String.format("%.1f", entry.totalLiabilityPercent)}% RESPONSIBLE")
                appendLine("   - Contradiction Score: ${String.format("%.2f", entry.contradictionScore)}")
                appendLine("   - Behavioral Deception: ${String.format("%.2f", entry.behavioralDeceptionScore)}")
                appendLine("   - Evidence Contribution: ${String.format("%.2f", entry.evidenceContribution)}")
                appendLine("   - Chronological Consistency: ${String.format("%.2f", entry.chronologicalConsistency)}")
                appendLine("   - Causal Responsibility: ${String.format("%.2f", entry.causalResponsibility)}")
                appendLine()
            }
        }
    }

    /**
     * Builds deductive conclusions based on all analysis.
     */
    private fun buildDeductiveConclusions(
        contradictions: List<Contradiction>,
        patterns: List<BehavioralPattern>,
        liability: Map<String, LiabilityEntry>
    ): String {
        return buildString {
            appendLine("Based on the complete forensic analysis of all evidence:")
            appendLine()

            // Primary liable party
            val topLiable = liability.values.maxByOrNull { it.totalLiabilityPercent }
            if (topLiable != null && topLiable.totalLiabilityPercent > 50) {
                appendLine("PRIMARY FINDING:")
                appendLine("${topLiable.entityId} bears ${String.format("%.1f", topLiable.totalLiabilityPercent)}% responsibility")
                appendLine("based on ${contradictions.count { it.claimA.entityId == topLiable.entityId || it.claimB.entityId == topLiable.entityId }} contradictions")
                appendLine("and ${patterns.count { it.entityId == topLiable.entityId }} behavioral deception patterns.")
                appendLine()
            }

            // Key contradictions impact
            val criticalContradictions = contradictions.filter { it.severity == Contradiction.Severity.CRITICAL }
            if (criticalContradictions.isNotEmpty()) {
                appendLine("CRITICAL CONTRADICTIONS:")
                criticalContradictions.forEach { c ->
                    appendLine("- ${c.explanation}")
                }
                appendLine()
            }

            appendLine("This report constitutes a complete forensic analysis.")
            appendLine("All conclusions are derived solely from the provided evidence.")
        }
    }

    /**
     * Calculates SHA-512 hash of content.
     */
    private fun calculateSha512(content: String): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a unique report ID.
     */
    private fun generateReportId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "VO-${timestamp}-${random}"
    }

    /**
     * Verifies a report has not been tampered with.
     */
    fun verifyReport(report: ForensicReport): Boolean {
        val calculatedHash = calculateSha512(report.narrativeContent)
        return calculatedHash == report.sha512Hash
    }
}

/**
 * Complete forensic report data class.
 */
data class ForensicReport(
    val id: String,
    val title: String,
    val generatedAt: LocalDateTime,
    val entities: List<Entity>,
    val timeline: List<TimelineEvent>,
    val contradictions: List<Contradiction>,
    val behavioralPatterns: List<BehavioralPattern>,
    val liabilityMatrix: List<LiabilityEntry>,
    val narrativeContent: String,
    val sha512Hash: String,
    val watermark: String,
    val patentNotice: String
) {
    /**
     * Returns report metadata for display.
     */
    fun getMetadata(): Map<String, String> {
        return mapOf(
            "Report ID" to id,
            "Title" to title,
            "Generated" to generatedAt.toString(),
            "Entities" to entities.size.toString(),
            "Timeline Events" to timeline.size.toString(),
            "Contradictions" to contradictions.size.toString(),
            "Behavioral Patterns" to behavioralPatterns.size.toString(),
            "SHA-512 Hash" to sha512Hash.take(32) + "..."
        )
    }
}

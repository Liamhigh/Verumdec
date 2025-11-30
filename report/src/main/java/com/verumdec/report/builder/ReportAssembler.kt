package com.verumdec.report.builder

import android.content.Context
import com.verumdec.core.util.HashUtils
import com.verumdec.report.*
import java.io.File
import java.util.*

/**
 * ReportAssembler - Assembles complete reports from analysis data.
 * Coordinates between data sources and PDF generation.
 */
class ReportAssembler(private val context: Context) {

    private val pdfBuilder = PDFBuilder(context)

    /**
     * Assemble a complete report from raw analysis data.
     */
    fun assembleReport(input: ReportInput): AssembledReport {
        val startTime = System.currentTimeMillis()

        // Build sections
        val sections = mutableListOf<ReportSection>()

        // Executive Summary
        sections.add(buildExecutiveSummary(input))

        // Case Overview
        sections.add(buildCaseOverview(input))

        // Evidence Inventory
        sections.add(buildEvidenceInventory(input))

        // Entity Analysis
        if (input.entities.isNotEmpty()) {
            sections.add(buildEntityAnalysis(input))
        }

        // Timeline Reconstruction
        if (input.timelineEvents.isNotEmpty()) {
            sections.add(buildTimelineSection(input))
        }

        // Contradiction Analysis
        if (input.contradictions.isNotEmpty()) {
            sections.add(buildContradictionAnalysis(input))
        }

        // Behavioral Patterns
        if (input.behavioralPatterns.isNotEmpty()) {
            sections.add(buildBehavioralAnalysis(input))
        }

        // Liability Assessment
        if (input.liabilityScores.isNotEmpty()) {
            sections.add(buildLiabilityAssessment(input))
        }

        // Deductive Reasoning
        if (input.deductiveFindings.isNotEmpty()) {
            sections.add(buildDeductiveReasoning(input))
        }

        // Conclusion
        sections.add(buildConclusion(input))

        // Constitution Compliance
        sections.add(buildConstitutionCompliance(input))

        // Calculate content hash
        val contentHash = calculateContentHash(input, sections)

        // Build PDF
        val config = ReportConfig(
            caseName = input.caseName,
            caseId = input.caseId,
            sha512Hash = contentHash,
            generatedAt = Date(),
            sections = sections
        )

        val pdfFile = pdfBuilder.buildReport(config)
        val processingTime = System.currentTimeMillis() - startTime

        return AssembledReport(
            success = true,
            pdfFile = pdfFile,
            sha512Hash = contentHash,
            sectionCount = sections.size,
            pageCount = estimatePageCount(sections),
            processingTimeMs = processingTime,
            generatedAt = Date()
        )
    }

    private fun buildExecutiveSummary(input: ReportInput): ReportSection {
        val paragraphs = mutableListOf<String>()

        paragraphs.add(
            "This forensic analysis report presents findings from the examination of " +
            "${input.evidenceFiles.size} evidence file(s) in the matter of \"${input.caseName}\"."
        )

        paragraphs.add(
            "The analysis identified ${input.entities.size} relevant entities and detected " +
            "${input.contradictions.size} contradiction(s) ranging from low to critical severity."
        )

        if (input.timelineEvents.isNotEmpty()) {
            paragraphs.add(
                "A timeline spanning ${input.timelineEvents.size} events was reconstructed, " +
                "revealing the chronological sequence of key occurrences."
            )
        }

        val highestLiability = input.liabilityScores.maxByOrNull { it.value }
        if (highestLiability != null) {
            val entityName = input.entities.find { it.id == highestLiability.key }?.name ?: "Unknown"
            paragraphs.add(
                "The highest liability score of ${String.format("%.1f", highestLiability.value)}% " +
                "was attributed to $entityName based on contradiction patterns and behavioral analysis."
            )
        }

        return ReportSection(
            title = "EXECUTIVE SUMMARY",
            paragraphs = paragraphs
        )
    }

    private fun buildCaseOverview(input: ReportInput): ReportSection {
        return ReportSection(
            title = "CASE OVERVIEW",
            paragraphs = listOf(
                "Case Name: ${input.caseName}",
                "Case ID: ${input.caseId}",
                "Analysis Date: ${Date()}",
                "Evidence Files: ${input.evidenceFiles.size}",
                "Entities Identified: ${input.entities.size}",
                "Contradictions Detected: ${input.contradictions.size}"
            )
        )
    }

    private fun buildEvidenceInventory(input: ReportInput): ReportSection {
        return ReportSection(
            title = "EVIDENCE INVENTORY",
            paragraphs = listOf(
                "The following evidence files were analyzed:"
            ),
            bulletPoints = input.evidenceFiles.map { file ->
                "${file.fileName} (${file.type}) - Hash: ${file.hash.take(16)}..."
            }
        )
    }

    private fun buildEntityAnalysis(input: ReportInput): ReportSection {
        val bulletPoints = input.entities.map { entity ->
            val score = input.liabilityScores[entity.id] ?: 0f
            "${entity.name}: ${entity.mentions} mentions, Liability ${String.format("%.1f", score)}%"
        }

        return ReportSection(
            title = "ENTITY ANALYSIS",
            paragraphs = listOf(
                "The following entities were identified and analyzed:"
            ),
            bulletPoints = bulletPoints
        )
    }

    private fun buildTimelineSection(input: ReportInput): ReportSection {
        val events = input.timelineEvents.sortedBy { it.timestamp }.take(30)
        
        return ReportSection(
            title = "TIMELINE RECONSTRUCTION",
            paragraphs = listOf(
                "The following chronological sequence was reconstructed from evidence:"
            ),
            bulletPoints = events.map { event ->
                "${event.formattedDate}: ${event.description.take(100)}"
            }
        )
    }

    private fun buildContradictionAnalysis(input: ReportInput): ReportSection {
        val critical = input.contradictions.count { it.severity == "CRITICAL" }
        val high = input.contradictions.count { it.severity == "HIGH" }
        val medium = input.contradictions.count { it.severity == "MEDIUM" }
        val low = input.contradictions.count { it.severity == "LOW" }

        val paragraphs = listOf(
            "${input.contradictions.size} contradiction(s) were detected:",
            "Severity breakdown: Critical: $critical, High: $high, Medium: $medium, Low: $low"
        )

        val bulletPoints = input.contradictions.sortedByDescending { 
            when (it.severity) {
                "CRITICAL" -> 4
                "HIGH" -> 3
                "MEDIUM" -> 2
                else -> 1
            }
        }.take(15).map { c ->
            "[${c.severity}] ${c.description.take(150)}"
        }

        return ReportSection(
            title = "CONTRADICTION ANALYSIS",
            paragraphs = paragraphs,
            bulletPoints = bulletPoints
        )
    }

    private fun buildBehavioralAnalysis(input: ReportInput): ReportSection {
        return ReportSection(
            title = "BEHAVIORAL PATTERN ANALYSIS",
            paragraphs = listOf(
                "The following behavioral patterns were detected in communications:"
            ),
            bulletPoints = input.behavioralPatterns.map { pattern ->
                "${pattern.entityName}: ${pattern.patternType} (${pattern.instanceCount} instances, ${pattern.severity} severity)"
            }
        )
    }

    private fun buildLiabilityAssessment(input: ReportInput): ReportSection {
        val sorted = input.liabilityScores.entries.sortedByDescending { it.value }
        
        val bulletPoints = sorted.map { (entityId, score) ->
            val entity = input.entities.find { it.id == entityId }
            "${entity?.name ?: entityId}: ${String.format("%.1f", score)}%"
        }

        return ReportSection(
            title = "LIABILITY ASSESSMENT",
            paragraphs = listOf(
                "Based on contradiction patterns, behavioral analysis, and evidence contribution, " +
                "the following liability scores were calculated:"
            ),
            bulletPoints = bulletPoints
        )
    }

    private fun buildDeductiveReasoning(input: ReportInput): ReportSection {
        return ReportSection(
            title = "DEDUCTIVE REASONING",
            paragraphs = input.deductiveFindings.take(10)
        )
    }

    private fun buildConclusion(input: ReportInput): ReportSection {
        val highestLiability = input.liabilityScores.maxByOrNull { it.value }
        val conclusionText = if (highestLiability != null && highestLiability.value > 50f) {
            val entityName = input.entities.find { it.id == highestLiability.key }?.name ?: "Unknown"
            "Based on the analysis, $entityName bears the highest responsibility with a " +
            "liability score of ${String.format("%.1f", highestLiability.value)}%. " +
            "This conclusion is supported by ${input.contradictions.size} detected contradiction(s) " +
            "and ${input.behavioralPatterns.filter { it.entityId == highestLiability.key }.size} behavioral flag(s)."
        } else {
            "The analysis indicates no party with conclusively high liability based on available evidence. " +
            "Further investigation may be required to establish clear responsibility."
        }

        return ReportSection(
            title = "CONCLUSION",
            paragraphs = listOf(
                conclusionText,
                "This analysis was conducted using the Verum Omnis Contradiction Engine, applying " +
                "systematic deductive reasoning to identify inconsistencies in evidence."
            )
        )
    }

    private fun buildConstitutionCompliance(input: ReportInput): ReportSection {
        val violations = input.constitutionViolations
        
        return if (violations.isEmpty()) {
            ReportSection(
                title = "CONSTITUTION COMPLIANCE",
                paragraphs = listOf(
                    "This analysis was conducted in full compliance with the Verum Omnis Constitutional Charter.",
                    "All principles of truth preservation, impartiality, transparency, privacy protection, " +
                    "and chain of custody were maintained throughout the analysis process."
                )
            )
        } else {
            ReportSection(
                title = "CONSTITUTION COMPLIANCE",
                paragraphs = listOf(
                    "The following constitutional warnings were generated during analysis:"
                ),
                bulletPoints = violations.map { v ->
                    "[${v.severity}] ${v.ruleName}: ${v.description}"
                }
            )
        }
    }

    private fun calculateContentHash(input: ReportInput, sections: List<ReportSection>): String {
        val content = StringBuilder()
        content.append(input.caseName)
        content.append(input.caseId)
        content.append(input.evidenceFiles.size)
        content.append(input.entities.size)
        content.append(input.contradictions.size)
        sections.forEach { section ->
            content.append(section.title)
            section.paragraphs.forEach { content.append(it) }
        }
        content.append(Date().time)
        
        return HashUtils.sha512(content.toString())
    }

    private fun estimatePageCount(sections: List<ReportSection>): Int {
        var lines = 20 // Headers and footers
        sections.forEach { section ->
            lines += 5 // Section header
            lines += section.paragraphs.size * 3
            lines += section.bulletPoints.size
        }
        return (lines / 50) + 1
    }
}

// Input data classes

data class ReportInput(
    val caseName: String,
    val caseId: String,
    val evidenceFiles: List<EvidenceFileInfo>,
    val entities: List<EntityInfo>,
    val timelineEvents: List<TimelineEventInfo>,
    val contradictions: List<ContradictionInfo>,
    val behavioralPatterns: List<BehavioralPatternInfo>,
    val liabilityScores: Map<String, Float>,
    val deductiveFindings: List<String>,
    val constitutionViolations: List<ConstitutionViolationInfo>
)

data class EvidenceFileInfo(
    val fileName: String,
    val type: String,
    val hash: String
)

data class EntityInfo(
    val id: String,
    val name: String,
    val mentions: Int
)

data class TimelineEventInfo(
    val timestamp: Long,
    val formattedDate: String,
    val description: String
)

data class ContradictionInfo(
    val severity: String,
    val description: String,
    val entityId: String
)

data class BehavioralPatternInfo(
    val entityId: String,
    val entityName: String,
    val patternType: String,
    val instanceCount: Int,
    val severity: String
)

data class ConstitutionViolationInfo(
    val ruleId: String,
    val ruleName: String,
    val description: String,
    val severity: String
)

data class AssembledReport(
    val success: Boolean,
    val pdfFile: File?,
    val sha512Hash: String,
    val sectionCount: Int,
    val pageCount: Int,
    val processingTimeMs: Long,
    val generatedAt: Date,
    val error: String? = null
)

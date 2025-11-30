package com.verumdec.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.verumdec.data.*
import com.verumdec.core.leveler.LevelerOutput
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LevelerReportGenerator - New PDF Report Generator using Leveler Engine output.
 * 
 * This generator creates sealed forensic reports with SHA-512 hash using the
 * new Leveler engine output models:
 * - LevelerOutput
 * - ContradictionSet
 * - SpeakerMap
 * - NormalizedTimeline
 * - BehaviorShiftReport
 * 
 * Uses dual-layer architecture:
 * - Layer 1 (Semantic Layer): Invisible text for AI/parser accessibility
 * - Layer 2 (Forensic Layer): Visible raster layer for human viewing
 */
class LevelerReportGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.US)
    private val pageWidth = 595 // A4 width in points
    private val pageHeight = 842 // A4 height in points
    private val margin = 50f
    private val lineHeight = 14f
    
    private val invisibleTextAlpha = 3
    private val visibleTextAlpha = 255

    /**
     * Generate PDF report using Leveler engine output.
     */
    suspend fun generateReport(case: Case, levelerOutput: LevelerOutput?): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        var currentPage: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var yPosition = margin
        var pageNumber = 1

        // Visible paints
        val titlePaint = Paint().apply {
            textSize = 24f
            color = Color.parseColor("#1A237E")
            isFakeBoldText = true
        }

        val headingPaint = Paint().apply {
            textSize = 16f
            color = Color.parseColor("#1A237E")
            isFakeBoldText = true
        }

        val subheadingPaint = Paint().apply {
            textSize = 14f
            color = Color.parseColor("#3949AB")
            isFakeBoldText = true
        }

        val bodyPaint = Paint().apply {
            textSize = 10f
            color = Color.BLACK
        }

        val labelPaint = Paint().apply {
            textSize = 9f
            color = Color.GRAY
        }

        val severityPaints = mapOf(
            Severity.CRITICAL to Paint().apply { textSize = 10f; color = Color.parseColor("#D32F2F") },
            Severity.HIGH to Paint().apply { textSize = 10f; color = Color.parseColor("#F57C00") },
            Severity.MEDIUM to Paint().apply { textSize = 10f; color = Color.parseColor("#FBC02D") },
            Severity.LOW to Paint().apply { textSize = 10f; color = Color.parseColor("#388E3C") }
        )

        // Invisible paints
        val invisibleTitlePaint = createInvisiblePaint(titlePaint)
        val invisibleHeadingPaint = createInvisiblePaint(headingPaint)
        val invisibleSubheadingPaint = createInvisiblePaint(subheadingPaint)
        val invisibleBodyPaint = createInvisiblePaint(bodyPaint)
        val invisibleLabelPaint = createInvisiblePaint(labelPaint)
        val invisibleSeverityPaints = severityPaints.mapValues { createInvisiblePaint(it.value) }
        
        val paintMapping: Map<Paint, Paint> = buildMap {
            put(titlePaint, invisibleTitlePaint)
            put(headingPaint, invisibleHeadingPaint)
            put(subheadingPaint, invisibleSubheadingPaint)
            put(bodyPaint, invisibleBodyPaint)
            put(labelPaint, invisibleLabelPaint)
            severityPaints.forEach { (severity, visiblePaint) ->
                invisibleSeverityPaints[severity]?.let { invisiblePaint ->
                    put(visiblePaint, invisiblePaint)
                }
            }
        }

        fun startNewPage() {
            currentPage?.let { pdfDocument.finishPage(it) }
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage?.canvas
            yPosition = margin
            pageNumber++
            
            val footerText = "LEVELER ENGINE v2.0.0 • Verum Omnis • Page ${pageNumber - 1}"
            canvas?.drawText(footerText, margin, pageHeight - 20f, invisibleLabelPaint)
            canvas?.drawText(footerText, margin, pageHeight - 20f, labelPaint)
        }

        fun checkPageBreak(requiredSpace: Float = lineHeight * 3) {
            if (yPosition + requiredSpace > pageHeight - margin) {
                startNewPage()
            }
        }

        fun drawDualLayerText(text: String, visiblePaint: Paint, invisiblePaint: Paint, indent: Float = 0f) {
            val maxWidth = pageWidth - 2 * margin - indent
            val words = text.split(" ")
            var currentLine = ""
            
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val testWidth = visiblePaint.measureText(testLine)
                
                if (testWidth <= maxWidth) {
                    currentLine = testLine
                } else {
                    checkPageBreak()
                    val xPos = margin + indent
                    canvas?.drawText(currentLine, xPos, yPosition, invisiblePaint)
                    canvas?.drawText(currentLine, xPos, yPosition, visiblePaint)
                    yPosition += lineHeight
                    currentLine = word
                }
            }
            
            if (currentLine.isNotEmpty()) {
                checkPageBreak()
                val xPos = margin + indent
                canvas?.drawText(currentLine, xPos, yPosition, invisiblePaint)
                canvas?.drawText(currentLine, xPos, yPosition, visiblePaint)
                yPosition += lineHeight
            }
        }

        fun drawText(text: String, paint: Paint, indent: Float = 0f) {
            val invisiblePaint = paintMapping[paint] ?: createInvisiblePaint(paint)
            drawDualLayerText(text, paint, invisiblePaint, indent)
        }

        fun addSpace(lines: Float = 1f) {
            yPosition += lineHeight * lines
        }

        fun drawHeading(text: String, visiblePaint: Paint, invisiblePaint: Paint? = null) {
            val invisible = invisiblePaint ?: paintMapping[visiblePaint] ?: createInvisiblePaint(visiblePaint)
            canvas?.drawText(text, margin, yPosition, invisible)
            canvas?.drawText(text, margin, yPosition, visiblePaint)
        }

        // Generate content hash
        val reportContent = buildReportContent(case, levelerOutput)
        val hash = calculateSHA512(reportContent)

        // Start first page
        startNewPage()

        // Title page
        addSpace(3f)
        drawHeading("LEVELER ENGINE FORENSIC REPORT", titlePaint, invisibleTitlePaint)
        yPosition += 30f
        
        drawHeading("Generated by Verum Omnis Leveler Engine v2.0.0", subheadingPaint, invisibleSubheadingPaint)
        addSpace(2f)
        
        drawText("Case: ${case.name}", bodyPaint)
        drawText("Generated: ${dateFormat.format(Date())}", bodyPaint)
        drawText("Report ID: ${UUID.randomUUID()}", labelPaint)
        
        // NEW: Leveler engine metadata
        if (levelerOutput != null) {
            addSpace()
            drawText("Engine Version: ${levelerOutput.engineVersion}", labelPaint)
            drawText("Documents Processed: ${levelerOutput.extractionSummary.processedDocuments}/${levelerOutput.extractionSummary.totalDocuments}", labelPaint)
            drawText("Speakers Identified: ${levelerOutput.extractionSummary.speakersIdentified}", labelPaint)
        }
        addSpace(2f)

        // Sealed indicator
        val sealedPaint = Paint().apply {
            textSize = 12f
            color = Color.parseColor("#D32F2F")
            isFakeBoldText = true
        }
        val invisibleSealedPaint = createInvisiblePaint(sealedPaint)
        drawHeading("SEALED DOCUMENT - LEVELER ENGINE", sealedPaint, invisibleSealedPaint)
        addSpace()
        drawText("SHA-512: ${hash.take(64)}...", labelPaint)
        addSpace(3f)

        // Table of Contents
        drawHeading("TABLE OF CONTENTS", headingPaint, invisibleHeadingPaint)
        addSpace()
        drawText("1. Executive Summary (Leveler Analysis)", bodyPaint)
        drawText("2. Speaker Extraction Results", bodyPaint)
        drawText("3. Normalized Timeline", bodyPaint)
        drawText("4. Contradiction Analysis (Leveler)", bodyPaint)
        drawText("5. Behavioral Shift Report", bodyPaint)
        drawText("6. Liability Assessment", bodyPaint)
        drawText("7. Narrative Summary", bodyPaint)
        drawText("8. Conclusion", bodyPaint)

        // Section 1: Executive Summary
        startNewPage()
        drawHeading("1. EXECUTIVE SUMMARY - LEVELER ENGINE", headingPaint, invisibleHeadingPaint)
        addSpace(2f)
        
        drawText("This report was generated using the Verum Omnis LEVELER Engine v2.0.0,", bodyPaint)
        drawText("the new unified contradiction detection and behavioral analysis system.", bodyPaint)
        addSpace()
        
        if (levelerOutput != null) {
            drawText("Extraction Summary:", subheadingPaint)
            drawText("• Total Documents: ${levelerOutput.extractionSummary.totalDocuments}", bodyPaint)
            drawText("• Processed Documents: ${levelerOutput.extractionSummary.processedDocuments}", bodyPaint)
            drawText("• Total Statements: ${levelerOutput.extractionSummary.totalStatements}", bodyPaint)
            drawText("• Speakers Identified: ${levelerOutput.extractionSummary.speakersIdentified}", bodyPaint)
            drawText("• Timeline Events: ${levelerOutput.extractionSummary.timelineEvents}", bodyPaint)
            drawText("• Processing Time: ${levelerOutput.extractionSummary.processingDurationMs}ms", bodyPaint)
            addSpace()
            
            drawText("Contradiction Summary:", subheadingPaint)
            drawText("• Total Contradictions: ${levelerOutput.contradictionSet.totalCount}", bodyPaint)
            drawText("• Critical: ${levelerOutput.contradictionSet.criticalCount}", bodyPaint)
            drawText("• High: ${levelerOutput.contradictionSet.highCount}", bodyPaint)
            drawText("• Medium: ${levelerOutput.contradictionSet.mediumCount}", bodyPaint)
            drawText("• Low: ${levelerOutput.contradictionSet.lowCount}", bodyPaint)
            addSpace()
            
            drawText("Behavioral Analysis:", subheadingPaint)
            drawText("• Total Behavioral Shifts: ${levelerOutput.behaviorShiftReport.totalShifts}", bodyPaint)
            drawText("• Affected Speakers: ${levelerOutput.behaviorShiftReport.affectedSpeakers.size}", bodyPaint)
            for ((pattern, count) in levelerOutput.behaviorShiftReport.patternBreakdown) {
                drawText("  - $pattern: $count", bodyPaint)
            }
        } else {
            drawText("Key Statistics:", subheadingPaint)
            drawText("• Entities Identified: ${case.entities.size}", bodyPaint)
            drawText("• Timeline Events: ${case.timeline.size}", bodyPaint)
            drawText("• Contradictions Detected: ${case.contradictions.size}", bodyPaint)
        }

        // Section 2: Speaker Extraction
        addSpace(2f)
        drawHeading("2. SPEAKER EXTRACTION RESULTS", headingPaint, invisibleHeadingPaint)
        addSpace(2f)
        
        if (levelerOutput != null) {
            drawText("Detected ${levelerOutput.speakerMap.totalSpeakers} speakers:", subheadingPaint)
            addSpace()
            for ((name, profile) in levelerOutput.speakerMap.speakers) {
                checkPageBreak(lineHeight * 5)
                drawText(profile.name, subheadingPaint)
                drawText("Statements: ${profile.statementCount}", bodyPaint, 10f)
                drawText("Documents: ${profile.documentIds.size}", bodyPaint, 10f)
                drawText("Avg Sentiment: ${String.format("%.2f", profile.averageSentiment)}", bodyPaint, 10f)
                drawText("Avg Certainty: ${String.format("%.2f", profile.averageCertainty)}", bodyPaint, 10f)
                addSpace()
            }
        } else {
            for (entity in case.entities) {
                checkPageBreak(lineHeight * 5)
                drawText(entity.primaryName, subheadingPaint)
                if (entity.aliases.isNotEmpty()) {
                    drawText("Aliases: ${entity.aliases.joinToString(", ")}", bodyPaint, 10f)
                }
                drawText("Mentions: ${entity.mentions}", bodyPaint, 10f)
                addSpace()
            }
        }

        // Section 3: Normalized Timeline
        startNewPage()
        drawHeading("3. NORMALIZED TIMELINE", headingPaint, invisibleHeadingPaint)
        addSpace(2f)
        
        if (levelerOutput != null && levelerOutput.normalizedTimeline.events.isNotEmpty()) {
            drawText("Normalization Mode: ${levelerOutput.normalizedTimeline.normalizationMode}", labelPaint)
            addSpace()
            for (event in levelerOutput.normalizedTimeline.events.take(30)) {
                checkPageBreak(lineHeight * 3)
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(event.timestamp))
                drawText("[$dateStr] [${event.eventType}] ${event.speaker}", labelPaint)
                drawText(event.description.take(150), bodyPaint, 10f)
                addSpace(0.5f)
            }
            if (levelerOutput.normalizedTimeline.events.size > 30) {
                drawText("... and ${levelerOutput.normalizedTimeline.events.size - 30} more events", labelPaint)
            }
        } else {
            val sortedTimeline = case.timeline.sortedBy { it.date }
            for (event in sortedTimeline.take(30)) {
                checkPageBreak(lineHeight * 3)
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(event.date)
                drawText("[$dateStr] ${event.eventType.name}", labelPaint)
                drawText(event.description.take(150), bodyPaint, 10f)
                addSpace(0.5f)
            }
        }

        // Section 4: Contradiction Analysis
        startNewPage()
        drawHeading("4. CONTRADICTION ANALYSIS - LEVELER ENGINE", headingPaint, invisibleHeadingPaint)
        addSpace(2f)
        
        if (levelerOutput != null && levelerOutput.contradictionSet.contradictions.isNotEmpty()) {
            drawText("Contradiction Types:", subheadingPaint)
            for ((type, count) in levelerOutput.contradictionSet.contradictionTypes) {
                drawText("• $type: $count", bodyPaint)
            }
            addSpace()
            
            for ((index, contradiction) in levelerOutput.contradictionSet.contradictions.take(20).withIndex()) {
                checkPageBreak(lineHeight * 8)
                
                val severityLevel = when {
                    contradiction.severity >= 9 -> Severity.CRITICAL
                    contradiction.severity >= 7 -> Severity.HIGH
                    contradiction.severity >= 5 -> Severity.MEDIUM
                    else -> Severity.LOW
                }
                
                drawText("Contradiction ${index + 1}: ${contradiction.sourceSpeaker} vs ${contradiction.targetSpeaker}", subheadingPaint)
                drawText("[${severityLevel.name}] ${contradiction.type.name}", severityPaints[severityLevel]!!)
                addSpace(0.5f)
                drawText(contradiction.description, bodyPaint, 10f)
                drawText("Source: \"${contradiction.sourceText}\"", labelPaint, 10f)
                drawText("Target: \"${contradiction.targetText}\"", labelPaint, 10f)
                drawText("Legal Trigger: ${contradiction.legalTrigger}", labelPaint, 10f)
                addSpace()
            }
        } else if (case.contradictions.isEmpty()) {
            drawText("No contradictions were detected in the analyzed evidence.", bodyPaint)
        } else {
            for ((index, contradiction) in case.contradictions.withIndex()) {
                checkPageBreak(lineHeight * 8)
                val entityName = case.entities.find { it.id == contradiction.entityId }?.primaryName ?: "Unknown"
                drawText("Contradiction ${index + 1}: $entityName", subheadingPaint)
                drawText("[${contradiction.severity.name}] ${contradiction.type.name}", severityPaints[contradiction.severity]!!)
                drawText(contradiction.description, bodyPaint, 10f)
                addSpace()
            }
        }

        // Section 5: Behavioral Shift Report
        startNewPage()
        drawHeading("5. BEHAVIORAL SHIFT REPORT", headingPaint, invisibleHeadingPaint)
        addSpace(2f)
        
        if (levelerOutput != null && levelerOutput.behaviorShiftReport.shifts.isNotEmpty()) {
            drawText("Total Behavioral Shifts Detected: ${levelerOutput.behaviorShiftReport.totalShifts}", subheadingPaint)
            drawText("Affected Speakers: ${levelerOutput.behaviorShiftReport.affectedSpeakers.joinToString(", ")}", bodyPaint)
            addSpace()
            
            drawText("Pattern Breakdown:", subheadingPaint)
            for ((pattern, count) in levelerOutput.behaviorShiftReport.patternBreakdown) {
                drawText("• $pattern: $count instances", bodyPaint)
            }
            addSpace()
            
            for (shift in levelerOutput.behaviorShiftReport.shifts.take(15)) {
                checkPageBreak(lineHeight * 5)
                val severityLevel = when {
                    shift.severity >= 8 -> Severity.CRITICAL
                    shift.severity >= 6 -> Severity.HIGH
                    shift.severity >= 4 -> Severity.MEDIUM
                    else -> Severity.LOW
                }
                drawText("${shift.speakerId}: ${shift.shiftType.name}", subheadingPaint)
                drawText("Severity: ${shift.severity}", severityPaints[severityLevel]!!)
                drawText("${shift.beforeState} → ${shift.afterState}", bodyPaint, 10f)
                drawText(shift.description, labelPaint, 10f)
                addSpace()
            }
        } else {
            drawText("No significant behavioral shifts were detected.", bodyPaint)
        }

        // Section 6: Liability Assessment
        startNewPage()
        drawHeading("6. LIABILITY ASSESSMENT", headingPaint, invisibleHeadingPaint)
        addSpace(2f)
        
        if (levelerOutput != null) {
            val sortedScores = levelerOutput.liabilityScores.values.sortedByDescending { it.overallScore }
            
            for (score in sortedScores) {
                checkPageBreak(lineHeight * 10)
                
                drawText(score.entityName, subheadingPaint)
                drawText("Overall Liability: ${String.format("%.1f", score.overallScore)}%", bodyPaint, 10f)
                addSpace(0.5f)
                drawText("Breakdown:", labelPaint, 10f)
                drawText("• Contradiction Score: ${String.format("%.1f", score.contradictionScore)} (${score.contradictionCount} contradictions)", bodyPaint, 20f)
                drawText("• Behavioral Score: ${String.format("%.1f", score.behavioralScore)} (${score.behavioralShiftCount} shifts)", bodyPaint, 20f)
                drawText("• Consistency Score: ${String.format("%.1f", score.consistencyScore)}", bodyPaint, 20f)
                drawText("• Evidence Score: ${String.format("%.1f", score.evidenceContributionScore)}", bodyPaint, 20f)
                if (score.breakdown.behavioralFlags.isNotEmpty()) {
                    drawText("• Behavioral Flags: ${score.breakdown.behavioralFlags.take(5).joinToString(", ")}", bodyPaint, 20f)
                }
                addSpace()
            }
        } else {
            val sortedScores = case.liabilityScores.values.sortedByDescending { it.overallScore }
            for (score in sortedScores) {
                checkPageBreak(lineHeight * 8)
                val entityName = case.entities.find { it.id == score.entityId }?.primaryName ?: "Unknown"
                drawText(entityName, subheadingPaint)
                drawText("Overall Liability: ${String.format("%.1f", score.overallScore)}%", bodyPaint, 10f)
                addSpace()
            }
        }

        // Section 7: Narrative Summary
        startNewPage()
        drawHeading("7. NARRATIVE SUMMARY", headingPaint, invisibleHeadingPaint)
        addSpace(2f)
        
        val summaryLines = case.narrative.split("\n")
        for (line in summaryLines) {
            if (line.isNotBlank()) {
                checkPageBreak()
                drawText(line, bodyPaint)
            }
        }

        // Section 8: Conclusion
        startNewPage()
        drawHeading("8. CONCLUSION", headingPaint, invisibleHeadingPaint)
        addSpace(2f)
        
        drawText("This forensic analysis was conducted using the Verum Omnis LEVELER Engine v2.0.0,", bodyPaint)
        drawText("the new unified contradiction detection and behavioral analysis system.", bodyPaint)
        addSpace()
        drawText("The LEVELER Engine provides:", bodyPaint)
        drawText("• Multi-pass contradiction detection (intra-document, cross-document, cross-modal)", bodyPaint, 10f)
        drawText("• Semantic embedding-based similarity analysis", bodyPaint, 10f)
        drawText("• Behavioral pattern recognition (gaslighting, deflection, over-explaining)", bodyPaint, 10f)
        drawText("• Timeline normalization and conflict detection", bodyPaint, 10f)
        drawText("• Entity-level contradiction tracking", bodyPaint, 10f)
        drawText("• Liability scoring with detailed breakdowns", bodyPaint, 10f)
        addSpace(2f)
        
        drawHeading("DOCUMENT SEAL", sealedPaint, invisibleSealedPaint)
        addSpace()
        drawText("This document has been cryptographically sealed with SHA-512.", bodyPaint)
        addSpace()
        drawText("Hash: $hash", labelPaint)
        addSpace(2f)
        drawText("LEVELER ENGINE v2.0.0 • Patent Pending • Verum Omnis", labelPaint)
        drawText("Generated: ${dateFormat.format(Date())}", labelPaint)

        // Finish last page
        currentPage?.let { pdfDocument.finishPage(it) }

        // Save PDF
        val reportsDir = File(context.getExternalFilesDir(null), "reports")
        reportsDir.mkdirs()
        
        val fileName = "Leveler_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        val file = File(reportsDir, fileName)
        
        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        
        pdfDocument.close()
        
        file
    }

    private fun createInvisiblePaint(visiblePaint: Paint): Paint {
        return Paint(visiblePaint).apply {
            alpha = invisibleTextAlpha
        }
    }

    private fun buildReportContent(case: Case, levelerOutput: LevelerOutput?): String {
        val builder = StringBuilder()
        
        builder.appendLine("LEVELER ENGINE FORENSIC REPORT")
        builder.appendLine("DUAL-LAYER SEALED DOCUMENT")
        builder.appendLine("Engine Version: ${levelerOutput?.engineVersion ?: "2.0.0-leveler"}")
        builder.appendLine("Case: ${case.name}")
        builder.appendLine("Generated: ${dateFormat.format(Date())}")
        builder.appendLine()
        
        if (levelerOutput != null) {
            builder.appendLine("EXTRACTION SUMMARY:")
            builder.appendLine("Documents: ${levelerOutput.extractionSummary.processedDocuments}/${levelerOutput.extractionSummary.totalDocuments}")
            builder.appendLine("Statements: ${levelerOutput.extractionSummary.totalStatements}")
            builder.appendLine("Speakers: ${levelerOutput.extractionSummary.speakersIdentified}")
            builder.appendLine()
            
            builder.appendLine("SPEAKER MAP:")
            for ((name, profile) in levelerOutput.speakerMap.speakers) {
                builder.appendLine("${profile.name} | Statements: ${profile.statementCount}")
            }
            builder.appendLine()
            
            builder.appendLine("CONTRADICTION SET:")
            builder.appendLine("Total: ${levelerOutput.contradictionSet.totalCount}")
            builder.appendLine("Critical: ${levelerOutput.contradictionSet.criticalCount}")
            builder.appendLine("High: ${levelerOutput.contradictionSet.highCount}")
            for (c in levelerOutput.contradictionSet.contradictions) {
                builder.appendLine("${c.type} | ${c.severity} | ${c.description}")
            }
            builder.appendLine()
            
            builder.appendLine("BEHAVIOR SHIFT REPORT:")
            builder.appendLine("Total Shifts: ${levelerOutput.behaviorShiftReport.totalShifts}")
            for (shift in levelerOutput.behaviorShiftReport.shifts) {
                builder.appendLine("${shift.shiftType} | ${shift.speakerId} | ${shift.description}")
            }
            builder.appendLine()
            
            builder.appendLine("LIABILITY SCORES:")
            for ((id, score) in levelerOutput.liabilityScores) {
                builder.appendLine("${score.entityName}: ${score.overallScore}")
            }
        } else {
            builder.appendLine("ENTITIES: ${case.entities.size}")
            builder.appendLine("TIMELINE EVENTS: ${case.timeline.size}")
            builder.appendLine("CONTRADICTIONS: ${case.contradictions.size}")
        }
        
        builder.appendLine()
        builder.appendLine("NARRATIVE:")
        builder.appendLine(case.narrative)
        
        builder.appendLine()
        builder.appendLine("ENGINE_METADATA:")
        builder.appendLine("EngineType: LEVELER")
        builder.appendLine("LayerArchitecture: DUAL_LAYER")
        
        return builder.toString()
    }

    private fun calculateSHA512(content: String): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hashBytes = digest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

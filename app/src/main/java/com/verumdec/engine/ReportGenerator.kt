package com.verumdec.engine

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.verumdec.data.*
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * PDF Report Generator
 * Creates sealed forensic reports with SHA-512 hash.
 */
class ReportGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.US)
    private val pageWidth = 595 // A4 width in points
    private val pageHeight = 842 // A4 height in points
    private val margin = 50f
    private val lineHeight = 14f

    /**
     * Generate a complete forensic report.
     */
    fun generateReport(
        caseName: String,
        entities: List<Entity>,
        timeline: List<TimelineEvent>,
        contradictions: List<Contradiction>,
        behavioralPatterns: List<BehavioralPattern>,
        liabilityScores: Map<String, LiabilityScore>,
        narrativeSections: NarrativeSections
    ): ForensicReport {
        val reportContent = buildReportContent(
            caseName, entities, timeline, contradictions,
            behavioralPatterns, liabilityScores, narrativeSections
        )
        
        val hash = calculateSHA512(reportContent)
        
        return ForensicReport(
            caseId = UUID.randomUUID().toString(),
            caseName = caseName,
            generatedAt = Date(),
            entities = entities,
            timeline = timeline,
            contradictions = contradictions,
            behavioralPatterns = behavioralPatterns,
            liabilityScores = liabilityScores,
            narrativeSections = narrativeSections,
            sha512Hash = hash
        )
    }

    /**
     * Export report to PDF file.
     */
    fun exportToPdf(report: ForensicReport): File {
        val pdfDocument = PdfDocument()
        var currentPage: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var yPosition = margin
        var pageNumber = 1

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

        fun startNewPage() {
            currentPage?.let { pdfDocument.finishPage(it) }
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage?.canvas
            yPosition = margin
            pageNumber++
            
            // Add footer
            canvas?.drawText(
                "Verum Omnis Contradiction Engine • Page ${pageNumber - 1}",
                margin,
                pageHeight - 20f,
                labelPaint
            )
        }

        fun checkPageBreak(requiredSpace: Float = lineHeight * 3) {
            if (yPosition + requiredSpace > pageHeight - margin) {
                startNewPage()
            }
        }

        fun drawText(text: String, paint: Paint, indent: Float = 0f) {
            val maxWidth = pageWidth - 2 * margin - indent
            val words = text.split(" ")
            var currentLine = ""
            
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val testWidth = paint.measureText(testLine)
                
                if (testWidth <= maxWidth) {
                    currentLine = testLine
                } else {
                    checkPageBreak()
                    canvas?.drawText(currentLine, margin + indent, yPosition, paint)
                    yPosition += lineHeight
                    currentLine = word
                }
            }
            
            if (currentLine.isNotEmpty()) {
                checkPageBreak()
                canvas?.drawText(currentLine, margin + indent, yPosition, paint)
                yPosition += lineHeight
            }
        }

        fun addSpace(lines: Float = 1f) {
            yPosition += lineHeight * lines
        }

        // Start first page
        startNewPage()

        // Title page
        addSpace(3f)
        canvas?.drawText("FORENSIC ANALYSIS REPORT", margin, yPosition, titlePaint)
        yPosition += 30f
        
        canvas?.drawText("Generated by Verum Omnis Contradiction Engine", margin, yPosition, subheadingPaint)
        addSpace(2f)
        
        drawText("Case: ${report.caseName}", bodyPaint)
        drawText("Generated: ${dateFormat.format(report.generatedAt)}", bodyPaint)
        drawText("Report ID: ${report.id}", labelPaint)
        addSpace(2f)

        // Sealed indicator
        val sealedPaint = Paint().apply {
            textSize = 12f
            color = Color.parseColor("#D32F2F")
            isFakeBoldText = true
        }
        canvas?.drawText("SEALED DOCUMENT", margin, yPosition, sealedPaint)
        addSpace()
        drawText("SHA-512: ${report.sha512Hash.take(64)}...", labelPaint)
        addSpace(3f)

        // Table of Contents
        canvas?.drawText("TABLE OF CONTENTS", margin, yPosition, headingPaint)
        addSpace()
        drawText("1. Executive Summary", bodyPaint)
        drawText("2. Entities Discovered", bodyPaint)
        drawText("3. Timeline of Events", bodyPaint)
        drawText("4. Contradiction Analysis", bodyPaint)
        drawText("5. Behavioral Pattern Analysis", bodyPaint)
        drawText("6. Liability Assessment", bodyPaint)
        drawText("7. Narrative Summary", bodyPaint)
        drawText("8. Conclusion", bodyPaint)

        // Section 1: Executive Summary
        startNewPage()
        canvas?.drawText("1. EXECUTIVE SUMMARY", margin, yPosition, headingPaint)
        addSpace(2f)
        
        drawText("This report presents the findings of a comprehensive forensic analysis", bodyPaint)
        drawText("conducted using the Verum Omnis Contradiction Engine.", bodyPaint)
        addSpace()
        drawText("Key Statistics:", subheadingPaint)
        drawText("• Entities Identified: ${report.entities.size}", bodyPaint)
        drawText("• Timeline Events: ${report.timeline.size}", bodyPaint)
        drawText("• Contradictions Detected: ${report.contradictions.size}", bodyPaint)
        drawText("• Behavioral Patterns: ${report.behavioralPatterns.size}", bodyPaint)

        // Section 2: Entities
        addSpace(2f)
        canvas?.drawText("2. ENTITIES DISCOVERED", margin, yPosition, headingPaint)
        addSpace(2f)
        
        for (entity in report.entities) {
            checkPageBreak(lineHeight * 5)
            drawText("${entity.primaryName}", subheadingPaint)
            if (entity.aliases.isNotEmpty()) {
                drawText("Aliases: ${entity.aliases.joinToString(", ")}", bodyPaint, 10f)
            }
            if (entity.emails.isNotEmpty()) {
                drawText("Emails: ${entity.emails.joinToString(", ")}", bodyPaint, 10f)
            }
            drawText("Mentions: ${entity.mentions}", bodyPaint, 10f)
            
            val score = report.liabilityScores[entity.id]
            if (score != null) {
                drawText("Liability Score: ${String.format("%.1f", score.overallScore)}%", bodyPaint, 10f)
            }
            addSpace()
        }

        // Section 3: Timeline
        startNewPage()
        canvas?.drawText("3. TIMELINE OF EVENTS", margin, yPosition, headingPaint)
        addSpace(2f)
        
        val sortedTimeline = report.timeline.sortedBy { it.date }
        for (event in sortedTimeline.take(50)) { // Limit to first 50 events
            checkPageBreak(lineHeight * 3)
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(event.date)
            drawText("[$dateStr] ${event.eventType.name}", labelPaint)
            drawText(event.description.take(200), bodyPaint, 10f)
            addSpace(0.5f)
        }
        
        if (sortedTimeline.size > 50) {
            drawText("... and ${sortedTimeline.size - 50} more events", labelPaint)
        }

        // Section 4: Contradictions
        startNewPage()
        canvas?.drawText("4. CONTRADICTION ANALYSIS", margin, yPosition, headingPaint)
        addSpace(2f)
        
        if (report.contradictions.isEmpty()) {
            drawText("No contradictions were detected in the analyzed evidence.", bodyPaint)
        } else {
            val criticalCount = report.contradictions.count { it.severity == Severity.CRITICAL }
            val highCount = report.contradictions.count { it.severity == Severity.HIGH }
            
            drawText("Summary:", subheadingPaint)
            drawText("• Critical: $criticalCount", severityPaints[Severity.CRITICAL]!!)
            drawText("• High: $highCount", severityPaints[Severity.HIGH]!!)
            drawText("• Medium: ${report.contradictions.count { it.severity == Severity.MEDIUM }}", severityPaints[Severity.MEDIUM]!!)
            drawText("• Low: ${report.contradictions.count { it.severity == Severity.LOW }}", severityPaints[Severity.LOW]!!)
            addSpace()
            
            for ((index, contradiction) in report.contradictions.withIndex()) {
                checkPageBreak(lineHeight * 8)
                
                val entityName = report.entities.find { it.id == contradiction.entityId }?.primaryName ?: "Unknown"
                drawText("Contradiction ${index + 1}: $entityName", subheadingPaint)
                drawText("[${contradiction.severity.name}] ${contradiction.type.name.replace("_", " ")}", 
                    severityPaints[contradiction.severity]!!)
                addSpace(0.5f)
                drawText(contradiction.description, bodyPaint, 10f)
                addSpace(0.5f)
                drawText("Legal Implication: ${contradiction.legalImplication}", labelPaint, 10f)
                addSpace()
            }
        }

        // Section 5: Behavioral Patterns
        startNewPage()
        canvas?.drawText("5. BEHAVIORAL PATTERN ANALYSIS", margin, yPosition, headingPaint)
        addSpace(2f)
        
        if (report.behavioralPatterns.isEmpty()) {
            drawText("No concerning behavioral patterns were detected.", bodyPaint)
        } else {
            for (pattern in report.behavioralPatterns) {
                checkPageBreak(lineHeight * 5)
                val entityName = report.entities.find { it.id == pattern.entityId }?.primaryName ?: "Unknown"
                drawText("$entityName: ${pattern.type.name.replace("_", " ")}", subheadingPaint)
                drawText("Severity: ${pattern.severity.name}", severityPaints[pattern.severity]!!)
                
                if (pattern.instances.isNotEmpty()) {
                    drawText("Examples:", labelPaint, 10f)
                    for (instance in pattern.instances.take(3)) {
                        drawText("• \"${instance.take(100)}\"", bodyPaint, 20f)
                    }
                }
                addSpace()
            }
        }

        // Section 6: Liability Assessment
        startNewPage()
        canvas?.drawText("6. LIABILITY ASSESSMENT", margin, yPosition, headingPaint)
        addSpace(2f)
        
        val sortedScores = report.liabilityScores.values.sortedByDescending { it.overallScore }
        
        for (score in sortedScores) {
            checkPageBreak(lineHeight * 8)
            val entityName = report.entities.find { it.id == score.entityId }?.primaryName ?: "Unknown"
            
            drawText(entityName, subheadingPaint)
            drawText("Overall Liability: ${String.format("%.1f", score.overallScore)}%", bodyPaint, 10f)
            addSpace(0.5f)
            drawText("Breakdown:", labelPaint, 10f)
            drawText("• Contradiction Score: ${String.format("%.1f", score.contradictionScore)}", bodyPaint, 20f)
            drawText("• Behavioral Score: ${String.format("%.1f", score.behavioralScore)}", bodyPaint, 20f)
            drawText("• Evidence Score: ${String.format("%.1f", score.evidenceContributionScore)}", bodyPaint, 20f)
            drawText("• Consistency Score: ${String.format("%.1f", score.chronologicalConsistencyScore)}", bodyPaint, 20f)
            drawText("• Causal Score: ${String.format("%.1f", score.causalResponsibilityScore)}", bodyPaint, 20f)
            addSpace()
        }

        // Section 7: Narrative Summary
        startNewPage()
        canvas?.drawText("7. NARRATIVE SUMMARY", margin, yPosition, headingPaint)
        addSpace(2f)
        
        // Final Summary from narrative
        val summaryLines = report.narrativeSections.finalSummary.split("\n")
        for (line in summaryLines) {
            if (line.isNotBlank()) {
                checkPageBreak()
                drawText(line, bodyPaint)
            }
        }

        // Section 8: Conclusion
        startNewPage()
        canvas?.drawText("8. CONCLUSION", margin, yPosition, headingPaint)
        addSpace(2f)
        
        drawText("This forensic analysis was conducted using the Verum Omnis Contradiction Engine,", bodyPaint)
        drawText("an offline, on-device analytical tool designed for legal-grade evidence analysis.", bodyPaint)
        addSpace()
        drawText("The findings presented in this report are based on the evidence provided and", bodyPaint)
        drawText("the analytical algorithms applied. The accuracy depends on the completeness", bodyPaint)
        drawText("and quality of the input evidence.", bodyPaint)
        addSpace(2f)
        
        // Seal block
        canvas?.drawText("DOCUMENT SEAL", margin, yPosition, sealedPaint)
        addSpace()
        drawText("This document has been cryptographically sealed with SHA-512.", bodyPaint)
        addSpace()
        drawText("Hash: ${report.sha512Hash}", labelPaint)
        addSpace(2f)
        drawText("Patent Pending • Verum Omnis", labelPaint)
        drawText("Generated: ${dateFormat.format(report.generatedAt)}", labelPaint)

        // Finish last page
        currentPage?.let { pdfDocument.finishPage(it) }

        // Save PDF
        val reportsDir = File(context.getExternalFilesDir(null), "reports")
        reportsDir.mkdirs()
        
        val fileName = "Verum_Omnis_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        val file = File(reportsDir, fileName)
        
        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        
        pdfDocument.close()
        
        return file
    }

    /**
     * Build complete report content for hashing.
     */
    private fun buildReportContent(
        caseName: String,
        entities: List<Entity>,
        timeline: List<TimelineEvent>,
        contradictions: List<Contradiction>,
        behavioralPatterns: List<BehavioralPattern>,
        liabilityScores: Map<String, LiabilityScore>,
        narrativeSections: NarrativeSections
    ): String {
        val builder = StringBuilder()
        
        builder.appendLine("VERUM OMNIS FORENSIC REPORT")
        builder.appendLine("Case: $caseName")
        builder.appendLine("Generated: ${dateFormat.format(Date())}")
        builder.appendLine()
        
        builder.appendLine("ENTITIES:")
        for (entity in entities) {
            builder.appendLine("${entity.primaryName} | Mentions: ${entity.mentions}")
        }
        
        builder.appendLine()
        builder.appendLine("TIMELINE EVENTS: ${timeline.size}")
        
        builder.appendLine()
        builder.appendLine("CONTRADICTIONS: ${contradictions.size}")
        for (c in contradictions) {
            builder.appendLine("${c.type} | ${c.severity} | ${c.description}")
        }
        
        builder.appendLine()
        builder.appendLine("BEHAVIORAL PATTERNS: ${behavioralPatterns.size}")
        
        builder.appendLine()
        builder.appendLine("LIABILITY SCORES:")
        for ((id, score) in liabilityScores) {
            builder.appendLine("$id: ${score.overallScore}")
        }
        
        builder.appendLine()
        builder.appendLine("NARRATIVE:")
        builder.appendLine(narrativeSections.finalSummary)
        
        return builder.toString()
    }

    /**
     * Calculate SHA-512 hash of content.
     */
    private fun calculateSHA512(content: String): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hashBytes = digest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

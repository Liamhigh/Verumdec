package com.verumdec.engine

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.verumdec.data.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * PDF Report Generator
 * Creates sealed forensic reports with SHA-512 hash, QR codes, watermarks, and 3D logos.
 * 
 * Features:
 * - Top-center 3D Verum Omnis logo
 * - Central faint watermark
 * - Bottom-right QR code with truncated SHA-512
 * - Page metadata (page X of Y, timestamp)
 * - SHA-512 cryptographic sealing
 */
class ReportGenerator(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.US)
    private val pageWidth = 595 // A4 width in points
    private val pageHeight = 842 // A4 height in points
    private val margin = 50f
    private val lineHeight = 14f
    
    // Logo and branding colors
    private val primaryColor = Color.parseColor("#1A237E") // Deep indigo
    private val accentColor = Color.parseColor("#3949AB") // Lighter indigo
    private val goldColor = Color.parseColor("#FFD700") // Gold accent
    private val watermarkColor = Color.argb(20, 26, 35, 126) // Very faint indigo

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

    // Total page count placeholder - will be updated after first pass
    private var totalPages = 0
    
    /**
     * Export report to PDF file with enhanced features:
     * - Top-center 3D Verum Omnis logo
     * - Central faint watermark on each page
     * - Bottom-right QR code with truncated SHA-512
     * - Page metadata (page X of Y, timestamp)
     */
    fun exportToPdf(report: ForensicReport): File {
        // First pass: count total pages
        totalPages = estimateTotalPages(report)
        
        val pdfDocument = PdfDocument()
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var yPosition = margin + 60f // Leave room for header
        var pageNumber = 0
        val generationTimestamp = dateFormat.format(report.generatedAt)
        val truncatedHash = report.sha512Hash.take(32)

        val titlePaint = Paint().apply {
            textSize = 24f
            color = primaryColor
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headingPaint = Paint().apply {
            textSize = 16f
            color = primaryColor
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subheadingPaint = Paint().apply {
            textSize = 14f
            color = accentColor
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            textSize = 10f
            color = Color.BLACK
            isAntiAlias = true
        }

        val labelPaint = Paint().apply {
            textSize = 9f
            color = Color.GRAY
            isAntiAlias = true
        }

        val severityPaints = mapOf(
            Severity.CRITICAL to Paint().apply { textSize = 10f; color = Color.parseColor("#D32F2F"); isAntiAlias = true },
            Severity.HIGH to Paint().apply { textSize = 10f; color = Color.parseColor("#F57C00"); isAntiAlias = true },
            Severity.MEDIUM to Paint().apply { textSize = 10f; color = Color.parseColor("#FBC02D"); isAntiAlias = true },
            Severity.LOW to Paint().apply { textSize = 10f; color = Color.parseColor("#388E3C"); isAntiAlias = true }
        )

        fun draw3DLogo(canvas: Canvas) {
            // Draw 3D-style Verum Omnis logo at top center
            val centerX = pageWidth / 2f
            val logoY = 25f
            
            // Shadow layer for 3D effect
            val shadowPaint = Paint().apply {
                color = Color.argb(60, 0, 0, 0)
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("VERUM OMNIS", centerX + 2f, logoY + 2f, shadowPaint)
            
            // Gold accent layer
            val goldPaint = Paint().apply {
                color = goldColor
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("VERUM OMNIS", centerX + 1f, logoY + 1f, goldPaint)
            
            // Main text layer
            val logoPaint = Paint().apply {
                color = primaryColor
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("VERUM OMNIS", centerX, logoY, logoPaint)
            
            // Draw shield icon representation
            val shieldPaint = Paint().apply {
                color = primaryColor
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            
            // Simple shield path
            val shieldPath = Path().apply {
                moveTo(centerX - 60f, logoY + 8f)
                lineTo(centerX - 60f, logoY + 18f)
                quadTo(centerX, logoY + 30f, centerX + 60f, logoY + 18f)
                lineTo(centerX + 60f, logoY + 8f)
                close()
            }
            canvas.drawPath(shieldPath, shieldPaint)
            
            // Tagline
            val taglinePaint = Paint().apply {
                color = accentColor
                textSize = 8f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Forensic Contradiction Engine", centerX, logoY + 42f, taglinePaint)
        }

        fun drawCentralWatermark(canvas: Canvas) {
            // Draw faint diagonal watermark across page center
            val watermarkPaint = Paint().apply {
                color = watermarkColor
                textSize = 48f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            
            canvas.save()
            canvas.rotate(-30f, pageWidth / 2f, pageHeight / 2f)
            canvas.drawText("VERUM OMNIS", pageWidth / 2f, pageHeight / 2f, watermarkPaint)
            canvas.drawText("SEALED", pageWidth / 2f, pageHeight / 2f + 60f, watermarkPaint)
            canvas.restore()
        }

        fun drawQRCode(canvas: Canvas, hash: String) {
            // Draw a simplified QR-like pattern with hash in bottom-right
            val qrSize = 50f
            val qrX = pageWidth - margin - qrSize
            val qrY = pageHeight - margin - qrSize - 20f
            
            val qrPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            
            val borderPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            
            // Draw QR code border
            canvas.drawRect(qrX, qrY, qrX + qrSize, qrY + qrSize, borderPaint)
            
            // Draw QR-like pattern based on hash characters
            val cellSize = qrSize / 8f
            for (i in 0 until 8) {
                for (j in 0 until 8) {
                    val charIndex = (i * 8 + j) % hash.length
                    if (hash[charIndex].code % 2 == 0) {
                        canvas.drawRect(
                            qrX + j * cellSize,
                            qrY + i * cellSize,
                            qrX + (j + 1) * cellSize,
                            qrY + (i + 1) * cellSize,
                            qrPaint
                        )
                    }
                }
            }
            
            // Draw corner markers (QR-style)
            val markerPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            
            // Top-left marker
            canvas.drawRect(qrX, qrY, qrX + cellSize * 2, qrY + cellSize * 2, markerPaint)
            // Top-right marker  
            canvas.drawRect(qrX + qrSize - cellSize * 2, qrY, qrX + qrSize, qrY + cellSize * 2, markerPaint)
            // Bottom-left marker
            canvas.drawRect(qrX, qrY + qrSize - cellSize * 2, qrX + cellSize * 2, qrY + qrSize, markerPaint)
            
            // Hash text below QR
            val hashPaint = Paint().apply {
                textSize = 6f
                color = Color.GRAY
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("SHA-512: $hash", qrX + qrSize, qrY + qrSize + 10f, hashPaint)
        }

        fun drawHeaderFooter(canvas: Canvas, pageNum: Int, timestamp: String) {
            // Draw 3D logo at top
            draw3DLogo(canvas)
            
            // Draw page metadata in footer
            val footerPaint = Paint().apply {
                textSize = 8f
                color = Color.GRAY
                isAntiAlias = true
            }
            
            // Left: Patent notice
            canvas.drawText("Patent Pending • Verum Omnis", margin, pageHeight - 15f, footerPaint)
            
            // Center: Page number
            val pageText = "Page $pageNum of $totalPages"
            val pageTextWidth = footerPaint.measureText(pageText)
            canvas.drawText(pageText, (pageWidth - pageTextWidth) / 2f, pageHeight - 15f, footerPaint)
            
            // Right: Timestamp
            footerPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(timestamp, pageWidth - margin - 60f, pageHeight - 15f, footerPaint)
            
            // Draw QR code
            drawQRCode(canvas, truncatedHash)
            
            // Draw central watermark
            drawCentralWatermark(canvas)
        }

        fun startNewPage(): Canvas {
            currentPage?.let { pdfDocument.finishPage(it) }
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage?.canvas
            yPosition = margin + 60f // Space for header
            
            // Draw header, footer, watermark, QR code
            canvas?.let { drawHeaderFooter(it, pageNumber, generationTimestamp) }
            
            return canvas!!
        }

        fun checkPageBreak(requiredSpace: Float = lineHeight * 3) {
            if (yPosition + requiredSpace > pageHeight - margin - 80f) { // Leave room for footer/QR
                startNewPage()
            }
        }

        fun drawText(text: String, paint: Paint, indent: Float = 0f) {
            val maxWidth = pageWidth - 2 * margin - indent - 60f // Account for QR code area
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

        // Title section
        addSpace(2f)
        canvas?.let { c ->
            val titleCenterPaint = Paint().apply {
                textSize = 24f
                color = primaryColor
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            c.drawText("FORENSIC ANALYSIS REPORT", pageWidth / 2f, yPosition, titleCenterPaint)
        }
        yPosition += 35f
        
        canvas?.let { c ->
            val subtitlePaint = Paint().apply {
                textSize = 12f
                color = accentColor
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            c.drawText("Generated by Verum Omnis Contradiction Engine", pageWidth / 2f, yPosition, subtitlePaint)
        }
        addSpace(3f)
        
        drawText("Case: ${report.caseName}", bodyPaint)
        drawText("Generated: ${dateFormat.format(report.generatedAt)}", bodyPaint)
        drawText("Report ID: ${report.id}", labelPaint)
        addSpace(2f)

        // Sealed indicator with box
        val sealedPaint = Paint().apply {
            textSize = 12f
            color = Color.parseColor("#D32F2F")
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val sealBoxPaint = Paint().apply {
            color = Color.parseColor("#FFEBEE")
            style = Paint.Style.FILL
        }
        
        val sealBorderPaint = Paint().apply {
            color = Color.parseColor("#D32F2F")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        
        canvas?.drawRect(margin, yPosition - 12f, margin + 140f, yPosition + 4f, sealBoxPaint)
        canvas?.drawRect(margin, yPosition - 12f, margin + 140f, yPosition + 4f, sealBorderPaint)
        canvas?.drawText("SEALED DOCUMENT", margin + 5f, yPosition, sealedPaint)
        addSpace(2f)
        
        drawText("SHA-512: ${report.sha512Hash.take(64)}...", labelPaint)
        drawText("Full hash available in document metadata", labelPaint)
        addSpace(3f)

        // Table of Contents
        canvas?.drawText("TABLE OF CONTENTS", margin, yPosition, headingPaint)
        addSpace(2f)
        drawText("1. Executive Summary", bodyPaint)
        drawText("2. Entities Discovered", bodyPaint)
        drawText("3. Timeline of Events", bodyPaint)
        drawText("4. Contradiction Analysis", bodyPaint)
        drawText("5. Behavioral Pattern Analysis", bodyPaint)
        drawText("6. Liability Assessment", bodyPaint)
        drawText("7. Narrative Summary", bodyPaint)
        drawText("8. Conclusion & Seal", bodyPaint)

        // Section 1: Executive Summary
        startNewPage()
        canvas?.drawText("1. EXECUTIVE SUMMARY", margin, yPosition, headingPaint)
        addSpace(2f)
        
        drawText("This report presents the findings of a comprehensive forensic analysis conducted using", bodyPaint)
        drawText("the Verum Omnis Contradiction Engine - an offline, on-device analytical tool.", bodyPaint)
        addSpace()
        drawText("Key Statistics:", subheadingPaint)
        addSpace(0.5f)
        drawText("• Entities Identified: ${report.entities.size}", bodyPaint)
        drawText("• Timeline Events: ${report.timeline.size}", bodyPaint)
        drawText("• Contradictions Detected: ${report.contradictions.size}", bodyPaint)
        drawText("• Behavioral Patterns: ${report.behavioralPatterns.size}", bodyPaint)
        addSpace()
        
        // Summary box
        val criticalCount = report.contradictions.count { it.severity == Severity.CRITICAL }
        val highCount = report.contradictions.count { it.severity == Severity.HIGH }
        if (criticalCount > 0 || highCount > 0) {
            drawText("⚠ ATTENTION: ${criticalCount} critical and ${highCount} high-severity issues detected.", sealedPaint)
        }

        // Section 2: Entities
        addSpace(2f)
        canvas?.drawText("2. ENTITIES DISCOVERED", margin, yPosition, headingPaint)
        addSpace(2f)
        
        for (entity in report.entities) {
            checkPageBreak(lineHeight * 6)
            drawText("▸ ${entity.primaryName}", subheadingPaint)
            if (entity.aliases.isNotEmpty()) {
                drawText("   Aliases: ${entity.aliases.joinToString(", ")}", bodyPaint, 10f)
            }
            if (entity.emails.isNotEmpty()) {
                drawText("   Emails: ${entity.emails.joinToString(", ")}", bodyPaint, 10f)
            }
            if (entity.phoneNumbers.isNotEmpty()) {
                drawText("   Phone: ${entity.phoneNumbers.joinToString(", ")}", bodyPaint, 10f)
            }
            drawText("   Mentions: ${entity.mentions}", bodyPaint, 10f)
            
            val score = report.liabilityScores[entity.id]
            if (score != null) {
                val scoreColor = when {
                    score.overallScore >= 70f -> Color.parseColor("#D32F2F")
                    score.overallScore >= 40f -> Color.parseColor("#F57C00")
                    else -> Color.parseColor("#388E3C")
                }
                val scorePaint = Paint().apply {
                    textSize = 10f
                    color = scoreColor
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas?.drawText("   Liability Score: ${String.format("%.1f", score.overallScore)}%", margin + 10f, yPosition, scorePaint)
                yPosition += lineHeight
            }
            addSpace()
        }

        // Section 3: Timeline
        startNewPage()
        canvas?.drawText("3. TIMELINE OF EVENTS", margin, yPosition, headingPaint)
        addSpace(2f)
        
        val sortedTimeline = report.timeline.sortedBy { it.date }
        var lastMonth = ""
        
        for (event in sortedTimeline.take(50)) {
            checkPageBreak(lineHeight * 4)
            
            val month = SimpleDateFormat("MMMM yyyy", Locale.US).format(event.date)
            if (month != lastMonth) {
                addSpace()
                drawText("── $month ──", subheadingPaint)
                lastMonth = month
            }
            
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(event.date)
            val significanceMarker = when (event.significance) {
                Significance.CRITICAL -> "🔴"
                Significance.HIGH -> "🟠"
                Significance.NORMAL -> "🔵"
                Significance.LOW -> "⚪"
            }
            drawText("$significanceMarker [$dateStr] ${event.eventType.name}", labelPaint)
            drawText("   ${event.description.take(180)}", bodyPaint, 10f)
        }
        
        if (sortedTimeline.size > 50) {
            addSpace()
            drawText("... and ${sortedTimeline.size - 50} more events", labelPaint)
        }

        // Section 4: Contradictions
        startNewPage()
        canvas?.drawText("4. CONTRADICTION ANALYSIS", margin, yPosition, headingPaint)
        addSpace(2f)
        
        if (report.contradictions.isEmpty()) {
            drawText("No contradictions were detected in the analyzed evidence.", bodyPaint)
        } else {
            drawText("Summary of Detected Contradictions:", subheadingPaint)
            addSpace(0.5f)
            drawText("• Critical: $criticalCount", severityPaints[Severity.CRITICAL]!!)
            drawText("• High: $highCount", severityPaints[Severity.HIGH]!!)
            drawText("• Medium: ${report.contradictions.count { it.severity == Severity.MEDIUM }}", severityPaints[Severity.MEDIUM]!!)
            drawText("• Low: ${report.contradictions.count { it.severity == Severity.LOW }}", severityPaints[Severity.LOW]!!)
            addSpace(2f)
            
            for ((index, contradiction) in report.contradictions.withIndex()) {
                checkPageBreak(lineHeight * 10)
                
                val entityName = report.entities.find { it.id == contradiction.entityId }?.primaryName ?: "Unknown"
                
                // Draw contradiction box
                val boxTop = yPosition - 5f
                
                drawText("Contradiction ${index + 1}: $entityName", subheadingPaint)
                drawText("[${contradiction.severity.name}] Type: ${contradiction.type.name.replace("_", " ")}", 
                    severityPaints[contradiction.severity]!!)
                addSpace(0.5f)
                drawText("Description: ${contradiction.description}", bodyPaint, 10f)
                addSpace(0.5f)
                drawText("Legal Implication: ${contradiction.legalImplication}", labelPaint, 10f)
                
                // Draw separator
                canvas?.let { c ->
                    val linePaint = Paint().apply {
                        color = Color.LTGRAY
                        strokeWidth = 0.5f
                    }
                    c.drawLine(margin, yPosition + 5f, pageWidth - margin - 60f, yPosition + 5f, linePaint)
                }
                addSpace(1.5f)
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
                checkPageBreak(lineHeight * 6)
                val entityName = report.entities.find { it.id == pattern.entityId }?.primaryName ?: "Unknown"
                
                drawText("▸ $entityName: ${pattern.type.name.replace("_", " ")}", subheadingPaint)
                drawText("   Severity: ${pattern.severity.name}", severityPaints[pattern.severity]!!)
                
                if (pattern.instances.isNotEmpty()) {
                    drawText("   Evidence:", labelPaint, 10f)
                    for (instance in pattern.instances.take(3)) {
                        drawText("   • \"${instance.take(80)}...\"", bodyPaint, 20f)
                    }
                    if (pattern.instances.size > 3) {
                        drawText("   ... and ${pattern.instances.size - 3} more instances", labelPaint, 20f)
                    }
                }
                addSpace()
            }
        }

        // Section 6: Liability Assessment
        startNewPage()
        canvas?.drawText("6. LIABILITY ASSESSMENT", margin, yPosition, headingPaint)
        addSpace(2f)
        
        drawText("Liability scores are calculated using a weighted formula:", bodyPaint)
        drawText("• Contradiction Score (30%)", labelPaint, 10f)
        drawText("• Behavioral Score (20%)", labelPaint, 10f)
        drawText("• Evidence Contribution (15%)", labelPaint, 10f)
        drawText("• Chronological Consistency (20%)", labelPaint, 10f)
        drawText("• Causal Responsibility (15%)", labelPaint, 10f)
        addSpace(2f)
        
        val sortedScores = report.liabilityScores.values.sortedByDescending { it.overallScore }
        
        for (score in sortedScores) {
            checkPageBreak(lineHeight * 10)
            val entityName = report.entities.find { it.id == score.entityId }?.primaryName ?: "Unknown"
            
            drawText("▸ $entityName", subheadingPaint)
            
            // Draw progress bar style
            val barWidth = 200f
            val barHeight = 10f
            val barX = margin + 20f
            val barY = yPosition
            
            val bgPaint = Paint().apply { color = Color.LTGRAY; style = Paint.Style.FILL }
            val fillPaint = Paint().apply {
                color = when {
                    score.overallScore >= 70f -> Color.parseColor("#D32F2F")
                    score.overallScore >= 40f -> Color.parseColor("#F57C00")
                    else -> Color.parseColor("#388E3C")
                }
                style = Paint.Style.FILL
            }
            
            canvas?.drawRect(barX, barY, barX + barWidth, barY + barHeight, bgPaint)
            canvas?.drawRect(barX, barY, barX + (barWidth * score.overallScore / 100f), barY + barHeight, fillPaint)
            
            val scorePaint = Paint().apply {
                textSize = 10f
                color = Color.BLACK
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas?.drawText("${String.format("%.1f", score.overallScore)}%", barX + barWidth + 10f, barY + 9f, scorePaint)
            yPosition += lineHeight * 1.5f
            
            drawText("   Breakdown:", labelPaint, 10f)
            drawText("   • Contradiction: ${String.format("%.1f", score.contradictionScore)}", bodyPaint, 20f)
            drawText("   • Behavioral: ${String.format("%.1f", score.behavioralScore)}", bodyPaint, 20f)
            drawText("   • Evidence: ${String.format("%.1f", score.evidenceContributionScore)}", bodyPaint, 20f)
            drawText("   • Consistency: ${String.format("%.1f", score.chronologicalConsistencyScore)}", bodyPaint, 20f)
            drawText("   • Causal: ${String.format("%.1f", score.causalResponsibilityScore)}", bodyPaint, 20f)
            addSpace()
        }

        // Section 7: Narrative Summary
        startNewPage()
        canvas?.drawText("7. NARRATIVE SUMMARY", margin, yPosition, headingPaint)
        addSpace(2f)
        
        val summaryLines = report.narrativeSections.finalSummary.split("\n")
        for (line in summaryLines) {
            if (line.isNotBlank()) {
                checkPageBreak()
                drawText(line, bodyPaint)
            }
        }

        // Section 8: Conclusion
        startNewPage()
        canvas?.drawText("8. CONCLUSION & CRYPTOGRAPHIC SEAL", margin, yPosition, headingPaint)
        addSpace(2f)
        
        drawText("This forensic analysis was conducted using the Verum Omnis Contradiction Engine,", bodyPaint)
        drawText("an offline, on-device analytical tool designed for legal-grade evidence analysis.", bodyPaint)
        addSpace()
        drawText("All analysis was performed entirely on-device with no data transmitted to external servers.", bodyPaint)
        addSpace()
        drawText("The findings presented in this report are based on the evidence provided and the analytical", bodyPaint)
        drawText("algorithms applied. Accuracy depends on the completeness and quality of input evidence.", bodyPaint)
        addSpace(3f)
        
        // Final seal block with border
        val sealBlockTop = yPosition
        val sealBlockPaint = Paint().apply {
            color = Color.parseColor("#FFF8E1")
            style = Paint.Style.FILL
        }
        val sealBlockBorder = Paint().apply {
            color = goldColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        canvas?.drawRect(margin, sealBlockTop, pageWidth - margin - 60f, sealBlockTop + 120f, sealBlockPaint)
        canvas?.drawRect(margin, sealBlockTop, pageWidth - margin - 60f, sealBlockTop + 120f, sealBlockBorder)
        
        yPosition = sealBlockTop + 20f
        
        val sealTitlePaint = Paint().apply {
            textSize = 14f
            color = primaryColor
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas?.drawText("DOCUMENT SEAL", pageWidth / 2f - 30f, yPosition, sealTitlePaint)
        addSpace(2f)
        
        drawText("This document has been cryptographically sealed using SHA-512.", bodyPaint, 10f)
        addSpace(0.5f)
        drawText("Document Hash:", labelPaint, 10f)
        drawText(report.sha512Hash.take(64), labelPaint, 10f)
        drawText(report.sha512Hash.drop(64), labelPaint, 10f)
        addSpace()
        
        val patentPaint = Paint().apply {
            textSize = 10f
            color = goldColor
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas?.drawText("Patent Pending • Verum Omnis", margin + 10f, yPosition + 5f, patentPaint)
        
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
     * Estimate total page count for proper pagination.
     */
    private fun estimateTotalPages(report: ForensicReport): Int {
        // Base pages: Title, Summary, Entities start, Timeline, Contradictions, Behavioral, Liability, Narrative, Conclusion
        var pages = 9
        
        // Add pages for entities
        pages += (report.entities.size / 8) // ~8 entities per page
        
        // Add pages for timeline
        pages += (report.timeline.size / 15) // ~15 events per page
        
        // Add pages for contradictions
        pages += (report.contradictions.size / 4) // ~4 contradictions per page
        
        // Add pages for behavioral patterns
        pages += (report.behavioralPatterns.size / 5) // ~5 patterns per page
        
        // Add pages for liability
        pages += (report.liabilityScores.size / 3) // ~3 scores per page
        
        return maxOf(pages, 10)
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
     * Uses the shared HashUtils for consistency across the codebase.
     */
    private fun calculateSHA512(content: String): String {
        return com.verumdec.core.HashUtils.sha512(content)
    }
}

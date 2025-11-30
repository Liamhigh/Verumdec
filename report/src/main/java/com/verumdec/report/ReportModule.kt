package com.verumdec.report

import android.content.Context
import android.graphics.Bitmap
import com.verumdec.report.pdf.PDFBuilder
import com.verumdec.report.util.FileUtils
import java.io.File

/**
 * Report Module - PDF Report Generation and Cryptographic Sealing
 *
 * This module handles PDF report generation and cryptographic sealing.
 * It produces court-ready documents with full narrative, timeline, and liability analysis.
 *
 * ## Key Responsibilities:
 * - Generate narrative layers (Objective, Contradiction, Behavioral, Deductive, Causal)
 * - Create final sealed PDF reports
 * - Apply SHA-512 hash sealing for integrity
 * - Embed Verum watermark and metadata
 * - Generate QR codes for verification
 * - Add "Patent Pending Verum Omnis" blocks
 *
 * ## Pipeline Stages:
 * - Stage 7: NARRATIVE GENERATION
 * - Stage 8: THE FINAL SEALED REPORT (PDF)
 *
 * ## Features:
 * - PDFBox integration for PDF generation
 * - Template-based report formatting
 * - Cryptographic sealing (SHA-512)
 * - Watermarking and branding (12-16% opacity)
 * - QR code generation with ZXing
 * - Metadata embedding
 * - Multi-page document support
 * - Automatic headers and footers
 *
 * ## Narrative Layers:
 * - A. Objective Narration Layer (clean chronological account)
 * - B. Contradiction Commentary Layer (flags and divergences)
 * - C. Behavioural Pattern Layer (manipulation patterns)
 * - D. Deductive Logic Layer (why contradictions matter)
 * - E. Causal Chain Layer (cause -> effect linking)
 * - F. Final Narrative (merged comprehensive story)
 *
 * ## Report Contents:
 * - Title
 * - Entities
 * - Timeline
 * - Contradictions
 * - Behavioural analysis
 * - Liability matrix
 * - Full narrative
 * - Sealed SHA-512 hash
 * - Verum watermark
 * - Footer with metadata
 * - QR code with SHA-512 + case ID
 * - "Patent Pending Verum Omnis" block
 *
 * @see com.verumdec.core.CoreModule
 * @see com.verumdec.analysis.AnalysisModule
 * @see com.verumdec.timeline.TimelineModule
 * @see com.verumdec.entity.EntityModule
 */
object ReportModule {

    /**
     * Module version for tracking compatibility
     */
    const val VERSION = "1.0.0"

    /**
     * Module name identifier
     */
    const val NAME = "report"

    private var isInitialized = false

    /**
     * Initialize the Report module.
     * Must be called before using PDF generation features.
     *
     * @param context Android context for initializing PDFBox
     */
    fun initialize(context: Context) {
        if (!isInitialized) {
            PDFBuilder.initialize(context)
            isInitialized = true
        }
    }

    /**
     * Check if the module is initialized.
     *
     * @return True if initialized, false otherwise
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Generate a narrative from analysis results.
     *
     * @param timeline Master timeline events
     * @param contradictions Detected contradictions
     * @param behavioralAnalysis Behavioral analysis results
     * @param liabilityScores Entity liability scores
     * @return Generated narrative text
     */
    fun generateNarrative(
        timeline: List<Any>,
        contradictions: List<Any>,
        behavioralAnalysis: Map<String, Any>,
        liabilityScores: Map<String, Int>
    ): String {
        val builder = StringBuilder()

        builder.appendLine("FORENSIC ANALYSIS NARRATIVE")
        builder.appendLine("=" .repeat(50))
        builder.appendLine()

        // Objective Narration Layer
        builder.appendLine("A. OBJECTIVE NARRATION")
        builder.appendLine("-".repeat(30))
        if (timeline.isNotEmpty()) {
            builder.appendLine("Timeline contains ${timeline.size} documented events.")
        } else {
            builder.appendLine("No timeline events recorded.")
        }
        builder.appendLine()

        // Contradiction Commentary Layer
        builder.appendLine("B. CONTRADICTION ANALYSIS")
        builder.appendLine("-".repeat(30))
        if (contradictions.isNotEmpty()) {
            builder.appendLine("${contradictions.size} contradictions detected in the evidence.")
        } else {
            builder.appendLine("No contradictions detected.")
        }
        builder.appendLine()

        // Behavioral Pattern Layer
        builder.appendLine("C. BEHAVIORAL PATTERNS")
        builder.appendLine("-".repeat(30))
        if (behavioralAnalysis.isNotEmpty()) {
            behavioralAnalysis.forEach { (key, value) ->
                builder.appendLine("- $key: $value")
            }
        } else {
            builder.appendLine("No significant behavioral patterns identified.")
        }
        builder.appendLine()

        // Liability Analysis
        builder.appendLine("D. LIABILITY ASSESSMENT")
        builder.appendLine("-".repeat(30))
        if (liabilityScores.isNotEmpty()) {
            liabilityScores.forEach { (entity, score) ->
                builder.appendLine("- $entity: $score%")
            }
        } else {
            builder.appendLine("No liability scores calculated.")
        }

        return builder.toString()
    }

    /**
     * Generate a sealed PDF report.
     *
     * @param context Android context
     * @param caseId The case identifier
     * @param caseName The case name for display
     * @param narrative The narrative content to include
     * @param logoBitmap Optional logo bitmap (will use default watermark if null)
     * @return Pair of (saved file, SHA-512 hash) or null if generation fails
     */
    fun generateSealedReport(
        context: Context,
        caseId: String,
        caseName: String,
        narrative: String,
        logoBitmap: Bitmap? = null
    ): Pair<File, String>? {
        if (!isInitialized) {
            initialize(context)
        }

        return try {
            val builder = PDFBuilder(context)
                .startDocument("Forensic Report: $caseName", caseId)
                .setLogo(logoBitmap)
                .useDefaultWatermark()
                .addPage()
                .drawLogo()

            // Parse narrative into sections
            val sections = parseNarrativeSections(narrative)

            sections.forEach { (title, content) ->
                builder.addSectionTitle(title)
                builder.addParagraph(content)
                builder.addSpace()
            }

            builder.buildAndSave()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate a comprehensive sealed report from forensic analysis data.
     *
     * @param context Android context
     * @param caseId The case identifier
     * @param caseName The case name
     * @param entities List of entity names
     * @param timeline Timeline events as strings
     * @param contradictions Contradiction descriptions
     * @param behavioralPatterns Behavioral pattern descriptions
     * @param liabilityScores Entity liability scores
     * @param logoBitmap Optional logo bitmap
     * @return Pair of (saved file, SHA-512 hash) or null if generation fails
     */
    fun generateComprehensiveReport(
        context: Context,
        caseId: String,
        caseName: String,
        entities: List<String>,
        timeline: List<String>,
        contradictions: List<String>,
        behavioralPatterns: List<String>,
        liabilityScores: Map<String, Int>,
        logoBitmap: Bitmap? = null
    ): Pair<File, String>? {
        if (!isInitialized) {
            initialize(context)
        }

        return try {
            val builder = PDFBuilder(context)
                .startDocument("Forensic Report: $caseName", caseId)
                .setLogo(logoBitmap)
                .useDefaultWatermark()
                .addPage()
                .drawLogo()

            // Title section
            builder.addSectionTitle("VERUM OMNIS FORENSIC ANALYSIS REPORT")
            builder.addKeyValue("Case ID", caseId)
            builder.addKeyValue("Case Name", caseName)
            builder.addSeparator()
            builder.addSpace()

            // Entities section
            if (entities.isNotEmpty()) {
                builder.addSectionTitle("IDENTIFIED ENTITIES")
                entities.forEach { entity ->
                    builder.addBulletPoint(entity)
                }
                builder.addSpace()
            }

            // Timeline section
            if (timeline.isNotEmpty()) {
                builder.addSectionTitle("CHRONOLOGICAL TIMELINE")
                timeline.forEach { event ->
                    builder.addBulletPoint(event)
                }
                builder.addSpace()
            }

            // Contradictions section
            if (contradictions.isNotEmpty()) {
                builder.addSectionTitle("DETECTED CONTRADICTIONS")
                contradictions.forEach { contradiction ->
                    builder.addBulletPoint(contradiction)
                }
                builder.addSpace()
            }

            // Behavioral patterns section
            if (behavioralPatterns.isNotEmpty()) {
                builder.addSectionTitle("BEHAVIORAL PATTERNS")
                behavioralPatterns.forEach { pattern ->
                    builder.addBulletPoint(pattern)
                }
                builder.addSpace()
            }

            // Liability scores section
            if (liabilityScores.isNotEmpty()) {
                builder.addSectionTitle("LIABILITY ASSESSMENT")
                liabilityScores.forEach { (entity, score) ->
                    builder.addKeyValue(entity, "$score%")
                }
                builder.addSpace()
            }

            builder.addSeparator()
            builder.addParagraph("This document has been cryptographically sealed. " +
                    "The QR code in the bottom-right corner contains the SHA-512 hash " +
                    "and case ID for verification purposes.")

            builder.buildAndSave()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Verify the integrity of a sealed report.
     *
     * @param context Android context
     * @param pdfFile The PDF file to verify
     * @return True if the hash matches, false otherwise
     */
    fun verifySealedReport(context: Context, pdfFile: File): Boolean {
        return FileUtils.verifySealedReport(context, pdfFile)
    }

    /**
     * Get all sealed reports.
     *
     * @param context Android context
     * @return List of sealed PDF files
     */
    fun getSealedReports(context: Context): List<File> {
        return FileUtils.getSealedReports(context)
    }

    /**
     * Parse narrative text into sections.
     */
    private fun parseNarrativeSections(narrative: String): List<Pair<String, String>> {
        val sections = mutableListOf<Pair<String, String>>()

        val lines = narrative.lines()
        var currentTitle = ""
        var currentContent = StringBuilder()

        for (line in lines) {
            when {
                line.startsWith("=") || line.startsWith("-") -> {
                    // Skip separator lines
                }
                line.matches(Regex("^[A-Z]\\..+")) -> {
                    // New section title (e.g., "A. OBJECTIVE NARRATION")
                    if (currentTitle.isNotEmpty()) {
                        sections.add(Pair(currentTitle, currentContent.toString().trim()))
                    }
                    currentTitle = line.substringAfter(". ").trim()
                    currentContent = StringBuilder()
                }
                line.isNotEmpty() && currentTitle.isNotEmpty() -> {
                    currentContent.appendLine(line)
                }
                line.isNotEmpty() && currentTitle.isEmpty() -> {
                    // Title or header
                    currentTitle = line.trim()
                }
            }
        }

        // Add last section
        if (currentTitle.isNotEmpty()) {
            sections.add(Pair(currentTitle, currentContent.toString().trim()))
        }

        return sections
    }
}

package com.verumdec.report

/**
 * Report Module - PDF Report Generation and Cryptographic Sealing
 *
 * This module handles the generation of sealed forensic reports.
 * Reports are cryptographically signed with SHA-512 for integrity.
 *
 * ## Features:
 * - Multi-layer narrative generation
 * - PDF report creation with branding
 * - SHA-512 cryptographic sealing
 * - QR code embedding
 * - Watermark rendering
 *
 * ## Narrative Layers:
 * - A. Objective Narration Layer (clean chronological account)
 * - B. Contradiction Commentary Layer (flags and divergences)
 * - C. Behavioural Pattern Layer (manipulation patterns)
 * - D. Deductive Logic Layer (why contradictions matter)
 * - E. Causal Chain Layer (cause -> effect linking)
 * - F. Final Narrative (merged comprehensive story)
 *
 * @see PDFBuilder
 * @see HeaderFooterRenderer
 * @see NarrativeBuilder
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
    
    /**
     * Report format constants
     */
    object Format {
        const val PAGE_WIDTH = 595 // A4 width in points
        const val PAGE_HEIGHT = 842 // A4 height in points
        const val MARGIN = 50f
        const val LINE_HEIGHT = 14f
    }

    /**
     * Initialize the Report module.
     */
    fun initialize() {
        // Report module initialization
    }

    /**
     * Get module information.
     */
    fun getInfo(): ModuleInfo {
        return ModuleInfo(
            name = NAME,
            version = VERSION,
            components = listOf("PDFBuilder", "HeaderFooterRenderer", "NarrativeBuilder", "QRCodeGenerator")
        )
    }
    
    data class ModuleInfo(
        val name: String,
        val version: String,
        val components: List<String>
    )
}

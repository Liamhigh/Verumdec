package com.verumdec.pdf

/**
 * PDF Module - PDF Processing and Metadata Extraction
 *
 * This module handles PDF document processing using PDFBox.
 * It extracts text, metadata, and structural information.
 *
 * ## Pipeline Stage: 1 - INPUT LAYER (Evidence Ingestion)
 *
 * ## Capabilities:
 * - Text extraction from PDFs
 * - Metadata parsing (dates, author, creation info)
 * - Embedded image detection
 * - Form field extraction
 *
 * @see PDFTextExtractor
 * @see PDFMetadataParser
 */
object PdfModule {

    /**
     * Module version for tracking compatibility
     */
    const val VERSION = "1.0.0"

    /**
     * Module name identifier
     */
    const val NAME = "pdf"

    /**
     * Initialize the PDF module.
     */
    fun initialize() {
        // PDF module initialization
    }

    /**
     * Get module information.
     */
    fun getInfo(): ModuleInfo {
        return ModuleInfo(
            name = NAME,
            version = VERSION,
            components = listOf("PDFTextExtractor", "PDFMetadataParser")
        )
    }
    
    data class ModuleInfo(
        val name: String,
        val version: String,
        val components: List<String>
    )
}

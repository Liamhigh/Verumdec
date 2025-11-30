package com.verumdec.ocr

/**
 * OCR Module - Text Recognition from Images
 *
 * This module handles optical character recognition using ML Kit.
 * It extracts text from images and scanned documents.
 *
 * ## Pipeline Stage: 1 - INPUT LAYER (Evidence Ingestion)
 *
 * ## Capabilities:
 * - Text extraction from images
 * - Scanned document processing
 * - Multi-language support
 * - Confidence scoring
 *
 * @see TextRecognizer
 * @see ImagePreprocessor
 */
object OcrModule {

    /**
     * Module version for tracking compatibility
     */
    const val VERSION = "1.0.0"

    /**
     * Module name identifier
     */
    const val NAME = "ocr"

    /**
     * Initialize the OCR module.
     */
    fun initialize() {
        // OCR module initialization
    }

    /**
     * Get module information.
     */
    fun getInfo(): ModuleInfo {
        return ModuleInfo(
            name = NAME,
            version = VERSION,
            components = listOf("TextRecognizer", "ImagePreprocessor")
        )
    }
    
    data class ModuleInfo(
        val name: String,
        val version: String,
        val components: List<String>
    )
}

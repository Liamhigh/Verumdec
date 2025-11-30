package com.verumdec.core

/**
 * Core Module - Foundation for Verumdec Offline Contradiction Engine
 *
 * This module provides the foundational components for the entire engine.
 * It contains shared utilities, data models, and base interfaces.
 *
 * ## Key Components:
 * - FileUtils: File operations and type detection
 * - HashUtils: SHA-512/256 cryptographic hashing
 * - MetadataUtils: Date parsing and metadata extraction
 *
 * ## Usage:
 * ```kotlin
 * // File utilities
 * val isImage = FileUtils.isImageFile("photo.jpg")
 * val mimeType = FileUtils.getMimeType("document.pdf")
 * 
 * // Hash utilities
 * val hash = HashUtils.sha512("content to hash")
 * val isValid = HashUtils.verifySha512(content, expectedHash)
 * 
 * // Metadata utilities
 * val date = MetadataUtils.parseDate("2024-01-15")
 * val emails = MetadataUtils.extractAllEmails(text)
 * ```
 *
 * @see FileUtils
 * @see HashUtils
 * @see MetadataUtils
 */
object CoreModule {

    /**
     * Module version for tracking compatibility
     */
    const val VERSION = "1.0.0"

    /**
     * Module name identifier
     */
    const val NAME = "core"
    
    /**
     * Build number
     */
    const val BUILD = 1

    /**
     * Initialize the core module.
     * Should be called before using any other modules.
     */
    fun initialize() {
        // Core module is stateless, no initialization needed
    }
    
    /**
     * Get module information.
     */
    fun getInfo(): ModuleInfo {
        return ModuleInfo(
            name = NAME,
            version = VERSION,
            build = BUILD,
            components = listOf("FileUtils", "HashUtils", "MetadataUtils")
        )
    }
    
    /**
     * Module information data class.
     */
    data class ModuleInfo(
        val name: String,
        val version: String,
        val build: Int,
        val components: List<String>
    )
}

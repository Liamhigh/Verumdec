package com.verumdec.report

import java.text.SimpleDateFormat
import java.util.*

/**
 * Header and Footer Renderer for PDF Reports
 * 
 * Handles rendering of page headers, footers, watermarks, QR codes, and logos.
 */
object HeaderFooterRenderer {
    
    /**
     * Header configuration
     */
    data class HeaderConfig(
        val showLogo: Boolean = true,
        val logoStyle: LogoStyle = LogoStyle.FULL_3D,
        val showTagline: Boolean = true,
        val tagline: String = "Forensic Contradiction Engine"
    )
    
    /**
     * Footer configuration
     */
    data class FooterConfig(
        val showPageNumber: Boolean = true,
        val showTimestamp: Boolean = true,
        val showPatentNotice: Boolean = true,
        val patentNotice: String = "Patent Pending • Verum Omnis"
    )
    
    /**
     * Watermark configuration
     */
    data class WatermarkConfig(
        val enabled: Boolean = true,
        val text: String = "VERUM OMNIS\nSEALED",
        val rotation: Float = -30f,
        val opacity: Int = 20 // 0-255
    )
    
    /**
     * QR code configuration
     */
    data class QRCodeConfig(
        val enabled: Boolean = true,
        val size: Float = 50f,
        val showHashLabel: Boolean = true,
        val hashPrefixLength: Int = 32
    )
    
    /**
     * Logo rendering style
     */
    enum class LogoStyle {
        SIMPLE,     // Just text
        FULL_3D,    // Text with shadow and 3D effect
        WITH_SHIELD // Text with shield icon
    }
    
    /**
     * Page metadata for headers/footers
     */
    data class PageMetadata(
        val pageNumber: Int,
        val totalPages: Int,
        val generationTimestamp: Date,
        val documentHash: String
    )
    
    /**
     * Get header height based on configuration.
     */
    fun getHeaderHeight(config: HeaderConfig): Float {
        var height = 20f // Base padding
        if (config.showLogo) height += 30f
        if (config.showTagline) height += 15f
        return height
    }
    
    /**
     * Get footer height based on configuration.
     */
    fun getFooterHeight(config: FooterConfig, qrConfig: QRCodeConfig): Float {
        var height = 20f // Base padding
        if (qrConfig.enabled) height = maxOf(height, qrConfig.size + 20f)
        return height
    }
    
    /**
     * Generate header text content.
     */
    fun generateHeaderContent(config: HeaderConfig): List<HeaderElement> {
        val elements = mutableListOf<HeaderElement>()
        
        if (config.showLogo) {
            elements.add(HeaderElement.Logo("VERUM OMNIS", config.logoStyle))
        }
        
        if (config.showTagline) {
            elements.add(HeaderElement.Tagline(config.tagline))
        }
        
        return elements
    }
    
    /**
     * Generate footer text content.
     */
    fun generateFooterContent(
        config: FooterConfig,
        metadata: PageMetadata
    ): List<FooterElement> {
        val elements = mutableListOf<FooterElement>()
        
        if (config.showPatentNotice) {
            elements.add(FooterElement.Text(
                text = config.patentNotice,
                position = FooterElement.Position.LEFT
            ))
        }
        
        if (config.showPageNumber) {
            elements.add(FooterElement.Text(
                text = "Page ${metadata.pageNumber} of ${metadata.totalPages}",
                position = FooterElement.Position.CENTER
            ))
        }
        
        if (config.showTimestamp) {
            val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
                .format(metadata.generationTimestamp)
            elements.add(FooterElement.Text(
                text = timestamp,
                position = FooterElement.Position.RIGHT
            ))
        }
        
        return elements
    }
    
    /**
     * Generate a decorative QR-like pattern from the document hash.
     * 
     * IMPORTANT: This creates a VISUAL pattern for branding/display purposes only.
     * It is NOT a scannable QR code. The pattern provides a visual representation
     * of the document's SHA-512 hash as a unique fingerprint, but cannot be scanned
     * by QR code readers. The full hash value is displayed as text below the pattern
     * for verification purposes.
     * 
     * @param hash The SHA-512 hash to visualize
     * @param gridSize Size of the pattern grid (default 8x8)
     * @return 2D boolean array where true = filled cell
     */
    fun generateQRPattern(hash: String, gridSize: Int = 8): Array<BooleanArray> {
        val pattern = Array(gridSize) { BooleanArray(gridSize) }
        
        // Generate pattern based on hash characters
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val charIndex = (i * gridSize + j) % hash.length
                pattern[i][j] = hash[charIndex].code % 2 == 0
            }
        }
        
        // Add corner markers (QR-style visual markers, not functional)
        // Top-left
        pattern[0][0] = true
        pattern[0][1] = true
        pattern[1][0] = true
        pattern[1][1] = true
        
        // Top-right
        pattern[0][gridSize-1] = true
        pattern[0][gridSize-2] = true
        pattern[1][gridSize-1] = true
        pattern[1][gridSize-2] = true
        
        // Bottom-left
        pattern[gridSize-1][0] = true
        pattern[gridSize-1][1] = true
        pattern[gridSize-2][0] = true
        pattern[gridSize-2][1] = true
        
        return pattern
    }
    
    /**
     * Header element types
     */
    sealed class HeaderElement {
        data class Logo(val text: String, val style: LogoStyle) : HeaderElement()
        data class Tagline(val text: String) : HeaderElement()
    }
    
    /**
     * Footer element types
     */
    sealed class FooterElement {
        enum class Position { LEFT, CENTER, RIGHT }
        data class Text(val text: String, val position: Position) : FooterElement()
        data class QRCode(val hash: String, val size: Float) : FooterElement()
    }
    
    /**
     * Format hash for display under QR code.
     */
    fun formatHashForDisplay(hash: String, prefixLength: Int = 32): String {
        return if (hash.length > prefixLength) {
            "SHA-512: ${hash.take(prefixLength)}"
        } else {
            "SHA-512: $hash"
        }
    }
}

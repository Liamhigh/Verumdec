package com.verumdec.report

/**
 * PDF Builder for Verum Omnis Reports
 * 
 * Provides utilities for building PDF documents with proper formatting,
 * branding, and cryptographic sealing.
 */
object PDFBuilder {
    
    /**
     * PDF Document settings
     */
    data class DocumentSettings(
        val pageWidth: Int = ReportModule.Format.PAGE_WIDTH,
        val pageHeight: Int = ReportModule.Format.PAGE_HEIGHT,
        val margin: Float = ReportModule.Format.MARGIN,
        val lineHeight: Float = ReportModule.Format.LINE_HEIGHT,
        val includeWatermark: Boolean = true,
        val includeQRCode: Boolean = true,
        val includeLogo: Boolean = true
    )
    
    /**
     * Text style for PDF rendering
     */
    data class TextStyle(
        val fontSize: Float,
        val color: Int,
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val alignment: Alignment = Alignment.LEFT
    ) {
        enum class Alignment { LEFT, CENTER, RIGHT }
    }
    
    /**
     * Predefined text styles
     */
    object Styles {
        val TITLE = TextStyle(fontSize = 24f, color = 0xFF1A237E.toInt(), isBold = true)
        val HEADING = TextStyle(fontSize = 16f, color = 0xFF1A237E.toInt(), isBold = true)
        val SUBHEADING = TextStyle(fontSize = 14f, color = 0xFF3949AB.toInt(), isBold = true)
        val BODY = TextStyle(fontSize = 10f, color = 0xFF000000.toInt())
        val LABEL = TextStyle(fontSize = 9f, color = 0xFF808080.toInt())
        val ALERT = TextStyle(fontSize = 10f, color = 0xFFD32F2F.toInt(), isBold = true)
    }
    
    /**
     * Severity color mapping
     */
    object SeverityColors {
        const val CRITICAL = 0xFFD32F2F.toInt() // Red
        const val HIGH = 0xFFF57C00.toInt() // Orange
        const val MEDIUM = 0xFFFBC02D.toInt() // Yellow
        const val LOW = 0xFF388E3C.toInt() // Green
    }
    
    /**
     * Brand colors
     */
    object BrandColors {
        const val PRIMARY = 0xFF1A237E.toInt() // Deep indigo
        const val ACCENT = 0xFF3949AB.toInt() // Lighter indigo
        const val GOLD = 0xFFFFD700.toInt() // Gold accent
        const val WATERMARK = 0x141A237E // Very faint indigo (alpha 20)
    }
    
    /**
     * Calculate required page count for content.
     */
    fun estimatePageCount(
        contentLines: Int,
        settings: DocumentSettings = DocumentSettings()
    ): Int {
        val usableHeight = settings.pageHeight - (2 * settings.margin) - 100 // Header/footer space
        val linesPerPage = (usableHeight / settings.lineHeight).toInt()
        return maxOf(1, (contentLines + linesPerPage - 1) / linesPerPage)
    }
    
    /**
     * Word wrap text to fit within a maximum width.
     */
    fun wrapText(text: String, maxWidth: Float, charWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            val testWidth = testLine.length * charWidth
            
            if (testWidth <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        
        return lines
    }
    
    /**
     * Format a section title with consistent styling.
     */
    fun formatSectionTitle(sectionNumber: Int, title: String): String {
        return "$sectionNumber. ${title.uppercase()}"
    }
    
    /**
     * Format a subsection title.
     */
    fun formatSubsectionTitle(title: String): String {
        return "▸ $title"
    }
    
    /**
     * Format a bullet point.
     */
    fun formatBulletPoint(text: String, indent: Int = 0): String {
        val prefix = " ".repeat(indent * 3)
        return "$prefix• $text"
    }
    
    /**
     * Format a numbered list item.
     */
    fun formatNumberedItem(number: Int, text: String, indent: Int = 0): String {
        val prefix = " ".repeat(indent * 3)
        return "$prefix$number. $text"
    }
}

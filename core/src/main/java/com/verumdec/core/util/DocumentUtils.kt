package com.verumdec.core.util

/**
 * Utility class for document analysis operations.
 */
object DocumentUtils {

    /**
     * Supported document extensions.
     */
    val SUPPORTED_EXTENSIONS = listOf("pdf", "doc", "docx", "txt", "rtf", "odt")

    /**
     * Check if file is a supported document format.
     */
    fun isDocumentFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in SUPPORTED_EXTENSIONS
    }

    /**
     * Document structure analysis result.
     */
    data class DocumentStructure(
        val pageCount: Int = 0,
        val hasImages: Boolean = false,
        val hasSignatures: Boolean = false,
        val hasTables: Boolean = false,
        val hasHeaders: Boolean = false,
        val hasFooters: Boolean = false,
        val isScanned: Boolean = false
    )

    /**
     * Count words in text.
     */
    fun countWords(text: String): Int {
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }

    /**
     * Count sentences in text.
     */
    fun countSentences(text: String): Int {
        return text.split(Regex("[.!?]+")).filter { it.isNotBlank() }.size
    }

    /**
     * Count paragraphs in text.
     */
    fun countParagraphs(text: String): Int {
        return text.split(Regex("\n\\s*\n")).filter { it.isNotBlank() }.size
    }

    /**
     * Calculate reading time in minutes.
     */
    fun calculateReadingTime(text: String, wordsPerMinute: Int = 200): Int {
        val wordCount = countWords(text)
        return (wordCount / wordsPerMinute) + 1
    }

    /**
     * Extract sections from document.
     */
    fun extractSections(text: String): List<DocumentSection> {
        val sections = mutableListOf<DocumentSection>()
        val lines = text.lines()
        
        var currentSection = ""
        var currentContent = StringBuilder()
        
        for (line in lines) {
            // Detect section headers (lines that are all caps or numbered)
            if (isLikelySectionHeader(line)) {
                if (currentSection.isNotEmpty()) {
                    sections.add(DocumentSection(currentSection, currentContent.toString().trim()))
                }
                currentSection = line.trim()
                currentContent = StringBuilder()
            } else {
                currentContent.appendLine(line)
            }
        }
        
        // Add last section
        if (currentSection.isNotEmpty()) {
            sections.add(DocumentSection(currentSection, currentContent.toString().trim()))
        }
        
        return sections
    }

    /**
     * Check if line is likely a section header.
     */
    private fun isLikelySectionHeader(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.length > 100) return false
        
        // All caps
        if (trimmed == trimmed.uppercase() && trimmed.any { it.isLetter() } && trimmed.length > 3) {
            return true
        }
        
        // Numbered sections
        if (Regex("^\\d+\\.\\s+[A-Z]").containsMatchIn(trimmed)) {
            return true
        }
        
        // Roman numerals
        if (Regex("^[IVXLCDM]+\\.\\s+").containsMatchIn(trimmed)) {
            return true
        }
        
        return false
    }

    /**
     * Document section container.
     */
    data class DocumentSection(
        val title: String,
        val content: String
    )

    /**
     * Detect document language (simplified).
     */
    fun detectLanguage(text: String): String {
        val sample = text.take(1000).lowercase()
        
        return when {
            sample.contains(" the ") && sample.contains(" and ") && sample.contains(" is ") -> "en"
            sample.contains(" le ") && sample.contains(" et ") && sample.contains(" est ") -> "fr"
            sample.contains(" der ") && sample.contains(" und ") && sample.contains(" ist ") -> "de"
            sample.contains(" el ") && sample.contains(" y ") && sample.contains(" es ") -> "es"
            sample.contains(" die ") && sample.contains(" en ") && sample.contains(" het ") -> "nl"
            else -> "unknown"
        }
    }

    /**
     * Check if text appears to be a legal document.
     */
    fun isLikelyLegalDocument(text: String): Boolean {
        val legalTerms = listOf(
            "hereby", "whereas", "pursuant", "hereinafter", "thereof",
            "jurisdiction", "liability", "indemnify", "warrant", "covenant",
            "terminate", "agreement", "contract", "party", "parties"
        )
        val sample = text.lowercase()
        val matchCount = legalTerms.count { sample.contains(it) }
        return matchCount >= 3
    }

    /**
     * Check if text appears to be financial document.
     */
    fun isLikelyFinancialDocument(text: String): Boolean {
        val financialTerms = listOf(
            "invoice", "payment", "total", "amount", "balance", "due",
            "credit", "debit", "transaction", "account", "bank", "transfer"
        )
        val sample = text.lowercase()
        val matchCount = financialTerms.count { sample.contains(it) }
        return matchCount >= 3
    }
}

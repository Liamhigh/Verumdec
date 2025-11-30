package com.verumdec.core.extraction

/**
 * TextNormalizer handles text preprocessing and normalization for the contradiction engine.
 * 
 * Performs:
 * - Whitespace normalization
 * - Unicode normalization
 * - Case handling
 * - Special character handling
 * - OCR error correction
 */
object TextNormalizer {

    // Common OCR error mappings: standard character -> list of erroneous variations
    // When correcting, we replace the errors (values) with the standard (key)
    private val ocrCorrections = mapOf(
        "l" to listOf("1", "|", "I"),  // Note: Context-dependent, use carefully
        "0" to listOf("O", "o"),  // Note: Context-dependent
        "1" to listOf("l", "I", "|"),  // Note: Context-dependent
        "rn" to listOf("m"),  // rn misread as m
        "cl" to listOf("d"),  // cl misread as d
        "li" to listOf("h"),  // li misread as h
        "vv" to listOf("w"),  // vv misread as w
        "'" to listOf("`", "´", "'", "'"),  // Various apostrophe variations -> standard
        "\"" to listOf(""", """, "„", "«", "»"),  // Various quote variations -> standard
        "-" to listOf("–", "—", "−", "‐", "‑", "‒", "―")  // Various dash variations -> standard
    )

    // Common abbreviations and expansions
    private val abbreviations = mapOf(
        "mr." to "mister",
        "mrs." to "missus",
        "ms." to "miss",
        "dr." to "doctor",
        "jr." to "junior",
        "sr." to "senior",
        "inc." to "incorporated",
        "ltd." to "limited",
        "corp." to "corporation",
        "co." to "company",
        "etc." to "etcetera",
        "vs." to "versus",
        "e.g." to "for example",
        "i.e." to "that is",
        "approx." to "approximately",
        "govt." to "government",
        "dept." to "department"
    )

    /**
     * Normalize text for consistent processing.
     *
     * @param text Raw text to normalize
     * @param preserveCase Whether to preserve original case (default: false)
     * @return Normalized text
     */
    fun normalize(text: String, preserveCase: Boolean = false): String {
        var normalized = text
        
        // Normalize unicode characters
        normalized = normalizeUnicode(normalized)
        
        // Normalize whitespace
        normalized = normalizeWhitespace(normalized)
        
        // Normalize quotes and punctuation
        normalized = normalizePunctuation(normalized)
        
        // Apply case normalization if requested
        if (!preserveCase) {
            normalized = normalized.lowercase()
        }
        
        return normalized.trim()
    }

    /**
     * Normalize text for semantic comparison.
     * More aggressive normalization for finding similar content.
     *
     * @param text Text to normalize
     * @return Normalized text suitable for semantic comparison
     */
    fun normalizeForComparison(text: String): String {
        var normalized = normalize(text, preserveCase = false)
        
        // Remove common stop words
        normalized = removeStopWords(normalized)
        
        // Expand abbreviations
        normalized = expandAbbreviations(normalized)
        
        return normalized.trim()
    }

    /**
     * Correct common OCR errors in text.
     * Replaces common erroneous character sequences with their correct forms.
     *
     * @param text Text that may contain OCR errors
     * @return Text with common OCR errors corrected
     */
    fun correctOcrErrors(text: String): String {
        var corrected = text
        
        // For each (correct, errors) pair, replace each error with the correct form
        for ((standardForm, errorVariations) in ocrCorrections) {
            for (errorVariation in errorVariations) {
                corrected = corrected.replace(errorVariation, standardForm)
            }
        }
        
        return corrected
    }

    /**
     * Normalize whitespace in text.
     * - Replaces multiple spaces with single space
     * - Normalizes line breaks
     * - Removes leading/trailing whitespace
     *
     * @param text Text to normalize
     * @return Text with normalized whitespace
     */
    fun normalizeWhitespace(text: String): String {
        return text
            .replace(Regex("[\u00A0\u2000-\u200B\u202F\u205F\u3000]"), " ") // Various space characters
            .replace(Regex("\\r\\n|\\r"), "\n") // Normalize line breaks
            .replace(Regex("[ \\t]+"), " ") // Multiple spaces/tabs to single space
            .replace(Regex("\\n+"), "\n") // Multiple newlines to single
            .trim()
    }

    /**
     * Normalize unicode characters.
     * Converts various unicode variants to ASCII equivalents.
     *
     * @param text Text to normalize
     * @return Text with normalized unicode characters
     */
    fun normalizeUnicode(text: String): String {
        return text
            // Smart quotes to straight quotes
            .replace("'", "'")
            .replace("'", "'")
            .replace(""", "\"")
            .replace(""", "\"")
            .replace("„", "\"")
            // Dashes
            .replace("–", "-")
            .replace("—", "-")
            .replace("−", "-")
            // Ellipsis
            .replace("…", "...")
            // Other common conversions
            .replace("•", "*")
            .replace("→", "->")
            .replace("←", "<-")
            .replace("©", "(c)")
            .replace("®", "(R)")
            .replace("™", "(TM)")
            .replace("°", " degrees")
            // Currency symbols (keep as is but normalize spacing)
            .replace(Regex("\\$\\s+"), "$")
            .replace(Regex("£\\s+"), "£")
            .replace(Regex("€\\s+"), "€")
    }

    /**
     * Normalize punctuation.
     * Standardizes various punctuation marks.
     *
     * @param text Text to normalize
     * @return Text with normalized punctuation
     */
    fun normalizePunctuation(text: String): String {
        return text
            // Normalize quotes
            .replace(Regex("[`´''‚]"), "'")
            .replace(Regex("[""„‟«»]"), "\"")
            // Normalize dashes
            .replace(Regex("[‐‑‒–—―]"), "-")
            // Remove excessive punctuation
            .replace(Regex("\\.{4,}"), "...")
            .replace(Regex("!{2,}"), "!")
            .replace(Regex("\\?{2,}"), "?")
            // Normalize spacing around punctuation
            .replace(Regex("\\s+([.,!?;:])"), "$1")
            .replace(Regex("([.,!?;:])(?=[^\\s])"), "$1 ")
    }

    /**
     * Remove common stop words from text.
     *
     * @param text Text to process
     * @return Text with stop words removed
     */
    fun removeStopWords(text: String): String {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", 
            "should", "may", "might", "must", "shall", "can", "to", "of", "in", 
            "for", "on", "with", "at", "by", "from", "as", "into", "through", 
            "during", "before", "after", "above", "below", "between", "under",
            "again", "further", "then", "once", "here", "there", "when", "where",
            "why", "how", "all", "each", "few", "more", "most", "other", "some",
            "such", "no", "nor", "not", "only", "own", "same", "so", "than",
            "too", "very", "just", "and", "but", "if", "or", "because", "until",
            "while", "this", "that", "these", "those", "it", "its"
        )
        
        return text.split(Regex("\\s+"))
            .filter { it.lowercase() !in stopWords }
            .joinToString(" ")
    }

    /**
     * Expand abbreviations in text.
     *
     * @param text Text containing abbreviations
     * @return Text with abbreviations expanded
     */
    fun expandAbbreviations(text: String): String {
        var expanded = text.lowercase()
        
        for ((abbr, full) in abbreviations) {
            expanded = expanded.replace(Regex("\\b${Regex.escape(abbr)}\\b", RegexOption.IGNORE_CASE), full)
        }
        
        return expanded
    }

    /**
     * Extract sentences from text.
     *
     * @param text Text to split into sentences
     * @return List of sentences
     */
    fun extractSentences(text: String): List<String> {
        return text
            .split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.length > 10 } // Filter out very short fragments
    }

    /**
     * Split text into paragraphs.
     *
     * @param text Text to split into paragraphs
     * @return List of paragraphs
     */
    fun extractParagraphs(text: String): List<String> {
        return text
            .split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Calculate text similarity using character-level comparison.
     * Useful for detecting typos or OCR variations.
     *
     * @param text1 First text
     * @param text2 Second text
     * @return Similarity score between 0.0 and 1.0
     */
    fun characterSimilarity(text1: String, text2: String): Double {
        if (text1.isEmpty() && text2.isEmpty()) return 1.0
        if (text1.isEmpty() || text2.isEmpty()) return 0.0
        
        val chars1 = text1.lowercase().toSet()
        val chars2 = text2.lowercase().toSet()
        
        val intersection = chars1.intersect(chars2).size
        val union = chars1.union(chars2).size
        
        return intersection.toDouble() / union.toDouble()
    }
}

package com.verumdec.core.extraction

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * DateNormalizer handles the normalization of date strings to a standard format.
 * 
 * All dates are normalized to YYYY-MM-DD format for consistent timeline ordering.
 * The normalizer handles various input formats including:
 * - DD/MM/YYYY, MM/DD/YYYY, YYYY-MM-DD
 * - "January 15, 2024", "15 Jan 2024"
 * - Relative dates ("yesterday", "last week")
 * - Natural language dates ("on Monday", "next Tuesday")
 */
object DateNormalizer {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val isoDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    
    // Date formats to try for parsing
    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("dd-MM-yyyy", Locale.US),
        SimpleDateFormat("MM-dd-yyyy", Locale.US),
        SimpleDateFormat("MMMM d, yyyy", Locale.US),
        SimpleDateFormat("MMMM dd, yyyy", Locale.US),
        SimpleDateFormat("d MMMM yyyy", Locale.US),
        SimpleDateFormat("dd MMMM yyyy", Locale.US),
        SimpleDateFormat("MMM d, yyyy", Locale.US),
        SimpleDateFormat("d MMM yyyy", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US),
        SimpleDateFormat("yyyyMMdd", Locale.US),
        SimpleDateFormat("MMMM yyyy", Locale.US),
        SimpleDateFormat("MMM yyyy", Locale.US)
    ).onEach { it.isLenient = false }
    
    // Patterns for extracting dates from text
    private val datePatterns = listOf(
        // ISO format: 2024-01-15
        Pattern.compile("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b"),
        // US format: 01/15/2024 or 1/15/2024
        Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})/(\\d{4})\\b"),
        // UK format: 15/01/2024
        Pattern.compile("\\b(\\d{1,2})-(\\d{1,2})-(\\d{4})\\b"),
        // Long format: January 15, 2024
        Pattern.compile("\\b(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(\\d{1,2}),?\\s+(\\d{4})\\b", Pattern.CASE_INSENSITIVE),
        // Short format: Jan 15, 2024
        Pattern.compile("\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\.?\\s+(\\d{1,2}),?\\s+(\\d{4})\\b", Pattern.CASE_INSENSITIVE),
        // Reverse format: 15 January 2024
        Pattern.compile("\\b(\\d{1,2})\\s+(January|February|March|April|May|June|July|August|September|October|November|December),?\\s+(\\d{4})\\b", Pattern.CASE_INSENSITIVE),
        // Short reverse: 15 Jan 2024
        Pattern.compile("\\b(\\d{1,2})\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\.?,?\\s+(\\d{4})\\b", Pattern.CASE_INSENSITIVE)
    )

    /**
     * Normalize a date string to YYYY-MM-DD format.
     *
     * @param rawDate The raw date string to normalize
     * @return Normalized date string in YYYY-MM-DD format, or null if parsing fails
     */
    fun normalize(rawDate: String?): String? {
        if (rawDate.isNullOrBlank()) return null
        
        val trimmed = rawDate.trim()
        
        // Try each date format
        for (format in dateFormats) {
            try {
                val date = format.parse(trimmed)
                if (date != null) {
                    return isoFormat.format(date)
                }
            } catch (e: Exception) {
                // Try next format
            }
        }
        
        // Try extracting date from text using patterns
        return extractDateFromText(trimmed)
    }

    /**
     * Normalize a date string and also return the epoch millis.
     *
     * @param rawDate The raw date string to normalize
     * @return Pair of (normalized string, epoch millis) or null if parsing fails
     */
    fun normalizeWithMillis(rawDate: String?): Pair<String, Long>? {
        if (rawDate.isNullOrBlank()) return null
        
        val trimmed = rawDate.trim()
        
        // Try each date format
        for (format in dateFormats) {
            try {
                val date = format.parse(trimmed)
                if (date != null) {
                    return Pair(isoFormat.format(date), date.time)
                }
            } catch (e: Exception) {
                // Try next format
            }
        }
        
        // Try extracting date from text using patterns
        val extracted = extractDateFromText(trimmed)
        if (extracted != null) {
            try {
                val date = isoFormat.parse(extracted)
                if (date != null) {
                    return Pair(extracted, date.time)
                }
            } catch (e: Exception) {
                // Fall through
            }
        }
        
        return null
    }

    /**
     * Convert epoch millis to normalized date string.
     *
     * @param millis Epoch milliseconds
     * @return Normalized date string in YYYY-MM-DD format
     */
    fun fromMillis(millis: Long): String {
        return isoFormat.format(Date(millis))
    }

    /**
     * Convert normalized date string to epoch millis.
     *
     * @param normalizedDate Date string in YYYY-MM-DD format
     * @return Epoch milliseconds, or null if parsing fails
     */
    fun toMillis(normalizedDate: String?): Long? {
        if (normalizedDate.isNullOrBlank()) return null
        
        return try {
            isoFormat.parse(normalizedDate)?.time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract and normalize a date from freeform text.
     *
     * @param text Text that may contain a date
     * @return Normalized date string in YYYY-MM-DD format, or null if no date found
     */
    fun extractDateFromText(text: String): String? {
        for (pattern in datePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val fullMatch = matcher.group(0)
                return normalize(fullMatch)
            }
        }
        return null
    }

    /**
     * Extract all dates from a text.
     *
     * @param text Text that may contain multiple dates
     * @return List of (raw date, normalized date) pairs found in the text
     */
    fun extractAllDatesFromText(text: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        
        for (pattern in datePatterns) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val raw = matcher.group(0)
                val normalized = normalize(raw)
                if (normalized != null) {
                    results.add(Pair(raw, normalized))
                }
            }
        }
        
        return results.distinctBy { it.second }
    }

    /**
     * Compare two normalized date strings.
     *
     * @param date1 First date string in YYYY-MM-DD format
     * @param date2 Second date string in YYYY-MM-DD format
     * @return negative if date1 < date2, 0 if equal, positive if date1 > date2
     */
    fun compare(date1: String?, date2: String?): Int {
        if (date1 == null && date2 == null) return 0
        if (date1 == null) return -1
        if (date2 == null) return 1
        return date1.compareTo(date2)
    }

    /**
     * Calculate the difference in days between two dates.
     *
     * @param date1 First date string in YYYY-MM-DD format
     * @param date2 Second date string in YYYY-MM-DD format
     * @return Number of days difference, or null if parsing fails
     */
    fun daysDifference(date1: String?, date2: String?): Long? {
        if (date1 == null || date2 == null) return null
        
        val millis1 = toMillis(date1) ?: return null
        val millis2 = toMillis(date2) ?: return null
        
        val diffMillis = kotlin.math.abs(millis2 - millis1)
        return diffMillis / (24 * 60 * 60 * 1000)
    }

    /**
     * Check if a date string is in valid normalized format.
     *
     * @param date Date string to check
     * @return true if the date is in YYYY-MM-DD format
     */
    fun isNormalized(date: String?): Boolean {
        if (date == null) return false
        return date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) && toMillis(date) != null
    }
}

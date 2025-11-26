package com.verumomnis.contradiction.nlp

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * NLP Utilities for Entity and Date Extraction
 *
 * Provides offline natural language processing capabilities for:
 * - Entity name extraction
 * - Date/time normalization
 * - Claim extraction from text
 */
object NlpUtils {

    private val datePatterns = listOf(
        DateTimeFormatter.ofPattern("d MMMM yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        DateTimeFormatter.ofPattern("MMMM d, yyyy")
    )

    private val relativeTimePatterns = mapOf(
        "today" to 0,
        "yesterday" to -1,
        "tomorrow" to 1,
        "last week" to -7,
        "next week" to 7,
        "last month" to -30,
        "next month" to 30
    )

    private val claimIndicators = listOf(
        "i said", "he said", "she said", "they said",
        "i told", "he told", "she told",
        "i promised", "he promised", "she promised",
        "i agreed", "he agreed", "she agreed",
        "i confirmed", "he confirmed", "she confirmed",
        "the deal", "no deal", "never",
        "i received", "i sent", "i paid",
        "he received", "he sent", "he paid",
        "she received", "she sent", "she paid"
    )

    private val denialPatterns = listOf(
        "i never", "he never", "she never", "they never",
        "i didn't", "he didn't", "she didn't", "they didn't",
        "i don't", "he doesn't", "she doesn't",
        "no one", "nobody", "nothing",
        "wasn't", "weren't", "isn't", "aren't",
        "not true", "false", "lie", "lies"
    )

    /**
     * Extracts potential entity names from text.
     */
    fun extractEntityNames(text: String): Set<String> {
        val entities = mutableSetOf<String>()

        // Pattern for capitalized names (simple approach)
        val namePattern = Regex("\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)\\b")
        namePattern.findAll(text).forEach { match ->
            val name = match.groupValues[1]
            // Filter out common words that might be capitalized
            if (!isCommonWord(name)) {
                entities.add(name)
            }
        }

        // Extract email addresses as entity identifiers
        val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        emailPattern.findAll(text).forEach { match ->
            entities.add(match.value)
        }

        // Extract phone numbers
        val phonePattern = Regex("\\+?\\d{1,3}[-.\\s]?\\d{2,4}[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,4}")
        phonePattern.findAll(text).forEach { match ->
            entities.add(match.value.replace(Regex("[-.\\s]"), ""))
        }

        return entities
    }

    /**
     * Checks if a word is a common word that shouldn't be treated as an entity.
     */
    private fun isCommonWord(word: String): Boolean {
        val commonWords = setOf(
            "The", "This", "That", "These", "Those",
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December",
            "Dear", "Hello", "Hi", "Thanks", "Regards", "Best",
            "Subject", "From", "To", "Date", "Re", "Fwd"
        )
        return word in commonWords
    }

    /**
     * Normalizes relative time expressions to absolute dates.
     */
    fun normalizeDate(text: String, referenceDate: LocalDate = LocalDate.now()): LocalDateTime? {
        val lowercaseText = text.lowercase().trim()

        // Check for relative time patterns
        for ((pattern, daysOffset) in relativeTimePatterns) {
            if (lowercaseText.contains(pattern)) {
                return referenceDate.plusDays(daysOffset.toLong()).atStartOfDay()
            }
        }

        // Check for "last [day]" pattern
        val lastDayPattern = Regex("last (monday|tuesday|wednesday|thursday|friday|saturday|sunday)")
        lastDayPattern.find(lowercaseText)?.let { match ->
            val dayName = match.groupValues[1]
            return findLastDayOfWeek(dayName, referenceDate)?.atStartOfDay()
        }

        // Check for "next [day]" pattern
        val nextDayPattern = Regex("next (monday|tuesday|wednesday|thursday|friday|saturday|sunday)")
        nextDayPattern.find(lowercaseText)?.let { match ->
            val dayName = match.groupValues[1]
            return findNextDayOfWeek(dayName, referenceDate)?.atStartOfDay()
        }

        // Try to parse as absolute date
        for (formatter in datePatterns) {
            try {
                val date = LocalDate.parse(text.trim(), formatter)
                return date.atStartOfDay()
            } catch (_: DateTimeParseException) {
                // Try next pattern
            }
        }

        return null
    }

    /**
     * Finds the last occurrence of a day of the week.
     */
    private fun findLastDayOfWeek(dayName: String, reference: LocalDate): LocalDate? {
        val targetDay = when (dayName.lowercase()) {
            "monday" -> java.time.DayOfWeek.MONDAY
            "tuesday" -> java.time.DayOfWeek.TUESDAY
            "wednesday" -> java.time.DayOfWeek.WEDNESDAY
            "thursday" -> java.time.DayOfWeek.THURSDAY
            "friday" -> java.time.DayOfWeek.FRIDAY
            "saturday" -> java.time.DayOfWeek.SATURDAY
            "sunday" -> java.time.DayOfWeek.SUNDAY
            else -> return null
        }

        var current = reference.minusDays(1)
        while (current.dayOfWeek != targetDay) {
            current = current.minusDays(1)
        }
        return current
    }

    /**
     * Finds the next occurrence of a day of the week.
     */
    private fun findNextDayOfWeek(dayName: String, reference: LocalDate): LocalDate? {
        val targetDay = when (dayName.lowercase()) {
            "monday" -> java.time.DayOfWeek.MONDAY
            "tuesday" -> java.time.DayOfWeek.TUESDAY
            "wednesday" -> java.time.DayOfWeek.WEDNESDAY
            "thursday" -> java.time.DayOfWeek.THURSDAY
            "friday" -> java.time.DayOfWeek.FRIDAY
            "saturday" -> java.time.DayOfWeek.SATURDAY
            "sunday" -> java.time.DayOfWeek.SUNDAY
            else -> return null
        }

        var current = reference.plusDays(1)
        while (current.dayOfWeek != targetDay) {
            current = current.plusDays(1)
        }
        return current
    }

    /**
     * Extracts potential claims from text.
     */
    fun extractClaims(text: String): List<ExtractedClaim> {
        val claims = mutableListOf<ExtractedClaim>()
        val sentences = text.split(Regex("[.!?]+")).filter { it.isNotBlank() }

        for (sentence in sentences) {
            val trimmedSentence = sentence.trim()
            val isClaimLikely = claimIndicators.any { trimmedSentence.lowercase().contains(it) }
            val isDenial = denialPatterns.any { trimmedSentence.lowercase().contains(it) }

            if (isClaimLikely || isDenial) {
                claims.add(
                    ExtractedClaim(
                        text = trimmedSentence,
                        isDenial = isDenial,
                        confidence = if (isClaimLikely && isDenial) 0.9f else 0.7f
                    )
                )
            }
        }

        return claims
    }

    /**
     * Extracts document metadata timestamps.
     */
    fun extractTimestamps(text: String): List<LocalDateTime> {
        val timestamps = mutableListOf<LocalDateTime>()

        // Extract date patterns
        val dateRegex = Regex("\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}")
        dateRegex.findAll(text).forEach { match ->
            normalizeDate(match.value)?.let { timestamps.add(it) }
        }

        // Extract written dates
        val writtenDateRegex = Regex("\\d{1,2}\\s+(January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4}", RegexOption.IGNORE_CASE)
        writtenDateRegex.findAll(text).forEach { match ->
            normalizeDate(match.value)?.let { timestamps.add(it) }
        }

        return timestamps.sortedBy { it }
    }
}

/**
 * Represents an extracted claim from text.
 */
data class ExtractedClaim(
    val text: String,
    val isDenial: Boolean,
    val confidence: Float
)

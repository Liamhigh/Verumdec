package com.verumdec.timeline.parser

import java.text.SimpleDateFormat
import java.util.*

/**
 * DateNormalizer - Parses and normalizes date references.
 */
class DateNormalizer {

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("dd MMM yyyy", Locale.US),
        SimpleDateFormat("MMM dd, yyyy", Locale.US)
    )

    /**
     * Parse a date string.
     */
    fun parse(dateStr: String): Date? {
        val trimmed = dateStr.trim()
        
        // Try relative dates first
        val relativeDate = parseRelativeDate(trimmed)
        if (relativeDate != null) return relativeDate
        
        // Try standard formats
        for (format in dateFormats) {
            try {
                format.isLenient = false
                return format.parse(trimmed)
            } catch (_: Exception) {}
        }
        
        return null
    }

    /**
     * Parse relative date expressions.
     */
    fun parseRelativeDate(expression: String, referenceDate: Date = Date()): Date? {
        val lower = expression.lowercase().trim()
        val calendar = Calendar.getInstance()
        calendar.time = referenceDate

        return when {
            lower == "today" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.time
            }
            lower == "yesterday" -> {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.time
            }
            lower == "tomorrow" -> {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                calendar.time
            }
            lower.startsWith("last ") -> parseLastReference(lower, calendar)
            lower.startsWith("next ") -> parseNextReference(lower, calendar)
            lower.matches(Regex("\\d+ days? ago")) -> {
                val days = lower.replace(Regex("[^0-9]"), "").toIntOrNull() ?: return null
                calendar.add(Calendar.DAY_OF_MONTH, -days)
                calendar.time
            }
            lower.matches(Regex("in \\d+ days?")) -> {
                val days = lower.replace(Regex("[^0-9]"), "").toIntOrNull() ?: return null
                calendar.add(Calendar.DAY_OF_MONTH, days)
                calendar.time
            }
            lower.matches(Regex("\\d+ weeks? ago")) -> {
                val weeks = lower.replace(Regex("[^0-9]"), "").toIntOrNull() ?: return null
                calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
                calendar.time
            }
            lower.matches(Regex("\\d+ months? ago")) -> {
                val months = lower.replace(Regex("[^0-9]"), "").toIntOrNull() ?: return null
                calendar.add(Calendar.MONTH, -months)
                calendar.time
            }
            else -> null
        }
    }

    private fun parseLastReference(lower: String, calendar: Calendar): Date? {
        return when {
            lower.contains("monday") -> findLastDayOfWeek(calendar, Calendar.MONDAY)
            lower.contains("tuesday") -> findLastDayOfWeek(calendar, Calendar.TUESDAY)
            lower.contains("wednesday") -> findLastDayOfWeek(calendar, Calendar.WEDNESDAY)
            lower.contains("thursday") -> findLastDayOfWeek(calendar, Calendar.THURSDAY)
            lower.contains("friday") -> findLastDayOfWeek(calendar, Calendar.FRIDAY)
            lower.contains("saturday") -> findLastDayOfWeek(calendar, Calendar.SATURDAY)
            lower.contains("sunday") -> findLastDayOfWeek(calendar, Calendar.SUNDAY)
            lower.contains("week") -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.time
            }
            lower.contains("month") -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.time
            }
            lower.contains("year") -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.time
            }
            else -> null
        }
    }

    private fun parseNextReference(lower: String, calendar: Calendar): Date? {
        return when {
            lower.contains("monday") -> findNextDayOfWeek(calendar, Calendar.MONDAY)
            lower.contains("tuesday") -> findNextDayOfWeek(calendar, Calendar.TUESDAY)
            lower.contains("wednesday") -> findNextDayOfWeek(calendar, Calendar.WEDNESDAY)
            lower.contains("thursday") -> findNextDayOfWeek(calendar, Calendar.THURSDAY)
            lower.contains("friday") -> findNextDayOfWeek(calendar, Calendar.FRIDAY)
            lower.contains("saturday") -> findNextDayOfWeek(calendar, Calendar.SATURDAY)
            lower.contains("sunday") -> findNextDayOfWeek(calendar, Calendar.SUNDAY)
            lower.contains("week") -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.time
            }
            lower.contains("month") -> {
                calendar.add(Calendar.MONTH, 1)
                calendar.time
            }
            else -> null
        }
    }

    private fun findLastDayOfWeek(calendar: Calendar, dayOfWeek: Int): Date {
        do {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        } while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek)
        return calendar.time
    }

    private fun findNextDayOfWeek(calendar: Calendar, dayOfWeek: Int): Date {
        do {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        } while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek)
        return calendar.time
    }

    /**
     * Format date to ISO string.
     */
    fun formatIso(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(date)
    }

    /**
     * Format for display.
     */
    fun formatDisplay(date: Date): String {
        return SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.US).format(date)
    }
}

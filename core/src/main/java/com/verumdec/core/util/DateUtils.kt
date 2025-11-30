package com.verumdec.core.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Utility class for date parsing and manipulation.
 * Handles various date formats and natural language date references.
 */
object DateUtils {

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US),
        SimpleDateFormat("dd MMM yyyy", Locale.US),
        SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("MMM dd, yyyy", Locale.US),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        SimpleDateFormat("d/M/yy, h:mm a", Locale.US),
        SimpleDateFormat("d/M/yy, HH:mm", Locale.US)
    )

    /**
     * Parse a date string using multiple format attempts.
     */
    fun parseDate(dateStr: String): Date? {
        val trimmed = dateStr.trim()
        for (format in dateFormats) {
            try {
                format.isLenient = false
                return format.parse(trimmed)
            } catch (_: Exception) {
                // Try next format
            }
        }
        return null
    }

    /**
     * Format a date to ISO format.
     */
    fun formatIso(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(date)
    }

    /**
     * Format date for display.
     */
    fun formatDisplay(date: Date): String {
        return SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.US).format(date)
    }

    /**
     * Format date for reports.
     */
    fun formatReport(date: Date): String {
        return SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.US).format(date)
    }

    /**
     * Format date only (no time).
     */
    fun formatDateOnly(date: Date): String {
        return SimpleDateFormat("dd MMMM yyyy", Locale.US).format(date)
    }

    /**
     * Parse relative date expressions.
     */
    fun parseRelativeDate(expression: String, referenceDate: Date = Date()): Date? {
        val lower = expression.lowercase().trim()
        val calendar = Calendar.getInstance()
        calendar.time = referenceDate

        return when {
            lower == "today" -> calendar.time
            lower == "yesterday" -> {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.time
            }
            lower == "tomorrow" -> {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                calendar.time
            }
            lower.startsWith("last ") -> {
                when {
                    lower.contains("week") -> {
                        calendar.add(Calendar.WEEK_OF_YEAR, -1)
                        calendar.time
                    }
                    lower.contains("month") -> {
                        calendar.add(Calendar.MONTH, -1)
                        calendar.time
                    }
                    lower.contains("monday") -> findLastDayOfWeek(calendar, Calendar.MONDAY)
                    lower.contains("tuesday") -> findLastDayOfWeek(calendar, Calendar.TUESDAY)
                    lower.contains("wednesday") -> findLastDayOfWeek(calendar, Calendar.WEDNESDAY)
                    lower.contains("thursday") -> findLastDayOfWeek(calendar, Calendar.THURSDAY)
                    lower.contains("friday") -> findLastDayOfWeek(calendar, Calendar.FRIDAY)
                    lower.contains("saturday") -> findLastDayOfWeek(calendar, Calendar.SATURDAY)
                    lower.contains("sunday") -> findLastDayOfWeek(calendar, Calendar.SUNDAY)
                    else -> null
                }
            }
            lower.startsWith("next ") -> {
                when {
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
            else -> parseDate(expression)
        }
    }

    private fun findLastDayOfWeek(calendar: Calendar, dayOfWeek: Int): Date {
        while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        return calendar.time
    }

    /**
     * Calculate duration between two dates.
     */
    fun durationBetween(start: Date, end: Date): Long {
        return end.time - start.time
    }

    /**
     * Format duration to human-readable string.
     */
    fun formatDuration(millis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

        return when {
            days > 0 -> "$days day(s), $hours hour(s)"
            hours > 0 -> "$hours hour(s), $minutes minute(s)"
            else -> "$minutes minute(s)"
        }
    }

    /**
     * Check if date is within a range.
     */
    fun isWithinRange(date: Date, start: Date, end: Date): Boolean {
        return date.after(start) && date.before(end)
    }

    /**
     * Get current timestamp as formatted string.
     */
    fun currentTimestamp(): String {
        return formatIso(Date())
    }
}

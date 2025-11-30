package com.verumdec.core

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions for metadata extraction and parsing.
 * Handles dates, times, and document metadata.
 */
object MetadataUtils {
    
    /**
     * Common date format patterns to try when parsing dates.
     */
    private val DATE_PATTERNS = listOf(
        // ISO formats
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
        
        // Email formats
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss",
        "dd MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss",
        
        // Common US/UK formats
        "MM/dd/yyyy HH:mm:ss",
        "MM/dd/yyyy HH:mm",
        "MM/dd/yyyy",
        "dd/MM/yyyy HH:mm:ss",
        "dd/MM/yyyy HH:mm",
        "dd/MM/yyyy",
        
        // WhatsApp formats
        "d/M/yy, h:mm a",
        "d/M/yy, HH:mm",
        "dd/MM/yyyy, HH:mm:ss",
        "dd/MM/yyyy, h:mm:ss a",
        "M/d/yy, h:mm a",
        
        // Full text formats
        "MMMM d, yyyy",
        "d MMMM yyyy",
        "MMM d, yyyy",
        "d MMM yyyy"
    )
    
    /**
     * Try to parse a date string using multiple format patterns.
     */
    fun parseDate(dateString: String): Date? {
        val trimmed = dateString.trim()
        
        for (pattern in DATE_PATTERNS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.isLenient = false
                return sdf.parse(trimmed)
            } catch (e: Exception) {
                // Try next pattern
            }
        }
        
        // Try with UK locale for month names
        for (pattern in DATE_PATTERNS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.UK)
                sdf.isLenient = false
                return sdf.parse(trimmed)
            } catch (e: Exception) {
                // Try next pattern
            }
        }
        
        return null
    }
    
    /**
     * Format a date for display.
     */
    fun formatDate(date: Date, pattern: String = "dd MMMM yyyy HH:mm:ss"): String {
        return SimpleDateFormat(pattern, Locale.US).format(date)
    }
    
    /**
     * Format a date for use in filenames.
     */
    fun formatDateForFilename(date: Date): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(date)
    }
    
    /**
     * Get a human-readable relative time string.
     */
    fun getRelativeTimeString(date: Date): String {
        val now = System.currentTimeMillis()
        val diffMs = now - date.time
        val diffSeconds = diffMs / 1000
        val diffMinutes = diffSeconds / 60
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24
        
        return when {
            diffSeconds < 60 -> "just now"
            diffMinutes < 60 -> "$diffMinutes minute${if (diffMinutes != 1L) "s" else ""} ago"
            diffHours < 24 -> "$diffHours hour${if (diffHours != 1L) "s" else ""} ago"
            diffDays < 7 -> "$diffDays day${if (diffDays != 1L) "s" else ""} ago"
            diffDays < 30 -> "${diffDays / 7} week${if (diffDays / 7 != 1L) "s" else ""} ago"
            diffDays < 365 -> "${diffDays / 30} month${if (diffDays / 30 != 1L) "s" else ""} ago"
            else -> "${diffDays / 365} year${if (diffDays / 365 != 1L) "s" else ""} ago"
        }
    }
    
    /**
     * Extract email address from a string.
     */
    fun extractEmail(text: String): String? {
        val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        return emailPattern.find(text)?.value
    }
    
    /**
     * Extract all email addresses from a string.
     */
    fun extractAllEmails(text: String): List<String> {
        val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        return emailPattern.findAll(text).map { it.value.lowercase() }.distinct().toList()
    }
    
    /**
     * Extract phone numbers from a string.
     */
    fun extractPhoneNumbers(text: String): List<String> {
        val phonePattern = Regex("\\+?[0-9][0-9\\s\\-()]{8,}[0-9]")
        return phonePattern.findAll(text)
            .map { it.value.replace(Regex("[\\s\\-()]"), "") }
            .filter { it.length >= 10 }
            .distinct()
            .toList()
    }
    
    /**
     * Extract name from email format like "John Doe <john@example.com>".
     */
    fun extractNameFromEmailFormat(emailString: String): String? {
        val nameMatch = Regex("([^<]+)<").find(emailString)
        return nameMatch?.groupValues?.get(1)?.trim()
    }
    
    /**
     * Parse email headers from raw email text.
     */
    fun parseEmailHeaders(emailText: String): EmailHeaders {
        val lines = emailText.lines()
        var from: String? = null
        var to: String? = null
        var subject: String? = null
        var date: Date? = null
        
        for (line in lines) {
            when {
                line.startsWith("From:", ignoreCase = true) -> 
                    from = line.substringAfter(":").trim()
                line.startsWith("To:", ignoreCase = true) -> 
                    to = line.substringAfter(":").trim()
                line.startsWith("Subject:", ignoreCase = true) -> 
                    subject = line.substringAfter(":").trim()
                line.startsWith("Date:", ignoreCase = true) -> 
                    date = parseDate(line.substringAfter(":").trim())
            }
            
            // Email headers end at first blank line
            if (line.isBlank() && (from != null || to != null)) break
        }
        
        return EmailHeaders(from, to, subject, date)
    }
    
    /**
     * Represents parsed email headers.
     */
    data class EmailHeaders(
        val from: String?,
        val to: String?,
        val subject: String?,
        val date: Date?
    )
    
    /**
     * Parse WhatsApp message format: "[date, time] sender: message"
     */
    fun parseWhatsAppMessage(line: String): WhatsAppMessage? {
        val pattern = Regex("\\[([^\\]]+)\\]\\s*([^:]+):\\s*(.+)")
        val match = pattern.find(line) ?: return null
        
        val dateStr = match.groupValues[1]
        val sender = match.groupValues[2].trim()
        val message = match.groupValues[3].trim()
        val date = parseDate(dateStr)
        
        return WhatsAppMessage(date, sender, message)
    }
    
    /**
     * Represents a parsed WhatsApp message.
     */
    data class WhatsAppMessage(
        val date: Date?,
        val sender: String,
        val message: String
    )
    
    /**
     * Extract monetary amounts from text.
     */
    fun extractMonetaryAmounts(text: String): List<String> {
        val patterns = listOf(
            Regex("\\$[0-9,]+\\.?[0-9]*"),
            Regex("£[0-9,]+\\.?[0-9]*"),
            Regex("€[0-9,]+\\.?[0-9]*"),
            Regex("R[0-9,]+\\.?[0-9]*"),
            Regex("[0-9,]+\\.[0-9]{2}\\s*(USD|GBP|EUR|ZAR)")
        )
        
        return patterns.flatMap { pattern ->
            pattern.findAll(text).map { it.value }
        }.distinct()
    }
    
    /**
     * Extract bank account numbers (IBAN or numeric).
     */
    fun extractBankAccounts(text: String): List<String> {
        val patterns = listOf(
            Regex("\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{4,30}\\b"), // IBAN
            Regex("\\b[0-9]{8,20}\\b") // Numeric account
        )
        
        return patterns.flatMap { pattern ->
            pattern.findAll(text).map { it.value }
        }.distinct()
    }
    
    /**
     * Normalize a name for comparison.
     */
    fun normalizeName(name: String): String {
        return name.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
    
    /**
     * Check if two names might refer to the same person.
     */
    fun namesMatch(name1: String, name2: String): Boolean {
        val n1 = normalizeName(name1)
        val n2 = normalizeName(name2)
        
        // Exact match
        if (n1 == n2) return true
        
        // One contains the other
        if (n1.contains(n2) || n2.contains(n1)) return true
        
        // First names match
        val parts1 = n1.split(" ")
        val parts2 = n2.split(" ")
        if (parts1.isNotEmpty() && parts2.isNotEmpty() && 
            parts1.first() == parts2.first() && parts1.first().length > 2) {
            return true
        }
        
        return false
    }
}

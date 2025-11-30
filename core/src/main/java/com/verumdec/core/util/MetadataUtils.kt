package com.verumdec.core.util

import java.util.*

/**
 * Utility class for extracting and managing metadata.
 */
object MetadataUtils {

    /**
     * Extract email address from string.
     */
    fun extractEmail(text: String): String? {
        val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        return emailPattern.find(text)?.value
    }

    /**
     * Extract all email addresses from text.
     */
    fun extractAllEmails(text: String): List<String> {
        val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        return emailPattern.findAll(text).map { it.value.lowercase() }.distinct().toList()
    }

    /**
     * Extract phone numbers from text.
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
     * Extract dates from text.
     */
    fun extractDates(text: String): List<String> {
        val datePatterns = listOf(
            Regex("\\d{1,2}/\\d{1,2}/\\d{2,4}"),
            Regex("\\d{4}-\\d{2}-\\d{2}"),
            Regex("\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}", RegexOption.IGNORE_CASE)
        )
        
        val dates = mutableListOf<String>()
        for (pattern in datePatterns) {
            dates.addAll(pattern.findAll(text).map { it.value })
        }
        return dates.distinct()
    }

    /**
     * Extract monetary amounts from text.
     */
    fun extractMonetaryAmounts(text: String): List<String> {
        val patterns = listOf(
            Regex("[$€£¥₹R]\\s*[0-9,]+\\.?[0-9]*"),
            Regex("[0-9,]+\\.?[0-9]*\\s*(?:USD|EUR|GBP|ZAR|dollars?|euros?|pounds?|rand)", RegexOption.IGNORE_CASE)
        )
        
        val amounts = mutableListOf<String>()
        for (pattern in patterns) {
            amounts.addAll(pattern.findAll(text).map { it.value })
        }
        return amounts.distinct()
    }

    /**
     * Extract name from email sender format.
     */
    fun extractNameFromEmailSender(sender: String): String? {
        // Format: "Name <email@example.com>"
        val nameMatch = Regex("([^<]+)<").find(sender)
        if (nameMatch != null) {
            return nameMatch.groupValues[1].trim()
        }
        
        // Extract from email username
        val email = extractEmail(sender) ?: return null
        val username = email.substringBefore('@')
        return username
            .replace(".", " ")
            .replace("_", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    /**
     * Extract bank account or IBAN numbers.
     */
    fun extractBankAccounts(text: String): List<String> {
        val patterns = listOf(
            Regex("[A-Z]{2}[0-9]{2}[A-Z0-9]{4,30}"), // IBAN
            Regex("\\b[0-9]{8,20}\\b") // Generic account number
        )
        
        val accounts = mutableListOf<String>()
        for (pattern in patterns) {
            accounts.addAll(pattern.findAll(text).map { it.value })
        }
        return accounts.distinct()
    }

    /**
     * Extract URLs from text.
     */
    fun extractUrls(text: String): List<String> {
        val urlPattern = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")
        return urlPattern.findAll(text).map { it.value }.distinct().toList()
    }

    /**
     * Build a metadata map from text content.
     */
    fun buildMetadataFromContent(text: String): Map<String, Any> {
        return mapOf(
            "emails" to extractAllEmails(text),
            "phones" to extractPhoneNumbers(text),
            "dates" to extractDates(text),
            "amounts" to extractMonetaryAmounts(text),
            "urls" to extractUrls(text),
            "bankAccounts" to extractBankAccounts(text),
            "wordCount" to text.split(Regex("\\s+")).size,
            "characterCount" to text.length,
            "extractedAt" to Date()
        )
    }

    /**
     * Sanitize text for safe display.
     */
    fun sanitizeText(text: String): String {
        return text
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "") // Remove control chars
            .trim()
    }

    /**
     * Normalize whitespace in text.
     */
    fun normalizeWhitespace(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim()
    }
}

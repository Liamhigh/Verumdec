package com.verumdec.core.extraction

import java.util.regex.Pattern

/**
 * SpeakerExtractor identifies and extracts speakers (actors) from document text.
 * 
 * Handles various formats:
 * - Chat format: "John: Hello there"
 * - Email format: "From: john@example.com"
 * - Quoted speech: '"I never said that," John replied.'
 * - Document signatures
 * - WhatsApp/SMS exports
 */
object SpeakerExtractor {

    // Patterns for identifying speakers
    private val chatPattern = Pattern.compile("^([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?):(.+)", Pattern.MULTILINE)
    private val emailFromPattern = Pattern.compile("From:\\s*([^<\\n]+)", Pattern.CASE_INSENSITIVE)
    private val emailToPattern = Pattern.compile("To:\\s*([^<\\n]+)", Pattern.CASE_INSENSITIVE)
    private val quotedSpeechPattern = Pattern.compile("\"([^\"]+)\"[,.]?\\s*(?:said|replied|asked|stated|claimed|insisted|argued|explained|added|responded)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)", Pattern.CASE_INSENSITIVE)
    private val reversedQuotePattern = Pattern.compile("([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)[:]?\\s*(?:said|replied|asked|stated|claimed|insisted|argued|explained|added|responded)[,:]?\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE)
    private val signaturePattern = Pattern.compile("(?:Signed|Signature|Regards|Sincerely|Best)[,:]?\\s*([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)", Pattern.CASE_INSENSITIVE)
    private val whatsAppPattern = Pattern.compile("\\[(\\d{1,2}/\\d{1,2}/\\d{2,4}),?\\s*\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[AP]M)?\\]\\s*([^:]+):", Pattern.CASE_INSENSITIVE)
    private val smsPattern = Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+(\\d{1,2}:\\d{2})\\s+([^:]+):", Pattern.MULTILINE)

    /**
     * Result of speaker extraction.
     */
    data class SpeakerStatement(
        val speaker: String,
        val text: String,
        val startIndex: Int,
        val endIndex: Int,
        val extractionMethod: ExtractionMethod
    )

    /**
     * Method used to extract the speaker.
     */
    enum class ExtractionMethod {
        CHAT_FORMAT,
        EMAIL_FROM,
        EMAIL_TO,
        QUOTED_SPEECH,
        SIGNATURE,
        WHATSAPP,
        SMS,
        DOCUMENT_AUTHOR,
        UNKNOWN
    }

    /**
     * Extract all speaker statements from text.
     *
     * @param text Document text
     * @param documentAuthor Optional document author to use as default speaker
     * @return List of extracted speaker statements
     */
    fun extractSpeakers(text: String, documentAuthor: String? = null): List<SpeakerStatement> {
        val statements = mutableListOf<SpeakerStatement>()
        
        // Try each extraction method
        statements.addAll(extractFromChatFormat(text))
        statements.addAll(extractFromWhatsApp(text))
        statements.addAll(extractFromSMS(text))
        statements.addAll(extractFromEmails(text))
        statements.addAll(extractFromQuotedSpeech(text))
        statements.addAll(extractFromSignatures(text))
        
        // If no speakers found and we have a document author, use that
        if (statements.isEmpty() && !documentAuthor.isNullOrBlank()) {
            // Split into sentences and attribute to author
            val sentences = TextNormalizer.extractSentences(text)
            sentences.forEachIndexed { index, sentence ->
                if (sentence.isNotBlank()) {
                    statements.add(
                        SpeakerStatement(
                            speaker = documentAuthor.trim(),
                            text = sentence,
                            startIndex = text.indexOf(sentence),
                            endIndex = text.indexOf(sentence) + sentence.length,
                            extractionMethod = ExtractionMethod.DOCUMENT_AUTHOR
                        )
                    )
                }
            }
        }
        
        return statements.distinctBy { it.text }
    }

    /**
     * Extract all unique speakers from text.
     *
     * @param text Document text
     * @param documentAuthor Optional document author
     * @return Set of unique speaker names
     */
    fun extractUniqueSpeakers(text: String, documentAuthor: String? = null): Set<String> {
        val statements = extractSpeakers(text, documentAuthor)
        return statements.map { normalizeSpeakerName(it.speaker) }.toSet()
    }

    /**
     * Extract speakers from chat format (Name: message).
     */
    private fun extractFromChatFormat(text: String): List<SpeakerStatement> {
        val statements = mutableListOf<SpeakerStatement>()
        val matcher = chatPattern.matcher(text)
        
        while (matcher.find()) {
            val speaker = matcher.group(1)?.trim() ?: continue
            val message = matcher.group(2)?.trim() ?: continue
            
            // Skip if it looks like an email header or time
            if (speaker.lowercase() in listOf("from", "to", "cc", "bcc", "subject", "date", "time", "sent")) continue
            if (speaker.matches(Regex("\\d+"))) continue
            
            statements.add(
                SpeakerStatement(
                    speaker = speaker,
                    text = message,
                    startIndex = matcher.start(),
                    endIndex = matcher.end(),
                    extractionMethod = ExtractionMethod.CHAT_FORMAT
                )
            )
        }
        
        return statements
    }

    /**
     * Extract speakers from WhatsApp format.
     */
    private fun extractFromWhatsApp(text: String): List<SpeakerStatement> {
        val statements = mutableListOf<SpeakerStatement>()
        val matcher = whatsAppPattern.matcher(text)
        
        var lastEnd = 0
        var lastSpeaker: String? = null
        
        while (matcher.find()) {
            // If we have a previous speaker, the text between is their message
            if (lastSpeaker != null) {
                val messageText = text.substring(lastEnd, matcher.start()).trim()
                if (messageText.isNotBlank()) {
                    statements.add(
                        SpeakerStatement(
                            speaker = lastSpeaker,
                            text = messageText,
                            startIndex = lastEnd,
                            endIndex = matcher.start(),
                            extractionMethod = ExtractionMethod.WHATSAPP
                        )
                    )
                }
            }
            
            lastSpeaker = matcher.group(2)?.trim()
            lastEnd = matcher.end()
        }
        
        // Handle last message
        if (lastSpeaker != null && lastEnd < text.length) {
            val messageText = text.substring(lastEnd).trim()
            if (messageText.isNotBlank()) {
                statements.add(
                    SpeakerStatement(
                        speaker = lastSpeaker,
                        text = messageText,
                        startIndex = lastEnd,
                        endIndex = text.length,
                        extractionMethod = ExtractionMethod.WHATSAPP
                    )
                )
            }
        }
        
        return statements
    }

    /**
     * Extract speakers from SMS format.
     */
    private fun extractFromSMS(text: String): List<SpeakerStatement> {
        val statements = mutableListOf<SpeakerStatement>()
        val matcher = smsPattern.matcher(text)
        
        while (matcher.find()) {
            val speaker = matcher.group(3)?.trim() ?: continue
            
            statements.add(
                SpeakerStatement(
                    speaker = speaker,
                    text = "", // Will be populated by next iteration or end of text
                    startIndex = matcher.start(),
                    endIndex = matcher.end(),
                    extractionMethod = ExtractionMethod.SMS
                )
            )
        }
        
        return statements
    }

    /**
     * Extract speakers from email headers.
     */
    private fun extractFromEmails(text: String): List<SpeakerStatement> {
        val statements = mutableListOf<SpeakerStatement>()
        
        // Extract From
        val fromMatcher = emailFromPattern.matcher(text)
        if (fromMatcher.find()) {
            val sender = fromMatcher.group(1)?.trim()
            if (!sender.isNullOrBlank()) {
                // Get email body (after headers)
                val headerEnd = findEmailBodyStart(text)
                val body = if (headerEnd > 0) text.substring(headerEnd).trim() else ""
                
                statements.add(
                    SpeakerStatement(
                        speaker = cleanEmailName(sender),
                        text = body,
                        startIndex = headerEnd,
                        endIndex = text.length,
                        extractionMethod = ExtractionMethod.EMAIL_FROM
                    )
                )
            }
        }
        
        return statements
    }

    /**
     * Extract speakers from quoted speech.
     */
    private fun extractFromQuotedSpeech(text: String): List<SpeakerStatement> {
        val statements = mutableListOf<SpeakerStatement>()
        
        // Pattern: "quote" said John
        var matcher = quotedSpeechPattern.matcher(text)
        while (matcher.find()) {
            val quote = matcher.group(1)?.trim() ?: continue
            val speaker = matcher.group(2)?.trim() ?: continue
            
            statements.add(
                SpeakerStatement(
                    speaker = speaker,
                    text = quote,
                    startIndex = matcher.start(),
                    endIndex = matcher.end(),
                    extractionMethod = ExtractionMethod.QUOTED_SPEECH
                )
            )
        }
        
        // Pattern: John said "quote"
        matcher = reversedQuotePattern.matcher(text)
        while (matcher.find()) {
            val speaker = matcher.group(1)?.trim() ?: continue
            val quote = matcher.group(2)?.trim() ?: continue
            
            statements.add(
                SpeakerStatement(
                    speaker = speaker,
                    text = quote,
                    startIndex = matcher.start(),
                    endIndex = matcher.end(),
                    extractionMethod = ExtractionMethod.QUOTED_SPEECH
                )
            )
        }
        
        return statements
    }

    /**
     * Extract speakers from signatures.
     */
    private fun extractFromSignatures(text: String): List<SpeakerStatement> {
        val statements = mutableListOf<SpeakerStatement>()
        val matcher = signaturePattern.matcher(text)
        
        while (matcher.find()) {
            val speaker = matcher.group(1)?.trim() ?: continue
            
            // Skip common words that aren't names
            if (speaker.lowercase() in listOf("you", "me", "us", "them", "regards")) continue
            
            statements.add(
                SpeakerStatement(
                    speaker = speaker,
                    text = "", // No specific text, just identifies a speaker
                    startIndex = matcher.start(),
                    endIndex = matcher.end(),
                    extractionMethod = ExtractionMethod.SIGNATURE
                )
            )
        }
        
        return statements
    }

    /**
     * Normalize a speaker name for consistent identification.
     */
    fun normalizeSpeakerName(name: String): String {
        return name
            .trim()
            .replace(Regex("\\s+"), " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
    }

    /**
     * Clean an email name (remove email address, clean formatting).
     */
    private fun cleanEmailName(emailName: String): String {
        // Remove email address if present
        val nameOnly = emailName
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\[[^\\]]+\\]"), "")
            .trim()
        
        // Remove quotes
        return nameOnly
            .replace("\"", "")
            .replace("'", "")
            .trim()
    }

    /**
     * Find where email body starts (after headers).
     */
    private fun findEmailBodyStart(text: String): Int {
        // Look for blank line after headers
        val blankLineIndex = text.indexOf("\n\n")
        if (blankLineIndex > 0) return blankLineIndex + 2
        
        // Look for common end-of-header patterns
        val patterns = listOf("Subject:", "Date:", "Sent:")
        var lastHeaderEnd = 0
        
        for (pattern in patterns) {
            val index = text.indexOf(pattern, ignoreCase = true)
            if (index > 0) {
                val lineEnd = text.indexOf("\n", index)
                if (lineEnd > lastHeaderEnd) {
                    lastHeaderEnd = lineEnd + 1
                }
            }
        }
        
        return lastHeaderEnd
    }

    /**
     * Determine if a text segment is likely a first-person statement.
     */
    fun isFirstPersonStatement(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower.startsWith("i ") ||
               lower.startsWith("i'm ") ||
               lower.startsWith("i've ") ||
               lower.startsWith("i'd ") ||
               lower.contains(" i ") ||
               lower.contains(" my ") ||
               lower.contains(" me ") ||
               lower.contains(" mine ")
    }
}

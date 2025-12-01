package com.verumdec.core.extraction

import com.verumdec.core.model.LegalCategory
import com.verumdec.core.model.Statement
import java.util.UUID

/**
 * StatementExtractor extracts structured Statement objects from raw document text.
 * 
 * Handles:
 * - Sentence tokenization
 * - Speaker attribution
 * - Timestamp extraction
 * - Legal category classification
 * - Sentiment analysis
 * - Certainty detection
 */
object StatementExtractor {

    // Keywords for legal category classification
    private val promiseKeywords = listOf("promise", "will", "shall", "commit", "guarantee", "assure", "pledge")
    private val denialKeywords = listOf("deny", "never", "didn't", "did not", "wasn't", "refuse", "reject", "no")
    private val admissionKeywords = listOf("admit", "acknowledge", "confess", "yes", "did", "agree")
    private val assertionKeywords = listOf("claim", "assert", "state", "believe", "think", "know")
    private val threatKeywords = listOf("warn", "threaten", "legal action", "sue", "consequences", "if you don't")
    private val requestKeywords = listOf("ask", "request", "need", "want", "please", "could you")
    private val financialKeywords = listOf("$", "£", "€", "dollar", "pound", "euro", "pay", "paid", "owe", "amount", "cost", "price", "fee")
    private val contractualKeywords = listOf("contract", "agreement", "terms", "conditions", "clause", "provision", "obligation")
    private val testimonyKeywords = listOf("testify", "witness", "swear", "under oath", "depose", "attest")

    // Keywords for sentiment analysis
    private val positiveWords = listOf("good", "great", "excellent", "happy", "pleased", "agree", "yes", "correct", "true", "confirm", "love", "wonderful", "appreciate")
    private val negativeWords = listOf("bad", "terrible", "wrong", "disagree", "no", "false", "deny", "reject", "hate", "awful", "never", "not", "refuse", "disappointed", "upset", "angry")

    // Keywords for certainty detection
    private val certainWords = listOf("definitely", "certainly", "absolutely", "sure", "know", "fact", "proven", "clearly", "always", "never", "every")
    private val uncertainWords = listOf("maybe", "perhaps", "possibly", "might", "could", "uncertain", "unclear", "think", "believe", "guess", "probably", "likely")

    /**
     * Extract statements from document text.
     *
     * @param text Raw document text
     * @param documentId Document identifier
     * @param documentName Human-readable document name
     * @param documentAuthor Optional author to use as default speaker
     * @return List of extracted Statement objects
     */
    fun extractStatements(
        text: String,
        documentId: String,
        documentName: String,
        documentAuthor: String? = null
    ): List<Statement> {
        val statements = mutableListOf<Statement>()
        
        // Normalize text first
        val normalized = TextNormalizer.normalize(text, preserveCase = true)
        
        // Extract speakers and their statements
        val speakerStatements = SpeakerExtractor.extractSpeakers(normalized, documentAuthor)
        
        if (speakerStatements.isNotEmpty()) {
            // Use speaker-attributed statements
            speakerStatements.forEachIndexed { index, ss ->
                if (ss.text.isNotBlank()) {
                    // Extract timestamp from the statement or surrounding context
                    val timestampResult = extractTimestamp(ss.text, normalized)
                    
                    statements.add(
                        Statement(
                            id = UUID.randomUUID().toString(),
                            speaker = ss.speaker,
                            text = ss.text,
                            documentId = documentId,
                            documentName = documentName,
                            lineNumber = index + 1,
                            timestamp = timestampResult?.first,
                            timestampMillis = timestampResult?.second,
                            context = extractContext(normalized, ss.startIndex, ss.endIndex),
                            sentiment = calculateSentiment(ss.text),
                            certainty = calculateCertainty(ss.text),
                            legalCategory = classifyLegalCategory(ss.text)
                        )
                    )
                }
            }
        } else {
            // No speakers identified - extract sentences and use document author or "Unknown"
            val defaultSpeaker = documentAuthor ?: "Unknown"
            val sentences = TextNormalizer.extractSentences(normalized)
            
            sentences.forEachIndexed { index, sentence ->
                val timestampResult = extractTimestamp(sentence, normalized)
                
                statements.add(
                    Statement(
                        id = UUID.randomUUID().toString(),
                        speaker = defaultSpeaker,
                        text = sentence,
                        documentId = documentId,
                        documentName = documentName,
                        lineNumber = index + 1,
                        timestamp = timestampResult?.first,
                        timestampMillis = timestampResult?.second,
                        context = "",
                        sentiment = calculateSentiment(sentence),
                        certainty = calculateCertainty(sentence),
                        legalCategory = classifyLegalCategory(sentence)
                    )
                )
            }
        }
        
        return statements
    }

    /**
     * Extract timestamp from text.
     *
     * @param text Text that may contain a timestamp
     * @param fullContext Full document context for finding timestamps
     * @return Pair of (normalized timestamp string, epoch millis) or null
     */
    private fun extractTimestamp(text: String, fullContext: String): Pair<String, Long>? {
        // First try to extract from the statement text itself
        var result = DateNormalizer.extractAllDatesFromText(text)
            .map { DateNormalizer.normalizeWithMillis(it.second) }
            .filterNotNull()
            .firstOrNull()
        
        if (result != null) return result
        
        // If not found, try finding a date near this text in the full context
        val textIndex = fullContext.indexOf(text)
        if (textIndex > 0) {
            // Look for dates in the 200 characters before this text
            val contextStart = maxOf(0, textIndex - 200)
            val contextBefore = fullContext.substring(contextStart, textIndex)
            result = DateNormalizer.extractAllDatesFromText(contextBefore)
                .map { DateNormalizer.normalizeWithMillis(it.second) }
                .filterNotNull()
                .lastOrNull() // Use the closest date
        }
        
        return result
    }

    /**
     * Extract surrounding context for a statement.
     *
     * @param text Full document text
     * @param startIndex Start index of the statement
     * @param endIndex End index of the statement
     * @return Context string (text before and after the statement)
     */
    private fun extractContext(text: String, startIndex: Int, endIndex: Int): String {
        val contextChars = 100
        val contextStart = maxOf(0, startIndex - contextChars)
        val contextEnd = minOf(text.length, endIndex + contextChars)
        
        val before = if (startIndex > 0) text.substring(contextStart, startIndex).trim() else ""
        val after = if (endIndex < text.length) text.substring(endIndex, contextEnd).trim() else ""
        
        return buildString {
            if (before.isNotBlank()) append("...$before ")
            append("[STATEMENT]")
            if (after.isNotBlank()) append(" $after...")
        }
    }

    /**
     * Calculate sentiment score for text.
     *
     * @param text Text to analyze
     * @return Sentiment score from -1.0 (negative) to 1.0 (positive)
     */
    fun calculateSentiment(text: String): Double {
        val lower = text.lowercase()
        
        val positiveCount = positiveWords.count { lower.contains(it) }
        val negativeCount = negativeWords.count { lower.contains(it) }
        
        val total = positiveCount + negativeCount
        if (total == 0) return 0.0
        
        return (positiveCount - negativeCount).toDouble() / total
    }

    /**
     * Calculate certainty score for text.
     *
     * @param text Text to analyze
     * @return Certainty score from 0.0 (uncertain) to 1.0 (certain)
     */
    fun calculateCertainty(text: String): Double {
        val lower = text.lowercase()
        
        val certainCount = certainWords.count { lower.contains(it) }
        val uncertainCount = uncertainWords.count { lower.contains(it) }
        
        val total = certainCount + uncertainCount
        if (total == 0) return 0.5 // Default to neutral certainty
        
        return (certainCount.toDouble() / total * 0.5) + 0.5 // Scale to 0.5-1.0 range for certain, 0.0-0.5 for uncertain
    }

    /**
     * Classify the legal category of a statement.
     *
     * @param text Statement text
     * @return Legal category
     */
    fun classifyLegalCategory(text: String): LegalCategory {
        val lower = text.lowercase()
        
        return when {
            promiseKeywords.any { lower.contains(it) } -> LegalCategory.PROMISE
            denialKeywords.any { lower.contains(it) } -> LegalCategory.DENIAL
            admissionKeywords.any { lower.contains(it) } -> LegalCategory.ADMISSION
            threatKeywords.any { lower.contains(it) } -> LegalCategory.THREAT
            requestKeywords.any { lower.contains(it) } -> LegalCategory.REQUEST
            financialKeywords.any { lower.contains(it) } -> LegalCategory.FINANCIAL
            contractualKeywords.any { lower.contains(it) } -> LegalCategory.CONTRACTUAL
            testimonyKeywords.any { lower.contains(it) } -> LegalCategory.TESTIMONY
            assertionKeywords.any { lower.contains(it) } -> LegalCategory.ASSERTION
            else -> LegalCategory.GENERAL
        }
    }

    /**
     * Check if a statement contains contradiction indicators.
     *
     * @param text Statement text
     * @return true if statement contains potential contradiction language
     */
    fun hasContradictionIndicators(text: String): Boolean {
        val lower = text.lowercase()
        
        val contradictionPatterns = listOf(
            "but", "however", "although", "nevertheless", "on the other hand",
            "that's not what", "i never said", "i didn't mean", "contrary to",
            "in fact", "actually", "to clarify", "correction"
        )
        
        return contradictionPatterns.any { lower.contains(it) }
    }

    /**
     * Detect if statement is a change of position from a previous claim.
     *
     * @param currentStatement Current statement
     * @param previousStatement Previous statement from same speaker
     * @return true if this appears to be a position change
     */
    fun isPositionChange(currentStatement: String, previousStatement: String): Boolean {
        val currentLower = currentStatement.lowercase()
        val previousLower = previousStatement.lowercase()
        
        // Check for negation patterns
        val negationPairs = listOf(
            Pair("did", "didn't"),
            Pair("was", "wasn't"),
            Pair("is", "isn't"),
            Pair("have", "haven't"),
            Pair("will", "won't"),
            Pair("can", "can't"),
            Pair("agree", "disagree"),
            Pair("true", "false"),
            Pair("yes", "no")
        )
        
        for ((positive, negative) in negationPairs) {
            if ((previousLower.contains(positive) && currentLower.contains(negative)) ||
                (previousLower.contains(negative) && currentLower.contains(positive))) {
                return true
            }
        }
        
        return false
    }
}

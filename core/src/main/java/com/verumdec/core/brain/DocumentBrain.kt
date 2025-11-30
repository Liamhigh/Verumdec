package com.verumdec.core.brain

import android.content.Context
import com.verumdec.core.util.DocumentUtils
import com.verumdec.core.util.HashUtils
import com.verumdec.core.util.MetadataUtils
import java.util.*

/**
 * DocumentBrain - Analyzes document structure and content.
 * Handles PDF, text, and other document formats.
 */
class DocumentBrain(context: Context) : BaseBrain(context) {

    override val brainName = "DocumentBrain"

    /**
     * Analyze document content.
     */
    fun analyze(content: String, fileName: String): BrainResult<DocumentAnalysis> {
        val metadata = mapOf(
            "fileName" to fileName,
            "sha512Hash" to HashUtils.sha512(content),
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(content, "ANALYZE_DOCUMENT", metadata) { text ->
            DocumentAnalysis(
                id = generateProcessingId(),
                fileName = fileName,
                contentHash = HashUtils.sha512(text),
                wordCount = DocumentUtils.countWords(text),
                sentenceCount = DocumentUtils.countSentences(text),
                paragraphCount = DocumentUtils.countParagraphs(text),
                readingTimeMinutes = DocumentUtils.calculateReadingTime(text),
                sections = DocumentUtils.extractSections(text),
                language = DocumentUtils.detectLanguage(text),
                isLegalDocument = DocumentUtils.isLikelyLegalDocument(text),
                isFinancialDocument = DocumentUtils.isLikelyFinancialDocument(text),
                extractedEmails = MetadataUtils.extractAllEmails(text),
                extractedPhones = MetadataUtils.extractPhoneNumbers(text),
                extractedDates = MetadataUtils.extractDates(text),
                extractedAmounts = MetadataUtils.extractMonetaryAmounts(text),
                analyzedAt = Date()
            )
        }
    }

    /**
     * Extract claims from document.
     */
    fun extractClaims(content: String): BrainResult<List<DocumentClaim>> {
        val metadata = mapOf(
            "sha512Hash" to HashUtils.sha512(content),
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(content, "EXTRACT_CLAIMS", metadata) { text ->
            val claims = mutableListOf<DocumentClaim>()
            val sentences = text.split(Regex("[.!?]")).filter { it.trim().length > 20 }

            for (sentence in sentences) {
                val trimmed = sentence.trim()
                val claimType = classifyClaim(trimmed)
                if (claimType != ClaimType.NONE) {
                    claims.add(DocumentClaim(
                        id = HashUtils.generateHashId("claim"),
                        text = trimmed,
                        type = claimType,
                        confidence = calculateConfidence(trimmed, claimType),
                        keywords = extractClaimKeywords(trimmed)
                    ))
                }
            }
            claims
        }
    }

    private fun classifyClaim(text: String): ClaimType {
        val lower = text.lowercase()
        return when {
            lower.contains("i promise") || lower.contains("will ") || lower.contains("shall ") -> ClaimType.PROMISE
            lower.contains("never") || lower.contains("didn't") || lower.contains("did not") -> ClaimType.DENIAL
            lower.contains("admit") || lower.contains("yes i") || lower.contains("i did") -> ClaimType.ADMISSION
            lower.contains("accuse") || lower.contains("fault") || lower.contains("blame") -> ClaimType.ACCUSATION
            lower.contains("claim") || lower.contains("assert") || lower.contains("state") -> ClaimType.ASSERTION
            lower.contains("paid") || lower.contains("transfer") || lower.contains("sent money") -> ClaimType.FINANCIAL
            else -> ClaimType.NONE
        }
    }

    private fun calculateConfidence(text: String, type: ClaimType): Float {
        var confidence = 0.5f
        if (text.contains("\"")) confidence += 0.1f // Has quotes
        if (text.length > 50) confidence += 0.1f // Substantial content
        if (type != ClaimType.NONE) confidence += 0.2f // Has classification
        return confidence.coerceAtMost(1.0f)
    }

    private fun extractClaimKeywords(text: String): List<String> {
        val stopWords = setOf("the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "i", "you", "he", "she", "it", "we", "they", "my", "your", "his", "her")

        return text.lowercase()
            .replace(Regex("[^a-z\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }
            .distinct()
            .take(10)
    }
}

data class DocumentAnalysis(
    val id: String,
    val fileName: String,
    val contentHash: String,
    val wordCount: Int,
    val sentenceCount: Int,
    val paragraphCount: Int,
    val readingTimeMinutes: Int,
    val sections: List<DocumentUtils.DocumentSection>,
    val language: String,
    val isLegalDocument: Boolean,
    val isFinancialDocument: Boolean,
    val extractedEmails: List<String>,
    val extractedPhones: List<String>,
    val extractedDates: List<String>,
    val extractedAmounts: List<String>,
    val analyzedAt: Date
)

data class DocumentClaim(
    val id: String,
    val text: String,
    val type: ClaimType,
    val confidence: Float,
    val keywords: List<String>
)

enum class ClaimType {
    PROMISE,
    DENIAL,
    ADMISSION,
    ACCUSATION,
    ASSERTION,
    FINANCIAL,
    NONE
}

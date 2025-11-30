package com.verumdec.core.brain

import android.content.Context
import com.verumdec.core.util.HashUtils
import java.util.*

/**
 * SignatureBrain - Analyzes signatures and authentication patterns.
 * Detects digital signatures, handwritten signature regions, and signature inconsistencies.
 */
class SignatureBrain(context: Context) : BaseBrain(context) {

    override val brainName = "SignatureBrain"

    /**
     * Analyze document for signatures.
     */
    fun analyzeDocument(content: String, fileName: String): BrainResult<SignatureAnalysis> {
        val metadata = mapOf(
            "fileName" to fileName,
            "sha512Hash" to HashUtils.sha512(content),
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(content, "ANALYZE_SIGNATURES", metadata) { text ->
            val signatures = detectSignatureBlocks(text)
            val digitalSignature = detectDigitalSignature(text)
            
            SignatureAnalysis(
                id = generateProcessingId(),
                fileName = fileName,
                hasSignatureBlock = signatures.isNotEmpty(),
                signatureBlocks = signatures,
                hasDigitalSignature = digitalSignature != null,
                digitalSignatureInfo = digitalSignature,
                hasWitness = detectWitnessBlock(text),
                hasNotarization = detectNotarization(text),
                hasDateWithSignature = detectSignatureDate(text) != null,
                signatureDate = detectSignatureDate(text),
                analyzedAt = Date()
            )
        }
    }

    /**
     * Compare signatures across documents.
     */
    fun compareSignatures(
        signature1: SignatureBlock,
        signature2: SignatureBlock
    ): BrainResult<SignatureComparison> {
        val metadata = mapOf(
            "signature1Id" to signature1.id,
            "signature2Id" to signature2.id,
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(Pair(signature1, signature2), "COMPARE_SIGNATURES", metadata) { (sig1, sig2) ->
            val nameMatch = compareNames(sig1.signerName, sig2.signerName)
            val titleMatch = compareTitles(sig1.title, sig2.title)
            
            SignatureComparison(
                id = generateProcessingId(),
                signature1Id = sig1.id,
                signature2Id = sig2.id,
                namesMatch = nameMatch,
                titlesMatch = titleMatch,
                overallSimilarity = calculateSimilarity(nameMatch, titleMatch),
                discrepancies = findDiscrepancies(sig1, sig2),
                analyzedAt = Date()
            )
        }
    }

    /**
     * Validate signature authenticity markers.
     */
    fun validateAuthenticity(signatureBlock: SignatureBlock, documentContent: String): BrainResult<AuthenticityValidation> {
        val metadata = mapOf(
            "signatureId" to signatureBlock.id,
            "sha512Hash" to HashUtils.sha512(documentContent),
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(signatureBlock, "VALIDATE_AUTHENTICITY", metadata) { signature ->
            val warnings = mutableListOf<String>()
            val flags = mutableListOf<AuthenticityFlag>()

            // Check for suspicious patterns
            if (signature.signerName.isBlank()) {
                warnings.add("Signer name is missing")
                flags.add(AuthenticityFlag.MISSING_NAME)
            }

            if (!hasValidDateFormat(documentContent, signature)) {
                warnings.add("Signature date format is inconsistent")
                flags.add(AuthenticityFlag.DATE_INCONSISTENCY)
            }

            if (detectTypedSignature(documentContent)) {
                warnings.add("Signature appears to be typed rather than handwritten")
                flags.add(AuthenticityFlag.TYPED_SIGNATURE)
            }

            AuthenticityValidation(
                id = generateProcessingId(),
                signatureId = signature.id,
                isLikelyAuthentic = flags.isEmpty(),
                confidenceScore = calculateAuthenticityScore(flags),
                warnings = warnings,
                flags = flags,
                validatedAt = Date()
            )
        }
    }

    private fun detectSignatureBlocks(text: String): List<SignatureBlock> {
        val blocks = mutableListOf<SignatureBlock>()
        val patterns = listOf(
            Regex("Signed:?\\s*[_\\s]*\\n?([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)", RegexOption.IGNORE_CASE),
            Regex("Signature:?\\s*[_\\s]*\\n?([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)", RegexOption.IGNORE_CASE),
            Regex("By:?\\s*[_\\s]*\\n?([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)", RegexOption.IGNORE_CASE),
            Regex("/s/\\s*([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*)", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.findAll(text).forEach { match ->
                val name = match.groupValues.getOrNull(1)?.trim() ?: ""
                if (name.isNotBlank() && name.length > 2) {
                    blocks.add(SignatureBlock(
                        id = HashUtils.generateHashId("sig"),
                        signerName = name,
                        title = extractTitle(text, match.range.last),
                        organization = extractOrganization(text, match.range.last),
                        rawText = match.value,
                        position = match.range.first
                    ))
                }
            }
        }

        return blocks.distinctBy { it.signerName }
    }

    private fun extractTitle(text: String, position: Int): String? {
        val window = text.substring(position, minOf(position + 200, text.length))
        val titlePatterns = listOf(
            Regex("Title:?\\s*([A-Za-z\\s]+)", RegexOption.IGNORE_CASE),
            Regex("Position:?\\s*([A-Za-z\\s]+)", RegexOption.IGNORE_CASE),
            Regex("(CEO|CFO|Director|Manager|President|Partner|Attorney|Counsel)", RegexOption.IGNORE_CASE)
        )

        for (pattern in titlePatterns) {
            val match = pattern.find(window)
            if (match != null) {
                return match.groupValues.getOrNull(1)?.trim()
            }
        }
        return null
    }

    private fun extractOrganization(text: String, position: Int): String? {
        val window = text.substring(position, minOf(position + 200, text.length))
        val orgPatterns = listOf(
            Regex("(?:Company|Organization|Firm|On behalf of):?\\s*([A-Za-z\\s&.,]+)", RegexOption.IGNORE_CASE),
            Regex("([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*(?:\\s+(?:Inc|LLC|Ltd|Corp|Company|LLP))\\b)")
        )

        for (pattern in orgPatterns) {
            val match = pattern.find(window)
            if (match != null) {
                return match.groupValues.getOrNull(1)?.trim()
            }
        }
        return null
    }

    private fun detectDigitalSignature(text: String): DigitalSignatureInfo? {
        val digitalPatterns = listOf(
            Regex("Digitally signed by:?\\s*(.+)", RegexOption.IGNORE_CASE),
            Regex("Digital Signature:?\\s*(.+)", RegexOption.IGNORE_CASE),
            Regex("Certificate:?\\s*([A-Fa-f0-9:]+)"),
            Regex("SHA-?256:?\\s*([A-Fa-f0-9]+)")
        )

        for (pattern in digitalPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return DigitalSignatureInfo(
                    signerInfo = match.groupValues.getOrNull(1) ?: "",
                    certificateHash = extractCertificateHash(text),
                    timestamp = extractDigitalTimestamp(text),
                    isValid = true // Would need actual validation
                )
            }
        }
        return null
    }

    private fun extractCertificateHash(text: String): String? {
        val hashPattern = Regex("(?:SHA-?(?:256|512)|MD5):?\\s*([A-Fa-f0-9]{32,128})")
        return hashPattern.find(text)?.groupValues?.getOrNull(1)
    }

    private fun extractDigitalTimestamp(text: String): Date? {
        val timestampPattern = Regex("(?:Timestamp|Signed on):?\\s*(\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2})")
        val match = timestampPattern.find(text)
        return match?.let { 
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(it.groupValues[1])
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun detectWitnessBlock(text: String): Boolean {
        return text.contains("witness", ignoreCase = true) &&
               (text.contains("signature", ignoreCase = true) || 
                text.contains("signed", ignoreCase = true))
    }

    private fun detectNotarization(text: String): Boolean {
        val notaryTerms = listOf("notary", "notarized", "notarial", "sworn before", "commissioner of oaths")
        return notaryTerms.any { text.contains(it, ignoreCase = true) }
    }

    private fun detectSignatureDate(text: String): Date? {
        val datePatterns = listOf(
            Regex("(?:Date|Dated|Signed on):?\\s*(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4})", RegexOption.IGNORE_CASE),
            Regex("(?:Date|Dated):?\\s*(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4})", RegexOption.IGNORE_CASE)
        )

        for (pattern in datePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return try {
                    com.verumdec.core.util.DateUtils.parseDate(match.groupValues[1])
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }

    private fun compareNames(name1: String?, name2: String?): Boolean {
        if (name1 == null || name2 == null) return false
        return name1.equals(name2, ignoreCase = true) ||
               name1.contains(name2, ignoreCase = true) ||
               name2.contains(name1, ignoreCase = true)
    }

    private fun compareTitles(title1: String?, title2: String?): Boolean {
        if (title1 == null || title2 == null) return title1 == title2
        return title1.equals(title2, ignoreCase = true)
    }

    private fun calculateSimilarity(nameMatch: Boolean, titleMatch: Boolean): Float {
        return when {
            nameMatch && titleMatch -> 1.0f
            nameMatch -> 0.7f
            titleMatch -> 0.3f
            else -> 0.0f
        }
    }

    private fun findDiscrepancies(sig1: SignatureBlock, sig2: SignatureBlock): List<String> {
        val discrepancies = mutableListOf<String>()
        
        if (!sig1.signerName.equals(sig2.signerName, ignoreCase = true)) {
            discrepancies.add("Signer names differ: '${sig1.signerName}' vs '${sig2.signerName}'")
        }
        if (sig1.title != sig2.title) {
            discrepancies.add("Titles differ: '${sig1.title}' vs '${sig2.title}'")
        }
        if (sig1.organization != sig2.organization) {
            discrepancies.add("Organizations differ: '${sig1.organization}' vs '${sig2.organization}'")
        }
        
        return discrepancies
    }

    private fun hasValidDateFormat(text: String, signature: SignatureBlock): Boolean {
        return detectSignatureDate(text) != null
    }

    private fun detectTypedSignature(text: String): Boolean {
        // Look for patterns suggesting typed rather than handwritten
        return text.contains("/s/") || text.contains("(signed)")
    }

    private fun calculateAuthenticityScore(flags: List<AuthenticityFlag>): Float {
        val deductions = mapOf(
            AuthenticityFlag.MISSING_NAME to 0.3f,
            AuthenticityFlag.DATE_INCONSISTENCY to 0.2f,
            AuthenticityFlag.TYPED_SIGNATURE to 0.1f,
            AuthenticityFlag.FORMAT_MISMATCH to 0.15f,
            AuthenticityFlag.SUSPICIOUS_PATTERN to 0.25f
        )
        
        var score = 1.0f
        for (flag in flags) {
            score -= deductions[flag] ?: 0.1f
        }
        return score.coerceIn(0f, 1f)
    }
}

data class SignatureBlock(
    val id: String,
    val signerName: String,
    val title: String?,
    val organization: String?,
    val rawText: String,
    val position: Int
)

data class DigitalSignatureInfo(
    val signerInfo: String,
    val certificateHash: String?,
    val timestamp: Date?,
    val isValid: Boolean
)

data class SignatureAnalysis(
    val id: String,
    val fileName: String,
    val hasSignatureBlock: Boolean,
    val signatureBlocks: List<SignatureBlock>,
    val hasDigitalSignature: Boolean,
    val digitalSignatureInfo: DigitalSignatureInfo?,
    val hasWitness: Boolean,
    val hasNotarization: Boolean,
    val hasDateWithSignature: Boolean,
    val signatureDate: Date?,
    val analyzedAt: Date
)

data class SignatureComparison(
    val id: String,
    val signature1Id: String,
    val signature2Id: String,
    val namesMatch: Boolean,
    val titlesMatch: Boolean,
    val overallSimilarity: Float,
    val discrepancies: List<String>,
    val analyzedAt: Date
)

data class AuthenticityValidation(
    val id: String,
    val signatureId: String,
    val isLikelyAuthentic: Boolean,
    val confidenceScore: Float,
    val warnings: List<String>,
    val flags: List<AuthenticityFlag>,
    val validatedAt: Date
)

enum class AuthenticityFlag {
    MISSING_NAME,
    DATE_INCONSISTENCY,
    TYPED_SIGNATURE,
    FORMAT_MISMATCH,
    SUSPICIOUS_PATTERN
}

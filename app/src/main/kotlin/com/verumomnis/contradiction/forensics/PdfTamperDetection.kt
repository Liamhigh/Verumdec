package com.verumomnis.contradiction.forensics

import java.security.MessageDigest

/**
 * PDF Tamper Detection Module
 *
 * Provides offline PDF forensic analysis capabilities:
 * - Document structure analysis
 * - Metadata consistency checking
 * - Digital signature verification
 * - Modification history detection
 * - Hidden content detection
 */
class PdfTamperDetection {

    companion object {
        private const val HASH_ALGORITHM = "SHA-512"
    }

    /**
     * Performs complete PDF forensic analysis.
     */
    fun analyzePdf(pdfContent: ByteArray, metadata: PdfMetadata): PdfForensicResult {
        val structureAnalysis = analyzeStructure(pdfContent)
        val metadataConsistency = checkMetadataConsistency(metadata)
        val modificationHistory = detectModifications(pdfContent, metadata)
        val hiddenContent = detectHiddenContent(pdfContent)
        val signatureStatus = verifySignatures(pdfContent)

        val tamperingScore = calculateTamperingScore(
            structureAnalysis,
            metadataConsistency,
            modificationHistory,
            hiddenContent,
            signatureStatus
        )

        return PdfForensicResult(
            structureAnalysis = structureAnalysis,
            metadataConsistency = metadataConsistency,
            modificationHistory = modificationHistory,
            hiddenContent = hiddenContent,
            signatureStatus = signatureStatus,
            contentHash = computeHash(pdfContent),
            tamperingScore = tamperingScore,
            isTampered = tamperingScore > 0.5f
        )
    }

    /**
     * Analyzes PDF internal structure for anomalies.
     */
    private fun analyzeStructure(content: ByteArray): PdfStructureAnalysis {
        val contentStr = String(content, Charsets.ISO_8859_1)

        // Check PDF header
        val hasValidHeader = contentStr.startsWith("%PDF-")
        val pdfVersion = if (hasValidHeader) {
            contentStr.substring(5, 8)
        } else null

        // Count objects
        val objectPattern = Regex("\\d+ \\d+ obj")
        val objectCount = objectPattern.findAll(contentStr).count()

        // Check for incremental updates (sign of modifications)
        val trailerCount = contentStr.split("%%EOF").size - 1
        val hasIncrementalUpdates = trailerCount > 1

        // Check for suspicious patterns
        val hasJavaScript = contentStr.contains("/JavaScript") || contentStr.contains("/JS")
        val hasExternalLinks = contentStr.contains("/URI") || contentStr.contains("/GoToR")
        val hasEmbeddedFiles = contentStr.contains("/EmbeddedFile")

        // Check cross-reference table
        val hasXrefStream = contentStr.contains("/XRef")
        val hasXrefTable = contentStr.contains("xref")

        return PdfStructureAnalysis(
            hasValidHeader = hasValidHeader,
            pdfVersion = pdfVersion,
            objectCount = objectCount,
            hasIncrementalUpdates = hasIncrementalUpdates,
            incrementalUpdateCount = trailerCount - 1,
            hasJavaScript = hasJavaScript,
            hasExternalLinks = hasExternalLinks,
            hasEmbeddedFiles = hasEmbeddedFiles,
            usesXrefStream = hasXrefStream,
            usesXrefTable = hasXrefTable
        )
    }

    /**
     * Checks metadata for consistency issues.
     */
    private fun checkMetadataConsistency(metadata: PdfMetadata): MetadataConsistency {
        val issues = mutableListOf<String>()

        // Check for date consistency
        if (metadata.creationDate != null && metadata.modificationDate != null) {
            if (metadata.modificationDate < metadata.creationDate) {
                issues.add("Modification date precedes creation date")
            }
        }

        // Check for producer/creator mismatch
        if (metadata.producer != null && metadata.creator != null) {
            val commonProducers = listOf("Adobe", "Microsoft", "LibreOffice", "PDFlib")
            val producerFamily = commonProducers.find { metadata.producer.contains(it, ignoreCase = true) }
            val creatorFamily = commonProducers.find { metadata.creator.contains(it, ignoreCase = true) }

            if (producerFamily != null && creatorFamily != null && producerFamily != creatorFamily) {
                issues.add("Producer/Creator software mismatch suggests re-saving")
            }
        }

        // Check for missing metadata
        if (metadata.creationDate == null && metadata.modificationDate != null) {
            issues.add("Creation date missing but modification date exists")
        }

        // Check for future dates
        val currentTime = System.currentTimeMillis()
        if (metadata.creationDate != null && metadata.creationDate > currentTime) {
            issues.add("Creation date is in the future")
        }

        val isConsistent = issues.isEmpty()
        val confidenceScore = if (isConsistent) 1.0f else 1.0f - (issues.size * 0.2f).coerceAtMost(0.8f)

        return MetadataConsistency(
            isConsistent = isConsistent,
            issues = issues,
            confidenceScore = confidenceScore
        )
    }

    /**
     * Detects signs of PDF modifications.
     */
    private fun detectModifications(content: ByteArray, metadata: PdfMetadata): ModificationHistory {
        val contentStr = String(content, Charsets.ISO_8859_1)
        val modifications = mutableListOf<ModificationEvent>()

        // Count number of EOF markers (each incremental save adds one)
        val eofPositions = mutableListOf<Int>()
        var searchPos = 0
        while (true) {
            val pos = contentStr.indexOf("%%EOF", searchPos)
            if (pos == -1) break
            eofPositions.add(pos)
            searchPos = pos + 5
        }

        // Analyze each version
        for (i in 0 until eofPositions.size - 1) {
            val updateSize = eofPositions[i + 1] - eofPositions[i]
            modifications.add(
                ModificationEvent(
                    versionNumber = i + 2,
                    approximateSize = updateSize,
                    eventType = ModificationEventType.INCREMENTAL_UPDATE
                )
            )
        }

        // Check for object stream modifications
        val objStmPattern = Regex("/ObjStm")
        val hasObjectStreams = objStmPattern.containsMatchIn(contentStr)

        // Check for linearization (web-optimized PDFs)
        val isLinearized = contentStr.contains("/Linearized")

        return ModificationHistory(
            totalVersions = eofPositions.size,
            modifications = modifications,
            hasObjectStreams = hasObjectStreams,
            isLinearized = isLinearized,
            wasRebuilt = eofPositions.size > 1 && !hasObjectStreams
        )
    }

    /**
     * Detects hidden or suspicious content in PDF.
     */
    private fun detectHiddenContent(content: ByteArray): HiddenContentAnalysis {
        val contentStr = String(content, Charsets.ISO_8859_1)
        val findings = mutableListOf<HiddenContentFinding>()

        // Check for hidden layers
        if (contentStr.contains("/OCProperties") || contentStr.contains("/OC")) {
            val layerPattern = Regex("/Name\\s*\\(([^)]+)\\)")
            val layers = layerPattern.findAll(contentStr).map { it.groupValues[1] }.toList()
            if (layers.isNotEmpty()) {
                findings.add(
                    HiddenContentFinding(
                        type = HiddenContentType.OPTIONAL_CONTENT_LAYERS,
                        description = "Document contains ${layers.size} optional content layers",
                        details = layers.take(5)
                    )
                )
            }
        }

        // Check for invisible text (white text on white background)
        if (contentStr.contains("1 1 1 rg") || contentStr.contains("1 g")) {
            findings.add(
                HiddenContentFinding(
                    type = HiddenContentType.WHITE_TEXT,
                    description = "Document may contain white/invisible text",
                    details = emptyList()
                )
            )
        }

        // Check for hidden annotations
        val annotPattern = Regex("/Annot.*?/F\\s+(\\d+)")
        annotPattern.findAll(contentStr).forEach { match ->
            val flags = match.groupValues[1].toIntOrNull() ?: 0
            if (flags and 2 != 0) { // Hidden flag
                findings.add(
                    HiddenContentFinding(
                        type = HiddenContentType.HIDDEN_ANNOTATIONS,
                        description = "Hidden annotations detected",
                        details = emptyList()
                    )
                )
            }
        }

        // Check for suspicious content after EOF
        val lastEof = contentStr.lastIndexOf("%%EOF")
        if (lastEof != -1 && lastEof + 6 < content.size) {
            val afterEof = content.sliceArray(lastEof + 6 until content.size)
            if (afterEof.any { it != 0.toByte() && it != '\n'.code.toByte() && it != '\r'.code.toByte() }) {
                findings.add(
                    HiddenContentFinding(
                        type = HiddenContentType.CONTENT_AFTER_EOF,
                        description = "Data found after document end marker",
                        details = listOf("${afterEof.size} bytes after %%EOF")
                    )
                )
            }
        }

        return HiddenContentAnalysis(
            hasHiddenContent = findings.isNotEmpty(),
            findings = findings
        )
    }

    /**
     * Verifies digital signatures in PDF.
     */
    private fun verifySignatures(content: ByteArray): SignatureStatus {
        val contentStr = String(content, Charsets.ISO_8859_1)

        // Check for signature dictionaries
        val hasSigField = contentStr.contains("/Sig") && contentStr.contains("/SubFilter")
        val sigCount = Regex("/Type\\s*/Sig").findAll(contentStr).count()

        if (!hasSigField || sigCount == 0) {
            return SignatureStatus(
                hasSignatures = false,
                signatureCount = 0,
                signatures = emptyList(),
                allValid = true // No signatures = nothing to validate
            )
        }

        // Parse signature information (simplified)
        val signatures = mutableListOf<SignatureInfo>()

        // Look for signature details
        val subFilterPattern = Regex("/SubFilter\\s*/([^\\s/>]+)")
        val subFilters = subFilterPattern.findAll(contentStr).map { it.groupValues[1] }.toList()

        for (i in 0 until sigCount) {
            val subFilter = subFilters.getOrNull(i) ?: "Unknown"
            signatures.add(
                SignatureInfo(
                    signatureIndex = i,
                    subFilter = subFilter,
                    isValid = null, // Would need actual cryptographic verification
                    signerName = null,
                    signDate = null,
                    coversWholeDocument = i == sigCount - 1 // Last signature typically covers all
                )
            )
        }

        return SignatureStatus(
            hasSignatures = true,
            signatureCount = sigCount,
            signatures = signatures,
            allValid = null // Cannot verify without proper certificate chain
        )
    }

    /**
     * Computes SHA-512 hash of content.
     */
    private fun computeHash(content: ByteArray): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = digest.digest(content)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculates overall tampering score.
     */
    private fun calculateTamperingScore(
        structure: PdfStructureAnalysis,
        metadata: MetadataConsistency,
        history: ModificationHistory,
        hidden: HiddenContentAnalysis,
        signatures: SignatureStatus
    ): Float {
        var score = 0f

        // Structure issues
        if (!structure.hasValidHeader) score += 0.3f
        if (structure.hasJavaScript) score += 0.1f
        if (structure.incrementalUpdateCount > 5) score += 0.15f

        // Metadata issues
        score += (1f - metadata.confidenceScore) * 0.2f

        // Modification history
        if (history.wasRebuilt) score += 0.15f
        if (history.totalVersions > 3) score += 0.1f

        // Hidden content
        if (hidden.hasHiddenContent) {
            score += 0.15f * hidden.findings.size.coerceAtMost(3)
        }

        // Signature issues
        if (signatures.hasSignatures && signatures.allValid == false) {
            score += 0.3f
        }

        return score.coerceIn(0f, 1f)
    }
}

/**
 * Complete PDF forensic analysis result.
 */
data class PdfForensicResult(
    val structureAnalysis: PdfStructureAnalysis,
    val metadataConsistency: MetadataConsistency,
    val modificationHistory: ModificationHistory,
    val hiddenContent: HiddenContentAnalysis,
    val signatureStatus: SignatureStatus,
    val contentHash: String,
    val tamperingScore: Float,
    val isTampered: Boolean
)

/**
 * PDF structure analysis.
 */
data class PdfStructureAnalysis(
    val hasValidHeader: Boolean,
    val pdfVersion: String?,
    val objectCount: Int,
    val hasIncrementalUpdates: Boolean,
    val incrementalUpdateCount: Int,
    val hasJavaScript: Boolean,
    val hasExternalLinks: Boolean,
    val hasEmbeddedFiles: Boolean,
    val usesXrefStream: Boolean,
    val usesXrefTable: Boolean
)

/**
 * PDF metadata for analysis.
 */
data class PdfMetadata(
    val title: String?,
    val author: String?,
    val subject: String?,
    val creator: String?,
    val producer: String?,
    val creationDate: Long?,
    val modificationDate: Long?,
    val keywords: String?
)

/**
 * Metadata consistency result.
 */
data class MetadataConsistency(
    val isConsistent: Boolean,
    val issues: List<String>,
    val confidenceScore: Float
)

/**
 * Modification history analysis.
 */
data class ModificationHistory(
    val totalVersions: Int,
    val modifications: List<ModificationEvent>,
    val hasObjectStreams: Boolean,
    val isLinearized: Boolean,
    val wasRebuilt: Boolean
)

/**
 * Modification event.
 */
data class ModificationEvent(
    val versionNumber: Int,
    val approximateSize: Int,
    val eventType: ModificationEventType
)

/**
 * Types of modification events.
 */
enum class ModificationEventType {
    INCREMENTAL_UPDATE,
    FULL_SAVE,
    OPTIMIZE,
    SIGN
}

/**
 * Hidden content analysis.
 */
data class HiddenContentAnalysis(
    val hasHiddenContent: Boolean,
    val findings: List<HiddenContentFinding>
)

/**
 * Hidden content finding.
 */
data class HiddenContentFinding(
    val type: HiddenContentType,
    val description: String,
    val details: List<String>
)

/**
 * Types of hidden content.
 */
enum class HiddenContentType {
    OPTIONAL_CONTENT_LAYERS,
    WHITE_TEXT,
    HIDDEN_ANNOTATIONS,
    EMBEDDED_FILES,
    CONTENT_AFTER_EOF
}

/**
 * Digital signature status.
 */
data class SignatureStatus(
    val hasSignatures: Boolean,
    val signatureCount: Int,
    val signatures: List<SignatureInfo>,
    val allValid: Boolean?
)

/**
 * Individual signature information.
 */
data class SignatureInfo(
    val signatureIndex: Int,
    val subFilter: String,
    val isValid: Boolean?,
    val signerName: String?,
    val signDate: String?,
    val coversWholeDocument: Boolean
)

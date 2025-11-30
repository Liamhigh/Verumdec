package com.verumdec.core.brain

import android.content.Context
import com.verumdec.core.util.HashUtils
import java.util.*

/**
 * PDFBrain - Analyzes PDF documents.
 * Handles structure analysis, text extraction guidance, and metadata.
 */
class PDFBrain(context: Context) : BaseBrain(context) {

    override val brainName = "PDFBrain"

    /**
     * Analyze PDF structure.
     */
    fun analyzeStructure(
        pageCount: Int,
        textContent: String,
        metadata: PDFMetadata
    ): BrainResult<PDFStructureAnalysis> {
        val inputMetadata = mapOf(
            "pageCount" to pageCount,
            "sha512Hash" to HashUtils.sha512(textContent),
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(Triple(pageCount, textContent, metadata), "ANALYZE_PDF", inputMetadata) { (pages, text, meta) ->
            val hasSignatures = detectSignatureFields(text)
            val hasForms = detectFormFields(text)
            val hasImages = estimateImageContent(text, pages)
            
            PDFStructureAnalysis(
                id = generateProcessingId(),
                fileName = meta.fileName,
                pageCount = pages,
                hasSignatureFields = hasSignatures,
                hasFormFields = hasForms,
                hasImages = hasImages,
                isScanned = detectIfScanned(text, pages),
                isEncrypted = meta.isEncrypted,
                hasWatermark = detectWatermark(text),
                textDensity = calculateTextDensity(text, pages),
                creationDate = meta.creationDate,
                modificationDate = meta.modificationDate,
                author = meta.author,
                producer = meta.producer,
                analyzedAt = Date()
            )
        }
    }

    /**
     * Classify PDF document type.
     */
    fun classifyDocument(textContent: String, structure: PDFStructureAnalysis): BrainResult<PDFClassification> {
        val metadata = mapOf(
            "structureId" to structure.id,
            "sha512Hash" to HashUtils.sha512(textContent),
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(Pair(textContent, structure), "CLASSIFY_PDF", metadata) { (text, struct) ->
            val docType = determineDocumentType(text, struct)
            val confidence = calculateClassificationConfidence(text, docType)
            
            PDFClassification(
                id = generateProcessingId(),
                documentType = docType,
                confidence = confidence,
                subTypes = identifySubTypes(text, docType),
                keyIndicators = extractKeyIndicators(text, docType),
                classifiedAt = Date()
            )
        }
    }

    /**
     * Extract evidence markers from PDF.
     */
    fun extractEvidenceMarkers(textContent: String, fileName: String): BrainResult<PDFEvidenceMarkers> {
        val metadata = mapOf(
            "fileName" to fileName,
            "sha512Hash" to HashUtils.sha512(textContent),
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(textContent, "EXTRACT_MARKERS", metadata) { text ->
            PDFEvidenceMarkers(
                id = generateProcessingId(),
                fileName = fileName,
                dates = extractDates(text),
                amounts = extractAmounts(text),
                names = extractNames(text),
                references = extractReferences(text),
                signatures = extractSignatureMarkers(text),
                stamps = extractStamps(text),
                extractedAt = Date()
            )
        }
    }

    // Helper methods
    
    private fun detectSignatureFields(text: String): Boolean {
        val signatureIndicators = listOf(
            "signature", "signed", "sign here", "_____________",
            "/s/", "x_______", "authorized signature"
        )
        return signatureIndicators.any { text.contains(it, ignoreCase = true) }
    }

    private fun detectFormFields(text: String): Boolean {
        val formIndicators = listOf(
            "[ ]", "[x]", "□", "☐", "☑", "name:", "date:", "address:",
            "please fill", "required field"
        )
        return formIndicators.any { text.contains(it, ignoreCase = true) }
    }

    private fun estimateImageContent(text: String, pageCount: Int): Boolean {
        // If text is sparse relative to pages, likely has images
        val textPerPage = text.length / maxOf(pageCount, 1)
        return textPerPage < 500 // Less than 500 chars per page suggests images
    }

    private fun detectIfScanned(text: String, pageCount: Int): Boolean {
        // Scanned docs often have OCR artifacts
        val ocrPatterns = listOf("I", "l", "1", "O", "0")
        val confusionRatio = ocrPatterns.sumOf { char -> 
            text.count { it.toString() == char }
        } / maxOf(text.length, 1).toFloat()
        
        // Also check if text density is low
        val textPerPage = text.length / maxOf(pageCount, 1)
        
        return confusionRatio > 0.1 || textPerPage < 200
    }

    private fun detectWatermark(text: String): Boolean {
        val watermarkIndicators = listOf(
            "confidential", "draft", "copy", "sample", "void",
            "do not copy", "official use only"
        )
        return watermarkIndicators.any { 
            val count = Regex(it, RegexOption.IGNORE_CASE).findAll(text).count()
            count >= 3 // Watermark would repeat
        }
    }

    private fun calculateTextDensity(text: String, pageCount: Int): Float {
        val charsPerPage = text.length / maxOf(pageCount, 1)
        return when {
            charsPerPage >= 3000 -> 1.0f // Dense
            charsPerPage >= 2000 -> 0.8f
            charsPerPage >= 1000 -> 0.6f
            charsPerPage >= 500 -> 0.4f
            else -> 0.2f // Sparse
        }
    }

    private fun determineDocumentType(text: String, structure: PDFStructureAnalysis): PDFDocumentType {
        val lower = text.lowercase()
        
        return when {
            lower.contains("invoice") || lower.contains("bill to") -> PDFDocumentType.INVOICE
            lower.contains("contract") || lower.contains("agreement") -> PDFDocumentType.CONTRACT
            lower.contains("receipt") || lower.contains("payment received") -> PDFDocumentType.RECEIPT
            lower.contains("bank statement") || lower.contains("account statement") -> PDFDocumentType.BANK_STATEMENT
            lower.contains("court") || lower.contains("plaintiff") || lower.contains("defendant") -> PDFDocumentType.LEGAL_FILING
            lower.contains("affidavit") || lower.contains("sworn") -> PDFDocumentType.AFFIDAVIT
            structure.hasSignatureFields && lower.contains("agreement") -> PDFDocumentType.SIGNED_AGREEMENT
            lower.contains("letter") || lower.contains("dear") -> PDFDocumentType.LETTER
            lower.contains("report") -> PDFDocumentType.REPORT
            else -> PDFDocumentType.GENERAL
        }
    }

    private fun calculateClassificationConfidence(text: String, docType: PDFDocumentType): Float {
        val typeIndicators = when (docType) {
            PDFDocumentType.INVOICE -> listOf("invoice", "bill", "total", "amount due", "payment")
            PDFDocumentType.CONTRACT -> listOf("agreement", "party", "terms", "conditions", "whereas")
            PDFDocumentType.RECEIPT -> listOf("receipt", "paid", "thank you", "transaction")
            PDFDocumentType.BANK_STATEMENT -> listOf("balance", "transaction", "account", "statement")
            PDFDocumentType.LEGAL_FILING -> listOf("court", "case", "motion", "order", "judgment")
            PDFDocumentType.AFFIDAVIT -> listOf("affidavit", "sworn", "notary", "witness")
            PDFDocumentType.SIGNED_AGREEMENT -> listOf("signature", "signed", "agreed", "date")
            PDFDocumentType.LETTER -> listOf("dear", "sincerely", "regards")
            PDFDocumentType.REPORT -> listOf("report", "summary", "findings", "conclusion")
            PDFDocumentType.GENERAL -> emptyList()
        }
        
        if (typeIndicators.isEmpty()) return 0.5f
        
        val matchCount = typeIndicators.count { text.contains(it, ignoreCase = true) }
        return (matchCount.toFloat() / typeIndicators.size).coerceAtMost(1f)
    }

    private fun identifySubTypes(text: String, mainType: PDFDocumentType): List<String> {
        val subTypes = mutableListOf<String>()
        
        when (mainType) {
            PDFDocumentType.CONTRACT -> {
                if (text.contains("employment", ignoreCase = true)) subTypes.add("Employment Contract")
                if (text.contains("lease", ignoreCase = true)) subTypes.add("Lease Agreement")
                if (text.contains("sale", ignoreCase = true)) subTypes.add("Sales Contract")
                if (text.contains("service", ignoreCase = true)) subTypes.add("Service Agreement")
            }
            PDFDocumentType.LEGAL_FILING -> {
                if (text.contains("motion", ignoreCase = true)) subTypes.add("Motion")
                if (text.contains("complaint", ignoreCase = true)) subTypes.add("Complaint")
                if (text.contains("answer", ignoreCase = true)) subTypes.add("Answer")
                if (text.contains("order", ignoreCase = true)) subTypes.add("Court Order")
            }
            else -> {}
        }
        
        return subTypes
    }

    private fun extractKeyIndicators(text: String, docType: PDFDocumentType): List<String> {
        val indicators = mutableListOf<String>()
        
        // Extract specific patterns based on document type
        when (docType) {
            PDFDocumentType.INVOICE -> {
                Regex("Invoice\\s*#?:?\\s*([A-Z0-9-]+)", RegexOption.IGNORE_CASE)
                    .find(text)?.let { indicators.add("Invoice: ${it.groupValues[1]}") }
            }
            PDFDocumentType.LEGAL_FILING -> {
                Regex("Case\\s*(?:No\\.?|Number)?:?\\s*([A-Z0-9-/]+)", RegexOption.IGNORE_CASE)
                    .find(text)?.let { indicators.add("Case: ${it.groupValues[1]}") }
            }
            else -> {}
        }
        
        return indicators.take(5)
    }

    private fun extractDates(text: String): List<String> {
        val datePatterns = listOf(
            Regex("\\d{1,2}/\\d{1,2}/\\d{2,4}"),
            Regex("\\d{4}-\\d{2}-\\d{2}"),
            Regex("\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}", RegexOption.IGNORE_CASE)
        )
        
        return datePatterns.flatMap { pattern ->
            pattern.findAll(text).map { it.value }.toList()
        }.distinct().take(20)
    }

    private fun extractAmounts(text: String): List<String> {
        val amountPattern = Regex("[$€£¥R]\\s*[0-9,]+\\.?[0-9]*")
        return amountPattern.findAll(text).map { it.value }.distinct().toList().take(20)
    }

    private fun extractNames(text: String): List<String> {
        val namePattern = Regex("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+\\b")
        return namePattern.findAll(text).map { it.value }.distinct().toList().take(20)
    }

    private fun extractReferences(text: String): List<String> {
        val refPatterns = listOf(
            Regex("(?:Ref|Reference|Case|File|Contract|Invoice)\\s*(?:No\\.?|#)?:?\\s*([A-Z0-9-/]+)", RegexOption.IGNORE_CASE)
        )
        
        return refPatterns.flatMap { pattern ->
            pattern.findAll(text).map { "${it.groupValues[0]}" }.toList()
        }.distinct().take(10)
    }

    private fun extractSignatureMarkers(text: String): List<String> {
        val markers = mutableListOf<String>()
        
        Regex("(?:Signed|Signature):?\\s*([A-Za-z\\s]+)", RegexOption.IGNORE_CASE)
            .findAll(text).forEach { markers.add(it.value) }
        
        return markers.distinct().take(10)
    }

    private fun extractStamps(text: String): List<String> {
        val stampIndicators = listOf(
            Regex("(?:Notary|Notarized|Certified|Official).{0,50}", RegexOption.IGNORE_CASE),
            Regex("(?:Received|Filed|Recorded)\\s+\\d{1,2}/\\d{1,2}/\\d{2,4}", RegexOption.IGNORE_CASE)
        )
        
        return stampIndicators.flatMap { pattern ->
            pattern.findAll(text).map { it.value.trim() }.toList()
        }.distinct().take(10)
    }
}

// Data classes

data class PDFMetadata(
    val fileName: String,
    val creationDate: Date? = null,
    val modificationDate: Date? = null,
    val author: String? = null,
    val producer: String? = null,
    val isEncrypted: Boolean = false
)

data class PDFStructureAnalysis(
    val id: String,
    val fileName: String,
    val pageCount: Int,
    val hasSignatureFields: Boolean,
    val hasFormFields: Boolean,
    val hasImages: Boolean,
    val isScanned: Boolean,
    val isEncrypted: Boolean,
    val hasWatermark: Boolean,
    val textDensity: Float,
    val creationDate: Date?,
    val modificationDate: Date?,
    val author: String?,
    val producer: String?,
    val analyzedAt: Date
)

enum class PDFDocumentType {
    INVOICE, CONTRACT, RECEIPT, BANK_STATEMENT, LEGAL_FILING,
    AFFIDAVIT, SIGNED_AGREEMENT, LETTER, REPORT, GENERAL
}

data class PDFClassification(
    val id: String,
    val documentType: PDFDocumentType,
    val confidence: Float,
    val subTypes: List<String>,
    val keyIndicators: List<String>,
    val classifiedAt: Date
)

data class PDFEvidenceMarkers(
    val id: String,
    val fileName: String,
    val dates: List<String>,
    val amounts: List<String>,
    val names: List<String>,
    val references: List<String>,
    val signatures: List<String>,
    val stamps: List<String>,
    val extractedAt: Date
)

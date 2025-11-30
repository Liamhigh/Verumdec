package com.verumdec.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.verumdec.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Evidence Ingestion Layer
 * Handles extraction of text and metadata from various evidence types.
 * Implements hash and origin propagation for chain of custody.
 */
class EvidenceProcessor(private val context: Context) {

    init {
        // Initialize PDFBox for Android
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Process a piece of evidence and extract text and metadata.
     * Includes hash generation and origin tracking.
     */
    suspend fun processEvidence(evidence: Evidence, uri: Uri): Evidence {
        return withContext(Dispatchers.IO) {
            // Calculate SHA-512 hash for chain of custody
            val contentHash = calculateHash(uri)
            
            val processed = when (evidence.type) {
                EvidenceType.PDF -> processPdf(evidence, uri)
                EvidenceType.IMAGE -> processImage(evidence, uri)
                EvidenceType.TEXT -> processText(evidence, uri)
                EvidenceType.EMAIL -> processEmail(evidence, uri)
                EvidenceType.WHATSAPP -> processWhatsApp(evidence, uri)
                EvidenceType.AUDIO -> processAudio(evidence, uri)
                EvidenceType.VIDEO -> processVideo(evidence, uri)
                EvidenceType.UNKNOWN -> processFallback(evidence, uri)
            }
            
            // Propagate hash and origin
            processed.copy(
                sha512Hash = contentHash,
                originUri = uri.toString(),
                processedAt = Date()
            )
        }
    }

    /**
     * Calculate SHA-512 hash for the evidence file.
     */
    private fun calculateHash(uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val digest = MessageDigest.getInstance("SHA-512")
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Extract text from PDF using PDFBox.
     */
    private fun processPdf(evidence: Evidence, uri: Uri): Evidence {
        var extractedText = ""
        var metadata = evidence.metadata
        var pageCount = 0

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    pageCount = document.numberOfPages
                    
                    // Extract text
                    val stripper = PDFTextStripper()
                    extractedText = stripper.getText(document)

                    // Extract metadata
                    val info = document.documentInformation
                    metadata = EvidenceMetadata(
                        creationDate = info.creationDate?.time?.let { Date(it.time) },
                        modificationDate = info.modificationDate?.time?.let { Date(it.time) },
                        author = info.author,
                        subject = info.subject,
                        pageCount = pageCount
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return evidence.copy(
            extractedText = extractedText,
            metadata = metadata,
            processed = true
        )
    }

    /**
     * Extract text from image using ML Kit OCR.
     */
    private suspend fun processImage(evidence: Evidence, uri: Uri): Evidence {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            val result = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(visionText.text)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }

            evidence.copy(
                extractedText = result,
                metadata = evidence.metadata.copy(
                    width = image.width,
                    height = image.height
                ),
                processed = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            evidence.copy(processed = true)
        }
    }

    /**
     * Process plain text evidence.
     */
    private fun processText(evidence: Evidence, uri: Uri): Evidence {
        var text = ""
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                text = inputStream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return evidence.copy(
            extractedText = text,
            metadata = evidence.metadata.copy(
                charCount = text.length,
                wordCount = text.split(Regex("\\s+")).size
            ),
            processed = true
        )
    }

    /**
     * Process email export (EML or text format).
     */
    private fun processEmail(evidence: Evidence, uri: Uri): Evidence {
        var text = ""
        var metadata = evidence.metadata

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                text = inputStream.bufferedReader().readText()
                
                // Parse email headers
                val fromMatch = Regex("From:\\s*(.+)", RegexOption.IGNORE_CASE).find(text)
                val toMatch = Regex("To:\\s*(.+)", RegexOption.IGNORE_CASE).find(text)
                val subjectMatch = Regex("Subject:\\s*(.+)", RegexOption.IGNORE_CASE).find(text)
                val dateMatch = Regex("Date:\\s*(.+)", RegexOption.IGNORE_CASE).find(text)
                val ccMatch = Regex("Cc:\\s*(.+)", RegexOption.IGNORE_CASE).find(text)

                metadata = EvidenceMetadata(
                    sender = fromMatch?.groupValues?.getOrNull(1)?.trim(),
                    receiver = toMatch?.groupValues?.getOrNull(1)?.trim(),
                    subject = subjectMatch?.groupValues?.getOrNull(1)?.trim(),
                    creationDate = dateMatch?.groupValues?.getOrNull(1)?.let { parseDate(it) },
                    cc = ccMatch?.groupValues?.getOrNull(1)?.trim()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return evidence.copy(
            extractedText = text,
            metadata = metadata,
            processed = true
        )
    }

    /**
     * Process WhatsApp export.
     */
    private fun processWhatsApp(evidence: Evidence, uri: Uri): Evidence {
        var text = ""
        var metadata = evidence.metadata
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                text = inputStream.bufferedReader().readText()
                
                // Extract WhatsApp specific metadata
                val messageCount = Regex("\\[\\d{1,2}/\\d{1,2}/\\d{2,4},").findAll(text).count()
                val participants = extractWhatsAppParticipants(text)
                
                metadata = evidence.metadata.copy(
                    messageCount = messageCount,
                    participants = participants.joinToString(", ")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return evidence.copy(
            extractedText = text,
            metadata = metadata,
            processed = true
        )
    }

    /**
     * Process audio evidence (metadata only, no transcription).
     */
    private fun processAudio(evidence: Evidence, uri: Uri): Evidence {
        var metadata = evidence.metadata
        
        try {
            // Get file size for duration estimation
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileSize = inputStream.available().toLong()
                // Rough duration estimate: 128kbps = 16KB/s
                val estimatedDurationSeconds = fileSize / 16000
                
                metadata = evidence.metadata.copy(
                    durationSeconds = estimatedDurationSeconds,
                    fileSize = fileSize
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return evidence.copy(
            extractedText = "[Audio file - transcription not available]",
            metadata = metadata,
            processed = true
        )
    }

    /**
     * Process video evidence (metadata only).
     */
    private fun processVideo(evidence: Evidence, uri: Uri): Evidence {
        var metadata = evidence.metadata
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileSize = inputStream.available().toLong()
                // Rough duration estimate: 5Mbps = 625KB/s
                val estimatedDurationSeconds = fileSize / 625000
                
                metadata = evidence.metadata.copy(
                    durationSeconds = estimatedDurationSeconds,
                    fileSize = fileSize
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return evidence.copy(
            extractedText = "[Video file - analysis not available]",
            metadata = metadata,
            processed = true
        )
    }

    /**
     * Fallback processing for unknown types.
     */
    private fun processFallback(evidence: Evidence, uri: Uri): Evidence {
        var text = ""
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Try to read as text
                val bytes = inputStream.readBytes()
                // Check if content is text-like
                val textRatio = bytes.count { it in 0x20..0x7E || it == 0x0A.toByte() || it == 0x0D.toByte() }
                    .toFloat() / bytes.size
                
                if (textRatio > 0.8) {
                    text = String(bytes)
                } else {
                    text = "[Binary file - content not extractable]"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return evidence.copy(
            extractedText = text,
            processed = true
        )
    }

    /**
     * Extract participants from WhatsApp chat.
     */
    private fun extractWhatsAppParticipants(text: String): Set<String> {
        val participants = mutableSetOf<String>()
        val pattern = Regex("\\] ([^:]+):")
        pattern.findAll(text).forEach { match ->
            val name = match.groupValues.getOrNull(1)?.trim()
            if (!name.isNullOrBlank() && name.length < 50) {
                participants.add(name)
            }
        }
        return participants
    }

    /**
     * Parse various date formats.
     */
    private fun parseDate(dateStr: String): Date? {
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "dd/MM/yyyy HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "dd MMM yyyy HH:mm",
            "MM/dd/yyyy HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss",
            "dd-MM-yyyy HH:mm:ss"
        )
        
        for (format in formats) {
            try {
                return SimpleDateFormat(format, Locale.US).parse(dateStr.trim())
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Classify content type from text analysis.
     */
    fun classifyContent(text: String): ContentClassification {
        val legalTerms = listOf("hereby", "whereas", "pursuant", "agreement", "contract")
        val financialTerms = listOf("invoice", "payment", "amount", "balance", "transaction")
        val communicationTerms = listOf("dear", "regards", "sincerely", "hi", "hello")
        
        val legalScore = legalTerms.count { text.contains(it, ignoreCase = true) }
        val financialScore = financialTerms.count { text.contains(it, ignoreCase = true) }
        val communicationScore = communicationTerms.count { text.contains(it, ignoreCase = true) }
        
        return when {
            legalScore >= 3 -> ContentClassification.LEGAL
            financialScore >= 3 -> ContentClassification.FINANCIAL
            communicationScore >= 2 -> ContentClassification.COMMUNICATION
            else -> ContentClassification.GENERAL
        }
    }

    companion object {
        /**
         * Determine evidence type from file extension.
         */
        fun getEvidenceType(fileName: String): EvidenceType {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return when (extension) {
                "pdf" -> EvidenceType.PDF
                "jpg", "jpeg", "png", "bmp", "gif", "webp" -> EvidenceType.IMAGE
                "txt" -> EvidenceType.TEXT
                "eml", "msg" -> EvidenceType.EMAIL
                "mp3", "wav", "m4a", "aac", "ogg", "flac" -> EvidenceType.AUDIO
                "mp4", "mov", "avi", "mkv", "webm" -> EvidenceType.VIDEO
                else -> {
                    if (fileName.contains("whatsapp", ignoreCase = true)) {
                        EvidenceType.WHATSAPP
                    } else {
                        EvidenceType.TEXT
                    }
                }
            }
        }

        /**
         * Determine MIME type from evidence type.
         */
        fun getMimeType(evidenceType: EvidenceType, fileName: String): String {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return when (evidenceType) {
                EvidenceType.PDF -> "application/pdf"
                EvidenceType.IMAGE -> when (extension) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    "webp" -> "image/webp"
                    else -> "image/*"
                }
                EvidenceType.AUDIO -> when (extension) {
                    "mp3" -> "audio/mpeg"
                    "wav" -> "audio/wav"
                    "m4a" -> "audio/mp4"
                    else -> "audio/*"
                }
                EvidenceType.VIDEO -> when (extension) {
                    "mp4" -> "video/mp4"
                    "mov" -> "video/quicktime"
                    else -> "video/*"
                }
                else -> "text/plain"
            }
        }
    }
}

/**
 * Content classification types.
 */
enum class ContentClassification {
    LEGAL,
    FINANCIAL,
    COMMUNICATION,
    GENERAL
}

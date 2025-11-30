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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Evidence Ingestion Layer
 * Handles extraction of text and metadata from various evidence types.
 */
class EvidenceProcessor(private val context: Context) {

    init {
        // Initialize PDFBox for Android
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Process a piece of evidence and extract text and metadata.
     */
    suspend fun processEvidence(evidence: Evidence, uri: Uri): Evidence {
        return withContext(Dispatchers.IO) {
            when (evidence.type) {
                EvidenceType.PDF -> processPdf(evidence, uri)
                EvidenceType.IMAGE -> processImage(evidence, uri)
                EvidenceType.TEXT -> processText(evidence, uri)
                EvidenceType.EMAIL -> processEmail(evidence, uri)
                EvidenceType.WHATSAPP -> processWhatsApp(evidence, uri)
                EvidenceType.UNKNOWN -> evidence.copy(processed = true)
            }
        }
    }

    /**
     * Extract text from PDF using PDFBox.
     */
    private fun processPdf(evidence: Evidence, uri: Uri): Evidence {
        var extractedText = ""
        var metadata = evidence.metadata

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    // Extract text
                    val stripper = PDFTextStripper()
                    extractedText = stripper.getText(document)

                    // Extract metadata
                    val info = document.documentInformation
                    metadata = EvidenceMetadata(
                        creationDate = info.creationDate?.time?.let { Date(it.time) },
                        modificationDate = info.modificationDate?.time?.let { Date(it.time) },
                        author = info.author,
                        subject = info.subject
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return evidence.copy(
            extractedText = extractedText,
            metadata = metadata,
            processed = true,
            origin = uri.toString(),
            contentHash = calculateHash(extractedText)
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
                processed = true,
                origin = uri.toString(),
                contentHash = calculateHash(result)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            evidence.copy(processed = true, origin = uri.toString())
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
            processed = true,
            origin = uri.toString(),
            contentHash = calculateHash(text)
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

                metadata = EvidenceMetadata(
                    sender = fromMatch?.groupValues?.getOrNull(1)?.trim(),
                    receiver = toMatch?.groupValues?.getOrNull(1)?.trim(),
                    subject = subjectMatch?.groupValues?.getOrNull(1)?.trim(),
                    creationDate = dateMatch?.groupValues?.getOrNull(1)?.let { parseDate(it) }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return evidence.copy(
            extractedText = text,
            metadata = metadata,
            processed = true,
            origin = uri.toString(),
            contentHash = calculateHash(text)
        )
    }

    /**
     * Process WhatsApp export.
     */
    private fun processWhatsApp(evidence: Evidence, uri: Uri): Evidence {
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
            processed = true,
            origin = uri.toString(),
            contentHash = calculateHash(text)
        )
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
            "MM/dd/yyyy HH:mm"
        )
        
        for (format in formats) {
            try {
                return SimpleDateFormat(format, Locale.US).parse(dateStr.trim())
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Calculate SHA-256 hash for content integrity verification.
     */
    private fun calculateHash(content: String): String {
        if (content.isBlank()) return ""
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
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
                else -> {
                    // Check for WhatsApp export pattern
                    if (fileName.contains("whatsapp", ignoreCase = true)) {
                        EvidenceType.WHATSAPP
                    } else {
                        EvidenceType.TEXT
                    }
                }
            }
        }
    }
}

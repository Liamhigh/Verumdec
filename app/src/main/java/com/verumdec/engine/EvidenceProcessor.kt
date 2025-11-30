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
            processed = true
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
            processed = true
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

    companion object {
        // Supported file extension mappings
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "bmp", "gif", "webp", "heic", "heif")
        private val PDF_EXTENSIONS = setOf("pdf")
        private val TEXT_EXTENSIONS = setOf("txt", "rtf", "md", "csv")
        private val EMAIL_EXTENSIONS = setOf("eml", "msg")
        private val DOCUMENT_EXTENSIONS = setOf("doc", "docx", "odt")
        private val VIDEO_EXTENSIONS = setOf("mp4", "mov", "avi", "mkv", "webm", "m4v")
        private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "m4a", "aac", "ogg", "flac")
        
        /**
         * Determine evidence type from file extension.
         * Enhanced to support more file types.
         */
        fun getEvidenceType(fileName: String): EvidenceType {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            val lowerName = fileName.lowercase()
            
            return when {
                // Check for WhatsApp export pattern first (can have various extensions)
                lowerName.contains("whatsapp") || lowerName.contains("wa_chat") -> EvidenceType.WHATSAPP
                
                // Standard type detection
                extension in PDF_EXTENSIONS -> EvidenceType.PDF
                extension in IMAGE_EXTENSIONS -> EvidenceType.IMAGE
                extension in EMAIL_EXTENSIONS -> EvidenceType.EMAIL
                extension in TEXT_EXTENSIONS -> EvidenceType.TEXT
                extension in DOCUMENT_EXTENSIONS -> EvidenceType.TEXT // Will be processed as text
                extension in VIDEO_EXTENSIONS -> EvidenceType.UNKNOWN // Video metadata only
                extension in AUDIO_EXTENSIONS -> EvidenceType.UNKNOWN // Audio metadata only
                
                // Default to text if unknown
                else -> EvidenceType.TEXT
            }
        }
        
        /**
         * Check if a file is a supported evidence type.
         */
        fun isSupportedFile(fileName: String): Boolean {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return extension in getAllSupportedExtensions()
        }
        
        /**
         * Get all supported file extensions.
         */
        fun getAllSupportedExtensions(): Set<String> {
            return IMAGE_EXTENSIONS + PDF_EXTENSIONS + TEXT_EXTENSIONS + 
                   EMAIL_EXTENSIONS + DOCUMENT_EXTENSIONS + VIDEO_EXTENSIONS + AUDIO_EXTENSIONS
        }
        
        /**
         * Get a human-readable description of the file type.
         */
        fun getFileTypeDescription(fileName: String): String {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return when {
                fileName.lowercase().contains("whatsapp") -> "WhatsApp Export"
                extension in PDF_EXTENSIONS -> "PDF Document"
                extension in IMAGE_EXTENSIONS -> "Image/Screenshot"
                extension in EMAIL_EXTENSIONS -> "Email Export"
                extension in TEXT_EXTENSIONS -> "Text File"
                extension in DOCUMENT_EXTENSIONS -> "Document"
                extension in VIDEO_EXTENSIONS -> "Video File"
                extension in AUDIO_EXTENSIONS -> "Audio File"
                else -> "Unknown File"
            }
        }
    }
}

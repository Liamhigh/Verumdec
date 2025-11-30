package com.verumdec.core.processor

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DocumentBrain - PDF/DOC/image ingestion, metadata extraction, hashing
 *
 * Processes document files to extract text content, metadata, and compute
 * cryptographic hashes for evidence integrity verification.
 *
 * Operates fully offline without external dependencies.
 */
class DocumentBrain {

    companion object {
        private const val HASH_ALGORITHM = "SHA-512"
        private val SUPPORTED_EXTENSIONS = setOf(
            "pdf", "doc", "docx", "txt", "rtf", "odt",
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp"
        )
    }

    /**
     * Process a document file and extract all relevant information.
     *
     * @param file The document file to process
     * @return DocumentBrainResult containing extracted data or error
     */
    fun process(file: File): DocumentBrainResult {
        return try {
            if (!file.exists()) {
                return DocumentBrainResult.Failure(
                    error = "File not found: ${file.absolutePath}",
                    errorCode = DocumentErrorCode.FILE_NOT_FOUND
                )
            }

            val extension = file.extension.lowercase()
            if (extension !in SUPPORTED_EXTENSIONS) {
                return DocumentBrainResult.Failure(
                    error = "Unsupported file format: $extension",
                    errorCode = DocumentErrorCode.UNSUPPORTED_FORMAT
                )
            }

            val fileHash = computeFileHash(file)
                ?: return DocumentBrainResult.Failure(
                    error = "Failed to compute file hash",
                    errorCode = DocumentErrorCode.HASH_COMPUTATION_FAILED
                )

            val metadata = extractMetadata(file, extension)
            val extractedText = extractText(file, extension)
            val pageCount = estimatePageCount(file, extension, extractedText)
            val mimeType = getMimeType(extension)
            val warnings = mutableListOf<String>()

            // Check for potential issues
            if (extractedText.isEmpty()) {
                warnings.add("No text could be extracted from document")
            }
            if (metadata.creationDate == null) {
                warnings.add("Creation date could not be determined")
            }
            if (metadata.author == null) {
                warnings.add("Document author is not specified")
            }

            DocumentBrainResult.Success(
                fileHash = fileHash,
                metadata = metadata,
                extractedText = extractedText,
                pageCount = pageCount,
                fileSize = file.length(),
                mimeType = mimeType,
                warnings = warnings
            )
        } catch (e: Exception) {
            DocumentBrainResult.Failure(
                error = "Processing error: ${e.message}",
                errorCode = DocumentErrorCode.PROCESSING_ERROR
            )
        }
    }

    /**
     * Process a document from an input stream.
     *
     * @param inputStream The input stream to read from
     * @param fileName The original filename for extension detection
     * @param fileSize The size of the file in bytes
     * @return DocumentBrainResult containing extracted data or error
     */
    fun process(inputStream: InputStream, fileName: String, fileSize: Long): DocumentBrainResult {
        return try {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            if (extension !in SUPPORTED_EXTENSIONS) {
                return DocumentBrainResult.Failure(
                    error = "Unsupported file format: $extension",
                    errorCode = DocumentErrorCode.UNSUPPORTED_FORMAT
                )
            }

            val bytes = inputStream.readBytes()
            val fileHash = computeHash(bytes)
                ?: return DocumentBrainResult.Failure(
                    error = "Failed to compute file hash",
                    errorCode = DocumentErrorCode.HASH_COMPUTATION_FAILED
                )

            val extractedText = extractTextFromBytes(bytes, extension)
            val metadata = extractMetadataFromBytes(bytes, extension)
            val pageCount = estimatePageCountFromText(extractedText)
            val mimeType = getMimeType(extension)
            val warnings = mutableListOf<String>()

            if (extractedText.isEmpty()) {
                warnings.add("No text could be extracted from document")
            }

            DocumentBrainResult.Success(
                fileHash = fileHash,
                metadata = metadata,
                extractedText = extractedText,
                pageCount = pageCount,
                fileSize = fileSize,
                mimeType = mimeType,
                warnings = warnings
            )
        } catch (e: Exception) {
            DocumentBrainResult.Failure(
                error = "Processing error: ${e.message}",
                errorCode = DocumentErrorCode.PROCESSING_ERROR
            )
        }
    }

    /**
     * Compute SHA-512 hash of a file.
     */
    private fun computeFileHash(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Compute SHA-512 hash of bytes.
     */
    private fun computeHash(bytes: ByteArray): String? {
        return try {
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            digest.update(bytes)
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract metadata from a document file.
     */
    private fun extractMetadata(file: File, extension: String): DocumentMetadata {
        val lastModified = Date(file.lastModified())
        val customProps = mutableMapOf<String, String>()

        customProps["fileName"] = file.name
        customProps["absolutePath"] = file.absolutePath
        customProps["extension"] = extension

        return when (extension) {
            "pdf" -> extractPdfMetadataFromFile(file, lastModified, customProps)
            "doc", "docx", "odt" -> extractOfficeMetadataFromFile(file, lastModified, customProps)
            "txt", "rtf" -> DocumentMetadata(
                modificationDate = lastModified,
                customProperties = customProps
            )
            else -> extractImageMetadataFromFile(file, lastModified, customProps)
        }
    }

    /**
     * Extract metadata from PDF file (basic implementation).
     */
    private fun extractPdfMetadataFromFile(
        file: File,
        lastModified: Date,
        customProps: MutableMap<String, String>
    ): DocumentMetadata {
        // Basic PDF metadata extraction by reading header bytes
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(1024)
                input.read(header)
                val headerStr = String(header, Charsets.ISO_8859_1)

                // Check PDF signature
                if (!headerStr.startsWith("%PDF-")) {
                    customProps["pdfVersion"] = "unknown"
                } else {
                    val version = headerStr.substring(5, 8)
                    customProps["pdfVersion"] = version
                }
            }

            DocumentMetadata(
                modificationDate = lastModified,
                producer = "PDF Document",
                customProperties = customProps
            )
        } catch (e: Exception) {
            DocumentMetadata(
                modificationDate = lastModified,
                customProperties = customProps
            )
        }
    }

    /**
     * Extract metadata from Office documents.
     */
    private fun extractOfficeMetadataFromFile(
        file: File,
        lastModified: Date,
        customProps: MutableMap<String, String>
    ): DocumentMetadata {
        return DocumentMetadata(
            modificationDate = lastModified,
            producer = "Office Document",
            customProperties = customProps
        )
    }

    /**
     * Extract metadata from image files.
     */
    private fun extractImageMetadataFromFile(
        file: File,
        lastModified: Date,
        customProps: MutableMap<String, String>
    ): DocumentMetadata {
        // Basic EXIF extraction for images
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(12)
                input.read(header)

                // Detect image type from magic bytes
                val imageType = when {
                    header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() -> "JPEG"
                    header[0] == 0x89.toByte() && header[1] == 0x50.toByte() -> "PNG"
                    header[0] == 0x47.toByte() && header[1] == 0x49.toByte() -> "GIF"
                    else -> "Unknown"
                }
                customProps["detectedImageType"] = imageType
            }

            DocumentMetadata(
                modificationDate = lastModified,
                producer = "Image Document",
                customProperties = customProps
            )
        } catch (e: Exception) {
            DocumentMetadata(
                modificationDate = lastModified,
                customProperties = customProps
            )
        }
    }

    /**
     * Extract metadata from byte array.
     */
    private fun extractMetadataFromBytes(bytes: ByteArray, extension: String): DocumentMetadata {
        val customProps = mutableMapOf<String, String>()
        customProps["extension"] = extension
        customProps["byteSize"] = bytes.size.toString()

        return DocumentMetadata(
            customProperties = customProps
        )
    }

    /**
     * Extract text from a document file.
     */
    private fun extractText(file: File, extension: String): String {
        return when (extension) {
            "txt" -> file.readText()
            "rtf" -> extractRtfText(file.readText())
            "pdf" -> extractPdfText(file)
            else -> ""
        }
    }

    /**
     * Extract text from bytes.
     */
    private fun extractTextFromBytes(bytes: ByteArray, extension: String): String {
        return when (extension) {
            "txt" -> String(bytes, Charsets.UTF_8)
            "rtf" -> extractRtfText(String(bytes, Charsets.UTF_8))
            else -> ""
        }
    }

    /**
     * Extract text from RTF content (basic implementation).
     */
    private fun extractRtfText(rtfContent: String): String {
        // Basic RTF text extraction - removes RTF control words
        return rtfContent
            .replace(Regex("\\{[^}]*\\}"), "")
            .replace(Regex("\\\\[a-z]+\\d*\\s?"), "")
            .replace(Regex("[\\r\\n]+"), "\n")
            .trim()
    }

    /**
     * Extract text from PDF (basic implementation).
     */
    private fun extractPdfText(file: File): String {
        // Basic PDF text extraction - looks for text strings
        return try {
            val content = file.readBytes()
            val text = StringBuilder()

            // Find text between parentheses in PDF content streams
            val contentStr = String(content, Charsets.ISO_8859_1)
            val textPattern = Regex("\\(([^)]+)\\)")
            textPattern.findAll(contentStr).forEach { match ->
                val extracted = match.groupValues[1]
                // Filter out control sequences
                if (extracted.length > 2 && !extracted.startsWith("\\")) {
                    text.append(extracted).append(" ")
                }
            }

            text.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Estimate page count for a document.
     */
    private fun estimatePageCount(file: File, extension: String, text: String): Int {
        return when (extension) {
            "pdf" -> estimatePdfPageCount(file)
            "txt", "rtf" -> estimatePageCountFromText(text)
            else -> 1
        }
    }

    /**
     * Estimate page count from PDF file.
     */
    private fun estimatePdfPageCount(file: File): Int {
        return try {
            val content = String(file.readBytes(), Charsets.ISO_8859_1)
            // Count /Page objects in PDF
            val pagePattern = Regex("/Type\\s*/Page[^s]")
            pagePattern.findAll(content).count().coerceAtLeast(1)
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Estimate page count from text content.
     */
    private fun estimatePageCountFromText(text: String): Int {
        // Estimate ~3000 characters per page
        return ((text.length / 3000) + 1).coerceAtLeast(1)
    }

    /**
     * Get MIME type for a file extension.
     */
    private fun getMimeType(extension: String): String {
        return when (extension) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "txt" -> "text/plain"
            "rtf" -> "application/rtf"
            "odt" -> "application/vnd.oasis.opendocument.text"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "tiff" -> "image/tiff"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    /**
     * Convert result to JSON string.
     */
    fun toJson(result: DocumentBrainResult): String {
        return when (result) {
            is DocumentBrainResult.Success -> buildSuccessJson(result)
            is DocumentBrainResult.Failure -> buildFailureJson(result)
        }
    }

    private fun buildSuccessJson(result: DocumentBrainResult.Success): String {
        val warningsJson = result.warnings.joinToString(",") { "\"$it\"" }
        val keywordsJson = result.metadata.keywords.joinToString(",") { "\"$it\"" }
        val customPropsJson = result.metadata.customProperties.entries
            .joinToString(",") { "\"${it.key}\":\"${escapeJson(it.value)}\"" }

        return """
        {
            "success": true,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "documentId": "${result.documentId}",
            "fileHash": "${result.fileHash}",
            "metadata": {
                "creationDate": ${result.metadata.creationDate?.time ?: "null"},
                "modificationDate": ${result.metadata.modificationDate?.time ?: "null"},
                "author": ${result.metadata.author?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "title": ${result.metadata.title?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "subject": ${result.metadata.subject?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "producer": ${result.metadata.producer?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "creator": ${result.metadata.creator?.let { "\"${escapeJson(it)}\"" } ?: "null"},
                "keywords": [$keywordsJson],
                "customProperties": {$customPropsJson}
            },
            "extractedTextLength": ${result.extractedText.length},
            "pageCount": ${result.pageCount},
            "fileSize": ${result.fileSize},
            "mimeType": "${result.mimeType}",
            "warnings": [$warningsJson]
        }
        """.trimIndent()
    }

    private fun buildFailureJson(result: DocumentBrainResult.Failure): String {
        return """
        {
            "success": false,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "error": "${escapeJson(result.error)}",
            "errorCode": "${result.errorCode}"
        }
        """.trimIndent()
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

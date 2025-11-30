package com.verumdec.core

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * Utility functions for file operations.
 * All operations are designed for offline, on-device use.
 */
object FileUtils {
    
    /**
     * Supported evidence file extensions
     */
    val SUPPORTED_EXTENSIONS = setOf(
        // Documents
        "pdf", "txt", "doc", "docx", "rtf",
        // Images
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic",
        // Email exports
        "eml", "msg",
        // Other
        "csv", "json", "xml"
    )
    
    /**
     * Get the file extension from a filename.
     */
    fun getExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }
    
    /**
     * Check if a file is a supported evidence type.
     */
    fun isSupportedFile(fileName: String): Boolean {
        return getExtension(fileName) in SUPPORTED_EXTENSIONS
    }
    
    /**
     * Check if a file is an image.
     */
    fun isImageFile(fileName: String): Boolean {
        val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic")
        return getExtension(fileName) in imageExtensions
    }
    
    /**
     * Check if a file is a PDF.
     */
    fun isPdfFile(fileName: String): Boolean {
        return getExtension(fileName) == "pdf"
    }
    
    /**
     * Check if a file is an email export.
     */
    fun isEmailFile(fileName: String): Boolean {
        val emailExtensions = setOf("eml", "msg")
        return getExtension(fileName) in emailExtensions
    }
    
    /**
     * Check if a file is a WhatsApp export (based on naming convention).
     */
    fun isWhatsAppExport(fileName: String): Boolean {
        return fileName.lowercase().contains("whatsapp") ||
               fileName.lowercase().contains("wa_chat")
    }
    
    /**
     * Check if a file is a text file.
     */
    fun isTextFile(fileName: String): Boolean {
        val textExtensions = setOf("txt", "csv", "json", "xml", "rtf")
        return getExtension(fileName) in textExtensions
    }
    
    /**
     * Get a safe filename (removes special characters).
     */
    fun getSafeFileName(fileName: String): String {
        return fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
    
    /**
     * Get file size in human-readable format.
     */
    fun getReadableFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
            sizeInBytes < 1024 * 1024 * 1024 -> "${sizeInBytes / (1024 * 1024)} MB"
            else -> "${sizeInBytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * Generate a unique filename with timestamp.
     */
    fun generateUniqueFileName(baseName: String, extension: String): String {
        val timestamp = System.currentTimeMillis()
        val safeName = getSafeFileName(baseName)
        return "${safeName}_$timestamp.$extension"
    }
    
    /**
     * Read text content from an input stream.
     */
    fun readTextFromStream(inputStream: InputStream): String {
        return inputStream.bufferedReader().use { it.readText() }
    }
    
    /**
     * Ensure a directory exists, creating it if necessary.
     */
    fun ensureDirectoryExists(directory: File): Boolean {
        return if (directory.exists()) {
            directory.isDirectory
        } else {
            directory.mkdirs()
        }
    }
    
    /**
     * Get MIME type from file extension.
     */
    fun getMimeType(fileName: String): String {
        return when (getExtension(fileName)) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "eml" -> "message/rfc822"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
    }
}

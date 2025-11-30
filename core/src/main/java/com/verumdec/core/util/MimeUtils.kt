package com.verumdec.core.util

import android.webkit.MimeTypeMap

/**
 * Utility class for MIME type detection and file type classification.
 */
object MimeUtils {

    // Common MIME types
    const val MIME_PDF = "application/pdf"
    const val MIME_IMAGE_JPEG = "image/jpeg"
    const val MIME_IMAGE_PNG = "image/png"
    const val MIME_IMAGE_GIF = "image/gif"
    const val MIME_IMAGE_WEBP = "image/webp"
    const val MIME_IMAGE_BMP = "image/bmp"
    const val MIME_TEXT_PLAIN = "text/plain"
    const val MIME_TEXT_HTML = "text/html"
    const val MIME_VIDEO_MP4 = "video/mp4"
    const val MIME_VIDEO_WEBM = "video/webm"
    const val MIME_VIDEO_MKV = "video/x-matroska"
    const val MIME_VIDEO_AVI = "video/x-msvideo"
    const val MIME_VIDEO_MOV = "video/quicktime"
    const val MIME_AUDIO_MP3 = "audio/mpeg"
    const val MIME_AUDIO_WAV = "audio/wav"
    const val MIME_AUDIO_OGG = "audio/ogg"
    const val MIME_AUDIO_M4A = "audio/mp4"
    const val MIME_EMAIL = "message/rfc822"
    const val MIME_JSON = "application/json"
    const val MIME_OCTET_STREAM = "application/octet-stream"

    /**
     * Get MIME type from file extension.
     */
    fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: guessMimeType(extension)
    }

    /**
     * Guess MIME type from extension.
     */
    private fun guessMimeType(extension: String): String {
        return when (extension) {
            "pdf" -> MIME_PDF
            "jpg", "jpeg" -> MIME_IMAGE_JPEG
            "png" -> MIME_IMAGE_PNG
            "gif" -> MIME_IMAGE_GIF
            "webp" -> MIME_IMAGE_WEBP
            "bmp" -> MIME_IMAGE_BMP
            "txt" -> MIME_TEXT_PLAIN
            "html", "htm" -> MIME_TEXT_HTML
            "mp4", "m4v" -> MIME_VIDEO_MP4
            "webm" -> MIME_VIDEO_WEBM
            "mkv" -> MIME_VIDEO_MKV
            "avi" -> MIME_VIDEO_AVI
            "mov" -> MIME_VIDEO_MOV
            "mp3" -> MIME_AUDIO_MP3
            "wav" -> MIME_AUDIO_WAV
            "ogg" -> MIME_AUDIO_OGG
            "m4a" -> MIME_AUDIO_M4A
            "eml", "msg" -> MIME_EMAIL
            "json" -> MIME_JSON
            else -> MIME_OCTET_STREAM
        }
    }

    /**
     * Check if file is an image.
     */
    fun isImage(fileName: String): Boolean {
        val mimeType = getMimeType(fileName)
        return mimeType.startsWith("image/")
    }

    /**
     * Check if file is a video.
     */
    fun isVideo(fileName: String): Boolean {
        val mimeType = getMimeType(fileName)
        return mimeType.startsWith("video/")
    }

    /**
     * Check if file is audio.
     */
    fun isAudio(fileName: String): Boolean {
        val mimeType = getMimeType(fileName)
        return mimeType.startsWith("audio/")
    }

    /**
     * Check if file is a PDF.
     */
    fun isPdf(fileName: String): Boolean {
        return getMimeType(fileName) == MIME_PDF
    }

    /**
     * Check if file is text-based.
     */
    fun isText(fileName: String): Boolean {
        val mimeType = getMimeType(fileName)
        return mimeType.startsWith("text/") || mimeType == MIME_JSON
    }

    /**
     * Check if file is an email.
     */
    fun isEmail(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in listOf("eml", "msg") || getMimeType(fileName) == MIME_EMAIL
    }

    /**
     * Check if content is likely WhatsApp export.
     */
    fun isWhatsAppExport(fileName: String, content: String? = null): Boolean {
        if (fileName.contains("whatsapp", ignoreCase = true)) return true
        if (fileName.contains("chat", ignoreCase = true) && fileName.endsWith(".txt")) return true
        // Check content pattern for WhatsApp format
        content?.let {
            val whatsAppPattern = Regex("\\[\\d{1,2}/\\d{1,2}/\\d{2,4},\\s*\\d{1,2}:\\d{2}")
            if (whatsAppPattern.containsMatchIn(it.take(500))) return true
        }
        return false
    }

    /**
     * Get file type category.
     */
    fun getFileCategory(fileName: String): FileCategory {
        return when {
            isPdf(fileName) -> FileCategory.PDF
            isImage(fileName) -> FileCategory.IMAGE
            isVideo(fileName) -> FileCategory.VIDEO
            isAudio(fileName) -> FileCategory.AUDIO
            isEmail(fileName) -> FileCategory.EMAIL
            isText(fileName) -> FileCategory.TEXT
            else -> FileCategory.UNKNOWN
        }
    }

    /**
     * File type categories.
     */
    enum class FileCategory {
        PDF,
        IMAGE,
        VIDEO,
        AUDIO,
        EMAIL,
        TEXT,
        UNKNOWN
    }
}

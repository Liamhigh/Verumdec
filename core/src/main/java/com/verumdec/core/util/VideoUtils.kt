package com.verumdec.core.util

/**
 * Utility class for video file analysis.
 * Provides metadata extraction capabilities for video evidence.
 */
object VideoUtils {

    /**
     * Supported video extensions.
     */
    val SUPPORTED_EXTENSIONS = listOf("mp4", "mov", "avi", "mkv", "webm", "m4v", "3gp", "wmv")

    /**
     * Check if file is a supported video format.
     */
    fun isVideoFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in SUPPORTED_EXTENSIONS
    }

    /**
     * Video metadata container.
     */
    data class VideoMetadata(
        val fileName: String,
        val durationMs: Long = 0,
        val width: Int = 0,
        val height: Int = 0,
        val frameRate: Float = 0f,
        val bitRate: Long = 0,
        val codec: String = "",
        val hasAudio: Boolean = false,
        val creationDate: String? = null,
        val location: String? = null
    )

    /**
     * Format duration in milliseconds to readable string.
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Format resolution.
     */
    fun formatResolution(width: Int, height: Int): String {
        return "${width}x${height}"
    }

    /**
     * Get resolution label.
     */
    fun getResolutionLabel(height: Int): String {
        return when {
            height >= 2160 -> "4K UHD"
            height >= 1440 -> "2K QHD"
            height >= 1080 -> "Full HD"
            height >= 720 -> "HD"
            height >= 480 -> "SD"
            else -> "Low"
        }
    }

    /**
     * Estimate file size category.
     */
    fun getFileSizeCategory(sizeBytes: Long): String {
        return when {
            sizeBytes > 1_000_000_000 -> "Very Large (>1GB)"
            sizeBytes > 100_000_000 -> "Large (100MB-1GB)"
            sizeBytes > 10_000_000 -> "Medium (10-100MB)"
            sizeBytes > 1_000_000 -> "Small (1-10MB)"
            else -> "Very Small (<1MB)"
        }
    }

    /**
     * Calculate approximate duration from file size and average bitrate.
     */
    fun estimateDuration(fileSizeBytes: Long, avgBitrateBps: Long = 5_000_000): Long {
        if (avgBitrateBps <= 0) return 0
        return (fileSizeBytes * 8 * 1000) / avgBitrateBps
    }
}

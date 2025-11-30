package com.verumdec.core.util

/**
 * Utility class for audio file analysis.
 * Provides metadata extraction capabilities for audio evidence.
 */
object AudioUtils {

    /**
     * Supported audio extensions.
     */
    val SUPPORTED_EXTENSIONS = listOf("mp3", "wav", "ogg", "m4a", "aac", "flac", "wma", "opus", "amr")

    /**
     * Check if file is a supported audio format.
     */
    fun isAudioFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in SUPPORTED_EXTENSIONS
    }

    /**
     * Audio metadata container.
     */
    data class AudioMetadata(
        val fileName: String,
        val durationMs: Long = 0,
        val sampleRate: Int = 0,
        val channels: Int = 0,
        val bitRate: Long = 0,
        val codec: String = "",
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val creationDate: String? = null
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
     * Get audio quality label based on bit rate.
     */
    fun getQualityLabel(bitRate: Long): String {
        return when {
            bitRate >= 320000 -> "High Quality (320+ kbps)"
            bitRate >= 256000 -> "Good Quality (256 kbps)"
            bitRate >= 192000 -> "Standard Quality (192 kbps)"
            bitRate >= 128000 -> "Medium Quality (128 kbps)"
            bitRate >= 64000 -> "Low Quality (64 kbps)"
            else -> "Very Low Quality"
        }
    }

    /**
     * Get channel configuration label.
     */
    fun getChannelLabel(channels: Int): String {
        return when (channels) {
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "5.1 Surround"
            8 -> "7.1 Surround"
            else -> "$channels Channels"
        }
    }

    /**
     * Estimate duration from file size.
     */
    fun estimateDuration(fileSizeBytes: Long, bitRateBps: Long = 128000): Long {
        if (bitRateBps <= 0) return 0
        return (fileSizeBytes * 8 * 1000) / bitRateBps
    }

    /**
     * Check if audio is voice recording (heuristic).
     */
    fun isLikelyVoiceRecording(metadata: AudioMetadata): Boolean {
        // Voice recordings typically have lower sample rates and are mono
        return metadata.channels == 1 || 
               metadata.sampleRate <= 22050 ||
               metadata.fileName.contains("voice", ignoreCase = true) ||
               metadata.fileName.contains("recording", ignoreCase = true) ||
               metadata.fileName.contains("memo", ignoreCase = true)
    }
}

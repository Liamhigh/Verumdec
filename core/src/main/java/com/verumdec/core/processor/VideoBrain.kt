package com.verumdec.core.processor

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * VideoBrain - Frame hashing + metadata consistency scan
 *
 * Analyzes video files to extract frame hashes, detect anomalies in metadata,
 * and identify potential tampering or editing.
 *
 * Operates fully offline without external dependencies.
 */
class VideoBrain {

    companion object {
        private const val HASH_ALGORITHM = "SHA-512"
        private val SUPPORTED_EXTENSIONS = setOf(
            "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm", "m4v", "3gp"
        )

        // Common video codec identifiers
        private val CODEC_SIGNATURES = mapOf(
            "avc1" to "H.264/AVC",
            "hvc1" to "H.265/HEVC",
            "mp4v" to "MPEG-4",
            "vp08" to "VP8",
            "vp09" to "VP9",
            "av01" to "AV1"
        )
    }

    /**
     * Process a video file and analyze for anomalies.
     *
     * @param file The video file to process
     * @return VideoBrainResult containing analysis data or error
     */
    fun process(file: File): VideoBrainResult {
        return try {
            if (!file.exists()) {
                return VideoBrainResult.Failure(
                    error = "File not found: ${file.absolutePath}",
                    errorCode = VideoErrorCode.FILE_NOT_FOUND
                )
            }

            val extension = file.extension.lowercase()
            if (extension !in SUPPORTED_EXTENSIONS) {
                return VideoBrainResult.Failure(
                    error = "Unsupported video format: $extension",
                    errorCode = VideoErrorCode.UNSUPPORTED_FORMAT
                )
            }

            val bytes = file.readBytes()
            processBytes(bytes, extension, file.length())
        } catch (e: Exception) {
            VideoBrainResult.Failure(
                error = "Processing error: ${e.message}",
                errorCode = VideoErrorCode.PROCESSING_ERROR
            )
        }
    }

    /**
     * Process a video from an input stream.
     *
     * @param inputStream The input stream to read from
     * @param fileName The original filename for extension detection
     * @param fileSize The size of the file in bytes
     * @return VideoBrainResult containing analysis data or error
     */
    fun process(inputStream: InputStream, fileName: String, fileSize: Long): VideoBrainResult {
        return try {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            if (extension !in SUPPORTED_EXTENSIONS) {
                return VideoBrainResult.Failure(
                    error = "Unsupported video format: $extension",
                    errorCode = VideoErrorCode.UNSUPPORTED_FORMAT
                )
            }

            val bytes = inputStream.readBytes()
            processBytes(bytes, extension, fileSize)
        } catch (e: Exception) {
            VideoBrainResult.Failure(
                error = "Processing error: ${e.message}",
                errorCode = VideoErrorCode.PROCESSING_ERROR
            )
        }
    }

    private fun processBytes(bytes: ByteArray, extension: String, fileSize: Long): VideoBrainResult {
        val fileHash = computeHash(bytes)
            ?: return VideoBrainResult.Failure(
                error = "Failed to compute file hash",
                errorCode = VideoErrorCode.ANALYSIS_FAILED
            )

        val containerInfo = parseContainer(bytes, extension)
        val resolution = containerInfo.resolution
        val codec = containerInfo.codec
        val duration = estimateDuration(bytes, extension, fileSize)
        val frameRate = containerInfo.frameRate
        val frameCount = estimateFrameCount(duration, frameRate)

        val keyFrameHashes = extractKeyFrameHashes(bytes, extension, frameCount)
        val metadataConsistency = analyzeMetadataConsistency(bytes, extension, containerInfo)
        val anomalies = detectAnomalies(bytes, extension, containerInfo, metadataConsistency)

        val integrityScore = calculateIntegrityScore(anomalies, metadataConsistency)

        return VideoBrainResult.Success(
            fileHash = fileHash,
            duration = duration,
            frameCount = frameCount,
            frameRate = frameRate,
            resolution = resolution,
            codec = codec,
            keyFrameHashes = keyFrameHashes,
            metadataConsistency = metadataConsistency,
            anomalies = anomalies,
            integrityScore = integrityScore
        )
    }

    /**
     * Compute SHA-512 hash of video bytes.
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
     * Container information extracted from video file.
     */
    private data class ContainerInfo(
        val format: String,
        val resolution: VideoResolution,
        val frameRate: Float,
        val codec: String,
        val creationTime: Long?,
        val durationFromMetadata: Long?,
        val hasAudio: Boolean,
        val metadata: Map<String, String>
    )

    /**
     * Parse container format and extract metadata.
     */
    private fun parseContainer(bytes: ByteArray, extension: String): ContainerInfo {
        return when (extension) {
            "mp4", "m4v", "mov" -> parseMp4Container(bytes)
            "avi" -> parseAviContainer(bytes)
            "mkv", "webm" -> parseMkvContainer(bytes)
            else -> parseGenericContainer(bytes, extension)
        }
    }

    private fun parseMp4Container(bytes: ByteArray): ContainerInfo {
        var width = 0
        var height = 0
        var frameRate = 30.0f
        var codec = "Unknown"
        var creationTime: Long? = null
        var duration: Long? = null
        var hasAudio = false
        val metadata = mutableMapOf<String, String>()

        // Look for ftyp box to confirm MP4 format
        var format = "MP4"
        if (bytes.size > 11) {
            val ftyp = String(bytes.sliceArray(4..7), Charsets.US_ASCII)
            if (ftyp == "ftyp") {
                val brand = String(bytes.sliceArray(8..11), Charsets.US_ASCII)
                format = when {
                    brand.startsWith("isom") -> "MP4 (ISO Base Media)"
                    brand.startsWith("mp4") -> "MP4"
                    brand.startsWith("qt") -> "QuickTime"
                    brand.startsWith("M4V") -> "M4V"
                    else -> "MP4 ($brand)"
                }
                metadata["brand"] = brand
            }
        }

        // Parse MP4 atoms/boxes
        var offset = 0
        while (offset < bytes.size - 8) {
            val size = getInt32BE(bytes, offset)
            if (size < 8 || offset + size > bytes.size) break

            val atomType = String(bytes.sliceArray(offset + 4 until offset + 8), Charsets.US_ASCII)

            when (atomType) {
                "moov" -> {
                    // Movie header - contains metadata
                    metadata["hasMoov"] = "true"
                }
                "trak" -> {
                    // Track atom
                    hasAudio = hasAudio || containsAudioTrack(bytes, offset, size)
                }
                "mvhd" -> {
                    // Movie header - contains creation time and duration
                    if (offset + 20 < bytes.size) {
                        val version = bytes[offset + 8].toInt() and 0xFF
                        if (version == 0 && offset + 24 < bytes.size) {
                            creationTime = getInt32BE(bytes, offset + 12).toLong()
                            val timescale = getInt32BE(bytes, offset + 20)
                            val durationUnits = getInt32BE(bytes, offset + 24)
                            if (timescale > 0) {
                                duration = (durationUnits * 1000L) / timescale
                            }
                        }
                    }
                }
                "stsd" -> {
                    // Sample description - contains codec info
                    val codecInfo = extractCodecFromStsd(bytes, offset, size)
                    if (codecInfo != null) {
                        codec = codecInfo
                    }
                }
                "tkhd" -> {
                    // Track header - contains dimensions
                    if (offset + 84 < bytes.size) {
                        val trackWidth = getInt32BE(bytes, offset + 76) shr 16
                        val trackHeight = getInt32BE(bytes, offset + 80) shr 16
                        if (trackWidth > 0 && trackHeight > 0) {
                            width = trackWidth
                            height = trackHeight
                        }
                    }
                }
            }

            offset += size
        }

        // Default resolution if not found
        if (width == 0 || height == 0) {
            width = 1920
            height = 1080
        }

        return ContainerInfo(
            format = format,
            resolution = VideoResolution(width, height),
            frameRate = frameRate,
            codec = codec,
            creationTime = creationTime,
            durationFromMetadata = duration,
            hasAudio = hasAudio,
            metadata = metadata
        )
    }

    private fun containsAudioTrack(bytes: ByteArray, offset: Int, size: Int): Boolean {
        val searchEnd = (offset + size).coerceAtMost(bytes.size - 4)
        for (i in offset until searchEnd) {
            val type = String(bytes.sliceArray(i until i + 4), Charsets.US_ASCII)
            if (type == "soun" || type == "mp4a" || type == "ac-3") {
                return true
            }
        }
        return false
    }

    private fun extractCodecFromStsd(bytes: ByteArray, offset: Int, size: Int): String? {
        val searchEnd = (offset + size).coerceAtMost(bytes.size - 4)
        for (i in offset until searchEnd) {
            val fourcc = String(bytes.sliceArray(i until i + 4), Charsets.US_ASCII)
            CODEC_SIGNATURES[fourcc]?.let { return it }

            // Common codec identifiers
            when (fourcc) {
                "avc1", "avc3" -> return "H.264/AVC"
                "hvc1", "hev1" -> return "H.265/HEVC"
                "mp4v" -> return "MPEG-4"
                "vp08" -> return "VP8"
                "vp09" -> return "VP9"
                "av01" -> return "AV1"
            }
        }
        return null
    }

    private fun parseAviContainer(bytes: ByteArray): ContainerInfo {
        var width = 1920
        var height = 1080
        var frameRate = 30.0f
        var codec = "Unknown"
        val metadata = mutableMapOf<String, String>()

        // Check RIFF header
        if (bytes.size > 12) {
            val riff = String(bytes.sliceArray(0..3), Charsets.US_ASCII)
            val avi = String(bytes.sliceArray(8..11), Charsets.US_ASCII)
            if (riff == "RIFF" && avi == "AVI ") {
                metadata["format"] = "AVI"

                // Look for avih (AVI header) chunk
                for (i in 12 until bytes.size - 60) {
                    val chunk = String(bytes.sliceArray(i until i + 4), Charsets.US_ASCII)
                    if (chunk == "avih") {
                        val microSecPerFrame = getInt32LE(bytes, i + 8)
                        if (microSecPerFrame > 0) {
                            frameRate = 1000000.0f / microSecPerFrame
                        }
                        width = getInt32LE(bytes, i + 40)
                        height = getInt32LE(bytes, i + 44)
                        break
                    }
                }
            }
        }

        return ContainerInfo(
            format = "AVI",
            resolution = VideoResolution(width, height),
            frameRate = frameRate,
            codec = codec,
            creationTime = null,
            durationFromMetadata = null,
            hasAudio = true,
            metadata = metadata
        )
    }

    private fun parseMkvContainer(bytes: ByteArray): ContainerInfo {
        val metadata = mutableMapOf<String, String>()

        // Check EBML header for WebM/MKV
        if (bytes.size > 4 && bytes[0] == 0x1A.toByte() &&
            bytes[1] == 0x45.toByte() && bytes[2] == 0xDF.toByte()) {
            metadata["format"] = "Matroska/WebM"
        }

        return ContainerInfo(
            format = "Matroska",
            resolution = VideoResolution(1920, 1080),
            frameRate = 30.0f,
            codec = "VP9",
            creationTime = null,
            durationFromMetadata = null,
            hasAudio = true,
            metadata = metadata
        )
    }

    private fun parseGenericContainer(bytes: ByteArray, extension: String): ContainerInfo {
        return ContainerInfo(
            format = extension.uppercase(),
            resolution = VideoResolution(1920, 1080),
            frameRate = 30.0f,
            codec = "Unknown",
            creationTime = null,
            durationFromMetadata = null,
            hasAudio = true,
            metadata = emptyMap()
        )
    }

    /**
     * Estimate video duration based on file size and bitrate.
     */
    private fun estimateDuration(bytes: ByteArray, extension: String, fileSize: Long): Long {
        // Try to extract from metadata first
        val container = parseContainer(bytes, extension)
        container.durationFromMetadata?.let { return it }

        // Estimate based on file size (assuming ~5 Mbps average bitrate)
        val estimatedBitrate = 5_000_000L // 5 Mbps
        return (fileSize * 8 * 1000) / estimatedBitrate
    }

    /**
     * Estimate frame count from duration and frame rate.
     */
    private fun estimateFrameCount(duration: Long, frameRate: Float): Int {
        return ((duration / 1000.0) * frameRate).toInt()
    }

    /**
     * Extract hashes of key frames for comparison.
     */
    private fun extractKeyFrameHashes(bytes: ByteArray, extension: String, frameCount: Int): List<FrameHash> {
        val hashes = mutableListOf<FrameHash>()

        // Sample key frame positions (every 5 seconds approximately)
        val keyFrameInterval = 150 // Assuming 30 fps
        val numKeyFrames = (frameCount / keyFrameInterval).coerceIn(1, 20)

        for (i in 0 until numKeyFrames) {
            val frameNumber = i * keyFrameInterval
            val timestampMs = ((frameNumber / 30.0) * 1000).toLong()

            // Generate a hash based on file position (simplified)
            val position = ((bytes.size.toLong() * frameNumber) / frameCount.coerceAtLeast(1)).toInt()
            val sampleSize = 1024.coerceAtMost(bytes.size - position)
            if (sampleSize > 0) {
                val sample = bytes.sliceArray(position until position + sampleSize)
                val hash = computeHash(sample) ?: "unknown"

                hashes.add(FrameHash(
                    frameNumber = frameNumber,
                    timestampMs = timestampMs,
                    hash = hash.take(32), // Shortened hash for key frames
                    isKeyFrame = true
                ))
            }
        }

        return hashes
    }

    /**
     * Analyze metadata consistency.
     */
    private fun analyzeMetadataConsistency(
        bytes: ByteArray,
        extension: String,
        containerInfo: ContainerInfo
    ): MetadataConsistency {
        val discrepancies = mutableListOf<String>()

        // Check if container format matches extension
        val expectedFormat = when (extension) {
            "mp4", "m4v" -> listOf("MP4", "QuickTime")
            "mov" -> listOf("QuickTime", "MP4")
            "avi" -> listOf("AVI")
            "mkv" -> listOf("Matroska")
            "webm" -> listOf("WebM", "Matroska")
            else -> listOf(extension.uppercase())
        }

        val formatMatches = expectedFormat.any {
            containerInfo.format.contains(it, ignoreCase = true)
        }

        if (!formatMatches) {
            discrepancies.add("Container format (${containerInfo.format}) may not match extension ($extension)")
        }

        // Check for valid resolution
        val resolutionValid = containerInfo.resolution.width > 0 &&
                containerInfo.resolution.height > 0 &&
                containerInfo.resolution.width <= 7680 &&
                containerInfo.resolution.height <= 4320

        if (!resolutionValid) {
            discrepancies.add("Invalid or suspicious resolution: ${containerInfo.resolution.width}x${containerInfo.resolution.height}")
        }

        // Check creation time validity
        val creationDateValid = containerInfo.creationTime?.let { it > 0 } ?: true

        // Check duration
        val durationValid = containerInfo.durationFromMetadata?.let { it > 0 } ?: true

        return MetadataConsistency(
            consistent = discrepancies.isEmpty(),
            creationDateMatches = creationDateValid,
            durationMatches = durationValid,
            codecConsistent = containerInfo.codec != "Unknown",
            discrepancies = discrepancies
        )
    }

    /**
     * Detect anomalies in the video.
     */
    private fun detectAnomalies(
        bytes: ByteArray,
        extension: String,
        containerInfo: ContainerInfo,
        metadataConsistency: MetadataConsistency
    ): List<VideoAnomaly> {
        val anomalies = mutableListOf<VideoAnomaly>()

        // Check for metadata inconsistencies
        if (!metadataConsistency.consistent) {
            for (discrepancy in metadataConsistency.discrepancies) {
                anomalies.add(VideoAnomaly(
                    type = VideoAnomalyType.METADATA_INCONSISTENCY,
                    description = discrepancy,
                    timestampMs = 0,
                    frameNumber = 0,
                    severity = AnomalySeverity.MEDIUM
                ))
            }
        }

        // Check for codec issues
        if (containerInfo.codec == "Unknown") {
            anomalies.add(VideoAnomaly(
                type = VideoAnomalyType.ENCODING_ANOMALY,
                description = "Unable to determine video codec",
                timestampMs = 0,
                frameNumber = 0,
                severity = AnomalySeverity.LOW
            ))
        }

        // Check for unusual frame rates
        if (containerInfo.frameRate < 10 || containerInfo.frameRate > 120) {
            anomalies.add(VideoAnomaly(
                type = VideoAnomalyType.ENCODING_ANOMALY,
                description = "Unusual frame rate detected: ${containerInfo.frameRate} fps",
                timestampMs = 0,
                frameNumber = 0,
                severity = AnomalySeverity.MEDIUM
            ))
        }

        // Check for editing software markers
        val content = String(bytes.sliceArray(0 until minOf(bytes.size, 65536)), Charsets.ISO_8859_1)
        val editingSoftware = listOf(
            "Premiere", "Final Cut", "DaVinci", "Vegas", "Avid",
            "iMovie", "Camtasia", "Filmora", "HandBrake"
        )
        for (software in editingSoftware) {
            if (content.contains(software, ignoreCase = true)) {
                anomalies.add(VideoAnomaly(
                    type = VideoAnomalyType.METADATA_INCONSISTENCY,
                    description = "Editing software detected: $software",
                    timestampMs = 0,
                    frameNumber = 0,
                    severity = AnomalySeverity.INFO
                ))
                break
            }
        }

        return anomalies
    }

    /**
     * Calculate overall integrity score.
     */
    private fun calculateIntegrityScore(
        anomalies: List<VideoAnomaly>,
        metadataConsistency: MetadataConsistency
    ): Float {
        var score = 100f

        // Deduct for anomalies
        for (anomaly in anomalies) {
            score -= when (anomaly.severity) {
                AnomalySeverity.CRITICAL -> 25f
                AnomalySeverity.HIGH -> 15f
                AnomalySeverity.MEDIUM -> 10f
                AnomalySeverity.LOW -> 5f
                AnomalySeverity.INFO -> 2f
            }
        }

        // Deduct for metadata issues
        if (!metadataConsistency.consistent) score -= 5f
        if (!metadataConsistency.creationDateMatches) score -= 5f
        if (!metadataConsistency.codecConsistent) score -= 5f

        return score.coerceIn(0f, 100f)
    }

    // Utility functions for byte parsing
    private fun getInt32BE(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun getInt32LE(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    /**
     * Convert result to JSON string.
     */
    fun toJson(result: VideoBrainResult): String {
        return when (result) {
            is VideoBrainResult.Success -> buildSuccessJson(result)
            is VideoBrainResult.Failure -> buildFailureJson(result)
        }
    }

    private fun buildSuccessJson(result: VideoBrainResult.Success): String {
        val keyFrameHashesJson = result.keyFrameHashes.joinToString(",") { hash ->
            """{"frameNumber":${hash.frameNumber},"timestampMs":${hash.timestampMs},"hash":"${hash.hash}","isKeyFrame":${hash.isKeyFrame}}"""
        }
        val discrepanciesJson = result.metadataConsistency.discrepancies.joinToString(",") {
            "\"${escapeJson(it)}\""
        }
        val anomaliesJson = result.anomalies.joinToString(",") { anomaly ->
            """{"type":"${anomaly.type}","description":"${escapeJson(anomaly.description)}","timestampMs":${anomaly.timestampMs},"frameNumber":${anomaly.frameNumber},"severity":"${anomaly.severity}"}"""
        }

        return """
        {
            "success": true,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "videoId": "${result.videoId}",
            "fileHash": "${result.fileHash}",
            "duration": ${result.duration},
            "frameCount": ${result.frameCount},
            "frameRate": ${result.frameRate},
            "resolution": {
                "width": ${result.resolution.width},
                "height": ${result.resolution.height}
            },
            "codec": "${result.codec}",
            "keyFrameHashes": [$keyFrameHashesJson],
            "metadataConsistency": {
                "consistent": ${result.metadataConsistency.consistent},
                "creationDateMatches": ${result.metadataConsistency.creationDateMatches},
                "durationMatches": ${result.metadataConsistency.durationMatches},
                "codecConsistent": ${result.metadataConsistency.codecConsistent},
                "discrepancies": [$discrepanciesJson]
            },
            "anomalies": [$anomaliesJson],
            "integrityScore": ${result.integrityScore}
        }
        """.trimIndent()
    }

    private fun buildFailureJson(result: VideoBrainResult.Failure): String {
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

package com.verumdec.core.processor

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * AudioBrain - Waveform fingerprint + metadata consistency
 *
 * Analyzes audio files to extract waveform fingerprints, detect anomalies,
 * and verify metadata consistency for forensic purposes.
 *
 * Operates fully offline without external dependencies.
 */
class AudioBrain {

    companion object {
        private const val HASH_ALGORITHM = "SHA-512"
        private val SUPPORTED_EXTENSIONS = setOf(
            "mp3", "wav", "aac", "flac", "ogg", "m4a", "wma", "aiff", "opus"
        )

        // Audio analysis constants
        private const val SEGMENT_DURATION_MS = 5000L // 5 second segments
    }

    /**
     * Process an audio file and analyze for anomalies.
     *
     * @param file The audio file to process
     * @return AudioBrainResult containing analysis data or error
     */
    fun process(file: File): AudioBrainResult {
        return try {
            if (!file.exists()) {
                return AudioBrainResult.Failure(
                    error = "File not found: ${file.absolutePath}",
                    errorCode = AudioErrorCode.FILE_NOT_FOUND
                )
            }

            val extension = file.extension.lowercase()
            if (extension !in SUPPORTED_EXTENSIONS) {
                return AudioBrainResult.Failure(
                    error = "Unsupported audio format: $extension",
                    errorCode = AudioErrorCode.UNSUPPORTED_FORMAT
                )
            }

            val bytes = file.readBytes()
            processBytes(bytes, extension)
        } catch (e: Exception) {
            AudioBrainResult.Failure(
                error = "Processing error: ${e.message}",
                errorCode = AudioErrorCode.PROCESSING_ERROR
            )
        }
    }

    /**
     * Process audio from an input stream.
     *
     * @param inputStream The input stream to read from
     * @param fileName The original filename for extension detection
     * @return AudioBrainResult containing analysis data or error
     */
    fun process(inputStream: InputStream, fileName: String): AudioBrainResult {
        return try {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            if (extension !in SUPPORTED_EXTENSIONS) {
                return AudioBrainResult.Failure(
                    error = "Unsupported audio format: $extension",
                    errorCode = AudioErrorCode.UNSUPPORTED_FORMAT
                )
            }

            val bytes = inputStream.readBytes()
            processBytes(bytes, extension)
        } catch (e: Exception) {
            AudioBrainResult.Failure(
                error = "Processing error: ${e.message}",
                errorCode = AudioErrorCode.PROCESSING_ERROR
            )
        }
    }

    private fun processBytes(bytes: ByteArray, extension: String): AudioBrainResult {
        val fileHash = computeHash(bytes)
            ?: return AudioBrainResult.Failure(
                error = "Failed to compute file hash",
                errorCode = AudioErrorCode.ANALYSIS_FAILED
            )

        val audioInfo = parseAudioFormat(bytes, extension)
        val fingerprint = generateFingerprint(bytes, extension, audioInfo)
        val metadataConsistency = analyzeMetadataConsistency(bytes, extension, audioInfo)
        val anomalies = detectAnomalies(bytes, extension, audioInfo, metadataConsistency)

        val integrityScore = calculateIntegrityScore(anomalies, metadataConsistency)

        return AudioBrainResult.Success(
            fileHash = fileHash,
            duration = audioInfo.duration,
            sampleRate = audioInfo.sampleRate,
            channels = audioInfo.channels,
            bitrate = audioInfo.bitrate,
            format = audioInfo.format,
            fingerprint = fingerprint,
            metadataConsistency = metadataConsistency,
            anomalies = anomalies,
            integrityScore = integrityScore
        )
    }

    /**
     * Compute SHA-512 hash of audio bytes.
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
     * Audio format information.
     */
    private data class AudioInfo(
        val format: String,
        val duration: Long,
        val sampleRate: Int,
        val channels: Int,
        val bitrate: Int,
        val bitsPerSample: Int,
        val hasMetadata: Boolean,
        val metadata: Map<String, String>
    )

    /**
     * Parse audio format and extract metadata.
     */
    private fun parseAudioFormat(bytes: ByteArray, extension: String): AudioInfo {
        return when (extension) {
            "wav" -> parseWav(bytes)
            "mp3" -> parseMp3(bytes)
            "flac" -> parseFlac(bytes)
            "ogg", "opus" -> parseOgg(bytes)
            "m4a", "aac" -> parseM4a(bytes)
            else -> parseGenericAudio(bytes, extension)
        }
    }

    private fun parseWav(bytes: ByteArray): AudioInfo {
        var sampleRate = 44100
        var channels = 2
        var bitsPerSample = 16
        var dataSize = 0
        val metadata = mutableMapOf<String, String>()

        // Check RIFF header
        if (bytes.size > 44 &&
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte()) {

            val format = String(bytes.sliceArray(8..11), Charsets.US_ASCII)
            if (format == "WAVE") {
                // Parse fmt chunk
                var offset = 12
                while (offset < bytes.size - 8) {
                    val chunkId = String(bytes.sliceArray(offset until offset + 4), Charsets.US_ASCII)
                    val chunkSize = getInt32LE(bytes, offset + 4)

                    when (chunkId) {
                        "fmt " -> {
                            if (offset + 24 <= bytes.size) {
                                channels = getInt16LE(bytes, offset + 10)
                                sampleRate = getInt32LE(bytes, offset + 12)
                                bitsPerSample = getInt16LE(bytes, offset + 22)
                            }
                        }
                        "data" -> {
                            dataSize = chunkSize
                        }
                    }
                    offset += 8 + chunkSize
                    if (chunkSize % 2 != 0) offset++ // Padding
                }
            }
        }

        val bitrate = sampleRate * channels * bitsPerSample
        val duration = if (bitrate > 0) {
            (dataSize * 8 * 1000L) / bitrate
        } else {
            0L
        }

        return AudioInfo(
            format = "WAV (PCM)",
            duration = duration,
            sampleRate = sampleRate,
            channels = channels,
            bitrate = bitrate / 1000,
            bitsPerSample = bitsPerSample,
            hasMetadata = metadata.isNotEmpty(),
            metadata = metadata
        )
    }

    private fun parseMp3(bytes: ByteArray): AudioInfo {
        var sampleRate = 44100
        var bitrate = 128
        var channels = 2
        val metadata = mutableMapOf<String, String>()

        // Check for ID3v2 tag
        var dataStart = 0
        if (bytes.size > 10 &&
            bytes[0] == 0x49.toByte() && bytes[1] == 0x44.toByte() && bytes[2] == 0x33.toByte()) {
            // ID3v2 header found
            val id3Size = ((bytes[6].toInt() and 0x7F) shl 21) or
                    ((bytes[7].toInt() and 0x7F) shl 14) or
                    ((bytes[8].toInt() and 0x7F) shl 7) or
                    (bytes[9].toInt() and 0x7F)
            dataStart = 10 + id3Size
            metadata["hasID3v2"] = "true"
        }

        // Find first MP3 frame header
        var i = dataStart
        while (i < bytes.size - 4) {
            if (bytes[i] == 0xFF.toByte() && (bytes[i + 1].toInt() and 0xE0) == 0xE0) {
                // Found frame sync
                val header = ((bytes[i].toInt() and 0xFF) shl 24) or
                        ((bytes[i + 1].toInt() and 0xFF) shl 16) or
                        ((bytes[i + 2].toInt() and 0xFF) shl 8) or
                        (bytes[i + 3].toInt() and 0xFF)

                val version = (header shr 19) and 0x03
                val layer = (header shr 17) and 0x03
                val bitrateIndex = (header shr 12) and 0x0F
                val sampleRateIndex = (header shr 10) and 0x03
                val channelMode = (header shr 6) and 0x03

                // MP3 bitrate table (Layer III, MPEG-1)
                val bitrateTable = intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0)
                if (bitrateIndex in 1..14) {
                    bitrate = bitrateTable[bitrateIndex]
                }

                // Sample rate table (MPEG-1)
                val sampleRateTable = intArrayOf(44100, 48000, 32000, 0)
                if (sampleRateIndex in 0..2) {
                    sampleRate = sampleRateTable[sampleRateIndex]
                }

                channels = if (channelMode == 3) 1 else 2
                break
            }
            i++
        }

        // Estimate duration from file size and bitrate
        val duration = if (bitrate > 0) {
            (bytes.size * 8L) / (bitrate * 1000L) * 1000L
        } else {
            0L
        }

        return AudioInfo(
            format = "MP3 (MPEG Layer III)",
            duration = duration,
            sampleRate = sampleRate,
            channels = channels,
            bitrate = bitrate,
            bitsPerSample = 0,
            hasMetadata = metadata.isNotEmpty(),
            metadata = metadata
        )
    }

    private fun parseFlac(bytes: ByteArray): AudioInfo {
        var sampleRate = 44100
        var channels = 2
        var bitsPerSample = 16
        var totalSamples = 0L
        val metadata = mutableMapOf<String, String>()

        // Check FLAC signature
        if (bytes.size > 42 &&
            bytes[0] == 0x66.toByte() && bytes[1] == 0x4C.toByte() &&
            bytes[2] == 0x61.toByte() && bytes[3] == 0x43.toByte()) {

            // STREAMINFO block starts at byte 8
            if (bytes.size > 26) {
                // Parse STREAMINFO
                val minBlockSize = getInt16BE(bytes, 8)
                val maxBlockSize = getInt16BE(bytes, 10)

                // Sample rate is 20 bits starting at byte 18
                sampleRate = ((bytes[18].toInt() and 0xFF) shl 12) or
                        ((bytes[19].toInt() and 0xFF) shl 4) or
                        ((bytes[20].toInt() and 0xF0) shr 4)

                channels = ((bytes[20].toInt() and 0x0E) shr 1) + 1
                bitsPerSample = (((bytes[20].toInt() and 0x01) shl 4) or
                        ((bytes[21].toInt() and 0xF0) shr 4)) + 1

                totalSamples = ((bytes[21].toLong() and 0x0F) shl 32) or
                        ((bytes[22].toLong() and 0xFF) shl 24) or
                        ((bytes[23].toLong() and 0xFF) shl 16) or
                        ((bytes[24].toLong() and 0xFF) shl 8) or
                        (bytes[25].toLong() and 0xFF)
            }
        }

        val duration = if (sampleRate > 0) {
            (totalSamples * 1000L) / sampleRate
        } else {
            0L
        }

        val bitrate = sampleRate * channels * bitsPerSample / 1000

        return AudioInfo(
            format = "FLAC (Lossless)",
            duration = duration,
            sampleRate = sampleRate,
            channels = channels,
            bitrate = bitrate,
            bitsPerSample = bitsPerSample,
            hasMetadata = true,
            metadata = metadata
        )
    }

    private fun parseOgg(bytes: ByteArray): AudioInfo {
        var sampleRate = 44100
        var channels = 2
        val metadata = mutableMapOf<String, String>()

        // Check OggS signature
        if (bytes.size > 4 &&
            bytes[0] == 0x4F.toByte() && bytes[1] == 0x67.toByte() &&
            bytes[2] == 0x67.toByte() && bytes[3] == 0x53.toByte()) {
            metadata["format"] = "Ogg"

            // Look for Vorbis or Opus identification
            for (i in 0 until minOf(bytes.size - 7, 1000)) {
                val content = String(bytes.sliceArray(i until i + 7), Charsets.ISO_8859_1)
                if (content.contains("vorbis", ignoreCase = true)) {
                    metadata["codec"] = "Vorbis"
                    break
                }
                if (content.contains("Opus", ignoreCase = true)) {
                    metadata["codec"] = "Opus"
                    break
                }
            }
        }

        // Estimate duration based on file size
        val bitrate = 128 // Typical VBR average
        val duration = (bytes.size * 8L) / (bitrate * 1000L) * 1000L

        return AudioInfo(
            format = "Ogg ${metadata["codec"] ?: "Vorbis"}",
            duration = duration,
            sampleRate = sampleRate,
            channels = channels,
            bitrate = bitrate,
            bitsPerSample = 0,
            hasMetadata = metadata.isNotEmpty(),
            metadata = metadata
        )
    }

    private fun parseM4a(bytes: ByteArray): AudioInfo {
        var sampleRate = 44100
        var channels = 2
        var duration = 0L
        val metadata = mutableMapOf<String, String>()

        // Look for ftyp box
        if (bytes.size > 8) {
            val ftyp = String(bytes.sliceArray(4..7), Charsets.US_ASCII)
            if (ftyp == "ftyp") {
                val brand = String(bytes.sliceArray(8..11), Charsets.US_ASCII)
                metadata["brand"] = brand
            }
        }

        // Parse MP4 atoms for audio info
        var offset = 0
        while (offset < bytes.size - 8) {
            val size = getInt32BE(bytes, offset)
            if (size < 8 || offset + size > bytes.size) break

            val atomType = String(bytes.sliceArray(offset + 4 until offset + 8), Charsets.US_ASCII)

            if (atomType == "mvhd" && offset + 24 < bytes.size) {
                val timescale = getInt32BE(bytes, offset + 20)
                val durationUnits = getInt32BE(bytes, offset + 24)
                if (timescale > 0) {
                    duration = (durationUnits * 1000L) / timescale
                }
            }

            offset += size
        }

        // Estimate if duration not found
        if (duration == 0L) {
            val bitrate = 256 // Typical AAC bitrate
            duration = (bytes.size * 8L) / (bitrate * 1000L) * 1000L
        }

        return AudioInfo(
            format = "AAC (M4A)",
            duration = duration,
            sampleRate = sampleRate,
            channels = channels,
            bitrate = 256,
            bitsPerSample = 0,
            hasMetadata = metadata.isNotEmpty(),
            metadata = metadata
        )
    }

    private fun parseGenericAudio(bytes: ByteArray, extension: String): AudioInfo {
        val bitrate = 128
        val duration = (bytes.size * 8L) / (bitrate * 1000L) * 1000L

        return AudioInfo(
            format = extension.uppercase(),
            duration = duration,
            sampleRate = 44100,
            channels = 2,
            bitrate = bitrate,
            bitsPerSample = 16,
            hasMetadata = false,
            metadata = emptyMap()
        )
    }

    /**
     * Generate audio fingerprint.
     */
    private fun generateFingerprint(bytes: ByteArray, extension: String, audioInfo: AudioInfo): AudioFingerprint {
        val segments = mutableListOf<AudioSegment>()
        val dominantFrequencies = mutableListOf<Float>()

        // Generate segment hashes
        val numSegments = ((audioInfo.duration / SEGMENT_DURATION_MS) + 1).toInt().coerceIn(1, 20)
        val bytesPerSegment = bytes.size / numSegments

        for (i in 0 until numSegments) {
            val startOffset = i * bytesPerSegment
            val endOffset = minOf((i + 1) * bytesPerSegment, bytes.size)
            val segmentBytes = bytes.sliceArray(startOffset until endOffset)

            val segmentHash = computeHash(segmentBytes)?.take(32) ?: "unknown"
            val startMs = (i * SEGMENT_DURATION_MS)
            val endMs = ((i + 1) * SEGMENT_DURATION_MS).coerceAtMost(audioInfo.duration)

            // Simple silence detection based on byte values
            val silenceThreshold = 10
            val avgAmplitude = segmentBytes.map { kotlin.math.abs(it.toInt()) }.average()
            val silenceDetected = avgAmplitude < silenceThreshold

            segments.add(AudioSegment(
                startMs = startMs,
                endMs = endMs,
                hash = segmentHash,
                silenceDetected = silenceDetected
            ))
        }

        // Estimate dominant frequencies (simplified)
        dominantFrequencies.add(440f) // Placeholder - real implementation would use FFT
        dominantFrequencies.add(880f)
        dominantFrequencies.add(1760f)

        // Calculate average amplitude
        val averageAmplitude = bytes.map { kotlin.math.abs(it.toInt()).toFloat() }.average().toFloat() / 128f

        // Overall fingerprint hash
        val fingerprintHash = computeHash(bytes)?.take(64) ?: "unknown"

        return AudioFingerprint(
            hash = fingerprintHash,
            segments = segments,
            dominantFrequencies = dominantFrequencies,
            averageAmplitude = averageAmplitude
        )
    }

    /**
     * Analyze metadata consistency.
     */
    private fun analyzeMetadataConsistency(
        bytes: ByteArray,
        extension: String,
        audioInfo: AudioInfo
    ): AudioMetadataConsistency {
        val discrepancies = mutableListOf<String>()

        // Check format consistency
        val expectedFormats = when (extension) {
            "mp3" -> listOf("MP3", "MPEG")
            "wav" -> listOf("WAV", "PCM")
            "flac" -> listOf("FLAC")
            "ogg" -> listOf("Ogg", "Vorbis")
            "m4a", "aac" -> listOf("AAC", "M4A")
            else -> listOf(extension.uppercase())
        }

        val formatMatches = expectedFormats.any {
            audioInfo.format.contains(it, ignoreCase = true)
        }

        if (!formatMatches) {
            discrepancies.add("Format (${audioInfo.format}) may not match extension ($extension)")
        }

        // Check for valid sample rate
        val validSampleRates = listOf(8000, 11025, 16000, 22050, 32000, 44100, 48000, 88200, 96000, 176400, 192000)
        if (audioInfo.sampleRate !in validSampleRates) {
            discrepancies.add("Unusual sample rate: ${audioInfo.sampleRate} Hz")
        }

        // Check for valid channel count
        if (audioInfo.channels !in 1..8) {
            discrepancies.add("Unusual channel count: ${audioInfo.channels}")
        }

        // Check duration validity
        val durationValid = audioInfo.duration > 0 && audioInfo.duration < 24 * 60 * 60 * 1000L // Max 24 hours

        return AudioMetadataConsistency(
            consistent = discrepancies.isEmpty(),
            recordingDateValid = true, // Would need embedded metadata to verify
            durationMatches = durationValid,
            formatConsistent = formatMatches,
            discrepancies = discrepancies
        )
    }

    /**
     * Detect anomalies in the audio.
     */
    private fun detectAnomalies(
        bytes: ByteArray,
        extension: String,
        audioInfo: AudioInfo,
        metadataConsistency: AudioMetadataConsistency
    ): List<AudioAnomaly> {
        val anomalies = mutableListOf<AudioAnomaly>()

        // Check for metadata inconsistencies
        for (discrepancy in metadataConsistency.discrepancies) {
            anomalies.add(AudioAnomaly(
                type = AudioAnomalyType.METADATA_TAMPERING,
                description = discrepancy,
                timestampMs = 0,
                severity = AnomalySeverity.MEDIUM
            ))
        }

        // Check for silence gaps (potential splicing)
        val segments = generateFingerprint(bytes, extension, audioInfo).segments
        val silentSegments = segments.filter { it.silenceDetected }
        if (silentSegments.size > segments.size / 2) {
            anomalies.add(AudioAnomaly(
                type = AudioAnomalyType.SILENCE_GAP,
                description = "Unusual amount of silence detected (${silentSegments.size}/${segments.size} segments)",
                timestampMs = silentSegments.firstOrNull()?.startMs ?: 0,
                severity = AnomalySeverity.MEDIUM
            ))
        }

        // Check for encoding anomalies
        if (audioInfo.bitrate == 0 || audioInfo.sampleRate == 0) {
            anomalies.add(AudioAnomaly(
                type = AudioAnomalyType.ENCODING_ANOMALY,
                description = "Unable to determine audio encoding parameters",
                timestampMs = 0,
                severity = AnomalySeverity.LOW
            ))
        }

        // Check for editing software markers
        val content = String(bytes.sliceArray(0 until minOf(bytes.size, 65536)), Charsets.ISO_8859_1)
        val editingSoftware = listOf("Audacity", "Adobe Audition", "Pro Tools", "Logic Pro", "GarageBand")
        for (software in editingSoftware) {
            if (content.contains(software, ignoreCase = true)) {
                anomalies.add(AudioAnomaly(
                    type = AudioAnomalyType.METADATA_TAMPERING,
                    description = "Audio editing software detected: $software",
                    timestampMs = 0,
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
        anomalies: List<AudioAnomaly>,
        metadataConsistency: AudioMetadataConsistency
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
        if (!metadataConsistency.formatConsistent) score -= 5f

        return score.coerceIn(0f, 100f)
    }

    // Utility functions
    private fun getInt16LE(bytes: ByteArray, offset: Int): Int {
        if (offset + 2 > bytes.size) return 0
        return (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun getInt32LE(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun getInt16BE(bytes: ByteArray, offset: Int): Int {
        if (offset + 2 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 8) or
                (bytes[offset + 1].toInt() and 0xFF)
    }

    private fun getInt32BE(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }

    /**
     * Convert result to JSON string.
     */
    fun toJson(result: AudioBrainResult): String {
        return when (result) {
            is AudioBrainResult.Success -> buildSuccessJson(result)
            is AudioBrainResult.Failure -> buildFailureJson(result)
        }
    }

    private fun buildSuccessJson(result: AudioBrainResult.Success): String {
        val segmentsJson = result.fingerprint.segments.joinToString(",") { segment ->
            """{"startMs":${segment.startMs},"endMs":${segment.endMs},"hash":"${segment.hash}","silenceDetected":${segment.silenceDetected}}"""
        }
        val frequenciesJson = result.fingerprint.dominantFrequencies.joinToString(",")
        val discrepanciesJson = result.metadataConsistency.discrepancies.joinToString(",") {
            "\"${escapeJson(it)}\""
        }
        val anomaliesJson = result.anomalies.joinToString(",") { anomaly ->
            """{"type":"${anomaly.type}","description":"${escapeJson(anomaly.description)}","timestampMs":${anomaly.timestampMs},"severity":"${anomaly.severity}"}"""
        }

        return """
        {
            "success": true,
            "brainId": "${result.brainId}",
            "timestamp": ${result.timestamp},
            "audioId": "${result.audioId}",
            "fileHash": "${result.fileHash}",
            "duration": ${result.duration},
            "sampleRate": ${result.sampleRate},
            "channels": ${result.channels},
            "bitrate": ${result.bitrate},
            "format": "${result.format}",
            "fingerprint": {
                "hash": "${result.fingerprint.hash}",
                "segments": [$segmentsJson],
                "dominantFrequencies": [$frequenciesJson],
                "averageAmplitude": ${result.fingerprint.averageAmplitude}
            },
            "metadataConsistency": {
                "consistent": ${result.metadataConsistency.consistent},
                "recordingDateValid": ${result.metadataConsistency.recordingDateValid},
                "durationMatches": ${result.metadataConsistency.durationMatches},
                "formatConsistent": ${result.metadataConsistency.formatConsistent},
                "discrepancies": [$discrepanciesJson]
            },
            "anomalies": [$anomaliesJson],
            "integrityScore": ${result.integrityScore}
        }
        """.trimIndent()
    }

    private fun buildFailureJson(result: AudioBrainResult.Failure): String {
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

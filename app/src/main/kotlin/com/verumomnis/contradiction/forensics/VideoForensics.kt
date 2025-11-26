package com.verumomnis.contradiction.forensics

import android.graphics.Bitmap
import java.security.MessageDigest

/**
 * Video Forensics Module
 *
 * Provides offline video forensic analysis capabilities:
 * - Frame hashing for tampering detection
 * - GOP (Group of Pictures) structure analysis
 * - Temporal consistency checking
 * - Re-encoding detection
 */
class VideoForensics {

    companion object {
        private const val HASH_ALGORITHM = "SHA-256"
        private const val FRAME_SAMPLE_RATE = 5 // Analyze every 5th frame
    }

    /**
     * Performs complete video forensic analysis.
     */
    fun analyzeVideo(frames: List<Bitmap>, metadata: VideoMetadata): VideoForensicResult {
        if (frames.isEmpty()) {
            return VideoForensicResult(
                frameHashes = emptyList(),
                gopAnalysis = GopAnalysis(emptyList(), 0, false),
                temporalConsistency = 1.0f,
                reEncodingScore = 0f,
                tamperingScore = 0f,
                isTampered = false,
                anomalies = emptyList()
            )
        }

        val frameHashes = computeFrameHashes(frames)
        val gopAnalysis = analyzeGopStructure(frames, metadata)
        val temporalConsistency = checkTemporalConsistency(frames)
        val reEncodingScore = detectReEncoding(frames, metadata)
        val anomalies = detectAnomalies(frames, frameHashes)

        val tamperingScore = calculateTamperingScore(
            gopAnalysis,
            temporalConsistency,
            reEncodingScore,
            anomalies
        )

        return VideoForensicResult(
            frameHashes = frameHashes,
            gopAnalysis = gopAnalysis,
            temporalConsistency = temporalConsistency,
            reEncodingScore = reEncodingScore,
            tamperingScore = tamperingScore,
            isTampered = tamperingScore > 0.5f,
            anomalies = anomalies
        )
    }

    /**
     * Computes perceptual hashes for frames.
     */
    private fun computeFrameHashes(frames: List<Bitmap>): List<FrameHash> {
        return frames.mapIndexed { index, frame ->
            if (index % FRAME_SAMPLE_RATE == 0) {
                val hash = computePerceptualHash(frame)
                FrameHash(
                    frameIndex = index,
                    hash = hash,
                    timestamp = index.toFloat() / 30f // Assuming 30fps
                )
            } else null
        }.filterNotNull()
    }

    /**
     * Computes perceptual hash (pHash) for an image.
     * This is resistant to minor visual changes but detects significant modifications.
     */
    private fun computePerceptualHash(bitmap: Bitmap): String {
        // Resize to small fixed size
        val scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, true)

        // Convert to grayscale and compute DCT-like hash
        val pixels = IntArray(32 * 32)
        scaled.getPixels(pixels, 0, 32, 0, 0, 32, 32)

        val grayscale = pixels.map { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }

        // Compute average
        val avg = grayscale.average()

        // Create binary hash based on comparison to average
        val hashBits = grayscale.map { if (it > avg) '1' else '0' }.joinToString("")

        // Convert to hex string
        return hashBits.chunked(4).map {
            it.toInt(2).toString(16)
        }.joinToString("")
    }

    /**
     * Analyzes GOP (Group of Pictures) structure for consistency.
     */
    private fun analyzeGopStructure(frames: List<Bitmap>, metadata: VideoMetadata): GopAnalysis {
        val gopBoundaries = mutableListOf<Int>()
        var previousHash = ""

        // Detect scene changes / I-frame boundaries
        for (i in frames.indices) {
            val currentHash = computePerceptualHash(frames[i])

            if (previousHash.isNotEmpty()) {
                val similarity = computeHashSimilarity(previousHash, currentHash)
                if (similarity < 0.7f) { // Scene change detected
                    gopBoundaries.add(i)
                }
            }
            previousHash = currentHash
        }

        // Analyze GOP lengths
        val gopLengths = mutableListOf<Int>()
        var prevBoundary = 0
        for (boundary in gopBoundaries) {
            gopLengths.add(boundary - prevBoundary)
            prevBoundary = boundary
        }

        // Check for inconsistent GOP structure (sign of tampering)
        val avgGopLength = if (gopLengths.isNotEmpty()) gopLengths.average() else 0.0
        val isConsistent = gopLengths.all { kotlin.math.abs(it - avgGopLength) < avgGopLength * 0.5 }

        return GopAnalysis(
            gopBoundaries = gopBoundaries,
            averageGopLength = avgGopLength.toInt(),
            isStructureConsistent = isConsistent
        )
    }

    /**
     * Computes similarity between two perceptual hashes.
     */
    private fun computeHashSimilarity(hash1: String, hash2: String): Float {
        if (hash1.length != hash2.length) return 0f

        val matchingChars = hash1.zip(hash2).count { (a, b) -> a == b }
        return matchingChars.toFloat() / hash1.length
    }

    /**
     * Checks temporal consistency between frames.
     */
    private fun checkTemporalConsistency(frames: List<Bitmap>): Float {
        if (frames.size < 2) return 1.0f

        var consistencyScore = 0f
        var comparisons = 0

        for (i in 1 until frames.size step FRAME_SAMPLE_RATE) {
            val hash1 = computePerceptualHash(frames[i - 1])
            val hash2 = computePerceptualHash(frames[i])
            val similarity = computeHashSimilarity(hash1, hash2)

            // Adjacent frames should have some similarity unless scene change
            consistencyScore += if (similarity > 0.3f || similarity < 0.1f) 1f else 0.5f
            comparisons++
        }

        return if (comparisons > 0) consistencyScore / comparisons else 1.0f
    }

    /**
     * Detects signs of re-encoding.
     */
    private fun detectReEncoding(frames: List<Bitmap>, metadata: VideoMetadata): Float {
        var score = 0f

        // Check for compression artifacts
        val artifactScore = detectCompressionArtifacts(frames)
        score += artifactScore * 0.5f

        // Check metadata for re-encoding signs
        if (metadata.encodingPasses > 1) {
            score += 0.3f
        }

        if (metadata.hasEmbeddedMetadata && metadata.creationDate == null) {
            // Metadata stripped - possible re-encoding
            score += 0.2f
        }

        return score.coerceIn(0f, 1f)
    }

    /**
     * Detects compression artifacts in frames.
     */
    private fun detectCompressionArtifacts(frames: List<Bitmap>): Float {
        if (frames.isEmpty()) return 0f

        val frame = frames[frames.size / 2] // Check middle frame
        val scaled = Bitmap.createScaledBitmap(frame, 64, 64, true)
        val pixels = IntArray(64 * 64)
        scaled.getPixels(pixels, 0, 64, 0, 0, 64, 64)

        // Check for blocking artifacts (8x8 block boundaries)
        var blockScore = 0
        for (y in 0 until 64 - 8 step 8) {
            for (x in 0 until 64 - 8 step 8) {
                val blockEdgeVariance = calculateBlockEdgeVariance(pixels, x, y, 64)
                if (blockEdgeVariance > 50) { // High variance at block boundaries
                    blockScore++
                }
            }
        }

        return (blockScore.toFloat() / 64).coerceIn(0f, 1f)
    }

    /**
     * Calculates variance at block edges.
     */
    private fun calculateBlockEdgeVariance(pixels: IntArray, blockX: Int, blockY: Int, stride: Int): Double {
        val edgePixels = mutableListOf<Int>()

        // Get pixels at block boundary
        for (i in 0..7) {
            val idx = (blockY + i) * stride + blockX + 7
            if (idx < pixels.size) {
                edgePixels.add(pixels[idx] and 0xFF)
            }
        }

        if (edgePixels.size < 2) return 0.0

        val mean = edgePixels.average()
        return edgePixels.map { (it - mean) * (it - mean) }.average()
    }

    /**
     * Detects specific anomalies in the video.
     */
    private fun detectAnomalies(frames: List<Bitmap>, hashes: List<FrameHash>): List<VideoAnomaly> {
        val anomalies = mutableListOf<VideoAnomaly>()

        // Check for duplicate frames
        val hashGroups = hashes.groupBy { it.hash }
        for ((hash, duplicates) in hashGroups) {
            if (duplicates.size > 1 && duplicates.size < hashes.size / 2) {
                // Non-consecutive duplicate frames (excluding still scenes)
                val indices = duplicates.map { it.frameIndex }
                if (indices.zipWithNext().any { (a, b) -> b - a > FRAME_SAMPLE_RATE * 2 }) {
                    anomalies.add(
                        VideoAnomaly(
                            type = AnomalyType.DUPLICATE_FRAMES,
                            frameIndices = indices,
                            confidence = 0.8f,
                            description = "Non-consecutive duplicate frames detected"
                        )
                    )
                }
            }
        }

        // Check for sudden visual jumps
        for (i in 1 until hashes.size) {
            val similarity = computeHashSimilarity(hashes[i - 1].hash, hashes[i].hash)
            if (similarity < 0.2f) {
                anomalies.add(
                    VideoAnomaly(
                        type = AnomalyType.VISUAL_DISCONTINUITY,
                        frameIndices = listOf(hashes[i - 1].frameIndex, hashes[i].frameIndex),
                        confidence = 1f - similarity,
                        description = "Sudden visual discontinuity between frames"
                    )
                )
            }
        }

        return anomalies
    }

    /**
     * Calculates overall tampering score.
     */
    private fun calculateTamperingScore(
        gopAnalysis: GopAnalysis,
        temporalConsistency: Float,
        reEncodingScore: Float,
        anomalies: List<VideoAnomaly>
    ): Float {
        var score = 0f

        // GOP structure issues
        if (!gopAnalysis.isStructureConsistent) {
            score += 0.25f
        }

        // Temporal inconsistency
        score += (1f - temporalConsistency) * 0.25f

        // Re-encoding signs
        score += reEncodingScore * 0.25f

        // Anomalies
        val anomalyScore = anomalies.map { it.confidence }.average().toFloat()
        score += if (anomalies.isNotEmpty()) anomalyScore * 0.25f else 0f

        return score.coerceIn(0f, 1f)
    }
}

/**
 * Complete video forensic analysis result.
 */
data class VideoForensicResult(
    val frameHashes: List<FrameHash>,
    val gopAnalysis: GopAnalysis,
    val temporalConsistency: Float,
    val reEncodingScore: Float,
    val tamperingScore: Float,
    val isTampered: Boolean,
    val anomalies: List<VideoAnomaly>
)

/**
 * Frame hash information.
 */
data class FrameHash(
    val frameIndex: Int,
    val hash: String,
    val timestamp: Float
)

/**
 * GOP structure analysis.
 */
data class GopAnalysis(
    val gopBoundaries: List<Int>,
    val averageGopLength: Int,
    val isStructureConsistent: Boolean
)

/**
 * Video metadata for analysis.
 */
data class VideoMetadata(
    val duration: Float,
    val frameRate: Float,
    val width: Int,
    val height: Int,
    val codec: String?,
    val encodingPasses: Int = 1,
    val hasEmbeddedMetadata: Boolean = true,
    val creationDate: String? = null
)

/**
 * Detected video anomaly.
 */
data class VideoAnomaly(
    val type: AnomalyType,
    val frameIndices: List<Int>,
    val confidence: Float,
    val description: String
)

/**
 * Types of video anomalies.
 */
enum class AnomalyType {
    DUPLICATE_FRAMES,
    VISUAL_DISCONTINUITY,
    TEMPORAL_INCONSISTENCY,
    COMPRESSION_ARTIFACT,
    SPLICING_DETECTED
}

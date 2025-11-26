package com.verumomnis.contradiction.forensics

import android.graphics.Bitmap

/**
 * Deepfake Fusion Module
 *
 * Combines multiple forensic analysis modules to provide
 * comprehensive deepfake/manipulation detection.
 *
 * This module fuses results from:
 * - Image forensics (ELA, noise analysis)
 * - Video forensics (frame consistency, GOP structure)
 * - Voice forensics (MFCC analysis, spectral features)
 *
 * To produce a unified manipulation confidence score.
 */
class DeepfakeFusion {

    private val imageForensics = ImageForensics()
    private val videoForensics = VideoForensics()
    private val voiceForensics = VoiceForensics()

    /**
     * Performs comprehensive deepfake analysis on multimedia content.
     */
    fun analyzeMedia(content: MediaContent): DeepfakeAnalysisResult {
        val imageResults = content.images?.map { imageForensics.analyzeImage(it) }
        val videoResult = content.videoFrames?.let { frames ->
            content.videoMetadata?.let { meta ->
                videoForensics.analyzeVideo(frames, meta)
            }
        }
        val voiceResult = content.audioSamples?.let { voiceForensics.analyzeAudio(it) }

        // Calculate individual manipulation scores
        val imageScore = imageResults?.map { it.tamperingScore }?.average()?.toFloat() ?: 0f
        val videoScore = videoResult?.tamperingScore ?: 0f
        val voiceScore = voiceResult?.tamperingScore ?: 0f

        // Analyze cross-modal consistency
        val crossModalConsistency = analyzeCrossModalConsistency(
            imageScore,
            videoScore,
            voiceScore
        )

        // Apply fusion algorithm
        val fusedScore = calculateFusedScore(
            imageScore = imageScore,
            videoScore = videoScore,
            voiceScore = voiceScore,
            crossModalConsistency = crossModalConsistency,
            weights = content.analysisWeights
        )

        // Generate detailed findings
        val findings = generateFindings(
            imageResults = imageResults,
            videoResult = videoResult,
            voiceResult = voiceResult,
            crossModalConsistency = crossModalConsistency
        )

        // Determine manipulation likelihood
        val manipulationLikelihood = classifyManipulation(fusedScore, findings)

        return DeepfakeAnalysisResult(
            overallScore = fusedScore,
            imageAnalysisScore = imageScore,
            videoAnalysisScore = videoScore,
            voiceAnalysisScore = voiceScore,
            crossModalConsistency = crossModalConsistency,
            manipulationLikelihood = manipulationLikelihood,
            findings = findings,
            recommendations = generateRecommendations(findings, manipulationLikelihood)
        )
    }

    /**
     * Analyzes consistency across different modalities.
     */
    private fun analyzeCrossModalConsistency(
        imageScore: Float,
        videoScore: Float,
        voiceScore: Float
    ): CrossModalConsistency {
        val scores = listOfNotNull(
            if (imageScore > 0) imageScore else null,
            if (videoScore > 0) videoScore else null,
            if (voiceScore > 0) voiceScore else null
        )

        if (scores.size < 2) {
            return CrossModalConsistency(
                isConsistent = true,
                consistencyScore = 1.0f,
                discrepancies = emptyList()
            )
        }

        // Check for discrepancies between modalities
        val discrepancies = mutableListOf<String>()
        val avgScore = scores.average().toFloat()
        val variance = scores.map { (it - avgScore) * (it - avgScore) }.average().toFloat()

        if (variance > 0.1f) {
            if (imageScore > avgScore + 0.2f) {
                discrepancies.add("Image manipulation indicators significantly higher than other modalities")
            }
            if (videoScore > avgScore + 0.2f) {
                discrepancies.add("Video manipulation indicators significantly higher than other modalities")
            }
            if (voiceScore > avgScore + 0.2f) {
                discrepancies.add("Voice manipulation indicators significantly higher than other modalities")
            }
        }

        // Check for unlikely combinations
        if (videoScore > 0.5f && voiceScore < 0.2f) {
            discrepancies.add("Video shows manipulation but audio appears authentic - possible video-only edit")
        }
        if (voiceScore > 0.5f && videoScore < 0.2f) {
            discrepancies.add("Audio shows manipulation but video appears authentic - possible audio replacement")
        }

        val consistencyScore = 1.0f - kotlin.math.sqrt(variance).coerceAtMost(0.5f) * 2

        return CrossModalConsistency(
            isConsistent = discrepancies.isEmpty(),
            consistencyScore = consistencyScore,
            discrepancies = discrepancies
        )
    }

    /**
     * Calculates fused manipulation score using weighted fusion.
     */
    private fun calculateFusedScore(
        imageScore: Float,
        videoScore: Float,
        voiceScore: Float,
        crossModalConsistency: CrossModalConsistency,
        weights: AnalysisWeights
    ): Float {
        // Normalize weights
        val totalWeight = weights.imageWeight + weights.videoWeight + weights.voiceWeight
        val normalizedImageWeight = weights.imageWeight / totalWeight
        val normalizedVideoWeight = weights.videoWeight / totalWeight
        val normalizedVoiceWeight = weights.voiceWeight / totalWeight

        // Calculate weighted score
        var fusedScore = 0f
        var activeWeight = 0f

        if (imageScore > 0 || weights.imageWeight > 0) {
            fusedScore += imageScore * normalizedImageWeight
            activeWeight += normalizedImageWeight
        }
        if (videoScore > 0 || weights.videoWeight > 0) {
            fusedScore += videoScore * normalizedVideoWeight
            activeWeight += normalizedVideoWeight
        }
        if (voiceScore > 0 || weights.voiceWeight > 0) {
            fusedScore += voiceScore * normalizedVoiceWeight
            activeWeight += normalizedVoiceWeight
        }

        // Re-normalize if some modalities weren't analyzed
        fusedScore = if (activeWeight > 0) fusedScore / activeWeight else 0f

        // Boost score if cross-modal consistency shows discrepancies
        if (!crossModalConsistency.isConsistent) {
            fusedScore = (fusedScore + 0.1f).coerceAtMost(1.0f)
        }

        return fusedScore
    }

    /**
     * Generates detailed findings from all analysis results.
     */
    private fun generateFindings(
        imageResults: List<ImageForensicResult>?,
        videoResult: VideoForensicResult?,
        voiceResult: VoiceForensicResult?,
        crossModalConsistency: CrossModalConsistency
    ): List<DeepfakeFinding> {
        val findings = mutableListOf<DeepfakeFinding>()

        // Image findings
        imageResults?.forEachIndexed { index, result ->
            if (result.isTampered) {
                findings.add(
                    DeepfakeFinding(
                        category = FindingCategory.IMAGE_MANIPULATION,
                        severity = classifySeverity(result.tamperingScore),
                        description = "Image ${index + 1} shows signs of manipulation",
                        confidence = result.tamperingScore,
                        details = buildImageDetails(result)
                    )
                )
            }
        }

        // Video findings
        videoResult?.let { result ->
            if (result.isTampered) {
                findings.add(
                    DeepfakeFinding(
                        category = FindingCategory.VIDEO_MANIPULATION,
                        severity = classifySeverity(result.tamperingScore),
                        description = "Video shows signs of manipulation or editing",
                        confidence = result.tamperingScore,
                        details = buildVideoDetails(result)
                    )
                )
            }

            result.anomalies.forEach { anomaly ->
                findings.add(
                    DeepfakeFinding(
                        category = FindingCategory.VIDEO_ANOMALY,
                        severity = classifySeverity(anomaly.confidence),
                        description = anomaly.description,
                        confidence = anomaly.confidence,
                        details = listOf("Frames: ${anomaly.frameIndices.joinToString(", ")}")
                    )
                )
            }
        }

        // Voice findings
        voiceResult?.let { result ->
            if (result.isTampered) {
                findings.add(
                    DeepfakeFinding(
                        category = FindingCategory.AUDIO_MANIPULATION,
                        severity = classifySeverity(result.tamperingScore),
                        description = "Audio shows signs of manipulation or synthesis",
                        confidence = result.tamperingScore,
                        details = buildVoiceDetails(result)
                    )
                )
            }
        }

        // Cross-modal findings
        crossModalConsistency.discrepancies.forEach { discrepancy ->
            findings.add(
                DeepfakeFinding(
                    category = FindingCategory.CROSS_MODAL_INCONSISTENCY,
                    severity = FindingSeverity.HIGH,
                    description = discrepancy,
                    confidence = 1f - crossModalConsistency.consistencyScore,
                    details = emptyList()
                )
            )
        }

        return findings.sortedByDescending { it.confidence }
    }

    private fun buildImageDetails(result: ImageForensicResult): List<String> {
        val details = mutableListOf<String>()

        if (result.errorLevelAnalysis.suspiciousRegions > 0) {
            details.add("${result.errorLevelAnalysis.suspiciousRegions} suspicious regions detected via ELA")
        }
        if (!result.noiseAnalysis.isConsistent) {
            details.add("Inconsistent noise patterns detected")
        }
        if (result.exifMetadata?.wasEdited == true) {
            details.add("EXIF metadata indicates editing software was used")
        }

        return details
    }

    private fun buildVideoDetails(result: VideoForensicResult): List<String> {
        val details = mutableListOf<String>()

        if (!result.gopAnalysis.isStructureConsistent) {
            details.add("GOP structure inconsistencies detected")
        }
        if (result.temporalConsistency < 0.8f) {
            details.add("Temporal inconsistencies between frames")
        }
        if (result.reEncodingScore > 0.5f) {
            details.add("Signs of re-encoding detected")
        }

        return details
    }

    private fun buildVoiceDetails(result: VoiceForensicResult): List<String> {
        val details = mutableListOf<String>()

        if (result.spectralFeatures.flatness > 0.8) {
            details.add("Unusually flat spectrum - possible synthetic audio")
        }
        if (result.voiceActivitySegments.isEmpty()) {
            details.add("No clear voice activity detected")
        }

        return details
    }

    private fun classifySeverity(score: Float): FindingSeverity {
        return when {
            score > 0.8f -> FindingSeverity.CRITICAL
            score > 0.6f -> FindingSeverity.HIGH
            score > 0.4f -> FindingSeverity.MEDIUM
            else -> FindingSeverity.LOW
        }
    }

    /**
     * Classifies the overall manipulation likelihood.
     */
    private fun classifyManipulation(score: Float, findings: List<DeepfakeFinding>): ManipulationLikelihood {
        val criticalFindings = findings.count { it.severity == FindingSeverity.CRITICAL }
        val highFindings = findings.count { it.severity == FindingSeverity.HIGH }

        return when {
            score > 0.8f || criticalFindings >= 2 -> ManipulationLikelihood.VERY_HIGH
            score > 0.6f || criticalFindings >= 1 || highFindings >= 3 -> ManipulationLikelihood.HIGH
            score > 0.4f || highFindings >= 1 -> ManipulationLikelihood.MEDIUM
            score > 0.2f || findings.isNotEmpty() -> ManipulationLikelihood.LOW
            else -> ManipulationLikelihood.VERY_LOW
        }
    }

    /**
     * Generates recommendations based on findings.
     */
    private fun generateRecommendations(
        findings: List<DeepfakeFinding>,
        likelihood: ManipulationLikelihood
    ): List<String> {
        val recommendations = mutableListOf<String>()

        when (likelihood) {
            ManipulationLikelihood.VERY_HIGH, ManipulationLikelihood.HIGH -> {
                recommendations.add("Exercise extreme caution - high probability of manipulation detected")
                recommendations.add("Seek additional verification from independent sources")
                recommendations.add("Consider expert forensic analysis before using as evidence")
            }
            ManipulationLikelihood.MEDIUM -> {
                recommendations.add("Some manipulation indicators detected - verify authenticity")
                recommendations.add("Cross-reference with original sources if available")
            }
            ManipulationLikelihood.LOW, ManipulationLikelihood.VERY_LOW -> {
                recommendations.add("No significant manipulation indicators detected")
                recommendations.add("Standard verification procedures recommended")
            }
        }

        // Specific recommendations based on findings
        val hasImageIssues = findings.any { it.category == FindingCategory.IMAGE_MANIPULATION }
        val hasVideoIssues = findings.any { it.category == FindingCategory.VIDEO_MANIPULATION }
        val hasAudioIssues = findings.any { it.category == FindingCategory.AUDIO_MANIPULATION }

        if (hasImageIssues) {
            recommendations.add("Image-specific: Request original RAW files if available")
        }
        if (hasVideoIssues) {
            recommendations.add("Video-specific: Compare with other recordings of same event")
        }
        if (hasAudioIssues) {
            recommendations.add("Audio-specific: Verify speaker identity through other means")
        }

        return recommendations
    }
}

/**
 * Input content for deepfake analysis.
 */
data class MediaContent(
    val images: List<Bitmap>? = null,
    val videoFrames: List<Bitmap>? = null,
    val videoMetadata: VideoMetadata? = null,
    val audioSamples: FloatArray? = null,
    val analysisWeights: AnalysisWeights = AnalysisWeights()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MediaContent
        if (audioSamples != null) {
            if (other.audioSamples == null) return false
            if (!audioSamples.contentEquals(other.audioSamples)) return false
        } else if (other.audioSamples != null) return false
        return true
    }

    override fun hashCode(): Int {
        return audioSamples?.contentHashCode() ?: 0
    }
}

/**
 * Weights for different analysis modalities.
 */
data class AnalysisWeights(
    val imageWeight: Float = 1.0f,
    val videoWeight: Float = 1.0f,
    val voiceWeight: Float = 1.0f
)

/**
 * Complete deepfake analysis result.
 */
data class DeepfakeAnalysisResult(
    val overallScore: Float,
    val imageAnalysisScore: Float,
    val videoAnalysisScore: Float,
    val voiceAnalysisScore: Float,
    val crossModalConsistency: CrossModalConsistency,
    val manipulationLikelihood: ManipulationLikelihood,
    val findings: List<DeepfakeFinding>,
    val recommendations: List<String>
)

/**
 * Cross-modal consistency analysis.
 */
data class CrossModalConsistency(
    val isConsistent: Boolean,
    val consistencyScore: Float,
    val discrepancies: List<String>
)

/**
 * Individual deepfake finding.
 */
data class DeepfakeFinding(
    val category: FindingCategory,
    val severity: FindingSeverity,
    val description: String,
    val confidence: Float,
    val details: List<String>
)

/**
 * Finding categories.
 */
enum class FindingCategory {
    IMAGE_MANIPULATION,
    VIDEO_MANIPULATION,
    VIDEO_ANOMALY,
    AUDIO_MANIPULATION,
    CROSS_MODAL_INCONSISTENCY
}

/**
 * Finding severity levels.
 */
enum class FindingSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Manipulation likelihood classification.
 */
enum class ManipulationLikelihood {
    VERY_HIGH,
    HIGH,
    MEDIUM,
    LOW,
    VERY_LOW
}

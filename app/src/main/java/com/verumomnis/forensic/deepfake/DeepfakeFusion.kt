package com.verumomnis.forensic.deepfake

import com.verumomnis.forensic.image.ImageForgeryDetector
import com.verumomnis.forensic.voice.VoiceForensics

/**
 * Deepfake Fusion Engine - Combines visual and audio forensic signals
 * into a unified synthetic probability score.
 */
object DeepfakeFusion {

    data class DeepfakeResult(
        val suspicionScore: Float,
        val flags: List<String>
    )

    /**
     * Fuse image and audio forensic results into combined deepfake assessment.
     */
    fun fuse(
        image: ImageForgeryDetector.ImageForensicResult?,
        audio: VoiceForensics.VoiceReport?
    ): DeepfakeResult {
        val flags = mutableListOf<String>()
        var score = 0f
        var components = 0

        if (image != null) {
            score += (image.elaScore + image.noiseScore) / 2f
            components++
            flags.addAll(image.flags)
        }

        if (audio != null) {
            score += (audio.mfccScore + audio.spectralScore + audio.envelopeScore) / 3f
            components++
            flags.addAll(audio.flags)
        }

        // Average across available components
        if (components > 0) {
            score /= components
        }

        // Add fusion-level assessment
        if (score > 0.7f) {
            flags.add("CRITICAL: Very high deepfake probability (synthetic signature detected)")
        } else if (score > 0.5f) {
            flags.add("WARNING: Elevated deepfake indicators detected")
        } else if (score > 0.3f) {
            flags.add("NOTICE: Minor synthetic markers present - further review recommended")
        }

        return DeepfakeResult(
            suspicionScore = score.coerceIn(0f, 1f),
            flags = flags
        )
    }
}

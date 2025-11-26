package com.verumomnis.forensic.image

import android.graphics.Bitmap

/**
 * Image Forgery Detector - Combines all image forensic analysis modules.
 */
object ImageForgeryDetector {

    data class ImageForensicResult(
        val elaScore: Float,
        val noiseScore: Float,
        val exifAnomalies: List<String>,
        val flags: List<String>
    )

    /**
     * Perform comprehensive image forensic analysis.
     */
    fun analyse(bitmap: Bitmap, exif: Map<String, String>): ImageForensicResult {
        val elaScore = ELADetector.computeELA(bitmap)
        val noiseScore = NoiseMap.detectNoise(bitmap)
        val exifAnomalies = ExifScanner.scan(exif)

        val flags = mutableListOf<String>()

        // ELA score thresholds
        if (elaScore > 0.6f) {
            flags.add("High ELA score (${String.format("%.2f", elaScore)}): Strong tampering indicators")
        } else if (elaScore > 0.4f) {
            flags.add("Moderate ELA score (${String.format("%.2f", elaScore)}): Possible editing detected")
        }

        // Noise score thresholds
        if (noiseScore > 0.5f) {
            flags.add("High noise variance (${String.format("%.2f", noiseScore)}): Possible splice detected")
        } else if (noiseScore > 0.3f) {
            flags.add("Elevated noise levels: May indicate composite image")
        }

        // EXIF anomalies
        flags.addAll(exifAnomalies)

        return ImageForensicResult(
            elaScore = elaScore,
            noiseScore = noiseScore,
            exifAnomalies = exifAnomalies,
            flags = flags
        )
    }
}

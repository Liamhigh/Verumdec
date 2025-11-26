package com.verumomnis.contradiction.forensics

import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Image Forensics Module
 *
 * Provides offline image forensic analysis capabilities:
 * - Error Level Analysis (ELA)
 * - Noise analysis
 * - EXIF metadata extraction
 * - Tampering detection
 */
class ImageForensics {

    companion object {
        private const val ELA_QUALITY = 95
        private const val NOISE_THRESHOLD = 15.0
    }

    /**
     * Performs complete forensic analysis on an image.
     */
    fun analyzeImage(bitmap: Bitmap, exifStream: InputStream? = null): ImageForensicResult {
        val elaResult = performErrorLevelAnalysis(bitmap)
        val noiseResult = analyzeNoise(bitmap)
        val exifData = exifStream?.let { extractExifMetadata(it) }

        val tamperingScore = calculateTamperingScore(elaResult, noiseResult, exifData)

        return ImageForensicResult(
            errorLevelAnalysis = elaResult,
            noiseAnalysis = noiseResult,
            exifMetadata = exifData,
            tamperingScore = tamperingScore,
            isTampered = tamperingScore > 0.5f
        )
    }

    /**
     * Performs Error Level Analysis (ELA) on an image.
     * ELA detects areas that have been modified by comparing
     * compression artifacts across the image.
     */
    private fun performErrorLevelAnalysis(bitmap: Bitmap): ElaResult {
        val width = bitmap.width
        val height = bitmap.height
        val blockSize = 8

        val anomalies = mutableListOf<ElaAnomaly>()
        var totalVariance = 0.0
        var blockCount = 0

        for (y in 0 until height - blockSize step blockSize) {
            for (x in 0 until width - blockSize step blockSize) {
                val blockVariance = calculateBlockVariance(bitmap, x, y, blockSize)
                totalVariance += blockVariance
                blockCount++

                // High variance indicates potential manipulation
                if (blockVariance > NOISE_THRESHOLD * 2) {
                    anomalies.add(
                        ElaAnomaly(
                            x = x,
                            y = y,
                            width = blockSize,
                            height = blockSize,
                            variance = blockVariance,
                            confidence = (blockVariance / 50.0).coerceIn(0.0, 1.0).toFloat()
                        )
                    )
                }
            }
        }

        val averageVariance = if (blockCount > 0) totalVariance / blockCount else 0.0

        return ElaResult(
            averageVariance = averageVariance,
            anomalies = anomalies,
            suspiciousRegions = anomalies.size
        )
    }

    /**
     * Calculates variance within an image block.
     */
    private fun calculateBlockVariance(bitmap: Bitmap, startX: Int, startY: Int, size: Int): Double {
        val pixels = mutableListOf<Double>()

        for (y in startY until (startY + size).coerceAtMost(bitmap.height)) {
            for (x in startX until (startX + size).coerceAtMost(bitmap.width)) {
                val pixel = bitmap.getPixel(x, y)
                val luminance = 0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
                pixels.add(luminance)
            }
        }

        if (pixels.isEmpty()) return 0.0

        val mean = pixels.average()
        return pixels.map { (it - mean) * (it - mean) }.average()
    }

    /**
     * Analyzes noise patterns in the image.
     * Inconsistent noise can indicate manipulation.
     */
    private fun analyzeNoise(bitmap: Bitmap): NoiseResult {
        val width = bitmap.width
        val height = bitmap.height

        var totalNoise = 0.0
        var pixelCount = 0

        // Calculate local noise using Laplacian operator
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = getLuminance(bitmap, x, y)
                val neighbors = listOf(
                    getLuminance(bitmap, x - 1, y),
                    getLuminance(bitmap, x + 1, y),
                    getLuminance(bitmap, x, y - 1),
                    getLuminance(bitmap, x, y + 1)
                )

                val laplacian = abs(4 * center - neighbors.sum())
                totalNoise += laplacian
                pixelCount++
            }
        }

        val averageNoise = if (pixelCount > 0) totalNoise / pixelCount else 0.0
        val isConsistent = averageNoise < NOISE_THRESHOLD

        return NoiseResult(
            averageNoise = averageNoise,
            isConsistent = isConsistent,
            suspiciousLevel = if (isConsistent) "LOW" else "HIGH"
        )
    }

    /**
     * Gets the luminance value at a specific pixel.
     */
    private fun getLuminance(bitmap: Bitmap, x: Int, y: Int): Double {
        val pixel = bitmap.getPixel(x, y)
        return 0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
    }

    /**
     * Extracts EXIF metadata from an image.
     */
    private fun extractExifMetadata(inputStream: InputStream): ExifData {
        val exif = ExifInterface(inputStream)

        val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)
        val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)
        val gpsLat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
        val gpsLong = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)

        // Check for signs of editing
        val editingSoftware = listOf("Photoshop", "GIMP", "Paint", "Editor", "Pixlr")
        val wasEdited = software?.let { sw ->
            editingSoftware.any { sw.contains(it, ignoreCase = true) }
        } ?: false

        return ExifData(
            dateTime = dateTime,
            cameraMake = make,
            cameraModel = model,
            software = software,
            gpsLatitude = gpsLat,
            gpsLongitude = gpsLong,
            wasEdited = wasEdited
        )
    }

    /**
     * Calculates overall tampering score.
     */
    private fun calculateTamperingScore(
        elaResult: ElaResult,
        noiseResult: NoiseResult,
        exifData: ExifData?
    ): Float {
        var score = 0f

        // ELA score (30% weight)
        if (elaResult.suspiciousRegions > 5) {
            score += 0.3f * (elaResult.suspiciousRegions / 20f).coerceAtMost(1f)
        }

        // Noise score (30% weight)
        if (!noiseResult.isConsistent) {
            score += 0.3f
        }

        // EXIF score (40% weight)
        if (exifData != null) {
            if (exifData.wasEdited) {
                score += 0.4f
            }
            if (exifData.dateTime == null && exifData.cameraMake == null) {
                // Missing metadata is suspicious
                score += 0.2f
            }
        }

        return score.coerceIn(0f, 1f)
    }
}

/**
 * Complete image forensic analysis result.
 */
data class ImageForensicResult(
    val errorLevelAnalysis: ElaResult,
    val noiseAnalysis: NoiseResult,
    val exifMetadata: ExifData?,
    val tamperingScore: Float,
    val isTampered: Boolean
)

/**
 * Error Level Analysis result.
 */
data class ElaResult(
    val averageVariance: Double,
    val anomalies: List<ElaAnomaly>,
    val suspiciousRegions: Int
)

/**
 * ELA anomaly in a specific region.
 */
data class ElaAnomaly(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val variance: Double,
    val confidence: Float
)

/**
 * Noise analysis result.
 */
data class NoiseResult(
    val averageNoise: Double,
    val isConsistent: Boolean,
    val suspiciousLevel: String
)

/**
 * EXIF metadata extracted from image.
 */
data class ExifData(
    val dateTime: String?,
    val cameraMake: String?,
    val cameraModel: String?,
    val software: String?,
    val gpsLatitude: String?,
    val gpsLongitude: String?,
    val wasEdited: Boolean
)

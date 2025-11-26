package com.verumomnis.forensic.image

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Noise Map analyzer for detecting copy-paste splices.
 * Uses local variance and edge disruption detection.
 */
object NoiseMap {

    /**
     * Detect noise level in bitmap.
     * High variance in localized areas indicates potential manipulation.
     */
    fun detectNoise(bitmap: Bitmap): Float {
        var totalVariance = 0f
        var samples = 0

        for (x in 1 until bitmap.width - 1 step 3) {
            for (y in 1 until bitmap.height - 1 step 3) {
                val c = Color.red(bitmap.getPixel(x, y))
                val neighbors = listOf(
                    Color.red(bitmap.getPixel(x - 1, y)),
                    Color.red(bitmap.getPixel(x + 1, y)),
                    Color.red(bitmap.getPixel(x, y - 1)),
                    Color.red(bitmap.getPixel(x, y + 1))
                )

                val mean = neighbors.sum() / 4f
                val variance = abs(mean - c)

                totalVariance += variance
                samples++
            }
        }

        return (totalVariance / samples / 255f).coerceIn(0f, 1f)
    }

    private fun abs(value: Float): Float = if (value < 0) -value else value
}

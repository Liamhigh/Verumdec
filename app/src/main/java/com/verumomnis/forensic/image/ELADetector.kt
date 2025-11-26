package com.verumomnis.forensic.image

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

/**
 * Error Level Analysis (ELA) detector.
 * Detects JPEG compression inconsistencies that indicate image manipulation.
 */
object ELADetector {

    /**
     * Compute ELA score for a bitmap.
     * Higher scores indicate potential tampering.
     */
    fun computeELA(bitmap: Bitmap): Float {
        var totalDiff = 0f
        var samples = 0

        // Sample the image at regular intervals
        for (x in 0 until bitmap.width step 4) {
            for (y in 0 until bitmap.height step 4) {
                val pixel = bitmap.getPixel(x, y)
                
                // Get neighboring pixels for comparison
                val neighbors = getNeighborPixels(bitmap, x, y)
                if (neighbors.isNotEmpty()) {
                    val avgR = neighbors.map { Color.red(it) }.average().toInt()
                    val avgG = neighbors.map { Color.green(it) }.average().toInt()
                    val avgB = neighbors.map { Color.blue(it) }.average().toInt()

                    val diff = (
                        abs(Color.red(pixel) - avgR) +
                        abs(Color.green(pixel) - avgG) +
                        abs(Color.blue(pixel) - avgB)
                    ) / 3f

                    totalDiff += diff
                    samples++
                }
            }
        }

        return if (samples > 0) {
            (totalDiff / samples / 255f).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    private fun getNeighborPixels(bitmap: Bitmap, x: Int, y: Int): List<Int> {
        val neighbors = mutableListOf<Int>()
        
        if (x > 0) neighbors.add(bitmap.getPixel(x - 1, y))
        if (x < bitmap.width - 1) neighbors.add(bitmap.getPixel(x + 1, y))
        if (y > 0) neighbors.add(bitmap.getPixel(x, y - 1))
        if (y < bitmap.height - 1) neighbors.add(bitmap.getPixel(x, y + 1))
        
        return neighbors
    }
}

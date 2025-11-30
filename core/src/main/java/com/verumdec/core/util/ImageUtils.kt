package com.verumdec.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class for image operations.
 */
object ImageUtils {

    /**
     * Supported image extensions.
     */
    val SUPPORTED_EXTENSIONS = listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    /**
     * Check if file is a supported image format.
     */
    fun isImageFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in SUPPORTED_EXTENSIONS
    }

    /**
     * Image metadata container.
     */
    data class ImageMetadata(
        val width: Int = 0,
        val height: Int = 0,
        val mimeType: String = "",
        val orientation: Int = 0,
        val hasAlpha: Boolean = false
    )

    /**
     * Get image dimensions without loading full bitmap.
     */
    fun getImageDimensions(file: File): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load bitmap with sample size for memory efficiency.
     */
    fun loadScaledBitmap(file: File, maxWidth: Int, maxHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(file.absolutePath, options)

            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false
            
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculate sample size for efficient bitmap loading.
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Resize bitmap to fit within bounds while maintaining aspect ratio.
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight
        
        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    /**
     * Rotate bitmap.
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Save bitmap to file.
     */
    fun saveBitmap(bitmap: Bitmap, file: File, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 90): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(format, quality, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get format label for resolution.
     */
    fun getResolutionLabel(width: Int, height: Int): String {
        val megapixels = (width * height) / 1_000_000.0
        return when {
            megapixels >= 12 -> "High Resolution (${String.format("%.1f", megapixels)} MP)"
            megapixels >= 5 -> "Medium Resolution (${String.format("%.1f", megapixels)} MP)"
            megapixels >= 2 -> "Standard Resolution (${String.format("%.1f", megapixels)} MP)"
            else -> "Low Resolution (${String.format("%.2f", megapixels)} MP)"
        }
    }

    /**
     * Check if image is likely a screenshot.
     */
    fun isLikelyScreenshot(fileName: String, width: Int, height: Int): Boolean {
        val name = fileName.lowercase()
        if (name.contains("screenshot") || name.contains("screen_shot") || name.contains("screen-shot")) {
            return true
        }
        // Common phone screen aspect ratios
        val ratio = width.toFloat() / height.toFloat()
        val phoneRatios = listOf(0.46f, 0.5f, 0.56f, 1.78f, 2.0f, 2.17f)
        return phoneRatios.any { kotlin.math.abs(ratio - it) < 0.05f }
    }
}

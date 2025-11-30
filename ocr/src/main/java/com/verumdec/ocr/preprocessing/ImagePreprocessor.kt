package com.verumdec.ocr.preprocessing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * ImagePreprocessor - Prepares images for better OCR results.
 */
class ImagePreprocessor {

    /**
     * Preprocess image for OCR.
     */
    fun preprocess(bitmap: Bitmap): Bitmap {
        var processed = bitmap
        processed = toGrayscale(processed)
        processed = increaseContrast(processed, 1.5f)
        return processed
    }

    /**
     * Convert to grayscale.
     */
    fun toGrayscale(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    /**
     * Increase contrast.
     */
    fun increaseContrast(source: Bitmap, contrast: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val colorMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    /**
     * Scale image to optimal OCR size.
     */
    fun scaleForOcr(source: Bitmap, maxDimension: Int = 2048): Bitmap {
        val ratio = maxDimension.toFloat() / maxOf(source.width, source.height)
        if (ratio >= 1f) return source
        
        val newWidth = (source.width * ratio).toInt()
        val newHeight = (source.height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    /**
     * Binarize image (black and white only).
     */
    fun binarize(source: Bitmap, threshold: Int = 128): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val pixel = source.getPixel(x, y)
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                val gray = (red + green + blue) / 3
                
                val newPixel = if (gray > threshold) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                result.setPixel(x, y, newPixel)
            }
        }
        
        return result
    }
}

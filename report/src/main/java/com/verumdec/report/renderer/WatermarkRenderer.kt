package com.verumdec.report.renderer

import android.graphics.*

/**
 * WatermarkRenderer - Renders central watermark on PDF pages.
 * Implements:
 * - Central faint watermark at 12-16% opacity
 * - Verum Omnis branding
 * - Diagonal placement for security
 */
class WatermarkRenderer {

    companion object {
        private const val WATERMARK_OPACITY = 38 // ~15% of 255
        private const val WATERMARK_TEXT = "VERUM OMNIS"
        private const val WATERMARK_SIZE = 48f
        private const val ROTATION_ANGLE = -30f
    }

    /**
     * Render watermark on canvas.
     */
    fun renderWatermark(canvas: Canvas, pageWidth: Int, pageHeight: Int) {
        val centerX = pageWidth / 2f
        val centerY = pageHeight / 2f

        canvas.save()

        // Rotate canvas for diagonal watermark
        canvas.rotate(ROTATION_ANGLE, centerX, centerY)

        // Main watermark text
        val mainPaint = Paint().apply {
            color = Color.argb(WATERMARK_OPACITY, 26, 35, 126) // Primary color with opacity
            textSize = WATERMARK_SIZE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
            letterSpacing = 0.2f
        }
        canvas.drawText(WATERMARK_TEXT, centerX, centerY, mainPaint)

        // Secondary text below
        val subPaint = Paint().apply {
            color = Color.argb(WATERMARK_OPACITY - 10, 26, 35, 126)
            textSize = 16f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Contradiction Engine", centerX, centerY + 30f, subPaint)

        // Draw shield outline
        val shieldPaint = Paint().apply {
            color = Color.argb(WATERMARK_OPACITY - 10, 26, 35, 126)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        val shieldPath = Path().apply {
            val size = 80f
            moveTo(centerX - size/2, centerY - 100f)
            lineTo(centerX + size/2, centerY - 100f)
            lineTo(centerX + size/2, centerY - 50f)
            lineTo(centerX, centerY - 30f)
            lineTo(centerX - size/2, centerY - 50f)
            close()
        }
        canvas.drawPath(shieldPath, shieldPaint)

        // Draw "V" in shield
        val vPaint = Paint().apply {
            color = Color.argb(WATERMARK_OPACITY, 26, 35, 126)
            textSize = 32f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("V", centerX, centerY - 55f, vPaint)

        canvas.restore()

        // Add corner watermarks (subtle)
        renderCornerWatermarks(canvas, pageWidth, pageHeight)
    }

    /**
     * Render subtle corner watermarks.
     */
    private fun renderCornerWatermarks(canvas: Canvas, pageWidth: Int, pageHeight: Int) {
        val cornerPaint = Paint().apply {
            color = Color.argb(20, 26, 35, 126) // Very faint
            textSize = 8f
            isAntiAlias = true
        }

        // Top-left
        canvas.drawText("VERUM", 30f, 30f, cornerPaint)

        // Top-right
        cornerPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("OMNIS", pageWidth - 30f, 30f, cornerPaint)

        // Bottom-left
        cornerPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("SEALED", 30f, pageHeight - 20f, cornerPaint)

        // Bottom-right - will be covered by QR code area, skip
    }

    /**
     * Render confidential stamp (optional).
     */
    fun renderConfidentialStamp(canvas: Canvas, pageWidth: Int, pageHeight: Int) {
        val centerX = pageWidth / 2f
        
        canvas.save()
        canvas.rotate(-15f, centerX, 100f)

        // Draw border
        val borderPaint = Paint().apply {
            color = Color.argb(100, 211, 47, 47)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        val rect = RectF(centerX - 80f, 70f, centerX + 80f, 110f)
        canvas.drawRoundRect(rect, 5f, 5f, borderPaint)

        // Draw text
        val textPaint = Paint().apply {
            color = Color.argb(100, 211, 47, 47)
            textSize = 14f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
            letterSpacing = 0.3f
        }
        canvas.drawText("CONFIDENTIAL", centerX, 95f, textPaint)

        canvas.restore()
    }
}

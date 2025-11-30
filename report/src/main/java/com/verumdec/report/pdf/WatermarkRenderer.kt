package com.verumdec.report.pdf

import android.graphics.Bitmap
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState

/**
 * Renderer for watermarks on PDF pages.
 * 
 * Applies a faint watermark image centered on each page with configurable opacity.
 * The watermark is drawn behind the main content (12-16% opacity) for subtle branding
 * while maintaining document readability.
 */
class WatermarkRenderer {

    companion object {
        /** Default watermark opacity (14% - middle of 12-16% range) */
        const val DEFAULT_OPACITY = 0.14f

        /** Minimum allowed opacity (12%) */
        const val MIN_OPACITY = 0.12f

        /** Maximum allowed opacity (16%) */
        const val MAX_OPACITY = 0.16f
    }

    /**
     * Apply a watermark to all pages in a PDF document.
     *
     * @param document The PDF document to watermark
     * @param watermarkBitmap The watermark image bitmap
     * @param opacity The opacity level (0.0 to 1.0, default 0.14)
     */
    fun applyWatermarkToAllPages(
        document: PDDocument,
        watermarkBitmap: Bitmap,
        opacity: Float = DEFAULT_OPACITY
    ) {
        val validOpacity = opacity.coerceIn(0f, 1f)
        val pdImage = LosslessFactory.createFromImage(document, watermarkBitmap)

        for (page in document.pages) {
            applyWatermarkToPage(document, page, pdImage, validOpacity)
        }
    }

    /**
     * Apply a watermark to a single PDF page.
     *
     * @param document The PDF document
     * @param page The page to apply the watermark to
     * @param watermarkBitmap The watermark image bitmap
     * @param opacity The opacity level (0.0 to 1.0)
     */
    fun applyWatermarkToPage(
        document: PDDocument,
        page: PDPage,
        watermarkBitmap: Bitmap,
        opacity: Float = DEFAULT_OPACITY
    ) {
        val pdImage = LosslessFactory.createFromImage(document, watermarkBitmap)
        applyWatermarkToPage(document, page, pdImage, opacity)
    }

    /**
     * Internal method to apply watermark using PDImageXObject.
     */
    private fun applyWatermarkToPage(
        document: PDDocument,
        page: PDPage,
        pdImage: com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject,
        opacity: Float
    ) {
        val pageWidth = page.mediaBox.width
        val pageHeight = page.mediaBox.height

        // Calculate centered position with scaling to fit nicely on page
        val maxWatermarkWidth = pageWidth * 0.6f
        val maxWatermarkHeight = pageHeight * 0.4f

        val imageWidth = pdImage.width.toFloat()
        val imageHeight = pdImage.height.toFloat()

        val scaleX = maxWatermarkWidth / imageWidth
        val scaleY = maxWatermarkHeight / imageHeight
        val scale = minOf(scaleX, scaleY, 1f) // Don't upscale

        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale

        val x = (pageWidth - scaledWidth) / 2
        val y = (pageHeight - scaledHeight) / 2

        // Create content stream that prepends to existing content (draws behind)
        PDPageContentStream(
            document,
            page,
            PDPageContentStream.AppendMode.PREPEND,
            true,
            true
        ).use { contentStream ->
            // Set transparency
            val graphicsState = PDExtendedGraphicsState()
            graphicsState.nonStrokingAlphaConstant = opacity
            graphicsState.strokingAlphaConstant = opacity
            contentStream.setGraphicsStateParameters(graphicsState)

            // Draw watermark centered on page
            contentStream.drawImage(pdImage, x, y, scaledWidth, scaledHeight)
        }
    }

    /**
     * Create a text-based watermark bitmap.
     *
     * @param text The text to use as watermark (e.g., "VERUM OMNIS")
     * @param width The width of the resulting bitmap
     * @param height The height of the resulting bitmap
     * @param textSize The text size in pixels
     * @return Bitmap containing the text watermark
     */
    fun createTextWatermark(
        text: String,
        width: Int = 400,
        height: Int = 200,
        textSize: Float = 48f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            this.textSize = textSize
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
            )
        }

        // Draw text centered with slight rotation for watermark effect
        canvas.save()
        canvas.rotate(-30f, width / 2f, height / 2f)
        canvas.drawText(text, width / 2f, height / 2f + textSize / 3, paint)
        canvas.restore()

        return bitmap
    }
}

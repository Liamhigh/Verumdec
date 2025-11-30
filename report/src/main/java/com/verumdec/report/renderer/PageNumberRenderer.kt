package com.verumdec.report.renderer

import android.graphics.*

/**
 * PageNumberRenderer - Renders page numbers and pagination info.
 */
class PageNumberRenderer {

    /**
     * Render page number at bottom of page.
     */
    fun renderPageNumber(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        currentPage: Int,
        totalPages: Int,
        style: PageNumberStyle = PageNumberStyle.CENTERED
    ) {
        val paint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            isAntiAlias = true
        }

        val text = "Page $currentPage of $totalPages"
        val y = pageHeight - 25f

        when (style) {
            PageNumberStyle.CENTERED -> {
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(text, pageWidth / 2, y, paint)
            }
            PageNumberStyle.LEFT -> {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(text, 50f, y, paint)
            }
            PageNumberStyle.RIGHT -> {
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(text, pageWidth - 50f, y, paint)
            }
            PageNumberStyle.ALTERNATING -> {
                if (currentPage % 2 == 0) {
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText(text, 50f, y, paint)
                } else {
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(text, pageWidth - 50f, y, paint)
                }
            }
        }
    }

    /**
     * Render section indicator.
     */
    fun renderSectionIndicator(
        canvas: Canvas,
        pageWidth: Float,
        sectionName: String,
        sectionNumber: Int
    ) {
        val paint = Paint().apply {
            color = Color.parseColor("#757575")
            textSize = 8f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        canvas.drawText("Section $sectionNumber: $sectionName", pageWidth - 50f, 25f, paint)
    }

    /**
     * Render document ID marker.
     */
    fun renderDocumentId(
        canvas: Canvas,
        pageHeight: Float,
        documentId: String
    ) {
        val paint = Paint().apply {
            color = Color.parseColor("#BDBDBD")
            textSize = 6f
            isAntiAlias = true
        }

        canvas.drawText("Doc ID: $documentId", 50f, pageHeight - 10f, paint)
    }

    /**
     * Render continuation indicator for multi-page sections.
     */
    fun renderContinuation(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        isContinued: Boolean,
        continuesOnNext: Boolean
    ) {
        val paint = Paint().apply {
            color = Color.parseColor("#9E9E9E")
            textSize = 7f
            isAntiAlias = true
            fontStyle = Typeface.ITALIC
        }

        if (isContinued) {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("(continued from previous page)", 50f, 95f, paint)
        }

        if (continuesOnNext) {
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("(continues on next page)", pageWidth - 50f, pageHeight - 70f, paint)
        }
    }
}

enum class PageNumberStyle {
    CENTERED,
    LEFT,
    RIGHT,
    ALTERNATING
}

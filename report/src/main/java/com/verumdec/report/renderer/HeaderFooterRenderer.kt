package com.verumdec.report.renderer

import android.graphics.*
import java.text.SimpleDateFormat
import java.util.*
import com.verumdec.report.generator.QRCodeGenerator

/**
 * HeaderFooterRenderer - Renders headers and footers for PDF pages.
 * Implements:
 * - Top-center Verum Omnis logo with 3D effect
 * - Case information in header
 * - Page numbers
 * - Footer with patent pending notice
 * - QR code with SHA-512 hash
 */
class HeaderFooterRenderer {

    companion object {
        private const val LOGO_SIZE = 40f
        private const val QR_SIZE = 50
        private const val FOOTER_OFFSET = 40f
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)

    /**
     * Render header with Verum Omnis logo.
     */
    fun renderHeader(canvas: Canvas, pageWidth: Float, caseName: String, caseId: String) {
        val centerX = pageWidth / 2

        // Draw 3D-style logo background (shield shape simulation)
        val logoPaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val logoShadowPaint = Paint().apply {
            color = Color.parseColor("#0D1B4A")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Draw shadow for 3D effect
        val shadowPath = Path().apply {
            moveTo(centerX - LOGO_SIZE/2 + 3, 15f + 3)
            lineTo(centerX + LOGO_SIZE/2 + 3, 15f + 3)
            lineTo(centerX + LOGO_SIZE/2 + 3, 45f + 3)
            lineTo(centerX + 3, 55f + 3)
            lineTo(centerX - LOGO_SIZE/2 + 3, 45f + 3)
            close()
        }
        canvas.drawPath(shadowPath, logoShadowPaint)

        // Draw main shield shape
        val shieldPath = Path().apply {
            moveTo(centerX - LOGO_SIZE/2, 15f)
            lineTo(centerX + LOGO_SIZE/2, 15f)
            lineTo(centerX + LOGO_SIZE/2, 45f)
            lineTo(centerX, 55f)
            lineTo(centerX - LOGO_SIZE/2, 45f)
            close()
        }
        canvas.drawPath(shieldPath, logoPaint)

        // Draw "V" letter in shield
        val letterPaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("V", centerX, 42f, letterPaint)

        // Draw "VERUM OMNIS" text below logo
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            textSize = 12f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
            letterSpacing = 0.1f
        }
        canvas.drawText("VERUM OMNIS", centerX, 70f, titlePaint)

        // Draw case name on left
        val casePaint = Paint().apply {
            color = Color.parseColor("#424242")
            textSize = 9f
            isAntiAlias = true
        }
        canvas.drawText("Case: $caseName", 50f, 35f, casePaint)
        canvas.drawText("ID: $caseId", 50f, 48f, casePaint)

        // Draw line separator
        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }
        canvas.drawLine(50f, 80f, pageWidth - 50f, 80f, linePaint)
    }

    /**
     * Render footer with page number, timestamp, QR code, and patent notice.
     */
    fun renderFooter(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        currentPage: Int,
        totalPages: Int,
        sha512Hash: String,
        generatedAt: Date,
        qrCodeGenerator: QRCodeGenerator
    ) {
        val footerY = pageHeight - FOOTER_OFFSET

        // Draw line separator
        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }
        canvas.drawLine(50f, footerY - 20f, pageWidth - 50f, footerY - 20f, linePaint)

        // Draw page number on left
        val pageNumPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            isAntiAlias = true
        }
        canvas.drawText("Page $currentPage of $totalPages", 50f, footerY, pageNumPaint)

        // Draw timestamp in center
        val timestampPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Generated: ${dateFormat.format(generatedAt)}", pageWidth / 2, footerY, timestampPaint)

        // Draw QR code on right
        val qrContent = sha512Hash.take(32) // Truncated SHA-512
        val qrBitmap = qrCodeGenerator.generateQRCode(qrContent, QR_SIZE)
        if (qrBitmap != null) {
            val qrX = pageWidth - 50f - QR_SIZE
            val qrY = footerY - 45f
            canvas.drawBitmap(qrBitmap, qrX, qrY, null)
        }

        // Draw patent notice
        val patentPaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            textSize = 7f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("✔ Patent Pending Verum Omnis", pageWidth / 2, footerY + 12f, patentPaint)
    }
}

package com.verumdec.report.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QR Code Generator for PDF sealing.
 * 
 * Generates QR codes containing SHA-512 hash and case ID for document verification.
 * Uses ZXing library for QR code generation with high error correction level
 * to ensure reliability even with partial damage to the printed document.
 */
object QRCodeGenerator {

    private const val DEFAULT_QR_SIZE = 150
    private const val DEFAULT_MARGIN = 1

    /**
     * Generate a QR code bitmap containing the SHA-512 hash and case ID.
     *
     * @param sha512Hash The SHA-512 hash of the sealed document
     * @param caseId The case identifier
     * @param size The size of the QR code in pixels (default: 150)
     * @return Bitmap containing the QR code, or null if generation fails
     */
    fun generateVerificationQR(
        sha512Hash: String,
        caseId: String,
        size: Int = DEFAULT_QR_SIZE
    ): Bitmap? {
        return try {
            val content = buildVerificationContent(sha512Hash, caseId)
            generateQRCode(content, size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate a QR code from any string content.
     *
     * @param content The string content to encode
     * @param size The size of the QR code in pixels
     * @return Bitmap containing the QR code, or null if generation fails
     */
    fun generateQRCode(content: String, size: Int = DEFAULT_QR_SIZE): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to DEFAULT_MARGIN,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Build the verification content string for the QR code.
     * Format: VERUM|{caseId}|{sha512Hash}
     *
     * @param sha512Hash The SHA-512 hash
     * @param caseId The case identifier
     * @return Formatted verification string
     */
    private fun buildVerificationContent(sha512Hash: String, caseId: String): String {
        return "VERUM|$caseId|$sha512Hash"
    }

    /**
     * Parse verification content from a scanned QR code.
     *
     * @param content The scanned QR code content
     * @return Pair of (caseId, sha512Hash) or null if parsing fails
     */
    fun parseVerificationContent(content: String): Pair<String, String>? {
        return try {
            val parts = content.split("|")
            if (parts.size == 3 && parts[0] == "VERUM") {
                Pair(parts[1], parts[2])
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

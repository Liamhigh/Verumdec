package com.verumdec.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QR Code Generator for Verumdec
 * 
 * Generates QR codes for forensic reports to enable quick verification.
 * Integrated from take2 repository.
 */
object QRCodeGenerator {
    
    /**
     * Generates a QR code bitmap from the given content
     * 
     * @param content The text content to encode in the QR code
     * @param size The size of the QR code in pixels (width and height)
     * @param errorCorrection The error correction level (default: HIGH for forensic use)
     * @return Bitmap of the QR code
     */
    fun generateQRCode(
        content: String,
        size: Int = 512,
        errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.H
    ): Bitmap {
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, errorCorrection)
            put(EncodeHintType.MARGIN, 1)
        }
        
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
    
    /**
     * Generates a QR code for a forensic report hash
     * 
     * @param hash The SHA-512 hash of the report
     * @param caseId The case identifier
     * @param size The size of the QR code in pixels
     * @return Bitmap of the QR code
     */
    fun generateForensicQRCode(
        hash: String,
        caseId: String,
        size: Int = 512
    ): Bitmap {
        val content = buildString {
            appendLine("VERUM OMNIS FORENSIC SEAL")
            appendLine("Case ID: $caseId")
            appendLine("SHA-512: $hash")
            appendLine("Verify at: verum-omnis.org/verify")
        }
        return generateQRCode(content, size, ErrorCorrectionLevel.H)
    }
}

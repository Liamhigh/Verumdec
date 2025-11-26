package com.verumomnis.forensic.pdf

import android.content.Context
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PDF Seal Engine - Creates SHA-512 watermarked forensic reports.
 * This is the core document sealing engine of Verum Omnis.
 */
object PdfSealEngine {

    private const val MARGIN = 50f
    private const val LINE_HEIGHT = 14f

    /**
     * Create a sealed PDF report from text content.
     */
    fun createSealedPdf(context: Context, text: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(context.filesDir, "VO_Sealed_Report_$timestamp.pdf")

        val document = PDDocument()
        
        try {
            // Add content pages
            addContentPages(document, text)
            
            // Calculate hash of content before watermarking
            val baos = ByteArrayOutputStream()
            document.save(baos)
            val contentHash = HashUtil.sha512(baos.toByteArray())

            // Add watermark to all pages
            document.pages.forEach { page ->
                addWatermarkToPage(document, page)
            }

            // Add footer with hash to first page
            val hashFooter = "SHA-512: ${contentHash.take(32)}... | Sealed: $timestamp | Patent Pending Verum Omnis"
            PdfFooter.add(document, hashFooter)

            // Save final document
            document.save(outputFile)
        } finally {
            document.close()
        }

        return outputFile
    }

    private fun addContentPages(document: PDDocument, text: String) {
        val lines = wrapText(text, 80)
        val linesPerPage = 45
        
        var currentLine = 0
        while (currentLine < lines.size) {
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            
            val cs = PDPageContentStream(document, page)
            cs.beginText()
            cs.setFont(PDType1Font.HELVETICA, 11f)
            cs.setLeading(LINE_HEIGHT)
            cs.newLineAtOffset(MARGIN, page.mediaBox.height - MARGIN)
            
            // Add header on first page
            if (currentLine == 0) {
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16f)
                cs.showText("VERUM OMNIS FORENSIC REPORT")
                cs.newLine()
                cs.newLine()
                cs.setFont(PDType1Font.HELVETICA, 11f)
            }
            
            // Add content lines
            var linesOnPage = 0
            while (currentLine < lines.size && linesOnPage < linesPerPage) {
                cs.showText(lines[currentLine])
                cs.newLine()
                currentLine++
                linesOnPage++
            }
            
            cs.endText()
            cs.close()
        }
        
        // Ensure at least one page exists
        if (document.numberOfPages == 0) {
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            
            val cs = PDPageContentStream(document, page)
            cs.beginText()
            cs.setFont(PDType1Font.HELVETICA_BOLD, 16f)
            cs.newLineAtOffset(MARGIN, page.mediaBox.height - MARGIN)
            cs.showText("VERUM OMNIS FORENSIC REPORT")
            cs.newLine()
            cs.newLine()
            cs.setFont(PDType1Font.HELVETICA, 11f)
            cs.showText("No content provided.")
            cs.endText()
            cs.close()
        }
    }

    private fun addWatermarkToPage(document: PDDocument, page: PDPage) {
        val cs = PDPageContentStream(
            document, 
            page, 
            PDPageContentStream.AppendMode.APPEND, 
            true,
            true
        )

        cs.beginText()
        cs.setFont(PDType1Font.HELVETICA_BOLD, 50f)
        cs.setNonStrokingColor(230, 230, 230)
        
        val pageWidth = page.mediaBox.width
        val pageHeight = page.mediaBox.height
        
        // Simple diagonal watermark
        cs.newLineAtOffset(pageWidth / 6, pageHeight / 3)
        cs.showText("VERUM OMNIS")
        
        cs.endText()
        cs.close()
    }

    private fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        val result = mutableListOf<String>()
        
        text.split("\n").forEach { paragraph ->
            if (paragraph.isEmpty()) {
                result.add("")
            } else {
                var remaining = paragraph
                while (remaining.length > maxCharsPerLine) {
                    val breakPoint = remaining.lastIndexOf(' ', maxCharsPerLine)
                    if (breakPoint > 0) {
                        result.add(remaining.substring(0, breakPoint))
                        remaining = remaining.substring(breakPoint + 1)
                    } else {
                        result.add(remaining.substring(0, maxCharsPerLine))
                        remaining = remaining.substring(maxCharsPerLine)
                    }
                }
                if (remaining.isNotEmpty()) {
                    result.add(remaining)
                }
            }
        }
        
        return result
    }
}

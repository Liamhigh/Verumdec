package com.verumomnis.forensic.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font

/**
 * Adds VERUM OMNIS watermark to PDF documents.
 */
object PdfWatermark {

    fun add(document: PDDocument, text: String) {
        if (document.numberOfPages == 0) return
        
        val page = document.pages[0]
        val cs = PDPageContentStream(
            document, 
            page, 
            PDPageContentStream.AppendMode.APPEND, 
            true,
            true
        )

        cs.beginText()
        cs.setFont(PDType1Font.HELVETICA_BOLD, 50f)
        cs.setNonStrokingColor(200, 200, 200) // Light gray
        
        // Position watermark in center of page
        val pageWidth = page.mediaBox.width
        val pageHeight = page.mediaBox.height
        
        cs.newLineAtOffset(pageWidth / 6, pageHeight / 3)
        cs.showText(text)
        cs.endText()
        cs.close()
    }
}

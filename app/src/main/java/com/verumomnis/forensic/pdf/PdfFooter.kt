package com.verumomnis.forensic.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font

/**
 * Adds footer with SHA-512 hash to PDF documents.
 */
object PdfFooter {

    fun add(document: PDDocument, footer: String) {
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
        cs.setFont(PDType1Font.COURIER, 8f)
        cs.setNonStrokingColor(100, 100, 100)
        cs.newLineAtOffset(30f, 20f)
        cs.showText(footer)
        cs.endText()
        cs.close()
    }
}

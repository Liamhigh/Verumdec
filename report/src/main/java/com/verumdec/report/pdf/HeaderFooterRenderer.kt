package com.verumdec.report.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renderer for automatic page headers and footers in PDF documents.
 * 
 * Provides consistent branding and document information across all pages:
 * - Headers: Document title and "VERUM OMNIS" branding
 * - Footers: Page numbers, generation date, and truncated SHA-512 hash
 */
class HeaderFooterRenderer {

    companion object {
        /** Default header font size */
        const val HEADER_FONT_SIZE = 10f

        /** Default footer font size */
        const val FOOTER_FONT_SIZE = 8f

        /** Margin from page edge in points */
        const val PAGE_MARGIN = 36f

        /** Header line Y offset from top */
        const val HEADER_Y_OFFSET = 30f

        /** Footer line Y offset from bottom */
        const val FOOTER_Y_OFFSET = 20f

        /** Date format for footer */
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"

        /** Number of characters to show from SHA-512 hash */
        const val HASH_TRUNCATE_LENGTH = 16
    }

    private val dateFormatter = SimpleDateFormat(DATE_FORMAT, Locale.US)

    /**
     * Apply headers and footers to all pages in a document.
     *
     * @param document The PDF document
     * @param documentTitle The title to display in the header
     * @param caseId The case identifier
     * @param sha512Hash The full SHA-512 hash (will be truncated in footer)
     * @param generatedAt The generation timestamp
     */
    fun applyToAllPages(
        document: PDDocument,
        documentTitle: String,
        caseId: String,
        sha512Hash: String,
        generatedAt: Date = Date()
    ) {
        val totalPages = document.numberOfPages
        val truncatedHash = truncateHash(sha512Hash)

        document.pages.forEachIndexed { index, page ->
            val pageNumber = index + 1
            applyToPage(
                document = document,
                page = page,
                documentTitle = documentTitle,
                caseId = caseId,
                truncatedHash = truncatedHash,
                pageNumber = pageNumber,
                totalPages = totalPages,
                generatedAt = generatedAt
            )
        }
    }

    /**
     * Apply header and footer to a single page.
     *
     * @param document The PDF document
     * @param page The page to apply header/footer to
     * @param documentTitle The title for the header
     * @param caseId The case identifier
     * @param truncatedHash The truncated SHA-512 hash
     * @param pageNumber The current page number
     * @param totalPages The total number of pages
     * @param generatedAt The generation timestamp
     */
    fun applyToPage(
        document: PDDocument,
        page: PDPage,
        documentTitle: String,
        caseId: String,
        truncatedHash: String,
        pageNumber: Int,
        totalPages: Int,
        generatedAt: Date
    ) {
        val pageWidth = page.mediaBox.width
        val pageHeight = page.mediaBox.height

        PDPageContentStream(
            document,
            page,
            PDPageContentStream.AppendMode.APPEND,
            true,
            true
        ).use { contentStream ->
            drawHeader(contentStream, documentTitle, caseId, pageWidth, pageHeight)
            drawFooter(
                contentStream, 
                truncatedHash, 
                pageNumber, 
                totalPages, 
                generatedAt, 
                pageWidth
            )
        }
    }

    /**
     * Draw the page header.
     */
    private fun drawHeader(
        contentStream: PDPageContentStream,
        documentTitle: String,
        caseId: String,
        pageWidth: Float,
        pageHeight: Float
    ) {
        val headerFont: PDFont = PDType1Font.HELVETICA_BOLD
        val smallFont: PDFont = PDType1Font.HELVETICA

        val headerY = pageHeight - HEADER_Y_OFFSET

        // Draw "VERUM OMNIS" centered at top
        contentStream.beginText()
        contentStream.setFont(headerFont, HEADER_FONT_SIZE)
        val brandText = "VERUM OMNIS"
        val brandWidth = headerFont.getStringWidth(brandText) / 1000 * HEADER_FONT_SIZE
        contentStream.newLineAtOffset((pageWidth - brandWidth) / 2, headerY)
        contentStream.showText(brandText)
        contentStream.endText()

        // Draw document title on the left
        contentStream.beginText()
        contentStream.setFont(smallFont, HEADER_FONT_SIZE - 2)
        contentStream.newLineAtOffset(PAGE_MARGIN, headerY - 12)
        val truncatedTitle = if (documentTitle.length > 50) {
            documentTitle.take(47) + "..."
        } else {
            documentTitle
        }
        contentStream.showText(truncatedTitle)
        contentStream.endText()

        // Draw case ID on the right
        contentStream.beginText()
        contentStream.setFont(smallFont, HEADER_FONT_SIZE - 2)
        val caseText = "Case: $caseId"
        val caseWidth = smallFont.getStringWidth(caseText) / 1000 * (HEADER_FONT_SIZE - 2)
        contentStream.newLineAtOffset(pageWidth - PAGE_MARGIN - caseWidth, headerY - 12)
        contentStream.showText(caseText)
        contentStream.endText()

        // Draw separator line
        contentStream.setLineWidth(0.5f)
        contentStream.moveTo(PAGE_MARGIN, headerY - 20)
        contentStream.lineTo(pageWidth - PAGE_MARGIN, headerY - 20)
        contentStream.stroke()
    }

    /**
     * Draw the page footer.
     */
    private fun drawFooter(
        contentStream: PDPageContentStream,
        truncatedHash: String,
        pageNumber: Int,
        totalPages: Int,
        generatedAt: Date,
        pageWidth: Float
    ) {
        val footerFont: PDFont = PDType1Font.HELVETICA
        val footerY = FOOTER_Y_OFFSET

        // Draw separator line
        contentStream.setLineWidth(0.5f)
        contentStream.moveTo(PAGE_MARGIN, footerY + 10)
        contentStream.lineTo(pageWidth - PAGE_MARGIN, footerY + 10)
        contentStream.stroke()

        // Draw page number centered
        contentStream.beginText()
        contentStream.setFont(footerFont, FOOTER_FONT_SIZE)
        val pageText = "Page $pageNumber of $totalPages"
        val pageTextWidth = footerFont.getStringWidth(pageText) / 1000 * FOOTER_FONT_SIZE
        contentStream.newLineAtOffset((pageWidth - pageTextWidth) / 2, footerY)
        contentStream.showText(pageText)
        contentStream.endText()

        // Draw date on the left
        contentStream.beginText()
        contentStream.setFont(footerFont, FOOTER_FONT_SIZE)
        contentStream.newLineAtOffset(PAGE_MARGIN, footerY)
        contentStream.showText(dateFormatter.format(generatedAt))
        contentStream.endText()

        // Draw truncated hash on the right
        contentStream.beginText()
        contentStream.setFont(footerFont, FOOTER_FONT_SIZE)
        val hashText = "SHA-512: $truncatedHash"
        val hashWidth = footerFont.getStringWidth(hashText) / 1000 * FOOTER_FONT_SIZE
        contentStream.newLineAtOffset(pageWidth - PAGE_MARGIN - hashWidth, footerY)
        contentStream.showText(hashText)
        contentStream.endText()

        // Draw "Patent Pending Verum Omnis" below the main footer line
        contentStream.beginText()
        contentStream.setFont(footerFont, FOOTER_FONT_SIZE - 1)
        val patentText = "Patent Pending Verum Omnis"
        val patentWidth = footerFont.getStringWidth(patentText) / 1000 * (FOOTER_FONT_SIZE - 1)
        contentStream.newLineAtOffset((pageWidth - patentWidth) / 2, footerY - 10)
        contentStream.showText(patentText)
        contentStream.endText()
    }

    /**
     * Truncate SHA-512 hash to first 16 characters.
     *
     * @param sha512Hash The full SHA-512 hash
     * @return First 16 characters of the hash
     */
    fun truncateHash(sha512Hash: String): String {
        return if (sha512Hash.length > HASH_TRUNCATE_LENGTH) {
            sha512Hash.take(HASH_TRUNCATE_LENGTH)
        } else {
            sha512Hash
        }
    }
}

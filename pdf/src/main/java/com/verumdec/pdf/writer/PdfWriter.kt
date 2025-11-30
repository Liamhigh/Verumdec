package com.verumdec.pdf.writer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * PdfWriter - Creates PDF documents.
 */
class PdfWriter {

    private val defaultPageWidth = 595 // A4 width in points
    private val defaultPageHeight = 842 // A4 height in points
    private val defaultMargin = 50f

    /**
     * Create a simple PDF document.
     */
    fun createDocument(
        outputPath: String,
        title: String,
        content: String
    ): Boolean {
        return try {
            val document = PdfDocument()
            val pages = splitContentIntoPages(content, 60) // ~60 lines per page

            for ((index, pageContent) in pages.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(defaultPageWidth, defaultPageHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                
                drawPage(page.canvas, title, pageContent, index + 1, pages.size)
                
                document.finishPage(page)
            }

            val file = File(outputPath)
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Create a report PDF with sections.
     */
    fun createReport(
        outputPath: String,
        title: String,
        sections: List<ReportSection>
    ): Boolean {
        return try {
            val document = PdfDocument()
            var pageNumber = 1
            
            for (section in sections) {
                val pages = splitContentIntoPages(section.content, 55)
                
                for ((index, pageContent) in pages.withIndex()) {
                    val pageInfo = PdfDocument.PageInfo.Builder(defaultPageWidth, defaultPageHeight, pageNumber).create()
                    val page = document.startPage(pageInfo)
                    
                    val sectionTitle = if (index == 0) section.title else "${section.title} (continued)"
                    drawReportPage(page.canvas, title, sectionTitle, pageContent, pageNumber)
                    
                    document.finishPage(page)
                    pageNumber++
                }
            }

            val file = File(outputPath)
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun drawPage(
        canvas: Canvas,
        title: String,
        content: String,
        pageNum: Int,
        totalPages: Int
    ) {
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            typeface = Typeface.DEFAULT
        }

        // Title
        paint.textSize = 18f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(title, defaultMargin, defaultMargin, paint)

        // Content
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        var y = defaultMargin + 40f
        val lines = content.split("\n")
        for (line in lines) {
            canvas.drawText(line, defaultMargin, y, paint)
            y += 14f
        }

        // Page number
        paint.textSize = 10f
        canvas.drawText(
            "Page $pageNum of $totalPages",
            (defaultPageWidth / 2).toFloat(),
            defaultPageHeight - 30f,
            paint
        )
    }

    private fun drawReportPage(
        canvas: Canvas,
        reportTitle: String,
        sectionTitle: String,
        content: String,
        pageNum: Int
    ) {
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
        }

        // Header
        paint.textSize = 10f
        canvas.drawText(reportTitle, defaultMargin, 30f, paint)

        // Section title
        paint.textSize = 14f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(sectionTitle, defaultMargin, defaultMargin + 10f, paint)

        // Content
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        var y = defaultMargin + 40f
        val lines = content.split("\n")
        for (line in lines) {
            canvas.drawText(line, defaultMargin, y, paint)
            y += 13f
        }

        // Footer
        paint.textSize = 9f
        canvas.drawText(
            "✔ Patent Pending Verum Omnis",
            defaultMargin,
            defaultPageHeight - 30f,
            paint
        )
        canvas.drawText(
            "$pageNum",
            (defaultPageWidth - defaultMargin),
            defaultPageHeight - 30f,
            paint
        )
    }

    private fun splitContentIntoPages(content: String, linesPerPage: Int): List<String> {
        val allLines = content.split("\n")
        val pages = mutableListOf<String>()
        
        for (i in allLines.indices step linesPerPage) {
            val pageLines = allLines.subList(i, minOf(i + linesPerPage, allLines.size))
            pages.add(pageLines.joinToString("\n"))
        }
        
        if (pages.isEmpty()) pages.add(content)
        return pages
    }
}

data class ReportSection(
    val title: String,
    val content: String
)

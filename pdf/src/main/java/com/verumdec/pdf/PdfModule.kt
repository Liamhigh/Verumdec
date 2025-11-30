package com.verumdec.pdf

import android.content.Context
import com.verumdec.pdf.reader.PdfReader
import com.verumdec.pdf.writer.PdfWriter

/**
 * PDF Module - PDF Processing
 *
 * This module handles PDF processing, parsing, and metadata extraction.
 * It provides utilities for reading from and writing to PDF documents.
 *
 * ## Key Features:
 * - Extract text from PDF documents
 * - Parse document metadata
 * - Create new PDF documents
 * - Add watermarks and QR codes
 */
object PdfModule {

    const val VERSION = "1.0.0"
    const val NAME = "pdf"

    private var isInitialized = false
    private var appContext: Context? = null
    
    private var pdfReader: PdfReader? = null
    private var pdfWriter: PdfWriter? = null

    /**
     * Initialize the PDF module.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        appContext = context.applicationContext
        pdfReader = PdfReader()
        pdfWriter = PdfWriter()
        
        isInitialized = true
    }

    /**
     * Get PDF reader.
     */
    fun getPdfReader(): PdfReader {
        return pdfReader ?: throw IllegalStateException("PdfModule not initialized")
    }

    /**
     * Get PDF writer.
     */
    fun getPdfWriter(): PdfWriter {
        return pdfWriter ?: throw IllegalStateException("PdfModule not initialized")
    }

    /**
     * Extract text (convenience method).
     */
    fun extractText(pdfPath: String): String {
        return pdfReader?.extractText(pdfPath) ?: ""
    }

    /**
     * Extract metadata (convenience method).
     */
    fun extractMetadata(pdfPath: String): Map<String, String> {
        return pdfReader?.extractMetadata(pdfPath) ?: emptyMap()
    }
}

package com.verumdec.ocr

import android.content.Context
import android.graphics.Bitmap
import com.verumdec.ocr.engine.TextRecognizer
import com.verumdec.ocr.preprocessing.ImagePreprocessor

/**
 * OCR Module - Text Recognition
 *
 * This module handles Optical Character Recognition (OCR) for extracting text from images
 * and scanned documents. Uses Android's ML Kit for text recognition.
 *
 * ## Key Features:
 * - Extract text from images (screenshots, photos)
 * - Process scanned documents
 * - Image preprocessing for improved accuracy
 * - Confidence scoring for extracted text
 */
object OcrModule {

    const val VERSION = "1.0.0"
    const val NAME = "ocr"

    private var isInitialized = false
    private var appContext: Context? = null
    
    private var textRecognizer: TextRecognizer? = null
    private var imagePreprocessor: ImagePreprocessor? = null

    /**
     * Initialize the OCR module.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        appContext = context.applicationContext
        textRecognizer = TextRecognizer(context)
        imagePreprocessor = ImagePreprocessor()
        
        isInitialized = true
    }

    /**
     * Get text recognizer.
     */
    fun getTextRecognizer(): TextRecognizer {
        return textRecognizer ?: throw IllegalStateException("OcrModule not initialized")
    }

    /**
     * Get image preprocessor.
     */
    fun getImagePreprocessor(): ImagePreprocessor {
        return imagePreprocessor ?: throw IllegalStateException("OcrModule not initialized")
    }

    /**
     * Extract text from image path (convenience method).
     */
    fun extractText(imagePath: String): String {
        return textRecognizer?.recognizeFromPath(imagePath) ?: ""
    }

    /**
     * Extract text from bitmap (convenience method).
     */
    fun extractText(bitmap: Bitmap): String {
        return textRecognizer?.recognizeFromBitmap(bitmap) ?: ""
    }
}

package com.verumomnis.forensic

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Application class for Verum Omnis Forensic Engine.
 * Initializes PDFBox for Android.
 */
class VerumApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox for Android
        PDFBoxResourceLoader.init(applicationContext)
    }
}

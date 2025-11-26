package com.verumomnis.contradiction

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * Verum Omnis Contradiction Engine Application
 *
 * This application provides offline forensic contradiction analysis,
 * turning raw evidence into legal-ready reports with:
 * - Timeline generation
 * - Entity discovery
 * - Contradiction detection
 * - Behavioral analysis
 * - Liability scoring
 * - SHA-512 sealed PDF reports
 */
class VerumOmnisApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox for Android
        PDFBoxResourceLoader.init(applicationContext)
    }
}

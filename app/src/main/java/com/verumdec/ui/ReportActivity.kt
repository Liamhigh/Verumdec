package com.verumdec.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Report viewing activity - placeholder for future enhancement.
 * Currently reports are exported directly as PDF via ForensicReportViewerActivity.
 */
class ReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reports are now viewed via ForensicReportViewerActivity
        // This activity exists for navigation graph compatibility
        finish()
    }
}

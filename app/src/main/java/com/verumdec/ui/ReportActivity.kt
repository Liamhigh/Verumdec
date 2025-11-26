package com.verumdec.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.verumdec.databinding.ActivityAnalysisBinding

/**
 * Report viewing activity - placeholder for future enhancement.
 * Currently reports are exported directly as PDF.
 */
class ReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reports are exported as PDF, this activity can be used for future enhancements
        finish()
    }
}

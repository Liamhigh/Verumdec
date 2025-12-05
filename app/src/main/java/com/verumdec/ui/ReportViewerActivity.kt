package com.verumdec.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.verumdec.databinding.ActivityReportViewerBinding
import java.io.File

/**
 * Report Viewer Activity for displaying forensic reports.
 * Shows the full story reconstruction, contradictions, and hash details.
 */
class ReportViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportViewerBinding
    private var caseId: String? = null
    private var reportText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        caseId = intent.getStringExtra("caseId")
        reportText = intent.getStringExtra("report")

        setupUI()
        displayReport()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.btnExport.setOnClickListener {
            exportReport()
        }

        binding.btnShare.setOnClickListener {
            shareReport()
        }
    }

    private fun displayReport() {
        val report = reportText ?: "No report available"
        binding.textReport.text = report
    }

    private fun exportReport() {
        val currentCaseId = caseId ?: return
        val currentReport = reportText ?: return

        try {
            // Report is already saved in the case folder
            val reportFile = File(filesDir, "cases/$currentCaseId/reports/report.txt")
            
            if (reportFile.exists()) {
                Toast.makeText(
                    this,
                    "Report saved to: ${reportFile.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "Report file not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareReport() {
        val currentCaseId = caseId ?: return

        try {
            val reportFile = File(filesDir, "cases/$currentCaseId/reports/report.txt")
            
            if (!reportFile.exists()) {
                Toast.makeText(this, "Report file not found", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                reportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Report"))

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

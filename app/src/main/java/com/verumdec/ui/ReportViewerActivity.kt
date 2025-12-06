package com.verumdec.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.verumdec.databinding.ActivityReportViewerBinding
import java.io.File

/**
 * ReportViewerActivity
 * 
 * This activity displays the generated forensic report in plain text format.
 * The report is loaded from /cases/{caseId}/report.txt
 */
class ReportViewerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityReportViewerBinding
    private var reportFile: File? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val caseName = intent.getStringExtra("caseName") ?: "Report"
        val reportPath = intent.getStringExtra("reportPath")
        
        supportActionBar?.title = "$caseName - Report"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        if (reportPath != null) {
            reportFile = File(reportPath)
            loadReport(reportFile!!)
        } else {
            Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnShareReport.setOnClickListener {
            shareReport()
        }
    }
    
    private fun loadReport(file: File) {
        try {
            if (file.exists()) {
                val reportText = file.readText()
                binding.textReportContent.text = reportText
            } else {
                binding.textReportContent.text = "Report file not found."
            }
        } catch (e: Exception) {
            binding.textReportContent.text = "Error loading report: ${e.message}"
            e.printStackTrace()
        }
    }
    
    private fun shareReport() {
        val file = reportFile ?: return
        
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Verum Omnis Forensic Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share Report"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing report: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

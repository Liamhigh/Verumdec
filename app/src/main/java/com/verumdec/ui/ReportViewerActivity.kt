package com.verumdec.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.verumdec.R
import com.verumdec.data.Case
import com.verumdec.databinding.ActivityReportViewerBinding
import com.verumdec.engine.ReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * ReportViewerActivity - Display and export forensic reports
 * 
 * Displays:
 * - Case metadata
 * - Evidence index with SHA-512 hashes
 * - Timeline
 * - Contradictions
 * - Narrative summary
 * - Final SHA-512 case fingerprint
 * 
 * Provides export functionality for the report
 */
class ReportViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportViewerBinding
    private var currentCase: Case? = null
    private lateinit var caseDirectory: File
    private var reportFile: File? = null

    companion object {
        const val EXTRA_CASE_ID = "case_id"
        var currentCase: Case? = null // Temporary storage (use proper state management in production)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadCase()
        setupCaseDirectory()
        displayReport()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Forensic Report"
    }

    private fun loadCase() {
        val caseId = intent.getStringExtra(EXTRA_CASE_ID)
        
        // Load from companion object (temporary solution)
        currentCase = Companion.currentCase
        
        if (currentCase == null) {
            Toast.makeText(this, "Error: No case data available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    private fun setupCaseDirectory() {
        val case = currentCase ?: return
        val casesDir = File(getExternalFilesDir(null), "cases")
        caseDirectory = File(casesDir, case.id)
    }

    private fun displayReport() {
        val case = currentCase ?: return

        // Display case metadata
        binding.textCaseName.text = case.name
        binding.textCaseId.text = "Case ID: ${case.id}"
        binding.textGeneratedDate.text = "Generated: ${android.text.format.DateFormat.format("MMM dd, yyyy HH:mm", case.updatedAt)}"

        // Display summary statistics
        binding.textEvidenceCount.text = "${case.evidence.size} items"
        binding.textEntitiesCount.text = "${case.entities.size} entities"
        binding.textContradictionsCount.text = "${case.contradictions.size} contradictions"
        binding.textTimelineEventsCount.text = "${case.timeline.size} events"

        // Display sections
        displayEvidenceIndex(case)
        displayTimeline(case)
        displayContradictions(case)
        displayNarrative(case)
        displayCaseFingerprint(case)

        // Setup export button
        binding.btnExport.setOnClickListener { exportReport() }
        binding.btnGeneratePdf.setOnClickListener { generatePdfReport() }
    }

    private fun displayEvidenceIndex(case: Case) {
        val sb = StringBuilder()
        sb.append("EVIDENCE INDEX\n")
        sb.append("═".repeat(50)).append("\n\n")

        for ((index, evidence) in case.evidence.withIndex()) {
            sb.append("${index + 1}. ${evidence.fileName}\n")
            sb.append("   Type: ${evidence.type}\n")
            sb.append("   Added: ${android.text.format.DateFormat.format("MMM dd, yyyy HH:mm", evidence.addedAt)}\n")
            
            // Compute SHA-512 hash
            val hash = computeFileHash(evidence.filePath)
            sb.append("   SHA-512: ${hash.take(32)}...\n")
            sb.append("\n")
        }

        binding.textEvidenceIndex.text = sb.toString()
    }

    private fun displayTimeline(case: Case) {
        if (case.timeline.isEmpty()) {
            binding.sectionTimeline.visibility = View.GONE
            return
        }

        val sb = StringBuilder()
        sb.append("TIMELINE\n")
        sb.append("═".repeat(50)).append("\n\n")

        for (event in case.timeline.sortedBy { it.date }) {
            sb.append("• ${android.text.format.DateFormat.format("MMM dd, yyyy", event.date)}\n")
            sb.append("  ${event.description}\n")
            sb.append("  Type: ${event.eventType}, Significance: ${event.significance}\n")
            sb.append("\n")
        }

        binding.textTimeline.text = sb.toString()
    }

    private fun displayContradictions(case: Case) {
        if (case.contradictions.isEmpty()) {
            binding.sectionContradictions.visibility = View.GONE
            return
        }

        val sb = StringBuilder()
        sb.append("CONTRADICTIONS DETECTED\n")
        sb.append("═".repeat(50)).append("\n\n")

        for ((index, contradiction) in case.contradictions.withIndex()) {
            sb.append("${index + 1}. ${contradiction.description}\n")
            sb.append("   Type: ${contradiction.type}\n")
            sb.append("   Severity: ${contradiction.severity}\n")
            if (contradiction.legalImplication.isNotEmpty()) {
                sb.append("   Legal Implication: ${contradiction.legalImplication}\n")
            }
            sb.append("\n")
        }

        binding.textContradictions.text = sb.toString()
    }

    private fun displayNarrative(case: Case) {
        if (case.narrative.isEmpty()) {
            binding.sectionNarrative.visibility = View.GONE
            return
        }

        val sb = StringBuilder()
        sb.append("NARRATIVE SUMMARY\n")
        sb.append("═".repeat(50)).append("\n\n")
        sb.append(case.narrative)

        binding.textNarrative.text = sb.toString()
    }

    private fun displayCaseFingerprint(case: Case) {
        // Compute SHA-512 hash of entire case directory
        lifecycleScope.launch {
            val hash = withContext(Dispatchers.IO) {
                computeCaseDirectoryHash()
            }

            binding.textCaseHash.text = "SHA-512 Case Fingerprint:\n$hash"
            
            // Save hash to case
            if (case.sealedHash == null) {
                currentCase = case.copy(sealedHash = hash)
            }
        }
    }

    private fun computeFileHash(filePath: String): String {
        return try {
            val file = File(filePath)
            if (!file.exists()) return "File not found"
            
            val digest = MessageDigest.getInstance("SHA-512")
            val buffer = ByteArray(8192)
            
            file.inputStream().use { input ->
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    digest.update(buffer, 0, bytesRead)
                    bytesRead = input.read(buffer)
                }
            }
            
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Error computing hash: ${e.message}"
        }
    }

    private fun computeCaseDirectoryHash(): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-512")
            val evidenceDir = File(caseDirectory, "evidence")
            
            if (evidenceDir.exists() && evidenceDir.isDirectory) {
                evidenceDir.listFiles()?.sortedBy { it.name }?.forEach { file ->
                    if (file.isFile) {
                        val buffer = ByteArray(8192)
                        file.inputStream().use { input ->
                            var bytesRead = input.read(buffer)
                            while (bytesRead != -1) {
                                digest.update(buffer, 0, bytesRead)
                                bytesRead = input.read(buffer)
                            }
                        }
                    }
                }
            }
            
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Error computing case hash: ${e.message}"
        }
    }

    private fun generatePdfReport() {
        val case = currentCase ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnGeneratePdf.isEnabled = false

        lifecycleScope.launch {
            try {
                val reportGenerator = ReportGenerator(this@ReportViewerActivity)
                
                withContext(Dispatchers.IO) {
                    val reportsDir = File(caseDirectory, "reports")
                    reportsDir.mkdirs()
                    
                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                        .format(System.currentTimeMillis())
                    reportFile = File(reportsDir, "Report_${timestamp}.pdf")
                    
                    // Generate PDF using ReportGenerator
                    reportGenerator.generatePdfReport(case, reportFile!!)
                }

                binding.progressBar.visibility = View.GONE
                binding.btnGeneratePdf.isEnabled = true
                Toast.makeText(this@ReportViewerActivity, "PDF report generated", Toast.LENGTH_SHORT).show()
                
                // Update UI to show PDF is ready
                binding.textPdfStatus.text = "PDF Report: ${reportFile?.name}"
                binding.textPdfStatus.visibility = View.VISIBLE
                binding.btnExport.visibility = View.VISIBLE

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnGeneratePdf.isEnabled = true
                Toast.makeText(
                    this@ReportViewerActivity,
                    "Error generating PDF: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun exportReport() {
        val file = reportFile
        
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Generate PDF report first", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Forensic Report - ${currentCase?.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Export Report"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting report: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.report_viewer_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_share -> {
                exportReport()
                true
            }
            R.id.action_print -> {
                printReport()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun printReport() {
        Toast.makeText(this, "Print feature coming soon", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear static reference
        Companion.currentCase = null
    }
}

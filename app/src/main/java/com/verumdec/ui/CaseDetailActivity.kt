package com.verumdec.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.verumdec.R
import com.verumdec.data.*
import com.verumdec.databinding.ActivityCaseDetailBinding
import com.verumdec.engine.ContradictionEngine
import com.verumdec.engine.EvidenceProcessor
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

/**
 * CaseDetailActivity - Manages a forensic case and its evidence
 * 
 * Responsibilities:
 * - Display case metadata
 * - Manage evidence list (add, view, delete)
 * - Launch capture activities (scanner, audio, video)
 * - Trigger forensic analysis
 * - Navigate to report viewer
 */
class CaseDetailActivity : AppCompatActivity(), ContradictionEngine.ProgressListener {

    private lateinit var binding: ActivityCaseDetailBinding
    private lateinit var engine: ContradictionEngine
    private lateinit var evidenceAdapter: EvidenceAdapter
    
    private var currentCase: Case? = null
    private val evidenceUris = mutableMapOf<String, Uri>()
    private lateinit var caseDirectory: File

    companion object {
        const val EXTRA_CASE_ID = "case_id"
        const val EXTRA_CASE_NAME = "case_name"
        const val REQUEST_CODE_SCANNER = 100
        const val REQUEST_CODE_AUDIO = 101
        const val REQUEST_CODE_VIDEO = 102
    }

    private val pickDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { addDocumentEvidence(it) }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // Permissions granted, proceed with capture
        } else {
            Toast.makeText(this, "Permissions required for media capture", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize engine
        engine = ContradictionEngine(this)

        // Load or create case
        val caseId = intent.getStringExtra(EXTRA_CASE_ID)
        val caseName = intent.getStringExtra(EXTRA_CASE_NAME)
        
        if (caseId != null) {
            loadCase(caseId)
        } else if (caseName != null) {
            createNewCase(caseName)
        } else {
            finish()
            return
        }

        setupUI()
        setupCaseDirectory()
    }

    private fun createNewCase(name: String) {
        val caseId = UUID.randomUUID().toString()
        currentCase = Case(
            id = caseId,
            name = name,
            createdAt = Date()
        )
    }

    private fun loadCase(caseId: String) {
        // TODO: Load case from storage
        // For now, create a placeholder
        currentCase = Case(
            id = caseId,
            name = "Case $caseId"
        )
    }

    private fun setupCaseDirectory() {
        val case = currentCase ?: return
        
        // Create case directory structure:
        // /Android/data/<package>/files/cases/{CASE_ID}/
        //     case.json
        //     evidence/
        //     reports/
        val casesDir = File(getExternalFilesDir(null), "cases")
        caseDirectory = File(casesDir, case.id)
        
        File(caseDirectory, "evidence").mkdirs()
        File(caseDirectory, "reports").mkdirs()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = currentCase?.name

        // Setup evidence RecyclerView
        evidenceAdapter = EvidenceAdapter(
            onDeleteClick = { evidence -> removeEvidence(evidence) }
        )
        binding.recyclerEvidence.apply {
            layoutManager = LinearLayoutManager(this@CaseDetailActivity)
            adapter = evidenceAdapter
        }

        // Setup buttons
        binding.btnAddTextNote.setOnClickListener { addTextNote() }
        binding.btnAddImage.setOnClickListener { showImageSourceDialog() }
        binding.btnAddAudio.setOnClickListener { launchAudioRecorder() }
        binding.btnAddVideo.setOnClickListener { launchVideoRecorder() }
        binding.btnAddDocument.setOnClickListener { pickDocument() }
        binding.btnGenerateReport.setOnClickListener { generateReport() }

        updateUI()
    }

    private fun addTextNote() {
        val input = android.widget.EditText(this).apply {
            hint = "Enter text note"
            setPadding(48, 32, 48, 32)
            minLines = 3
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Text Note")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val text = input.text?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    addTextEvidence(text)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addTextEvidence(text: String) {
        val case = currentCase ?: return
        
        // Save text to file
        val fileName = "TEXT_${System.currentTimeMillis()}.txt"
        val file = File(File(caseDirectory, "evidence"), fileName)
        file.writeText(text)

        val evidence = Evidence(
            type = EvidenceType.TEXT,
            fileName = fileName,
            filePath = file.absolutePath,
            extractedText = text,
            processed = true
        )

        case.evidence.add(evidence)
        evidenceAdapter.submitList(case.evidence.toList())
        updateUI()
        
        Toast.makeText(this, "Text note added", Toast.LENGTH_SHORT).show()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchScanner()
                    1 -> pickDocument() // Use document picker for gallery
                }
            }
            .show()
    }

    private fun launchScanner() {
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            return
        }

        val intent = Intent(this, ScannerActivity::class.java).apply {
            putExtra(ScannerActivity.EXTRA_CASE_ID, currentCase?.id)
        }
        startActivityForResult(intent, REQUEST_CODE_SCANNER)
    }

    private fun launchAudioRecorder() {
        // Check audio permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            return
        }

        val intent = Intent(this, AudioRecorderActivity::class.java).apply {
            putExtra(AudioRecorderActivity.EXTRA_CASE_ID, currentCase?.id)
        }
        startActivityForResult(intent, REQUEST_CODE_AUDIO)
    }

    private fun launchVideoRecorder() {
        // Check camera and audio permissions
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) 
            != PackageManager.PERMISSION_GRANTED }) {
            requestPermissionLauncher.launch(permissions)
            return
        }

        val intent = Intent(this, VideoRecorderActivity::class.java).apply {
            putExtra(VideoRecorderActivity.EXTRA_CASE_ID, currentCase?.id)
        }
        startActivityForResult(intent, REQUEST_CODE_VIDEO)
    }

    private fun pickDocument() {
        pickDocumentLauncher.launch(arrayOf(
            "application/pdf",
            "image/*",
            "text/*",
            "audio/*",
            "video/*"
        ))
    }

    private fun addDocumentEvidence(uri: Uri) {
        val case = currentCase ?: return

        // Get file name
        val cursor = contentResolver.query(uri, null, null, null, null)
        var fileName = "Unknown"
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = it.getString(nameIndex)
                }
            }
        }

        // Persist permission
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Some URIs don't support persistent permissions
        }

        val evidenceType = EvidenceProcessor.getEvidenceType(fileName)
        val evidence = Evidence(
            type = evidenceType,
            fileName = fileName,
            filePath = uri.toString()
        )

        case.evidence.add(evidence)
        evidenceUris[evidence.id] = uri
        
        evidenceAdapter.submitList(case.evidence.toList())
        updateUI()
        
        Toast.makeText(this, "Evidence added: $fileName", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_SCANNER -> {
                    val filePath = data?.getStringExtra("file_path")
                    if (filePath != null) {
                        addCapturedEvidence(filePath, EvidenceType.IMAGE)
                    }
                }
                REQUEST_CODE_AUDIO -> {
                    val filePath = data?.getStringExtra("file_path")
                    if (filePath != null) {
                        addCapturedEvidence(filePath, EvidenceType.TEXT) // Audio as text after processing
                    }
                }
                REQUEST_CODE_VIDEO -> {
                    val filePath = data?.getStringExtra("file_path")
                    if (filePath != null) {
                        addCapturedEvidence(filePath, EvidenceType.TEXT) // Video as text after processing
                    }
                }
            }
        }
    }

    private fun addCapturedEvidence(filePath: String, type: EvidenceType) {
        val case = currentCase ?: return
        val file = File(filePath)
        
        val evidence = Evidence(
            type = type,
            fileName = file.name,
            filePath = filePath
        )

        case.evidence.add(evidence)
        evidenceAdapter.submitList(case.evidence.toList())
        updateUI()
        
        Toast.makeText(this, "Evidence captured: ${file.name}", Toast.LENGTH_SHORT).show()
    }

    private fun removeEvidence(evidence: Evidence) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Evidence")
            .setMessage("Are you sure you want to remove ${evidence.fileName}?")
            .setPositiveButton("Remove") { _, _ ->
                currentCase?.evidence?.remove(evidence)
                evidenceUris.remove(evidence.id)
                evidenceAdapter.submitList(currentCase?.evidence?.toList() ?: emptyList())
                updateUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateReport() {
        val case = currentCase ?: return
        
        if (case.evidence.isEmpty()) {
            Toast.makeText(this, "Add some evidence first", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress dialog
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Generating Forensic Report")
            .setMessage("Processing evidence...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            try {
                // Run forensic analysis on background thread
                currentCase = engine.analyze(case, evidenceUris, this@CaseDetailActivity)
                progressDialog.dismiss()
                
                // Navigate to report viewer
                navigateToReportViewer()
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(
                    this@CaseDetailActivity, 
                    "Error generating report: ${e.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun navigateToReportViewer() {
        val case = currentCase ?: return
        
        val intent = Intent(this, ReportViewerActivity::class.java).apply {
            putExtra(ReportViewerActivity.EXTRA_CASE_ID, case.id)
        }
        // Store case for ReportViewerActivity
        ReportViewerActivity.currentCase = case
        startActivity(intent)
    }

    private fun updateUI() {
        val case = currentCase
        
        if (case == null) {
            finish()
            return
        }

        binding.textCaseName.text = case.name
        binding.textCaseId.text = "ID: ${case.id.substring(0, 8)}"
        binding.textCreatedAt.text = "Created: ${android.text.format.DateFormat.format("MMM dd, yyyy", case.createdAt)}"
        
        val evidenceCount = case.evidence.size
        binding.textEvidenceCount.text = "$evidenceCount items"
        binding.emptyState.visibility = if (evidenceCount == 0) View.VISIBLE else View.GONE
        binding.recyclerEvidence.visibility = if (evidenceCount > 0) View.VISIBLE else View.GONE
        
        binding.btnGenerateReport.isEnabled = evidenceCount > 0
    }

    // ContradictionEngine.ProgressListener implementation
    override fun onProgressUpdate(
        stage: ContradictionEngine.AnalysisStage,
        progress: Int,
        message: String
    ) {
        runOnUiThread {
            // Update progress UI if needed
            binding.textProgress?.text = message
        }
    }

    override fun onComplete(case: Case) {
        runOnUiThread {
            currentCase = case
            Toast.makeText(this, "Analysis complete!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.case_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save_case -> {
                saveCase()
                true
            }
            R.id.action_export_case -> {
                exportCase()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveCase() {
        // TODO: Implement case persistence
        Toast.makeText(this, "Case saved", Toast.LENGTH_SHORT).show()
    }

    private fun exportCase() {
        // TODO: Implement case export
        Toast.makeText(this, "Export feature coming soon", Toast.LENGTH_SHORT).show()
    }
}

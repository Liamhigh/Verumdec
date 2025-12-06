package com.verumdec.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.verumdec.R
import com.verumdec.databinding.ActivityCaseDetailBinding
import com.verumdec.engine.EvidenceProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.verumomnis.engine.EngineManager
import java.io.File

/**
 * CaseDetailActivity
 * 
 * This activity allows users to:
 * - Add Text Note → save text → engine.ingest()
 * - Add Image → OCR → engine.ingest()
 * - Add Document → extract text → engine.ingest()
 * - Generate Report → engine.runFullPipeline() → save report → navigate to ReportViewerActivity
 */
class CaseDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCaseDetailBinding
    private lateinit var evidenceProcessor: EvidenceProcessor
    private lateinit var engineManager: EngineManager
    
    private var caseId: String = ""
    private var caseName: String = ""
    private val evidenceTexts = mutableListOf<String>()
    private val evidenceFiles = mutableListOf<String>()
    
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { addDocument(it) }
    }
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { addImage(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get case info from intent
        caseId = intent.getStringExtra("caseId") ?: return finish()
        caseName = intent.getStringExtra("caseName") ?: "Unknown Case"
        
        evidenceProcessor = EvidenceProcessor(this)
        engineManager = EngineManager()
        
        setupUI()
    }
    
    private fun setupUI() {
        supportActionBar?.title = caseName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.textCaseName.text = caseName
        binding.textCaseId.text = "ID: $caseId"
        
        // Setup buttons
        binding.btnAddText.setOnClickListener { addTextNote() }
        binding.btnAddImage.setOnClickListener { addImageEvidence() }
        binding.btnAddDocument.setOnClickListener { addDocumentEvidence() }
        binding.btnGenerateReport.setOnClickListener { generateReport() }
        
        updateEvidenceCount()
    }
    
    private fun addTextNote() {
        val input = android.widget.EditText(this).apply {
            hint = "Enter text evidence"
            setPadding(48, 32, 48, 32)
            minLines = 3
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Text Note")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val text = input.text?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    evidenceTexts.add(text)
                    evidenceFiles.add("Text Note ${evidenceFiles.size + 1}")
                    updateEvidenceCount()
                    Toast.makeText(this, "Text note added", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addImageEvidence() {
        pickImageLauncher.launch("image/*")
    }
    
    private fun addDocumentEvidence() {
        pickFileLauncher.launch(arrayOf("application/pdf", "text/*"))
    }
    
    private fun addImage(uri: Uri) {
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Processing Image")
            .setMessage("Extracting text via OCR...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        lifecycleScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    // Use existing EvidenceProcessor to extract text from image
                    val evidence = com.verumdec.data.Evidence(
                        type = com.verumdec.data.EvidenceType.IMAGE,
                        fileName = "Image ${evidenceFiles.size + 1}"
                    )
                    val processed = evidenceProcessor.processEvidence(evidence, uri)
                    processed.extractedText
                }
                
                if (text.isNotBlank()) {
                    evidenceTexts.add(text)
                    evidenceFiles.add("Image ${evidenceFiles.size + 1}")
                    updateEvidenceCount()
                    Toast.makeText(this@CaseDetailActivity, "Image processed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@CaseDetailActivity, "No text found in image", Toast.LENGTH_SHORT).show()
                }
                
                progressDialog.dismiss()
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@CaseDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun addDocument(uri: Uri) {
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Processing Document")
            .setMessage("Extracting text...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        lifecycleScope.launch {
            try {
                val fileName = getFileName(uri)
                val text = withContext(Dispatchers.IO) {
                    // Use existing EvidenceProcessor to extract text
                    val evidenceType = EvidenceProcessor.getEvidenceType(fileName)
                    val evidence = com.verumdec.data.Evidence(
                        type = evidenceType,
                        fileName = fileName
                    )
                    val processed = evidenceProcessor.processEvidence(evidence, uri)
                    processed.extractedText
                }
                
                if (text.isNotBlank()) {
                    evidenceTexts.add(text)
                    evidenceFiles.add(fileName)
                    updateEvidenceCount()
                    Toast.makeText(this@CaseDetailActivity, "Document processed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@CaseDetailActivity, "No text found in document", Toast.LENGTH_SHORT).show()
                }
                
                progressDialog.dismiss()
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@CaseDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun generateReport() {
        if (evidenceTexts.isEmpty()) {
            Toast.makeText(this, "Add some evidence first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Generating Report")
            .setMessage("Running four-layer forensic pipeline...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        lifecycleScope.launch {
            try {
                // Run the four-layer forensic engine pipeline
                val casesDir = File(getExternalFilesDir(null), "cases")
                val reportFile = engineManager.runMultipleEvidenceAndSave(
                    evidenceTexts = evidenceTexts,
                    caseId = caseId,
                    casesDir = casesDir
                )
                
                progressDialog.dismiss()
                
                // Navigate to ReportViewerActivity
                val intent = Intent(this@CaseDetailActivity, ReportViewerActivity::class.java).apply {
                    putExtra("caseId", caseId)
                    putExtra("caseName", caseName)
                    putExtra("reportPath", reportFile.absolutePath)
                }
                startActivity(intent)
                
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@CaseDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
    
    private fun updateEvidenceCount() {
        binding.textEvidenceCount.text = "${evidenceTexts.size} evidence items"
        binding.btnGenerateReport.isEnabled = evidenceTexts.isNotEmpty()
    }
    
    private fun getFileName(uri: Uri): String {
        var fileName = "Unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

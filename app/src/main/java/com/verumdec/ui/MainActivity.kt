package com.verumdec.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.verumdec.R
import com.verumdec.data.*
import com.verumdec.databinding.ActivityMainBinding
import com.verumdec.engine.ContradictionEngine
import com.verumdec.engine.EvidenceProcessor
import com.verumdec.engine.ForensicEngine
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity(), ContradictionEngine.ProgressListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var engine: ContradictionEngine
    private lateinit var forensicEngine: ForensicEngine
    private lateinit var evidenceAdapter: EvidenceAdapter
    
    private var currentCase: Case? = null
    private val evidenceUris = mutableMapOf<String, Uri>()

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { addEvidence(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        engine = ContradictionEngine(this)
        forensicEngine = ForensicEngine(this)
        setupUI()
    }

    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)

        // Setup evidence RecyclerView
        evidenceAdapter = EvidenceAdapter(
            onDeleteClick = { evidence -> removeEvidence(evidence) }
        )
        binding.recyclerEvidence.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = evidenceAdapter
        }

        // Setup buttons
        binding.btnNewCase.setOnClickListener { showNewCaseDialog() }
        binding.btnOpenCase.setOnClickListener { 
            Toast.makeText(this, "Open case feature coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.btnAddEvidence.setOnClickListener { pickFile() }
        binding.fabAdd.setOnClickListener { pickFile() }
        binding.btnAnalyze.setOnClickListener { runAnalysis() }
        binding.btnViewResults.setOnClickListener { viewResults() }

        updateUI()
    }

    private fun showNewCaseDialog() {
        val input = TextInputEditText(this).apply {
            hint = "Enter case name"
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("New Case")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text?.toString()?.trim()
                if (!name.isNullOrEmpty()) {
                    createNewCase(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNewCase(name: String) {
        lifecycleScope.launch {
            try {
                // Create case using ForensicEngine
                val caseId = forensicEngine.createCase(name)
                
                Toast.makeText(this@MainActivity, "Case '$name' created", Toast.LENGTH_SHORT).show()
                
                // Navigate to CaseDetailActivity
                val intent = Intent(this@MainActivity, CaseDetailActivity::class.java)
                intent.putExtra("caseId", caseId)
                startActivity(intent)
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error creating case: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun pickFile() {
        if (currentCase == null) {
            showNewCaseDialog()
            return
        }

        pickFileLauncher.launch(arrayOf(
            "application/pdf",
            "image/*",
            "text/*"
        ))
    }

    private fun addEvidence(uri: Uri) {
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
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

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
    }

    private fun removeEvidence(evidence: Evidence) {
        currentCase?.evidence?.remove(evidence)
        evidenceUris.remove(evidence.id)
        evidenceAdapter.submitList(currentCase?.evidence?.toList() ?: emptyList())
        updateUI()
    }

    private fun runAnalysis() {
        val case = currentCase ?: return
        if (case.evidence.isEmpty()) {
            Toast.makeText(this, "Add some evidence first", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress dialog
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Analyzing...")
            .setMessage("Processing evidence files...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            try {
                currentCase = engine.analyze(case, evidenceUris, this@MainActivity)
                progressDialog.dismiss()
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun viewResults() {
        val case = currentCase ?: return
        
        val intent = Intent(this, AnalysisActivity::class.java).apply {
            putExtra("case_id", case.id)
        }
        // Store case in companion object for simplicity (in production use proper state management)
        AnalysisActivity.currentCase = case
        startActivity(intent)
    }

    private fun updateUI() {
        val case = currentCase
        
        binding.textCaseName.text = case?.name ?: "No case loaded"
        
        val evidenceCount = case?.evidence?.size ?: 0
        binding.textEvidenceCount.text = "$evidenceCount files"
        binding.textNoEvidence.visibility = if (evidenceCount == 0) View.VISIBLE else View.GONE
        binding.recyclerEvidence.visibility = if (evidenceCount > 0) View.VISIBLE else View.GONE
        
        binding.btnAnalyze.isEnabled = evidenceCount > 0
        
        // Show stats if analysis has been run
        if (case != null && case.entities.isNotEmpty()) {
            binding.cardStats.visibility = View.VISIBLE
            binding.textEntitiesCount.text = case.entities.size.toString()
            binding.textContradictionsCount.text = case.contradictions.size.toString()
            binding.textTimelineCount.text = case.timeline.size.toString()
        }
    }

    // ContradictionEngine.ProgressListener implementation
    override fun onProgressUpdate(
        stage: ContradictionEngine.AnalysisStage,
        progress: Int,
        message: String
    ) {
        runOnUiThread {
            // Update progress if needed
        }
    }

    override fun onComplete(case: Case) {
        runOnUiThread {
            currentCase = case
            updateUI()
            Toast.makeText(this, "Analysis complete!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
        }
    }
}

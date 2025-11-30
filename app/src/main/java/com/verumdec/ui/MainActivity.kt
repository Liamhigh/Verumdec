package com.verumdec.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.verumdec.R
import com.verumdec.data.*
import com.verumdec.databinding.ActivityMainBinding
import com.verumdec.engine.ContradictionEngine
import com.verumdec.engine.EvidenceProcessor
import com.verumdec.viewmodel.CaseManagementViewModel
import com.verumdec.viewmodel.EngineExecutionViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), ContradictionEngine.ProgressListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var engine: ContradictionEngine
    private lateinit var evidenceAdapter: EvidenceAdapter
    private lateinit var caseViewModel: CaseManagementViewModel
    private lateinit var engineViewModel: EngineExecutionViewModel
    
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
        caseViewModel = ViewModelProvider(this)[CaseManagementViewModel::class.java]
        engineViewModel = ViewModelProvider(this)[EngineExecutionViewModel::class.java]
        
        setupUI()
        observeViewModels()
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
        binding.btnOpenCase.setOnClickListener { showOpenCaseDialog() }
        binding.btnAddEvidence.setOnClickListener { pickFile() }
        binding.fabAdd.setOnClickListener { pickFile() }
        binding.btnAnalyze.setOnClickListener { runAnalysis() }
        binding.btnViewResults.setOnClickListener { viewResults() }

        updateUI()
    }
    
    private fun observeViewModels() {
        // Observe case list for Open Case dialog
        caseViewModel.cases.observe(this) { cases ->
            // Cases list is available for the dialog
        }
        
        caseViewModel.currentCase.observe(this) { case ->
            if (case != null) {
                currentCase = case
                evidenceUris.clear()
                evidenceAdapter.submitList(case.evidence.toList())
                updateUI()
            }
        }
        
        caseViewModel.operationState.observe(this) { state ->
            when (state) {
                is CaseManagementViewModel.OperationState.SaveComplete -> {
                    Toast.makeText(this, "Case saved", Toast.LENGTH_SHORT).show()
                }
                is CaseManagementViewModel.OperationState.LoadComplete -> {
                    Toast.makeText(this, "Case loaded", Toast.LENGTH_SHORT).show()
                }
                is CaseManagementViewModel.OperationState.DeleteComplete -> {
                    Toast.makeText(this, "Case deleted", Toast.LENGTH_SHORT).show()
                }
                is CaseManagementViewModel.OperationState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> { /* Idle, Loading states */ }
            }
        }
        
        // Observe engine execution state
        engineViewModel.executionState.observe(this) { state ->
            when (state) {
                is EngineExecutionViewModel.ExecutionState.Complete -> {
                    currentCase = engineViewModel.currentCase.value
                    updateUI()
                }
                is EngineExecutionViewModel.ExecutionState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> { /* Idle, Running, Cancelled */ }
            }
        }
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
    
    private fun showOpenCaseDialog() {
        val cases = caseViewModel.cases.value ?: emptyList()
        
        if (cases.isEmpty()) {
            Toast.makeText(this, "No saved cases found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val caseNames = cases.map { "${it.name} (${it.evidenceCount} files)" }.toTypedArray()
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Open Case")
            .setItems(caseNames) { _, which ->
                val selectedCase = cases[which]
                caseViewModel.loadCase(selectedCase.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNewCase(name: String) {
        currentCase = caseViewModel.createCase(name)
        evidenceUris.clear()
        evidenceAdapter.submitList(emptyList())
        binding.cardStats.visibility = View.GONE
        updateUI()
        Toast.makeText(this, "Case '$name' created", Toast.LENGTH_SHORT).show()
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
                
                // Save case after analysis
                currentCase?.let { caseViewModel.saveCase(it) }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun viewResults() {
        val case = currentCase ?: return
        NavigationHelper.navigateToAnalysis(this, case)
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

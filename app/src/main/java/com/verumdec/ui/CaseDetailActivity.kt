package com.verumdec.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.verumdec.data.Case
import com.verumdec.databinding.ActivityCaseDetailBinding
import com.verumdec.engine.ForensicEngine
import kotlinx.coroutines.launch

/**
 * Case Detail Activity for managing a specific case.
 * Allows adding evidence, generating reports, and viewing case details.
 */
class CaseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaseDetailBinding
    private lateinit var forensicEngine: ForensicEngine
    private lateinit var evidenceAdapter: EvidenceAdapter
    private var caseId: String? = null
    private var currentCase: Case? = null

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Evidence was added, reload case
            loadCase()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        forensicEngine = ForensicEngine(this)
        caseId = intent.getStringExtra("caseId")

        if (caseId == null) {
            Toast.makeText(this, "Error: No case ID provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        loadCase()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Setup RecyclerView
        evidenceAdapter = EvidenceAdapter(
            onDeleteClick = { evidence ->
                // Handle delete if needed
                Toast.makeText(this, "Delete not implemented yet", Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerEvidence.apply {
            layoutManager = LinearLayoutManager(this@CaseDetailActivity)
            adapter = evidenceAdapter
        }

        // Setup buttons
        binding.btnAddTextNote.setOnClickListener {
            showAddTextNoteDialog()
        }

        binding.btnScanEvidence.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            intent.putExtra("caseId", caseId)
            scannerLauncher.launch(intent)
        }

        binding.btnGenerateReport.setOnClickListener {
            generateReport()
        }
    }

    private fun loadCase() {
        val currentCaseId = caseId ?: return

        lifecycleScope.launch {
            try {
                currentCase = forensicEngine.loadCase(currentCaseId)
                updateUI()
            } catch (e: Exception) {
                Toast.makeText(
                    this@CaseDetailActivity,
                    "Error loading case: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateUI() {
        val case = currentCase ?: return

        supportActionBar?.title = case.name
        binding.textCaseName.text = "Name: ${case.name}"
        binding.textCaseId.text = "ID: ${case.id}"
        binding.textEvidenceCount.text = "Evidence: ${case.evidence.size} items"

        if (case.evidence.isEmpty()) {
            binding.textNoEvidence.visibility = View.VISIBLE
            binding.recyclerEvidence.visibility = View.GONE
        } else {
            binding.textNoEvidence.visibility = View.GONE
            binding.recyclerEvidence.visibility = View.VISIBLE
            evidenceAdapter.submitList(case.evidence.toList())
        }
    }

    private fun showAddTextNoteDialog() {
        val input = TextInputEditText(this).apply {
            hint = "Enter text note"
            setPadding(48, 32, 48, 32)
            minLines = 3
            maxLines = 10
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Text Note")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val text = input.text?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    addTextNote(text)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addTextNote(text: String) {
        val currentCaseId = caseId ?: return

        lifecycleScope.launch {
            try {
                forensicEngine.addTextNote(currentCaseId, text)
                loadCase()
                Toast.makeText(
                    this@CaseDetailActivity,
                    "Text note added",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@CaseDetailActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun generateReport() {
        val currentCaseId = caseId ?: return

        binding.btnGenerateReport.isEnabled = false
        binding.btnGenerateReport.text = "Generating..."

        lifecycleScope.launch {
            try {
                val report = forensicEngine.generateReport(currentCaseId)
                
                binding.btnGenerateReport.isEnabled = true
                binding.btnGenerateReport.text = "Generate Report"

                // Navigate to ReportViewerActivity
                val intent = Intent(this@CaseDetailActivity, ReportViewerActivity::class.java)
                intent.putExtra("caseId", currentCaseId)
                intent.putExtra("report", report)
                startActivity(intent)

            } catch (e: Exception) {
                binding.btnGenerateReport.isEnabled = true
                binding.btnGenerateReport.text = "Generate Report"
                Toast.makeText(
                    this@CaseDetailActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

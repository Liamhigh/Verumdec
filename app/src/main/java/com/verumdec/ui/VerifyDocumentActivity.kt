package com.verumdec.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.verumdec.R
import com.verumdec.data.Evidence
import com.verumdec.databinding.ActivityVerifyDocumentBinding
import com.verumdec.viewmodel.DocumentIntakeViewModel

/**
 * Activity for verifying and reviewing documents before analysis.
 * Allows users to preview extracted text and confirm document processing.
 */
class VerifyDocumentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyDocumentBinding
    private lateinit var viewModel: DocumentIntakeViewModel
    private lateinit var documentAdapter: VerifyDocumentAdapter

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { addDocument(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DocumentIntakeViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupButtons()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_verify_document)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupRecyclerView() {
        documentAdapter = VerifyDocumentAdapter(
            onItemClick = { evidence -> showDocumentPreview(evidence) },
            onVerifyClick = { evidence -> verifyDocument(evidence) },
            onRemoveClick = { evidence -> removeDocument(evidence) }
        )
        
        binding.recyclerDocuments.apply {
            layoutManager = LinearLayoutManager(this@VerifyDocumentActivity)
            adapter = documentAdapter
        }
    }

    private fun setupButtons() {
        binding.btnAddDocument.setOnClickListener {
            pickFileLauncher.launch(arrayOf(
                "application/pdf",
                "image/*",
                "text/*"
            ))
        }

        binding.btnVerifyAll.setOnClickListener {
            verifyAllDocuments()
        }

        binding.btnProceed.setOnClickListener {
            proceedWithVerifiedDocuments()
        }
    }

    private fun observeViewModel() {
        viewModel.documents.observe(this) { documents ->
            documentAdapter.submitList(documents)
            updateUI(documents)
        }

        viewModel.processingState.observe(this) { state ->
            when (state) {
                is DocumentIntakeViewModel.ProcessingState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.textStatus.text = state.message
                }
                is DocumentIntakeViewModel.ProcessingState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.textStatus.text = state.message
                }
                is DocumentIntakeViewModel.ProcessingState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun addDocument(uri: Uri) {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

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

        val mimeType = contentResolver.getType(uri)
        viewModel.addDocument(uri, fileName, mimeType)
    }

    private fun showDocumentPreview(evidence: Evidence) {
        val extractedText = evidence.extractedText.ifEmpty { 
            getString(R.string.msg_not_processed)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(evidence.fileName)
            .setMessage(extractedText.take(2000))
            .setPositiveButton(R.string.btn_ok, null)
            .show()
    }

    private fun verifyDocument(evidence: Evidence) {
        viewModel.processDocument(evidence)
    }

    private fun verifyAllDocuments() {
        val documents = viewModel.documents.value ?: return
        documents.filter { !it.processed }.forEach { evidence ->
            viewModel.processDocument(evidence)
        }
    }

    private fun removeDocument(evidence: Evidence) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_confirm_delete)
            .setMessage(getString(R.string.msg_confirm_remove_document, evidence.fileName))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewModel.removeDocument(evidence)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun proceedWithVerifiedDocuments() {
        val documents = viewModel.documents.value ?: return
        val unprocessed = documents.count { !it.processed }
        
        if (unprocessed > 0) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_unprocessed_documents)
                .setMessage(getString(R.string.msg_unprocessed_documents, unprocessed))
                .setPositiveButton(R.string.btn_proceed_anyway) { _, _ ->
                    finishWithResult()
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        } else {
            finishWithResult()
        }
    }

    private fun finishWithResult() {
        // Store documents for retrieval by calling activity
        currentDocuments = viewModel.documents.value ?: emptyList()
        currentDocumentUris = viewModel.getAllDocumentUris()
        setResult(RESULT_OK)
        finish()
    }

    private fun updateUI(documents: List<Evidence>) {
        val hasDocuments = documents.isNotEmpty()
        binding.textEmptyState.visibility = if (hasDocuments) View.GONE else View.VISIBLE
        binding.recyclerDocuments.visibility = if (hasDocuments) View.VISIBLE else View.GONE
        binding.btnVerifyAll.isEnabled = hasDocuments
        binding.btnProceed.isEnabled = hasDocuments

        val processed = documents.count { it.processed }
        binding.textStatus.text = getString(R.string.status_documents, processed, documents.size)
    }

    companion object {
        var currentDocuments: List<Evidence> = emptyList()
        var currentDocumentUris: Map<String, Uri> = emptyMap()

        fun newIntent(context: Context): Intent {
            return Intent(context, VerifyDocumentActivity::class.java)
        }
    }
}

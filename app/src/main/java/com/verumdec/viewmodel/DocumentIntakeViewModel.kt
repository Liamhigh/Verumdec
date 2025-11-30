package com.verumdec.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.verumdec.data.Evidence
import com.verumdec.data.EvidenceType
import com.verumdec.engine.EvidenceProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for document intake operations.
 * Handles document selection, processing, and validation.
 */
class DocumentIntakeViewModel(application: Application) : AndroidViewModel(application) {

    private val evidenceProcessor = EvidenceProcessor(application)

    private val _documents = MutableLiveData<List<Evidence>>(emptyList())
    val documents: LiveData<List<Evidence>> = _documents

    private val _processingState = MutableLiveData<ProcessingState>(ProcessingState.Idle)
    val processingState: LiveData<ProcessingState> = _processingState

    private val _currentDocument = MutableLiveData<Evidence?>()
    val currentDocument: LiveData<Evidence?> = _currentDocument

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val documentUris = mutableMapOf<String, Uri>()

    /**
     * Add a new document from URI.
     */
    fun addDocument(uri: Uri, fileName: String, mimeType: String?) {
        viewModelScope.launch {
            _processingState.value = ProcessingState.Loading("Adding document...")
            try {
                val evidenceType = determineEvidenceType(fileName, mimeType)
                val evidence = Evidence(
                    type = evidenceType,
                    fileName = fileName,
                    filePath = uri.toString()
                )
                
                documentUris[evidence.id] = uri
                
                val currentList = _documents.value.orEmpty().toMutableList()
                currentList.add(evidence)
                _documents.value = currentList
                
                _processingState.value = ProcessingState.Success("Document added")
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error(e.message ?: "Failed to add document")
                _error.value = e.message
            }
        }
    }

    /**
     * Process a document to extract text content.
     */
    fun processDocument(evidence: Evidence) {
        viewModelScope.launch {
            _processingState.value = ProcessingState.Loading("Processing ${evidence.fileName}...")
            try {
                val uri = documentUris[evidence.id]
                if (uri == null) {
                    _processingState.value = ProcessingState.Error("Document URI not found")
                    return@launch
                }

                val processed = withContext(Dispatchers.IO) {
                    evidenceProcessor.processEvidence(evidence, uri)
                }

                updateDocument(processed)
                _currentDocument.value = processed
                _processingState.value = ProcessingState.Success("Processing complete")
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error(e.message ?: "Processing failed")
                _error.value = e.message
            }
        }
    }

    /**
     * Remove a document from the list.
     */
    fun removeDocument(evidence: Evidence) {
        documentUris.remove(evidence.id)
        val currentList = _documents.value.orEmpty().toMutableList()
        currentList.removeAll { it.id == evidence.id }
        _documents.value = currentList
    }

    /**
     * Get URI for a document.
     */
    fun getDocumentUri(evidenceId: String): Uri? = documentUris[evidenceId]

    /**
     * Get all document URIs.
     */
    fun getAllDocumentUris(): Map<String, Uri> = documentUris.toMap()

    /**
     * Clear all documents.
     */
    fun clearAll() {
        documentUris.clear()
        _documents.value = emptyList()
        _currentDocument.value = null
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _error.value = null
    }

    private fun updateDocument(evidence: Evidence) {
        val currentList = _documents.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == evidence.id }
        if (index >= 0) {
            currentList[index] = evidence
            _documents.value = currentList
        }
    }

    private fun determineEvidenceType(fileName: String, mimeType: String?): EvidenceType {
        return when {
            mimeType?.contains("pdf") == true || fileName.endsWith(".pdf", ignoreCase = true) -> 
                EvidenceType.PDF
            mimeType?.startsWith("image/") == true || 
                fileName.matches(Regex(".*\\.(jpg|jpeg|png|gif|bmp|webp)$", RegexOption.IGNORE_CASE)) -> 
                EvidenceType.IMAGE
            mimeType?.contains("text") == true || fileName.endsWith(".txt", ignoreCase = true) -> 
                EvidenceType.TEXT
            fileName.contains("email", ignoreCase = true) || fileName.endsWith(".eml") -> 
                EvidenceType.EMAIL
            fileName.contains("whatsapp", ignoreCase = true) -> 
                EvidenceType.WHATSAPP
            else -> EvidenceType.UNKNOWN
        }
    }

    sealed class ProcessingState {
        object Idle : ProcessingState()
        data class Loading(val message: String) : ProcessingState()
        data class Success(val message: String) : ProcessingState()
        data class Error(val message: String) : ProcessingState()
    }
}

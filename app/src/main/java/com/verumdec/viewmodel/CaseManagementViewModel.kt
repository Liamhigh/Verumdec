package com.verumdec.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.verumdec.data.Case
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

/**
 * ViewModel for case management with local JSON database storage.
 * Handles CRUD operations for cases stored as JSON files.
 */
class CaseManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .setPrettyPrinting()
        .create()

    private val casesDir: File = File(application.filesDir, CASES_DIRECTORY).also { 
        if (!it.exists()) it.mkdirs() 
    }

    private val _cases = MutableLiveData<List<CaseInfo>>(emptyList())
    val cases: LiveData<List<CaseInfo>> = _cases

    private val _currentCase = MutableLiveData<Case?>()
    val currentCase: LiveData<Case?> = _currentCase

    private val _operationState = MutableLiveData<OperationState>(OperationState.Idle)
    val operationState: LiveData<OperationState> = _operationState

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadCaseList()
    }

    /**
     * Load list of all saved cases.
     */
    fun loadCaseList() {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val caseInfoList = withContext(Dispatchers.IO) {
                    getCaseInfoList()
                }
                _cases.value = caseInfoList
                _operationState.value = OperationState.Idle
            } catch (e: Exception) {
                _error.value = "Failed to load cases: ${e.message}"
                _operationState.value = OperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Create a new case.
     */
    fun createCase(name: String): Case {
        val case = Case(name = name)
        _currentCase.value = case
        return case
    }

    /**
     * Save a case to local storage.
     */
    fun saveCase(case: Case) {
        viewModelScope.launch {
            _operationState.value = OperationState.Saving
            try {
                val updatedCase = case.copy(updatedAt = Date())
                
                withContext(Dispatchers.IO) {
                    val caseFile = File(casesDir, "${updatedCase.id}.json")
                    caseFile.writeText(gson.toJson(updatedCase))
                }
                
                _currentCase.value = updatedCase
                loadCaseList() // Refresh list
                _operationState.value = OperationState.SaveComplete
            } catch (e: Exception) {
                _error.value = "Failed to save case: ${e.message}"
                _operationState.value = OperationState.Error(e.message ?: "Save failed")
            }
        }
    }

    /**
     * Load a case from local storage.
     */
    fun loadCase(caseId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val case = withContext(Dispatchers.IO) {
                    val caseFile = File(casesDir, "$caseId.json")
                    if (caseFile.exists()) {
                        gson.fromJson(caseFile.readText(), Case::class.java)
                    } else {
                        null
                    }
                }
                
                if (case != null) {
                    _currentCase.value = case
                    _operationState.value = OperationState.LoadComplete
                } else {
                    _error.value = "Case not found"
                    _operationState.value = OperationState.Error("Case not found")
                }
            } catch (e: Exception) {
                _error.value = "Failed to load case: ${e.message}"
                _operationState.value = OperationState.Error(e.message ?: "Load failed")
            }
        }
    }

    /**
     * Delete a case from local storage.
     */
    fun deleteCase(caseId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Deleting
            try {
                withContext(Dispatchers.IO) {
                    val caseFile = File(casesDir, "$caseId.json")
                    if (caseFile.exists()) {
                        caseFile.delete()
                    }
                }
                
                // Clear current case if it was the deleted one
                if (_currentCase.value?.id == caseId) {
                    _currentCase.value = null
                }
                
                loadCaseList() // Refresh list
                _operationState.value = OperationState.DeleteComplete
            } catch (e: Exception) {
                _error.value = "Failed to delete case: ${e.message}"
                _operationState.value = OperationState.Error(e.message ?: "Delete failed")
            }
        }
    }

    /**
     * Export case to a specific location.
     */
    fun exportCase(case: Case, exportDir: File): File? {
        return try {
            val exportFile = File(exportDir, "case_${case.id}_${System.currentTimeMillis()}.json")
            exportFile.writeText(gson.toJson(case))
            exportFile
        } catch (e: Exception) {
            _error.value = "Export failed: ${e.message}"
            null
        }
    }

    /**
     * Import case from a file.
     */
    fun importCase(file: File) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val case = withContext(Dispatchers.IO) {
                    gson.fromJson(file.readText(), Case::class.java)
                }
                
                // Save imported case
                saveCase(case)
            } catch (e: Exception) {
                _error.value = "Import failed: ${e.message}"
                _operationState.value = OperationState.Error(e.message ?: "Import failed")
            }
        }
    }

    /**
     * Set the current case.
     */
    fun setCurrentCase(case: Case?) {
        _currentCase.value = case
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _error.value = null
    }

    private fun getCaseInfoList(): List<CaseInfo> {
        return casesDir.listFiles { _, name -> name.endsWith(".json") }
            ?.mapNotNull { file ->
                try {
                    val case = gson.fromJson(file.readText(), Case::class.java)
                    CaseInfo(
                        id = case.id,
                        name = case.name,
                        createdAt = case.createdAt,
                        updatedAt = case.updatedAt,
                        evidenceCount = case.evidence.size,
                        entityCount = case.entities.size,
                        contradictionCount = case.contradictions.size,
                        hasAnalysis = case.entities.isNotEmpty()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    data class CaseInfo(
        val id: String,
        val name: String,
        val createdAt: Date,
        val updatedAt: Date,
        val evidenceCount: Int,
        val entityCount: Int,
        val contradictionCount: Int,
        val hasAnalysis: Boolean
    )

    sealed class OperationState {
        object Idle : OperationState()
        object Loading : OperationState()
        object Saving : OperationState()
        object SaveComplete : OperationState()
        object LoadComplete : OperationState()
        object Deleting : OperationState()
        object DeleteComplete : OperationState()
        data class Error(val message: String) : OperationState()
    }

    companion object {
        private const val CASES_DIRECTORY = "cases"
    }
}

package com.verumdec.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.verumdec.data.*
import com.verumdec.engine.ContradictionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Contradiction Engine execution workflow.
 * Manages the full analysis pipeline from evidence processing to report generation.
 */
class EngineExecutionViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = ContradictionEngine(application)

    private val _currentCase = MutableLiveData<Case?>()
    val currentCase: LiveData<Case?> = _currentCase

    private val _executionState = MutableLiveData<ExecutionState>(ExecutionState.Idle)
    val executionState: LiveData<ExecutionState> = _executionState

    private val _currentStage = MutableLiveData<ContradictionEngine.AnalysisStage?>()
    val currentStage: LiveData<ContradictionEngine.AnalysisStage?> = _currentStage

    private val _stageProgress = MutableLiveData<Int>(0)
    val stageProgress: LiveData<Int> = _stageProgress

    private val _stageMessage = MutableLiveData<String>("")
    val stageMessage: LiveData<String> = _stageMessage

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var analysisJob: Job? = null

    private val progressListener = object : ContradictionEngine.ProgressListener {
        override fun onProgressUpdate(
            stage: ContradictionEngine.AnalysisStage,
            progress: Int,
            message: String
        ) {
            _currentStage.postValue(stage)
            _stageProgress.postValue(progress)
            _stageMessage.postValue(message)
        }

        override fun onComplete(case: Case) {
            _currentCase.postValue(case)
            _executionState.postValue(ExecutionState.Complete)
        }

        override fun onError(error: String) {
            _executionState.postValue(ExecutionState.Error(error))
            _error.postValue(error)
        }
    }

    /**
     * Start the analysis engine with the given case and evidence URIs.
     */
    fun startAnalysis(case: Case, evidenceUris: Map<String, Uri>) {
        analysisJob?.cancel()
        
        analysisJob = viewModelScope.launch {
            _executionState.value = ExecutionState.Running
            _currentCase.value = case
            
            try {
                val result = withContext(Dispatchers.IO) {
                    engine.analyze(case, evidenceUris, progressListener)
                }
                _currentCase.value = result
            } catch (e: Exception) {
                _executionState.value = ExecutionState.Error(e.message ?: "Analysis failed")
                _error.value = e.message
            }
        }
    }

    /**
     * Cancel the running analysis.
     */
    fun cancelAnalysis() {
        analysisJob?.cancel()
        _executionState.value = ExecutionState.Cancelled
        _currentStage.value = null
        _stageProgress.value = 0
        _stageMessage.value = ""
    }

    /**
     * Reset the engine state for a new analysis.
     */
    fun reset() {
        cancelAnalysis()
        _currentCase.value = null
        _executionState.value = ExecutionState.Idle
        _error.value = null
    }

    /**
     * Get a summary of the current case analysis.
     */
    fun getCaseSummary(): CaseSummary? {
        return _currentCase.value?.let { engine.getSummary(it) }
    }

    /**
     * Get the overall progress percentage across all stages.
     */
    fun getOverallProgress(): Int {
        val stage = _currentStage.value ?: return 0
        val stageProgress = _stageProgress.value ?: 0
        
        val stageWeight = when (stage) {
            ContradictionEngine.AnalysisStage.PROCESSING_EVIDENCE -> 0
            ContradictionEngine.AnalysisStage.DISCOVERING_ENTITIES -> 15
            ContradictionEngine.AnalysisStage.GENERATING_TIMELINE -> 30
            ContradictionEngine.AnalysisStage.ANALYZING_CONTRADICTIONS -> 45
            ContradictionEngine.AnalysisStage.ANALYZING_BEHAVIOR -> 60
            ContradictionEngine.AnalysisStage.CALCULATING_LIABILITY -> 75
            ContradictionEngine.AnalysisStage.GENERATING_NARRATIVE -> 90
            ContradictionEngine.AnalysisStage.COMPLETE -> 100
        }
        
        val stageContribution = (stageProgress * 15) / 100
        return stageWeight + stageContribution
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        analysisJob?.cancel()
    }

    sealed class ExecutionState {
        object Idle : ExecutionState()
        object Running : ExecutionState()
        object Complete : ExecutionState()
        object Cancelled : ExecutionState()
        data class Error(val message: String) : ExecutionState()
    }
}

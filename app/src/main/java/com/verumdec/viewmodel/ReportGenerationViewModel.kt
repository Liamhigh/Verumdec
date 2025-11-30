package com.verumdec.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.verumdec.data.*
import com.verumdec.engine.ContradictionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel for report generation operations.
 * Handles PDF report generation, sealing, and export.
 */
class ReportGenerationViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = ContradictionEngine(application)

    private val _generationState = MutableLiveData<GenerationState>(GenerationState.Idle)
    val generationState: LiveData<GenerationState> = _generationState

    private val _generatedReport = MutableLiveData<File?>()
    val generatedReport: LiveData<File?> = _generatedReport

    private val _reportPreview = MutableLiveData<ReportPreview?>()
    val reportPreview: LiveData<ReportPreview?> = _reportPreview

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * Generate a report from the analyzed case.
     */
    fun generateReport(case: Case) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Generating("Preparing report...")
            
            try {
                // First create preview
                _generationState.value = GenerationState.Generating("Building report sections...")
                val preview = createReportPreview(case)
                _reportPreview.value = preview

                // Generate PDF
                _generationState.value = GenerationState.Generating("Generating PDF...")
                val reportFile = withContext(Dispatchers.IO) {
                    engine.generateReport(case)
                }
                
                _generatedReport.value = reportFile
                _generationState.value = GenerationState.Complete(reportFile)
            } catch (e: Exception) {
                _generationState.value = GenerationState.Error(e.message ?: "Report generation failed")
                _error.value = e.message
            }
        }
    }

    /**
     * Generate preview of report contents.
     */
    fun previewReport(case: Case) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Generating("Creating preview...")
            try {
                val preview = createReportPreview(case)
                _reportPreview.value = preview
                _generationState.value = GenerationState.PreviewReady
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Get the generated report file.
     */
    fun getReportFile(): File? = _generatedReport.value

    /**
     * Clear the generated report.
     */
    fun clearReport() {
        _generatedReport.value = null
        _reportPreview.value = null
        _generationState.value = GenerationState.Idle
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _error.value = null
    }

    private fun createReportPreview(case: Case): ReportPreview {
        val summary = engine.getSummary(case)
        
        val entitySummaries = case.entities.map { entity ->
            val score = case.liabilityScores[entity.id]
            EntitySummary(
                name = entity.primaryName,
                aliases = entity.aliases,
                statementCount = entity.statements.size,
                liabilityScore = score?.overallScore ?: 0f
            )
        }

        val contradictionSummaries = case.contradictions.map { contradiction ->
            val entity = case.entities.find { it.id == contradiction.entityId }
            ContradictionSummary(
                entityName = entity?.primaryName ?: "Unknown",
                type = contradiction.type.name,
                severity = contradiction.severity.name,
                description = contradiction.description
            )
        }

        return ReportPreview(
            caseName = case.name,
            totalEvidence = summary.totalEvidence,
            entitiesFound = summary.entitiesFound,
            totalContradictions = summary.totalContradictions,
            criticalContradictions = summary.criticalContradictions,
            highestLiabilityEntity = summary.highestLiabilityEntity,
            highestLiabilityScore = summary.highestLiabilityScore,
            entitySummaries = entitySummaries,
            contradictionSummaries = contradictionSummaries,
            narrative = case.narrative
        )
    }

    data class ReportPreview(
        val caseName: String,
        val totalEvidence: Int,
        val entitiesFound: Int,
        val totalContradictions: Int,
        val criticalContradictions: Int,
        val highestLiabilityEntity: String?,
        val highestLiabilityScore: Float,
        val entitySummaries: List<EntitySummary>,
        val contradictionSummaries: List<ContradictionSummary>,
        val narrative: String
    )

    data class EntitySummary(
        val name: String,
        val aliases: List<String>,
        val statementCount: Int,
        val liabilityScore: Float
    )

    data class ContradictionSummary(
        val entityName: String,
        val type: String,
        val severity: String,
        val description: String
    )

    sealed class GenerationState {
        object Idle : GenerationState()
        data class Generating(val message: String) : GenerationState()
        object PreviewReady : GenerationState()
        data class Complete(val file: File) : GenerationState()
        data class Error(val message: String) : GenerationState()
    }
}

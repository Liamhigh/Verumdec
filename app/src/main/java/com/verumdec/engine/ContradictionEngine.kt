package com.verumdec.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import com.verumdec.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Main Contradiction Engine
 * Orchestrates the full forensic analysis pipeline with Constitution enforcement.
 * 
 * Pipeline Stages:
 * 1. Evidence Ingestion (PDF, Image, Text, Email, WhatsApp)
 * 2. Entity Discovery (Names, Emails, Phones, Companies)
 * 3. Timeline Generation (Chronological event ordering)
 * 4. Contradiction Analysis (Truth engine)
 * 5. Behavioral Analysis (Pattern detection)
 * 6. Liability Matrix (Mathematical scoring)
 * 7. Narrative Generation (Five layers)
 * 8. Report Sealing (SHA-512 + PDF)
 * 9. Constitution Enforcement (Validation at each step)
 */
class ContradictionEngine(private val context: Context) {

    companion object {
        private const val TAG = "ContradictionEngine"
    }

    private val evidenceProcessor = EvidenceProcessor(context)
    private val entityDiscovery = EntityDiscovery()
    private val timelineGenerator = TimelineGenerator()
    private val contradictionAnalyzer = ContradictionAnalyzer()
    private val behavioralAnalyzer = BehavioralAnalyzer()
    private val liabilityCalculator = LiabilityCalculator()
    private val narrativeGenerator = NarrativeGenerator()
    private val reportGenerator = ReportGenerator(context)

    /**
     * Analysis progress listener.
     */
    interface ProgressListener {
        fun onProgressUpdate(stage: AnalysisStage, progress: Int, message: String)
        fun onComplete(case: Case)
        fun onError(error: String)
        fun onConstitutionWarning(warning: String) {} // Optional callback for constitution warnings
    }

    enum class AnalysisStage {
        VALIDATING_INPUT,
        PROCESSING_EVIDENCE,
        DISCOVERING_ENTITIES,
        GENERATING_TIMELINE,
        ANALYZING_CONTRADICTIONS,
        ANALYZING_BEHAVIOR,
        CALCULATING_LIABILITY,
        GENERATING_NARRATIVE,
        VALIDATING_OUTPUT,
        COMPLETE
    }

    /**
     * Run the full analysis pipeline with Constitution enforcement.
     */
    suspend fun analyze(
        case: Case,
        evidenceUris: Map<String, Uri>,
        listener: ProgressListener
    ): Case = withContext(Dispatchers.IO) {
        try {
            var currentCase = case
            
            // Stage 0: Validate Input (Constitution Check)
            listener.onProgressUpdate(AnalysisStage.VALIDATING_INPUT, 0, "Validating input...")
            for (evidence in currentCase.evidence) {
                val validationResult = ConstitutionEnforcer.validateEvidence(evidence)
                if (!validationResult.isValid) {
                    val errorMsg = validationResult.violations
                        .filter { it.severity == ConstitutionEnforcer.ViolationSeverity.CRITICAL }
                        .joinToString("\n") { it.description }
                    throw ConstitutionViolationException("Evidence validation failed: $errorMsg")
                }
                validationResult.warnings.forEach { listener.onConstitutionWarning(it) }
            }
            listener.onProgressUpdate(AnalysisStage.VALIDATING_INPUT, 100, "Input validated")

            // Stage 1: Process Evidence
            listener.onProgressUpdate(AnalysisStage.PROCESSING_EVIDENCE, 0, "Processing evidence files...")
            val processedEvidence = mutableListOf<Evidence>()
            val totalEvidence = currentCase.evidence.size
            
            for ((index, evidence) in currentCase.evidence.withIndex()) {
                val uri = evidenceUris[evidence.id] ?: continue
                val processed = evidenceProcessor.processEvidence(evidence, uri)
                processedEvidence.add(processed)
                
                val progress = ((index + 1) * 100 / totalEvidence)
                listener.onProgressUpdate(
                    AnalysisStage.PROCESSING_EVIDENCE, 
                    progress, 
                    "Processed ${index + 1}/$totalEvidence files"
                )
            }
            
            currentCase = currentCase.copy(evidence = processedEvidence.toMutableList())

            // Stage 2: Discover Entities
            listener.onProgressUpdate(AnalysisStage.DISCOVERING_ENTITIES, 0, "Discovering entities...")
            val entities = entityDiscovery.discoverEntities(processedEvidence)
            currentCase = currentCase.copy(entities = entities.toMutableList())
            
            // Constitution check for entities
            val entityValidation = ConstitutionEnforcer.validateEntities(entities, processedEvidence)
            entityValidation.warnings.forEach { listener.onConstitutionWarning(it) }
            
            listener.onProgressUpdate(
                AnalysisStage.DISCOVERING_ENTITIES, 
                100, 
                "Found ${entities.size} entities"
            )

            // Stage 3: Generate Timeline
            listener.onProgressUpdate(AnalysisStage.GENERATING_TIMELINE, 0, "Building timeline...")
            val timeline = timelineGenerator.generateTimeline(processedEvidence, entities)
            currentCase = currentCase.copy(timeline = timeline.toMutableList())
            
            // Constitution check for timeline
            val timelineValidation = ConstitutionEnforcer.validateTimeline(timeline, processedEvidence)
            timelineValidation.warnings.forEach { listener.onConstitutionWarning(it) }
            
            listener.onProgressUpdate(
                AnalysisStage.GENERATING_TIMELINE, 
                100, 
                "Generated ${timeline.size} timeline events"
            )

            // Stage 4: Analyze Contradictions
            listener.onProgressUpdate(AnalysisStage.ANALYZING_CONTRADICTIONS, 0, "Detecting contradictions...")
            val contradictions = contradictionAnalyzer.analyzeContradictions(processedEvidence, entities, timeline)
            currentCase = currentCase.copy(contradictions = contradictions.toMutableList())
            
            // Constitution check for contradictions
            val contradictionValidation = ConstitutionEnforcer.validateContradictions(contradictions, entities)
            contradictionValidation.warnings.forEach { listener.onConstitutionWarning(it) }
            
            listener.onProgressUpdate(
                AnalysisStage.ANALYZING_CONTRADICTIONS, 
                100, 
                "Found ${contradictions.size} contradictions"
            )

            // Stage 5: Analyze Behavior
            listener.onProgressUpdate(AnalysisStage.ANALYZING_BEHAVIOR, 0, "Analyzing behavioral patterns...")
            val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(processedEvidence, entities, timeline)
            
            // Constitution check for behavioral patterns
            val behaviorValidation = ConstitutionEnforcer.validateBehavioralPatterns(behavioralPatterns, entities)
            behaviorValidation.warnings.forEach { listener.onConstitutionWarning(it) }
            
            listener.onProgressUpdate(
                AnalysisStage.ANALYZING_BEHAVIOR, 
                100, 
                "Detected ${behavioralPatterns.size} behavioral patterns"
            )

            // Stage 6: Calculate Liability
            listener.onProgressUpdate(AnalysisStage.CALCULATING_LIABILITY, 0, "Calculating liability scores...")
            val liabilityScores = liabilityCalculator.calculateLiability(
                entities, contradictions, behavioralPatterns, processedEvidence, timeline
            )
            currentCase = currentCase.copy(liabilityScores = liabilityScores.toMutableMap())
            
            // Constitution check for liability scores
            val liabilityValidation = ConstitutionEnforcer.validateLiabilityScores(
                liabilityScores, entities, contradictions, behavioralPatterns
            )
            liabilityValidation.warnings.forEach { listener.onConstitutionWarning(it) }
            
            listener.onProgressUpdate(
                AnalysisStage.CALCULATING_LIABILITY, 
                100, 
                "Calculated scores for ${liabilityScores.size} entities"
            )

            // Stage 7: Generate Narrative
            listener.onProgressUpdate(AnalysisStage.GENERATING_NARRATIVE, 0, "Generating narrative...")
            val narrativeSections = narrativeGenerator.generateNarrative(
                entities, timeline, contradictions, behavioralPatterns, liabilityScores
            )
            currentCase = currentCase.copy(narrative = narrativeSections.finalSummary)
            listener.onProgressUpdate(
                AnalysisStage.GENERATING_NARRATIVE, 
                100, 
                "Narrative generated"
            )
            
            // Stage 8: Validate Output (Final Constitution Check)
            listener.onProgressUpdate(AnalysisStage.VALIDATING_OUTPUT, 0, "Validating output...")
            val caseValidation = ConstitutionEnforcer.validateCase(currentCase)
            if (!caseValidation.isValid) {
                val criticalViolations = caseValidation.violations
                    .filter { it.severity == ConstitutionEnforcer.ViolationSeverity.CRITICAL }
                Log.w(TAG, "Constitution violations detected: ${criticalViolations.size}")
                criticalViolations.forEach { Log.w(TAG, "Violation: ${it.description}") }
            }
            caseValidation.warnings.forEach { listener.onConstitutionWarning(it) }
            listener.onProgressUpdate(AnalysisStage.VALIDATING_OUTPUT, 100, "Output validated")

            // Complete
            listener.onProgressUpdate(AnalysisStage.COMPLETE, 100, "Analysis complete!")
            listener.onComplete(currentCase)

            currentCase
        } catch (e: Exception) {
            listener.onError(e.message ?: "Unknown error occurred")
            throw e
        }
    }

    /**
     * Generate and export PDF report with Constitution validation.
     */
    suspend fun generateReport(case: Case): File = withContext(Dispatchers.IO) {
        // Validate case before report generation
        val caseValidation = ConstitutionEnforcer.validateCase(case)
        if (!caseValidation.isValid) {
            Log.w(TAG, "Constitution warnings during report generation:")
            caseValidation.violations.forEach { Log.w(TAG, it.description) }
        }
        
        val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(
            case.evidence, case.entities, case.timeline
        )
        
        val narrativeSections = narrativeGenerator.generateNarrative(
            case.entities, case.timeline, case.contradictions, 
            behavioralPatterns, case.liabilityScores
        )
        
        val report = reportGenerator.generateReport(
            caseName = case.name,
            entities = case.entities,
            timeline = case.timeline,
            contradictions = case.contradictions,
            behavioralPatterns = behavioralPatterns,
            liabilityScores = case.liabilityScores,
            narrativeSections = narrativeSections
        )
        
        // Validate report before sealing
        val reportValidation = ConstitutionEnforcer.validateReportForSealing(report)
        if (!reportValidation.isValid) {
            throw ConstitutionViolationException(
                "Report failed constitution validation: " +
                reportValidation.violations.joinToString("; ") { it.description }
            )
        }
        
        reportGenerator.exportToPdf(report)
    }

    /**
     * Get a quick summary of the case.
     */
    fun getSummary(case: Case): CaseSummary {
        val criticalContradictions = case.contradictions.count { it.severity == Severity.CRITICAL }
        val highContradictions = case.contradictions.count { it.severity == Severity.HIGH }
        
        val highestLiability = case.liabilityScores.maxByOrNull { it.value.overallScore }
        val highestLiabilityEntity = case.entities.find { it.id == highestLiability?.key }
        
        return CaseSummary(
            totalEvidence = case.evidence.size,
            processedEvidence = case.evidence.count { it.processed },
            entitiesFound = case.entities.size,
            timelineEvents = case.timeline.size,
            totalContradictions = case.contradictions.size,
            criticalContradictions = criticalContradictions,
            highContradictions = highContradictions,
            highestLiabilityEntity = highestLiabilityEntity?.primaryName,
            highestLiabilityScore = highestLiability?.value?.overallScore ?: 0f
        )
    }
}

/**
 * Summary data for quick display.
 */
data class CaseSummary(
    val totalEvidence: Int,
    val processedEvidence: Int,
    val entitiesFound: Int,
    val timelineEvents: Int,
    val totalContradictions: Int,
    val criticalContradictions: Int,
    val highContradictions: Int,
    val highestLiabilityEntity: String?,
    val highestLiabilityScore: Float
)

/**
 * Exception thrown when constitution validation fails.
 */
class ConstitutionViolationException(message: String) : Exception(message)

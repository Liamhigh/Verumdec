package com.verumdec.engine

import android.content.Context
import android.net.Uri
import com.verumdec.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Main Contradiction Engine
 * Orchestrates the full forensic analysis pipeline.
 */
class ContradictionEngine(private val context: Context) {

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
    }

    enum class AnalysisStage {
        PROCESSING_EVIDENCE,
        DISCOVERING_ENTITIES,
        GENERATING_TIMELINE,
        ANALYZING_CONTRADICTIONS,
        ANALYZING_BEHAVIOR,
        CALCULATING_LIABILITY,
        GENERATING_NARRATIVE,
        COMPLETE
    }

    /**
     * Run the full analysis pipeline.
     */
    suspend fun analyze(
        case: Case,
        evidenceUris: Map<String, Uri>,
        listener: ProgressListener
    ): Case = withContext(Dispatchers.IO) {
        try {
            var currentCase = case

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
            listener.onProgressUpdate(
                AnalysisStage.DISCOVERING_ENTITIES, 
                100, 
                "Found ${entities.size} entities"
            )

            // Stage 3: Generate Timeline
            listener.onProgressUpdate(AnalysisStage.GENERATING_TIMELINE, 0, "Building timeline...")
            val timeline = timelineGenerator.generateTimeline(processedEvidence, entities)
            currentCase = currentCase.copy(timeline = timeline.toMutableList())
            listener.onProgressUpdate(
                AnalysisStage.GENERATING_TIMELINE, 
                100, 
                "Generated ${timeline.size} timeline events"
            )

            // Stage 4: Analyze Contradictions
            listener.onProgressUpdate(AnalysisStage.ANALYZING_CONTRADICTIONS, 0, "Detecting contradictions...")
            val contradictions = contradictionAnalyzer.analyzeContradictions(processedEvidence, entities, timeline)
            currentCase = currentCase.copy(contradictions = contradictions.toMutableList())
            listener.onProgressUpdate(
                AnalysisStage.ANALYZING_CONTRADICTIONS, 
                100, 
                "Found ${contradictions.size} contradictions"
            )

            // Stage 5: Analyze Behavior
            listener.onProgressUpdate(AnalysisStage.ANALYZING_BEHAVIOR, 0, "Analyzing behavioral patterns...")
            val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(processedEvidence, entities, timeline)
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
     * Generate and export PDF report.
     */
    suspend fun generateReport(case: Case): File = withContext(Dispatchers.IO) {
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

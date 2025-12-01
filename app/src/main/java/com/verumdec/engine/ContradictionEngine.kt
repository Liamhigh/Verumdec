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
 * 
 * This engine now integrates the LEVELER ENGINE for unified analysis,
 * providing enhanced semantic contradiction detection, entity profiling,
 * and behavioral anomaly detection.
 */
class ContradictionEngine(private val context: Context?) {

    private val evidenceProcessor = context?.let { EvidenceProcessor(it) }
    private val entityDiscovery = EntityDiscovery()
    private val timelineGenerator = TimelineGenerator()
    private val contradictionAnalyzer = ContradictionAnalyzer()
    private val behavioralAnalyzer = BehavioralAnalyzer()
    private val liabilityCalculator = LiabilityCalculator()
    private val narrativeGenerator = NarrativeGenerator()
    private val reportGenerator = context?.let { ReportGenerator(it) }
    
    // LEVELER ENGINE - Unified analysis engine for enhanced contradiction detection
    private val levelerEngine = LevelerEngine(context)
    
    // Store the latest Leveler output for report generation
    private var lastLevelerOutput: LevelerOutput? = null

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
        RUNNING_LEVELER_ENGINE,
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
                val processor = evidenceProcessor ?: continue
                val processed = processor.processEvidence(evidence, uri)
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

            // Stage 4: Analyze Contradictions (basic analyzer)
            listener.onProgressUpdate(AnalysisStage.ANALYZING_CONTRADICTIONS, 0, "Detecting contradictions...")
            val basicContradictions = contradictionAnalyzer.analyzeContradictions(processedEvidence, entities, timeline)
            listener.onProgressUpdate(
                AnalysisStage.ANALYZING_CONTRADICTIONS, 
                50, 
                "Basic analysis found ${basicContradictions.size} contradictions"
            )

            // Stage 5: Run LEVELER ENGINE for enhanced analysis
            listener.onProgressUpdate(AnalysisStage.RUNNING_LEVELER_ENGINE, 0, "Running LEVELER ENGINE v${LevelerEngine.VERSION}...")
            val basicBehavioralPatterns = behavioralAnalyzer.analyzeBehavior(processedEvidence, entities, timeline)
            
            val levelerOutput = levelerEngine.process(
                evidence = processedEvidence,
                entities = entities,
                timeline = timeline,
                existingContradictions = basicContradictions,
                behavioralPatterns = basicBehavioralPatterns
            )
            lastLevelerOutput = levelerOutput
            
            // Use Leveler's enhanced contradictions and behavioral patterns
            val contradictions = levelerOutput.contradictions
            val behavioralPatterns = levelerOutput.behavioralPatterns
            
            currentCase = currentCase.copy(contradictions = contradictions.toMutableList())
            listener.onProgressUpdate(
                AnalysisStage.RUNNING_LEVELER_ENGINE, 
                100, 
                "LEVELER ENGINE complete: ${levelerOutput.statistics.levelerDetectedContradictions} additional contradictions detected"
            )

            // Stage 6: Analyze Behavior (already done by Leveler, just report)
            listener.onProgressUpdate(AnalysisStage.ANALYZING_BEHAVIOR, 100, 
                "Behavioral analysis complete: ${behavioralPatterns.size} patterns detected"
            )

            // Stage 7: Calculate Liability
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

            // Stage 8: Generate Narrative (with Leveler context)
            listener.onProgressUpdate(AnalysisStage.GENERATING_NARRATIVE, 0, "Generating narrative...")
            val narrativeSections = narrativeGenerator.generateNarrative(
                entities, timeline, contradictions, behavioralPatterns, liabilityScores
            )
            
            // Append Leveler analysis summary to the narrative
            val levelerSummary = buildLevelerNarrativeSummary(levelerOutput)
            val enhancedNarrative = narrativeSections.copy(
                finalSummary = "${narrativeSections.finalSummary}\n\n$levelerSummary"
            )
            
            currentCase = currentCase.copy(narrative = enhancedNarrative.finalSummary)
            listener.onProgressUpdate(
                AnalysisStage.GENERATING_NARRATIVE, 
                100, 
                "Narrative generated with LEVELER ENGINE insights"
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
     * Now includes LEVELER ENGINE analysis output in the report.
     */
    suspend fun generateReport(case: Case): File = withContext(Dispatchers.IO) {
        // Run Leveler if not already run
        val levelerOutput = lastLevelerOutput ?: run {
            val basicBehavioralPatterns = behavioralAnalyzer.analyzeBehavior(
                case.evidence, case.entities, case.timeline
            )
            levelerEngine.process(
                evidence = case.evidence,
                entities = case.entities,
                timeline = case.timeline,
                existingContradictions = case.contradictions,
                behavioralPatterns = basicBehavioralPatterns
            )
        }
        
        // Use Leveler-enhanced behavioral patterns
        val behavioralPatterns = levelerOutput.behavioralPatterns
        
        // Generate narrative with Leveler insights
        val baseNarrativeSections = narrativeGenerator.generateNarrative(
            case.entities, case.timeline, levelerOutput.contradictions, 
            behavioralPatterns, case.liabilityScores
        )
        
        // Enhance narrative with Leveler analysis summary
        val levelerSummary = buildLevelerNarrativeSummary(levelerOutput)
        val enhancedNarrativeSections = baseNarrativeSections.copy(
            finalSummary = "${baseNarrativeSections.finalSummary}\n\n$levelerSummary"
        )
        
        val report = reportGenerator?.generateReport(
            caseName = case.name,
            entities = case.entities,
            timeline = case.timeline,
            contradictions = levelerOutput.contradictions,
            behavioralPatterns = behavioralPatterns,
            liabilityScores = case.liabilityScores,
            narrativeSections = enhancedNarrativeSections
        ) ?: throw IllegalStateException("ReportGenerator not initialized")
        
        reportGenerator.exportToPdf(report)
    }
    
    /**
     * Build a narrative summary section from the Leveler output.
     */
    private fun buildLevelerNarrativeSummary(output: LevelerOutput): String {
        val sb = StringBuilder()
        sb.appendLine("═══════════════════════════════════════════════════════════════")
        sb.appendLine("LEVELER ENGINE ANALYSIS REPORT")
        sb.appendLine("═══════════════════════════════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("Engine: ${output.engineName} v${output.engineVersion}")
        sb.appendLine("Analysis Completed: ${output.processedAt}")
        sb.appendLine("Scan Hash: ${output.scanHash}")
        sb.appendLine()
        sb.appendLine("ANALYSIS STATISTICS:")
        sb.appendLine("─────────────────────────────────────────────────────────────────")
        sb.appendLine("  • Total Statements Indexed: ${output.statistics.totalStatements}")
        sb.appendLine("  • Entity Profiles Built: ${output.statistics.totalEntities}")
        sb.appendLine("  • Timeline Events Analyzed: ${output.statistics.totalTimelineEvents}")
        sb.appendLine()
        sb.appendLine("CONTRADICTION DETECTION:")
        sb.appendLine("─────────────────────────────────────────────────────────────────")
        sb.appendLine("  • Total Contradictions: ${output.statistics.totalContradictions}")
        sb.appendLine("  • LEVELER-Detected: ${output.statistics.levelerDetectedContradictions}")
        sb.appendLine("  • Semantic Contradictions: ${output.statistics.semanticContradictions}")
        sb.appendLine("  • Financial Discrepancies: ${output.statistics.financialContradictions}")
        sb.appendLine()
        sb.appendLine("SEVERITY BREAKDOWN:")
        sb.appendLine("─────────────────────────────────────────────────────────────────")
        sb.appendLine("  • CRITICAL: ${output.statistics.criticalSeverityCount}")
        sb.appendLine("  • HIGH: ${output.statistics.highSeverityCount}")
        sb.appendLine("  • MEDIUM: ${output.statistics.mediumSeverityCount}")
        sb.appendLine("  • LOW: ${output.statistics.lowSeverityCount}")
        sb.appendLine()
        sb.appendLine("BEHAVIORAL ANALYSIS:")
        sb.appendLine("─────────────────────────────────────────────────────────────────")
        sb.appendLine("  • Behavioral Patterns Detected: ${output.statistics.totalBehavioralPatterns}")
        sb.appendLine("  • Entities with Contradictions: ${output.statistics.entitiesWithContradictions}")
        sb.appendLine("  • Avg Statements per Entity: ${String.format("%.1f", output.statistics.averageStatementsPerEntity)}")
        sb.appendLine()
        sb.appendLine("ANALYSIS LOG:")
        sb.appendLine("─────────────────────────────────────────────────────────────────")
        output.analysisLog.forEach { sb.appendLine("  $it") }
        sb.appendLine()
        sb.appendLine("═══════════════════════════════════════════════════════════════")
        sb.appendLine("END OF LEVELER ENGINE REPORT")
        sb.appendLine("═══════════════════════════════════════════════════════════════")
        
        return sb.toString()
    }
    
    /**
     * Get the last Leveler output (useful for debugging/inspection).
     */
    fun getLastLevelerOutput(): LevelerOutput? = lastLevelerOutput

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

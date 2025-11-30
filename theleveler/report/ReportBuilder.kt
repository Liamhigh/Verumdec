package ai.verum.theleveler.report

import ai.verum.theleveler.analysis.ContradictionEngine
import ai.verum.theleveler.entity.EntityProfiler
import ai.verum.theleveler.narrative.NarrativeEngine
import ai.verum.theleveler.timeline.TimelineBuilder

/**
 * Public entry point for the Leveler engine.
 * Orchestrates all modules to produce a complete report.
 */
object ReportBuilder {

    /**
     * Generate a complete Leveler report from raw text.
     */
    fun generate(rawText: String, sourceId: String? = null): LevelerReport {
        // Step 1: Run the contradiction engine
        val engineOutput = ContradictionEngine.analyse(rawText, sourceId)
        
        // Step 2: Profile entities
        val profiles = EntityProfiler.profileActors(engineOutput.parsed.statements)
        
        // Step 3: Build timeline summary
        val timelineSummary = TimelineBuilder.getSummary(engineOutput.parsed.timelineEvents)
        
        // Step 4: Generate narrative
        val narrative = NarrativeEngine.generate(
            statements = engineOutput.parsed.statements,
            timelineEvents = engineOutput.parsed.timelineEvents,
            report = engineOutput.report,
            profiles = profiles
        )
        
        return LevelerReport(
            narrative = narrative,
            contradictionReport = engineOutput.report,
            entityProfiles = profiles,
            timelineSummary = timelineSummary,
            statistics = ReportStatistics(
                totalStatements = engineOutput.parsed.statements.size,
                totalActors = engineOutput.parsed.actors.size,
                totalContradictions = engineOutput.report.contradictions.size,
                totalBehaviourShifts = engineOutput.report.behaviourShifts.size,
                totalTimelineEvents = engineOutput.parsed.timelineEvents.size
            )
        )
    }

    /**
     * Generate report from multiple documents.
     */
    fun generateFromMultiple(documents: Map<String, String>): LevelerReport {
        // Combine and analyse
        val engineOutput = ContradictionEngine.analyseMultipleDocuments(documents)
        
        // Profile entities
        val profiles = EntityProfiler.profileActors(engineOutput.parsed.statements)
        
        // Build timeline summary
        val timelineSummary = TimelineBuilder.getSummary(engineOutput.parsed.timelineEvents)
        
        // Generate narrative
        val narrative = NarrativeEngine.generate(
            statements = engineOutput.parsed.statements,
            timelineEvents = engineOutput.parsed.timelineEvents,
            report = engineOutput.report,
            profiles = profiles
        )
        
        return LevelerReport(
            narrative = narrative,
            contradictionReport = engineOutput.report,
            entityProfiles = profiles,
            timelineSummary = timelineSummary,
            statistics = ReportStatistics(
                totalStatements = engineOutput.parsed.statements.size,
                totalActors = engineOutput.parsed.actors.size,
                totalContradictions = engineOutput.report.contradictions.size,
                totalBehaviourShifts = engineOutput.report.behaviourShifts.size,
                totalTimelineEvents = engineOutput.parsed.timelineEvents.size
            )
        )
    }
}

/**
 * Complete Leveler report.
 */
data class LevelerReport(
    val narrative: NarrativeEngine.Narrative,
    val contradictionReport: ai.verum.theleveler.core.ContradictionReport,
    val entityProfiles: Map<String, EntityProfiler.EntityProfile>,
    val timelineSummary: TimelineBuilder.TimelineSummary,
    val statistics: ReportStatistics
) {
    /**
     * Get the full narrative text.
     */
    fun getFullText(): String = narrative.toFullText()
    
    /**
     * Check if any contradictions were found.
     */
    fun hasContradictions(): Boolean = contradictionReport.contradictions.isNotEmpty()
    
    /**
     * Get high confidence contradictions only.
     */
    fun getHighConfidenceContradictions() = 
        contradictionReport.contradictions.filter { it.confidence >= 0.7 }
}

/**
 * Report statistics.
 */
data class ReportStatistics(
    val totalStatements: Int,
    val totalActors: Int,
    val totalContradictions: Int,
    val totalBehaviourShifts: Int,
    val totalTimelineEvents: Int
)

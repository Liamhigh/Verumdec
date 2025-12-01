package ai.verum.theleveler.report

import ai.verum.theleveler.analysis.ContradictionEngine
import ai.verum.theleveler.entity.EntityProfile
import ai.verum.theleveler.entity.EntityProfiler
import ai.verum.theleveler.narrative.NarrativeEngine
import ai.verum.theleveler.timeline.TimelineBuilder

/**
 * The final entry point for Leveler reports.
 * 
 * Usage:
 *   ReportBuilder.generate(fullText)
 */
object ReportBuilder {

    /**
     * Generate a complete Leveler report from raw text.
     */
    fun generate(rawText: String, sourceId: String? = null): LevelerReport {
        // Step 1: Run the contradiction engine
        val engineOutput = ContradictionEngine.analyse(rawText, sourceId)
        
        // Step 2: Profile entities
        val profiles = EntityProfiler.profile(engineOutput.parsed.statements)
        val profilesMap = profiles.associateBy { it.actor.normalized }
        
        // Step 3: Generate narrative
        val narrative = NarrativeEngine.generate(
            statements = engineOutput.parsed.statements,
            timelineEvents = engineOutput.parsed.timelineEvents,
            report = engineOutput.report,
            profiles = profilesMap
        )
        
        return LevelerReport(
            narrative = narrative.toFullText(),
            contradictions = engineOutput.report.contradictions.size,
            behaviourShifts = engineOutput.report.behaviourShifts.size,
            actors = profiles.size
        )
    }
}

/**
 * Complete Leveler report output.
 */
data class LevelerReport(
    val narrative: String,
    val contradictions: Int,
    val behaviourShifts: Int,
    val actors: Int
)

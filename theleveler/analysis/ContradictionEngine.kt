package ai.verum.theleveler.analysis

import ai.verum.theleveler.core.*
import ai.verum.theleveler.extraction.DocumentParser

/**
 * Master Contradiction Engine.
 * Orchestrates all analysis passes and produces a unified ContradictionReport.
 */
object ContradictionEngine {

    /**
     * Engine output containing parsed data and contradiction report.
     */
    data class EngineOutput(
        val parsed: DocumentParser.ParseResult,
        val report: ContradictionReport
    )

    /**
     * Analyse raw text and produce a full contradiction report.
     */
    fun analyse(rawText: String, sourceId: String? = null): EngineOutput {
        // Step 1: Parse the document
        val parsed = DocumentParser.parse(rawText, sourceId)
        
        // Step 2: Run all analysis passes
        val report = analyseStatements(parsed.statements, parsed.timelineEvents)
        
        return EngineOutput(parsed, report)
    }

    /**
     * Analyse pre-parsed statements and timeline events.
     */
    fun analyseStatements(
        statements: List<Statement>,
        timelineEvents: List<TimelineEvent>
    ): ContradictionReport {
        val allContradictions = mutableListOf<Contradiction>()
        
        // Pass 1: Statement-to-statement comparison
        val statementContradictions = StatementComparer.compareStatements(statements)
        allContradictions.addAll(statementContradictions)
        
        // Pass 2: Timeline comparison
        val timelineContradictions = TimelineComparer.compareTimeline(timelineEvents)
        allContradictions.addAll(timelineContradictions)
        
        // Pass 3: Behaviour analysis
        val behaviourShifts = BehaviourAnalyser.analyseStatements(statements)
        
        // Convert significant behaviour shifts to contradictions
        val behaviourContradictions = behaviourShifts
            .filter { it.shiftType in listOf(
                BehaviourShiftType.CERTAINTY_DROP,
                BehaviourShiftType.EVASIVE_TONE
            )}
            .map { shift ->
                Contradiction(
                    actor = shift.actor,
                    firstStatement = shift.fromStatement,
                    secondStatement = shift.toStatement,
                    type = ContradictionType.BEHAVIOURAL_SHIFT,
                    confidence = 0.6,
                    explanation = shift.description
                )
            }
        allContradictions.addAll(behaviourContradictions)
        
        // Deduplicate and sort by confidence
        val uniqueContradictions = deduplicateContradictions(allContradictions)
            .sortedByDescending { it.confidence }
        
        return ContradictionReport(
            contradictions = uniqueContradictions,
            behaviourShifts = behaviourShifts
        )
    }

    /**
     * Analyse multiple documents together.
     */
    fun analyseMultipleDocuments(documents: Map<String, String>): EngineOutput {
        val allStatements = mutableListOf<Statement>()
        val allEvents = mutableListOf<TimelineEvent>()
        val allActors = mutableSetOf<Actor>()
        
        for ((sourceId, text) in documents) {
            val parsed = DocumentParser.parse(text, sourceId)
            allStatements.addAll(parsed.statements)
            allEvents.addAll(parsed.timelineEvents)
            allActors.addAll(parsed.actors)
        }
        
        val report = analyseStatements(allStatements, allEvents)
        
        val combinedParsed = DocumentParser.ParseResult(
            statements = allStatements,
            timelineEvents = allEvents,
            actors = allActors
        )
        
        return EngineOutput(combinedParsed, report)
    }

    /**
     * Remove duplicate contradictions.
     */
    private fun deduplicateContradictions(contradictions: List<Contradiction>): List<Contradiction> {
        val seen = mutableSetOf<String>()
        val unique = mutableListOf<Contradiction>()
        
        for (c in contradictions) {
            // Create a key based on actor and statement texts
            val key = "${c.actor.normalized}|${c.firstStatement.text.hashCode()}|${c.secondStatement.text.hashCode()}"
            val reverseKey = "${c.actor.normalized}|${c.secondStatement.text.hashCode()}|${c.firstStatement.text.hashCode()}"
            
            if (key !in seen && reverseKey !in seen) {
                seen.add(key)
                unique.add(c)
            }
        }
        
        return unique
    }
}

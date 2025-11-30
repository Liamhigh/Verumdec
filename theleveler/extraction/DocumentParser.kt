package ai.verum.theleveler.extraction

import ai.verum.theleveler.core.Actor
import ai.verum.theleveler.core.Statement
import ai.verum.theleveler.core.TimelineEvent

/**
 * Unified document parser that extracts statements and timeline events.
 */
object DocumentParser {

    data class ParseResult(
        val statements: List<Statement>,
        val timelineEvents: List<TimelineEvent>,
        val actors: Set<Actor>
    )

    fun parse(text: String, sourceId: String? = null): ParseResult {
        val statements = StatementExtractor.extract(text)
            .map { it.copy(sourceId = sourceId) }
        
        val timelineEvents = TimelineExtractor.extract(text)
            .map { it.copy(sourceId = sourceId) }
        
        val actors = statements.map { it.actor }.toSet()
        
        return ParseResult(
            statements = statements,
            timelineEvents = timelineEvents,
            actors = actors
        )
    }
}

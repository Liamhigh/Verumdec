package ai.verum.theleveler.timeline

import ai.verum.theleveler.core.TimelineEvent
import ai.verum.theleveler.extraction.DateNormalizer

/**
 * Builds and sorts timeline from extracted events.
 * If all timestamps exist, sorts chronologically.
 * Otherwise preserves extraction order.
 */
object TimelineBuilder {

    /**
     * Build a sorted timeline from events.
     */
    fun build(events: List<TimelineEvent>): List<TimelineEvent> {
        if (events.isEmpty()) return emptyList()
        
        // Check if all events have valid timestamps
        val allHaveTimestamps = events.all { 
            it.timestamp != null && DateNormalizer.normalize(it.timestamp) != null 
        }
        
        return if (allHaveTimestamps) {
            // Sort by normalized timestamp
            events.sortedWith { e1, e2 ->
                DateNormalizer.compare(e1.timestamp, e2.timestamp)
            }
        } else {
            // Preserve extraction order, but try to sort those with timestamps
            val withTimestamp = events.filter { 
                it.timestamp != null && DateNormalizer.normalize(it.timestamp) != null 
            }.sortedWith { e1, e2 ->
                DateNormalizer.compare(e1.timestamp, e2.timestamp)
            }
            
            val withoutTimestamp = events.filter { 
                it.timestamp == null || DateNormalizer.normalize(it.timestamp) == null 
            }
            
            // Interleave: timestamped events first, then non-timestamped
            withTimestamp + withoutTimestamp
        }
    }

    /**
     * Build timeline grouped by actor.
     */
    fun buildByActor(events: List<TimelineEvent>): Map<String, List<TimelineEvent>> {
        val byActor = events
            .filter { it.actor != null }
            .groupBy { it.actor!!.normalized }
        
        return byActor.mapValues { (_, actorEvents) -> build(actorEvents) }
    }

    /**
     * Get timeline summary with date range.
     */
    fun getSummary(events: List<TimelineEvent>): TimelineSummary {
        val sorted = build(events)
        
        val firstDate = sorted.firstOrNull()?.timestamp?.let { DateNormalizer.normalize(it) }
        val lastDate = sorted.lastOrNull()?.timestamp?.let { DateNormalizer.normalize(it) }
        
        val actors = sorted.mapNotNull { it.actor?.rawName }.distinct()
        
        return TimelineSummary(
            eventCount = sorted.size,
            firstDate = firstDate,
            lastDate = lastDate,
            actors = actors,
            events = sorted
        )
    }

    /**
     * Timeline summary data class.
     */
    data class TimelineSummary(
        val eventCount: Int,
        val firstDate: String?,
        val lastDate: String?,
        val actors: List<String>,
        val events: List<TimelineEvent>
    )
}

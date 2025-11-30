package com.verumdec.timeline.generator

import java.util.*

/**
 * MasterTimelineBuilder - Builds comprehensive event timelines.
 */
class MasterTimelineBuilder {

    /**
     * Build a master timeline from events.
     */
    fun build(events: List<TimelineEventInput>): MasterTimeline {
        val sorted = events.sortedBy { it.timestamp }
        
        return MasterTimeline(
            id = UUID.randomUUID().toString(),
            events = sorted,
            eventCount = sorted.size,
            startDate = sorted.firstOrNull()?.timestamp,
            endDate = sorted.lastOrNull()?.timestamp,
            builtAt = Date()
        )
    }

    /**
     * Merge multiple timelines.
     */
    fun merge(timelines: List<MasterTimeline>): MasterTimeline {
        val allEvents = timelines.flatMap { it.events }
        return build(allEvents)
    }

    /**
     * Filter timeline by entity.
     */
    fun filterByEntity(timeline: MasterTimeline, entityId: String): MasterTimeline {
        val filtered = timeline.events.filter { entityId in it.entityIds }
        return timeline.copy(
            events = filtered,
            eventCount = filtered.size
        )
    }

    /**
     * Filter timeline by event type.
     */
    fun filterByType(timeline: MasterTimeline, eventType: String): MasterTimeline {
        val filtered = timeline.events.filter { it.eventType == eventType }
        return timeline.copy(
            events = filtered,
            eventCount = filtered.size
        )
    }

    /**
     * Filter timeline by date range.
     */
    fun filterByDateRange(
        timeline: MasterTimeline,
        startDate: Date,
        endDate: Date
    ): MasterTimeline {
        val filtered = timeline.events.filter { 
            it.timestamp.after(startDate) && it.timestamp.before(endDate)
        }
        return timeline.copy(
            events = filtered,
            eventCount = filtered.size,
            startDate = startDate,
            endDate = endDate
        )
    }
}

data class TimelineEventInput(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Date,
    val description: String,
    val eventType: String,
    val entityIds: List<String>,
    val sourceEvidenceId: String,
    val significance: String = "NORMAL"
)

data class MasterTimeline(
    val id: String,
    val events: List<TimelineEventInput>,
    val eventCount: Int,
    val startDate: Date?,
    val endDate: Date?,
    val builtAt: Date
)

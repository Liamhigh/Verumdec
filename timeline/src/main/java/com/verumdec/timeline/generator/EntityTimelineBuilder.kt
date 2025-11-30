package com.verumdec.timeline.generator

import java.util.*

/**
 * EntityTimelineBuilder - Builds per-entity timelines.
 */
class EntityTimelineBuilder {

    /**
     * Build timelines for all entities.
     */
    fun buildAll(
        masterTimeline: MasterTimeline,
        entityIds: List<String>
    ): Map<String, EntityTimeline> {
        return entityIds.associateWith { entityId ->
            build(masterTimeline, entityId)
        }
    }

    /**
     * Build timeline for a single entity.
     */
    fun build(masterTimeline: MasterTimeline, entityId: String): EntityTimeline {
        val entityEvents = masterTimeline.events.filter { entityId in it.entityIds }
        val sorted = entityEvents.sortedBy { it.timestamp }

        return EntityTimeline(
            id = UUID.randomUUID().toString(),
            entityId = entityId,
            events = sorted,
            eventCount = sorted.size,
            firstAppearance = sorted.firstOrNull()?.timestamp,
            lastAppearance = sorted.lastOrNull()?.timestamp,
            activityLevel = calculateActivityLevel(sorted, masterTimeline),
            builtAt = Date()
        )
    }

    /**
     * Analyze entity activity patterns.
     */
    fun analyzeActivity(timeline: EntityTimeline): EntityActivityAnalysis {
        val events = timeline.events
        if (events.size < 2) {
            return EntityActivityAnalysis(
                entityId = timeline.entityId,
                totalEvents = events.size,
                averageEventsPerDay = 0f,
                peakActivity = null,
                quietPeriods = emptyList()
            )
        }

        val sorted = events.sortedBy { it.timestamp }
        val durationMs = sorted.last().timestamp.time - sorted.first().timestamp.time
        val durationDays = durationMs / (1000 * 60 * 60 * 24).toFloat()
        val avgPerDay = if (durationDays > 0) events.size / durationDays else events.size.toFloat()

        // Group by day for peak analysis
        val byDay = events.groupBy { event ->
            val cal = Calendar.getInstance()
            cal.time = event.timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.time
        }
        val peak = byDay.maxByOrNull { it.value.size }

        // Find quiet periods (gaps > 7 days)
        val quietPeriods = mutableListOf<QuietPeriod>()
        for (i in 1 until sorted.size) {
            val gapMs = sorted[i].timestamp.time - sorted[i - 1].timestamp.time
            val gapDays = gapMs / (1000 * 60 * 60 * 24)
            if (gapDays >= 7) {
                quietPeriods.add(QuietPeriod(
                    startDate = sorted[i - 1].timestamp,
                    endDate = sorted[i].timestamp,
                    durationDays = gapDays.toInt()
                ))
            }
        }

        return EntityActivityAnalysis(
            entityId = timeline.entityId,
            totalEvents = events.size,
            averageEventsPerDay = avgPerDay,
            peakActivity = peak?.let { PeakActivity(it.key, it.value.size) },
            quietPeriods = quietPeriods
        )
    }

    private fun calculateActivityLevel(
        events: List<TimelineEventInput>,
        masterTimeline: MasterTimeline
    ): ActivityLevel {
        if (masterTimeline.eventCount == 0) return ActivityLevel.NONE
        
        val ratio = events.size.toFloat() / masterTimeline.eventCount
        return when {
            ratio >= 0.5 -> ActivityLevel.DOMINANT
            ratio >= 0.3 -> ActivityLevel.HIGH
            ratio >= 0.15 -> ActivityLevel.MEDIUM
            ratio >= 0.05 -> ActivityLevel.LOW
            else -> ActivityLevel.MINIMAL
        }
    }
}

data class EntityTimeline(
    val id: String,
    val entityId: String,
    val events: List<TimelineEventInput>,
    val eventCount: Int,
    val firstAppearance: Date?,
    val lastAppearance: Date?,
    val activityLevel: ActivityLevel,
    val builtAt: Date
)

enum class ActivityLevel {
    NONE, MINIMAL, LOW, MEDIUM, HIGH, DOMINANT
}

data class EntityActivityAnalysis(
    val entityId: String,
    val totalEvents: Int,
    val averageEventsPerDay: Float,
    val peakActivity: PeakActivity?,
    val quietPeriods: List<QuietPeriod>
)

data class PeakActivity(
    val date: Date,
    val eventCount: Int
)

data class QuietPeriod(
    val startDate: Date,
    val endDate: Date,
    val durationDays: Int
)

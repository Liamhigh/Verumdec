package com.verumdec.core.brain

import android.content.Context
import com.verumdec.core.util.DateUtils
import com.verumdec.core.util.HashUtils
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * TimelineBrain - Builds and analyzes chronological timelines.
 * Implements clustering, gap analysis, and temporal pattern detection.
 */
class TimelineBrain(context: Context) : BaseBrain(context) {

    override val brainName = "TimelineBrain"

    /**
     * Build timeline from events.
     */
    fun buildTimeline(events: List<TimelineEvent>): BrainResult<Timeline> {
        val metadata = mapOf(
            "eventCount" to events.size,
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(events, "BUILD_TIMELINE", metadata) { eventList ->
            val sortedEvents = eventList.sortedBy { it.timestamp }
            val clustered = clusterEvents(sortedEvents)
            val gaps = analyzeGaps(sortedEvents)
            
            Timeline(
                id = generateProcessingId(),
                events = sortedEvents,
                eventCount = sortedEvents.size,
                clusters = clustered,
                gaps = gaps,
                startDate = sortedEvents.firstOrNull()?.timestamp,
                endDate = sortedEvents.lastOrNull()?.timestamp,
                totalDurationMs = calculateDuration(sortedEvents),
                builtAt = Date()
            )
        }
    }

    /**
     * Analyze timeline for patterns.
     */
    fun analyzePatterns(timeline: Timeline): BrainResult<TimelinePatternAnalysis> {
        val metadata = mapOf(
            "timelineId" to timeline.id,
            "eventCount" to timeline.eventCount,
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(timeline, "ANALYZE_PATTERNS", metadata) { tl ->
            val activityPeaks = findActivityPeaks(tl.events)
            val quietPeriods = findQuietPeriods(tl.events)
            val regularPatterns = detectRegularPatterns(tl.events)
            
            TimelinePatternAnalysis(
                id = generateProcessingId(),
                timelineId = tl.id,
                activityPeaks = activityPeaks,
                quietPeriods = quietPeriods,
                regularPatterns = regularPatterns,
                hasUnusualGaps = tl.gaps.any { it.isUnusual },
                averageEventFrequency = calculateAverageFrequency(tl.events),
                analyzedAt = Date()
            )
        }
    }

    /**
     * Merge multiple timelines.
     */
    fun mergeTimelines(timelines: List<Timeline>): BrainResult<Timeline> {
        val metadata = mapOf(
            "timelineCount" to timelines.size,
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(timelines, "MERGE_TIMELINES", metadata) { tls ->
            val allEvents = tls.flatMap { it.events }.sortedBy { it.timestamp }
            val deduplicated = deduplicateEvents(allEvents)
            val clustered = clusterEvents(deduplicated)
            val gaps = analyzeGaps(deduplicated)
            
            Timeline(
                id = generateProcessingId(),
                events = deduplicated,
                eventCount = deduplicated.size,
                clusters = clustered,
                gaps = gaps,
                startDate = deduplicated.firstOrNull()?.timestamp,
                endDate = deduplicated.lastOrNull()?.timestamp,
                totalDurationMs = calculateDuration(deduplicated),
                builtAt = Date()
            )
        }
    }

    /**
     * Generate entity-specific timeline.
     */
    fun generateEntityTimeline(
        timeline: Timeline,
        entityId: String
    ): BrainResult<EntityTimeline> {
        val metadata = mapOf(
            "timelineId" to timeline.id,
            "entityId" to entityId,
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(Pair(timeline, entityId), "ENTITY_TIMELINE", metadata) { (tl, entity) ->
            val entityEvents = tl.events.filter { entity in it.entityIds }
            val sortedEvents = entityEvents.sortedBy { it.timestamp }
            
            EntityTimeline(
                id = generateProcessingId(),
                entityId = entity,
                events = sortedEvents,
                eventCount = sortedEvents.size,
                firstAppearance = sortedEvents.firstOrNull()?.timestamp,
                lastAppearance = sortedEvents.lastOrNull()?.timestamp,
                activityLevel = calculateActivityLevel(sortedEvents, tl.totalDurationMs),
                generatedAt = Date()
            )
        }
    }

    /**
     * Cluster events by time proximity.
     */
    private fun clusterEvents(events: List<TimelineEvent>): List<EventCluster> {
        if (events.isEmpty()) return emptyList()
        
        val clusters = mutableListOf<EventCluster>()
        var currentCluster = mutableListOf<TimelineEvent>()
        val clusterThresholdMs = TimeUnit.HOURS.toMillis(24) // 24 hour window
        
        for (event in events) {
            if (currentCluster.isEmpty()) {
                currentCluster.add(event)
            } else {
                val lastEvent = currentCluster.last()
                val timeDiff = event.timestamp.time - lastEvent.timestamp.time
                
                if (timeDiff <= clusterThresholdMs) {
                    currentCluster.add(event)
                } else {
                    // Save current cluster and start new one
                    if (currentCluster.isNotEmpty()) {
                        clusters.add(createCluster(currentCluster))
                    }
                    currentCluster = mutableListOf(event)
                }
            }
        }
        
        // Add final cluster
        if (currentCluster.isNotEmpty()) {
            clusters.add(createCluster(currentCluster))
        }
        
        return clusters
    }

    private fun createCluster(events: List<TimelineEvent>): EventCluster {
        val sortedEvents = events.sortedBy { it.timestamp }
        val entityIds = events.flatMap { it.entityIds }.distinct()
        val eventTypes = events.map { it.type }.distinct()
        
        return EventCluster(
            id = HashUtils.generateHashId("cluster"),
            events = sortedEvents,
            eventCount = events.size,
            startTime = sortedEvents.first().timestamp,
            endTime = sortedEvents.last().timestamp,
            durationMs = sortedEvents.last().timestamp.time - sortedEvents.first().timestamp.time,
            involvedEntityIds = entityIds,
            eventTypes = eventTypes,
            significance = calculateClusterSignificance(events)
        )
    }

    private fun calculateClusterSignificance(events: List<TimelineEvent>): ClusterSignificance {
        val highSignificanceCount = events.count { it.significance == EventSignificance.HIGH }
        val criticalCount = events.count { it.significance == EventSignificance.CRITICAL }
        
        return when {
            criticalCount > 0 -> ClusterSignificance.CRITICAL
            highSignificanceCount >= 3 -> ClusterSignificance.HIGH
            events.size >= 5 -> ClusterSignificance.MEDIUM
            else -> ClusterSignificance.LOW
        }
    }

    /**
     * Analyze gaps in timeline.
     */
    private fun analyzeGaps(events: List<TimelineEvent>): List<TimelineGap> {
        if (events.size < 2) return emptyList()
        
        val gaps = mutableListOf<TimelineGap>()
        val sortedEvents = events.sortedBy { it.timestamp }
        
        // Calculate average gap to determine what's unusual
        val allGaps = mutableListOf<Long>()
        for (i in 1 until sortedEvents.size) {
            val gap = sortedEvents[i].timestamp.time - sortedEvents[i - 1].timestamp.time
            allGaps.add(gap)
        }
        
        val averageGap = if (allGaps.isNotEmpty()) allGaps.average() else 0.0
        val unusualThreshold = averageGap * 3 // 3x average is unusual
        
        for (i in 1 until sortedEvents.size) {
            val gapMs = sortedEvents[i].timestamp.time - sortedEvents[i - 1].timestamp.time
            val gapDays = TimeUnit.MILLISECONDS.toDays(gapMs)
            
            if (gapDays >= 1) { // Only track gaps of 1+ days
                val isUnusual = gapMs > unusualThreshold
                
                gaps.add(TimelineGap(
                    id = HashUtils.generateHashId("gap"),
                    startTime = sortedEvents[i - 1].timestamp,
                    endTime = sortedEvents[i].timestamp,
                    durationMs = gapMs,
                    durationDays = gapDays.toInt(),
                    isUnusual = isUnusual,
                    precedingEvent = sortedEvents[i - 1],
                    followingEvent = sortedEvents[i]
                ))
            }
        }
        
        return gaps
    }

    private fun calculateDuration(events: List<TimelineEvent>): Long {
        if (events.size < 2) return 0
        val sorted = events.sortedBy { it.timestamp }
        return sorted.last().timestamp.time - sorted.first().timestamp.time
    }

    private fun findActivityPeaks(events: List<TimelineEvent>): List<ActivityPeak> {
        if (events.isEmpty()) return emptyList()
        
        val peaks = mutableListOf<ActivityPeak>()
        val sortedEvents = events.sortedBy { it.timestamp }
        
        // Group by day
        val byDay = sortedEvents.groupBy { 
            val cal = Calendar.getInstance()
            cal.time = it.timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.time
        }
        
        val averagePerDay = byDay.values.map { it.size }.average()
        
        for ((date, dayEvents) in byDay) {
            if (dayEvents.size > averagePerDay * 1.5) {
                peaks.add(ActivityPeak(
                    date = date,
                    eventCount = dayEvents.size,
                    peakIntensity = dayEvents.size / averagePerDay.toFloat(),
                    eventTypes = dayEvents.map { it.type }.distinct()
                ))
            }
        }
        
        return peaks.sortedByDescending { it.eventCount }
    }

    private fun findQuietPeriods(events: List<TimelineEvent>): List<QuietPeriod> {
        val gaps = analyzeGaps(events)
        return gaps.filter { it.isUnusual }.map { gap ->
            QuietPeriod(
                startDate = gap.startTime,
                endDate = gap.endTime,
                durationDays = gap.durationDays,
                precedingContext = gap.precedingEvent.description,
                followingContext = gap.followingEvent.description
            )
        }
    }

    private fun detectRegularPatterns(events: List<TimelineEvent>): List<RegularPattern> {
        // Simplified pattern detection
        val patterns = mutableListOf<RegularPattern>()
        
        val byDayOfWeek = events.groupBy { 
            val cal = Calendar.getInstance()
            cal.time = it.timestamp
            cal.get(Calendar.DAY_OF_WEEK)
        }
        
        // Check for day-of-week patterns
        for ((day, dayEvents) in byDayOfWeek) {
            if (dayEvents.size >= 3) {
                patterns.add(RegularPattern(
                    patternType = "Weekly on ${getDayName(day)}",
                    frequency = dayEvents.size,
                    confidence = calculatePatternConfidence(dayEvents.size, events.size)
                ))
            }
        }
        
        return patterns.sortedByDescending { it.confidence }
    }

    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Unknown"
        }
    }

    private fun calculatePatternConfidence(patternCount: Int, totalEvents: Int): Float {
        if (totalEvents == 0) return 0f
        return (patternCount.toFloat() / totalEvents).coerceAtMost(1f)
    }

    private fun calculateAverageFrequency(events: List<TimelineEvent>): Float {
        if (events.size < 2) return 0f
        val sorted = events.sortedBy { it.timestamp }
        val durationDays = TimeUnit.MILLISECONDS.toDays(
            sorted.last().timestamp.time - sorted.first().timestamp.time
        ).toFloat()
        return if (durationDays > 0) events.size / durationDays else events.size.toFloat()
    }

    private fun calculateActivityLevel(events: List<TimelineEvent>, totalDurationMs: Long): ActivityLevel {
        if (events.isEmpty() || totalDurationMs <= 0) return ActivityLevel.NONE
        
        val durationDays = TimeUnit.MILLISECONDS.toDays(totalDurationMs).toFloat()
        val frequency = if (durationDays > 0) events.size / durationDays else 0f
        
        return when {
            frequency >= 5 -> ActivityLevel.VERY_HIGH
            frequency >= 2 -> ActivityLevel.HIGH
            frequency >= 0.5 -> ActivityLevel.MEDIUM
            frequency >= 0.1 -> ActivityLevel.LOW
            else -> ActivityLevel.NONE
        }
    }

    private fun deduplicateEvents(events: List<TimelineEvent>): List<TimelineEvent> {
        val seen = mutableSetOf<String>()
        return events.filter { event ->
            val key = "${event.timestamp.time}-${event.description.take(50)}"
            if (key in seen) {
                false
            } else {
                seen.add(key)
                true
            }
        }
    }
}

// Data classes

data class TimelineEvent(
    val id: String,
    val timestamp: Date,
    val description: String,
    val type: TimelineEventType,
    val entityIds: List<String>,
    val sourceEvidenceId: String,
    val significance: EventSignificance = EventSignificance.NORMAL
)

enum class TimelineEventType {
    COMMUNICATION, PAYMENT, PROMISE, DOCUMENT, MEETING, DEADLINE, 
    CONTRADICTION, ADMISSION, DENIAL, BEHAVIOR_CHANGE, OTHER
}

enum class EventSignificance {
    LOW, NORMAL, HIGH, CRITICAL
}

enum class ClusterSignificance {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class ActivityLevel {
    NONE, LOW, MEDIUM, HIGH, VERY_HIGH
}

data class Timeline(
    val id: String,
    val events: List<TimelineEvent>,
    val eventCount: Int,
    val clusters: List<EventCluster>,
    val gaps: List<TimelineGap>,
    val startDate: Date?,
    val endDate: Date?,
    val totalDurationMs: Long,
    val builtAt: Date
)

data class EventCluster(
    val id: String,
    val events: List<TimelineEvent>,
    val eventCount: Int,
    val startTime: Date,
    val endTime: Date,
    val durationMs: Long,
    val involvedEntityIds: List<String>,
    val eventTypes: List<TimelineEventType>,
    val significance: ClusterSignificance
)

data class TimelineGap(
    val id: String,
    val startTime: Date,
    val endTime: Date,
    val durationMs: Long,
    val durationDays: Int,
    val isUnusual: Boolean,
    val precedingEvent: TimelineEvent,
    val followingEvent: TimelineEvent
)

data class EntityTimeline(
    val id: String,
    val entityId: String,
    val events: List<TimelineEvent>,
    val eventCount: Int,
    val firstAppearance: Date?,
    val lastAppearance: Date?,
    val activityLevel: ActivityLevel,
    val generatedAt: Date
)

data class TimelinePatternAnalysis(
    val id: String,
    val timelineId: String,
    val activityPeaks: List<ActivityPeak>,
    val quietPeriods: List<QuietPeriod>,
    val regularPatterns: List<RegularPattern>,
    val hasUnusualGaps: Boolean,
    val averageEventFrequency: Float,
    val analyzedAt: Date
)

data class ActivityPeak(
    val date: Date,
    val eventCount: Int,
    val peakIntensity: Float,
    val eventTypes: List<TimelineEventType>
)

data class QuietPeriod(
    val startDate: Date,
    val endDate: Date,
    val durationDays: Int,
    val precedingContext: String,
    val followingContext: String
)

data class RegularPattern(
    val patternType: String,
    val frequency: Int,
    val confidence: Float
)

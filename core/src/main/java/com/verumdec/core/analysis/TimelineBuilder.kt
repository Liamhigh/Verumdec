package com.verumdec.core.analysis

import com.verumdec.core.extraction.DateNormalizer
import com.verumdec.core.model.EventType
import com.verumdec.core.model.Statement
import com.verumdec.core.model.TimelineEvent
import java.util.UUID

/**
 * TimelineBuilder constructs a chronological timeline from statements and events.
 * 
 * Features:
 * - Builds TimelineEvent objects from statements with timestamps
 * - Normalizes all timestamps to YYYY-MM-DD format before insertion
 * - Sorts events chronologically
 * - Detects timeline gaps and anomalies
 */
object TimelineBuilder {

    /**
     * Build a timeline from a list of statements.
     *
     * @param statements List of statements with timestamps
     * @return List of TimelineEvent objects sorted chronologically
     */
    fun build(statements: List<Statement>): List<TimelineEvent> {
        val events = mutableListOf<TimelineEvent>()
        
        for (statement in statements) {
            // Only create events for statements with timestamps
            val normalizedTimestamp = getNormalizedTimestamp(statement)
            val timestampMillis = getTimestampMillis(statement)
            
            if (normalizedTimestamp != null && timestampMillis != null) {
                events.add(
                    TimelineEvent(
                        id = UUID.randomUUID().toString(),
                        timestamp = normalizedTimestamp,
                        timestampMillis = timestampMillis,
                        description = statement.text.take(200),
                        type = classifyEventType(statement),
                        entityIds = listOf(statement.speaker),
                        statementIds = listOf(statement.id),
                        documentId = statement.documentId,
                        confidence = calculateConfidence(statement),
                        rawDateText = statement.timestamp ?: ""
                    )
                )
            }
        }
        
        // Sort by timestamp millis
        return events.sortedBy { it.timestampMillis }
    }

    /**
     * Build a timeline for a specific entity (speaker).
     *
     * @param statements All statements
     * @param entityId Entity identifier to filter by
     * @return List of TimelineEvent objects for the entity, sorted chronologically
     */
    fun buildForEntity(statements: List<Statement>, entityId: String): List<TimelineEvent> {
        val entityStatements = statements.filter { 
            it.speaker.equals(entityId, ignoreCase = true) 
        }
        return build(entityStatements)
    }

    /**
     * Merge multiple timelines into one.
     *
     * @param timelines List of timelines to merge
     * @return Merged and sorted timeline
     */
    fun merge(timelines: List<List<TimelineEvent>>): List<TimelineEvent> {
        return timelines.flatten()
            .sortedBy { it.timestampMillis }
            .distinctBy { it.id }
    }

    /**
     * Get normalized timestamp from statement.
     * Ensures timestamp is in YYYY-MM-DD format.
     */
    private fun getNormalizedTimestamp(statement: Statement): String? {
        // If already has a string timestamp, normalize it
        if (statement.timestamp != null) {
            val normalized = DateNormalizer.normalize(statement.timestamp)
            if (normalized != null) return normalized
        }
        
        // If has millis, convert to string
        if (statement.timestampMillis != null) {
            return DateNormalizer.fromMillis(statement.timestampMillis)
        }
        
        return null
    }

    /**
     * Get timestamp in milliseconds from statement.
     */
    private fun getTimestampMillis(statement: Statement): Long? {
        // If already has millis, use it
        if (statement.timestampMillis != null) {
            return statement.timestampMillis
        }
        
        // If has string timestamp, convert to millis
        if (statement.timestamp != null) {
            return DateNormalizer.toMillis(DateNormalizer.normalize(statement.timestamp))
        }
        
        return null
    }

    /**
     * Classify the event type based on statement content.
     */
    private fun classifyEventType(statement: Statement): EventType {
        val text = statement.text.lowercase()
        
        return when {
            text.contains("paid") || text.contains("payment") || text.contains("transfer") || text.contains("$") -> EventType.PAYMENT
            text.contains("requested") || text.contains("asked") || text.contains("need") -> EventType.REQUEST
            text.contains("promised") || text.contains("will") || text.contains("commit") -> EventType.PROMISE
            text.contains("agreed") || text.contains("contract") || text.contains("signed") -> EventType.AGREEMENT
            text.contains("met") || text.contains("meeting") || text.contains("discussed") -> EventType.MEETING
            text.contains("email") || text.contains("called") || text.contains("messaged") -> EventType.COMMUNICATION
            text.contains("sued") || text.contains("filed") || text.contains("court") || text.contains("legal") -> EventType.LEGAL_ACTION
            text.contains("deadline") || text.contains("due") || text.contains("by the") -> EventType.DEADLINE
            text.contains("said") || text.contains("stated") || text.contains("claimed") -> EventType.STATEMENT
            else -> EventType.OTHER
        }
    }

    /**
     * Calculate confidence score for the timestamp.
     */
    private fun calculateConfidence(statement: Statement): Double {
        var confidence = 0.5 // Base confidence
        
        // Higher confidence if we have both string and millis
        if (statement.timestamp != null && statement.timestampMillis != null) {
            confidence += 0.3
        }
        
        // Higher confidence if timestamp is in normalized format
        if (statement.timestamp != null && DateNormalizer.isNormalized(statement.timestamp)) {
            confidence += 0.2
        }
        
        return confidence.coerceIn(0.0, 1.0)
    }

    /**
     * Find gaps in a timeline (periods with no events).
     *
     * @param events Timeline events
     * @param minGapDays Minimum number of days to consider a gap
     * @return List of gap periods as (start timestamp, end timestamp, days)
     */
    fun findGaps(events: List<TimelineEvent>, minGapDays: Long = 30): List<Triple<String, String, Long>> {
        if (events.size < 2) return emptyList()
        
        val gaps = mutableListOf<Triple<String, String, Long>>()
        val sorted = events.sortedBy { it.timestampMillis }
        
        for (i in 0 until sorted.size - 1) {
            val current = sorted[i]
            val next = sorted[i + 1]
            
            val daysDiff = DateNormalizer.daysDifference(current.timestamp, next.timestamp)
            if (daysDiff != null && daysDiff >= minGapDays) {
                gaps.add(Triple(current.timestamp ?: "", next.timestamp ?: "", daysDiff))
            }
        }
        
        return gaps
    }

    /**
     * Find events that cluster around a specific date.
     *
     * @param events Timeline events
     * @param targetDate Date to search around (YYYY-MM-DD format)
     * @param windowDays Number of days before/after to include
     * @return List of events within the window
     */
    fun findEventsAroundDate(events: List<TimelineEvent>, targetDate: String, windowDays: Long = 7): List<TimelineEvent> {
        return events.filter { event ->
            val diff = DateNormalizer.daysDifference(event.timestamp, targetDate)
            diff != null && diff <= windowDays
        }
    }

    /**
     * Get the date range of a timeline.
     *
     * @param events Timeline events
     * @return Pair of (earliest date, latest date) or null if empty
     */
    fun getDateRange(events: List<TimelineEvent>): Pair<String, String>? {
        if (events.isEmpty()) return null
        
        val sorted = events.sortedBy { it.timestampMillis }
        val earliest = sorted.first().timestamp ?: return null
        val latest = sorted.last().timestamp ?: return null
        
        return Pair(earliest, latest)
    }
}

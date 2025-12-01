package com.verumdec.timeline.detection

import com.verumdec.core.model.Contradiction
import com.verumdec.core.model.ContradictionType
import com.verumdec.core.model.EventType
import com.verumdec.core.model.LegalTrigger
import com.verumdec.core.model.Statement
import com.verumdec.core.model.TimelineConflict
import com.verumdec.core.model.TimelineConflictType
import com.verumdec.core.model.TimelineEvent
import java.util.UUID
import kotlin.math.abs

/**
 * TimelineContradictionDetector handles timeline-based contradiction detection.
 *
 * Rules:
 * - If two statements refer to events with impossible or inconsistent order, flag them
 * - Detect missing dates, unclear dates, or contradictory dates
 * - If an event references a future action that occurs before its cause, flag it
 * - Compare all timestamps to detect mismatches between documents
 */
class TimelineContradictionDetector {
    
    private val events = mutableListOf<TimelineEvent>()
    private val conflicts = mutableListOf<TimelineConflict>()
    
    /**
     * Check if timeline objects exist.
     */
    fun hasTimelineObjects(): Boolean = events.isNotEmpty()
    
    /**
     * Get all timeline events.
     */
    fun getAllEvents(): List<TimelineEvent> = events.toList()
    
    /**
     * Get all detected conflicts.
     */
    fun getConflicts(): List<TimelineConflict> = conflicts.toList()
    
    /**
     * Add a timeline event.
     */
    fun addEvent(event: TimelineEvent) {
        events.add(event)
    }
    
    /**
     * Build timeline events from statements.
     *
     * @param statements List of statements with timestamps
     */
    fun buildTimelineFromStatements(statements: List<Statement>) {
        for (statement in statements) {
            // Use timestampMillis for sorting, timestamp string for display
            val timestampMillis = statement.timestampMillis ?: continue
            val timestamp = statement.timestamp
            
            val event = TimelineEvent(
                timestamp = timestamp,
                timestampMillis = timestampMillis,
                description = statement.text.take(200),
                type = classifyEventType(statement),
                entityIds = listOf(statement.speaker),
                statementIds = listOf(statement.id),
                documentId = statement.documentId,
                confidence = 1.0 // Timestamp was verified non-null above
            )
            events.add(event)
        }
    }
    
    /**
     * Detect all timeline-based contradictions.
     *
     * @return List of timeline contradictions converted to standard Contradiction format
     */
    fun detectTimelineContradictions(): List<Contradiction> {
        conflicts.clear()
        
        // Sort events by timestampMillis
        val sortedEvents = events.sortedBy { it.timestampMillis }
        
        // Detect causality violations
        detectCausalityViolations(sortedEvents)
        
        // Detect date mismatches between documents
        detectDateMismatches(sortedEvents)
        
        // Detect impossible sequences
        detectImpossibleSequences(sortedEvents)
        
        // Convert conflicts to standard Contradiction format
        return conflicts.map { conflict ->
            Contradiction(
                type = ContradictionType.TIMELINE,
                sourceStatement = createStatementFromEvent(conflict.earlierEvent),
                targetStatement = createStatementFromEvent(conflict.laterEvent),
                sourceDocument = conflict.earlierEvent.documentId,
                sourceLineNumber = 0,
                severity = conflict.severity,
                description = conflict.description,
                legalTrigger = LegalTrigger.TIMELINE_INCONSISTENCY,
                affectedEntities = (conflict.earlierEvent.entityIds + conflict.laterEvent.entityIds).distinct()
            )
        }
    }
    
    /**
     * Detect causality violations where effects appear before causes.
     */
    private fun detectCausalityViolations(sortedEvents: List<TimelineEvent>) {
        // Patterns that indicate causal relationships
        val causalPatterns = listOf(
            CausalPattern("agreed to", "signed the agreement", "Agreement signed before discussed"),
            CausalPattern("promised to pay", "received payment", "Payment received before promise"),
            CausalPattern("requested", "delivered", "Delivery before request"),
            CausalPattern("filed lawsuit", "settled", "Settlement before lawsuit filed"),
            CausalPattern("sent invoice", "paid invoice", "Invoice paid before sent"),
            CausalPattern("made complaint", "resolved complaint", "Resolution before complaint")
        )
        
        for (i in sortedEvents.indices) {
            for (j in sortedEvents.indices) {
                if (i == j) continue
                
                val eventA = sortedEvents[i]
                val eventB = sortedEvents[j]
                
                for (pattern in causalPatterns) {
                    if (matchesCausalPattern(eventA, eventB, pattern)) {
                        // Effect appears before cause - this is a violation
                        if (eventB.timestampMillis < eventA.timestampMillis) {
                            conflicts.add(
                                TimelineConflict(
                                    earlierEvent = eventA,
                                    laterEvent = eventB,
                                    description = pattern.violationDescription + 
                                        ": '${eventB.description.take(50)}...' happened before " +
                                        "'${eventA.description.take(50)}...'",
                                    severity = 8,
                                    conflictType = TimelineConflictType.CAUSALITY_VIOLATION
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Detect date mismatches where the same event has different dates.
     */
    private fun detectDateMismatches(sortedEvents: List<TimelineEvent>) {
        // Group events by their descriptions (normalized)
        val eventsByDescription = events.groupBy { normalizeDescription(it.description) }
        
        for ((_, similarEvents) in eventsByDescription) {
            if (similarEvents.size >= 2) {
                // Check if timestamps differ significantly (more than 1 day)
                val timestamps = similarEvents.map { it.timestampMillis }.distinct()
                if (timestamps.size > 1) {
                    val minTime = timestamps.minOrNull() ?: continue
                    val maxTime = timestamps.maxOrNull() ?: continue
                    val diffDays = (maxTime - minTime) / (24 * 60 * 60 * 1000)
                    
                    if (diffDays > 0) {
                        val earlierEvent = similarEvents.minByOrNull { it.timestampMillis }!!
                        val laterEvent = similarEvents.maxByOrNull { it.timestampMillis }!!
                        
                        val severity = when {
                            diffDays > 30 -> 9
                            diffDays > 7 -> 7
                            diffDays > 1 -> 5
                            else -> 3
                        }
                        
                        conflicts.add(
                            TimelineConflict(
                                earlierEvent = earlierEvent,
                                laterEvent = laterEvent,
                                description = "Same event has different dates: " +
                                    "Document '${earlierEvent.documentId}' vs " +
                                    "Document '${laterEvent.documentId}' ($diffDays days difference)",
                                severity = severity,
                                conflictType = TimelineConflictType.DATE_MISMATCH
                            )
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Detect impossible sequences of events.
     */
    private fun detectImpossibleSequences(sortedEvents: List<TimelineEvent>) {
        // Check for events by the same entity that are impossibly close in time
        // but in different locations or contexts
        val eventsByEntity = events.groupBy { it.entityIds.firstOrNull() ?: "" }
        
        for ((entityId, entityEvents) in eventsByEntity) {
            if (entityId.isBlank() || entityEvents.size < 2) continue
            
            val sorted = entityEvents.sortedBy { it.timestampMillis }
            for (i in 0 until sorted.size - 1) {
                val eventA = sorted[i]
                val eventB = sorted[i + 1]
                
                // Check for impossibly rapid transitions
                val timeDiffMinutes = (eventB.timestampMillis - eventA.timestampMillis) / (60 * 1000)
                
                // If different documents claim events happened at nearly same time
                if (eventA.documentId != eventB.documentId && timeDiffMinutes < 5) {
                    val descA = eventA.description.lowercase()
                    val descB = eventB.description.lowercase()
                    
                    // Check if they seem to be different activities
                    if (!descA.contains(descB.take(20)) && !descB.contains(descA.take(20))) {
                        conflicts.add(
                            TimelineConflict(
                                earlierEvent = eventA,
                                laterEvent = eventB,
                                description = "Entity '$entityId' cannot have done both " +
                                    "'${eventA.description.take(40)}...' and " +
                                    "'${eventB.description.take(40)}...' within $timeDiffMinutes minutes",
                                severity = 6,
                                conflictType = TimelineConflictType.IMPOSSIBLE_SEQUENCE
                            )
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Check for contradictory date references between documents.
     *
     * @param documents Map of document IDs to their events
     * @return List of contradictory date conflicts
     */
    fun detectCrossDocumentDateConflicts(documents: Map<String, List<TimelineEvent>>): List<TimelineConflict> {
        val crossConflicts = mutableListOf<TimelineConflict>()
        val documentIds = documents.keys.toList()
        
        for (i in documentIds.indices) {
            for (j in i + 1 until documentIds.size) {
                val docAEvents = documents[documentIds[i]] ?: continue
                val docBEvents = documents[documentIds[j]] ?: continue
                
                // Compare events that reference similar content
                for (eventA in docAEvents) {
                    for (eventB in docBEvents) {
                        val similarity = calculateEventSimilarity(eventA, eventB)
                        if (similarity > 0.6) {
                            val timeDiff = abs(eventA.timestampMillis - eventB.timestampMillis)
                            val daysDiff = timeDiff / (24 * 60 * 60 * 1000)
                            
                            if (daysDiff > 1) {
                                crossConflicts.add(
                                    TimelineConflict(
                                        earlierEvent = if (eventA.timestampMillis < eventB.timestampMillis) eventA else eventB,
                                        laterEvent = if (eventA.timestampMillis >= eventB.timestampMillis) eventA else eventB,
                                        description = "Cross-document date conflict: Similar events " +
                                            "dated differently by $daysDiff days",
                                        severity = calculateDateConflictSeverity(daysDiff),
                                        conflictType = TimelineConflictType.CONTRADICTORY_DATES
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        
        return crossConflicts
    }
    
    /**
     * Build a master chronological timeline.
     *
     * @return Ordered list of events
     */
    fun buildMasterTimeline(): List<TimelineEvent> {
        return events.sortedBy { it.timestampMillis }
    }
    
    /**
     * Build a timeline for a specific entity.
     *
     * @param entityId Entity identifier
     * @return Ordered list of events for the entity
     */
    fun buildEntityTimeline(entityId: String): List<TimelineEvent> {
        return events
            .filter { it.entityIds.contains(entityId) }
            .sortedBy { it.timestampMillis }
    }
    
    /**
     * Classify event type based on statement content.
     */
    private fun classifyEventType(statement: Statement): EventType {
        val text = statement.text.lowercase()
        return when {
            text.contains("paid") || text.contains("payment") || text.contains("transfer") -> EventType.PAYMENT
            text.contains("requested") || text.contains("asked") -> EventType.REQUEST
            text.contains("promised") || text.contains("will") || text.contains("commit") -> EventType.PROMISE
            text.contains("agreed") || text.contains("contract") || text.contains("signed") -> EventType.AGREEMENT
            text.contains("met") || text.contains("meeting") -> EventType.MEETING
            text.contains("said") || text.contains("stated") || text.contains("claimed") -> EventType.STATEMENT
            text.contains("sued") || text.contains("filed") || text.contains("court") -> EventType.LEGAL_ACTION
            text.contains("deadline") || text.contains("due") -> EventType.DEADLINE
            else -> EventType.OTHER
        }
    }
    
    /**
     * Normalize description for comparison.
     */
    private fun normalizeDescription(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 3 }
            .sorted()
            .take(5)
            .joinToString(" ")
    }
    
    /**
     * Calculate similarity between two events.
     */
    private fun calculateEventSimilarity(eventA: TimelineEvent, eventB: TimelineEvent): Double {
        val wordsA = eventA.description.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        val wordsB = eventB.description.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        
        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0.0
        
        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size
        
        return intersection.toDouble() / union.toDouble()
    }
    
    /**
     * Calculate severity based on days difference.
     */
    private fun calculateDateConflictSeverity(daysDiff: Long): Int {
        return when {
            daysDiff > 365 -> 10
            daysDiff > 90 -> 9
            daysDiff > 30 -> 8
            daysDiff > 7 -> 6
            daysDiff > 1 -> 4
            else -> 2
        }
    }
    
    /**
     * Match causal pattern between events.
     */
    private fun matchesCausalPattern(
        eventA: TimelineEvent,
        eventB: TimelineEvent,
        pattern: CausalPattern
    ): Boolean {
        val textA = eventA.description.lowercase()
        val textB = eventB.description.lowercase()
        
        return (textA.contains(pattern.causePattern) && textB.contains(pattern.effectPattern))
    }
    
    /**
     * Create a Statement from a TimelineEvent.
     */
    private fun createStatementFromEvent(event: TimelineEvent): Statement {
        return Statement(
            id = event.statementIds.firstOrNull() ?: event.id,
            speaker = event.entityIds.firstOrNull() ?: "Unknown",
            text = event.description,
            documentId = event.documentId,
            documentName = event.documentId,
            lineNumber = 0,
            timestamp = event.timestamp,
            timestampMillis = event.timestampMillis
        )
    }
    
    /**
     * Clear all events and conflicts.
     */
    fun clear() {
        events.clear()
        conflicts.clear()
    }
    
    /**
     * Represents a causal relationship pattern.
     */
    private data class CausalPattern(
        val causePattern: String,
        val effectPattern: String,
        val violationDescription: String
    )
}

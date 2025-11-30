package com.verumdec.core.model

import java.util.UUID

/**
 * Represents a timeline event for chronological ordering.
 *
 * @property id Unique identifier for this event
 * @property timestamp When the event occurred
 * @property description Description of the event
 * @property type The category of event
 * @property entityIds Entities involved in this event
 * @property statementIds Associated statement IDs
 * @property documentId Source document identifier
 * @property confidence Confidence score for timestamp accuracy (0.0 to 1.0)
 * @property rawDateText Original date text from document
 */
data class TimelineEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val description: String,
    val type: EventType,
    val entityIds: List<String> = emptyList(),
    val statementIds: List<String> = emptyList(),
    val documentId: String,
    val confidence: Double = 1.0,
    val rawDateText: String = ""
)

/**
 * Types of timeline events.
 */
enum class EventType {
    STATEMENT,
    PAYMENT,
    REQUEST,
    PROMISE,
    AGREEMENT,
    MEETING,
    COMMUNICATION,
    LEGAL_ACTION,
    DEADLINE,
    CONTRADICTION,
    DOCUMENT_CREATED,
    OTHER
}

/**
 * Represents a timeline contradiction where events are in impossible order.
 *
 * @property id Unique identifier
 * @property earlierEvent The event that should come first chronologically
 * @property laterEvent The event that should come later chronologically
 * @property description Explanation of the timeline conflict
 * @property severity Severity score (1-10)
 * @property conflictType Type of timeline conflict
 */
data class TimelineConflict(
    val id: String = UUID.randomUUID().toString(),
    val earlierEvent: TimelineEvent,
    val laterEvent: TimelineEvent,
    val description: String,
    val severity: Int,
    val conflictType: TimelineConflictType
)

/**
 * Types of timeline conflicts.
 */
enum class TimelineConflictType {
    /** Event references future action before its cause */
    CAUSALITY_VIOLATION,
    /** Same event has different dates in different documents */
    DATE_MISMATCH,
    /** Missing date makes timeline unclear */
    MISSING_DATE,
    /** Contradictory date references */
    CONTRADICTORY_DATES,
    /** Impossible sequence of events */
    IMPOSSIBLE_SEQUENCE
}

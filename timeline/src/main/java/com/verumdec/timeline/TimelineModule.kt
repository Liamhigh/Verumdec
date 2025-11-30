package com.verumdec.timeline

import com.verumdec.core.model.Statement
import com.verumdec.core.model.TimelineConflict
import com.verumdec.core.model.TimelineEvent
import com.verumdec.timeline.detection.TimelineContradictionDetector

/**
 * Timeline Module - Event Chronologization and Timeline Generation
 *
 * This module handles event chronologization and timeline generation.
 * It normalizes timestamps and builds comprehensive timelines of events.
 *
 * ## Key Responsibilities:
 * - Normalize timestamps ("Last Friday" -> actual date)
 * - Build Master Chronological Timeline
 * - Generate Per-Entity Timelines
 * - Create Event-Type Timelines (Payments, Requests, Contradictions, Promises)
 * - Resolve relative dates based on document context
 *
 * ## Pipeline Stage: 3 - TIMELINE GENERATION (Core of the Narrative)
 * This creates the spine of the narrative through chronological ordering.
 *
 * ## Timeline-Based Contradiction Detection:
 * - If two statements refer to events with impossible or inconsistent order, flag them
 * - Detect missing dates, unclear dates, or contradictory dates
 * - If an event references a future action that occurs before its cause, flag it
 * - Compare all timestamps to detect mismatches between documents
 *
 * ## Timeline Types:
 * - Master Chronological Timeline (all events)
 * - Per-Entity Timeline (statements by each entity)
 * - Event-Type Timeline (categorized events)
 *
 * @see com.verumdec.core.CoreModule
 * @see com.verumdec.entity.EntityModule
 */
object TimelineModule {

    /**
     * Module version for tracking compatibility
     */
    const val VERSION = "2.0.0"

    /**
     * Module name identifier
     */
    const val NAME = "timeline"

    // Singleton detector instance
    private var contradictionDetector: TimelineContradictionDetector? = null

    /**
     * Initialize the Timeline module.
     * Creates the TimelineContradictionDetector.
     */
    fun initialize() {
        contradictionDetector = TimelineContradictionDetector()
    }

    /**
     * Get or create the TimelineContradictionDetector.
     *
     * @return TimelineContradictionDetector instance
     */
    fun getContradictionDetector(): TimelineContradictionDetector {
        if (contradictionDetector == null) {
            initialize()
        }
        return contradictionDetector!!
    }

    /**
     * Normalize a date reference to an actual date.
     *
     * @param dateReference Natural language date reference
     * @param contextDate Document date for relative resolution (milliseconds)
     * @return Normalized date timestamp (milliseconds)
     */
    fun normalizeDate(dateReference: String, contextDate: Long): Long {
        val lowerRef = dateReference.lowercase().trim()
        val dayMillis = 24 * 60 * 60 * 1000L
        val weekMillis = 7 * dayMillis
        
        return when {
            lowerRef.contains("yesterday") -> contextDate - dayMillis
            lowerRef.contains("today") -> contextDate
            lowerRef.contains("tomorrow") -> contextDate + dayMillis
            lowerRef.contains("last week") -> contextDate - weekMillis
            lowerRef.contains("next week") -> contextDate + weekMillis
            lowerRef.contains("last month") -> contextDate - (30 * dayMillis)
            lowerRef.contains("next month") -> contextDate + (30 * dayMillis)
            lowerRef.contains("last friday") -> findLastDayOfWeek(contextDate, 5)
            lowerRef.contains("last monday") -> findLastDayOfWeek(contextDate, 1)
            else -> contextDate // Default to context date if can't parse
        }
    }

    /**
     * Find the last occurrence of a specific day of the week.
     * If today is the target day, returns last week's occurrence.
     */
    private fun findLastDayOfWeek(fromDate: Long, targetDayOfWeek: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = fromDate
        val currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        var daysBack = currentDayOfWeek - targetDayOfWeek
        // If daysBack is 0 (same day) or negative, go back a full week
        // "Last Friday" on a Friday means the previous Friday, not today
        if (daysBack <= 0) daysBack += 7
        return fromDate - (daysBack * 24 * 60 * 60 * 1000L)
    }

    /**
     * Build timeline from statements.
     *
     * @param statements List of statements with timestamps
     */
    fun buildTimelineFromStatements(statements: List<Statement>) {
        getContradictionDetector().buildTimelineFromStatements(statements)
    }

    /**
     * Build a master chronological timeline from events.
     *
     * @return Ordered timeline
     */
    fun buildMasterTimeline(): List<TimelineEvent> {
        return getContradictionDetector().buildMasterTimeline()
    }

    /**
     * Build a timeline for a specific entity.
     *
     * @param entityId Entity identifier
     * @return Ordered list of events for the entity
     */
    fun buildEntityTimeline(entityId: String): List<TimelineEvent> {
        return getContradictionDetector().buildEntityTimeline(entityId)
    }

    /**
     * Detect timeline-based contradictions.
     *
     * @return List of detected contradictions
     */
    fun detectContradictions(): List<com.verumdec.core.model.Contradiction> {
        return getContradictionDetector().detectTimelineContradictions()
    }

    /**
     * Get all timeline conflicts.
     *
     * @return List of timeline conflicts
     */
    fun getTimelineConflicts(): List<TimelineConflict> {
        return getContradictionDetector().getConflicts()
    }

    /**
     * Get all timeline events.
     *
     * @return List of all events
     */
    fun getAllEvents(): List<TimelineEvent> {
        return getContradictionDetector().getAllEvents()
    }

    /**
     * Reset the module state for a new analysis.
     */
    fun reset() {
        contradictionDetector?.clear()
    }
}

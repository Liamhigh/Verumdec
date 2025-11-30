package com.verumdec.timeline

import android.content.Context
import com.verumdec.timeline.generator.MasterTimelineBuilder
import com.verumdec.timeline.generator.EntityTimelineBuilder
import com.verumdec.timeline.parser.DateNormalizer

/**
 * Timeline Module - Event Chronologization
 *
 * This module handles event chronologization and timeline generation.
 * It normalizes timestamps and builds comprehensive timelines of events.
 *
 * ## Key Features:
 * - Normalize timestamps ("Last Friday" -> actual date)
 * - Build Master Chronological Timeline
 * - Generate Per-Entity Timelines
 * - Create Event-Type Timelines
 * - Gap analysis and pattern detection
 */
object TimelineModule {

    const val VERSION = "1.0.0"
    const val NAME = "timeline"

    private var isInitialized = false
    private var appContext: Context? = null
    
    private var dateNormalizer: DateNormalizer? = null
    private var masterTimelineBuilder: MasterTimelineBuilder? = null
    private var entityTimelineBuilder: EntityTimelineBuilder? = null

    /**
     * Initialize the Timeline module.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        appContext = context.applicationContext
        dateNormalizer = DateNormalizer()
        masterTimelineBuilder = MasterTimelineBuilder()
        entityTimelineBuilder = EntityTimelineBuilder()
        
        isInitialized = true
    }

    /**
     * Get date normalizer.
     */
    fun getDateNormalizer(): DateNormalizer {
        return dateNormalizer ?: throw IllegalStateException("TimelineModule not initialized")
    }

    /**
     * Get master timeline builder.
     */
    fun getMasterTimelineBuilder(): MasterTimelineBuilder {
        return masterTimelineBuilder ?: throw IllegalStateException("TimelineModule not initialized")
    }

    /**
     * Get entity timeline builder.
     */
    fun getEntityTimelineBuilder(): EntityTimelineBuilder {
        return entityTimelineBuilder ?: throw IllegalStateException("TimelineModule not initialized")
    }
}

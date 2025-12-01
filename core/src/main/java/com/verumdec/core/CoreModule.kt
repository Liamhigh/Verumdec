package com.verumdec.core

/**
 * Core Module
 * -------------------------------------------
 * This module contains foundational data models and utilities
 * shared across all Verumdec modules.
 *
 * Key contents:
 * - Statement: Core data model for extracted statements
 * - Contradiction: Data model for detected contradictions
 * - TimelineEvent: Data model for timeline events
 * - StatementIndex: Indexing and search for statements
 * - ContradictionReport: Unified output model
 *
 * The Leveler orchestration class has been moved to the :app module
 * since it depends on all other modules (entity, timeline, analysis).
 */
object CoreModule {
    const val VERSION = "1.0.0"
    const val NAME = "Verumdec Core"
}

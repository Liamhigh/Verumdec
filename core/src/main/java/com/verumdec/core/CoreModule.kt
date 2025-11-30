package com.verumdec.core

import com.verumdec.core.model.Statement
import com.verumdec.core.model.StatementIndex

/**
 * Core Module - Foundation Layer
 *
 * This module provides the foundational components for the Verumdec offline contradiction engine.
 * It contains shared data models, utilities, and base interfaces used across all other modules.
 *
 * ## Key Responsibilities:
 * - Define common data models (Statement, StatementIndex, Contradiction, etc.)
 * - Provide shared utility functions and extensions
 * - Define base interfaces for pipeline stages
 * - Configuration constants and settings
 *
 * ## Pipeline Role:
 * This is the foundational layer that all other modules depend on.
 * It establishes the common vocabulary and data structures for the entire engine.
 *
 * ## Data Models:
 * - Statement: Individual statements with speaker, text, sentiment, certainty
 * - StatementIndex: Searchable index of all statements with semantic embeddings
 * - Contradiction: Detected contradiction with severity and legal triggers
 * - ContradictionReport: Unified report of all findings
 * - TimelineEvent: Chronological event representation
 * - TimelineConflict: Timeline-based contradiction
 * - BehavioralAnomaly: Detected behavioral pattern issue
 *
 * @see com.verumdec.analysis.AnalysisModule
 * @see com.verumdec.entity.EntityModule
 * @see com.verumdec.timeline.TimelineModule
 * @see com.verumdec.report.ReportModule
 */
object CoreModule {

    /**
     * Module version for tracking compatibility
     */
    const val VERSION = "2.0.0"

    /**
     * Module name identifier
     */
    const val NAME = "core"

    /**
     * Initialize the core module.
     * This should be called before using any other modules.
     */
    fun initialize() {
        // Core initialization complete
    }

    /**
     * Create a new StatementIndex.
     *
     * @return New StatementIndex instance
     */
    fun createStatementIndex(): StatementIndex {
        return StatementIndex()
    }

    /**
     * Create a Statement from basic components.
     *
     * @param speaker Who made the statement
     * @param text The statement text
     * @param documentId Source document identifier
     * @param documentName Human-readable document name
     * @param lineNumber Line number in document
     * @param timestamp Optional timestamp
     * @return New Statement instance
     */
    fun createStatement(
        speaker: String,
        text: String,
        documentId: String,
        documentName: String,
        lineNumber: Int,
        timestamp: Long? = null
    ): Statement {
        return Statement(
            speaker = speaker,
            text = text,
            documentId = documentId,
            documentName = documentName,
            lineNumber = lineNumber,
            timestamp = timestamp
        )
    }
}

package com.verumdec.analysis

import com.verumdec.analysis.engine.ContradictionEngine
import com.verumdec.analysis.engine.LinguisticDriftDetector
import com.verumdec.analysis.engine.SemanticEmbeddingGenerator
import com.verumdec.core.model.ContradictionReport
import com.verumdec.core.model.Statement

/**
 * Analysis Module - Core Truth Engine
 *
 * This module handles contradiction detection, behavioral analysis, and liability calculation.
 * It is the core "truth engine" of the Verumdec system.
 *
 * ## Key Responsibilities:
 * - Detect Direct Contradictions (A says X then NOT X)
 * - Detect Cross-Document Contradictions
 * - Detect Behavioral Contradictions (story shifts, tone changes)
 * - Detect Missing-Evidence Contradictions
 * - Calculate contradiction severity scores
 * - Analyze behavioral patterns (gaslighting, deflection, manipulation)
 * - Calculate liability scores for each entity
 *
 * ## Pipeline Stages:
 * - Stage 4: CONTRADICTION ANALYSIS (the truth engine)
 * - Stage 5: BEHAVIOURAL ANALYSIS
 * - Stage 6: LIABILITY MATRIX (Mathematical Scoring)
 *
 * ## Multi-Pass Contradiction Detection:
 * - Pass 1: Compare every statement against every other in same document
 * - Pass 2: Compare statements across imported documents
 * - Pass 3: Cross-modal contradiction checks (text vs timeline vs entities)
 * - Pass 4: Linguistic drift detection on each speaker's statements
 *
 * ## Contradiction Severity Levels:
 * - Critical (9-10): Flips liability
 * - High (7-8): Dishonest intent likely
 * - Medium (5-6): Unclear/error
 * - Low (1-4): Harmless inconsistency
 *
 * ## Behavioral Patterns Detected:
 * - Gaslighting
 * - Deflection
 * - Pressure tactics
 * - Financial/emotional manipulation
 * - Sudden withdrawal/ghosting
 * - Over-explaining (fraud red flag)
 * - Slip-up admissions
 * - Blame shifting
 *
 * @see com.verumdec.core.CoreModule
 * @see com.verumdec.entity.EntityModule
 * @see com.verumdec.timeline.TimelineModule
 */
object AnalysisModule {

    /**
     * Module version for tracking compatibility
     */
    const val VERSION = "2.0.0"

    /**
     * Module name identifier
     */
    const val NAME = "analysis"

    // Singleton engine instance
    private var contradictionEngine: ContradictionEngine? = null

    /**
     * Initialize the Analysis module.
     * Creates the ContradictionEngine and related components.
     */
    fun initialize() {
        contradictionEngine = ContradictionEngine()
    }

    /**
     * Get or create the ContradictionEngine.
     *
     * @return ContradictionEngine instance
     */
    fun getContradictionEngine(): ContradictionEngine {
        if (contradictionEngine == null) {
            initialize()
        }
        return contradictionEngine!!
    }

    /**
     * Create a new SemanticEmbeddingGenerator.
     *
     * @return New SemanticEmbeddingGenerator instance
     */
    fun createEmbeddingGenerator(): SemanticEmbeddingGenerator {
        return SemanticEmbeddingGenerator()
    }

    /**
     * Create a new LinguisticDriftDetector.
     *
     * @return New LinguisticDriftDetector instance
     */
    fun createLinguisticDriftDetector(): LinguisticDriftDetector {
        return LinguisticDriftDetector()
    }

    /**
     * Run full contradiction analysis on a set of statements.
     *
     * @param caseId Unique identifier for this analysis
     * @param statements List of statements to analyze
     * @return ContradictionReport with all findings
     */
    fun runFullAnalysis(caseId: String, statements: List<Statement>): ContradictionReport {
        val engine = getContradictionEngine()
        engine.reset()
        engine.indexStatements(statements)
        engine.generateEmbeddings()
        engine.buildEntityProfiles()
        engine.buildTimeline()
        return engine.runFullAnalysis(caseId)
    }

    /**
     * Calculate liability score for an entity based on their contradictions.
     *
     * @param entityId Entity identifier
     * @param report ContradictionReport from analysis
     * @return Liability score (0-100)
     */
    fun calculateLiability(entityId: String, report: ContradictionReport): Int {
        val involvement = report.affectedEntities[entityId]
        return involvement?.liabilityScore ?: 0
    }

    /**
     * Reset the module state for a new analysis.
     */
    fun reset() {
        contradictionEngine?.reset()
    }
}

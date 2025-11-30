package com.verumdec.analysis

/**
 * Analysis Module - Contradiction Detection and Behavioral Analysis
 *
 * This module handles the core "truth engine" functionality:
 * - Contradiction detection (direct, cross-document, temporal, behavioral)
 * - Behavioral pattern recognition (gaslighting, deflection, manipulation)
 * - Liability matrix calculation
 *
 * ## Pipeline Stages:
 * - Stage 4: CONTRADICTION ANALYSIS (the truth engine)
 * - Stage 5: BEHAVIOURAL ANALYSIS
 * - Stage 6: LIABILITY MATRIX (Mathematical Scoring)
 *
 * ## Contradiction Types:
 * - Direct: A says X, then A says NOT X
 * - Cross-Document: Different stories in different documents
 * - Temporal: Timeline inconsistencies
 * - Behavioral: Story shifts, tone changes
 * - Missing Evidence: References to non-existent proof
 * - Third-Party: Contradicted by another party
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
 * @see ContradictionDetector
 * @see BehavioralAnalyzer
 * @see LiabilityCalculator
 */
object AnalysisModule {

    /**
     * Module version for tracking compatibility
     */
    const val VERSION = "1.0.0"

    /**
     * Module name identifier
     */
    const val NAME = "analysis"
    
    /**
     * Severity levels for contradictions
     */
    enum class SeverityLevel(val weight: Float) {
        CRITICAL(1.0f),  // Flips liability
        HIGH(0.7f),      // Dishonest intent likely
        MEDIUM(0.4f),    // Unclear/error
        LOW(0.1f)        // Harmless inconsistency
    }
    
    /**
     * Liability calculation weights
     */
    object Weights {
        const val CONTRADICTION = 0.30f
        const val BEHAVIORAL = 0.20f
        const val EVIDENCE = 0.15f
        const val CONSISTENCY = 0.20f
        const val CAUSAL = 0.15f
    }

    /**
     * Initialize the Analysis module.
     */
    fun initialize() {
        // Analysis module initialization
    }

    /**
     * Get module information.
     */
    fun getInfo(): ModuleInfo {
        return ModuleInfo(
            name = NAME,
            version = VERSION,
            components = listOf("ContradictionDetector", "BehavioralAnalyzer", "LiabilityCalculator")
        )
    }
    
    data class ModuleInfo(
        val name: String,
        val version: String,
        val components: List<String>
    )
}

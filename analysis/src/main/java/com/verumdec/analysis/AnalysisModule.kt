package com.verumdec.analysis

import android.content.Context
import com.verumdec.analysis.engine.ContradictionDetector
import com.verumdec.analysis.engine.BehavioralAnalyzer
import com.verumdec.analysis.engine.LiabilityEngine

/**
 * Analysis Module - Core Truth Engine
 *
 * This module handles contradiction detection, behavioral analysis, and liability calculation.
 * It is the core "truth engine" of the Verumdec system.
 *
 * ## Key Features:
 * - Direct contradiction detection (A says X then NOT X)
 * - Cross-document contradiction detection
 * - Behavioral pattern analysis (gaslighting, deflection, manipulation)
 * - Temporal consistency checking
 * - Liability matrix calculation
 *
 * ## Contradiction Severity Levels:
 * - Critical: Flips liability
 * - High: Dishonest intent likely
 * - Medium: Unclear/error
 * - Low: Harmless inconsistency
 */
object AnalysisModule {

    const val VERSION = "1.0.0"
    const val NAME = "analysis"

    private var isInitialized = false
    private var appContext: Context? = null
    
    private var contradictionDetector: ContradictionDetector? = null
    private var behavioralAnalyzer: BehavioralAnalyzer? = null
    private var liabilityEngine: LiabilityEngine? = null

    /**
     * Initialize the Analysis module.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        appContext = context.applicationContext
        contradictionDetector = ContradictionDetector()
        behavioralAnalyzer = BehavioralAnalyzer()
        liabilityEngine = LiabilityEngine()
        
        isInitialized = true
    }

    /**
     * Get contradiction detector.
     */
    fun getContradictionDetector(): ContradictionDetector {
        return contradictionDetector ?: throw IllegalStateException("AnalysisModule not initialized")
    }

    /**
     * Get behavioral analyzer.
     */
    fun getBehavioralAnalyzer(): BehavioralAnalyzer {
        return behavioralAnalyzer ?: throw IllegalStateException("AnalysisModule not initialized")
    }

    /**
     * Get liability engine.
     */
    fun getLiabilityEngine(): LiabilityEngine {
        return liabilityEngine ?: throw IllegalStateException("AnalysisModule not initialized")
    }
}

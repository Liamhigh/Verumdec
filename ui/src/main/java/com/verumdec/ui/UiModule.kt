package com.verumdec.ui

/**
 * UI Module - Presentation Layer Components
 *
 * This module provides shared UI components and layouts.
 * It contains reusable views for the forensic analysis interface.
 *
 * ## Pipeline Stage: 9 - User Interaction Layer
 *
 * ## Components:
 * - Evidence list/grid views
 * - Timeline visualization
 * - Contradiction cards
 * - Liability score gauges
 * - Report preview components
 *
 * @see EvidenceAdapter
 * @see TimelineAdapter
 * @see ContradictionCard
 */
object UiModule {

    /**
     * Module version for tracking compatibility
     */
    const val VERSION = "1.0.0"

    /**
     * Module name identifier
     */
    const val NAME = "ui"

    /**
     * Initialize the UI module.
     */
    fun initialize() {
        // UI module initialization
    }

    /**
     * Get module information.
     */
    fun getInfo(): ModuleInfo {
        return ModuleInfo(
            name = NAME,
            version = VERSION,
            components = listOf("EvidenceAdapter", "TimelineAdapter", "ContradictionCard", "LiabilityGauge")
        )
    }
    
    data class ModuleInfo(
        val name: String,
        val version: String,
        val components: List<String>
    )
}

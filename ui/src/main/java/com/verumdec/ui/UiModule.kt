package com.verumdec.ui

import android.content.Context
import com.verumdec.ui.theme.VerumTheme
import com.verumdec.ui.components.ComponentRegistry

/**
 * UI Module - Presentation Layer
 *
 * This module provides the presentation layer, shared layouts, and reusable UI components.
 * It contains common UI elements used across the application.
 *
 * ## Key Features:
 * - Shared UI components and custom views
 * - Common layouts and themes
 * - Reusable composables
 * - UI utilities and extensions
 */
object UiModule {

    const val VERSION = "1.0.0"
    const val NAME = "ui"

    private var isInitialized = false
    private var appContext: Context? = null
    
    private var theme: VerumTheme? = null
    private var componentRegistry: ComponentRegistry? = null

    /**
     * Initialize the UI module.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        appContext = context.applicationContext
        theme = VerumTheme()
        componentRegistry = ComponentRegistry()
        
        isInitialized = true
    }

    /**
     * Get theme.
     */
    fun getTheme(): VerumTheme {
        return theme ?: throw IllegalStateException("UiModule not initialized")
    }

    /**
     * Get component registry.
     */
    fun getComponentRegistry(): ComponentRegistry {
        return componentRegistry ?: throw IllegalStateException("UiModule not initialized")
    }
}

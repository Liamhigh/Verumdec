package ai.verum.theleveler

import ai.verum.theleveler.report.LevelerReport
import ai.verum.theleveler.report.ReportBuilder

/**
 * The Leveler - Verum Omnis Contradiction Engine
 * 
 * Simple facade API for the complete Leveler engine.
 * Works offline, no cloud dependencies, pure Kotlin.
 * 
 * Compatible with: Android, JVM, Desktop, Backend, Web (Kotlin/JS)
 * 
 * Usage:
 * ```kotlin
 * val report = Leveler.analyze(rawText)
 * println(report.narrative)
 * ```
 */
object Leveler {

    /**
     * Analyse text and generate a complete contradiction report.
     * 
     * @param text Raw text containing statements, conversations, or documents
     * @return Complete LevelerReport with narrative, contradictions, and analysis
     */
    fun analyze(text: String): LevelerReport {
        return ReportBuilder.generate(text)
    }
}

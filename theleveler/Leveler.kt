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
 * println(report.getFullText())
 * ```
 */
object Leveler {

    /**
     * Analyse text and generate a complete contradiction report.
     * 
     * @param text Raw text containing statements, conversations, or documents
     * @param sourceId Optional identifier for the source document
     * @return Complete LevelerReport with narrative, contradictions, and analysis
     */
    fun analyze(text: String, sourceId: String? = null): LevelerReport {
        return ReportBuilder.generate(text, sourceId)
    }

    /**
     * Analyse multiple documents together.
     * 
     * @param documents Map of source IDs to document text
     * @return Complete LevelerReport combining all documents
     */
    fun analyzeMultiple(documents: Map<String, String>): LevelerReport {
        return ReportBuilder.generateFromMultiple(documents)
    }

    /**
     * Quick check if text contains contradictions.
     * 
     * @param text Raw text to analyse
     * @return True if contradictions are detected
     */
    fun hasContradictions(text: String): Boolean {
        return analyze(text).hasContradictions()
    }

    /**
     * Get just the narrative text (no structured data).
     * 
     * @param text Raw text to analyse
     * @return Full narrative as a string
     */
    fun getNarrative(text: String): String {
        return analyze(text).getFullText()
    }

    /**
     * Get the number of contradictions in text.
     * 
     * @param text Raw text to analyse
     * @return Count of contradictions found
     */
    fun countContradictions(text: String): Int {
        return analyze(text).statistics.totalContradictions
    }
}

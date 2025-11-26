package com.verumomnis.forensic

import android.content.Context
import com.verumomnis.forensic.contradiction.*
import com.verumomnis.forensic.pdf.PdfSealEngine
import com.verumomnis.forensic.model.ForensicResult
import java.io.File

/**
 * Main Verum Omnis Engine - Orchestrates all forensic analysis modules.
 * This is the "Nine Brains" orchestrator that processes evidence
 * and produces sealed forensic reports.
 */
object VerumOmnisEngine {

    private val claimHistory = mutableListOf<Claim>()

    /**
     * Process text input and generate a forensic report.
     */
    fun process(context: Context, text: String): ForensicResult {
        // Brain 1: Extract claims
        val claim = ClaimExtractor.extract(text)
        
        // Brain 2: Detect contradictions
        val contradictions = ContradictionDetector.check(claim, claimHistory)
        
        // Brain 3: Analyse behaviour
        val behaviourFlags = BehaviourBrain.detectStressMarkers(text)
        val manipulationFlags = BehaviourBrain.detectManipulation(text)
        
        // Brain 4: Timeline analysis (future implementation)
        TimelineBrain.buildTimeline(claimHistory + claim)
        
        // Store claim for future analysis
        claimHistory.add(claim)

        // Build comprehensive report
        val report = buildReport(
            text = text,
            claim = claim,
            contradictions = contradictions,
            behaviourFlags = behaviourFlags + manipulationFlags
        )

        // Seal report as PDF
        val pdf = PdfSealEngine.createSealedPdf(context, report)

        return ForensicResult(
            contradictions = contradictions,
            behaviouralFlags = behaviourFlags + manipulationFlags,
            pdfFile = pdf
        )
    }

    /**
     * Process a file and generate a sealed PDF.
     */
    fun processPdf(context: Context, reportFile: File): File {
        val content = reportFile.readText()
        return PdfSealEngine.createSealedPdf(context, content)
    }

    /**
     * Clear claim history (for new case analysis).
     */
    fun clearHistory() {
        claimHistory.clear()
    }

    /**
     * Get current claim count.
     */
    fun getClaimCount(): Int = claimHistory.size

    private fun buildReport(
        text: String,
        claim: Claim,
        contradictions: List<String>,
        behaviourFlags: List<String>
    ): String {
        val severity = ContradictionDetector.scoreSeverity(contradictions)
        
        return buildString {
            appendLine("=" .repeat(60))
            appendLine("VERUM OMNIS FORENSIC ANALYSIS REPORT")
            appendLine("=" .repeat(60))
            appendLine()
            
            appendLine("INPUT STATEMENT:")
            appendLine("-".repeat(40))
            appendLine(text)
            appendLine()
            
            appendLine("CLAIM CLASSIFICATION:")
            appendLine("-".repeat(40))
            appendLine("Type: ${claim.claimType}")
            appendLine("Entities detected: ${claim.entities.joinToString(", ").ifEmpty { "None" }}")
            appendLine("Time references: ${claim.timeRefs.joinToString(", ").ifEmpty { "None" }}")
            appendLine()
            
            appendLine("CONTRADICTION ANALYSIS:")
            appendLine("-".repeat(40))
            appendLine("Severity Level: ${severity.uppercase()}")
            if (contradictions.isEmpty()) {
                appendLine("No contradictions detected with prior statements.")
            } else {
                contradictions.forEachIndexed { index, c ->
                    appendLine("${index + 1}. $c")
                }
            }
            appendLine()
            
            appendLine("BEHAVIOURAL ANALYSIS:")
            appendLine("-".repeat(40))
            if (behaviourFlags.isEmpty()) {
                appendLine("No behavioural red flags detected.")
            } else {
                behaviourFlags.forEachIndexed { index, flag ->
                    appendLine("${index + 1}. $flag")
                }
            }
            appendLine()
            
            appendLine("LIABILITY ASSESSMENT:")
            appendLine("-".repeat(40))
            val liabilityScore = calculateLiabilityScore(contradictions, behaviourFlags)
            appendLine("Preliminary Liability Score: ${liabilityScore}%")
            appendLine()
            
            appendLine("=" .repeat(60))
            appendLine("END OF FORENSIC REPORT")
            appendLine("=" .repeat(60))
        }
    }

    private fun calculateLiabilityScore(
        contradictions: List<String>,
        behaviourFlags: List<String>
    ): Int {
        var score = 0
        
        // Each contradiction adds 15 points
        score += contradictions.size * 15
        
        // Each behaviour flag adds 10 points
        score += behaviourFlags.size * 10
        
        return score.coerceIn(0, 100)
    }
}

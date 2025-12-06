package org.verumomnis.engine

import java.text.SimpleDateFormat
import java.util.*

/**
 * LAYER 4 — REPORT ENGINE
 * 
 * Purpose: Generate final forensic report containing:
 * 1. Narrative summary
 * 2. Contradictions list
 * 3. Legal classification
 * 4. Summary of findings
 * 
 * This layer builds the final structured output.
 */
class ReportEngine {
    
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.US)
    
    /**
     * Build complete forensic report
     */
    fun build(
        sentences: List<NarrativeEngine.Sentence>,
        contradictions: List<ContradictionEngine.ContradictionResult>,
        legal: List<ClassificationEngine.LegalFinding>,
        caseId: String
    ): String {
        val builder = StringBuilder()
        
        // Section 1: PRE-ANALYSIS DECLARATION
        builder.appendLine(buildPreAnalysisDeclaration())
        builder.appendLine()
        
        // Section 2: NARRATIVE STRUCTURE
        builder.appendLine(buildNarrativeStructure(sentences))
        builder.appendLine()
        
        // Section 3: CONTRADICTIONS DETECTED
        builder.appendLine(buildContradictionsSection(contradictions))
        builder.appendLine()
        
        // Section 4: LEGAL CLASSIFICATION
        builder.appendLine(buildLegalClassification(legal))
        builder.appendLine()
        
        // Section 5: SUMMARY FINDINGS
        builder.appendLine(buildSummaryFindings(contradictions, legal))
        builder.appendLine()
        
        // Section 6: POST-ANALYSIS DECLARATION
        builder.appendLine(buildPostAnalysisDeclaration())
        
        return builder.toString()
    }
    
    /**
     * Section 1: PRE-ANALYSIS DECLARATION
     */
    private fun buildPreAnalysisDeclaration(): String {
        return """
================================================================
VERUM OMNIS FORENSIC REPORT
Deterministic Analysis Engine
================================================================

PRE-ANALYSIS DECLARATION
-------------------------

This is a deterministic forensic report. No AI interpretation included.

This report was generated using a rule-based, deterministic analysis engine.
The engine applies fixed contradiction detection rules and legal classification
logic to all evidence without variation or interpretation.

All findings are based solely on:
1. Exact text matching
2. Predefined contradiction rules
3. Fixed legal category mappings
4. Deterministic logic flow

Generated: ${dateFormat.format(Date())}
        """.trimIndent()
    }
    
    /**
     * Section 2: NARRATIVE STRUCTURE
     */
    private fun buildNarrativeStructure(sentences: List<NarrativeEngine.Sentence>): String {
        val builder = StringBuilder()
        
        builder.appendLine("NARRATIVE STRUCTURE")
        builder.appendLine("===================")
        builder.appendLine()
        builder.appendLine("Index | Timestamp           | Sentence")
        builder.appendLine("------|---------------------|" + "-".repeat(60))
        
        for (sentence in sentences) {
            val timestampStr = if (sentence.timestamp != null) {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(sentence.timestamp))
            } else {
                "No timestamp      "
            }
            
            val sentencePreview = if (sentence.text.length > 60) {
                sentence.text.take(57) + "..."
            } else {
                sentence.text
            }
            
            builder.appendLine(String.format("%-5d | %-19s | %s", sentence.index, timestampStr, sentencePreview))
        }
        
        builder.appendLine()
        builder.appendLine("Total sentences analyzed: ${sentences.size}")
        
        return builder.toString()
    }
    
    /**
     * Section 3: CONTRADICTIONS DETECTED
     */
    private fun buildContradictionsSection(contradictions: List<ContradictionEngine.ContradictionResult>): String {
        val builder = StringBuilder()
        
        builder.appendLine("CONTRADICTIONS DETECTED")
        builder.appendLine("=======================")
        builder.appendLine()
        
        if (contradictions.isEmpty()) {
            builder.appendLine("No contradictions detected in the evidence.")
        } else {
            contradictions.forEachIndexed { index, contradiction ->
                builder.appendLine("#${index + 1}")
                builder.appendLine()
                builder.appendLine("A: \"${contradiction.a.text}\"")
                builder.appendLine("   (Index: ${contradiction.a.index})")
                builder.appendLine()
                builder.appendLine("B: \"${contradiction.b.text}\"")
                builder.appendLine("   (Index: ${contradiction.b.index})")
                builder.appendLine()
                builder.appendLine("Reason: ${contradiction.reason}")
                builder.appendLine()
                builder.appendLine("-".repeat(70))
                builder.appendLine()
            }
            
            builder.appendLine("Total contradictions: ${contradictions.size}")
        }
        
        return builder.toString()
    }
    
    /**
     * Section 4: LEGAL CLASSIFICATION
     */
    private fun buildLegalClassification(legal: List<ClassificationEngine.LegalFinding>): String {
        val builder = StringBuilder()
        
        builder.appendLine("LEGAL CLASSIFICATION")
        builder.appendLine("====================")
        builder.appendLine()
        
        if (legal.isEmpty()) {
            builder.appendLine("No legal categories triggered.")
        } else {
            for (finding in legal) {
                builder.appendLine("Subject: ${finding.subject.name.replace("_", " ")}")
                builder.appendLine()
                builder.appendLine("Evidence:")
                
                finding.contradictions.forEachIndexed { index, contradiction ->
                    builder.appendLine("  - Contradiction #${index + 1}")
                    builder.appendLine("    \"${contradiction.a.text.take(60)}...\"")
                    builder.appendLine("    vs")
                    builder.appendLine("    \"${contradiction.b.text.take(60)}...\"")
                }
                
                builder.appendLine()
                builder.appendLine("Total contradictions in this category: ${finding.contradictions.size}")
                builder.appendLine()
                builder.appendLine("-".repeat(70))
                builder.appendLine()
            }
        }
        
        return builder.toString()
    }
    
    /**
     * Section 5: SUMMARY FINDINGS
     */
    private fun buildSummaryFindings(
        contradictions: List<ContradictionEngine.ContradictionResult>,
        legal: List<ClassificationEngine.LegalFinding>
    ): String {
        val builder = StringBuilder()
        
        builder.appendLine("SUMMARY FINDINGS")
        builder.appendLine("================")
        builder.appendLine()
        
        builder.appendLine("Total contradictions: ${contradictions.size}")
        builder.appendLine("Categories triggered: ${legal.size}")
        builder.appendLine()
        
        if (legal.isNotEmpty()) {
            builder.appendLine("Legal categories identified:")
            for (finding in legal) {
                builder.appendLine("  - ${finding.subject.name.replace("_", " ")} (${finding.contradictions.size} contradiction(s))")
            }
        }
        
        builder.appendLine()
        
        // Find most severe contradiction (first one detected)
        if (contradictions.isNotEmpty()) {
            builder.appendLine("Most severe contradiction pair:")
            val first = contradictions.first()
            builder.appendLine("  \"${first.a.text}\"")
            builder.appendLine("  vs")
            builder.appendLine("  \"${first.b.text}\"")
            builder.appendLine("  Reason: ${first.reason}")
        }
        
        return builder.toString()
    }
    
    /**
     * Section 6: POST-ANALYSIS DECLARATION
     */
    private fun buildPostAnalysisDeclaration(): String {
        return """
POST-ANALYSIS DECLARATION
--------------------------

End of deterministic evaluation.

This report was generated using fixed, rule-based logic.
The same evidence processed again will produce identical results.

The analysis is reproducible, auditable, and deterministic.
No machine learning, AI, or adaptive logic was used.

All contradiction rules and classification mappings are fixed
and do not vary based on evidence content.

================================================================
END OF FORENSIC REPORT
================================================================
        """.trimIndent()
    }
}

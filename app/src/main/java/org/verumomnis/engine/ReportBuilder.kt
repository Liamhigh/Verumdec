package org.verumomnis.engine

import java.text.SimpleDateFormat
import java.util.*

/**
 * STEP 12: REPORT BUILD
 * Plain-text report with exact sections:
 * 1. PRE-ANALYSIS DECLARATION
 * 2. Critical Legal Subjects
 * 3. Dishonesty Detection Matrix
 * 4. Tagged Evidence Table
 * 5. Contradictions Summary
 * 6. Behavioral Flags
 * 7. Dishonesty Score
 * 8. Top 3 Liabilities
 * 9. Recommended Actions
 * 10. POST-ANALYSIS DECLARATION
 */
class ReportBuilder {
    
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.US)
    
    /**
     * Build complete forensic report.
     * This step ALWAYS generates the SAME structured report format.
     */
    fun buildReport(
        caseId: String,
        narrativeList: List<Sentence>,
        contradictions: List<ContradictionResult>,
        omissions: List<OmissionResult>,
        categoryScores: Map<SubjectTag, Int>,
        dishonestyScore: Float,
        topLiabilities: List<LiabilityEntry>,
        recommendedActions: List<RecommendedAction>
    ): ForensicReportData {
        
        return ForensicReportData(
            caseId = caseId,
            preAnalysisDeclaration = buildPreAnalysisDeclaration(),
            criticalLegalSubjects = categoryScores,
            dishonestyMatrix = buildDishonestyMatrix(narrativeList),
            taggedEvidence = narrativeList,
            contradictions = contradictions,
            omissions = omissions,
            behavioralFlags = buildBehavioralFlags(narrativeList),
            dishonestyScore = dishonestyScore,
            topLiabilities = topLiabilities,
            recommendedActions = recommendedActions,
            postAnalysisDeclaration = buildPostAnalysisDeclaration()
        )
    }
    
    /**
     * Generate plain text report output.
     */
    fun generatePlainTextReport(report: ForensicReportData): String {
        val builder = StringBuilder()
        
        // Header
        builder.appendLine("================================================================")
        builder.appendLine("VERUM OMNIS FORENSIC ANALYSIS REPORT")
        builder.appendLine("================================================================")
        builder.appendLine("Case ID: ${report.caseId}")
        builder.appendLine("Generated: ${dateFormat.format(Date())}")
        builder.appendLine("================================================================")
        builder.appendLine()
        
        // SECTION 1: PRE-ANALYSIS DECLARATION
        builder.appendLine("1. PRE-ANALYSIS DECLARATION")
        builder.appendLine("----------------------------")
        builder.appendLine(report.preAnalysisDeclaration)
        builder.appendLine()
        
        // SECTION 2: CRITICAL LEGAL SUBJECTS
        builder.appendLine("2. CRITICAL LEGAL SUBJECTS TABLE")
        builder.appendLine("---------------------------------")
        builder.appendLine("Subject | Severity Score")
        builder.appendLine("---------------------------------")
        for ((subject, score) in report.criticalLegalSubjects) {
            builder.appendLine("${subject.name.replace("_", " ")} | $score")
        }
        builder.appendLine()
        
        // SECTION 3: DISHONESTY DETECTION MATRIX
        builder.appendLine("3. DISHONESTY DETECTION MATRIX")
        builder.appendLine("-------------------------------")
        builder.appendLine("Flagged Sentences: ${report.dishonestyMatrix.size}")
        for ((index, sentence) in report.dishonestyMatrix.withIndex()) {
            builder.appendLine("[$index] ${sentence.text}")
            if (sentence.keywords.isNotEmpty()) {
                builder.appendLine("    Keywords: ${sentence.keywords.joinToString(", ")}")
            }
            if (sentence.behaviors.isNotEmpty()) {
                builder.appendLine("    Behaviors: ${sentence.behaviors.joinToString(", ") { it.name.replace("_", " ") }}")
            }
        }
        builder.appendLine()
        
        // SECTION 4: TAGGED EVIDENCE TABLE
        builder.appendLine("4. TAGGED EVIDENCE TABLE")
        builder.appendLine("------------------------")
        builder.appendLine("Total Evidence Items: ${report.taggedEvidence.size}")
        for ((index, sentence) in report.taggedEvidence.withIndex().take(50)) {
            if (sentence.subjectTags.isNotEmpty()) {
                builder.appendLine("[$index] ${sentence.text.take(100)}...")
                builder.appendLine("    Tags: ${sentence.subjectTags.joinToString(", ") { it.name.replace("_", " ") }}")
            }
        }
        builder.appendLine()
        
        // SECTION 5: CONTRADICTIONS SUMMARY
        builder.appendLine("5. CONTRADICTIONS SUMMARY")
        builder.appendLine("-------------------------")
        builder.appendLine("Total Contradictions: ${report.contradictions.size}")
        for ((index, contradiction) in report.contradictions.withIndex()) {
            builder.appendLine()
            builder.appendLine("Contradiction ${index + 1} [${contradiction.severity.name}]:")
            builder.appendLine("  Statement A: ${contradiction.statementA.text}")
            builder.appendLine("  Statement B: ${contradiction.statementB.text}")
            builder.appendLine("  Analysis: ${contradiction.description}")
        }
        builder.appendLine()
        
        // OMISSIONS (part of contradictions analysis)
        builder.appendLine("OMISSIONS DETECTED:")
        builder.appendLine("Total Omissions: ${report.omissions.size}")
        for ((index, omission) in report.omissions.withIndex()) {
            builder.appendLine()
            builder.appendLine("Omission ${index + 1} [${omission.severity.name}]:")
            builder.appendLine("  Description: ${omission.description}")
            builder.appendLine("  Context: ${omission.context}")
        }
        builder.appendLine()
        
        // SECTION 6: BEHAVIORAL FLAGS
        builder.appendLine("6. BEHAVIORAL FLAGS")
        builder.appendLine("-------------------")
        for ((behavior, count) in report.behavioralFlags) {
            builder.appendLine("${behavior.name.replace("_", " ")}: $count instances")
        }
        builder.appendLine()
        
        // SECTION 7: DISHONESTY SCORE
        builder.appendLine("7. DISHONESTY SCORE")
        builder.appendLine("-------------------")
        builder.appendLine("Score: ${String.format("%.2f", report.dishonestyScore)}%")
        builder.appendLine("(Flagged Sentences / Total Sentences × 100)")
        builder.appendLine()
        
        // SECTION 8: TOP 3 LIABILITIES
        builder.appendLine("8. TOP 3 LIABILITIES")
        builder.appendLine("--------------------")
        for ((index, liability) in report.topLiabilities.withIndex()) {
            builder.appendLine()
            builder.appendLine("${index + 1}. ${liability.category.name.replace("_", " ")}")
            builder.appendLine("   Total Severity: ${liability.totalSeverity}")
            builder.appendLine("   Contradictions: ${liability.contradictionCount}")
            builder.appendLine("   Recurrence: ${liability.recurrence}")
        }
        builder.appendLine()
        
        // SECTION 9: RECOMMENDED ACTIONS
        builder.appendLine("9. RECOMMENDED ACTIONS")
        builder.appendLine("----------------------")
        for ((index, action) in report.recommendedActions.withIndex()) {
            builder.appendLine()
            builder.appendLine("Action ${index + 1}:")
            builder.appendLine("  Authority: ${action.authority}")
            builder.appendLine("  Action: ${action.action}")
            builder.appendLine("  Legal Basis: ${action.legalBasis}")
        }
        builder.appendLine()
        
        // SECTION 10: POST-ANALYSIS DECLARATION
        builder.appendLine("10. POST-ANALYSIS DECLARATION")
        builder.appendLine("------------------------------")
        builder.appendLine(report.postAnalysisDeclaration)
        builder.appendLine()
        
        builder.appendLine("================================================================")
        builder.appendLine("END OF REPORT")
        builder.appendLine("================================================================")
        
        return builder.toString()
    }
    
    private fun buildPreAnalysisDeclaration(): String {
        return """
This forensic analysis was conducted using the Verum Omnis Forensic Engine,
a deterministic evidence analysis system. This engine processes ALL evidence
through the SAME 12-step forensic pipeline, ensuring consistent and unbiased
analysis regardless of evidence type or content.

The engine operates on fixed rules and does NOT adapt its analysis based on
evidence content. Every case follows the identical analytical pathway.

This report represents the output of that fixed analysis pipeline.
        """.trimIndent()
    }
    
    private fun buildPostAnalysisDeclaration(): String {
        return """
This analysis was completed using a deterministic, non-adaptive forensic engine.
The findings are based solely on the evidence provided and the fixed analytical
rules embedded in the Verum Omnis system.

No human interpretation or bias was introduced during the automated analysis phase.
This report should be reviewed by qualified legal professionals before taking action.

The engine's immutability ensures that the same evidence will ALWAYS produce the
same analytical output, making this process auditable and reproducible.

Generated by: Verum Omnis Forensic Engine v1.0
Date: ${dateFormat.format(Date())}
        """.trimIndent()
    }
    
    private fun buildDishonestyMatrix(narrativeList: List<Sentence>): List<Sentence> {
        return narrativeList.filter { it.isFlagged }
    }
    
    private fun buildBehavioralFlags(narrativeList: List<Sentence>): Map<BehaviorFlag, Int> {
        val behaviorCounts = mutableMapOf<BehaviorFlag, Int>()
        
        for (sentence in narrativeList) {
            for (behavior in sentence.behaviors) {
                behaviorCounts[behavior] = (behaviorCounts[behavior] ?: 0) + 1
            }
        }
        
        return behaviorCounts
    }
}

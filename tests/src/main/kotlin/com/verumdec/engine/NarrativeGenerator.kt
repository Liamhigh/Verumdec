package com.verumdec.engine

import com.verumdec.data.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Narrative Generator
 * Builds comprehensive legal narratives from analysis results.
 */
class NarrativeGenerator {

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.US)

    /**
     * Generate complete narrative sections.
     */
    fun generateNarrative(
        entities: List<Entity>,
        timeline: List<TimelineEvent>,
        contradictions: List<Contradiction>,
        behavioralPatterns: List<BehavioralPattern>,
        liabilityScores: Map<String, LiabilityScore>
    ): NarrativeSections {
        return NarrativeSections(
            objectiveNarration = generateObjectiveNarration(timeline, entities),
            contradictionCommentary = generateContradictionCommentary(contradictions, entities),
            behavioralPatternAnalysis = generateBehavioralAnalysis(behavioralPatterns, entities),
            deductiveLogic = generateDeductiveLogic(contradictions, entities),
            causalChain = generateCausalChain(timeline, entities),
            finalSummary = generateFinalSummary(entities, liabilityScores, contradictions)
        )
    }

    /**
     * Generate objective chronological narration.
     */
    private fun generateObjectiveNarration(
        timeline: List<TimelineEvent>,
        entities: List<Entity>
    ): String {
        if (timeline.isEmpty()) return "No timeline events to narrate."
        
        val builder = StringBuilder()
        builder.appendLine("CHRONOLOGICAL ACCOUNT OF EVENTS")
        builder.appendLine("================================")
        builder.appendLine()
        
        val sortedEvents = timeline.sortedBy { it.date }
        var currentMonth = ""
        
        for (event in sortedEvents) {
            val month = SimpleDateFormat("MMMM yyyy", Locale.US).format(event.date)
            if (month != currentMonth) {
                builder.appendLine()
                builder.appendLine("--- $month ---")
                currentMonth = month
            }
            
            val date = dateFormat.format(event.date)
            val entityNames = event.entityIds.mapNotNull { id ->
                entities.find { it.id == id }?.primaryName
            }.joinToString(", ")
            
            builder.appendLine()
            builder.appendLine("On $date:")
            if (entityNames.isNotBlank()) {
                builder.appendLine("Involving: $entityNames")
            }
            builder.appendLine(event.description)
            
            if (event.significance == Significance.CRITICAL || event.significance == Significance.HIGH) {
                builder.appendLine("[${event.significance.name} SIGNIFICANCE EVENT]")
            }
        }
        
        return builder.toString()
    }

    /**
     * Generate commentary on contradictions.
     */
    private fun generateContradictionCommentary(
        contradictions: List<Contradiction>,
        entities: List<Entity>
    ): String {
        if (contradictions.isEmpty()) return "No contradictions detected in the evidence."
        
        val builder = StringBuilder()
        builder.appendLine("CONTRADICTION ANALYSIS")
        builder.appendLine("======================")
        builder.appendLine()
        builder.appendLine("${contradictions.size} contradiction(s) detected:")
        builder.appendLine()
        
        // Group by entity
        val byEntity = contradictions.groupBy { it.entityId }
        
        for ((entityId, entityContradictions) in byEntity) {
            val entityName = entities.find { it.id == entityId }?.primaryName ?: "Unknown Entity"
            
            builder.appendLine("## $entityName")
            builder.appendLine("Total contradictions: ${entityContradictions.size}")
            builder.appendLine()
            
            for ((index, contradiction) in entityContradictions.withIndex()) {
                builder.appendLine("Contradiction ${index + 1} [${contradiction.severity.name}]:")
                builder.appendLine("Type: ${contradiction.type.name.replace("_", " ")}")
                builder.appendLine()
                
                builder.appendLine("Statement A:")
                builder.appendLine("\"${contradiction.statementA.text}\"")
                contradiction.statementA.date?.let { 
                    builder.appendLine("(${dateFormat.format(it)})")
                }
                builder.appendLine()
                
                builder.appendLine("Statement B:")
                builder.appendLine("\"${contradiction.statementB.text}\"")
                contradiction.statementB.date?.let {
                    builder.appendLine("(${dateFormat.format(it)})")
                }
                builder.appendLine()
                
                builder.appendLine("Analysis: ${contradiction.description}")
                builder.appendLine("Legal Implication: ${contradiction.legalImplication}")
                builder.appendLine()
                builder.appendLine("---")
                builder.appendLine()
            }
        }
        
        return builder.toString()
    }

    /**
     * Generate behavioral pattern analysis.
     */
    private fun generateBehavioralAnalysis(
        patterns: List<BehavioralPattern>,
        entities: List<Entity>
    ): String {
        if (patterns.isEmpty()) return "No behavioral patterns detected."
        
        val builder = StringBuilder()
        builder.appendLine("BEHAVIORAL PATTERN ANALYSIS")
        builder.appendLine("===========================")
        builder.appendLine()
        
        val byEntity = patterns.groupBy { it.entityId }
        
        for ((entityId, entityPatterns) in byEntity) {
            val entityName = entities.find { it.id == entityId }?.primaryName ?: "Unknown Entity"
            
            builder.appendLine("## $entityName")
            builder.appendLine()
            
            for (pattern in entityPatterns) {
                builder.appendLine("Pattern: ${pattern.type.name.replace("_", " ")}")
                builder.appendLine("Severity: ${pattern.severity.name}")
                builder.appendLine()
                
                builder.appendLine("Description:")
                builder.appendLine(getBehaviorDescription(pattern.type))
                builder.appendLine()
                
                if (pattern.instances.isNotEmpty()) {
                    builder.appendLine("Instances detected:")
                    for (instance in pattern.instances.take(5)) {
                        builder.appendLine("  • \"$instance\"")
                    }
                    if (pattern.instances.size > 5) {
                        builder.appendLine("  ... and ${pattern.instances.size - 5} more instances")
                    }
                }
                builder.appendLine()
                builder.appendLine("---")
                builder.appendLine()
            }
        }
        
        return builder.toString()
    }

    /**
     * Get description for behavioral pattern type.
     */
    private fun getBehaviorDescription(type: BehaviorType): String {
        return when (type) {
            BehaviorType.GASLIGHTING -> "Attempts to make the other party question their own perception of reality."
            BehaviorType.DEFLECTION -> "Redirecting attention away from the relevant issues."
            BehaviorType.PRESSURE_TACTICS -> "Using urgency or pressure to force decisions."
            BehaviorType.FINANCIAL_MANIPULATION -> "Using financial leverage or promises to manipulate."
            BehaviorType.EMOTIONAL_MANIPULATION -> "Exploiting emotional connections for advantage."
            BehaviorType.SUDDEN_WITHDRAWAL -> "Abrupt cessation of communication or engagement."
            BehaviorType.GHOSTING -> "Extended periods of ignoring communications."
            BehaviorType.OVER_EXPLAINING -> "Excessive justification that may indicate deception."
            BehaviorType.SLIP_UP_ADMISSION -> "Accidental admissions within denials or explanations."
            BehaviorType.DELAYED_RESPONSE -> "Strategic timing of responses, often after receiving something."
            BehaviorType.BLAME_SHIFTING -> "Attempting to transfer responsibility to others."
            BehaviorType.PASSIVE_ADMISSION -> "Indirect acknowledgments suggesting awareness of wrongdoing."
        }
    }

    /**
     * Generate deductive logic section explaining WHY contradictions matter.
     */
    private fun generateDeductiveLogic(
        contradictions: List<Contradiction>,
        entities: List<Entity>
    ): String {
        if (contradictions.isEmpty()) return "No deductive analysis required - no contradictions found."
        
        val builder = StringBuilder()
        builder.appendLine("DEDUCTIVE ANALYSIS")
        builder.appendLine("==================")
        builder.appendLine()
        builder.appendLine("The following logical deductions arise from the contradictions detected:")
        builder.appendLine()
        
        var deductionNum = 1
        
        for (contradiction in contradictions.filter { it.severity == Severity.CRITICAL || it.severity == Severity.HIGH }) {
            val entityName = entities.find { it.id == contradiction.entityId }?.primaryName ?: "The party"
            
            builder.appendLine("Deduction $deductionNum:")
            builder.appendLine()
            
            when (contradiction.type) {
                ContradictionType.DIRECT -> {
                    builder.appendLine("$entityName stated: \"${contradiction.statementA.text.take(100)}...\"")
                    builder.appendLine("Later, $entityName stated: \"${contradiction.statementB.text.take(100)}...\"")
                    builder.appendLine()
                    builder.appendLine("DEDUCTION: Since both statements cannot be simultaneously true, at least one statement is false. This indicates either:")
                    builder.appendLine("  a) Deliberate deception at one or both times")
                    builder.appendLine("  b) Unreliable memory suggesting other claims may also be inaccurate")
                    builder.appendLine("  c) Changed circumstances that were not disclosed")
                }
                
                ContradictionType.CROSS_DOCUMENT -> {
                    builder.appendLine("$entityName provided different accounts in different documents.")
                    builder.appendLine()
                    builder.appendLine("DEDUCTION: The inconsistency across documents suggests the party has not maintained a truthful account, or has tailored their story for different audiences.")
                }
                
                ContradictionType.TEMPORAL -> {
                    builder.appendLine("The timeline of $entityName's statements is internally inconsistent.")
                    builder.appendLine()
                    builder.appendLine("DEDUCTION: Events claimed to have occurred cannot have happened in the sequence described, indicating fabrication.")
                }
                
                ContradictionType.BEHAVIORAL -> {
                    builder.appendLine("$entityName's behavior pattern indicates deceptive conduct.")
                    builder.appendLine()
                    builder.appendLine("DEDUCTION: The behavioral indicators suggest the party was aware of wrongdoing and attempted to conceal or minimize it.")
                }
                
                ContradictionType.MISSING_EVIDENCE -> {
                    builder.appendLine("$entityName referenced evidence that was never provided.")
                    builder.appendLine()
                    builder.appendLine("DEDUCTION: The failure to provide referenced evidence suggests either:")
                    builder.appendLine("  a) The evidence does not exist")
                    builder.appendLine("  b) The evidence contradicts the party's claims")
                }
                
                ContradictionType.THIRD_PARTY -> {
                    builder.appendLine("$entityName's account is contradicted by another party.")
                    builder.appendLine()
                    builder.appendLine("DEDUCTION: One of the parties is providing false information. The weight of other evidence should determine which account is credible.")
                }
            }
            
            builder.appendLine()
            builder.appendLine("---")
            builder.appendLine()
            deductionNum++
        }
        
        return builder.toString()
    }

    /**
     * Generate causal chain showing cause -> effect relationships.
     */
    private fun generateCausalChain(
        timeline: List<TimelineEvent>,
        entities: List<Entity>
    ): String {
        if (timeline.isEmpty()) return "Insufficient data for causal chain analysis."
        
        val builder = StringBuilder()
        builder.appendLine("CAUSAL CHAIN ANALYSIS")
        builder.appendLine("=====================")
        builder.appendLine()
        builder.appendLine("The following cause-effect relationships were identified:")
        builder.appendLine()
        
        val sortedEvents = timeline.sortedBy { it.date }
        val significantEvents = sortedEvents.filter { 
            it.significance == Significance.HIGH || it.significance == Significance.CRITICAL 
        }
        
        if (significantEvents.size < 2) {
            builder.appendLine("Insufficient significant events to establish causal chains.")
            return builder.toString()
        }
        
        var chainNum = 1
        for (i in 0 until significantEvents.size - 1) {
            val cause = significantEvents[i]
            val effect = significantEvents[i + 1]
            
            val causeEntity = cause.entityIds.firstOrNull()?.let { id ->
                entities.find { it.id == id }?.primaryName
            } ?: "A party"
            
            val effectEntity = effect.entityIds.firstOrNull()?.let { id ->
                entities.find { it.id == id }?.primaryName
            } ?: "A party"
            
            builder.appendLine("Chain $chainNum:")
            builder.appendLine("CAUSE: On ${dateFormat.format(cause.date)}, $causeEntity ${cause.description.take(100)}")
            builder.appendLine("   ↓")
            builder.appendLine("EFFECT: On ${dateFormat.format(effect.date)}, $effectEntity ${effect.description.take(100)}")
            builder.appendLine()
            
            // Analyze the causal relationship
            builder.appendLine("Analysis: The earlier event created conditions that led to the subsequent action.")
            builder.appendLine()
            builder.appendLine("---")
            builder.appendLine()
            
            chainNum++
        }
        
        return builder.toString()
    }

    /**
     * Generate final summary with liability assessment.
     */
    private fun generateFinalSummary(
        entities: List<Entity>,
        liabilityScores: Map<String, LiabilityScore>,
        contradictions: List<Contradiction>
    ): String {
        val builder = StringBuilder()
        builder.appendLine("FINAL SUMMARY AND LIABILITY ASSESSMENT")
        builder.appendLine("======================================")
        builder.appendLine()
        
        // Sort entities by liability score
        val sortedEntities = entities.sortedByDescending { 
            liabilityScores[it.id]?.overallScore ?: 0f 
        }
        
        builder.appendLine("LIABILITY SCORES:")
        builder.appendLine()
        
        for (entity in sortedEntities) {
            val score = liabilityScores[entity.id]
            if (score != null) {
                builder.appendLine("${entity.primaryName}: ${String.format("%.1f", score.overallScore)}%")
                builder.appendLine("  Contradiction Score: ${String.format("%.1f", score.contradictionScore)}")
                builder.appendLine("  Behavioral Score: ${String.format("%.1f", score.behavioralScore)}")
                builder.appendLine("  Evidence Score: ${String.format("%.1f", score.evidenceContributionScore)}")
                builder.appendLine("  Consistency Score: ${String.format("%.1f", score.chronologicalConsistencyScore)}")
                builder.appendLine("  Causal Score: ${String.format("%.1f", score.causalResponsibilityScore)}")
                builder.appendLine()
            }
        }
        
        builder.appendLine("---")
        builder.appendLine()
        builder.appendLine("SUMMARY:")
        builder.appendLine()
        
        val criticalContradictions = contradictions.filter { it.severity == Severity.CRITICAL }
        val highContradictions = contradictions.filter { it.severity == Severity.HIGH }
        
        builder.appendLine("Total contradictions detected: ${contradictions.size}")
        builder.appendLine("  - Critical: ${criticalContradictions.size}")
        builder.appendLine("  - High: ${highContradictions.size}")
        builder.appendLine("  - Medium: ${contradictions.count { it.severity == Severity.MEDIUM }}")
        builder.appendLine("  - Low: ${contradictions.count { it.severity == Severity.LOW }}")
        builder.appendLine()
        
        val highestLiability = sortedEntities.firstOrNull()
        val highestScore = liabilityScores[highestLiability?.id]
        
        if (highestLiability != null && highestScore != null && highestScore.overallScore > 50f) {
            builder.appendLine("CONCLUSION:")
            builder.appendLine("Based on the analysis, ${highestLiability.primaryName} bears the highest responsibility")
            builder.appendLine("with a liability score of ${String.format("%.1f", highestScore.overallScore)}%.")
            builder.appendLine()
            
            if (criticalContradictions.isNotEmpty()) {
                builder.appendLine("Critical findings include ${criticalContradictions.size} critical contradiction(s)")
                builder.appendLine("that significantly impact the credibility of their account.")
            }
        } else {
            builder.appendLine("CONCLUSION:")
            builder.appendLine("The analysis indicates no party with conclusively high liability based on available evidence.")
            builder.appendLine("Further investigation may be required.")
        }
        
        return builder.toString()
    }
}

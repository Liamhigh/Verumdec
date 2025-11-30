package ai.verum.theleveler.narrative

import ai.verum.theleveler.core.*
import ai.verum.theleveler.entity.EntityProfiler
import ai.verum.theleveler.timeline.TimelineBuilder

/**
 * Generates a full forensic narrative from contradiction analysis.
 * Produces neutral, factual, structured output suitable for legal review.
 */
object NarrativeEngine {

    /**
     * Generate a complete forensic narrative.
     */
    fun generate(
        statements: List<Statement>,
        timelineEvents: List<TimelineEvent>,
        report: ContradictionReport,
        profiles: Map<String, EntityProfiler.EntityProfile>
    ): Narrative {
        val sections = mutableListOf<NarrativeSection>()
        
        // Section 1: Summary
        sections.add(generateSummary(statements, report))
        
        // Section 2: Parties Involved
        sections.add(generatePartiesSection(profiles))
        
        // Section 3: Chronological Narrative
        sections.add(generateChronologicalSection(timelineEvents))
        
        // Section 4: Contradictions
        sections.add(generateContradictionsSection(report.contradictions))
        
        // Section 5: Behaviour Shifts
        sections.add(generateBehaviourSection(report.behaviourShifts))
        
        // Section 6: Closing Summary
        sections.add(generateClosingSummary(report))
        
        return Narrative(
            sections = sections,
            totalStatements = statements.size,
            totalContradictions = report.contradictions.size,
            totalBehaviourShifts = report.behaviourShifts.size
        )
    }

    /**
     * Generate summary section.
     */
    private fun generateSummary(
        statements: List<Statement>,
        report: ContradictionReport
    ): NarrativeSection {
        val actors = statements.map { it.actor.rawName }.distinct()
        
        val content = buildString {
            appendLine("FORENSIC ANALYSIS SUMMARY")
            appendLine("=" .repeat(40))
            appendLine()
            appendLine("This analysis reviewed ${statements.size} statements from ${actors.size} parties.")
            appendLine()
            appendLine("Key findings:")
            appendLine("- ${report.contradictions.size} contradictions identified")
            appendLine("- ${report.behaviourShifts.size} behavioural shifts detected")
            
            val directCount = report.contradictions.count { it.type == ContradictionType.DIRECT_CONTRADICTION }
            val timelineCount = report.contradictions.count { it.type == ContradictionType.TIMELINE_CONTRADICTION }
            
            if (directCount > 0) {
                appendLine("- $directCount direct contradictions (opposing statements)")
            }
            if (timelineCount > 0) {
                appendLine("- $timelineCount timeline inconsistencies")
            }
        }
        
        return NarrativeSection(
            title = "Summary",
            content = content,
            sectionType = SectionType.SUMMARY
        )
    }

    /**
     * Generate parties section.
     */
    private fun generatePartiesSection(
        profiles: Map<String, EntityProfiler.EntityProfile>
    ): NarrativeSection {
        val content = buildString {
            appendLine("PARTIES INVOLVED")
            appendLine("=" .repeat(40))
            appendLine()
            
            for ((_, profile) in profiles) {
                appendLine("${profile.actor.rawName}")
                appendLine("-".repeat(profile.actor.rawName.length))
                appendLine("Statements on record: ${profile.statementCount}")
                appendLine("Communication style: ${profile.communicationStyle.name.lowercase().replace("_", " ")}")
                appendLine("Average certainty: ${String.format("%.0f", profile.averageCertainty * 100)}%")
                
                if (profile.themes.isNotEmpty()) {
                    appendLine("Topics discussed: ${profile.themes.joinToString(", ")}")
                }
                
                appendLine()
            }
        }
        
        return NarrativeSection(
            title = "Parties Involved",
            content = content,
            sectionType = SectionType.PARTIES
        )
    }

    /**
     * Generate chronological narrative section.
     */
    private fun generateChronologicalSection(
        events: List<TimelineEvent>
    ): NarrativeSection {
        val timeline = TimelineBuilder.build(events)
        
        val content = buildString {
            appendLine("CHRONOLOGICAL NARRATIVE")
            appendLine("=" .repeat(40))
            appendLine()
            
            if (timeline.isEmpty()) {
                appendLine("No timeline events could be extracted from the provided statements.")
            } else {
                for (event in timeline) {
                    val timestamp = event.timestamp ?: "Unknown date"
                    val actor = event.actor?.rawName ?: "Unknown"
                    
                    appendLine("[$timestamp] $actor:")
                    appendLine("  ${event.description}")
                    appendLine()
                }
            }
        }
        
        return NarrativeSection(
            title = "Chronological Narrative",
            content = content,
            sectionType = SectionType.CHRONOLOGY
        )
    }

    /**
     * Generate contradictions section.
     */
    private fun generateContradictionsSection(
        contradictions: List<Contradiction>
    ): NarrativeSection {
        val content = buildString {
            appendLine("CONTRADICTIONS IDENTIFIED")
            appendLine("=" .repeat(40))
            appendLine()
            
            if (contradictions.isEmpty()) {
                appendLine("No contradictions were detected in the analysed statements.")
            } else {
                for ((index, c) in contradictions.withIndex()) {
                    appendLine("Contradiction #${index + 1}")
                    appendLine("-".repeat(20))
                    appendLine("Type: ${c.type.name.lowercase().replace("_", " ")}")
                    appendLine("Actor: ${c.actor.rawName}")
                    appendLine("Confidence: ${String.format("%.0f", c.confidence * 100)}%")
                    appendLine()
                    appendLine("First statement: \"${c.firstStatement.text}\"")
                    appendLine("Second statement: \"${c.secondStatement.text}\"")
                    appendLine()
                    appendLine("Analysis: ${c.explanation}")
                    appendLine()
                    appendLine()
                }
            }
        }
        
        return NarrativeSection(
            title = "Contradictions",
            content = content,
            sectionType = SectionType.CONTRADICTIONS
        )
    }

    /**
     * Generate behaviour shifts section.
     */
    private fun generateBehaviourSection(
        shifts: List<BehaviourShift>
    ): NarrativeSection {
        val content = buildString {
            appendLine("BEHAVIOURAL ANALYSIS")
            appendLine("=" .repeat(40))
            appendLine()
            
            if (shifts.isEmpty()) {
                appendLine("No significant behavioural shifts were detected.")
            } else {
                val byActor = shifts.groupBy { it.actor.rawName }
                
                for ((actor, actorShifts) in byActor) {
                    appendLine("$actor:")
                    appendLine("-".repeat(actor.length + 1))
                    
                    for (shift in actorShifts) {
                        appendLine("• ${shift.shiftType.name.lowercase().replace("_", " ")}: ${shift.description}")
                    }
                    
                    appendLine()
                }
            }
        }
        
        return NarrativeSection(
            title = "Behavioural Analysis",
            content = content,
            sectionType = SectionType.BEHAVIOUR
        )
    }

    /**
     * Generate closing summary.
     */
    private fun generateClosingSummary(report: ContradictionReport): NarrativeSection {
        val content = buildString {
            appendLine("CLOSING SUMMARY")
            appendLine("=" .repeat(40))
            appendLine()
            
            val highConfidence = report.contradictions.count { it.confidence >= 0.7 }
            val mediumConfidence = report.contradictions.count { it.confidence in 0.4..0.69 }
            
            appendLine("This forensic analysis has identified:")
            appendLine()
            appendLine("Total contradictions: ${report.contradictions.size}")
            if (highConfidence > 0) {
                appendLine("- High confidence: $highConfidence")
            }
            if (mediumConfidence > 0) {
                appendLine("- Medium confidence: $mediumConfidence")
            }
            appendLine()
            appendLine("Behavioural shifts: ${report.behaviourShifts.size}")
            appendLine()
            appendLine("This report presents factual analysis only. Legal conclusions should be")
            appendLine("drawn by qualified legal professionals based on the complete evidence.")
        }
        
        return NarrativeSection(
            title = "Closing Summary",
            content = content,
            sectionType = SectionType.CLOSING
        )
    }

    /**
     * Narrative data class.
     */
    data class Narrative(
        val sections: List<NarrativeSection>,
        val totalStatements: Int,
        val totalContradictions: Int,
        val totalBehaviourShifts: Int
    ) {
        fun toFullText(): String {
            return sections.joinToString("\n\n") { it.content }
        }
    }

    /**
     * Narrative section data class.
     */
    data class NarrativeSection(
        val title: String,
        val content: String,
        val sectionType: SectionType
    )

    /**
     * Section type enum.
     */
    enum class SectionType {
        SUMMARY,
        PARTIES,
        CHRONOLOGY,
        CONTRADICTIONS,
        BEHAVIOUR,
        CLOSING
    }
}

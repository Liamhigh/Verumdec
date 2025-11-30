package ai.verum.theleveler.narrative

import ai.verum.theleveler.core.*
import ai.verum.theleveler.entity.EntityProfile
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
        profiles: Map<String, EntityProfile>
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
        val actors = statements.map { it.actor.displayName }.distinct()
        
        val content = buildString {
            appendLine("FORENSIC NARRATIVE REPORT")
            appendLine("=" .repeat(40))
            appendLine()
            appendLine("SUMMARY")
            appendLine("--------")
            appendLine("This document reconstructs the full sequence of events,")
            appendLine("identifies contradictions between statements,")
            appendLine("and highlights behavioural or linguistic changes.")
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
        profiles: Map<String, EntityProfile>
    ): NarrativeSection {
        val content = buildString {
            appendLine("PARTIES INVOLVED")
            appendLine("----------------")
            
            for ((_, profile) in profiles) {
                appendLine("- ${profile.actor.displayName}:")
                appendLine("  Statements: ${profile.statementCount}")
                
                if (profile.themes.isNotEmpty()) {
                    appendLine("  Themes: ${profile.themes.joinToString(", ")}")
                }
                if (profile.firstSeen != null) {
                    appendLine("  First Appearance: ${profile.firstSeen}")
                }
                if (profile.lastSeen != null) {
                    appendLine("  Last Appearance: ${profile.lastSeen}")
                }
            }
            appendLine()
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
            appendLine("------------------------")
            
            if (timeline.isEmpty()) {
                appendLine("No timeline events could be extracted from the provided statements.")
            } else {
                for (event in timeline) {
                    val timestamp = event.timestamp ?: "[no timestamp]"
                    val actor = event.actor?.displayName ?: "Unknown"
                    
                    appendLine("* $timestamp — $actor:")
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
            appendLine("CONTRADICTIONS DETECTED")
            appendLine("------------------------")
            
            if (contradictions.isEmpty()) {
                appendLine("None detected.")
            } else {
                for (c in contradictions) {
                    appendLine("Actor: ${c.actor.displayName}")
                    appendLine("Type: ${c.type}")
                    appendLine("Confidence: ${(c.confidence * 100).toInt()}%")
                    appendLine("Explanation: ${c.explanation}")
                    appendLine("- Earlier: \"${c.firstStatement.text}\" (${c.firstStatement.timestamp ?: "no timestamp"})")
                    appendLine("- Later: \"${c.secondStatement.text}\" (${c.secondStatement.timestamp ?: "no timestamp"})")
                    appendLine()
                }
            }
            appendLine()
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
            appendLine("BEHAVIOURAL SHIFTS")
            appendLine("-------------------")
            
            if (shifts.isEmpty()) {
                appendLine("None detected.")
            } else {
                for (shift in shifts) {
                    appendLine("Actor: ${shift.actor.displayName}")
                    appendLine("Type: ${shift.shiftType}")
                    appendLine("Description: ${shift.description}")
                    appendLine("- From: \"${shift.fromStatement.text}\"")
                    appendLine("- To: \"${shift.toStatement.text}\"")
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
            appendLine()
            appendLine("END OF REPORT")
            appendLine("=" .repeat(40))
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

package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for NarrativeGenerator
 * Tests narrative generation logic for forensic reports
 */
class NarrativeGeneratorTest {

    private lateinit var narrativeGenerator: NarrativeGenerator
    
    @Before
    fun setup() {
        narrativeGenerator = NarrativeGenerator()
    }

    @Test
    fun testGeneratesObjectiveNarration() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "John Doe",
            aliases = mutableListOf(),
            emails = mutableListOf("john@example.com"),
            phones = mutableListOf()
        )

        val timeline = listOf(
            TimelineEvent(
                id = "te1",
                date = Date(),
                description = "Contract was signed",
                sourceEvidenceId = "ev1",
                entityIds = listOf("entity1"),
                eventType = EventType.DOCUMENT,
                significance = Significance.HIGH
            )
        )

        // Act
        val narrative = narrativeGenerator.generateNarrative(
            listOf(entity),
            timeline,
            emptyList(),
            emptyList(),
            emptyMap()
        )

        // Assert
        assertTrue("Should generate objective narration", narrative.objectiveNarration.isNotBlank())
        assertTrue("Narration should include timeline info", 
            narrative.objectiveNarration.contains("CHRONOLOGICAL", ignoreCase = true))
    }

    @Test
    fun testGeneratesContradictionCommentary() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "John Doe",
            aliases = mutableListOf(),
            emails = mutableListOf(),
            phones = mutableListOf()
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "I paid the full amount",
            date = Date(System.currentTimeMillis() - 1000000),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("paid", "full", "amount")
        )

        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "I never paid anything",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("never", "paid")
        )

        val contradictions = listOf(
            Contradiction(
                entityId = "entity1",
                statementA = stmt1,
                statementB = stmt2,
                type = ContradictionType.DIRECT,
                severity = Severity.CRITICAL,
                description = "Direct contradiction about payment",
                legalImplication = "This indicates dishonesty"
            )
        )

        // Act
        val narrative = narrativeGenerator.generateNarrative(
            listOf(entity),
            emptyList(),
            contradictions,
            emptyList(),
            emptyMap()
        )

        // Assert
        assertTrue("Should generate contradiction commentary", narrative.contradictionCommentary.isNotBlank())
        assertTrue("Commentary should mention contradictions", 
            narrative.contradictionCommentary.contains("contradiction", ignoreCase = true))
        assertTrue("Commentary should include entity name",
            narrative.contradictionCommentary.contains("John Doe", ignoreCase = true) ||
            narrative.contradictionCommentary.contains("entity", ignoreCase = true))
    }

    @Test
    fun testGeneratesDeductiveLogic() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Defendant",
            aliases = mutableListOf(),
            emails = mutableListOf(),
            phones = mutableListOf()
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "The deal never existed",
            date = Date(System.currentTimeMillis() - 1000000),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("deal", "never", "existed")
        )

        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "I admit the deal existed",
            date = Date(),
            sourceEvidenceId = "ev2",
            type = StatementType.ADMISSION,
            keywords = listOf("admit", "deal", "existed")
        )

        val contradictions = listOf(
            Contradiction(
                entityId = "entity1",
                statementA = stmt1,
                statementB = stmt2,
                type = ContradictionType.DIRECT,
                severity = Severity.CRITICAL,
                description = "Denial followed by admission",
                legalImplication = "False denial exposed"
            )
        )

        // Act
        val narrative = narrativeGenerator.generateNarrative(
            listOf(entity),
            emptyList(),
            contradictions,
            emptyList(),
            emptyMap()
        )

        // Assert
        assertTrue("Should generate deductive logic", narrative.deductiveLogic.isNotBlank())
        assertTrue("Deductive section should explain implications",
            narrative.deductiveLogic.contains("DEDUCT", ignoreCase = true) ||
            narrative.deductiveLogic.contains("logic", ignoreCase = true))
    }

    @Test
    fun testGeneratesFinalSummary() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Primary Defendant",
            aliases = mutableListOf(),
            emails = mutableListOf(),
            phones = mutableListOf()
        )

        val liabilityScores = mapOf(
            "entity1" to LiabilityScore(
                entityId = "entity1",
                overallScore = 85f,
                contradictionScore = 90f,
                behavioralScore = 80f,
                evidenceContributionScore = 75f,
                chronologicalConsistencyScore = 85f,
                causalResponsibilityScore = 70f
            )
        )

        // Act
        val narrative = narrativeGenerator.generateNarrative(
            listOf(entity),
            emptyList(),
            emptyList(),
            emptyList(),
            liabilityScores
        )

        // Assert
        assertTrue("Should generate final summary", narrative.finalSummary.isNotBlank())
        assertTrue("Summary should include liability info",
            narrative.finalSummary.contains("LIABILITY", ignoreCase = true) ||
            narrative.finalSummary.contains("score", ignoreCase = true))
    }

    @Test
    fun testHandlesEmptyInput() {
        // Act
        val narrative = narrativeGenerator.generateNarrative(
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyMap()
        )

        // Assert
        assertNotNull("Should handle empty input", narrative)
        // Even with empty input, should provide some narrative structure
        assertTrue("Objective narration should exist", narrative.objectiveNarration.isNotEmpty())
    }

    @Test
    fun testIncludesBehavioralAnalysis() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Suspect",
            aliases = mutableListOf(),
            emails = mutableListOf(),
            phones = mutableListOf()
        )

        val behavioralPatterns = listOf(
            BehavioralPattern(
                id = "bp1",
                entityId = "entity1",
                type = BehaviorType.GASLIGHTING,
                instances = listOf("You're imagining things", "That never happened"),
                firstDetectedAt = Date(),
                severity = Severity.HIGH
            ),
            BehavioralPattern(
                id = "bp2",
                entityId = "entity1",
                type = BehaviorType.BLAME_SHIFTING,
                instances = listOf("It's your fault", "You made me do this"),
                firstDetectedAt = Date(),
                severity = Severity.MEDIUM
            )
        )

        // Act
        val narrative = narrativeGenerator.generateNarrative(
            listOf(entity),
            emptyList(),
            emptyList(),
            behavioralPatterns,
            emptyMap()
        )

        // Assert
        assertTrue("Should include behavioral analysis", narrative.behavioralPatternAnalysis.isNotBlank())
        assertTrue("Should mention behavioral patterns",
            narrative.behavioralPatternAnalysis.contains("BEHAVIORAL", ignoreCase = true) ||
            narrative.behavioralPatternAnalysis.contains("pattern", ignoreCase = true))
    }

    @Test
    fun testCausalChainGeneration() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Actor",
            aliases = mutableListOf(),
            emails = mutableListOf(),
            phones = mutableListOf()
        )

        val timeline = listOf(
            TimelineEvent(
                id = "te1",
                date = Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000),
                description = "Initial action taken",
                sourceEvidenceId = "ev1",
                entityIds = listOf("entity1"),
                eventType = EventType.COMMUNICATION,
                significance = Significance.HIGH
            ),
            TimelineEvent(
                id = "te2",
                date = Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000),
                description = "Consequence occurred",
                sourceEvidenceId = "ev2",
                entityIds = listOf("entity1"),
                eventType = EventType.PAYMENT,
                significance = Significance.CRITICAL
            )
        )

        // Act
        val narrative = narrativeGenerator.generateNarrative(
            listOf(entity),
            timeline,
            emptyList(),
            emptyList(),
            emptyMap()
        )

        // Assert
        assertTrue("Should generate causal chain", narrative.causalChain.isNotBlank())
        assertTrue("Causal chain should show cause-effect",
            narrative.causalChain.contains("CAUSAL", ignoreCase = true) ||
            narrative.causalChain.contains("chain", ignoreCase = true))
    }

    @Test
    fun testNarrativeIncludesAllSections() {
        // Arrange - Create comprehensive case data
        val entity = Entity(
            id = "entity1",
            primaryName = "Complete Entity",
            aliases = mutableListOf(),
            emails = mutableListOf("entity@test.com"),
            phones = mutableListOf()
        )

        val timeline = listOf(
            TimelineEvent(
                id = "te1",
                date = Date(),
                description = "Event happened",
                sourceEvidenceId = "ev1",
                entityIds = listOf("entity1"),
                eventType = EventType.COMMUNICATION,
                significance = Significance.HIGH
            )
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "Statement 1",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )

        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "Statement 2",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("test")
        )

        val contradictions = listOf(
            Contradiction(
                entityId = "entity1",
                statementA = stmt1,
                statementB = stmt2,
                type = ContradictionType.DIRECT,
                severity = Severity.HIGH,
                description = "Test contradiction",
                legalImplication = "Test implication"
            )
        )

        val behavioralPatterns = listOf(
            BehavioralPattern(
                id = "bp1",
                entityId = "entity1",
                type = BehaviorType.DEFLECTION,
                instances = listOf("But what about..."),
                firstDetectedAt = Date(),
                severity = Severity.MEDIUM
            )
        )

        val liabilityScores = mapOf(
            "entity1" to LiabilityScore(
                entityId = "entity1",
                overallScore = 70f,
                contradictionScore = 75f,
                behavioralScore = 65f,
                evidenceContributionScore = 70f,
                chronologicalConsistencyScore = 75f,
                causalResponsibilityScore = 60f
            )
        )

        // Act
        val narrative = narrativeGenerator.generateNarrative(
            listOf(entity),
            timeline,
            contradictions,
            behavioralPatterns,
            liabilityScores
        )

        // Assert - All sections should be populated
        assertTrue("Objective narration exists", narrative.objectiveNarration.isNotBlank())
        assertTrue("Contradiction commentary exists", narrative.contradictionCommentary.isNotBlank())
        assertTrue("Behavioral analysis exists", narrative.behavioralPatternAnalysis.isNotBlank())
        assertTrue("Deductive logic exists", narrative.deductiveLogic.isNotBlank())
        assertTrue("Causal chain exists", narrative.causalChain.isNotBlank())
        assertTrue("Final summary exists", narrative.finalSummary.isNotBlank())
    }
}

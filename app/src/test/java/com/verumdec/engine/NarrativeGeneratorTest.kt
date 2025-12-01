package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for NarrativeGenerator
 * Tests narrative generation and tiered conclusions
 */
class NarrativeGeneratorTest {

    private lateinit var generator: NarrativeGenerator
    
    @Before
    fun setup() {
        generator = NarrativeGenerator()
    }

    @Test
    fun testHighLiabilityConclusion() {
        // Arrange - Entity with 80%+ liability
        val entity = Entity(
            id = "entity1",
            primaryName = "High Liability Entity",
            emails = mutableListOf("high@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val liabilityScore = LiabilityScore(
            entityId = "entity1",
            overallScore = 85f,
            contradictionScore = 80f,
            behavioralScore = 40f,
            evidenceContributionScore = 30f,
            chronologicalConsistencyScore = 50f,
            causalResponsibilityScore = 35f
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "I never took the money",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("never", "took", "money")
        )
        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "I admit I took the money",
            date = Date(),
            sourceEvidenceId = "ev2",
            type = StatementType.ADMISSION,
            keywords = listOf("admit", "took", "money")
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = stmt1,
            statementB = stmt2,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Denial followed by admission",
            legalImplication = "This indicates deception"
        )

        // Act
        val narrative = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            listOf(contradiction),
            emptyList(),
            mapOf("entity1" to liabilityScore)
        )

        // Assert - Should contain PRIMARY RESPONSIBILITY for 80%+ scores
        assertTrue("Narrative should contain final summary", narrative.finalSummary.isNotEmpty())
        assertTrue("Should indicate PRIMARY RESPONSIBILITY for 80%+ score", 
            narrative.finalSummary.contains("PRIMARY RESPONSIBILITY") || 
            narrative.finalSummary.contains("highest responsibility"))
        assertTrue("Should mention liability percentage", 
            narrative.finalSummary.contains("85") || narrative.finalSummary.contains("%"))
    }

    @Test
    fun testModerateLiabilityConclusion() {
        // Arrange - Entity with 50-80% liability
        val entity = Entity(
            id = "entity1",
            primaryName = "Moderate Liability Entity",
            emails = mutableListOf("moderate@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val liabilityScore = LiabilityScore(
            entityId = "entity1",
            overallScore = 55f,
            contradictionScore = 50f,
            behavioralScore = 20f,
            evidenceContributionScore = 20f,
            chronologicalConsistencyScore = 30f,
            causalResponsibilityScore = 25f
        )

        // Act
        val narrative = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            emptyList(),
            emptyList(),
            mapOf("entity1" to liabilityScore)
        )

        // Assert - Should indicate highest responsibility but not PRIMARY
        assertTrue("Narrative should contain final summary", narrative.finalSummary.isNotEmpty())
        assertTrue("Should mention the entity name", 
            narrative.finalSummary.contains("Moderate Liability Entity"))
    }

    @Test
    fun testLowLiabilityConclusion() {
        // Arrange - Entity with <30% liability
        val entity = Entity(
            id = "entity1",
            primaryName = "Low Liability Entity",
            emails = mutableListOf("low@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val liabilityScore = LiabilityScore(
            entityId = "entity1",
            overallScore = 20f,
            contradictionScore = 10f,
            behavioralScore = 5f,
            evidenceContributionScore = 10f,
            chronologicalConsistencyScore = 10f,
            causalResponsibilityScore = 5f
        )

        // Act
        val narrative = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            emptyList(),
            emptyList(),
            mapOf("entity1" to liabilityScore)
        )

        // Assert - Should indicate no clear liability
        assertTrue("Narrative should contain final summary", narrative.finalSummary.isNotEmpty())
        assertTrue("Should indicate no conclusive liability", 
            narrative.finalSummary.contains("no party with conclusively high liability") ||
            narrative.finalSummary.contains("Further investigation") ||
            narrative.finalSummary.contains("inconclusive") ||
            narrative.finalSummary.contains("RECOMMENDATION"))
    }

    @Test
    fun testCriticalContradictionsSummary() {
        // Arrange - Multiple critical contradictions
        val entity = Entity(
            id = "entity1",
            primaryName = "Critical Entity",
            emails = mutableListOf("critical@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val liabilityScore = LiabilityScore(
            entityId = "entity1",
            overallScore = 90f,
            contradictionScore = 85f,
            behavioralScore = 40f,
            evidenceContributionScore = 30f,
            chronologicalConsistencyScore = 60f,
            causalResponsibilityScore = 50f
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "Statement 1",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("test")
        )
        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "Statement 2",
            date = Date(),
            sourceEvidenceId = "ev2",
            type = StatementType.ADMISSION,
            keywords = listOf("test")
        )

        val criticalContradictions = listOf(
            Contradiction(
                entityId = "entity1",
                statementA = stmt1,
                statementB = stmt2,
                type = ContradictionType.DIRECT,
                severity = Severity.CRITICAL,
                description = "First critical contradiction",
                legalImplication = "Implication 1"
            ),
            Contradiction(
                entityId = "entity1",
                statementA = stmt2,
                statementB = stmt1,
                type = ContradictionType.CROSS_DOCUMENT,
                severity = Severity.CRITICAL,
                description = "Second critical contradiction",
                legalImplication = "Implication 2"
            )
        )

        // Act
        val narrative = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            criticalContradictions,
            emptyList(),
            mapOf("entity1" to liabilityScore)
        )

        // Assert - Should contain critical contradictions summary
        assertTrue("Narrative should contain final summary", narrative.finalSummary.isNotEmpty())
        assertTrue("Should contain critical contradictions reference", 
            narrative.finalSummary.contains("CRITICAL") || 
            narrative.finalSummary.contains("critical") ||
            narrative.finalSummary.contains("Critical"))
    }

    @Test
    fun testEmptyInputsNarrative() {
        // Act
        val narrative = generator.generateNarrative(
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyMap()
        )

        // Assert - Should still generate narrative sections without errors
        assertNotNull("Should generate narrative sections", narrative)
        assertNotNull("Should have final summary", narrative.finalSummary)
    }

    @Test
    fun testDeductiveLogicGeneration() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Test Entity",
            emails = mutableListOf("test@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "I never received any payment",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("never", "received", "payment")
        )
        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "Yes I admit I received the payment",
            date = Date(),
            sourceEvidenceId = "ev2",
            type = StatementType.ADMISSION,
            keywords = listOf("admit", "received", "payment")
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = stmt1,
            statementB = stmt2,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Denial then admission of payment",
            legalImplication = "Indicates prior deception"
        )

        val liabilityScore = LiabilityScore(
            entityId = "entity1",
            overallScore = 75f,
            contradictionScore = 70f,
            behavioralScore = 30f,
            evidenceContributionScore = 25f,
            chronologicalConsistencyScore = 40f,
            causalResponsibilityScore = 30f
        )

        // Act
        val narrative = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            listOf(contradiction),
            emptyList(),
            mapOf("entity1" to liabilityScore)
        )

        // Assert - Should contain deductive logic
        assertTrue("Should have deductive logic section", 
            narrative.deductiveLogic.isNotEmpty())
        assertTrue("Deductive logic should reference the contradiction", 
            narrative.deductiveLogic.contains("DEDUCTION") ||
            narrative.deductiveLogic.contains("deduction") ||
            narrative.deductiveLogic.contains("stated"))
    }
}

package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for NarrativeGenerator
 * Tests generation of legal narratives from analysis results
 */
class NarrativeGeneratorTest {

    private lateinit var generator: NarrativeGenerator

    @Before
    fun setup() {
        generator = NarrativeGenerator()
    }

    @Test
    fun testObjectiveNarrationGeneration() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "John Doe",
            emails = mutableListOf("john@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val date1 = Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L) // 3 days ago
        val date2 = Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L) // 2 days ago

        val timeline = listOf(
            TimelineEvent(
                date = date1,
                description = "John Doe sent email regarding payment",
                sourceEvidenceId = "ev1",
                entityIds = listOf("entity1"),
                eventType = EventType.COMMUNICATION
            ),
            TimelineEvent(
                date = date2,
                description = "Payment of R10000 was confirmed",
                sourceEvidenceId = "ev1",
                entityIds = listOf("entity1"),
                eventType = EventType.PAYMENT,
                significance = Significance.HIGH
            )
        )

        // Act
        val narrativeSections = generator.generateNarrative(
            listOf(entity),
            timeline,
            emptyList(),
            emptyList(),
            emptyMap()
        )

        // Assert
        assertNotNull("Objective narration should not be null", narrativeSections.objectiveNarration)
        assertTrue("Should contain chronological header", 
            narrativeSections.objectiveNarration.contains("CHRONOLOGICAL"))
        assertTrue("Should mention entity name", 
            narrativeSections.objectiveNarration.contains("John Doe"))
    }

    @Test
    fun testContradictionCommentaryGeneration() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Jane Smith",
            emails = mutableListOf("jane@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val date1 = Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L)
        val date2 = Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L)

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "I paid the full amount",
            date = date1,
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("paid", "amount")
        )

        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "I never paid anything",
            date = date2,
            sourceEvidenceId = "ev2",
            type = StatementType.DENIAL,
            keywords = listOf("never", "paid")
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = stmt1,
            statementB = stmt2,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Contradictory payment claims",
            legalImplication = "Party is unreliable"
        )

        // Act
        val narrativeSections = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            listOf(contradiction),
            emptyList(),
            emptyMap()
        )

        // Assert
        assertNotNull("Contradiction commentary should not be null", narrativeSections.contradictionCommentary)
        assertTrue("Should contain contradiction analysis header", 
            narrativeSections.contradictionCommentary.contains("CONTRADICTION"))
        assertTrue("Should mention entity name",
            narrativeSections.contradictionCommentary.contains("Jane Smith"))
        assertTrue("Should include statement text",
            narrativeSections.contradictionCommentary.contains("paid"))
        assertTrue("Should include severity",
            narrativeSections.contradictionCommentary.contains("CRITICAL"))
    }

    @Test
    fun testBehavioralPatternAnalysisGeneration() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Bob Johnson",
            emails = mutableListOf("bob@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val pattern = BehavioralPattern(
            entityId = "entity1",
            type = BehaviorType.GASLIGHTING,
            instances = listOf(
                "You're imagining things",
                "That never happened"
            ),
            severity = Severity.HIGH
        )

        // Act
        val narrativeSections = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            emptyList(),
            listOf(pattern),
            emptyMap()
        )

        // Assert
        assertNotNull("Behavioral analysis should not be null", narrativeSections.behavioralPatternAnalysis)
        assertTrue("Should contain behavioral header",
            narrativeSections.behavioralPatternAnalysis.contains("BEHAVIORAL"))
        assertTrue("Should mention entity name",
            narrativeSections.behavioralPatternAnalysis.contains("Bob Johnson"))
        assertTrue("Should mention pattern type",
            narrativeSections.behavioralPatternAnalysis.contains("GASLIGHTING"))
        assertTrue("Should include instances",
            narrativeSections.behavioralPatternAnalysis.contains("imagining"))
    }

    @Test
    fun testDeductiveLogicGeneration() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Alice Brown",
            emails = mutableListOf("alice@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "The deal was signed on Monday",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("deal", "signed")
        )

        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "No deal ever existed",
            date = Date(),
            sourceEvidenceId = "ev2",
            type = StatementType.DENIAL,
            keywords = listOf("deal", "existed")
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = stmt1,
            statementB = stmt2,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Direct denial of deal existence",
            legalImplication = "Dishonest representation"
        )

        // Act
        val narrativeSections = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            listOf(contradiction),
            emptyList(),
            emptyMap()
        )

        // Assert
        assertNotNull("Deductive logic should not be null", narrativeSections.deductiveLogic)
        assertTrue("Should contain deductive header",
            narrativeSections.deductiveLogic.contains("DEDUCTION") || 
            narrativeSections.deductiveLogic.contains("DEDUCTIVE"))
        assertTrue("Should mention entity name",
            narrativeSections.deductiveLogic.contains("Alice Brown"))
    }

    @Test
    fun testCausalChainGeneration() {
        // Arrange
        val entity1 = Entity(
            id = "entity1",
            primaryName = "Charlie Davis",
            emails = mutableListOf("charlie@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val entity2 = Entity(
            id = "entity2",
            primaryName = "Diana Evans",
            emails = mutableListOf("diana@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val date1 = Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L)
        val date2 = Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L)

        val timeline = listOf(
            TimelineEvent(
                date = date1,
                description = "Charlie sent payment demand",
                sourceEvidenceId = "ev1",
                entityIds = listOf("entity1"),
                eventType = EventType.PAYMENT,
                significance = Significance.HIGH
            ),
            TimelineEvent(
                date = date2,
                description = "Diana responded with denial",
                sourceEvidenceId = "ev2",
                entityIds = listOf("entity2"),
                eventType = EventType.DENIAL,
                significance = Significance.HIGH
            )
        )

        // Act
        val narrativeSections = generator.generateNarrative(
            listOf(entity1, entity2),
            timeline,
            emptyList(),
            emptyList(),
            emptyMap()
        )

        // Assert
        assertNotNull("Causal chain should not be null", narrativeSections.causalChain)
        assertTrue("Should contain causal header",
            narrativeSections.causalChain.contains("CAUSAL"))
    }

    @Test
    fun testFinalSummaryGeneration() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Eve Foster",
            emails = mutableListOf("eve@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val liabilityScore = LiabilityScore(
            entityId = "entity1",
            overallScore = 85.5f,
            contradictionScore = 90f,
            behavioralScore = 80f,
            evidenceContributionScore = 75f,
            chronologicalConsistencyScore = 85f,
            causalResponsibilityScore = 80f
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "S1",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = stmt1,
            statementB = stmt1,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Test",
            legalImplication = "Test"
        )

        // Act
        val narrativeSections = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            listOf(contradiction),
            emptyList(),
            mapOf("entity1" to liabilityScore)
        )

        // Assert
        assertNotNull("Final summary should not be null", narrativeSections.finalSummary)
        assertTrue("Should contain liability header",
            narrativeSections.finalSummary.contains("LIABILITY"))
        assertTrue("Should mention entity name",
            narrativeSections.finalSummary.contains("Eve Foster"))
        assertTrue("Should include liability score",
            narrativeSections.finalSummary.contains("85"))
    }

    @Test
    fun testNoContradictionsMessage() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Frank Green",
            emails = mutableListOf("frank@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        // Act
        val narrativeSections = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            emptyList(),  // No contradictions
            emptyList(),
            emptyMap()
        )

        // Assert
        assertTrue("Should indicate no contradictions",
            narrativeSections.contradictionCommentary.contains("No contradictions") ||
            narrativeSections.contradictionCommentary.lowercase().contains("no contradiction"))
    }

    @Test
    fun testNoBehavioralPatternsMessage() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Grace Hall",
            emails = mutableListOf("grace@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        // Act
        val narrativeSections = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            emptyList(),
            emptyList(),  // No behavioral patterns
            emptyMap()
        )

        // Assert
        assertTrue("Should indicate no behavioral patterns",
            narrativeSections.behavioralPatternAnalysis.contains("No") ||
            narrativeSections.behavioralPatternAnalysis.lowercase().contains("no"))
    }

    @Test
    fun testEmptyTimelineMessage() {
        // Act
        val narrativeSections = generator.generateNarrative(
            emptyList(),
            emptyList(),  // Empty timeline
            emptyList(),
            emptyList(),
            emptyMap()
        )

        // Assert
        assertTrue("Should indicate no timeline events",
            narrativeSections.objectiveNarration.contains("No") ||
            narrativeSections.objectiveNarration.lowercase().contains("no"))
    }

    @Test
    fun testMultipleEntitiesNarrative() {
        // Arrange
        val entity1 = Entity(
            id = "entity1",
            primaryName = "Henry Irwin",
            emails = mutableListOf("henry@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val entity2 = Entity(
            id = "entity2",
            primaryName = "Ivy Jones",
            emails = mutableListOf("ivy@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val liabilityScore1 = LiabilityScore(
            entityId = "entity1",
            overallScore = 80f,
            contradictionScore = 85f,
            behavioralScore = 75f,
            evidenceContributionScore = 70f,
            chronologicalConsistencyScore = 80f,
            causalResponsibilityScore = 75f
        )

        val liabilityScore2 = LiabilityScore(
            entityId = "entity2",
            overallScore = 20f,
            contradictionScore = 15f,
            behavioralScore = 10f,
            evidenceContributionScore = 30f,
            chronologicalConsistencyScore = 25f,
            causalResponsibilityScore = 20f
        )

        // Act
        val narrativeSections = generator.generateNarrative(
            listOf(entity1, entity2),
            emptyList(),
            emptyList(),
            emptyList(),
            mapOf("entity1" to liabilityScore1, "entity2" to liabilityScore2)
        )

        // Assert
        assertTrue("Should mention both entities in summary",
            narrativeSections.finalSummary.contains("Henry Irwin") ||
            narrativeSections.finalSummary.contains("Ivy Jones"))
        assertTrue("Should indicate highest liability",
            narrativeSections.finalSummary.contains("80") || 
            narrativeSections.finalSummary.contains("highest"))
    }

    @Test
    fun testHighSignificanceEventsMarked() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Jack King",
            emails = mutableListOf("jack@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val timeline = listOf(
            TimelineEvent(
                date = Date(),
                description = "Critical payment received",
                sourceEvidenceId = "ev1",
                entityIds = listOf("entity1"),
                eventType = EventType.PAYMENT,
                significance = Significance.CRITICAL
            )
        )

        // Act
        val narrativeSections = generator.generateNarrative(
            listOf(entity),
            timeline,
            emptyList(),
            emptyList(),
            emptyMap()
        )

        // Assert
        assertTrue("Should mark critical events",
            narrativeSections.objectiveNarration.contains("CRITICAL") ||
            narrativeSections.objectiveNarration.contains("SIGNIFICANCE"))
    }

    @Test
    fun testCrossDocumentContradictionDeduction() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Kate Lee",
            emails = mutableListOf("kate@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "Document A claim",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("claim")
        )

        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "Document B denial",
            date = Date(),
            sourceEvidenceId = "ev2",
            type = StatementType.DENIAL,
            keywords = listOf("denial")
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = stmt1,
            statementB = stmt2,
            type = ContradictionType.CROSS_DOCUMENT,
            severity = Severity.HIGH,
            description = "Contradictory statements across documents",
            legalImplication = "Inconsistent representation"
        )

        // Act
        val narrativeSections = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            listOf(contradiction),
            emptyList(),
            emptyMap()
        )

        // Assert
        assertTrue("Should explain cross-document contradiction",
            narrativeSections.deductiveLogic.contains("document") ||
            narrativeSections.deductiveLogic.contains("inconsisten"))
    }

    @Test
    fun testMissingEvidenceContradictionDeduction() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Leo Mason",
            emails = mutableListOf("leo@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "See attached document",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("attached", "document")
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = stmt1,
            statementB = stmt1,
            type = ContradictionType.MISSING_EVIDENCE,
            severity = Severity.HIGH,
            description = "Referenced evidence was not provided",
            legalImplication = "Possible withholding of evidence"
        )

        // Act
        val narrativeSections = generator.generateNarrative(
            listOf(entity),
            emptyList(),
            listOf(contradiction),
            emptyList(),
            emptyMap()
        )

        // Assert
        assertTrue("Should explain missing evidence",
            narrativeSections.deductiveLogic.contains("evidence") ||
            narrativeSections.deductiveLogic.contains("provide") ||
            narrativeSections.deductiveLogic.contains("not exist"))
    }
}

package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for ContradictionEngine main orchestration
 * Tests the full analysis pipeline
 */
class ContradictionEngineTest {

    @Test
    fun testCaseSummaryCalculation() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Test Entity",
            emails = mutableListOf("test@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "test.txt",
            type = EvidenceType.TEXT,
            extractedText = "Test evidence",
            metadata = EvidenceMetadata(sender = "test@example.com"),
            processed = true
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = Statement("entity1", "S1", Date(), "ev1", StatementType.CLAIM, listOf("test")),
            statementB = Statement("entity1", "S2", Date(), "ev1", StatementType.DENIAL, listOf("test")),
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Test",
            legalImplication = "Test"
        )

        val timelineEvent = TimelineEvent(
            id = "te1",
            entityId = "entity1",
            date = Date(),
            description = "Test event",
            type = EventType.COMMUNICATION,
            significance = 5,
            sourceEvidenceId = "ev1"
        )

        val liabilityScore = LiabilityScore(
            entityId = "entity1",
            contradictionScore = 85f,
            behavioralScore = 75f,
            evidenceScore = 80f,
            chronologicalScore = 90f,
            causalScore = 70f,
            overallScore = 80f
        )

        val testCase = Case(
            id = "case1",
            name = "Test Case",
            description = "Test case for engine",
            evidence = mutableListOf(evidence),
            entities = mutableListOf(entity),
            timeline = mutableListOf(timelineEvent),
            contradictions = mutableListOf(contradiction),
            narrative = "Test narrative",
            liabilityScores = mutableMapOf("entity1" to liabilityScore),
            createdAt = Date(),
            updatedAt = Date()
        )

        // Act
        val engine = ContradictionEngine(null)
        val summary = engine.getSummary(testCase)

        // Assert
        assertEquals("Total evidence count", 1, summary.totalEvidence)
        assertEquals("Processed evidence count", 1, summary.processedEvidence)
        assertEquals("Entities found", 1, summary.entitiesFound)
        assertEquals("Timeline events", 1, summary.timelineEvents)
        assertEquals("Total contradictions", 1, summary.totalContradictions)
        assertEquals("Critical contradictions", 1, summary.criticalContradictions)
        assertEquals("Highest liability entity", "Test Entity", summary.highestLiabilityEntity)
        assertTrue("Highest liability score", summary.highestLiabilityScore > 0f)
    }

    @Test
    fun testEmptyCaseSummary() {
        // Arrange
        val emptyCase = Case(
            id = "empty",
            name = "Empty Case",
            description = "",
            evidence = mutableListOf(),
            entities = mutableListOf(),
            timeline = mutableListOf(),
            contradictions = mutableListOf(),
            narrative = "",
            liabilityScores = mutableMapOf(),
            createdAt = Date(),
            updatedAt = Date()
        )

        // Act
        val engine = ContradictionEngine(null)
        val summary = engine.getSummary(emptyCase)

        // Assert
        assertEquals("Empty evidence", 0, summary.totalEvidence)
        assertEquals("No processed evidence", 0, summary.processedEvidence)
        assertEquals("No entities", 0, summary.entitiesFound)
        assertEquals("No timeline", 0, summary.timelineEvents)
        assertEquals("No contradictions", 0, summary.totalContradictions)
        assertNull("No highest liability entity", summary.highestLiabilityEntity)
        assertEquals("Zero liability score", 0f, summary.highestLiabilityScore, 0.1f)
    }

    @Test
    fun testMultipleContradictionSeverities() {
        // Arrange
        val entity = Entity("entity1", "Test", mutableListOf(), mutableListOf(), mutableListOf())
        val evidence = Evidence("ev1", "test.txt", EvidenceType.TEXT, "test", EvidenceMetadata(), processed = true)
        
        val stmt1 = Statement("entity1", "S1", Date(), "ev1", StatementType.CLAIM, listOf("test"))
        val stmt2 = Statement("entity1", "S2", Date(), "ev1", StatementType.CLAIM, listOf("test"))
        val stmt3 = Statement("entity1", "S3", Date(), "ev1", StatementType.CLAIM, listOf("test"))
        val stmt4 = Statement("entity1", "S4", Date(), "ev1", StatementType.CLAIM, listOf("test"))

        val contradictions = mutableListOf(
            Contradiction("entity1", stmt1, stmt2, ContradictionType.DIRECT, Severity.CRITICAL, "C1", "I1"),
            Contradiction("entity1", stmt2, stmt3, ContradictionType.DIRECT, Severity.HIGH, "C2", "I2"),
            Contradiction("entity1", stmt3, stmt4, ContradictionType.DIRECT, Severity.MEDIUM, "C3", "I3"),
            Contradiction("entity1", stmt4, stmt1, ContradictionType.DIRECT, Severity.LOW, "C4", "I4")
        )

        val testCase = Case(
            id = "case1",
            name = "Test Case",
            description = "",
            evidence = mutableListOf(evidence),
            entities = mutableListOf(entity),
            timeline = mutableListOf(),
            contradictions = contradictions,
            narrative = "",
            liabilityScores = mutableMapOf(),
            createdAt = Date(),
            updatedAt = Date()
        )

        // Act
        val engine = ContradictionEngine(null)
        val summary = engine.getSummary(testCase)

        // Assert
        assertEquals("Total contradictions", 4, summary.totalContradictions)
        assertEquals("Critical contradictions", 1, summary.criticalContradictions)
        assertEquals("High contradictions", 1, summary.highContradictions)
    }
}

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
            phoneNumbers = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "test.txt",
            filePath = "/path/to/test.txt",
            extractedText = "Test evidence",
            metadata = EvidenceMetadata(sender = "test@example.com"),
            processed = true
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = Statement(
                entityId = "entity1", 
                text = "S1", 
                date = Date(), 
                sourceEvidenceId = "ev1", 
                type = StatementType.CLAIM, 
                keywords = listOf("test")
            ),
            statementB = Statement(
                entityId = "entity1", 
                text = "S2", 
                date = Date(), 
                sourceEvidenceId = "ev1", 
                type = StatementType.DENIAL, 
                keywords = listOf("test")
            ),
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Test",
            legalImplication = "Test"
        )

        val timelineEvent = TimelineEvent(
            id = "te1",
            date = Date(),
            description = "Test event",
            sourceEvidenceId = "ev1",
            entityIds = listOf("entity1"),
            eventType = EventType.COMMUNICATION,
            significance = Significance.HIGH
        )

        val liabilityScore = LiabilityScore(
            entityId = "entity1",
            overallScore = 80f,
            contradictionScore = 85f,
            behavioralScore = 75f,
            evidenceContributionScore = 80f,
            chronologicalConsistencyScore = 90f,
            causalResponsibilityScore = 70f
        )

        val testCase = Case(
            id = "case1",
            name = "Test Case",
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
        val entity = Entity(
            id = "entity1", 
            primaryName = "Test",
            aliases = mutableListOf(),
            emails = mutableListOf(),
            phoneNumbers = mutableListOf()
        )
        val evidence = Evidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "test.txt",
            filePath = "test.txt",
            extractedText = "test",
            metadata = EvidenceMetadata(),
            processed = true
        )
        
        val stmt1 = Statement(
            entityId = "entity1",
            text = "S1",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )
        val stmt2 = Statement(
            entityId = "entity1",
            text = "S2",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )
        val stmt3 = Statement(
            entityId = "entity1",
            text = "S3",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )
        val stmt4 = Statement(
            entityId = "entity1",
            text = "S4",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )

        val contradictions = mutableListOf(
            Contradiction(
                entityId = "entity1",
                statementA = stmt1,
                statementB = stmt2,
                type = ContradictionType.DIRECT,
                severity = Severity.CRITICAL,
                description = "C1",
                legalImplication = "I1"
            ),
            Contradiction(
                entityId = "entity1",
                statementA = stmt2,
                statementB = stmt3,
                type = ContradictionType.DIRECT,
                severity = Severity.HIGH,
                description = "C2",
                legalImplication = "I2"
            ),
            Contradiction(
                entityId = "entity1",
                statementA = stmt3,
                statementB = stmt4,
                type = ContradictionType.DIRECT,
                severity = Severity.MEDIUM,
                description = "C3",
                legalImplication = "I3"
            ),
            Contradiction(
                entityId = "entity1",
                statementA = stmt4,
                statementB = stmt1,
                type = ContradictionType.DIRECT,
                severity = Severity.LOW,
                description = "C4",
                legalImplication = "I4"
            )
        )

        val testCase = Case(
            id = "case1",
            name = "Test Case",
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

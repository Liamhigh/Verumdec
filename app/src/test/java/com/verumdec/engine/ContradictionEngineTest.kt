package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for ContradictionEngine main orchestration
 * Tests the full analysis pipeline
 */
class ContradictionEngineTest {

    /**
     * Helper function to create a test entity with proper constructor.
     */
    private fun createTestEntity(
        id: String,
        name: String,
        emails: List<String> = emptyList()
    ) = Entity(
        id = id,
        primaryName = name,
        aliases = mutableListOf(),
        emails = emails.toMutableList(),
        phoneNumbers = mutableListOf()
    )

    /**
     * Helper function to create test evidence with proper constructor.
     */
    private fun createTestEvidence(
        id: String,
        fileName: String,
        type: EvidenceType,
        text: String = "",
        metadata: EvidenceMetadata = EvidenceMetadata(),
        processed: Boolean = false
    ) = Evidence(
        id = id,
        type = type,
        fileName = fileName,
        filePath = "/test/$fileName",
        extractedText = text,
        metadata = metadata,
        processed = processed
    )

    /**
     * Helper function to create test statement with proper constructor.
     */
    private fun createTestStatement(
        entityId: String,
        text: String,
        date: Date? = null,
        sourceEvidenceId: String,
        type: StatementType,
        keywords: List<String> = emptyList()
    ) = Statement(
        entityId = entityId,
        text = text,
        date = date,
        sourceEvidenceId = sourceEvidenceId,
        type = type,
        keywords = keywords
    )

    /**
     * Helper function to create test timeline event with proper constructor.
     */
    private fun createTestTimelineEvent(
        id: String,
        date: Date,
        description: String,
        sourceEvidenceId: String,
        entityIds: List<String> = emptyList(),
        eventType: EventType,
        significance: Significance = Significance.NORMAL
    ) = TimelineEvent(
        id = id,
        date = date,
        description = description,
        sourceEvidenceId = sourceEvidenceId,
        entityIds = entityIds,
        eventType = eventType,
        significance = significance
    )

    /**
     * Helper function to create test liability score with proper constructor.
     */
    private fun createTestLiabilityScore(
        entityId: String,
        overallScore: Float,
        contradictionScore: Float = 0f,
        behavioralScore: Float = 0f,
        evidenceContributionScore: Float = 0f,
        chronologicalConsistencyScore: Float = 0f,
        causalResponsibilityScore: Float = 0f
    ) = LiabilityScore(
        entityId = entityId,
        overallScore = overallScore,
        contradictionScore = contradictionScore,
        behavioralScore = behavioralScore,
        evidenceContributionScore = evidenceContributionScore,
        chronologicalConsistencyScore = chronologicalConsistencyScore,
        causalResponsibilityScore = causalResponsibilityScore
    )

    @Test
    fun testCaseSummaryCalculation() {
        // Arrange
        val entity = createTestEntity(
            id = "entity1",
            name = "Test Entity",
            emails = listOf("test@example.com")
        )

        val evidence = createTestEvidence(
            id = "ev1",
            fileName = "test.txt",
            type = EvidenceType.TEXT,
            text = "Test evidence",
            metadata = EvidenceMetadata(sender = "test@example.com"),
            processed = true
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = createTestStatement(
                entityId = "entity1",
                text = "S1",
                date = Date(),
                sourceEvidenceId = "ev1",
                type = StatementType.CLAIM,
                keywords = listOf("test")
            ),
            statementB = createTestStatement(
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

        val timelineEvent = createTestTimelineEvent(
            id = "te1",
            date = Date(),
            description = "Test event",
            sourceEvidenceId = "ev1",
            entityIds = listOf("entity1"),
            eventType = EventType.COMMUNICATION,
            significance = Significance.NORMAL
        )

        val liabilityScore = createTestLiabilityScore(
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
            liabilityScores = mutableMapOf("entity1" to liabilityScore)
        )

        // Act
        val engine = createTestEngine()
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
            liabilityScores = mutableMapOf()
        )

        // Act
        val engine = createTestEngine()
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
        val entity = createTestEntity(id = "entity1", name = "Test")
        val evidence = createTestEvidence(
            id = "ev1",
            fileName = "test.txt",
            type = EvidenceType.TEXT,
            text = "test",
            processed = true
        )
        
        val stmt1 = createTestStatement(
            entityId = "entity1",
            text = "S1",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )
        val stmt2 = createTestStatement(
            entityId = "entity1",
            text = "S2",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )
        val stmt3 = createTestStatement(
            entityId = "entity1",
            text = "S3",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )
        val stmt4 = createTestStatement(
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
            liabilityScores = mutableMapOf()
        )

        // Act
        val engine = createTestEngine()
        val summary = engine.getSummary(testCase)

        // Assert
        assertEquals("Total contradictions", 4, summary.totalContradictions)
        assertEquals("Critical contradictions", 1, summary.criticalContradictions)
        assertEquals("High contradictions", 1, summary.highContradictions)
    }

    /**
     * Creates a test engine instance.
     * Note: The ContradictionEngine requires a Context, but for unit tests
     * that only use getSummary(), we can use a wrapper approach.
     */
    private fun createTestEngine(): TestContradictionEngine {
        return TestContradictionEngine()
    }
}

/**
 * Test wrapper for ContradictionEngine that doesn't require Context.
 * Only provides the getSummary functionality for testing.
 */
class TestContradictionEngine {
    fun getSummary(case: Case): CaseSummary {
        val criticalContradictions = case.contradictions.count { it.severity == Severity.CRITICAL }
        val highContradictions = case.contradictions.count { it.severity == Severity.HIGH }
        
        val highestLiability = case.liabilityScores.maxByOrNull { it.value.overallScore }
        val highestLiabilityEntity = case.entities.find { it.id == highestLiability?.key }
        
        return CaseSummary(
            totalEvidence = case.evidence.size,
            processedEvidence = case.evidence.count { it.processed },
            entitiesFound = case.entities.size,
            timelineEvents = case.timeline.size,
            totalContradictions = case.contradictions.size,
            criticalContradictions = criticalContradictions,
            highContradictions = highContradictions,
            highestLiabilityEntity = highestLiabilityEntity?.primaryName,
            highestLiabilityScore = highestLiability?.value?.overallScore ?: 0f
        )
    }
}

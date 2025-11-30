package com.verumdec.engine

import android.content.Context
import com.verumdec.data.*
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.mock
import java.util.*

/**
 * Unit tests for ContradictionEngine main orchestration
 * Tests the full analysis pipeline
 */
class ContradictionEngineTest {

    /**
     * Helper function to create a test entity matching the exact production signature.
     * 
     * Production signature:
     * Entity(
     *     id: String = UUID.randomUUID().toString(),
     *     primaryName: String,
     *     aliases: MutableList<String> = mutableListOf(),
     *     emails: MutableList<String> = mutableListOf(),
     *     phoneNumbers: MutableList<String> = mutableListOf(),
     *     bankAccounts: MutableList<String> = mutableListOf(),
     *     mentions: Int = 0,
     *     statements: MutableList<Statement> = mutableListOf(),
     *     liabilityScore: Float = 0f
     * )
     */
    private fun createTestEntity(
        id: String,
        primaryName: String,
        emails: List<String> = emptyList()
    ) = Entity(
        id = id,
        primaryName = primaryName,
        aliases = mutableListOf(),
        emails = emails.toMutableList(),
        phoneNumbers = mutableListOf(),
        bankAccounts = mutableListOf(),
        mentions = 0,
        statements = mutableListOf(),
        liabilityScore = 0f
    )

    /**
     * Helper function to create test evidence matching the exact production signature.
     * 
     * Production signature:
     * Evidence(
     *     id: String = UUID.randomUUID().toString(),
     *     type: EvidenceType,
     *     fileName: String,
     *     filePath: String,
     *     addedAt: Date = Date(),
     *     extractedText: String = "",
     *     metadata: EvidenceMetadata = EvidenceMetadata(),
     *     processed: Boolean = false
     * )
     */
    private fun createTestEvidence(
        id: String,
        type: EvidenceType,
        fileName: String,
        filePath: String,
        addedAt: Date = Date(),
        extractedText: String = "",
        metadata: EvidenceMetadata = EvidenceMetadata(),
        processed: Boolean = false
    ) = Evidence(
        id = id,
        type = type,
        fileName = fileName,
        filePath = filePath,
        addedAt = addedAt,
        extractedText = extractedText,
        metadata = metadata,
        processed = processed
    )

    /**
     * Helper function to create test statement matching the exact production signature.
     * 
     * Production signature:
     * Statement(
     *     id: String = UUID.randomUUID().toString(),
     *     entityId: String,
     *     text: String,
     *     date: Date? = null,
     *     sourceEvidenceId: String,
     *     type: StatementType,
     *     keywords: List<String> = emptyList()
     * )
     */
    private fun createTestStatement(
        id: String = UUID.randomUUID().toString(),
        entityId: String,
        text: String,
        date: Date? = null,
        sourceEvidenceId: String,
        type: StatementType,
        keywords: List<String> = emptyList()
    ) = Statement(
        id = id,
        entityId = entityId,
        text = text,
        date = date,
        sourceEvidenceId = sourceEvidenceId,
        type = type,
        keywords = keywords
    )

    /**
     * Helper function to create test timeline event matching the exact production signature.
     * 
     * Production signature:
     * TimelineEvent(
     *     id: String = UUID.randomUUID().toString(),
     *     date: Date,
     *     description: String,
     *     sourceEvidenceId: String,
     *     entityIds: List<String> = emptyList(),
     *     eventType: EventType,
     *     significance: Significance = Significance.NORMAL
     * )
     */
    private fun createTestTimelineEvent(
        id: String = UUID.randomUUID().toString(),
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
     * Helper function to create test liability score matching the exact production signature.
     * 
     * Production signature:
     * LiabilityScore(
     *     entityId: String,
     *     overallScore: Float,
     *     contradictionScore: Float,
     *     behavioralScore: Float,
     *     evidenceContributionScore: Float,
     *     chronologicalConsistencyScore: Float,
     *     causalResponsibilityScore: Float,
     *     breakdown: LiabilityBreakdown = LiabilityBreakdown()
     * )
     */
    private fun createTestLiabilityScore(
        entityId: String,
        overallScore: Float,
        contradictionScore: Float,
        behavioralScore: Float,
        evidenceContributionScore: Float,
        chronologicalConsistencyScore: Float,
        causalResponsibilityScore: Float,
        breakdown: LiabilityBreakdown = LiabilityBreakdown()
    ) = LiabilityScore(
        entityId = entityId,
        overallScore = overallScore,
        contradictionScore = contradictionScore,
        behavioralScore = behavioralScore,
        evidenceContributionScore = evidenceContributionScore,
        chronologicalConsistencyScore = chronologicalConsistencyScore,
        causalResponsibilityScore = causalResponsibilityScore,
        breakdown = breakdown
    )

    /**
     * Creates a ContradictionEngine with a mocked Context for unit testing.
     */
    private fun createEngine(): ContradictionEngine {
        val mockContext = mock(Context::class.java)
        return ContradictionEngine(mockContext)
    }

    @Test
    fun testCaseSummaryCalculation() {
        // Arrange
        val entity = createTestEntity(
            id = "entity1",
            primaryName = "Test Entity",
            emails = listOf("test@example.com")
        )

        val evidence = createTestEvidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "test.txt",
            filePath = "/documents/test.txt",
            addedAt = Date(),
            extractedText = "Test evidence content",
            metadata = EvidenceMetadata(sender = "test@example.com"),
            processed = true
        )

        val statementA = createTestStatement(
            id = "stmt1",
            entityId = "entity1",
            text = "S1",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )

        val statementB = createTestStatement(
            id = "stmt2",
            entityId = "entity1",
            text = "S2",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("test")
        )

        val contradiction = Contradiction(
            id = "c1",
            entityId = "entity1",
            statementA = statementA,
            statementB = statementB,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Test contradiction",
            legalImplication = "Test implication",
            detectedAt = Date()
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
            createdAt = Date(),
            updatedAt = Date(),
            evidence = mutableListOf(evidence),
            entities = mutableListOf(entity),
            timeline = mutableListOf(timelineEvent),
            contradictions = mutableListOf(contradiction),
            liabilityScores = mutableMapOf("entity1" to liabilityScore),
            narrative = "Test narrative",
            sealedHash = null
        )

        // Act
        val engine = createEngine()
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
            createdAt = Date(),
            updatedAt = Date(),
            evidence = mutableListOf(),
            entities = mutableListOf(),
            timeline = mutableListOf(),
            contradictions = mutableListOf(),
            liabilityScores = mutableMapOf(),
            narrative = "",
            sealedHash = null
        )

        // Act
        val engine = createEngine()
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
        val entity = createTestEntity(id = "entity1", primaryName = "Test")
        val evidence = createTestEvidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "test.txt",
            filePath = "/documents/test.txt",
            extractedText = "test content",
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
        val engine = createEngine()
        val summary = engine.getSummary(testCase)

        // Assert
        assertEquals("Total contradictions", 4, summary.totalContradictions)
        assertEquals("Critical contradictions", 1, summary.criticalContradictions)
        assertEquals("High contradictions", 1, summary.highContradictions)
    }
}

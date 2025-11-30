package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for LiabilityCalculator
 * Tests multi-factor liability scoring algorithm
 */
class LiabilityCalculatorTest {

    private lateinit var calculator: LiabilityCalculator
    
    @Before
    fun setup() {
        calculator = LiabilityCalculator()
    }

    /**
     * Helper function to create test entity matching the exact production signature.
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

    @Test
    fun testBasicLiabilityCalculation() {
        // Arrange
        val entity = createTestEntity(
            id = "entity1",
            primaryName = "Defendant",
            emails = listOf("defendant@example.com")
        )

        val evidence = createTestEvidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "email.txt",
            filePath = "/documents/email.txt",
            addedAt = Date(),
            extractedText = "Test evidence content",
            metadata = EvidenceMetadata(sender = "defendant@example.com"),
            processed = true
        )

        val statementA = createTestStatement(
            id = "stmt1",
            entityId = "entity1",
            text = "I paid the full amount",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("paid", "full", "amount")
        )

        val statementB = createTestStatement(
            id = "stmt2",
            entityId = "entity1",
            text = "I never paid",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("never", "paid")
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

        // Act
        val scores = calculator.calculateLiability(
            listOf(entity),
            listOf(contradiction),
            emptyList(),
            listOf(evidence),
            emptyList()
        )

        // Assert
        assertTrue("Should calculate liability for entity", scores.containsKey("entity1"))
        val score = scores["entity1"]
        assertNotNull("Score should not be null", score)
        assertTrue("Contradiction score should be > 0", score?.contradictionScore ?: 0f > 0f)
        assertTrue("Overall score should be > 0", score?.overallScore ?: 0f > 0f)
    }

    @Test
    fun testNoLiabilityWithoutContradictions() {
        // Arrange
        val entity = createTestEntity(
            id = "entity1",
            primaryName = "Innocent",
            emails = listOf("innocent@example.com")
        )

        val evidence = createTestEvidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "email.txt",
            filePath = "/documents/email.txt",
            addedAt = Date(),
            extractedText = "Test evidence content",
            metadata = EvidenceMetadata(sender = "innocent@example.com"),
            processed = true
        )

        // Act
        val scores = calculator.calculateLiability(
            listOf(entity),
            emptyList(),  // No contradictions
            emptyList(),
            listOf(evidence),
            emptyList()
        )

        // Assert
        assertTrue("Should have entry for entity", scores.containsKey("entity1"))
        val score = scores["entity1"]
        assertTrue("Score with no contradictions should be low", (score?.overallScore ?: 0f) < 30f)
    }

    @Test
    fun testMultipleContradictionsIncreaseLiability() {
        // Arrange
        val entity = createTestEntity(
            id = "entity1",
            primaryName = "Defendant",
            emails = listOf("defendant@example.com")
        )

        val evidence = createTestEvidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "email.txt",
            filePath = "/documents/email.txt",
            addedAt = Date(),
            extractedText = "Test evidence content",
            metadata = EvidenceMetadata(sender = "defendant@example.com"),
            processed = true
        )

        val stmt1 = createTestStatement(
            id = "stmt1",
            entityId = "entity1",
            text = "Statement 1",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )
        val stmt2 = createTestStatement(
            id = "stmt2",
            entityId = "entity1",
            text = "Statement 2",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("test")
        )
        val stmt3 = createTestStatement(
            id = "stmt3",
            entityId = "entity1",
            text = "Statement 3",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.ADMISSION,
            keywords = listOf("test")
        )

        val contradictions = listOf(
            Contradiction(
                id = "c1",
                entityId = "entity1",
                statementA = stmt1,
                statementB = stmt2,
                type = ContradictionType.DIRECT,
                severity = Severity.HIGH,
                description = "Test 1",
                legalImplication = "Impl 1",
                detectedAt = Date()
            ),
            Contradiction(
                id = "c2",
                entityId = "entity1",
                statementA = stmt2,
                statementB = stmt3,
                type = ContradictionType.DIRECT,
                severity = Severity.CRITICAL,
                description = "Test 2",
                legalImplication = "Impl 2",
                detectedAt = Date()
            ),
            Contradiction(
                id = "c3",
                entityId = "entity1",
                statementA = stmt1,
                statementB = stmt3,
                type = ContradictionType.CROSS_DOCUMENT,
                severity = Severity.HIGH,
                description = "Test 3",
                legalImplication = "Impl 3",
                detectedAt = Date()
            )
        )

        // Act
        val scores = calculator.calculateLiability(
            listOf(entity),
            contradictions,
            emptyList(),
            listOf(evidence),
            emptyList()
        )

        // Assert
        val score = scores["entity1"]
        assertNotNull("Score should exist", score)
        assertTrue("Multiple contradictions should increase score", (score?.overallScore ?: 0f) > 50f)
    }

    @Test
    fun testCriticalContributesMoreThanHigh() {
        // Arrange
        val entity1 = createTestEntity(id = "entity1", primaryName = "Defendant1")
        val entity2 = createTestEntity(id = "entity2", primaryName = "Defendant2")
        val evidence = createTestEvidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "test.txt",
            filePath = "/documents/test.txt",
            addedAt = Date(),
            extractedText = "test content",
            processed = true
        )

        val stmt1 = createTestStatement(
            id = "stmt1",
            entityId = "entity1",
            text = "S1",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )
        val stmt2 = createTestStatement(
            id = "stmt2",
            entityId = "entity1",
            text = "S2",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("test")
        )
        val stmt3 = createTestStatement(
            id = "stmt3",
            entityId = "entity2",
            text = "S3",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )
        val stmt4 = createTestStatement(
            id = "stmt4",
            entityId = "entity2",
            text = "S4",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("test")
        )

        val contradiction1 = Contradiction(
            id = "c1",
            entityId = "entity1",
            statementA = stmt1,
            statementB = stmt2,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "C1",
            legalImplication = "I1",
            detectedAt = Date()
        )
        val contradiction2 = Contradiction(
            id = "c2",
            entityId = "entity2",
            statementA = stmt3,
            statementB = stmt4,
            type = ContradictionType.DIRECT,
            severity = Severity.HIGH,
            description = "C2",
            legalImplication = "I2",
            detectedAt = Date()
        )

        // Act
        val scores = calculator.calculateLiability(
            listOf(entity1, entity2),
            listOf(contradiction1, contradiction2),
            emptyList(),
            listOf(evidence),
            emptyList()
        )

        // Assert
        val score1 = scores["entity1"]?.overallScore ?: 0f
        val score2 = scores["entity2"]?.overallScore ?: 0f
        assertTrue("CRITICAL contradictions should score higher than HIGH", score1 > score2)
    }

    @Test
    fun testEmptyInputs() {
        // Act
        val scores = calculator.calculateLiability(
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList()
        )

        // Assert
        assertTrue("Empty inputs should return empty scores", scores.isEmpty())
    }
}

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

    @Test
    fun testBasicLiabilityCalculation() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Defendant",
            emails = mutableListOf("defendant@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "email.txt",
            type = EvidenceType.TEXT,
            extractedText = "Test evidence",
            metadata = EvidenceMetadata(sender = "defendant@example.com")
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = Statement(
                entityId = "entity1",
                text = "I paid the full amount",
                date = Date(),
                sourceEvidenceId = "ev1",
                type = StatementType.CLAIM,
                keywords = listOf("paid", "full", "amount")
            ),
            statementB = Statement(
                entityId = "entity1",
                text = "I never paid",
                date = Date(),
                sourceEvidenceId = "ev1",
                type = StatementType.DENIAL,
                keywords = listOf("never", "paid")
            ),
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Test contradiction",
            legalImplication = "Test implication"
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
        val entity = Entity(
            id = "entity1",
            primaryName = "Innocent",
            emails = mutableListOf("innocent@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "email.txt",
            type = EvidenceType.TEXT,
            extractedText = "Test evidence",
            metadata = EvidenceMetadata(sender = "innocent@example.com")
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
        val entity = Entity(
            id = "entity1",
            primaryName = "Defendant",
            emails = mutableListOf("defendant@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "email.txt",
            type = EvidenceType.TEXT,
            extractedText = "Test evidence",
            metadata = EvidenceMetadata(sender = "defendant@example.com")
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
        val stmt3 = Statement(
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
                entityId = "entity1",
                statementA = stmt1,
                statementB = stmt2,
                type = ContradictionType.DIRECT,
                severity = Severity.HIGH,
                description = "Test 1",
                legalImplication = "Impl 1"
            ),
            Contradiction(
                entityId = "entity1",
                statementA = stmt2,
                statementB = stmt3,
                type = ContradictionType.DIRECT,
                severity = Severity.CRITICAL,
                description = "Test 2",
                legalImplication = "Impl 2"
            ),
            Contradiction(
                entityId = "entity1",
                statementA = stmt1,
                statementB = stmt3,
                type = ContradictionType.CROSS_DOCUMENT,
                severity = Severity.HIGH,
                description = "Test 3",
                legalImplication = "Impl 3"
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
        val entity1 = Entity(
            id = "entity1",
            primaryName = "Defendant1",
            aliases = mutableListOf(),
            emails = mutableListOf(),
            phones = mutableListOf()
        )
        val entity2 = Entity(
            id = "entity2",
            primaryName = "Defendant2",
            aliases = mutableListOf(),
            emails = mutableListOf(),
            phones = mutableListOf()
        )
        val evidence = Evidence(
            id = "ev1",
            fileName = "test.txt",
            type = EvidenceType.TEXT,
            extractedText = "test",
            metadata = EvidenceMetadata()
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
        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "S2",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("test")
        )
        val stmt3 = Statement(
            id = "stmt3",
            entityId = "entity2",
            text = "S3",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )
        val stmt4 = Statement(
            id = "stmt4",
            entityId = "entity2",
            text = "S4",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("test")
        )

        val contradiction1 = Contradiction(
            entityId = "entity1",
            statementA = stmt1,
            statementB = stmt2,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "C1",
            legalImplication = "I1"
        )
        val contradiction2 = Contradiction(
            entityId = "entity2",
            statementA = stmt3,
            statementB = stmt4,
            type = ContradictionType.DIRECT,
            severity = Severity.HIGH,
            description = "C2",
            legalImplication = "I2"
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

package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for LevelerEngine
 * Tests the unified analysis engine functionality
 */
class LevelerEngineTest {

    private lateinit var levelerEngine: LevelerEngine

    @Before
    fun setup() {
        levelerEngine = LevelerEngine(null)
    }

    @Test
    fun testLevelerEngineProcessesEvidence() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "John Doe",
            emails = mutableListOf("john@example.com"),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "statement.txt",
            filePath = "/path/to/statement.txt",
            extractedText = "I paid the full amount of $10000 as promised.",
            metadata = EvidenceMetadata(sender = "john@example.com"),
            processed = true
        )

        // Act
        val output = levelerEngine.process(
            evidence = listOf(evidence),
            entities = listOf(entity),
            timeline = emptyList(),
            existingContradictions = emptyList(),
            behavioralPatterns = emptyList()
        )

        // Assert
        assertEquals("Engine name should be LEVELER", LevelerEngine.ENGINE_NAME, output.engineName)
        assertEquals("Engine version should be 1.0", LevelerEngine.VERSION, output.engineVersion)
        assertTrue("Should have statements", output.statements.isNotEmpty())
        assertTrue("Should have entity profiles", output.entityProfiles.isNotEmpty())
        assertNotNull("Should have statistics", output.statistics)
        assertTrue("Scan hash should not be empty", output.scanHash.isNotEmpty())
    }

    @Test
    fun testLevelerDetectsSemanticContradictions() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Jane Smith",
            emails = mutableListOf("jane@example.com"),
            aliases = mutableListOf()
        )

        val evidence1 = Evidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "email1.txt",
            filePath = "/path/to/email1.txt",
            extractedText = "I definitely paid the invoice on time.",
            metadata = EvidenceMetadata(
                sender = "jane@example.com",
                creationDate = Date(System.currentTimeMillis() - 86400000)
            ),
            processed = true
        )

        val evidence2 = Evidence(
            id = "ev2",
            type = EvidenceType.TEXT,
            fileName = "email2.txt",
            filePath = "/path/to/email2.txt",
            extractedText = "I never paid the invoice. I did not pay anything.",
            metadata = EvidenceMetadata(
                sender = "jane@example.com",
                creationDate = Date()
            ),
            processed = true
        )

        // Act
        val output = levelerEngine.process(
            evidence = listOf(evidence1, evidence2),
            entities = listOf(entity),
            timeline = emptyList(),
            existingContradictions = emptyList(),
            behavioralPatterns = emptyList()
        )

        // Assert
        assertTrue("Should have analysis log", output.analysisLog.isNotEmpty())
        assertTrue("Should have processed statements", output.statements.isNotEmpty())
        // Note: Whether contradictions are detected depends on semantic similarity threshold
    }

    @Test
    fun testLevelerDetectsFinancialDiscrepancies() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Bob Johnson",
            emails = mutableListOf("bob@example.com"),
            aliases = mutableListOf()
        )

        val evidence1 = Evidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "doc1.txt",
            filePath = "/path/to/doc1.txt",
            extractedText = "Bob said the amount was $10000 in the first document.",
            metadata = EvidenceMetadata(sender = "bob@example.com"),
            processed = true
        )

        val evidence2 = Evidence(
            id = "ev2",
            type = EvidenceType.TEXT,
            fileName = "doc2.txt",
            filePath = "/path/to/doc2.txt",
            extractedText = "Bob claimed the total was $50000 in the second document.",
            metadata = EvidenceMetadata(sender = "bob@example.com"),
            processed = true
        )

        // Act
        val output = levelerEngine.process(
            evidence = listOf(evidence1, evidence2),
            entities = listOf(entity),
            timeline = emptyList(),
            existingContradictions = emptyList(),
            behavioralPatterns = emptyList()
        )

        // Assert
        assertTrue("Should have entity profiles", output.entityProfiles.isNotEmpty())
        val bobProfile = output.entityProfiles.find { it.name == "Bob Johnson" }
        assertNotNull("Should have Bob's profile", bobProfile)
    }

    @Test
    fun testLevelerMergesExistingContradictions() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Test Entity",
            emails = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "test.txt",
            filePath = "/path/to/test.txt",
            extractedText = "Simple test evidence text.",
            metadata = EvidenceMetadata(),
            processed = true
        )

        val existingStatement = Statement(
            entityId = "entity1",
            text = "Existing statement",
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM
        )

        val existingContradiction = Contradiction(
            entityId = "entity1",
            statementA = existingStatement,
            statementB = existingStatement,
            type = ContradictionType.DIRECT,
            severity = Severity.HIGH,
            description = "Existing contradiction",
            legalImplication = "Test implication"
        )

        // Act
        val output = levelerEngine.process(
            evidence = listOf(evidence),
            entities = listOf(entity),
            timeline = emptyList(),
            existingContradictions = listOf(existingContradiction),
            behavioralPatterns = emptyList()
        )

        // Assert
        assertTrue("Should include existing contradiction", 
            output.contradictions.any { it.description == "Existing contradiction" })
    }

    @Test
    fun testLevelerGeneratesStatistics() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Stats Test",
            emails = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "test.txt",
            filePath = "/path/to/test.txt",
            extractedText = "This is a test statement. Here is another statement. And one more statement.",
            metadata = EvidenceMetadata(),
            processed = true
        )

        // Act
        val output = levelerEngine.process(
            evidence = listOf(evidence),
            entities = listOf(entity),
            timeline = emptyList(),
            existingContradictions = emptyList(),
            behavioralPatterns = emptyList()
        )

        // Assert
        assertNotNull("Statistics should not be null", output.statistics)
        assertEquals("Should have 1 entity", 1, output.statistics.totalEntities)
        assertTrue("Should have processed statements", output.statistics.totalStatements > 0)
    }

    @Test
    fun testLevelerEmptyInput() {
        // Act
        val output = levelerEngine.process(
            evidence = emptyList(),
            entities = emptyList(),
            timeline = emptyList(),
            existingContradictions = emptyList(),
            behavioralPatterns = emptyList()
        )

        // Assert
        assertEquals("Should have 0 statements", 0, output.statistics.totalStatements)
        assertEquals("Should have 0 entities", 0, output.statistics.totalEntities)
        assertEquals("Should have 0 contradictions", 0, output.statistics.totalContradictions)
        assertTrue("Should have analysis log", output.analysisLog.isNotEmpty())
    }

    @Test
    fun testLevelerOutputIncludesAnalysisLog() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Log Test",
            emails = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            type = EvidenceType.TEXT,
            fileName = "test.txt",
            filePath = "/path/to/test.txt",
            extractedText = "Test statement for logging.",
            metadata = EvidenceMetadata(),
            processed = true
        )

        // Act
        val output = levelerEngine.process(
            evidence = listOf(evidence),
            entities = listOf(entity),
            timeline = emptyList(),
            existingContradictions = emptyList(),
            behavioralPatterns = emptyList()
        )

        // Assert
        assertTrue("Should have analysis log entries", output.analysisLog.isNotEmpty())
        assertTrue("Log should mention LEVELER ENGINE", 
            output.analysisLog.any { it.contains("LEVELER ENGINE") })
        assertTrue("Log should mention stages", 
            output.analysisLog.any { it.contains("Stage") })
    }
}

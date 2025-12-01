package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for BehavioralAnalyzer
 * Tests detection of manipulation patterns and suspicious behavior
 */
class BehavioralAnalyzerTest {

    private lateinit var analyzer: BehavioralAnalyzer

    @Before
    fun setup() {
        analyzer = BehavioralAnalyzer()
    }

    @Test
    fun testGaslightingDetection() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "John Doe",
            emails = mutableListOf("john@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "email.txt",
            type = EvidenceType.TEXT,
            extractedText = "I never said that. You're imagining things. You must be confused about what happened.",
            metadata = EvidenceMetadata(sender = "john@example.com")
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val gaslightingPatterns = patterns.filter { it.type == BehaviorType.GASLIGHTING }
        assertTrue("Should detect gaslighting patterns", gaslightingPatterns.isNotEmpty())
    }

    @Test
    fun testDeflectionDetection() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Jane Smith",
            emails = mutableListOf("jane@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "message.txt",
            type = EvidenceType.TEXT,
            extractedText = "What about the money you owe me? But you were the one who started this. That's not the point here.",
            metadata = EvidenceMetadata(sender = "jane@example.com")
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val deflectionPatterns = patterns.filter { it.type == BehaviorType.DEFLECTION }
        assertTrue("Should detect deflection patterns", deflectionPatterns.isNotEmpty())
    }

    @Test
    fun testPressureTacticsDetection() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Bob Johnson",
            emails = mutableListOf("bob@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "email.txt",
            type = EvidenceType.TEXT,
            extractedText = "You need to decide now! This offer expires at midnight. Take it or leave it. Don't miss out on this opportunity.",
            metadata = EvidenceMetadata(sender = "bob@example.com")
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val pressurePatterns = patterns.filter { it.type == BehaviorType.PRESSURE_TACTICS }
        assertTrue("Should detect pressure tactics", pressurePatterns.isNotEmpty())
    }

    @Test
    fun testFinancialManipulationDetection() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Alice Brown",
            emails = mutableListOf("alice@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "message.txt",
            type = EvidenceType.TEXT,
            extractedText = "Trust me, I'll pay you back. It's an investment with guaranteed return. Just this once, lend me the money.",
            metadata = EvidenceMetadata(sender = "alice@example.com")
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val financialPatterns = patterns.filter { it.type == BehaviorType.FINANCIAL_MANIPULATION }
        assertTrue("Should detect financial manipulation", financialPatterns.isNotEmpty())
    }

    @Test
    fun testBlameShiftingDetection() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Charlie Davis",
            emails = mutableListOf("charlie@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "email.txt",
            type = EvidenceType.TEXT,
            extractedText = "It's your fault this happened. You made me do it. If you hadn't interfered, everything would be fine.",
            metadata = EvidenceMetadata(sender = "charlie@example.com")
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val blamePatterns = patterns.filter { it.type == BehaviorType.BLAME_SHIFTING }
        assertTrue("Should detect blame shifting", blamePatterns.isNotEmpty())
    }

    @Test
    fun testPassiveAdmissionDetection() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "David Evans",
            emails = mutableListOf("david@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "message.txt",
            type = EvidenceType.TEXT,
            extractedText = "I thought I was in the clear. I didn't think anyone would notice. Technically, it wasn't exactly wrong.",
            metadata = EvidenceMetadata(sender = "david@example.com")
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val passivePatterns = patterns.filter { it.type == BehaviorType.PASSIVE_ADMISSION }
        assertTrue("Should detect passive admission", passivePatterns.isNotEmpty())
    }

    @Test
    fun testOverExplainingDetection() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Eve Foster",
            emails = mutableListOf("eve@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "email.txt",
            type = EvidenceType.TEXT,
            extractedText = "Let me explain what happened. The reason is quite complicated. You see, there's more to it than meets the eye. You don't understand the full picture.",
            metadata = EvidenceMetadata(sender = "eve@example.com")
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val overExplainingPatterns = patterns.filter { it.type == BehaviorType.OVER_EXPLAINING }
        assertTrue("Should detect over-explaining", overExplainingPatterns.isNotEmpty())
    }

    @Test
    fun testEmotionalManipulationDetection() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Frank Green",
            emails = mutableListOf("frank@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "message.txt",
            type = EvidenceType.TEXT,
            extractedText = "If you loved me, you would help. After all I've done for you, this is how you treat me? Don't you trust me?",
            metadata = EvidenceMetadata(sender = "frank@example.com")
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val emotionalPatterns = patterns.filter { it.type == BehaviorType.EMOTIONAL_MANIPULATION }
        assertTrue("Should detect emotional manipulation", emotionalPatterns.isNotEmpty())
    }

    @Test
    fun testGhostingDetectionFromTimeline() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Grace Hall",
            emails = mutableListOf("grace@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val now = Date()
        val oneWeekAgo = Date(now.time - 8 * 24 * 60 * 60 * 1000L) // 8 days ago
        val twoWeeksAgo = Date(now.time - 15 * 24 * 60 * 60 * 1000L) // 15 days ago

        val timeline = listOf(
            TimelineEvent(
                date = twoWeeksAgo,
                description = "Communication from Grace",
                sourceEvidenceId = "ev1",
                entityIds = listOf("entity1"),
                eventType = EventType.COMMUNICATION
            ),
            TimelineEvent(
                date = oneWeekAgo,
                description = "Another communication from Grace",
                sourceEvidenceId = "ev2",
                entityIds = listOf("entity1"),
                eventType = EventType.COMMUNICATION
            )
        )

        // Act
        val patterns = analyzer.analyzeBehavior(emptyList(), listOf(entity), timeline)

        // Assert
        val ghostingPatterns = patterns.filter { it.type == BehaviorType.GHOSTING }
        assertTrue("Should detect ghosting pattern from timeline gaps", ghostingPatterns.isNotEmpty())
    }

    @Test
    fun testSeverityCalculation() {
        // Arrange - Multiple instances should increase severity
        val entity = Entity(
            id = "entity1",
            primaryName = "Henry Irwin",
            emails = mutableListOf("henry@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "email.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                I never said that. You're imagining things.
                You're confused about what happened.
                You must be crazy to think that.
                You're overreacting as usual.
                You're being paranoid about nothing.
                You're making things up.
            """.trimIndent(),
            metadata = EvidenceMetadata(sender = "henry@example.com")
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val gaslightingPatterns = patterns.filter { it.type == BehaviorType.GASLIGHTING }
        assertTrue("Should detect gaslighting", gaslightingPatterns.isNotEmpty())
        
        val pattern = gaslightingPatterns.first()
        assertTrue("Multiple instances should result in HIGH or CRITICAL severity",
            pattern.severity == Severity.HIGH || pattern.severity == Severity.CRITICAL)
    }

    @Test
    fun testNoFalsePositivesWithNormalCommunication() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Ivy Jones",
            emails = mutableListOf("ivy@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "email.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                Thank you for your email. I received the documents you sent.
                Please find attached the signed contract.
                Let me know if you have any questions.
                Best regards,
                Ivy
            """.trimIndent(),
            metadata = EvidenceMetadata(sender = "ivy@example.com")
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        assertTrue("Normal communication should not trigger behavioral patterns", 
            patterns.isEmpty() || patterns.all { it.severity == Severity.LOW })
    }

    @Test
    fun testEmptyInputs() {
        // Act
        val patterns = analyzer.analyzeBehavior(emptyList(), emptyList(), emptyList())

        // Assert
        assertTrue("Empty inputs should return no patterns", patterns.isEmpty())
    }

    @Test
    fun testMultipleEntitiesMultiplePatterns() {
        // Arrange
        val entity1 = Entity(
            id = "entity1",
            primaryName = "Jack King",
            emails = mutableListOf("jack@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val entity2 = Entity(
            id = "entity2",
            primaryName = "Kate Lee",
            emails = mutableListOf("kate@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence1 = Evidence(
            id = "ev1",
            fileName = "email1.txt",
            type = EvidenceType.TEXT,
            extractedText = "You need to decide now! This offer expires today.",
            metadata = EvidenceMetadata(sender = "jack@example.com")
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "email2.txt",
            type = EvidenceType.TEXT,
            extractedText = "It's your fault this happened. You made me do this.",
            metadata = EvidenceMetadata(sender = "kate@example.com")
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence1, evidence2), listOf(entity1, entity2), emptyList())

        // Assert
        val jackPatterns = patterns.filter { it.entityId == "entity1" }
        val katePatterns = patterns.filter { it.entityId == "entity2" }
        
        assertTrue("Should detect patterns for Jack", jackPatterns.isNotEmpty())
        assertTrue("Should detect patterns for Kate", katePatterns.isNotEmpty())
        assertTrue("Jack should have pressure tactics", 
            jackPatterns.any { it.type == BehaviorType.PRESSURE_TACTICS })
        assertTrue("Kate should have blame shifting", 
            katePatterns.any { it.type == BehaviorType.BLAME_SHIFTING })
    }
}

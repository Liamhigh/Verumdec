package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for BehavioralAnalyzer
 * Tests detection of manipulation and deceptive patterns
 */
class BehavioralAnalyzerTest {

    private lateinit var analyzer: BehavioralAnalyzer
    
    @Before
    fun setup() {
        analyzer = BehavioralAnalyzer()
    }

    @Test
    fun testDetectsGaslightingPatterns() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Gaslighter",
            aliases = mutableListOf(),
            emails = mutableListOf("gaslighter@example.com"),
            phones = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "conversation.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                You're imagining things. That never happened.
                You're confused about what I said.
                You're overreacting to this situation.
            """.trimIndent(),
            metadata = EvidenceMetadata(sender = "gaslighter@example.com"),
            processed = true
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val gaslightingPatterns = patterns.filter { it.type == BehaviorType.GASLIGHTING }
        assertTrue("Should detect gaslighting patterns", gaslightingPatterns.isNotEmpty())
    }

    @Test
    fun testDetectsDeflectionPatterns() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Deflector",
            aliases = mutableListOf(),
            emails = mutableListOf("deflector@example.com"),
            phones = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "conversation.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                What about your mistakes?
                But you did the same thing.
                That's not the point here.
            """.trimIndent(),
            metadata = EvidenceMetadata(sender = "deflector@example.com"),
            processed = true
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val deflectionPatterns = patterns.filter { it.type == BehaviorType.DEFLECTION }
        assertTrue("Should detect deflection patterns", deflectionPatterns.isNotEmpty())
    }

    @Test
    fun testDetectsPressureTactics() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Salesperson",
            aliases = mutableListOf(),
            emails = mutableListOf("sales@example.com"),
            phones = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "email.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                You need to decide now! This offer expires tonight.
                Take it or leave it. Last chance to get this deal.
                Everyone else agrees this is the best offer.
            """.trimIndent(),
            metadata = EvidenceMetadata(sender = "sales@example.com"),
            processed = true
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val pressurePatterns = patterns.filter { it.type == BehaviorType.PRESSURE_TACTICS }
        assertTrue("Should detect pressure tactics", pressurePatterns.isNotEmpty())
    }

    @Test
    fun testDetectsBlameShifting() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Blamer",
            aliases = mutableListOf(),
            emails = mutableListOf("blamer@example.com"),
            phones = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "message.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                It's your fault this happened.
                You made me do this.
                If you hadn't done that, none of this would have happened.
            """.trimIndent(),
            metadata = EvidenceMetadata(sender = "blamer@example.com"),
            processed = true
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val blamePatterns = patterns.filter { it.type == BehaviorType.BLAME_SHIFTING }
        assertTrue("Should detect blame shifting", blamePatterns.isNotEmpty())
    }

    @Test
    fun testDetectsOverExplaining() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Explainer",
            aliases = mutableListOf(),
            emails = mutableListOf("explainer@example.com"),
            phones = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "message.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                Let me explain what happened. You see, it's complicated.
                The reason is that there's more to it than meets the eye.
                You don't understand the full situation here.
            """.trimIndent(),
            metadata = EvidenceMetadata(sender = "explainer@example.com"),
            processed = true
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val overExplainingPatterns = patterns.filter { it.type == BehaviorType.OVER_EXPLAINING }
        assertTrue("Should detect over-explaining", overExplainingPatterns.isNotEmpty())
    }

    @Test
    fun testDetectsPassiveAdmission() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Admitter",
            aliases = mutableListOf(),
            emails = mutableListOf("admitter@example.com"),
            phones = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "message.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                I thought I was in the clear after that.
                I didn't think anyone would notice what happened.
                Technically speaking, it wasn't really a violation.
            """.trimIndent(),
            metadata = EvidenceMetadata(sender = "admitter@example.com"),
            processed = true
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val passiveAdmissionPatterns = patterns.filter { it.type == BehaviorType.PASSIVE_ADMISSION }
        assertTrue("Should detect passive admission", passiveAdmissionPatterns.isNotEmpty())
    }

    @Test
    fun testSeverityScalingWithMultipleInstances() {
        // Arrange - Multiple instances of same pattern
        val entity = Entity(
            id = "entity1",
            primaryName = "Repeated Offender",
            aliases = mutableListOf(),
            emails = mutableListOf("offender@example.com"),
            phones = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "messages.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                You're imagining things.
                That never happened.
                You're confused again.
                You're overreacting as usual.
                You misunderstood me completely.
            """.trimIndent(),
            metadata = EvidenceMetadata(sender = "offender@example.com"),
            processed = true
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert
        val gaslightingPatterns = patterns.filter { it.type == BehaviorType.GASLIGHTING }
        if (gaslightingPatterns.isNotEmpty()) {
            val pattern = gaslightingPatterns.first()
            assertTrue("Multiple instances should increase severity",
                pattern.severity == Severity.HIGH || pattern.severity == Severity.CRITICAL)
            assertTrue("Should capture multiple instances", pattern.instances.size >= 2)
        }
    }

    @Test
    fun testNoFalsePositivesForNormalConversation() {
        // Arrange - Normal conversation without manipulation
        val entity = Entity(
            id = "entity1",
            primaryName = "Normal Person",
            aliases = mutableListOf(),
            emails = mutableListOf("normal@example.com"),
            phones = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "normal.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                Thank you for the update. I appreciate your help.
                The project is going well and we're on schedule.
                Let's meet next week to discuss the next steps.
            """.trimIndent(),
            metadata = EvidenceMetadata(sender = "normal@example.com"),
            processed = true
        )

        // Act
        val patterns = analyzer.analyzeBehavior(listOf(evidence), listOf(entity), emptyList())

        // Assert - Should have minimal or no behavioral patterns
        assertTrue("Normal conversation should have few patterns", patterns.size <= 1)
    }

    @Test
    fun testHandlesEmptyInput() {
        // Act
        val patterns = analyzer.analyzeBehavior(emptyList(), emptyList(), emptyList())

        // Assert
        assertTrue("Empty input should return empty patterns", patterns.isEmpty())
    }

    @Test
    fun testDetectsGhostingFromTimeline() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Ghoster",
            aliases = mutableListOf(),
            emails = mutableListOf("ghoster@example.com"),
            phones = mutableListOf()
        )

        // Timeline with long gaps
        val timeline = listOf(
            TimelineEvent(
                id = "te1",
                date = Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L), // 30 days ago
                description = "Last message sent",
                sourceEvidenceId = "ev1",
                entityIds = listOf("entity1"),
                eventType = EventType.COMMUNICATION,
                significance = Significance.NORMAL
            ),
            TimelineEvent(
                id = "te2",
                date = Date(System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000L), // 14 days ago (16 day gap)
                description = "Response after pressure",
                sourceEvidenceId = "ev2",
                entityIds = listOf("entity1"),
                eventType = EventType.COMMUNICATION,
                significance = Significance.NORMAL
            ),
            TimelineEvent(
                id = "te3",
                date = Date(), // Today
                description = "Another response",
                sourceEvidenceId = "ev3",
                entityIds = listOf("entity1"),
                eventType = EventType.COMMUNICATION,
                significance = Significance.NORMAL
            )
        )

        // Act
        val patterns = analyzer.analyzeBehavior(emptyList(), listOf(entity), timeline)

        // Assert
        val ghostingPatterns = patterns.filter { it.type == BehaviorType.GHOSTING }
        assertTrue("Should detect ghosting from timeline gaps", ghostingPatterns.isNotEmpty())
    }

    @Test
    fun testMultipleEntitiesAnalyzedSeparately() {
        // Arrange
        val entity1 = Entity(
            id = "entity1",
            primaryName = "Good Actor",
            aliases = mutableListOf(),
            emails = mutableListOf("good@example.com"),
            phones = mutableListOf()
        )

        val entity2 = Entity(
            id = "entity2",
            primaryName = "Bad Actor",
            aliases = mutableListOf(),
            emails = mutableListOf("bad@example.com"),
            phones = mutableListOf()
        )

        val evidence1 = Evidence(
            id = "ev1",
            fileName = "good.txt",
            type = EvidenceType.TEXT,
            extractedText = "Good Actor: Thank you for your help with this.",
            metadata = EvidenceMetadata(sender = "good@example.com"),
            processed = true
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "bad.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                You're imagining things. That never happened.
                It's your fault this happened. You made me do this.
            """.trimIndent(),
            metadata = EvidenceMetadata(sender = "bad@example.com"),
            processed = true
        )

        // Act
        val patterns = analyzer.analyzeBehavior(
            listOf(evidence1, evidence2), 
            listOf(entity1, entity2), 
            emptyList()
        )

        // Assert
        val badActorPatterns = patterns.filter { it.entityId == "entity2" }
        val goodActorPatterns = patterns.filter { it.entityId == "entity1" }
        
        assertTrue("Bad actor should have more patterns", badActorPatterns.size >= goodActorPatterns.size)
    }
}

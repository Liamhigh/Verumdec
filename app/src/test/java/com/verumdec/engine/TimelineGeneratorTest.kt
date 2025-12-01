package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for TimelineGenerator
 * Tests timeline generation from various evidence types
 */
class TimelineGeneratorTest {

    private lateinit var generator: TimelineGenerator

    @Before
    fun setup() {
        generator = TimelineGenerator()
    }

    @Test
    fun testWhatsAppMessageParsing() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "John",
            emails = mutableListOf(),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "whatsapp_chat.txt",
            type = EvidenceType.WHATSAPP,
            extractedText = """
                [12/5/23, 10:30 AM] John: I will pay you tomorrow
                [12/5/23, 10:35 AM] Jane: Ok, please do
                [12/6/23, 11:00 AM] John: Sorry, I forgot. Will send it now
            """.trimIndent(),
            metadata = EvidenceMetadata()
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence), listOf(entity))

        // Assert
        assertTrue("Should parse WhatsApp messages", timeline.isNotEmpty())
        assertTrue("Should find messages from John", timeline.any { it.description.contains("John") })
    }

    @Test
    fun testEmailEventCreation() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Sender Name",
            emails = mutableListOf("sender@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val creationDate = Date()
        val evidence = Evidence(
            id = "ev1",
            fileName = "email.eml",
            type = EvidenceType.EMAIL,
            extractedText = "This is the email body content about the payment.",
            metadata = EvidenceMetadata(
                sender = "sender@example.com",
                subject = "Payment Confirmation",
                creationDate = creationDate
            )
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence), listOf(entity))

        // Assert
        assertTrue("Should create email event", timeline.isNotEmpty())
        val emailEvent = timeline.first()
        assertEquals("Email event should be COMMUNICATION type", EventType.COMMUNICATION, emailEvent.eventType)
    }

    @Test
    fun testPaymentEventClassification() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Jane",
            emails = mutableListOf("jane@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "whatsapp_chat.txt",
            type = EvidenceType.WHATSAPP,
            extractedText = "[12/5/23, 10:30 AM] Jane: I paid you R5000 yesterday",
            metadata = EvidenceMetadata()
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence), listOf(entity))

        // Assert
        assertTrue("Should classify payment message", timeline.isNotEmpty())
        val paymentEvent = timeline.find { it.eventType == EventType.PAYMENT }
        assertNotNull("Should detect payment event type", paymentEvent)
    }

    @Test
    fun testPromiseEventClassification() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Bob",
            emails = mutableListOf("bob@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "whatsapp_chat.txt",
            type = EvidenceType.WHATSAPP,
            extractedText = "[12/5/23, 10:30 AM] Bob: I will pay you by monday",
            metadata = EvidenceMetadata()
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence), listOf(entity))

        // Assert
        assertTrue("Should classify promise message", timeline.isNotEmpty())
        val promiseEvent = timeline.find { it.eventType == EventType.PROMISE }
        assertNotNull("Should detect promise event type", promiseEvent)
    }

    @Test
    fun testDenialEventClassification() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Charlie",
            emails = mutableListOf("charlie@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "whatsapp_chat.txt",
            type = EvidenceType.WHATSAPP,
            extractedText = "[12/5/23, 10:30 AM] Charlie: I never agreed to that deal. It's not true.",
            metadata = EvidenceMetadata()
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence), listOf(entity))

        // Assert
        assertTrue("Should classify denial message", timeline.isNotEmpty())
        val denialEvent = timeline.find { it.eventType == EventType.DENIAL }
        assertNotNull("Should detect denial event type", denialEvent)
    }

    @Test
    fun testAdmissionEventClassification() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Diana",
            emails = mutableListOf("diana@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "whatsapp_chat.txt",
            type = EvidenceType.WHATSAPP,
            extractedText = "[12/5/23, 10:30 AM] Diana: Yes I admit I made a mistake. I did take the money.",
            metadata = EvidenceMetadata()
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence), listOf(entity))

        // Assert
        assertTrue("Should classify admission message", timeline.isNotEmpty())
        val admissionEvent = timeline.find { it.eventType == EventType.ADMISSION }
        assertNotNull("Should detect admission event type", admissionEvent)
    }

    @Test
    fun testEventSignificance() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Eve",
            emails = mutableListOf("eve@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "whatsapp_chat.txt",
            type = EvidenceType.WHATSAPP,
            extractedText = """
                [12/5/23, 10:30 AM] Eve: Hello there
                [12/5/23, 10:35 AM] Eve: I paid you $5000 yesterday
                [12/5/23, 10:40 AM] Eve: I never signed that contract
            """.trimIndent(),
            metadata = EvidenceMetadata()
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence), listOf(entity))

        // Assert
        val paymentEvent = timeline.find { it.eventType == EventType.PAYMENT }
        val denialEvent = timeline.find { it.eventType == EventType.DENIAL }
        
        assertNotNull("Should find payment event", paymentEvent)
        assertNotNull("Should find denial event", denialEvent)
        assertEquals("Payment events should be HIGH significance", Significance.HIGH, paymentEvent?.significance)
        assertEquals("Denial events should be HIGH significance", Significance.HIGH, denialEvent?.significance)
    }

    @Test
    fun testTimelineOrdering() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Frank",
            emails = mutableListOf(),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "whatsapp_chat.txt",
            type = EvidenceType.WHATSAPP,
            extractedText = """
                [12/7/23, 10:30 AM] Frank: Third message
                [12/5/23, 10:30 AM] Frank: First message  
                [12/6/23, 10:30 AM] Frank: Second message
            """.trimIndent(),
            metadata = EvidenceMetadata()
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence), listOf(entity))

        // Assert
        assertTrue("Should have at least 3 events", timeline.size >= 3)
        for (i in 0 until timeline.size - 1) {
            assertTrue("Events should be sorted chronologically", 
                timeline[i].date <= timeline[i + 1].date)
        }
    }

    @Test
    fun testGenericDocumentEventCreation() {
        // Arrange
        val creationDate = Date()
        val evidence = Evidence(
            id = "ev1",
            fileName = "contract.pdf",
            type = EvidenceType.PDF,
            extractedText = "This contract was signed on 15/03/2023. The total amount is R50000.",
            metadata = EvidenceMetadata(creationDate = creationDate)
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence), emptyList())

        // Assert
        assertTrue("Should create document event", timeline.isNotEmpty())
        val docEvent = timeline.find { it.eventType == EventType.DOCUMENT }
        assertNotNull("Should find document event", docEvent)
    }

    @Test
    fun testEntityLinking() {
        // Arrange
        val entity1 = Entity(
            id = "entity1",
            primaryName = "George",
            emails = mutableListOf(),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val entity2 = Entity(
            id = "entity2",
            primaryName = "Helen",
            emails = mutableListOf(),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "whatsapp_chat.txt",
            type = EvidenceType.WHATSAPP,
            extractedText = """
                [12/5/23, 10:30 AM] George: Hello Helen
                [12/5/23, 10:35 AM] Helen: Hi George
            """.trimIndent(),
            metadata = EvidenceMetadata()
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence), listOf(entity1, entity2))

        // Assert
        val georgeEvents = timeline.filter { "entity1" in it.entityIds }
        val helenEvents = timeline.filter { "entity2" in it.entityIds }
        
        assertTrue("Should link events to George", georgeEvents.isNotEmpty())
        assertTrue("Should link events to Helen", helenEvents.isNotEmpty())
    }

    @Test
    fun testEmptyEvidence() {
        // Act
        val timeline = generator.generateTimeline(emptyList(), emptyList())

        // Assert
        assertTrue("Empty evidence should return empty timeline", timeline.isEmpty())
    }

    @Test
    fun testEmptyExtractedText() {
        // Arrange
        val evidence = Evidence(
            id = "ev1",
            fileName = "empty.txt",
            type = EvidenceType.TEXT,
            extractedText = "",
            metadata = EvidenceMetadata()
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence), emptyList())

        // Assert
        assertTrue("Empty extracted text should return empty timeline", timeline.isEmpty())
    }

    @Test
    fun testDateExtractionFromText() {
        // Arrange
        val evidence = Evidence(
            id = "ev1",
            fileName = "document.txt",
            type = EvidenceType.TEXT,
            extractedText = "On 25/03/2023, the payment of R10000 was made. The contract was signed on 2023-01-15.",
            metadata = EvidenceMetadata()
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence), emptyList())

        // Assert
        assertTrue("Should extract dated events from text", timeline.isNotEmpty())
    }

    @Test
    fun testMultipleEvidenceSources() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Ivan",
            emails = mutableListOf("ivan@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence1 = Evidence(
            id = "ev1",
            fileName = "whatsapp.txt",
            type = EvidenceType.WHATSAPP,
            extractedText = "[12/5/23, 10:30 AM] Ivan: I will pay tomorrow",
            metadata = EvidenceMetadata()
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "email.eml",
            type = EvidenceType.EMAIL,
            extractedText = "Please find attached the signed contract.",
            metadata = EvidenceMetadata(
                sender = "ivan@example.com",
                subject = "Contract",
                creationDate = Date()
            )
        )

        // Act
        val timeline = generator.generateTimeline(listOf(evidence1, evidence2), listOf(entity))

        // Assert
        assertTrue("Should include events from multiple sources", timeline.size >= 2)
        
        val whatsappEvents = timeline.filter { it.sourceEvidenceId == "ev1" }
        val emailEvents = timeline.filter { it.sourceEvidenceId == "ev2" }
        
        assertTrue("Should have events from WhatsApp", whatsappEvents.isNotEmpty())
        assertTrue("Should have events from email", emailEvents.isNotEmpty())
    }
}

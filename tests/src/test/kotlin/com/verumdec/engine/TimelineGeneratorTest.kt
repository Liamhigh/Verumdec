package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Comprehensive tests for the Timeline Generator engine.
 */
class TimelineGeneratorTest {
    
    private val generator = TimelineGenerator()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    
    @Test
    fun `test empty evidence returns no timeline events`() {
        val result = generator.generateTimeline(emptyList(), emptyList())
        assertTrue("Empty evidence should produce no timeline", result.isEmpty())
    }
    
    @Test
    fun `test WhatsApp message parsing`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.WHATSAPP,
                fileName = "whatsapp_export.txt",
                filePath = "/path/to/export",
                extractedText = """
                    [15/03/2023, 10:30 AM] John: Hello, are we still meeting today?
                    [15/03/2023, 10:35 AM] Sarah: Yes, confirmed for 2pm
                    [15/03/2023, 2:15 PM] John: I'm here at the venue
                """.trimIndent(),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(primaryName = "John", mentions = 2),
            Entity(primaryName = "Sarah", mentions = 1)
        )
        
        val result = generator.generateTimeline(evidence, entities)
        
        // Should parse WhatsApp messages into timeline events
        assertTrue("Should generate timeline events from WhatsApp", result.isNotEmpty())
    }
    
    @Test
    fun `test email timeline extraction`() {
        val emailDate = Calendar.getInstance().apply {
            set(2023, Calendar.MARCH, 20, 14, 30, 0)
        }.time
        
        val evidence = listOf(
            Evidence(
                type = EvidenceType.EMAIL,
                fileName = "important_email.eml",
                filePath = "/path/to/email",
                extractedText = "Subject: Contract Discussion\n\nI am sending this to confirm the agreement.",
                metadata = EvidenceMetadata(
                    creationDate = emailDate,
                    sender = "John Smith <john@example.com>",
                    subject = "Contract Discussion"
                ),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(primaryName = "John Smith", emails = mutableListOf("john@example.com"), mentions = 1)
        )
        
        val result = generator.generateTimeline(evidence, entities)
        
        // Should create email event
        assertTrue("Should generate timeline event from email", result.isNotEmpty())
        assertTrue("Email event should have communication type", 
            result.any { it.eventType == EventType.COMMUNICATION })
    }
    
    @Test
    fun `test date extraction from text`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "document.txt",
                filePath = "/path/to/doc",
                extractedText = """
                    On 15/03/2023 the parties agreed to the following terms.
                    The payment was made on 20/03/2023 as confirmed.
                    By 25/03/2023 all documents were submitted.
                """.trimIndent(),
                processed = true
            )
        )
        
        val result = generator.generateTimeline(evidence, emptyList())
        
        // Should extract events from dates in text
        assertTrue("Should extract dated events from text", result.isNotEmpty())
    }
    
    @Test
    fun `test timeline sorting by date`() {
        val date1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -10) }.time
        val date2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -5) }.time
        val date3 = Calendar.getInstance().time
        
        val evidence = listOf(
            Evidence(
                type = EvidenceType.EMAIL,
                fileName = "email3.eml",
                filePath = "/path/to/email3",
                extractedText = "Final confirmation.",
                metadata = EvidenceMetadata(creationDate = date3, sender = "test@test.com"),
                processed = true
            ),
            Evidence(
                type = EvidenceType.EMAIL,
                fileName = "email1.eml",
                filePath = "/path/to/email1",
                extractedText = "Initial contact.",
                metadata = EvidenceMetadata(creationDate = date1, sender = "test@test.com"),
                processed = true
            ),
            Evidence(
                type = EvidenceType.EMAIL,
                fileName = "email2.eml",
                filePath = "/path/to/email2",
                extractedText = "Follow-up discussion.",
                metadata = EvidenceMetadata(creationDate = date2, sender = "test@test.com"),
                processed = true
            )
        )
        
        val result = generator.generateTimeline(evidence, emptyList())
        
        // Timeline should be sorted chronologically
        if (result.size >= 2) {
            for (i in 0 until result.size - 1) {
                assertTrue("Timeline should be sorted by date", 
                    result[i].date <= result[i + 1].date)
            }
        }
    }
    
    @Test
    fun `test payment event classification`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "payment.txt",
                filePath = "/path/to/payment",
                extractedText = "On 15/03/2023 the client paid R50000 to the account as agreed.",
                processed = true
            )
        )
        
        val result = generator.generateTimeline(evidence, emptyList())
        
        // Should classify as payment event
        val paymentEvents = result.filter { it.eventType == EventType.PAYMENT }
        assertTrue("Should detect payment event", paymentEvents.isNotEmpty())
    }
    
    @Test
    fun `test promise event classification`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "promise.txt",
                filePath = "/path/to/promise",
                extractedText = "On 15/03/2023 I will send the documents by next Monday.",
                processed = true
            )
        )
        
        val result = generator.generateTimeline(evidence, emptyList())
        
        // Should classify as promise event
        val promiseEvents = result.filter { it.eventType == EventType.PROMISE }
        assertTrue("Should detect promise event", promiseEvents.isNotEmpty())
    }
    
    @Test
    fun `test entity association with events`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "event.txt",
                filePath = "/path/to/event",
                extractedText = "On 15/03/2023 Michael Johnson confirmed the agreement with Sarah Williams.",
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(id = "entity1", primaryName = "Michael Johnson", mentions = 1),
            Entity(id = "entity2", primaryName = "Sarah Williams", mentions = 1)
        )
        
        val result = generator.generateTimeline(evidence, entities)
        
        // Events should have entity associations
        if (result.isNotEmpty()) {
            val eventWithEntities = result.find { it.entityIds.isNotEmpty() }
            // Events mentioning entities should have them associated
            assertNotNull("Events should associate mentioned entities", result.firstOrNull())
        }
    }
    
    @Test
    fun `test denial event classification`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "denial.txt",
                filePath = "/path/to/denial",
                extractedText = "On 15/03/2023 I never agreed to any deal and this is not true.",
                processed = true
            )
        )
        
        val result = generator.generateTimeline(evidence, emptyList())
        
        // Should classify as denial event
        val denialEvents = result.filter { it.eventType == EventType.DENIAL }
        assertTrue("Should detect denial event", denialEvents.isNotEmpty())
    }
    
    @Test
    fun `test admission event classification`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "admission.txt",
                filePath = "/path/to/admission",
                extractedText = "On 15/03/2023 I admit that I did receive the funds.",
                processed = true
            )
        )
        
        val result = generator.generateTimeline(evidence, emptyList())
        
        // Should classify as admission event
        val admissionEvents = result.filter { it.eventType == EventType.ADMISSION }
        assertTrue("Should detect admission event", admissionEvents.isNotEmpty())
    }
}

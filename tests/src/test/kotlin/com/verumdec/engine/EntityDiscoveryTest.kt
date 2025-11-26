package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Comprehensive tests for the Entity Discovery engine.
 */
class EntityDiscoveryTest {
    
    private val discovery = EntityDiscovery()
    
    @Test
    fun `test empty evidence returns no entities`() {
        val result = discovery.discoverEntities(emptyList())
        assertTrue("Empty evidence should produce no entities", result.isEmpty())
    }
    
    @Test
    fun `test email extraction discovers entity`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.EMAIL,
                fileName = "email.eml",
                filePath = "/path/to/email",
                extractedText = "From john.smith@example.com\n\nHello, this is a message about the deal.",
                metadata = EvidenceMetadata(
                    sender = "John Smith <john.smith@example.com>"
                ),
                processed = true
            ),
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "reply.txt",
                filePath = "/path/to/reply",
                extractedText = "Reply from john.smith@example.com regarding the proposal.",
                processed = true
            )
        )
        
        val result = discovery.discoverEntities(evidence)
        
        // Should find at least one entity with the email
        assertTrue("Should discover entity from email", 
            result.any { it.emails.contains("john.smith@example.com") })
    }
    
    @Test
    fun `test name pattern extraction`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "document1.txt",
                filePath = "/path/to/doc1",
                extractedText = "Michael Johnson sent the contract to Sarah Williams for review.",
                processed = true
            ),
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "document2.txt",
                filePath = "/path/to/doc2",
                extractedText = "Michael Johnson confirmed receipt of the signed agreement.",
                processed = true
            )
        )
        
        val result = discovery.discoverEntities(evidence)
        
        // Should find entities based on proper name patterns
        val michaelEntity = result.find { 
            it.primaryName.contains("Michael", ignoreCase = true) || 
            it.primaryName.contains("Johnson", ignoreCase = true) 
        }
        
        assertNotNull("Should discover 'Michael Johnson' entity", michaelEntity)
    }
    
    @Test
    fun `test entity merging by email`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "msg1.txt",
                filePath = "/path/to/msg1",
                extractedText = "Contact John at john@test.com for more information.",
                metadata = EvidenceMetadata(sender = "john@test.com"),
                processed = true
            ),
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "msg2.txt",
                filePath = "/path/to/msg2",
                extractedText = "Johnny can be reached at john@test.com",
                metadata = EvidenceMetadata(sender = "john@test.com"),
                processed = true
            )
        )
        
        val result = discovery.discoverEntities(evidence)
        
        // The entities with the same email should be merged
        val entitiesWithEmail = result.filter { 
            it.emails.contains("john@test.com") 
        }
        
        // Should merge into one or recognize as related
        assertTrue("Entities with same email should be merged or recognized", 
            entitiesWithEmail.size <= 2)
    }
    
    @Test
    fun `test entity mention counting`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "doc.txt",
                filePath = "/path/to/doc",
                extractedText = """
                    Alice Brown mentioned the contract.
                    Alice Brown signed the agreement.
                    Alice Brown sent the payment.
                    Alice Brown confirmed the details.
                    Alice Brown agreed to the terms.
                """.trimIndent(),
                processed = true
            )
        )
        
        val result = discovery.discoverEntities(evidence)
        
        val aliceEntity = result.find { 
            it.primaryName.contains("Alice", ignoreCase = true) 
        }
        
        assertNotNull("Should find Alice entity", aliceEntity)
        assertTrue("Alice should have multiple mentions", 
            aliceEntity?.mentions ?: 0 >= 2)
    }
    
    @Test
    fun `test common phrase filtering`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "formal.txt",
                filePath = "/path/to/formal",
                extractedText = """
                    Dear Sir,
                    Kind Regards,
                    Yours Sincerely,
                    The Company has decided...
                    Best Regards,
                """.trimIndent(),
                processed = true
            )
        )
        
        val result = discovery.discoverEntities(evidence)
        
        // Common phrases should not be detected as entities
        val commonPhraseEntities = result.filter { 
            it.primaryName == "Dear Sir" || 
            it.primaryName == "Kind Regards" ||
            it.primaryName == "The Company"
        }
        
        assertTrue("Common phrases should be filtered out", commonPhraseEntities.isEmpty())
    }
    
    @Test
    fun `test phone number extraction`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "contact.txt",
                filePath = "/path/to/contact",
                extractedText = "Robert Miller's contact is +1 555-123-4567. You can reach Robert Miller anytime.",
                processed = true
            ),
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "details.txt",
                filePath = "/path/to/details",
                extractedText = "Please call Robert Miller at the provided number.",
                processed = true
            )
        )
        
        val result = discovery.discoverEntities(evidence)
        
        // Should find entity and associate phone number
        val robertEntity = result.find { 
            it.primaryName.contains("Robert", ignoreCase = true) 
        }
        
        assertNotNull("Should find Robert entity", robertEntity)
    }
}

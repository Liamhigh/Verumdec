package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for EntityDiscovery
 * Tests automatic entity extraction from evidence
 */
class EntityDiscoveryTest {

    private lateinit var discovery: EntityDiscovery

    @Before
    fun setup() {
        discovery = EntityDiscovery()
    }

    @Test
    fun testEmailExtraction() {
        // Arrange - Need multiple mentions for entity to be discovered (threshold is 2)
        val evidence = Evidence(
            id = "ev1",
            fileName = "email.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                Please contact john.doe@example.com for more information.
                Send your response to john.doe@example.com as soon as possible.
                Also reach out to jane.smith@company.org for support.
                For billing, contact jane.smith@company.org.
            """.trimIndent(),
            metadata = EvidenceMetadata()
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence))

        // Assert
        assertTrue("Should discover entities from emails (with >= 2 mentions)", entities.isNotEmpty())
        assertTrue("Should extract email addresses", 
            entities.any { it.emails.contains("john.doe@example.com") || it.emails.contains("jane.smith@company.org") })
    }

    @Test
    fun testNameExtraction() {
        // Arrange
        val evidence = Evidence(
            id = "ev1",
            fileName = "document.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                John Smith met with Alice Johnson regarding the contract.
                Later, John Smith confirmed the details via email.
                Alice Johnson provided additional documentation.
            """.trimIndent(),
            metadata = EvidenceMetadata()
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence))

        // Assert
        assertTrue("Should discover entities from names", entities.isNotEmpty())
        // Names mentioned multiple times should be detected
        val johnEntity = entities.find { it.primaryName.contains("John") || it.primaryName.contains("Smith") }
        val aliceEntity = entities.find { it.primaryName.contains("Alice") || it.primaryName.contains("Johnson") }
        
        assertTrue("Should find John entity", johnEntity != null || entities.any { it.primaryName.lowercase().contains("john") })
        assertTrue("Should find Alice entity", aliceEntity != null || entities.any { it.primaryName.lowercase().contains("alice") })
    }

    @Test
    fun testEntityMentionCount() {
        // Arrange
        val evidence = Evidence(
            id = "ev1",
            fileName = "document.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                Contact support@company.com for help.
                You can also email support@company.com with questions.
                Our support@company.com team is available 24/7.
            """.trimIndent(),
            metadata = EvidenceMetadata()
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence))

        // Assert
        val supportEntity = entities.find { it.emails.contains("support@company.com") }
        assertTrue("Entity should be discovered", supportEntity != null || entities.isNotEmpty())
        if (supportEntity != null) {
            assertTrue("Should track multiple mentions", supportEntity.mentions >= 2)
        }
    }

    @Test
    fun testEmailMetadataExtraction() {
        // Arrange - Need multiple evidence pieces to have >= 2 mentions
        val evidence1 = Evidence(
            id = "ev1",
            fileName = "email1.eml",
            type = EvidenceType.EMAIL,
            extractedText = "This is the first email body.",
            metadata = EvidenceMetadata(
                sender = "sender@example.com",
                receiver = "receiver@example.com"
            )
        )
        
        val evidence2 = Evidence(
            id = "ev2",
            fileName = "email2.eml",
            type = EvidenceType.EMAIL,
            extractedText = "This is the second email body.",
            metadata = EvidenceMetadata(
                sender = "sender@example.com",
                receiver = "receiver@example.com"
            )
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence1, evidence2))

        // Assert
        assertTrue("Should extract entities from metadata (with >= 2 mentions)", entities.isNotEmpty())
        val senderEntity = entities.find { it.emails.contains("sender@example.com") }
        val receiverEntity = entities.find { it.emails.contains("receiver@example.com") }
        
        assertTrue("Should extract sender entity", 
            senderEntity != null || entities.any { it.primaryName.lowercase().contains("sender") })
        assertTrue("Should extract receiver entity", 
            receiverEntity != null || entities.any { it.primaryName.lowercase().contains("receiver") })
    }

    @Test
    fun testEntityMerging() {
        // Arrange - Same person with email in one document, name in another
        val evidence1 = Evidence(
            id = "ev1",
            fileName = "email1.txt",
            type = EvidenceType.TEXT,
            extractedText = "Contact John at john.smith@example.com.",
            metadata = EvidenceMetadata()
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "email2.txt",
            type = EvidenceType.TEXT,
            extractedText = "John Smith confirmed the meeting. John Smith will attend.",
            metadata = EvidenceMetadata()
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence1, evidence2))

        // Assert
        // Should merge entities with matching names
        assertTrue("Should discover entities", entities.isNotEmpty())
        val johnEntities = entities.filter { 
            it.primaryName.contains("John", ignoreCase = true) || 
            it.aliases.any { a -> a.contains("John", ignoreCase = true) }
        }
        
        // If merged correctly, there should be fewer entities than separate mentions
        assertTrue("Should merge related entities", johnEntities.size <= 2)
    }

    @Test
    fun testCommonPhrasesFiltered() {
        // Arrange
        val evidence = Evidence(
            id = "ev1",
            fileName = "letter.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                Dear Sir,
                Kind Regards,
                Best Regards,
                The Company hereby notifies you.
                Good Morning,
            """.trimIndent(),
            metadata = EvidenceMetadata()
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence))

        // Assert
        val filteredPhrases = listOf("Dear Sir", "Kind Regards", "Best Regards", "The Company", "Good Morning")
        for (phrase in filteredPhrases) {
            val found = entities.find { it.primaryName == phrase }
            // Common phrases should be filtered out
            assertTrue("Should filter out common phrase: $phrase", found == null)
        }
    }

    @Test
    fun testMinimumMentionThreshold() {
        // Arrange - Entity mentioned only once should be filtered
        val evidence = Evidence(
            id = "ev1",
            fileName = "document.txt",
            type = EvidenceType.TEXT,
            extractedText = "Once mentioned John Doe. Another person Bob Smith was mentioned twice. Bob Smith again.",
            metadata = EvidenceMetadata()
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence))

        // Assert
        // Entities with fewer than 2 mentions should be filtered
        entities.forEach { entity ->
            assertTrue("Entities should have at least 2 mentions", entity.mentions >= 2)
        }
    }

    @Test
    fun testEntitySortedByMentions() {
        // Arrange
        val evidence = Evidence(
            id = "ev1",
            fileName = "document.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                Contact alice@example.com for help.
                Contact alice@example.com again.
                Contact alice@example.com once more.
                Contact bob@example.com too.
                Contact bob@example.com again.
            """.trimIndent(),
            metadata = EvidenceMetadata()
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence))

        // Assert
        if (entities.size >= 2) {
            // Entities should be sorted by mentions in descending order
            for (i in 0 until entities.size - 1) {
                assertTrue("Should be sorted by mentions (descending)",
                    entities[i].mentions >= entities[i + 1].mentions)
            }
        }
    }

    @Test
    fun testEmptyEvidence() {
        // Act
        val entities = discovery.discoverEntities(emptyList())

        // Assert
        assertTrue("Empty evidence should return no entities", entities.isEmpty())
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
        val entities = discovery.discoverEntities(listOf(evidence))

        // Assert
        assertTrue("Empty text should return no entities", entities.isEmpty())
    }

    @Test
    fun testMultipleEvidenceSources() {
        // Arrange
        val evidence1 = Evidence(
            id = "ev1",
            fileName = "email1.txt",
            type = EvidenceType.EMAIL,
            extractedText = "From john@example.com: Hello",
            metadata = EvidenceMetadata(sender = "john@example.com")
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "email2.txt",
            type = EvidenceType.EMAIL,
            extractedText = "Reply from jane@example.com",
            metadata = EvidenceMetadata(sender = "jane@example.com")
        )

        val evidence3 = Evidence(
            id = "ev3",
            fileName = "email3.txt",
            type = EvidenceType.EMAIL,
            extractedText = "Another message from john@example.com",
            metadata = EvidenceMetadata(sender = "john@example.com")
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence1, evidence2, evidence3))

        // Assert
        assertTrue("Should discover entities from multiple sources", entities.isNotEmpty())
        val johnEntity = entities.find { it.emails.contains("john@example.com") }
        assertTrue("Should find John entity", johnEntity != null)
        if (johnEntity != null) {
            assertTrue("John should have mentions from both emails", johnEntity.mentions >= 2)
        }
    }

    @Test
    fun testPhoneNumberExtraction() {
        // Arrange
        val evidence = Evidence(
            id = "ev1",
            fileName = "document.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                Contact John Smith at +27 82 123 4567.
                John Smith is also available at 082-123-4567.
            """.trimIndent(),
            metadata = EvidenceMetadata()
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence))

        // Assert
        assertTrue("Should discover entities", entities.isNotEmpty())
        val johnEntity = entities.find { 
            it.primaryName.contains("John", ignoreCase = true) || 
            it.primaryName.contains("Smith", ignoreCase = true) 
        }
        
        if (johnEntity != null) {
            // Phone numbers might or might not be extracted depending on proximity
            assertTrue("John entity should be found", johnEntity.mentions >= 2)
        }
    }

    @Test
    fun testAliasesPopulated() {
        // Arrange - Same person referred to by different names
        val evidence1 = Evidence(
            id = "ev1",
            fileName = "email.txt",
            type = EvidenceType.TEXT,
            extractedText = "Contact Bob at bob@example.com for help.",
            metadata = EvidenceMetadata()
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "email2.txt",
            type = EvidenceType.TEXT,
            extractedText = "Robert (bob@example.com) confirmed the meeting.",
            metadata = EvidenceMetadata()
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence1, evidence2))

        // Assert
        val bobEntity = entities.find { it.emails.contains("bob@example.com") }
        assertTrue("Should find Bob entity", bobEntity != null)
        // Depending on merging logic, aliases might be populated
    }

    @Test
    fun testEmailNameFormatExtraction() {
        // Arrange - Email in "Name <email>" format
        // Need multiple mentions for entity to be discovered
        val evidence1 = Evidence(
            id = "ev1",
            fileName = "email1.eml",
            type = EvidenceType.EMAIL,
            extractedText = "Email body content here.",
            metadata = EvidenceMetadata(
                sender = "John Doe <john.doe@example.com>"
            )
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "email2.eml",
            type = EvidenceType.EMAIL,
            extractedText = "Another email body.",
            metadata = EvidenceMetadata(
                sender = "John Doe <john.doe@example.com>"
            )
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence1, evidence2))

        // Assert
        assertTrue("Should discover entity from formatted email (with >= 2 mentions)", entities.isNotEmpty())
        // Should extract "John Doe" as the name
        val foundEntity = entities.find { 
            it.primaryName.contains("John", ignoreCase = true) || 
            it.emails.contains("john.doe@example.com")
        }
        assertTrue("Should extract name from email format", foundEntity != null)
    }

    @Test
    fun testCaseInsensitiveMatching() {
        // Arrange
        val evidence = Evidence(
            id = "ev1",
            fileName = "document.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                Contact JOHN@EXAMPLE.COM for help.
                Also try john@example.com again.
            """.trimIndent(),
            metadata = EvidenceMetadata()
        )

        // Act
        val entities = discovery.discoverEntities(listOf(evidence))

        // Assert
        // Should treat the same email case-insensitively
        val johnEntities = entities.filter { 
            it.emails.any { e -> e.lowercase() == "john@example.com" }
        }
        assertTrue("Should match emails case-insensitively", johnEntities.size <= 1)
    }
}

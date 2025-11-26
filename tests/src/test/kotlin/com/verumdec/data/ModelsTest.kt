package com.verumdec.data

import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Tests for data models to ensure proper construction and behavior.
 */
class ModelsTest {
    
    @Test
    fun `test Case creation with defaults`() {
        val case = Case(name = "Test Case")
        
        assertNotNull("Case ID should be generated", case.id)
        assertEquals("Case name should match", "Test Case", case.name)
        assertTrue("Evidence list should be empty", case.evidence.isEmpty())
        assertTrue("Entities list should be empty", case.entities.isEmpty())
        assertTrue("Timeline should be empty", case.timeline.isEmpty())
        assertTrue("Contradictions should be empty", case.contradictions.isEmpty())
        assertTrue("Liability scores should be empty", case.liabilityScores.isEmpty())
        assertEquals("Narrative should be empty", "", case.narrative)
        assertNull("Sealed hash should be null initially", case.sealedHash)
    }
    
    @Test
    fun `test Evidence types`() {
        assertEquals(6, EvidenceType.values().size)
        assertNotNull(EvidenceType.PDF)
        assertNotNull(EvidenceType.IMAGE)
        assertNotNull(EvidenceType.TEXT)
        assertNotNull(EvidenceType.EMAIL)
        assertNotNull(EvidenceType.WHATSAPP)
        assertNotNull(EvidenceType.UNKNOWN)
    }
    
    @Test
    fun `test Evidence creation`() {
        val evidence = Evidence(
            type = EvidenceType.PDF,
            fileName = "contract.pdf",
            filePath = "/path/to/contract.pdf"
        )
        
        assertNotNull("Evidence ID should be generated", evidence.id)
        assertEquals("Type should match", EvidenceType.PDF, evidence.type)
        assertEquals("File name should match", "contract.pdf", evidence.fileName)
        assertEquals("File path should match", "/path/to/contract.pdf", evidence.filePath)
        assertFalse("Should not be processed initially", evidence.processed)
        assertEquals("Extracted text should be empty", "", evidence.extractedText)
    }
    
    @Test
    fun `test Entity creation`() {
        val entity = Entity(
            primaryName = "John Doe",
            emails = mutableListOf("john@example.com"),
            phoneNumbers = mutableListOf("+1234567890"),
            mentions = 10
        )
        
        assertNotNull("Entity ID should be generated", entity.id)
        assertEquals("Primary name should match", "John Doe", entity.primaryName)
        assertTrue("Should have email", entity.emails.contains("john@example.com"))
        assertTrue("Should have phone", entity.phoneNumbers.contains("+1234567890"))
        assertEquals("Mentions should match", 10, entity.mentions)
        assertTrue("Aliases should be empty initially", entity.aliases.isEmpty())
        assertTrue("Statements should be empty initially", entity.statements.isEmpty())
    }
    
    @Test
    fun `test Statement types`() {
        assertEquals(7, StatementType.values().size)
        assertNotNull(StatementType.CLAIM)
        assertNotNull(StatementType.DENIAL)
        assertNotNull(StatementType.PROMISE)
        assertNotNull(StatementType.ADMISSION)
        assertNotNull(StatementType.ACCUSATION)
        assertNotNull(StatementType.EXPLANATION)
        assertNotNull(StatementType.OTHER)
    }
    
    @Test
    fun `test TimelineEvent creation`() {
        val date = Date()
        val event = TimelineEvent(
            date = date,
            description = "Contract signed",
            sourceEvidenceId = "ev1",
            entityIds = listOf("entity1", "entity2"),
            eventType = EventType.DOCUMENT,
            significance = Significance.HIGH
        )
        
        assertNotNull("Event ID should be generated", event.id)
        assertEquals("Date should match", date, event.date)
        assertEquals("Description should match", "Contract signed", event.description)
        assertEquals("Event type should match", EventType.DOCUMENT, event.eventType)
        assertEquals("Significance should match", Significance.HIGH, event.significance)
        assertEquals("Should have 2 entity IDs", 2, event.entityIds.size)
    }
    
    @Test
    fun `test EventType enum`() {
        assertEquals(9, EventType.values().size)
        assertNotNull(EventType.COMMUNICATION)
        assertNotNull(EventType.PAYMENT)
        assertNotNull(EventType.PROMISE)
        assertNotNull(EventType.DOCUMENT)
        assertNotNull(EventType.CONTRADICTION)
        assertNotNull(EventType.ADMISSION)
        assertNotNull(EventType.DENIAL)
        assertNotNull(EventType.BEHAVIOR_CHANGE)
        assertNotNull(EventType.OTHER)
    }
    
    @Test
    fun `test Significance enum`() {
        assertEquals(4, Significance.values().size)
        assertNotNull(Significance.CRITICAL)
        assertNotNull(Significance.HIGH)
        assertNotNull(Significance.NORMAL)
        assertNotNull(Significance.LOW)
    }
    
    @Test
    fun `test Contradiction creation`() {
        val stmtA = Statement(
            entityId = "entity1",
            text = "I never agreed",
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL
        )
        
        val stmtB = Statement(
            entityId = "entity1",
            text = "I did agree to the terms",
            sourceEvidenceId = "ev2",
            type = StatementType.ADMISSION
        )
        
        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = stmtA,
            statementB = stmtB,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Direct contradiction detected",
            legalImplication = "This undermines credibility"
        )
        
        assertNotNull("Contradiction ID should be generated", contradiction.id)
        assertEquals("Entity ID should match", "entity1", contradiction.entityId)
        assertEquals("Type should be DIRECT", ContradictionType.DIRECT, contradiction.type)
        assertEquals("Severity should be CRITICAL", Severity.CRITICAL, contradiction.severity)
    }
    
    @Test
    fun `test ContradictionType enum`() {
        assertEquals(6, ContradictionType.values().size)
        assertNotNull(ContradictionType.DIRECT)
        assertNotNull(ContradictionType.CROSS_DOCUMENT)
        assertNotNull(ContradictionType.BEHAVIORAL)
        assertNotNull(ContradictionType.MISSING_EVIDENCE)
        assertNotNull(ContradictionType.TEMPORAL)
        assertNotNull(ContradictionType.THIRD_PARTY)
    }
    
    @Test
    fun `test Severity enum`() {
        assertEquals(4, Severity.values().size)
        assertNotNull(Severity.CRITICAL)
        assertNotNull(Severity.HIGH)
        assertNotNull(Severity.MEDIUM)
        assertNotNull(Severity.LOW)
    }
    
    @Test
    fun `test LiabilityScore creation`() {
        val score = LiabilityScore(
            entityId = "entity1",
            overallScore = 75.5f,
            contradictionScore = 45.0f,
            behavioralScore = 20.0f,
            evidenceContributionScore = 5.0f,
            chronologicalConsistencyScore = 15.0f,
            causalResponsibilityScore = 10.0f
        )
        
        assertEquals("Entity ID should match", "entity1", score.entityId)
        assertEquals("Overall score should match", 75.5f, score.overallScore, 0.01f)
        assertEquals("Contradiction score should match", 45.0f, score.contradictionScore, 0.01f)
        assertNotNull("Breakdown should exist", score.breakdown)
    }
    
    @Test
    fun `test LiabilityBreakdown defaults`() {
        val breakdown = LiabilityBreakdown()
        
        assertEquals(0, breakdown.totalContradictions)
        assertEquals(0, breakdown.criticalContradictions)
        assertTrue(breakdown.behavioralFlags.isEmpty())
        assertEquals(0, breakdown.evidenceProvided)
        assertEquals(0, breakdown.evidenceWithheld)
        assertEquals(0, breakdown.storyChanges)
        assertEquals(0, breakdown.initiatedEvents)
        assertFalse(breakdown.benefitedFinancially)
        assertFalse(breakdown.controlledInformation)
    }
    
    @Test
    fun `test BehavioralPattern creation`() {
        val pattern = BehavioralPattern(
            entityId = "entity1",
            type = BehaviorType.GASLIGHTING,
            instances = listOf("You're imagining things", "That never happened"),
            firstDetectedAt = Date(),
            severity = Severity.HIGH
        )
        
        assertNotNull("Pattern ID should be generated", pattern.id)
        assertEquals("Entity ID should match", "entity1", pattern.entityId)
        assertEquals("Type should be GASLIGHTING", BehaviorType.GASLIGHTING, pattern.type)
        assertEquals("Should have 2 instances", 2, pattern.instances.size)
        assertEquals("Severity should be HIGH", Severity.HIGH, pattern.severity)
    }
    
    @Test
    fun `test BehaviorType enum`() {
        assertEquals(12, BehaviorType.values().size)
        assertNotNull(BehaviorType.GASLIGHTING)
        assertNotNull(BehaviorType.DEFLECTION)
        assertNotNull(BehaviorType.PRESSURE_TACTICS)
        assertNotNull(BehaviorType.FINANCIAL_MANIPULATION)
        assertNotNull(BehaviorType.EMOTIONAL_MANIPULATION)
        assertNotNull(BehaviorType.SUDDEN_WITHDRAWAL)
        assertNotNull(BehaviorType.GHOSTING)
        assertNotNull(BehaviorType.OVER_EXPLAINING)
        assertNotNull(BehaviorType.SLIP_UP_ADMISSION)
        assertNotNull(BehaviorType.DELAYED_RESPONSE)
        assertNotNull(BehaviorType.BLAME_SHIFTING)
        assertNotNull(BehaviorType.PASSIVE_ADMISSION)
    }
    
    @Test
    fun `test ForensicReport creation`() {
        val report = ForensicReport(
            caseId = "case1",
            caseName = "Test Case",
            entities = emptyList(),
            timeline = emptyList(),
            contradictions = emptyList(),
            behavioralPatterns = emptyList(),
            liabilityScores = emptyMap(),
            narrativeSections = NarrativeSections(),
            sha512Hash = "abc123"
        )
        
        assertNotNull("Report ID should be generated", report.id)
        assertEquals("Case ID should match", "case1", report.caseId)
        assertEquals("Case name should match", "Test Case", report.caseName)
        assertEquals("SHA-512 hash should match", "abc123", report.sha512Hash)
        assertEquals("Version should be default", "1.0.0", report.version)
    }
    
    @Test
    fun `test NarrativeSections defaults`() {
        val sections = NarrativeSections()
        
        assertEquals("", sections.objectiveNarration)
        assertEquals("", sections.contradictionCommentary)
        assertEquals("", sections.behavioralPatternAnalysis)
        assertEquals("", sections.deductiveLogic)
        assertEquals("", sections.causalChain)
        assertEquals("", sections.finalSummary)
    }
    
    @Test
    fun `test EvidenceMetadata creation`() {
        val metadata = EvidenceMetadata(
            creationDate = Date(),
            modificationDate = Date(),
            author = "John Doe",
            sender = "john@example.com",
            receiver = "jane@example.com",
            subject = "Important Document",
            exifData = mapOf("Make" to "Canon", "Model" to "EOS")
        )
        
        assertNotNull("Creation date should exist", metadata.creationDate)
        assertEquals("Author should match", "John Doe", metadata.author)
        assertEquals("Sender should match", "john@example.com", metadata.sender)
        assertEquals("Receiver should match", "jane@example.com", metadata.receiver)
        assertEquals("Subject should match", "Important Document", metadata.subject)
        assertEquals("Should have 2 EXIF entries", 2, metadata.exifData.size)
    }
}

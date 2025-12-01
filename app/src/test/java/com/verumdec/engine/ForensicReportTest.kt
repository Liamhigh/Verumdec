package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit tests for ForensicReport data structures and validation
 * Tests report content generation without Android dependencies
 * 
 * Note: PDF generation itself requires Android Context, but we can test
 * the report content structure and validation logic.
 */
class ForensicReportTest {

    @Test
    fun testForensicReportCreation() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Test Entity",
            emails = mutableListOf("test@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf(),
            mentions = 5
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "Test statement 1",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )

        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "Test statement 2",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("test")
        )

        val timelineEvent = TimelineEvent(
            id = "te1",
            date = Date(),
            description = "Test event",
            sourceEvidenceId = "ev1",
            entityIds = listOf("entity1"),
            eventType = EventType.COMMUNICATION
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = stmt1,
            statementB = stmt2,
            type = ContradictionType.DIRECT,
            severity = Severity.HIGH,
            description = "Test contradiction",
            legalImplication = "Test implication"
        )

        val behavioralPattern = BehavioralPattern(
            entityId = "entity1",
            type = BehaviorType.GASLIGHTING,
            instances = listOf("Test instance"),
            severity = Severity.MEDIUM
        )

        val liabilityScore = LiabilityScore(
            entityId = "entity1",
            overallScore = 75.5f,
            contradictionScore = 80f,
            behavioralScore = 70f,
            evidenceContributionScore = 75f,
            chronologicalConsistencyScore = 80f,
            causalResponsibilityScore = 70f
        )

        val narrativeSections = NarrativeSections(
            objectiveNarration = "Chronological account",
            contradictionCommentary = "Contradiction analysis",
            behavioralPatternAnalysis = "Behavioral analysis",
            deductiveLogic = "Deductive reasoning",
            causalChain = "Cause and effect",
            finalSummary = "Final summary of the case"
        )

        // Act
        val report = ForensicReport(
            caseId = "case1",
            caseName = "Test Case",
            entities = listOf(entity),
            timeline = listOf(timelineEvent),
            contradictions = listOf(contradiction),
            behavioralPatterns = listOf(behavioralPattern),
            liabilityScores = mapOf("entity1" to liabilityScore),
            narrativeSections = narrativeSections,
            sha512Hash = "abc123hash"
        )

        // Assert
        assertEquals("Case name should match", "Test Case", report.caseName)
        assertEquals("Should have 1 entity", 1, report.entities.size)
        assertEquals("Should have 1 timeline event", 1, report.timeline.size)
        assertEquals("Should have 1 contradiction", 1, report.contradictions.size)
        assertEquals("Should have 1 behavioral pattern", 1, report.behavioralPatterns.size)
        assertEquals("Should have 1 liability score", 1, report.liabilityScores.size)
        assertEquals("Hash should match", "abc123hash", report.sha512Hash)
        assertNotNull("Generated date should not be null", report.generatedAt)
        assertNotNull("Report ID should not be null", report.id)
    }

    @Test
    fun testNarrativeSectionsStructure() {
        // Arrange & Act
        val narrative = NarrativeSections(
            objectiveNarration = "On January 1st, the contract was signed.",
            contradictionCommentary = "The party contradicted their earlier statement.",
            behavioralPatternAnalysis = "Gaslighting behavior detected.",
            deductiveLogic = "Therefore, the party's credibility is undermined.",
            causalChain = "Event A led to Event B.",
            finalSummary = "The defendant bears 80% liability."
        )

        // Assert
        assertTrue("Objective narration should not be empty", narrative.objectiveNarration.isNotBlank())
        assertTrue("Contradiction commentary should not be empty", narrative.contradictionCommentary.isNotBlank())
        assertTrue("Behavioral analysis should not be empty", narrative.behavioralPatternAnalysis.isNotBlank())
        assertTrue("Deductive logic should not be empty", narrative.deductiveLogic.isNotBlank())
        assertTrue("Causal chain should not be empty", narrative.causalChain.isNotBlank())
        assertTrue("Final summary should not be empty", narrative.finalSummary.isNotBlank())
    }

    @Test
    fun testLiabilityBreakdownStructure() {
        // Arrange & Act
        val breakdown = LiabilityBreakdown(
            totalContradictions = 5,
            criticalContradictions = 2,
            behavioralFlags = listOf("GASLIGHTING", "DEFLECTION"),
            evidenceProvided = 3,
            evidenceWithheld = 1,
            storyChanges = 2,
            initiatedEvents = 4,
            benefitedFinancially = true,
            controlledInformation = false
        )

        // Assert
        assertEquals("Total contradictions should match", 5, breakdown.totalContradictions)
        assertEquals("Critical contradictions should match", 2, breakdown.criticalContradictions)
        assertEquals("Should have 2 behavioral flags", 2, breakdown.behavioralFlags.size)
        assertEquals("Evidence provided should match", 3, breakdown.evidenceProvided)
        assertEquals("Evidence withheld should match", 1, breakdown.evidenceWithheld)
        assertEquals("Story changes should match", 2, breakdown.storyChanges)
        assertEquals("Initiated events should match", 4, breakdown.initiatedEvents)
        assertTrue("Should indicate financial benefit", breakdown.benefitedFinancially)
        assertFalse("Should not indicate information control", breakdown.controlledInformation)
    }

    @Test
    fun testContradictionTypes() {
        // Test all contradiction types are properly defined
        val types = ContradictionType.values()
        
        assertEquals("Should have 6 contradiction types", 6, types.size)
        assertTrue("Should include DIRECT", types.contains(ContradictionType.DIRECT))
        assertTrue("Should include CROSS_DOCUMENT", types.contains(ContradictionType.CROSS_DOCUMENT))
        assertTrue("Should include BEHAVIORAL", types.contains(ContradictionType.BEHAVIORAL))
        assertTrue("Should include MISSING_EVIDENCE", types.contains(ContradictionType.MISSING_EVIDENCE))
        assertTrue("Should include TEMPORAL", types.contains(ContradictionType.TEMPORAL))
        assertTrue("Should include THIRD_PARTY", types.contains(ContradictionType.THIRD_PARTY))
    }

    @Test
    fun testSeverityLevels() {
        // Test all severity levels are properly defined
        val severities = Severity.values()
        
        assertEquals("Should have 4 severity levels", 4, severities.size)
        assertTrue("Should include CRITICAL", severities.contains(Severity.CRITICAL))
        assertTrue("Should include HIGH", severities.contains(Severity.HIGH))
        assertTrue("Should include MEDIUM", severities.contains(Severity.MEDIUM))
        assertTrue("Should include LOW", severities.contains(Severity.LOW))
    }

    @Test
    fun testBehaviorTypes() {
        // Test all behavior types are properly defined
        val types = BehaviorType.values()
        
        assertEquals("Should have 12 behavior types", 12, types.size)
        assertTrue("Should include GASLIGHTING", types.contains(BehaviorType.GASLIGHTING))
        assertTrue("Should include DEFLECTION", types.contains(BehaviorType.DEFLECTION))
        assertTrue("Should include PRESSURE_TACTICS", types.contains(BehaviorType.PRESSURE_TACTICS))
        assertTrue("Should include FINANCIAL_MANIPULATION", types.contains(BehaviorType.FINANCIAL_MANIPULATION))
        assertTrue("Should include EMOTIONAL_MANIPULATION", types.contains(BehaviorType.EMOTIONAL_MANIPULATION))
        assertTrue("Should include SUDDEN_WITHDRAWAL", types.contains(BehaviorType.SUDDEN_WITHDRAWAL))
        assertTrue("Should include GHOSTING", types.contains(BehaviorType.GHOSTING))
        assertTrue("Should include OVER_EXPLAINING", types.contains(BehaviorType.OVER_EXPLAINING))
        assertTrue("Should include SLIP_UP_ADMISSION", types.contains(BehaviorType.SLIP_UP_ADMISSION))
        assertTrue("Should include DELAYED_RESPONSE", types.contains(BehaviorType.DELAYED_RESPONSE))
        assertTrue("Should include BLAME_SHIFTING", types.contains(BehaviorType.BLAME_SHIFTING))
        assertTrue("Should include PASSIVE_ADMISSION", types.contains(BehaviorType.PASSIVE_ADMISSION))
    }

    @Test
    fun testStatementTypes() {
        // Test all statement types are properly defined
        val types = StatementType.values()
        
        assertEquals("Should have 7 statement types", 7, types.size)
        assertTrue("Should include CLAIM", types.contains(StatementType.CLAIM))
        assertTrue("Should include DENIAL", types.contains(StatementType.DENIAL))
        assertTrue("Should include PROMISE", types.contains(StatementType.PROMISE))
        assertTrue("Should include ADMISSION", types.contains(StatementType.ADMISSION))
        assertTrue("Should include ACCUSATION", types.contains(StatementType.ACCUSATION))
        assertTrue("Should include EXPLANATION", types.contains(StatementType.EXPLANATION))
        assertTrue("Should include OTHER", types.contains(StatementType.OTHER))
    }

    @Test
    fun testEventTypes() {
        // Test all event types are properly defined
        val types = EventType.values()
        
        assertEquals("Should have 9 event types", 9, types.size)
        assertTrue("Should include COMMUNICATION", types.contains(EventType.COMMUNICATION))
        assertTrue("Should include PAYMENT", types.contains(EventType.PAYMENT))
        assertTrue("Should include PROMISE", types.contains(EventType.PROMISE))
        assertTrue("Should include DOCUMENT", types.contains(EventType.DOCUMENT))
        assertTrue("Should include CONTRADICTION", types.contains(EventType.CONTRADICTION))
        assertTrue("Should include ADMISSION", types.contains(EventType.ADMISSION))
        assertTrue("Should include DENIAL", types.contains(EventType.DENIAL))
        assertTrue("Should include BEHAVIOR_CHANGE", types.contains(EventType.BEHAVIOR_CHANGE))
        assertTrue("Should include OTHER", types.contains(EventType.OTHER))
    }

    @Test
    fun testEvidenceTypes() {
        // Test all evidence types are properly defined
        val types = EvidenceType.values()
        
        assertEquals("Should have 6 evidence types", 6, types.size)
        assertTrue("Should include PDF", types.contains(EvidenceType.PDF))
        assertTrue("Should include IMAGE", types.contains(EvidenceType.IMAGE))
        assertTrue("Should include TEXT", types.contains(EvidenceType.TEXT))
        assertTrue("Should include EMAIL", types.contains(EvidenceType.EMAIL))
        assertTrue("Should include WHATSAPP", types.contains(EvidenceType.WHATSAPP))
        assertTrue("Should include UNKNOWN", types.contains(EvidenceType.UNKNOWN))
    }

    @Test
    fun testSignificanceLevels() {
        // Test all significance levels are properly defined
        val levels = Significance.values()
        
        assertEquals("Should have 4 significance levels", 4, levels.size)
        assertTrue("Should include CRITICAL", levels.contains(Significance.CRITICAL))
        assertTrue("Should include HIGH", levels.contains(Significance.HIGH))
        assertTrue("Should include NORMAL", levels.contains(Significance.NORMAL))
        assertTrue("Should include LOW", levels.contains(Significance.LOW))
    }

    @Test
    fun testCaseStructure() {
        // Arrange & Act
        val case = Case(
            name = "Test Legal Case",
            description = "A test case for forensic analysis"
        )

        // Assert
        assertNotNull("Case ID should be generated", case.id)
        assertEquals("Case name should match", "Test Legal Case", case.name)
        assertEquals("Description should match", "A test case for forensic analysis", case.description)
        assertNotNull("Created date should not be null", case.createdAt)
        assertNotNull("Updated date should not be null", case.updatedAt)
        assertTrue("Evidence list should be empty initially", case.evidence.isEmpty())
        assertTrue("Entities list should be empty initially", case.entities.isEmpty())
        assertTrue("Timeline should be empty initially", case.timeline.isEmpty())
        assertTrue("Contradictions should be empty initially", case.contradictions.isEmpty())
        assertTrue("Liability scores should be empty initially", case.liabilityScores.isEmpty())
        assertEquals("Narrative should be empty initially", "", case.narrative)
        assertNull("Sealed hash should be null initially", case.sealedHash)
    }

    @Test
    fun testLiabilityScoreRanges() {
        // Test that liability scores are properly constrained
        val validScore = LiabilityScore(
            entityId = "test",
            overallScore = 75.0f,
            contradictionScore = 80.0f,
            behavioralScore = 70.0f,
            evidenceContributionScore = 60.0f,
            chronologicalConsistencyScore = 85.0f,
            causalResponsibilityScore = 65.0f
        )

        // Assert all scores are within expected ranges (0-100)
        assertTrue("Overall score should be >= 0", validScore.overallScore >= 0f)
        assertTrue("Overall score should be <= 100", validScore.overallScore <= 100f)
        assertTrue("Contradiction score should be >= 0", validScore.contradictionScore >= 0f)
        assertTrue("Behavioral score should be >= 0", validScore.behavioralScore >= 0f)
        assertTrue("Evidence score should be >= 0", validScore.evidenceContributionScore >= 0f)
        assertTrue("Consistency score should be >= 0", validScore.chronologicalConsistencyScore >= 0f)
        assertTrue("Causal score should be >= 0", validScore.causalResponsibilityScore >= 0f)
    }

    @Test
    fun testEntityWithAllFields() {
        // Test entity with all fields populated
        val entity = Entity(
            id = "ent1",
            primaryName = "John Doe",
            aliases = mutableListOf("Johnny", "J. Doe", "JD"),
            emails = mutableListOf("john@example.com", "jdoe@work.com"),
            phones = mutableListOf("+1234567890", "+0987654321"),
            bankAccounts = mutableListOf("1234567890"),
            mentions = 15,
            liabilityScore = 72.5f
        )

        // Assert
        assertEquals("Entity ID should match", "ent1", entity.id)
        assertEquals("Primary name should match", "John Doe", entity.primaryName)
        assertEquals("Should have 3 aliases", 3, entity.aliases.size)
        assertEquals("Should have 2 emails", 2, entity.emails.size)
        assertEquals("Should have 2 phones", 2, entity.phones.size)
        assertEquals("Should have 1 bank account", 1, entity.bankAccounts.size)
        assertEquals("Mentions should match", 15, entity.mentions)
        assertEquals("Liability score should match", 72.5f, entity.liabilityScore, 0.01f)
    }

    @Test
    fun testEvidenceWithFullMetadata() {
        // Test evidence with complete metadata
        val evidence = Evidence(
            id = "ev1",
            type = EvidenceType.EMAIL,
            fileName = "important_email.eml",
            filePath = "/documents/emails/important_email.eml",
            extractedText = "This is the email content.",
            metadata = EvidenceMetadata(
                creationDate = Date(),
                modificationDate = Date(),
                author = "John Doe",
                sender = "john@example.com",
                receiver = "jane@example.com",
                subject = "Important Contract Details",
                exifData = mapOf("key" to "value")
            ),
            processed = true
        )

        // Assert
        assertEquals("Evidence ID should match", "ev1", evidence.id)
        assertEquals("Type should be EMAIL", EvidenceType.EMAIL, evidence.type)
        assertEquals("Filename should match", "important_email.eml", evidence.fileName)
        assertTrue("Should be marked as processed", evidence.processed)
        assertNotNull("Creation date should not be null", evidence.metadata.creationDate)
        assertEquals("Author should match", "John Doe", evidence.metadata.author)
        assertEquals("Sender should match", "john@example.com", evidence.metadata.sender)
        assertEquals("Receiver should match", "jane@example.com", evidence.metadata.receiver)
        assertEquals("Subject should match", "Important Contract Details", evidence.metadata.subject)
    }

    @Test
    fun testContradictionWithFullDetails() {
        // Test contradiction with all fields
        val stmtA = Statement(
            id = "s1",
            entityId = "e1",
            text = "I paid the money",
            date = Date(System.currentTimeMillis() - 86400000),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("paid", "money")
        )

        val stmtB = Statement(
            id = "s2",
            entityId = "e1",
            text = "I never paid",
            date = Date(),
            sourceEvidenceId = "ev2",
            type = StatementType.DENIAL,
            keywords = listOf("never", "paid")
        )

        val contradiction = Contradiction(
            id = "c1",
            entityId = "e1",
            statementA = stmtA,
            statementB = stmtB,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Party claimed payment, then denied it",
            legalImplication = "This indicates potential fraud or dishonesty"
        )

        // Assert
        assertEquals("Contradiction ID should match", "c1", contradiction.id)
        assertEquals("Entity ID should match", "e1", contradiction.entityId)
        assertEquals("Type should be DIRECT", ContradictionType.DIRECT, contradiction.type)
        assertEquals("Severity should be CRITICAL", Severity.CRITICAL, contradiction.severity)
        assertNotNull("Statement A should not be null", contradiction.statementA)
        assertNotNull("Statement B should not be null", contradiction.statementB)
        assertTrue("Description should not be empty", contradiction.description.isNotBlank())
        assertTrue("Legal implication should not be empty", contradiction.legalImplication.isNotBlank())
    }
}

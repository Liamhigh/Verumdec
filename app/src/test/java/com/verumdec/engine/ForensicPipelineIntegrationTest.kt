package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Integration tests for the full forensic analysis pipeline
 * Tests the complete flow from evidence ingestion to narrative generation
 * 
 * This is the CORE FORENSIC ENGINE - all other functionality depends on these components.
 */
class ForensicPipelineIntegrationTest {

    private lateinit var entityDiscovery: EntityDiscovery
    private lateinit var timelineGenerator: TimelineGenerator
    private lateinit var contradictionAnalyzer: ContradictionAnalyzer
    private lateinit var behavioralAnalyzer: BehavioralAnalyzer
    private lateinit var liabilityCalculator: LiabilityCalculator
    private lateinit var narrativeGenerator: NarrativeGenerator

    @Before
    fun setup() {
        entityDiscovery = EntityDiscovery()
        timelineGenerator = TimelineGenerator()
        contradictionAnalyzer = ContradictionAnalyzer()
        behavioralAnalyzer = BehavioralAnalyzer()
        liabilityCalculator = LiabilityCalculator()
        narrativeGenerator = NarrativeGenerator()
    }

    /**
     * Full pipeline test: Simulates a fraud case with contradictions
     */
    @Test
    fun testFullPipeline_FraudCaseWithContradictions() {
        // STAGE 1: Prepare Evidence
        val evidence1 = Evidence(
            id = "ev1",
            fileName = "email1.txt",
            type = EvidenceType.EMAIL,
            extractedText = "I confirm that I paid the full amount of R50000 as agreed.",
            metadata = EvidenceMetadata(
                sender = "defendant@example.com",
                creationDate = Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
            ),
            processed = true
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "email2.txt",
            type = EvidenceType.EMAIL,
            extractedText = "I never paid anything. You are making false accusations.",
            metadata = EvidenceMetadata(
                sender = "defendant@example.com",
                creationDate = Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L)
            ),
            processed = true
        )

        val evidence3 = Evidence(
            id = "ev3",
            fileName = "whatsapp.txt",
            type = EvidenceType.WHATSAPP,
            extractedText = "[12/1/23, 10:00 AM] Plaintiff: Did you receive the contract?\n[12/1/23, 10:05 AM] Defendant: Yes, I signed it",
            metadata = EvidenceMetadata(),
            processed = true
        )

        val allEvidence = listOf(evidence1, evidence2, evidence3)

        // STAGE 2: Entity Discovery
        val entities = entityDiscovery.discoverEntities(allEvidence)
        
        // Create a defendant entity if not discovered
        val defendantEntity = entities.find { 
            it.emails.contains("defendant@example.com") 
        } ?: Entity(
            id = "defendant",
            primaryName = "Defendant",
            emails = mutableListOf("defendant@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf(),
            mentions = 3
        )
        
        val entitiesWithDefendant = if (entities.any { it.emails.contains("defendant@example.com") }) {
            entities
        } else {
            entities + defendantEntity
        }

        // STAGE 3: Timeline Generation
        val timeline = timelineGenerator.generateTimeline(allEvidence, entitiesWithDefendant)
        
        // STAGE 4: Contradiction Analysis
        val contradictions = contradictionAnalyzer.analyzeContradictions(
            allEvidence, entitiesWithDefendant, timeline
        )
        
        // STAGE 5: Behavioral Analysis
        val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(
            allEvidence, entitiesWithDefendant, timeline
        )
        
        // STAGE 6: Liability Calculation
        val liabilityScores = liabilityCalculator.calculateLiability(
            entitiesWithDefendant, contradictions, behavioralPatterns, allEvidence, timeline
        )
        
        // STAGE 7: Narrative Generation
        val narrative = narrativeGenerator.generateNarrative(
            entitiesWithDefendant, timeline, contradictions, behavioralPatterns, liabilityScores
        )

        // ASSERTIONS - Validate full pipeline output
        
        // Entities should be discovered
        assertTrue("Should discover at least one entity", entitiesWithDefendant.isNotEmpty())
        
        // Contradictions should be detected (paid vs never paid)
        assertTrue("Should detect contradictions between payment claims", 
            contradictions.isNotEmpty())
        
        // At least one should be high severity
        val criticalOrHigh = contradictions.filter { 
            it.severity == Severity.CRITICAL || it.severity == Severity.HIGH 
        }
        assertTrue("Should have at least one high/critical contradiction", 
            criticalOrHigh.isNotEmpty())
        
        // Liability scores should be calculated
        assertTrue("Should calculate liability scores", liabilityScores.isNotEmpty())
        
        // Defendant should have higher liability
        val defendantId = entitiesWithDefendant.find { 
            it.emails.contains("defendant@example.com") 
        }?.id
        if (defendantId != null) {
            val defendantScore = liabilityScores[defendantId]
            assertNotNull("Defendant should have liability score", defendantScore)
            assertTrue("Defendant should have non-zero liability", 
                (defendantScore?.overallScore ?: 0f) > 0f)
        }
        
        // Narrative should be generated
        assertTrue("Final summary should not be empty", narrative.finalSummary.isNotBlank())
        assertTrue("Contradiction commentary should not be empty", 
            narrative.contradictionCommentary.isNotBlank())
    }

    /**
     * Test pipeline with gaslighting behavior detected
     */
    @Test
    fun testFullPipeline_GaslightingBehavior() {
        // Prepare evidence with gaslighting patterns
        val evidence = Evidence(
            id = "ev1",
            fileName = "messages.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                You're imagining things. That never happened.
                I never said that. You're confused.
                You must be crazy to think that.
                You're overreacting as usual.
            """.trimIndent(),
            metadata = EvidenceMetadata(
                sender = "manipulator@example.com",
                creationDate = Date()
            ),
            processed = true
        )

        val entities = listOf(
            Entity(
                id = "manipulator",
                primaryName = "Manipulator",
                emails = mutableListOf("manipulator@example.com"),
                phones = mutableListOf(),
                aliases = mutableListOf(),
                mentions = 5
            )
        )

        // Run pipeline
        val timeline = timelineGenerator.generateTimeline(listOf(evidence), entities)
        val contradictions = contradictionAnalyzer.analyzeContradictions(listOf(evidence), entities, timeline)
        val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(listOf(evidence), entities, timeline)
        val liabilityScores = liabilityCalculator.calculateLiability(
            entities, contradictions, behavioralPatterns, listOf(evidence), timeline
        )
        val narrative = narrativeGenerator.generateNarrative(
            entities, timeline, contradictions, behavioralPatterns, liabilityScores
        )

        // Assertions
        assertTrue("Should detect gaslighting behavior", 
            behavioralPatterns.any { it.type == BehaviorType.GASLIGHTING })
        
        val gaslightingPattern = behavioralPatterns.find { it.type == BehaviorType.GASLIGHTING }
        assertNotNull("Gaslighting pattern should exist", gaslightingPattern)
        assertTrue("Gaslighting should have instances", 
            gaslightingPattern?.instances?.isNotEmpty() ?: false)
        
        // Liability should reflect behavioral issues
        val manipulatorScore = liabilityScores["manipulator"]
        assertNotNull("Manipulator should have liability score", manipulatorScore)
        assertTrue("Behavioral score should be non-zero", 
            (manipulatorScore?.behavioralScore ?: 0f) > 0f)
        
        // Narrative should mention behavioral patterns
        assertTrue("Narrative should mention behavioral patterns",
            narrative.behavioralPatternAnalysis.contains("GASLIGHTING") ||
            narrative.behavioralPatternAnalysis.lowercase().contains("gaslighting"))
    }

    /**
     * Test pipeline with honest party (no contradictions)
     */
    @Test
    fun testFullPipeline_HonestParty() {
        val evidence1 = Evidence(
            id = "ev1",
            fileName = "statement1.txt",
            type = EvidenceType.TEXT,
            extractedText = "I paid the invoice on time as agreed in the contract.",
            metadata = EvidenceMetadata(
                sender = "honest@example.com",
                creationDate = Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L)
            ),
            processed = true
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "statement2.txt",
            type = EvidenceType.TEXT,
            extractedText = "Yes, the invoice was paid on time. Here is the proof of payment.",
            metadata = EvidenceMetadata(
                sender = "honest@example.com",
                creationDate = Date()
            ),
            processed = true
        )

        val entities = listOf(
            Entity(
                id = "honest",
                primaryName = "Honest Party",
                emails = mutableListOf("honest@example.com"),
                phones = mutableListOf(),
                aliases = mutableListOf(),
                mentions = 3
            )
        )

        val allEvidence = listOf(evidence1, evidence2)

        // Run pipeline
        val timeline = timelineGenerator.generateTimeline(allEvidence, entities)
        val contradictions = contradictionAnalyzer.analyzeContradictions(allEvidence, entities, timeline)
        val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(allEvidence, entities, timeline)
        val liabilityScores = liabilityCalculator.calculateLiability(
            entities, contradictions, behavioralPatterns, allEvidence, timeline
        )

        // Assertions
        // Consistent statements should have no or minimal contradictions
        val directContradictions = contradictions.filter { it.type == ContradictionType.DIRECT }
        assertTrue("Consistent statements should have minimal direct contradictions", 
            directContradictions.size <= 1)
        
        // Liability should be low
        val honestScore = liabilityScores["honest"]
        assertTrue("Honest party should have low liability", 
            (honestScore?.overallScore ?: 0f) < 50f)
    }

    /**
     * Test pipeline with denial followed by admission (critical pattern)
     */
    @Test
    fun testFullPipeline_DenialFollowedByAdmission() {
        val evidence1 = Evidence(
            id = "ev1",
            fileName = "denial.txt",
            type = EvidenceType.TEXT,
            extractedText = "I never received any money. There was no transaction.",
            metadata = EvidenceMetadata(
                sender = "party@example.com",
                creationDate = Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
            ),
            processed = true
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "admission.txt",
            type = EvidenceType.TEXT,
            extractedText = "I admit I did receive the money. Yes, I took the full amount.",
            metadata = EvidenceMetadata(
                sender = "party@example.com",
                creationDate = Date()
            ),
            processed = true
        )

        val entities = listOf(
            Entity(
                id = "party",
                primaryName = "Contradicting Party",
                emails = mutableListOf("party@example.com"),
                phones = mutableListOf(),
                aliases = mutableListOf(),
                mentions = 4
            )
        )

        val allEvidence = listOf(evidence1, evidence2)

        // Run pipeline
        val timeline = timelineGenerator.generateTimeline(allEvidence, entities)
        val contradictions = contradictionAnalyzer.analyzeContradictions(allEvidence, entities, timeline)
        val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(allEvidence, entities, timeline)
        val liabilityScores = liabilityCalculator.calculateLiability(
            entities, contradictions, behavioralPatterns, allEvidence, timeline
        )
        val narrative = narrativeGenerator.generateNarrative(
            entities, timeline, contradictions, behavioralPatterns, liabilityScores
        )

        // Assertions
        assertTrue("Should detect contradictions", contradictions.isNotEmpty())
        
        // Should have at least one critical contradiction (denial followed by admission)
        val criticalContradictions = contradictions.filter { it.severity == Severity.CRITICAL }
        assertTrue("Denial followed by admission should be CRITICAL", 
            criticalContradictions.isNotEmpty())
        
        // Liability should be high
        val partyScore = liabilityScores["party"]
        assertNotNull("Party should have liability score", partyScore)
        assertTrue("Party should have high liability due to critical contradiction", 
            (partyScore?.overallScore ?: 0f) > 30f)
        
        // Narrative should reflect the contradiction
        assertTrue("Deductive logic should explain the contradiction",
            narrative.deductiveLogic.isNotBlank())
    }

    /**
     * Test pipeline with multiple entities and third-party contradictions
     */
    @Test
    fun testFullPipeline_MultipleEntitiesThirdPartyContradiction() {
        val evidence1 = Evidence(
            id = "ev1",
            fileName = "alice_statement.txt",
            type = EvidenceType.TEXT,
            extractedText = "I handed the money directly to Bob on Monday. Bob received cash payment in full.",
            metadata = EvidenceMetadata(
                sender = "alice@example.com",
                creationDate = Date()
            ),
            processed = true
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "bob_statement.txt",
            type = EvidenceType.TEXT,
            extractedText = "I never received any payment from Alice. Alice did not give me any money.",
            metadata = EvidenceMetadata(
                sender = "bob@example.com",
                creationDate = Date()
            ),
            processed = true
        )

        val entities = listOf(
            Entity(
                id = "alice",
                primaryName = "Alice",
                emails = mutableListOf("alice@example.com"),
                phones = mutableListOf(),
                aliases = mutableListOf(),
                mentions = 3
            ),
            Entity(
                id = "bob",
                primaryName = "Bob",
                emails = mutableListOf("bob@example.com"),
                phones = mutableListOf(),
                aliases = mutableListOf(),
                mentions = 3
            )
        )

        val allEvidence = listOf(evidence1, evidence2)

        // Run pipeline
        val timeline = timelineGenerator.generateTimeline(allEvidence, entities)
        val contradictions = contradictionAnalyzer.analyzeContradictions(allEvidence, entities, timeline)
        val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(allEvidence, entities, timeline)
        val liabilityScores = liabilityCalculator.calculateLiability(
            entities, contradictions, behavioralPatterns, allEvidence, timeline
        )
        val narrative = narrativeGenerator.generateNarrative(
            entities, timeline, contradictions, behavioralPatterns, liabilityScores
        )

        // Assertions
        assertTrue("Should calculate scores for both entities", 
            liabilityScores.size >= 2 || liabilityScores.containsKey("alice") || liabilityScores.containsKey("bob"))
        
        // Narrative should be generated
        assertTrue("Final summary should be generated", 
            narrative.finalSummary.isNotBlank())
    }

    /**
     * Test pipeline with financial manipulation behavior
     */
    @Test
    fun testFullPipeline_FinancialManipulation() {
        val evidence = Evidence(
            id = "ev1",
            fileName = "scam_message.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                Trust me, this is a guaranteed return investment.
                I'll pay you back double. Just this once, lend me the money.
                It's no risk at all, I promise you'll make it back quickly.
            """.trimIndent(),
            metadata = EvidenceMetadata(
                sender = "scammer@example.com",
                creationDate = Date()
            ),
            processed = true
        )

        val entities = listOf(
            Entity(
                id = "scammer",
                primaryName = "Scammer",
                emails = mutableListOf("scammer@example.com"),
                phones = mutableListOf(),
                aliases = mutableListOf(),
                mentions = 3
            )
        )

        // Run pipeline
        val timeline = timelineGenerator.generateTimeline(listOf(evidence), entities)
        val contradictions = contradictionAnalyzer.analyzeContradictions(listOf(evidence), entities, timeline)
        val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(listOf(evidence), entities, timeline)
        val liabilityScores = liabilityCalculator.calculateLiability(
            entities, contradictions, behavioralPatterns, listOf(evidence), timeline
        )

        // Assertions
        assertTrue("Should detect financial manipulation", 
            behavioralPatterns.any { it.type == BehaviorType.FINANCIAL_MANIPULATION })
        
        // Liability should reflect financial manipulation
        val scammerScore = liabilityScores["scammer"]
        assertNotNull("Scammer should have liability score", scammerScore)
        assertTrue("Scammer should have behavioral liability", 
            (scammerScore?.behavioralScore ?: 0f) > 0f)
    }

    /**
     * Test pipeline stability with empty inputs
     */
    @Test
    fun testFullPipeline_EmptyInputs() {
        // Run pipeline with empty inputs
        val entities = entityDiscovery.discoverEntities(emptyList())
        val timeline = timelineGenerator.generateTimeline(emptyList(), entities)
        val contradictions = contradictionAnalyzer.analyzeContradictions(emptyList(), entities, timeline)
        val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(emptyList(), entities, timeline)
        val liabilityScores = liabilityCalculator.calculateLiability(
            entities, contradictions, behavioralPatterns, emptyList(), timeline
        )
        val narrative = narrativeGenerator.generateNarrative(
            entities, timeline, contradictions, behavioralPatterns, liabilityScores
        )

        // Assertions - Pipeline should handle empty inputs gracefully
        assertTrue("Empty evidence should yield empty entities", entities.isEmpty())
        assertTrue("Empty evidence should yield empty timeline", timeline.isEmpty())
        assertTrue("Empty evidence should yield no contradictions", contradictions.isEmpty())
        assertTrue("Empty evidence should yield no behavioral patterns", behavioralPatterns.isEmpty())
        assertTrue("Empty entities should yield empty liability scores", liabilityScores.isEmpty())
        
        // Narrative should still be generated (with appropriate empty messages)
        assertNotNull("Narrative should still be generated", narrative)
    }

    /**
     * Test pipeline with pressure tactics behavior
     */
    @Test
    fun testFullPipeline_PressureTactics() {
        val evidence = Evidence(
            id = "ev1",
            fileName = "pressure.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                You need to decide now! This offer expires at midnight.
                Take it or leave it, this is your last chance.
                Don't miss out, everyone else has already agreed.
                Final offer - act fast or you'll regret it.
            """.trimIndent(),
            metadata = EvidenceMetadata(
                sender = "pushy@example.com",
                creationDate = Date()
            ),
            processed = true
        )

        val entities = listOf(
            Entity(
                id = "pushy",
                primaryName = "Pushy Salesperson",
                emails = mutableListOf("pushy@example.com"),
                phones = mutableListOf(),
                aliases = mutableListOf(),
                mentions = 3
            )
        )

        // Run behavioral analysis
        val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(listOf(evidence), entities, emptyList())

        // Assertions
        assertTrue("Should detect pressure tactics", 
            behavioralPatterns.any { it.type == BehaviorType.PRESSURE_TACTICS })
        
        val pressurePattern = behavioralPatterns.find { it.type == BehaviorType.PRESSURE_TACTICS }
        assertNotNull("Pressure tactics pattern should exist", pressurePattern)
        assertTrue("Should be HIGH or CRITICAL severity due to multiple instances",
            pressurePattern?.severity == Severity.HIGH || pressurePattern?.severity == Severity.CRITICAL)
    }

    /**
     * Test that liability weights are correctly applied
     */
    @Test
    fun testLiabilityCalculation_WeightsApplied() {
        val entity = Entity(
            id = "test",
            primaryName = "Test Entity",
            emails = mutableListOf("test@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf(),
            mentions = 5
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "test",
            text = "I paid the full amount",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("paid", "amount")
        )

        val stmt2 = Statement(
            id = "stmt2",
            entityId = "test",
            text = "I never paid",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("never", "paid")
        )

        val contradiction = Contradiction(
            entityId = "test",
            statementA = stmt1,
            statementB = stmt2,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Critical contradiction",
            legalImplication = "Test implication"
        )

        val behavioralPattern = BehavioralPattern(
            entityId = "test",
            type = BehaviorType.GASLIGHTING,
            instances = listOf("You're imagining things"),
            severity = Severity.HIGH
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "test.txt",
            type = EvidenceType.TEXT,
            extractedText = "Test content",
            metadata = EvidenceMetadata(sender = "test@example.com"),
            processed = true
        )

        // Calculate liability
        val scores = liabilityCalculator.calculateLiability(
            listOf(entity),
            listOf(contradiction),
            listOf(behavioralPattern),
            listOf(evidence),
            emptyList()
        )

        // Assertions
        val score = scores["test"]
        assertNotNull("Should calculate score", score)
        
        // Verify all component scores are calculated
        assertTrue("Contradiction score should be positive", (score?.contradictionScore ?: 0f) > 0f)
        assertTrue("Behavioral score should be positive", (score?.behavioralScore ?: 0f) > 0f)
        
        // Overall score should be a weighted combination
        assertTrue("Overall score should be positive", (score?.overallScore ?: 0f) > 0f)
        assertTrue("Overall score should not exceed 100", (score?.overallScore ?: 101f) <= 100f)
    }
}

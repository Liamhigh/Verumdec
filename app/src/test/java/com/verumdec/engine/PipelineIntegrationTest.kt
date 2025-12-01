package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Integration tests for the full Verum Omnis pipeline
 * Verifies that all engine components are properly wired together
 * and work cohesively to analyze contradictions and generate results.
 */
class PipelineIntegrationTest {

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

    @Test
    fun testFullPipelineWithContradictingEvidence() {
        // Arrange - Create evidence that contains contradictions
        val evidence1 = Evidence(
            id = "ev1",
            fileName = "statement1.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                John Smith here. I paid the full amount of 10000 dollars on March 15th.
                The payment was completed as agreed and confirmed by email.
            """.trimIndent(),
            metadata = EvidenceMetadata(
                sender = "john.smith@example.com",
                creationDate = Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000) // 7 days ago
            ),
            processed = true
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "statement2.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                John Smith: I never paid anything. No deal ever existed between us.
                There was no agreement and definitely no payment of 10000 dollars.
            """.trimIndent(),
            metadata = EvidenceMetadata(
                sender = "john.smith@example.com",
                creationDate = Date() // Today
            ),
            processed = true
        )

        val evidenceList = listOf(evidence1, evidence2)

        // Act - Step 1: Entity Discovery
        val entities = entityDiscovery.discoverEntities(evidenceList)
        
        // Assert - Should find John Smith
        assertTrue("Should discover at least one entity", entities.isNotEmpty())
        val johnEntity = entities.find { 
            it.primaryName.contains("John", ignoreCase = true) || 
            it.emails.any { email -> email.contains("john", ignoreCase = true) }
        }
        assertNotNull("Should find John Smith entity", johnEntity)

        // Act - Step 2: Timeline Generation
        val timeline = timelineGenerator.generateTimeline(evidenceList, entities)
        
        // Assert - Should have timeline events
        assertTrue("Should generate timeline events", timeline.isNotEmpty())

        // Act - Step 3: Contradiction Analysis
        val contradictions = contradictionAnalyzer.analyzeContradictions(evidenceList, entities, timeline)
        
        // Assert - Should detect contradictions (paid vs never paid)
        assertTrue("Should detect contradictions", contradictions.isNotEmpty())
        val hasPaymentContradiction = contradictions.any { c ->
            c.description.contains("paid", ignoreCase = true) ||
            c.description.contains("never", ignoreCase = true) ||
            c.type == ContradictionType.DIRECT ||
            c.type == ContradictionType.CROSS_DOCUMENT
        }
        assertTrue("Should detect payment contradiction", hasPaymentContradiction)

        // Act - Step 4: Behavioral Analysis
        val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(evidenceList, entities, timeline)
        
        // Assert - May detect behavioral patterns (denial patterns)
        // Note: Not all cases will have behavioral patterns
        assertNotNull("Behavioral analysis should not return null", behavioralPatterns)

        // Act - Step 5: Liability Calculation
        val liabilityScores = liabilityCalculator.calculateLiability(
            entities, contradictions, behavioralPatterns, evidenceList, timeline
        )
        
        // Assert - Should have liability scores for entities
        assertFalse("Should calculate liability scores", liabilityScores.isEmpty())
        if (johnEntity != null) {
            val johnScore = liabilityScores[johnEntity.id]
            assertNotNull("Should have score for John Smith", johnScore)
            assertTrue("John Smith should have positive liability due to contradictions", 
                johnScore?.overallScore ?: 0f > 0f)
        }

        // Act - Step 6: Narrative Generation
        val narrativeSections = narrativeGenerator.generateNarrative(
            entities, timeline, contradictions, behavioralPatterns, liabilityScores
        )
        
        // Assert - Should generate narrative sections
        assertTrue("Should generate objective narration", narrativeSections.objectiveNarration.isNotEmpty())
        assertTrue("Should generate contradiction commentary", narrativeSections.contradictionCommentary.isNotEmpty())
        assertTrue("Should generate final summary", narrativeSections.finalSummary.isNotEmpty())
    }

    @Test
    fun testPipelineWithConsistentEvidence() {
        // Arrange - Create consistent evidence (no contradictions expected)
        val evidence = Evidence(
            id = "ev1",
            fileName = "consistent.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                Sarah Jones confirmed the meeting for Tuesday.
                The project timeline was discussed and agreed upon.
                Sarah Jones: Yes, Tuesday works perfectly for everyone.
            """.trimIndent(),
            metadata = EvidenceMetadata(
                sender = "sarah.jones@example.com",
                creationDate = Date()
            ),
            processed = true
        )

        val evidenceList = listOf(evidence)

        // Act - Full pipeline
        val entities = entityDiscovery.discoverEntities(evidenceList)
        val timeline = timelineGenerator.generateTimeline(evidenceList, entities)
        val contradictions = contradictionAnalyzer.analyzeContradictions(evidenceList, entities, timeline)
        val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(evidenceList, entities, timeline)
        val liabilityScores = liabilityCalculator.calculateLiability(
            entities, contradictions, behavioralPatterns, evidenceList, timeline
        )
        val narrativeSections = narrativeGenerator.generateNarrative(
            entities, timeline, contradictions, behavioralPatterns, liabilityScores
        )

        // Assert - Consistent evidence should have minimal contradictions
        assertTrue("Consistent evidence should have few or no contradictions", contradictions.size <= 1)
        
        // Liability should be low for consistent behavior
        val maxScore = liabilityScores.values.maxOfOrNull { it.overallScore } ?: 0f
        assertTrue("Liability should be low for consistent evidence", maxScore < 50f)
    }

    @Test
    fun testPipelineHandlesEmptyEvidence() {
        // Act - Run pipeline with empty evidence
        val entities = entityDiscovery.discoverEntities(emptyList())
        val timeline = timelineGenerator.generateTimeline(emptyList(), entities)
        val contradictions = contradictionAnalyzer.analyzeContradictions(emptyList(), entities, timeline)
        val behavioralPatterns = behavioralAnalyzer.analyzeBehavior(emptyList(), entities, timeline)
        val liabilityScores = liabilityCalculator.calculateLiability(
            entities, contradictions, behavioralPatterns, emptyList(), timeline
        )
        val narrativeSections = narrativeGenerator.generateNarrative(
            entities, timeline, contradictions, behavioralPatterns, liabilityScores
        )

        // Assert - Should handle gracefully without crashing
        assertTrue("Empty evidence should result in no entities", entities.isEmpty())
        assertTrue("Empty evidence should result in no timeline", timeline.isEmpty())
        assertTrue("Empty evidence should result in no contradictions", contradictions.isEmpty())
        assertTrue("Empty evidence should result in no liability scores", liabilityScores.isEmpty())
        assertNotNull("Narrative should still be generated", narrativeSections)
    }

    @Test
    fun testEntityDiscoveryFindsMultipleEntities() {
        // Arrange - Evidence with multiple entities
        val evidence = Evidence(
            id = "ev1",
            fileName = "conversation.txt",
            type = EvidenceType.TEXT,
            extractedText = """
                From: alice@company.com
                To: bob@client.org
                Subject: Contract Discussion
                
                Alice Brown mentioned that the contract needs review.
                Bob Johnson replied that he would send the updated terms.
                Charlie Davis was CC'd on the email for reference.
                
                Contact: alice@company.com, bob@client.org
            """.trimIndent(),
            metadata = EvidenceMetadata(
                sender = "alice@company.com",
                receiver = "bob@client.org",
                creationDate = Date()
            ),
            processed = true
        )

        // Act
        val entities = entityDiscovery.discoverEntities(listOf(evidence))

        // Assert - Should find multiple entities
        assertTrue("Should find multiple entities", entities.size >= 2)
    }

    @Test
    fun testContradictionSeverityScoring() {
        // Arrange - Create evidence with clear denial followed by admission (CRITICAL)
        val evidence1 = Evidence(
            id = "ev1",
            fileName = "denial.txt",
            type = EvidenceType.TEXT,
            extractedText = "I never received any money from the account.",
            metadata = EvidenceMetadata(
                sender = "suspect@example.com",
                creationDate = Date(System.currentTimeMillis() - 1000000)
            ),
            processed = true
        )

        val evidence2 = Evidence(
            id = "ev2",
            fileName = "admission.txt",
            type = EvidenceType.TEXT,
            extractedText = "I admit I did receive the money from the account.",
            metadata = EvidenceMetadata(
                sender = "suspect@example.com",
                creationDate = Date()
            ),
            processed = true
        )

        val evidenceList = listOf(evidence1, evidence2)

        // Act
        val entities = entityDiscovery.discoverEntities(evidenceList)
        val timeline = timelineGenerator.generateTimeline(evidenceList, entities)
        val contradictions = contradictionAnalyzer.analyzeContradictions(evidenceList, entities, timeline)

        // Assert - Should have contradictions with appropriate severity
        assertTrue("Should detect contradictions", contradictions.isNotEmpty())
        
        // Check if any contradiction is marked as CRITICAL or HIGH
        val hasSevereContradiction = contradictions.any { 
            it.severity == Severity.CRITICAL || it.severity == Severity.HIGH 
        }
        assertTrue("Denial followed by admission should be severe", hasSevereContradiction)
    }

    @Test
    fun testLiabilityScoreComponents() {
        // Arrange
        val entity = Entity(
            id = "entity1",
            primaryName = "Test Person",
            emails = mutableListOf("test@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf(),
            mentions = 5
        )

        val statement1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "I sent the payment",
            date = Date(System.currentTimeMillis() - 1000000),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("sent", "payment")
        )

        val statement2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "I never sent any payment",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("never", "sent", "payment")
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = statement1,
            statementB = statement2,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Direct contradiction about payment",
            legalImplication = "This indicates deception"
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "test.txt",
            type = EvidenceType.TEXT,
            extractedText = "Test evidence",
            metadata = EvidenceMetadata(sender = "test@example.com"),
            processed = true
        )

        // Act
        val scores = liabilityCalculator.calculateLiability(
            listOf(entity),
            listOf(contradiction),
            emptyList(),
            listOf(evidence),
            emptyList()
        )

        // Assert
        assertTrue("Should have score for entity", scores.containsKey("entity1"))
        val score = scores["entity1"]!!
        
        // Verify all score components are calculated
        assertTrue("Contradiction score should be > 0", score.contradictionScore > 0f)
        assertTrue("Overall score should be > 0", score.overallScore > 0f)
        assertTrue("Overall score should not exceed 100", score.overallScore <= 100f)
        
        // Verify breakdown is populated
        assertEquals("Should track critical contradictions", 1, score.breakdown.criticalContradictions)
        assertEquals("Should track total contradictions", 1, score.breakdown.totalContradictions)
    }

    @Test
    fun testNarrativeGenerationStructure() {
        // Arrange - Create a complete case scenario
        val entity = Entity(
            id = "entity1",
            primaryName = "John Defendant",
            aliases = mutableListOf(),
            emails = mutableListOf("john@example.com"),
            phones = mutableListOf(),
            mentions = 10
        )

        val timeline = listOf(
            TimelineEvent(
                id = "te1",
                date = Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000),
                description = "Initial claim made",
                sourceEvidenceId = "ev1",
                entityIds = listOf("entity1"),
                eventType = EventType.CLAIM,
                significance = Significance.HIGH
            ),
            TimelineEvent(
                id = "te2",
                date = Date(),
                description = "Contradicting statement made",
                sourceEvidenceId = "ev2",
                entityIds = listOf("entity1"),
                eventType = EventType.CONTRADICTION,
                significance = Significance.CRITICAL
            )
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "Original claim",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("claim")
        )
        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "Contradicting claim",
            date = Date(),
            sourceEvidenceId = "ev2",
            type = StatementType.DENIAL,
            keywords = listOf("claim")
        )

        val contradictions = listOf(
            Contradiction(
                entityId = "entity1",
                statementA = stmt1,
                statementB = stmt2,
                type = ContradictionType.DIRECT,
                severity = Severity.CRITICAL,
                description = "Direct contradiction detected",
                legalImplication = "This undermines credibility"
            )
        )

        val liabilityScores = mapOf(
            "entity1" to LiabilityScore(
                entityId = "entity1",
                overallScore = 75f,
                contradictionScore = 80f,
                behavioralScore = 50f,
                evidenceContributionScore = 70f,
                chronologicalConsistencyScore = 85f,
                causalResponsibilityScore = 60f
            )
        )

        // Act
        val narrativeSections = narrativeGenerator.generateNarrative(
            listOf(entity),
            timeline,
            contradictions,
            emptyList(),
            liabilityScores
        )

        // Assert - All sections should be populated
        assertTrue("Objective narration should exist", narrativeSections.objectiveNarration.isNotBlank())
        assertTrue("Contradiction commentary should exist", narrativeSections.contradictionCommentary.isNotBlank())
        assertTrue("Final summary should exist", narrativeSections.finalSummary.isNotBlank())
        
        // Verify content includes key information
        assertTrue("Summary should mention liability", 
            narrativeSections.finalSummary.contains("LIABILITY", ignoreCase = true))
        assertTrue("Contradiction commentary should mention contradiction", 
            narrativeSections.contradictionCommentary.contains("contradiction", ignoreCase = true))
    }

    @Test
    fun testCaseSummaryFromEngine() {
        // Arrange - Create a complete case
        val entity = Entity(
            id = "entity1",
            primaryName = "Test Entity",
            emails = mutableListOf("test@example.com"),
            phones = mutableListOf(),
            aliases = mutableListOf()
        )

        val evidence = Evidence(
            id = "ev1",
            fileName = "test.txt",
            type = EvidenceType.TEXT,
            extractedText = "Test evidence",
            metadata = EvidenceMetadata(),
            processed = true
        )

        val stmt1 = Statement(
            id = "stmt1",
            entityId = "entity1",
            text = "S1",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.CLAIM,
            keywords = listOf("test")
        )
        val stmt2 = Statement(
            id = "stmt2",
            entityId = "entity1",
            text = "S2",
            date = Date(),
            sourceEvidenceId = "ev1",
            type = StatementType.DENIAL,
            keywords = listOf("test")
        )

        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = stmt1,
            statementB = stmt2,
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Test",
            legalImplication = "Test"
        )

        val timelineEvent = TimelineEvent(
            id = "te1",
            date = Date(),
            description = "Test event",
            sourceEvidenceId = "ev1",
            entityIds = listOf("entity1"),
            eventType = EventType.COMMUNICATION,
            significance = Significance.HIGH
        )

        val liabilityScore = LiabilityScore(
            entityId = "entity1",
            overallScore = 80f,
            contradictionScore = 85f,
            behavioralScore = 75f,
            evidenceContributionScore = 80f,
            chronologicalConsistencyScore = 90f,
            causalResponsibilityScore = 70f
        )

        val testCase = Case(
            id = "case1",
            name = "Integration Test Case",
            description = "Testing pipeline integration",
            evidence = mutableListOf(evidence),
            entities = mutableListOf(entity),
            timeline = mutableListOf(timelineEvent),
            contradictions = mutableListOf(contradiction),
            narrative = "Test narrative",
            liabilityScores = mutableMapOf("entity1" to liabilityScore),
            createdAt = Date(),
            updatedAt = Date()
        )

        // Act - Use the engine to get summary (with null context for testing)
        val engine = ContradictionEngine(null)
        val summary = engine.getSummary(testCase)

        // Assert
        assertEquals("Should count evidence", 1, summary.totalEvidence)
        assertEquals("Should count processed evidence", 1, summary.processedEvidence)
        assertEquals("Should count entities", 1, summary.entitiesFound)
        assertEquals("Should count timeline events", 1, summary.timelineEvents)
        assertEquals("Should count contradictions", 1, summary.totalContradictions)
        assertEquals("Should count critical contradictions", 1, summary.criticalContradictions)
        assertEquals("Should identify highest liability entity", "Test Entity", summary.highestLiabilityEntity)
        assertEquals("Should report highest score", 80f, summary.highestLiabilityScore, 0.1f)
    }
}

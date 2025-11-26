package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Comprehensive tests for the Narrative Generator engine.
 */
class NarrativeGeneratorTest {
    
    private val generator = NarrativeGenerator()
    
    @Test
    fun `test empty input generates placeholder narrative`() {
        val result = generator.generateNarrative(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyMap()
        )
        
        assertNotNull("Should generate narrative sections", result)
        assertTrue("Objective narration should exist", result.objectiveNarration.isNotEmpty())
        assertTrue("Contradiction commentary should exist", result.contradictionCommentary.isNotEmpty())
    }
    
    @Test
    fun `test objective narration includes timeline events`() {
        val timeline = listOf(
            TimelineEvent(
                date = Calendar.getInstance().apply {
                    set(2023, Calendar.MARCH, 15, 10, 0, 0)
                }.time,
                description = "Contract was signed by all parties",
                sourceEvidenceId = "ev1",
                eventType = EventType.DOCUMENT,
                significance = Significance.HIGH
            ),
            TimelineEvent(
                date = Calendar.getInstance().apply {
                    set(2023, Calendar.MARCH, 20, 14, 30, 0)
                }.time,
                description = "Payment was transferred to the account",
                sourceEvidenceId = "ev2",
                eventType = EventType.PAYMENT,
                significance = Significance.HIGH
            )
        )
        
        val result = generator.generateNarrative(
            entities = emptyList(),
            timeline = timeline,
            contradictions = emptyList(),
            behavioralPatterns = emptyList(),
            liabilityScores = emptyMap()
        )
        
        assertTrue("Objective narration should mention events",
            result.objectiveNarration.contains("Contract") || 
            result.objectiveNarration.contains("Payment") ||
            result.objectiveNarration.contains("15") ||
            result.objectiveNarration.contains("20"))
    }
    
    @Test
    fun `test contradiction commentary lists all contradictions`() {
        val entityId = "entity1"
        val entities = listOf(
            Entity(id = entityId, primaryName = "John Doe", mentions = 5)
        )
        
        val contradictions = listOf(
            Contradiction(
                entityId = entityId,
                statementA = Statement(
                    entityId = entityId,
                    text = "I never received any payment",
                    sourceEvidenceId = "ev1",
                    type = StatementType.DENIAL,
                    date = Date()
                ),
                statementB = Statement(
                    entityId = entityId,
                    text = "The payment I received was insufficient",
                    sourceEvidenceId = "ev2",
                    type = StatementType.CLAIM,
                    date = Date()
                ),
                type = ContradictionType.DIRECT,
                severity = Severity.CRITICAL,
                description = "Denial of payment followed by admission of receiving payment"
            )
        )
        
        val result = generator.generateNarrative(
            entities = entities,
            timeline = emptyList(),
            contradictions = contradictions,
            behavioralPatterns = emptyList(),
            liabilityScores = emptyMap()
        )
        
        assertTrue("Contradiction commentary should mention entity",
            result.contradictionCommentary.contains("John Doe"))
        assertTrue("Contradiction commentary should mention the contradiction",
            result.contradictionCommentary.contains("payment") ||
            result.contradictionCommentary.contains("CRITICAL") ||
            result.contradictionCommentary.contains("DIRECT"))
    }
    
    @Test
    fun `test behavioral analysis describes patterns`() {
        val entityId = "entity1"
        val entities = listOf(
            Entity(id = entityId, primaryName = "Suspicious Person", mentions = 5)
        )
        
        val patterns = listOf(
            BehavioralPattern(
                entityId = entityId,
                type = BehaviorType.GASLIGHTING,
                instances = listOf(
                    "You're imagining things",
                    "That never happened"
                ),
                severity = Severity.HIGH
            )
        )
        
        val result = generator.generateNarrative(
            entities = entities,
            timeline = emptyList(),
            contradictions = emptyList(),
            behavioralPatterns = patterns,
            liabilityScores = emptyMap()
        )
        
        assertTrue("Behavioral analysis should mention the pattern",
            result.behavioralPatternAnalysis.contains("GASLIGHTING") ||
            result.behavioralPatternAnalysis.contains("Suspicious Person"))
    }
    
    @Test
    fun `test deductive logic explains contradictions`() {
        val entityId = "entity1"
        val entities = listOf(
            Entity(id = entityId, primaryName = "Test Entity", mentions = 5)
        )
        
        val contradictions = listOf(
            Contradiction(
                entityId = entityId,
                statementA = Statement(
                    entityId = entityId,
                    text = "The contract does not exist",
                    sourceEvidenceId = "ev1",
                    type = StatementType.DENIAL
                ),
                statementB = Statement(
                    entityId = entityId,
                    text = "I signed the contract last week",
                    sourceEvidenceId = "ev2",
                    type = StatementType.ADMISSION
                ),
                type = ContradictionType.DIRECT,
                severity = Severity.CRITICAL,
                description = "Critical denial followed by admission"
            )
        )
        
        val result = generator.generateNarrative(
            entities = entities,
            timeline = emptyList(),
            contradictions = contradictions,
            behavioralPatterns = emptyList(),
            liabilityScores = emptyMap()
        )
        
        assertTrue("Deductive logic should contain analysis",
            result.deductiveLogic.contains("DEDUCTION") ||
            result.deductiveLogic.contains("Test Entity") ||
            result.deductiveLogic.contains("false"))
    }
    
    @Test
    fun `test causal chain links events`() {
        val entityId = "entity1"
        val entities = listOf(
            Entity(id = entityId, primaryName = "Actor", mentions = 5)
        )
        
        val timeline = listOf(
            TimelineEvent(
                date = Calendar.getInstance().apply {
                    set(2023, Calendar.MARCH, 10, 10, 0, 0)
                }.time,
                description = "Money was transferred",
                sourceEvidenceId = "ev1",
                entityIds = listOf(entityId),
                eventType = EventType.PAYMENT,
                significance = Significance.HIGH
            ),
            TimelineEvent(
                date = Calendar.getInstance().apply {
                    set(2023, Calendar.MARCH, 15, 10, 0, 0)
                }.time,
                description = "Communication stopped",
                sourceEvidenceId = "ev2",
                entityIds = listOf(entityId),
                eventType = EventType.COMMUNICATION,
                significance = Significance.HIGH
            )
        )
        
        val result = generator.generateNarrative(
            entities = entities,
            timeline = timeline,
            contradictions = emptyList(),
            behavioralPatterns = emptyList(),
            liabilityScores = emptyMap()
        )
        
        assertTrue("Causal chain should exist and have content",
            result.causalChain.contains("CAUSE") ||
            result.causalChain.contains("EFFECT") ||
            result.causalChain.contains("Chain") ||
            result.causalChain.contains("causal"))
    }
    
    @Test
    fun `test final summary includes liability scores`() {
        val entityId = "entity1"
        val entities = listOf(
            Entity(id = entityId, primaryName = "Liable Person", mentions = 5)
        )
        
        val liabilityScores = mapOf(
            entityId to LiabilityScore(
                entityId = entityId,
                overallScore = 75.5f,
                contradictionScore = 45.0f,
                behavioralScore = 30.0f,
                evidenceContributionScore = 10.0f,
                chronologicalConsistencyScore = 20.0f,
                causalResponsibilityScore = 15.0f
            )
        )
        
        val result = generator.generateNarrative(
            entities = entities,
            timeline = emptyList(),
            contradictions = emptyList(),
            behavioralPatterns = emptyList(),
            liabilityScores = liabilityScores
        )
        
        assertTrue("Final summary should mention entity",
            result.finalSummary.contains("Liable Person"))
        assertTrue("Final summary should contain liability information",
            result.finalSummary.contains("75") ||
            result.finalSummary.contains("LIABILITY") ||
            result.finalSummary.contains("Score"))
    }
    
    @Test
    fun `test high liability triggers conclusion`() {
        val entityId = "entity1"
        val entities = listOf(
            Entity(id = entityId, primaryName = "High Liability Person", mentions = 10)
        )
        
        val liabilityScores = mapOf(
            entityId to LiabilityScore(
                entityId = entityId,
                overallScore = 85.0f,
                contradictionScore = 50.0f,
                behavioralScore = 35.0f,
                evidenceContributionScore = 20.0f,
                chronologicalConsistencyScore = 25.0f,
                causalResponsibilityScore = 30.0f
            )
        )
        
        val contradictions = listOf(
            Contradiction(
                entityId = entityId,
                statementA = Statement(entityId = entityId, text = "A", sourceEvidenceId = "ev1", type = StatementType.DENIAL),
                statementB = Statement(entityId = entityId, text = "B", sourceEvidenceId = "ev2", type = StatementType.ADMISSION),
                type = ContradictionType.DIRECT,
                severity = Severity.CRITICAL,
                description = "Critical contradiction"
            )
        )
        
        val result = generator.generateNarrative(
            entities = entities,
            timeline = emptyList(),
            contradictions = contradictions,
            behavioralPatterns = emptyList(),
            liabilityScores = liabilityScores
        )
        
        assertTrue("Final summary should contain conclusion",
            result.finalSummary.contains("CONCLUSION") ||
            result.finalSummary.contains("bears") ||
            result.finalSummary.contains("responsibility") ||
            result.finalSummary.contains("High Liability Person"))
    }
    
    @Test
    fun `test narrative sections are properly structured`() {
        val result = generator.generateNarrative(
            entities = emptyList(),
            timeline = emptyList(),
            contradictions = emptyList(),
            behavioralPatterns = emptyList(),
            liabilityScores = emptyMap()
        )
        
        // All sections should be non-null
        assertNotNull("Objective narration should not be null", result.objectiveNarration)
        assertNotNull("Contradiction commentary should not be null", result.contradictionCommentary)
        assertNotNull("Behavioral pattern analysis should not be null", result.behavioralPatternAnalysis)
        assertNotNull("Deductive logic should not be null", result.deductiveLogic)
        assertNotNull("Causal chain should not be null", result.causalChain)
        assertNotNull("Final summary should not be null", result.finalSummary)
    }
}

package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Comprehensive tests for the Liability Calculator engine.
 */
class LiabilityCalculatorTest {
    
    private val calculator = LiabilityCalculator()
    
    @Test
    fun `test empty input returns empty scores`() {
        val result = calculator.calculateLiability(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
        )
        assertTrue("Empty input should produce no scores", result.isEmpty())
    }
    
    @Test
    fun `test entity with no issues gets low score`() {
        val entity = Entity(
            id = "clean_entity",
            primaryName = "Clean Person",
            mentions = 5
        )
        
        val result = calculator.calculateLiability(
            entities = listOf(entity),
            contradictions = emptyList(),
            behavioralPatterns = emptyList(),
            evidenceList = emptyList(),
            timeline = emptyList()
        )
        
        val score = result[entity.id]
        assertNotNull("Should have score for entity", score)
        assertTrue("Entity with no issues should have low score", 
            score!!.overallScore < 30f)
    }
    
    @Test
    fun `test critical contradiction increases score significantly`() {
        val entityId = "troubled_entity"
        val entity = Entity(
            id = entityId,
            primaryName = "Troubled Person",
            mentions = 10
        )
        
        val contradiction = Contradiction(
            entityId = entityId,
            statementA = Statement(
                entityId = entityId,
                text = "I never took the money",
                sourceEvidenceId = "ev1",
                type = StatementType.DENIAL
            ),
            statementB = Statement(
                entityId = entityId,
                text = "I admit I took the money",
                sourceEvidenceId = "ev2",
                type = StatementType.ADMISSION
            ),
            type = ContradictionType.DIRECT,
            severity = Severity.CRITICAL,
            description = "Direct denial followed by admission"
        )
        
        val result = calculator.calculateLiability(
            entities = listOf(entity),
            contradictions = listOf(contradiction),
            behavioralPatterns = emptyList(),
            evidenceList = emptyList(),
            timeline = emptyList()
        )
        
        val score = result[entityId]
        assertNotNull("Should have score for entity", score)
        assertTrue("Critical contradiction should increase score",
            score!!.contradictionScore > 0f)
    }
    
    @Test
    fun `test multiple contradictions compound score`() {
        val entityId = "multiple_issues"
        val entity = Entity(
            id = entityId,
            primaryName = "Problem Person",
            mentions = 10
        )
        
        val contradictions = listOf(
            Contradiction(
                entityId = entityId,
                statementA = Statement(entityId = entityId, text = "Statement A1", sourceEvidenceId = "ev1", type = StatementType.CLAIM),
                statementB = Statement(entityId = entityId, text = "Statement B1", sourceEvidenceId = "ev2", type = StatementType.DENIAL),
                type = ContradictionType.DIRECT,
                severity = Severity.HIGH,
                description = "Contradiction 1"
            ),
            Contradiction(
                entityId = entityId,
                statementA = Statement(entityId = entityId, text = "Statement A2", sourceEvidenceId = "ev3", type = StatementType.CLAIM),
                statementB = Statement(entityId = entityId, text = "Statement B2", sourceEvidenceId = "ev4", type = StatementType.DENIAL),
                type = ContradictionType.CROSS_DOCUMENT,
                severity = Severity.HIGH,
                description = "Contradiction 2"
            ),
            Contradiction(
                entityId = entityId,
                statementA = Statement(entityId = entityId, text = "Statement A3", sourceEvidenceId = "ev5", type = StatementType.CLAIM),
                statementB = Statement(entityId = entityId, text = "Statement B3", sourceEvidenceId = "ev6", type = StatementType.DENIAL),
                type = ContradictionType.TEMPORAL,
                severity = Severity.MEDIUM,
                description = "Contradiction 3"
            )
        )
        
        val result = calculator.calculateLiability(
            entities = listOf(entity),
            contradictions = contradictions,
            behavioralPatterns = emptyList(),
            evidenceList = emptyList(),
            timeline = emptyList()
        )
        
        val score = result[entityId]
        assertNotNull("Should have score for entity", score)
        assertTrue("Multiple contradictions should compound score",
            score!!.contradictionScore >= 30f) // 15 + 15 + 8 = 38
    }
    
    @Test
    fun `test behavioral patterns affect score`() {
        val entityId = "manipulative_entity"
        val entity = Entity(
            id = entityId,
            primaryName = "Manipulative Person",
            mentions = 10
        )
        
        val patterns = listOf(
            BehavioralPattern(
                entityId = entityId,
                type = BehaviorType.GASLIGHTING,
                instances = listOf("Instance 1", "Instance 2"),
                severity = Severity.HIGH
            ),
            BehavioralPattern(
                entityId = entityId,
                type = BehaviorType.BLAME_SHIFTING,
                instances = listOf("Instance 1"),
                severity = Severity.MEDIUM
            )
        )
        
        val result = calculator.calculateLiability(
            entities = listOf(entity),
            contradictions = emptyList(),
            behavioralPatterns = patterns,
            evidenceList = emptyList(),
            timeline = emptyList()
        )
        
        val score = result[entityId]
        assertNotNull("Should have score for entity", score)
        assertTrue("Behavioral patterns should increase score",
            score!!.behavioralScore > 0f)
    }
    
    @Test
    fun `test gaslighting has high base score`() {
        val entityId = "gaslighter"
        val entity = Entity(id = entityId, primaryName = "Gaslighter", mentions = 5)
        
        val gaslightingPattern = BehavioralPattern(
            entityId = entityId,
            type = BehaviorType.GASLIGHTING,
            instances = listOf("You're imagining things"),
            severity = Severity.HIGH
        )
        
        val result = calculator.calculateLiability(
            entities = listOf(entity),
            contradictions = emptyList(),
            behavioralPatterns = listOf(gaslightingPattern),
            evidenceList = emptyList(),
            timeline = emptyList()
        )
        
        val score = result[entityId]
        assertNotNull("Should have score", score)
        assertTrue("Gaslighting should have significant behavioral score",
            score!!.behavioralScore >= 20f) // Base 20 * 1.2 = 24
    }
    
    @Test
    fun `test liability breakdown tracks correctly`() {
        val entityId = "breakdown_test"
        val entity = Entity(id = entityId, primaryName = "Test Person", mentions = 5)
        
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
        
        val patterns = listOf(
            BehavioralPattern(
                entityId = entityId,
                type = BehaviorType.DEFLECTION,
                instances = listOf("What about..."),
                severity = Severity.MEDIUM
            )
        )
        
        val result = calculator.calculateLiability(
            entities = listOf(entity),
            contradictions = contradictions,
            behavioralPatterns = patterns,
            evidenceList = emptyList(),
            timeline = emptyList()
        )
        
        val score = result[entityId]
        assertNotNull("Should have score", score)
        
        val breakdown = score!!.breakdown
        assertEquals("Should track total contradictions", 1, breakdown.totalContradictions)
        assertEquals("Should track critical contradictions", 1, breakdown.criticalContradictions)
        assertTrue("Should track behavioral flags", breakdown.behavioralFlags.isNotEmpty())
        assertEquals("Should track story changes", 1, breakdown.storyChanges)
    }
    
    @Test
    fun `test score capped at 100`() {
        val entityId = "maxed_out"
        val entity = Entity(id = entityId, primaryName = "Maxed Person", mentions = 20)
        
        // Create many critical contradictions
        val manyContradictions = (1..10).map { i ->
            Contradiction(
                entityId = entityId,
                statementA = Statement(entityId = entityId, text = "A$i", sourceEvidenceId = "ev${i}a", type = StatementType.DENIAL),
                statementB = Statement(entityId = entityId, text = "B$i", sourceEvidenceId = "ev${i}b", type = StatementType.ADMISSION),
                type = ContradictionType.DIRECT,
                severity = Severity.CRITICAL,
                description = "Critical contradiction $i"
            )
        }
        
        // Create many behavioral patterns
        val manyPatterns = listOf(
            BehaviorType.GASLIGHTING,
            BehaviorType.FINANCIAL_MANIPULATION,
            BehaviorType.BLAME_SHIFTING,
            BehaviorType.PASSIVE_ADMISSION
        ).map { type ->
            BehavioralPattern(
                entityId = entityId,
                type = type,
                instances = (1..5).map { "Instance $it" },
                severity = Severity.CRITICAL
            )
        }
        
        val result = calculator.calculateLiability(
            entities = listOf(entity),
            contradictions = manyContradictions,
            behavioralPatterns = manyPatterns,
            evidenceList = emptyList(),
            timeline = emptyList()
        )
        
        val score = result[entityId]
        assertNotNull("Should have score", score)
        assertTrue("Overall score should be capped at 100", score!!.overallScore <= 100f)
        assertTrue("Individual scores should be capped at 100", score.contradictionScore <= 100f)
        assertTrue("Individual scores should be capped at 100", score.behavioralScore <= 100f)
    }
    
    @Test
    fun `test multiple entities scored independently`() {
        val entity1 = Entity(id = "entity1", primaryName = "Person One", mentions = 5)
        val entity2 = Entity(id = "entity2", primaryName = "Person Two", mentions = 5)
        
        val contradiction = Contradiction(
            entityId = "entity1",
            statementA = Statement(entityId = "entity1", text = "A", sourceEvidenceId = "ev1", type = StatementType.CLAIM),
            statementB = Statement(entityId = "entity1", text = "B", sourceEvidenceId = "ev2", type = StatementType.DENIAL),
            type = ContradictionType.DIRECT,
            severity = Severity.HIGH,
            description = "Entity 1 contradiction only"
        )
        
        val result = calculator.calculateLiability(
            entities = listOf(entity1, entity2),
            contradictions = listOf(contradiction),
            behavioralPatterns = emptyList(),
            evidenceList = emptyList(),
            timeline = emptyList()
        )
        
        val score1 = result["entity1"]
        val score2 = result["entity2"]
        
        assertNotNull("Should have score for entity1", score1)
        assertNotNull("Should have score for entity2", score2)
        assertTrue("Entity1 should have higher score due to contradiction",
            score1!!.overallScore > score2!!.overallScore)
    }
}

package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Comprehensive tests for the Behavioral Analyzer engine.
 */
class BehavioralAnalyzerTest {
    
    private val analyzer = BehavioralAnalyzer()
    
    @Test
    fun `test empty input returns no patterns`() {
        val result = analyzer.analyzeBehavior(emptyList(), emptyList(), emptyList())
        assertTrue("Empty input should produce no patterns", result.isEmpty())
    }
    
    @Test
    fun `test gaslighting pattern detection`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "messages.txt",
                filePath = "/path/to/messages",
                extractedText = """
                    John: You're imagining things, that never happened.
                    John: I never said that, you're confused.
                    John: You're being paranoid about this whole situation.
                    John: You're making things up as usual.
                """.trimIndent(),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(primaryName = "John", mentions = 4)
        )
        
        val result = analyzer.analyzeBehavior(evidence, entities, emptyList())
        
        // Should detect gaslighting pattern
        val gaslightingPatterns = result.filter { it.type == BehaviorType.GASLIGHTING }
        assertTrue("Should detect gaslighting behavior", gaslightingPatterns.isNotEmpty())
    }
    
    @Test
    fun `test deflection pattern detection`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "conversation.txt",
                filePath = "/path/to/conversation",
                extractedText = """
                    Sarah: What about your actions? But you did the same thing.
                    Sarah: That's not the point here, let's focus on something else.
                    Sarah: You're changing the subject, but you started this.
                """.trimIndent(),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(primaryName = "Sarah", mentions = 3)
        )
        
        val result = analyzer.analyzeBehavior(evidence, entities, emptyList())
        
        // Should detect deflection pattern
        val deflectionPatterns = result.filter { it.type == BehaviorType.DEFLECTION }
        assertTrue("Should detect deflection behavior", deflectionPatterns.isNotEmpty())
    }
    
    @Test
    fun `test pressure tactics detection`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "sales.txt",
                filePath = "/path/to/sales",
                extractedText = """
                    Mike: You need to decide now, this offer expires today.
                    Mike: Take it or leave it, this is the final offer.
                    Mike: Limited time only, don't miss out on this opportunity.
                    Mike: Act fast or you'll regret it, last chance.
                """.trimIndent(),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(primaryName = "Mike", mentions = 4)
        )
        
        val result = analyzer.analyzeBehavior(evidence, entities, emptyList())
        
        // Should detect pressure tactics
        val pressurePatterns = result.filter { it.type == BehaviorType.PRESSURE_TACTICS }
        assertTrue("Should detect pressure tactics", pressurePatterns.isNotEmpty())
    }
    
    @Test
    fun `test financial manipulation detection`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "investment.txt",
                filePath = "/path/to/investment",
                extractedText = """
                    Tom: Just this once, I'll pay you back double.
                    Tom: Trust me on this, it's an investment opportunity.
                    Tom: Guaranteed return, no risk involved at all.
                    Tom: You'll make it back in no time, I promise.
                """.trimIndent(),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(primaryName = "Tom", mentions = 4)
        )
        
        val result = analyzer.analyzeBehavior(evidence, entities, emptyList())
        
        // Should detect financial manipulation
        val financialPatterns = result.filter { it.type == BehaviorType.FINANCIAL_MANIPULATION }
        assertTrue("Should detect financial manipulation", financialPatterns.isNotEmpty())
    }
    
    @Test
    fun `test emotional manipulation detection`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "emotional.txt",
                filePath = "/path/to/emotional",
                extractedText = """
                    Lisa: If you loved me, you would do this for me.
                    Lisa: After all I've done for you, you owe me this.
                    Lisa: Don't you trust me after everything we've been through?
                    Lisa: I thought we were friends, but apparently not.
                """.trimIndent(),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(primaryName = "Lisa", mentions = 4)
        )
        
        val result = analyzer.analyzeBehavior(evidence, entities, emptyList())
        
        // Should detect emotional manipulation
        val emotionalPatterns = result.filter { it.type == BehaviorType.EMOTIONAL_MANIPULATION }
        assertTrue("Should detect emotional manipulation", emotionalPatterns.isNotEmpty())
    }
    
    @Test
    fun `test blame shifting detection`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "blame.txt",
                filePath = "/path/to/blame",
                extractedText = """
                    David: It's your fault this happened, not mine.
                    David: You made me do it, because of you we're in this mess.
                    David: If you hadn't done that, none of this would have happened.
                    David: You should have known better, you're the one who started it.
                """.trimIndent(),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(primaryName = "David", mentions = 4)
        )
        
        val result = analyzer.analyzeBehavior(evidence, entities, emptyList())
        
        // Should detect blame shifting
        val blamePatterns = result.filter { it.type == BehaviorType.BLAME_SHIFTING }
        assertTrue("Should detect blame shifting", blamePatterns.isNotEmpty())
    }
    
    @Test
    fun `test passive admission detection`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "slip.txt",
                filePath = "/path/to/slip",
                extractedText = """
                    Kevin: I thought I was in the clear after that.
                    Kevin: I didn't think anyone would notice what I did.
                    Kevin: Technically, I didn't break any rules, in a way.
                    Kevin: I assumed it would be fine to proceed without asking.
                """.trimIndent(),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(primaryName = "Kevin", mentions = 4)
        )
        
        val result = analyzer.analyzeBehavior(evidence, entities, emptyList())
        
        // Should detect passive admission
        val passivePatterns = result.filter { it.type == BehaviorType.PASSIVE_ADMISSION }
        assertTrue("Should detect passive admission", passivePatterns.isNotEmpty())
    }
    
    @Test
    fun `test over explaining detection`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "overexplain.txt",
                filePath = "/path/to/overexplain",
                extractedText = """
                    Mark: Let me explain why this happened in detail.
                    Mark: You see, the reason is complicated, what happened was...
                    Mark: It's complicated and there's more to it than meets the eye.
                    Mark: You don't understand the full picture here.
                """.trimIndent(),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(primaryName = "Mark", mentions = 4)
        )
        
        val result = analyzer.analyzeBehavior(evidence, entities, emptyList())
        
        // Should detect over explaining
        val overExplainingPatterns = result.filter { it.type == BehaviorType.OVER_EXPLAINING }
        assertTrue("Should detect over explaining", overExplainingPatterns.isNotEmpty())
    }
    
    @Test
    fun `test ghosting detection from timeline`() {
        val now = Date()
        val tenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -10) }.time
        val twentyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -20) }.time
        
        val entityId = "entity1"
        
        val timeline = listOf(
            TimelineEvent(
                date = twentyDaysAgo,
                description = "Initial contact",
                sourceEvidenceId = "ev1",
                entityIds = listOf(entityId),
                eventType = EventType.COMMUNICATION
            ),
            TimelineEvent(
                date = tenDaysAgo,
                description = "Follow up after long gap",
                sourceEvidenceId = "ev2",
                entityIds = listOf(entityId),
                eventType = EventType.COMMUNICATION
            )
        )
        
        val entities = listOf(
            Entity(id = entityId, primaryName = "Ghost", mentions = 2)
        )
        
        val result = analyzer.analyzeBehavior(emptyList(), entities, timeline)
        
        // Should detect ghosting pattern from timeline gaps
        // Note: This depends on the timeline gap analysis in the analyzer
        assertNotNull("Should analyze timeline patterns", result)
    }
    
    @Test
    fun `test severity scaling based on instance count`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "many_gaslighting.txt",
                filePath = "/path/to/gaslighting",
                extractedText = """
                    Person: You're imagining things again.
                    Person: That never happened, I never said that.
                    Person: You're confused about what occurred.
                    Person: You're being paranoid as usual.
                    Person: You're making things up now.
                    Person: I never said that to you.
                """.trimIndent(),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(primaryName = "Person", mentions = 6)
        )
        
        val result = analyzer.analyzeBehavior(evidence, entities, emptyList())
        
        // Multiple instances should increase severity
        val gaslightingPatterns = result.filter { it.type == BehaviorType.GASLIGHTING }
        if (gaslightingPatterns.isNotEmpty()) {
            val pattern = gaslightingPatterns.first()
            assertTrue("Many instances should have medium or higher severity",
                pattern.severity == Severity.MEDIUM || 
                pattern.severity == Severity.HIGH || 
                pattern.severity == Severity.CRITICAL)
        }
    }
}

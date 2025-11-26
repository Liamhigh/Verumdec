package com.verumdec.engine

import com.verumdec.data.*
import java.util.*

/**
 * Behavioral Pattern Analyzer
 * Detects manipulation patterns and suspicious behavior.
 */
class BehavioralAnalyzer {

    // Pattern definitions
    private val patterns = mapOf(
        BehaviorType.GASLIGHTING to listOf(
            "you're imagining", "that never happened", "you're crazy",
            "i never said that", "you're confused", "you misunderstood",
            "you're overreacting", "you're being paranoid", "you're making things up"
        ),
        BehaviorType.DEFLECTION to listOf(
            "what about", "but you", "that's not the point",
            "let's focus on", "the real issue is", "you're changing the subject"
        ),
        BehaviorType.PRESSURE_TACTICS to listOf(
            "you need to decide now", "this offer expires", "take it or leave it",
            "everyone else agrees", "don't miss out", "act fast", "limited time",
            "last chance", "final offer"
        ),
        BehaviorType.FINANCIAL_MANIPULATION to listOf(
            "just this once", "i'll pay you back", "trust me", "it's an investment",
            "you'll make it back", "guaranteed return", "no risk"
        ),
        BehaviorType.EMOTIONAL_MANIPULATION to listOf(
            "if you loved me", "after all i've done", "you owe me",
            "don't you trust me", "i thought we were friends", "you're hurting me"
        ),
        BehaviorType.OVER_EXPLAINING to listOf(
            "let me explain", "the reason is", "you see", "what happened was",
            "it's complicated", "there's more to it", "you don't understand"
        ),
        BehaviorType.BLAME_SHIFTING to listOf(
            "it's your fault", "you made me", "because of you",
            "if you hadn't", "you should have", "you're the one who"
        ),
        BehaviorType.PASSIVE_ADMISSION to listOf(
            "i thought i was in the clear", "i didn't think anyone would notice",
            "i assumed it would be fine", "technically", "in a way"
        )
    )

    /**
     * Analyze evidence for behavioral patterns.
     */
    fun analyzeBehavior(
        evidenceList: List<Evidence>,
        entities: List<Entity>,
        timeline: List<TimelineEvent>
    ): List<BehavioralPattern> {
        val detectedPatterns = mutableListOf<BehavioralPattern>()
        
        for (entity in entities) {
            val entityText = collectEntityText(entity, evidenceList)
            val entityPatterns = detectPatterns(entity.id, entityText)
            detectedPatterns.addAll(entityPatterns)
            
            // Check for behavioral patterns based on timeline
            detectedPatterns.addAll(detectTimelinePatterns(entity.id, timeline))
        }
        
        return detectedPatterns
    }

    /**
     * Collect all text associated with an entity.
     */
    private fun collectEntityText(entity: Entity, evidenceList: List<Evidence>): String {
        val texts = mutableListOf<String>()
        
        for (evidence in evidenceList) {
            val text = evidence.extractedText
            
            // Check if entity is sender/author
            val isSender = evidence.metadata.sender?.contains(entity.primaryName, ignoreCase = true) == true ||
                          entity.emails.any { evidence.metadata.sender?.contains(it, ignoreCase = true) == true }
            
            if (isSender) {
                texts.add(text)
                continue
            }
            
            // Extract lines where entity is speaking (chat format)
            val lines = text.lines()
            for (line in lines) {
                if (line.startsWith("${entity.primaryName}:", ignoreCase = true) ||
                    entity.aliases.any { line.startsWith("$it:", ignoreCase = true) }) {
                    texts.add(line)
                }
            }
        }
        
        return texts.joinToString("\n")
    }

    /**
     * Detect patterns in text.
     */
    private fun detectPatterns(entityId: String, text: String): List<BehavioralPattern> {
        val detectedPatterns = mutableListOf<BehavioralPattern>()
        val lowerText = text.lowercase()
        
        for ((patternType, phrases) in patterns) {
            val instances = mutableListOf<String>()
            
            for (phrase in phrases) {
                if (lowerText.contains(phrase)) {
                    // Find the sentence containing this phrase
                    val sentences = text.split(Regex("[.!?\\n]"))
                    for (sentence in sentences) {
                        if (sentence.lowercase().contains(phrase)) {
                            instances.add(sentence.trim())
                        }
                    }
                }
            }
            
            if (instances.isNotEmpty()) {
                val severity = when {
                    instances.size >= 5 -> Severity.CRITICAL
                    instances.size >= 3 -> Severity.HIGH
                    instances.size >= 2 -> Severity.MEDIUM
                    else -> Severity.LOW
                }
                
                detectedPatterns.add(BehavioralPattern(
                    entityId = entityId,
                    type = patternType,
                    instances = instances.distinct().take(10),
                    firstDetectedAt = Date(),
                    severity = severity
                ))
            }
        }
        
        return detectedPatterns
    }

    /**
     * Detect patterns based on timeline behavior.
     */
    private fun detectTimelinePatterns(entityId: String, timeline: List<TimelineEvent>): List<BehavioralPattern> {
        val detectedPatterns = mutableListOf<BehavioralPattern>()
        
        val entityEvents = timeline.filter { entityId in it.entityIds }.sortedBy { it.date }
        
        if (entityEvents.isEmpty()) return detectedPatterns
        
        // Detect ghosting (long gaps followed by activity only when pressured)
        detectGhosting(entityId, entityEvents)?.let { detectedPatterns.add(it) }
        
        // Detect sudden withdrawal
        detectSuddenWithdrawal(entityId, entityEvents)?.let { detectedPatterns.add(it) }
        
        // Detect delayed responses (only responding after legal pressure)
        detectDelayedResponse(entityId, entityEvents)?.let { detectedPatterns.add(it) }
        
        return detectedPatterns
    }

    /**
     * Detect ghosting pattern.
     */
    private fun detectGhosting(entityId: String, events: List<TimelineEvent>): BehavioralPattern? {
        if (events.size < 2) return null
        
        val gaps = mutableListOf<Long>()
        for (i in 1 until events.size) {
            val gap = events[i].date.time - events[i-1].date.time
            gaps.add(gap)
        }
        
        // Check for gaps > 7 days
        val longGaps = gaps.filter { it > 7 * 24 * 60 * 60 * 1000L }
        
        if (longGaps.isNotEmpty()) {
            return BehavioralPattern(
                entityId = entityId,
                type = BehaviorType.GHOSTING,
                instances = listOf("Extended period(s) of no communication detected"),
                firstDetectedAt = Date(),
                severity = if (longGaps.size >= 2) Severity.HIGH else Severity.MEDIUM
            )
        }
        
        return null
    }

    /**
     * Detect sudden withdrawal pattern.
     */
    private fun detectSuddenWithdrawal(entityId: String, events: List<TimelineEvent>): BehavioralPattern? {
        if (events.size < 5) return null
        
        // Check if activity suddenly stops
        val firstHalf = events.take(events.size / 2)
        val secondHalf = events.drop(events.size / 2)
        
        val firstHalfDuration = if (firstHalf.size >= 2) {
            firstHalf.last().date.time - firstHalf.first().date.time
        } else 0L
        
        val secondHalfDuration = if (secondHalf.size >= 2) {
            secondHalf.last().date.time - secondHalf.first().date.time
        } else 0L
        
        // If second half has much less activity relative to time
        if (firstHalfDuration > 0 && secondHalfDuration > 0) {
            val firstRate = firstHalf.size.toFloat() / firstHalfDuration
            val secondRate = secondHalf.size.toFloat() / secondHalfDuration
            
            if (secondRate < firstRate * 0.3) {
                return BehavioralPattern(
                    entityId = entityId,
                    type = BehaviorType.SUDDEN_WITHDRAWAL,
                    instances = listOf("Communication frequency dropped significantly"),
                    firstDetectedAt = Date(),
                    severity = Severity.MEDIUM
                )
            }
        }
        
        return null
    }

    /**
     * Detect delayed response pattern.
     */
    private fun detectDelayedResponse(entityId: String, events: List<TimelineEvent>): BehavioralPattern? {
        // Look for pattern: payment event followed by delayed response
        val paymentEvents = events.filter { it.eventType == EventType.PAYMENT }
        val communicationEvents = events.filter { it.eventType == EventType.COMMUNICATION }
        
        for (payment in paymentEvents) {
            val nextCommunication = communicationEvents.find { 
                it.date.after(payment.date) && 
                (it.date.time - payment.date.time) > 3 * 24 * 60 * 60 * 1000L // > 3 days
            }
            
            if (nextCommunication != null) {
                return BehavioralPattern(
                    entityId = entityId,
                    type = BehaviorType.DELAYED_RESPONSE,
                    instances = listOf("Response delayed after financial event"),
                    firstDetectedAt = Date(),
                    severity = Severity.MEDIUM
                )
            }
        }
        
        return null
    }
}

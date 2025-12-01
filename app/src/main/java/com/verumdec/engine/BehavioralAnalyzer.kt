package com.verumdec.engine

import com.verumdec.data.*
import java.util.*

/**
 * Behavioral Pattern Analyzer (Gold Standard Implementation)
 * Detects manipulation patterns, suspicious behavior, and stress markers.
 * 
 * Based on the Verum Omnis forensic doctrine:
 * - Detects behavioral red flags across 12+ pattern types
 * - Identifies stress markers and defensive language spikes
 * - Flags avoidance patterns and slip-up admissions
 * - Supports multi-pass behavioral contradiction detection
 */
class BehavioralAnalyzer {

    // Pattern definitions - Enhanced with gold standard forensic patterns
    private val patterns = mapOf(
        BehaviorType.GASLIGHTING to listOf(
            "you're imagining", "that never happened", "you're crazy",
            "i never said that", "you're confused", "you misunderstood",
            "you're overreacting", "you're being paranoid", "you're making things up",
            "you're too sensitive", "you're remembering it wrong", "that's not what happened"
        ),
        BehaviorType.DEFLECTION to listOf(
            "what about", "but you", "that's not the point",
            "let's focus on", "the real issue is", "you're changing the subject",
            "why are you asking", "that's irrelevant", "we should discuss",
            "more importantly", "the bigger picture"
        ),
        BehaviorType.PRESSURE_TACTICS to listOf(
            "you need to decide now", "this offer expires", "take it or leave it",
            "everyone else agrees", "don't miss out", "act fast", "limited time",
            "last chance", "final offer", "time is running out", "now or never",
            "you're running out of time", "deadline approaching"
        ),
        BehaviorType.FINANCIAL_MANIPULATION to listOf(
            "just this once", "i'll pay you back", "trust me", "it's an investment",
            "you'll make it back", "guaranteed return", "no risk",
            "you won't regret it", "easy money", "quick return",
            "special opportunity", "insider deal", "can't lose"
        ),
        BehaviorType.EMOTIONAL_MANIPULATION to listOf(
            "if you loved me", "after all i've done", "you owe me",
            "don't you trust me", "i thought we were friends", "you're hurting me",
            "how could you", "i'm disappointed", "you've let me down",
            "i gave you everything", "you're being selfish"
        ),
        BehaviorType.OVER_EXPLAINING to listOf(
            "let me explain", "the reason is", "you see", "what happened was",
            "it's complicated", "there's more to it", "you don't understand",
            "to be clear", "allow me to clarify", "what i meant was",
            "the thing is", "basically", "essentially what happened"
        ),
        BehaviorType.BLAME_SHIFTING to listOf(
            "it's your fault", "you made me", "because of you",
            "if you hadn't", "you should have", "you're the one who",
            "they made me", "i had no choice", "i was forced to",
            "blame them", "it wasn't my decision", "i was just following"
        ),
        BehaviorType.PASSIVE_ADMISSION to listOf(
            "i thought i was in the clear", "i didn't think anyone would notice",
            "i assumed it would be fine", "technically", "in a way",
            "i suppose", "sort of", "kind of", "more or less",
            "i guess so", "if you put it that way"
        ),
        BehaviorType.SLIP_UP_ADMISSION to listOf(
            "i mean, i didn't", "well, technically", "okay fine",
            "alright, maybe i", "i might have", "perhaps i did",
            "okay, i admit", "fine, you caught me", "yes, but"
        )
    )
    
    // Stress markers for behavioral analysis (Gold Standard)
    private val stressMarkers = listOf(
        "i don't remember", "i can't recall", "i don't know",
        "i'm not sure", "maybe", "perhaps", "possibly",
        "i think", "i believe", "as far as i know",
        "to the best of my knowledge", "i may have", "i might have"
    )
    
    // Defensive language indicators
    private val defensiveLanguage = listOf(
        "why are you asking", "that's not relevant", "i refuse to answer",
        "i don't have to explain", "none of your business", "who told you that",
        "where did you hear that", "that's a lie", "absolutely not",
        "how dare you", "i would never", "that's ridiculous"
    )
    
    // Avoidance patterns
    private val avoidancePatterns = listOf(
        "i don't recall", "i can't remember", "it was a long time ago",
        "i'm not the right person to ask", "you should ask someone else",
        "i wasn't involved", "that wasn't my responsibility",
        "i wasn't there", "i don't have that information"
    )

    /**
     * Analyze evidence for behavioral patterns (Gold Standard Implementation).
     * 
     * This performs multi-pass behavioral analysis:
     * - Pass 1: Pattern detection in text
     * - Pass 2: Stress marker and defensive language detection  
     * - Pass 3: Timeline-based behavioral patterns
     * - Pass 4: Avoidance and slip-up detection
     */
    fun analyzeBehavior(
        evidenceList: List<Evidence>,
        entities: List<Entity>,
        timeline: List<TimelineEvent>
    ): List<BehavioralPattern> {
        val detectedPatterns = mutableListOf<BehavioralPattern>()
        
        for (entity in entities) {
            val entityText = collectEntityText(entity, evidenceList)
            
            // Pass 1: Standard pattern detection
            val entityPatterns = detectPatterns(entity.id, entityText)
            detectedPatterns.addAll(entityPatterns)
            
            // Pass 2: Stress markers and defensive language detection
            val stressPatterns = detectStressMarkers(entity.id, entityText)
            detectedPatterns.addAll(stressPatterns)
            
            // Pass 3: Avoidance pattern detection
            val avoidanceDetected = detectAvoidancePatterns(entity.id, entityText)
            detectedPatterns.addAll(avoidanceDetected)
            
            // Pass 4: Timeline-based behavioral patterns
            detectedPatterns.addAll(detectTimelinePatterns(entity.id, timeline))
        }
        
        return detectedPatterns
    }
    
    /**
     * Detect stress markers in text (Gold Standard BehaviourBrain feature).
     * Stress markers indicate potential deception or uncertainty.
     */
    private fun detectStressMarkers(entityId: String, text: String): List<BehavioralPattern> {
        val detectedPatterns = mutableListOf<BehavioralPattern>()
        val lowerText = text.lowercase()
        
        // Detect stress markers
        val stressInstances = mutableListOf<String>()
        for (marker in stressMarkers) {
            if (lowerText.contains(marker)) {
                val sentences = text.split(Regex("[.!?\\n]"))
                for (sentence in sentences) {
                    if (sentence.lowercase().contains(marker)) {
                        stressInstances.add(sentence.trim())
                    }
                }
            }
        }
        
        if (stressInstances.size >= 2) {
            detectedPatterns.add(BehavioralPattern(
                entityId = entityId,
                type = BehaviorType.DEFLECTION, // Use deflection as closest match
                instances = stressInstances.distinct().take(10),
                firstDetectedAt = Date(),
                severity = when {
                    stressInstances.size >= 5 -> Severity.HIGH
                    stressInstances.size >= 3 -> Severity.MEDIUM
                    else -> Severity.LOW
                }
            ))
        }
        
        // Detect defensive language spikes
        val defensiveInstances = mutableListOf<String>()
        for (phrase in defensiveLanguage) {
            if (lowerText.contains(phrase)) {
                val sentences = text.split(Regex("[.!?\\n]"))
                for (sentence in sentences) {
                    if (sentence.lowercase().contains(phrase)) {
                        defensiveInstances.add(sentence.trim())
                    }
                }
            }
        }
        
        if (defensiveInstances.isNotEmpty()) {
            detectedPatterns.add(BehavioralPattern(
                entityId = entityId,
                type = BehaviorType.BLAME_SHIFTING, // Defensive language often precedes blame shifting
                instances = defensiveInstances.distinct().take(10),
                firstDetectedAt = Date(),
                severity = when {
                    defensiveInstances.size >= 3 -> Severity.HIGH
                    defensiveInstances.size >= 2 -> Severity.MEDIUM
                    else -> Severity.LOW
                }
            ))
        }
        
        return detectedPatterns
    }
    
    /**
     * Detect avoidance patterns in text.
     * Avoidance patterns indicate potential concealment or reluctance to provide information.
     */
    private fun detectAvoidancePatterns(entityId: String, text: String): List<BehavioralPattern> {
        val detectedPatterns = mutableListOf<BehavioralPattern>()
        val lowerText = text.lowercase()
        
        val avoidanceInstances = mutableListOf<String>()
        for (pattern in avoidancePatterns) {
            if (lowerText.contains(pattern)) {
                val sentences = text.split(Regex("[.!?\\n]"))
                for (sentence in sentences) {
                    if (sentence.lowercase().contains(pattern)) {
                        avoidanceInstances.add(sentence.trim())
                    }
                }
            }
        }
        
        if (avoidanceInstances.size >= 2) {
            detectedPatterns.add(BehavioralPattern(
                entityId = entityId,
                type = BehaviorType.GHOSTING, // Avoidance is similar to ghosting behavior
                instances = avoidanceInstances.distinct().take(10),
                firstDetectedAt = Date(),
                severity = when {
                    avoidanceInstances.size >= 4 -> Severity.HIGH
                    avoidanceInstances.size >= 2 -> Severity.MEDIUM
                    else -> Severity.LOW
                }
            ))
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

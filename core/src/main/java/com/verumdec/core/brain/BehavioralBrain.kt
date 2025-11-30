package com.verumdec.core.brain

import android.content.Context
import com.verumdec.core.util.HashUtils
import java.util.*

/**
 * BehavioralBrain - Analyzes behavioral patterns in communications.
 * Detects manipulation, deception indicators, and psychological patterns.
 */
class BehavioralBrain(context: Context) : BaseBrain(context) {

    override val brainName = "BehavioralBrain"

    // Pattern definitions for behavioral indicators
    private val behaviorPatterns = mapOf(
        BehaviorPatternType.GASLIGHTING to listOf(
            "you're imagining", "that never happened", "you're crazy",
            "i never said that", "you're confused", "you misunderstood",
            "you're overreacting", "you're being paranoid", "you're making things up",
            "you must have dreamed that", "you're too sensitive"
        ),
        BehaviorPatternType.DEFLECTION to listOf(
            "what about", "but you", "that's not the point",
            "let's focus on", "the real issue is", "you're changing the subject",
            "that's irrelevant", "stop deflecting"
        ),
        BehaviorPatternType.PRESSURE_TACTICS to listOf(
            "you need to decide now", "this offer expires", "take it or leave it",
            "everyone else agrees", "don't miss out", "act fast", "limited time",
            "last chance", "final offer", "now or never"
        ),
        BehaviorPatternType.FINANCIAL_MANIPULATION to listOf(
            "just this once", "i'll pay you back", "trust me", "it's an investment",
            "you'll make it back", "guaranteed return", "no risk", "easy money",
            "just lend me", "i promise to repay"
        ),
        BehaviorPatternType.EMOTIONAL_MANIPULATION to listOf(
            "if you loved me", "after all i've done", "you owe me",
            "don't you trust me", "i thought we were friends", "you're hurting me",
            "you're being selfish", "i can't believe you would", "how could you"
        ),
        BehaviorPatternType.OVER_EXPLAINING to listOf(
            "let me explain", "the reason is", "you see", "what happened was",
            "it's complicated", "there's more to it", "you don't understand",
            "if you just listen", "the truth is", "to be perfectly clear"
        ),
        BehaviorPatternType.BLAME_SHIFTING to listOf(
            "it's your fault", "you made me", "because of you",
            "if you hadn't", "you should have", "you're the one who",
            "this is on you", "you caused this", "you brought this on yourself"
        ),
        BehaviorPatternType.PASSIVE_ADMISSION to listOf(
            "i thought i was in the clear", "i didn't think anyone would notice",
            "i assumed it would be fine", "technically", "in a way",
            "not exactly", "sort of", "kind of", "more or less"
        ),
        BehaviorPatternType.MINIMIZATION to listOf(
            "it's not a big deal", "you're overreacting", "it was just",
            "only a small", "barely anything", "hardly matters",
            "what's the big deal", "relax", "calm down"
        ),
        BehaviorPatternType.THREATENING to listOf(
            "you'll regret", "i'll make sure", "you'll see what happens",
            "don't test me", "i'm warning you", "you don't want to",
            "remember i know", "be careful", "watch yourself"
        )
    )

    /**
     * Analyze text for behavioral patterns.
     */
    fun analyze(content: String, entityId: String): BrainResult<BehavioralAnalysis> {
        val metadata = mapOf(
            "entityId" to entityId,
            "sha512Hash" to HashUtils.sha512(content),
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(content, "ANALYZE_BEHAVIOR", metadata) { text ->
            val detectedPatterns = detectPatterns(text, entityId)
            val riskScore = calculateRiskScore(detectedPatterns)
            val primaryConcerns = identifyPrimaryConcerns(detectedPatterns)
            
            BehavioralAnalysis(
                id = generateProcessingId(),
                entityId = entityId,
                patterns = detectedPatterns,
                patternCount = detectedPatterns.size,
                riskScore = riskScore,
                riskLevel = classifyRiskLevel(riskScore),
                primaryConcerns = primaryConcerns,
                manipulationIndicatorCount = countManipulationIndicators(detectedPatterns),
                deceptionIndicatorCount = countDeceptionIndicators(detectedPatterns),
                analyzedAt = Date()
            )
        }
    }

    /**
     * Compare behavioral patterns between two entities.
     */
    fun compareEntities(
        analysis1: BehavioralAnalysis,
        analysis2: BehavioralAnalysis
    ): BrainResult<BehavioralComparison> {
        val metadata = mapOf(
            "entity1Id" to analysis1.entityId,
            "entity2Id" to analysis2.entityId,
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(Pair(analysis1, analysis2), "COMPARE_BEHAVIOR", metadata) { (a1, a2) ->
            val entity1Patterns = a1.patterns.map { it.type }.toSet()
            val entity2Patterns = a2.patterns.map { it.type }.toSet()
            
            BehavioralComparison(
                id = generateProcessingId(),
                entity1Id = a1.entityId,
                entity2Id = a2.entityId,
                entity1RiskScore = a1.riskScore,
                entity2RiskScore = a2.riskScore,
                sharedPatterns = entity1Patterns.intersect(entity2Patterns).toList(),
                uniqueToEntity1 = (entity1Patterns - entity2Patterns).toList(),
                uniqueToEntity2 = (entity2Patterns - entity1Patterns).toList(),
                higherRiskEntity = if (a1.riskScore > a2.riskScore) a1.entityId else a2.entityId,
                riskDifferential = kotlin.math.abs(a1.riskScore - a2.riskScore),
                comparedAt = Date()
            )
        }
    }

    /**
     * Analyze communication timeline for behavioral shifts.
     */
    fun analyzeTimeline(
        communications: List<TimedCommunication>,
        entityId: String
    ): BrainResult<BehavioralTimelineAnalysis> {
        val metadata = mapOf(
            "entityId" to entityId,
            "communicationCount" to communications.size,
            "evidenceId" to generateProcessingId()
        )

        return processWithEnforcement(communications, "ANALYZE_TIMELINE", metadata) { comms ->
            val sortedComms = comms.sortedBy { it.timestamp }
            val shifts = detectBehavioralShifts(sortedComms, entityId)
            
            BehavioralTimelineAnalysis(
                id = generateProcessingId(),
                entityId = entityId,
                totalCommunications = comms.size,
                behavioralShifts = shifts,
                hasSignificantShift = shifts.any { it.significance >= ShiftSignificance.HIGH },
                overallTrend = calculateOverallTrend(shifts),
                analyzedAt = Date()
            )
        }
    }

    private fun detectPatterns(text: String, entityId: String): List<DetectedBehaviorPattern> {
        val detected = mutableListOf<DetectedBehaviorPattern>()
        val lowerText = text.lowercase()

        for ((patternType, phrases) in behaviorPatterns) {
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
                detected.add(DetectedBehaviorPattern(
                    id = HashUtils.generateHashId("pattern"),
                    entityId = entityId,
                    type = patternType,
                    instances = instances.distinct().take(5),
                    instanceCount = instances.size,
                    severity = calculatePatternSeverity(patternType, instances.size),
                    detectedAt = Date()
                ))
            }
        }
        
        return detected
    }

    private fun calculatePatternSeverity(type: BehaviorPatternType, count: Int): PatternSeverity {
        val baseWeight = when (type) {
            BehaviorPatternType.THREATENING -> 3
            BehaviorPatternType.GASLIGHTING -> 3
            BehaviorPatternType.FINANCIAL_MANIPULATION -> 2
            BehaviorPatternType.PASSIVE_ADMISSION -> 2
            BehaviorPatternType.EMOTIONAL_MANIPULATION -> 2
            BehaviorPatternType.BLAME_SHIFTING -> 2
            BehaviorPatternType.PRESSURE_TACTICS -> 2
            BehaviorPatternType.DEFLECTION -> 1
            BehaviorPatternType.OVER_EXPLAINING -> 1
            BehaviorPatternType.MINIMIZATION -> 1
        }
        
        val score = baseWeight * count
        return when {
            score >= 6 -> PatternSeverity.CRITICAL
            score >= 4 -> PatternSeverity.HIGH
            score >= 2 -> PatternSeverity.MEDIUM
            else -> PatternSeverity.LOW
        }
    }

    private fun calculateRiskScore(patterns: List<DetectedBehaviorPattern>): Float {
        if (patterns.isEmpty()) return 0f
        
        var score = 0f
        for (pattern in patterns) {
            val baseScore = when (pattern.type) {
                BehaviorPatternType.THREATENING -> 25f
                BehaviorPatternType.GASLIGHTING -> 20f
                BehaviorPatternType.FINANCIAL_MANIPULATION -> 18f
                BehaviorPatternType.PASSIVE_ADMISSION -> 15f
                BehaviorPatternType.EMOTIONAL_MANIPULATION -> 12f
                BehaviorPatternType.BLAME_SHIFTING -> 12f
                BehaviorPatternType.PRESSURE_TACTICS -> 10f
                BehaviorPatternType.DEFLECTION -> 8f
                BehaviorPatternType.OVER_EXPLAINING -> 6f
                BehaviorPatternType.MINIMIZATION -> 5f
            }
            
            // Multiply by severity
            score += baseScore * when (pattern.severity) {
                PatternSeverity.CRITICAL -> 1.5f
                PatternSeverity.HIGH -> 1.2f
                PatternSeverity.MEDIUM -> 1.0f
                PatternSeverity.LOW -> 0.7f
            }
        }
        
        return score.coerceAtMost(100f)
    }

    private fun classifyRiskLevel(score: Float): RiskLevel {
        return when {
            score >= 70 -> RiskLevel.CRITICAL
            score >= 50 -> RiskLevel.HIGH
            score >= 30 -> RiskLevel.MEDIUM
            score >= 10 -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }

    private fun identifyPrimaryConcerns(patterns: List<DetectedBehaviorPattern>): List<String> {
        return patterns
            .filter { it.severity >= PatternSeverity.MEDIUM }
            .sortedByDescending { it.instanceCount }
            .take(3)
            .map { "${it.type.name.lowercase().replace("_", " ")}: ${it.instanceCount} instance(s)" }
    }

    private fun countManipulationIndicators(patterns: List<DetectedBehaviorPattern>): Int {
        val manipulationTypes = setOf(
            BehaviorPatternType.GASLIGHTING,
            BehaviorPatternType.EMOTIONAL_MANIPULATION,
            BehaviorPatternType.FINANCIAL_MANIPULATION,
            BehaviorPatternType.PRESSURE_TACTICS
        )
        return patterns.count { it.type in manipulationTypes }
    }

    private fun countDeceptionIndicators(patterns: List<DetectedBehaviorPattern>): Int {
        val deceptionTypes = setOf(
            BehaviorPatternType.DEFLECTION,
            BehaviorPatternType.OVER_EXPLAINING,
            BehaviorPatternType.PASSIVE_ADMISSION,
            BehaviorPatternType.MINIMIZATION
        )
        return patterns.count { it.type in deceptionTypes }
    }

    private fun detectBehavioralShifts(
        communications: List<TimedCommunication>,
        entityId: String
    ): List<BehavioralShift> {
        val shifts = mutableListOf<BehavioralShift>()
        
        if (communications.size < 2) return shifts
        
        for (i in 1 until communications.size) {
            val before = detectPatterns(communications[i - 1].content, entityId)
            val after = detectPatterns(communications[i].content, entityId)
            
            val beforeTypes = before.map { it.type }.toSet()
            val afterTypes = after.map { it.type }.toSet()
            
            val newPatterns = afterTypes - beforeTypes
            val removedPatterns = beforeTypes - afterTypes
            
            if (newPatterns.isNotEmpty() || removedPatterns.isNotEmpty()) {
                val significance = when {
                    newPatterns.any { it in setOf(BehaviorPatternType.THREATENING, BehaviorPatternType.GASLIGHTING) } -> 
                        ShiftSignificance.CRITICAL
                    newPatterns.size >= 3 -> ShiftSignificance.HIGH
                    newPatterns.size >= 2 -> ShiftSignificance.MEDIUM
                    else -> ShiftSignificance.LOW
                }
                
                shifts.add(BehavioralShift(
                    timestamp = communications[i].timestamp,
                    beforePatterns = beforeTypes.toList(),
                    afterPatterns = afterTypes.toList(),
                    newPatterns = newPatterns.toList(),
                    removedPatterns = removedPatterns.toList(),
                    significance = significance
                ))
            }
        }
        
        return shifts
    }

    private fun calculateOverallTrend(shifts: List<BehavioralShift>): BehavioralTrend {
        if (shifts.isEmpty()) return BehavioralTrend.STABLE
        
        val criticalShifts = shifts.count { it.significance == ShiftSignificance.CRITICAL }
        val highShifts = shifts.count { it.significance == ShiftSignificance.HIGH }
        
        return when {
            criticalShifts >= 2 -> BehavioralTrend.ESCALATING
            highShifts >= 3 -> BehavioralTrend.ESCALATING
            shifts.size >= 5 -> BehavioralTrend.ERRATIC
            else -> BehavioralTrend.STABLE
        }
    }
}

// Data classes and enums

enum class BehaviorPatternType {
    GASLIGHTING,
    DEFLECTION,
    PRESSURE_TACTICS,
    FINANCIAL_MANIPULATION,
    EMOTIONAL_MANIPULATION,
    OVER_EXPLAINING,
    BLAME_SHIFTING,
    PASSIVE_ADMISSION,
    MINIMIZATION,
    THREATENING
}

enum class PatternSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class RiskLevel {
    MINIMAL, LOW, MEDIUM, HIGH, CRITICAL
}

enum class ShiftSignificance {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class BehavioralTrend {
    STABLE, IMPROVING, ESCALATING, ERRATIC
}

data class DetectedBehaviorPattern(
    val id: String,
    val entityId: String,
    val type: BehaviorPatternType,
    val instances: List<String>,
    val instanceCount: Int,
    val severity: PatternSeverity,
    val detectedAt: Date
)

data class BehavioralAnalysis(
    val id: String,
    val entityId: String,
    val patterns: List<DetectedBehaviorPattern>,
    val patternCount: Int,
    val riskScore: Float,
    val riskLevel: RiskLevel,
    val primaryConcerns: List<String>,
    val manipulationIndicatorCount: Int,
    val deceptionIndicatorCount: Int,
    val analyzedAt: Date
)

data class BehavioralComparison(
    val id: String,
    val entity1Id: String,
    val entity2Id: String,
    val entity1RiskScore: Float,
    val entity2RiskScore: Float,
    val sharedPatterns: List<BehaviorPatternType>,
    val uniqueToEntity1: List<BehaviorPatternType>,
    val uniqueToEntity2: List<BehaviorPatternType>,
    val higherRiskEntity: String,
    val riskDifferential: Float,
    val comparedAt: Date
)

data class TimedCommunication(
    val timestamp: Date,
    val content: String,
    val entityId: String
)

data class BehavioralShift(
    val timestamp: Date,
    val beforePatterns: List<BehaviorPatternType>,
    val afterPatterns: List<BehaviorPatternType>,
    val newPatterns: List<BehaviorPatternType>,
    val removedPatterns: List<BehaviorPatternType>,
    val significance: ShiftSignificance
)

data class BehavioralTimelineAnalysis(
    val id: String,
    val entityId: String,
    val totalCommunications: Int,
    val behavioralShifts: List<BehavioralShift>,
    val hasSignificantShift: Boolean,
    val overallTrend: BehavioralTrend,
    val analyzedAt: Date
)

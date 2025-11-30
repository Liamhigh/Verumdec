package com.verumdec.analysis.engine

import java.util.*

/**
 * BehavioralAnalyzer - Detects behavioral manipulation patterns.
 */
class BehavioralAnalyzer {

    // Pattern definitions
    private val behaviorPatterns = mapOf(
        BehaviorType.GASLIGHTING to listOf(
            "you're imagining", "that never happened", "you're crazy",
            "i never said that", "you're confused", "you misunderstood",
            "you're overreacting", "you're being paranoid"
        ),
        BehaviorType.DEFLECTION to listOf(
            "what about", "but you", "that's not the point",
            "let's focus on", "the real issue is"
        ),
        BehaviorType.PRESSURE_TACTICS to listOf(
            "you need to decide now", "this offer expires", "take it or leave it",
            "everyone else agrees", "don't miss out", "act fast", "limited time"
        ),
        BehaviorType.FINANCIAL_MANIPULATION to listOf(
            "just this once", "i'll pay you back", "trust me", "it's an investment",
            "you'll make it back", "guaranteed return", "no risk"
        ),
        BehaviorType.EMOTIONAL_MANIPULATION to listOf(
            "if you loved me", "after all i've done", "you owe me",
            "don't you trust me", "i thought we were friends"
        ),
        BehaviorType.OVER_EXPLAINING to listOf(
            "let me explain", "the reason is", "you see", "what happened was",
            "it's complicated", "there's more to it"
        ),
        BehaviorType.BLAME_SHIFTING to listOf(
            "it's your fault", "you made me", "because of you",
            "if you hadn't", "you should have"
        ),
        BehaviorType.PASSIVE_ADMISSION to listOf(
            "i thought i was in the clear", "i didn't think anyone would notice",
            "i assumed it would be fine", "technically", "in a way"
        )
    )

    /**
     * Analyze text for behavioral patterns.
     */
    fun analyze(text: String, entityId: String): BehavioralAnalysisResult {
        val detectedPatterns = mutableListOf<DetectedPattern>()
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
                detectedPatterns.add(DetectedPattern(
                    type = patternType,
                    instances = instances.distinct().take(5),
                    instanceCount = instances.size,
                    severity = calculatePatternSeverity(patternType, instances.size)
                ))
            }
        }

        val riskScore = calculateRiskScore(detectedPatterns)

        return BehavioralAnalysisResult(
            entityId = entityId,
            patterns = detectedPatterns,
            riskScore = riskScore,
            riskLevel = classifyRiskLevel(riskScore),
            analyzedAt = Date()
        )
    }

    /**
     * Analyze multiple communications.
     */
    fun analyzeMultiple(communications: List<CommunicationInput>): Map<String, BehavioralAnalysisResult> {
        val results = mutableMapOf<String, BehavioralAnalysisResult>()

        val byEntity = communications.groupBy { it.entityId }

        for ((entityId, comms) in byEntity) {
            val combinedText = comms.joinToString("\n") { it.text }
            results[entityId] = analyze(combinedText, entityId)
        }

        return results
    }

    /**
     * Compare behavioral patterns between entities.
     */
    fun compare(
        result1: BehavioralAnalysisResult,
        result2: BehavioralAnalysisResult
    ): BehavioralComparison {
        val patterns1 = result1.patterns.map { it.type }.toSet()
        val patterns2 = result2.patterns.map { it.type }.toSet()

        return BehavioralComparison(
            entity1Id = result1.entityId,
            entity2Id = result2.entityId,
            entity1RiskScore = result1.riskScore,
            entity2RiskScore = result2.riskScore,
            sharedPatterns = patterns1.intersect(patterns2).toList(),
            uniqueToEntity1 = (patterns1 - patterns2).toList(),
            uniqueToEntity2 = (patterns2 - patterns1).toList(),
            higherRiskEntity = if (result1.riskScore > result2.riskScore) result1.entityId else result2.entityId
        )
    }

    private fun calculatePatternSeverity(type: BehaviorType, count: Int): PatternSeverity {
        val baseWeight = when (type) {
            BehaviorType.GASLIGHTING -> 3
            BehaviorType.FINANCIAL_MANIPULATION -> 3
            BehaviorType.PASSIVE_ADMISSION -> 2
            BehaviorType.EMOTIONAL_MANIPULATION -> 2
            BehaviorType.BLAME_SHIFTING -> 2
            BehaviorType.PRESSURE_TACTICS -> 2
            BehaviorType.DEFLECTION -> 1
            BehaviorType.OVER_EXPLAINING -> 1
        }

        val score = baseWeight * count
        return when {
            score >= 6 -> PatternSeverity.CRITICAL
            score >= 4 -> PatternSeverity.HIGH
            score >= 2 -> PatternSeverity.MEDIUM
            else -> PatternSeverity.LOW
        }
    }

    private fun calculateRiskScore(patterns: List<DetectedPattern>): Float {
        if (patterns.isEmpty()) return 0f

        var score = 0f
        for (pattern in patterns) {
            val baseScore = when (pattern.type) {
                BehaviorType.GASLIGHTING -> 25f
                BehaviorType.FINANCIAL_MANIPULATION -> 22f
                BehaviorType.PASSIVE_ADMISSION -> 18f
                BehaviorType.EMOTIONAL_MANIPULATION -> 15f
                BehaviorType.BLAME_SHIFTING -> 12f
                BehaviorType.PRESSURE_TACTICS -> 10f
                BehaviorType.DEFLECTION -> 8f
                BehaviorType.OVER_EXPLAINING -> 6f
            }

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
}

// Data classes

enum class BehaviorType {
    GASLIGHTING, DEFLECTION, PRESSURE_TACTICS, FINANCIAL_MANIPULATION,
    EMOTIONAL_MANIPULATION, OVER_EXPLAINING, BLAME_SHIFTING, PASSIVE_ADMISSION
}

enum class PatternSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class RiskLevel {
    MINIMAL, LOW, MEDIUM, HIGH, CRITICAL
}

data class DetectedPattern(
    val type: BehaviorType,
    val instances: List<String>,
    val instanceCount: Int,
    val severity: PatternSeverity
)

data class CommunicationInput(
    val entityId: String,
    val text: String,
    val timestamp: Date
)

data class BehavioralAnalysisResult(
    val entityId: String,
    val patterns: List<DetectedPattern>,
    val riskScore: Float,
    val riskLevel: RiskLevel,
    val analyzedAt: Date
)

data class BehavioralComparison(
    val entity1Id: String,
    val entity2Id: String,
    val entity1RiskScore: Float,
    val entity2RiskScore: Float,
    val sharedPatterns: List<BehaviorType>,
    val uniqueToEntity1: List<BehaviorType>,
    val uniqueToEntity2: List<BehaviorType>,
    val higherRiskEntity: String
)

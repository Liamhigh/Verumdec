package com.verumdec.analysis.engine

/**
 * LiabilityEngine - Calculates liability scores for entities.
 */
class LiabilityEngine {

    // Weight factors
    private val weights = LiabilityWeights(
        contradiction = 0.30f,
        behavioral = 0.25f,
        evidence = 0.15f,
        consistency = 0.15f,
        causal = 0.15f
    )

    /**
     * Calculate liability for an entity.
     */
    fun calculate(input: LiabilityInput): LiabilityResult {
        // Calculate component scores
        val contradictionScore = calculateContradictionScore(input.contradictions)
        val behavioralScore = calculateBehavioralScore(input.behavioralPatterns)
        val evidenceScore = calculateEvidenceScore(input)
        val consistencyScore = calculateConsistencyScore(input)
        val causalScore = calculateCausalScore(input)

        // Calculate weighted overall score
        val overallScore =
            contradictionScore * weights.contradiction +
            behavioralScore * weights.behavioral +
            evidenceScore * weights.evidence +
            consistencyScore * weights.consistency +
            causalScore * weights.causal

        // Build breakdown
        val breakdown = LiabilityBreakdown(
            totalContradictions = input.contradictions.size,
            criticalContradictions = input.contradictions.count { it.severity == ContradictionSeverity.CRITICAL },
            highContradictions = input.contradictions.count { it.severity == ContradictionSeverity.HIGH },
            behavioralFlags = input.behavioralPatterns.map { it.type.name },
            evidenceProvided = input.evidenceProvided,
            evidenceExpected = input.evidenceExpected,
            storyChanges = input.storyChanges,
            initiatedEvents = input.initiatedEvents,
            benefitedFinancially = input.benefitedFinancially
        )

        // Generate reasoning
        val reasoning = generateReasoning(
            input.entityName,
            overallScore,
            contradictionScore,
            behavioralScore,
            breakdown
        )

        return LiabilityResult(
            entityId = input.entityId,
            entityName = input.entityName,
            overallScore = overallScore.coerceIn(0f, 100f),
            contradictionScore = contradictionScore,
            behavioralScore = behavioralScore,
            evidenceScore = evidenceScore,
            consistencyScore = consistencyScore,
            causalScore = causalScore,
            breakdown = breakdown,
            reasoning = reasoning,
            liabilityLevel = classifyLiability(overallScore)
        )
    }

    /**
     * Calculate scores for multiple entities.
     */
    fun calculateAll(inputs: List<LiabilityInput>): List<LiabilityResult> {
        return inputs.map { calculate(it) }.sortedByDescending { it.overallScore }
    }

    /**
     * Compare liability between two entities.
     */
    fun compare(result1: LiabilityResult, result2: LiabilityResult): LiabilityComparison {
        val scoreDiff = result1.overallScore - result2.overallScore
        val higherLiability = if (scoreDiff > 0) result1.entityId else result2.entityId

        val factors = mutableListOf<String>()

        if (result1.contradictionScore > result2.contradictionScore * 1.5) {
            factors.add("${result1.entityName} has significantly more contradictions")
        } else if (result2.contradictionScore > result1.contradictionScore * 1.5) {
            factors.add("${result2.entityName} has significantly more contradictions")
        }

        if (result1.behavioralScore > result2.behavioralScore * 1.5) {
            factors.add("${result1.entityName} shows more manipulative behavior patterns")
        } else if (result2.behavioralScore > result1.behavioralScore * 1.5) {
            factors.add("${result2.entityName} shows more manipulative behavior patterns")
        }

        return LiabilityComparison(
            entity1 = result1,
            entity2 = result2,
            scoreDifference = kotlin.math.abs(scoreDiff),
            higherLiabilityEntityId = higherLiability,
            keyDifferentiatingFactors = factors
        )
    }

    private fun calculateContradictionScore(contradictions: List<ContradictionResult>): Float {
        if (contradictions.isEmpty()) return 0f

        var score = 0f
        for (c in contradictions) {
            score += when (c.severity) {
                ContradictionSeverity.CRITICAL -> 30f
                ContradictionSeverity.HIGH -> 18f
                ContradictionSeverity.MEDIUM -> 10f
                ContradictionSeverity.LOW -> 4f
            }
        }

        return score.coerceAtMost(100f)
    }

    private fun calculateBehavioralScore(patterns: List<DetectedPattern>): Float {
        if (patterns.isEmpty()) return 0f

        var score = 0f
        for (p in patterns) {
            val baseScore = when (p.type) {
                BehaviorType.GASLIGHTING -> 22f
                BehaviorType.FINANCIAL_MANIPULATION -> 20f
                BehaviorType.PASSIVE_ADMISSION -> 18f
                BehaviorType.EMOTIONAL_MANIPULATION -> 14f
                BehaviorType.BLAME_SHIFTING -> 12f
                BehaviorType.PRESSURE_TACTICS -> 10f
                BehaviorType.DEFLECTION -> 8f
                BehaviorType.OVER_EXPLAINING -> 6f
            }

            score += baseScore * when (p.severity) {
                PatternSeverity.CRITICAL -> 1.4f
                PatternSeverity.HIGH -> 1.2f
                PatternSeverity.MEDIUM -> 1.0f
                PatternSeverity.LOW -> 0.7f
            }
        }

        return score.coerceAtMost(100f)
    }

    private fun calculateEvidenceScore(input: LiabilityInput): Float {
        if (input.evidenceExpected == 0) return 0f

        val ratio = input.evidenceProvided.toFloat() / input.evidenceExpected

        // Higher score = more liability (less evidence provided when expected)
        return when {
            ratio < 0.2f -> 80f  // Provided very little of expected evidence
            ratio < 0.5f -> 50f
            ratio < 0.8f -> 25f
            else -> 10f
        }
    }

    private fun calculateConsistencyScore(input: LiabilityInput): Float {
        var score = 0f

        // Story changes increase liability
        score += input.storyChanges * 12f

        // Direct contradictions heavily penalize consistency
        val directContradictions = input.contradictions.count { 
            it.type == ContradictionType.DIRECT 
        }
        score += directContradictions * 15f

        return score.coerceAtMost(100f)
    }

    private fun calculateCausalScore(input: LiabilityInput): Float {
        var score = 0f

        // Who initiated events?
        score += input.initiatedEvents * 5f

        // Financial benefit increases causal responsibility
        if (input.benefitedFinancially) {
            score += 25f
        }

        // Who controlled information?
        if (input.evidenceProvided > input.evidenceExpected / 2) {
            score += 10f
        }

        return score.coerceAtMost(100f)
    }

    private fun generateReasoning(
        entityName: String,
        overallScore: Float,
        contradictionScore: Float,
        behavioralScore: Float,
        breakdown: LiabilityBreakdown
    ): List<String> {
        val reasons = mutableListOf<String>()

        if (breakdown.criticalContradictions > 0) {
            reasons.add("$entityName has ${breakdown.criticalContradictions} critical contradiction(s) that significantly impact credibility")
        }

        if (breakdown.highContradictions > 0) {
            reasons.add("$entityName has ${breakdown.highContradictions} high-severity contradiction(s)")
        }

        if (behavioralScore > 50) {
            reasons.add("$entityName exhibits concerning behavioral patterns including ${breakdown.behavioralFlags.take(3).joinToString(", ")}")
        }

        if (breakdown.storyChanges > 2) {
            reasons.add("$entityName's account has changed ${breakdown.storyChanges} times")
        }

        if (breakdown.benefitedFinancially) {
            reasons.add("$entityName appears to have benefited financially from the events in question")
        }

        if (breakdown.evidenceProvided < breakdown.evidenceExpected / 2) {
            reasons.add("$entityName provided significantly less evidence than expected")
        }

        if (reasons.isEmpty()) {
            reasons.add("$entityName has a relatively clean record based on available evidence")
        }

        return reasons
    }

    private fun classifyLiability(score: Float): LiabilityLevel {
        return when {
            score >= 75 -> LiabilityLevel.CRITICAL
            score >= 55 -> LiabilityLevel.HIGH
            score >= 35 -> LiabilityLevel.MEDIUM
            score >= 15 -> LiabilityLevel.LOW
            else -> LiabilityLevel.MINIMAL
        }
    }
}

// Data classes

data class LiabilityWeights(
    val contradiction: Float,
    val behavioral: Float,
    val evidence: Float,
    val consistency: Float,
    val causal: Float
)

data class LiabilityInput(
    val entityId: String,
    val entityName: String,
    val contradictions: List<ContradictionResult>,
    val behavioralPatterns: List<DetectedPattern>,
    val evidenceProvided: Int,
    val evidenceExpected: Int,
    val storyChanges: Int,
    val initiatedEvents: Int,
    val benefitedFinancially: Boolean
)

data class LiabilityBreakdown(
    val totalContradictions: Int,
    val criticalContradictions: Int,
    val highContradictions: Int,
    val behavioralFlags: List<String>,
    val evidenceProvided: Int,
    val evidenceExpected: Int,
    val storyChanges: Int,
    val initiatedEvents: Int,
    val benefitedFinancially: Boolean
)

enum class LiabilityLevel {
    MINIMAL, LOW, MEDIUM, HIGH, CRITICAL
}

data class LiabilityResult(
    val entityId: String,
    val entityName: String,
    val overallScore: Float,
    val contradictionScore: Float,
    val behavioralScore: Float,
    val evidenceScore: Float,
    val consistencyScore: Float,
    val causalScore: Float,
    val breakdown: LiabilityBreakdown,
    val reasoning: List<String>,
    val liabilityLevel: LiabilityLevel
)

data class LiabilityComparison(
    val entity1: LiabilityResult,
    val entity2: LiabilityResult,
    val scoreDifference: Float,
    val higherLiabilityEntityId: String,
    val keyDifferentiatingFactors: List<String>
)

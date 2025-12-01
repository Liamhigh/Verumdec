package com.verumdec.engine

import com.verumdec.data.*

/**
 * Liability Matrix Calculator
 * Computes liability scores for each entity based on multiple factors.
 */
class LiabilityCalculator {

    // Weight factors for different components
    private val weights = mapOf(
        "contradiction" to 0.30f,
        "behavioral" to 0.20f,
        "evidence" to 0.15f,
        "consistency" to 0.20f,
        "causal" to 0.15f
    )

    /**
     * Calculate liability scores for all entities.
     */
    fun calculateLiability(
        entities: List<Entity>,
        contradictions: List<Contradiction>,
        behavioralPatterns: List<BehavioralPattern>,
        evidenceList: List<Evidence>,
        timeline: List<TimelineEvent>
    ): Map<String, LiabilityScore> {
        val scores = mutableMapOf<String, LiabilityScore>()
        
        for (entity in entities) {
            scores[entity.id] = calculateEntityLiability(
                entity,
                contradictions.filter { it.entityId == entity.id },
                behavioralPatterns.filter { it.entityId == entity.id },
                evidenceList,
                timeline.filter { entity.id in it.entityIds }
            )
        }
        
        return scores
    }

    /**
     * Calculate liability for a single entity.
     */
    private fun calculateEntityLiability(
        entity: Entity,
        contradictions: List<Contradiction>,
        behavioralPatterns: List<BehavioralPattern>,
        evidenceList: List<Evidence>,
        entityTimeline: List<TimelineEvent>
    ): LiabilityScore {
        // 1. Contradiction Score (0-100)
        val contradictionScore = calculateContradictionScore(contradictions)
        
        // 2. Behavioral Score (0-100)
        val behavioralScore = calculateBehavioralScore(behavioralPatterns)
        
        // 3. Evidence Contribution Score (0-100, higher = more suspicious if low)
        val evidenceScore = calculateEvidenceScore(entity, evidenceList)
        
        // 4. Chronological Consistency Score (0-100, higher = less consistent)
        val consistencyScore = calculateConsistencyScore(contradictions, entityTimeline)
        
        // 5. Causal Responsibility Score (0-100)
        val causalScore = calculateCausalScore(entity, entityTimeline)
        
        // Calculate weighted overall score
        val overallScore = 
            contradictionScore * weights["contradiction"]!! +
            behavioralScore * weights["behavioral"]!! +
            evidenceScore * weights["evidence"]!! +
            consistencyScore * weights["consistency"]!! +
            causalScore * weights["causal"]!!
        
        // Build breakdown
        val breakdown = LiabilityBreakdown(
            totalContradictions = contradictions.size,
            criticalContradictions = contradictions.count { it.severity == Severity.CRITICAL },
            behavioralFlags = behavioralPatterns.map { it.type.name },
            evidenceProvided = countEvidenceProvided(entity, evidenceList),
            evidenceWithheld = countEvidenceWithheld(entity, contradictions),
            storyChanges = contradictions.count { it.type == ContradictionType.DIRECT },
            initiatedEvents = countInitiatedEvents(entity, entityTimeline),
            benefitedFinancially = checkFinancialBenefit(entity, entityTimeline),
            controlledInformation = checkInformationControl(entity, evidenceList)
        )
        
        return LiabilityScore(
            entityId = entity.id,
            overallScore = overallScore.coerceIn(0f, 100f),
            contradictionScore = contradictionScore,
            behavioralScore = behavioralScore,
            evidenceContributionScore = evidenceScore,
            chronologicalConsistencyScore = consistencyScore,
            causalResponsibilityScore = causalScore,
            breakdown = breakdown
        )
    }

    /**
     * Calculate score based on contradictions.
     */
    private fun calculateContradictionScore(contradictions: List<Contradiction>): Float {
        if (contradictions.isEmpty()) return 0f
        
        var score = 0f
        
        for (contradiction in contradictions) {
            score += when (contradiction.severity) {
                Severity.CRITICAL -> 25f
                Severity.HIGH -> 15f
                Severity.MEDIUM -> 8f
                Severity.LOW -> 3f
            }
        }
        
        return score.coerceAtMost(100f)
    }

    /**
     * Calculate score based on behavioral patterns.
     */
    private fun calculateBehavioralScore(patterns: List<BehavioralPattern>): Float {
        if (patterns.isEmpty()) return 0f
        
        var score = 0f
        
        for (pattern in patterns) {
            val baseScore = when (pattern.type) {
                BehaviorType.GASLIGHTING -> 20f
                BehaviorType.FINANCIAL_MANIPULATION -> 18f
                BehaviorType.PASSIVE_ADMISSION -> 15f
                BehaviorType.BLAME_SHIFTING -> 12f
                BehaviorType.PRESSURE_TACTICS -> 10f
                BehaviorType.DEFLECTION -> 8f
                BehaviorType.EMOTIONAL_MANIPULATION -> 8f
                BehaviorType.OVER_EXPLAINING -> 6f
                BehaviorType.GHOSTING -> 5f
                BehaviorType.SUDDEN_WITHDRAWAL -> 5f
                BehaviorType.DELAYED_RESPONSE -> 4f
                BehaviorType.SLIP_UP_ADMISSION -> 15f
            }
            
            // Multiply by severity
            score += baseScore * when (pattern.severity) {
                Severity.CRITICAL -> 1.5f
                Severity.HIGH -> 1.2f
                Severity.MEDIUM -> 1.0f
                Severity.LOW -> 0.7f
            }
        }
        
        return score.coerceAtMost(100f)
    }

    /**
     * Calculate score based on evidence contribution.
     * Low contribution when expected = higher liability.
     */
    private fun calculateEvidenceScore(entity: Entity, evidenceList: List<Evidence>): Float {
        val entityEvidence = evidenceList.count { evidence ->
            evidence.metadata.sender?.contains(entity.primaryName, ignoreCase = true) == true ||
            evidence.metadata.author?.contains(entity.primaryName, ignoreCase = true) == true ||
            entity.emails.any { evidence.metadata.sender?.contains(it, ignoreCase = true) == true }
        }
        
        val totalEvidence = evidenceList.size
        
        if (totalEvidence == 0) return 0f
        
        val contributionRatio = entityEvidence.toFloat() / totalEvidence
        
        // If entity is frequently mentioned but provides little evidence, score higher
        return if (entity.mentions > 5 && contributionRatio < 0.2f) {
            50f
        } else if (contributionRatio < 0.1f) {
            30f
        } else {
            10f
        }
    }

    /**
     * Calculate chronological consistency score.
     */
    private fun calculateConsistencyScore(
        contradictions: List<Contradiction>,
        entityTimeline: List<TimelineEvent>
    ): Float {
        if (entityTimeline.isEmpty()) return 0f
        
        var score = 0f
        
        // Count story changes over time
        val storyChanges = contradictions.count { 
            it.type == ContradictionType.DIRECT || it.type == ContradictionType.TEMPORAL 
        }
        
        score += storyChanges * 15f
        
        // Check for behavioral changes in timeline
        val eventTypes = entityTimeline.map { it.eventType }
        val hasContradictionEvents = EventType.CONTRADICTION in eventTypes
        val hasDenialFollowedByAdmission = eventTypes.zipWithNext().any { (a, b) ->
            a == EventType.DENIAL && b == EventType.ADMISSION
        }
        
        if (hasContradictionEvents) score += 20f
        if (hasDenialFollowedByAdmission) score += 30f
        
        return score.coerceAtMost(100f)
    }

    /**
     * Calculate causal responsibility score.
     * Higher score indicates greater causal responsibility in the matter.
     */
    private fun calculateCausalScore(entity: Entity, entityTimeline: List<TimelineEvent>): Float {
        var score = 0f
        
        // Who initiated events? Critical events indicate higher responsibility
        val criticalCount = entityTimeline.count { it.significance == Significance.CRITICAL }
        val highCount = entityTimeline.count { it.significance == Significance.HIGH }
        score += criticalCount * 10f
        score += highCount * 5f
        
        // Check for payment events (who received money vs who paid)
        val paymentEvents = entityTimeline.filter { it.eventType == EventType.PAYMENT }
        val receivedPayments = paymentEvents.count { 
            it.description.contains("received", ignoreCase = true) ||
            it.description.contains("got", ignoreCase = true) ||
            it.description.contains("took", ignoreCase = true)
        }
        val madePayments = paymentEvents.count {
            it.description.contains("paid", ignoreCase = true) ||
            it.description.contains("sent", ignoreCase = true) ||
            it.description.contains("transferred", ignoreCase = true)
        }
        // Receiving money without providing service/goods indicates higher liability
        score += receivedPayments * 15f
        // Making payments indicates lower liability (reduce score)
        score -= madePayments * 5f
        
        // Check for promise events (who made promises)
        val promises = entityTimeline.count { it.eventType == EventType.PROMISE }
        score += promises * 3f
        
        // Check for denial events after evidence presented
        val denialAfterEvidence = entityTimeline.count { it.eventType == EventType.DENIAL }
        score += denialAfterEvidence * 8f
        
        // Check for admission events (indicates some responsibility acceptance)
        val admissions = entityTimeline.count { it.eventType == EventType.ADMISSION }
        score += admissions * 5f
        
        return score.coerceIn(0f, 100f)
    }

    /**
     * Count evidence provided by entity.
     */
    private fun countEvidenceProvided(entity: Entity, evidenceList: List<Evidence>): Int {
        return evidenceList.count { evidence ->
            evidence.metadata.sender?.contains(entity.primaryName, ignoreCase = true) == true ||
            evidence.metadata.author?.contains(entity.primaryName, ignoreCase = true) == true
        }
    }

    /**
     * Count potential withheld evidence.
     */
    private fun countEvidenceWithheld(entity: Entity, contradictions: List<Contradiction>): Int {
        return contradictions.count { it.type == ContradictionType.MISSING_EVIDENCE }
    }

    /**
     * Count events initiated by entity.
     */
    private fun countInitiatedEvents(entity: Entity, timeline: List<TimelineEvent>): Int {
        return timeline.count { it.significance == Significance.HIGH || it.significance == Significance.CRITICAL }
    }

    /**
     * Check if entity benefited financially.
     */
    private fun checkFinancialBenefit(entity: Entity, timeline: List<TimelineEvent>): Boolean {
        return timeline.any { event ->
            event.eventType == EventType.PAYMENT &&
            (event.description.contains("received", ignoreCase = true) ||
             event.description.contains("got", ignoreCase = true) ||
             event.description.contains("paid to", ignoreCase = true))
        }
    }

    /**
     * Check if entity controlled information flow.
     */
    private fun checkInformationControl(entity: Entity, evidenceList: List<Evidence>): Boolean {
        val entityEvidence = evidenceList.count { evidence ->
            evidence.metadata.sender?.contains(entity.primaryName, ignoreCase = true) == true
        }
        return entityEvidence > evidenceList.size / 2
    }
}

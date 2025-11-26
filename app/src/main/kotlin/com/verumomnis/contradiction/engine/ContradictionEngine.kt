package com.verumomnis.contradiction.engine

import java.time.LocalDateTime

/**
 * Core Contradiction Engine
 *
 * This engine analyzes evidence to detect contradictions, build timelines,
 * identify behavioral patterns, and calculate liability scores.
 *
 * All processing is done entirely offline on-device.
 */
class ContradictionEngine {

    private val entities = mutableMapOf<String, Entity>()
    private val claims = mutableListOf<Claim>()
    private val contradictions = mutableListOf<Contradiction>()
    private val timeline = mutableListOf<TimelineEvent>()
    private val behavioralPatterns = mutableListOf<BehavioralPattern>()

    /**
     * Adds a claim to the engine for analysis.
     */
    fun addClaim(claim: Claim) {
        claims.add(claim)

        // Ensure entity exists
        val entity = entities.getOrPut(claim.entityId) {
            Entity(id = claim.entityId, primaryName = claim.entityId)
        }
        entity.claims.add(claim)

        // Add to timeline
        timeline.add(
            TimelineEvent(
                id = "event_${timeline.size}",
                timestamp = claim.timestamp,
                entityId = claim.entityId,
                eventType = TimelineEvent.EventType.STATEMENT,
                description = claim.statement,
                sourceDocument = claim.sourceDocument,
                linkedClaim = claim
            )
        )
    }

    /**
     * Registers an entity with known information.
     */
    fun registerEntity(entity: Entity) {
        entities[entity.id] = entity
    }

    /**
     * Runs full contradiction analysis across all claims.
     */
    fun analyzeContradictions(): List<Contradiction> {
        contradictions.clear()

        // Group claims by entity
        val claimsByEntity = claims.groupBy { it.entityId }

        for ((entityId, entityClaims) in claimsByEntity) {
            // Check for direct contradictions within same entity
            for (i in entityClaims.indices) {
                for (j in i + 1 until entityClaims.size) {
                    val claimA = entityClaims[i]
                    val claimB = entityClaims[j]

                    val contradiction = detectContradiction(claimA, claimB)
                    if (contradiction != null) {
                        contradictions.add(contradiction)

                        // Add contradiction event to timeline
                        timeline.add(
                            TimelineEvent(
                                id = "contradiction_${timeline.size}",
                                timestamp = claimB.timestamp,
                                entityId = entityId,
                                eventType = TimelineEvent.EventType.CONTRADICTION,
                                description = contradiction.explanation,
                                sourceDocument = claimB.sourceDocument,
                                linkedContradiction = contradiction
                            )
                        )
                    }
                }
            }
        }

        return contradictions.sortedByDescending { it.severity.weight }
    }

    /**
     * Detects if two claims contradict each other.
     */
    private fun detectContradiction(claimA: Claim, claimB: Claim): Contradiction? {
        val assertionA = claimA.extractAssertion()
        val assertionB = claimB.extractAssertion()

        // Check for direct negation patterns
        if (isDirectNegation(assertionA, assertionB)) {
            return Contradiction(
                id = "contradiction_${contradictions.size}",
                claimA = claimA,
                claimB = claimB,
                type = determineContradictionType(claimA, claimB),
                severity = Contradiction.Severity.CRITICAL,
                explanation = buildContradictionExplanation(claimA, claimB),
                liabilityImpact = 1.0f
            )
        }

        // Check for semantic contradictions
        if (isSemanticallyContradictory(assertionA, assertionB)) {
            return Contradiction(
                id = "contradiction_${contradictions.size}",
                claimA = claimA,
                claimB = claimB,
                type = determineContradictionType(claimA, claimB),
                severity = Contradiction.Severity.HIGH,
                explanation = buildContradictionExplanation(claimA, claimB),
                liabilityImpact = 0.75f
            )
        }

        return null
    }

    /**
     * Checks if one assertion directly negates another.
     */
    private fun isDirectNegation(assertionA: String, assertionB: String): Boolean {
        val negationPatterns = listOf(
            "no " to "",
            "never " to "",
            "not " to "",
            "didn't " to "did ",
            "don't " to "do ",
            "wasn't " to "was ",
            "weren't " to "were ",
            "isn't " to "is ",
            "aren't " to "are ",
            "haven't " to "have ",
            "hasn't " to "has ",
            "won't " to "will ",
            "can't " to "can ",
            "couldn't " to "could "
        )

        for ((negative, positive) in negationPatterns) {
            if (assertionA.contains(negative) && assertionB.contains(positive)) {
                val normalizedA = assertionA.replace(negative, positive)
                if (calculateSimilarity(normalizedA, assertionB) > 0.7) {
                    return true
                }
            }
            if (assertionB.contains(negative) && assertionA.contains(positive)) {
                val normalizedB = assertionB.replace(negative, positive)
                if (calculateSimilarity(assertionA, normalizedB) > 0.7) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Checks if two assertions are semantically contradictory.
     */
    private fun isSemanticallyContradictory(assertionA: String, assertionB: String): Boolean {
        // Contradiction keywords that indicate conflicting statements
        val contradictionIndicators = listOf(
            "deal" to "no deal",
            "agreed" to "never agreed",
            "received" to "never received",
            "sent" to "never sent",
            "paid" to "never paid",
            "existed" to "never existed",
            "signed" to "never signed"
        )

        for ((positive, negative) in contradictionIndicators) {
            if ((assertionA.contains(positive) && assertionB.contains(negative)) ||
                (assertionA.contains(negative) && assertionB.contains(positive))
            ) {
                return true
            }
        }

        return false
    }

    /**
     * Calculates similarity between two strings using Jaccard index.
     */
    private fun calculateSimilarity(a: String, b: String): Float {
        val wordsA = a.split(" ").toSet()
        val wordsB = b.split(" ").toSet()
        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    /**
     * Determines the type of contradiction based on sources.
     */
    private fun determineContradictionType(claimA: Claim, claimB: Claim): Contradiction.ContradictionType {
        return when {
            claimA.sourceType != claimB.sourceType -> Contradiction.ContradictionType.CROSS_DOCUMENT
            else -> Contradiction.ContradictionType.DIRECT
        }
    }

    /**
     * Builds a human-readable explanation of the contradiction.
     */
    private fun buildContradictionExplanation(claimA: Claim, claimB: Claim): String {
        return buildString {
            append("On ${claimA.timestamp.toLocalDate()}, ")
            append("stated: \"${claimA.statement}\". ")
            append("Then on ${claimB.timestamp.toLocalDate()}, ")
            append("stated: \"${claimB.statement}\". ")
            append("This contradiction indicates that ")
            if (claimA.timestamp.isBefore(claimB.timestamp)) {
                append("the earlier statement may have been false, or the story changed after external pressure.")
            } else {
                append("the later statement contradicts previously established facts.")
            }
        }
    }

    /**
     * Analyzes behavioral patterns across all entities.
     */
    fun analyzeBehavior(): List<BehavioralPattern> {
        behavioralPatterns.clear()

        for ((entityId, entity) in entities) {
            analyzeEntityBehavior(entityId, entity.claims)
        }

        return behavioralPatterns
    }

    /**
     * Analyzes behavioral patterns for a single entity.
     */
    private fun analyzeEntityBehavior(entityId: String, entityClaims: List<Claim>) {
        // Detect over-explaining (classic fraud red flag)
        val longStatements = entityClaims.filter { it.statement.length > 200 }
        if (longStatements.size > entityClaims.size / 2) {
            behavioralPatterns.add(
                BehavioralPattern(
                    id = "pattern_${behavioralPatterns.size}",
                    entityId = entityId,
                    patternType = BehavioralPattern.PatternType.OVER_EXPLAINING,
                    instances = longStatements,
                    confidence = 0.8f,
                    description = "Entity consistently provides overly detailed explanations, a common indicator of deception."
                )
            )
        }

        // Detect blame shifting
        val blamePatterns = listOf("your fault", "you should have", "if you hadn't", "because of you", "you caused")
        val blameShiftingClaims = entityClaims.filter { claim ->
            blamePatterns.any { claim.statement.lowercase().contains(it) }
        }
        if (blameShiftingClaims.isNotEmpty()) {
            behavioralPatterns.add(
                BehavioralPattern(
                    id = "pattern_${behavioralPatterns.size}",
                    entityId = entityId,
                    patternType = BehavioralPattern.PatternType.BLAME_SHIFTING,
                    instances = blameShiftingClaims,
                    confidence = 0.9f,
                    description = "Entity exhibits pattern of shifting blame to others."
                )
            )
        }

        // Detect passive admissions
        val admissionPatterns = listOf("thought i was", "assumed", "didn't think", "in the clear", "wouldn't notice")
        val passiveAdmissions = entityClaims.filter { claim ->
            admissionPatterns.any { claim.statement.lowercase().contains(it) }
        }
        if (passiveAdmissions.isNotEmpty()) {
            behavioralPatterns.add(
                BehavioralPattern(
                    id = "pattern_${behavioralPatterns.size}",
                    entityId = entityId,
                    patternType = BehavioralPattern.PatternType.PASSIVE_ADMISSION,
                    instances = passiveAdmissions,
                    confidence = 0.95f,
                    description = "Entity made inadvertent admissions revealing awareness of wrongdoing."
                )
            )
        }
    }

    /**
     * Calculates liability scores for all entities.
     */
    fun calculateLiability(): Map<String, LiabilityEntry> {
        val liabilityMap = mutableMapOf<String, LiabilityEntry>()

        for ((entityId, entity) in entities) {
            // Calculate individual scores
            val contradictionScore = calculateContradictionScore(entityId)
            val behavioralScore = calculateBehavioralScore(entityId)
            val evidenceScore = calculateEvidenceScore(entityId)
            val consistencyScore = calculateConsistencyScore(entity)
            val causalScore = calculateCausalScore(entityId)

            // Weighted total
            val totalScore = (
                contradictionScore * 0.30f +
                behavioralScore * 0.25f +
                evidenceScore * 0.15f +
                (1f - consistencyScore) * 0.15f +
                causalScore * 0.15f
            )

            liabilityMap[entityId] = LiabilityEntry(
                entityId = entityId,
                contradictionScore = contradictionScore,
                behavioralDeceptionScore = behavioralScore,
                evidenceContribution = evidenceScore,
                chronologicalConsistency = consistencyScore,
                causalResponsibility = causalScore,
                totalLiabilityPercent = (totalScore * 100).coerceIn(0f, 100f)
            )

            entity.liabilityScore = totalScore
        }

        return liabilityMap
    }

    private fun calculateContradictionScore(entityId: String): Float {
        val entityContradictions = contradictions.count {
            it.claimA.entityId == entityId || it.claimB.entityId == entityId
        }
        val entityClaims = entities[entityId]?.claims?.size ?: 1
        return (entityContradictions.toFloat() / entityClaims).coerceIn(0f, 1f)
    }

    private fun calculateBehavioralScore(entityId: String): Float {
        val entityPatterns = behavioralPatterns.filter { it.entityId == entityId }
        if (entityPatterns.isEmpty()) return 0f
        return entityPatterns.map { it.confidence }.average().toFloat()
    }

    private fun calculateEvidenceScore(entityId: String): Float {
        val entityClaims = entities[entityId]?.claims ?: return 0f
        val documentTypes = entityClaims.map { it.sourceType }.distinct()
        // More document types = better evidence contribution
        return (documentTypes.size.toFloat() / Claim.SourceType.entries.size).coerceIn(0f, 1f)
    }

    private fun calculateConsistencyScore(entity: Entity): Float {
        if (entity.claims.size < 2) return 1f

        // Check if story remains stable over time
        val sortedClaims = entity.claims.sortedBy { it.timestamp }
        var consistentPairs = 0
        var totalPairs = 0

        for (i in sortedClaims.indices) {
            for (j in i + 1 until sortedClaims.size) {
                totalPairs++
                val contradiction = detectContradiction(sortedClaims[i], sortedClaims[j])
                if (contradiction == null) {
                    consistentPairs++
                }
            }
        }

        return if (totalPairs == 0) 1f else consistentPairs.toFloat() / totalPairs
    }

    private fun calculateCausalScore(entityId: String): Float {
        val entityEvents = timeline.filter { it.entityId == entityId }
        if (entityEvents.isEmpty()) return 0f

        // Check if entity initiated key events
        val initiationIndicators = listOf("request", "demand", "sent", "initiated", "started")
        val initiatedEvents = entityEvents.count { event ->
            initiationIndicators.any { event.description.lowercase().contains(it) }
        }

        return (initiatedEvents.toFloat() / entityEvents.size).coerceIn(0f, 1f)
    }

    /**
     * Returns the complete timeline sorted chronologically.
     */
    fun getTimeline(): List<TimelineEvent> {
        return timeline.sortedBy { it.timestamp }
    }

    /**
     * Returns all registered entities.
     */
    fun getEntities(): Map<String, Entity> = entities.toMap()

    /**
     * Returns all detected contradictions.
     */
    fun getContradictions(): List<Contradiction> = contradictions.toList()

    /**
     * Returns all detected behavioral patterns.
     */
    fun getBehavioralPatterns(): List<BehavioralPattern> = behavioralPatterns.toList()

    /**
     * Resets the engine state.
     */
    fun reset() {
        entities.clear()
        claims.clear()
        contradictions.clear()
        timeline.clear()
        behavioralPatterns.clear()
    }
}

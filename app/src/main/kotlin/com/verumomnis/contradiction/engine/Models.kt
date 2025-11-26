package com.verumomnis.contradiction.engine

import java.time.LocalDateTime

/**
 * Represents a claim made by an entity at a specific time.
 * Claims are the atomic units of contradiction detection.
 *
 * Format: [Entity] claims [Fact] at [Time]
 */
data class Claim(
    val id: String,
    val entityId: String,
    val statement: String,
    val timestamp: LocalDateTime,
    val sourceDocument: String,
    val sourceType: SourceType,
    val confidence: Float = 1.0f
) {
    enum class SourceType {
        PDF,
        EMAIL,
        WHATSAPP,
        SMS,
        IMAGE,
        AUDIO_TRANSCRIPT,
        VIDEO_TRANSCRIPT,
        TYPED_STATEMENT
    }

    /**
     * Extracts the factual assertion from the statement.
     * Used for semantic comparison with other claims.
     */
    fun extractAssertion(): String {
        return statement
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

/**
 * Represents an entity discovered in the evidence.
 * Entities are people, organizations, or other identifiable actors.
 */
data class Entity(
    val id: String,
    val primaryName: String,
    val aliases: Set<String> = emptySet(),
    val email: String? = null,
    val phone: String? = null,
    val bankAccount: String? = null,
    val claims: MutableList<Claim> = mutableListOf(),
    var liabilityScore: Float = 0f
) {
    /**
     * Returns all known identifiers for this entity.
     */
    fun getAllIdentifiers(): Set<String> {
        val identifiers = mutableSetOf(primaryName)
        identifiers.addAll(aliases)
        email?.let { identifiers.add(it) }
        phone?.let { identifiers.add(it) }
        return identifiers
    }
}

/**
 * Represents a detected contradiction between claims.
 */
data class Contradiction(
    val id: String,
    val claimA: Claim,
    val claimB: Claim,
    val type: ContradictionType,
    val severity: Severity,
    val explanation: String,
    val liabilityImpact: Float
) {
    enum class ContradictionType {
        DIRECT,           // A says X, then A says NOT X
        CROSS_DOCUMENT,   // A says X in email, NOT X in WhatsApp
        BEHAVIORAL,       // Sudden story shifts, tone changes
        MISSING_EVIDENCE, // A refers to document that was never provided
        TEMPORAL          // Timeline impossibilities
    }

    enum class Severity(val weight: Float) {
        CRITICAL(1.0f),   // Flips liability
        HIGH(0.75f),      // Dishonest intent likely
        MEDIUM(0.5f),     // Unclear/error
        LOW(0.25f)        // Harmless inconsistency
    }
}

/**
 * Timeline event for chronological ordering of all evidence.
 */
data class TimelineEvent(
    val id: String,
    val timestamp: LocalDateTime,
    val entityId: String?,
    val eventType: EventType,
    val description: String,
    val sourceDocument: String,
    val linkedClaim: Claim? = null,
    val linkedContradiction: Contradiction? = null
) {
    enum class EventType {
        STATEMENT,
        PAYMENT,
        REQUEST,
        PROMISE,
        DENIAL,
        CONTRADICTION,
        DOCUMENT_SUBMITTED,
        MISSING_DOCUMENT,
        STORY_CHANGE
    }
}

/**
 * Behavioral pattern detected in entity communications.
 */
data class BehavioralPattern(
    val id: String,
    val entityId: String,
    val patternType: PatternType,
    val instances: List<Claim>,
    val confidence: Float,
    val description: String
) {
    enum class PatternType {
        GASLIGHTING,
        DEFLECTION,
        PRESSURE_TACTICS,
        FINANCIAL_MANIPULATION,
        EMOTIONAL_MANIPULATION,
        SUDDEN_WITHDRAWAL,
        GHOSTING_AFTER_PAYMENT,
        OVER_EXPLAINING,          // Classic fraud red flag
        SLIP_UP_ADMISSION,
        PASSIVE_ADMISSION,
        DELAYED_RESPONSE,
        BLAME_SHIFTING
    }
}

/**
 * Liability matrix entry for an entity.
 */
data class LiabilityEntry(
    val entityId: String,
    val contradictionScore: Float,      // How often they changed story
    val behavioralDeceptionScore: Float, // Gaslighting, blame shifting
    val evidenceContribution: Float,     // Did they provide evidence or excuses
    val chronologicalConsistency: Float, // Is story stable over time
    val causalResponsibility: Float,     // Who initiated, delayed, benefited
    val totalLiabilityPercent: Float     // Final percentage
)

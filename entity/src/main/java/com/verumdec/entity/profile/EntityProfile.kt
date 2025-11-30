package com.verumdec.entity.profile

import java.util.UUID

/**
 * EntityProfile tracks all attributes, claims, and behaviors for a person,
 * company, or legal structure involved in the case.
 *
 * This enables entity-level contradiction detection by comparing profiles
 * across documents and identifying when claims conflict.
 *
 * @property id Unique identifier for this entity
 * @property name Primary name for the entity
 * @property aliases All known aliases or references to this entity
 * @property type Type of entity (person, company, etc.)
 * @property role Role in the case (plaintiff, defendant, witness, etc.)
 * @property attributes Key attributes tracked for this entity
 * @property claims All claims made by this entity
 * @property actions All actions attributed to this entity
 * @property financialFigures All financial amounts associated with entity
 * @property timelineFootprint Timestamps where entity appears
 * @property statementIds All statement IDs from this entity
 * @property documentIds Documents where entity appears
 * @property behavioralProfile Behavioral analysis data
 */
data class EntityProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val aliases: MutableList<String> = mutableListOf(),
    val type: EntityType = EntityType.PERSON,
    var role: String = "",
    val attributes: MutableMap<String, String> = mutableMapOf(),
    val claims: MutableList<EntityClaim> = mutableListOf(),
    val actions: MutableList<EntityAction> = mutableListOf(),
    val financialFigures: MutableList<FinancialFigure> = mutableListOf(),
    val timelineFootprint: MutableList<Long> = mutableListOf(),
    val statementIds: MutableList<String> = mutableListOf(),
    val documentIds: MutableSet<String> = mutableSetOf(),
    var behavioralProfile: BehavioralProfile = BehavioralProfile()
) {
    /**
     * Add an alias for this entity.
     */
    fun addAlias(alias: String) {
        if (alias.isNotBlank() && !aliases.contains(alias)) {
            aliases.add(alias)
        }
    }
    
    /**
     * Add a claim made by this entity.
     */
    fun addClaim(claim: EntityClaim) {
        claims.add(claim)
    }
    
    /**
     * Add an action attributed to this entity.
     */
    fun addAction(action: EntityAction) {
        actions.add(action)
    }
    
    /**
     * Add a financial figure associated with this entity.
     */
    fun addFinancialFigure(figure: FinancialFigure) {
        financialFigures.add(figure)
    }
    
    /**
     * Check if the entity is known by a particular name.
     */
    fun isKnownAs(nameToCheck: String): Boolean {
        return name.equals(nameToCheck, ignoreCase = true) ||
               aliases.any { it.equals(nameToCheck, ignoreCase = true) }
    }
    
    /**
     * Get summary for narrative generation.
     */
    fun getSummary(): String {
        return buildString {
            append("$name")
            if (role.isNotBlank()) append(" ($role)")
            if (aliases.isNotEmpty()) append(", also known as: ${aliases.joinToString(", ")}")
        }
    }
}

/**
 * Types of entities.
 */
enum class EntityType {
    PERSON,
    COMPANY,
    ORGANIZATION,
    GOVERNMENT_BODY,
    LEGAL_STRUCTURE,
    SITE,
    UNKNOWN
}

/**
 * Represents a claim made by an entity.
 */
data class EntityClaim(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timestamp: Long?,
    val documentId: String,
    val statementId: String,
    val category: ClaimCategory,
    val subject: String = ""
)

/**
 * Categories of claims.
 */
enum class ClaimCategory {
    ASSERTION,
    DENIAL,
    PROMISE,
    ADMISSION,
    ACCUSATION,
    DEFENSE,
    FINANCIAL,
    FACTUAL,
    OPINION
}

/**
 * Represents an action attributed to an entity.
 */
data class EntityAction(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val timestamp: Long?,
    val documentId: String,
    val type: ActionType,
    val targetEntityId: String? = null
)

/**
 * Types of actions.
 */
enum class ActionType {
    PAYMENT,
    REQUEST,
    COMMUNICATION,
    AGREEMENT,
    VIOLATION,
    THREAT,
    SUPPORT,
    OPPOSITION,
    MEETING,
    OTHER
}

/**
 * Represents a financial figure associated with an entity.
 */
data class FinancialFigure(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val currency: String = "USD",
    val description: String,
    val timestamp: Long?,
    val documentId: String,
    val context: String = ""
)

/**
 * Behavioral profile tracking sentiment, certainty, and patterns.
 */
data class BehavioralProfile(
    var sentimentTrend: MutableList<SentimentDataPoint> = mutableListOf(),
    var certaintyTrend: MutableList<CertaintyDataPoint> = mutableListOf(),
    var deflectionCount: Int = 0,
    var toneShifts: MutableList<ToneShift> = mutableListOf(),
    var patterns: MutableSet<BehavioralPattern> = mutableSetOf()
)

/**
 * Sentiment data point for trend tracking.
 */
data class SentimentDataPoint(
    val timestamp: Long,
    val sentiment: Double,
    val statementId: String
)

/**
 * Certainty data point for trend tracking.
 */
data class CertaintyDataPoint(
    val timestamp: Long,
    val certainty: Double,
    val statementId: String
)

/**
 * Represents a detected tone shift.
 */
data class ToneShift(
    val beforeStatementId: String,
    val afterStatementId: String,
    val beforeTone: String,
    val afterTone: String,
    val triggerEvent: String = ""
)

/**
 * Behavioral patterns that can be detected.
 */
enum class BehavioralPattern {
    GASLIGHTING,
    DEFLECTION,
    PRESSURE_TACTICS,
    FINANCIAL_MANIPULATION,
    EMOTIONAL_MANIPULATION,
    WITHDRAWAL,
    GHOSTING,
    OVER_EXPLAINING,
    SLIP_UP_ADMISSION,
    BLAME_SHIFTING,
    CONSISTENT,
    COOPERATIVE
}

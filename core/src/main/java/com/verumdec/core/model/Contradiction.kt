package com.verumdec.core.model

import java.util.UUID

/**
 * Represents a detected contradiction between statements.
 *
 * @property id Unique identifier for this contradiction
 * @property type The type/category of contradiction
 * @property sourceStatement The first statement in the contradiction pair
 * @property targetStatement The second statement in the contradiction pair
 * @property sourceDocument Source document identifier
 * @property sourceLineNumber Line number in source document
 * @property severity Severity score (1-10)
 * @property description Human-readable description of the contradiction
 * @property legalTrigger Recommended legal trigger (optional)
 * @property affectedEntities List of entity IDs affected by this contradiction
 * @property similarityScore Semantic similarity score between statements
 */
data class Contradiction(
    val id: String = UUID.randomUUID().toString(),
    val type: ContradictionType,
    val sourceStatement: Statement,
    val targetStatement: Statement,
    val sourceDocument: String,
    val sourceLineNumber: Int,
    val severity: Int,
    val description: String,
    val legalTrigger: LegalTrigger? = null,
    val affectedEntities: List<String> = emptyList(),
    val similarityScore: Double = 0.0
) {
    init {
        require(severity in 1..10) { "Severity must be between 1 and 10" }
    }
}

/**
 * Types of contradictions that can be detected.
 */
enum class ContradictionType {
    /** Direct contradiction: A says X then NOT X */
    DIRECT,
    /** Cross-document contradiction: Document A says X, Document B says NOT X */
    CROSS_DOCUMENT,
    /** Timeline contradiction: Events in impossible or inconsistent order */
    TIMELINE,
    /** Entity contradiction: Entity claims conflict across documents */
    ENTITY,
    /** Behavioral contradiction: Sudden denial after prior certainty */
    BEHAVIORAL,
    /** Semantic contradiction: High similarity with opposite meaning */
    SEMANTIC,
    /** Financial contradiction: Financial figures change without explanation */
    FINANCIAL,
    /** Missing evidence contradiction: Claims unsupported by evidence */
    MISSING_EVIDENCE
}

/**
 * Legal triggers that may be associated with contradictions.
 */
enum class LegalTrigger {
    FRAUD,
    MISREPRESENTATION,
    CONCEALMENT,
    PERJURY_RISK,
    BREACH_OF_CONTRACT,
    TIMELINE_INCONSISTENCY,
    UNRELIABLE_TESTIMONY,
    FINANCIAL_DISCREPANCY,
    CONFLICT_OF_INTEREST,
    NEGLIGENCE
}

/**
 * Severity levels for contradictions.
 */
enum class ContradictionSeverity(val score: Int, val description: String) {
    CRITICAL(10, "Flips liability"),
    HIGH(8, "Dishonest intent likely"),
    MEDIUM(5, "Unclear/error"),
    LOW(2, "Harmless inconsistency")
}

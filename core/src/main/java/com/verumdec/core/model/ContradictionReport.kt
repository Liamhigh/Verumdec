package com.verumdec.core.model

/**
 * Unified ContradictionReport containing all contradiction sources.
 * This is the central output of the ContradictionEngine, combining:
 * - Semantic contradictions
 * - Timeline contradictions
 * - Entity contradictions
 * - Behavioral contradictions
 *
 * @property caseId Identifier for the case being analyzed
 * @property totalContradictions Total count of all contradictions
 * @property contradictions All detected contradictions
 * @property timelineConflicts All timeline-based conflicts
 * @property behavioralAnomalies All behavioral pattern anomalies
 * @property affectedEntities Map of entity IDs to their involvement
 * @property documentLinks Map of documents to their contradictions
 * @property severityBreakdown Count of contradictions by severity
 * @property legalTriggers Recommended legal triggers with evidence
 * @property summary Human-readable summary of findings
 * @property verificationStatus Status of engine self-verification
 */
data class ContradictionReport(
    val caseId: String,
    val totalContradictions: Int,
    val contradictions: List<Contradiction>,
    val timelineConflicts: List<TimelineConflict>,
    val behavioralAnomalies: List<BehavioralAnomaly>,
    val affectedEntities: Map<String, EntityInvolvement>,
    val documentLinks: Map<String, List<String>>,
    val severityBreakdown: Map<Int, Int>,
    val legalTriggers: List<LegalTriggerEvidence>,
    val summary: String,
    val verificationStatus: VerificationStatus
) {
    /**
     * Get contradictions filtered by type.
     */
    fun getContradictionsByType(type: ContradictionType): List<Contradiction> {
        return contradictions.filter { it.type == type }
    }
    
    /**
     * Get contradictions filtered by severity.
     */
    fun getContradictionsBySeverity(minSeverity: Int): List<Contradiction> {
        return contradictions.filter { it.severity >= minSeverity }
    }
    
    /**
     * Get critical contradictions (severity >= 8).
     */
    fun getCriticalContradictions(): List<Contradiction> {
        return getContradictionsBySeverity(8)
    }
    
    /**
     * Check if the report contains any contradictions.
     */
    fun hasContradictions(): Boolean {
        return totalContradictions > 0 || timelineConflicts.isNotEmpty() || behavioralAnomalies.isNotEmpty()
    }
}

/**
 * Represents an entity's involvement in contradictions.
 */
data class EntityInvolvement(
    val entityId: String,
    val entityName: String,
    val contradictionCount: Int,
    val contradictionIds: List<String>,
    val liabilityScore: Int,
    val primaryRole: String
)

/**
 * Represents a behavioral anomaly detected in statements.
 */
data class BehavioralAnomaly(
    val id: String,
    val entityId: String,
    val type: BehavioralAnomalyType,
    val description: String,
    val severity: Int,
    val statementIds: List<String>,
    val beforeState: String,
    val afterState: String
)

/**
 * Types of behavioral anomalies.
 */
enum class BehavioralAnomalyType {
    /** Sudden denial after prior certainty */
    SUDDEN_DENIAL,
    /** Tone shifts from cooperative to defensive */
    TONE_SHIFT,
    /** Confidence declines after legal triggers */
    CONFIDENCE_DECLINE,
    /** Statements become vague after contradictions */
    VAGUENESS_INCREASE,
    /** Deflection language increases */
    DEFLECTION_PATTERN,
    /** Over-explaining (fraud red flag) */
    OVER_EXPLAINING,
    /** Blame shifting pattern */
    BLAME_SHIFTING,
    /** Sudden withdrawal or ghosting */
    WITHDRAWAL,
    /** Gaslighting behavior */
    GASLIGHTING,
    /** Pressure tactics */
    PRESSURE_TACTICS
}

/**
 * Legal trigger with supporting evidence.
 */
data class LegalTriggerEvidence(
    val trigger: LegalTrigger,
    val description: String,
    val supportingContradictionIds: List<String>,
    val confidence: Double,
    val recommendation: String
)

/**
 * Status of engine self-verification.
 */
data class VerificationStatus(
    val statementsIndexed: Boolean,
    val embeddingsGenerated: Boolean,
    val timelineObjectsExist: Boolean,
    val entityProfilesExist: Boolean,
    val contradictionPassRan: Boolean,
    val warnings: List<String>,
    val autoCorrections: List<String>
)

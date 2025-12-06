package org.verumomnis.engine

/**
 * Verum Omnis Forensic Engine - Data Models
 * These models represent the data structures used throughout the 12-step forensic pipeline.
 */

/**
 * A single sentence from the evidence with metadata
 */
data class Sentence(
    val text: String,
    val sourceEvidenceId: String,
    val timestamp: Long? = null,
    val subjectTags: MutableList<SubjectTag> = mutableListOf(),
    val keywords: MutableList<String> = mutableListOf(),
    val behaviors: MutableList<BehaviorFlag> = mutableListOf(),
    val severity: SeverityLevel = SeverityLevel.LOW,
    var isFlagged: Boolean = false
)

/**
 * Legal subject classifications
 */
enum class SubjectTag {
    SHAREHOLDER_OPPRESSION,
    BREACH_OF_FIDUCIARY_DUTY,
    CYBERCRIME,
    FRAUDULENT_EVIDENCE,
    EMOTIONAL_EXPLOITATION
}

/**
 * Behavioral analysis flags
 */
enum class BehaviorFlag {
    EVASION,
    GASLIGHTING,
    BLAME_SHIFTING,
    SELECTIVE_DISCLOSURE,
    REFUSAL_TO_ANSWER,
    JUSTIFICATION_LOOPS,
    UNAUTHORIZED_ACCESS_ATTEMPTS
}

/**
 * Severity levels for scoring
 */
enum class SeverityLevel(val score: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3)
}

/**
 * A detected contradiction between statements
 */
data class ContradictionResult(
    val statementA: Sentence,
    val statementB: Sentence,
    val description: String,
    val severity: SeverityLevel = SeverityLevel.HIGH
)

/**
 * An omission detected in the evidence
 */
data class OmissionResult(
    val description: String,
    val context: String,
    val severity: SeverityLevel
)

/**
 * Top liability entry
 */
data class LiabilityEntry(
    val category: SubjectTag,
    val totalSeverity: Int,
    val contradictionCount: Int,
    val recurrence: Int
)

/**
 * Recommended action based on findings
 */
data class RecommendedAction(
    val authority: String,
    val action: String,
    val legalBasis: String
)

/**
 * Complete forensic report structure
 */
data class ForensicReportData(
    val caseId: String,
    val preAnalysisDeclaration: String,
    val criticalLegalSubjects: Map<SubjectTag, Int>,
    val dishonestyMatrix: List<Sentence>,
    val taggedEvidence: List<Sentence>,
    val contradictions: List<ContradictionResult>,
    val omissions: List<OmissionResult>,
    val behavioralFlags: Map<BehaviorFlag, Int>,
    val dishonestyScore: Float,
    val topLiabilities: List<LiabilityEntry>,
    val recommendedActions: List<RecommendedAction>,
    val postAnalysisDeclaration: String
)

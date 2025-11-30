package com.verumdec.data

import java.util.Date
import java.util.UUID

/**
 * Represents a legal case containing all evidence and analysis.
 */
data class Case(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val evidence: MutableList<Evidence> = mutableListOf(),
    val entities: MutableList<Entity> = mutableListOf(),
    val timeline: MutableList<TimelineEvent> = mutableListOf(),
    val contradictions: MutableList<Contradiction> = mutableListOf(),
    val liabilityScores: MutableMap<String, LiabilityScore> = mutableMapOf(),
    val narrative: String = "",
    val sealedHash: String? = null
)

/**
 * Represents a piece of evidence (document, image, text, etc.)
 */
data class Evidence(
    val id: String = UUID.randomUUID().toString(),
    val type: EvidenceType,
    val fileName: String,
    val filePath: String,
    val addedAt: Date = Date(),
    val extractedText: String = "",
    val metadata: EvidenceMetadata = EvidenceMetadata(),
    val processed: Boolean = false,
    val sha512Hash: String = "",
    val originUri: String = "",
    val processedAt: Date? = null
)

enum class EvidenceType {
    PDF,
    IMAGE,
    TEXT,
    EMAIL,
    WHATSAPP,
    AUDIO,
    VIDEO,
    UNKNOWN
}

/**
 * Metadata extracted from evidence files.
 */
data class EvidenceMetadata(
    val creationDate: Date? = null,
    val modificationDate: Date? = null,
    val author: String? = null,
    val sender: String? = null,
    val receiver: String? = null,
    val subject: String? = null,
    val cc: String? = null,
    val exifData: Map<String, String> = emptyMap(),
    val pageCount: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val charCount: Int = 0,
    val wordCount: Int = 0,
    val messageCount: Int = 0,
    val participants: String = "",
    val durationSeconds: Long = 0,
    val fileSize: Long = 0
)

/**
 * Represents a discovered entity (person, company, etc.)
 */
data class Entity(
    val id: String = UUID.randomUUID().toString(),
    val primaryName: String,
    val aliases: MutableList<String> = mutableListOf(),
    val emails: MutableList<String> = mutableListOf(),
    val phoneNumbers: MutableList<String> = mutableListOf(),
    val bankAccounts: MutableList<String> = mutableListOf(),
    val mentions: Int = 0,
    val statements: MutableList<Statement> = mutableListOf(),
    val liabilityScore: Float = 0f
)

/**
 * A statement or claim made by an entity.
 */
data class Statement(
    val id: String = UUID.randomUUID().toString(),
    val entityId: String,
    val text: String,
    val date: Date? = null,
    val sourceEvidenceId: String,
    val type: StatementType,
    val keywords: List<String> = emptyList()
)

enum class StatementType {
    CLAIM,
    DENIAL,
    PROMISE,
    ADMISSION,
    ACCUSATION,
    EXPLANATION,
    OTHER
}

/**
 * An event on the timeline.
 */
data class TimelineEvent(
    val id: String = UUID.randomUUID().toString(),
    val date: Date,
    val description: String,
    val sourceEvidenceId: String,
    val entityIds: List<String> = emptyList(),
    val eventType: EventType,
    val significance: Significance = Significance.NORMAL
)

enum class EventType {
    COMMUNICATION,
    PAYMENT,
    PROMISE,
    DOCUMENT,
    CONTRADICTION,
    ADMISSION,
    DENIAL,
    BEHAVIOR_CHANGE,
    OTHER
}

enum class Significance {
    CRITICAL,
    HIGH,
    NORMAL,
    LOW
}

/**
 * A detected contradiction between statements or facts.
 */
data class Contradiction(
    val id: String = UUID.randomUUID().toString(),
    val entityId: String,
    val statementA: Statement,
    val statementB: Statement,
    val type: ContradictionType,
    val severity: Severity,
    val description: String,
    val legalImplication: String = "",
    val detectedAt: Date = Date()
)

enum class ContradictionType {
    DIRECT,           // A says X, then A says NOT X
    CROSS_DOCUMENT,   // Different story in different documents
    BEHAVIORAL,       // Sudden story shifts, tone changes
    MISSING_EVIDENCE, // References document that was never provided
    TEMPORAL,         // Timeline inconsistency
    THIRD_PARTY       // Contradicted by another entity's statement
}

enum class Severity {
    CRITICAL,  // Flips liability
    HIGH,      // Dishonest intent likely
    MEDIUM,    // Unclear/error
    LOW        // Harmless inconsistency
}

/**
 * Liability score for an entity.
 */
data class LiabilityScore(
    val entityId: String,
    val overallScore: Float,  // 0-100 percentage
    val contradictionScore: Float,
    val behavioralScore: Float,
    val evidenceContributionScore: Float,
    val chronologicalConsistencyScore: Float,
    val causalResponsibilityScore: Float,
    val breakdown: LiabilityBreakdown = LiabilityBreakdown()
)

/**
 * Detailed breakdown of liability factors.
 */
data class LiabilityBreakdown(
    val totalContradictions: Int = 0,
    val criticalContradictions: Int = 0,
    val behavioralFlags: List<String> = emptyList(),
    val evidenceProvided: Int = 0,
    val evidenceWithheld: Int = 0,
    val storyChanges: Int = 0,
    val initiatedEvents: Int = 0,
    val benefitedFinancially: Boolean = false,
    val controlledInformation: Boolean = false
)

/**
 * Behavioral pattern detected in communication.
 */
data class BehavioralPattern(
    val id: String = UUID.randomUUID().toString(),
    val entityId: String,
    val type: BehaviorType,
    val instances: List<String> = emptyList(),
    val firstDetectedAt: Date? = null,
    val severity: Severity = Severity.MEDIUM
)

enum class BehaviorType {
    GASLIGHTING,
    DEFLECTION,
    PRESSURE_TACTICS,
    FINANCIAL_MANIPULATION,
    EMOTIONAL_MANIPULATION,
    SUDDEN_WITHDRAWAL,
    GHOSTING,
    OVER_EXPLAINING,
    SLIP_UP_ADMISSION,
    DELAYED_RESPONSE,
    BLAME_SHIFTING,
    PASSIVE_ADMISSION
}

/**
 * The final sealed forensic report.
 */
data class ForensicReport(
    val id: String = UUID.randomUUID().toString(),
    val caseId: String,
    val caseName: String,
    val generatedAt: Date = Date(),
    val entities: List<Entity>,
    val timeline: List<TimelineEvent>,
    val contradictions: List<Contradiction>,
    val behavioralPatterns: List<BehavioralPattern>,
    val liabilityScores: Map<String, LiabilityScore>,
    val narrativeSections: NarrativeSections,
    val sha512Hash: String,
    val version: String = "1.0.0"
)

/**
 * Different sections of the generated narrative.
 */
data class NarrativeSections(
    val objectiveNarration: String = "",          // Clean chronological account
    val contradictionCommentary: String = "",     // Flags where stories diverged
    val behavioralPatternAnalysis: String = "",   // Manipulation patterns
    val deductiveLogic: String = "",              // WHY contradictions matter
    val causalChain: String = "",                 // Cause -> effect links
    val finalSummary: String = ""                 // Merged legal story
)

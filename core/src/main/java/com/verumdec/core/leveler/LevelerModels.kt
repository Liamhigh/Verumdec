package com.verumdec.core.leveler

import java.util.Date
import java.util.UUID

/**
 * LevelerOutput - Unified output from the Leveler contradiction engine.
 * Contains all analysis results from the pipeline.
 */
data class LevelerOutput(
    val caseId: String,
    val caseName: String,
    val generatedAt: Date,
    val speakerMap: SpeakerMap,
    val normalizedTimeline: NormalizedTimeline,
    val contradictionSet: ContradictionSet,
    val behaviorShiftReport: BehaviorShiftReport,
    val liabilityScores: Map<String, LiabilityScoreEntry>,
    val extractionSummary: ExtractionSummary,
    val processedDocuments: List<ProcessedDocument>,
    val engineVersion: String
)

/**
 * CaseFile - Input case for the Leveler engine.
 */
data class CaseFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Date = Date(),
    val documents: MutableList<DocumentInfo> = mutableListOf()
)

/**
 * DocumentInfo - Information about a document in a case.
 */
data class DocumentInfo(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val type: DocumentType,
    val addedAt: Date = Date()
)

/**
 * DocumentType - Types of documents that can be processed.
 */
enum class DocumentType {
    PDF,
    IMAGE,
    TEXT,
    EMAIL,
    CHAT,
    UNKNOWN
}

/**
 * ProcessedDocument - Document after text extraction.
 */
data class ProcessedDocument(
    val id: String,
    val fileName: String,
    val documentType: DocumentType,
    val extractedText: String,
    val metadata: DocumentMetadata,
    val processed: Boolean
)

/**
 * DocumentMetadata - Metadata extracted from a document.
 */
data class DocumentMetadata(
    val creationDate: Date?,
    val author: String?,
    val pageCount: Int
)

/**
 * SpeakerMap - Map of all speakers/entities discovered in the case.
 */
data class SpeakerMap(
    val speakers: Map<String, SpeakerProfile>,
    val totalSpeakers: Int,
    val crossDocumentSpeakers: Int
)

/**
 * SpeakerProfile - Profile of a speaker/entity.
 */
data class SpeakerProfile(
    val id: String,
    val name: String,
    val aliases: MutableList<String>,
    val statementCount: Int,
    val firstAppearance: Long?,
    val lastAppearance: Long?,
    val documentIds: List<String>,
    val averageSentiment: Double,
    val averageCertainty: Double
)

/**
 * NormalizedTimeline - Chronologically ordered timeline of events.
 */
data class NormalizedTimeline(
    val events: List<TimelineEventEntry>,
    val totalEvents: Int,
    val normalizationMode: TimestampNormalizationMode,
    val earliestTimestamp: Long?,
    val latestTimestamp: Long?
)

/**
 * TimelineEventEntry - Single event in the timeline.
 */
data class TimelineEventEntry(
    val id: String,
    val timestamp: Long,
    val description: String,
    val speaker: String,
    val documentId: String,
    val statementId: String,
    val eventType: String,
    val confidence: Double
)

/**
 * TimestampNormalizationMode - How timestamps are normalized.
 */
enum class TimestampNormalizationMode {
    DOCUMENT_RELATIVE,
    ABSOLUTE,
    INFERRED
}

/**
 * ContradictionSet - Collection of all detected contradictions.
 */
data class ContradictionSet(
    val contradictions: List<ContradictionEntry>,
    val totalCount: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val contradictionTypes: Map<String, Int>
)

/**
 * ContradictionEntry - Single detected contradiction.
 */
data class ContradictionEntry(
    val id: String,
    val type: com.verumdec.core.model.ContradictionType,
    val sourceStatementId: String,
    val targetStatementId: String,
    val sourceText: String,
    val targetText: String,
    val sourceSpeaker: String,
    val targetSpeaker: String,
    val sourceDocument: String,
    val targetDocument: String,
    val severity: Int,
    val description: String,
    val legalTrigger: String
)

/**
 * BehaviorShiftReport - Report of all behavioral shifts detected.
 */
data class BehaviorShiftReport(
    val shifts: List<BehaviorShift>,
    val totalShifts: Int,
    val affectedSpeakers: List<String>,
    val patternBreakdown: Map<String, Int>
)

/**
 * BehaviorShift - Single behavioral shift detected.
 */
data class BehaviorShift(
    val id: String,
    val speakerId: String,
    val shiftType: ShiftType,
    val beforeStatementId: String,
    val afterStatementId: String,
    val beforeState: String,
    val afterState: String,
    val severity: Int,
    val description: String
)

/**
 * ShiftType - Types of behavioral shifts.
 */
enum class ShiftType {
    TONE_SHIFT_POSITIVE,
    TONE_SHIFT_NEGATIVE,
    CERTAINTY_DECLINE,
    CERTAINTY_INCREASE,
    DEFLECTION,
    GASLIGHTING,
    OVER_EXPLAINING,
    BLAME_SHIFTING,
    WITHDRAWAL,
    SUDDEN_DENIAL
}

/**
 * LiabilityScoreEntry - Liability score for a single entity.
 */
data class LiabilityScoreEntry(
    val entityId: String,
    val entityName: String,
    val overallScore: Float,
    val contradictionScore: Float,
    val behavioralScore: Float,
    val consistencyScore: Float,
    val evidenceContributionScore: Float,
    val contradictionCount: Int,
    val behavioralShiftCount: Int,
    val breakdown: LiabilityBreakdownEntry
)

/**
 * LiabilityBreakdownEntry - Detailed breakdown of liability factors.
 */
data class LiabilityBreakdownEntry(
    val totalContradictions: Int,
    val criticalContradictions: Int,
    val behavioralFlags: List<String>,
    val storyChanges: Int
)

/**
 * ExtractionSummary - Summary of the extraction process.
 */
data class ExtractionSummary(
    val totalDocuments: Int,
    val processedDocuments: Int,
    val totalStatements: Int,
    val speakersIdentified: Int,
    val timelineEvents: Int,
    val processingDurationMs: Long
)

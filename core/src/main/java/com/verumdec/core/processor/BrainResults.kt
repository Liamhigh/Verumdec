package com.verumdec.core.processor

import java.util.Date
import java.util.UUID

/**
 * Base sealed class for all Brain processing results.
 * All Brain subsystems return a subtype of BrainResult that can be serialized to JSON.
 */
sealed class BrainResult {
    abstract val success: Boolean
    abstract val timestamp: Long
    abstract val brainId: String
}

// ============================================
// Document Brain Results
// ============================================

/**
 * Result from DocumentBrain processing.
 * Contains extracted metadata, content hash, and document analysis.
 */
sealed class DocumentBrainResult : BrainResult() {
    override val brainId: String = "DocumentBrain"

    data class Success(
        override val timestamp: Long = System.currentTimeMillis(),
        val documentId: String = UUID.randomUUID().toString(),
        val fileHash: String,
        val metadata: DocumentMetadata,
        val extractedText: String,
        val pageCount: Int,
        val fileSize: Long,
        val mimeType: String,
        val warnings: List<String> = emptyList()
    ) : DocumentBrainResult() {
        override val success: Boolean = true
    }

    data class Failure(
        override val timestamp: Long = System.currentTimeMillis(),
        val error: String,
        val errorCode: DocumentErrorCode
    ) : DocumentBrainResult() {
        override val success: Boolean = false
    }
}

data class DocumentMetadata(
    val creationDate: Date? = null,
    val modificationDate: Date? = null,
    val author: String? = null,
    val title: String? = null,
    val subject: String? = null,
    val producer: String? = null,
    val creator: String? = null,
    val keywords: List<String> = emptyList(),
    val customProperties: Map<String, String> = emptyMap()
)

enum class DocumentErrorCode {
    FILE_NOT_FOUND,
    UNSUPPORTED_FORMAT,
    CORRUPTED_FILE,
    PASSWORD_PROTECTED,
    PROCESSING_ERROR,
    HASH_COMPUTATION_FAILED
}

// ============================================
// Image Brain Results
// ============================================

/**
 * Result from ImageBrain processing.
 * Detects edits, inconsistencies, and compression anomalies.
 */
sealed class ImageBrainResult : BrainResult() {
    override val brainId: String = "ImageBrain"

    data class Success(
        override val timestamp: Long = System.currentTimeMillis(),
        val imageId: String = UUID.randomUUID().toString(),
        val imageHash: String,
        val dimensions: ImageDimensions,
        val format: String,
        val compressionAnalysis: CompressionAnalysis,
        val editDetection: EditDetection,
        val exifData: Map<String, String> = emptyMap(),
        val anomalies: List<ImageAnomaly> = emptyList(),
        val integrityScore: Float
    ) : ImageBrainResult() {
        override val success: Boolean = true
    }

    data class Failure(
        override val timestamp: Long = System.currentTimeMillis(),
        val error: String,
        val errorCode: ImageErrorCode
    ) : ImageBrainResult() {
        override val success: Boolean = false
    }
}

data class ImageDimensions(
    val width: Int,
    val height: Int,
    val bitDepth: Int = 24
)

data class CompressionAnalysis(
    val compressionType: String,
    val qualityEstimate: Float,
    val recompressionDetected: Boolean,
    val compressionArtifacts: List<String> = emptyList()
)

data class EditDetection(
    val edited: Boolean,
    val editRegions: List<EditRegion> = emptyList(),
    val cloneDetected: Boolean,
    val spliceDetected: Boolean,
    val filterApplied: Boolean,
    val resizeDetected: Boolean,
    val confidence: Float
)

data class EditRegion(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val type: String,
    val confidence: Float
)

data class ImageAnomaly(
    val type: ImageAnomalyType,
    val description: String,
    val severity: AnomalySeverity,
    val location: String? = null
)

enum class ImageAnomalyType {
    METADATA_INCONSISTENCY,
    COMPRESSION_ARTIFACT,
    CLONE_DETECTION,
    SPLICE_DETECTION,
    NOISE_PATTERN_MISMATCH,
    LIGHTING_INCONSISTENCY,
    SHADOW_INCONSISTENCY,
    EXIF_MANIPULATION
}

enum class AnomalySeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}

enum class ImageErrorCode {
    FILE_NOT_FOUND,
    UNSUPPORTED_FORMAT,
    CORRUPTED_IMAGE,
    PROCESSING_ERROR,
    ANALYSIS_FAILED
}

// ============================================
// Video Brain Results
// ============================================

/**
 * Result from VideoBrain processing.
 * Performs frame hashing and metadata consistency scanning.
 */
sealed class VideoBrainResult : BrainResult() {
    override val brainId: String = "VideoBrain"

    data class Success(
        override val timestamp: Long = System.currentTimeMillis(),
        val videoId: String = UUID.randomUUID().toString(),
        val fileHash: String,
        val duration: Long,
        val frameCount: Int,
        val frameRate: Float,
        val resolution: VideoResolution,
        val codec: String,
        val keyFrameHashes: List<FrameHash>,
        val metadataConsistency: MetadataConsistency,
        val anomalies: List<VideoAnomaly> = emptyList(),
        val integrityScore: Float
    ) : VideoBrainResult() {
        override val success: Boolean = true
    }

    data class Failure(
        override val timestamp: Long = System.currentTimeMillis(),
        val error: String,
        val errorCode: VideoErrorCode
    ) : VideoBrainResult() {
        override val success: Boolean = false
    }
}

data class VideoResolution(
    val width: Int,
    val height: Int
)

data class FrameHash(
    val frameNumber: Int,
    val timestampMs: Long,
    val hash: String,
    val isKeyFrame: Boolean
)

data class MetadataConsistency(
    val consistent: Boolean,
    val creationDateMatches: Boolean,
    val durationMatches: Boolean,
    val codecConsistent: Boolean,
    val discrepancies: List<String> = emptyList()
)

data class VideoAnomaly(
    val type: VideoAnomalyType,
    val description: String,
    val timestampMs: Long,
    val frameNumber: Int,
    val severity: AnomalySeverity
)

enum class VideoAnomalyType {
    FRAME_DISCONTINUITY,
    TIMESTAMP_JUMP,
    QUALITY_CHANGE,
    SPLICE_DETECTED,
    METADATA_INCONSISTENCY,
    AUDIO_VIDEO_DESYNC,
    ENCODING_ANOMALY
}

enum class VideoErrorCode {
    FILE_NOT_FOUND,
    UNSUPPORTED_FORMAT,
    CORRUPTED_VIDEO,
    PROCESSING_ERROR,
    CODEC_NOT_SUPPORTED,
    ANALYSIS_FAILED
}

// ============================================
// Audio Brain Results
// ============================================

/**
 * Result from AudioBrain processing.
 * Performs waveform fingerprinting and metadata consistency analysis.
 */
sealed class AudioBrainResult : BrainResult() {
    override val brainId: String = "AudioBrain"

    data class Success(
        override val timestamp: Long = System.currentTimeMillis(),
        val audioId: String = UUID.randomUUID().toString(),
        val fileHash: String,
        val duration: Long,
        val sampleRate: Int,
        val channels: Int,
        val bitrate: Int,
        val format: String,
        val fingerprint: AudioFingerprint,
        val metadataConsistency: AudioMetadataConsistency,
        val anomalies: List<AudioAnomaly> = emptyList(),
        val integrityScore: Float
    ) : AudioBrainResult() {
        override val success: Boolean = true
    }

    data class Failure(
        override val timestamp: Long = System.currentTimeMillis(),
        val error: String,
        val errorCode: AudioErrorCode
    ) : AudioBrainResult() {
        override val success: Boolean = false
    }
}

data class AudioFingerprint(
    val hash: String,
    val segments: List<AudioSegment>,
    val dominantFrequencies: List<Float>,
    val averageAmplitude: Float
)

data class AudioSegment(
    val startMs: Long,
    val endMs: Long,
    val hash: String,
    val silenceDetected: Boolean
)

data class AudioMetadataConsistency(
    val consistent: Boolean,
    val recordingDateValid: Boolean,
    val durationMatches: Boolean,
    val formatConsistent: Boolean,
    val discrepancies: List<String> = emptyList()
)

data class AudioAnomaly(
    val type: AudioAnomalyType,
    val description: String,
    val timestampMs: Long,
    val severity: AnomalySeverity
)

enum class AudioAnomalyType {
    SPLICE_DETECTED,
    NOISE_FLOOR_CHANGE,
    COMPRESSION_ARTIFACT,
    SILENCE_GAP,
    VOLUME_DISCONTINUITY,
    BACKGROUND_CHANGE,
    ENCODING_ANOMALY,
    METADATA_TAMPERING
}

enum class AudioErrorCode {
    FILE_NOT_FOUND,
    UNSUPPORTED_FORMAT,
    CORRUPTED_AUDIO,
    PROCESSING_ERROR,
    ANALYSIS_FAILED
}

// ============================================
// Behavioral Brain Results
// ============================================

/**
 * Result from BehavioralBrain processing.
 * Detects contradictions between documents and behavioral patterns.
 */
sealed class BehavioralBrainResult : BrainResult() {
    override val brainId: String = "BehavioralBrain"

    data class Success(
        override val timestamp: Long = System.currentTimeMillis(),
        val analysisId: String = UUID.randomUUID().toString(),
        val entityId: String,
        val contradictions: List<DocumentContradiction>,
        val behavioralPatterns: List<BehavioralPatternResult>,
        val storyConsistency: StoryConsistency,
        val riskScore: Float,
        val warnings: List<String> = emptyList()
    ) : BehavioralBrainResult() {
        override val success: Boolean = true
    }

    data class Failure(
        override val timestamp: Long = System.currentTimeMillis(),
        val error: String,
        val errorCode: BehavioralErrorCode
    ) : BehavioralBrainResult() {
        override val success: Boolean = false
    }
}

data class DocumentContradiction(
    val id: String = UUID.randomUUID().toString(),
    val documentA: String,
    val documentB: String,
    val statementA: String,
    val statementB: String,
    val type: ContradictionType,
    val severity: AnomalySeverity,
    val explanation: String,
    val legalImplication: String
)

enum class ContradictionType {
    DIRECT,
    TEMPORAL,
    SEMANTIC,
    FACTUAL,
    OMISSION
}

data class BehavioralPatternResult(
    val patternType: BehavioralPatternType,
    val instances: List<String>,
    val frequency: Int,
    val severity: AnomalySeverity,
    val confidence: Float
)

enum class BehavioralPatternType {
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

data class StoryConsistency(
    val overallConsistent: Boolean,
    val consistencyScore: Float,
    val storyVersions: Int,
    val majorShifts: List<StoryShift>
)

data class StoryShift(
    val date: Date?,
    val description: String,
    val fromStatement: String,
    val toStatement: String,
    val significance: AnomalySeverity
)

enum class BehavioralErrorCode {
    INSUFFICIENT_DATA,
    ENTITY_NOT_FOUND,
    PROCESSING_ERROR,
    ANALYSIS_FAILED
}

// ============================================
// Timeline Brain Results
// ============================================

/**
 * Result from TimelineBrain processing.
 * Auto-builds legal timeline from evidence timestamps.
 */
sealed class TimelineBrainResult : BrainResult() {
    override val brainId: String = "TimelineBrain"

    data class Success(
        override val timestamp: Long = System.currentTimeMillis(),
        val timelineId: String = UUID.randomUUID().toString(),
        val events: List<TimelineEventResult>,
        val dateRange: DateRange,
        val entityParticipation: Map<String, Int>,
        val keyMoments: List<KeyMoment>,
        val gaps: List<TimelineGap>,
        val warnings: List<String> = emptyList()
    ) : TimelineBrainResult() {
        override val success: Boolean = true
    }

    data class Failure(
        override val timestamp: Long = System.currentTimeMillis(),
        val error: String,
        val errorCode: TimelineErrorCode
    ) : TimelineBrainResult() {
        override val success: Boolean = false
    }
}

data class TimelineEventResult(
    val id: String = UUID.randomUUID().toString(),
    val date: Date,
    val description: String,
    val sourceDocumentId: String,
    val sourceDocumentName: String,
    val entityIds: List<String>,
    val eventType: TimelineEventType,
    val significance: AnomalySeverity,
    val verified: Boolean
)

enum class TimelineEventType {
    COMMUNICATION,
    PAYMENT,
    PROMISE,
    DOCUMENT_CREATED,
    DOCUMENT_MODIFIED,
    CONTRADICTION,
    ADMISSION,
    DENIAL,
    BEHAVIOR_CHANGE,
    AGREEMENT,
    BREACH,
    OTHER
}

data class DateRange(
    val start: Date,
    val end: Date,
    val durationDays: Int
)

data class KeyMoment(
    val date: Date,
    val description: String,
    val significance: AnomalySeverity,
    val relatedEventIds: List<String>
)

data class TimelineGap(
    val startDate: Date,
    val endDate: Date,
    val durationDays: Int,
    val description: String,
    val suspiciousLevel: AnomalySeverity
)

enum class TimelineErrorCode {
    NO_DATES_FOUND,
    INSUFFICIENT_DATA,
    PROCESSING_ERROR,
    INVALID_DATE_FORMAT
}

// ============================================
// Email Brain Results
// ============================================

/**
 * Result from EmailBrain processing.
 * Performs header parsing and spoof detection.
 */
sealed class EmailBrainResult : BrainResult() {
    override val brainId: String = "EmailBrain"

    data class Success(
        override val timestamp: Long = System.currentTimeMillis(),
        val emailId: String = UUID.randomUUID().toString(),
        val headers: EmailHeaders,
        val authentication: EmailAuthentication,
        val spoofAnalysis: SpoofAnalysis,
        val threadInfo: EmailThreadInfo,
        val attachments: List<EmailAttachment>,
        val warnings: List<String> = emptyList(),
        val trustScore: Float
    ) : EmailBrainResult() {
        override val success: Boolean = true
    }

    data class Failure(
        override val timestamp: Long = System.currentTimeMillis(),
        val error: String,
        val errorCode: EmailErrorCode
    ) : EmailBrainResult() {
        override val success: Boolean = false
    }
}

data class EmailHeaders(
    val from: String,
    val to: List<String>,
    val cc: List<String> = emptyList(),
    val bcc: List<String> = emptyList(),
    val subject: String,
    val date: Date?,
    val messageId: String?,
    val inReplyTo: String?,
    val references: List<String> = emptyList(),
    val receivedChain: List<ReceivedHeader> = emptyList(),
    val customHeaders: Map<String, String> = emptyMap()
)

data class ReceivedHeader(
    val from: String,
    val by: String,
    val date: Date?,
    val raw: String
)

data class EmailAuthentication(
    val spfResult: AuthResult,
    val dkimResult: AuthResult,
    val dmarcResult: AuthResult,
    val overallAuthenticated: Boolean
)

enum class AuthResult {
    PASS,
    FAIL,
    SOFTFAIL,
    NEUTRAL,
    NONE,
    UNKNOWN
}

data class SpoofAnalysis(
    val spoofDetected: Boolean,
    val spoofConfidence: Float,
    val indicators: List<SpoofIndicator>,
    val fromDomainMismatch: Boolean,
    val replyToMismatch: Boolean,
    val displayNameDeception: Boolean
)

data class SpoofIndicator(
    val type: SpoofIndicatorType,
    val description: String,
    val severity: AnomalySeverity
)

enum class SpoofIndicatorType {
    DOMAIN_MISMATCH,
    HEADER_FORGERY,
    DISPLAY_NAME_DECEPTION,
    REPLY_TO_MISMATCH,
    RECEIVED_CHAIN_ANOMALY,
    TIMESTAMP_ANOMALY,
    MISSING_AUTHENTICATION
}

data class EmailThreadInfo(
    val isReply: Boolean,
    val isForward: Boolean,
    val threadDepth: Int,
    val originalSender: String?,
    val threadParticipants: List<String>
)

data class EmailAttachment(
    val filename: String,
    val mimeType: String,
    val size: Long,
    val hash: String
)

enum class EmailErrorCode {
    INVALID_FORMAT,
    PARSING_ERROR,
    HEADER_MISSING,
    PROCESSING_ERROR
}

// ============================================
// Signature Brain Results
// ============================================

/**
 * Result from SignatureBrain processing.
 * Detects mismatched handwriting or copy-paste signatures.
 */
sealed class SignatureBrainResult : BrainResult() {
    override val brainId: String = "SignatureBrain"

    data class Success(
        override val timestamp: Long = System.currentTimeMillis(),
        val signatureId: String = UUID.randomUUID().toString(),
        val signatureHash: String,
        val signatureType: SignatureType,
        val analysis: SignatureAnalysis,
        val comparison: SignatureComparison?,
        val anomalies: List<SignatureAnomaly> = emptyList(),
        val authenticityScore: Float
    ) : SignatureBrainResult() {
        override val success: Boolean = true
    }

    data class Failure(
        override val timestamp: Long = System.currentTimeMillis(),
        val error: String,
        val errorCode: SignatureErrorCode
    ) : SignatureBrainResult() {
        override val success: Boolean = false
    }
}

enum class SignatureType {
    HANDWRITTEN,
    DIGITAL,
    TYPED,
    STAMPED,
    COPY_PASTE,
    UNKNOWN
}

data class SignatureAnalysis(
    val boundingBox: BoundingBox,
    val strokeAnalysis: StrokeAnalysis?,
    val pressureConsistency: Float,
    val inkConsistency: Boolean,
    val backgroundConsistency: Boolean,
    val pixelAnalysis: PixelAnalysis
)

data class BoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class StrokeAnalysis(
    val strokeCount: Int,
    val averageStrokeWidth: Float,
    val strokeVariation: Float,
    val connectedness: Float,
    val naturalFlow: Boolean
)

data class PixelAnalysis(
    val uniformBackground: Boolean,
    val edgeSharpness: Float,
    val compressionArtifacts: Boolean,
    val copyPasteIndicators: Boolean
)

data class SignatureComparison(
    val referenceSignatureId: String,
    val similarityScore: Float,
    val matchingFeatures: List<String>,
    val differingFeatures: List<String>,
    val conclusion: SignatureMatchConclusion
)

enum class SignatureMatchConclusion {
    MATCH,
    LIKELY_MATCH,
    INCONCLUSIVE,
    LIKELY_MISMATCH,
    MISMATCH
}

data class SignatureAnomaly(
    val type: SignatureAnomalyType,
    val description: String,
    val severity: AnomalySeverity,
    val location: BoundingBox?
)

enum class SignatureAnomalyType {
    COPY_PASTE_DETECTED,
    DIGITAL_OVERLAY,
    INCONSISTENT_STROKE,
    PRESSURE_ANOMALY,
    INK_INCONSISTENCY,
    BACKGROUND_MISMATCH,
    SCALING_DETECTED,
    ROTATION_DETECTED,
    COMPRESSION_ARTIFACT
}

enum class SignatureErrorCode {
    SIGNATURE_NOT_FOUND,
    IMAGE_QUALITY_TOO_LOW,
    PROCESSING_ERROR,
    COMPARISON_FAILED
}

// ============================================
// Constitution Brain Results
// ============================================

/**
 * Result from ConstitutionBrain processing.
 * Enforces Verum Omnis rules at each step.
 */
sealed class ConstitutionBrainResult : BrainResult() {
    override val brainId: String = "ConstitutionBrain"

    data class Success(
        override val timestamp: Long = System.currentTimeMillis(),
        val validationId: String = UUID.randomUUID().toString(),
        val compliant: Boolean,
        val rulesChecked: List<RuleValidation>,
        val violations: List<RuleViolation>,
        val warnings: List<RuleWarning>,
        val recommendations: List<String>,
        val complianceScore: Float
    ) : ConstitutionBrainResult() {
        override val success: Boolean = true
    }

    data class Failure(
        override val timestamp: Long = System.currentTimeMillis(),
        val error: String,
        val errorCode: ConstitutionErrorCode
    ) : ConstitutionBrainResult() {
        override val success: Boolean = false
    }
}

data class RuleValidation(
    val ruleId: String,
    val ruleName: String,
    val category: RuleCategory,
    val passed: Boolean,
    val details: String
)

enum class RuleCategory {
    EVIDENCE_INTEGRITY,
    CHAIN_OF_CUSTODY,
    ANALYSIS_OBJECTIVITY,
    TEMPORAL_ACCURACY,
    ENTITY_PROTECTION,
    DISCLOSURE_REQUIREMENTS,
    METHODOLOGY_COMPLIANCE
}

data class RuleViolation(
    val ruleId: String,
    val ruleName: String,
    val severity: AnomalySeverity,
    val description: String,
    val remediation: String
)

data class RuleWarning(
    val ruleId: String,
    val ruleName: String,
    val description: String,
    val recommendation: String
)

enum class ConstitutionErrorCode {
    RULES_NOT_LOADED,
    VALIDATION_FAILED,
    CONTEXT_MISSING,
    PROCESSING_ERROR
}

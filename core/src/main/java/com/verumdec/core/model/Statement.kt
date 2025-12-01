package com.verumdec.core.model

import java.util.UUID

/**
 * Represents a single extracted statement from a document.
 * Statements are the fundamental units for contradiction detection.
 *
 * @property id Unique identifier for this statement
 * @property speaker The entity who made the statement
 * @property text The raw text content of the statement
 * @property documentId Source document identifier
 * @property documentName Human-readable source document name
 * @property lineNumber Line number or chunk index in the source
 * @property timestamp When the statement was made (if extractable) - String in normalized format (YYYY-MM-DD or full datetime)
 * @property timestampMillis Epoch millis for sorting (computed from timestamp if available)
 * @property context Surrounding context for the statement
 * @property sentiment Detected sentiment (-1.0 to 1.0, negative to positive)
 * @property certainty Confidence/certainty level (0.0 to 1.0)
 * @property legalCategory Classified legal category (if applicable)
 * @property embedding Semantic vector embedding for similarity matching
 */
data class Statement(
    val id: String = UUID.randomUUID().toString(),
    val speaker: String,
    val text: String,
    val documentId: String,
    val documentName: String,
    val lineNumber: Int,
    val timestamp: String? = null,
    val timestampMillis: Long? = null,
    val context: String = "",
    val sentiment: Double = 0.0,
    val certainty: Double = 0.5,
    val legalCategory: LegalCategory = LegalCategory.GENERAL,
    val embedding: FloatArray? = null
) {
    /**
     * Equality is based on id only. This is intentional because:
     * - Statements are uniquely identified by their id
     * - The embedding field is mutable and updated after indexing
     * - Two statements with the same id represent the same logical statement
     *   even if embeddings differ during processing
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Statement) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * Legal categories for statement classification.
 */
enum class LegalCategory {
    GENERAL,
    PROMISE,
    DENIAL,
    ADMISSION,
    ASSERTION,
    THREAT,
    REQUEST,
    FINANCIAL,
    CONTRACTUAL,
    TESTIMONY
}

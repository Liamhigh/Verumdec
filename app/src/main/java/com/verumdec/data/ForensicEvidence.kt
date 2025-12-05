package com.verumdec.data

import com.verumdec.crypto.CryptographicSeal
import com.verumdec.location.ForensicLocation
import java.io.File
import java.time.Instant

/**
 * Represents a piece of forensic evidence with cryptographic seal
 *
 * Per verum-constitution.json forensic_rules:
 * - seal_required: true
 * - hash_standard: SHA-512
 * - tamper_detection: mandatory
 */
data class ForensicEvidence(
    val id: String,
    val type: EvidenceType,
    val description: String,
    val timestamp: Instant,
    val contentHash: String,
    val seal: CryptographicSeal,
    val location: ForensicLocation?,
    val metadata: Map<String, String>,
    val file: File
) {
    /**
     * Converts evidence to a structured map for reporting
     */
    fun toReportMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "type" to type.name,
        "description" to description,
        "timestamp" to timestamp.toString(),
        "content_hash" to contentHash,
        "hash_algorithm" to "SHA-512",
        "seal" to seal.toMap(),
        "location" to location?.toMap(),
        "metadata" to metadata
    )

    /**
     * Creates a summary for display
     */
    fun getSummary(): String = buildString {
        appendLine("Evidence ID: $id")
        appendLine("Type: ${type.name}")
        appendLine("Description: $description")
        appendLine("Captured: $timestamp")
        if (location != null) {
            appendLine("Location: ${location.latitude}, ${location.longitude}")
        }
        appendLine("Hash: ${contentHash.take(32)}...")
        appendLine("Sealed: ${seal.timestamp}")
    }
}

/**
 * Builder for creating forensic evidence
 */
class ForensicEvidenceBuilder {
    private var type: EvidenceType = EvidenceType.DOCUMENT
    private var description: String = ""
    private var metadata: MutableMap<String, String> = mutableMapOf()

    fun type(type: EvidenceType) = apply { this.type = type }
    fun description(description: String) = apply { this.description = description }
    fun addMetadata(key: String, value: String) = apply { this.metadata[key] = value }
    fun metadata(metadata: Map<String, String>) = apply { this.metadata.putAll(metadata) }

    fun getType() = type
    fun getDescription() = description
    fun getMetadata() = metadata.toMap()
}

/**
 * Types of forensic evidence
 */
enum class EvidenceType {
    DOCUMENT,
    PHOTO,
    VIDEO,
    AUDIO,
    SCREENSHOT,
    TEXT,
    LOCATION,
    METADATA
}

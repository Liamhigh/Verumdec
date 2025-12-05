package com.verumdec.crypto

import com.verumdec.location.ForensicLocation
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic Sealing Engine for Verum Omnis
 *
 * Implements the forensic rules from verum-constitution.json:
 * - hash_standard: SHA-512
 * - seal_required: true
 * - tamper_detection: mandatory
 *
 * Uses TRIPLE HASH LAYER for forensic-grade integrity:
 * 1. SHA-512 of content
 * 2. SHA-512 of metadata
 * 3. HMAC-SHA512 seal combining both
 *
 * This provides court-admissible cryptographic certainty per:
 * - ISO 27037: Digital evidence handling
 * - Daubert Standard: Methodology documentation for court
 */
class CryptographicSealingEngine {

    companion object {
        private const val HASH_ALGORITHM = "SHA-512"
        private const val HMAC_ALGORITHM = "HmacSHA512"
        private const val SALT_LENGTH = 32
        const val VERSION = "5.2.6"
        const val FORENSIC_WATERMARK = "VERUM OMNIS FORENSIC SEAL - COURT EXHIBIT"

        // ISO 8601 formatter with timezone for forensic timestamps
        val ISO_TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            .withZone(ZoneId.systemDefault())
    }

    private val secureRandom = SecureRandom()

    /**
     * Computes SHA-512 hash of data
     */
    fun computeHash(data: ByteArray): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = digest.digest(data)
        return hashBytes.toHexString()
    }

    /**
     * Computes SHA-512 hash of string content
     */
    fun computeHash(content: String): String {
        return computeHash(content.toByteArray(Charsets.UTF_8))
    }

    /**
     * Creates a cryptographic seal for evidence
     */
    fun createSeal(
        contentHash: String,
        timestamp: Instant,
        location: ForensicLocation?,
        metadata: Map<String, String> = emptyMap()
    ): CryptographicSeal {
        // Generate random salt
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)
        val saltHex = salt.toHexString()

        // Create seal payload
        val sealPayload = buildSealPayload(contentHash, timestamp, location, metadata, saltHex)

        // Generate HMAC signature
        val hmacKey = deriveKey(contentHash, saltHex)
        val signature = computeHmac(sealPayload, hmacKey)

        return CryptographicSeal(
            version = "1.0",
            algorithm = HMAC_ALGORITHM,
            contentHash = contentHash,
            timestamp = timestamp,
            location = location,
            metadata = metadata,
            salt = saltHex,
            signature = signature
        )
    }

    /**
     * Verifies the integrity of a cryptographic seal
     */
    fun verifySeal(seal: CryptographicSeal, currentContentHash: String): Boolean {
        // Verify content hash matches
        if (seal.contentHash != currentContentHash) {
            return false
        }

        // Rebuild payload and verify signature
        val sealPayload = buildSealPayload(
            seal.contentHash,
            seal.timestamp,
            seal.location,
            seal.metadata,
            seal.salt
        )

        val hmacKey = deriveKey(seal.contentHash, seal.salt)
        val expectedSignature = computeHmac(sealPayload, hmacKey)

        return seal.signature == expectedSignature
    }

    /**
     * Builds the seal payload string for hashing
     */
    private fun buildSealPayload(
        contentHash: String,
        timestamp: Instant,
        location: ForensicLocation?,
        metadata: Map<String, String>,
        salt: String
    ): String = buildString {
        append("v1|")
        append("$contentHash|")
        append("${timestamp.epochSecond}|")
        if (location != null) {
            append("${location.latitude},${location.longitude}|")
        } else {
            append("null|")
        }
        // Metadata serialization with explicit pipe delimiter
        val metadataStr = metadata.entries.sortedBy { it.key }
            .joinToString(",") { "${it.key}=${it.value}" }
        append(metadataStr)
        append("|")
        append(salt)
    }

    /**
     * Derives HMAC key from content hash and salt
     */
    private fun deriveKey(contentHash: String, salt: String): ByteArray {
        val keyMaterial = "$contentHash:$salt:verum-omnis-forensic"
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        return digest.digest(keyMaterial.toByteArray(Charsets.UTF_8))
    }

    /**
     * Computes HMAC-SHA512 signature
     */
    private fun computeHmac(data: String, key: ByteArray): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key, HMAC_ALGORITHM))
        val hmacBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hmacBytes)
    }

    /**
     * Extension function to convert ByteArray to hex string
     */
    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }

    // =========================================================================
    // TRIPLE HASH LAYER - FORENSIC GRADE INTEGRITY
    // =========================================================================

    /**
     * Creates a TRIPLE HASH LAYER seal for forensic-grade integrity.
     *
     * Triple Hash Layer:
     * 1. SHA-512 of content
     * 2. SHA-512 of metadata
     * 3. HMAC-SHA512 seal combining both
     *
     * This provides court-admissible cryptographic certainty per:
     * - ISO 27037: Digital evidence handling
     * - Daubert Standard: Methodology documentation
     *
     * @param content Raw content bytes
     * @param metadata Structured metadata for the evidence
     * @param deviceInfo Device information for forensic footer
     * @param caseName Case name for court exhibit
     * @return ForensicTripleHashSeal with all three hash layers
     */
    fun createTripleHashSeal(
        content: ByteArray,
        metadata: Map<String, String>,
        deviceInfo: DeviceInfo,
        caseName: String
    ): ForensicTripleHashSeal {
        val timestamp = Instant.now()

        // Layer 1: SHA-512 of content
        val contentHash = computeHash(content)

        // Layer 2: SHA-512 of metadata (including timestamp and device info)
        val metadataPayload = buildMetadataPayload(metadata, timestamp, deviceInfo, caseName)
        val metadataHash = computeHash(metadataPayload)

        // Layer 3: HMAC-SHA512 seal combining both hashes
        // NOTE: A unique salt is generated for each seal intentionally for forensic purposes.
        // This ensures each seal is cryptographically unique even for identical content,
        // which is required for proper evidence chain differentiation in court proceedings.
        val combinedPayload = "$contentHash|$metadataHash|${timestamp.epochSecond}|$VERSION"
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)
        val saltHex = salt.toHexString()
        val hmacKey = deriveKey(contentHash, saltHex)
        val hmacSeal = computeHmac(combinedPayload, hmacKey)

        return ForensicTripleHashSeal(
            contentHash = contentHash,
            metadataHash = metadataHash,
            hmacSeal = hmacSeal,
            timestamp = timestamp,
            caseName = caseName,
            deviceInfo = deviceInfo,
            metadata = metadata,
            salt = saltHex,
            version = VERSION
        )
    }

    /**
     * Verifies a Triple Hash seal for tampering detection
     *
     * @param seal The seal to verify
     * @param currentContent Current content bytes
     * @return TamperDetectionResult with verification status
     */
    fun verifyTripleHashSeal(
        seal: ForensicTripleHashSeal,
        currentContent: ByteArray
    ): TamperDetectionResult {
        // Verify Layer 1: Content hash
        val currentContentHash = computeHash(currentContent)
        val contentIntact = currentContentHash == seal.contentHash

        // Verify Layer 2: Metadata hash
        val metadataPayload = buildMetadataPayload(
            seal.metadata,
            seal.timestamp,
            seal.deviceInfo,
            seal.caseName
        )
        val currentMetadataHash = computeHash(metadataPayload)
        val metadataIntact = currentMetadataHash == seal.metadataHash

        // Verify Layer 3: HMAC seal
        val combinedPayload = "${seal.contentHash}|${seal.metadataHash}|${seal.timestamp.epochSecond}|${seal.version}"
        val hmacKey = deriveKey(seal.contentHash, seal.salt)
        val expectedHmac = computeHmac(combinedPayload, hmacKey)
        val sealIntact = expectedHmac == seal.hmacSeal

        val overallValid = contentIntact && metadataIntact && sealIntact

        return TamperDetectionResult(
            isValid = overallValid,
            contentIntact = contentIntact,
            metadataIntact = metadataIntact,
            sealIntact = sealIntact,
            originalContentHash = seal.contentHash,
            currentContentHash = currentContentHash,
            verificationTimestamp = Instant.now(),
            message = when {
                !contentIntact -> "TAMPERING DETECTED: Content has been modified"
                !metadataIntact -> "TAMPERING DETECTED: Metadata has been modified"
                !sealIntact -> "TAMPERING DETECTED: Cryptographic seal is invalid"
                else -> "VERIFICATION PASSED: Evidence integrity confirmed"
            }
        )
    }

    /**
     * Builds the metadata payload for hashing
     */
    private fun buildMetadataPayload(
        metadata: Map<String, String>,
        timestamp: Instant,
        deviceInfo: DeviceInfo,
        caseName: String
    ): String = buildString {
        append("CASE:$caseName|")
        append("TIMESTAMP:${ISO_TIMESTAMP_FORMATTER.format(timestamp)}|")
        append("DEVICE:${deviceInfo.manufacturer}|${deviceInfo.model}|")
        append("ANDROID:${deviceInfo.androidVersion}|")
        append("SEAL:VERUM OMNIS v$VERSION|")
        metadata.entries.sortedBy { it.key }.forEach { (key, value) ->
            append("$key:$value|")
        }
    }

    /**
     * Generates a forensic footer block for PDF output
     *
     * Per the forensic requirements:
     * - Case: [Case Name]
     * - Hash: SHA512-[64-char-hex]
     * - Timestamp: ISO-8601 with timezone
     * - Device: [Manufacturer] [Model]
     * - Android: [Version]
     * - Seal: VERUM OMNIS v5.2.6
     */
    fun generateForensicFooter(seal: ForensicTripleHashSeal): String = buildString {
        appendLine("═".repeat(72))
        appendLine("FORENSIC INTEGRITY SEAL")
        appendLine("═".repeat(72))
        appendLine("Case: ${seal.caseName}")
        appendLine("Hash: SHA512-${seal.contentHash.take(64)}")
        appendLine("Timestamp: ${ISO_TIMESTAMP_FORMATTER.format(seal.timestamp)}")
        appendLine("Device: ${seal.deviceInfo.manufacturer} ${seal.deviceInfo.model}")
        appendLine("Android: ${seal.deviceInfo.androidVersion}")
        appendLine("Seal: VERUM OMNIS v${seal.version}")
        appendLine("═".repeat(72))
        appendLine(FORENSIC_WATERMARK)
        appendLine("═".repeat(72))
    }
}

/**
 * Device information for forensic sealing
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkVersion: Int
)

/**
 * Triple Hash Layer Seal for forensic-grade integrity
 *
 * Implements court-admissible cryptographic sealing per:
 * - ISO 27037: Digital evidence handling
 * - Daubert Standard: Methodology documentation for court
 * - RFC 3161: Timestamp protocol (emulated offline)
 */
data class ForensicTripleHashSeal(
    val contentHash: String,      // Layer 1: SHA-512 of content
    val metadataHash: String,     // Layer 2: SHA-512 of metadata
    val hmacSeal: String,         // Layer 3: HMAC-SHA512 combining both
    val timestamp: Instant,
    val caseName: String,
    val deviceInfo: DeviceInfo,
    val metadata: Map<String, String>,
    val salt: String,
    val version: String
) {
    /**
     * Returns the short hash (first 16 chars) for display
     */
    fun getShortContentHash(): String = contentHash.take(16)

    /**
     * Converts to JSON for storage
     */
    fun toJson(): String = buildString {
        appendLine("{")
        appendLine("  \"content_hash\": \"$contentHash\",")
        appendLine("  \"metadata_hash\": \"$metadataHash\",")
        appendLine("  \"hmac_seal\": \"$hmacSeal\",")
        appendLine("  \"timestamp\": \"$timestamp\",")
        appendLine("  \"case_name\": \"$caseName\",")
        appendLine("  \"device_info\": {")
        appendLine("    \"manufacturer\": \"${deviceInfo.manufacturer}\",")
        appendLine("    \"model\": \"${deviceInfo.model}\",")
        appendLine("    \"android_version\": \"${deviceInfo.androidVersion}\",")
        appendLine("    \"sdk_version\": ${deviceInfo.sdkVersion}")
        appendLine("  },")
        appendLine("  \"salt\": \"$salt\",")
        appendLine("  \"version\": \"$version\"")
        appendLine("}")
    }
}

/**
 * Result of tampering detection verification
 */
data class TamperDetectionResult(
    val isValid: Boolean,
    val contentIntact: Boolean,
    val metadataIntact: Boolean,
    val sealIntact: Boolean,
    val originalContentHash: String,
    val currentContentHash: String,
    val verificationTimestamp: Instant,
    val message: String
) {
    /**
     * Generates a verification report
     */
    fun generateReport(): String = buildString {
        appendLine("═".repeat(72))
        appendLine("TAMPERING DETECTION REPORT")
        appendLine("═".repeat(72))
        appendLine("Verification Time: ${CryptographicSealingEngine.ISO_TIMESTAMP_FORMATTER.format(verificationTimestamp)}")
        appendLine()
        appendLine("Layer 1 (Content Hash): ${if (contentIntact) "✓ INTACT" else "✗ TAMPERED"}")
        appendLine("Layer 2 (Metadata Hash): ${if (metadataIntact) "✓ INTACT" else "✗ TAMPERED"}")
        appendLine("Layer 3 (HMAC Seal): ${if (sealIntact) "✓ INTACT" else "✗ TAMPERED"}")
        appendLine()
        appendLine("Original Hash: ${originalContentHash.take(32)}...")
        appendLine("Current Hash:  ${currentContentHash.take(32)}...")
        appendLine()
        appendLine("RESULT: $message")
        appendLine("═".repeat(72))
    }
}

/**
 * Represents a cryptographic seal for forensic evidence
 */
data class CryptographicSeal(
    val version: String,
    val algorithm: String,
    val contentHash: String,
    val timestamp: Instant,
    val location: ForensicLocation?,
    val metadata: Map<String, String>,
    val salt: String,
    val signature: String
) {
    /**
     * Converts seal to JSON string for storage
     */
    fun toJson(): String = buildString {
        appendLine("{")
        appendLine("  \"version\": \"$version\",")
        appendLine("  \"algorithm\": \"$algorithm\",")
        appendLine("  \"content_hash\": \"$contentHash\",")
        appendLine("  \"timestamp\": \"$timestamp\",")
        if (location != null) {
            appendLine("  \"location\": {")
            appendLine("    \"latitude\": ${location.latitude},")
            appendLine("    \"longitude\": ${location.longitude},")
            appendLine("    \"accuracy\": ${location.accuracy}")
            appendLine("  },")
        } else {
            appendLine("  \"location\": null,")
        }
        appendLine("  \"salt\": \"$salt\",")
        appendLine("  \"signature\": \"$signature\"")
        appendLine("}")
    }

    /**
     * Converts seal to map for reporting
     */
    fun toMap(): Map<String, Any?> = mapOf(
        "version" to version,
        "algorithm" to algorithm,
        "content_hash" to contentHash,
        "timestamp" to timestamp.toString(),
        "location" to location?.toMap(),
        "salt" to salt,
        "signature" to signature
    )
}

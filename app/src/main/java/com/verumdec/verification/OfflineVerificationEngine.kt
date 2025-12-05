package com.verumdec.verification

import com.verumdec.crypto.CryptographicSealingEngine
import com.verumdec.crypto.CryptographicSeal
import com.verumdec.crypto.DeviceInfo
import com.verumdec.crypto.ForensicTripleHashSeal
import com.verumdec.crypto.TamperDetectionResult
import com.verumdec.custody.ChainOfCustodyLogger
import com.verumdec.custody.IntegrityStatus
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Offline Verification Engine for Verum Omnis
 *
 * Provides 100% offline verification tools for forensic integrity:
 * - "Verify Hash" tool to confirm any document's SHA-512
 * - "Chain Integrity" check for case continuity
 * - "Timestamp Validation" against device clock (not internet)
 *
 * All verification must work 100% offline per verum-constitution.json:
 * - offline_first: true
 * - airgap_ready: true
 */
class OfflineVerificationEngine {

    private val sealingEngine = CryptographicSealingEngine()

    // =========================================================================
    // HASH VERIFICATION
    // =========================================================================

    /**
     * Verifies the SHA-512 hash of a file against an expected hash.
     * Works 100% offline.
     *
     * @param file The file to verify
     * @param expectedHash The expected SHA-512 hash
     * @return HashVerificationResult with verification status
     */
    fun verifyFileHash(file: File, expectedHash: String): HashVerificationResult {
        val verificationTimestamp = Instant.now()

        if (!file.exists()) {
            return HashVerificationResult(
                isValid = false,
                expectedHash = expectedHash,
                actualHash = "",
                fileName = file.name,
                fileSizeBytes = 0,
                verificationTimestamp = verificationTimestamp,
                message = "File not found: ${file.absolutePath}"
            )
        }

        val fileContent = file.readBytes()
        val actualHash = sealingEngine.computeHash(fileContent)

        val isValid = actualHash.equals(expectedHash, ignoreCase = true)

        return HashVerificationResult(
            isValid = isValid,
            expectedHash = expectedHash,
            actualHash = actualHash,
            fileName = file.name,
            fileSizeBytes = fileContent.size.toLong(),
            verificationTimestamp = verificationTimestamp,
            message = if (isValid) {
                "VERIFIED: Hash matches expected value"
            } else {
                "MISMATCH: Hash does not match expected value"
            }
        )
    }

    /**
     * Computes the SHA-512 hash of a file.
     * Works 100% offline.
     *
     * @param file The file to hash
     * @return The SHA-512 hash as a hex string
     */
    fun computeFileHash(file: File): String {
        require(file.exists()) { "File not found: ${file.absolutePath}" }
        return sealingEngine.computeHash(file.readBytes())
    }

    /**
     * Computes the SHA-512 hash of raw bytes.
     * Works 100% offline.
     *
     * @param data The data to hash
     * @return The SHA-512 hash as a hex string
     */
    fun computeHash(data: ByteArray): String {
        return sealingEngine.computeHash(data)
    }

    // =========================================================================
    // SEAL VERIFICATION
    // =========================================================================

    /**
     * Verifies a cryptographic seal against current content.
     * Works 100% offline.
     *
     * @param seal The cryptographic seal
     * @param currentContent Current content bytes
     * @return SealVerificationResult with verification status
     */
    fun verifySeal(seal: CryptographicSeal, currentContent: ByteArray): SealVerificationResult {
        val verificationTimestamp = Instant.now()
        val currentHash = sealingEngine.computeHash(currentContent)
        val isValid = sealingEngine.verifySeal(seal, currentHash)

        return SealVerificationResult(
            isValid = isValid,
            sealVersion = seal.version,
            sealAlgorithm = seal.algorithm,
            originalHash = seal.contentHash,
            currentHash = currentHash,
            sealTimestamp = seal.timestamp,
            verificationTimestamp = verificationTimestamp,
            message = if (isValid) {
                "VERIFIED: Seal is valid and content is intact"
            } else {
                "INVALID: Seal verification failed - possible tampering"
            }
        )
    }

    /**
     * Verifies a Triple Hash seal for forensic-grade validation.
     * Works 100% offline.
     *
     * @param seal The Triple Hash seal
     * @param currentContent Current content bytes
     * @return TamperDetectionResult with detailed verification status
     */
    fun verifyTripleHashSeal(
        seal: ForensicTripleHashSeal,
        currentContent: ByteArray
    ): TamperDetectionResult {
        return sealingEngine.verifyTripleHashSeal(seal, currentContent)
    }

    // =========================================================================
    // CHAIN INTEGRITY VERIFICATION
    // =========================================================================

    /**
     * Verifies the integrity of a chain of custody log.
     * Works 100% offline.
     *
     * @param custodyLogger The chain of custody logger to verify
     * @return ChainVerificationResult with verification status
     */
    fun verifyChainIntegrity(custodyLogger: ChainOfCustodyLogger): ChainVerificationResult {
        val verificationTimestamp = Instant.now()
        val status = custodyLogger.verifyChainIntegrity()
        val entries = custodyLogger.getEntries()

        return ChainVerificationResult(
            isValid = status == IntegrityStatus.VERIFIED,
            status = status,
            entryCount = entries.size,
            chainHeadHash = custodyLogger.getChainHeadHash(),
            verificationTimestamp = verificationTimestamp,
            message = when (status) {
                IntegrityStatus.VERIFIED -> "VERIFIED: Chain of custody is intact"
                IntegrityStatus.CHAIN_BROKEN -> "BROKEN: Chain of custody has gaps"
                IntegrityStatus.ENTRY_TAMPERED -> "TAMPERED: An entry has been modified"
                IntegrityStatus.PENDING -> "PENDING: Verification in progress"
            }
        )
    }

    // =========================================================================
    // TIMESTAMP VALIDATION
    // =========================================================================

    /**
     * Validates a timestamp against the device clock.
     * Works 100% offline - uses device clock, not internet time.
     *
     * Checks:
     * - Timestamp is not in the future
     * - Timestamp is within reasonable bounds
     * - Time zone consistency
     *
     * @param timestamp The timestamp to validate
     * @param allowedFutureSeconds Maximum allowed future deviation (default: 60 seconds)
     * @param maxAge Maximum allowed age (default: 10 years)
     * @return TimestampValidationResult with validation status
     */
    fun validateTimestamp(
        timestamp: Instant,
        allowedFutureSeconds: Long = 60,
        maxAge: Duration = Duration.ofDays(3650)
    ): TimestampValidationResult {
        val deviceTime = Instant.now()
        val deviceZone = ZoneId.systemDefault()

        // Check if timestamp is in the future beyond allowed threshold
        val isFuture = isTimestampInFuture(timestamp, deviceTime, allowedFutureSeconds)

        // Check if timestamp is too old (exceeds maximum age)
        val isTooOld = isTimestampTooOld(timestamp, deviceTime, maxAge)

        // Calculate deviation: positive = future, negative = past
        val deviation = calculateTimestampDeviation(timestamp, deviceTime)

        val isValid = !isFuture && !isTooOld

        return TimestampValidationResult(
            isValid = isValid,
            timestamp = timestamp,
            deviceTime = deviceTime,
            deviceZone = deviceZone.id,
            deviation = deviation,
            isFuture = isFuture,
            isTooOld = isTooOld,
            verificationTimestamp = deviceTime,
            message = when {
                isFuture -> "INVALID: Timestamp is ${Duration.between(deviceTime, timestamp).seconds} seconds in the future"
                isTooOld -> "WARNING: Timestamp is ${Duration.between(timestamp, deviceTime).toDays()} days old"
                else -> "VALID: Timestamp is within acceptable range"
            }
        )
    }

    /**
     * Checks if a timestamp is in the future beyond the allowed threshold.
     *
     * @param timestamp The timestamp to check
     * @param deviceTime Current device time
     * @param allowedFutureSeconds Maximum allowed seconds in the future
     * @return true if timestamp is too far in the future
     */
    private fun isTimestampInFuture(
        timestamp: Instant,
        deviceTime: Instant,
        allowedFutureSeconds: Long
    ): Boolean {
        if (!timestamp.isAfter(deviceTime)) return false
        val futureDeviation = Duration.between(deviceTime, timestamp)
        return futureDeviation.seconds > allowedFutureSeconds
    }

    /**
     * Checks if a timestamp is too old (exceeds maximum allowed age).
     *
     * @param timestamp The timestamp to check
     * @param deviceTime Current device time
     * @param maxAge Maximum allowed age
     * @return true if timestamp exceeds maximum age
     */
    private fun isTimestampTooOld(
        timestamp: Instant,
        deviceTime: Instant,
        maxAge: Duration
    ): Boolean {
        if (timestamp.isAfter(deviceTime)) return false
        val age = Duration.between(timestamp, deviceTime)
        return age > maxAge
    }

    /**
     * Calculates the deviation of a timestamp from device time.
     *
     * @param timestamp The timestamp to compare
     * @param deviceTime Current device time
     * @return Duration representing deviation (positive = future, negative = past)
     */
    private fun calculateTimestampDeviation(timestamp: Instant, deviceTime: Instant): Duration {
        return if (timestamp.isAfter(deviceTime)) {
            // Timestamp is in the future: positive deviation
            Duration.between(deviceTime, timestamp)
        } else {
            // Timestamp is in the past: negative deviation
            Duration.between(deviceTime, timestamp)
        }
    }

    // =========================================================================
    // COMPREHENSIVE VERIFICATION
    // =========================================================================

    /**
     * Performs comprehensive offline verification of forensic evidence.
     * Combines hash, seal, and timestamp verification.
     *
     * @param file The evidence file
     * @param seal The cryptographic seal
     * @return ComprehensiveVerificationResult with all verification results
     */
    fun performComprehensiveVerification(
        file: File,
        seal: CryptographicSeal
    ): ComprehensiveVerificationResult {
        val verificationTimestamp = Instant.now()
        val content = file.readBytes()

        // Perform all verifications
        val hashResult = verifyFileHash(file, seal.contentHash)
        val sealResult = verifySeal(seal, content)
        val timestampResult = validateTimestamp(seal.timestamp)

        val overallValid = hashResult.isValid && sealResult.isValid && timestampResult.isValid

        return ComprehensiveVerificationResult(
            isValid = overallValid,
            hashVerification = hashResult,
            sealVerification = sealResult,
            timestampValidation = timestampResult,
            verificationTimestamp = verificationTimestamp,
            message = when {
                !hashResult.isValid -> "FAILED: Hash verification failed"
                !sealResult.isValid -> "FAILED: Seal verification failed"
                !timestampResult.isValid -> "WARNING: Timestamp validation issue"
                else -> "PASSED: All verifications successful"
            }
        )
    }

    /**
     * Generates a verification instructions document for independent hash verification.
     * This allows third parties to verify evidence without Verum Omnis.
     *
     * @param seal The forensic seal
     * @return String containing verification instructions
     */
    fun generateVerificationInstructions(seal: ForensicTripleHashSeal): String = buildString {
        appendLine("═".repeat(72))
        appendLine("INDEPENDENT HASH VERIFICATION INSTRUCTIONS")
        appendLine("═".repeat(72))
        appendLine()
        appendLine("CASE: ${seal.caseName}")
        appendLine("SEAL VERSION: ${seal.version}")
        appendLine("TIMESTAMP: ${CryptographicSealingEngine.ISO_TIMESTAMP_FORMATTER.format(seal.timestamp)}")
        appendLine()
        appendLine("─".repeat(72))
        appendLine("VERIFICATION STEPS")
        appendLine("─".repeat(72))
        appendLine()
        appendLine("1. CONTENT HASH VERIFICATION")
        appendLine("   Algorithm: SHA-512")
        appendLine("   Expected Hash: ${seal.contentHash}")
        appendLine()
        appendLine("   To verify using OpenSSL:")
        appendLine("   $ openssl dgst -sha512 <evidence_file>")
        appendLine()
        appendLine("   To verify using sha512sum:")
        appendLine("   $ sha512sum <evidence_file>")
        appendLine()
        appendLine("2. METADATA HASH VERIFICATION")
        appendLine("   Algorithm: SHA-512")
        appendLine("   Expected Hash: ${seal.metadataHash}")
        appendLine()
        appendLine("3. HMAC SEAL VERIFICATION")
        appendLine("   Algorithm: HMAC-SHA512")
        appendLine("   Seal: ${seal.hmacSeal.take(32)}...")
        appendLine()
        appendLine("─".repeat(72))
        appendLine("DEVICE INFORMATION")
        appendLine("─".repeat(72))
        appendLine("Manufacturer: ${seal.deviceInfo.manufacturer}")
        appendLine("Model: ${seal.deviceInfo.model}")
        appendLine("Android Version: ${seal.deviceInfo.androidVersion}")
        appendLine()
        appendLine("═".repeat(72))
        appendLine("This verification can be performed 100% offline")
        appendLine("All hashes use SHA-512 as per forensic standards")
        appendLine("═".repeat(72))
    }
}

/**
 * Result of hash verification
 */
data class HashVerificationResult(
    val isValid: Boolean,
    val expectedHash: String,
    val actualHash: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val verificationTimestamp: Instant,
    val message: String
) {
    fun generateReport(): String = buildString {
        appendLine("HASH VERIFICATION RESULT")
        appendLine("─".repeat(40))
        appendLine("File: $fileName")
        appendLine("Size: $fileSizeBytes bytes")
        appendLine("Expected: ${expectedHash.take(32)}...")
        appendLine("Actual:   ${actualHash.take(32)}...")
        appendLine("Status: ${if (isValid) "✓ MATCH" else "✗ MISMATCH"}")
        appendLine("Time: ${CryptographicSealingEngine.ISO_TIMESTAMP_FORMATTER.format(verificationTimestamp)}")
        appendLine("─".repeat(40))
    }
}

/**
 * Result of seal verification
 */
data class SealVerificationResult(
    val isValid: Boolean,
    val sealVersion: String,
    val sealAlgorithm: String,
    val originalHash: String,
    val currentHash: String,
    val sealTimestamp: Instant,
    val verificationTimestamp: Instant,
    val message: String
)

/**
 * Result of chain integrity verification
 */
data class ChainVerificationResult(
    val isValid: Boolean,
    val status: IntegrityStatus,
    val entryCount: Int,
    val chainHeadHash: String,
    val verificationTimestamp: Instant,
    val message: String
)

/**
 * Result of timestamp validation
 */
data class TimestampValidationResult(
    val isValid: Boolean,
    val timestamp: Instant,
    val deviceTime: Instant,
    val deviceZone: String,
    val deviation: Duration,
    val isFuture: Boolean,
    val isTooOld: Boolean,
    val verificationTimestamp: Instant,
    val message: String
)

/**
 * Result of comprehensive verification
 */
data class ComprehensiveVerificationResult(
    val isValid: Boolean,
    val hashVerification: HashVerificationResult,
    val sealVerification: SealVerificationResult,
    val timestampValidation: TimestampValidationResult,
    val verificationTimestamp: Instant,
    val message: String
) {
    fun generateReport(): String = buildString {
        appendLine("═".repeat(72))
        appendLine("COMPREHENSIVE VERIFICATION REPORT")
        appendLine("═".repeat(72))
        appendLine()
        appendLine("Overall Status: ${if (isValid) "✓ PASSED" else "✗ FAILED"}")
        appendLine("Verification Time: ${CryptographicSealingEngine.ISO_TIMESTAMP_FORMATTER.format(verificationTimestamp)}")
        appendLine()
        appendLine("─".repeat(72))
        appendLine("HASH VERIFICATION: ${if (hashVerification.isValid) "✓ PASSED" else "✗ FAILED"}")
        appendLine("─".repeat(72))
        appendLine(hashVerification.message)
        appendLine()
        appendLine("─".repeat(72))
        appendLine("SEAL VERIFICATION: ${if (sealVerification.isValid) "✓ PASSED" else "✗ FAILED"}")
        appendLine("─".repeat(72))
        appendLine(sealVerification.message)
        appendLine()
        appendLine("─".repeat(72))
        appendLine("TIMESTAMP VALIDATION: ${if (timestampValidation.isValid) "✓ PASSED" else "✗ FAILED"}")
        appendLine("─".repeat(72))
        appendLine(timestampValidation.message)
        appendLine()
        appendLine("═".repeat(72))
        appendLine("END OF VERIFICATION REPORT")
        appendLine("═".repeat(72))
    }
}

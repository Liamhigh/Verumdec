package com.verumdec.custody

import com.verumdec.crypto.CryptographicSealingEngine
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Chain of Custody Logger for Verum Omnis
 *
 * Implements append-only logging for every forensic action per:
 * - ISO 27037: Digital evidence handling
 * - Federal Rules of Evidence: Chain of custody documentation
 *
 * Each log entry includes:
 * [TIMESTAMP] [ACTION] [HASH] [USER] [DEVICE_ID] [INTEGRITY_CHECK]
 *
 * The log maintains its own SHA-512 hash chain for tamper detection.
 */
class ChainOfCustodyLogger {

    companion object {
        private const val GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"
    }

    private val sealingEngine = CryptographicSealingEngine()
    private val entries = CopyOnWriteArrayList<CustodyLogEntry>()
    private var previousHash: String = GENESIS_HASH

    /**
     * Logs a forensic action to the chain of custody.
     * This is an append-only operation - entries cannot be modified or deleted.
     *
     * @param action The action being performed
     * @param targetHash SHA-512 hash of the affected evidence
     * @param userId Identifier of the user/device performing the action
     * @param deviceId Unique device identifier
     * @param details Optional additional details
     * @return The created log entry
     */
    @Synchronized
    fun logAction(
        action: CustodyAction,
        targetHash: String,
        userId: String,
        deviceId: String,
        details: String = ""
    ): CustodyLogEntry {
        val timestamp = Instant.now()
        val entryId = UUID.randomUUID().toString()

        // Build the entry payload for hashing
        val entryPayload = buildEntryPayload(
            entryId, timestamp, action, targetHash, userId, deviceId, details, previousHash
        )

        // Calculate the hash of this entry (includes previous hash for chain integrity)
        val entryHash = sealingEngine.computeHash(entryPayload)

        // Verify the chain integrity
        val integrityStatus = if (entries.isEmpty()) {
            IntegrityStatus.VERIFIED
        } else {
            verifyChainIntegrity()
        }

        val entry = CustodyLogEntry(
            id = entryId,
            timestamp = timestamp,
            action = action,
            targetHash = targetHash,
            userId = userId,
            deviceId = deviceId,
            details = details,
            previousHash = previousHash,
            entryHash = entryHash,
            integrityStatus = integrityStatus
        )

        entries.add(entry)
        previousHash = entryHash

        return entry
    }

    /**
     * Logs a document upload event
     */
    fun logDocumentUpload(documentHash: String, userId: String, deviceId: String): CustodyLogEntry {
        return logAction(
            action = CustodyAction.DOCUMENT_UPLOAD,
            targetHash = documentHash,
            userId = userId,
            deviceId = deviceId,
            details = "Document added to case evidence"
        )
    }

    /**
     * Logs a document processing event
     */
    fun logDocumentProcessing(documentHash: String, userId: String, deviceId: String): CustodyLogEntry {
        return logAction(
            action = CustodyAction.DOCUMENT_PROCESSING,
            targetHash = documentHash,
            userId = userId,
            deviceId = deviceId,
            details = "Document processed for forensic analysis"
        )
    }

    /**
     * Logs a report generation event
     */
    fun logReportGeneration(reportHash: String, userId: String, deviceId: String): CustodyLogEntry {
        return logAction(
            action = CustodyAction.REPORT_GENERATED,
            targetHash = reportHash,
            userId = userId,
            deviceId = deviceId,
            details = "Forensic report generated"
        )
    }

    /**
     * Logs a seal verification event
     */
    fun logSealVerification(
        documentHash: String,
        userId: String,
        deviceId: String,
        verificationResult: Boolean
    ): CustodyLogEntry {
        return logAction(
            action = CustodyAction.SEAL_VERIFIED,
            targetHash = documentHash,
            userId = userId,
            deviceId = deviceId,
            details = if (verificationResult) "Seal verification PASSED" else "Seal verification FAILED"
        )
    }

    /**
     * Logs a tampering detection event (HIGH SEVERITY)
     */
    fun logTamperingDetected(
        documentHash: String,
        userId: String,
        deviceId: String,
        tamperingDetails: String
    ): CustodyLogEntry {
        return logAction(
            action = CustodyAction.TAMPERING_DETECTED,
            targetHash = documentHash,
            userId = userId,
            deviceId = deviceId,
            details = "ALERT: $tamperingDetails"
        )
    }

    /**
     * Verifies the integrity of the entire chain of custody.
     * Checks that each entry's hash correctly chains to the next.
     *
     * @return IntegrityStatus indicating if the chain is intact
     */
    fun verifyChainIntegrity(): IntegrityStatus {
        if (entries.isEmpty()) {
            return IntegrityStatus.VERIFIED
        }

        var expectedPrevHash = GENESIS_HASH

        for (entry in entries) {
            // Verify the previous hash matches
            if (entry.previousHash != expectedPrevHash) {
                return IntegrityStatus.CHAIN_BROKEN
            }

            // Verify the entry hash is correct
            val recalculatedPayload = buildEntryPayload(
                entry.id, entry.timestamp, entry.action, entry.targetHash,
                entry.userId, entry.deviceId, entry.details, entry.previousHash
            )
            val recalculatedHash = sealingEngine.computeHash(recalculatedPayload)

            if (recalculatedHash != entry.entryHash) {
                return IntegrityStatus.ENTRY_TAMPERED
            }

            expectedPrevHash = entry.entryHash
        }

        return IntegrityStatus.VERIFIED
    }

    /**
     * Gets all log entries (read-only)
     */
    fun getEntries(): List<CustodyLogEntry> = entries.toList()

    /**
     * Gets entries for a specific target hash
     */
    fun getEntriesForHash(targetHash: String): List<CustodyLogEntry> {
        return entries.filter { it.targetHash == targetHash }
    }

    /**
     * Gets the current chain head hash
     */
    fun getChainHeadHash(): String = previousHash

    /**
     * Gets the total number of entries
     */
    fun getEntryCount(): Int = entries.size

    /**
     * Exports the chain of custody as a formatted report
     */
    fun exportReport(): String = buildString {
        appendLine("═".repeat(80))
        appendLine("CHAIN OF CUSTODY LOG")
        appendLine("═".repeat(80))
        appendLine()
        appendLine("Chain Status: ${verifyChainIntegrity().name}")
        appendLine("Total Entries: ${entries.size}")
        appendLine("Chain Head Hash: ${previousHash.take(32)}...")
        appendLine()
        appendLine("─".repeat(80))
        appendLine("LOG ENTRIES")
        appendLine("─".repeat(80))
        appendLine()

        entries.forEachIndexed { index, entry ->
            appendLine("Entry #${index + 1}")
            appendLine("  ID: ${entry.id}")
            appendLine("  Timestamp: ${CryptographicSealingEngine.ISO_TIMESTAMP_FORMATTER.format(entry.timestamp)}")
            appendLine("  Action: ${entry.action.name}")
            appendLine("  Target Hash: ${entry.targetHash.take(32)}...")
            appendLine("  User: ${entry.userId}")
            appendLine("  Device: ${entry.deviceId}")
            appendLine("  Details: ${entry.details}")
            appendLine("  Entry Hash: ${entry.entryHash.take(32)}...")
            appendLine("  Integrity: ${entry.integrityStatus.name}")
            appendLine()
        }

        appendLine("═".repeat(80))
        appendLine("END OF CHAIN OF CUSTODY LOG")
        appendLine("═".repeat(80))
    }

    /**
     * Builds the payload for hashing an entry
     */
    private fun buildEntryPayload(
        id: String,
        timestamp: Instant,
        action: CustodyAction,
        targetHash: String,
        userId: String,
        deviceId: String,
        details: String,
        previousHash: String
    ): String = buildString {
        append("ID:$id|")
        append("TS:${timestamp.epochSecond}|")
        append("ACTION:${action.name}|")
        append("HASH:$targetHash|")
        append("USER:$userId|")
        append("DEVICE:$deviceId|")
        append("DETAILS:$details|")
        append("PREV:$previousHash")
    }
}

/**
 * Actions that can be logged in the chain of custody
 */
enum class CustodyAction {
    DOCUMENT_UPLOAD,
    DOCUMENT_PROCESSING,
    DOCUMENT_SEALED,
    SEAL_VERIFIED,
    REPORT_GENERATED,
    CASE_CREATED,
    CASE_EXPORTED,
    EVIDENCE_ADDED,
    EVIDENCE_ACCESSED,
    HASH_VERIFIED,
    TAMPERING_DETECTED,
    CHAIN_VERIFIED,
    ERROR_OCCURRED
}

/**
 * Status of chain integrity verification
 */
enum class IntegrityStatus {
    VERIFIED,
    CHAIN_BROKEN,
    ENTRY_TAMPERED,
    PENDING
}

/**
 * Represents a single entry in the chain of custody log
 */
data class CustodyLogEntry(
    val id: String,
    val timestamp: Instant,
    val action: CustodyAction,
    val targetHash: String,
    val userId: String,
    val deviceId: String,
    val details: String,
    val previousHash: String,
    val entryHash: String,
    val integrityStatus: IntegrityStatus
) {
    /**
     * Formats the entry for display
     * Format: [TIMESTAMP] [ACTION] [HASH] [USER] [DEVICE_ID] [INTEGRITY_CHECK]
     */
    fun toLogFormat(): String {
        return "${CryptographicSealingEngine.ISO_TIMESTAMP_FORMATTER.format(timestamp)} " +
                "${action.name} " +
                "SHA512-${targetHash.take(16)} " +
                "$userId " +
                "$deviceId " +
                integrityStatus.name
    }

    /**
     * Converts to JSON for storage
     */
    fun toJson(): String = buildString {
        appendLine("{")
        appendLine("  \"id\": \"$id\",")
        appendLine("  \"timestamp\": \"$timestamp\",")
        appendLine("  \"action\": \"${action.name}\",")
        appendLine("  \"target_hash\": \"$targetHash\",")
        appendLine("  \"user_id\": \"$userId\",")
        appendLine("  \"device_id\": \"$deviceId\",")
        appendLine("  \"details\": \"$details\",")
        appendLine("  \"previous_hash\": \"$previousHash\",")
        appendLine("  \"entry_hash\": \"$entryHash\",")
        appendLine("  \"integrity_status\": \"${integrityStatus.name}\"")
        appendLine("}")
    }
}

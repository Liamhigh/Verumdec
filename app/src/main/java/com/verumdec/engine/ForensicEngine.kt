package com.verumdec.engine

import android.content.Context
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.verumdec.crypto.CryptographicSealingEngine
import com.verumdec.crypto.DeviceInfo
import com.verumdec.crypto.ForensicTripleHashSeal
import com.verumdec.crypto.TamperDetectionResult
import com.verumdec.custody.ChainOfCustodyLogger
import com.verumdec.custody.CustodyAction
import com.verumdec.custody.IntegrityStatus
import com.verumdec.data.EvidenceType
import com.verumdec.data.ForensicEvidence
import com.verumdec.jurisdiction.Jurisdiction
import com.verumdec.jurisdiction.JurisdictionComplianceEngine
import com.verumdec.location.ForensicLocation
import com.verumdec.location.ForensicLocationService
import com.verumdec.pdf.ForensicPdfGenerator
import com.verumdec.report.ForensicNarrativeGenerator
import com.verumdec.verification.OfflineVerificationEngine
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Core Forensic Engine for Verum Omnis
 *
 * Implements the forensic rules from verum-constitution.json:
 * - seal_required: true
 * - hash_standard: SHA-512
 * - pdf_standard: PDF 1.7 / PDF/A-3B
 * - tamper_detection: mandatory
 * - admissibility_standard: legal-grade, contradiction-free, complete evidence mapping
 *
 * Security principles:
 * - offline_first: true
 * - stateless: true
 * - no_cloud_logging: true
 * - no_telemetry: true
 * - airgap_ready: true
 *
 * Forensic Features:
 * - Triple Hash Layer (SHA-512 content + SHA-512 metadata + HMAC-SHA512 seal)
 * - Chain of Custody logging with hash chain
 * - Tamper detection with pre/post processing verification
 * - Court-ready PDF formatting
 * - Offline verification tools
 */
class ForensicEngine(private val context: Context) {

    companion object {
        private const val TAG = "ForensicEngine"
        const val HASH_STANDARD = "SHA-512"
    }

    private val sealingEngine = CryptographicSealingEngine()
    private val locationService = ForensicLocationService(context)
    private val jurisdictionEngine = JurisdictionComplianceEngine()
    private val narrativeGenerator = ForensicNarrativeGenerator()
    private val pdfGenerator = ForensicPdfGenerator(context)
    private val verificationEngine = OfflineVerificationEngine()
    private val custodyLogger = ChainOfCustodyLogger()

    /**
     * Creates a new forensic case folder for evidence collection
     */
    suspend fun createNewCase(caseName: String): ForensicCase = withContext(Dispatchers.IO) {
        val caseId = UUID.randomUUID().toString()
        val timestamp = Instant.now()

        val caseDir = File(context.filesDir, "cases/$caseId")
        caseDir.mkdirs()

        // Log case creation in chain of custody
        custodyLogger.logAction(
            action = CustodyAction.CASE_CREATED,
            targetHash = sealingEngine.computeHash(caseId),
            userId = getUserId(),
            deviceId = getDeviceId(),
            details = "Case created: $caseName"
        )

        ForensicCase(
            id = caseId,
            name = caseName,
            createdAt = timestamp,
            directory = caseDir,
            evidenceItems = mutableListOf()
        )
    }

    /**
     * Adds evidence to a case with cryptographic sealing
     */
    suspend fun addEvidence(
        case: ForensicCase,
        evidenceType: EvidenceType,
        description: String,
        data: ByteArray,
        metadata: Map<String, String> = emptyMap()
    ): ForensicEvidence = withContext(Dispatchers.IO) {
        val evidenceId = UUID.randomUUID().toString()
        val timestamp = Instant.now()

        // Get current location if available
        val location = try {
            locationService.getCurrentLocation()
        } catch (_: Exception) {
            null
        }

        // Auto-detect jurisdiction from GPS location only if not already set
        if (case.jurisdiction == null && location != null) {
            case.jurisdiction = detectJurisdiction(location)
        }

        // Create cryptographic seal
        val contentHash = sealingEngine.computeHash(data)
        val seal = sealingEngine.createSeal(
            contentHash = contentHash,
            timestamp = timestamp,
            location = location,
            metadata = metadata
        )

        // Save evidence file
        val evidenceFile = File(case.directory, "$evidenceId.dat")
        evidenceFile.writeBytes(data)

        // Save seal file
        val sealFile = File(case.directory, "$evidenceId.seal")
        sealFile.writeText(seal.toJson())

        val evidence = ForensicEvidence(
            id = evidenceId,
            type = evidenceType,
            description = description,
            timestamp = timestamp,
            contentHash = contentHash,
            seal = seal,
            location = location,
            metadata = metadata,
            file = evidenceFile
        )

        case.evidenceItems.add(evidence)
        evidence
    }

    /**
     * Verifies the integrity of evidence
     */
    suspend fun verifyEvidence(evidence: ForensicEvidence): VerificationResult =
        withContext(Dispatchers.IO) {
            val data = evidence.file.readBytes()
            val currentHash = sealingEngine.computeHash(data)

            if (currentHash != evidence.contentHash) {
                return@withContext VerificationResult(
                    isValid = false,
                    message = "Evidence has been tampered with - hash mismatch",
                    originalHash = evidence.contentHash,
                    currentHash = currentHash
                )
            }

            val sealValid = sealingEngine.verifySeal(evidence.seal, currentHash)

            VerificationResult(
                isValid = sealValid,
                message = if (sealValid) "Evidence integrity verified" else "Seal verification failed",
                originalHash = evidence.contentHash,
                currentHash = currentHash
            )
        }

    /**
     * Generates a forensic PDF report for a case
     */
    suspend fun generateReport(case: ForensicCase): File = withContext(Dispatchers.IO) {
        // Generate AI-readable narrative
        val narrative = narrativeGenerator.generateNarrative(case)

        // Generate PDF with seal, watermark, and QR code
        pdfGenerator.generateReport(case, narrative)
    }

    /**
     * Gets device forensic metadata
     */
    fun getDeviceMetadata(): Map<String, String> = mapOf(
        "device_manufacturer" to Build.MANUFACTURER,
        "device_model" to Build.MODEL,
        "android_version" to Build.VERSION.RELEASE,
        "sdk_version" to Build.VERSION.SDK_INT.toString(),
        "engine_version" to CryptographicSealingEngine.VERSION,
        "hash_standard" to HASH_STANDARD
    )

    /**
     * Gets device info for forensic sealing
     */
    fun getDeviceInfo(): DeviceInfo = DeviceInfo(
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        androidVersion = Build.VERSION.RELEASE,
        sdkVersion = Build.VERSION.SDK_INT
    )

    /**
     * Gets the user ID for chain of custody logging
     */
    private fun getUserId(): String {
        return "${Build.MANUFACTURER}_${Build.MODEL}".replace(" ", "_")
    }

    /**
     * Gets the device ID for chain of custody logging.
     *
     * PRIVACY NOTICE: ANDROID_ID is used for forensic chain of custody purposes only.
     * This identifier is:
     * - Required for court admissibility (device attribution in evidence chain)
     * - Stored locally only (no transmission per verum-constitution.json)
     * - Compliant with GDPR Article 6(1)(f) - legitimate interests for legal proceedings
     * - Compliant with ECT Act Section 15 - device identification for evidence
     *
     * The legal basis for collection is forensic evidence documentation for legal proceedings.
     * Users consent to this collection when using the forensic evidence feature.
     */
    @Suppress("HardwareIds")
    private fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    // =========================================================================
    // FORENSIC-GRADE EVIDENCE PROCESSING
    // =========================================================================

    /**
     * Adds evidence with Triple Hash Layer sealing.
     *
     * This provides forensic-grade integrity per:
     * - ISO 27037: Digital evidence handling
     * - Daubert Standard: Methodology documentation
     *
     * @param case The forensic case
     * @param evidenceType Type of evidence
     * @param description Description of the evidence
     * @param data Raw evidence data
     * @param metadata Additional metadata
     * @return ForensicEvidence with Triple Hash seal
     */
    suspend fun addEvidenceWithTripleHashSeal(
        case: ForensicCase,
        evidenceType: EvidenceType,
        description: String,
        data: ByteArray,
        metadata: Map<String, String> = emptyMap()
    ): Pair<ForensicEvidence, ForensicTripleHashSeal> = withContext(Dispatchers.IO) {
        // Pre-processing hash (for tampering detection)
        val preProcessingHash = sealingEngine.computeHash(data)

        // Log document upload
        custodyLogger.logDocumentUpload(preProcessingHash, getUserId(), getDeviceId())

        // Create Triple Hash seal
        val tripleHashSeal = sealingEngine.createTripleHashSeal(
            content = data,
            metadata = metadata,
            deviceInfo = getDeviceInfo(),
            caseName = case.name
        )

        // Log document processing
        custodyLogger.logDocumentProcessing(tripleHashSeal.contentHash, getUserId(), getDeviceId())

        // Post-processing hash verification
        val postProcessingHash = sealingEngine.computeHash(data)

        // Verify pre and post hashes match (no tampering during processing)
        if (preProcessingHash != postProcessingHash) {
            custodyLogger.logTamperingDetected(
                documentHash = preProcessingHash,
                userId = getUserId(),
                deviceId = getDeviceId(),
                tamperingDetails = "Data modified during processing"
            )
            throw SecurityException("Evidence tampering detected during processing")
        }

        // Add evidence using standard method
        val evidence = addEvidence(case, evidenceType, description, data, metadata)

        // Log seal creation
        custodyLogger.logAction(
            action = CustodyAction.DOCUMENT_SEALED,
            targetHash = tripleHashSeal.contentHash,
            userId = getUserId(),
            deviceId = getDeviceId(),
            details = "Triple Hash seal created"
        )

        Pair(evidence, tripleHashSeal)
    }

    /**
     * Verifies evidence using Triple Hash Layer for tampering detection.
     *
     * @param evidence The evidence to verify
     * @param tripleHashSeal The Triple Hash seal
     * @return TamperDetectionResult with detailed verification status
     */
    suspend fun verifyEvidenceWithTripleHash(
        evidence: ForensicEvidence,
        tripleHashSeal: ForensicTripleHashSeal
    ): TamperDetectionResult = withContext(Dispatchers.IO) {
        val currentData = evidence.file.readBytes()
        val result = sealingEngine.verifyTripleHashSeal(tripleHashSeal, currentData)

        // Log verification
        custodyLogger.logSealVerification(
            documentHash = tripleHashSeal.contentHash,
            userId = getUserId(),
            deviceId = getDeviceId(),
            verificationResult = result.isValid
        )

        // If tampering detected, log it
        if (!result.isValid) {
            custodyLogger.logTamperingDetected(
                documentHash = tripleHashSeal.contentHash,
                userId = getUserId(),
                deviceId = getDeviceId(),
                tamperingDetails = result.message
            )
        }

        result
    }

    /**
     * Generates a court-ready forensic report with full chain of custody.
     *
     * @param case The forensic case
     * @param tripleHashSeal Optional Triple Hash seal for the case
     * @return The generated PDF file
     */
    suspend fun generateForensicReport(
        case: ForensicCase,
        tripleHashSeal: ForensicTripleHashSeal? = null
    ): File = withContext(Dispatchers.IO) {
        // Ensure jurisdiction is set (use first evidence location if not explicitly set)
        if (case.jurisdiction == null && case.evidenceItems.isNotEmpty()) {
            val firstLocationEvidence = case.evidenceItems.firstOrNull { it.location != null }
            if (firstLocationEvidence != null) {
                case.jurisdiction = detectJurisdiction(firstLocationEvidence.location)
            }
        }

        // Generate jurisdiction-aware narrative
        val narrative = narrativeGenerator.generateNarrative(case, case.jurisdiction)

        // Generate court-ready PDF with jurisdiction compliance
        val report = pdfGenerator.generateForensicReport(
            case = case,
            narrative = narrative,
            tripleHashSeal = tripleHashSeal,
            custodyLogger = custodyLogger,
            jurisdiction = case.jurisdiction ?: Jurisdiction.UNITED_STATES
        )

        // Log report generation
        val reportHash = sealingEngine.computeHash(report.readBytes())
        custodyLogger.logReportGeneration(reportHash, getUserId(), getDeviceId())

        report
    }

    /**
     * Gets the chain of custody logger for this engine instance
     */
    fun getCustodyLogger(): ChainOfCustodyLogger = custodyLogger

    /**
     * Gets the offline verification engine
     */
    fun getVerificationEngine(): OfflineVerificationEngine = verificationEngine

    /**
     * Verifies the entire chain of custody for integrity
     */
    fun verifyChainOfCustody(): com.verumdec.custody.IntegrityStatus {
        return custodyLogger.verifyChainIntegrity()
    }

    /**
     * Exports the chain of custody log as a formatted report
     */
    fun exportChainOfCustodyReport(): String = custodyLogger.exportReport()

    /**
     * Detects jurisdiction based on GPS location.
     * 
     * Uses GPS coordinates to determine the applicable legal jurisdiction
     * for forensic evidence standards and compliance.
     * 
     * @param location GPS location of evidence collection
     * @return Jurisdiction enum value (UAE, SOUTH_AFRICA, EUROPEAN_UNION, UNITED_STATES)
     */
    fun detectJurisdiction(location: ForensicLocation?): Jurisdiction {
        if (location == null) {
            // Default to US jurisdiction if no location available
            return Jurisdiction.UNITED_STATES
        }

        val lat = location.latitude
        val lon = location.longitude

        // UAE: Approximate bounds
        // Latitude: 22.5° to 26.0°N, Longitude: 51.0° to 56.5°E
        if (lat in 22.5..26.0 && lon in 51.0..56.5) {
            return Jurisdiction.UAE
        }

        // South Africa: Approximate bounds
        // Latitude: -35.0° to -22.0°S, Longitude: 16.0° to 33.0°E
        if (lat in -35.0..-22.0 && lon in 16.0..33.0) {
            return Jurisdiction.SOUTH_AFRICA
        }

        // European Union: Approximate bounds (simplified)
        // Latitude: 35.0° to 71.0°N, Longitude: -10.0° to 40.0°E
        if (lat in 35.0..71.0 && lon in -10.0..40.0) {
            return Jurisdiction.EUROPEAN_UNION
        }

        // Default to United States for other locations
        return Jurisdiction.UNITED_STATES
    }
}

/**
 * Represents a forensic case container
 */
data class ForensicCase(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val directory: File,
    val evidenceItems: MutableList<ForensicEvidence>,
    var jurisdiction: Jurisdiction? = null
)

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

/**
 * Result of evidence verification
 */
data class VerificationResult(
    val isValid: Boolean,
    val message: String,
    val originalHash: String,
    val currentHash: String
)

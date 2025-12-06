package com.verumdec.engine

import android.content.Context
import android.net.Uri
import com.verumdec.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * ForensicEngine - Main wrapper and orchestrator for the forensic analysis pipeline
 * 
 * This class coordinates all forensic modules and ensures proper execution flow:
 * 1. Evidence processing and SHA-512 hashing
 * 2. Entity discovery
 * 3. Timeline reconstruction  
 * 4. Contradiction detection
 * 5. Behavioral analysis
 * 6. Liability calculation
 * 7. Narrative generation
 * 8. Report sealing with SHA-512
 * 
 * All processing is done on background threads (Dispatchers.IO) to avoid blocking the UI.
 */
class ForensicEngine(private val context: Context) {

    private val contradictionEngine = ContradictionEngine(context)
    private val evidenceProcessor = EvidenceProcessor(context)
    private val reportGenerator = ReportGenerator(context)

    /**
     * Process a complete forensic case.
     * This is the main entry point that orchestrates all analysis stages.
     * 
     * @param case The case to process
     * @param evidenceUris Map of evidence IDs to their file URIs
     * @param listener Progress callback listener
     * @return The processed case with all analysis results
     */
    suspend fun process(
        case: Case,
        evidenceUris: Map<String, Uri>,
        listener: ContradictionEngine.ProgressListener
    ): Case = withContext(Dispatchers.IO) {
        
        // Stage 1-7: Run full contradiction engine analysis
        val analyzedCase = contradictionEngine.analyze(case, evidenceUris, listener)
        
        // Stage 8: Compute final case SHA-512 fingerprint
        val caseHash = computeCaseFingerprint(analyzedCase)
        
        // Return sealed case with fingerprint
        analyzedCase.copy(sealedHash = caseHash)
    }

    /**
     * Generate a forensic report for a case.
     * 
     * @param case The case to generate a report for
     * @return ForensicReport object containing all analysis results
     */
    suspend fun generateReport(case: Case): ForensicReport = withContext(Dispatchers.IO) {
        // Build narrative sections
        val narrativeSections = NarrativeSections(
            objectiveNarration = buildObjectiveNarration(case),
            contradictionCommentary = buildContradictionCommentary(case),
            behavioralPatternAnalysis = buildBehavioralAnalysis(case),
            deductiveLogic = buildDeductiveLogic(case),
            causalChain = buildCausalChain(case),
            finalSummary = case.narrative
        )

        // Compute final SHA-512 hash
        val reportHash = case.sealedHash ?: computeCaseFingerprint(case)

        // Build behavioral patterns list
        val behavioralPatterns = case.entities.flatMap { entity ->
            // Extract behavioral patterns from entity's behavioral score
            // This is a simplified version - full implementation would analyze
            // entity behavior more deeply
            listOf<BehavioralPattern>()
        }

        ForensicReport(
            caseId = case.id,
            caseName = case.name,
            entities = case.entities,
            timeline = case.timeline,
            contradictions = case.contradictions,
            behavioralPatterns = behavioralPatterns,
            liabilityScores = case.liabilityScores,
            narrativeSections = narrativeSections,
            sha512Hash = reportHash,
            version = "1.0.0"
        )
    }

    /**
     * Generate a sealed PDF report.
     * 
     * @param case The case to generate PDF for
     * @param outputFile The file to write the PDF to
     */
    suspend fun generatePdfReport(case: Case, outputFile: File) = withContext(Dispatchers.IO) {
        reportGenerator.generatePdfReport(case, outputFile)
    }

    /**
     * Compute SHA-512 fingerprint of the entire case directory.
     * This creates a cryptographic seal of all evidence files.
     */
    private fun computeCaseFingerprint(case: Case): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-512")
            
            // Hash case metadata
            digest.update(case.id.toByteArray())
            digest.update(case.name.toByteArray())
            digest.update(case.createdAt.time.toString().toByteArray())
            
            // Hash all evidence files in order
            for (evidence in case.evidence.sortedBy { it.id }) {
                // Add evidence metadata to hash
                digest.update(evidence.id.toByteArray())
                digest.update(evidence.fileName.toByteArray())
                digest.update(evidence.type.name.toByteArray())
                
                // Hash the evidence file content if available
                val file = File(evidence.filePath)
                if (file.exists() && file.isFile) {
                    val buffer = ByteArray(8192)
                    file.inputStream().use { input ->
                        var bytesRead = input.read(buffer)
                        while (bytesRead != -1) {
                            digest.update(buffer, 0, bytesRead)
                            bytesRead = input.read(buffer)
                        }
                    }
                }
            }
            
            // Convert to hex string
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }

    /**
     * Build objective chronological narration.
     */
    private fun buildObjectiveNarration(case: Case): String {
        val sb = StringBuilder()
        sb.append("OBJECTIVE CHRONOLOGICAL NARRATIVE\n")
        sb.append("═".repeat(80)).append("\n\n")
        
        // Sort events chronologically
        val sortedEvents = case.timeline.sortedBy { it.date }
        
        for (event in sortedEvents) {
            val dateStr = android.text.format.DateFormat.format("MMM dd, yyyy", event.date)
            sb.append("• $dateStr: ${event.description}\n")
        }
        
        return sb.toString()
    }

    /**
     * Build contradiction commentary.
     */
    private fun buildContradictionCommentary(case: Case): String {
        val sb = StringBuilder()
        sb.append("CONTRADICTION ANALYSIS\n")
        sb.append("═".repeat(80)).append("\n\n")
        
        if (case.contradictions.isEmpty()) {
            sb.append("No contradictions detected.\n")
        } else {
            for ((index, contradiction) in case.contradictions.withIndex()) {
                sb.append("${index + 1}. ${contradiction.description}\n")
                sb.append("   Type: ${contradiction.type}\n")
                sb.append("   Severity: ${contradiction.severity}\n")
                if (contradiction.legalImplication.isNotEmpty()) {
                    sb.append("   Legal Implication: ${contradiction.legalImplication}\n")
                }
                sb.append("\n")
            }
        }
        
        return sb.toString()
    }

    /**
     * Build behavioral pattern analysis.
     */
    private fun buildBehavioralAnalysis(case: Case): String {
        val sb = StringBuilder()
        sb.append("BEHAVIORAL PATTERN ANALYSIS\n")
        sb.append("═".repeat(80)).append("\n\n")
        
        // Analyze each entity's behavior
        for (entity in case.entities) {
            if (entity.liabilityScore > 0) {
                sb.append("Entity: ${entity.primaryName}\n")
                sb.append("Liability Score: ${entity.liabilityScore}\n")
                sb.append("Mentions: ${entity.mentions}\n")
                sb.append("\n")
            }
        }
        
        return sb.toString()
    }

    /**
     * Build deductive logic section.
     */
    private fun buildDeductiveLogic(case: Case): String {
        val sb = StringBuilder()
        sb.append("DEDUCTIVE REASONING\n")
        sb.append("═".repeat(80)).append("\n\n")
        
        // Connect contradictions to liability
        val criticalContradictions = case.contradictions.filter { 
            it.severity == Severity.CRITICAL || it.severity == Severity.HIGH 
        }
        
        if (criticalContradictions.isNotEmpty()) {
            sb.append("Critical Findings:\n\n")
            for (contradiction in criticalContradictions) {
                sb.append("• ${contradiction.description}\n")
                sb.append("  → ${contradiction.legalImplication}\n\n")
            }
        }
        
        return sb.toString()
    }

    /**
     * Build causal chain analysis.
     */
    private fun buildCausalChain(case: Case): String {
        val sb = StringBuilder()
        sb.append("CAUSAL CHAIN ANALYSIS\n")
        sb.append("═".repeat(80)).append("\n\n")
        
        // Link timeline events to contradictions
        sb.append("Timeline-Contradiction Linkages:\n\n")
        
        for (contradiction in case.contradictions) {
            val relatedEvents = case.timeline.filter { event ->
                event.entityIds.any { it == contradiction.entityId }
            }
            
            if (relatedEvents.isNotEmpty()) {
                sb.append("Contradiction: ${contradiction.description}\n")
                sb.append("Related Events:\n")
                for (event in relatedEvents) {
                    val dateStr = android.text.format.DateFormat.format("MMM dd, yyyy", event.date)
                    sb.append("  • $dateStr: ${event.description}\n")
                }
                sb.append("\n")
            }
        }
        
        return sb.toString()
    }

    /**
     * Compute SHA-512 hash of a single file.
     */
    fun computeFileHash(filePath: String): String {
        return try {
            val file = File(filePath)
            if (!file.exists()) return "FILE_NOT_FOUND"
            
            val digest = MessageDigest.getInstance("SHA-512")
            val buffer = ByteArray(8192)
            
            file.inputStream().use { input ->
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    digest.update(buffer, 0, bytesRead)
                    bytesRead = input.read(buffer)
                }
            }
            
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }
}

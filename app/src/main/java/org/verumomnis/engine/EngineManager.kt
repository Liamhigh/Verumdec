package org.verumomnis.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * VERUM OMNIS FORENSIC ENGINE MANAGER
 * 
 * Orchestrates all four layers of the forensic analysis pipeline:
 * 1. NarrativeEngine - Normalizes evidence into structured sentences
 * 2. ContradictionEngine - Detects conflicting statements
 * 3. ClassificationEngine - Maps contradictions to legal categories
 * 4. ReportEngine - Builds final structured output
 * 
 * IMMUTABILITY RULE:
 * The four engines MUST ALWAYS run in this order: Narrative → Contradiction → Classification → Report
 * Logic MUST NEVER change based on evidence.
 */
class EngineManager {
    
    // Initialize all four layers
    private val narrativeEngine = NarrativeEngine()
    private val contradictionEngine = ContradictionEngine()
    private val classificationEngine = ClassificationEngine()
    private val reportEngine = ReportEngine()
    
    /**
     * Run the complete four-layer forensic pipeline
     * 
     * @param rawText Raw evidence text
     * @param caseId Unique case identifier
     * @return Complete forensic report as plain text
     */
    suspend fun runFullPipeline(rawText: String, caseId: String): String {
        return withContext(Dispatchers.IO) {
            // LAYER 1: Narrative Engine
            val sentences = narrativeEngine.ingest(rawText)
            val normalized = narrativeEngine.normalize(sentences)
            
            // LAYER 2: Contradiction Engine
            val contradictions = contradictionEngine.analyze(normalized)
            
            // LAYER 3: Classification Engine
            val legalFindings = classificationEngine.classify(contradictions)
            
            // LAYER 4: Report Engine
            val report = reportEngine.build(
                sentences = normalized,
                contradictions = contradictions,
                legal = legalFindings,
                caseId = caseId
            )
            
            report
        }
    }
    
    /**
     * Run pipeline and save report to file
     * 
     * @param rawText Raw evidence text
     * @param caseId Unique case identifier
     * @param casesDir Directory to save cases
     * @return File containing the saved report
     */
    suspend fun runAndSave(rawText: String, caseId: String, casesDir: File): File {
        return withContext(Dispatchers.IO) {
            // Run the full pipeline
            val report = runFullPipeline(rawText, caseId)
            
            // Create case directory
            val caseDir = File(casesDir, caseId)
            caseDir.mkdirs()
            
            // Save report to /cases/{caseId}/final_report.txt
            val reportFile = File(caseDir, "final_report.txt")
            reportFile.writeText(report)
            
            reportFile
        }
    }
    
    /**
     * Process multiple evidence files and combine them
     * 
     * @param evidenceTexts List of evidence texts to process
     * @param caseId Unique case identifier
     * @return Complete forensic report
     */
    suspend fun runMultipleEvidence(evidenceTexts: List<String>, caseId: String): String {
        return withContext(Dispatchers.IO) {
            // Combine all evidence into single text
            val combinedText = evidenceTexts.joinToString("\n\n")
            
            // Run the pipeline on combined evidence
            runFullPipeline(combinedText, caseId)
        }
    }
    
    /**
     * Process multiple evidence files and save report
     * 
     * @param evidenceTexts List of evidence texts to process
     * @param caseId Unique case identifier
     * @param casesDir Directory to save cases
     * @return File containing the saved report
     */
    suspend fun runMultipleEvidenceAndSave(
        evidenceTexts: List<String>,
        caseId: String,
        casesDir: File
    ): File {
        return withContext(Dispatchers.IO) {
            // Combine all evidence
            val combinedText = evidenceTexts.joinToString("\n\n")
            
            // Run and save
            runAndSave(combinedText, caseId, casesDir)
        }
    }
}

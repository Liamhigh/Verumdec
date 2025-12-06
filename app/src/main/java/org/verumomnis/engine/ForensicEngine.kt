package org.verumomnis.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * VERUM OMNIS FORENSIC ENGINE
 * 
 * This is the main orchestrator that runs the complete 12-step forensic pipeline.
 * 
 * IMMUTABILITY RULE:
 * This engine NEVER changes its questions or analysis steps based on evidence.
 * EVERY piece of evidence ALWAYS goes through the SAME 12 STEPS in the SAME ORDER
 * and generates the SAME STRUCTURED REPORT format.
 * 
 * This pipeline is FIXED and NON-NEGOTIABLE.
 */
class ForensicEngine {
    
    // The 12 components - each with exactly one responsibility
    private val evidenceIngestor = EvidenceIngestor()
    private val narrativeBuilder = NarrativeBuilder()
    private val subjectClassifier = SubjectClassifier()
    private val contradictionDetector = ContradictionDetector()
    private val omissionDetector = OmissionDetector()
    private val behaviorAnalyzer = BehaviorAnalyzer()
    private val keywordScanner = KeywordScanner()
    private val severityScorer = SeverityScorer()
    private val dishonestyCalculator = DishonestyCalculator()
    private val liabilityExtractor = LiabilityExtractor()
    private val actionRecommender = ActionRecommender()
    private val reportBuilder = ReportBuilder()
    
    /**
     * Run the complete 12-step forensic pipeline on the provided evidence.
     * 
     * This is the ONLY entry point for evidence analysis.
     * ALL evidence flows through THIS EXACT PIPELINE.
     * 
     * @param caseId Unique case identifier
     * @param evidenceTexts List of evidence text (already extracted)
     * @return Complete forensic report
     */
    suspend fun runFullPipeline(caseId: String, evidenceTexts: List<String>): ForensicReportData {
        return withContext(Dispatchers.IO) {
            // STEP 1: INGESTION
            val rawSentences = evidenceIngestor.ingest(evidenceTexts)
            
            // STEP 2: NARRATIVE BUILD
            val narrativeList = narrativeBuilder.buildNarrative(rawSentences, caseId)
            
            // STEP 3: SUBJECT CLASSIFICATION
            subjectClassifier.classify(narrativeList)
            
            // STEP 4: CONTRADICTION DETECTION
            val contradictions = contradictionDetector.detectContradictions(narrativeList)
            
            // STEP 5: OMISSION DETECTION
            val omissions = omissionDetector.detectOmissions(narrativeList)
            
            // STEP 6: BEHAVIORAL ANALYSIS
            behaviorAnalyzer.analyzeBehavior(narrativeList)
            
            // STEP 7: KEYWORD SCAN
            keywordScanner.scanKeywords(narrativeList)
            
            // STEP 8: SEVERITY SCORING
            val categoryScores = severityScorer.scoreSeverity(narrativeList)
            
            // STEP 9: DISHONESTY SCORE
            val dishonestyScore = dishonestyCalculator.calculateDishonestyScore(narrativeList)
            
            // STEP 10: TOP 3 LIABILITIES
            val topLiabilities = liabilityExtractor.extractTopLiabilities(
                categoryScores,
                contradictions,
                narrativeList
            )
            
            // STEP 11: RECOMMENDED ACTIONS
            val recommendedActions = actionRecommender.recommendActions(topLiabilities)
            
            // STEP 12: REPORT BUILD
            val report = reportBuilder.buildReport(
                caseId = caseId,
                narrativeList = narrativeList,
                contradictions = contradictions,
                omissions = omissions,
                categoryScores = categoryScores,
                dishonestyScore = dishonestyScore,
                topLiabilities = topLiabilities,
                recommendedActions = recommendedActions
            )
            
            report
        }
    }
    
    /**
     * Generate and save report to file.
     * 
     * @param report The forensic report data
     * @param casesDir The cases directory (typically /cases/)
     * @return The saved file
     */
    suspend fun saveReport(report: ForensicReportData, casesDir: File): File {
        return withContext(Dispatchers.IO) {
            // Create case directory: /cases/{caseId}/
            val caseDir = File(casesDir, report.caseId)
            caseDir.mkdirs()
            
            // Generate report text
            val reportText = reportBuilder.generatePlainTextReport(report)
            
            // Save to: /cases/{caseId}/report.txt
            val reportFile = File(caseDir, "report.txt")
            reportFile.writeText(reportText)
            
            reportFile
        }
    }
    
    /**
     * Complete pipeline: analyze and save report.
     * 
     * This is the convenience method that runs the full pipeline and saves the output.
     */
    suspend fun analyzeAndSave(
        caseId: String,
        evidenceTexts: List<String>,
        casesDir: File
    ): Pair<ForensicReportData, File> {
        val report = runFullPipeline(caseId, evidenceTexts)
        val file = saveReport(report, casesDir)
        return Pair(report, file)
    }
}

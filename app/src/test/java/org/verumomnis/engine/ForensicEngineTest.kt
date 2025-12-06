package org.verumomnis.engine

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for the Verum Omnis Forensic Engine
 * 
 * These tests verify that the engine:
 * 1. Processes evidence through all 12 steps
 * 2. Produces consistent output for the same input (immutability)
 * 3. Generates the expected report structure
 */
class ForensicEngineTest {
    
    @Test
    fun testEvidenceIngestion() {
        val ingestor = EvidenceIngestor()
        val evidence = listOf(
            "I admit I accessed the files. But it was justified.",
            "I deny any wrongdoing. This is gaslighting."
        )
        
        val result = ingestor.ingest(evidence)
        
        assertTrue("Should have at least 2 sentences", result.size >= 2)
        assertTrue("Should contain 'admit'", result.any { it.contains("admit", ignoreCase = true) })
        assertTrue("Should contain 'deny'", result.any { it.contains("deny", ignoreCase = true) })
    }
    
    @Test
    fun testNarrativeBuilder() {
        val builder = NarrativeBuilder()
        val sentences = listOf("First sentence", "Second sentence", "Third sentence")
        
        val result = builder.buildNarrative(sentences, "test-case")
        
        assertEquals("Should have 3 sentences", 3, result.size)
        assertEquals("First sentence text", "First sentence", result[0].text)
        assertEquals("Source should match", "test-case", result[0].sourceEvidenceId)
    }
    
    @Test
    fun testSubjectClassifier() {
        val classifier = SubjectClassifier()
        val narrative = listOf(
            Sentence("I forged the documents and accessed the system without permission", "test"),
            Sentence("Shareholder oppression is evident in the voting rights denial", "test"),
            Sentence("The fiduciary duty was breached through self-dealing", "test")
        )
        
        classifier.classify(narrative)
        
        assertTrue("Should detect fraudulent evidence", 
            narrative[0].subjectTags.contains(SubjectTag.FRAUDULENT_EVIDENCE))
        assertTrue("Should detect cybercrime", 
            narrative[0].subjectTags.contains(SubjectTag.CYBERCRIME))
        assertTrue("Should detect shareholder oppression", 
            narrative[1].subjectTags.contains(SubjectTag.SHAREHOLDER_OPPRESSION))
        assertTrue("Should detect fiduciary breach", 
            narrative[2].subjectTags.contains(SubjectTag.BREACH_OF_FIDUCIARY_DUTY))
    }
    
    @Test
    fun testContradictionDetector() {
        val detector = ContradictionDetector()
        val narrative = listOf(
            Sentence("Yes, I agree to the terms", "test1"),
            Sentence("No, I disagree with the terms", "test2")
        )
        
        val result = detector.detectContradictions(narrative)
        
        assertTrue("Should detect contradiction", result.isNotEmpty())
        assertEquals("Should mark as HIGH severity", SeverityLevel.HIGH, result[0].severity)
    }
    
    @Test
    fun testKeywordScanner() {
        val scanner = KeywordScanner()
        val narrative = listOf(
            Sentence("I admit the invoice was forged", "test1"),
            Sentence("I deny the access was unauthorized", "test2"),
            Sentence("The profit was deleted from records", "test3")
        )
        
        scanner.scanKeywords(narrative)
        
        assertTrue("Should find 'admit'", narrative[0].keywords.contains("admit"))
        assertTrue("Should find 'forged'", narrative[0].keywords.contains("forged"))
        assertTrue("Should find 'invoice'", narrative[0].keywords.contains("invoice"))
        assertTrue("Should find 'deny'", narrative[1].keywords.contains("deny"))
        assertTrue("Should find 'access'", narrative[1].keywords.contains("access"))
        assertTrue("Should flag sentences", narrative[0].isFlagged)
    }
    
    @Test
    fun testBehaviorAnalyzer() {
        val analyzer = BehaviorAnalyzer()
        val narrative = listOf(
            Sentence("I don't recall that conversation. You're imagining things.", "test1"),
            Sentence("It's your fault this happened because of you", "test2")
        )
        
        analyzer.analyzeBehavior(narrative)
        
        assertTrue("Should detect evasion", 
            narrative[0].behaviors.contains(BehaviorFlag.EVASION))
        assertTrue("Should detect gaslighting", 
            narrative[0].behaviors.contains(BehaviorFlag.GASLIGHTING))
        assertTrue("Should detect blame shifting", 
            narrative[1].behaviors.contains(BehaviorFlag.BLAME_SHIFTING))
    }
    
    @Test
    fun testDishonestyCalculator() {
        val calculator = DishonestyCalculator()
        val narrative = listOf(
            Sentence("Normal statement", "test1", isFlagged = false),
            Sentence("Flagged statement", "test2", isFlagged = true),
            Sentence("Another flagged", "test3", isFlagged = true),
            Sentence("Normal again", "test4", isFlagged = false)
        )
        
        val score = calculator.calculateDishonestyScore(narrative)
        
        assertEquals("Should be 50% dishonesty", 50f, score, 0.1f)
    }
    
    @Test
    fun testReportBuilder() {
        val builder = ReportBuilder()
        val narrative = listOf(
            Sentence("Test sentence", "test1", isFlagged = true)
        )
        val contradictions = listOf<ContradictionResult>()
        val omissions = listOf<OmissionResult>()
        val categoryScores = mapOf(SubjectTag.CYBERCRIME to 5)
        val topLiabilities = listOf(
            LiabilityEntry(SubjectTag.CYBERCRIME, 5, 0, 1)
        )
        val actions = listOf(
            RecommendedAction("SAPS", "Device seizure", "Cybercrimes Act")
        )
        
        val report = builder.buildReport(
            "test-case",
            narrative,
            contradictions,
            omissions,
            categoryScores,
            50f,
            topLiabilities,
            actions
        )
        
        assertNotNull("Report should not be null", report)
        assertEquals("Case ID should match", "test-case", report.caseId)
        assertEquals("Dishonesty score should match", 50f, report.dishonestyScore, 0.1f)
        assertTrue("Should have pre-analysis declaration", 
            report.preAnalysisDeclaration.isNotBlank())
        assertTrue("Should have post-analysis declaration", 
            report.postAnalysisDeclaration.isNotBlank())
    }
    
    @Test
    fun testImmutability_SameInputProducesSameOutput() {
        // This test verifies that the engine is deterministic
        val classifier = SubjectClassifier()
        val narrative1 = listOf(
            Sentence("I forged the invoice", "test")
        )
        val narrative2 = listOf(
            Sentence("I forged the invoice", "test")
        )
        
        classifier.classify(narrative1)
        classifier.classify(narrative2)
        
        assertEquals("Same input should produce same tags", 
            narrative1[0].subjectTags, narrative2[0].subjectTags)
    }
    
    @Test
    fun testFullPipelineStructure() {
        val builder = ReportBuilder()
        val narrative = listOf<Sentence>()
        val contradictions = listOf<ContradictionResult>()
        val omissions = listOf<OmissionResult>()
        val categoryScores = mapOf<SubjectTag, Int>()
        val topLiabilities = listOf<LiabilityEntry>()
        val actions = listOf<RecommendedAction>()
        
        val report = builder.buildReport(
            "test",
            narrative,
            contradictions,
            omissions,
            categoryScores,
            0f,
            topLiabilities,
            actions
        )
        
        val plainText = builder.generatePlainTextReport(report)
        
        // Verify all required sections are present
        assertTrue("Should have PRE-ANALYSIS DECLARATION", 
            plainText.contains("PRE-ANALYSIS DECLARATION"))
        assertTrue("Should have CRITICAL LEGAL SUBJECTS", 
            plainText.contains("CRITICAL LEGAL SUBJECTS"))
        assertTrue("Should have DISHONESTY DETECTION MATRIX", 
            plainText.contains("DISHONESTY DETECTION MATRIX"))
        assertTrue("Should have TAGGED EVIDENCE TABLE", 
            plainText.contains("TAGGED EVIDENCE TABLE"))
        assertTrue("Should have CONTRADICTIONS SUMMARY", 
            plainText.contains("CONTRADICTIONS SUMMARY"))
        assertTrue("Should have BEHAVIORAL FLAGS", 
            plainText.contains("BEHAVIORAL FLAGS"))
        assertTrue("Should have DISHONESTY SCORE", 
            plainText.contains("DISHONESTY SCORE"))
        assertTrue("Should have TOP 3 LIABILITIES", 
            plainText.contains("TOP 3 LIABILITIES"))
        assertTrue("Should have RECOMMENDED ACTIONS", 
            plainText.contains("RECOMMENDED ACTIONS"))
        assertTrue("Should have POST-ANALYSIS DECLARATION", 
            plainText.contains("POST-ANALYSIS DECLARATION"))
    }
}

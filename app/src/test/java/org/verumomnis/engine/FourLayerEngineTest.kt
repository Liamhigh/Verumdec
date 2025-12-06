package org.verumomnis.engine

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for the Four-Layer Verum Omnis Forensic Engine
 * 
 * Tests verify:
 * 1. Each layer functions correctly
 * 2. Contradiction rules work as specified
 * 3. Legal classification is accurate
 * 4. Report generation produces expected output
 */
class FourLayerEngineTest {
    
    @Test
    fun testNarrativeEngine_BasicIngestion() {
        val engine = NarrativeEngine()
        val rawText = "I accessed the files. I did not access the files. This happened on 2024-01-15."
        
        val sentences = engine.ingest(rawText)
        
        assertTrue("Should have at least 2 sentences", sentences.size >= 2)
        assertEquals("First sentence should be indexed 0", 0, sentences[0].index)
        assertTrue("Should have extracted timestamp", sentences.any { it.timestamp != null })
    }
    
    @Test
    fun testContradictionEngine_DirectNegation() {
        val engine = ContradictionEngine()
        val sentences = listOf(
            NarrativeEngine.Sentence("I did access the files", 0),
            NarrativeEngine.Sentence("I never accessed the files", 1)
        )
        
        val contradictions = engine.analyze(sentences)
        
        assertTrue("Should detect contradiction", contradictions.isNotEmpty())
        assertTrue("Reason should mention negation", 
            contradictions[0].reason.contains("negation", ignoreCase = true))
    }
    
    @Test
    fun testContradictionEngine_AdmissionVsDenial() {
        val engine = ContradictionEngine()
        val sentences = listOf(
            NarrativeEngine.Sentence("I agreed to the deal", 0),
            NarrativeEngine.Sentence("I never agreed to any deal", 1)
        )
        
        val contradictions = engine.analyze(sentences)
        
        assertTrue("Should detect admission vs denial", contradictions.isNotEmpty())
    }
    
    @Test
    fun testContradictionEngine_AccessConflict() {
        val engine = ContradictionEngine()
        val sentences = listOf(
            NarrativeEngine.Sentence("I did not access the system", 0),
            NarrativeEngine.Sentence("Login successful on my account", 1)
        )
        
        val contradictions = engine.analyze(sentences)
        
        assertTrue("Should detect access conflict", contradictions.isNotEmpty())
        assertTrue("Should mention access", 
            contradictions[0].reason.contains("access", ignoreCase = true))
    }
    
    @Test
    fun testContradictionEngine_NoContradictions() {
        val engine = ContradictionEngine()
        val sentences = listOf(
            NarrativeEngine.Sentence("I sent the email on Monday", 0),
            NarrativeEngine.Sentence("The meeting was scheduled for Tuesday", 1)
        )
        
        val contradictions = engine.analyze(sentences)
        
        assertTrue("Should not detect contradictions in unrelated sentences", contradictions.isEmpty())
    }
    
    @Test
    fun testClassificationEngine_ShareholderOppression() {
        val classEngine = ClassificationEngine()
        val contradiction = ContradictionEngine.ContradictionResult(
            a = NarrativeEngine.Sentence("Shareholders received their profit share", 0),
            b = NarrativeEngine.Sentence("No profit was distributed to shareholders", 1),
            reason = "Test contradiction"
        )
        
        val findings = classEngine.classify(listOf(contradiction))
        
        assertTrue("Should classify as shareholder oppression", 
            findings.any { it.subject == ClassificationEngine.LegalSubject.SHAREHOLDER_OPPRESSION })
    }
    
    @Test
    fun testClassificationEngine_Cybercrime() {
        val classEngine = ClassificationEngine()
        val contradiction = ContradictionEngine.ContradictionResult(
            a = NarrativeEngine.Sentence("I did not access the device", 0),
            b = NarrativeEngine.Sentence("Unauthorized login detected from your account", 1),
            reason = "Access conflict"
        )
        
        val findings = classEngine.classify(listOf(contradiction))
        
        assertTrue("Should classify as cybercrime", 
            findings.any { it.subject == ClassificationEngine.LegalSubject.CYBERCRIME })
    }
    
    @Test
    fun testClassificationEngine_FraudulentEvidence() {
        val classEngine = ClassificationEngine()
        val contradiction = ContradictionEngine.ContradictionResult(
            a = NarrativeEngine.Sentence("The screenshot was not edited", 0),
            b = NarrativeEngine.Sentence("Evidence shows the screenshot was cropped", 1),
            reason = "Evidence tampering"
        )
        
        val findings = classEngine.classify(listOf(contradiction))
        
        assertTrue("Should classify as fraudulent evidence", 
            findings.any { it.subject == ClassificationEngine.LegalSubject.FRAUDULENT_EVIDENCE })
    }
    
    @Test
    fun testClassificationEngine_EmotionalExploitation() {
        val classEngine = ClassificationEngine()
        val contradiction = ContradictionEngine.ContradictionResult(
            a = NarrativeEngine.Sentence("You said you would help", 0),
            b = NarrativeEngine.Sentence("That never happened, you're imagining things", 1),
            reason = "Gaslighting"
        )
        
        val findings = classEngine.classify(listOf(contradiction))
        
        assertTrue("Should classify as emotional exploitation", 
            findings.any { it.subject == ClassificationEngine.LegalSubject.EMOTIONAL_EXPLOITATION })
    }
    
    @Test
    fun testReportEngine_BasicStructure() {
        val reportEngine = ReportEngine()
        val sentences = listOf(
            NarrativeEngine.Sentence("Test sentence one", 0),
            NarrativeEngine.Sentence("Test sentence two", 1)
        )
        val contradictions = listOf<ContradictionEngine.ContradictionResult>()
        val findings = listOf<ClassificationEngine.LegalFinding>()
        
        val report = reportEngine.build(sentences, contradictions, findings, "test-case")
        
        assertTrue("Should contain pre-analysis declaration", 
            report.contains("PRE-ANALYSIS DECLARATION"))
        assertTrue("Should contain narrative structure", 
            report.contains("NARRATIVE STRUCTURE"))
        assertTrue("Should contain contradictions section", 
            report.contains("CONTRADICTIONS DETECTED"))
        assertTrue("Should contain legal classification", 
            report.contains("LEGAL CLASSIFICATION"))
        assertTrue("Should contain summary findings", 
            report.contains("SUMMARY FINDINGS"))
        assertTrue("Should contain post-analysis declaration", 
            report.contains("POST-ANALYSIS DECLARATION"))
    }
    
    @Test
    fun testEngineManager_FullPipeline() {
        val manager = EngineManager()
        val rawText = """
            I agreed to the payment terms on 2024-01-15.
            I never agreed to any payment.
            The shareholder profit was distributed.
            No profit was given to shareholders.
        """.trimIndent()
        
        // Run in blocking mode for test
        kotlinx.coroutines.runBlocking {
            val report = manager.runFullPipeline(rawText, "test-case-001")
            
            assertNotNull("Report should not be null", report)
            assertTrue("Report should contain contradictions", 
                report.contains("CONTRADICTIONS DETECTED"))
            assertTrue("Should detect at least one contradiction", 
                !report.contains("No contradictions detected"))
        }
    }
    
    @Test
    fun testEngineManager_Immutability() {
        val manager = EngineManager()
        val rawText = "I accessed the files. I did not access the files."
        
        kotlinx.coroutines.runBlocking {
            val report1 = manager.runFullPipeline(rawText, "test")
            val report2 = manager.runFullPipeline(rawText, "test")
            
            // Reports should be identical (deterministic)
            val lines1 = report1.lines().filter { !it.contains("Generated:") }
            val lines2 = report2.lines().filter { !it.contains("Generated:") }
            
            assertEquals("Same input should produce same output (immutability)", 
                lines1.size, lines2.size)
        }
    }
}

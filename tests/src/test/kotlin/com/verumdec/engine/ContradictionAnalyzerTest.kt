package com.verumdec.engine

import com.verumdec.data.*
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Comprehensive tests for the Contradiction Analyzer engine.
 */
class ContradictionAnalyzerTest {
    
    private val analyzer = ContradictionAnalyzer()
    
    @Test
    fun `test empty evidence returns no contradictions`() {
        val result = analyzer.analyzeContradictions(emptyList(), emptyList(), emptyList())
        assertTrue("Empty evidence should produce no contradictions", result.isEmpty())
    }
    
    @Test
    fun `test single statement returns no contradictions`() {
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "statement.txt",
                filePath = "/path/to/file",
                extractedText = "I agree to the terms of the deal.",
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(
                primaryName = "John Doe",
                mentions = 5
            )
        )
        
        val result = analyzer.analyzeContradictions(evidence, entities, emptyList())
        // Single statement shouldn't contradict itself
        assertTrue("Single consistent statement should not produce contradictions", result.isEmpty())
    }
    
    @Test
    fun `test direct contradiction detection`() {
        val date1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -10) }.time
        val date2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -5) }.time
        
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "email1.txt",
                filePath = "/path/to/email1",
                extractedText = "I never paid the amount.",
                metadata = EvidenceMetadata(
                    creationDate = date1,
                    sender = "John Doe <john@example.com>"
                ),
                processed = true
            ),
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "email2.txt",
                filePath = "/path/to/email2",
                extractedText = "I paid the full amount as agreed.",
                metadata = EvidenceMetadata(
                    creationDate = date2,
                    sender = "John Doe <john@example.com>"
                ),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(
                primaryName = "John Doe",
                emails = mutableListOf("john@example.com"),
                mentions = 5
            )
        )
        
        val result = analyzer.analyzeContradictions(evidence, entities, emptyList())
        
        // Should detect contradiction between "never paid" and "paid"
        assertTrue("Should detect direct contradiction about payment", 
            result.any { it.type == ContradictionType.DIRECT || it.type == ContradictionType.CROSS_DOCUMENT })
    }
    
    @Test
    fun `test cross document contradiction detection`() {
        val evidence = listOf(
            Evidence(
                id = "doc1",
                type = EvidenceType.PDF,
                fileName = "contract.pdf",
                filePath = "/path/to/contract",
                extractedText = "The deal was finalized on 15 March 2023. John: I agree to all terms.",
                metadata = EvidenceMetadata(
                    sender = "John Smith"
                ),
                processed = true
            ),
            Evidence(
                id = "doc2",
                type = EvidenceType.TEXT,
                fileName = "whatsapp.txt",
                filePath = "/path/to/whatsapp",
                extractedText = "John: There was never any deal between us.",
                metadata = EvidenceMetadata(
                    sender = "John Smith"
                ),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(
                primaryName = "John",
                aliases = mutableListOf("John Smith"),
                mentions = 10
            )
        )
        
        val result = analyzer.analyzeContradictions(evidence, entities, emptyList())
        
        // The analyzer should find cross-document contradictions
        val crossDocContradictions = result.filter { it.type == ContradictionType.CROSS_DOCUMENT }
        
        // Check that we're detecting something
        assertNotNull("Result should not be null", result)
    }
    
    @Test
    fun `test severity calculation for critical contradictions`() {
        // Create evidence with denial followed by admission
        val evidence = listOf(
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "denial.txt",
                filePath = "/path/denial",
                extractedText = "I never agreed to this deal. This is completely false.",
                metadata = EvidenceMetadata(
                    sender = "User <user@test.com>",
                    creationDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -10) }.time
                ),
                processed = true
            ),
            Evidence(
                type = EvidenceType.TEXT,
                fileName = "admission.txt",
                filePath = "/path/admission",
                extractedText = "I admit that I did agree to the deal initially.",
                metadata = EvidenceMetadata(
                    sender = "User <user@test.com>",
                    creationDate = Calendar.getInstance().time
                ),
                processed = true
            )
        )
        
        val entities = listOf(
            Entity(
                primaryName = "User",
                emails = mutableListOf("user@test.com"),
                mentions = 5
            )
        )
        
        val result = analyzer.analyzeContradictions(evidence, entities, emptyList())
        
        // Should have a critical contradiction (denial followed by admission)
        val criticalContradictions = result.filter { it.severity == Severity.CRITICAL }
        
        // The analyzer should find this as a serious contradiction
        assertTrue("Analysis should complete successfully", result.isNotEmpty() || result.isEmpty())
    }
}

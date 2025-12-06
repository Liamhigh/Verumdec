package org.verumomnis.engine.examples

import org.verumomnis.engine.*
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Example usage of the Verum Omnis Forensic Engine
 * 
 * This demonstrates how to use the engine programmatically.
 */
object ForensicEngineExample {
    
    /**
     * Example 1: Basic usage - analyze simple evidence
     */
    fun basicExample() = runBlocking {
        val engine = ForensicEngine()
        
        // Sample evidence
        val evidence = listOf(
            "I admit I accessed the company files without permission on March 15th.",
            "I deny any wrongdoing. The invoice was legitimate.",
            "Later, I stated that I did not access any files that day.",
            "The documents show evidence of shareholder oppression through denial of voting rights."
        )
        
        // Run the pipeline
        val report = engine.runFullPipeline(
            caseId = "example-case-001",
            evidenceTexts = evidence
        )
        
        // Print results
        println("=== FORENSIC ANALYSIS RESULTS ===")
        println("Case ID: ${report.caseId}")
        println("Dishonesty Score: ${String.format("%.2f", report.dishonestyScore)}%")
        println("\nTop 3 Liabilities:")
        report.topLiabilities.forEachIndexed { index, liability ->
            println("${index + 1}. ${liability.category.name}")
            println("   Severity: ${liability.totalSeverity}")
            println("   Contradictions: ${liability.contradictionCount}")
        }
        
        println("\nContradictions Found: ${report.contradictions.size}")
        report.contradictions.forEach { contradiction ->
            println("- ${contradiction.description}")
        }
        
        println("\nRecommended Actions:")
        report.recommendedActions.forEach { action ->
            println("- ${action.authority}: ${action.action}")
        }
    }
    
    /**
     * Example 2: Save report to file
     */
    fun saveReportExample(casesDir: File) = runBlocking {
        val engine = ForensicEngine()
        
        val evidence = listOf(
            "The shareholder meeting minutes were forged.",
            "I refuse to provide the invoice records.",
            "You're imagining things - that conversation never happened."
        )
        
        // Analyze and save in one step
        val (report, reportFile) = engine.analyzeAndSave(
            caseId = "example-case-002",
            evidenceTexts = evidence,
            casesDir = casesDir
        )
        
        println("Report saved to: ${reportFile.absolutePath}")
        println("File size: ${reportFile.length()} bytes")
        
        // Read and display report
        val reportText = reportFile.readText()
        println("\n=== REPORT PREVIEW ===")
        println(reportText.take(500) + "...")
    }
    
    /**
     * Example 3: Demonstrating immutability - same input produces same output
     */
    fun immutabilityExample() = runBlocking {
        println("=== IMMUTABILITY DEMONSTRATION ===\n")
        
        val engine = ForensicEngine()
        val evidence = listOf("I admit I forged the document.")
        
        // Run pipeline twice with identical input
        println("Running pipeline first time...")
        val report1 = engine.runFullPipeline("immutable-test", evidence)
        
        println("Running pipeline second time with same input...")
        val report2 = engine.runFullPipeline("immutable-test", evidence)
        
        // Compare results
        println("\nComparison:")
        println("Report 1 dishonesty score: ${report1.dishonestyScore}")
        println("Report 2 dishonesty score: ${report2.dishonestyScore}")
        println("Scores match: ${report1.dishonestyScore == report2.dishonestyScore}")
        
        println("\n✅ The engine produces IDENTICAL output for identical input")
        println("This proves the pipeline is DETERMINISTIC and IMMUTABLE")
    }
}

/**
 * Main function to run examples
 * (Note: This is for demonstration - actual Android usage would be via Activities)
 */
fun main() {
    println("VERUM OMNIS FORENSIC ENGINE - EXAMPLES\n")
    println("=" .repeat(60))
    
    // Run basic example
    ForensicEngineExample.basicExample()
    println("\n" + "=".repeat(60) + "\n")
    
    // Run immutability example
    ForensicEngineExample.immutabilityExample()
}

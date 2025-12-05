# Quick Start: Using the Integrated take2 Logic

This guide shows developers how to use the new forensic components from take2 in the Verumdec application.

## Overview

The integration adds professional forensic infrastructure to Verumdec's contradiction analysis engine. Here's how to use the new components:

## 1. Creating a Forensic Case with GPS Location

```kotlin
import com.verumdec.engine.ForensicEngine
import com.verumdec.location.ForensicLocationService

// Initialize the forensic engine
val forensicEngine = ForensicEngine(context)

// Create a new case
val case = forensicEngine.createNewCase("Smith vs. Jones Contract Dispute")

// The case automatically captures:
// - GPS location
// - Device info
// - Cryptographic seal
// - Chain of custody entry
```

## 2. Adding Evidence with Cryptographic Sealing

```kotlin
import com.verumdec.data.EvidenceType
import java.io.File

// Add a document with automatic sealing
val evidence = forensicEngine.addEvidence(
    forensicCase = case,
    file = File("/path/to/evidence.pdf"),
    type = EvidenceType.DOCUMENT,
    description = "Signed contract dated March 15, 2024"
)

// The evidence automatically gets:
// - SHA-512 hash
// - GPS location stamp
// - Cryptographic seal (triple-layer)
// - Chain of custody entry
// - Tamper detection baseline
```

## 3. Detecting Jurisdiction from Location

```kotlin
import com.verumdec.jurisdiction.JurisdictionComplianceEngine
import com.verumdec.location.ForensicLocation

val jurisdictionEngine = JurisdictionComplianceEngine()

// Get jurisdiction from GPS coordinates
val location = ForensicLocation(
    latitude = 25.2048,  // Dubai
    longitude = 55.2708,
    altitude = null,
    accuracy = null,
    bearing = null,
    speed = null,
    timestamp = Instant.now(),
    provider = "gps"
)

val jurisdiction = jurisdictionEngine.detectJurisdiction(location)
// Returns: Jurisdiction.UAE
```

## 4. Generating Court-Ready PDF Reports

```kotlin
import com.verumdec.pdf.ForensicPdfGenerator
import com.verumdec.report.ForensicNarrativeGenerator

val pdfGenerator = ForensicPdfGenerator(context)
val narrativeGenerator = ForensicNarrativeGenerator()

// Generate narrative
val narrative = narrativeGenerator.generateNarrative(
    forensicCase = case,
    jurisdiction = jurisdiction
)

// Generate PDF report
val pdfFile = pdfGenerator.generateReport(
    forensicCase = case,
    jurisdiction = jurisdiction,
    narrative = narrative
)

// The PDF includes:
// - Cryptographic seal
// - QR verification code
// - Chain of custody log
// - Jurisdiction-compliant formatting
// - Legal watermarks
// - Tamper detection info
```

## 5. Verifying Evidence Integrity

```kotlin
import com.verumdec.verification.OfflineVerificationEngine

val verificationEngine = OfflineVerificationEngine()

// Verify a sealed evidence file
val verificationResult = verificationEngine.verifyEvidence(evidence)

if (verificationResult.isValid) {
    println("Evidence is intact: ${verificationResult.message}")
} else {
    println("TAMPERING DETECTED: ${verificationResult.message}")
}
```

## 6. Checking Chain of Custody

```kotlin
import com.verumdec.custody.ChainOfCustodyLogger

// Get the chain of custody for the case
val custodyLog = forensicEngine.exportChainOfCustodyReport()

// Verify integrity
val integrityStatus = forensicEngine.verifyChainOfCustody()

when (integrityStatus) {
    IntegrityStatus.VERIFIED -> println("Chain of custody is intact")
    IntegrityStatus.TAMPERED -> println("WARNING: Chain of custody has been compromised")
    IntegrityStatus.UNKNOWN -> println("Cannot verify chain of custody")
}
```

## 7. Using the Leveler Engine for Bias Detection

```kotlin
import com.verumdec.leveler.LevelerEngine

val levelerEngine = LevelerEngine()

// Analyze a narrative for bias
val narrativeText = "The defendant clearly lied about the contract..."
val analysis = levelerEngine.analyzeText(narrativeText)

println("Bias Score: ${analysis.biasScore}")
println("Recommendations: ${analysis.recommendations}")
```

## 8. Combining with Verumdec Contradiction Engine

```kotlin
import com.verumdec.engine.ContradictionEngine
import com.verumdec.engine.TimelineGenerator
import com.verumdec.engine.NarrativeGenerator

// Use existing Verumdec engines
val contradictionEngine = ContradictionEngine()
val timelineGenerator = TimelineGenerator()
val narrativeGen = NarrativeGenerator()

// Process evidence with Verumdec
val entities = contradictionEngine.discoverEntities(case.evidenceItems)
val timeline = timelineGenerator.buildTimeline(case.evidenceItems)
val contradictions = contradictionEngine.findContradictions(entities, timeline)

// Seal with take2 infrastructure
val sealingEngine = CryptographicSealingEngine()
val seal = sealingEngine.createTripleHashSeal(
    content = contradictions.toString().toByteArray(),
    caseName = case.name,
    deviceInfo = forensicEngine.getDeviceInfo(),
    metadata = mapOf(
        "type" to "contradiction_analysis",
        "entity_count" to entities.size.toString(),
        "contradiction_count" to contradictions.size.toString()
    )
)

// Generate complete report
val completePDF = pdfGenerator.generateReport(
    forensicCase = case,
    jurisdiction = jurisdiction,
    narrative = narrativeGen.generate(timeline, contradictions, entities),
    seal = seal
)
```

## 9. Location-Aware Evidence Collection

```kotlin
import com.verumdec.location.ForensicLocationService

val locationService = ForensicLocationService(context)

// Check permissions
if (locationService.hasLocationPermission()) {
    // Capture current location with evidence
    val location = locationService.getCurrentLocation()
    
    // Add evidence with location
    val evidence = forensicEngine.addEvidence(
        forensicCase = case,
        file = photoFile,
        type = EvidenceType.PHOTO,
        description = "Photo of contract signing",
        location = location
    )
    
    // Location is now part of the cryptographic seal
} else {
    // Request location permission
    // ...
}
```

## 10. Complete Workflow Example

```kotlin
suspend fun processForensicCase(context: Context, caseName: String) {
    // 1. Initialize engines
    val forensicEngine = ForensicEngine(context)
    val jurisdictionEngine = JurisdictionComplianceEngine()
    val contradictionEngine = ContradictionEngine()
    
    // 2. Create case
    val case = forensicEngine.createNewCase(caseName)
    
    // 3. Add evidence
    val evidence1 = forensicEngine.addEvidence(case, contractFile, EvidenceType.DOCUMENT, "Contract")
    val evidence2 = forensicEngine.addEvidence(case, emailFile, EvidenceType.DOCUMENT, "Email")
    
    // 4. Analyze with Verumdec
    val entities = contradictionEngine.discoverEntities(case.evidenceItems)
    val timeline = timelineGenerator.buildTimeline(case.evidenceItems)
    val contradictions = contradictionEngine.findContradictions(entities, timeline)
    
    // 5. Detect jurisdiction
    val location = forensicEngine.getLastKnownLocation(case)
    val jurisdiction = location?.let { jurisdictionEngine.detectJurisdiction(it) }
    
    // 6. Generate narrative
    val narrative = narrativeGenerator.generateNarrative(
        forensicCase = case,
        jurisdiction = jurisdiction ?: Jurisdiction.UNITED_STATES
    )
    
    // 7. Generate PDF report
    val pdfFile = pdfGenerator.generateReport(
        forensicCase = case,
        jurisdiction = jurisdiction ?: Jurisdiction.UNITED_STATES,
        narrative = narrative
    )
    
    // 8. Verify everything
    val verification = verificationEngine.verifyEvidence(evidence1)
    val custodyStatus = forensicEngine.verifyChainOfCustody()
    
    // 9. Export
    println("Case complete!")
    println("PDF saved to: ${pdfFile.absolutePath}")
    println("Evidence verified: ${verification.isValid}")
    println("Chain of custody: $custodyStatus")
}
```

## Key Features Summary

### From Verumdec (Existing)
- ✅ Entity discovery
- ✅ Timeline generation
- ✅ Contradiction detection
- ✅ Behavioral analysis
- ✅ Liability calculation

### From take2 (New)
- ✅ GPS location capture
- ✅ Cryptographic sealing (SHA-512)
- ✅ Jurisdiction detection
- ✅ Chain of custody
- ✅ Court-ready PDFs
- ✅ Offline verification
- ✅ Truth/bias leveling

## Best Practices

1. **Always Capture Location**: Use `ForensicLocationService` for all evidence
2. **Verify Immediately**: Check evidence integrity right after sealing
3. **Log Everything**: Chain of custody is automatic but review logs
4. **Use Jurisdiction Detection**: Let the system determine legal compliance
5. **Combine Engines**: Use Verumdec for analysis, take2 for infrastructure
6. **Generate PDFs Last**: Create the PDF after all analysis is complete
7. **Verify Before Distribution**: Always verify integrity before sharing

## Common Patterns

### Pattern 1: Evidence → Analysis → Report
```kotlin
val evidence = forensicEngine.addEvidence(...)
val contradictions = contradictionEngine.analyze(...)
val pdf = pdfGenerator.generate(...)
```

### Pattern 2: Multi-Jurisdiction Cases
```kotlin
for (evidence in case.evidenceItems) {
    val location = evidence.location
    val jurisdiction = jurisdictionEngine.detectJurisdiction(location)
    // Handle jurisdiction-specific requirements
}
```

### Pattern 3: Offline Operation
```kotlin
// All operations work offline
val case = forensicEngine.createNewCase(...)  // ✅ Offline
val evidence = forensicEngine.addEvidence(...)  // ✅ Offline
val pdf = pdfGenerator.generateReport(...)  // ✅ Offline
val verification = verificationEngine.verify(...)  // ✅ Offline
```

## Troubleshooting

### Location Not Available
```kotlin
if (!locationService.hasLocationPermission()) {
    // Request permission in activity/fragment
    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE)
}
```

### Tamper Detection Triggered
```kotlin
val result = verificationEngine.verifyEvidence(evidence)
if (!result.isValid) {
    println("Original hash: ${result.originalHash}")
    println("Current hash: ${result.currentHash}")
    // Evidence has been modified
}
```

### Chain of Custody Broken
```kotlin
val status = forensicEngine.verifyChainOfCustody()
if (status == IntegrityStatus.TAMPERED) {
    val report = forensicEngine.exportChainOfCustodyReport()
    // Review the full audit trail
}
```

## Next Steps

1. Review `INTEGRATION_SUMMARY.md` for component details
2. Review `TAKE2_LOGIC_REFERENCE.md` for architectural overview
3. Build the project with Android SDK
4. Run integration tests
5. Deploy to device for testing

---

**Last Updated**: December 5, 2025  
**For More Info**: See INTEGRATION_SUMMARY.md and TAKE2_LOGIC_REFERENCE.md

# Verum Omnis Forensic Engine - Implementation Guide

## Overview

This document describes the implementation of the **Verum Omnis Forensic Engine**, a deterministic, 12-step forensic analysis pipeline for Android.

## Architecture

### Package Structure

```
org.verumomnis.engine/
├── ForensicEngine.kt          # Main orchestrator
├── ForensicModels.kt          # Data models
├── EvidenceIngestor.kt        # Step 1
├── NarrativeBuilder.kt        # Step 2
├── SubjectClassifier.kt       # Step 3
├── ContradictionDetector.kt   # Step 4
├── OmissionDetector.kt        # Step 5
├── BehaviorAnalyzer.kt        # Step 6
├── KeywordScanner.kt          # Step 7
├── SeverityScorer.kt          # Step 8
├── DishonestyCalculator.kt    # Step 9
├── LiabilityExtractor.kt      # Step 10
├── ActionRecommender.kt       # Step 11
└── ReportBuilder.kt           # Step 12
```

### UI Components

```
com.verumdec.ui/
├── MainActivity.kt            # Entry point - create new case
├── CaseDetailActivity.kt      # Add evidence and generate report
└── ReportViewerActivity.kt    # Display generated report
```

## The 12-Step Forensic Pipeline

The engine processes ALL evidence through these exact steps in this exact order:

### STEP 1: EVIDENCE INGESTION
**Component**: `EvidenceIngestor`
- **Input**: List of evidence text strings
- **Output**: List<String> rawSentences
- **Function**: Split all evidence into individual sentences

### STEP 2: NARRATIVE BUILD
**Component**: `NarrativeBuilder`
- **Input**: Raw sentences
- **Output**: List<Sentence> narrativeList
- **Function**: Create Sentence objects with metadata (source, timestamp)

### STEP 3: SUBJECT CLASSIFICATION
**Component**: `SubjectClassifier`
- **Input**: Narrative list
- **Output**: Same list with subject tags added
- **Function**: Classify each sentence into legal subjects:
  - SHAREHOLDER_OPPRESSION
  - BREACH_OF_FIDUCIARY_DUTY
  - CYBERCRIME
  - FRAUDULENT_EVIDENCE
  - EMOTIONAL_EXPLOITATION

### STEP 4: CONTRADICTION DETECTION
**Component**: `ContradictionDetector`
- **Input**: Narrative list
- **Output**: List<ContradictionResult>
- **Function**: Compare sentences for contradictions
- **Severity**: All contradictions marked as HIGH

### STEP 5: OMISSION DETECTION
**Component**: `OmissionDetector`
- **Input**: Narrative list
- **Output**: List<OmissionResult>
- **Function**: Detect missing context, hidden details, timeline gaps
- **Severity**: MEDIUM or HIGH

### STEP 6: BEHAVIORAL ANALYSIS
**Component**: `BehaviorAnalyzer`
- **Input**: Narrative list
- **Output**: Same list with behavior flags added
- **Function**: Scan for behavioral patterns:
  - Evasion
  - Gaslighting
  - Blame-shifting
  - Selective disclosure
  - Refusal to answer
  - Justification loops
  - Unauthorized access attempts

### STEP 7: KEYWORD SCAN
**Component**: `KeywordScanner`
- **Input**: Narrative list
- **Output**: Same list with keywords added
- **Function**: Scan for specific keywords:
  - "admit", "deny", "forged", "delete", "access", "refuse", "invoice", "profit"

### STEP 8: SEVERITY SCORING
**Component**: `SeverityScorer`
- **Input**: Narrative list
- **Output**: Map<SubjectTag, Int> category scores
- **Function**: Assign severity scores:
  - LOW = 1
  - MEDIUM = 2
  - HIGH = 3
- Aggregate scores per category

### STEP 9: DISHONESTY SCORE
**Component**: `DishonestyCalculator`
- **Input**: Narrative list
- **Output**: Float percentage
- **Function**: Calculate dishonesty_score = (flagged_sentences / total_sentences) * 100

### STEP 10: TOP 3 LIABILITIES
**Component**: `LiabilityExtractor`
- **Input**: Category scores, contradictions, narrative
- **Output**: List<LiabilityEntry> (top 3)
- **Function**: Rank by:
  1. Total severity (primary)
  2. Total contradictions (secondary)
  3. Recurrence (tertiary)

### STEP 11: RECOMMENDED ACTIONS
**Component**: `ActionRecommender`
- **Input**: Top 3 liabilities
- **Output**: List<RecommendedAction>
- **Function**: Based on Verum Omnis template:
  - RAKEZ → shareholder injunction (UAE Art. 110)
  - SAPS → cybercrime device seizure
  - Civil → damages claim

### STEP 12: REPORT BUILD
**Component**: `ReportBuilder`
- **Input**: All previous outputs
- **Output**: ForensicReportData
- **Function**: Generate structured report with 10 sections:
  1. PRE-ANALYSIS DECLARATION
  2. Critical Legal Subjects Table
  3. Dishonesty Detection Matrix
  4. Tagged Evidence Table
  5. Contradictions Summary
  6. Behavioral Flags
  7. Dishonesty Score
  8. Top 3 Liabilities
  9. Recommended Actions
  10. POST-ANALYSIS DECLARATION

## Report Output

The report is saved to: `/cases/{caseId}/report.txt`

### Report Structure

```
================================================================
VERUM OMNIS FORENSIC ANALYSIS REPORT
================================================================
Case ID: {caseId}
Generated: {timestamp}
================================================================

1. PRE-ANALYSIS DECLARATION
----------------------------
[Declaration text explaining deterministic nature of engine]

2. CRITICAL LEGAL SUBJECTS TABLE
---------------------------------
Subject | Severity Score
---------------------------------
[Table of subject tags and scores]

3. DISHONESTY DETECTION MATRIX
-------------------------------
Flagged Sentences: {count}
[List of flagged sentences with keywords and behaviors]

4. TAGGED EVIDENCE TABLE
------------------------
Total Evidence Items: {count}
[List of sentences with their subject tags]

5. CONTRADICTIONS SUMMARY
-------------------------
Total Contradictions: {count}
[Detailed contradiction analysis]

6. BEHAVIORAL FLAGS
-------------------
[Counts of each behavioral pattern detected]

7. DISHONESTY SCORE
-------------------
Score: {percentage}%
(Flagged Sentences / Total Sentences × 100)

8. TOP 3 LIABILITIES
--------------------
[Top 3 liabilities ranked by severity, contradictions, recurrence]

9. RECOMMENDED ACTIONS
----------------------
[Legal actions based on findings]

10. POST-ANALYSIS DECLARATION
------------------------------
[Final declaration about engine immutability]

================================================================
END OF REPORT
================================================================
```

## UI Workflow

### 1. MainActivity
- User enters case name
- Creates folder: `/cases/{caseId}/`
- Navigates to CaseDetailActivity

### 2. CaseDetailActivity
**Buttons**:
- **Add Text Note** → save text → engine.ingest()
- **Add Image** → OCR → engine.ingest()
- **Add Document** → extract text → engine.ingest()
- **Generate Report** → engine.runFullPipeline() → save report → navigate to ReportViewerActivity

### 3. ReportViewerActivity
- Reads `/cases/{caseId}/report.txt`
- Displays full structured forensic output
- Allows sharing the report

## Threading Rules

All operations follow proper threading:

```kotlin
// Engine operations run on IO dispatcher
suspend fun runFullPipeline(caseId: String, evidenceTexts: List<String>): ForensicReportData {
    return withContext(Dispatchers.IO) {
        // ... pipeline steps ...
    }
}

// UI updates run on Main dispatcher
lifecycleScope.launch {
    val report = forensicEngine.runFullPipeline(caseId, evidenceTexts)
    // UI updates happen on Main thread automatically
}
```

## Immutability Rule (CRITICAL)

**THE ENGINE NEVER CHANGES ITS QUESTIONS OR STEPS BASED ON EVIDENCE.**

Every piece of evidence ALWAYS goes through:
- The SAME 12 steps
- In the SAME order
- With the SAME analysis logic
- Generating the SAME report structure

This pipeline is **FIXED** and **NON-NEGOTIABLE**.

### Why Immutability Matters

1. **Reproducibility**: Same input always produces same output
2. **Auditability**: Analysis method can be verified
3. **Legal Validity**: Process is consistent and documented
4. **No Bias**: Engine doesn't adapt based on who or what is being analyzed

## Testing

Unit tests verify:
1. Each component works correctly
2. Full pipeline produces expected output
3. Report contains all required sections
4. Immutability (same input → same output)

Run tests:
```bash
./gradlew test
```

## Usage Example

```kotlin
// Initialize engine
val forensicEngine = ForensicEngine()

// Collect evidence texts (already extracted from PDFs, images, etc.)
val evidenceTexts = listOf(
    "I admit I accessed the files without permission.",
    "I deny any wrongdoing.",
    "The invoice was forged."
)

// Run the complete 12-step pipeline
val report = forensicEngine.runFullPipeline(
    caseId = "case-12345",
    evidenceTexts = evidenceTexts
)

// Save report to file
val casesDir = File(context.getExternalFilesDir(null), "cases")
val reportFile = forensicEngine.saveReport(report, casesDir)

// Report is now available at: /cases/case-12345/report.txt
```

## Key Design Principles

1. **Single Responsibility**: Each component has exactly one job
2. **Deterministic**: No randomness, no AI, no variation
3. **Sequential Processing**: Steps always run in order 1→12
4. **Immutable Pipeline**: Same steps for every case
5. **Structured Output**: Always produces same report format
6. **Thread Safe**: Proper use of coroutines and dispatchers

## Conclusion

This implementation provides a complete, deterministic forensic engine that:
- ✅ Processes ALL evidence through the same 12-step pipeline
- ✅ Generates structured, reproducible reports
- ✅ Maintains immutability (never changes based on evidence)
- ✅ Follows proper Android threading patterns
- ✅ Saves reports to `/cases/{caseId}/report.txt`
- ✅ Provides complete UI workflow from case creation to report viewing

The engine is now ready to use and will consistently analyze evidence according to the Verum Omnis specification.

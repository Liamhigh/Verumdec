# Verum Omnis Forensic Engine - Visual Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                      VERUM OMNIS FORENSIC ENGINE                    │
│                    Deterministic 12-Step Pipeline                   │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                         ANDROID APPLICATION                         │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────┐      ┌──────────────────┐      ┌──────────────────┐
│  MainActivity    │─────▶│ CaseDetailActivity│─────▶│ ReportViewerActivity│
│                  │      │                  │      │                  │
│ - Create Case    │      │ - Add Text Note  │      │ - Display Report │
│ - Enter Name     │      │ - Add Image (OCR)│      │ - Share Report   │
│                  │      │ - Add PDF/Doc    │      │                  │
│                  │      │ - Generate Report│      │                  │
└──────────────────┘      └──────────────────┘      └──────────────────┘
                                   │
                                   │ calls
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      FORENSIC ENGINE ORCHESTRATOR                   │
│                    org.verumomnis.engine.ForensicEngine             │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   │ executes
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          12-STEP PIPELINE                           │
│                    (Same steps, same order, every time)             │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────┐
│ STEP 1           │  EvidenceIngestor
│ INGESTION        │  ─────────────────────────────────────────┐
└──────────────────┘  Input: List<String> evidence             │
         │            Output: List<String> rawSentences        │
         ▼                                                      │
┌──────────────────┐                                           │
│ STEP 2           │  NarrativeBuilder                         │
│ NARRATIVE BUILD  │  ─────────────────────────────────────────┤
└──────────────────┘  Input: Raw sentences                     │
         │            Output: List<Sentence> narrative         │
         ▼                                                      │
┌──────────────────┐                                           │
│ STEP 3           │  SubjectClassifier                        │
│ CLASSIFY SUBJECTS│  ─────────────────────────────────────────┤
└──────────────────┘  Tags: 5 Legal Categories                 │
         │            - SHAREHOLDER_OPPRESSION                 │
         │            - BREACH_OF_FIDUCIARY_DUTY               │
         │            - CYBERCRIME                             │  FIXED
         │            - FRAUDULENT_EVIDENCE                    │  IMMUTABLE
         │            - EMOTIONAL_EXPLOITATION                 │  PIPELINE
         ▼                                                      │
┌──────────────────┐                                           │  (Never
│ STEP 4           │  ContradictionDetector                    │   Changes)
│ DETECT CONTRADX  │  ─────────────────────────────────────────┤
└──────────────────┘  Severity: HIGH                           │
         │                                                      │
         ▼                                                      │
┌──────────────────┐                                           │
│ STEP 5           │  OmissionDetector                         │
│ DETECT OMISSIONS │  ─────────────────────────────────────────┤
└──────────────────┘  Severity: MEDIUM/HIGH                    │
         │                                                      │
         ▼                                                      │
┌──────────────────┐                                           │
│ STEP 6           │  BehaviorAnalyzer                         │
│ ANALYZE BEHAVIOR │  ─────────────────────────────────────────┤
└──────────────────┘  Flags: 7 Patterns                        │
         │            - Evasion, Gaslighting, Blame-shifting   │
         ▼            - Selective disclosure, Refusal, etc.    │
┌──────────────────┐                                           │
│ STEP 7           │  KeywordScanner                           │
│ SCAN KEYWORDS    │  ─────────────────────────────────────────┤
└──────────────────┘  Keywords: admit, deny, forged, delete,   │
         │            access, refuse, invoice, profit          │
         ▼                                                      │
┌──────────────────┐                                           │
│ STEP 8           │  SeverityScorer                           │
│ SCORE SEVERITY   │  ─────────────────────────────────────────┤
└──────────────────┘  Scores: LOW(1), MEDIUM(2), HIGH(3)       │
         │                                                      │
         ▼                                                      │
┌──────────────────┐                                           │
│ STEP 9           │  DishonestyCalculator                     │
│ CALC DISHONESTY  │  ─────────────────────────────────────────┤
└──────────────────┘  Formula: (flagged/total) × 100           │
         │                                                      │
         ▼                                                      │
┌──────────────────┐                                           │
│ STEP 10          │  LiabilityExtractor                       │
│ TOP 3 LIABILITIES│  ─────────────────────────────────────────┤
└──────────────────┘  Rank by: Severity, Contradictions,       │
         │            Recurrence                               │
         ▼                                                      │
┌──────────────────┐                                           │
│ STEP 11          │  ActionRecommender                        │
│ RECOMMEND ACTIONS│  ─────────────────────────────────────────┤
└──────────────────┘  Actions: RAKEZ, SAPS, Civil              │
         │                                                      │
         ▼                                                      │
┌──────────────────┐                                           │
│ STEP 12          │  ReportBuilder                            │
│ BUILD REPORT     │  ─────────────────────────────────────────┘
└──────────────────┘  Output: 10-Section Report
         │
         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         FORENSIC REPORT                             │
│                    /cases/{caseId}/report.txt                       │
└─────────────────────────────────────────────────────────────────────┘

REPORT STRUCTURE:
┌───────────────────────────────────────────────────────────────┐
│ 1. PRE-ANALYSIS DECLARATION                                   │
│ 2. Critical Legal Subjects Table                             │
│ 3. Dishonesty Detection Matrix                               │
│ 4. Tagged Evidence Table                                     │
│ 5. Contradictions Summary                                    │
│ 6. Behavioral Flags                                          │
│ 7. Dishonesty Score                                          │
│ 8. Top 3 Liabilities                                         │
│ 9. Recommended Actions                                       │
│ 10. POST-ANALYSIS DECLARATION                                │
└───────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                      KEY DESIGN PRINCIPLES                          │
├─────────────────────────────────────────────────────────────────────┤
│ ✅ IMMUTABILITY     │ Pipeline never changes based on evidence      │
│ ✅ DETERMINISM      │ Same input → same output (always)             │
│ ✅ SEQUENTIAL       │ Steps always execute 1→2→3→...→12             │
│ ✅ STRUCTURED       │ Report always has 10 sections                 │
│ ✅ OFFLINE          │ All processing on-device, no internet         │
│ ✅ REPRODUCIBLE     │ Analysis is auditable and verifiable          │
│ ✅ THREADED         │ IO for processing, Main for UI                │
└─────────────────────────────────────────────────────────────────────┘

DATA FLOW:
Evidence (Text/PDF/Image) 
    → EvidenceProcessor (existing)
    → Text Extraction
    → ForensicEngine.runFullPipeline()
    → 12 Steps Execute
    → ForensicReportData
    → ReportBuilder.generatePlainTextReport()
    → File saved to /cases/{caseId}/report.txt
    → ReportViewerActivity displays

THREADING MODEL:
┌──────────────────┐         ┌──────────────────┐
│ UI Thread        │         │ IO Thread        │
│ (Main)           │         │ (Background)     │
├──────────────────┤         ├──────────────────┤
│ - Button clicks  │   ┌────▶│ - Ingest         │
│ - Navigation     │   │     │ - Classify       │
│ - Display        │◀──┘     │ - Detect         │
│                  │         │ - Analyze        │
│ lifecycleScope   │         │ - Score          │
│   .launch { }    │         │ - Calculate      │
│                  │         │ - Build Report   │
│                  │         │                  │
│                  │         │ withContext(     │
│                  │         │   Dispatchers.IO │
│                  │         │ )                │
└──────────────────┘         └──────────────────┘

FILE STRUCTURE:
/storage/emulated/0/Android/data/com.verumdec/files/
└── cases/
    ├── case-abc-123/
    │   └── report.txt
    ├── case-def-456/
    │   └── report.txt
    └── case-xyz-789/
        └── report.txt

PACKAGE ORGANIZATION:
org.verumomnis.engine/
├── ForensicEngine.kt          Main orchestrator
├── ForensicModels.kt          Data models
├── EvidenceIngestor.kt        Step 1
├── NarrativeBuilder.kt        Step 2
├── SubjectClassifier.kt       Step 3
├── ContradictionDetector.kt   Step 4
├── OmissionDetector.kt        Step 5
├── BehaviorAnalyzer.kt        Step 6
├── KeywordScanner.kt          Step 7
├── SeverityScorer.kt          Step 8
├── DishonestyCalculator.kt    Step 9
├── LiabilityExtractor.kt      Step 10
├── ActionRecommender.kt       Step 11
├── ReportBuilder.kt           Step 12
└── examples/
    └── ForensicEngineExample.kt

TESTING:
org.verumomnis.engine/
└── ForensicEngineTest.kt
    ├── testEvidenceIngestion()
    ├── testNarrativeBuilder()
    ├── testSubjectClassifier()
    ├── testContradictionDetector()
    ├── testKeywordScanner()
    ├── testBehaviorAnalyzer()
    ├── testDishonestyCalculator()
    ├── testReportBuilder()
    ├── testImmutability_SameInputProducesSameOutput()
    └── testFullPipelineStructure()

┌─────────────────────────────────────────────────────────────────────┐
│                     IMPLEMENTATION STATS                            │
├─────────────────────────────────────────────────────────────────────┤
│ Total Files:           24                                           │
│ Engine Components:     14 (.kt files)                               │
│ UI Components:         3 Activities + 2 Layouts                     │
│ Documentation:         4 (.md files)                                │
│ Tests:                 11 unit tests                                │
│ Lines of Code:         ~3,500                                       │
│ Engine Code:           1,199 lines                                  │
└─────────────────────────────────────────────────────────────────────┘

Legend:
──▶  Data flow / Navigation
│    Sequential execution
┌──┐ Component / Module
═══  Section separator
```

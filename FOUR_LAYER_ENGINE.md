# Verum Omnis Four-Layer Forensic Engine

## Architecture Overview

The Verum Omnis Forensic Engine is implemented as **four deterministic layers** that process evidence sequentially:

```
Evidence Input
      ↓
LAYER 1: NarrativeEngine     → Normalize to structured sentences
      ↓
LAYER 2: ContradictionEngine → Detect conflicting statements
      ↓
LAYER 3: ClassificationEngine → Map to legal categories
      ↓
LAYER 4: ReportEngine        → Build final report
      ↓
Output: /cases/{caseId}/final_report.txt
```

## Layer 1: Narrative Engine

**Purpose**: Convert raw text evidence into clean, indexed, timestamp-aware sentences.

**Class**: `NarrativeEngine`

**Functions**:
- `ingest(rawText: String): List<Sentence>`
- `tokenize(): List<Sentence>`
- `extractTimestamp(sentence: String): Long?`
- `normalize(sentences: List<Sentence>): List<Sentence>`

**Data Model**:
```kotlin
data class Sentence(
    val text: String,
    val index: Int,
    val timestamp: Long? = null
)
```

**Logic**:
1. Split raw text into sentences using punctuation rules (`.`, `!`, `?`)
2. Trim whitespace and remove empty entries
3. Preserve order using index
4. Attempt to detect timestamps (YYYY-MM-DD, HH:MM)
5. Output `List<Sentence>` narrative

**Example**:
```kotlin
val engine = NarrativeEngine()
val sentences = engine.ingest("I accessed the files. I did not access them.")
// Returns: [Sentence("I accessed the files", 0), Sentence("I did not access them", 1)]
```

## Layer 2: Contradiction Engine

**Purpose**: Detect contradictions between any two sentences using fixed rules.

**Class**: `ContradictionEngine`

**Functions**:
- `analyze(sentences: List<Sentence>): List<ContradictionResult>`
- `contradictionRule(a: Sentence, b: Sentence): String?`

**Data Model**:
```kotlin
data class ContradictionResult(
    val a: Sentence,
    val b: Sentence,
    val reason: String
)
```

### Contradiction Rules (7 Rules)

#### RULE 1 — Direct Negation
If one sentence contains 'never', 'did not', 'no', while the other affirms same event.

**Example**:
- A: "I did access the files"
- B: "I never accessed the files"
- Result: CONTRADICTION

#### RULE 2 — Denial vs Evidence
Specific patterns of denial contradicting evidence:
- "no payment" vs "payment made"
- "never met" vs "we met Monday"

#### RULE 3 — Timeline Conflicts
Different months/dates for same event topic (met, invoice, payment, call, meeting).

**Logic**: If timestamps differ by more than 30 days for same topic.

#### RULE 4 — Quantity Conflicts
Different numbers for same subject:
- "one meeting" vs "three meetings"

#### RULE 5 — Admission vs Later Denial
- "I agreed" vs "I never agreed"
- "I admitted" vs "I deny"

#### RULE 6 — Action vs Outcome Conflict
- "I sent nothing" vs "email attached"

#### RULE 7 — Data Access Claim Conflicts
- "I did not access" vs evidence of access attempt

**Example**:
```kotlin
val engine = ContradictionEngine()
val sentences = listOf(
    Sentence("I agreed to the deal", 0),
    Sentence("I never agreed", 1)
)
val contradictions = engine.analyze(sentences)
// Returns: [ContradictionResult(..., reason: "Admission contradicts later denial")]
```

## Layer 3: Classification Engine

**Purpose**: Map contradictions to legal subject categories.

**Class**: `ClassificationEngine`

**Functions**:
- `classify(results: List<ContradictionResult>): List<LegalFinding>`
- `classifyContradiction(contradiction: ContradictionResult): List<LegalSubject>`

**Legal Subjects** (5 Categories):
```kotlin
enum class LegalSubject {
    SHAREHOLDER_OPPRESSION,
    BREACH_OF_FIDUCIARY_DUTY,
    CYBERCRIME,
    FRAUDULENT_EVIDENCE,
    EMOTIONAL_EXPLOITATION
}
```

**Data Model**:
```kotlin
data class LegalFinding(
    val subject: LegalSubject,
    val contradictions: List<ContradictionResult>
)
```

### Classification Rules (5 Rules)

#### RULE A — Corporate/Business Conflicts → ShareholderOppression
**Trigger Keywords**: profit, agreement, deal, decision, responsibility, ownership, shareholder, dividend, equity, voting, board

#### RULE B — Evidence Tampering → FraudulentEvidence
**Trigger Keywords**: delete, removed, cropped, missing, screenshot, edited, forged, fabricated, altered, tampered

#### RULE C — Device/Account Access → Cybercrime
**Trigger Keywords**: access, login, password, device, breach, unauthorized, hacked, cyber, computer, data

#### RULE D — Trust/Duty Conflicts → BreachOfFiduciaryDuty
**Trigger Keywords**: managing, accounting, decision-making, duty, lied, fiduciary, trust, loyalty, care, director

#### RULE E — Manipulation/Denial → EmotionalExploitation
**Trigger Keywords**: gaslight, you said, you did, never happened, emotional, imagining, crazy, sensitive, overreacting

**Note**: Each contradiction may map to multiple categories.

**Example**:
```kotlin
val engine = ClassificationEngine()
val contradiction = ContradictionResult(
    a = Sentence("Shareholders received profit", 0),
    b = Sentence("No profit distributed", 1),
    reason = "Test"
)
val findings = engine.classify(listOf(contradiction))
// Returns: [LegalFinding(SHAREHOLDER_OPPRESSION, [contradiction])]
```

## Layer 4: Report Engine

**Purpose**: Generate final forensic report with structured output.

**Class**: `ReportEngine`

**Functions**:
- `build(sentences, contradictions, legal, caseId): String`

### Report Sections (6 Required Sections)

#### 1. PRE-ANALYSIS DECLARATION
```
This is a deterministic forensic report. No AI interpretation included.
```

#### 2. NARRATIVE STRUCTURE
```
Index | Timestamp           | Sentence
------|---------------------|----------------------------------------
0     | 2024-01-15         | I accessed the files
1     | No timestamp       | I did not access the files
```

#### 3. CONTRADICTIONS DETECTED
```
#1

A: "I accessed the files"
   (Index: 0)

B: "I did not access the files"
   (Index: 1)

Reason: Direct negation: one sentence denies while the other affirms
```

#### 4. LEGAL CLASSIFICATION
```
Subject: CYBERCRIME

Evidence:
  - Contradiction #1
    "I did not access the device..."
    vs
    "Unauthorized login detected..."
```

#### 5. SUMMARY FINDINGS
```
Total contradictions: 3
Categories triggered: 2

Legal categories identified:
  - CYBERCRIME (2 contradiction(s))
  - FRAUDULENT_EVIDENCE (1 contradiction(s))
```

#### 6. POST-ANALYSIS DECLARATION
```
End of deterministic evaluation.

This report was generated using fixed, rule-based logic.
The same evidence processed again will produce identical results.
```

**Output Path**: `/cases/{caseId}/final_report.txt`

## Engine Manager (Orchestrator)

**Class**: `EngineManager`

Coordinates all four layers in exact order.

**Functions**:
```kotlin
suspend fun runFullPipeline(rawText: String, caseId: String): String
suspend fun runAndSave(rawText: String, caseId: String, casesDir: File): File
suspend fun runMultipleEvidence(evidenceTexts: List<String>, caseId: String): String
suspend fun runMultipleEvidenceAndSave(evidenceTexts: List<String>, caseId: String, casesDir: File): File
```

**Pipeline Order** (IMMUTABLE):
```kotlin
// LAYER 1: Narrative
val sentences = narrativeEngine.ingest(rawText)
val normalized = narrativeEngine.normalize(sentences)

// LAYER 2: Contradiction
val contradictions = contradictionEngine.analyze(normalized)

// LAYER 3: Classification
val legalFindings = classificationEngine.classify(contradictions)

// LAYER 4: Report
val report = reportEngine.build(sentences, contradictions, legalFindings, caseId)
```

## Usage Example

```kotlin
val manager = EngineManager()

val evidence = """
    I agreed to the payment on 2024-01-15.
    I never agreed to any payment.
    The profit was distributed to shareholders.
    No profit was given to shareholders.
""".trimIndent()

// Run the complete pipeline
lifecycleScope.launch {
    val reportFile = manager.runAndSave(
        rawText = evidence,
        caseId = "case-123",
        casesDir = File(context.getExternalFilesDir(null), "cases")
    )
    
    // Report saved to: /cases/case-123/final_report.txt
}
```

## Immutability Rule

**CRITICAL**: The four engines MUST ALWAYS run in this order:
```
Narrative → Contradiction → Classification → Report
```

**Logic MUST NEVER change based on evidence.**

- Same input → Same output (always)
- No AI, no learning, no adaptation
- Fixed rules only
- Deterministic and reproducible

## Testing

All four layers have comprehensive unit tests in `FourLayerEngineTest.kt`:

- ✅ Narrative Engine: Basic ingestion, timestamp extraction
- ✅ Contradiction Engine: All 7 contradiction rules
- ✅ Classification Engine: All 5 legal categories
- ✅ Report Engine: Report structure verification
- ✅ Engine Manager: Full pipeline, immutability

Run tests:
```bash
./gradlew test
```

## Key Differences from 12-Step Implementation

The four-layer architecture is:

1. **Simpler**: 4 layers instead of 12 components
2. **Clearer**: Each layer has one specific purpose
3. **Rule-based**: Explicit contradiction rules (7) and classification rules (5)
4. **More deterministic**: Fixed logic paths, no scoring calculations
5. **Court-ready**: Designed for legal admissibility with explicit rule citations

## File Locations

```
org/verumomnis/engine/
├── NarrativeEngine.kt          (Layer 1)
├── ContradictionEngine.kt      (Layer 2)
├── ClassificationEngine.kt     (Layer 3)
├── ReportEngine.kt             (Layer 4)
└── EngineManager.kt            (Orchestrator)

test/org/verumomnis/engine/
└── FourLayerEngineTest.kt      (Unit tests)
```

---

**This is the complete Verum Omnis Four-Layer Forensic Engine specification and implementation.**

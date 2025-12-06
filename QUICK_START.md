# Verum Omnis Forensic Engine - Quick Start

## What Is This?

The **Verum Omnis Forensic Engine** is a deterministic Android application that analyzes legal evidence through a fixed 12-step forensic pipeline.

**Key Features**:
- ✅ Processes text, PDFs, images (OCR), and documents
- ✅ Runs the SAME analysis on EVERY case (no bias, no variation)
- ✅ Generates structured forensic reports
- ✅ Detects contradictions, omissions, and behavioral patterns
- ✅ Calculates dishonesty scores and liability rankings
- ✅ Recommends legal actions based on findings

## How To Use

### 1. Create a New Case
- Open the app
- Tap "New Case"
- Enter a case name (e.g., "Smith v. Jones")
- Tap "Create"

### 2. Add Evidence
You'll be taken to the Case Detail screen. Add evidence using:

- **Add Text Note**: Type or paste text evidence directly
- **Add Image (OCR)**: Upload images - text will be extracted automatically
- **Add Document (PDF/Text)**: Upload PDF or text files

Add as many evidence items as needed.

### 3. Generate Report
- Once you have evidence, tap "Generate Forensic Report"
- The engine will run all 12 analysis steps
- This may take a few seconds for large cases

### 4. View Report
- The report opens automatically
- Scroll through all 10 sections
- Tap "Share Report" to send it via email or other apps

## What The Report Contains

Every report has these exact 10 sections:

1. **PRE-ANALYSIS DECLARATION** - Explains the engine's deterministic nature
2. **Critical Legal Subjects Table** - Categorizes evidence by legal subject
3. **Dishonesty Detection Matrix** - Lists flagged sentences
4. **Tagged Evidence Table** - Shows all evidence with tags
5. **Contradictions Summary** - Details contradictory statements
6. **Behavioral Flags** - Lists manipulation patterns detected
7. **Dishonesty Score** - Percentage of flagged vs total sentences
8. **Top 3 Liabilities** - Highest-ranked legal issues
9. **Recommended Actions** - Suggested legal steps (RAKEZ, SAPS, Civil)
10. **POST-ANALYSIS DECLARATION** - Final statements on validity

## The 12-Step Pipeline

Every case goes through these steps in this exact order:

1. **Ingest** - Convert all evidence to text
2. **Build Narrative** - Create chronological narrative
3. **Classify Subjects** - Tag legal subjects (shareholder oppression, cybercrime, etc.)
4. **Detect Contradictions** - Find conflicting statements
5. **Detect Omissions** - Find missing context or gaps
6. **Analyze Behavior** - Flag gaslighting, evasion, blame-shifting
7. **Scan Keywords** - Search for: admit, deny, forged, delete, access, refuse, invoice, profit
8. **Score Severity** - Rate LOW/MEDIUM/HIGH
9. **Calculate Dishonesty** - Compute percentage score
10. **Extract Top 3 Liabilities** - Rank by severity and contradictions
11. **Recommend Actions** - Suggest legal remedies
12. **Build Report** - Generate final structured output

**The engine NEVER changes these steps. Same process for every case.**

## File Storage

Reports are saved to:
```
/storage/emulated/0/Android/data/com.verumdec/files/cases/{caseId}/report.txt
```

You can access them through:
- The app's "Share Report" button
- Android's file manager
- Connected computer via USB

## Legal Subject Categories

The engine classifies evidence into 5 categories:

- **SHAREHOLDER_OPPRESSION** - Unfair treatment of shareholders
- **BREACH_OF_FIDUCIARY_DUTY** - Violation of trust/loyalty obligations
- **CYBERCRIME** - Unauthorized access, hacking, data theft
- **FRAUDULENT_EVIDENCE** - Forged or fabricated documents
- **EMOTIONAL_EXPLOITATION** - Manipulation, gaslighting, coercion

## Behavioral Patterns Detected

- **Evasion** - "I don't recall", vague responses
- **Gaslighting** - "You're imagining things"
- **Blame-Shifting** - "It's your fault"
- **Selective Disclosure** - Partial information
- **Refusal to Answer** - "No comment"
- **Justification Loops** - Excessive explanations
- **Unauthorized Access Attempts** - Hacking mentions

## Keywords Scanned

The engine specifically searches for:
- admit
- deny
- forged
- delete
- access
- refuse
- invoice
- profit

## Recommended Actions

Based on findings, the engine may recommend:

- **RAKEZ** - Shareholder injunction (UAE Article 110)
- **SAPS** - Cybercrime device seizure (South African Police)
- **Civil Court** - Damages claim for breach/fraud/distress

## Important Notes

### Immutability
The engine is **deterministic** - it NEVER changes its analysis based on evidence content. Every case follows the identical 12-step process.

### No AI
This is NOT an AI system. It uses fixed rules and keyword matching. It doesn't "learn" or "adapt."

### Legal Disclaimer
This tool assists with evidence analysis but does NOT replace legal counsel. Reports should be reviewed by qualified attorneys before use in legal proceedings.

### Privacy
All processing happens **on-device**. No data is sent to external servers.

## Technical Details

- **Platform**: Android (API 24+)
- **Language**: Kotlin
- **Threading**: Coroutines with Dispatchers.IO for processing
- **Dependencies**: ML Kit (OCR), PDFBox (PDF extraction)
- **Report Format**: Plain text (.txt)

## For Developers

See `VERUM_OMNIS_ENGINE.md` for full technical documentation including:
- Architecture details
- Component breakdown
- Threading patterns
- Testing instructions
- Code examples

## Support

For issues or questions, contact the development team or file an issue in the repository.

---

**Verum Omnis - Truth Through Consistent Analysis**

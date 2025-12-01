# Copilot Instructions for Verumdec

This document provides context and guidelines for GitHub Copilot when working on the Verumdec (Verum Omnis) Contradiction Engine project.

## Project Overview

Verumdec is an **offline forensic contradiction engine** for legal-grade analysis. It processes documents (PDFs, images, text, emails, WhatsApp exports) to detect contradictions, behavioral patterns, and calculate liability scores—all 100% offline on Android devices.

## Technology Stack

- **Language**: Kotlin 1.9.10
- **Platform**: Android (API 34, min SDK 24)
- **Build System**: Gradle 8.4 with Kotlin DSL (`.kts` files)
- **PDF Processing**: Apache PDFBox Android 2.0.27.0
- **OCR**: Google ML Kit Text Recognition 16.0.0
- **UI**: Material Design 3, ViewBinding
- **Testing**: JUnit 4, AndroidJUnit for instrumentation tests
- **Cryptography**: Java MessageDigest (SHA-512)

## Project Structure

```
/app/src/main/java/com/verumdec/
├── engine/              # Core analysis engines
│   ├── ContradictionEngine.kt    # Main orchestrator (9-stage pipeline)
│   ├── ContradictionAnalyzer.kt  # Contradiction detection
│   ├── BehavioralAnalyzer.kt     # Behavioral pattern detection
│   ├── EntityDiscovery.kt        # Entity extraction
│   ├── EvidenceProcessor.kt      # PDF/Image/Text/Email processing
│   ├── TimelineGenerator.kt      # Event chronologization
│   ├── LiabilityCalculator.kt    # Multi-factor scoring
│   ├── NarrativeGenerator.kt     # 5-layer narrative generation
│   └── ReportGenerator.kt        # PDF report generation
├── data/                # Data models
│   └── Models.kt        # All data classes and enums
└── ui/                  # User interface
    ├── MainActivity.kt
    ├── AnalysisActivity.kt
    └── *Adapter.kt      # RecyclerView adapters
```

## Coding Conventions

### Kotlin Style

- Use **data classes** for all model objects
- Use **sealed classes** or **enums** for finite states
- Prefer **immutable** collections (`List`, `Set`) over mutable ones when possible
- Use `UUID.randomUUID().toString()` for entity IDs
- Follow standard Kotlin naming conventions:
  - Classes: `PascalCase`
  - Functions/variables: `camelCase`
  - Constants: `SCREAMING_SNAKE_CASE`

### Documentation

- Use KDoc-style comments (`/** */`) for classes and public functions
- Keep documentation concise and focused on "what" and "why"

### Android Specifics

- Use **coroutines** with `Dispatchers.IO` for background processing
- Use **ViewBinding** for UI access (not `findViewById` or Kotlin synthetics)
- Handle context carefully to avoid memory leaks
- All processing must work **offline**—no network calls allowed in the analysis engine

### Testing

- Test files go in `app/src/test/java/com/verumdec/engine/`
- Use the pattern `*Test.kt` for test class names
- Follow AAA pattern: Arrange, Act, Assert
- Test classes currently exist for:
  - `ContradictionAnalyzerTest` (6 tests)
  - `LiabilityCalculatorTest` (5 tests)
  - `ContradictionEngineTest` (3 tests)

## Build and Test Commands

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests ContradictionAnalyzerTest

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run lint checks
./gradlew lint

# Clean and rebuild
./gradlew clean build
```

## Pipeline Architecture

The Contradiction Engine follows a 9-stage pipeline:

1. **Evidence Processing** - Extract text and metadata from documents
2. **Entity Discovery** - Find people, companies, emails, phones
3. **Timeline Generation** - Build chronological event timeline
4. **Contradiction Analysis** - Detect 6 types of contradictions
5. **Behavioral Analysis** - Detect 12 manipulation patterns
6. **Liability Calculation** - Weighted multi-factor scoring
7. **Narrative Generation** - 5-layer legal narrative
8. **Report Generation** - PDF with SHA-512 sealing
9. **AI Strategy Integration** - Output ready for AI interpretation

## Important Data Types

### Contradiction Types
- `DIRECT` - A says X, then A says NOT X
- `CROSS_DOCUMENT` - Different story in different documents
- `BEHAVIORAL` - Sudden story shifts
- `MISSING_EVIDENCE` - References unprovided document
- `TEMPORAL` - Timeline inconsistency
- `THIRD_PARTY` - Contradicted by another entity

### Severity Levels
- `CRITICAL` - Flips liability
- `HIGH` - Dishonest intent likely
- `MEDIUM` - Unclear/error
- `LOW` - Harmless inconsistency

### Behavior Types
Gaslighting, Deflection, Pressure tactics, Financial manipulation, Emotional manipulation, Sudden withdrawal, Ghosting, Over-explaining, Slip-up admission, Delayed response, Blame shifting, Passive admission

## Security Requirements

- **Never** add network calls to the analysis engine
- **Never** store or transmit user evidence to external services
- All reports must be sealed with **SHA-512** hash for tamper detection
- Handle file access through proper Android content providers

## Key Files for Common Tasks

| Task | Relevant Files |
|------|---------------|
| Add new contradiction type | `Models.kt`, `ContradictionAnalyzer.kt` |
| Add behavioral pattern | `Models.kt`, `BehavioralAnalyzer.kt` |
| Modify liability algorithm | `LiabilityCalculator.kt` |
| Update report format | `ReportGenerator.kt` |
| Add new evidence type | `Models.kt`, `EvidenceProcessor.kt` |
| UI changes | `ui/` directory, `res/layout/` |

## Dependencies

All dependencies are declared in `app/build.gradle.kts`. Key dependencies:
- AndroidX core, appcompat, material
- Lifecycle (ViewModel, LiveData)
- Navigation components
- Kotlin coroutines
- PDFBox Android for PDF processing
- ML Kit for OCR

## Known Limitations

- Library modules (`core`, `ocr`, `pdf`, etc.) are placeholders—all implementation is in `:app`
- OCR accuracy depends on image quality (DPI > 150 recommended)
- No cloud sync or multi-device support

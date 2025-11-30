# Verumdec Deployment Guide

> **Status**: ✅ Ready for Build (with Android SDK)

---

## Quick Start: Building the APK

### Prerequisites

- **JDK 17+** (OpenJDK or Oracle JDK)
- **Android SDK** (API level 34)
  - Install via Android Studio or command line
- **Internet connection** (for Gradle dependencies)

### Environment Setup

Set your Android SDK path:

```bash
# Option 1: Set environment variable
export ANDROID_HOME=/path/to/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

# Option 2: Or create local.properties in project root
echo "sdk.dir=/path/to/android-sdk" > local.properties
```

### Build Commands

```bash
# Navigate to project directory
cd Verumdec

# Clean build (recommended for first build)
./gradlew clean build

# Build debug APK (faster, for testing)
./gradlew assembleDebug

# Build release APK (for deployment)
./gradlew assembleRelease

# Run tests before building
./gradlew test

# Check for lint warnings
./gradlew lint
```

### Output Locations

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Test Results**: `app/build/reports/tests/`
- **Lint Report**: `app/build/reports/lint-results.html`

---

## Project Structure

### Monolithic Design (Current)

All code is implemented in the `:app` module for rapid deployment:

```
/app/src/main/java/com/verumdec/
├── engine/              # Core contradiction analysis
│   ├── ContradictionEngine.kt          # Main orchestrator (9-stage pipeline)
│   ├── ContradictionAnalyzer.kt        # Detects contradictions
│   ├── BehavioralAnalyzer.kt           # Behavioral pattern detection
│   ├── EntityDiscovery.kt              # Entity extraction
│   ├── EvidenceProcessor.kt            # PDF/Image/Text/Email processing
│   ├── TimelineGenerator.kt            # Event chronologization
│   ├── LiabilityCalculator.kt          # Multi-factor scoring
│   ├── NarrativeGenerator.kt           # 5-layer narrative generation
│   └── ReportGenerator.kt              # PDF report generation
├── data/                # Data models
│   └── Models.kt
├── ui/                  # User interface
│   ├── MainActivity.kt
│   ├── AnalysisActivity.kt
│   └── *Adapter.kt
└── ...
```

### Future: Modularization

Library modules are placeholders for future refactoring:

- `:core` - Shared data models
- `:ocr` - OCR functionality
- `:pdf` - PDF processing
- `:entity` - Entity extraction
- `:timeline` - Timeline generation
- `:analysis` - Analysis engines
- `:report` - Report generation
- `:ui` - UI components

---

## Testing

### Run Tests Locally

```bash
# All unit tests
./gradlew test

# Specific test class
./gradlew test --tests ContradictionAnalyzerTest

# With detailed output
./gradlew test --info

# Android instrumentation tests (requires emulator/device)
./gradlew connectedAndroidTest
```

### Test Coverage

Test files added:
- `app/src/test/java/com/verumdec/engine/ContradictionAnalyzerTest.kt` (6 tests)
- `app/src/test/java/com/verumdec/engine/LiabilityCalculatorTest.kt` (5 tests)
- `app/src/test/java/com/verumdec/engine/ContradictionEngineTest.kt` (3 tests)

**Total**: 14 unit tests covering:
- Direct contradiction detection
- Denial-admission sequences
- Cross-document contradictions
- Liability calculation with multiple factors
- Case summary generation

### Test Results Interpretation

```
✅ GREEN: All tests pass - code is production-ready for that component
❌ FAILED: Test failure - investigation needed before deployment
⚠️ SKIPPED: Conditional tests - review why they were skipped
```

---

## Continuous Integration (CI/CD)

### GitHub Actions Workflow

Add this to `.github/workflows/build.yml`:

```yaml
name: Build and Test

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run tests
        run: ./gradlew test
      
      - name: Build debug APK
        run: ./gradlew assembleDebug
      
      - name: Build release APK
        run: ./gradlew assembleRelease
      
      - name: Upload APK artifacts
        uses: actions/upload-artifact@v4
        with:
          name: apks
          path: app/build/outputs/apk/
      
      - name: Upload test reports
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-reports
          path: app/build/reports/
```

---

## Performance Benchmarks

### Analysis Pipeline Speed (Target)

| Stage | Time | Notes |
|-------|------|-------|
| Evidence Processing | 2-5s | PDFs, images, text files |
| Entity Discovery | 1-3s | Name/email extraction + clustering |
| Timeline Generation | 1-2s | Date parsing, event classification |
| Contradiction Analysis | 2-4s | All 6 types of contradictions |
| Behavioral Analysis | 1-2s | 12 pattern types |
| Liability Calculation | 1-2s | Multi-factor scoring |
| Narrative Generation | 2-3s | 5-layer narrative |
| Report Generation | 3-5s | PDF creation + SHA-512 sealing |
| **Total** | **13-26s** | For typical case with 5-10 documents |

---

## Security & Cryptography

### SHA-512 Report Sealing

Reports are cryptographically sealed with:

```
Hash Algorithm: SHA-512
Input: Complete report content
Output: 128-character hex string (printed on report)
Purpose: Tamper detection and authenticity verification
```

### Privacy & Offline Operation

- ✅ 100% offline - no internet required after build
- ✅ No telemetry or tracking
- ✅ No external API calls
- ✅ All processing on device
- ✅ No data sent to servers

---

## Known Limitations

1. **No backup/sync** - Data stored locally only
2. **Single device** - No cross-device synchronization
3. **Manual import** - Evidence must be manually uploaded
4. **Limited OCR** - ML Kit offline OCR has accuracy limits
5. **No collaborative review** - Single-user analysis

---

## Troubleshooting

### Build Errors

| Error | Solution |
|-------|----------|
| `SDK location not found` | Set `ANDROID_HOME` env var or create `local.properties` |
| `NDK not found` | Install NDK via Android SDK Manager |
| `Gradle sync failed` | Run `./gradlew --refresh-dependencies` |
| `Java version mismatch` | Ensure JDK 17+ is installed |

### Runtime Errors

| Error | Solution |
|-------|----------|
| `OutOfMemoryError` | Increase heap: `GRADLE_OPTS="-Xmx2g"` |
| `PDFBox parsing fails` | Verify PDF file is valid |
| `OCR not recognizing text` | Check image quality (DPI > 150 recommended) |

### Testing Issues

| Error | Solution |
|-------|----------|
| Tests don't run | Ensure JUnit4 in dependencies |
| Mock objects fail | Use Mockito library if needed |
| Lint warnings | Review `app/build/reports/lint-results.html` |

---

## Deployment Checklist

- [ ] Android SDK installed and configured
- [ ] JDK 17+ installed
- [ ] Run `./gradlew test` - all tests pass
- [ ] Run `./gradlew lint` - review warnings
- [ ] Build debug APK: `./gradlew assembleDebug`
- [ ] Test on emulator or physical device
- [ ] Build release APK: `./gradlew assembleRelease`
- [ ] Sign release APK (if needed)
- [ ] Create GitHub Release with APK
- [ ] Document known issues
- [ ] Set up CI/CD pipeline

---

## Release Process

### Version Numbering

Format: `MAJOR.MINOR.PATCH`

Current: `1.0.0`

Update in `app/build.gradle.kts`:
```kotlin
versionCode = 1    // Increment for each release
versionName = "1.0.0"  // Update for major/minor/patch
```

### Release APK Signing

```bash
# Generate keystore (first time only)
keytool -genkey -v -keystore verumdec.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias verumdec

# Add to local.properties
echo "
storeFile=../verumdec.keystore
storePassword=YOUR_PASSWORD
keyAlias=verumdec
keyPassword=YOUR_PASSWORD
" >> local.properties

# Build signed release APK
./gradlew assembleRelease
```

---

## Support & Maintenance

### Regular Tasks

- **Weekly**: Run tests and lint checks
- **Before release**: Full testing on multiple Android versions
- **Monthly**: Dependency updates and security checks
- **Quarterly**: Performance benchmarking

### Contact

For issues or questions about deployment:
- GitHub Issues: https://github.com/Liamhigh/Verumdec/issues
- Documentation: See README.md and PROJECT_STATUS.md

---

## Next Steps After Deployment

1. **Gather user feedback** on contradiction detection accuracy
2. **Optimize performance** based on real-world usage
3. **Add analytics** to understand feature usage
4. **Implement cloud sync** (optional) for multi-device support
5. **Refactor into modules** for better maintainability
6. **Expand OCR** with custom models for specialized documents

---

*Last Updated: November 30, 2025*
*Version: 1.0.0 (Ready for Build)*

# Verum Forensic App - Implementation Documentation

## Overview

The Verum Forensic App is a **local-only, offline-first forensic analysis tool** for Android that allows users to create forensic cases, add evidence, run comprehensive forensic analysis, and generate sealed reports with cryptographic integrity.

## Architecture

### Multi-Module Structure

The app follows a modular architecture with the following modules:

- **app**: Android entry point and UI activities
- **core**: Data models shared across all modules
- **entity**: Entity discovery and profiling  
- **analysis**: Contradiction detection and behavioral analysis
- **timeline**: Timeline reconstruction
- **report**: PDF report generation
- **ui**: Reusable UI components
- **ocr**: Text extraction from images
- **pdf**: PDF processing

### Module Dependencies

```kotlin
dependencies {
    implementation(project(":core"))
    implementation(project(":entity"))
    implementation(project(":analysis"))
    implementation(project(":timeline"))
    implementation(project(":report"))
    implementation(project(":ui"))
    implementation(project(":ocr"))
    implementation(project(":pdf"))
}
```

## Activities

### 1. MainActivity
- **Purpose**: App launcher and case management
- **Features**:
  - Create new forensic cases
  - View existing cases
  - Navigate to CaseDetailActivity

### 2. CaseDetailActivity
- **Purpose**: Main evidence management interface
- **Features**:
  - Display case metadata (ID, name, creation date)
  - Add evidence in multiple formats:
    - Text notes
    - Images (camera or gallery)
    - Documents (PDF, images, text)
    - Audio recordings
    - Video recordings
  - Launch capture activities
  - Generate forensic report
  - Navigate to ReportViewerActivity
- **File Path**: `/app/src/main/java/com/verumdec/ui/CaseDetailActivity.kt`

### 3. ScannerActivity
- **Purpose**: Capture images using CameraX
- **Features**:
  - Real-time camera preview
  - High-quality image capture
  - Auto-save to evidence directory
- **Saves to**: `/cases/{CASE_ID}/evidence/IMG_yyyyMMdd_HHmmss.jpg`
- **File Path**: `/app/src/main/java/com/verumdec/ui/ScannerActivity.kt`

### 4. AudioRecorderActivity
- **Purpose**: Record audio evidence
- **Features**:
  - Record/stop controls
  - Real-time duration display (chronometer)
  - Preview before saving
- **Saves to**: `/cases/{CASE_ID}/evidence/AUD_yyyyMMdd_HHmmss.m4a`
- **File Path**: `/app/src/main/java/com/verumdec/ui/AudioRecorderActivity.kt`

### 5. VideoRecorderActivity
- **Purpose**: Record video evidence
- **Features**:
  - Real-time camera preview
  - Video recording with audio
  - Recording indicator
- **Saves to**: `/cases/{CASE_ID}/evidence/VID_yyyyMMdd_HHmmss.mp4`
- **File Path**: `/app/src/main/java/com/verumdec/ui/VideoRecorderActivity.kt`

### 6. ReportViewerActivity
- **Purpose**: Display and export forensic reports
- **Features**:
  - Case metadata display
  - Evidence index with SHA-512 hashes
  - Timeline visualization
  - Contradictions summary
  - Narrative sections
  - Case fingerprint (SHA-512 of entire case directory)
  - PDF generation
  - Export/share functionality
- **File Path**: `/app/src/main/java/com/verumdec/ui/ReportViewerActivity.kt`

## Navigation Flow

```
MainActivity
    ↓ (Create Case)
CaseDetailActivity
    ↓ (Add Evidence)
    ├→ ScannerActivity (capture image)
    ├→ AudioRecorderActivity (record audio)
    ├→ VideoRecorderActivity (record video)
    └→ Document picker (select files)
    ↓ (Generate Report)
ReportViewerActivity
    ↓ (Export)
Share/Export dialog
```

## Forensic Engine

### ForensicEngine Class
**Location**: `/app/src/main/java/com/verumdec/engine/ForensicEngine.kt`

Main orchestrator that coordinates all forensic modules:

```kotlin
suspend fun process(
    case: Case,
    evidenceUris: Map<String, Uri>,
    listener: ProgressListener
): Case
```

### Analysis Pipeline

The forensic engine executes the following stages:

1. **Evidence Processing**
   - Read evidence files
   - Extract text content
   - Compute SHA-512 hashes
   - Extract metadata

2. **Entity Discovery**
   - Identify people, companies, organizations
   - Extract emails, phone numbers, bank accounts
   - Resolve aliases and references

3. **Timeline Reconstruction**
   - Parse dates and timestamps
   - Order events chronologically
   - Normalize relative dates

4. **Contradiction Detection**
   - Direct contradictions (A says X, then NOT X)
   - Cross-document contradictions
   - Timeline inconsistencies
   - Behavioral contradictions
   - Missing evidence contradictions

5. **Behavioral Analysis**
   - Detect manipulation patterns
   - Identify gaslighting, deflection
   - Track story changes
   - Measure linguistic drift

6. **Liability Calculation**
   - Score entities based on contradictions
   - Weight by severity
   - Factor in behavioral patterns
   - Calculate causal responsibility

7. **Narrative Generation**
   - Objective chronological account
   - Contradiction commentary
   - Behavioral pattern analysis
   - Deductive reasoning
   - Causal chain linkages

8. **Report Sealing**
   - Generate SHA-512 case fingerprint
   - Seal report with cryptographic hash
   - Create PDF with Verum watermark

### Existing Forensic Components

All forensic logic is **already implemented** in the repository:

- `ContradictionEngine.kt` - Main analysis orchestrator
- `ContradictionAnalyzer.kt` - Contradiction detection
- `EvidenceProcessor.kt` - File processing and hashing
- `TimelineGenerator.kt` - Timeline construction
- `NarrativeGenerator.kt` - Narrative generation
- `ReportGenerator.kt` - PDF report assembly
- `BehavioralAnalyzer.kt` - Behavioral pattern detection
- `LiabilityCalculator.kt` - Liability scoring
- `EntityDiscovery.kt` - Entity extraction

**No forensic algorithms were simplified or removed.**

## Data Models

### Case
```kotlin
data class Case(
    val id: String,
    val name: String,
    val createdAt: Date,
    val evidence: MutableList<Evidence>,
    val entities: MutableList<Entity>,
    val timeline: MutableList<TimelineEvent>,
    val contradictions: MutableList<Contradiction>,
    val liabilityScores: MutableMap<String, LiabilityScore>,
    val narrative: String,
    val sealedHash: String?
)
```

### Evidence
```kotlin
data class Evidence(
    val id: String,
    val type: EvidenceType, // TEXT, IMAGE, AUDIO, VIDEO, DOCUMENT
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val contentHash: String // SHA-512
)
```

### Report
```kotlin
data class ForensicReport(
    val caseId: String,
    val caseName: String,
    val entities: List<Entity>,
    val timeline: List<TimelineEvent>,
    val contradictions: List<Contradiction>,
    val behavioralPatterns: List<BehavioralPattern>,
    val liabilityScores: Map<String, LiabilityScore>,
    val narrativeSections: NarrativeSections,
    val sha512Hash: String,
    val version: String
)
```

## File Storage

All data is stored locally in app-private storage:

```
/Android/data/com.verumdec/files/cases/{CASE_ID}/
    case.json           # Case metadata
    evidence/           # Evidence files
        IMG_*.jpg       # Images
        AUD_*.m4a       # Audio
        VID_*.mp4       # Video
        TEXT_*.txt      # Text notes
        DOC_*.pdf       # Documents
    reports/            # Generated reports
        Report_*.pdf    # Sealed PDF reports
```

## Security & Privacy

### Cryptographic Integrity
- All evidence files are hashed with SHA-512
- Case directory is fingerprinted with SHA-512
- Reports are cryptographically sealed
- Tamper detection through hash verification

### Privacy Guarantees
- ✅ **100% local processing** - No cloud, no servers
- ✅ **Offline-first** - Works without internet
- ✅ **No telemetry** - Zero analytics or tracking
- ✅ **No external APIs** - All processing on-device
- ✅ **App-private storage** - Files not accessible to other apps

## Background Processing

All long-running operations use `Dispatchers.IO`:

```kotlin
lifecycleScope.launch {
    val result = withContext(Dispatchers.IO) {
        // Heavy forensic processing
        engine.process(case, evidenceUris, listener)
    }
}
```

### Progress Indicators

```kotlin
interface ProgressListener {
    fun onProgressUpdate(stage: AnalysisStage, progress: Int, message: String)
    fun onComplete(case: Case)
    fun onError(error: String)
}
```

## Permissions

Required permissions in `AndroidManifest.xml`:

- `CAMERA` - Image and video capture
- `RECORD_AUDIO` - Audio recording and video audio
- `READ_EXTERNAL_STORAGE` - Import documents (SDK < 33)
- `READ_MEDIA_IMAGES` - Import images (SDK >= 33)

## Dependencies

### Core Android
- AndroidX Core, AppCompat, Material Design
- Lifecycle, ViewModel, LiveData
- Navigation Components
- Coroutines

### Forensic Processing
- PDFBox Android - PDF processing
- ML Kit Text Recognition - OCR (offline)
- Gson - JSON serialization

### Media Capture
- CameraX - Image and video capture
- MediaRecorder - Audio recording

## Build Configuration

### Gradle Version
- Gradle: 8.4
- Android Gradle Plugin: 8.1.4
- Kotlin: 1.9.22

### SDK Versions
- compileSdk: 34
- minSdk: 24
- targetSdk: 34
- JVM Target: 17

## Known Limitations

### Build Environment Issue
The current CI/CD environment has network restrictions that prevent access to Google's Maven repository (`dl.google.com`). This blocks:
- Downloading Android Gradle Plugin
- Downloading AndroidX libraries
- Building APK
- Running tests

**To resolve**: Whitelist `dl.google.com` and `maven.google.com` in network configuration.

### Testing Status
- ✅ Code implementation complete
- ❌ Cannot build due to network restrictions
- ❌ Cannot run unit tests
- ❌ Cannot generate APK
- ❌ Cannot test on device/emulator

## Specification Compliance

### Requirements Met ✅
- [x] Multi-module architecture (app, core, entity, analysis, timeline, report, ui, ocr, pdf)
- [x] Module dependencies wired in app/build.gradle.kts
- [x] MainActivity creates case and navigates to CaseDetailActivity
- [x] CaseDetailActivity manages evidence
- [x] ScannerActivity captures images with CameraX
- [x] AudioRecorderActivity records audio with MediaRecorder
- [x] VideoRecorderActivity records video with CameraX
- [x] ReportViewerActivity displays and exports reports
- [x] All activities added to AndroidManifest.xml
- [x] ForensicEngine wrapper orchestrates all modules
- [x] File storage in /cases/{CASE_ID}/ structure
- [x] Background processing with Dispatchers.IO
- [x] Progress indicators during analysis
- [x] SHA-512 hashing for evidence and case fingerprint
- [x] Local-only, offline-first architecture
- [x] No cloud features, analytics, or telemetry
- [x] All existing forensic logic preserved

### Requirements NOT Violated ❌
- [x] No new forensic algorithms invented
- [x] No contradiction logic simplified
- [x] No SHA-512 hashing removed
- [x] No module structure changed
- [x] No report structure altered
- [x] No cloud features added
- [x] No analytics added

## Usage Workflow

### 1. Create a Case
1. Launch app (MainActivity)
2. Tap "New Case"
3. Enter case name
4. App creates UUID and directory structure
5. Navigate to CaseDetailActivity

### 2. Add Evidence
In CaseDetailActivity:
- **Text Note**: Tap "Text Note" → Enter text → Add
- **Image**: Tap "Image" → Take Photo or Gallery → Capture/Select
- **Document**: Tap "Document" → Select PDF/image/text file
- **Audio**: Tap "Audio" → Record → Stop → Done
- **Video**: Tap "Video" → Record → Stop → Auto-save

### 3. Generate Report
1. Tap "Generate Forensic Report"
2. App processes evidence (shows progress):
   - Processing evidence files
   - Discovering entities
   - Building timeline
   - Detecting contradictions
   - Analyzing behavior
   - Calculating liability
   - Generating narrative
3. Navigate to ReportViewerActivity

### 4. View and Export Report
In ReportViewerActivity:
- View evidence index with SHA-512 hashes
- Review timeline
- Check contradictions
- Read narrative summary
- Verify case fingerprint
- Tap "Generate PDF Report"
- Tap "Export" to share PDF

## Development Notes

### Code Style
- Kotlin coding conventions
- Material Design 3
- MVVM architecture pattern (where applicable)
- Reactive UI updates
- Proper error handling
- Progress feedback

### Future Enhancements
- Case persistence (save/load from JSON)
- Case encryption
- Batch evidence import
- Advanced search and filtering
- Timeline visualization
- Entity relationship graphs
- PDF report customization
- Print functionality

## Support

For issues or questions, please refer to:
- Main developer specification document
- Contradiction engine manual
- Source code comments
- Unit tests (when build is enabled)

---

**Version**: 1.0.0  
**Last Updated**: December 2024  
**Status**: Implementation Complete - Awaiting Build Environment Fix

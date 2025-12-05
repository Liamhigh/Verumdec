# COPILOT CODE AGENT — BUILD THE COMPLETE ANDROID APP

## App Purpose (Functional Contract)

This repository contains all the logic required for an offline forensic evidence app.
Copilot Code Agent must assemble this code into a fully working Android Studio app that builds into a functional APK.

The app must run entirely offline, with no network access.

## ✔ Core Behaviour the App Must Implement

### 1. Create a Case

User enters a case name.

- Generate a unique case ID.
- Create folders:
  ```
  /cases/{caseId}/
  /cases/{caseId}/evidence/
  /cases/{caseId}/reports/
  ```
- Save a case.json file with:
  - caseId
  - caseName
  - timestamp
  - evidence[]

### 2. Add Evidence

User must be able to add:

- Text notes
- Photos (camera)
- Audio recordings
- Video recordings
- Imported files

When evidence is added:

- Save file to `/cases/{caseId}/evidence/`
- Compute SHA-512 hash
- Append metadata to case.json:
  - evidenceId
  - type
  - timestamp
  - hash
  - path

### 3. Process the Case

When the user taps Generate Report:

- Load case.json
- Recompute hashes on all evidence
- Produce a final case hash
- Build a structured report object:
  - summary
  - evidence list
  - all hashes
  - final case hash

All processing stays on-device.

### 4. Generate Report

Save a text or PDF report to:

```
/cases/{caseId}/reports/final_report.txt
```

This must include:

- Case metadata
- Evidence list
- Hashes
- Summary

### 5. View Report

ReportViewerActivity must display the generated report.

## ✔ Required Activities

Copilot must ensure the following Activities exist and function:

- **MainActivity** → create case
- **CaseDetailActivity** → add evidence + generate report
- **ScannerActivity** → capture photo
- **AudioRecorderActivity** → record audio
- **VideoRecorderActivity** → record video
- **ReportViewerActivity** → display final report

Navigation must work between them.

## ✔ Technical Requirements

Copilot Code Agent must build a complete Android project using:

- Kotlin
- Android SDK 34
- Coroutines for file & hashing work
- CameraX for photos
- MediaRecorder for audio/video
- Standard file I/O
- **No external APIs**
- **No networking**
- **No Firebase**
- **No cloud integration**

Everything must remain local.

## ⭐ Build Requirements (Critical)

The final output must be:

- A complete Android Studio project
- That syncs with Gradle
- That compiles without errors
- That produces a working APK
- Using the logic already present in the repository
- Filling in any missing code automatically when needed

Copilot MUST NOT:

- Delete working logic
- Replace engine code unless necessary to fix errors
- Convert app to cloud architecture
- Add network calls

**The goal is a fully functional offline forensic app.**

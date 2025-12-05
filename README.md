# Forensic Evidence App

## Overview

This project is an offline-first Android application designed to let a user create a case, collect evidence, process it locally through a simple forensic workflow, and generate a final report. The app does not rely on external servers. All data is stored and processed on the device.

This README describes what the app is supposed to do, so that developers can understand the intended behaviour when generating or updating code.

## Core Purpose

The app follows a basic forensic workflow:

1. Create a new case
2. Add evidence (text, images, audio, video, documents)
3. Store evidence locally
4. Run simple analysis (hashing, metadata extraction, basic checks)
5. Generate a final report
6. Allow the user to view or export that report

The app is meant to act as a self-contained mobile evidence toolkit.

## Main Features

### 1. Case Creation

- User enters a case name
- App generates a unique case ID
- Case folder is created on device storage
- Metadata file (case.json) is created

### 2. Evidence Capture

The user may add any of the following:

- Text notes
- Photos (via camera or gallery)
- Audio recordings
- Video recordings
- Imported documents

Evidence is stored in:

```
/cases/{caseId}/evidence/
```

Each item includes:

- Evidence ID
- Type (text/image/audio/video/file)
- Timestamp
- Hash (SHA-512)
- File path

### 3. Local Forensic Processing

The app runs basic offline processing:

- File hashing (SHA-512)
- Timestamp extraction
- Optional GPS tagging (if user allows)
- Basic text summary or metadata extraction

This produces a structured analysis result used in the report.

### 4. Report Generation

The app produces a final case report that contains:

- Case name and metadata
- List of evidence items
- Evidence hashes
- Basic summaries
- A single combined report file

Reports are saved under:

```
/cases/{caseId}/reports/
```

### 5. Report Viewer

The user can open the generated report inside the app.

## Offline-First Design

- No data leaves the device
- No cloud uploads
- No external services
- Fully self-contained mobile workflow

This allows the app to operate in low-connectivity or secure environments.

## Main Activities / Screens

### MainActivity

- Lets the user create a new case
- Navigates to CaseDetail screen

### CaseDetailActivity (or screen)

- Shows case metadata
- Shows list of added evidence
- Buttons to add evidence
- Button to generate the final report

### ScannerActivity

- Captures photos or scanned documents
- Saves them into the case folder

### AudioRecorderActivity

- Records a short audio clip
- Saves the audio file and computes a hash

### VideoRecorderActivity

- Records a short video clip
- Saves the file to evidence folder

### ReportViewerActivity

- Loads and displays the generated report

## Required Logic (High-Level)

### Case Management

- Create folder
- Save case metadata
- Maintain list of evidence

### Evidence Handling

- Save files
- Generate SHA-512 hash
- Append item to the case's evidence list

### Processing

- Run analysis
- Produce a structured result object

### Report Generation

- Build report text or PDF
- Save it to /reports/
- Show it in the viewer screen

## Technology Stack (Generic)

- Kotlin
- Android SDK
- CameraX (for photos)
- MediaRecorder (audio/video)
- Coroutines (for background work)
- File I/O (for local storage)
- Optional: simple PDF generator

## App Flow Summary

```
Start
  ↓
MainActivity → create case
  ↓
CaseDetailActivity → add evidence (image/audio/video/text/file)
  ↓
Processing Engine → hashing + metadata
  ↓
Generate Report
  ↓
ReportViewerActivity → user views or exports report
  ✔
```

## Why This Approach

This README provides a clean, neutral description of the app's functional behavior without complex architecture details or brand-specific information. It allows any developer or AI system to understand the core workflow and implement or extend the functionality as needed.

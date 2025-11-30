// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Verumdec - Offline Contradiction Engine
// This project follows a modular architecture with the following modules:
// - app: Main application entry point
// - core: Generic logic & data models shared across modules
// - ocr: OCR utilities and Tesseract integration for text extraction
// - pdf: PDF processing and parsing
// - entity: Entity and claim extraction from documents
// - timeline: Event chronologization and timeline generation
// - analysis: Contradiction detection and behavioral analysis
// - report: PDF generation and cryptographic sealing
// - ui: Presentation layer, layouts, and UI components

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

// Common configurations for all subprojects
subprojects {
    afterEvaluate {
        // Common Android configurations are applied in individual module build files
        // to maintain flexibility while sharing version constraints
    }
}

// Build configuration constants shared across modules
extra.apply {
    set("compileSdk", 34)
    set("minSdk", 24)
    set("targetSdk", 34)
    set("jvmTarget", "17")
}

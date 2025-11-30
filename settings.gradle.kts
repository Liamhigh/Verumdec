pluginManagement {
    repositories {
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Verumdec"

// Main application module
include(":app")

// Core module - Generic logic & data models shared across all modules
include(":core")

// OCR module - OCR utilities and Tesseract integration for text extraction from images
include(":ocr")

// PDF module - PDF processing, parsing, and metadata extraction
include(":pdf")

// Entity module - Entity and claim extraction from processed documents
include(":entity")

// Timeline module - Event chronologization and timeline generation
include(":timeline")

// Analysis module - Contradiction detection and behavioral analysis engine
include(":analysis")

// Report module - PDF report generation and cryptographic sealing
include(":report")

// UI module - Presentation layer, layouts, and reusable UI components
include(":ui")

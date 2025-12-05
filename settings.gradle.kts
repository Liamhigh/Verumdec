pluginManagement {
    repositories {
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

// Forensic Engine modules
include(":forensic_engine")  // Main forensic engine facade
include(":contradiction")    // Contradiction detection module
include(":timeline")        // Timeline analysis module
include(":image")           // Image processing module
include(":voice")           // Voice analysis module

// Supporting modules
include(":core")    // Generic logic & data models shared across all modules
include(":ocr")     // OCR utilities and text extraction from images
include(":pdf")     // PDF processing, parsing, and metadata extraction
include(":entity")  // Entity and claim extraction from processed documents
include(":analysis")// Contradiction detection and behavioral analysis engine
include(":report")  // PDF report generation and cryptographic sealing
include(":ui")      // Presentation layer, layouts, and reusable UI components

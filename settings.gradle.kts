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

// Library modules
include(":core")      // Generic logic & data models shared across all modules
include(":entity")    // Entity and claim extraction from processed documents
include(":timeline")  // Event chronologization and timeline generation
include(":analysis")  // Contradiction detection and behavioral analysis engine

// Future modules (not yet implemented):
// include(":ocr")     // OCR utilities and text extraction from images
// include(":pdf")     // PDF processing, parsing, and metadata extraction
// include(":report")  // PDF report generation and cryptographic sealing
// include(":ui")      // Presentation layer, layouts, and reusable UI components

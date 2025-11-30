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

// NOTE: Library modules (core, ocr, pdf, entity, timeline, analysis, report, ui) are currently
// placeholders. All implementations are in the :app module.
// To do: Refactor to move implementations to their respective modules.
// 
// Future module structure:
// include(":core")    // Generic logic & data models shared across all modules
// include(":ocr")     // OCR utilities and text extraction from images
// include(":pdf")     // PDF processing, parsing, and metadata extraction
// include(":entity")  // Entity and claim extraction from processed documents
// include(":timeline")// Event chronologization and timeline generation
// include(":analysis")// Contradiction detection and behavioral analysis engine
// include(":report")  // PDF report generation and cryptographic sealing
// include(":ui")      // Presentation layer, layouts, and reusable UI components

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

// Core modules - NEW LEVELER ENGINE
include(":core")       // Generic logic & data models shared across all modules
include(":entity")     // Entity and claim extraction from processed documents
include(":timeline")   // Event chronologization and timeline generation
include(":analysis")   // Contradiction detection and behavioral analysis engine

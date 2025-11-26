// Timeline Module
// Purpose: Event chronologization and timeline generation
// This module contains:
// - Timestamp normalization ("Last Friday" -> actual date)
// - Master Chronological Timeline generation
// - Per-Entity Timeline generation
// - Event-Type Timeline (Payments, Requests, Contradictions, Promises)
// - Relative date resolution
//
// Pipeline Stage 3: TIMELINE GENERATION (Core of the Narrative)
// Builds the spine of the narrative through chronological ordering

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.verumdec.timeline"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core module dependency
    implementation(project(":core"))

    // Entity module for entity references in timeline
    implementation(project(":entity"))

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}

// Analysis Module
// Purpose: Contradiction detection and behavioral analysis engine
// This module contains:
// - Direct Contradictions (A says X then NOT X)
// - Cross-Document Contradictions
// - Behavioral Contradictions (story shifts, tone changes)
// - Missing-Evidence Contradictions
// - Contradiction severity scoring (Critical, High, Medium, Low)
// - Behavioral pattern detection (gaslighting, deflection, manipulation)
// - Liability matrix calculation
//
// Pipeline Stage 4: CONTRADICTION ANALYSIS (the truth engine)
// Pipeline Stage 5: BEHAVIOURAL ANALYSIS
// Pipeline Stage 6: LIABILITY MATRIX (Mathematical Scoring)

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.verumdec.analysis"
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

    // Entity module for entity analysis
    implementation(project(":entity"))

    // Timeline module for chronological contradiction detection
    implementation(project(":timeline"))

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}

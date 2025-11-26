// Report Module
// Purpose: PDF report generation and cryptographic sealing
// This module contains:
// - Narrative generation (Objective, Contradiction, Behavioral, Deductive layers)
// - Final sealed PDF report generation
// - SHA-512 hash sealing
// - Verum watermark and metadata embedding
// - QR code generation
// - "Patent Pending Verum Omnis" block
//
// Pipeline Stage 7: NARRATIVE GENERATION
// Pipeline Stage 8: THE FINAL SEALED REPORT (PDF)
// Produces court-ready documents with cryptographic integrity

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.verumdec.report"
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    // Core module dependency
    implementation(project(":core"))

    // Analysis module for contradiction and liability data
    implementation(project(":analysis"))

    // Timeline module for chronological narrative
    implementation(project(":timeline"))

    // Entity module for entity information in reports
    implementation(project(":entity"))

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    // TODO: Add PDFBox dependency for PDF generation when implementing
    // implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}

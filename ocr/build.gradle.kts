// OCR Module
// Purpose: OCR utilities and Tesseract integration for text extraction
// This module contains:
// - Text recognition from images
// - Tesseract OCR wrapper and utilities
// - Image preprocessing for OCR
// - Character and layout recognition
//
// Pipeline Stage 1: INPUT LAYER - Evidence Ingestion (Offline)
// Handles: Images, Screenshots, and scanned documents

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.verumdec.ocr"
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

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    // TODO: Add Tesseract OCR and ML Kit dependencies when implementing
    // implementation("com.google.mlkit:text-recognition:16.0.0")
}

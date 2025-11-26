// PDF Module
// Purpose: PDF processing, parsing, and metadata extraction
// This module contains:
// - PDF text extraction
// - PDF metadata parsing (dates, EXIF, timestamps, file creation info)
// - Document structure analysis
// - PDF page and content utilities
//
// Pipeline Stage 1: INPUT LAYER - Evidence Ingestion (Offline)
// Handles: PDFs and their embedded content

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.verumdec.pdf"
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

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    // TODO: Add PDFBox dependency when implementing
    // implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}

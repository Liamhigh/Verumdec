// Entity Module
// Purpose: Entity and claim extraction from processed documents
// This module contains:
// - Entity discovery (names, emails, phone numbers, companies, bank accounts)
// - Entity clustering by frequency and co-occurrence
// - Alias and reference resolution ("He", "my partner", etc.)
// - Claim and assertion extraction
// - Statement mapping per entity
//
// Pipeline Stage 2: ENTITY DISCOVERY - Who are the players?
// Creates entity IDs, alias lists, unique signatures, timeline footprints

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.verumdec.entity"
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
}

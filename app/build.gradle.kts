plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.verumdec"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.verumdec"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
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
    
    buildFeatures {
        viewBinding = true
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
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // ============================================
    // Verumdec Module Dependencies
    // These modules form the offline contradiction engine pipeline
    // ============================================
    
    // Core module - shared data models and utilities
    implementation(project(":core"))
    
    // OCR module - text extraction from images (Pipeline Stage 1)
    implementation(project(":ocr"))
    
    // PDF module - PDF processing and parsing (Pipeline Stage 1)
    implementation(project(":pdf"))
    
    // Entity module - entity and claim extraction (Pipeline Stage 2)
    implementation(project(":entity"))
    
    // Timeline module - event chronologization (Pipeline Stage 3)
    implementation(project(":timeline"))
    
    // Analysis module - contradiction and behavioral analysis (Pipeline Stages 4-6)
    implementation(project(":analysis"))
    
    // Report module - PDF generation and sealing (Pipeline Stages 7-8)
    implementation(project(":report"))
    
    // UI module - shared presentation components (Pipeline Stage 9)
    implementation(project(":ui"))

    // ============================================
    // Core Android
    // ============================================
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    
    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // PDF processing - Apache PDFBox for Android (lightweight port)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    
    // ML Kit for text recognition (OCR) - works offline
    implementation("com.google.mlkit:text-recognition:16.0.0")
    
    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Document picker
    implementation("androidx.activity:activity-ktx:1.8.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // RecyclerView for lists
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // CardView for cards
    implementation("androidx.cardview:cardview:1.0.0")
    
    // ViewPager2 for swipe views
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

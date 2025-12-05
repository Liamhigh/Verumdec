// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Verumdec - Offline Contradiction Engine
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.22")
    }
}

// Build configuration constants shared across modules
extra.apply {
    set("compileSdk", 34)
    set("minSdk", 24)
    set("targetSdk", 34)
    set("jvmTarget", "17")
}

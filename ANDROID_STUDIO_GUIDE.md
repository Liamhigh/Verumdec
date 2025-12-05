# Android Studio Deployment Guide

This guide explains how to open, build, and deploy the Verumdec app using Android Studio.

## Prerequisites

Before opening the project in Android Studio, ensure you have:

1. **Android Studio** (Hedgehog 2023.1.1 or later recommended)
   - Download from: https://developer.android.com/studio

2. **JDK 17** (Java Development Kit)
   - Android Studio usually includes this, or download from: https://adoptium.net/

3. **Android SDK** with the following components:
   - Android SDK Platform 34 (Android 14)
   - Android SDK Build-Tools 34.0.0 or later
   - Android SDK Platform-Tools

## Opening the Project in Android Studio

### Method 1: Using Git Desktop (Recommended for this project)

1. **Clone the repository using Git Desktop**:
   - Open Git Desktop
   - Go to File → Clone Repository
   - Select the "URL" tab
   - Enter: `https://github.com/Liamhigh/Verumdec`
   - Choose a local path where you want to clone
   - Click "Clone"

2. **Open in Android Studio**:
   - Launch Android Studio
   - Click "Open" on the welcome screen (or File → Open if you have a project open)
   - Navigate to the cloned repository folder
   - Select the `Verumdec` root folder (the one containing `build.gradle.kts`)
   - Click "OK"

### Method 2: Direct Clone from Android Studio

1. **Clone from Version Control**:
   - Launch Android Studio
   - Click "Get from VCS" on the welcome screen (or File → New → Project from Version Control)
   - Enter URL: `https://github.com/Liamhigh/Verumdec`
   - Choose a directory
   - Click "Clone"

## First-Time Setup

After opening the project, Android Studio will:

1. **Sync Gradle** (this may take several minutes on first run)
   - Android Studio will download required dependencies
   - If prompted about Gradle JDK, select JDK 17
   - Wait for "Gradle sync finished" message in the Build window

2. **Download Dependencies**
   - The following libraries will be downloaded automatically:
     - AndroidX libraries
     - Google Play Services Location
     - iText PDF library
     - ZXing (QR code)
     - ML Kit (OCR)
     - And others as specified in `app/build.gradle.kts`

3. **Configure SDK**
   - If Android Studio shows "SDK not found", go to File → Project Structure
   - Select SDK Location and point to your Android SDK path
   - Usually: `C:\Users\<YourName>\AppData\Local\Android\Sdk` (Windows)
   - Or: `/Users/<YourName>/Library/Android/sdk` (macOS)

## Building the APK

### Debug Build (for testing)

1. **Via Build Menu**:
   - Go to Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Wait for the build to complete
   - Click "locate" in the notification to find the APK
   - APK location: `app/build/outputs/apk/debug/app-debug.apk`

2. **Via Gradle Panel**:
   - Open the Gradle panel (View → Tool Windows → Gradle)
   - Navigate to: Verumdec → app → Tasks → build → assembleDebug
   - Double-click to run
   - APK will be in: `app/build/outputs/apk/debug/app-debug.apk`

3. **Via Terminal in Android Studio**:
   ```bash
   ./gradlew assembleDebug
   ```

### Release Build (for production)

1. **Via Build Menu**:
   - Go to Build → Generate Signed Bundle / APK
   - Select "APK"
   - Click "Next"
   - Create a new key store or select an existing one
   - Fill in the key store password and key alias
   - Click "Next", select "release" build variant
   - Click "Finish"

2. **Via Gradle Panel**:
   - Navigate to: Verumdec → app → Tasks → build → assembleRelease
   - Double-click to run
   - APK will be in: `app/build/outputs/apk/release/app-release-unsigned.apk`

3. **Via Terminal in Android Studio**:
   ```bash
   ./gradlew assembleRelease
   ```

## Running on Device/Emulator

### On Physical Device

1. **Enable Developer Options on your Android device**:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Go back to Settings → Developer Options
   - Enable "USB Debugging"

2. **Connect device via USB**

3. **Run the app**:
   - Click the "Run" button (green play icon) in Android Studio toolbar
   - Select your device from the list
   - Click "OK"

### On Emulator

1. **Create an emulator**:
   - Go to Tools → Device Manager
   - Click "Create Device"
   - Select a phone model (e.g., Pixel 6)
   - Select system image (API 34 - Android 14)
   - Click "Finish"

2. **Run the app**:
   - Click the "Run" button (green play icon)
   - Select your emulator
   - Click "OK"

## Project Structure

```
Verumdec/
├── app/                           # Main application module
│   ├── src/
│   │   └── main/
│   │       ├── java/com/verumdec/
│   │       │   ├── crypto/        # Cryptographic sealing (from take2)
│   │       │   ├── custody/       # Chain of custody (from take2)
│   │       │   ├── data/          # Data models
│   │       │   ├── engine/        # Core engines
│   │       │   ├── jurisdiction/  # Legal compliance (from take2)
│   │       │   ├── leveler/       # Bias correction (from take2)
│   │       │   ├── location/      # GPS services (from take2)
│   │       │   ├── pdf/           # PDF generation (from take2)
│   │       │   ├── report/        # Narrative generation (from take2)
│   │       │   ├── ui/            # UI components
│   │       │   └── verification/  # Offline verification (from take2)
│   │       ├── res/               # Resources (layouts, strings, etc.)
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts           # App module build configuration
├── build.gradle.kts               # Root build configuration
├── settings.gradle.kts            # Project settings
└── gradle/                        # Gradle wrapper files
```

## Key Features

### From Original Verumdec
- Contradiction engine
- Timeline generation
- Entity discovery
- Behavioral analysis
- Liability calculation
- Narrative generation

### From take2 Integration
- Cryptographic sealing (SHA-512)
- GPS location capture
- Jurisdiction detection (UAE, SA, EU, US)
- Chain of custody logging
- Court-ready PDF generation
- Offline verification
- Truth/bias leveling

## Troubleshooting

### Gradle Sync Fails

**Issue**: "Could not resolve dependencies"
**Solution**: 
- Check your internet connection
- Go to File → Invalidate Caches → Invalidate and Restart
- Try File → Sync Project with Gradle Files

### SDK Not Found

**Issue**: "Android SDK is not found"
**Solution**:
- Go to File → Project Structure → SDK Location
- Set the Android SDK location
- Click "Apply"

### Build Fails with "Duplicate class" error

**Issue**: Duplicate class errors
**Solution**:
- Clean the project: Build → Clean Project
- Rebuild: Build → Rebuild Project

### Out of Memory Error

**Issue**: "Out of memory" during build
**Solution**:
- Edit `gradle.properties` and add/modify:
  ```
  org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
  ```

### Slow Gradle Build

**Solution**:
- Enable parallel builds in `gradle.properties`:
  ```
  org.gradle.parallel=true
  org.gradle.caching=true
  ```

## Minimum Requirements

- **Minimum SDK**: API 24 (Android 7.0 Nougat)
- **Target SDK**: API 34 (Android 14)
- **Compile SDK**: API 34
- **JVM Target**: Java 17

## Dependencies Overview

The project uses the following key dependencies:

**Android Core**
- AndroidX Core KTX
- Material Design Components
- ConstraintLayout

**Forensic Features (from take2)**
- Google Play Services Location (GPS)
- iText PDF (PDF generation)
- ZXing (QR codes)
- SLF4J (Logging)

**Existing Verumdec**
- Apache PDFBox Android (PDF processing)
- Google ML Kit (OCR)
- Gson (JSON parsing)

## Build Variants

The project includes two build variants:

1. **Debug**
   - Debuggable
   - Not optimized
   - Suitable for development and testing

2. **Release**
   - Optimized with ProGuard
   - Minified
   - Requires signing for distribution

## Additional Resources

- **Documentation**: See `INTEGRATION_SUMMARY.md`, `TAKE2_LOGIC_REFERENCE.md`, and `QUICK_START.md` in the repository root
- **Project Status**: See `PROJECT_STATUS.md` for implementation details
- **Deployment Guide**: See `DEPLOYMENT.md` for deployment options

## Getting Help

If you encounter issues:

1. Check the Build Output panel in Android Studio
2. Review the Event Log for detailed error messages
3. Search for the error message on Stack Overflow
4. Check the project's GitHub Issues page

## Next Steps After Successful Build

1. **Test on Device**: Install and test the APK on a physical device
2. **Review Features**: Explore the integrated take2 components
3. **Generate Reports**: Try creating a forensic case and generating a court-ready PDF
4. **Verify Integrity**: Test the cryptographic sealing and verification features

---

**Last Updated**: December 5, 2025  
**Repository**: https://github.com/Liamhigh/Verumdec  
**Build System**: Gradle 8.4  
**Android Gradle Plugin**: 8.2.2

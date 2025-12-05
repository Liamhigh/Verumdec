# Building Verumdec - Complete Android Studio Project

This document provides complete instructions for building the Verumdec Android application.

## Prerequisites

### Required Software

1. **Android Studio** (Latest stable version recommended)
   - Download from: https://developer.android.com/studio
   - Version: Hedgehog (2023.1.1) or newer

2. **Java Development Kit (JDK)**
   - JDK 17 (required for this project)
   - Android Studio typically includes the required JDK

3. **Android SDK Components**
   - Android SDK Platform 34 (Android 14.0)
   - Android SDK Build-Tools 34.0.0 or newer
   - Android SDK Platform-Tools
   - Android SDK Tools

### System Requirements

- **Windows**: Windows 10 or newer (64-bit)
- **macOS**: macOS 10.14 or newer
- **Linux**: 64-bit distribution with glibc 2.31 or newer
- **RAM**: Minimum 8 GB, recommended 16 GB
- **Disk Space**: Minimum 8 GB free space

## Project Setup

### Step 1: Clone the Repository

```bash
git clone https://github.com/Liamhigh/Verumdec.git
cd Verumdec
```

### Step 2: Configure local.properties

Create a `local.properties` file in the project root directory:

**For macOS/Linux:**
```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
```

**For Windows:**
```properties
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
```

**Note:** You can copy `local.properties.template` and modify it:
```bash
cp local.properties.template local.properties
# Then edit local.properties with your actual Android SDK path
```

### Step 3: Open Project in Android Studio

1. Launch Android Studio
2. Click **File → Open**
3. Navigate to the cloned `Verumdec` directory
4. Click **OK**
5. Wait for Gradle sync to complete (this may take a few minutes)

### Step 4: Install Required SDK Components

If Android Studio prompts you to install missing SDK components:

1. Click **Install missing SDK packages**
2. Accept the license agreements
3. Click **Next** and wait for installation to complete

Alternatively, install manually via SDK Manager:

1. Go to **Tools → SDK Manager**
2. Under **SDK Platforms**, ensure these are checked:
   - Android 14.0 (API Level 34) - SDK Platform
3. Under **SDK Tools**, ensure these are checked:
   - Android SDK Build-Tools 34.x
   - Android SDK Platform-Tools
   - Android Emulator
   - Google Play Services
4. Click **Apply** to install

## Building the Application

### Option 1: Build via Android Studio

#### Debug Build (APK)

1. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Wait for the build to complete
3. Click **locate** in the notification to find the APK
4. APK location: `app/build/outputs/apk/debug/app-debug.apk`

#### Release Build (APK)

1. Click **Build → Generate Signed Bundle / APK**
2. Select **APK** and click **Next**
3. Create or select a keystore (see "Code Signing" section below)
4. Enter keystore password and key details
5. Click **Next**
6. Select **release** build variant
7. Click **Finish**
8. APK location: `app/build/outputs/apk/release/app-release.apk`

#### App Bundle (AAB) for Play Store

1. Click **Build → Generate Signed Bundle / APK**
2. Select **Android App Bundle** and click **Next**
3. Create or select a keystore
4. Enter keystore credentials
5. Click **Next**
6. Select **release** build variant
7. Click **Finish**
8. AAB location: `app/build/outputs/bundle/release/app-release.aab`

### Option 2: Build via Command Line

#### Debug APK

```bash
# For Unix/Linux/macOS:
./gradlew assembleDebug

# For Windows:
gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

#### Release APK (unsigned)

```bash
# For Unix/Linux/macOS:
./gradlew assembleRelease

# For Windows:
gradlew.bat assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

#### Release App Bundle

```bash
# For Unix/Linux/macOS:
./gradlew bundleRelease

# For Windows:
gradlew.bat bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

### Build Variants

The project includes two build variants:

- **debug**: Development builds with debugging enabled
- **release**: Production builds with optimizations

## Code Signing

For release builds, you need a keystore for code signing.

### Create a New Keystore

```bash
keytool -genkey -v -keystore verumdec.keystore -alias verumdec -keyalg RSA -keysize 2048 -validity 10000
```

You'll be prompted to enter:
- Keystore password
- Key password
- Your name
- Organizational unit
- Organization
- City/Locality
- State/Province
- Country code

**Important:** Keep your keystore and passwords secure. You'll need them for all future app updates.

### Configure Signing in Gradle (Optional)

Create `keystore.properties` in the project root:

```properties
storeFile=/path/to/verumdec.keystore
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=verumdec
keyPassword=YOUR_KEY_PASSWORD
```

**Note:** Add `keystore.properties` to `.gitignore` to keep credentials private.

## Running the Application

### On an Emulator

1. Click **Tools → Device Manager**
2. Click **Create Device**
3. Select a device profile (e.g., Pixel 6)
4. Select a system image (API 34 recommended)
5. Click **Finish**
6. Click the **Run** button (green play icon) or press **Shift+F10**
7. Select your emulator from the device list

### On a Physical Device

1. Enable **Developer Options** on your Android device:
   - Go to **Settings → About Phone**
   - Tap **Build Number** 7 times
2. Enable **USB Debugging**:
   - Go to **Settings → Developer Options**
   - Enable **USB Debugging**
3. Connect your device via USB
4. Allow USB debugging when prompted on your device
5. Click the **Run** button in Android Studio
6. Select your device from the list

### Install APK Directly

```bash
# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Install release APK
adb install app/build/outputs/apk/release/app-release.apk
```

## Testing

### Run Unit Tests

```bash
# Via Gradle
./gradlew test

# Or in Android Studio
# Right-click on 'test' folder → Run 'All Tests'
```

### Run Instrumented Tests

```bash
# Via Gradle (requires connected device/emulator)
./gradlew connectedAndroidTest
```

## Troubleshooting

### Common Issues

#### "SDK location not found"

**Solution:** Create `local.properties` with correct Android SDK path (see Step 2).

#### "Unsupported Gradle version"

**Solution:** Update Gradle wrapper:
```bash
./gradlew wrapper --gradle-version=8.4
```

#### "Build tools version not available"

**Solution:** Install the required build tools via SDK Manager or update `build.gradle.kts` to use an installed version.

#### "Out of memory" during build

**Solution:** Increase Gradle memory in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

#### Missing dependencies

**Solution:** Ensure you have internet connection and run:
```bash
./gradlew --refresh-dependencies
```

### Clean Build

If you encounter build issues, try cleaning:

```bash
# Via Gradle
./gradlew clean build

# In Android Studio
# Build → Clean Project
# Build → Rebuild Project
```

## Project Structure

```
Verumdec/
├── app/                          # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/verumdec/
│   │   │   │   ├── engine/      # Core analysis engine
│   │   │   │   ├── ui/          # UI components (Activities, Adapters)
│   │   │   │   └── data/        # Data models
│   │   │   ├── res/             # Resources (layouts, strings, etc.)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                # Unit tests
│   └── build.gradle.kts         # App module build configuration
├── build.gradle.kts             # Root build configuration
├── settings.gradle.kts          # Project settings
├── gradle.properties            # Gradle properties
└── local.properties             # Local configuration (SDK path)
```

## Dependencies

Key libraries used in this project:

- **AndroidX Core** - Core Android functionality
- **Material Components** - Material Design UI components
- **PDFBox Android** - PDF processing
- **ML Kit** - Text recognition (OCR)
- **Kotlin Coroutines** - Asynchronous programming
- **RecyclerView** - List displays
- **Navigation** - Screen navigation

See `app/build.gradle.kts` for complete dependency list.

## Next Steps

After successfully building the app:

1. **Test the application** thoroughly on real devices
2. **Customize branding** (app icon, colors, strings)
3. **Configure ProGuard** for release builds (optional)
4. **Prepare for distribution** via Google Play Store or direct APK sharing

## Additional Resources

- [Android Studio User Guide](https://developer.android.com/studio/intro)
- [Gradle Build Configuration](https://developer.android.com/build)
- [App Signing](https://developer.android.com/studio/publish/app-signing)
- [Publishing to Play Store](https://developer.android.com/studio/publish)

## Support

For issues or questions about building the app:

1. Check this BUILDING.md document
2. Review the troubleshooting section
3. Check Android Studio's error messages and suggestions
4. Consult the Android Developer documentation

---

**Verum Omnis - Offline Contradiction Engine**
*Building truth through forensic analysis*

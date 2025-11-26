# Building Verumdec APK

This document provides detailed instructions for building the Verumdec Android APK.

## Quick Start

The fastest way to get an APK is through **GitHub Actions**:

1. Go to the repository's **Actions** tab
2. Click on **Build APK** workflow
3. Click **Run workflow**
4. Once complete, download the APK from the **Artifacts** section

## Prerequisites

### Software Requirements
- **JDK 17** or later ([Adoptium](https://adoptium.net/) recommended)
- **Android Studio** Arctic Fox (2020.3.1) or later OR Android SDK Command Line Tools
- **Android SDK** with API 34 (Android 14)

### Environment Setup

1. **Install JDK 17**:
   ```bash
   # macOS (using Homebrew)
   brew install openjdk@17
   
   # Ubuntu/Debian
   sudo apt install openjdk-17-jdk
   
   # Windows: Download from https://adoptium.net/
   ```

2. **Install Android SDK**:
   - Option A: Install Android Studio (includes SDK)
   - Option B: Install Command Line Tools from https://developer.android.com/studio#command-tools

3. **Set Environment Variables**:
   ```bash
   # Add to ~/.bashrc or ~/.zshrc
   export ANDROID_HOME=$HOME/Android/Sdk  # or your SDK path
   export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
   ```

## Building with Android Studio (Recommended)

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Liamhigh/Verumdec.git
   cd Verumdec
   ```

2. **Open in Android Studio**:
   - File → Open → Select the `Verumdec` folder
   - Wait for Gradle sync to complete

3. **Build Debug APK**:
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

4. **Build Signed Release APK**:
   - Build → Generate Signed Bundle / APK
   - Select APK
   - Create or select a keystore
   - Enter credentials and build

## Building from Command Line

1. **Clone and navigate**:
   ```bash
   git clone https://github.com/Liamhigh/Verumdec.git
   cd Verumdec
   ```

2. **Create local.properties** (if not exists):
   ```bash
   echo "sdk.dir=$ANDROID_HOME" > local.properties
   ```

3. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   Output: `app/build/outputs/apk/debug/app-debug.apk`

4. **Build Release APK** (unsigned):
   ```bash
   ./gradlew assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Signing the APK

For distribution, you need to sign the release APK:

1. **Create a keystore** (first time only):
   ```bash
   keytool -genkey -v -keystore verumdec-release-key.jks \
           -keyalg RSA -keysize 2048 -validity 10000 \
           -alias verumdec
   ```

2. **Sign the APK**:
   ```bash
   # Using Android's build system
   ./gradlew assembleRelease
   
   # Or manually
   apksigner sign --ks verumdec-release-key.jks \
                  --out app-signed.apk \
                  app/build/outputs/apk/release/app-release-unsigned.apk
   ```

## Troubleshooting

### Gradle sync fails
```bash
# Clean and retry
./gradlew clean
./gradlew --refresh-dependencies assembleDebug
```

### SDK not found
Ensure `ANDROID_HOME` is set correctly:
```bash
echo $ANDROID_HOME  # Should print your SDK path
ls $ANDROID_HOME/platforms  # Should list android-XX folders
```

### Java version issues
```bash
java -version  # Should show version 17 or higher
```

### Missing build tools
```bash
# Install required SDK components
sdkmanager "build-tools;34.0.0" "platforms;android-34"
```

## Project Structure

```
Verumdec/
├── app/                    # Main application module
│   ├── src/main/
│   │   ├── java/com/verumdec/
│   │   │   ├── data/      # Data models
│   │   │   ├── engine/    # Core engine components
│   │   │   └── ui/        # Activities and adapters
│   │   └── res/           # Android resources
│   └── build.gradle.kts
├── core/                   # Shared utilities module
├── ocr/                    # OCR processing module
├── pdf/                    # PDF processing module
├── entity/                 # Entity extraction module
├── timeline/               # Timeline generation module
├── analysis/               # Contradiction analysis module
├── report/                 # Report generation module
├── ui/                     # Shared UI components module
├── build.gradle.kts        # Root build configuration
└── settings.gradle.kts     # Module settings
```

## Continuous Integration

The repository includes a GitHub Actions workflow that automatically:
- Builds debug and release APKs on every push
- Runs unit tests
- Uploads APKs as artifacts

To trigger a build:
1. Push to `main` or `master` branch
2. Open a Pull Request
3. Manually trigger from Actions tab

## Next Steps After Building

1. **Install on device**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Enable installation from unknown sources** (Settings → Security)

3. **Launch Verumdec** and start analyzing your documents!

---

*Verumdec - Offline Contradiction Engine for Legal-Grade Forensic Analysis*

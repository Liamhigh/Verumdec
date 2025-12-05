# Verumdec Android Studio Quick Start

Get the Verumdec Android app building in under 10 minutes.

## Prerequisites

- **Android Studio** (Hedgehog or newer)
- **8 GB RAM** minimum
- **Internet connection** (for initial dependency download)

## Step-by-Step Guide

### 1. Install Android Studio

Download from https://developer.android.com/studio

**Windows:**
- Run the installer
- Accept defaults
- Let it install Android SDK

**macOS:**
- Drag Android Studio to Applications
- Launch and complete setup wizard
- Install recommended SDK components

**Linux:**
- Extract the archive
- Run `studio.sh`
- Complete setup wizard

### 2. Clone the Repository

```bash
git clone https://github.com/Liamhigh/Verumdec.git
```

Or download ZIP from GitHub and extract.

### 3. Open in Android Studio

1. Launch Android Studio
2. Click **"Open"** (not "New Project")
3. Navigate to the `Verumdec` folder
4. Click **OK**

Android Studio will:
- Detect it's a Gradle project
- Start importing
- Download Gradle wrapper (if needed)
- Sync Gradle files

**This may take 2-5 minutes on first open.**

### 4. Configure SDK Path

If you see "SDK location not found" error:

**Option A: Let Android Studio configure it**
- Click "Set SDK location"
- Accept the default path
- Click OK

**Option B: Manual configuration**

Create `local.properties` in project root:

```properties
# macOS/Linux:
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk

# Windows:
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
```

### 5. Install Missing SDK Components

If prompted "Install missing SDK packages":

1. Click **"Install missing..."**
2. Review the components (typically SDK Platform 34)
3. Accept license agreements
4. Click **Finish**
5. Wait for download and installation

### 6. Sync Gradle

Once SDK is configured:

1. Click **"Sync Project with Gradle Files"** (elephant icon in toolbar)
2. Wait for sync to complete (1-3 minutes)
3. Gradle should download all dependencies

### 7. Build the Project

**Option A: Build Menu**
1. Go to **Build → Make Project**
2. Wait for build to complete
3. Check Build window for any errors

**Option B: Run Configuration**
1. Click **Run → Run 'app'**
2. Android Studio will build automatically

### 8. Run the App

**On Emulator:**

1. Click **Tools → Device Manager**
2. Click **Create Device**
3. Select **Pixel 6** (or any recent device)
4. Click **Next**
5. Select **API 34** system image
6. Click **Next**, then **Finish**
7. Start the emulator
8. Click **Run** (green play button)

**On Real Device:**

1. Enable Developer Options on your Android phone:
   - Settings → About Phone
   - Tap "Build Number" 7 times
2. Enable USB Debugging:
   - Settings → Developer Options
   - Toggle "USB Debugging" ON
3. Connect phone via USB
4. Allow USB debugging when prompted on phone
5. Click **Run** in Android Studio
6. Select your device from the list

### 9. Generate APK

To create an installable APK:

**Debug APK (for testing):**
1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Wait for build
3. Click **"locate"** in notification
4. APK is at: `app/build/outputs/apk/debug/app-debug.apk`

**Release APK (for distribution):**
1. **Build → Generate Signed Bundle / APK**
2. Select **APK**
3. Click **Next**
4. Click **"Create new..."** to create keystore
5. Fill in keystore details:
   - Path: Choose location (e.g., `verumdec.keystore`)
   - Password: Create strong password
   - Alias: `verumdec`
   - Validity: 25 years
6. Click **OK**, then **Next**
7. Select **release** variant
8. Click **Finish**

**Save your keystore and passwords!** You'll need them for updates.

## Troubleshooting

### "Gradle sync failed"

**Fix:**
- Check internet connection
- Click **File → Invalidate Caches → Invalidate and Restart**
- Delete `.gradle` folder in project root
- Re-sync

### "Build tools version not available"

**Fix:**
- Open **Tools → SDK Manager**
- Click **SDK Tools** tab
- Check **Android SDK Build-Tools 34**
- Click **Apply**

### "Unsupported Gradle version"

**Fix:**
- Update Gradle wrapper:
  ```bash
  ./gradlew wrapper --gradle-version=8.4
  ```

### "Out of memory"

**Fix:**
Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

### Emulator won't start

**Fix:**
- Enable virtualization in BIOS (Intel VT-x or AMD-V)
- Windows: Disable Hyper-V
- macOS: Grant permissions in System Preferences

## Next Steps

✅ **App is running!** Now you can:

1. **Explore the code**
   - `app/src/main/java/com/verumdec/engine/` - Core engines
   - `app/src/main/java/com/verumdec/ui/` - UI screens
   - `app/src/main/res/layout/` - XML layouts

2. **Customize branding**
   - Edit `app/src/main/res/values/strings.xml`
   - Change colors in `app/src/main/res/values/colors.xml`
   - Replace app icon in `app/src/main/res/mipmap-*/`

3. **Test features**
   - Add evidence files
   - Run analysis
   - Generate reports

4. **Read detailed docs**
   - [BUILDING.md](BUILDING.md) - Complete build guide
   - [PROJECT_STATUS.md](PROJECT_STATUS.md) - Current status
   - [DEPLOYMENT.md](DEPLOYMENT.md) - Deployment options

## Common Development Tasks

### Clean build
```bash
./gradlew clean build
```

### Run tests
```bash
./gradlew test
```

### Update dependencies
Edit `app/build.gradle.kts`, then sync Gradle.

### Add new library
```kotlin
// In app/build.gradle.kts, dependencies section:
implementation("group:artifact:version")
```
Then sync Gradle.

### Change app name
Edit `app/src/main/res/values/strings.xml`:
```xml
<string name="app_name">Your App Name</string>
```

### Change app icon
Replace images in:
- `app/src/main/res/mipmap-mdpi/ic_launcher.png` (48x48)
- `app/src/main/res/mipmap-hdpi/ic_launcher.png` (72x72)
- `app/src/main/res/mipmap-xhdpi/ic_launcher.png` (96x96)
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` (144x144)
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` (192x192)

Or use **Image Asset Studio**:
- Right-click `res` folder
- **New → Image Asset**
- Follow wizard

## Project Structure

```
Verumdec/
├── app/                    # Main app module
│   ├── src/
│   │   ├── main/          # Main source code
│   │   │   ├── java/      # Kotlin code
│   │   │   ├── res/       # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/          # Unit tests
│   └── build.gradle.kts   # App build config
├── gradle/                 # Gradle wrapper
├── build.gradle.kts       # Root build config
├── settings.gradle.kts    # Project settings
├── local.properties       # SDK path (you create this)
└── README.md              # Project documentation
```

## Getting Help

- **Build errors**: Check Android Studio's Build window
- **Runtime errors**: Check Logcat window
- **Gradle issues**: Try "Clean Project" and "Rebuild Project"
- **SDK issues**: Check SDK Manager for missing components

## Success Checklist

- [ ] Android Studio installed and launched
- [ ] Project opened successfully
- [ ] Gradle sync completed without errors
- [ ] Project builds successfully
- [ ] App runs on emulator or device
- [ ] APK generated successfully

---

**You're ready to develop!** 🚀

For detailed documentation, see [BUILDING.md](BUILDING.md).

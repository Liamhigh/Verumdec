# Android Studio Quick Start Guide

## Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or newer
- **JDK**: 17 or newer
- **SDK**: Android SDK 34 (with SDK Tools)
- **Internet connection**: Required for initial Gradle sync

## Step 1: Clone the Repository

```bash
git clone https://github.com/Liamhigh/Verumdec.git
cd Verumdec
```

## Step 2: Open in Android Studio

1. Launch Android Studio
2. Click **File → Open** (or "Open" on welcome screen)
3. Navigate to the cloned `Verumdec` folder
4. Click **OK**

Android Studio will:
- Detect the Gradle project
- Download dependencies
- Sync Gradle files
- Build the project

**Expected time**: 2-5 minutes (first time)

## Step 3: Verify Gradle Sync

Wait for the Gradle sync to complete. You should see:
```
BUILD SUCCESSFUL in Xs
```

If sync fails:
1. **File → Invalidate Caches → Invalidate and Restart**
2. Check internet connection
3. Verify JDK 17 is configured: **File → Project Structure → SDK Location**

## Step 4: Build the Project

**Option A: Menu**
- **Build → Make Project** (Ctrl+F9 / Cmd+F9)

**Option B: Gradle Panel**
1. Open Gradle panel (right side)
2. Expand **app → Tasks → build**
3. Double-click **assembleDebug**

## Step 5: Run on Emulator

### Create Emulator (if needed)
1. **Tools → Device Manager**
2. Click **Create Device**
3. Select device (e.g., Pixel 6)
4. Select system image: **API 34 (Android 14.0)**
5. Click **Finish**

### Run the App
1. Click **Run → Run 'app'** (Shift+F10 / Ctrl+R)
2. Select emulator from device list
3. Click **OK**

The app will install and launch automatically.

## Step 6: Run on Physical Device

### Enable Developer Options
1. On your Android device:
   - Go to **Settings → About Phone**
   - Tap **Build Number** 7 times
   - Developer options enabled!

2. Enable USB Debugging:
   - Go to **Settings → Developer Options**
   - Enable **USB Debugging**

### Connect and Run
1. Connect device via USB
2. Allow USB debugging when prompted
3. In Android Studio, select your device from dropdown
4. Click **Run**

## Testing the App

### Create a Case
1. Open app
2. Tap **"New Case"**
3. Enter case name (e.g., "Test Case 1")
4. Tap **"Create"**

### Add Evidence
1. You'll be on CaseDetailActivity
2. Tap **"Add Text Note"**
3. Enter note text
4. Tap **"Add"**
5. Or tap **"Scan Evidence"** to add photos/files

### Generate Report
1. After adding evidence
2. Tap **"Generate Report"**
3. View report in ReportViewerActivity
4. Report includes SHA-512 hash seal

## Build Variants

### Debug Build (for testing)
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build (for production)
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

## Common Issues

### Issue: "Gradle sync failed"
**Solution**: 
- Check internet connection
- File → Invalidate Caches → Invalidate and Restart
- Update Gradle: File → Project Structure → Project → Gradle Version

### Issue: "SDK not found"
**Solution**:
- File → Project Structure → SDK Location
- Set Android SDK location
- Download missing SDK components via SDK Manager

### Issue: "Build failed: Cannot resolve symbol"
**Solution**:
- Build → Clean Project
- Build → Rebuild Project
- Invalidate caches and restart

### Issue: "Emulator won't start"
**Solution**:
- Check HAXM/Hypervisor is installed
- Tools → SDK Manager → SDK Tools
- Install "Intel x86 Emulator Accelerator"

## Project Structure

```
Verumdec/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/verumdec/
│   │       │   ├── engine/          # Forensic logic
│   │       │   │   ├── ForensicEngine.kt
│   │       │   │   ├── ContradictionEngine.kt
│   │       │   │   └── ...
│   │       │   ├── ui/              # Activities
│   │       │   │   ├── MainActivity.kt
│   │       │   │   ├── CaseDetailActivity.kt
│   │       │   │   ├── ScannerActivity.kt
│   │       │   │   ├── ReportViewerActivity.kt
│   │       │   │   └── ...
│   │       │   └── data/            # Data models
│   │       │       └── Models.kt
│   │       ├── res/                 # Resources
│   │       │   ├── layout/
│   │       │   ├── drawable/
│   │       │   └── values/
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## Key Files

- **ForensicEngine.kt**: Case management, evidence storage, hashing
- **ScannerActivity.kt**: Photo capture and file selection
- **CaseDetailActivity.kt**: Case management UI
- **ReportViewerActivity.kt**: Report display
- **MainActivity.kt**: Case creation

## Permissions

The app requests these permissions at runtime:
- **CAMERA**: For capturing photos
- **READ_MEDIA_IMAGES**: For selecting images (Android 13+)
- **READ_EXTERNAL_STORAGE**: For file access (Android 12 and below)

## Next Steps

After successful build and testing:

1. **Code Signing**: Configure signing for release builds
2. **ProGuard**: Enable minification for production
3. **Testing**: Run unit tests and UI tests
4. **Deployment**: Upload to Google Play Console

## Support

For build issues or questions:
- Check `PRODUCTION_READINESS.md` for detailed assessment
- Review `IMPLEMENTATION_SUMMARY.md` for feature details
- Check `VERIFICATION_CHECKLIST.md` for requirements verification

---

**The app is ready for Android Studio!** Simply open the project, sync Gradle, and run. All code is complete and functional.

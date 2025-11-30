# Local Testing Setup Guide

> **Quick Note**: Tests run automatically on GitHub Actions. For local development, follow these steps.

## Prerequisites

### 1. Install Android SDK

**Option A: Via Android Studio** (Easiest)
- Download Android Studio: https://developer.android.com/studio
- Launch Android Studio → SDK Manager
- Install API 34 and build tools
- Close Android Studio (it will set `ANDROID_HOME`)

**Option B: Command Line**
```bash
# macOS (Homebrew)
brew install android-commandlinetools
export ANDROID_HOME=/usr/local/share/android-commandlinetools

# Linux (Ubuntu)
sudo apt-get install android-sdk
export ANDROID_HOME=/usr/lib/android-sdk

# Then download API 34
sdkmanager "platforms;android-34" "build-tools;34.0.0"
```

### 2. Set ANDROID_HOME

Add to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/tools/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

Then reload:
```bash
source ~/.bashrc  # or ~/.zshrc for macOS
```

### 3. Verify Setup

```bash
echo $ANDROID_HOME
# Should output: /path/to/android/sdk

adb version
# Should show ADB version

sdkmanager --list_installed
# Should show "Installed packages"
```

---

## Running Tests Locally

```bash
cd /workspaces/Verumdec

# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests ContradictionAnalyzerTest

# Run with verbose output
./gradlew test --info

# Clean and test
./gradlew clean test
```

### Expected Output

```
> Task :app:testDebugUnitTest

com.verumdec.engine.ContradictionAnalyzerTest
    ✓ testDirectContradictionDetection
    ✓ testDenialFollowedByAdmission
    ✓ testCrossDocumentContradiction
    ✓ testThirdPartyContradiction
    ✓ testNoFalsePositives
    ✓ testEmptyEvidence

com.verumdec.engine.LiabilityCalculatorTest
    ✓ testBasicLiabilityCalculation
    ✓ testNoLiabilityWithoutContradictions
    ✓ testMultipleContradictionsIncreaseLiability
    ✓ testCriticalContributesMoreThanHigh
    ✓ testEmptyInputs

com.verumdec.engine.ContradictionEngineTest
    ✓ testCaseSummaryCalculation
    ✓ testEmptyCaseSummary
    ✓ testMultipleContradictionSeverities

BUILD SUCCESSFUL in 30s
```

---

## Troubleshooting

### Error: "SDK location not found"

```bash
# Solution 1: Set ANDROID_HOME
export ANDROID_HOME=/path/to/sdk
./gradlew test

# Solution 2: Create local.properties
echo "sdk.dir=/path/to/sdk" > local.properties
./gradlew test

# Solution 3: Use Android Studio default
export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
export ANDROID_HOME=$HOME/Android/Sdk           # Linux
./gradlew test
```

### Error: "API 34 not found"

```bash
# Install the API level
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"

# Or in Android Studio:
# SDK Manager → Platforms tab → Select API 34 → Install
```

### Error: "Gradle sync failed"

```bash
# Refresh dependencies
./gradlew --refresh-dependencies

# Clean and rebuild
./gradlew clean build

# Update gradle
./gradlew wrapper --gradle-version=8.4
```

### Tests run but pass/fail inconsistently

```bash
# Run with clean build
./gradlew clean test

# Check for environment issues
gradle --version
java -version
echo $ANDROID_HOME
```

---

## GitHub Actions (Cloud Testing)

Tests automatically run on:
- Every push to `main` or `develop`
- Every pull request to `main`
- Manual trigger via "Run workflow" button

**No setup needed** - GitHub Actions runner has everything configured.

**View results**:
1. Go to GitHub repo → **Actions** tab
2. Click on the workflow run
3. Expand **test** or **build** job to see logs
4. Download artifacts (APKs, test reports)

---

## Test Reports

After running tests locally:

```bash
# View HTML test report
open app/build/reports/tests/testDebugUnitTest/index.html  # macOS
xdg-open app/build/reports/tests/testDebugUnitTest/index.html  # Linux
start app/build/reports/tests/testDebugUnitTest/index.html  # Windows
```

---

## Performance Tips

```bash
# Run tests faster with parallelization
./gradlew test -Dorg.gradle.workers.max=4

# Skip certain tests
./gradlew test --exclude-task :app:testReleaseUnitTest

# Cache dependencies
./gradlew build --build-cache

# Offline mode (if all deps downloaded)
./gradlew test --offline
```

---

## CI/CD vs Local

| Aspect | Local | GitHub Actions |
|--------|-------|----------------|
| SDK Setup | Manual | Auto |
| Test Speed | 2-5 min | 5-10 min |
| Environment | Your machine | Ubuntu VM |
| Consistency | Varies | Exact same every time |
| Debugging | Can inspect directly | Via logs |

---

*Last Updated: November 30, 2025*

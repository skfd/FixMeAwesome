# Survey Me - Deployment and Release Strategy

## 1. Overview

This document outlines the comprehensive deployment and release strategies for the Survey Me Android application, covering build processes, continuous integration/continuous deployment (CI/CD) pipelines, distribution channels, release management, and monitoring procedures.

### 1.1 Deployment Objectives

- **Reliability**: Ensure consistent, error-free deployments
- **Automation**: Minimize manual intervention in release processes
- **Traceability**: Track all changes from commit to production
- **Rollback Capability**: Quick recovery from problematic releases
- **Multi-channel Distribution**: Support various distribution platforms
- **Quality Assurance**: Automated testing before each release
- **Performance Monitoring**: Track app performance post-deployment
- **User Feedback Integration**: Rapid response to user issues

### 1.2 Release Principles

1. **Semantic Versioning**: Follow SemVer (MAJOR.MINOR.PATCH) strictly
2. **Release Trains**: Regular, predictable release schedules
3. **Feature Flags**: Gradual feature rollouts and A/B testing
4. **Staged Rollouts**: Progressive deployment to user segments
5. **Automated Testing**: Comprehensive test suite execution
6. **Code Signing**: Secure app signing and certificate management
7. **Release Notes**: Detailed changelog for each release
8. **Rollback Plan**: Documented rollback procedures

## 2. Build Configuration

### 2.1 Gradle Build Configuration

```gradle
android {
    compileSdk 34
    buildToolsVersion "34.0.0"

    defaultConfig {
        applicationId "com.surveyme"
        minSdk 23
        targetSdk 34
        versionCode generateVersionCode()
        versionName generateVersionName()

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"

        // Build config fields
        buildConfigField "String", "API_BASE_URL", "\"${getApiUrl()}\""
        buildConfigField "String", "BUILD_TIME", "\"${getBuildTime()}\""
        buildConfigField "String", "GIT_SHA", "\"${getGitSha()}\""

        // Resource configs
        resConfigs "en", "de", "fr", "es", "it", "pt", "ja", "ko", "zh"

        // Vector drawables
        vectorDrawables {
            useSupportLibrary true
        }
    }

    signingConfigs {
        debug {
            storeFile file("debug.keystore")
            storePassword "android"
            keyAlias "androiddebugkey"
            keyPassword "android"
        }

        release {
            storeFile file(System.getenv("KEYSTORE_FILE") ?: "release.keystore")
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix ".debug"
            versionNameSuffix "-DEBUG"
            debuggable true
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.debug

            buildConfigField "Boolean", "ENABLE_LOGGING", "true"
            buildConfigField "Boolean", "ENABLE_CRASHLYTICS", "false"
        }

        staging {
            initWith debug
            applicationIdSuffix ".staging"
            versionNameSuffix "-STAGING"
            debuggable true
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'

            buildConfigField "Boolean", "ENABLE_LOGGING", "true"
            buildConfigField "Boolean", "ENABLE_CRASHLYTICS", "true"
        }

        release {
            debuggable false
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release

            buildConfigField "Boolean", "ENABLE_LOGGING", "false"
            buildConfigField "Boolean", "ENABLE_CRASHLYTICS", "true"

            // Enable R8 full mode
            proguardFiles 'proguard-rules-release.pro'
        }

        benchmark {
            initWith release
            signingConfig signingConfigs.debug
            matchingFallbacks = ['release']
            proguardFiles 'proguard-rules-benchmark.pro'
        }
    }

    flavorDimensions "distribution"
    productFlavors {
        googlePlay {
            dimension "distribution"
            buildConfigField "String", "DISTRIBUTION_CHANNEL", "\"GOOGLE_PLAY\""
        }

        fdroid {
            dimension "distribution"
            buildConfigField "String", "DISTRIBUTION_CHANNEL", "\"FDROID\""
            // Remove proprietary dependencies
        }

        github {
            dimension "distribution"
            buildConfigField "String", "DISTRIBUTION_CHANNEL", "\"GITHUB\""
        }

        huawei {
            dimension "distribution"
            buildConfigField "String", "DISTRIBUTION_CHANNEL", "\"HUAWEI_APP_GALLERY\""
        }
    }

    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    packagingOptions {
        resources {
            excludes += [
                'META-INF/DEPENDENCIES',
                'META-INF/LICENSE',
                'META-INF/LICENSE.txt',
                'META-INF/license.txt',
                'META-INF/NOTICE',
                'META-INF/NOTICE.txt',
                'META-INF/notice.txt',
                'META-INF/ASL2.0'
            ]
        }
    }
}

def generateVersionCode() {
    // Generate version code based on date and build number
    def date = new Date()
    def formattedDate = date.format('yyMMdd')
    def buildNumber = System.getenv("BUILD_NUMBER") ?: "0"
    return Integer.parseInt(formattedDate + buildNumber.padLeft(3, '0'))
}

def generateVersionName() {
    def versionMajor = 1
    def versionMinor = 0
    def versionPatch = System.getenv("PATCH_NUMBER") ?: "0"
    return "${versionMajor}.${versionMinor}.${versionPatch}"
}

def getGitSha() {
    def process = "git rev-parse --short HEAD".execute()
    return process.text.trim()
}

def getBuildTime() {
    return new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"))
}

def getApiUrl() {
    switch (System.getenv("ENVIRONMENT") ?: "production") {
        case "development": return "https://dev-api.surveyme.app"
        case "staging": return "https://staging-api.surveyme.app"
        case "production": return "https://api.surveyme.app"
        default: return "https://api.surveyme.app"
    }
}
```

### 2.2 ProGuard/R8 Configuration

```proguard
# proguard-rules-release.pro

# Keep data classes
-keep class com.surveyme.domain.model.** { *; }
-keep class com.surveyme.data.remote.dto.** { *; }

# Keep Room entities and DAOs
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# Keep Retrofit interfaces
-keep interface com.surveyme.data.remote.api.** { *; }

# Serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Kotlin
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata { public <methods>; }

# Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# OSMdroid
-keep class org.osmdroid.** { *; }
-keep interface org.osmdroid.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
```

## 3. CI/CD Pipeline

### 3.1 GitHub Actions Workflow

```yaml
# .github/workflows/android-ci.yml
name: Android CI/CD

on:
  push:
    branches: [ main, develop, release/* ]
    tags: [ 'v*' ]
  pull_request:
    branches: [ main, develop ]
  workflow_dispatch:

env:
  JAVA_VERSION: '17'
  ANDROID_SDK_VERSION: '34'
  GRADLE_VERSION: '8.2'

jobs:
  # Code quality checks
  lint:
    name: Lint Check
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run Lint
        run: ./gradlew lint

      - name: Upload Lint results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: lint-results
          path: app/build/reports/lint-results-*.html

  # Unit tests
  unit-tests:
    name: Unit Tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      - name: Run Unit Tests
        run: ./gradlew test

      - name: Generate Test Report
        run: ./gradlew jacocoTestReport

      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-results
          path: |
            app/build/reports/tests/
            app/build/reports/jacoco/

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
          flags: unit-tests
          fail_ci_if_error: true

  # Instrumented tests
  instrumented-tests:
    name: Instrumented Tests
    runs-on: macos-latest
    strategy:
      matrix:
        api-level: [26, 30, 33]
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      - name: AVD cache
        uses: actions/cache@v3
        id: avd-cache
        with:
          path: |
            ~/.android/avd/*
            ~/.android/adb*
          key: avd-${{ matrix.api-level }}

      - name: Create AVD and generate snapshot for caching
        if: steps.avd-cache.outputs.cache-hit != 'true'
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: ${{ matrix.api-level }}
          arch: x86_64
          force-avd-creation: false
          emulator-options: -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -camera-back none
          disable-animations: false
          script: echo "Generated AVD snapshot for caching."

      - name: Run Instrumented Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: ${{ matrix.api-level }}
          arch: x86_64
          force-avd-creation: false
          emulator-options: -no-snapshot-save -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -camera-back none
          disable-animations: true
          script: ./gradlew connectedAndroidTest

      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: instrumented-test-results-${{ matrix.api-level }}
          path: app/build/reports/androidTests/

  # Build APK and AAB
  build:
    name: Build APK/AAB
    runs-on: ubuntu-latest
    needs: [lint, unit-tests]
    if: github.event_name == 'push'
    strategy:
      matrix:
        build-type: [debug, staging, release]
        distribution: [googlePlay, fdroid, github]
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'

      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      - name: Decode Keystore
        if: matrix.build-type == 'release'
        env:
          ENCODED_KEYSTORE: ${{ secrets.KEYSTORE_BASE64 }}
        run: |
          echo $ENCODED_KEYSTORE | base64 -d > app/release.keystore

      - name: Build APK
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          BUILD_NUMBER: ${{ github.run_number }}
        run: |
          ./gradlew assemble${{ matrix.distribution }}${{ matrix.build-type }}

      - name: Build AAB
        if: matrix.distribution == 'googlePlay' && matrix.build-type == 'release'
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: |
          ./gradlew bundle${{ matrix.distribution }}${{ matrix.build-type }}

      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: apk-${{ matrix.distribution }}-${{ matrix.build-type }}
          path: app/build/outputs/apk/**/*.apk

      - name: Upload AAB
        if: matrix.distribution == 'googlePlay' && matrix.build-type == 'release'
        uses: actions/upload-artifact@v3
        with:
          name: aab-${{ matrix.distribution }}-${{ matrix.build-type }}
          path: app/build/outputs/bundle/**/*.aab

  # Deploy to Google Play
  deploy-play-store:
    name: Deploy to Play Store
    runs-on: ubuntu-latest
    needs: [build, instrumented-tests]
    if: startsWith(github.ref, 'refs/tags/v')
    steps:
      - uses: actions/checkout@v4

      - name: Download AAB
        uses: actions/download-artifact@v3
        with:
          name: aab-googlePlay-release
          path: app/build/outputs/bundle/

      - name: Create Release Notes
        run: |
          echo "Release ${GITHUB_REF#refs/tags/}" > release-notes.txt
          git log --pretty=format:"- %s" $(git describe --tags --abbrev=0 HEAD^)..HEAD >> release-notes.txt

      - name: Upload to Play Console
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJson: ${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}
          packageName: com.surveyme
          releaseFiles: app/build/outputs/bundle/**/*.aab
          track: internal
          status: draft
          whatsNewDirectory: release-notes/

  # Create GitHub Release
  github-release:
    name: Create GitHub Release
    runs-on: ubuntu-latest
    needs: build
    if: startsWith(github.ref, 'refs/tags/v')
    steps:
      - uses: actions/checkout@v4

      - name: Download APKs
        uses: actions/download-artifact@v3
        with:
          path: apks/

      - name: Generate Changelog
        id: changelog
        run: |
          echo "CHANGELOG<<EOF" >> $GITHUB_ENV
          git log --pretty=format:"- %s" $(git describe --tags --abbrev=0 HEAD^)..HEAD >> $GITHUB_ENV
          echo "EOF" >> $GITHUB_ENV

      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          tag_name: ${{ github.ref }}
          name: Release ${{ github.ref }}
          body: |
            ## Changes
            ${{ env.CHANGELOG }}

            ## Downloads
            - Google Play APK: `surveyme-googleplay-release.apk`
            - F-Droid APK: `surveyme-fdroid-release.apk`
            - Universal APK: `surveyme-github-release.apk`
          files: |
            apks/**/*.apk
          draft: false
          prerelease: false
```

### 3.2 GitLab CI Configuration

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - deploy
  - release

variables:
  ANDROID_COMPILE_SDK: "34"
  ANDROID_BUILD_TOOLS: "34.0.0"
  ANDROID_SDK_TOOLS: "9477386"

before_script:
  - apt-get --quiet update --yes
  - apt-get --quiet install --yes wget tar unzip lib32stdc++6 lib32z1
  - export ANDROID_HOME="${PWD}/android-sdk-linux"
  - install -d $ANDROID_HOME
  - wget --output-document=$ANDROID_HOME/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_TOOLS}_latest.zip
  - pushd $ANDROID_HOME
  - unzip -d cmdline-tools cmdline-tools.zip
  - pushd cmdline-tools
  - mv cmdline-tools latest
  - popd
  - popd
  - export PATH=$PATH:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools
  - sdkmanager --sdk_root=${ANDROID_HOME} --update
  - echo "y" | sdkmanager --sdk_root=${ANDROID_HOME} "platforms;android-${ANDROID_COMPILE_SDK}" "platform-tools" "build-tools;${ANDROID_BUILD_TOOLS}"
  - chmod +x ./gradlew

build:
  stage: build
  script:
    - ./gradlew assembleDebug
  artifacts:
    paths:
      - app/build/outputs/apk/debug/

unit-test:
  stage: test
  script:
    - ./gradlew test

lint:
  stage: test
  script:
    - ./gradlew lint
  artifacts:
    paths:
      - app/build/reports/

deploy-internal:
  stage: deploy
  script:
    - ./gradlew publishBundle --track internal
  only:
    - tags

release:
  stage: release
  script:
    - ./gradlew assembleRelease
    - ./gradlew bundleRelease
  artifacts:
    paths:
      - app/build/outputs/apk/release/
      - app/build/outputs/bundle/release/
  only:
    - tags
```

## 4. Release Management

### 4.1 Version Strategy

```kotlin
// Version.kt
object Version {
    const val MAJOR = 1
    const val MINOR = 0
    const val PATCH = 0

    val versionCode: Int
        get() = MAJOR * 10000 + MINOR * 100 + PATCH

    val versionName: String
        get() = "$MAJOR.$MINOR.$PATCH"

    val versionNameSuffix: String
        get() = when (BuildConfig.BUILD_TYPE) {
            "debug" -> "-DEBUG"
            "staging" -> "-STAGING"
            "release" -> ""
            else -> "-UNKNOWN"
        }

    val fullVersionName: String
        get() = versionName + versionNameSuffix
}
```

### 4.2 Release Checklist

```markdown
## Pre-Release Checklist

### Code Preparation
- [ ] All features for release are merged to main branch
- [ ] Version numbers updated (versionCode and versionName)
- [ ] CHANGELOG.md updated with release notes
- [ ] All TODO comments addressed or documented
- [ ] Deprecated code removed
- [ ] Feature flags configured correctly

### Testing
- [ ] All unit tests passing
- [ ] All instrumented tests passing on target API levels
- [ ] Manual smoke testing completed
- [ ] Performance testing completed
- [ ] Security scanning completed
- [ ] Accessibility testing completed
- [ ] Compatibility testing on various devices

### Documentation
- [ ] README.md updated
- [ ] API documentation updated
- [ ] User guide updated
- [ ] Release notes prepared in multiple languages
- [ ] Screenshots updated for store listings
- [ ] Privacy policy reviewed and updated

### Build
- [ ] ProGuard rules reviewed and tested
- [ ] Build optimizations verified (minification, resource shrinking)
- [ ] APK size within acceptable limits
- [ ] Signing configuration verified
- [ ] Version control tag created

### Distribution
- [ ] Internal testing track updated
- [ ] Beta testers notified
- [ ] Store listing metadata updated
- [ ] Content rating questionnaire completed
- [ ] Target audience and distribution settings verified

### Post-Release
- [ ] Release monitoring dashboard prepared
- [ ] Crash reporting enabled and monitored
- [ ] User feedback channels monitored
- [ ] Performance metrics tracked
- [ ] Rollback plan documented and tested
```

## 5. Distribution Channels

### 5.1 Google Play Store

```gradle
play {
    serviceAccountCredentials = file("play-service-account.json")
    defaultToAppBundles = true

    track = "internal" // internal, alpha, beta, production
    releaseStatus = "draft" // draft, completed, inProgress, halted
    userFraction = 0.1 // Staged rollout percentage

    listings {
        defaultLanguage = "en-US"

        // Store listing details
        title = "Survey Me - OSM Field Mapper"
        shortDescription = "Field survey tool for OpenStreetMap contributors"
        fullDescription = file("store/descriptions/full-description.txt")
        video = "https://youtube.com/watch?v=demo"

        // Screenshots
        phoneScreenshots = fileTree("store/screenshots/phone")
        sevenInchScreenshots = fileTree("store/screenshots/7inch")
        tenInchScreenshots = fileTree("store/screenshots/10inch")

        // Graphics
        icon = file("store/icon/icon-512.png")
        featureGraphic = file("store/feature-graphic.png")
    }
}
```

### 5.2 F-Droid Repository

```yaml
# metadata/com.surveyme.yml
Categories:
  - Navigation
License: GPL-3.0-or-later
WebSite: https://surveyme.app
SourceCode: https://github.com/surveyme/android
IssueTracker: https://github.com/surveyme/android/issues

AutoName: Survey Me
Description: |-
    Survey Me is a field survey application for OpenStreetMap contributors.

    Features:
    * Live GPS tracking
    * Points of Interest management
    * Proximity notifications
    * Offline map support
    * GPX import/export

RepoType: git
Repo: https://github.com/surveyme/android.git

Builds:
  - versionName: '1.0.0'
    versionCode: 10000
    commit: v1.0.0
    subdir: app
    gradle:
      - fdroid

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: '1.0.0'
CurrentVersionCode: 10000
```

### 5.3 Direct APK Distribution

```kotlin
class ApkDistributor {
    fun generateDownloadLinks(release: Release): Map<String, String> {
        return mapOf(
            "universal" to "${BASE_URL}/releases/${release.version}/surveyme-universal.apk",
            "arm64-v8a" to "${BASE_URL}/releases/${release.version}/surveyme-arm64-v8a.apk",
            "armeabi-v7a" to "${BASE_URL}/releases/${release.version}/surveyme-armeabi-v7a.apk",
            "x86_64" to "${BASE_URL}/releases/${release.version}/surveyme-x86_64.apk"
        )
    }

    fun generateQrCode(downloadUrl: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(downloadUrl, BarcodeFormat.QR_CODE, 512, 512)
        return bitMatrix.toBitmap()
    }

    fun verifyApkSignature(apkFile: File): Boolean {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            PackageManager.GET_SIGNATURES
        )

        return packageInfo?.signatures?.firstOrNull()?.let { signature ->
            val expectedSignature = getExpectedSignature()
            signature.toByteArray().contentEquals(expectedSignature)
        } ?: false
    }
}
```

## 6. Update Mechanisms

### 6.1 In-App Update

```kotlin
class InAppUpdateManager(
    private val activity: AppCompatActivity
) {
    private val appUpdateManager = AppUpdateManagerFactory.create(activity)
    private var updateInfo: AppUpdateInfo? = null

    fun checkForUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { info ->
            updateInfo = info

            when {
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    requestUpdate(AppUpdateType.FLEXIBLE)
                }

                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.updatePriority() >= 4 &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                    requestUpdate(AppUpdateType.IMMEDIATE)
                }

                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    popupSnackbarForCompleteUpdate()
                }
            }
        }
    }

    private fun requestUpdate(updateType: Int) {
        appUpdateManager.startUpdateFlowForResult(
            updateInfo!!,
            updateType,
            activity,
            UPDATE_REQUEST_CODE
        )
    }

    private fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            activity.findViewById(R.id.main_container),
            "An update has been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("RESTART") { appUpdateManager.completeUpdate() }
            show()
        }
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 1001
    }
}
```

### 6.2 Remote Configuration

```kotlin
class RemoteConfigManager {
    private val remoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
    }

    fun fetchAndActivate() {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    applyRemoteConfig()
                }
            }
    }

    private fun applyRemoteConfig() {
        // Feature flags
        FeatureFlags.isNewMapEngineEnabled = remoteConfig.getBoolean("new_map_engine_enabled")
        FeatureFlags.isOfflineModeEnabled = remoteConfig.getBoolean("offline_mode_enabled")

        // Configuration values
        Config.maxPoisToDisplay = remoteConfig.getLong("max_pois_display").toInt()
        Config.proximityNotificationRadius = remoteConfig.getLong("proximity_radius").toInt()

        // Force update check
        val minimumVersion = remoteConfig.getLong("minimum_app_version").toInt()
        if (BuildConfig.VERSION_CODE < minimumVersion) {
            showForceUpdateDialog()
        }
    }
}
```

## 7. Monitoring and Analytics

### 7.1 Crash Reporting

```kotlin
class CrashReportingManager {
    fun initialize(application: Application) {
        if (BuildConfig.ENABLE_CRASHLYTICS) {
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(true)
                setUserId(getUserId())
                setCustomKey("app_version", BuildConfig.VERSION_NAME)
                setCustomKey("build_type", BuildConfig.BUILD_TYPE)
                setCustomKey("distribution", BuildConfig.DISTRIBUTION_CHANNEL)
            }

            // Set up custom crash handler
            Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
                logCrashDetails(thread, exception)
                defaultHandler?.uncaughtException(thread, exception)
            }
        }
    }

    private fun logCrashDetails(thread: Thread, exception: Throwable) {
        FirebaseCrashlytics.getInstance().apply {
            log("Fatal Exception on thread: ${thread.name}")
            log("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            log("OS Version: ${Build.VERSION.RELEASE}")
            log("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            recordException(exception)
        }
    }
}
```

### 7.2 Performance Monitoring

```kotlin
class PerformanceMonitor {
    private val performanceMonitor = Firebase.performance

    fun startTrace(name: String): Trace {
        return performanceMonitor.newTrace(name).apply {
            start()
        }
    }

    fun recordNetworkRequest(
        url: String,
        method: String,
        responseCode: Int,
        requestSize: Long,
        responseSize: Long,
        startTime: Long,
        endTime: Long
    ) {
        val metric = performanceMonitor.newHttpMetric(url, method)
        metric.setHttpResponseCode(responseCode)
        metric.setRequestPayloadSize(requestSize)
        metric.setResponsePayloadSize(responseSize)
        metric.setResponseContentType("application/json")
        metric.start()
        // Simulate the timing
        Thread.sleep(endTime - startTime)
        metric.stop()
    }

    fun recordCustomMetric(name: String, value: Long) {
        performanceMonitor.newTrace("custom_metrics").apply {
            start()
            putMetric(name, value)
            stop()
        }
    }
}
```

## 8. Rollback Procedures

### 8.1 Automated Rollback

```kotlin
class RollbackManager {
    fun monitorReleaseHealth(release: Release) {
        // Monitor crash rate
        val crashRate = getCrashRate(release)
        if (crashRate > CRASH_THRESHOLD) {
            initiateRollback(release, "High crash rate: $crashRate%")
        }

        // Monitor user feedback
        val negativeReviews = getNegativeReviewRate(release)
        if (negativeReviews > REVIEW_THRESHOLD) {
            initiateRollback(release, "High negative review rate: $negativeReviews%")
        }

        // Monitor core metrics
        val coreMetrics = getCoreMetrics(release)
        if (!coreMetrics.areHealthy()) {
            initiateRollback(release, "Core metrics unhealthy")
        }
    }

    private fun initiateRollback(release: Release, reason: String) {
        // Halt current rollout
        playConsoleApi.haltRollout(release.version)

        // Promote previous stable version
        val previousVersion = getPreviousStableVersion()
        playConsoleApi.promoteVersion(previousVersion, track = "production")

        // Notify team
        notificationService.sendAlert(
            "Rollback Initiated",
            "Version ${release.version} rolled back. Reason: $reason"
        )

        // Log rollback
        analytics.logEvent("rollback_initiated", mapOf(
            "version" to release.version,
            "reason" to reason,
            "timestamp" to System.currentTimeMillis()
        ))
    }

    companion object {
        const val CRASH_THRESHOLD = 2.0 // 2% crash rate
        const val REVIEW_THRESHOLD = 30.0 // 30% negative reviews
    }
}
```

## 9. Security Considerations

### 9.1 App Signing

```gradle
android {
    signingConfigs {
        release {
            if (project.hasProperty("SIGNING_STORE_FILE")) {
                storeFile file(project.property("SIGNING_STORE_FILE"))
                storePassword project.property("SIGNING_STORE_PASSWORD")
                keyAlias project.property("SIGNING_KEY_ALIAS")
                keyPassword project.property("SIGNING_KEY_PASSWORD")

                // Enable v1 (JAR) and v2 (APK) signing
                v1SigningEnabled true
                v2SigningEnabled true

                // Enable v3 and v4 signing for Android 9+
                enableV3Signing true
                enableV4Signing true
            }
        }
    }
}
```

### 9.2 Certificate Pinning

```kotlin
class CertificatePinner {
    fun createPinnedClient(): OkHttpClient {
        val certificatePinner = CertificatePinner.Builder()
            .add("api.surveyme.app", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .add("tile.openstreetmap.org", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
            .build()

        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .build()
    }
}
```

---

*This deployment and release strategy document provides comprehensive guidance for building, testing, and distributing the Survey Me application across multiple platforms and channels.*
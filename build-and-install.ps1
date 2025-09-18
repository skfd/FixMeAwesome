# Survey Me Build and Install Script

Write-Host "Survey Me - Build and Install Script" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Set Java environment
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Find Android SDK
if (-not $env:ANDROID_HOME) {
    $possiblePaths = @(
        "$env:LOCALAPPDATA\Android\Sdk",
        "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk"
    )

    foreach ($path in $possiblePaths) {
        if (Test-Path $path) {
            $env:ANDROID_HOME = $path
            break
        }
    }

    if (-not $env:ANDROID_HOME) {
        Write-Host "ERROR: Cannot find Android SDK. Please set ANDROID_HOME environment variable." -ForegroundColor Red
        exit 1
    }
}

Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
Write-Host "ANDROID_HOME: $env:ANDROID_HOME" -ForegroundColor Green
Write-Host ""

# Function to run command and check result
function Run-Command {
    param(
        [string]$Command,
        [string]$Description
    )

    Write-Host "$Description..." -ForegroundColor Yellow
    $output = Invoke-Expression $Command 2>&1

    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed: $Description" -ForegroundColor Red
        Write-Host $output
        exit $LASTEXITCODE
    }

    return $output
}

# Clean build
Run-Command ".\gradlew.bat clean" "Cleaning previous builds"

# Increment version code automatically
Write-Host "Incrementing version code..." -ForegroundColor Yellow
$gradleFile = "app\build.gradle.kts"
$content = Get-Content $gradleFile
$versionCodeLine = $content | Where-Object { $_ -match "versionCode\s*=\s*(\d+)" }
if ($versionCodeLine) {
    $currentVersion = [int]$Matches[1]
    $newVersion = $currentVersion + 1
    $content = $content -replace "versionCode\s*=\s*\d+", "versionCode = $newVersion"
    Set-Content $gradleFile $content
    Write-Host "Version code updated: $currentVersion -> $newVersion" -ForegroundColor Green
}

# Build debug APK
Run-Command ".\gradlew.bat assembleDebug" "Building debug APK"

Write-Host ""
Write-Host "Build successful!" -ForegroundColor Green
Write-Host "APK location: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
Write-Host ""

# Check for connected devices
Write-Host "Checking for connected devices..." -ForegroundColor Yellow
$devices = adb devices | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Host "No devices connected. Please connect a device or start an emulator." -ForegroundColor Red
    exit 1
}

Write-Host "Found device(s):" -ForegroundColor Green
adb devices

# Uninstall existing app
Write-Host ""
Write-Host "Uninstalling existing app (if any)..." -ForegroundColor Yellow
adb uninstall com.surveyme.debug 2>&1 | Out-Null

# Install new APK
Write-Host "Installing APK..." -ForegroundColor Yellow
$installResult = adb install app\build\outputs\apk\debug\app-debug.apk 2>&1
if ($installResult -match "Success") {
    Write-Host "App installed successfully!" -ForegroundColor Green
} else {
    Write-Host "Installation failed:" -ForegroundColor Red
    Write-Host $installResult
    exit 1
}

# Launch app option
Write-Host ""
$launch = Read-Host "Do you want to launch the app? (Y/N)"
if ($launch -eq "Y" -or $launch -eq "y") {
    Write-Host "Launching Survey Me..." -ForegroundColor Yellow
    adb shell am start -n com.surveyme.debug/com.surveyme.presentation.MainActivity
}

Write-Host ""
Write-Host "Done!" -ForegroundColor Green
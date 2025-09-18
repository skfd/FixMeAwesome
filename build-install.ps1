# Survey Me Build and Install Script
Write-Host "Survey Me - Build and Install Script" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

# Set Java environment for this session
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "JAVA_HOME set to: $env:JAVA_HOME" -ForegroundColor Green

# Clean build
Write-Host "Cleaning previous builds..." -ForegroundColor Yellow
& .\gradlew.bat clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "Clean failed!" -ForegroundColor Red
    exit 1
}

# Build debug APK
Write-Host "Building debug APK..." -ForegroundColor Yellow
& .\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "Build successful!" -ForegroundColor Green
Write-Host ""

# Check for connected devices
Write-Host "Checking for connected devices..." -ForegroundColor Yellow
$devices = & adb devices 2>$null | Select-String "device$"
if ($null -eq $devices) {
    Write-Host "No devices connected. Please connect a device or start an emulator." -ForegroundColor Red
    exit 1
}

# Uninstall existing app
Write-Host "Uninstalling existing app (if any)..." -ForegroundColor Yellow
& adb uninstall com.surveyme.debug 2>$null | Out-Null

# Install new APK
Write-Host "Installing APK..." -ForegroundColor Yellow
$result = & adb install app\build\outputs\apk\debug\app-debug.apk 2>&1
if ($result -match "Success") {
    Write-Host "App installed successfully!" -ForegroundColor Green

    # Launch app
    Write-Host "Launching Survey Me..." -ForegroundColor Yellow
    & adb shell am start -n com.surveyme.debug/com.surveyme.presentation.MainActivity
    Write-Host "App launched!" -ForegroundColor Green
} else {
    Write-Host "Installation failed!" -ForegroundColor Red
    Write-Host $result
    exit 1
}

Write-Host ""
Write-Host "Done! Test the following:" -ForegroundColor Cyan
Write-Host "1. Background location permission request on Map tab"
Write-Host "2. No crashes when switching between tabs"
Write-Host "3. Your location appears on the map"
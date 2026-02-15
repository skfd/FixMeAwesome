# Fast Restart Script
# Builds and restarts the app without confirmation prompts
# Usage: .\fast-restart.ps1

Write-Host "Fast Restart..." -ForegroundColor Cyan

# Set Java environment
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Find Android SDK
if (-not $env:ANDROID_HOME) {
    if (Test-Path "$env:LOCALAPPDATA\Android\Sdk") {
        $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
    }
    else {
        Write-Host "ERROR: ANDROID_HOME not set." -ForegroundColor Red
        exit 1
    }
}

# Build (incremental, no clean)
Write-Host "Building (incremental)..." -ForegroundColor Yellow
# Run directly to show output
& .\gradlew.bat assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build Failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

# Install
Write-Host "Installing..." -ForegroundColor Yellow
# Try to install over existing first (preserve data)
# -r: replace existing application
# -t: allow test packages
$installOutput = & adb install -r -t app\build\outputs\apk\debug\app-debug.apk 2>&1 | Out-String

if ($installOutput -match "Success") {
    Write-Host "Installed!" -ForegroundColor Green
}
else {
    Write-Host "Update failed, trying clean install..." -ForegroundColor Yellow
    & adb uninstall com.surveyme.debug | Out-Null
    $installOutput = & adb install -t app\build\outputs\apk\debug\app-debug.apk 2>&1 | Out-String
    
    if ($installOutput -match "Success") {
        Write-Host "Clean Install Successful!" -ForegroundColor Green
    }
    else {
        Write-Host "Install Failed!" -ForegroundColor Red
        Write-Host $installOutput
        exit 1
    }
}

# Launch
Write-Host "Launching..." -ForegroundColor Yellow
adb shell am start -n com.surveyme.debug/com.surveyme.presentation.MainActivity

Write-Host "Done!" -ForegroundColor Green

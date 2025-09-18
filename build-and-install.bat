@echo off
echo Setting up environment...

:: Set JAVA_HOME to Android Studio's bundled JDK
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

:: Check if ANDROID_HOME is set, if not try common locations
if "%ANDROID_HOME%"=="" (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
    ) else if exist "C:\Users\%USERNAME%\AppData\Local\Android\Sdk" (
        set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
    ) else (
        echo ERROR: Cannot find Android SDK. Please set ANDROID_HOME environment variable.
        exit /b 1
    )
)

echo JAVA_HOME: %JAVA_HOME%
echo ANDROID_HOME: %ANDROID_HOME%
echo.

:: Clean previous builds
echo Cleaning previous builds...
call gradlew.bat clean
if %ERRORLEVEL% NEQ 0 (
    echo Build failed during clean!
    exit /b %ERRORLEVEL%
)

:: Build debug APK
echo Building debug APK...
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b %ERRORLEVEL%
)

echo.
echo Build successful! APK created at: app\build\outputs\apk\debug\app-debug.apk
echo.

:: Check if device is connected
echo Checking for connected devices...
adb devices | findstr /r "device$" >nul
if %ERRORLEVEL% NEQ 0 (
    echo No devices connected. Please connect a device or start an emulator.
    exit /b 1
)

:: Uninstall existing app (ignore errors if not installed)
echo Uninstalling existing app (if any)...
adb uninstall com.surveyme.debug >nul 2>&1

:: Install the new APK
echo Installing APK...
adb install app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% NEQ 0 (
    echo Installation failed!
    exit /b %ERRORLEVEL%
)

echo.
echo Success! App installed and ready to launch.
echo.

:: Optional: Launch the app
set /p LAUNCH=Do you want to launch the app? (Y/N):
if /i "%LAUNCH%"=="Y" (
    echo Launching Survey Me...
    adb shell am start -n com.surveyme.debug/com.surveyme.presentation.MainActivity
)

echo Done!
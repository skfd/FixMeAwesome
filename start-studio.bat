@echo off
echo Starting Android Studio...
if exist "C:\Program Files\Android\Android Studio\bin\studio64.exe" (
    start "" "C:\Program Files\Android\Android Studio\bin\studio64.exe" .
) else (
    echo Android Studio not found at default location.
    echo Please edit this script to set the correct path.
    pause
)

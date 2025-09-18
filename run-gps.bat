@echo off
echo GPS Simulator Options:
echo ----------------------
echo 1. Auto-walk North (Simple)
echo 2. WSAD Controller (Advanced)
echo 3. Exit
echo.
set /p choice=Choose option (1-3):

if "%choice%"=="1" (
    echo Starting auto-walk simulator...
    powershell -ExecutionPolicy Bypass -File gps-auto-walk.ps1
) else if "%choice%"=="2" (
    echo Starting WSAD controller...
    echo.
    echo NOTE: After pressing any key to start,
    echo Hold W/A/S/D keys to move in that direction
    echo Press Q to quit
    echo.
    powershell -ExecutionPolicy Bypass -File gps-sim.ps1
) else (
    echo Goodbye!
)
pause
# Automation Scripts

This repository includes several PowerShell scripts to streamline development, testing, and GPS simulation.

To run these scripts, use PowerShell:
```powershell
./script-name.ps1
```

## Build & Install

| Script | Description |
| :--- | :--- |
| **`build-and-install.ps1`** | **Recommended.** Builds the Debug APK, **automatically increments the version code**, uninstalls the old app, installs the new one, and launches it. |
| **`build-install.ps1`** | Similar to the above but does **not** increment the version code. Useful for quick iterations where versioning is not critical. |
| **`run-tests.ps1`** | Runs unit tests (`testDebugUnitTest`) and saves the output to `test_output.txt`. |
| **`copy-to-dropbox.ps1`** | Copies the generated APK to a Dropbox folder (requires configuration of the destination path). |

## GPS Simulation (Android Emulator)

These scripts allow you to simulate location movement on the Android Emulator. Ensure your emulator is running before executing these.

| Script | Description |
| :--- | :--- |
| **`gps-simulator.ps1`** | **Interactive Simulator.** Allows you to control the GPS location using **WASD** keys. Includes speed controls and clipboard support. **Default Location:** Toronto (Dundas Square). |
| **`gps-toronto-walker.ps1`** | **Toronto Station Walker.** Simulations walking between Bike Share stations in Toronto. Use **WASD** to move, **N** to walk to the next station, or **T** to teleport. |
| **`gps-toronto-tour.ps1`** | **Automated Tour.** Automatically moves the GPS location through a predefined list of Toronto Bike Share stations, pausing at each one. |

## Debugging

| Script | Description |
| :--- | :--- |
| **`show-logs.ps1`** | **Log Viewer.** Streams `adb logcat` output filtered for the app package (`com.surveyme`), crashes (`AndroidRuntime`), and system errors. Press `Ctrl+C` to stop. |

## Usage Notes

*   **Execution Policy:** You may need to set your PowerShell execution policy to run scripts: `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`
*   **Emulator:** These scripts require an active Android Emulator and `adb` in your system PATH.

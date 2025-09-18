# GPS Walker - Continues from Current Location
# Moves at 10 km/h (2.78 m/s) with WASD controls

Write-Host "GPS Walker - Continuous Movement" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""

# Check if emulator is running
$devices = adb devices | Select-String "emulator"
if (-not $devices) {
    Write-Host "No emulator detected! Please start the Android emulator first." -ForegroundColor Red
    exit 1
}

Write-Host "Emulator detected!" -ForegroundColor Green
Write-Host ""

# Get current location from emulator
Write-Host "Getting current GPS location..." -ForegroundColor Yellow
$locationOutput = adb shell dumpsys location | Select-String "last location" -Context 0,1

# Try to parse current location (this is approximate as getting exact GPS from emulator is tricky)
# For now, we'll ask the user for their current location
Write-Host ""
Write-Host "Enter your current location (or press Enter to use default 37.7749, -122.4194):" -ForegroundColor Yellow
Write-Host -NoNewline "Current GPS> " -ForegroundColor Green
$currentInput = Read-Host

if ([string]::IsNullOrWhiteSpace($currentInput)) {
    # Default to San Francisco downtown
    $lat = 37.7749
    $lon = -122.4194
    Write-Host "Using default location: $lat, $lon" -ForegroundColor Gray
} else {
    $coords = $currentInput -replace '\s+', '' -split ','
    if ($coords.Count -eq 2) {
        try {
            $lat = [double]$coords[0]
            $lon = [double]$coords[1]
            Write-Host "Starting from: $lat, $lon" -ForegroundColor Green
        } catch {
            Write-Host "Invalid format, using default location" -ForegroundColor Yellow
            $lat = 37.7749
            $lon = -122.4194
        }
    } else {
        Write-Host "Invalid format, using default location" -ForegroundColor Yellow
        $lat = 37.7749
        $lon = -122.4194
    }
}

Write-Host ""
Write-Host "Controls:" -ForegroundColor Yellow
Write-Host "  W = North" -ForegroundColor Gray
Write-Host "  S = South" -ForegroundColor Gray
Write-Host "  A = West" -ForegroundColor Gray
Write-Host "  D = East" -ForegroundColor Gray
Write-Host "  Q = Quit" -ForegroundColor Gray
Write-Host ""
Write-Host "Speed: ~10 km/h (moves every second)" -ForegroundColor Gray
Write-Host ""

# Movement speed: 10 km/h = 2.78 m/s
# At equator: 1 degree latitude = 111,000 meters
# So 2.78 meters = 0.000025 degrees
$moveDist = 0.000025

# Initial position set
adb emu geo fix $lon $lat
Write-Host "GPS Position: $lat, $lon" -ForegroundColor Green

while ($true) {
    # Check for key press (non-blocking)
    if ([Console]::KeyAvailable) {
        $key = [Console]::ReadKey($true)

        switch ($key.KeyChar) {
            'w' {
                $lat += $moveDist
                Write-Host "Moving North..." -ForegroundColor Cyan -NoNewline
            }
            's' {
                $lat -= $moveDist
                Write-Host "Moving South..." -ForegroundColor Cyan -NoNewline
            }
            'a' {
                $lon -= $moveDist
                Write-Host "Moving West..." -ForegroundColor Cyan -NoNewline
            }
            'd' {
                $lon += $moveDist
                Write-Host "Moving East..." -ForegroundColor Cyan -NoNewline
            }
            'q' {
                Write-Host "`nGoodbye! Final position: $lat, $lon" -ForegroundColor Yellow
                exit
            }
            default {
                continue
            }
        }

        # Update GPS position
        adb emu geo fix $lon $lat
        Write-Host " Position: $lat, $lon" -ForegroundColor Green
    }

    # Small delay to prevent CPU overload
    Start-Sleep -Milliseconds 100
}
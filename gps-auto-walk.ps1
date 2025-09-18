# Automatic GPS Walker - Walks north continuously
Write-Host "GPS Auto Walker - Walking North" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan

# Starting position (San Francisco)
$latitude = 37.7749
$longitude = -122.4194
$speedKmh = 10

Write-Host "Starting at: $latitude, $longitude" -ForegroundColor Green
Write-Host "Speed: $speedKmh km/h" -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop" -ForegroundColor Red
Write-Host ""

# Send initial position
adb emu geo fix $longitude $latitude 2>$null

Write-Host "Walking north..." -ForegroundColor Green

# Walk continuously
$updateInterval = 0.5  # seconds
$metersPerSecond = $speedKmh * 1000 / 3600

try {
    while ($true) {
        # Calculate movement
        $metersPerUpdate = $metersPerSecond * $updateInterval
        $latChange = $metersPerUpdate / 111000.0

        # Update position
        $latitude += $latChange

        # Send to emulator
        adb emu geo fix $longitude $latitude 2>$null

        # Display current position
        Write-Host -NoNewline "`rCurrent: $([Math]::Round($latitude, 6)), $([Math]::Round($longitude, 6))  "

        Start-Sleep -Milliseconds 500
    }
} catch {
    Write-Host ""
    Write-Host "Stopped!" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Final position: $([Math]::Round($latitude, 6)), $([Math]::Round($longitude, 6))" -ForegroundColor Green
# Simple GPS Walker for Android Emulator
# Press W to walk north continuously

Write-Host "GPS Walker - Simple North Walking Simulator" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""

# Starting position (San Francisco)
$latitude = 37.7749
$longitude = -122.4194
$speedKmh = 10

Write-Host "Starting position: $latitude, $longitude" -ForegroundColor Green
Write-Host "Speed: $speedKmh km/h" -ForegroundColor Yellow
Write-Host ""
Write-Host "Commands:" -ForegroundColor White
Write-Host "  Enter - Walk north for 5 seconds"
Write-Host "  Q + Enter - Quit"
Write-Host ""

# Send initial position
$cmd = "adb emu geo fix $longitude $latitude"
Invoke-Expression $cmd | Out-Null

while ($true) {
    Write-Host ""
    Write-Host -NoNewline "Press Enter to walk north (Q to quit): "
    $input = Read-Host

    if ($input -eq 'Q' -or $input -eq 'q') {
        break
    }

    Write-Host "Walking north for 5 seconds..." -ForegroundColor Green

    # Walk for 5 seconds
    $endTime = (Get-Date).AddSeconds(5)
    $updateInterval = 0.5  # Update every 500ms

    while ((Get-Date) -lt $endTime) {
        # Calculate distance traveled
        # 10 km/h = 2.78 m/s
        $metersPerUpdate = ($speedKmh * 1000 / 3600) * $updateInterval

        # Convert meters to degrees latitude
        # 1 degree latitude = ~111km
        $latChange = $metersPerUpdate / 111000.0

        # Update position
        $latitude += $latChange

        # Send to emulator
        $cmd = "adb emu geo fix $longitude $latitude"
        Invoke-Expression $cmd 2>$null | Out-Null

        Write-Host "." -NoNewline -ForegroundColor Yellow

        Start-Sleep -Milliseconds 500
    }

    Write-Host ""
    Write-Host "Stopped at: $([Math]::Round($latitude, 6)), $([Math]::Round($longitude, 6))" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "GPS Walker stopped" -ForegroundColor Yellow
Write-Host "Final position: $([Math]::Round($latitude, 6)), $([Math]::Round($longitude, 6))" -ForegroundColor Green
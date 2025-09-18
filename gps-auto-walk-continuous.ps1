# GPS Auto Walker - Walks from Current Location
# Automatically moves north at walking speed

param(
    [string]$StartLocation = "",
    [string]$Direction = "north"
)

Write-Host "GPS Auto Walker - Continuous Movement" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Check if emulator is running
$devices = adb devices | Select-String "emulator"
if (-not $devices) {
    Write-Host "No emulator detected! Please start the Android emulator first." -ForegroundColor Red
    exit 1
}

Write-Host "Emulator detected!" -ForegroundColor Green
Write-Host ""

# Get starting location
if ([string]::IsNullOrWhiteSpace($StartLocation)) {
    Write-Host "Enter starting location (or press Enter to continue from current):" -ForegroundColor Yellow
    Write-Host "Examples:" -ForegroundColor Gray
    Write-Host "  37.7956, -122.3933  (Ferry Building)" -ForegroundColor Gray
    Write-Host "  37.8024, -122.4058  (Coit Tower)" -ForegroundColor Gray
    Write-Host ""
    Write-Host -NoNewline "Start GPS> " -ForegroundColor Green
    $input = Read-Host

    if ([string]::IsNullOrWhiteSpace($input)) {
        # Try to continue from last known position (use Ferry Building as fallback)
        Write-Host "Continuing from last position (or Ferry Building if first run)" -ForegroundColor Yellow
        $lat = 37.7956
        $lon = -122.3933
    } else {
        $coords = $input -replace '\s+', '' -split ','
        if ($coords.Count -eq 2) {
            try {
                $lat = [double]$coords[0]
                $lon = [double]$coords[1]
                Write-Host "Starting from: $lat, $lon" -ForegroundColor Green
            } catch {
                Write-Host "Invalid format, using Ferry Building location" -ForegroundColor Yellow
                $lat = 37.7956
                $lon = -122.3933
            }
        }
    }
} else {
    $coords = $StartLocation -replace '\s+', '' -split ','
    $lat = [double]$coords[0]
    $lon = [double]$coords[1]
    Write-Host "Starting from: $lat, $lon" -ForegroundColor Green
}

Write-Host ""
Write-Host "Walking $Direction at ~5 km/h (updates every second)" -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

# Movement speed: 5 km/h = 1.39 m/s (walking speed)
# At equator: 1 degree latitude = 111,000 meters
# So 1.39 meters = 0.0000125 degrees
$moveDist = 0.0000125

# Set initial position
adb emu geo fix $lon $lat
Write-Host "Starting walk from: $lat, $lon" -ForegroundColor Green
Write-Host ""

$counter = 0
while ($true) {
    # Move based on direction
    switch ($Direction.ToLower()) {
        "north" { $lat += $moveDist }
        "south" { $lat -= $moveDist }
        "east"  { $lon += $moveDist }
        "west"  { $lon -= $moveDist }
        default { $lat += $moveDist }  # Default to north
    }

    # Update GPS position
    adb emu geo fix $lon $lat

    $counter++
    if ($counter % 5 -eq 0) {
        # Show position every 5 seconds
        Write-Host "Position: $lat, $lon (walking $Direction)" -ForegroundColor Green

        # Check proximity to POIs
        $pois = @(
            @{name="Ferry Building"; lat=37.7956; lon=-122.3933; radius=100},
            @{name="Golden Gate Bridge"; lat=37.8199; lon=-122.4783; radius=200},
            @{name="Coit Tower"; lat=37.8024; lon=-122.4058; radius=150},
            @{name="Union Square"; lat=37.7880; lon=-122.4075; radius=100},
            @{name="Embarcadero Station"; lat=37.7929; lon=-122.3972; radius=50}
        )

        foreach ($poi in $pois) {
            $dlat = $lat - $poi.lat
            $dlon = $lon - $poi.lon
            $distDeg = [Math]::Sqrt($dlat*$dlat + $dlon*$dlon)
            $distMeters = [Math]::Round($distDeg * 111000)

            if ($distMeters -le $poi.radius) {
                Write-Host "  >>> WITHIN RANGE of $($poi.name)! (~$distMeters m)" -ForegroundColor Yellow
            }
        }
    }

    # Wait 1 second before next move
    Start-Sleep -Seconds 1
}
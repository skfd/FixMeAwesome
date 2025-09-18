# GPS Location Setter for Android Emulator
# Usage: .\gps-set.ps1
# Enter coordinates like: 37.7956, -122.3933

Write-Host "GPS Location Setter" -ForegroundColor Cyan
Write-Host "===================" -ForegroundColor Cyan
Write-Host ""

# Check if emulator is running
$devices = adb devices | Select-String "emulator"
if (-not $devices) {
    Write-Host "No emulator detected! Please start the Android emulator first." -ForegroundColor Red
    exit 1
}

Write-Host "Emulator detected!" -ForegroundColor Green
Write-Host ""
Write-Host "Enter coordinates in format: latitude, longitude" -ForegroundColor Yellow
Write-Host "Examples:" -ForegroundColor Gray
Write-Host "  Ferry Building:     37.7956, -122.3933" -ForegroundColor Gray
Write-Host "  Golden Gate Bridge: 37.8199, -122.4783" -ForegroundColor Gray
Write-Host "  Coit Tower:        37.8024, -122.4058" -ForegroundColor Gray
Write-Host "  Union Square:      37.7880, -122.4075" -ForegroundColor Gray
Write-Host "  Embarcadero:       37.7929, -122.3972" -ForegroundColor Gray
Write-Host ""
Write-Host "Type 'q' to quit" -ForegroundColor Gray
Write-Host ""

while ($true) {
    Write-Host -NoNewline "GPS> " -ForegroundColor Green
    $input = Read-Host

    if ($input -eq 'q' -or $input -eq 'quit' -or $input -eq 'exit') {
        Write-Host "Goodbye!" -ForegroundColor Cyan
        break
    }

    # Parse the input - handle various formats
    $coords = $input -replace '\s+', '' -split ','

    if ($coords.Count -eq 2) {
        try {
            $lat = [double]$coords[0]
            $lon = [double]$coords[1]

            # Validate coordinates
            if ($lat -ge -90 -and $lat -le 90 -and $lon -ge -180 -and $lon -le 180) {
                # Send to emulator
                $cmd = "geo fix $lon $lat"
                echo $cmd | adb emu geo fix $lon $lat

                Write-Host "Location set to: $lat, $lon" -ForegroundColor Green

                # Show distance to nearest POI for reference
                $pois = @(
                    @{name="Ferry Building"; lat=37.7956; lon=-122.3933; radius=100},
                    @{name="Golden Gate Bridge"; lat=37.8199; lon=-122.4783; radius=200},
                    @{name="Coit Tower"; lat=37.8024; lon=-122.4058; radius=150},
                    @{name="Union Square"; lat=37.7880; lon=-122.4075; radius=100},
                    @{name="Embarcadero Station"; lat=37.7929; lon=-122.3972; radius=50}
                )

                foreach ($poi in $pois) {
                    # Simple distance calculation (approximate)
                    $dlat = $lat - $poi.lat
                    $dlon = $lon - $poi.lon
                    $distDeg = [Math]::Sqrt($dlat*$dlat + $dlon*$dlon)
                    $distMeters = [Math]::Round($distDeg * 111000)  # Very rough approximation

                    if ($distMeters -le $poi.radius) {
                        Write-Host "  WITHIN range of $($poi.name) (~$distMeters m, radius: $($poi.radius)m)" -ForegroundColor Yellow
                    } elseif ($distMeters -le $poi.radius * 2) {
                        Write-Host "  Near $($poi.name) (~$distMeters m away, radius: $($poi.radius)m)" -ForegroundColor Gray
                    }
                }
            } else {
                Write-Host "Invalid coordinates! Latitude must be -90 to 90, Longitude -180 to 180" -ForegroundColor Red
            }
        } catch {
            Write-Host "Invalid format! Please enter: latitude, longitude" -ForegroundColor Red
        }
    } else {
        Write-Host "Invalid format! Please enter: latitude, longitude" -ForegroundColor Red
    }

    Write-Host ""
}
# Toronto GPS Walker - Walk around downtown Toronto bike share stations
# Starts at Dundas Square and walks through downtown

$startLat = 43.65615
$startLon = -79.38143
$currentLat = $startLat
$currentLon = $startLon

# Key Toronto bike stations to visit
$stations = @(
    @{name="Dundas Square"; lat=43.65615; lon=-79.38143},
    @{name="Union Station"; lat=43.6457; lon=-79.38017},
    @{name="St. George Station"; lat=43.66719; lon=-79.39956},
    @{name="Queens Park"; lat=43.6681; lon=-79.39374},
    @{name="Wellesley Station"; lat=43.66499; lon=-79.38351},
    @{name="College & Huron"; lat=43.65803; lon=-79.39817},
    @{name="Temperance St"; lat=43.65085; lon=-79.37988},
    @{name="Bay & Queens Quay"; lat=43.64085; lon=-79.37675},
    @{name="King & Spadina"; lat=43.64518; lon=-79.39509},
    @{name="Metro Hall"; lat=43.64582; lon=-79.38845}
)

$currentStationIndex = 0
$targetStation = $stations[0]
$walkSpeed = 0.00003  # Walking speed in degrees

Write-Host "Toronto GPS Walker - WASD Controls" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host "W/S = North/South, A/D = West/East"
Write-Host "Q = Quit, N = Next station, T = Teleport to station"
Write-Host "Current location: Dundas Square"
Write-Host ""

# Set initial position
adb emu geo fix $startLon $startLat
Write-Host "Set GPS to: $($targetStation.name) ($startLat, $startLon)" -ForegroundColor Green

while ($true) {
    if ([Console]::KeyAvailable) {
        $key = [Console]::ReadKey($true).Key

        $oldLat = $currentLat
        $oldLon = $currentLon

        switch ($key) {
            "W" { $currentLat += $walkSpeed * 2 }  # North (faster for testing)
            "S" { $currentLat -= $walkSpeed * 2 }  # South
            "A" { $currentLon -= $walkSpeed * 2 }  # West
            "D" { $currentLon += $walkSpeed * 2 }  # East
            "N" {
                # Move to next station
                $currentStationIndex = ($currentStationIndex + 1) % $stations.Count
                $targetStation = $stations[$currentStationIndex]
                Write-Host "`nNavigating to: $($targetStation.name)" -ForegroundColor Yellow
                continue
            }
            "T" {
                # Teleport to current target station
                $currentLat = $targetStation.lat
                $currentLon = $targetStation.lon
                adb emu geo fix $currentLon $currentLat
                Write-Host "`nTeleported to: $($targetStation.name) ($currentLat, $currentLon)" -ForegroundColor Magenta
                continue
            }
            "Q" {
                Write-Host "`nStopping Toronto GPS Walker" -ForegroundColor Red
                break
            }
            default { continue }
        }

        # Update GPS position
        adb emu geo fix $currentLon $currentLat

        # Check proximity to stations
        foreach ($station in $stations) {
            $distance = [Math]::Sqrt([Math]::Pow($currentLat - $station.lat, 2) + [Math]::Pow($currentLon - $station.lon, 2))
            if ($distance -lt 0.0008) {  # About 75 meters
                Write-Host "Near: $($station.name) (within notification range)" -ForegroundColor Green
            }
        }

        Write-Host "GPS: $currentLat, $currentLon" -ForegroundColor Gray
    }

    Start-Sleep -Milliseconds 50
}
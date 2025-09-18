# Toronto Bike Share Station Tour
# Automatically walks between bike share stations

param(
    [int]$DelaySeconds = 5  # Delay at each station
)

Write-Host "Toronto Bike Share Station Tour" -ForegroundColor Cyan
Write-Host "===============================" -ForegroundColor Cyan
Write-Host "This script will walk through Toronto bike stations"
Write-Host "Press Ctrl+C to stop at any time"
Write-Host ""

# Toronto bike share stations route
$stations = @(
    @{name="Dundas Square"; lat=43.65615; lon=-79.38143},
    @{name="Temperance St (60 docks)"; lat=43.65085; lon=-79.37988},
    @{name="Union Station (47 docks)"; lat=43.6457; lon=-79.38017},
    @{name="Bay & Queens Quay Ferry (57 docks)"; lat=43.64085; lon=-79.37675},
    @{name="Distillery District"; lat=43.65039; lon=-79.35639},
    @{name="Riverdale Park"; lat=43.67117; lon=-79.35461},
    @{name="Wellesley Station (26 docks)"; lat=43.66499; lon=-79.38351},
    @{name="St. George Station"; lat=43.66719; lon=-79.39956},
    @{name="Queens Park"; lat=43.6681; lon=-79.39374},
    @{name="College & Huron"; lat=43.65803; lon=-79.39817},
    @{name="King & Spadina"; lat=43.64518; lon=-79.39509},
    @{name="Queen & Ossington"; lat=43.64393; lon=-79.41974},
    @{name="High Park"; lat=43.64558; lon=-79.46562},
    @{name="Metro Hall Plaza"; lat=43.64582; lon=-79.38845}
)

function Move-ToLocation {
    param($fromLat, $fromLon, $toLat, $toLon, $steps = 20)

    for ($i = 1; $i -le $steps; $i++) {
        $progress = $i / $steps
        $lat = $fromLat + ($toLat - $fromLat) * $progress
        $lon = $fromLon + ($toLon - $fromLon) * $progress

        adb emu geo fix $lon $lat
        Write-Progress -Activity "Walking" -Status "Step $i of $steps" -PercentComplete ($progress * 100)
        Start-Sleep -Milliseconds 500
    }
    Write-Progress -Activity "Walking" -Completed
}

# Main tour loop
$currentIndex = 0
while ($true) {
    $current = $stations[$currentIndex]
    $nextIndex = ($currentIndex + 1) % $stations.Count
    $next = $stations[$nextIndex]

    Write-Host "`n📍 At: $($current.name)" -ForegroundColor Green
    Write-Host "   Coordinates: $($current.lat), $($current.lon)" -ForegroundColor Gray

    # Set position at station
    adb emu geo fix $($current.lon) $($current.lat)

    Write-Host "   Waiting $DelaySeconds seconds for proximity notification..." -ForegroundColor Yellow
    Start-Sleep -Seconds $DelaySeconds

    Write-Host "   Walking to: $($next.name)" -ForegroundColor Cyan

    # Walk to next station
    Move-ToLocation -fromLat $current.lat -fromLon $current.lon `
                    -toLat $next.lat -toLon $next.lon -steps 30

    $currentIndex = $nextIndex

    # Check for exit
    if ([Console]::KeyAvailable) {
        $key = [Console]::ReadKey($true).Key
        if ($key -eq "Q") {
            Write-Host "`nTour ended!" -ForegroundColor Red
            break
        }
    }
}

Write-Host "`nTour complete! Press any key to exit..."
$null = [Console]::ReadKey($true)
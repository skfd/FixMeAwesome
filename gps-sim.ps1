# GPS Movement Simulator with WSAD Controls
# Fixed version without syntax errors

Write-Host "`nGPS Movement Simulator" -ForegroundColor Cyan
Write-Host "======================" -ForegroundColor Cyan
Write-Host ""

# Starting position (San Francisco)
$global:latitude = 37.7749
$global:longitude = -122.4194
$global:speedKmh = 10

# Add keyboard detection type
Add-Type @"
    using System;
    using System.Runtime.InteropServices;
    public class Win32 {
        [DllImport("user32.dll")]
        public static extern short GetAsyncKeyState(int vKey);
    }
"@

function Test-Key {
    param($VirtualKeyCode)
    return [Win32]::GetAsyncKeyState($VirtualKeyCode) -band 0x8000
}

function Update-Position {
    param(
        [bool]$moveNorth,
        [bool]$moveSouth,
        [bool]$moveEast,
        [bool]$moveWest,
        [double]$deltaTime
    )

    if (-not ($moveNorth -or $moveSouth -or $moveEast -or $moveWest)) {
        return
    }

    # Calculate movement in meters
    $metersPerSecond = $global:speedKmh * 1000 / 3600
    $metersThisFrame = $metersPerSecond * $deltaTime

    # Calculate direction
    $moveY = 0
    $moveX = 0

    if ($moveNorth) { $moveY = 1 }
    if ($moveSouth) { $moveY = -1 }
    if ($moveEast) { $moveX = 1 }
    if ($moveWest) { $moveX = -1 }

    # Normalize for diagonal movement
    if ($moveX -ne 0 -and $moveY -ne 0) {
        $magnitude = [Math]::Sqrt($moveX * $moveX + $moveY * $moveY)
        $moveX = $moveX / $magnitude
        $moveY = $moveY / $magnitude
    }

    # Apply movement
    $latMeters = $moveY * $metersThisFrame
    $lonMeters = $moveX * $metersThisFrame

    # Convert to degrees
    $latChange = $latMeters / 111000.0
    $lonChange = $lonMeters / (111000.0 * [Math]::Cos($global:latitude * [Math]::PI / 180))

    # Update position
    $global:latitude += $latChange
    $global:longitude += $lonChange

    # Send to emulator
    $cmd = "adb emu geo fix $($global:longitude) $($global:latitude)"
    $null = Invoke-Expression $cmd 2>$null
}

function Show-Status {
    param(
        [string]$movement = "Stopped"
    )

    Clear-Host
    Write-Host "`nGPS Movement Simulator" -ForegroundColor Cyan
    Write-Host "======================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Position: $([Math]::Round($global:latitude, 6)), $([Math]::Round($global:longitude, 6))" -ForegroundColor Green
    Write-Host "Speed: $($global:speedKmh) km/h" -ForegroundColor Yellow
    Write-Host "Movement: $movement" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Controls:" -ForegroundColor White
    Write-Host "  W - North  |  S - South"
    Write-Host "  A - West   |  D - East"
    Write-Host "  + Speed up |  - Slow down"
    Write-Host "  Q - Quit"
    Write-Host ""
    Write-Host "Hold keys for continuous movement" -ForegroundColor DarkGray
}

# Key codes
$KEY_W = 0x57
$KEY_S = 0x53
$KEY_A = 0x41
$KEY_D = 0x44
$KEY_Q = 0x51
$KEY_PLUS = 0xBB
$KEY_MINUS = 0xBD

# Initialize
Write-Host "Initializing..." -ForegroundColor Yellow
$null = adb emu geo fix $global:longitude $global:latitude 2>$null
Write-Host "GPS position set!" -ForegroundColor Green
Write-Host "Press any key to start..."
$null = $host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

# Main loop
$lastTime = [DateTime]::Now
$lastDisplay = [DateTime]::Now
$running = $true

Show-Status

while ($running) {
    # Calculate delta time
    $now = [DateTime]::Now
    $deltaTime = ($now - $lastTime).TotalSeconds
    $lastTime = $now

    # Check keys
    $north = Test-Key $KEY_W
    $south = Test-Key $KEY_S
    $west = Test-Key $KEY_A
    $east = Test-Key $KEY_D
    $quit = Test-Key $KEY_Q

    # Quit check
    if ($quit) {
        $running = $false
        break
    }

    # Speed controls
    if (Test-Key $KEY_PLUS) {
        $global:speedKmh = [Math]::Min(50, $global:speedKmh + 1)
    }
    if (Test-Key $KEY_MINUS) {
        $global:speedKmh = [Math]::Max(1, $global:speedKmh - 1)
    }

    # Update position
    Update-Position -moveNorth $north -moveSouth $south -moveEast $east -moveWest $west -deltaTime $deltaTime

    # Update display every 250ms
    if (($now - $lastDisplay).TotalMilliseconds -ge 250) {
        # Build movement string
        $moveStr = ""
        if ($north) { $moveStr += "N" }
        if ($south) { $moveStr += "S" }
        if ($west) { $moveStr += "W" }
        if ($east) { $moveStr += "E" }
        if ($moveStr -eq "") { $moveStr = "Stopped" }

        Show-Status -movement $moveStr
        $lastDisplay = $now
    }

    # Small delay to prevent CPU overload
    Start-Sleep -Milliseconds 10
}

Write-Host "`nSimulator stopped!" -ForegroundColor Yellow
Write-Host "Final position: $([Math]::Round($global:latitude, 6)), $([Math]::Round($global:longitude, 6))" -ForegroundColor Green
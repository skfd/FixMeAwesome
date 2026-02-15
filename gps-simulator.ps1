# GPS Movement Simulator for Android Emulator
# Uses WSAD keys to move GPS location at 10 km/h

Add-Type @"
    using System;
    using System.Runtime.InteropServices;
    public class KeyReader {
        [DllImport("user32.dll")]
        public static extern short GetAsyncKeyState(int vKey);
    }
"@

class GPSSimulator {
    [double]$Latitude = 43.65615   # Toronto (Dundas Square)
    [double]$Longitude = -79.38143
    [double]$SpeedKmh = 10        # Speed in km/h
    [double]$UpdateInterval = 0.2  # Update every 200ms

    # Virtual key codes for WSAD
    $VK_W = 0x57
    $VK_A = 0x41
    $VK_S = 0x53
    $VK_D = 0x44
    $VK_Q = 0x51
    $VK_PLUS = 0xBB
    $VK_MINUS = 0xBD
    $VK_SPACE = 0x20

    [void] SendGPSLocation() {
        # Send GPS coordinates to emulator
        $cmd = "adb emu geo fix $($this.Longitude) $($this.Latitude)"
        Invoke-Expression $cmd 2>$null
    }

    [void] UpdatePosition([bool]$north, [bool]$south, [bool]$east, [bool]$west) {
        if (-not ($north -or $south -or $east -or $west)) {
            return
        }

        # Calculate meters per update
        $metersPerUpdate = ($this.SpeedKmh * 1000 / 3600) * $this.UpdateInterval

        # Calculate movement
        $moveNorth = 0
        $moveEast = 0

        if ($north) { $moveNorth += $metersPerUpdate }
        if ($south) { $moveNorth -= $metersPerUpdate }
        if ($east) { $moveEast += $metersPerUpdate }
        if ($west) { $moveEast -= $metersPerUpdate }

        # Normalize diagonal movement
        if ($moveNorth -ne 0 -and $moveEast -ne 0) {
            $magnitude = [Math]::Sqrt($moveNorth * $moveNorth + $moveEast * $moveEast)
            $moveNorth = ($moveNorth / $magnitude) * $metersPerUpdate
            $moveEast = ($moveEast / $magnitude) * $metersPerUpdate
        }

        # Convert meters to degrees
        # Latitude: 1 degree = ~111km
        $latChange = $moveNorth / 111000.0

        # Longitude: 1 degree = 111km * cos(latitude)
        $lonChange = $moveEast / (111000.0 * [Math]::Cos([Math]::PI * $this.Latitude / 180))

        # Update position
        $this.Latitude += $latChange
        $this.Longitude += $lonChange

        # Send to emulator
        $this.SendGPSLocation()
    }

    [void] DisplayStatus([bool]$north, [bool]$south, [bool]$east, [bool]$west) {
        Clear-Host
        Write-Host "=== GPS Movement Simulator ===" -ForegroundColor Cyan
        Write-Host "Speed: $($this.SpeedKmh) km/h" -ForegroundColor Yellow
        Write-Host "Location: $([Math]::Round($this.Latitude, 6)), $([Math]::Round($this.Longitude, 6))" -ForegroundColor Green
        Write-Host ""
        Write-Host "Controls:" -ForegroundColor White
        Write-Host "  W - Move North    ↑"
        Write-Host "  S - Move South    ↓"
        Write-Host "  A - Move West     ←"
        Write-Host "  D - Move East     →"
        Write-Host "  + - Increase speed"
        Write-Host "  - - Decrease speed"
        Write-Host "  SPACE - Copy location"
        Write-Host "  Q - Quit"
        Write-Host ""
        Write-Host -NoNewline "Movement: " -ForegroundColor White

        $movement = @()
        if ($north) { $movement += "↑" }
        if ($south) { $movement += "↓" }
        if ($west) { $movement += "←" }
        if ($east) { $movement += "→" }

        if ($movement.Count -gt 0) {
            Write-Host ($movement -join " ") -ForegroundColor Cyan
        }
        else {
            Write-Host "Stopped" -ForegroundColor Gray
        }

        Write-Host ""
        Write-Host "Press keys to move, hold for continuous movement" -ForegroundColor DarkGray
    }

    [bool] IsKeyPressed([int]$vKey) {
        return [KeyReader]::GetAsyncKeyState($vKey) -band 0x8000
    }

    [void] Run() {
        Write-Host "Starting GPS Movement Simulator..." -ForegroundColor Cyan
        Write-Host "Make sure your Android emulator is running!" -ForegroundColor Yellow
        Write-Host "Press Enter to start..."
        Read-Host

        # Send initial location
        $this.SendGPSLocation()

        $lastUpdate = [DateTime]::Now
        $lastDisplay = [DateTime]::Now
        $displayInterval = [TimeSpan]::FromSeconds(0.5)

        # Key states
        $wasNorth = $false
        $wasSouth = $false
        $wasEast = $false
        $wasWest = $false

        $this.DisplayStatus($false, $false, $false, $false)

        while ($true) {
            # Check key states
            $north = $this.IsKeyPressed($this.VK_W)
            $south = $this.IsKeyPressed($this.VK_S)
            $east = $this.IsKeyPressed($this.VK_D)
            $west = $this.IsKeyPressed($this.VK_A)
            $quit = $this.IsKeyPressed($this.VK_Q)
            $plus = $this.IsKeyPressed($this.VK_PLUS)
            $minus = $this.IsKeyPressed($this.VK_MINUS)
            $space = $this.IsKeyPressed($this.VK_SPACE)

            # Check for quit
            if ($quit) {
                break
            }

            # Adjust speed
            if ($plus -and -not $wasPlus) {
                $this.SpeedKmh = [Math]::Min(50, $this.SpeedKmh + 5)
                $this.DisplayStatus($north, $south, $east, $west)
                $wasPlus = $true
            }
            elseif (-not $plus) {
                $wasPlus = $false
            }

            if ($minus -and -not $wasMinus) {
                $this.SpeedKmh = [Math]::Max(5, $this.SpeedKmh - 5)
                $this.DisplayStatus($north, $south, $east, $west)
                $wasMinus = $true
            }
            elseif (-not $minus) {
                $wasMinus = $false
            }

            # Copy location to clipboard
            if ($space -and -not $wasSpace) {
                $location = "$($this.Latitude),$($this.Longitude)"
                Set-Clipboard -Value $location
                Write-Host "`nLocation copied to clipboard: $location" -ForegroundColor Green
                Write-Host "Google Maps: https://maps.google.com/?q=$location" -ForegroundColor Cyan
                Start-Sleep -Seconds 2
                $this.DisplayStatus($north, $south, $east, $west)
                $wasSpace = $true
            }
            elseif (-not $space) {
                $wasSpace = $false
            }

            # Update display if movement state changed
            if ($north -ne $wasNorth -or $south -ne $wasSouth -or
                $east -ne $wasEast -or $west -ne $wasWest) {
                $this.DisplayStatus($north, $south, $east, $west)
                $wasNorth = $north
                $wasSouth = $south
                $wasEast = $east
                $wasWest = $west
            }

            # Update position periodically
            $now = [DateTime]::Now
            if (($now - $lastUpdate).TotalSeconds -ge $this.UpdateInterval) {
                $this.UpdatePosition($north, $south, $east, $west)
                $lastUpdate = $now
            }

            # Refresh display periodically
            if (($now - $lastDisplay) -gt $displayInterval) {
                $this.DisplayStatus($north, $south, $east, $west)
                $lastDisplay = $now
            }

            # Small delay to prevent CPU hogging
            Start-Sleep -Milliseconds 10
        }

        Write-Host "`nGPS Simulator stopped" -ForegroundColor Yellow
        Write-Host "Final location: $([Math]::Round($this.Latitude, 6)), $([Math]::Round($this.Longitude, 6))" -ForegroundColor Green
    }
}

# Check if ADB is available
try {
    $adbTest = adb devices 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ADB is not available. Please make sure ADB is in PATH." -ForegroundColor Red
        exit 1
    }

    # Check if emulator is running
    if ($adbTest -notmatch "emulator") {
        Write-Host "Warning: No emulator detected. Make sure Android emulator is running." -ForegroundColor Yellow
        Write-Host "Continue anyway? (Y/N): " -NoNewline
        $continue = Read-Host
        if ($continue -ne 'Y' -and $continue -ne 'y') {
            exit 0
        }
    }
}
catch {
    Write-Host "Error checking ADB: $_" -ForegroundColor Red
    exit 1
}

# Run the simulator
$simulator = [GPSSimulator]::new()
$simulator.Run()
Write-Host "Monitoring logs for com.surveyme..."
Write-Host "Press Ctrl+C to stop."

# Clear logs first to start fresh (optional, but helpful)
adb logcat -c

# Filter for the app, crashes, and System.err
adb logcat -v color *:S com.surveyme:V AndroidRuntime:E System.err:E

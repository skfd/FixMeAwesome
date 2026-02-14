$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "Running tests..."

cmd /c ".\gradlew.bat testDebugUnitTest" > test_output.txt 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "Tests passed!"
}
else {
    Write-Host "Tests failed!"
}

Write-Host "Output saved to test_output.txt"

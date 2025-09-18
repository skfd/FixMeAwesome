$apkDir = "~\Code\FixMeAwesome\app\build\outputs\apk\debug"
$metaFile = Join-Path $apkDir "output-metadata.json"
$meta = Get-Content $metaFile | ConvertFrom-Json
$version = $meta.elements[0].versionName
$apkName = $meta.elements[0].outputFile
$srcApk = Join-Path $apkDir $apkName
$destApk = Join-Path "~\Dropbox\my-apps" ("app-debug-$version.apk")
Copy-Item $srcApk -Destination $destApk -Force
Write-Host "Copied $apkName as app-debug-$version.apk to Dropbox."
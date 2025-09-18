@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
gradlew.bat clean assembleDebug
adb uninstall com.surveyme.debug 2>nul
adb install app\build\outputs\apk\debug\app-debug.apk
echo Done!
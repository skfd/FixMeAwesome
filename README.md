# Survey Me

Android mapping app for OpenStreetMap field surveys with GPS tracking and POI notifications.

## Requirements

- **JDK 17+**
- **Android Studio Hedgehog (2023.1.1)** or later
- **Android SDK** (API 34)
- **Android Device/Emulator** (API 23+)

## Setup

1. Clone repository
```bash
git clone https://github.com/yourusername/survey-me.git
cd survey-me
```

2. Open in Android Studio
3. Sync Gradle files
4. Update `local.properties` with your SDK path:
```
sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

## Build & Run

### Android Studio
- **Run**: Click green play button or `Shift+F10`
- **Debug**: Click bug icon or `Shift+F9`
- **Build APK**: Build → Build Bundle(s)/APK(s) → Build APK(s)

### Command Line
```bash
# Debug build
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

## Automation Scripts
See [SCRIPTS.md](SCRIPTS.md) for details on available PowerShell scripts for building, testing, and GPS simulation.


## Project Structure
```
app/
├── src/main/java/com/surveyme/
│   ├── core/          # Utilities, constants
│   ├── data/          # Repositories, data sources
│   ├── domain/        # Business logic, models
│   └── presentation/  # UI (Activities, Fragments)
└── src/main/res/      # Resources (layouts, values)
```

## Features Roadmap
See [FEATURES_ROADMAP.md](FEATURES_ROADMAP.md) for development phases.

## Architecture
- MVVM with Clean Architecture
- Navigation Component
- Kotlin Coroutines
- View Binding

## License
GPL-3.0

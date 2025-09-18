# Survey Me - Android Application for OpenStreetMap Contributors

## Important Development Rules

### Version Management
**ALWAYS increment the version code in `app/build.gradle.kts` when making code changes:**
- Increment `versionCode` by 1 for any code modification
- Update `versionName` following semantic versioning (MAJOR.MINOR.PATCH)
- This prevents installation errors like `INSTALL_FAILED_VERSION_DOWNGRADE`
- Required for proper app updates and testing

## Project Overview

Survey Me is a specialized Android mapping application designed specifically for dedicated OpenStreetMap (OSM) contributors who engage in long walks and field surveys. The application serves as a powerful field survey tool that combines real-time GPS tracking with Points of Interest (PoI) notifications to assist mappers in identifying and documenting geographic features that need to be added or updated in OpenStreetMap.

## Executive Summary

### Purpose and Vision

The primary goal of Survey Me is to enhance the efficiency and effectiveness of OSM field surveys by providing contributors with a mobile tool that actively assists them during their mapping expeditions. The app addresses several key challenges faced by OSM contributors:

1. **Field Survey Efficiency**: Mappers often walk or cycle through areas to identify missing or outdated map features. Survey Me helps them systematically cover areas and identify points that need attention.

2. **Data Collection Organization**: By providing proximity-based notifications for Points of Interest, the app ensures that contributors don't miss important locations that need to be surveyed.

3. **Continuous Operation**: Unlike typical navigation apps that may be killed by the system, Survey Me is designed to run continuously during survey sessions, ensuring complete tracking data and reliable notifications.

### Target Audience

The application is specifically designed for:

- **Active OpenStreetMap Contributors**: Individuals who regularly participate in mapping activities and understand OSM tagging conventions
- **Field Survey Teams**: Groups conducting organized mapping parties or systematic area surveys
- **Local Knowledge Experts**: Community members who want to contribute their local knowledge to OSM
- **Professional Surveyors**: Organizations conducting professional surveys for OSM improvement projects

## Core Features

### 1. Interactive Map Interface

The application's main interface is centered around a fully interactive map that serves as the primary workspace for users. Key capabilities include:

- **Standard Map Controls**: Full support for panning, zooming, and rotation gestures
- **Multiple Map Layers**: Support for various base map styles (street, satellite, terrain)
- **Real-time Rendering**: Smooth map rendering with efficient tile caching
- **Offline Capability**: Ability to download and cache map tiles for offline use during surveys

### 2. Live User Tracking

Survey Me implements robust GPS tracking functionality that goes beyond typical location-aware applications:

- **High-Precision GPS**: Utilizes Android's fused location provider for optimal accuracy
- **Background Tracking**: Continues tracking even when the app is in the background
- **Track Recording**: Records the complete path taken during survey sessions
- **Battery Optimization**: Implements intelligent location update frequencies based on movement patterns

### 3. Points of Interest Management

The PoI system is central to the survey workflow:

- **GPX File Import**: Loads PoI data from standard GPX format files
- **Visual Markers**: Displays PoIs on the map with customizable icons and labels
- **Categorization**: Supports different PoI categories with distinct visual representations
- **Information Display**: Shows detailed information about each PoI when selected

### 4. Proximity Notifications

The notification system actively assists users during their surveys:

- **Geofencing**: Creates virtual perimeters around each PoI
- **Smart Notifications**: Sends alerts when users approach PoIs within configurable distances
- **Notification Customization**: Users can configure notification preferences per PoI category
- **Do Not Disturb**: Temporary suppression of notifications for already-surveyed points

### 5. Persistent Operation

Survey Me is engineered to maintain continuous operation:

- **Foreground Service**: Runs as an Android foreground service to prevent system termination
- **Wake Locks**: Maintains partial wake locks to ensure tracking continues
- **Memory Priority**: Requests high memory priority to avoid being killed by low memory conditions
- **Explicit Stop Control**: Only stops tracking when explicitly commanded by the user

## Technical Architecture

### Technology Stack

#### Core Technologies
- **Language**: Kotlin 1.9+ (primary), Java (legacy compatibility)
- **Minimum SDK**: Android 6.0 (API Level 23)
- **Target SDK**: Android 14 (API Level 34)
- **Build System**: Gradle 8.x with Kotlin DSL

#### Key Libraries and Frameworks
- **Mapping Engine**: OpenStreetMap Android (osmdroid) 6.1.x
- **Location Services**: Google Play Services Location 21.x
- **Dependency Injection**: Hilt/Dagger 2.x
- **Database**: Room Persistence Library 2.6.x
- **Networking**: Retrofit 2.x with OkHttp 4.x
- **Reactive Programming**: Kotlin Coroutines and Flow
- **UI Components**: Material Design Components 1.11.x

### Application Architecture

The application follows Clean Architecture principles with MVVM (Model-View-ViewModel) pattern:

#### Presentation Layer
- **Activities**: Single activity architecture with Navigation Component
- **Fragments**: Feature-specific fragments for different screens
- **ViewModels**: Lifecycle-aware ViewModels managing UI state
- **Data Binding**: Two-way data binding for reactive UI updates

#### Domain Layer
- **Use Cases**: Encapsulated business logic operations
- **Repository Interfaces**: Abstractions for data access
- **Domain Models**: Business entities independent of data sources

#### Data Layer
- **Repositories**: Concrete implementations managing data sources
- **Local Data Sources**: Room database and SharedPreferences
- **Remote Data Sources**: REST APIs for map tiles and updates
- **File System**: GPX file parsing and track storage

## Development Setup

### Prerequisites

1. **Development Environment**
   - Android Studio Hedgehog (2023.1.1) or later
   - JDK 17 or later
   - Android SDK with build tools 34.0.0+
   - Git for version control

2. **Device/Emulator Requirements**
   - Physical device recommended for GPS testing
   - If using emulator: Extended controls for GPS simulation
   - Google Play Services installed (for location services)

### Build Instructions

```bash
# Clone the repository
git clone https://github.com/yourusername/survey-me.git
cd survey-me

# Set up local properties
echo "sdk.dir=/path/to/android/sdk" > local.properties

# Build debug variant
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run all tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### Configuration

#### API Keys and Services

Create `app/src/main/res/values/api_keys.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Map tile server API key if required -->
    <string name="map_tile_api_key" translatable="false">YOUR_API_KEY</string>

    <!-- Analytics service key -->
    <string name="analytics_key" translatable="false">YOUR_ANALYTICS_KEY</string>
</resources>
```

#### Build Variants

The project supports multiple build variants:

- **Debug**: Development build with debugging tools enabled
- **Release**: Production-ready build with optimizations
- **Beta**: Testing build with crash reporting but without obfuscation

### Code Style and Standards

#### Kotlin Style Guide
- Follow official Kotlin coding conventions
- Use meaningful variable and function names
- Prefer immutability (val over var)
- Use data classes for simple data holders
- Leverage Kotlin's null safety features

#### Code Organization
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/surveyme/
│   │   │   ├── data/          # Data layer
│   │   │   ├── domain/        # Domain layer
│   │   │   ├── presentation/  # UI layer
│   │   │   └── core/          # Shared utilities
│   │   ├── res/               # Resources
│   │   └── AndroidManifest.xml
│   └── test/                  # Unit tests
```

## Testing Strategy

### Unit Testing
- Comprehensive unit tests for ViewModels, Use Cases, and Repositories
- Mock dependencies using MockK
- Minimum 80% code coverage for business logic

### Integration Testing
- Test data flow between layers
- Verify Room database operations
- Test GPS tracking service lifecycle

### UI Testing
- Espresso tests for critical user journeys
- Screenshot testing for different device configurations
- Accessibility testing compliance

## Deployment and Distribution

### Release Process

1. **Version Management**
   - Semantic versioning (MAJOR.MINOR.PATCH)
   - Automated version code incrementation
   - Git tags for each release

2. **Build Pipeline**
   - CI/CD using GitHub Actions or GitLab CI
   - Automated testing on multiple device configurations
   - Code quality checks with detekt and ktlint

3. **Distribution Channels**
   - Google Play Store (primary)
   - F-Droid for open-source distribution
   - Direct APK downloads from GitHub releases

### App Signing

```gradle
android {
    signingConfigs {
        release {
            storeFile file(RELEASE_STORE_FILE)
            storePassword RELEASE_STORE_PASSWORD
            keyAlias RELEASE_KEY_ALIAS
            keyPassword RELEASE_KEY_PASSWORD
        }
    }
}
```

## Performance Considerations

### Battery Optimization
- Adaptive location update frequencies based on movement speed
- Batch notification processing to reduce wake locks
- Efficient map tile caching to minimize network usage
- Background work scheduling using WorkManager

### Memory Management
- Proper lifecycle management to prevent memory leaks
- Image and map tile recycling
- Lazy loading of PoI data
- Pagination for large datasets

### Network Optimization
- Offline-first architecture with sync when connected
- Compressed data transfer using GZIP
- Intelligent prefetching of map tiles
- Request batching and caching

## Security and Privacy

### User Privacy
- No personal data collection without explicit consent
- Local-only storage of tracking data by default
- Optional cloud sync with end-to-end encryption
- Clear data deletion mechanisms

### Application Security
- ProGuard/R8 obfuscation for release builds
- Certificate pinning for API communications
- Secure storage using Android Keystore
- Input validation and sanitization

## Future Enhancements

### Planned Features

1. **Collaborative Mapping**
   - Real-time sharing of survey progress with team members
   - Synchronized PoI lists across devices
   - Conflict resolution for simultaneous edits

2. **Advanced Survey Tools**
   - Photo capture and geotagging
   - Audio note recording
   - Quick OSM tag editor
   - Measurement tools for distances and areas

3. **Data Analysis**
   - Heat maps of surveyed areas
   - Coverage statistics and reports
   - Contribution history and achievements
   - Export to various formats (KML, GeoJSON)

4. **Integration Enhancements**
   - Direct upload to OpenStreetMap
   - Integration with JOSM and iD editors
   - Support for Mapillary and KartaView
   - OSM changeset management

## Support and Documentation

### User Documentation
- Comprehensive user guide in the app
- Video tutorials for key features
- FAQ section
- Community forum integration

### Developer Documentation
- API documentation using Dokka
- Architecture decision records (ADRs)
- Contributing guidelines
- Code review checklist

## License and Attribution

This project is licensed under the GNU General Public License v3.0 (GPL-3.0), ensuring it remains free and open-source for the OpenStreetMap community.

### Third-party Attributions
- OpenStreetMap data © OpenStreetMap contributors
- Map tiles from various providers (configurable)
- Icons from Material Design Icons
- Additional open-source libraries as listed in app/build.gradle

## Contact and Contributing

### Contributing
We welcome contributions from the OSM community! Please see CONTRIBUTING.md for guidelines.

### Issue Reporting
Report bugs and request features through our GitHub Issues tracker.

### Community
- OSM Forum discussion thread
- Matrix/IRC channel: #surveyme:matrix.org
- Mailing list: surveyme-dev@lists.openstreetmap.org

---

*This document serves as the primary reference for developers working on Survey Me. It should be updated whenever significant architectural decisions or changes are made to the project.*
- rebuild, reinstall and tell me what to test after you change code
- update feature roadmap when something is implemented
- after implementing a feature give me one line COMMIT MESSAGE  I should use, and TO TEST list of thinkgings s to test
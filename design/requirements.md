# Survey Me - Software Requirements Specification (SRS)

## Document Information

- **Version**: 1.0.0
- **Date**: January 2025
- **Status**: Draft
- **Stakeholders**: OpenStreetMap Community, Development Team, End Users
- **Document Owner**: Survey Me Development Team

## 1. Introduction

### 1.1 Purpose

This Software Requirements Specification (SRS) document provides a comprehensive description of the functional and non-functional requirements for the Survey Me Android application. This document is intended for developers, testers, project managers, and stakeholders involved in the development and deployment of the application.

### 1.2 Scope

Survey Me is an Android mobile application designed to assist OpenStreetMap contributors in conducting field surveys. The application provides real-time GPS tracking, Points of Interest (PoI) management, and proximity-based notifications to enhance the efficiency of mapping activities. The system will operate as a standalone mobile application with optional cloud synchronization capabilities.

### 1.3 Definitions, Acronyms, and Abbreviations

- **OSM**: OpenStreetMap - A collaborative project to create a free editable map of the world
- **PoI**: Point of Interest - A specific geographic location that may be of interest to mappers
- **GPX**: GPS Exchange Format - An XML schema for describing waypoints, tracks, and routes
- **GPS**: Global Positioning System
- **API**: Application Programming Interface
- **UI**: User Interface
- **UX**: User Experience
- **GNSS**: Global Navigation Satellite System
- **WGS84**: World Geodetic System 1984 - The reference coordinate system used by GPS

### 1.4 References

- OpenStreetMap Wiki: https://wiki.openstreetmap.org/
- GPX 1.1 Schema Documentation: https://www.topografix.com/GPX/1/1/
- Android Developer Documentation: https://developer.android.com/
- Material Design Guidelines: https://material.io/design/

## 2. Overall Description

### 2.1 Product Perspective

Survey Me operates within the broader OpenStreetMap ecosystem as a field data collection tool. The application interfaces with:

- **Android Operating System**: For device services and hardware access
- **GPS/GNSS Hardware**: For location determination
- **File System**: For GPX file storage and retrieval
- **Network Services**: For map tile downloads and optional data synchronization
- **Notification System**: For proximity alerts

### 2.2 Product Functions

The primary functions of Survey Me include:

1. Display interactive maps with user location
2. Track user movement continuously, even in background
3. Load and display Points of Interest from GPX files
4. Send proximity notifications when approaching PoIs
5. Maintain persistent operation without system termination
6. Record tracking data for later analysis

### 2.3 User Classes and Characteristics

#### 2.3.1 Primary Users: OSM Contributors
- **Technical Proficiency**: Medium to High
- **Domain Knowledge**: Strong understanding of OSM and mapping concepts
- **Usage Pattern**: Regular, extended sessions (1-4 hours)
- **Key Needs**: Reliability, accuracy, battery efficiency

#### 2.3.2 Secondary Users: Casual Mappers
- **Technical Proficiency**: Low to Medium
- **Domain Knowledge**: Basic understanding of mapping
- **Usage Pattern**: Occasional, shorter sessions
- **Key Needs**: Ease of use, clear instructions

#### 2.3.3 Tertiary Users: Professional Surveyors
- **Technical Proficiency**: High
- **Domain Knowledge**: Expert level
- **Usage Pattern**: Daily, full workday sessions
- **Key Needs**: Advanced features, data export, integration capabilities

### 2.4 Operating Environment

- **Platform**: Android 6.0 (API Level 23) and higher
- **Device Types**: Smartphones and tablets with GPS capability
- **Network**: Optional internet connectivity for map tiles
- **Storage**: Minimum 100MB free space for app and cache
- **Memory**: Minimum 2GB RAM recommended

### 2.5 Design and Implementation Constraints

- Must comply with Google Play Store policies
- Must respect Android battery optimization guidelines
- Must handle limited or no network connectivity
- Must support devices with varying GPS accuracy
- Must maintain user privacy and data security
- Must be compatible with standard GPX format

### 2.6 Assumptions and Dependencies

- Users have Android devices with functioning GPS
- Users have basic knowledge of map reading
- GPX files follow standard 1.1 specification
- Map tile servers remain accessible
- Google Play Services available for enhanced location

## 3. Functional Requirements

### 3.1 Map Display and Interaction

#### FR-MAP-001: Map Rendering
- **Priority**: Critical
- **Description**: The system shall render interactive maps using OpenStreetMap data
- **Acceptance Criteria**:
  - Maps display at multiple zoom levels (5-19)
  - Smooth panning and zooming gestures
  - Support for pinch-to-zoom and double-tap zoom
  - Rotation gesture support with north indicator

#### FR-MAP-002: Map Layers
- **Priority**: High
- **Description**: The system shall support multiple map layer types
- **Acceptance Criteria**:
  - Standard street map layer
  - Satellite imagery layer (when available)
  - Terrain/topographic layer option
  - Ability to switch between layers

#### FR-MAP-003: Offline Map Support
- **Priority**: High
- **Description**: The system shall cache map tiles for offline use
- **Acceptance Criteria**:
  - Automatic caching of viewed areas
  - Manual download of specific regions
  - Cache size management (configurable limit)
  - Cache expiration handling

#### FR-MAP-004: Map Controls
- **Priority**: Medium
- **Description**: The system shall provide on-screen map controls
- **Acceptance Criteria**:
  - Zoom in/out buttons
  - Current location button
  - Compass/north alignment button
  - Scale indicator

### 3.2 User Location and Tracking

#### FR-LOC-001: GPS Location Display
- **Priority**: Critical
- **Description**: The system shall display current user location on map
- **Acceptance Criteria**:
  - Location marker with accuracy circle
  - Direction indicator (when moving)
  - Smooth location updates
  - GPS status indicator

#### FR-LOC-002: Continuous Background Tracking
- **Priority**: Critical
- **Description**: The system shall track user location continuously in background
- **Acceptance Criteria**:
  - Tracking continues when app minimized
  - Tracking continues when screen off
  - Persistent notification during tracking
  - Wake lock management

#### FR-LOC-003: Track Recording
- **Priority**: High
- **Description**: The system shall record user movement tracks
- **Acceptance Criteria**:
  - Timestamp for each location point
  - Altitude and accuracy recording
  - Track segmentation for pauses
  - Export to GPX format

#### FR-LOC-004: Tracking Controls
- **Priority**: Critical
- **Description**: The system shall provide explicit tracking controls
- **Acceptance Criteria**:
  - Start/stop tracking button
  - Pause/resume functionality
  - Clear confirmation for stop action
  - Visual tracking status indicator

### 3.3 Points of Interest Management

#### FR-POI-001: GPX File Import
- **Priority**: Critical
- **Description**: The system shall import PoI data from GPX files
- **Acceptance Criteria**:
  - Support for GPX 1.0 and 1.1 formats
  - File picker integration
  - Validation of GPX structure
  - Error handling for invalid files

#### FR-POI-002: PoI Display
- **Priority**: Critical
- **Description**: The system shall display PoIs on the map
- **Acceptance Criteria**:
  - Distinct markers for PoIs
  - Clustering at low zoom levels
  - Labels for PoI names
  - Different icons for categories

#### FR-POI-003: PoI Information
- **Priority**: High
- **Description**: The system shall show detailed PoI information
- **Acceptance Criteria**:
  - Name and description display
  - Coordinates and elevation
  - Distance from current location
  - Custom attributes from GPX

#### FR-POI-004: PoI Management
- **Priority**: Medium
- **Description**: The system shall allow PoI list management
- **Acceptance Criteria**:
  - Load multiple GPX files
  - Enable/disable PoI sets
  - Delete imported PoI sets
  - PoI search functionality

### 3.4 Proximity Notifications

#### FR-NOT-001: Proximity Detection
- **Priority**: Critical
- **Description**: The system shall detect when user approaches PoIs
- **Acceptance Criteria**:
  - Configurable proximity radius (50m-500m)
  - Geofencing implementation
  - Battery-efficient detection
  - Works in background

#### FR-NOT-002: Notification Delivery
- **Priority**: Critical
- **Description**: The system shall send notifications for nearby PoIs
- **Acceptance Criteria**:
  - Android notification with sound/vibration
  - PoI name and distance in notification
  - Tap to view on map action
  - Notification grouping for multiple PoIs

#### FR-NOT-003: Notification Preferences
- **Priority**: High
- **Description**: The system shall allow notification customization
- **Acceptance Criteria**:
  - Enable/disable per PoI category
  - Quiet hours configuration
  - Sound and vibration settings
  - Minimum time between notifications

#### FR-NOT-004: Notification History
- **Priority**: Low
- **Description**: The system shall maintain notification history
- **Acceptance Criteria**:
  - List of recent notifications
  - Timestamp and location
  - Mark as visited/surveyed
  - Clear history option

### 3.5 Application Lifecycle

#### FR-LIFE-001: Persistent Operation
- **Priority**: Critical
- **Description**: The system shall maintain operation without system termination
- **Acceptance Criteria**:
  - Foreground service implementation
  - High priority process
  - Memory pressure handling
  - Crash recovery mechanism

#### FR-LIFE-002: Explicit Termination
- **Priority**: Critical
- **Description**: The system shall only stop when explicitly commanded
- **Acceptance Criteria**:
  - Clear stop button in app
  - Confirmation dialog
  - Stop action in notification
  - Force stop recognition

#### FR-LIFE-003: State Preservation
- **Priority**: High
- **Description**: The system shall preserve state across restarts
- **Acceptance Criteria**:
  - Save current track
  - Restore map position
  - Maintain PoI selections
  - Resume tracking if active

### 3.6 Data Management

#### FR-DATA-001: Local Storage
- **Priority**: High
- **Description**: The system shall manage local data storage
- **Acceptance Criteria**:
  - SQLite database for tracks
  - File storage for GPX
  - SharedPreferences for settings
  - Cache management

#### FR-DATA-002: Data Export
- **Priority**: Medium
- **Description**: The system shall export collected data
- **Acceptance Criteria**:
  - Export tracks as GPX
  - Share via Android sharing
  - Email integration
  - Cloud storage support

#### FR-DATA-003: Data Import
- **Priority**: Medium
- **Description**: The system shall import various data formats
- **Acceptance Criteria**:
  - GPX waypoint import
  - KML basic support
  - CSV coordinate lists
  - Format conversion

## 4. Non-Functional Requirements

### 4.1 Performance Requirements

#### NFR-PERF-001: Application Startup
- **Metric**: Time to interactive map display
- **Target**: < 3 seconds on mid-range devices
- **Measurement**: From app launch to map rendered

#### NFR-PERF-002: GPS Acquisition
- **Metric**: Time to first GPS fix
- **Target**: < 30 seconds with clear sky view
- **Measurement**: From GPS enable to location display

#### NFR-PERF-003: Map Rendering Performance
- **Metric**: Frame rate during map interaction
- **Target**: > 30 FPS for panning and zooming
- **Measurement**: GPU profiling tools

#### NFR-PERF-004: Memory Usage
- **Metric**: RAM consumption during operation
- **Target**: < 150MB for typical usage
- **Measurement**: Android Studio profiler

#### NFR-PERF-005: Battery Efficiency
- **Metric**: Battery drain rate during tracking
- **Target**: < 15% per hour of active tracking
- **Measurement**: Battery historian analysis

#### NFR-PERF-006: Storage Efficiency
- **Metric**: App size and cache usage
- **Target**: < 50MB APK, < 500MB cache
- **Measurement**: Storage analyzer

### 4.2 Reliability Requirements

#### NFR-REL-001: Application Stability
- **Metric**: Mean time between failures (MTBF)
- **Target**: > 100 hours of operation
- **Measurement**: Crash reporting analytics

#### NFR-REL-002: Data Integrity
- **Metric**: Track data loss rate
- **Target**: < 0.1% data loss
- **Measurement**: Automated testing validation

#### NFR-REL-003: Recovery Capability
- **Metric**: Recovery time from crash
- **Target**: < 5 seconds to resume tracking
- **Measurement**: Automated crash recovery tests

#### NFR-REL-004: Network Resilience
- **Metric**: Operation without network
- **Target**: 100% core functionality offline
- **Measurement**: Offline mode testing

### 4.3 Usability Requirements

#### NFR-USE-001: Learning Curve
- **Metric**: Time for new user to start tracking
- **Target**: < 2 minutes without documentation
- **Measurement**: User testing sessions

#### NFR-USE-002: Error Messages
- **Metric**: User understanding of errors
- **Target**: 90% comprehension rate
- **Measurement**: User feedback surveys

#### NFR-USE-003: Accessibility
- **Metric**: WCAG 2.1 compliance level
- **Target**: Level AA compliance
- **Measurement**: Accessibility scanner

#### NFR-USE-004: Language Support
- **Metric**: Number of supported languages
- **Target**: 10+ major languages
- **Measurement**: Translation coverage

### 4.4 Security Requirements

#### NFR-SEC-001: Data Privacy
- **Requirement**: No personal data collection without consent
- **Implementation**: Opt-in for all data sharing
- **Verification**: Privacy audit

#### NFR-SEC-002: Secure Storage
- **Requirement**: Encrypted storage for sensitive data
- **Implementation**: Android Keystore usage
- **Verification**: Security testing

#### NFR-SEC-003: Network Security
- **Requirement**: Secure communication channels
- **Implementation**: HTTPS only, certificate pinning
- **Verification**: Network traffic analysis

#### NFR-SEC-004: Permission Management
- **Requirement**: Minimal permission requests
- **Implementation**: Runtime permissions
- **Verification**: Permission audit

### 4.5 Compatibility Requirements

#### NFR-COMP-001: Android Version Support
- **Requirement**: Android 6.0 (API 23) to latest
- **Coverage**: 95%+ of active devices
- **Testing**: Multiple version testing

#### NFR-COMP-002: Device Compatibility
- **Requirement**: Phones and tablets
- **Screen sizes**: 4" to 10"
- **Testing**: Device lab testing

#### NFR-COMP-003: GPS Hardware
- **Requirement**: Standard GPS, GLONASS, Galileo
- **Accuracy**: Utilize best available
- **Testing**: Multiple chipset testing

### 4.6 Maintainability Requirements

#### NFR-MAIN-001: Code Quality
- **Metric**: Code coverage
- **Target**: > 80% test coverage
- **Measurement**: Coverage reports

#### NFR-MAIN-002: Documentation
- **Metric**: Documentation completeness
- **Target**: 100% public API documented
- **Measurement**: Documentation generation

#### NFR-MAIN-003: Modular Architecture
- **Metric**: Component independence
- **Target**: < 5% circular dependencies
- **Measurement**: Dependency analysis

#### NFR-MAIN-004: Update Mechanism
- **Requirement**: Over-the-air updates
- **Implementation**: Google Play updates
- **Testing**: Update testing

## 5. System Interfaces

### 5.1 User Interfaces

#### UI-001: Main Map Screen
- Full-screen map display
- Floating action buttons for controls
- Bottom sheet for PoI details
- Top bar for status information

#### UI-002: Settings Screen
- Categorized preference groups
- Visual feedback for changes
- Reset to defaults option
- Import/export settings

#### UI-003: Track Management
- List view of recorded tracks
- Swipe actions for delete/share
- Sorting and filtering options
- Batch operations support

### 5.2 Hardware Interfaces

#### HW-001: GPS Receiver
- Access to device GPS hardware
- Support for assisted GPS (A-GPS)
- Fallback to network location
- External GPS support (Bluetooth)

#### HW-002: Storage
- Internal storage access
- External SD card support
- USB OTG for file transfer
- Cloud storage integration

### 5.3 Software Interfaces

#### SW-001: Android OS Services
- Location services API
- Notification manager
- File system access
- Power management

#### SW-002: External Services
- OpenStreetMap tile servers
- Nominatim geocoding service
- Weather data API (optional)
- Elevation data service

### 5.4 Communication Interfaces

#### COM-001: Network Protocols
- HTTP/HTTPS for tiles
- WebSocket for real-time updates
- REST API for data sync
- Protocol buffers for efficiency

## 6. Quality Attributes

### 6.1 Availability
- Target: 99.9% availability when device is on
- Measurement: Uptime monitoring
- Recovery: Automatic restart on failure

### 6.2 Scalability
- Handle 100,000+ PoIs efficiently
- Support tracks with 50,000+ points
- Manage 10GB+ of cached map data

### 6.3 Flexibility
- Plugin architecture for extensions
- Configurable data sources
- Customizable UI themes
- Scriptable automation

### 6.4 Portability
- Abstract hardware dependencies
- Platform-agnostic core logic
- Potential for iOS version
- Web version possibility

## 7. Constraints and Assumptions

### 7.1 Technical Constraints
- Limited by device GPS accuracy
- Battery life limitations
- Storage space constraints
- Network bandwidth limitations

### 7.2 Business Constraints
- Zero budget for paid services initially
- Volunteer development model
- Open-source licensing requirements
- Community-driven priorities

### 7.3 Regulatory Constraints
- GDPR compliance for EU users
- COPPA compliance for minors
- Export control regulations
- Location data privacy laws

## 8. Acceptance Criteria

### 8.1 Functional Acceptance
- All critical requirements implemented
- 95% of high priority requirements complete
- No critical bugs in production
- User acceptance testing passed

### 8.2 Performance Acceptance
- Meets all performance targets
- Battery life goals achieved
- Smooth UI operation verified
- Memory usage within limits

### 8.3 Quality Acceptance
- Code review completed
- Security audit passed
- Accessibility compliance verified
- Documentation complete

## 9. Future Considerations

### 9.1 Version 2.0 Features
- Collaborative mapping features
- Machine learning for PoI suggestions
- Augmented reality overlay
- Voice navigation support

### 9.2 Platform Expansion
- iOS version development
- Web application version
- Wear OS companion app
- Desktop synchronization tool

### 9.3 Integration Possibilities
- JOSM plugin development
- iD editor integration
- Mapillary photo upload
- OSM API direct editing

## Appendices

### Appendix A: Glossary
[Comprehensive glossary of technical terms and acronyms used in the document]

### Appendix B: Use Case Diagrams
[Visual representations of user interactions with the system]

### Appendix C: Data Flow Diagrams
[Diagrams showing how data moves through the system]

### Appendix D: State Diagrams
[State transition diagrams for key system components]

---

*This requirements document is a living document and will be updated as the project evolves. All changes should be tracked through version control and approved by relevant stakeholders.*
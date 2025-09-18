# Survey Me - Changelog

## Version 1.2.3 (2025-09-18)

**Toronto Data**: "read file from data_samples folder"
Integrated 20 Toronto Bike Share stations as default POI data loaded from JSON on app launch.

## Version 1.2.2 (2025-09-18)

**Phase 13**: Proximity Detection - "notifications are not sent"
Fixed Android 13+ notification permissions and added runtime permission request.

**Phase 12**: POI Display on Map
Added POI markers to map with category icons and auto-insertion of sample data.

**Phase 10-11**: POI System Foundation
Created Room database for POIs with GPX import support and type converters.

**GPS Tools**: "create gps walker"
Built PowerShell GPS simulators with WASD controls for testing without physical movement.

**Build System**: Automation setup
Created build-install.ps1 script and fixed gradle-wrapper.jar issue.

**Bug Fixes**: App crash on fragment destroy
Fixed NullPointerException in MapFragment and added missing manifest permissions.
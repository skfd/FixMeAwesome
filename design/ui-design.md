# Survey Me - UI/UX Design Specifications

## 1. Design Philosophy and Principles

### 1.1 Core Design Philosophy

Survey Me follows a **"Map-First, Minimal-Chrome"** design philosophy that prioritizes the map view and geographic information while minimizing UI elements that obstruct the user's view of the map. The design emphasizes clarity, efficiency, and accessibility for users who are often walking or cycling while using the app.

### 1.2 Design Principles

1. **Glanceability**: Information must be readable at a glance while walking
2. **Thumb-Reachability**: Primary controls within thumb reach for one-handed use
3. **Context-Awareness**: UI adapts based on user activity and environment
4. **Progressive Disclosure**: Show only essential information, details on demand
5. **Consistency**: Uniform interaction patterns throughout the app
6. **Accessibility**: WCAG 2.1 AA compliance for all interactive elements
7. **Performance**: Smooth 60fps interactions even on mid-range devices
8. **Offline-First**: Full functionality without network connectivity

## 2. Visual Design System

### 2.1 Color Palette

#### Primary Colors
```scss
// Brand Colors
$primary-blue: #2196F3;      // Primary actions, selected states
$primary-dark: #1976D2;      // Pressed states, emphasis
$primary-light: #BBDEFB;     // Backgrounds, disabled states

// Semantic Colors
$success-green: #4CAF50;     // GPS lock, completed actions
$warning-orange: #FF9800;    // Poor GPS, warnings
$error-red: #F44336;         // Errors, stop actions
$info-cyan: #00BCD4;         // Information, tips

// Neutral Colors
$gray-900: #212121;          // Primary text
$gray-700: #616161;          // Secondary text
$gray-500: #9E9E9E;          // Disabled text, borders
$gray-300: #E0E0E0;          // Dividers
$gray-100: #F5F5F5;          // Backgrounds
$white: #FFFFFF;             // Cards, surfaces
$black: #000000;             // High contrast text
```

#### POI Category Colors
```scss
$poi-amenity: #8BC34A;       // Light green
$poi-shop: #FF5722;          // Deep orange
$poi-tourism: #9C27B0;       // Purple
$poi-transport: #3F51B5;     // Indigo
$poi-natural: #009688;       // Teal
$poi-historic: #795548;      // Brown
$poi-leisure: #FFC107;       // Amber
$poi-unknown: #607D8B;       // Blue grey
```

### 2.2 Typography

```scss
// Font Family
$font-primary: 'Roboto', system-ui, -apple-system, sans-serif;
$font-mono: 'Roboto Mono', 'Courier New', monospace;

// Font Sizes (Material Design Type Scale)
$display-large: 57sp;        // App title only
$display-medium: 45sp;       // Never used
$display-small: 36sp;        // Never used

$headline-large: 32sp;       // Screen titles
$headline-medium: 28sp;      // Section headers
$headline-small: 24sp;       // Card titles

$title-large: 22sp;          // Dialog titles
$title-medium: 16sp;         // List subheaders
$title-small: 14sp;          // Tab labels

$body-large: 16sp;           // Primary content
$body-medium: 14sp;          // Default text
$body-small: 12sp;           // Secondary text

$label-large: 14sp;          // Buttons
$label-medium: 12sp;         // Chips
$label-small: 11sp;          // Overlines

// Line Heights
$line-height-tight: 1.2;
$line-height-normal: 1.5;
$line-height-relaxed: 1.75;

// Font Weights
$font-light: 300;
$font-regular: 400;
$font-medium: 500;
$font-semibold: 600;
$font-bold: 700;
```

### 2.3 Spacing System

```scss
// Base unit: 8dp grid
$space-xxs: 2dp;   // Hairline spacing
$space-xs: 4dp;    // Tight spacing
$space-sm: 8dp;    // Small spacing
$space-md: 16dp;   // Default spacing
$space-lg: 24dp;   // Large spacing
$space-xl: 32dp;   // Extra large spacing
$space-xxl: 48dp;  // Jumbo spacing
$space-xxxl: 64dp; // Maximum spacing

// Component-specific spacing
$card-padding: 16dp;
$list-item-padding: 16dp;
$button-padding: 8dp 16dp;
$dialog-padding: 24dp;
$bottom-sheet-padding: 16dp;
```

### 2.4 Elevation and Shadows

```scss
// Material Design elevation levels
$elevation-0: none;                           // Flat
$elevation-1: 0 1px 3px rgba(0,0,0,0.12);   // Cards at rest
$elevation-2: 0 2px 6px rgba(0,0,0,0.16);   // FAB at rest
$elevation-3: 0 3px 9px rgba(0,0,0,0.20);   // Raised button
$elevation-4: 0 4px 12px rgba(0,0,0,0.24);  // Modal bottom sheet
$elevation-5: 0 6px 18px rgba(0,0,0,0.28);  // Dialog
$elevation-6: 0 8px 24px rgba(0,0,0,0.32);  // Navigation drawer
```

## 3. Screen Layouts and Wireframes

### 3.1 Main Map Screen

```
┌─────────────────────────────────┐
│ Status Bar                      │
├─────────────────────────────────┤
│                                 │
│     [Map View Full Screen]     │
│                                 │
│  ○ (User Location)             │
│                                 │
│  ◆ ◆ ◆ (POI Markers)          │
│                                 │
│                    [N↑]  [⊕][⊖] │ ← Map controls
│                                 │
│ ┌─────────────────────┐        │
│ │ GPS: Good | 5m      │        │ ← Status chip
│ └─────────────────────┘        │
│                                 │
│          [ ⦿ ]                  │ ← FAB (Start tracking)
│                                 │
├─────────────────────────────────┤
│ Bottom Navigation Bar           │
│ [Map] [POIs] [Tracks] [Settings]│
└─────────────────────────────────┘
```

#### Component Specifications

**Floating Action Button (FAB)**
- Size: 56dp × 56dp (regular), 40dp × 40dp (mini)
- Position: Bottom right, 16dp margin
- States:
  - Idle: Blue with location icon
  - Tracking: Red with stop icon, pulsing animation
  - Paused: Yellow with pause icon

**Map Controls**
- Size: 40dp × 40dp per button
- Position: Right edge, 16dp margin
- Buttons:
  - Compass (rotation reset)
  - My Location (center on user)
  - Zoom In/Out
  - Layers (map style selector)

**GPS Status Chip**
- Height: 32dp
- Padding: 8dp horizontal, 4dp vertical
- Colors:
  - Excellent (< 5m): Green background
  - Good (5-10m): Blue background
  - Fair (10-25m): Yellow background
  - Poor (> 25m): Red background

### 3.2 POI List Screen

```
┌─────────────────────────────────┐
│ ← POI Sources          [Import] │
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ 🔍 Search POIs...           │ │
│ └─────────────────────────────┘ │
│                                 │
│ Filter: [All] [Near] [Categories]│
│                                 │
│ ┌─────────────────────────────┐ │
│ │ ◆ Restaurant Name           │ │
│ │   500m away · Food          │ │
│ │   ──────────────────────    │ │
│ │ ◆ Shop Name                 │ │
│ │   750m away · Shopping      │ │
│ │   ──────────────────────    │ │
│ │ ◆ Historic Site            │ │
│ │   1.2km away · Tourism      │ │
│ └─────────────────────────────┘ │
│                                 │
│ Showing 47 of 234 POIs         │
└─────────────────────────────────┘
```

#### List Item Specifications

```
┌───────────────────────────────────┐
│ ┌───┐                             │
│ │ ◆ │  POI Name                ▼ │
│ └───┘  Category · Distance       │
│        Last notified: 2 hrs ago  │
└───────────────────────────────────┘
```

- Height: 72dp (standard), 88dp (with subtitle)
- Icon size: 40dp × 40dp
- Text: Title (16sp medium), Subtitle (14sp regular)
- Swipe actions: Delete (red), Edit (blue)

### 3.3 Track Management Screen

```
┌─────────────────────────────────┐
│ ← My Tracks          [Export All]│
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ ACTIVE TRACK                 │ │
│ │ Morning Survey               │ │
│ │ ● Recording · 01:23:45       │ │
│ │ 5.2 km · 423 points         │ │
│ │ [PAUSE] [STOP]              │ │
│ └─────────────────────────────┘ │
│                                 │
│ RECENT TRACKS                  │
│ ┌─────────────────────────────┐ │
│ │ [====] Yesterday's Walk     │ │
│ │        2.3 km · 45 min      │ │
│ │ ──────────────────────      │ │
│ │ [====] Sunday Survey        │ │
│ │        8.7 km · 2.5 hrs     │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

### 3.4 Settings Screen

```
┌─────────────────────────────────┐
│ ← Settings                      │
├─────────────────────────────────┤
│ TRACKING                        │
│ ┌─────────────────────────────┐ │
│ │ GPS Accuracy      [High ▼]  │ │
│ │ Update Interval   [5 sec ▼] │ │
│ │ Background Track  [ON ─●]   │ │
│ └─────────────────────────────┘ │
│                                 │
│ NOTIFICATIONS                   │
│ ┌─────────────────────────────┐ │
│ │ POI Alerts        [ON ─●]   │ │
│ │ Alert Radius      [50m ▼]   │ │
│ │ Vibration         [ON ─●]   │ │
│ │ Sound             [Default ▼]│ │
│ └─────────────────────────────┘ │
│                                 │
│ MAP                            │
│ ┌─────────────────────────────┐ │
│ │ Default Style     [OSM ▼]   │ │
│ │ Offline Maps      [Manage]  │ │
│ │ Cache Size        234 MB    │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

## 4. Component Specifications

### 4.1 Navigation Components

#### Bottom Navigation Bar
```kotlin
data class BottomNavItem(
    val icon: Int,
    val label: String,
    val route: String,
    val badge: Int? = null
)

val navigationItems = listOf(
    BottomNavItem(R.drawable.ic_map, "Map", "map"),
    BottomNavItem(R.drawable.ic_poi, "POIs", "pois", badge = 47),
    BottomNavItem(R.drawable.ic_tracks, "Tracks", "tracks"),
    BottomNavItem(R.drawable.ic_settings, "Settings", "settings")
)
```

- Height: 56dp (phone), 64dp (tablet)
- Icon size: 24dp × 24dp
- Label: 12sp, appears on selection
- Active indicator: 3dp height, primary color

#### Navigation Drawer (Tablet)
```
┌─────────────────┐
│ Survey Me       │
│ user@email.com  │
├─────────────────┤
│ 📍 Map          │
│ ◆ POIs (47)     │
│ 〜 Tracks       │
│ ⚙ Settings      │
├─────────────────┤
│ 📊 Statistics   │
│ 💾 Backup       │
│ ℹ About         │
└─────────────────┘
```

- Width: 320dp (standard), 360dp (wide)
- Item height: 48dp
- Section dividers: 1dp, gray-300

### 4.2 Input Components

#### Text Fields
```xml
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="POI Name"
    app:errorEnabled="true"
    app:counterEnabled="true"
    app:counterMaxLength="50"
    app:startIconDrawable="@drawable/ic_poi">

    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="text"
        android:maxLength="50" />

</com.google.android.material.textfield.TextInputLayout>
```

#### Buttons

**Contained Button (Primary Actions)**
- Height: 36dp
- Min width: 64dp
- Corner radius: 4dp
- Text: 14sp, ALL CAPS
- Padding: 8dp vertical, 16dp horizontal

**Text Button (Secondary Actions)**
- Height: 36dp
- Text: 14sp, ALL CAPS
- Padding: 8dp
- Ripple effect on touch

**Icon Button**
- Size: 48dp × 48dp touch target
- Icon: 24dp × 24dp
- Padding: 12dp

### 4.3 Feedback Components

#### Snackbar
```kotlin
Snackbar.make(view, "POI added successfully", Snackbar.LENGTH_SHORT)
    .setAction("UNDO") { /* Undo action */ }
    .setBackgroundTint(primaryColor)
    .setTextColor(Color.WHITE)
    .setActionTextColor(accentColor)
    .show()
```

- Position: Above bottom navigation
- Duration: SHORT (2s), LONG (3.5s), INDEFINITE
- Max width: 600dp (tablet)
- Animation: Slide up/down

#### Progress Indicators

**Linear Progress**
```xml
<com.google.android.material.progressindicator.LinearProgressIndicator
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:indeterminate="true"
    app:indicatorColor="@color/primary"
    app:trackThickness="4dp" />
```

**Circular Progress**
```xml
<com.google.android.material.progressindicator.CircularProgressIndicator
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:indeterminate="true"
    app:indicatorColor="@color/primary"
    app:indicatorSize="48dp"
    app:trackThickness="4dp" />
```

## 5. Interaction Patterns

### 5.1 Gesture Support

| Gesture | Action | Context |
|---------|--------|---------|
| Tap | Select/Open | All interactive elements |
| Double Tap | Zoom in | Map view |
| Long Press | Context menu | List items, map markers |
| Pinch | Zoom | Map view |
| Spread | Zoom out | Map view |
| Drag | Pan | Map view, lists |
| Swipe Horizontal | Delete/Archive | List items |
| Swipe Vertical | Scroll | Lists, settings |
| Two-finger Rotate | Rotate map | Map view |

### 5.2 Animation Specifications

#### Transition Animations
```kotlin
// Shared element transition
val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
    activity,
    view,
    "transition_poi_detail"
)

// Duration and interpolators
const val ANIMATION_DURATION_SHORT = 200L
const val ANIMATION_DURATION_MEDIUM = 300L
const val ANIMATION_DURATION_LONG = 500L

val INTERPOLATOR_FAST_OUT_SLOW_IN = FastOutSlowInInterpolator()
val INTERPOLATOR_FAST_OUT_LINEAR_IN = FastOutLinearInInterpolator()
val INTERPOLATOR_LINEAR_OUT_SLOW_IN = LinearOutSlowInInterpolator()
```

#### Loading States
```kotlin
sealed class UiState<T> {
    class Loading<T> : UiState<T>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error<T>(val exception: Throwable) : UiState<T>()
    class Empty<T> : UiState<T>()
}
```

### 5.3 Feedback Patterns

#### Visual Feedback
- **Touch Feedback**: Ripple effect on all touchable elements
- **Selection**: Primary color background, elevated shadow
- **Disabled State**: 38% opacity, no interaction
- **Error State**: Red text/border, error message below

#### Haptic Feedback
```kotlin
enum class HapticFeedback {
    LIGHT_IMPACT,    // Button press
    MEDIUM_IMPACT,   // Toggle switch
    HEAVY_IMPACT,    // Delete confirmation
    SELECTION,       // List item selection
    SUCCESS,         // Task completion
    WARNING,         // Approaching limit
    ERROR           // Operation failed
}
```

## 6. Responsive Design

### 6.1 Breakpoints

| Breakpoint | Width | Columns | Gutter | Margin | Layout |
|------------|-------|---------|--------|--------|--------|
| Phone Portrait | 0-599dp | 4 | 16dp | 16dp | Single pane |
| Phone Landscape | 600-719dp | 8 | 16dp | 24dp | Single pane |
| Tablet Portrait | 720-839dp | 8 | 24dp | 24dp | Master-detail |
| Tablet Landscape | 840-1023dp | 12 | 24dp | 24dp | Master-detail |
| Desktop | 1024dp+ | 12 | 24dp | 24dp | Multi-pane |

### 6.2 Adaptive Layouts

```kotlin
@Composable
fun AdaptiveLayout(
    windowSizeClass: WindowSizeClass,
    content: @Composable () -> Unit
) {
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> CompactLayout(content)
        WindowWidthSizeClass.Medium -> MediumLayout(content)
        WindowWidthSizeClass.Expanded -> ExpandedLayout(content)
    }
}
```

### 6.3 Density Buckets

| Density | DPI Range | Scale Factor | Asset Suffix |
|---------|-----------|--------------|--------------|
| ldpi | ~120 dpi | 0.75x | -ldpi |
| mdpi | ~160 dpi | 1.0x (baseline) | -mdpi |
| hdpi | ~240 dpi | 1.5x | -hdpi |
| xhdpi | ~320 dpi | 2.0x | -xhdpi |
| xxhdpi | ~480 dpi | 3.0x | -xxhdpi |
| xxxhdpi | ~640 dpi | 4.0x | -xxxhdpi |

## 7. Accessibility

### 7.1 Content Descriptions

```kotlin
// Image content descriptions
imageView.contentDescription = "POI marker for ${poi.name}, ${poi.category}"

// Action descriptions
button.contentDescription = "Start tracking your location"

// State descriptions
switch.contentDescription = if (isEnabled) {
    "Notifications enabled, tap to disable"
} else {
    "Notifications disabled, tap to enable"
}
```

### 7.2 Touch Targets

- **Minimum Size**: 48dp × 48dp
- **Recommended Size**: 56dp × 56dp for primary actions
- **Spacing**: Minimum 8dp between targets

### 7.3 Color Contrast

| Element | Foreground | Background | Ratio | WCAG Level |
|---------|------------|------------|-------|------------|
| Body Text | #212121 | #FFFFFF | 16:1 | AAA |
| Secondary Text | #616161 | #FFFFFF | 7.5:1 | AAA |
| Primary Button | #FFFFFF | #2196F3 | 4.5:1 | AA |
| Disabled Text | #9E9E9E | #FFFFFF | 4.5:1 | AA |

### 7.4 Screen Reader Support

```kotlin
// Announce changes
view.announceForAccessibility("Track recording started")

// Group related elements
linearLayout.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

// Custom actions
ViewCompat.addAccessibilityAction(
    poiItem,
    "Navigate to POI"
) { _, _ ->
    navigateToPoi(poi)
    true
}
```

## 8. Dark Theme

### 8.1 Dark Color Palette

```scss
// Dark Theme Colors
$dark-primary: #90CAF9;          // Lighter blue
$dark-primary-variant: #42A5F5;  // Accent
$dark-secondary: #FFB74D;        // Orange accent

$dark-background: #121212;       // Main background
$dark-surface: #1E1E1E;         // Cards, sheets
$dark-surface-elevated: #2C2C2C; // Elevated surfaces

$dark-on-primary: #000000;       // Text on primary
$dark-on-secondary: #000000;     // Text on secondary
$dark-on-background: #E0E0E0;    // Text on background
$dark-on-surface: #E0E0E0;       // Text on surface

$dark-error: #CF6679;            // Error color
```

### 8.2 Elevation in Dark Theme

```scss
// Elevation via overlay opacity
$dark-elevation-1: 5%;   // 0.05 opacity white
$dark-elevation-2: 7%;   // 0.07 opacity white
$dark-elevation-3: 8%;   // 0.08 opacity white
$dark-elevation-4: 9%;   // 0.09 opacity white
$dark-elevation-5: 11%;  // 0.11 opacity white
$dark-elevation-6: 12%;  // 0.12 opacity white
```

## 9. Micro-interactions

### 9.1 Button Press

```kotlin
// Scale animation on press
val scaleAnimation = button.animate()
    .scaleX(0.95f)
    .scaleY(0.95f)
    .setDuration(100)
    .setInterpolator(DecelerateInterpolator())
    .withEndAction {
        button.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(100)
            .start()
    }
```

### 9.2 FAB Transform

```kotlin
// Morph FAB to full screen
val circularReveal = ViewAnimationUtils.createCircularReveal(
    expandedView,
    fab.x.toInt() + fab.width / 2,
    fab.y.toInt() + fab.height / 2,
    fab.width / 2f,
    hypot(screenWidth.toDouble(), screenHeight.toDouble()).toFloat()
)
circularReveal.duration = 300
circularReveal.start()
```

### 9.3 List Item Selection

```kotlin
// Stagger animation for list items
recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(
    context,
    R.anim.layout_animation_fall_down
)
```

## 10. Notification Design

### 10.1 Proximity Notification

```
┌─────────────────────────────────────┐
│ 📍 Survey Me                    Now │
├─────────────────────────────────────┤
│ POI Nearby: Restaurant Name         │
│ 50m away · Tap to view on map      │
│                                     │
│ [VIEW]              [DISMISS]       │
└─────────────────────────────────────┘
```

### 10.2 Tracking Notification (Persistent)

```
┌─────────────────────────────────────┐
│ 📍 Survey Me · Tracking Active       │
├─────────────────────────────────────┤
│ Recording: 01:23:45 · 5.2 km        │
│ GPS: Good · Battery: 78%            │
│                                     │
│ [PAUSE]    [STOP]    [OPEN APP]     │
└─────────────────────────────────────┘
```

## 11. Onboarding Flow

### 11.1 Welcome Screen Sequence

```
Screen 1: Welcome
┌─────────────────────────────────┐
│                                 │
│     [App Logo]                  │
│                                 │
│     Welcome to Survey Me        │
│                                 │
│  Your companion for OSM surveys │
│                                 │
│         [GET STARTED]           │
│                                 │
└─────────────────────────────────┘

Screen 2: Permissions
┌─────────────────────────────────┐
│                                 │
│     [Location Icon]             │
│                                 │
│   Location Permission Needed    │
│                                 │
│  Survey Me needs location      │
│  access to track your walks    │
│                                 │
│      [GRANT PERMISSION]         │
│         [Skip]                  │
└─────────────────────────────────┘

Screen 3: Import POIs
┌─────────────────────────────────┐
│                                 │
│     [POI Icon]                  │
│                                 │
│      Import POI Data            │
│                                 │
│  Load GPX files with points    │
│  of interest to survey         │
│                                 │
│      [IMPORT GPX FILE]          │
│      [Do this later]            │
└─────────────────────────────────┘

Screen 4: Ready
┌─────────────────────────────────┐
│                                 │
│     [Checkmark Icon]            │
│                                 │
│      You're All Set!            │
│                                 │
│  Start exploring and mapping   │
│  your neighborhood             │
│                                 │
│      [START MAPPING]            │
│                                 │
└─────────────────────────────────┘
```

## 12. Error States and Empty States

### 12.1 Empty States

```kotlin
data class EmptyState(
    val icon: Int,
    val title: String,
    val description: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null
)

val emptyStates = mapOf(
    "no_pois" to EmptyState(
        icon = R.drawable.ic_empty_pois,
        title = "No POIs loaded",
        description = "Import a GPX file to see points of interest",
        actionLabel = "Import GPX",
        action = { importGpx() }
    ),
    "no_tracks" to EmptyState(
        icon = R.drawable.ic_empty_tracks,
        title = "No tracks recorded",
        description = "Start tracking to record your survey routes",
        actionLabel = "Start Tracking",
        action = { startTracking() }
    )
)
```

### 12.2 Error States

```
┌─────────────────────────────────┐
│                                 │
│     [Error Icon]                │
│                                 │
│    Something went wrong         │
│                                 │
│  Unable to load map tiles.     │
│  Check your connection.        │
│                                 │
│        [RETRY]                  │
│                                 │
└─────────────────────────────────┘
```

## 13. Loading States

### 13.1 Skeleton Screens

```kotlin
@Composable
fun PoiListSkeleton() {
    Column {
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Icon placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            shimmerBrush(),
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    // Title placeholder
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(20.dp)
                            .background(shimmerBrush())
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subtitle placeholder
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                            .background(shimmerBrush())
                    )
                }
            }
        }
    }
}
```

## 14. Map UI Elements

### 14.1 User Location Marker

```kotlin
// Location marker with accuracy circle
class LocationOverlay(context: Context) : Overlay() {
    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (location == null) return

        val point = mapView.projection.toPixels(geoPoint, null)

        // Draw accuracy circle
        val accuracyRadius = mapView.projection.metersToEquatorPixels(accuracy)
        canvas.drawCircle(
            point.x.toFloat(),
            point.y.toFloat(),
            accuracyRadius,
            accuracyPaint
        )

        // Draw location dot
        canvas.drawCircle(
            point.x.toFloat(),
            point.y.toFloat(),
            8.dp,
            locationPaint
        )

        // Draw direction arrow if moving
        if (bearing != null && speed > 0.5) {
            canvas.save()
            canvas.rotate(bearing, point.x.toFloat(), point.y.toFloat())
            canvas.drawPath(arrowPath, directionPaint)
            canvas.restore()
        }
    }
}
```

### 14.2 POI Clustering

```
Zoom < 12: Large clusters
┌──────┐
│  47  │  (Number of POIs)
└──────┘

Zoom 12-15: Small clusters
┌────┐
│ 12 │
└────┘

Zoom > 15: Individual markers
  ◆   (Category-colored diamond)
```

## 15. Platform-Specific Considerations

### 15.1 Android Version Adaptations

| API Level | Version | Special Handling |
|-----------|---------|-----------------|
| 23-25 | 6.0-7.1 | Runtime permissions |
| 26-28 | 8.0-9.0 | Notification channels |
| 29 | 10 | Scoped storage |
| 30 | 11 | One-time permissions |
| 31+ | 12+ | Material You theming |

### 15.2 Device-Specific Optimizations

```kotlin
// Adapt UI for different device classes
when (deviceProfile) {
    DeviceProfile.PHONE_SMALL -> {
        // Reduce padding, smaller text
        contentPadding = 12.dp
        fontSize = 14.sp
    }
    DeviceProfile.PHONE_NORMAL -> {
        // Standard layout
        contentPadding = 16.dp
        fontSize = 16.sp
    }
    DeviceProfile.TABLET -> {
        // Multi-pane layout
        showMasterDetail = true
        contentPadding = 24.dp
    }
    DeviceProfile.FOLDABLE -> {
        // Adapt to folding state
        adaptToFoldingFeature()
    }
}
```

---

*This UI/UX design specification provides comprehensive guidelines for implementing Survey Me's user interface. All designs should prioritize usability during field surveys while maintaining Material Design principles and accessibility standards.*
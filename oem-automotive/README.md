# 🚙 Sample 6: OEM Automotive & Connected Digital Cockpit

This sample demonstrates how to integrate the **Veg Sahayyak SDK** into an **Android Automotive OS (AAOS)** head unit or an aftermarket in-dash landscape tablet.

It showcases a production-grade, **Landscape-only** automotive digital cockpit layout utilizing `:speed-core`, `:speed-ui`, and `:speed-overlay` for high-contrast driver instrument clusters, head-up windshield projection (HUD), and compact telematics overlays that keep drivers informed without distraction.

---

## 🌟 Key Architecture & Capabilities

1. **Dual Driver Instrument Telematics (`:speed-ui`)**:
   - **Night Windshield HUD (`HudDisplay`)**: High-contrast OLED black display with built-in vertical mirror reflection support (`isMirrored = true/false`) for windshield glass reflection.
   - **Digital Cockpit Dial (`SpeedGauge`)**: Fluid analog dial with animated needle, dynamic speed threshold coloration, and custom client branding.
2. **Compact Speed Overlay Widget (`:speed-overlay`)**:
   - **Picture-in-Picture Floating Speedometer (`PipWidget`)**: Integrates directly into infotainment navigation panels or floating windows with dynamic sonar ripple warnings.
3. **Dynamic ADAS Road Profiles (`DrivingProfile`)**:
   - Real-time speed regulation adapting dynamically between Autobahn/Highway (`100 km/h`), Arterial (`80 km/h`), and City (`50 km/h`).
4. **Interactive CAN-Bus / Bench Testing Simulator**:
   - Allows automotive Tier-1 engineers and QA to simulate vehicle CAN-bus telemetry directly at their bench/desk before road trials.

---

## 🛠️ Step-by-Step Integration Guide

### Step 1: Add the Dependencies
```kotlin
// build.gradle.kts
dependencies {
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    
    implementation("com.vegsahayyak:speed-core")
    implementation("com.vegsahayyak:speed-ui")
    implementation("com.vegsahayyak:speed-overlay")
}
```

### Step 2: Lock Orientation to Landscape in AndroidManifest.xml
In-dash automotive head units operate strictly in landscape mode:
```xml
<activity
    android:name=".MainActivity"
    android:screenOrientation="landscape"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### Step 3: Initialize the Telematics Engine
```kotlin
SpeedAssistant.with(context, enterpriseJwt)
    .setDrivingProfile(DrivingProfile.CarHighway)
    .start()
```

### Step 4: Render Windshield HUD or Analog Cockpit Cluster
```kotlin
// Night Windshield HUD with reflection flip support
HudDisplay(
    speed = currentSpeedKmh,
    warningSpeed = 100f,
    isTrackingActive = isTrackingActive,
    isOverLimit = isOverLimit,
    isMirrored = isWindshieldMirrored,
    onToggleMirror = { isWindshieldMirrored = !isWindshieldMirrored },
    gpsAccuracyMeters = gpsAccuracyMeters,
    modifier = Modifier.fillMaxSize()
)

// Or Full Instrument Cockpit Gauge
SpeedGauge(
    speed = currentSpeedKmh,
    warningSpeed = 100f,
    isTrackingActive = isTrackingActive,
    isOverLimit = isOverLimit,
    gpsAccuracyMeters = gpsAccuracyMeters,
    modifier = Modifier.fillMaxSize()
)
```

---
*Explore [MainActivity.kt](src/main/java/com/vegsahayyak/samples/oem/MainActivity.kt) for the complete, runnable automotive cockpit implementation.*

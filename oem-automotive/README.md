# 🚙 OEM Automotive Head Unit Sample

This sample demonstrates how to integrate the Veg Sahayyak SDK into an Android Automotive OS (AAOS) head unit or an aftermarket dashboard tablet.

It focuses on building a driver-centric, **Landscape-only** dashboard layout utilizing the `:speed-ui` module for large, easily readable gauges and speed limit signs that don't distract the driver.

## Step-by-Step Integration Guide

### Step 1: Add the Dependencies
```kotlin
dependencies {
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    
    implementation("com.vegsahayyak:speed-core")
    implementation("com.vegsahayyak:speed-ui")
}
```

### Step 2: Lock Orientation to Landscape
Since this is a head unit dashboard, we lock the `MainActivity` to landscape mode in the `AndroidManifest.xml`:
```xml
<activity
    android:name=".MainActivity"
    android:screenOrientation="landscape"
    ... >
```
*(Note: As always, the Manifest Merger handles all location permissions automatically!)*

### Step 3: Build the Dashboard UI
Use Jetpack Compose `Row` layouts to construct a split-screen dashboard (e.g., Media/Maps on the left, Telematics HUD on the right).

```kotlin
SpeedHUD(
    speedKmh = currentSpeed,
    modifier = Modifier.fillMaxHeight().weight(1f),
    theme = HudTheme.AutomotiveDark
)
```

---
*Explore the `MainActivity.kt` file in this directory to see the complete, runnable code for these steps.*

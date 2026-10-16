# 🚚 Fleet Tracking & Regulatory Telematics Sample

This sample demonstrates how commercial fleet management systems (e.g., heavy logistics, intercity passenger transit, last-mile courier delivery) integrate the **Veg Sahayyak SDK** to enforce statutory speed regulations and capture live telematics.

---

## 🎯 Key B2B Commercial Capabilities

### 1. Dynamic Regulatory Speed Windows (Corridor Enforcement)
Commercial transport vehicles must adhere to both maximum and minimum speed limits:
- **Maximum Warning Limit**: Prevents high-speed rollover hazards and statutory speeding violations.
- **Minimum Highway Warning Limit**: Flags expressway under-speed conditions (e.g., slow vehicles obstructing high-speed freight lanes).

The SDK provides explicit public setters to mathematically inject these limits at runtime:
```kotlin
SpeedAssistant.with(context, enterpriseJwt)
    .setWarningSpeedThreshold(80.0f)   // Upper statutory limit (km/h)
    .setMinWarningSpeedThreshold(20.0f) // Minimum expressway speed (km/h)
    .start()
```

### 2. Live Reactive Telematics Stream
The UI observes the SDK's reactive `SpeedData` flow to monitor real-time speed, GPS horizontal accuracy, and compliance violations:
```kotlin
val speedState by SpeedAssistant.observeFlow()
    .collectAsState(initial = SpeedData.SpeedUnavailable)

when (val state = speedState) {
    is SpeedData.SpeedChanged -> {
        val speedKmh = state.speedMps * 3.6f
        val isOver = state.isOverLimit       // True if speed > upper limit
        val isUnder = state.isUnderLimit     // True if speed < lower limit
        val accuracy = state.accuracyMeters  // Horizontal GPS accuracy in meters
    }
    is SpeedData.LowAccuracy -> { /* Handle weak GPS signal */ }
    is SpeedData.GPSDisabled -> { /* Prompt driver to enable location */ }
    is SpeedData.SpeedUnavailable -> { /* Awaiting satellite fix */ }
}
```

### 3. Autonomous Telematics Engine & Enterprise Privacy Mode
- **Autonomous Sync**: When started, the SDK automatically schedules `TelemetryJobService` via Android's native `JobScheduler` (zero third-party networking or database dependencies required).
- **Enterprise Opt-Out / Privacy Mode**: For enterprise fleets that manage telemetry independently and require complete driver privacy, the SDK checks the commercial JWT for the `"unlimited": true` claim. When detected, local telemetry logging is short-circuited at the C++ core layer, eliminating disk I/O and network overhead.

---

## 🚀 Step-by-Step Integration Guide

### Step 1: Add SDK Dependencies
In your module's `build.gradle.kts`:
```kotlin
dependencies {
    // Veg Sahayyak SDK BOM and Core Module
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    implementation("com.vegsahayyak:speed-core")

    // Jetpack Compose & AndroidX
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.material3:material3")
}
```

### Step 2: Declare Permissions
Ensure fine and coarse location permissions are declared in `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### Step 3: Initialize & Enforce Fleet Limits
```kotlin
// Heavy Truck Configuration Example
val truckMaxSpeedKmh = 80.0f
val truckMinSpeedKmh = 20.0f

SpeedAssistant.with(context, fleetEnterpriseJwt)
    .setWarningSpeedThreshold(truckMaxSpeedKmh)
    .setMinWarningSpeedThreshold(truckMinSpeedKmh)
    .start()
```

### Step 4: Dynamically Switch Vehicle Profiles
When the vehicle type changes (e.g., driver re-assigned from a truck to a delivery van), update the active limits dynamically without restarting the service:
```kotlin
SpeedAssistant.with(context)
    .setWarningSpeedThreshold(newProfile.maxKmh)
    .setMinWarningSpeedThreshold(newProfile.minKmh)
```

---

## 📱 Running & Testing the Sample

1. Connect an Android device with USB debugging enabled:
   ```bash
   adb devices
   ```
2. Build and install the debug APK:
   ```bash
   ./gradlew :fleet-tracking:installDebug
   ```
3. Launch the app and grant location permissions:
   - Toggle between **Heavy Truck (20 - 80 km/h)**, **Intercity Coach (30 - 90 km/h)**, and **Delivery Van (10 - 60 km/h)**.
   - Observe the dynamic regulatory corridor bar and the real-time compliance badge.

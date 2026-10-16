# 🟢 Veg Sahayyak SDK - Basic Integration Sample

This sample demonstrates the foundational, straightforward integration of the **Veg Sahayyak B2B Telematics SDK** (`:speed-core`) into an Android application using modern Jetpack Compose.

---

## 🎯 What This Sample Demonstrates
1. **Cryptographic Engine Unlock**: Initializing the SDK with a commercial RSA-2048 signed JWT token (`SpeedAssistant.with(context, jwt)`).
2. **Foreground Speed Tracking**: Launching high-frequency, low-latency GPS speed monitoring.
3. **Reactive Flow Observation**: Listening to the type-safe Kotlin Flow (`SpeedAssistant.observeFlow()`) to render live speed and overspeed alerts.
4. **Dynamic Driving Profiles**: Adjusting overspeed thresholds on the fly using `DrivingProfile` presets (`CarCity`, `CarHighway`, `SchoolZone`).
5. **Clean SDK Architecture**: 100% decoupled from any proprietary application resources—uses the official Veg Sahayyak logo and clean Material 3 design tokens.

---

## 📋 System Requirements
- **Android OS**: Android 7.0+ (API Level 24+)
- **Compile SDK**: API 34+ (37 recommended)
- **Kotlin**: 2.0+ with Compose Compiler plugin
- **GPS Hardware**: Location services enabled

---

## 🛠️ Step-by-Step Integration Guide

### Step 1: Add the SDK Dependencies
Add the BOM and core telematics library to your app-level `build.gradle.kts`:

```kotlin
dependencies {
    // 1. Bill of Materials (BOM) to align SDK versioning
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    
    // 2. Veg Sahayyak Core Telematics Engine
    implementation("com.vegsahayyak:speed-core")

    // 3. Jetpack Compose UI
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
```

### Step 2: Declare Permissions in Manifest
Declare standard location permissions in `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
    <application
        android:icon="@drawable/ic_veg_sahayyak_logo"
        android:label="Your App Name">
        ...
    </application>
</manifest>
```

### Step 3: Request Runtime Permissions
Before starting the telematics engine, request user approval at runtime:

```kotlin
val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (fineGranted || coarseGranted) {
        startTelematics()
    }
}
```

### Step 4: Unlock the Vault & Start Telematics
Initialize `SpeedAssistant` by passing your commercial JWT token. This performs local RSA-2048 cryptographic validation, validates package binding, and unlocks the high-performance C++ math engine:

```kotlin
// Initialize and start foreground GPS monitoring
SpeedAssistant.with(context, enterpriseJwt)
    .setDrivingProfile(DrivingProfile.CarCity) // Sets 80 km/h threshold with native hysteresis
    .start()
```

### Step 5: Consume Live Speed in Compose
Collect the live speed state via `SpeedAssistant.observeFlow()`:

```kotlin
@Composable
fun SpeedDisplay() {
    val speedState by SpeedAssistant.observeFlow()
        .collectAsState(initial = SpeedData.SpeedUnavailable)

    val speedKmh = when (val state = speedState) {
        is SpeedData.SpeedChanged -> state.speedMps * 3.6f
        else -> 0f
    }
    val isOverLimit = (speedState as? SpeedData.SpeedChanged)?.isOverLimit ?: false

    Text(
        text = "${"%.1f".format(speedKmh)} km/h",
        color = if (isOverLimit) Color.Red else Color.Cyan
    )
}
```

### Step 6: Handling SDK States
The `SpeedData` sealed class provides exhaustive states for all GPS and hardware conditions:

| `SpeedData` State | Description |
| :--- | :--- |
| `SpeedData.SpeedChanged(speedMps, isOverLimit, isUnderLimit)` | Live speed update with calculated overspeed/underspeed limits. |
| `SpeedData.LowAccuracy` | Weak satellite reception (e.g. tunnels or indoor parking). |
| `SpeedData.GPSDisabled` | Device location services are toggled off by the user. |
| `SpeedData.SpeedUnavailable` | Telematics service is active and acquiring satellite lock. |
| `SpeedData.Disabled` | Telematics engine is stopped. |

---

## 🧪 Running & Verifying Locally
1. Connect an Android device or emulator running Android 7.0 (API 24) or newer.
2. Select the `basic-integration` configuration in Android Studio and click **Run**.
3. Accept the location permission prompt.
4. Verify that live speed is observed and the foreground telematics notification appears in the status bar.

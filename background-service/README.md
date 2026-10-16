# 👻 Headless Background Service & AutoStart Sample

This sample demonstrates how to run the **Veg Sahayyak SDK** entirely in the background as a headless foreground service with persistent notifications and automatic vehicle motion detection.

It combines the core telematic tracking engine (`:speed-core`) with the Google Play Services Activity Recognition plugin (`:speed-autostart`) to autonomously wake up the telematics engine when the user starts driving, without requiring manual app interaction.

---

## 🎯 Key Architectural Concepts

### 1. Zero-Touch Automatic In-Vehicle Wakeup (`:speed-autostart`)
The `:speed-autostart` module registers for Google Play Services Activity Recognition transitions:
- When the OS detects `DetectedActivity.IN_VEHICLE` with transition `ACTIVITY_TRANSITION_ENTER`, `AutoStartReceiver` catches the broadcast and invokes `SpeedAssistant.with(context).start()`.
- When the user exits the vehicle (`ACTIVITY_TRANSITION_EXIT`), `AutoStartReceiver` invokes `SpeedAssistant.with(context).stop()` to halt GPS tracking and conserve battery.

### 2. Commercial Token Caching
Because background broadcast receivers wake up without an active Activity or UI, the initial call to:
```kotlin
SpeedAssistant.with(context, enterpriseJwt)
```
cryptographically caches the JWT token into local secure preferences (`SpeedAssistantPrefs`). Subsequent background calls can simply invoke `SpeedAssistant.with(context).start()`.

### 3. Persistent Foreground Notification
When active, `SpeedometerService` displays a persistent notification with the vehicle's live speed and compliance state. This guarantees that Android's Low Memory Killer (LMK) will not terminate the tracking process while driving.

### 4. Screen-Off Continuous GPS Polling
By default, the SDK can halt GPS polling when the screen locks to conserve power. For enterprise dispatch and headless telematics, continuous tracking can be enabled via:
```kotlin
SpeedAssistant.with(context).preferences.setUpdateWhileScreenOff(true)
```

---

## 🚀 Step-by-Step Integration Guide

### Step 1: Add Dependencies
In your module's `build.gradle.kts`:
```kotlin
dependencies {
    // Veg Sahayyak SDK BOM, Core Engine, and AutoStart Plugin
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    implementation("com.vegsahayyak:speed-core")
    implementation("com.vegsahayyak:speed-autostart")

    // Jetpack Compose & AndroidX
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.material3:material3")
}
```

### Step 2: Declare Permissions
In `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
*(Note: A Foreground Service with `foregroundServiceType="location"` does **not** require `ACCESS_BACKGROUND_LOCATION` on Android 10+).*

### Step 3: Initialize the SDK & Arm AutoStart
In your `Application.onCreate()` or launcher Activity:
```kotlin
// 1. Initialize SDK with enterprise JWT token
SpeedAssistant.with(context, enterpriseJwt)
    .setDrivingProfile(DrivingProfile.CarHighway)

// 2. Arm Activity Recognition
AutoStartManager.enable(
    context = context,
    onPermissionRequired = {
        // Request Manifest.permission.ACTIVITY_RECOGNITION at runtime on Android 10+
    },
    onSuccess = {
        // Armed: AutoStartReceiver will autonomously handle IN_VEHICLE transitions
    },
    onError = { exception ->
        // Handle Play Services availability issues
    }
)
```

### Step 4: Manual Simulation (Desk Testing)
During development and QA testing, you can manually simulate driving events without needing a vehicle:
```kotlin
// Simulate vehicle entry (starts SpeedometerService & notification)
SpeedAssistant.with(context).start()

// Simulate vehicle exit (stops SpeedometerService & notification)
SpeedAssistant.with(context).stop()
```

---

## 📱 Running & Testing the Sample

1. Connect your Android device with USB debugging enabled:
   ```bash
   adb devices
   ```
2. Build and install the APK:
   ```bash
   ./gradlew :background-service:installDebug
   ```
3. Open the app and grant runtime permissions:
   - Notice the status badge reads **`ARMED`**.
   - Use **`Simulate Entry (Start Service)`** to launch the foreground service and inspect the persistent notification in your Android notification shade.
   - Use **`Simulate Exit (Stop Service)`** to halt the background service.
   - Toggle **`Screen-Off Continuous GPS`** to see how background tracking behaves when the device screen is locked.

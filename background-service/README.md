# 👻 Headless Background Service Sample

This sample demonstrates how to run the Veg Sahayyak SDK entirely in the background without any visible UI. It utilizes Google Play Services Activity Recognition (`:speed-autostart` module) to autonomously detect when the user starts driving and wakes up the telematics engine.

This integration is ideal for logistics companies, delivery drivers, or gig-economy apps where the driver is primarily looking at Google Maps or Waze, but you still need to log their speed and trigger audio warnings.

## Step-by-Step Integration Guide

### Step 1: Add the Dependencies
You must import both the Core SDK and the Autostart plugin.

```kotlin
dependencies {
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    
    implementation("com.vegsahayyak:speed-core")
    implementation("com.vegsahayyak:speed-autostart") // Play Services integration
}
```

### Step 2: Request Background Runtime Permissions
Because this module wakes up the SDK while the app is in the background, you must explicitly request `ACCESS_BACKGROUND_LOCATION` and `ACTIVITY_RECOGNITION` dynamically at runtime on Android 10+. 

*(Note: You do not need to manually add these to your `AndroidManifest.xml`, as the `:speed-autostart` module automatically injects them via Manifest Merger).*

### Step 3: Register the AutoStart Manager
In your Application class, initialize the `AutoStartManager`. You do not need to call `start()` manually. The manager will automatically invoke it when driving is detected.

```kotlin
val token = "eyJhbGciOiJSUzI1NiIs..." 

// 1. Initialize the base SDK but DO NOT call .start()
SpeedAssistant.with(context, token)
    .setDrivingProfile(DrivingProfile.CarHighway)

// 2. Register the Activity Recognition listener
AutoStartManager.registerDriveDetection(context)
```

### Step 4: Handle Audio/Haptic Warnings
Since there is no visual UI, the Core SDK will automatically fire its configured Android Notifications, SOS vibration patterns, and Text-to-Speech (TTS) voice warnings (e.g., "Speed Limit Exceeded") to alert the driver while they are looking at another navigation app.

### Step 5: How to Validate AutoStart
Since the SDK relies on Google Play Services Activity Recognition, testing this at your desk can be tricky. You have two ways to validate it works:

1. **Physical Test:** Install the app on a physical device, open it once to grant permissions, close it, and take a drive. Check your Logcat later for `VegSahayyak` to see the background wake-up logs.
2. **Android Emulator (Instant Test):**
   * Open your Android Emulator's **Extended Controls** (the three dots `...` on the side menu).
   * Go to **Virtual sensors** -> **Additional sensors**.
   * Under **Activity Recognition**, change the state to `In Vehicle`.
   * The `AutoStartManager` will immediately detect this and wake up the SDK!

---
*Explore the `MainActivity.kt` file in this directory to see the complete, runnable code for these steps.*

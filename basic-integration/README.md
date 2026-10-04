# 🟢 Basic Integration Sample

This sample demonstrates the absolute minimum code required to initialize the Veg Sahayyak SDK and start tracking location and speed in the foreground.

## Step-by-Step Integration Guide

### Step 1: Add the Dependencies
Ensure your root `settings.gradle.kts` has the Veg Sahayyak GitHub Packages repository configured. Then, in your app-level `build.gradle.kts`, add the Core SDK:

```kotlin
dependencies {
    // 1. Align versions across modules
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    
    // 2. Import the mandatory core engine
    implementation("com.vegsahayyak:speed-core")
}
```

### Step 2: Request Runtime Permissions
Thanks to the Android **Manifest Merger**, you do not need to declare any `<uses-permission>` tags in your `AndroidManifest.xml`—the SDK automatically injects them for you! 

However, you must still request location permissions dynamically from the user at runtime in your Kotlin code before starting the SDK.

### Step 3: Initialize the Vault
In your Application class or main Entry Point (like `MainActivity.kt`), inject your enterprise JWT token to unlock the Native C++ Vault and start tracking:

```kotlin
val token = "eyJhbGciOiJSUzI1NiIs..." // Your enterprise license token

SpeedAssistant.with(context, token)
    .setDrivingProfile(DrivingProfile.CarCity)
    .start()
```

### Step 4: Observe Real-Time Data (Optional)
If you want to draw your own UI instead of using the `:speed-ui` module, you can directly collect the raw speed data flow from the core engine:

```kotlin
lifecycleScope.launch {
    SpeedCoreInternal.speedRepository?.observeFlow()?.collect { speedData ->
        println("Current Speed: ${speedData.speedKmh} km/h")
        println("Is Speeding: ${speedData.isSpeeding}")
    }
}
```

---
*Explore the `MainActivity.kt` file in this directory to see the complete, runnable code for these steps.*

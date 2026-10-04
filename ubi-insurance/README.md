# 🛡️ UBI (Usage-Based Insurance) Sample

This sample demonstrates how to configure the Veg Sahayyak SDK for Usage-Based Insurance applications (Pay-How-You-Drive). It focuses on tracking speeding violation durations to calculate driver risk, secured by the Native C++ Vault to prevent spoofing.

## Step-by-Step Integration Guide

### Step 1: Add the Dependencies
```kotlin
dependencies {
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    implementation("com.vegsahayyak:speed-core")
}
```

### Step 2: Configure the UBI Driving Profile
To properly assess risk, insurance apps must track urban driving behaviors where accidents are most frequent. We use the `DrivingProfile.CarCity` preset, which aggressively tunes the Exponential Moving Average (EMA).

```kotlin
val token = "eyJhbGciOiJSUzI1NiIs..." // Your insurance commercial token

SpeedAssistant.with(context, token)
    .setDrivingProfile(DrivingProfile.CarCity)
    .start()
```

### Step 3: Track Speeding Duration for Risk Scoring
In a UBI app, you can build a risk profile by calculating how long a driver stays above the speed limit (duration of violation).

```kotlin
lifecycleScope.launch {
    SpeedCoreInternal.speedRepository?.observeFlow()?.collect { speedData ->
        if (speedData.isSpeeding) {
            println("🚨 Speeding violation active!")
            InsuranceBackend.incrementRiskPenalty()
        }
    }
}
```

### Step 4: Security & Anti-Spoofing
UBI applications are massive targets for GPS-spoofing and app tampering (users trying to lower their insurance premiums by injecting fake, safe driving data). 

The Veg Sahayyak SDK mitigates this autonomously:
1. **Dalvik Hooking Protection:** The SDK actively verifies the JVM state via `isNativeLoaded`. If a user attempts to hook the JVM to spoof the GPS input, the C++ Vault intentionally throws a fatal `UnsatisfiedLinkError` and crashes the app to prevent fraud.
2. **Cryptographic Validation:** Ensure your backend only accepts telemetry payloads that have been signed by the SDK's C++ Vault key.

---
*Explore the `MainActivity.kt` file in this directory to see the complete, runnable code for these steps.*

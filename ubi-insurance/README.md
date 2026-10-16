# 🛡️ Sample 5: Usage-Based Insurance (UBI) & Risk Profiling

This sample demonstrates how insurance carriers, underwriters, and insurtech platforms integrate the Veg Sahayyak SDK (`:speed-core`) to implement **Pay-How-You-Drive (PHYD)** policies. By converting real-time GPS telemetry into dynamic driving risk scores, insurers can reward safe policyholders with premium discounts while mitigating claim risks.

---

## 📦 What `:speed-core` Delivers for Insurers

| Feature | Actuarial & Technical Value |
| :--- | :--- |
| **Real-Time Speed Compliance** | Sub-second detection of speeding events (`isOverLimit`) mapped against statutory territory limits. |
| **Dynamic Driving Profiles** | Context-aware risk thresholds (`DrivingProfile.CarCity` 50 km/h, `DrivingProfile.SchoolZone` 30 km/h, `DrivingProfile.CarHighway` 120 km/h). |
| **Native C++ Anti-Spoofing Vault** | Dalvik/ART hooking detection and atomic GPS time high-water mark validation to prevent policyholders from falsifying driving records. |
| **Battery-Optimized Engine** | Zero unnecessary disk I/O under enterprise unlimited licenses, ensuring seamless background tracking without draining the policyholder's battery. |

---

## 🚀 Step-by-Step Integration Guide

### Step 1: Add Dependencies

Add the BOM and Core telematics modules to `build.gradle.kts`:

```kotlin
dependencies {
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    implementation("com.vegsahayyak:speed-core")
}
```

### Step 2: Initialize with the Insurance Commercial Token

Authenticate with your RSA-2048 enterprise JWT license key to unlock the Native C++ vault:

```kotlin
val insuranceJwt = "eyJhbGciOiJSUzI1NiIs..." // Your commercial token

SpeedAssistant.with(context, insuranceJwt)
    .setDrivingProfile(DrivingProfile.CarCity)
    .start()
```

### Step 3: Consume Live Telematics via `SpeedAssistant.observeFlow()`

Observe the public telematics flow to track speed compliance and compute safety scores:

```kotlin
val speedState by SpeedAssistant.observeFlow()
    .collectAsState(initial = SpeedData.SpeedUnavailable)

val currentSpeedKmh = when (val state = speedState) {
    is SpeedData.SpeedChanged -> state.speedMps * 3.6f
    else -> 0f
}
val isSpeeding = (speedState as? SpeedData.SpeedChanged)?.isOverLimit ?: false

// Actuarial penalty calculation
if (isSpeeding) {
    deductSafetyPoints(severity = currentSpeedKmh - speedLimit)
}
```

### Step 4: Dynamic Territory / Zone Profile Switching

When policyholders navigate between municipal zones, adjust the active driving profile dynamically:

```kotlin
// Urban streets (50 km/h statutory speed limit)
SpeedAssistant.with(context).setDrivingProfile(DrivingProfile.CarCity)

// High-risk school and residential quiet zones (30 km/h limit)
SpeedAssistant.with(context).setDrivingProfile(DrivingProfile.SchoolZone)

// Expressways (120 km/h statutory highway limit)
SpeedAssistant.with(context).setDrivingProfile(DrivingProfile.CarHighway)
```

---

## 🔒 Security & Anti-Fraud Protection

Insurance telematics is a prime target for GPS spoofing and app tampering (users attempting to lower premiums by feeding fabricated safe routes into the app). 

The Veg Sahayyak SDK protects underwriters through multi-layered on-device cryptographic verification:
1. **Dalvik/ART Hooking Defense:** The Native C++ Vault checks low-level execution context. If instrumentation frameworks (e.g. Frida, Xposed) attempt to hook location providers, the vault halts execution.
2. **GPS High-Water Mark Validation:** Verifies atomic timestamps against hardware satellite clocks, detecting artificial playback of pre-recorded trips or mocked system times.

---

## 📱 Live Demonstration Features in this Sample

When running this sample on device, you can test:
1. **Actuarial Safety Score (0–100):** Real-time score that degrades during overspeed events and gradually recovers during safe driving.
2. **4 Premium Policy Tiers:** Dynamic discount status from **Tier 1 (25% Discount)** down to **Tier 4 (+15% Risk Surcharge)**.
3. **Statutory Zone Switcher:** Switch between **Urban City (50 km/h)**, **School Zone (30 km/h)**, and **Highway (120 km/h)** to see how threshold changes affect compliance.
4. **Desk Testing Simulator:** Quick speed buttons (`0 km/h [Idle]`, `42 km/h [Safe]`, `58 km/h [Minor Violation]`, `78 km/h [Severe Violation]`) plus a smooth slider to test risk degradation right at your desk without driving.

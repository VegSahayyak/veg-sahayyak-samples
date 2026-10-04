# 🚚 Fleet Tracking & Telemetry Sample

This sample demonstrates how to integrate the Veg Sahayyak SDK for a B2B fleet management application (e.g., delivery trucks, logistics, taxis). It showcases setting custom maximum and minimum speed boundaries and utilizing the **Offline-First Telemetry Engine** to securely sync violations to your backend.

## Step-by-Step Integration Guide

### Step 1: Add the Dependencies
Import both the Core SDK and ensure your BOM is aligned to the latest version.

```kotlin
dependencies {
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    implementation("com.vegsahayyak:speed-core")
}
```

### Step 2: Configure Fleet Thresholds
Unlike regular consumer apps, commercial vehicles often have strict regulatory limits. You can bypass the standard `DrivingProfile` presets and mathematically inject your own thresholds:

```kotlin
// Example: Heavy Truck configuration
val truckMaxSpeedKmh = 80.0 // Regulatory maximum speed for trucks
val truckMinSpeedKmh = 20.0 // Minimum speed on expressways

SpeedAssistant.with(context, token)
    .setWarningSpeedThreshold(truckMaxSpeedKmh)
    .setMinWarningSpeedThreshold(truckMinSpeedKmh)
    .start()
```

### Step 3: Enable Background Sync
The SDK's Native C++ Vault securely caches speeding violations during network dropouts (e.g., when the truck drives through a rural area without cellular service). 

To automatically wake up your app and sync this data to a PostgreSQL/Supabase backend every 4 hours, ensure your token supports telemetry and simply start the core. The SDK automatically schedules a `TelemetryJobService` in the background.

```kotlin
// Monitor the offline caching status (Optional, for debugging)
lifecycleScope.launch {
    SpeedCoreInternal.telemetryManager?.observeUnsyncedCount()?.collect { count ->
        println("Violations pending sync: $count")
    }
}
```

### Step 4: Enterprise Opt-Out
If a specific premium B2B client requests absolute privacy (zero telemetry logged or sent), you must issue them an Enterprise JWT containing the `"unlimited": true` claim.
When the SDK detects this token, it short-circuits the `TelemetryManager` at the C++ layer. No code changes are required in this sample app to support this; it is handled autonomously by the Native Vault.

---
*Explore the `MainActivity.kt` file in this directory to see the complete, runnable code for these steps.*

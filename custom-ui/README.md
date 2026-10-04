# 🎨 Custom UI (White-Label Branding) Sample

This sample demonstrates how to leverage the `:speed-ui` module. Instead of building your own speedometer from scratch, you can drop in our highly optimized Jetpack Compose components and aggressively theme them to perfectly match your brand's design language.

## Step-by-Step Integration Guide

### Step 1: Add the Dependencies
You need the Core SDK for data and the UI module for the visual components.

```kotlin
dependencies {
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    
    implementation("com.vegsahayyak:speed-core")
    implementation("com.vegsahayyak:speed-ui")
}
```

### Step 2: Request Runtime Permissions
As always, the SDK Manifest Merger handles the XML boilerplate. You only need to request `ACCESS_FINE_LOCATION` dynamically in your Activity before initializing the SDK.

### Step 3: Use the UI Components
The SDK provides several drop-in Compose components like `SpeedGauge`, `SpeedLimitSign`, and `MiniHud`. You can easily pass in your brand's colors and typography.

```kotlin
SpeedGauge(
    speedKmh = currentSpeed,
    modifier = Modifier.size(300.dp),
    gaugeColor = BrandColors.NeonGreen,
    warningColor = BrandColors.AlertRed,
    textStyle = BrandTypography.DigitalDisplay
)
```

---
*Explore the `MainActivity.kt` file in this directory to see the complete, runnable code for these steps.*

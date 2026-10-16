# 🎨 Sample 4: Custom UI (White-Label Branding)

This sample demonstrates how enterprise telematics and automotive partners leverage the `:speed-ui` module to build custom-branded speedometer experiences. Instead of writing custom canvas rendering and animation mathematics from scratch, partners can drop in pre-built, production-tested Jetpack Compose components and theme them to match their corporate brand identity.

---

## 📦 What `:speed-ui` Delivers

The `:speed-ui` module is fully decoupled from backend telematics logic and provides:

| Component | Description |
| :--- | :--- |
| **`SpeedGauge`** | Stateless, animated analog dial gauge with smooth needle physics, multi-stop dynamic arc coloring, GPS accuracy indicator, and metric cycler (`km/h`, `mph`, `m/s`). |
| **`HudDisplay`** | Dedicated OLED pure-black (`#000000`) Head-Up Display designed for night windshield reflection, featuring massive typography, warning limits, and vertical mirror flipping (`isMirrored`). |
| **`SpeedUiColorScheme`** | Theme specification controlling safe cruising color, overspeed redline, warning approach glow, needle color, and dial track styling. |
| **`ThemeMode`** | Preset lighting environments: `SYSTEM`, `DARK_GRAY`, `OLED_BLACK`, `HIGH_CONTRAST`, and `HUD_MIRRORED`. |
| **`VoiceCommandButton`** | Floating mic action button that captures driver voice commands (e.g. setting speed limits hands-free) and parses structured `VoiceAction` intents. |

---

## 🚀 Step-by-Step Integration Guide

### Step 1: Add Dependencies

Add the BOM, Core telematics, and UI modules to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    implementation("com.vegsahayyak:speed-core")
    implementation("com.vegsahayyak:speed-ui")
}
```

### Step 2: Define Your Brand's `SpeedUiColorScheme`

Define a custom color scheme matching your client brand design system:

```kotlin
val cyberpunkScheme = SpeedUiColorScheme(
    safeColor = Color(0xFF00FFCC),             // Neon Cyan
    overSpeedColor = Color(0xFFFF0055),        // Electric Crimson
    warningApproachColor = Color(0xFFFFB800),  // Vivid Amber
    underSpeedColor = Color(0xFF00B0FF),       // Sky Blue
    warningGlowColor = Color(0xFF00FFCC),      // Glow halo
    needleColor = Color(0xFFBD00FF),           // Vivid Purple needle
    inactiveTrackColor = Color(0xFF232A38),    // Unlit track background
    labelLight = Color(0xFFF0F6FC),            // High-legibility dial text
    labelDark = Color(0xFF0D1117)
)
```

### Step 3: Embed the Analog Speedometer (`SpeedGauge`)

Drop `SpeedGauge` into any Compose layout. Pass your custom `colorScheme` and stream speed data from `SpeedAssistant`:

```kotlin
val speedState by SpeedAssistant.observeFlow()
    .collectAsState(initial = SpeedData.SpeedUnavailable)

val currentSpeedKmh = when (val state = speedState) {
    is SpeedData.SpeedChanged -> state.speedMps * 3.6f
    else -> 0f
}

SpeedGauge(
    speed = currentSpeedKmh,
    warningSpeed = 80f,
    isTrackingActive = speedState is SpeedData.SpeedChanged,
    isOverLimit = currentSpeedKmh > 80f,
    modifier = Modifier.fillMaxWidth().height(340.dp),
    colorScheme = cyberpunkScheme
)
```

### Step 4: Embed the Night Windshield HUD (`HudDisplay`)

For night driving or projection onto the vehicle windshield, toggle into HUD mode:

```kotlin
var isMirrored by remember { mutableStateOf(false) }

HudDisplay(
    speed = currentSpeedKmh,
    warningSpeed = 80f,
    isTrackingActive = true,
    isOverLimit = currentSpeedKmh > 80f,
    isMirrored = isMirrored,
    onToggleMirror = { isMirrored = !isMirrored },
    modifier = Modifier.fillMaxWidth().height(340.dp),
    colorScheme = cyberpunkScheme
)
```

---

## 📱 Live Demonstration Features in this Sample

When running this sample on device, you can test:
1. **Brand Palette Switching:** Toggle in real-time between **Cyberpunk Neon**, **Racing Track**, and **Luxury Gold**.
2. **Component Selector:** Switch between the **Analog Dial Gauge** and **Night Windshield HUD**.
3. **Desk Testing Speed Simulator:** Use the quick preset buttons (`0 km/h [Idle]`, `55 km/h [Safe]`, `76 km/h [Approaching Limit]`, `98 km/h [Overspeed Alert]`) or the slider to test gauge animations, needle physics, and color transitions right at your desk without driving.

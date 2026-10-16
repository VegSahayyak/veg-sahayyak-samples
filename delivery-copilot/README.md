# Delivery Co-Pilot

A production-grade, two-wheeler last-mile telematics and speed safety assistant designed for delivery pilots and quick-commerce motorcycle riders (food, grocery, parcel couriers).

This sample demonstrates how logistics enterprises, delivery fleets, and gig platforms integrate the **Veg Sahayyak Android SDK** to protect two-wheeler riders from overspeeding accidents while maintaining full productivity on the road.

---

## Key Features & Safety Measures

### 1. Hands-Free Voice Commands (`:speed-ui`)
Motorcycle and scooter riders wear gloves and must keep both hands firmly on the handlebars. Delivery Co-Pilot integrates `VoiceCommandButton` and a custom `DeliveryVoiceCommandParser` to allow hands-free voice control:
* *"Set limit to 40"* — Updates maximum speed threshold.
* *"Monsoon mode"* / *"Rain mode"* — Immediately lowers speed limit to 30 km/h with wet-road braking caution.
* *"Flyover mode"* / *"Bypass mode"* — Elevates limit to 65 km/h for expressway corridors.
* *"Urban mode"* — Resets to 45 km/h for dense city traffic.

### 2. Floating PiP Navigation Overlay (`:speed-overlay`)
Riders navigate using Google Maps, Waze, or delivery partner apps full-screen. 
* Uses `com.vegsahayyak.speedoverlay.OverlayService` and `PipWidget` to draw a compact, draggable speed bubble over any app.
* Features animated peripheral sonar waves that alert the rider of overspeeding without requiring them to stare at the screen.

### 3. Portrait-Only Ergonomics & Low-Glare Palette
* **Portrait Lock:** Strictly optimized for vertical motorcycle handlebar mounts (`android:screenOrientation="portrait"`).
* **Glare-Resistant Palette:** Subtle, matte carbon backgrounds (`#0B0F17`) and high-glance aviation contrast (Aviation Sky `#0EA5E9`, Safe Mint `#10B981`, Amber `#F59E0B`, Crimson `#EF4444`) readable in harsh sunlight within a 200ms glance.

### 4. Rider Shift & Fatigue Safety Monitor
* Tracks active shift duration.
* Provides proactive rest stop recommendations (e.g., 10-minute hydration break after 2 hours of continuous riding).
* Displays live safe-riding compliance index and incident counts.

### 5. Bench Simulator for Desk Evaluation
* Simulates motorcycle throttle (0–90 km/h) and delivery presets (`Idle 0`, `Pickup 20`, `Cruise 42`, `Rush 58`).
* Tests GPS accuracy states (`±3m`, `±12m`, `±30m`).

---

## SDK Modules Used

```kotlin
dependencies {
    implementation(platform("com.vegsahayyak:speed-bom:2.2.0"))
    implementation("com.vegsahayyak:speed-core")
    implementation("com.vegsahayyak:speed-ui")
    implementation("com.vegsahayyak:speed-overlay")
}
```

---

## License & Verification

The sample is authenticated via a signed commercial license token authorized for:
```json
{
  "client_id": "Veg Sahayyak Sample - Delivery Co-Pilot",
  "package_name": "com.vegsahayyak.samples.delivery",
  "modules": ["speed-core", "speed-ui", "speed-overlay"]
}
```

# 🚀 Veg Sahayyak SDK - Official Integration Samples

Welcome to the official repository of sample applications for the **Veg Sahayyak SDK**. These examples demonstrate how to seamlessly integrate our enterprise-grade telematics, speed tracking, and background services into various Android use-cases.

## 🗂️ Sample Applications

This is a single Android Studio project containing multiple "App Modules". Explore the modules below to find the integration pattern that matches your product:

| App Module | Description | Module Requirements |
| :--- | :--- | :--- |
| [📁 **basic-integration**](./basic-integration/) | The simplest possible implementation. Shows how to initialize the SDK, inject your commercial JWT, and start tracking speed in the foreground. | `:speed-core` |
| [📁 **fleet-tracking**](./fleet-tracking/) | Demonstrates a B2B fleet use-case. Focuses on setting custom max/min thresholds for trucks, utilizing the Offline-First Telemetry Engine, and Supabase cloud sync. | `:speed-core` |
| [📁 **ubi-insurance**](./ubi-insurance/) | Shows how to configure the SDK for Usage-Based Insurance (UBI). Uses `DrivingProfile.City` and logs extreme acceleration/braking events safely via the Native C++ Vault. | `:speed-core` |
| [📁 **background-service**](./background-service/) | A zero-touch implementation. Uses Google Play Activity Recognition to auto-start the SDK the moment the driver begins moving, without opening the app. | `:speed-core`, `:speed-autostart` |
| [📁 **custom-ui**](./custom-ui/) | A highly visual sample. Demonstrates injecting corporate brand colors via `SpeedUiColorScheme`, rendering the `SpeedGauge` composable, and enabling OLED HUD Mirroring. | `:speed-core`, `:speed-ui` |
| [📁 **oem-automotive**](./oem-automotive/) | An advanced integration utilizing the Picture-in-Picture (PiP) Overlay widget floating over Google Maps or custom navigation systems. | `:speed-core`, `:speed-overlay` |

## 🛠️ How to Run
1. Clone this repository.
2. Open Android Studio -> **Open an Existing Project** and select this root folder.
3. Add your GitHub Packages Read-Only PAT to `local.properties`:
   ```properties
   gpr.user=YOUR_GITHUB_USERNAME
   gpr.key=ghp_YOUR_READ_ONLY_TOKEN
   ```
4. In the Android Studio run configurations dropdown (at the top), select the specific module you want to run (e.g., `basic-integration`) and hit **Run**.

---
*Powered by the Veg Sahayyak Native C++ Hybrid Vault.*

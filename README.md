# 🚀 Veg Sahayyak SDK - Official Integration Samples

Welcome to the official repository of integration sample applications for the **Veg Sahayyak B2B Telematics SDK**. These enterprise examples demonstrate how to integrate our high-precision speed tracking, cryptographic vault licensing, and background services into various Android commercial solutions.

---

## 🗂️ Sample Applications Overview

This multi-module Android Studio project contains 7 self-contained app modules, ordered from basic to advanced:

| App Module | Complexity | Description | Package ID | Required SDK Modules |
| :--- | :---: | :--- | :--- | :--- |
| [📁 **basic-integration**](./basic-integration/) | 🟢 *Simple* | Clean starter project. Demonstrates commercial JWT unlock, foreground speed tracking, and reactive Compose Flow observation. | `com.vegsahayyak.samples.basic` | `:speed-core` |
| [📁 **fleet-tracking**](./fleet-tracking/) | 🟡 *Medium* | Heavy vehicle fleet management. Demonstrates custom speed thresholds for trucks, the Offline-First SQLite Telemetry Engine, and automatic cloud synchronization. | `com.vegsahayyak.samples.fleet` | `:speed-core` |
| [📁 **ubi-insurance**](./ubi-insurance/) | 🟡 *Medium* | Usage-Based Insurance (UBI). Evaluates real-time driver risk scoring, aggressive acceleration/braking penalties, and safety compliance. | `com.vegsahayyak.samples.ubi` | `:speed-core` |
| [📁 **background-service**](./background-service/) | 🟠 *Advanced* | Zero-touch background tracking. Uses Google Play Activity Recognition to auto-launch telematics the moment the vehicle begins moving. | `com.vegsahayyak.samples.background` | `:speed-core`, `:speed-autostart` |
| [📁 **custom-ui**](./custom-ui/) | 🟠 *Advanced* | White-label custom branding. Demonstrates dynamic gauge theming with `SpeedUiColorScheme` and high-performance Canvas gauge rendering. | `com.vegsahayyak.samples.customui` | `:speed-core`, `:speed-ui` |
| [📁 **oem-automotive**](./oem-automotive/) | 🔴 *Complex* | In-vehicle dashboard. Features landscape HUD mirroring and Picture-in-Picture (PiP) Floating Overlays on top of Google Maps or custom navigation. | `com.vegsahayyak.samples.oem` | `:speed-core`, `:speed-ui`, `:speed-overlay` |
| [📁 **delivery-copilot**](./delivery-copilot/) | 🔴 *Complex* | Two-wheeler last-mile delivery pilot assistant. Features hands-free voice commands, floating PiP speed bubble over navigation apps, weather hazard profiles, and rider fatigue monitoring. | `com.vegsahayyak.samples.delivery` | `:speed-core`, `:speed-ui`, `:speed-overlay` |

---

## 🛡️ Clean Architectural Standards
- **Zero Proprietary App Leaks**: Sample apps and the SDK are completely decoupled from consumer apps like Speed Assistant. All samples use official **Veg Sahayyak** branding and standard Material 3 components.
- **Broad Device Compatibility**: Compatible with **Android 7.0+ (API Level 24 to 35+)**, verified on real physical tablets and automotive head-units.
- **Kotlin 2.0 & Jetpack Compose**: Built with modern Kotlin DSL, Gradle 8.9+, and Android Jetpack Compose BOM.

---

## 🛠️ How to Build and Run

### 1. Clone & Open
Clone this repository and open the root directory in **Android Studio** (Koala / Ladybug / Panda).

### 2. Configure GitHub Packages Registry (Optional for Releases)
For fetching official release artifacts from the GitHub Packages registry, copy `local.properties.example` to `local.properties`:
```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```
*(Note: If you are building against locally modified SDK artifacts, Gradle automatically checks `mavenLocal()` first).*

### 3. Select Run Configuration
In the Android Studio run configurations dropdown at the top toolbar:
1. Select the desired module (e.g., `basic-integration`).
2. Select your connected physical Android device or emulator.
3. Click **Run (Shift + F10)**.

---

## 🔑 Pre-Configured License Tokens
Each sample module comes pre-configured with a **Dedicated Evaluation JWT License** cryptographically signed for its specific `applicationId` and required SDK modules. You do **not** need to manually generate or purchase a license token to run and explore these samples.

For commercial production deployment, contact [Veg Sahayyak Enterprise](https://vegsahayyak.com) to receive production tokens bound to your organization's unique package ID and authorized feature tier.

---
*Built with ❤️ by the Veg Sahayyak Telematics Team.*

# LifeRhythm

> **Understand your day. Understand yourself.**

LifeRhythm is a personal physical and digital wellness Android application that analyzes device usage rhythms and physical activity to create a personal daily rhythm profile.

Built with **Zero-Cloud Telemetry**: all biometric and behavioral processing stays strictly local to your device chip.

---

## 🌟 Key Features

- **Today's Rhythm Dashboard**:
  - Estimated nighttime inactivity interval with High / Medium / Low confidence scores.
  - Identification of last app activity before rest and first activity after rest.
  - Digital screen usage and total phone-free downtime breakdown.
  - Prominently displays: *"Estimated from phone activity. Inactivity does not confirm sleep."*
  - Behavioral Rhythm Score (0–100 non-medical rhythm index).
- **"Where Did My Day Go?" Interactive Timeline**:
  - 24-hour visual ribbon mapping active screen time, daytime pauses, and nighttime inactivity blocks.
  - Chronological session filtering and details.
- **You vs. You Baseline Trends**:
  - 7-Day rhythm harmony charts.
  - Personal comparison of screen time, downtime, and movement against your personal baseline.
  - Transparent "Observed Patterns" explicitly distinguishing correlation from causation.
- **Physical Rhythm & Health Connect**:
  - Native steps, distance, and active session integration without location or invasive biometric requests.
- **Privacy by Design**:
  - 100% offline capable: zero external telemetry, tracking, or cloud sync.
  - Full local data export in JSON and CSV formats.
  - One-tap permanent data erasure.

---

## 🛠️ Architecture & Technology

- **UI**: Jetpack Compose, Material 3, Edge-to-Edge (`enableEdgeToEdge`), Serene Metric design system.
- **Language**: 100% Kotlin with Coroutines and Flow.
- **Data & Telemetry**: Native Android `UsageStatsManager` / `UsageEvents`.
- **Local Persistence**: Android Room Database (`DailyRhythmEntity` & `DailyRhythmDao`).
- **Permissions**:
  - `android.permission.PACKAGE_USAGE_STATS` (granted manually by user via Android Settings).
  - `android.permission.POST_NOTIFICATIONS` (for optional bedtime gentle reflections).

---

## 🚀 How to Build & Run

### Prerequisites
- JDK 17
- Android SDK (compileSdk 36, minSdk 24)

### Building via Command Line
```bash
# Run unit tests
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 Installing & Testing on a Physical Android Phone

1. **Enable Developer Options on Android Phone**:
   - Go to **Settings** → **About phone**.
   - Tap **Build number** 7 times until you see "You are now a developer!".
   - Under **Settings** → **System** → **Developer options**, enable **USB debugging** (and "Install via USB" if prompted).
2. **Transfer APK to Device**:
   - Connect your phone via USB cable and run:
     ```bash
     adb install -r app/build/outputs/apk/debug/app-debug.apk
     ```
   - *Alternatively*, download the `liferhythm-debug-apk` artifact from GitHub Actions and open it on your phone.
3. **Grant Usage Access**:
   - Open **LifeRhythm**.
   - In Step 4 of Onboarding (or in Settings), tap **Open Android Settings**.
   - Find **LifeRhythm** in the Usage Access list and toggle it **On**.
   - Switch back to **LifeRhythm**; the app automatically updates in real-time with your actual local device usage sessions and phone-free intervals!

---

## 🛡️ Non-Negotiable Guarantees

1. **100% Offline Capable**: No background remote analytics, external telemetry pings, or shadow server profiles.
2. **Sleep Estimation Disclaimer**: Inactivity patterns provide general wellness estimations and never constitute a medical diagnosis.
3. **Zero Lockout**: Every journaling, breathing, and offline module functions properly even if permissions are revoked.

---

## 📄 License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

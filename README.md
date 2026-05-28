<p align="center">
  <a href="https://github.com/Hrishi2861/YourCarTalks">
    <img src="docs/logo.svg" alt="YourCarTalks Logo" width="200">
  </a>
</p>

<h1 align="center">YourCarTalks</h1>

<p align="center">
  <em>Your car greets you back. Every time you drive.</em>
</p>

<p align="center">
  <a href="https://kotlinlang.org">
    <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  </a>
  <a href="https://developer.android.com/studio">
    <img src="https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84?logo=android&logoColor=white" alt="Android">
  </a>
  <a href="https://developer.android.com/training/cars">
    <img src="https://img.shields.io/badge/Android%20Auto-Ready-E94560?logo=androidauto&logoColor=white" alt="Android Auto">
  </a>
  <a href="https://developer.android.com/about/versions/10">
    <img src="https://img.shields.io/badge/API-29%2B-4CAF50" alt="API">
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
  </a>
</p>

<br>

YourCarTalks is an Android app that plays **"Welcome to Your [car name]"** through your car's speakers every time you connect to **Android Auto**. Set your car's name once, and get a personalized greeting every drive.

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 🎙️ Smart Greeting
Hear *"Welcome to Your Beast"*, *"Welcome to Your XUV700"* — whatever name you choose — through your car's speakers.

</td>
<td width="50%">

### 🔌 Silent Detection
Uses Android Auto's `CarConnection` API. No icon appears in Android Auto's app drawer — it's purely phone-side.

</td>
</tr>
<tr>
<td width="50%">

### 🔋 Battery Friendly
Dedicated foreground service with low-importance notification. Won't get killed by the OS.

</td>
<td width="50%">

### 🚀 Start on Boot
Automatically resumes after device reboot — set it and forget it.

</td>
</tr>
</table>

<br>

## 🎯 How It Works

```
┌─────────────────────────────────────────────────┐
│          Phone connects to Android Auto          │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│     CarConnection emits CONNECTION_TYPE_         │
│               PROJECTION                         │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│      GreetingService (foreground) detects it     │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│     2-second delay (audio routing stabilizes)    │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│     TTS speaks "Welcome to Your {car_name}"      │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│        Audio plays through car speakers          │
│              ✦ Welcome to Your Tesla ✦           │
└─────────────────────────────────────────────────┘
```

<br>

## 🛡️ Permissions

| Permission | Purpose |
|---|---|
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Keep the connection monitor alive in the background |
| `POST_NOTIFICATIONS` (Android 13+) | Foreground service notification |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent the OS from killing the background service |
| `RECEIVE_BOOT_COMPLETED` | Restart the service after device reboot |

All permissions are requested during the **first-launch setup wizard** with clear explanations.

<br>

## 🚀 Getting Started

### Prerequisites

- Android phone with **Android 10+** (API 29)
- [Android Auto](https://www.android.com/auto/) set up on your phone and car
- A connected device or emulator for testing

### Build & Install

```bash
# Clone
git clone https://github.com/Hrishi2861/YourCarTalks.git
cd YourCarTalks

# Build
./gradlew assembleDebug

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### First Run

1. Open the app
2. Enter your car's name
3. Follow the setup wizard to grant permissions
4. That's it! Connect to Android Auto and hear your greeting 🎉

<br>

## 🧰 Tech Stack

| | |
|---|---|
| **Language** | [Kotlin](https://kotlinlang.org/) |
| **UI** | [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/) |
| **Car API** | [`androidx.car.app:app`](https://developer.android.com/jetpack/androidx/releases/car-app) (CarConnection only) |
| **Architecture** | Single-activity + Lifecycle-aware Foreground Service |
| **Persistence** | [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Preferences) |
| **Min SDK** | API 29 (Android 10) |
| **Target SDK** | API 34 |

<br>

## 📄 [License](License)

---

<p align="center">
  <sub>Made with ❤️ for everyone who talks to their car</sub>
</p>

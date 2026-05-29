# Changelog

## v1.2 (2026-05-29)

### Greeting Messages & Driver Name

**Greeting Messages:**
- 20 handcrafted greeting messages in 5 categories (Performance, Racing, Sci-Fi, Attitude, Time-Based)
- Random mode picks a different message each drive
- Messages can be selected directly in Settings
- Fallback to "Welcome to your {car_name}" when no driver name is set

**Driver Name:**
- New driver name input in Setup Wizard (optional — can skip)
- "Tap to set your name" placeholder in Settings
- Greeting becomes: "Welcome Hrishi. Your Tesla is ready for launch."
- Editable inline in Settings (same pattern as Car Name)

**Full greeting logic:**
- With driver name + specific message: "Welcome {name}. {formatted message}"
- With driver name + random: "Welcome {name}. {random message from all 20}"
- Without driver name: "Welcome to your {car_name}" (unchanged)

## v1.1 (2026-05-29)

### Kokoro Voice, Bluetooth Permission & Crash Fix

**Kokoro BF Isabella TTS:**
- Added 4th TTS option: Kokoro British Female (Isabella) — ~330 MB download
- Uses `OfflineTtsKokoroModelConfig` from sherpa-onnx 1.13.2 AAR
- Downloadable from `k2-fsa/sherpa-onnx` releases (kokoro-en-v0_19)
- Falls back to system TTS if model not downloaded
- Full integration: settings radio button, test button, model download card, greeting dispatch

**Bluetooth Permission (Android 12+):**
- Added `BLUETOOTH_CONNECT` permission to manifest
- New `BluetoothStep` in setup wizard (auto-skipped on API < 31)
- Runtime permission requested via `ActivityResultContracts.RequestPermission`
- Required for `connectedDevice` foreground service type on Android 14+

**Crash Fix (API 34+):**
- Check `BLUETOOTH_CONNECT` permission before calling `startForegroundService()`
- Wrapped `startForeground()` in try-catch `SecurityException` as safety net
- Prevents instant crash on devices running Android 14+

## v1.0 (2026-05-28)

### First Stable Release

**Multi-Engine TTS:**
- System TTS (device's built-in speech engine)
- Sherpa-ONNX Male — offline AI voice, ~80 MB download
- Sherpa-ONNX Female — offline AI voice, ~80 MB download
- Test button for each TTS engine in Settings — preview voices with your car name
- Audio focus handling (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`) for all engines

**Model Download & Management:**
- Download Sherpa-ONNX models directly from the app (tar.bz2 → auto-extract)
- Live download progress with MB counter and progress bar
- Extraction state shown with indeterminate progress indicator
- Error handling with Retry option
- LZMA + BZip2 extraction via Apache Commons Compress
- Model cleanup on reinstall (old directory deleted before extraction)

**Android Auto Detection:**
- Projection mode support (`CarConnection.CONNECTION_TYPE_PROJECTION`)
- Native Android Automotive support (`CarConnection.CONNECTION_TYPE_NATIVE`)
- 2-second greeting delay for audio routing stability

**Notification:**
- Minimal notification — only visible when connected to Android Auto
- Shows "Connected to Android Auto" during active connection
- No persistent notification when idle (uses `stopForeground(STOP_FOREGROUND_REMOVE)`)
- Foreground service resumes automatically on reconnect

**Theme System:**
- Light, Dark, and System-default theme modes
- Explicit custom color schemes (dark: `#121212` background, `#1E1E1E` surfaces)
- Window background and status bar icons sync with theme
- Theme selection in both Setup Wizard and Settings

**Settings Screen:**
- Edit car name inline
- TTS engine selection with radio buttons
- Test button per engine
- Model download card (shows when Sherpa engine selected and not downloaded)
- Theme selector (System / Light / Dark)
- Service status indicator

**Setup Wizard:**
- Car name input
- Battery optimization disable request
- Auto-start permission guide
- Notification permission (Android 13+)
- Theme selection step

**Technical:**
- Migrated to Jetpack Compose + Material 3 with explicit color schemes
- Kotlin coroutines for async model download and extraction
- `androidx.car.app` (CarConnection) for silent AA detection — no CarAppService, no AA launcher icon
- `androidx.datastore` for preferences (car name, TTS method, theme mode)
- `androidx.lifecycle:lifecycle-service` for lifecycle-aware foreground service
- Sherpa-ONNX 1.13.2 for offline TTS
- Apache Commons Compress 1.26.0 for archive extraction
- Min SDK 29, Target SDK 34

## v0.1 (2026-05-28)

### Initial Pre-release

**Core features:**
- Android Auto connection detection via `CarConnection` API (projection mode)
- Personalized TTS greeting: "Welcome to Your {car_name}"
- 2-second delay before speaking to let audio routing stabilize
- Foreground `LifecycleService` for reliable background monitoring
- `BootComplete` receiver to restart service after device reboot

**Setup wizard:**
- Car name input screen
- Battery optimization disable request
- Auto-start permission guide (OEM settings)
- Notification permission request (Android 13+)

**Settings screen:**
- View and edit car name
- Service status indicator

**Technical:**
- Kotlin + Jetpack Compose + Material 3
- `androidx.car.app` (CarConnection) for silent AA detection — no CarAppService, no AA launcher icon
- `androidx.datastore` for preferences
- `androidx.lifecycle:lifecycle-service` for lifecycle-aware service
- Min SDK 29, Target SDK 34

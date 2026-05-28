# Changelog

## v0.1 (2026-05-28)

### Initial Release

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

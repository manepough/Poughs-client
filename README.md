# BG Toolkit — Blockman Go Launcher + Hitbox Panel
## How it works
1. Open the app → press **LAUNCH BLOCKMAN GO**
2. App asks for "Display over other apps" permission (one time only)
3. Blockman Go opens, this app closes
4. A small **🟧 draggable bubble** floats over the game
5. **Tap the bubble** → Hitbox Expander panel slides in
6. Use the panel controls (factor, part, activate, reset)
7. Press **Close Panel & Stop Service** to remove the overlay

## Build with Android Studio
1. Open Android Studio → **Open** → select this folder
2. Wait for Gradle sync
3. Connect Android phone (USB debugging ON)
4. Click **▶ Run** — it installs the APK automatically

## Manual APK build
```
cd BlockmanLauncher
./gradlew assembleDebug
# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```
Then install via:  `adb install app/build/outputs/apk/debug/app-debug.apk`

## Permissions needed on device
- **Display over other apps** (SYSTEM_ALERT_WINDOW) — for the floating panel
- Granted automatically on prompt when you press Launch

## Notes
- Tested on Android 8.0+ (API 26+)
- Blockman Go package: `com.sandboxol.blockymods`
- The hitbox logic in OverlayService.java is the simulation layer —
  real in-game modification would require root or game mod integration

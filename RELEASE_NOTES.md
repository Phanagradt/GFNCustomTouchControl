### New Features
- **Cloud Sync for Freestyle Filters, Crosshair & Theme**:
  - When signed in with Google, XenosPad now automatically backs up and restores all 3 Freestyle Filter slots, Crosshair configuration, App Color Scheme, Font preset, Floating Bubble style, and Dark Theme alongside your controller layouts.
- **Gyroscope Aiming (Tilt-to-Aim)**:
  - Uses your phone's real hardware gyroscope sensor (`Sensor.TYPE_GYROSCOPE`) with Portrait/Landscape axis remapping, deadzone, smoothing, Invert X/Y, and **Always On** or **Hold ADS (LT / RMB)** modes. Toggleable from **Setup** or the floating **Left HUD Menu**.
- **Performance HUD Overlay (FPS / Battery / Temp / RAM)**:
  - Compact draggable GeForce-style stats pill showing live FPS, Battery % (with charging status), Battery Temperature (°C), and optional RAM %. Toggleable from **Setup** or the floating **Left HUD Menu**.
- **6-Digit Layout Share Codes**:
  - Generate a temporary 5-minute 6-character code for any controller layout in **Profiles** and let friends import a copy directly with one tap (requires Google Sign-In).
- **Edit Layout Directly Over the Live Game**:
  - Tap **Edit Layout** in the floating **Left HUD Menu** to drag, snap, resize, and adjust opacity on your controls right on top of your running game, then save straight to your profile and Cloud Save.

### In-Place Updates (No Uninstall Needed)
- Every GitHub Release APK uses the same package ID (`com.aistudio.nexuspad.vktrmz`), the same signing key (`NEXUSPAD_KEYSTORE_BASE64` / `debug.keystore.base64`), and an automatically increasing `versionCode` so updates install right over your existing XenosPad app without deleting your layouts or data.
- *One-time switch note:* Only if your currently installed APK was signed with a different key (for example, an older build before `NEXUSPAD_KEYSTORE_BASE64` was added, or switching between AI Studio's Export APK and GitHub Releases), uninstall once to move onto the permanent signing key—every future update from that same source will overwrite cleanly in-place.

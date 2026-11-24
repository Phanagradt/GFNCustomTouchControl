### New Features
- **Complete App-Wide Multi-Language Translation**:
  - Every screen—**Cockpit**, **Profiles**, **Freestyle Game Filters**, **Crosshair**, **Shadow Capture & Gallery**, **Test Sandbox**, **Recording Quality**, **App Updates**, **Personalization**, **What's New**, and the **Floating Overlay Menu**—now translates cleanly into **English**, **ไทย (Thai)**, **Español**, **日本語**, and **中文** (with System Auto detection).
- **Custom App Color Scheme & 2–3 Color Mixer**:
  - Choose from 7 curated tactical color presets or mix your own **2-color** or **3-color** custom palette in **Setup** to theme the entire app and overlay controls live.
- **Custom Font Presets & `.ttf` / `.otf` Font Upload**:
  - Switch between bundled gaming fonts (*Space Grotesk*, *Orbitron*, *Rajdhani*, *Poppins*, *Nunito*, *JetBrains Mono*) or upload your own `.ttf`/`.otf` font files (e.g. from DaFont.com) with native validation and instant preview.
- **Custom Floating Bubble Size & UI**:
  - Resize the floating bubble (`36–88 dp`), adjust opacity (`25%–100%`), pick from **5 shapes** (*Tactical Ring*, *Cyber Pill*, *Hex Shield*, *Stealth Orb*, or *Custom Image*), toggle the live status dot, and sync with your app theme or custom accent color.
- **Custom Testing Background Image**:
  - Upload any photo from your device via the Android Photo Picker as the background inside the **Test Sandbox** (and reset back to default anytime).
- **First-Launch "What's New" Pop-Up**:
  - Automatically greets you once after installing a new release with localized highlights, and can also be reopened anytime from **Setup → App Updates**.
- **Animated Built-In Virtual Keyboard + Gboard-Style Auto-Correct & Word Suggestions**:
  - Added tactile key-press scale/glow animations, an enlarged floating key preview pop-up, and an offline **English & Thai** next-word suggestion and auto-correct bar across both the NexusPad IME and the floating overlay keyboard.

### Bug Fixes & In-Place Update Reliability
- **Fixed `"App not installed as package appears to be invalid"` on Updates**:
  - Standardized GitHub Actions release builds to sign every APK with the same deterministic signing key (instead of generating a random keystore per build) and added pre-install APK certificate & version verification inside the **Check for Updates** flow so updates install right over your previous version while preserving all layouts, filters, and gallery captures.
  - *Note:* If your device currently has an older APK built with the previous random CI key, uninstall that old build **once** before installing this release—every update after that will upgrade smoothly in-place.

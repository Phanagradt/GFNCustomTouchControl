### New Features
- **Screen Recording & Instant Replay Quality Settings (Resolution & FPS)**:
  - Choose your preferred recording resolution (**480p SD**, **720p HD**, **1080p Full HD**, **1440p QHD**, or **Native Screen**) and frame rate (**24**, **30**, **48**, **60**, **90**, or **120 FPS**) in **Setup** or directly from the **Shadow Capture & Gallery** deck.
  - Displays live target H.264 bitrate (`Mbps`) and estimated storage usage (`~MB/min`), with automatic `MediaCodecList` hardware-encoder capability checks that gracefully step down if a device caps 1440p or high frame rates.
- **Direct In-App GitHub Release Updates (In-Place Upgrade — Keeps All Data)**:
  - Added an **App Updates (GitHub Releases)** system in **Setup** (plus an update badge indicator on the Setup tab) so users can check for new releases, read the "What's New" notes, download the signed release APK with live progress, and launch Android's installer directly from inside NexusPad **without uninstalling the old version or losing saved layouts, filters, and gallery captures**.
  - Updated GitHub Actions release builds to automatically embed monotonically increasing `versionCode`, matching `versionName`, and the repository slug while keeping the exact same signing key for seamless one-tap in-place updates.
- **Real OpenGL ES 2.0 GeForce Freestyle GPU Post-Processing Filter Engine**: Full 18-module hardware-accelerated GLSL fragment shader pipeline (*Sharpen*, *Sharpen+*, *Brightness/Contrast*, *Details*, *Color*, *Colorblind*, *Night Mode*, *Splitscreen*, *Black & White*, *Sepia*, *Vignette*, *Depth of Field / Tilt-Shift*, *Old Film*, *Mood*, *Special FX*, *Painterly*, *Letterbox*, and *Stickers*).
- **GeForce-Style Instant Replay (10s – 1h), Screen Recording, Screenshot & Gallery**: Capture PNG screenshots, manual MP4 recordings, and 10s–1h Instant Replay clips with a dedicated **Gallery** tab.

### UI & Performance Improvements
- **Silky-Smooth 120Hz Zero-Stutter Sliders App-Wide**: Custom Canvas `SmoothSlider` and in-memory `GameFilterBridge` eliminate lag while dragging sliders across **Game Filters**, **Crosshair**, **Layout Editor**, **Setup**, and **Gallery**.
- **Dynamic Frame-Accurate Replay Muxing**: Instant Replay segment stitching now calculates exact per-frame presentation timestamps from your chosen recording FPS.

### Bug Fixes
- **Fixed MediaProjection Screen Capture & Recording Crashes**: Resolved Android 15/16 (`targetSdk 36`) foreground service permission ordering for `FOREGROUND_SERVICE_MEDIA_PROJECTION`, guarded empty-frame MP4 muxer teardown, and added safe video thumbnail/playback fallback handling.
- **Fixed Overlay Mouse & Trackpad Feedback**: Preserved reliable press highlights for `LMB`, `RMB`, `MMB` and continuous non-freezing trackpad camera drags on floating overlays.

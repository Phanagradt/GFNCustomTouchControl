### New Features
- **Game Compatibility Mode (GeForce NOW ↔ Android Games)**: Switch between **GeForce NOW (Cloud PC)** mode and **Android Games (Roblox, Minecraft, etc.)** mode in **Setup** or directly inside the floating menu (`Mode: GFN` / `Mode: Android`) to tailor camera panning zones, key behavior, and mouse clicks per game type.
- **Customizable Crosshair HUD**: Toggle a precision aiming crosshair directly in the app or from the floating bubble menu. Includes 5 built-in presets (*Pro Lime +*, *Headshot Dot*, *Holo Ring*, *Red T-Bar*, *Sniper X*), live color & outline swatches, thickness/length/gap/opacity sliders, center dot toggle, and custom crosshair image upload.
- **Floating Keyboard & Multi-Language Input (EN / ไทย)**: Added a movable **Floating Keyboard** window (`FLOAT` / `DOCK` button on the keyboard bar, floating bubble menu, and Settings) plus a built-in **Thai (Kedmanee ไทย)** layout with one-tap `🌐 EN / ไทย` switching.
- **Auto App Language & Settings Selector**: NexusPad detects your phone's system language automatically on install and adds an **App Language** selector in **Setup** (*System Default*, *English*, *ไทย*, *Español*, *日本語*, *中文*).
- **GeForce-Style Game Color Filter**: Tune **Brightness**, **Contrast**, **Shadows**, **Color Vibrance**, **Temperature**, and tint presets live from the app or the floating bubble menu.

### UI & Controls Improvements
- **Continuous 360° Trackpad Mouse Camera**: Rebuilt Mouse Trackpad camera control using unbroken continuous `continueStroke` gesture streaming with sub-pixel accumulation and automatic edge recentering, eliminating single-flick cutoffs, held-key interruptions (`WASD`/`Shift`), and frame stutters.
- **Silky-Smooth Bubble & Floating Keyboard Dragging**: Anchored floating window dragging to raw screen coordinates (`rawX` / `rawY`) to eliminate coordinate oscillation jitter while moving the floating bubble or floating keyboard.
- **Left-Side Docked Overlay Menu & Smooth Grid Editor**: Tapping the floating bubble opens the menu cleanly along the **left side** of the screen, paired with hardware-accelerated dragging and configurable grid size in Layout Studio.

### Bug Fixes
- **Fixed LMB / RMB / MMB Hold & Telemetry State**: Prevented Accessibility pointer gestures from self-tapping NexusPad's own Play-Test and Sandbox windows (which previously canceled the held finger and flashed `MOUSE_UP` immediately), so holding `LMB`, `RMB`, or `MMB` cleanly displays `MOUSE_DOWN` while held and `MOUSE_UP` on release, alongside reliable gameplay dispatch.
- **Fixed Custom Button Image Upload & True Full-Screen Landscape Layout**: Reliable `ImageDecoder` + Files picker import and edge-to-edge `FLAG_LAYOUT_NO_LIMITS` full-screen landscape coverage.


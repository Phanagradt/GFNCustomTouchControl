### Bug Fixes
- **GeForce NOW In-Game Mouse & Camera Control**: Fixed duplicate mouse cursors inside active games. The app's virtual cursor now appears only outside games (in the GeForce NOW lobby) and hides automatically once a game stream starts, routing trackpad movement, left/right clicks, and scrolling directly into GeForce NOW's locked pointer engine for both in-game menu cursors and 360° 3D camera turning.
- **Reliable Left & Right Mouse Clicking**: Fixed left/right click input not registering in GeForce NOW for both 1-finger/2-finger trackpad taps and dedicated LMB/RMB buttons.

### New Features & Enhancements
- **Anti-AFK / Queue Keeper Toggle**: Toggleable keep-alive in Web Play Mode and Setup that periodically sends harmless zero-net-movement ticks to prevent GeForce NOW idle disconnects during queues or short breaks, with a live `AFK` HUD indicator.
- **Physical Controller Support & Auto-Hide Touch Controls**: Automatic Bluetooth and USB gamepad detection that auto-hides on-screen touch buttons (with manual override), prioritizes your physical controller as Player 1, adds a stick deadzone slider, and includes a Nintendo A/B & X/Y layout swap toggle.
- **Pinned Games Hub**: Pin favorite GeForce NOW games on the Cockpit screen linked to a saved Layout Profile, Freestyle Filter Slot, and Built-in Region Bypass server—tapping Play auto-applies all three and launches Web Play Mode immediately.
- **Drag-to-Aim / Turn Camera Buttons**: Toggleable option in Layout Studio & Live Edit on any button or shoulder paddle so holding the button presses its bound key/click while sliding the same finger turns the camera simultaneously in Mouse Look or Right Analog Stick mode.
- **Full 18-Module Filter Studio in Web Play Mode**: Brought the complete NVIDIA GeForce Freestyle filter suite (Sharpen/Sharpen+/Details, Brightness/Contrast/Highlights/Shadows/Gamma, Color Vibrance/Temp/Hue Tint, Bloom, Colorblind, Night Mode, Splitscreen, B&W, Sepia, Vignette, Depth of Field, Old Film, Mood LUTs, Special FX, Painterly, Letterbox, and Stickers + Custom Sticker upload) directly into Web Play Mode with live WebView rendering.
- **Full-Featured Live Edit Mode**: Upgraded Live Edit in both the GeForce NOW Web Player and floating overlay to match the full Layout Studio—add or rebind controls, build custom combos, customize label/size/opacity/shape/color/Hold-Toggle-Turbo/touchpad settings, nudge, mirror, duplicate, delete, undo, and adjust grid snapping live over your game without touching the background.
- **Live Server Ping for Built-in Region Bypass**: Automatically measures and displays real round-trip latency (ms) from your current location to every target GeForce NOW region server with color-coded ping badges and a one-tap refresh button across the Cockpit, Setup, and in-game Web Player menu.

### Minor Changes
- Removed the `(Better xCloud Style)` label from the Built-in Region Bypass section across all languages.
- Updated GitHub Release notes generation so future releases automatically list only the bug fixes, minor tweaks, and new features added in that specific version.

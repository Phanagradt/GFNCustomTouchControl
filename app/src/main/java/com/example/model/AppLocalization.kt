package com.example.model

import android.content.res.Resources
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.os.ConfigurationCompat
import java.util.Locale

enum class AppLanguage(val code: String, val nativeLabel: String) {
    SYSTEM("system", "System Language (Auto)"),
    ENGLISH("en", "English"),
    THAI("th", "ไทย (Thai)"),
    SPANISH("es", "Español"),
    JAPANESE("ja", "日本語"),
    CHINESE("zh", "中文")
}

/**
 * Full app-wide localized string table.
 * Implemented as a zero-constructor-parameter class with internal setters rather than a
 * >255-parameter data class so it never exceeds the JVM bytecode 255-parameter limit.
 */
class NexusStrings internal constructor() {
    // Navigation
    var navCockpit: String = "Cockpit"; internal set
    var navProfiles: String = "Profiles"; internal set
    var navVisuals: String = "Visuals"; internal set
    var navGallery: String = "Gallery"; internal set
    var navSandbox: String = "Sandbox"; internal set
    var navSetup: String = "Setup"; internal set

    // Common buttons & status
    var startOverlay: String = "Start Overlay"; internal set
    var stopOverlay: String = "Stop Overlay"; internal set
    var launchOverlay: String = "Launch Overlay"; internal set
    var commonEdit: String = "Edit"; internal set
    var commonCustomize: String = "Customize"; internal set
    var commonCancel: String = "Cancel"; internal set
    var commonClose: String = "Close"; internal set
    var commonReset: String = "Reset"; internal set
    var commonEnable: String = "Enable"; internal set
    var commonEnabled: String = "Enabled"; internal set
    var commonStop: String = "Stop"; internal set
    var commonOverlay: String = "Overlay"; internal set
    var commonAll: String = "All"; internal set
    var commonActive: String = "ACTIVE"; internal set
    var commonReady: String = "READY"; internal set
    var commonGrant: String = "GRANT"; internal set
    var commonSelect: String = "SELECT"; internal set
    var commonSetup: String = "SETUP"; internal set
    var commonLaunch: String = "LAUNCH"; internal set
    var commonOpenSettings: String = "Open Settings"; internal set
    var commonNotNow: String = "Not Now"; internal set
    var commonGranted: String = "Granted"; internal set
    var commonShare: String = "Share"; internal set
    var commonDelete: String = "Delete"; internal set
    var commonControlsSuffix: String = "controls"; internal set

    // Cockpit Screen
    var cockpitOverlayLive: String = "OVERLAY LIVE"; internal set
    var cockpitArmed: String = "COCKPIT ARMED"; internal set
    var cockpitIntegrationHeader: String = "GEFORCE NOW INTEGRATION"; internal set
    var cockpitOverlayEngineTitle: String = "Screen Overlay Engine"; internal set
    var cockpitOverlayEngineGranted: String = "Display over other apps permission granted"; internal set
    var cockpitOverlayEngineMissing: String = "Required to float virtual controls over GeForce NOW"; internal set
    var cockpitImeTitle: String = "Gamepad & Built-In Keyboard IME"; internal set
    var cockpitImeSelected: String = "Active for key dispatch & built-in typing keyboard"; internal set
    var cockpitImeEnabled: String = "Enabled — tap to select as active input method"; internal set
    var cockpitImeDisabled: String = "Enable XenosPad IME for game keys & built-in keyboard"; internal set
    var cockpitMouseTitle: String = "Mouse Click & Scroll Gestures"; internal set
    var cockpitMouseReady: String = "Enabled — Left, Right, Middle click & Scroll active"; internal set
    var cockpitMouseMissing: String = "Enable XenosPad Mouse Gestures for LMB, RMB, MMB & Scroll"; internal set
    var cockpitGfnTitle: String = "GeForce NOW Stream Target"; internal set
    var cockpitGfnInstalled: String = "Detected com.nvidia.geforcenow — ready to launch"; internal set
    var cockpitGfnMissing: String = "Launch Play Store or verify layouts in Test Sandbox"; internal set
    var cockpitQuickProfile: String = "QUICK PROFILE SWITCH"; internal set
    var cockpitSavedSuffix: String = "saved"; internal set
    var cockpitVibrationHeader: String = "BUTTON VIBRATION"; internal set
    var cockpitActiveTopology: String = "ACTIVE BINDING TOPOLOGY"; internal set
    var cockpitMappedSuffix: String = "virtual controls mapped"; internal set
    var cockpitTapCustomize: String = "Tap to customize positions"; internal set
    var cockpitLayoutStudio: String = "Layout Studio"; internal set
    var cockpitTestControls: String = "Test Controls"; internal set

    // Vibration modes
    var vibNoneTitle: String = "No Vibration"; internal set
    var vibNoneSub: String = "Completely off — zero shake on press"; internal set
    var vibLightTitle: String = "Light Tap"; internal set
    var vibLightSub: String = "Subtle keyboard-style feedback"; internal set
    var vibStrongTitle: String = "Crisp Click"; internal set
    var vibStrongSub: String = "Firm virtual key click"; internal set

    // Profiles Screen
    var profilesTitle: String = "Controller & Key Profiles"; internal set
    var profilesSubtitle: String = "Customize per-game layouts for GeForce NOW & Android games"; internal set
    var profilesImport: String = "Import"; internal set
    var profilesSearchHint: String = "Search game or layout name..."; internal set
    var profilesEmptyTitle: String = "No matching controller profiles"; internal set
    var profilesEmptySub: String = "Create a new layout or clear your genre filter."; internal set
    var profilesArm: String = "Arm"; internal set
    var profilesNewTitle: String = "New Layout Profile"; internal set
    var profilesStarterTemplate: String = "Starter Template"; internal set
    var profilesNameLabel: String = "Profile Name"; internal set
    var profilesTargetGameLabel: String = "Target Game"; internal set
    var profilesGenreTag: String = "Genre Tag"; internal set
    var profilesCreateAndEdit: String = "Create & Edit"; internal set

    // Visuals: Game Filter & Crosshair
    var visualsGameFilterTab: String = "Game Filter"; internal set
    var visualsCrosshairTab: String = "Crosshair"; internal set
    var visualsFilterSubtitle: String = "18-module OpenGL ES Freestyle GPU pixel shader suite"; internal set
    var visualsCrosshairSubtitle: String = "Adjust reticle shape, color, outline, gap, thickness & custom image"; internal set
    var filterPanelTitle: String = "GAME FILTER"; internal set
    var filterGpuBadge: String = "GPU SHADER"; internal set
    var filterOffHint: String = "Filter Off — tap Style 1–3 to activate"; internal set
    var filterStyleLabel: String = "Style"; internal set
    var filterOffLabel: String = "Off"; internal set
    var filterGpuEngineActive: String = "Live GPU Shader Engine: ACTIVE"; internal set
    var filterGpuEngineTitle: String = "Live GPU Per-Pixel Shader"; internal set
    var filterGpuEngineActiveDesc: String = "Real OpenGL Sharpen, HDR, Shadows, DoF & FX active on live game stream."; internal set
    var filterGpuEngineIdleDesc: String = "Pick 'A single app' (e.g., GeForce NOW) for true 60fps pixel shaders."; internal set
    var filterPreviewPhoto: String = "Photo"; internal set
    var filterCompare: String = "Compare"; internal set
    var filterOriginal: String = "Original"; internal set
    var filterFiltered: String = "FILTERED"; internal set
    var filterPreviewActiveBanner: String = "Live GPU Shader Overlay ACTIVE — real per-pixel GLSL filtering on your game"; internal set
    var filterPreviewIdleBanner: String = "Real OpenGL ES 2.0 shader preview • Enable Live GPU Shader below for in-game pixels"; internal set
    var filterCatEssential: String = "Essential"; internal set
    var filterCatUtility: String = "Utility"; internal set
    var filterCatArtistic: String = "Artistic"; internal set
    var filterModSharpen: String = "Sharpen"; internal set
    var filterModSharpenPlus: String = "Sharpen+"; internal set
    var filterModBrightnessContrast: String = "Brightness / Contrast"; internal set
    var filterModDetails: String = "Details (HDR & Clarity)"; internal set
    var filterModColor: String = "Color & Vibrance"; internal set
    var filterModColorblind: String = "Colorblind Assist"; internal set
    var filterModNightMode: String = "Night Mode"; internal set
    var filterModSplitscreen: String = "Splitscreen Compare"; internal set
    var filterModBlackWhite: String = "Black & White"; internal set
    var filterModSepia: String = "Sepia"; internal set
    var filterModVignette: String = "Vignette"; internal set
    var filterModDof: String = "Depth of Field / Tilt-Shift"; internal set
    var filterModOldFilm: String = "Old Film"; internal set
    var filterModPainterly: String = "Painterly"; internal set
    var filterModLetterbox: String = "Letterbox"; internal set
    var filterModMood: String = "Mood Presets"; internal set
    var filterModSpecialFx: String = "Special FX"; internal set
    var filterModStickers: String = "Stickers"; internal set

    // Crosshair
    var crosshairTitle: String = "CROSSHAIR HUD"; internal set
    var crosshairEnable: String = "Enable Crosshair"; internal set
    var crosshairPresets: String = "Presets"; internal set
    var crosshairColor: String = "Crosshair Color"; internal set
    var crosshairOutline: String = "Outline"; internal set
    var crosshairThickness: String = "Thickness"; internal set
    var crosshairLength: String = "Length"; internal set
    var crosshairGap: String = "Center Gap"; internal set
    var crosshairOpacity: String = "Opacity"; internal set
    var crosshairCenterDot: String = "Center Dot"; internal set
    var crosshairUploadImage: String = "Upload Image"; internal set
    var crosshairReset: String = "Reset"; internal set
    var crosshairSize: String = "Image Size"; internal set

    // Gallery Screen
    var galleryTitle: String = "Shadow Capture & Gallery"; internal set
    var gallerySubtitle: String = "GeForce-style Instant Replay (10s – 1h), manual recording, and screenshots."; internal set
    var galleryScreenshotsTab: String = "Screenshots"; internal set
    var galleryVideosTab: String = "Videos & Replays"; internal set
    var galleryReplayOn: String = "Replay ON"; internal set
    var gallerySaveClipPrefix: String = "Save Last"; internal set
    var gallerySaveClipSuffix: String = "Clip Now"; internal set
    var galleryReplayBufferHeader: String = "INSTANT REPLAY BUFFER LENGTH"; internal set
    var galleryCaptureQualityHeader: String = "CAPTURE QUALITY"; internal set
    var galleryEmptyTitle: String = "No captures yet"; internal set
    var galleryEmptySubtitle: String = "Use Screenshot, Instant Replay (10s–1h), or Record above — or from the Floating Bubble menu while playing."; internal set
    var galleryCannotDecodeImage: String = "Unable to decode image"; internal set
    var galleryCannotPlayVideo: String = "Unable to play this video clip"; internal set
    var galleryDeleteBroken: String = "Delete Broken Clip"; internal set

    // Sandbox Screen
    var sandboxTitle: String = "Stream Test Sandbox"; internal set
    var sandboxNoProfile: String = "No profile active"; internal set
    var sandboxCustomBg: String = "Custom BG"; internal set
    var sandboxResetBg: String = "Reset BG"; internal set
    var sandboxTelemetryHeader: String = "LIVE INPUT TELEMETRY (TOUCH + HARDWARE)"; internal set
    var sandboxTelemetryEmpty: String = "Tap buttons, drag sticks, or press hardware keys above to inspect KeyEvent & Axis telemetry."; internal set
    var sandboxFilterOff: String = "FILTER OFF"; internal set

    // Setup Screen
    var setupTitle: String = "Overlay & System Setup"; internal set
    var setupSubtitle: String = "Configure appearance, fonts, bubble, language, recording, updates, and permissions."; internal set
    var languageCardTitle: String = "APP LANGUAGE"; internal set
    var languageSubtitle: String = "Automatically uses your phone's system language by default"; internal set
    var targetModeCardTitle: String = "GAME COMPATIBILITY MODE"; internal set
    var targetModeSubtitle: String = "Choose whether XenosPad targets GeForce NOW cloud PC or native Android games"; internal set
    var targetModeGfnTitle: String = "GeForce NOW (Cloud PC)"; internal set
    var targetModeGfnSubtitle: String = "Linux scan codes + center mouse camera & GFN touch gestures"; internal set
    var targetModeAndroidTitle: String = "Android Games (Roblox, Minecraft, etc.)"; internal set
    var targetModeAndroidSubtitle: String = "Standard Android key events + right-side 360° camera drag & native mouse clicks"; internal set

    // Setup: Personalization (Color Mixer, Fonts, Floating Bubble)
    var themeCardTitle: String = "APP COLOR SCHEME & MIXER"; internal set
    var themeCardSubtitle: String = "Choose a curated theme preset or mix 2–3 custom colors for the entire app"; internal set
    var themePresetsLabel: String = "Theme Presets"; internal set
    var themeCustomMixTitle: String = "Custom Color Mix"; internal set
    var themeMix2Colors: String = "2 Colors"; internal set
    var themeMix3Colors: String = "3 Colors"; internal set
    var themePrimaryColor: String = "Primary Color"; internal set
    var themeSecondaryColor: String = "Secondary Color"; internal set
    var themeTertiaryColor: String = "Accent (3rd Color)"; internal set
    var fontCardTitle: String = "FONT CUSTOMIZATION"; internal set
    var fontCardSubtitle: String = "Pick a built-in gaming font or upload your own .ttf / .otf file (e.g. from DaFont.com)"; internal set
    var fontUploadButton: String = "Upload Font (.ttf / .otf)"; internal set
    var fontUploadedLabel: String = "Uploaded Font"; internal set
    var fontSamplePreview: String = "XENOSPAD • Ready to play 12345 — สวัสดี"; internal set
    var bubbleCardTitle: String = "FLOATING BUBBLE SIZE & UI"; internal set
    var bubbleCardSubtitle: String = "Customize your floating overlay button shape, size, opacity, color, or custom image"; internal set
    var bubbleStyleLabel: String = "Bubble Shape & Style"; internal set
    var bubbleSizeLabel: String = "Bubble Size"; internal set
    var bubbleOpacityLabel: String = "Bubble Opacity"; internal set
    var bubbleStatusDotLabel: String = "Show Live Status Dot"; internal set
    var bubbleStatusDotSub: String = "Shows recording, replay, lock, or hidden indicator on the bubble"; internal set
    var bubbleUseThemeAccent: String = "Match App Color Scheme"; internal set
    var bubbleUseThemeAccentSub: String = "Automatically sync bubble border & icon with your chosen app theme"; internal set
    var bubbleCustomColorLabel: String = "Bubble Accent Color"; internal set
    var bubbleUploadIconButton: String = "Upload Bubble Image"; internal set
    var bubbleLivePreview: String = "Live Bubble Preview"; internal set

    // Setup: Recording Quality
    var recQualityTitle: String = "SCREEN RECORDING & REPLAY QUALITY"; internal set
    var recQualitySubtitle: String = "Choose output resolution and frame rate for Recordings & Instant Replay."; internal set
    var recResolutionLabel: String = "Resolution"; internal set
    var recFpsLabel: String = "Frame Rate (FPS)"; internal set
    var recEncoderProfileLabel: String = "TARGET H.264 ENCODER PROFILE"; internal set
    var recAutoFallbackNote: String = "Auto-Fallback: If your phone's hardware H.264 encoder caps 1440p or 90/120 FPS, XenosPad automatically steps down to the highest stable resolution & FPS without failing."; internal set

    // Setup: App Updates
    var updatesCardTitle: String = "APP UPDATES (GITHUB RELEASES)"; internal set
    var updatesCardSubtitle: String = "Check GitHub Releases for newer versions and open the latest release page to download."; internal set
    var updatesInstalledVersion: String = "Installed Version"; internal set
    var updatesSigningKeyLabel: String = "Signing Key SHA-256"; internal set
    var updatesRepoLabel: String = "GitHub Repository (owner/repo)"; internal set
    var updatesRepoPlaceholder: String = "e.g. yourname/XenosPad"; internal set
    var updatesRepoAutoHint: String = "Auto-configured from GitHub Actions. Same signing key preserves all app data on every update."; internal set
    var updatesRepoManualHint: String = "Paste your GitHub repo slug or URL. GitHub Actions builds auto-fill this automatically."; internal set
    var updatesAutoCheckTitle: String = "Auto-Check on App Launch"; internal set
    var updatesAutoCheckSub: String = "Silently check GitHub Releases for newer APKs when XenosPad opens"; internal set
    var updatesCheckButton: String = "Check for Updates"; internal set
    var updatesChecking: String = "Checking GitHub Releases..."; internal set
    var updatesUpToDate: String = "You're up to date!"; internal set
    var updatesNewAvailable: String = "Update available, download at GitHub"; internal set
    var updatesDownloadAndUpdate: String = "Download at GitHub"; internal set
    var updatesDownloading: String = "Downloading"; internal set
    var updatesCancelDownload: String = "Cancel Download"; internal set
    var updatesReadyTitle: String = "Ready to install"; internal set
    var updatesReadyKeepsData: String = "Signature verified! Tap below to update in-place — all layouts, filters, and gallery are preserved."; internal set
    var updatesReadyNeedPerm: String = "One-time setup: Android will ask to enable \"Install unknown apps\" for XenosPad, then tap Install Update again."; internal set
    var updatesInstallButton: String = "Install Update (Keeps Data)"; internal set
    var updatesAllowAndInstall: String = "Allow & Install Update"; internal set
    var updatesSigMismatchTitle: String = "One-Time Reinstall Needed (Key Changed)"; internal set
    var updatesSigMismatchBody: String = "Your currently installed build was signed with an older random test key. Uninstall it once, install this new release, and every future update will install cleanly on top without uninstalling!"; internal set
    var updatesViewWhatsNew: String = "What's New in This Version"; internal set

    // Setup: Permissions & Preferences
    var setupVibrationCardTitle: String = "BUTTON VIBRATION / HAPTICS"; internal set
    var setupPermsCardTitle: String = "ANDROID OVERLAY & IME PERMISSIONS"; internal set
    var setupPerm1Title: String = "1. Display Over Other Apps"; internal set
    var setupPerm1Desc: String = "Allows floating virtual buttons and analog sticks to appear above GeForce NOW."; internal set
    var setupPerm2Title: String = "2. Enable XenosPad IME & Keyboard"; internal set
    var setupPerm2Desc: String = "Dispatches real KeyEvents for gamepad/keyboard binds and provides a built-in virtual typing keyboard."; internal set
    var setupPerm3Title: String = "3. Mouse Click & Scroll Gestures"; internal set
    var setupPerm3Desc: String = "Accessibility gesture engine for Left Click, Right Click, Middle Click, and Scroll Up/Down."; internal set
    var setupEnableIme: String = "Enable IME"; internal set
    var setupImeSelected: String = "IME Selected ✓"; internal set
    var setupSelectIme: String = "Select Active IME"; internal set
    var setupAlertsReady: String = "Alerts Ready"; internal set
    var setupAllowAlerts: String = "Allow Alerts"; internal set
    var keyboardOptionsTitle: String = "KEYBOARD & FLOATING MODE"; internal set
    var floatingKeyboardTitle: String = "Floating Keyboard by Default"; internal set
    var floatingKeyboardSubtitle: String = "Opens a movable keyboard above the system taskbar so keys are never hidden"; internal set
    var openFloatingKeyboard: String = "Open Floating Keyboard"; internal set
    var setupNotationTitle: String = "CONTROLLER NOTATION"; internal set
    var setupHudPrefsTitle: String = "HUD & INTERACTION PREFERENCES"; internal set
    var setupGlobalOpacity: String = "Global Overlay Opacity"; internal set
    var gridSizeTitle: String = "Layout Grid Size"; internal set
    var snapToGridTitle: String = "Snap Controls to Grid"; internal set
    var snapToGridSubtitle: String = "Align buttons cleanly on release in the Layout Studio"; internal set
    var setupTelemetryTitle: String = "Show Telemetry HUD"; internal set
    var setupTelemetrySub: String = "Display live KeyEvent & axis monitor in Sandbox"; internal set
    var setupAutoLaunchTitle: String = "Auto-Launch GeForce NOW"; internal set
    var setupAutoLaunchSub: String = "Open GeForce NOW automatically when starting overlay"; internal set
    var setupDarkModeTitle: String = "Tactical Dark Cockpit"; internal set
    var setupDarkModeSub: String = "High-contrast obsidian theme for low-glare play"; internal set
    var setupInfoNote: String = "Note: On non-rooted Android devices, virtual buttons dispatch real KeyEvents through the XenosPad IME bridge for keyboard and digital gamepad bindings. Multi-touch is supported simultaneously across all overlay buttons and sticks."; internal set

    // Overlay Permission Dialog
    var permDialogTitle: String = "Enable Floating Game Overlay"; internal set
    var permDialogBody: String = "To display your custom virtual controller buttons and keyboard hotkeys on top of GeForce NOW, grant XenosPad the \"Display over other apps\" permission in Android Settings."; internal set

    // Overlay Menu
    var menuChooseLayoutHeader: String = "CHOOSE LAYOUT"; internal set
    var menuDragKbHint: String = "DRAG KEYBOARD ABOVE TASKBAR"; internal set
    var menuHidePad: String = "Hide Pad"; internal set
    var menuShowPad: String = "Show Pad"; internal set
    var menuSwitch: String = "Switch"; internal set
    var menuLayouts: String = "Layouts"; internal set
    var menuTargetMode: String = "Mode"; internal set
    var menuFilter: String = "Filter"; internal set
    var menuCrosshair: String = "Crosshair"; internal set
    var menuScreenshot: String = "Screenshot"; internal set
    var menuReplay: String = "Instant Replay"; internal set
    var menuSaveReplay: String = "Save Replay"; internal set
    var menuRecord: String = "Record"; internal set
    var menuStopRecord: String = "Stop Rec"; internal set
    var menuKeyboard: String = "Keyboard"; internal set
    var menuFloatKb: String = "Float KB"; internal set
    var menuUnstick: String = "Unstick"; internal set
    var menuLock: String = "Lock"; internal set
    var menuUnlock: String = "Unlock"; internal set
    var menuStop: String = "Stop"; internal set
    var menuOpenApp: String = "Open XenosPad"; internal set

    // What's New Popup
    var whatsNewBadge: String = "WHAT'S NEW"; internal set
    var whatsNewTitle: String = "Welcome to XenosPad"; internal set
    var whatsNewSubtitle: String = "Here's everything new in this release:"; internal set
    var whatsNewGotIt: String = "Awesome, Let's Play!"; internal set
    var whatsNewItems: List<Pair<String, String>> = listOf(
        "Full Multi-Language Translation" to "Every screen—Freestyle Filters, Gallery, Recording Quality, Updates, Cockpit, Profiles & Setup—now translates completely.",
        "Seamless In-Place APK Updates" to "Fixed GitHub Release signing & added pre-install APK signature verification so future updates install right over your old version.",
        "App Color Mixer (2–3 Colors) & Custom Fonts" to "Pick curated color themes or mix 2–3 custom colors, plus choose gaming font presets or upload your own .ttf/.otf from DaFont.com.",
        "Custom Floating Bubble & Test Background" to "Resize and restyle the floating bubble (5 shapes + custom icon) and upload your own custom background image in the Test Sandbox.",
        "Animated Virtual Keyboard + Auto-Correct" to "Built-in keyboard now features crisp key-press pop-up animations and a Gboard-style EN/TH word suggestion & auto-correct bar."
    ); internal set

    // Cloud Layout Save (Optional Google Sign-In)
    var cloudSaveTitle: String = "CLOUD SAVE & SYNC"; internal set
    var cloudSaveSubtitle: String = "Optional Google account for Cloud Save — automatically backs up & restores your layouts, Freestyle filters, crosshair, and theme across updates."; internal set
    var cloudSaveSignInButton: String = "Sign up / Log in with Google"; internal set
    var cloudSaveSigningIn: String = "Signing in with Google..."; internal set
    var cloudSaveSignInShort: String = "Sign In"; internal set
    var cloudSaveActiveBadge: String = "AUTO-BACKUP ON"; internal set
    var cloudSaveSignedOutLabel: String = "Cloud Save: Local Only"; internal set
    var cloudSaveSignedOutHint: String = "Sign in with Google to sync layouts, filters, crosshair & theme"; internal set
    var cloudSaveSyncing: String = "Syncing layouts, filters & theme..."; internal set
    var cloudSaveBackedUpSuffix: String = "layouts + theme synced"; internal set
    var cloudSaveAutoSaveHint: String = "Layouts, filters, crosshair & theme auto-save to cloud"; internal set
    var cloudSaveBackupNow: String = "Back Up Now"; internal set
    var cloudSaveRestore: String = "Restore"; internal set
    var cloudSaveSyncShort: String = "Sync"; internal set
    var cloudSaveSignOut: String = "Sign Out"; internal set
    var cloudSaveSignOutNote: String = "Signing out keeps all layouts & settings on this device."; internal set

    // Gyroscope Aiming (Tilt-to-Aim)
    var gyroAimTitle: String = "GYROSCOPE AIMING (TILT-TO-AIM)"; internal set
    var gyroAimSubtitle: String = "Tilt your phone to fine-tune camera aim using the hardware gyroscope sensor."; internal set
    var gyroEnableLabel: String = "Enable Gyroscope Aiming"; internal set
    var gyroModeLabel: String = "Activation Mode"; internal set
    var gyroModeAlways: String = "Always On"; internal set
    var gyroModeHoldAds: String = "Hold ADS (LT / RMB)"; internal set
    var gyroSensitivityLabel: String = "Gyro Sensitivity"; internal set
    var gyroDeadzoneLabel: String = "Deadzone Filter"; internal set
    var gyroSmoothingLabel: String = "Motion Smoothing"; internal set
    var gyroInvertX: String = "Invert X"; internal set
    var gyroInvertY: String = "Invert Y"; internal set
    var gyroNoHardware: String = "No hardware gyroscope detected on this device."; internal set
    var gyroHudTile: String = "Gyro Aim"; internal set

    // Performance HUD Overlay
    var perfHudTitle: String = "PERFORMANCE HUD OVERLAY"; internal set
    var perfHudSubtitle: String = "Compact draggable GeForce-style bar showing live FPS, Battery %, and Temperature over games."; internal set
    var perfHudEnableLabel: String = "Show Performance HUD"; internal set
    var perfHudShowFps: String = "FPS"; internal set
    var perfHudShowBattery: String = "Battery"; internal set
    var perfHudShowTemp: String = "Temp °C"; internal set
    var perfHudShowRam: String = "RAM %"; internal set
    var perfHudOpacityLabel: String = "HUD Opacity"; internal set
    var perfHudTile: String = "Perf HUD"; internal set

    // 6-Digit Layout Share Codes
    var shareCodeAction: String = "Share Code"; internal set
    var shareCodeEnterButton: String = "Share Code"; internal set
    var shareCodeCreateTitle: String = "6-DIGIT SHARE CODE"; internal set
    var shareCodeCreateSubtitle: String = "Give this temporary 5-minute code to a friend so they can import a copy of this layout."; internal set
    var shareCodeGenerating: String = "Generating share code..."; internal set
    var shareCodeCopyButton: String = "Copy Code"; internal set
    var shareCodeCopied: String = "Share code copied!"; internal set
    var shareCodeExpiresNote: String = "Expires automatically in 5 minutes"; internal set
    var shareCodeImportTitle: String = "IMPORT BY SHARE CODE"; internal set
    var shareCodeImportSubtitle: String = "Enter a friend's 6-character layout code to save a copy into your profiles."; internal set
    var shareCodeInputLabel: String = "6-character code (e.g. NXP48K)"; internal set
    var shareCodeImportButton: String = "Import Layout"; internal set
    var shareCodeImporting: String = "Fetching shared layout..."; internal set
    var shareCodeSignInRequired: String = "Sign in with Google first to create or import 6-digit share codes."; internal set
    var shareCodeNotFound: String = "Code not found or expired. Ask your friend for a new code."; internal set

    // Live Edit Over Game
    var liveEditTile: String = "Edit Layout"; internal set
    var liveEditToolbarTitle: String = "LIVE EDIT"; internal set
    var liveEditDragHint: String = "Drag controls directly over the game • Tap to adjust"; internal set
    var liveEditSizeLabel: String = "Size"; internal set
    var liveEditOpacityLabel: String = "Alpha"; internal set
    var liveEditSnapLabel: String = "Snap"; internal set
    var liveEditSaveButton: String = "Save"; internal set
    var liveEditCancelButton: String = "Cancel"; internal set
    var liveEditSavedToast: String = "Layout saved!"; internal set

    // Better GFN Web Player & Built-in Region Bypass
    var webPlayerTitle: String = "Better GFN Web Player"; internal set
    var webPlayerSubtitle: String = "Play GeForce NOW inside XenosPad with direct virtual gamepad/KB/mouse injection, CSS filters, live WebRTC stats & built-in Region Bypass — zero extra permissions needed."; internal set
    var webPlayerLaunchButton: String = "Play GeForce NOW"; internal set
    var webPlayerZeroPermBadge: String = "ZERO EXTRA PERMS"; internal set
    var webPlayerCardHeader: String = "BETTER GFN WEB PLAYER & REGION BYPASS"; internal set
    var webPlayerRegionHeader: String = "Built-in Region Bypass (Better xCloud Style)"; internal set
    var webPlayerRegionNote: String = "Sends regional X-Forwarded-For, Accept-Language, and timezone headers to GeForce NOW endpoints (header bypass, not a remote VPN tunnel)."; internal set
    var webPlayerUserAgentLabel: String = "Browser User-Agent"; internal set
    var webPlayerSpoofLocaleTitle: String = "Match Region Timezone & Language"; internal set
    var webPlayerSpoofLocaleSub: String = "Aligns browser timezone & locale with your chosen Region Bypass"; internal set
    var webPlayerVirtualPadTitle: String = "Virtual Xbox 360 Gamepad API"; internal set
    var webPlayerVirtualPadSub: String = "Exposes XenosPad controls directly to play.geforcenow.com as a native controller"; internal set
    var webPlayerStretchTitle: String = "Stretch Video to Fill Screen"; internal set
    var webPlayerStretchSub: String = "Removes side black bars on wide 20:9 phone displays"; internal set
    var webPlayerStatsHudTitle: String = "Live WebRTC Stream Stats HUD"; internal set
    var webPlayerStatsHudSub: String = "Shows real stream FPS, ping (RTT), bitrate, and packet loss"; internal set
    var webPlayerHideControls: String = "Hide Pad"; internal set
    var webPlayerShowControls: String = "Show Pad"; internal set
    var webPlayerReload: String = "Reload"; internal set
    var webPlayerExit: String = "Exit"; internal set
    var webPlayerStretchShort: String = "Stretch"; internal set
    var webPlayerAuthPopupTitle: String = "Sign-In Window"; internal set
}

private inline fun buildNexusStrings(block: NexusStrings.() -> Unit): NexusStrings =
    NexusStrings().apply(block)

object AppLocalization {

    private val englishCached: NexusStrings by lazy { NexusStrings() }
    private val thaiCached: NexusStrings by lazy { buildThaiStrings() }
    private val spanishCached: NexusStrings by lazy { buildSpanishStrings() }
    private val japaneseCached: NexusStrings by lazy { buildJapaneseStrings() }
    private val chineseCached: NexusStrings by lazy { buildChineseStrings() }

    fun resolveEffectiveLanguage(selected: AppLanguage, systemLocale: Locale? = null): AppLanguage {
        if (selected != AppLanguage.SYSTEM) return selected
        val locale = systemLocale
            ?: runCatching { ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0] }.getOrNull()
            ?: Locale.getDefault()
        return when (locale.language.lowercase(Locale.ROOT)) {
            "th" -> AppLanguage.THAI
            "es" -> AppLanguage.SPANISH
            "ja" -> AppLanguage.JAPANESE
            "zh" -> AppLanguage.CHINESE
            else -> AppLanguage.ENGLISH
        }
    }

    fun stringsFor(selected: AppLanguage, systemLocale: Locale? = null): NexusStrings {
        return when (resolveEffectiveLanguage(selected, systemLocale)) {
            AppLanguage.THAI -> thaiCached
            AppLanguage.SPANISH -> spanishCached
            AppLanguage.JAPANESE -> japaneseCached
            AppLanguage.CHINESE -> chineseCached
            else -> englishCached
        }
    }

    private fun buildThaiStrings(): NexusStrings = buildNexusStrings {
        navCockpit = "หน้าหลัก"
        navProfiles = "โปรไฟล์"
        navVisuals = "ฟิลเตอร์"
        navGallery = "แกลเลอรี"
        navSandbox = "ทดสอบ"
        navSetup = "ตั้งค่า"

        startOverlay = "เริ่มโอเวอร์เลย์"
        stopOverlay = "หยุดโอเวอร์เลย์"
        launchOverlay = "เปิดโอเวอร์เลย์"
        commonEdit = "แก้ไข"
        commonCustomize = "ปรับแต่ง"
        commonCancel = "ยกเลิก"
        commonClose = "ปิด"
        commonReset = "รีเซ็ต"
        commonEnable = "เปิดใช้งาน"
        commonEnabled = "เปิดอยู่"
        commonStop = "หยุด"
        commonOverlay = "โอเวอร์เลย์"
        commonAll = "ทั้งหมด"
        commonActive = "กำลังใช้งาน"
        commonReady = "พร้อมใช้"
        commonGrant = "อนุญาต"
        commonSelect = "เลือก"
        commonSetup = "ตั้งค่า"
        commonLaunch = "เปิดแอป"
        commonOpenSettings = "เปิดการตั้งค่า"
        commonNotNow = "ไว้ทีหลัง"
        commonGranted = "อนุญาตแล้ว"
        commonShare = "แชร์"
        commonDelete = "ลบ"
        commonControlsSuffix = "ปุ่ม"

        cockpitOverlayLive = "โอเวอร์เลย์กำลังทำงาน"
        cockpitArmed = "พร้อมใช้งาน"
        cockpitIntegrationHeader = "การเชื่อมต่อ GEFORCE NOW และระบบ"
        cockpitOverlayEngineTitle = "ระบบแสดงโอเวอร์เลย์ทับหน้าจอ"
        cockpitOverlayEngineGranted = "ได้รับสิทธิ์แสดงทับแอปอื่นเรียบร้อยแล้ว"
        cockpitOverlayEngineMissing = "จำเป็นต้องเปิดเพื่อแสดงปุ่มเสมือนทับ GeForce NOW"
        cockpitImeTitle = "จอยเกมและคีย์บอร์ดในตัว (IME)"
        cockpitImeSelected = "พร้อมส่งปุ่มเกมและใช้งานคีย์บอร์ดพิมพ์ในตัว"
        cockpitImeEnabled = "เปิดไว้แล้ว — แตะเพื่อเลือกเป็นคีย์บอร์ดหลัก"
        cockpitImeDisabled = "เปิดใช้งาน XenosPad IME สำหรับปุ่มเกมและคีย์บอร์ด"
        cockpitMouseTitle = "ระบบคลิกเมาส์และเลื่อนลูกกลิ้ง"
        cockpitMouseReady = "เปิดอยู่ — คลิกซ้าย ขวา กลาง และเลื่อนเมาส์พร้อมใช้งาน"
        cockpitMouseMissing = "เปิดบริการช่วยการเข้าถึงสำหรับ LMB, RMB, MMB และการเลื่อน"
        cockpitGfnTitle = "แอปเป้าหมาย GeForce NOW"
        cockpitGfnInstalled = "ตรวจพบ GeForce NOW — แตะเพื่อเปิดทันที"
        cockpitGfnMissing = "เปิด Play Store หรือทดสอบปุ่มในหน้าทดสอบ (Sandbox)"
        cockpitQuickProfile = "สลับโปรไฟล์ด่วน"
        cockpitSavedSuffix = "รายการ"
        cockpitVibrationHeader = "การสั่นเมื่อกดปุ่ม"
        cockpitActiveTopology = "ผังปุ่มที่ใช้งานอยู่"
        cockpitMappedSuffix = "ปุ่มที่ตั้งค่าไว้"
        cockpitTapCustomize = "แตะเพื่อปรับตำแหน่งปุ่ม"
        cockpitLayoutStudio = "สตูดิโอจัดปุ่ม"
        cockpitTestControls = "ทดสอบปุ่ม"

        vibNoneTitle = "ปิดการสั่น"
        vibNoneSub = "ปิดสนิท — ไม่สั่นเมื่อกดปุ่ม"
        vibLightTitle = "สั่นเบา"
        vibLightSub = "ความรู้สึกนุ่มนวลเหมือนพิมพ์คีย์บอร์ด"
        vibStrongTitle = "สั่นคมชัด"
        vibStrongSub = "ตอบสนองแน่นชัดเจนทุกการกด"

        profilesTitle = "โปรไฟล์ปุ่มและคีย์บอร์ด"
        profilesSubtitle = "ปรับแต่งเลย์เอาต์แยกตามเกมสำหรับ GeForce NOW และเกม Android"
        profilesImport = "นำเข้า"
        profilesSearchHint = "ค้นหาชื่อเกมหรือโปรไฟล์..."
        profilesEmptyTitle = "ไม่พบโปรไฟล์ที่ค้นหา"
        profilesEmptySub = "สร้างเลย์เอาต์ใหม่หรือล้างตัวกรองประเภทเกม"
        profilesArm = "เลือกใช้"
        profilesNewTitle = "สร้างโปรไฟล์เลย์เอาต์ใหม่"
        profilesStarterTemplate = "เทมเพลตเริ่มต้น"
        profilesNameLabel = "ชื่อโปรไฟล์"
        profilesTargetGameLabel = "เกมเป้าหมาย"
        profilesGenreTag = "ประเภทเกม"
        profilesCreateAndEdit = "สร้างและแก้ไข"

        visualsGameFilterTab = "ฟิลเตอร์เกม"
        visualsCrosshairTab = "เป้าเล็ง (Crosshair)"
        visualsFilterSubtitle = "ชุดปรับแต่งภาพ GPU OpenGL ES Freestyle 18 โมดูลแบบเรียลไทม์"
        visualsCrosshairSubtitle = "ปรับรูปแบบ สี เส้นขอบ ช่องว่าง ความหนา และอัปโหลดรูปเป้าเล็งเอง"
        filterPanelTitle = "ฟิลเตอร์ภาพเกม"
        filterGpuBadge = "GPU เฉดเดอร์"
        filterOffHint = "ปิดฟิลเตอร์ — แตะ สไตล์ 1–3 เพื่อเปิดใช้งาน"
        filterStyleLabel = "สไตล์"
        filterOffLabel = "ปิด"
        filterGpuEngineActive = "ระบบประมวลผล GPU สด: กำลังทำงาน"
        filterGpuEngineTitle = "GPU เฉดเดอร์ปรับพิกเซลสด"
        filterGpuEngineActiveDesc = "กำลังปรับความคมชัด HDR เงา ชัดตื้น และเอฟเฟกต์ OpenGL บนหน้าจอเกม"
        filterGpuEngineIdleDesc = "เลือก 'แอปเดียว' (เช่น GeForce NOW) เพื่อเปิดเฉดเดอร์ 60fps แท้"
        filterPreviewPhoto = "เลือกรูป"
        filterCompare = "เปรียบเทียบ"
        filterOriginal = "ต้นฉบับ"
        filterFiltered = "ใส่ฟิลเตอร์"
        filterPreviewActiveBanner = "GPU เฉดเดอร์กำลังทำงาน — ปรับภาพระดับพิกเซลแบบเรียลไทม์บนเกมของคุณ"
        filterPreviewIdleBanner = "ตัวอย่างเฉดเดอร์ OpenGL ES 2.0 • เปิด GPU เฉดเดอร์ด้านล่างเพื่อใช้ในเกมจริง"
        filterCatEssential = "หลัก"
        filterCatUtility = "ช่วยเหลือ"
        filterCatArtistic = "ศิลปะ"
        filterModSharpen = "เพิ่มความคมชัด (Sharpen)"
        filterModSharpenPlus = "คมชัดขั้นสูง (Sharpen+)"
        filterModBrightnessContrast = "ความสว่าง / คอนทราสต์"
        filterModDetails = "รายละเอียด (HDR & Clarity)"
        filterModColor = "สีและความสด (Color)"
        filterModColorblind = "โหมดช่วยตาบอดสี"
        filterModNightMode = "โหมดกลางคืน (ลดแสงสีฟ้า)"
        filterModSplitscreen = "แบ่งครึ่งจอเปรียบเทียบ"
        filterModBlackWhite = "ขาวดำ (Black & White)"
        filterModSepia = "ซีเปีย (Sepia)"
        filterModVignette = "ขอบมืด (Vignette)"
        filterModDof = "ชัดตื้น / Tilt-Shift"
        filterModOldFilm = "ฟิล์มเก่า (Old Film)"
        filterModPainterly = "ภาพวาดสีน้ำมัน (Painterly)"
        filterModLetterbox = "แถบดำภาพยนตร์ (Letterbox)"
        filterModMood = "โทนสีสำเร็จรูป (Mood)"
        filterModSpecialFx = "เอฟเฟกต์พิเศษ (Special FX)"
        filterModStickers = "สติกเกอร์ (Stickers)"

        crosshairTitle = "เป้าเล็งช่วยยิง (CROSSHAIR)"
        crosshairEnable = "เปิดใช้งานเป้าเล็ง"
        crosshairPresets = "พรีเซ็ต"
        crosshairColor = "สีเป้าเล็ง"
        crosshairOutline = "เส้นขอบ"
        crosshairThickness = "ความหนาเส้น"
        crosshairLength = "ความยาวเส้น"
        crosshairGap = "ช่องว่างตรงกลาง"
        crosshairOpacity = "ความทึบแสง"
        crosshairCenterDot = "จุดตรงกลาง"
        crosshairUploadImage = "อัปโหลดรูป"
        crosshairReset = "รีเซ็ต"
        crosshairSize = "ขนาดรูปเป้า"

        galleryTitle = "บันทึกหน้าจอและแกลเลอรี"
        gallerySubtitle = "รีเพลย์ย้อนหลังสไตล์ GeForce (10 วิ – 1 ชม.), อัดวิดีโอ และแคปภาพหน้าจอ"
        galleryScreenshotsTab = "ภาพหน้าจอ"
        galleryVideosTab = "วิดีโอและรีเพลย์"
        galleryReplayOn = "รีเพลย์เปิดอยู่"
        gallerySaveClipPrefix = "บันทึกย้อนหลัง"
        gallerySaveClipSuffix = "ตอนนี้"
        galleryReplayBufferHeader = "ความยาวบัฟเฟอร์รีเพลย์ย้อนหลัง"
        galleryCaptureQualityHeader = "คุณภาพการบันทึก"
        galleryEmptyTitle = "ยังไม่มีภาพหรือวิดีโอ"
        galleryEmptySubtitle = "กดแคปหน้าจอ รีเพลย์ย้อนหลัง หรือบันทึกวิดีโอด้านบน — หรือจากเมนูฟองลอยขณะเล่นเกม"
        galleryCannotDecodeImage = "ไม่สามารถเปิดรูปภาพนี้ได้"
        galleryCannotPlayVideo = "ไม่สามารถเล่นคลิปวิดีโอนี้ได้"
        galleryDeleteBroken = "ลบคลิปที่เสียหาย"

        sandboxTitle = "สนามทดสอบปุ่ม (Sandbox)"
        sandboxNoProfile = "ยังไม่ได้เลือกโปรไฟล์"
        sandboxCustomBg = "เปลี่ยนพื้นหลัง"
        sandboxResetBg = "คืนค่าเดิม"
        sandboxTelemetryHeader = "บันทึกสัญญาณปุ่มแบบสด (สัมผัส + ฮาร์ดแวร์)"
        sandboxTelemetryEmpty = "แตะปุ่ม ลากจอย หรือกดคีย์บอร์ดด้านบนเพื่อตรวจสอบค่า KeyEvent และแกนบังคับ"
        sandboxFilterOff = "ปิดฟิลเตอร์"

        setupTitle = "ตั้งค่าโอเวอร์เลย์และระบบ"
        setupSubtitle = "ปรับแต่งธีมสี ฟอนต์ ฟองลอย ภาษา คุณภาพวิดีโอ อัปเดตแอป และสิทธิ์ระบบ"
        languageCardTitle = "ภาษาของแอป (APP LANGUAGE)"
        languageSubtitle = "ค่าเริ่มต้นจะใช้ภาษาของเครื่องโดยอัตโนมัติ"
        targetModeCardTitle = "โหมดเกมเป้าหมาย (COMPATIBILITY MODE)"
        targetModeSubtitle = "เลือกว่าจะใช้เลย์เอาต์กับ GeForce NOW หรือเกม Android อื่น ๆ"
        targetModeGfnTitle = "GeForce NOW (เกมคลาวด์ PC)"
        targetModeGfnSubtitle = "ใช้รหัสสแกนคีย์ Linux + เลื่อนเมาส์กลางจอและคำสั่งสัมผัสสำหรับ GFN"
        targetModeAndroidTitle = "เกม Android (Roblox, Minecraft ฯลฯ)"
        targetModeAndroidSubtitle = "ใช้คีย์ Android มาตรฐาน + เลื่อนมุมกล้องฝั่งขวาและคลิกเมาส์สำหรับเกมมือถือ"

        themeCardTitle = "ธีมสีแอปและตัวผสมสี (2–3 สี)"
        themeCardSubtitle = "เลือกชุดสีสำเร็จรูป หรือผสมสีเอง 2–3 สีให้เข้ากับสไตล์ของคุณ"
        themePresetsLabel = "ชุดสีสำเร็จรูป"
        themeCustomMixTitle = "ผสมสีเอง (Custom Mix)"
        themeMix2Colors = "ผสม 2 สี"
        themeMix3Colors = "ผสม 3 สี"
        themePrimaryColor = "สีหลัก (Primary)"
        themeSecondaryColor = "สีรอง (Secondary)"
        themeTertiaryColor = "สีเน้น (สีที่ 3)"
        fontCardTitle = "ปรับแต่งฟอนต์ (FONT)"
        fontCardSubtitle = "เลือกฟอนต์เกมมิ่งในตัว หรืออัปโหลดไฟล์ .ttf / .otf เอง (เช่น จาก DaFont.com)"
        fontUploadButton = "อัปโหลดฟอนต์ (.ttf / .otf)"
        fontUploadedLabel = "ฟอนต์ที่อัปโหลด"
        fontSamplePreview = "XENOSPAD • พร้อมลุยเกม 12345 — สวัสดีครับ"
        bubbleCardTitle = "ขนาดและดีไซน์ฟองลอย (FLOATING BUBBLE)"
        bubbleCardSubtitle = "ปรับรูปทรง ขนาด ความโปร่งแสง สี หรืออัปโหลดรูปไอคอนฟองลอยเอง"
        bubbleStyleLabel = "รูปแบบฟองลอย"
        bubbleSizeLabel = "ขนาดฟองลอย"
        bubbleOpacityLabel = "ความทึบแสงฟองลอย"
        bubbleStatusDotLabel = "แสดงจุดสถานะสด"
        bubbleStatusDotSub = "แสดงจุดสถานะเมื่อกำลังอัดวิดีโอ รีเพลย์ ล็อก หรือซ่อนปุ่ม"
        bubbleUseThemeAccent = "ใช้สีตามธีมแอป"
        bubbleUseThemeAccentSub = "ซิงก์สีขอบและไอคอนฟองลอยกับธีมสีแอปอัตโนมัติ"
        bubbleCustomColorLabel = "สีขอบฟองลอย"
        bubbleUploadIconButton = "อัปโหลดรูปฟองลอย"
        bubbleLivePreview = "ตัวอย่างฟองลอยแบบสด"

        recQualityTitle = "คุณภาพการบันทึกหน้าจอและรีเพลย์"
        recQualitySubtitle = "เลือกความละเอียดและเฟรมเรต (FPS) สำหรับวิดีโอและรีเพลย์ย้อนหลัง"
        recResolutionLabel = "ความละเอียด (Resolution)"
        recFpsLabel = "เฟรมเรต (FPS)"
        recEncoderProfileLabel = "โปรไฟล์เข้ารหัส H.264 เป้าหมาย"
        recAutoFallbackNote = "ปรับลดอัตโนมัติ: หากชิปเครื่องไม่รองรับ 1440p หรือ 90/120 FPS แอปจะลดลงมาที่ค่าระดับสูงสุดที่เสถียรให้อัตโนมัติ"

        updatesCardTitle = "อัปเดตแอป (GITHUB RELEASES)"
        updatesCardSubtitle = "ตรวจสอบเวอร์ชันใหม่บน GitHub Releases และเปิดหน้าดาวน์โหลดรุ่นล่าสุด"
        updatesInstalledVersion = "เวอร์ชันที่ติดตั้งอยู่"
        updatesSigningKeyLabel = "ลายเซ็นคีย์ SHA-256"
        updatesRepoLabel = "คลัง GitHub (owner/repo)"
        updatesRepoPlaceholder = "เช่น yourname/XenosPad"
        updatesRepoAutoHint = "ตั้งค่าอัตโนมัติจาก GitHub Actions ใช้ลายเซ็นเดียวกันทุกเวอร์ชันเพื่อเก็บข้อมูลเดิมไว้ครบ"
        updatesRepoManualHint = "วางชื่อ owner/repo หรือลิงก์ GitHub ระบบจะอัปเดตให้อัตโนมัติ"
        updatesAutoCheckTitle = "ตรวจสอบอัปเดตอัตโนมัติเมื่อเปิดแอป"
        updatesAutoCheckSub = "เช็กเวอร์ชันใหม่บน GitHub Releases เบื้องหลังทุกครั้งที่เปิด XenosPad"
        updatesCheckButton = "ตรวจสอบอัปเดต"
        updatesChecking = "กำลังตรวจสอบ GitHub Releases..."
        updatesUpToDate = "คุณใช้เวอร์ชันล่าสุดแล้ว!"
        updatesNewAvailable = "มีอัปเดตใหม่ ดาวน์โหลดได้ที่ GitHub"
        updatesDownloadAndUpdate = "ดาวน์โหลดที่ GitHub"
        updatesDownloading = "กำลังดาวน์โหลด"
        updatesCancelDownload = "ยกเลิกการดาวน์โหลด"
        updatesReadyTitle = "พร้อมติดตั้ง"
        updatesReadyKeepsData = "ตรวจสอบลายเซ็นตรงกันแล้ว! แตะด้านล่างเพื่ออัปเดตทับทันที โดยเก็บเลย์เอาต์ ฟิลเตอร์ และแกลเลอรีเดิมไว้ครบ"
        updatesReadyNeedPerm = "ตั้งค่าครั้งแรก: Android จะขอสิทธิ์ \"ติดตั้งแอปที่ไม่รู้จัก\" สำหรับ XenosPad จากนั้นกดติดตั้งอีกครั้ง"
        updatesInstallButton = "ติดตั้งอัปเดต (เก็บข้อมูลเดิม)"
        updatesAllowAndInstall = "อนุญาตและติดตั้งอัปเดต"
        updatesSigMismatchTitle = "ต้องลบติดตั้งใหม่ 1 ครั้ง (คีย์เก่าไม่ตรงกัน)"
        updatesSigMismatchBody = "เวอร์ชันเก่าในเครื่องถูกเซ็นด้วยคีย์สุ่มเก่า กรุณาลบแอปเก่า 1 ครั้งแล้วติดตั้งเวอร์ชันนี้ หลังจากนี้ทุกอัปเดตจะติดตั้งทับได้ตลอดโดยไม่ต้องลบอีก!"
        updatesViewWhatsNew = "ดูสิ่งใหม่ในเวอร์ชันนี้ (What's New)"

        setupVibrationCardTitle = "การสั่นเมื่อกดปุ่ม / HAPTICS"
        setupPermsCardTitle = "สิทธิ์โอเวอร์เลย์และคีย์บอร์ด ANDROID"
        setupPerm1Title = "1. แสดงทับแอปอื่น (Display Over Other Apps)"
        setupPerm1Desc = "อนุญาตให้แสดงปุ่มเสมือนและจอยอนาล็อกลอยเหนือ GeForce NOW"
        setupPerm2Title = "2. เปิดใช้งาน XenosPad IME และคีย์บอร์ด"
        setupPerm2Desc = "ส่งคำสั่งปุ่มจริงสำหรับจอย/คีย์บอร์ด และเปิดคีย์บอร์ดพิมพ์ในตัว"
        setupPerm3Title = "3. ระบบคลิกเมาส์และเลื่อน (Accessibility)"
        setupPerm3Desc = "บริการช่วยการเข้าถึงสำหรับคลิกซ้าย คลิกขวา คลิกกลาง และเลื่อนลูกกลิ้ง"
        setupEnableIme = "เปิด IME"
        setupImeSelected = "เลือก IME แล้ว ✓"
        setupSelectIme = "เลือก IME หลัก"
        setupAlertsReady = "แจ้งเตือนพร้อม"
        setupAllowAlerts = "เปิดแจ้งเตือน"
        keyboardOptionsTitle = "คีย์บอร์ดและโหมดลอยตัว"
        floatingKeyboardTitle = "ใช้คีย์บอร์ดแบบลอยตัวเป็นค่าเริ่มต้น"
        floatingKeyboardSubtitle = "แสดงคีย์บอร์ดลอยเหนือแถบงาน (Taskbar) เพื่อไม่ให้ปุ่มถูกบัง"
        openFloatingKeyboard = "เปิดคีย์บอร์ดลอยตัว"
        setupNotationTitle = "สัญลักษณ์ปุ่มจอย (NOTATION)"
        setupHudPrefsTitle = "การตั้งค่าหน้าจอและการควบคุม"
        setupGlobalOpacity = "ความทึบแสงปุ่มทั้งหมด"
        gridSizeTitle = "ขนาดช่องตารางจัดวาง"
        snapToGridTitle = "จัดปุ่มให้ตรงตามตาราง"
        snapToGridSubtitle = "ดูดปุ่มเข้าเส้นตารางเมื่อปล่อยนิ้วในหน้าแก้ไขเลย์เอาต์"
        setupTelemetryTitle = "แสดงหน้าต่างสัญญาณปุ่ม (Telemetry)"
        setupTelemetrySub = "แสดงค่า KeyEvent และแกนจอยแบบเรียลไทม์ในหน้าทดสอบ"
        setupAutoLaunchTitle = "เปิด GeForce NOW อัตโนมัติ"
        setupAutoLaunchSub = "เปิดแอป GeForce NOW ทันทีเมื่อเริ่มโอเวอร์เลย์"
        setupDarkModeTitle = "ธีมมืด (Tactical Dark)"
        setupDarkModeSub = "โทนสีเข้มสบายตา คอนทราสต์สูงสำหรับเล่นเกม"
        setupInfoNote = "หมายเหตุ: บนเครื่อง Android ที่ไม่ได้รูท ปุ่มเสมือนจะส่ง KeyEvent จริงผ่าน XenosPad IME และรองรับมัลติทัชพร้อมกันทุกปุ่มและจอยอนาล็อก"

        permDialogTitle = "เปิดสิทธิ์แสดงโอเวอร์เลย์ทับหน้าจอ"
        permDialogBody = "เพื่อให้ปุ่มเสมือนและคีย์ลัดแสดงทับ GeForce NOW ได้ กรุณาเปิดสิทธิ์ \"แสดงทับแอปอื่น\" ให้กับ XenosPad ในการตั้งค่า Android"

        menuChooseLayoutHeader = "เลือกรูปแบบเลย์เอาต์"
        menuDragKbHint = "ลากคีย์บอร์ดขึ้นเหนือแถบงานได้อิสระ"
        menuHidePad = "ซ่อนปุ่ม"
        menuShowPad = "แสดงปุ่ม"
        menuSwitch = "สลับโปรไฟล์"
        menuLayouts = "เลือกรูปแบบ"
        menuTargetMode = "โหมดเกม"
        menuFilter = "ฟิลเตอร์สี"
        menuCrosshair = "เป้าเล็ง"
        menuScreenshot = "แคปหน้าจอ"
        menuReplay = "รีเพลย์ย้อนหลัง"
        menuSaveReplay = "บันทึกรีเพลย์"
        menuRecord = "บันทึกวิดีโอ"
        menuStopRecord = "หยุดบันทึก"
        menuKeyboard = "คีย์บอร์ด"
        menuFloatKb = "คีย์บอร์ดลอย"
        menuUnstick = "รีเซ็ตปุ่มค้าง"
        menuLock = "ล็อกปุ่ม"
        menuUnlock = "ปลดล็อก"
        menuStop = "ปิดโอเวอร์เลย์"
        menuOpenApp = "เปิดแอป XenosPad"

        whatsNewBadge = "มีอะไรใหม่"
        whatsNewTitle = "ยินดีต้อนรับสู่ XenosPad"
        whatsNewSubtitle = "อัปเดตและฟีเจอร์ใหม่ทั้งหมดในเวอร์ชันนี้:"
        whatsNewGotIt = "เยี่ยมเลย เริ่มใช้งาน!"
        whatsNewItems = listOf(
            "แปลภาษาไทยและทุกภาษาครบทั้งแอป" to "แปลครบทุกหน้า ทั้งฟิลเตอร์ Freestyle, แกลเลอรี, คุณภาพวิดีโอ, อัปเดตแอป, หน้าหลัก, โปรไฟล์ และตั้งค่า",
            "อัปเดตทับเวอร์ชันเดิมได้จริง ไม่ต้องลบแอป" to "แก้ระบบเซ็นลายเซ็น APK บน GitHub และตรวจเช็กลายเซ็นก่อนติดตั้ง เพื่อให้ทุกอัปเดตในอนาคตติดตั้งทับได้ทันที",
            "ผสมสีธีมแอปเอง 2–3 สี และเปลี่ยนฟอนต์ได้" to "เลือกชุดสีหรือผสมสีเอง 2–3 สี พร้อมเปลี่ยนฟอนต์เกมมิ่งหรืออัปโหลดไฟล์ .ttf/.otf จาก DaFont.com",
            "ปรับแต่งฟองลอยและภาพพื้นหลังหน้าทดสอบ" to "เปลี่ยนขนาดและดีไซน์ฟองลอย 5 แบบ (+ อัปโหลดรูปเอง) และอัปโหลดรูปพื้นหลังทดสอบเองในหน้า Sandbox",
            "คีย์บอร์ดในตัวมีแอนิเมชัน + เดาคำอัตโนมัติ" to "เพิ่มแอนิเมชันเด้งเมื่อกดปุ่ม ป๊อปอัปตัวอักษร และแถบแนะนำคำ/แก้คำผิดภาษาไทย-อังกฤษสไตล์ Gboard"
        )

        cloudSaveTitle = "สำรองคลาวด์ & ซิงก์ (CLOUD SAVE)"
        cloudSaveSubtitle = "ล็อกอิน Google เฉพาะเพื่อสำรองคลาวด์ — สำรองและกู้คืนเลย์เอาต์ ฟิลเตอร์ เป้าเล็ง และธีมอัตโนมัติเมื่ออัปเดตแอป"
        cloudSaveSignInButton = "สมัคร / เข้าสู่ระบบด้วย Google"
        cloudSaveSigningIn = "กำลังเข้าสู่ระบบด้วย Google..."
        cloudSaveSignInShort = "เข้าสู่ระบบ"
        cloudSaveActiveBadge = "สำรองอัตโนมัติเปิดอยู่"
        cloudSaveSignedOutLabel = "คลาวด์เซฟ: เก็บในเครื่องเท่านั้น"
        cloudSaveSignedOutHint = "เข้าสู่ระบบ Google เพื่อซิงก์เลย์เอาต์ ฟิลเตอร์ เป้าเล็ง และธีม"
        cloudSaveSyncing = "กำลังซิงก์เลย์เอาต์ ฟิลเตอร์ และธีม..."
        cloudSaveBackedUpSuffix = "เลย์เอาต์ + ธีมซิงก์แล้ว"
        cloudSaveAutoSaveHint = "เลย์เอาต์ ฟิลเตอร์ เป้าเล็ง และธีมบันทึกขึ้นคลาวด์อัตโนมัติ"
        cloudSaveBackupNow = "สำรองตอนนี้"
        cloudSaveRestore = "กู้คืนจากคลาวด์"
        cloudSaveSyncShort = "ซิงก์"
        cloudSaveSignOut = "ออกจากระบบ"
        cloudSaveSignOutNote = "การออกจากระบบจะยังคงเลย์เอาต์และการตั้งค่าทั้งหมดไว้ในเครื่องนี้"

        gyroAimTitle = "เล็งด้วยไจโรสโคป (TILT-TO-AIM)"
        gyroAimSubtitle = "เอียงโทรศัพท์เพื่อปรับมุมกล้องอย่างละเอียดด้วยเซ็นเซอร์ไจโรสโคปจริง"
        gyroEnableLabel = "เปิดใช้งานการเล็งด้วยไจโร"
        gyroModeLabel = "โหมดการทำงาน"
        gyroModeAlways = "เปิดตลอดเวลา"
        gyroModeHoldAds = "เฉพาะตอนกดเล็ง (LT / RMB)"
        gyroSensitivityLabel = "ความไวไจโร"
        gyroDeadzoneLabel = "กันสั่น (Deadzone)"
        gyroSmoothingLabel = "ความนุ่มนวล"
        gyroInvertX = "กลับด้านแกน X"
        gyroInvertY = "กลับด้านแกน Y"
        gyroNoHardware = "อุปกรณ์นี้ไม่พบเซ็นเซอร์ไจโรสโคป"
        gyroHudTile = "ไจโรเล็ง"

        perfHudTitle = "แถบแสดงสถานะเครื่อง (PERF HUD)"
        perfHudSubtitle = "แถบลอยสไตล์ GeForce ลากย้ายได้ แสดง FPS แบตเตอรี่ และอุณหภูมิแบบเรียลไทม์บนเกม"
        perfHudEnableLabel = "แสดงแถบ Performance HUD"
        perfHudShowFps = "FPS"
        perfHudShowBattery = "แบตเตอรี่"
        perfHudShowTemp = "อุณหภูมิ °C"
        perfHudShowRam = "RAM %"
        perfHudOpacityLabel = "ความทึบของ HUD"
        perfHudTile = "สถานะเครื่อง"

        shareCodeAction = "รหัสแชร์"
        shareCodeEnterButton = "ใส่รหัสแชร์"
        shareCodeCreateTitle = "รหัสแชร์เลย์เอาต์ 6 หลัก"
        shareCodeCreateSubtitle = "ส่งรหัสชั่วคราว 5 นาทีนี้ให้เพื่อนเพื่อนำเข้าเลย์เอาต์ของคุณได้ทันที"
        shareCodeGenerating = "กำลังสร้างรหัสแชร์..."
        shareCodeCopyButton = "คัดลอกรหัส"
        shareCodeCopied = "คัดลอกรหัสแชร์แล้ว!"
        shareCodeExpiresNote = "รหัสจะหมดอายุอัตโนมัติใน 5 นาที"
        shareCodeImportTitle = "นำเข้าด้วยรหัสแชร์"
        shareCodeImportSubtitle = "กรอกรหัส 6 หลักจากเพื่อนเพื่อบันทึกลงในโปรไฟล์ของคุณ"
        shareCodeInputLabel = "รหัส 6 หลัก (เช่น NXP48K)"
        shareCodeImportButton = "นำเข้าเลย์เอาต์"
        shareCodeImporting = "กำลังดึงข้อมูลเลย์เอาต์..."
        shareCodeSignInRequired = "กรุณาเข้าสู่ระบบ Google ก่อนสร้างหรือนำเข้ารหัสแชร์ 6 หลัก"
        shareCodeNotFound = "ไม่พบรหัสนี้หรือรหัสหมดอายุแล้ว ลองขอรหัสใหม่จากเพื่อน"

        liveEditTile = "แก้เลย์เอาต์"
        liveEditToolbarTitle = "แก้ไขบนเกมสด"
        liveEditDragHint = "ลากปุ่มบนหน้าจอเกมได้ทันที • แตะเพื่อปรับขนาด/ความทึบ"
        liveEditSizeLabel = "ขนาด"
        liveEditOpacityLabel = "ความทึบ"
        liveEditSnapLabel = "ดูดเส้นกริด"
        liveEditSaveButton = "บันทึก"
        liveEditCancelButton = "ยกเลิก"
        liveEditSavedToast = "บันทึกเลย์เอาต์เรียบร้อย!"

        webPlayerTitle = "Better GFN เว็บเพลเยอร์"
        webPlayerSubtitle = "เล่น GeForce NOW ใน XenosPad โดยตรง ส่งปุ่มจอย/คีย์บอร์ด/เมาส์เสถียรกว่า ฟิลเตอร์ CSS สด สถิติ WebRTC และบายพาสโซนในตัว ไม่ต้องขอสิทธิ์เสริม"
        webPlayerLaunchButton = "เปิดเล่น GeForce NOW"
        webPlayerZeroPermBadge = "ไม่ต้องขอสิทธิ์เพิ่ม"
        webPlayerCardHeader = "BETTER GFN เว็บเพลเยอร์ & บายพาสโซน"
        webPlayerRegionHeader = "บายพาสโซนในตัว (สไตล์ Better xCloud)"
        webPlayerRegionNote = "ส่งเฮดเดอร์ X-Forwarded-For, Accept-Language และโซนเวลาตามภูมิภาคไปยังเซิร์ฟเวอร์ GeForce NOW (บายพาสระดับเฮดเดอร์ ไม่ใช่เซิร์ฟเวอร์ VPN ภายนอก)"
        webPlayerUserAgentLabel = "โปรไฟล์เบราว์เซอร์ (User-Agent)"
        webPlayerSpoofLocaleTitle = "ปรับโซนเวลาและภาษาให้ตรงภูมิภาค"
        webPlayerSpoofLocaleSub = "ปรับโซนเวลาและภาษาของเบราว์เซอร์ให้สอดคล้องกับโซนที่เลือก"
        webPlayerVirtualPadTitle = "จอยจำลอง Xbox 360 (Gamepad API)"
        webPlayerVirtualPadSub = "ส่งอินพุตจากปุ่ม XenosPad เข้า play.geforcenow.com เป็นจอยเกมโดยตรง"
        webPlayerStretchTitle = "ขยายภาพเต็มหน้าจอ (ตัดขอบดำ)"
        webPlayerStretchSub = "ยืดภาพวิดีโอให้เต็มจอมือถือ 20:9 ไม่มีแถบดำด้านข้าง"
        webPlayerStatsHudTitle = "แถบสถิติสตรีม WebRTC สด"
        webPlayerStatsHudSub = "แสดงค่า FPS สตรีม, ปิง (RTT), บิตเรต และ Packet Loss แบบเรียลไทม์"
        webPlayerHideControls = "ซ่อนปุ่ม"
        webPlayerShowControls = "แสดงปุ่ม"
        webPlayerReload = "รีโหลด"
        webPlayerExit = "ออก"
        webPlayerStretchShort = "เต็มจอ"
        webPlayerAuthPopupTitle = "หน้าต่างเข้าสู่ระบบ"
    }

    private fun buildSpanishStrings(): NexusStrings = buildNexusStrings {
        navCockpit = "Panel"
        navProfiles = "Perfiles"
        navVisuals = "Filtros"
        navGallery = "Galería"
        navSandbox = "Pruebas"
        navSetup = "Ajustes"

        startOverlay = "Iniciar Overlay"
        stopOverlay = "Detener Overlay"
        launchOverlay = "Lanzar Overlay"
        commonEdit = "Editar"
        commonCustomize = "Personalizar"
        commonCancel = "Cancelar"
        commonClose = "Cerrar"
        commonReset = "Reiniciar"
        commonEnable = "Activar"
        commonEnabled = "Activado"
        commonStop = "Detener"
        commonOverlay = "Overlay"
        commonAll = "Todos"
        commonActive = "ACTIVO"
        commonReady = "LISTO"
        commonGrant = "PERMITIR"
        commonSelect = "ELEGIR"
        commonSetup = "AJUSTAR"
        commonLaunch = "ABRIR"
        commonOpenSettings = "Abrir Ajustes"
        commonNotNow = "Ahora no"
        commonGranted = "Concedido"
        commonShare = "Compartir"
        commonDelete = "Eliminar"
        commonControlsSuffix = "controles"

        cockpitOverlayLive = "OVERLAY ACTIVO"
        cockpitArmed = "PANEL LISTO"
        cockpitIntegrationHeader = "INTEGRACIÓN GEFORCE NOW"
        cockpitOverlayEngineTitle = "Motor de Superposición"
        cockpitOverlayEngineGranted = "Permiso de mostrar sobre otras apps concedido"
        cockpitOverlayEngineMissing = "Necesario para mostrar botones sobre GeForce NOW"
        cockpitImeTitle = "Gamepad y Teclado IME Integrado"
        cockpitImeSelected = "Activo para teclas de juego y teclado integrado"
        cockpitImeEnabled = "Activado — toca para seleccionarlo como método activo"
        cockpitImeDisabled = "Activa XenosPad IME para teclas y teclado integrado"
        cockpitMouseTitle = "Gestos de Clic y Rueda de Ratón"
        cockpitMouseReady = "Activo — Clic Izquierdo, Derecho, Central y Scroll"
        cockpitMouseMissing = "Activa Accesibilidad de XenosPad para LMB, RMB y MMB"
        cockpitGfnTitle = "Destino GeForce NOW"
        cockpitGfnInstalled = "GeForce NOW detectado — listo para abrir"
        cockpitGfnMissing = "Abre Play Store o prueba en la zona de Pruebas"
        cockpitQuickProfile = "CAMBIO RÁPIDO DE PERFIL"
        cockpitSavedSuffix = "guardados"
        cockpitVibrationHeader = "VIBRACIÓN DE BOTONES"
        cockpitActiveTopology = "DISTRIBUCIÓN ACTIVA"
        cockpitMappedSuffix = "controles asignados"
        cockpitTapCustomize = "Toca para ajustar posiciones"
        cockpitLayoutStudio = "Editor de Diseño"
        cockpitTestControls = "Probar Controles"

        vibNoneTitle = "Sin Vibración"
        vibNoneSub = "Totalmente desactivada al tocar"
        vibLightTitle = "Toque Suave"
        vibLightSub = "Respuesta sutil estilo teclado"
        vibStrongTitle = "Clic Firme"
        vibStrongSub = "Vibración nítida en cada pulsación"

        profilesTitle = "Perfiles de Control y Teclas"
        profilesSubtitle = "Personaliza diseños por juego para GeForce NOW y Android"
        profilesImport = "Importar"
        profilesSearchHint = "Buscar juego o diseño..."
        profilesEmptyTitle = "No se encontraron perfiles"
        profilesEmptySub = "Crea un diseño nuevo o limpia el filtro de género."
        profilesArm = "Activar"
        profilesNewTitle = "Nuevo Perfil de Diseño"
        profilesStarterTemplate = "Plantilla Inicial"
        profilesNameLabel = "Nombre del Perfil"
        profilesTargetGameLabel = "Juego Objetivo"
        profilesGenreTag = "Género"
        profilesCreateAndEdit = "Crear y Editar"

        visualsGameFilterTab = "Filtro Juego"
        visualsCrosshairTab = "Mira (Crosshair)"
        visualsFilterSubtitle = "Suite GPU OpenGL ES Freestyle de 18 módulos en tiempo real"
        visualsCrosshairSubtitle = "Ajusta forma, color, contorno, grosor o sube tu propia imagen"
        filterPanelTitle = "FILTRO DE JUEGO"
        filterGpuBadge = "GPU SHADER"
        filterOffHint = "Filtro apagado — toca Estilo 1–3 para activar"
        filterStyleLabel = "Estilo"
        filterOffLabel = "Off"
        filterGpuEngineActive = "Motor GPU Shader: ACTIVO"
        filterGpuEngineTitle = "Shader GPU por Píxel en Vivo"
        filterGpuEngineActiveDesc = "Enfoque OpenGL, HDR, Sombras, DoF y FX activos en tu partida."
        filterGpuEngineIdleDesc = "Elige 'Una sola app' (GeForce NOW) para shaders reales a 60fps."
        filterPreviewPhoto = "Foto"
        filterCompare = "Comparar"
        filterOriginal = "Original"
        filterFiltered = "FILTRADO"
        filterPreviewActiveBanner = "Shader GPU ACTIVO — filtrado GLSL por píxel en tiempo real"
        filterPreviewIdleBanner = "Vista previa OpenGL ES 2.0 • Activa Shader GPU abajo para el juego"
        filterCatEssential = "Esencial"
        filterCatUtility = "Utilidad"
        filterCatArtistic = "Artístico"
        filterModSharpen = "Nitidez (Sharpen)"
        filterModSharpenPlus = "Nitidez+ (Sharpen+)"
        filterModBrightnessContrast = "Brillo / Contraste"
        filterModDetails = "Detalles (HDR y Claridad)"
        filterModColor = "Color y Vibrancia"
        filterModColorblind = "Daltonismo"
        filterModNightMode = "Modo Nocturno"
        filterModSplitscreen = "Pantalla Dividida"
        filterModBlackWhite = "Blanco y Negro"
        filterModSepia = "Sepia"
        filterModVignette = "Viñeta"
        filterModDof = "Profundidad de Campo"
        filterModOldFilm = "Película Antigua"
        filterModPainterly = "Pictórico"
        filterModLetterbox = "Bandas Cine (Letterbox)"
        filterModMood = "Ambiente (Mood)"
        filterModSpecialFx = "Efectos Especiales"
        filterModStickers = "Pegatinas"

        crosshairTitle = "MIRA EN PANTALLA"
        crosshairEnable = "Activar Mira"
        crosshairPresets = "Preajustes"
        crosshairColor = "Color"
        crosshairOutline = "Contorno"
        crosshairThickness = "Grosor"
        crosshairLength = "Longitud"
        crosshairGap = "Espacio Central"
        crosshairOpacity = "Opacidad"
        crosshairCenterDot = "Punto Central"
        crosshairUploadImage = "Subir Imagen"
        crosshairReset = "Reiniciar"
        crosshairSize = "Tamaño"

        galleryTitle = "Captura y Galería"
        gallerySubtitle = "Repetición instantánea (10s – 1h), grabación manual y capturas."
        galleryScreenshotsTab = "Capturas"
        galleryVideosTab = "Videos y Clips"
        galleryReplayOn = "Replay ON"
        gallerySaveClipPrefix = "Guardar Últimos"
        gallerySaveClipSuffix = "Ahora"
        galleryReplayBufferHeader = "DURACIÓN DE REPETICIÓN INSTANTÁNEA"
        galleryCaptureQualityHeader = "CALIDAD DE GRABACIÓN"
        galleryEmptyTitle = "Sin capturas todavía"
        galleryEmptySubtitle = "Usa Captura, Repetición (10s–1h) o Grabar arriba — o desde la burbuja flotante."
        galleryCannotDecodeImage = "No se pudo cargar la imagen"
        galleryCannotPlayVideo = "No se pudo reproducir este clip"
        galleryDeleteBroken = "Eliminar Clip Dañado"

        sandboxTitle = "Zona de Pruebas"
        sandboxNoProfile = "Sin perfil activo"
        sandboxCustomBg = "Fondo Propio"
        sandboxResetBg = "Fondo Base"
        sandboxTelemetryHeader = "TELEMETRÍA EN VIVO (TÁCTIL + HARDWARE)"
        sandboxTelemetryEmpty = "Toca botones, mueve joysticks o pulsa teclas físicas para ver eventos."
        sandboxFilterOff = "FILTRO OFF"

        setupTitle = "Configuración y Sistema"
        setupSubtitle = "Configura colores, fuentes, burbuja, idioma, grabación, actualizaciones y permisos."
        languageCardTitle = "IDIOMA DE LA APP"
        languageSubtitle = "Usa el idioma de tu teléfono automáticamente al instalar"
        targetModeCardTitle = "MODO DE COMPATIBILIDAD"
        targetModeSubtitle = "Elige entre GeForce NOW en la nube o juegos nativos de Android"
        targetModeGfnTitle = "GeForce NOW (PC Cloud)"
        targetModeGfnSubtitle = "Códigos Linux y cámara táctil central para GeForce NOW"
        targetModeAndroidTitle = "Juegos Android (Roblox, Minecraft, etc.)"
        targetModeAndroidSubtitle = "Teclas Android estándar y giro de cámara en zona derecha"

        themeCardTitle = "ESQUEMA DE COLOR Y MEZCLADOR"
        themeCardSubtitle = "Elige un tema predefinido o mezcla 2–3 colores personalizados"
        themePresetsLabel = "Temas"
        themeCustomMixTitle = "Mezcla Propia"
        themeMix2Colors = "2 Colores"
        themeMix3Colors = "3 Colores"
        themePrimaryColor = "Color Principal"
        themeSecondaryColor = "Color Secundario"
        themeTertiaryColor = "Acento (3er Color)"
        fontCardTitle = "PERSONALIZACIÓN DE FUENTE"
        fontCardSubtitle = "Elige una fuente gamer o sube tu archivo .ttf / .otf (ej. DaFont.com)"
        fontUploadButton = "Subir Fuente (.ttf / .otf)"
        fontUploadedLabel = "Fuente Subida"
        fontSamplePreview = "XENOSPAD • Listo para jugar 12345"
        bubbleCardTitle = "TAMAÑO Y DISEÑO DE BURBUJA"
        bubbleCardSubtitle = "Personaliza forma, tamaño, opacidad, color o icono de la burbuja flotante"
        bubbleStyleLabel = "Estilo de Burbuja"
        bubbleSizeLabel = "Tamaño"
        bubbleOpacityLabel = "Opacidad"
        bubbleStatusDotLabel = "Punto de Estado en Vivo"
        bubbleStatusDotSub = "Muestra indicador de grabación, replay, bloqueo o pausa"
        bubbleUseThemeAccent = "Usar Color del Tema"
        bubbleUseThemeAccentSub = "Sincroniza automáticamente el borde e icono con tu tema"
        bubbleCustomColorLabel = "Color de Burbuja"
        bubbleUploadIconButton = "Subir Imagen"
        bubbleLivePreview = "Vista Previa en Vivo"

        recQualityTitle = "CALIDAD DE GRABACIÓN Y REPLAY"
        recQualitySubtitle = "Elige resolución y FPS para grabaciones y repetición instantánea."
        recResolutionLabel = "Resolución"
        recFpsLabel = "Fotogramas (FPS)"
        recEncoderProfileLabel = "PERFIL DE CODIFICADOR H.264"
        recAutoFallbackNote = "Auto-ajuste: si el hardware limita 1440p o 90/120 FPS, XenosPad selecciona automáticamente el máximo estable."

        updatesCardTitle = "ACTUALIZACIONES (GITHUB RELEASES)"
        updatesCardSubtitle = "Comprueba si hay nuevas versiones en GitHub Releases y abre la página oficial de descarga."
        updatesInstalledVersion = "Versión Instalada"
        updatesSigningKeyLabel = "Firma SHA-256"
        updatesRepoLabel = "Repositorio GitHub (owner/repo)"
        updatesRepoPlaceholder = "ej. tunombre/XenosPad"
        updatesRepoAutoHint = "Configurado desde GitHub Actions. Misma firma digital para actualizar encima sin borrar datos."
        updatesRepoManualHint = "Pega tu slug o URL de GitHub."
        updatesAutoCheckTitle = "Comprobar al Abrir"
        updatesAutoCheckSub = "Buscar nuevos APK en GitHub Releases automáticamente al iniciar"
        updatesCheckButton = "Buscar Actualizaciones"
        updatesChecking = "Consultando GitHub Releases..."
        updatesUpToDate = "¡Estás al día!"
        updatesNewAvailable = "Actualización disponible, descárgala en GitHub"
        updatesDownloadAndUpdate = "Descargar en GitHub"
        updatesDownloading = "Descargando"
        updatesCancelDownload = "Cancelar Descarga"
        updatesReadyTitle = "Lista para instalar"
        updatesReadyKeepsData = "¡Firma verificada! Toca abajo para actualizar conservando todos tus diseños y galería."
        updatesReadyNeedPerm = "Permiso único: permite \"Instalar apps desconocidas\" para XenosPad y vuelve a tocar Instalar."
        updatesInstallButton = "Instalar Actualización"
        updatesAllowAndInstall = "Permitir e Instalar"
        updatesSigMismatchTitle = "Reinstalación Única Necesaria"
        updatesSigMismatchBody = "Tu versión actual usaba una clave aleatoria antigua. Desinstálala una sola vez, instala esta nueva versión y todas las futuras actualizarán encima automáticamente."
        updatesViewWhatsNew = "Novedades de Esta Versión"

        setupVibrationCardTitle = "VIBRACIÓN / HÁPTICA"
        setupPermsCardTitle = "PERMISOS DE OVERLAY E IME"
        setupPerm1Title = "1. Mostrar sobre otras apps"
        setupPerm1Desc = "Permite mostrar controles virtuales sobre GeForce NOW."
        setupPerm2Title = "2. Activar XenosPad IME y Teclado"
        setupPerm2Desc = "Envía eventos de teclado/gamepad reales y activa el teclado integrado."
        setupPerm3Title = "3. Gestos de Ratón y Scroll"
        setupPerm3Desc = "Servicio de accesibilidad para LMB, RMB, MMB y desplazamiento."
        setupEnableIme = "Activar IME"
        setupImeSelected = "IME Activo ✓"
        setupSelectIme = "Elegir IME Activo"
        setupAlertsReady = "Alertas Listas"
        setupAllowAlerts = "Permitir Alertas"
        keyboardOptionsTitle = "TECLADO Y MODO FLOTANTE"
        floatingKeyboardTitle = "Teclado Flotante por Defecto"
        floatingKeyboardSubtitle = "Flota sobre la barra de tareas para que no tape tus teclas"
        openFloatingKeyboard = "Abrir Teclado Flotante"
        setupNotationTitle = "NOTACIÓN DEL CONTROL"
        setupHudPrefsTitle = "PREFERENCIAS DE HUD"
        setupGlobalOpacity = "Opacidad Global"
        gridSizeTitle = "Tamaño de Cuadrícula"
        snapToGridTitle = "Ajustar Controles a la Cuadrícula"
        snapToGridSubtitle = "Alinea los controles al soltar en el editor"
        setupTelemetryTitle = "Mostrar Telemetría"
        setupTelemetrySub = "Ver eventos en tiempo real en la Zona de Pruebas"
        setupAutoLaunchTitle = "Abrir GeForce NOW Automáticamente"
        setupAutoLaunchSub = "Inicia GeForce NOW al encender el overlay"
        setupDarkModeTitle = "Modo Oscuro Táctico"
        setupDarkModeSub = "Tema obsidiana de alto contraste"
        setupInfoNote = "Nota: Sin root, los botones virtuales envían KeyEvents reales mediante XenosPad IME con soporte multitáctil completo."

        permDialogTitle = "Activar Overlay Flotante"
        permDialogBody = "Para mostrar tus botones virtuales sobre GeForce NOW, concede el permiso \"Mostrar sobre otras apps\" en Ajustes de Android."

        menuChooseLayoutHeader = "ELEGIR DISEÑO"
        menuDragKbHint = "ARRASTRA EL TECLADO LIBREMENTE"
        menuHidePad = "Ocultar"
        menuShowPad = "Mostrar"
        menuSwitch = "Cambiar"
        menuLayouts = "Diseños"
        menuTargetMode = "Modo"
        menuFilter = "Filtro"
        menuCrosshair = "Mira"
        menuScreenshot = "Captura"
        menuReplay = "Repetición"
        menuSaveReplay = "Guardar Clip"
        menuRecord = "Grabar"
        menuStopRecord = "Detener"
        menuKeyboard = "Teclado"
        menuFloatKb = "Tec. Flot."
        menuUnstick = "Liberar"
        menuLock = "Bloquear"
        menuUnlock = "Desbloq."
        menuStop = "Detener"
        menuOpenApp = "Abrir XenosPad"

        whatsNewBadge = "NOVEDADES"
        whatsNewTitle = "Bienvenido a XenosPad"
        whatsNewSubtitle = "Todo lo nuevo en esta actualización:"
        whatsNewGotIt = "¡Genial, a Jugar!"
        whatsNewItems = listOf(
            "Traducción Completa de Toda la App" to "Filtros Freestyle, Galería, Calidad de Grabación, Actualizaciones, Panel y Ajustes completamente traducidos.",
            "Actualizaciones Directas sin Desinstalar" to "Firma fija en GitHub Releases y verificación previa de APK para actualizar encima sin perder datos.",
            "Mezclador de 2–3 Colores y Fuentes Propias" to "Elige temas o mezcla 2–3 colores, y usa fuentes predefinidas o sube tus .ttf/.otf de DaFont.com.",
            "Burbuja Flotante y Fondo de Pruebas Propios" to "Personaliza tamaño, forma e icono de la burbuja y sube tu propia imagen de fondo en Pruebas.",
            "Teclado Animado con Autocorrección" to "Animaciones al pulsar cada tecla, vista previa flotante y sugerencias de palabras estilo Gboard."
        )

        cloudSaveTitle = "GUARDADO Y SINCRONIZACIÓN EN LA NUBE"
        cloudSaveSubtitle = "Cuenta opcional de Google: respalda y restaura diseños, filtros Freestyle, mira y tema automáticamente."
        cloudSaveSignInButton = "Registrarse / Entrar con Google"
        cloudSaveSigningIn = "Iniciando sesión con Google..."
        cloudSaveSignInShort = "Entrar"
        cloudSaveActiveBadge = "RESPALDO ACTIVO"
        cloudSaveSignedOutLabel = "Cloud Save: Solo Local"
        cloudSaveSignedOutHint = "Inicia sesión con Google para sincronizar diseños, filtros, mira y tema"
        cloudSaveSyncing = "Sincronizando diseños, filtros y tema..."
        cloudSaveBackedUpSuffix = "diseños + tema"
        cloudSaveAutoSaveHint = "Diseños, filtros, mira y tema se guardan solos"
        cloudSaveBackupNow = "Respaldar Ahora"
        cloudSaveRestore = "Restaurar"
        cloudSaveSyncShort = "Sinc."
        cloudSaveSignOut = "Cerrar Sesión"
        cloudSaveSignOutNote = "Al cerrar sesión se mantienen todos tus diseños y ajustes locales."

        gyroAimTitle = "APUNTADO CON GIROSCOPIO (TILT-TO-AIM)"
        gyroAimSubtitle = "Inclina tu teléfono para ajustar la cámara con precisión mediante el giroscopio físico."
        gyroEnableLabel = "Activar Giroscopio"
        gyroModeLabel = "Modo de Activación"
        gyroModeAlways = "Siempre Activo"
        gyroModeHoldAds = "Al Apuntar (LT / RMB)"
        gyroSensitivityLabel = "Sensibilidad Giro"
        gyroDeadzoneLabel = "Zona Muerta"
        gyroSmoothingLabel = "Suavizado"
        gyroInvertX = "Invertir X"
        gyroInvertY = "Invertir Y"
        gyroNoHardware = "No se detectó sensor de giroscopio en este dispositivo."
        gyroHudTile = "Giroscopio"

        perfHudTitle = "HUD DE RENDIMIENTO (FPS / TEMP)"
        perfHudSubtitle = "Barra flotante compacta estilo GeForce con FPS en vivo, batería y temperatura sobre el juego."
        perfHudEnableLabel = "Mostrar HUD de Rendimiento"
        perfHudShowFps = "FPS"
        perfHudShowBattery = "Batería"
        perfHudShowTemp = "Temp °C"
        perfHudShowRam = "RAM %"
        perfHudOpacityLabel = "Opacidad del HUD"
        perfHudTile = "HUD FPS"

        shareCodeAction = "Código"
        shareCodeEnterButton = "Código"
        shareCodeCreateTitle = "CÓDIGO DE COMPARTIR (6 DÍGITOS)"
        shareCodeCreateSubtitle = "Comparte este código temporal de 5 minutos para que un amigo importe una copia del diseño."
        shareCodeGenerating = "Generando código..."
        shareCodeCopyButton = "Copiar Código"
        shareCodeCopied = "¡Código copiado!"
        shareCodeExpiresNote = "Expira automáticamente en 5 minutos"
        shareCodeImportTitle = "IMPORTAR CON CÓDIGO"
        shareCodeImportSubtitle = "Ingresa el código de 6 caracteres de un amigo para guardar su diseño."
        shareCodeInputLabel = "Código de 6 caracteres (ej. NXP48K)"
        shareCodeImportButton = "Importar Diseño"
        shareCodeImporting = "Buscando diseño compartido..."
        shareCodeSignInRequired = "Inicia sesión con Google para crear o importar códigos de 6 dígitos."
        shareCodeNotFound = "Código no encontrado o expirado. Pide un código nuevo."

        liveEditTile = "Editar HUD"
        liveEditToolbarTitle = "EDICIÓN EN VIVO"
        liveEditDragHint = "Arrastra botones sobre el juego • Toca para ajustar"
        liveEditSizeLabel = "Tam."
        liveEditOpacityLabel = "Opac."
        liveEditSnapLabel = "Grilla"
        liveEditSaveButton = "Guardar"
        liveEditCancelButton = "Cancelar"
        liveEditSavedToast = "¡Diseño guardado!"

        webPlayerTitle = "Better GFN Web Player"
        webPlayerSubtitle = "Juega GeForce NOW dentro de XenosPad con inyección directa de mando/teclado/ratón, filtros CSS, estadísticas WebRTC y Bypass de Región integrado — sin permisos extra."
        webPlayerLaunchButton = "Jugar GeForce NOW"
        webPlayerZeroPermBadge = "SIN PERMISOS EXTRA"
        webPlayerCardHeader = "REPRODUCTOR WEB BETTER GFN Y BYPASS DE REGIÓN"
        webPlayerRegionHeader = "Bypass de Región Integrado (Estilo Better xCloud)"
        webPlayerRegionNote = "Envía cabeceras X-Forwarded-For, Accept-Language y zona horaria a los servidores de GeForce NOW (bypass de cabeceras, no un túnel VPN remoto)."
        webPlayerUserAgentLabel = "User-Agent del Navegador"
        webPlayerSpoofLocaleTitle = "Igualar Zona Horaria e Idioma"
        webPlayerSpoofLocaleSub = "Alinea la zona horaria y el idioma del navegador con la región elegida"
        webPlayerVirtualPadTitle = "Mando Virtual Xbox 360 (Gamepad API)"
        webPlayerVirtualPadSub = "Expone los controles de XenosPad directamente a play.geforcenow.com"
        webPlayerStretchTitle = "Estirar Video a Pantalla Completa"
        webPlayerStretchSub = "Elimina las barras negras laterales en pantallas 20:9"
        webPlayerStatsHudTitle = "HUD de Estadísticas WebRTC"
        webPlayerStatsHudSub = "Muestra FPS reales del stream, ping (RTT), bitrate y pérdida de paquetes"
        webPlayerHideControls = "Ocultar"
        webPlayerShowControls = "Mostrar"
        webPlayerReload = "Recargar"
        webPlayerExit = "Salir"
        webPlayerStretchShort = "Estirar"
        webPlayerAuthPopupTitle = "Inicio de Sesión"
    }

    private fun buildJapaneseStrings(): NexusStrings = buildNexusStrings {
        navCockpit = "ホーム"
        navProfiles = "プロファイル"
        navVisuals = "フィルター"
        navGallery = "ギャラリー"
        navSandbox = "テスト"
        navSetup = "設定"

        startOverlay = "オーバーレイ開始"
        stopOverlay = "オーバーレイ停止"
        launchOverlay = "オーバーレイ起動"
        commonEdit = "編集"
        commonCustomize = "カスタマイズ"
        commonCancel = "キャンセル"
        commonClose = "閉じる"
        commonReset = "リセット"
        commonEnable = "有効化"
        commonEnabled = "有効"
        commonStop = "停止"
        commonOverlay = "オーバーレイ"
        commonAll = "すべて"
        commonActive = "有効"
        commonReady = "準備完了"
        commonGrant = "許可"
        commonSelect = "選択"
        commonSetup = "設定"
        commonLaunch = "起動"
        commonOpenSettings = "設定を開く"
        commonNotNow = "後で"
        commonGranted = "許可済み"
        commonShare = "共有"
        commonDelete = "削除"
        commonControlsSuffix = "個のボタン"

        cockpitOverlayLive = "オーバーレイ稼働中"
        cockpitArmed = "待機中"
        cockpitIntegrationHeader = "GEFORCE NOW 連携ステータス"
        cockpitOverlayEngineTitle = "画面オーバーレイ権限"
        cockpitOverlayEngineGranted = "他のアプリの上に重ねて表示が許可されています"
        cockpitOverlayEngineMissing = "GeForce NOW 上に仮想ボタンを表示するために必要です"
        cockpitImeTitle = "ゲームパッド＆内蔵キーボード IME"
        cockpitImeSelected = "キー送信と内蔵キーボードが有効です"
        cockpitImeEnabled = "有効 — タップして入力方法として選択"
        cockpitImeDisabled = "XenosPad IME を有効にしてキー入力を使用"
        cockpitMouseTitle = "マウスクリック・スクロール操作"
        cockpitMouseReady = "有効 — 左・右・中クリック＆スクロール対応"
        cockpitMouseMissing = "ユーザー補助で LMB / RMB / MMB を有効化"
        cockpitGfnTitle = "GeForce NOW アプリ"
        cockpitGfnInstalled = "GeForce NOW を検出 — タップで起動"
        cockpitGfnMissing = "Play ストアを開くかテスト画面で確認できます"
        cockpitQuickProfile = "プロファイルクイック切替"
        cockpitSavedSuffix = "件"
        cockpitVibrationHeader = "ボタン振動フィードバック"
        cockpitActiveTopology = "現在のレイアウト構成"
        cockpitMappedSuffix = "個のコントロールを配置"
        cockpitTapCustomize = "タップして配置を編集"
        cockpitLayoutStudio = "レイアウト編集"
        cockpitTestControls = "動作テスト"

        vibNoneTitle = "振動オフ"
        vibNoneSub = "タップ時の振動を完全に無効化"
        vibLightTitle = "ソフトタップ"
        vibLightSub = "キーボードのような軽い振動"
        vibStrongTitle = "しっかりクリック"
        vibStrongSub = "はっきりした押下フィードバック"

        profilesTitle = "コントローラー＆キー設定"
        profilesSubtitle = "GeForce NOW や Android ゲーム向けにレイアウトを管理"
        profilesImport = "インポート"
        profilesSearchHint = "ゲーム名・レイアウトを検索..."
        profilesEmptyTitle = "該当するプロファイルがありません"
        profilesEmptySub = "新規作成するかジャンル絞り込みを解除してください。"
        profilesArm = "使用"
        profilesNewTitle = "新しいレイアウトを作成"
        profilesStarterTemplate = "テンプレート"
        profilesNameLabel = "プロファイル名"
        profilesTargetGameLabel = "対象ゲーム"
        profilesGenreTag = "ジャンル"
        profilesCreateAndEdit = "作成して編集"

        visualsGameFilterTab = "ゲームフィルター"
        visualsCrosshairTab = "クロスヘア"
        visualsFilterSubtitle = "18モジュール OpenGL ES Freestyle GPU シェーダー"
        visualsCrosshairSubtitle = "照準の形状・色・縁取り・カスタム画像を細かく調整"
        filterPanelTitle = "ゲームフィルター"
        filterGpuBadge = "GPU シェーダー"
        filterOffHint = "オフ — スタイル 1〜3 をタップして有効化"
        filterStyleLabel = "スタイル"
        filterOffLabel = "オフ"
        filterGpuEngineActive = "ライブ GPU シェーダー: 稼働中"
        filterGpuEngineTitle = "ライブ GPU ピクセルシェーダー"
        filterGpuEngineActiveDesc = "OpenGL シャープ・HDR・シャドウ・被写界深度が稼働中です。"
        filterGpuEngineIdleDesc = "「単一アプリ」(GeForce NOW等)を選ぶと60fpsシェーダーが動作します。"
        filterPreviewPhoto = "画像変更"
        filterCompare = "比較"
        filterOriginal = "オリジナル"
        filterFiltered = "適用後"
        filterPreviewActiveBanner = "ライブ GPU シェーダー有効 — ゲーム映像をリアルタイム処理中"
        filterPreviewIdleBanner = "OpenGL ES 2.0 プレビュー • 下からライブ GPU シェーダーを有効化"
        filterCatEssential = "基本"
        filterCatUtility = "補助"
        filterCatArtistic = "演出"
        filterModSharpen = "シャープ (Sharpen)"
        filterModSharpenPlus = "シャープ+ (Sharpen+)"
        filterModBrightnessContrast = "明るさ / コントラスト"
        filterModDetails = "ディテール (HDR & 明瞭度)"
        filterModColor = "カラー＆鮮やかさ"
        filterModColorblind = "色覚サポート"
        filterModNightMode = "ナイトモード"
        filterModSplitscreen = "分割画面比較"
        filterModBlackWhite = "モノクロ"
        filterModSepia = "セピア"
        filterModVignette = "ビネット"
        filterModDof = "被写界深度 / チルトシフト"
        filterModOldFilm = "オールドフィルム"
        filterModPainterly = "ペイント風"
        filterModLetterbox = "レターボックス"
        filterModMood = "ムードプリセット"
        filterModSpecialFx = "スペシャル FX"
        filterModStickers = "ステッカー"

        crosshairTitle = "クロスヘア HUD"
        crosshairEnable = "クロスヘアを有効化"
        crosshairPresets = "プリセット"
        crosshairColor = "色"
        crosshairOutline = "縁取り"
        crosshairThickness = "太さ"
        crosshairLength = "長さ"
        crosshairGap = "中央の隙間"
        crosshairOpacity = "不透明度"
        crosshairCenterDot = "中央ドット"
        crosshairUploadImage = "画像をアップロード"
        crosshairReset = "リセット"
        crosshairSize = "サイズ"

        galleryTitle = "キャプチャ＆ギャラリー"
        gallerySubtitle = "インスタントリプレイ (10秒〜1時間)、画面録画、スクリーンショット"
        galleryScreenshotsTab = "スクショ"
        galleryVideosTab = "動画・リプレイ"
        galleryReplayOn = "リプレイ ON"
        gallerySaveClipPrefix = "直前の"
        gallerySaveClipSuffix = "を保存"
        galleryReplayBufferHeader = "インスタントリプレイ保存時間"
        galleryCaptureQualityHeader = "録画品質"
        galleryEmptyTitle = "まだキャプチャがありません"
        galleryEmptySubtitle = "上のボタンまたはプレイ中のフローティングメニューから撮影できます。"
        galleryCannotDecodeImage = "画像を読み込めませんでした"
        galleryCannotPlayVideo = "この動画を再生できません"
        galleryDeleteBroken = "破損クリップを削除"

        sandboxTitle = "テストサンドボックス"
        sandboxNoProfile = "アクティブなプロファイルなし"
        sandboxCustomBg = "背景画像"
        sandboxResetBg = "背景リセット"
        sandboxTelemetryHeader = "リアルタイム入力ログ (タッチ + ハードウェア)"
        sandboxTelemetryEmpty = "ボタンやスティックを操作すると KeyEvent と軸データが表示されます。"
        sandboxFilterOff = "フィルター OFF"

        setupTitle = "オーバーレイとシステム設定"
        setupSubtitle = "カラー、フォント、バブル、言語、録画画質、アプリ更新、権限を設定します。"
        languageCardTitle = "アプリの言語 (LANGUAGE)"
        languageSubtitle = "初期設定では端末のシステム言語に自動で合わせます"
        targetModeCardTitle = "互換モード (TARGET GAME MODE)"
        targetModeSubtitle = "GeForce NOW 向けか Roblox・Minecraft などの Android ゲーム向けかを選択"
        targetModeGfnTitle = "GeForce NOW (クラウドPC)"
        targetModeGfnSubtitle = "Linux スキャンコード＋中央視点マウスジェスチャー"
        targetModeAndroidTitle = "Android ゲーム (Roblox, Minecraft など)"
        targetModeAndroidSubtitle = "標準 Android キー入力＋右画面カメラ操作・ネイティブクリック"

        themeCardTitle = "アプリカラー＆カスタムミックス"
        themeCardSubtitle = "プリセットまたは 2〜3 色のカスタムカラーを自由にブレンド"
        themePresetsLabel = "プリセットテーマ"
        themeCustomMixTitle = "カスタムミックス"
        themeMix2Colors = "2色ミックス"
        themeMix3Colors = "3色ミックス"
        themePrimaryColor = "メインカラー"
        themeSecondaryColor = "サブカラー"
        themeTertiaryColor = "アクセント (3色目)"
        fontCardTitle = "フォントカスタマイズ"
        fontCardSubtitle = "内蔵フォントを選ぶか DaFont 等の .ttf / .otf をアップロード"
        fontUploadButton = "フォントをアップロード (.ttf/.otf)"
        fontUploadedLabel = "読み込み済みフォント"
        fontSamplePreview = "XENOSPAD • ゲーム準備完了 12345"
        bubbleCardTitle = "フローティングバブルのサイズ・UI"
        bubbleCardSubtitle = "バブルの形・大きさ・透明度・色・カスタムアイコンを変更"
        bubbleStyleLabel = "バブルスタイル"
        bubbleSizeLabel = "バブルサイズ"
        bubbleOpacityLabel = "不透明度"
        bubbleStatusDotLabel = "ステータスドットを表示"
        bubbleStatusDotSub = "録画中・リプレイ・ロック状態を表示します"
        bubbleUseThemeAccent = "アプリテーマに合わせる"
        bubbleUseThemeAccentSub = "テーマカラーとバブル枠線を自動同期"
        bubbleCustomColorLabel = "カスタム枠線カラー"
        bubbleUploadIconButton = "バブル画像をアップロード"
        bubbleLivePreview = "ライブプレビュー"

        recQualityTitle = "録画・リプレイ画質設定"
        recQualitySubtitle = "出力解像度とフレームレート (FPS) を選択します。"
        recResolutionLabel = "解像度"
        recFpsLabel = "フレームレート (FPS)"
        recEncoderProfileLabel = "H.264 エンコーダ設定"
        recAutoFallbackNote = "自動調整: 端末の上限を超える場合、安定した最高解像度・FPS へ自動調整します。"

        updatesCardTitle = "アプリ更新 (GITHUB RELEASES)"
        updatesCardSubtitle = "GitHub Releases で最新バージョンを確認し、リリースページからダウンロードできます。"
        updatesInstalledVersion = "現在のバージョン"
        updatesSigningKeyLabel = "署名鍵 SHA-256"
        updatesRepoLabel = "GitHub リポジトリ (owner/repo)"
        updatesRepoPlaceholder = "例: yourname/XenosPad"
        updatesRepoAutoHint = "GitHub Actions から自動設定済み。同一署名鍵でデータを保持したまま更新します。"
        updatesRepoManualHint = "GitHub リポジトリの owner/repo を入力してください。"
        updatesAutoCheckTitle = "起動時に自動チェック"
        updatesAutoCheckSub = "起動時に GitHub Releases の最新版を自動確認"
        updatesCheckButton = "アップデートを確認"
        updatesChecking = "GitHub Releases を確認中..."
        updatesUpToDate = "最新バージョンです！"
        updatesNewAvailable = "アップデートがあります。GitHub からダウンロード"
        updatesDownloadAndUpdate = "GitHub でダウンロード"
        updatesDownloading = "ダウンロード中"
        updatesCancelDownload = "キャンセル"
        updatesReadyTitle = "インストール準備完了"
        updatesReadyKeepsData = "署名一致を確認しました！データを保持したまま上書き更新できます。"
        updatesReadyNeedPerm = "初回のみ「不明なアプリのインストール」を許可してください。"
        updatesInstallButton = "上書きインストール"
        updatesAllowAndInstall = "許可してインストール"
        updatesSigMismatchTitle = "1回のみ再インストールが必要"
        updatesSigMismatchBody = "現在のアプリは古いテスト鍵で署名されています。一度だけアンインストールして新版を入れると、次回以降は常に上書き更新できます。"
        updatesViewWhatsNew = "このバージョンの新機能"

        setupVibrationCardTitle = "振動 / ハプティクス"
        setupPermsCardTitle = "オーバーレイ・IME 権限"
        setupPerm1Title = "1. 他のアプリの上に表示"
        setupPerm1Desc = "GeForce NOW 上に仮想コントローラーを表示します。"
        setupPerm2Title = "2. XenosPad IME・キーボードを有効化"
        setupPerm2Desc = "実キーイベント送信と内蔵キーボードを使用可能にします。"
        setupPerm3Title = "3. マウスクリック・スクロール補助"
        setupPerm3Desc = "左・右・中クリックとスクロール用のユーザー補助サービス。"
        setupEnableIme = "IME を有効化"
        setupImeSelected = "IME 選択済み ✓"
        setupSelectIme = "IME を選択"
        setupAlertsReady = "通知許可済み"
        setupAllowAlerts = "通知を許可"
        keyboardOptionsTitle = "キーボードとフローティングモード"
        floatingKeyboardTitle = "フローティングキーボードを優先"
        floatingKeyboardSubtitle = "タスクバーの上に浮かせて表示し、キーの隠れを防ぎます"
        openFloatingKeyboard = "フローティングキーボードを開く"
        setupNotationTitle = "ボタン表記スタイル"
        setupHudPrefsTitle = "HUD・操作設定"
        setupGlobalOpacity = "全体の不透明度"
        gridSizeTitle = "グリッドサイズ"
        snapToGridTitle = "グリッドにスナップ"
        snapToGridSubtitle = "配置編集で指を離したときにグリッドへ吸着します"
        setupTelemetryTitle = "テレメトリ HUD を表示"
        setupTelemetrySub = "テスト画面でリアルタイム入力ログを表示"
        setupAutoLaunchTitle = "GeForce NOW を自動起動"
        setupAutoLaunchSub = "オーバーレイ開始時に自動で GeForce NOW を開く"
        setupDarkModeTitle = "ダークコックピット"
        setupDarkModeSub = "ゲームに集中できる高コントラスト暗色テーマ"
        setupInfoNote = "注: 非root端末でも XenosPad IME 経由でマルチタッチ対応のキー入力が可能です。"

        permDialogTitle = "フローティング権限を有効化"
        permDialogBody = "GeForce NOW 上にコントローラーを表示するため、Android 設定で「他のアプリの上に重ねて表示」を許可してください。"

        menuChooseLayoutHeader = "レイアウトを選択"
        menuDragKbHint = "バーをドラッグして移動"
        menuHidePad = "非表示"
        menuShowPad = "表示"
        menuSwitch = "切替"
        menuLayouts = "レイアウト"
        menuTargetMode = "モード"
        menuFilter = "フィルター"
        menuCrosshair = "クロスヘア"
        menuScreenshot = "スクショ"
        menuReplay = "リプレイ"
        menuSaveReplay = "クリップ保存"
        menuRecord = "録画"
        menuStopRecord = "録画停止"
        menuKeyboard = "キーボード"
        menuFloatKb = "浮動KB"
        menuUnstick = "全解除"
        menuLock = "ロック"
        menuUnlock = "ロック解除"
        menuStop = "終了"
        menuOpenApp = "アプリを開く"

        whatsNewBadge = "新機能"
        whatsNewTitle = "XenosPad へようこそ"
        whatsNewSubtitle = "今回のアップデート内容："
        whatsNewGotIt = "了解、プレイ開始！"
        whatsNewItems = listOf(
            "全画面の完全多言語対応" to "フィルター、ギャラリー、録画画質、アップデート、設定など全画面が選択言語に切り替わります。",
            "上書きアップデート対応＆署名チェック" to "GitHub Releases の署名鍵を固定化し、アンインストール不要で更新可能になりました。",
            "2〜3色カラーミキサー＆カスタムフォント" to "自由な配色ミックスに加え、DaFont 等の .ttf/.otf フォントを適用できます。",
            "カスタムバブル＆テスト背景画像" to "フローティングバブルの形・サイズ変更や、テスト画面の背景アップロードに対応しました。",
            "キーボードアニメーション＆予測変換" to "キー押下ポップアップと Gboard 風の自動修正・単語予測バーを搭載しました。"
        )

        cloudSaveTitle = "クラウド保存＆同期"
        cloudSaveSubtitle = "クラウド保存専用の任意アカウントです。レイアウト・フィルター・クロスヘア・テーマを自動同期します。"
        cloudSaveSignInButton = "Google で登録 / ログイン"
        cloudSaveSigningIn = "Google でログイン中..."
        cloudSaveSignInShort = "ログイン"
        cloudSaveActiveBadge = "自動バックアップON"
        cloudSaveSignedOutLabel = "クラウド保存: ローカルのみ"
        cloudSaveSignedOutHint = "Google ログインでレイアウト・フィルター・照準・テーマを同期"
        cloudSaveSyncing = "レイアウト・フィルター・テーマを同期中..."
        cloudSaveBackedUpSuffix = "件 + テーマ同期済"
        cloudSaveAutoSaveHint = "レイアウト・フィルター・照準・テーマを自動保存"
        cloudSaveBackupNow = "今すぐ保存"
        cloudSaveRestore = "クラウドから復元"
        cloudSaveSyncShort = "同期"
        cloudSaveSignOut = "ログアウト"
        cloudSaveSignOutNote = "ログアウトしても端末内のレイアウトと設定は残ります。"

        gyroAimTitle = "ジャイロエイム（傾き照準）"
        gyroAimSubtitle = "端末のジャイロセンサーを使って傾きでカメラ照準を微調整します。"
        gyroEnableLabel = "ジャイロエイムを有効化"
        gyroModeLabel = "起動モード"
        gyroModeAlways = "常時オン"
        gyroModeHoldAds = "ADS長押し時 (LT / RMB)"
        gyroSensitivityLabel = "ジャイロ感度"
        gyroDeadzoneLabel = "デッドゾーン"
        gyroSmoothingLabel = "スムージング"
        gyroInvertX = "X軸反転"
        gyroInvertY = "Y軸反転"
        gyroNoHardware = "この端末にジャイロセンサーが検出されません。"
        gyroHudTile = "ジャイロ"

        perfHudTitle = "パフォーマンスHUD（FPS / 温度）"
        perfHudSubtitle = "ゲーム上に移動可能なGeForce風の小型バーでFPS・バッテリー・温度をリアルタイム表示します。"
        perfHudEnableLabel = "パフォーマンスHUDを表示"
        perfHudShowFps = "FPS"
        perfHudShowBattery = "バッテリー"
        perfHudShowTemp = "温度 °C"
        perfHudShowRam = "RAM %"
        perfHudOpacityLabel = "HUD不透明度"
        perfHudTile = "性能HUD"

        shareCodeAction = "共有コード"
        shareCodeEnterButton = "コード入力"
        shareCodeCreateTitle = "6桁レイアウト共有コード"
        shareCodeCreateSubtitle = "5分間有効なコードをフレンドに伝えると、このレイアウトのコピーを取り込めます。"
        shareCodeGenerating = "共有コードを作成中..."
        shareCodeCopyButton = "コードをコピー"
        shareCodeCopied = "共有コードをコピーしました！"
        shareCodeExpiresNote = "5分後に自動で期限切れになります"
        shareCodeImportTitle = "共有コードから取り込み"
        shareCodeImportSubtitle = "フレンドの6桁コードを入力してプロフィールに追加します。"
        shareCodeInputLabel = "6桁コード（例: NXP48K）"
        shareCodeImportButton = "取り込む"
        shareCodeImporting = "レイアウトを取得中..."
        shareCodeSignInRequired = "6桁共有コードの作成・取り込みには Google ログインが必要です。"
        shareCodeNotFound = "コードが見つからないか期限切れです。新しいコードを受け取ってください。"

        liveEditTile = "ライブ編集"
        liveEditToolbarTitle = "ライブ編集"
        liveEditDragHint = "ゲーム上で直接ボタンをドラッグ • タップで調整"
        liveEditSizeLabel = "サイズ"
        liveEditOpacityLabel = "透明度"
        liveEditSnapLabel = "グリッド"
        liveEditSaveButton = "保存"
        liveEditCancelButton = "中止"
        liveEditSavedToast = "レイアウトを保存しました！"

        webPlayerTitle = "Better GFN ウェブプレーヤー"
        webPlayerSubtitle = "XenosPad内でGeForce NOWを直接プレイ。仮想コントローラー/KB/マウス直接注入、CSSフィルター、WebRTC統計、内蔵リージョンバイパスを追加権限なしで利用可能。"
        webPlayerLaunchButton = "GeForce NOWを起動"
        webPlayerZeroPermBadge = "追加権限不要"
        webPlayerCardHeader = "BETTER GFN ウェブプレーヤー & リージョンバイパス"
        webPlayerRegionHeader = "内蔵リージョンバイパス (Better xCloud方式)"
        webPlayerRegionNote = "GeForce NOW通信に地域別X-Forwarded-For・Accept-Language・タイムゾーンヘッダーを付与します（ヘッダー方式であり外部VPNサーバーではありません）。"
        webPlayerUserAgentLabel = "ブラウザ User-Agent"
        webPlayerSpoofLocaleTitle = "タイムゾーン・言語を地域に合わせる"
        webPlayerSpoofLocaleSub = "選択したリージョンに合わせてブラウザの言語と時刻情報を調整"
        webPlayerVirtualPadTitle = "仮想 Xbox 360 ゲームパッドAPI"
        webPlayerVirtualPadSub = "XenosPad操作をplay.geforcenow.comにネイティブコントローラーとして直接送信"
        webPlayerStretchTitle = "画面いっぱいに映像を拡大"
        webPlayerStretchSub = "20:9スマホ画面の左右の黒帯を消去します"
        webPlayerStatsHudTitle = "リアルタイムWebRTC統計HUD"
        webPlayerStatsHudSub = "配信FPS・Ping (RTT)・ビットレート・パケットロスを表示"
        webPlayerHideControls = "非表示"
        webPlayerShowControls = "表示"
        webPlayerReload = "再読込"
        webPlayerExit = "終了"
        webPlayerStretchShort = "拡大"
        webPlayerAuthPopupTitle = "サインインウィンドウ"
    }

    private fun buildChineseStrings(): NexusStrings = buildNexusStrings {
        navCockpit = "控制台"
        navProfiles = "配置"
        navVisuals = "滤镜"
        navGallery = "媒体库"
        navSandbox = "测试"
        navSetup = "设置"

        startOverlay = "启动悬浮"
        stopOverlay = "停止悬浮"
        launchOverlay = "开启悬浮层"
        commonEdit = "编辑"
        commonCustomize = "自定义"
        commonCancel = "取消"
        commonClose = "关闭"
        commonReset = "重置"
        commonEnable = "启用"
        commonEnabled = "已启用"
        commonStop = "停止"
        commonOverlay = "悬浮层"
        commonAll = "全部"
        commonActive = "当前使用"
        commonReady = "就绪"
        commonGrant = "去授权"
        commonSelect = "去选择"
        commonSetup = "去设置"
        commonLaunch = "启动"
        commonOpenSettings = "打开设置"
        commonNotNow = "暂不"
        commonGranted = "已授权"
        commonShare = "分享"
        commonDelete = "删除"
        commonControlsSuffix = "个按键"

        cockpitOverlayLive = "悬浮层运行中"
        cockpitArmed = "已准备就绪"
        cockpitIntegrationHeader = "GEFORCE NOW 系统状态"
        cockpitOverlayEngineTitle = "悬浮窗显示引擎"
        cockpitOverlayEngineGranted = "已获得“显示在其他应用上层”权限"
        cockpitOverlayEngineMissing = "需要在 GeForce NOW 上方显示虚拟按键"
        cockpitImeTitle = "手柄与内置键盘输入法 (IME)"
        cockpitImeSelected = "已激活按键传输与内置打字键盘"
        cockpitImeEnabled = "已启用 — 点击切换为当前输入法"
        cockpitImeDisabled = "启用 XenosPad 输入法以支持按键和内置键盘"
        cockpitMouseTitle = "鼠标点击与滚轮手势"
        cockpitMouseReady = "已启用 — 支持左键、右键、中键和滚轮"
        cockpitMouseMissing = "开启无障碍服务以支持 LMB、RMB、MMB 和滚轮"
        cockpitGfnTitle = "GeForce NOW 目标应用"
        cockpitGfnInstalled = "已检测到 GeForce NOW — 点击启动"
        cockpitGfnMissing = "打开商店或前往测试区验证按键布局"
        cockpitQuickProfile = "快速切换配置"
        cockpitSavedSuffix = "套"
        cockpitVibrationHeader = "按键振动反馈"
        cockpitActiveTopology = "当前按键布局概览"
        cockpitMappedSuffix = "个虚拟控件"
        cockpitTapCustomize = "点击调整按键位置"
        cockpitLayoutStudio = "布局编辑器"
        cockpitTestControls = "测试按键"

        vibNoneTitle = "关闭振动"
        vibNoneSub = "完全关闭 — 按键无任何振动"
        vibLightTitle = "轻柔触感"
        vibLightSub = "类似打字键盘的细腻反馈"
        vibStrongTitle = "清脆点按"
        vibStrongSub = "清晰的实体按键式振动反馈"

        profilesTitle = "手柄与键位配置"
        profilesSubtitle = "为 GeForce NOW 和安卓游戏定制专属布局"
        profilesImport = "导入"
        profilesSearchHint = "搜索游戏或配置名称..."
        profilesEmptyTitle = "未找到匹配的配置"
        profilesEmptySub = "请新建布局或清除分类筛选。"
        profilesArm = "启用"
        profilesNewTitle = "新建布局配置"
        profilesStarterTemplate = "起始模板"
        profilesNameLabel = "配置名称"
        profilesTargetGameLabel = "目标游戏"
        profilesGenreTag = "游戏类型"
        profilesCreateAndEdit = "创建并编辑"

        visualsGameFilterTab = "游戏滤镜"
        visualsCrosshairTab = "准星设置"
        visualsFilterSubtitle = "18 模块 OpenGL ES Freestyle 实时 GPU 像素着色器"
        visualsCrosshairSubtitle = "自由调整准星形状、颜色、描边、间距及自定义图片"
        filterPanelTitle = "游戏滤镜"
        filterGpuBadge = "GPU 着色器"
        filterOffHint = "滤镜已关闭 — 点击方案 1–3 开启"
        filterStyleLabel = "方案"
        filterOffLabel = "关"
        filterGpuEngineActive = "实时 GPU 着色器: 运行中"
        filterGpuEngineTitle = "实时 GPU 逐像素滤镜"
        filterGpuEngineActiveDesc = "OpenGL 锐化、HDR、阴影、景深与特效正在实时生效。"
        filterGpuEngineIdleDesc = "选择“单个应用”(如 GeForce NOW)开启真 60fps 像素滤镜。"
        filterPreviewPhoto = "换图"
        filterCompare = "对比"
        filterOriginal = "原图"
        filterFiltered = "滤镜后"
        filterPreviewActiveBanner = "实时 GPU 着色器已开启 — 游戏画面逐像素增强中"
        filterPreviewIdleBanner = "OpenGL ES 2.0 实时预览 • 在下方启用实时 GPU 滤镜"
        filterCatEssential = "基础"
        filterCatUtility = "辅助"
        filterCatArtistic = "艺术"
        filterModSharpen = "锐化 (Sharpen)"
        filterModSharpenPlus = "高级锐化 (Sharpen+)"
        filterModBrightnessContrast = "亮度 / 对比度"
        filterModDetails = "细节 (HDR 与清晰度)"
        filterModColor = "色彩与自然饱和度"
        filterModColorblind = "色盲辅助"
        filterModNightMode = "夜间护眼模式"
        filterModSplitscreen = "分屏对比"
        filterModBlackWhite = "黑白滤镜"
        filterModSepia = "复古怀旧 (Sepia)"
        filterModVignette = "暗角 (Vignette)"
        filterModDof = "景深 / 移轴"
        filterModOldFilm = "老电影"
        filterModPainterly = "油画风格"
        filterModLetterbox = "电影宽屏黑边"
        filterModMood = "氛围预设"
        filterModSpecialFx = "特殊效果"
        filterModStickers = "贴纸叠加"

        crosshairTitle = "准星 HUD"
        crosshairEnable = "启用准星"
        crosshairPresets = "预设"
        crosshairColor = "准星颜色"
        crosshairOutline = "描边轮廓"
        crosshairThickness = "线条粗细"
        crosshairLength = "线条长度"
        crosshairGap = "中心间距"
        crosshairOpacity = "不透明度"
        crosshairCenterDot = "中心点"
        crosshairUploadImage = "上传图片"
        crosshairReset = "重置"
        crosshairSize = "图片大小"

        galleryTitle = "录屏与媒体库"
        gallerySubtitle = "GeForce 风格即时重放 (10秒–1小时)、手动录屏与截图。"
        galleryScreenshotsTab = "截图"
        galleryVideosTab = "视频与重放"
        galleryReplayOn = "重放已开"
        gallerySaveClipPrefix = "立即保存最近"
        gallerySaveClipSuffix = "片段"
        galleryReplayBufferHeader = "即时重放缓存时长"
        galleryCaptureQualityHeader = "录制画质"
        galleryEmptyTitle = "暂无录制内容"
        galleryEmptySubtitle = "点击上方截图、即时重放或录屏 — 或在游戏内通过悬浮球菜单录制。"
        galleryCannotDecodeImage = "无法解码此图片"
        galleryCannotPlayVideo = "无法播放该视频片段"
        galleryDeleteBroken = "删除损坏片段"

        sandboxTitle = "按键测试沙盒"
        sandboxNoProfile = "未启用配置"
        sandboxCustomBg = "自定义背景"
        sandboxResetBg = "恢复默认"
        sandboxTelemetryHeader = "实时输入监测 (触控 + 外设)"
        sandboxTelemetryEmpty = "点击按键、拖动摇杆或按下外设按键即可查看实时信号。"
        sandboxFilterOff = "滤镜关闭"

        setupTitle = "悬浮层与系统设置"
        setupSubtitle = "自定义配色、字体、悬浮球、语言、录屏画质、应用更新与系统权限。"
        languageCardTitle = "应用语言"
        languageSubtitle = "默认自动跟随手机系统语言"
        targetModeCardTitle = "游戏兼容模式"
        targetModeSubtitle = "选择适配 GeForce NOW 云端或 Roblox、Minecraft 等本地安卓游戏"
        targetModeGfnTitle = "GeForce NOW (云端 PC)"
        targetModeGfnSubtitle = "Linux 硬件扫描码 + 居中触控鼠标视角"
        targetModeAndroidTitle = "安卓本地游戏 (Roblox / Minecraft 等)"
        targetModeAndroidSubtitle = "标准安卓按键 + 右半屏 360° 视角滑动与原生点击"

        themeCardTitle = "应用配色方案与混色器"
        themeCardSubtitle = "选择预设主题或自由混合 2–3 种自定义颜色"
        themePresetsLabel = "预设主题"
        themeCustomMixTitle = "自定义混色"
        themeMix2Colors = "2色混合"
        themeMix3Colors = "3色混合"
        themePrimaryColor = "主色"
        themeSecondaryColor = "副色"
        themeTertiaryColor = "点缀色 (第3色)"
        fontCardTitle = "自定义字体"
        fontCardSubtitle = "选择内置电竞字体或上传 .ttf / .otf 字体文件 (如 DaFont.com)"
        fontUploadButton = "上传字体 (.ttf / .otf)"
        fontUploadedLabel = "已上传字体"
        fontSamplePreview = "XENOSPAD • 准备开战 12345"
        bubbleCardTitle = "悬浮球大小与样式"
        bubbleCardSubtitle = "自定义悬浮球形状、尺寸、透明度、颜色或专属图标"
        bubbleStyleLabel = "悬浮球样式"
        bubbleSizeLabel = "悬浮球大小"
        bubbleOpacityLabel = "透明度"
        bubbleStatusDotLabel = "显示实时状态点"
        bubbleStatusDotSub = "指示录屏、重放、锁定或隐藏状态"
        bubbleUseThemeAccent = "跟随应用主题色"
        bubbleUseThemeAccentSub = "自动将悬浮球边框与图标同步到应用配色"
        bubbleCustomColorLabel = "自定义边框颜色"
        bubbleUploadIconButton = "上传悬浮球图片"
        bubbleLivePreview = "悬浮球实时预览"

        recQualityTitle = "录屏与即时重放画质"
        recQualitySubtitle = "选择录屏和即时重放的输出分辨率与帧率 (FPS)。"
        recResolutionLabel = "分辨率"
        recFpsLabel = "帧率 (FPS)"
        recEncoderProfileLabel = "目标 H.264 编码配置"
        recAutoFallbackNote = "自动降级保护：若手机硬件不支持 1440p 或 90/120 FPS，将自动选择最高稳定规格。"

        updatesCardTitle = "应用更新 (GITHUB RELEASES)"
        updatesCardSubtitle = "检查 GitHub Releases 最新版本，并跳转到最新发版页面下载。"
        updatesInstalledVersion = "当前安装版本"
        updatesSigningKeyLabel = "签名指纹 SHA-256"
        updatesRepoLabel = "GitHub 仓库 (owner/repo)"
        updatesRepoPlaceholder = "例如 yourname/XenosPad"
        updatesRepoAutoHint = "由 GitHub Actions 自动配置。固定签名确保每次升级无损保留数据。"
        updatesRepoManualHint = "输入您的 GitHub 仓库 owner/repo 或链接。"
        updatesAutoCheckTitle = "启动时自动检查更新"
        updatesAutoCheckSub = "打开 XenosPad 时在后台静默检查 GitHub 新版本"
        updatesCheckButton = "检查更新"
        updatesChecking = "正在检查 GitHub Releases..."
        updatesUpToDate = "已是最新版本！"
        updatesNewAvailable = "有可用更新，请前往 GitHub 下载"
        updatesDownloadAndUpdate = "前往 GitHub 下载"
        updatesDownloading = "正在下载"
        updatesCancelDownload = "取消下载"
        updatesReadyTitle = "准备安装"
        updatesReadyKeepsData = "签名校验通过！点击下方直接覆盖安装，保留全部布局、滤镜与媒体库。"
        updatesReadyNeedPerm = "首次更新需要允许“安装未知应用”，授权后再点击安装即可。"
        updatesInstallButton = "安装更新 (保留数据)"
        updatesAllowAndInstall = "授权并安装"
        updatesSigMismatchTitle = "需重装一次 (旧签名不同)"
        updatesSigMismatchBody = "您手机上当前的旧版本使用了随机测试签名。只需卸载旧版一次并安装此新版本，之后所有更新都将无缝覆盖安装！"
        updatesViewWhatsNew = "查看本版本更新内容"

        setupVibrationCardTitle = "按键振动 / 触感反馈"
        setupPermsCardTitle = "悬浮窗与输入法权限"
        setupPerm1Title = "1. 显示在其他应用上层"
        setupPerm1Desc = "允许在 GeForce NOW 上方显示虚拟按键与摇杆。"
        setupPerm2Title = "2. 启用 XenosPad 输入法与键盘"
        setupPerm2Desc = "发送真实按键事件并启用内置虚拟键盘。"
        setupPerm3Title = "3. 鼠标点击与滚轮无障碍服务"
        setupPerm3Desc = "用于左键、右键、中键及滚轮手势模拟。"
        setupEnableIme = "启用输入法"
        setupImeSelected = "已选择输入法 ✓"
        setupSelectIme = "选择当前输入法"
        setupAlertsReady = "通知已开启"
        setupAllowAlerts = "允许通知"
        keyboardOptionsTitle = "键盘与悬浮模式"
        floatingKeyboardTitle = "默认使用悬浮键盘"
        floatingKeyboardSubtitle = "键盘悬浮于系统任务栏上方，避免按键被遮挡"
        openFloatingKeyboard = "打开悬浮键盘"
        setupNotationTitle = "手柄按键显示风格"
        setupHudPrefsTitle = "HUD 与交互偏好"
        setupGlobalOpacity = "全局按键透明度"
        gridSizeTitle = "布局网格大小"
        snapToGridTitle = "控件吸附网格"
        snapToGridSubtitle = "在布局编辑中松手时自动对齐网格"
        setupTelemetryTitle = "显示实时输入监测"
        setupTelemetrySub = "在测试沙盒显示按键与摇杆信号日志"
        setupAutoLaunchTitle = "自动启动 GeForce NOW"
        setupAutoLaunchSub = "启动悬浮层时自动打开 GeForce NOW"
        setupDarkModeTitle = "暗黑战术主题"
        setupDarkModeSub = "高对比度深色界面，减少反光干扰"
        setupInfoNote = "提示：无需 Root，虚拟按钮通过 XenosPad IME 发送真实按键事件，全面支持多点触控。"

        permDialogTitle = "开启悬浮窗权限"
        permDialogBody = "要在 GeForce NOW 上方显示虚拟手柄与按键，请在安卓设置中授予“显示在其他应用上层”权限。"

        menuChooseLayoutHeader = "选择布局"
        menuDragKbHint = "拖动顶部栏自由移动键盘"
        menuHidePad = "隐藏按键"
        menuShowPad = "显示按键"
        menuSwitch = "切换配置"
        menuLayouts = "切换布局"
        menuTargetMode = "模式"
        menuFilter = "游戏滤镜"
        menuCrosshair = "准星"
        menuScreenshot = "截图"
        menuReplay = "即时重放"
        menuSaveReplay = "保存重放"
        menuRecord = "录屏"
        menuStopRecord = "停止录屏"
        menuKeyboard = "键盘"
        menuFloatKb = "悬浮键盘"
        menuUnstick = "重置卡键"
        menuLock = "锁定"
        menuUnlock = "解锁"
        menuStop = "关闭"
        menuOpenApp = "打开应用"

        whatsNewBadge = "更新内容"
        whatsNewTitle = "欢迎使用 XenosPad"
        whatsNewSubtitle = "本版本全新升级内容："
        whatsNewGotIt = "太棒了，开始游戏！"
        whatsNewItems = listOf(
            "全应用完整多语言翻译" to "滤镜、媒体库、录屏画质、应用更新、控制台与设置界面全面适配所选语言。",
            "真正无损覆盖升级 APK" to "固定 GitHub Releases 签名并新增安装前签名校验，以后更新无需卸载旧版。",
            "2–3色主题混色器与自定义字体" to "支持多套电竞主题、2–3色自由调色以及上传 DaFont 等 .ttf/.otf 字体。",
            "自定义悬浮球与测试背景图" to "支持 5 种悬浮球外观、大小调节、自定义图标以及沙盒自定义背景图。",
            "虚拟键盘按键动画与智能联想" to "新增按键缩放弹窗动画以及 Gboard 风格中英泰联想词与自动纠错栏。"
        )

        cloudSaveTitle = "云端同步与备份 (CLOUD SAVE)"
        cloudSaveSubtitle = "可选 Google 账号 — 自动云端备份并恢复你的按键布局、画质滤镜、准星与主题样式。"
        cloudSaveSignInButton = "使用 Google 注册 / 登录"
        cloudSaveSigningIn = "正在使用 Google 登录..."
        cloudSaveSignInShort = "登录"
        cloudSaveActiveBadge = "自动云备份已开启"
        cloudSaveSignedOutLabel = "云端备份：仅本地保存"
        cloudSaveSignedOutHint = "登录 Google 自动同步布局、滤镜、准星与主题"
        cloudSaveSyncing = "正在同步布局、滤镜与主题..."
        cloudSaveBackedUpSuffix = "个布局 + 主题已同步"
        cloudSaveAutoSaveHint = "布局、滤镜、准星与主题均会自动同步到云端"
        cloudSaveBackupNow = "立即备份"
        cloudSaveRestore = "从云端恢复"
        cloudSaveSyncShort = "同步"
        cloudSaveSignOut = "退出登录"
        cloudSaveSignOutNote = "退出登录不会删除本设备上的本地布局与设置。"

        gyroAimTitle = "陀螺仪体感瞄准 (TILT-TO-AIM)"
        gyroAimSubtitle = "使用手机硬件陀螺仪传感器，通过倾斜机身微调游戏视角瞄准。"
        gyroEnableLabel = "开启陀螺仪瞄准"
        gyroModeLabel = "触发模式"
        gyroModeAlways = "始终开启"
        gyroModeHoldAds = "按住开镜时 (LT / RMB)"
        gyroSensitivityLabel = "陀螺仪灵敏度"
        gyroDeadzoneLabel = "防抖死区"
        gyroSmoothingLabel = "平滑滤波"
        gyroInvertX = "反转 X 轴"
        gyroInvertY = "反转 Y 轴"
        gyroNoHardware = "当前设备未检测到硬件陀螺仪传感器。"
        gyroHudTile = "陀螺仪"

        perfHudTitle = "性能监控悬浮条 (PERF HUD)"
        perfHudSubtitle = "可拖动的 GeForce 风格悬浮条，在游戏上方实时显示 FPS、电量与温度。"
        perfHudEnableLabel = "显示性能监控悬浮条"
        perfHudShowFps = "帧率 FPS"
        perfHudShowBattery = "电量"
        perfHudShowTemp = "温度 °C"
        perfHudShowRam = "内存 %"
        perfHudOpacityLabel = "悬浮条透明度"
        perfHudTile = "性能条"

        shareCodeAction = "分享码"
        shareCodeEnterButton = "输入分享码"
        shareCodeCreateTitle = "6 位布局分享码"
        shareCodeCreateSubtitle = "将此 5 分钟临时代码发给好友，即可一键导入该按键布局副本。"
        shareCodeGenerating = "正在生成分享码..."
        shareCodeCopyButton = "复制代码"
        shareCodeCopied = "分享码已复制！"
        shareCodeExpiresNote = "将在 5 分钟后自动失效"
        shareCodeImportTitle = "通过分享码导入"
        shareCodeImportSubtitle = "输入好友的 6 位布局代码并保存到你的布局列表中。"
        shareCodeInputLabel = "6 位分享码（例如 NXP48K）"
        shareCodeImportButton = "导入布局"
        shareCodeImporting = "正在获取分享布局..."
        shareCodeSignInRequired = "请先登录 Google 账号以创建或导入 6 位分享码。"
        shareCodeNotFound = "未找到该分享码或已过期，请向好友获取新代码。"

        liveEditTile = "实时编辑"
        liveEditToolbarTitle = "游戏内实时编辑"
        liveEditDragHint = "直接在游戏画面上拖动按钮 • 点击选中调节"
        liveEditSizeLabel = "大小"
        liveEditOpacityLabel = "透明度"
        liveEditSnapLabel = "吸附"
        liveEditSaveButton = "保存"
        liveEditCancelButton = "取消"
        liveEditSavedToast = "布局已保存！"

        webPlayerTitle = "Better GFN 网页播放器"
        webPlayerSubtitle = "在 XenosPad 内直接畅玩 GeForce NOW，支持虚拟手柄/键鼠直接注入、CSS 滤镜、WebRTC 实时状态与内置地区绕过，无需额外权限。"
        webPlayerLaunchButton = "启动 GeForce NOW"
        webPlayerZeroPermBadge = "零额外权限"
        webPlayerCardHeader = "BETTER GFN 网页播放器与地区绕过"
        webPlayerRegionHeader = "内置地区绕过 (Better xCloud 方式)"
        webPlayerRegionNote = "向 GeForce NOW 接口注入对应地区的 X-Forwarded-For、Accept-Language 和时区信息（请求头绕过，非远程 VPN 隧道）。"
        webPlayerUserAgentLabel = "浏览器标识 (User-Agent)"
        webPlayerSpoofLocaleTitle = "匹配所选地区时区与语言"
        webPlayerSpoofLocaleSub = "使浏览器时区和语言与所选地区保持一致"
        webPlayerVirtualPadTitle = "虚拟 Xbox 360 手柄 API"
        webPlayerVirtualPadSub = "将 XenosPad 虚拟按键直接作为原生手柄注入 play.geforcenow.com"
        webPlayerStretchTitle = "拉伸画面铺满屏幕"
        webPlayerStretchSub = "消除 20:9 宽屏手机两侧黑边"
        webPlayerStatsHudTitle = "实时 WebRTC 串流状态栏"
        webPlayerStatsHudSub = "显示实时串流 FPS、延迟 (RTT)、码率与丢包率"
        webPlayerHideControls = "隐藏按键"
        webPlayerShowControls = "显示按键"
        webPlayerReload = "刷新"
        webPlayerExit = "退出"
        webPlayerStretchShort = "拉伸"
        webPlayerAuthPopupTitle = "登录窗口"
    }
}

val LocalNexusStrings = staticCompositionLocalOf { AppLocalization.stringsFor(AppLanguage.ENGLISH) }

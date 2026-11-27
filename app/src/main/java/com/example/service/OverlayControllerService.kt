package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.capture.CaptureBridge
import com.example.cloud.CloudLayoutSyncRepository
import com.example.data.NexusDatabase
import com.example.data.OverlaySettings
import com.example.data.PreferencesRepository
import com.example.data.ProfileRepository
import com.example.filter.FreestyleGlTextureView
import com.example.filter.GameFilterBridge
import com.example.input.GyroAimEngine
import com.example.input.InputBridge
import com.example.model.AppColorSchemeConfig
import com.example.model.AppFontConfig
import com.example.model.AppLanguage
import com.example.model.AppLocalization
import com.example.model.AxisPair
import com.example.model.ControlShape
import com.example.model.ControlType
import com.example.model.ControllerProfile
import com.example.model.CrosshairPresets
import com.example.model.CrosshairSettings
import com.example.model.FloatingBubbleConfig
import com.example.model.FloatingBubbleStyle
import com.example.model.GameFilterState
import com.example.model.GyroAimConfig
import com.example.model.LocalNexusStrings
import com.example.model.PerfHudConfig
import com.example.model.ReplayBufferPreset
import com.example.model.TargetPlatformMode
import com.example.model.VirtualControl
import com.example.ui.components.CommandPickerSheetContent
import com.example.ui.components.ControlInspectorSheetContent
import com.example.ui.components.CrosshairOverlayCanvas
import com.example.ui.components.CrosshairPanelContent
import com.example.ui.components.FloatingBubbleVisual
import com.example.ui.components.GeForceFilterPanelContent
import com.example.ui.components.GridSettingsContent
import com.example.ui.components.LayoutStudioHelper
import com.example.ui.components.LiveOverlayEditToolbar
import com.example.ui.components.PerformanceHudPill
import com.example.ui.components.ScreenFilterOverlayCanvas
import com.example.ui.components.VirtualControlCanvasItem
import com.example.ui.components.computeOverlayBlurRadius
import com.example.ui.components.computeOverlayDimAmount
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.NexusPadTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import com.example.util.SystemIntegrations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

class OverlayControllerService : Service() {

    companion object {
        private const val CHANNEL_ID = "nexuspad_overlay_channel"
        private const val NOTIFICATION_ID = 4107
        const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"
        const val ACTION_OPEN_FLOATING_KB = "com.example.action.OPEN_FLOATING_KB"
        const val EXTRA_VISUALS_TAB = "extra_visuals_tab"

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, OverlayControllerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startFloatingKeyboard(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, OverlayControllerService::class.java).apply {
                action = ACTION_OPEN_FLOATING_KB
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayControllerService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private lateinit var lifecycleOwner: OverlayLifecycleOwner
    private val controlViews = mutableListOf<ComposeView>()
    private val controlViewParams = mutableListOf<WindowManager.LayoutParams>()

    private var gpuFilterOverlayView: FreestyleGlTextureView? = null
    private var gpuFilterOverlayParams: WindowManager.LayoutParams? = null
    private var filterOverlayView: ComposeView? = null
    private var filterOverlayParams: WindowManager.LayoutParams? = null
    private var lastAppliedFilterFlags: Int = -1
    private var lastAppliedFilterDim: Float = -1f
    private var lastAppliedFilterBlur: Int = -1
    private var crosshairOverlayView: ComposeView? = null
    private var crosshairOverlayParams: WindowManager.LayoutParams? = null
    private var floatingKeyboardView: ComposeView? = null
    private var floatingKeyboardParams: WindowManager.LayoutParams? = null
    private var floatingBubbleView: ComposeView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private var leftMenuView: ComposeView? = null
    private var filterPanelView: ComposeView? = null
    private var crosshairPanelView: ComposeView? = null
    private var layoutPickerPanelView: ComposeView? = null
    private var captureBannerView: ComposeView? = null
    private var perfHudView: ComposeView? = null
    private var perfHudParams: WindowManager.LayoutParams? = null
    private var liveEditBackdropView: ComposeView? = null
    private var liveEditToolbarView: ComposeView? = null
    private var liveEditToolbarParams: WindowManager.LayoutParams? = null
    private var liveEditStudioSheetView: ComposeView? = null

    private enum class LiveEditStudioSheet {
        ADD_CONTROL,
        REBIND_CONTROL,
        CUSTOMIZE_CONTROL,
        GRID_SETTINGS
    }

    private var gyroAimEngine: GyroAimEngine? = null
    private var performanceMonitor: PerformanceMonitor? = null

    private val buttonsVisible = MutableStateFlow(true)
    private val bubbleLocked = MutableStateFlow(false)
    private val leftMenuOpen = MutableStateFlow(false)
    private val filterPanelOpen = MutableStateFlow(false)
    private val crosshairPanelOpen = MutableStateFlow(false)
    private val layoutPickerPanelOpen = MutableStateFlow(false)
    private val floatingKeyboardOpen = MutableStateFlow(false)
    private val floatingKeyboardDefaultFlow = MutableStateFlow(false)

    private val filterStateFlow = MutableStateFlow(GameFilterState.default())
    private val crosshairFlow = MutableStateFlow(CrosshairSettings())
    private val gyroAimFlow = MutableStateFlow(GyroAimConfig())
    private val perfHudFlow = MutableStateFlow(PerfHudConfig())
    private val liveEditModeFlow = MutableStateFlow(false)
    private val liveEditDraftProfileFlow = MutableStateFlow<ControllerProfile?>(null)
    private val liveEditUndoStackFlow = MutableStateFlow<List<ControllerProfile>>(emptyList())
    private val liveEditSelectedIdFlow = MutableStateFlow<String?>(null)
    private val liveEditSnapGridFlow = MutableStateFlow(true)
    private val liveEditGridDivisionsFlow = MutableStateFlow(12)
    private val liveEditActiveSheetFlow = MutableStateFlow<LiveEditStudioSheet?>(null)
    private val liveEditControlEntryMap = linkedMapOf<String, Pair<ComposeView, WindowManager.LayoutParams>>()

    private val appLanguageFlow = MutableStateFlow(AppLanguage.SYSTEM)
    private val targetModeFlow = MutableStateFlow(TargetPlatformMode.GEFORCE_NOW)
    private val replayDurationSecFlow = MutableStateFlow(30)
    private val colorSchemeFlow = MutableStateFlow(AppColorSchemeConfig())
    private val fontConfigFlow = MutableStateFlow(AppFontConfig())
    private val floatingBubbleFlow = MutableStateFlow(FloatingBubbleConfig())
    private val activeProfileName = MutableStateFlow("XenosPad")
    private val activeProfileIdFlow = MutableStateFlow<String?>(null)
    private val allProfilesFlow = MutableStateFlow<List<ControllerProfile>>(emptyList())

    private var lastRenderedProfile: ControllerProfile? = null
    private var lastRenderedSettings: OverlaySettings = OverlaySettings()

    private var profileRepoRef: ProfileRepository? = null
    private var prefsRepoRef: PreferencesRepository? = null
    private var collectorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner = OverlayLifecycleOwner().apply {
            performRestore(null)
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        gyroAimEngine = GyroAimEngine(applicationContext)
        performanceMonitor = PerformanceMonitor(applicationContext)

        InputBridge.setOverlayRunning(true)
        InputBridge.attachFloatKeyboardHandler {
            serviceScope.launch {
                floatingKeyboardOpen.value = true
            }
        }
        attachFloatingBubble()
        observeActiveProfile()
        observeLeftMenuState()
        observeFilterPanelState()
        observeCrosshairPanelState()
        observeLayoutPickerPanelState()
        observeFloatingKeyboardState()
        observeCaptureStatusBanner()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_OPEN_FLOATING_KB -> {
                floatingKeyboardOpen.value = true
            }
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val (screenW, screenH) = realScreenSize()

        // Clamp floating bubble inside updated landscape/portrait screen bounds
        floatingBubbleView?.let { bubble ->
            val bp = bubbleParams ?: return@let
            bp.x = bp.x.coerceIn(0, (screenW - bp.width).coerceAtLeast(0))
            bp.y = bp.y.coerceIn(0, (screenH - bp.height).coerceAtLeast(0))
            runCatching { windowManager.updateViewLayout(bubble, bp) }
        }

        // Clamp Perf HUD pill inside updated screen bounds
        perfHudView?.let { hud ->
            val hp = perfHudParams ?: return@let
            hp.x = hp.x.coerceIn(0, (screenW - 220).coerceAtLeast(0))
            hp.y = hp.y.coerceIn(0, (screenH - 80).coerceAtLeast(0))
            runCatching { windowManager.updateViewLayout(hud, hp) }
        }

        // Clamp Live Edit toolbar inside updated screen bounds
        liveEditToolbarView?.let { tb ->
            val tp = liveEditToolbarParams ?: return@let
            tp.x = tp.x.coerceIn(0, (screenW - 360).coerceAtLeast(0))
            tp.y = tp.y.coerceIn(0, (screenH - 180).coerceAtLeast(0))
            runCatching { windowManager.updateViewLayout(tb, tp) }
        }

        // Re-layout virtual controls edge-to-edge across the new orientation dimensions
        clearControlWindows()
        if (liveEditModeFlow.value) {
            showLiveEditBackdrop()
            val draft = liveEditDraftProfileFlow.value
            if (draft != null) {
                renderLiveEditControlWindows(draft, lastRenderedSettings)
            }
        } else {
            val profile = lastRenderedProfile
            if (buttonsVisible.value && profile != null) {
                renderControlWindows(profile, lastRenderedSettings)
            }
        }
        if (floatingKeyboardOpen.value) {
            dismissFloatingKeyboardOverlay()
            showFloatingKeyboardOverlay()
        }
        ensureFloatingChromeOnTop()
    }

    private fun realScreenSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            if (bounds.width() > 0 && bounds.height() > 0) {
                return bounds.width() to bounds.height()
            }
        }
        val pt = Point()
        @Suppress("DEPRECATION")
        runCatching { windowManager.defaultDisplay?.getRealSize(pt) }
        return if (pt.x > 0 && pt.y > 0) {
            pt.x to pt.y
        } else {
            val dm = resources.displayMetrics
            dm.widthPixels to dm.heightPixels
        }
    }

    private fun applyFullScreenOverlayFlags(params: WindowManager.LayoutParams) {
        params.flags = params.flags or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun observeActiveProfile() {
        val db = NexusDatabase.getInstance(applicationContext)
        val profileRepo = ProfileRepository(db.profileDao())
        profileRepoRef = profileRepo
        val prefsRepo = PreferencesRepository(applicationContext)
        prefsRepoRef = prefsRepo

        collectorJob?.cancel()
        collectorJob = serviceScope.launch {
            profileRepo.ensureSeeded()
            launch {
                profileRepo.profiles.collect { list ->
                    allProfilesFlow.value = list
                }
            }
            launch {
                prefsRepo.settingsFlow
                    .map { it.gameFilter }
                    .distinctUntilChanged()
                    .collect { persistedFilter ->
                        GameFilterBridge.syncFromPersisted(persistedFilter)
                    }
            }
            launch {
                combine(
                    GameFilterBridge.liveFilterState,
                    GameFilterBridge.isGpuShaderActive
                ) { filterState, gpuActive ->
                    filterState to gpuActive
                }.collect { (filterState, gpuActive) ->
                    applyLiveFilterState(filterState, gpuActive)
                }
            }
            launch {
                GameFilterBridge.gpuContentSize.collect { (w, h) ->
                    gpuFilterOverlayView?.updateProjectionBufferSize(w, h)
                }
            }
            launch {
                prefsRepo.settingsFlow
                    .map { it.crosshair }
                    .distinctUntilChanged()
                    .collect { crosshair ->
                        applyLiveCrosshairState(crosshair)
                    }
            }
            launch {
                prefsRepo.settingsFlow
                    .map { Triple(it.appLanguage, it.floatingKeyboardDefault, it.targetMode) }
                    .distinctUntilChanged()
                    .collect { (lang, floatKb, mode) ->
                        appLanguageFlow.value = lang
                        floatingKeyboardDefaultFlow.value = floatKb
                        targetModeFlow.value = mode
                        InputBridge.setTargetMode(mode)
                    }
            }
            launch {
                prefsRepo.settingsFlow
                    .map { Triple(it.instantReplayDurationSec, it.recordingResolution, it.recordingFps) }
                    .distinctUntilChanged()
                    .collect { (sec, res, fps) ->
                        replayDurationSecFlow.value = sec
                        CaptureBridge.setReplayDurationSec(sec)
                        CaptureBridge.setRecordingResolution(res)
                        CaptureBridge.setRecordingFps(fps)
                    }
            }

            launch {
                prefsRepo.settingsFlow
                    .map { Triple(it.colorScheme, it.fontConfig, it.floatingBubble) }
                    .distinctUntilChanged()
                    .collect { (scheme, fontCfg, bubbleCfg) ->
                        colorSchemeFlow.value = scheme
                        fontConfigFlow.value = fontCfg
                        floatingBubbleFlow.value = bubbleCfg
                        updateFloatingBubbleWindowSize(bubbleCfg)
                    }
            }

            launch {
                prefsRepo.settingsFlow
                    .map { it.gyroAim }
                    .distinctUntilChanged()
                    .collect { cfg ->
                        gyroAimFlow.value = cfg
                        gyroAimEngine?.updateConfig(cfg, pauseForEdit = liveEditModeFlow.value)
                    }
            }

            launch {
                prefsRepo.settingsFlow
                    .map { it.perfHud }
                    .distinctUntilChanged()
                    .collect { cfg ->
                        perfHudFlow.value = cfg
                        syncPerfHudOverlay(cfg)
                    }
            }

            val controlSettingsFlow = prefsRepo.settingsFlow
                .map { s ->
                    s.copy(
                        gameFilter = GameFilterState.default(),
                        crosshair = CrosshairSettings(),
                        recordingResolution = com.example.model.RecordingResolution.P720,
                        recordingFps = com.example.model.RecordingFps.FPS_30,
                        githubRepoSlug = "",
                        autoCheckUpdates = true,
                        colorScheme = AppColorSchemeConfig(),
                        fontConfig = AppFontConfig(),
                        floatingBubble = FloatingBubbleConfig(),
                        customTestBgPath = "",
                        lastSeenWhatsNewVersionCode = 0,
                        gyroAim = GyroAimConfig(),
                        perfHud = PerfHudConfig(),
                        appearanceUpdatedAtMs = 0L
                    )
                }
                .distinctUntilChanged()

            com.example.input.PhysicalControllerMonitor.ensureStarted(applicationContext)

            combine(
                profileRepo.activeProfile,
                controlSettingsFlow,
                buttonsVisible,
                com.example.input.PhysicalControllerMonitor.connectedControllers
            ) { profile, settings, visible, controllers ->
                val autoHidden = com.example.input.PhysicalControllerMonitor.shouldAutoHideTouchControls(
                    connectedCount = controllers.size,
                    autoHideEnabled = settings.physicalController.autoHideTouchControls,
                    manualOverrideShow = false
                )
                Triple(profile, settings, visible && !autoHidden)
            }.collect { (profile, settings, visible) ->
                lastRenderedProfile = profile
                lastRenderedSettings = settings
                activeProfileName.value = profile?.name ?: "XenosPad"
                activeProfileIdFlow.value = profile?.id
                if (liveEditModeFlow.value) {
                    return@collect
                }
                clearControlWindows()
                InputBridge.releaseAll()
                if (visible && profile != null) {
                    renderControlWindows(profile, settings)
                }
                ensureFloatingChromeOnTop()
            }
        }
    }

    private fun observeLeftMenuState() {
        serviceScope.launch {
            leftMenuOpen.collect { open ->
                if (open) {
                    filterPanelOpen.value = false
                    crosshairPanelOpen.value = false
                    layoutPickerPanelOpen.value = false
                    showLeftMenuOverlay()
                } else {
                    dismissLeftMenuOverlay()
                }
            }
        }
    }

    private fun observeFilterPanelState() {
        serviceScope.launch {
            filterPanelOpen.collect { open ->
                if (open) {
                    leftMenuOpen.value = false
                    crosshairPanelOpen.value = false
                    layoutPickerPanelOpen.value = false
                    showFloatingFilterPanel()
                } else {
                    dismissFloatingFilterPanel()
                }
            }
        }
    }

    private fun observeCrosshairPanelState() {
        serviceScope.launch {
            crosshairPanelOpen.collect { open ->
                if (open) {
                    leftMenuOpen.value = false
                    filterPanelOpen.value = false
                    layoutPickerPanelOpen.value = false
                    showFloatingCrosshairPanel()
                } else {
                    dismissFloatingCrosshairPanel()
                }
            }
        }
    }

    private fun observeLayoutPickerPanelState() {
        serviceScope.launch {
            layoutPickerPanelOpen.collect { open ->
                if (open) {
                    leftMenuOpen.value = false
                    filterPanelOpen.value = false
                    crosshairPanelOpen.value = false
                    showFloatingLayoutPickerPanel()
                } else {
                    dismissFloatingLayoutPickerPanel()
                }
            }
        }
    }

    private fun observeCaptureStatusBanner() {
        serviceScope.launch {
            CaptureBridge.statusBanner.collect { msg ->
                if (!msg.isNullOrBlank()) {
                    showCaptureBanner(msg)
                    delay(2400L)
                    if (CaptureBridge.statusBanner.value == msg) {
                        CaptureBridge.postBanner(null)
                    }
                } else {
                    dismissCaptureBanner()
                }
            }
        }
    }

    private fun observeFloatingKeyboardState() {
        serviceScope.launch {
            floatingKeyboardOpen.collect { open ->
                if (open) {
                    showFloatingKeyboardOverlay()
                } else {
                    dismissFloatingKeyboardOverlay()
                }
            }
        }
    }

    private fun cycleNextProfile() {
        val repo = profileRepoRef ?: return
        val list = allProfilesFlow.value
        if (list.size <= 1) return
        val currentIdx = list.indexOfFirst { it.isActive }
        val next = list[(currentIdx + 1) % list.size]
        serviceScope.launch {
            repo.setActiveProfile(next.id)
        }
    }

    private fun openMainApp(openCrosshairTab: Boolean = false, openFilterTab: Boolean = false) {
        leftMenuOpen.value = false
        filterPanelOpen.value = false
        crosshairPanelOpen.value = false
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            when {
                openCrosshairTab -> putExtra(EXTRA_VISUALS_TAB, "crosshair")
                openFilterTab -> putExtra(EXTRA_VISUALS_TAB, "filter")
            }
        }
        runCatching { startActivity(intent) }
    }

    private fun openKeyboardFromMenu() {
        leftMenuOpen.value = false
        if (floatingKeyboardDefaultFlow.value) {
            floatingKeyboardOpen.value = true
            return
        }
        val shown = InputBridge.requestShowKeyboard()
        if (!shown) {
            floatingKeyboardOpen.value = true
        }
    }

    private fun computeBubblePixelSize(config: FloatingBubbleConfig): Pair<Int, Int> {
        val density = resources.displayMetrics.density
        val baseDp = config.clampedSizeDp()
        val wDp = if (config.style == FloatingBubbleStyle.CYBER_PILL) baseDp * 1.45f else baseDp
        val hDp = if (config.style == FloatingBubbleStyle.CYBER_PILL) baseDp * 0.86f else baseDp
        return (wDp * density).roundToInt().coerceAtLeast(36) to (hDp * density).roundToInt().coerceAtLeast(36)
    }

    private fun updateFloatingBubbleWindowSize(config: FloatingBubbleConfig) {
        val bubble = floatingBubbleView ?: return
        val bp = bubbleParams ?: return
        val (newW, newH) = computeBubblePixelSize(config)
        if (bp.width == newW && bp.height == newH) return
        val (screenW, screenH) = realScreenSize()
        bp.width = newW
        bp.height = newH
        bp.x = bp.x.coerceIn(0, (screenW - newW).coerceAtLeast(0))
        bp.y = bp.y.coerceIn(0, (screenH - newH).coerceAtLeast(0))
        runCatching { windowManager.updateViewLayout(bubble, bp) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun attachFloatingBubble() {
        val (screenW, screenH) = realScreenSize()
        val density = resources.displayMetrics.density
        val touchSlopPx = ViewConfiguration.get(this).scaledTouchSlop.toFloat()
        val (initW, initH) = computeBubblePixelSize(floatingBubbleFlow.value)
        val params = WindowManager.LayoutParams(
            initW,
            initH,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (18 * density).roundToInt()
            y = (screenH * 0.14f).roundToInt().coerceIn(0, (screenH - initH).coerceAtLeast(0))
            applyFullScreenOverlayFlags(this)
        }
        bubbleParams = params

        val bubble = ComposeView(this).apply {
            attachLifecycle(this)
            var dragStartRawX = 0f
            var dragStartRawY = 0f
            var dragStartWinX = 0
            var dragStartWinY = 0
            var dragCurW = screenW
            var dragCurH = screenH
            var didDrag = false

            setContent {
                val visible by buttonsVisible.collectAsState()
                val locked by bubbleLocked.collectAsState()
                val menuOpen by leftMenuOpen.collectAsState()
                val filterOpen by filterPanelOpen.collectAsState()
                val crossOpen by crosshairPanelOpen.collectAsState()
                val layoutOpen by layoutPickerPanelOpen.collectAsState()
                val isRec by CaptureBridge.isRecording.collectAsState()
                val isRep by CaptureBridge.isInstantReplayOn.collectAsState()
                val colorScheme by colorSchemeFlow.collectAsState()
                val fontConfig by fontConfigFlow.collectAsState()
                val bubbleConfig by floatingBubbleFlow.collectAsState()
                val anyPanelOpen = menuOpen || filterOpen || crossOpen || layoutOpen

                NexusPadTheme(
                    darkTheme = true,
                    colorSchemeConfig = colorScheme,
                    fontConfig = fontConfig
                ) {
                    val baseAccent = if (bubbleConfig.useThemeAccent) {
                        Color(colorScheme.effectivePrimaryHex())
                    } else {
                        Color(bubbleConfig.customAccentHex)
                    }
                    val accent = when {
                        isRec -> MacroCoral
                        anyPanelOpen -> Color(colorScheme.effectiveSecondaryHex())
                        locked -> TurboAmber
                        !visible -> Color(0xFF9FB0C7)
                        else -> baseAccent
                    }
                    FloatingBubbleVisual(
                        config = bubbleConfig,
                        accentColor = accent,
                        isPanelOpen = anyPanelOpen,
                        isRecording = isRec,
                        isReplayOn = isRep,
                        isLocked = locked,
                        isHidden = !visible,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInteropFilter { event ->
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> {
                                        dragStartRawX = event.rawX
                                        dragStartRawY = event.rawY
                                        dragStartWinX = params.x
                                        dragStartWinY = params.y
                                        didDrag = false
                                        val (cw, ch) = realScreenSize()
                                        dragCurW = cw
                                        dragCurH = ch
                                        true
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        val totalDx = event.rawX - dragStartRawX
                                        val totalDy = event.rawY - dragStartRawY
                                        if (!didDrag && hypot(totalDx, totalDy) > touchSlopPx) {
                                            didDrag = true
                                        }
                                        if (didDrag && !bubbleLocked.value) {
                                            val newX = (dragStartWinX + totalDx).roundToInt()
                                                .coerceIn(0, (dragCurW - params.width).coerceAtLeast(0))
                                            val newY = (dragStartWinY + totalDy).roundToInt()
                                                .coerceIn(0, (dragCurH - params.height).coerceAtLeast(0))
                                            if (newX != params.x || newY != params.y) {
                                                params.x = newX
                                                params.y = newY
                                                runCatching { windowManager.updateViewLayout(this@apply, params) }
                                            }
                                        }
                                        true
                                    }
                                    MotionEvent.ACTION_UP -> {
                                        if (!didDrag) {
                                            if (filterPanelOpen.value || crosshairPanelOpen.value || layoutPickerPanelOpen.value) {
                                                filterPanelOpen.value = false
                                                crosshairPanelOpen.value = false
                                                layoutPickerPanelOpen.value = false
                                            } else {
                                                leftMenuOpen.value = !leftMenuOpen.value
                                            }
                                        }
                                        true
                                    }
                                    else -> true
                                }
                            }
                    )
                }
            }
        }

        floatingBubbleView = bubble
        runCatching { windowManager.addView(bubble, params) }
    }

    private fun applyLiveFilterState(state: GameFilterState, gpuActive: Boolean) {
        filterStateFlow.value = state
        val activeStyle = state.activeStyle
        if (activeStyle == null) {
            removeScreenFilterOverlay()
            removeGpuFilterOverlay()
            return
        }

        // 1. Real OpenGL ES 2.0 Live GPU Shader Surface (when MediaProjection GPU Engine is active)
        if (gpuActive) {
            val existingGl = gpuFilterOverlayView
            if (existingGl == null) {
                val glParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    overlayWindowType(),
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    applyFullScreenOverlayFlags(this)
                }
                val glView = FreestyleGlTextureView(this, FreestyleGlTextureView.Mode.LIVE_PROJECTION).apply {
                    setFilterStyle(activeStyle)
                    setProjectionSurfaceCallback { surface, w, h ->
                        GameFilterBridge.publishGpuTargetSurface(surface, w, h)
                    }
                }
                gpuFilterOverlayView = glView
                gpuFilterOverlayParams = glParams
                runCatching { windowManager.addView(glView, glParams) }
                reStackAllLayers()
            } else {
                existingGl.setFilterStyle(activeStyle)
            }
        } else {
            removeGpuFilterOverlay()
        }

        // 2. Compose Sticker / Quick Optical Overlay Layer
        val dim = if (gpuActive) 0f else computeOverlayDimAmount(activeStyle)
        val blurRadius = computeOverlayBlurRadius(activeStyle, gpuShaderActive = gpuActive)
        var baseFlags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (dim > 0.005f) {
            baseFlags = baseFlags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0) {
            baseFlags = baseFlags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        }

        val existingView = filterOverlayView
        val existingParams = filterOverlayParams
        if (existingView != null && existingParams != null) {
            val layoutChanged = baseFlags != lastAppliedFilterFlags ||
                kotlin.math.abs(dim - lastAppliedFilterDim) > 0.004f ||
                blurRadius != lastAppliedFilterBlur
            if (layoutChanged) {
                existingParams.flags = baseFlags
                applyFullScreenOverlayFlags(existingParams)
                existingParams.dimAmount = dim
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    existingParams.blurBehindRadius = blurRadius
                }
                lastAppliedFilterFlags = baseFlags
                lastAppliedFilterDim = dim
                lastAppliedFilterBlur = blurRadius
                runCatching { windowManager.updateViewLayout(existingView, existingParams) }
            }
        } else {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayWindowType(),
                baseFlags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                dimAmount = dim
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    blurBehindRadius = blurRadius
                }
                applyFullScreenOverlayFlags(this)
            }
            val view = ComposeView(this).apply {
                attachLifecycle(this)
                setContent {
                    val currentFilter by filterStateFlow.collectAsState()
                    val gpuOn by GameFilterBridge.isGpuShaderActive.collectAsState()
                    val styleToDraw = currentFilter.activeStyle
                    if (styleToDraw != null) {
                        ScreenFilterOverlayCanvas(
                            style = styleToDraw,
                            gpuShaderActive = gpuOn
                        )
                    }
                }
            }
            filterOverlayView = view
            filterOverlayParams = params
            lastAppliedFilterFlags = baseFlags
            lastAppliedFilterDim = dim
            lastAppliedFilterBlur = blurRadius
            runCatching { windowManager.addView(view, params) }
            reStackAllLayers()
        }
    }

    private fun removeGpuFilterOverlay() {
        gpuFilterOverlayView?.let { glView ->
            glView.setProjectionSurfaceCallback(null)
            glView.setFilterStyle(null)
            runCatching { windowManager.removeViewImmediate(glView) }
        }
        gpuFilterOverlayView = null
        gpuFilterOverlayParams = null
        GameFilterBridge.publishGpuTargetSurface(null)
    }

    private fun removeScreenFilterOverlay() {
        filterOverlayView?.let { view ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        filterOverlayView = null
        filterOverlayParams = null
        lastAppliedFilterFlags = -1
        lastAppliedFilterDim = -1f
        lastAppliedFilterBlur = -1
    }

    private fun applyLiveCrosshairState(crosshair: CrosshairSettings) {
        crosshairFlow.value = crosshair
        if (!crosshair.enabled) {
            removeCrosshairOverlay()
            return
        }

        if (crosshairOverlayView == null) {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                applyFullScreenOverlayFlags(this)
            }
            val view = ComposeView(this).apply {
                attachLifecycle(this)
                setContent {
                    val current by crosshairFlow.collectAsState()
                    CrosshairOverlayCanvas(settings = current)
                }
            }
            crosshairOverlayView = view
            crosshairOverlayParams = params
            runCatching { windowManager.addView(view, params) }
            reStackAllLayers()
        }
    }

    private fun removeCrosshairOverlay() {
        crosshairOverlayView?.let { view ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        crosshairOverlayView = null
        crosshairOverlayParams = null
    }

    private fun reStackAllLayers() {
        // Keep gpuFilterOverlayView undisturbed at the very bottom so its EGL surface never resets;
        // stack filterOverlayView (Stickers/HUD), crosshair, controls, and floating chrome above it.
        filterOverlayView?.let { v ->
            val p = filterOverlayParams ?: return@let
            runCatching { windowManager.removeViewImmediate(v) }
            runCatching { windowManager.addView(v, p) }
        }
        crosshairOverlayView?.let { v ->
            val p = crosshairOverlayParams ?: return@let
            runCatching { windowManager.removeViewImmediate(v) }
            runCatching { windowManager.addView(v, p) }
        }
        val snapshot = controlViews.zip(controlViewParams)
        snapshot.forEach { (view, _) ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        snapshot.forEach { (view, params) ->
            runCatching { windowManager.addView(view, params) }
        }
        ensureFloatingChromeOnTop()
    }

    private fun showLeftMenuOverlay() {
        if (leftMenuView != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            applyFullScreenOverlayFlags(this)
        }

        val view = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val visible by buttonsVisible.collectAsState()
                val locked by bubbleLocked.collectAsState()
                val profileName by activeProfileName.collectAsState()
                val profileCount = allProfilesFlow.collectAsState().value.size
                val filterState by filterStateFlow.collectAsState()
                val crosshair by crosshairFlow.collectAsState()
                val appLang by appLanguageFlow.collectAsState()
                val targetMode by targetModeFlow.collectAsState()
                val replaySec by replayDurationSecFlow.collectAsState()
                val isReplayOn by CaptureBridge.isInstantReplayOn.collectAsState()
                val isRecording by CaptureBridge.isRecording.collectAsState()
                val recElapsedMs by CaptureBridge.recordingElapsedMs.collectAsState()
                val colorScheme by colorSchemeFlow.collectAsState()
                val fontConfig by fontConfigFlow.collectAsState()
                val gyroAim by gyroAimFlow.collectAsState()
                val perfHud by perfHudFlow.collectAsState()
                val localizedStrings = remember(appLang) { AppLocalization.stringsFor(appLang) }

                CompositionLocalProvider(LocalNexusStrings provides localizedStrings) {
                    NexusPadTheme(
                        darkTheme = true,
                        colorSchemeConfig = colorScheme,
                        fontConfig = fontConfig
                    ) {
                        LeftSideOverlayMenu(
                            profileName = profileName,
                            buttonsVisible = visible,
                            bubbleLocked = locked,
                            canSwitchProfile = profileCount > 0,
                            activeFilterSlot = filterState.activeSlot,
                            crosshairEnabled = crosshair.enabled,
                            gyroEnabled = gyroAim.enabled,
                            hasHardwareGyro = gyroAimEngine?.hasHardwareGyro == true,
                            perfHudEnabled = perfHud.enabled,
                            targetMode = targetMode,
                            isReplayOn = isReplayOn,
                            replayDurationSec = replaySec,
                            isRecording = isRecording,
                            recordingElapsedMs = recElapsedMs,
                            onDismiss = { leftMenuOpen.value = false },
                            onToggleButtons = {
                                buttonsVisible.value = !visible
                                leftMenuOpen.value = false
                            },
                            onOpenLayouts = {
                                leftMenuOpen.value = false
                                layoutPickerPanelOpen.value = true
                            },
                            onStartLiveEdit = {
                                leftMenuOpen.value = false
                                enterLiveEditMode()
                            },
                            onToggleGyro = {
                                val next = gyroAim.copy(enabled = !gyroAim.enabled)
                                gyroAimFlow.value = next
                                gyroAimEngine?.updateConfig(next, pauseForEdit = liveEditModeFlow.value)
                                serviceScope.launch { prefsRepoRef?.updateGyroAim(next) }
                            },
                            onTogglePerfHud = {
                                val next = perfHud.copy(enabled = !perfHud.enabled)
                                perfHudFlow.value = next
                                syncPerfHudOverlay(next)
                                serviceScope.launch { prefsRepoRef?.updatePerfHud(next) }
                            },
                            onToggleTargetMode = {
                                val next = if (targetMode == TargetPlatformMode.GEFORCE_NOW) {
                                    TargetPlatformMode.ANDROID_NATIVE
                                } else {
                                    TargetPlatformMode.GEFORCE_NOW
                                }
                                targetModeFlow.value = next
                                InputBridge.setTargetMode(next)
                                serviceScope.launch {
                                    prefsRepoRef?.setTargetPlatformMode(next)
                                }
                            },
                            onOpenFilters = {
                                leftMenuOpen.value = false
                                filterPanelOpen.value = true
                            },
                            onOpenCrosshair = {
                                leftMenuOpen.value = false
                                crosshairPanelOpen.value = true
                            },
                            onTakeScreenshot = {
                                leftMenuOpen.value = false
                                ScreenCaptureService.requestScreenshot(this@OverlayControllerService)
                            },
                            onToggleReplay = {
                                leftMenuOpen.value = false
                                ScreenCaptureService.toggleInstantReplay(this@OverlayControllerService, replaySec)
                            },
                            onSaveReplay = {
                                leftMenuOpen.value = false
                                ScreenCaptureService.saveInstantReplayClip(this@OverlayControllerService, replaySec)
                            },
                            onToggleRecord = {
                                leftMenuOpen.value = false
                                ScreenCaptureService.toggleRecording(this@OverlayControllerService)
                            },
                            onOpenKeyboard = { openKeyboardFromMenu() },
                            onOpenFloatingKeyboard = {
                                leftMenuOpen.value = false
                                floatingKeyboardOpen.value = !floatingKeyboardOpen.value
                            },
                            onReleaseAll = {
                                InputBridge.releaseAll()
                                leftMenuOpen.value = false
                            },
                            onToggleLock = {
                                bubbleLocked.value = !locked
                                leftMenuOpen.value = false
                            },
                            onStopOverlay = {
                                leftMenuOpen.value = false
                                stopSelf()
                            },
                            onOpenApp = { openMainApp(openCrosshairTab = false) }
                        )
                    }
                }
            }
        }
        leftMenuView = view
        runCatching { windowManager.addView(view, params) }
    }

    private fun dismissLeftMenuOverlay() {
        leftMenuView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        leftMenuView = null
    }

    private fun showFloatingFilterPanel() {
        if (filterPanelView != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            applyFullScreenOverlayFlags(this)
        }

        val panel = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val filterState by GameFilterBridge.liveFilterState.collectAsState()
                val isGpuShaderActive by GameFilterBridge.isGpuShaderActive.collectAsState()
                val appLang by appLanguageFlow.collectAsState()
                val colorScheme by colorSchemeFlow.collectAsState()
                val fontConfig by fontConfigFlow.collectAsState()
                val localizedStrings = remember(appLang) { AppLocalization.stringsFor(appLang) }

                CompositionLocalProvider(LocalNexusStrings provides localizedStrings) {
                    NexusPadTheme(
                        darkTheme = true,
                        colorSchemeConfig = colorScheme,
                        fontConfig = fontConfig
                    ) {
                        LeftDockedPanelWrapper(
                            onDismiss = { filterPanelOpen.value = false }
                        ) {
                            GeForceFilterPanelContent(
                                filterState = filterState,
                                compactMode = true,
                                isGpuShaderActive = isGpuShaderActive,
                                onToggleGpuShader = {
                                    if (!isGpuShaderActive && filterState.activeSlot == 0) {
                                        GameFilterBridge.selectLiveSlot(1)
                                        serviceScope.launch {
                                            prefsRepoRef?.setActiveFilterSlot(1)
                                        }
                                    }
                                    ScreenCaptureService.toggleGpuFilter(this@OverlayControllerService)
                                },
                                onUploadStickerClick = {
                                    openMainApp(openFilterTab = true)
                                },
                                onSelectSlot = { slot ->
                                    GameFilterBridge.selectLiveSlot(slot)
                                    serviceScope.launch {
                                        prefsRepoRef?.setActiveFilterSlot(slot)
                                    }
                                },
                                onPreviewStyle = { previewStyle ->
                                    GameFilterBridge.updateLiveStyle(previewStyle)
                                },
                                onUpdateStyle = { updatedStyle ->
                                    GameFilterBridge.updateLiveStyle(updatedStyle)
                                    serviceScope.launch {
                                        prefsRepoRef?.updateFilterStyle(updatedStyle)
                                    }
                                },
                                onResetSlot = {
                                    val slot = filterState.activeSlot
                                    if (slot in 1..3) {
                                        GameFilterBridge.syncFromPersisted(filterState.withResetSlot(slot))
                                        serviceScope.launch {
                                            prefsRepoRef?.resetFilterSlot(slot)
                                        }
                                    }
                                },
                                onClose = { filterPanelOpen.value = false }
                            )
                        }
                    }
                }
            }
        }
        filterPanelView = panel
        runCatching { windowManager.addView(panel, params) }
    }

    private fun dismissFloatingFilterPanel() {
        filterPanelView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        filterPanelView = null
    }

    private fun showFloatingCrosshairPanel() {
        if (crosshairPanelView != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            applyFullScreenOverlayFlags(this)
        }

        val panel = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val crosshair by crosshairFlow.collectAsState()
                val appLang by appLanguageFlow.collectAsState()
                val colorScheme by colorSchemeFlow.collectAsState()
                val fontConfig by fontConfigFlow.collectAsState()
                val localizedStrings = remember(appLang) { AppLocalization.stringsFor(appLang) }

                CompositionLocalProvider(LocalNexusStrings provides localizedStrings) {
                    NexusPadTheme(
                        darkTheme = true,
                        colorSchemeConfig = colorScheme,
                        fontConfig = fontConfig
                    ) {
                        LeftDockedPanelWrapper(
                            onDismiss = { crosshairPanelOpen.value = false }
                        ) {
                            CrosshairPanelContent(
                                crosshair = crosshair,
                                compactMode = true,
                                onUpdate = { next ->
                                    applyLiveCrosshairState(next)
                                    serviceScope.launch {
                                        prefsRepoRef?.updateCrosshair(next)
                                    }
                                },
                                onSelectPreset = { preset ->
                                    val next = preset.applyTo(crosshair)
                                    applyLiveCrosshairState(next)
                                    serviceScope.launch {
                                        prefsRepoRef?.updateCrosshair(next)
                                    }
                                },
                                onReset = {
                                    val reset = CrosshairSettings(enabled = crosshair.enabled)
                                    applyLiveCrosshairState(reset)
                                    serviceScope.launch {
                                        prefsRepoRef?.updateCrosshair(reset)
                                    }
                                },
                                onUploadCustomImageClick = {
                                    openMainApp(openCrosshairTab = true)
                                },
                                onClose = { crosshairPanelOpen.value = false }
                            )
                        }
                    }
                }
            }
        }
        crosshairPanelView = panel
        runCatching { windowManager.addView(panel, params) }
    }

    private fun dismissFloatingCrosshairPanel() {
        crosshairPanelView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        crosshairPanelView = null
    }

    private fun showFloatingLayoutPickerPanel() {
        if (layoutPickerPanelView != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            applyFullScreenOverlayFlags(this)
        }

        val panel = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val profiles by allProfilesFlow.collectAsState()
                val activeId by activeProfileIdFlow.collectAsState()
                val appLang by appLanguageFlow.collectAsState()
                val colorScheme by colorSchemeFlow.collectAsState()
                val fontConfig by fontConfigFlow.collectAsState()
                val localizedStrings = remember(appLang) { AppLocalization.stringsFor(appLang) }

                CompositionLocalProvider(LocalNexusStrings provides localizedStrings) {
                    NexusPadTheme(
                        darkTheme = true,
                        colorSchemeConfig = colorScheme,
                        fontConfig = fontConfig
                    ) {
                        val accentColor = Color(colorScheme.effectivePrimaryHex())
                        LeftDockedPanelWrapper(
                            onDismiss = { layoutPickerPanelOpen.value = false }
                        ) {
                            Surface(
                                color = Color(0xF20D121A),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.2.dp, Color(0xFF2B384C), RoundedCornerShape(18.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = localizedStrings.menuChooseLayoutHeader,
                                                color = accentColor,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = localizedStrings.menuLayouts,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = { layoutPickerPanelOpen.value = false },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = localizedStrings.commonClose, tint = Color(0xFFA2B3C9))
                                        }
                                    }

                                    profiles.forEach { profile ->
                                        val isCurrent = profile.id == activeId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isCurrent) accentColor.copy(alpha = 0.14f) else Color(0xFF151C28))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isCurrent) accentColor else Color(0xFF2B384C),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    layoutPickerPanelOpen.value = false
                                                    serviceScope.launch {
                                                        profileRepoRef?.setActiveProfile(profile.id)
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = profile.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${profile.gameTitle} • ${profile.controls.size} ${localizedStrings.commonControlsSuffix}",
                                                    color = Color(0xFF9FB0C7),
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = localizedStrings.commonActive,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        layoutPickerPanelView = panel
        runCatching { windowManager.addView(panel, params) }
    }

    private fun dismissFloatingLayoutPickerPanel() {
        layoutPickerPanelView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        layoutPickerPanelView = null
    }

    private fun showCaptureBanner(message: String) {
        dismissCaptureBanner()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (18 * resources.displayMetrics.density).roundToInt()
            y = (18 * resources.displayMetrics.density).roundToInt()
            applyFullScreenOverlayFlags(this)
        }
        val view = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val colorScheme by colorSchemeFlow.collectAsState()
                val fontConfig by fontConfigFlow.collectAsState()
                val accentColor = Color(colorScheme.effectivePrimaryHex())
                NexusPadTheme(
                    darkTheme = true,
                    colorSchemeConfig = colorScheme,
                    fontConfig = fontConfig
                ) {
                    Surface(
                        color = Color(0xEC0C1219),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(1.2.dp, accentColor, RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = message,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
        captureBannerView = view
        runCatching { windowManager.addView(view, params) }
    }

    private fun dismissCaptureBanner() {
        captureBannerView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        captureBannerView = null
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun showFloatingKeyboardOverlay() {
        if (floatingKeyboardView != null) return
        val (screenW, screenH) = realScreenSize()
        val density = resources.displayMetrics.density
        val kbWidthPx = ((screenW - (24 * density).roundToInt()).coerceAtMost((540 * density).roundToInt()))
            .coerceAtLeast((280 * density).roundToInt())
        val estKbHeightPx = (290 * density).roundToInt()

        val params = WindowManager.LayoutParams(
            kbWidthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = ((screenW - kbWidthPx) / 2).coerceAtLeast(0)
            y = (screenH - estKbHeightPx - (24 * density).roundToInt()).coerceAtLeast((20 * density).roundToInt())
            applyFullScreenOverlayFlags(this)
        }
        floatingKeyboardParams = params

        val initialKbLang = when (AppLocalization.resolveEffectiveLanguage(appLanguageFlow.value)) {
            AppLanguage.THAI -> NexusVirtualKeyboardView.KeyboardLanguage.THAI
            else -> NexusVirtualKeyboardView.KeyboardLanguage.ENGLISH
        }

        val view = ComposeView(this).apply {
            attachLifecycle(this)
            var kbStartRawX = 0f
            var kbStartRawY = 0f
            var kbStartWinX = 0
            var kbStartWinY = 0
            var kbCurW = screenW
            var kbCurH = screenH

            setContent {
                val appLang by appLanguageFlow.collectAsState()
                val colorScheme by colorSchemeFlow.collectAsState()
                val fontConfig by fontConfigFlow.collectAsState()
                val localizedStrings = remember(appLang) { AppLocalization.stringsFor(appLang) }
                val accentColor = Color(colorScheme.effectivePrimaryHex())

                CompositionLocalProvider(LocalNexusStrings provides localizedStrings) {
                    NexusPadTheme(
                        darkTheme = true,
                        colorSchemeConfig = colorScheme,
                        fontConfig = fontConfig
                    ) {
                        Surface(
                            color = Color(0xF20B0E14),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Silky 1:1 raw-screen-coordinate Drag Bar (never stutters or fights window coords)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF141B26))
                                        .pointerInteropFilter { event ->
                                            when (event.actionMasked) {
                                                MotionEvent.ACTION_DOWN -> {
                                                    kbStartRawX = event.rawX
                                                    kbStartRawY = event.rawY
                                                    kbStartWinX = params.x
                                                    kbStartWinY = params.y
                                                    val (cw, ch) = realScreenSize()
                                                    kbCurW = cw
                                                    kbCurH = ch
                                                    true
                                                }
                                                MotionEvent.ACTION_MOVE -> {
                                                    val totalDx = event.rawX - kbStartRawX
                                                    val totalDy = event.rawY - kbStartRawY
                                                    val measuredH = this@apply.height.coerceAtLeast((140 * density).roundToInt())
                                                    val newX = (kbStartWinX + totalDx).roundToInt()
                                                        .coerceIn(0, (kbCurW - params.width).coerceAtLeast(0))
                                                    val newY = (kbStartWinY + totalDy).roundToInt()
                                                        .coerceIn(0, (kbCurH - measuredH).coerceAtLeast(0))
                                                    if (newX != params.x || newY != params.y) {
                                                        params.x = newX
                                                        params.y = newY
                                                        runCatching { windowManager.updateViewLayout(this@apply, params) }
                                                    }
                                                    true
                                                }
                                                else -> true
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = localizedStrings.menuDragKbHint,
                                            tint = accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = localizedStrings.menuDragKbHint,
                                            color = Color(0xFFD5E1F0),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { floatingKeyboardOpen.value = false },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = localizedStrings.commonClose,
                                            tint = Color(0xFFD5E1F0),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                AndroidView(
                                    factory = { ctx ->
                                        NexusVirtualKeyboardView(
                                            context = ctx,
                                            initialLanguage = initialKbLang,
                                            isFloatingMode = true,
                                            onCommitText = { InputBridge.dispatchCommitText(it) },
                                            onReplaceWord = { deleteCount, replacement ->
                                                repeat(deleteCount.coerceAtLeast(0)) {
                                                    InputBridge.dispatchBackspace()
                                                }
                                                InputBridge.dispatchCommitText(replacement)
                                            },
                                            onBackspace = { InputBridge.dispatchBackspace() },
                                            onEnter = { InputBridge.dispatchEnter() },
                                            onSpecialKey = { keyCode ->
                                                InputBridge.dispatchRawKey(keyCode, KeyEvent.ACTION_DOWN)
                                                InputBridge.dispatchRawKey(keyCode, KeyEvent.ACTION_UP)
                                            },
                                            onSwitchIme = { SystemIntegrations.showInputMethodPicker(ctx) },
                                            onHideKeyboard = { floatingKeyboardOpen.value = false },
                                            onToggleFloating = {
                                                floatingKeyboardOpen.value = false
                                                InputBridge.requestShowKeyboard()
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
        floatingKeyboardView = view
        runCatching { windowManager.addView(view, params) }
    }

    private fun dismissFloatingKeyboardOverlay() {
        floatingKeyboardView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        floatingKeyboardView = null
        floatingKeyboardParams = null
    }

    private fun ensureFloatingChromeOnTop() {
        perfHudView?.let { hud ->
            val hp = perfHudParams ?: return@let
            runCatching { windowManager.removeViewImmediate(hud) }
            runCatching { windowManager.addView(hud, hp) }
        }
        if (liveEditModeFlow.value) {
            liveEditToolbarView?.let { tb ->
                val tp = liveEditToolbarParams ?: return@let
                runCatching { windowManager.removeViewImmediate(tb) }
                runCatching { windowManager.addView(tb, tp) }
            }
            liveEditActiveSheetFlow.value?.let { activeSheet ->
                dismissLiveEditStudioSheet(clearFlow = false)
                showLiveEditStudioSheet(activeSheet)
            }
        }
        if (floatingKeyboardOpen.value) {
            floatingKeyboardView?.let { kb ->
                val kp = floatingKeyboardParams ?: return@let
                runCatching { windowManager.removeViewImmediate(kb) }
                runCatching { windowManager.addView(kb, kp) }
            }
        }
        floatingBubbleView?.let { bubble ->
            val params = bubbleParams ?: return@let
            runCatching { windowManager.removeViewImmediate(bubble) }
            runCatching { windowManager.addView(bubble, params) }
        }
        if (leftMenuOpen.value) {
            dismissLeftMenuOverlay()
            showLeftMenuOverlay()
        }
        if (filterPanelOpen.value) {
            dismissFloatingFilterPanel()
            showFloatingFilterPanel()
        }
        if (crosshairPanelOpen.value) {
            dismissFloatingCrosshairPanel()
            showFloatingCrosshairPanel()
        }
        if (layoutPickerPanelOpen.value) {
            dismissFloatingLayoutPickerPanel()
            showFloatingLayoutPickerPanel()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun syncPerfHudOverlay(config: PerfHudConfig) {
        if (!config.enabled) {
            performanceMonitor?.stop()
            dismissPerfHudOverlay()
            return
        }
        performanceMonitor?.start()
        if (perfHudView != null) return

        val density = resources.displayMetrics.density
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (14 * density).roundToInt()
            y = (14 * density).roundToInt()
            applyFullScreenOverlayFlags(this)
        }
        perfHudParams = params

        val view = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val stats by (performanceMonitor?.stats
                    ?: remember { MutableStateFlow(PerfStatsSnapshot()) }).collectAsState()
                val hudCfg by perfHudFlow.collectAsState()
                val colorScheme by colorSchemeFlow.collectAsState()
                val accent = Color(colorScheme.effectivePrimaryHex())

                var downRawX = remember { 0f }
                var downRawY = remember { 0f }
                var startWinX = remember { 0 }
                var startWinY = remember { 0 }

                NexusPadTheme(darkTheme = true, colorSchemeConfig = colorScheme) {
                    PerformanceHudPill(
                        stats = stats,
                        config = hudCfg,
                        accentColor = accent,
                        modifier = Modifier.pointerInteropFilter { event ->
                            val currentParams = perfHudParams ?: return@pointerInteropFilter false
                            val currentView = perfHudView ?: return@pointerInteropFilter false
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    downRawX = event.rawX
                                    downRawY = event.rawY
                                    startWinX = currentParams.x
                                    startWinY = currentParams.y
                                    true
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    val (screenW, screenH) = realScreenSize()
                                    val vw = currentView.width.coerceAtLeast((120 * density).roundToInt())
                                    val vh = currentView.height.coerceAtLeast((32 * density).roundToInt())
                                    currentParams.x = (startWinX + (event.rawX - downRawX).roundToInt())
                                        .coerceIn(0, (screenW - vw).coerceAtLeast(0))
                                    currentParams.y = (startWinY + (event.rawY - downRawY).roundToInt())
                                        .coerceIn(0, (screenH - vh).coerceAtLeast(0))
                                    runCatching { windowManager.updateViewLayout(currentView, currentParams) }
                                    true
                                }
                                else -> true
                            }
                        }
                    )
                }
            }
        }
        perfHudView = view
        runCatching { windowManager.addView(view, params) }
    }

    private fun dismissPerfHudOverlay() {
        perfHudView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        perfHudView = null
        perfHudParams = null
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun enterLiveEditMode() {
        val currentProfile = lastRenderedProfile ?: return
        leftMenuOpen.value = false
        filterPanelOpen.value = false
        crosshairPanelOpen.value = false
        layoutPickerPanelOpen.value = false
        buttonsVisible.value = true
        InputBridge.releaseAll()

        liveEditDraftProfileFlow.value = currentProfile
        liveEditUndoStackFlow.value = emptyList()
        liveEditSelectedIdFlow.value = currentProfile.controls.firstOrNull()?.id
        liveEditSnapGridFlow.value = currentProfile.gridSnap
        liveEditGridDivisionsFlow.value = lastRenderedSettings.gridDivisions.coerceIn(4, 32)
        liveEditActiveSheetFlow.value = null
        liveEditModeFlow.value = true
        gyroAimEngine?.updateConfig(gyroAimFlow.value, pauseForEdit = true)

        clearControlWindows()
        showLiveEditBackdrop()
        renderLiveEditControlWindows(currentProfile, lastRenderedSettings)
        showLiveEditToolbar()
        ensureFloatingChromeOnTop()
    }

    private fun exitLiveEditMode(saveChanges: Boolean) {
        val draft = liveEditDraftProfileFlow.value
        val snap = liveEditSnapGridFlow.value
        liveEditModeFlow.value = false
        liveEditDraftProfileFlow.value = null
        liveEditUndoStackFlow.value = emptyList()
        liveEditSelectedIdFlow.value = null
        dismissLiveEditStudioSheet()
        dismissLiveEditToolbar()
        dismissLiveEditBackdrop()
        gyroAimEngine?.updateConfig(gyroAimFlow.value, pauseForEdit = false)

        if (saveChanges && draft != null) {
            val saved = draft.copy(
                gridSnap = snap,
                updatedAt = System.currentTimeMillis()
            )
            lastRenderedProfile = saved
            serviceScope.launch {
                profileRepoRef?.saveProfile(saved)
                if (Firebase.auth.currentUser != null) {
                    runCatching {
                        CloudLayoutSyncRepository(applicationContext).pushLayout(saved)
                    }
                }
                val strings = AppLocalization.stringsFor(appLanguageFlow.value)
                Toast.makeText(applicationContext, strings.liveEditSavedToast, Toast.LENGTH_SHORT).show()
            }
            clearControlWindows()
            if (buttonsVisible.value) {
                renderControlWindows(saved, lastRenderedSettings)
            }
        } else {
            clearControlWindows()
            val persisted = lastRenderedProfile
            if (buttonsVisible.value && persisted != null) {
                renderControlWindows(persisted, lastRenderedSettings)
            }
        }
        ensureFloatingChromeOnTop()
    }

    private fun syncLiveEditControlWindowBounds(ctrl: VirtualControl) {
        val entry = liveEditControlEntryMap[ctrl.id] ?: return
        val (view, params) = entry
        val (screenW, screenH) = realScreenSize()
        val density = resources.displayMetrics.density
        val newW = (controlWidthDp(ctrl) * density).roundToInt()
        val newH = (controlHeightDp(ctrl) * density).roundToInt()
        params.width = newW
        params.height = newH
        params.x = (ctrl.xPercent * screenW - newW / 2f)
            .roundToInt()
            .coerceIn(0, (screenW - newW).coerceAtLeast(0))
        params.y = (ctrl.yPercent * screenH - newH / 2f)
            .roundToInt()
            .coerceIn(0, (screenH - newH).coerceAtLeast(0))
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun mutateLiveEditDraft(
        nextSelectedId: String? = liveEditSelectedIdFlow.value,
        rebuildWindows: Boolean = false,
        recordUndo: Boolean = true,
        transform: (ControllerProfile) -> ControllerProfile
    ) {
        val current = liveEditDraftProfileFlow.value ?: return
        val updated = transform(current)
        if (recordUndo) {
            liveEditUndoStackFlow.value = (liveEditUndoStackFlow.value + current).takeLast(15)
        }
        liveEditDraftProfileFlow.value = updated
        liveEditSelectedIdFlow.value = nextSelectedId

        if (rebuildWindows) {
            clearControlWindows()
            renderLiveEditControlWindows(updated, lastRenderedSettings)
            ensureFloatingChromeOnTop()
        } else {
            nextSelectedId?.let { id ->
                updated.controls.firstOrNull { it.id == id }?.let { syncLiveEditControlWindowBounds(it) }
            }
        }
    }

    private fun undoLiveEditDraft() {
        val stack = liveEditUndoStackFlow.value
        if (stack.isEmpty()) return
        val previous = stack.last()
        liveEditUndoStackFlow.value = stack.dropLast(1)
        liveEditDraftProfileFlow.value = previous
        val currentSel = liveEditSelectedIdFlow.value
        if (currentSel != null && previous.controls.none { it.id == currentSel }) {
            liveEditSelectedIdFlow.value = previous.controls.firstOrNull()?.id
        }
        clearControlWindows()
        renderLiveEditControlWindows(previous, lastRenderedSettings)
        ensureFloatingChromeOnTop()
    }

    private fun adjustSelectedLiveEditControl(sizeDeltaDp: Float = 0f, opacityDelta: Float = 0f) {
        val selectedId = liveEditSelectedIdFlow.value ?: return
        mutateLiveEditDraft(nextSelectedId = selectedId, rebuildWindows = false, recordUndo = true) { draft ->
            draft.copy(
                controls = draft.controls.map { ctrl ->
                    if (ctrl.id == selectedId) {
                        ctrl.copy(
                            sizeDp = (ctrl.sizeDp + sizeDeltaDp).coerceIn(32f, 240f),
                            opacity = (ctrl.opacity + opacityDelta).coerceIn(0.18f, 1.0f)
                        )
                    } else {
                        ctrl
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun showLiveEditToolbar() {
        dismissLiveEditToolbar()
        val (screenW, _) = realScreenSize()
        val density = resources.displayMetrics.density
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenW * 0.08f).roundToInt().coerceAtLeast((8 * density).roundToInt())
            y = (12 * density).roundToInt()
            applyFullScreenOverlayFlags(this)
        }
        liveEditToolbarParams = params

        val view = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val draft by liveEditDraftProfileFlow.collectAsState()
                val undoStack by liveEditUndoStackFlow.collectAsState()
                val selectedId by liveEditSelectedIdFlow.collectAsState()
                val snapGrid by liveEditSnapGridFlow.collectAsState()
                val appLang by appLanguageFlow.collectAsState()
                val colorScheme by colorSchemeFlow.collectAsState()
                val fontConfig by fontConfigFlow.collectAsState()
                val localizedStrings = remember(appLang) { AppLocalization.stringsFor(appLang) }
                val accent = Color(colorScheme.effectivePrimaryHex())
                val selectedControl = remember(draft, selectedId) {
                    draft?.controls?.firstOrNull { it.id == selectedId }
                }

                var downRawX = remember { 0f }
                var downRawY = remember { 0f }
                var startWinX = remember { 0 }
                var startWinY = remember { 0 }
                var draggingToolbar = remember { false }
                val touchSlop = remember { ViewConfiguration.get(this@OverlayControllerService).scaledTouchSlop }

                CompositionLocalProvider(LocalNexusStrings provides localizedStrings) {
                    NexusPadTheme(
                        darkTheme = true,
                        colorSchemeConfig = colorScheme,
                        fontConfig = fontConfig
                    ) {
                        LiveOverlayEditToolbar(
                            profileName = draft?.name ?: activeProfileName.value,
                            selectedControl = selectedControl,
                            snapToGrid = snapGrid,
                            accentColor = accent,
                            onSizeDelta = { delta -> adjustSelectedLiveEditControl(sizeDeltaDp = delta) },
                            onOpacityDelta = { delta -> adjustSelectedLiveEditControl(opacityDelta = delta) },
                            onToggleSnap = {
                                val nextSnap = !liveEditSnapGridFlow.value
                                liveEditSnapGridFlow.value = nextSnap
                                serviceScope.launch { prefsRepoRef?.setGridSnapEnabled(nextSnap) }
                            },
                            onCancel = { exitLiveEditMode(saveChanges = false) },
                            onSave = { exitLiveEditMode(saveChanges = true) },
                            canUndo = undoStack.isNotEmpty(),
                            onAddControl = { showLiveEditStudioSheet(LiveEditStudioSheet.ADD_CONTROL) },
                            onRebindControl = {
                                if (liveEditSelectedIdFlow.value != null) {
                                    showLiveEditStudioSheet(LiveEditStudioSheet.REBIND_CONTROL)
                                }
                            },
                            onCustomizeControl = {
                                if (liveEditSelectedIdFlow.value != null) {
                                    showLiveEditStudioSheet(LiveEditStudioSheet.CUSTOMIZE_CONTROL)
                                }
                            },
                            onDuplicateControl = {
                                val ctrl = selectedControl ?: return@LiveOverlayEditToolbar
                                val clone = LayoutStudioHelper.duplicateControl(ctrl)
                                mutateLiveEditDraft(
                                    nextSelectedId = clone.id,
                                    rebuildWindows = true
                                ) { current ->
                                    current.copy(controls = current.controls + clone)
                                }
                            },
                            onDeleteControl = {
                                val targetId = selectedControl?.id ?: return@LiveOverlayEditToolbar
                                mutateLiveEditDraft(
                                    nextSelectedId = null,
                                    rebuildWindows = true
                                ) { current ->
                                    current.copy(controls = current.controls.filterNot { it.id == targetId })
                                }
                            },
                            onUndo = { undoLiveEditDraft() },
                            onOpenGridSettings = { showLiveEditStudioSheet(LiveEditStudioSheet.GRID_SETTINGS) },
                            modifier = Modifier.pointerInteropFilter { event ->
                                val tp = liveEditToolbarParams ?: return@pointerInteropFilter false
                                val tv = liveEditToolbarView ?: return@pointerInteropFilter false
                                when (event.actionMasked) {
                                    MotionEvent.ACTION_DOWN -> {
                                        downRawX = event.rawX
                                        downRawY = event.rawY
                                        startWinX = tp.x
                                        startWinY = tp.y
                                        draggingToolbar = false
                                        false
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        val dx = event.rawX - downRawX
                                        val dy = event.rawY - downRawY
                                        if (!draggingToolbar && kotlin.math.hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                                            draggingToolbar = true
                                        }
                                        if (draggingToolbar) {
                                            val (sw, sh) = realScreenSize()
                                            val vw = tv.width.coerceAtLeast((240 * density).roundToInt())
                                            val vh = tv.height.coerceAtLeast((56 * density).roundToInt())
                                            tp.x = (startWinX + dx.roundToInt()).coerceIn(0, (sw - vw).coerceAtLeast(0))
                                            tp.y = (startWinY + dy.roundToInt()).coerceIn(0, (sh - vh).coerceAtLeast(0))
                                            runCatching { windowManager.updateViewLayout(tv, tp) }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    else -> {
                                        val wasDrag = draggingToolbar
                                        draggingToolbar = false
                                        wasDrag
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        liveEditToolbarView = view
        runCatching { windowManager.addView(view, params) }
    }

    private fun showLiveEditStudioSheet(sheet: LiveEditStudioSheet) {
        dismissLiveEditStudioSheet(clearFlow = false)
        liveEditActiveSheetFlow.value = sheet

        // Focusable full-screen modal overlay so search/label TextFields can receive keyboard input
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            applyFullScreenOverlayFlags(this)
        }

        val view = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val draft by liveEditDraftProfileFlow.collectAsState()
                val selectedId by liveEditSelectedIdFlow.collectAsState()
                val snapGrid by liveEditSnapGridFlow.collectAsState()
                val divisions by liveEditGridDivisionsFlow.collectAsState()
                val activeSheet by liveEditActiveSheetFlow.collectAsState()
                val appLang by appLanguageFlow.collectAsState()
                val colorScheme by colorSchemeFlow.collectAsState()
                val fontConfig by fontConfigFlow.collectAsState()
                val localizedStrings = remember(appLang) { AppLocalization.stringsFor(appLang) }
                val accent = Color(colorScheme.effectivePrimaryHex())
                val selectedCtrl = remember(draft, selectedId) {
                    draft?.controls?.firstOrNull { it.id == selectedId }
                }

                CompositionLocalProvider(LocalNexusStrings provides localizedStrings) {
                    NexusPadTheme(
                        darkTheme = true,
                        colorSchemeConfig = colorScheme,
                        fontConfig = fontConfig
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.62f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { dismissLiveEditStudioSheet() },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFF131622),
                                shadowElevation = 16.dp,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                                    .widthIn(max = 480.dp)
                                    .heightIn(max = 520.dp)
                                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {}
                            ) {
                                when (activeSheet) {
                                    LiveEditStudioSheet.ADD_CONTROL -> {
                                        CommandPickerSheetContent(
                                            title = "Add Virtual Control",
                                            includeClusters = true,
                                            onPick = { binding ->
                                                val newCtrl = LayoutStudioHelper.createControlFromBinding(binding)
                                                mutateLiveEditDraft(
                                                    nextSelectedId = newCtrl.id,
                                                    rebuildWindows = true
                                                ) { current ->
                                                    current.copy(controls = current.controls + newCtrl)
                                                }
                                                dismissLiveEditStudioSheet()
                                            },
                                            onClose = { dismissLiveEditStudioSheet() }
                                        )
                                    }

                                    LiveEditStudioSheet.REBIND_CONTROL -> {
                                        if (selectedCtrl != null) {
                                            val isCluster = selectedCtrl.controlType == ControlType.ANALOG_STICK ||
                                                selectedCtrl.controlType == ControlType.DPAD_CLUSTER ||
                                                selectedCtrl.controlType == ControlType.WASD_CLUSTER ||
                                                selectedCtrl.controlType == ControlType.TOUCHPAD
                                            CommandPickerSheetContent(
                                                title = "Rebind '${selectedCtrl.label}'",
                                                includeClusters = isCluster,
                                                onPick = { binding ->
                                                    val rebound = LayoutStudioHelper.rebindControl(selectedCtrl, binding)
                                                    val typeChanged = rebound.controlType != selectedCtrl.controlType
                                                    mutateLiveEditDraft(
                                                        nextSelectedId = rebound.id,
                                                        rebuildWindows = typeChanged
                                                    ) { current ->
                                                        current.copy(
                                                            controls = current.controls.map { c ->
                                                                if (c.id == rebound.id) rebound else c
                                                            }
                                                        )
                                                    }
                                                    dismissLiveEditStudioSheet()
                                                },
                                                onClose = { dismissLiveEditStudioSheet() }
                                            )
                                        }
                                    }

                                    LiveEditStudioSheet.CUSTOMIZE_CONTROL -> {
                                        if (selectedCtrl != null) {
                                            ControlInspectorSheetContent(
                                                control = selectedCtrl,
                                                onChange = { updated ->
                                                    mutateLiveEditDraft(
                                                        nextSelectedId = updated.id,
                                                        rebuildWindows = false
                                                    ) { current ->
                                                        current.copy(
                                                            controls = current.controls.map { c ->
                                                                if (c.id == updated.id) updated else c
                                                            }
                                                        )
                                                    }
                                                },
                                                onPickCustomImage = null,
                                                onNudge = { dx, dy ->
                                                    val nudged = LayoutStudioHelper.nudgeControl(selectedCtrl, dx, dy)
                                                    mutateLiveEditDraft(
                                                        nextSelectedId = nudged.id,
                                                        rebuildWindows = false
                                                    ) { current ->
                                                        current.copy(
                                                            controls = current.controls.map { c ->
                                                                if (c.id == nudged.id) nudged else c
                                                            }
                                                        )
                                                    }
                                                },
                                                onMirror = {
                                                    val mirrored = LayoutStudioHelper.mirrorControl(selectedCtrl)
                                                    mutateLiveEditDraft(
                                                        nextSelectedId = mirrored.id,
                                                        rebuildWindows = false
                                                    ) { current ->
                                                        current.copy(
                                                            controls = current.controls.map { c ->
                                                                if (c.id == mirrored.id) mirrored else c
                                                            }
                                                        )
                                                    }
                                                },
                                                onClose = { dismissLiveEditStudioSheet() }
                                            )
                                        }
                                    }

                                    LiveEditStudioSheet.GRID_SETTINGS -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Grid & Snapping",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(onClick = { dismissLiveEditStudioSheet() }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                                }
                                            }
                                            GridSettingsContent(
                                                gridSnapEnabled = snapGrid,
                                                gridDivisions = divisions,
                                                onGridSnapChange = { enabled ->
                                                    liveEditSnapGridFlow.value = enabled
                                                    serviceScope.launch { prefsRepoRef?.setGridSnapEnabled(enabled) }
                                                },
                                                onGridDivisionsChange = { nextDiv ->
                                                    val clamped = nextDiv.coerceIn(4, 32)
                                                    liveEditGridDivisionsFlow.value = clamped
                                                    serviceScope.launch { prefsRepoRef?.setGridDivisions(clamped) }
                                                }
                                            )
                                        }
                                    }

                                    null -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
        liveEditStudioSheetView = view
        runCatching { windowManager.addView(view, params) }
    }

    private fun dismissLiveEditStudioSheet(clearFlow: Boolean = true) {
        liveEditStudioSheetView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        liveEditStudioSheetView = null
        if (clearFlow) {
            liveEditActiveSheetFlow.value = null
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun showLiveEditBackdrop() {
        dismissLiveEditBackdrop()
        // Full-screen touch-blocking window (FLAG_NOT_FOCUSABLE only — NO FLAG_NOT_TOUCHABLE
        // and NO FLAG_NOT_TOUCH_MODAL) placed below live-edit controls & toolbar so zero
        // touches outside the user's layout controls can ever leak to the game underneath.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            applyFullScreenOverlayFlags(this)
        }

        val view = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val snapGrid by liveEditSnapGridFlow.collectAsState()
                val divisions by liveEditGridDivisionsFlow.collectAsState()
                val safeDivisions = divisions.coerceIn(4, 32)
                val colorScheme by colorSchemeFlow.collectAsState()
                val accent = Color(colorScheme.effectivePrimaryHex())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF06070F).copy(alpha = 0.52f))
                        .pointerInteropFilter { true }
                ) {
                    if (snapGrid) {
                        val rows = (safeDivisions * 0.66f).roundToInt().coerceIn(4, 24)
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val lineColor = Color.White.copy(alpha = 0.11f)
                            val centerLineColor = accent.copy(alpha = 0.28f)
                            val strokePx = 1.dp.toPx()
                            for (c in 1 until safeDivisions) {
                                val x = size.width * (c.toFloat() / safeDivisions.toFloat())
                                val isCenter = safeDivisions % 2 == 0 && c == safeDivisions / 2
                                drawLine(
                                    if (isCenter) centerLineColor else lineColor,
                                    androidx.compose.ui.geometry.Offset(x, 0f),
                                    androidx.compose.ui.geometry.Offset(x, size.height),
                                    strokeWidth = if (isCenter) strokePx * 1.4f else strokePx
                                )
                            }
                            for (r in 1 until rows) {
                                val y = size.height * (r.toFloat() / rows.toFloat())
                                val isCenter = rows % 2 == 0 && r == rows / 2
                                drawLine(
                                    if (isCenter) centerLineColor else lineColor,
                                    androidx.compose.ui.geometry.Offset(0f, y),
                                    androidx.compose.ui.geometry.Offset(size.width, y),
                                    strokeWidth = if (isCenter) strokePx * 1.4f else strokePx
                                )
                            }
                        }
                    }
                }
            }
        }
        liveEditBackdropView = view
        runCatching { windowManager.addView(view, params) }
    }

    private fun dismissLiveEditBackdrop() {
        liveEditBackdropView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        liveEditBackdropView = null
    }

    private fun dismissLiveEditToolbar() {
        liveEditToolbarView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        liveEditToolbarView = null
        liveEditToolbarParams = null
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun renderLiveEditControlWindows(profile: ControllerProfile, settings: OverlaySettings) {
        val (screenW, screenH) = realScreenSize()
        val density = resources.displayMetrics.density

        profile.controls.forEach { initialControl ->
            val widthDp = controlWidthDp(initialControl)
            val heightDp = controlHeightDp(initialControl)
            val wPx = (widthDp * density).roundToInt()
            val hPx = (heightDp * density).roundToInt()

            val xPx = (initialControl.xPercent * screenW - wPx / 2f)
                .roundToInt()
                .coerceIn(0, (screenW - wPx).coerceAtLeast(0))
            val yPx = (initialControl.yPercent * screenH - hPx / 2f)
                .roundToInt()
                .coerceIn(0, (screenH - hPx).coerceAtLeast(0))

            // In Live Edit mode ALL controls (even pass-through touchpads) are touchable & draggable
            val params = WindowManager.LayoutParams(
                wPx,
                hPx,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = xPx
                y = yPx
                applyFullScreenOverlayFlags(this)
            }

            val controlId = initialControl.id
            val view = ComposeView(this).apply {
                attachLifecycle(this)
                setContent {
                    val draft by liveEditDraftProfileFlow.collectAsState()
                    val selectedId by liveEditSelectedIdFlow.collectAsState()
                    val liveCtrl = remember(draft, controlId) {
                        draft?.controls?.firstOrNull { it.id == controlId } ?: initialControl
                    }
                    val isSelected = selectedId == controlId

                    var downRawX = remember { 0f }
                    var downRawY = remember { 0f }
                    var startWinX = remember { 0 }
                    var startWinY = remember { 0 }

                    NexusPadTheme {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInteropFilter { event ->
                                    val entry = liveEditControlEntryMap[controlId]
                                        ?: return@pointerInteropFilter false
                                    val (ctrlView, ctrlParams) = entry
                                    when (event.actionMasked) {
                                        MotionEvent.ACTION_DOWN -> {
                                            liveEditSelectedIdFlow.value = controlId
                                            downRawX = event.rawX
                                            downRawY = event.rawY
                                            startWinX = ctrlParams.x
                                            startWinY = ctrlParams.y
                                            true
                                        }
                                        MotionEvent.ACTION_MOVE -> {
                                            val (sw, sh) = realScreenSize()
                                            val rawNewX = (startWinX + (event.rawX - downRawX).roundToInt())
                                                .coerceIn(0, (sw - ctrlParams.width).coerceAtLeast(0))
                                            val rawNewY = (startWinY + (event.rawY - downRawY).roundToInt())
                                                .coerceIn(0, (sh - ctrlParams.height).coerceAtLeast(0))

                                            val rawXPercent = ((rawNewX + ctrlParams.width / 2f) / sw.coerceAtLeast(1))
                                                .coerceIn(0.03f, 0.97f)
                                            val rawYPercent = ((rawNewY + ctrlParams.height / 2f) / sh.coerceAtLeast(1))
                                                .coerceIn(0.04f, 0.96f)

                                            val snapStep = 1f / liveEditGridDivisionsFlow.value.coerceIn(4, 32)
                                            val finalXPercent = if (liveEditSnapGridFlow.value) {
                                                (kotlin.math.round(rawXPercent / snapStep) * snapStep).coerceIn(0.03f, 0.97f)
                                            } else {
                                                rawXPercent
                                            }
                                            val finalYPercent = if (liveEditSnapGridFlow.value) {
                                                (kotlin.math.round(rawYPercent / snapStep) * snapStep).coerceIn(0.04f, 0.96f)
                                            } else {
                                                rawYPercent
                                            }

                                            ctrlParams.x = (finalXPercent * sw - ctrlParams.width / 2f)
                                                .roundToInt()
                                                .coerceIn(0, (sw - ctrlParams.width).coerceAtLeast(0))
                                            ctrlParams.y = (finalYPercent * sh - ctrlParams.height / 2f)
                                                .roundToInt()
                                                .coerceIn(0, (sh - ctrlParams.height).coerceAtLeast(0))
                                            runCatching { windowManager.updateViewLayout(ctrlView, ctrlParams) }

                                            val currentDraft = liveEditDraftProfileFlow.value
                                            if (currentDraft != null) {
                                                liveEditDraftProfileFlow.value = currentDraft.copy(
                                                    controls = currentDraft.controls.map { c ->
                                                        if (c.id == controlId) {
                                                            c.copy(xPercent = finalXPercent, yPercent = finalYPercent)
                                                        } else {
                                                            c
                                                        }
                                                    }
                                                )
                                            }
                                            true
                                        }
                                        else -> true
                                    }
                                }
                        ) {
                            VirtualControlCanvasItem(
                                control = liveCtrl,
                                notation = settings.controllerNotation,
                                globalOpacity = settings.globalOpacity,
                                vibrationMode = settings.vibrationMode,
                                isEditMode = true,
                                isSelected = isSelected,
                                isPressed = false,
                                isToggled = false,
                                onSelect = { liveEditSelectedIdFlow.value = controlId },
                                onDragDelta = { _, _ -> }
                            )
                        }
                    }
                }
            }
            controlViews += view
            controlViewParams += params
            liveEditControlEntryMap[controlId] = view to params
            runCatching { windowManager.addView(view, params) }
        }
    }

    private fun renderControlWindows(profile: ControllerProfile, settings: OverlaySettings) {
        val (screenW, screenH) = realScreenSize()
        val density = resources.displayMetrics.density

        profile.controls.forEach { control ->
            val widthDp = controlWidthDp(control)
            val heightDp = controlHeightDp(control)
            val wPx = (widthDp * density).roundToInt()
            val hPx = (heightDp * density).roundToInt()

            val xPx = (control.xPercent * screenW - wPx / 2f)
                .roundToInt()
                .coerceIn(0, (screenW - wPx).coerceAtLeast(0))
            val yPx = (control.yPercent * screenH - hPx / 2f)
                .roundToInt()
                .coerceIn(0, (screenH - hPx).coerceAtLeast(0))

            val isPassThroughPad = control.binding.axisPair == AxisPair.TOUCHPAD_DIRECT_PASS
            val windowFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                if (isPassThroughPad) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0

            val params = WindowManager.LayoutParams(
                wPx,
                hPx,
                overlayWindowType(),
                windowFlags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = xPx
                y = yPx
                applyFullScreenOverlayFlags(this)
            }

            val view = ComposeView(this).apply {
                attachLifecycle(this)
                setContent {
                    val isControlPressed by remember(control.id) {
                        InputBridge.pressedControlIds.map { control.id in it }.distinctUntilChanged()
                    }.collectAsState(initial = false)
                    val isControlToggled by remember(control.id) {
                        InputBridge.toggledControlIds.map { control.id in it }.distinctUntilChanged()
                    }.collectAsState(initial = false)
                    NexusPadTheme {
                        VirtualControlCanvasItem(
                            control = control,
                            notation = settings.controllerNotation,
                            globalOpacity = settings.globalOpacity,
                            vibrationMode = settings.vibrationMode,
                            isEditMode = false,
                            isSelected = false,
                            isPressed = isControlPressed,
                            isToggled = isControlToggled,
                            onSelect = {},
                            onDragDelta = { _, _ -> }
                        )
                    }
                }
            }
            controlViews += view
            controlViewParams += params
            runCatching { windowManager.addView(view, params) }
        }
    }

    private fun controlWidthDp(control: VirtualControl): Float = when (control.shape) {
        ControlShape.PILL -> control.sizeDp * 1.45f
        ControlShape.PADDLE -> control.sizeDp * 1.35f
        ControlShape.KEYCAP -> control.sizeDp * 1.08f
        ControlShape.TOUCHPAD_RECT -> control.sizeDp * 1.45f
        else -> control.sizeDp
    }

    private fun controlHeightDp(control: VirtualControl): Float = when (control.shape) {
        ControlShape.PILL -> control.sizeDp * 0.78f
        ControlShape.PADDLE -> control.sizeDp * 0.84f
        ControlShape.KEYCAP -> control.sizeDp * 0.94f
        ControlShape.TOUCHPAD_RECT -> control.sizeDp
        else -> control.sizeDp
    }

    private fun clearControlWindows() {
        controlViews.forEach { view ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        controlViews.clear()
        controlViewParams.clear()
        liveEditControlEntryMap.clear()
    }

    private fun attachLifecycle(view: ComposeView) {
        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeViewModelStoreOwner(lifecycleOwner)
        view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
    }

    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_channel_description)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayControllerService::class.java).setAction(ACTION_STOP_OVERLAY),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("XenosPad Floating Bubble Active")
            .setContentText("Tap the floating bubble anytime to open the Left HUD Menu.")
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "Stop Overlay", stopIntent)
            .build()
    }

    override fun onDestroy() {
        collectorJob?.cancel()
        gyroAimEngine?.stop()
        gyroAimEngine = null
        performanceMonitor?.stop()
        performanceMonitor = null
        InputBridge.attachFloatKeyboardHandler(null)
        dismissLiveEditStudioSheet()
        dismissLiveEditToolbar()
        dismissLiveEditBackdrop()
        dismissPerfHudOverlay()
        dismissFloatingKeyboardOverlay()
        dismissFloatingLayoutPickerPanel()
        dismissFloatingCrosshairPanel()
        dismissFloatingFilterPanel()
        dismissLeftMenuOverlay()
        dismissCaptureBanner()
        clearControlWindows()
        removeCrosshairOverlay()
        removeScreenFilterOverlay()
        removeGpuFilterOverlay()
        if (GameFilterBridge.isGpuShaderActive.value) {
            ScreenCaptureService.stopGpuFilter(this)
        }
        floatingBubbleView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        floatingBubbleView = null
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycleOwner.viewModelStore.clear()
        InputBridge.releaseAll()
        InputBridge.setOverlayRunning(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    private class OverlayLifecycleOwner :
        LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        fun performRestore(savedState: Bundle?) {
            savedStateController.performRestore(savedState)
        }

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }
}

@Composable
private fun LeftDockedPanelWrapper(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(start = 10.dp, top = 12.dp, bottom = 12.dp, end = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxPanelHeight = (maxHeight * 0.88f).coerceAtLeast(280.dp)
        Box(
            modifier = Modifier
                .widthIn(max = 316.dp)
                .heightIn(max = maxPanelHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .verticalScroll(rememberScrollState())
        ) {
            content()
        }
    }
}

private data class DockActionItem(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
private fun LeftSideOverlayMenu(
    profileName: String,
    buttonsVisible: Boolean,
    bubbleLocked: Boolean,
    canSwitchProfile: Boolean,
    activeFilterSlot: Int,
    crosshairEnabled: Boolean,
    gyroEnabled: Boolean,
    hasHardwareGyro: Boolean,
    perfHudEnabled: Boolean,
    targetMode: TargetPlatformMode,
    isReplayOn: Boolean,
    replayDurationSec: Int,
    isRecording: Boolean,
    recordingElapsedMs: Long,
    onDismiss: () -> Unit,
    onToggleButtons: () -> Unit,
    onOpenLayouts: () -> Unit,
    onStartLiveEdit: () -> Unit,
    onToggleGyro: () -> Unit,
    onTogglePerfHud: () -> Unit,
    onToggleTargetMode: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenCrosshair: () -> Unit,
    onTakeScreenshot: () -> Unit,
    onToggleReplay: () -> Unit,
    onSaveReplay: () -> Unit,
    onToggleRecord: () -> Unit,
    onOpenKeyboard: () -> Unit,
    onOpenFloatingKeyboard: () -> Unit,
    onReleaseAll: () -> Unit,
    onToggleLock: () -> Unit,
    onStopOverlay: () -> Unit,
    onOpenApp: () -> Unit
) {
    val strings = LocalNexusStrings.current
    val slideX = remember { Animatable(-28f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            slideX.animateTo(0f, tween(durationMillis = 170, easing = FastOutSlowInEasing))
        }
        launch {
            alphaAnim.animateTo(1f, tween(durationMillis = 150, easing = FastOutSlowInEasing))
        }
    }

    val themePrimary = MaterialTheme.colorScheme.primary
    val themeSurface = MaterialTheme.colorScheme.surface
    val themeOutline = MaterialTheme.colorScheme.outline

    val actions = listOf(
        DockActionItem(
            label = strings.menuLayouts,
            icon = Icons.Default.Layers,
            tint = if (canSwitchProfile) themePrimary else Color(0xFF607087),
            enabled = canSwitchProfile,
            onClick = onOpenLayouts
        ),
        DockActionItem(
            label = strings.liveEditTile,
            icon = Icons.Default.Edit,
            tint = if (canSwitchProfile) themePrimary else Color(0xFF607087),
            enabled = canSwitchProfile,
            onClick = onStartLiveEdit
        ),
        DockActionItem(
            label = if (buttonsVisible) strings.menuHidePad else strings.menuShowPad,
            icon = if (buttonsVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            tint = TacticalCyan,
            onClick = onToggleButtons
        ),
        DockActionItem(
            label = strings.gyroHudTile,
            icon = Icons.Default.ScreenRotation,
            tint = when {
                !hasHardwareGyro -> Color(0xFF607087)
                gyroEnabled -> themePrimary
                else -> TacticalCyan
            },
            enabled = hasHardwareGyro,
            onClick = onToggleGyro
        ),
        DockActionItem(
            label = strings.perfHudTile,
            icon = Icons.Default.Speed,
            tint = if (perfHudEnabled) themePrimary else TacticalCyan,
            onClick = onTogglePerfHud
        ),
        DockActionItem(
            label = "${strings.menuTargetMode}: ${targetMode.shortLabel}",
            icon = Icons.Default.Gamepad,
            tint = if (targetMode == TargetPlatformMode.ANDROID_NATIVE) TacticalCyan else themePrimary,
            onClick = onToggleTargetMode
        ),
        DockActionItem(
            label = if (activeFilterSlot in 1..3) "${strings.menuFilter} S$activeFilterSlot" else strings.menuFilter,
            icon = Icons.Default.Tune,
            tint = if (activeFilterSlot in 1..3) themePrimary else TacticalCyan,
            onClick = onOpenFilters
        ),
        DockActionItem(
            label = strings.menuCrosshair,
            icon = Icons.Default.CenterFocusStrong,
            tint = if (crosshairEnabled) themePrimary else TacticalCyan,
            onClick = onOpenCrosshair
        ),
        DockActionItem(
            label = strings.menuScreenshot,
            icon = Icons.Default.CameraAlt,
            tint = TacticalCyan,
            onClick = onTakeScreenshot
        ),
        DockActionItem(
            label = if (isReplayOn) "${strings.galleryReplayOn} (${ReplayBufferPreset.formatDuration(replayDurationSec)})" else strings.menuReplay,
            icon = Icons.Default.Replay,
            tint = if (isReplayOn) themePrimary else TacticalCyan,
            onClick = onToggleReplay
        ),
        DockActionItem(
            label = strings.menuSaveReplay,
            icon = Icons.Default.Save,
            tint = if (isReplayOn) themePrimary else Color(0xFF607087),
            enabled = isReplayOn,
            onClick = onSaveReplay
        ),
        DockActionItem(
            label = if (isRecording) CaptureBridge.formatDurationMs(recordingElapsedMs) else strings.menuRecord,
            icon = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
            tint = MacroCoral,
            onClick = onToggleRecord
        ),
        DockActionItem(
            label = strings.menuKeyboard,
            icon = Icons.Default.Keyboard,
            tint = TacticalCyan,
            onClick = onOpenKeyboard
        ),
        DockActionItem(
            label = strings.menuFloatKb,
            icon = Icons.Default.Keyboard,
            tint = themePrimary,
            onClick = onOpenFloatingKeyboard
        ),
        DockActionItem(
            label = strings.menuUnstick,
            icon = Icons.Default.RestartAlt,
            tint = TurboAmber,
            onClick = onReleaseAll
        ),
        DockActionItem(
            label = if (bubbleLocked) strings.menuUnlock else strings.menuLock,
            icon = if (bubbleLocked) Icons.Default.LockOpen else Icons.Default.Lock,
            tint = TurboAmber,
            onClick = onToggleLock
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x5204060A))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(start = 10.dp, top = 12.dp, bottom = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxH = (maxHeight * 0.90f).coerceAtLeast(260.dp)
        Surface(
            color = themeSurface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .width(248.dp)
                .heightIn(max = maxH)
                .offset { IntOffset(slideX.value.roundToInt(), 0) }
                .alpha(alphaAnim.value)
                .border(1.2.dp, themeOutline.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "XENOSPAD HUD",
                            color = themePrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = profileName,
                            color = Color.White,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Menu", tint = Color(0xFFA2B3C9))
                    }
                }

                // 2-column action grid docked neatly on left side of screen
                actions.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        pair.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF151C28))
                                    .border(
                                        1.dp,
                                        item.tint.copy(alpha = if (item.enabled) 0.45f else 0.16f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(enabled = item.enabled, onClick = item.onClick)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = item.tint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = item.label,
                                    color = if (item.enabled) Color.White else Color(0xFF6B7B91),
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF151C28))
                            .border(1.dp, TacticalCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenApp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = TacticalCyan, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = strings.menuOpenApp,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF28121A))
                            .border(1.dp, MacroCoral.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onStopOverlay)
                            .padding(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = strings.menuStop, tint = MacroCoral, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = strings.menuStop,
                            color = MacroCoral,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

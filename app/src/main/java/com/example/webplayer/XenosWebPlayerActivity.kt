package com.example.webplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.NexusDatabase
import com.example.data.OverlaySettings
import com.example.data.PreferencesRepository
import com.example.data.ProfileRepository
import com.example.filter.GameFilterBridge
import com.example.input.GyroAimEngine
import com.example.input.InputBridge
import com.example.input.PhysicalControllerInfo
import com.example.input.PhysicalControllerMonitor
import com.example.model.AppLocalization
import com.example.model.ControlType
import com.example.model.ControllerProfile
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import com.example.model.GfnRegionPreset
import com.example.model.LocalNexusStrings
import com.example.model.PhysicalControllerConfig
import com.example.model.StickerPreset
import com.example.model.WebRtcStreamStats
import com.example.model.WebStreamQualityConfig
import com.example.service.PerformanceMonitor
import com.example.ui.components.CommandPickerSheetContent
import com.example.ui.components.ControlInspectorSheetContent
import com.example.ui.components.CrosshairOverlayCanvas
import com.example.ui.components.EditProfileMetaDialog
import com.example.ui.components.GeForceFilterPanelContent
import com.example.ui.components.GridSettingsContent
import com.example.ui.components.LayoutStudioHelper
import com.example.ui.components.PositionedVirtualControl
import com.example.ui.components.StickerOverlayCanvas
import com.example.ui.theme.NexusPadTheme
import com.example.ui.theme.TacticalCyan
import com.example.util.ButtonImageCodec
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class VirtualMouseCursorState(
    val xPx: Float = 0f,
    val yPx: Float = 0f,
    val visible: Boolean = false,
    val leftPressed: Boolean = false,
    val rightPressed: Boolean = false,
    val initialized: Boolean = false,
    val lastActivityMs: Long = 0L
)

/**
 * Dedicated Better xCloud-style GeForce NOW Web Player Activity.
 *
 * Hosts https://play.geforcenow.com in a hardware-accelerated landscape WebView with:
 * - Direct JavaScript Virtual Xbox 360 Gamepad + KB/Mouse/Wheel injection via InputBridge.WebInputSink
 *   (zero IME, Accessibility, or Draw-Over-Other-Apps permissions required)
 * - Built-in Better xCloud-style Region Bypass (X-Forwarded-For + Accept-Language + optional Intl timezone/locale)
 * - Zero-permission GPU CSS Freestyle filters & Stretch-to-Fill video mode
 * - Real RTCPeerConnection.getStats() telemetry pill + device battery/temperature
 * - Full XenosPad virtual control overlay, Crosshair HUD, Gyroscope Aiming, and Live On-Game Layout Edit
 * - In-app OAuth popup window support for NVIDIA / Steam / Epic / Microsoft sign-in
 */
class XenosWebPlayerActivity : ComponentActivity() {

    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var profileRepo: ProfileRepository
    private lateinit var gyroAimEngine: GyroAimEngine
    private lateinit var performanceMonitor: PerformanceMonitor

    private var mainWebView: WebView? = null
    private var initialLaunchUrl: String? = null
    private var latestWebConfig: WebStreamQualityConfig = WebStreamQualityConfig()
    private var latestSettings: OverlaySettings = OverlaySettings()

    private val _streamStats = MutableStateFlow(WebRtcStreamStats())
    private val streamStats = _streamStats.asStateFlow()

    private val _pageProgress = MutableStateFlow(0)
    private val pageProgress = _pageProgress.asStateFlow()

    private val _mouseCursorState = MutableStateFlow(VirtualMouseCursorState())
    private val mouseCursorState = _mouseCursorState.asStateFlow()

    private val _inGameStream = MutableStateFlow(false)
    private val inGameStream = _inGameStream.asStateFlow()

    private var mouseButtonMask: Int = 0
    private var mouseDownTimeMs: Long = 0L
    private var mouseHoverEntered: Boolean = false
    private val buttonDownUptimeMs = LongArray(3)
    private val pendingButtonRelease = arrayOfNulls<Runnable>(3)
    private val setActionButtonMethod by lazy {
        runCatching {
            MotionEvent::class.java.getMethod("setActionButton", Int::class.javaPrimitiveType)
        }.getOrNull()
    }

    private var popupWebViewState by mutableStateOf<WebView?>(null)

    private val webInputSink = object : InputBridge.WebInputSink {
        override fun onGamepadButton(androidKeyCode: Int, isDown: Boolean) {
            val idx = BetterGfnScriptEngine.mapKeyCodeToGamepadButtonIndex(androidKeyCode)
            if (idx != null) {
                evalJs(BetterGfnScriptEngine.buildGamepadButtonJs(idx, isDown))
            } else {
                evalJs(BetterGfnScriptEngine.buildKeyboardJs(androidKeyCode, 0, isDown))
            }
        }

        override fun onKeyboardKey(androidKeyCode: Int, metaState: Int, isDown: Boolean) {
            evalJs(BetterGfnScriptEngine.buildKeyboardJs(androidKeyCode, metaState, isDown))
        }

        override fun onMouseButton(button: Int, isDown: Boolean) {
            dispatchNativeMouseButton(button, isDown)
        }

        override fun onMouseScroll(scrollUp: Boolean) {
            dispatchNativeMouseScroll(scrollUp)
        }

        override fun onMouseMove(dxPx: Float, dyPx: Float) {
            dispatchNativeMouseMove(dxPx, dyPx)
        }

        override fun onAnalogStick(isLeft: Boolean, x: Float, y: Float) {
            evalJs(BetterGfnScriptEngine.buildAnalogStickJs(isLeft, x, y))
        }

        override fun onResetAll() {
            resetNativeMouseState()
            evalJs(BetterGfnScriptEngine.buildResetAllInputsJs())
        }
    }

    private inner class XenosWebJsBridge {
        @JavascriptInterface
        fun onWebRtcStats(rawJson: String) {
            val parsed = BetterGfnScriptEngine.parseWebRtcStatsJson(rawJson)
            _streamStats.value = parsed
            if (parsed.connected && (parsed.fps > 0 || parsed.resolution.isNotBlank())) {
                if (!_inGameStream.value) {
                    _inGameStream.value = true
                    _mouseCursorState.value = _mouseCursorState.value.copy(visible = false)
                }
            }
        }

        @JavascriptInterface
        fun onStreamStateChanged(active: Boolean) {
            _inGameStream.value = active
            if (active) {
                mouseHoverEntered = false
                _mouseCursorState.value = _mouseCursorState.value.copy(visible = false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersiveFullscreen()

        initialLaunchUrl = intent?.getStringExtra(EXTRA_INITIAL_URL)?.takeIf {
            it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
        }

        prefsRepo = PreferencesRepository(applicationContext)
        profileRepo = ProfileRepository(NexusDatabase.getInstance(applicationContext).profileDao())
        gyroAimEngine = GyroAimEngine(applicationContext)
        performanceMonitor = PerformanceMonitor(applicationContext)
        PhysicalControllerMonitor.ensureStarted(applicationContext)

        lifecycleScope.launch {
            profileRepo.ensureSeeded()
        }

        InputBridge.attachWebInputSink(webInputSink)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val popup = popupWebViewState
                    if (popup != null) {
                        closePopupWebView()
                        return
                    }
                    val web = mainWebView
                    if (web != null && web.canGoBack()) {
                        web.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )

        setContent {
            val settings by prefsRepo.settingsFlow.collectAsStateWithLifecycle(initialValue = OverlaySettings())
            val liveFilterState by GameFilterBridge.liveFilterState.collectAsStateWithLifecycle()
            val activeProfile by profileRepo.activeProfile.collectAsStateWithLifecycle(initialValue = null)
            val allProfiles by profileRepo.profiles.collectAsStateWithLifecycle(initialValue = emptyList())
            val liveStreamStats by streamStats.collectAsStateWithLifecycle()
            val perfStats by performanceMonitor.stats.collectAsStateWithLifecycle()
            val progress by pageProgress.collectAsStateWithLifecycle()
            val mouseCursor by mouseCursorState.collectAsStateWithLifecycle()
            val isInGameStream by inGameStream.collectAsStateWithLifecycle()
            val pressedIds by InputBridge.pressedControlIds.collectAsStateWithLifecycle()
            val toggledIds by InputBridge.toggledControlIds.collectAsStateWithLifecycle()
            val connectedControllers by PhysicalControllerMonitor.connectedControllers.collectAsStateWithLifecycle()

            LaunchedEffect(settings.gameFilter) {
                GameFilterBridge.syncFromPersisted(settings.gameFilter)
            }

            val effectiveSettings = remember(settings, liveFilterState) {
                settings.copy(gameFilter = liveFilterState)
            }

            val localizedStrings = remember(settings.appLanguage) {
                AppLocalization.stringsFor(settings.appLanguage)
            }

            LaunchedEffect(effectiveSettings) {
                latestSettings = effectiveSettings
                latestWebConfig = effectiveSettings.webPlayerConfig
                gyroAimEngine.updateConfig(effectiveSettings.gyroAim)
                if (effectiveSettings.webPlayerConfig.showWebRtcStatsHud) {
                    performanceMonitor.start()
                } else {
                    performanceMonitor.stop()
                }
                applyLiveStyleAndBootstrap(effectiveSettings)
            }

            CompositionLocalProvider(LocalNexusStrings provides localizedStrings) {
                NexusPadTheme(
                    darkTheme = effectiveSettings.darkTheme,
                    colorSchemeConfig = effectiveSettings.colorScheme,
                    fontConfig = effectiveSettings.fontConfig
                ) {
                    WebPlayerScreenContent(
                        settings = effectiveSettings,
                        activeProfile = activeProfile,
                        allProfiles = allProfiles,
                        connectedControllers = connectedControllers,
                        streamStats = liveStreamStats,
                        perfStats = perfStats,
                        loadProgress = progress,
                        mouseCursor = mouseCursor,
                        inGameStream = isInGameStream,
                        onHideMouseCursor = {
                            _mouseCursorState.value = _mouseCursorState.value.copy(visible = false)
                        },
                        pressedIds = pressedIds,
                        toggledIds = toggledIds,
                        popupWebView = popupWebViewState,
                        onClosePopup = { closePopupWebView() },
                        onBindMainWebView = { webView ->
                            mainWebView = webView
                            loadGeForceNow(webView, effectiveSettings.webPlayerConfig, initialLaunchUrl)
                        },
                        onSelectProfile = { id ->
                            lifecycleScope.launch { profileRepo.setActiveProfile(id) }
                        },
                        onSaveEditedProfile = { updated ->
                            lifecycleScope.launch {
                                profileRepo.saveProfile(updated)
                                Toast.makeText(
                                    this@XenosWebPlayerActivity,
                                    localizedStrings.liveEditSavedToast,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onGridSnapChange = { enabled ->
                            lifecycleScope.launch { prefsRepo.setGridSnapEnabled(enabled) }
                        },
                        onGridDivisionsChange = { divisions ->
                            lifecycleScope.launch { prefsRepo.setGridDivisions(divisions) }
                        },
                        onSelectRegion = { region ->
                            val next = effectiveSettings.webPlayerConfig.copy(regionPreset = region)
                            latestWebConfig = next
                            lifecycleScope.launch { prefsRepo.updateWebPlayerConfig(next) }
                            mainWebView?.let { loadGeForceNow(it, next) }
                        },
                        onSelectFilterSlot = { slot ->
                            GameFilterBridge.selectLiveSlot(slot)
                            val updatedSettings = effectiveSettings.copy(
                                gameFilter = liveFilterState.copy(activeSlot = slot)
                            )
                            latestSettings = updatedSettings
                            applyLiveStyleAndBootstrap(updatedSettings)
                            lifecycleScope.launch { prefsRepo.setActiveFilterSlot(slot) }
                        },
                        onPreviewFilterStyle = { previewStyle ->
                            GameFilterBridge.updateLiveStyle(previewStyle)
                            val nextFilter = liveFilterState.withUpdatedStyle(previewStyle)
                                .copy(activeSlot = previewStyle.slot.coerceIn(1, 3))
                            val updatedSettings = effectiveSettings.copy(gameFilter = nextFilter)
                            latestSettings = updatedSettings
                            applyLiveStyleAndBootstrap(updatedSettings)
                        },
                        onUpdateFilterStyle = { updatedStyle ->
                            GameFilterBridge.updateLiveStyle(updatedStyle)
                            val nextFilter = liveFilterState.withUpdatedStyle(updatedStyle)
                                .copy(activeSlot = updatedStyle.slot.coerceIn(1, 3))
                            val updatedSettings = effectiveSettings.copy(gameFilter = nextFilter)
                            latestSettings = updatedSettings
                            applyLiveStyleAndBootstrap(updatedSettings)
                            lifecycleScope.launch { prefsRepo.updateFilterStyle(updatedStyle) }
                        },
                        onResetFilterSlot = {
                            val current = GameFilterBridge.liveFilterState.value
                            val slot = current.activeSlot
                            if (slot in 1..3) {
                                val resetState = current.withResetSlot(slot)
                                GameFilterBridge.syncFromPersisted(resetState)
                                val updatedSettings = effectiveSettings.copy(gameFilter = resetState)
                                latestSettings = updatedSettings
                                applyLiveStyleAndBootstrap(updatedSettings)
                                lifecycleScope.launch { prefsRepo.resetFilterSlot(slot) }
                            }
                        },
                        onPickStickerImage = { uri ->
                            lifecycleScope.launch {
                                ButtonImageCodec.encodeUriToBase64(this@XenosWebPlayerActivity, uri, cropSquare = false)
                                    .onSuccess { encoded ->
                                        val currentState = GameFilterBridge.liveFilterState.value
                                        val slot = if (currentState.activeSlot in 1..3) currentState.activeSlot else 1
                                        val updated = currentState.styleForSlot(slot).copy(
                                            stickersEnabled = true,
                                            stickerPreset = StickerPreset.CUSTOM_IMAGE,
                                            customStickerBase64 = encoded
                                        )
                                        GameFilterBridge.updateLiveStyle(updated)
                                        val nextFilter = currentState.withUpdatedStyle(updated).copy(activeSlot = slot)
                                        val updatedSettings = effectiveSettings.copy(gameFilter = nextFilter)
                                        latestSettings = updatedSettings
                                        applyLiveStyleAndBootstrap(updatedSettings)
                                        prefsRepo.updateFilterStyle(updated)
                                    }
                                    .onFailure { err ->
                                        Toast.makeText(
                                            this@XenosWebPlayerActivity,
                                            err.localizedMessage ?: "Unable to load custom sticker",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        },
                        onToggleStretch = {
                            val next = settings.webPlayerConfig.copy(
                                stretchVideoToFill = !settings.webPlayerConfig.stretchVideoToFill
                            )
                            lifecycleScope.launch { prefsRepo.updateWebPlayerConfig(next) }
                        },
                        onToggleCrosshair = {
                            val next = settings.crosshair.copy(enabled = !settings.crosshair.enabled)
                            lifecycleScope.launch { prefsRepo.updateCrosshair(next) }
                        },
                        onToggleGyro = {
                            val next = settings.gyroAim.copy(enabled = !settings.gyroAim.enabled)
                            lifecycleScope.launch { prefsRepo.updateGyroAim(next) }
                        },
                        onToggleStatsHud = {
                            val next = settings.webPlayerConfig.copy(
                                showWebRtcStatsHud = !settings.webPlayerConfig.showWebRtcStatsHud
                            )
                            lifecycleScope.launch { prefsRepo.updateWebPlayerConfig(next) }
                        },
                        onToggleAntiAfk = {
                            val next = settings.webPlayerConfig.copy(
                                antiAfkEnabled = !settings.webPlayerConfig.antiAfkEnabled
                            )
                            lifecycleScope.launch { prefsRepo.updateWebPlayerConfig(next) }
                            Toast.makeText(
                                this@XenosWebPlayerActivity,
                                if (next.antiAfkEnabled) "Anti-AFK / Queue Keeper ON" else "Anti-AFK OFF",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onToggleAutoHideTouch = {
                            val next = settings.physicalController.copy(
                                autoHideTouchControls = !settings.physicalController.autoHideTouchControls
                            )
                            lifecycleScope.launch { prefsRepo.updatePhysicalController(next) }
                        },
                        onBrowserBack = {
                            mainWebView?.let { if (it.canGoBack()) it.goBack() }
                        },
                        onBrowserReload = {
                            mainWebView?.let { loadGeForceNow(it, latestWebConfig, initialLaunchUrl) }
                        },
                        onResetInputs = {
                            InputBridge.releaseAll()
                        },
                        onExit = { finish() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveFullscreen()
        PhysicalControllerMonitor.scanNow()
        InputBridge.attachWebInputSink(webInputSink)
        mainWebView?.onResume()
        if (latestSettings.gyroAim.enabled) gyroAimEngine.start()
        if (latestWebConfig.showWebRtcStatsHud) performanceMonitor.start()
    }

    override fun onPause() {
        super.onPause()
        InputBridge.releaseAll()
        gyroAimEngine.stop()
        performanceMonitor.stop()
        mainWebView?.onPause()
        runCatching { CookieManager.getInstance().flush() }
    }

    override fun onDestroy() {
        InputBridge.releaseAll()
        InputBridge.attachWebInputSink(null)
        gyroAimEngine.stop()
        performanceMonitor.stop()
        closePopupWebView()
        mainWebView?.let { web ->
            web.removeJavascriptInterface("XenosWebBridge")
            web.stopLoading()
            web.destroy()
        }
        mainWebView = null
        super.onDestroy()
    }

    private fun applyImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun evalJs(script: String) {
        val web = mainWebView ?: return
        runOnUiThread {
            if (mainWebView === web) {
                web.evaluateJavascript(script, null)
            }
        }
    }

    private fun obtainMouseMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        xPx: Float,
        yPx: Float,
        buttonState: Int,
        actionButton: Int = 0,
        vScroll: Float = 0f
    ): MotionEvent {
        val props = arrayOf(
            MotionEvent.PointerProperties().apply {
                id = 0
                toolType = MotionEvent.TOOL_TYPE_MOUSE
            }
        )
        val coords = arrayOf(
            MotionEvent.PointerCoords().apply {
                x = xPx
                y = yPx
                pressure = if (buttonState != 0) 1f else 0f
                size = 1f
                if (vScroll != 0f) {
                    setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll)
                }
            }
        )
        val ev = MotionEvent.obtain(
            downTime,
            eventTime,
            action,
            1,
            props,
            coords,
            0,
            buttonState,
            1f,
            1f,
            0,
            0,
            InputDevice.SOURCE_MOUSE,
            0
        )
        if (actionButton != 0) {
            runCatching { setActionButtonMethod?.invoke(ev, actionButton) }
        }
        return ev
    }

    private fun ensureMouseCursorCoords(web: WebView): Pair<Float, Float> {
        val w = web.width.coerceAtLeast(1).toFloat()
        val h = web.height.coerceAtLeast(1).toFloat()
        val cur = _mouseCursorState.value
        return if (!cur.initialized) {
            (w / 2f) to (h / 2f)
        } else {
            cur.xPx.coerceIn(1f, (w - 1f).coerceAtLeast(1f)) to
                cur.yPx.coerceIn(1f, (h - 1f).coerceAtLeast(1f))
        }
    }

    private fun dispatchNativeMouseMove(dxPx: Float, dyPx: Float) {
        val web = mainWebView ?: return
        runOnUiThread {
            if (mainWebView !== web) return@runOnUiThread
            val density = resources.displayMetrics.density.coerceAtLeast(1f)
            if (_inGameStream.value) {
                // Inside an active GeForce NOW game stream: NEVER show the app's duplicate cursor
                // and NEVER spam clamped Android hover events. Stream pure relative deltas directly
                // into GeForce NOW's locked pointer engine so both the in-game GFN cursor (menus)
                // and 360-degree 3D camera turning (gameplay) work natively.
                if (_mouseCursorState.value.visible) {
                    _mouseCursorState.value = _mouseCursorState.value.copy(visible = false)
                }
                val streamScaleDivisor = density.coerceAtMost(1.85f)
                web.evaluateJavascript(
                    BetterGfnScriptEngine.buildGameMouseMoveJs(
                        dxCss = dxPx / streamScaleDivisor,
                        dyCss = dyPx / streamScaleDivisor
                    ),
                    null
                )
                return@runOnUiThread
            }

            val w = web.width.coerceAtLeast(1).toFloat()
            val h = web.height.coerceAtLeast(1).toFloat()
            val (curX, curY) = ensureMouseCursorCoords(web)
            val rawX = curX + dxPx
            val rawY = curY + dyPx
            val maxX = (w - 1f).coerceAtLeast(1f)
            val maxY = (h - 1f).coerceAtLeast(1f)
            val nextX = rawX.coerceIn(1f, maxX)
            val nextY = rawY.coerceIn(1f, maxY)
            val atEdge = rawX < 1f || rawX > maxX || rawY < 1f || rawY > maxY
            val now = SystemClock.uptimeMillis()

            _mouseCursorState.value = _mouseCursorState.value.copy(
                xPx = nextX,
                yPx = nextY,
                visible = true,
                initialized = true,
                lastActivityMs = now
            )

            if (mouseButtonMask != 0) {
                val moveEv = obtainMouseMotionEvent(
                    downTime = mouseDownTimeMs,
                    eventTime = now,
                    action = MotionEvent.ACTION_MOVE,
                    xPx = nextX,
                    yPx = nextY,
                    buttonState = mouseButtonMask
                )
                try {
                    web.dispatchTouchEvent(moveEv)
                } finally {
                    moveEv.recycle()
                }
            } else {
                if (!mouseHoverEntered) {
                    mouseHoverEntered = true
                    val enterEv = obtainMouseMotionEvent(
                        downTime = now,
                        eventTime = now,
                        action = MotionEvent.ACTION_HOVER_ENTER,
                        xPx = nextX,
                        yPx = nextY,
                        buttonState = 0
                    )
                    try {
                        web.dispatchGenericMotionEvent(enterEv)
                    } finally {
                        enterEv.recycle()
                    }
                }
                val hoverEv = obtainMouseMotionEvent(
                    downTime = now,
                    eventTime = now,
                    action = MotionEvent.ACTION_HOVER_MOVE,
                    xPx = nextX,
                    yPx = nextY,
                    buttonState = 0
                )
                try {
                    web.dispatchGenericMotionEvent(hoverEv)
                } finally {
                    hoverEv.recycle()
                }
            }

            web.evaluateJavascript(
                BetterGfnScriptEngine.buildSyncMouseMoveJs(
                    cssX = nextX / density,
                    cssY = nextY / density,
                    dxCss = dxPx / density,
                    dyCss = dyPx / density,
                    forceRelative = atEdge || mouseButtonMask != 0
                ),
                null
            )
        }
    }

    private fun dispatchNativeMouseButton(button: Int, isDown: Boolean) {
        val web = mainWebView ?: return
        val clampedBtn = button.coerceIn(0, 2)
        runOnUiThread {
            if (mainWebView !== web) return@runOnUiThread
            if (!isDown) {
                val elapsed = SystemClock.uptimeMillis() - buttonDownUptimeMs[clampedBtn]
                if (elapsed in 0L until MIN_MOUSE_CLICK_HOLD_MS) {
                    pendingButtonRelease[clampedBtn]?.let { web.removeCallbacks(it) }
                    val delayMs = (MIN_MOUSE_CLICK_HOLD_MS - elapsed).coerceAtLeast(16L)
                    val releaseTask = Runnable {
                        pendingButtonRelease[clampedBtn] = null
                        if (mainWebView === web) {
                            performMouseButtonEventNow(web, clampedBtn, isDown = false)
                        }
                    }
                    pendingButtonRelease[clampedBtn] = releaseTask
                    web.postDelayed(releaseTask, delayMs)
                    return@runOnUiThread
                }
            } else {
                pendingButtonRelease[clampedBtn]?.let { web.removeCallbacks(it) }
                pendingButtonRelease[clampedBtn] = null
            }
            performMouseButtonEventNow(web, clampedBtn, isDown)
        }
    }

    private fun performMouseButtonEventNow(web: WebView, clampedBtn: Int, isDown: Boolean) {
        val (curX, curY) = ensureMouseCursorCoords(web)
        val bit = when (clampedBtn) {
            0 -> MotionEvent.BUTTON_PRIMARY
            1 -> MotionEvent.BUTTON_TERTIARY
            2 -> MotionEvent.BUTTON_SECONDARY
            else -> MotionEvent.BUTTON_PRIMARY
        }
        val prevMask = mouseButtonMask
        mouseButtonMask = if (isDown) (mouseButtonMask or bit) else (mouseButtonMask and bit.inv())
        val now = SystemClock.uptimeMillis()
        if (isDown) {
            buttonDownUptimeMs[clampedBtn] = now
        }

        if (_inGameStream.value) {
            // Inside active GeForce NOW game stream: keep app cursor hidden and dispatch exact
            // LMB / MMB / RMB events directly to GeForce NOW's locked pointer target (avoids
            // Android WebView converting secondary mouse clicks into left clicks or context menus).
            if (isDown) {
                web.requestFocus()
            }
            _mouseCursorState.value = _mouseCursorState.value.copy(
                visible = false,
                leftPressed = (mouseButtonMask and MotionEvent.BUTTON_PRIMARY) != 0,
                rightPressed = (mouseButtonMask and MotionEvent.BUTTON_SECONDARY) != 0,
                lastActivityMs = now
            )
            web.evaluateJavascript(
                BetterGfnScriptEngine.buildGameMouseButtonJs(clampedBtn, isDown),
                null
            )
            return
        }

        _mouseCursorState.value = _mouseCursorState.value.copy(
            xPx = curX,
            yPx = curY,
            visible = true,
            initialized = true,
            leftPressed = (mouseButtonMask and MotionEvent.BUTTON_PRIMARY) != 0,
            rightPressed = (mouseButtonMask and MotionEvent.BUTTON_SECONDARY) != 0,
            lastActivityMs = now
        )

        if (isDown) {
            web.requestFocus()
            if (mouseHoverEntered) {
                mouseHoverEntered = false
                val exitEv = obtainMouseMotionEvent(
                    downTime = now,
                    eventTime = now,
                    action = MotionEvent.ACTION_HOVER_EXIT,
                    xPx = curX,
                    yPx = curY,
                    buttonState = 0
                )
                try {
                    web.dispatchGenericMotionEvent(exitEv)
                } finally {
                    exitEv.recycle()
                }
            }
            if (prevMask == 0) {
                mouseDownTimeMs = now
                val downEv = obtainMouseMotionEvent(
                    downTime = mouseDownTimeMs,
                    eventTime = now,
                    action = MotionEvent.ACTION_DOWN,
                    xPx = curX,
                    yPx = curY,
                    buttonState = mouseButtonMask,
                    actionButton = bit
                )
                try {
                    web.dispatchTouchEvent(downEv)
                } finally {
                    downEv.recycle()
                }
            } else {
                val chordEv = obtainMouseMotionEvent(
                    downTime = mouseDownTimeMs,
                    eventTime = now,
                    action = MotionEvent.ACTION_MOVE,
                    xPx = curX,
                    yPx = curY,
                    buttonState = mouseButtonMask,
                    actionButton = bit
                )
                try {
                    web.dispatchTouchEvent(chordEv)
                } finally {
                    chordEv.recycle()
                }
            }
            val pressEv = obtainMouseMotionEvent(
                downTime = mouseDownTimeMs,
                eventTime = now,
                action = MotionEvent.ACTION_BUTTON_PRESS,
                xPx = curX,
                yPx = curY,
                buttonState = mouseButtonMask,
                actionButton = bit
            )
            try {
                web.dispatchGenericMotionEvent(pressEv)
            } finally {
                pressEv.recycle()
            }
        } else {
            val releaseEv = obtainMouseMotionEvent(
                downTime = mouseDownTimeMs,
                eventTime = now,
                action = MotionEvent.ACTION_BUTTON_RELEASE,
                xPx = curX,
                yPx = curY,
                buttonState = mouseButtonMask,
                actionButton = bit
            )
            try {
                web.dispatchGenericMotionEvent(releaseEv)
            } finally {
                releaseEv.recycle()
            }
            if (mouseButtonMask == 0) {
                val upEv = obtainMouseMotionEvent(
                    downTime = mouseDownTimeMs,
                    eventTime = now,
                    action = MotionEvent.ACTION_UP,
                    xPx = curX,
                    yPx = curY,
                    buttonState = 0,
                    actionButton = bit
                )
                try {
                    web.dispatchTouchEvent(upEv)
                } finally {
                    upEv.recycle()
                }
                mouseHoverEntered = true
                val enterEv = obtainMouseMotionEvent(
                    downTime = now,
                    eventTime = now,
                    action = MotionEvent.ACTION_HOVER_ENTER,
                    xPx = curX,
                    yPx = curY,
                    buttonState = 0
                )
                try {
                    web.dispatchGenericMotionEvent(enterEv)
                } finally {
                    enterEv.recycle()
                }
            } else {
                val chordUpEv = obtainMouseMotionEvent(
                    downTime = mouseDownTimeMs,
                    eventTime = now,
                    action = MotionEvent.ACTION_MOVE,
                    xPx = curX,
                    yPx = curY,
                    buttonState = mouseButtonMask,
                    actionButton = bit
                )
                try {
                    web.dispatchTouchEvent(chordUpEv)
                } finally {
                    chordUpEv.recycle()
                }
            }
        }

        // Guarantee GeForce NOW receives pointer/mouse down+up+click/contextmenu even if Android
        // WebView drops ACTION_BUTTON_PRESS on newer APIs (deduplicates automatically against isTrusted!).
        val density = resources.displayMetrics.density.coerceAtLeast(1f)
        web.evaluateJavascript(
            BetterGfnScriptEngine.buildEnsureMouseButtonJs(
                button = clampedBtn,
                isDown = isDown,
                cssX = curX / density,
                cssY = curY / density
            ),
            null
        )
    }

    private fun dispatchNativeMouseScroll(scrollUp: Boolean) {
        val web = mainWebView ?: return
        runOnUiThread {
            if (mainWebView !== web) return@runOnUiThread
            if (_inGameStream.value) {
                if (_mouseCursorState.value.visible) {
                    _mouseCursorState.value = _mouseCursorState.value.copy(visible = false)
                }
                web.evaluateJavascript(
                    BetterGfnScriptEngine.buildGameMouseWheelJs(scrollUp),
                    null
                )
                return@runOnUiThread
            }
            val (curX, curY) = ensureMouseCursorCoords(web)
            val now = SystemClock.uptimeMillis()
            _mouseCursorState.value = _mouseCursorState.value.copy(
                xPx = curX,
                yPx = curY,
                visible = true,
                initialized = true,
                lastActivityMs = now
            )
            val scrollEv = obtainMouseMotionEvent(
                downTime = now,
                eventTime = now,
                action = MotionEvent.ACTION_SCROLL,
                xPx = curX,
                yPx = curY,
                buttonState = mouseButtonMask,
                vScroll = if (scrollUp) 1.5f else -1.5f
            )
            try {
                web.dispatchGenericMotionEvent(scrollEv)
            } finally {
                scrollEv.recycle()
            }
        }
    }

    private fun resetNativeMouseState() {
        val web = mainWebView
        runOnUiThread {
            for (i in pendingButtonRelease.indices) {
                pendingButtonRelease[i]?.let { web?.removeCallbacks(it) }
                pendingButtonRelease[i] = null
            }
            if (mouseButtonMask != 0 && web != null && mainWebView === web) {
                val (curX, curY) = ensureMouseCursorCoords(web)
                val now = SystemClock.uptimeMillis()
                val upEv = obtainMouseMotionEvent(
                    downTime = mouseDownTimeMs,
                    eventTime = now,
                    action = MotionEvent.ACTION_UP,
                    xPx = curX,
                    yPx = curY,
                    buttonState = 0
                )
                try {
                    web.dispatchTouchEvent(upEv)
                } finally {
                    upEv.recycle()
                }
            }
            mouseButtonMask = 0
            mouseHoverEntered = false
            _mouseCursorState.value = _mouseCursorState.value.copy(
                leftPressed = false,
                rightPressed = false
            )
        }
    }

    private var lastAppliedBootstrapKey: Pair<WebStreamQualityConfig, PhysicalControllerConfig>? = null
    private var lastAppliedVideoStyleKey: Triple<GameFilterStyle?, Boolean, Boolean>? = null

    private fun applyLiveStyleAndBootstrap(settings: OverlaySettings) {
        val web = mainWebView ?: return
        runOnUiThread {
            if (mainWebView !== web) return@runOnUiThread
            val desiredUa = settings.webPlayerConfig.userAgentMode.userAgentString
            if (web.settings.userAgentString != desiredUa) {
                web.settings.userAgentString = desiredUa
            }
            val bootstrapKey = settings.webPlayerConfig to settings.physicalController
            if (bootstrapKey != lastAppliedBootstrapKey) {
                lastAppliedBootstrapKey = bootstrapKey
                web.evaluateJavascript(
                    BetterGfnScriptEngine.buildBootstrapScript(
                        config = settings.webPlayerConfig,
                        physicalConfig = settings.physicalController
                    ),
                    null
                )
            }
            val styleKey = Triple(
                settings.gameFilter.activeStyle,
                settings.webPlayerConfig.stretchVideoToFill,
                settings.webPlayerConfig.enableCssFilters
            )
            if (styleKey != lastAppliedVideoStyleKey) {
                lastAppliedVideoStyleKey = styleKey
                web.evaluateJavascript(
                    BetterGfnScriptEngine.buildVideoStyleScript(
                        style = settings.gameFilter.activeStyle,
                        stretchToFill = settings.webPlayerConfig.stretchVideoToFill,
                        enableCssFilters = settings.webPlayerConfig.enableCssFilters
                    ),
                    null
                )
            }
        }
    }

    private fun loadGeForceNow(
        webView: WebView,
        config: WebStreamQualityConfig,
        overrideUrl: String? = null
    ) {
        webView.settings.userAgentString = config.userAgentMode.userAgentString
        val headers = BetterGfnScriptEngine.buildNavigationHeaders(config)
        val targetUrl = overrideUrl?.takeIf {
            it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
        } ?: BetterGfnScriptEngine.GFN_WEB_URL
        if (headers.isEmpty()) {
            webView.loadUrl(targetUrl)
        } else {
            webView.loadUrl(targetUrl, headers)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    internal fun createConfiguredMainWebView(context: Context): WebView {
        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
            isLongClickable = false
            setOnLongClickListener { true }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                userAgentString = latestWebConfig.userAgentMode.userAgentString
            }
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.addJavascriptInterface(XenosWebJsBridge(), "XenosWebBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    return true
                }
                if (BetterGfnScriptEngine.isTrustedGfnOrigin(url) && latestWebConfig.regionPreset.isEnabled) {
                    val headers = BetterGfnScriptEngine.buildNavigationHeaders(latestWebConfig)
                    view?.loadUrl(url, headers)
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                _inGameStream.value = false
                view?.evaluateJavascript(
                    BetterGfnScriptEngine.buildBootstrapScript(
                        config = latestWebConfig,
                        physicalConfig = latestSettings.physicalController
                    ),
                    null
                )
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(
                    BetterGfnScriptEngine.buildBootstrapScript(
                        config = latestWebConfig,
                        physicalConfig = latestSettings.physicalController
                    ),
                    null
                )
                view?.evaluateJavascript(
                    BetterGfnScriptEngine.buildVideoStyleScript(
                        style = latestSettings.gameFilter.activeStyle,
                        stretchToFill = latestWebConfig.stretchVideoToFill,
                        enableCssFilters = latestWebConfig.enableCssFilters
                    ),
                    null
                )
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                _pageProgress.value = newProgress
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                val req = request ?: return
                val origin = req.origin?.toString()
                val allowed = req.resources.filter { res ->
                    res == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID ||
                        (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE &&
                            BetterGfnScriptEngine.isTrustedGfnOrigin(origin))
                }.toTypedArray()
                runOnUiThread {
                    if (allowed.isNotEmpty()) {
                        req.grant(allowed)
                    } else {
                        req.deny()
                    }
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                val popup = createAuthPopupWebView(context)
                popupWebViewState = popup
                transport.webView = popup
                resultMsg.sendToTarget()
                return true
            }
        }

        return webView
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createAuthPopupWebView(context: Context): WebView {
        val popup = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                userAgentString = latestWebConfig.userAgentMode.userAgentString
            }
        }
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(popup, true)

        popup.webViewClient = WebViewClient()
        popup.webChromeClient = object : WebChromeClient() {
            override fun onCloseWindow(window: WebView?) {
                runOnUiThread { closePopupWebView() }
            }
        }
        return popup
    }

    private fun closePopupWebView() {
        val popup = popupWebViewState
        popupWebViewState = null
        popup?.let {
            it.stopLoading()
            it.destroy()
        }
        runCatching { CookieManager.getInstance().flush() }
    }

    companion object {
        private const val MIN_MOUSE_CLICK_HOLD_MS = 50L
        const val EXTRA_INITIAL_URL = "extra_initial_url"

        fun launch(context: Context, initialUrl: String? = null) {
            val intent = Intent(context, XenosWebPlayerActivity::class.java).apply {
                if (!initialUrl.isNullOrBlank()) {
                    putExtra(EXTRA_INITIAL_URL, initialUrl)
                }
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebPlayerScreenContent(
    settings: OverlaySettings,
    activeProfile: ControllerProfile?,
    allProfiles: List<ControllerProfile>,
    connectedControllers: List<PhysicalControllerInfo>,
    streamStats: WebRtcStreamStats,
    perfStats: com.example.service.PerfStatsSnapshot,
    loadProgress: Int,
    mouseCursor: VirtualMouseCursorState,
    inGameStream: Boolean,
    onHideMouseCursor: () -> Unit,
    pressedIds: Set<String>,
    toggledIds: Set<String>,
    popupWebView: WebView?,
    onClosePopup: () -> Unit,
    onBindMainWebView: (WebView) -> Unit,
    onSelectProfile: (String) -> Unit,
    onSaveEditedProfile: (ControllerProfile) -> Unit,
    onGridSnapChange: (Boolean) -> Unit,
    onGridDivisionsChange: (Int) -> Unit,
    onSelectRegion: (GfnRegionPreset) -> Unit,
    onSelectFilterSlot: (Int) -> Unit,
    onPreviewFilterStyle: (GameFilterStyle) -> Unit,
    onUpdateFilterStyle: (GameFilterStyle) -> Unit,
    onResetFilterSlot: () -> Unit,
    onPickStickerImage: (Uri) -> Unit,
    onToggleStretch: () -> Unit,
    onToggleCrosshair: () -> Unit,
    onToggleGyro: () -> Unit,
    onToggleStatsHud: () -> Unit,
    onToggleAntiAfk: () -> Unit,
    onToggleAutoHideTouch: () -> Unit,
    onBrowserBack: () -> Unit,
    onBrowserReload: () -> Unit,
    onResetInputs: () -> Unit,
    onExit: () -> Unit
) {
    val strings = LocalNexusStrings.current
    val accent = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var controlsVisible by remember { mutableStateOf(settings.webPlayerConfig.showControlsByDefault) }
    var manualShowControlsOverride by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showFullFilterStudio by remember { mutableStateOf(false) }
    var isLiveEditMode by remember { mutableStateOf(false) }
    var liveEditDraft by remember { mutableStateOf<ControllerProfile?>(null) }
    var liveEditUndoStack by remember { mutableStateOf<List<ControllerProfile>>(emptyList()) }
    var selectedEditControlId by remember { mutableStateOf<String?>(null) }
    var snapToGrid by remember(settings.gridSnapEnabled) { mutableStateOf(settings.gridSnapEnabled) }
    var gridDivisions by remember(settings.gridDivisions) { mutableIntStateOf(settings.gridDivisions.coerceIn(4, 32)) }
    var hideLiveStudioTools by remember { mutableStateOf(false) }
    var showLiveAddSheet by remember { mutableStateOf(false) }
    var showLiveRebindSheet by remember { mutableStateOf(false) }
    var showLiveInspectorSheet by remember { mutableStateOf(false) }
    var showLiveGridDialog by remember { mutableStateOf(false) }
    var showLiveMetaDialog by remember { mutableStateOf(false) }
    var bubbleOffsetX by remember { mutableFloatStateOf(24f) }
    var bubbleOffsetY by remember { mutableFloatStateOf(16f) }

    val stickerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickStickerImage(uri)
        }
    }

    LaunchedEffect(connectedControllers.isEmpty()) {
        if (connectedControllers.isEmpty()) {
            manualShowControlsOverride = false
        }
    }

    val touchAutoHidden = PhysicalControllerMonitor.shouldAutoHideTouchControls(
        connectedCount = connectedControllers.size,
        autoHideEnabled = settings.physicalController.autoHideTouchControls,
        manualOverrideShow = manualShowControlsOverride
    )
    val effectiveControlsVisible = controlsVisible && !touchAutoHidden
    val primaryControllerName = connectedControllers.firstOrNull()?.name

    val displayedProfile = if (isLiveEditMode) liveEditDraft ?: activeProfile else activeProfile

    fun mutateLiveDraft(
        nextSelectedId: String? = selectedEditControlId,
        transform: (ControllerProfile) -> ControllerProfile
    ) {
        val current = liveEditDraft ?: return
        val updated = transform(current)
        liveEditUndoStack = (liveEditUndoStack + current).takeLast(15)
        liveEditDraft = updated
        selectedEditControlId = nextSelectedId
    }

    fun exitLiveEdit() {
        isLiveEditMode = false
        liveEditDraft = null
        liveEditUndoStack = emptyList()
        selectedEditControlId = null
        hideLiveStudioTools = false
        showLiveAddSheet = false
        showLiveRebindSheet = false
        showLiveInspectorSheet = false
        showLiveGridDialog = false
        showLiveMetaDialog = false
    }

    BackHandler(enabled = isLiveEditMode) {
        when {
            showLiveAddSheet -> showLiveAddSheet = false
            showLiveRebindSheet -> showLiveRebindSheet = false
            showLiveInspectorSheet -> showLiveInspectorSheet = false
            showLiveGridDialog -> showLiveGridDialog = false
            showLiveMetaDialog -> showLiveMetaDialog = false
            else -> exitLiveEdit()
        }
    }

    BackHandler(enabled = showFullFilterStudio && !isLiveEditMode) {
        showFullFilterStudio = false
    }

    BackHandler(enabled = menuExpanded && !isLiveEditMode && !showFullFilterStudio) {
        menuExpanded = false
    }

    LaunchedEffect(mouseCursor.lastActivityMs, mouseCursor.visible, mouseCursor.leftPressed, mouseCursor.rightPressed) {
        if (mouseCursor.visible && !mouseCursor.leftPressed && !mouseCursor.rightPressed) {
            delay(6000L)
            onHideMouseCursor()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val canvasWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val canvasHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        // 1. Hardware-accelerated GeForce NOW WebView
        AndroidView(
            factory = { ctx ->
                val activity = ctx as XenosWebPlayerActivity
                activity.createConfiguredMainWebView(ctx).also(onBindMainWebView)
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("web_player_webview")
        )

        // Page loading indicator
        if (loadProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                color = accent,
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
            )
        }

        // 1b. Non-interactive Virtual Mouse Cursor overlay (ONLY outside active game streams,
        // auto-hides after 6s idle — inside games GeForce NOW renders & owns the cursor directly)
        if (mouseCursor.visible && !isLiveEditMode && !inGameStream) {
            VirtualMouseCursorOverlay(
                cursor = mouseCursor,
                accentColor = accent,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 1c. Freestyle Stickers & Frame Overlay (Tactical HUD, Cinema REC, Neon Corners, CRT Bezel, Custom PNG)
        val activeFilterStyle = settings.gameFilter.activeStyle
        if (settings.webPlayerConfig.enableCssFilters && activeFilterStyle != null && activeFilterStyle.stickersEnabled && !isLiveEditMode) {
            StickerOverlayCanvas(
                style = activeFilterStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Crosshair HUD (non-interactive overlay)
        if (settings.crosshair.enabled && !isLiveEditMode) {
            CrosshairOverlayCanvas(
                settings = settings.crosshair,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2b. Touch-Blocking Live Edit Canvas Scrim + Grid
        // While editing layout live in-game, consumes 100% of background touches so nothing
        // behind the user's controls or edit studio tools can ever tap the GeForce NOW screen.
        if (isLiveEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF06070F).copy(alpha = 0.56f))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            var allReleased = false
                            while (!allReleased) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                                if (event.changes.none { it.pressed }) {
                                    allReleased = true
                                }
                            }
                            selectedEditControlId = null
                        }
                    }
                    .testTag("web_player_live_edit_scrim")
            ) {
                if (snapToGrid && !hideLiveStudioTools) {
                    val cols = gridDivisions.coerceIn(4, 32)
                    val rows = (cols * 0.66f).roundToInt().coerceIn(4, 24)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 1.dp.toPx()
                        for (c in 1 until cols) {
                            val isCenter = c == cols / 2
                            val x = size.width * (c.toFloat() / cols.toFloat())
                            drawLine(
                                color = Color.White.copy(alpha = if (isCenter) 0.22f else 0.10f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = if (isCenter) 1.5.dp.toPx() else strokePx
                            )
                        }
                        for (r in 1 until rows) {
                            val isCenter = r == rows / 2
                            val y = size.height * (r.toFloat() / rows.toFloat())
                            drawLine(
                                color = Color.White.copy(alpha = if (isCenter) 0.22f else 0.10f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = if (isCenter) 1.5.dp.toPx() else strokePx
                            )
                        }
                    }
                }
            }
        }

        // 3. XenosPad Virtual Controls layer (direct WebInputSink routing in play mode,
        //    auto-hides when a physical controller is connected unless manually overridden)
        if ((effectiveControlsVisible || isLiveEditMode) && displayedProfile != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                displayedProfile.controls.forEach { control ->
                    PositionedVirtualControl(
                        control = control,
                        containerWidthPx = canvasWidthPx,
                        containerHeightPx = canvasHeightPx,
                        notation = settings.controllerNotation,
                        globalOpacity = settings.globalOpacity,
                        vibrationMode = settings.vibrationMode,
                        isEditMode = isLiveEditMode,
                        isSelected = isLiveEditMode && control.id == selectedEditControlId,
                        isPressed = !isLiveEditMode && control.id in pressedIds,
                        isToggled = !isLiveEditMode && control.id in toggledIds,
                        onSelect = {
                            if (isLiveEditMode) {
                                selectedEditControlId = control.id
                            }
                        },
                        onCommitPercent = { rawX, rawY ->
                            if (isLiveEditMode) {
                                val divs = gridDivisions.coerceIn(4, 32).toFloat()
                                val aspectY = (divs * 0.66f).roundToInt().coerceIn(4, 24).toFloat()
                                val clampedX = rawX.coerceIn(0.02f, 0.98f)
                                val clampedY = rawY.coerceIn(0.03f, 0.97f)
                                val finalX = if (snapToGrid) {
                                    ((clampedX * divs).roundToInt() / divs).coerceIn(0.02f, 0.98f)
                                } else {
                                    clampedX
                                }
                                val finalY = if (snapToGrid) {
                                    ((clampedY * aspectY).roundToInt() / aspectY).coerceIn(0.03f, 0.97f)
                                } else {
                                    clampedY
                                }
                                mutateLiveDraft(nextSelectedId = control.id) { current ->
                                    current.copy(
                                        controls = current.controls.map { c ->
                                            if (c.id == control.id) c.copy(xPercent = finalX, yPercent = finalY) else c
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        // 4. WebRTC Stream Stats + Device Telemetry Pill (top-end)
        if (settings.webPlayerConfig.showWebRtcStatsHud && !isLiveEditMode) {
            WebPlayerStatsPill(
                streamStats = streamStats,
                perfStats = perfStats,
                region = settings.webPlayerConfig.regionPreset,
                accentColor = accent,
                antiAfkEnabled = settings.webPlayerConfig.antiAfkEnabled,
                connectedControllerName = primaryControllerName,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 16.dp)
            )
        }

        // 5. Full In-Game Layout Studio HUD (matching LayoutEditorScreen full capabilities!)
        if (isLiveEditMode && liveEditDraft != null) {
            val draft = liveEditDraft!!
            val selectedCtrl = draft.controls.firstOrNull { it.id == selectedEditControlId }
            val hasChanges = liveEditUndoStack.isNotEmpty()

            // Top-Left Pill: Cancel + Save
            Surface(
                color = Color(0xE60B0E14),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 10.dp)
                    .border(
                        width = 1.dp,
                        color = if (hasChanges) accent else Color(0xFF314057),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { exitLiveEdit() },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("live_edit_cancel_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = strings.liveEditCancelButton, tint = Color.White)
                    }
                    Button(
                        onClick = {
                            val toSave = liveEditDraft?.copy(
                                gridSnap = snapToGrid,
                                updatedAt = System.currentTimeMillis()
                            )
                            exitLiveEdit()
                            if (toSave != null) {
                                onSaveEditedProfile(toSave)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasChanges) accent else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (hasChanges) MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("live_edit_save_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (hasChanges) strings.liveEditSaveButton else "Saved",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
            }

            // Top-Right Pill: Add, Grid Settings, Undo, Profile Meta, Hide Tools
            Surface(
                color = Color(0xE60B0E14),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 10.dp)
                    .border(1.dp, Color(0xFF314057), RoundedCornerShape(24.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    if (!hideLiveStudioTools) {
                        IconButton(
                            onClick = { showLiveAddSheet = true },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("live_edit_add_control_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Control", tint = accent)
                        }
                        IconButton(
                            onClick = { showLiveGridDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("live_edit_grid_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "Grid Settings",
                                tint = if (snapToGrid) accent else Color(0xFF9FB0C7)
                            )
                        }
                        if (liveEditUndoStack.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val prev = liveEditUndoStack.lastOrNull() ?: return@IconButton
                                    liveEditUndoStack = liveEditUndoStack.dropLast(1)
                                    liveEditDraft = prev
                                    if (selectedEditControlId !in prev.controls.map { it.id }) {
                                        selectedEditControlId = prev.controls.firstOrNull()?.id
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("live_edit_undo_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color.White)
                            }
                        }
                        IconButton(
                            onClick = { showLiveMetaDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("live_edit_meta_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Profile Details", tint = TacticalCyan)
                        }
                    }
                    IconButton(
                        onClick = { hideLiveStudioTools = !hideLiveStudioTools },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("live_edit_toggle_hud_button")
                    ) {
                        Icon(
                            imageVector = if (hideLiveStudioTools) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (hideLiveStudioTools) "Show Studio Tools" else "Hide Studio Tools",
                            tint = Color(0xFF9FB0C7)
                        )
                    }
                }
            }

            // Auto-flipping contextual Selected-Control Studio Capsule (Bind, Style, Duplicate, Delete)
            if (selectedCtrl != null && !hideLiveStudioTools) {
                val dockTop = selectedCtrl.yPercent >= 0.52f
                Surface(
                    color = Color(0xEE121721),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .align(if (dockTop) Alignment.TopCenter else Alignment.BottomCenter)
                        .padding(
                            top = if (dockTop) 60.dp else 0.dp,
                            bottom = if (dockTop) 0.dp else 14.dp,
                            start = 12.dp,
                            end = 12.dp
                        )
                        .border(1.dp, Color(selectedCtrl.accent.hex).copy(alpha = 0.72f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
                            Text(
                                text = selectedCtrl.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(selectedCtrl.accent.hex),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = selectedCtrl.binding.codeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9FB0C7)
                            )
                        }

                        OutlinedButton(
                            onClick = { showLiveRebindSheet = true },
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("live_edit_rebind_button")
                        ) {
                            Text("Bind", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showLiveInspectorSheet = true },
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("live_edit_customize_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Style", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = {
                                val clone = LayoutStudioHelper.duplicateControl(selectedCtrl)
                                mutateLiveDraft(nextSelectedId = clone.id) { current ->
                                    current.copy(controls = current.controls + clone)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("live_edit_duplicate_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = {
                                val targetId = selectedCtrl.id
                                mutateLiveDraft(nextSelectedId = null) { current ->
                                    current.copy(controls = current.controls.filterNot { it.id == targetId })
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("live_edit_delete_button")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { selectedEditControlId = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Deselect", tint = Color(0xFF9FB0C7), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        } else {
            // 6. Draggable Quick Menu Bubble
            val maxBubbleX = (canvasWidthPx - with(density) { 56.dp.toPx() }).coerceAtLeast(0f)
            val maxBubbleY = (canvasHeightPx - with(density) { 56.dp.toPx() }).coerceAtLeast(0f)
            WebPlayerQuickBubble(
                accentColor = accent,
                regionActive = settings.webPlayerConfig.regionPreset.isEnabled,
                controlsVisible = effectiveControlsVisible,
                onClick = {
                    val opening = !menuExpanded
                    if (opening) {
                        InputBridge.releaseAll()
                    }
                    menuExpanded = opening
                },
                onDrag = { dx, dy ->
                    bubbleOffsetX = (bubbleOffsetX + dx).coerceIn(0f, maxBubbleX)
                    bubbleOffsetY = (bubbleOffsetY + dy).coerceIn(0f, maxBubbleY)
                },
                modifier = Modifier.offset {
                    IntOffset(bubbleOffsetX.roundToInt(), bubbleOffsetY.roundToInt())
                }
            )
        }

        // 7. Touch-Blocking Scrim + Slide-Out Quick Menu Card
        // While the menu is open, all touches outside the menu are intercepted and consumed
        // so nothing underneath (WebView, virtual buttons, bubble) can be triggered; tapping
        // outside automatically closes the menu first before normal screen touches resume.
        if (menuExpanded && !isLiveEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            var allReleased = false
                            while (!allReleased) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                                if (event.changes.none { it.pressed }) {
                                    allReleased = true
                                }
                            }
                            menuExpanded = false
                        }
                    }
                    .testTag("web_player_menu_scrim")
            )
            WebPlayerQuickMenuCard(
                activeProfile = activeProfile,
                allProfiles = allProfiles,
                webConfig = settings.webPlayerConfig,
                filterState = settings.gameFilter,
                controlsVisible = effectiveControlsVisible,
                crosshairEnabled = settings.crosshair.enabled,
                gyroEnabled = settings.gyroAim.enabled,
                accentColor = accent,
                connectedControllerName = primaryControllerName,
                autoHideTouchForController = settings.physicalController.autoHideTouchControls,
                touchAutoHiddenByController = touchAutoHidden,
                onClose = { menuExpanded = false },
                onToggleControlsVisible = {
                    if (touchAutoHidden) {
                        manualShowControlsOverride = true
                        controlsVisible = true
                    } else if (effectiveControlsVisible) {
                        controlsVisible = false
                        manualShowControlsOverride = false
                    } else {
                        controlsVisible = true
                        manualShowControlsOverride = true
                    }
                },
                onEnterLiveEdit = {
                    if (activeProfile != null) {
                        InputBridge.releaseAll()
                        liveEditDraft = activeProfile
                        liveEditUndoStack = emptyList()
                        selectedEditControlId = activeProfile.controls.firstOrNull()?.id
                        snapToGrid = activeProfile.gridSnap
                        gridDivisions = settings.gridDivisions.coerceIn(4, 32)
                        hideLiveStudioTools = false
                        isLiveEditMode = true
                        controlsVisible = true
                        manualShowControlsOverride = true
                        menuExpanded = false
                    }
                },
                onSelectProfile = onSelectProfile,
                onSelectRegion = { region ->
                    onSelectRegion(region)
                    menuExpanded = false
                },
                onSelectFilterSlot = onSelectFilterSlot,
                onUpdateFilterStyle = onUpdateFilterStyle,
                onOpenFullFilterStudio = {
                    menuExpanded = false
                    if (settings.gameFilter.activeSlot == 0) {
                        onSelectFilterSlot(1)
                    }
                    showFullFilterStudio = true
                },
                onToggleStretch = onToggleStretch,
                onToggleCrosshair = onToggleCrosshair,
                onToggleGyro = onToggleGyro,
                onToggleStatsHud = onToggleStatsHud,
                onToggleAntiAfk = onToggleAntiAfk,
                onToggleAutoHideTouch = onToggleAutoHideTouch,
                onBrowserBack = onBrowserBack,
                onBrowserReload = {
                    onBrowserReload()
                    menuExpanded = false
                },
                onResetInputs = {
                    onResetInputs()
                    menuExpanded = false
                },
                onExitPlayer = onExit,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp)
            )
        }

        // 7b. Full 18-Module GeForce Freestyle Filter Studio Side Drawer inside Web Play Mode
        if (showFullFilterStudio && !isLiveEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.22f))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            var allReleased = false
                            while (!allReleased) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                                if (event.changes.none { it.pressed }) {
                                    allReleased = true
                                }
                            }
                            showFullFilterStudio = false
                        }
                    }
                    .testTag("web_player_filter_studio_scrim")
            )
            Surface(
                shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                color = Color(0xF20D1117),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .widthIn(min = 320.dp, max = 385.dp)
                    .padding(vertical = 8.dp)
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
                    )
                    .pointerInput(Unit) {
                        // Intercept touches inside the Filter Studio drawer so they never hit the scrim or game
                    }
                    .testTag("web_player_full_filter_studio")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp)
                ) {
                    GeForceFilterPanelContent(
                        filterState = settings.gameFilter,
                        onSelectSlot = onSelectFilterSlot,
                        onPreviewStyle = onPreviewFilterStyle,
                        onUpdateStyle = onUpdateFilterStyle,
                        onResetSlot = onResetFilterSlot,
                        onUploadStickerClick = {
                            stickerPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        compactMode = true,
                        onClose = { showFullFilterStudio = false }
                    )
                }
            }
        }

        // 8. In-App OAuth Popup Window Overlay (NVIDIA / Steam / Epic / Microsoft sign-in)
        if (popupWebView != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF13111E),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .testTag("web_player_auth_popup")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1C1A2D))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.webPlayerAuthPopupTitle,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClosePopup,
                            modifier = Modifier.testTag("web_player_auth_popup_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.commonClose,
                                tint = Color.White
                            )
                        }
                    }
                    AndroidView(
                        factory = { popupWebView },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }

    // 9. Full Layout Studio Sheets & Dialogs for Live Edit Mode
    val activeLiveDraft = liveEditDraft
    if (isLiveEditMode && activeLiveDraft != null) {
        val activeSelectedControl = remember(activeLiveDraft, selectedEditControlId) {
            activeLiveDraft.controls.firstOrNull { it.id == selectedEditControlId }
        }

        if (showLiveAddSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLiveAddSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                CommandPickerSheetContent(
                    title = "Add Virtual Control",
                    includeClusters = true,
                    onPick = { binding ->
                        val newCtrl = LayoutStudioHelper.createControlFromBinding(binding)
                        mutateLiveDraft(nextSelectedId = newCtrl.id) { current ->
                            current.copy(controls = current.controls + newCtrl)
                        }
                        showLiveAddSheet = false
                    }
                )
            }
        }

        if (showLiveRebindSheet && activeSelectedControl != null) {
            ModalBottomSheet(
                onDismissRequest = { showLiveRebindSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                val isCluster = activeSelectedControl.controlType == ControlType.ANALOG_STICK ||
                    activeSelectedControl.controlType == ControlType.DPAD_CLUSTER ||
                    activeSelectedControl.controlType == ControlType.WASD_CLUSTER ||
                    activeSelectedControl.controlType == ControlType.TOUCHPAD
                CommandPickerSheetContent(
                    title = "Rebind '${activeSelectedControl.label}'",
                    includeClusters = isCluster,
                    onPick = { binding ->
                        val rebound = LayoutStudioHelper.rebindControl(activeSelectedControl, binding)
                        mutateLiveDraft(nextSelectedId = rebound.id) { current ->
                            current.copy(
                                controls = current.controls.map { c ->
                                    if (c.id == rebound.id) rebound else c
                                }
                            )
                        }
                        showLiveRebindSheet = false
                    }
                )
            }
        }

        if (showLiveInspectorSheet && activeSelectedControl != null) {
            ModalBottomSheet(
                onDismissRequest = { showLiveInspectorSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                ControlInspectorSheetContent(
                    control = activeSelectedControl,
                    onChange = { updated ->
                        mutateLiveDraft(nextSelectedId = updated.id) { current ->
                            current.copy(
                                controls = current.controls.map { c ->
                                    if (c.id == updated.id) updated else c
                                }
                            )
                        }
                    },
                    onPickCustomImage = { uri ->
                        val targetId = activeSelectedControl.id
                        scope.launch {
                            val result = ButtonImageCodec.encodeUriToBase64(context, uri)
                            result.onSuccess { encoded ->
                                mutateLiveDraft(nextSelectedId = targetId) { current ->
                                    current.copy(
                                        controls = current.controls.map { c ->
                                            if (c.id == targetId) c.copy(customImageBase64 = encoded) else c
                                        }
                                    )
                                }
                            }.onFailure { err ->
                                Toast.makeText(
                                    context,
                                    err.message ?: "Unable to import image",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onNudge = { dx, dy ->
                        val nudged = LayoutStudioHelper.nudgeControl(activeSelectedControl, dx, dy)
                        mutateLiveDraft(nextSelectedId = nudged.id) { current ->
                            current.copy(
                                controls = current.controls.map { c ->
                                    if (c.id == nudged.id) nudged else c
                                }
                            )
                        }
                    },
                    onMirror = {
                        val mirrored = LayoutStudioHelper.mirrorControl(activeSelectedControl)
                        mutateLiveDraft(nextSelectedId = mirrored.id) { current ->
                            current.copy(
                                controls = current.controls.map { c ->
                                    if (c.id == mirrored.id) mirrored else c
                                }
                            )
                        }
                    },
                    onClose = { showLiveInspectorSheet = false }
                )
            }
        }

        if (showLiveGridDialog) {
            AlertDialog(
                onDismissRequest = { showLiveGridDialog = false },
                title = { Text("Grid & Snapping") },
                text = {
                    GridSettingsContent(
                        gridSnapEnabled = snapToGrid,
                        gridDivisions = gridDivisions,
                        onGridSnapChange = { enabled ->
                            snapToGrid = enabled
                            onGridSnapChange(enabled)
                        },
                        onGridDivisionsChange = { divisions ->
                            gridDivisions = divisions
                            onGridDivisionsChange(divisions)
                        }
                    )
                },
                confirmButton = {
                    Button(onClick = { showLiveGridDialog = false }) {
                        Text("Done")
                    }
                }
            )
        }

        if (showLiveMetaDialog) {
            EditProfileMetaDialog(
                initialName = activeLiveDraft.name,
                initialGame = activeLiveDraft.gameTitle,
                initialGenre = activeLiveDraft.genre,
                onDismiss = { showLiveMetaDialog = false },
                onConfirm = { name, game, genre ->
                    mutateLiveDraft { current ->
                        current.copy(name = name, gameTitle = game, genre = genre)
                    }
                    showLiveMetaDialog = false
                }
            )
        }
    }
}

@Composable
private fun VirtualMouseCursorOverlay(
    cursor: VirtualMouseCursorState,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val x = cursor.xPx.coerceIn(0f, size.width)
        val y = cursor.yPx.coerceIn(0f, size.height)
        val scale = 1.dp.toPx()

        if (cursor.leftPressed || cursor.rightPressed) {
            val ringColor = if (cursor.rightPressed) Color(0xFFFFB74D) else accentColor
            drawCircle(
                color = ringColor.copy(alpha = 0.28f),
                radius = 16f * scale,
                center = Offset(x, y)
            )
            drawCircle(
                color = ringColor.copy(alpha = 0.85f),
                radius = 9f * scale,
                center = Offset(x, y),
                style = Stroke(width = 1.8f * scale)
            )
        }

        val pointerPath = Path().apply {
            moveTo(x, y)
            lineTo(x, y + 18f * scale)
            lineTo(x + 4.8f * scale, y + 13.6f * scale)
            lineTo(x + 8.4f * scale, y + 20.5f * scale)
            lineTo(x + 11.4f * scale, y + 19.0f * scale)
            lineTo(x + 7.8f * scale, y + 12.2f * scale)
            lineTo(x + 13.8f * scale, y + 12.0f * scale)
            close()
        }

        // Crisp dark outline + bright white interior (stays visible over light & dark scenes)
        drawPath(
            path = pointerPath,
            color = Color(0xFF0A0C14),
            style = Stroke(
                width = 2.6f * scale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = pointerPath,
            color = Color.White
        )
    }
}

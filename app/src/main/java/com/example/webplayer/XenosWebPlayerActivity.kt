package com.example.webplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.model.AppLocalization
import com.example.model.ControllerProfile
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import com.example.model.GfnRegionPreset
import com.example.model.LocalNexusStrings
import com.example.model.WebRtcStreamStats
import com.example.model.WebStreamQualityConfig
import com.example.service.PerformanceMonitor
import com.example.ui.components.CrosshairOverlayCanvas
import com.example.ui.components.LiveOverlayEditToolbar
import com.example.ui.components.VirtualControlCanvasItem
import com.example.ui.theme.NexusPadTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    private var latestWebConfig: WebStreamQualityConfig = WebStreamQualityConfig()
    private var latestSettings: OverlaySettings = OverlaySettings()

    private val _streamStats = MutableStateFlow(WebRtcStreamStats())
    private val streamStats = _streamStats.asStateFlow()

    private val _pageProgress = MutableStateFlow(0)
    private val pageProgress = _pageProgress.asStateFlow()

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
            evalJs(BetterGfnScriptEngine.buildMouseButtonJs(button, isDown))
        }

        override fun onMouseScroll(scrollUp: Boolean) {
            evalJs(BetterGfnScriptEngine.buildMouseWheelJs(scrollUp))
        }

        override fun onMouseMove(dxPx: Float, dyPx: Float) {
            evalJs(BetterGfnScriptEngine.buildMouseMoveJs(dxPx, dyPx))
        }

        override fun onAnalogStick(isLeft: Boolean, x: Float, y: Float) {
            evalJs(BetterGfnScriptEngine.buildAnalogStickJs(isLeft, x, y))
        }

        override fun onResetAll() {
            evalJs(BetterGfnScriptEngine.buildResetAllInputsJs())
        }
    }

    private inner class XenosWebJsBridge {
        @JavascriptInterface
        fun onWebRtcStats(rawJson: String) {
            val parsed = BetterGfnScriptEngine.parseWebRtcStatsJson(rawJson)
            _streamStats.value = parsed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersiveFullscreen()

        prefsRepo = PreferencesRepository(applicationContext)
        profileRepo = ProfileRepository(NexusDatabase.getInstance(applicationContext).profileDao())
        gyroAimEngine = GyroAimEngine(applicationContext)
        performanceMonitor = PerformanceMonitor(applicationContext)

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
            val pressedIds by InputBridge.pressedControlIds.collectAsStateWithLifecycle()
            val toggledIds by InputBridge.toggledControlIds.collectAsStateWithLifecycle()

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
                        streamStats = liveStreamStats,
                        perfStats = perfStats,
                        loadProgress = progress,
                        pressedIds = pressedIds,
                        toggledIds = toggledIds,
                        popupWebView = popupWebViewState,
                        onClosePopup = { closePopupWebView() },
                        onBindMainWebView = { webView ->
                            mainWebView = webView
                            loadGeForceNow(webView, effectiveSettings.webPlayerConfig)
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
                        onUpdateFilterStyle = { updatedStyle ->
                            GameFilterBridge.updateLiveStyle(updatedStyle)
                            val nextFilter = liveFilterState.withUpdatedStyle(updatedStyle)
                                .copy(activeSlot = updatedStyle.slot.coerceIn(1, 3))
                            val updatedSettings = effectiveSettings.copy(gameFilter = nextFilter)
                            latestSettings = updatedSettings
                            applyLiveStyleAndBootstrap(updatedSettings)
                            lifecycleScope.launch { prefsRepo.updateFilterStyle(updatedStyle) }
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
                        onBrowserBack = {
                            mainWebView?.let { if (it.canGoBack()) it.goBack() }
                        },
                        onBrowserReload = {
                            mainWebView?.let { loadGeForceNow(it, latestWebConfig) }
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

    private fun applyLiveStyleAndBootstrap(settings: OverlaySettings) {
        val web = mainWebView ?: return
        runOnUiThread {
            if (mainWebView !== web) return@runOnUiThread
            val desiredUa = settings.webPlayerConfig.userAgentMode.userAgentString
            if (web.settings.userAgentString != desiredUa) {
                web.settings.userAgentString = desiredUa
            }
            web.evaluateJavascript(
                BetterGfnScriptEngine.buildBootstrapScript(settings.webPlayerConfig),
                null
            )
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

    private fun loadGeForceNow(webView: WebView, config: WebStreamQualityConfig) {
        webView.settings.userAgentString = config.userAgentMode.userAgentString
        val headers = BetterGfnScriptEngine.buildNavigationHeaders(config)
        val targetUrl = BetterGfnScriptEngine.GFN_WEB_URL
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
                view?.evaluateJavascript(
                    BetterGfnScriptEngine.buildBootstrapScript(latestWebConfig),
                    null
                )
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(
                    BetterGfnScriptEngine.buildBootstrapScript(latestWebConfig),
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
        fun launch(context: Context) {
            val intent = Intent(context, XenosWebPlayerActivity::class.java)
            context.startActivity(intent)
        }
    }
}

@Composable
private fun WebPlayerScreenContent(
    settings: OverlaySettings,
    activeProfile: ControllerProfile?,
    allProfiles: List<ControllerProfile>,
    streamStats: WebRtcStreamStats,
    perfStats: com.example.service.PerfStatsSnapshot,
    loadProgress: Int,
    pressedIds: Set<String>,
    toggledIds: Set<String>,
    popupWebView: WebView?,
    onClosePopup: () -> Unit,
    onBindMainWebView: (WebView) -> Unit,
    onSelectProfile: (String) -> Unit,
    onSaveEditedProfile: (ControllerProfile) -> Unit,
    onSelectRegion: (GfnRegionPreset) -> Unit,
    onSelectFilterSlot: (Int) -> Unit,
    onUpdateFilterStyle: (GameFilterStyle) -> Unit,
    onToggleStretch: () -> Unit,
    onToggleCrosshair: () -> Unit,
    onToggleGyro: () -> Unit,
    onToggleStatsHud: () -> Unit,
    onBrowserBack: () -> Unit,
    onBrowserReload: () -> Unit,
    onResetInputs: () -> Unit,
    onExit: () -> Unit
) {
    val strings = LocalNexusStrings.current
    val accent = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    var controlsVisible by remember { mutableStateOf(settings.webPlayerConfig.showControlsByDefault) }
    var menuExpanded by remember { mutableStateOf(false) }
    var isLiveEditMode by remember { mutableStateOf(false) }
    var liveEditDraft by remember { mutableStateOf<ControllerProfile?>(null) }
    var selectedEditControlId by remember { mutableStateOf<String?>(null) }
    var snapToGrid by remember(settings.gridSnapEnabled) { mutableStateOf(settings.gridSnapEnabled) }
    var bubbleOffsetX by remember { mutableFloatStateOf(24f) }
    var bubbleOffsetY by remember { mutableFloatStateOf(16f) }

    val displayedProfile = if (isLiveEditMode) liveEditDraft ?: activeProfile else activeProfile

    BackHandler(enabled = menuExpanded && !isLiveEditMode) {
        menuExpanded = false
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

        // 2. Crosshair HUD (non-interactive overlay)
        if (settings.crosshair.enabled && !isLiveEditMode) {
            CrosshairOverlayCanvas(
                settings = settings.crosshair,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. XenosPad Virtual Controls layer (direct WebInputSink routing!)
        if ((controlsVisible || isLiveEditMode) && displayedProfile != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                displayedProfile.controls.forEach { control ->
                    val halfSizePx = with(density) { (control.sizeDp.dp / 2f).toPx() }
                    val xPx = (control.xPercent * canvasWidthPx - halfSizePx)
                        .roundToInt()
                        .coerceIn(0, (canvasWidthPx - halfSizePx * 2f).roundToInt().coerceAtLeast(0))
                    val yPx = (control.yPercent * canvasHeightPx - halfSizePx)
                        .roundToInt()
                        .coerceIn(0, (canvasHeightPx - halfSizePx * 2f).roundToInt().coerceAtLeast(0))

                    VirtualControlCanvasItem(
                        control = control,
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
                        onDragDelta = { dx, dy ->
                            if (isLiveEditMode) {
                                val current = liveEditDraft ?: return@VirtualControlCanvasItem
                                val divs = settings.gridDivisions.coerceIn(4, 32).toFloat()
                                val aspectY = (divs * 0.66f).roundToInt().coerceIn(4, 24).toFloat()
                                val updatedControls = current.controls.map { c ->
                                    if (c.id != control.id) c else {
                                        val rawX = (c.xPercent + dx / canvasWidthPx).coerceIn(0.02f, 0.98f)
                                        val rawY = (c.yPercent + dy / canvasHeightPx).coerceIn(0.03f, 0.97f)
                                        val finalX = if (snapToGrid) ((rawX * divs).roundToInt() / divs).coerceIn(0.02f, 0.98f) else rawX
                                        val finalY = if (snapToGrid) ((rawY * aspectY).roundToInt() / aspectY).coerceIn(0.03f, 0.97f) else rawY
                                        c.copy(xPercent = finalX, yPercent = finalY)
                                    }
                                }
                                liveEditDraft = current.copy(controls = updatedControls)
                            }
                        },
                        modifier = Modifier.offset { IntOffset(xPx, yPx) }
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
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 16.dp)
            )
        }

        // 5. Live On-Game Layout Edit Toolbar
        if (isLiveEditMode && liveEditDraft != null) {
            val draft = liveEditDraft!!
            val selectedCtrl = draft.controls.firstOrNull { it.id == selectedEditControlId }
            LiveOverlayEditToolbar(
                profileName = draft.name,
                selectedControl = selectedCtrl,
                snapToGrid = snapToGrid,
                accentColor = accent,
                onSizeDelta = { delta ->
                    val targetId = selectedEditControlId ?: return@LiveOverlayEditToolbar
                    liveEditDraft = draft.copy(
                        controls = draft.controls.map { c ->
                            if (c.id == targetId) c.copy(sizeDp = (c.sizeDp + delta).coerceIn(38f, 160f)) else c
                        }
                    )
                },
                onOpacityDelta = { delta ->
                    val targetId = selectedEditControlId ?: return@LiveOverlayEditToolbar
                    liveEditDraft = draft.copy(
                        controls = draft.controls.map { c ->
                            if (c.id == targetId) c.copy(opacity = (c.opacity + delta).coerceIn(0.20f, 1.0f)) else c
                        }
                    )
                },
                onToggleSnap = { snapToGrid = !snapToGrid },
                onCancel = {
                    isLiveEditMode = false
                    liveEditDraft = null
                    selectedEditControlId = null
                },
                onSave = {
                    val toSave = liveEditDraft
                    isLiveEditMode = false
                    liveEditDraft = null
                    selectedEditControlId = null
                    if (toSave != null) {
                        onSaveEditedProfile(toSave)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            )
        } else {
            // 6. Draggable Quick Menu Bubble
            val maxBubbleX = (canvasWidthPx - with(density) { 56.dp.toPx() }).coerceAtLeast(0f)
            val maxBubbleY = (canvasHeightPx - with(density) { 56.dp.toPx() }).coerceAtLeast(0f)
            WebPlayerQuickBubble(
                accentColor = accent,
                regionActive = settings.webPlayerConfig.regionPreset.isEnabled,
                controlsVisible = controlsVisible,
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
                controlsVisible = controlsVisible,
                crosshairEnabled = settings.crosshair.enabled,
                gyroEnabled = settings.gyroAim.enabled,
                accentColor = accent,
                onClose = { menuExpanded = false },
                onToggleControlsVisible = { controlsVisible = !controlsVisible },
                onEnterLiveEdit = {
                    if (activeProfile != null) {
                        InputBridge.releaseAll()
                        liveEditDraft = activeProfile
                        selectedEditControlId = activeProfile.controls.firstOrNull()?.id
                        isLiveEditMode = true
                        controlsVisible = true
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
                onToggleStretch = onToggleStretch,
                onToggleCrosshair = onToggleCrosshair,
                onToggleGyro = onToggleGyro,
                onToggleStatsHud = onToggleStatsHud,
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
}

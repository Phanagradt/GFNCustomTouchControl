package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.data.NexusDatabase
import com.example.data.OverlaySettings
import com.example.data.PreferencesRepository
import com.example.data.ProfileRepository
import com.example.input.InputBridge
import com.example.model.AxisPair
import com.example.model.ControlShape
import com.example.model.ControllerProfile
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import com.example.model.VirtualControl
import com.example.ui.components.GeForceFilterPanelContent
import com.example.ui.components.ScreenFilterOverlayCanvas
import com.example.ui.components.VirtualControlCanvasItem
import com.example.ui.components.computeOverlayDimAmount
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

class OverlayControllerService : Service() {

    companion object {
        private const val CHANNEL_ID = "nexuspad_overlay_channel"
        private const val NOTIFICATION_ID = 4107
        const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, OverlayControllerService::class.java)
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
    private var filterOverlayView: ComposeView? = null
    private var filterOverlayParams: WindowManager.LayoutParams? = null
    private var floatingBubbleView: ComposeView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var menuWheelView: ComposeView? = null
    private var filterPanelView: ComposeView? = null

    private val buttonsVisible = MutableStateFlow(true)
    private val bubbleLocked = MutableStateFlow(false)
    private val menuWheelOpen = MutableStateFlow(false)
    private val filterPanelOpen = MutableStateFlow(false)
    private val filterStateFlow = MutableStateFlow(GameFilterState.default())
    private val activeProfileName = MutableStateFlow("NexusPad")
    private val allProfilesFlow = MutableStateFlow<List<ControllerProfile>>(emptyList())
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

        InputBridge.setOverlayRunning(true)
        attachFloatingBubble()
        observeActiveProfile()
        observeMenuWheelState()
        observeFilterPanelState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_OVERLAY) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
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
                    .collect { filterState ->
                        applyLiveFilterState(filterState)
                    }
            }
            val controlSettingsFlow = prefsRepo.settingsFlow
                .map { s ->
                    s.copy(gameFilter = GameFilterState.default())
                }
                .distinctUntilChanged()

            combine(
                profileRepo.activeProfile,
                controlSettingsFlow,
                buttonsVisible
            ) { profile, settings, visible ->
                Triple(profile, settings, visible)
            }.collect { (profile, settings, visible) ->
                activeProfileName.value = profile?.name ?: "NexusPad"
                clearControlWindows()
                InputBridge.releaseAll()
                if (visible && profile != null) {
                    renderControlWindows(profile, settings)
                }
                ensureFloatingChromeOnTop()
            }
        }
    }

    private fun observeMenuWheelState() {
        serviceScope.launch {
            menuWheelOpen.collect { open ->
                if (open) {
                    filterPanelOpen.value = false
                    showMenuWheelOverlay()
                } else {
                    dismissMenuWheelOverlay()
                }
            }
        }
    }

    private fun observeFilterPanelState() {
        serviceScope.launch {
            filterPanelOpen.collect { open ->
                if (open) {
                    menuWheelOpen.value = false
                    showFloatingFilterPanel()
                } else {
                    dismissFloatingFilterPanel()
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

    private fun openMainApp() {
        menuWheelOpen.value = false
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { startActivity(intent) }
    }

    private fun openKeyboardFromWheel() {
        menuWheelOpen.value = false
        val shown = InputBridge.requestShowKeyboard()
        if (!shown) {
            SystemIntegrations.showInputMethodPicker(this)
        }
    }

    private fun attachFloatingBubble() {
        val dm = resources.displayMetrics
        val bubblePx = (56 * dm.density).roundToInt()
        val params = WindowManager.LayoutParams(
            bubblePx,
            bubblePx,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (dm.widthPixels - bubblePx - (18 * dm.density).roundToInt()).coerceAtLeast(0)
            y = (dm.heightPixels * 0.14f).roundToInt()
        }
        bubbleParams = params

        val bubble = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val visible by buttonsVisible.collectAsState()
                val locked by bubbleLocked.collectAsState()
                val wheelOpen by menuWheelOpen.collectAsState()

                NexusPadTheme {
                    val accent = when {
                        wheelOpen -> TacticalCyan
                        locked -> TurboAmber
                        !visible -> Color(0xFF9FB0C7)
                        else -> GeForceLime
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xEE182130), Color(0xEE0B0E14))
                                )
                            )
                            .border(2.dp, accent, CircleShape)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val pointerId = down.id
                                    down.consume()
                                    val touchSlop = viewConfiguration.touchSlop
                                    var prevPos = down.position
                                    var totalMoved = 0f
                                    var didDrag = false
                                    val screenW = resources.displayMetrics.widthPixels
                                    val screenH = resources.displayMetrics.heightPixels

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId }
                                        if (change == null || !change.pressed) {
                                            break
                                        }
                                        val dx = change.position.x - prevPos.x
                                        val dy = change.position.y - prevPos.y
                                        prevPos = change.position
                                        totalMoved += hypot(dx, dy)

                                        if (totalMoved > touchSlop) {
                                            didDrag = true
                                            if (!bubbleLocked.value) {
                                                change.consume()
                                                params.x = (params.x + dx.roundToInt())
                                                    .coerceIn(0, (screenW - params.width).coerceAtLeast(0))
                                                params.y = (params.y + dy.roundToInt())
                                                    .coerceIn(0, (screenH - params.height).coerceAtLeast(0))
                                                runCatching { windowManager.updateViewLayout(this@apply, params) }
                                            }
                                        }
                                    }

                                    if (!didDrag) {
                                        if (filterPanelOpen.value) {
                                            filterPanelOpen.value = false
                                        } else {
                                            menuWheelOpen.value = !menuWheelOpen.value
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (wheelOpen || filterPanelOpen.collectAsState().value) {
                                Icons.Default.Close
                            } else {
                                Icons.Default.Gamepad
                            },
                            contentDescription = "Open NexusPad Menu Wheel",
                            tint = accent,
                            modifier = Modifier.size(25.dp)
                        )
                        if (locked && !wheelOpen) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(TurboAmber)
                            )
                        } else if (!visible && !wheelOpen) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF9FB0C7))
                            )
                        }
                    }
                }
            }
        }

        floatingBubbleView = bubble
        runCatching { windowManager.addView(bubble, params) }
    }

    private fun applyLiveFilterState(state: GameFilterState) {
        filterStateFlow.value = state
        val activeStyle = state.activeStyle
        if (activeStyle == null) {
            removeScreenFilterOverlay()
            return
        }

        val dim = computeOverlayDimAmount(activeStyle)
        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            if (dim > 0.005f) WindowManager.LayoutParams.FLAG_DIM_BEHIND else 0

        val existingView = filterOverlayView
        val existingParams = filterOverlayParams
        if (existingView != null && existingParams != null) {
            existingParams.flags = baseFlags
            existingParams.dimAmount = dim
            runCatching { windowManager.updateViewLayout(existingView, existingParams) }
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
            }
            val view = ComposeView(this).apply {
                attachLifecycle(this)
                setContent {
                    val currentFilter by filterStateFlow.collectAsState()
                    val styleToDraw = currentFilter.activeStyle
                    if (styleToDraw != null) {
                        ScreenFilterOverlayCanvas(style = styleToDraw)
                    }
                }
            }
            filterOverlayView = view
            filterOverlayParams = params
            runCatching { windowManager.addView(view, params) }
            // Keep screen filter strictly underneath all virtual buttons, bubble, wheel & panel
            bringControlsAndChromeAboveFilter()
        }
    }

    private fun removeScreenFilterOverlay() {
        filterOverlayView?.let { view ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        filterOverlayView = null
        filterOverlayParams = null
    }

    private fun bringControlsAndChromeAboveFilter() {
        val snapshot = controlViews.zip(controlViewParams)
        snapshot.forEach { (view, _) ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        snapshot.forEach { (view, params) ->
            runCatching { windowManager.addView(view, params) }
        }
        ensureFloatingChromeOnTop()
    }

    private fun showMenuWheelOverlay() {
        if (menuWheelView != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val wheelView = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val visible by buttonsVisible.collectAsState()
                val locked by bubbleLocked.collectAsState()
                val profileName by activeProfileName.collectAsState()
                val profileCount = allProfilesFlow.collectAsState().value.size
                val filterState by filterStateFlow.collectAsState()

                NexusPadTheme {
                    RadialMenuWheelOverlay(
                        profileName = profileName,
                        buttonsVisible = visible,
                        bubbleLocked = locked,
                        canSwitchProfile = profileCount > 1,
                        activeFilterSlot = filterState.activeSlot,
                        onDismiss = { menuWheelOpen.value = false },
                        onToggleButtons = {
                            buttonsVisible.value = !visible
                            menuWheelOpen.value = false
                        },
                        onSwitchProfile = {
                            cycleNextProfile()
                        },
                        onOpenKeyboard = {
                            openKeyboardFromWheel()
                        },
                        onOpenFilters = {
                            menuWheelOpen.value = false
                            filterPanelOpen.value = true
                        },
                        onReleaseAll = {
                            InputBridge.releaseAll()
                            menuWheelOpen.value = false
                        },
                        onToggleLock = {
                            bubbleLocked.value = !locked
                            menuWheelOpen.value = false
                        },
                        onStopOverlay = {
                            menuWheelOpen.value = false
                            stopSelf()
                        },
                        onOpenApp = {
                            openMainApp()
                        }
                    )
                }
            }
        }
        menuWheelView = wheelView
        runCatching { windowManager.addView(wheelView, params) }
    }

    private fun dismissMenuWheelOverlay() {
        menuWheelView?.let { view ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        menuWheelView = null
    }

    private fun showFloatingFilterPanel() {
        if (filterPanelView != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val panel = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val filterState by filterStateFlow.collectAsState()

                NexusPadTheme(darkTheme = true) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { filterPanelOpen.value = false }
                            )
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val maxPanelHeight = (maxHeight * 0.82f).coerceAtLeast(320.dp)
                        Box(
                            modifier = Modifier
                                .widthIn(max = 368.dp)
                                .heightIn(max = maxPanelHeight)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {}
                                )
                                .verticalScroll(rememberScrollState())
                        ) {
                            GeForceFilterPanelContent(
                                filterState = filterState,
                                compactMode = true,
                                onSelectSlot = { slot ->
                                    val optimistic = filterState.copy(activeSlot = slot.coerceIn(0, 3))
                                    applyLiveFilterState(optimistic)
                                    serviceScope.launch {
                                        prefsRepoRef?.setActiveFilterSlot(slot)
                                    }
                                },
                                onUpdateStyle = { updatedStyle ->
                                    val optimistic = filterState
                                        .withUpdatedStyle(updatedStyle)
                                        .copy(activeSlot = updatedStyle.slot)
                                    applyLiveFilterState(optimistic)
                                    serviceScope.launch {
                                        prefsRepoRef?.updateFilterStyle(updatedStyle)
                                    }
                                },
                                onResetSlot = {
                                    val slot = filterState.activeSlot
                                    if (slot in 1..3) {
                                        applyLiveFilterState(filterState.withResetSlot(slot))
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
        filterPanelView?.let { view ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        filterPanelView = null
    }

    private fun ensureFloatingChromeOnTop() {
        floatingBubbleView?.let { bubble ->
            val params = bubbleParams ?: return@let
            runCatching { windowManager.removeViewImmediate(bubble) }
            runCatching { windowManager.addView(bubble, params) }
        }
        if (menuWheelOpen.value) {
            dismissMenuWheelOverlay()
            showMenuWheelOverlay()
        }
        if (filterPanelOpen.value) {
            dismissFloatingFilterPanel()
            showFloatingFilterPanel()
        }
    }

    private fun renderControlWindows(profile: ControllerProfile, settings: OverlaySettings) {
        val dm = resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels
        val density = dm.density

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
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
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
            .setContentTitle("NexusPad Floating Bubble Active")
            .setContentText("Tap the floating bubble anytime to open the Menu Wheel.")
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "Stop Overlay", stopIntent)
            .build()
    }

    override fun onDestroy() {
        collectorJob?.cancel()
        dismissFloatingFilterPanel()
        dismissMenuWheelOverlay()
        clearControlWindows()
        removeScreenFilterOverlay()
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

private data class WheelActionNode(
    val angleDeg: Float,
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
private fun RadialMenuWheelOverlay(
    profileName: String,
    buttonsVisible: Boolean,
    bubbleLocked: Boolean,
    canSwitchProfile: Boolean,
    activeFilterSlot: Int,
    onDismiss: () -> Unit,
    onToggleButtons: () -> Unit,
    onSwitchProfile: () -> Unit,
    onOpenKeyboard: () -> Unit,
    onOpenFilters: () -> Unit,
    onReleaseAll: () -> Unit,
    onToggleLock: () -> Unit,
    onStopOverlay: () -> Unit,
    onOpenApp: () -> Unit
) {
    val scaleAnim = remember { Animatable(0.76f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
            )
        }
    }

    val sectorStep = 360f / 7f
    val nodes = listOf(
        WheelActionNode(
            angleDeg = -90f,
            label = if (buttonsVisible) "Hide Pad" else "Show Pad",
            icon = if (buttonsVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            tint = TacticalCyan,
            onClick = onToggleButtons
        ),
        WheelActionNode(
            angleDeg = -90f + sectorStep,
            label = "Switch",
            icon = Icons.Default.SwapHoriz,
            tint = if (canSwitchProfile) GeForceLime else Color(0xFF607087),
            enabled = canSwitchProfile,
            onClick = onSwitchProfile
        ),
        WheelActionNode(
            angleDeg = -90f + sectorStep * 2f,
            label = if (activeFilterSlot in 1..3) "Filter S$activeFilterSlot" else "Filter",
            icon = Icons.Default.Tune,
            tint = if (activeFilterSlot in 1..3) GeForceLime else TacticalCyan,
            onClick = onOpenFilters
        ),
        WheelActionNode(
            angleDeg = -90f + sectorStep * 3f,
            label = "Keyboard",
            icon = Icons.Default.Keyboard,
            tint = TacticalCyan,
            onClick = onOpenKeyboard
        ),
        WheelActionNode(
            angleDeg = -90f + sectorStep * 4f,
            label = "Stop",
            icon = Icons.Default.PowerSettingsNew,
            tint = MacroCoral,
            onClick = onStopOverlay
        ),
        WheelActionNode(
            angleDeg = -90f + sectorStep * 5f,
            label = "Unstick",
            icon = Icons.Default.RestartAlt,
            tint = TurboAmber,
            onClick = onReleaseAll
        ),
        WheelActionNode(
            angleDeg = -90f + sectorStep * 6f,
            label = if (bubbleLocked) "Unlock" else "Lock",
            icon = if (bubbleLocked) Icons.Default.LockOpen else Icons.Default.Lock,
            tint = TurboAmber,
            onClick = onToggleLock
        )
    )

    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x9905070B))
            .alpha(alphaAnim.value)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        val minDimDp = minOf(maxWidth, maxHeight)
        val wheelDiameterDp = (minDimDp * 0.80f).coerceIn(264.dp, 316.dp)
        val orbitRadiusDp = wheelDiameterDp * 0.35f
        val nodeSizeDp = 60.dp
        val hubSizeDp = 82.dp

        val orbitRadiusPx = with(density) { orbitRadiusDp.toPx() }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scaleAnim.value)
        ) {
            Box(
                modifier = Modifier
                    .size(wheelDiameterDp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val outerR = (size.minDimension / 2f) - 4.dp.toPx()
                    val innerR = with(density) { (hubSizeDp / 2f).toPx() }

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xEB151D2B), Color(0xEC0A0E16)),
                            center = c,
                            radius = outerR
                        ),
                        radius = outerR
                    )
                    drawCircle(
                        color = GeForceLime.copy(alpha = 0.45f),
                        radius = outerR,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                    drawCircle(
                        color = TacticalCyan.copy(alpha = 0.20f),
                        radius = outerR * 0.72f,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // 7 crisp sector spokes between wheel segments
                    for (i in 0 until 7) {
                        val spokeDeg = -90f + sectorStep / 2f + i * sectorStep
                        val rad = spokeDeg * PI / 180.0
                        val sx = c.x + (innerR + 6.dp.toPx()) * cos(rad).toFloat()
                        val sy = c.y + (innerR + 6.dp.toPx()) * sin(rad).toFloat()
                        val ex = c.x + (outerR - 6.dp.toPx()) * cos(rad).toFloat()
                        val ey = c.y + (outerR - 6.dp.toPx()) * sin(rad).toFloat()
                        drawLine(
                            color = Color(0xFF2B394F),
                            start = Offset(sx, sy),
                            end = Offset(ex, ey),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                // 6 Orbital Radial Menu Nodes
                nodes.forEach { node ->
                    val rad = node.angleDeg * PI / 180.0
                    val ox = (orbitRadiusPx * cos(rad)).roundToInt()
                    val oy = (orbitRadiusPx * sin(rad)).roundToInt()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .offset { IntOffset(ox, oy) }
                            .size(nodeSizeDp)
                            .clip(CircleShape)
                            .background(Color(0xCC121A26))
                            .border(1.2.dp, node.tint.copy(alpha = if (node.enabled) 0.75f else 0.25f), CircleShape)
                            .clickable(enabled = node.enabled, onClick = node.onClick)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = node.icon,
                            contentDescription = node.label,
                            tint = node.tint,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = node.label,
                            color = if (node.enabled) Color.White else Color(0xFF7B8B9E),
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Center Hub (Active profile + Close)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(hubSizeDp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF1D293B), Color(0xFF0E141E))
                            )
                        )
                        .border(2.dp, GeForceLime, CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Menu Wheel",
                        tint = GeForceLime,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = profileName.take(10),
                        color = Color.White,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                color = Color(0xE6101622),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .border(1.dp, TacticalCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenApp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = TacticalCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Open NexusPad App",
                        color = Color.White,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

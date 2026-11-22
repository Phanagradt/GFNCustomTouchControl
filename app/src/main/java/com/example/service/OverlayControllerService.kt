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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
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
import com.example.model.ControllerProfile
import com.example.model.VirtualControl
import com.example.ui.components.VirtualControlCanvasItem
import com.example.ui.theme.NexusPadTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    private var hudPillView: ComposeView? = null
    private val buttonsVisible = MutableStateFlow(true)
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
        attachHudPill()
        observeActiveProfile()
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
        val prefsRepo = PreferencesRepository(applicationContext)

        collectorJob?.cancel()
        collectorJob = serviceScope.launch {
            profileRepo.ensureSeeded()
            combine(
                profileRepo.activeProfile,
                prefsRepo.settingsFlow,
                buttonsVisible
            ) { profile, settings, visible ->
                Triple(profile, settings, visible)
            }.collect { (profile, settings, visible) ->
                clearControlWindows()
                if (visible && profile != null) {
                    renderControlWindows(profile, settings)
                }
            }
        }
    }

    private fun attachHudPill() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 48
        }

        val pill = ComposeView(this).apply {
            attachLifecycle(this)
            setContent {
                val visible by buttonsVisible.collectAsState()
                NexusPadTheme {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xE60B0E14))
                            .border(1.dp, Color(0xFF8CE000), RoundedCornerShape(24.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    params.x += dragAmount.x.roundToInt()
                                    params.y += dragAmount.y.roundToInt()
                                    runCatching { windowManager.updateViewLayout(this@apply, params) }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = "NexusPad Overlay",
                            tint = Color(0xFF8CE000),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "NEXUSPAD",
                            color = Color.White,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Icon(
                            imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (visible) "Hide Overlay Buttons" else "Show Overlay Buttons",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { buttonsVisible.value = !visible }
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop Overlay",
                            tint = Color(0xFFFF5277),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { stopSelf() }
                        )
                    }
                }
            }
        }
        hudPillView = pill
        runCatching { windowManager.addView(pill, params) }
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

            val params = WindowManager.LayoutParams(
                wPx,
                hPx,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = xPx
                y = yPx
            }

            val view = ComposeView(this).apply {
                attachLifecycle(this)
                setContent {
                    val pressedIds by InputBridge.pressedControlIds.collectAsState()
                    val toggledIds by InputBridge.toggledControlIds.collectAsState()
                    NexusPadTheme {
                        VirtualControlCanvasItem(
                            control = control,
                            notation = settings.controllerNotation,
                            globalOpacity = settings.globalOpacity,
                            hapticsEnabled = settings.hapticsEnabled,
                            isEditMode = false,
                            isSelected = false,
                            isPressed = control.id in pressedIds,
                            isToggled = control.id in toggledIds,
                            onSelect = {},
                            onDragDelta = { _, _ -> }
                        )
                    }
                }
            }
            controlViews += view
            runCatching { windowManager.addView(view, params) }
        }
    }

    private fun controlWidthDp(control: VirtualControl): Float = when (control.shape) {
        com.example.model.ControlShape.PILL -> control.sizeDp * 1.45f
        com.example.model.ControlShape.PADDLE -> control.sizeDp * 1.35f
        else -> control.sizeDp
    }

    private fun controlHeightDp(control: VirtualControl): Float = when (control.shape) {
        com.example.model.ControlShape.PILL -> control.sizeDp * 0.78f
        com.example.model.ControlShape.PADDLE -> control.sizeDp * 0.84f
        else -> control.sizeDp
    }

    private fun clearControlWindows() {
        controlViews.forEach { view ->
            runCatching { windowManager.removeViewImmediate(view) }
        }
        controlViews.clear()
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
            .setContentTitle("NexusPad Overlay Active")
            .setContentText("Virtual controller & keyboard buttons ready for GeForce NOW.")
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "Stop Overlay", stopIntent)
            .build()
    }

    override fun onDestroy() {
        collectorJob?.cancel()
        clearControlWindows()
        hudPillView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        hudPillView = null
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        lifecycleOwner.viewModelStore.clear()
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

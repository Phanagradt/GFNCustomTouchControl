package com.example.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PhysicalControllerInfo(
    val deviceId: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val isBluetoothOrExternal: Boolean
)

/**
 * Monitors real hardware Bluetooth & USB gamepads via Android's InputManager.
 * Used to automatically hide on-screen touch controls when a physical controller is active
 * and prioritize the real gamepad in WebView's HTML5 Gamepad API.
 */
object PhysicalControllerMonitor {

    private val _connectedControllers = MutableStateFlow<List<PhysicalControllerInfo>>(emptyList())
    val connectedControllers: StateFlow<List<PhysicalControllerInfo>> = _connectedControllers.asStateFlow()

    @Volatile
    private var initialized = false

    @Volatile
    private var inputManagerRef: InputManager? = null

    private val listener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = scanNow()
        override fun onInputDeviceRemoved(deviceId: Int) = scanNow()
        override fun onInputDeviceChanged(deviceId: Int) = scanNow()
    }

    fun ensureStarted(context: Context) {
        if (initialized) {
            scanNow()
            return
        }
        synchronized(this) {
            if (initialized) {
                scanNow()
                return
            }
            val appCtx = context.applicationContext
            val manager = appCtx.getSystemService(Context.INPUT_SERVICE) as? InputManager
            inputManagerRef = manager
            if (manager != null) {
                runCatching {
                    manager.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
                }
            }
            initialized = true
            scanNow()
        }
    }

    fun scanNow() {
        val ids = runCatching { InputDevice.getDeviceIds() }.getOrNull() ?: IntArray(0)
        val found = ArrayList<PhysicalControllerInfo>()
        for (id in ids) {
            val dev = runCatching { InputDevice.getDevice(id) }.getOrNull() ?: continue
            if (isPhysicalGameController(dev)) {
                val cleanName = dev.name?.trim().takeUnless { it.isNullOrBlank() } ?: "Game Controller"
                found += PhysicalControllerInfo(
                    deviceId = dev.id,
                    name = cleanName,
                    vendorId = dev.vendorId,
                    productId = dev.productId,
                    isBluetoothOrExternal = dev.isExternal
                )
            }
        }
        _connectedControllers.value = found
    }

    fun isPhysicalGameController(device: InputDevice): Boolean {
        if (device.isVirtual) return false
        val sources = device.sources
        val hasGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        val hasJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        if (!hasGamepad && !hasJoystick) return false

        val nameLower = (device.name ?: "").lowercase()
        // Exclude built-in sensors, fingerprint pads, or virtual keyboard relays that occasionally advertise joystick bits
        if (nameLower.contains("fingerprint") ||
            nameLower.contains("uinput") && nameLower.contains("fpc") ||
            nameLower.contains("accelerometer") ||
            nameLower.contains("gyroscope")
        ) {
            return false
        }

        // Verify it actually has either gamepad buttons or joystick motion ranges
        val hasMotionAxis = device.motionRanges.any { range ->
            (range.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        }
        return hasGamepad || hasMotionAxis
    }

    /**
     * Pure helper for unit tests and UI components to decide whether touch controls
     * should auto-hide given connected controllers, config, and user override.
     */
    fun shouldAutoHideTouchControls(
        connectedCount: Int,
        autoHideEnabled: Boolean,
        manualOverrideShow: Boolean
    ): Boolean = connectedCount > 0 && autoHideEnabled && !manualOverrideShow
}

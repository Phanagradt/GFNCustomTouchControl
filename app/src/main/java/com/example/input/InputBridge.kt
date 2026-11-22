package com.example.input

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import com.example.model.AxisPair
import com.example.model.BindingDomain
import com.example.model.InputBinding
import com.example.model.StickState
import com.example.model.TelemetryEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

object InputBridge {

    fun interface KeyEventSink {
        fun onDispatchKeyEvent(event: KeyEvent)
    }

    private var activeSink: KeyEventSink? = null
    private val eventIdGen = AtomicLong(1L)

    private val _isImeConnected = MutableStateFlow(false)
    val isImeConnected: StateFlow<Boolean> = _isImeConnected.asStateFlow()

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    private val _pressedControlIds = MutableStateFlow<Set<String>>(emptySet())
    val pressedControlIds: StateFlow<Set<String>> = _pressedControlIds.asStateFlow()

    private val _toggledControlIds = MutableStateFlow<Set<String>>(emptySet())
    val toggledControlIds: StateFlow<Set<String>> = _toggledControlIds.asStateFlow()

    private val _leftStick = MutableStateFlow(StickState())
    val leftStick: StateFlow<StickState> = _leftStick.asStateFlow()

    private val _rightStick = MutableStateFlow(StickState())
    val rightStick: StateFlow<StickState> = _rightStick.asStateFlow()

    private val _telemetry = MutableStateFlow<List<TelemetryEvent>>(emptyList())
    val telemetry: StateFlow<List<TelemetryEvent>> = _telemetry.asStateFlow()

    private val _activeClusterDirections = MutableStateFlow<Map<String, Set<Int>>>(emptyMap())

    fun attachImeSink(sink: KeyEventSink?) {
        activeSink = sink
        _isImeConnected.value = sink != null
    }

    fun setOverlayRunning(running: Boolean) {
        _isOverlayRunning.value = running
    }

    fun pressControl(controlId: String, sourceLabel: String, binding: InputBinding) {
        _pressedControlIds.update { it + controlId }
        dispatchBindingAction(sourceLabel, binding, isDown = true)
    }

    fun releaseControl(controlId: String, sourceLabel: String, binding: InputBinding) {
        _pressedControlIds.update { it - controlId }
        dispatchBindingAction(sourceLabel, binding, isDown = false)
    }

    fun toggleControl(controlId: String, sourceLabel: String, binding: InputBinding): Boolean {
        val willBeActive = controlId !in _toggledControlIds.value
        _toggledControlIds.update { current ->
            if (willBeActive) current + controlId else current - controlId
        }
        if (willBeActive) {
            _pressedControlIds.update { it + controlId }
        } else {
            _pressedControlIds.update { it - controlId }
        }
        dispatchBindingAction(sourceLabel, binding, isDown = willBeActive)
        return willBeActive
    }

    fun pulseTurbo(sourceLabel: String, binding: InputBinding) {
        dispatchBindingAction(sourceLabel, binding, isDown = true, actionOverride = "TURBO_HIT")
        sendKeyToSink(binding.primaryKeyCode, KeyEvent.ACTION_UP, binding.modifierMetaState, binding.domain)
    }

    fun updateAnalogStick(
        axisPair: AxisPair,
        sourceLabel: String,
        rawX: Float,
        rawY: Float,
        deadzone: Float,
        sensitivity: Float,
        active: Boolean
    ) {
        val adjX = applyDeadzoneAndCurve(rawX, deadzone, sensitivity)
        val adjY = applyDeadzoneAndCurve(rawY, deadzone, sensitivity)
        val state = StickState(x = adjX, y = adjY, active = active)

        if (axisPair == AxisPair.RIGHT_STICK) {
            _rightStick.value = state
        } else {
            _leftStick.value = state
        }

        if (active && (kotlin.math.abs(adjX) > 0.02f || kotlin.math.abs(adjY) > 0.02f)) {
            recordTelemetry(
                sourceLabel = sourceLabel,
                codeLabel = if (axisPair == AxisPair.RIGHT_STICK) "AXIS_Z/RZ" else "AXIS_X/Y",
                actionName = "AXIS_MOVE",
                detail = "X=%.2f  Y=%.2f".format(adjX, adjY),
                domain = BindingDomain.ANALOG_AXIS
            )
        } else if (!active) {
            recordTelemetry(
                sourceLabel = sourceLabel,
                codeLabel = if (axisPair == AxisPair.RIGHT_STICK) "AXIS_Z/RZ" else "AXIS_X/Y",
                actionName = "AXIS_CENTER",
                detail = "X=0.00  Y=0.00",
                domain = BindingDomain.ANALOG_AXIS
            )
        }
    }

    fun updateDirectionalCluster(
        controlId: String,
        sourceLabel: String,
        axisPair: AxisPair,
        x: Float,
        y: Float,
        active: Boolean
    ) {
        val keySet = if (!active) {
            emptySet()
        } else {
            val threshold = 0.35f
            val codes = mutableSetOf<Int>()
            when (axisPair) {
                AxisPair.WASD_KEYS -> {
                    if (y < -threshold) codes += KeyEvent.KEYCODE_W
                    if (y > threshold) codes += KeyEvent.KEYCODE_S
                    if (x < -threshold) codes += KeyEvent.KEYCODE_A
                    if (x > threshold) codes += KeyEvent.KEYCODE_D
                }
                else -> {
                    if (y < -threshold) codes += KeyEvent.KEYCODE_DPAD_UP
                    if (y > threshold) codes += KeyEvent.KEYCODE_DPAD_DOWN
                    if (x < -threshold) codes += KeyEvent.KEYCODE_DPAD_LEFT
                    if (x > threshold) codes += KeyEvent.KEYCODE_DPAD_RIGHT
                }
            }
            codes
        }

        val previous = _activeClusterDirections.value[controlId] ?: emptySet()
        val released = previous - keySet
        val pressed = keySet - previous

        val domain = if (axisPair == AxisPair.WASD_KEYS || axisPair == AxisPair.ARROW_KEYS) {
            BindingDomain.KEYBOARD
        } else {
            BindingDomain.CONTROLLER
        }

        released.forEach { code ->
            sendKeyToSink(code, KeyEvent.ACTION_UP, 0, domain)
            recordTelemetry(
                sourceLabel = sourceLabel,
                codeLabel = safeKeyCodeName(code),
                actionName = "KEY_UP",
                detail = "Cluster release",
                domain = domain
            )
        }
        pressed.forEach { code ->
            sendKeyToSink(code, KeyEvent.ACTION_DOWN, 0, domain)
            recordTelemetry(
                sourceLabel = sourceLabel,
                codeLabel = safeKeyCodeName(code),
                actionName = "KEY_DOWN",
                detail = "Cluster active",
                domain = domain
            )
        }

        _activeClusterDirections.update { it + (controlId to keySet) }
    }

    fun recordPhysicalInput(keyCode: Int, actionName: String, detail: String, domain: BindingDomain) {
        recordTelemetry("Hardware", safeKeyCodeName(keyCode), actionName, detail, domain)
    }

    fun clearTelemetry() {
        _telemetry.value = emptyList()
    }

    private fun dispatchBindingAction(
        sourceLabel: String,
        binding: InputBinding,
        isDown: Boolean,
        actionOverride: String? = null
    ) {
        val action = if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
        if (binding.secondaryKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            if (isDown) {
                sendKeyToSink(binding.secondaryKeyCode, KeyEvent.ACTION_DOWN, 0, binding.domain)
                sendKeyToSink(binding.primaryKeyCode, KeyEvent.ACTION_DOWN, binding.modifierMetaState, binding.domain)
            } else {
                sendKeyToSink(binding.primaryKeyCode, KeyEvent.ACTION_UP, binding.modifierMetaState, binding.domain)
                sendKeyToSink(binding.secondaryKeyCode, KeyEvent.ACTION_UP, 0, binding.domain)
            }
        } else if (binding.primaryKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            sendKeyToSink(binding.primaryKeyCode, action, binding.modifierMetaState, binding.domain)
        }

        val keyDesc = if (binding.secondaryKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            "${safeKeyCodeName(binding.secondaryKeyCode)} + ${safeKeyCodeName(binding.primaryKeyCode)}"
        } else {
            safeKeyCodeName(binding.primaryKeyCode)
        }

        recordTelemetry(
            sourceLabel = sourceLabel,
            codeLabel = binding.codeLabel,
            actionName = actionOverride ?: if (isDown) "KEY_DOWN" else "KEY_UP",
            detail = keyDesc,
            domain = binding.domain
        )
    }

    private fun safeKeyCodeName(keyCode: Int): String =
        runCatching { KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_") }
            .getOrElse {
                when (keyCode) {
                    KeyEvent.KEYCODE_W -> "W"
                    KeyEvent.KEYCODE_A -> "A"
                    KeyEvent.KEYCODE_S -> "S"
                    KeyEvent.KEYCODE_D -> "D"
                    KeyEvent.KEYCODE_BUTTON_L2 -> "BUTTON_L2"
                    KeyEvent.KEYCODE_BUTTON_R2 -> "BUTTON_R2"
                    else -> "KEY_$keyCode"
                }
            }

    private fun sendKeyToSink(
        keyCode: Int,
        action: Int,
        metaState: Int,
        domain: BindingDomain
    ) {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return
        val sink = activeSink ?: return
        val now = SystemClock.uptimeMillis()
        val source = if (domain == BindingDomain.CONTROLLER) {
            InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD
        } else {
            InputDevice.SOURCE_KEYBOARD
        }
        val event = KeyEvent(
            now,
            now,
            action,
            keyCode,
            0,
            metaState,
            -1,
            0,
            KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
            source
        )
        sink.onDispatchKeyEvent(event)
    }

    private fun recordTelemetry(
        sourceLabel: String,
        codeLabel: String,
        actionName: String,
        detail: String,
        domain: BindingDomain
    ) {
        val entry = TelemetryEvent(
            id = eventIdGen.getAndIncrement(),
            timestampMs = System.currentTimeMillis(),
            sourceLabel = sourceLabel,
            codeLabel = codeLabel,
            actionName = actionName,
            detail = detail,
            domain = domain
        )
        _telemetry.update { list -> (listOf(entry) + list).take(40) }
    }

    private fun applyDeadzoneAndCurve(value: Float, deadzone: Float, sensitivity: Float): Float {
        val abs = kotlin.math.abs(value)
        if (abs <= deadzone) return 0f
        val normalized = ((abs - deadzone) / (1f - deadzone)).coerceIn(0f, 1f)
        val curved = (normalized * sensitivity).coerceIn(0f, 1f)
        return if (value < 0f) -curved else curved
    }
}

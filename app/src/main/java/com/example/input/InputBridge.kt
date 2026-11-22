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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InputBridge {

    fun interface KeyEventSink {
        fun onDispatchKeyEvent(event: KeyEvent)
    }

    private var activeSink: KeyEventSink? = null
    private val eventIdGen = AtomicLong(1L)
    private val activeDownTimes = ConcurrentHashMap<Int, Long>()
    private val activeModifiers = ConcurrentHashMap<Int, Int>()

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
        sendKeyToSink(binding.primaryKeyCode, KeyEvent.ACTION_UP, 0, binding.domain)
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
        val threshold = 0.35f
        val keySet = if (!active) {
            emptySet()
        } else {
            val codes = mutableSetOf<Int>()
            when (axisPair) {
                AxisPair.WASD_KEYS -> {
                    if (y < -threshold) codes += KeyEvent.KEYCODE_W
                    if (y > threshold) codes += KeyEvent.KEYCODE_S
                    if (x < -threshold) codes += KeyEvent.KEYCODE_A
                    if (x > threshold) codes += KeyEvent.KEYCODE_D
                }
                AxisPair.TOUCHPAD_QE -> {
                    if (x < -threshold) codes += KeyEvent.KEYCODE_Q
                    if (x > threshold) codes += KeyEvent.KEYCODE_E
                    if (y < -threshold) codes += KeyEvent.KEYCODE_DPAD_UP
                    if (y > threshold) codes += KeyEvent.KEYCODE_DPAD_DOWN
                }
                AxisPair.TOUCHPAD_IJKL -> {
                    if (y < -threshold) codes += KeyEvent.KEYCODE_I
                    if (y > threshold) codes += KeyEvent.KEYCODE_K
                    if (x < -threshold) codes += KeyEvent.KEYCODE_J
                    if (x > threshold) codes += KeyEvent.KEYCODE_L
                }
                AxisPair.TOUCHPAD_DIRECT_PASS -> Unit
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

        val domain = when (axisPair) {
            AxisPair.DPAD_4WAY, AxisPair.TOUCHPAD_DPAD -> BindingDomain.CONTROLLER
            else -> BindingDomain.KEYBOARD
        }

        pressed.forEach { code ->
            sendKeyToSink(code, KeyEvent.ACTION_DOWN, 0, domain)
            recordTelemetry(
                sourceLabel = sourceLabel,
                codeLabel = safeKeyCodeName(code),
                actionName = "KEY_DOWN",
                detail = "scan=${resolveLinuxScanCode(code, domain)}",
                domain = domain
            )
        }
        released.forEach { code ->
            sendKeyToSink(code, KeyEvent.ACTION_UP, 0, domain)
            recordTelemetry(
                sourceLabel = sourceLabel,
                codeLabel = safeKeyCodeName(code),
                actionName = "KEY_UP",
                detail = "scan=${resolveLinuxScanCode(code, domain)}",
                domain = domain
            )
        }

        _activeClusterDirections.update { it + (controlId to keySet) }
    }

    fun updateTouchpadLook(
        controlId: String,
        sourceLabel: String,
        axisPair: AxisPair,
        normX: Float,
        normY: Float,
        deadzone: Float,
        invertY: Boolean,
        active: Boolean
    ) {
        val effY = if (invertY) -normY else normY
        val thresh = (deadzone * 2.2f).coerceIn(0.14f, 0.45f)
        val filteredX = if (!active || kotlin.math.abs(normX) < thresh) 0f else normX
        val filteredY = if (!active || kotlin.math.abs(effY) < thresh) 0f else effY

        _rightStick.value = StickState(
            x = filteredX.coerceIn(-1f, 1f),
            y = filteredY.coerceIn(-1f, 1f),
            active = active
        )

        if (axisPair != AxisPair.TOUCHPAD_DIRECT_PASS) {
            // Map continuous swipe vector cleanly to held directional look keys with threshold 0.35
            val scaledX = if (filteredX == 0f) 0f else kotlin.math.sign(filteredX) * 0.8f
            val scaledY = if (filteredY == 0f) 0f else kotlin.math.sign(filteredY) * 0.8f
            updateDirectionalCluster(
                controlId = controlId,
                sourceLabel = sourceLabel,
                axisPair = axisPair,
                x = scaledX,
                y = scaledY,
                active = active
            )
        }
    }

    fun recordPhysicalInput(keyCode: Int, actionName: String, detail: String, domain: BindingDomain) {
        recordTelemetry("Hardware", safeKeyCodeName(keyCode), actionName, detail, domain)
    }

    fun clearTelemetry() {
        _telemetry.value = emptyList()
    }

    fun releaseAll() {
        val heldKeys = activeDownTimes.keys.toList()
        val nonModifierKeys = heldKeys.filter { modifierBitsForKeyCode(it) == 0 }
        val modifierKeys = (heldKeys.filter { modifierBitsForKeyCode(it) != 0 } + activeModifiers.keys).distinct()
        nonModifierKeys.forEach { code ->
            sendKeyToSink(code, KeyEvent.ACTION_UP, 0, effectiveDomainForKey(code, BindingDomain.KEYBOARD))
        }
        modifierKeys.forEach { code ->
            sendKeyToSink(code, KeyEvent.ACTION_UP, 0, effectiveDomainForKey(code, BindingDomain.KEYBOARD))
        }
        activeModifiers.clear()
        activeDownTimes.clear()
        _pressedControlIds.value = emptySet()
        _toggledControlIds.value = emptySet()
        _activeClusterDirections.value = emptyMap()
        _leftStick.value = StickState()
        _rightStick.value = StickState()
    }

    private fun dispatchBindingAction(
        sourceLabel: String,
        binding: InputBinding,
        isDown: Boolean,
        actionOverride: String? = null
    ) {
        val action = if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
        if (binding.secondaryKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            val secDomain = effectiveDomainForKey(binding.secondaryKeyCode, binding.domain)
            val priDomain = effectiveDomainForKey(binding.primaryKeyCode, binding.domain)
            if (isDown) {
                sendKeyToSink(binding.secondaryKeyCode, KeyEvent.ACTION_DOWN, binding.modifierMetaState, secDomain)
                sendKeyToSink(binding.primaryKeyCode, KeyEvent.ACTION_DOWN, binding.modifierMetaState, priDomain)
            } else {
                sendKeyToSink(binding.primaryKeyCode, KeyEvent.ACTION_UP, 0, priDomain)
                sendKeyToSink(binding.secondaryKeyCode, KeyEvent.ACTION_UP, 0, secDomain)
            }
        } else if (binding.primaryKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            val extraMeta = if (isDown) binding.modifierMetaState else 0
            sendKeyToSink(binding.primaryKeyCode, action, extraMeta, effectiveDomainForKey(binding.primaryKeyCode, binding.domain))
        }

        val keyDesc = if (binding.secondaryKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            "${safeKeyCodeName(binding.secondaryKeyCode)}+${safeKeyCodeName(binding.primaryKeyCode)}"
        } else {
            "${safeKeyCodeName(binding.primaryKeyCode)} (scan=${resolveLinuxScanCode(binding.primaryKeyCode, binding.domain)})"
        }

        recordTelemetry(
            sourceLabel = sourceLabel,
            codeLabel = binding.codeLabel,
            actionName = actionOverride ?: if (isDown) "KEY_DOWN" else "KEY_UP",
            detail = keyDesc,
            domain = binding.domain
        )
    }

    private fun modifierBitsForKeyCode(keyCode: Int): Int = when (keyCode) {
        KeyEvent.KEYCODE_SHIFT_LEFT -> KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        KeyEvent.KEYCODE_SHIFT_RIGHT -> KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_RIGHT_ON
        KeyEvent.KEYCODE_CTRL_LEFT -> KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        KeyEvent.KEYCODE_CTRL_RIGHT -> KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_RIGHT_ON
        KeyEvent.KEYCODE_ALT_LEFT -> KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        KeyEvent.KEYCODE_ALT_RIGHT -> KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON
        else -> 0
    }

    private fun currentModifierMetaState(): Int =
        activeModifiers.values.fold(0) { acc, mask -> acc or mask }

    fun currentEffectiveMetaState(extraMeta: Int = 0): Int =
        currentModifierMetaState() or extraMeta

    private fun effectiveDomainForKey(keyCode: Int, fallback: BindingDomain): BindingDomain = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_1,
        KeyEvent.KEYCODE_BUTTON_2,
        KeyEvent.KEYCODE_BUTTON_3,
        KeyEvent.KEYCODE_BUTTON_4,
        KeyEvent.KEYCODE_BUTTON_5 -> BindingDomain.MOUSE
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_C,
        KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_BUTTON_Z,
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_L2,
        KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_BUTTON_SELECT,
        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_BUTTON_MODE,
        KeyEvent.KEYCODE_BUTTON_THUMBL,
        KeyEvent.KEYCODE_BUTTON_THUMBR -> BindingDomain.CONTROLLER
        else -> if (fallback == BindingDomain.COMBO) BindingDomain.KEYBOARD else fallback
    }

    private fun safeKeyCodeName(keyCode: Int): String =
        runCatching { KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_") }
            .getOrElse {
                when (keyCode) {
                    KeyEvent.KEYCODE_W -> "W"
                    KeyEvent.KEYCODE_A -> "A"
                    KeyEvent.KEYCODE_S -> "S"
                    KeyEvent.KEYCODE_D -> "D"
                    KeyEvent.KEYCODE_Q -> "Q"
                    KeyEvent.KEYCODE_E -> "E"
                    KeyEvent.KEYCODE_I -> "I"
                    KeyEvent.KEYCODE_J -> "J"
                    KeyEvent.KEYCODE_K -> "K"
                    KeyEvent.KEYCODE_L -> "L"
                    KeyEvent.KEYCODE_DPAD_UP -> "DPAD_UP"
                    KeyEvent.KEYCODE_DPAD_DOWN -> "DPAD_DOWN"
                    KeyEvent.KEYCODE_DPAD_LEFT -> "DPAD_LEFT"
                    KeyEvent.KEYCODE_DPAD_RIGHT -> "DPAD_RIGHT"
                    KeyEvent.KEYCODE_BUTTON_1 -> "M1"
                    KeyEvent.KEYCODE_BUTTON_2 -> "M2"
                    KeyEvent.KEYCODE_BUTTON_3 -> "M3"
                    KeyEvent.KEYCODE_BUTTON_4 -> "M4"
                    KeyEvent.KEYCODE_BUTTON_5 -> "M5"
                    KeyEvent.KEYCODE_BUTTON_L2 -> "BUTTON_L2"
                    KeyEvent.KEYCODE_BUTTON_R2 -> "BUTTON_R2"
                    else -> "KEY_$keyCode"
                }
            }

    private fun safeUptimeMillis(): Long =
        runCatching { SystemClock.uptimeMillis() }.getOrElse { System.currentTimeMillis() }

    private fun sendKeyToSink(
        keyCode: Int,
        action: Int,
        metaState: Int,
        domain: BindingDomain
    ) {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return
        val modBits = modifierBitsForKeyCode(keyCode)
        if (modBits != 0) {
            if (action == KeyEvent.ACTION_DOWN) {
                activeModifiers[keyCode] = modBits
            } else {
                activeModifiers.remove(keyCode)
            }
        }
        val combinedMetaState = currentEffectiveMetaState(metaState)

        val sink = activeSink ?: return
        val now = safeUptimeMillis()
        val downTime = if (action == KeyEvent.ACTION_DOWN) {
            activeDownTimes[keyCode] = now
            now
        } else {
            activeDownTimes.remove(keyCode) ?: now
        }
        val scanCode = resolveLinuxScanCode(keyCode, domain)
        val source = when (domain) {
            BindingDomain.CONTROLLER -> InputDevice.SOURCE_GAMEPAD
            BindingDomain.MOUSE -> InputDevice.SOURCE_MOUSE or InputDevice.SOURCE_KEYBOARD
            else -> InputDevice.SOURCE_KEYBOARD
        }
        // Keep touch mode intact without setting FLAG_SOFT_KEYBOARD, which causes cloud streaming
        // clients (like GeForce NOW) to ignore hardware scanCode and treat KEYCODE_A(29) as CTRL(29).
        val flags = effectiveDispatchFlags()
        val event = KeyEvent(
            downTime,
            now,
            action,
            keyCode,
            0,
            combinedMetaState,
            -1,
            scanCode,
            flags,
            source
        )
        sink.onDispatchKeyEvent(event)
    }

    fun effectiveDispatchFlags(): Int = KeyEvent.FLAG_KEEP_TOUCH_MODE

    /**
     * Maps Android KeyEvent keycodes to exact Linux evdev scan codes (uapi/linux/input-event-codes.h)
     * so cloud streaming clients like GeForce NOW do not fall back to interpreting Android keyCode
     * integers as raw Linux scan codes (e.g. Android KEYCODE_A=29 vs Linux KEY_LEFTCTRL=29).
     */
    fun resolveLinuxScanCode(keyCode: Int, domain: BindingDomain): Int = when (keyCode) {
        KeyEvent.KEYCODE_ESCAPE -> 1
        KeyEvent.KEYCODE_1 -> 2
        KeyEvent.KEYCODE_2 -> 3
        KeyEvent.KEYCODE_3 -> 4
        KeyEvent.KEYCODE_4 -> 5
        KeyEvent.KEYCODE_5 -> 6
        KeyEvent.KEYCODE_6 -> 7
        KeyEvent.KEYCODE_7 -> 8
        KeyEvent.KEYCODE_8 -> 9
        KeyEvent.KEYCODE_9 -> 10
        KeyEvent.KEYCODE_0 -> 11
        KeyEvent.KEYCODE_MINUS -> 12
        KeyEvent.KEYCODE_EQUALS -> 13
        KeyEvent.KEYCODE_DEL -> 14
        KeyEvent.KEYCODE_TAB -> 15
        KeyEvent.KEYCODE_Q -> 16
        KeyEvent.KEYCODE_W -> 17
        KeyEvent.KEYCODE_E -> 18
        KeyEvent.KEYCODE_R -> 19
        KeyEvent.KEYCODE_T -> 20
        KeyEvent.KEYCODE_Y -> 21
        KeyEvent.KEYCODE_U -> 22
        KeyEvent.KEYCODE_I -> 23
        KeyEvent.KEYCODE_O -> 24
        KeyEvent.KEYCODE_P -> 25
        KeyEvent.KEYCODE_LEFT_BRACKET -> 26
        KeyEvent.KEYCODE_RIGHT_BRACKET -> 27
        KeyEvent.KEYCODE_ENTER -> 28
        KeyEvent.KEYCODE_CTRL_LEFT -> 29
        KeyEvent.KEYCODE_A -> 30
        KeyEvent.KEYCODE_S -> 31
        KeyEvent.KEYCODE_D -> 32
        KeyEvent.KEYCODE_F -> 33
        KeyEvent.KEYCODE_G -> 34
        KeyEvent.KEYCODE_H -> 35
        KeyEvent.KEYCODE_J -> 36
        KeyEvent.KEYCODE_K -> 37
        KeyEvent.KEYCODE_L -> 38
        KeyEvent.KEYCODE_SEMICOLON -> 39
        KeyEvent.KEYCODE_APOSTROPHE -> 40
        KeyEvent.KEYCODE_GRAVE -> 41
        KeyEvent.KEYCODE_SHIFT_LEFT -> 42
        KeyEvent.KEYCODE_BACKSLASH -> 43
        KeyEvent.KEYCODE_Z -> 44
        KeyEvent.KEYCODE_X -> 45
        KeyEvent.KEYCODE_C -> 46
        KeyEvent.KEYCODE_V -> 47
        KeyEvent.KEYCODE_B -> 48
        KeyEvent.KEYCODE_N -> 49
        KeyEvent.KEYCODE_M -> 50
        KeyEvent.KEYCODE_COMMA -> 51
        KeyEvent.KEYCODE_PERIOD -> 52
        KeyEvent.KEYCODE_SLASH -> 53
        KeyEvent.KEYCODE_SHIFT_RIGHT -> 54
        KeyEvent.KEYCODE_ALT_LEFT -> 56
        KeyEvent.KEYCODE_SPACE -> 57
        KeyEvent.KEYCODE_CAPS_LOCK -> 58
        KeyEvent.KEYCODE_F1 -> 59
        KeyEvent.KEYCODE_F2 -> 60
        KeyEvent.KEYCODE_F3 -> 61
        KeyEvent.KEYCODE_F4 -> 62
        KeyEvent.KEYCODE_F5 -> 63
        KeyEvent.KEYCODE_F6 -> 64
        KeyEvent.KEYCODE_F7 -> 65
        KeyEvent.KEYCODE_F8 -> 66
        KeyEvent.KEYCODE_F9 -> 67
        KeyEvent.KEYCODE_F10 -> 68
        KeyEvent.KEYCODE_F11 -> 87
        KeyEvent.KEYCODE_F12 -> 88
        KeyEvent.KEYCODE_CTRL_RIGHT -> 97
        KeyEvent.KEYCODE_ALT_RIGHT -> 100
        KeyEvent.KEYCODE_DPAD_UP -> if (domain == BindingDomain.CONTROLLER) 544 else 103
        KeyEvent.KEYCODE_DPAD_LEFT -> if (domain == BindingDomain.CONTROLLER) 546 else 105
        KeyEvent.KEYCODE_DPAD_RIGHT -> if (domain == BindingDomain.CONTROLLER) 547 else 106
        KeyEvent.KEYCODE_DPAD_DOWN -> if (domain == BindingDomain.CONTROLLER) 545 else 108
        KeyEvent.KEYCODE_BUTTON_A -> 304
        KeyEvent.KEYCODE_BUTTON_B -> 305
        KeyEvent.KEYCODE_BUTTON_C -> 306
        KeyEvent.KEYCODE_BUTTON_X -> 307
        KeyEvent.KEYCODE_BUTTON_Y -> 308
        KeyEvent.KEYCODE_BUTTON_Z -> 309
        KeyEvent.KEYCODE_BUTTON_L1 -> 310
        KeyEvent.KEYCODE_BUTTON_R1 -> 311
        KeyEvent.KEYCODE_BUTTON_L2 -> 312
        KeyEvent.KEYCODE_BUTTON_R2 -> 313
        KeyEvent.KEYCODE_BUTTON_SELECT -> 314
        KeyEvent.KEYCODE_BUTTON_START -> 315
        KeyEvent.KEYCODE_BUTTON_MODE -> 316
        KeyEvent.KEYCODE_BUTTON_THUMBL -> 317
        KeyEvent.KEYCODE_BUTTON_THUMBR -> 318
        KeyEvent.KEYCODE_BUTTON_1 -> 272 // BTN_LEFT (M1)
        KeyEvent.KEYCODE_BUTTON_2 -> 273 // BTN_RIGHT (M2)
        KeyEvent.KEYCODE_BUTTON_3 -> 274 // BTN_MIDDLE (M3 / MMB)
        KeyEvent.KEYCODE_BUTTON_4 -> 275 // BTN_SIDE (M4)
        KeyEvent.KEYCODE_BUTTON_5 -> 276 // BTN_EXTRA (M5)
        else -> 0
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

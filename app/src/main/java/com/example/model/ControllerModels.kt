package com.example.model

import android.view.KeyEvent
import com.squareup.moshi.JsonClass

enum class BindingDomain(val displayName: String) {
    CONTROLLER("Controller"),
    KEYBOARD("Keyboard"),
    COMBO("Combo Macro"),
    ANALOG_AXIS("Analog Axis")
}

enum class ControlType(val displayName: String) {
    BUTTON("Action Button"),
    ANALOG_STICK("Analog Stick"),
    DPAD_CLUSTER("D-Pad"),
    WASD_CLUSTER("WASD Pad"),
    SHOULDER_PADDLE("Shoulder Paddle")
}

enum class ControlShape(val displayName: String) {
    CIRCLE("Circle"),
    PILL("Pill"),
    HEX("Tactical Hex"),
    PADDLE("Paddle"),
    RING("Stick Ring"),
    CROSS("D-Pad Cross")
}

enum class TriggerBehavior(val displayName: String, val subtitle: String) {
    HOLD("Hold", "Pressed while finger touches"),
    TOGGLE("Toggle", "Tap to latch on/off"),
    TURBO("Turbo", "Auto-repeat rapid fire")
}

enum class ControlAccent(val displayName: String, val hex: Long) {
    LIME("GeForce Lime", 0xFF8CE000),
    CYAN("Tactical Cyan", 0xFF00E5FF),
    AMBER("Turbo Amber", 0xFFFFB300),
    CORAL("Combat Coral", 0xFFFF5277),
    VIOLET("Macro Violet", 0xFFB388FF),
    SLATE("Stealth Slate", 0xFF9FB0C7)
}

enum class AxisPair(val displayName: String) {
    NONE("None"),
    LEFT_STICK("Left Stick (AXIS_X / AXIS_Y)"),
    RIGHT_STICK("Right Stick (AXIS_Z / AXIS_RZ)"),
    WASD_KEYS("Keyboard W / A / S / D"),
    ARROW_KEYS("Keyboard Arrow Keys"),
    DPAD_4WAY("Gamepad D-Pad 4-Way")
}

enum class GameGenre(val displayName: String) {
    FPS("FPS / Shooter"),
    ACTION_RPG("Action RPG"),
    MMO_MOBA("MMO / Strategy"),
    RACING("Racing / Sim"),
    CUSTOM("Custom Layout")
}

enum class ControllerNotation(val displayName: String) {
    XBOX("Xbox (A / B / X / Y)"),
    PLAYSTATION("PlayStation (✕ / ○ / □ / △)"),
    PC_HYBRID("PC Hybrid (Keycodes)")
}

@JsonClass(generateAdapter = true)
data class InputBinding(
    val id: String,
    val domain: BindingDomain,
    val primaryKeyCode: Int,
    val secondaryKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN,
    val modifierMetaState: Int = 0,
    val codeLabel: String,
    val commandTitle: String,
    val axisPair: AxisPair = AxisPair.NONE
)

@JsonClass(generateAdapter = true)
data class VirtualControl(
    val id: String,
    val label: String,
    val controlType: ControlType,
    val shape: ControlShape,
    val xPercent: Float,
    val yPercent: Float,
    val sizeDp: Float = 64f,
    val opacity: Float = 0.82f,
    val accent: ControlAccent = ControlAccent.LIME,
    val behavior: TriggerBehavior = TriggerBehavior.HOLD,
    val turboRateHz: Int = 10,
    val deadzone: Float = 0.12f,
    val sensitivity: Float = 1.0f,
    val hapticOnPress: Boolean = true,
    val binding: InputBinding
)

@JsonClass(generateAdapter = true)
data class ControllerProfile(
    val id: String,
    val name: String,
    val gameTitle: String,
    val genre: GameGenre,
    val controls: List<VirtualControl>,
    val globalOpacity: Float = 0.85f,
    val gridSnap: Boolean = true,
    val isActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class TelemetryEvent(
    val id: Long,
    val timestampMs: Long,
    val sourceLabel: String,
    val codeLabel: String,
    val actionName: String,
    val detail: String,
    val domain: BindingDomain
)

data class StickState(
    val x: Float = 0f,
    val y: Float = 0f,
    val active: Boolean = false
)

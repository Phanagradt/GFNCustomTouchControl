package com.example.model

import com.squareup.moshi.JsonClass

/**
 * Controls when hardware gyroscope tilt aiming is active during gameplay.
 */
enum class GyroActivationMode {
    ALWAYS_ON,
    HOLD_ADS
}

/**
 * Hardware gyroscope aiming (tilt-to-aim) configuration.
 * Uses Android Sensor.TYPE_GYROSCOPE with display-rotation axis remapping.
 */
@JsonClass(generateAdapter = true)
data class GyroAimConfig(
    val enabled: Boolean = false,
    val activationMode: GyroActivationMode = GyroActivationMode.ALWAYS_ON,
    val sensitivity: Float = 1.2f,
    val deadzoneRadPerSec: Float = 0.04f,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val smoothing: Float = 0.35f
) {
    fun clampedSensitivity(): Float = sensitivity.coerceIn(0.2f, 4.0f)
    fun clampedDeadzone(): Float = deadzoneRadPerSec.coerceIn(0.0f, 0.30f)
    fun clampedSmoothing(): Float = smoothing.coerceIn(0.0f, 0.85f)
}

/**
 * Draggable GeForce-style Performance HUD overlay configuration (FPS / Battery / Temp / RAM).
 */
@JsonClass(generateAdapter = true)
data class PerfHudConfig(
    val enabled: Boolean = false,
    val showFps: Boolean = true,
    val showBattery: Boolean = true,
    val showTemp: Boolean = true,
    val showRam: Boolean = false,
    val opacity: Float = 0.86f
) {
    fun clampedOpacity(): Float = opacity.coerceIn(0.35f, 1.0f)
}

package com.example.input

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import com.example.model.GyroActivationMode
import com.example.model.GyroAimConfig
import kotlin.math.abs

/**
 * Hardware Gyroscope Aiming (Tilt-to-Aim) engine backed by Android Sensor.TYPE_GYROSCOPE.
 * Automatically remaps yaw/pitch angular velocities across Portrait and Landscape rotations,
 * applies deadzone + exponential low-pass smoothing, and optionally gates aiming to Hold-ADS.
 */
class GyroAimEngine(
    private val context: Context,
    private val onAimDelta: (dxPx: Float, dyPx: Float) -> Unit = { dx, dy ->
        InputBridge.dispatchGyroAimDelta(dx, dy)
    },
    private val onIdleReset: () -> Unit = { InputBridge.resetGyroAim() }
) : SensorEventListener {

    private val sensorManager: SensorManager? =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val hasHardwareGyro: Boolean
        get() = gyroSensor != null

    @Volatile
    private var config: GyroAimConfig = GyroAimConfig()

    @Volatile
    private var pausedForEdit: Boolean = false

    private var registered = false
    private var lastTimestampNs = 0L
    private var smoothDx = 0f
    private var smoothDy = 0f
    private var wasEmitting = false

    fun updateConfig(newConfig: GyroAimConfig, pauseForEdit: Boolean = pausedForEdit) {
        config = newConfig
        pausedForEdit = pauseForEdit
        if (config.enabled && !pausedForEdit) {
            start()
        } else {
            stop()
        }
    }

    fun start() {
        val sensor = gyroSensor ?: return
        if (registered) return
        lastTimestampNs = 0L
        smoothDx = 0f
        smoothDy = 0f
        wasEmitting = false
        registered = sensorManager?.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_GAME
        ) == true
    }

    fun stop() {
        if (registered) {
            sensorManager?.unregisterListener(this)
            registered = false
        }
        lastTimestampNs = 0L
        smoothDx = 0f
        smoothDy = 0f
        if (wasEmitting) {
            wasEmitting = false
            onIdleReset()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!config.enabled || pausedForEdit) return
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE || event.values.size < 3) return

        val ts = event.timestamp
        if (lastTimestampNs == 0L) {
            lastTimestampNs = ts
            return
        }
        val dtSec = ((ts - lastTimestampNs) / 1_000_000_000.0).toFloat()
        lastTimestampNs = ts
        if (dtSec <= 0f || dtSec > 0.2f) return

        if (config.activationMode == GyroActivationMode.HOLD_ADS && !InputBridge.isAdsActive.value) {
            if (wasEmitting) {
                smoothDx = 0f
                smoothDy = 0f
                wasEmitting = false
                onIdleReset()
            }
            return
        }

        val rotation = currentDisplayRotation()
        val (rawDx, rawDy) = computeAimDeltaPx(
            gyroX = event.values[0],
            gyroY = event.values[1],
            gyroZ = event.values[2],
            dtSec = dtSec,
            displayRotation = rotation,
            config = config
        )

        val alpha = (1f - config.clampedSmoothing()).coerceIn(0.15f, 1.0f)
        smoothDx = smoothDx * (1f - alpha) + rawDx * alpha
        smoothDy = smoothDy * (1f - alpha) + rawDy * alpha

        if (abs(smoothDx) >= 0.08f || abs(smoothDy) >= 0.08f) {
            wasEmitting = true
            onAimDelta(smoothDx, smoothDy)
        } else if (wasEmitting) {
            smoothDx = 0f
            smoothDy = 0f
            wasEmitting = false
            onIdleReset()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    @Suppress("DEPRECATION")
    private fun currentDisplayRotation(): Int {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.rotation
            } else {
                null
            } ?: (context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                ?.defaultDisplay?.rotation
            ?: Surface.ROTATION_90
        }.getOrDefault(Surface.ROTATION_90)
    }

    companion object {
        // Base pixel gain per radian of angular turn at 1.0x sensitivity
        private const val BASE_PIXELS_PER_RADIAN = 420f
        private const val MAX_DELTA_PER_FRAME_PX = 90f

        fun hasGyroscope(context: Context): Boolean {
            val sm = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            return sm?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        }

        /**
         * Pure testable mapping from device-frame gyroscope rad/s into screen-space (dxPx, dyPx).
         * Remaps yaw (horizontal look) & pitch (vertical look) cleanly for all 4 display rotations.
         */
        fun computeAimDeltaPx(
            gyroX: Float,
            gyroY: Float,
            gyroZ: Float,
            dtSec: Float,
            displayRotation: Int,
            config: GyroAimConfig
        ): Pair<Float, Float> {
            if (dtSec <= 0f) return 0f to 0f

            // Remap sensor angular velocities (rad/s) to screen-space yaw (turn right = +) & pitch (look down = +)
            val (yawRate, pitchRate) = when (displayRotation) {
                Surface.ROTATION_90 -> {
                    // Landscape left (counter-clockwise 90°): turning phone right rotates around -X / -Z
                    val yaw = -(gyroX * 0.85f + gyroZ * 0.45f)
                    val pitch = gyroY
                    yaw to pitch
                }
                Surface.ROTATION_270 -> {
                    // Landscape right (clockwise 90°): axes mirrored relative to ROTATION_90
                    val yaw = (gyroX * 0.85f - gyroZ * 0.45f)
                    val pitch = -gyroY
                    yaw to pitch
                }
                Surface.ROTATION_180 -> {
                    val yaw = gyroY
                    val pitch = gyroX
                    yaw to pitch
                }
                else -> {
                    // Portrait (ROTATION_0)
                    val yaw = -(gyroY * 0.85f + gyroZ * 0.45f)
                    val pitch = -gyroX
                    yaw to pitch
                }
            }

            val deadzone = config.clampedDeadzone()
            val cleanYaw = applyDeadzone(yawRate, deadzone)
            val cleanPitch = applyDeadzone(pitchRate, deadzone)

            val gain = BASE_PIXELS_PER_RADIAN * config.clampedSensitivity() * dtSec
            var dx = (cleanYaw * gain).coerceIn(-MAX_DELTA_PER_FRAME_PX, MAX_DELTA_PER_FRAME_PX)
            var dy = (cleanPitch * gain).coerceIn(-MAX_DELTA_PER_FRAME_PX, MAX_DELTA_PER_FRAME_PX)

            if (config.invertX) dx = -dx
            if (config.invertY) dy = -dy
            return dx to dy
        }

        private fun applyDeadzone(value: Float, deadzone: Float): Float {
            val mag = abs(value)
            if (mag <= deadzone) return 0f
            return if (value > 0f) mag - deadzone else -(mag - deadzone)
        }
    }
}

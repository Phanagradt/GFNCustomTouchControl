package com.example.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class PerfStatsSnapshot(
    val fps: Int = 0,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val batteryTempC: Float = 0f,
    val usedRamPercent: Int = 0
)

/**
 * Lightweight, real-telemetry Performance HUD monitor.
 * Measures actual UI/display FPS via Android Choreographer.FrameCallback,
 * live battery % / charging state / battery temperature (°C) via ACTION_BATTERY_CHANGED,
 * and system memory load via ActivityManager.getMemoryInfo.
 *
 * Zero overhead when stopped (no callbacks or timers run while Performance HUD is disabled).
 */
class PerformanceMonitor(private val context: Context) {

    private val _stats = MutableStateFlow(PerfStatsSnapshot())
    val stats: StateFlow<PerfStatsSnapshot> = _stats.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val frameTimestamps = ArrayDeque<Long>(160)
    private var running = false
    private var lastPublishMs = 0L

    private var cachedBatteryPct = 100
    private var cachedCharging = false
    private var cachedTempC = 0f
    private var cachedRamPct = 0
    private var lastSystemPollMs = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            frameTimestamps.addLast(frameTimeNanos)
            val cutoff = frameTimeNanos - ONE_SECOND_NANOS
            while (frameTimestamps.isNotEmpty() && frameTimestamps.first() < cutoff) {
                frameTimestamps.removeFirst()
            }

            val nowMs = System.currentTimeMillis()
            if (nowMs - lastSystemPollMs >= SYSTEM_POLL_INTERVAL_MS) {
                lastSystemPollMs = nowMs
                pollBatteryAndMemory()
            }
            if (nowMs - lastPublishMs >= UI_PUBLISH_INTERVAL_MS) {
                lastPublishMs = nowMs
                val fps = computeFpsFromFrameTimesNanos(frameTimestamps.toList())
                _stats.value = PerfStatsSnapshot(
                    fps = fps,
                    batteryPercent = cachedBatteryPct,
                    isCharging = cachedCharging,
                    batteryTempC = cachedTempC,
                    usedRamPercent = cachedRamPct
                )
            }

            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        if (running) return
        running = true
        frameTimestamps.clear()
        lastPublishMs = 0L
        lastSystemPollMs = 0L
        mainHandler.post {
            if (!running) return@post
            pollBatteryAndMemory()
            _stats.value = _stats.value.copy(
                batteryPercent = cachedBatteryPct,
                isCharging = cachedCharging,
                batteryTempC = cachedTempC,
                usedRamPercent = cachedRamPct
            )
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    fun stop() {
        if (!running) return
        running = false
        mainHandler.post {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            frameTimestamps.clear()
        }
    }

    private fun pollBatteryAndMemory() {
        runCatching {
            val intent = context.applicationContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            if (intent != null) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                if (level >= 0) {
                    cachedBatteryPct = ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
                }
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                cachedCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                if (tempTenths > 0) {
                    cachedTempC = tempTenths / 10f
                }
            }
        }
        runCatching {
            val am = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (am != null) {
                val info = ActivityManager.MemoryInfo()
                am.getMemoryInfo(info)
                if (info.totalMem > 0L) {
                    val used = (info.totalMem - info.availMem).coerceAtLeast(0L)
                    cachedRamPct = ((used * 100.0) / info.totalMem).roundToInt().coerceIn(0, 100)
                }
            }
        }
    }

    companion object {
        private const val ONE_SECOND_NANOS = 1_000_000_000L
        private const val UI_PUBLISH_INTERVAL_MS = 350L
        private const val SYSTEM_POLL_INTERVAL_MS = 2_000L

        /**
         * Pure testable FPS calculator from monotonic nanosecond frame timestamps.
         */
        fun computeFpsFromFrameTimesNanos(frameTimes: List<Long>): Int {
            if (frameTimes.size < 2) return 0
            val spanNanos = frameTimes.last() - frameTimes.first()
            if (spanNanos <= 0L) return 0
            val intervals = frameTimes.size - 1
            val fps = (intervals * 1_000_000_000.0) / spanNanos.toDouble()
            return fps.roundToInt().coerceIn(0, 240)
        }
    }
}

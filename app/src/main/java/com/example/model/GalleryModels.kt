package com.example.model

import com.squareup.moshi.JsonClass
import kotlin.math.roundToInt

enum class CaptureMediaType(val label: String) {
    SCREENSHOT("Screenshot"),
    REPLAY("Instant Replay"),
    RECORDING("Recording")
}

@JsonClass(generateAdapter = true)
data class CapturedMediaItem(
    val id: String,
    val filePath: String,
    val fileName: String,
    val type: CaptureMediaType,
    val createdAtMs: Long,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val replayWindowSec: Int = 0
)

enum class ReplayBufferPreset(val seconds: Int, val label: String) {
    SEC_10(10, "10s"),
    SEC_20(20, "20s"),
    SEC_30(30, "30s"),
    MIN_1(60, "1m"),
    MIN_5(300, "5m"),
    MIN_15(900, "15m"),
    MIN_30(1800, "30m"),
    HOUR_1(3600, "1h");

    companion object {
        fun formatDuration(seconds: Int): String {
            val s = seconds.coerceIn(10, 3600)
            return when {
                s >= 3600 -> "${s / 3600}h"
                s >= 60 -> {
                    val m = s / 60
                    val rem = s % 60
                    if (rem == 0) "${m}m" else "${m}m ${rem}s"
                }
                else -> "${s}s"
            }
        }
    }
}

enum class RecordingResolution(
    val label: String,
    val shortLabel: String,
    val maxLongEdgePx: Int,
    val baseBitrateMbps: Float,
    val subtitle: String
) {
    P480("480p SD", "480p", 854, 2.2f, "Smallest size — best for long replay buffers"),
    P720("720p HD", "720p", 1280, 4.5f, "Balanced clarity & storage (Recommended)"),
    P1080("1080p Full HD", "1080p", 1920, 9.0f, "Sharp Full HD gameplay recording"),
    P1440("1440p QHD", "1440p", 2560, 14.5f, "High-detail QHD (auto-falls back if encoder capped)"),
    NATIVE("Native Screen", "Native", 4096, 18.0f, "Full display resolution up to encoder max")
}

enum class RecordingFps(
    val fps: Int,
    val label: String,
    val bitrateScale: Float
) {
    FPS_24(24, "24 FPS", 0.85f),
    FPS_30(30, "30 FPS", 1.0f),
    FPS_48(48, "48 FPS", 1.3f),
    FPS_60(60, "60 FPS", 1.55f),
    FPS_90(90, "90 FPS", 1.95f),
    FPS_120(120, "120 FPS", 2.25f)
}

object RecordingConfigHelper {

    fun computeTargetBitrateBps(resolution: RecordingResolution, fps: RecordingFps): Int {
        val rawBps = (resolution.baseBitrateMbps * fps.bitrateScale * 1_000_000f).roundToInt()
        return rawBps.coerceIn(1_500_000, 28_000_000)
    }

    fun formatBitrateMbps(resolution: RecordingResolution, fps: RecordingFps): String {
        val mbps = computeTargetBitrateBps(resolution, fps) / 1_000_000f
        return "%.1f Mbps".format(mbps)
    }

    fun estimateStorageMbPerMinute(resolution: RecordingResolution, fps: RecordingFps): Int {
        val bps = computeTargetBitrateBps(resolution, fps).toDouble()
        val bytesPerMin = (bps / 8.0) * 60.0
        return (bytesPerMin / (1024.0 * 1024.0)).roundToInt().coerceAtLeast(1)
    }

    fun fallbackLongEdgeCandidates(preferred: RecordingResolution): List<Int> {
        val base = listOf(4096, 2560, 1920, 1280, 960, 854, 720, 640)
        return (listOf(preferred.maxLongEdgePx) + base.filter { it < preferred.maxLongEdgePx }).distinct()
    }

    fun fallbackFpsCandidates(preferred: RecordingFps): List<Int> {
        val base = listOf(120, 90, 60, 48, 30, 24)
        return (listOf(preferred.fps) + base.filter { it < preferred.fps }).distinct()
    }
}

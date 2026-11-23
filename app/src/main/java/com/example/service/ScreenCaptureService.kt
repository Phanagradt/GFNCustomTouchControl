package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.capture.CaptureBridge
import com.example.capture.CaptureConsentActivity
import com.example.filter.GameFilterBridge
import com.example.model.CaptureMediaType
import com.example.model.RecordingConfigHelper
import com.example.model.RecordingFps
import com.example.model.RecordingResolution
import com.example.model.ReplayBufferPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenCaptureService : Service() {

    companion object {
        private const val CHANNEL_ID = "nexuspad_capture_channel"
        private const val NOTIFICATION_ID = 4118

        const val ACTION_START_WITH_PROJECTION = "com.example.action.CAPTURE_START_WITH_PROJECTION"
        const val ACTION_TAKE_SCREENSHOT = "com.example.action.CAPTURE_TAKE_SCREENSHOT"
        const val ACTION_START_REPLAY = "com.example.action.CAPTURE_START_REPLAY"
        const val ACTION_STOP_REPLAY = "com.example.action.CAPTURE_STOP_REPLAY"
        const val ACTION_SAVE_REPLAY = "com.example.action.CAPTURE_SAVE_REPLAY"
        const val ACTION_START_RECORDING = "com.example.action.CAPTURE_START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.example.action.CAPTURE_STOP_RECORDING"
        const val ACTION_START_GPU_FILTER = "com.example.action.CAPTURE_START_GPU_FILTER"
        const val ACTION_STOP_GPU_FILTER = "com.example.action.CAPTURE_STOP_GPU_FILTER"
        const val ACTION_STOP_ALL = "com.example.action.CAPTURE_STOP_ALL"

        private const val EXTRA_RESULT_CODE = "extra_result_code"
        private const val EXTRA_DATA = "extra_data"
        private const val EXTRA_PENDING_ACTION = "extra_pending_action"
        private const val EXTRA_REPLAY_SEC = "extra_replay_sec"

        @Volatile
        private var instanceRef: ScreenCaptureService? = null

        fun isProjectionActive(): Boolean = instanceRef?.mediaProjection != null

        fun requestScreenshot(context: Context) {
            val svc = instanceRef
            if (svc?.mediaProjection != null) {
                context.startService(Intent(context, ScreenCaptureService::class.java).apply {
                    action = ACTION_TAKE_SCREENSHOT
                })
            } else {
                CaptureConsentActivity.requestCapture(context, ACTION_TAKE_SCREENSHOT)
            }
        }

        fun toggleInstantReplay(context: Context, durationSec: Int) {
            CaptureBridge.setReplayDurationSec(durationSec)
            val svc = instanceRef
            if (CaptureBridge.isInstantReplayOn.value && svc != null) {
                context.startService(Intent(context, ScreenCaptureService::class.java).apply {
                    action = ACTION_STOP_REPLAY
                })
            } else if (svc?.mediaProjection != null) {
                context.startService(Intent(context, ScreenCaptureService::class.java).apply {
                    action = ACTION_START_REPLAY
                    putExtra(EXTRA_REPLAY_SEC, durationSec)
                })
            } else {
                CaptureConsentActivity.requestCapture(context, ACTION_START_REPLAY, durationSec)
            }
        }

        fun saveInstantReplayClip(context: Context, durationSec: Int) {
            CaptureBridge.setReplayDurationSec(durationSec)
            if (!CaptureBridge.isInstantReplayOn.value || instanceRef == null) {
                CaptureBridge.postBanner("Turn on Instant Replay first to save clips")
                return
            }
            context.startService(Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_SAVE_REPLAY
                putExtra(EXTRA_REPLAY_SEC, durationSec)
            })
        }

        fun toggleRecording(context: Context) {
            val svc = instanceRef
            if (CaptureBridge.isRecording.value && svc != null) {
                context.startService(Intent(context, ScreenCaptureService::class.java).apply {
                    action = ACTION_STOP_RECORDING
                })
            } else if (svc?.mediaProjection != null) {
                context.startService(Intent(context, ScreenCaptureService::class.java).apply {
                    action = ACTION_START_RECORDING
                })
            } else {
                CaptureConsentActivity.requestCapture(context, ACTION_START_RECORDING)
            }
        }

        fun toggleGpuFilter(context: Context) {
            val svc = instanceRef
            if (GameFilterBridge.isGpuShaderActive.value && svc != null) {
                context.startService(Intent(context, ScreenCaptureService::class.java).apply {
                    action = ACTION_STOP_GPU_FILTER
                })
            } else {
                CaptureConsentActivity.requestCapture(context, ACTION_START_GPU_FILTER)
            }
        }

        fun stopGpuFilter(context: Context) {
            if (!GameFilterBridge.isGpuShaderActive.value) return
            val svc = instanceRef
            if (svc != null) {
                context.startService(Intent(context, ScreenCaptureService::class.java).apply {
                    action = ACTION_STOP_GPU_FILTER
                })
            } else {
                GameFilterBridge.setGpuShaderActive(false)
            }
        }

        fun startWithProjection(
            context: Context,
            resultCode: Int,
            data: Intent,
            pendingAction: String,
            replayDurationSec: Int
        ) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_START_WITH_PROJECTION
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
                putExtra(EXTRA_PENDING_ACTION, pendingAction)
                putExtra(EXTRA_REPLAY_SEC, replayDurationSec)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private data class ReplayChunk(
        val file: File,
        var startedAtMs: Long,
        var endedAtMs: Long = 0L
    )

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var persistentRecorderSurface: Surface? = null

    private var captureWidth = 1280
    private var captureHeight = 720
    private var captureFps = 30
    private var captureBitrate = 4_500_000
    private var captureDpi = 320
    private var isForegroundStarted = false

    private var currentReplayChunk: ReplayChunk? = null
    private var queuedNextReplayChunk: ReplayChunk? = null
    private val replayChunks = ArrayDeque<ReplayChunk>()
    private var currentRecordingFile: File? = null
    private var recordingStartMs = 0L
    private var recordingTickerJob: Job? = null
    private var replayRotateJob: Job? = null
    private var gpuSurfaceJob: Job? = null
    private var replayDurationSec = 30

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            mainHandler.post {
                stopAllAndTearDown(postBanner = false)
            }
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            if (width <= 0 || height <= 0) return
            mainHandler.post {
                if (GameFilterBridge.isGpuShaderActive.value) {
                    GameFilterBridge.updateGpuContentSize(width, height)
                    runCatching { virtualDisplay?.resize(width, height, captureDpi) }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instanceRef = this
        captureThread = HandlerThread("NexusCaptureThread").apply { start() }
        captureHandler = Handler(captureThread!!.looper)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_WITH_PROJECTION -> {
                if (!promoteForeground("NexusPad Screen Capture Active")) {
                    return START_NOT_STICKY
                }
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                @Suppress("DEPRECATION")
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    intent.getParcelableExtra(EXTRA_DATA)
                }
                val pending = intent.getStringExtra(EXTRA_PENDING_ACTION) ?: ACTION_TAKE_SCREENSHOT
                replayDurationSec = intent.getIntExtra(EXTRA_REPLAY_SEC, CaptureBridge.replayDurationSec.value)
                    .coerceIn(10, 3600)

                if (resultCode == Activity.RESULT_OK && data != null && initMediaProjection(resultCode, data)) {
                    dispatchCaptureAction(pending)
                } else {
                    stopAllAndTearDown(postBanner = false)
                }
            }
            ACTION_TAKE_SCREENSHOT -> takeScreenshotNow()
            ACTION_START_REPLAY -> {
                replayDurationSec = intent.getIntExtra(EXTRA_REPLAY_SEC, CaptureBridge.replayDurationSec.value)
                    .coerceIn(10, 3600)
                startInstantReplayLoop()
            }
            ACTION_STOP_REPLAY -> stopInstantReplayLoop(tearDownIfIdle = true)
            ACTION_SAVE_REPLAY -> {
                replayDurationSec = intent.getIntExtra(EXTRA_REPLAY_SEC, CaptureBridge.replayDurationSec.value)
                    .coerceIn(10, 3600)
                saveReplayBufferClip()
            }
            ACTION_START_RECORDING -> startManualRecording()
            ACTION_STOP_RECORDING -> stopManualRecording(tearDownAfter = true)
            ACTION_START_GPU_FILTER -> startLiveGpuFilterProjection()
            ACTION_STOP_GPU_FILTER -> stopLiveGpuFilterProjection(tearDownIfIdle = true)
            ACTION_STOP_ALL -> stopAllAndTearDown(postBanner = false)
        }
        return START_NOT_STICKY
    }

    private fun promoteForeground(contentText: String): Boolean {
        val notification = buildNotification(contentText)
        if (isForegroundStarted) {
            runCatching {
                getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
            }
            return true
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForegroundStarted = true
            true
        } catch (_: Exception) {
            isForegroundStarted = false
            CaptureBridge.postBanner("Please allow screen capture permission to start")
            stopAllAndTearDown(postBanner = false)
            false
        }
    }

    private fun initMediaProjection(resultCode: Int, data: Intent): Boolean {
        if (mediaProjection != null) return true
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val proj = runCatching { mgr.getMediaProjection(resultCode, data) }.getOrNull() ?: return false
        proj.registerCallback(projectionCallback, mainHandler)
        mediaProjection = proj
        computeCaptureDimensions()
        return true
    }

    private fun computeCaptureDimensions() {
        val displayMgr = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val defaultDisplay = displayMgr.getDisplay(Display.DEFAULT_DISPLAY)
        val displayCtx = if (defaultDisplay != null) createDisplayContext(defaultDisplay) else this
        val dm = displayCtx.resources.displayMetrics
        captureDpi = dm.densityDpi.coerceAtLeast(160)
        val rawW: Int
        val rawH: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = displayCtx.getSystemService(WINDOW_SERVICE) as WindowManager
            val b = wm.maximumWindowMetrics.bounds
            rawW = b.width()
            rawH = b.height()
        } else {
            val pt = Point()
            @Suppress("DEPRECATION")
            defaultDisplay?.getRealSize(pt)
            rawW = if (pt.x > 0) pt.x else dm.widthPixels
            rawH = if (pt.y > 0) pt.y else dm.heightPixels
        }

        val preferredRes = CaptureBridge.recordingResolution.value
        val preferredFps = CaptureBridge.recordingFps.value
        val resolved = resolveEncoderSafeConfig(
            rawW = rawW.coerceAtLeast(320),
            rawH = rawH.coerceAtLeast(320),
            preferredRes = preferredRes,
            preferredFps = preferredFps
        )
        captureWidth = resolved.width
        captureHeight = resolved.height
        captureFps = resolved.fps
        captureBitrate = resolved.bitrateBps
    }

    private data class ResolvedEncoderConfig(
        val width: Int,
        val height: Int,
        val fps: Int,
        val bitrateBps: Int
    )

    private fun resolveEncoderSafeConfig(
        rawW: Int,
        rawH: Int,
        preferredRes: RecordingResolution,
        preferredFps: RecordingFps
    ): ResolvedEncoderConfig {
        val avcCaps = findAvcVideoCapabilities()
        val alignW = (avcCaps?.widthAlignment ?: 16).coerceAtLeast(16)
        val alignH = (avcCaps?.heightAlignment ?: 16).coerceAtLeast(16)
        val longEdge = maxOf(rawW, rawH).toFloat().coerceAtLeast(1f)
        val targetBitrate = RecordingConfigHelper.computeTargetBitrateBps(preferredRes, preferredFps)
        val clampedBitrate = avcCaps?.bitrateRange?.let { range ->
            targetBitrate.coerceIn(range.lower, range.upper)
        } ?: targetBitrate

        val edgeCandidates = RecordingConfigHelper.fallbackLongEdgeCandidates(preferredRes)
        val fpsCandidates = RecordingConfigHelper.fallbackFpsCandidates(preferredFps)

        // First pass: find the highest supported (size, fps) pair honoring preferred resolution then FPS
        for (targetMaxEdge in edgeCandidates) {
            val scale = (targetMaxEdge.toFloat() / longEdge).coerceAtMost(1f)
            val w = (((rawW * scale).toInt() / alignW) * alignW).coerceAtLeast(320)
            val h = (((rawH * scale).toInt() / alignH) * alignH).coerceAtLeast(320)
            if (avcCaps != null && !avcCaps.isSizeSupported(w, h)) continue

            for (fpsCandidate in fpsCandidates) {
                if (avcCaps == null || avcCaps.areSizeAndRateSupported(w, h, fpsCandidate.toDouble())) {
                    return ResolvedEncoderConfig(
                        width = w,
                        height = h,
                        fps = fpsCandidate,
                        bitrateBps = clampedBitrate
                    )
                }
            }
        }

        // Second pass: relax to size-only check at 30 FPS
        for (targetMaxEdge in edgeCandidates) {
            val scale = (targetMaxEdge.toFloat() / longEdge).coerceAtMost(1f)
            val w = (((rawW * scale).toInt() / alignW) * alignW).coerceAtLeast(320)
            val h = (((rawH * scale).toInt() / alignH) * alignH).coerceAtLeast(320)
            if (avcCaps == null || avcCaps.isSizeSupported(w, h)) {
                return ResolvedEncoderConfig(
                    width = w,
                    height = h,
                    fps = preferredFps.fps.coerceAtMost(30),
                    bitrateBps = clampedBitrate.coerceAtMost(6_000_000)
                )
            }
        }

        return ResolvedEncoderConfig(
            width = 640,
            height = 480,
            fps = 30,
            bitrateBps = 2_500_000
        )
    }

    private fun findAvcVideoCapabilities(): MediaCodecInfo.VideoCapabilities? {
        return runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                .firstOrNull { info ->
                    info.isEncoder && info.supportedTypes.any {
                        it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true)
                    }
                }
                ?.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
                ?.videoCapabilities
        }.getOrNull()
    }

    private fun attachOrCreateVirtualDisplay(
        proj: MediaProjection,
        vdName: String,
        surface: Surface
    ): Boolean {
        val existingVd = virtualDisplay
        if (existingVd != null) {
            val reused = runCatching {
                existingVd.resize(captureWidth, captureHeight, captureDpi)
                existingVd.setSurface(surface)
                true
            }.getOrDefault(false)
            if (reused) return true
            runCatching { existingVd.release() }
            virtualDisplay = null
        }

        val created = runCatching {
            proj.createVirtualDisplay(
                vdName,
                captureWidth,
                captureHeight,
                captureDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                captureHandler
            )
        }.getOrNull()
        virtualDisplay = created
        return created != null
    }

    private fun dispatchCaptureAction(action: String) {
        when (action) {
            ACTION_TAKE_SCREENSHOT -> takeScreenshotNow()
            ACTION_START_REPLAY -> startInstantReplayLoop()
            ACTION_START_RECORDING -> startManualRecording()
            ACTION_START_GPU_FILTER -> startLiveGpuFilterProjection()
        }
    }

    private fun startLiveGpuFilterProjection() {
        val proj = mediaProjection
        if (proj == null) {
            CaptureConsentActivity.requestCapture(applicationContext, ACTION_START_GPU_FILTER)
            return
        }
        if (CaptureBridge.isRecording.value) {
            stopManualRecording(tearDownAfter = false)
        }
        if (CaptureBridge.isInstantReplayOn.value) {
            stopInstantReplayLoop(tearDownIfIdle = false)
        }

        computeCaptureDimensions()
        GameFilterBridge.setGpuShaderActive(true)
        promoteForeground("Live GPU Freestyle Filter Active")
        CaptureBridge.postBanner("Live GPU Shader Filter ON")

        gpuSurfaceJob?.cancel()
        gpuSurfaceJob = serviceScope.launch {
            GameFilterBridge.gpuTargetSurface.collect { surface ->
                if (!GameFilterBridge.isGpuShaderActive.value) return@collect
                val activeProj = mediaProjection ?: return@collect
                if (surface == null || !surface.isValid) return@collect
                val (w, h) = GameFilterBridge.gpuContentSize.value
                val targetW = w.coerceAtLeast(320)
                val targetH = h.coerceAtLeast(320)
                captureWidth = targetW
                captureHeight = targetH
                val attached = attachOrCreateVirtualDisplay(activeProj, "NexusGpuFilterVD", surface)
                if (!attached) {
                    CaptureBridge.postBanner("Could not attach Live GPU Shader display")
                    stopLiveGpuFilterProjection(tearDownIfIdle = true)
                }
            }
        }
    }

    private fun stopLiveGpuFilterProjection(tearDownIfIdle: Boolean) {
        gpuSurfaceJob?.cancel()
        gpuSurfaceJob = null
        GameFilterBridge.setGpuShaderActive(false)
        CaptureBridge.postBanner("Live GPU Shader Filter OFF")
        if (tearDownIfIdle && !CaptureBridge.isRecording.value && !CaptureBridge.isInstantReplayOn.value) {
            stopAllAndTearDown(postBanner = false)
        }
    }

    private fun takeScreenshotNow() {
        val proj = mediaProjection
        if (proj == null) {
            CaptureConsentActivity.requestCapture(applicationContext, ACTION_TAKE_SCREENSHOT)
            return
        }

        val wasReplaying = CaptureBridge.isInstantReplayOn.value
        val wasRecording = CaptureBridge.isRecording.value
        if (wasRecording) {
            CaptureBridge.postBanner("Stop recording first before taking a screenshot")
            return
        }

        if (wasReplaying) {
            finalizeCurrentReplayChunkIntoQueue()
        }
        computeCaptureDimensions()

        serviceScope.launch {
            // Brief settle delay so left overlay menu finishes hiding cleanly
            delay(180L)
            val reader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
            var captured = false
            if (!attachOrCreateVirtualDisplay(proj, "NexusScreenshotVD", reader.surface)) {
                runCatching { reader.close() }
                CaptureBridge.postBanner("Could not capture screen frame")
                if (wasReplaying) {
                    startReplaySegmentRecorder()
                } else {
                    stopAllAndTearDown(postBanner = false)
                }
                return@launch
            }

            reader.setOnImageAvailableListener({ r ->
                if (captured) return@setOnImageAvailableListener
                val image = runCatching { r.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
                captured = true
                try {
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * captureWidth
                    val paddedBitmap = Bitmap.createBitmap(
                        captureWidth + rowPadding / pixelStride,
                        captureHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    paddedBitmap.copyPixelsFromBuffer(buffer)
                    val cropped = Bitmap.createBitmap(paddedBitmap, 0, 0, captureWidth, captureHeight)
                    if (cropped != paddedBitmap) {
                        paddedBitmap.recycle()
                    }
                    serviceScope.launch(Dispatchers.IO) {
                        runCatching { CaptureBridge.saveScreenshotBitmap(applicationContext, cropped) }
                        cropped.recycle()
                        withContext(Dispatchers.Main) {
                            CaptureBridge.postBanner("Screenshot Saved to Gallery")
                        }
                    }
                } finally {
                    image.close()
                    reader.setOnImageAvailableListener(null, null)
                    reader.close()
                    if (wasReplaying) {
                        startReplaySegmentRecorder()
                    } else {
                        stopAllAndTearDown(postBanner = false)
                    }
                }
            }, captureHandler)

            // Safety timeout in case no frame updates arrive within 2s
            delay(2000L)
            if (!captured) {
                captured = true
                runCatching {
                    reader.setOnImageAvailableListener(null, null)
                    reader.close()
                }
                CaptureBridge.postBanner("Screenshot timed out waiting for frame")
                if (wasReplaying) {
                    startReplaySegmentRecorder()
                } else {
                    stopAllAndTearDown(postBanner = false)
                }
            }
        }
    }

    private fun startInstantReplayLoop() {
        val proj = mediaProjection
        if (proj == null) {
            CaptureConsentActivity.requestCapture(applicationContext, ACTION_START_REPLAY, replayDurationSec)
            return
        }
        if (CaptureBridge.isRecording.value) {
            stopManualRecording(tearDownAfter = false)
        }
        clearRingBufferFiles()
        CaptureBridge.setInstantReplayOn(true)
        promoteForeground("Instant Replay ON • Last ${ReplayBufferPreset.formatDuration(replayDurationSec)}")
        CaptureBridge.postBanner("Instant Replay ON (${ReplayBufferPreset.formatDuration(replayDurationSec)})")
        startReplaySegmentRecorder()
    }

    private fun chunkDurationMsForWindow(windowSec: Int): Long =
        if (windowSec <= 120) 5_000L else 10_000L

    private fun startReplaySegmentRecorder() {
        val proj = mediaProjection ?: return
        computeCaptureDimensions()
        val chunkMs = chunkDurationMsForWindow(replayDurationSec)
        val ringDir = CaptureBridge.replayRingDir(applicationContext)
        val file1 = File(ringDir, "chunk_${System.currentTimeMillis()}.mp4")
        val chunk1 = ReplayChunk(file1, System.currentTimeMillis())

        runCatching { mediaRecorder?.release() }
        mediaRecorder = null

        val recorder = buildConfiguredRecorder(file1, maxDurationMs = chunkMs.toInt())
        val prepared = runCatching {
            recorder.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED &&
                    CaptureBridge.isInstantReplayOn.value
                ) {
                    finalizeCurrentReplayChunkIntoQueue()
                    startReplaySegmentRecorder()
                }
            }
            recorder.prepare()
        }.isSuccess

        if (!prepared) {
            runCatching { recorder.release() }
            CaptureBridge.postBanner("Unable to start Replay encoder")
            stopAllAndTearDown(postBanner = false)
            return
        }

        val surface = recorder.surface
        if (!attachOrCreateVirtualDisplay(proj, "NexusReplayVD", surface)) {
            runCatching { recorder.release() }
            file1.delete()
            CaptureBridge.postBanner("Unable to attach display for Instant Replay")
            stopAllAndTearDown(postBanner = false)
            return
        }

        mediaRecorder = recorder
        currentReplayChunk = chunk1
        persistentRecorderSurface = surface

        val started = runCatching { recorder.start(); true }.getOrDefault(false)
        if (!started) {
            runCatching { recorder.release() }
            mediaRecorder = null
            currentReplayChunk = null
            file1.delete()
            CaptureBridge.postBanner("Could not start Instant Replay")
            stopAllAndTearDown(postBanner = false)
            return
        }

        // Deterministic fallback rotation ensures reliable 5s/10s segments for up to 1 hour
        replayRotateJob?.cancel()
        replayRotateJob = serviceScope.launch {
            while (isActive && CaptureBridge.isInstantReplayOn.value) {
                delay(1500L)
                val active = currentReplayChunk
                if (active != null && (System.currentTimeMillis() - active.startedAtMs) >= chunkMs + 1200L) {
                    finalizeCurrentReplayChunkIntoQueue()
                    startReplaySegmentRecorder()
                    break
                }
            }
        }
    }

    private fun finalizeCurrentReplayChunkIntoQueue() {
        replayRotateJob?.cancel()
        val mr = mediaRecorder
        mediaRecorder = null
        var stopOk = false
        if (mr != null) {
            stopOk = runCatching { mr.stop(); true }.getOrDefault(false)
            runCatching { mr.release() }
        }
        currentReplayChunk?.let { chunk ->
            chunk.endedAtMs = System.currentTimeMillis()
            if (stopOk && CaptureBridge.isValidVideoFile(chunk.file)) {
                replayChunks.addLast(chunk)
            } else {
                chunk.file.delete()
            }
            currentReplayChunk = null
        }
        queuedNextReplayChunk?.file?.delete()
        queuedNextReplayChunk = null
        pruneOldReplayChunks()
    }

    private fun pruneOldReplayChunks() {
        val windowMs = replayDurationSec * 1000L + 15_000L
        val cutoff = System.currentTimeMillis() - windowMs
        while (replayChunks.size > 1 && replayChunks.first().endedAtMs in 1 until cutoff) {
            val evicted = replayChunks.removeFirst()
            evicted.file.delete()
        }
    }

    private fun saveReplayBufferClip() {
        if (!CaptureBridge.isInstantReplayOn.value) {
            CaptureBridge.postBanner("Instant Replay is off")
            return
        }
        val targetSec = replayDurationSec.coerceIn(10, 3600)
        CaptureBridge.postBanner("Saving Last ${ReplayBufferPreset.formatDuration(targetSec)}...")
        finalizeCurrentReplayChunkIntoQueue()

        // Snapshot needed chunk paths from the tail covering targetSec
        val windowMs = targetSec * 1000L
        val now = System.currentTimeMillis()
        val selectedFiles = mutableListOf<File>()
        var accumulatedMs = 0L
        for (chunk in replayChunks.reversed()) {
            if (!chunk.file.exists() || chunk.file.length() <= 1024L) continue
            selectedFiles.add(0, chunk.file)
            val dur = (chunk.endedAtMs - chunk.startedAtMs).coerceAtLeast(1000L)
            accumulatedMs += dur
            if (accumulatedMs >= windowMs || (now - chunk.startedAtMs) >= windowMs) {
                break
            }
        }

        // Resume Instant Replay ring immediately while muxing runs in background!
        startReplaySegmentRecorder()

        if (selectedFiles.isEmpty()) {
            CaptureBridge.postBanner("Buffer warming up — try again in a few seconds")
            return
        }

        serviceScope.launch(Dispatchers.IO) {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(CaptureBridge.galleryDir(applicationContext), "replay_${stamp}.mp4")
            val success = muxMp4Segments(selectedFiles, outFile)
            if (success && outFile.exists() && outFile.length() > 1024L) {
                CaptureBridge.registerSavedVideo(
                    context = applicationContext,
                    videoFile = outFile,
                    type = CaptureMediaType.REPLAY,
                    replayWindowSec = targetSec
                )
                withContext(Dispatchers.Main) {
                    CaptureBridge.postBanner("Saved Replay (${ReplayBufferPreset.formatDuration(targetSec)})")
                }
            } else {
                outFile.delete()
                withContext(Dispatchers.Main) {
                    CaptureBridge.postBanner("Failed to save Replay clip")
                }
            }
        }
    }

    private fun stopInstantReplayLoop(tearDownIfIdle: Boolean) {
        CaptureBridge.setInstantReplayOn(false)
        finalizeCurrentReplayChunkIntoQueue()
        clearRingBufferFiles()
        CaptureBridge.postBanner("Instant Replay OFF")
        if (tearDownIfIdle && !CaptureBridge.isRecording.value) {
            stopAllAndTearDown(postBanner = false)
        }
    }

    private fun startManualRecording() {
        val proj = mediaProjection
        if (proj == null) {
            CaptureConsentActivity.requestCapture(applicationContext, ACTION_START_RECORDING)
            return
        }
        if (CaptureBridge.isInstantReplayOn.value) {
            stopInstantReplayLoop(tearDownIfIdle = false)
        }

        computeCaptureDimensions()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(CaptureBridge.galleryDir(applicationContext), "rec_${stamp}.mp4")
        currentRecordingFile = outFile
        CaptureBridge.setActiveRecordingFile(outFile.absolutePath)

        runCatching { mediaRecorder?.release() }
        val recorder = buildConfiguredRecorder(outFile, maxDurationMs = 0)
        val prepared = runCatching { recorder.prepare(); true }.getOrDefault(false)
        if (!prepared) {
            runCatching { recorder.release() }
            outFile.delete()
            currentRecordingFile = null
            CaptureBridge.setActiveRecordingFile(null)
            CaptureBridge.postBanner("Failed to prepare screen recorder")
            stopAllAndTearDown(postBanner = false)
            return
        }
        val surface = recorder.surface
        if (!attachOrCreateVirtualDisplay(proj, "NexusRecordVD", surface)) {
            runCatching { recorder.release() }
            outFile.delete()
            currentRecordingFile = null
            CaptureBridge.setActiveRecordingFile(null)
            CaptureBridge.postBanner("Unable to attach display for recording")
            stopAllAndTearDown(postBanner = false)
            return
        }

        mediaRecorder = recorder
        persistentRecorderSurface = surface

        val started = runCatching { recorder.start(); true }.getOrDefault(false)
        if (!started) {
            runCatching { recorder.release() }
            mediaRecorder = null
            outFile.delete()
            currentRecordingFile = null
            CaptureBridge.setActiveRecordingFile(null)
            CaptureBridge.postBanner("Failed to start screen recording")
            stopAllAndTearDown(postBanner = false)
            return
        }

        recordingStartMs = System.currentTimeMillis()
        CaptureBridge.setRecordingState(true, 0L)
        promoteForeground("Recording ${captureWidth}×${captureHeight} @ ${captureFps} FPS • NexusPad")
        CaptureBridge.postBanner("Recording Started (${captureWidth}×${captureHeight} • ${captureFps} FPS)")

        recordingTickerJob?.cancel()
        recordingTickerJob = serviceScope.launch {
            while (isActive && CaptureBridge.isRecording.value) {
                CaptureBridge.setRecordingState(true, System.currentTimeMillis() - recordingStartMs)
                delay(500L)
            }
        }
    }

    private fun stopManualRecording(tearDownAfter: Boolean) {
        recordingTickerJob?.cancel()
        CaptureBridge.setRecordingState(false, 0L)
        val mr = mediaRecorder
        mediaRecorder = null
        var stopOk = false
        if (mr != null) {
            stopOk = runCatching { mr.stop(); true }.getOrDefault(false)
            runCatching { mr.release() }
        }
        val savedFile = currentRecordingFile
        currentRecordingFile = null
        CaptureBridge.setActiveRecordingFile(null)
        if (stopOk && savedFile != null && CaptureBridge.isValidVideoFile(savedFile)) {
            serviceScope.launch(Dispatchers.IO) {
                CaptureBridge.registerSavedVideo(
                    context = applicationContext,
                    videoFile = savedFile,
                    type = CaptureMediaType.RECORDING
                )
                withContext(Dispatchers.Main) {
                    CaptureBridge.postBanner("Recording Saved to Gallery")
                }
            }
        } else {
            savedFile?.delete()
            if (savedFile != null) {
                CaptureBridge.postBanner("Recording too short — record for at least 1 second")
            }
        }

        if (tearDownAfter && !CaptureBridge.isInstantReplayOn.value) {
            stopAllAndTearDown(postBanner = false)
        }
    }

    @Suppress("DEPRECATION")
    private fun buildConfiguredRecorder(outputFile: File, maxDurationMs: Int): MediaRecorder {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }
        return recorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(outputFile.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(captureWidth, captureHeight)
            setVideoFrameRate(captureFps)
            setVideoEncodingBitRate(captureBitrate)
            if (maxDurationMs > 0) {
                setMaxDuration(maxDurationMs)
            }
        }
    }

    private fun muxMp4Segments(segments: List<File>, outFile: File): Boolean {
        if (segments.size == 1) {
            return runCatching {
                segments.first().copyTo(outFile, overwrite = true)
                true
            }.getOrDefault(false)
        }
        var muxer: MediaMuxer? = null
        return try {
            val buffer = ByteBuffer.allocate(512 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            var outTrackIndex = -1
            var ptsOffsetUs = 0L

            segments.forEach { file ->
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(file.absolutePath)
                    var videoTrack = -1
                    var trackFormat: MediaFormat? = null
                    for (i in 0 until extractor.trackCount) {
                        val fmt = extractor.getTrackFormat(i)
                        val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                        if (mime.startsWith("video/")) {
                            videoTrack = i
                            trackFormat = fmt
                            break
                        }
                    }
                    if (videoTrack >= 0 && trackFormat != null) {
                        extractor.selectTrack(videoTrack)
                        if (muxer == null) {
                            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                            outTrackIndex = muxer!!.addTrack(trackFormat)
                            muxer!!.start()
                        }
                        var maxLocalPtsUs = 0L
                        while (true) {
                            bufferInfo.offset = 0
                            bufferInfo.size = extractor.readSampleData(buffer, 0)
                            if (bufferInfo.size < 0) break
                            val sampleTime = extractor.sampleTime.coerceAtLeast(0L)
                            maxLocalPtsUs = maxOf(maxLocalPtsUs, sampleTime)
                            bufferInfo.presentationTimeUs = ptsOffsetUs + sampleTime
                            bufferInfo.flags = extractor.sampleFlags
                            muxer!!.writeSampleData(outTrackIndex, buffer, bufferInfo)
                            extractor.advance()
                        }
                        val frameStepUs = 1_000_000L / captureFps.coerceAtLeast(24)
                        ptsOffsetUs += maxLocalPtsUs + frameStepUs
                    }
                } finally {
                    runCatching { extractor.release() }
                }
            }
            muxer != null
        } catch (_: Exception) {
            false
        } finally {
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
        }
    }

    private fun clearRingBufferFiles() {
        while (replayChunks.isNotEmpty()) {
            replayChunks.removeFirst().file.delete()
        }
        currentReplayChunk?.file?.delete()
        currentReplayChunk = null
        queuedNextReplayChunk?.file?.delete()
        queuedNextReplayChunk = null
        CaptureBridge.replayRingDir(applicationContext).listFiles()?.forEach { it.delete() }
    }

    private fun stopAllAndTearDown(postBanner: Boolean) {
        replayRotateJob?.cancel()
        recordingTickerJob?.cancel()
        gpuSurfaceJob?.cancel()
        gpuSurfaceJob = null
        GameFilterBridge.setGpuShaderActive(false)
        CaptureBridge.setInstantReplayOn(false)
        CaptureBridge.setRecordingState(false, 0L)
        CaptureBridge.setActiveRecordingFile(null)

        val mr = mediaRecorder
        mediaRecorder = null
        var stopOk = false
        if (mr != null) {
            stopOk = runCatching { mr.stop(); true }.getOrDefault(false)
            runCatching { mr.release() }
        }
        val abandonedFile = currentRecordingFile
        currentRecordingFile = null
        if (abandonedFile != null && (!stopOk || !CaptureBridge.isValidVideoFile(abandonedFile))) {
            abandonedFile.delete()
        }

        runCatching { persistentRecorderSurface?.release() }
        persistentRecorderSurface = null

        runCatching { virtualDisplay?.release() }
        virtualDisplay = null

        mediaProjection?.let { proj ->
            runCatching { proj.unregisterCallback(projectionCallback) }
            runCatching { proj.stop() }
        }
        mediaProjection = null
        clearRingBufferFiles()
        if (postBanner) {
            CaptureBridge.postBanner("Capture Stopped")
        }
        isForegroundStarted = false
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NexusPad Capture & Instant Replay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for GeForce-style screen recording, replay buffer, and screenshots."
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, ScreenCaptureService::class.java).setAction(ACTION_STOP_ALL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NexusPad Shadow Capture")
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "Stop Capture", stopIntent)
            .build()
    }

    override fun onDestroy() {
        if (instanceRef === this) {
            instanceRef = null
        }
        replayRotateJob?.cancel()
        recordingTickerJob?.cancel()
        gpuSurfaceJob?.cancel()
        GameFilterBridge.setGpuShaderActive(false)
        CaptureBridge.setInstantReplayOn(false)
        CaptureBridge.setRecordingState(false, 0L)
        runCatching { mediaRecorder?.release() }
        runCatching { persistentRecorderSurface?.release() }
        runCatching { virtualDisplay?.release() }
        mediaProjection?.let {
            runCatching { it.unregisterCallback(projectionCallback) }
            runCatching { it.stop() }
        }
        serviceScope.cancel()
        captureThread?.quitSafely()
        super.onDestroy()
    }
}

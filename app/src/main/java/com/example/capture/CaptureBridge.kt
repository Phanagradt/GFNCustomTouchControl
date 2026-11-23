package com.example.capture

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.CaptureMediaType
import com.example.model.CapturedMediaItem
import com.example.model.RecordingFps
import com.example.model.RecordingResolution
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object CaptureBridge {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val itemAdapter = moshi.adapter(CapturedMediaItem::class.java)

    private val _isInstantReplayOn = MutableStateFlow(false)
    val isInstantReplayOn: StateFlow<Boolean> = _isInstantReplayOn.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingElapsedMs = MutableStateFlow(0L)
    val recordingElapsedMs: StateFlow<Long> = _recordingElapsedMs.asStateFlow()

    private val _replayDurationSec = MutableStateFlow(30)
    val replayDurationSec: StateFlow<Int> = _replayDurationSec.asStateFlow()

    private val _recordingResolution = MutableStateFlow(RecordingResolution.P720)
    val recordingResolution: StateFlow<RecordingResolution> = _recordingResolution.asStateFlow()

    private val _recordingFps = MutableStateFlow(RecordingFps.FPS_30)
    val recordingFps: StateFlow<RecordingFps> = _recordingFps.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _galleryItems = MutableStateFlow<List<CapturedMediaItem>>(emptyList())
    val galleryItems: StateFlow<List<CapturedMediaItem>> = _galleryItems.asStateFlow()

    @Volatile
    private var activeRecordingFilePath: String? = null

    fun setActiveRecordingFile(path: String?) {
        activeRecordingFilePath = path
    }

    fun setInstantReplayOn(active: Boolean) {
        _isInstantReplayOn.value = active
    }

    fun setRecordingState(recording: Boolean, elapsedMs: Long = 0L) {
        _isRecording.value = recording
        _recordingElapsedMs.value = elapsedMs
    }

    fun setReplayDurationSec(seconds: Int) {
        _replayDurationSec.value = seconds.coerceIn(10, 3600)
    }

    fun setRecordingResolution(resolution: RecordingResolution) {
        _recordingResolution.value = resolution
    }

    fun setRecordingFps(fps: RecordingFps) {
        _recordingFps.value = fps
    }

    fun postBanner(message: String?) {
        _statusBanner.value = message
    }

    fun galleryDir(context: Context): File =
        File(context.filesDir, "nexus_gallery").apply { mkdirs() }

    fun replayRingDir(context: Context): File =
        File(context.cacheDir, "replay_ring").apply { mkdirs() }

    fun refreshGallery(context: Context) {
        val dir = galleryDir(context)
        val files = dir.listFiles().orEmpty()
        val metaMap = mutableMapOf<String, CapturedMediaItem>()
        files.filter { it.extension.equals("json", ignoreCase = true) }.forEach { metaFile ->
            runCatching {
                itemAdapter.fromJson(metaFile.readText())
            }.getOrNull()?.let { parsed ->
                metaMap[parsed.fileName] = parsed
            }
        }

        val activePath = activeRecordingFilePath
        val validItems = mutableListOf<CapturedMediaItem>()

        files
            .filter {
                it.isFile && (
                    it.extension.equals("png", ignoreCase = true) ||
                        it.extension.equals("jpg", ignoreCase = true) ||
                        it.extension.equals("mp4", ignoreCase = true)
                    )
            }
            .forEach { mediaFile ->
                val isImage = mediaFile.extension.equals("png", ignoreCase = true) ||
                    mediaFile.extension.equals("jpg", ignoreCase = true)
                if (!isImage) {
                    // Skip the currently-written recording file from Gallery & pruning
                    if (activePath != null && mediaFile.absolutePath == activePath) {
                        return@forEach
                    }
                    if (!isValidVideoFile(mediaFile)) {
                        // Clean up corrupt/0-frame MP4 stubs left by interrupted recordings
                        mediaFile.delete()
                        File(dir, "${mediaFile.nameWithoutExtension}.json").delete()
                        return@forEach
                    }
                }

                val meta = metaMap[mediaFile.name]
                val item = if (meta != null) {
                    val resolvedDuration = if (!isImage && meta.durationMs <= 0L) {
                        readVideoDurationMs(mediaFile)
                    } else {
                        meta.durationMs
                    }
                    meta.copy(
                        filePath = mediaFile.absolutePath,
                        sizeBytes = mediaFile.length(),
                        durationMs = resolvedDuration
                    )
                } else {
                    val type = when {
                        isImage -> CaptureMediaType.SCREENSHOT
                        mediaFile.name.startsWith("replay_", ignoreCase = true) -> CaptureMediaType.REPLAY
                        else -> CaptureMediaType.RECORDING
                    }
                    CapturedMediaItem(
                        id = mediaFile.nameWithoutExtension,
                        filePath = mediaFile.absolutePath,
                        fileName = mediaFile.name,
                        type = type,
                        createdAtMs = mediaFile.lastModified(),
                        durationMs = if (isImage) 0L else readVideoDurationMs(mediaFile),
                        sizeBytes = mediaFile.length()
                    )
                }
                validItems += item
            }

        _galleryItems.value = validItems.sortedByDescending { it.createdAtMs }
    }

    fun isValidVideoFile(file: File): Boolean {
        if (!file.exists() || file.length() <= 1024L) return false
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            hasVideo == "yes" && durationMs > 0L
        } catch (_: Exception) {
            false
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun saveScreenshotBitmap(context: Context, bitmap: Bitmap): CapturedMediaItem {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "shot_${stamp}.png"
        val target = File(galleryDir(context), fileName)
        target.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        val item = CapturedMediaItem(
            id = UUID.randomUUID().toString(),
            filePath = target.absolutePath,
            fileName = fileName,
            type = CaptureMediaType.SCREENSHOT,
            createdAtMs = System.currentTimeMillis(),
            durationMs = 0L,
            sizeBytes = target.length()
        )
        writeMetadata(context, item)
        exportImageToMediaStore(context, target, fileName)
        refreshGallery(context)
        return item
    }

    fun registerSavedVideo(
        context: Context,
        videoFile: File,
        type: CaptureMediaType,
        replayWindowSec: Int = 0
    ): CapturedMediaItem {
        val duration = readVideoDurationMs(videoFile)
        val item = CapturedMediaItem(
            id = UUID.randomUUID().toString(),
            filePath = videoFile.absolutePath,
            fileName = videoFile.name,
            type = type,
            createdAtMs = System.currentTimeMillis(),
            durationMs = duration,
            sizeBytes = videoFile.length(),
            replayWindowSec = replayWindowSec
        )
        writeMetadata(context, item)
        exportVideoToMediaStore(context, videoFile, videoFile.name)
        refreshGallery(context)
        return item
    }

    fun deleteMediaItem(context: Context, item: CapturedMediaItem): Boolean {
        val mediaFile = File(item.filePath)
        val metaFile = File(galleryDir(context), "${mediaFile.nameWithoutExtension}.json")
        val deleted = mediaFile.delete()
        metaFile.delete()
        refreshGallery(context)
        return deleted
    }

    private fun writeMetadata(context: Context, item: CapturedMediaItem) {
        val metaFile = File(galleryDir(context), "${File(item.fileName).nameWithoutExtension}.json")
        runCatching {
            metaFile.writeText(itemAdapter.toJson(item))
        }
    }

    private fun readVideoDurationMs(file: File): Long {
        if (!file.exists() || file.length() <= 0L) return 0L
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun exportImageToMediaStore(context: Context, source: File, fileName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NexusPad")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }

    private fun exportVideoToMediaStore(context: Context, source: File, fileName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/NexusPad")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { input -> input.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }

    fun formatDurationMs(durationMs: Long): String {
        val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
        val hours = totalSec / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, mins, secs)
        } else {
            "%02d:%02d".format(mins, secs)
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024f * 1024f))
            bytes >= 1024L -> "${bytes / 1024L} KB"
            else -> "$bytes B"
        }
    }
}

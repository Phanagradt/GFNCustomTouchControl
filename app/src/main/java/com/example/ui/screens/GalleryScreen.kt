package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.capture.CaptureBridge
import com.example.model.CaptureMediaType
import com.example.model.CapturedMediaItem
import com.example.model.RecordingConfigHelper
import com.example.model.RecordingFps
import com.example.model.RecordingResolution
import com.example.model.ReplayBufferPreset
import com.example.service.ScreenCaptureService
import com.example.ui.components.SmoothSlider
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class GalleryFilterTab(val label: String) {
    ALL("All"),
    SCREENSHOTS("Screenshots"),
    VIDEOS("Videos & Replays")
}

@Composable
fun GalleryScreen(
    replayDurationSec: Int,
    recordingResolution: RecordingResolution,
    recordingFps: RecordingFps,
    onReplayDurationChange: (Int) -> Unit,
    onRecordingResolutionChange: (RecordingResolution) -> Unit,
    onRecordingFpsChange: (RecordingFps) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaItems by CaptureBridge.galleryItems.collectAsStateWithLifecycle()
    val isReplayOn by CaptureBridge.isInstantReplayOn.collectAsStateWithLifecycle()
    val isRecording by CaptureBridge.isRecording.collectAsStateWithLifecycle()
    val recordingElapsedMs by CaptureBridge.recordingElapsedMs.collectAsStateWithLifecycle()
    val banner by CaptureBridge.statusBanner.collectAsStateWithLifecycle()

    var filterTab by remember { mutableStateOf(GalleryFilterTab.ALL) }
    var viewingItem by remember { mutableStateOf<CapturedMediaItem?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            CaptureBridge.refreshGallery(context)
        }
    }

    LaunchedEffect(banner) {
        if (!banner.isNullOrBlank()) {
            onShowMessage(banner!!)
            CaptureBridge.postBanner(null)
        }
    }

    val filteredItems = remember(mediaItems, filterTab) {
        when (filterTab) {
            GalleryFilterTab.ALL -> mediaItems
            GalleryFilterTab.SCREENSHOTS -> mediaItems.filter { it.type == CaptureMediaType.SCREENSHOT }
            GalleryFilterTab.VIDEOS -> mediaItems.filter {
                it.type == CaptureMediaType.REPLAY || it.type == CaptureMediaType.RECORDING
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Shadow Capture & Gallery",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "GeForce-style Instant Replay (10s – 1h), manual recording, and screenshots.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { CaptureBridge.refreshGallery(context) },
                modifier = Modifier.testTag("gallery_refresh_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Gallery")
            }
        }

        // GeForce ShadowPlay-style capture deck
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { ScreenCaptureService.requestScreenshot(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("gallery_screenshot_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = TacticalCyan)
                        Spacer(Modifier.width(6.dp))
                        Text("Screenshot", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = { ScreenCaptureService.toggleInstantReplay(context, replayDurationSec) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReplayOn) GeForceLime else Color(0xFF1D2634),
                            contentColor = if (isReplayOn) Color(0xFF0A1400) else Color.White
                        ),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("gallery_replay_toggle_button")
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isReplayOn) "Replay ON" else "Instant Replay",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { ScreenCaptureService.toggleRecording(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) MacroCoral else Color(0xFF1D2634),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("gallery_record_button")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isRecording) Color.White else MacroCoral
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isRecording) CaptureBridge.formatDurationMs(recordingElapsedMs) else "Record",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isReplayOn) {
                    Button(
                        onClick = { ScreenCaptureService.saveInstantReplayClip(context, replayDurationSec) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TacticalCyan,
                            contentColor = Color(0xFF031820)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("gallery_save_replay_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Save Last ${ReplayBufferPreset.formatDuration(replayDurationSec)} Clip Now",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                var replayDraftSec by remember { mutableIntStateOf(replayDurationSec.coerceIn(10, 3600)) }
                var replayDragging by remember { mutableStateOf(false) }
                LaunchedEffect(replayDurationSec, replayDragging) {
                    if (!replayDragging) {
                        replayDraftSec = replayDurationSec.coerceIn(10, 3600)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INSTANT REPLAY BUFFER LENGTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ReplayBufferPreset.formatDuration(replayDraftSec),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = GeForceLime
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ReplayBufferPreset.entries, key = { it.seconds }) { preset ->
                        FilterChip(
                            selected = replayDurationSec == preset.seconds,
                            onClick = { onReplayDurationChange(preset.seconds) },
                            label = { Text(preset.label) },
                            modifier = Modifier.testTag("gallery_replay_preset_${preset.label}")
                        )
                    }
                }

                SmoothSlider(
                    value = replayDurationSec.toFloat().coerceIn(10f, 3600f),
                    onValueChange = { raw ->
                        replayDragging = true
                        replayDraftSec = when {
                            raw < 60f -> (raw / 5f).roundToInt() * 5
                            raw < 600f -> (raw / 30f).roundToInt() * 30
                            else -> (raw / 300f).roundToInt() * 300
                        }.coerceIn(10, 3600)
                    },
                    onValueChangeFinished = { raw ->
                        replayDragging = false
                        val snapped = when {
                            raw < 60f -> (raw / 5f).roundToInt() * 5
                            raw < 600f -> (raw / 30f).roundToInt() * 30
                            else -> (raw / 300f).roundToInt() * 300
                        }.coerceIn(10, 3600)
                        replayDraftSec = snapped
                        onReplayDurationChange(snapped)
                    },
                    valueRange = 10f..3600f,
                    accentColor = GeForceLime,
                    modifier = Modifier.testTag("gallery_replay_slider")
                )

                // Quick Recording Quality Strip (Resolution + FPS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAPTURE QUALITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${recordingResolution.shortLabel} • ${recordingFps.label} • ${RecordingConfigHelper.formatBitrateMbps(recordingResolution, recordingFps)} (~${RecordingConfigHelper.estimateStorageMbPerMinute(recordingResolution, recordingFps)} MB/m)",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = TacticalCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(RecordingResolution.entries, key = { it.name }) { res ->
                        FilterChip(
                            selected = recordingResolution == res,
                            onClick = { onRecordingResolutionChange(res) },
                            label = { Text(res.shortLabel) },
                            modifier = Modifier.testTag("gallery_res_${res.name.lowercase()}")
                        )
                    }
                    items(RecordingFps.entries, key = { it.name }) { fps ->
                        FilterChip(
                            selected = recordingFps == fps,
                            onClick = { onRecordingFpsChange(fps) },
                            label = { Text(fps.label) },
                            modifier = Modifier.testTag("gallery_fps_${fps.fps}")
                        )
                    }
                }
            }
        }

        // Category Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GalleryFilterTab.entries.forEach { tab ->
                val count = when (tab) {
                    GalleryFilterTab.ALL -> mediaItems.size
                    GalleryFilterTab.SCREENSHOTS -> mediaItems.count { it.type == CaptureMediaType.SCREENSHOT }
                    GalleryFilterTab.VIDEOS -> mediaItems.count { it.type != CaptureMediaType.SCREENSHOT }
                }
                FilterChip(
                    selected = filterTab == tab,
                    onClick = { filterTab = tab },
                    label = { Text("${tab.label} ($count)") },
                    modifier = Modifier.testTag("gallery_tab_${tab.name.lowercase()}")
                )
            }
        }

        // Gallery Grid or Empty State
        if (filteredItems.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = TacticalCyan.copy(alpha = 0.65f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No captures yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Use Screenshot, Instant Replay (10s–1h), or Record above — or from the Floating Bubble menu while playing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(164.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    GalleryMediaCard(
                        item = item,
                        onClick = { viewingItem = item },
                        onShare = { shareMediaItem(context, item) },
                        onDelete = {
                            CaptureBridge.deleteMediaItem(context, item)
                            onShowMessage("Deleted ${item.fileName}")
                        }
                    )
                }
            }
        }
    }

    viewingItem?.let { current ->
        MediaPreviewDialog(
            item = current,
            onDismiss = { viewingItem = null },
            onShare = { shareMediaItem(context, current) },
            onDelete = {
                CaptureBridge.deleteMediaItem(context, current)
                viewingItem = null
                onShowMessage("Deleted ${current.fileName}")
            }
        )
    }
}

@Composable
private fun GalleryMediaCard(
    item: CapturedMediaItem,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val thumb by produceState<ImageBitmap?>(initialValue = null, item.filePath) {
        value = withContext(Dispatchers.IO) {
            loadThumbnail(item)
        }
    }

    val isVideo = item.type != CaptureMediaType.SCREENSHOT
    val badgeColor = when (item.type) {
        CaptureMediaType.SCREENSHOT -> TacticalCyan
        CaptureMediaType.REPLAY -> GeForceLime
        CaptureMediaType.RECORDING -> MacroCoral
    }
    val dateText = remember(item.createdAtMs) {
        SimpleDateFormat("MMM d • HH:mm", Locale.getDefault()).format(Date(item.createdAtMs))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("gallery_item_${item.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(Color(0xFF0A0F17)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = thumb
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = item.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = badgeColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xAA080C12))
                            .border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    color = Color(0xCC090D14),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    val tagText = when (item.type) {
                        CaptureMediaType.SCREENSHOT -> "PNG"
                        CaptureMediaType.REPLAY -> "REPLAY ${ReplayBufferPreset.formatDuration(item.replayWindowSec.coerceAtLeast(10))}"
                        CaptureMediaType.RECORDING -> "VIDEO"
                    }
                    Text(
                        text = tagText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isVideo && item.durationMs > 0L) {
                    Surface(
                        color = Color(0xCC090D14),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = CaptureBridge.formatDurationMs(item.durationMs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = CaptureBridge.formatFileSize(item.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TacticalCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = TurboAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaPreviewDialog(
    item: CapturedMediaItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xF5080B10),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.type.label,
                            color = GeForceLime,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.fileName,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = TacticalCyan)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MacroCoral)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.type == CaptureMediaType.SCREENSHOT) {
                        val bmp = remember(item.filePath) {
                            runCatching { BitmapFactory.decodeFile(item.filePath)?.asImageBitmap() }.getOrNull()
                        }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = item.fileName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Unable to decode image", color = Color.White)
                        }
                    } else {
                        var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
                        var playbackError by remember(item.filePath) { mutableStateOf(false) }
                        DisposableEffect(item.filePath) {
                            onDispose {
                                runCatching { videoViewRef?.stopPlayback() }
                            }
                        }
                        if (playbackError) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Unable to play this video clip",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Button(
                                    onClick = onDelete,
                                    colors = ButtonDefaults.buttonColors(containerColor = MacroCoral)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Delete Broken Clip")
                                }
                            }
                        } else {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        videoViewRef = this
                                        val controller = MediaController(ctx)
                                        controller.setAnchorView(this)
                                        setMediaController(controller)
                                        setOnErrorListener { _, _, _ ->
                                            playbackError = true
                                            true
                                        }
                                        setVideoPath(item.filePath)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun loadThumbnail(item: CapturedMediaItem): ImageBitmap? {
    val file = File(item.filePath)
    if (!file.exists() || file.length() <= 1024L) return null
    return if (item.type == CaptureMediaType.SCREENSHOT) {
        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        runCatching { BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap() }.getOrNull()
    } else {
        if (item.durationMs <= 0L) return null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            if (hasVideo != "yes") return null
            retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.asImageBitmap()
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}

private fun shareMediaItem(context: Context, item: CapturedMediaItem) {
    val file = File(item.filePath)
    if (!file.exists()) return
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull() ?: return
    val mime = if (item.type == CaptureMediaType.SCREENSHOT) "image/png" else "video/mp4"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share ${item.type.label}"))
    }
}

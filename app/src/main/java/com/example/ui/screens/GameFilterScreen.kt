package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.filter.FreestyleGlTextureView
import com.example.model.ColorMatrixBuilder
import com.example.model.CrosshairPreset
import com.example.model.CrosshairSettings
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import com.example.model.LocalNexusStrings
import com.example.ui.components.CrosshairOverlayCanvas
import com.example.ui.components.CrosshairPanelContent
import com.example.ui.components.GeForceFilterPanelContent
import com.example.ui.components.StickerOverlayCanvas
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TacticalCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun GameFilterScreen(
    filterState: GameFilterState,
    isGpuShaderActive: Boolean,
    crosshair: CrosshairSettings,
    isOverlayRunning: Boolean,
    initialSection: Int = 0,
    onSelectSlot: (Int) -> Unit,
    onPreviewStyle: (GameFilterStyle) -> Unit,
    onUpdateStyle: (GameFilterStyle) -> Unit,
    onResetSlot: () -> Unit,
    onToggleGpuShader: () -> Unit,
    onPickStickerImage: (Uri) -> Unit,
    onUpdateCrosshair: (CrosshairSettings) -> Unit,
    onSelectCrosshairPreset: (CrosshairPreset) -> Unit,
    onResetCrosshair: () -> Unit,
    onPickCrosshairImage: (Uri) -> Unit,
    onToggleOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalNexusStrings.current
    var selectedSection by remember(initialSection) { mutableIntStateOf(initialSection.coerceIn(0, 1)) }
    LaunchedEffect(initialSection) {
        selectedSection = initialSection.coerceIn(0, 1)
    }

    var compareOriginal by remember { mutableStateOf(false) }
    var customPreviewUri by remember { mutableStateOf<Uri?>(null) }

    val activeStyle = filterState.activeStyle
    val appliedColorFilter = remember(activeStyle, compareOriginal) {
        if (!compareOriginal && activeStyle != null) {
            ColorFilter.colorMatrix(ColorMatrixBuilder.toComposeColorMatrix(activeStyle))
        } else {
            null
        }
    }

    val previewBitmap by produceState<Bitmap?>(initialValue = null, customPreviewUri) {
        value = withContext(Dispatchers.IO) {
            if (customPreviewUri != null) {
                decodePreviewBitmapFromUri(context, customPreviewUri!!)
                    ?: decodeDefaultArenaBitmap(context)
            } else {
                decodeDefaultArenaBitmap(context)
            }
        }
    }

    val previewImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customPreviewUri = uri
        }
    }

    val stickerImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickStickerImage(uri)
        }
    }

    val crosshairImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickCrosshairImage(uri)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 700.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Segmented Switcher (Game Filter vs Crosshair)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedSection == 0) strings.visualsGameFilterTab else strings.crosshairTitle,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = if (selectedSection == 0) {
                                    "18-module OpenGL ES Freestyle GPU pixel shader suite"
                                } else {
                                    "Adjust reticle shape, color, outline, gap, thickness & custom image"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onToggleOverlay,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOverlayRunning) MacroCoral else GeForceLime,
                                contentColor = if (isOverlayRunning) Color.White else Color(0xFF0A1400)
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("filters_overlay_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isOverlayRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isOverlayRunning) "Stop" else "Overlay")
                        }
                    }

                    // Mode Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple(0, strings.visualsGameFilterTab, Icons.Default.Tune),
                            Triple(1, strings.visualsCrosshairTab, Icons.Default.CenterFocusStrong)
                        ).forEach { (idx, label, icon) ->
                            val selected = selectedSection == idx
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (selected) GeForceLime else Color.Transparent)
                                    .clickable { selectedSection = idx }
                                    .testTag("visuals_section_tab_$idx")
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (selected) Color(0xFF091200) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    color = if (selected) Color(0xFF091200) else MaterialTheme.colorScheme.onSurface,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            if (selectedSection == 0) {
                // Real OpenGL ES 2.0 Live GPU Shader Preview + Interactive Splitscreen Handle
                item {
                    LiveGpuShaderPreviewCard(
                        bitmap = previewBitmap,
                        activeStyle = activeStyle,
                        compareOriginal = compareOriginal,
                        isGpuShaderActive = isGpuShaderActive,
                        onToggleCompare = { compareOriginal = !compareOriginal },
                        onPickPhoto = {
                            previewImagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onPreviewSplitPosition = { pos ->
                            val current = activeStyle ?: return@LiveGpuShaderPreviewCard
                            onPreviewStyle(current.copy(splitscreenPosition = pos))
                        },
                        onCommitSplitPosition = { pos ->
                            val current = activeStyle ?: return@LiveGpuShaderPreviewCard
                            onUpdateStyle(current.copy(splitscreenPosition = pos))
                        }
                    )
                }

                // Full 18-Module GeForce Experience Freestyle Panel
                item {
                    GeForceFilterPanelContent(
                        filterState = filterState,
                        onSelectSlot = onSelectSlot,
                        onUpdateStyle = onUpdateStyle,
                        onResetSlot = onResetSlot,
                        onPreviewStyle = onPreviewStyle,
                        isGpuShaderActive = isGpuShaderActive,
                        onToggleGpuShader = onToggleGpuShader,
                        onUploadStickerClick = {
                            stickerImagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        compactMode = false
                    )
                }
            } else {
                // Live Crosshair HUD Preview Box
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(184.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.5.dp,
                                if (crosshair.enabled) GeForceLime else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_game_arena_bg),
                            contentDescription = "Crosshair Live Preview",
                            contentScale = ContentScale.Crop,
                            colorFilter = appliedColorFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x55080B10))
                        )
                        CrosshairOverlayCanvas(settings = crosshair)
                        Surface(
                            color = Color(0xCC0A0E14),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .border(1.dp, Color(0xFF2B384A), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = if (crosshair.enabled) {
                                    "CROSSHAIR: ${crosshair.style.displayName.uppercase()}"
                                } else {
                                    "CROSSHAIR: OFF"
                                },
                                color = if (crosshair.enabled) GeForceLime else Color(0xFFB4C3D6),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item {
                    CrosshairPanelContent(
                        crosshair = crosshair,
                        onUpdate = onUpdateCrosshair,
                        onSelectPreset = onSelectCrosshairPreset,
                        onReset = onResetCrosshair,
                        onUploadCustomImageClick = {
                            crosshairImagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        compactMode = false
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveGpuShaderPreviewCard(
    bitmap: Bitmap?,
    activeStyle: GameFilterStyle?,
    compareOriginal: Boolean,
    isGpuShaderActive: Boolean,
    onToggleCompare: () -> Unit,
    onPickPhoto: () -> Unit,
    onPreviewSplitPosition: (Float) -> Unit,
    onCommitSplitPosition: (Float) -> Unit
) {
    val effectiveStyle = if (compareOriginal) null else activeStyle
    var glViewRef by remember { mutableStateOf<FreestyleGlTextureView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            glViewRef?.setFilterStyle(null)
            glViewRef = null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0E14))
            .border(
                1.5.dp,
                when {
                    isGpuShaderActive && effectiveStyle != null -> GeForceLime
                    effectiveStyle != null -> TacticalCyan
                    else -> MaterialTheme.colorScheme.outline
                },
                RoundedCornerShape(16.dp)
            )
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }

        // Real OpenGL ES 2.0 GLSL Fragment Shader Surface
        AndroidView(
            factory = { ctx ->
                FreestyleGlTextureView(ctx, FreestyleGlTextureView.Mode.BITMAP).apply {
                    glViewRef = this
                    setSourceBitmap(bitmap)
                    setFilterStyle(effectiveStyle)
                }
            },
            update = { view ->
                view.setSourceBitmap(bitmap)
                view.setFilterStyle(effectiveStyle)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Stickers Layer (rendered identically over the shader output)
        if (effectiveStyle != null && effectiveStyle.stickersEnabled) {
            StickerOverlayCanvas(
                style = effectiveStyle,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Interactive draggable Splitscreen divider handle when Splitscreen is enabled
        if (effectiveStyle != null && effectiveStyle.splitscreenEnabled) {
            val latestPreview by rememberUpdatedState(onPreviewSplitPosition)
            val latestCommit by rememberUpdatedState(onCommitSplitPosition)
            var dragPos by remember(effectiveStyle.slot) {
                mutableFloatStateOf(effectiveStyle.splitscreenPosition.coerceIn(0.05f, 0.95f))
            }
            var isDraggingSplit by remember { mutableStateOf(false) }

            LaunchedEffect(effectiveStyle.splitscreenPosition, isDraggingSplit) {
                if (!isDraggingSplit) {
                    dragPos = effectiveStyle.splitscreenPosition.coerceIn(0.05f, 0.95f)
                }
            }

            val handleX = (dragPos * widthPx).roundToInt()
            val handleHalfPx = with(density) { 20.dp.roundToPx() }

            Box(
                modifier = Modifier
                    .offset { IntOffset((handleX - handleHalfPx).coerceIn(0, (widthPx.roundToInt() - handleHalfPx * 2).coerceAtLeast(0)), 0) }
                    .width(40.dp)
                    .fillMaxHeight()
                    .pointerInput(widthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = { isDraggingSplit = true },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val next = (dragPos + dragAmount / widthPx).coerceIn(0.05f, 0.95f)
                                dragPos = next
                                latestPreview(next)
                            },
                            onDragEnd = {
                                isDraggingSplit = false
                                latestCommit(dragPos)
                            },
                            onDragCancel = {
                                isDraggingSplit = false
                                latestCommit(dragPos)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xEE0B1018))
                        .border(1.5.dp, GeForceLime, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Drag Splitscreen Divider",
                        tint = GeForceLime,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ORIGINAL vs FILTERED split badges
            Surface(
                color = Color(0xAA080B10),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, bottom = 34.dp)
            ) {
                Text(
                    text = "ORIGINAL",
                    color = Color(0xFFB4C3D6),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Surface(
                color = Color(0xAA0A1604),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 34.dp)
            ) {
                Text(
                    text = "FILTERED",
                    color = GeForceLime,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Subtle top/bottom gradient scrim for legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(listOf(Color(0x88080B10), Color.Transparent))
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xB8080B10)))
                )
        )

        // Status badge (top-left)
        Surface(
            color = Color(0xCC0A0E14),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .border(1.dp, Color(0xFF2B384A), RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = if (effectiveStyle != null) GeForceLime else Color(0xFF8C9EB5),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = when {
                        compareOriginal -> "GLSL PREVIEW: ORIGINAL"
                        activeStyle != null -> "GLSL S${activeStyle.slot} • ${activeStyle.activeModuleCount()} MODULES"
                        else -> "FILTER: OFF"
                    },
                    color = if (effectiveStyle != null) GeForceLime else Color(0xFFB4C3D6),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }

        // Top-right actions: Pick Photo + Compare
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xCC0A0E14),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF334257), RoundedCornerShape(8.dp))
                    .clickable(onClick = onPickPhoto)
                    .testTag("filter_pick_preview_photo_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Pick Preview Screenshot",
                        tint = TacticalCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Photo",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                color = if (compareOriginal) TacticalCyan else Color(0xCC0A0E14),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (compareOriginal) TacticalCyan else Color(0xFF334257),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onToggleCompare)
                    .testTag("filter_compare_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = "Compare Original",
                        tint = if (compareOriginal) Color(0xFF001B21) else Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (compareOriginal) "Original" else "Compare",
                        color = if (compareOriginal) Color(0xFF001B21) else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom status strip
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = if (isGpuShaderActive) GeForceLime else TacticalCyan,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isGpuShaderActive) {
                    "Live GPU Shader Overlay ACTIVE — real per-pixel GLSL filtering on your game"
                } else {
                    "Real OpenGL ES 2.0 shader preview • Enable Live GPU Shader below for in-game pixels"
                },
                color = Color(0xFFE3ECF7),
                fontSize = 10.sp
            )
        }
    }
}

private fun decodeDefaultArenaBitmap(context: Context): Bitmap? {
    return runCatching {
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeResource(context.resources, R.drawable.img_game_arena_bg, opts)
    }.getOrNull()
}

private fun decodePreviewBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val maxSide = maxOf(info.size.width, info.size.height)
                if (maxSide > 1280) {
                    val scale = 1280f / maxSide.toFloat()
                    decoder.setTargetSize(
                        (info.size.width * scale).roundToInt().coerceAtLeast(1),
                        (info.size.height * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
        if (raw != null && raw.config != Bitmap.Config.ARGB_8888) {
            raw.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            raw
        }
    }.getOrNull()
}

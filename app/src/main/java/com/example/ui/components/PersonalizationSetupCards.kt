package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppColorSchemeConfig
import com.example.model.AppFontConfig
import com.example.model.AppFontPreset
import com.example.model.AppThemePreset
import com.example.model.FloatingBubbleConfig
import com.example.model.FloatingBubbleStyle
import com.example.model.LocalNexusStrings
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

val HexagonBubbleShape: Shape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val rx = size.width * 0.48f
    val ry = size.height * 0.48f
    for (i in 0 until 6) {
        val angleRad = Math.toRadians((60.0 * i) - 30.0)
        val px = cx + (rx * cos(angleRad)).toFloat()
        val py = cy + (ry * sin(angleRad)).toFloat()
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

@Composable
fun FloatingBubbleVisual(
    config: FloatingBubbleConfig,
    accentColor: Color,
    isPanelOpen: Boolean = false,
    isRecording: Boolean = false,
    isReplayOn: Boolean = false,
    isLocked: Boolean = false,
    isHidden: Boolean = false,
    modifier: Modifier = Modifier
) {
    val customBitmap by produceState<ImageBitmap?>(initialValue = null, config.customIconPath) {
        value = if (config.customIconPath.isNotBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(config.customIconPath)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
                }.getOrNull()
            }
        } else {
            null
        }
    }

    val bubbleShape: Shape = when (config.style) {
        FloatingBubbleStyle.TACTICAL_RING -> CircleShape
        FloatingBubbleStyle.CYBER_PILL -> RoundedCornerShape(50)
        FloatingBubbleStyle.HEX_BADGE -> HexagonBubbleShape
        FloatingBubbleStyle.STEALTH_ORB -> CircleShape
        FloatingBubbleStyle.CUSTOM_IMAGE -> CircleShape
    }

    val bgBrush = when (config.style) {
        FloatingBubbleStyle.STEALTH_ORB -> Brush.radialGradient(
            colors = listOf(Color(0x99121822), Color(0xBB090C11))
        )
        FloatingBubbleStyle.CYBER_PILL -> Brush.horizontalGradient(
            colors = listOf(Color(0xF0172233), Color(0xF00B1018))
        )
        else -> Brush.radialGradient(
            colors = listOf(Color(0xEE182130), Color(0xEE0B0E14))
        )
    }

    val borderWidth = when (config.style) {
        FloatingBubbleStyle.STEALTH_ORB -> 1.2.dp
        FloatingBubbleStyle.HEX_BADGE -> 2.2.dp
        else -> 2.dp
    }
    val iconSize = (config.clampedSizeDp() * 0.44f).coerceIn(16f, 38f).dp

    Box(
        modifier = modifier
            .alpha(config.clampedOpacity())
            .padding(2.dp)
            .clip(bubbleShape)
            .background(bgBrush)
            .border(
                width = borderWidth,
                color = if (config.style == FloatingBubbleStyle.STEALTH_ORB) accentColor.copy(alpha = 0.65f) else accentColor,
                shape = bubbleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (config.style == FloatingBubbleStyle.CUSTOM_IMAGE && customBitmap != null && !isPanelOpen) {
            Image(
                bitmap = customBitmap!!,
                contentDescription = "Custom Floating Bubble",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (config.style == FloatingBubbleStyle.CYBER_PILL) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = if (isPanelOpen) Icons.Default.Close else Icons.Default.Gamepad,
                    contentDescription = "Open NexusPad Menu",
                    tint = accentColor,
                    modifier = Modifier.size((iconSize.value * 0.85f).dp)
                )
                if (!isPanelOpen) {
                    Text(
                        text = "HUD",
                        color = accentColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (config.clampedSizeDp() * 0.19f).coerceIn(9f, 14f).sp
                    )
                }
            }
        } else {
            if (config.style == FloatingBubbleStyle.TACTICAL_RING) {
                Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.25f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
            Icon(
                imageVector = if (isPanelOpen) Icons.Default.Close else Icons.Default.Gamepad,
                contentDescription = "Open NexusPad Menu",
                tint = accentColor,
                modifier = Modifier.size(iconSize)
            )
        }

        if (config.showStatusDot && !isPanelOpen) {
            val topDotColor = when {
                isRecording -> MacroCoral
                isReplayOn -> TacticalCyan
                else -> null
            }
            if (topDotColor != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(topDotColor)
                )
            }
            val bottomDotColor = when {
                isLocked -> TurboAmber
                isHidden -> Color(0xFF9FB0C7)
                else -> null
            }
            if (bottomDotColor != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(bottomDotColor)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppColorSchemeCard(
    config: AppColorSchemeConfig,
    onConfigChange: (AppColorSchemeConfig) -> Unit
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ColorLens, contentDescription = null, tint = primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.themeCardTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = primary
                    )
                    Text(
                        text = strings.themeCardSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live 2–3 Color Blend Bar Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(primary, secondary, tertiary)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            )

            Text(
                text = strings.themePresetsLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppThemePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = config.preset == preset,
                        onClick = {
                            if (preset == AppThemePreset.CUSTOM_MIX) {
                                onConfigChange(config.copy(preset = AppThemePreset.CUSTOM_MIX))
                            } else {
                                onConfigChange(
                                    config.copy(
                                        preset = preset,
                                        customPrimaryHex = preset.primaryHex,
                                        customSecondaryHex = preset.secondaryHex,
                                        customTertiaryHex = preset.tertiaryHex
                                    )
                                )
                            }
                        },
                        leadingIcon = {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(preset.primaryHex))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(preset.secondaryHex))
                                )
                            }
                        },
                        label = {
                            Text(
                                if (preset == AppThemePreset.CUSTOM_MIX) strings.themeCustomMixTitle else preset.displayName
                            )
                        },
                        modifier = Modifier.testTag("setup_theme_preset_${preset.name.lowercase()}")
                    )
                }
            }

            if (config.preset == AppThemePreset.CUSTOM_MIX) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.themeCustomMixTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = !config.useThreeColors,
                                    onClick = { onConfigChange(config.copy(useThreeColors = false)) },
                                    label = { Text(strings.themeMix2Colors) },
                                    modifier = Modifier.testTag("setup_theme_mix_2")
                                )
                                FilterChip(
                                    selected = config.useThreeColors,
                                    onClick = { onConfigChange(config.copy(useThreeColors = true)) },
                                    label = { Text(strings.themeMix3Colors) },
                                    modifier = Modifier.testTag("setup_theme_mix_3")
                                )
                            }
                        }

                        ColorSwatchPickerRow(
                            label = strings.themePrimaryColor,
                            selectedHex = config.customPrimaryHex,
                            onSelect = { hex ->
                                onConfigChange(
                                    config.copy(
                                        preset = AppThemePreset.CUSTOM_MIX,
                                        customPrimaryHex = hex
                                    )
                                )
                            }
                        )

                        ColorSwatchPickerRow(
                            label = strings.themeSecondaryColor,
                            selectedHex = config.customSecondaryHex,
                            onSelect = { hex ->
                                onConfigChange(
                                    config.copy(
                                        preset = AppThemePreset.CUSTOM_MIX,
                                        customSecondaryHex = hex
                                    )
                                )
                            }
                        )

                        if (config.useThreeColors) {
                            ColorSwatchPickerRow(
                                label = strings.themeTertiaryColor,
                                selectedHex = config.customTertiaryHex,
                                onSelect = { hex ->
                                    onConfigChange(
                                        config.copy(
                                            preset = AppThemePreset.CUSTOM_MIX,
                                            customTertiaryHex = hex
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatchPickerRow(
    label: String,
    selectedHex: Long,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppColorSchemeConfig.swatchColors.forEach { hex ->
                val isSelected = hex == selectedHex
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF0A0D12),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FontCustomizationCard(
    config: AppFontConfig,
    onConfigChange: (AppFontConfig) -> Unit,
    onPickCustomFont: (Uri) -> Unit
) {
    val strings = LocalNexusStrings.current
    val secondary = MaterialTheme.colorScheme.secondary

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onPickCustomFont(uri)
        }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FontDownload, contentDescription = null, tint = secondary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.fontCardTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondary
                    )
                    Text(
                        text = strings.fontCardSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppFontPreset.entries.filter { it != AppFontPreset.CUSTOM_UPLOAD }.forEach { preset ->
                    FilterChip(
                        selected = config.preset == preset,
                        onClick = { onConfigChange(config.copy(preset = preset)) },
                        label = { Text(preset.displayName) },
                        modifier = Modifier.testTag("setup_font_${preset.name.lowercase()}")
                    )
                }
                if (config.customFontPath.isNotBlank()) {
                    FilterChip(
                        selected = config.preset == AppFontPreset.CUSTOM_UPLOAD,
                        onClick = { onConfigChange(config.copy(preset = AppFontPreset.CUSTOM_UPLOAD)) },
                        label = {
                            Text(
                                config.customFontName.ifBlank { strings.fontUploadedLabel }
                            )
                        },
                        modifier = Modifier.testTag("setup_font_custom_uploaded")
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    fontPickerLauncher.launch(
                        arrayOf(
                            "font/ttf",
                            "font/otf",
                            "application/x-font-ttf",
                            "application/x-font-opentype",
                            "application/octet-stream",
                            "*/*"
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("setup_upload_font_button")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.fontUploadButton)
            }

            // Live typography preview box
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = strings.fontSamplePreview,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (config.preset == AppFontPreset.CUSTOM_UPLOAD && config.customFontName.isNotBlank()) {
                            "${strings.fontUploadedLabel}: ${config.customFontName}"
                        } else {
                            "${config.preset.displayName} • ${config.preset.subtitle}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingBubbleCustomizationCard(
    config: FloatingBubbleConfig,
    onConfigChange: (FloatingBubbleConfig) -> Unit,
    onPickCustomBubbleIcon: (Uri) -> Unit
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val effectiveAccent = if (config.useThemeAccent) primary else Color(config.customAccentHex)

    val bubbleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickCustomBubbleIcon(uri)
        }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.BubbleChart, contentDescription = null, tint = primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.bubbleCardTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = primary
                    )
                    Text(
                        text = strings.bubbleCardSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Bubble Preview Stage
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.bubbleLivePreview,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${config.style.displayName} • ${config.clampedSizeDp().roundToInt()}dp • ${(config.clampedOpacity() * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val previewW = if (config.style == FloatingBubbleStyle.CYBER_PILL) {
                        (config.clampedSizeDp() * 1.45f).dp
                    } else {
                        config.clampedSizeDp().dp
                    }
                    val previewH = if (config.style == FloatingBubbleStyle.CYBER_PILL) {
                        (config.clampedSizeDp() * 0.86f).dp
                    } else {
                        config.clampedSizeDp().dp
                    }
                    FloatingBubbleVisual(
                        config = config,
                        accentColor = effectiveAccent,
                        isReplayOn = true,
                        modifier = Modifier.size(width = previewW, height = previewH)
                    )
                }
            }

            Text(
                text = strings.bubbleStyleLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FloatingBubbleStyle.entries.forEach { style ->
                    FilterChip(
                        selected = config.style == style,
                        onClick = { onConfigChange(config.copy(style = style)) },
                        label = { Text(style.displayName) },
                        modifier = Modifier.testTag("setup_bubble_style_${style.name.lowercase()}")
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    bubbleImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("setup_upload_bubble_icon_button")
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.bubbleUploadIconButton)
            }

            // Bubble Size slider
            Column {
                var sizeDraft by remember { mutableFloatStateOf(config.clampedSizeDp()) }
                var sizeDragging by remember { mutableStateOf(false) }
                LaunchedEffect(config.sizeDp, sizeDragging) {
                    if (!sizeDragging) sizeDraft = config.clampedSizeDp()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.bubbleSizeLabel, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${sizeDraft.roundToInt()} dp",
                        style = MaterialTheme.typography.labelMedium,
                        color = primary
                    )
                }
                SmoothSlider(
                    value = config.clampedSizeDp(),
                    onValueChange = { v ->
                        sizeDragging = true
                        sizeDraft = v
                    },
                    onValueChangeFinished = { v ->
                        sizeDragging = false
                        sizeDraft = v
                        onConfigChange(config.copy(sizeDp = v.coerceIn(36f, 88f)))
                    },
                    valueRange = 36f..88f,
                    accentColor = primary,
                    modifier = Modifier.testTag("setup_bubble_size_slider")
                )
            }

            // Bubble Opacity slider
            Column {
                var alphaDraft by remember { mutableFloatStateOf(config.clampedOpacity()) }
                var alphaDragging by remember { mutableStateOf(false) }
                LaunchedEffect(config.opacity, alphaDragging) {
                    if (!alphaDragging) alphaDraft = config.clampedOpacity()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.bubbleOpacityLabel, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${(alphaDraft * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = primary
                    )
                }
                SmoothSlider(
                    value = config.clampedOpacity(),
                    onValueChange = { v ->
                        alphaDragging = true
                        alphaDraft = v
                    },
                    onValueChangeFinished = { v ->
                        alphaDragging = false
                        alphaDraft = v
                        onConfigChange(config.copy(opacity = v.coerceIn(0.25f, 1.0f)))
                    },
                    valueRange = 0.25f..1.0f,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("setup_bubble_opacity_slider")
                )
            }

            // Status dot toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConfigChange(config.copy(showStatusDot = !config.showStatusDot)) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(strings.bubbleStatusDotLabel, style = MaterialTheme.typography.titleSmall)
                    Text(
                        strings.bubbleStatusDotSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.showStatusDot,
                    onCheckedChange = { onConfigChange(config.copy(showStatusDot = it)) },
                    modifier = Modifier.testTag("setup_bubble_status_dot_switch")
                )
            }

            // Match App Theme toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConfigChange(config.copy(useThemeAccent = !config.useThemeAccent)) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(strings.bubbleUseThemeAccent, style = MaterialTheme.typography.titleSmall)
                    Text(
                        strings.bubbleUseThemeAccentSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.useThemeAccent,
                    onCheckedChange = { onConfigChange(config.copy(useThemeAccent = it)) },
                    modifier = Modifier.testTag("setup_bubble_theme_accent_switch")
                )
            }

            if (!config.useThemeAccent) {
                ColorSwatchPickerRow(
                    label = strings.bubbleCustomColorLabel,
                    selectedHex = config.customAccentHex,
                    onSelect = { hex -> onConfigChange(config.copy(customAccentHex = hex)) }
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CrosshairPreset
import com.example.model.CrosshairPresets
import com.example.model.CrosshairSettings
import com.example.model.CrosshairStyle
import com.example.model.LocalNexusStrings
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TacticalCyan
import com.example.util.ButtonImageCodec
import kotlin.math.roundToInt

@Composable
fun CrosshairOverlayCanvas(
    settings: CrosshairSettings,
    modifier: Modifier = Modifier
) {
    if (!settings.enabled) return

    val customBitmap = remember(settings.customImageBase64) {
        ButtonImageCodec.decodeBase64ToImageBitmap(settings.customImageBase64)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(settings.opacity.coerceIn(0.15f, 1.0f)),
        contentAlignment = Alignment.Center
    ) {
        if (settings.style == CrosshairStyle.CUSTOM_IMAGE && customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = "Custom Crosshair",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            settings.offsetXdp.dp.roundToPx(),
                            settings.offsetYdp.dp.roundToPx()
                        )
                    }
                    .size(settings.customImageSizeDp.coerceIn(16f, 112f).dp)
            )
            return@Box
        }

        val mainColor = Color(settings.colorHex)
        val outlineColor = Color(settings.outlineColorHex)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f + settings.offsetXdp.dp.toPx()
            val cy = size.height / 2f + settings.offsetYdp.dp.toPx()
            val thick = settings.thicknessDp.coerceIn(1f, 8f).dp.toPx()
            val len = settings.lengthDp.coerceIn(3f, 26f).dp.toPx()
            val gap = settings.gapDp.coerceIn(0f, 22f).dp.toPx()
            val outlineExtra = if (settings.outlineEnabled) {
                settings.outlineThicknessDp.coerceIn(0.5f, 4f).dp.toPx() * 2f
            } else {
                0f
            }

            fun drawArm(start: Offset, end: Offset) {
                if (settings.outlineEnabled) {
                    drawLine(
                        color = outlineColor,
                        start = start,
                        end = end,
                        strokeWidth = thick + outlineExtra,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(
                    color = mainColor,
                    start = start,
                    end = end,
                    strokeWidth = thick,
                    cap = StrokeCap.Round
                )
            }

            when (settings.style) {
                CrosshairStyle.CLASSIC_PLUS -> {
                    drawArm(Offset(cx - gap - len, cy), Offset(cx - gap, cy))
                    drawArm(Offset(cx + gap, cy), Offset(cx + gap + len, cy))
                    drawArm(Offset(cx, cy - gap - len), Offset(cx, cy - gap))
                    drawArm(Offset(cx, cy + gap), Offset(cx, cy + gap + len))
                }
                CrosshairStyle.T_SHAPE -> {
                    drawArm(Offset(cx - gap - len, cy), Offset(cx - gap, cy))
                    drawArm(Offset(cx + gap, cy), Offset(cx + gap + len, cy))
                    drawArm(Offset(cx, cy + gap), Offset(cx, cy + gap + len))
                }
                CrosshairStyle.X_MARK -> {
                    val diag = 0.7071f
                    drawArm(
                        Offset(cx - (gap + len) * diag, cy - (gap + len) * diag),
                        Offset(cx - gap * diag, cy - gap * diag)
                    )
                    drawArm(
                        Offset(cx + gap * diag, cy + gap * diag),
                        Offset(cx + (gap + len) * diag, cy + (gap + len) * diag)
                    )
                    drawArm(
                        Offset(cx + gap * diag, cy - gap * diag),
                        Offset(cx + (gap + len) * diag, cy - (gap + len) * diag)
                    )
                    drawArm(
                        Offset(cx - gap * diag, cy + gap * diag),
                        Offset(cx - (gap + len) * diag, cy + (gap + len) * diag)
                    )
                }
                CrosshairStyle.CIRCLE_DOT -> {
                    val radius = (gap + len * 0.75f).coerceAtLeast(6.dp.toPx())
                    if (settings.outlineEnabled) {
                        drawCircle(
                            color = outlineColor,
                            radius = radius,
                            center = Offset(cx, cy),
                            style = Stroke(width = thick + outlineExtra)
                        )
                    }
                    drawCircle(
                        color = mainColor,
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = thick)
                    )
                }
                CrosshairStyle.DOT_ONLY, CrosshairStyle.CUSTOM_IMAGE -> Unit
            }

            if (settings.centerDot || settings.style == CrosshairStyle.DOT_ONLY) {
                val dotR = (thick * 0.65f).coerceAtLeast(1.8.dp.toPx())
                if (settings.outlineEnabled) {
                    drawCircle(
                        color = outlineColor,
                        radius = dotR + outlineExtra * 0.5f,
                        center = Offset(cx, cy)
                    )
                }
                drawCircle(
                    color = mainColor,
                    radius = dotR,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@Composable
fun CrosshairPanelContent(
    crosshair: CrosshairSettings,
    onUpdate: (CrosshairSettings) -> Unit,
    onSelectPreset: (CrosshairPreset) -> Unit,
    onReset: () -> Unit,
    onUploadCustomImageClick: () -> Unit,
    compactMode: Boolean = false,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    val customBitmap = remember(crosshair.customImageBase64) {
        ButtonImageCodec.decodeBase64ToImageBitmap(crosshair.customImageBase64)
    }

    Surface(
        color = Color(0xF00E1218),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(1.2.dp, Color(0xFF2B3748), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compactMode) 12.dp else 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = if (crosshair.enabled) GeForceLime else Color(0xFF7D8FA6),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = strings.crosshairTitle,
                        color = Color(0xFFF3F7FC),
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compactMode) 14.sp else 16.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("crosshair_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = strings.crosshairReset,
                            tint = Color(0xFFD5E1F0)
                        )
                    }
                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("crosshair_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Crosshair Panel",
                                tint = Color(0xFFD5E1F0)
                            )
                        }
                    }
                }
            }

            // Master Enable Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF151B24))
                    .clickable { onUpdate(crosshair.copy(enabled = !crosshair.enabled)) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.crosshairEnable,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = crosshair.enabled,
                    onCheckedChange = { onUpdate(crosshair.copy(enabled = it)) },
                    modifier = Modifier.testTag("crosshair_enable_switch")
                )
            }

            // Presets Row
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = strings.crosshairPresets,
                    color = Color(0xFF9FB0C7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CrosshairPresets.presets, key = { it.id }) { preset ->
                        FilterChip(
                            selected = false,
                            onClick = { onSelectPreset(preset) },
                            label = { Text(preset.name, fontSize = 11.sp) },
                            modifier = Modifier.testTag("crosshair_preset_${preset.id}")
                        )
                    }
                }
            }

            // Shape selector
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(CrosshairStyle.entries) { style ->
                    FilterChip(
                        selected = crosshair.style == style,
                        onClick = { onUpdate(crosshair.copy(enabled = true, style = style)) },
                        label = { Text(style.displayName, fontSize = 11.sp) }
                    )
                }
            }

            // Custom Upload Image section
            Surface(
                color = Color(0xFF151B24),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF283446), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0A0F16))
                            .border(1.dp, Color(0xFF314057), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (customBitmap != null) {
                            Image(
                                bitmap = customBitmap,
                                contentDescription = "Custom crosshair preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(4.dp)
                            )
                        } else {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF7D8FA6))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.crosshairUploadImage,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (customBitmap != null) "PNG / image active" else "Custom reticle art",
                            color = Color(0xFF8C9EB5),
                            fontSize = 10.sp
                        )
                    }
                    OutlinedButton(
                        onClick = onUploadCustomImageClick,
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("crosshair_upload_image_button")
                    ) {
                        Text(strings.crosshairUploadImage, fontSize = 11.sp)
                    }
                    if (customBitmap != null) {
                        IconButton(
                            onClick = {
                                onUpdate(
                                    crosshair.copy(
                                        customImageBase64 = null,
                                        style = CrosshairStyle.CLASSIC_PLUS
                                    )
                                )
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Remove Crosshair Image",
                                tint = Color(0xFFFF5277)
                            )
                        }
                    }
                }
            }

            if (crosshair.style == CrosshairStyle.CUSTOM_IMAGE && customBitmap != null) {
                CrosshairSlider(
                    label = strings.crosshairSize,
                    value = crosshair.customImageSizeDp,
                    range = 16f..96f,
                    unit = "dp",
                    accent = GeForceLime,
                    onValueChange = { onUpdate(crosshair.copy(customImageSizeDp = it)) }
                )
            } else {
                // Color Swatches
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.crosshairColor,
                        color = Color(0xFFD6E0EE),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CrosshairPresets.colorSwatches.forEach { (name, hex) ->
                            val selected = crosshair.colorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { onUpdate(crosshair.copy(enabled = true, colorHex = hex)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) Color.White else Color(0xFF334154),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = name,
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Outline Switch + Outline Swatches & Thickness
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF151B24))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.crosshairOutline,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CrosshairPresets.outlineSwatches.forEach { (name, hex) ->
                                val sel = crosshair.outlineColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .border(
                                            width = if (sel) 2.dp else 1.dp,
                                            color = if (sel) GeForceLime else Color(0xFF47576E),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onUpdate(crosshair.copy(outlineEnabled = true, outlineColorHex = hex))
                                        }
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Switch(
                                checked = crosshair.outlineEnabled,
                                onCheckedChange = { onUpdate(crosshair.copy(outlineEnabled = it)) }
                            )
                        }
                    }
                    if (crosshair.outlineEnabled) {
                        CrosshairSlider(
                            label = "${strings.crosshairOutline} Width",
                            value = crosshair.outlineThicknessDp,
                            range = 0.5f..4.0f,
                            unit = "dp",
                            accent = TacticalCyan,
                            decimals = true,
                            onValueChange = { onUpdate(crosshair.copy(outlineThicknessDp = it)) }
                        )
                    }
                }

                // Thickness, Length, Gap, Opacity & Center Dot
                CrosshairSlider(
                    label = strings.crosshairThickness,
                    value = crosshair.thicknessDp,
                    range = 1.0f..8.0f,
                    unit = "dp",
                    accent = GeForceLime,
                    decimals = true,
                    onValueChange = { onUpdate(crosshair.copy(thicknessDp = it)) }
                )
                if (crosshair.style != CrosshairStyle.DOT_ONLY) {
                    CrosshairSlider(
                        label = strings.crosshairLength,
                        value = crosshair.lengthDp,
                        range = 3.0f..24.0f,
                        unit = "dp",
                        accent = GeForceLime,
                        decimals = true,
                        onValueChange = { onUpdate(crosshair.copy(lengthDp = it)) }
                    )
                    CrosshairSlider(
                        label = strings.crosshairGap,
                        value = crosshair.gapDp,
                        range = 0.0f..20.0f,
                        unit = "dp",
                        accent = TacticalCyan,
                        decimals = true,
                        onValueChange = { onUpdate(crosshair.copy(gapDp = it)) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable { onUpdate(crosshair.copy(centerDot = !crosshair.centerDot)) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.crosshairCenterDot,
                        color = Color(0xFFD6E0EE),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = crosshair.centerDot,
                        onCheckedChange = { onUpdate(crosshair.copy(centerDot = it)) }
                    )
                }
            }

            CrosshairSlider(
                label = strings.crosshairOpacity,
                value = crosshair.opacity * 100f,
                range = 20f..100f,
                unit = "%",
                accent = GeForceLime,
                onValueChange = { onUpdate(crosshair.copy(opacity = (it / 100f).coerceIn(0.2f, 1.0f))) }
            )
        }
    }
}

@Composable
private fun CrosshairSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    accent: Color,
    decimals: Boolean = false,
    onValueChange: (Float) -> Unit
) {
    val clamped = value.coerceIn(range.start, range.endInclusive)
    var displayVal by remember { mutableFloatStateOf(clamped) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(clamped, dragging) {
        if (!dragging) displayVal = clamped
    }

    val text = if (decimals) {
        "%.1f %s".format(displayVal, unit)
    } else {
        "${displayVal.roundToInt()} $unit"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color(0xFFD6E0EE),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = text,
                color = accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
        SmoothSlider(
            value = clamped,
            onValueChange = { v ->
                dragging = true
                displayVal = v
                onValueChange(v)
            },
            onValueChangeFinished = { v ->
                dragging = false
                displayVal = v
                onValueChange(v)
            },
            valueRange = range,
            accentColor = accent,
            inactiveTrackColor = Color(0xFF283344),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FilterTintPreset
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TacticalCyan
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Computes Android WindowManager dimAmount for negative brightness so underlying
 * cloud game streams darken smoothly while NexusPad controls above stay bright.
 */
fun computeOverlayDimAmount(style: GameFilterStyle?): Float {
    if (style == null) return 0f
    val darkFromBrightness = (-style.brightness).coerceAtLeast(0f) * 0.85f
    val darkFromNegativeShadows = (-style.shadows).coerceAtLeast(0f) * 0.45f
    return (darkFromBrightness + darkFromNegativeShadows).coerceIn(0f, 0.58f)
}

/**
 * Full-screen non-touchable screen filter layer rendered underneath virtual buttons
 * and the floating bubble in OverlayControllerService.
 */
@Composable
fun ScreenFilterOverlayCanvas(
    style: GameFilterStyle,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // 1. Shadows lift / positive brightness veil
        val liftStrength = (style.shadows.coerceAtLeast(0f) * 0.14f) +
            (style.brightness.coerceAtLeast(0f) * 0.18f)
        if (liftStrength > 0.004f) {
            drawRect(
                color = Color.White.copy(alpha = liftStrength.coerceIn(0f, 0.24f))
            )
        }

        // 2. Fallback subtle dark tint when brightness/shadows are negative
        val darkStrength = ((-style.brightness).coerceAtLeast(0f) * 0.22f) +
            ((-style.shadows).coerceAtLeast(0f) * 0.18f)
        if (darkStrength > 0.004f) {
            drawRect(
                color = Color.Black.copy(alpha = darkStrength.coerceIn(0f, 0.34f))
            )
        }

        // 3. Contrast edge framing (radial vignette / crisp center pop)
        val contrastAmount = style.contrast
        if (contrastAmount > 0.02f) {
            val edgeAlpha = (contrastAmount * 0.30f).coerceIn(0f, 0.22f)
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.58f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = edgeAlpha)
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.maxDimension * 0.72f
                )
            )
        } else if (contrastAmount < -0.02f) {
            val hazeAlpha = (abs(contrastAmount) * 0.18f).coerceIn(0f, 0.14f)
            drawRect(color = Color(0xFF7D8594).copy(alpha = hazeAlpha))
        }

        // 4. Temperature shift (Warm Amber vs Cool Ice-Blue)
        val temp = style.temperature
        if (abs(temp) > 0.015f) {
            val tempColor = if (temp > 0f) Color(0xFFFF9838) else Color(0xFF39B8FF)
            val tempAlpha = (abs(temp) * 0.32f).coerceIn(0f, 0.20f)
            drawRect(color = tempColor.copy(alpha = tempAlpha))
        }

        // 5. Color / Vibrance crisp accent sheen
        if (style.colorVibrance > 0.05f) {
            val vibranceGlow = ((style.colorVibrance - 0.05f) * 0.06f).coerceIn(0f, 0.06f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = vibranceGlow * 0.45f),
                        Color.Transparent,
                        Color(0xFF8CE000).copy(alpha = vibranceGlow * 0.35f)
                    )
                )
            )
        } else if (style.colorVibrance < -0.05f) {
            val desatVeil = (abs(style.colorVibrance) * 0.34f).coerceIn(0f, 0.24f)
            drawRect(
                color = Color(0xFF808080).copy(alpha = desatVeil),
                blendMode = BlendMode.SrcOver
            )
        }

        // 6. Custom Tint Color + Intensity
        if (style.tintPreset != FilterTintPreset.NEUTRAL && style.tintIntensity > 0.01f) {
            val baseTint = Color(style.tintPreset.argbHex)
            drawRect(
                color = baseTint.copy(alpha = (style.tintIntensity * 0.48f).coerceIn(0f, 0.32f))
            )
        }
    }
}

/**
 * NVIDIA GeForce Experience Freestyle (Alt+F3) UI Replica.
 * Shared identically between the in-app Filters tab and the Floating Bubble Filter Panel.
 */
@Composable
fun GeForceFilterPanelContent(
    filterState: GameFilterState,
    onSelectSlot: (Int) -> Unit,
    onUpdateStyle: (GameFilterStyle) -> Unit,
    onResetSlot: () -> Unit,
    compactMode: Boolean = false,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeSlot = filterState.activeSlot
    val editingSlot = if (activeSlot in 1..3) activeSlot else 1
    val currentStyle = filterState.styleForSlot(editingSlot)
    var brightnessExpanded by remember { mutableStateOf(true) }
    var colorExpanded by remember { mutableStateOf(true) }

    val panelBg = Color(0xF00E1218)
    val cardBg = Color(0xFF151B24)
    val borderColor = Color(0xFF2B3748)

    Surface(
        color = panelBg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(1.2.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compactMode) 12.dp else 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // GeForce Experience Header Bar
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
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (filterState.isEnabled) GeForceLime else Color(0xFF55657A))
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "GAME FILTER",
                                color = Color(0xFFF3F7FC),
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (compactMode) 14.sp else 16.sp,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                color = Color(0xFF1F2B12),
                                shape = RoundedCornerShape(5.dp),
                                modifier = Modifier.border(
                                    1.dp,
                                    GeForceLime.copy(alpha = 0.55f),
                                    RoundedCornerShape(5.dp)
                                )
                            ) {
                                Text(
                                    text = "FREESTYLE",
                                    color = GeForceLime,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (filterState.isEnabled) {
                                "Style $activeSlot • ${currentStyle.name}"
                            } else {
                                "Filter Off — tap Style 1–3 to activate"
                            },
                            color = Color(0xFF91A3BA),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onResetSlot,
                        enabled = filterState.isEnabled,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("filter_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Active Style",
                            tint = if (filterState.isEnabled) Color(0xFFD5E1F0) else Color(0xFF4D5C70)
                        )
                    }
                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("filter_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Filter Panel",
                                tint = Color(0xFFD5E1F0)
                            )
                        }
                    }
                }
            }

            // Style Segmented Selector: Off | 1 | 2 | 3
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0A0D12))
                    .border(1.dp, Color(0xFF232D3B), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0 to "Off", 1 to "1", 2 to "2", 3 to "3").forEach { (slot, label) ->
                    val selected = activeSlot == slot
                    val slotName = if (slot == 0) null else filterState.styleForSlot(slot).name
                    Box(
                        modifier = Modifier
                            .weight(if (slot == 0) 0.9f else 1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                when {
                                    selected && slot == 0 -> Color(0xFF2A3444)
                                    selected -> GeForceLime
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { onSelectSlot(slot) }
                            .testTag("filter_slot_$slot")
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                color = when {
                                    selected && slot == 0 -> Color.White
                                    selected -> Color(0xFF091200)
                                    else -> Color(0xFFA2B3C9)
                                },
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (slotName != null && !compactMode) {
                                Text(
                                    text = slotName,
                                    color = if (selected) Color(0xFF132600) else Color(0xFF6B7B91),
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Section 1: Brightness / Contrast (Brightness, Contrast, Shadows)
            FilterAccordionCard(
                title = "Brightness / Contrast",
                icon = Icons.Default.Brightness6,
                accentColor = GeForceLime,
                expanded = brightnessExpanded,
                onToggleExpand = { brightnessExpanded = !brightnessExpanded },
                cardBg = cardBg,
                borderColor = borderColor
            ) {
                FilterValueSlider(
                    label = "Brightness",
                    value = currentStyle.brightness,
                    valueRange = -0.50f..0.50f,
                    accentColor = GeForceLime,
                    onValueChange = { onUpdateStyle(currentStyle.copy(brightness = it)) },
                    testTag = "filter_slider_brightness"
                )
                FilterValueSlider(
                    label = "Contrast",
                    value = currentStyle.contrast,
                    valueRange = -0.50f..0.50f,
                    accentColor = GeForceLime,
                    onValueChange = { onUpdateStyle(currentStyle.copy(contrast = it)) },
                    testTag = "filter_slider_contrast"
                )
                FilterValueSlider(
                    label = "Shadows",
                    value = currentStyle.shadows,
                    valueRange = -0.50f..1.00f,
                    accentColor = GeForceLime,
                    onValueChange = { onUpdateStyle(currentStyle.copy(shadows = it)) },
                    testTag = "filter_slider_shadows"
                )
            }

            // Section 2: Color (Color/Vibrance, Temperature, Tint Color, Tint Intensity)
            FilterAccordionCard(
                title = "Color",
                icon = Icons.Default.Palette,
                accentColor = TacticalCyan,
                expanded = colorExpanded,
                onToggleExpand = { colorExpanded = !colorExpanded },
                cardBg = cardBg,
                borderColor = borderColor
            ) {
                FilterValueSlider(
                    label = "Color / Vibrance",
                    value = currentStyle.colorVibrance,
                    valueRange = -0.50f..1.00f,
                    accentColor = TacticalCyan,
                    onValueChange = { onUpdateStyle(currentStyle.copy(colorVibrance = it)) },
                    testTag = "filter_slider_vibrance"
                )
                FilterValueSlider(
                    label = "Temperature",
                    value = currentStyle.temperature,
                    valueRange = -0.50f..0.50f,
                    accentColor = Color(0xFFFFB300),
                    customValueText = formatTemperatureLabel(currentStyle.temperature),
                    onValueChange = { onUpdateStyle(currentStyle.copy(temperature = it)) },
                    testTag = "filter_slider_temperature"
                )

                // Tint Color Swatches
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tint Color",
                            color = Color(0xFFDCE6F2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currentStyle.tintPreset.displayName,
                            color = if (currentStyle.tintPreset == FilterTintPreset.NEUTRAL) {
                                Color(0xFF7D8FA6)
                            } else {
                                Color(currentStyle.tintPreset.argbHex)
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterTintPreset.entries.forEach { preset ->
                            val selected = currentStyle.tintPreset == preset
                            val swatchColor = if (preset == FilterTintPreset.NEUTRAL) {
                                Color(0xFF4C596B)
                            } else {
                                Color(preset.argbHex)
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val nextIntensity = if (
                                            preset != FilterTintPreset.NEUTRAL &&
                                            currentStyle.tintIntensity < 0.05f
                                        ) {
                                            0.20f
                                        } else if (preset == FilterTintPreset.NEUTRAL) {
                                            0f
                                        } else {
                                            currentStyle.tintIntensity
                                        }
                                        onUpdateStyle(
                                            currentStyle.copy(
                                                tintPreset = preset,
                                                tintIntensity = nextIntensity
                                            )
                                        )
                                    }
                                    .testTag("filter_tint_${preset.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) Color.White else Color(0xFF334154),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = preset.displayName,
                                            tint = Color(0xFF0A1018),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                FilterValueSlider(
                    label = "Tint Intensity",
                    value = currentStyle.tintIntensity,
                    valueRange = 0f..0.60f,
                    accentColor = if (currentStyle.tintPreset == FilterTintPreset.NEUTRAL) {
                        TacticalCyan
                    } else {
                        Color(currentStyle.tintPreset.argbHex)
                    },
                    onValueChange = { nextIntensity ->
                        val nextPreset = if (
                            nextIntensity > 0.01f &&
                            currentStyle.tintPreset == FilterTintPreset.NEUTRAL
                        ) {
                            FilterTintPreset.WARM_AMBER
                        } else {
                            currentStyle.tintPreset
                        }
                        onUpdateStyle(
                            currentStyle.copy(
                                tintPreset = nextPreset,
                                tintIntensity = nextIntensity
                            )
                        )
                    },
                    testTag = "filter_slider_tint_intensity"
                )
            }
        }
    }
}

@Composable
private fun FilterAccordionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    cardBg: Color,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    color = Color(0xFFF0F4FA),
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = Color(0xFF8FA1B8)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun FilterValueSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    testTag: String,
    customValueText: String? = null
) {
    val pct = (value * 100f).roundToInt()
    val readout = customValueText ?: when {
        pct > 0 -> "+$pct%"
        else -> "$pct%"
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
            Surface(
                color = Color(0xFF0A0E14),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.border(1.dp, Color(0xFF283446), RoundedCornerShape(6.dp))
            ) {
                Text(
                    text = readout,
                    color = if (pct == 0) Color(0xFF8C9EB5) else accentColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF283344)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

private fun formatTemperatureLabel(temp: Float): String {
    val pct = (temp * 100f).roundToInt()
    return when {
        pct > 2 -> "+$pct% Warm"
        pct < -2 -> "$pct% Cool"
        else -> "0% Neutral"
    }
}

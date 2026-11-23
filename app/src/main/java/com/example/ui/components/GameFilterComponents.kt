package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Vignette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlackWhiteTone
import com.example.model.ColorblindMode
import com.example.model.FilterTintPreset
import com.example.model.FreestyleCategory
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import com.example.model.MoodFilterPreset
import com.example.model.SpecialFxMode
import com.example.model.StickerPreset
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import kotlin.math.roundToInt

/**
 * Full NVIDIA GeForce Experience Freestyle (Alt+F3) UI Replica.
 * Includes all 18 requested pixel-shader filter modules grouped into Essential, Utility, and
 * Artistic categories, Live GPU Shader activation toggle, and zero-stutter SmoothFilterSliders.
 *
 * Shared identically between the in-app Visuals screen and the Floating Bubble Left Panel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeForceFilterPanelContent(
    filterState: GameFilterState,
    onSelectSlot: (Int) -> Unit,
    onUpdateStyle: (GameFilterStyle) -> Unit,
    onResetSlot: () -> Unit,
    onPreviewStyle: ((GameFilterStyle) -> Unit)? = null,
    isGpuShaderActive: Boolean = false,
    onToggleGpuShader: (() -> Unit)? = null,
    onUploadStickerClick: (() -> Unit)? = null,
    compactMode: Boolean = false,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeSlot = filterState.activeSlot
    val editingSlot = if (activeSlot in 1..3) activeSlot else 1
    val currentStyle = filterState.styleForSlot(editingSlot)

    // Fast in-memory preview helper (zero DataStore I/O during drag!) + clean commit on release
    fun preview(next: GameFilterStyle) {
        if (onPreviewStyle != null) onPreviewStyle(next) else onUpdateStyle(next)
    }

    fun commit(next: GameFilterStyle) {
        onPreviewStyle?.invoke(next)
        onUpdateStyle(next)
    }

    var selectedCategory by remember { mutableStateOf(FreestyleCategory.ESSENTIAL) }
    var expandedKey by remember { mutableStateOf<String?>("brightness_contrast") }

    val panelBg = Color(0xF20D1117)
    val cardBg = Color(0xFF141B24)
    val borderColor = Color(0xFF283446)

    val hueSpectrumBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFF0000),
                Color(0xFFFFFF00),
                Color(0xFF00FF00),
                Color(0xFF00FFFF),
                Color(0xFF0000FF),
                Color(0xFFFF00FF),
                Color(0xFFFF0000)
            )
        )
    }

    Surface(
        color = panelBg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(1.2.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compactMode) 11.dp else 15.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // GeForce Freestyle Header
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
                            .height(26.dp)
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
                                    text = "GPU SHADER",
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
                                "Style $activeSlot • ${currentStyle.name} (${currentStyle.activeModuleCount()} active)"
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

            // Live GPU Per-Pixel Shader Engine Toggle Card
            if (onToggleGpuShader != null) {
                Surface(
                    color = if (isGpuShaderActive) Color(0xFF162614) else Color(0xFF111822),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isGpuShaderActive) GeForceLime.copy(alpha = 0.7f) else Color(0xFF2A394D),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = if (isGpuShaderActive) GeForceLime else TacticalCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGpuShaderActive) "Live GPU Shader Engine: ACTIVE" else "Live GPU Per-Pixel Shader",
                                color = if (isGpuShaderActive) GeForceLime else Color(0xFFECF3FC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (isGpuShaderActive) {
                                    "Real OpenGL Sharpen, HDR, Shadows, DoF & FX active on live game stream."
                                } else {
                                    "Pick 'A single app' (e.g., GeForce NOW) for true 60fps pixel shaders."
                                },
                                color = Color(0xFF8FA3BA),
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                        Button(
                            onClick = onToggleGpuShader,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isGpuShaderActive) MacroCoral else GeForceLime,
                                contentColor = if (isGpuShaderActive) Color.White else Color(0xFF0A1400)
                            ),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("filter_gpu_engine_toggle")
                        ) {
                            Text(
                                text = if (isGpuShaderActive) "Stop" else "Enable",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Style Slot Selector: Off | 1 | 2 | 3
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF090C11))
                    .border(1.dp, Color(0xFF232D3B), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0 to "Off", 1 to "1", 2 to "2", 3 to "3").forEach { (slot, label) ->
                    val selected = activeSlot == slot
                    val slotName = if (slot == 0) null else filterState.styleForSlot(slot).name
                    Box(
                        modifier = Modifier
                            .weight(if (slot == 0) 0.85f else 1f)
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

            // Category Switcher: Essential | Utility | Artistic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0A0E15))
                    .border(1.dp, Color(0xFF232E3F), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FreestyleCategory.entries.forEach { cat ->
                    val selected = selectedCategory == cat
                    val activeInCat = countActiveInCategory(currentStyle, cat)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (selected) Color(0xFF1E2B3A) else Color.Transparent)
                            .border(
                                1.dp,
                                if (selected) TacticalCyan.copy(alpha = 0.65f) else Color.Transparent,
                                RoundedCornerShape(7.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .testTag("filter_category_${cat.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = cat.displayName,
                                color = if (selected) Color.White else Color(0xFF8FA2B8),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            if (activeInCat > 0) {
                                Surface(
                                    color = GeForceLime.copy(alpha = 0.22f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$activeInCat",
                                        color = GeForceLime,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Content
            when (selectedCategory) {
                FreestyleCategory.ESSENTIAL -> {
                    // 1. Sharpen
                    FilterModuleCard(
                        title = "Sharpen",
                        subtitle = "Edge crispness & grain guard",
                        icon = Icons.Default.Details,
                        accentColor = GeForceLime,
                        enabled = currentStyle.sharpenEnabled,
                        expanded = expandedKey == "sharpen",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(sharpenEnabled = on))
                            if (on) expandedKey = "sharpen"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "sharpen") null else "sharpen" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_sharpen"
                    ) {
                        SmoothFilterSlider(
                            label = "Sharpen",
                            value = currentStyle.sharpenAmount,
                            valueRange = 0f..1f,
                            accentColor = GeForceLime,
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(sharpenEnabled = true, sharpenAmount = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(sharpenEnabled = true, sharpenAmount = it)) },
                            testTag = "filter_slider_sharpen"
                        )
                        SmoothFilterSlider(
                            label = "Ignore Film Grain",
                            value = currentStyle.sharpenIgnoreFilmGrain,
                            valueRange = 0f..1f,
                            accentColor = TacticalCyan,
                            defaultValue = 0.20f,
                            onValueChange = { preview(currentStyle.copy(sharpenEnabled = true, sharpenIgnoreFilmGrain = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(sharpenEnabled = true, sharpenIgnoreFilmGrain = it)) },
                            testTag = "filter_slider_ignore_grain"
                        )
                    }

                    // 2. Sharpen+
                    FilterModuleCard(
                        title = "Sharpen+",
                        subtitle = "Texture-aware without haloing UI text",
                        icon = Icons.Default.AutoFixHigh,
                        accentColor = GeForceLime,
                        enabled = currentStyle.sharpenPlusEnabled,
                        expanded = expandedKey == "sharpen_plus",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(sharpenPlusEnabled = on))
                            if (on) expandedKey = "sharpen_plus"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "sharpen_plus") null else "sharpen_plus" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_sharpen_plus"
                    ) {
                        SmoothFilterSlider(
                            label = "Intensity",
                            value = currentStyle.sharpenPlusIntensity,
                            valueRange = 0f..1f,
                            accentColor = GeForceLime,
                            defaultValue = 0.45f,
                            onValueChange = { preview(currentStyle.copy(sharpenPlusEnabled = true, sharpenPlusIntensity = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(sharpenPlusEnabled = true, sharpenPlusIntensity = it)) },
                            testTag = "filter_slider_sharpen_plus_int"
                        )
                        SmoothFilterSlider(
                            label = "Texture Details",
                            value = currentStyle.sharpenPlusTextureDetails,
                            valueRange = -1f..1f,
                            accentColor = TacticalCyan,
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(sharpenPlusEnabled = true, sharpenPlusTextureDetails = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(sharpenPlusEnabled = true, sharpenPlusTextureDetails = it)) },
                            testTag = "filter_slider_sharpen_plus_tex"
                        )
                    }

                    // 3. Brightness / Contrast
                    FilterModuleCard(
                        title = "Brightness / Contrast",
                        subtitle = "Exposure, Contrast, Highlights, Shadows & Gamma",
                        icon = Icons.Default.Brightness6,
                        accentColor = GeForceLime,
                        enabled = currentStyle.brightnessContrastEnabled,
                        expanded = expandedKey == "brightness_contrast",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(brightnessContrastEnabled = on))
                            if (on) expandedKey = "brightness_contrast"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "brightness_contrast") null else "brightness_contrast" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_bc"
                    ) {
                        SmoothFilterSlider(
                            label = "Exposure",
                            value = currentStyle.effectiveExposure,
                            valueRange = -1f..1f,
                            accentColor = GeForceLime,
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(brightnessContrastEnabled = true, exposure = it, brightness = 0f)) },
                            onValueChangeFinished = { commit(currentStyle.copy(brightnessContrastEnabled = true, exposure = it, brightness = 0f)) },
                            testTag = "filter_slider_exposure"
                        )
                        SmoothFilterSlider(
                            label = "Contrast",
                            value = currentStyle.contrast,
                            valueRange = -1f..1f,
                            accentColor = GeForceLime,
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(brightnessContrastEnabled = true, contrast = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(brightnessContrastEnabled = true, contrast = it)) },
                            testTag = "filter_slider_contrast"
                        )
                        SmoothFilterSlider(
                            label = "Highlights",
                            value = currentStyle.highlights,
                            valueRange = -1f..1f,
                            accentColor = Color(0xFFFFE082),
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(brightnessContrastEnabled = true, highlights = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(brightnessContrastEnabled = true, highlights = it)) },
                            testTag = "filter_slider_highlights"
                        )
                        SmoothFilterSlider(
                            label = "Shadows",
                            value = currentStyle.shadows,
                            valueRange = -1f..1f,
                            accentColor = TacticalCyan,
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(brightnessContrastEnabled = true, shadows = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(brightnessContrastEnabled = true, shadows = it)) },
                            testTag = "filter_slider_shadows"
                        )
                        SmoothFilterSlider(
                            label = "Gamma",
                            value = currentStyle.gamma,
                            valueRange = -1f..1f,
                            accentColor = GeForceLime,
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(brightnessContrastEnabled = true, gamma = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(brightnessContrastEnabled = true, gamma = it)) },
                            testTag = "filter_slider_gamma"
                        )
                    }

                    // 4. Details
                    FilterModuleCard(
                        title = "Details",
                        subtitle = "Sharpen, Clarity, HDR Toning & Bloom",
                        icon = Icons.Default.Flare,
                        accentColor = TacticalCyan,
                        enabled = currentStyle.detailsEnabled,
                        expanded = expandedKey == "details",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(detailsEnabled = on))
                            if (on) expandedKey = "details"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "details") null else "details" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_details"
                    ) {
                        SmoothFilterSlider(
                            label = "Sharpen",
                            value = currentStyle.detailsSharpen,
                            valueRange = 0f..1f,
                            accentColor = TacticalCyan,
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(detailsEnabled = true, detailsSharpen = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(detailsEnabled = true, detailsSharpen = it)) },
                            testTag = "filter_slider_details_sharpen"
                        )
                        SmoothFilterSlider(
                            label = "Clarity",
                            value = currentStyle.detailsClarity,
                            valueRange = -1f..1f,
                            accentColor = GeForceLime,
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(detailsEnabled = true, detailsClarity = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(detailsEnabled = true, detailsClarity = it)) },
                            testTag = "filter_slider_details_clarity"
                        )
                        SmoothFilterSlider(
                            label = "HDR Toning",
                            value = currentStyle.detailsHdrToning,
                            valueRange = -1f..1f,
                            accentColor = TurboAmber,
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(detailsEnabled = true, detailsHdrToning = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(detailsEnabled = true, detailsHdrToning = it)) },
                            testTag = "filter_slider_details_hdr"
                        )
                        SmoothFilterSlider(
                            label = "Bloom",
                            value = currentStyle.detailsBloom,
                            valueRange = 0f..1f,
                            accentColor = Color(0xFFFFD54F),
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(detailsEnabled = true, detailsBloom = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(detailsEnabled = true, detailsBloom = it)) },
                            testTag = "filter_slider_details_bloom"
                        )
                    }

                    // 5. Color
                    FilterModuleCard(
                        title = "Color",
                        subtitle = "Tint Hue, Tint Intensity, Temperature & Vibrance",
                        icon = Icons.Default.Palette,
                        accentColor = TacticalCyan,
                        enabled = currentStyle.colorEnabled,
                        expanded = expandedKey == "color",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(colorEnabled = on))
                            if (on) expandedKey = "color"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "color") null else "color" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_color"
                    ) {
                        val (tr, tg, tb) = currentStyle.resolvedTintRgb()
                        val tintChipColor = Color(tr, tg, tb, 1f)

                        SmoothFilterSlider(
                            label = "Tint Color",
                            value = currentStyle.tintHue,
                            valueRange = 0f..360f,
                            accentColor = tintChipColor,
                            defaultValue = 36f,
                            centerOrigin = false,
                            trackBrush = hueSpectrumBrush,
                            valueFormatter = { "${it.roundToInt()}°" },
                            onValueChange = { hue ->
                                val nextInt = if (currentStyle.tintIntensity < 0.02f) 0.20f else currentStyle.tintIntensity
                                preview(
                                    currentStyle.copy(
                                        colorEnabled = true,
                                        tintPreset = FilterTintPreset.NEUTRAL,
                                        tintHue = hue,
                                        tintIntensity = nextInt
                                    )
                                )
                            },
                            onValueChangeFinished = { hue ->
                                val nextInt = if (currentStyle.tintIntensity < 0.02f) 0.20f else currentStyle.tintIntensity
                                commit(
                                    currentStyle.copy(
                                        colorEnabled = true,
                                        tintPreset = FilterTintPreset.NEUTRAL,
                                        tintHue = hue,
                                        tintIntensity = nextInt
                                    )
                                )
                            },
                            testTag = "filter_slider_tint_hue"
                        )

                        // Quick Tint Swatches
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterTintPreset.entries.forEach { preset ->
                                val selected = currentStyle.tintPreset == preset && (preset == FilterTintPreset.NEUTRAL || currentStyle.tintIntensity > 0.01f)
                                val swatchColor = if (preset == FilterTintPreset.NEUTRAL) Color(0xFF4C596B) else Color(preset.argbHex)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val nextIntensity = if (preset != FilterTintPreset.NEUTRAL && currentStyle.tintIntensity < 0.05f) {
                                                0.22f
                                            } else if (preset == FilterTintPreset.NEUTRAL) {
                                                0f
                                            } else {
                                                currentStyle.tintIntensity
                                            }
                                            commit(
                                                currentStyle.copy(
                                                    colorEnabled = true,
                                                    tintPreset = preset,
                                                    tintHue = preset.hueDegrees,
                                                    tintIntensity = nextIntensity
                                                )
                                            )
                                        }
                                        .testTag("filter_tint_${preset.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(
                                                width = if (selected) 2.2.dp else 1.dp,
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
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SmoothFilterSlider(
                            label = "Tint Intensity",
                            value = currentStyle.tintIntensity,
                            valueRange = 0f..1f,
                            accentColor = tintChipColor,
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(colorEnabled = true, tintIntensity = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(colorEnabled = true, tintIntensity = it)) },
                            testTag = "filter_slider_tint_intensity"
                        )
                        SmoothFilterSlider(
                            label = "Temperature",
                            value = currentStyle.temperature,
                            valueRange = -1f..1f,
                            accentColor = Color(0xFFFFB300),
                            defaultValue = 0f,
                            valueFormatter = { formatTemperatureLabel(it) },
                            onValueChange = { preview(currentStyle.copy(colorEnabled = true, temperature = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(colorEnabled = true, temperature = it)) },
                            testTag = "filter_slider_temperature"
                        )
                        SmoothFilterSlider(
                            label = "Vibrance",
                            value = currentStyle.colorVibrance,
                            valueRange = -1f..1f,
                            accentColor = TacticalCyan,
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(colorEnabled = true, colorVibrance = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(colorEnabled = true, colorVibrance = it)) },
                            testTag = "filter_slider_vibrance"
                        )
                    }
                }

                FreestyleCategory.ACCESSIBILITY -> {
                    // 6. Colorblind
                    FilterModuleCard(
                        title = "Colorblind",
                        subtitle = "Protanopia, Deuteranopia & Tritanopia LMS shift",
                        icon = Icons.Default.Visibility,
                        accentColor = TacticalCyan,
                        enabled = currentStyle.colorblindEnabled,
                        expanded = expandedKey == "colorblind" || currentStyle.colorblindEnabled,
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(colorblindEnabled = on))
                            if (on) expandedKey = "colorblind"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "colorblind") null else "colorblind" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_colorblind"
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ColorblindMode.entries.forEach { mode ->
                                ModeOptionPill(
                                    label = mode.shortCode,
                                    selected = currentStyle.colorblindMode == mode,
                                    accentColor = TacticalCyan,
                                    onClick = { commit(currentStyle.copy(colorblindEnabled = true, colorblindMode = mode)) },
                                    testTag = "filter_cb_${mode.name.lowercase()}"
                                )
                            }
                        }
                        SmoothFilterSlider(
                            label = "Intensity",
                            value = currentStyle.colorblindIntensity,
                            valueRange = 0f..1f,
                            accentColor = TacticalCyan,
                            defaultValue = 0.65f,
                            onValueChange = { preview(currentStyle.copy(colorblindEnabled = true, colorblindIntensity = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(colorblindEnabled = true, colorblindIntensity = it)) },
                            testTag = "filter_slider_colorblind_int"
                        )
                    }

                    // 7. Night Mode
                    FilterModuleCard(
                        title = "Night Mode",
                        subtitle = "Reduces blue light for late-night comfort",
                        icon = Icons.Default.NightsStay,
                        accentColor = TurboAmber,
                        enabled = currentStyle.nightModeEnabled,
                        expanded = expandedKey == "night_mode" || currentStyle.nightModeEnabled,
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(nightModeEnabled = on))
                            if (on) expandedKey = "night_mode"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "night_mode") null else "night_mode" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_night_mode"
                    ) {
                        SmoothFilterSlider(
                            label = "Intensity",
                            value = currentStyle.nightModeIntensity,
                            valueRange = 0f..1f,
                            accentColor = TurboAmber,
                            defaultValue = 0.45f,
                            onValueChange = { preview(currentStyle.copy(nightModeEnabled = true, nightModeIntensity = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(nightModeEnabled = true, nightModeIntensity = it)) },
                            testTag = "filter_slider_night_mode"
                        )
                    }

                    // 8. Splitscreen
                    FilterModuleCard(
                        title = "Splitscreen",
                        subtitle = "Compare unfiltered Original (left) vs Filtered (right)",
                        icon = Icons.Default.Compare,
                        accentColor = GeForceLime,
                        enabled = currentStyle.splitscreenEnabled,
                        expanded = expandedKey == "splitscreen" || currentStyle.splitscreenEnabled,
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(splitscreenEnabled = on))
                            if (on) expandedKey = "splitscreen"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "splitscreen") null else "splitscreen" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_splitscreen"
                    ) {
                        SmoothFilterSlider(
                            label = "Divider Position",
                            value = currentStyle.splitscreenPosition,
                            valueRange = 0f..1f,
                            accentColor = GeForceLime,
                            defaultValue = 0.50f,
                            onValueChange = { preview(currentStyle.copy(splitscreenEnabled = true, splitscreenPosition = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(splitscreenEnabled = true, splitscreenPosition = it)) },
                            testTag = "filter_slider_splitscreen"
                        )
                    }
                }

                FreestyleCategory.ARTISTIC -> {
                    // 9. Black & White
                    FilterModuleCard(
                        title = "Black & White",
                        subtitle = "Pure Mono, Warm, Cool & Noir grayscale",
                        icon = Icons.Default.FilterBAndW,
                        accentColor = Color(0xFFE2EAF5),
                        enabled = currentStyle.blackWhiteEnabled,
                        expanded = expandedKey == "bw",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(blackWhiteEnabled = on))
                            if (on) expandedKey = "bw"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "bw") null else "bw" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_bw"
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BlackWhiteTone.entries.forEach { tone ->
                                ModeOptionPill(
                                    label = tone.displayName,
                                    selected = currentStyle.blackWhiteTone == tone,
                                    accentColor = Color(0xFFE2EAF5),
                                    onClick = { commit(currentStyle.copy(blackWhiteEnabled = true, blackWhiteTone = tone)) },
                                    testTag = "filter_bw_${tone.name.lowercase()}"
                                )
                            }
                        }
                        SmoothFilterSlider(
                            label = "Intensity",
                            value = currentStyle.blackWhiteIntensity,
                            valueRange = 0f..1f,
                            accentColor = Color(0xFFE2EAF5),
                            defaultValue = 0.85f,
                            onValueChange = { preview(currentStyle.copy(blackWhiteEnabled = true, blackWhiteIntensity = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(blackWhiteEnabled = true, blackWhiteIntensity = it)) },
                            testTag = "filter_slider_bw_int"
                        )
                    }

                    // 10. Sepia
                    FilterModuleCard(
                        title = "Sepia",
                        subtitle = "Warm vintage film tone",
                        icon = Icons.Default.FilterVintage,
                        accentColor = Color(0xFFD99B52),
                        enabled = currentStyle.sepiaEnabled,
                        expanded = expandedKey == "sepia",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(sepiaEnabled = on))
                            if (on) expandedKey = "sepia"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "sepia") null else "sepia" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_sepia"
                    ) {
                        SmoothFilterSlider(
                            label = "Intensity",
                            value = currentStyle.sepiaIntensity,
                            valueRange = 0f..1f,
                            accentColor = Color(0xFFD99B52),
                            defaultValue = 0.65f,
                            onValueChange = { preview(currentStyle.copy(sepiaEnabled = true, sepiaIntensity = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(sepiaEnabled = true, sepiaIntensity = it)) },
                            testTag = "filter_slider_sepia_int"
                        )
                    }

                    // 11. Vignette
                    FilterModuleCard(
                        title = "Vignette",
                        subtitle = "Darken or brighten screen corners",
                        icon = Icons.Default.Vignette,
                        accentColor = TacticalCyan,
                        enabled = currentStyle.vignetteEnabled,
                        expanded = expandedKey == "vignette",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(vignetteEnabled = on))
                            if (on) expandedKey = "vignette"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "vignette") null else "vignette" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_vignette"
                    ) {
                        SmoothFilterSlider(
                            label = "Intensity",
                            value = currentStyle.vignetteIntensity,
                            valueRange = -1f..1f,
                            accentColor = TacticalCyan,
                            defaultValue = 0.45f,
                            onValueChange = { preview(currentStyle.copy(vignetteEnabled = true, vignetteIntensity = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(vignetteEnabled = true, vignetteIntensity = it)) },
                            testTag = "filter_slider_vignette"
                        )
                    }

                    // 12. Depth of Field / Tilt-Shift
                    FilterModuleCard(
                        title = "Depth of Field / Tilt-Shift",
                        subtitle = "Cinematic lens bokeh & miniature look",
                        icon = Icons.Default.BlurOn,
                        accentColor = Color(0xFF90CAF9),
                        enabled = currentStyle.dofEnabled,
                        expanded = expandedKey == "dof",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(dofEnabled = on))
                            if (on) expandedKey = "dof"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "dof") null else "dof" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_dof"
                    ) {
                        SmoothFilterSlider(
                            label = "Blur Curve",
                            value = currentStyle.dofBlurCurve,
                            valueRange = 0.10f..1f,
                            accentColor = Color(0xFF90CAF9),
                            defaultValue = 0.45f,
                            onValueChange = { preview(currentStyle.copy(dofEnabled = true, dofBlurCurve = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(dofEnabled = true, dofBlurCurve = it)) },
                            testTag = "filter_slider_dof_curve"
                        )
                        SmoothFilterSlider(
                            label = "Focus Depth",
                            value = currentStyle.dofFocusDepth,
                            valueRange = 0f..1f,
                            accentColor = GeForceLime,
                            defaultValue = 0.50f,
                            onValueChange = { preview(currentStyle.copy(dofEnabled = true, dofFocusDepth = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(dofEnabled = true, dofFocusDepth = it)) },
                            testTag = "filter_slider_dof_focus"
                        )
                        SmoothFilterSlider(
                            label = "Near Blur",
                            value = currentStyle.dofNearBlur,
                            valueRange = 0f..1f,
                            accentColor = TacticalCyan,
                            defaultValue = 0.45f,
                            onValueChange = { preview(currentStyle.copy(dofEnabled = true, dofNearBlur = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(dofEnabled = true, dofNearBlur = it)) },
                            testTag = "filter_slider_dof_near"
                        )
                        SmoothFilterSlider(
                            label = "Far Blur",
                            value = currentStyle.dofFarBlur,
                            valueRange = 0f..1f,
                            accentColor = TacticalCyan,
                            defaultValue = 0.55f,
                            onValueChange = { preview(currentStyle.copy(dofEnabled = true, dofFarBlur = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(dofEnabled = true, dofFarBlur = it)) },
                            testTag = "filter_slider_dof_far"
                        )
                    }

                    // 13. Old Film
                    FilterModuleCard(
                        title = "Old Film",
                        subtitle = "Vintage grain, flicker, scratches & dirt",
                        icon = Icons.Default.MovieFilter,
                        accentColor = Color(0xFFE0A96D),
                        enabled = currentStyle.oldFilmEnabled,
                        expanded = expandedKey == "old_film",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(oldFilmEnabled = on))
                            if (on) expandedKey = "old_film"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "old_film") null else "old_film" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_old_film"
                    ) {
                        SmoothFilterSlider(
                            label = "Gamma",
                            value = currentStyle.oldFilmGamma,
                            valueRange = -1f..1f,
                            accentColor = Color(0xFFE0A96D),
                            defaultValue = 0.10f,
                            onValueChange = { preview(currentStyle.copy(oldFilmEnabled = true, oldFilmGamma = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(oldFilmEnabled = true, oldFilmGamma = it)) },
                            testTag = "filter_slider_oldfilm_gamma"
                        )
                        SmoothFilterSlider(
                            label = "Exposure",
                            value = currentStyle.oldFilmExposure,
                            valueRange = -1f..1f,
                            accentColor = Color(0xFFE0A96D),
                            defaultValue = -0.05f,
                            onValueChange = { preview(currentStyle.copy(oldFilmEnabled = true, oldFilmExposure = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(oldFilmEnabled = true, oldFilmExposure = it)) },
                            testTag = "filter_slider_oldfilm_exposure"
                        )
                        SmoothFilterSlider(
                            label = "Contrast",
                            value = currentStyle.oldFilmContrast,
                            valueRange = -1f..1f,
                            accentColor = Color(0xFFE0A96D),
                            defaultValue = 0.18f,
                            onValueChange = { preview(currentStyle.copy(oldFilmEnabled = true, oldFilmContrast = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(oldFilmEnabled = true, oldFilmContrast = it)) },
                            testTag = "filter_slider_oldfilm_contrast"
                        )
                        SmoothFilterSlider(
                            label = "Vignette Intensity",
                            value = currentStyle.oldFilmVignette,
                            valueRange = 0f..1f,
                            accentColor = Color(0xFFE0A96D),
                            defaultValue = 0.45f,
                            onValueChange = { preview(currentStyle.copy(oldFilmEnabled = true, oldFilmVignette = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(oldFilmEnabled = true, oldFilmVignette = it)) },
                            testTag = "filter_slider_oldfilm_vignette"
                        )
                        SmoothFilterSlider(
                            label = "Filter Strength",
                            value = currentStyle.oldFilmStrength,
                            valueRange = 0f..1f,
                            accentColor = GeForceLime,
                            defaultValue = 0.70f,
                            onValueChange = { preview(currentStyle.copy(oldFilmEnabled = true, oldFilmStrength = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(oldFilmEnabled = true, oldFilmStrength = it)) },
                            testTag = "filter_slider_oldfilm_strength"
                        )
                        SmoothFilterSlider(
                            label = "Film Dirt Strength",
                            value = currentStyle.oldFilmDirtStrength,
                            valueRange = 0f..1f,
                            accentColor = TurboAmber,
                            defaultValue = 0.45f,
                            onValueChange = { preview(currentStyle.copy(oldFilmEnabled = true, oldFilmDirtStrength = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(oldFilmEnabled = true, oldFilmDirtStrength = it)) },
                            testTag = "filter_slider_oldfilm_dirt"
                        )
                    }

                    // 14. Mood LUTs
                    FilterModuleCard(
                        title = "Mood",
                        subtitle = "Cinematic color-grading LUT presets",
                        icon = Icons.Default.Contrast,
                        accentColor = Color(0xFFB388FF),
                        enabled = currentStyle.moodEnabled,
                        expanded = expandedKey == "mood",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(moodEnabled = on))
                            if (on) expandedKey = "mood"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "mood") null else "mood" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_mood"
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MoodFilterPreset.entries.forEach { preset ->
                                ModeOptionPill(
                                    label = preset.displayName,
                                    selected = currentStyle.moodPreset == preset,
                                    accentColor = Color(0xFFB388FF),
                                    onClick = { commit(currentStyle.copy(moodEnabled = true, moodPreset = preset)) },
                                    testTag = "filter_mood_${preset.name.lowercase()}"
                                )
                            }
                        }
                        SmoothFilterSlider(
                            label = "Intensity",
                            value = currentStyle.moodIntensity,
                            valueRange = 0f..1f,
                            accentColor = Color(0xFFB388FF),
                            defaultValue = 0.60f,
                            onValueChange = { preview(currentStyle.copy(moodEnabled = true, moodIntensity = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(moodEnabled = true, moodIntensity = it)) },
                            testTag = "filter_slider_mood_int"
                        )
                    }

                    // 15. Special FX
                    FilterModuleCard(
                        title = "Special FX",
                        subtitle = "Retro Pixel, Pencil Sketch & Half-Tone Comic",
                        icon = Icons.Default.AutoAwesome,
                        accentColor = MacroCoral,
                        enabled = currentStyle.specialFxEnabled,
                        expanded = expandedKey == "special_fx",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(specialFxEnabled = on))
                            if (on) expandedKey = "special_fx"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "special_fx") null else "special_fx" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_special_fx"
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SpecialFxMode.entries.forEach { fxMode ->
                                ModeOptionPill(
                                    label = fxMode.displayName,
                                    selected = currentStyle.specialFxMode == fxMode,
                                    accentColor = MacroCoral,
                                    onClick = { commit(currentStyle.copy(specialFxEnabled = true, specialFxMode = fxMode)) },
                                    testTag = "filter_sfx_${fxMode.name.lowercase()}"
                                )
                            }
                        }
                        SmoothFilterSlider(
                            label = "Intensity",
                            value = currentStyle.specialFxIntensity,
                            valueRange = 0f..1f,
                            accentColor = MacroCoral,
                            defaultValue = 0.75f,
                            onValueChange = { preview(currentStyle.copy(specialFxEnabled = true, specialFxIntensity = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(specialFxEnabled = true, specialFxIntensity = it)) },
                            testTag = "filter_slider_sfx_int"
                        )
                    }

                    // 16. Painterly
                    FilterModuleCard(
                        title = "Painterly",
                        subtitle = "Hand-painted & cel-shaded cartoon aesthetic",
                        icon = Icons.Default.Brush,
                        accentColor = Color(0xFFFF80AB),
                        enabled = currentStyle.painterlyEnabled,
                        expanded = expandedKey == "painterly",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(painterlyEnabled = on))
                            if (on) expandedKey = "painterly"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "painterly") null else "painterly" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_painterly"
                    ) {
                        SmoothFilterSlider(
                            label = "Edge Sharpening",
                            value = currentStyle.painterlyEdgeSharpening,
                            valueRange = 0f..1f,
                            accentColor = Color(0xFFFF80AB),
                            defaultValue = 0.45f,
                            onValueChange = { preview(currentStyle.copy(painterlyEnabled = true, painterlyEdgeSharpening = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(painterlyEnabled = true, painterlyEdgeSharpening = it)) },
                            testTag = "filter_slider_painterly_sharp"
                        )
                        SmoothFilterSlider(
                            label = "Filter Radius",
                            value = currentStyle.painterlyFilterRadius,
                            valueRange = 0.10f..1f,
                            accentColor = Color(0xFFFF80AB),
                            defaultValue = 0.45f,
                            onValueChange = { preview(currentStyle.copy(painterlyEnabled = true, painterlyFilterRadius = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(painterlyEnabled = true, painterlyFilterRadius = it)) },
                            testTag = "filter_slider_painterly_radius"
                        )
                        SmoothFilterSlider(
                            label = "Edge Threshold",
                            value = currentStyle.painterlyEdgeThreshold,
                            valueRange = 0.05f..1f,
                            accentColor = TacticalCyan,
                            defaultValue = 0.30f,
                            onValueChange = { preview(currentStyle.copy(painterlyEnabled = true, painterlyEdgeThreshold = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(painterlyEnabled = true, painterlyEdgeThreshold = it)) },
                            testTag = "filter_slider_painterly_thresh"
                        )
                    }

                    // 17. Letterbox
                    FilterModuleCard(
                        title = "Letterbox",
                        subtitle = "Cinematic black bars top/bottom or sides",
                        icon = Icons.Default.Crop169,
                        accentColor = Color(0xFFB0BEC5),
                        enabled = currentStyle.letterboxEnabled,
                        expanded = expandedKey == "letterbox",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(letterboxEnabled = on))
                            if (on) expandedKey = "letterbox"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "letterbox") null else "letterbox" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_letterbox"
                    ) {
                        SmoothFilterSlider(
                            label = "Height (Top / Bottom)",
                            value = currentStyle.letterboxHeight,
                            valueRange = 0f..0.40f,
                            accentColor = Color(0xFFB0BEC5),
                            defaultValue = 0.12f,
                            onValueChange = { preview(currentStyle.copy(letterboxEnabled = true, letterboxHeight = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(letterboxEnabled = true, letterboxHeight = it)) },
                            testTag = "filter_slider_letterbox_h"
                        )
                        SmoothFilterSlider(
                            label = "Width (Left / Right)",
                            value = currentStyle.letterboxWidth,
                            valueRange = 0f..0.40f,
                            accentColor = Color(0xFFB0BEC5),
                            defaultValue = 0f,
                            onValueChange = { preview(currentStyle.copy(letterboxEnabled = true, letterboxWidth = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(letterboxEnabled = true, letterboxWidth = it)) },
                            testTag = "filter_slider_letterbox_w"
                        )
                    }

                    // 18. Stickers
                    FilterModuleCard(
                        title = "Stickers",
                        subtitle = "Static HUD frames, bezels & custom PNG stickers",
                        icon = Icons.Default.FilterFrames,
                        accentColor = GeForceLime,
                        enabled = currentStyle.stickersEnabled,
                        expanded = expandedKey == "stickers",
                        onToggleEnabled = { on ->
                            commit(currentStyle.copy(stickersEnabled = on))
                            if (on) expandedKey = "stickers"
                        },
                        onToggleExpand = { expandedKey = if (expandedKey == "stickers") null else "stickers" },
                        cardBg = cardBg,
                        borderColor = borderColor,
                        switchTag = "filter_toggle_stickers"
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StickerPreset.entries.forEach { preset ->
                                ModeOptionPill(
                                    label = preset.displayName,
                                    selected = currentStyle.stickerPreset == preset,
                                    accentColor = GeForceLime,
                                    onClick = { commit(currentStyle.copy(stickersEnabled = true, stickerPreset = preset)) },
                                    testTag = "filter_sticker_${preset.name.lowercase()}"
                                )
                            }
                        }
                        if (onUploadStickerClick != null) {
                            Button(
                                onClick = onUploadStickerClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1F2B3A),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("filter_upload_sticker_button")
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp), tint = GeForceLime)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (currentStyle.customStickerBase64.isNullOrBlank()) "Upload Custom Sticker Image" else "Replace Custom Sticker Image",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        SmoothFilterSlider(
                            label = "Scale",
                            value = currentStyle.stickerScale,
                            valueRange = 0.30f..1.50f,
                            accentColor = GeForceLime,
                            defaultValue = 1.0f,
                            onValueChange = { preview(currentStyle.copy(stickersEnabled = true, stickerScale = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(stickersEnabled = true, stickerScale = it)) },
                            testTag = "filter_slider_sticker_scale"
                        )
                        SmoothFilterSlider(
                            label = "Opacity",
                            value = currentStyle.stickerOpacity,
                            valueRange = 0.10f..1.0f,
                            accentColor = TacticalCyan,
                            defaultValue = 0.85f,
                            onValueChange = { preview(currentStyle.copy(stickersEnabled = true, stickerOpacity = it)) },
                            onValueChangeFinished = { commit(currentStyle.copy(stickersEnabled = true, stickerOpacity = it)) },
                            testTag = "filter_slider_sticker_opacity"
                        )
                        if (currentStyle.stickerPreset == StickerPreset.CUSTOM_IMAGE) {
                            SmoothFilterSlider(
                                label = "Position X",
                                value = currentStyle.stickerX,
                                valueRange = 0.10f..0.90f,
                                accentColor = TacticalCyan,
                                defaultValue = 0.50f,
                                onValueChange = { preview(currentStyle.copy(stickersEnabled = true, stickerX = it)) },
                                onValueChangeFinished = { commit(currentStyle.copy(stickersEnabled = true, stickerX = it)) },
                                testTag = "filter_slider_sticker_x"
                            )
                            SmoothFilterSlider(
                                label = "Position Y",
                                value = currentStyle.stickerY,
                                valueRange = 0.10f..0.90f,
                                accentColor = TacticalCyan,
                                defaultValue = 0.50f,
                                onValueChange = { preview(currentStyle.copy(stickersEnabled = true, stickerY = it)) },
                                onValueChangeFinished = { commit(currentStyle.copy(stickersEnabled = true, stickerY = it)) },
                                testTag = "filter_slider_sticker_y"
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun countActiveInCategory(style: GameFilterStyle, cat: FreestyleCategory): Int = when (cat) {
    FreestyleCategory.ESSENTIAL -> listOf(
        style.sharpenEnabled,
        style.sharpenPlusEnabled,
        style.brightnessContrastEnabled,
        style.detailsEnabled,
        style.colorEnabled
    ).count { it }
    FreestyleCategory.ACCESSIBILITY -> listOf(
        style.colorblindEnabled,
        style.nightModeEnabled,
        style.splitscreenEnabled
    ).count { it }
    FreestyleCategory.ARTISTIC -> listOf(
        style.blackWhiteEnabled,
        style.sepiaEnabled,
        style.vignetteEnabled,
        style.dofEnabled,
        style.oldFilmEnabled,
        style.moodEnabled,
        style.specialFxEnabled,
        style.painterlyEnabled,
        style.letterboxEnabled,
        style.stickersEnabled
    ).count { it }
}

@Composable
private fun FilterModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    expanded: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleExpand: () -> Unit,
    cardBg: Color,
    borderColor: Color,
    switchTag: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) cardBg else Color(0xFF10151D))
            .border(
                1.dp,
                if (enabled) accentColor.copy(alpha = 0.45f) else borderColor,
                RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) accentColor else Color(0xFF6B7C93),
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Color(0xFFF3F7FC) else Color(0xFFA2B1C6),
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF7B8CA3),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF0A1400),
                    checkedTrackColor = GeForceLime,
                    uncheckedThumbColor = Color(0xFF9FB0C4),
                    uncheckedTrackColor = Color(0xFF232E3D)
                ),
                modifier = Modifier.testTag(switchTag)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = Color(0xFF8FA1B8),
                modifier = Modifier.size(20.dp)
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
private fun ModeOptionPill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (selected) accentColor.copy(alpha = 0.22f) else Color(0xFF0C1017),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .heightIn(min = 38.dp)
            .border(
                1.dp,
                if (selected) accentColor else Color(0xFF2A3648),
                RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else Color(0xFF9EB0C7),
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
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

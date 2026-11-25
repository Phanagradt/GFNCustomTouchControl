package com.example.webplayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ControllerProfile
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import com.example.model.GfnRegionPreset
import com.example.model.LocalNexusStrings
import com.example.model.WebRtcStreamStats
import com.example.model.WebStreamQualityConfig
import com.example.service.PerfStatsSnapshot
import com.example.ui.components.SmoothSlider
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import java.util.Locale

/**
 * Compact live WebRTC + Device Telemetry pill inside the Better-GFN Web Player.
 * Displays real RTCPeerConnection.getStats() metrics (FPS, Ping RTT, Mbps, Packet Loss,
 * resolution/codec) alongside real battery % and temperature.
 */
@Composable
fun WebPlayerStatsPill(
    streamStats: WebRtcStreamStats,
    perfStats: PerfStatsSnapshot,
    region: GfnRegionPreset,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val displayFps = if (streamStats.fps > 0) streamStats.fps else perfStats.fps
    val fpsColor = when {
        displayFps >= 55 -> accentColor
        displayFps >= 30 -> TurboAmber
        displayFps > 0 -> MacroCoral
        else -> Color(0xFFB8C4D4)
    }
    val pingColor = when {
        streamStats.rttMs in 1..45 -> accentColor
        streamStats.rttMs in 46..85 -> TurboAmber
        streamStats.rttMs > 85 -> MacroCoral
        else -> Color(0xFFB8C4D4)
    }
    val lossColor = when {
        streamStats.packetLossPercent >= 2f -> MacroCoral
        streamStats.packetLossPercent >= 0.5f -> TurboAmber
        else -> TacticalCyan
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0A0F16).copy(alpha = 0.86f),
        shadowElevation = 6.dp,
        modifier = modifier
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .testTag("web_player_stats_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (region.isEnabled) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TacticalCyan.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = region.shortCode,
                        color = TacticalCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            HudMetric(
                icon = Icons.Default.Speed,
                text = if (displayFps > 0) "$displayFps FPS" else "-- FPS",
                tint = fpsColor
            )
            if (streamStats.rttMs > 0 || streamStats.connected) {
                HudMetric(
                    icon = Icons.Default.NetworkCheck,
                    text = if (streamStats.rttMs > 0) "${streamStats.rttMs}ms" else "--ms",
                    tint = pingColor
                )
            }
            if (streamStats.bitrateMbps > 0f) {
                Text(
                    text = String.format(Locale.US, "%.1fMbps", streamStats.bitrateMbps),
                    color = Color(0xFFE4ECF7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
            if (streamStats.packetLossPercent > 0f) {
                Text(
                    text = String.format(Locale.US, "PL %.1f%%", streamStats.packetLossPercent),
                    color = lossColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
            if (streamStats.resolution.isNotBlank()) {
                val tag = if (streamStats.codec.isNotBlank()) {
                    "${streamStats.resolution} ${streamStats.codec}"
                } else {
                    streamStats.resolution
                }
                Text(
                    text = tag,
                    color = Color(0xFF9EB0C5),
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
            HudMetric(
                icon = if (perfStats.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                text = "${perfStats.batteryPercent}%",
                tint = if (perfStats.batteryPercent <= 15 && !perfStats.isCharging) MacroCoral else Color(0xFFE4ECF7)
            )
            if (perfStats.batteryTempC > 0f) {
                HudMetric(
                    icon = Icons.Default.DeviceThermostat,
                    text = String.format(Locale.US, "%.1f°C", perfStats.batteryTempC),
                    tint = if (perfStats.batteryTempC >= 42f) MacroCoral else TacticalCyan
                )
            }
        }
    }
}

@Composable
private fun HudMetric(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily,
            maxLines = 1
        )
    }
}

/**
 * Draggable floating Quick Menu handle for XenosWebPlayerActivity.
 */
@Composable
fun WebPlayerQuickBubble(
    accentColor: Color,
    regionActive: Boolean,
    controlsVisible: Boolean,
    onClick: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xFF13111E).copy(alpha = 0.88f))
            .border(
                width = 1.6.dp,
                color = if (regionActive) TacticalCyan else accentColor,
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .clickable(onClick = onClick)
            .testTag("web_player_quick_bubble"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_xenospad_fg),
            contentDescription = "Open XenosPad Better-GFN Menu",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !controlsVisible -> TurboAmber
                        regionActive -> TacticalCyan
                        else -> accentColor
                    }
                )
        )
    }
}

/**
 * Compact slide-out tactical menu for XenosWebPlayerActivity:
 * Toggle virtual controls, launch Live Layout Edit, switch profiles, switch Region Bypass,
 * toggle CSS Freestyle filters, crosshair, gyro aim, stretch video, and browser controls.
 */
@Composable
fun WebPlayerQuickMenuCard(
    activeProfile: ControllerProfile?,
    allProfiles: List<ControllerProfile>,
    webConfig: WebStreamQualityConfig,
    filterState: GameFilterState,
    controlsVisible: Boolean,
    crosshairEnabled: Boolean,
    gyroEnabled: Boolean,
    accentColor: Color,
    onClose: () -> Unit,
    onToggleControlsVisible: () -> Unit,
    onEnterLiveEdit: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onSelectRegion: (GfnRegionPreset) -> Unit,
    onSelectFilterSlot: (Int) -> Unit,
    onUpdateFilterStyle: (GameFilterStyle) -> Unit,
    onToggleStretch: () -> Unit,
    onToggleCrosshair: () -> Unit,
    onToggleGyro: () -> Unit,
    onToggleStatsHud: () -> Unit,
    onBrowserBack: () -> Unit,
    onBrowserReload: () -> Unit,
    onResetInputs: () -> Unit,
    onExitPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF13111E).copy(alpha = 0.96f),
        shadowElevation = 14.dp,
        modifier = modifier
            .widthIn(min = 300.dp, max = 380.dp)
            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .testTag("web_player_quick_menu")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_xenospad_fg),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column {
                        Text(
                            text = strings.webPlayerTitle,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeProfile?.name ?: "play.geforcenow.com",
                            color = accentColor,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("web_player_menu_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.commonClose,
                        tint = Color.White
                    )
                }
            }

            // Primary tactical toggles (2 rows of 3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickMenuTile(
                    icon = if (controlsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    label = if (controlsVisible) strings.webPlayerHideControls else strings.webPlayerShowControls,
                    active = controlsVisible,
                    accentColor = accentColor,
                    onClick = onToggleControlsVisible,
                    testTag = "web_menu_toggle_controls",
                    modifier = Modifier.weight(1f)
                )
                QuickMenuTile(
                    icon = Icons.Default.Edit,
                    label = strings.liveEditTile,
                    active = false,
                    accentColor = TacticalCyan,
                    onClick = onEnterLiveEdit,
                    testTag = "web_menu_live_edit",
                    modifier = Modifier.weight(1f)
                )
                QuickMenuTile(
                    icon = Icons.Default.AspectRatio,
                    label = strings.webPlayerStretchShort,
                    active = webConfig.stretchVideoToFill,
                    accentColor = accentColor,
                    onClick = onToggleStretch,
                    testTag = "web_menu_toggle_stretch",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickMenuTile(
                    icon = Icons.Default.ScreenRotation,
                    label = "Gyro",
                    active = gyroEnabled,
                    accentColor = TacticalCyan,
                    onClick = onToggleGyro,
                    testTag = "web_menu_toggle_gyro",
                    modifier = Modifier.weight(1f)
                )
                QuickMenuTile(
                    icon = Icons.Default.CenterFocusStrong,
                    label = strings.visualsCrosshairTab,
                    active = crosshairEnabled,
                    accentColor = accentColor,
                    onClick = onToggleCrosshair,
                    testTag = "web_menu_toggle_crosshair",
                    modifier = Modifier.weight(1f)
                )
                QuickMenuTile(
                    icon = Icons.Default.Speed,
                    label = "Stats",
                    active = webConfig.showWebRtcStatsHud,
                    accentColor = TacticalCyan,
                    onClick = onToggleStatsHud,
                    testTag = "web_menu_toggle_stats",
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            // Region Bypass quick switcher
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = TacticalCyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = strings.webPlayerRegionHeader,
                    color = TacticalCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(GfnRegionPreset.entries) { region ->
                    val selected = webConfig.regionPreset == region
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectRegion(region) },
                        label = {
                            Text(
                                text = if (region == GfnRegionPreset.OFF) "Off" else "${region.shortCode} • ${region.displayName.substringBefore(" (")}",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TacticalCyan.copy(alpha = 0.22f),
                            selectedLabelColor = TacticalCyan,
                            labelColor = Color(0xFFD5DCE8)
                        ),
                        modifier = Modifier.testTag("web_menu_region_${region.name.lowercase()}")
                    )
                }
            }

            // Freestyle Web Video Filter slots (Better xCloud-style SVG Clarity + Color Matrix)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${strings.visualsGameFilterTab} • ${filterState.activeStyle?.name ?: strings.filterOffLabel}",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0 to strings.filterOffLabel, 1 to "S1", 2 to "S2", 3 to "S3").forEach { (slot, label) ->
                    val selected = filterState.activeSlot == slot
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) accentColor.copy(alpha = 0.24f) else Color(0xFF1F1C33)
                            )
                            .border(
                                1.dp,
                                if (selected) accentColor else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectFilterSlot(slot) }
                            .testTag("web_menu_filter_slot_$slot"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) accentColor else Color(0xFFD5DCE8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Live in-stream Clarity Boost & Color Sliders when Style 1..3 is active
            val activeStyle = filterState.activeStyle
            if (activeStyle != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1B182E))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WebFilterMiniSlider(
                        label = "Clarity / Sharpen",
                        value = if (activeStyle.sharpenEnabled) activeStyle.sharpenAmount else 0f,
                        range = 0f..1f,
                        valueText = "${(if (activeStyle.sharpenEnabled) activeStyle.sharpenAmount * 100 else 0f).toInt()}%",
                        accent = TacticalCyan,
                        onChange = { v ->
                            onUpdateFilterStyle(
                                activeStyle.copy(
                                    sharpenEnabled = v > 0.02f,
                                    sharpenAmount = v
                                )
                            )
                        }
                    )
                    WebFilterMiniSlider(
                        label = "Brightness",
                        value = activeStyle.exposure,
                        range = -0.6f..0.6f,
                        valueText = String.format(Locale.US, "%+.0f%%", activeStyle.exposure * 100f),
                        accent = accentColor,
                        onChange = { v ->
                            onUpdateFilterStyle(
                                activeStyle.copy(
                                    brightnessContrastEnabled = true,
                                    exposure = v
                                )
                            )
                        }
                    )
                    WebFilterMiniSlider(
                        label = "Contrast",
                        value = activeStyle.contrast,
                        range = -0.5f..0.8f,
                        valueText = String.format(Locale.US, "%+.0f%%", activeStyle.contrast * 100f),
                        accent = accentColor,
                        onChange = { v ->
                            onUpdateFilterStyle(
                                activeStyle.copy(
                                    brightnessContrastEnabled = true,
                                    contrast = v
                                )
                            )
                        }
                    )
                    WebFilterMiniSlider(
                        label = "Vibrance",
                        value = activeStyle.colorVibrance,
                        range = -0.6f..1.0f,
                        valueText = String.format(Locale.US, "%+.0f%%", activeStyle.colorVibrance * 100f),
                        accent = TacticalCyan,
                        onChange = { v ->
                            onUpdateFilterStyle(
                                activeStyle.copy(
                                    colorEnabled = true,
                                    colorVibrance = v
                                )
                            )
                        }
                    )
                }
            }

            // Profile switcher
            if (allProfiles.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = strings.cockpitQuickProfile,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(allProfiles, key = { it.id }) { profile ->
                        val selected = profile.id == activeProfile?.id
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectProfile(profile.id) },
                            label = {
                                Text(
                                    text = profile.name,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.22f),
                                selectedLabelColor = accentColor,
                                labelColor = Color(0xFFD5DCE8)
                            ),
                            modifier = Modifier.testTag("web_menu_profile_${profile.id}")
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

            // Browser / Session Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActionButtonChip(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Back",
                    tint = Color(0xFFD5DCE8),
                    bg = Color(0xFF1F1C33),
                    onClick = onBrowserBack,
                    testTag = "web_menu_back",
                    modifier = Modifier.weight(1f)
                )
                ActionButtonChip(
                    icon = Icons.Default.Refresh,
                    label = strings.webPlayerReload,
                    tint = TacticalCyan,
                    bg = TacticalCyan.copy(alpha = 0.15f),
                    onClick = onBrowserReload,
                    testTag = "web_menu_reload",
                    modifier = Modifier.weight(1f)
                )
                ActionButtonChip(
                    icon = Icons.Default.RestartAlt,
                    label = "Unstick",
                    tint = TurboAmber,
                    bg = TurboAmber.copy(alpha = 0.15f),
                    onClick = onResetInputs,
                    testTag = "web_menu_unstick",
                    modifier = Modifier.weight(1f)
                )
                ActionButtonChip(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = strings.webPlayerExit,
                    tint = MacroCoral,
                    bg = MacroCoral.copy(alpha = 0.18f),
                    onClick = onExitPlayer,
                    testTag = "web_menu_exit",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickMenuTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (active) accentColor.copy(alpha = 0.22f) else Color(0xFF1F1C33)
            )
            .border(
                1.dp,
                if (active) accentColor else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) accentColor else Color(0xFFD5DCE8),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = if (active) accentColor else Color(0xFFD5DCE8),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionButtonChip(
    icon: ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WebFilterMiniSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    accent: Color,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color(0xFFD5DCE8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily
            )
        }
        SmoothSlider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

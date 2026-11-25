package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LocalNexusStrings
import com.example.model.PerfHudConfig
import com.example.model.VirtualControl
import com.example.service.PerfStatsSnapshot
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import kotlin.math.roundToInt

/**
 * Compact, draggable GeForce-style Performance HUD pill rendered inside OverlayControllerService.
 */
@Composable
fun PerformanceHudPill(
    stats: PerfStatsSnapshot,
    config: PerfHudConfig,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val fpsColor = when {
        stats.fps >= 50 -> accentColor
        stats.fps >= 30 -> TurboAmber
        stats.fps > 0 -> MacroCoral
        else -> Color(0xFFB8C4D4)
    }
    val batteryColor = when {
        stats.isCharging -> accentColor
        stats.batteryPercent <= 15 -> MacroCoral
        stats.batteryPercent <= 30 -> TurboAmber
        else -> Color(0xFFE6EDF5)
    }
    val tempColor = when {
        stats.batteryTempC >= 43f -> MacroCoral
        stats.batteryTempC >= 39f -> TurboAmber
        else -> TacticalCyan
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0A0F16).copy(alpha = 0.88f),
        shadowElevation = 6.dp,
        modifier = modifier
            .alpha(config.clampedOpacity())
            .border(1.dp, accentColor.copy(alpha = 0.50f), RoundedCornerShape(12.dp))
            .testTag("perf_hud_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Drag Performance HUD",
                tint = Color(0xFF6A7B92),
                modifier = Modifier.size(13.dp)
            )
            if (config.showFps) {
                PerfMetricItem(
                    icon = Icons.Default.Speed,
                    value = if (stats.fps > 0) "${stats.fps} FPS" else "-- FPS",
                    tint = fpsColor
                )
            }
            if (config.showBattery) {
                PerfMetricItem(
                    icon = if (stats.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                    value = "${stats.batteryPercent}%",
                    tint = batteryColor
                )
            }
            if (config.showTemp) {
                val tempLabel = if (stats.batteryTempC > 0f) {
                    "%.1f°C".format(stats.batteryTempC)
                } else {
                    "--°C"
                }
                PerfMetricItem(
                    icon = Icons.Default.DeviceThermostat,
                    value = tempLabel,
                    tint = tempColor
                )
            }
            if (config.showRam) {
                PerfMetricItem(
                    icon = Icons.Default.Memory,
                    value = "${stats.usedRamPercent}%",
                    tint = Color(0xFFD6E0EC)
                )
            }
        }
    }
}

@Composable
private fun PerfMetricItem(
    icon: ImageVector,
    value: String,
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
            text = value,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily,
            maxLines = 1
        )
    }
}

/**
 * Compact floating Live Edit toolbar rendered over the running game so users can reposition,
 * resize, change opacity, snap to grid, and Save/Cancel directly on top of GeForce NOW / Android games.
 */
@Composable
fun LiveOverlayEditToolbar(
    profileName: String,
    selectedControl: VirtualControl?,
    snapToGrid: Boolean,
    accentColor: Color,
    onSizeDelta: (Float) -> Unit,
    onOpacityDelta: (Float) -> Unit,
    onToggleSnap: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0C121B).copy(alpha = 0.95f),
        shadowElevation = 10.dp,
        modifier = modifier
            .border(1.2.dp, accentColor.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .testTag("live_edit_toolbar")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top bar: LIVE EDIT badge + Profile/Selected readout + Snap + Cancel + Save
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "${strings.liveEditToolbarTitle} • $profileName",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (selectedControl != null) {
                            "${selectedControl.label}  X=${(selectedControl.xPercent * 100).roundToInt()}%  Y=${(selectedControl.yPercent * 100).roundToInt()}%"
                        } else {
                            strings.liveEditDragHint
                        },
                        color = if (selectedControl != null) TacticalCyan else Color(0xFF9EB0C5),
                        fontSize = 10.sp,
                        fontFamily = if (selectedControl != null) JetBrainsMonoFontFamily else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Snap toggle chip
                CompactEditChip(
                    icon = Icons.Default.GridOn,
                    label = strings.liveEditSnapLabel,
                    containerColor = if (snapToGrid) accentColor.copy(alpha = 0.22f) else Color(0xFF1A2331),
                    contentColor = if (snapToGrid) accentColor else Color(0xFFB0C0D4),
                    onClick = onToggleSnap,
                    testTag = "live_edit_snap_toggle"
                )

                // Cancel button
                CompactEditChip(
                    icon = Icons.Default.Close,
                    label = strings.liveEditCancelButton,
                    containerColor = MacroCoral.copy(alpha = 0.18f),
                    contentColor = MacroCoral,
                    onClick = onCancel,
                    testTag = "live_edit_cancel_button"
                )

                // Save button
                CompactEditChip(
                    icon = Icons.Default.Check,
                    label = strings.liveEditSaveButton,
                    containerColor = accentColor,
                    contentColor = Color(0xFF071105),
                    onClick = onSave,
                    testTag = "live_edit_save_button"
                )
            }

            // Secondary row: selected control Size & Opacity steppers
            if (selectedControl != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactStepperControl(
                        label = "${strings.liveEditSizeLabel} ${selectedControl.sizeDp.roundToInt()}dp",
                        accentColor = accentColor,
                        onDecrement = { onSizeDelta(-8f) },
                        onIncrement = { onSizeDelta(8f) },
                        decTag = "live_edit_size_minus",
                        incTag = "live_edit_size_plus"
                    )
                    CompactStepperControl(
                        label = "${strings.liveEditOpacityLabel} ${(selectedControl.opacity * 100).roundToInt()}%",
                        accentColor = TacticalCyan,
                        onDecrement = { onOpacityDelta(-0.08f) },
                        onIncrement = { onOpacityDelta(0.08f) },
                        decTag = "live_edit_alpha_minus",
                        incTag = "live_edit_alpha_plus"
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactEditChip(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun CompactStepperControl(
    label: String,
    accentColor: Color,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decTag: String,
    incTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141D29))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF202C3D))
                .clickable(onClick = onDecrement)
                .testTag(decTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease $label",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.width(2.dp))
        Text(
            text = label,
            color = accentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily,
            maxLines = 1
        )
        Spacer(Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF202C3D))
                .clickable(onClick = onIncrement)
                .testTag(incTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase $label",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

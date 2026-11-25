package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.GyroActivationMode
import com.example.model.GyroAimConfig
import com.example.model.LocalNexusStrings
import com.example.model.PerfHudConfig
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GyroAimSetupCard(
    config: GyroAimConfig,
    hasHardwareGyro: Boolean,
    onUpdate: (GyroAimConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (config.enabled) primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large
            )
            .testTag("gyro_aim_setup_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = null,
                    tint = if (config.enabled) primary else TacticalCyan,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.gyroAimTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (config.enabled) primary else TacticalCyan
                    )
                    Text(
                        text = strings.gyroAimSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.enabled,
                    enabled = hasHardwareGyro,
                    onCheckedChange = { onUpdate(config.copy(enabled = it)) },
                    modifier = Modifier.testTag("gyro_enable_switch")
                )
            }

            if (!hasHardwareGyro) {
                Surface(
                    color = TurboAmber.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = TurboAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = strings.gyroNoHardware,
                            style = MaterialTheme.typography.bodySmall,
                            color = TurboAmber
                        )
                    }
                }
            }

            if (hasHardwareGyro) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.gyroModeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = config.activationMode == GyroActivationMode.ALWAYS_ON,
                            onClick = { onUpdate(config.copy(activationMode = GyroActivationMode.ALWAYS_ON)) },
                            label = { Text(strings.gyroModeAlways) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("gyro_mode_always_chip")
                        )
                        FilterChip(
                            selected = config.activationMode == GyroActivationMode.HOLD_ADS,
                            onClick = { onUpdate(config.copy(activationMode = GyroActivationMode.HOLD_ADS)) },
                            label = { Text(strings.gyroModeHoldAds) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("gyro_mode_hold_ads_chip")
                        )
                    }
                }

                // Sensitivity slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.gyroSensitivityLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "%.2fx".format(config.clampedSensitivity()),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = primary
                        )
                    }
                    SmoothSlider(
                        value = config.clampedSensitivity(),
                        onValueChange = { onUpdate(config.copy(sensitivity = it)) },
                        valueRange = 0.2f..3.5f,
                        accentColor = primary,
                        modifier = Modifier.testTag("gyro_sensitivity_slider")
                    )
                }

                // Deadzone slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.gyroDeadzoneLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "%.2f rad/s".format(config.clampedDeadzone()),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = TacticalCyan
                        )
                    }
                    SmoothSlider(
                        value = config.clampedDeadzone(),
                        onValueChange = { onUpdate(config.copy(deadzoneRadPerSec = it)) },
                        valueRange = 0.0f..0.25f,
                        accentColor = TacticalCyan,
                        modifier = Modifier.testTag("gyro_deadzone_slider")
                    )
                }

                // Smoothing slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.gyroSmoothingLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(config.clampedSmoothing() * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = primary
                        )
                    }
                    SmoothSlider(
                        value = config.clampedSmoothing(),
                        onValueChange = { onUpdate(config.copy(smoothing = it)) },
                        valueRange = 0.0f..0.80f,
                        accentColor = primary,
                        modifier = Modifier.testTag("gyro_smoothing_slider")
                    )
                }

                // Invert X & Y chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = config.invertX,
                        onClick = { onUpdate(config.copy(invertX = !config.invertX)) },
                        label = { Text(strings.gyroInvertX) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("gyro_invert_x_chip")
                    )
                    FilterChip(
                        selected = config.invertY,
                        onClick = { onUpdate(config.copy(invertY = !config.invertY)) },
                        label = { Text(strings.gyroInvertY) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("gyro_invert_y_chip")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerfHudSetupCard(
    config: PerfHudConfig,
    onUpdate: (PerfHudConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (config.enabled) primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large
            )
            .testTag("perf_hud_setup_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = if (config.enabled) primary else TacticalCyan,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.perfHudTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (config.enabled) primary else TacticalCyan
                    )
                    Text(
                        text = strings.perfHudSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { onUpdate(config.copy(enabled = it)) },
                    modifier = Modifier.testTag("perf_hud_enable_switch")
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = config.showFps,
                    onClick = { onUpdate(config.copy(showFps = !config.showFps)) },
                    label = { Text(strings.perfHudShowFps) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("perf_hud_fps_chip")
                )
                FilterChip(
                    selected = config.showBattery,
                    onClick = { onUpdate(config.copy(showBattery = !config.showBattery)) },
                    label = { Text(strings.perfHudShowBattery) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("perf_hud_battery_chip")
                )
                FilterChip(
                    selected = config.showTemp,
                    onClick = { onUpdate(config.copy(showTemp = !config.showTemp)) },
                    label = { Text(strings.perfHudShowTemp) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("perf_hud_temp_chip")
                )
                FilterChip(
                    selected = config.showRam,
                    onClick = { onUpdate(config.copy(showRam = !config.showRam)) },
                    label = { Text(strings.perfHudShowRam) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("perf_hud_ram_chip")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.perfHudOpacityLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(config.clampedOpacity() * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = primary
                    )
                }
                SmoothSlider(
                    value = config.clampedOpacity(),
                    onValueChange = { onUpdate(config.copy(opacity = it)) },
                    valueRange = 0.35f..1.0f,
                    accentColor = primary,
                    modifier = Modifier.testTag("perf_hud_opacity_slider")
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.input.PhysicalControllerInfo
import com.example.input.PhysicalControllerMonitor
import com.example.model.PhysicalControllerConfig
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TacticalCyan
import kotlin.math.roundToInt

/**
 * Setup card for Physical Bluetooth/USB Controller detection, Auto-Hide Touch Controls,
 * Player-1 slot priority, Nintendo face-button swap, and analog stick deadzone.
 */
@Composable
fun PhysicalControllerSetupCard(
    config: PhysicalControllerConfig,
    connectedControllers: List<PhysicalControllerInfo>,
    onUpdate: (PhysicalControllerConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val hasController = connectedControllers.isNotEmpty()

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (hasController) TacticalCyan.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large
            )
            .testTag("setup_physical_controller_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = if (hasController) TacticalCyan else primary
                    )
                    Column {
                        Text(
                            text = "PHYSICAL CONTROLLER SUPPORT",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasController) TacticalCyan else primary
                        )
                        Text(
                            text = "Bluetooth & USB gamepads with Auto-Hide Touch Controls",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = { PhysicalControllerMonitor.scanNow() },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("setup_pad_rescan_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan for physical controllers",
                        tint = TacticalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Live detected hardware controller banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (hasController) {
                    TacticalCyan.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (hasController) Color(0xFF4ADE80) else Color(0xFF94A3B8))
                        )
                        Text(
                            text = if (hasController) {
                                "${connectedControllers.size} Controller${if (connectedControllers.size > 1) "s" else ""} Connected"
                            } else {
                                "No Physical Controller Detected"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasController) TacticalCyan else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (hasController) {
                        connectedControllers.forEach { pad ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (pad.isBluetoothOrExternal) {
                                        Icons.Default.BluetoothConnected
                                    } else {
                                        Icons.Default.Usb
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = pad.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Connect any Xbox, PlayStation DualSense/DualShock, Switch Pro, Backbone, or USB-C gamepad. Touch controls will auto-hide once detected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            PhysicalControllerToggleRow(
                title = "Auto-Hide Touch Controls",
                subtitle = "Automatically hides virtual on-screen buttons when a physical gamepad is connected (can still be shown manually anytime)",
                checked = config.autoHideTouchControls,
                onCheckedChange = { onUpdate(config.copy(autoHideTouchControls = it)) },
                testTag = "setup_pad_auto_hide"
            )

            PhysicalControllerToggleRow(
                title = "Prioritize Physical Controller (Player 1)",
                subtitle = "Places your physical Bluetooth/USB controller in Slot 0 ahead of XenosPad's virtual gamepad in Web Play Mode",
                checked = config.preferPhysicalSlotZero,
                onCheckedChange = { onUpdate(config.copy(preferPhysicalSlotZero = it)) },
                testTag = "setup_pad_slot_zero"
            )

            PhysicalControllerToggleRow(
                title = "Swap A/B & X/Y (Nintendo Layout)",
                subtitle = "Remaps Nintendo Switch Pro / Joy-Con face buttons to match Xbox layout in GeForce NOW",
                checked = config.swapFaceButtonsAB,
                onCheckedChange = { onUpdate(config.copy(swapFaceButtonsAB = it)) },
                testTag = "setup_pad_swap_ab"
            )

            // Stick Deadzone Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                var deadzoneDraft by remember { mutableFloatStateOf(config.stickDeadzone) }
                var dragging by remember { mutableStateOf(false) }
                LaunchedEffect(config.stickDeadzone, dragging) {
                    if (!dragging) deadzoneDraft = config.stickDeadzone
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Analog Stick Deadzone",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(deadzoneDraft * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                SmoothSlider(
                    value = config.stickDeadzone.coerceIn(0f, 0.30f),
                    onValueChange = { v ->
                        dragging = true
                        deadzoneDraft = v
                    },
                    onValueChangeFinished = { v ->
                        dragging = false
                        deadzoneDraft = v
                        onUpdate(config.copy(stickDeadzone = v.coerceIn(0f, 0.30f)))
                    },
                    valueRange = 0f..0.30f,
                    accentColor = TacticalCyan,
                    modifier = Modifier.testTag("setup_pad_deadzone_slider")
                )
            }
        }
    }
}

@Composable
private fun PhysicalControllerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.OverlaySettings
import com.example.model.ControllerNotation
import com.example.ui.SystemReadiness
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import kotlin.math.roundToInt

@Composable
fun SetupScreen(
    settings: OverlaySettings,
    readiness: SystemReadiness,
    onHapticsChange: (Boolean) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onGridSnapChange: (Boolean) -> Unit,
    onNotationChange: (ControllerNotation) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onShowTelemetryChange: (Boolean) -> Unit,
    onAutoLaunchChange: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Overlay & System Setup",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Configure button notation, tactile feedback, and GeForce NOW input permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
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
                        Text(
                            text = "ANDROID OVERLAY & IME PERMISSIONS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        PermissionStepRow(
                            icon = Icons.Default.Layers,
                            title = "1. Display Over Other Apps",
                            description = "Allows floating virtual buttons and analog sticks to appear above GeForce NOW.",
                            granted = readiness.canDrawOverlays,
                            actionLabel = if (readiness.canDrawOverlays) "Granted" else "Open Settings",
                            testTag = "setup_overlay_perm_button",
                            onClick = onRequestOverlayPermission
                        )

                        PermissionStepRow(
                            icon = Icons.Default.Keyboard,
                            title = "2. Enable NexusPad IME",
                            description = "Android InputMethodService dispatches real KeyEvents for gamepad & PC keyboard bindings.",
                            granted = readiness.isImeEnabled,
                            actionLabel = if (readiness.isImeEnabled) "Enabled" else "Enable IME",
                            testTag = "setup_enable_ime_button",
                            onClick = onOpenImeSettings
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onShowImePicker,
                                enabled = readiness.isImeEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("setup_select_ime_button")
                            ) {
                                Text(if (readiness.isImeSelected) "IME Selected ✓" else "Select Active IME")
                            }

                            OutlinedButton(
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("setup_notif_perm_button")
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (readiness.hasNotificationPermission) "Alerts Ready" else "Allow Alerts")
                            }
                        }
                    }
                }
            }

            item {
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
                        Text(
                            text = "CONTROLLER NOTATION",
                            style = MaterialTheme.typography.labelMedium,
                            color = TacticalCyan
                        )
                        ControllerNotation.entries.forEach { notation ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNotationChange(notation) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.controllerNotation == notation,
                                    onClick = { onNotationChange(notation) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = notation.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "HUD & INTERACTION PREFERENCES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Global Overlay Opacity", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${(settings.globalOpacity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = settings.globalOpacity,
                                onValueChange = onOpacityChange,
                                valueRange = 0.25f..1.0f,
                                modifier = Modifier.testTag("setup_opacity_slider")
                            )
                        }

                        PreferenceSwitchRow(
                            icon = Icons.Default.Vibration,
                            title = "Tactile Key Haptics",
                            subtitle = "Vibrate lightly on virtual button & stick press",
                            checked = settings.hapticsEnabled,
                            onCheckedChange = onHapticsChange,
                            testTag = "setup_haptics_switch"
                        )
                        PreferenceSwitchRow(
                            icon = Icons.Default.GridOn,
                            title = "Snap Controls to Grid",
                            subtitle = "Align buttons cleanly in the Layout Studio",
                            checked = settings.gridSnapEnabled,
                            onCheckedChange = onGridSnapChange,
                            testTag = "setup_gridsnap_switch"
                        )
                        PreferenceSwitchRow(
                            icon = Icons.Default.Gamepad,
                            title = "Show Telemetry HUD",
                            subtitle = "Display live KeyEvent & axis monitor in Sandbox",
                            checked = settings.showTelemetryOverlay,
                            onCheckedChange = onShowTelemetryChange,
                            testTag = "setup_telemetry_switch"
                        )
                        PreferenceSwitchRow(
                            icon = Icons.Default.RocketLaunch,
                            title = "Auto-Launch GeForce NOW",
                            subtitle = "Open GeForce NOW automatically when starting overlay",
                            checked = settings.autoLaunchGeForceNow,
                            onCheckedChange = onAutoLaunchChange,
                            testTag = "setup_autolaunch_switch"
                        )
                        PreferenceSwitchRow(
                            icon = Icons.Default.DarkMode,
                            title = "Tactical Dark Cockpit",
                            subtitle = "High-contrast obsidian theme for low-glare play",
                            checked = settings.darkTheme,
                            onCheckedChange = onDarkThemeChange,
                            testTag = "setup_theme_switch"
                        )
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TacticalCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Note: On non-rooted Android devices, virtual buttons dispatch real KeyEvents through the NexusPad IME bridge for keyboard and digital gamepad bindings. Analog axes and live touch-to-controller ergonomics can be calibrated and tested in the built-in Test Sandbox.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PermissionStepRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else icon,
            contentDescription = null,
            tint = if (granted) GeForceLime else TurboAmber,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onClick,
            modifier = Modifier
                .height(48.dp)
                .testTag(testTag)
        ) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
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

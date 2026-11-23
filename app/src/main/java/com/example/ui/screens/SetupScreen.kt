package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capture.CaptureBridge
import com.example.data.OverlaySettings
import com.example.data.VibrationMode
import com.example.model.AppLanguage
import com.example.model.ControllerNotation
import com.example.model.LocalNexusStrings
import com.example.model.RecordingConfigHelper
import com.example.model.RecordingFps
import com.example.model.RecordingResolution
import com.example.model.TargetPlatformMode
import com.example.ui.SystemReadiness
import com.example.ui.components.SmoothSlider
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import com.example.update.GitHubUpdateManager
import com.example.update.UpdateUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    settings: OverlaySettings,
    readiness: SystemReadiness,
    updateState: UpdateUiState,
    onVibrationModeChange: (VibrationMode) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onGridSnapChange: (Boolean) -> Unit,
    onGridDivisionsChange: (Int) -> Unit,
    onNotationChange: (ControllerNotation) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onShowTelemetryChange: (Boolean) -> Unit,
    onAutoLaunchChange: (Boolean) -> Unit,
    onFloatingKeyboardDefaultChange: (Boolean) -> Unit,
    onOpenFloatingKeyboard: () -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onTargetModeChange: (TargetPlatformMode) -> Unit,
    onRecordingResolutionChange: (RecordingResolution) -> Unit,
    onRecordingFpsChange: (RecordingFps) -> Unit,
    onGithubRepoSlugChange: (String) -> Unit,
    onAutoCheckUpdatesChange: (Boolean) -> Unit,
    onCheckForUpdates: (String?) -> Unit,
    onDownloadUpdate: () -> Unit,
    onCancelUpdateDownload: () -> Unit,
    onInstallUpdate: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    val context = LocalContext.current
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
                    text = strings.setupTitle,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = strings.setupSubtitle,
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = TacticalCyan)
                            Column {
                                Text(
                                    text = strings.languageCardTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TacticalCyan
                                )
                                Text(
                                    text = strings.languageSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = settings.appLanguage == lang,
                                    onClick = { onAppLanguageChange(lang) },
                                    label = { Text(lang.nativeLabel) },
                                    modifier = Modifier.testTag("setup_language_${lang.name.lowercase()}")
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Gamepad, contentDescription = null, tint = GeForceLime)
                            Column {
                                Text(
                                    text = strings.targetModeCardTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GeForceLime
                                )
                                Text(
                                    text = strings.targetModeSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TargetPlatformMode.entries.forEach { mode ->
                            val title = when (mode) {
                                TargetPlatformMode.GEFORCE_NOW -> strings.targetModeGfnTitle
                                TargetPlatformMode.ANDROID_NATIVE -> strings.targetModeAndroidTitle
                            }
                            val subtitle = when (mode) {
                                TargetPlatformMode.GEFORCE_NOW -> strings.targetModeGfnSubtitle
                                TargetPlatformMode.ANDROID_NATIVE -> strings.targetModeAndroidSubtitle
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTargetModeChange(mode) }
                                    .padding(vertical = 4.dp)
                                    .testTag("setup_target_mode_${mode.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.targetMode == mode,
                                    onClick = { onTargetModeChange(mode) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                RecordingQualityCard(
                    resolution = settings.recordingResolution,
                    fps = settings.recordingFps,
                    onResolutionChange = onRecordingResolutionChange,
                    onFpsChange = onRecordingFpsChange
                )
            }

            item {
                GitHubUpdateCard(
                    savedRepoSlug = settings.githubRepoSlug,
                    autoCheckUpdates = settings.autoCheckUpdates,
                    canInstallPackages = readiness.canInstallPackages,
                    updateState = updateState,
                    onSaveRepoSlug = onGithubRepoSlugChange,
                    onAutoCheckChange = onAutoCheckUpdatesChange,
                    onCheckNow = onCheckForUpdates,
                    onDownloadUpdate = onDownloadUpdate,
                    onCancelDownload = onCancelUpdateDownload,
                    onInstallUpdate = onInstallUpdate,
                    onOpenReleaseUrl = { url -> GitHubUpdateManager.openReleaseInBrowser(context, url) }
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "BUTTON VIBRATION / HAPTICS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        VibrationMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onVibrationModeChange(mode) }
                                    .padding(vertical = 4.dp)
                                    .testTag("setup_vibration_${mode.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.vibrationMode == mode,
                                    onClick = { onVibrationModeChange(mode) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                            title = "2. Enable NexusPad IME & Keyboard",
                            description = "Dispatches real KeyEvents for gamepad/keyboard binds and provides a built-in virtual typing keyboard.",
                            granted = readiness.isImeEnabled,
                            actionLabel = if (readiness.isImeEnabled) "Enabled" else "Enable IME",
                            testTag = "setup_enable_ime_button",
                            onClick = onOpenImeSettings
                        )

                        PermissionStepRow(
                            icon = Icons.Default.Mouse,
                            title = "3. Mouse Click & Scroll Gestures",
                            description = "Accessibility gesture engine for Left Click, Right Click, Middle Click, and Scroll Up/Down.",
                            granted = readiness.isAccessibilityEnabled,
                            actionLabel = if (readiness.isAccessibilityEnabled) "Enabled" else "Enable",
                            testTag = "setup_accessibility_button",
                            onClick = onOpenAccessibilitySettings
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

                        PreferenceSwitchRow(
                            icon = Icons.Default.Keyboard,
                            title = strings.floatingKeyboardTitle,
                            subtitle = strings.floatingKeyboardSubtitle,
                            checked = settings.floatingKeyboardDefault,
                            onCheckedChange = onFloatingKeyboardDefaultChange,
                            testTag = "setup_floating_kb_switch"
                        )

                        OutlinedButton(
                            onClick = onOpenFloatingKeyboard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("setup_open_floating_kb_button")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.openFloatingKeyboard)
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
                            var opacityDraft by remember { mutableFloatStateOf(settings.globalOpacity) }
                            var opacityDragging by remember { mutableStateOf(false) }
                            LaunchedEffect(settings.globalOpacity, opacityDragging) {
                                if (!opacityDragging) opacityDraft = settings.globalOpacity
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Global Overlay Opacity", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${(opacityDraft * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            SmoothSlider(
                                value = settings.globalOpacity,
                                onValueChange = { v ->
                                    opacityDragging = true
                                    opacityDraft = v
                                },
                                onValueChangeFinished = { v ->
                                    opacityDragging = false
                                    opacityDraft = v
                                    onOpacityChange(v)
                                },
                                valueRange = 0.25f..1.0f,
                                accentColor = GeForceLime,
                                modifier = Modifier.testTag("setup_opacity_slider")
                            )
                        }

                        PreferenceSwitchRow(
                            icon = Icons.Default.GridOn,
                            title = strings.snapToGridTitle,
                            subtitle = strings.snapToGridSubtitle,
                            checked = settings.gridSnapEnabled,
                            onCheckedChange = onGridSnapChange,
                            testTag = "setup_gridsnap_switch"
                        )
                        Column {
                            val persistedDivsX = settings.gridDivisions.coerceIn(4, 32)
                            var gridDraft by remember { mutableFloatStateOf(persistedDivsX.toFloat()) }
                            var gridDragging by remember { mutableStateOf(false) }
                            LaunchedEffect(persistedDivsX, gridDragging) {
                                if (!gridDragging) gridDraft = persistedDivsX.toFloat()
                            }
                            val displayDivsX = gridDraft.roundToInt().coerceIn(4, 32)
                            val displayDivsY = (displayDivsX * 0.66f).roundToInt().coerceIn(4, 24)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.gridSizeTitle, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${displayDivsX} × ${displayDivsY}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            SmoothSlider(
                                value = persistedDivsX.toFloat(),
                                onValueChange = { v ->
                                    gridDragging = true
                                    gridDraft = v
                                },
                                onValueChangeFinished = { v ->
                                    gridDragging = false
                                    val snapped = v.roundToInt().coerceIn(4, 32)
                                    gridDraft = snapped.toFloat()
                                    onGridDivisionsChange(snapped)
                                },
                                valueRange = 4f..32f,
                                accentColor = TacticalCyan,
                                modifier = Modifier.testTag("setup_grid_size_slider")
                            )
                        }
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
                            text = "Note: On non-rooted Android devices, virtual buttons dispatch real KeyEvents through the NexusPad IME bridge for keyboard and digital gamepad bindings. Multi-touch is supported simultaneously across all overlay buttons and sticks.",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordingQualityCard(
    resolution: RecordingResolution,
    fps: RecordingFps,
    onResolutionChange: (RecordingResolution) -> Unit,
    onFpsChange: (RecordingFps) -> Unit
) {
    val bitrateLabel = remember(resolution, fps) {
        RecordingConfigHelper.formatBitrateMbps(resolution, fps)
    }
    val storageMbPerMin = remember(resolution, fps) {
        RecordingConfigHelper.estimateStorageMbPerMinute(resolution, fps)
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
                Icon(Icons.Default.Videocam, contentDescription = null, tint = TacticalCyan)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SCREEN RECORDING & REPLAY QUALITY",
                        style = MaterialTheme.typography.labelMedium,
                        color = TacticalCyan
                    )
                    Text(
                        text = "Choose output resolution and frame rate for Recordings & Instant Replay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Resolution",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecordingResolution.entries.forEach { res ->
                        FilterChip(
                            selected = resolution == res,
                            onClick = { onResolutionChange(res) },
                            label = { Text(res.label) },
                            modifier = Modifier.testTag("setup_rec_res_${res.name.lowercase()}")
                        )
                    }
                }
                Text(
                    text = resolution.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Frame Rate (FPS)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecordingFps.entries.forEach { option ->
                        FilterChip(
                            selected = fps == option,
                            onClick = { onFpsChange(option) },
                            label = { Text(option.label) },
                            modifier = Modifier.testTag("setup_rec_fps_${option.fps}")
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TARGET H.264 ENCODER PROFILE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${resolution.shortLabel} @ ${fps.label} • $bitrateLabel",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GeForceLime
                        )
                    }
                    Surface(
                        color = TacticalCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "~$storageMbPerMin MB/min",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = TacticalCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = "Auto-Fallback: If your phone's hardware H.264 encoder caps 1440p or 90/120 FPS, NexusPad automatically steps down to the highest stable resolution & FPS without failing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GitHubUpdateCard(
    savedRepoSlug: String,
    autoCheckUpdates: Boolean,
    canInstallPackages: Boolean,
    updateState: UpdateUiState,
    onSaveRepoSlug: (String) -> Unit,
    onAutoCheckChange: (Boolean) -> Unit,
    onCheckNow: (String?) -> Unit,
    onDownloadUpdate: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenReleaseUrl: (String) -> Unit
) {
    val builtInRepo = remember { GitHubUpdateManager.defaultBuiltInRepo() }
    val effectiveSaved = savedRepoSlug.ifBlank { builtInRepo }
    var repoDraft by remember(effectiveSaved) { mutableStateOf(effectiveSaved) }

    val highlightBorder = when (updateState) {
        is UpdateUiState.UpdateAvailable, is UpdateUiState.ReadyToInstall -> GeForceLime
        is UpdateUiState.Error -> MacroCoral
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, highlightBorder, MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = GeForceLime)
                    Column {
                        Text(
                            text = "APP UPDATES (GITHUB RELEASES)",
                            style = MaterialTheme.typography.labelMedium,
                            color = GeForceLime
                        )
                        Text(
                            text = "Update directly inside NexusPad without deleting your current version or losing layouts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Installed Version",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "v${GitHubUpdateManager.currentVersionName()} (Build ${GitHubUpdateManager.currentVersionCode()})",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("setup_installed_version_text")
                    )
                }
            }

            OutlinedTextField(
                value = repoDraft,
                onValueChange = { repoDraft = it },
                label = { Text("GitHub Repository (owner/repo)") },
                placeholder = { Text("e.g. yourname/NexusPad") },
                singleLine = true,
                supportingText = {
                    Text(
                        if (builtInRepo.isNotBlank()) {
                            "Auto-configured from GitHub Actions ($builtInRepo). Same signing key preserves all app data."
                        } else {
                            "Paste your GitHub repo slug or URL. GitHub Actions builds auto-fill this automatically."
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_github_repo_input")
            )

            PreferenceSwitchRow(
                icon = Icons.Default.Refresh,
                title = "Auto-Check on App Launch",
                subtitle = "Silently check GitHub Releases for newer APKs when NexusPad opens",
                checked = autoCheckUpdates,
                onCheckedChange = onAutoCheckChange,
                testTag = "setup_auto_update_switch"
            )

            val isBusy = updateState is UpdateUiState.Checking || updateState is UpdateUiState.Downloading
            Button(
                onClick = {
                    onSaveRepoSlug(repoDraft)
                    onCheckNow(repoDraft)
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("setup_check_updates_button")
            ) {
                if (updateState is UpdateUiState.Checking) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Checking GitHub Releases...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Check for Updates")
                }
            }

            when (updateState) {
                is UpdateUiState.Idle -> Unit

                is UpdateUiState.Checking -> Unit

                is UpdateUiState.UpToDate -> {
                    Surface(
                        color = GeForceLime.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GeForceLime, modifier = Modifier.size(20.dp))
                            Text(
                                text = "You're up to date! Installed v${updateState.currentVersion} matches latest release ${updateState.latestTag}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                is UpdateUiState.UpdateAvailable -> {
                    val release = updateState.release
                    Surface(
                        color = GeForceLime.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GeForceLime.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
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
                                    text = "NEW UPDATE: ${release.tagName}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GeForceLime
                                )
                                if (release.apkSizeBytes > 0L) {
                                    Text(
                                        text = CaptureBridge.formatFileSize(release.apkSizeBytes),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (release.releaseNotes.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                ) {
                                    Text(
                                        text = release.releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .verticalScroll(rememberScrollState())
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (release.apkDownloadUrl != null) {
                                    Button(
                                        onClick = onDownloadUpdate,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GeForceLime,
                                            contentColor = Color(0xFF0A1400)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("setup_download_update_button")
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Download & Update", fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (release.htmlUrl.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { onOpenReleaseUrl(release.htmlUrl) },
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("setup_open_release_button")
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("GitHub")
                                    }
                                }
                            }
                        }
                    }
                }

                is UpdateUiState.Downloading -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading ${updateState.release.tagName}...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (updateState.totalBytes > 0L) {
                                        "${(updateState.progress * 100).roundToInt()}%"
                                    } else {
                                        CaptureBridge.formatFileSize(updateState.bytesDownloaded)
                                    },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TacticalCyan
                                )
                            }
                            if (updateState.totalBytes > 0L) {
                                LinearProgressIndicator(
                                    progress = { updateState.progress },
                                    color = GeForceLime,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "${CaptureBridge.formatFileSize(updateState.bytesDownloaded)} / ${CaptureBridge.formatFileSize(updateState.totalBytes)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LinearProgressIndicator(
                                    color = GeForceLime,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            OutlinedButton(
                                onClick = onCancelDownload,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("setup_cancel_download_button")
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cancel Download")
                            }
                        }
                    }
                }

                is UpdateUiState.ReadyToInstall -> {
                    Surface(
                        color = GeForceLime.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GeForceLime.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ready to install ${updateState.release.tagName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GeForceLime
                            )
                            Text(
                                text = if (canInstallPackages) {
                                    "Tap below to launch Android's installer. Your existing layouts, filters, and gallery are preserved."
                                } else {
                                    "One-time setup: Android will ask to enable \"Install unknown apps\" for NexusPad, then tap Install Update again."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onInstallUpdate,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GeForceLime,
                                    contentColor = Color(0xFF0A1400)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("setup_install_update_button")
                            ) {
                                Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (canInstallPackages) "Install Update (Keeps Data)" else "Allow & Install Update",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                is UpdateUiState.Error -> {
                    Surface(
                        color = MacroCoral.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MacroCoral, modifier = Modifier.size(20.dp))
                            Text(
                                text = updateState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

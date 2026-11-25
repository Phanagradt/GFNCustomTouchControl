package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.VibrationMode
import com.example.model.BindingDomain
import com.example.model.ControlType
import com.example.model.ControllerProfile
import com.example.model.LocalNexusStrings
import com.example.ui.SystemReadiness
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.MacroViolet
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber

@Composable
fun CockpitScreen(
    activeProfile: ControllerProfile?,
    allProfiles: List<ControllerProfile>,
    readiness: SystemReadiness,
    isOverlayRunning: Boolean,
    vibrationMode: VibrationMode,
    onVibrationModeChange: (VibrationMode) -> Unit,
    onToggleOverlay: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenSandbox: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenImeSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onLaunchGeForceNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroCockpitCard(
                    activeProfile = activeProfile,
                    isOverlayRunning = isOverlayRunning,
                    onToggleOverlay = onToggleOverlay,
                    onOpenEditor = { activeProfile?.let { onOpenEditor(it.id) } }
                )
            }

            item {
                Text(
                    text = strings.cockpitIntegrationHeader,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReadinessRowCard(
                        icon = Icons.Default.Layers,
                        title = strings.cockpitOverlayEngineTitle,
                        subtitle = if (readiness.canDrawOverlays) {
                            strings.cockpitOverlayEngineGranted
                        } else {
                            strings.cockpitOverlayEngineMissing
                        },
                        statusText = if (readiness.canDrawOverlays) strings.commonReady else strings.commonGrant,
                        isReady = readiness.canDrawOverlays,
                        testTag = "readiness_overlay_card",
                        onClick = onRequestOverlayPermission
                    )
                    ReadinessRowCard(
                        icon = Icons.Default.Keyboard,
                        title = strings.cockpitImeTitle,
                        subtitle = when {
                            readiness.isImeSelected -> strings.cockpitImeSelected
                            readiness.isImeEnabled -> strings.cockpitImeEnabled
                            else -> strings.cockpitImeDisabled
                        },
                        statusText = when {
                            readiness.isImeSelected -> strings.commonActive
                            readiness.isImeEnabled -> strings.commonSelect
                            else -> strings.commonSetup
                        },
                        isReady = readiness.isImeSelected,
                        testTag = "readiness_ime_card",
                        onClick = onOpenImeSettings
                    )
                    ReadinessRowCard(
                        icon = Icons.Default.Mouse,
                        title = strings.cockpitMouseTitle,
                        subtitle = if (readiness.isAccessibilityEnabled) {
                            strings.cockpitMouseReady
                        } else {
                            strings.cockpitMouseMissing
                        },
                        statusText = if (readiness.isAccessibilityEnabled) strings.commonReady else strings.commonEnable.uppercase(),
                        isReady = readiness.isAccessibilityEnabled,
                        testTag = "readiness_accessibility_card",
                        onClick = onOpenAccessibilitySettings
                    )
                    ReadinessRowCard(
                        icon = Icons.Default.RocketLaunch,
                        title = strings.cockpitGfnTitle,
                        subtitle = if (readiness.isGeForceNowInstalled) {
                            strings.cockpitGfnInstalled
                        } else {
                            strings.cockpitGfnMissing
                        },
                        statusText = if (readiness.isGeForceNowInstalled) strings.commonLaunch else strings.navSandbox.uppercase(),
                        isReady = readiness.isGeForceNowInstalled,
                        testTag = "readiness_gfn_card",
                        onClick = if (readiness.isGeForceNowInstalled) onLaunchGeForceNow else onOpenSandbox
                    )
                }
            }

            if (allProfiles.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.cockpitQuickProfile,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "${allProfiles.size} ${strings.cockpitSavedSuffix}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allProfiles, key = { it.id }) { profile ->
                            val selected = profile.id == activeProfile?.id
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectProfile(profile.id) },
                                label = {
                                    Text(
                                        text = profile.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Gamepad,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("quick_profile_chip_${profile.id}")
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.cockpitVibrationHeader,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(VibrationMode.entries) { mode ->
                            val modeLabel = when (mode) {
                                VibrationMode.NONE -> strings.vibNoneTitle
                                VibrationMode.LIGHT -> strings.vibLightTitle
                                VibrationMode.STRONG -> strings.vibStrongTitle
                            }
                            FilterChip(
                                selected = vibrationMode == mode,
                                onClick = { onVibrationModeChange(mode) },
                                label = { Text(modeLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("cockpit_vibration_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            if (activeProfile != null) {
                item {
                    ActiveLayoutSchematicCard(
                        profile = activeProfile,
                        onOpenEditor = { onOpenEditor(activeProfile.id) },
                        onOpenSandbox = onOpenSandbox
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HeroCockpitCard(
    activeProfile: ControllerProfile?,
    isOverlayRunning: Boolean,
    onToggleOverlay: () -> Unit,
    onOpenEditor: () -> Unit
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isOverlayRunning) primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.large
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_cockpit),
                contentDescription = "XenosPad Virtual Cockpit",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x550B0E14),
                                Color(0xCC0B0E14),
                                Color(0xF50B0E14)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isOverlayRunning) primary.copy(alpha = 0.22f) else Color(0xAA121721),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(
                            1.dp,
                            if (isOverlayRunning) primary else Color(0xFF314057),
                            RoundedCornerShape(20.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOverlayRunning) primary else secondary)
                            )
                            Text(
                                text = if (isOverlayRunning) strings.cockpitOverlayLive else strings.cockpitArmed,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverlayRunning) primary else Color.White
                            )
                        }
                    }

                    activeProfile?.let {
                        Surface(
                            color = Color(0xAA1A2230),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = it.genre.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = secondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = activeProfile?.name ?: "XenosPad",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeProfile?.gameTitle ?: strings.profilesSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB6C4D9),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onToggleOverlay,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOverlayRunning) MacroCoral else primary,
                                contentColor = if (isOverlayRunning) Color.White else MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("hero_toggle_overlay_button")
                        ) {
                            Icon(
                                imageVector = if (isOverlayRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isOverlayRunning) strings.stopOverlay else strings.launchOverlay,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenEditor,
                            enabled = activeProfile != null,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("hero_edit_layout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(strings.commonCustomize)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadinessRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    statusText: String,
    isReady: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isReady) primary.copy(alpha = 0.14f) else TurboAmber.copy(alpha = 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isReady) primary else TurboAmber,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = if (isReady) primary.copy(alpha = 0.18f) else TurboAmber.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (isReady) primary else TurboAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isReady) primary else TurboAmber
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActiveLayoutSchematicCard(
    profile: ControllerProfile,
    onOpenEditor: () -> Unit,
    onOpenSandbox: () -> Unit
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val gamepadCount = profile.controls.count {
        it.binding.domain == BindingDomain.CONTROLLER || it.binding.domain == BindingDomain.ANALOG_AXIS
    }
    val mouseCount = profile.controls.count { it.binding.domain == BindingDomain.MOUSE }
    val keyboardCount = profile.controls.count { it.binding.domain == BindingDomain.KEYBOARD }
    val comboCount = profile.controls.count { it.binding.domain == BindingDomain.COMBO }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.cockpitActiveTopology,
                        style = MaterialTheme.typography.labelMedium,
                        color = primary
                    )
                    Text(
                        text = "${profile.controls.size} ${strings.cockpitMappedSuffix}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (gamepadCount > 0) StatBadge("${gamepadCount} PAD", primary)
                    if (mouseCount > 0) StatBadge("${mouseCount} MSE", TurboAmber)
                    if (keyboardCount > 0) StatBadge("${keyboardCount} KEY", TacticalCyan)
                    if (comboCount > 0) StatBadge("${comboCount} MACRO", MacroViolet)
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenEditor)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_game_arena_bg),
                    contentDescription = strings.cockpitActiveTopology,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x990B0E14))
                )
                ProfileMiniMapCanvas(
                    profile = profile,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color(0xCC0B0E14),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = strings.cockpitTapCustomize,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenEditor,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cockpit_open_studio_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.cockpitLayoutStudio)
                }
                Button(
                    onClick = onOpenSandbox,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cockpit_open_sandbox_button")
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.cockpitTestControls)
                }
            }
        }
    }
}

@Composable
fun StatBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.border(0.8.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ProfileMiniMapCanvas(
    profile: ControllerProfile,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        profile.controls.forEach { control ->
            val cx = control.xPercent * size.width
            val cy = control.yPercent * size.height
            val color = Color(control.accent.hex)
            val r = when (control.controlType) {
                ControlType.TOUCHPAD -> 18.dp.toPx()
                ControlType.ANALOG_STICK, ControlType.DPAD_CLUSTER, ControlType.WASD_CLUSTER -> 14.dp.toPx()
                ControlType.SHOULDER_PADDLE -> 10.dp.toPx()
                ControlType.BUTTON -> 7.5.dp.toPx()
            }
            drawCircle(
                color = color.copy(alpha = 0.25f),
                radius = r,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = color,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
    }
}

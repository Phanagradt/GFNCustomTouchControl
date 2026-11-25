package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.OverlaySettings
import com.example.model.BindingDomain
import com.example.model.ColorMatrixBuilder
import com.example.model.ControllerProfile
import com.example.model.LocalNexusStrings
import com.example.model.StickState
import com.example.model.TelemetryEvent
import com.example.ui.components.CrosshairOverlayCanvas
import com.example.ui.components.PositionedVirtualControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.MacroViolet
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber

@Composable
fun TestArenaScreen(
    activeProfile: ControllerProfile?,
    allProfiles: List<ControllerProfile>,
    settings: OverlaySettings,
    isOverlayRunning: Boolean,
    pressedIds: Set<String>,
    toggledIds: Set<String>,
    leftStick: StickState,
    rightStick: StickState,
    telemetry: List<TelemetryEvent>,
    onSelectProfile: (String) -> Unit,
    onEditProfile: (String) -> Unit,
    onToggleOverlay: () -> Unit,
    onClearTelemetry: () -> Unit,
    onPickCustomBg: (Uri) -> Unit,
    onResetCustomBg: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val bgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickCustomBg(uri)
    }

    val customBgBitmap by produceState<ImageBitmap?>(initialValue = null, settings.customTestBgPath) {
        value = if (settings.customTestBgPath.isNotBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val f = File(settings.customTestBgPath)
                    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
                }.getOrNull()
            }
        } else {
            null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top control bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.sandboxTitle,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = activeProfile?.let { "${it.name} • ${it.controls.size} ${strings.commonControlsSuffix}" }
                        ?: strings.sandboxNoProfile,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                activeProfile?.let { profile ->
                    OutlinedButton(
                        onClick = { onEditProfile(profile.id) },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sandbox_edit_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.commonEdit)
                    }
                }
                Button(
                    onClick = onToggleOverlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOverlayRunning) MacroCoral else primary,
                        contentColor = if (isOverlayRunning) Color.White else MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("sandbox_overlay_button")
                ) {
                    Icon(
                        imageVector = if (isOverlayRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isOverlayRunning) strings.commonStop else strings.commonOverlay)
                }
            }
        }

        // Quick profile switcher + Custom Background picker chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "custom_bg_chip") {
                AssistChip(
                    onClick = {
                        bgPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    label = { Text(strings.sandboxCustomBg) },
                    leadingIcon = {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("sandbox_pick_bg_button")
                )
            }
            if (settings.customTestBgPath.isNotBlank()) {
                item(key = "reset_bg_chip") {
                    AssistChip(
                        onClick = onResetCustomBg,
                        label = { Text(strings.sandboxResetBg) },
                        leadingIcon = {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("sandbox_reset_bg_button")
                    )
                }
            }
            items(allProfiles, key = { it.id }) { profile ->
                FilterChip(
                    selected = profile.id == activeProfile?.id,
                    onClick = { onSelectProfile(profile.id) },
                    label = { Text(profile.name) },
                    leadingIcon = {
                        Icon(Icons.Default.Gamepad, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }

        // Interactive simulated GeForce NOW stream viewport
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.35f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                .testTag("sandbox_viewport")
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            val activeFilterStyle = settings.gameFilter.activeStyle
            val viewportColorFilter = remember(activeFilterStyle) {
                activeFilterStyle?.let {
                    ColorFilter.colorMatrix(ColorMatrixBuilder.toComposeColorMatrix(it))
                }
            }

            if (customBgBitmap != null) {
                Image(
                    bitmap = customBgBitmap!!,
                    contentDescription = strings.sandboxCustomBg,
                    contentScale = ContentScale.Crop,
                    colorFilter = viewportColorFilter,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_game_arena_bg),
                    contentDescription = strings.sandboxTitle,
                    contentScale = ContentScale.Crop,
                    colorFilter = viewportColorFilter,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x550B0E14))
            )

            // Live Dual-Stick Crosshair Target Feedback
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val reticle = Offset(
                    x = (center.x + rightStick.x * size.width * 0.22f + leftStick.x * size.width * 0.08f)
                        .coerceIn(24f, size.width - 24f),
                    y = (center.y + rightStick.y * size.height * 0.22f + leftStick.y * size.height * 0.08f)
                        .coerceIn(24f, size.height - 24f)
                )
                drawCircle(
                    color = TacticalCyan.copy(alpha = 0.25f),
                    radius = 28.dp.toPx(),
                    center = reticle
                )
                drawCircle(
                    color = GeForceLime,
                    radius = 18.dp.toPx(),
                    center = reticle,
                    style = Stroke(width = 1.6.dp.toPx())
                )
                drawCircle(
                    color = GeForceLime,
                    radius = 3.dp.toPx(),
                    center = reticle
                )
            }

            CrosshairOverlayCanvas(settings = settings.crosshair)

            // Live stick readouts top bar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = Color(0xCC0B0E14),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "L: %+.2f, %+.2f".format(leftStick.x, leftStick.y),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = Color(0xCC0B0E14),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (activeFilterStyle != null) {
                            "${strings.menuFilter.uppercase()} S${activeFilterStyle.slot}"
                        } else {
                            strings.sandboxFilterOff
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = if (activeFilterStyle != null) primary else Color(0xFF9FB0C7),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = Color(0xCC0B0E14),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "R: %+.2f, %+.2f".format(rightStick.x, rightStick.y),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TacticalCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Render active virtual controls live
            activeProfile?.controls?.forEach { control ->
                PositionedVirtualControl(
                    control = control,
                    containerWidthPx = widthPx,
                    containerHeightPx = heightPx,
                    notation = settings.controllerNotation,
                    globalOpacity = settings.globalOpacity,
                    vibrationMode = settings.vibrationMode,
                    isEditMode = false,
                    isSelected = false,
                    isPressed = control.id in pressedIds,
                    isToggled = control.id in toggledIds,
                    onSelect = {},
                    onDragDelta = { _, _ -> }
                )
            }
        }

        // Telemetry log panel
        if (settings.showTelemetryOverlay) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(primary)
                            )
                            Text(
                                text = strings.sandboxTelemetryHeader,
                                style = MaterialTheme.typography.labelSmall,
                                color = primary
                            )
                        }
                        IconButton(
                            onClick = onClearTelemetry,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("sandbox_clear_telemetry_button")
                        ) {
                            Icon(
                                Icons.Default.CleaningServices,
                                contentDescription = strings.commonReset,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (telemetry.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.sandboxTelemetryEmpty,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(telemetry, key = { it.id }) { event ->
                                val tagColor = when (event.domain) {
                                    BindingDomain.CONTROLLER, BindingDomain.ANALOG_AXIS -> GeForceLime
                                    BindingDomain.MOUSE -> TurboAmber
                                    BindingDomain.KEYBOARD -> TacticalCyan
                                    BindingDomain.COMBO -> MacroViolet
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = event.codeLabel,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = tagColor
                                        )
                                        Text(
                                            text = event.actionName,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = if (event.actionName.contains("TURBO")) TurboAmber else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = event.detail,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = event.sourceLabel,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

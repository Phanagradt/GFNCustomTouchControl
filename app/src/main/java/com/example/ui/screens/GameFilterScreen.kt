package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ColorMatrixBuilder
import com.example.model.CrosshairPreset
import com.example.model.CrosshairSettings
import com.example.model.GameFilterState
import com.example.model.GameFilterStyle
import com.example.model.LocalNexusStrings
import com.example.ui.components.CrosshairOverlayCanvas
import com.example.ui.components.CrosshairPanelContent
import com.example.ui.components.GeForceFilterPanelContent
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TacticalCyan

@Composable
fun GameFilterScreen(
    filterState: GameFilterState,
    crosshair: CrosshairSettings,
    isOverlayRunning: Boolean,
    initialSection: Int = 0,
    onSelectSlot: (Int) -> Unit,
    onUpdateStyle: (GameFilterStyle) -> Unit,
    onResetSlot: () -> Unit,
    onUpdateCrosshair: (CrosshairSettings) -> Unit,
    onSelectCrosshairPreset: (CrosshairPreset) -> Unit,
    onResetCrosshair: () -> Unit,
    onPickCrosshairImage: (Uri) -> Unit,
    onToggleOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    var selectedSection by remember(initialSection) { mutableIntStateOf(initialSection.coerceIn(0, 1)) }
    LaunchedEffect(initialSection) {
        selectedSection = initialSection.coerceIn(0, 1)
    }

    var compareOriginal by remember { mutableStateOf(false) }
    val activeStyle = filterState.activeStyle
    val appliedColorFilter = remember(activeStyle, compareOriginal) {
        if (!compareOriginal && activeStyle != null) {
            ColorFilter.colorMatrix(ColorMatrixBuilder.toComposeColorMatrix(activeStyle))
        } else {
            null
        }
    }

    val crosshairImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickCrosshairImage(uri)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Segmented Switcher (Game Filter vs Crosshair)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedSection == 0) strings.visualsGameFilterTab else strings.crosshairTitle,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = if (selectedSection == 0) {
                                    "Customize Shadows, Brightness, Contrast & Color for your stream"
                                } else {
                                    "Adjust reticle shape, color, outline, gap, thickness & custom image"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onToggleOverlay,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOverlayRunning) MacroCoral else GeForceLime,
                                contentColor = if (isOverlayRunning) Color.White else Color(0xFF0A1400)
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("filters_overlay_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isOverlayRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isOverlayRunning) "Stop" else "Overlay")
                        }
                    }

                    // Mode Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple(0, strings.visualsGameFilterTab, Icons.Default.Tune),
                            Triple(1, strings.visualsCrosshairTab, Icons.Default.CenterFocusStrong)
                        ).forEach { (idx, label, icon) ->
                            val selected = selectedSection == idx
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (selected) GeForceLime else Color.Transparent)
                                    .clickable { selectedSection = idx }
                                    .testTag("visuals_section_tab_$idx")
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (selected) Color(0xFF091200) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    color = if (selected) Color(0xFF091200) else MaterialTheme.colorScheme.onSurface,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            if (selectedSection == 0) {
                // Live ColorMatrix Game Stream Preview
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.5.dp,
                                if (filterState.isEnabled && !compareOriginal) GeForceLime else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_game_arena_bg),
                            contentDescription = "Live Game Filter Preview",
                            contentScale = ContentScale.Crop,
                            colorFilter = appliedColorFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x66080B10),
                                            Color.Transparent,
                                            Color(0xAA080B10)
                                        )
                                    )
                                )
                        )

                        Surface(
                            color = Color(0xCC0A0E14),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .border(1.dp, Color(0xFF2B384A), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = when {
                                    compareOriginal -> "PREVIEW: ORIGINAL"
                                    activeStyle != null -> "ACTIVE: STYLE ${activeStyle.slot} • ${activeStyle.name.uppercase()}"
                                    else -> "FILTER: OFF"
                                },
                                color = if (activeStyle != null && !compareOriginal) GeForceLime else Color(0xFFB4C3D6),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { compareOriginal = !compareOriginal }
                                .padding(6.dp)
                                .testTag("filter_compare_button")
                        ) {
                            Surface(
                                color = if (compareOriginal) TacticalCyan else Color(0xCC0A0E14),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.border(
                                    1.dp,
                                    if (compareOriginal) TacticalCyan else Color(0xFF334257),
                                    RoundedCornerShape(8.dp)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Compare,
                                        contentDescription = "Compare Original",
                                        tint = if (compareOriginal) Color(0xFF001B21) else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (compareOriginal) "Original" else "Compare",
                                        color = if (compareOriginal) Color(0xFF001B21) else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = TacticalCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Live-synced with Floating Bubble → Left Filter menu in GeForce NOW",
                                color = Color(0xFFE3ECF7),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // GeForce Experience Freestyle Panel Replica
                item {
                    GeForceFilterPanelContent(
                        filterState = filterState,
                        onSelectSlot = onSelectSlot,
                        onUpdateStyle = onUpdateStyle,
                        onResetSlot = onResetSlot,
                        compactMode = false
                    )
                }
            } else {
                // Live Crosshair HUD Preview Box
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(184.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.5.dp,
                                if (crosshair.enabled) GeForceLime else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_game_arena_bg),
                            contentDescription = "Crosshair Live Preview",
                            contentScale = ContentScale.Crop,
                            colorFilter = appliedColorFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x55080B10))
                        )
                        CrosshairOverlayCanvas(settings = crosshair)
                        Surface(
                            color = Color(0xCC0A0E14),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .border(1.dp, Color(0xFF2B384A), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = if (crosshair.enabled) {
                                    "CROSSHAIR: ${crosshair.style.displayName.uppercase()}"
                                } else {
                                    "CROSSHAIR: OFF"
                                },
                                color = if (crosshair.enabled) GeForceLime else Color(0xFFB4C3D6),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                item {
                    CrosshairPanelContent(
                        crosshair = crosshair,
                        onUpdate = onUpdateCrosshair,
                        onSelectPreset = onSelectCrosshairPreset,
                        onReset = onResetCrosshair,
                        onUploadCustomImageClick = {
                            crosshairImagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        compactMode = false
                    )
                }
            }
        }
    }
}


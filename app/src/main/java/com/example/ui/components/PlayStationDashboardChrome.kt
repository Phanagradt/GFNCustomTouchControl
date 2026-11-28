package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.NexusRoutes
import com.example.data.VibrationMode
import com.example.model.AppPlatformTheme
import com.example.model.ControllerProfile
import com.example.model.GameFilterState
import com.example.model.GfnRegionPreset
import com.example.ui.screens.CommunityAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Ps5CanvasBg = Color(0xFF060A14)
val Ps5DeepBlueBg = Color(0xFF0A1429)
val Ps5GlassSurface = Color(0xE8101829)
val Ps5AccentBlue = Color(0xFF0070D1)
val Ps5IceHighlight = Color(0xFF66B8FF)
val Ps5GoldAccent = Color(0xFFE8C56E)
val Ps5MutedText = Color(0xFFA6B4CA)
val Ps5BetaAmber = Color(0xFFF5B731)

/**
 * Authentic PlayStation 5 Floating Top Header Bar matching the user's PS5 screenshot:
 * - Left: Bold white "Games" | muted "Media" tab switcher + subtle BETA pill
 * - Right: Search icon, Settings gear (with blue update dot), circular PSN Avatar, Clock (h:mm a)
 */
@Composable
fun Ps5TopHeaderBar(
    currentRoute: String?,
    displayName: String,
    avatarBase64: String?,
    hasUpdateBadge: Boolean,
    isPrimaryAdmin: Boolean,
    onSelectGames: () -> Unit,
    onSelectMedia: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAdminPanel: () -> Unit
) {
    val clockLabel = remember {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }
    val isMediaTab = currentRoute == NexusRoutes.GALLERY

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xE0050912), Color(0x88050912), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Games | Media + BETA pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Games",
                    color = if (!isMediaTab) Color.White else Ps5MutedText.copy(alpha = 0.7f),
                    fontWeight = if (!isMediaTab) FontWeight.SemiBold else FontWeight.Light,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onSelectGames)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("ps5_top_tab_games")
                )
                Text(
                    text = "Media",
                    color = if (isMediaTab) Color.White else Ps5MutedText.copy(alpha = 0.7f),
                    fontWeight = if (isMediaTab) FontWeight.SemiBold else FontWeight.Light,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onSelectMedia)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("ps5_top_tab_media")
                )
                Surface(
                    color = Ps5BetaAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(
                        1.dp,
                        Ps5BetaAmber.copy(alpha = 0.65f),
                        RoundedCornerShape(4.dp)
                    )
                ) {
                    Text(
                        text = "BETA",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 8.sp,
                        color = Ps5BetaAmber,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Right: Admin (if admin), Search, Settings, PSN Avatar, Clock
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isPrimaryAdmin) {
                    IconButton(
                        onClick = onOpenAdminPanel,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("topbar_admin_panel_button")
                    ) {
                        AdminRoleBadge(size = 22.dp)
                    }
                }
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("topbar_workshop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Workshop & Community",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Box {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("ps5_top_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    if (hasUpdateBadge) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 5.dp, end = 5.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Ps5IceHighlight)
                                .border(1.dp, Ps5CanvasBg, CircleShape)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                        .clickable(onClick = onOpenProfile)
                        .testTag("topbar_profile_button")
                ) {
                    CommunityAvatar(
                        avatarBase64 = avatarBase64,
                        username = displayName,
                        size = 26.dp,
                        borderColor = Ps5IceHighlight
                    )
                }
                Text(
                    text = clockLabel,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

/**
 * Sleek PlayStation 5 translucent bottom control bar:
 * [PS] CONTROL CENTER              (✕) PLAY   (△) HUD   (○) HOME
 */
@Composable
fun Ps5BottomControlBar(
    isHomeRoute: Boolean,
    isOverlayRunning: Boolean,
    onOpenControlCenter: () -> Unit,
    onPrimaryAction: () -> Unit,
    onToggleHud: () -> Unit,
    onBackOrHome: () -> Unit
) {
    Surface(
        color = Color(0xEB070C17),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF1F304D))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF14213A))
                        .border(1.dp, Ps5IceHighlight.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenControlCenter)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("ps5_footer_control_center_btn")
                ) {
                    Text(
                        text = "PS",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        text = "CONTROL CENTER",
                        color = Color(0xFFDCE8FA),
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Ps5SymbolPrompt(
                        symbol = "✕",
                        tint = Color(0xFF7BB5FF),
                        label = if (isHomeRoute) "PLAY" else "SELECT",
                        onClick = onPrimaryAction,
                        testTag = "ps5_footer_cross_btn"
                    )
                    Ps5SymbolPrompt(
                        symbol = "△",
                        tint = Color(0xFF4ADE80),
                        label = if (isOverlayRunning) "STOP HUD" else "HUD",
                        onClick = onToggleHud,
                        testTag = "ps5_footer_triangle_btn"
                    )
                    Ps5SymbolPrompt(
                        symbol = "○",
                        tint = Color(0xFFFF7A7A),
                        label = if (isHomeRoute) "MENU" else "HOME",
                        onClick = onBackOrHome,
                        testTag = "ps5_footer_circle_btn"
                    )
                }
            }
        }
    }
}

@Composable
private fun Ps5SymbolPrompt(
    symbol: String,
    tint: Color,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(19.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A253A))
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                color = tint,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
        Text(
            text = label,
            color = Color(0xFFE6EEF9),
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp
        )
    }
}

/**
 * Authentic PlayStation 5 Control Center Overlay:
 * Floating glass Activity Cards + horizontal circular PS5 icon bar at the bottom.
 */
@Composable
fun Ps5ControlCenterOverlay(
    currentRoute: String?,
    displayName: String,
    avatarBase64: String?,
    activeProfile: ControllerProfile?,
    allProfiles: List<ControllerProfile>,
    isOverlayRunning: Boolean,
    filterState: GameFilterState,
    selectedRegion: GfnRegionPreset,
    vibrationMode: VibrationMode,
    hasUpdateBadge: Boolean,
    isPrimaryAdmin: Boolean,
    onNavigateRoute: (String) -> Unit,
    onLaunchGeForceNow: () -> Unit,
    onLaunchXboxCloud: () -> Unit,
    onToggleOverlay: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onOpenEditor: (String) -> Unit,
    onSelectRegion: (GfnRegionPreset) -> Unit,
    onVibrationModeChange: (VibrationMode) -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSwitchTheme: (AppPlatformTheme) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB803060D))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("ps5_control_center_overlay")
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Activity Cards row (authentic PS5 floating glass cards)
            Surface(
                color = Ps5GlassSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2B426B), RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CommunityAvatar(
                                avatarBase64 = avatarBase64,
                                username = displayName,
                                size = 32.dp,
                                borderColor = Ps5IceHighlight
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Control Center",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                    Surface(
                                        color = Ps5BetaAmber.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "BETA",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 8.sp,
                                            color = Ps5BetaAmber,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "$displayName · ${activeProfile?.name ?: "No active layout"}",
                                    color = Ps5MutedText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close Control Center", tint = Color.White)
                        }
                    }

                    // Primary Quick Action Cards (Cloud Play, Xbox Cloud, Overlay HUD)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Ps5ControlCard(
                            icon = Icons.Default.CloudQueue,
                            title = "GeForce NOW",
                            subtitle = "Region ${selectedRegion.shortCode}",
                            accent = Ps5IceHighlight,
                            onClick = {
                                onDismiss()
                                onLaunchGeForceNow()
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "ps5_cc_gfn"
                        )
                        Ps5ControlCard(
                            icon = Icons.Default.SportsEsports,
                            title = "Xbox Cloud",
                            subtitle = "xbox.com/play",
                            accent = Color(0xFF4ADE80),
                            onClick = {
                                onDismiss()
                                onLaunchXboxCloud()
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "ps5_cc_xcloud"
                        )
                        Ps5ControlCard(
                            icon = Icons.Default.Layers,
                            title = if (isOverlayRunning) "Stop HUD" else "Start HUD",
                            subtitle = if (isOverlayRunning) "Running" else "Ready",
                            accent = if (isOverlayRunning) Color(0xFFFF7A7A) else Ps5GoldAccent,
                            onClick = {
                                onToggleOverlay()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "ps5_cc_overlay"
                        )
                    }

                    // Quick Region Bypass Strip
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CLOUD REGION BYPASS",
                            color = Ps5MutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(GfnRegionPreset.entries.take(6)) { region ->
                                val isSel = region == selectedRegion
                                Surface(
                                    color = if (isSel) Ps5AccentBlue else Color(0xFF18243A),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .border(
                                            1.dp,
                                            if (isSel) Color.White else Color(0xFF2D4164),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onSelectRegion(region) }
                                ) {
                                    Text(
                                        text = if (region == GfnRegionPreset.OFF) "Off" else region.shortCode,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Active Profile Switcher
                    if (allProfiles.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CONTROLLER LAYOUTS",
                                    color = Ps5MutedText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                if (activeProfile != null) {
                                    Text(
                                        text = "Edit Active",
                                        color = Ps5IceHighlight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                onDismiss()
                                                onOpenEditor(activeProfile.id)
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allProfiles, key = { it.id }) { profile ->
                                    val isSel = profile.id == activeProfile?.id
                                    Surface(
                                        color = if (isSel) Ps5AccentBlue.copy(alpha = 0.45f) else Color(0xFF172236),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .border(
                                                1.dp,
                                                if (isSel) Color.White else Color(0xFF2C3F60),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onSelectProfile(profile.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Gamepad,
                                                contentDescription = null,
                                                tint = if (isSel) Color.White else Ps5MutedText,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = profile.name,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Haptic quick toggle + Visual Filter status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF172236),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val entries = VibrationMode.entries
                                    val next = entries[(entries.indexOf(vibrationMode) + 1) % entries.size]
                                    onVibrationModeChange(next)
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Haptics", color = Ps5MutedText, fontSize = 10.sp)
                                Text(vibrationMode.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                        Surface(
                            color = Color(0xFF172236),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onDismiss()
                                    onNavigateRoute(NexusRoutes.FILTERS)
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Visual Filters", color = Ps5MutedText, fontSize = 10.sp)
                                Text(
                                    text = if (filterState.activeSlot > 0) "Slot ${filterState.activeSlot} ON" else "Off",
                                    color = if (filterState.activeSlot > 0) Ps5IceHighlight else Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Floating Circular PS5 Dock Icons
            Surface(
                color = Ps5GlassSurface,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2B426B), RoundedCornerShape(32.dp))
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Ps5DockIcon(
                            icon = Icons.Default.Home,
                            label = "Home",
                            selected = currentRoute == NexusRoutes.COCKPIT,
                            onClick = {
                                onNavigateRoute(NexusRoutes.COCKPIT)
                                onDismiss()
                            },
                            testTag = "ps5_dock_home"
                        )
                    }
                    item {
                        Ps5DockIcon(
                            icon = Icons.Default.FolderCopy,
                            label = "Switcher",
                            selected = currentRoute == NexusRoutes.PROFILES,
                            onClick = {
                                onNavigateRoute(NexusRoutes.PROFILES)
                                onDismiss()
                            },
                            testTag = "ps5_dock_profiles"
                        )
                    }
                    item {
                        Ps5DockIcon(
                            icon = Icons.Default.Palette,
                            label = "Workshop",
                            selected = false,
                            onClick = {
                                onDismiss()
                                onOpenWorkshop()
                            },
                            testTag = "ps5_dock_workshop"
                        )
                    }
                    item {
                        Ps5DockIcon(
                            icon = Icons.Default.Tune,
                            label = "Visuals",
                            selected = currentRoute == NexusRoutes.FILTERS,
                            onClick = {
                                onNavigateRoute(NexusRoutes.FILTERS)
                                onDismiss()
                            },
                            testTag = "ps5_dock_filters"
                        )
                    }
                    item {
                        Ps5DockIcon(
                            icon = Icons.Default.PhotoLibrary,
                            label = "Media",
                            selected = currentRoute == NexusRoutes.GALLERY,
                            onClick = {
                                onNavigateRoute(NexusRoutes.GALLERY)
                                onDismiss()
                            },
                            testTag = "ps5_dock_gallery"
                        )
                    }
                    item {
                        Ps5DockIcon(
                            icon = Icons.Default.TouchApp,
                            label = "Sandbox",
                            selected = currentRoute == NexusRoutes.SANDBOX,
                            onClick = {
                                onNavigateRoute(NexusRoutes.SANDBOX)
                                onDismiss()
                            },
                            testTag = "ps5_dock_sandbox"
                        )
                    }
                    item {
                        Ps5DockIcon(
                            icon = if (hasUpdateBadge) Icons.Default.Download else Icons.Default.Settings,
                            label = if (hasUpdateBadge) "Update" else "Settings",
                            selected = currentRoute == NexusRoutes.SETUP,
                            accent = if (hasUpdateBadge) Ps5IceHighlight else null,
                            onClick = {
                                onNavigateRoute(NexusRoutes.SETUP)
                                onDismiss()
                            },
                            testTag = "ps5_dock_settings"
                        )
                    }
                    item {
                        Ps5DockIcon(
                            icon = Icons.Default.Edit,
                            label = "Profile",
                            selected = false,
                            onClick = {
                                onDismiss()
                                onOpenProfile()
                            },
                            testTag = "ps5_dock_profile"
                        )
                    }
                    if (isPrimaryAdmin) {
                        item {
                            Ps5DockIcon(
                                icon = Icons.Default.SportsEsports,
                                label = "Admin",
                                selected = false,
                                accent = Ps5BetaAmber,
                                onClick = {
                                    onDismiss()
                                    onOpenAdminPanel()
                                },
                                testTag = "ps5_dock_admin"
                            )
                        }
                    }
                    item {
                        Ps5DockIcon(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            label = "Default",
                            selected = false,
                            accent = Color(0xFFFF8080),
                            onClick = {
                                onDismiss()
                                onSwitchTheme(AppPlatformTheme.XENOS_DEFAULT)
                            },
                            testTag = "ps5_dock_exit"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Ps5ControlCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF162238),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Ps5MutedText,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Ps5DockIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    accent: Color? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else Color(0xFF1A2842))
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = accent ?: if (selected) Color.White else Color(0xFF314973),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    selected -> Ps5CanvasBg
                    accent != null -> accent
                    else -> Color.White
                },
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = label,
            color = if (selected) Color.White else Ps5MutedText,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

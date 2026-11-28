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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.JetBrainsMonoFontFamily

private val SteamDrawerBg = Color(0xF5151921)
private val SteamDrawerCard = Color(0xFF1E2430)
private val SteamAccentBlue = Color(0xFF1A9FFF)
private val SteamSky = Color(0xFF66C0F4)
private val SteamGreen = Color(0xFF59BF40)
private val SteamBetaAmber = Color(0xFFF5B731)

/**
 * Authentic SteamOS / Steam Big Picture Mode Left Slide-Out Menu ("STEAM MENU").
 */
@Composable
fun SteamBigPictureLeftMenuOverlay(
    currentRoute: String?,
    hasUpdateBadge: Boolean,
    isPrimaryAdmin: Boolean,
    onNavigateRoute: (String) -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onExitBigPicture: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA605070B))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("steam_left_menu_overlay")
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .widthIn(min = 250.dp, max = 300.dp)
                .background(SteamDrawerBg)
                .border(width = 1.dp, color = Color(0xFF2B3545))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "STEAM",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF0E141B),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = SteamBetaAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(1.dp, SteamBetaAmber.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "BETA",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                            color = SteamBetaAmber,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Menu", tint = Color(0xFF9FB0C7))
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SteamMenuRow(
                    icon = Icons.Default.Dashboard,
                    label = "Home",
                    selected = currentRoute == NexusRoutes.COCKPIT,
                    onClick = {
                        onNavigateRoute(NexusRoutes.COCKPIT)
                        onDismiss()
                    },
                    testTag = "steam_menu_home"
                )
                SteamMenuRow(
                    icon = Icons.Default.Storefront,
                    label = "Steam Store",
                    badgeText = "LIVE",
                    badgeColor = SteamAccentBlue,
                    selected = currentRoute == NexusRoutes.STORE,
                    onClick = {
                        onNavigateRoute(NexusRoutes.STORE)
                        onDismiss()
                    },
                    testTag = "steam_menu_store"
                )
                SteamMenuRow(
                    icon = Icons.Default.FolderCopy,
                    label = "Library & Layouts",
                    selected = currentRoute == NexusRoutes.PROFILES,
                    onClick = {
                        onNavigateRoute(NexusRoutes.PROFILES)
                        onDismiss()
                    },
                    testTag = "steam_menu_profiles"
                )
                SteamMenuRow(
                    icon = Icons.Default.Tune,
                    label = "Filters & Crosshair",
                    selected = currentRoute == NexusRoutes.FILTERS,
                    onClick = {
                        onNavigateRoute(NexusRoutes.FILTERS)
                        onDismiss()
                    },
                    testTag = "steam_menu_filters"
                )
                SteamMenuRow(
                    icon = Icons.Default.PhotoLibrary,
                    label = "Media & Clips",
                    selected = currentRoute == NexusRoutes.GALLERY,
                    onClick = {
                        onNavigateRoute(NexusRoutes.GALLERY)
                        onDismiss()
                    },
                    testTag = "steam_menu_gallery"
                )
                SteamMenuRow(
                    icon = Icons.Default.TouchApp,
                    label = "Controller Sandbox",
                    selected = currentRoute == NexusRoutes.SANDBOX,
                    onClick = {
                        onNavigateRoute(NexusRoutes.SANDBOX)
                        onDismiss()
                    },
                    testTag = "steam_menu_sandbox"
                )

                HorizontalDivider(
                    color = Color(0xFF283140),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                SteamMenuRow(
                    icon = Icons.Default.Palette,
                    label = "Theme Workshop",
                    selected = false,
                    onClick = {
                        onDismiss()
                        onOpenWorkshop()
                    },
                    testTag = "steam_menu_workshop"
                )
                SteamMenuRow(
                    icon = Icons.Default.Person,
                    label = "Steam Profile",
                    selected = false,
                    onClick = {
                        onDismiss()
                        onOpenProfile()
                    },
                    testTag = "steam_menu_profile"
                )
                SteamMenuRow(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    badgeText = if (hasUpdateBadge) "UPDATE" else null,
                    badgeColor = SteamGreen,
                    selected = currentRoute == NexusRoutes.SETUP,
                    onClick = {
                        onNavigateRoute(NexusRoutes.SETUP)
                        onDismiss()
                    },
                    testTag = "steam_menu_settings"
                )

                if (isPrimaryAdmin) {
                    SteamMenuRow(
                        icon = Icons.Default.SportsEsports,
                        label = "Admin Panel",
                        badgeText = "ADMIN",
                        badgeColor = SteamBetaAmber,
                        selected = false,
                        onClick = {
                            onDismiss()
                            onOpenAdminPanel()
                        },
                        testTag = "steam_menu_admin"
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFF283140),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SteamMenuRow(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = "Exit Big Picture Mode",
                selected = false,
                tintOverride = Color(0xFFE08080),
                onClick = {
                    onDismiss()
                    onExitBigPicture()
                },
                testTag = "steam_menu_exit_big_picture"
            )
        }
    }
}

@Composable
private fun SteamMenuRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    badgeText: String? = null,
    badgeColor: Color = SteamAccentBlue,
    tintOverride: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFF263347) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(22.dp)
                .background(if (selected) SteamAccentBlue else Color.Transparent)
        )
        Spacer(Modifier.width(13.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintOverride ?: if (selected) Color.White else Color(0xFF9FB0C7),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = tintOverride ?: if (selected) Color.White else Color(0xFFD5DFEB),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!badgeText.isNullOrBlank()) {
            Surface(
                color = badgeColor.copy(alpha = 0.22f),
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier.padding(end = 14.dp)
            ) {
                Text(
                    text = badgeText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Authentic Steam Deck Right Slide-Out Quick Access Menu ("... OPTIONS").
 * Provides live one-tap control over XenosPad Overlay, GeForce NOW launch,
 * Active Controller Layout, Server Region, Vibration, Filters, and Theme.
 */
@Composable
fun SteamQuickAccessMenuOverlay(
    activeProfile: ControllerProfile?,
    allProfiles: List<ControllerProfile>,
    isOverlayRunning: Boolean,
    filterState: GameFilterState,
    selectedRegion: GfnRegionPreset,
    vibrationMode: VibrationMode,
    onToggleOverlay: () -> Unit,
    onLaunchWebPlayer: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onSelectRegion: (GfnRegionPreset) -> Unit,
    onVibrationModeChange: (VibrationMode) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchTheme: (AppPlatformTheme) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA605070B))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("steam_qam_overlay")
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .widthIn(min = 270.dp, max = 330.dp)
                .background(SteamDrawerBg)
                .border(width = 1.dp, color = Color(0xFF2B3545))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color.White,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "···",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = "QUICK ACCESS",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Quick Access", tint = Color(0xFF9FB0C7))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Play GeForce NOW button
                Button(
                    onClick = {
                        onDismiss()
                        onLaunchWebPlayer()
                    },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SteamGreen),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("steam_qam_launch_gfn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("LAUNCH GEFORCE NOW", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                // Virtual Overlay HUD Toggle
                Surface(
                    color = SteamDrawerCard,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Gamepad, contentDescription = null, tint = SteamSky, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Overlay Controller HUD", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    text = if (isOverlayRunning) "Running on screen" else "Tap to start overlay",
                                    color = if (isOverlayRunning) SteamGreen else Color(0xFF8F98A0),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = isOverlayRunning,
                            onCheckedChange = { onToggleOverlay() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SteamAccentBlue
                            )
                        )
                    }
                }

                // Active Profile Quick Switcher
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CONTROLLER LAYOUT",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SteamSky
                    )
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
                                }
                            )
                        }
                    }
                }

                // GeForce NOW Region Quick Picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "GEFORCE NOW REGION",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SteamSky
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GfnRegionPreset.entries.take(6)) { region ->
                            FilterChip(
                                selected = region == selectedRegion,
                                onClick = { onSelectRegion(region) },
                                label = { Text(region.shortCode, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Haptics / Vibration
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = SteamSky, modifier = Modifier.size(14.dp))
                        Text(
                            text = "HAPTIC FEEDBACK",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SteamSky
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(VibrationMode.entries) { mode ->
                            FilterChip(
                                selected = mode == vibrationMode,
                                onClick = { onVibrationModeChange(mode) },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Visual Filter Shortcut
                Surface(
                    color = SteamDrawerCard,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onOpenFilters()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Game Filters & Crosshair", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = if (filterState.isEnabled) "Slot ${filterState.activeSlot} active" else "Filters off — tap to customize",
                                color = Color(0xFF9FB0C7),
                                fontSize = 11.sp
                            )
                        }
                        Icon(Icons.Default.Tune, contentDescription = null, tint = SteamSky, modifier = Modifier.size(18.dp))
                    }
                }

                // Theme Beta Quick Switcher
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "APP THEME",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SteamSky
                        )
                        Surface(color = SteamBetaAmber.copy(alpha = 0.22f), shape = RoundedCornerShape(3.dp)) {
                            Text(
                                text = "BETA",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SteamBetaAmber,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AppPlatformTheme.entries) { theme ->
                            FilterChip(
                                selected = theme == AppPlatformTheme.STEAM_DECK,
                                onClick = {
                                    onSwitchTheme(theme)
                                    if (theme != AppPlatformTheme.STEAM_DECK) onDismiss()
                                },
                                label = { Text(theme.shortName, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open All Settings", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ButtonThemePreset
import com.example.model.CommunityTopicPost
import com.example.model.ControllerProfile
import com.example.model.GameFilterState
import com.example.model.GfnRegionPreset
import com.example.model.PinnedGame
import com.example.model.WebStreamQualityConfig
import com.example.ui.SystemReadiness
import com.example.ui.components.NintendoJoyConMark
import com.example.ui.components.SwitchThemePalette
import com.example.ui.components.switchPaletteFor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.update.UpdateUiState
import java.util.UUID

private data class SwitchSoftwareTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String?,
    val gradient: List<Color>,
    val icon: ImageVector,
    val useHeroArtwork: Boolean = false,
    val isEmptySlot: Boolean = false,
    val onLaunch: () -> Unit
)

private data class SwitchDockApplet(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val isRedOnlinePill: Boolean = false,
    val hasBadgeDot: Boolean = false,
    val onLaunch: () -> Unit
)

private val SWITCH_PINNED_GRADIENTS = listOf(
    listOf(Color(0xFF0EA5E9), Color(0xFF1D4ED8)),
    listOf(Color(0xFFEF4444), Color(0xFF991B1B)),
    listOf(Color(0xFF10B981), Color(0xFF047857)),
    listOf(Color(0xFFF59E0B), Color(0xFFB45309)),
    listOf(Color(0xFF8B5CF6), Color(0xFF5B21B6))
)

/**
 * Faithful Nintendo Switch Home Menu replica matching the reference screenshot:
 * - Supports both Basic White (#EBEBEB) and Basic Black (#282828)
 * - Cyan focused software title floating above a horizontal carousel of crisp 1:1 square software tiles
 * - Clean empty-slot tiles at the end of the row (matching the Switch Home grid)
 * - Signature centered white/charcoal pill Applet Dock with Nintendo Switch Online red circle,
 *   colored system icons, cyan focus ring, and blue/cyan active applet label underneath
 */
@Composable
fun NintendoSwitchHomeScreen(
    isDarkMode: Boolean,
    activeProfile: ControllerProfile?,
    allProfiles: List<ControllerProfile>,
    readiness: SystemReadiness,
    isOverlayRunning: Boolean,
    webPlayerConfig: WebStreamQualityConfig,
    pinnedGames: List<PinnedGame>,
    filterState: GameFilterState,
    updateState: UpdateUiState,
    workshopThemes: List<ButtonThemePreset>,
    communityPosts: List<CommunityTopicPost>,
    switchSelectTrigger: Int,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onLaunchGeForceNow: () -> Unit,
    onLaunchXboxCloud: () -> Unit,
    onLaunchPinnedGame: (PinnedGame) -> Unit,
    onSavePinnedGame: (PinnedGame) -> Unit,
    onToggleOverlay: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenPublicProfile: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSandbox: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuickMenu: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = remember(isDarkMode) { switchPaletteFor(isDarkMode) }
    var focusedTileIndex by remember { mutableIntStateOf(0) }
    var focusedDockIndex by remember { mutableIntStateOf(0) }
    var focusInDock by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    val softwareTiles = remember(
        webPlayerConfig.regionPreset,
        isOverlayRunning,
        activeProfile?.id,
        activeProfile?.name,
        pinnedGames,
        filterState.activeSlot,
        readiness.canDrawOverlays
    ) {
        buildList {
            add(
                SwitchSoftwareTile(
                    id = "sw_gfn",
                    title = "GeForce NOW Cloud Play",
                    subtitle = "Region ${webPlayerConfig.regionPreset.shortCode} · Bypass VPN & Filters",
                    badge = "CLOUD",
                    gradient = listOf(Color(0xFFE60012), Color(0xFF99000C)),
                    icon = Icons.Default.CloudQueue,
                    useHeroArtwork = true,
                    onLaunch = onLaunchGeForceNow
                )
            )
            add(
                SwitchSoftwareTile(
                    id = "sw_xcloud",
                    title = "Xbox Cloud Gaming",
                    subtitle = "xbox.com/play · Region ${webPlayerConfig.regionPreset.shortCode}",
                    badge = "XCLOUD",
                    gradient = listOf(Color(0xFF107C10), Color(0xFF0A4D0A)),
                    icon = Icons.Default.SportsEsports,
                    onLaunch = onLaunchXboxCloud
                )
            )
            add(
                SwitchSoftwareTile(
                    id = "sw_overlay",
                    title = if (isOverlayRunning) "Touch Overlay (Live)" else "Touch Overlay HUD",
                    subtitle = activeProfile?.name ?: "Floating gamepad",
                    badge = if (isOverlayRunning) "LIVE" else "HUD",
                    gradient = listOf(Color(0xFF0AB9E6), Color(0xFF0369A1)),
                    icon = if (isOverlayRunning) Icons.Default.Stop else Icons.Default.Layers,
                    onLaunch = {
                        if (!readiness.canDrawOverlays && !isOverlayRunning) {
                            onRequestOverlayPermission()
                        } else {
                            onToggleOverlay()
                        }
                    }
                )
            )
            pinnedGames.forEachIndexed { idx, pinned ->
                val grad = SWITCH_PINNED_GRADIENTS[idx % SWITCH_PINNED_GRADIENTS.size]
                add(
                    SwitchSoftwareTile(
                        id = "sw_pin_${pinned.id}",
                        title = pinned.title,
                        subtitle = "Pinned · ${pinned.regionPreset.shortCode}",
                        badge = "GAME",
                        gradient = grad,
                        icon = Icons.Default.SportsEsports,
                        onLaunch = { onLaunchPinnedGame(pinned) }
                    )
                )
            }
            add(
                SwitchSoftwareTile(
                    id = "sw_studio",
                    title = "Layout Studio",
                    subtitle = activeProfile?.name ?: "Customize buttons",
                    badge = "EDIT",
                    gradient = listOf(Color(0xFF6366F1), Color(0xFF3730A3)),
                    icon = Icons.Default.Edit,
                    onLaunch = { activeProfile?.let { onOpenEditor(it.id) } ?: onOpenProfiles() }
                )
            )
            // Classic clean Nintendo Switch empty/pin software slots so the row matches the iconic screenshot
            add(
                SwitchSoftwareTile(
                    id = "sw_empty_pin",
                    title = "Pin a Cloud Game",
                    subtitle = "Add a quick-launch software tile",
                    badge = null,
                    gradient = emptyList(),
                    icon = Icons.Default.Add,
                    isEmptySlot = true,
                    onLaunch = { showPinDialog = true }
                )
            )
            if (pinnedGames.isEmpty()) {
                add(
                    SwitchSoftwareTile(
                        id = "sw_empty_slot_2",
                        title = "Pin Another Title",
                        subtitle = "Tap to add to Switch Home",
                        badge = null,
                        gradient = emptyList(),
                        icon = Icons.Default.Add,
                        isEmptySlot = true,
                        onLaunch = { showPinDialog = true }
                    )
                )
            }
        }
    }

    val hasUpdate = updateState is UpdateUiState.UpdateAvailable || updateState is UpdateUiState.ReadyToInstall
    val dockApplets = remember(hasUpdate, filterState.activeSlot, isDarkMode, updateState) {
        listOf(
            SwitchDockApplet(
                id = "dock_online",
                label = "GeForce NOW Online",
                icon = Icons.Default.CloudQueue,
                tint = Color(0xFFE60012),
                isRedOnlinePill = true,
                onLaunch = onLaunchGeForceNow
            ),
            SwitchDockApplet(
                id = "dock_workshop",
                label = "Community Workshop",
                icon = Icons.Default.Palette,
                tint = Color(0xFFF59E0B),
                onLaunch = onOpenWorkshop
            ),
            SwitchDockApplet(
                id = "dock_news",
                label = if (hasUpdate) "System Update Ready" else "News & Feed",
                icon = Icons.Default.Newspaper,
                tint = Color(0xFF22C55E),
                hasBadgeDot = hasUpdate,
                onLaunch = {
                    when (updateState) {
                        is UpdateUiState.ReadyToInstall -> onInstallUpdate()
                        is UpdateUiState.UpdateAvailable -> onDownloadUpdate()
                        else -> onOpenWorkshop()
                    }
                }
            ),
            SwitchDockApplet(
                id = "dock_xcloud",
                label = "Xbox Cloud Gaming",
                icon = Icons.Default.ShoppingBag,
                tint = Color(0xFFFF6B35),
                onLaunch = onLaunchXboxCloud
            ),
            SwitchDockApplet(
                id = "dock_album",
                label = "Album · Captures",
                icon = Icons.Default.PhotoLibrary,
                tint = Color(0xFF2563EB),
                onLaunch = onOpenGallery
            ),
            SwitchDockApplet(
                id = "dock_visuals",
                label = if (filterState.activeSlot > 0) "Visual FX (Slot ${filterState.activeSlot})" else "Visuals & Filters",
                icon = Icons.Default.Tune,
                tint = Color(0xFF06B6D4),
                onLaunch = onOpenFilters
            ),
            SwitchDockApplet(
                id = "dock_controllers",
                label = "Controllers & Profiles",
                icon = Icons.Default.Gamepad,
                tint = if (isDarkMode) Color(0xFFD0D0D0) else Color(0xFF5A5A5A),
                onLaunch = onOpenProfiles
            ),
            SwitchDockApplet(
                id = "dock_sandbox",
                label = "Input Sandbox",
                icon = Icons.Default.TouchApp,
                tint = if (isDarkMode) Color(0xFFD0D0D0) else Color(0xFF5A5A5A),
                onLaunch = onOpenSandbox
            ),
            SwitchDockApplet(
                id = "dock_settings",
                label = "System Settings",
                icon = Icons.Default.Settings,
                tint = if (isDarkMode) Color(0xFFD0D0D0) else Color(0xFF5A5A5A),
                hasBadgeDot = hasUpdate,
                onLaunch = onOpenSettings
            ),
            SwitchDockApplet(
                id = "dock_power",
                label = "Quick Menu · Light/Dark",
                icon = Icons.Default.PowerSettingsNew,
                tint = if (isDarkMode) Color(0xFFD0D0D0) else Color(0xFF5A5A5A),
                onLaunch = onOpenQuickMenu
            )
        )
    }

    val safeTileIdx = focusedTileIndex.coerceIn(0, (softwareTiles.size - 1).coerceAtLeast(0))
    val safeDockIdx = focusedDockIndex.coerceIn(0, (dockApplets.size - 1).coerceAtLeast(0))
    val focusedTile = softwareTiles[safeTileIdx]
    val focusedApplet = dockApplets[safeDockIdx]

    LaunchedEffect(switchSelectTrigger) {
        if (switchSelectTrigger > 0) {
            if (focusInDock) {
                focusedApplet.onLaunch()
            } else {
                focusedTile.onLaunch()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvasBg)
            .testTag("nintendo_switch_home_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Floating Focused Software Title + Horizontal 1:1 Square Tile Strip (exact Switch silhouette)
        item(key = "switch_software_carousel") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Floating cyan title above the software tiles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = focusedTile.title,
                            color = palette.focusCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = focusedTile.subtitle,
                            color = palette.secondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = palette.focusCyan,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .clickable(onClick = focusedTile.onLaunch)
                            .testTag("switch_launch_focused_btn")
                    ) {
                        Text(
                            text = if (focusedTile.isEmptySlot) "+ Pin" else "Start",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val tileSize: Dp = if (maxWidth >= 620.dp) 184.dp else 154.dp
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(softwareTiles, key = { _, tile -> tile.id }) { idx, tile ->
                            val isFocused = !focusInDock && idx == safeTileIdx
                            SwitchSquareSoftwareTile(
                                tile = tile,
                                tileSize = tileSize,
                                focused = isFocused,
                                palette = palette,
                                onClick = {
                                    focusInDock = false
                                    if (isFocused) {
                                        tile.onLaunch()
                                    } else {
                                        focusedTileIndex = idx
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Signature Centered Floating Pill Applet Dock (exact match to screenshot!)
        item(key = "switch_applet_pill_dock") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = palette.dockSurface,
                    shape = RoundedCornerShape(50),
                    shadowElevation = if (isDarkMode) 2.dp else 4.dp,
                    modifier = Modifier
                        .border(1.dp, palette.dividerColor.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .testTag("switch_applet_dock")
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(dockApplets, key = { _, a -> a.id }) { idx, applet ->
                            val isSelected = idx == safeDockIdx
                            SwitchDockAppletButton(
                                applet = applet,
                                selected = isSelected,
                                palette = palette,
                                onClick = {
                                    focusInDock = true
                                    if (isSelected) {
                                        applet.onLaunch()
                                    } else {
                                        focusedDockIndex = idx
                                        applet.onLaunch()
                                    }
                                }
                            )
                        }
                    }
                }

                // Active Applet Label underneath the dock (matching "Nintendo Switch Online" in screenshot)
                Text(
                    text = focusedApplet.label,
                    color = palette.labelBlue,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("switch_dock_active_label")
                )
            }
        }

        // 3. Compact Update Banner if an update is available
        if (updateState is UpdateUiState.UpdateAvailable || updateState is UpdateUiState.ReadyToInstall) {
            item(key = "switch_update_banner") {
                val isReady = updateState is UpdateUiState.ReadyToInstall
                Surface(
                    color = palette.tileSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .border(1.5.dp, palette.focusCyan, RoundedCornerShape(10.dp))
                        .clickable { if (isReady) onInstallUpdate() else onDownloadUpdate() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = palette.focusCyan)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isReady) "System Update Ready to Install" else "XenosPad Update Available",
                                color = palette.primaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isReady) "Tap to install update now" else "Tap to download latest version",
                                color = palette.secondaryText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. Below-the-fold: Controller Layouts strip
        if (allProfiles.isNotEmpty()) {
            item(key = "switch_profiles_strip") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Controllers & Touch Layouts",
                            color = palette.primaryText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Manage",
                            color = palette.labelBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onOpenProfiles)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(allProfiles, key = { _, p -> p.id }) { _, profile ->
                            val isActive = profile.id == activeProfile?.id
                            Surface(
                                color = palette.tileSurface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .width(196.dp)
                                    .border(
                                        width = if (isActive) 2.dp else 1.dp,
                                        color = if (isActive) palette.focusCyan else palette.dividerColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectProfile(profile.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isActive) palette.focusCyan else palette.canvasBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Gamepad,
                                            contentDescription = null,
                                            tint = if (isActive) Color.White else palette.secondaryText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.name,
                                            color = palette.primaryText,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isActive) "Active · ${profile.controls.size} inputs" else "${profile.controls.size} inputs",
                                            color = if (isActive) palette.focusCyan else palette.secondaryText,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Below-the-fold: Nintendo News Channel / Live Community Posts
        if (communityPosts.isNotEmpty()) {
            item(key = "switch_news_feed") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "News & Community Workshop",
                            color = palette.primaryText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${workshopThemes.size} Themes",
                            color = palette.labelBlue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onOpenWorkshop)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(communityPosts.take(6), key = { _, post -> post.id }) { _, post ->
                            Surface(
                                color = palette.tileSurface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .width(232.dp)
                                    .border(1.dp, palette.dividerColor, RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (post.ownerId.isNotBlank()) onOpenPublicProfile(post.ownerId) else onOpenWorkshop()
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Forum, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                                        Text(
                                            text = post.authorName.ifBlank { "Player" },
                                            color = palette.primaryText,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Text(
                                        text = post.title.ifBlank { post.body },
                                        color = palette.secondaryText,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        SwitchPinGameDialog(
            profiles = allProfiles,
            defaultProfileId = activeProfile?.id ?: allProfiles.firstOrNull()?.id.orEmpty(),
            defaultRegion = webPlayerConfig.regionPreset,
            onDismiss = { showPinDialog = false },
            onSave = { pinned ->
                onSavePinnedGame(pinned)
                showPinDialog = false
            }
        )
    }
}

@Composable
private fun SwitchSquareSoftwareTile(
    tile: SwitchSoftwareTile,
    tileSize: Dp,
    focused: Boolean,
    palette: SwitchThemePalette,
    onClick: () -> Unit
) {
    val borderWidth by animateDpAsState(
        targetValue = if (focused) 3.5.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switchTileBorder"
    )
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier
            .size(tileSize)
            .shadow(if (focused) 6.dp else 1.dp, shape)
            .clip(shape)
            .background(
                if (tile.isEmptySlot) {
                    palette.emptyTileSurface
                } else {
                    palette.tileSurface
                }
            )
            .border(
                width = borderWidth,
                color = if (focused) palette.focusCyan else palette.dividerColor.copy(alpha = 0.7f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .testTag("switch_tile_${tile.id}")
    ) {
        if (tile.isEmptySlot) {
            // Iconic blank Switch white/charcoal tile with subtle center + mark on focus
            if (focused) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = tile.title,
                        tint = palette.focusCyan,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = "Pin Game",
                        color = palette.secondaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(tile.gradient))
            )
            if (tile.useHeroArtwork) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_cockpit),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.78f,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x40000000),
                                Color(0xCC0A0D12)
                            )
                        )
                    )
            )
            if (tile.badge != null) {
                Surface(
                    color = Color(0xCC0D1117),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = tile.badge,
                        color = Color.White,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Icon(
                imageVector = tile.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.94f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 16.dp)
                    .size(44.dp)
            )
            Text(
                text = tile.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            )
        }
    }
}

@Composable
private fun SwitchDockAppletButton(
    applet: SwitchDockApplet,
    selected: Boolean,
    palette: SwitchThemePalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.border(2.5.dp, palette.focusCyan, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .testTag("switch_${applet.id}"),
        contentAlignment = Alignment.Center
    ) {
        if (applet.isRedOnlinePill) {
            // Iconic Nintendo Switch Online bright red circle with dual Joy-Con mark & "ONLINE"
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(palette.onlineRed),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NintendoJoyConMark(
                        size = 15.dp,
                        leftColor = Color.White,
                        rightColor = Color.White,
                        cutoutColor = palette.onlineRed
                    )
                    Text(
                        text = "ONLINE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 5.5.sp,
                        lineHeight = 6.sp
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (selected) palette.focusCyan.copy(alpha = 0.12f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = applet.icon,
                    contentDescription = applet.label,
                    tint = applet.tint,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        if (applet.hasBadgeDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.onlineRed)
                    .border(1.dp, palette.dockSurface, CircleShape)
            )
        }
    }
}

@Composable
private fun SwitchPinGameDialog(
    profiles: List<ControllerProfile>,
    defaultProfileId: String,
    defaultRegion: GfnRegionPreset,
    onDismiss: () -> Unit,
    onSave: (PinnedGame) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedProfileId by remember { mutableStateOf(defaultProfileId) }
    var selectedRegion by remember { mutableStateOf(defaultRegion) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pin Game to Switch Home", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Game Title") },
                    placeholder = { Text("e.g. Super Mario Odyssey / Fortnite") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (profiles.isNotEmpty()) {
                    Text("Controller Layout", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(profiles) { _, p ->
                            FilterChip(
                                selected = selectedProfileId == p.id,
                                onClick = { selectedProfileId = p.id },
                                label = { Text(p.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                Text("Bypass VPN Region", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(GfnRegionPreset.entries) { _, r ->
                        FilterChip(
                            selected = selectedRegion == r,
                            onClick = { selectedRegion = r },
                            label = { Text(r.shortCode, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = title.trim()
                    if (clean.isNotEmpty()) {
                        onSave(
                            PinnedGame(
                                id = UUID.randomUUID().toString(),
                                title = clean,
                                profileId = selectedProfileId,
                                regionPreset = selectedRegion
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0AB9E6))
            ) {
                Text("Pin Software", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

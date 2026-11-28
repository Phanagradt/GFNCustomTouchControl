package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.components.Ps5AccentBlue
import com.example.ui.components.Ps5CanvasBg
import com.example.ui.components.Ps5DeepBlueBg
import com.example.ui.components.Ps5GoldAccent
import com.example.ui.components.Ps5IceHighlight
import com.example.ui.components.Ps5MutedText
import com.example.update.UpdateUiState
import com.example.util.ButtonImageCodec
import java.util.UUID

private data class Ps5AppTile(
    val id: String,
    val shortLabel: String,
    val kicker: String,
    val headline: String,
    val description: String,
    val ctaLabel: String,
    val icon: ImageVector,
    val tileGradient: List<Color>,
    val bgTop: Color,
    val bgGlow: Color,
    val useHeroArtwork: Boolean = false,
    val onLaunch: () -> Unit
)

private data class Ps5ActivityCardData(
    val id: String,
    val title: String,
    val subtitle: String,
    val gradient: List<Color>,
    val accent: Color,
    val icon: ImageVector,
    val imageBitmap: ImageBitmap? = null,
    val onClick: () -> Unit
)

private val PS5_PINNED_COLORS = listOf(
    Triple(listOf(Color(0xFF1D4ED8), Color(0xFF0A1633)), Color(0xFF0A1B3D), Color(0xFF38BDF8)),
    Triple(listOf(Color(0xFF7E22CE), Color(0xFF1F0C36)), Color(0xFF1A0D2E), Color(0xFFC084FC)),
    Triple(listOf(Color(0xFFB91C1C), Color(0xFF280B10)), Color(0xFF240B10), Color(0xFFF87171)),
    Triple(listOf(Color(0xFF0F766E), Color(0xFF082224)), Color(0xFF082026), Color(0xFF2DD4BF))
)

/**
 * Faithful PlayStation 5 Home Screen replica matching the user's reference screenshot:
 * - Top-left compact rounded square icon row with enlarged luminous white-bordered focused tile + label below
 * - Full-bleed atmospheric 4K-style dark backdrop with subtle PS5 golden/blue light motes
 * - Mid-left light-weight editorial hero typography ("Official news..." kicker, crisp light headline,
 *   subtitle, and high-contrast white Play pill + Control Center button)
 * - Bottom horizontal row of widescreen 16:9 Activity & News cards wired to real XenosPad features & live community posts
 */
@Composable
fun PlayStationHomeScreen(
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
    ps5SelectTrigger: Int,
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
    onOpenControlCenter: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenSteamLink: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(1) } // Default focused on "Explore / GeForce NOW" just like screenshot!
    var showPinGameDialog by remember { mutableStateOf(false) }

    val appTiles = remember(
        webPlayerConfig.regionPreset,
        isOverlayRunning,
        activeProfile?.id,
        activeProfile?.name,
        pinnedGames,
        filterState.activeSlot,
        workshopThemes.size,
        readiness.canDrawOverlays
    ) {
        buildList {
            add(
                Ps5AppTile(
                    id = "ps5_workshop_bag",
                    shortLabel = "Workshop",
                    kicker = "XenosPad Community | ${workshopThemes.size} Themes",
                    headline = "Button Theme Workshop",
                    description = "Discover custom button skins, creator profiles, and community builds.",
                    ctaLabel = "Open Workshop",
                    icon = Icons.Default.ShoppingBag,
                    tileGradient = listOf(Color(0xFF14284B), Color(0xFF0A1426)),
                    bgTop = Color(0xFF0B1C3A),
                    bgGlow = Ps5IceHighlight,
                    onLaunch = onOpenWorkshop
                )
            )
            add(
                Ps5AppTile(
                    id = "ps5_explore_gfn",
                    shortLabel = "Explore",
                    kicker = "Cloud Streaming · Region ${webPlayerConfig.regionPreset.shortCode} | Ready",
                    headline = "GeForce NOW Cloud Play",
                    description = "Stream PC games with Bypass VPN, 18 Freestyle filters, Instant Replay & virtual controller.",
                    ctaLabel = "Play",
                    icon = Icons.Default.Explore,
                    tileGradient = listOf(Color(0xFF172033), Color(0xFF0B0F1A)),
                    bgTop = Color(0xFF0D1D3B),
                    bgGlow = Color(0xFF4C8DFF),
                    useHeroArtwork = true,
                    onLaunch = onLaunchGeForceNow
                )
            )
            add(
                Ps5AppTile(
                    id = "ps5_xcloud",
                    shortLabel = "Xbox Cloud",
                    kicker = "Cloud Streaming · xbox.com/play | Bypass VPN Active",
                    headline = "Xbox Cloud Gaming",
                    description = "Play Game Pass cloud titles inside XenosPad with Region Bypass, GPU Filters & Replay.",
                    ctaLabel = "Play",
                    icon = Icons.Default.CloudQueue,
                    tileGradient = listOf(Color(0xFF14532D), Color(0xFF071F12)),
                    bgTop = Color(0xFF0A2419),
                    bgGlow = Color(0xFF34D399),
                    onLaunch = onLaunchXboxCloud
                )
            )
            add(
                Ps5AppTile(
                    id = "ps5_steam_link",
                    shortLabel = "Steam Link",
                    kicker = "PC Remote Play · Local Wi-Fi | Touch HUD Ready",
                    headline = "Steam Link · My PC",
                    description = "Play games from your own PC or laptop using XenosPad's custom touch screen, gyro aim & filters.",
                    ctaLabel = "Open PC Hub",
                    icon = Icons.Default.CastConnected,
                    tileGradient = listOf(Color(0xFF184E77), Color(0xFF0A1A2B)),
                    bgTop = Color(0xFF0D2742),
                    bgGlow = Color(0xFF66C0F4),
                    onLaunch = onOpenSteamLink
                )
            )
            add(
                Ps5AppTile(
                    id = "ps5_overlay_hud",
                    shortLabel = "Overlay",
                    kicker = "Floating Gamepad | ${activeProfile?.name ?: "Default Layout"}",
                    headline = if (isOverlayRunning) "Touch Overlay Active" else "Start Touch Overlay HUD",
                    description = "Float your custom DualSense-style buttons over any Android or cloud game.",
                    ctaLabel = if (isOverlayRunning) "Stop HUD" else "Launch HUD",
                    icon = if (isOverlayRunning) Icons.Default.Stop else Icons.Default.Layers,
                    tileGradient = listOf(Color(0xFF1E3A8A), Color(0xFF0E172E)),
                    bgTop = Color(0xFF0F214A),
                    bgGlow = Ps5IceHighlight,
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
                val pal = PS5_PINNED_COLORS[idx % PS5_PINNED_COLORS.size]
                val linkedProfile = allProfiles.firstOrNull { it.id == pinned.profileId }?.name
                add(
                    Ps5AppTile(
                        id = "ps5_pinned_${pinned.id}",
                        shortLabel = pinned.title,
                        kicker = "Pinned Game | Region ${pinned.regionPreset.shortCode}",
                        headline = pinned.title,
                        description = linkedProfile?.let { "Launches in GeForce NOW with layout: $it" }
                            ?: "One-tap GeForce NOW launch with custom touch overlay.",
                        ctaLabel = "Play",
                        icon = Icons.Default.SportsEsports,
                        tileGradient = pal.first,
                        bgTop = pal.second,
                        bgGlow = pal.third,
                        onLaunch = { onLaunchPinnedGame(pinned) }
                    )
                )
            }
            add(
                Ps5AppTile(
                    id = "ps5_share_factory",
                    shortLabel = "Media",
                    kicker = "Captures & Clips | Instant Replay",
                    headline = "Media Gallery & Replays",
                    description = "Review your Instant Replay clips, manual recordings, and screenshots.",
                    ctaLabel = "Open Gallery",
                    icon = Icons.Default.PhotoLibrary,
                    tileGradient = listOf(Color(0xFF0D4F4A), Color(0xFF071F20)),
                    bgTop = Color(0xFF092329),
                    bgGlow = Color(0xFF2DD4BF),
                    onLaunch = onOpenGallery
                )
            )
            add(
                Ps5AppTile(
                    id = "ps5_layout_studio",
                    shortLabel = "Studio",
                    kicker = "Controller Profiles | ${allProfiles.size} Saved",
                    headline = "Layout Studio",
                    description = "Arrange buttons, custom artwork, toggles, and macros on an interactive grid.",
                    ctaLabel = "Customize",
                    icon = Icons.Default.Edit,
                    tileGradient = listOf(Color(0xFF4C1D95), Color(0xFF170B2E)),
                    bgTop = Color(0xFF1C1038),
                    bgGlow = Color(0xFFA78BFA),
                    onLaunch = { activeProfile?.let { onOpenEditor(it.id) } ?: onOpenProfiles() }
                )
            )
            add(
                Ps5AppTile(
                    id = "ps5_visual_fx",
                    shortLabel = "Visuals",
                    kicker = "Freestyle Shaders | ${if (filterState.activeSlot > 0) "Slot ${filterState.activeSlot} Active" else "Off"}",
                    headline = "Visual Filters & Crosshair",
                    description = "Dial in 18 real-time color & clarity shaders plus custom precision crosshairs.",
                    ctaLabel = "Open Visuals",
                    icon = Icons.Default.Tune,
                    tileGradient = listOf(Color(0xFF164E63), Color(0xFF08202B)),
                    bgTop = Color(0xFF0A2430),
                    bgGlow = Color(0xFF38BDF8),
                    onLaunch = onOpenFilters
                )
            )
            add(
                Ps5AppTile(
                    id = "ps5_sandbox",
                    shortLabel = "Sandbox",
                    kicker = "DualSense & Touch Diagnostics",
                    headline = "Controller Test Arena",
                    description = "Test analog sticks, triggers, gyro telemetry, and multi-touch latency.",
                    ctaLabel = "Open Sandbox",
                    icon = Icons.Default.TouchApp,
                    tileGradient = listOf(Color(0xFF78350F), Color(0xFF261306)),
                    bgTop = Color(0xFF24160A),
                    bgGlow = Ps5GoldAccent,
                    onLaunch = onOpenSandbox
                )
            )
            add(
                Ps5AppTile(
                    id = "ps5_pin_game",
                    shortLabel = "Add Game",
                    kicker = "Home Customization",
                    headline = "Pin a Game to PS5 Home",
                    description = "Add any GeForce NOW game shortcut right to your PlayStation 5 tile bar.",
                    ctaLabel = "Pin Game",
                    icon = Icons.Default.Add,
                    tileGradient = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                    bgTop = Ps5DeepBlueBg,
                    bgGlow = Ps5IceHighlight,
                    onLaunch = { showPinGameDialog = true }
                )
            )
        }
    }

    val safeIndex = selectedIndex.coerceIn(0, (appTiles.size - 1).coerceAtLeast(0))
    val focusedTile = appTiles[safeIndex]

    LaunchedEffect(ps5SelectTrigger) {
        if (ps5SelectTrigger > 0) {
            focusedTile.onLaunch()
        }
    }

    // Bottom horizontal Activity & News Cards (matching the 4 widescreen 16:9 cards in the PS5 screenshot)
    val activityCards = remember(
        updateState,
        communityPosts,
        workshopThemes,
        allProfiles,
        filterState.activeSlot
    ) {
        buildList {
            if (updateState is UpdateUiState.ReadyToInstall) {
                add(
                    Ps5ActivityCardData(
                        id = "ps5_act_install_update",
                        title = "Install System Update",
                        subtitle = "Downloaded & ready",
                        gradient = listOf(Color(0xFF0070D1), Color(0xFF081B38)),
                        accent = Ps5IceHighlight,
                        icon = Icons.Default.Download,
                        onClick = onInstallUpdate
                    )
                )
            } else if (updateState is UpdateUiState.UpdateAvailable) {
                add(
                    Ps5ActivityCardData(
                        id = "ps5_act_download_update",
                        title = "XenosPad ${updateState.release.tagName} Available",
                        subtitle = "Download latest update",
                        gradient = listOf(Color(0xFF0E4B8A), Color(0xFF08162C)),
                        accent = Ps5IceHighlight,
                        icon = Icons.Default.Download,
                        onClick = onDownloadUpdate
                    )
                )
            }

            // Live Community Posts as official PS5 News cards!
            communityPosts.take(3).forEachIndexed { idx, post ->
                val bmp = post.imageBase64?.takeIf { it.isNotBlank() }?.let {
                    ButtonImageCodec.decodeBase64ToImageBitmap(it)
                }
                val gradients = listOf(
                    listOf(Color(0xFF1E3A5F), Color(0xFF0B1424)),
                    listOf(Color(0xFF4C1D95), Color(0xFF150A2B)),
                    listOf(Color(0xFF92400E), Color(0xFF241106))
                )
                add(
                    Ps5ActivityCardData(
                        id = "ps5_act_post_${post.id}",
                        title = post.title.ifBlank { "${post.authorName} · ${post.topic}" },
                        subtitle = "By ${post.authorName.ifBlank { "Community" }}",
                        gradient = gradients[idx % gradients.size],
                        accent = Ps5IceHighlight,
                        icon = Icons.Default.Forum,
                        imageBitmap = bmp,
                        onClick = {
                            if (post.ownerId.isNotBlank()) onOpenPublicProfile(post.ownerId) else onOpenWorkshop()
                        }
                    )
                )
            }

            add(
                Ps5ActivityCardData(
                    id = "ps5_act_workshop",
                    title = "Explore ${workshopThemes.size.coerceAtLeast(1)} Workshop Themes",
                    subtitle = "Community button skins",
                    gradient = listOf(Color(0xFF1D4ED8), Color(0xFF0B1530)),
                    accent = Ps5IceHighlight,
                    icon = Icons.Default.Palette,
                    onClick = onOpenWorkshop
                )
            )
            add(
                Ps5ActivityCardData(
                    id = "ps5_act_visuals",
                    title = if (filterState.activeSlot > 0) "Visual Filter Slot ${filterState.activeSlot} Active" else "Customize Visual Filters",
                    subtitle = "18 Freestyle shaders & reticle",
                    gradient = listOf(Color(0xFF0F766E), Color(0xFF081E24)),
                    accent = Color(0xFF2DD4BF),
                    icon = Icons.Default.Tune,
                    onClick = onOpenFilters
                )
            )
            add(
                Ps5ActivityCardData(
                    id = "ps5_act_captures",
                    title = "Instant Replay & Captures",
                    subtitle = "View saved gameplay clips",
                    gradient = listOf(Color(0xFF6B21A8), Color(0xFF180A29)),
                    accent = Color(0xFFC084FC),
                    icon = Icons.Default.PhotoLibrary,
                    onClick = onOpenGallery
                )
            )
        }
    }

    val animatedBgTop by animateColorAsState(
        targetValue = focusedTile.bgTop,
        animationSpec = tween(420),
        label = "ps5BgTop"
    )
    val animatedBgGlow by animateColorAsState(
        targetValue = focusedTile.bgGlow,
        animationSpec = tween(420),
        label = "ps5BgGlow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Ps5CanvasBg)
            .testTag("ps5_home_screen")
    ) {
        // Full-bleed PS5 4K-style atmospheric dynamic background
        Box(modifier = Modifier.fillMaxSize()) {
            if (focusedTile.useHeroArtwork) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_cockpit),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.45f,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            animatedBgTop.copy(alpha = 0.88f),
                            animatedBgTop.copy(alpha = 0.52f),
                            Ps5CanvasBg.copy(alpha = 0.96f)
                        )
                    )
                )
                // Signature right-center dramatic PS5 hero aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedBgGlow.copy(alpha = 0.34f),
                            animatedBgGlow.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.68f, size.height * 0.34f),
                        radius = size.maxDimension * 0.58f
                    ),
                    center = Offset(size.width * 0.68f, size.height * 0.34f),
                    radius = size.maxDimension * 0.58f
                )
                // Signature subtle golden PS5 home-screen light motes
                val motes = listOf(
                    Triple(0.18f, 0.28f, 2.5f),
                    Triple(0.35f, 0.22f, 1.8f),
                    Triple(0.56f, 0.18f, 2.2f),
                    Triple(0.76f, 0.26f, 3.0f),
                    Triple(0.84f, 0.46f, 2.0f),
                    Triple(0.62f, 0.52f, 1.6f)
                )
                motes.forEach { (fx, fy, r) ->
                    drawCircle(
                        color = Ps5GoldAccent.copy(alpha = 0.26f),
                        radius = r * density,
                        center = Offset(size.width * fx, size.height * fy)
                    )
                }
            }
        }

        // Scrollable PS5 Home Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Top-Left Compact PS5 App & Game Icon Bar (Focused icon enlarges with luminous white ring + label below!)
            item(key = "ps5_top_icon_strip") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.height(104.dp)
                ) {
                    itemsIndexed(appTiles, key = { _, tile -> tile.id }) { index, tile ->
                        val isFocused = index == safeIndex
                        Ps5TopIconTile(
                            tile = tile,
                            focused = isFocused,
                            onClick = {
                                if (isFocused) {
                                    tile.onLaunch()
                                } else {
                                    selectedIndex = index
                                }
                            }
                        )
                    }
                }
            }

            // 2. Mid-Left Editorial Hero Copy + White Play Pill (exact PS5 Home focal block)
            item(key = "ps5_editorial_hero") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = focusedTile.kicker,
                        color = Color(0xFFC2CEE0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = focusedTile.headline,
                        color = Color.White,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 32.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = focusedTile.description,
                        color = Color(0xFFB5C3D8),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Iconic PS5 high-contrast white pill Play CTA
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .clickable(onClick = focusedTile.onLaunch)
                                .testTag("ps5_hero_play_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Ps5CanvasBg,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = focusedTile.ctaLabel,
                                    color = Ps5CanvasBg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Circular PS5 "···" Options / Control Center pill
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x55FFFFFF))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                .clickable(onClick = onOpenControlCenter)
                                .testTag("ps5_hero_more_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Open Control Center",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 3. Bottom Horizontal 16:9 Activity & News Cards Row (exact PS5 bottom row)
            item(key = "ps5_activity_cards_row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activityCards, key = { it.id }) { card ->
                        Ps5WidescreenActivityCard(card = card)
                    }
                }
            }

            // 4. Below-the-Fold Quick Profile Switcher Strip
            if (allProfiles.isNotEmpty()) {
                item(key = "ps5_profiles_section") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Controller Layouts",
                                color = Color.White,
                                fontWeight = FontWeight.Light,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "View All",
                                color = Ps5IceHighlight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onOpenProfiles)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(allProfiles, key = { it.id }) { profile ->
                                val isActive = profile.id == activeProfile?.id
                                Surface(
                                    color = if (isActive) Color(0xCC12284B) else Color(0xB30F1727),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .width(196.dp)
                                        .border(
                                            width = if (isActive) 1.5.dp else 1.dp,
                                            color = if (isActive) Color.White else Color(0xFF283A5A),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onSelectProfile(profile.id) }
                                        .testTag("ps5_profile_card_${profile.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isActive) Ps5AccentBlue else Color(0xFF1E2D49)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Gamepad,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = profile.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isActive) "Active · ${profile.controls.size} controls" else "${profile.controls.size} controls",
                                                color = if (isActive) Ps5IceHighlight else Ps5MutedText,
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
        }
    }

    if (showPinGameDialog) {
        Ps5PinGameDialog(
            profiles = allProfiles,
            defaultProfileId = activeProfile?.id ?: allProfiles.firstOrNull()?.id.orEmpty(),
            defaultRegion = webPlayerConfig.regionPreset,
            onDismiss = { showPinGameDialog = false },
            onSave = { pinned ->
                onSavePinnedGame(pinned)
                showPinGameDialog = false
            }
        )
    }
}

@Composable
private fun Ps5TopIconTile(
    tile: Ps5AppTile,
    focused: Boolean,
    onClick: () -> Unit
) {
    val iconSize by animateDpAsState(
        targetValue = if (focused) 72.dp else 52.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ps5TileSize"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (focused) 16.dp else 12.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ps5TileCorner"
    )

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("ps5_tile_${tile.id}")
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(cornerRadius))
                .background(Brush.linearGradient(tile.tileGradient))
                .border(
                    width = if (focused) 1.8.dp else 1.dp,
                    color = if (focused) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(cornerRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (tile.useHeroArtwork && !focused) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_cockpit),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.55f,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Icon(
                imageVector = tile.icon,
                contentDescription = tile.shortLabel,
                tint = Color.White,
                modifier = Modifier.size(if (focused) 32.dp else 24.dp)
            )
        }
        if (focused) {
            Text(
                text = tile.shortLabel,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(82.dp)
            )
        }
    }
}

@Composable
private fun Ps5WidescreenActivityCard(card: Ps5ActivityCardData) {
    Box(
        modifier = Modifier
            .size(width = 198.dp, height = 110.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(card.gradient))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .clickable(onClick = card.onClick)
            .testTag(card.id)
    ) {
        if (card.imageBitmap != null) {
            Image(
                bitmap = card.imageBitmap,
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = card.icon,
                contentDescription = null,
                tint = card.accent.copy(alpha = 0.35f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(44.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0x55060A14), Color(0xE6050810))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = card.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = card.subtitle,
                color = Color(0xFFB5C4DB),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Ps5PinGameDialog(
    profiles: List<ControllerProfile>,
    defaultProfileId: String,
    defaultRegion: GfnRegionPreset,
    onDismiss: () -> Unit,
    onSave: (PinnedGame) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf(defaultRegion) }
    var selectedProfileId by remember { mutableStateOf(defaultProfileId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0E1626),
        titleContentColor = Color.White,
        textContentColor = Ps5MutedText,
        title = { Text("Pin Game to PS5 Home", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 42) title = it },
                    label = { Text("Game Title") },
                    placeholder = { Text("e.g., Spider-Man, Fortnite") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("GeForce NOW Region", color = Color.White, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GfnRegionPreset.entries.take(6)) { preset ->
                        FilterChip(
                            selected = selectedRegion == preset,
                            onClick = { selectedRegion = preset },
                            label = { Text(preset.shortCode, fontSize = 11.sp) }
                        )
                    }
                }
                if (profiles.isNotEmpty()) {
                    Text("Controller Profile", color = Color.White, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(profiles) { p ->
                            FilterChip(
                                selected = selectedProfileId == p.id,
                                onClick = { selectedProfileId = p.id },
                                label = { Text(p.name, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    if (cleanTitle.isNotEmpty()) {
                        onSave(
                            PinnedGame(
                                id = UUID.randomUUID().toString(),
                                title = cleanTitle,
                                profileId = selectedProfileId,
                                regionPreset = selectedRegion
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Ps5AccentBlue)
            ) {
                Text("Pin Game", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Ps5MutedText)
            }
        }
    )
}

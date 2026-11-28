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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storefront
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
import com.example.ui.components.XboxBrandGreen
import com.example.ui.components.XboxCanvasBg
import com.example.ui.components.XboxFocusGreen
import com.example.ui.components.XboxMutedText
import com.example.ui.components.XboxOrbMark
import com.example.ui.components.XboxTileSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.update.UpdateUiState
import java.util.UUID

private data class XboxHomeTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String?,
    val backdropTop: Color,
    val backdropGlow: Color,
    val cardGradient: List<Color>,
    val icon: ImageVector,
    val useHeroArtwork: Boolean = false,
    val onLaunch: () -> Unit
)

private val XBOX_PINNED_PALETTES = listOf(
    Triple(Color(0xFF102A43), Color(0xFF38BDF8), listOf(Color(0xFF1D4E89), Color(0xFF0A1628))),
    Triple(Color(0xFF2B123D), Color(0xFFC084FC), listOf(Color(0xFF6B21A8), Color(0xFF1B0B2A))),
    Triple(Color(0xFF3B1214), Color(0xFFF87171), listOf(Color(0xFF991B1B), Color(0xFF1C090A))),
    Triple(Color(0xFF123322), Color(0xFF3AE635), listOf(Color(0xFF166534), Color(0xFF0A1C12))),
    Triple(Color(0xFF3B2609), Color(0xFFFBBF24), listOf(Color(0xFF92400E), Color(0xFF1F1406)))
)

/**
 * Faithful Xbox Series X|S Home Dashboard replica matching the user's reference screenshot:
 * - Full-bleed dynamic ambient backdrop that shifts color & atmosphere with the focused tile
 * - Open upper showcase space letting the dynamic background breathe
 * - Signature bottom-aligned Recently Played tile strip with larger green-focused primary tile
 * - 4-card Xbox feature showcase row right beneath ("Browse your games", "Customize your Home",
 *   "Game Pass / Cloud / Updates", and "Play like a Pro")
 * - Scroll-down Quick Resume Profiles & Live Community Workshop sections
 */
@Composable
fun XboxHomeScreen(
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
    xboxSelectTrigger: Int,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onLaunchWebPlayer: () -> Unit,
    onLaunchXboxCloud: () -> Unit = {},
    onLaunchPinnedGame: (PinnedGame) -> Unit,
    onSavePinnedGame: (PinnedGame) -> Unit,
    onToggleOverlay: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenPublicProfile: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSandbox: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var showPinGameDialog by remember { mutableStateOf(false) }

    val homeTiles = remember(
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
                XboxHomeTile(
                    id = "xbox_cloud_gaming",
                    title = "Xbox Cloud Gaming",
                    subtitle = "xbox.com/play · Region ${webPlayerConfig.regionPreset.shortCode} · Bypass VPN & Filters",
                    badge = "XCLOUD",
                    backdropTop = Color(0xFF0A2A17),
                    backdropGlow = XboxFocusGreen,
                    cardGradient = listOf(Color(0xFF107C10), Color(0xFF071D0C)),
                    icon = Icons.Default.SportsEsports,
                    useHeroArtwork = true,
                    onLaunch = onLaunchXboxCloud
                )
            )
            add(
                XboxHomeTile(
                    id = "xbox_gfn_play",
                    title = "GeForce NOW",
                    subtitle = "Cloud Play · Region ${webPlayerConfig.regionPreset.shortCode} · Bypass VPN & Filters",
                    badge = "GFN",
                    backdropTop = Color(0xFF0E2538),
                    backdropGlow = Color(0xFF2E86DE),
                    cardGradient = listOf(Color(0xFF143554), Color(0xFF091420)),
                    icon = Icons.Default.CloudQueue,
                    useHeroArtwork = true,
                    onLaunch = onLaunchWebPlayer
                )
            )
            add(
                XboxHomeTile(
                    id = "xbox_overlay_hud",
                    title = if (isOverlayRunning) "Overlay HUD (Live)" else "Touch Overlay",
                    subtitle = activeProfile?.name?.let { "Active profile: $it" } ?: "Floating gamepad overlay",
                    badge = if (isOverlayRunning) "LIVE" else "HUD",
                    backdropTop = Color(0xFF0D2E1B),
                    backdropGlow = XboxFocusGreen,
                    cardGradient = listOf(Color(0xFF107C10), Color(0xFF09230D)),
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
                val palette = XBOX_PINNED_PALETTES[idx % XBOX_PINNED_PALETTES.size]
                val linkedName = allProfiles.firstOrNull { it.id == pinned.profileId }?.name
                add(
                    XboxHomeTile(
                        id = "xbox_pinned_${pinned.id}",
                        title = pinned.title,
                        subtitle = linkedName?.let { "Pinned · $it" } ?: "Pinned GeForce NOW game",
                        badge = "PINNED",
                        backdropTop = palette.first,
                        backdropGlow = palette.second,
                        cardGradient = palette.third,
                        icon = Icons.Default.SportsEsports,
                        onLaunch = { onLaunchPinnedGame(pinned) }
                    )
                )
            }
            add(
                XboxHomeTile(
                    id = "xbox_store",
                    title = "Xbox Store",
                    subtitle = "Browse live Xbox & PC Game Pass titles",
                    badge = "STORE",
                    backdropTop = Color(0xFF0E2B20),
                    backdropGlow = Color(0xFF2CC84D),
                    cardGradient = listOf(Color(0xFF135C2B), Color(0xFF0A1D12)),
                    icon = Icons.Default.Storefront,
                    onLaunch = onOpenStore
                )
            )
            add(
                XboxHomeTile(
                    id = "xbox_layout_studio",
                    title = "Layout Studio",
                    subtitle = activeProfile?.let { "Customize ${it.name}" } ?: "Edit touch buttons & macros",
                    badge = "STUDIO",
                    backdropTop = Color(0xFF1B1B36),
                    backdropGlow = Color(0xFF8B5CF6),
                    cardGradient = listOf(Color(0xFF4338CA), Color(0xFF16133B)),
                    icon = Icons.Default.Edit,
                    onLaunch = { activeProfile?.let { onOpenEditor(it.id) } ?: onOpenProfiles() }
                )
            )
            add(
                XboxHomeTile(
                    id = "xbox_visuals",
                    title = "Visuals & FX",
                    subtitle = if (filterState.activeSlot > 0) "GPU Filter Slot ${filterState.activeSlot} active" else "Shaders & custom crosshairs",
                    badge = "FX",
                    backdropTop = Color(0xFF102933),
                    backdropGlow = Color(0xFF22D3EE),
                    cardGradient = listOf(Color(0xFF0E7490), Color(0xFF09222C)),
                    icon = Icons.Default.Tune,
                    onLaunch = onOpenFilters
                )
            )
            add(
                XboxHomeTile(
                    id = "xbox_captures",
                    title = "Captures",
                    subtitle = "Instant replays, recordings & screenshots",
                    badge = "CLIPS",
                    backdropTop = Color(0xFF2E1624),
                    backdropGlow = Color(0xFFEC4899),
                    cardGradient = listOf(Color(0xFF9D174D), Color(0xFF260917)),
                    icon = Icons.Default.PhotoLibrary,
                    onLaunch = onOpenGallery
                )
            )
            add(
                XboxHomeTile(
                    id = "xbox_sandbox",
                    title = "Test Sandbox",
                    subtitle = "Verify sticks, triggers & gyro telemetry",
                    badge = "TEST",
                    backdropTop = Color(0xFF2E220D),
                    backdropGlow = Color(0xFFF59E0B),
                    cardGradient = listOf(Color(0xFFB45309), Color(0xFF241406)),
                    icon = Icons.Default.TouchApp,
                    onLaunch = onOpenSandbox
                )
            )
            add(
                XboxHomeTile(
                    id = "xbox_pin_new",
                    title = "Pin a Game",
                    subtitle = "Add a quick-launch tile to Xbox Home",
                    badge = "NEW",
                    backdropTop = Color(0xFF1D2228),
                    backdropGlow = XboxFocusGreen,
                    cardGradient = listOf(Color(0xFF282E35), Color(0xFF14181C)),
                    icon = Icons.Default.Add,
                    onLaunch = { showPinGameDialog = true }
                )
            )
        }
    }

    val safeIndex = selectedIndex.coerceIn(0, (homeTiles.size - 1).coerceAtLeast(0))
    val focusedTile = homeTiles[safeIndex]

    LaunchedEffect(xboxSelectTrigger) {
        if (xboxSelectTrigger > 0) {
            focusedTile.onLaunch()
        }
    }

    val animatedTopBg by animateColorAsState(
        targetValue = focusedTile.backdropTop,
        animationSpec = tween(420),
        label = "xboxBackdropTop"
    )
    val animatedGlow by animateColorAsState(
        targetValue = focusedTile.backdropGlow,
        animationSpec = tween(420),
        label = "xboxBackdropGlow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(XboxCanvasBg)
            .testTag("xbox_home_screen")
    ) {
        // Full-bleed Xbox Series X|S dynamic ambient background (shifts smoothly per focused tile)
        Box(modifier = Modifier.fillMaxSize()) {
            if (focusedTile.useHeroArtwork) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_cockpit),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.42f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                // Cosmic Xbox ambient gradient & central orbital horizon glow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            animatedTopBg.copy(alpha = 0.85f),
                            animatedTopBg.copy(alpha = 0.45f),
                            XboxCanvasBg
                        )
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedGlow.copy(alpha = 0.36f),
                            animatedGlow.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.36f),
                        radius = size.maxDimension * 0.55f
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.36f),
                    radius = size.maxDimension * 0.55f
                )
                // Subtle light-flare beam down the center (inspired by Starfield/Xbox dynamic backgrounds)
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.18f),
                            animatedGlow.copy(alpha = 0.32f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(size.width * 0.5f, 0f),
                    end = Offset(size.width * 0.5f, size.height * 0.85f),
                    strokeWidth = 3f
                )
            }
        }

        // Main scrollable Xbox Home content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Open breathing hero showcase space matching the Xbox Series X|S Home silhouette
            item(key = "xbox_hero_breathing_space") {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 14.dp)
                ) {
                    val heroHeight = if (maxWidth >= 600.dp) 136.dp else 108.dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroHeight),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = focusedTile.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = focusedTile.subtitle,
                                    color = Color(0xFFD6DCE5),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Surface(
                                color = XboxBrandGreen,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .border(1.5.dp, XboxFocusGreen, RoundedCornerShape(6.dp))
                                    .clickable(onClick = focusedTile.onLaunch)
                                    .testTag("xbox_hero_launch_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Launch",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Signature Xbox Bottom-Aligned Recently Played Tile Row (Larger glowing green focused tile!)
            item(key = "xbox_recent_tiles_row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(158.dp)
                ) {
                    itemsIndexed(homeTiles, key = { _, tile -> tile.id }) { index, tile ->
                        val isFocused = index == safeIndex
                        XboxDashboardSquareTile(
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

            // Exact 4-Card Xbox Feature Showcase Row below the tile strip
            item(key = "xbox_four_card_row") {
                val hasUpdate = updateState is UpdateUiState.UpdateAvailable ||
                    updateState is UpdateUiState.ReadyToInstall
                val thirdCardBadge = when {
                    updateState is UpdateUiState.ReadyToInstall -> "INSTALL UPDATE"
                    updateState is UpdateUiState.UpdateAvailable -> "UPDATE READY"
                    else -> "GAME PASS · HUB"
                }
                val thirdCardTitle = when (updateState) {
                    is UpdateUiState.ReadyToInstall -> "Install XenosPad Update"
                    is UpdateUiState.UpdateAvailable -> "XenosPad ${updateState.release.tagName}"
                    else -> if (workshopThemes.isNotEmpty()) {
                        "Workshop (${workshopThemes.size} Themes)"
                    } else {
                        "Community Workshop"
                    }
                }
                val thirdCardAction: () -> Unit = when (updateState) {
                    is UpdateUiState.ReadyToInstall -> onInstallUpdate
                    is UpdateUiState.UpdateAvailable -> onDownloadUpdate
                    else -> onOpenWorkshop
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val useFourAcross = maxWidth >= 620.dp
                    if (useFourAcross) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            XboxBrowseGamesCard(
                                onClick = onOpenProfiles,
                                modifier = Modifier.weight(1f)
                            )
                            XboxCustomizeHomeCard(
                                onClick = onOpenFilters,
                                modifier = Modifier.weight(1f)
                            )
                            XboxHighlightPromoCard(
                                badge = thirdCardBadge,
                                title = thirdCardTitle,
                                highlightUpdate = hasUpdate,
                                onClick = thirdCardAction,
                                modifier = Modifier.weight(1f)
                            )
                            XboxPlayLikeProCard(
                                onClick = { activeProfile?.let { onOpenEditor(it.id) } ?: onOpenSandbox() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                XboxBrowseGamesCard(
                                    onClick = onOpenProfiles,
                                    modifier = Modifier.weight(1f)
                                )
                                XboxCustomizeHomeCard(
                                    onClick = onOpenFilters,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                XboxHighlightPromoCard(
                                    badge = thirdCardBadge,
                                    title = thirdCardTitle,
                                    highlightUpdate = hasUpdate,
                                    onClick = thirdCardAction,
                                    modifier = Modifier.weight(1f)
                                )
                                XboxPlayLikeProCard(
                                    onClick = { activeProfile?.let { onOpenEditor(it.id) } ?: onOpenSandbox() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Below-the-fold Xbox Section 1: Quick Resume Controller Profiles
            if (allProfiles.isNotEmpty()) {
                item(key = "xbox_quick_resume_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Quick Resume · Controller Layouts",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "See all",
                            color = XboxFocusGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onOpenProfiles)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                item(key = "xbox_quick_resume_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(allProfiles, key = { _, p -> p.id }) { _, profile ->
                            val isActive = profile.id == activeProfile?.id
                            Surface(
                                color = if (isActive) Color(0xFF172B19) else XboxTileSurface,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .width(208.dp)
                                    .border(
                                        width = if (isActive) 1.5.dp else 1.dp,
                                        color = if (isActive) XboxFocusGreen else Color(0xFF2D3137),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onSelectProfile(profile.id) }
                                    .testTag("xbox_profile_card_${profile.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isActive) XboxBrandGreen else Color(0xFF2E333A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Gamepad,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isActive) "Active · ${profile.controls.size} controls" else "${profile.controls.size} controls",
                                            color = if (isActive) XboxFocusGreen else XboxMutedText,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Below-the-fold Xbox Section 2: Community Feed (Real live posts)
            if (communityPosts.isNotEmpty()) {
                item(key = "xbox_community_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Official Club & Community Feed",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Open Hub",
                            color = XboxFocusGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onOpenWorkshop)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                item(key = "xbox_community_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(communityPosts.take(6), key = { _, post -> post.id }) { _, post ->
                            Surface(
                                color = XboxTileSurface,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .width(240.dp)
                                    .border(1.dp, Color(0xFF2D3137), RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (post.ownerId.isNotBlank()) {
                                            onOpenPublicProfile(post.ownerId)
                                        } else {
                                            onOpenWorkshop()
                                        }
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Forum,
                                            contentDescription = null,
                                            tint = XboxFocusGreen,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = post.authorName.ifBlank { "Community Member" },
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = post.topic.uppercase(),
                                            color = XboxMutedText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = post.title.ifBlank { post.body },
                                        color = Color(0xFFE0E5EC),
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

            // Compact System & Permissions Footer Strip
            item(key = "xbox_system_footer_strip") {
                Surface(
                    color = Color(0xFF181B1F),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, Color(0xFF2A2F35), RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenSettings)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            XboxOrbMark(size = 20.dp)
                            Column {
                                Text(
                                    text = "Xbox Dashboard Theme (Beta)",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (readiness.canDrawOverlays) {
                                        "Overlay ready · Tap for Settings & Themes"
                                    } else {
                                        "Tap to enable Overlay permission or customize Settings"
                                    },
                                    color = XboxMutedText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Text(
                            text = "Settings",
                            color = XboxFocusGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    if (showPinGameDialog) {
        XboxPinGameDialog(
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
private fun XboxDashboardSquareTile(
    tile: XboxHomeTile,
    focused: Boolean,
    onClick: () -> Unit
) {
    val tileWidth by animateDpAsState(
        targetValue = if (focused) 144.dp else 102.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "xboxTileWidth"
    )
    val tileHeight by animateDpAsState(
        targetValue = if (focused) 148.dp else 102.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "xboxTileHeight"
    )

    Box(
        modifier = Modifier
            .size(width = tileWidth, height = tileHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(tile.cardGradient))
            .border(
                width = if (focused) 2.5.dp else 1.dp,
                color = if (focused) XboxFocusGreen else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .testTag("xbox_tile_${tile.id}")
    ) {
        if (tile.useHeroArtwork) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_cockpit),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.72f,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Subtle dark vignette for crisp icon & text legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x55000000),
                            Color(0xCC0A0D10)
                        )
                    )
                )
        )

        if (tile.badge != null && focused) {
            Surface(
                color = Color(0xCC101418),
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(7.dp)
            ) {
                Text(
                    text = tile.badge,
                    color = XboxFocusGreen,
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
            tint = Color.White.copy(alpha = if (focused) 0.95f else 0.85f),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = if (focused) 18.dp else 8.dp)
                .size(if (focused) 40.dp else 30.dp)
        )

        // Signature Xbox dark translucent title bar at the bottom of the focused tile
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(if (focused) Color(0xCC161B20) else Color(0x99101418))
                .padding(horizontal = 8.dp, vertical = if (focused) 7.dp else 5.dp)
        ) {
            Text(
                text = tile.title,
                color = Color.White,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = if (focused) 12.sp else 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Card 1: "Browse your games" — matches the iconic 4-strip vertical art collage + controller glyph.
 */
@Composable
private fun XboxBrowseGamesCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.55f)
            .clip(RoundedCornerShape(7.dp))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .testTag("xbox_card_browse_games")
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Brush.verticalGradient(listOf(Color(0xFF4B6B3B), Color(0xFF1F2E1A)))))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Brush.verticalGradient(listOf(Color(0xFF9E5A3C), Color(0xFF331B16)))))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Brush.verticalGradient(listOf(Color(0xFF5D3FD3), Color(0xFF1D1347)))))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Brush.verticalGradient(listOf(Color(0xFF3B82C4), Color(0xFF1D4F2E)))))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0x22000000), Color(0xD60C0E10))))
        )
        Icon(
            imageVector = Icons.Default.SportsEsports,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 10.dp)
                .size(28.dp)
        )
        Text(
            text = "Browse your games",
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

/**
 * Card 2: "Customize your Home" — matches the dark starry slate card with brush/display glyph & clouds.
 */
@Composable
private fun XboxCustomizeHomeCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.55f)
            .clip(RoundedCornerShape(7.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF132D3E), Color(0xFF0A1620))))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .testTag("xbox_card_customize_home")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Minimal stylized cloud silhouettes matching Xbox's "Customize your Home" card
            drawCircle(
                color = Color(0x55DCEEFB),
                radius = size.height * 0.18f,
                center = Offset(size.width * 0.12f, size.height * 0.24f)
            )
            drawCircle(
                color = Color(0x337CC6FE),
                radius = size.height * 0.22f,
                center = Offset(size.width * 0.88f, size.height * 0.82f)
            )
        }
        Icon(
            imageVector = Icons.Default.Brush,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 10.dp)
                .size(28.dp)
        )
        Text(
            text = "Customize your Home",
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

/**
 * Card 3: Featured Game Pass / Workshop / Update card with the iconic top-left dark pill badge.
 */
@Composable
private fun XboxHighlightPromoCard(
    badge: String,
    title: String,
    highlightUpdate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.55f)
            .clip(RoundedCornerShape(7.dp))
            .background(
                Brush.linearGradient(
                    if (highlightUpdate) {
                        listOf(Color(0xFF107C10), Color(0xFF0B2B14))
                    } else {
                        listOf(Color(0xFF1F6E8C), Color(0xFF163D5C), Color(0xFF0E1B2A))
                    }
                )
            )
            .border(
                width = if (highlightUpdate) 1.5.dp else 1.dp,
                color = if (highlightUpdate) XboxFocusGreen else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(7.dp)
            )
            .clickable(onClick = onClick)
            .testTag("xbox_card_highlight_promo")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC0A0E12))))
        )
        Surface(
            color = Color(0xCC12161A),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                XboxOrbMark(size = 10.dp)
                Text(
                    text = badge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 8.sp,
                    letterSpacing = 0.4.sp
                )
            }
        }
        Icon(
            imageVector = if (highlightUpdate) Icons.Default.Download else Icons.Default.Palette,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 4.dp, bottom = 10.dp)
                .size(26.dp)
        )
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

/**
 * Card 4: "Play like a Pro" — deep matte obsidian hardware card opening Layout Studio / Sandbox.
 */
@Composable
private fun XboxPlayLikeProCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1.55f)
            .clip(RoundedCornerShape(7.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2B2F36), Color(0xFF0A0B0D))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .testTag("xbox_card_play_pro")
    ) {
        Icon(
            imageVector = Icons.Default.Gamepad,
            contentDescription = null,
            tint = Color(0xFFDCE2EA),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 10.dp)
                .size(34.dp)
        )
        Text(
            text = "Play like a Pro",
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun XboxPinGameDialog(
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
        containerColor = Color(0xFF1A1E22),
        titleContentColor = Color.White,
        textContentColor = XboxMutedText,
        title = { Text("Pin Game to Xbox Home", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 42) title = it },
                    label = { Text("Game Title") },
                    placeholder = { Text("e.g., Starfield, Forza Horizon 5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("GeForce NOW Region", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(GfnRegionPreset.entries.take(6)) { _, preset ->
                        FilterChip(
                            selected = selectedRegion == preset,
                            onClick = { selectedRegion = preset },
                            label = { Text(preset.shortCode, fontSize = 11.sp) }
                        )
                    }
                }
                if (profiles.isNotEmpty()) {
                    Text("Controller Profile", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(profiles) { _, p ->
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
                colors = ButtonDefaults.buttonColors(containerColor = XboxBrandGreen)
            ) {
                Text("Pin to Home", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = XboxMutedText)
            }
        }
    )
}

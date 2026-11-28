package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.update.UpdateUiState
import com.example.util.ButtonImageCodec
import java.util.UUID

private val SteamCanvasBg = Color(0xFF0E1218)
private val SteamCardFooterBg = Color(0xFF232730)
private val SteamFocusCyan = Color(0xFF66C0F4)
private val SteamActionBlue = Color(0xFF1A9FFF)
private val SteamPlayGreen = Color(0xFF59BF40)
private val SteamFriendsGreen = Color(0xFF4CA844)

private sealed interface SteamShelfItem {
    val key: String
    val title: String
    val statusLine: String
    val badgeTag: String
    val ambientTopColor: Color
    val coverGradient: List<Color>
    val accentColor: Color
    val icon: ImageVector
    val primaryActionLabel: String

    data class WebPlayHero(
        val regionCode: String,
        val isOverlayLive: Boolean,
        val profileName: String
    ) : SteamShelfItem {
        override val key = "shelf_gfn_webplay"
        override val title = "GeForce NOW Web Play"
        override val statusLine = "REGION: $regionCode  •  LAYOUT: $profileName"
        override val badgeTag = if (isOverlayLive) "HUD LIVE" else "CLOUD PLAY"
        override val ambientTopColor = Color(0xFF3E4A16) // Warm olive-amber glow like Steam Big Picture hero
        override val coverGradient = listOf(Color(0xFF283A10), Color(0xFF112016), Color(0xFF0A1210))
        override val accentColor = SteamPlayGreen
        override val icon = Icons.Default.CloudQueue
        override val primaryActionLabel = "PLAY NOW"
    }

    data class PinnedGameItem(
        val game: PinnedGame,
        val paletteIndex: Int
    ) : SteamShelfItem {
        override val key = "shelf_pin_${game.id}"
        override val title = game.title
        override val statusLine = "PINNED GAME  •  REGION: ${game.regionPreset.shortCode}"
        override val badgeTag = "GFN PIN"
        override val ambientTopColor = PIN_AMBIENT_PALETTE[paletteIndex % PIN_AMBIENT_PALETTE.size]
        override val coverGradient = PIN_COVER_GRADIENTS[paletteIndex % PIN_COVER_GRADIENTS.size]
        override val accentColor = PIN_ACCENT_COLORS[paletteIndex % PIN_ACCENT_COLORS.size]
        override val icon = Icons.Default.SportsEsports
        override val primaryActionLabel = "LAUNCH"
    }

    data class ProfileLayoutItem(
        val profile: ControllerProfile,
        val isActive: Boolean
    ) : SteamShelfItem {
        override val key = "shelf_profile_${profile.id}"
        override val title = profile.name
        override val statusLine = "${profile.controls.size} MAPPED CONTROLS  •  ${if (isActive) "ACTIVE LAYOUT" else profile.gameTitle.ifBlank { "CONTROLLER PROFILE" }}"
        override val badgeTag = if (isActive) "ACTIVE" else "LAYOUT"
        override val ambientTopColor = if (isActive) Color(0xFF163A54) else Color(0xFF2B2246)
        override val coverGradient = if (isActive) {
            listOf(Color(0xFF14344D), Color(0xFF0E1F31), Color(0xFF0B111A))
        } else {
            listOf(Color(0xFF2B204A), Color(0xFF19152B), Color(0xFF0D0E16))
        }
        override val accentColor = if (isActive) SteamFocusCyan else Color(0xFFB388FF)
        override val icon = Icons.Default.Gamepad
        override val primaryActionLabel = if (isActive) "EDIT LAYOUT" else "ACTIVATE"
    }

    data object SteamStoreItem : SteamShelfItem {
        override val key = "shelf_steam_store"
        override val title = "Steam Store"
        override val statusLine = "BROWSE STEAM GAMES  •  GREAT ON DECK & GFN"
        override val badgeTag = "STORE"
        override val ambientTopColor = Color(0xFF123656)
        override val coverGradient = listOf(Color(0xFF1B4B75), Color(0xFF122842), Color(0xFF0B1422))
        override val accentColor = SteamActionBlue
        override val icon = Icons.Default.Storefront
        override val primaryActionLabel = "OPEN STORE"
    }
}

private val PIN_AMBIENT_PALETTE = listOf(
    Color(0xFF4F3814),
    Color(0xFF471B2A),
    Color(0xFF143A4A),
    Color(0xFF33214E),
    Color(0xFF1B442A)
)

private val PIN_COVER_GRADIENTS = listOf(
    listOf(Color(0xFF5A3D12), Color(0xFF2B1D0D), Color(0xFF12100E)),
    listOf(Color(0xFF581828), Color(0xFF2A101B), Color(0xFF120C12)),
    listOf(Color(0xFF12465A), Color(0xFF0E2534), Color(0xFF0A1118)),
    listOf(Color(0xFF3D235C), Color(0xFF1F1432), Color(0xFF0F0D18)),
    listOf(Color(0xFF1A4F2B), Color(0xFF102A1B), Color(0xFF0A1310))
)

private val PIN_ACCENT_COLORS = listOf(
    Color(0xFFF5B731),
    Color(0xFFFF5C8A),
    Color(0xFF4FC3F7),
    Color(0xFFC084FC),
    Color(0xFF59BF40)
)

private data class SteamNewsCardData(
    val id: String,
    val headerTag: String,
    val title: String,
    val subtitle: String,
    val footerText: String,
    val gradient: List<Color>,
    val accent: Color,
    val icon: ImageVector,
    val imageBitmap: ImageBitmap? = null,
    val onClick: () -> Unit
)

/**
 * Faithful Steam Big Picture Mode / SteamOS Gaming Mode Home Screen replica:
 * - Dynamic ambient top glow synced to the currently focused carousel tile
 * - Horizontal shelf where focused item expands into a wide 16:9 hero banner with cyan focus edge
 *   and neighbors appear as 2:3 vertical Steam cover capsules
 * - Centered 3-pill SteamOS tab bar (WHAT'S NEW | COMMUNITY [count] | DECK TOOLS)
 * - Horizontal NEWS / IN-GAME EVENT card strip wired to real XenosPad features
 */
@Composable
fun SteamBigPictureHomeScreen(
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
    steamSelectTrigger: Int,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onLaunchWebPlayer: () -> Unit,
    onLaunchPinnedGame: (PinnedGame) -> Unit,
    onSavePinnedGame: (PinnedGame) -> Unit,
    onDeletePinnedGame: (String) -> Unit,
    onToggleOverlay: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenStore: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSandbox: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuickAccess: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shelfItems = remember(
        webPlayerConfig.regionPreset,
        isOverlayRunning,
        activeProfile?.id,
        activeProfile?.name,
        pinnedGames,
        allProfiles
    ) {
        buildList<SteamShelfItem> {
            add(
                SteamShelfItem.WebPlayHero(
                    regionCode = webPlayerConfig.regionPreset.shortCode,
                    isOverlayLive = isOverlayRunning,
                    profileName = activeProfile?.name ?: "Default"
                )
            )
            pinnedGames.forEachIndexed { idx, pin ->
                add(SteamShelfItem.PinnedGameItem(pin, idx))
            }
            allProfiles.take(6).forEach { profile ->
                add(SteamShelfItem.ProfileLayoutItem(profile, isActive = profile.id == activeProfile?.id))
            }
            add(SteamShelfItem.SteamStoreItem)
        }
    }

    var focusedIndex by remember { mutableIntStateOf(0) }
    val safeFocusedIndex = focusedIndex.coerceIn(0, (shelfItems.lastIndex).coerceAtLeast(0))
    val focusedItem = shelfItems[safeFocusedIndex]

    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0 = WHAT'S NEW, 1 = COMMUNITY, 2 = DECK TOOLS
    var showAddPinDialog by remember { mutableStateOf(false) }

    val ambientColor by animateColorAsState(
        targetValue = focusedItem.ambientTopColor,
        animationSpec = tween(durationMillis = 420),
        label = "steamAmbientBackdrop"
    )

    val triggerFocusedItemAction: () -> Unit = {
        when (val item = focusedItem) {
            is SteamShelfItem.WebPlayHero -> onLaunchWebPlayer()
            is SteamShelfItem.PinnedGameItem -> onLaunchPinnedGame(item.game)
            is SteamShelfItem.ProfileLayoutItem -> {
                if (item.isActive) {
                    onOpenEditor(item.profile.id)
                } else {
                    onSelectProfile(item.profile.id)
                }
            }
            is SteamShelfItem.SteamStoreItem -> onOpenStore()
        }
    }

    LaunchedEffect(steamSelectTrigger) {
        if (steamSelectTrigger > 0) {
            triggerFocusedItemAction()
        }
    }

    // Build bottom shelf cards per tab
    val communityCount = workshopThemes.size + communityPosts.size
    val bottomCards = remember(
        selectedBottomTab,
        isOverlayRunning,
        filterState.activeSlot,
        filterState.isEnabled,
        updateState,
        readiness,
        activeProfile?.id,
        activeProfile?.name,
        workshopThemes,
        communityPosts,
        pinnedGames.size
    ) {
        when (selectedBottomTab) {
            // WHAT'S NEW
            0 -> buildList {
                if (updateState is UpdateUiState.UpdateAvailable) {
                    add(
                        SteamNewsCardData(
                            id = "news_update",
                            headerTag = "SYSTEM UPDATE",
                            title = "XenosPad ${updateState.release.tagName} Available",
                            subtitle = "Tap to download the latest build directly",
                            footerText = "UPDATE READY • TAP TO DOWNLOAD",
                            gradient = listOf(Color(0xFF174A2C), Color(0xFF0E261B)),
                            accent = SteamPlayGreen,
                            icon = Icons.Default.Download,
                            onClick = onDownloadUpdate
                        )
                    )
                } else if (updateState is UpdateUiState.ReadyToInstall) {
                    add(
                        SteamNewsCardData(
                            id = "news_install",
                            headerTag = "SYSTEM UPDATE",
                            title = "Install Update Now",
                            subtitle = "APK downloaded & verified",
                            footerText = "READY TO INSTALL",
                            gradient = listOf(Color(0xFF1A572A), Color(0xFF0F2C1B)),
                            accent = SteamPlayGreen,
                            icon = Icons.Default.Download,
                            onClick = onInstallUpdate
                        )
                    )
                }

                add(
                    SteamNewsCardData(
                        id = "news_overlay",
                        headerTag = "OVERLAY HUD",
                        title = if (isOverlayRunning) "Virtual Pad HUD Running" else "Start Overlay Controller",
                        subtitle = activeProfile?.name ?: "Touch gamepad & mouse overlay",
                        footerText = if (isOverlayRunning) "LIVE • TAP TO STOP" else "READY • TAP TO LAUNCH",
                        gradient = if (isOverlayRunning) {
                            listOf(Color(0xFF154E2A), Color(0xFF0E2118))
                        } else {
                            listOf(Color(0xFF15385A), Color(0xFF0E1D2F))
                        },
                        accent = if (isOverlayRunning) SteamPlayGreen else SteamActionBlue,
                        icon = if (isOverlayRunning) Icons.Default.Stop else Icons.Default.Gamepad,
                        onClick = onToggleOverlay
                    )
                )

                add(
                    SteamNewsCardData(
                        id = "news_store",
                        headerTag = "STEAM STOREFRONT",
                        title = "Browse Great on Deck",
                        subtitle = "Search Steam titles playable on GeForce NOW",
                        footerText = "LIVE STORE • STEAM & GFN",
                        gradient = listOf(Color(0xFF173D63), Color(0xFF0E1E33)),
                        accent = SteamFocusCyan,
                        icon = Icons.Default.Storefront,
                        onClick = onOpenStore
                    )
                )

                add(
                    SteamNewsCardData(
                        id = "news_filter",
                        headerTag = "GAME FILTER",
                        title = if (filterState.isEnabled) "Filter Style ${filterState.activeSlot} Active" else "GPU Filters & Crosshair",
                        subtitle = "Freestyle color tuning & custom reticles",
                        footerText = if (filterState.isEnabled) "ACTIVE • TAP TO TUNE" else "TAP TO CONFIGURE",
                        gradient = listOf(Color(0xFF42225A), Color(0xFF1B122A)),
                        accent = Color(0xFFC084FC),
                        icon = Icons.Default.Tune,
                        onClick = onOpenFilters
                    )
                )

                communityPosts.take(3).forEach { post ->
                    val bmp = ButtonImageCodec.decodeBase64ToImageBitmap(post.imageBase64)
                    add(
                        SteamNewsCardData(
                            id = "news_post_${post.id}",
                            headerTag = post.topic.uppercase(),
                            title = post.title,
                            subtitle = post.body,
                            footerText = "POSTED BY ${post.authorName.uppercase()}",
                            gradient = listOf(Color(0xFF2A2D3A), Color(0xFF151822)),
                            accent = SteamFocusCyan,
                            icon = Icons.Default.Forum,
                            imageBitmap = bmp,
                            onClick = onOpenWorkshop
                        )
                    )
                }
            }

            // COMMUNITY & WORKSHOP
            1 -> buildList {
                add(
                    SteamNewsCardData(
                        id = "comm_hub",
                        headerTag = "WORKSHOP",
                        title = "Button Theme Workshop",
                        subtitle = "Browse, like & download creator button skins",
                        footerText = "${workshopThemes.size} THEMES • OPEN WORKSHOP",
                        gradient = listOf(Color(0xFF1B3A5E), Color(0xFF101D30)),
                        accent = SteamFocusCyan,
                        icon = Icons.Default.Palette,
                        onClick = onOpenWorkshop
                    )
                )
                workshopThemes.take(4).forEach { theme ->
                    add(
                        SteamNewsCardData(
                            id = "comm_theme_${theme.id}",
                            headerTag = "BUTTON THEME",
                            title = theme.name,
                            subtitle = "${theme.gameTag} • ${theme.buttonStyles.size} styled buttons",
                            footerText = "BY ${theme.authorName.uppercase()} • ♥ ${theme.likesCount}",
                            gradient = listOf(Color(0xFF243044), Color(0xFF131A26)),
                            accent = SteamPlayGreen,
                            icon = Icons.Default.Palette,
                            onClick = onOpenWorkshop
                        )
                    )
                }
                communityPosts.take(4).forEach { post ->
                    val bmp = ButtonImageCodec.decodeBase64ToImageBitmap(post.imageBase64)
                    add(
                        SteamNewsCardData(
                            id = "comm_post_${post.id}",
                            headerTag = "DISCUSSION",
                            title = post.title,
                            subtitle = post.body,
                            footerText = "${post.authorName.uppercase()} • ${post.commentsCount} REPLIES",
                            gradient = listOf(Color(0xFF282B38), Color(0xFF141821)),
                            accent = SteamActionBlue,
                            icon = Icons.Default.Forum,
                            imageBitmap = bmp,
                            onClick = onOpenWorkshop
                        )
                    )
                }
            }

            // DECK TOOLS (RECOMMENDED)
            else -> listOf(
                SteamNewsCardData(
                    id = "tool_editor",
                    headerTag = "LAYOUT STUDIO",
                    title = "Customize Controls",
                    subtitle = activeProfile?.name ?: "Drag, resize & theme every button",
                    footerText = "${activeProfile?.controls?.size ?: 0} CONTROLS • OPEN STUDIO",
                    gradient = listOf(Color(0xFF183C5C), Color(0xFF0E1D2F)),
                    accent = SteamFocusCyan,
                    icon = Icons.Default.Edit,
                    onClick = { activeProfile?.let { onOpenEditor(it.id) } }
                ),
                SteamNewsCardData(
                    id = "tool_gallery",
                    headerTag = "MEDIA CAPTURE",
                    title = "Clips & Instant Replay",
                    subtitle = "Watch recorded clips & post highlights to profile",
                    footerText = "GALLERY • SCREEN RECORDER",
                    gradient = listOf(Color(0xFF471E35), Color(0xFF1F101B)),
                    accent = Color(0xFFFF6B9A),
                    icon = Icons.Default.PhotoLibrary,
                    onClick = onOpenGallery
                ),
                SteamNewsCardData(
                    id = "tool_sandbox",
                    headerTag = "INPUT TESTER",
                    title = "Controller Test Arena",
                    subtitle = "Verify scan codes, sticks & mouse flicks live",
                    footerText = "SANDBOX • ZERO LATENCY",
                    gradient = listOf(Color(0xFF1B4438), Color(0xFF0E211C)),
                    accent = SteamPlayGreen,
                    icon = Icons.Default.TouchApp,
                    onClick = onOpenSandbox
                ),
                SteamNewsCardData(
                    id = "tool_readiness",
                    headerTag = "SYSTEM STATUS",
                    title = if (readiness.canDrawOverlays) "Overlay Ready" else "Grant Overlay Access",
                    subtitle = if (readiness.canDrawOverlays) "Quick Access & permissions configured" else "Enable Draw Over Apps for floating pad",
                    footerText = if (readiness.canDrawOverlays) "ALL SYSTEMS READY" else "TAP TO GRANT",
                    gradient = listOf(Color(0xFF3E3114), Color(0xFF1E190E)),
                    accent = Color(0xFFF5B731),
                    icon = Icons.Default.Layers,
                    onClick = {
                        if (readiness.canDrawOverlays) onOpenQuickAccess() else onRequestOverlayPermission()
                    }
                ),
                SteamNewsCardData(
                    id = "tool_settings",
                    headerTag = "SYSTEM SETTINGS",
                    title = "Updates & Preferences",
                    subtitle = "Check GitHub updates, gyro, themes & cloud sync",
                    footerText = "SETTINGS • BETA THEMES",
                    gradient = listOf(Color(0xFF252D3D), Color(0xFF131822)),
                    accent = SteamFocusCyan,
                    icon = Icons.Default.Settings,
                    onClick = onOpenSettings
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SteamCanvasBg)
            .testTag("steam_big_picture_home")
    ) {
        // Authentic Steam Big Picture top ambient glow synced to the focused tile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ambientColor.copy(alpha = 0.72f),
                            ambientColor.copy(alpha = 0.34f),
                            SteamCanvasBg
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. ICONIC STEAM BIG PICTURE HORIZONTAL CAROUSEL
            // Focused item = wide 16:9 hero banner with cyan focus bar & controller icon
            // Neighbors = tall 2:3 vertical Steam cover posters
            item(key = "steam_hero_carousel") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        itemsIndexed(shelfItems, key = { _, item -> item.key }) { idx, item ->
                            val isFocused = idx == safeFocusedIndex
                            SteamCarouselCapsule(
                                item = item,
                                isFocused = isFocused,
                                onClick = {
                                    if (isFocused) {
                                        triggerFocusedItemAction()
                                    } else {
                                        focusedIndex = idx
                                    }
                                }
                            )
                        }

                        // "+ Pin Game" poster at the end of the shelf
                        item(key = "shelf_add_pin") {
                            Box(
                                modifier = Modifier
                                    .width(102.dp)
                                    .height(148.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF1B222D))
                                    .border(1.dp, Color(0xFF364459), RoundedCornerShape(3.dp))
                                    .clickable { showAddPinDialog = true }
                                    .testTag("steam_carousel_add_pin"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF293548)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Pin Game", tint = SteamFocusCyan, modifier = Modifier.size(20.dp))
                                    }
                                    Text(
                                        text = "PIN GAME",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB5C4D6)
                                    )
                                }
                            }
                        }
                    }

                    // Focused item title & Playtime/Status caption directly below the wide hero card (exact Steam layout)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = focusedItem.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = Color(0xFFC8C5B0),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = focusedItem.statusLine,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = Color(0xFFC8C5B0),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (focusedItem is SteamShelfItem.PinnedGameItem) {
                                TextButton(
                                    onClick = {
                                        onDeletePinnedGame(focusedItem.game.id)
                                        focusedIndex = 0
                                    }
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Unpin", tint = Color(0xFFE07070), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Compact Steam-styled action row for the focused item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = triggerFocusedItemAction,
                                shape = RoundedCornerShape(3.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SteamPlayGreen),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(40.dp)
                                    .testTag("steam_focused_primary_action")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(focusedItem.primaryActionLabel, color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = onToggleOverlay,
                                shape = RoundedCornerShape(3.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOverlayRunning) Color(0xFF3A2428) else Color(0xFF233042)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("steam_home_overlay_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isOverlayRunning) Icons.Default.Stop else Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = if (isOverlayRunning) Color(0xFFFF7A8A) else SteamFocusCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (isOverlayRunning) "STOP HUD" else "OVERLAY",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenQuickAccess,
                                shape = RoundedCornerShape(3.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text("···", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 2. CENTERED 3-PILL STEAM BIG PICTURE TAB BAR
            // Exact visual match to: [ WHAT'S NEW ]   COMMUNITY [👥 N]   DECK TOOLS
            item(key = "steam_center_tabs") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SteamCenterTabPill(
                        label = "WHAT'S NEW",
                        selected = selectedBottomTab == 0,
                        onClick = { selectedBottomTab = 0 },
                        testTag = "steam_tab_whats_new"
                    )
                    Spacer(Modifier.width(8.dp))
                    SteamCenterTabPill(
                        label = "COMMUNITY",
                        selected = selectedBottomTab == 1,
                        badgeCount = communityCount,
                        onClick = { selectedBottomTab = 1 },
                        testTag = "steam_tab_community"
                    )
                    Spacer(Modifier.width(8.dp))
                    SteamCenterTabPill(
                        label = "DECK TOOLS",
                        selected = selectedBottomTab == 2,
                        onClick = { selectedBottomTab = 2 },
                        testTag = "steam_tab_deck_tools"
                    )
                }
            }

            // 3. BOTTOM HORIZONTAL "NEWS / IN-GAME EVENT" CARD STRIP
            item(key = "steam_bottom_news_strip") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(bottomCards, key = { it.id }) { card ->
                        SteamNewsEventCard(card = card)
                    }
                }
            }
        }
    }

    if (showAddPinDialog) {
        SteamQuickPinGameDialog(
            defaultRegion = webPlayerConfig.regionPreset,
            onDismiss = { showAddPinDialog = false },
            onSave = { title, region ->
                onSavePinnedGame(
                    PinnedGame(
                        id = UUID.randomUUID().toString(),
                        title = title.trim(),
                        regionPreset = region,
                        profileId = activeProfile?.id ?: allProfiles.firstOrNull()?.id.orEmpty()
                    )
                )
                showAddPinDialog = false
            }
        )
    }
}

@Composable
private fun SteamCarouselCapsule(
    item: SteamShelfItem,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val cardWidth by animateDpAsState(
        targetValue = if (isFocused) 262.dp else 104.dp,
        animationSpec = tween(durationMillis = 260),
        label = "steamCapsuleWidth"
    )
    val cardHeight = 148.dp

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.linearGradient(item.coverGradient))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) Color(0x88FFFFFF) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(3.dp)
            )
            .clickable(onClick = onClick)
            .testTag("steam_carousel_item_${item.key}")
    ) {
        // Use hero artwork backdrop for GeForce NOW main tile so it looks rich like game cover art
        if (item is SteamShelfItem.WebPlayHero) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_cockpit),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                alpha = 0.55f,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xCC0D140E), Color(0x66192A14), Color(0x330D140E))
                        )
                    )
            )
        } else {
            // Subtle glossy diagonal sheen like real Steam game cover art
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                item.accentColor.copy(alpha = 0.28f),
                                Color.Transparent,
                                Color(0xCC06090F)
                            )
                        )
                    )
            )
        }

        if (isFocused) {
            // Wide 16:9 Steam Hero Poster layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xBB0D1218),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.border(1.dp, item.accentColor.copy(alpha = 0.65f), RoundedCornerShape(3.dp))
                    ) {
                        Text(
                            text = item.badgeTag,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = item.accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.accentColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = item.title.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    letterSpacing = 0.4.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 32.dp)
                )
            }

            // Bottom-right crisp white controller icon badge (exact match to screenshot!)
            Surface(
                color = Color(0xCC0B0F14),
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = "Controller Supported",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                        .size(15.dp)
                )
            }

            // Signature glowing Steam Big Picture cyan focus edge along the bottom!
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(SteamActionBlue, SteamFocusCyan, Color.White, SteamFocusCyan)
                        )
                    )
            )
        } else {
            // Unfocused tall 2:3 vertical Steam cover capsule
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(9.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color(0xAA0C1016),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = item.badgeTag,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = item.accentColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(item.accentColor.copy(alpha = 0.22f))
                        .border(1.dp, item.accentColor.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SteamCenterTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    badgeCount: Int? = null
) {
    Surface(
        color = if (selected) Color(0xFF3E4552) else Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else Color(0xFFB5BFC9),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp
            )
            if (badgeCount != null) {
                Surface(
                    color = SteamFriendsGreen,
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "$badgeCount",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SteamNewsEventCard(card: SteamNewsCardData) {
    Column(
        modifier = Modifier
            .width(214.dp)
            .clickable(onClick = card.onClick)
            .testTag("steam_news_card_${card.id}"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Iconic tiny uppercase header ABOVE the Steam card (e.g., NEWS / IN-GAME EVENT)
        Text(
            text = card.headerTag,
            color = Color(0xFFB8C2CE),
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .border(1.dp, Color(0xFF2D3542), RoundedCornerShape(3.dp))
        ) {
            // 16:9 visual card body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(114.dp)
                    .background(Brush.linearGradient(card.gradient))
            ) {
                if (card.imageBitmap != null) {
                    Image(
                        bitmap = card.imageBitmap,
                        contentDescription = card.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x220A0E14), Color(0xDD0A0E14))
                                )
                            )
                    )
                }

                Icon(
                    imageVector = card.icon,
                    contentDescription = null,
                    tint = card.accent.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(22.dp)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = card.title,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = card.subtitle,
                        color = Color(0xFFC7D3E3),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Signature dark SteamOS metadata footer bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SteamCardFooterBg)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    text = card.footerText,
                    color = Color(0xFF8F98A6),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SteamQuickPinGameDialog(
    defaultRegion: GfnRegionPreset,
    onDismiss: () -> Unit,
    onSave: (String, GfnRegionPreset) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var region by remember { mutableStateOf(defaultRegion) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pin Game to Steam Shelf") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 42) title = it },
                    label = { Text("Game Title (e.g. Dead by Daylight)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("GeForce NOW Region", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GfnRegionPreset.entries.take(6)) { preset ->
                        FilterChip(
                            selected = region == preset,
                            onClick = { region = preset },
                            label = { Text(preset.shortCode, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, region) },
                enabled = title.trim().isNotEmpty()
            ) {
                Text("Pin Game")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.cloud.CloudUserInfo
import com.example.model.ButtonThemePreset
import com.example.model.CommunityTopicPost
import com.example.model.CommunityUserProfile
import com.example.model.ControllerProfile
import com.example.model.GameFilterState
import com.example.model.GfnRegionPreset
import com.example.model.PinnedGame
import com.example.model.WebStreamQualityConfig
import com.example.ui.SystemReadiness
import com.example.ui.components.EpicBlueAccent
import com.example.ui.components.EpicBorderColor
import com.example.ui.components.EpicCanvasBg
import com.example.ui.components.EpicMutedText
import com.example.ui.components.EpicOnlineGreen
import com.example.ui.components.EpicSocialPanelCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.update.UpdateUiState
import com.example.util.ButtonImageCodec
import java.util.UUID

private data class EpicQuickTileData(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String?,
    val gradient: List<Color>,
    val accent: Color,
    val icon: ImageVector,
    val useHeroArtwork: Boolean = false,
    val onClick: () -> Unit
)

private data class EpicEditorialItem(
    val id: String,
    val metaTag: String,
    val title: String,
    val summary: String,
    val ctaLabel: String,
    val gradient: List<Color>,
    val accent: Color,
    val icon: ImageVector,
    val imageBitmap: ImageBitmap? = null,
    val starred: Boolean = false,
    val onClick: () -> Unit
)

private val EPIC_QUICK_GRADIENTS = listOf(
    listOf(Color(0xFF1D4E89), Color(0xFF0E2240)),
    listOf(Color(0xFF6B1D7B), Color(0xFF281036)),
    listOf(Color(0xFF6E1418), Color(0xFF1F0A0C)),
    listOf(Color(0xFF8C5311), Color(0xFF2B1A09))
)

private val EPIC_QUICK_ACCENTS = listOf(
    Color(0xFF38BDF8),
    Color(0xFFE879F9),
    Color(0xFFFF5252),
    Color(0xFFF5B731)
)

/**
 * Faithful Epic Games Launcher Home Screen replica matching the user's reference screenshot:
 * - Pure matte charcoal canvas (#121212)
 * - Top "Quick Launch" horizontal strip of sleek 16:9 game & launch tiles
 * - "News" section with 2 prominent featured editorial cards (thumbnail + timestamp + copy + underlined link)
 *   followed by horizontal editorial rows (thumbnail left, starred meta tag + summary + underlined link right)
 * - Persistent Right-Hand Social Panel on tablets/landscape (and accessible from top bar on compact phones)
 */
@Composable
fun EpicGamesHomeScreen(
    cloudUser: CloudUserInfo?,
    myProfile: CommunityUserProfile?,
    displayName: String,
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
    onSignIn: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onLaunchWebPlayer: () -> Unit,
    onLaunchPinnedGame: (PinnedGame) -> Unit,
    onSavePinnedGame: (PinnedGame) -> Unit,
    onToggleOverlay: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenStore: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenMyProfile: () -> Unit,
    onOpenPublicProfile: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSandbox: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPinGameDialog by remember { mutableStateOf(false) }

    val quickLaunchTiles = remember(
        webPlayerConfig.regionPreset,
        isOverlayRunning,
        activeProfile?.id,
        activeProfile?.name,
        pinnedGames
    ) {
        buildList {
            add(
                EpicQuickTileData(
                    id = "epic_ql_gfn",
                    title = "GEFORCE NOW",
                    subtitle = "Region: ${webPlayerConfig.regionPreset.shortCode}",
                    badge = "PLAY",
                    gradient = listOf(Color(0xFF1A4B7C), Color(0xFF0E1D34)),
                    accent = EpicBlueAccent,
                    icon = Icons.Default.CloudQueue,
                    useHeroArtwork = true,
                    onClick = onLaunchWebPlayer
                )
            )
            add(
                EpicQuickTileData(
                    id = "epic_ql_overlay",
                    title = if (isOverlayRunning) "HUD ACTIVE" else "OVERLAY PAD",
                    subtitle = activeProfile?.name ?: "Controller HUD",
                    badge = if (isOverlayRunning) "LIVE" else "START",
                    gradient = if (isOverlayRunning) {
                        listOf(Color(0xFF19522B), Color(0xFF0E2316))
                    } else {
                        listOf(Color(0xFF4A1B66), Color(0xFF1D0E2B))
                    },
                    accent = if (isOverlayRunning) EpicOnlineGreen else Color(0xFFD084FF),
                    icon = if (isOverlayRunning) Icons.Default.Stop else Icons.Default.Gamepad,
                    onClick = onToggleOverlay
                )
            )
            pinnedGames.forEachIndexed { idx, pin ->
                add(
                    EpicQuickTileData(
                        id = "epic_ql_pin_${pin.id}",
                        title = pin.title.uppercase(),
                        subtitle = "GFN • ${pin.regionPreset.shortCode}",
                        badge = "PINNED",
                        gradient = EPIC_QUICK_GRADIENTS[idx % EPIC_QUICK_GRADIENTS.size],
                        accent = EPIC_QUICK_ACCENTS[idx % EPIC_QUICK_ACCENTS.size],
                        icon = Icons.Default.SportsEsports,
                        onClick = { onLaunchPinnedGame(pin) }
                    )
                )
            }
            add(
                EpicQuickTileData(
                    id = "epic_ql_store",
                    title = "EPIC STORE",
                    subtitle = "Free Games & Deals",
                    badge = "STORE",
                    gradient = listOf(Color(0xFF0F3D6E), Color(0xFF0C1A2B)),
                    accent = EpicBlueAccent,
                    icon = Icons.Default.Storefront,
                    onClick = onOpenStore
                )
            )
            add(
                EpicQuickTileData(
                    id = "epic_ql_studio",
                    title = "LAYOUT STUDIO",
                    subtitle = "${activeProfile?.controls?.size ?: 0} mapped controls",
                    badge = "EDIT",
                    gradient = listOf(Color(0xFF4F2B12), Color(0xFF1E130B)),
                    accent = Color(0xFFF5B731),
                    icon = Icons.Default.Edit,
                    onClick = { activeProfile?.let { onOpenEditor(it.id) } }
                )
            )
        }
    }

    // Two large featured editorial cards matching the top 2-column News section of the screenshot
    val featuredNews = remember(
        updateState,
        activeProfile?.id,
        activeProfile?.name,
        isOverlayRunning,
        communityPosts
    ) {
        val list = mutableListOf<EpicEditorialItem>()

        if (updateState is UpdateUiState.UpdateAvailable) {
            list.add(
                EpicEditorialItem(
                    id = "epic_feat_update",
                    metaTag = "UPDATE AVAILABLE • ${updateState.release.tagName}",
                    title = "XenosPad Update Ready",
                    summary = "A newer XenosPad release is available on GitHub. Download and install the latest improvements directly.",
                    ctaLabel = "Download now",
                    gradient = listOf(Color(0xFF145232), Color(0xFF0C2217)),
                    accent = EpicOnlineGreen,
                    icon = Icons.Default.Download,
                    onClick = onDownloadUpdate
                )
            )
        } else if (updateState is UpdateUiState.ReadyToInstall) {
            list.add(
                EpicEditorialItem(
                    id = "epic_feat_install",
                    metaTag = "DOWNLOAD COMPLETE",
                    title = "Install XenosPad Update",
                    summary = "Your downloaded update APK is verified and ready to install.",
                    ctaLabel = "Install now",
                    gradient = listOf(Color(0xFF145232), Color(0xFF0C2217)),
                    accent = EpicOnlineGreen,
                    icon = Icons.Default.Download,
                    onClick = onInstallUpdate
                )
            )
        }

        list.add(
            EpicEditorialItem(
                id = "epic_feat_store",
                metaTag = "EPIC GAMES STORE • GEFORCE NOW",
                title = "Browse Free Games & Top Titles",
                summary = "Explore the live Epic Games Store directly inside XenosPad and launch your cloud library on GeForce NOW.",
                ctaLabel = "Browse Store",
                gradient = listOf(Color(0xFF15457A), Color(0xFF0D213B), Color(0xFF0A1424)),
                accent = Color(0xFF38BDF8),
                icon = Icons.Default.Storefront,
                onClick = onOpenStore
            )
        )

        val topPost = communityPosts.firstOrNull()
        if (topPost != null) {
            list.add(
                EpicEditorialItem(
                    id = "epic_feat_post_${topPost.id}",
                    metaTag = "${topPost.topic.uppercase()} • BY ${topPost.authorName.uppercase()}",
                    title = topPost.title,
                    summary = topPost.body,
                    ctaLabel = "Read more",
                    gradient = listOf(Color(0xFF3D214F), Color(0xFF1B1226)),
                    accent = Color(0xFFC084FC),
                    icon = Icons.Default.Forum,
                    imageBitmap = ButtonImageCodec.decodeBase64ToImageBitmap(topPost.imageBase64),
                    onClick = onOpenWorkshop
                )
            )
        } else {
            list.add(
                EpicEditorialItem(
                    id = "epic_feat_workshop",
                    metaTag = "BUTTON THEME WORKSHOP",
                    title = "Customize Every Control Skin",
                    summary = "Create, test, and publish custom button icon packs or download creator themes for Dead by Daylight and more.",
                    ctaLabel = "Open Workshop",
                    gradient = listOf(Color(0xFF47251A), Color(0xFF1F120E)),
                    accent = Color(0xFFF5B731),
                    icon = Icons.Default.Palette,
                    onClick = onOpenWorkshop
                )
            )
        }

        list.take(2)
    }

    // Stacked horizontal feed rows matching the Kena row at the bottom of the screenshot
    val feedRows = remember(
        readiness,
        filterState.activeSlot,
        filterState.isEnabled,
        activeProfile?.id,
        activeProfile?.name,
        communityPosts,
        workshopThemes
    ) {
        buildList {
            if (!readiness.canDrawOverlays) {
                add(
                    EpicEditorialItem(
                        id = "epic_row_perm",
                        metaTag = "SYSTEM SETUP REQUIRED",
                        title = "Enable Draw Over Apps",
                        summary = "Grant overlay permission so XenosPad can render your virtual controller and crosshair over GeForce NOW.",
                        ctaLabel = "Grant permission",
                        gradient = listOf(Color(0xFF5A3812), Color(0xFF26190B)),
                        accent = Color(0xFFF5B731),
                        icon = Icons.Default.Layers,
                        starred = true,
                        onClick = onRequestOverlayPermission
                    )
                )
            }
            add(
                EpicEditorialItem(
                    id = "epic_row_filters",
                    metaTag = if (filterState.isEnabled) "ACTIVE • FILTER SLOT ${filterState.activeSlot}" else "VISUALS & CROSSHAIR",
                    title = "GPU Game Filters & Custom Crosshair",
                    summary = "Boost shadow visibility, color vibrance, and overlay custom reticles with zero-lag GPU rendering.",
                    ctaLabel = "Configure",
                    gradient = listOf(Color(0xFF23395D), Color(0xFF101B2E)),
                    accent = Color(0xFF38BDF8),
                    icon = Icons.Default.Tune,
                    starred = true,
                    onClick = onOpenFilters
                )
            )
            add(
                EpicEditorialItem(
                    id = "epic_row_gallery",
                    metaTag = "INSTANT REPLAY & CLIPS",
                    title = "Gameplay Clips & Highlights",
                    summary = "Review recorded gameplay clips and screenshots in your Gallery, then publish highlights directly to your Epic Profile.",
                    ctaLabel = "Open Gallery",
                    gradient = listOf(Color(0xFF4A1E34), Color(0xFF1E0F18)),
                    accent = Color(0xFFFF6B9A),
                    icon = Icons.Default.PhotoLibrary,
                    starred = false,
                    onClick = onOpenGallery
                )
            )
            add(
                EpicEditorialItem(
                    id = "epic_row_sandbox",
                    metaTag = "CONTROLLER SANDBOX",
                    title = "Test Sticks, Macros & Touchpad Aim",
                    summary = "Verify evdev scan codes, toggle latches, and drag-to-aim responsiveness before launching a match.",
                    ctaLabel = "Open Sandbox",
                    gradient = listOf(Color(0xFF184A39), Color(0xFF0E231C)),
                    accent = EpicOnlineGreen,
                    icon = Icons.Default.TouchApp,
                    starred = false,
                    onClick = onOpenSandbox
                )
            )
            communityPosts.drop(1).take(3).forEach { post ->
                add(
                    EpicEditorialItem(
                        id = "epic_row_post_${post.id}",
                        metaTag = "${post.topic.uppercase()} • ${post.authorName.uppercase()}",
                        title = post.title,
                        summary = post.body,
                        ctaLabel = "Read more",
                        gradient = listOf(Color(0xFF282832), Color(0xFF16161C)),
                        accent = EpicBlueAccent,
                        icon = Icons.Default.Forum,
                        imageBitmap = ButtonImageCodec.decodeBase64ToImageBitmap(post.imageBase64),
                        starred = true,
                        onClick = onOpenWorkshop
                    )
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(EpicCanvasBg)
            .testTag("epic_games_home_screen")
    ) {
        val showInlineSocialPanel = maxWidth >= 700.dp
        val showTwoColumnNews = maxWidth >= 520.dp

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Center Main Feed (Quick Launch + News)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. QUICK LAUNCH SECTION
                item(key = "epic_quick_launch") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Quick Launch",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(quickLaunchTiles, key = { it.id }) { tile ->
                                EpicQuickLaunchTile(tile = tile)
                            }
                            item(key = "epic_ql_add_pin") {
                                Box(
                                    modifier = Modifier
                                        .width(126.dp)
                                        .height(94.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1A1A1A))
                                        .border(1.dp, EpicBorderColor, RoundedCornerShape(6.dp))
                                        .clickable { showPinGameDialog = true }
                                        .testTag("epic_quick_launch_add_pin"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF282828)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Pin Game", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            text = "Pin Game",
                                            color = EpicMutedText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. NEWS SECTION — Top 2 Featured Editorial Cards
                item(key = "epic_news_featured") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "News",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (showTwoColumnNews && featuredNews.size >= 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                featuredNews.forEach { item ->
                                    EpicFeaturedNewsCard(
                                        item = item,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(featuredNews, key = { it.id }) { item ->
                                    EpicFeaturedNewsCard(
                                        item = item,
                                        modifier = Modifier.width(268.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. HORIZONTAL DIVIDER + STACKED EDITORIAL FEED ROWS (Kena-style)
                item(key = "epic_news_divider") {
                    HorizontalDivider(color = Color(0xFF242424))
                }

                items(feedRows, key = { it.id }) { row ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        EpicHorizontalNewsRow(item = row)
                        HorizontalDivider(color = Color(0xFF202020))
                    }
                }
            }

            // Right-Hand Persistent Social Panel on tablets / widescreen (exact 3-column Epic layout!)
            if (showInlineSocialPanel) {
                EpicSocialPanelCard(
                    cloudUser = cloudUser,
                    myProfile = myProfile,
                    displayName = displayName,
                    activeProfileName = activeProfile?.name,
                    isOverlayRunning = isOverlayRunning,
                    workshopThemes = workshopThemes,
                    communityPosts = communityPosts,
                    onSignIn = onSignIn,
                    onOpenMyProfile = onOpenMyProfile,
                    onOpenWorkshop = onOpenWorkshop,
                    onOpenPublicProfile = onOpenPublicProfile,
                    modifier = Modifier
                        .width(252.dp)
                        .fillMaxHeight()
                        .padding(top = 12.dp, bottom = 12.dp, end = 12.dp)
                )
            }
        }
    }

    if (showPinGameDialog) {
        EpicQuickPinGameDialog(
            defaultRegion = webPlayerConfig.regionPreset,
            onDismiss = { showPinGameDialog = false },
            onSave = { title, region ->
                onSavePinnedGame(
                    PinnedGame(
                        id = UUID.randomUUID().toString(),
                        title = title.trim(),
                        regionPreset = region,
                        profileId = activeProfile?.id ?: allProfiles.firstOrNull()?.id.orEmpty()
                    )
                )
                showPinGameDialog = false
            }
        )
    }
}

@Composable
private fun EpicQuickLaunchTile(tile: EpicQuickTileData) {
    Box(
        modifier = Modifier
            .width(166.dp)
            .height(94.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.linearGradient(tile.gradient))
            .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(6.dp))
            .clickable(onClick = tile.onClick)
            .testTag(tile.id)
    ) {
        if (tile.useHeroArtwork) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_cockpit),
                contentDescription = tile.title,
                contentScale = ContentScale.Crop,
                alpha = 0.48f,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0xCC0D1420))))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(tile.accent.copy(alpha = 0.22f), Color(0xBB0C0C0E))
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!tile.badge.isNullOrBlank()) {
                    Surface(
                        color = Color(0xBB121212),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = tile.badge,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tile.accent,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                Icon(
                    imageVector = tile.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = tile.title,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tile.subtitle,
                    color = Color(0xFFC4C4CC),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EpicFeaturedNewsCard(
    item: EpicEditorialItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = item.onClick)
            .testTag(item.id),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Large crisp 16:9 visual card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.linearGradient(item.gradient))
                .border(1.dp, Color(0xFF282828), RoundedCornerShape(6.dp))
        ) {
            if (item.imageBitmap != null) {
                Image(
                    bitmap = item.imageBitmap,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0x22000000), Color(0xCC0E0E10))))
                )
            }

            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.accent.copy(alpha = 0.88f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(26.dp)
            )

            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            )
        }

        // Muted uppercase metadata timestamp/category (exact Epic style)
        Text(
            text = item.metaTag,
            color = EpicMutedText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Crisp 2-line white editorial copy
        Text(
            text = item.summary,
            color = Color(0xFFEAEAEA),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Signature Epic underlined "Read more" link
        Text(
            text = item.ctaLabel,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline
        )
    }
}

@Composable
private fun EpicHorizontalNewsRow(item: EpicEditorialItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .testTag(item.id),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left 16:9 thumbnail matching the Kena row in the screenshot
        Box(
            modifier = Modifier
                .width(116.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.linearGradient(item.gradient))
                .border(1.dp, Color(0xFF282828), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageBitmap != null) {
                Image(
                    bitmap = item.imageBitmap,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.accent,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Right text column: ★ META TAG, editorial summary, underlined Read more
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.starred) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = EpicMutedText,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Text(
                    text = item.metaTag,
                    color = EpicMutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${item.title} — ${item.summary}",
                color = Color(0xFFE6E6E6),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.ctaLabel,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

@Composable
private fun EpicQuickPinGameDialog(
    defaultRegion: GfnRegionPreset,
    onDismiss: () -> Unit,
    onSave: (String, GfnRegionPreset) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var region by remember { mutableStateOf(defaultRegion) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Quick Launch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 42) title = it },
                    label = { Text("Game Title (e.g. Fortnite, Rocket League)") },
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
                Text("Add Tile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

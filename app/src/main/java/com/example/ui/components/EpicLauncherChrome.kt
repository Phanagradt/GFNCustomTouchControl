package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NexusRoutes
import com.example.cloud.CloudUserInfo
import com.example.model.ButtonThemePreset
import com.example.model.CommunityTopicPost
import com.example.model.CommunityUserProfile
import com.example.ui.screens.CommunityAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily

val EpicCanvasBg = Color(0xFF121212)
val EpicSidebarBg = Color(0xFF121212)
val EpicCardBg = Color(0xFF1C1C1C)
val EpicPillBg = Color(0xFF2A2A2A)
val EpicBorderColor = Color(0xFF2E2E2E)
val EpicMutedText = Color(0xFF9A9A9A)
val EpicOnlineGreen = Color(0xFF4CAF50)
val EpicBlueAccent = Color(0xFF0078F2)
val EpicBetaAmber = Color(0xFFF5B731)

private val EpicAvatarColors = listOf(
    Color(0xFF4CAF50),
    Color(0xFFE91E63),
    Color(0xFF9C27B0),
    Color(0xFF29B6F6),
    Color(0xFFFF5252),
    Color(0xFFAB47BC),
    Color(0xFF9CCC65),
    Color(0xFF66BB6A)
)

/**
 * Crisp matte-white Epic Games shield mark matching the top-left of the Epic Games Launcher.
 */
@Composable
fun EpicGamesShieldMark(modifier: Modifier = Modifier) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(
            topStart = 3.dp,
            topEnd = 3.dp,
            bottomStart = 8.dp,
            bottomEnd = 8.dp
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "EPIC",
                color = Color(0xFF121212),
                fontWeight = FontWeight.Black,
                fontSize = 8.sp,
                lineHeight = 8.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "GAMES",
                color = Color(0xFF121212),
                fontWeight = FontWeight.Bold,
                fontSize = 5.sp,
                lineHeight = 5.sp,
                letterSpacing = 0.4.sp
            )
        }
    }
}

/**
 * Authentic Epic Games Launcher left navigation rail/drawer matching the reference screenshot:
 * Top: Epic Games logo + BETA badge
 * Nav: Home, Store, Library, Visuals & Filters, Media & Clips, Sandbox
 * Bottom pinned: Workshop/Downloads, Settings, User Profile, Exit Theme
 */
@Composable
fun EpicLauncherSidebarContent(
    currentRoute: String?,
    displayName: String,
    avatarBase64: String?,
    hasUpdateBadge: Boolean,
    isPrimaryAdmin: Boolean,
    onNavigateRoute: (String) -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onExitEpicTheme: () -> Unit,
    onCloseDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(EpicSidebarBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Brand Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EpicGamesShieldMark()
                Text(
                    text = "Epic Games",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Surface(
                    color = EpicBetaAmber.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.border(1.dp, EpicBetaAmber.copy(alpha = 0.65f), RoundedCornerShape(3.dp))
                ) {
                    Text(
                        text = "BETA",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 8.sp,
                        color = EpicBetaAmber,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            if (onCloseDrawer != null) {
                IconButton(onClick = onCloseDrawer, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Sidebar", tint = EpicMutedText)
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // Top Navigation Links
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EpicSidebarNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                selected = currentRoute == NexusRoutes.COCKPIT,
                onClick = {
                    onNavigateRoute(NexusRoutes.COCKPIT)
                    onCloseDrawer?.invoke()
                },
                testTag = "epic_sidebar_home"
            )
            EpicSidebarNavItem(
                icon = Icons.Default.LocalOffer,
                label = "Store",
                selected = currentRoute == NexusRoutes.STORE,
                onClick = {
                    onNavigateRoute(NexusRoutes.STORE)
                    onCloseDrawer?.invoke()
                },
                testTag = "epic_sidebar_store"
            )
            EpicSidebarNavItem(
                icon = Icons.Default.GridView,
                label = "Library",
                selected = currentRoute == NexusRoutes.PROFILES,
                onClick = {
                    onNavigateRoute(NexusRoutes.PROFILES)
                    onCloseDrawer?.invoke()
                },
                testTag = "epic_sidebar_library"
            )
            EpicSidebarNavItem(
                icon = Icons.Default.Tune,
                label = "Visuals & FX",
                selected = currentRoute == NexusRoutes.FILTERS,
                onClick = {
                    onNavigateRoute(NexusRoutes.FILTERS)
                    onCloseDrawer?.invoke()
                },
                testTag = "epic_sidebar_visuals"
            )
            EpicSidebarNavItem(
                icon = Icons.Default.PhotoLibrary,
                label = "Media & Clips",
                selected = currentRoute == NexusRoutes.GALLERY,
                onClick = {
                    onNavigateRoute(NexusRoutes.GALLERY)
                    onCloseDrawer?.invoke()
                },
                testTag = "epic_sidebar_gallery"
            )
            EpicSidebarNavItem(
                icon = Icons.Default.TouchApp,
                label = "Sandbox",
                selected = currentRoute == NexusRoutes.SANDBOX,
                onClick = {
                    onNavigateRoute(NexusRoutes.SANDBOX)
                    onCloseDrawer?.invoke()
                },
                testTag = "epic_sidebar_sandbox"
            )
            if (isPrimaryAdmin) {
                EpicSidebarNavItem(
                    icon = Icons.Default.SportsEsports,
                    label = "Admin Panel",
                    selected = false,
                    accentColor = EpicBetaAmber,
                    onClick = {
                        onCloseDrawer?.invoke()
                        onOpenAdminPanel()
                    },
                    testTag = "epic_sidebar_admin"
                )
            }
        }

        // Bottom Pinned Utility Section (exact Epic Launcher bottom-left pattern: Downloads / Settings / Username)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EpicSidebarNavItem(
                icon = if (hasUpdateBadge) Icons.Default.Download else Icons.Default.Palette,
                label = if (hasUpdateBadge) "Updates Ready" else "Workshop",
                selected = false,
                accentColor = if (hasUpdateBadge) EpicOnlineGreen else null,
                onClick = {
                    onCloseDrawer?.invoke()
                    if (hasUpdateBadge) onNavigateRoute(NexusRoutes.SETUP) else onOpenWorkshop()
                },
                testTag = "epic_sidebar_downloads"
            )
            EpicSidebarNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                selected = currentRoute == NexusRoutes.SETUP,
                onClick = {
                    onNavigateRoute(NexusRoutes.SETUP)
                    onCloseDrawer?.invoke()
                },
                testTag = "epic_sidebar_settings"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        onCloseDrawer?.invoke()
                        onOpenProfile()
                    }
                    .padding(horizontal = 6.dp, vertical = 9.dp)
                    .testTag("epic_sidebar_profile"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CommunityAvatar(
                    avatarBase64 = avatarBase64,
                    username = displayName,
                    size = 22.dp,
                    borderColor = EpicMutedText
                )
                Text(
                    text = displayName,
                    color = Color(0xFFD2D2D2),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            EpicSidebarNavItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = "Default Theme",
                selected = false,
                accentColor = Color(0xFFE07A7A),
                onClick = {
                    onCloseDrawer?.invoke()
                    onExitEpicTheme()
                },
                testTag = "epic_sidebar_exit_theme"
            )
        }
    }
}

@Composable
private fun EpicSidebarNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    accentColor: Color? = null
) {
    val contentColor = when {
        accentColor != null -> accentColor
        selected -> Color.White
        else -> EpicMutedText
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Color(0xFF222222) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EpicLeftDrawerOverlay(
    currentRoute: String?,
    displayName: String,
    avatarBase64: String?,
    hasUpdateBadge: Boolean,
    isPrimaryAdmin: Boolean,
    onNavigateRoute: (String) -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onExitEpicTheme: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA6000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("epic_left_drawer_overlay")
    ) {
        EpicLauncherSidebarContent(
            currentRoute = currentRoute,
            displayName = displayName,
            avatarBase64 = avatarBase64,
            hasUpdateBadge = hasUpdateBadge,
            isPrimaryAdmin = isPrimaryAdmin,
            onNavigateRoute = onNavigateRoute,
            onOpenWorkshop = onOpenWorkshop,
            onOpenProfile = onOpenProfile,
            onOpenAdminPanel = onOpenAdminPanel,
            onExitEpicTheme = onExitEpicTheme,
            onCloseDrawer = onDismiss,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .widthIn(min = 236.dp, max = 276.dp)
                .border(1.dp, EpicBorderColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        )
    }
}

private data class EpicSocialEntry(
    val uid: String,
    val username: String,
    val activityLine: String,
    val avatarBase64: String? = null
)

/**
 * Authentic Epic Games Launcher Right-Hand Social Panel matching the screenshot:
 * - Thin rounded outline border (#2E2E2E)
 * - Pill tab toggle (COMMUNITY / WORKSHOP) matching (FRIENDS / REQUESTS)
 * - Search input ("Search players or themes")
 * - ONLINE counter + Top "YOU" row with colored circular avatar + green online dot
 * - Live creator/player rows from real community posts & workshop themes
 * - Underlined footer action link
 */
@Composable
fun EpicSocialPanelCard(
    cloudUser: CloudUserInfo?,
    myProfile: CommunityUserProfile?,
    displayName: String,
    activeProfileName: String?,
    isOverlayRunning: Boolean,
    workshopThemes: List<ButtonThemePreset>,
    communityPosts: List<CommunityTopicPost>,
    onSignIn: () -> Unit,
    onOpenMyProfile: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenPublicProfile: (String) -> Unit,
    onClosePanel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = FRIENDS / CREATORS, 1 = WORKSHOP
    var searchQuery by remember { mutableStateOf("") }

    val creators = remember(workshopThemes, communityPosts, cloudUser?.uid) {
        val map = linkedMapOf<String, EpicSocialEntry>()
        communityPosts.forEach { post ->
            if (post.ownerId.isNotBlank() && post.ownerId != cloudUser?.uid && post.ownerId !in map) {
                map[post.ownerId] = EpicSocialEntry(
                    uid = post.ownerId,
                    username = post.authorName.ifBlank { "Creator" },
                    activityLine = "Posted in ${post.topic}",
                    avatarBase64 = post.authorAvatarBase64
                )
            }
        }
        workshopThemes.forEach { theme ->
            if (theme.ownerId.isNotBlank() && theme.ownerId != "builtin" && theme.ownerId != cloudUser?.uid && theme.ownerId !in map) {
                map[theme.ownerId] = EpicSocialEntry(
                    uid = theme.ownerId,
                    username = theme.authorName.ifBlank { "Theme Creator" },
                    activityLine = "Created ${theme.name}"
                )
            }
        }
        map.values.toList()
    }

    val filteredCreators = remember(creators, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) creators else creators.filter {
            it.username.contains(q, ignoreCase = true) || it.activityLine.contains(q, ignoreCase = true)
        }
    }

    val filteredThemes = remember(workshopThemes, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) workshopThemes else workshopThemes.filter {
            it.name.contains(q, ignoreCase = true) || it.gameTag.contains(q, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(EpicCanvasBg)
            .border(1.dp, EpicBorderColor, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top mini action icons (matching the top-right 3 mini icons in Epic Social panel)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenWorkshop, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Community", tint = Color(0xFFC8C8C8), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onOpenMyProfile, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = "Profile", tint = Color(0xFFC8C8C8), modifier = Modifier.size(16.dp))
            }
            if (onClosePanel != null) {
                IconButton(onClick = onClosePanel, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Social Panel", tint = Color(0xFFC8C8C8), modifier = Modifier.size(16.dp))
                }
            }
        }

        // Centered Pill Toggle: [ FRIENDS ]  WORKSHOP
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A1A))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            EpicSocialTabPill(
                label = "COMMUNITY",
                selected = activeTab == 0,
                onClick = { activeTab = 0 }
            )
            EpicSocialTabPill(
                label = "WORKSHOP",
                selected = activeTab == 1,
                onClick = { activeTab = 1 }
            )
        }

        // Outlined Search Box ("Search or add players")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(4.dp))
                .background(Color(0xFF161616))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = if (activeTab == 0) "Search creators or posts" else "Search button themes",
                        color = EpicMutedText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { if (it.length <= 40) searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                    cursorBrush = SolidColor(EpicBlueAccent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFB5B5B5), modifier = Modifier.size(16.dp))
        }

        // Scrollable List
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (activeTab == 0) {
                Text(
                    text = "ONLINE (${filteredCreators.size + 1})",
                    color = EpicMutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )

                // User's own "YOU" row (exact match to G GeologyRocks [YOU])
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onClosePanel?.invoke()
                            onOpenMyProfile()
                        }
                        .padding(vertical = 3.dp)
                        .testTag("epic_social_you_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EpicSocialCircleAvatar(
                        username = displayName,
                        avatarBase64 = myProfile?.avatarBase64,
                        bgColor = EpicOnlineGreen,
                        showOnlineDot = true
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = displayName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                color = EpicPillBg,
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = "YOU",
                                    color = Color(0xFFB5B5B5),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isOverlayRunning) {
                                "Playing • ${activeProfileName ?: "Overlay Active"}"
                            } else {
                                "Online • ${activeProfileName ?: "XenosPad Ready"}"
                            },
                            color = EpicMutedText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Real Community Creators
                filteredCreators.forEachIndexed { idx, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                onClosePanel?.invoke()
                                onOpenPublicProfile(entry.uid)
                            }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EpicSocialCircleAvatar(
                            username = entry.username,
                            avatarBase64 = entry.avatarBase64,
                            bgColor = EpicAvatarColors[(idx + 1) % EpicAvatarColors.size],
                            showOnlineDot = true
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.username,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = entry.activityLine,
                                color = EpicMutedText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (filteredCreators.isEmpty()) {
                    Text(
                        text = if (cloudUser == null) {
                            "Sign in to browse active XenosPad creators and community discussions."
                        } else {
                            "No other creators loaded yet — open Workshop to explore themes & posts."
                        },
                        color = EpicMutedText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            } else {
                Text(
                    text = "WORKSHOP THEMES (${filteredThemes.size})",
                    color = EpicMutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                filteredThemes.take(10).forEachIndexed { idx, theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                onClosePanel?.invoke()
                                onOpenWorkshop()
                            }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EpicSocialCircleAvatar(
                            username = theme.name,
                            avatarBase64 = null,
                            bgColor = EpicAvatarColors[idx % EpicAvatarColors.size],
                            showOnlineDot = false
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = theme.name,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${theme.gameTag} • ♥ ${theme.likesCount}",
                                color = EpicMutedText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = EpicBorderColor)

        // Bottom underlined social link (exact match to "Connect your social account to find friends on Epic Games!")
        Text(
            text = if (cloudUser == null) {
                "Connect your Google account to join the XenosPad community!"
            } else {
                "Open Community Hub & Button Theme Workshop"
            },
            color = Color(0xFFD8D8D8),
            fontSize = 11.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable {
                    onClosePanel?.invoke()
                    if (cloudUser == null) onSignIn() else onOpenWorkshop()
                }
                .testTag("epic_social_footer_cta")
        )
    }
}

@Composable
private fun EpicSocialTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Color(0xFF343434) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else EpicMutedText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EpicSocialCircleAvatar(
    username: String,
    avatarBase64: String?,
    bgColor: Color,
    showOnlineDot: Boolean
) {
    Box(modifier = Modifier.size(34.dp)) {
        if (!avatarBase64.isNullOrBlank()) {
            CommunityAvatar(
                avatarBase64 = avatarBase64,
                username = username,
                size = 32.dp,
                borderColor = bgColor
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.trim().firstOrNull()?.uppercase() ?: "P",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
        if (showOnlineDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(EpicOnlineGreen)
                    .border(1.5.dp, EpicCanvasBg, CircleShape)
            )
        }
    }
}

@Composable
fun EpicSocialPanelDrawerOverlay(
    cloudUser: CloudUserInfo?,
    myProfile: CommunityUserProfile?,
    displayName: String,
    activeProfileName: String?,
    isOverlayRunning: Boolean,
    workshopThemes: List<ButtonThemePreset>,
    communityPosts: List<CommunityTopicPost>,
    onSignIn: () -> Unit,
    onOpenMyProfile: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenPublicProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA6000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("epic_social_drawer_overlay")
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .widthIn(min = 270.dp, max = 320.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(10.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            EpicSocialPanelCard(
                cloudUser = cloudUser,
                myProfile = myProfile,
                displayName = displayName,
                activeProfileName = activeProfileName,
                isOverlayRunning = isOverlayRunning,
                workshopThemes = workshopThemes,
                communityPosts = communityPosts,
                onSignIn = onSignIn,
                onOpenMyProfile = onOpenMyProfile,
                onOpenWorkshop = onOpenWorkshop,
                onOpenPublicProfile = onOpenPublicProfile,
                onClosePanel = onDismiss,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

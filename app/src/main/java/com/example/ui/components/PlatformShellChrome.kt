package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppPlatformTheme
import com.example.model.CommunityTopicPost
import com.example.ui.screens.CommunityAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PlatformNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val showBadge: Boolean = false
)

/**
 * Platform-authentic top app bar that adapts its real structure to Steam Deck Gaming Mode,
 * Epic Games Launcher, Xbox Guide Header, Nintendo Switch Top Bar, PlayStation 5 Header,
 * or clean Xenos Default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformTopBar(
    platformTheme: AppPlatformTheme,
    isPrimaryAdmin: Boolean,
    avatarBase64: String?,
    displayName: String,
    onOpenAdminPanel: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenStore: (() -> Unit)? = null,
    onOpenEpicMenu: () -> Unit = {},
    onOpenEpicSocial: () -> Unit = {},
    currentRoute: String? = null,
    hasUpdateBadge: Boolean = false,
    isOverlayRunning: Boolean = false,
    xboxGamerscore: Int = 1250,
    onOpenXboxGuide: () -> Unit = {},
    onOpenXboxLibrary: () -> Unit = {},
    onLaunchXboxCloudPlay: () -> Unit = {},
    onOpenXboxSettings: () -> Unit = {},
    onSelectPs5Games: () -> Unit = {},
    onSelectPs5Media: () -> Unit = {},
    onOpenPs5Settings: () -> Unit = {},
    nintendoSwitchDarkMode: Boolean = false,
    communityPosts: List<CommunityTopicPost> = emptyList(),
    onOpenPublicProfile: (String) -> Unit = {},
    onToggleNintendoSwitchDarkMode: () -> Unit = {}
) {
    val clockLabel = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    when (platformTheme) {
        AppPlatformTheme.STEAM_DECK -> {
            // Authentic Steam Big Picture Mode top status bar (matching SteamOS Gaming Mode)
            Surface(
                color = Color(0xE80C1016),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Subtle Steam Big Picture + BETA indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "BIG PICTURE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF9FB0C7)
                        )
                        Surface(
                            color = Color(0x33F5B731),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier.border(1.dp, Color(0x99F5B731), RoundedCornerShape(3.dp))
                        ) {
                            Text(
                                text = "BETA",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 8.sp,
                                color = Color(0xFFF5B731),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    if (isPrimaryAdmin) {
                        IconButton(
                            onClick = onOpenAdminPanel,
                            modifier = Modifier.size(30.dp).testTag("topbar_admin_panel_button")
                        ) {
                            AdminRoleBadge(size = 22.dp)
                        }
                    }

                    // Search icon -> opens Steam Store
                    if (onOpenStore != null) {
                        IconButton(
                            onClick = onOpenStore,
                            modifier = Modifier.size(32.dp).testTag("steam_topbar_search_store")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Steam Store",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Blue Steam notification / Workshop pill (matches the blue icon in Steam Big Picture)
                    Surface(
                        color = Color(0xFF1A9FFF),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .size(width = 24.dp, height = 22.dp)
                            .clickable(onClick = onOpenWorkshop)
                            .testTag("topbar_workshop_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Workshop & Community",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFFD0DCEB), modifier = Modifier.size(14.dp))
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color(0xFF59BF40), modifier = Modifier.size(14.dp))

                    Text(
                        text = clockLabel,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )

                    // Small square Steam avatar in the far top-right corner
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .border(1.dp, Color(0xFF66C0F4), RoundedCornerShape(3.dp))
                            .clickable(onClick = onOpenProfile)
                            .testTag("topbar_profile_button")
                    ) {
                        CommunityAvatar(
                            avatarBase64 = avatarBase64,
                            username = displayName,
                            size = 26.dp,
                            borderColor = Color(0xFF66C0F4)
                        )
                    }
                }
            }
        }

        AppPlatformTheme.EPIC_GAMES -> {
            Surface(color = EpicCanvasBg, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onOpenEpicMenu)
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .testTag("epic_topbar_menu_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Epic Launcher Menu",
                                tint = EpicMutedText,
                                modifier = Modifier.size(18.dp)
                            )
                            EpicGamesShieldMark()
                            Text(
                                text = "Epic Games",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = EpicBetaAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(3.dp),
                                modifier = Modifier.border(
                                    1.dp,
                                    EpicBetaAmber.copy(alpha = 0.65f),
                                    RoundedCornerShape(3.dp)
                                )
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

                        Spacer(Modifier.weight(1f))

                        if (isPrimaryAdmin) {
                            IconButton(
                                onClick = onOpenAdminPanel,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("topbar_admin_panel_button")
                            ) {
                                AdminRoleBadge(size = 22.dp)
                            }
                        }
                        IconButton(
                            onClick = onOpenEpicSocial,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("epic_topbar_social_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Epic Social Panel",
                                tint = Color(0xFFD8D8D8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenWorkshop,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("topbar_workshop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Workshop",
                                tint = Color(0xFFD8D8D8),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onOpenProfile)
                                .padding(2.dp)
                                .testTag("topbar_profile_button")
                        ) {
                            CommunityAvatar(
                                avatarBase64 = avatarBase64,
                                username = displayName,
                                size = 28.dp,
                                borderColor = EpicOnlineGreen
                            )
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFF242424))
                }
            }
        }

        AppPlatformTheme.XBOX -> {
            XboxTopHeaderBar(
                displayName = displayName,
                avatarBase64 = avatarBase64,
                gamerscore = xboxGamerscore,
                currentRoute = currentRoute,
                hasUpdateBadge = hasUpdateBadge,
                isOverlayRunning = isOverlayRunning,
                isPrimaryAdmin = isPrimaryAdmin,
                onOpenGuide = onOpenXboxGuide,
                onOpenProfile = onOpenProfile,
                onOpenLibrary = onOpenXboxLibrary,
                onOpenStore = { onOpenStore?.invoke() },
                onLaunchCloudPlay = onLaunchXboxCloudPlay,
                onOpenWorkshop = onOpenWorkshop,
                onOpenSettings = onOpenXboxSettings,
                onOpenAdminPanel = onOpenAdminPanel
            )
        }

        AppPlatformTheme.NINTENDO_SWITCH -> {
            NintendoSwitchTopHeaderBar(
                isDarkMode = nintendoSwitchDarkMode,
                displayName = displayName,
                avatarBase64 = avatarBase64,
                communityPosts = communityPosts,
                isPrimaryAdmin = isPrimaryAdmin,
                onOpenProfile = onOpenProfile,
                onOpenPublicProfile = onOpenPublicProfile,
                onToggleDarkMode = onToggleNintendoSwitchDarkMode,
                onOpenAdminPanel = onOpenAdminPanel
            )
        }

        AppPlatformTheme.PLAYSTATION -> {
            Ps5TopHeaderBar(
                currentRoute = currentRoute,
                displayName = displayName,
                avatarBase64 = avatarBase64,
                hasUpdateBadge = hasUpdateBadge,
                isPrimaryAdmin = isPrimaryAdmin,
                onSelectGames = onSelectPs5Games,
                onSelectMedia = onSelectPs5Media,
                onOpenSearch = onOpenWorkshop,
                onOpenSettings = onOpenPs5Settings,
                onOpenProfile = onOpenProfile,
                onOpenAdminPanel = onOpenAdminPanel
            )
        }

        AppPlatformTheme.XENOS_DEFAULT -> {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_xenospad_fg),
                            contentDescription = "XenosPad logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = "XenosPad",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (isPrimaryAdmin) {
                        IconButton(
                            onClick = onOpenAdminPanel,
                            modifier = Modifier.testTag("topbar_admin_panel_button")
                        ) {
                            AdminRoleBadge(size = 28.dp)
                        }
                    }
                    IconButton(
                        onClick = onOpenWorkshop,
                        modifier = Modifier.testTag("topbar_workshop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Button Theme Workshop & Community",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier.testTag("topbar_profile_button")
                    ) {
                        CommunityAvatar(
                            avatarBase64 = avatarBase64,
                            username = displayName,
                            size = 30.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}

/**
 * Platform-authentic bottom bar respecting system navigation bar insets.
 */
@Composable
fun PlatformBottomNavigationBar(
    platformTheme: AppPlatformTheme,
    tabs: List<PlatformNavItem>,
    currentRoute: String?,
    onSelectRoute: (String) -> Unit,
    onOpenSteamMenu: () -> Unit = {},
    onOpenSteamQuickAccess: () -> Unit = {},
    onSteamPrimaryAction: () -> Unit = {},
    onSteamBackAction: () -> Unit = {},
    isOverlayRunning: Boolean = false,
    onOpenXboxGuide: () -> Unit = {},
    onXboxPrimaryAction: () -> Unit = {},
    onXboxToggleHud: () -> Unit = {},
    onXboxBackAction: () -> Unit = {},
    onOpenPs5ControlCenter: () -> Unit = {},
    onPs5PrimaryAction: () -> Unit = {},
    onPs5ToggleHud: () -> Unit = {},
    onPs5BackAction: () -> Unit = {},
    nintendoSwitchDarkMode: Boolean = false,
    onOpenSwitchQuickMenu: () -> Unit = {},
    onSwitchPrimaryAction: () -> Unit = {},
    onSwitchToggleHud: () -> Unit = {},
    onSwitchBackAction: () -> Unit = {}
) {
    when (platformTheme) {
        AppPlatformTheme.STEAM_DECK -> {
            // Exact Steam Big Picture Mode bottom controller prompt footer:
            // [STEAM] MENU                     [···] OPTIONS   (A) SELECT   (B) BACK
            Surface(
                color = Color(0xFF0A0D12),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFF1A222E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: [STEAM] MENU button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onOpenSteamMenu)
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .testTag("steam_footer_menu_btn")
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "STEAM",
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                letterSpacing = 0.6.sp,
                                color = Color(0xFF0A0D12),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "MENU",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Right: [···] OPTIONS   (A) SELECT   (B) BACK
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onOpenSteamQuickAccess)
                                .padding(horizontal = 3.dp, vertical = 4.dp)
                                .testTag("steam_footer_options_btn")
                        ) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "···",
                                    color = Color(0xFF0A0D12),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = "OPTIONS",
                                color = Color(0xFFE0E6EE),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onSteamPrimaryAction)
                                .padding(horizontal = 3.dp, vertical = 4.dp)
                                .testTag("steam_footer_select_btn")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "A",
                                    color = Color(0xFF0A0D12),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = "SELECT",
                                color = Color(0xFFE0E6EE),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onSteamBackAction)
                                .padding(horizontal = 3.dp, vertical = 4.dp)
                                .testTag("steam_footer_back_btn")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "B",
                                    color = Color(0xFF0A0D12),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = "BACK",
                                color = Color(0xFFE0E6EE),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        AppPlatformTheme.EPIC_GAMES -> {
            // Sleek matte charcoal Epic Games Launcher navigation dock for compact screens
            Surface(
                color = Color(0xFF161616),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFF262626))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Color(0xFF252525) else Color.Transparent)
                                    .clickable { onSelectRoute(tab.route) }
                                    .padding(vertical = 6.dp, horizontal = 2.dp)
                                    .testTag(tab.testTag)
                            ) {
                                NavIconWithOptionalBadge(
                                    icon = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    tint = if (selected) Color.White else Color(0xFF8A8A8A),
                                    showBadge = tab.showBadge
                                )
                                Text(
                                    text = tab.label,
                                    fontSize = 9.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) Color.White else Color(0xFF8A8A8A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        AppPlatformTheme.XBOX -> {
            XboxBottomControllerBar(
                isHomeRoute = currentRoute == com.example.NexusRoutes.COCKPIT,
                isOverlayRunning = isOverlayRunning,
                onOpenGuide = onOpenXboxGuide,
                onPrimaryAction = onXboxPrimaryAction,
                onToggleHud = onXboxToggleHud,
                onBackOrHome = onXboxBackAction
            )
        }

        AppPlatformTheme.NINTENDO_SWITCH -> {
            NintendoSwitchBottomStatusBar(
                isDarkMode = nintendoSwitchDarkMode,
                isHomeRoute = currentRoute == com.example.NexusRoutes.COCKPIT,
                isOverlayRunning = isOverlayRunning,
                onOpenQuickMenu = onOpenSwitchQuickMenu,
                onPrimaryAction = onSwitchPrimaryAction,
                onToggleHud = onSwitchToggleHud,
                onBackOrHome = onSwitchBackAction
            )
        }

        AppPlatformTheme.PLAYSTATION -> {
            Ps5BottomControlBar(
                isHomeRoute = currentRoute == com.example.NexusRoutes.COCKPIT,
                isOverlayRunning = isOverlayRunning,
                onOpenControlCenter = onOpenPs5ControlCenter,
                onPrimaryAction = onPs5PrimaryAction,
                onToggleHud = onPs5ToggleHud,
                onBackOrHome = onPs5BackAction
            )
        }

        else -> {
            // Clean M3 NavigationBar (adapts to Xbox / Epic / Xenos colorScheme & shapes)
            NavigationBar(
                windowInsets = WindowInsets(0),
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onSelectRoute(tab.route) },
                        icon = {
                            NavIconWithOptionalBadge(
                                icon = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                showBadge = tab.showBadge
                            )
                        },
                        label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavIconWithOptionalBadge(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    showBadge: Boolean
) {
    if (showBadge) {
        BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.primary) }) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(21.dp))
        }
    } else {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(21.dp))
    }
}

/**
 * Interactive Platform UI Theme Selector Card for Settings and First-Launch Setup.
 * Renders each platform's own brand logo and includes a 3-dot (⋮) Dark / White mode menu
 * right beside the Nintendo Switch theme.
 */
@Composable
fun PlatformThemeSelectorCard(
    selectedTheme: AppPlatformTheme,
    onSelectTheme: (AppPlatformTheme) -> Unit,
    nintendoSwitchDarkMode: Boolean = false,
    onNintendoSwitchDarkModeChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    var switchModeMenuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.SportsEsports, contentDescription = null, tint = primary)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "APP & CONSOLE UI THEME",
                            style = MaterialTheme.typography.labelMedium,
                            color = primary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Color(0x33F5B731),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(1.dp, Color(0xAAF5B731), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = "BETA",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF5B731),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Beta feature (work in progress) — Transforms the interface, Profile & live GeForce NOW Storefront (Steam, Epic & Xbox). Switch back to Xenos Default anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppPlatformTheme.entries.forEach { theme ->
                    val isSelected = selectedTheme == theme
                    val isSwitchTheme = theme == AppPlatformTheme.NINTENDO_SWITCH
                    val themePrimary = Color(theme.accentPrimaryHex)
                    val themeBg = if (isSwitchTheme && !nintendoSwitchDarkMode) {
                        Color(0xFFF3F3F3)
                    } else {
                        Color(theme.bgHex)
                    }
                    val cardShape = RoundedCornerShape(theme.cardCornerDp.coerceIn(6, 16).dp)
                    val selectedTitleColor = if (isSwitchTheme && !nintendoSwitchDarkMode) Color(0xFF1F1F1F) else Color.White
                    val selectedSubColor = if (isSwitchTheme && !nintendoSwitchDarkMode) Color(0xFF5A5A5A) else Color(0xFFB8C7D9)

                    Surface(
                        color = if (isSelected) themeBg else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = cardShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) themePrimary else MaterialTheme.colorScheme.outlineVariant,
                                shape = cardShape
                            )
                            .clickable { onSelectTheme(theme) }
                            .testTag("platform_theme_${theme.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Dedicated Platform Brand Logo Badge
                            PlatformThemeLogoBadge(
                                theme = theme,
                                nintendoDarkMode = nintendoSwitchDarkMode
                            )

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = theme.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) selectedTitleColor else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (theme != AppPlatformTheme.XENOS_DEFAULT) {
                                        Surface(
                                            color = Color(0x33F5B731),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "BETA",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFD98A00),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (isSwitchTheme) {
                                        Surface(
                                            color = if (nintendoSwitchDarkMode) Color(0xFF282828) else Color(0xFFE3F5FC),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.border(
                                                0.8.dp,
                                                if (nintendoSwitchDarkMode) Color(0xFF19D9EA) else Color(0xFF0AB9E6),
                                                RoundedCornerShape(4.dp)
                                            )
                                        ) {
                                            Text(
                                                text = if (nintendoSwitchDarkMode) "DARK" else "WHITE",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (nintendoSwitchDarkMode) Color(0xFF19D9EA) else Color(0xFF0284C7),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (theme.hasLiveGfnStore) {
                                        Surface(
                                            color = themePrimary.copy(alpha = 0.22f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Storefront,
                                                    contentDescription = null,
                                                    tint = themePrimary,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = "STORE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = themePrimary
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = theme.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) selectedSubColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Three dots (⋮) next to Nintendo Switch theme for switching between Dark & White mode
                            if (isSwitchTheme && onNintendoSwitchDarkModeChange != null) {
                                Box {
                                    IconButton(
                                        onClick = { switchModeMenuExpanded = true },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("nintendo_theme_more_menu")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Switch White or Dark mode",
                                            tint = if (isSelected) selectedTitleColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = switchModeMenuExpanded,
                                        onDismissRequest = { switchModeMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Basic White (Light)",
                                                    fontWeight = if (!nintendoSwitchDarkMode) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.LightMode, contentDescription = null, tint = Color(0xFF0AB9E6))
                                            },
                                            trailingIcon = if (!nintendoSwitchDarkMode) {
                                                { Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF0AB9E6)) }
                                            } else null,
                                            onClick = {
                                                switchModeMenuExpanded = false
                                                onNintendoSwitchDarkModeChange(false)
                                                if (!isSelected) onSelectTheme(AppPlatformTheme.NINTENDO_SWITCH)
                                            },
                                            modifier = Modifier.testTag("nintendo_mode_white_item")
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Basic Black (Dark)",
                                                    fontWeight = if (nintendoSwitchDarkMode) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.DarkMode, contentDescription = null, tint = Color(0xFF19D9EA))
                                            },
                                            trailingIcon = if (nintendoSwitchDarkMode) {
                                                { Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF19D9EA)) }
                                            } else null,
                                            onClick = {
                                                switchModeMenuExpanded = false
                                                onNintendoSwitchDarkModeChange(true)
                                                if (!isSelected) onSelectTheme(AppPlatformTheme.NINTENDO_SWITCH)
                                            },
                                            modifier = Modifier.testTag("nintendo_mode_dark_item")
                                        )
                                    }
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(themePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
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

/**
 * Renders each platform's iconic brand emblem cleanly inside the Theme Selection card.
 */
@Composable
fun PlatformThemeLogoBadge(
    theme: AppPlatformTheme,
    nintendoDarkMode: Boolean = false,
    size: Dp = 44.dp
) {
    val shape = RoundedCornerShape(theme.cardCornerDp.coerceIn(6, 12).dp)
    val themePrimary = Color(theme.accentPrimaryHex)

    when (theme) {
        AppPlatformTheme.XENOS_DEFAULT -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(Color(0xFF141828), Color(0xFF221A3D))))
                    .border(1.5.dp, themePrimary, shape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_xenospad_fg),
                    contentDescription = "XenosPad Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size - 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }

        AppPlatformTheme.STEAM_DECK -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(Color(0xFF0E1B2E), Color(0xFF1B3A5C))))
                    .border(1.5.dp, Color(0xFF66C0F4), shape),
                contentAlignment = Alignment.Center
            ) {
                SteamCrankLogoMark(size = 28.dp)
            }
        }

        AppPlatformTheme.EPIC_GAMES -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(Color(0xFF141414), Color(0xFF2A2A2A))))
                    .border(1.5.dp, Color(0xFFE0E0E0), shape),
                contentAlignment = Alignment.Center
            ) {
                EpicGamesShieldMark()
            }
        }

        AppPlatformTheme.XBOX -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(Color(0xFF0D2812), Color(0xFF107C10))))
                    .border(1.5.dp, XboxFocusGreen, shape),
                contentAlignment = Alignment.Center
            ) {
                XboxOrbMark(
                    size = 28.dp,
                    sphereColor = Color.White,
                    arcColor = XboxBrandGreen
                )
            }
        }

        AppPlatformTheme.NINTENDO_SWITCH -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(Color(0xFFE60012))
                    .border(
                        1.5.dp,
                        if (nintendoDarkMode) Color(0xFF19D9EA) else Color.White,
                        shape
                    ),
                contentAlignment = Alignment.Center
            ) {
                NintendoJoyConMark(
                    size = 26.dp,
                    leftColor = Color.White,
                    rightColor = Color.White,
                    cutoutColor = Color(0xFFE60012)
                )
            }
        }

        AppPlatformTheme.PLAYSTATION -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(Brush.linearGradient(listOf(Color(0xFF003791), Color(0xFF0070D1))))
                    .border(1.5.dp, Color(0xFF66B8FF), shape),
                contentAlignment = Alignment.Center
            ) {
                PlayStationEmblemMark()
            }
        }
    }
}

@Composable
private fun SteamCrankLogoMark(size: Dp = 28.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val radius = this.size.minDimension / 2f
        val center = Offset(w / 2f, h / 2f)

        // Outer Steam circular medallion
        drawCircle(
            color = Color(0xFF12233B),
            radius = radius,
            center = center
        )
        drawCircle(
            color = Color.White,
            radius = radius - 1.5f,
            center = center,
            style = Stroke(width = 2.2f)
        )

        // Upper-right pivot ring & hub
        val topPivot = Offset(w * 0.65f, h * 0.36f)
        val lowPivot = Offset(w * 0.37f, h * 0.67f)
        val tailStart = Offset(w * 0.08f, h * 0.56f)

        // Diagonal crank arm connecting pivots
        drawLine(
            color = Color.White,
            start = tailStart,
            end = lowPivot,
            strokeWidth = w * 0.14f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = lowPivot,
            end = topPivot,
            strokeWidth = w * 0.16f,
            cap = StrokeCap.Round
        )

        // Upper-right joint
        drawCircle(color = Color.White, radius = w * 0.17f, center = topPivot)
        drawCircle(color = Color(0xFF12233B), radius = w * 0.085f, center = topPivot)

        // Lower-left joint
        drawCircle(color = Color.White, radius = w * 0.13f, center = lowPivot)
        drawCircle(color = Color(0xFF12233B), radius = w * 0.06f, center = lowPivot)
    }
}

@Composable
private fun PlayStationEmblemMark() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = "PS",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            letterSpacing = (-0.5).sp,
            lineHeight = 15.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("△", color = Color(0xFF66E0A3), fontSize = 6.5.sp, fontWeight = FontWeight.Bold, lineHeight = 7.sp)
            Text("○", color = Color(0xFFFF8A8A), fontSize = 6.5.sp, fontWeight = FontWeight.Bold, lineHeight = 7.sp)
            Text("✕", color = Color(0xFF8CC8FF), fontSize = 6.5.sp, fontWeight = FontWeight.Bold, lineHeight = 7.sp)
            Text("□", color = Color(0xFFF3A4E8), fontSize = 6.5.sp, fontWeight = FontWeight.Bold, lineHeight = 7.sp)
        }
    }
}

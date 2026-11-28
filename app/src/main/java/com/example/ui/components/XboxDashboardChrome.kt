package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NexusRoutes
import com.example.cloud.CloudUserInfo
import com.example.data.VibrationMode
import com.example.model.AppPlatformTheme
import com.example.model.ButtonThemePreset
import com.example.model.CommunityTopicPost
import com.example.model.CommunityUserProfile
import com.example.model.ControllerProfile
import com.example.model.GameFilterState
import com.example.model.GfnRegionPreset
import com.example.ui.screens.CommunityAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val XboxCanvasBg = Color(0xFF101010)
val XboxAcrylicBg = Color(0xF01C1C1C)
val XboxTileSurface = Color(0xFF222222)
val XboxFocusGreen = Color(0xFF3AE635)
val XboxBrandGreen = Color(0xFF107C10)
val XboxButtonBlue = Color(0xFF2196F3)
val XboxButtonRed = Color(0xFFE53935)
val XboxMutedText = Color(0xFFB3B3B3)
val XboxBetaAmber = Color(0xFFF5B731)

/**
 * Crisp Xbox Nexus Sphere emblem drawn natively with Compose Canvas.
 */
@Composable
fun XboxOrbMark(
    size: Dp = 24.dp,
    sphereColor: Color = Color.White,
    arcColor: Color = XboxBrandGreen,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(sphereColor, sphereColor.copy(alpha = 0.85f)),
                center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
                radius = radius * 1.4f
            ),
            radius = radius,
            center = center
        )
        val strokeW = this.size.minDimension * 0.14f
        val leftArc = Path().apply {
            moveTo(center.x - radius * 0.56f, center.y - radius * 0.48f)
            quadraticTo(
                center.x + radius * 0.05f,
                center.y - radius * 0.12f,
                center.x + radius * 0.54f,
                center.y + radius * 0.52f
            )
        }
        val rightArc = Path().apply {
            moveTo(center.x + radius * 0.56f, center.y - radius * 0.48f)
            quadraticTo(
                center.x - radius * 0.05f,
                center.y - radius * 0.12f,
                center.x - radius * 0.54f,
                center.y + radius * 0.52f
            )
        }
        drawPath(path = leftArc, color = arcColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
        drawPath(path = rightArc, color = arcColor, style = Stroke(width = strokeW, cap = StrokeCap.Round))
    }
}

/**
 * Authentic Xbox Series X|S Top-of-Home floating bar matching the user's screenshot:
 * - Left: Gamerpic + Gamertag + ULTIMATE pill + BETA tag + circular G Gamerscore
 * - Center: 5 translucent circular quick-access icons (My Games & Apps, Store, Game Pass/Cloud, Search/Workshop, Settings with green dot)
 * - Right: Overlay/Mic indicator, Battery, Clock (hh:mm a)
 */
@Composable
fun XboxTopHeaderBar(
    displayName: String,
    avatarBase64: String?,
    gamerscore: Int,
    currentRoute: String?,
    hasUpdateBadge: Boolean,
    isOverlayRunning: Boolean,
    isPrimaryAdmin: Boolean,
    onOpenGuide: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenStore: () -> Unit,
    onLaunchCloudPlay: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdminPanel: () -> Unit
) {
    val clockLabel = remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()).lowercase(Locale.getDefault())
    }
    val formattedScore = remember(gamerscore) {
        NumberFormat.getIntegerInstance(Locale.US).format(gamerscore)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xE80A0C0F), Color(0x990A0C0F), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        val isWide = maxWidth >= 560.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                XboxGamertagCluster(
                    displayName = displayName,
                    avatarBase64 = avatarBase64,
                    formattedScore = formattedScore,
                    onClick = onOpenProfile,
                    modifier = Modifier.weight(1f)
                )

                XboxCenterQuickAccessRow(
                    currentRoute = currentRoute,
                    hasUpdateBadge = hasUpdateBadge,
                    onOpenLibrary = onOpenLibrary,
                    onOpenStore = onOpenStore,
                    onLaunchCloudPlay = onLaunchCloudPlay,
                    onOpenWorkshop = onOpenWorkshop,
                    onOpenSettings = onOpenSettings
                )

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XboxStatusCluster(
                        clockLabel = clockLabel,
                        isOverlayRunning = isOverlayRunning,
                        isPrimaryAdmin = isPrimaryAdmin,
                        onOpenAdminPanel = onOpenAdminPanel,
                        onOpenGuide = onOpenGuide
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    XboxGamertagCluster(
                        displayName = displayName,
                        avatarBase64 = avatarBase64,
                        formattedScore = formattedScore,
                        onClick = onOpenProfile,
                        modifier = Modifier.weight(1f)
                    )
                    XboxStatusCluster(
                        clockLabel = clockLabel,
                        isOverlayRunning = isOverlayRunning,
                        isPrimaryAdmin = isPrimaryAdmin,
                        onOpenAdminPanel = onOpenAdminPanel,
                        onOpenGuide = onOpenGuide
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    XboxCenterQuickAccessRow(
                        currentRoute = currentRoute,
                        hasUpdateBadge = hasUpdateBadge,
                        onOpenLibrary = onOpenLibrary,
                        onOpenStore = onOpenStore,
                        onLaunchCloudPlay = onLaunchCloudPlay,
                        onOpenWorkshop = onOpenWorkshop,
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun XboxGamertagCluster(
    displayName: String,
    avatarBase64: String?,
    formattedScore: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(end = 8.dp, top = 2.dp, bottom = 2.dp)
            .testTag("topbar_profile_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CommunityAvatar(
                avatarBase64 = avatarBase64,
                username = displayName,
                size = 32.dp,
                borderColor = XboxFocusGreen
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = displayName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 124.dp)
                )
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = "ULTIMATE",
                        color = Color(0xFF0E0E0E),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 7.sp,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Surface(
                    color = XboxBetaAmber.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.border(1.dp, XboxBetaAmber.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                ) {
                    Text(
                        text = "BETA",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 7.sp,
                        color = XboxBetaAmber,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        color = Color(0xFF101010),
                        fontWeight = FontWeight.Black,
                        fontSize = 8.sp,
                        lineHeight = 8.sp
                    )
                }
                Text(
                    text = formattedScore,
                    color = Color(0xFFDCDCDC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun XboxCenterQuickAccessRow(
    currentRoute: String?,
    hasUpdateBadge: Boolean,
    onOpenLibrary: () -> Unit,
    onOpenStore: () -> Unit,
    onLaunchCloudPlay: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        XboxCircularQuickIcon(
            icon = Icons.Default.LibraryBooks,
            contentDescription = "My Games & Apps",
            selected = currentRoute == NexusRoutes.PROFILES,
            onClick = onOpenLibrary,
            testTag = "xbox_top_library_btn"
        )
        XboxCircularQuickIcon(
            icon = Icons.Default.Storefront,
            contentDescription = "Microsoft & Xbox Store",
            selected = currentRoute == NexusRoutes.STORE,
            onClick = onOpenStore,
            testTag = "xbox_top_store_btn"
        )
        // Game Pass / GeForce NOW Cloud Play pill button matching center icon in screenshot
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0x991E242B))
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                .clickable(onClick = onLaunchCloudPlay)
                .testTag("xbox_top_cloudplay_btn"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "XCLOUD",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 6.sp,
                    lineHeight = 7.sp
                )
                Text(
                    text = "PLAY",
                    color = XboxFocusGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 6.5.sp,
                    lineHeight = 7.sp
                )
            }
        }
        XboxCircularQuickIcon(
            icon = Icons.Default.Search,
            contentDescription = "Search Workshop & Community",
            selected = false,
            onClick = onOpenWorkshop,
            testTag = "topbar_workshop_button"
        )
        Box {
            XboxCircularQuickIcon(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                selected = currentRoute == NexusRoutes.SETUP,
                onClick = onOpenSettings,
                testTag = "xbox_top_settings_btn"
            )
            // Signature Xbox bright green notification dot on the Settings gear
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 5.dp, end = 5.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (hasUpdateBadge) XboxFocusGreen else Color(0xFF2FD42B))
                    .border(1.dp, Color(0xFF101010), CircleShape)
            )
        }
    }
}

@Composable
private fun XboxCircularQuickIcon(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (selected) XboxBrandGreen else Color(0x881A1F24))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) XboxFocusGreen else Color.White.copy(alpha = 0.14f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun XboxStatusCluster(
    clockLabel: String,
    isOverlayRunning: Boolean,
    isPrimaryAdmin: Boolean,
    onOpenAdminPanel: () -> Unit,
    onOpenGuide: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isPrimaryAdmin) {
            IconButton(
                onClick = onOpenAdminPanel,
                modifier = Modifier
                    .size(30.dp)
                    .testTag("topbar_admin_panel_button")
            ) {
                AdminRoleBadge(size = 22.dp)
            }
        }
        Icon(
            imageVector = if (isOverlayRunning) Icons.Default.SportsEsports else Icons.Default.MicOff,
            contentDescription = if (isOverlayRunning) "Overlay HUD Active" else "Guide",
            tint = if (isOverlayRunning) XboxFocusGreen else Color(0xFFD0D5DC),
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onOpenGuide)
        )
        Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = null,
            tint = Color(0xFFD0D5DC),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = clockLabel,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Sleek Xbox Series X|S bottom controller footer:
 * [Xbox Orb] GUIDE                  (A) SELECT   (X) HUD   (B) HOME
 */
@Composable
fun XboxBottomControllerBar(
    isHomeRoute: Boolean,
    isOverlayRunning: Boolean,
    onOpenGuide: () -> Unit,
    onPrimaryAction: () -> Unit,
    onToggleHud: () -> Unit,
    onBackOrHome: () -> Unit
) {
    Surface(
        color = Color(0xF00D0F12),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF22272D))
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
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1C2126))
                        .border(1.dp, XboxFocusGreen.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenGuide)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("xbox_footer_guide_btn")
                ) {
                    XboxOrbMark(size = 18.dp)
                    Text(
                        text = "GUIDE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    XboxFaceButtonPrompt(
                        letter = "A",
                        color = XboxBrandGreen,
                        label = if (isHomeRoute) "LAUNCH" else "SELECT",
                        onClick = onPrimaryAction,
                        testTag = "xbox_footer_a_btn"
                    )
                    XboxFaceButtonPrompt(
                        letter = "X",
                        color = XboxButtonBlue,
                        label = if (isOverlayRunning) "STOP HUD" else "HUD",
                        onClick = onToggleHud,
                        testTag = "xbox_footer_x_btn"
                    )
                    XboxFaceButtonPrompt(
                        letter = "B",
                        color = XboxButtonRed,
                        label = if (isHomeRoute) "GUIDE" else "HOME",
                        onClick = onBackOrHome,
                        testTag = "xbox_footer_b_btn"
                    )
                }
            }
        }
    }
}

@Composable
private fun XboxFaceButtonPrompt(
    letter: String,
    color: Color,
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
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp
            )
        }
        Text(
            text = label,
            color = Color(0xFFE6EBF2),
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp
        )
    }
}

/**
 * Authentic Xbox Series X|S Guide Slide-Over Panel (left-anchored translucent charcoal Acrylic).
 * Provides instant navigation, live community activity, quick capture/visual toggles, and system actions.
 */
@Composable
fun XboxGuideOverlay(
    currentRoute: String?,
    cloudUser: CloudUserInfo?,
    myProfile: CommunityUserProfile?,
    displayName: String,
    activeProfile: ControllerProfile?,
    allProfiles: List<ControllerProfile>,
    isOverlayRunning: Boolean,
    filterState: GameFilterState,
    selectedRegion: GfnRegionPreset,
    vibrationMode: VibrationMode,
    hasUpdateBadge: Boolean,
    isPrimaryAdmin: Boolean,
    workshopThemes: List<ButtonThemePreset>,
    communityPosts: List<CommunityTopicPost>,
    onNavigateRoute: (String) -> Unit,
    onLaunchWebPlayer: () -> Unit,
    onLaunchXboxCloud: () -> Unit = {},
    onToggleOverlay: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onOpenEditor: (String) -> Unit,
    onSelectRegion: (GfnRegionPreset) -> Unit,
    onVibrationModeChange: (VibrationMode) -> Unit,
    onSignIn: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPublicProfile: (String) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSwitchTheme: (AppPlatformTheme) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Home, 1=People, 2=Capture & FX, 3=Profile & System

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA6000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("xbox_guide_overlay")
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .widthIn(min = 286.dp, max = 356.dp)
                .background(XboxAcrylicBg)
                .border(1.dp, Color(0xFF333940))
                .statusBarsPadding()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            // Top Xbox Guide Icon Strip (Home Orb | People | Capture & FX | Profile & System)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14171A))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    XboxGuideTabButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        testTag = "xbox_guide_tab_home"
                    ) {
                        XboxOrbMark(
                            size = 20.dp,
                            sphereColor = if (selectedTab == 0) Color.White else XboxMutedText,
                            arcColor = if (selectedTab == 0) XboxBrandGreen else Color(0xFF14171A)
                        )
                    }
                    XboxGuideTabButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        testTag = "xbox_guide_tab_people"
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = "Community",
                            tint = if (selectedTab == 1) Color.White else XboxMutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    XboxGuideTabButton(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        testTag = "xbox_guide_tab_capture"
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = "Captures & Quick Controls",
                            tint = if (selectedTab == 2) Color.White else XboxMutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    XboxGuideTabButton(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        testTag = "xbox_guide_tab_profile"
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile & System",
                            tint = if (selectedTab == 3) Color.White else XboxMutedText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Xbox Guide", tint = XboxMutedText)
                }
            }

            HorizontalDivider(thickness = 1.dp, color = Color(0xFF2B3036))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Home & Jump-In Tab
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Xbox Guide",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Surface(
                                color = XboxBetaAmber.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "BETA",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 8.sp,
                                    color = XboxBetaAmber,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        XboxGuideActionTile(
                            icon = Icons.Default.Home,
                            title = "Home",
                            subtitle = "Xbox Dashboard",
                            selected = currentRoute == NexusRoutes.COCKPIT,
                            onClick = {
                                onNavigateRoute(NexusRoutes.COCKPIT)
                                onDismiss()
                            },
                            testTag = "xbox_guide_home"
                        )
                        XboxGuideActionTile(
                            icon = Icons.Default.SportsEsports,
                            title = "Xbox Cloud Gaming",
                            subtitle = "Play xbox.com/play with Bypass VPN (${selectedRegion.shortCode}) & Filters",
                            accentColor = XboxFocusGreen,
                            onClick = {
                                onDismiss()
                                onLaunchXboxCloud()
                            },
                            testTag = "xbox_guide_xcloud"
                        )
                        XboxGuideActionTile(
                            icon = Icons.Default.CloudQueue,
                            title = "GeForce NOW Cloud Play",
                            subtitle = "Launch full-screen web stream (${selectedRegion.shortCode})",
                            accentColor = Color(0xFF76B900),
                            onClick = {
                                onDismiss()
                                onLaunchWebPlayer()
                            },
                            testTag = "xbox_guide_cloudplay"
                        )
                        XboxGuideActionTile(
                            icon = Icons.Default.Layers,
                            title = if (isOverlayRunning) "Stop Touch Overlay HUD" else "Start Touch Overlay HUD",
                            subtitle = activeProfile?.name ?: "Active controller layout",
                            accentColor = if (isOverlayRunning) Color(0xFFFF7070) else XboxFocusGreen,
                            onClick = {
                                onToggleOverlay()
                                onDismiss()
                            },
                            testTag = "xbox_guide_overlay"
                        )
                        XboxGuideActionTile(
                            icon = Icons.Default.LibraryBooks,
                            title = "My Games & Apps",
                            subtitle = "${allProfiles.size} controller profiles",
                            selected = currentRoute == NexusRoutes.PROFILES,
                            onClick = {
                                onNavigateRoute(NexusRoutes.PROFILES)
                                onDismiss()
                            },
                            testTag = "xbox_guide_profiles"
                        )
                        XboxGuideActionTile(
                            icon = Icons.Default.Storefront,
                            title = "Xbox Store",
                            subtitle = "Browse live Xbox & PC Game Pass catalog",
                            selected = currentRoute == NexusRoutes.STORE,
                            onClick = {
                                onNavigateRoute(NexusRoutes.STORE)
                                onDismiss()
                            },
                            testTag = "xbox_guide_store"
                        )
                        XboxGuideActionTile(
                            icon = Icons.Default.Palette,
                            title = "Workshop & Community",
                            subtitle = "Button themes & creator feed",
                            onClick = {
                                onDismiss()
                                onOpenWorkshop()
                            },
                            testTag = "xbox_guide_workshop"
                        )
                        XboxGuideActionTile(
                            icon = Icons.Default.TouchApp,
                            title = "Controller Sandbox",
                            subtitle = "Live input & deadzone tester",
                            selected = currentRoute == NexusRoutes.SANDBOX,
                            onClick = {
                                onNavigateRoute(NexusRoutes.SANDBOX)
                                onDismiss()
                            },
                            testTag = "xbox_guide_sandbox"
                        )

                        if (allProfiles.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "QUICK RESUME PROFILES",
                                color = XboxMutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            allProfiles.take(4).forEach { profile ->
                                val isActive = profile.id == activeProfile?.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isActive) XboxBrandGreen.copy(alpha = 0.35f) else XboxTileSurface)
                                        .border(
                                            width = if (isActive) 1.5.dp else 1.dp,
                                            color = if (isActive) XboxFocusGreen else Color(0xFF2D3238),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onSelectProfile(profile.id) }
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Gamepad,
                                        contentDescription = null,
                                        tint = if (isActive) XboxFocusGreen else XboxMutedText,
                                        modifier = Modifier.size(18.dp)
                                    )
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
                                            text = profile.gameTitle.ifBlank { "${profile.controls.size} controls" },
                                            color = XboxMutedText,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenEditor(profile.id)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Layout",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // People & Community Tab
                        Text(
                            text = "People & Community",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        XboxGuideActionTile(
                            icon = Icons.Default.Palette,
                            title = "Open Community Hub",
                            subtitle = "${workshopThemes.size} themes · ${communityPosts.size} posts",
                            accentColor = XboxFocusGreen,
                            onClick = {
                                onDismiss()
                                onOpenWorkshop()
                            },
                            testTag = "xbox_guide_open_community"
                        )

                        val authors = remember(communityPosts, workshopThemes, cloudUser?.uid) {
                            val seen = linkedMapOf<String, Pair<String, String>>()
                            communityPosts.forEach { post ->
                                if (post.ownerId.isNotBlank() && post.ownerId != cloudUser?.uid && post.ownerId !in seen) {
                                    seen[post.ownerId] = post.authorName.ifBlank { "Player" } to "Posted in ${post.topic}"
                                }
                            }
                            workshopThemes.forEach { theme ->
                                if (theme.ownerId.isNotBlank() && theme.ownerId != "builtin" && theme.ownerId != cloudUser?.uid && theme.ownerId !in seen) {
                                    seen[theme.ownerId] = theme.authorName.ifBlank { "Creator" } to "Published ${theme.name}"
                                }
                            }
                            seen.entries.toList()
                        }

                        if (authors.isEmpty()) {
                            Surface(
                                color = XboxTileSurface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Browse the Workshop to discover creators, share layouts, and view Xbox-styled player profiles.",
                                    color = XboxMutedText,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            authors.take(10).forEach { (uid, info) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(XboxTileSurface)
                                        .clickable {
                                            onDismiss()
                                            onOpenPublicProfile(uid)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CommunityAvatar(
                                        avatarBase64 = null,
                                        username = info.first,
                                        size = 30.dp,
                                        borderColor = XboxFocusGreen
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = info.first,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = info.second,
                                            color = XboxMutedText,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Capture, Visuals & Performance Quick Controls
                        Text(
                            text = "Capture & Visuals",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        XboxGuideActionTile(
                            icon = Icons.Default.PhotoLibrary,
                            title = "Captures & Gallery",
                            subtitle = "Clips, instant replays & screenshots",
                            selected = currentRoute == NexusRoutes.GALLERY,
                            onClick = {
                                onNavigateRoute(NexusRoutes.GALLERY)
                                onDismiss()
                            },
                            testTag = "xbox_guide_gallery"
                        )
                        XboxGuideActionTile(
                            icon = Icons.Default.Tune,
                            title = "Visual Filters & Crosshair",
                            subtitle = if (filterState.activeSlot > 0) "Slot ${filterState.activeSlot} active" else "GPU shaders & custom reticles",
                            selected = currentRoute == NexusRoutes.FILTERS,
                            onClick = {
                                onNavigateRoute(NexusRoutes.FILTERS)
                                onDismiss()
                            },
                            testTag = "xbox_guide_filters"
                        )

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "CLOUD STREAM REGION",
                            color = XboxMutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GfnRegionPreset.entries.take(4).forEach { region ->
                                val isSel = region == selectedRegion
                                Surface(
                                    color = if (isSel) XboxBrandGreen else XboxTileSurface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            1.dp,
                                            if (isSel) XboxFocusGreen else Color(0xFF333940),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onSelectRegion(region) }
                                ) {
                                    Text(
                                        text = region.shortCode,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "HAPTIC FEEDBACK",
                            color = XboxMutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VibrationMode.entries.forEach { mode ->
                                val isSel = mode == vibrationMode
                                Surface(
                                    color = if (isSel) XboxBrandGreen else XboxTileSurface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            1.dp,
                                            if (isSel) XboxFocusGreen else Color(0xFF333940),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onVibrationModeChange(mode) }
                                ) {
                                    Text(
                                        text = mode.name.take(4),
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // Profile & System Tab
                        Text(
                            text = "Profile & System",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Surface(
                            color = XboxTileSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    if (cloudUser == null) onSignIn() else onOpenProfile()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CommunityAvatar(
                                    avatarBase64 = myProfile?.avatarBase64,
                                    username = displayName,
                                    size = 38.dp,
                                    borderColor = XboxFocusGreen
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (cloudUser != null) "View Xbox-style Profile" else "Sign in with Google",
                                        color = XboxFocusGreen,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        XboxGuideActionTile(
                            icon = if (hasUpdateBadge) Icons.Default.Download else Icons.Default.Settings,
                            title = if (hasUpdateBadge) "System Update Ready" else "Settings",
                            subtitle = "Updates, themes, input & cloud backup",
                            accentColor = if (hasUpdateBadge) XboxFocusGreen else null,
                            selected = currentRoute == NexusRoutes.SETUP,
                            onClick = {
                                onNavigateRoute(NexusRoutes.SETUP)
                                onDismiss()
                            },
                            testTag = "xbox_guide_settings"
                        )
                        if (isPrimaryAdmin) {
                            XboxGuideActionTile(
                                icon = Icons.Default.SportsEsports,
                                title = "Admin Panel",
                                subtitle = "Manage cosmetics & roles",
                                accentColor = XboxBetaAmber,
                                onClick = {
                                    onDismiss()
                                    onOpenAdminPanel()
                                },
                                testTag = "xbox_guide_admin"
                            )
                        }
                        XboxGuideActionTile(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            title = "Switch to Xenos Default Theme",
                            subtitle = "Exit Xbox Dashboard Beta",
                            accentColor = Color(0xFFFF8080),
                            onClick = {
                                onDismiss()
                                onSwitchTheme(AppPlatformTheme.XENOS_DEFAULT)
                            },
                            testTag = "xbox_guide_exit_theme"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun XboxGuideTabButton(
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Color(0xFF252B31) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(testTag)
    ) {
        content()
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) XboxFocusGreen else Color.Transparent)
        )
    }
}

@Composable
private fun XboxGuideActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    selected: Boolean = false,
    accentColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) XboxBrandGreen.copy(alpha = 0.35f) else XboxTileSurface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) XboxFocusGreen else Color(0xFF2D3238),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor ?: if (selected) XboxFocusGreen else Color.White,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = XboxMutedText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = XboxFocusGreen,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

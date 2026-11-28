package com.example.ui.components

import android.content.Context
import android.os.BatteryManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NexusRoutes
import com.example.data.VibrationMode
import com.example.model.AppPlatformTheme
import com.example.model.CommunityTopicPost
import com.example.model.ControllerProfile
import com.example.model.GameFilterState
import com.example.model.GfnRegionPreset
import com.example.ui.screens.CommunityAvatar
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SwitchThemePalette(
    val isDark: Boolean,
    val canvasBg: Color,
    val tileSurface: Color,
    val emptyTileSurface: Color,
    val dockSurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val dividerColor: Color,
    val focusCyan: Color,
    val labelBlue: Color,
    val onlineRed: Color,
    val avatarRing: Color,
    val buttonCircleBg: Color,
    val buttonCircleText: Color
)

fun switchPaletteFor(isDark: Boolean): SwitchThemePalette = if (isDark) {
    SwitchThemePalette(
        isDark = true,
        canvasBg = Color(0xFF282828),
        tileSurface = Color(0xFF383838),
        emptyTileSurface = Color(0xFF343434),
        dockSurface = Color(0xFF363636),
        primaryText = Color(0xFFF4F4F4),
        secondaryText = Color(0xFFAEAEAE),
        dividerColor = Color(0xFF474747),
        focusCyan = Color(0xFF19D9EA),
        labelBlue = Color(0xFF2FE0F0),
        onlineRed = Color(0xFFE60012),
        avatarRing = Color(0xFF4E4E4E),
        buttonCircleBg = Color(0xFFF2F2F2),
        buttonCircleText = Color(0xFF222222)
    )
} else {
    SwitchThemePalette(
        isDark = false,
        canvasBg = Color(0xFFEBEBEB),
        tileSurface = Color(0xFFFFFFFF),
        emptyTileSurface = Color(0xFFF8F8F8),
        dockSurface = Color(0xFFFAFAFA),
        primaryText = Color(0xFF2D2D2D),
        secondaryText = Color(0xFF6E6E6E),
        dividerColor = Color(0xFFD0D0D0),
        focusCyan = Color(0xFF0AB9E6),
        labelBlue = Color(0xFF2475D8),
        onlineRed = Color(0xFFE60012),
        avatarRing = Color(0xFFFFFFFF),
        buttonCircleBg = Color(0xFF262626),
        buttonCircleText = Color(0xFFFFFFFF)
    )
}

val SwitchBetaAmber = Color(0xFFD98A00)

/**
 * Crisp Nintendo Switch Dual Joy-Con emblem drawn natively on Compose Canvas.
 */
@Composable
fun NintendoJoyConMark(
    size: Dp = 24.dp,
    leftColor: Color = Color.White,
    rightColor: Color = Color.White,
    cutoutColor: Color = Color(0xFFE60012),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val pad = w * 0.08f
        val gap = w * 0.08f
        val halfW = (w - pad * 2f - gap) / 2f
        val corner = CornerRadius(w * 0.22f, w * 0.22f)
        val strokeW = (w * 0.085f).coerceAtLeast(1.4f)

        // Left Joy-Con (outlined with solid top analog stick dot)
        drawRoundRect(
            color = leftColor,
            topLeft = Offset(pad, pad),
            size = Size(halfW, h - pad * 2f),
            cornerRadius = corner,
            style = Stroke(width = strokeW)
        )
        drawCircle(
            color = leftColor,
            radius = halfW * 0.24f,
            center = Offset(pad + halfW * 0.5f, pad + (h - pad * 2f) * 0.34f)
        )

        // Right Joy-Con (solid with cutout lower analog stick dot)
        val rightLeft = pad + halfW + gap
        drawRoundRect(
            color = rightColor,
            topLeft = Offset(rightLeft, pad),
            size = Size(halfW, h - pad * 2f),
            cornerRadius = corner
        )
        drawCircle(
            color = cutoutColor,
            radius = halfW * 0.25f,
            center = Offset(rightLeft + halfW * 0.5f, pad + (h - pad * 2f) * 0.62f)
        )
    }
}

/**
 * Iconic bottom-left Nintendo Switch handheld + Joy-Con status icon with 4 green LED player dots.
 */
@Composable
fun SwitchHandheldLedIndicator(
    inkColor: Color,
    isOverlayRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 4 vertical player LED dots (matching screenshot bottom-left)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(4) { idx ->
                val lit = idx == 0 || (isOverlayRunning && idx == 1)
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(if (lit) Color(0xFF4CD964) else inkColor.copy(alpha = 0.25f))
                )
            }
        }
        // Compact Switch Handheld silhouette
        Canvas(modifier = Modifier.size(width = 24.dp, height = 16.dp)) {
            val w = size.width
            val h = size.height
            val joyW = w * 0.25f
            val screenLeft = joyW + w * 0.05f
            val screenW = w - joyW * 2f - w * 0.1f

            // Left Joy-Con
            drawRoundRect(
                color = inkColor,
                topLeft = Offset(0f, 0f),
                size = Size(joyW, h),
                cornerRadius = CornerRadius(h * 0.32f, h * 0.32f)
            )
            // Center screen bezel
            drawRoundRect(
                color = inkColor,
                topLeft = Offset(screenLeft, h * 0.06f),
                size = Size(screenW, h * 0.88f),
                cornerRadius = CornerRadius(2f, 2f),
                style = Stroke(width = 1.6f)
            )
            // Right Joy-Con
            drawRoundRect(
                color = inkColor,
                topLeft = Offset(w - joyW, 0f),
                size = Size(joyW, h),
                cornerRadius = CornerRadius(h * 0.32f, h * 0.32f)
            )
        }
    }
}

/**
 * Authentic Nintendo Switch Top Header Bar matching the reference screenshot:
 * - Left: Circular user profile avatars (current player + nearby community avatars) + BETA tag
 * - Right: Clock (11:07 AM), Wi-Fi icon, real Battery %, Battery gauge icon
 */
@Composable
fun NintendoSwitchTopHeaderBar(
    isDarkMode: Boolean,
    displayName: String,
    avatarBase64: String?,
    communityPosts: List<CommunityTopicPost>,
    isPrimaryAdmin: Boolean,
    onOpenProfile: () -> Unit,
    onOpenPublicProfile: (String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onOpenAdminPanel: () -> Unit
) {
    val palette = remember(isDarkMode) { switchPaletteFor(isDarkMode) }
    val context = LocalContext.current
    val clockLabel = remember {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }
    val batteryPct = remember(context) {
        runCatching {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 1..100 }
        }.getOrNull() ?: 85
    }
    val extraUsers = remember(communityPosts, displayName) {
        communityPosts
            .filter { it.ownerId.isNotBlank() && it.authorName.isNotBlank() && it.authorName != displayName }
            .distinctBy { it.ownerId }
            .take(2)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.canvasBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Row of circular user profile avatars (matching Switch Home top-left)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.tileSurface)
                    .border(2.dp, palette.focusCyan, CircleShape)
                    .clickable(onClick = onOpenProfile)
                    .testTag("topbar_profile_button"),
                contentAlignment = Alignment.Center
            ) {
                CommunityAvatar(
                    avatarBase64 = avatarBase64,
                    username = displayName,
                    size = 30.dp,
                    borderColor = Color.Transparent
                )
            }

            extraUsers.forEach { post ->
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(palette.tileSurface)
                        .border(1.5.dp, palette.avatarRing, CircleShape)
                        .clickable { onOpenPublicProfile(post.ownerId) },
                    contentAlignment = Alignment.Center
                ) {
                    CommunityAvatar(
                        avatarBase64 = null,
                        username = post.authorName,
                        size = 27.dp,
                        borderColor = Color.Transparent
                    )
                }
            }

            Surface(
                color = SwitchBetaAmber.copy(alpha = if (isDarkMode) 0.22f else 0.14f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.border(
                    1.dp,
                    SwitchBetaAmber.copy(alpha = 0.65f),
                    RoundedCornerShape(4.dp)
                )
            ) {
                Text(
                    text = "BETA",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 8.sp,
                    color = if (isDarkMode) Color(0xFFF5B731) else Color(0xFFB36B00),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        // Right: Admin badge (if admin), White/Dark quick chip, Clock, Wi-Fi, Battery %
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

            // Quick Light/Dark Switch icon right in the top bar for instant toggling too
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(palette.tileSurface)
                    .border(1.dp, palette.dividerColor, CircleShape)
                    .clickable(onClick = onToggleDarkMode)
                    .testTag("switch_topbar_theme_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkMode) "Switch to Basic White" else "Switch to Basic Black",
                    tint = palette.primaryText,
                    modifier = Modifier.size(15.dp)
                )
            }

            Text(
                text = clockLabel,
                color = palette.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = palette.primaryText,
                modifier = Modifier.size(16.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "$batteryPct%",
                    color = palette.primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                SwitchBatteryGauge(
                    percent = batteryPct,
                    frameColor = palette.primaryText
                )
            }
        }
    }
}

@Composable
private fun SwitchBatteryGauge(
    percent: Int,
    frameColor: Color
) {
    Canvas(modifier = Modifier.size(width = 20.dp, height = 11.dp)) {
        val bodyW = size.width - 3f
        val bodyH = size.height
        drawRoundRect(
            color = frameColor,
            topLeft = Offset(0f, 0.5f),
            size = Size(bodyW, bodyH - 1f),
            cornerRadius = CornerRadius(2.5f, 2.5f),
            style = Stroke(width = 1.5f)
        )
        // Battery cap
        drawRect(
            color = frameColor,
            topLeft = Offset(bodyW + 0.5f, bodyH * 0.28f),
            size = Size(2.5f, bodyH * 0.44f)
        )
        val fillFrac = (percent.coerceIn(5, 100) / 100f)
        val fillColor = when {
            percent <= 20 -> Color(0xFFE60012)
            else -> Color(0xFF4CD964)
        }
        drawRect(
            color = fillColor,
            topLeft = Offset(2.2f, 2.5f),
            size = Size((bodyW - 4.4f) * fillFrac, bodyH - 5f)
        )
    }
}

/**
 * Authentic Nintendo Switch Bottom Status Bar matching the screenshot:
 * Hairline divider + bottom-left Handheld/Joy-Con LED icon + bottom-right (A) OK prompt
 */
@Composable
fun NintendoSwitchBottomStatusBar(
    isDarkMode: Boolean,
    isHomeRoute: Boolean,
    isOverlayRunning: Boolean,
    onOpenQuickMenu: () -> Unit,
    onPrimaryAction: () -> Unit,
    onToggleHud: () -> Unit,
    onBackOrHome: () -> Unit
) {
    val palette = remember(isDarkMode) { switchPaletteFor(isDarkMode) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.canvasBg)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = palette.dividerColor,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenQuickMenu)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("switch_footer_handheld_btn")
            ) {
                SwitchHandheldLedIndicator(
                    inkColor = palette.primaryText,
                    isOverlayRunning = isOverlayRunning
                )
                Text(
                    text = if (isOverlayRunning) "HUD Live" else "Quick Menu",
                    color = palette.secondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SwitchButtonPrompt(
                    letter = "X",
                    label = if (isOverlayRunning) "Stop HUD" else "HUD",
                    palette = palette,
                    onClick = onToggleHud,
                    testTag = "switch_footer_x_btn"
                )
                if (!isHomeRoute) {
                    SwitchButtonPrompt(
                        letter = "B",
                        label = "Home",
                        palette = palette,
                        onClick = onBackOrHome,
                        testTag = "switch_footer_b_btn"
                    )
                }
                SwitchButtonPrompt(
                    letter = "A",
                    label = "OK",
                    palette = palette,
                    onClick = onPrimaryAction,
                    testTag = "switch_footer_a_btn"
                )
            }
        }
    }
}

@Composable
private fun SwitchButtonPrompt(
    letter: String,
    label: String,
    palette: SwitchThemePalette,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(19.dp)
                .clip(CircleShape)
                .background(palette.buttonCircleBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                color = palette.buttonCircleText,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
        Text(
            text = label,
            color = palette.primaryText,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

/**
 * Authentic Nintendo Switch Quick Settings / Sleep Side Panel (right-anchored like holding HOME on Switch).
 * Lets the user switch between Basic White & Basic Black, launch Cloud Play, toggle HUD, switch region,
 * and manage profiles.
 */
@Composable
fun NintendoSwitchQuickMenuOverlay(
    isDarkMode: Boolean,
    currentRoute: String?,
    displayName: String,
    avatarBase64: String?,
    activeProfile: ControllerProfile?,
    allProfiles: List<ControllerProfile>,
    isOverlayRunning: Boolean,
    filterState: GameFilterState,
    selectedRegion: GfnRegionPreset,
    vibrationMode: VibrationMode,
    isPrimaryAdmin: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
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
    val palette = remember(isDarkMode) { switchPaletteFor(isDarkMode) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("switch_quick_menu_overlay")
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .widthIn(min = 286.dp, max = 356.dp)
                .background(palette.canvasBg)
                .border(1.dp, palette.dividerColor)
                .statusBarsPadding()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.onlineRed),
                        contentAlignment = Alignment.Center
                    ) {
                        NintendoJoyConMark(
                            size = 18.dp,
                            leftColor = Color.White,
                            rightColor = Color.White,
                            cutoutColor = palette.onlineRed
                        )
                    }
                    Column {
                        Text(
                            text = "Switch Quick Menu",
                            color = palette.primaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isDarkMode) "Basic Black (Dark)" else "Basic White (Light)",
                            color = palette.focusCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.secondaryText)
                }
            }

            HorizontalDivider(color = palette.dividerColor)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Basic White / Basic Black Theme Switcher Card
                Text(
                    text = "DISPLAY THEME",
                    color = palette.secondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SwitchModeToggleChip(
                        title = "Basic White",
                        icon = Icons.Default.LightMode,
                        selected = !isDarkMode,
                        palette = palette,
                        onClick = { onToggleDarkMode(false) },
                        testTag = "switch_quick_white_mode",
                        modifier = Modifier.weight(1f)
                    )
                    SwitchModeToggleChip(
                        title = "Basic Black",
                        icon = Icons.Default.DarkMode,
                        selected = isDarkMode,
                        palette = palette,
                        onClick = { onToggleDarkMode(true) },
                        testTag = "switch_quick_dark_mode",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Cloud Gaming & Overlay Quick Actions
                Text(
                    text = "CLOUD PLAY & HUD",
                    color = palette.secondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                SwitchQuickActionRow(
                    icon = Icons.Default.CloudQueue,
                    title = "GeForce NOW Web Play",
                    subtitle = "Region ${selectedRegion.shortCode} · Bypass VPN & Filters",
                    accentColor = palette.onlineRed,
                    palette = palette,
                    onClick = {
                        onDismiss()
                        onLaunchGeForceNow()
                    }
                )
                SwitchQuickActionRow(
                    icon = Icons.Default.SportsEsports,
                    title = "Xbox Cloud Gaming",
                    subtitle = "xbox.com/play · Bypass VPN & Filters",
                    accentColor = Color(0xFF107C10),
                    palette = palette,
                    onClick = {
                        onDismiss()
                        onLaunchXboxCloud()
                    }
                )
                SwitchQuickActionRow(
                    icon = Icons.Default.Layers,
                    title = if (isOverlayRunning) "Stop Touch Overlay HUD" else "Start Touch Overlay HUD",
                    subtitle = activeProfile?.name ?: "Floating gamepad controls",
                    accentColor = if (isOverlayRunning) palette.onlineRed else palette.focusCyan,
                    palette = palette,
                    onClick = {
                        onToggleOverlay()
                        onDismiss()
                    }
                )

                // Region Bypass Quick Picker
                Text(
                    text = "BYPASS VPN REGION",
                    color = palette.secondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GfnRegionPreset.entries) { preset ->
                        val selected = preset == selectedRegion
                        Surface(
                            color = if (selected) palette.focusCyan else palette.tileSurface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .border(1.dp, if (selected) palette.focusCyan else palette.dividerColor, RoundedCornerShape(16.dp))
                                .clickable { onSelectRegion(preset) }
                        ) {
                            Text(
                                text = preset.shortCode,
                                color = if (selected) Color.White else palette.primaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Navigation Shortcuts
                Text(
                    text = "NAVIGATION",
                    color = palette.secondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                SwitchQuickActionRow(
                    icon = Icons.Default.Home,
                    title = "Switch Home",
                    subtitle = "Software tiles & applet dock",
                    accentColor = palette.focusCyan,
                    palette = palette,
                    selected = currentRoute == NexusRoutes.COCKPIT,
                    onClick = {
                        onNavigateRoute(NexusRoutes.COCKPIT)
                        onDismiss()
                    }
                )
                SwitchQuickActionRow(
                    icon = Icons.Default.FolderCopy,
                    title = "Controllers & Profiles",
                    subtitle = "${allProfiles.size} saved layouts",
                    accentColor = palette.focusCyan,
                    palette = palette,
                    selected = currentRoute == NexusRoutes.PROFILES,
                    onClick = {
                        onNavigateRoute(NexusRoutes.PROFILES)
                        onDismiss()
                    }
                )
                SwitchQuickActionRow(
                    icon = Icons.Default.Tune,
                    title = "Visuals & Crosshair",
                    subtitle = if (filterState.activeSlot > 0) "Slot ${filterState.activeSlot} active" else "18 Freestyle shaders",
                    accentColor = Color(0xFF06B6D4),
                    palette = palette,
                    selected = currentRoute == NexusRoutes.FILTERS,
                    onClick = {
                        onNavigateRoute(NexusRoutes.FILTERS)
                        onDismiss()
                    }
                )
                SwitchQuickActionRow(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Album · Captures",
                    subtitle = "Instant replays, recordings & shots",
                    accentColor = Color(0xFF2563EB),
                    palette = palette,
                    selected = currentRoute == NexusRoutes.GALLERY,
                    onClick = {
                        onNavigateRoute(NexusRoutes.GALLERY)
                        onDismiss()
                    }
                )
                SwitchQuickActionRow(
                    icon = Icons.Default.Palette,
                    title = "Community Workshop",
                    subtitle = "Button themes & creator feed",
                    accentColor = Color(0xFFF59E0B),
                    palette = palette,
                    onClick = {
                        onDismiss()
                        onOpenWorkshop()
                    }
                )
                SwitchQuickActionRow(
                    icon = Icons.Default.TouchApp,
                    title = "Controller Sandbox",
                    subtitle = "Live input & gyro tester",
                    accentColor = palette.secondaryText,
                    palette = palette,
                    selected = currentRoute == NexusRoutes.SANDBOX,
                    onClick = {
                        onNavigateRoute(NexusRoutes.SANDBOX)
                        onDismiss()
                    }
                )
                SwitchQuickActionRow(
                    icon = Icons.Default.Settings,
                    title = "System Settings",
                    subtitle = "Updates, themes & cloud save",
                    accentColor = palette.secondaryText,
                    palette = palette,
                    selected = currentRoute == NexusRoutes.SETUP,
                    onClick = {
                        onNavigateRoute(NexusRoutes.SETUP)
                        onDismiss()
                    }
                )

                // Vibration mode
                Text(
                    text = "HD RUMBLE / HAPTICS",
                    color = palette.secondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VibrationMode.entries.forEach { mode ->
                        val selected = mode == vibrationMode
                        Surface(
                            color = if (selected) palette.focusCyan else palette.tileSurface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (selected) palette.focusCyan else palette.dividerColor, RoundedCornerShape(10.dp))
                                .clickable { onVibrationModeChange(mode) }
                        ) {
                            Text(
                                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = if (selected) Color.White else palette.primaryText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Quick Profile Switcher
                if (allProfiles.isNotEmpty()) {
                    Text(
                        text = "ACTIVE CONTROLLER LAYOUT",
                        color = palette.secondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    allProfiles.take(4).forEach { profile ->
                        val isActive = profile.id == activeProfile?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.tileSurface)
                                .border(
                                    width = if (isActive) 2.dp else 1.dp,
                                    color = if (isActive) palette.focusCyan else palette.dividerColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectProfile(profile.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = if (isActive) palette.focusCyan else palette.secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = profile.name,
                                color = palette.primaryText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onOpenEditor(profile.id)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Layout", tint = palette.secondaryText, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }

                // Profile & Theme Exit
                HorizontalDivider(color = palette.dividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.tileSurface)
                        .border(1.dp, palette.dividerColor, RoundedCornerShape(10.dp))
                        .clickable {
                            onDismiss()
                            onOpenProfile()
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CommunityAvatar(avatarBase64 = avatarBase64, username = displayName, size = 30.dp, borderColor = palette.focusCyan)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayName, color = palette.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("View Nintendo Switch Profile", color = palette.secondaryText, fontSize = 11.sp)
                    }
                    if (isPrimaryAdmin) {
                        IconButton(onClick = { onDismiss(); onOpenAdminPanel() }, modifier = Modifier.size(30.dp)) {
                            AdminRoleBadge(size = 22.dp)
                        }
                    }
                }

                SwitchQuickActionRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Return to XenosPad Default Theme",
                    subtitle = "Switch app theme anytime in Settings",
                    accentColor = palette.onlineRed,
                    palette = palette,
                    onClick = {
                        onDismiss()
                        onSwitchTheme(AppPlatformTheme.XENOS_DEFAULT)
                    }
                )
            }
        }
    }
}

@Composable
private fun SwitchModeToggleChip(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    palette: SwitchThemePalette,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) palette.focusCyan.copy(alpha = 0.16f) else palette.tileSurface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) palette.focusCyan else palette.dividerColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) palette.focusCyan else palette.secondaryText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                color = palette.primaryText,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SwitchQuickActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    palette: SwitchThemePalette,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.tileSurface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) palette.focusCyan else palette.dividerColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.primaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = palette.secondaryText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppPlatformTheme
import com.example.model.CommunityUserProfile
import com.example.model.CosmeticItem
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily

/**
 * Renders a genuine platform-styled profile header card (Steam, Epic Games, Xbox,
 * Nintendo Switch, PlayStation 5, or Xenos Default) while preserving all equipped
 * cosmetics and top-right Admin/Verified role corner badges.
 */
@Composable
fun PlatformAdaptiveProfileHeader(
    platformTheme: AppPlatformTheme,
    profile: CommunityUserProfile?,
    equippedFrame: CosmeticItem?,
    equippedNameTag: CosmeticItem?,
    equippedVerificationIcon: CosmeticItem?,
    clipsCount: Int,
    themesCount: Int,
    postsCount: Int,
    isOwnProfile: Boolean,
    isPrimaryAdmin: Boolean,
    onEditProfileClick: () -> Unit,
    onOpenShopClick: () -> Unit,
    onOpenAdminPanelClick: () -> Unit
) {
    when (platformTheme) {
        AppPlatformTheme.STEAM_DECK -> SteamProfileHeaderCard(
            profile = profile,
            equippedFrame = equippedFrame,
            equippedNameTag = equippedNameTag,
            equippedVerificationIcon = equippedVerificationIcon,
            clipsCount = clipsCount,
            themesCount = themesCount,
            postsCount = postsCount,
            isOwnProfile = isOwnProfile,
            isPrimaryAdmin = isPrimaryAdmin,
            onEditProfileClick = onEditProfileClick,
            onOpenShopClick = onOpenShopClick,
            onOpenAdminPanelClick = onOpenAdminPanelClick
        )
        AppPlatformTheme.EPIC_GAMES -> EpicProfileHeaderCard(
            profile = profile,
            equippedFrame = equippedFrame,
            equippedNameTag = equippedNameTag,
            equippedVerificationIcon = equippedVerificationIcon,
            clipsCount = clipsCount,
            themesCount = themesCount,
            postsCount = postsCount,
            isOwnProfile = isOwnProfile,
            isPrimaryAdmin = isPrimaryAdmin,
            onEditProfileClick = onEditProfileClick,
            onOpenShopClick = onOpenShopClick,
            onOpenAdminPanelClick = onOpenAdminPanelClick
        )
        AppPlatformTheme.XBOX -> XboxProfileHeaderCard(
            profile = profile,
            equippedFrame = equippedFrame,
            equippedNameTag = equippedNameTag,
            equippedVerificationIcon = equippedVerificationIcon,
            clipsCount = clipsCount,
            themesCount = themesCount,
            postsCount = postsCount,
            isOwnProfile = isOwnProfile,
            isPrimaryAdmin = isPrimaryAdmin,
            onEditProfileClick = onEditProfileClick,
            onOpenShopClick = onOpenShopClick,
            onOpenAdminPanelClick = onOpenAdminPanelClick
        )
        AppPlatformTheme.NINTENDO_SWITCH -> SwitchProfileHeaderCard(
            profile = profile,
            equippedFrame = equippedFrame,
            equippedNameTag = equippedNameTag,
            equippedVerificationIcon = equippedVerificationIcon,
            clipsCount = clipsCount,
            themesCount = themesCount,
            postsCount = postsCount,
            isOwnProfile = isOwnProfile,
            isPrimaryAdmin = isPrimaryAdmin,
            onEditProfileClick = onEditProfileClick,
            onOpenShopClick = onOpenShopClick,
            onOpenAdminPanelClick = onOpenAdminPanelClick
        )
        AppPlatformTheme.PLAYSTATION -> PlayStationProfileHeaderCard(
            profile = profile,
            equippedFrame = equippedFrame,
            equippedNameTag = equippedNameTag,
            equippedVerificationIcon = equippedVerificationIcon,
            clipsCount = clipsCount,
            themesCount = themesCount,
            postsCount = postsCount,
            isOwnProfile = isOwnProfile,
            isPrimaryAdmin = isPrimaryAdmin,
            onEditProfileClick = onEditProfileClick,
            onOpenShopClick = onOpenShopClick,
            onOpenAdminPanelClick = onOpenAdminPanelClick
        )
        AppPlatformTheme.XENOS_DEFAULT -> XenosDefaultProfileHeaderCard(
            profile = profile,
            equippedFrame = equippedFrame,
            equippedNameTag = equippedNameTag,
            equippedVerificationIcon = equippedVerificationIcon,
            clipsCount = clipsCount,
            themesCount = themesCount,
            postsCount = postsCount,
            isOwnProfile = isOwnProfile,
            isPrimaryAdmin = isPrimaryAdmin,
            onEditProfileClick = onEditProfileClick,
            onOpenShopClick = onOpenShopClick,
            onOpenAdminPanelClick = onOpenAdminPanelClick
        )
    }
}

@Composable
private fun SteamProfileHeaderCard(
    profile: CommunityUserProfile?,
    equippedFrame: CosmeticItem?,
    equippedNameTag: CosmeticItem?,
    equippedVerificationIcon: CosmeticItem?,
    clipsCount: Int,
    themesCount: Int,
    postsCount: Int,
    isOwnProfile: Boolean,
    isPrimaryAdmin: Boolean,
    onEditProfileClick: () -> Unit,
    onOpenShopClick: () -> Unit,
    onOpenAdminPanelClick: () -> Unit
) {
    val steamLevel = (1 + clipsCount * 3 + themesCount * 5 + postsCount * 2).coerceAtMost(999)
    val steamBlue = Color(0xFF66C0F4)
    val steamGreen = Color(0xFF59BF40)
    val shape = RoundedCornerShape(6.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171D25)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2B3E54), shape)
            .testTag("profile_header_steam")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1F3349), Color(0xFF171D25), Color(0xFF12171E))
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(
                        end = if (profile?.adminRole == true || profile?.verifiedRole == true) 66.dp else 0.dp
                    )
                ) {
                    // Steam square-framed avatar halo
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(2.dp, steamBlue, RoundedCornerShape(6.dp))
                            .padding(2.dp)
                    ) {
                        FramedCommunityAvatar(
                            avatarBase64 = profile?.avatarBase64,
                            username = profile?.username ?: "Player",
                            frameItem = equippedFrame,
                            size = 66.dp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile?.username ?: "Steam Player",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (equippedVerificationIcon != null) {
                                ProfileVerificationIconBadge(item = equippedVerificationIcon, size = 19.dp)
                            }
                        }
                        if (equippedNameTag != null) {
                            ProfileNameTagChip(item = equippedNameTag)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(steamGreen)
                            )
                            Text(
                                text = "Currently Online • XenosPad Deck",
                                fontSize = 11.sp,
                                color = steamBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Steam Level Badge Ring
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LEVEL", fontSize = 9.sp, color = Color(0xFF8F98A0), fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF141B23))
                                .border(2.dp, steamBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$steamLevel",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Steam summary / bio box
                Surface(
                    color = Color(0xAA0E141B),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = profile?.bio?.ifBlank { "No Steam profile summary given." } ?: "Loading profile...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC6D4DF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                // Steam Showcase Strip
                Surface(
                    color = Color(0xFF10161E),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF26364A), RoundedCornerShape(4.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PlatformMetricCell("$clipsCount", "Captures", steamBlue)
                        PlatformMetricCell("$themesCount", "Workshop", steamGreen)
                        PlatformMetricCell("$postsCount", "Discussions", Color.White)
                    }
                }

                if (isOwnProfile) {
                    ProfileActionButtonsRow(
                        isPrimaryAdmin = isPrimaryAdmin,
                        shopAccent = Color(0xFF1A9FFF),
                        shopTextColor = Color.White,
                        onEditProfileClick = onEditProfileClick,
                        onOpenShopClick = onOpenShopClick,
                        onOpenAdminPanelClick = onOpenAdminPanelClick
                    )
                }
            }

            ProfileRoleCornerBadges(
                adminRole = profile?.adminRole == true,
                verifiedRole = profile?.verifiedRole == true,
                badgeSize = 28.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            )
        }
    }
}

@Composable
private fun EpicProfileHeaderCard(
    profile: CommunityUserProfile?,
    equippedFrame: CosmeticItem?,
    equippedNameTag: CosmeticItem?,
    equippedVerificationIcon: CosmeticItem?,
    clipsCount: Int,
    themesCount: Int,
    postsCount: Int,
    isOwnProfile: Boolean,
    isPrimaryAdmin: Boolean,
    onEditProfileClick: () -> Unit,
    onOpenShopClick: () -> Unit,
    onOpenAdminPanelClick: () -> Unit
) {
    val epicBlue = Color(0xFF0078F2)
    val xp = (clipsCount * 150 + themesCount * 300 + postsCount * 100)
    val shape = RoundedCornerShape(8.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181D)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2D2D36), shape)
            .testTag("profile_header_epic")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(
                        end = if (profile?.adminRole == true || profile?.verifiedRole == true) 66.dp else 0.dp
                    )
                ) {
                    FramedCommunityAvatar(
                        avatarBase64 = profile?.avatarBase64,
                        username = profile?.username ?: "Epic Player",
                        frameItem = equippedFrame,
                        size = 64.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "EPIC ACCOUNT",
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                            color = epicBlue
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile?.username ?: "Epic Player",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (equippedVerificationIcon != null) {
                                ProfileVerificationIconBadge(item = equippedVerificationIcon, size = 19.dp)
                            }
                        }
                        if (equippedNameTag != null) ProfileNameTagChip(item = equippedNameTag)
                        Text(
                            text = profile?.bio?.ifBlank { "Epic Games Cloud Player" } ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0A0B0)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EpicStatPill("XP", "$xp", epicBlue, Modifier.weight(1f))
                    EpicStatPill("CLIPS", "$clipsCount", Color.White, Modifier.weight(1f))
                    EpicStatPill("THEMES", "$themesCount", Color.White, Modifier.weight(1f))
                    EpicStatPill("POSTS", "$postsCount", Color.White, Modifier.weight(1f))
                }

                if (isOwnProfile) {
                    ProfileActionButtonsRow(
                        isPrimaryAdmin = isPrimaryAdmin,
                        shopAccent = epicBlue,
                        shopTextColor = Color.White,
                        onEditProfileClick = onEditProfileClick,
                        onOpenShopClick = onOpenShopClick,
                        onOpenAdminPanelClick = onOpenAdminPanelClick
                    )
                }
            }

            ProfileRoleCornerBadges(
                adminRole = profile?.adminRole == true,
                verifiedRole = profile?.verifiedRole == true,
                badgeSize = 28.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun EpicStatPill(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF23232B),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = valueColor)
            Text(label, fontSize = 9.sp, letterSpacing = 0.8.sp, color = Color(0xFF9494A6))
        }
    }
}

@Composable
private fun XboxProfileHeaderCard(
    profile: CommunityUserProfile?,
    equippedFrame: CosmeticItem?,
    equippedNameTag: CosmeticItem?,
    equippedVerificationIcon: CosmeticItem?,
    clipsCount: Int,
    themesCount: Int,
    postsCount: Int,
    isOwnProfile: Boolean,
    isPrimaryAdmin: Boolean,
    onEditProfileClick: () -> Unit,
    onOpenShopClick: () -> Unit,
    onOpenAdminPanelClick: () -> Unit
) {
    val xboxGreen = Color(0xFF107C10)
    val xboxBright = Color(0xFF52B043)
    val gamerscore = 1000 + clipsCount * 50 + themesCount * 150 + postsCount * 25
    val shape = RoundedCornerShape(10.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, xboxGreen.copy(alpha = 0.8f), shape)
            .testTag("profile_header_xbox")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF103A10), Color(0xFF171717), Color(0xFF141414))
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(
                        end = if (profile?.adminRole == true || profile?.verifiedRole == true) 66.dp else 0.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(2.5.dp, xboxBright, CircleShape)
                            .padding(2.dp)
                    ) {
                        FramedCommunityAvatar(
                            avatarBase64 = profile?.avatarBase64,
                            username = profile?.username ?: "Gamertag",
                            frameItem = equippedFrame,
                            size = 66.dp
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile?.username ?: "Xbox Gamertag",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (equippedVerificationIcon != null) {
                                ProfileVerificationIconBadge(item = equippedVerificationIcon, size = 19.dp)
                            }
                        }
                        if (equippedNameTag != null) ProfileNameTagChip(item = equippedNameTag)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(color = xboxGreen, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = "G  $gamerscore",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Text("Online", fontSize = 11.sp, color = xboxBright, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (!profile?.bio.isNullOrBlank()) {
                    Text(
                        text = profile!!.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD0D0D0)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PlatformMetricCell("$clipsCount", "Game Clips", xboxBright)
                    PlatformMetricCell("$themesCount", "Themes", Color.White)
                    PlatformMetricCell("$postsCount", "Feed Posts", Color.White)
                }

                if (isOwnProfile) {
                    ProfileActionButtonsRow(
                        isPrimaryAdmin = isPrimaryAdmin,
                        shopAccent = xboxGreen,
                        shopTextColor = Color.White,
                        onEditProfileClick = onEditProfileClick,
                        onOpenShopClick = onOpenShopClick,
                        onOpenAdminPanelClick = onOpenAdminPanelClick
                    )
                }
            }

            ProfileRoleCornerBadges(
                adminRole = profile?.adminRole == true,
                verifiedRole = profile?.verifiedRole == true,
                badgeSize = 28.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun SwitchProfileHeaderCard(
    profile: CommunityUserProfile?,
    equippedFrame: CosmeticItem?,
    equippedNameTag: CosmeticItem?,
    equippedVerificationIcon: CosmeticItem?,
    clipsCount: Int,
    themesCount: Int,
    postsCount: Int,
    isOwnProfile: Boolean,
    isPrimaryAdmin: Boolean,
    onEditProfileClick: () -> Unit,
    onOpenShopClick: () -> Unit,
    onOpenAdminPanelClick: () -> Unit
) {
    val joyRed = Color(0xFFE60012)
    val joyBlue = Color(0xFF00C3E3)
    val shape = RoundedCornerShape(10.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, joyRed.copy(alpha = 0.65f), shape)
            .testTag("profile_header_switch")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Iconic Switch Red Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Brush.horizontalGradient(listOf(joyBlue, joyRed)))
                )
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(
                            end = if (profile?.adminRole == true || profile?.verifiedRole == true) 66.dp else 0.dp
                        )
                    ) {
                        FramedCommunityAvatar(
                            avatarBase64 = profile?.avatarBase64,
                            username = profile?.username ?: "Switch User",
                            frameItem = equippedFrame,
                            size = 64.dp
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${profile?.username ?: "Player"}'s Page",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (equippedVerificationIcon != null) {
                                    ProfileVerificationIconBadge(item = equippedVerificationIcon, size = 19.dp)
                                }
                            }
                            if (equippedNameTag != null) ProfileNameTagChip(item = equippedNameTag)
                            Text(
                                text = profile?.bio?.ifBlank { "Play Activity • Online" } ?: "Online",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PlatformMetricCell("$clipsCount", "Album Clips", joyRed)
                        PlatformMetricCell("$themesCount", "Themes", joyBlue)
                        PlatformMetricCell("$postsCount", "Topics", MaterialTheme.colorScheme.onSurface)
                    }

                    if (isOwnProfile) {
                        ProfileActionButtonsRow(
                            isPrimaryAdmin = isPrimaryAdmin,
                            shopAccent = joyRed,
                            shopTextColor = Color.White,
                            onEditProfileClick = onEditProfileClick,
                            onOpenShopClick = onOpenShopClick,
                            onOpenAdminPanelClick = onOpenAdminPanelClick
                        )
                    }
                }
            }

            ProfileRoleCornerBadges(
                adminRole = profile?.adminRole == true,
                verifiedRole = profile?.verifiedRole == true,
                badgeSize = 28.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 12.dp)
            )
        }
    }
}

@Composable
private fun PlayStationProfileHeaderCard(
    profile: CommunityUserProfile?,
    equippedFrame: CosmeticItem?,
    equippedNameTag: CosmeticItem?,
    equippedVerificationIcon: CosmeticItem?,
    clipsCount: Int,
    themesCount: Int,
    postsCount: Int,
    isOwnProfile: Boolean,
    isPrimaryAdmin: Boolean,
    onEditProfileClick: () -> Unit,
    onOpenShopClick: () -> Unit,
    onOpenAdminPanelClick: () -> Unit
) {
    val psBlue = Color(0xFF0070D1)
    val psGlow = Color(0xFF5BB2FF)
    val shape = RoundedCornerShape(22.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1428)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, psGlow.copy(alpha = 0.45f), shape)
            .testTag("profile_header_ps5")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF17366E), Color(0xFF0A1224), Color(0xFF060B17))
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(
                        end = if (profile?.adminRole == true || profile?.verifiedRole == true) 66.dp else 0.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                            .padding(2.dp)
                    ) {
                        FramedCommunityAvatar(
                            avatarBase64 = profile?.avatarBase64,
                            username = profile?.username ?: "PSN Player",
                            frameItem = equippedFrame,
                            size = 66.dp
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile?.username ?: "PlayStation Player",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Light,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (equippedVerificationIcon != null) {
                                ProfileVerificationIconBadge(item = equippedVerificationIcon, size = 19.dp)
                            }
                        }
                        if (equippedNameTag != null) ProfileNameTagChip(item = equippedNameTag)
                        Text(
                            text = profile?.bio?.ifBlank { "Online • PlayStation 5 Style" } ?: "Online",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB3C9E8)
                        )
                    }
                }

                // Trophy-style stats pill bar
                Surface(
                    color = Color(0x6616294D),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFE5EEFF), modifier = Modifier.size(16.dp))
                            PlatformMetricCell("$themesCount", "Themes", Color(0xFFE5EEFF))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(16.dp))
                            PlatformMetricCell("$clipsCount", "Captures", Color(0xFFFFD166))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = psGlow, modifier = Modifier.size(16.dp))
                            PlatformMetricCell("$postsCount", "Posts", psGlow)
                        }
                    }
                }

                if (isOwnProfile) {
                    ProfileActionButtonsRow(
                        isPrimaryAdmin = isPrimaryAdmin,
                        shopAccent = psBlue,
                        shopTextColor = Color.White,
                        onEditProfileClick = onEditProfileClick,
                        onOpenShopClick = onOpenShopClick,
                        onOpenAdminPanelClick = onOpenAdminPanelClick
                    )
                }
            }

            ProfileRoleCornerBadges(
                adminRole = profile?.adminRole == true,
                verifiedRole = profile?.verifiedRole == true,
                badgeSize = 28.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
            )
        }
    }
}

@Composable
private fun XenosDefaultProfileHeaderCard(
    profile: CommunityUserProfile?,
    equippedFrame: CosmeticItem?,
    equippedNameTag: CosmeticItem?,
    equippedVerificationIcon: CosmeticItem?,
    clipsCount: Int,
    themesCount: Int,
    postsCount: Int,
    isOwnProfile: Boolean,
    isPrimaryAdmin: Boolean,
    onEditProfileClick: () -> Unit,
    onOpenShopClick: () -> Unit,
    onOpenAdminPanelClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f), RoundedCornerShape(18.dp))
            .testTag("profile_header_xenos")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(
                        end = if (profile?.adminRole == true || profile?.verifiedRole == true) 68.dp else 0.dp
                    )
                ) {
                    FramedCommunityAvatar(
                        avatarBase64 = profile?.avatarBase64,
                        username = profile?.username ?: "Player",
                        frameItem = equippedFrame,
                        size = 64.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile?.username ?: "XenosPad Player",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (equippedVerificationIcon != null) {
                                ProfileVerificationIconBadge(item = equippedVerificationIcon, size = 20.dp)
                            }
                        }
                        if (equippedNameTag != null) ProfileNameTagChip(item = equippedNameTag)
                        Text(
                            text = profile?.bio?.ifBlank { "No bio set yet." } ?: "Loading profile...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isOwnProfile) {
                    ProfileActionButtonsRow(
                        isPrimaryAdmin = isPrimaryAdmin,
                        shopAccent = GeForceLime,
                        shopTextColor = Color.Black,
                        onEditProfileClick = onEditProfileClick,
                        onOpenShopClick = onOpenShopClick,
                        onOpenAdminPanelClick = onOpenAdminPanelClick
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PlatformMetricCell("$clipsCount", "Clips", MaterialTheme.colorScheme.primary)
                    PlatformMetricCell("$themesCount", "Themes", MaterialTheme.colorScheme.primary)
                    PlatformMetricCell("$postsCount", "Posts", MaterialTheme.colorScheme.primary)
                }
            }

            ProfileRoleCornerBadges(
                adminRole = profile?.adminRole == true,
                verifiedRole = profile?.verifiedRole == true,
                badgeSize = 30.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun PlatformMetricCell(value: String, label: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileActionButtonsRow(
    isPrimaryAdmin: Boolean,
    shopAccent: Color,
    shopTextColor: Color,
    onEditProfileClick: () -> Unit,
    onOpenShopClick: () -> Unit,
    onOpenAdminPanelClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEditProfileClick,
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("profile_edit_btn")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Edit Profile", fontSize = 12.sp, maxLines = 1)
            }
            Button(
                onClick = onOpenShopClick,
                colors = ButtonDefaults.buttonColors(containerColor = shopAccent),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("profile_cosmetic_shop_btn")
            ) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = shopTextColor, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Shop & Gear", fontSize = 12.sp, color = shopTextColor, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        if (isPrimaryAdmin) {
            OutlinedButton(
                onClick = onOpenAdminPanelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("profile_open_admin_panel_btn")
            ) {
                AdminRoleBadge(size = 20.dp)
                Spacer(Modifier.width(8.dp))
                Text("Open Admin Management Panel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

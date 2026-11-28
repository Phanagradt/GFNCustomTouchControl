package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CosmeticItem
import com.example.model.CosmeticItemType
import com.example.model.VerificationIconPreset
import com.example.util.ButtonImageCodec

val AdminRoleGoldLight = Color(0xFFFFD54F)
val AdminRoleGoldDeep = Color(0xFFC5941B)
val VerifiedRoleBlueLight = Color(0xFF38B6FF)
val VerifiedRoleBlueDeep = Color(0xFF1565C0)

/**
 * Visual role badges rendered on the top-right corner of a user's profile:
 * - Admin Role: White wrench icon with a gold background.
 * - Verified Role: White checkmark icon with a blue background.
 */
@Composable
fun ProfileRoleCornerBadges(
    adminRole: Boolean,
    verifiedRole: Boolean,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 28.dp,
    showTextLabels: Boolean = false
) {
    if (!adminRole && !verifiedRole) return
    Row(
        modifier = modifier.testTag("profile_role_corner_badges"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (adminRole) {
            AdminRoleBadge(size = badgeSize, showLabel = showTextLabels)
        }
        if (verifiedRole) {
            VerifiedRoleBadge(size = badgeSize, showLabel = showTextLabels)
        }
    }
}

@Composable
fun AdminRoleBadge(
    size: Dp = 28.dp,
    showLabel: Boolean = false,
    modifier: Modifier = Modifier
) {
    val goldBrush = remember {
        Brush.linearGradient(colors = listOf(AdminRoleGoldLight, AdminRoleGoldDeep))
    }
    if (showLabel) {
        Row(
            modifier = modifier
                .shadow(3.dp, RoundedCornerShape(999.dp))
                .clip(RoundedCornerShape(999.dp))
                .background(goldBrush)
                .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("role_badge_admin"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Admin Role",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "ADMIN",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(goldBrush)
                .border(1.2.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                .testTag("role_badge_admin"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Admin Role",
                tint = Color.White,
                modifier = Modifier.size(size * 0.58f)
            )
        }
    }
}

@Composable
fun VerifiedRoleBadge(
    size: Dp = 28.dp,
    showLabel: Boolean = false,
    modifier: Modifier = Modifier
) {
    val blueBrush = remember {
        Brush.linearGradient(colors = listOf(VerifiedRoleBlueLight, VerifiedRoleBlueDeep))
    }
    if (showLabel) {
        Row(
            modifier = modifier
                .shadow(3.dp, RoundedCornerShape(999.dp))
                .clip(RoundedCornerShape(999.dp))
                .background(blueBrush)
                .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("role_badge_verified"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Verified Role",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "VERIFIED",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(blueBrush)
                .border(1.2.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                .testTag("role_badge_verified"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Verified Role",
                tint = Color.White,
                modifier = Modifier.size(size * 0.62f)
            )
        }
    }
}

/**
 * Renders a user avatar wrapped in their equipped custom Profile Frame (gradient border or custom frame image).
 */
@Composable
fun FramedCommunityAvatar(
    avatarBase64: String?,
    username: String,
    frameItem: CosmeticItem?,
    size: Dp = 64.dp,
    onClick: (() -> Unit)? = null
) {
    val avatarBmp: ImageBitmap? = remember(avatarBase64) {
        ButtonImageCodec.decodeBase64ToImageBitmap(avatarBase64)
    }
    val frameBmp: ImageBitmap? = remember(frameItem?.imageBase64) {
        ButtonImageCodec.decodeBase64ToImageBitmap(frameItem?.imageBase64)
    }
    val initials = remember(username) {
        username.trim().split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }.ifEmpty { "P" }
    }

    val ringWidth = (frameItem?.borderWidthDp ?: 2.0f).coerceIn(1.5f, 6.5f).dp
    val ringBrush = if (frameItem != null) {
        Brush.sweepGradient(
            colors = listOf(
                Color(frameItem.primaryColorHex),
                Color(frameItem.secondaryColorHex),
                Color(frameItem.primaryColorHex)
            )
        )
    } else {
        val fallback = MaterialTheme.colorScheme.primary
        Brush.linearGradient(listOf(fallback, fallback.copy(alpha = 0.7f)))
    }

    val outerSize = if (frameItem != null) size + ringWidth * 2.4f else size

    Box(
        modifier = Modifier
            .size(outerSize)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing frame ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(width = ringWidth, brush = ringBrush, shape = CircleShape)
                .padding(ringWidth + 1.5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            if (avatarBmp != null) {
                Image(
                    bitmap = avatarBmp,
                    contentDescription = "$username avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp,
                    color = if (frameItem != null) Color(frameItem.primaryColorHex) else MaterialTheme.colorScheme.primary
                )
            }
        }

        // Optional uploaded custom frame overlay PNG/WebP
        if (frameBmp != null) {
            Image(
                bitmap = frameBmp,
                contentDescription = frameItem?.name ?: "Profile Frame",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

/**
 * Renders a user's equipped custom Name Tag chip next to or below their username.
 */
@Composable
fun ProfileNameTagChip(
    item: CosmeticItem,
    modifier: Modifier = Modifier
) {
    val miniBmp = remember(item.imageBase64) {
        ButtonImageCodec.decodeBase64ToImageBitmap(item.imageBase64)
    }
    val bgBrush = remember(item.primaryColorHex, item.secondaryColorHex) {
        Brush.horizontalGradient(
            listOf(Color(item.primaryColorHex), Color(item.secondaryColorHex))
        )
    }
    val textColor = Color(item.textColorHex)
    val label = item.tagText.ifBlank { item.name }.take(20)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bgBrush)
            .border(
                width = item.borderWidthDp.coerceIn(0.8f, 2.5f).dp,
                color = textColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(7.dp)
            )
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
            .testTag("profile_name_tag_${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (miniBmp != null) {
            Image(
                bitmap = miniBmp,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
            )
        }
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Renders a user's equipped custom Verification Icon next to their username.
 */
@Composable
fun ProfileVerificationIconBadge(
    item: CosmeticItem,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val customBmp = remember(item.imageBase64) {
        ButtonImageCodec.decodeBase64ToImageBitmap(item.imageBase64)
    }
    val bgBrush = remember(item.primaryColorHex, item.secondaryColorHex) {
        Brush.linearGradient(
            listOf(Color(item.primaryColorHex), Color(item.secondaryColorHex))
        )
    }
    val tint = Color(item.textColorHex)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgBrush)
            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
            .testTag("profile_verification_icon_${item.id}"),
        contentAlignment = Alignment.Center
    ) {
        if (customBmp != null) {
            Image(
                bitmap = customBmp,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = verificationPresetVector(item.iconPreset),
                contentDescription = item.name,
                tint = tint,
                modifier = Modifier.size(size * 0.64f)
            )
        }
    }
}

fun verificationPresetVector(presetWireId: String?): ImageVector {
    return when (VerificationIconPreset.fromWire(presetWireId)) {
        VerificationIconPreset.CHECK -> Icons.Default.Check
        VerificationIconPreset.STAR -> Icons.Default.Star
        VerificationIconPreset.BOLT -> Icons.Default.Bolt
        VerificationIconPreset.SHIELD -> Icons.Default.Shield
        VerificationIconPreset.WORKSPACE -> Icons.Default.WorkspacePremium
        VerificationIconPreset.DIAMOND -> Icons.Default.Diamond
        VerificationIconPreset.VERIFIED -> Icons.Default.Verified
        VerificationIconPreset.CUSTOM -> Icons.Default.AutoAwesome
    }
}

/**
 * Compact visual preview tile for any CosmeticItem (used in Admin Panel, Shop, and Wardrobe).
 */
@Composable
fun CosmeticItemPreviewTile(
    item: CosmeticItem,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0A0F18),
        modifier = modifier
            .size(56.dp)
            .border(1.dp, Color(item.primaryColorHex).copy(alpha = 0.55f), RoundedCornerShape(12.dp))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(4.dp)) {
            when (item.type) {
                CosmeticItemType.FRAME -> {
                    FramedCommunityAvatar(
                        avatarBase64 = null,
                        username = "XP",
                        frameItem = item,
                        size = 36.dp
                    )
                }
                CosmeticItemType.NAME_TAG -> {
                    ProfileNameTagChip(item = item)
                }
                CosmeticItemType.VERIFICATION_ICON -> {
                    ProfileVerificationIconBadge(item = item, size = 32.dp)
                }
            }
        }
    }
}

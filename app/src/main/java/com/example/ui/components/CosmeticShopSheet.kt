package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CommunityUserProfile
import com.example.model.CosmeticItem
import com.example.model.CosmeticItemType
import com.example.model.ItemAvailability
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.TacticalCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmeticShopAndWardrobeSheet(
    profile: CommunityUserProfile?,
    cosmeticItems: List<CosmeticItem>,
    ownedItemIds: Set<String>,
    onClaimFreeItem: (CosmeticItem) -> Unit,
    onEquipItem: (CosmeticItemType, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var sheetTab by remember { mutableIntStateOf(0) } // 0 = Free Shop, 1 = My Wardrobe
    var typeFilter by remember { mutableStateOf<CosmeticItemType?>(null) }

    val freeShopItems = remember(cosmeticItems, typeFilter) {
        cosmeticItems.filter { item ->
            item.availability == ItemAvailability.FREE_SHOP &&
                (typeFilter == null || item.type == typeFilter)
        }
    }
    val wardrobeItems = remember(cosmeticItems, ownedItemIds, typeFilter) {
        cosmeticItems.filter { item ->
            item.id in ownedItemIds &&
                (typeFilter == null || item.type == typeFilter)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = GeForceLime
                )
                Column {
                    Text(
                        text = "Cosmetic Shop & Wardrobe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Claim free profile frames, name tags & icons, or equip exclusive rewards.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(
                    0 to "Free Shop",
                    1 to "My Wardrobe (${ownedItemIds.size})"
                )
                tabs.forEachIndexed { idx, (tabIdx, label) ->
                    SegmentedButton(
                        selected = sheetTab == tabIdx,
                        onClick = { sheetTab = tabIdx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        modifier = Modifier.testTag("cosmetic_sheet_tab_$idx")
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = typeFilter == null,
                        onClick = { typeFilter = null },
                        label = { Text("All") }
                    )
                }
                items(CosmeticItemType.entries) { t ->
                    FilterChip(
                        selected = typeFilter == t,
                        onClick = { typeFilter = t },
                        label = { Text(t.pluralLabel) }
                    )
                }
            }

            val displayList = if (sheetTab == 0) freeShopItems else wardrobeItems
            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (sheetTab == 0) {
                            "No free items in the shop right now. Check back soon!"
                        } else {
                            "You haven't unlocked any items in this category yet. Claim free items from the Shop!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayList, key = { it.id }) { item ->
                        val isOwned = item.id in ownedItemIds
                        val isEquipped = when (item.type) {
                            CosmeticItemType.FRAME -> profile?.equippedFrameId == item.id
                            CosmeticItemType.NAME_TAG -> profile?.equippedNameTagId == item.id
                            CosmeticItemType.VERIFICATION_ICON -> profile?.equippedVerificationIconId == item.id
                        }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isEquipped) 1.6.dp else 1.dp,
                                    color = if (isEquipped) GeForceLime else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(14.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CosmeticItemPreviewTile(item = item)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (item.availability == ItemAvailability.EXCLUSIVE) {
                                            Surface(
                                                color = AdminRoleGoldLight.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = AdminRoleGoldLight,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = "EXCLUSIVE",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = AdminRoleGoldLight
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = item.type.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TacticalCyan
                                    )
                                    if (item.description.isNotBlank()) {
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isEquipped) {
                                    OutlinedButton(
                                        onClick = { onEquipItem(item.type, null) },
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = GeForceLime, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Equipped", fontSize = 11.sp, color = GeForceLime)
                                    }
                                } else if (isOwned) {
                                    Button(
                                        onClick = { onEquipItem(item.type, item.id) },
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        modifier = Modifier
                                            .height(38.dp)
                                            .testTag("cosmetic_equip_${item.id}")
                                    ) {
                                        Text("Equip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (item.availability == ItemAvailability.FREE_SHOP) {
                                    Button(
                                        onClick = { onClaimFreeItem(item) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GeForceLime),
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        modifier = Modifier
                                            .height(38.dp)
                                            .testTag("cosmetic_claim_${item.id}")
                                    ) {
                                        Text("Claim Free", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.ExtraBold)
                                    }
                                } else {
                                    Icon(Icons.Default.Lock, contentDescription = "Exclusive", tint = AdminRoleGoldLight)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

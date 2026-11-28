package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloud.CloudUserInfo
import com.example.model.AdminConfig
import com.example.model.CommunityUserProfile
import com.example.model.CosmeticItem
import com.example.model.CosmeticItemType
import com.example.model.CosmeticPalettes
import com.example.model.ItemAvailability
import com.example.model.VerificationIconPreset
import com.example.ui.components.AdminRoleBadge
import com.example.ui.components.AdminRoleGoldDeep
import com.example.ui.components.AdminRoleGoldLight
import com.example.ui.components.CosmeticItemPreviewTile
import com.example.ui.components.FramedCommunityAvatar
import com.example.ui.components.ProfileNameTagChip
import com.example.ui.components.ProfileRoleCornerBadges
import com.example.ui.components.ProfileVerificationIconBadge
import com.example.ui.components.VerifiedRoleBadge
import com.example.ui.components.verificationPresetVector
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.MacroCoral
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import com.example.util.ButtonImageCodec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    cloudUser: CloudUserInfo?,
    cosmeticItems: List<CosmeticItem>,
    searchedUsers: List<CommunityUserProfile>,
    selectedUser: CommunityUserProfile?,
    selectedUserOwnedIds: Set<String>,
    isBusy: Boolean,
    onRefresh: () -> Unit,
    onCreateCosmeticItem: (
        name: String,
        description: String,
        type: CosmeticItemType,
        availability: ItemAvailability,
        primaryColorHex: Long,
        secondaryColorHex: Long,
        textColorHex: Long,
        borderWidthDp: Float,
        tagText: String,
        iconPreset: String,
        imageUri: Uri?,
        onCreated: () -> Unit
    ) -> Unit,
    onToggleAvailability: (CosmeticItem) -> Unit,
    onDeleteItem: (CosmeticItem) -> Unit,
    onSearchUsers: (String) -> Unit,
    onSelectUser: (CommunityUserProfile?) -> Unit,
    onGrantItem: (CosmeticItem) -> Unit,
    onRevokeItem: (CosmeticItem) -> Unit,
    onUpdateUserRoles: (CommunityUserProfile, adminRole: Boolean, verifiedRole: Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val isAuthorized = cloudUser?.isPrimaryAdmin() == true
    var activeTab by remember { mutableIntStateOf(0) } // 0=Create Items, 1=Users & Grant, 2=Roles & Badges

    LaunchedEffect(isAuthorized) {
        if (isAuthorized) {
            onRefresh()
            onSearchUsers("")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminRoleBadge(size = 26.dp)
                        Column {
                            Text(
                                text = "Admin Management",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isAuthorized) {
                                Text(
                                    text = AdminConfig.PRIMARY_ADMIN_EMAIL,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AdminRoleGoldLight
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAuthorized) {
                        IconButton(
                            onClick = {
                                onRefresh()
                                onSearchUsers("")
                            },
                            modifier = Modifier.testTag("admin_refresh_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Admin Data")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!isAuthorized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MacroCoral.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Restricted",
                            tint = MacroCoral,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Restricted Admin Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "This panel is strictly restricted to the authorized administrator account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(
                    Triple(0, "Items", Icons.Default.Add),
                    Triple(1, "Grant", Icons.Default.CardGiftcard),
                    Triple(2, "Roles", Icons.Default.VerifiedUser)
                )
                tabs.forEachIndexed { idx, (tabIdx, label, icon) ->
                    SegmentedButton(
                        selected = activeTab == tabIdx,
                        onClick = { activeTab = tabIdx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("admin_tab_${label.lowercase()}")
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (isBusy) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Syncing with cloud...", style = MaterialTheme.typography.labelSmall, color = AdminRoleGoldLight)
                }
            }

            when (activeTab) {
                0 -> AdminItemCreatorTab(
                    cosmeticItems = cosmeticItems,
                    isBusy = isBusy,
                    onCreateCosmeticItem = onCreateCosmeticItem,
                    onToggleAvailability = onToggleAvailability,
                    onDeleteItem = onDeleteItem
                )

                1 -> AdminUserDistributionTab(
                    cosmeticItems = cosmeticItems,
                    searchedUsers = searchedUsers,
                    selectedUser = selectedUser,
                    selectedUserOwnedIds = selectedUserOwnedIds,
                    onSearchUsers = onSearchUsers,
                    onSelectUser = onSelectUser,
                    onGrantItem = onGrantItem,
                    onRevokeItem = onRevokeItem
                )

                2 -> AdminRolesManagementTab(
                    searchedUsers = searchedUsers,
                    onSearchUsers = onSearchUsers,
                    onUpdateUserRoles = onUpdateUserRoles
                )
            }
        }
    }
}

@Composable
private fun AdminItemCreatorTab(
    cosmeticItems: List<CosmeticItem>,
    isBusy: Boolean,
    onCreateCosmeticItem: (
        name: String,
        description: String,
        type: CosmeticItemType,
        availability: ItemAvailability,
        primaryColorHex: Long,
        secondaryColorHex: Long,
        textColorHex: Long,
        borderWidthDp: Float,
        tagText: String,
        iconPreset: String,
        imageUri: Uri?,
        onCreated: () -> Unit
    ) -> Unit,
    onToggleAvailability: (CosmeticItem) -> Unit,
    onDeleteItem: (CosmeticItem) -> Unit
) {
    val context = LocalContext.current
    var itemType by remember { mutableStateOf(CosmeticItemType.FRAME) }
    var availability by remember { mutableStateOf(ItemAvailability.FREE_SHOP) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tagText by remember { mutableStateOf("FOUNDER") }
    var iconPreset by remember { mutableStateOf(VerificationIconPreset.VERIFIED.wireId) }
    var primaryColorHex by remember { mutableLongStateOf(CosmeticPalettes.PRIMARY_SWATCHES.first().second) }
    var secondaryColorHex by remember { mutableLongStateOf(CosmeticPalettes.SECONDARY_SWATCHES.first().second) }
    var textColorHex by remember { mutableLongStateOf(CosmeticPalettes.TEXT_SWATCHES.first().second) }
    var borderWidthDp by remember { mutableFloatStateOf(3.2f) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var previewImageB64 by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        pickedImageUri = uri
    }

    LaunchedEffect(pickedImageUri) {
        val uri = pickedImageUri
        previewImageB64 = if (uri != null) {
            ButtonImageCodec.encodeUriToBase64(
                context = context,
                uri = uri,
                cropSquare = true,
                maxDimPx = 128,
                maxEncodedBytes = 36 * 1024
            ).getOrNull()
        } else {
            null
        }
    }

    val livePreviewItem = remember(
        name, description, itemType, availability, primaryColorHex, secondaryColorHex,
        textColorHex, borderWidthDp, tagText, iconPreset, previewImageB64
    ) {
        CosmeticItem(
            id = "preview",
            name = name.ifBlank { "Custom ${itemType.label}" },
            description = description,
            type = itemType,
            availability = availability,
            primaryColorHex = primaryColorHex,
            secondaryColorHex = secondaryColorHex,
            textColorHex = textColorHex,
            borderWidthDp = borderWidthDp,
            tagText = tagText.ifBlank { "TAG" },
            iconPreset = iconPreset,
            imageBase64 = previewImageB64
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Profile Preview Card
        item(key = "live_preview") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdminRoleGoldLight.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE PROFILE PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminRoleGoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        AvailabilityPill(availability = availability)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0A0F18))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FramedCommunityAvatar(
                            avatarBase64 = null,
                            username = "PlayerOne",
                            frameItem = if (itemType == CosmeticItemType.FRAME) livePreviewItem else null,
                            size = 54.dp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "PlayerOne",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (itemType == CosmeticItemType.VERIFICATION_ICON) {
                                    ProfileVerificationIconBadge(item = livePreviewItem, size = 20.dp)
                                }
                            }
                            if (itemType == CosmeticItemType.NAME_TAG) {
                                ProfileNameTagChip(item = livePreviewItem)
                            } else {
                                Text(
                                    text = livePreviewItem.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB0BEC5)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Creator Form
        item(key = "creator_form") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Create Custom Cosmetic Item",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 1. Item Type Selector
                    Text("Item Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CosmeticItemType.entries.forEach { t ->
                            FilterChip(
                                selected = itemType == t,
                                onClick = { itemType = t },
                                label = { Text(t.label, fontSize = 11.sp, maxLines = 1) },
                                modifier = Modifier.testTag("admin_type_${t.name.lowercase()}")
                            )
                        }
                    }

                    // 2. Availability Selector (Free in Shop vs Exclusive)
                    Text("Availability", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AvailabilityOptionCard(
                            selected = availability == ItemAvailability.FREE_SHOP,
                            title = "Free in Shop",
                            subtitle = "Users can claim for free",
                            accent = GeForceLime,
                            icon = Icons.Default.Storefront,
                            onClick = { availability = ItemAvailability.FREE_SHOP },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_avail_free")
                        )
                        AvailabilityOptionCard(
                            selected = availability == ItemAvailability.EXCLUSIVE,
                            title = "Exclusive",
                            subtitle = "Admin manual grant only",
                            accent = AdminRoleGoldLight,
                            icon = Icons.Default.Lock,
                            onClick = { availability = ItemAvailability.EXCLUSIVE },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_avail_exclusive")
                        )
                    }

                    // 3. Name & Description
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(50) },
                        label = { Text("Item Name") },
                        placeholder = {
                            Text(
                                when (itemType) {
                                    CosmeticItemType.FRAME -> "e.g., Golden Aura Frame"
                                    CosmeticItemType.NAME_TAG -> "e.g., Founder Tag"
                                    CosmeticItemType.VERIFICATION_ICON -> "e.g., Pro Creator Star"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_item_name_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(220) },
                        label = { Text("Description (Optional)") },
                        placeholder = { Text("Short note shown in Shop & Wardrobe...") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Type-specific fields
                    if (itemType == CosmeticItemType.NAME_TAG) {
                        OutlinedTextField(
                            value = tagText,
                            onValueChange = { tagText = it.take(24) },
                            label = { Text("Badge Text") },
                            placeholder = { Text("e.g., FOUNDER, VIP, DBD PRO") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_tag_text_input")
                        )
                    }

                    if (itemType == CosmeticItemType.VERIFICATION_ICON) {
                        Text("Icon Symbol", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(VerificationIconPreset.entries) { preset ->
                                val selected = iconPreset == preset.wireId
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) Color(primaryColorHex).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) Color(primaryColorHex) else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { iconPreset = preset.wireId }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = verificationPresetVector(preset.wireId),
                                            contentDescription = preset.label,
                                            tint = if (selected) Color(primaryColorHex) else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(preset.label, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Color Swatches
                    ColorSwatchRow(
                        label = "Primary Color",
                        swatches = CosmeticPalettes.PRIMARY_SWATCHES,
                        selectedHex = primaryColorHex,
                        onSelect = { primaryColorHex = it }
                    )
                    ColorSwatchRow(
                        label = "Secondary / Glow Color",
                        swatches = CosmeticPalettes.SECONDARY_SWATCHES,
                        selectedHex = secondaryColorHex,
                        onSelect = { secondaryColorHex = it }
                    )
                    if (itemType != CosmeticItemType.FRAME) {
                        ColorSwatchRow(
                            label = if (itemType == CosmeticItemType.NAME_TAG) "Text Color" else "Icon Color",
                            swatches = CosmeticPalettes.TEXT_SWATCHES,
                            selectedHex = textColorHex,
                            onSelect = { textColorHex = it }
                        )
                    } else {
                        Column {
                            Text(
                                text = "Frame Thickness: ${"%.1f".format(borderWidthDp)} dp",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Slider(
                                value = borderWidthDp,
                                onValueChange = { borderWidthDp = it },
                                valueRange = 1.5f..6.5f
                            )
                        }
                    }

                    // Optional Custom Artwork Upload (Photo Picker — zero storage permission!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("admin_upload_artwork_btn")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (pickedImageUri != null) "Change Artwork" else "Upload Custom Image (Optional)",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (pickedImageUri != null) {
                            IconButton(onClick = { pickedImageUri = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Image", tint = MacroCoral)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onCreateCosmeticItem(
                                name.ifBlank { "Custom ${itemType.label}" },
                                description,
                                itemType,
                                availability,
                                primaryColorHex,
                                secondaryColorHex,
                                textColorHex,
                                borderWidthDp,
                                if (itemType == CosmeticItemType.NAME_TAG) tagText.ifBlank { name.ifBlank { "VIP" } } else "",
                                iconPreset,
                                pickedImageUri
                            ) {
                                name = ""
                                description = ""
                                pickedImageUri = null
                                previewImageB64 = null
                            }
                        },
                        enabled = !isBusy && name.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminRoleGoldDeep),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_create_item_submit_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Create ${itemType.label}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Created Items Catalog
        item(key = "catalog_header") {
            Text(
                text = "Created Items (${cosmeticItems.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (cosmeticItems.isEmpty()) {
            item(key = "catalog_empty") {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No custom items created yet. Create a Profile Frame, Name Tag, or Verification Icon above!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(cosmeticItems, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
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
                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.type.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalCyan
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AvailabilityPill(availability = item.availability)
                                TextButton(
                                    onClick = { onToggleAvailability(item) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = if (item.availability == ItemAvailability.FREE_SHOP) "Make Exclusive" else "Put in Free Shop",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { onDeleteItem(item) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Item", tint = MacroCoral)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUserDistributionTab(
    cosmeticItems: List<CosmeticItem>,
    searchedUsers: List<CommunityUserProfile>,
    selectedUser: CommunityUserProfile?,
    selectedUserOwnedIds: Set<String>,
    onSearchUsers: (String) -> Unit,
    onSelectUser: (CommunityUserProfile?) -> Unit,
    onGrantItem: (CosmeticItem) -> Unit,
    onRevokeItem: (CosmeticItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<CosmeticItemType?>(null) }

    val itemById = remember(cosmeticItems) { cosmeticItems.associateBy { it.id } }
    val filteredItems = remember(cosmeticItems, filterType) {
        if (filterType == null) cosmeticItems else cosmeticItems.filter { it.type == filterType }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "user_search_bar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search user by username") },
                    placeholder = { Text("Enter username...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_user_search_input")
                )
                Button(
                    onClick = { onSearchUsers(query) },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("admin_user_search_btn")
                ) {
                    Text("Find")
                }
            }
        }

        if (selectedUser != null) {
            item(key = "selected_user_panel") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, AdminRoleGoldLight.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FramedCommunityAvatar(
                                avatarBase64 = selectedUser.avatarBase64,
                                username = selectedUser.username,
                                frameItem = selectedUser.equippedFrameId?.let { itemById[it] },
                                size = 44.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Distributing to: ${selectedUser.username}",
                                    fontWeight = FontWeight.Bold,
                                    color = AdminRoleGoldLight
                                )
                                Text(
                                    text = "Owns ${selectedUserOwnedIds.size} item(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onSelectUser(null) }) {
                                Text("Change User")
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = filterType == null,
                                    onClick = { filterType = null },
                                    label = { Text("All (${cosmeticItems.size})") }
                                )
                            }
                            items(CosmeticItemType.entries) { t ->
                                FilterChip(
                                    selected = filterType == t,
                                    onClick = { filterType = t },
                                    label = { Text(t.pluralLabel) }
                                )
                            }
                        }
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item(key = "no_items_to_grant") {
                    Text(
                        text = "No cosmetic items available in this category yet. Create some in the Items tab first!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(filteredItems, key = { "grant_${it.id}" }) { item ->
                    val owned = item.id in selectedUserOwnedIds
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CosmeticItemPreviewTile(item = item)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.type.label, style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                                    AvailabilityPill(availability = item.availability)
                                }
                            }
                            if (owned) {
                                OutlinedButton(
                                    onClick = { onRevokeItem(item) },
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Revoke", fontSize = 12.sp, color = MacroCoral)
                                }
                            } else {
                                Button(
                                    onClick = { onGrantItem(item) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AdminRoleGoldDeep),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("admin_grant_${item.id}")
                                ) {
                                    Text("Grant", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item(key = "users_results_title") {
                Text(
                    text = "Matching Users (${searchedUsers.size}) — Tap a user to assign items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (searchedUsers.isEmpty()) {
                item(key = "users_empty") {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null, tint = TacticalCyan)
                            Text(
                                text = "No users found matching \"$query\". Users appear once they sign in and create a profile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(searchedUsers, key = { it.uid }) { user ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                            .clickable { onSelectUser(user) }
                            .testTag("admin_user_row_${user.uid}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FramedCommunityAvatar(
                                avatarBase64 = user.avatarBase64,
                                username = user.username,
                                frameItem = user.equippedFrameId?.let { itemById[it] },
                                size = 44.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.username, fontWeight = FontWeight.Bold)
                                Text(
                                    text = user.bio.ifBlank { "Tap to assign frames, tags & icons" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            ProfileRoleCornerBadges(
                                adminRole = user.adminRole,
                                verifiedRole = user.verifiedRole,
                                badgeSize = 24.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRolesManagementTab(
    searchedUsers: List<CommunityUserProfile>,
    onSearchUsers: (String) -> Unit,
    onUpdateUserRoles: (CommunityUserProfile, adminRole: Boolean, verifiedRole: Boolean) -> Unit
) {
    var query by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Role Legend Banner
        item(key = "role_legend") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AdminRoleGoldLight.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Profile Corner Role Badges",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AdminRoleBadge(showLabel = true)
                        Text(
                            text = "White wrench on gold background",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VerifiedRoleBadge(showLabel = true)
                        Text(
                            text = "White checkmark on blue background",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "role_search_bar") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search user by username") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_roles_search_input")
                )
                Button(
                    onClick = { onSearchUsers(query) },
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("Find")
                }
            }
        }

        items(searchedUsers, key = { "role_${it.uid}" }) { user ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(end = 68.dp)
                        ) {
                            CommunityAvatar(
                                avatarBase64 = user.avatarBase64,
                                username = user.username,
                                size = 44.dp
                            )
                            Column {
                                Text(user.username, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "UID: ${user.uid.take(12)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AdminRoleBadge(size = 26.dp)
                                Column {
                                    Text("Admin Role", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Gold wrench badge on profile corner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = user.adminRole,
                                onCheckedChange = { checked ->
                                    onUpdateUserRoles(user, checked, user.verifiedRole)
                                },
                                modifier = Modifier.testTag("admin_toggle_admin_role_${user.uid}")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VerifiedRoleBadge(size = 26.dp)
                                Column {
                                    Text("Verified Role", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Blue checkmark badge on profile corner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = user.verifiedRole,
                                onCheckedChange = { checked ->
                                    onUpdateUserRoles(user, user.adminRole, checked)
                                },
                                modifier = Modifier.testTag("admin_toggle_verified_role_${user.uid}")
                            )
                        }
                    }

                    // Live preview of the user's top-right corner role indicators
                    ProfileRoleCornerBadges(
                        adminRole = user.adminRole,
                        verifiedRole = user.verifiedRole,
                        badgeSize = 26.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailabilityOptionCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .border(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selected) accent else MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun AvailabilityPill(availability: ItemAvailability) {
    val isFree = availability == ItemAvailability.FREE_SHOP
    val color = if (isFree) GeForceLime else AdminRoleGoldLight
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = availability.shortBadge,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ColorSwatchRow(
    label: String,
    swatches: List<Pair<String, Long>>,
    selectedHex: Long,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(swatches, key = { it.first }) { (name, hex) ->
                val isSelected = selectedHex == hex
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = name,
                            tint = if (hex == 0xFFFFFFFFL || hex == 0xFFFFD54FL || hex == 0xFFC6FF00L) Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

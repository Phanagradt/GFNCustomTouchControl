package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cloud.CloudUserInfo
import com.example.model.BindingDomain
import com.example.model.ControllerProfile
import com.example.model.GameGenre
import com.example.model.LocalNexusStrings
import com.example.model.ProfileTemplate
import com.example.ui.CloudSaveUiState
import com.example.ui.ShareCodeUiState
import com.example.ui.components.CloudSaveCompactBanner
import com.example.ui.components.ShareCodeExportDialog
import com.example.ui.components.ShareCodeImportDialog
import com.example.ui.theme.MacroViolet
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber

@Composable
fun ProfilesScreen(
    profiles: List<ControllerProfile>,
    activeProfileId: String?,
    cloudUser: CloudUserInfo?,
    cloudSyncState: CloudSaveUiState,
    shareCodeState: ShareCodeUiState,
    onCloudSignIn: () -> Unit,
    onCloudSyncNow: () -> Unit,
    onCreateShareCode: (ControllerProfile) -> Unit,
    onImportByShareCode: (String) -> Unit,
    onDismissShareCode: () -> Unit,
    onSetActive: (String) -> Unit,
    onEditProfile: (String) -> Unit,
    onDuplicateProfile: (ControllerProfile) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onCreateProfile: (name: String, gameTitle: String, genre: GameGenre, template: ProfileTemplate) -> Unit,
    onImportJsonClick: () -> Unit,
    onExportProfileClick: (ControllerProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalNexusStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<GameGenre?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showShareExportDialog by remember { mutableStateOf(false) }
    var showShareImportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(shareCodeState) {
        if (shareCodeState is ShareCodeUiState.Imported) {
            showShareImportDialog = false
            onDismissShareCode()
        }
    }

    val filtered = remember(profiles, searchQuery, selectedGenre) {
        profiles.filter { profile ->
            val matchesGenre = selectedGenre == null || profile.genre == selectedGenre
            val matchesQuery = searchQuery.isBlank() ||
                profile.name.contains(searchQuery, ignoreCase = true) ||
                profile.gameTitle.contains(searchQuery, ignoreCase = true)
            matchesGenre && matchesQuery
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 880.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = strings.profilesTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.profilesSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onDismissShareCode()
                                showShareImportDialog = true
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("profiles_enter_share_code_button")
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.shareCodeEnterButton, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onImportJsonClick,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("profiles_import_json_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.profilesImport, maxLines = 1)
                        }
                    }
                }

                CloudSaveCompactBanner(
                    user = cloudUser,
                    syncState = cloudSyncState,
                    onSignInClick = onCloudSignIn,
                    onSyncNowClick = onCloudSyncNow
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.profilesSearchHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profiles_search_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedGenre == null,
                            onClick = { selectedGenre = null },
                            label = { Text(strings.commonAll) }
                        )
                    }
                    items(GameGenre.entries) { genre ->
                        FilterChip(
                            selected = selectedGenre == genre,
                            onClick = { selectedGenre = if (selectedGenre == genre) null else genre },
                            label = { Text(genre.displayName) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = strings.profilesEmptyTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = strings.profilesEmptySub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { profile ->
                        ProfileCard(
                            profile = profile,
                            isActive = profile.id == activeProfileId,
                            canDelete = profiles.size > 1,
                            onSetActive = { onSetActive(profile.id) },
                            onEdit = { onEditProfile(profile.id) },
                            onShareCode = {
                                showShareExportDialog = true
                                if (cloudUser != null) {
                                    onCreateShareCode(profile)
                                }
                            },
                            onDuplicate = { onDuplicateProfile(profile) },
                            onExport = { onExportProfileClick(profile) },
                            onDelete = { onDeleteProfile(profile.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_profile_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create New Profile")
        }
    }

    if (showCreateDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, game, genre, template ->
                showCreateDialog = false
                onCreateProfile(name, game, genre, template)
            }
        )
    }

    if (showShareExportDialog) {
        ShareCodeExportDialog(
            state = shareCodeState,
            isSignedIn = cloudUser != null,
            onSignInClick = {
                showShareExportDialog = false
                onDismissShareCode()
                onCloudSignIn()
            },
            onDismiss = {
                showShareExportDialog = false
                onDismissShareCode()
            }
        )
    }

    if (showShareImportDialog) {
        ShareCodeImportDialog(
            state = shareCodeState,
            isSignedIn = cloudUser != null,
            onSignInClick = {
                showShareImportDialog = false
                onDismissShareCode()
                onCloudSignIn()
            },
            onImportCode = onImportByShareCode,
            onDismiss = {
                showShareImportDialog = false
                onDismissShareCode()
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: ControllerProfile,
    isActive: Boolean,
    canDelete: Boolean,
    onSetActive: () -> Unit,
    onEdit: () -> Unit,
    onShareCode: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalNexusStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val gamepadCount = profile.controls.count {
        it.binding.domain == BindingDomain.CONTROLLER || it.binding.domain == BindingDomain.ANALOG_AXIS
    }
    val mouseCount = profile.controls.count { it.binding.domain == BindingDomain.MOUSE }
    val keyCount = profile.controls.count { it.binding.domain == BindingDomain.KEYBOARD }
    val comboCount = profile.controls.count { it.binding.domain == BindingDomain.COMBO }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) primary else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large
            )
            .testTag("profile_card_${profile.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isActive) {
                            Surface(
                                color = primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = strings.commonActive,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${profile.gameTitle} • ${profile.genre.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(
                        onClick = onShareCode,
                        modifier = Modifier.testTag("share_code_profile_${profile.id}")
                    ) {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = strings.shareCodeAction,
                            tint = TacticalCyan
                        )
                    }
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.testTag("duplicate_profile_${profile.id}")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Profile")
                    }
                    IconButton(
                        onClick = onExport,
                        modifier = Modifier.testTag("export_profile_${profile.id}")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export JSON")
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_profile_${profile.id}")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = strings.commonDelete,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0A0F18))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                ) {
                    ProfileMiniMapCanvas(
                        profile = profile,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (gamepadCount > 0) item { StatBadge("$gamepadCount PAD", primary) }
                        if (mouseCount > 0) item { StatBadge("$mouseCount MSE", TurboAmber) }
                        if (keyCount > 0) item { StatBadge("$keyCount KEY", TacticalCyan) }
                        if (comboCount > 0) item { StatBadge("$comboCount MACRO", MacroViolet) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isActive) {
                            OutlinedButton(
                                onClick = onSetActive,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("activate_profile_${profile.id}")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.profilesArm)
                            }
                        }
                        Button(
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("edit_profile_${profile.id}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.commonEdit)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, gameTitle: String, genre: GameGenre, template: ProfileTemplate) -> Unit
) {
    val strings = LocalNexusStrings.current
    var name by remember { mutableStateOf("") }
    var gameTitle by remember { mutableStateOf("") }
    var template by remember { mutableStateOf(ProfileTemplate.PC_MOUSE_WASD) }
    var genre by remember { mutableStateOf(template.defaultGenre) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.profilesNewTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.profilesStarterTemplate, style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ProfileTemplate.entries) { entry ->
                        FilterChip(
                            selected = template == entry,
                            onClick = {
                                template = entry
                                genre = entry.defaultGenre
                            },
                            label = { Text(entry.title) }
                        )
                    }
                }
                Text(
                    text = template.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.profilesNameLabel) },
                    placeholder = { Text(template.title) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_profile_name_input")
                )
                OutlinedTextField(
                    value = gameTitle,
                    onValueChange = { gameTitle = it },
                    label = { Text(strings.profilesTargetGameLabel) },
                    placeholder = { Text("e.g., Dead by Daylight / Roblox") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_profile_game_input")
                )
                Text(strings.profilesGenreTag, style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GameGenre.entries) { entry ->
                        FilterChip(
                            selected = genre == entry,
                            onClick = { genre = entry },
                            label = { Text(entry.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, gameTitle, genre, template) },
                modifier = Modifier.testTag("confirm_create_profile_button")
            ) {
                Text(strings.profilesCreateAndEdit)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.commonCancel)
            }
        }
    )
}

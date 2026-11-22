package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CloudLayoutMapper
import com.example.model.BindingDomain
import com.example.model.ControllerProfile
import com.example.model.GameGenre
import com.example.model.ProfileTemplate
import com.example.ui.ImportCodeUiState
import com.example.ui.ShareCodeUiState
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MacroViolet
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import kotlinx.coroutines.delay

@Composable
fun ProfilesScreen(
    profiles: List<ControllerProfile>,
    activeProfileId: String?,
    shareCodeState: ShareCodeUiState,
    importCodeState: ImportCodeUiState,
    onSetActive: (String) -> Unit,
    onEditProfile: (String) -> Unit,
    onDuplicateProfile: (ControllerProfile) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onCreateProfile: (name: String, gameTitle: String, genre: GameGenre, template: ProfileTemplate) -> Unit,
    onImportJsonClick: () -> Unit,
    onExportJsonClick: (ControllerProfile) -> Unit,
    onGenerateShareCode: (ControllerProfile) -> Unit,
    onResetShareCode: () -> Unit,
    onRedeemShareCode: (String) -> Unit,
    onResetImportCode: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<GameGenre?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportSheetProfile by remember { mutableStateOf<ControllerProfile?>(null) }

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Controller & Key Profiles",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Cloud-backed layouts & 5-minute friend share codes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            onResetImportCode()
                            showImportDialog = true
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("profiles_import_json_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search game or layout name...") },
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
                            label = { Text("All") }
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
                            text = "No matching controller profiles",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Create a new layout or clear your genre filter.",
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
                            onDuplicate = { onDuplicateProfile(profile) },
                            onExport = {
                                onResetShareCode()
                                exportSheetProfile = profile
                                onGenerateShareCode(profile)
                            },
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

    if (exportSheetProfile != null) {
        val target = exportSheetProfile!!
        ExportShareProfileDialog(
            profile = target,
            shareCodeState = shareCodeState,
            onRegenerateCode = { onGenerateShareCode(target) },
            onExportJsonFile = {
                exportSheetProfile = null
                onResetShareCode()
                onExportJsonClick(target)
            },
            onCopyCode = { code -> onShowMessage("Copied share code $code (valid 5 min)") },
            onDismiss = {
                exportSheetProfile = null
                onResetShareCode()
            }
        )
    }

    if (showImportDialog) {
        ImportLayoutDialog(
            importCodeState = importCodeState,
            onRedeemCode = onRedeemShareCode,
            onImportJsonFile = {
                showImportDialog = false
                onResetImportCode()
                onImportJsonClick()
            },
            onDismiss = {
                showImportDialog = false
                onResetImportCode()
            }
        )
    }
}

@Composable
private fun ExportShareProfileDialog(
    profile: ControllerProfile,
    shareCodeState: ShareCodeUiState,
    onRegenerateCode: () -> Unit,
    onExportJsonFile: () -> Unit,
    onCopyCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var remainingSeconds by remember { mutableLongStateOf(300L) }

    val readyTicket = (shareCodeState as? ShareCodeUiState.Ready)?.ticket
    LaunchedEffect(readyTicket?.code, readyTicket?.expiresAtMs) {
        if (readyTicket != null) {
            while (true) {
                val secs = ((readyTicket.expiresAtMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                remainingSeconds = secs
                if (secs == 0L) break
                delay(1000L)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Share, contentDescription = null, tint = GeForceLime)
                Text("Export & Share \"${profile.name}\"", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = Color(0xFF0E1624),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GeForceLime.copy(alpha = 0.45f), MaterialTheme.shapes.medium)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "5-MINUTE FRIEND SHARE CODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = GeForceLime,
                            fontWeight = FontWeight.Bold
                        )

                        when (shareCodeState) {
                            is ShareCodeUiState.Idle,
                            is ShareCodeUiState.Generating -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = GeForceLime,
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = "Creating temporary share code...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9FB0C7)
                                )
                            }
                            is ShareCodeUiState.Ready -> {
                                val ticket = shareCodeState.ticket
                                val mins = remainingSeconds / 60
                                val secs = remainingSeconds % 60
                                val expired = remainingSeconds == 0L

                                Text(
                                    text = ticket.code,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 30.sp,
                                    letterSpacing = 6.sp,
                                    color = if (expired) Color(0xFF8191A8) else Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("share_code_value")
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = if (expired) MaterialTheme.colorScheme.error else TacticalCyan,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (expired) {
                                            "Code expired after 5 minutes"
                                        } else {
                                            "Valid for %02d:%02d".format(mins, secs)
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (expired) MaterialTheme.colorScheme.error else TacticalCyan
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("NexusPad Share Code", ticket.code))
                                            onCopyCode(ticket.code)
                                        },
                                        enabled = !expired,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("copy_share_code_button")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Copy Code")
                                    }
                                    if (expired) {
                                        OutlinedButton(
                                            onClick = onRegenerateCode,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                        ) {
                                            Text("New Code")
                                        }
                                    }
                                }

                                Text(
                                    text = "Anyone who imports this code before the timer runs out keeps a full copy permanently.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9FB0C7),
                                    textAlign = TextAlign.Center
                                )
                            }
                            is ShareCodeUiState.Error -> {
                                Text(
                                    text = shareCodeState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                OutlinedButton(onClick = onRegenerateCode) {
                                    Text("Retry Code")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = onExportJsonFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("export_json_file_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Backup JSON File")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ImportLayoutDialog(
    importCodeState: ImportCodeUiState,
    onRedeemCode: (String) -> Unit,
    onImportJsonFile: () -> Unit,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    val isLoading = importCodeState is ImportCodeUiState.Loading
    val redeemedSuccess = (importCodeState as? ImportCodeUiState.Success)?.redeemed

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.QrCode2, contentDescription = null, tint = TacticalCyan)
                Text("Import Controller Layout")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter a friend's 6-character Share Code before its 5-minute window expires. Once imported, the layout is saved permanently to your account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { next ->
                        codeInput = CloudLayoutMapper.sanitizeShareCode(next).take(12)
                    },
                    label = { Text("Friend's Share Code") },
                    placeholder = { Text("e.g. K7M9PQ") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_share_code_input")
                )

                if (importCodeState is ImportCodeUiState.Error) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = importCodeState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (redeemedSuccess != null) {
                    Surface(
                        color = GeForceLime.copy(alpha = 0.16f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GeForceLime, MaterialTheme.shapes.small)
                    ) {
                        Text(
                            text = "Imported \"${redeemedSuccess.profile.name}\" from ${redeemedSuccess.authorName}! It's now saved permanently in your Profiles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GeForceLime,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = { onRedeemCode(codeInput) },
                    enabled = !isLoading && codeInput.length >= 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("redeem_share_code_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Importing Layout...")
                    } else {
                        Text("Import by Code")
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = onImportJsonFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("import_json_file_button")
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import from JSON File")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (redeemedSuccess != null) "Done" else "Close")
            }
        }
    )
}

@Composable
private fun ProfileCard(
    profile: ControllerProfile,
    isActive: Boolean,
    canDelete: Boolean,
    onSetActive: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
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
                color = if (isActive) GeForceLime else MaterialTheme.colorScheme.outlineVariant,
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
                                color = GeForceLime.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GeForceLime,
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
                        onClick = onDuplicate,
                        modifier = Modifier.testTag("duplicate_profile_${profile.id}")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Profile")
                    }
                    IconButton(
                        onClick = onExport,
                        modifier = Modifier.testTag("export_profile_${profile.id}")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Code or Export JSON")
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_profile_${profile.id}")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete Profile",
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
                        if (gamepadCount > 0) item { StatBadge("$gamepadCount PAD", GeForceLime) }
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
                                Text("Arm")
                            }
                        }
                        Button(
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("edit_profile_${profile.id}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit")
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
    var name by remember { mutableStateOf("") }
    var gameTitle by remember { mutableStateOf("") }
    var template by remember { mutableStateOf(ProfileTemplate.PC_MOUSE_WASD) }
    var genre by remember { mutableStateOf(template.defaultGenre) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New GeForce NOW Layout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Starter Template", style = MaterialTheme.typography.labelMedium)
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
                    label = { Text("Profile Name") },
                    placeholder = { Text(template.title) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_profile_name_input")
                )
                OutlinedTextField(
                    value = gameTitle,
                    onValueChange = { gameTitle = it },
                    label = { Text("Target Game") },
                    placeholder = { Text("e.g., Dead by Daylight / Apex") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_profile_game_input")
                )
                Text("Genre Tag", style = MaterialTheme.typography.labelMedium)
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
                Text("Create & Edit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

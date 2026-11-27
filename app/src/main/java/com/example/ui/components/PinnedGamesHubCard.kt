package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ControlAccent
import com.example.model.ControllerProfile
import com.example.model.GameFilterState
import com.example.model.GfnRegionPreset
import com.example.model.PinnedGame
import com.example.model.PinnedGameCatalog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TacticalCyan
import com.example.webplayer.RegionPingMonitor
import com.example.webplayer.RegionPingResult
import java.util.UUID

/**
 * Cockpit Pinned Games Hub: lets players pin favorite GeForce NOW titles linked to a
 * specific Controller Layout Profile, Freestyle Filter Slot, and Built-in Region Bypass.
 * Tapping Play auto-applies all 3 settings and opens Web Play Mode immediately.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PinnedGamesHubCard(
    pinnedGames: List<PinnedGame>,
    profiles: List<ControllerProfile>,
    activeProfileId: String?,
    filterState: GameFilterState,
    defaultRegion: GfnRegionPreset,
    onLaunchPinnedGame: (PinnedGame) -> Unit,
    onSavePinnedGame: (PinnedGame) -> Unit,
    onDeletePinnedGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()
    val regionPings by RegionPingMonitor.pings.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        RegionPingMonitor.refreshIfStale(scope)
    }

    var editingGame by remember { mutableStateOf<PinnedGame?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    fun openCreateDialog(prefillTitle: String = "") {
        isCreatingNew = true
        editingGame = PinnedGame(
            id = UUID.randomUUID().toString(),
            title = prefillTitle,
            profileId = activeProfileId ?: profiles.firstOrNull()?.id.orEmpty(),
            filterSlot = filterState.activeSlot.coerceIn(0, 3),
            regionPreset = defaultRegion,
            accentHex = ControlAccent.LIME.hex,
            customLaunchUrl = ""
        )
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .testTag("cockpit_pinned_games_hub")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = primary.copy(alpha = 0.16f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "PINNED GAMES HUB",
                            style = MaterialTheme.typography.labelMedium,
                            color = primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Auto-loads layout, filter & region in 1 tap",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = { openCreateDialog() },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("pinned_games_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Pin Game", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (pinnedGames.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Pin your favorite GeForce NOW games to launch with the exact layout profile, Freestyle filter, and server region.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PinnedGameCatalog.quickSuggestions.take(4).forEach { title ->
                                FilterChip(
                                    selected = false,
                                    onClick = { openCreateDialog(prefillTitle = title) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    label = { Text(title, fontSize = 12.sp) },
                                    modifier = Modifier.testTag("quick_pin_${title.lowercase().replace(" ", "_")}")
                                )
                            }
                        }
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(pinnedGames, key = { it.id }) { game ->
                        val linkedProfile = profiles.firstOrNull { it.id == game.profileId }
                        val filterLabel = when (game.filterSlot) {
                            1 -> "S1 • ${filterState.styleForSlot(1).name}"
                            2 -> "S2 • ${filterState.styleForSlot(2).name}"
                            3 -> "S3 • ${filterState.styleForSlot(3).name}"
                            else -> "Filter Off"
                        }
                        PinnedGameCardItem(
                            game = game,
                            profileName = linkedProfile?.name ?: "Default Layout",
                            filterLabel = filterLabel,
                            pingResult = regionPings[game.regionPreset],
                            onPlay = { onLaunchPinnedGame(game) },
                            onEdit = {
                                isCreatingNew = false
                                editingGame = game
                            },
                            onDelete = { onDeletePinnedGame(game.id) }
                        )
                    }
                }
            }
        }
    }

    val dialogTarget = editingGame
    if (dialogTarget != null) {
        PinnedGameEditorDialog(
            initialGame = dialogTarget,
            isNew = isCreatingNew,
            profiles = profiles,
            filterState = filterState,
            regionPings = regionPings,
            onDismiss = { editingGame = null },
            onSave = { saved ->
                onSavePinnedGame(saved)
                editingGame = null
            }
        )
    }
}

@Composable
private fun PinnedGameCardItem(
    game: PinnedGame,
    profileName: String,
    filterLabel: String,
    pingResult: RegionPingResult?,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardAccent = Color(game.accentHex)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .width(248.dp)
            .border(1.dp, cardAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .testTag("pinned_game_card_${game.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(cardAccent, TacticalCyan)
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("pinned_game_edit_${game.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit pinned game",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("pinned_game_delete_${game.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove pinned game",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Linked Profile, Filter, and Region pills
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    PinnedSpecRow(
                        icon = Icons.Default.Gamepad,
                        text = profileName,
                        tint = cardAccent
                    )
                    PinnedSpecRow(
                        icon = Icons.Default.Tune,
                        text = filterLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = TacticalCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (game.regionPreset.isEnabled) {
                                "${game.regionPreset.shortCode} • ${game.regionPreset.displayName.substringBefore(" (")}"
                            } else {
                                "Local Server"
                            },
                            fontSize = 11.sp,
                            color = TacticalCyan,
                            fontFamily = JetBrainsMonoFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        RegionPingPill(result = pingResult)
                    }
                }

                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardAccent,
                        contentColor = Color(0xFF0A0D12)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("pinned_game_play_${game.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Play in Web Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PinnedSpecRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            fontSize = 11.sp,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PinnedGameEditorDialog(
    initialGame: PinnedGame,
    isNew: Boolean,
    profiles: List<ControllerProfile>,
    filterState: GameFilterState,
    regionPings: Map<GfnRegionPreset, RegionPingResult>,
    onDismiss: () -> Unit,
    onSave: (PinnedGame) -> Unit
) {
    var title by remember(initialGame.id) { mutableStateOf(initialGame.title) }
    var selectedProfileId by remember(initialGame.id) {
        mutableStateOf(
            if (profiles.any { it.id == initialGame.profileId }) {
                initialGame.profileId
            } else {
                profiles.firstOrNull()?.id.orEmpty()
            }
        )
    }
    var selectedFilterSlot by remember(initialGame.id) { mutableIntStateOf(initialGame.filterSlot.coerceIn(0, 3)) }
    var selectedRegion by remember(initialGame.id) { mutableStateOf(initialGame.regionPreset) }
    var selectedAccentHex by remember(initialGame.id) { mutableLongStateOf(initialGame.accentHex) }
    var customUrl by remember(initialGame.id) { mutableStateOf(initialGame.customLaunchUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) "Pin Game Preset" else "Edit Pinned Game",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Game Title") },
                    placeholder = { Text("e.g. Fortnite, Apex Legends") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pinned_editor_title_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PinnedGameCatalog.quickSuggestions.take(6).forEach { suggestion ->
                        FilterChip(
                            selected = title.equals(suggestion, ignoreCase = true),
                            onClick = { title = suggestion },
                            label = { Text(suggestion, fontSize = 11.sp) }
                        )
                    }
                }

                // Linked Layout Profile
                Text(
                    text = "AUTO-LOAD LAYOUT PROFILE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    profiles.forEach { profile ->
                        FilterChip(
                            selected = selectedProfileId == profile.id,
                            onClick = { selectedProfileId = profile.id },
                            label = {
                                Text(
                                    text = profile.name,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                // Linked Freestyle Filter Slot
                Text(
                    text = "AUTO-LOAD FREESTYLE FILTER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val slotOptions = listOf(
                        0 to "Off",
                        1 to "S1 (${filterState.styleForSlot(1).name})",
                        2 to "S2 (${filterState.styleForSlot(2).name})",
                        3 to "S3 (${filterState.styleForSlot(3).name})"
                    )
                    slotOptions.forEach { (slot, label) ->
                        FilterChip(
                            selected = selectedFilterSlot == slot,
                            onClick = { selectedFilterSlot = slot },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Linked Region Bypass
                Text(
                    text = "AUTO-LOAD REGION BYPASS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalCyan
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GfnRegionPreset.entries.forEach { region ->
                        FilterChip(
                            selected = selectedRegion == region,
                            onClick = { selectedRegion = region },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = if (region == GfnRegionPreset.OFF) {
                                            "Off"
                                        } else {
                                            "${region.shortCode} • ${region.displayName.substringBefore(" (")}"
                                        },
                                        fontSize = 11.sp
                                    )
                                    RegionPingPill(result = regionPings[region])
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalCyan.copy(alpha = 0.22f),
                                selectedLabelColor = TacticalCyan
                            )
                        )
                    }
                }

                // Accent Color
                Text(
                    text = "CARD ACCENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ControlAccent.entries.forEach { swatch ->
                        val isChosen = selectedAccentHex == swatch.hex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(swatch.hex))
                                .border(
                                    width = if (isChosen) 2.5.dp else 1.dp,
                                    color = if (isChosen) Color.White else Color.Black.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { selectedAccentHex = swatch.hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChosen) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    label = { Text("Direct GeForce NOW URL (Optional)") },
                    placeholder = { Text("https://play.geforcenow.com/...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim().ifEmpty { "GeForce NOW Game" }
                    onSave(
                        initialGame.copy(
                            title = cleanTitle,
                            profileId = selectedProfileId,
                            filterSlot = selectedFilterSlot,
                            regionPreset = selectedRegion,
                            accentHex = selectedAccentHex,
                            customLaunchUrl = customUrl.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("pinned_editor_save_button")
            ) {
                Text(if (isNew) "Pin Game" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

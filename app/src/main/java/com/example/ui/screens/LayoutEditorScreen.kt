package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.OverlaySettings
import com.example.model.AxisPair
import com.example.model.BindingDomain
import com.example.model.CommandCatalog
import com.example.model.ControlAccent
import com.example.model.ControlShape
import com.example.model.ControlType
import com.example.model.GameGenre
import com.example.model.InputBinding
import com.example.model.TelemetryEvent
import com.example.model.TriggerBehavior
import com.example.model.VirtualControl
import com.example.ui.EditorUiState
import com.example.ui.components.PositionedVirtualControl
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutEditorScreen(
    editorState: EditorUiState,
    settings: OverlaySettings,
    pressedIds: Set<String>,
    toggledIds: Set<String>,
    latestTelemetry: TelemetryEvent?,
    onSelectControl: (String?) -> Unit,
    onMoveControl: (controlId: String, dxPx: Float, dyPx: Float, widthPx: Float, heightPx: Float) -> Unit,
    onUpdateControl: (VirtualControl) -> Unit,
    onAddControl: (ControlType, InputBinding) -> Unit,
    onDuplicateControl: () -> Unit,
    onDeleteControl: () -> Unit,
    onTogglePlayTest: () -> Unit,
    onUndo: () -> Unit,
    onSave: () -> Unit,
    onUpdateProfileMeta: (name: String, gameTitle: String, genre: GameGenre) -> Unit,
    onBack: () -> Unit
) {
    val profile = editorState.profile
    var showAddControlSheet by remember { mutableStateOf(false) }
    var showInspectorSheet by remember { mutableStateOf(false) }
    var showCommandRebindSheet by remember { mutableStateOf(false) }
    var showMetaDialog by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val handleBack = {
        if (editorState.hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(onBack = handleBack)

    if (profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading Layout Studio...")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A10))
    ) {
        // Top Studio Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = handleBack,
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showMetaDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile Details",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (editorState.isPlayTestMode) {
                            latestTelemetry?.let { "TEST: ${it.codeLabel} ${it.actionName}" } ?: "PLAY-TEST MODE ACTIVE"
                        } else {
                            "${profile.controls.size} controls • Drag to position"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (editorState.isPlayTestMode) TacticalCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onUndo,
                    enabled = editorState.undoStack.isNotEmpty(),
                    modifier = Modifier.testTag("editor_undo_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }

                FilterChip(
                    selected = editorState.isPlayTestMode,
                    onClick = onTogglePlayTest,
                    label = { Text(if (editorState.isPlayTestMode) "Testing" else "Test") },
                    leadingIcon = {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("editor_playtest_toggle")
                )

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (editorState.hasUnsavedChanges) GeForceLime else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (editorState.hasUnsavedChanges) Color(0xFF0A1400) else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("editor_save_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Interactive Canvas Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("editor_canvas")
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            Image(
                painter = painterResource(id = R.drawable.img_game_arena_bg),
                contentDescription = "GeForce NOW Stream Canvas",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x660B0E14))
            )

            // Alignment grid & safe-zone lines in Edit Mode
            if (!editorState.isPlayTestMode && settings.gridSnapEnabled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stepX = size.width / 10f
                    val stepY = size.height / 8f
                    for (i in 1 until 10) {
                        drawLine(
                            color = Color.White.copy(alpha = if (i == 5) 0.16f else 0.06f),
                            start = Offset(i * stepX, 0f),
                            end = Offset(i * stepX, size.height),
                            strokeWidth = if (i == 5) 1.5.dp.toPx() else 1.dp.toPx()
                        )
                    }
                    for (j in 1 until 8) {
                        drawLine(
                            color = Color.White.copy(alpha = if (j == 4) 0.16f else 0.06f),
                            start = Offset(0f, j * stepY),
                            end = Offset(size.width, j * stepY),
                            strokeWidth = if (j == 4) 1.5.dp.toPx() else 1.dp.toPx()
                        )
                    }
                }
            }

            profile.controls.forEach { control ->
                PositionedVirtualControl(
                    control = control,
                    containerWidthPx = widthPx,
                    containerHeightPx = heightPx,
                    notation = settings.controllerNotation,
                    globalOpacity = settings.globalOpacity,
                    hapticsEnabled = settings.hapticsEnabled,
                    isEditMode = !editorState.isPlayTestMode,
                    isSelected = control.id == editorState.selectedControlId,
                    isPressed = control.id in pressedIds,
                    isToggled = control.id in toggledIds,
                    onSelect = { onSelectControl(control.id) },
                    onDragDelta = { dx, dy ->
                        onMoveControl(control.id, dx, dy, widthPx, heightPx)
                    }
                )
            }
        }

        // Bottom Studio Action & Selected Control Dock
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val selected = editorState.selectedControl
                if (selected != null && !editorState.isPlayTestMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selected.label}  •  ${selected.binding.commandTitle}",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(selected.accent.hex),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Pos ${(selected.xPercent * 100).roundToInt()}%, ${(selected.yPercent * 100).roundToInt()}%  •  ${selected.sizeDp.roundToInt()}dp  •  ${selected.behavior.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = onDuplicateControl,
                                modifier = Modifier.testTag("editor_duplicate_control_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Control")
                            }
                            IconButton(
                                onClick = onDeleteControl,
                                modifier = Modifier.testTag("editor_delete_control_button")
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Control",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCommandRebindSheet = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("editor_rebind_button")
                        ) {
                            Text("Bind: ${selected.binding.codeLabel}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { showInspectorSheet = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("editor_customize_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Style & Mode")
                        }
                        Button(
                            onClick = { showAddControlSheet = true },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("editor_add_control_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Control")
                            Spacer(Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editorState.isPlayTestMode) {
                                "Play-test live! Switch off Test to drag buttons."
                            } else {
                                "Tap any virtual button to customize or add new controls."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { showAddControlSheet = true },
                            enabled = !editorState.isPlayTestMode,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("editor_add_control_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Add Control")
                        }
                    }
                }
            }
        }
    }

    // Add Control Modal Sheet
    if (showAddControlSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddControlSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CommandPickerSheetContent(
                title = "Add Virtual Control",
                includeClusters = true,
                onPick = { binding ->
                    val type = when (binding.axisPair) {
                        AxisPair.LEFT_STICK, AxisPair.RIGHT_STICK -> ControlType.ANALOG_STICK
                        AxisPair.DPAD_4WAY -> ControlType.DPAD_CLUSTER
                        AxisPair.WASD_KEYS, AxisPair.ARROW_KEYS -> ControlType.WASD_CLUSTER
                        AxisPair.NONE -> {
                            if (binding.id == "btn_lt" || binding.id == "btn_rt") {
                                ControlType.SHOULDER_PADDLE
                            } else {
                                ControlType.BUTTON
                            }
                        }
                    }
                    onAddControl(type, binding)
                    showAddControlSheet = false
                }
            )
        }
    }

    // Rebind Selected Control Command Sheet
    if (showCommandRebindSheet && editorState.selectedControl != null) {
        val selected = editorState.selectedControl!!
        ModalBottomSheet(
            onDismissRequest = { showCommandRebindSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CommandPickerSheetContent(
                title = "Rebind \"${selected.label}\"",
                includeClusters = selected.controlType != ControlType.BUTTON && selected.controlType != ControlType.SHOULDER_PADDLE,
                onPick = { binding ->
                    val newType = when (binding.axisPair) {
                        AxisPair.LEFT_STICK, AxisPair.RIGHT_STICK -> ControlType.ANALOG_STICK
                        AxisPair.DPAD_4WAY -> ControlType.DPAD_CLUSTER
                        AxisPair.WASD_KEYS, AxisPair.ARROW_KEYS -> ControlType.WASD_CLUSTER
                        AxisPair.NONE -> selected.controlType
                    }
                    onUpdateControl(
                        selected.copy(
                            binding = binding,
                            controlType = newType,
                            label = if (selected.label == selected.binding.codeLabel) binding.codeLabel else selected.label
                        )
                    )
                    showCommandRebindSheet = false
                }
            )
        }
    }

    // Selected Control Style & Behavior Inspector Sheet
    if (showInspectorSheet && editorState.selectedControl != null) {
        val selected = editorState.selectedControl!!
        ModalBottomSheet(
            onDismissRequest = { showInspectorSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ControlInspectorSheetContent(
                control = selected,
                onChange = onUpdateControl,
                onClose = { showInspectorSheet = false }
            )
        }
    }

    // Profile Metadata Dialog
    if (showMetaDialog) {
        EditProfileMetaDialog(
            initialName = profile.name,
            initialGame = profile.gameTitle,
            initialGenre = profile.genre,
            onDismiss = { showMetaDialog = false },
            onConfirm = { name, game, genre ->
                onUpdateProfileMeta(name, game, genre)
                showMetaDialog = false
            }
        )
    }

    // Unsaved changes confirmation
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Save Layout Changes?") },
            text = { Text("You have unsaved button positions or keybindings in \"${profile.name}\".") },
            confirmButton = {
                Button(onClick = {
                    showUnsavedDialog = false
                    onSave()
                    onBack()
                }) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onBack()
                }) {
                    Text("Discard")
                }
            }
        )
    }
}

@Composable
private fun CommandPickerSheetContent(
    title: String,
    includeClusters: Boolean,
    onPick: (InputBinding) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedDomain by remember { mutableStateOf<BindingDomain?>(null) }

    val commands = remember(query, selectedDomain, includeClusters) {
        val base = if (includeClusters) {
            CommandCatalog.allCommands
        } else {
            CommandCatalog.controllerCommands + CommandCatalog.keyboardCommands + CommandCatalog.comboCommands
        }
        base.filter { cmd ->
            val matchesDomain = selectedDomain == null || cmd.domain == selectedDomain
            val matchesQuery = query.isBlank() ||
                cmd.codeLabel.contains(query, ignoreCase = true) ||
                cmd.commandTitle.contains(query, ignoreCase = true)
            matchesDomain && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search A, RT, WASD, Space, Tab, Ctrl...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedDomain == null,
                    onClick = { selectedDomain = null },
                    label = { Text("All") }
                )
            }
            items(BindingDomain.entries) { domain ->
                if (includeClusters || domain != BindingDomain.ANALOG_AXIS) {
                    FilterChip(
                        selected = selectedDomain == domain,
                        onClick = { selectedDomain = if (selectedDomain == domain) null else domain },
                        label = { Text(domain.displayName) }
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.height(340.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(commands, key = { it.id }) { cmd ->
                val accent = when (cmd.domain) {
                    BindingDomain.CONTROLLER, BindingDomain.ANALOG_AXIS -> GeForceLime
                    BindingDomain.KEYBOARD -> TacticalCyan
                    BindingDomain.COMBO -> TurboAmber
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onPick(cmd) }
                        .testTag("pick_command_${cmd.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = accent.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = cmd.codeLabel,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cmd.commandTitle, style = MaterialTheme.typography.titleSmall)
                            Text(
                                cmd.domain.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlInspectorSheetContent(
    control: VirtualControl,
    onChange: (VirtualControl) -> Unit,
    onClose: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Customize Control", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClose) { Text("Done") }
            }
        }

        item {
            OutlinedTextField(
                value = control.label,
                onValueChange = { onChange(control.copy(label = it.take(12))) },
                label = { Text("Button Label") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inspector_label_input")
            )
        }

        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Size", style = MaterialTheme.typography.titleSmall)
                    Text("${control.sizeDp.roundToInt()} dp", style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = control.sizeDp,
                    onValueChange = { onChange(control.copy(sizeDp = it)) },
                    valueRange = 48f..160f
                )
            }
        }

        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Control Opacity", style = MaterialTheme.typography.titleSmall)
                    Text("${(control.opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = control.opacity,
                    onValueChange = { onChange(control.copy(opacity = it)) },
                    valueRange = 0.25f..1.0f
                )
            }
        }

        if (control.controlType == ControlType.BUTTON || control.controlType == ControlType.SHOULDER_PADDLE) {
            item {
                Text("Shape", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val shapes = listOf(
                        ControlShape.CIRCLE,
                        ControlShape.PILL,
                        ControlShape.HEX,
                        ControlShape.PADDLE
                    )
                    items(shapes) { shape ->
                        FilterChip(
                            selected = control.shape == shape,
                            onClick = { onChange(control.copy(shape = shape)) },
                            label = { Text(shape.displayName) }
                        )
                    }
                }
            }

            item {
                Text("Trigger Mode", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TriggerBehavior.entries.forEach { behavior ->
                        FilterChip(
                            selected = control.behavior == behavior,
                            onClick = { onChange(control.copy(behavior = behavior)) },
                            label = { Text(behavior.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (control.behavior == TriggerBehavior.TURBO) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Turbo Fire Rate", style = MaterialTheme.typography.bodyMedium)
                        Text("${control.turboRateHz} Hz", style = MaterialTheme.typography.labelMedium, color = TurboAmber)
                    }
                    Slider(
                        value = control.turboRateHz.toFloat(),
                        onValueChange = { onChange(control.copy(turboRateHz = it.roundToInt())) },
                        valueRange = 4f..20f
                    )
                }
            }
        }

        if (control.controlType == ControlType.ANALOG_STICK) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Analog Deadzone", style = MaterialTheme.typography.titleSmall)
                        Text("${(control.deadzone * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = control.deadzone,
                        onValueChange = { onChange(control.copy(deadzone = it)) },
                        valueRange = 0.02f..0.40f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sensitivity Curve", style = MaterialTheme.typography.titleSmall)
                        Text("%.2fx".format(control.sensitivity), style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = control.sensitivity,
                        onValueChange = { onChange(control.copy(sensitivity = it)) },
                        valueRange = 0.5f..2.2f
                    )
                }
            }
        }

        item {
            Text("HUD Color Accent", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ControlAccent.entries.forEach { accent ->
                    val selected = control.accent == accent
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(accent.hex))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onChange(control.copy(accent = accent)) }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Haptic Pulse on Press", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = control.hapticOnPress,
                    onCheckedChange = { onChange(control.copy(hapticOnPress = it)) }
                )
            }
        }
    }
}

@Composable
private fun EditProfileMetaDialog(
    initialName: String,
    initialGame: String,
    initialGenre: GameGenre,
    onDismiss: () -> Unit,
    onConfirm: (name: String, gameTitle: String, genre: GameGenre) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var game by remember { mutableStateOf(initialGame) }
    var genre by remember { mutableStateOf(initialGenre) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Layout Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = game,
                    onValueChange = { game = it },
                    label = { Text("Target Game") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
            Button(onClick = { onConfirm(name, game, genre) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

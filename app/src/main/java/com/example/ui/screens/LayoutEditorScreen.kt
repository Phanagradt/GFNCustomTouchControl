package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.util.ButtonImageCodec
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
    onPickCustomImage: (controlId: String, uri: Uri) -> Unit,
    onAddControl: (ControlType, InputBinding) -> Unit,
    onDuplicateControl: () -> Unit,
    onMirrorControl: () -> Unit,
    onNudgeControl: (dxPercent: Float, dyPercent: Float) -> Unit,
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
    var hideFloatingTools by remember { mutableStateOf(false) }

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

    // Full-screen uninterrupted canvas
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A10))
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
                .then(
                    if (!editorState.isPlayTestMode) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(onTap = { onSelectControl(null) })
                        }
                    } else {
                        Modifier
                    }
                )
        )

        // Snap grid overlay
        if (!editorState.isPlayTestMode && settings.gridSnapEnabled && !hideFloatingTools) {
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

        // Virtual controls positioned across 100% of full screen
        profile.controls.forEach { control ->
            PositionedVirtualControl(
                control = control,
                containerWidthPx = widthPx,
                containerHeightPx = heightPx,
                notation = settings.controllerNotation,
                globalOpacity = settings.globalOpacity,
                vibrationMode = settings.vibrationMode,
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

        // TOP-LEFT CORNER: Back + Save button (compact pill, never interrupts center/edges)
        Surface(
            color = Color(0xD90B0E14),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 10.dp, top = 10.dp)
                .border(
                    width = 1.dp,
                    color = if (editorState.hasUnsavedChanges) GeForceLime else Color(0xFF314057),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = handleBack,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("editor_back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (editorState.hasUnsavedChanges) GeForceLime else Color(0xFF1A2230),
                        contentColor = if (editorState.hasUnsavedChanges) Color(0xFF0A1400) else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("editor_save_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (editorState.hasUnsavedChanges) "Save" else "Saved",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
        }

        // TOP-RIGHT CORNER: Compact floating tools pill (collapsible)
        Surface(
            color = Color(0xD90B0E14),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 10.dp, top = 10.dp)
                .border(1.dp, Color(0xFF314057), RoundedCornerShape(24.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                if (!hideFloatingTools) {
                    if (!editorState.isPlayTestMode) {
                        IconButton(
                            onClick = { showAddControlSheet = true },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("editor_add_control_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Control", tint = GeForceLime)
                        }
                        if (editorState.undoStack.isNotEmpty()) {
                            IconButton(
                                onClick = onUndo,
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("editor_undo_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color.White)
                            }
                        }
                        IconButton(
                            onClick = { showMetaDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("editor_meta_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Profile Name", tint = TacticalCyan)
                        }
                    }

                    IconButton(
                        onClick = onTogglePlayTest,
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("editor_playtest_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = if (editorState.isPlayTestMode) "Stop Test" else "Play-Test",
                            tint = if (editorState.isPlayTestMode) GeForceLime else Color.White
                        )
                    }
                }

                IconButton(
                    onClick = { hideFloatingTools = !hideFloatingTools },
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("editor_toggle_hud_button")
                ) {
                    Icon(
                        imageVector = if (hideFloatingTools) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (hideFloatingTools) "Show Editor Tools" else "Hide Editor Tools",
                        tint = Color(0xFF9FB0C7)
                    )
                }
            }
        }

        // Play-test mini telemetry pill (top center, non-blocking)
        if (editorState.isPlayTestMode && !hideFloatingTools) {
            Surface(
                color = Color(0xCC0B0E14),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 60.dp)
                    .border(1.dp, TacticalCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = latestTelemetry?.let { "${it.codeLabel} • ${it.actionName}" }
                        ?: "MULTI-TOUCH TEST ACTIVE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = TacticalCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Auto-flipping contextual selected-control floating capsule
        // Moves to top-center when selected control is in the lower half, or bottom-center when in upper half!
        val selected = editorState.selectedControl
        if (selected != null && !editorState.isPlayTestMode && !hideFloatingTools) {
            val dockAlignment = if (selected.yPercent >= 0.52f) {
                Alignment.TopCenter
            } else {
                Alignment.BottomCenter
            }
            val extraPaddingModifier = if (selected.yPercent >= 0.52f) {
                Modifier.statusBarsPadding().padding(top = 62.dp, start = 12.dp, end = 12.dp)
            } else {
                Modifier.navigationBarsPadding().padding(bottom = 14.dp, start = 12.dp, end = 12.dp)
            }

            Surface(
                color = Color(0xE8121721),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(dockAlignment)
                    .then(extraPaddingModifier)
                    .border(1.dp, Color(selected.accent.hex).copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp)) {
                        Text(
                            text = selected.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(selected.accent.hex),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selected.binding.codeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9FB0C7)
                        )
                    }

                    OutlinedButton(
                        onClick = { showCommandRebindSheet = true },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("editor_rebind_button")
                    ) {
                        Text("Bind", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showInspectorSheet = true },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("editor_customize_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Style", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = onDuplicateControl,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("editor_duplicate_control_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onDeleteControl,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("editor_delete_control_button")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSelectControl(null) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Deselect", tint = Color(0xFF9FB0C7), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

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
                        AxisPair.TOUCHPAD_ARROWS,
                        AxisPair.TOUCHPAD_QE,
                        AxisPair.TOUCHPAD_IJKL,
                        AxisPair.TOUCHPAD_DPAD,
                        AxisPair.TOUCHPAD_DIRECT_PASS -> ControlType.TOUCHPAD
                        AxisPair.DPAD_4WAY -> ControlType.DPAD_CLUSTER
                        AxisPair.WASD_KEYS, AxisPair.ARROW_KEYS -> ControlType.WASD_CLUSTER
                        AxisPair.NONE -> {
                            if (binding.id == "btn_lt" || binding.id == "btn_rt" ||
                                binding.id == "mouse_m1" || binding.id == "mouse_m2"
                            ) {
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
                        AxisPair.TOUCHPAD_ARROWS,
                        AxisPair.TOUCHPAD_QE,
                        AxisPair.TOUCHPAD_IJKL,
                        AxisPair.TOUCHPAD_DPAD,
                        AxisPair.TOUCHPAD_DIRECT_PASS -> ControlType.TOUCHPAD
                        AxisPair.DPAD_4WAY -> ControlType.DPAD_CLUSTER
                        AxisPair.WASD_KEYS, AxisPair.ARROW_KEYS -> ControlType.WASD_CLUSTER
                        AxisPair.NONE -> selected.controlType
                    }
                    val newShape = if (newType == ControlType.TOUCHPAD) {
                        ControlShape.TOUCHPAD_RECT
                    } else {
                        selected.shape
                    }
                    onUpdateControl(
                        selected.copy(
                            binding = binding,
                            controlType = newType,
                            shape = newShape,
                            label = if (selected.label == selected.binding.codeLabel) binding.codeLabel else selected.label
                        )
                    )
                    showCommandRebindSheet = false
                }
            )
        }
    }

    if (showInspectorSheet && editorState.selectedControl != null) {
        val selected = editorState.selectedControl!!
        ModalBottomSheet(
            onDismissRequest = { showInspectorSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ControlInspectorSheetContent(
                control = selected,
                onChange = onUpdateControl,
                onPickCustomImage = { uri -> onPickCustomImage(selected.id, uri) },
                onNudge = onNudgeControl,
                onMirror = {
                    onMirrorControl()
                    showInspectorSheet = false
                },
                onClose = { showInspectorSheet = false }
            )
        }
    }

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
    var customComboMode by remember { mutableStateOf(false) }
    var comboStepFirst by remember { mutableStateOf<InputBinding?>(null) }

    val commands = remember(query, selectedDomain, includeClusters, customComboMode) {
        val base = when {
            customComboMode -> CommandCatalog.singleButtonCommands
            includeClusters -> CommandCatalog.allCommands
            else -> CommandCatalog.controllerCommands +
                CommandCatalog.mouseCommands +
                CommandCatalog.keyboardCommands +
                CommandCatalog.comboCommands
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (customComboMode) {
                    if (comboStepFirst == null) "Combo Builder • Pick Key 1" else "Pick Key 2 (${comboStepFirst!!.codeLabel} + ?)"
                } else {
                    title
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = {
                    customComboMode = !customComboMode
                    comboStepFirst = null
                },
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("picker_custom_combo_toggle")
            ) {
                Text(if (customComboMode) "Standard" else "+ Custom Combo", fontSize = 12.sp)
            }
        }

        if (customComboMode && comboStepFirst != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "First key: ${comboStepFirst!!.codeLabel} — now tap second key or mouse button",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { comboStepFirst = null }) {
                        Text("Reset")
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search M1–M5, MMB, A, RT, WASD, Shift, Ctrl...") },
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
                val showDomain = when {
                    customComboMode -> domain != BindingDomain.ANALOG_AXIS && domain != BindingDomain.COMBO
                    includeClusters -> true
                    else -> domain != BindingDomain.ANALOG_AXIS
                }
                if (showDomain) {
                    FilterChip(
                        selected = selectedDomain == domain,
                        onClick = { selectedDomain = if (selectedDomain == domain) null else domain },
                        label = { Text(domain.displayName) }
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.height(320.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(commands, key = { it.id }) { cmd ->
                val accent = when (cmd.domain) {
                    BindingDomain.CONTROLLER, BindingDomain.ANALOG_AXIS -> GeForceLime
                    BindingDomain.MOUSE -> TurboAmber
                    BindingDomain.KEYBOARD -> TacticalCyan
                    BindingDomain.COMBO -> Color(0xFFB388FF)
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable {
                            if (!customComboMode) {
                                onPick(cmd)
                            } else if (comboStepFirst == null) {
                                comboStepFirst = cmd
                            } else {
                                onPick(CommandCatalog.createCustomComboBinding(comboStepFirst!!, cmd))
                            }
                        }
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
    onPickCustomImage: (Uri) -> Unit,
    onNudge: (dxPercent: Float, dyPercent: Float) -> Unit,
    onMirror: () -> Unit,
    onClose: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickCustomImage(uri)
        }
    }
    val customBitmap = remember(control.customImageBase64) {
        ButtonImageCodec.decodeBase64ToImageBitmap(control.customImageBase64)
    }

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
                label = { Text("Button Label (optional if custom image set)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inspector_label_input")
            )
        }

        if (control.controlType == ControlType.BUTTON || control.controlType == ControlType.SHOULDER_PADDLE) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0A0F18))
                                .border(1.dp, Color(control.accent.hex).copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customBitmap != null) {
                                Image(
                                    bitmap = customBitmap,
                                    contentDescription = "Custom button artwork preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Custom Button Image", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (customBitmap != null) "Custom icon active & synced" else "Upload your own icon or art",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .testTag("inspector_upload_image_button")
                            ) {
                                Text(if (customBitmap != null) "Change" else "Upload", fontSize = 12.sp)
                            }
                            if (customBitmap != null) {
                                IconButton(
                                    onClick = { onChange(control.copy(customImageBase64 = null)) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .testTag("inspector_remove_image_button")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Remove Custom Image",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Precision Position (1% Nudge)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "X ${(control.xPercent * 100).roundToInt()}% • Y ${(control.yPercent * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = TacticalCyan
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onNudge(-0.01f, 0f) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(44.dp).testTag("inspector_nudge_left")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Nudge Left")
                    }
                    OutlinedButton(
                        onClick = { onNudge(0f, -0.01f) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(44.dp).testTag("inspector_nudge_up")
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Nudge Up")
                    }
                    OutlinedButton(
                        onClick = { onNudge(0f, 0.01f) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(44.dp).testTag("inspector_nudge_down")
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Nudge Down")
                    }
                    OutlinedButton(
                        onClick = { onNudge(0.01f, 0f) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(44.dp).testTag("inspector_nudge_right")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Nudge Right")
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onMirror,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(44.dp).testTag("inspector_mirror_button")
                    ) {
                        Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mirror", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            val maxSize = if (control.controlType == ControlType.TOUCHPAD) 240f else 160f
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Size", style = MaterialTheme.typography.titleSmall)
                    Text("${control.sizeDp.roundToInt()} dp", style = MaterialTheme.typography.labelMedium)
                }
                Slider(
                    value = control.sizeDp.coerceIn(48f, maxSize),
                    onValueChange = { onChange(control.copy(sizeDp = it)) },
                    valueRange = 48f..maxSize
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
                        ControlShape.KEYCAP,
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
                        valueRange = 2f..25f
                    )
                }
            }
        }

        if (control.controlType == ControlType.ANALOG_STICK || control.controlType == ControlType.TOUCHPAD) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (control.controlType == ControlType.TOUCHPAD) "Swipe Deadzone" else "Analog Deadzone",
                            style = MaterialTheme.typography.titleSmall
                        )
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
                        Text(
                            if (control.controlType == ControlType.TOUCHPAD) "Touchpad Sensitivity" else "Sensitivity Curve",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text("%.2fx".format(control.sensitivity), style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = control.sensitivity,
                        onValueChange = { onChange(control.copy(sensitivity = it)) },
                        valueRange = 0.5f..2.2f
                    )
                    if (control.controlType == ControlType.TOUCHPAD) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Invert Look Y-Axis", style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = control.invertY,
                                onCheckedChange = { onChange(control.copy(invertY = it)) }
                            )
                        }
                    }
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

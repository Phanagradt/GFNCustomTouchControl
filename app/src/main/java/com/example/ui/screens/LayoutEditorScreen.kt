package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R
import com.example.data.OverlaySettings
import com.example.model.AxisPair
import com.example.model.BindingDomain
import com.example.model.ButtonThemePreset
import com.example.model.ColorMatrixBuilder
import com.example.model.CommandCatalog
import com.example.model.ControlAccent
import com.example.model.ControlShape
import com.example.model.ControlType
import com.example.ui.components.SmoothSlider
import com.example.model.GameGenre
import com.example.model.InputBinding
import com.example.model.TelemetryEvent
import com.example.model.TouchpadMode
import com.example.model.TriggerBehavior
import com.example.model.VirtualControl
import com.example.ui.EditorUiState
import com.example.ui.components.CommandPickerSheetContent
import com.example.ui.components.ControlInspectorSheetContent
import com.example.ui.components.EditProfileMetaDialog
import com.example.ui.components.GridSettingsContent
import com.example.ui.components.LayoutStudioHelper
import com.example.ui.components.PositionedVirtualControl
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private tailrec fun Context.findEditorActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findEditorActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutEditorScreen(
    editorState: EditorUiState,
    settings: OverlaySettings,
    pressedIds: Set<String>,
    toggledIds: Set<String>,
    latestTelemetry: TelemetryEvent?,
    onSelectControl: (String?) -> Unit,
    onCommitControlPercent: (controlId: String, xPercent: Float, yPercent: Float) -> Unit,
    onGridSnapChange: (Boolean) -> Unit,
    onGridDivisionsChange: (Int) -> Unit,
    onUpdateControl: (VirtualControl) -> Unit,
    onResizeControlScales: (controlId: String, widthScale: Float, heightScale: Float) -> Unit = { _, _, _ -> },
    onPickCustomImage: (controlId: String, uri: Uri) -> Unit,
    onPickCustomBackground: (controlId: String, uri: Uri) -> Unit = { _, _ -> },
    onPickCustomStroke: (controlId: String, uri: Uri) -> Unit = { _, _ -> },
    onAddControl: (ControlType, InputBinding) -> Unit,
    onDuplicateControl: () -> Unit,
    onMirrorControl: () -> Unit,
    onNudgeControl: (dxPercent: Float, dyPercent: Float) -> Unit,
    onDeleteControl: () -> Unit,
    onTogglePlayTest: () -> Unit,
    onUndo: () -> Unit,
    onSave: () -> Unit,
    onUpdateProfileMeta: (name: String, gameTitle: String, genre: GameGenre) -> Unit,
    onPickCustomBg: (Uri) -> Unit = {},
    onResetCustomBg: () -> Unit = {},
    localButtonThemes: List<ButtonThemePreset> = emptyList(),
    activeButtonThemeId: String? = null,
    onSelectButtonTheme: (String?) -> Unit = {},
    onApplyThemeToAllButtons: () -> Unit = {},
    onOpenThemeCreator: () -> Unit = {},
    onOpenThemeWorkshop: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(view) {
        val window = context.findEditorActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val profile = editorState.profile
    var showAddControlSheet by remember { mutableStateOf(false) }
    var showInspectorSheet by remember { mutableStateOf(false) }
    var showCommandRebindSheet by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }
    var showMetaDialog by remember { mutableStateOf(false) }
    var showBgDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var hideFloatingTools by remember { mutableStateOf(false) }

    val editorBgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPickCustomBg(uri)
    }

    val customBgBitmap by produceState<ImageBitmap?>(initialValue = null, settings.customTestBgPath) {
        value = if (settings.customTestBgPath.isNotBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val f = File(settings.customTestBgPath)
                    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
                }.getOrNull()
            }
        } else {
            null
        }
    }

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

        val activeFilterStyle = settings.gameFilter.activeStyle
        val canvasColorFilter = remember(activeFilterStyle) {
            activeFilterStyle?.let {
                ColorFilter.colorMatrix(ColorMatrixBuilder.toComposeColorMatrix(it))
            }
        }

        if (customBgBitmap != null) {
            Image(
                bitmap = customBgBitmap!!,
                contentDescription = "Custom Layout Canvas Background",
                contentScale = ContentScale.Crop,
                colorFilter = canvasColorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_game_arena_bg),
                contentDescription = "GeForce NOW Stream Canvas",
                contentScale = ContentScale.Crop,
                colorFilter = canvasColorFilter,
                modifier = Modifier.fillMaxSize()
            )
        }
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

        // Configurable snap grid overlay
        if (!editorState.isPlayTestMode && settings.gridSnapEnabled && !hideFloatingTools) {
            val divsX = settings.gridDivisions.coerceIn(4, 32)
            val divsY = (divsX * 0.66f).roundToInt().coerceIn(4, 24)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = size.width / divsX.toFloat()
                val stepY = size.height / divsY.toFloat()
                for (i in 1 until divsX) {
                    val isCenter = i == divsX / 2
                    drawLine(
                        color = Color.White.copy(alpha = if (isCenter) 0.20f else 0.08f),
                        start = Offset(i * stepX, 0f),
                        end = Offset(i * stepX, size.height),
                        strokeWidth = if (isCenter) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }
                for (j in 1 until divsY) {
                    val isCenter = j == divsY / 2
                    drawLine(
                        color = Color.White.copy(alpha = if (isCenter) 0.20f else 0.08f),
                        start = Offset(0f, j * stepY),
                        end = Offset(size.width, j * stepY),
                        strokeWidth = if (isCenter) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }
            }
        }

        // Virtual controls positioned across 100% of true fullscreen canvas
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
                onCommitPercent = { xPct, yPct ->
                    onCommitControlPercent(control.id, xPct, yPct)
                },
                onResizeScales = { wScale, hScale ->
                    onResizeControlScales(control.id, wScale, hScale)
                }
            )
        }

        // TOP-LEFT CORNER: Back + Save button
        Surface(
            color = Color(0xD90B0E14),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 10.dp)
                .border(
                    width = 1.dp,
                    color = if (editorState.hasUnsavedChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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
                        containerColor = if (editorState.hasUnsavedChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (editorState.hasUnsavedChanges) MaterialTheme.colorScheme.onPrimary else Color.White
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
                            Icon(Icons.Default.Add, contentDescription = "Add Control", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { showGridDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("editor_grid_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "Grid Settings",
                                tint = if (settings.gridSnapEnabled) MaterialTheme.colorScheme.primary else Color(0xFF9FB0C7)
                            )
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
                            onClick = { showThemeDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("editor_theme_preset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Button Theme Preset",
                                tint = if (activeButtonThemeId != null) GeForceLime else Color(0xFF9FB0C7)
                            )
                        }
                        IconButton(
                            onClick = { showBgDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("editor_custom_bg_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Canvas Background",
                                tint = if (settings.customTestBgPath.isNotBlank()) MaterialTheme.colorScheme.primary else Color(0xFF9FB0C7)
                            )
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
                            tint = if (editorState.isPlayTestMode) MaterialTheme.colorScheme.primary else Color.White
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
            val isHeld = pressedIds.isNotEmpty()
            val accentColor = if (isHeld) MaterialTheme.colorScheme.primary else TacticalCyan
            Surface(
                color = Color(0xCC0B0E14),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .border(1.dp, accentColor.copy(alpha = if (isHeld) 0.9f else 0.6f), RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = latestTelemetry?.let { "${it.codeLabel} • ${it.actionName}" }
                        ?: "MULTI-TOUCH TEST ACTIVE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = if (isHeld) FontWeight.Bold else FontWeight.Medium,
                    color = accentColor,
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
                Modifier.padding(top = 62.dp, start = 12.dp, end = 12.dp)
            } else {
                Modifier.padding(bottom = 14.dp, start = 12.dp, end = 12.dp)
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
                            text = selected.label.ifBlank { selected.binding.codeLabel },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(selected.accent.hex),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selected.label.isBlank()) "${selected.binding.codeLabel} • Blank" else selected.binding.codeLabel,
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
                    val type = LayoutStudioHelper.controlTypeForBinding(binding)
                    onAddControl(type, binding)
                    showAddControlSheet = false
                }
            )
        }
    }

    if (showBgDialog) {
        AlertDialog(
            onDismissRequest = { showBgDialog = false },
            title = { Text("Canvas Background") },
            text = {
                Text(
                    text = if (settings.customTestBgPath.isNotBlank()) {
                        "Your custom game screenshot is shared between Sandbox and Layout Studio so you can align buttons over real game HUDs."
                    } else {
                        "Pick a screenshot from your game to align buttons accurately in both Layout Studio and Sandbox."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBgDialog = false
                        editorBgPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("editor_pick_bg_confirm")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pick Image")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (settings.customTestBgPath.isNotBlank()) {
                        TextButton(
                            onClick = {
                                showBgDialog = false
                                onResetCustomBg()
                            },
                            modifier = Modifier.testTag("editor_reset_bg_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset BG")
                        }
                    }
                    TextButton(onClick = { showBgDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
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
                    onUpdateControl(LayoutStudioHelper.rebindControl(selected, binding))
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
                onPickCustomBackground = { uri -> onPickCustomBackground(selected.id, uri) },
                onPickCustomStroke = { uri -> onPickCustomStroke(selected.id, uri) },
                onNudge = onNudgeControl,
                onMirror = {
                    onMirrorControl()
                    showInspectorSheet = false
                },
                onClose = { showInspectorSheet = false }
            )
        }
    }

    if (showThemeDialog) {
        val activeTheme = localButtonThemes.firstOrNull { it.id == activeButtonThemeId }
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Button Theme Preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "New buttons you add automatically inherit the active Button Theme (icons, strokes, backgrounds & style).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item(key = "theme_none") {
                            val selectedNone = activeButtonThemeId == null
                            Surface(
                                color = if (selectedNone) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectButtonTheme(null) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Default XenosPad Style", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    if (selectedNone) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        items(localButtonThemes, key = { it.id }) { theme ->
                            val selected = theme.id == activeButtonThemeId
                            Surface(
                                color = if (selected) GeForceLime.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) GeForceLime else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectButtonTheme(theme.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(theme.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "${theme.gameTag}${if (theme.isDraft) " • Draft" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = GeForceLime)
                                }
                            }
                        }
                    }

                    if (activeTheme != null) {
                        Button(
                            onClick = {
                                onApplyThemeToAllButtons()
                                showThemeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply \"${activeTheme.name}\" to All Buttons")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                showThemeDialog = false
                                onOpenThemeCreator()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Create Theme", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                showThemeDialog = false
                                onOpenThemeWorkshop()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Workshop", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Done") }
            }
        )
    }

    if (showGridDialog) {
        AlertDialog(
            onDismissRequest = { showGridDialog = false },
            title = { Text("Layout Grid Settings") },
            text = {
                GridSettingsContent(
                    gridSnapEnabled = settings.gridSnapEnabled,
                    gridDivisions = settings.gridDivisions,
                    onGridSnapChange = onGridSnapChange,
                    onGridDivisionsChange = onGridDivisionsChange
                )
            },
            confirmButton = {
                Button(onClick = { showGridDialog = false }) {
                    Text("Done")
                }
            }
        )
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

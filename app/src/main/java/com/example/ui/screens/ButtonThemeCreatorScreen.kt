package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VibrationMode
import com.example.model.BindingDomain
import com.example.model.ButtonStyleOverride
import com.example.model.ButtonThemePreset
import com.example.model.CommandCatalog
import com.example.model.CommunityConstants
import com.example.model.ControlAccent
import com.example.model.ControlShape
import com.example.model.ControllerNotation
import com.example.model.InputBinding
import com.example.ui.components.LayoutStudioHelper
import com.example.ui.components.SmoothSlider
import com.example.ui.components.VirtualControlCanvasItem
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import com.example.util.ButtonImageCodec
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

private enum class ThemeImageSlot {
    ICON,
    BACKGROUND,
    STROKE
}

private enum class ThemeButtonFilter(val label: String) {
    ALL("All"),
    CONTROLLER("Gamepad"),
    KEYBOARD("Keyboard"),
    MOUSE("Mouse")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonThemeCreatorScreen(
    initialTheme: ButtonThemePreset?,
    autoSavedDraft: ButtonThemePreset? = null,
    isSignedIn: Boolean,
    isPublishing: Boolean,
    notation: ControllerNotation,
    onAutoSaveDraft: (ButtonThemePreset) -> Unit = {},
    onAutoSaveDraftSync: (ButtonThemePreset) -> Unit = {},
    onClearAutoSavedDraft: () -> Unit = {},
    onSaveDraft: (ButtonThemePreset) -> Unit,
    onTestTheme: (ButtonThemePreset) -> Unit,
    onPublishTheme: (ButtonThemePreset) -> Unit,
    onRequestSignIn: () -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val primary = MaterialTheme.colorScheme.primary

    // If user opens a fresh creator and an unsaved auto-draft exists, restore it automatically!
    val seedTheme = remember(initialTheme?.id, autoSavedDraft?.id) {
        initialTheme ?: autoSavedDraft
    }
    var restoredFromAutoDraft by remember(initialTheme?.id) {
        mutableStateOf(initialTheme == null && autoSavedDraft != null)
    }

    var themeId by remember(seedTheme?.id) {
        mutableStateOf(
            if (seedTheme != null && seedTheme.ownerId != "builtin") {
                seedTheme.id
            } else {
                "theme_${UUID.randomUUID().toString().take(10)}"
            }
        )
    }
    var name by remember(seedTheme?.id) {
        mutableStateOf(
            when {
                seedTheme == null -> ""
                seedTheme.ownerId == "builtin" -> "${seedTheme.name} Remix"
                else -> seedTheme.name
            }
        )
    }
    var gameTag by remember(seedTheme?.id) { mutableStateOf(seedTheme?.gameTag ?: "Dead by Daylight") }
    var category by remember(seedTheme?.id) { mutableStateOf(seedTheme?.category ?: "Horror") }
    var description by remember(seedTheme?.id) { mutableStateOf(seedTheme?.description ?: "") }
    var defaultStyle by remember(seedTheme?.id) {
        mutableStateOf(seedTheme?.defaultStyle ?: ButtonStyleOverride())
    }
    var buttonStyles by remember(seedTheme?.id) {
        mutableStateOf(seedTheme?.buttonStyles ?: emptyMap())
    }
    var hasDirtyEdits by remember { mutableStateOf(false) }
    var autoSavedIndicator by remember { mutableStateOf(restoredFromAutoDraft) }
    var didExplicitSaveOrPublish by remember { mutableStateOf(false) }

    // null = editing "Default Style (All Buttons)", non-null = editing that specific binding ID
    var selectedBindingId by remember { mutableStateOf<String?>(null) }
    var buttonFilter by remember { mutableStateOf(ThemeButtonFilter.ALL) }
    var buttonSearch by remember { mutableStateOf("") }
    var pendingImageSlot by remember { mutableStateOf<ThemeImageSlot?>(null) }

    val allCustomizableBindings = remember {
        CommandCatalog.controllerCommands + CommandCatalog.mouseCommands + CommandCatalog.keyboardCommands
    }

    val filteredBindings = remember(buttonFilter, buttonSearch) {
        val base = when (buttonFilter) {
            ThemeButtonFilter.ALL -> allCustomizableBindings
            ThemeButtonFilter.CONTROLLER -> CommandCatalog.controllerCommands
            ThemeButtonFilter.KEYBOARD -> CommandCatalog.keyboardCommands
            ThemeButtonFilter.MOUSE -> CommandCatalog.mouseCommands
        }
        val q = buttonSearch.trim().lowercase()
        if (q.isEmpty()) base else base.filter {
            it.codeLabel.lowercase().contains(q) || it.commandTitle.lowercase().contains(q)
        }
    }

    fun currentEditingStyle(): ButtonStyleOverride {
        val bid = selectedBindingId
        return if (bid == null) defaultStyle else (buttonStyles[bid] ?: defaultStyle)
    }

    fun updateEditingStyle(transform: (ButtonStyleOverride) -> ButtonStyleOverride) {
        hasDirtyEdits = true
        val bid = selectedBindingId
        if (bid == null) {
            defaultStyle = transform(defaultStyle)
        } else {
            val base = buttonStyles[bid] ?: defaultStyle
            buttonStyles = buttonStyles + (bid to transform(base))
        }
    }

    fun buildThemeSnapshot(isDraftFlag: Boolean): ButtonThemePreset {
        return ButtonThemePreset(
            id = themeId,
            name = name.trim().ifEmpty { "Untitled Button Theme" },
            gameTag = gameTag.trim().ifEmpty { "Universal" },
            description = description.trim(),
            category = category.ifBlank { "Custom" },
            ownerId = seedTheme?.ownerId?.takeIf { it != "builtin" } ?: "",
            authorName = seedTheme?.authorName ?: "",
            authorAvatarBase64 = seedTheme?.authorAvatarBase64,
            isDraft = isDraftFlag,
            isPublished = !isDraftFlag,
            defaultStyle = defaultStyle,
            buttonStyles = buttonStyles,
            likesCount = seedTheme?.likesCount ?: 0,
            favoritesCount = seedTheme?.favoritesCount ?: 0,
            downloadsCount = seedTheme?.downloadsCount ?: 0,
            commentsCount = seedTheme?.commentsCount ?: 0,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    val latestSnapshotProvider = rememberUpdatedState { buildThemeSnapshot(isDraftFlag = true) }
    val latestHasDirty = rememberUpdatedState(hasDirtyEdits)
    val latestExplicitSaved = rememberUpdatedState(didExplicitSaveOrPublish)

    // Debounced auto-save on every edit
    LaunchedEffect(name, gameTag, category, description, defaultStyle, buttonStyles, hasDirtyEdits) {
        if (hasDirtyEdits && !didExplicitSaveOrPublish) {
            delay(450L)
            onAutoSaveDraft(buildThemeSnapshot(isDraftFlag = true))
            autoSavedIndicator = true
        }
    }

    // Immediate synchronous auto-save when app goes to background or user swipes/closes the app tab!
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if ((event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) &&
                latestHasDirty.value && !latestExplicitSaved.value
            ) {
                onAutoSaveDraftSync(latestSnapshotProvider.value())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (latestHasDirty.value && !latestExplicitSaved.value) {
                onAutoSaveDraftSync(latestSnapshotProvider.value())
            }
        }
    }

    val handleBack: () -> Unit = {
        if (hasDirtyEdits && !didExplicitSaveOrPublish) {
            onAutoSaveDraftSync(buildThemeSnapshot(isDraftFlag = true))
            onShowMessage("Draft auto-saved locally")
        }
        onBack()
    }
    BackHandler(onBack = handleBack)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val slot = pendingImageSlot
        pendingImageSlot = null
        if (uri != null && slot != null) {
            scope.launch {
                val cropSquare = slot == ThemeImageSlot.ICON
                // Compact resolution so multi-button themes fit well inside cloud workshop budget
                ButtonImageCodec.encodeUriToBase64(
                    context = context,
                    uri = uri,
                    cropSquare = cropSquare,
                    maxDimPx = 112,
                    maxEncodedBytes = 18_500
                ).onSuccess { encoded ->
                    updateEditingStyle { style ->
                        when (slot) {
                            ThemeImageSlot.ICON -> style.copy(customImageBase64 = encoded)
                            ThemeImageSlot.BACKGROUND -> style.copy(
                                customBackgroundBase64 = encoded,
                                showBackground = true
                            )
                            ThemeImageSlot.STROKE -> style.copy(
                                customStrokeBase64 = encoded,
                                showStroke = true
                            )
                        }
                    }
                    onShowMessage(
                        when (slot) {
                            ThemeImageSlot.ICON -> "Button icon image uploaded"
                            ThemeImageSlot.BACKGROUND -> "Custom button background uploaded"
                            ThemeImageSlot.STROKE -> "Custom button stroke frame uploaded"
                        }
                    )
                }.onFailure { err ->
                    onShowMessage(err.localizedMessage ?: "Unable to import image")
                }
            }
        }
    }

    val previewTheme = remember(name, gameTag, category, description, defaultStyle, buttonStyles) {
        buildThemeSnapshot(isDraftFlag = true)
    }
    val previewBinding: InputBinding = remember(selectedBindingId) {
        selectedBindingId?.let { CommandCatalog.findById(it) } ?: CommandCatalog.findById("key_space")
    }
    val previewControl = remember(previewTheme, previewBinding) {
        LayoutStudioHelper.createControlFromBinding(previewBinding, previewTheme)
    }
    val editingStyle = currentEditingStyle()
    val hasSpecificOverride = selectedBindingId != null && buttonStyles.containsKey(selectedBindingId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Button Theme Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${buttonStyles.size} custom buttons • ${if (autoSavedIndicator) "Auto-saved" else "Draft & test before publishing"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (autoSavedIndicator) GeForceLime else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = handleBack,
                        modifier = Modifier.testTag("theme_creator_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            didExplicitSaveOrPublish = true
                            hasDirtyEdits = false
                            onSaveDraft(buildThemeSnapshot(isDraftFlag = true))
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("theme_creator_save_draft")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Draft", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = {
                            didExplicitSaveOrPublish = true
                            hasDirtyEdits = false
                            onTestTheme(buildThemeSnapshot(isDraftFlag = true))
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("theme_creator_test")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp), tint = TacticalCyan)
                        Spacer(Modifier.width(4.dp))
                        Text("Test", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = {
                            if (!isSignedIn) {
                                onRequestSignIn()
                            } else {
                                didExplicitSaveOrPublish = true
                                hasDirtyEdits = false
                                onPublishTheme(buildThemeSnapshot(isDraftFlag = false))
                            }
                        },
                        enabled = !isPublishing,
                        colors = ButtonDefaults.buttonColors(containerColor = primary),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1.35f)
                            .height(48.dp)
                            .testTag("theme_creator_publish")
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(17.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isSignedIn) "Publish" else "Sign In",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (restoredFromAutoDraft) {
                Surface(
                    color = GeForceLime.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GeForceLime.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Restored your auto-saved draft theme.",
                            style = MaterialTheme.typography.labelMedium,
                            color = GeForceLime,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                restoredFromAutoDraft = false
                                autoSavedIndicator = false
                                hasDirtyEdits = false
                                themeId = "theme_${UUID.randomUUID().toString().take(10)}"
                                name = ""
                                gameTag = "Dead by Daylight"
                                category = "Horror"
                                description = ""
                                defaultStyle = ButtonStyleOverride()
                                buttonStyles = emptyMap()
                                onClearAutoSavedDraft()
                            }
                        ) {
                            Text("Start Fresh", fontSize = 11.sp)
                        }
                    }
                }
            }
            // 1. Live Preview Stage
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0F18)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, primary.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(166.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 24.dp.toPx()
                        var x = step
                        while (x < size.width) {
                            drawLine(Color.White.copy(alpha = 0.05f), Offset(x, 0f), Offset(x, size.height), 1f)
                            x += step
                        }
                        var y = step
                        while (y < size.height) {
                            drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y), Offset(size.width, y), 1f)
                            y += step
                        }
                    }

                    VirtualControlCanvasItem(
                        control = previewControl,
                        notation = notation,
                        globalOpacity = 1f,
                        vibrationMode = VibrationMode.NONE,
                        isEditMode = true,
                        isSelected = false,
                        isPressed = false,
                        isToggled = false,
                        onSelect = {},
                        onDragDelta = { _, _ -> }
                    )

                    Surface(
                        color = Color(0xCC0D131E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (selectedBindingId == null) {
                                "PREVIEW • DEFAULT STYLE"
                            } else {
                                "PREVIEW • ${previewBinding.codeLabel}"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (hasSpecificOverride) {
                        TextButton(
                            onClick = {
                                val bid = selectedBindingId ?: return@TextButton
                                buttonStyles = buttonStyles - bid
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp), tint = TurboAmber)
                            Spacer(Modifier.width(4.dp))
                            Text("Reset Key", fontSize = 11.sp, color = TurboAmber)
                        }
                    }
                }
            }

            // 2. Theme Identity Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Theme Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 60) { name = it; hasDirtyEdits = true } },
                        label = { Text("Theme Name (e.g. DBD Bloodweb)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme_creator_name_input")
                    )
                    OutlinedTextField(
                        value = gameTag,
                        onValueChange = { if (it.length <= 50) { gameTag = it; hasDirtyEdits = true } },
                        label = { Text("Game / Style Tag") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme_creator_game_input")
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 300) { description = it; hasDirtyEdits = true } },
                        label = { Text("Description (optional)") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme_creator_desc_input")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(CommunityConstants.THEME_CATEGORIES.filterNot { it == "All" }) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat; hasDirtyEdits = true },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            // 3. Button Picker Strip (Default Style + Every Button in Catalog)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choose Button to Customize",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set a Default Style for all new buttons, or tap any specific key/gamepad button to give it its own icon, stroke & background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ThemeButtonFilter.entries.forEach { tab ->
                            FilterChip(
                                selected = buttonFilter == tab,
                                onClick = { buttonFilter = tab },
                                label = { Text(tab.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = buttonSearch,
                        onValueChange = { buttonSearch = it },
                        placeholder = { Text("Search keys (Space, Shift, A, LMB...)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item(key = "__default__") {
                            val selected = selectedBindingId == null
                            Surface(
                                color = if (selected) primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedBindingId = null }
                                    .testTag("theme_button_default")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "ALL BUTTONS",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (selected) primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Default Style",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(filteredBindings, key = { it.id }) { binding ->
                            val selected = selectedBindingId == binding.id
                            val customized = buttonStyles.containsKey(binding.id)
                            Surface(
                                color = if (selected) TacticalCyan.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = when {
                                            selected -> TacticalCyan
                                            customized -> GeForceLime.copy(alpha = 0.7f)
                                            else -> MaterialTheme.colorScheme.outlineVariant
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedBindingId = binding.id }
                                    .testTag("theme_button_${binding.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (customized) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(GeForceLime)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = binding.codeLabel,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (selected) TacticalCyan else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = when (binding.domain) {
                                                BindingDomain.CONTROLLER -> "Gamepad"
                                                BindingDomain.MOUSE -> "Mouse"
                                                else -> "Key"
                                            },
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Style Customization Panel for Selected Button / Default Style
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (selectedBindingId == null) {
                            "Default Style Settings"
                        } else {
                            "Custom Style for ${previewBinding.codeLabel}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Shape
                    Text("Button Shape", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(
                            listOf(
                                ControlShape.CIRCLE,
                                ControlShape.HEX,
                                ControlShape.KEYCAP,
                                ControlShape.PILL,
                                ControlShape.PADDLE
                            )
                        ) { shape ->
                            FilterChip(
                                selected = editingStyle.shape == shape,
                                onClick = { updateEditingStyle { it.copy(shape = shape) } },
                                label = { Text(shape.name) }
                            )
                        }
                    }

                    // Blank Label & Custom Label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Blank Label (Icon Only)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Hide button text cleanly with no dark label bar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = editingStyle.blankLabel,
                            onCheckedChange = { blank ->
                                updateEditingStyle { it.copy(blankLabel = blank) }
                            }
                        )
                    }

                    if (!editingStyle.blankLabel && selectedBindingId != null) {
                        OutlinedTextField(
                            value = editingStyle.customLabel.orEmpty(),
                            onValueChange = { txt ->
                                updateEditingStyle { it.copy(customLabel = txt.take(12).ifBlank { null }) }
                            },
                            label = { Text("Custom Label Override (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider()

                    // Custom Button Icon Image + Size + Opacity
                    Text("Button Icon Artwork", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                pendingImageSlot = ThemeImageSlot.ICON
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("theme_creator_pick_icon")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (editingStyle.customImageBase64 != null) "Change Icon" else "Upload Icon")
                        }
                        if (editingStyle.customImageBase64 != null) {
                            OutlinedButton(
                                onClick = { updateEditingStyle { it.copy(customImageBase64 = null) } },
                                modifier = Modifier.height(46.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Icon", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (editingStyle.customImageBase64 != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Icon Size", style = MaterialTheme.typography.labelSmall)
                            Text("${(editingStyle.customImageScale * 100).roundToInt()}%", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = primary)
                        }
                        SmoothSlider(
                            value = editingStyle.customImageScale.coerceIn(0.30f, 1.80f),
                            onValueChange = { s -> updateEditingStyle { it.copy(customImageScale = s) } },
                            valueRange = 0.30f..1.80f,
                            accentColor = primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Icon Opacity", style = MaterialTheme.typography.labelSmall)
                            Text("${(editingStyle.customImageOpacity * 100).roundToInt()}%", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = TacticalCyan)
                        }
                        SmoothSlider(
                            value = editingStyle.customImageOpacity.coerceIn(0.10f, 1.0f),
                            onValueChange = { a -> updateEditingStyle { it.copy(customImageOpacity = a) } },
                            valueRange = 0.10f..1.0f,
                            accentColor = TacticalCyan
                        )
                    }

                    HorizontalDivider()

                    // Background Customization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Button Background", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = editingStyle.showBackground,
                            onCheckedChange = { show -> updateEditingStyle { it.copy(showBackground = show) } }
                        )
                    }

                    if (editingStyle.showBackground) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CommunityConstants.PRESET_BG_COLORS) { (_, hex) ->
                                val selected = editingStyle.customBackgroundColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) Color.White else Color(0xFF475569),
                                            shape = CircleShape
                                        )
                                        .clickable { updateEditingStyle { it.copy(customBackgroundColorHex = hex) } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    pendingImageSlot = ThemeImageSlot.BACKGROUND
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (editingStyle.customBackgroundBase64 != null) "Change BG Image" else "Upload BG Image", fontSize = 12.sp)
                            }
                            if (editingStyle.customBackgroundBase64 != null) {
                                OutlinedButton(
                                    onClick = { updateEditingStyle { it.copy(customBackgroundBase64 = null) } },
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove BG", modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Stroke Customization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Button Stroke / Frame", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = editingStyle.showStroke,
                            onCheckedChange = { show -> updateEditingStyle { it.copy(showStroke = show) } }
                        )
                    }

                    if (editingStyle.showStroke) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Stroke Width", style = MaterialTheme.typography.labelSmall)
                            Text(String.format("%.1f dp", editingStyle.strokeWidthDp), fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = primary)
                        }
                        SmoothSlider(
                            value = editingStyle.strokeWidthDp.coerceIn(0.5f, 6.0f),
                            onValueChange = { w -> updateEditingStyle { it.copy(strokeWidthDp = w) } },
                            valueRange = 0.5f..6.0f,
                            accentColor = primary
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CommunityConstants.PRESET_STROKE_COLORS) { (_, hex) ->
                                val selected = editingStyle.customStrokeColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) Color.White else Color(0xFF475569),
                                            shape = CircleShape
                                        )
                                        .clickable { updateEditingStyle { it.copy(customStrokeColorHex = hex) } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0A0D12), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    pendingImageSlot = ThemeImageSlot.STROKE
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (editingStyle.customStrokeBase64 != null) "Change Stroke Frame" else "Upload Stroke Frame", fontSize = 12.sp)
                            }
                            if (editingStyle.customStrokeBase64 != null) {
                                OutlinedButton(
                                    onClick = { updateEditingStyle { it.copy(customStrokeBase64 = null) } },
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Stroke", modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Accent & Dimensions
                    Text("Accent Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ControlAccent.entries) { acc ->
                            val selected = editingStyle.accent == acc
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(acc.hex))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { updateEditingStyle { it.copy(accent = acc) } }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Width Scale (X)", style = MaterialTheme.typography.labelSmall)
                        Text("${(editingStyle.widthScale * 100).roundToInt()}%", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = TacticalCyan)
                    }
                    SmoothSlider(
                        value = editingStyle.widthScale.coerceIn(0.45f, 2.6f),
                        onValueChange = { w -> updateEditingStyle { it.copy(widthScale = w) } },
                        valueRange = 0.45f..2.6f,
                        accentColor = TacticalCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Height Scale (Y)", style = MaterialTheme.typography.labelSmall)
                        Text("${(editingStyle.heightScale * 100).roundToInt()}%", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = TurboAmber)
                    }
                    SmoothSlider(
                        value = editingStyle.heightScale.coerceIn(0.45f, 2.6f),
                        onValueChange = { h -> updateEditingStyle { it.copy(heightScale = h) } },
                        valueRange = 0.45f..2.6f,
                        accentColor = TurboAmber
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

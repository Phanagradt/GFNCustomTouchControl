package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AxisPair
import com.example.model.BindingDomain
import com.example.model.CommandCatalog
import com.example.model.ControlAccent
import com.example.model.ControlShape
import com.example.model.ControlType
import com.example.model.GameGenre
import com.example.model.InputBinding
import com.example.model.TouchpadMode
import com.example.model.TriggerBehavior
import com.example.model.VirtualControl
import com.example.ui.theme.GeForceLime
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TurboAmber
import com.example.util.ButtonImageCodec
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Shared control creation & transformation helpers used identically by Layout Studio,
 * the in-game Web Player Live Studio, and the Floating Overlay Live Studio.
 */
object LayoutStudioHelper {

    fun controlTypeForBinding(binding: InputBinding, fallbackType: ControlType? = null): ControlType =
        when (binding.axisPair) {
            AxisPair.LEFT_STICK, AxisPair.RIGHT_STICK -> ControlType.ANALOG_STICK
            AxisPair.TOUCHPAD_ARROWS,
            AxisPair.TOUCHPAD_QE,
            AxisPair.TOUCHPAD_IJKL,
            AxisPair.TOUCHPAD_DPAD,
            AxisPair.TOUCHPAD_DIRECT_PASS -> ControlType.TOUCHPAD
            AxisPair.DPAD_4WAY -> ControlType.DPAD_CLUSTER
            AxisPair.WASD_KEYS, AxisPair.ARROW_KEYS -> ControlType.WASD_CLUSTER
            AxisPair.NONE -> fallbackType ?: if (
                binding.id == "btn_lt" || binding.id == "btn_rt" ||
                binding.id == "mouse_left" || binding.id == "mouse_right"
            ) {
                ControlType.SHOULDER_PADDLE
            } else {
                ControlType.BUTTON
            }
        }

    fun createControlFromBinding(binding: InputBinding): VirtualControl {
        val controlType = controlTypeForBinding(binding)
        val defaultShape = when (controlType) {
            ControlType.ANALOG_STICK -> ControlShape.RING
            ControlType.TOUCHPAD -> ControlShape.TOUCHPAD_RECT
            ControlType.DPAD_CLUSTER, ControlType.WASD_CLUSTER -> ControlShape.CROSS
            ControlType.SHOULDER_PADDLE -> ControlShape.PADDLE
            ControlType.BUTTON -> when (binding.domain) {
                BindingDomain.KEYBOARD, BindingDomain.MOUSE -> ControlShape.KEYCAP
                else -> ControlShape.CIRCLE
            }
        }
        val defaultSize = when (controlType) {
            ControlType.TOUCHPAD -> 148f
            ControlType.ANALOG_STICK, ControlType.DPAD_CLUSTER, ControlType.WASD_CLUSTER -> 118f
            ControlType.SHOULDER_PADDLE -> 72f
            ControlType.BUTTON -> 60f
        }
        val defaultAccent = when (binding.domain) {
            BindingDomain.CONTROLLER -> ControlAccent.LIME
            BindingDomain.MOUSE -> ControlAccent.AMBER
            BindingDomain.KEYBOARD -> ControlAccent.CYAN
            BindingDomain.COMBO -> ControlAccent.VIOLET
            BindingDomain.ANALOG_AXIS -> ControlAccent.CYAN
        }
        return VirtualControl(
            id = UUID.randomUUID().toString(),
            label = binding.codeLabel,
            controlType = controlType,
            shape = defaultShape,
            xPercent = 0.50f,
            yPercent = 0.50f,
            sizeDp = defaultSize,
            opacity = 0.85f,
            accent = defaultAccent,
            behavior = TriggerBehavior.HOLD,
            binding = binding
        )
    }

    fun rebindControl(control: VirtualControl, binding: InputBinding): VirtualControl {
        val newType = controlTypeForBinding(binding, fallbackType = control.controlType)
        val newShape = if (newType == ControlType.TOUCHPAD) {
            ControlShape.TOUCHPAD_RECT
        } else {
            control.shape
        }
        return control.copy(
            binding = binding,
            controlType = newType,
            shape = newShape,
            label = if (control.label == control.binding.codeLabel) binding.codeLabel else control.label
        )
    }

    fun duplicateControl(control: VirtualControl): VirtualControl = control.copy(
        id = UUID.randomUUID().toString(),
        xPercent = (control.xPercent + 0.08f).coerceAtMost(0.92f),
        yPercent = (control.yPercent + 0.08f).coerceAtMost(0.90f)
    )

    fun mirrorControl(control: VirtualControl): VirtualControl = control.copy(
        id = UUID.randomUUID().toString(),
        xPercent = (1f - control.xPercent).coerceIn(0.05f, 0.95f)
    )

    fun nudgeControl(control: VirtualControl, dxPercent: Float, dyPercent: Float): VirtualControl =
        control.copy(
            xPercent = (control.xPercent + dxPercent).coerceIn(0.03f, 0.97f),
            yPercent = (control.yPercent + dyPercent).coerceIn(0.04f, 0.96f)
        )
}

@Composable
fun CommandPickerSheetContent(
    title: String,
    includeClusters: Boolean,
    onPick: (InputBinding) -> Unit,
    onClose: (() -> Unit)? = null
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                if (onClose != null) {
                    TextButton(onClick = onClose) {
                        Text("Close")
                    }
                }
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
            placeholder = { Text("Search LMB, RMB, MMB, Scroll, A, RT, WASD, Shift...") },
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
            modifier = Modifier.height(300.dp),
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
private fun CustomButtonImagePickerCard(
    control: VirtualControl,
    onChange: (VirtualControl) -> Unit,
    onPickCustomImage: (Uri) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickCustomImage(uri)
        }
    }
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onPickCustomImage(uri)
        }
    }
    val customBitmap = remember(control.customImageBase64) {
        ButtonImageCodec.decodeBase64ToImageBitmap(control.customImageBase64)
    }

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
                    text = if (customBitmap != null) "Custom icon active (saved with layout)" else "Upload your own icon or art",
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
                OutlinedButton(
                    onClick = {
                        documentPickerLauncher.launch(arrayOf("image/*"))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("inspector_upload_file_button")
                ) {
                    Text("Files", fontSize = 11.sp)
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

@Composable
fun ControlInspectorSheetContent(
    control: VirtualControl,
    onChange: (VirtualControl) -> Unit,
    onPickCustomImage: ((Uri) -> Unit)? = null,
    onNudge: (dxPercent: Float, dyPercent: Float) -> Unit,
    onMirror: () -> Unit,
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
                label = { Text("Button Label (optional if custom image set)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inspector_label_input")
            )
        }

        if (onPickCustomImage != null &&
            (control.controlType == ControlType.BUTTON || control.controlType == ControlType.SHOULDER_PADDLE)
        ) {
            item {
                CustomButtonImagePickerCard(
                    control = control,
                    onChange = onChange,
                    onPickCustomImage = onPickCustomImage
                )
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
            var sizeDraft by remember(control.id) { mutableFloatStateOf(control.sizeDp.coerceIn(48f, maxSize)) }
            var sizeDragging by remember(control.id) { mutableStateOf(false) }
            LaunchedEffect(control.sizeDp, sizeDragging) {
                if (!sizeDragging) sizeDraft = control.sizeDp.coerceIn(48f, maxSize)
            }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Size", style = MaterialTheme.typography.titleSmall)
                    Text("${sizeDraft.roundToInt()} dp", style = MaterialTheme.typography.labelMedium)
                }
                SmoothSlider(
                    value = control.sizeDp.coerceIn(48f, maxSize),
                    onValueChange = { v ->
                        sizeDragging = true
                        sizeDraft = v
                        onChange(control.copy(sizeDp = v))
                    },
                    onValueChangeFinished = { v ->
                        sizeDragging = false
                        sizeDraft = v
                        onChange(control.copy(sizeDp = v))
                    },
                    valueRange = 48f..maxSize,
                    accentColor = GeForceLime
                )
            }
        }

        item {
            var opacityDraft by remember(control.id) { mutableFloatStateOf(control.opacity.coerceIn(0.25f, 1.0f)) }
            var opacityDragging by remember(control.id) { mutableStateOf(false) }
            LaunchedEffect(control.opacity, opacityDragging) {
                if (!opacityDragging) opacityDraft = control.opacity.coerceIn(0.25f, 1.0f)
            }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Control Opacity", style = MaterialTheme.typography.titleSmall)
                    Text("${(opacityDraft * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                }
                SmoothSlider(
                    value = control.opacity.coerceIn(0.25f, 1.0f),
                    onValueChange = { v ->
                        opacityDragging = true
                        opacityDraft = v
                        onChange(control.copy(opacity = v))
                    },
                    onValueChangeFinished = { v ->
                        opacityDragging = false
                        opacityDraft = v
                        onChange(control.copy(opacity = v))
                    },
                    valueRange = 0.25f..1.0f,
                    accentColor = TacticalCyan
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
                    var turboDraft by remember(control.id) { mutableFloatStateOf(control.turboRateHz.toFloat()) }
                    var turboDragging by remember(control.id) { mutableStateOf(false) }
                    LaunchedEffect(control.turboRateHz, turboDragging) {
                        if (!turboDragging) turboDraft = control.turboRateHz.toFloat()
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Turbo Fire Rate", style = MaterialTheme.typography.bodyMedium)
                        Text("${turboDraft.roundToInt()} Hz", style = MaterialTheme.typography.labelMedium, color = TurboAmber)
                    }
                    SmoothSlider(
                        value = control.turboRateHz.toFloat().coerceIn(2f, 25f),
                        onValueChange = { v ->
                            turboDragging = true
                            turboDraft = v
                        },
                        onValueChangeFinished = { v ->
                            turboDragging = false
                            val hz = v.roundToInt().coerceIn(2, 25)
                            turboDraft = hz.toFloat()
                            onChange(control.copy(turboRateHz = hz))
                        },
                        valueRange = 2f..25f,
                        accentColor = TurboAmber
                    )
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (control.dragToAimEnabled) TacticalCyan.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.medium
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Drag Button to Aim / Turn", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Hold this button to press its key while sliding your finger to turn the camera",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = control.dragToAimEnabled,
                                onCheckedChange = { onChange(control.copy(dragToAimEnabled = it)) },
                                modifier = Modifier.testTag("inspector_drag_to_aim_switch")
                            )
                        }

                        if (control.dragToAimEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TouchpadMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = control.dragAimMode == mode,
                                        onClick = { onChange(control.copy(dragAimMode = mode)) },
                                        label = { Text(mode.displayName, fontSize = 11.sp) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("inspector_drag_aim_${mode.name.lowercase()}")
                                    )
                                }
                            }

                            var dragSensDraft by remember(control.id) {
                                mutableFloatStateOf(control.dragAimSensitivity.coerceIn(0.4f, 6.0f))
                            }
                            var dragSensDragging by remember(control.id) { mutableStateOf(false) }
                            LaunchedEffect(control.dragAimSensitivity, dragSensDragging) {
                                if (!dragSensDragging) {
                                    dragSensDraft = control.dragAimSensitivity.coerceIn(0.4f, 6.0f)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Drag Aim Sensitivity", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "%.2fx".format(dragSensDraft),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TacticalCyan
                                )
                            }
                            SmoothSlider(
                                value = control.dragAimSensitivity.coerceIn(0.4f, 6.0f),
                                onValueChange = { v ->
                                    dragSensDragging = true
                                    dragSensDraft = v
                                },
                                onValueChangeFinished = { v ->
                                    dragSensDragging = false
                                    dragSensDraft = v
                                    onChange(control.copy(dragAimSensitivity = v))
                                },
                                valueRange = 0.4f..6.0f,
                                accentColor = TacticalCyan
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Invert Drag Y-Axis", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = control.dragAimInvertY,
                                    onCheckedChange = { onChange(control.copy(dragAimInvertY = it)) },
                                    modifier = Modifier.testTag("inspector_drag_aim_invert_y")
                                )
                            }
                        }
                    }
                }
            }
        }

        if (control.controlType == ControlType.ANALOG_STICK || control.controlType == ControlType.TOUCHPAD) {
            if (control.controlType == ControlType.TOUCHPAD) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Trackpad Behavior", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TouchpadMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = control.touchpadMode == mode,
                                    onClick = { onChange(control.copy(touchpadMode = mode)) },
                                    label = { Text(mode.displayName, fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("inspector_touchpad_${mode.name.lowercase()}")
                                )
                            }
                        }
                        Text(
                            text = if (control.touchpadMode == TouchpadMode.MOUSE) {
                                "Full Mouse: unbounded 360° camera look & cursor movement. Higher sensitivity turns the camera further around without hitting a screen-edge wall."
                            } else {
                                "Right Analog Stick: smooth floating right thumbstick with follow-anchor (max turn speed follows the game's controller sensitivity)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (control.touchpadMode == TouchpadMode.MOUSE) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        MaterialTheme.shapes.medium
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Trackpad Tap Gestures", style = MaterialTheme.typography.titleSmall)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("1-Finger Tap Click (LMB)", style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                text = "Disable to prevent accidental clicks while swiping camera",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = control.touchpadTapToClickEnabled,
                                            onCheckedChange = {
                                                onChange(control.copy(touchpadTapToClickEnabled = it))
                                            },
                                            modifier = Modifier.testTag("inspector_touchpad_tap_click_switch")
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("2-Finger Tap Click (RMB)", style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                text = "Disable to ignore two-finger right-click taps on the pad",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = control.touchpadTwoFingerRightClickEnabled,
                                            onCheckedChange = {
                                                onChange(control.copy(touchpadTwoFingerRightClickEnabled = it))
                                            },
                                            modifier = Modifier.testTag("inspector_touchpad_two_finger_click_switch")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                val maxSens = if (control.controlType == ControlType.TOUCHPAD) 6.0f else 2.5f
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var deadzoneDraft by remember(control.id) { mutableFloatStateOf(control.deadzone.coerceIn(0.02f, 0.40f)) }
                    var deadzoneDragging by remember(control.id) { mutableStateOf(false) }
                    LaunchedEffect(control.deadzone, deadzoneDragging) {
                        if (!deadzoneDragging) deadzoneDraft = control.deadzone.coerceIn(0.02f, 0.40f)
                    }
                    var sensDraft by remember(control.id, maxSens) {
                        mutableFloatStateOf(control.sensitivity.coerceIn(0.5f, maxSens))
                    }
                    var sensDragging by remember(control.id) { mutableStateOf(false) }
                    LaunchedEffect(control.sensitivity, sensDragging, maxSens) {
                        if (!sensDragging) sensDraft = control.sensitivity.coerceIn(0.5f, maxSens)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (control.controlType == ControlType.TOUCHPAD) "Swipe Deadzone" else "Analog Deadzone",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text("${(deadzoneDraft * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                    }
                    SmoothSlider(
                        value = control.deadzone.coerceIn(0.02f, 0.40f),
                        onValueChange = { v ->
                            deadzoneDragging = true
                            deadzoneDraft = v
                        },
                        onValueChangeFinished = { v ->
                            deadzoneDragging = false
                            deadzoneDraft = v
                            onChange(control.copy(deadzone = v))
                        },
                        valueRange = 0.02f..0.40f,
                        accentColor = TacticalCyan
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (control.controlType == ControlType.TOUCHPAD) "Touchpad Turn Sensitivity" else "Sensitivity Curve",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text("%.2fx".format(sensDraft), style = MaterialTheme.typography.labelMedium)
                    }
                    SmoothSlider(
                        value = control.sensitivity.coerceIn(0.5f, maxSens),
                        onValueChange = { v ->
                            sensDragging = true
                            sensDraft = v
                        },
                        onValueChangeFinished = { v ->
                            sensDragging = false
                            sensDraft = v
                            onChange(control.copy(sensitivity = v))
                        },
                        valueRange = 0.5f..maxSens,
                        accentColor = GeForceLime
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
fun GridSettingsContent(
    gridSnapEnabled: Boolean,
    gridDivisions: Int,
    onGridSnapChange: (Boolean) -> Unit,
    onGridDivisionsChange: (Int) -> Unit
) {
    val divsX = gridDivisions.coerceIn(4, 32)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show & Snap Grid", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Buttons snap smoothly when released",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = gridSnapEnabled,
                onCheckedChange = onGridSnapChange,
                modifier = Modifier.testTag("editor_grid_snap_switch")
            )
        }
        Column {
            var gridDraft by remember { mutableFloatStateOf(divsX.toFloat()) }
            var gridDragging by remember { mutableStateOf(false) }
            LaunchedEffect(divsX, gridDragging) {
                if (!gridDragging) gridDraft = divsX.toFloat()
            }
            val displayDivsX = gridDraft.roundToInt().coerceIn(4, 32)
            val displayDivsY = (displayDivsX * 0.66f).roundToInt().coerceIn(4, 24)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Grid Density", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${displayDivsX} × ${displayDivsY} cells",
                    fontFamily = JetBrainsMonoFontFamily,
                    color = GeForceLime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            SmoothSlider(
                value = divsX.toFloat(),
                onValueChange = { v ->
                    gridDragging = true
                    gridDraft = v
                },
                onValueChangeFinished = { v ->
                    gridDragging = false
                    val snapped = v.roundToInt().coerceIn(4, 32)
                    gridDraft = snapped.toFloat()
                    onGridDivisionsChange(snapped)
                },
                valueRange = 4f..32f,
                accentColor = GeForceLime,
                modifier = Modifier.testTag("editor_grid_size_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Large (Coarse)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Small (Fine)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EditProfileMetaDialog(
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

package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VibrationMode
import com.example.input.InputBridge
import com.example.model.AxisPair
import com.example.model.CommandCatalog
import com.example.model.ControlShape
import com.example.model.ControlType
import com.example.model.ControllerNotation
import com.example.model.TriggerBehavior
import com.example.model.VirtualControl
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.util.SystemIntegrations
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun VirtualControlCanvasItem(
    control: VirtualControl,
    notation: ControllerNotation,
    globalOpacity: Float,
    vibrationMode: VibrationMode,
    isEditMode: Boolean,
    isSelected: Boolean,
    isPressed: Boolean,
    isToggled: Boolean,
    onSelect: () -> Unit,
    onDragDelta: (dxPx: Float, dyPx: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val currentVibration by rememberUpdatedState(vibrationMode)
    val currentControl by rememberUpdatedState(control)

    val effectiveAlpha = (control.opacity * globalOpacity).coerceIn(0.18f, 1.0f)
    val accentColor = Color(control.accent.hex)
    val sizeDp = control.sizeDp.dp

    val displayCode = remember(control.binding, notation) {
        CommandCatalog.formatFaceLabel(control.binding, notation)
    }

    var isTurboHolding by remember(control.id) { mutableStateOf(false) }

    LaunchedEffect(isTurboHolding, control.behavior, control.turboRateHz, isEditMode) {
        if (!isEditMode && isTurboHolding && control.behavior == TriggerBehavior.TURBO) {
            val intervalMs = (1000L / control.turboRateHz.coerceIn(2, 24)).coerceAtLeast(35L)
            while (isTurboHolding) {
                InputBridge.pulseTurbo(currentControl.label, currentControl.binding)
                delay(intervalMs)
            }
        }
    }

    val widthDp = when (control.shape) {
        ControlShape.PILL -> sizeDp * 1.45f
        ControlShape.PADDLE -> sizeDp * 1.35f
        else -> sizeDp
    }
    val heightDp = when (control.shape) {
        ControlShape.PILL -> sizeDp * 0.78f
        ControlShape.PADDLE -> sizeDp * 0.84f
        else -> sizeDp
    }

    Box(
        modifier = modifier
            .size(width = widthDp, height = heightDp)
            .alpha(if (isEditMode) 0.96f else effectiveAlpha)
            .testTag("virtual_control_${control.id}")
            .then(
                if (isEditMode) {
                    Modifier
                        .pointerInput(control.id) {
                            detectTapGestures(onTap = { onSelect() })
                        }
                        .pointerInput(control.id) {
                            detectDragGestures(
                                onDragStart = { onSelect() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDragDelta(dragAmount.x, dragAmount.y)
                                }
                            )
                        }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (control.controlType) {
            ControlType.ANALOG_STICK -> {
                AnalogStickSurface(
                    control = control,
                    accentColor = accentColor,
                    isEditMode = isEditMode,
                    isSelected = isSelected,
                    vibrationMode = currentVibration
                )
            }
            ControlType.DPAD_CLUSTER, ControlType.WASD_CLUSTER -> {
                DirectionalPadSurface(
                    control = control,
                    accentColor = accentColor,
                    isEditMode = isEditMode,
                    isSelected = isSelected,
                    vibrationMode = currentVibration
                )
            }
            else -> {
                ActionButtonSurface(
                    control = control,
                    displayCode = displayCode,
                    accentColor = accentColor,
                    isEditMode = isEditMode,
                    isSelected = isSelected,
                    isPressed = isPressed || isTurboHolding,
                    isToggled = isToggled,
                    onPressStart = {
                        SystemIntegrations.performKeyHaptic(
                            view = view,
                            vibrationMode = currentVibration,
                            controlEnabled = currentControl.hapticOnPress
                        )
                        when (currentControl.behavior) {
                            TriggerBehavior.HOLD -> {
                                InputBridge.pressControl(currentControl.id, currentControl.label, currentControl.binding)
                            }
                            TriggerBehavior.TOGGLE -> {
                                InputBridge.toggleControl(currentControl.id, currentControl.label, currentControl.binding)
                            }
                            TriggerBehavior.TURBO -> {
                                isTurboHolding = true
                            }
                        }
                    },
                    onPressEnd = {
                        when (currentControl.behavior) {
                            TriggerBehavior.HOLD -> {
                                InputBridge.releaseControl(currentControl.id, currentControl.label, currentControl.binding)
                            }
                            TriggerBehavior.TURBO -> {
                                isTurboHolding = false
                            }
                            TriggerBehavior.TOGGLE -> Unit
                        }
                    }
                )
            }
        }

        if (isEditMode && isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 2.dp,
                        color = Color(0xFF8CE000),
                        shape = RoundedCornerShape(14.dp)
                    )
            )
        }
    }
}

@Composable
private fun ActionButtonSurface(
    control: VirtualControl,
    displayCode: String,
    accentColor: Color,
    isEditMode: Boolean,
    isSelected: Boolean,
    isPressed: Boolean,
    isToggled: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit
) {
    val active = isPressed || isToggled
    val fillAlpha = if (active) 0.42f else 0.16f
    val borderColor = if (active) accentColor else accentColor.copy(alpha = 0.72f)
    val latestOnPressStart by rememberUpdatedState(onPressStart)
    val latestOnPressEnd by rememberUpdatedState(onPressEnd)

    val touchModifier = if (!isEditMode) {
        Modifier.pointerInput(control.id) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val pointerId = down.id
                down.consume()
                latestOnPressStart()
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val tracked = event.changes.firstOrNull { it.id == pointerId }
                        if (tracked == null || !tracked.pressed) {
                            break
                        }
                        tracked.consume()
                    }
                } finally {
                    latestOnPressEnd()
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(touchModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = if (active) 3.dp.toPx() else 1.8.dp.toPx()
            when (control.shape) {
                ControlShape.CIRCLE, ControlShape.RING -> {
                    val radius = (size.minDimension / 2f) - strokePx
                    drawCircle(
                        color = Color(0xFF0E141E).copy(alpha = 0.72f),
                        radius = radius
                    )
                    drawCircle(
                        color = accentColor.copy(alpha = fillAlpha),
                        radius = radius
                    )
                    drawCircle(
                        color = borderColor,
                        radius = radius,
                        style = Stroke(width = strokePx)
                    )
                }
                ControlShape.HEX -> {
                    val path = buildHexagonPath(size.width, size.height, strokePx)
                    drawPath(path, Color(0xFF0E141E).copy(alpha = 0.76f))
                    drawPath(path, accentColor.copy(alpha = fillAlpha))
                    drawPath(path, borderColor, style = Stroke(width = strokePx))
                }
                ControlShape.PADDLE -> {
                    val corner = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    drawRoundRect(
                        color = Color(0xFF0E141E).copy(alpha = 0.78f),
                        topLeft = Offset(strokePx, strokePx),
                        size = Size(size.width - strokePx * 2, size.height - strokePx * 2),
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = accentColor.copy(alpha = fillAlpha),
                        topLeft = Offset(strokePx, strokePx),
                        size = Size(size.width - strokePx * 2, size.height - strokePx * 2),
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset(strokePx, strokePx),
                        size = Size(size.width - strokePx * 2, size.height - strokePx * 2),
                        cornerRadius = corner,
                        style = Stroke(width = strokePx)
                    )
                }
                else -> {
                    val corner = CornerRadius(size.height / 2f, size.height / 2f)
                    drawRoundRect(
                        color = Color(0xFF0E141E).copy(alpha = 0.76f),
                        topLeft = Offset(strokePx, strokePx),
                        size = Size(size.width - strokePx * 2, size.height - strokePx * 2),
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = accentColor.copy(alpha = fillAlpha),
                        topLeft = Offset(strokePx, strokePx),
                        size = Size(size.width - strokePx * 2, size.height - strokePx * 2),
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset(strokePx, strokePx),
                        size = Size(size.width - strokePx * 2, size.height - strokePx * 2),
                        cornerRadius = corner,
                        style = Stroke(width = strokePx)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = control.label,
                color = if (active) Color.White else Color(0xFFF0F4FA),
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (control.label.length > 4) 11.sp else 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (control.label != displayCode || control.behavior != TriggerBehavior.HOLD) {
                val badge = buildString {
                    if (control.label != displayCode) append(displayCode)
                    if (control.behavior == TriggerBehavior.TOGGLE) {
                        if (isNotEmpty()) append(" • ")
                        append(if (isToggled) "ON" else "TGL")
                    } else if (control.behavior == TriggerBehavior.TURBO) {
                        if (isNotEmpty()) append(" • ")
                        append("${control.turboRateHz}Hz")
                    }
                }
                if (badge.isNotEmpty()) {
                    Text(
                        text = badge,
                        color = accentColor,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (isEditMode && isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8CE000))
            )
        }
    }
}

@Composable
private fun AnalogStickSurface(
    control: VirtualControl,
    accentColor: Color,
    isEditMode: Boolean,
    isSelected: Boolean,
    vibrationMode: VibrationMode
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val latestVibration by rememberUpdatedState(vibrationMode)
    var stickX by remember(control.id) { mutableFloatStateOf(0f) }
    var stickY by remember(control.id) { mutableFloatStateOf(0f) }
    var isActive by remember(control.id) { mutableStateOf(false) }

    val radiusPx = with(density) { (control.sizeDp.dp / 2f).toPx() }
    val thumbRadiusPx = radiusPx * 0.36f
    val maxTravel = (radiusPx - thumbRadiusPx).coerceAtLeast(12f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(control.id, control.deadzone, control.sensitivity) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pointerId = down.id
                            down.consume()
                            isActive = true
                            SystemIntegrations.performKeyHaptic(
                                view = view,
                                vibrationMode = latestVibration,
                                controlEnabled = control.hapticOnPress
                            )
                            val center = Offset(size.width / 2f, size.height / 2f)

                            fun updateFromPos(pos: Offset) {
                                val dx = pos.x - center.x
                                val dy = pos.y - center.y
                                val dist = hypot(dx, dy)
                                val clamped = dist.coerceAtMost(maxTravel)
                                val angle = kotlin.math.atan2(dy, dx)
                                val normX = if (maxTravel > 0f) (clamped * cos(angle)) / maxTravel else 0f
                                val normY = if (maxTravel > 0f) (clamped * sin(angle)) / maxTravel else 0f
                                stickX = normX
                                stickY = normY
                                InputBridge.updateAnalogStick(
                                    axisPair = control.binding.axisPair,
                                    sourceLabel = control.label,
                                    rawX = normX,
                                    rawY = normY,
                                    deadzone = control.deadzone,
                                    sensitivity = control.sensitivity,
                                    active = true
                                )
                            }

                            updateFromPos(down.position)
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pointer = event.changes.firstOrNull { it.id == pointerId }
                                    if (pointer == null || !pointer.pressed) {
                                        break
                                    }
                                    pointer.consume()
                                    updateFromPos(pointer.position)
                                }
                            } finally {
                                isActive = false
                                stickX = 0f
                                stickY = 0f
                                InputBridge.updateAnalogStick(
                                    axisPair = control.binding.axisPair,
                                    sourceLabel = control.label,
                                    rawX = 0f,
                                    rawY = 0f,
                                    deadzone = control.deadzone,
                                    sensitivity = control.sensitivity,
                                    active = false
                                )
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = (size.minDimension / 2f) - 2.dp.toPx()

            drawCircle(
                color = Color(0xFF0C121B).copy(alpha = 0.62f),
                radius = outerR
            )
            drawCircle(
                color = accentColor.copy(alpha = if (isActive || isSelected) 0.85f else 0.45f),
                radius = outerR,
                style = Stroke(width = if (isActive) 2.5.dp.toPx() else 1.5.dp.toPx())
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.22f),
                radius = outerR * control.deadzone.coerceIn(0.05f, 0.45f),
                style = Stroke(width = 1.dp.toPx())
            )
            drawLine(
                color = accentColor.copy(alpha = 0.16f),
                start = Offset(center.x - outerR * 0.7f, center.y),
                end = Offset(center.x + outerR * 0.7f, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = accentColor.copy(alpha = 0.16f),
                start = Offset(center.x, center.y - outerR * 0.7f),
                end = Offset(center.x, center.y + outerR * 0.7f),
                strokeWidth = 1.dp.toPx()
            )

            val puckCenter = Offset(
                x = center.x + stickX * maxTravel,
                y = center.y + stickY * maxTravel
            )
            if (isActive) {
                drawLine(
                    color = accentColor.copy(alpha = 0.65f),
                    start = center,
                    end = puckCenter,
                    strokeWidth = 2.dp.toPx()
                )
            }
            drawCircle(
                color = Color(0xFF1A2434).copy(alpha = 0.92f),
                radius = thumbRadiusPx,
                center = puckCenter
            )
            drawCircle(
                color = accentColor.copy(alpha = if (isActive) 0.45f else 0.24f),
                radius = thumbRadiusPx,
                center = puckCenter
            )
            drawCircle(
                color = accentColor,
                radius = thumbRadiusPx,
                center = puckCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Text(
            text = control.label,
            color = Color(0xFFF0F4FA).copy(alpha = 0.85f),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
    }
}

@Composable
private fun DirectionalPadSurface(
    control: VirtualControl,
    accentColor: Color,
    isEditMode: Boolean,
    isSelected: Boolean,
    vibrationMode: VibrationMode
) {
    val view = LocalView.current
    val latestVibration by rememberUpdatedState(vibrationMode)
    var activeX by remember(control.id) { mutableFloatStateOf(0f) }
    var activeY by remember(control.id) { mutableFloatStateOf(0f) }
    var isPressed by remember(control.id) { mutableStateOf(false) }

    val isWasd = control.binding.axisPair == AxisPair.WASD_KEYS
    val labels = if (isWasd) listOf("W", "D", "S", "A") else listOf("▲", "▶", "▼", "◀")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(control.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pointerId = down.id
                            down.consume()
                            isPressed = true
                            SystemIntegrations.performKeyHaptic(
                                view = view,
                                vibrationMode = latestVibration,
                                controlEnabled = control.hapticOnPress
                            )
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxR = (size.width.coerceAtMost(size.height) / 2f).coerceAtLeast(16f)

                            fun dispatchAt(pos: Offset) {
                                val nx = ((pos.x - center.x) / maxR).coerceIn(-1f, 1f)
                                val ny = ((pos.y - center.y) / maxR).coerceIn(-1f, 1f)
                                activeX = nx
                                activeY = ny
                                InputBridge.updateDirectionalCluster(
                                    controlId = control.id,
                                    sourceLabel = control.label,
                                    axisPair = control.binding.axisPair,
                                    x = nx,
                                    y = ny,
                                    active = true
                                )
                            }

                            dispatchAt(down.position)
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pointer = event.changes.firstOrNull { it.id == pointerId }
                                    if (pointer == null || !pointer.pressed) {
                                        break
                                    }
                                    pointer.consume()
                                    dispatchAt(pointer.position)
                                }
                            } finally {
                                isPressed = false
                                activeX = 0f
                                activeY = 0f
                                InputBridge.updateDirectionalCluster(
                                    controlId = control.id,
                                    sourceLabel = control.label,
                                    axisPair = control.binding.axisPair,
                                    x = 0f,
                                    y = 0f,
                                    active = false
                                )
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val arm = w * 0.34f
            val pad = 3.dp.toPx()

            drawRoundRect(
                color = Color(0xFF0E141E).copy(alpha = 0.78f),
                topLeft = Offset((w - arm) / 2f, pad),
                size = Size(arm, h - pad * 2),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF0E141E).copy(alpha = 0.78f),
                topLeft = Offset(pad, (h - arm) / 2f),
                size = Size(w - pad * 2, arm),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )

            if (isPressed && activeY < -0.35f) {
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.48f),
                    topLeft = Offset((w - arm) / 2f, pad),
                    size = Size(arm, h * 0.36f),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }
            if (isPressed && activeY > 0.35f) {
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.48f),
                    topLeft = Offset((w - arm) / 2f, h * 0.64f - pad),
                    size = Size(arm, h * 0.36f),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }
            if (isPressed && activeX < -0.35f) {
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.48f),
                    topLeft = Offset(pad, (h - arm) / 2f),
                    size = Size(w * 0.36f, arm),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }
            if (isPressed && activeX > 0.35f) {
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.48f),
                    topLeft = Offset(w * 0.64f - pad, (h - arm) / 2f),
                    size = Size(w * 0.36f, arm),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }

            val strokeColor = if (isPressed || isSelected) accentColor else accentColor.copy(alpha = 0.6f)
            drawCircle(
                color = strokeColor,
                radius = w * 0.47f,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Text(
            text = labels[0],
            color = Color.White,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
        )
        Text(
            text = labels[1],
            color = Color.White,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )
        Text(
            text = labels[2],
            color = Color.White,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
        Text(
            text = labels[3],
            color = Color.White,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        )
    }
}

private fun buildHexagonPath(width: Float, height: Float, inset: Float): Path {
    val cx = width / 2f
    val cy = height / 2f
    val rx = (width / 2f) - inset
    val ry = (height / 2f) - inset
    return Path().apply {
        for (i in 0 until 6) {
            val angle = (PI / 3.0) * i - (PI / 6.0)
            val x = cx + rx * cos(angle).toFloat()
            val y = cy + ry * sin(angle).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

@Composable
fun PositionedVirtualControl(
    control: VirtualControl,
    containerWidthPx: Float,
    containerHeightPx: Float,
    notation: ControllerNotation,
    globalOpacity: Float,
    vibrationMode: VibrationMode,
    isEditMode: Boolean,
    isSelected: Boolean,
    isPressed: Boolean,
    isToggled: Boolean,
    onSelect: () -> Unit,
    onDragDelta: (dxPx: Float, dyPx: Float) -> Unit
) {
    val density = LocalDensity.current
    val widthDp = when (control.shape) {
        ControlShape.PILL -> control.sizeDp * 1.45f
        ControlShape.PADDLE -> control.sizeDp * 1.35f
        else -> control.sizeDp
    }
    val heightDp = when (control.shape) {
        ControlShape.PILL -> control.sizeDp * 0.78f
        ControlShape.PADDLE -> control.sizeDp * 0.84f
        else -> control.sizeDp
    }
    val wPx = with(density) { widthDp.dp.toPx() }
    val hPx = with(density) { heightDp.dp.toPx() }

    val xPx = (control.xPercent * containerWidthPx - wPx / 2f)
        .coerceIn(0f, (containerWidthPx - wPx).coerceAtLeast(0f))
    val yPx = (control.yPercent * containerHeightPx - hPx / 2f)
        .coerceIn(0f, (containerHeightPx - hPx).coerceAtLeast(0f))

    Box(
        modifier = Modifier.offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
    ) {
        VirtualControlCanvasItem(
            control = control,
            notation = notation,
            globalOpacity = globalOpacity,
            vibrationMode = vibrationMode,
            isEditMode = isEditMode,
            isSelected = isSelected,
            isPressed = isPressed,
            isToggled = isToggled,
            onSelect = onSelect,
            onDragDelta = onDragDelta
        )
    }
}

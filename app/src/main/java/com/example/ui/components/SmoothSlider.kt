package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Silky-smooth 120Hz zero-stutter custom slider.
 *
 * Eliminates the classic Compose + DataStore drag stutter by:
 * 1. Keeping an immediate local drag state (`localValue`) that ignores stale async prop
 *    emissions while `isDragging` is true (zero rubber-banding).
 * 2. Handling direct pointer drag/tap geometry on a hardware Canvas with zero step-snapping
 *    jitter during movement (optional snap on commit or smooth float interpolation).
 * 3. Separating ultra-fast in-memory preview (`onValueChange`) from disk persistence
 *    (`onValueChangeFinished`).
 */
@Composable
fun SmoothSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    accentColor: Color = Color(0xFF76B900),
    inactiveTrackColor: Color = Color(0xFF283344),
    trackBrush: Brush? = null,
    centerOrigin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val clampedExternal = value.coerceIn(valueRange.start, valueRange.endInclusive)

    var localValue by remember { mutableFloatStateOf(clampedExternal) }
    var isDragging by remember { mutableStateOf(false) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestOnFinished by rememberUpdatedState(onValueChangeFinished)
    val latestRange by rememberUpdatedState(valueRange)

    LaunchedEffect(clampedExternal, isDragging) {
        if (!isDragging) {
            localValue = clampedExternal
        }
    }

    fun pxToValue(xPx: Float): Float {
        val pad = 20f
        val usable = (trackWidthPx - pad * 2f).coerceAtLeast(1f)
        val fraction = ((xPx - pad) / usable).coerceIn(0f, 1f)
        return latestRange.start + fraction * (latestRange.endInclusive - latestRange.start)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(valueRange.start, valueRange.endInclusive) {
                detectTapGestures { offset ->
                    val target = pxToValue(offset.x)
                    localValue = target
                    latestOnChange(target)
                    latestOnFinished?.invoke(target)
                }
            }
            .pointerInput(valueRange.start, valueRange.endInclusive) {
                detectHorizontalDragGestures(
                    onDragStart = { startOffset ->
                        isDragging = true
                        val target = pxToValue(startOffset.x)
                        localValue = target
                        latestOnChange(target)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val target = pxToValue(change.position.x)
                        if (abs(target - localValue) > span * 0.001f) {
                            localValue = target
                            latestOnChange(target)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        latestOnFinished?.invoke(localValue)
                    },
                    onDragCancel = {
                        isDragging = false
                        latestOnFinished?.invoke(localValue)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            val pad = 12.dp.toPx()
            val trackHeight = 5.dp.toPx()
            val centerY = size.height / 2f
            val usableW = (size.width - pad * 2f).coerceAtLeast(1f)
            val fraction = ((localValue - valueRange.start) / span).coerceIn(0f, 1f)
            val thumbX = pad + fraction * usableW

            // Inactive base track (or custom spectrum gradient)
            if (trackBrush != null) {
                drawRoundRect(
                    brush = trackBrush,
                    topLeft = Offset(pad, centerY - trackHeight / 2f),
                    size = Size(usableW, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                )
            } else {
                drawRoundRect(
                    color = inactiveTrackColor,
                    topLeft = Offset(pad, centerY - trackHeight / 2f),
                    size = Size(usableW, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                )

                // Active fill (either from start or from 0-center origin for bipolar sliders)
                if (centerOrigin && valueRange.start < 0f && valueRange.endInclusive > 0f) {
                    val zeroFrac = ((0f - valueRange.start) / span).coerceIn(0f, 1f)
                    val zeroX = pad + zeroFrac * usableW
                    val leftX = minOf(zeroX, thumbX)
                    val fillW = abs(thumbX - zeroX).coerceAtLeast(1f)
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(leftX, centerY - trackHeight / 2f),
                        size = Size(fillW, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                    )
                    // Subtle zero tick marker
                    drawLine(
                        color = Color(0xAAFFFFFF),
                        start = Offset(zeroX, centerY - 6.dp.toPx()),
                        end = Offset(zeroX, centerY + 6.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                } else {
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(pad, centerY - trackHeight / 2f),
                        size = Size((thumbX - pad).coerceAtLeast(0f), trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                    )
                }
            }

            // Outer glow ring while dragging
            if (isDragging) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.28f),
                    radius = 14.dp.toPx(),
                    center = Offset(thumbX, centerY)
                )
            }

            // Crisp thumb with subtle dark rim
            val thumbRadius = if (isDragging) 9.5.dp.toPx() else 8.dp.toPx()
            drawCircle(
                color = Color(0xFF0A0E14),
                radius = thumbRadius + 1.5.dp.toPx(),
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = if (trackBrush != null) Color.White else accentColor,
                radius = thumbRadius,
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = thumbRadius * 0.35f,
                center = Offset(thumbX, centerY)
            )
        }
    }
}

/**
 * Labeled GeForce Freestyle slider with live local numeric badge and tap-badge-to-reset.
 */
@Composable
fun SmoothFilterSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    testTag: String,
    defaultValue: Float = 0f,
    centerOrigin: Boolean = valueRange.start < 0f && valueRange.endInclusive > 0f,
    trackBrush: Brush? = null,
    valueFormatter: ((Float) -> String)? = null
) {
    val clampedExternal = value.coerceIn(valueRange.start, valueRange.endInclusive)
    var displayVal by remember { mutableFloatStateOf(clampedExternal) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(clampedExternal, dragging) {
        if (!dragging) displayVal = clampedExternal
    }

    val pct = (displayVal * 100f).roundToInt()
    val readout = valueFormatter?.invoke(displayVal) ?: when {
        pct > 0 && centerOrigin -> "+$pct%"
        else -> "$pct%"
    }
    val isAtDefault = abs(displayVal - defaultValue) < 0.015f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color(0xFFD6E0EE),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Surface(
                color = Color(0xFF0A0E14),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .border(
                        1.dp,
                        if (isAtDefault) Color(0xFF283446) else accentColor.copy(alpha = 0.55f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable {
                        val resetVal = defaultValue.coerceIn(valueRange.start, valueRange.endInclusive)
                        displayVal = resetVal
                        onValueChange(resetVal)
                        onValueChangeFinished?.invoke(resetVal)
                    }
            ) {
                Text(
                    text = readout,
                    color = if (isAtDefault) Color(0xFF8C9EB5) else accentColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
        SmoothSlider(
            value = clampedExternal,
            valueRange = valueRange,
            accentColor = accentColor,
            centerOrigin = centerOrigin,
            trackBrush = trackBrush,
            onValueChange = { next ->
                dragging = true
                displayVal = next
                onValueChange(next)
            },
            onValueChangeFinished = { finalVal ->
                dragging = false
                displayVal = finalVal
                onValueChangeFinished?.invoke(finalVal)
            },
            modifier = Modifier.testTag(testTag)
        )
    }
}

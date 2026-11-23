package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.input.InputBridge
import com.example.model.TargetPlatformMode
import kotlin.math.hypot

class NexusAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    // LMB / RMB hold strokes
    private var activeLeftStroke: GestureDescription.StrokeDescription? = null
    private var leftGestureInFlight = false
    private var pendingLeftRelease = false

    private var activeRightStroke: GestureDescription.StrokeDescription? = null
    private var rightGestureInFlight = false
    private var pendingRightRelease = false

    // Continuous 60fps Touchpad Mouse Camera Panning (unbroken StrokeDescription chaining)
    private var isMousePanRequested = false
    private var panStrokeInFlight = false
    private var activePanStroke: GestureDescription.StrokeDescription? = null
    private var panCursorX = 0f
    private var panCursorY = 0f
    private var pendingPanDx = 0f
    private var pendingPanDy = 0f

    override fun onServiceConnected() {
        super.onServiceConnected()
        InputBridge.attachPointerSink(object : InputBridge.PointerGestureSink {
            override fun onLeftButton(isDown: Boolean) {
                runOnMain { dispatchLeftButton(isDown) }
            }

            override fun onRightButton(isDown: Boolean) {
                runOnMain { dispatchRightButton(isDown) }
            }

            override fun onMiddleClick() {
                runOnMain { dispatchMiddleTap() }
            }

            override fun onScroll(scrollUp: Boolean) {
                runOnMain { dispatchTwoFingerScroll(scrollUp) }
            }

            override fun onMouseSwipeStart() {
                runOnMain { startMousePan() }
            }

            override fun onMouseSwipe(dxPx: Float, dyPx: Float) {
                runOnMain { queueMousePanDelta(dxPx, dyPx) }
            }

            override fun onMouseSwipeEnd() {
                runOnMain { endMousePan() }
            }

            override fun onMouseFlick(dxPx: Float, dyPx: Float) {
                runOnMain { dispatchMouseFlick(dxPx, dyPx) }
            }
        })
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        InputBridge.attachPointerSink(null)
        resetAllStrokes()
        super.onDestroy()
    }

    private fun resetAllStrokes() {
        activeLeftStroke = null
        leftGestureInFlight = false
        pendingLeftRelease = false
        activeRightStroke = null
        rightGestureInFlight = false
        pendingRightRelease = false
        isMousePanRequested = false
        panStrokeInFlight = false
        activePanStroke = null
        pendingPanDx = 0f
        pendingPanDy = 0f
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun screenBoundsAndDensity(): Triple<Float, Float, Float> {
        val wm = getSystemService(WINDOW_SERVICE) as? WindowManager
        val density = resources.displayMetrics.density
        if (wm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            if (bounds.width() > 0 && bounds.height() > 0) {
                return Triple(bounds.width().toFloat(), bounds.height().toFloat(), density)
            }
        }
        val realDm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm?.defaultDisplay?.getRealMetrics(realDm)
        val w = (if (realDm.widthPixels > 0) realDm.widthPixels else resources.displayMetrics.widthPixels).toFloat()
        val h = (if (realDm.heightPixels > 0) realDm.heightPixels else resources.displayMetrics.heightPixels).toFloat()
        return Triple(w, h, density)
    }

    private data class PanZone(
        val anchorX: Float,
        val anchorY: Float,
        val minX: Float,
        val maxX: Float,
        val minY: Float,
        val maxY: Float
    )

    private fun currentPanZone(): PanZone {
        val (w, h, _) = screenBoundsAndDensity()
        return when (InputBridge.targetMode.value) {
            TargetPlatformMode.GEFORCE_NOW -> PanZone(
                anchorX = w * 0.50f,
                anchorY = h * 0.48f,
                minX = w * 0.14f,
                maxX = w * 0.86f,
                minY = h * 0.14f,
                maxY = h * 0.84f
            )
            // Roblox, Minecraft, and mobile shooters pan the camera from the right half of the screen
            TargetPlatformMode.ANDROID_NATIVE -> PanZone(
                anchorX = w * 0.72f,
                anchorY = h * 0.46f,
                minX = w * 0.52f,
                maxX = w * 0.94f,
                minY = h * 0.14f,
                maxY = h * 0.84f
            )
        }
    }

    private fun startMousePan() {
        val zone = currentPanZone()
        isMousePanRequested = true
        pendingPanDx = 0f
        pendingPanDy = 0f
        if (!panStrokeInFlight && activePanStroke == null) {
            panCursorX = zone.anchorX
            panCursorY = zone.anchorY
        }
    }

    private fun queueMousePanDelta(dxPx: Float, dyPx: Float) {
        if (!isMousePanRequested) {
            startMousePan()
        }
        pendingPanDx += dxPx
        pendingPanDy += dyPx
        if (!panStrokeInFlight && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stepMousePanStroke()
        }
    }

    private fun endMousePan() {
        isMousePanRequested = false
        pendingPanDx = 0f
        pendingPanDy = 0f
        if (!panStrokeInFlight && activePanStroke != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            finishPanStroke()
        }
    }

    private fun stepMousePanStroke() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (panStrokeInFlight) return

        val zone = currentPanZone()
        val dx = pendingPanDx
        val dy = pendingPanDy
        pendingPanDx = 0f
        pendingPanDy = 0f

        val current = activePanStroke
        if (!isMousePanRequested) {
            if (current != null) finishPanStroke()
            return
        }
        if (hypot(dx, dy) < 0.35f) {
            return
        }

        if (current == null) {
            panCursorX = zone.anchorX
            panCursorY = zone.anchorY
            val targetX = (panCursorX + dx).coerceIn(zone.minX, zone.maxX)
            val targetY = (panCursorY + dy).coerceIn(zone.minY, zone.maxY)
            val path = Path().apply {
                moveTo(panCursorX, panCursorY)
                lineTo(targetX, targetY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 16L, true)
            activePanStroke = stroke
            panStrokeInFlight = true
            panCursorX = targetX
            panCursorY = targetY
            dispatchPanGesture(stroke)
        } else {
            val rawNextX = panCursorX + dx
            val rawNextY = panCursorY + dy
            val hitEdge = rawNextX <= zone.minX || rawNextX >= zone.maxX ||
                rawNextY <= zone.minY || rawNextY >= zone.maxY

            if (hitEdge) {
                // Reached pan zone edge: lift finger cleanly and reset to anchor so 360° rotation never stops
                val path = Path().apply {
                    moveTo(panCursorX, panCursorY)
                    lineTo(rawNextX.coerceIn(zone.minX, zone.maxX), rawNextY.coerceIn(zone.minY, zone.maxY))
                }
                val endStroke = current.continueStroke(path, 0, 12L, false)
                activePanStroke = null
                panCursorX = zone.anchorX
                panCursorY = zone.anchorY
                panStrokeInFlight = true
                dispatchPanGesture(endStroke)
            } else {
                val path = Path().apply {
                    moveTo(panCursorX, panCursorY)
                    lineTo(rawNextX, rawNextY)
                }
                val nextStroke = current.continueStroke(path, 0, 16L, true)
                activePanStroke = nextStroke
                panCursorX = rawNextX
                panCursorY = rawNextY
                panStrokeInFlight = true
                dispatchPanGesture(nextStroke)
            }
        }
    }

    private fun finishPanStroke() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val current = activePanStroke ?: return
        activePanStroke = null
        panStrokeInFlight = true
        val path = Path().apply { moveTo(panCursorX, panCursorY) }
        val releaseStroke = current.continueStroke(path, 0, 10L, false)
        dispatchPanGesture(releaseStroke)
    }

    private fun dispatchPanGesture(stroke: GestureDescription.StrokeDescription) {
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        val dispatched = runCatching {
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    panStrokeInFlight = false
                    if (isMousePanRequested) {
                        if (hypot(pendingPanDx, pendingPanDy) >= 0.35f) {
                            stepMousePanStroke()
                        }
                    } else if (activePanStroke != null) {
                        finishPanStroke()
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    panStrokeInFlight = false
                    activePanStroke = null
                    pendingPanDx = 0f
                    pendingPanDy = 0f
                }
            }, mainHandler)
        }.getOrDefault(false)
        if (!dispatched) {
            panStrokeInFlight = false
            activePanStroke = null
        }
    }

    private fun dispatchMouseFlick(dxPx: Float, dyPx: Float) {
        val zone = currentPanZone()
        val dist = hypot(dxPx, dyPx)
        if (dist < 1f) return
        val endX = (zone.anchorX + dxPx).coerceIn(zone.minX, zone.maxX)
        val endY = (zone.anchorY + dyPx).coerceIn(zone.minY, zone.maxY)
        val durationMs = (dist * 0.45f).toLong().coerceIn(55L, 165L)
        val path = Path().apply {
            moveTo(zone.anchorX, zone.anchorY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        runCatching { dispatchGesture(gesture, null, null) }
    }

    private fun dispatchLeftButton(isDown: Boolean) {
        // Never interrupt active camera panning gesture — IME KeyEvent(BTN_LEFT scan=272) still fires immediately
        if (isMousePanRequested || panStrokeInFlight || activePanStroke != null) return

        val (w, h, _) = screenBoundsAndDensity()
        val cx = w * 0.50f
        val cy = h * 0.50f
        val path = Path().apply { moveTo(cx, cy) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isDown) {
                pendingLeftRelease = false
                leftGestureInFlight = true
                val stroke = GestureDescription.StrokeDescription(path, 0, 40L, true)
                activeLeftStroke = stroke
                val gesture = GestureDescription.Builder().addStroke(stroke).build()
                runCatching {
                    dispatchGesture(gesture, object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            leftGestureInFlight = false
                            if (pendingLeftRelease) {
                                pendingLeftRelease = false
                                finishLeftStroke(cx, cy)
                            }
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            leftGestureInFlight = false
                            pendingLeftRelease = false
                            activeLeftStroke = null
                        }
                    }, mainHandler)
                }
            } else {
                if (leftGestureInFlight) {
                    pendingLeftRelease = true
                } else {
                    finishLeftStroke(cx, cy)
                }
            }
        } else if (isDown) {
            val stroke = GestureDescription.StrokeDescription(path, 0, 50L)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            runCatching { dispatchGesture(gesture, null, null) }
        }
    }

    private fun finishLeftStroke(cx: Float, cy: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val prev = activeLeftStroke ?: return
        activeLeftStroke = null
        val path = Path().apply { moveTo(cx, cy) }
        val releaseStroke = prev.continueStroke(path, 0, 16L, false)
        val gesture = GestureDescription.Builder().addStroke(releaseStroke).build()
        runCatching { dispatchGesture(gesture, null, null) }
    }

    private fun dispatchRightButton(isDown: Boolean) {
        if (isMousePanRequested || panStrokeInFlight || activePanStroke != null) return
        val (w, h, density) = screenBoundsAndDensity()
        val cx = w * 0.50f
        val cy = h * 0.50f

        when (InputBridge.targetMode.value) {
            TargetPlatformMode.GEFORCE_NOW -> {
                if (!isDown) return
                val offset = 28f * density
                val leftPath = Path().apply { moveTo(cx - offset, cy) }
                val rightPath = Path().apply { moveTo(cx + offset, cy) }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(leftPath, 0, 55L))
                    .addStroke(GestureDescription.StrokeDescription(rightPath, 0, 55L))
                    .build()
                runCatching { dispatchGesture(gesture, null, null) }
            }
            TargetPlatformMode.ANDROID_NATIVE -> {
                val tapX = w * 0.64f
                val tapY = h * 0.50f
                val path = Path().apply { moveTo(tapX, tapY) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (isDown) {
                        pendingRightRelease = false
                        rightGestureInFlight = true
                        val stroke = GestureDescription.StrokeDescription(path, 0, 45L, true)
                        activeRightStroke = stroke
                        val gesture = GestureDescription.Builder().addStroke(stroke).build()
                        runCatching {
                            dispatchGesture(gesture, object : GestureResultCallback() {
                                override fun onCompleted(gestureDescription: GestureDescription?) {
                                    rightGestureInFlight = false
                                    if (pendingRightRelease) {
                                        pendingRightRelease = false
                                        finishRightStroke(tapX, tapY)
                                    }
                                }

                                override fun onCancelled(gestureDescription: GestureDescription?) {
                                    rightGestureInFlight = false
                                    pendingRightRelease = false
                                    activeRightStroke = null
                                }
                            }, mainHandler)
                        }
                    } else {
                        if (rightGestureInFlight) {
                            pendingRightRelease = true
                        } else {
                            finishRightStroke(tapX, tapY)
                        }
                    }
                }
            }
        }
    }

    private fun finishRightStroke(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val prev = activeRightStroke ?: return
        activeRightStroke = null
        val path = Path().apply { moveTo(x, y) }
        val releaseStroke = prev.continueStroke(path, 0, 16L, false)
        val gesture = GestureDescription.Builder().addStroke(releaseStroke).build()
        runCatching { dispatchGesture(gesture, null, null) }
    }

    private fun dispatchMiddleTap() {
        if (isMousePanRequested || panStrokeInFlight || activePanStroke != null) return
        val (w, h, density) = screenBoundsAndDensity()
        val cx = w * 0.50f
        val cy = h * 0.50f
        val gesture = if (InputBridge.targetMode.value == TargetPlatformMode.GEFORCE_NOW) {
            val offset = 36f * density
            val p1 = Path().apply { moveTo(cx - offset, cy) }
            val p2 = Path().apply { moveTo(cx, cy) }
            val p3 = Path().apply { moveTo(cx + offset, cy) }
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(p1, 0, 55L))
                .addStroke(GestureDescription.StrokeDescription(p2, 0, 55L))
                .addStroke(GestureDescription.StrokeDescription(p3, 0, 55L))
                .build()
        } else {
            val p = Path().apply { moveTo(cx, cy) }
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(p, 0, 45L))
                .build()
        }
        runCatching { dispatchGesture(gesture, null, null) }
    }

    private fun dispatchTwoFingerScroll(scrollUp: Boolean) {
        if (isMousePanRequested || panStrokeInFlight || activePanStroke != null) return
        val (w, h, density) = screenBoundsAndDensity()
        val cx = w * 0.50f
        val cy = h * 0.50f
        val offset = 28f * density
        val travel = 96f * density
        val startY = if (scrollUp) cy - travel * 0.5f else cy + travel * 0.5f
        val endY = if (scrollUp) cy + travel * 0.5f else cy - travel * 0.5f

        val p1 = Path().apply {
            moveTo(cx - offset, startY)
            lineTo(cx - offset, endY)
        }
        val p2 = Path().apply {
            moveTo(cx + offset, startY)
            lineTo(cx + offset, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p1, 0, 120L))
            .addStroke(GestureDescription.StrokeDescription(p2, 0, 120L))
            .build()
        runCatching { dispatchGesture(gesture, null, null) }
    }
}

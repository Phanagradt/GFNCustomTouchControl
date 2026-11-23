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
import kotlin.math.hypot

class NexusAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeLeftStroke: GestureDescription.StrokeDescription? = null
    private var leftGestureInFlight = false
    private var pendingLeftRelease = false
    private var lastSwipeTimestamp = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        InputBridge.attachPointerSink(object : InputBridge.PointerGestureSink {
            override fun onLeftButton(isDown: Boolean) {
                runOnMain { dispatchLeftButton(isDown) }
            }

            override fun onRightButton(isDown: Boolean) {
                runOnMain {
                    if (isDown) {
                        dispatchRightTap()
                    }
                }
            }

            override fun onMiddleClick() {
                runOnMain { dispatchMiddleTap() }
            }

            override fun onScroll(scrollUp: Boolean) {
                runOnMain { dispatchTwoFingerScroll(scrollUp) }
            }

            override fun onMouseSwipe(dxPx: Float, dyPx: Float) {
                runOnMain { dispatchMouseSwipeDelta(dxPx, dyPx) }
            }
        })
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        InputBridge.attachPointerSink(null)
        activeLeftStroke = null
        leftGestureInFlight = false
        pendingLeftRelease = false
        super.onDestroy()
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun screenCenterAndDensity(): Triple<Float, Float, Float> {
        val wm = getSystemService(WINDOW_SERVICE) as? WindowManager
        val density = resources.displayMetrics.density
        if (wm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            return Triple(bounds.width() * 0.5f, bounds.height() * 0.5f, density)
        }
        val realDm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm?.defaultDisplay?.getRealMetrics(realDm)
        val w = if (realDm.widthPixels > 0) realDm.widthPixels else resources.displayMetrics.widthPixels
        val h = if (realDm.heightPixels > 0) realDm.heightPixels else resources.displayMetrics.heightPixels
        return Triple(w * 0.5f, h * 0.5f, density)
    }

    private fun dispatchLeftButton(isDown: Boolean) {
        val (cx, cy, _) = screenCenterAndDensity()
        val path = Path().apply { moveTo(cx, cy) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isDown) {
                pendingLeftRelease = false
                leftGestureInFlight = true
                val stroke = GestureDescription.StrokeDescription(path, 0, 45L, true)
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
            val stroke = GestureDescription.StrokeDescription(path, 0, 55L)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            runCatching { dispatchGesture(gesture, null, null) }
        }
    }

    private fun finishLeftStroke(cx: Float, cy: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val prev = activeLeftStroke ?: return
        activeLeftStroke = null
        val path = Path().apply { moveTo(cx, cy) }
        val releaseStroke = prev.continueStroke(path, 0, 20L, false)
        val gesture = GestureDescription.Builder().addStroke(releaseStroke).build()
        runCatching { dispatchGesture(gesture, null, null) }
    }

    private fun dispatchRightTap() {
        val (cx, cy, density) = screenCenterAndDensity()
        val offset = 28f * density
        val leftPath = Path().apply { moveTo(cx - offset, cy) }
        val rightPath = Path().apply { moveTo(cx + offset, cy) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(leftPath, 0, 60L))
            .addStroke(GestureDescription.StrokeDescription(rightPath, 0, 60L))
            .build()
        runCatching { dispatchGesture(gesture, null, null) }
    }

    private fun dispatchMiddleTap() {
        val (cx, cy, density) = screenCenterAndDensity()
        val offset = 36f * density
        val p1 = Path().apply { moveTo(cx - offset, cy) }
        val p2 = Path().apply { moveTo(cx, cy) }
        val p3 = Path().apply { moveTo(cx + offset, cy) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p1, 0, 65L))
            .addStroke(GestureDescription.StrokeDescription(p2, 0, 65L))
            .addStroke(GestureDescription.StrokeDescription(p3, 0, 65L))
            .build()
        runCatching { dispatchGesture(gesture, null, null) }
    }

    private fun dispatchTwoFingerScroll(scrollUp: Boolean) {
        val (cx, cy, density) = screenCenterAndDensity()
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
            .addStroke(GestureDescription.StrokeDescription(p1, 0, 140L))
            .addStroke(GestureDescription.StrokeDescription(p2, 0, 140L))
            .build()
        runCatching { dispatchGesture(gesture, null, null) }
    }

    private fun dispatchMouseSwipeDelta(dxPx: Float, dyPx: Float) {
        val now = System.currentTimeMillis()
        if (now - lastSwipeTimestamp < 42L) return
        val dist = hypot(dxPx, dyPx)
        if (dist < 4f) return
        lastSwipeTimestamp = now

        val (cx, cy, _) = screenCenterAndDensity()
        val clampX = dxPx.coerceIn(-cx * 0.6f, cx * 0.6f)
        val clampY = dyPx.coerceIn(-cy * 0.6f, cy * 0.6f)
        val swipePath = Path().apply {
            moveTo(cx, cy)
            lineTo(cx + clampX, cy + clampY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(swipePath, 0, 32L))
            .build()
        runCatching { dispatchGesture(gesture, null, null) }
    }
}

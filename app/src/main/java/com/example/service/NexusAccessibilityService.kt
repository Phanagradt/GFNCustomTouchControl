package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.input.InputBridge

class NexusAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeLeftStroke: GestureDescription.StrokeDescription? = null
    private var activeRightStrokes: Pair<GestureDescription.StrokeDescription, GestureDescription.StrokeDescription>? = null

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
        })
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        InputBridge.attachPointerSink(null)
        activeLeftStroke = null
        activeRightStrokes = null
        super.onDestroy()
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun screenCenter(): Pair<Float, Float> {
        val dm = resources.displayMetrics
        return (dm.widthPixels * 0.5f) to (dm.heightPixels * 0.5f)
    }

    private fun dispatchLeftButton(isDown: Boolean) {
        val (cx, cy) = screenCenter()
        val path = Path().apply { moveTo(cx, cy) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isDown) {
                val stroke = GestureDescription.StrokeDescription(path, 0, 45L, true)
                activeLeftStroke = stroke
                val gesture = GestureDescription.Builder().addStroke(stroke).build()
                runCatching { dispatchGesture(gesture, null, null) }
            } else {
                val prev = activeLeftStroke
                activeLeftStroke = null
                if (prev != null) {
                    val releaseStroke = prev.continueStroke(path, 0, 20L, false)
                    val gesture = GestureDescription.Builder().addStroke(releaseStroke).build()
                    runCatching { dispatchGesture(gesture, null, null) }
                }
            }
        } else if (isDown) {
            val stroke = GestureDescription.StrokeDescription(path, 0, 55L)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            runCatching { dispatchGesture(gesture, null, null) }
        }
    }

    private fun dispatchRightButton(isDown: Boolean) {
        val (cx, cy) = screenCenter()
        val offset = 46f * resources.displayMetrics.density / 2f
        val leftPath = Path().apply { moveTo(cx - offset, cy) }
        val rightPath = Path().apply { moveTo(cx + offset, cy) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isDown) {
                val s1 = GestureDescription.StrokeDescription(leftPath, 0, 45L, true)
                val s2 = GestureDescription.StrokeDescription(rightPath, 0, 45L, true)
                activeRightStrokes = s1 to s2
                val gesture = GestureDescription.Builder().addStroke(s1).addStroke(s2).build()
                runCatching { dispatchGesture(gesture, null, null) }
            } else {
                val prev = activeRightStrokes
                activeRightStrokes = null
                if (prev != null) {
                    val r1 = prev.first.continueStroke(leftPath, 0, 20L, false)
                    val r2 = prev.second.continueStroke(rightPath, 0, 20L, false)
                    val gesture = GestureDescription.Builder().addStroke(r1).addStroke(r2).build()
                    runCatching { dispatchGesture(gesture, null, null) }
                }
            }
        } else if (isDown) {
            val s1 = GestureDescription.StrokeDescription(leftPath, 0, 60L)
            val s2 = GestureDescription.StrokeDescription(rightPath, 0, 60L)
            val gesture = GestureDescription.Builder().addStroke(s1).addStroke(s2).build()
            runCatching { dispatchGesture(gesture, null, null) }
        }
    }

    private fun dispatchMiddleTap() {
        val (cx, cy) = screenCenter()
        val offset = 40f * resources.displayMetrics.density
        val p1 = Path().apply { moveTo(cx - offset, cy) }
        val p2 = Path().apply { moveTo(cx, cy) }
        val p3 = Path().apply { moveTo(cx + offset, cy) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p1, 0, 60L))
            .addStroke(GestureDescription.StrokeDescription(p2, 0, 60L))
            .addStroke(GestureDescription.StrokeDescription(p3, 0, 60L))
            .build()
        runCatching { dispatchGesture(gesture, null, null) }
    }

    private fun dispatchTwoFingerScroll(scrollUp: Boolean) {
        val (cx, cy) = screenCenter()
        val density = resources.displayMetrics.density
        val offset = 28f * density
        val travel = 88f * density
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
}

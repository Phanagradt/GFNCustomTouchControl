package com.example.service

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.input.InputBridge

class NexusGamepadImeService : InputMethodService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        InputBridge.attachImeSink { event ->
            if (Looper.myLooper() == Looper.getMainLooper()) {
                currentInputConnection?.sendKeyEvent(event)
            } else {
                mainHandler.post {
                    currentInputConnection?.sendKeyEvent(event)
                }
            }
        }
    }

    override fun onEvaluateInputViewShown(): Boolean = false

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val density = resources.displayMetrics.density
        val padH = (16 * density).toInt()
        val padV = (10 * density).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(padH, padV, padH, padV)
            setBackgroundColor(0xFF0B0E14.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val statusText = TextView(this).apply {
            text = "NEXUSPAD IME ACTIVE  •  Dispatching Gamepad & Keyboard Events"
            setTextColor(0xFF8CE000.toInt())
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val switchBtn = TextView(this).apply {
            text = "SWITCH KEYBOARD"
            setTextColor(0xFF00E5FF.toInt())
            textSize = 11f
            setPadding((10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
            setBackgroundColor(0xFF1A2230.toInt())
            setOnClickListener {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showInputMethodPicker()
            }
        }

        container.addView(statusText)
        container.addView(switchBtn)
        return container
    }

    override fun onDestroy() {
        InputBridge.attachImeSink(null)
        super.onDestroy()
    }
}

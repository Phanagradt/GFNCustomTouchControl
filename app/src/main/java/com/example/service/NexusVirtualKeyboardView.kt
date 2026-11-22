package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

@SuppressLint("ViewConstructor")
class NexusVirtualKeyboardView(
    context: Context,
    private val onCommitText: (String) -> Unit,
    private val onBackspace: () -> Unit,
    private val onEnter: () -> Unit,
    private val onSpecialKey: (Int) -> Unit,
    private val onSwitchIme: () -> Unit,
    private val onHideKeyboard: () -> Unit
) : LinearLayout(context) {

    private enum class KeyboardPage { LETTERS, SYMBOLS }
    private enum class ShiftMode { OFF, ONCE, CAPS }

    private var currentPage = KeyboardPage.LETTERS
    private var shiftMode = ShiftMode.ONCE
    private var enterLabel = "ENTER"
    private val keyContainer: LinearLayout
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var isRepeatingBackspace = false

    private val density = resources.displayMetrics.density

    private val backspaceRepeater = object : Runnable {
        override fun run() {
            if (isRepeatingBackspace) {
                onBackspace()
                repeatHandler.postDelayed(this, 65L)
            }
        }
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#0B0E14"))
        val padH = dp(6)
        val padV = dp(6)
        setPadding(padH, padV, padH, dp(10))
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Top tactical bar: Brand + quick TAB / ESC + Switch IME + Hide Keyboard
        addView(buildTopUtilityBar())

        keyContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        addView(keyContainer)
        rebuildRows()
    }

    fun updateEditorActionLabel(label: String, shouldCapitalizeInitially: Boolean) {
        enterLabel = label
        if (currentPage == KeyboardPage.LETTERS && shiftMode != ShiftMode.CAPS) {
            shiftMode = if (shouldCapitalizeInitially) ShiftMode.ONCE else ShiftMode.OFF
        }
        rebuildRows()
    }

    private fun buildTopUtilityBar(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(6))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

            val brand = TextView(context).apply {
                text = "NEXUSPAD KEYBOARD"
                setTextColor(Color.parseColor("#8CE000"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                paint.isFakeBoldText = true
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(brand)

            addView(createUtilityChip("ESC", "#00E5FF") { onSpecialKey(KeyEvent.KEYCODE_ESCAPE) })
            addView(createUtilityChip("TAB", "#00E5FF") { onSpecialKey(KeyEvent.KEYCODE_TAB) })
            addView(createUtilityChip("IME", "#9FB0C7") { onSwitchIme() })
            addView(createUtilityChip("HIDE", "#FFB300") { onHideKeyboard() })
        }
    }

    private fun createUtilityChip(label: String, textHex: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(Color.parseColor(textHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            background = keyDrawable("#151C28", "#2B384E", radiusDp = 8f)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            lp.marginStart = dp(6)
            layoutParams = lp
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
        }
    }

    private fun rebuildRows() {
        keyContainer.removeAllViews()
        if (currentPage == KeyboardPage.LETTERS) {
            val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            val row2 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
            val row3 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
            val row4 = listOf("z", "x", "c", "v", "b", "n", "m")

            keyContainer.addView(buildCharRow(row1, compactHeight = true))
            keyContainer.addView(buildCharRow(row2.map { cased(it) }))
            keyContainer.addView(buildCharRow(row3.map { cased(it) }, sidePadWeight = 0.45f))
            keyContainer.addView(buildShiftRow(row4.map { cased(it) }))
            keyContainer.addView(buildBottomRow(modeToggleLabel = "?123"))
        } else {
            val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            val row2 = listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
            val row3 = listOf("*", "\"", "'", ":", ";", "!", "?", "~", "`")
            val row4 = listOf("=", "\\", "<", ">", "[", "]", "{", "}")

            keyContainer.addView(buildCharRow(row1, compactHeight = true))
            keyContainer.addView(buildCharRow(row2))
            keyContainer.addView(buildCharRow(row3, sidePadWeight = 0.3f))
            keyContainer.addView(buildSymbolDeleteRow(row4))
            keyContainer.addView(buildBottomRow(modeToggleLabel = "ABC"))
        }
    }

    private fun cased(ch: String): String =
        if (shiftMode == ShiftMode.OFF) ch else ch.uppercase()

    private fun buildCharRow(keys: List<String>, sidePadWeight: Float = 0f, compactHeight: Boolean = false): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(5)
            }
            if (sidePadWeight > 0f) {
                addView(android.view.View(context), LayoutParams(0, 1, sidePadWeight))
            }
            keys.forEach { key ->
                addView(
                    createKeyButton(
                        label = key,
                        weight = 1f,
                        heightDp = if (compactHeight) 36 else 44,
                        bgHex = if (compactHeight) "#111722" else "#182130"
                    ) {
                        onCommitText(key)
                        if (currentPage == KeyboardPage.LETTERS && shiftMode == ShiftMode.ONCE) {
                            shiftMode = ShiftMode.OFF
                            rebuildRows()
                        }
                    }
                )
            }
            if (sidePadWeight > 0f) {
                addView(android.view.View(context), LayoutParams(0, 1, sidePadWeight))
            }
        }
    }

    private fun buildShiftRow(keys: List<String>): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(5)
            }

            val shiftLabel = when (shiftMode) {
                ShiftMode.OFF -> "⇧"
                ShiftMode.ONCE -> "⬆"
                ShiftMode.CAPS -> "⇪"
            }
            val shiftBg = if (shiftMode == ShiftMode.OFF) "#131A26" else "#233616"
            val shiftStroke = if (shiftMode == ShiftMode.OFF) "#2B384E" else "#8CE000"
            val shiftText = if (shiftMode == ShiftMode.OFF) "#D8E4F2" else "#8CE000"

            addView(
                createKeyButton(
                    label = shiftLabel,
                    weight = 1.45f,
                    bgHex = shiftBg,
                    strokeHex = shiftStroke,
                    textHex = shiftText
                ) {
                    shiftMode = when (shiftMode) {
                        ShiftMode.OFF -> ShiftMode.ONCE
                        ShiftMode.ONCE -> ShiftMode.CAPS
                        ShiftMode.CAPS -> ShiftMode.OFF
                    }
                    rebuildRows()
                }
            )

            keys.forEach { key ->
                addView(createKeyButton(label = key, weight = 1f) {
                    onCommitText(key)
                    if (shiftMode == ShiftMode.ONCE) {
                        shiftMode = ShiftMode.OFF
                        rebuildRows()
                    }
                })
            }

            addView(createBackspaceButton(weight = 1.45f))
        }
    }

    private fun buildSymbolDeleteRow(keys: List<String>): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(5)
            }
            keys.forEach { key ->
                addView(createKeyButton(label = key, weight = 1f) { onCommitText(key) })
            }
            addView(createBackspaceButton(weight = 1.5f))
        }
    }

    private fun buildBottomRow(modeToggleLabel: String): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

            addView(
                createKeyButton(
                    label = modeToggleLabel,
                    weight = 1.4f,
                    bgHex = "#131A26",
                    textHex = "#00E5FF"
                ) {
                    currentPage = if (currentPage == KeyboardPage.LETTERS) {
                        KeyboardPage.SYMBOLS
                    } else {
                        KeyboardPage.LETTERS
                    }
                    rebuildRows()
                }
            )

            addView(createKeyButton(label = ",", weight = 0.9f, bgHex = "#131A26") { onCommitText(",") })
            addView(createKeyButton(label = "SPACE", weight = 4.2f, bgHex = "#1B2536", textHex = "#C8D6E8") {
                onCommitText(" ")
            })
            addView(createKeyButton(label = ".", weight = 0.9f, bgHex = "#131A26") { onCommitText(".") })
            addView(
                createKeyButton(
                    label = enterLabel,
                    weight = 1.7f,
                    bgHex = "#243A10",
                    strokeHex = "#8CE000",
                    textHex = "#8CE000"
                ) {
                    onEnter()
                }
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createBackspaceButton(weight: Float): TextView {
        return TextView(context).apply {
            text = "⌫"
            setTextColor(Color.parseColor("#FF5277"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            background = keyDrawable("#1E1520", "#4A2735")
            layoutParams = LayoutParams(0, dp(44), weight).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            }
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onBackspace()
                        isRepeatingBackspace = true
                        repeatHandler.removeCallbacks(backspaceRepeater)
                        repeatHandler.postDelayed(backspaceRepeater, 340L)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        isRepeatingBackspace = false
                        repeatHandler.removeCallbacks(backspaceRepeater)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun createKeyButton(
        label: String,
        weight: Float,
        heightDp: Int = 44,
        bgHex: String = "#182130",
        strokeHex: String = "#2A364B",
        textHex: String = "#F0F4FA",
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(Color.parseColor(textHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (label.length > 2) 12f else 16f)
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            background = keyDrawable(bgHex, strokeHex)
            layoutParams = LayoutParams(0, dp(heightDp), weight).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            }
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
        }
    }

    private fun keyDrawable(bgHex: String, strokeHex: String, radiusDp: Float = 9f): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * density
            setColor(Color.parseColor(bgHex))
            setStroke((1f * density).toInt().coerceAtLeast(1), Color.parseColor(strokeHex))
        }
    }

    private fun dp(value: Int): Int = (value * density).toInt()
}

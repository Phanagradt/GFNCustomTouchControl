package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

@SuppressLint("ViewConstructor")
class NexusVirtualKeyboardView(
    context: Context,
    private val initialLanguage: KeyboardLanguage = KeyboardLanguage.ENGLISH,
    private val isFloatingMode: Boolean = false,
    private val onCommitText: (String) -> Unit,
    private val onBackspace: () -> Unit,
    private val onEnter: () -> Unit,
    private val onSpecialKey: (Int) -> Unit,
    private val onSwitchIme: () -> Unit,
    private val onHideKeyboard: () -> Unit,
    private val onToggleFloating: (() -> Unit)? = null,
    private val onReplaceWord: (deleteCount: Int, replacement: String) -> Unit = { deleteCount, replacement ->
        repeat(deleteCount) { onBackspace() }
        onCommitText(replacement)
    }
) : LinearLayout(context) {

    enum class KeyboardLanguage(val badge: String) {
        ENGLISH("EN"),
        THAI("ไทย")
    }

    private enum class KeyboardPage { LETTERS, SYMBOLS }
    private enum class ShiftMode { OFF, ONCE, CAPS }

    private var currentLanguage = initialLanguage
    private var currentPage = KeyboardPage.LETTERS
    private var shiftMode = ShiftMode.ONCE
    private var enterLabel = "ENTER"
    private var autoCorrectEnabled = true
    private val composingWord = StringBuilder()
    private var lastCommittedWord = ""
    private var activeAutoCorrect: String? = null

    private lateinit var langChipView: TextView
    private lateinit var autoChipView: TextView
    private val suggestionContainer: LinearLayout
    private val keyContainer: LinearLayout
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var isRepeatingBackspace = false

    private val density = resources.displayMetrics.density

    private var keyPreviewPopup: PopupWindow? = null
    private var keyPreviewTextView: TextView? = null

    private val backspaceRepeater = object : Runnable {
        override fun run() {
            if (isRepeatingBackspace) {
                handleBackspaceInternal()
                repeatHandler.postDelayed(this, 65L)
            }
        }
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#0B0E14"))
        val padH = dp(6)
        val padV = dp(6)
        val bottomPad = if (isFloatingMode) dp(8) else dp(18)
        setPadding(padH, padV, padH, bottomPad)
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Top tactical bar: Brand + AUTO + Language + FLOAT + ESC + TAB + IME + HIDE
        addView(buildTopUtilityBar())

        // Gboard-style Suggestion & Auto-Correct Strip
        suggestionContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(6))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(34))
        }
        addView(suggestionContainer)

        keyContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        addView(keyContainer)
        rebuildRows()
        refreshSuggestions()
    }

    override fun onDetachedFromWindow() {
        dismissKeyPreview()
        isRepeatingBackspace = false
        repeatHandler.removeCallbacks(backspaceRepeater)
        super.onDetachedFromWindow()
    }

    fun setLanguage(language: KeyboardLanguage) {
        currentLanguage = language
        composingWord.setLength(0)
        lastCommittedWord = ""
        if (::langChipView.isInitialized) {
            langChipView.text = "🌐 ${currentLanguage.badge}"
        }
        rebuildRows()
        refreshSuggestions()
    }

    fun updateEditorActionLabel(label: String, shouldCapitalizeInitially: Boolean) {
        enterLabel = label
        composingWord.setLength(0)
        lastCommittedWord = ""
        if (currentPage == KeyboardPage.LETTERS && currentLanguage == KeyboardLanguage.ENGLISH && shiftMode != ShiftMode.CAPS) {
            shiftMode = if (shouldCapitalizeInitially) ShiftMode.ONCE else ShiftMode.OFF
        } else if (currentLanguage == KeyboardLanguage.THAI && shiftMode == ShiftMode.ONCE) {
            shiftMode = ShiftMode.OFF
        }
        rebuildRows()
        refreshSuggestions()
    }

    private fun buildTopUtilityBar(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(4))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

            val brand = TextView(context).apply {
                text = if (isFloatingMode) "FLOATING KB" else "NEXUSPAD KB"
                setTextColor(Color.parseColor("#8CE000"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                paint.isFakeBoldText = true
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(brand)

            autoChipView = createUtilityChip(
                label = "AUTO",
                textHex = "#8CE000",
                bgHex = "#1B2E12",
                strokeHex = "#8CE000"
            ) {
                autoCorrectEnabled = !autoCorrectEnabled
                updateAutoChipStyle()
                refreshSuggestions()
            }
            addView(autoChipView)

            langChipView = createUtilityChip("🌐 ${currentLanguage.badge}", "#8CE000") {
                currentLanguage = if (currentLanguage == KeyboardLanguage.ENGLISH) {
                    KeyboardLanguage.THAI
                } else {
                    KeyboardLanguage.ENGLISH
                }
                if (currentLanguage == KeyboardLanguage.THAI && shiftMode == ShiftMode.ONCE) {
                    shiftMode = ShiftMode.OFF
                }
                composingWord.setLength(0)
                lastCommittedWord = ""
                langChipView.text = "🌐 ${currentLanguage.badge}"
                currentPage = KeyboardPage.LETTERS
                rebuildRows()
                refreshSuggestions()
            }
            addView(langChipView)

            if (onToggleFloating != null) {
                addView(
                    createUtilityChip(
                        label = if (isFloatingMode) "DOCK" else "FLOAT",
                        textHex = "#00E5FF",
                        bgHex = "#0E2533",
                        strokeHex = "#00E5FF"
                    ) { onToggleFloating.invoke() }
                )
            }

            addView(createUtilityChip("ESC", "#00E5FF") {
                composingWord.setLength(0)
                onSpecialKey(KeyEvent.KEYCODE_ESCAPE)
                refreshSuggestions()
            })
            addView(createUtilityChip("TAB", "#00E5FF") {
                composingWord.setLength(0)
                onSpecialKey(KeyEvent.KEYCODE_TAB)
                refreshSuggestions()
            })
            if (!isFloatingMode) {
                addView(createUtilityChip("IME", "#9FB0C7") { onSwitchIme() })
            }
            addView(createUtilityChip(if (isFloatingMode) "✕" else "HIDE", "#FFB300") {
                dismissKeyPreview()
                onHideKeyboard()
            })
        }
    }

    private fun updateAutoChipStyle() {
        if (!::autoChipView.isInitialized) return
        if (autoCorrectEnabled) {
            autoChipView.text = "AUTO"
            autoChipView.setTextColor(Color.parseColor("#8CE000"))
            autoChipView.background = keyDrawable("#1B2E12", "#8CE000", radiusDp = 8f)
        } else {
            autoChipView.text = "RAW"
            autoChipView.setTextColor(Color.parseColor("#9FB0C7"))
            autoChipView.background = keyDrawable("#151C28", "#2B384E", radiusDp = 8f)
        }
    }

    private fun refreshSuggestions() {
        suggestionContainer.removeAllViews()
        val isThai = currentLanguage == KeyboardLanguage.THAI
        val result = KeyboardWordPredictor.predict(
            currentWord = composingWord.toString(),
            previousWord = lastCommittedWord,
            isThai = isThai
        )
        activeAutoCorrect = if (autoCorrectEnabled) result.autoCorrect else null

        val pills = result.suggestions.ifEmpty {
            if (isThai) listOf("สวัสดี", "ขอบคุณ", "ครับ") else listOf("I", "Hey", "Ready")
        }

        pills.forEachIndexed { idx, word ->
            val isHighlightedAuto = idx == 0 && activeAutoCorrect != null && word == activeAutoCorrect
            val pill = TextView(context).apply {
                text = if (isHighlightedAuto) "✓ $word" else word
                setTextColor(
                    Color.parseColor(
                        if (isHighlightedAuto) "#8CE000" else "#E2ECF8"
                    )
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                paint.isFakeBoldText = isHighlightedAuto
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                background = keyDrawable(
                    bgHex = if (isHighlightedAuto) "#1E3312" else "#141C2A",
                    strokeHex = if (isHighlightedAuto) "#8CE000" else "#263348",
                    radiusDp = 10f
                )
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
                setOnClickListener {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    animateTapPulse(this)
                    applySuggestionSelection(word)
                }
            }
            suggestionContainer.addView(pill)
        }
    }

    private fun applySuggestionSelection(word: String) {
        val addSpace = currentLanguage == KeyboardLanguage.ENGLISH
        val suffix = if (addSpace) " " else ""
        val deleteCount = composingWord.length
        if (deleteCount > 0) {
            onReplaceWord(deleteCount, word + suffix)
        } else {
            onCommitText(word + suffix)
        }
        lastCommittedWord = word
        composingWord.setLength(0)
        if (currentLanguage == KeyboardLanguage.ENGLISH && shiftMode == ShiftMode.ONCE) {
            shiftMode = ShiftMode.OFF
            rebuildRows()
        }
        refreshSuggestions()
    }

    private fun handleCharacterCommit(key: String) {
        onCommitText(key)
        val ch = key.firstOrNull()
        if (ch != null && (ch.isLetter() || ch == '\'' || isThaiCombiner(ch))) {
            composingWord.append(key)
        } else {
            if (composingWord.isNotEmpty()) {
                lastCommittedWord = composingWord.toString()
                composingWord.setLength(0)
            }
        }
        if (currentPage == KeyboardPage.LETTERS && shiftMode == ShiftMode.ONCE) {
            shiftMode = ShiftMode.OFF
            rebuildRows()
        }
        refreshSuggestions()
    }

    private fun handleSpaceCommit() {
        val correction = activeAutoCorrect
        val current = composingWord.toString()
        if (autoCorrectEnabled &&
            !correction.isNullOrBlank() &&
            current.isNotEmpty() &&
            !correction.equals(current, ignoreCase = false)
        ) {
            onReplaceWord(current.length, "$correction ")
            lastCommittedWord = correction
        } else {
            onCommitText(" ")
            if (current.isNotEmpty()) {
                lastCommittedWord = current
            }
        }
        composingWord.setLength(0)
        refreshSuggestions()
    }

    private fun handleBackspaceInternal() {
        onBackspace()
        if (composingWord.isNotEmpty()) {
            composingWord.deleteCharAt(composingWord.length - 1)
        }
        refreshSuggestions()
    }

    private fun isThaiCombiner(c: Char): Boolean {
        val code = c.code
        return code in 0x0E00..0x0E7F
    }

    private fun createUtilityChip(
        label: String,
        textHex: String,
        bgHex: String = "#151C28",
        strokeHex: String = "#2B384E",
        onClick: () -> Unit
    ): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(Color.parseColor(textHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            background = keyDrawable(bgHex, strokeHex, radiusDp = 8f)
            setPadding(dp(7), dp(5), dp(7), dp(5))
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            lp.marginStart = dp(4)
            layoutParams = lp
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                animateTapPulse(this)
                onClick()
            }
        }
    }

    private fun animateTapPulse(view: View) {
        view.animate().cancel()
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(45L)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(85L)
                    .start()
            }
            .start()
    }

    private fun showKeyPreview(anchor: View, label: String) {
        if (label.isBlank() || label.length > 2) return
        if (!anchor.isAttachedToWindow) return

        val previewView = keyPreviewTextView ?: TextView(context).apply {
            setTextColor(Color.parseColor("#8CE000"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            background = keyDrawable("#162336", "#8CE000", radiusDp = 12f, strokeWidthDp = 1.6f)
            elevation = 12f * density
            keyPreviewTextView = this
        }
        previewView.text = label

        val popupW = dp(48)
        val popupH = dp(52)
        val popup = keyPreviewPopup ?: PopupWindow(previewView, popupW, popupH, false).apply {
            isTouchable = false
            isClippingEnabled = false
            keyPreviewPopup = this
        }

        val xOff = (anchor.width - popupW) / 2
        val yOff = -(anchor.height + popupH + dp(6))
        runCatching {
            if (popup.isShowing) {
                popup.update(anchor, xOff, yOff, popupW, popupH)
            } else {
                popup.showAsDropDown(anchor, xOff, yOff)
            }
        }
    }

    private fun dismissKeyPreview() {
        runCatching {
            if (keyPreviewPopup?.isShowing == true) {
                keyPreviewPopup?.dismiss()
            }
        }
    }

    private fun rebuildRows() {
        dismissKeyPreview()
        keyContainer.removeAllViews()
        if (currentPage == KeyboardPage.SYMBOLS) {
            val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            val row2 = listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
            val row3 = listOf("*", "\"", "'", ":", ";", "!", "?", "~", "`")
            val row4 = listOf("=", "\\", "<", ">", "[", "]", "{", "}")

            keyContainer.addView(buildCharRow(row1, compactHeight = true))
            keyContainer.addView(buildCharRow(row2))
            keyContainer.addView(buildCharRow(row3, sidePadWeight = 0.3f))
            keyContainer.addView(buildSymbolDeleteRow(row4))
            keyContainer.addView(buildBottomRow(modeToggleLabel = currentLanguage.badge))
            return
        }

        if (currentLanguage == KeyboardLanguage.THAI) {
            rebuildThaiKedmaneeRows()
        } else {
            val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            val row2 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
            val row3 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
            val row4 = listOf("z", "x", "c", "v", "b", "n", "m")

            keyContainer.addView(buildCharRow(row1, compactHeight = true))
            keyContainer.addView(buildCharRow(row2.map { cased(it) }))
            keyContainer.addView(buildCharRow(row3.map { cased(it) }, sidePadWeight = 0.45f))
            keyContainer.addView(buildShiftRow(row4.map { cased(it) }))
            keyContainer.addView(buildBottomRow(modeToggleLabel = "?123"))
        }
    }

    private fun rebuildThaiKedmaneeRows() {
        val shifted = shiftMode != ShiftMode.OFF
        val row1 = if (!shifted) {
            listOf("ๅ", "/", "_", "ภ", "ถ", "ุ", "ึ", "ค", "ต", "จ", "ข", "ช")
        } else {
            listOf("+", "๑", "๒", "๓", "๔", "ู", "฿", "๕", "๖", "๗", "๘", "๙")
        }
        val row2 = if (!shifted) {
            listOf("ๆ", "ไ", "ำ", "พ", "ะ", "ั", "ี", "ร", "น", "ย", "บ", "ล")
        } else {
            listOf("๐", "\"", "ฎ", "ฑ", "ธ", "ํ", "๊", "ณ", "ฯ", "ญ", "ฐ", ",")
        }
        val row3 = if (!shifted) {
            listOf("ฟ", "ห", "ก", "ด", "เ", "้", "่", "า", "ส", "ว", "ง")
        } else {
            listOf("ฤ", "ฆ", "ฏ", "โ", "ฌ", "็", "๋", "ษ", "ศ", "ซ", ".")
        }
        val row4 = if (!shifted) {
            listOf("ผ", "ป", "แ", "อ", "ิ", "ื", "ท", "ม", "ใ", "ฝ")
        } else {
            listOf("(", ")", "ฉ", "ฮ", "ฺ", "์", "?", "ฒ", "ฬ", "ฦ")
        }

        keyContainer.addView(buildCharRow(row1, compactHeight = true, smallText = true))
        keyContainer.addView(buildCharRow(row2, smallText = true))
        keyContainer.addView(buildCharRow(row3, sidePadWeight = 0.2f, smallText = true))
        keyContainer.addView(buildShiftRow(row4, smallText = true))
        keyContainer.addView(buildBottomRow(modeToggleLabel = "?123"))
    }

    private fun cased(ch: String): String =
        if (shiftMode == ShiftMode.OFF) ch else ch.uppercase()

    private fun buildCharRow(
        keys: List<String>,
        sidePadWeight: Float = 0f,
        compactHeight: Boolean = false,
        smallText: Boolean = false
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(4)
            }
            if (sidePadWeight > 0f) {
                addView(View(context), LayoutParams(0, 1, sidePadWeight))
            }
            keys.forEach { key ->
                addView(
                    createKeyButton(
                        label = key,
                        weight = 1f,
                        heightDp = if (compactHeight) 35 else 42,
                        bgHex = if (compactHeight) "#111722" else "#182130",
                        forceSmallText = smallText,
                        showPopupPreview = true
                    ) {
                        handleCharacterCommit(key)
                    }
                )
            }
            if (sidePadWeight > 0f) {
                addView(View(context), LayoutParams(0, 1, sidePadWeight))
            }
        }
    }

    private fun buildShiftRow(keys: List<String>, smallText: Boolean = false): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(4)
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
                    weight = 1.35f,
                    heightDp = 42,
                    bgHex = shiftBg,
                    strokeHex = shiftStroke,
                    textHex = shiftText,
                    showPopupPreview = false
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
                addView(
                    createKeyButton(
                        label = key,
                        weight = 1f,
                        heightDp = 42,
                        forceSmallText = smallText,
                        showPopupPreview = true
                    ) {
                        handleCharacterCommit(key)
                    }
                )
            }

            addView(createBackspaceButton(weight = 1.35f, heightDp = 42))
        }
    }

    private fun buildSymbolDeleteRow(keys: List<String>): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(4)
            }
            keys.forEach { key ->
                addView(
                    createKeyButton(
                        label = key,
                        weight = 1f,
                        heightDp = 42,
                        showPopupPreview = true
                    ) { handleCharacterCommit(key) }
                )
            }
            addView(createBackspaceButton(weight = 1.5f, heightDp = 42))
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
                    weight = 1.35f,
                    heightDp = 42,
                    bgHex = "#131A26",
                    textHex = "#00E5FF",
                    showPopupPreview = false
                ) {
                    currentPage = if (currentPage == KeyboardPage.LETTERS) {
                        KeyboardPage.SYMBOLS
                    } else {
                        KeyboardPage.LETTERS
                    }
                    rebuildRows()
                }
            )

            addView(
                createKeyButton(
                    label = ",",
                    weight = 0.85f,
                    heightDp = 42,
                    bgHex = "#131A26",
                    showPopupPreview = true
                ) { handleCharacterCommit(",") }
            )
            addView(
                createKeyButton(
                    label = if (currentLanguage == KeyboardLanguage.THAI) "วรรค (SPACE)" else "SPACE",
                    weight = 4.0f,
                    heightDp = 42,
                    bgHex = "#1B2536",
                    textHex = "#C8D6E8",
                    forceSmallText = true,
                    showPopupPreview = false
                ) {
                    handleSpaceCommit()
                }
            )
            addView(
                createKeyButton(
                    label = ".",
                    weight = 0.85f,
                    heightDp = 42,
                    bgHex = "#131A26",
                    showPopupPreview = true
                ) { handleCharacterCommit(".") }
            )
            addView(
                createKeyButton(
                    label = enterLabel,
                    weight = 1.7f,
                    heightDp = 42,
                    bgHex = "#243A10",
                    strokeHex = "#8CE000",
                    textHex = "#8CE000",
                    showPopupPreview = false
                ) {
                    composingWord.setLength(0)
                    lastCommittedWord = ""
                    onEnter()
                    refreshSuggestions()
                }
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createBackspaceButton(weight: Float, heightDp: Int = 44): TextView {
        val defaultBg = keyDrawable("#1E1520", "#4A2735")
        val pressedBg = keyDrawable("#3D1827", "#FF5277", strokeWidthDp = 1.5f)
        return TextView(context).apply {
            text = "⌫"
            setTextColor(Color.parseColor("#FF5277"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            background = defaultBg
            layoutParams = LayoutParams(0, dp(heightDp), weight).apply {
                marginStart = dp(1)
                marginEnd = dp(1)
            }
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        v.background = pressedBg
                        v.animate().cancel()
                        v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(45L).start()
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        handleBackspaceInternal()
                        isRepeatingBackspace = true
                        repeatHandler.removeCallbacks(backspaceRepeater)
                        repeatHandler.postDelayed(backspaceRepeater, 340L)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        v.background = defaultBg
                        v.animate().cancel()
                        v.animate().scaleX(1f).scaleY(1f).setDuration(90L).start()
                        isRepeatingBackspace = false
                        repeatHandler.removeCallbacks(backspaceRepeater)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createKeyButton(
        label: String,
        weight: Float,
        heightDp: Int = 44,
        bgHex: String = "#182130",
        strokeHex: String = "#2A364B",
        textHex: String = "#F0F4FA",
        forceSmallText: Boolean = false,
        showPopupPreview: Boolean = false,
        onClick: () -> Unit
    ): TextView {
        val normalBg = keyDrawable(bgHex, strokeHex)
        val pressedBg = keyDrawable("#25391B", "#8CE000", strokeWidthDp = 1.5f)
        val normalTextColor = Color.parseColor(textHex)
        val pressedTextColor = Color.parseColor("#8CE000")

        return TextView(context).apply {
            text = label
            setTextColor(normalTextColor)
            val textSp = when {
                label.length > 3 -> 11f
                label.length > 1 -> 12.5f
                forceSmallText -> 13.5f
                else -> 16f
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp)
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            background = normalBg
            layoutParams = LayoutParams(0, dp(heightDp), weight).apply {
                marginStart = dp(1)
                marginEnd = dp(1)
            }
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        v.background = pressedBg
                        setTextColor(pressedTextColor)
                        v.animate().cancel()
                        v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(42L).start()
                        if (showPopupPreview) {
                            showKeyPreview(v, label)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        v.isPressed = false
                        v.background = normalBg
                        setTextColor(normalTextColor)
                        v.animate().cancel()
                        v.animate().scaleX(1f).scaleY(1f).setDuration(95L).start()
                        dismissKeyPreview()
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onClick()
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        v.background = normalBg
                        setTextColor(normalTextColor)
                        v.animate().cancel()
                        v.animate().scaleX(1f).scaleY(1f).setDuration(95L).start()
                        dismissKeyPreview()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun keyDrawable(
        bgHex: String,
        strokeHex: String,
        radiusDp: Float = 8f,
        strokeWidthDp: Float = 1f
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * density
            setColor(Color.parseColor(bgHex))
            setStroke((strokeWidthDp * density).toInt().coerceAtLeast(1), Color.parseColor(strokeHex))
        }
    }

    private fun dp(value: Int): Int = (value * density).toInt()
}

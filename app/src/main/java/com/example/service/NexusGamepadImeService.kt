package com.example.service

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.example.input.InputBridge
import java.util.Locale

class NexusGamepadImeService : InputMethodService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val keyCharMap by lazy { KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD) }
    private var keyboardView: NexusVirtualKeyboardView? = null

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
        InputBridge.attachKeyboardTextSink(object : InputBridge.KeyboardTextSink {
            override fun commitText(text: String) {
                mainHandler.post { handleCommitText(text) }
            }

            override fun backspace() {
                mainHandler.post { handleBackspace() }
            }

            override fun enter() {
                mainHandler.post { handleEnter() }
            }
        })
        InputBridge.attachKeyboardController {
            mainHandler.post {
                @Suppress("DEPRECATION")
                requestShowSelf(InputMethodManager.SHOW_FORCED)
            }
        }
    }

    override fun onEvaluateInputViewShown(): Boolean = super.onEvaluateInputViewShown()

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val defaultLang = if (Locale.getDefault().language.equals("th", ignoreCase = true)) {
            NexusVirtualKeyboardView.KeyboardLanguage.THAI
        } else {
            NexusVirtualKeyboardView.KeyboardLanguage.ENGLISH
        }
        val view = NexusVirtualKeyboardView(
            context = this,
            initialLanguage = defaultLang,
            isFloatingMode = false,
            onCommitText = ::handleCommitText,
            onBackspace = ::handleBackspace,
            onEnter = ::handleEnter,
            onSpecialKey = ::handleSpecialKey,
            onSwitchIme = {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showInputMethodPicker()
            },
            onHideKeyboard = {
                requestHideSelf(0)
            },
            onToggleFloating = {
                requestHideSelf(0)
                if (!InputBridge.requestFloatingKeyboard()) {
                    OverlayControllerService.startFloatingKeyboard(this)
                }
            },
            onReplaceWord = ::handleReplaceWord
        )
        keyboardView = view
        syncEditorMeta(currentInputEditorInfo)
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        syncEditorMeta(info)
    }

    private fun syncEditorMeta(info: EditorInfo?) {
        val imeAction = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val label = when (imeAction) {
            EditorInfo.IME_ACTION_SEARCH -> "SEARCH"
            EditorInfo.IME_ACTION_SEND -> "SEND"
            EditorInfo.IME_ACTION_GO -> "GO"
            EditorInfo.IME_ACTION_NEXT -> "NEXT"
            EditorInfo.IME_ACTION_DONE -> "DONE"
            else -> "ENTER"
        }
        val inputType = info?.inputType ?: InputType.TYPE_NULL
        val capSentences = (inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0 ||
            (inputType and InputType.TYPE_TEXT_FLAG_CAP_WORDS) != 0
        keyboardView?.updateEditorActionLabel(label, shouldCapitalizeInitially = capSentences)
    }

    private fun handleCommitText(text: String) {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val isRawTarget = info == null || info.inputType == InputType.TYPE_NULL
        if (isRawTarget && text.length == 1 && text[0].code in 32..126) {
            val events = keyCharMap.getEvents(text.toCharArray())
            if (events != null && events.isNotEmpty()) {
                events.forEach { ev ->
                    InputBridge.dispatchRawKey(ev.keyCode, ev.action, ev.metaState)
                }
                return
            }
        }
        ic.commitText(text, 1)
    }

    private fun handleReplaceWord(deleteCount: Int, replacement: String) {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val isRawTarget = info == null || info.inputType == InputType.TYPE_NULL
        if (isRawTarget) {
            repeat(deleteCount) {
                InputBridge.dispatchRawKey(KeyEvent.KEYCODE_DEL, KeyEvent.ACTION_DOWN)
                InputBridge.dispatchRawKey(KeyEvent.KEYCODE_DEL, KeyEvent.ACTION_UP)
            }
            val events = keyCharMap.getEvents(replacement.toCharArray())
            if (events != null && events.isNotEmpty()) {
                events.forEach { ev ->
                    InputBridge.dispatchRawKey(ev.keyCode, ev.action, ev.metaState)
                }
            } else {
                ic.commitText(replacement, 1)
            }
        } else {
            ic.beginBatchEdit()
            if (deleteCount > 0) {
                ic.deleteSurroundingText(deleteCount, 0)
            }
            ic.commitText(replacement, 1)
            ic.endBatchEdit()
        }
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val isRawTarget = info == null || info.inputType == InputType.TYPE_NULL
        if (isRawTarget) {
            InputBridge.dispatchRawKey(KeyEvent.KEYCODE_DEL, KeyEvent.ACTION_DOWN)
            InputBridge.dispatchRawKey(KeyEvent.KEYCODE_DEL, KeyEvent.ACTION_UP)
        } else {
            val selected = ic.getSelectedText(0)
            if (selected.isNullOrEmpty()) {
                ic.deleteSurroundingText(1, 0)
            } else {
                ic.commitText("", 1)
            }
        }
    }

    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val actionId = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val isMultiLine = ((info?.inputType ?: 0) and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        if (!isMultiLine && actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
            if (ic.performEditorAction(actionId)) return
        }
        val isRawTarget = info == null || info.inputType == InputType.TYPE_NULL
        if (isRawTarget) {
            InputBridge.dispatchRawKey(KeyEvent.KEYCODE_ENTER, KeyEvent.ACTION_DOWN)
            InputBridge.dispatchRawKey(KeyEvent.KEYCODE_ENTER, KeyEvent.ACTION_UP)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    private fun handleSpecialKey(keyCode: Int) {
        InputBridge.dispatchRawKey(keyCode, KeyEvent.ACTION_DOWN)
        InputBridge.dispatchRawKey(keyCode, KeyEvent.ACTION_UP)
    }

    override fun onDestroy() {
        InputBridge.attachImeSink(null)
        InputBridge.attachKeyboardTextSink(null)
        InputBridge.attachKeyboardController(null)
        keyboardView = null
        super.onDestroy()
    }
}

package com.thamnis.thkb.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.SystemClock
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodSubtype
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.thamnis.thkb.data.AssetRepository
import com.thamnis.thkb.data.ClipboardHistoryManager
import com.thamnis.thkb.data.TodoListManager
import com.thamnis.thkb.settings.KbPreferences
import com.thamnis.thkb.settings.SettingsActivity
import com.thamnis.thkb.ui.KeyboardRoot

class ThKbInputMethodService :
    InputMethodService(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var preferences: KbPreferences
    private lateinit var assets: AssetRepository
    private lateinit var clipboardHistory: ClipboardHistoryManager
    private lateinit var todoManager: TodoListManager
    private var audioManager: AudioManager? = null
    private var inputComposeView: ComposeView? = null

    private var returnAction by mutableStateOf(ReturnAction.DEFAULT)
    private var inputContext by mutableStateOf(InputContext.NORMAL)
    private var isRawInput by mutableStateOf(false)
    private var activeEditorAction by mutableStateOf(EditorInfo.IME_ACTION_NONE)
    private var subtypeLayoutOverride by mutableStateOf<String?>(null)
    private var inputSessionKey by mutableStateOf(0)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        preferences = KbPreferences(this)
        assets = AssetRepository(this)
        clipboardHistory = ClipboardHistoryManager(this)
        todoManager = TodoListManager(this)
        audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
    }

    override fun onCreateInputView(): View {
        window?.window?.let { win ->
            WindowCompat.setDecorFitsSystemWindows(win, false)
            win.setBackgroundDrawableResource(android.R.color.transparent)
            @Suppress("DEPRECATION")
            win.navigationBarColor = 0xFF1B1C1E.toInt()
            win.decorView.setViewTreeLifecycleOwner(this)
            win.decorView.setViewTreeSavedStateRegistryOwner(this)
            win.decorView.setViewTreeViewModelStoreOwner(this)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        return ComposeView(this).apply {
            inputComposeView = this
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@ThKbInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@ThKbInputMethodService)
            setViewTreeViewModelStoreOwner(this@ThKbInputMethodService)

            setContent {
                val settings by preferences.settings.collectAsState()
                val clipboardEntries by clipboardHistory.entries.collectAsState()
                val todoTasks by todoManager.tasks.collectAsState()
                val effectiveLayoutId = subtypeLayoutOverride ?: settings.layoutId

                KeyboardRoot(
                    inputSessionKey = inputSessionKey,
                    layoutId = effectiveLayoutId,
                    inputContext = inputContext,
                    autoCapOnOpen = KeyboardStateLogic.shouldAutoCapOnOpen(
                        context = inputContext,
                        autoCapPref = settings.autoCapOnOpen,
                    ),
                    showKeyPreview = settings.showKeyPreview,
                    longPressDelayMs = settings.longPressDelayMs,
                    rapidDeleteDelayMs = settings.rapidDeleteDelayMs,
                    deleteGesturesEnabled = settings.deleteGesturesEnabled,
                    resetForwardDeleteOnChar = settings.resetForwardDeleteOnChar,
                    emojiGridColumns = settings.emojiGridColumns,
                    returnAction = returnAction,
                    assets = assets,
                    clipboardEntries = clipboardEntries,
                    todoTasks = todoTasks,
                    favoriteItems = settings.favoriteItems,
                    recentEmojis = settings.recentEmojis,
                    onToggleFavorite = { item ->
                        preferences.toggleFavoriteItem(item)
                    },
                    onRecordRecentEmoji = { emoji ->
                        preferences.recordRecentEmoji(emoji)
                    },
                    onPinClip = { text ->
                        clipboardHistory.togglePin(text)
                    },
                    onDeleteClip = { text ->
                        clipboardHistory.remove(text)
                    },
                    onClearUnpinnedClips = {
                        clipboardHistory.clearUnpinned()
                    },
                    onAddTodoTask = { title ->
                        todoManager.addTask(title)
                    },
                    onToggleTodoTask = { id ->
                        todoManager.toggleTask(id)
                    },
                    onDeleteTodoTask = { id ->
                        todoManager.deleteTask(id)
                    },
                    onClearCompletedTodoTasks = {
                        todoManager.clearCompleted()
                    },
                    onKeyFeedback = {
                        playFeedback(
                            haptics = settings.hapticsEnabled,
                            sound = settings.soundEnabled,
                        )
                    },
                    onCommitText = { text ->
                        commitTextSafe(text)
                    },
                    onBackspace = {
                        performBackspace()
                    },
                    onForwardDelete = {
                        performForwardDelete()
                    },
                    onDeleteWord = { forward ->
                        performDeleteWord(forward)
                    },
                    onDeleteParagraph = { forward ->
                        performDeleteParagraph(forward)
                    },
                    onSendKeyEvent = { keyCode ->
                        sendDownUpKeyEvents(keyCode)
                    },
                    onSendCtrlKey = { char ->
                        sendCtrlKeyCombination(char)
                    },
                    onReturn = {
                        performReturnAction()
                    },
                    onPaste = {
                        currentInputConnection?.performContextMenuAction(android.R.id.paste)
                    },
                    onMoveCursor = { deltaSteps ->
                        moveCursor(deltaSteps)
                    },
                    onMoveCursorVertical = { deltaLines ->
                        moveCursorVertical(deltaLines)
                    },
                    shouldAutoCapNow = {
                        shouldAutoCapAtCurrentCursor(settings.autoCapOnOpen)
                    },
                    onOpenSettings = {
                        val intent = Intent(this@ThKbInputMethodService, SettingsActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    },
                )
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!restarting) {
            inputSessionKey++
        }
        val rawInputType = info?.inputType ?: 0
        inputContext = KeyboardStateLogic.resolveInputContext(rawInputType, info?.packageName)
        isRawInput = rawInputType == InputType.TYPE_NULL
        val imeOptions = info?.imeOptions ?: 0
        returnAction = KeyboardStateLogic.resolveReturnAction(imeOptions)
        activeEditorAction = imeOptions and EditorInfo.IME_MASK_ACTION
        clipboardHistory.syncFromSystemClipboard()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        if (outInsets == null) return
        val headroomPx = (TOP_POPUP_HEADROOM_DP * resources.displayMetrics.density).toInt()
        outInsets.contentTopInsets = headroomPx
        outInsets.visibleTopInsets = headroomPx
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
    }

    companion object {
        const val TOP_POPUP_HEADROOM_DP = 68
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        @Suppress("DEPRECATION")
        subtypeLayoutOverride = when (newSubtype?.locale) {
            "fr_FR" -> "azerty_fr"
            "en_US" -> "qwerty_en"
            else -> null
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        clipboardHistory.destroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        super.onDestroy()
    }

    private fun commitTextSafe(text: String) {
        val ic = currentInputConnection ?: return
        if (isRawInput && text.length == 1) {
            sendKeyChar(text[0])
            return
        }
        ic.finishComposingText()
        if (!ic.commitText(text, 1) && text.length == 1) {
            sendKeyChar(text[0])
        }
    }

    private fun performBackspace() {
        val ic = currentInputConnection ?: return
        if (isRawInput) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            return
        }
        ic.beginBatchEdit()
        try {
            ic.finishComposingText()
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
            } else if (!ic.deleteSurroundingText(1, 0)) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            }
        } finally {
            ic.endBatchEdit()
        }
    }

    private fun performForwardDelete() {
        val ic = currentInputConnection ?: return
        if (isRawInput) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_FORWARD_DEL)
            return
        }
        ic.beginBatchEdit()
        try {
            ic.finishComposingText()
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
            } else if (!ic.deleteSurroundingText(0, 1)) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_FORWARD_DEL)
            }
        } finally {
            ic.endBatchEdit()
        }
    }

    private fun performDeleteWord(forward: Boolean) {
        val ic = currentInputConnection ?: return
        if (isRawInput) {
            sendDownUpKeyEvents(if (forward) KeyEvent.KEYCODE_FORWARD_DEL else KeyEvent.KEYCODE_DEL)
            return
        }
        ic.beginBatchEdit()
        try {
            ic.finishComposingText()
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
                return
            }
            if (!forward) {
                val before = ic.getTextBeforeCursor(64, 0)?.toString().orEmpty()
                if (before.isEmpty()) return
                val count = KeyboardStateLogic.wordDeleteCountBefore(before)
                ic.deleteSurroundingText(count, 0)
            } else {
                val after = ic.getTextAfterCursor(64, 0)?.toString().orEmpty()
                if (after.isEmpty()) return
                val count = KeyboardStateLogic.wordDeleteCountAfter(after)
                ic.deleteSurroundingText(0, count)
            }
        } finally {
            ic.endBatchEdit()
        }
    }

    private fun performDeleteParagraph(forward: Boolean) {
        val ic = currentInputConnection ?: return
        if (isRawInput) {
            sendDownUpKeyEvents(if (forward) KeyEvent.KEYCODE_FORWARD_DEL else KeyEvent.KEYCODE_DEL)
            return
        }
        ic.beginBatchEdit()
        try {
            ic.finishComposingText()
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
                return
            }
            if (!forward) {
                val before = ic.getTextBeforeCursor(2048, 0)?.toString().orEmpty()
                if (before.isEmpty()) return
                val count = KeyboardStateLogic.paragraphDeleteCountBefore(before)
                ic.deleteSurroundingText(count, 0)
            } else {
                val after = ic.getTextAfterCursor(2048, 0)?.toString().orEmpty()
                if (after.isEmpty()) return
                val count = KeyboardStateLogic.paragraphDeleteCountAfter(after)
                ic.deleteSurroundingText(0, count)
            }
        } finally {
            ic.endBatchEdit()
        }
    }

    private fun sendCtrlKeyCombination(char: Char) {
        val ic = currentInputConnection ?: return
        val lower = char.lowercaseChar()
        val keyCode = if (lower in 'a'..'z') {
            KeyEvent.KEYCODE_A + (lower - 'a')
        } else {
            return
        }
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(
            KeyEvent(
                now,
                now,
                KeyEvent.ACTION_DOWN,
                keyCode,
                0,
                KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
            ),
        )
        ic.sendKeyEvent(
            KeyEvent(
                now,
                now,
                KeyEvent.ACTION_UP,
                keyCode,
                0,
                KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
            ),
        )
    }

    private fun performReturnAction() {
        val ic = currentInputConnection ?: return
        ic.finishComposingText()
        if (returnAction != ReturnAction.DEFAULT && activeEditorAction != EditorInfo.IME_ACTION_NONE) {
            if (!ic.performEditorAction(activeEditorAction)) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
    }

    private fun moveCursor(deltaSteps: Int) {
        if (deltaSteps == 0) return
        val ic = currentInputConnection ?: return
        ic.finishComposingText()
        if (!isRawInput) {
            val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
            if (extracted != null && extracted.selectionStart >= 0) {
                val textLength = extracted.text?.length ?: 0
                val target = (extracted.selectionStart + deltaSteps).coerceIn(0, textLength)
                if (ic.setSelection(target, target)) {
                    return
                }
            }
        }

        val keyCode = if (deltaSteps < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(kotlin.math.abs(deltaSteps)) {
            sendDownUpKeyEvents(keyCode)
        }
    }

    private fun moveCursorVertical(deltaLines: Int) {
        if (deltaLines == 0) return
        currentInputConnection?.finishComposingText()
        val keyCode = if (deltaLines < 0) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN
        repeat(kotlin.math.abs(deltaLines)) {
            sendDownUpKeyEvents(keyCode)
        }
    }

    private fun shouldAutoCapAtCurrentCursor(autoCapPref: Boolean): Boolean {
        if (!autoCapPref || inputContext != InputContext.NORMAL || isRawInput) return false
        val ic = currentInputConnection ?: return false
        val before = ic.getTextBeforeCursor(16, 0)?.toString().orEmpty()
        return KeyboardStateLogic.shouldAutoCapBeforeCursor(before, inputContext, autoCapPref)
    }

    private fun playFeedback(haptics: Boolean, sound: Boolean) {
        if (haptics) {
            @Suppress("DEPRECATION")
            inputComposeView?.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING,
            )
        }
        if (sound) {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1f)
        }
    }
}

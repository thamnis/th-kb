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
import com.thamnis.thkb.engine.SuggestionCandidate
import com.thamnis.thkb.engine.SuggestionEngine
import com.thamnis.thkb.settings.KbPreferences
import com.thamnis.thkb.settings.KbSettings
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
    private data class PendingReplacement(
        val originalPrefix: String,
        val replacedText: String,
    )

    private lateinit var suggestionEngine: SuggestionEngine
    private var suggestions by mutableStateOf<List<SuggestionCandidate>>(emptyList())
    private var pendingReplacement: PendingReplacement? = null
    private var suppressedAutoReplacePrefix: String? = null
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
        suggestionEngine = SuggestionEngine(this)
        Thread { suggestionEngine.warmUpAsync() }.start()
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
                    suggestions = suggestions,
                    onSelectSuggestion = { candidateWord ->
                        val ic = currentInputConnection ?: return@KeyboardRoot
                        val before = ic.getTextBeforeCursor(96, 0)
                        val (_, prefix) = suggestionEngine.extractContext(before)
                        val replacementWithSpace = "$candidateWord "
                        ic.beginBatchEdit()
                        try {
                            ic.finishComposingText()
                            if (prefix.isNotEmpty()) {
                                ic.deleteSurroundingText(prefix.length, 0)
                            }
                            ic.commitText(replacementWithSpace, 1)
                        } finally {
                            ic.endBatchEdit()
                        }
                        pendingReplacement = PendingReplacement(
                            originalPrefix = prefix,
                            replacedText = replacementWithSpace,
                        )
                        suppressedAutoReplacePrefix = null
                        playFeedback(
                            haptics = settings.hapticsEnabled,
                            sound = settings.soundEnabled,
                        )
                        refreshSuggestions(effectiveLayoutId, settings)
                    },
                    onCommitText = { text ->
                        if (text in AUTO_REPLACE_TRIGGERS) {
                            val autoReplaced = tryAutoReplaceBeforeTrigger(text, effectiveLayoutId, settings)
                            if (!autoReplaced) {
                                pendingReplacement = null
                                suppressedAutoReplacePrefix = null
                                commitTextSafe(text)
                            }
                        } else {
                            pendingReplacement = null
                            suppressedAutoReplacePrefix = null
                            commitTextSafe(text)
                        }
                        refreshSuggestions(effectiveLayoutId, settings)
                    },
                    onBackspace = {
                        performBackspace()
                        refreshSuggestions(effectiveLayoutId, settings)
                    },
                    onForwardDelete = {
                        pendingReplacement = null
                        performForwardDelete()
                        refreshSuggestions(effectiveLayoutId, settings)
                    },
                    onDeleteWord = { forward ->
                        pendingReplacement = null
                        performDeleteWord(forward)
                        refreshSuggestions(effectiveLayoutId, settings)
                    },
                    onDeleteParagraph = { forward ->
                        pendingReplacement = null
                        performDeleteParagraph(forward)
                        refreshSuggestions(effectiveLayoutId, settings)
                    },
                    onSendKeyEvent = { keyCode ->
                        pendingReplacement = null
                        sendDownUpKeyEvents(keyCode)
                    },
                    onSendCtrlKey = { char ->
                        pendingReplacement = null
                        sendCtrlKeyCombination(char)
                    },
                    onReturn = {
                        pendingReplacement = null
                        performReturnAction()
                    },
                    onPaste = {
                        pendingReplacement = null
                        currentInputConnection?.performContextMenuAction(android.R.id.paste)
                    },
                    onUndo = {
                        pendingReplacement = null
                        performUndoAction()
                    },
                    onRedo = {
                        pendingReplacement = null
                        performRedoAction()
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
        pendingReplacement = null
        suppressedAutoReplacePrefix = null
        clipboardHistory.syncFromSystemClipboard()
        val settings = preferences.settings.value
        val effectiveLayoutId = subtypeLayoutOverride ?: settings.layoutId
        refreshSuggestions(effectiveLayoutId, settings)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        val pending = pendingReplacement
        if (pending != null) {
            val tail = currentInputConnection?.getTextBeforeCursor(pending.replacedText.length, 0)?.toString()
            if (tail != pending.replacedText) {
                pendingReplacement = null
            }
        }
        val settings = preferences.settings.value
        val effectiveLayoutId = subtypeLayoutOverride ?: settings.layoutId
        refreshSuggestions(effectiveLayoutId, settings)
    }

    private fun tryAutoReplaceBeforeTrigger(
        triggerText: String,
        effectiveLayoutId: String,
        settings: KbSettings,
    ): Boolean {
        if (isRawInput || !settings.suggestionsEnabled || settings.autocompleteMode == KbSettings.AUTOCOMPLETE_MODE_OFF) {
            return false
        }
        if (inputContext == InputContext.PASSWORD || inputContext == InputContext.TERMINAL || inputContext == InputContext.NUMBER) {
            return false
        }
        val ic = currentInputConnection ?: return false
        val before = ic.getTextBeforeCursor(96, 0) ?: return false
        val (ctxWords, prefix) = suggestionEngine.extractContext(before)
        if (prefix.length < 2 || prefix == suppressedAutoReplacePrefix) {
            return false
        }
        val capHint = shouldAutoCapAtCurrentCursor(settings.autoCapOnOpen)
        val replacement = suggestionEngine.findAutoReplacement(
            layoutId = effectiveLayoutId,
            contextWords = ctxWords,
            prefix = prefix,
            mode = settings.autocompleteMode,
            ivorianEnabled = settings.ivorianFrenchEnabled,
            capitalizeHint = capHint,
        ) ?: return false

        val fullReplacedText = replacement.word + triggerText
        ic.beginBatchEdit()
        try {
            ic.finishComposingText()
            ic.deleteSurroundingText(prefix.length, 0)
            ic.commitText(fullReplacedText, 1)
        } finally {
            ic.endBatchEdit()
        }
        pendingReplacement = PendingReplacement(
            originalPrefix = prefix,
            replacedText = fullReplacedText,
        )
        suppressedAutoReplacePrefix = null
        return true
    }

    private fun refreshSuggestions(effectiveLayoutId: String, settings: KbSettings) {
        if (!settings.suggestionsEnabled || settings.autocompleteMode == KbSettings.AUTOCOMPLETE_MODE_OFF) {
            if (suggestions.isNotEmpty()) suggestions = emptyList()
            return
        }
        if (inputContext == InputContext.PASSWORD || inputContext == InputContext.TERMINAL || inputContext == InputContext.NUMBER) {
            if (suggestions.isNotEmpty()) suggestions = emptyList()
            return
        }
        val ic = currentInputConnection ?: run {
            if (suggestions.isNotEmpty()) suggestions = emptyList()
            return
        }
        val before = ic.getTextBeforeCursor(96, 0)
        val (ctxWords, prefix) = suggestionEngine.extractContext(before)
        val capHint = shouldAutoCapAtCurrentCursor(settings.autoCapOnOpen)
        suggestions = suggestionEngine.suggestForInput(
            layoutId = effectiveLayoutId,
            contextWords = ctxWords,
            prefix = prefix,
            mode = settings.autocompleteMode,
            ivorianEnabled = settings.ivorianFrenchEnabled,
            capitalizeHint = capHint,
            maxResults = 3,
        )
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
        private val AUTO_REPLACE_TRIGGERS = setOf(" ", ".", ",", "!", "?")
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
            pendingReplacement = null
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            return
        }
        val pending = pendingReplacement
        if (pending != null) {
            val selected = ic.getSelectedText(0)
            if (selected.isNullOrEmpty()) {
                val tail = ic.getTextBeforeCursor(pending.replacedText.length, 0)?.toString()
                if (tail == pending.replacedText) {
                    ic.beginBatchEdit()
                    try {
                        ic.finishComposingText()
                        ic.deleteSurroundingText(pending.replacedText.length, 0)
                        if (pending.originalPrefix.isNotEmpty()) {
                            ic.commitText(pending.originalPrefix, 1)
                        }
                    } finally {
                        ic.endBatchEdit()
                    }
                    pendingReplacement = null
                    suppressedAutoReplacePrefix = pending.originalPrefix.ifEmpty { null }
                    return
                }
            }
            pendingReplacement = null
        }
        suppressedAutoReplacePrefix = null
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

    private fun performUndoAction() {
        val ic = currentInputConnection ?: return
        playFeedback(
            haptics = preferences.settings.value.hapticsEnabled,
            sound = preferences.settings.value.soundEnabled,
        )
        val handled = ic.performContextMenuAction(android.R.id.undo)
        if (!handled) {
            val now = SystemClock.uptimeMillis()
            ic.sendKeyEvent(
                KeyEvent(
                    now,
                    now,
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_Z,
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
                    KeyEvent.KEYCODE_Z,
                    0,
                    KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                ),
            )
        }
    }

    private fun performRedoAction() {
        val ic = currentInputConnection ?: return
        playFeedback(
            haptics = preferences.settings.value.hapticsEnabled,
            sound = preferences.settings.value.soundEnabled,
        )
        val handled = ic.performContextMenuAction(android.R.id.redo)
        if (!handled) {
            val now = SystemClock.uptimeMillis()
            ic.sendKeyEvent(
                KeyEvent(
                    now,
                    now,
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_Z,
                    0,
                    KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                ),
            )
            ic.sendKeyEvent(
                KeyEvent(
                    now,
                    now,
                    KeyEvent.ACTION_UP,
                    KeyEvent.KEYCODE_Z,
                    0,
                    KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                ),
            )
            ic.sendKeyEvent(
                KeyEvent(
                    now,
                    now,
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_Y,
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
                    KeyEvent.KEYCODE_Y,
                    0,
                    KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                ),
            )
        }
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

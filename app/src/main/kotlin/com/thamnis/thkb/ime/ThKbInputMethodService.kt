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
import androidx.compose.runtime.LaunchedEffect
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
import com.thamnis.thkb.data.DialectPackManager
import com.thamnis.thkb.data.PersonalDictionaryManager
import com.thamnis.thkb.data.TodoListManager
import com.thamnis.thkb.engine.KeyCenterPoint
import com.thamnis.thkb.engine.SuggestionCandidate
import com.thamnis.thkb.engine.SuggestionEngine
import com.thamnis.thkb.engine.SwipeTrailPoint
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
    private lateinit var personalDictionary: PersonalDictionaryManager
    private lateinit var dialectManager: DialectPackManager
    private data class PendingReplacement(
        val originalPrefix: String,
        val replacedText: String,
    )

    private data class TextSnapshot(
        val before: String,
        val after: String,
    )

    private lateinit var suggestionEngine: SuggestionEngine
    private lateinit var voiceInputManager: VoiceInputManager
    private var suggestions by mutableStateOf<List<SuggestionCandidate>>(emptyList())
    private var pendingReplacement: PendingReplacement? = null
    private var suppressedAutoReplacePrefix: String? = null
    private var swipeAlternativesTail: String? = null
    private var voiceLanguageOverride by mutableStateOf<String?>(null)
    private var pendingVoiceStartAfterPermission = false
    private val undoStack = ArrayDeque<TextSnapshot>()
    private val redoStack = ArrayDeque<TextSnapshot>()
    private var lastCharSnapshotMs = 0L
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
        personalDictionary = PersonalDictionaryManager(this)
        dialectManager = DialectPackManager(this)
        suggestionEngine = SuggestionEngine(this)
        suggestionEngine.syncPersonalDictionary(personalDictionary.state.value, personalDictionary.toJson())
        suggestionEngine.syncDialectOverlay(dialectManager.buildMergedOverlayJson())
        voiceInputManager = VoiceInputManager(this) { spokenText ->
            commitVoiceResult(spokenText)
        }
        Thread {
            suggestionEngine.warmUpAsync()
            assets.loadEmojiSections()
            assets.loadKaomoji()
        }.start()
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
                val personalState by personalDictionary.state.collectAsState()
                val dialectState by dialectManager.state.collectAsState()
                val voiceUiState by voiceInputManager.state.collectAsState()
                val effectiveLayoutId = subtypeLayoutOverride ?: settings.layoutId

                LaunchedEffect(Unit) {
                    VoicePermissionBridge.results.collect { granted ->
                        if (granted && pendingVoiceStartAfterPermission) {
                            pendingVoiceStartAfterPermission = false
                            kotlinx.coroutines.delay(140L)
                            voiceInputManager.startListening(resolveVoiceLanguageTag(effectiveLayoutId))
                        } else {
                            pendingVoiceStartAfterPermission = false
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    VoicePermissionBridge.spokenResults.collect { spoken ->
                        pendingVoiceStartAfterPermission = false
                        kotlinx.coroutines.delay(120L)
                        commitVoiceResult(spoken)
                    }
                }

                LaunchedEffect(personalState) {
                    suggestionEngine.syncPersonalDictionary(
                        personalState,
                        personalDictionary.toJson(personalState),
                    )
                }

                LaunchedEffect(dialectState) {
                    suggestionEngine.syncDialectOverlay(
                        dialectManager.buildMergedOverlayJson(dialectState),
                    )
                }

                KeyboardRoot(
                    inputSessionKey = inputSessionKey,
                    layoutId = effectiveLayoutId,
                    inputContext = inputContext,
                    autoCapOnOpen = KeyboardStateLogic.shouldAutoCapOnOpen(
                        context = inputContext,
                        autoCapPref = settings.autoCapOnOpen,
                    ),
                    showKeyPreview = settings.showKeyPreview,
                    longPressDelayMs = KbSettings.DEFAULT_LONG_PRESS_DELAY_MS,
                    rapidDeleteDelayMs = KbSettings.DEFAULT_RAPID_DELETE_DELAY_MS,
                    deleteGesturesEnabled = settings.deleteGesturesEnabled,
                    resetForwardDeleteOnChar = settings.resetForwardDeleteOnChar,
                    emojiGridColumns = KbSettings.DEFAULT_EMOJI_GRID_COLUMNS,
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
                    onSelectSuggestion = { candidate ->
                        val ic = currentInputConnection ?: return@KeyboardRoot
                        captureUndoSnapshot(coalesceTyping = false)
                        val before = ic.getTextBeforeCursor(96, 0)
                        val (ctxWords, prefix) = suggestionEngine.extractContext(before)
                        val replacementWithSpace = "${candidate.word} "
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
                        if (inputContext != InputContext.PASSWORD && inputContext != InputContext.NUMBER) {
                            val prevWord = ctxWords.lastOrNull().orEmpty()
                            val boost = if (candidate.isLiteral) 10 else 3
                            personalDictionary.learnWord(prevWord, candidate.word, boost)
                            suggestionEngine.learnWord(prevWord, candidate.word, boost)
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
                    onBlockSuggestion = { word ->
                        personalDictionary.blockWord(word)
                        suggestionEngine.blockWord(word)
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
                                val before = currentInputConnection?.getTextBeforeCursor(96, 0)
                                val (ctxWords, typedPrefix) = suggestionEngine.extractContext(before)
                                if (typedPrefix.length >= 2 &&
                                    inputContext != InputContext.PASSWORD &&
                                    inputContext != InputContext.TERMINAL &&
                                    inputContext != InputContext.NUMBER
                                ) {
                                    val prevWord = ctxWords.lastOrNull().orEmpty()
                                    personalDictionary.learnWord(prevWord, typedPrefix, 2)
                                    suggestionEngine.learnWord(prevWord, typedPrefix, 2)
                                }
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
                        captureUndoSnapshot(coalesceTyping = false)
                        currentInputConnection?.performContextMenuAction(android.R.id.paste)
                    },
                    onUndo = {
                        pendingReplacement = null
                        performUndoAction()
                        refreshSuggestions(effectiveLayoutId, settings)
                    },
                    onRedo = {
                        pendingReplacement = null
                        performRedoAction()
                        refreshSuggestions(effectiveLayoutId, settings)
                    },
                    onMoveCursor = { deltaSteps ->
                        moveCursor(deltaSteps)
                    },
                    onMoveCursorVertical = { deltaLines ->
                        moveCursorVertical(deltaLines)
                    },
                    glideTypingEnabled = settings.glideTypingEnabled && !isRawInput &&
                        inputContext != InputContext.PASSWORD &&
                        inputContext != InputContext.TERMINAL &&
                        inputContext != InputContext.NUMBER,
                    glideCharByChar = settings.glideCharByChar,
                    onToggleGlideMode = {
                        preferences.setGlideCharByChar(!settings.glideCharByChar)
                    },
                    onSwipeWord = { keyCenters, trailPoints, appendTrailingSpace ->
                        handleSwipeWord(
                            effectiveLayoutId = effectiveLayoutId,
                            settings = settings,
                            keyCenters = keyCenters,
                            trailPoints = trailPoints,
                            appendTrailingSpace = appendTrailingSpace,
                        )
                    },
                    voiceUiState = voiceUiState,
                    onStartVoiceTyping = {
                        val lang = resolveVoiceLanguageTag(effectiveLayoutId)
                        if (VoicePermissionBridge.hasRecordAudioPermission(this@ThKbInputMethodService)) {
                            voiceInputManager.startListening(lang)
                        } else {
                            pendingVoiceStartAfterPermission = true
                            VoicePermissionBridge.requestPermission(this@ThKbInputMethodService)
                        }
                    },
                    onStopVoiceTyping = {
                        voiceInputManager.stopAndCommitIfPartial()
                    },
                    onCancelVoiceTyping = {
                        voiceInputManager.cancel()
                    },
                    onToggleVoiceLanguage = {
                        val currentLang = resolveVoiceLanguageTag(effectiveLayoutId)
                        val nextLang = if (currentLang.startsWith("fr", ignoreCase = true)) "en-US" else "fr-FR"
                        voiceLanguageOverride = nextLang
                        voiceInputManager.startListening(nextLang)
                    },
                    onLaunchSystemVoicePopup = {
                        val lang = resolveVoiceLanguageTag(effectiveLayoutId)
                        pendingVoiceStartAfterPermission = true
                        voiceInputManager.launchSystemPopupFallback(lang)
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
        if (!restarting && !pendingVoiceStartAfterPermission && !voiceInputManager.state.value.panelVisible) {
            inputSessionKey++
            undoStack.clear()
            redoStack.clear()
            lastCharSnapshotMs = 0L
        }
        val rawInputType = info?.inputType ?: 0
        inputContext = KeyboardStateLogic.resolveInputContext(rawInputType, info?.packageName)
        isRawInput = rawInputType == InputType.TYPE_NULL
        val imeOptions = info?.imeOptions ?: 0
        returnAction = KeyboardStateLogic.resolveReturnAction(imeOptions)
        activeEditorAction = imeOptions and EditorInfo.IME_MASK_ACTION
        pendingReplacement = null
        suppressedAutoReplacePrefix = null
        swipeAlternativesTail = null
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
        val activeSwipeTail = swipeAlternativesTail
        if (activeSwipeTail != null) {
            val tail = currentInputConnection?.getTextBeforeCursor(activeSwipeTail.length, 0)?.toString()
            if (tail == activeSwipeTail) {
                return
            }
            swipeAlternativesTail = null
        }
        val settings = preferences.settings.value
        val effectiveLayoutId = subtypeLayoutOverride ?: settings.layoutId
        refreshSuggestions(effectiveLayoutId, settings)
    }

    private fun resolveVoiceLanguageTag(effectiveLayoutId: String): String {
        voiceLanguageOverride?.let { return it }
        return if (effectiveLayoutId.contains("fr") || effectiveLayoutId.contains("ci")) {
            "fr-FR"
        } else {
            "en-US"
        }
    }

    private fun commitVoiceResult(spokenText: String) {
        val cleaned = spokenText.trim()
        if (cleaned.isEmpty()) return
        val ic = currentInputConnection ?: return
        val settings = preferences.settings.value
        val capStart = shouldAutoCapAtCurrentCursor(settings.autoCapOnOpen)
        val formatted = if (capStart) {
            cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            cleaned
        }
        val beforeChar = ic.getTextBeforeCursor(1, 0)?.firstOrNull()
        val needsSpace = beforeChar != null && !beforeChar.isWhitespace() &&
            beforeChar !in charArrayOf('(', '[', '{', '"', '\'', '/')
        val payload = if (needsSpace) " $formatted" else formatted
        commitTextSafe(payload)
        pendingReplacement = PendingReplacement(originalPrefix = "", replacedText = payload)
        swipeAlternativesTail = null
        playFeedback(haptics = settings.hapticsEnabled, sound = settings.soundEnabled)
        val effectiveLayoutId = subtypeLayoutOverride ?: settings.layoutId
        refreshSuggestions(effectiveLayoutId, settings)
    }

    private fun handleSwipeWord(
        effectiveLayoutId: String,
        settings: KbSettings,
        keyCenters: List<KeyCenterPoint>,
        trailPoints: List<SwipeTrailPoint>,
        appendTrailingSpace: Boolean = false,
    ) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(96, 0)
        val (ctxWords, currentPrefix) = suggestionEngine.extractContext(before)
        val capHint = shouldAutoCapAtCurrentCursor(settings.autoCapOnOpen)
        val candidates = suggestionEngine.decodeSwipePath(
            layoutId = effectiveLayoutId,
            contextWords = ctxWords,
            keyCenters = keyCenters,
            swipePoints = trailPoints,
            capitalizeHint = capHint,
            maxResults = 3,
        )
        val winner = candidates.firstOrNull() ?: return
        val lastBeforeChar = before?.lastOrNull()
        val needsLeadingSpace = lastBeforeChar != null &&
            !lastBeforeChar.isWhitespace() &&
            lastBeforeChar !in charArrayOf('(', '[', '{', '"', '\'', '/', '@', '#')
        val baseCommitted = if (needsLeadingSpace && currentPrefix.isEmpty()) {
            " ${winner.word}"
        } else if (currentPrefix.isEmpty()) {
            winner.word
        } else {
            " ${winner.word}"
        }
        val committed = if (appendTrailingSpace) "$baseCommitted " else baseCommitted
        commitTextSafe(committed, coalesceUndo = false)
        pendingReplacement = PendingReplacement(
            originalPrefix = "",
            replacedText = committed,
        )
        swipeAlternativesTail = committed
        suppressedAutoReplacePrefix = null
        suggestions = if (candidates.size >= 3) {
            listOf(candidates[1], candidates[0], candidates[2])
        } else {
            candidates
        }
        playFeedback(haptics = settings.hapticsEnabled, sound = settings.soundEnabled)
    }

    private fun tryAutoReplaceBeforeTrigger(
        triggerText: String,
        effectiveLayoutId: String,
        settings: KbSettings,
    ): Boolean {
        if (isRawInput || !settings.suggestionsEnabled) {
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
            mode = KbSettings.AUTOCOMPLETE_MODE_ADVANCED,
            ivorianEnabled = false,
            capitalizeHint = capHint,
        ) ?: return false

        val fullReplacedText = replacement.word + triggerText
        captureUndoSnapshot(coalesceTyping = false)
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
        if (!settings.suggestionsEnabled) {
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
        val rawCandidates = suggestionEngine.suggestForInput(
            layoutId = effectiveLayoutId,
            contextWords = ctxWords,
            prefix = prefix,
            mode = KbSettings.AUTOCOMPLETE_MODE_ADVANCED,
            ivorianEnabled = false,
            capitalizeHint = capHint,
            maxResults = 3,
        )
        suggestions = SuggestionEngine.formatStripCandidates(prefix, rawCandidates)
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
        if (!pendingVoiceStartAfterPermission) {
            voiceInputManager.cancel()
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        voiceInputManager.destroy()
        clipboardHistory.destroy()
        personalDictionary.destroy()
        dialectManager.destroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
        super.onDestroy()
    }

    private fun readCurrentSnapshot(): TextSnapshot? {
        if (isRawInput || inputContext == InputContext.PASSWORD) return null
        val ic = currentInputConnection ?: return null
        val before = ic.getTextBeforeCursor(4096, 0)?.toString() ?: return null
        val after = ic.getTextAfterCursor(4096, 0)?.toString().orEmpty()
        return TextSnapshot(before = before, after = after)
    }

    private fun captureUndoSnapshot(coalesceTyping: Boolean = false) {
        val snap = readCurrentSnapshot() ?: return
        val now = SystemClock.uptimeMillis()
        if (undoStack.lastOrNull() == snap) {
            redoStack.clear()
            return
        }
        if (coalesceTyping && undoStack.isNotEmpty() && (now - lastCharSnapshotMs) < 550L) {
            lastCharSnapshotMs = now
            redoStack.clear()
            return
        }
        lastCharSnapshotMs = if (coalesceTyping) now else 0L
        if (undoStack.size >= 45) {
            undoStack.removeFirst()
        }
        undoStack.addLast(snap)
        redoStack.clear()
    }

    private fun applyTextSnapshot(target: TextSnapshot): Boolean {
        val ic = currentInputConnection ?: return false
        val curBefore = ic.getTextBeforeCursor(4096, 0)?.toString() ?: return false
        val curAfter = ic.getTextAfterCursor(4096, 0)?.toString().orEmpty()

        var commonPrefix = 0
        val maxPrefix = minOf(curBefore.length, target.before.length)
        while (commonPrefix < maxPrefix && curBefore[commonPrefix] == target.before[commonPrefix]) {
            commonPrefix++
        }

        var commonSuffix = 0
        val maxSuffix = minOf(curAfter.length, target.after.length)
        while (commonSuffix < maxSuffix &&
            curAfter[curAfter.length - 1 - commonSuffix] == target.after[target.after.length - 1 - commonSuffix]
        ) {
            commonSuffix++
        }

        val delBefore = curBefore.length - commonPrefix
        val delAfter = curAfter.length - commonSuffix
        val insBefore = target.before.substring(commonPrefix)
        val insAfter = target.after.substring(0, target.after.length - commonSuffix)

        ic.beginBatchEdit()
        try {
            ic.finishComposingText()
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
            }
            if (delBefore > 0 || delAfter > 0) {
                ic.deleteSurroundingText(delBefore, delAfter)
            }
            if (insAfter.isEmpty()) {
                if (insBefore.isNotEmpty()) {
                    ic.commitText(insBefore, 1)
                }
            } else {
                val combined = insBefore + insAfter
                ic.commitText(combined, 1)
                val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
                if (extracted != null) {
                    val targetCursor = (extracted.selectionEnd - insAfter.length).coerceAtLeast(0)
                    ic.setSelection(targetCursor, targetCursor)
                } else {
                    repeat(insAfter.length) {
                        sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
                    }
                }
            }
        } finally {
            ic.endBatchEdit()
        }
        return true
    }

    private fun commitTextSafe(text: String, coalesceUndo: Boolean = text.length == 1 && text !in AUTO_REPLACE_TRIGGERS) {
        val ic = currentInputConnection ?: return
        if (isRawInput && text.length == 1) {
            sendKeyChar(text[0])
            return
        }
        captureUndoSnapshot(coalesceTyping = coalesceUndo)
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
        captureUndoSnapshot(coalesceTyping = false)
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
        captureUndoSnapshot(coalesceTyping = false)
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
        captureUndoSnapshot(coalesceTyping = false)
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
        captureUndoSnapshot(coalesceTyping = false)
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
        val current = readCurrentSnapshot()
        while (undoStack.isNotEmpty() && undoStack.last() == current) {
            undoStack.removeLast()
        }
        if (undoStack.isNotEmpty()) {
            val target = undoStack.removeLast()
            if (current != null && redoStack.lastOrNull() != current) {
                if (redoStack.size >= 45) redoStack.removeFirst()
                redoStack.addLast(current)
            }
            lastCharSnapshotMs = 0L
            swipeAlternativesTail = null
            if (applyTextSnapshot(target)) {
                return
            }
        }
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
        val current = readCurrentSnapshot()
        while (redoStack.isNotEmpty() && redoStack.last() == current) {
            redoStack.removeLast()
        }
        if (redoStack.isNotEmpty()) {
            val target = redoStack.removeLast()
            if (current != null && undoStack.lastOrNull() != current) {
                if (undoStack.size >= 45) undoStack.removeFirst()
                undoStack.addLast(current)
            }
            lastCharSnapshotMs = 0L
            swipeAlternativesTail = null
            if (applyTextSnapshot(target)) {
                return
            }
        }
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

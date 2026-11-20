package com.thamnis.thkb.ui

import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.EmojiNature
import androidx.compose.material.icons.outlined.EmojiPeople
import androidx.compose.material.icons.outlined.EmojiSymbols
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.emoji2.emojipicker.EmojiPickerView
import com.thamnis.thkb.data.AssetRepository
import com.thamnis.thkb.data.ClipboardEntry
import com.thamnis.thkb.data.EmojiSection
import com.thamnis.thkb.data.KaomojiItem
import com.thamnis.thkb.data.TodoTask
import com.thamnis.thkb.engine.KeyCenterPoint
import com.thamnis.thkb.engine.SuggestionCandidate
import com.thamnis.thkb.engine.SuggestionEngine
import com.thamnis.thkb.engine.SwipeTrailPoint
import com.thamnis.thkb.ime.AccentPopupState
import com.thamnis.thkb.ime.InputContext
import com.thamnis.thkb.ime.KeyPreviewState
import com.thamnis.thkb.ime.KeyboardLayer
import com.thamnis.thkb.ime.KeyboardStateLogic
import com.thamnis.thkb.ime.PanelMode
import com.thamnis.thkb.ime.ReturnAction
import com.thamnis.thkb.ime.ShiftState
import com.thamnis.thkb.ime.ThKbInputMethodService
import com.thamnis.thkb.ime.VoiceUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

private data class TimedSwipePoint(
    val x: Float,
    val y: Float,
    val timeMs: Long,
)

private data class EmojiToolsPopupState(
    val anchorX: Float,
    val anchorY: Float,
    val selectedIndex: Int = -1,
)

private data class ActivePointer(
    val keyId: String,
    val startPosition: Offset,
    var lastGlideX: Float,
    var lastGlideY: Float,
    var isGliding: Boolean = false,
    var isSwipeTyping: Boolean = false,
    var deleteGestureTriggered: Boolean = false,
    var lastCharGlideKey: String? = null,
    var charDwellCommitted: Boolean = false,
    var longPressJob: Job? = null,
    var repeatJob: Job? = null,
    var charDwellJob: Job? = null,
)

@Composable
fun KeyboardRoot(
    inputSessionKey: Int = 0,
    layoutId: String,
    inputContext: InputContext = InputContext.NORMAL,
    autoCapOnOpen: Boolean = true,
    showKeyPreview: Boolean = true,
    longPressDelayMs: Long = 450L,
    rapidDeleteDelayMs: Long = 580L,
    deleteGesturesEnabled: Boolean = true,
    resetForwardDeleteOnChar: Boolean = false,
    emojiGridColumns: Int = 7,
    returnAction: ReturnAction,
    assets: AssetRepository,
    clipboardEntries: List<ClipboardEntry> = emptyList(),
    todoTasks: List<TodoTask> = emptyList(),
    favoriteItems: Set<String> = emptySet(),
    recentEmojis: List<String> = emptyList(),
    onToggleFavorite: (String) -> Unit = {},
    onRecordRecentEmoji: (String) -> Unit = {},
    onPinClip: (String) -> Unit = {},
    onDeleteClip: (String) -> Unit = {},
    onClearUnpinnedClips: () -> Unit = {},
    onAddTodoTask: (String) -> Unit = {},
    onToggleTodoTask: (String) -> Unit = {},
    onDeleteTodoTask: (String) -> Unit = {},
    onClearCompletedTodoTasks: () -> Unit = {},
    onKeyFeedback: () -> Unit,
    onCommitText: (String) -> Unit,
    onBackspace: () -> Unit,
    onForwardDelete: () -> Unit = {},
    onDeleteWord: (Boolean) -> Unit = {},
    onDeleteParagraph: (Boolean) -> Unit = {},
    onSendKeyEvent: (Int) -> Unit = {},
    onSendCtrlKey: (Char) -> Unit = {},
    onReturn: () -> Unit,
    onPaste: () -> Unit,
    onMoveCursor: (Int) -> Unit,
    onMoveCursorVertical: (Int) -> Unit = {},
    shouldAutoCapNow: () -> Boolean = { false },
    onOpenSettings: () -> Unit,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    suggestions: List<SuggestionCandidate> = emptyList(),
    onSelectSuggestion: (SuggestionCandidate) -> Unit = {},
    onBlockSuggestion: (String) -> Unit = {},
    glideTypingEnabled: Boolean = true,
    glideCharByChar: Boolean = false,
    onToggleGlideMode: () -> Unit = {},
    onToggleLayout: () -> Unit = {},
    onSwipeWord: (List<KeyCenterPoint>, List<SwipeTrailPoint>, Boolean) -> Unit = { _, _, _ -> },
    voiceUiState: VoiceUiState = VoiceUiState(),
    onStartVoiceTyping: () -> Unit = {},
    onStopVoiceTyping: () -> Unit = {},
    onCancelVoiceTyping: () -> Unit = {},
    onToggleVoiceLanguage: () -> Unit = {},
    onLaunchSystemVoicePopup: () -> Unit = {},
) {
    var shiftState by remember(inputSessionKey, autoCapOnOpen) {
        mutableStateOf(if (autoCapOnOpen) ShiftState.ONCE else ShiftState.OFF)
    }
    var lastShiftTapMs by remember(inputSessionKey) { mutableStateOf(0L) }
    var layer by remember(inputSessionKey, inputContext) {
        mutableStateOf(KeyboardStateLogic.initialLayerForContext(inputContext))
    }
    var lastNonNumericLayer by remember(inputSessionKey) { mutableStateOf(KeyboardLayer.ALPHA) }
    var panelMode by remember(inputSessionKey) { mutableStateOf(PanelMode.KEYBOARD) }

    LaunchedEffect(voiceUiState.panelVisible, voiceUiState.isListening, voiceUiState.errorMessage) {
        if (voiceUiState.panelVisible || voiceUiState.isListening || voiceUiState.errorMessage != null) {
            panelMode = PanelMode.VOICE
        } else if (panelMode == PanelMode.VOICE) {
            panelMode = PanelMode.KEYBOARD
        }
    }
    var accentPopup by remember(inputSessionKey) { mutableStateOf<AccentPopupState?>(null) }
    var emojiToolsPopup by remember(inputSessionKey) { mutableStateOf<EmojiToolsPopupState?>(null) }
    var keyPreview by remember(inputSessionKey) { mutableStateOf<KeyPreviewState?>(null) }
    var isForwardDeleteMode by remember(inputSessionKey) { mutableStateOf(false) }
    var ctrlActive by remember(inputSessionKey) { mutableStateOf(false) }
    var isEmojiSearchMode by remember(inputSessionKey) { mutableStateOf(false) }
    var emojiSearchQuery by remember(inputSessionKey) { mutableStateOf("") }
    var scrollPanelHeightDp by remember { mutableStateOf(250f) }
    val emojiGridState = rememberLazyGridState()
    val density = LocalDensity.current
    var rootWidthPx by remember { mutableStateOf(1080f) }

    val canResizePanel = (panelMode == PanelMode.CLIPBOARD && clipboardEntries.size > 4) ||
        (panelMode == PanelMode.TODO && todoTasks.size > 3)

    val activeLayoutId = when (layer) {
        KeyboardLayer.ALPHA -> layoutId
        KeyboardLayer.SYMBOLS -> "symbols"
        KeyboardLayer.SYMBOLS_MORE -> "symbols_more"
        KeyboardLayer.NUMERIC -> "numeric"
    }
    val rawRows = remember(activeLayoutId) { assets.loadLayout(activeLayoutId) }
    val rows = remember(rawRows, layer, inputContext) {
        if (layer != KeyboardLayer.ALPHA) {
            rawRows
        } else {
            when (inputContext) {
                InputContext.URL -> rawRows.mapIndexed { idx, r ->
                    if (idx == 3) r.map { if (it == ",") "/" else it } else r
                }
                InputContext.EMAIL -> rawRows.mapIndexed { idx, r ->
                    if (idx == 3) r.map { if (it == ",") "@" else it } else r
                }
                else -> rawRows
            }
        }
    }
    val kaomojiItems = remember { assets.loadKaomoji() }
    val emojiSections = remember { assets.loadEmojiSections() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { rootWidthPx = it.size.width.toFloat() },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ThKbInputMethodService.TOP_POPUP_HEADROOM_DP.dp),
            )
            Surface(
                color = BgColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 10.dp),
                ) {
                    val isEmojiPanel = panelMode == PanelMode.EMOJI || panelMode == PanelMode.KAOMOJI
                    AnimatedContent(
                        targetState = isEmojiPanel,
                        transitionSpec = {
                            if (targetState) {
                                (fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing)) +
                                    slideInVertically(animationSpec = tween(150, easing = FastOutSlowInEasing)) { it / 16 })
                                    .togetherWith(fadeOut(animationSpec = tween(100)))
                            } else {
                                (fadeIn(animationSpec = tween(140, easing = FastOutSlowInEasing)) +
                                    slideInVertically(animationSpec = tween(150, easing = FastOutSlowInEasing)) { -it / 18 })
                                    .togetherWith(fadeOut(animationSpec = tween(100)))
                            }
                        },
                        label = "emojiContainerTransition",
                    ) { showEmoji ->
                        if (showEmoji) {
                            EmojiAndKaomojiPanel(
                                panelHeightDp = 344f,
                                emojiGridColumns = emojiGridColumns,
                                emojiSections = emojiSections,
                                kaomojiItems = kaomojiItems,
                                favoriteItems = favoriteItems,
                                recentEmojis = recentEmojis,
                                emojiGridState = emojiGridState,
                                searchQuery = emojiSearchQuery,
                                onSearchQueryChange = { emojiSearchQuery = it },
                                onOpenKeyboardSearch = {
                                    onKeyFeedback()
                                    isEmojiSearchMode = true
                                    layer = KeyboardLayer.ALPHA
                                    panelMode = PanelMode.KEYBOARD
                                },
                                onBackToKeyboard = {
                                    onKeyFeedback()
                                    isEmojiSearchMode = false
                                    panelMode = PanelMode.KEYBOARD
                                },
                                onToggleFavorite = onToggleFavorite,
                                onCommitEmoji = { emoji ->
                                    onKeyFeedback()
                                    onRecordRecentEmoji(emoji)
                                    onCommitText(emoji)
                                },
                                onCommitKaomoji = { text ->
                                    onKeyFeedback()
                                    onCommitText(text)
                                },
                                onBackspace = {
                                    onKeyFeedback()
                                    onBackspace()
                                },
                                onBackspaceRepeat = onBackspace,
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (isEmojiSearchMode && panelMode == PanelMode.KEYBOARD) {
                                    EmojiSearchKeyboardHeader(
                                        query = emojiSearchQuery,
                                        recentEmojis = recentEmojis,
                                        onClearQuery = {
                                            onKeyFeedback()
                                            emojiSearchQuery = ""
                                        },
                                        onBackToEmojiGrid = {
                                            onKeyFeedback()
                                            isEmojiSearchMode = false
                                            panelMode = PanelMode.EMOJI
                                        },
                                        onSelectEmoji = { emoji ->
                                            onKeyFeedback()
                                            onRecordRecentEmoji(emoji)
                                            onCommitText(emoji)
                                        },
                                    )
                                } else {
                                    ToolbarRow(
                                        panelMode = panelMode,
                                        canResize = canResizePanel,
                                        isExpanded = canResizePanel && scrollPanelHeightDp > 300f,
                                        onResizeDeltaDp = { deltaDp ->
                                            scrollPanelHeightDp = (scrollPanelHeightDp - deltaDp).coerceIn(250f, 460f)
                                        },
                                        onToggleExpand = {
                                            scrollPanelHeightDp = if (scrollPanelHeightDp > 300f) 250f else 420f
                                        },
                                        onBackToKeyboard = {
                                            onKeyFeedback()
                                            panelMode = PanelMode.KEYBOARD
                                        },
                                        suggestions = suggestions,
                                        onSelectSuggestion = onSelectSuggestion,
                                        onBlockSuggestion = onBlockSuggestion,
                                        voiceActive = panelMode == PanelMode.VOICE || voiceUiState.isListening,
                                        onToggleVoice = {
                                            onKeyFeedback()
                                            if (panelMode == PanelMode.VOICE) {
                                                onCancelVoiceTyping()
                                                panelMode = PanelMode.KEYBOARD
                                            } else {
                                                onStartVoiceTyping()
                                            }
                                        },
                                        onUndo = onUndo,
                                        onRedo = onRedo,
                                    )
                                }

                                if (inputContext == InputContext.TERMINAL && panelMode == PanelMode.KEYBOARD && !isEmojiSearchMode) {
                                    TerminalQuickBar(
                                        ctrlActive = ctrlActive,
                                        onToggleCtrl = {
                                            onKeyFeedback()
                                            ctrlActive = !ctrlActive
                                        },
                                        onSendKey = { keyCode ->
                                            onKeyFeedback()
                                            onSendKeyEvent(keyCode)
                                        },
                                        onCommitText = { text ->
                                            onKeyFeedback()
                                            onCommitText(text)
                                        },
                                    )
                                }

                                AnimatedContent(
                                    targetState = panelMode,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(140))
                                            .togetherWith(fadeOut(animationSpec = tween(100)))
                                    },
                                    label = "panelModeTransition",
                                ) { currentPanelMode ->
                                    when (currentPanelMode) {
                        PanelMode.CLIPBOARD -> {
                            ClipboardPanel(
                                panelHeightDp = if (canResizePanel) scrollPanelHeightDp else 250f,
                                entries = clipboardEntries,
                                onCommitClip = { text ->
                                    onKeyFeedback()
                                    onCommitText(text)
                                },
                                onSystemPaste = {
                                    onKeyFeedback()
                                    onPaste()
                                },
                                onPinClip = onPinClip,
                                onDeleteClip = onDeleteClip,
                                onClearUnpinned = onClearUnpinnedClips,
                            )
                        }
                        PanelMode.TODO -> {
                            TodoListPanel(
                                panelHeightDp = if (canResizePanel) scrollPanelHeightDp else 250f,
                                tasks = todoTasks,
                                onAddTask = onAddTodoTask,
                                onToggleTask = onToggleTodoTask,
                                onDeleteTask = onDeleteTodoTask,
                                onClearCompleted = onClearCompletedTodoTasks,
                                onCommitTaskText = { text ->
                                    onKeyFeedback()
                                    onCommitText(text)
                                },
                                onKeyFeedback = onKeyFeedback,
                            )
                        }
                        PanelMode.VOICE -> {
                            VoiceTypingPanel(
                                voiceUiState = voiceUiState,
                                onStartOrStop = {
                                    onKeyFeedback()
                                    if (voiceUiState.isListening) {
                                        onStopVoiceTyping()
                                    } else {
                                        onStartVoiceTyping()
                                    }
                                },
                                onToggleLanguage = {
                                    onKeyFeedback()
                                    onToggleVoiceLanguage()
                                },
                                onLaunchSystemVoice = {
                                    onKeyFeedback()
                                    onLaunchSystemVoicePopup()
                                },
                                onBackToKeyboard = {
                                    onKeyFeedback()
                                    onCancelVoiceTyping()
                                    panelMode = PanelMode.KEYBOARD
                                },
                                onBackspace = {
                                    onKeyFeedback()
                                    onBackspace()
                                },
                            )
                        }
                        PanelMode.KEYBOARD -> {
                            TouchKeyGrid(
                                rows = rows,
                                layoutId = activeLayoutId,
                                languageLayoutId = layoutId,
                                isAlphaLayer = layer == KeyboardLayer.ALPHA,
                                rootWidthPx = rootWidthPx,
                                shiftState = shiftState,
                                isForwardDeleteMode = isForwardDeleteMode,
                                longPressDelayMs = longPressDelayMs,
                                rapidDeleteDelayMs = rapidDeleteDelayMs,
                                deleteGesturesEnabled = deleteGesturesEnabled,
                                returnAction = if (isEmojiSearchMode) ReturnAction.SEARCH else returnAction,
                                accentPopup = accentPopup,
                                emojiToolsPopup = emojiToolsPopup,
                                assets = assets,
                                onShowAccentPopup = {
                                    accentPopup = it
                                    if (it != null) keyPreview = null
                                },
                                onShowEmojiToolsPopup = {
                                    emojiToolsPopup = it
                                    if (it != null) {
                                        accentPopup = null
                                        keyPreview = null
                                    }
                                },
                                onSelectEmojiTool = { toolIdx ->
                                    emojiToolsPopup = null
                                    onKeyFeedback()
                                    when (toolIdx) {
                                        0 -> panelMode = PanelMode.CLIPBOARD
                                        1 -> panelMode = PanelMode.TODO
                                        2 -> onOpenSettings()
                                    }
                                },
                                onShowKeyPreview = { preview ->
                                    keyPreview = if (showKeyPreview) preview else null
                                },
                                onToggleForwardDeleteMode = {
                                    isForwardDeleteMode = !isForwardDeleteMode
                                },
                                onKeyFeedback = onKeyFeedback,
                                onKeyAction = { keyId, fromAccent ->
                                    if (fromAccent != null) {
                                        if (isEmojiSearchMode) {
                                            emojiSearchQuery += fromAccent
                                        } else {
                                            onCommitText(fromAccent)
                                        }
                                        shiftState = KeyboardStateLogic.shiftAfterCharacterCommit(shiftState)
                                        if (resetForwardDeleteOnChar) isForwardDeleteMode = false
                                        return@TouchKeyGrid
                                    }
                                    when (keyId) {
                                        "SHIFT" -> {
                                            val now = android.os.SystemClock.uptimeMillis()
                                            val isDoubleTap = shiftState == ShiftState.ONCE && (now - lastShiftTapMs) <= 320L
                                            shiftState = KeyboardStateLogic.nextShiftOnTap(shiftState, isDoubleTap)
                                            lastShiftTapMs = if (shiftState == ShiftState.CAPS_LOCK) 0L else now
                                        }
                                        "BACKSPACE" -> {
                                            if (isEmojiSearchMode) {
                                                if (emojiSearchQuery.isNotEmpty()) {
                                                    emojiSearchQuery = emojiSearchQuery.dropLast(1)
                                                } else {
                                                    isEmojiSearchMode = false
                                                    panelMode = PanelMode.EMOJI
                                                }
                                            } else {
                                                if (isForwardDeleteMode) onForwardDelete() else onBackspace()
                                                if (shiftState == ShiftState.OFF && shouldAutoCapNow()) {
                                                    shiftState = ShiftState.ONCE
                                                }
                                            }
                                        }
                                        "SYMBOLS" -> {
                                            layer = KeyboardLayer.SYMBOLS
                                            lastNonNumericLayer = KeyboardLayer.SYMBOLS
                                        }
                                        "MORE_SYMBOLS" -> {
                                            layer = KeyboardLayer.SYMBOLS_MORE
                                            lastNonNumericLayer = KeyboardLayer.SYMBOLS_MORE
                                        }
                                        "ALPHA" -> {
                                            layer = KeyboardLayer.ALPHA
                                            lastNonNumericLayer = KeyboardLayer.ALPHA
                                        }
                                        "NUMERIC" -> layer = KeyboardLayer.NUMERIC
                                        "EMOJI_KEY" -> {
                                            isEmojiSearchMode = false
                                            panelMode = PanelMode.EMOJI
                                        }
                                        "SPACE" -> {
                                            if (isEmojiSearchMode) {
                                                if (emojiSearchQuery.isNotEmpty() && !emojiSearchQuery.endsWith(" ")) {
                                                    emojiSearchQuery += " "
                                                }
                                            } else {
                                                onCommitText(" ")
                                                if (resetForwardDeleteOnChar) isForwardDeleteMode = false
                                                if (shiftState == ShiftState.OFF && shouldAutoCapNow()) {
                                                    shiftState = ShiftState.ONCE
                                                }
                                            }
                                        }
                                        "RETURN" -> {
                                            if (isEmojiSearchMode) {
                                                isEmojiSearchMode = false
                                                panelMode = PanelMode.EMOJI
                                            } else {
                                                onReturn()
                                                if (shiftState == ShiftState.OFF && shouldAutoCapNow()) {
                                                    shiftState = ShiftState.ONCE
                                                }
                                            }
                                        }
                                        else -> {
                                            if (!isEmojiSearchMode && ctrlActive && keyId.length == 1 && keyId[0].isLetter()) {
                                                onSendCtrlKey(keyId[0])
                                                ctrlActive = false
                                                return@TouchKeyGrid
                                            }
                                            val output = KeyboardStateLogic.resolveKeyLabel(
                                                rawKey = keyId,
                                                shiftState = shiftState,
                                                returnAction = returnAction,
                                            )
                                            if (isEmojiSearchMode) {
                                                emojiSearchQuery += output
                                            } else {
                                                onCommitText(output)
                                            }
                                            shiftState = KeyboardStateLogic.shiftAfterCharacterCommit(shiftState)
                                            if (resetForwardDeleteOnChar) isForwardDeleteMode = false
                                        }
                                    }
                                },
                                onBackspaceRepeat = {
                                    if (isEmojiSearchMode) {
                                        if (emojiSearchQuery.isNotEmpty()) {
                                            emojiSearchQuery = emojiSearchQuery.dropLast(1)
                                        }
                                    } else {
                                        if (isForwardDeleteMode) onForwardDelete() else onBackspace()
                                    }
                                },
                                onDeleteWord = {
                                    if (isEmojiSearchMode) {
                                        emojiSearchQuery = emojiSearchQuery.trimEnd().substringBeforeLast(' ', "")
                                    } else {
                                        onDeleteWord(isForwardDeleteMode)
                                    }
                                },
                                onDeleteParagraph = {
                                    if (isEmojiSearchMode) {
                                        emojiSearchQuery = ""
                                    } else {
                                        onDeleteParagraph(isForwardDeleteMode)
                                    }
                                },
                                onToggleNumericMode = {
                                    layer = if (layer == KeyboardLayer.NUMERIC) {
                                        // On Numeric layer, the bottom-left key is ABC with glide-up hint "?#" -> switch to SYMBOLS
                                        KeyboardLayer.SYMBOLS
                                    } else {
                                        lastNonNumericLayer = layer
                                        KeyboardLayer.NUMERIC
                                    }
                                },
                                onMoveCursor = onMoveCursor,
                                onMoveCursorVertical = onMoveCursorVertical,
                                onToggleLayout = onToggleLayout,
                                glideTypingEnabled = glideTypingEnabled,
                                glideCharByChar = glideCharByChar,
                                onToggleGlideMode = {
                                    onToggleGlideMode()
                                },
                                onSwipeWord = { keyCenters, trailPoints, appendTrailingSpace ->
                                    if (isEmojiSearchMode) {
                                        val startCh = trailPoints.firstOrNull()?.let { pt ->
                                            keyCenters.minByOrNull { hypot(it.x - pt.x, it.y - pt.y) }?.ch
                                        }.orEmpty()
                                        val endCh = trailPoints.lastOrNull()?.let { pt ->
                                            keyCenters.minByOrNull { hypot(it.x - pt.x, it.y - pt.y) }?.ch
                                        }.orEmpty()
                                        if (startCh.isNotEmpty()) {
                                            emojiSearchQuery += startCh + endCh
                                        }
                                    } else {
                                        onSwipeWord(keyCenters, trailPoints, appendTrailingSpace)
                                        shiftState = KeyboardStateLogic.shiftAfterCharacterCommit(shiftState)
                                        if (resetForwardDeleteOnChar) isForwardDeleteMode = false
                                    }
                                },
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
            }
        }
    }

        var activePreview by remember { mutableStateOf<KeyPreviewState?>(null) }
        if (keyPreview != null) {
            activePreview = keyPreview
        }
        AnimatedVisibility(
            visible = keyPreview != null && accentPopup == null && panelMode == PanelMode.KEYBOARD,
            enter = scaleIn(
                initialScale = 0.65f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh,
                ),
            ) + fadeIn(animationSpec = tween(50)),
            exit = scaleOut(
                targetScale = 0.75f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = tween(60),
            ) + fadeOut(animationSpec = tween(60)),
        ) {
            val preview = activePreview ?: return@AnimatedVisibility
            val bubbleW = with(density) { 48.dp.toPx() }
            val bubbleH = with(density) { 54.dp.toPx() }
            val gridTopInRootPx = with(density) { (ThKbInputMethodService.TOP_POPUP_HEADROOM_DP + 44).dp.toPx() }
            val targetOffsetX = (preview.centerX - bubbleW / 2f)
                .coerceIn(with(density) { 4.dp.toPx() }, (rootWidthPx - bubbleW - with(density) { 4.dp.toPx() }).coerceAtLeast(4f))
            val targetOffsetY = (gridTopInRootPx + preview.topY - bubbleH - with(density) { 6.dp.toPx() })
                .coerceAtLeast(with(density) { 4.dp.toPx() })
            val bubbleX by animateFloatAsState(
                targetValue = targetOffsetX,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 850f),
                label = "previewBubbleX",
            )
            val bubbleY by animateFloatAsState(
                targetValue = targetOffsetY,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 850f),
                label = "previewBubbleY",
            )

            Surface(
                color = KeyPressedColor,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .zIndex(18f)
                    .size(width = 48.dp, height = 54.dp)
                    .offset { IntOffset(bubbleX.roundToInt(), bubbleY.roundToInt()) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = preview.label,
                        transitionSpec = {
                            (scaleIn(initialScale = 0.8f, animationSpec = tween(70)) + fadeIn(animationSpec = tween(70)))
                                .togetherWith(scaleOut(targetScale = 0.8f, animationSpec = tween(50)) + fadeOut(animationSpec = tween(50)))
                        },
                        label = "previewLabelAnim",
                    ) { label ->
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        var activePopup by remember { mutableStateOf<AccentPopupState?>(null) }
        if (accentPopup != null) {
            activePopup = accentPopup
        }
        AnimatedVisibility(
            visible = accentPopup != null && panelMode == PanelMode.KEYBOARD,
            enter = scaleIn(
                initialScale = 0.75f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ) + fadeIn(animationSpec = tween(90)),
            exit = scaleOut(
                targetScale = 0.85f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = tween(70),
            ) + fadeOut(animationSpec = tween(70)),
        ) {
            val popup = activePopup ?: return@AnimatedVisibility
            val cols = popupColsFor(popup.variants.size)
            val cellDp = if (cols > 5) 38.dp else 44.dp
            val popupRows = popup.variants.chunked(cols)
            val popupHeightPx = with(density) { (popupRows.size * (cellDp.value + 4) + 16).dp.toPx() }
            val gridTopInRootPx = with(density) { (ThKbInputMethodService.TOP_POPUP_HEADROOM_DP + 44).dp.toPx() }
            val popupOffsetY = (gridTopInRootPx + popup.anchorY - popupHeightPx - with(density) { 6.dp.toPx() })
                .coerceAtLeast(with(density) { 4.dp.toPx() })
                .roundToInt()
            val popupOffsetX = popup.anchorX.roundToInt()

            Surface(
                color = Color(0xFF2C2E33),
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .zIndex(20f)
                    .offset { IntOffset(popupOffsetX, popupOffsetY) },
            ) {
                val selectedIdx = popup.selectedIndex
                val actualRowIdx = if (selectedIdx in popup.variants.indices) selectedIdx / cols else 0
                val actualColIdx = if (selectedIdx in popup.variants.indices) selectedIdx % cols else 0
                val displayRowIdx = (popupRows.size - 1) - actualRowIdx

                // Smoothly animated gliding bubble passing across variants
                val bubbleCol by animateFloatAsState(
                    targetValue = actualColIdx.toFloat(),
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 650f),
                    label = "popupBubbleCol",
                )
                val bubbleRow by animateFloatAsState(
                    targetValue = displayRowIdx.toFloat(),
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 650f),
                    label = "popupBubbleRow",
                )
                val cellStepX = cellDp + 2.dp
                val cellStepY = cellDp + 4.dp

                Box(modifier = Modifier.padding(8.dp)) {
                    // Floating selection bubble
                    if (selectedIdx in popup.variants.indices) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    val xPx = with(density) { (bubbleCol * cellStepX.toPx()).roundToInt() }
                                    val yPx = with(density) { (bubbleRow * cellStepY.toPx()).roundToInt() }
                                    IntOffset(xPx, yPx)
                                }
                                .size(cellDp)
                                .background(ReturnBgColor, CircleShape),
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        popupRows.reversed().forEachIndexed { revRowIdx, rowItems ->
                            val actualRow = popupRows.lastIndex - revRowIdx
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                rowItems.forEachIndexed { colIdx, variant ->
                                    val flatIndex = actualRow * cols + colIdx
                                    val isSelected = flatIndex == selectedIdx
                                    val textColor by animateColorAsState(
                                        targetValue = if (isSelected) ReturnFgColor else Color.White,
                                        animationSpec = tween(durationMillis = 100),
                                        label = "variantTextColor",
                                    )
                                    val textScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.15f else 1.0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                        label = "variantTextScale",
                                    )
                                    Box(
                                        modifier = Modifier.size(cellDp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = variant,
                                            color = textColor,
                                            fontSize = if (cols > 5) 18.sp else 20.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            modifier = Modifier.graphicsLayer {
                                                scaleX = textScale
                                                scaleY = textScale
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        var activeEmojiToolsPopup by remember { mutableStateOf<EmojiToolsPopupState?>(null) }
        if (emojiToolsPopup != null) {
            activeEmojiToolsPopup = emojiToolsPopup
        }
        AnimatedVisibility(
            visible = emojiToolsPopup != null && panelMode == PanelMode.KEYBOARD,
            enter = scaleIn(
                initialScale = 0.72f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ) + fadeIn(animationSpec = tween(90)),
            exit = scaleOut(
                targetScale = 0.82f,
                transformOrigin = TransformOrigin(0.5f, 1f),
                animationSpec = tween(70),
            ) + fadeOut(animationSpec = tween(70)),
        ) {
            val toolsPopup = activeEmojiToolsPopup ?: return@AnimatedVisibility
            val cellDp = 44.dp
            val popupHeightPx = with(density) { (cellDp.value + 16).dp.toPx() }
            val gridTopInRootPx = with(density) { (ThKbInputMethodService.TOP_POPUP_HEADROOM_DP + 44).dp.toPx() }
            val popupOffsetY = (gridTopInRootPx + toolsPopup.anchorY - popupHeightPx - with(density) { 6.dp.toPx() })
                .coerceAtLeast(with(density) { 4.dp.toPx() })
                .roundToInt()
            val popupOffsetX = toolsPopup.anchorX.roundToInt()
            val toolIcons = listOf(
                Icons.Outlined.ContentPaste to "Clipboard",
                Icons.Outlined.Checklist to "To-Do",
                Icons.Outlined.Settings to "Settings",
            )
            val selectedIdx = toolsPopup.selectedIndex
            val bubbleCol by animateFloatAsState(
                targetValue = selectedIdx.coerceAtLeast(0).toFloat(),
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 650f),
                label = "emojiToolBubbleCol",
            )
            val cellStepX = cellDp + 4.dp

            Surface(
                color = Color(0xFF2C2E33),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .zIndex(21f)
                    .offset { IntOffset(popupOffsetX, popupOffsetY) },
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    if (selectedIdx in 0..2) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    val xPx = with(density) { (bubbleCol * cellStepX.toPx()).roundToInt() }
                                    IntOffset(xPx, 0)
                                }
                                .size(cellDp)
                                .background(ReturnBgColor, CircleShape),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        toolIcons.forEachIndexed { idx, (iconVec, desc) ->
                            val isSelected = idx == selectedIdx
                            val iconTint by animateColorAsState(
                                targetValue = if (isSelected) ReturnFgColor else Color.White,
                                animationSpec = tween(100),
                                label = "emojiToolIconTint",
                            )
                            Box(
                                modifier = Modifier
                                    .size(cellDp)
                                    .clip(CircleShape)
                                    .clickable {
                                        emojiToolsPopup = null
                                        onKeyFeedback()
                                        when (idx) {
                                            0 -> panelMode = PanelMode.CLIPBOARD
                                            1 -> panelMode = PanelMode.TODO
                                            2 -> onOpenSettings()
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = iconVec,
                                    contentDescription = desc,
                                    tint = iconTint,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiSearchKeyboardHeader(
    query: String,
    recentEmojis: List<String>,
    onClearQuery: () -> Unit,
    onBackToEmojiGrid: () -> Unit,
    onSelectEmoji: (String) -> Unit,
) {
    val matchingEmojis = remember(query, recentEmojis) {
        if (query.isBlank()) {
            recentEmojis.ifEmpty { listOf("😂", "❤️", "👍", "🔥", "🙏", "😍", "🥳", "✅", "🇨🇮", "✨") }
        } else {
            SuggestionEngine.searchEmojisBilingual(query)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ActionKeyBgColor)
                    .clickable(onClick = onBackToEmojiGrid),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to Emoji Grid",
                    tint = ToolbarTextColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ActionKeyBgColor)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (query.isEmpty()) {
                        "🔍 Type on keyboard to search emojis…"
                    } else {
                        "🔍 $query|"
                    },
                    color = if (query.isEmpty()) HintTextColor else Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (query.isNotEmpty()) {
                    Text(
                        text = "Clear",
                        color = ReturnBgColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onClearQuery)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (matchingEmojis.isEmpty()) {
                Text(
                    text = "No matching emojis for \"$query\"",
                    color = HintTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            } else {
                matchingEmojis.take(28).forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectEmoji(emoji) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 26.sp,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarRow(
    panelMode: PanelMode,
    canResize: Boolean,
    isExpanded: Boolean,
    suggestions: List<SuggestionCandidate>,
    onSelectSuggestion: (SuggestionCandidate) -> Unit,
    onBlockSuggestion: (String) -> Unit,
    voiceActive: Boolean,
    onToggleVoice: () -> Unit,
    onResizeDeltaDp: (Float) -> Unit,
    onToggleExpand: () -> Unit,
    onBackToKeyboard: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    val density = LocalDensity.current
    val latestOnResize by rememberUpdatedState(onResizeDeltaDp)
    val isSuggestionMode = panelMode == PanelMode.KEYBOARD && suggestions.isNotEmpty()
    val isSubPanel = panelMode == PanelMode.CLIPBOARD || panelMode == PanelMode.TODO

    val resizeArrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "resizeArrowRotation",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .then(
                if (canResize) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = with(density) { dragAmount.toDp().value }
                            latestOnResize(deltaDp)
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        if (canResize) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(HintTextColor, RoundedCornerShape(2.dp)),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSuggestionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        val displayCandidates = suggestions.take(3)
                        displayCandidates.forEachIndexed { idx, candidate ->
                            if (idx > 0) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(14.dp)
                                        .background(HintTextColor.copy(alpha = 0.25f)),
                                )
                            }
                            val isCenter = when {
                                displayCandidates.size >= 3 -> idx == 1
                                displayCandidates.size == 2 && displayCandidates[0].isLiteral -> idx == 1
                                else -> idx == 0
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (isCenter) {
                                            Modifier.background(ReturnBgColor.copy(alpha = 0.16f))
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .combinedClickable(
                                        onClick = { onSelectSuggestion(candidate) },
                                        onLongClick = {
                                            if (!candidate.isLiteral) {
                                                onBlockSuggestion(candidate.word)
                                            }
                                        },
                                    )
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (candidate.isLiteral) "“${candidate.word}”" else candidate.word,
                                    color = if (isCenter) ReturnBgColor else Color(0xFFE3E3E3),
                                    fontSize = if (isCenter) 15.sp else 14.sp,
                                    fontWeight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                } else if (isSubPanel) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ToolbarIconButton(
                            icon = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to Keyboard",
                            active = false,
                            onClick = onBackToKeyboard,
                        )
                        if (canResize) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isExpanded) ReturnBgColor.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable(onClick = onToggleExpand),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowUp,
                                    contentDescription = "Resize",
                                    tint = if (isExpanded) ReturnBgColor else ToolbarTextColor,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer {
                                            rotationZ = resizeArrowRotation
                                        },
                                )
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ToolbarIconButton(
                    icon = Icons.Outlined.Mic,
                    contentDescription = "Voice Typing",
                    active = voiceActive,
                    onClick = onToggleVoice,
                )

                // Dedicated Undo / Redo Toolbar Pill (unlabeled with vertical bar divider)
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ActionKeyBgColor)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                var undoPressed by remember { mutableStateOf(false) }
                val undoScale by animateFloatAsState(
                    targetValue = if (undoPressed) 0.85f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "undoScale",
                )
                if (undoPressed) {
                    LaunchedEffect(Unit) {
                        delay(120L)
                        undoPressed = false
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            scaleX = undoScale
                            scaleY = undoScale
                        }
                        .clip(CircleShape)
                        .clickable {
                            undoPressed = true
                            onUndo()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = "Undo",
                        tint = ToolbarTextColor,
                        modifier = Modifier.size(17.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(HintTextColor.copy(alpha = 0.35f), RoundedCornerShape(0.5.dp)),
                )

                var redoPressed by remember { mutableStateOf(false) }
                val redoScale by animateFloatAsState(
                    targetValue = if (redoPressed) 0.85f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "redoScale",
                )
                if (redoPressed) {
                    LaunchedEffect(Unit) {
                        delay(120L)
                        redoPressed = false
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            scaleX = redoScale
                            scaleY = redoScale
                        }
                        .clip(CircleShape)
                        .clickable {
                            redoPressed = true
                            onRedo()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Redo,
                        contentDescription = "Redo",
                        tint = ToolbarTextColor,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun TerminalQuickBar(
    ctrlActive: Boolean,
    onToggleCtrl: () -> Unit,
    onSendKey: (Int) -> Unit,
    onCommitText: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryChip(
            label = "CTRL",
            active = ctrlActive,
            onClick = onToggleCtrl,
        )
        listOf(
            "ESC" to { onSendKey(KeyEvent.KEYCODE_ESCAPE) },
            "TAB" to { onSendKey(KeyEvent.KEYCODE_TAB) },
            "^" to { onCommitText("^") },
            "<" to { onCommitText("<") },
            ">" to { onCommitText(">") },
            "←" to { onSendKey(KeyEvent.KEYCODE_DPAD_LEFT) },
            "↓" to { onSendKey(KeyEvent.KEYCODE_DPAD_DOWN) },
            "↑" to { onSendKey(KeyEvent.KEYCODE_DPAD_UP) },
            "→" to { onSendKey(KeyEvent.KEYCODE_DPAD_RIGHT) },
            "-" to { onCommitText("-") },
            "/" to { onCommitText("/") },
            "|" to { onCommitText("|") },
            "~" to { onCommitText("~") },
            "&" to { onCommitText("&") },
            "$" to { onCommitText("$") },
        ).forEach { (label, action) ->
            CategoryChip(label = label, active = false, onClick = action)
        }
    }
}

@Composable
private fun TouchKeyGrid(
    rows: List<List<String>>,
    layoutId: String,
    languageLayoutId: String,
    isAlphaLayer: Boolean,
    rootWidthPx: Float,
    shiftState: ShiftState,
    isForwardDeleteMode: Boolean,
    longPressDelayMs: Long,
    rapidDeleteDelayMs: Long,
    deleteGesturesEnabled: Boolean,
    returnAction: ReturnAction,
    accentPopup: AccentPopupState?,
    emojiToolsPopup: EmojiToolsPopupState?,
    assets: AssetRepository,
    onShowAccentPopup: (AccentPopupState?) -> Unit,
    onShowEmojiToolsPopup: (EmojiToolsPopupState?) -> Unit,
    onSelectEmojiTool: (Int) -> Unit,
    onShowKeyPreview: (KeyPreviewState?) -> Unit,
    onToggleForwardDeleteMode: () -> Unit,
    onKeyFeedback: () -> Unit,
    onKeyAction: (String, String?) -> Unit,
    onBackspaceRepeat: () -> Unit,
    onDeleteWord: () -> Unit,
    onDeleteParagraph: () -> Unit,
    onToggleNumericMode: () -> Unit,
    onMoveCursor: (Int) -> Unit,
    onMoveCursorVertical: (Int) -> Unit,
    onToggleLayout: () -> Unit,
    glideTypingEnabled: Boolean,
    glideCharByChar: Boolean,
    onToggleGlideMode: () -> Unit,
    onSwipeWord: (List<KeyCenterPoint>, List<SwipeTrailPoint>, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val glideThresholdPx = with(density) { 12.dp.toPx() }
    val glideStepPx = with(density) { 14.dp.toPx() }
    val glideVerticalStepPx = with(density) { 18.dp.toPx() }
    val cancelPopupMovePx = with(density) { 14.dp.toPx() }
    val deleteSwipeThresholdPx = with(density) { 18.dp.toPx() }
    val wordSwipeStepPx = with(density) { 24.dp.toPx() }
    val trailLifetimeMs = 175L

    val keyBounds = remember(rows) { mutableMapOf<String, Rect>() }
    val pressedKeys = remember(rows) { mutableStateMapOf<String, Boolean>() }
    val activePointers = remember(rows) { mutableMapOf<PointerId, ActivePointer>() }
    val visualTrail = remember(rows) { mutableStateListOf<TimedSwipePoint>() }
    val wordSwipePoints = remember(rows) { mutableListOf<Offset>() }
    val glidedKeyTimestamps = remember(rows) { mutableStateMapOf<String, Long>() }
    var activeGlidedKey by remember(rows) { mutableStateOf<String?>(null) }
    var spaceGlideOffsetPx by remember(rows) { mutableStateOf(0f) }
    var spaceGlideOffsetY by remember(rows) { mutableStateOf(0f) }
    var specialActionHudText by remember(rows) { mutableStateOf<String?>(null) }
    var specialActionSeq by remember(rows) { mutableStateOf(0) }
    var trailClockMs by remember { mutableStateOf(0L) }
    var activeSwipePointerId by remember(rows) { mutableStateOf<PointerId?>(null) }
    var gridCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(specialActionSeq) {
        if (specialActionSeq > 0) {
            delay(560L)
            specialActionHudText = null
        }
    }

    // Continuous 60fps comet-trail decay & soft button-glide glow decay loop
    LaunchedEffect(visualTrail.isNotEmpty(), glidedKeyTimestamps.isNotEmpty(), activeGlidedKey) {
        while (visualTrail.isNotEmpty() || glidedKeyTimestamps.isNotEmpty()) {
            val now = android.os.SystemClock.uptimeMillis()
            trailClockMs = now
            visualTrail.removeAll { now - it.timeMs > trailLifetimeMs }
            val expiredKeys = glidedKeyTimestamps.entries
                .filter { (k, t) -> k != activeGlidedKey && (now - t) > 240L }
                .map { it.key }
            expiredKeys.forEach { glidedKeyTimestamps.remove(it) }
            delay(16L)
        }
    }

    val latestShift by rememberUpdatedState(shiftState)
    val latestPopup by rememberUpdatedState(accentPopup)
    val latestEmojiToolsPopup by rememberUpdatedState(emojiToolsPopup)
    val latestIsAlphaLayer by rememberUpdatedState(isAlphaLayer)
    val latestLayoutId by rememberUpdatedState(layoutId)
    val latestLanguageLayoutId by rememberUpdatedState(languageLayoutId)
    val latestRootWidthPx by rememberUpdatedState(rootWidthPx)
    val latestLongPressDelayMs by rememberUpdatedState(longPressDelayMs)
    val latestRapidDeleteDelayMs by rememberUpdatedState(rapidDeleteDelayMs)
    val latestDeleteGesturesEnabled by rememberUpdatedState(deleteGesturesEnabled)
    val latestGlideTypingEnabled by rememberUpdatedState(glideTypingEnabled)
    val latestGlideCharByChar by rememberUpdatedState(glideCharByChar)
    val latestOnToggleGlideMode by rememberUpdatedState(onToggleGlideMode)
    val latestOnSwipeWord by rememberUpdatedState(onSwipeWord)
    val latestOnShowAccentPopup by rememberUpdatedState(onShowAccentPopup)
    val latestOnShowEmojiToolsPopup by rememberUpdatedState(onShowEmojiToolsPopup)
    val latestOnSelectEmojiTool by rememberUpdatedState(onSelectEmojiTool)
    val latestOnShowKeyPreview by rememberUpdatedState(onShowKeyPreview)
    val latestOnToggleForwardDelete by rememberUpdatedState(onToggleForwardDeleteMode)
    val latestOnKeyAction by rememberUpdatedState(onKeyAction)
    val latestOnKeyFeedback by rememberUpdatedState(onKeyFeedback)
    val latestOnBackspaceRepeat by rememberUpdatedState(onBackspaceRepeat)
    val latestOnDeleteWord by rememberUpdatedState(onDeleteWord)
    val latestOnDeleteParagraph by rememberUpdatedState(onDeleteParagraph)
    val latestOnToggleNumericMode by rememberUpdatedState(onToggleNumericMode)
    val latestOnMoveCursor by rememberUpdatedState(onMoveCursor)
    val latestOnMoveCursorVertical by rememberUpdatedState(onMoveCursorVertical)
    val latestOnToggleLayout by rememberUpdatedState(onToggleLayout)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { gridCoords = it }
            .pointerInput(rows, layoutId, isAlphaLayer) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()

                        for (change in event.changes) {
                            val localPos = change.position
                            val id = change.id

                            if (change.pressed && !change.previousPressed) {
                                val hitKey = findHitKey(keyBounds, localPos) ?: continue

                                if (latestEmojiToolsPopup != null) {
                                    latestOnShowEmojiToolsPopup(null)
                                }

                                if (latestPopup == null && activePointers.isNotEmpty() && hitKey != "SHIFT") {
                                    val toCommit = activePointers.entries.filter { (_, prev) ->
                                        prev.keyId != "SHIFT" && prev.keyId != "BACKSPACE" && !prev.isGliding
                                    }
                                    for ((prevId, prev) in toCommit) {
                                        activePointers.remove(prevId)
                                        prev.longPressJob?.cancel()
                                        prev.repeatJob?.cancel()
                                        pressedKeys.remove(prev.keyId)
                                        latestOnKeyAction(prev.keyId, null)
                                    }
                                }

                                pressedKeys[hitKey] = true
                                latestOnKeyFeedback()

                                val pointer = ActivePointer(
                                    keyId = hitKey,
                                    startPosition = localPos,
                                    lastGlideX = localPos.x,
                                    lastGlideY = localPos.y,
                                )

                                when (hitKey) {
                                    "BACKSPACE" -> {
                                        // Very-long-press rapid delete mode
                                        pointer.repeatJob = scope.launch {
                                            delay(latestRapidDeleteDelayMs)
                                            pointer.deleteGestureTriggered = true
                                            while (true) {
                                                latestOnBackspaceRepeat()
                                                delay(35L)
                                            }
                                        }
                                    }
                                    "SPACE" -> {
                                        // Hold Spacebar for 1s without gliding -> toggle French AZERTY <-> English QWERTY
                                        pointer.longPressJob = scope.launch {
                                            delay(1000L)
                                            if (!pointer.isGliding) {
                                                pointer.deleteGestureTriggered = true
                                                glidedKeyTimestamps["SPACE"] = android.os.SystemClock.uptimeMillis()
                                                latestOnKeyFeedback()
                                                latestOnToggleLayout()
                                            }
                                        }
                                    }
                                    "EMOJI_KEY" -> {
                                        // Hold Emoji button -> reveal hidden Tools popup (Clipboard, To-Do, Settings)
                                        val rect = keyBounds["EMOJI_KEY"]
                                        pointer.longPressJob = scope.launch {
                                            delay(latestLongPressDelayMs)
                                            if (!pointer.isGliding) {
                                                pointer.deleteGestureTriggered = true
                                                latestOnKeyFeedback()
                                                val popupWidthPx = with(density) { (3 * 48f + 12f).dp.toPx() }
                                                val centerX = rect?.center?.x ?: localPos.x
                                                val popupLeftX = (centerX - popupWidthPx / 2f).coerceIn(
                                                    with(density) { 6.dp.toPx() },
                                                    (latestRootWidthPx - popupWidthPx - with(density) { 6.dp.toPx() }).coerceAtLeast(6f),
                                                )
                                                latestOnShowEmojiToolsPopup(
                                                    EmojiToolsPopupState(
                                                        anchorX = popupLeftX,
                                                        anchorY = rect?.top ?: localPos.y,
                                                        selectedIndex = -1,
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                    "SHIFT", "SYMBOLS", "MORE_SYMBOLS", "ALPHA", "RETURN" -> {
                                        // No long-press character popup for control keys
                                    }
                                    else -> {
                                        val rect = keyBounds[hitKey]
                                        val previewChar = KeyboardStateLogic.resolveKeyLabel(
                                            rawKey = hitKey,
                                            shiftState = latestShift,
                                            returnAction = returnAction,
                                        )
                                        if (rect != null) {
                                            latestOnShowKeyPreview(
                                                KeyPreviewState(
                                                    label = previewChar,
                                                    centerX = rect.center.x,
                                                    topY = rect.top,
                                                ),
                                            )
                                        }
                                        pointer.longPressJob = scope.launch {
                                            delay(latestLongPressDelayMs)
                                            val baseChar = KeyboardStateLogic.resolveKeyLabel(
                                                rawKey = hitKey,
                                                shiftState = latestShift,
                                                returnAction = returnAction,
                                            )
                                            val hint = if (latestIsAlphaLayer) {
                                                KeyboardStateLogic.hintForKey(latestLanguageLayoutId, hitKey)
                                            } else {
                                                null
                                            }
                                            val accents = assets.lookupAccents(baseChar)
                                            val rawVariants = buildList {
                                                if (hint != null) add(hint)
                                                accents.forEach { if (it != hint) add(it) }
                                            }
                                            if (rawVariants.isEmpty()) return@launch
                                            latestOnKeyFeedback()
                                            val cols = popupColsFor(rawVariants.size)
                                            val cellDpVal = if (cols > 5) 38f else 44f
                                            val cellStepPx = with(density) { (cellDpVal + 2f).dp.toPx() }
                                            val padPx = with(density) { 8.dp.toPx() }
                                            val popupWidthPx = with(density) { (cols * (cellDpVal + 2f) + 16f).dp.toPx() }
                                            val fingerX = localPos.x
                                            val desiredLeftX = fingerX - popupWidthPx / 2f
                                            val popupLeftX = desiredLeftX.coerceIn(
                                                with(density) { 6.dp.toPx() },
                                                (latestRootWidthPx - popupWidthPx - with(density) { 6.dp.toPx() }).coerceAtLeast(6f),
                                            )
                                            val fingerCol = ((fingerX - (popupLeftX + padPx)) / cellStepPx)
                                                .toInt()
                                                .coerceIn(0, cols - 1)
                                            val orderedVariants = rawVariants.toMutableList()
                                            if (fingerCol in orderedVariants.indices && fingerCol != 0) {
                                                val primary = orderedVariants[0]
                                                orderedVariants[0] = orderedVariants[fingerCol]
                                                orderedVariants[fingerCol] = primary
                                            }
                                            latestOnShowAccentPopup(
                                                AccentPopupState(
                                                    baseKey = baseChar,
                                                    variants = orderedVariants,
                                                    selectedIndex = fingerCol.coerceIn(0, orderedVariants.lastIndex),
                                                    anchorX = popupLeftX,
                                                    anchorY = rect?.top ?: localPos.y,
                                                ),
                                            )
                                        }
                                    }
                                }

                                activePointers[id] = pointer
                                change.consume()
                            } else if (change.pressed && change.previousPressed) {
                                val pointer = activePointers[id] ?: continue
                                val totalDx = localPos.x - pointer.startPosition.x
                                val totalDy = localPos.y - pointer.startPosition.y

                                if (pointer.keyId == "SPACE") {
                                    if (!pointer.isGliding && (abs(totalDx) > glideThresholdPx || abs(totalDy) > glideThresholdPx)) {
                                        pointer.isGliding = true
                                        pointer.longPressJob?.cancel()
                                        pointer.lastGlideX = localPos.x
                                        pointer.lastGlideY = localPos.y
                                        activeGlidedKey = "SPACE"
                                        glidedKeyTimestamps["SPACE"] = android.os.SystemClock.uptimeMillis()
                                    }
                                    if (pointer.isGliding) {
                                        val now = android.os.SystemClock.uptimeMillis()
                                        activeGlidedKey = "SPACE"
                                        glidedKeyTimestamps["SPACE"] = now
                                        spaceGlideOffsetPx = (localPos.x - pointer.startPosition.x).coerceIn(-64f, 64f)
                                        spaceGlideOffsetY = (localPos.y - pointer.startPosition.y).coerceIn(-24f, 24f)
                                        val stepDx = localPos.x - pointer.lastGlideX
                                        val stepsX = (stepDx / glideStepPx).toInt()
                                        if (stepsX != 0) {
                                            latestOnMoveCursor(stepsX)
                                            pointer.lastGlideX += stepsX * glideStepPx
                                        }
                                        val stepDy = localPos.y - pointer.lastGlideY
                                        val stepsY = (stepDy / glideVerticalStepPx).toInt()
                                        if (stepsY != 0) {
                                            latestOnMoveCursorVertical(stepsY)
                                            pointer.lastGlideY += stepsY * glideVerticalStepPx
                                        }
                                    }
                                } else if (pointer.keyId == "EMOJI_KEY") {
                                    val toolsPopup = latestEmojiToolsPopup
                                    if (toolsPopup != null && (abs(totalDx) > 8f || totalDy < -10f)) {
                                        val cellStepPx = with(density) { 48.dp.toPx() }
                                        val padPx = with(density) { 8.dp.toPx() }
                                        val col = ((localPos.x - (toolsPopup.anchorX + padPx)) / cellStepPx)
                                            .toInt()
                                            .coerceIn(0, 2)
                                        if (col != toolsPopup.selectedIndex) {
                                            latestOnShowEmojiToolsPopup(toolsPopup.copy(selectedIndex = col))
                                        }
                                    }
                                } else if (pointer.keyId == "SHIFT") {
                                    // Glide Up on SHIFT -> Toggle Char-by-Char Glide vs Word Glide Mode
                                    if (!pointer.deleteGestureTriggered && totalDy < -deleteSwipeThresholdPx && abs(totalDy) > abs(totalDx)) {
                                        pointer.deleteGestureTriggered = true
                                        pointer.isGliding = true
                                        pointer.longPressJob?.cancel()
                                        activeGlidedKey = "SHIFT"
                                        glidedKeyTimestamps["SHIFT"] = android.os.SystemClock.uptimeMillis()
                                        specialActionHudText = if (!latestGlideCharByChar) {
                                            "⇧ Glide Mode: Char-by-Char"
                                        } else {
                                            "⇧ Glide Mode: Word & Sentence"
                                        }
                                        specialActionSeq++
                                        latestOnKeyFeedback()
                                        latestOnToggleGlideMode()
                                    }
                                } else if (pointer.keyId == "BACKSPACE" && latestDeleteGesturesEnabled) {
                                    if (abs(totalDx) > glideThresholdPx || abs(totalDy) > glideThresholdPx) {
                                        activeGlidedKey = "BACKSPACE"
                                        glidedKeyTimestamps["BACKSPACE"] = android.os.SystemClock.uptimeMillis()
                                    }
                                    if (!pointer.deleteGestureTriggered) {
                                        when {
                                            // Glide down -> switch Backspace <-> Right-delete mode
                                            totalDy > deleteSwipeThresholdPx && abs(totalDy) > abs(totalDx) -> {
                                                pointer.deleteGestureTriggered = true
                                                pointer.isGliding = true
                                                pointer.repeatJob?.cancel()
                                                latestOnKeyFeedback()
                                                latestOnToggleForwardDelete()
                                            }
                                            // Glide up -> delete one paragraph at once
                                            totalDy < -deleteSwipeThresholdPx && abs(totalDy) > abs(totalDx) -> {
                                                pointer.deleteGestureTriggered = true
                                                pointer.isGliding = true
                                                pointer.repeatJob?.cancel()
                                                latestOnKeyFeedback()
                                                latestOnDeleteParagraph()
                                            }
                                            // Glide left -> word delete mode
                                            totalDx < -deleteSwipeThresholdPx && abs(totalDx) >= abs(totalDy) -> {
                                                pointer.deleteGestureTriggered = true
                                                pointer.isGliding = true
                                                pointer.lastGlideX = localPos.x
                                                pointer.repeatJob?.cancel()
                                                latestOnKeyFeedback()
                                                latestOnDeleteWord()
                                                pointer.repeatJob = scope.launch {
                                                    delay(280L)
                                                    while (true) {
                                                        latestOnDeleteWord()
                                                        delay(140L)
                                                    }
                                                }
                                            }
                                        }
                                    } else if (pointer.isGliding && totalDx < -deleteSwipeThresholdPx) {
                                        val stepDx = pointer.lastGlideX - localPos.x
                                        if (stepDx >= wordSwipeStepPx) {
                                            latestOnDeleteWord()
                                            pointer.lastGlideX = localPos.x
                                        }
                                    }
                                } else if (
                                    (pointer.keyId == "SYMBOLS" && latestIsAlphaLayer) ||
                                    pointer.keyId in setOf("ALPHA", "NUMERIC")
                                ) {
                                    // Bottom-left alphanum switch button only (never Row 2 symbol page toggle)
                                    if (!pointer.deleteGestureTriggered && totalDy < -deleteSwipeThresholdPx && abs(totalDy) > abs(totalDx)) {
                                        pointer.deleteGestureTriggered = true
                                        pointer.longPressJob?.cancel()
                                        activeGlidedKey = pointer.keyId
                                        glidedKeyTimestamps[pointer.keyId] = android.os.SystemClock.uptimeMillis()
                                        latestOnKeyFeedback()
                                        latestOnToggleNumericMode()
                                    }
                                } else {
                                    val popup = latestPopup
                                    if (popup != null) {
                                        val cols = popupColsFor(popup.variants.size)
                                        val cellDpVal = if (cols > 5) 38f else 44f
                                        val cellStepPx = with(density) { (cellDpVal + 2f).dp.toPx() }
                                        val padPx = with(density) { 8.dp.toPx() }
                                        val col = ((localPos.x - (popup.anchorX + padPx)) / cellStepPx)
                                            .toInt()
                                            .coerceIn(0, cols - 1)
                                        val rowCount = (popup.variants.size + cols - 1) / cols
                                        val rowOffset = if (rowCount > 1 && totalDy < -cellStepPx * 0.65f) 1 else 0
                                        val nextIndex = (rowOffset * cols + col).coerceIn(0, popup.variants.lastIndex)
                                        if (nextIndex != popup.selectedIndex) {
                                            latestOnShowAccentPopup(popup.copy(selectedIndex = nextIndex))
                                        }
                                    } else {
                                        val now = android.os.SystemClock.uptimeMillis()
                                        val isSwipeCandidateKey = latestGlideTypingEnabled &&
                                            latestIsAlphaLayer &&
                                            pointer.keyId.length == 1 &&
                                            pointer.keyId[0].isLetter()
                                        val hoverKey = findHitKey(keyBounds, localPos)
                                        if (pointer.isSwipeTyping) {
                                            if (activeSwipePointerId != id) {
                                                visualTrail.clear()
                                                wordSwipePoints.clear()
                                                visualTrail.add(TimedSwipePoint(pointer.startPosition.x, pointer.startPosition.y, now))
                                                wordSwipePoints.add(pointer.startPosition)
                                                activeSwipePointerId = id
                                            }
                                            val lastVisual = visualTrail.lastOrNull()
                                            if (lastVisual == null || hypot(localPos.x - lastVisual.x, localPos.y - lastVisual.y) >= 3.5f) {
                                                visualTrail.add(TimedSwipePoint(localPos.x, localPos.y, now))
                                                trailClockMs = now
                                                visualTrail.removeAll { now - it.timeMs > trailLifetimeMs }
                                            }
                                            if (hoverKey != null) {
                                                activeGlidedKey = hoverKey
                                                glidedKeyTimestamps[hoverKey] = now
                                            }

                                            if (latestGlideCharByChar) {
                                                // Char-by-Char Word & Sentence Glide Mode: dwell on any letter, SPACE, or BACKSPACE (longer 260ms delay so sliding past keys won't misfire)
                                                if (hoverKey != null && hoverKey != pointer.lastCharGlideKey) {
                                                    pointer.lastCharGlideKey = hoverKey
                                                    pointer.charDwellJob?.cancel()
                                                    val isAllowedCharKey = (hoverKey.length == 1 && hoverKey != "SHIFT") ||
                                                        hoverKey == "SPACE" ||
                                                        hoverKey == "BACKSPACE"
                                                    if (isAllowedCharKey) {
                                                        pointer.charDwellJob = scope.launch {
                                                            delay(260L)
                                                            val t1 = android.os.SystemClock.uptimeMillis()
                                                            glidedKeyTimestamps[hoverKey] = t1
                                                            latestOnKeyFeedback()
                                                            latestOnKeyAction(hoverKey, null)
                                                            // Hold longer on the same key to repeat (e.g. double letter "ll" or second space)
                                                            delay(520L)
                                                            val t2 = android.os.SystemClock.uptimeMillis()
                                                            glidedKeyTimestamps[hoverKey] = t2
                                                            latestOnKeyFeedback()
                                                            latestOnKeyAction(hoverKey, null)
                                                        }
                                                    }
                                                }
                                            } else {
                                                // FUTO-inspired Word & Sentence Glide Mode
                                                if (hoverKey == "SPACE" && wordSwipePoints.size >= 3) {
                                                    // Sentence Gliding: touching SPACE mid-glide commits current word + space and starts next word!
                                                    pointer.charDwellJob?.cancel()
                                                    val pointsSnapshot = wordSwipePoints.map { pt -> SwipeTrailPoint(pt.x, pt.y) }
                                                    val centersSnapshot = keyBounds.entries.mapNotNull { (k, rect) ->
                                                        if (k.length == 1 && k[0].isLetter()) {
                                                            KeyCenterPoint(ch = k, x = rect.center.x, y = rect.center.y)
                                                        } else {
                                                            null
                                                        }
                                                    }
                                                    wordSwipePoints.clear()
                                                    pointer.lastCharGlideKey = "SPACE"
                                                    if (centersSnapshot.isNotEmpty()) {
                                                        latestOnSwipeWord(centersSnapshot, pointsSnapshot, true)
                                                    }
                                                } else if (hoverKey != "SPACE") {
                                                    val lastWordPt = wordSwipePoints.lastOrNull()
                                                    if (lastWordPt == null || hypot(localPos.x - lastWordPt.x, localPos.y - lastWordPt.y) >= 4f) {
                                                        wordSwipePoints.add(localPos)
                                                    }
                                                    if (hoverKey != null && hoverKey != pointer.lastCharGlideKey) {
                                                        pointer.lastCharGlideKey = hoverKey
                                                        pointer.charDwellJob?.cancel()
                                                        if (hoverKey.length == 1 && hoverKey[0].isLetter()) {
                                                            // Hybrid dwell-to-commit: pausing stationary on a key for 480ms commits that char directly
                                                            pointer.charDwellJob = scope.launch {
                                                                delay(480L)
                                                                pointer.charDwellCommitted = true
                                                                wordSwipePoints.clear()
                                                                glidedKeyTimestamps[hoverKey] = android.os.SystemClock.uptimeMillis()
                                                                latestOnKeyFeedback()
                                                                latestOnKeyAction(hoverKey, null)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else if (isSwipeCandidateKey && hypot(totalDx, totalDy) > glideThresholdPx) {
                                            pointer.isSwipeTyping = true
                                            pointer.isGliding = true
                                            pointer.longPressJob?.cancel()
                                            latestOnShowKeyPreview(null)
                                            visualTrail.clear()
                                            wordSwipePoints.clear()
                                            visualTrail.add(TimedSwipePoint(pointer.startPosition.x, pointer.startPosition.y, now))
                                            visualTrail.add(TimedSwipePoint(localPos.x, localPos.y, now))
                                            trailClockMs = now
                                            wordSwipePoints.add(pointer.startPosition)
                                            wordSwipePoints.add(localPos)
                                            activeSwipePointerId = id
                                            activeGlidedKey = hoverKey ?: pointer.keyId
                                            glidedKeyTimestamps[pointer.keyId] = now
                                            if (hoverKey != null) {
                                                glidedKeyTimestamps[hoverKey] = now
                                            }
                                            if (latestGlideCharByChar) {
                                                // Commit the starting letter right away in Char-by-Char glide mode
                                                pointer.lastCharGlideKey = pointer.keyId
                                                pointer.charDwellCommitted = true
                                                latestOnKeyAction(pointer.keyId, null)
                                            } else {
                                                pointer.lastCharGlideKey = pointer.keyId
                                            }
                                        } else if (abs(totalDx) > cancelPopupMovePx || abs(totalDy) > cancelPopupMovePx) {
                                            // Cancel pending long-press popup if finger glides before delay
                                            pointer.longPressJob?.cancel()
                                            if (totalDx < -deleteSwipeThresholdPx) {
                                                pointer.isGliding = true
                                            }
                                        }
                                    }
                                }
                                change.consume()
                            } else if (!change.pressed && change.previousPressed) {
                                val pointer = activePointers.remove(id) ?: continue
                                pointer.longPressJob?.cancel()
                                pointer.repeatJob?.cancel()
                                pointer.charDwellJob?.cancel()
                                pressedKeys.remove(pointer.keyId)
                                latestOnShowKeyPreview(null)
                                activeGlidedKey = null
                                if (pointer.keyId == "SPACE") {
                                    spaceGlideOffsetPx = 0f
                                    spaceGlideOffsetY = 0f
                                }

                                val toolsPopup = latestEmojiToolsPopup
                                val popup = latestPopup
                                if (pointer.keyId == "EMOJI_KEY" && toolsPopup != null) {
                                    if (toolsPopup.selectedIndex in 0..2) {
                                        latestOnSelectEmojiTool(toolsPopup.selectedIndex)
                                    }
                                } else if (popup != null) {
                                    val chosen = popup.variants[popup.selectedIndex]
                                    latestOnShowAccentPopup(null)
                                    latestOnKeyAction(pointer.keyId, chosen)
                                } else if (pointer.isSwipeTyping) {
                                    val pointsSnapshot = wordSwipePoints.map { pt -> SwipeTrailPoint(pt.x, pt.y) }
                                    val centersSnapshot = keyBounds.entries.mapNotNull { (k, rect) ->
                                        if (k.length == 1 && k[0].isLetter()) {
                                            KeyCenterPoint(ch = k, x = rect.center.x, y = rect.center.y)
                                        } else {
                                            null
                                        }
                                    }
                                    activeSwipePointerId = null
                                    wordSwipePoints.clear()
                                    if (!latestGlideCharByChar &&
                                        !pointer.charDwellCommitted &&
                                        pointsSnapshot.size >= 2 &&
                                        centersSnapshot.isNotEmpty()
                                    ) {
                                        latestOnSwipeWord(centersSnapshot, pointsSnapshot, false)
                                    }
                                } else if (!pointer.isGliding && !pointer.deleteGestureTriggered) {
                                    latestOnKeyAction(pointer.keyId, null)
                                }
                                change.consume()
                            }
                        }

                        if (event.changes.none { it.pressed }) {
                            break
                        }
                    }
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEachIndexed { rowIndex, row ->
                val centerNineKeyRow = isAlphaLayer && rowIndex == 1 && row.size == 9
                val rowHeight = if (layoutId == "numeric") 56.dp else 52.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (centerNineKeyRow) {
                        Spacer(modifier = Modifier.weight(0.5f))
                    }
                    row.forEach { keyId ->
                        val weight = keyWeightFor(keyId, layoutId)
                        val isPill = isPillKey(keyId, rowIndex)
                        val isLessRoundedControl = isLessRoundedControlKey(keyId, rowIndex)
                        val isAction = isActionKey(keyId)
                        val isPressed = pressedKeys[keyId] == true
                        val isReturn = keyId == "RETURN"
                        val isShiftActive = keyId == "SHIFT" && shiftState != ShiftState.OFF
                        val isForwardDeleteKey = keyId == "BACKSPACE" && isForwardDeleteMode
                        val isSpaceGliding = keyId == "SPACE" && activePointers.values.any { it.keyId == "SPACE" && it.isGliding }

                        val bg = when {
                            isReturn -> ReturnBgColor
                            isShiftActive || isForwardDeleteKey -> KeyPressedColor
                            isPressed -> KeyPressedColor
                            isPill || isLessRoundedControl -> PillKeyBgColor
                            isAction -> ActionKeyBgColor
                            else -> KeyBgColor
                        }
                        val fg = when {
                            isReturn -> ReturnFgColor
                            isForwardDeleteKey -> ReturnBgColor
                            else -> Color.White
                        }
                        val shape: Shape = when {
                            isPill -> RoundedCornerShape(50)
                            isLessRoundedControl -> RoundedCornerShape(10.dp)
                            else -> RoundedCornerShape(8.dp)
                        }
                        val hint = when {
                            keyId == "SHIFT" && glideTypingEnabled && isAlphaLayer -> if (glideCharByChar) "Char" else "Word"
                            rowIndex == 3 && keyId == "SYMBOLS" -> "123"
                            rowIndex == 3 && keyId == "NUMERIC" -> "?#"
                            rowIndex == 3 && keyId == "ALPHA" && layoutId == "numeric" -> "?#"
                            rowIndex == 3 && keyId == "ALPHA" && !isAlphaLayer -> "123"
                            isAlphaLayer -> KeyboardStateLogic.hintForKey(languageLayoutId, keyId)
                            else -> null
                        }
                        val lastGlidedAt = glidedKeyTimestamps[keyId]
                        val isKeyGlided = activeGlidedKey == keyId ||
                            (lastGlidedAt != null && (trailClockMs - lastGlidedAt) in 0..220)

                        val keyLabel = if (keyId == "SPACE") {
                            if (languageLayoutId.contains("fr") || languageLayoutId.contains("ci")) {
                                "Français • AZERTY"
                            } else {
                                "English • QWERTY"
                            }
                        } else {
                            KeyboardStateLogic.resolveKeyLabel(keyId, shiftState, returnAction)
                        }

                        KeyCap(
                            keyId = keyId,
                            label = keyLabel,
                            shiftState = shiftState,
                            isForwardDeleteKey = isForwardDeleteKey,
                            isPressed = isPressed,
                            isGlidedOver = isKeyGlided,
                            isSpaceGliding = isSpaceGliding,
                            spaceGlideOffsetPx = if (keyId == "SPACE") spaceGlideOffsetPx else 0f,
                            spaceGlideOffsetY = if (keyId == "SPACE") spaceGlideOffsetY else 0f,
                            returnAction = returnAction,
                            icon = keyIconFor(keyId, returnAction),
                            hint = hint,
                            weight = weight,
                            shape = shape,
                            backgroundColor = bg,
                            foregroundColor = fg,
                            onBoundsChanged = { coords ->
                                val parent = gridCoords ?: return@KeyCap
                                if (parent.isAttached && coords.isAttached) {
                                    keyBounds[keyId] = parent.localBoundingBoxOf(coords, clipBounds = false)
                                }
                            },
                        )
                    }
                    if (centerNineKeyRow) {
                        Spacer(modifier = Modifier.weight(0.5f))
                    }
                }
            }
        }

        if (visualTrail.size >= 2) {
            val nowMs = trailClockMs.coerceAtLeast(visualTrail.last().timeMs)
            Canvas(modifier = Modifier.matchParentSize()) {
                val n = visualTrail.size
                for (i in 1 until n) {
                    val p0 = visualTrail[i - 1]
                    val p1 = visualTrail[i]
                    val ageMs = (nowMs - p1.timeMs).coerceAtLeast(0L)
                    val freshness = (1f - ageMs.toFloat() / trailLifetimeMs.toFloat()).coerceIn(0f, 1f)
                    val orderFactor = i.toFloat() / (n - 1).coerceAtLeast(1)
                    val intensity = (freshness * (0.25f + 0.75f * orderFactor)).coerceIn(0f, 1f)
                    if (intensity <= 0.02f) continue

                    val o0 = Offset(p0.x, p0.y)
                    val o1 = Offset(p1.x, p1.y)
                    drawLine(
                        color = ReturnBgColor.copy(alpha = 0.42f * intensity),
                        start = o0,
                        end = o1,
                        strokeWidth = (3.dp.toPx() + 9.dp.toPx() * intensity),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f * intensity),
                        start = o0,
                        end = o1,
                        strokeWidth = (1.2.dp.toPx() + 2.8.dp.toPx() * intensity),
                        cap = StrokeCap.Round,
                    )
                }
            }
            val currentGlided = activeGlidedKey
            if (currentGlided != null && (glideCharByChar || currentGlided == "SPACE")) {
                Surface(
                    color = ActionKeyBgColor.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp),
                ) {
                    Text(
                        text = when {
                            glideCharByChar && currentGlided == "SPACE" -> "Char Glide: ␣ (Space)"
                            glideCharByChar && currentGlided == "BACKSPACE" -> "Char Glide: ⌫"
                            glideCharByChar -> "Char Glide: ${currentGlided.uppercase()}"
                            else -> "Sentence Glide • Space = Next Word"
                        },
                        color = ReturnBgColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = specialActionHudText != null,
            enter = scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            ) + fadeIn(tween(80)),
            exit = scaleOut(targetScale = 0.85f, animationSpec = tween(100)) + fadeOut(tween(100)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp),
        ) {
            Surface(
                color = ReturnBgColor,
                shape = RoundedCornerShape(50),
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = specialActionHudText.orEmpty(),
                    color = ReturnFgColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                )
            }
        }
    }
}

private fun popupColsFor(size: Int): Int =
    if (size <= 5) size else ((size + 1) / 2).coerceIn(4, 8)

@Composable
private fun RowScope.KeyCap(
    keyId: String,
    label: String,
    shiftState: ShiftState,
    isForwardDeleteKey: Boolean,
    isPressed: Boolean,
    isGlidedOver: Boolean,
    isSpaceGliding: Boolean,
    spaceGlideOffsetPx: Float,
    spaceGlideOffsetY: Float,
    returnAction: ReturnAction,
    icon: ImageVector?,
    hint: String?,
    weight: Float,
    shape: Shape,
    backgroundColor: Color,
    foregroundColor: Color,
    onBoundsChanged: (LayoutCoordinates) -> Unit,
) {
    val glideGlow by animateFloatAsState(
        targetValue = if (isGlidedOver) 1f else 0f,
        animationSpec = tween(durationMillis = if (isGlidedOver) 55 else 190),
        label = "keyGlideGlow",
    )
    val keyScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.93f
            isSpaceGliding -> 1.03f
            isGlidedOver -> 1.045f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "keyScale",
    )
    val animatedBg by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(durationMillis = 80),
        label = "keyBg",
    )
    val backspaceFlip by animateFloatAsState(
        targetValue = if (isForwardDeleteKey) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "backspaceFlip",
    )
    val animatedSpaceShiftPx by animateFloatAsState(
        targetValue = if (isSpaceGliding) spaceGlideOffsetPx.coerceIn(-44f, 44f) else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "spaceCursorGlideShift",
    )
    val animatedSpaceShiftY by animateFloatAsState(
        targetValue = if (isSpaceGliding) spaceGlideOffsetY.coerceIn(-16f, 16f) else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "spaceCursorGlideShiftY",
    )

    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = keyScale
                scaleY = keyScale
                if (keyId == "SPACE" && isSpaceGliding) {
                    translationX = animatedSpaceShiftPx * 0.14f
                    translationY = animatedSpaceShiftY * 0.35f
                }
            }
            .onGloballyPositioned(onBoundsChanged)
            .background(KeyShadowColor, shape)
            .padding(bottom = 1.5.dp)
            .background(animatedBg, shape)
            .then(
                if (glideGlow > 0.01f) {
                    Modifier.background(
                        color = ReturnBgColor.copy(alpha = 0.24f * glideGlow),
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        if (hint != null) {
            Text(
                text = hint,
                color = if (keyId == "SHIFT" && hint == "Char") ReturnBgColor else HintTextColor,
                fontSize = 10.sp,
                fontWeight = if (keyId == "SHIFT" && hint == "Char") FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 5.dp),
            )
        }
        when {
            keyId == "SHIFT" -> {
                ShiftArrowIcon(
                    shiftState = shiftState,
                    tint = foregroundColor,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.Center),
                )
            }
            keyId == "BACKSPACE" -> {
                Icon(
                    imageVector = icon ?: Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = label,
                    tint = foregroundColor,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            rotationY = backspaceFlip
                        },
                )
            }
            keyId == "RETURN" -> {
                AnimatedContent(
                    targetState = returnAction,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.8f))
                            .togetherWith(fadeOut(animationSpec = tween(90)) + scaleOut(targetScale = 0.8f))
                    },
                    label = "returnActionAnim",
                    modifier = Modifier.align(Alignment.Center),
                ) { act ->
                    val returnIcon = keyIconFor("RETURN", act)
                    if (returnIcon != null) {
                        Icon(
                            imageVector = returnIcon,
                            contentDescription = label,
                            tint = foregroundColor,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Text(
                            text = act.label,
                            color = foregroundColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            keyId == "SPACE" && isSpaceGliding -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = animatedSpaceShiftPx
                                translationY = animatedSpaceShiftY
                            }
                            .size(20.dp)
                            .background(ReturnBgColor.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(ReturnBgColor, CircleShape),
                        )
                    }
                }
            }
            icon != null -> {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = foregroundColor,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.Center),
                )
            }
            else -> {
                Text(
                    text = label,
                    color = foregroundColor,
                    fontSize = when {
                        keyId in setOf("SPACE", "SYMBOLS", "MORE_SYMBOLS", "ALPHA", "NUMERIC") -> 14.sp
                        keyId.length == 1 && keyId[0].isDigit() -> 24.sp
                        else -> 21.sp
                    },
                    fontWeight = when {
                        keyId.length == 1 && keyId[0].isDigit() -> FontWeight.SemiBold
                        isActionKey(keyId) -> FontWeight.Medium
                        else -> FontWeight.Normal
                    },
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun ShiftArrowIcon(
    shiftState: ShiftState,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val arrowScale by animateFloatAsState(
        targetValue = if (shiftState != ShiftState.OFF) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "shiftArrowScale",
    )
    val capsLockBarAlpha by animateFloatAsState(
        targetValue = if (shiftState == ShiftState.CAPS_LOCK) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "capsLockBarAlpha",
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = arrowScale
            scaleY = arrowScale
        },
    ) {
        val w = size.width
        val h = size.height
        val shaftBottom = if (shiftState == ShiftState.CAPS_LOCK) h * 0.70f else h * 0.80f
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.14f)
            lineTo(w * 0.16f, h * 0.50f)
            lineTo(w * 0.36f, h * 0.50f)
            lineTo(w * 0.36f, shaftBottom)
            lineTo(w * 0.64f, shaftBottom)
            lineTo(w * 0.64f, h * 0.50f)
            lineTo(w * 0.84f, h * 0.50f)
            close()
        }
        if (shiftState == ShiftState.OFF) {
            drawPath(
                path = path,
                color = tint,
                style = Stroke(
                    width = 1.9.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        } else {
            drawPath(
                path = path,
                color = tint,
                style = Fill,
            )
        }
        if (capsLockBarAlpha > 0f) {
            drawRoundRect(
                color = tint.copy(alpha = capsLockBarAlpha),
                topLeft = Offset(w * 0.36f, h * 0.78f),
                size = Size(w * 0.28f, h * 0.10f),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
        }
    }
}

private fun keyIconFor(
    keyId: String,
    returnAction: ReturnAction,
): ImageVector? = when (keyId) {
    "BACKSPACE" -> Icons.AutoMirrored.Outlined.Backspace
    "EMOJI_KEY" -> Icons.Outlined.SentimentSatisfied
    "RETURN" -> when (returnAction) {
        ReturnAction.SEARCH -> Icons.Outlined.Search
        ReturnAction.SEND -> Icons.AutoMirrored.Outlined.Send
        ReturnAction.NEXT, ReturnAction.GO -> Icons.AutoMirrored.Outlined.ArrowForward
        ReturnAction.DONE -> Icons.Outlined.Done
        ReturnAction.DEFAULT -> Icons.AutoMirrored.Outlined.KeyboardReturn
    }
    else -> null
}

private fun findHitKey(bounds: Map<String, Rect>, pos: Offset): String? {
    for ((key, rect) in bounds) {
        if (rect.contains(pos)) return key
    }
    var bestKey: String? = null
    var bestDistSq = Float.MAX_VALUE
    for ((key, rect) in bounds) {
        val clampedX = pos.x.coerceIn(rect.left, rect.right)
        val clampedY = pos.y.coerceIn(rect.top, rect.bottom)
        val dx = pos.x - clampedX
        val dy = pos.y - clampedY
        val distSq = dx * dx + dy * dy
        if (distSq < bestDistSq && distSq <= 2500f) {
            bestDistSq = distSq
            bestKey = key
        }
    }
    return bestKey
}

private fun keyWeightFor(keyId: String, layoutId: String = ""): Float {
    if (layoutId == "numeric") return 1f
    return when (keyId) {
        "SPACE" -> 4.0f
        "SHIFT", "BACKSPACE", "SYMBOLS", "MORE_SYMBOLS", "ALPHA", "NUMERIC", "RETURN" -> 1.5f
        else -> 1f
    }
}

private fun isPillKey(keyId: String, rowIndex: Int): Boolean =
    rowIndex == 3 && keyId == "RETURN"

private fun isLessRoundedControlKey(keyId: String, rowIndex: Int): Boolean =
    keyId in setOf("SHIFT", "MORE_SYMBOLS", "BACKSPACE") ||
        (rowIndex == 2 && keyId == "SYMBOLS") ||
        (rowIndex == 3 && keyId in setOf("SYMBOLS", "ALPHA", "NUMERIC"))

private fun isActionKey(keyId: String): Boolean = keyId in setOf(
    "SHIFT",
    "BACKSPACE",
    "SYMBOLS",
    "MORE_SYMBOLS",
    "ALPHA",
    "NUMERIC",
    "RETURN",
    ",",
    ".",
    "EMOJI_KEY",
)

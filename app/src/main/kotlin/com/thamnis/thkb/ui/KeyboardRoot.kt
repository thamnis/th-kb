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
import com.thamnis.thkb.engine.SuggestionCandidate
import com.thamnis.thkb.ime.AccentPopupState
import com.thamnis.thkb.ime.InputContext
import com.thamnis.thkb.ime.KeyPreviewState
import com.thamnis.thkb.ime.KeyboardLayer
import com.thamnis.thkb.ime.KeyboardStateLogic
import com.thamnis.thkb.ime.PanelMode
import com.thamnis.thkb.ime.ReturnAction
import com.thamnis.thkb.ime.ShiftState
import com.thamnis.thkb.ime.ThKbInputMethodService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val BgColor = Color(0xFF1B1C1E)
private val KeyBgColor = Color(0xFF3A3C41)
private val KeyShadowColor = Color(0xFF0E0F11)
private val KeyPressedColor = Color(0xFF5A5D64)
private val ActionKeyBgColor = Color(0xFF2B2D31)
private val PillKeyBgColor = Color(0xFF43474E)
private val ReturnBgColor = Color(0xFFA8C7FA)
private val ReturnFgColor = Color(0xFF062E6F)
private val HintTextColor = Color(0xFF9AA0A6)
private val ToolbarTextColor = Color(0xFFE3E3E3)

private data class ActivePointer(
    val keyId: String,
    val startPosition: Offset,
    var lastGlideX: Float,
    var lastGlideY: Float,
    var isGliding: Boolean = false,
    var deleteGestureTriggered: Boolean = false,
    var longPressJob: Job? = null,
    var repeatJob: Job? = null,
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
    var accentPopup by remember(inputSessionKey) { mutableStateOf<AccentPopupState?>(null) }
    var keyPreview by remember(inputSessionKey) { mutableStateOf<KeyPreviewState?>(null) }
    var isForwardDeleteMode by remember(inputSessionKey) { mutableStateOf(false) }
    var ctrlActive by remember(inputSessionKey) { mutableStateOf(false) }
    var scrollPanelHeightDp by remember { mutableStateOf(250f) }
    val density = LocalDensity.current
    var rootWidthPx by remember { mutableStateOf(1080f) }

    val canResizePanel = panelMode == PanelMode.EMOJI ||
        panelMode == PanelMode.KAOMOJI ||
        (panelMode == PanelMode.CLIPBOARD && clipboardEntries.size > 4) ||
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
                    if (panelMode != PanelMode.EMOJI && panelMode != PanelMode.KAOMOJI) {
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
                            onToggleEmoji = {
                                panelMode = PanelMode.EMOJI
                            },
                            onToggleClipboard = {
                                panelMode = if (panelMode == PanelMode.CLIPBOARD) PanelMode.KEYBOARD else PanelMode.CLIPBOARD
                            },
                            onToggleTodo = {
                                panelMode = if (panelMode == PanelMode.TODO) PanelMode.KEYBOARD else PanelMode.TODO
                            },
                            onOpenSettings = {
                                onKeyFeedback()
                                onOpenSettings()
                            },
                            suggestions = suggestions,
                            onSelectSuggestion = onSelectSuggestion,
                            onBlockSuggestion = onBlockSuggestion,
                            onUndo = onUndo,
                            onRedo = onRedo,
                        )
                    }

                    if (inputContext == InputContext.TERMINAL && panelMode == PanelMode.KEYBOARD) {
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
                            if (targetState == PanelMode.EMOJI || targetState == PanelMode.KAOMOJI) {
                                (slideInVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) { it / 4 } +
                                    fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.96f, animationSpec = tween(200)))
                                    .togetherWith(
                                        slideOutVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) { -it / 6 } +
                                            fadeOut(animationSpec = tween(140))
                                    )
                            } else if (initialState == PanelMode.EMOJI || initialState == PanelMode.KAOMOJI) {
                                (slideInVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) { -it / 6 } +
                                    fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.96f, animationSpec = tween(200)))
                                    .togetherWith(
                                        slideOutVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) { it / 4 } +
                                            fadeOut(animationSpec = tween(140))
                                    )
                            } else {
                                (fadeIn(animationSpec = tween(160)) + scaleIn(initialScale = 0.96f, animationSpec = tween(160)))
                                    .togetherWith(fadeOut(animationSpec = tween(120)))
                            }
                        },
                        label = "panelModeTransition",
                    ) { currentPanelMode ->
                        when (currentPanelMode) {
                            PanelMode.EMOJI, PanelMode.KAOMOJI -> {
                            EmojiAndKaomojiPanel(
                                panelHeightDp = (scrollPanelHeightDp + 44f).coerceIn(294f, 504f),
                                emojiGridColumns = emojiGridColumns,
                                emojiSections = emojiSections,
                                kaomojiItems = kaomojiItems,
                                favoriteItems = favoriteItems,
                                recentEmojis = recentEmojis,
                                onResizeDeltaDp = { deltaDp ->
                                    scrollPanelHeightDp = (scrollPanelHeightDp - deltaDp).coerceIn(250f, 460f)
                                },
                                onBackToKeyboard = {
                                    onKeyFeedback()
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
                        }
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
                        PanelMode.KEYBOARD -> {
                            TouchKeyGrid(
                                rows = rows,
                                layoutId = activeLayoutId,
                                isAlphaLayer = layer == KeyboardLayer.ALPHA,
                                rootWidthPx = rootWidthPx,
                                shiftState = shiftState,
                                isForwardDeleteMode = isForwardDeleteMode,
                                longPressDelayMs = longPressDelayMs,
                                rapidDeleteDelayMs = rapidDeleteDelayMs,
                                deleteGesturesEnabled = deleteGesturesEnabled,
                                returnAction = returnAction,
                                accentPopup = accentPopup,
                                assets = assets,
                                onShowAccentPopup = {
                                    accentPopup = it
                                    if (it != null) keyPreview = null
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
                                        onCommitText(fromAccent)
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
                                            if (isForwardDeleteMode) onForwardDelete() else onBackspace()
                                            if (shiftState == ShiftState.OFF && shouldAutoCapNow()) {
                                                shiftState = ShiftState.ONCE
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
                                        "EMOJI_KEY" -> panelMode = PanelMode.EMOJI
                                        "SPACE" -> {
                                            onCommitText(" ")
                                            if (resetForwardDeleteOnChar) isForwardDeleteMode = false
                                            if (shiftState == ShiftState.OFF && shouldAutoCapNow()) {
                                                shiftState = ShiftState.ONCE
                                            }
                                        }
                                        "RETURN" -> {
                                            onReturn()
                                            if (shiftState == ShiftState.OFF && shouldAutoCapNow()) {
                                                shiftState = ShiftState.ONCE
                                            }
                                        }
                                        else -> {
                                            if (ctrlActive && keyId.length == 1 && keyId[0].isLetter()) {
                                                onSendCtrlKey(keyId[0])
                                                ctrlActive = false
                                                return@TouchKeyGrid
                                            }
                                            val output = KeyboardStateLogic.resolveKeyLabel(
                                                rawKey = keyId,
                                                shiftState = shiftState,
                                                returnAction = returnAction,
                                            )
                                            onCommitText(output)
                                            shiftState = KeyboardStateLogic.shiftAfterCharacterCommit(shiftState)
                                            if (resetForwardDeleteOnChar) isForwardDeleteMode = false
                                        }
                                    }
                                },
                                onBackspaceRepeat = {
                                    if (isForwardDeleteMode) onForwardDelete() else onBackspace()
                                },
                                onDeleteWord = {
                                    onDeleteWord(isForwardDeleteMode)
                                },
                                onDeleteParagraph = {
                                    onDeleteParagraph(isForwardDeleteMode)
                                },
                                onToggleNumericMode = {
                                    layer = if (layer == KeyboardLayer.NUMERIC) {
                                        if (lastNonNumericLayer == KeyboardLayer.SYMBOLS || lastNonNumericLayer == KeyboardLayer.SYMBOLS_MORE) {
                                            KeyboardLayer.SYMBOLS
                                        } else {
                                            KeyboardLayer.ALPHA
                                        }
                                    } else {
                                        lastNonNumericLayer = layer
                                        KeyboardLayer.NUMERIC
                                    }
                                },
                                onMoveCursor = onMoveCursor,
                                onMoveCursorVertical = onMoveCursorVertical,
                            )
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
    onResizeDeltaDp: (Float) -> Unit,
    onToggleExpand: () -> Unit,
    onToggleEmoji: () -> Unit,
    onToggleClipboard: () -> Unit,
    onToggleTodo: () -> Unit,
    onOpenSettings: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    val density = LocalDensity.current
    val latestOnResize by rememberUpdatedState(onResizeDeltaDp)
    var showToolsOverride by remember { mutableStateOf(false) }

    LaunchedEffect(suggestions) {
        if (suggestions.isNotEmpty()) {
            showToolsOverride = false
        }
    }

    val isSuggestionMode = panelMode == PanelMode.KEYBOARD && suggestions.isNotEmpty() && !showToolsOverride

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
            if (isSuggestionMode) {
                // Suggestion Strip view
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = "Show Tools",
                    active = false,
                    onClick = { showToolsOverride = true },
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
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
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isCenter) Modifier.background(ActionKeyBgColor.copy(alpha = 0.55f)) else Modifier,
                                )
                                .combinedClickable(
                                    onClick = { onSelectSuggestion(candidate) },
                                    onLongClick = {
                                        if (!candidate.isLiteral) {
                                            onBlockSuggestion(candidate.word)
                                        }
                                    },
                                )
                                .padding(horizontal = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (candidate.isLiteral) "“${candidate.word}”" else candidate.word,
                                color = if (isCenter) Color.White else Color(0xFFDDDDDD),
                                fontSize = if (isCenter) 15.sp else 14.sp,
                                fontWeight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (panelMode == PanelMode.KEYBOARD && suggestions.isNotEmpty()) {
                        ToolbarIconButton(
                            icon = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Show Suggestions",
                            active = false,
                            onClick = { showToolsOverride = false },
                        )
                    }
                    ToolbarIconButton(
                        icon = if (panelMode == PanelMode.EMOJI || panelMode == PanelMode.KAOMOJI) {
                            Icons.Outlined.Keyboard
                        } else {
                            Icons.Outlined.EmojiEmotions
                        },
                        contentDescription = "Emoji",
                        active = panelMode == PanelMode.EMOJI || panelMode == PanelMode.KAOMOJI,
                        onClick = onToggleEmoji,
                    )
                    ToolbarIconButton(
                        icon = if (panelMode == PanelMode.CLIPBOARD) {
                            Icons.Outlined.Keyboard
                        } else {
                            Icons.Outlined.ContentPaste
                        },
                        contentDescription = "Clipboard",
                        active = panelMode == PanelMode.CLIPBOARD,
                        onClick = onToggleClipboard,
                    )
                    ToolbarIconButton(
                        icon = if (panelMode == PanelMode.TODO) {
                            Icons.Outlined.Keyboard
                        } else {
                            Icons.Outlined.Checklist
                        },
                        contentDescription = "Todo List",
                        active = panelMode == PanelMode.TODO,
                        onClick = onToggleTodo,
                    )
                    if (canResize) {
                        ToolbarIconButton(
                            icon = if (isExpanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
                            contentDescription = "Resize",
                            active = isExpanded,
                            onClick = onToggleExpand,
                        )
                    }
                    ToolbarIconButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        active = false,
                        onClick = onOpenSettings,
                    )
                }
            }

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
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val animatedBg by animateColorAsState(
        targetValue = if (active) ReturnBgColor else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "toolbarBtnBg",
    )
    val animatedTint by animateColorAsState(
        targetValue = if (active) ReturnFgColor else ToolbarTextColor,
        animationSpec = tween(durationMillis = 180),
        label = "toolbarBtnTint",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (active) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "toolbarBtnScale",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = animatedTint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
    }
}

@Composable
private fun CategoryChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val animatedBg by animateColorAsState(
        targetValue = if (active) ReturnBgColor else ActionKeyBgColor,
        animationSpec = tween(durationMillis = 180),
        label = "chipBg",
    )
    val animatedFg by animateColorAsState(
        targetValue = if (active) ReturnFgColor else ToolbarTextColor,
        animationSpec = tween(durationMillis = 180),
        label = "chipFg",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chipScale",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = animatedFg,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun ClipboardPanel(
    panelHeightDp: Float,
    entries: List<ClipboardEntry>,
    onCommitClip: (String) -> Unit,
    onSystemPaste: () -> Unit,
    onPinClip: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    onClearUnpinned: () -> Unit,
) {
    val contentHeight = (panelHeightDp - 40f).coerceAtLeast(180f).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryChip(
                label = "Paste from system",
                active = true,
                onClick = onSystemPaste,
            )
            if (entries.any { !it.pinned }) {
                CategoryChip(
                    label = "Clear unpinned",
                    active = false,
                    onClick = onClearUnpinned,
                )
            }
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Clipboard history is empty",
                    color = HintTextColor,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(entries, key = { it.text }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KeyBgColor)
                            .clickable { onCommitClip(entry.text) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = entry.text,
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (entry.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin",
                                tint = if (entry.pinned) ReturnBgColor else HintTextColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onPinClip(entry.text) },
                            )
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = HintTextColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onDeleteClip(entry.text) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoListPanel(
    panelHeightDp: Float,
    tasks: List<TodoTask>,
    onAddTask: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onCommitTaskText: (String) -> Unit,
    onKeyFeedback: () -> Unit,
) {
    val density = LocalDensity.current
    val contentHeight = (panelHeightDp - 48f).coerceAtLeast(180f).dp
    var isAddingTask by remember { mutableStateOf(false) }
    var newTaskText by remember { mutableStateOf("") }
    var lastInsertedTaskId by remember { mutableStateOf<String?>(null) }

    val completedCount = tasks.count { it.isCompleted }
    val totalCount = tasks.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "todoProgress",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Top action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Todo",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                if (totalCount > 0) {
                    Text(
                        text = "$completedCount/$totalCount",
                        color = if (completedCount == totalCount && totalCount > 0) Color(0xFF81C784) else HintTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AnimatedVisibility(
                    visible = completedCount > 0,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    CategoryChip(
                        label = "Clear done",
                        active = false,
                        onClick = {
                            onKeyFeedback()
                            onClearCompleted()
                        },
                    )
                }

                CategoryChip(
                    label = if (isAddingTask) "Close" else "+ Add task",
                    active = isAddingTask,
                    onClick = {
                        onKeyFeedback()
                        isAddingTask = !isAddingTask
                    },
                )
            }
        }

        // Animated progress bar
        if (totalCount > 0) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp)),
                color = Color(0xFF81C784),
                trackColor = KeyBgColor,
            )
        }

        // Animated inline new task composer
        AnimatedVisibility(
            visible = isAddingTask,
            enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(KeyBgColor)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(ReturnBgColor),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (newTaskText.isEmpty()) {
                            Text(
                                text = "Enter new task...",
                                color = HintTextColor,
                                fontSize = 14.sp,
                            )
                        }
                        innerTextField()
                    },
                )

                val submitScale by animateFloatAsState(
                    targetValue = if (newTaskText.isNotBlank()) 1.1f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "submitScale",
                )

                Box(
                    modifier = Modifier
                        .scale(submitScale)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (newTaskText.isNotBlank()) ReturnBgColor else ActionKeyBgColor)
                        .clickable(enabled = newTaskText.isNotBlank()) {
                            onKeyFeedback()
                            onAddTask(newTaskText.trim())
                            newTaskText = ""
                            isAddingTask = false
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Add",
                        color = if (newTaskText.isNotBlank()) ReturnFgColor else HintTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // List of tasks
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Checklist,
                        contentDescription = null,
                        tint = HintTextColor,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = "No tasks yet. Tap '+ Add task' to create one!",
                        color = HintTextColor,
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(tasks, key = { it.id }) { task ->
                    TodoItemRow(
                        task = task,
                        isJustInserted = lastInsertedTaskId == task.id,
                        onToggle = {
                            onKeyFeedback()
                            onToggleTask(task.id)
                        },
                        onDelete = {
                            onKeyFeedback()
                            onDeleteTask(task.id)
                        },
                        onCommit = {
                            onKeyFeedback()
                            lastInsertedTaskId = task.id
                            onCommitTaskText(task.title)
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoItemRow(
    task: TodoTask,
    isJustInserted: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textColor by animateColorAsState(
        targetValue = if (task.isCompleted) HintTextColor else Color.White,
        animationSpec = tween(durationMillis = 250),
        label = "textColor",
    )
    val strikeProgress by animateFloatAsState(
        targetValue = if (task.isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "strikeProgress",
    )
    val cardBgColor by animateColorAsState(
        targetValue = if (isJustInserted) ActionKeyBgColor else KeyBgColor,
        animationSpec = tween(durationMillis = 200),
        label = "cardBgColor",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardBgColor)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Animated Checkbox
        Box(
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = task.isCompleted,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 50)) +
                        scaleIn(initialScale = 0.6f, animationSpec = tween(220, delayMillis = 50)))
                        .togetherWith(fadeOut(animationSpec = tween(90)) + scaleOut(targetScale = 0.6f))
                },
                label = "checkIcon",
            ) { completed ->
                Icon(
                    imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (completed) "Completed" else "Incomplete",
                    tint = if (completed) Color(0xFF81C784) else HintTextColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Animated Text with Strikethrough
        Text(
            text = task.title,
            color = textColor,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onCommit)
                .drawWithContent {
                    drawContent()
                    if (strikeProgress > 0f) {
                        val y = size.height / 2f
                        drawLine(
                            color = HintTextColor,
                            start = Offset(0f, y),
                            end = Offset(size.width * strikeProgress, y),
                            strokeWidth = 1.8f * density.density,
                            cap = StrokeCap.Round,
                        )
                    }
                },
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Action Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Paste into text icon
            Icon(
                imageVector = Icons.Outlined.ContentPaste,
                contentDescription = "Paste into text",
                tint = if (isJustInserted) Color(0xFF81C784) else HintTextColor,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onCommit),
            )

            // Delete icon
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete task",
                tint = HintTextColor,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onDelete),
            )
        }
    }
}

@Composable
private fun EmojiAndKaomojiPanel(
    panelHeightDp: Float,
    emojiGridColumns: Int,
    emojiSections: List<EmojiSection>,
    kaomojiItems: List<KaomojiItem>,
    favoriteItems: Set<String>,
    recentEmojis: List<String>,
    onResizeDeltaDp: (Float) -> Unit,
    onBackToKeyboard: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCommitEmoji: (String) -> Unit,
    onCommitKaomoji: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackspaceRepeat: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Emojis, 1 = Kaomoji :-), 2 = Favorites ★

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val latestOnResize by rememberUpdatedState(onResizeDeltaDp)
    val latestOnBackspace by rememberUpdatedState(onBackspace)
    val latestOnBackspaceRepeat by rememberUpdatedState(onBackspaceRepeat)

    val recentList = remember(recentEmojis, favoriteItems) {
        val base = (recentEmojis + favoriteItems.filter { it.length <= 3 }).distinct()
        if (base.isNotEmpty()) base else listOf("😂", "❤️", "👍", "🔥", "🙏", "😍", "🥳", "✅")
    }
    val allEmojiSections = remember(recentList, emojiSections) {
        listOf(EmojiSection(category = "Recent", emojis = recentList)) + emojiSections
    }

    val emojiGridState = rememberLazyGridState()
    val categoryScrollState = rememberScrollState()

    // Section start indices in the grid: 1 header item + N emojis per section
    val sectionStartIndices = remember(allEmojiSections) {
        var acc = 0
        allEmojiSections.map { sec ->
            val start = acc
            acc += 1 + sec.emojis.size
            start
        }
    }

    // Two-way synchronization: as user scrolls the emoji grid, calculate visible section
    val currentVisibleSectionIdx by remember {
        derivedStateOf {
            val firstVisible = emojiGridState.firstVisibleItemIndex
            val idx = sectionStartIndices.indexOfLast { it <= firstVisible }
            if (idx >= 0) idx else 0
        }
    }

    // Auto-scroll the top category bar so the active icon stays in view
    LaunchedEffect(currentVisibleSectionIdx) {
        val targetScrollPx = with(density) { (currentVisibleSectionIdx * 42).dp.toPx() }.toInt()
        categoryScrollState.animateScrollTo(targetScrollPx)
    }

    val kaomojiCategories = remember(kaomojiItems) { listOf("all") + kaomojiItems.map { it.category }.distinct() }
    var selectedKaomojiCat by remember { mutableStateOf("all") }

    val contentHeight = (panelHeightDp - 96f).coerceAtLeast(180f).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp),
    ) {
        // Top Bar: Back arrow + Symbol Category Icons (plus vertical drag-to-resize)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaDp = with(density) { dragAmount.toDp().value }
                        latestOnResize(deltaDp)
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 3.dp)
                    .size(width = 32.dp, height = 3.dp)
                    .background(HintTextColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Circular Back button to keyboard
                var backPressed by remember { mutableStateOf(false) }
                val backScale by animateFloatAsState(
                    targetValue = if (backPressed) 0.88f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "emojiBackScale",
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer {
                            scaleX = backScale
                            scaleY = backScale
                        }
                        .clip(CircleShape)
                        .background(ActionKeyBgColor)
                        .clickable {
                            backPressed = true
                            onBackToKeyboard()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = ToolbarTextColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Category Symbol Icons row (takes full remaining width, horizontally scrollable)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(categoryScrollState),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (selectedTab == 0) {
                        allEmojiSections.forEachIndexed { idx, section ->
                            val active = currentVisibleSectionIdx == idx
                            val catBg by animateColorAsState(
                                targetValue = if (active) ReturnBgColor else Color.Transparent,
                                animationSpec = tween(120),
                                label = "catBgAnim",
                            )
                            val catFg by animateColorAsState(
                                targetValue = if (active) ReturnFgColor else HintTextColor,
                                animationSpec = tween(120),
                                label = "catFgAnim",
                            )
                            val catScale by animateFloatAsState(
                                targetValue = if (active) 1.08f else 1.0f,
                                label = "catScaleAnim",
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer {
                                        scaleX = catScale
                                        scaleY = catScale
                                    }
                                    .clip(CircleShape)
                                    .background(catBg)
                                    .clickable {
                                        scope.launch {
                                            emojiGridState.scrollToItem(sectionStartIndices[idx])
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = emojiCategoryIconFor(section.category),
                                    contentDescription = section.category,
                                    tint = catFg,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    } else if (selectedTab == 1) {
                        kaomojiCategories.forEach { cat ->
                            val active = selectedKaomojiCat == cat
                            val catBg by animateColorAsState(
                                targetValue = if (active) ReturnBgColor else Color.Transparent,
                                animationSpec = tween(120),
                                label = "kaomojiCatBgAnim",
                            )
                            val catFg by animateColorAsState(
                                targetValue = if (active) ReturnFgColor else HintTextColor,
                                animationSpec = tween(120),
                                label = "kaomojiCatFgAnim",
                            )
                            val catScale by animateFloatAsState(
                                targetValue = if (active) 1.08f else 1.0f,
                                label = "kaomojiCatScaleAnim",
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer {
                                        scaleX = catScale
                                        scaleY = catScale
                                    }
                                    .clip(CircleShape)
                                    .background(catBg)
                                    .clickable { selectedKaomojiCat = cat },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = kaomojiCategoryIconFor(cat),
                                    contentDescription = cat,
                                    tint = catFg,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Middle Scrollable Grid with animated horizontal slide transitions
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { direction * it / 3 } +
                    fadeIn(animationSpec = tween(200)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { -direction * it / 3 } +
                            fadeOut(animationSpec = tween(160))
                    )
            },
            modifier = Modifier.weight(1f),
            label = "emojiTabContentTransition",
        ) { currentTab ->
            when (currentTab) {
                0 -> {
                    NativeEmojiGridContent(
                        contentHeight = contentHeight,
                        gridColumns = emojiGridColumns,
                        sections = allEmojiSections,
                        gridState = emojiGridState,
                        onSelect = onCommitEmoji,
                    )
                }
                1 -> {
                    KaomojiGridContent(
                        contentHeight = contentHeight,
                        items = kaomojiItems,
                        selectedCategory = selectedKaomojiCat,
                        favoriteItems = favoriteItems,
                        onToggleFavorite = onToggleFavorite,
                        onSelect = onCommitKaomoji,
                    )
                }
                else -> {
                    FavoritesGridContent(
                        contentHeight = contentHeight,
                        favoriteItems = favoriteItems.toList(),
                        recentEmojis = recentEmojis,
                        onToggleFavorite = onToggleFavorite,
                        onSelect = onCommitKaomoji,
                    )
                }
            }
        }

        // Bottom Bar: ABC | ☺ (Emojis) | ★ (Favorites) | :-) (Kaomoji) | ⌫ (Backspace)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            var abcPressed by remember { mutableStateOf(false) }
            val abcScale by animateFloatAsState(
                targetValue = if (abcPressed) 0.88f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "abcBtnScale",
            )
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = abcScale
                        scaleY = abcScale
                    }
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        abcPressed = true
                        onBackToKeyboard()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "ABC",
                    color = ToolbarTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Emoji tab pill (☺)
                val isTab0 = selectedTab == 0
                val tab0Width by animateDpAsState(
                    targetValue = if (isTab0) 68.dp else 48.dp,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
                    label = "tab0W",
                )
                val tab0Bg by animateColorAsState(
                    targetValue = if (isTab0) ReturnBgColor else ActionKeyBgColor,
                    animationSpec = tween(140),
                    label = "tab0Bg",
                )
                val tab0Fg by animateColorAsState(
                    targetValue = if (isTab0) ReturnFgColor else ToolbarTextColor,
                    animationSpec = tween(140),
                    label = "tab0Fg",
                )
                Box(
                    modifier = Modifier
                        .size(width = tab0Width, height = 36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(tab0Bg)
                        .clickable { selectedTab = 0 },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SentimentSatisfied,
                        contentDescription = "Emojis",
                        tint = tab0Fg,
                        modifier = Modifier.size(22.dp),
                    )
                }

                // Favorites tab pill (★)
                val isTab2 = selectedTab == 2
                val tab2Width by animateDpAsState(
                    targetValue = if (isTab2) 68.dp else 48.dp,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
                    label = "tab2W",
                )
                val tab2Bg by animateColorAsState(
                    targetValue = if (isTab2) ReturnBgColor else ActionKeyBgColor,
                    animationSpec = tween(140),
                    label = "tab2Bg",
                )
                val tab2Fg by animateColorAsState(
                    targetValue = if (isTab2) ReturnFgColor else ToolbarTextColor,
                    animationSpec = tween(140),
                    label = "tab2Fg",
                )
                Box(
                    modifier = Modifier
                        .size(width = tab2Width, height = 36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(tab2Bg)
                        .clickable { selectedTab = 2 },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isTab2) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorites",
                        tint = tab2Fg,
                        modifier = Modifier.size(21.dp),
                    )
                }

                // Kaomoji tab pill (:-))
                val isTab1 = selectedTab == 1
                val tab1Width by animateDpAsState(
                    targetValue = if (isTab1) 68.dp else 48.dp,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
                    label = "tab1W",
                )
                val tab1Bg by animateColorAsState(
                    targetValue = if (isTab1) ReturnBgColor else ActionKeyBgColor,
                    animationSpec = tween(140),
                    label = "tab1Bg",
                )
                val tab1Fg by animateColorAsState(
                    targetValue = if (isTab1) ReturnFgColor else ToolbarTextColor,
                    animationSpec = tween(140),
                    label = "tab1Fg",
                )
                Box(
                    modifier = Modifier
                        .size(width = tab1Width, height = 36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(tab1Bg)
                        .clickable { selectedTab = 1 },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ":-)",
                        color = tab1Fg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Right Backspace icon with animated press and repeat
            var backspacePressed by remember { mutableStateOf(false) }
            val backspaceScale by animateFloatAsState(
                targetValue = if (backspacePressed) 0.88f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "emojiBkspScale",
            )
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 38.dp)
                    .graphicsLayer {
                        scaleX = backspaceScale
                        scaleY = backspaceScale
                    }
                    .clip(RoundedCornerShape(50))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                            down.consume()
                            backspacePressed = true
                            latestOnBackspace()
                            val repeatJob = scope.launch {
                                delay(380L)
                                while (true) {
                                    latestOnBackspaceRepeat()
                                    delay(50L)
                                }
                            }
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.pressed }) break
                            }
                            backspacePressed = false
                            repeatJob.cancel()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Backspace",
                    tint = ToolbarTextColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private fun emojiCategoryIconFor(category: String): ImageVector = when {
    category.startsWith("Recent") -> Icons.Outlined.Schedule
    category.startsWith("Smileys") -> Icons.Outlined.SentimentSatisfied
    category.startsWith("Hands") -> Icons.Outlined.EmojiPeople
    category.startsWith("Animals") -> Icons.Outlined.EmojiNature
    category.startsWith("Food") -> Icons.Outlined.EmojiFoodBeverage
    category.startsWith("Travel") -> Icons.Outlined.DirectionsCar
    category.startsWith("Activities") -> Icons.Outlined.EmojiEvents
    category.startsWith("Symbols") -> Icons.Outlined.EmojiSymbols
    category.startsWith("Flags") -> Icons.Outlined.Flag
    else -> Icons.Outlined.EmojiSymbols
}

private fun kaomojiCategoryIconFor(category: String): ImageVector = when (category) {
    "happy" -> Icons.Outlined.SentimentVerySatisfied
    "love" -> Icons.Outlined.FavoriteBorder
    "shrug" -> Icons.Outlined.SentimentNeutral
    "sad" -> Icons.Outlined.SentimentDissatisfied
    "angry" -> Icons.Outlined.Whatshot
    else -> Icons.Outlined.Apps
}

@Composable
private fun NativeEmojiGridContent(
    contentHeight: Dp,
    gridColumns: Int,
    sections: List<EmojiSection>,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onSelect: (String) -> Unit,
) {
    val emojiFontSize = when {
        gridColumns <= 6 -> 30.sp
        gridColumns == 7 -> 27.sp
        gridColumns == 8 -> 24.sp
        else -> 21.sp
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(gridColumns.coerceIn(5, 10)),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight)
            .padding(horizontal = 6.dp),
    ) {
        sections.forEach { section ->
            item(
                key = "header_${section.category}",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                Text(
                    text = section.category,
                    color = HintTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
            items(
                items = section.emojis,
                key = { emoji -> "${section.category}_$emoji" },
            ) { emoji ->
                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = emoji,
                        fontSize = emojiFontSize,
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesGridContent(
    contentHeight: Dp,
    favoriteItems: List<String>,
    recentEmojis: List<String>,
    onToggleFavorite: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    val combined = remember(favoriteItems, recentEmojis) {
        (favoriteItems + recentEmojis.filterNot { it in favoriteItems }).distinct()
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 86.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        items(combined) { item ->
            val isFav = item in favoriteItems
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(KeyBgColor)
                    .clickable { onSelect(item) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item,
                    color = Color.White,
                    fontSize = if (item.length <= 2) 20.sp else 13.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFav) ReturnBgColor else HintTextColor,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onToggleFavorite(item) },
                )
            }
        }
    }
}

@Composable
private fun KaomojiGridContent(
    contentHeight: Dp,
    items: List<KaomojiItem>,
    selectedCategory: String,
    favoriteItems: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    val filtered = remember(items, selectedCategory) {
        if (selectedCategory == "all") items else items.filter { it.category == selectedCategory }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        items(filtered) { item ->
            val isFav = item.text in favoriteItems
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(KeyBgColor)
                    .clickable { onSelect(item.text) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.text,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFav) ReturnBgColor else HintTextColor,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onToggleFavorite(item.text) },
                )
            }
        }
    }
}

@Composable
private fun TouchKeyGrid(
    rows: List<List<String>>,
    layoutId: String,
    isAlphaLayer: Boolean,
    rootWidthPx: Float,
    shiftState: ShiftState,
    isForwardDeleteMode: Boolean,
    longPressDelayMs: Long,
    rapidDeleteDelayMs: Long,
    deleteGesturesEnabled: Boolean,
    returnAction: ReturnAction,
    accentPopup: AccentPopupState?,
    assets: AssetRepository,
    onShowAccentPopup: (AccentPopupState?) -> Unit,
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
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val glideThresholdPx = with(density) { 12.dp.toPx() }
    val glideStepPx = with(density) { 14.dp.toPx() }
    val glideVerticalStepPx = with(density) { 18.dp.toPx() }
    val cancelPopupMovePx = with(density) { 14.dp.toPx() }
    val deleteSwipeThresholdPx = with(density) { 18.dp.toPx() }
    val wordSwipeStepPx = with(density) { 24.dp.toPx() }

    val keyBounds = remember(rows) { mutableMapOf<String, Rect>() }
    val pressedKeys = remember(rows) { mutableStateMapOf<String, Boolean>() }
    val activePointers = remember(rows) { mutableMapOf<PointerId, ActivePointer>() }
    var gridCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val latestShift by rememberUpdatedState(shiftState)
    val latestPopup by rememberUpdatedState(accentPopup)
    val latestIsAlphaLayer by rememberUpdatedState(isAlphaLayer)
    val latestLayoutId by rememberUpdatedState(layoutId)
    val latestRootWidthPx by rememberUpdatedState(rootWidthPx)
    val latestLongPressDelayMs by rememberUpdatedState(longPressDelayMs)
    val latestRapidDeleteDelayMs by rememberUpdatedState(rapidDeleteDelayMs)
    val latestDeleteGesturesEnabled by rememberUpdatedState(deleteGesturesEnabled)
    val latestOnShowAccentPopup by rememberUpdatedState(onShowAccentPopup)
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
                                    "SHIFT", "SPACE", "SYMBOLS", "MORE_SYMBOLS", "ALPHA", "EMOJI_KEY", "RETURN" -> {
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
                                            val baseChar = if (latestShift != ShiftState.OFF) {
                                                hitKey.uppercase()
                                            } else {
                                                hitKey
                                            }
                                            val hint = if (latestIsAlphaLayer) {
                                                KeyboardStateLogic.hintForKey(latestLayoutId, hitKey)
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
                                        pointer.lastGlideX = localPos.x
                                        pointer.lastGlideY = localPos.y
                                    }
                                    if (pointer.isGliding) {
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
                                } else if (pointer.keyId == "BACKSPACE" && latestDeleteGesturesEnabled) {
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
                                } else if (pointer.keyId in setOf("SYMBOLS", "MORE_SYMBOLS", "ALPHA", "NUMERIC")) {
                                    if (!pointer.deleteGestureTriggered && totalDy < -deleteSwipeThresholdPx && abs(totalDy) > abs(totalDx)) {
                                        pointer.deleteGestureTriggered = true
                                        pointer.longPressJob?.cancel()
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
                                    } else if (abs(totalDx) > cancelPopupMovePx || abs(totalDy) > cancelPopupMovePx) {
                                        // Cancel pending long-press popup if finger glides before delay
                                        pointer.longPressJob?.cancel()
                                        if (totalDx < -deleteSwipeThresholdPx) {
                                            pointer.isGliding = true
                                        }
                                    }
                                }
                                change.consume()
                            } else if (!change.pressed && change.previousPressed) {
                                val pointer = activePointers.remove(id) ?: continue
                                pointer.longPressJob?.cancel()
                                pointer.repeatJob?.cancel()
                                pressedKeys.remove(pointer.keyId)
                                latestOnShowKeyPreview(null)

                                val popup = latestPopup
                                if (popup != null) {
                                    val chosen = popup.variants[popup.selectedIndex]
                                    latestOnShowAccentPopup(null)
                                    latestOnKeyAction(pointer.keyId, chosen)
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
                            keyId == "SYMBOLS" -> "123"
                            keyId == "NUMERIC" -> "?#"
                            keyId == "ALPHA" && layoutId == "numeric" -> "ABC"
                            keyId == "ALPHA" && !isAlphaLayer -> "123"
                            isAlphaLayer -> KeyboardStateLogic.hintForKey(layoutId, keyId)
                            else -> null
                        }

                        KeyCap(
                            keyId = keyId,
                            label = KeyboardStateLogic.resolveKeyLabel(keyId, shiftState, returnAction),
                            shiftState = shiftState,
                            isForwardDeleteKey = isForwardDeleteKey,
                            isPressed = isPressed,
                            isSpaceGliding = isSpaceGliding,
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
    isSpaceGliding: Boolean,
    returnAction: ReturnAction,
    icon: ImageVector?,
    hint: String?,
    weight: Float,
    shape: Shape,
    backgroundColor: Color,
    foregroundColor: Color,
    onBoundsChanged: (LayoutCoordinates) -> Unit,
) {
    val keyScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else if (isSpaceGliding) 1.03f else 1.0f,
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

    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = keyScale
                scaleY = keyScale
            }
            .onGloballyPositioned(onBoundsChanged)
            .background(KeyShadowColor, shape)
            .padding(bottom = 1.5.dp)
            .background(animatedBg, shape),
    ) {
        if (hint != null) {
            Text(
                text = hint,
                color = HintTextColor,
                fontSize = 10.sp,
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
                Text(
                    text = "‹   $label   ›",
                    color = ReturnBgColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.Center),
                )
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
    rowIndex == 3 && keyId in setOf("SYMBOLS", "ALPHA", "NUMERIC", "RETURN")

private fun isLessRoundedControlKey(keyId: String, rowIndex: Int): Boolean =
    keyId in setOf("SHIFT", "MORE_SYMBOLS", "BACKSPACE") || (rowIndex == 2 && keyId == "SYMBOLS")

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

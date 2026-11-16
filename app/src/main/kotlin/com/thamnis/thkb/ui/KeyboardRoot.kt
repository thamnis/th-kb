package com.thamnis.thkb.ui

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import com.thamnis.thkb.ime.AccentPopupState
import com.thamnis.thkb.ime.InputContext
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
    longPressDelayMs: Long = 450L,
    rapidDeleteDelayMs: Long = 580L,
    deleteGesturesEnabled: Boolean = true,
    resetForwardDeleteOnChar: Boolean = false,
    emojiGridColumns: Int = 7,
    returnAction: ReturnAction,
    assets: AssetRepository,
    clipboardEntries: List<ClipboardEntry> = emptyList(),
    favoriteItems: Set<String> = emptySet(),
    recentEmojis: List<String> = emptyList(),
    onToggleFavorite: (String) -> Unit = {},
    onRecordRecentEmoji: (String) -> Unit = {},
    onPinClip: (String) -> Unit = {},
    onDeleteClip: (String) -> Unit = {},
    onClearUnpinnedClips: () -> Unit = {},
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
) {
    var shiftState by remember(inputSessionKey, autoCapOnOpen) {
        mutableStateOf(if (autoCapOnOpen) ShiftState.ONCE else ShiftState.OFF)
    }
    var lastShiftTapMs by remember(inputSessionKey) { mutableStateOf(0L) }
    var layer by remember(inputSessionKey, inputContext) {
        mutableStateOf(KeyboardStateLogic.initialLayerForContext(inputContext))
    }
    var panelMode by remember(inputSessionKey) { mutableStateOf(PanelMode.KEYBOARD) }
    var accentPopup by remember(inputSessionKey) { mutableStateOf<AccentPopupState?>(null) }
    var isForwardDeleteMode by remember(inputSessionKey) { mutableStateOf(false) }
    var ctrlActive by remember(inputSessionKey) { mutableStateOf(false) }
    var scrollPanelHeightDp by remember { mutableStateOf(250f) }
    val density = LocalDensity.current
    var rootWidthPx by remember { mutableStateOf(1080f) }

    val canResizePanel = panelMode == PanelMode.EMOJI ||
        panelMode == PanelMode.KAOMOJI ||
        (panelMode == PanelMode.CLIPBOARD && clipboardEntries.size > 4)

    val activeLayoutId = when (layer) {
        KeyboardLayer.ALPHA -> layoutId
        KeyboardLayer.SYMBOLS -> "symbols"
        KeyboardLayer.SYMBOLS_MORE -> "symbols_more"
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
                    ToolbarRow(
                        panelMode = panelMode,
                        canResize = canResizePanel,
                        isExpanded = canResizePanel && scrollPanelHeightDp > 300f,
                        onResizeDeltaDp = { deltaDp ->
                            scrollPanelHeightDp = (scrollPanelHeightDp - deltaDp).coerceIn(236f, 460f)
                        },
                        onToggleExpand = {
                            scrollPanelHeightDp = if (scrollPanelHeightDp > 300f) 250f else 420f
                        },
                        onToggleEmoji = {
                            panelMode = if (panelMode == PanelMode.EMOJI) PanelMode.KEYBOARD else PanelMode.EMOJI
                        },
                        onToggleClipboard = {
                            panelMode = if (panelMode == PanelMode.CLIPBOARD) PanelMode.KEYBOARD else PanelMode.CLIPBOARD
                        },
                        onOpenSettings = {
                            onKeyFeedback()
                            onOpenSettings()
                        },
                    )

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

                    when (panelMode) {
                        PanelMode.EMOJI, PanelMode.KAOMOJI -> {
                            EmojiAndKaomojiPanel(
                                panelHeightDp = scrollPanelHeightDp,
                                emojiGridColumns = emojiGridColumns,
                                emojiSections = emojiSections,
                                kaomojiItems = kaomojiItems,
                                favoriteItems = favoriteItems,
                                recentEmojis = recentEmojis,
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
                                onShowAccentPopup = { accentPopup = it },
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
                                        "SYMBOLS" -> layer = KeyboardLayer.SYMBOLS
                                        "MORE_SYMBOLS" -> layer = KeyboardLayer.SYMBOLS_MORE
                                        "ALPHA" -> layer = KeyboardLayer.ALPHA
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
                                onMoveCursor = onMoveCursor,
                                onMoveCursorVertical = onMoveCursorVertical,
                            )
                        }
                    }
                }
            }
        }

        val popup = accentPopup
        if (popup != null && panelMode == PanelMode.KEYBOARD) {
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
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    popupRows.reversed().forEachIndexed { revRowIdx, rowItems ->
                        val actualRowIdx = popupRows.lastIndex - revRowIdx
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            rowItems.forEachIndexed { colIdx, variant ->
                                val flatIndex = actualRowIdx * cols + colIdx
                                val selected = flatIndex == popup.selectedIndex
                                Box(
                                    modifier = Modifier
                                        .size(cellDp)
                                        .background(
                                            color = if (selected) ReturnBgColor else Color.Transparent,
                                            shape = CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = variant,
                                        color = if (selected) ReturnFgColor else Color.White,
                                        fontSize = if (cols > 5) 18.sp else 20.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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

@Composable
private fun ToolbarRow(
    panelMode: PanelMode,
    canResize: Boolean,
    isExpanded: Boolean,
    onResizeDeltaDp: (Float) -> Unit,
    onToggleExpand: () -> Unit,
    onToggleEmoji: () -> Unit,
    onToggleClipboard: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val density = LocalDensity.current
    val latestOnResize by rememberUpdatedState(onResizeDeltaDp)

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
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) ReturnBgColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) ReturnFgColor else ToolbarTextColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CategoryChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) ReturnBgColor else ActionKeyBgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) ReturnFgColor else ToolbarTextColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
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
private fun EmojiAndKaomojiPanel(
    panelHeightDp: Float,
    emojiGridColumns: Int,
    emojiSections: List<EmojiSection>,
    kaomojiItems: List<KaomojiItem>,
    favoriteItems: Set<String>,
    recentEmojis: List<String>,
    onToggleFavorite: (String) -> Unit,
    onCommitEmoji: (String) -> Unit,
    onCommitKaomoji: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackspaceRepeat: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Emojis, 1 = Kaomoji, 2 = Favorites
    val scope = rememberCoroutineScope()
    val latestOnBackspace by rememberUpdatedState(onBackspace)
    val latestOnBackspaceRepeat by rememberUpdatedState(onBackspaceRepeat)
    val contentHeight = (panelHeightDp - 36f).coerceAtLeast(180f).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryChip(
                    label = "Emojis",
                    active = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                )
                CategoryChip(
                    label = "Kaomoji (^_^)",
                    active = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                )
                CategoryChip(
                    label = "★ Favorites",
                    active = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                )
            }

            when (selectedTab) {
                0 -> {
                    NativeEmojiGridContent(
                        contentHeight = contentHeight,
                        gridColumns = emojiGridColumns,
                        sections = emojiSections,
                        onSelect = onCommitEmoji,
                    )
                }
                1 -> {
                    KaomojiGridContent(
                        contentHeight = contentHeight,
                        items = kaomojiItems,
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

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 10.dp)
                .size(width = 58.dp, height = 44.dp)
                .background(KeyShadowColor, RoundedCornerShape(10.dp))
                .padding(bottom = 1.5.dp)
                .background(PillKeyBgColor, RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                        down.consume()
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
                        repeatJob.cancel()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Backspace,
                contentDescription = "Backspace",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun NativeEmojiGridContent(
    contentHeight: Dp,
    gridColumns: Int,
    sections: List<EmojiSection>,
    onSelect: (String) -> Unit,
) {
    val categories = remember(sections) { listOf("All") + sections.map { it.category } }
    var selectedCategory by remember { mutableStateOf("All") }
    val visibleSections = remember(sections, selectedCategory) {
        if (selectedCategory == "All") sections else sections.filter { it.category == selectedCategory }
    }
    val emojiFontSize = when {
        gridColumns <= 6 -> 28.sp
        gridColumns == 7 -> 25.sp
        gridColumns == 8 -> 22.sp
        else -> 20.sp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            categories.forEach { cat ->
                CategoryChip(
                    label = cat,
                    active = cat == selectedCategory,
                    onClick = { selectedCategory = cat },
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns.coerceIn(5, 10)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            visibleSections.forEach { section ->
                item(
                    key = "header_${section.category}",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    Text(
                        text = section.category,
                        color = ToolbarTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
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
    favoriteItems: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    val categories = remember(items) { listOf("all") + items.map { it.category }.distinct() }
    var selectedCategory by remember { mutableStateOf("all") }
    val filtered = remember(items, selectedCategory) {
        if (selectedCategory == "all") items else items.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            categories.forEach { cat ->
                CategoryChip(
                    label = cat,
                    active = cat == selectedCategory,
                    onClick = { selectedCategory = cat },
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
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
    onToggleForwardDeleteMode: () -> Unit,
    onKeyFeedback: () -> Unit,
    onKeyAction: (String, String?) -> Unit,
    onBackspaceRepeat: () -> Unit,
    onDeleteWord: () -> Unit,
    onDeleteParagraph: () -> Unit,
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
    val latestOnToggleForwardDelete by rememberUpdatedState(onToggleForwardDeleteMode)
    val latestOnKeyAction by rememberUpdatedState(onKeyAction)
    val latestOnKeyFeedback by rememberUpdatedState(onKeyFeedback)
    val latestOnBackspaceRepeat by rememberUpdatedState(onBackspaceRepeat)
    val latestOnDeleteWord by rememberUpdatedState(onDeleteWord)
    val latestOnDeleteParagraph by rememberUpdatedState(onDeleteParagraph)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (centerNineKeyRow) {
                        Spacer(modifier = Modifier.weight(0.5f))
                    }
                    row.forEach { keyId ->
                        val weight = keyWeightFor(keyId)
                        val isPill = isPillKey(keyId, rowIndex)
                        val isLessRoundedControl = isLessRoundedControlKey(keyId, rowIndex)
                        val isAction = isActionKey(keyId)
                        val isPressed = pressedKeys[keyId] == true
                        val isReturn = keyId == "RETURN"
                        val isShiftActive = keyId == "SHIFT" && shiftState != ShiftState.OFF
                        val isForwardDeleteKey = keyId == "BACKSPACE" && isForwardDeleteMode

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
                        val hint = if (isAlphaLayer) KeyboardStateLogic.hintForKey(layoutId, keyId) else null

                        KeyCap(
                            keyId = keyId,
                            label = KeyboardStateLogic.resolveKeyLabel(keyId, shiftState, returnAction),
                            shiftState = shiftState,
                            isForwardDeleteKey = isForwardDeleteKey,
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
    icon: ImageVector?,
    hint: String?,
    weight: Float,
    shape: Shape,
    backgroundColor: Color,
    foregroundColor: Color,
    onBoundsChanged: (LayoutCoordinates) -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .height(52.dp)
            .onGloballyPositioned(onBoundsChanged)
            .background(KeyShadowColor, shape)
            .padding(bottom = 1.5.dp)
            .background(backgroundColor, shape),
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
            icon != null -> {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = foregroundColor,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.Center)
                        .graphicsLayer(scaleX = if (isForwardDeleteKey) -1f else 1f),
                )
            }
            else -> {
                Text(
                    text = label,
                    color = foregroundColor,
                    fontSize = when (keyId) {
                        "SPACE", "RETURN", "SYMBOLS", "MORE_SYMBOLS", "ALPHA" -> 14.sp
                        else -> 21.sp
                    },
                    fontWeight = if (isActionKey(keyId)) FontWeight.Medium else FontWeight.Normal,
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
    Canvas(modifier = modifier) {
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
        if (shiftState == ShiftState.CAPS_LOCK) {
            drawRoundRect(
                color = tint,
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

private fun keyWeightFor(keyId: String): Float = when (keyId) {
    "SPACE" -> 4.0f
    "SHIFT", "BACKSPACE", "SYMBOLS", "MORE_SYMBOLS", "ALPHA", "RETURN" -> 1.5f
    else -> 1f
}

private fun isPillKey(keyId: String, rowIndex: Int): Boolean =
    rowIndex == 3 && keyId in setOf("SYMBOLS", "ALPHA", "RETURN")

private fun isLessRoundedControlKey(keyId: String, rowIndex: Int): Boolean =
    keyId in setOf("SHIFT", "MORE_SYMBOLS", "BACKSPACE") || (rowIndex == 2 && keyId == "SYMBOLS")

private fun isActionKey(keyId: String): Boolean = keyId in setOf(
    "SHIFT",
    "BACKSPACE",
    "SYMBOLS",
    "MORE_SYMBOLS",
    "ALPHA",
    "RETURN",
    ",",
    ".",
    "EMOJI_KEY",
)

package com.thamnis.thkb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.emoji2.emojipicker.EmojiPickerView
import com.thamnis.thkb.data.AssetRepository
import com.thamnis.thkb.data.KaomojiItem
import com.thamnis.thkb.ime.AccentPopupState
import com.thamnis.thkb.ime.KeyboardLayer
import com.thamnis.thkb.ime.KeyboardStateLogic
import com.thamnis.thkb.ime.PanelMode
import com.thamnis.thkb.ime.ReturnAction
import com.thamnis.thkb.ime.ShiftState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BgColor = Color(0xFF1B1C1E)
private val KeyBgColor = Color(0xFF34363A)
private val KeyPressedColor = Color(0xFF52555A)
private val ActionKeyBgColor = Color(0xFF282A2E)
private val AccentBgColor = Color(0xFFA8C7FA)
private val AccentFgColor = Color(0xFF062E6F)
private val ToolbarTextColor = Color(0xFFE3E3E3)

private data class ActivePointer(
    val keyId: String,
    val startPosition: Offset,
    var lastGlideX: Float,
    var isGliding: Boolean = false,
    var longPressJob: Job? = null,
    var repeatJob: Job? = null,
)

@Composable
fun KeyboardRoot(
    layoutId: String,
    returnAction: ReturnAction,
    assets: AssetRepository,
    onKeyFeedback: () -> Unit,
    onCommitText: (String) -> Unit,
    onBackspace: () -> Unit,
    onReturn: () -> Unit,
    onPaste: () -> Unit,
    onMoveCursor: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var layer by remember { mutableStateOf(KeyboardLayer.ALPHA) }
    var panelMode by remember { mutableStateOf(PanelMode.KEYBOARD) }
    var accentPopup by remember { mutableStateOf<AccentPopupState?>(null) }

    val activeLayoutId = when (layer) {
        KeyboardLayer.ALPHA -> layoutId
        KeyboardLayer.SYMBOLS -> "symbols"
        KeyboardLayer.SYMBOLS_MORE -> "symbols_more"
    }
    val rows = remember(activeLayoutId) { assets.loadLayout(activeLayoutId) }
    val kaomojiItems = remember { assets.loadKaomoji() }

    Surface(
        color = BgColor,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            ToolbarRow(
                panelMode = panelMode,
                onToggleEmoji = {
                    panelMode = if (panelMode == PanelMode.EMOJI) PanelMode.KEYBOARD else PanelMode.EMOJI
                },
                onToggleKaomoji = {
                    panelMode = if (panelMode == PanelMode.KAOMOJI) PanelMode.KEYBOARD else PanelMode.KAOMOJI
                },
                onPaste = {
                    onKeyFeedback()
                    onPaste()
                },
                onOpenSettings = {
                    onKeyFeedback()
                    onOpenSettings()
                },
            )

            when (panelMode) {
                PanelMode.EMOJI -> {
                    EmojiPanel(
                        onEmojiPicked = { emoji ->
                            onKeyFeedback()
                            onCommitText(emoji)
                        },
                    )
                }
                PanelMode.KAOMOJI -> {
                    KaomojiPanel(
                        items = kaomojiItems,
                        onSelect = { text ->
                            onKeyFeedback()
                            onCommitText(text)
                        },
                    )
                }
                PanelMode.KEYBOARD -> {
                    TouchKeyGrid(
                        rows = rows,
                        shiftState = shiftState,
                        returnAction = returnAction,
                        accentPopup = accentPopup,
                        assets = assets,
                        onShowAccentPopup = { accentPopup = it },
                        onKeyFeedback = onKeyFeedback,
                        onKeyAction = { keyId, fromAccent ->
                            if (fromAccent != null) {
                                onCommitText(fromAccent)
                                shiftState = KeyboardStateLogic.shiftAfterCharacterCommit(shiftState)
                                return@TouchKeyGrid
                            }
                            when (keyId) {
                                "SHIFT" -> shiftState = KeyboardStateLogic.nextShiftOnTap(shiftState)
                                "BACKSPACE" -> onBackspace()
                                "SYMBOLS" -> layer = KeyboardLayer.SYMBOLS
                                "MORE_SYMBOLS" -> layer = KeyboardLayer.SYMBOLS_MORE
                                "ALPHA" -> layer = KeyboardLayer.ALPHA
                                "SPACE" -> onCommitText(" ")
                                "RETURN" -> onReturn()
                                else -> {
                                    val output = KeyboardStateLogic.resolveKeyLabel(
                                        rawKey = keyId,
                                        shiftState = shiftState,
                                        returnAction = returnAction,
                                    )
                                    onCommitText(output)
                                    shiftState = KeyboardStateLogic.shiftAfterCharacterCommit(shiftState)
                                }
                            }
                        },
                        onShiftLongPress = {
                            shiftState = KeyboardStateLogic.nextShiftOnLongPress(shiftState)
                        },
                        onBackspaceRepeat = onBackspace,
                        onMoveCursor = onMoveCursor,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarRow(
    panelMode: PanelMode,
    onToggleEmoji: () -> Unit,
    onToggleKaomoji: () -> Unit,
    onPaste: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarChip(
                label = if (panelMode == PanelMode.EMOJI) "ABC" else "☺",
                active = panelMode == PanelMode.EMOJI,
                onClick = onToggleEmoji,
            )
            ToolbarChip(
                label = if (panelMode == PanelMode.KAOMOJI) "ABC" else "(^_^)",
                active = panelMode == PanelMode.KAOMOJI,
                onClick = onToggleKaomoji,
            )
            ToolbarChip(
                label = "Paste",
                active = false,
                onClick = onPaste,
            )
        }

        ToolbarChip(
            label = "⚙",
            active = false,
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun ToolbarChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                color = if (active) AccentBgColor else ActionKeyBgColor,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) AccentFgColor else ToolbarTextColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EmojiPanel(
    onEmojiPicked: (String) -> Unit,
) {
    AndroidView(
        factory = { context ->
            EmojiPickerView(context).apply {
                setOnEmojiPickedListener { item ->
                    onEmojiPicked(item.emoji)
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(234.dp),
    )
}

@Composable
private fun KaomojiPanel(
    items: List<KaomojiItem>,
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
            .height(234.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            categories.forEach { cat ->
                ToolbarChip(
                    label = cat,
                    active = cat == selectedCategory,
                    onClick = { selectedCategory = cat },
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 108.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(filtered) { item ->
                Box(
                    modifier = Modifier
                        .background(KeyBgColor, RoundedCornerShape(8.dp))
                        .clickable { onSelect(item.text) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.text,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TouchKeyGrid(
    rows: List<List<String>>,
    shiftState: ShiftState,
    returnAction: ReturnAction,
    accentPopup: AccentPopupState?,
    assets: AssetRepository,
    onShowAccentPopup: (AccentPopupState?) -> Unit,
    onKeyFeedback: () -> Unit,
    onKeyAction: (String, String?) -> Unit,
    onShiftLongPress: () -> Unit,
    onBackspaceRepeat: () -> Unit,
    onMoveCursor: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val glideThresholdPx = with(density) { 12.dp.toPx() }
    val glideStepPx = with(density) { 14.dp.toPx() }
    val accentCellWidthPx = with(density) { 38.dp.toPx() }

    val keyBounds = remember { mutableMapOf<String, Rect>() }
    val pressedKeys = remember { mutableStateMapOf<String, Boolean>() }
    val activePointers = remember { mutableMapOf<PointerId, ActivePointer>() }
    var gridCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val latestShift by rememberUpdatedState(shiftState)
    val latestPopup by rememberUpdatedState(accentPopup)
    val latestOnKeyAction by rememberUpdatedState(onKeyAction)
    val latestOnKeyFeedback by rememberUpdatedState(onKeyFeedback)
    val latestOnShiftLongPress by rememberUpdatedState(onShiftLongPress)
    val latestOnBackspaceRepeat by rememberUpdatedState(onBackspaceRepeat)
    val latestOnMoveCursor by rememberUpdatedState(onMoveCursor)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { gridCoords = it }
            .pointerInput(rows) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        val rootWindowOffset = gridCoords?.boundsInWindow()?.topLeft ?: Offset.Zero

                        for (change in event.changes) {
                            val windowPos = change.position + rootWindowOffset
                            val id = change.id

                            if (change.pressed && !change.previousPressed) {
                                val hitKey = findHitKey(keyBounds, windowPos) ?: continue
                                pressedKeys[hitKey] = true
                                latestOnKeyFeedback()

                                val pointer = ActivePointer(
                                    keyId = hitKey,
                                    startPosition = windowPos,
                                    lastGlideX = windowPos.x,
                                )

                                when (hitKey) {
                                    "BACKSPACE" -> {
                                        pointer.repeatJob = scope.launch {
                                            delay(380L)
                                            while (true) {
                                                latestOnBackspaceRepeat()
                                                delay(50L)
                                            }
                                        }
                                    }
                                    "SHIFT" -> {
                                        pointer.longPressJob = scope.launch {
                                            delay(380L)
                                            latestOnShiftLongPress()
                                        }
                                    }
                                    else -> {
                                        val baseChar = if (latestShift != ShiftState.OFF) {
                                            hitKey.uppercase()
                                        } else {
                                            hitKey
                                        }
                                        val variants = assets.lookupAccents(baseChar)
                                        if (variants.isNotEmpty()) {
                                            val rect = keyBounds[hitKey]
                                            pointer.longPressJob = scope.launch {
                                                delay(350L)
                                                val localX = (rect?.left ?: windowPos.x) - rootWindowOffset.x
                                                val localY = (rect?.top ?: windowPos.y) - rootWindowOffset.y
                                                onShowAccentPopup(
                                                    AccentPopupState(
                                                        baseKey = baseChar,
                                                        variants = variants,
                                                        selectedIndex = 0,
                                                        anchorX = localX,
                                                        anchorY = localY,
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                }

                                activePointers[id] = pointer
                                change.consume()
                            } else if (change.pressed && change.previousPressed) {
                                val pointer = activePointers[id] ?: continue
                                if (pointer.keyId == "SPACE") {
                                    val totalDx = windowPos.x - pointer.startPosition.x
                                    if (!pointer.isGliding && kotlin.math.abs(totalDx) > glideThresholdPx) {
                                        pointer.isGliding = true
                                        pointer.lastGlideX = windowPos.x
                                    }
                                    if (pointer.isGliding) {
                                        val stepDx = windowPos.x - pointer.lastGlideX
                                        val steps = (stepDx / glideStepPx).toInt()
                                        if (steps != 0) {
                                            latestOnMoveCursor(steps)
                                            pointer.lastGlideX += steps * glideStepPx
                                        }
                                    }
                                } else {
                                    val popup = latestPopup
                                    if (popup != null) {
                                        val dx = windowPos.x - pointer.startPosition.x
                                        val deltaIndex = (dx / accentCellWidthPx).roundToInt()
                                        val nextIndex = deltaIndex.coerceIn(0, popup.variants.lastIndex)
                                        if (nextIndex != popup.selectedIndex) {
                                            onShowAccentPopup(popup.copy(selectedIndex = nextIndex))
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
                                    onShowAccentPopup(null)
                                    latestOnKeyAction(pointer.keyId, chosen)
                                } else if (!pointer.isGliding) {
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
                .padding(horizontal = 3.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    row.forEach { keyId ->
                        val weight = keyWeightFor(keyId)
                        val isAction = isActionKey(keyId)
                        val isPressed = pressedKeys[keyId] == true
                        val isReturn = keyId == "RETURN"
                        val isShiftActive = keyId == "SHIFT" && shiftState != ShiftState.OFF

                        val bg = when {
                            isReturn -> AccentBgColor
                            isShiftActive -> KeyPressedColor
                            isPressed -> KeyPressedColor
                            isAction -> ActionKeyBgColor
                            else -> KeyBgColor
                        }
                        val fg = if (isReturn) AccentFgColor else Color.White

                        KeyCap(
                            keyId = keyId,
                            label = KeyboardStateLogic.resolveKeyLabel(keyId, shiftState, returnAction),
                            weight = weight,
                            backgroundColor = bg,
                            foregroundColor = fg,
                            onBoundsChanged = { rect -> keyBounds[keyId] = rect },
                        )
                    }
                }
            }
        }

        if (accentPopup != null) {
            val popupOffsetY = (accentPopup.anchorY - with(density) { 52.dp.toPx() }).roundToInt()
            val popupOffsetX = accentPopup.anchorX.roundToInt().coerceAtLeast(8)
            Surface(
                color = Color(0xFF2C2E33),
                shape = RoundedCornerShape(10.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .zIndex(10f)
                    .offset { IntOffset(popupOffsetX, popupOffsetY) },
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    accentPopup.variants.forEachIndexed { index, variant ->
                        val selected = index == accentPopup.selectedIndex
                        Box(
                            modifier = Modifier
                                .widthIn(min = 34.dp)
                                .height(40.dp)
                                .background(
                                    color = if (selected) AccentBgColor else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = variant,
                                color = if (selected) AccentFgColor else Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.KeyCap(
    keyId: String,
    label: String,
    weight: Float,
    backgroundColor: Color,
    foregroundColor: Color,
    onBoundsChanged: (Rect) -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .height(54.dp)
            .onGloballyPositioned { coords ->
                onBoundsChanged(coords.boundsInWindow())
            }
            .background(backgroundColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foregroundColor,
            fontSize = if (keyId == "SPACE" || keyId == "RETURN" || keyId == "SYMBOLS" || keyId == "ALPHA") 14.sp else 20.sp,
            fontWeight = if (isActionKey(keyId)) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

private fun findHitKey(bounds: Map<String, Rect>, pos: Offset): String? {
    for ((key, rect) in bounds) {
        if (rect.contains(pos)) return key
    }
    return null
}

private fun keyWeightFor(keyId: String): Float = when (keyId) {
    "SPACE" -> 4.5f
    "SHIFT", "BACKSPACE", "SYMBOLS", "MORE_SYMBOLS", "ALPHA", "RETURN" -> 1.5f
    else -> 1f
}

private fun isActionKey(keyId: String): Boolean = keyId in setOf(
    "SHIFT",
    "BACKSPACE",
    "SYMBOLS",
    "MORE_SYMBOLS",
    "ALPHA",
    "RETURN",
)

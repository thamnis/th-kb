package com.thamnis.thkb.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.EmojiNature
import androidx.compose.material.icons.outlined.EmojiPeople
import androidx.compose.material.icons.outlined.EmojiSymbols
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thamnis.thkb.data.EmojiSection
import com.thamnis.thkb.data.KaomojiItem
import com.thamnis.thkb.engine.SuggestionEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun EmojiAndKaomojiPanel(
    panelHeightDp: Float,
    emojiGridColumns: Int,
    emojiSections: List<EmojiSection>,
    kaomojiItems: List<KaomojiItem>,
    favoriteItems: Set<String>,
    recentEmojis: List<String>,
    emojiGridState: LazyGridState,
    onResizeDeltaDp: (Float) -> Unit,
    onBackToKeyboard: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCommitEmoji: (String) -> Unit,
    onCommitKaomoji: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackspaceRepeat: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Emojis, 1 = Kaomoji :-), 2 = Favorites ★
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

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

    val searchMatchingSections = remember(searchQuery, allEmojiSections) {
        if (searchQuery.isBlank()) {
            allEmojiSections
        } else {
            val hits = SuggestionEngine.searchEmojisBilingual(searchQuery)
            val catFiltered = allEmojiSections.filter {
                SuggestionEngine.stripAccents(it.category.lowercase())
                    .contains(SuggestionEngine.stripAccents(searchQuery.trim().lowercase()))
            }
            buildList {
                if (hits.isNotEmpty()) {
                    add(EmojiSection(category = "Matches (${hits.size})", emojis = hits))
                }
                addAll(catFiltered)
            }
        }
    }

    val categoryScrollState = rememberScrollState()

    val sectionStartIndices = remember(allEmojiSections) {
        var acc = 0
        allEmojiSections.map { sec ->
            val start = acc
            acc += 1 + sec.emojis.size
            start
        }
    }

    val currentVisibleSectionIdx by remember {
        derivedStateOf {
            val firstVisible = emojiGridState.firstVisibleItemIndex
            val idx = sectionStartIndices.indexOfLast { it <= firstVisible }
            if (idx >= 0) idx else 0
        }
    }

    LaunchedEffect(currentVisibleSectionIdx) {
        val targetScrollPx = with(density) { (currentVisibleSectionIdx * 42).dp.toPx() }.toInt()
        categoryScrollState.animateScrollTo(targetScrollPx)
    }

    val kaomojiCategories = remember(kaomojiItems) { listOf("all") + kaomojiItems.map { it.category }.distinct() }
    var selectedKaomojiCat by remember { mutableStateOf("all") }

    val searchOverhead = if (isSearchActive) 84f else 0f
    val contentHeight = (panelHeightDp - 96f - searchOverhead).coerceAtLeast(110f).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp),
    ) {
        // Top Bar: Back arrow + Search Toggle + Symbol Category Icons (plus vertical drag-to-resize)
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
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(ActionKeyBgColor)
                        .clickable { onBackToKeyboard() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = ToolbarTextColor,
                        modifier = Modifier.size(19.dp),
                    )
                }

                // Inline Rust-Backed Search Toggle Button (Proposition C)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isSearchActive) ReturnBgColor else ActionKeyBgColor)
                        .clickable {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Outlined.Close else Icons.Outlined.Search,
                        contentDescription = "Search Emojis",
                        tint = if (isSearchActive) ReturnFgColor else ToolbarTextColor,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .horizontalScroll(categoryScrollState)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (selectedTab == 0) {
                        allEmojiSections.forEachIndexed { idx, section ->
                            val active = !isSearchActive && currentVisibleSectionIdx == idx
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
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(catBg)
                                    .clickable {
                                        isSearchActive = false
                                        searchQuery = ""
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
                                    modifier = Modifier.size(19.dp),
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
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(catBg)
                                    .clickable { selectedKaomojiCat = cat },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = kaomojiCategoryIconFor(cat),
                                    contentDescription = cat,
                                    tint = catFg,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Inline Bilingual Emoji & Kaomoji Search Strip (EN / FR / Nouchi)
        AnimatedVisibility(
            visible = isSearchActive,
            enter = expandVertically(tween(150)) + fadeIn(tween(140)),
            exit = shrinkVertically(tween(120)) + fadeOut(tween(100)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ActionKeyBgColor)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) {
                            "Search EN / FR / Nouchi (e.g. feu, coeur, garba, ci)…"
                        } else {
                            "🔍 $searchQuery"
                        },
                        color = if (searchQuery.isEmpty()) HintTextColor else Color.White,
                        fontSize = 12.5.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "Clear",
                            color = ReturnBgColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { searchQuery = "" }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                // Compact inline quick-search letters & popular bilingual tags
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf("feu", "coeur", "rire", "merci", "ci", "garba", "fete", "love", "ok").forEach { tag ->
                        val activeTag = searchQuery.equals(tag, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (activeTag) ReturnBgColor else KeyBgColor)
                                .clickable {
                                    searchQuery = if (activeTag) "" else tag
                                }
                                .padding(horizontal = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = tag,
                                color = if (activeTag) ReturnFgColor else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                // Single-row scrollable A-Z mini filter bar for typing any custom search query
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    "abcdefghijklmnopqrstuvwxyz".forEach { ch ->
                        Box(
                            modifier = Modifier
                                .size(width = 22.dp, height = 22.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(KeyBgColor)
                                .clickable { searchQuery += ch },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = ch.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
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
                            fadeOut(animationSpec = tween(160)),
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
                        sections = if (isSearchActive && searchQuery.isNotBlank()) {
                            searchMatchingSections
                        } else {
                            allEmojiSections
                        },
                        gridState = emojiGridState,
                        onSelect = onCommitEmoji,
                    )
                }
                1 -> {
                    KaomojiGridContent(
                        contentHeight = contentHeight,
                        items = kaomojiItems,
                        selectedCategory = selectedKaomojiCat,
                        searchQuery = if (isSearchActive) searchQuery else "",
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onBackToKeyboard() }
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
                    .pointerInput(isSearchActive, searchQuery) {
                        awaitEachGesture {
                            val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                            down.consume()
                            backspacePressed = true
                            if (isSearchActive && searchQuery.isNotEmpty()) {
                                searchQuery = searchQuery.dropLast(1)
                            } else {
                                latestOnBackspace()
                            }
                            val repeatJob = scope.launch {
                                delay(380L)
                                while (true) {
                                    if (isSearchActive && searchQuery.isNotEmpty()) {
                                        searchQuery = searchQuery.dropLast(1)
                                    } else {
                                        latestOnBackspaceRepeat()
                                    }
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
internal fun NativeEmojiGridContent(
    contentHeight: Dp,
    gridColumns: Int,
    sections: List<EmojiSection>,
    gridState: LazyGridState,
    onSelect: (String) -> Unit,
) {
    val emojiFontSize = when {
        gridColumns <= 6 -> 32.sp
        gridColumns == 7 -> 30.sp
        gridColumns == 8 -> 28.5.sp
        else -> 24.sp
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(gridColumns.coerceIn(5, 10)),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight)
            .padding(horizontal = 2.dp),
    ) {
        sections.forEach { section ->
            item(
                key = "header_${section.category}",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "emoji_header",
            ) {
                Text(
                    text = section.category,
                    color = HintTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
            items(
                items = section.emojis,
                key = { emoji -> "${section.category}_$emoji" },
                contentType = { "emoji_cell" },
            ) { emoji ->
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(6.dp))
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
internal fun FavoritesGridContent(
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
internal fun KaomojiGridContent(
    contentHeight: Dp,
    items: List<KaomojiItem>,
    selectedCategory: String,
    searchQuery: String = "",
    favoriteItems: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    val filtered = remember(items, selectedCategory, searchQuery) {
        val byCat = if (selectedCategory == "all") items else items.filter { it.category == selectedCategory }
        if (searchQuery.isBlank()) {
            byCat
        } else {
            val needle = SuggestionEngine.stripAccents(searchQuery.trim().lowercase())
            items.filter { entry ->
                SuggestionEngine.stripAccents(entry.category.lowercase()).contains(needle) ||
                    entry.tags.any { SuggestionEngine.stripAccents(it.lowercase()).contains(needle) }
            }
        }
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

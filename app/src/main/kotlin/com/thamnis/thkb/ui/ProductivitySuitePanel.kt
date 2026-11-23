package com.thamnis.thkb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thamnis.thkb.data.CurrencyConverter
import com.thamnis.thkb.data.ScratchNote
import com.thamnis.thkb.data.TypingMetricsSnapshot
import com.thamnis.thkb.engine.FancyFontStyle
import com.thamnis.thkb.engine.FancyTextTransformer
import kotlinx.coroutines.launch

internal enum class SuiteTab(val label: String) {
    NOTES("Notes"),
    FANCY("Fancy"),
    CURRENCY("FX €↔$"),
    OCR_METRICS("OCR & Level"),
}

@Composable
internal fun ProductivitySuitePanel(
    panelHeightDp: Float,
    notes: List<ScratchNote>,
    activeFancyStyle: FancyFontStyle,
    currencyConverter: CurrencyConverter,
    ratesMap: Map<String, Double>,
    metricsSnapshot: TypingMetricsSnapshot,
    latestImageClipUri: String?,
    onAddNote: (String, String) -> Unit,
    onTogglePinNote: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSelectFancyStyle: (FancyFontStyle) -> Unit,
    onCommitText: (String) -> Unit,
    onLaunchOcr: (String?) -> Unit,
    onOpenMetricsPage: () -> Unit,
    onKeyFeedback: () -> Unit,
) {
    var activeTab by remember { mutableStateOf(SuiteTab.NOTES) }
    val contentHeight = (panelHeightDp - 44f).coerceAtLeast(180f).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SuiteTab.entries.forEach { tab ->
                CategoryChip(
                    label = tab.label,
                    active = activeTab == tab,
                    onClick = {
                        onKeyFeedback()
                        activeTab = tab
                    },
                )
            }
        }

        when (activeTab) {
            SuiteTab.NOTES -> NotesTabContent(
                contentHeight = contentHeight,
                notes = notes,
                onAddNote = onAddNote,
                onTogglePinNote = onTogglePinNote,
                onDeleteNote = onDeleteNote,
                onInsertNote = onCommitText,
                onKeyFeedback = onKeyFeedback,
            )
            SuiteTab.FANCY -> FancyTextTabContent(
                contentHeight = contentHeight,
                activeStyle = activeFancyStyle,
                onSelectStyle = onSelectFancyStyle,
                onCommitStyled = onCommitText,
                onKeyFeedback = onKeyFeedback,
            )
            SuiteTab.CURRENCY -> CurrencyTabContent(
                contentHeight = contentHeight,
                converter = currencyConverter,
                ratesMap = ratesMap,
                onInsertText = onCommitText,
                onKeyFeedback = onKeyFeedback,
            )
            SuiteTab.OCR_METRICS -> OcrAndMetricsTabContent(
                contentHeight = contentHeight,
                metrics = metricsSnapshot,
                latestImageClipUri = latestImageClipUri,
                onLaunchOcr = onLaunchOcr,
                onOpenMetricsPage = onOpenMetricsPage,
                onKeyFeedback = onKeyFeedback,
            )
        }
    }
}

@Composable
private fun NotesTabContent(
    contentHeight: androidx.compose.ui.unit.Dp,
    notes: List<ScratchNote>,
    onAddNote: (String, String) -> Unit,
    onTogglePinNote: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onInsertNote: (String) -> Unit,
    onKeyFeedback: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var draftTitle by remember { mutableStateOf("") }
    var draftBody by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Quick Scratchpad (${notes.size})",
                color = HintTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            CategoryChip(
                label = if (adding) "Close" else "+ New Note",
                active = adding,
                onClick = {
                    onKeyFeedback()
                    adding = !adding
                },
            )
        }

        if (adding) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(KeyBgColor)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BasicTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    singleLine = true,
                    textStyle = TextStyle(color = KeyTextColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(ReturnBgColor),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (draftTitle.isEmpty()) Text("Title (optional)", color = HintTextColor, fontSize = 12.5.sp)
                        inner()
                    },
                )
                BasicTextField(
                    value = draftBody,
                    onValueChange = { draftBody = it },
                    textStyle = TextStyle(color = KeyTextColor, fontSize = 13.sp),
                    cursorBrush = SolidColor(ReturnBgColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    decorationBox = { inner ->
                        if (draftBody.isEmpty()) Text("Write quick note, template, address, or snippet...", color = HintTextColor, fontSize = 12.5.sp)
                        inner()
                    },
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    CategoryChip(
                        label = "Save Note",
                        active = draftTitle.isNotBlank() || draftBody.isNotBlank(),
                        onClick = {
                            if (draftTitle.isNotBlank() || draftBody.isNotBlank()) {
                                onKeyFeedback()
                                onAddNote(draftTitle, draftBody)
                                draftTitle = ""
                                draftBody = ""
                                adding = false
                            }
                        },
                    )
                }
            }
        }

        if (notes.isEmpty() && !adding) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No notes saved yet. Tap '+ New Note' for instant snippets.",
                    color = HintTextColor,
                    fontSize = 12.5.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KeyBgColor)
                            .clickable {
                                onKeyFeedback()
                                onInsertNote(note.body.ifBlank { note.title })
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                        ) {
                            Text(
                                text = note.title,
                                color = KeyTextColor,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (note.body.isNotBlank() && note.body != note.title) {
                                Text(
                                    text = note.body,
                                    color = HintTextColor,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin note",
                                tint = if (note.pinned) ReturnBgColor else HintTextColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onTogglePinNote(note.id) },
                            )
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete note",
                                tint = HintTextColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onDeleteNote(note.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FancyTextTabContent(
    contentHeight: androidx.compose.ui.unit.Dp,
    activeStyle: FancyFontStyle,
    onSelectStyle: (FancyFontStyle) -> Unit,
    onCommitStyled: (String) -> Unit,
    onKeyFeedback: () -> Unit,
) {
    var sampleInput by remember { mutableStateOf("th-kb Pro") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(KeyBgColor)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicTextField(
                value = sampleInput,
                onValueChange = { sampleInput = it },
                singleLine = true,
                textStyle = TextStyle(color = KeyTextColor, fontSize = 13.sp),
                cursorBrush = SolidColor(ReturnBgColor),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (sampleInput.isEmpty()) Text("Preview text or select live style below…", color = HintTextColor, fontSize = 12.sp)
                    inner()
                },
            )
            if (activeStyle != FancyFontStyle.NORMAL) {
                Spacer(Modifier.width(6.dp))
                CategoryChip(
                    label = "Reset Normal",
                    active = true,
                    onClick = {
                        onKeyFeedback()
                        onSelectStyle(FancyFontStyle.NORMAL)
                    },
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(FancyFontStyle.entries.filter { it != FancyFontStyle.NORMAL }) { style ->
                val previewText = FancyTextTransformer.transform(
                    sampleInput.ifBlank { style.displayName },
                    style,
                )
                val isLive = activeStyle == style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isLive) ReturnBgColor.copy(alpha = 0.2f) else KeyBgColor)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (sampleInput.isNotBlank()) {
                                    onKeyFeedback()
                                    onCommitStyled(previewText)
                                }
                            },
                    ) {
                        Text(
                            text = style.displayName,
                            color = HintTextColor,
                            fontSize = 10.5.sp,
                        )
                        Text(
                            text = previewText,
                            color = KeyTextColor,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    CategoryChip(
                        label = if (isLive) "Live ✓" else "Use Live",
                        active = isLive,
                        onClick = {
                            onKeyFeedback()
                            onSelectStyle(if (isLive) FancyFontStyle.NORMAL else style)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyTabContent(
    contentHeight: androidx.compose.ui.unit.Dp,
    converter: CurrencyConverter,
    ratesMap: Map<String, Double>,
    onInsertText: (String) -> Unit,
    onKeyFeedback: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val codes = remember { CurrencyConverter.DEFAULT_CURRENCIES.map { it.code } }
    var amountStr by remember { mutableStateOf("100") }
    var fromCode by remember { mutableStateOf("EUR") }
    var toCode by remember { mutableStateOf("XOF") }
    var isRefreshing by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val converted = remember(amount, fromCode, toCode, ratesMap) {
        converter.convert(amount, fromCode, toCode)
    }
    val formatted = remember(converted, toCode) {
        converter.formatResult(converted, toCode)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // From <-> To selector & result display
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CategoryChip(
                label = fromCode,
                active = true,
                onClick = {
                    onKeyFeedback()
                    fromCode = codes[(codes.indexOf(fromCode) + 1) % codes.size]
                },
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(ActionKeyBgColor)
                    .clickable {
                        onKeyFeedback()
                        val tmp = fromCode
                        fromCode = toCode
                        toCode = tmp
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = "Swap", tint = ToolbarTextColor, modifier = Modifier.size(16.dp))
            }
            CategoryChip(
                label = toCode,
                active = true,
                onClick = {
                    onKeyFeedback()
                    toCode = codes[(codes.indexOf(toCode) + 1) % codes.size]
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(ActionKeyBgColor)
                    .clickable(enabled = !isRefreshing) {
                        onKeyFeedback()
                        isRefreshing = true
                        scope.launch {
                            converter.refreshLiveRates()
                            isRefreshing = false
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Refresh Rates",
                    tint = if (isRefreshing) ReturnBgColor else HintTextColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Quick preset pairs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("EUR" to "XOF", "USD" to "XOF", "USD" to "EUR", "GBP" to "EUR", "CAD" to "XOF").forEach { (f, t) ->
                CategoryChip(
                    label = "$f→$t",
                    active = fromCode == f && toCode == t,
                    onClick = {
                        onKeyFeedback()
                        fromCode = f
                        toCode = t
                    },
                )
            }
        }

        // Amount input + big result
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(KeyBgColor)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicTextField(
                value = amountStr,
                onValueChange = { next ->
                    if (next.length <= 12 && next.all { it.isDigit() || it == '.' }) {
                        amountStr = next
                    }
                },
                singleLine = true,
                textStyle = TextStyle(color = KeyTextColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(ReturnBgColor),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "= $formatted $toCode",
                color = ReturnBgColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Quick keypad / amounts + 1-tap insert
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("10", "100", "1000", "5000").forEach { preset ->
                CategoryChip(
                    label = preset,
                    active = amountStr == preset,
                    onClick = {
                        onKeyFeedback()
                        amountStr = preset
                    },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            CategoryChip(
                label = "Insert",
                active = true,
                onClick = {
                    onKeyFeedback()
                    onInsertText("$formatted $toCode")
                },
            )
        }
    }
}

@Composable
private fun OcrAndMetricsTabContent(
    contentHeight: androidx.compose.ui.unit.Dp,
    metrics: TypingMetricsSnapshot,
    latestImageClipUri: String?,
    onLaunchOcr: (String?) -> Unit,
    onOpenMetricsPage: () -> Unit,
    onKeyFeedback: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // OCR Scanner Launch Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KeyBgColor)
                .clickable {
                    onKeyFeedback()
                    onLaunchOcr(null)
                }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = ReturnBgColor, modifier = Modifier.size(24.dp))
                Column {
                    Text("OCR Text Scanner", color = KeyTextColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Scan text from camera or gallery into field", color = HintTextColor, fontSize = 11.5.sp)
                }
            }
            CategoryChip(
                label = "Open",
                active = true,
                onClick = {
                    onKeyFeedback()
                    onLaunchOcr(null)
                },
            )
        }

        if (latestImageClipUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ActionKeyBgColor)
                    .clickable {
                        onKeyFeedback()
                        onLaunchOcr(latestImageClipUri)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Scan Latest Clipboard Screenshot", color = ToolbarTextColor, fontSize = 12.5.sp)
                Text("Scan →", color = ReturnBgColor, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Language Level & Vocabulary Metrics summary card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KeyBgColor)
                .clickable {
                    onKeyFeedback()
                    onOpenMetricsPage()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Language Level: ${metrics.cefrLevel}", color = ReturnBgColor, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${metrics.totalWords} words · ${metrics.uniqueCount} unique · ${"%.0f".format(metrics.vocabularyRichnessPct)}% richness",
                    color = HintTextColor,
                    fontSize = 11.5.sp,
                )
            }
            CategoryChip(
                label = "Details",
                active = false,
                onClick = {
                    onKeyFeedback()
                    onOpenMetricsPage()
                },
            )
        }
    }
}

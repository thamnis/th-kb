package com.thamnis.thkb.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thamnis.thkb.data.ClipboardEntry
import com.thamnis.thkb.data.TodoTask

@Composable
internal fun ClipboardPanel(
    panelHeightDp: Float,
    entries: List<ClipboardEntry>,
    onCommitClip: (String) -> Unit,
    onCommitMediaClip: (ClipboardEntry) -> Unit = { onCommitClip(it.text) },
    onSystemPaste: () -> Unit,
    onPinClip: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    onClearUnpinned: () -> Unit,
    onSyncScreenshots: () -> Unit = {},
) {
    val contentHeight = (panelHeightDp - 40f).coerceAtLeast(180f).dp
    val context = LocalContext.current

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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryChip(
                label = "Paste",
                active = true,
                onClick = onSystemPaste,
            )
            CategoryChip(
                label = "Screenshots",
                active = false,
                onClick = onSyncScreenshots,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (entries.any { !it.pinned }) {
                CategoryChip(
                    label = "Clear",
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
                items(entries, key = { it.mediaUri ?: it.text }) { entry ->
                    val thumbnailBitmap = remember(entry.mediaUri) {
                        val uriStr = entry.mediaUri ?: return@remember null
                        runCatching {
                            val opts = android.graphics.BitmapFactory.Options().apply {
                                inSampleSize = 4
                            }
                            context.contentResolver.openInputStream(android.net.Uri.parse(uriStr))?.use { stream ->
                                android.graphics.BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                            }
                        }.getOrNull()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KeyBgColor)
                            .clickable {
                                if (entry.isRichMedia) {
                                    onCommitMediaClip(entry)
                                } else {
                                    onCommitClip(entry.text)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (entry.isRichMedia) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ActionKeyBgColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (thumbnailBitmap != null) {
                                    Image(
                                        bitmap = thumbnailBitmap,
                                        contentDescription = entry.text,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(38.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = entry.text,
                                        tint = ReturnBgColor,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = entry.text,
                            color = KeyTextColor,
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
                                    .clickable { onPinClip(entry.mediaUri ?: entry.text) },
                            )
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = HintTextColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onDeleteClip(entry.mediaUri ?: entry.text) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TodoListPanel(
    panelHeightDp: Float,
    tasks: List<TodoTask>,
    onAddTask: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onCommitTaskText: (String) -> Unit,
    onKeyFeedback: () -> Unit,
) {
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
                    color = KeyTextColor,
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
                    textStyle = TextStyle(color = KeyTextColor, fontSize = 14.sp),
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
internal fun TodoItemRow(
    task: TodoTask,
    isJustInserted: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textColor by animateColorAsState(
        targetValue = if (task.isCompleted) HintTextColor else KeyTextColor,
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

        val strikeColor = HintTextColor

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
                            color = strikeColor,
                            start = Offset(0f, y),
                            end = Offset(size.width * strikeProgress, y),
                            strokeWidth = 1.8f * density.density,
                            cap = StrokeCap.Round,
                        )
                    }
                },
        )

        Spacer(modifier = Modifier.width(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentPaste,
                contentDescription = "Paste into text",
                tint = if (isJustInserted) Color(0xFF81C784) else HintTextColor,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onCommit),
            )
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

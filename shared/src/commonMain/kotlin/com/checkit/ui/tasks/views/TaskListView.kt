package com.checkit.ui.tasks.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.checkit.domain.ListSection
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.ui.tasks.TaskListDisplayType
import com.checkit.ui.tasks.TaskListEntry
import com.checkit.ui.theme.toColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@Composable
internal fun TaskListView(
    items: List<TaskListEntry>,
    showListName: Boolean,
    displayType: TaskListDisplayType = TaskListDisplayType.Standard,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    onMoveItem: (from: Int, to: Int) -> Unit = { _, _ -> },
    onMoveComplete: () -> Unit = {},
    reorderEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val currentOnMoveItem by rememberUpdatedState(onMoveItem)
    val currentOnMoveComplete by rememberUpdatedState(onMoveComplete)
    val currentItems by rememberUpdatedState(items)

    val dragDropState = rememberTaskListDragDropState(listState) { from, to ->
        currentOnMoveItem(from, to)
    }

    DisposableEffect(dragDropState) {
        onDispose { currentOnMoveComplete() }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (reorderEnabled) {
                    Modifier.pointerInput(dragDropState) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                dragDropState.onDragStart(offset) { index ->
                                    currentItems.getOrNull(index).isDraggable
                                }
                                if (dragDropState.isDragging) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            onDragEnd = {
                                dragDropState.onDragInterrupted()
                                currentOnMoveComplete()
                            },
                            onDragCancel = {
                                dragDropState.onDragInterrupted()
                                currentOnMoveComplete()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDropState.onDrag(dragAmount)
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(items, key = { _, it -> it.key }) { _, item ->
            val isDragging = item.key == dragDropState.draggingItemKey
            val isSettling = item.key == dragDropState.previousKeyOfDraggedItem
            val lift by animateFloatAsState(
                targetValue = if (isDragging) 1.03f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "task-drag-lift"
            )
            val elevation by animateFloatAsState(
                targetValue = if (isDragging || isSettling) 16f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "task-drag-elevation"
            )

            DraggableTaskRow(
                dragDropState = dragDropState,
                key = item.key
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = lift
                            scaleY = lift
                            shadowElevation = elevation
                        }
                ) {
                    when (item) {
                        is TaskListEntry.Task -> {
                            val task = item.item
                            TaskRow(
                                task = task,
                                onClick = {
                                    if (!dragDropState.shouldIgnoreClick()) {
                                        onTaskClick(task)
                                    }
                                },
                                showList = showListName,
                                displayType = displayType
                            )
                        }
                        is TaskListEntry.Note -> {
                            val note = item.item
                            NoteRow(
                                note = note,
                                onClick = {
                                    if (!dragDropState.shouldIgnoreClick()) {
                                        onNoteClick(note)
                                    }
                                },
                                showList = showListName,
                                displayType = displayType
                            )
                        }
                        is TaskListEntry.SectionHeader -> {
                            SectionHeaderRow(item.section)
                        }
                        is TaskListEntry.PinnedHeader -> {
                            PinnedHeaderRow()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberTaskListDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): TaskListDragDropState {
    val scope = rememberCoroutineScope()
    val onMoveState = rememberUpdatedState(onMove)
    val state = remember(lazyListState) {
        TaskListDragDropState(
            listState = lazyListState,
            scope = scope,
            onMove = { from, to -> onMoveState.value(from, to) }
        )
    }
    LaunchedEffect(state) {
        while (true) {
            val diff = state.scrollChannel.receive()
            lazyListState.scrollBy(diff)
        }
    }
    return state
}

private class TaskListDragDropState(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit
) {
    var draggingItemKey by mutableStateOf<String?>(null)
        private set
    var previousKeyOfDraggedItem by mutableStateOf<String?>(null)
        private set

    val isDragging: Boolean get() = draggingItemKey != null

    internal val scrollChannel = Channel<Float>(Channel.CONFLATED)
    internal val previousItemOffset = Animatable(0f)

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)
    private var consumeNextClick by mutableStateOf(false)

    val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
        } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingItemKey }

    fun shouldIgnoreClick(): Boolean {
        if (isDragging || consumeNextClick) {
            consumeNextClick = false
            return true
        }
        return false
    }

    fun onDragStart(offset: Offset, canDrag: (Int) -> Boolean) {
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
            offset.y.toInt() in info.offset until (info.offset + info.size)
        } ?: return
        val key = item.key as? String ?: return
        if (!canDrag(item.index)) return

        draggingItemKey = key
        draggingItemInitialOffset = item.offset
        draggingItemDraggedDelta = 0f
        consumeNextClick = true
        previousKeyOfDraggedItem = null
        scope.launch { previousItemOffset.snapTo(0f) }
    }

    fun onDrag(offset: Offset) {
        if (draggingItemKey == null) return
        draggingItemDraggedDelta += offset.y

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = (startOffset + endOffset) / 2f

        val targetItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            middleOffset.toInt() in item.offset until item.offsetEnd &&
                item.index != draggingItem.index
        }
        if (targetItem != null) {
            if (
                draggingItem.index == listState.firstVisibleItemIndex ||
                targetItem.index == listState.firstVisibleItemIndex
            ) {
                listState.requestScrollToItem(
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset
                )
            }
            onMove(draggingItem.index, targetItem.index)
        } else {
            val overscroll = when {
                draggingItemDraggedDelta > 0 ->
                    (endOffset - listState.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                draggingItemDraggedDelta < 0 ->
                    (startOffset - listState.layoutInfo.viewportStartOffset).coerceAtMost(0f)
                else -> 0f
            }
            if (overscroll != 0f) {
                scrollChannel.trySend(overscroll)
            }
        }
    }

    fun onDragInterrupted() {
        val key = draggingItemKey
        if (key != null) {
            previousKeyOfDraggedItem = key
            val startOffset = draggingItemOffset
            scope.launch {
                previousItemOffset.snapTo(startOffset)
                previousItemOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = 1f
                    )
                )
                if (previousKeyOfDraggedItem == key) {
                    previousKeyOfDraggedItem = null
                }
            }
        }
        draggingItemDraggedDelta = 0f
        draggingItemKey = null
        draggingItemInitialOffset = 0
    }

    private val LazyListItemInfo.offsetEnd: Int
        get() = offset + size
}

@Composable
private fun LazyItemScope.DraggableTaskRow(
    dragDropState: TaskListDragDropState,
    key: String,
    content: @Composable () -> Unit
) {
    val dragging = key == dragDropState.draggingItemKey
    val settling = key == dragDropState.previousKeyOfDraggedItem
    val dragModifier = when {
        dragging -> Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = dragDropState.draggingItemOffset }
        settling -> Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = dragDropState.previousItemOffset.value }
        else -> Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
    }
    Box(modifier = dragModifier) {
        content()
    }
}

private val TaskListEntry?.isDraggable: Boolean
    get() = this is TaskListEntry.Task || this is TaskListEntry.Note

@Composable
private fun SectionHeaderRow(section: ListSection?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (section != null) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                color = section.color.toColor(),
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "Unsectioned",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PinnedHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PushPin,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Pinned",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun ListDisplayTypeMenu(
    selected: TaskListDisplayType,
    onSelect: (TaskListDisplayType) -> Unit
) {
    var isPopupOpen by remember { mutableStateOf(false) }
    val visibleState = remember { MutableTransitionState(false) }

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(
            onClick = {
                isPopupOpen = true
                visibleState.targetState = true
            }
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = selected.icon(),
                    contentDescription = "view options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        if (isPopupOpen) {
            if (visibleState.isIdle && !visibleState.targetState) {
                isPopupOpen = false
            }

            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(x = 0, y = 130),
                onDismissRequest = { visibleState.targetState = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = scaleIn(
                        initialScale = 0.7f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = tween(200)
                    ) + fadeIn(),
                    exit = scaleOut(
                        targetScale = 0.7f,
                        transformOrigin = TransformOrigin(1f, 0f),
                        animationSpec = tween(150)
                    ) + fadeOut()
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .heightIn(min = 36.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TaskListDisplayType.entries.forEach { displayType ->
                                    ViewOptionChip(
                                        icon = displayType.icon(),
                                        label = displayType.label(),
                                        selected = selected == displayType,
                                        onClick = {
                                            onSelect(displayType)
                                        }
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

private fun TaskListDisplayType.icon(): ImageVector =
    when (this) {
        TaskListDisplayType.Brief -> Icons.AutoMirrored.Filled.ViewList
        TaskListDisplayType.Standard -> Icons.Outlined.ViewDay
        TaskListDisplayType.Detail -> Icons.AutoMirrored.Filled.Article
    }

private fun TaskListDisplayType.label(): String =
    when (this) {
        TaskListDisplayType.Brief -> "Brief"
        TaskListDisplayType.Standard -> "Standard"
        TaskListDisplayType.Detail -> "Detail"
    }

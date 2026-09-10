package com.checkit.ui.quicknote

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.checkit.data.QuickNoteSyncStatus
import com.checkit.domain.QuickNote
import com.checkit.domain.QuickNoteType
import com.checkit.ui.components.AiQuickAddBar
import com.checkit.ui.components.SectionLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * QuickNote content (banner, list, capture, dialogs) hosted as the second
 * segment of the My Day tab.
 */
@Composable
fun QuickNoteContent(
    viewModel: QuickNoteViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { viewModel.refresh() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier.fillMaxSize()) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        QuickNoteHeaderBanner(
            syncState = state.syncState,
            itemCount = state.next.size,
            onRetry = { viewModel.refresh(force = true) },
        )
            val listState = rememberLazyListState()
            val haptic = LocalHapticFeedback.current
            val currentOnMoveItem by rememberUpdatedState(viewModel::moveDragging)
            val currentOnMoveComplete by rememberUpdatedState(viewModel::commitDrag)

            val dragDropState = rememberQuickNoteDragDropState(listState) { from, to ->
                // Notes start at index 1 due to "header-next". 
                // Subtract 1 to pass correct relative indices to ViewModel.
                currentOnMoveItem(from - 1, to - 1)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .pointerInput(dragDropState) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                dragDropState.onDragStart(offset)
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
                                viewModel.cancelDrag()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDropState.onDrag(dragAmount)
                            }
                        )
                    },
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item(key = "header-next") {
                    Text(
                        text = "NEXT",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (state.visibleNext.isEmpty()) {
                    item(key = "empty-next") {
                        Text(
                            "Nothing here. Capture a thought below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
                itemsIndexed(state.visibleNext, key = { _, note -> "next-${note.id}" }) { _, note ->
                    DraggableQuickNoteRow(
                        dragDropState = dragDropState,
                        key = "next-${note.id}"
                    ) {
                        NextRow(
                            note = note,
                            onDeleteSwipe = { viewModel.swipeRight(note.id) },
                            onReminderSwipe = { viewModel.openReminderPicker(note.id) },
                            onImageClick = viewModel::openImagePreview,
                        )
                    }
                }
                item(key = "header-deleted") {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "TO BE DELETED",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (state.toBeDeleted.isEmpty()) {
                    item(key = "empty-deleted") {
                        Text(
                            "Deleted notes disappear after 24 hours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
                items(state.toBeDeleted, key = { "deleted-${it.id}" }) { note ->
                    DeletedRow(
                        note = note,
                        onDeleteSwipe = { viewModel.deletePermanently(note.id) },
                        onRestoreSwipe = { viewModel.restore(note.id) },
                        onImageClick = viewModel::openImagePreview
                    )
                }
                item(key = "bottom-spacer") { Spacer(Modifier.height(16.dp)) }
            }
            QuickCaptureBar(
                input = state.input,
                onInputChange = viewModel::updateInput,
                onSubmit = viewModel::submitInput,
                onCameraClick = viewModel::onCameraClick,
            )
    }

    if (state.reminderPickerId != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissReminderPicker,
            title = {
                Text(
                    "Remind Me Later",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Note stays in Next and moves to top when reminded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    FilledTonalButton(
                        onClick = viewModel::setReminder15Min,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("15 minutes")
                    }

                    FilledTonalButton(
                        onClick = viewModel::setReminder30Min,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("30 minutes")
                    }

                    FilledTonalButton(
                        onClick = viewModel::setReminder1Hour,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("1 hour")
                    }
                }
            },
            confirmButton = {},
            dismissButton = null,
        )
    }

    val pendingImagePath = state.pendingImagePath
    if (pendingImagePath != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPendingImage,
            title = { Text("Add photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val preview = rememberDecodedImage(pendingImagePath)
                    if (preview != null) {
                        Image(
                            preview,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    OutlinedTextField(
                        value = state.pendingImageTitle,
                        onValueChange = viewModel::updatePendingImageTitle,
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmPendingImage) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPendingImage) { Text("Cancel") }
            },
        )
    }

    val previewImagePath = state.previewImagePath
    if (previewImagePath != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissImagePreview,
            title = null,
            text = {
                val full = rememberDecodedImage(previewImagePath)
                if (full != null) {
                    Image(
                        full,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
            },
            confirmButton = {
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}

@Composable
private fun rememberDecodedImage(path: String): ImageBitmap? {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        // Default (not IO: IO is unavailable on Kotlin/Native targets).
        bitmap = withContext(Dispatchers.Default) { decodeQuickNoteImage(path) }
    }
    return bitmap
}

@Composable
private fun QuickNoteThumbnail(
    path: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberDecodedImage(path)
    if (bitmap != null) {
        Image(
            bitmap,
            contentDescription = null,
            modifier = modifier.size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick(path) },
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun rememberQuickNoteDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): QuickNoteDragDropState {
    val scope = rememberCoroutineScope()
    val onMoveState = rememberUpdatedState(onMove)
    val state = remember(lazyListState) {
        QuickNoteDragDropState(
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

private class QuickNoteDragDropState(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit
) {
    var draggingItemKey by mutableStateOf<String?>(null)
        private set
    var previousKeyOfDraggedItem by mutableStateOf<String?>(null)
        private set

    val isDragging: Boolean get() = draggingItemKey != null

    val scrollChannel = Channel<Float>(Channel.CONFLATED)
    val previousItemOffset = Animatable(0f)

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)

    val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
        } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingItemKey }

    fun onDragStart(offset: Offset) {
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
            offset.y.toInt() in info.offset until (info.offset + info.size)
        } ?: return
        val key = item.key as? String ?: return
        if (!key.startsWith("next-")) return // Only allow dragging NEXT items

        draggingItemKey = key
        draggingItemInitialOffset = item.offset
        draggingItemDraggedDelta = 0f
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
            middleOffset.toInt() in item.offset until (item.offset + item.size) &&
                    item.index != draggingItem.index &&
                    (item.key as? String)?.startsWith("next-") == true
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
}

@Composable
private fun LazyItemScope.DraggableQuickNoteRow(
    dragDropState: QuickNoteDragDropState,
    key: String,
    content: @Composable () -> Unit
) {
    val dragging = key == dragDropState.draggingItemKey
    val settling = key == dragDropState.previousKeyOfDraggedItem

    val lift by animateFloatAsState(
        targetValue = if (dragging) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "note-drag-lift"
    )
    val elevation by animateFloatAsState(
        targetValue = if (dragging || settling) 8f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "note-drag-elevation"
    )

    val dragModifier = when {
        dragging -> Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY = dragDropState.draggingItemOffset
                scaleX = lift
                scaleY = lift
                shadowElevation = elevation
                shape = RoundedCornerShape(10.dp)
                clip = false
            }
        settling -> Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY = dragDropState.previousItemOffset.value
                shadowElevation = elevation
                shape = RoundedCornerShape(10.dp)
                clip = false
            }
        else -> Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
    }

    Box(modifier = dragModifier) {
        content()
    }
}

@Composable
private fun NextRow(
    note: QuickNote,
    onDeleteSwipe: () -> Unit,
    onReminderSwipe: () -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteSwipe()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onReminderSwipe()
                    false
                }
                else -> false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.DeleteSweep
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Alarm
                else -> null
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            Box(
                Modifier.fillMaxSize()
                    .background(color, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = alignment,
            ) {
                icon?.let { Icon(it, contentDescription = null) }
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            ) {
            Text(
                note.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (note.type == QuickNoteType.IMAGE && note.attachmentLocalPath != null) {
                Spacer(Modifier.width(8.dp))
                QuickNoteThumbnail(note.attachmentLocalPath, onClick = onImageClick)
            }
            if (note.remindAt != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatReminder(note.remindAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeletedRow(
    note: QuickNote,
    onDeleteSwipe: () -> Unit,
    onRestoreSwipe: () -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteSwipe()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRestoreSwipe()
                    true
                }
                else -> false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.DeleteSweep
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.RestoreFromTrash
                else -> null
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            Box(
                Modifier.fillMaxSize()
                    .background(color, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = alignment,
            ) {
                icon?.let { Icon(it, contentDescription = null) }
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (note.type == QuickNoteType.IMAGE && note.attachmentLocalPath != null) {
                    QuickNoteThumbnail(note.attachmentLocalPath, onClick = onImageClick)
                }
                Text(
                    formatRemaining(note.deleteAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun QuickCaptureBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AiQuickAddBar(
            value = input,
            onValueChange = onInputChange,
            placeholder = "Capture a thought...",
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                onSubmit()
                focusManager.clearFocus()
            }),
            haloPadding = 6.dp
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onCameraClick) {
            Icon(Icons.Default.PhotoCamera, contentDescription = "Take photo")
        }
    }
}

@Composable
private fun QuickNoteHeaderBanner(
    syncState: com.checkit.data.QuickNoteSyncState,
    itemCount: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Always composed with a fixed height so the list below never jumps
    // when sync status changes.
    val status = syncState.status
    val containerColor = when (status) {
        QuickNoteSyncStatus.OFFLINE, QuickNoteSyncStatus.ERROR ->
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        QuickNoteSyncStatus.SYNCING ->
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val contentColor = when (status) {
        QuickNoteSyncStatus.OFFLINE, QuickNoteSyncStatus.ERROR ->
            MaterialTheme.colorScheme.onErrorContainer
        QuickNoteSyncStatus.SYNCING ->
            MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier.fillMaxWidth()
            .height(52.dp)
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (status) {
            QuickNoteSyncStatus.SYNCING ->
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = contentColor,
                )
            QuickNoteSyncStatus.OFFLINE, QuickNoteSyncStatus.ERROR ->
                Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
            else ->
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        val text = when (status) {
            QuickNoteSyncStatus.SYNCING -> "Syncing…"
            QuickNoteSyncStatus.OFFLINE ->
                "You're offline. Changes are saved on this device."
            else -> syncState.message ?: "Synced"
        }
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (status == QuickNoteSyncStatus.OFFLINE || status == QuickNoteSyncStatus.ERROR) {
            TextButton(onClick = onRetry) {
                Text("Retry", color = contentColor)
            }
        }
        Text(
            "$itemCount",
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

internal fun formatRemaining(deleteAt: Long?): String {
    if (deleteAt == null) return ""
    val remaining = (deleteAt - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(0L)
    val hours = remaining / 3_600_000L
    if (hours >= 1) return "${hours}h"
    val minutes = (remaining / 60_000L).coerceAtLeast(1L)
    return "${minutes}m"
}

internal fun formatReminder(remindAt: Long): String {
    val remaining = (remindAt - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(0L)
    val minutes = remaining / 60_000L
    if (minutes < 60) return "in ${minutes.coerceAtLeast(1)}m"
    return "in ${minutes / 60}h"
}

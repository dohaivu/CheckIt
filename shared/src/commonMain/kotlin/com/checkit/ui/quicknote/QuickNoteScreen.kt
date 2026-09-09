package com.checkit.ui.quicknote

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.checkit.domain.QuickNote
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.SectionLabel
import com.checkit.ui.components.TinyTopAppBar
import kotlin.math.roundToInt
import kotlin.time.Clock

@Composable
fun QuickNoteScreen(
    viewModel: QuickNoteViewModel,
    onNavigateBack: () -> Unit,
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Quick notes", style = MaterialTheme.typography.titleMedium) },
            )
        },
        bottomBar = {
            QuickCaptureBar(
                input = state.input,
                onInputChange = viewModel::updateInput,
                onSubmit = viewModel::submitInput,
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item(key = "header-next") {
                    SectionLabel("NEXT")
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
                itemsIndexed(state.visibleNext, key = { _, note -> "next-${note.id}" }) { index, note ->
                    NextRow(
                        note = note,
                        index = index,
                        itemCount = state.visibleNext.size,
                        onDeleteSwipe = { viewModel.swipeRight(note.id) },
                        onReminderSwipe = { viewModel.openReminderPicker(note.id) },
                        onDragDelta = { deltaPx, itemHeightPx ->
                            DragAccumulator.delta += deltaPx
                            val steps = (DragAccumulator.delta / itemHeightPx).roundToInt()
                            if (steps != 0) {
                                DragAccumulator.delta = 0f
                                val from = state.visibleNext.indexOfFirst { it.id == note.id }
                                val to = (from + steps).coerceIn(0, state.visibleNext.lastIndex)
                                if (from >= 0 && to != from) viewModel.moveDragging(from, to)
                            }
                        },
                        onDragEnd = {
                            DragAccumulator.delta = 0f
                            viewModel.commitDrag()
                        },
                        onDragCancel = {
                            DragAccumulator.delta = 0f
                            viewModel.cancelDrag()
                        },
                    )
                }
                item(key = "header-deleted") {
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("TO BE DELETED")
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
                    DeletedRow(note = note)
                }
                item(key = "bottom-spacer") { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (state.reminderPickerId != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissReminderPicker,
            title = { Text("Remind me in") },
            text = { Text("The note stays in Next.") },
            confirmButton = {
                TextButton(onClick = viewModel::setReminder30Min) { Text("30 minutes") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::setReminder1Hour) { Text("1 hour") }
                    TextButton(onClick = viewModel::dismissReminderPicker) { Text("Cancel") }
                }
            },
        )
    }
}

private object DragAccumulator {
    var delta: Float = 0f
}

@Composable
private fun NextRow(
    note: QuickNote,
    index: Int,
    itemCount: Int,
    onDeleteSwipe: () -> Unit,
    onReminderSwipe: () -> Unit,
    onDragDelta: (deltaPx: Float, itemHeightPx: Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDeleteSwipe()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onReminderSwipe()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val value = dismissState.dismissDirection
            val color = when (value) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer
                SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surfaceVariant
            }
            val icon = when (value) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.DeleteSweep
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Alarm
                SwipeToDismissBoxValue.Settled -> Icons.Default.Notifications
            }
            val alignment = when (value) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            Box(
                Modifier.fillMaxSize().background(color, RoundedCornerShape(10.dp)).padding(horizontal = 16.dp),
                contentAlignment = alignment,
            ) {
                Icon(icon, contentDescription = null)
            }
        },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val density = LocalDensity.current
                val itemHeightPx = remember(density) { with(density) { 72.dp.toPx() } }
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp).pointerInput(note.id, index, itemCount) {
                        detectDragGesturesAfterLongPress(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount.y, itemHeightPx)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragCancel() },
                            onDragStart = { DragAccumulator.delta = 0f },
                        )
                    }.padding(4.dp),
                )
                Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(note.content, style = MaterialTheme.typography.bodyMedium)
                    if (note.remindAt != null) {
                        Text(
                            "Reminder ${formatReminder(note.remindAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletedRow(note: QuickNote, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            Text(
                formatRemaining(note.deleteAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}

@Composable
private fun QuickCaptureBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            AppOutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = "+ Capture a thought",
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onSubmit,
            enabled = input.isNotBlank(),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Capture")
        }
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

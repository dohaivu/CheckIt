package com.checkit.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.ui.TaskListDisplayType

@Composable
internal fun TaskListView(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    lists: List<TaskList>,
    showListName: Boolean,
    displayType: TaskListDisplayType = TaskListDisplayType.Standard,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(tasks, key = { "task-${it.id}" }) { task ->
            TaskRow(
                task = task,
                onClick = { onTaskClick(task) },
                list = if (showListName) task.list else null,
                displayType = displayType
            )
        }
        items(notes, key = { "note-${it.id}" }) { note ->
            NoteRow(
                note = note,
                onClick = { onNoteClick(note) },
                list = if (showListName) note.list else null,
                displayType = displayType
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
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

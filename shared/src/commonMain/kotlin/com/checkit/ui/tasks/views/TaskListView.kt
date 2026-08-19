package com.checkit.ui.tasks.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.checkit.domain.ListSection
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.ui.tasks.TaskListEntry
import com.checkit.ui.tasks.TaskListDisplayType
import com.checkit.ui.theme.toColor

@Composable
internal fun TaskListView(
    items: List<TaskListEntry>,
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

        items(items, key = { it.key }) { item ->
            when (item) {
                is TaskListEntry.Task -> {
                    val task = item.item
                    TaskRow(
                        task = task,
                        onClick = { onTaskClick(task) },
                        showList = showListName,
                        displayType = displayType
                    )
                }
                is TaskListEntry.Note -> {
                    val note = item.item
                    NoteRow(
                        note = note,
                        onClick = { onNoteClick(note) },
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
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionHeaderRow(section: ListSection?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        color = section?.color?.toColor()?.copy(alpha = 0.15f) ?: Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (section != null) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(section.color.toColor())
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
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

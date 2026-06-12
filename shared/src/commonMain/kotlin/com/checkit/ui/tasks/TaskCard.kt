package com.checkit.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem

@Composable
internal fun TaskCard(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    timeLabel: String? = null,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    minHeight: Dp = 64.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
    titleMaxLines: Int = 2,
    completedOverlay: Boolean = false,
    containerAlpha: Float = 0.11f
) {
    val shape = RoundedCornerShape(8.dp)
    val clickableModifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(clickableModifier)
    ) {
        CardStripe(color = color)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .background(color.copy(alpha = containerAlpha))
        ) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight())
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (leadingContent != null) {
                    Box(
                        modifier = Modifier.padding(top = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        leadingContent()
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = titleMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timeLabel ?: supportingText.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (completedOverlay) {
            CompletedOverlay()
        }
    }
}

@Composable
internal fun TaskTimelineCard(
    task: TaskItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    timeLabel: String? = task.timeRangeLabel(),
    selected: Boolean = false,
    completedOverlay: Boolean = false
) {
    TaskCard(
        title = task.name.ifBlank { "Untitled task" },
        timeLabel = timeLabel,
        color = task.cardColor(),
        leadingContent = {
            TaskStatusIcon(task.status, task.priority)
        },
        completedOverlay = completedOverlay,
        onClick = onClick,
        modifier = modifier,
        containerAlpha = if (selected) SelectedTaskCardAlpha else DefaultTaskCardAlpha,
        minHeight = 36.dp,
        titleMaxLines = 1,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    )
}

@Composable
internal fun NoteTimelineCard(
    note: NoteItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    timeLabel: String? = note.startTimeMinutes?.toClockLabel(),
    selected: Boolean = false,
    completedOverlay: Boolean = false
) {
    val title = note.title.ifBlank { note.content.ifBlank { "Empty note" } }
    val subtitle = if (note.title.isBlank()) timeLabel else timeLabel ?: note.content
    TaskCard(
        title = title,
        timeLabel = subtitle,
        color = note.cardColor(),
        leadingContent = {
            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        completedOverlay = completedOverlay,
        onClick = onClick,
        modifier = modifier,
        containerAlpha = if (selected) SelectedTaskCardAlpha else DefaultTaskCardAlpha,
        minHeight = 36.dp,
        titleMaxLines = 1,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    )
}

@Composable
internal fun DailyPlanTimelineCard(
    item: DailyPlanItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    title: String = item.timelineTitle(),
    timeLabel: String? = item.timelineTimeLabel() ?: item.timelineSupportingText(),
    selected: Boolean = false,
    completedOverlay: Boolean = false
) {
    TaskCard(
        title = title,
        timeLabel = timeLabel,
        color = DailyPlanCardColor,
        leadingContent = {
            Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        completedOverlay = completedOverlay,
        onClick = onClick,
        modifier = modifier,
        containerAlpha = if (selected) SelectedTaskCardAlpha else DefaultTaskCardAlpha,
        minHeight = 36.dp,
        titleMaxLines = 1,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    )
}

@Composable
internal fun AllDayTaskCard(
    task: TaskItem,
    modifier: Modifier = Modifier,
    completedOverlay: Boolean = false
) {
    AllDayTypeCard(
        title = task.name.ifBlank { "Untitled task" },
        color = task.cardColor(),
        icon = { TaskStatusIcon(task.status, task.priority) },
        modifier = modifier,
        completedOverlay = completedOverlay
    )
}

@Composable
internal fun AllDayNoteCard(
    note: NoteItem,
    modifier: Modifier = Modifier,
    completedOverlay: Boolean = false
) {
    AllDayTypeCard(
        title = note.title.ifBlank { note.content.ifBlank { "Empty note" } },
        color = note.cardColor(),
        icon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = modifier,
        completedOverlay = completedOverlay
    )
}

@Composable
internal fun AllDayDailyPlanCard(
    item: DailyPlanItem,
    modifier: Modifier = Modifier,
    title: String = item.timelineTitle(),
    completedOverlay: Boolean = false
) {
    AllDayTypeCard(
        title = title,
        color = DailyPlanCardColor,
        icon = { Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(18.dp)) },
        modifier = modifier,
        completedOverlay = completedOverlay
    )
}

@Composable
private fun AllDayTypeCard(
    title: String,
    color: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    completedOverlay: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = DefaultTaskCardAlpha))
    ) {
        CardStripe(color = color)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight())
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (completedOverlay) {
            CompletedOverlay()
        }
    }
}

@Composable
private fun CardStripe(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(4.dp)
            .background(color)
    )
}

@Composable
private fun BoxScope.CompletedOverlay() {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = CompletedRowCoverAlpha))
    )
}

private fun DailyPlanItem.timelineTimeLabel(): String? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes
    return if (end == null) start.toClockLabel() else "${start.toClockLabel()} - ${end.toClockLabel()}"
}

private fun DailyPlanItem.timelineTitle(): String =
    when (source) {
        DailyPlanItemSource.CheckInNote -> note.orEmpty().ifBlank { "Empty note" }
        else -> titleSnapshot.ifBlank { "Untitled item" }
    }

private fun DailyPlanItem.timelineSupportingText(): String =
    when {
        source == DailyPlanItemSource.CheckInNote -> source.timelineLabel()
        !note.isNullOrBlank() -> note.orEmpty()
        else -> source.timelineLabel()
    }

private fun DailyPlanItemSource.timelineLabel(): String = when (this) {
    DailyPlanItemSource.ExistingTask -> "Task"
    DailyPlanItemSource.CheckInManualDone -> "CheckIn done"
    DailyPlanItemSource.CheckInNote -> "CheckIn note"
}

private val DailyPlanCardColor = Color(0xFF64748B)

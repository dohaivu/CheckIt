package com.checkit.ui.tasks.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskStatus
import com.checkit.ui.tasks.DailyPlanIcon
import com.checkit.ui.tasks.NoteIcon
import com.checkit.ui.tasks.TaskIcon
import com.checkit.ui.tasks.cardColor
import com.checkit.ui.tasks.isOverdue
import com.checkit.ui.tasks.priorityColor
import com.checkit.ui.tasks.timeRangeLabel
import com.checkit.ui.tasks.toClockLabel

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
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    titleMaxLines: Int = 2,
    isHighlighted: Boolean = false,
    completedOverlay: Boolean = false,
    containerAlpha: Float = 0.11f,
    showSupportingText: Boolean = true,
    inlineSupportingText: String? = null,
    titleTextStyle: TextStyle? = null,
    inlineSupportingTextStyle: SpanStyle? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val resolvedInlineSupportingTextStyle = inlineSupportingTextStyle ?: remember(isHighlighted, colorScheme, typography) {
        SpanStyle(
            color = if (isHighlighted) {
                colorScheme.error
            } else {
                colorScheme.onSurfaceVariant
            },
            fontSize = typography.labelMedium.fontSize,
            fontWeight = FontWeight.Medium
        )
    }
    val titleText = remember(title, inlineSupportingText, resolvedInlineSupportingTextStyle) {
        buildAnnotatedString {
            append(title)
            if (!inlineSupportingText.isNullOrBlank()) {
                append("  ")
                withStyle(resolvedInlineSupportingTextStyle) {
                    append("· ")
                    append(inlineSupportingText)
                }
            }
        }
    }
    BaseTaskCard(
        color = color,
        modifier = modifier,
        onClick = onClick,
        minHeight = minHeight,
        containerAlpha = containerAlpha,
        completedOverlay = completedOverlay
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    text = titleText,
                    style = titleTextStyle ?: typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                if (showSupportingText) {
                    Text(
                        text = timeLabel ?: supportingText.orEmpty(),
                        style = typography.labelMedium,
                        color = if (isHighlighted) colorScheme.error else colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BaseTaskCard(
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    minHeight: Dp = Dp.Unspecified,
    containerAlpha: Float = 0.11f,
    completedOverlay: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(if (minHeight != Dp.Unspecified) Modifier.heightIn(min = minHeight) else Modifier)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .background(color.copy(alpha = containerAlpha))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.Top
        ) {
            CardStripe(color = color)
            content()
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
    completed: Boolean = task.status == TaskStatus.Completed,
    completedOverlay: Boolean = false,
    isOverdue: Boolean? = null,
    displayMode: TimelineItemDisplayMode = TimelineItemDisplayMode.Comfortable
) {
    val compact = displayMode != TimelineItemDisplayMode.Comfortable && !timeLabel.isNullOrBlank()
    val ultraCompact = displayMode == TimelineItemDisplayMode.UltraCompact
    val highlighted = isOverdue ?: task.isOverdue()
    TaskCard(
        title = task.name.ifBlank { "Untitled task" },
        timeLabel = timeLabel,
        color = task.cardColor(),
        leadingContent = {
            TaskIcon(
                completed = completed,
                color = task.priority.priorityColor()
            )
        },
        completedOverlay = completedOverlay,
        onClick = onClick,
        modifier = modifier,
        containerAlpha = if (selected) SelectedTaskCardAlpha else DefaultTaskCardAlpha,
        minHeight = if (ultraCompact) 0.dp else 36.dp,
        contentPadding = if (ultraCompact) {
            PaddingValues(horizontal = 9.dp, vertical = 1.dp)
        } else {
            PaddingValues(horizontal = 10.dp, vertical = 8.dp)
        },
        titleMaxLines = 2,
        isHighlighted = highlighted,
        showSupportingText = !compact,
        inlineSupportingText = timeLabel.takeIf { compact },
        titleTextStyle = if (ultraCompact) MaterialTheme.typography.bodyMedium else null,
        inlineSupportingTextStyle = if (ultraCompact) {
            SpanStyle(
                color = if (highlighted) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                fontWeight = FontWeight.Medium
            )
        } else {
            null
        }
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
            NoteIcon(status = note.status)
        },
        completedOverlay = completedOverlay,
        onClick = onClick,
        modifier = modifier,
        containerAlpha = if (selected) SelectedTaskCardAlpha else DefaultTaskCardAlpha,
        minHeight = 36.dp,
        titleMaxLines = 2,
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
    completedOverlay: Boolean = false,
    isOverdue: Boolean,
    displayMode: TimelineItemDisplayMode = TimelineItemDisplayMode.Comfortable
) {
    val compact = displayMode != TimelineItemDisplayMode.Comfortable && !timeLabel.isNullOrBlank()
    val ultraCompact = displayMode == TimelineItemDisplayMode.UltraCompact
    TaskCard(
        title = title,
        timeLabel = timeLabel,
        color =  item.cardColor(),
        leadingContent = {
            DailyPlanIcon(item.source, item.status == DailyPlanItemStatus.Done)
        },
        completedOverlay = completedOverlay,
        onClick = onClick,
        modifier = modifier,
        containerAlpha = if (selected) SelectedTaskCardAlpha else DefaultTaskCardAlpha,
        minHeight = if (ultraCompact) 0.dp else 36.dp,
        contentPadding = if (ultraCompact) {
            PaddingValues(horizontal = 9.dp, vertical = 1.dp)
        } else {
            PaddingValues(horizontal = 10.dp, vertical = 8.dp)
        },
        titleMaxLines = 2,
        isHighlighted = isOverdue,
        showSupportingText = !compact,
        inlineSupportingText = timeLabel.takeIf { compact },
        titleTextStyle = if (ultraCompact) MaterialTheme.typography.bodyMedium else null,
        inlineSupportingTextStyle = if (ultraCompact) {
            SpanStyle(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                fontWeight = FontWeight.Medium
            )
        } else {
            null
        }
    )
}

@Composable
internal fun TaskAllDayCard(
    task: TaskItem,
    modifier: Modifier = Modifier,
    completedOverlay: Boolean = false
) {
    AllDayTypeCard(
        title = task.name.ifBlank { "Untitled task" },
        color = task.cardColor(),
        icon = {
            TaskIcon(
                completed = task.status == TaskStatus.Completed,
                color = task.priority.priorityColor()
            )
        },
        modifier = modifier,
        completedOverlay = completedOverlay
    )
}

@Composable
internal fun NoteAllDayCard(
    note: NoteItem,
    modifier: Modifier = Modifier,
    completedOverlay: Boolean = false
) {
    AllDayTypeCard(
        title = note.title.ifBlank { note.content.ifBlank { "Empty note" } },
        color = note.cardColor(),
        icon = { NoteIcon(note.status) },
        modifier = modifier,
        completedOverlay = completedOverlay
    )
}

@Composable
internal fun DailyPlanAllDayCard(
    item: DailyPlanItem,
    modifier: Modifier = Modifier,
    title: String = item.timelineTitle(),
    completedOverlay: Boolean = false
) {
    AllDayTypeCard(
        title = title,
        color = item.cardColor(),
        icon = { DailyPlanIcon(item.source, item.status == DailyPlanItemStatus.Done) },
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
    BaseTaskCard(
        color = color,
        modifier = modifier.height(32.dp),
        containerAlpha = DefaultTaskCardAlpha,
        completedOverlay = completedOverlay
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
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
}

@Composable
internal fun CardStripe(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(4.dp)
            .background(color)
    )
}

@Composable
internal fun BoxScope.CompletedOverlay() {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = CompletedRowCoverAlpha))
    )
}

internal fun DailyPlanItem.timelineTimeLabel(): String? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes
    return if (end == null) start.toClockLabel() else "${start.toClockLabel()} - ${end.toClockLabel()}"
}

internal fun DailyPlanItem.timelineTitle(): String =
    when (source) {
        DailyPlanItemSource.MyDayNote,
        DailyPlanItemSource.MyDayReminder -> checkInNoteTitle()
        else -> title.ifBlank { "Untitled item" }
    }

internal fun DailyPlanItem.timelineSupportingText(): String =
    when {
        source == DailyPlanItemSource.MyDayNote || source == DailyPlanItemSource.MyDayReminder -> source.timelineLabel()
        !note.isNullOrBlank() -> note.orEmpty()
        else -> source.timelineLabel()
    }

internal fun DailyPlanItemSource.timelineLabel(): String = when (this) {
    DailyPlanItemSource.ExistingTask -> "Task"
    DailyPlanItemSource.MyDayTask -> "CheckIn done"
    DailyPlanItemSource.MyDayNote -> "CheckIn note"
    DailyPlanItemSource.MyDayReminder -> "Reminder"
}

internal fun DailyPlanItem.checkInNoteTitle(): String =
    title
        .ifBlank { note.orEmpty() }
        .ifBlank { "Empty note" }

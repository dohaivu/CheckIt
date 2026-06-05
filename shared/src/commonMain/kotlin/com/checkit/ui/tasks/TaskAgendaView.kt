package com.checkit.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.ui.shortName
import com.checkit.ui.shortMonthName
import com.checkit.ui.today
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

@Composable
internal fun TaskAgendaView(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    lists: List<TaskList>,
    showListName: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = today()
    val listById = remember(lists) { lists.associateBy { it.id } }
    val tasksByDate = remember(tasks) { tasks.filter { it.dueDate != null }.groupBy { it.dueDate } }
    val notesByDate = remember(notes) { notes.groupBy { it.date } }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = TodayIndex)
    val scope = rememberCoroutineScope()

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(
                count = AgendaDayCount,
                key = { index -> "agenda-day-${today.plus(index - TodayIndex, DateTimeUnit.DAY)}" }
            ) { index ->
                val date = today.plus(index - TodayIndex, DateTimeUnit.DAY)
                AgendaDaySection(
                    date = date,
                    today = today,
                    tasks = tasksByDate[date].orEmpty(),
                    notes = notesByDate[date].orEmpty(),
                    lists = listById,
                    showListName = showListName,
                    onTaskClick = onTaskClick,
                    onNoteClick = onNoteClick
                )
            }
        }

        FilledTonalButton(
            onClick = { scope.launch { state.animateScrollToItem(TodayIndex) } },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Today")
        }
    }
}

@Composable
private fun AgendaDaySection(
    date: LocalDate,
    today: LocalDate,
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    lists: Map<Long, TaskList>,
    showListName: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit
) {
    val allDayTasks = tasks
        .filter { it.startTimeMinutes == null }
        .sortedBy { it.sortOrder }
    val timedTasks = tasks
        .filter { it.startTimeMinutes != null }
        .sortedWith(compareBy<TaskItem> { it.startTimeMinutes ?: Int.MAX_VALUE }.thenBy { it.sortOrder })
    val dayNotes = notes.sortedBy { it.sortOrder }

    val hasAllDayItems = allDayTasks.isNotEmpty() || dayNotes.isNotEmpty()

    Column {
        AgendaDayHeader(date = date, today = today)
        Spacer(Modifier.height(8.dp))
        if (allDayTasks.isEmpty() && dayNotes.isEmpty() && timedTasks.isEmpty()) {
            AgendaEmptyDay()
        } else {
            if (hasAllDayItems) {
                AgendaAllDayRow(
                    tasks = allDayTasks,
                    notes = dayNotes,
                    lists = lists,
                    showListName = showListName,
                    showBottomLine = timedTasks.isNotEmpty(),
                    onTaskClick = onTaskClick,
                    onNoteClick = onNoteClick
                )
            }
            timedTasks.forEachIndexed { index, task ->
                AgendaTimedTaskRow(
                    task = task,
                    list = if (showListName) lists[task.listId] else null,
                    showTopLine = index > 0 || hasAllDayItems,
                    showBottomLine = index < timedTasks.lastIndex,
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
private fun AgendaDayHeader(date: LocalDate, today: LocalDate) {
    Text(
        text = if (date == today) "Today, ${date.agendaDateLabel()}" else date.agendaDateLabel(),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun AgendaEmptyDay() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AgendaAxisLabel(text = "")
        Spacer(Modifier.width(14.dp))
        Text(
            text = "No items",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AgendaAllDayRow(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    lists: Map<Long, TaskList>,
    showListName: Boolean,
    showBottomLine: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit
) {
    AgendaAxisRow(
        label = "All Day",
        showTopLine = false,
        showBottomLine = showBottomLine
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tasks.forEach { task ->
                AgendaCard(
                    title = task.name.ifBlank { "Untitled task" },
                    supportingText = if (showListName) lists[task.listId]?.name else null,
                    color = agendaTaskColor(task, lists[task.listId]),
                    onClick = { onTaskClick(task) }
                )
            }
            notes.forEach { note ->
                AgendaCard(
                    title = note.content.ifBlank { "Empty note" },
                    supportingText = if (showListName) lists[note.listId]?.name else null,
                    color = lists[note.listId]?.color?.toColor() ?: MaterialTheme.colorScheme.secondary,
                    icon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    onClick = { onNoteClick(note) }
                )
            }
        }
    }
}

@Composable
private fun AgendaTimedTaskRow(
    task: TaskItem,
    list: TaskList?,
    showTopLine: Boolean,
    showBottomLine: Boolean,
    onClick: () -> Unit
) {
    AgendaAxisRow(
        label = task.startTimeMinutes?.toClockLabel() ?: "",
        showTopLine = showTopLine,
        showBottomLine = showBottomLine
    ) {
        AgendaCard(
            title = task.name.ifBlank { "Untitled task" },
            timeLabel = task.timeRangeLabel(),
            supportingText = list?.name,
            color = agendaTaskColor(task, list),
            onClick = onClick
        )
    }
}

@Composable
private fun AgendaAxisRow(
    label: String,
    showTopLine: Boolean,
    showBottomLine: Boolean,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        AgendaAxisLabel(text = label)
        AgendaAxisMarker(showTopLine = showTopLine, showBottomLine = showBottomLine)
        Box(
            Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun AgendaAxisLabel(text: String) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(34.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun AgendaAxisMarker(
    showTopLine: Boolean,
    showBottomLine: Boolean
) {
    Box(
        modifier = Modifier
            .width(14.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (showTopLine) {
            Box(
                modifier = Modifier
                    .padding(top = 0.dp)
                    .width(1.dp)
                    .height(12.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        )
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline)
        )
        Box(
            modifier = Modifier
                .padding(top = 14.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        )
        if (showBottomLine) {
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

@Composable
private fun AgendaCard(
    title: String,
    color: Color,
    onClick: () -> Unit,
    timeLabel: String? = null,
    supportingText: String? = null,
    icon: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.11f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .heightIn(min = 64.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier.padding(top = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (timeLabel != null) {
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (supportingText != null) {
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun agendaTaskColor(task: TaskItem, list: TaskList?): Color =
    list?.color?.toColor() ?: task.priority.color()

private fun LocalDate.agendaDateLabel(): String =
    "${dayOfWeek.shortName()}, ${shortMonthName()} $day"

private const val AgendaDayCount = 20_001
private const val TodayIndex = AgendaDayCount / 2

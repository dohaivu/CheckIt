package com.checkit.ui.tasks

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskStatus
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
    dayLimit: Int? = null,
    modifier: Modifier = Modifier
) {
    val today = today()
    val listById = remember(lists) { lists.associateBy { it.id } }
    val tasksByDate = remember(tasks) { tasks.filter { it.dueDate != null }.groupBy { it.dueDate } }
    val notesByDate = remember(notes) { notes.groupBy { it.date } }
    val boundedDayCount = dayLimit?.coerceAtLeast(1)
    val initialIndex = if (boundedDayCount == null) TodayIndex else 0
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()

    LaunchedEffect(today, initialIndex) {
        state.scrollToItem(initialIndex)
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(
                count = boundedDayCount ?: AgendaDayCount,
                key = { index -> "agenda-day-${today.plus(index - initialIndex, DateTimeUnit.DAY)}" }
            ) { index ->
                val date = today.plus(index - initialIndex, DateTimeUnit.DAY)
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

        if (boundedDayCount != 1) {
            FilledTonalButton(
                onClick = { scope.launch { state.animateScrollToItem(initialIndex) } },
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
                    list = lists[task.listId],
                    showListName = showListName,
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
    val isToday = date == today
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isToday) "Today, ${date.agendaDateLabel()}" else date.agendaDateLabel(),
            modifier = if (isToday) {
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            } else {
                Modifier
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isToday) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
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
                TaskCard(
                    title = task.name.ifBlank { "Untitled task" },
                    supportingText = if (showListName) lists[task.listId]?.name else null,
                    color = taskCardColor(task, lists[task.listId]),
                    completed = task.status == TaskStatus.Completed,
                    onClick = { onTaskClick(task) }
                )
            }
            notes.forEach { note ->
                TaskCard(
                    title = note.content.ifBlank { "Empty note" },
                    supportingText = if (showListName) lists[note.listId]?.name else null,
                    color = lists[note.listId]?.color?.toColor() ?: MaterialTheme.colorScheme.secondary,
                    leadingContent = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    completed = note.status == TaskStatus.Completed,
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
    showListName: Boolean,
    showTopLine: Boolean,
    showBottomLine: Boolean,
    onClick: () -> Unit
) {
    AgendaAxisRow(
        label = task.startTimeMinutes?.toClockLabel() ?: "",
        showTopLine = showTopLine,
        showBottomLine = showBottomLine
    ) {
        TaskCard(
            title = task.name.ifBlank { "Untitled task" },
            timeLabel = task.timeRangeLabel(),
            supportingText = if (showListName) list?.name else null,
            color = taskCardColor(task, list),
            completed = task.status == TaskStatus.Completed,
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

private fun LocalDate.agendaDateLabel(): String =
    "${dayOfWeek.shortName()}, ${shortMonthName()} $day"

private const val AgendaDayCount = 20_001
private const val TodayIndex = AgendaDayCount / 2

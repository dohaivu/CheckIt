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
import androidx.compose.material.icons.automirrored.filled.Notes
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun TaskAgendaView(
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    lists: List<TaskList>,
    showListName: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit,
    dayLimit: Int? = null,
    focusedDate: LocalDate = today(),
    modifier: Modifier = Modifier
) {
    val today = today()
    val listById = remember(lists) { lists.associateBy { it.id } }
    val tasksByDate = remember(tasks) { tasks.filter { it.doDate != null }.groupBy { it.doDate } }
    val notesByDate = remember(notes) { notes.groupBy { it.date } }
    val boundedDayCount = dayLimit?.coerceAtLeast(1)
    val initialIndex = if (boundedDayCount == null) TodayIndex else 0
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()

    LaunchedEffect(focusedDate, initialIndex) {
        state.scrollToItem(initialIndex)
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(
                count = boundedDayCount ?: AgendaDayCount,
                key = { index -> "agenda-day-${focusedDate.plus(index - initialIndex, DateTimeUnit.DAY)}" }
            ) { index ->
                val date = focusedDate.plus(index - initialIndex, DateTimeUnit.DAY)
                AgendaDaySection(
                    date = date,
                    today = today,
                    tasks = tasksByDate[date].orEmpty(),
                    notes = notesByDate[date].orEmpty(),
                    lists = listById,
                    showListName = showListName,
                    showHeader = boundedDayCount != 1,
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

private sealed class TimedAgendaItem {
    abstract val startTimeMinutes: Int
    abstract val sortOrder: Int

    data class Task(val task: TaskItem) : TimedAgendaItem() {
        override val startTimeMinutes: Int = task.startTimeMinutes!!
        override val sortOrder: Int = task.sortOrder
    }

    data class Note(val note: NoteItem) : TimedAgendaItem() {
        override val startTimeMinutes: Int = note.startTimeMinutes!!
        override val sortOrder: Int = note.sortOrder
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
    showHeader: Boolean,
    onTaskClick: (TaskItem) -> Unit,
    onNoteClick: (NoteItem) -> Unit
) {
    val allDayTasks = tasks
        .filter { it.startTimeMinutes == null }
        .sortedBy { it.sortOrder }
    val timedTasks = tasks
        .filter { it.startTimeMinutes != null }
    val allDayNotes = notes
        .filter { it.startTimeMinutes == null }
        .sortedBy { it.sortOrder }
    val timedNotes = notes
        .filter { it.startTimeMinutes != null }

    val timedItems = (timedTasks.map { TimedAgendaItem.Task(it) } + timedNotes.map { TimedAgendaItem.Note(it) })
        .sortedWith(compareBy<TimedAgendaItem> { it.startTimeMinutes }.thenBy { it.sortOrder })

    val hasAllDayItems = allDayTasks.isNotEmpty() || allDayNotes.isNotEmpty()
    val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val nowMinutes = now.hour * 60 + now.minute
    val nextTimedItemIndex = remember(timedItems, nowMinutes) {
        timedItems.indexOfFirst { (it.startTimeMinutes) > nowMinutes }
    }

    Column {
        if (showHeader) {
            AgendaDayHeader(date = date, today = today)
            Spacer(Modifier.height(8.dp))
        }
        if (allDayTasks.isEmpty() && allDayNotes.isEmpty() && timedItems.isEmpty()) {
            AgendaEmptyDay()
        } else {
            if (hasAllDayItems) {
                AgendaAllDayRow(
                    tasks = allDayTasks,
                    notes = allDayNotes,
                    lists = lists,
                    showListName = showListName,
                    showBottomLine = timedItems.isNotEmpty(),
                    onTaskClick = onTaskClick,
                    onNoteClick = onNoteClick
                )
            }
            timedItems.forEachIndexed { index, item ->
                when (item) {
                    is TimedAgendaItem.Task -> {
                        AgendaTimedTaskRow(
                            task = item.task,
                            list = lists[item.task.listId],
                            showListName = showListName,
                            showTopLine = index > 0 || hasAllDayItems,
                            showBottomLine = index < timedItems.lastIndex,
                            isHighlighted = index == nextTimedItemIndex,
                            onClick = { onTaskClick(item.task) }
                        )
                    }
                    is TimedAgendaItem.Note -> {
                        AgendaTimedNoteRow(
                            note = item.note,
                            list = lists[item.note.listId],
                            showListName = showListName,
                            showTopLine = index > 0 || hasAllDayItems,
                            showBottomLine = index < timedItems.lastIndex,
                            isHighlighted = index == nextTimedItemIndex,
                            onClick = { onNoteClick(item.note) }
                        )
                    }
                }
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
        AgendaAxisLabel(text = "", isHighlighted = false)
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
        showBottomLine = showBottomLine,
        isHighlighted = false
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tasks.forEach { task ->
                TaskCard(
                    title = task.name.ifBlank { "Untitled task" },
                    supportingText = if (showListName) lists[task.listId]?.name else null,
                    leadingContent = { TaskStatusIcon(task.status, task.priority) },
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
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) },
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
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    AgendaAxisRow(
        label = task.startTimeMinutes?.toClockLabel() ?: "",
        showTopLine = showTopLine,
        showBottomLine = showBottomLine,
        isHighlighted = isHighlighted
    ) {
        TaskCard(
            title = task.name.ifBlank { "Untitled task" },
            timeLabel = task.timeRangeLabel(),
            supportingText = if (showListName) list?.name else null,
            color = taskCardColor(task, list),
            leadingContent = { TaskStatusIcon(task.status, task.priority) },
            completed = task.status == TaskStatus.Completed,
            onClick = onClick
        )
    }
}

@Composable
private fun AgendaTimedNoteRow(
    note: NoteItem,
    list: TaskList?,
    showListName: Boolean,
    showTopLine: Boolean,
    showBottomLine: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    AgendaAxisRow(
        label = note.startTimeMinutes?.toClockLabel() ?: "",
        showTopLine = showTopLine,
        showBottomLine = showBottomLine,
        isHighlighted = isHighlighted
    ) {
        TaskCard(
            title = note.content.ifBlank { "Empty note" },
            timeLabel = note.startTimeMinutes?.toClockLabel(),
            supportingText = if (showListName) list?.name else null,
            color = list?.color?.toColor() ?: MaterialTheme.colorScheme.secondary,
            leadingContent = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) },
            completed = note.status == TaskStatus.Completed,
            onClick = onClick
        )
    }
}

@Composable
private fun AgendaAxisRow(
    label: String,
    showTopLine: Boolean,
    showBottomLine: Boolean,
    isHighlighted: Boolean,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        AgendaAxisLabel(text = label, isHighlighted)
        AgendaAxisMarker(showTopLine = showTopLine, showBottomLine = showBottomLine, isHighlighted = isHighlighted)
        Box(
            Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun AgendaAxisLabel(text: String, isHighlighted: Boolean) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(34.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = text,
            style = if (isHighlighted) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun AgendaAxisMarker(
    showTopLine: Boolean,
    showBottomLine: Boolean,
    isHighlighted: Boolean,
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
                .background(if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
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

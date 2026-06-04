package com.checkit.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.tab_tasks
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.ui.TaskUiState
import com.checkit.ui.TaskWorkspaceView
import com.checkit.ui.components.TinyTopAppBar
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TaskScreen(
    state: TaskUiState,
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                TaskSidebar(
                    lists = state.board.lists,
                    filters = state.board.filters,
                    selectedListId = state.selectedListId,
                    selectedFilterId = state.selectedFilterId,
                    onListClick = { listId ->
                        viewModel.selectList(listId)
                        scope.launch { drawerState.close() }
                    },
                    onFilterClick = { filterId ->
                        viewModel.selectFilter(filterId)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TinyTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open sidebar")
                        }
                    },
                    title = {
                        Text(
                            stringResource(Res.string.tab_tasks),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        TaskWorkspaceView.entries.forEach { view ->
                            IconButton(onClick = { viewModel.selectView(view) }) {
                                Icon(
                                    imageVector = view.icon(),
                                    contentDescription = view.name,
                                    tint = if (state.selectedView == view) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (state.isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                TaskContent(
                    state = state,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
        }
    }
}

@Composable
private fun TaskSidebar(
    lists: List<TaskList>,
    filters: List<TaskFilter>,
    selectedListId: Long?,
    selectedFilterId: Long?,
    onListClick: (Long) -> Unit,
    onFilterClick: (Long) -> Unit
) {
    Surface(
        modifier = Modifier.width(260.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { SidebarHeader("Lists") }
            items(lists, key = { "list-${it.id}" }) { list ->
                SidebarItem(
                    title = list.name,
                    icon = materialIcon(list.icon),
                    color = list.color.toColor(),
                    selected = selectedListId == list.id,
                    onClick = { onListClick(list.id) }
                )
            }
            item {
                Spacer(Modifier.height(10.dp))
                SidebarHeader("Filters")
            }
            items(filters, key = { "filter-${it.id}" }) { filter ->
                SidebarItem(
                    title = filter.name,
                    icon = materialIcon(filter.icon),
                    color = filter.color.toColor(),
                    selected = selectedFilterId == filter.id,
                    onClick = { onFilterClick(filter.id) }
                )
            }
        }
    }
}

@Composable
private fun SidebarHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun SidebarItem(
    title: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun TaskContent(
    state: TaskUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${state.visibleTasks.size} tasks · ${state.visibleNotes.size} notes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        when (state.selectedView) {
            TaskWorkspaceView.List -> ListView(state.visibleTasks, state.visibleNotes)
            TaskWorkspaceView.Agenda -> AgendaView(state.visibleTasks)
            TaskWorkspaceView.Timeline -> TimelineView(state.visibleTasks)
        }
    }
}

@Composable
private fun ListView(tasks: List<TaskItem>, notes: List<NoteItem>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tasks, key = { "task-${it.id}" }) { task -> TaskRow(task) }
        items(notes, key = { "note-${it.id}" }) { note -> NoteRow(note) }
    }
}

@Composable
private fun AgendaView(tasks: List<TaskItem>) {
    val grouped = tasks.groupBy { it.dueDate }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        grouped.forEach { (date, dayTasks) ->
            item(key = "date-$date") {
                Text(
                    text = date?.compact() ?: "No date",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(dayTasks, key = { "agenda-${it.id}" }) { task -> TaskRow(task) }
        }
    }
}

@Composable
private fun TimelineView(tasks: List<TaskItem>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tasks.sortedWith(compareBy<TaskItem> { it.startTimeMinutes ?: Int.MAX_VALUE }.thenBy { it.sortOrder }), key = { "timeline-${it.id}" }) { task ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Box(Modifier.width(1.dp).height(68.dp).background(MaterialTheme.colorScheme.outlineVariant))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = task.timeRangeLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TaskRow(task)
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskItem) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = if (task.status == TaskStatus.Completed) Icons.Default.CheckCircle else Icons.Default.TaskAlt,
                    contentDescription = null,
                    tint = task.priority.color(),
                    modifier = Modifier.size(22.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (task.description.isNotBlank()) {
                        Text(
                            task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(task.status.name, style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                task.dueDate?.let { CompactChip(Icons.Default.Event, it.compact()) }
                task.durationMinutes?.let { CompactChip(Icons.Default.Schedule, "${it}m") }
                if (task.repeatRRule != null) CompactChip(Icons.Default.MoreTime, "Repeats")
            }
            task.subtasks.takeIf { it.isNotEmpty() }?.let { subtasks ->
                Text(
                    text = "${subtasks.count { it.isCompleted }}/${subtasks.size} subtasks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                task.tags.forEach { tag ->
                    CompactChip(materialIcon(tag.icon), tag.name)
                }
            }
        }
    }
}

@Composable
private fun NoteRow(note: NoteItem) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(note.content, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    note.tags.forEach { tag -> CompactChip(materialIcon(tag.icon), tag.name) }
                }
            }
        }
    }
}

@Composable
private fun CompactChip(icon: ImageVector, label: String) {
    AssistChip(
        onClick = {},
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}

private fun TaskWorkspaceView.icon(): ImageVector = when (this) {
    TaskWorkspaceView.List -> Icons.Default.ViewList
    TaskWorkspaceView.Agenda -> Icons.Default.ViewAgenda
    TaskWorkspaceView.Timeline -> Icons.Default.Schedule
}

private fun materialIcon(name: String): ImageVector = when (name) {
    "Delete" -> Icons.Default.Delete
    "Home" -> Icons.Default.Home
    "Inbox" -> Icons.Default.Inbox
    "Notes" -> Icons.Default.Notes
    "PriorityHigh" -> Icons.Default.PriorityHigh
    "Schedule" -> Icons.Default.Schedule
    "TaskAlt" -> Icons.Default.TaskAlt
    "Today" -> Icons.Default.Today
    "Work" -> Icons.Default.Work
    else -> Icons.Default.LocalOffer
}

private fun TaskPriority.color(): Color = when (this) {
    TaskPriority.None -> Color(0xFF64748B)
    TaskPriority.Low -> Color(0xFF0891B2)
    TaskPriority.Medium -> Color(0xFFCA8A04)
    TaskPriority.High -> Color(0xFFDC2626)
    TaskPriority.Urgent -> Color(0xFF9333EA)
}

private fun String.toColor(): Color =
    removePrefix("#")
        .toIntOrNull(16)
        ?.let { rgb ->
            Color(
                red = ((rgb shr 16) and 0xFF) / 255f,
                green = ((rgb shr 8) and 0xFF) / 255f,
                blue = (rgb and 0xFF) / 255f
            )
        }
        ?: Color(0xFF64748B)

private fun LocalDate.compact(): String = "${month.number}/$day/$year"

private fun TaskItem.timeRangeLabel(): String {
    val start = startTimeMinutes?.toClockLabel() ?: "Any time"
    val end = endTimeMinutes?.toClockLabel()
    return if (end == null) start else "$start - $end"
}

private fun Int.toClockLabel(): String {
    val hour = this / 60
    val minute = this % 60
    val suffix = if (hour >= 12) "PM" else "AM"
    val displayHour = when (val normalized = hour % 12) {
        0 -> 12
        else -> normalized
    }
    return "$displayHour:${minute.toString().padStart(2, '0')} $suffix"
}

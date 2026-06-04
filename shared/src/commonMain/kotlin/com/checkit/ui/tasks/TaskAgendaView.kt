package com.checkit.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList

@Composable
internal fun TaskAgendaView(
    tasks: List<TaskItem>,
    lists: List<TaskList>,
    showListName: Boolean,
    onTaskClick: (TaskItem) -> Unit
) {
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
            items(dayTasks, key = { "agenda-${it.id}" }) { task ->
                TaskRow(
                    task = task,
                    onClick = { onTaskClick(task) },
                    list = if (showListName) lists.firstOrNull { it.id == task.listId } else null
                )
            }
        }
    }
}

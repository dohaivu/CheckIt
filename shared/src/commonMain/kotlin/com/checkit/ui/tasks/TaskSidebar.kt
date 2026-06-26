package com.checkit.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.domain.Goal
import com.checkit.domain.Objective
import com.checkit.domain.TaskTag
import com.checkit.ui.theme.materialIcon
import com.checkit.ui.theme.toColor

@Composable
internal fun TaskSidebar(
    goals: List<Goal>,
    lists: List<Objective>,
    tags: List<TaskTag>,
    isBoardSelected: Boolean,
    selectedListId: Long?,
    selectedGoalId: Long?,
    selectedTagId: Long?,
    onBoardClick: () -> Unit,
    onGoalClick: (Long) -> Unit,
    onListClick: (Long) -> Unit,
    onTagClick: (Long) -> Unit,
    onAddGoalClick: () -> Unit,
    onEditGoalClick: (Goal) -> Unit,
    onAddListClick: () -> Unit,
    onEditListClick: (Objective) -> Unit,
    onAddTagClick: () -> Unit,
    onEditTagClick: (TaskTag) -> Unit
) {
    Surface(
        modifier = Modifier.width(260.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                SidebarHeader("Board")
            }
            item {
                SidebarItem(
                    title = "All tasks",
                    icon = materialIcon("AllInclusive"),
                    color = MaterialTheme.colorScheme.primary,
                    selected = isBoardSelected,
                    onClick = onBoardClick
                )
            }
            item {
                Spacer(Modifier.height(10.dp))
                SidebarHeader(
                    text = "Goals",
                    action = {
                        IconButton(onClick = onAddGoalClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add goal",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }
            items(goals, key = { "goal-${it.id}" }) { goal ->
                SidebarItem(
                    title = goal.title,
                    icon = materialIcon(goal.icon),
                    color = goal.color.toColor(),
                    selected = selectedGoalId == goal.id,
                    onClick = { onGoalClick(goal.id) },
                    onLongClick = {
                        onEditGoalClick(goal)
                    }
                )
            }
            item {
                Spacer(Modifier.height(10.dp))
                SidebarHeader(
                    text = "Lists",
                    action = {
                        IconButton(onClick = onAddListClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add list",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }
            items(lists, key = { "list-${it.id}" }) { list ->
                SidebarItem(
                    title = list.name,
                    icon = materialIcon(list.icon),
                    color = list.color.toColor(),
                    selected = selectedListId == list.id,
                    onClick = { onListClick(list.id) },
                    onLongClick = {
                        onEditListClick(list)
                    }
                )
            }
            item {
                Spacer(Modifier.height(10.dp))
                SidebarHeader(
                    text = "Tags",
                    action = {
                        IconButton(onClick = onAddTagClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add tag",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }
            items(tags, key = { "tag-${it.id}" }) { tag ->
                SidebarItem(
                    title = tag.name,
                    icon = null,
                    color = tag.color.toColor(),
                    selected = selectedTagId == tag.id,
                    onClick = { onTagClick(tag.id) },
                    onLongClick = {
                        onEditTagClick(tag)
                    }
                )
            }
        }
    }
}

@Composable
private fun SidebarHeader(
    text: String,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

@Composable
private fun SidebarItem(
    title: String,
    icon: ImageVector?,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .pointerInput(title) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick?.invoke() }
                )
            }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        } else {
            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        }
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

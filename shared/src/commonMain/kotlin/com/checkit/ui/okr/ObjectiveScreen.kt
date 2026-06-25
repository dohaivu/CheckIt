package com.checkit.ui.okr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.checkit.domain.Goal
import com.checkit.domain.KeyResult
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList

@Composable
internal fun ObjectiveScreen(
    goal: Goal,
    board: TaskBoard,
    viewModel: ObjectiveViewModel,
    onTaskClick: (TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val objectives = remember(board.lists, goal.id) {
        board.lists
            .filter { it.goalId == goal.id }
            .sortedWith(compareBy<TaskList> { it.sortOrder }.thenBy { it.name })
    }
    val keyResultsByObjective = remember(board.keyResults) {
        board.keyResults
            .sortedWith(compareBy<KeyResult> { it.sortOrder }.thenBy { it.title })
            .groupBy { it.objectiveId }
    }
    val tasksByKeyResult = remember(board.tasks) {
        board.tasks
            .filter { !it.isTrashed && it.keyResult != null }
            .sortedWith(compareBy<TaskItem> { it.sortOrder }.thenBy { it.name })
            .groupBy { it.keyResult?.id }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(objectives, key = { "objective-${it.id}" }) { objective ->
            ObjectiveBranch(
                objective = objective,
                keyResults = keyResultsByObjective[objective.id].orEmpty(),
                tasksByKeyResult = tasksByKeyResult,
                expandedNodeKeys = state.expandedNodeKeys,
                selectedNodeKey = state.selectedNodeKey,
                onToggleExpanded = viewModel::toggleExpanded,
                onSelectNode = viewModel::selectNode,
                onTaskClick = onTaskClick,
                onAddKeyResult = { objective ->
                    viewModel.openNewKeyResult(objective.id)
                }
            )
        }
    }

    state.keyResultEditor?.let { editor ->
        KeyResultEditorSheet(
            editor = editor,
            onDismiss = viewModel::dismissKeyResultEditor,
            onSave = viewModel::saveKeyResultEditor,
            onTitleChange = viewModel::updateKeyResultTitle,
            onTargetValueChange = viewModel::updateKeyResultTargetValue,
            onCurrentValueChange = viewModel::updateKeyResultCurrentValue,
            onUnitChange = viewModel::updateKeyResultUnit
        )
    }
}

@Composable
private fun ObjectiveBranch(
    objective: TaskList,
    keyResults: List<KeyResult>,
    tasksByKeyResult: Map<Long?, List<TaskItem>>,
    expandedNodeKeys: Set<String>,
    selectedNodeKey: String?,
    onToggleExpanded: (String) -> Unit,
    onSelectNode: (String) -> Unit,
    onTaskClick: (TaskItem) -> Unit,
    onAddKeyResult: (TaskList) -> Unit
) {
    val nodeKey = objective.nodeKey()
    val isExpanded = nodeKey in expandedNodeKeys
    TreeNodeRow(
        text = objective.name,
        nodeKey = nodeKey,
        depth = 0,
        hasChildren = keyResults.isNotEmpty(),
        isExpanded = isExpanded,
        isSelected = selectedNodeKey == nodeKey,
        onToggleExpanded = onToggleExpanded,
        onSelectNode = onSelectNode,
        onAddClick = {
            onAddKeyResult(objective)
        }
    )
    AnimatedChildren(visible = isExpanded && keyResults.isNotEmpty(), depth = 1) {
        keyResults.forEach { keyResult ->
            KeyResultBranch(
                keyResult = keyResult,
                tasks = tasksByKeyResult[keyResult.id].orEmpty(),
                expandedNodeKeys = expandedNodeKeys,
                selectedNodeKey = selectedNodeKey,
                onToggleExpanded = onToggleExpanded,
                onSelectNode = onSelectNode,
                onTaskClick = onTaskClick
            )
        }
    }
}

@Composable
private fun KeyResultBranch(
    keyResult: KeyResult,
    tasks: List<TaskItem>,
    expandedNodeKeys: Set<String>,
    selectedNodeKey: String?,
    onToggleExpanded: (String) -> Unit,
    onSelectNode: (String) -> Unit,
    onTaskClick: (TaskItem) -> Unit
) {
    val nodeKey = keyResult.nodeKey()
    val isExpanded = nodeKey in expandedNodeKeys
    TreeNodeRow(
        text = keyResult.title,
        nodeKey = nodeKey,
        depth = 1,
        hasChildren = tasks.isNotEmpty(),
        isExpanded = isExpanded,
        isSelected = selectedNodeKey == nodeKey,
        onToggleExpanded = onToggleExpanded,
        onSelectNode = onSelectNode
    )
    AnimatedChildren(visible = isExpanded && tasks.isNotEmpty(), depth = 2) {
        tasks.forEach { task ->
            val taskNodeKey = task.nodeKey()
            TreeNodeRow(
                text = task.name,
                nodeKey = taskNodeKey,
                depth = 2,
                hasChildren = false,
                isExpanded = false,
                isSelected = selectedNodeKey == taskNodeKey,
                onToggleExpanded = onToggleExpanded,
                onSelectNode = {
                    onSelectNode(it)
                    onTaskClick(task)
                }
            )
        }
    }
}

@Composable
private fun AnimatedChildren(
    visible: Boolean,
    depth: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TreeGuideLine(depth = depth)
            Column(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}

@Composable
private fun TreeNodeRow(
    text: String,
    nodeKey: String,
    depth: Int,
    hasChildren: Boolean,
    isExpanded: Boolean,
    isSelected: Boolean,
    onToggleExpanded: (String) -> Unit,
    onSelectNode: (String) -> Unit,
    onAddClick: (()-> Unit)? = null
) {
    val background = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = nodeIndent(depth), top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable { onSelectNode(nodeKey) }
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasChildren) {
            IconButton(
                onClick = { onToggleExpanded(nodeKey) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.KeyboardArrowDown
                    } else {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    },
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Spacer(Modifier.size(36.dp))
        }
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(vertical = 9.dp)
        )
        if (isSelected && onAddClick != null) {
            IconButton(onClick = onAddClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TreeGuideLine(depth: Int) {
    Box(
        modifier = Modifier
            .padding(start = guideLineStart(depth))
            .size(width = 1.dp, height = 1.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
    )
}

private fun nodeIndent(depth: Int): Dp = (depth * 18).dp

private fun guideLineStart(depth: Int): Dp = ((depth - 1).coerceAtLeast(0) * 18 + 18).dp

private fun TaskList.nodeKey(): String = "objective-$id"

private fun KeyResult.nodeKey(): String = "key-result-$id"

private fun TaskItem.nodeKey(): String = "task-$id"

package com.checkit.ui.okr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.checkit.domain.Goal
import com.checkit.domain.KeyResult
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.Objective
import com.checkit.ui.components.icons.AppIcons
import com.checkit.ui.components.icons.Target
import com.checkit.ui.theme.toColor

@Composable
internal fun GoalScreen(
    goal: Goal,
    board: TaskBoard,
    goalViewModel: GoalViewModel,
    keyResultViewModel: KeyResultViewModel,
    onTaskClick: (TaskItem) -> Unit,
    onAddTask: (KeyResult) -> Unit,
    onEditObjective: (Objective) -> Unit,
    modifier: Modifier = Modifier
) {
    val goalState by goalViewModel.uiState.collectAsState()
    val keyResultState by keyResultViewModel.uiState.collectAsState()
    val objectives = remember(board.objectives, goal.id) {
        board.objectives
            .filter { it.goalId == goal.id }
            .sortedWith(compareBy<Objective> { it.sortOrder }.thenBy { it.name })
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
                collapsedNodeKeys = goalState.collapsedNodeKeys,
                selectedNodeKey = goalState.selectedNodeKey,
                onToggleExpanded = goalViewModel::toggleExpanded,
                onSelectNode = goalViewModel::selectNode,
                onTaskClick = onTaskClick,
                onAddKeyResult = { objective ->
                    keyResultViewModel.openNewKeyResult(objective.id)
                },
                onAddTask = onAddTask,
                onEditKeyResult = keyResultViewModel::openEditKeyResult,
                onEditObjective = onEditObjective
            )
        }
    }

    keyResultState.keyResultEditor?.let { editor ->
        KeyResultEditorSheet(
            editor = editor,
            onDismiss = keyResultViewModel::dismissKeyResultEditor,
            onSave = keyResultViewModel::saveKeyResultEditor,
            onDelete = keyResultViewModel::deleteKeyResultEditor,
            onTitleChange = keyResultViewModel::updateKeyResultTitle,
            onTargetValueChange = keyResultViewModel::updateKeyResultTargetValue,
            onCurrentValueChange = keyResultViewModel::updateKeyResultCurrentValue,
            onUnitChange = keyResultViewModel::updateKeyResultUnit
        )
    }
}

@Composable
private fun ObjectiveBranch(
    objective: Objective,
    keyResults: List<KeyResult>,
    tasksByKeyResult: Map<Long?, List<TaskItem>>,
    collapsedNodeKeys: Set<String>,
    selectedNodeKey: String?,
    onToggleExpanded: (String) -> Unit,
    onSelectNode: (String) -> Unit,
    onTaskClick: (TaskItem) -> Unit,
    onAddKeyResult: (Objective) -> Unit,
    onAddTask: (KeyResult) -> Unit,
    onEditKeyResult: (KeyResult) -> Unit,
    onEditObjective: (Objective) -> Unit
) {
    val nodeKey = objective.nodeKey()
    val isExpanded = nodeKey !in collapsedNodeKeys
    val color = objective.color.toColor()
    TreeNodeRow(
        text = objective.name,
        nodeKey = nodeKey,
        depth = 0,
        isLast = true, // Root objectives are independent
        hasChildren = keyResults.isNotEmpty(),
        isExpanded = isExpanded,
        isSelected = selectedNodeKey == nodeKey,
        color = color,
        onToggleExpanded = onToggleExpanded,
        onSelectNode = onSelectNode,
        onLongClick = { onEditObjective(objective) },
        onAddClick = {
            onAddKeyResult(objective)
        },
        leadingContent = {
            Icon(
                imageVector = AppIcons.Target,
                contentDescription = "target",
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    )
    AnimatedChildren(visible = isExpanded && keyResults.isNotEmpty()) {
        keyResults.forEachIndexed { index, keyResult ->
            KeyResultBranch(
                keyResult = keyResult,
                tasks = tasksByKeyResult[keyResult.id].orEmpty(),
                collapsedNodeKeys = collapsedNodeKeys,
                selectedNodeKey = selectedNodeKey,
                isLast = index == keyResults.lastIndex,
                color = color,
                onToggleExpanded = onToggleExpanded,
                onSelectNode = onSelectNode,
                onTaskClick = onTaskClick,
                onAddTask = onAddTask,
                onEditKeyResult = onEditKeyResult
            )
        }
    }
}

@Composable
private fun KeyResultBranch(
    keyResult: KeyResult,
    tasks: List<TaskItem>,
    collapsedNodeKeys: Set<String>,
    selectedNodeKey: String?,
    isLast: Boolean,
    color: Color? = null,
    onToggleExpanded: (String) -> Unit,
    onSelectNode: (String) -> Unit,
    onTaskClick: (TaskItem) -> Unit,
    onAddTask: (KeyResult) -> Unit,
    onEditKeyResult: (KeyResult) -> Unit
) {
    val nodeKey = keyResult.nodeKey()
    val isExpanded = nodeKey !in collapsedNodeKeys
    TreeNodeRow(
        text = keyResult.title,
        nodeKey = nodeKey,
        depth = 1,
        isLast = isLast,
        hasChildren = tasks.isNotEmpty(),
        isExpanded = isExpanded,
        isSelected = selectedNodeKey == nodeKey,
        color = color,
        onToggleExpanded = onToggleExpanded,
        onSelectNode = onSelectNode,
        onLongClick = { onEditKeyResult(keyResult) },
        onAddClick = {
            onAddTask(keyResult)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${(keyResult.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                LinearProgressIndicator(
                    progress = { keyResult.progress.toFloat() },
                    modifier = Modifier.width(48.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    )
    
    // Continue Objective's line through tasks if this KR is not the last one
    val ancestorLines = if (isLast) emptyList() else listOf(guideLineStart(1))
    
    AnimatedChildren(visible = isExpanded && tasks.isNotEmpty()) {
        tasks.forEachIndexed { index, task ->
            val taskNodeKey = task.nodeKey()
            TreeNodeRow(
                text = task.name,
                nodeKey = taskNodeKey,
                depth = 2,
                isLast = index == tasks.lastIndex,
                hasChildren = false,
                isExpanded = false,
                isSelected = selectedNodeKey == taskNodeKey,
                color = color,
                onToggleExpanded = onToggleExpanded,
                onSelectNode = onSelectNode,
                onLongClick = { onTaskClick(task) },
                ancestorLines = ancestorLines
            )
        }
    }
}


@Composable
private fun AnimatedChildren(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun TreeNodeRow(
    text: String,
    nodeKey: String,
    depth: Int,
    isLast: Boolean,
    hasChildren: Boolean,
    isExpanded: Boolean,
    isSelected: Boolean,
    color: Color? = null,
    onToggleExpanded: (String) -> Unit,
    onSelectNode: (String) -> Unit,
    onLongClick: (() -> Unit)? = null,
    onAddClick: (()-> Unit)? = null,
    ancestorLines: List<Dp> = emptyList(),
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val background = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    val lineColor = color ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    val rotation by animateFloatAsState(if (isExpanded) 90f else 0f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val strokePx = 1.2.dp.toPx() // Slightly thinner for more elegance
                val curveRadius = 12.dp.toPx() // Larger radius for more organic feel
                val dashColor = lineColor.copy(alpha = 0.4f)

                // Draw vertical lines for ancestors
                ancestorLines.forEach { xDp ->
                    val x = xDp.toPx()
                    drawLine(
                        color = dashColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }

                if (depth > 0) {
                    val x = guideLineStart(depth).toPx()
                    val yCenter = size.height / 2
                    val xIconCenter = (nodeIndent(depth) + 18.dp).toPx()
                    
                    if (isLast) {
                        // Rounded L-shape connector
                        val path = Path().apply {
                            moveTo(x, 0f)
                            lineTo(x, yCenter - curveRadius)
                            quadraticTo(x, yCenter, x + curveRadius, yCenter)
                            lineTo(xIconCenter, yCenter)
                        }
                        drawPath(
                            path = path,
                            color = dashColor,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    } else {
                        // Vertical line passing through
                        drawLine(
                            color = dashColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                        // Rounded branch connector
                        val path = Path().apply {
                            moveTo(x, yCenter - curveRadius)
                            quadraticTo(x, yCenter, x + curveRadius, yCenter)
                            lineTo(xIconCenter, yCenter)
                        }
                        drawPath(
                            path = path,
                            color = dashColor,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }
                
                // If expanded, draw the vertical line for children starting from icon center
                if (isExpanded && hasChildren) {
                    val x = (nodeIndent(depth) + 18.dp).toPx()
                    val yCenter = size.height / 2
                    drawLine(
                        color = dashColor,
                        start = Offset(x, yCenter),
                        end = Offset(x, size.height),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }
            }
            .padding(start = nodeIndent(depth), top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .combinedClickable(
                onClick = { onSelectNode(nodeKey) },
                onLongClick = onLongClick
            )
            .padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasChildren) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .combinedClickable(
                        onClick = { onToggleExpanded(nodeKey) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp).graphicsLayer {
                        this.rotationZ = rotation
                    }
                )
            }
        } else {
            Spacer(Modifier.size(36.dp))
        }
        leadingContent?.invoke()
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(vertical = 9.dp)
        )
        trailingContent?.invoke()
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

private fun nodeIndent(depth: Int): Dp = (depth * 18).dp

private fun guideLineStart(depth: Int): Dp = ((depth - 1).coerceAtLeast(0) * 18 + 18).dp

private fun Objective.nodeKey(): String = "objective-$id"

private fun KeyResult.nodeKey(): String = "key-result-$id"

private fun TaskItem.nodeKey(): String = "task-$id"

package com.checkit.ui.myday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.leftovers_banner_carry_all
import checkit.shared.generated.resources.leftovers_item_carry
import checkit.shared.generated.resources.leftovers_section_title
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.Objective
import com.checkit.domain.TaskItem
import com.checkit.domain.TagItem
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.TagPicker
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.tasks.isOverdue
import com.checkit.ui.tasks.timeRangeLabel
import com.checkit.ui.tasks.views.DailyPlanTimelineCard
import com.checkit.ui.tasks.views.TaskTimelineCard
import com.checkit.ui.today
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SuggestionsSheet(
    tasks: List<TaskItem>,
    leftovers: List<DailyPlanItem>,
    availableTags: List<TagItem>,
    onDismiss: () -> Unit,
    onTaskClick: (TaskItem) -> Unit,
    onAddTask: (TaskItem) -> Unit,
    onQuickAdd: (String, List<Long>) -> Unit,
    onCarryLeftover: (DailyPlanItem) -> Unit,
    onCarryAllLeftovers: () -> Unit,
    onCreateTask: () -> Unit,
    onNewTagClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var quickAddText by remember { mutableStateOf("") }
    var selectedTagIds by remember { mutableStateOf(emptySet<Long>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add to My Day",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(onClick = onCreateTask) {
                    Text("New Task")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppOutlinedTextField(
                    value = quickAddText,
                    onValueChange = { quickAddText = it },
                    placeholder = "Quick add to plan...",
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    contentPadding = PaddingValues(12.dp)
                )

                TagPicker(
                    availableTags = availableTags,
                    selectedTagIds = selectedTagIds,
                    onTagToggle = { tagId ->
                        selectedTagIds = if (tagId in selectedTagIds) {
                            selectedTagIds - tagId
                        } else {
                            selectedTagIds + tagId
                        }
                        availableTags.firstOrNull { it.id == tagId }?.let { tag ->
                            quickAddText = quickAddText.ifEmpty { tag.name }
                        }
                    },
                    onNewTagClick = onNewTagClick,
                    modifier = Modifier.align(Alignment.Start)
                )

                Button(
                    onClick = {
                        onQuickAdd(quickAddText, selectedTagIds.toList())
                        quickAddText = ""
                        selectedTagIds = emptySet()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    enabled = quickAddText.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to My Day")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (leftovers.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.leftovers_section_title),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = onCarryAllLeftovers) {
                                Text(stringResource(Res.string.leftovers_banner_carry_all))
                            }
                        }
                    }
                    items(leftovers, key = { "leftover-${it.id}" }) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                DailyPlanTimelineCard(
                                    item = item,
                                    isOverdue = item.isOverdue(today())
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { onCarryLeftover(item) }) {
                                        Text(stringResource(Res.string.leftovers_item_carry))
                                    }
                                }
                            }
                        }
                    }
                }
                if (tasks.isEmpty() && leftovers.isEmpty()) {
                    item { EmptyStateText("No suggested tasks") }
                } else if (tasks.isNotEmpty()) {
                    items(tasks, key = { it.id }) { task ->
                        SuggestionCard(
                            task = task,
                            list = task.objective,
                            onClick = { onTaskClick(task) },
                            onAdd = { onAddTask(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    task: TaskItem,
    list: Objective?,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    TaskTimelineCard(
        task = task,
        timeLabel = task.timeRangeLabel().takeIf { it.isNotBlank() } ?: task.doDate?.localizedCompactDateWithDayName() ?: list?.name,
        onClick = onClick,
        trailingContent = {
            IconButton(
                onClick = onAdd,
                shape = CircleShape,
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

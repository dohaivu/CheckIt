package com.checkit.ui.twelveweek

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.cancel
import checkit.shared.generated.resources.plan_save
import checkit.shared.generated.resources.twelve_week_abandon_cycle
import checkit.shared.generated.resources.twelve_week_add_cycle
import checkit.shared.generated.resources.twelve_week_add_goal
import checkit.shared.generated.resources.twelve_week_check_in
import checkit.shared.generated.resources.twelve_week_check_in_note
import checkit.shared.generated.resources.twelve_week_check_in_save
import checkit.shared.generated.resources.twelve_week_check_in_history
import checkit.shared.generated.resources.twelve_week_check_in_history_empty
import checkit.shared.generated.resources.twelve_week_check_in_title
import checkit.shared.generated.resources.twelve_week_complete_cycle
import checkit.shared.generated.resources.twelve_week_complete_title
import checkit.shared.generated.resources.twelve_week_cycle_title_label
import checkit.shared.generated.resources.twelve_week_delete_goal
import checkit.shared.generated.resources.twelve_week_edit_goal
import checkit.shared.generated.resources.twelve_week_edit_cycle
import checkit.shared.generated.resources.twelve_week_empty_subtitle
import checkit.shared.generated.resources.twelve_week_empty_title
import checkit.shared.generated.resources.twelve_week_final_status_achieved
import checkit.shared.generated.resources.twelve_week_final_status_missed
import checkit.shared.generated.resources.twelve_week_final_status_partial
import checkit.shared.generated.resources.twelve_week_goal_note_label
import checkit.shared.generated.resources.twelve_week_goal_title_label
import checkit.shared.generated.resources.twelve_week_no_goals
import checkit.shared.generated.resources.twelve_week_review_note
import checkit.shared.generated.resources.twelve_week_score_label
import checkit.shared.generated.resources.twelve_week_start
import checkit.shared.generated.resources.twelve_week_start_sheet_title
import checkit.shared.generated.resources.twelve_week_starts_on
import checkit.shared.generated.resources.twelve_week_tactics_empty
import checkit.shared.generated.resources.twelve_week_title
import checkit.shared.generated.resources.twelve_week_average
import checkit.shared.generated.resources.twelve_week_latest
import checkit.shared.generated.resources.twelve_week_week_number
import checkit.shared.generated.resources.twelve_week_week_of_12
import com.checkit.domain.TaskItem
import com.checkit.domain.TwelveWeekCycleCard
import com.checkit.domain.TwelveWeekCycleStatus
import com.checkit.domain.TwelveWeekGoalCard
import com.checkit.domain.TwelveWeekGoalFinalStatus
import com.checkit.domain.weekDateRange
import com.checkit.ui.components.AppEditorBottomSheet
import com.checkit.ui.components.AppOutlinedTextField
import com.checkit.ui.components.DateTimeRangeDetailChip
import com.checkit.ui.components.DeleteOverflowMenu
import com.checkit.ui.components.EditorOverflowMenu
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.tasks.cardColor
import com.checkit.ui.tasks.isOverdue
import com.checkit.ui.tasks.views.TaskTitleRow
import com.checkit.ui.localizedCompactDate
import com.checkit.ui.tasks.views.ViewOptionsMenu
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TwelveWeekScreen(
    state: TwelveWeekUiState,
    viewModel: TwelveWeekViewModel,
    onBack: () -> Unit,
    onAddTactic: (Long) -> Unit,
    onToggleTactic: (TaskItem) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.twelve_week_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cancel)
                        )
                    }
                },
                actions = {
                    if (!state.isLoading) {
                        IconButton(onClick = { viewModel.openCycleEditor() }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(Res.string.twelve_week_add_cycle)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            when {
                state.isLoading -> Text(
                    text = stringResource(Res.string.twelve_week_empty_subtitle),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.workspace.cycleCards.isEmpty() -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EmptyCycleCard(onStart = { viewModel.openCycleEditor() })
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.workspace.cycleCards.forEach { cycleCard ->
                        CycleCard(
                            cycleCard = cycleCard,
                            isActive = cycleCard.cycle.status == TwelveWeekCycleStatus.Active,
                            hasCheckIns = state.workspace.checkIns.any { it.cycleId == cycleCard.cycle.id },
                            onCheckIn = {
                                cycleCard.currentWeekIndex?.let { index ->
                                    viewModel.openCheckInSheet(cycleCard.cycle.id, index)
                                }
                            },
                            onComplete = { viewModel.openCompleteSheet(cycleCard.cycle.id) },
                            onAbandon = { viewModel.abandonCycle(cycleCard.cycle.id) },
                            onEditCycle = { viewModel.openCycleEditor(cycleCard.cycle.id) },
                            onAddGoal = { viewModel.openAddGoalEditor(cycleCard.cycle.id) },
                            onCheckInHistory = { viewModel.openCheckInHistory(cycleCard.cycle.id) },
                            onEditGoal = viewModel::openEditGoalEditor,
                            onAddTactic = onAddTactic,
                            onToggleTactic = onToggleTactic
                        )
                    }
                }
            }
        }
    }

    state.cycleEditor?.let { editor ->
        CycleEditorSheet(
            editor = editor,
            onDismiss = viewModel::dismissCycleEditor,
            onTitleChange = viewModel::updateCycleEditorTitle,
            onSave = viewModel::saveCycleEditor
        )
    }
    state.goalEditor?.let { editor ->
        GoalEditorSheet(
            editor = editor,
            onDismiss = viewModel::dismissGoalEditor,
            onTitleChange = viewModel::updateGoalEditorTitle,
            onNoteChange = viewModel::updateGoalEditorNote,
            onSave = viewModel::saveGoalEditor,
            onDelete = { editor.goalId?.let(viewModel::deleteGoal) }
        )
    }
    state.checkInSheet?.let { sheet ->
        WeeklyCheckInSheet(
            sheet = sheet,
            onDismiss = viewModel::dismissCheckInSheet,
            onNoteChange = viewModel::updateCheckInNote,
            onScoreChange = viewModel::updateScore,
            onSave = viewModel::saveCheckIn
        )
    }
    state.completeSheet?.let { sheet ->
        CompleteCycleSheet(
            sheet = sheet,
            onDismiss = viewModel::dismissCompleteSheet,
            onStatusChange = viewModel::setFinalStatus,
            onNoteChange = viewModel::updateCompleteNote,
            onSave = viewModel::saveCompleteCycle
        )
    }
    state.checkInHistory?.let { sheet ->
        CheckInHistorySheet(
            sheet = sheet,
            onDismiss = viewModel::dismissCheckInHistory
        )
    }
}

@Composable
private fun EmptyCycleCard(onStart: () -> Unit) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
        )
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(Res.string.twelve_week_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(Res.string.twelve_week_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
        Button(onClick = onStart) {
            Text(stringResource(Res.string.twelve_week_start))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CycleCard(
    cycleCard: TwelveWeekCycleCard,
    isActive: Boolean,
    hasCheckIns: Boolean,
    onCheckIn: () -> Unit,
    onComplete: () -> Unit,
    onAbandon: () -> Unit,
    onEditCycle: () -> Unit,
    onAddGoal: () -> Unit,
    onCheckInHistory: () -> Unit,
    onEditGoal: (Long) -> Unit,
    onAddTactic: (Long) -> Unit,
    onToggleTactic: (TaskItem) -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
        )
    )
    val weekLabel = cycleCard.currentWeekIndex?.let { index ->
        stringResource(Res.string.twelve_week_week_of_12, index + 1)
    }.orEmpty()
    val startDate = LocalDate.fromEpochDays(cycleCard.cycle.startEpochDays)
    val endDate = LocalDate.fromEpochDays(cycleCard.cycle.endEpochDays)
    val dateRangeLabel = if (startDate.year == endDate.year) {
        "${startDate.localizedCompactDate()} – ${endDate.localizedCompactDate()}, ${endDate.year}"
    } else {
        "${startDate.localizedCompactDate()}, ${startDate.year} – ${endDate.localizedCompactDate()}, ${endDate.year}"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = gradient,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .then(
                        if (isActive) {
                            Modifier.combinedClickable(onClick = {}, onLongClick = onEditCycle)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cycleCard.cycle.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (weekLabel.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Text(
                                text = weekLabel,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    if (isActive) {
                        EditorOverflowMenu { onDismiss ->
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.twelve_week_add_goal)) },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    onDismiss()
                                    onAddGoal()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.twelve_week_check_in)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    onDismiss()
                                    onCheckIn()
                                },
                                enabled = cycleCard.currentWeekIndex != null
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.twelve_week_check_in_history)) },
                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                                onClick = {
                                    onDismiss()
                                    onCheckInHistory()
                                },
                                enabled = hasCheckIns
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.twelve_week_complete_cycle)) },
                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                                onClick = {
                                    onDismiss()
                                    onComplete()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.twelve_week_abandon_cycle)) },
                                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                                onClick = {
                                    onDismiss()
                                    onAbandon()
                                }
                            )
                        }
                    } else {
                        EditorOverflowMenu { onDismiss ->
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.twelve_week_check_in_history)) },
                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                                onClick = {
                                    onDismiss()
                                    onCheckInHistory()
                                },
                                enabled = hasCheckIns
                            )
                        }
                    }
                }
                Text(
                    text = dateRangeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cycleCard.goals.isEmpty()) {
                    if (isActive) {
                        Text(
                            text = stringResource(Res.string.twelve_week_no_goals),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    cycleCard.goals.forEach { goalCard ->
                        GoalCard(
                            card = goalCard,
                            isEditable = isActive,
                            onClick = { onEditGoal(goalCard.goal.id) },
                            onAddTactic = { onAddTactic(goalCard.goal.id) },
                            onToggleTactic = onToggleTactic
                        )
                    }
                }
            }
            Spacer(Modifier.size(4.dp))
        }
    }
}

@Composable
private fun GoalCard(
    card: TwelveWeekGoalCard,
    isEditable: Boolean,
    onClick: () -> Unit,
    onAddTactic: () -> Unit,
    onToggleTactic: (TaskItem) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (isEditable) Modifier.clickable(onClick = onClick) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = card.goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    card.goal.note.takeIf { it.isNotBlank() }?.let { note ->
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (isEditable) {
                    IconButton(onClick = onAddTactic) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }

            if (card.averageScore != null || card.latestScore != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    card.averageScore?.let { average ->
                        SummaryChip(
                            label = stringResource(
                                Res.string.twelve_week_average,
                                formatAverage(average)
                            )
                        )
                    }
                    card.latestScore?.let { latest ->
                        SummaryChip(
                            label = stringResource(
                                Res.string.twelve_week_latest,
                                latest.score.toString()
                            )
                        )
                    }
                }
            }

            if (card.tactics.isEmpty()) {
                if (isEditable) {
                    Text(
                        text = stringResource(Res.string.twelve_week_tactics_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            } else {
                card.tactics.forEach { tactic ->
                    TacticRow(task = tactic, onClick = { onToggleTactic(tactic) })
                }
            }
        }
    }
}

@Composable
private fun TacticRow(task: TaskItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { onClick() }
            .height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(task.cardColor())
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TaskTitleRow(task, descriptionMaxLines = 0)
                DateTimeRangeDetailChip(
                    task.doDate,
                    task.startTimeMinutes,
                    task.endTimeMinutes,
                    isOverdue = task.isOverdue()
                )
            }
        }
    }
}

@Composable
private fun CycleEditorSheet(
    editor: TwelveWeekCycleEditorState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onSave: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(
                    if (editor.cycleId == null) Res.string.twelve_week_start_sheet_title
                    else Res.string.twelve_week_edit_cycle
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            AppOutlinedTextField(
                value = editor.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.twelve_week_cycle_title_label),
                maxLines = 1
            )
            Text(
                text = stringResource(
                    Res.string.twelve_week_starts_on,
                    LocalDate.fromEpochDays(editor.startEpochDays).localizedCompactDate()
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.cancel))
                }
                Button(
                    onClick = onSave,
                    enabled = editor.title.isNotBlank() && !editor.isSaving
                ) {
                    Text(stringResource(Res.string.plan_save))
                }
            }
        }
    }
}

@Composable
private fun GoalEditorSheet(
    editor: TwelveWeekGoalEditorState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        if (editor.goalId == null) Res.string.twelve_week_add_goal
                        else Res.string.twelve_week_edit_goal
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (editor.goalId != null) {
                    DeleteOverflowMenu(
                        onDelete = onDelete,
                        label = stringResource(Res.string.twelve_week_delete_goal)
                    )
                }
            }
            AppOutlinedTextField(
                value = editor.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.twelve_week_goal_title_label),
                maxLines = 1
            )
            AppOutlinedTextField(
                value = editor.note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.twelve_week_goal_note_label),
                minLines = 2,
                maxLines = 4
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss, enabled = !editor.isSaving) {
                    Text(stringResource(Res.string.cancel))
                }
                Button(
                    onClick = onSave,
                    enabled = editor.title.isNotBlank() && !editor.isSaving
                ) {
                    Text(stringResource(Res.string.plan_save))
                }
            }
        }
    }
}

@Composable
private fun WeeklyCheckInSheet(
    sheet: TwelveWeekCheckInSheetState,
    onDismiss: () -> Unit,
    onNoteChange: (String) -> Unit,
    onScoreChange: (Long, String) -> Unit,
    onSave: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.twelve_week_check_in_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            sheet.scores.forEach { field ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = field.goalTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AppOutlinedTextField(
                        value = field.score,
                        onValueChange = { onScoreChange(field.goalId, it) },
                        modifier = Modifier.width(72.dp),
                        placeholder = stringResource(Res.string.twelve_week_score_label),
                        maxLines = 1
                    )
                }
            }
            AppOutlinedTextField(
                value = sheet.note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.twelve_week_check_in_note),
                minLines = 2,
                maxLines = 4
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss, enabled = !sheet.isSaving) {
                    Text(stringResource(Res.string.cancel))
                }
                Button(
                    onClick = onSave,
                    enabled = sheet.canSave && !sheet.isSaving
                ) {
                    Text(stringResource(Res.string.twelve_week_check_in_save))
                }
            }
        }
    }
}

@Composable
private fun CompleteCycleSheet(
    sheet: TwelveWeekCompleteSheetState,
    onDismiss: () -> Unit,
    onStatusChange: (Long, TwelveWeekGoalFinalStatus) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.twelve_week_complete_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            sheet.goalTitles.forEach { (goalId, title) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinalStatusButton(
                            label = stringResource(Res.string.twelve_week_final_status_achieved),
                            selected = sheet.finalStatuses[goalId] == TwelveWeekGoalFinalStatus.Achieved,
                            onClick = { onStatusChange(goalId, TwelveWeekGoalFinalStatus.Achieved) },
                            modifier = Modifier.weight(1f)
                        )
                        FinalStatusButton(
                            label = stringResource(Res.string.twelve_week_final_status_partial),
                            selected = sheet.finalStatuses[goalId] == TwelveWeekGoalFinalStatus.Partial,
                            onClick = { onStatusChange(goalId, TwelveWeekGoalFinalStatus.Partial) },
                            modifier = Modifier.weight(1f)
                        )
                        FinalStatusButton(
                            label = stringResource(Res.string.twelve_week_final_status_missed),
                            selected = sheet.finalStatuses[goalId] == TwelveWeekGoalFinalStatus.Missed,
                            onClick = { onStatusChange(goalId, TwelveWeekGoalFinalStatus.Missed) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            AppOutlinedTextField(
                value = sheet.reviewNote,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.twelve_week_review_note),
                minLines = 2,
                maxLines = 4
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss, enabled = !sheet.isSaving) {
                    Text(stringResource(Res.string.cancel))
                }
                Button(onClick = onSave, enabled = !sheet.isSaving) {
                    Text(stringResource(Res.string.twelve_week_complete_cycle))
                }
            }
        }
    }
}

@Composable
private fun FinalStatusButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SummaryChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun formatAverage(value: Double): String =
    if (value == value.toInt().toDouble()) {
        value.toInt().toString()
    } else {
        (value * 10).roundToInt().toString().let { tenths ->
            "${tenths.dropLast(1)}.${tenths.last()}"
        }
    }

@Composable
private fun CheckInHistorySheet(
    sheet: TwelveWeekCheckInHistoryState,
    onDismiss: () -> Unit
) {
    AppEditorBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.twelve_week_check_in_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sheet.cycleTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (sheet.weeks.isEmpty()) {
                Text(
                    text = stringResource(Res.string.twelve_week_check_in_history_empty),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                sheet.weeks.forEach { entry ->
                    WeekHistoryEntryCard(
                        entry = entry,
                        startEpochDays = sheet.startEpochDays
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekHistoryEntryCard(entry: TwelveWeekHistoryEntry, startEpochDays: Int) {
    val range = weekDateRange(startEpochDays, entry.weekIndex)
    val startDate = LocalDate.fromEpochDays(range.first)
    val endDate = LocalDate.fromEpochDays(range.last)
    val dateLabel = "${startDate.localizedCompactDate()} – ${endDate.localizedCompactDate()}"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.twelve_week_week_number, entry.weekIndex + 1),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.note.isNotBlank()) {
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            entry.scores.forEach { score ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = score.goalTitle,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = score.score.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

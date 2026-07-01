package com.checkit.ui.tasks

import com.checkit.domain.TaskPriority
import kotlinx.datetime.LocalDate

data class TaskEditorActions(
    val onDismiss: () -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onRestore: () -> Unit,
    val onComplete: () -> Unit,
    val onOpen: () -> Unit,
    val onAddToMyDay: () -> Unit,
    val onTaskNameChange: (String) -> Unit,
    val onTaskListChange: (Long) -> Unit,
    val onTaskDescriptionChange: (String) -> Unit,
    val onTaskDoDateChange: (LocalDate?) -> Unit,
    val onTaskStartTimeChange: (Int?) -> Unit,
    val onTaskEndTimeChange: (Int?) -> Unit,
    val onDailyPlanStartTimeChange: (Int?) -> Unit,
    val onDailyPlanEndTimeChange: (Int?) -> Unit,
    val onDailyPlanStatus: () -> Unit,
    val onDailyPlanDelete: (Long) -> Unit,
    val onTaskRepeatChange: (RepeatPreset) -> Unit,
    val onTaskPriorityChange: (TaskPriority) -> Unit,
    val onTaskReminderToggle: (Int) -> Unit,
    val onSubTaskToggle: (Int) -> Unit,
    val onSubTaskAdd: () -> Unit,
    val onSubTaskNameChange: (Int, String) -> Unit,
    val onSubTaskRemove: (Int) -> Unit,
    val onSubTaskMove: (Int, Int) -> Unit,
    val onTaskTagToggle: (Long) -> Unit,
    val onNoteTitleChange: (String) -> Unit,
    val onNoteContentChange: (String) -> Unit,
    val onNoteListChange: (Long) -> Unit,
    val onNoteDateChange: (LocalDate?) -> Unit,
    val onNoteStartTimeChange: (Int?) -> Unit,
    val onNoteTagToggle: (Long) -> Unit,
    val onSwitchAddModeToTask: () -> Unit,
    val onSwitchAddModeToNote: () -> Unit
)

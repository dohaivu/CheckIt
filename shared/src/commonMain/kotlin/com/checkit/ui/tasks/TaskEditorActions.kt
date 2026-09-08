package com.checkit.ui.tasks

import com.checkit.domain.DailyPlanItem
import com.checkit.domain.TaskPriority
import kotlinx.datetime.LocalDate

data class TaskEditorActions(
    val common: Common,
    val task: Task,
    val dailyPlan: DailyPlan,
    val subTask: SubTask,
    val note: Note
) {
    data class Common(
        val onDismiss: () -> Unit,
        val onSave: () -> Unit,
        val onDelete: () -> Unit,
        val onRestore: () -> Unit,
        val onComplete: () -> Unit,
        val onReopen: () -> Unit,
        val onPinToggle: () -> Unit,
        val onNewTagClick: () -> Unit,
        val onAddToMyDay: () -> Unit
    )

    data class Task(
        val onNameChange: (String) -> Unit,
        val onListChange: (Long) -> Unit,
        val onDescriptionChange: (String) -> Unit,
        val onDoDateChange: (LocalDate?) -> Unit,
        val onTimeChange: (Int?, Int?) -> Unit,
        val onRepeatChange: (RepeatPreset) -> Unit,
        val onPriorityChange: (TaskPriority) -> Unit,
        val onReminderToggle: (Int) -> Unit,
        val onTagToggle: (Long) -> Unit,
        val onLabelChange: (String) -> Unit
    )

    data class DailyPlan(
        val onTimeChange: (Int?, Int?) -> Unit,
        val onTitleChange: (String) -> Unit,
        val onNoteChange: (String) -> Unit,
        val onLabelChange: (String) -> Unit,
        val onStatus: () -> Unit,
        val onDelete: (Long) -> Unit,
        val onStartSprint: (DailyPlanItem) -> Unit,
        val onStartOngoingSprint: (DailyPlanItem) -> Unit
    )

    data class SubTask(
        val onToggle: (Int) -> Unit,
        val onAdd: () -> Unit,
        val onNameChange: (Int, String) -> Unit,
        val onRemove: (Int) -> Unit,
        val onMove: (Int, Int) -> Unit
    )

    data class Note(
        val onTitleChange: (String) -> Unit,
        val onContentChange: (String) -> Unit,
        val onListChange: (Long) -> Unit,
        val onDateChange: (LocalDate?) -> Unit,
        val onStartTimeChange: (Int?) -> Unit,
        val onTagToggle: (Long) -> Unit,
        val onLabelChange: (String) -> Unit
    )
}

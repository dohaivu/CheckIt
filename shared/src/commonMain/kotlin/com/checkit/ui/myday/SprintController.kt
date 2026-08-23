package com.checkit.ui.myday

import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.TaskItem
import com.checkit.ui.UiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Clock

/** Handles sprints: starting (incl. from items/editor/tasks), quick sprint sheet, and pomodoro transitions. */
internal class SprintController(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    fun executeFabAction(action: FabAction) {
        when (action) {
            is FabAction.QuickSprint -> {
                openQuickSprint()
                setLastFabAction(action)
            }
            is FabAction.TagSprint -> {
                val tag = action.tag
                startSprint(description = tag.name, tagIds = listOf(tag.id))
                setLastFabAction(action)
            }
        }
    }

    fun setLastFabAction(action: FabAction) {
        scope.launch {
            when (action) {
                is FabAction.QuickSprint -> deps.settingsRepository.setLastFabAction("QuickSprint", null)
                is FabAction.TagSprint -> deps.settingsRepository.setLastFabAction("TagSprint", action.tag.id)
            }
        }
    }

    fun startSprint(taskId: Long? = null, dailyPlanItemId: Long? = null, description: String = "", tagIds: List<Long> = emptyList()) {
        dismissQuickSprint()
        if (!deps.sprintManager.startSprint(taskId, dailyPlanItemId, description, tagIds = tagIds)) {
            state.sendEvent(UiEvent.ShowSnackbar("A sprint is already in progress"))
        }
    }

    fun startSprintByItemId(itemId: Long) {
        val item = state.uiState.value.items.firstOrNull { it.id == itemId }
        if (item != null) {
            startSprint(
                taskId = item.taskId,
                dailyPlanItemId = item.id,
                description = item.title,
                tagIds = item.tags.map { it.id }
            )
        }
    }

    fun startSprintWithTask(task: TaskItem) {
        dismissQuickSprint()
        if (!deps.sprintManager.startSprint(task.id, null, task.name, tagIds = task.tags.map { it.id })) {
            state.sendEvent(UiEvent.ShowSnackbar("A sprint is already in progress"))
        }
    }

    fun startSprintWithChoice(choice: SprintChoice) {
        dismissQuickSprint()
        val success = when (choice) {
            is SprintChoice.Task -> deps.sprintManager.startSprint(
                choice.task.id,
                null,
                choice.task.name,
                tagIds = choice.task.tags.map { it.id }
            )
            is SprintChoice.PlanItem -> {
                // If the item is already Done, we start a NEW session (new daily plan item) on finish.
                val itemId = if (choice.item.status == DailyPlanItemStatus.Done) null else choice.item.id
                deps.sprintManager.startSprint(
                    choice.item.taskId,
                    itemId,
                    choice.item.title,
                    tagIds = choice.item.tags.map { it.id }
                )
            }
        }
        if (!success) {
            state.sendEvent(UiEvent.ShowSnackbar("A sprint is already in progress"))
        }
    }

    fun startSprintForItem(item: DailyPlanItem) {
        val tasks = state.uiState.value.tasks
        startSprintWithChoice(SprintChoice.PlanItem(item, tasks.find { it.id == item.taskId }))
    }

    fun startOngoingSprintForItem(item: DailyPlanItem) {
        val startTimeMinutes = item.startTimeMinutes ?: return

        val date = LocalDate.fromEpochDays(item.dateEpochDays)
        val scheduledTimeMillis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds() + (startTimeMinutes * 60 * 1000L)
        val nowMillis = Clock.System.now().toEpochMilliseconds()

        val elapsedSeconds = ((nowMillis - scheduledTimeMillis) / 1000).toInt()
        val pomodoroDurationSeconds = 25 * 60
        val gracePeriodSeconds = 5 * 60

        val (durationSeconds, isPomodoro) = when {
            elapsedSeconds < 0 -> pomodoroDurationSeconds to true
            elapsedSeconds < pomodoroDurationSeconds -> pomodoroDurationSeconds to true
            else -> (elapsedSeconds + gracePeriodSeconds) to false
        }

        val success = deps.sprintManager.startSprint(
            taskId = item.taskId,
            dailyPlanItemId = item.id,
            description = item.title,
            durationSeconds = durationSeconds,
            isPomodoro = isPomodoro,
            tagIds = item.tags.map { it.id },
            startTimeEpochMillis = if (elapsedSeconds < 0) nowMillis else scheduledTimeMillis
        )

        if (!success) {
            state.sendEvent(UiEvent.ShowSnackbar("A sprint is already in progress"))
        }
    }

    fun startNewSprintFromEditor() {
        val editor = state.uiState.value.itemEditor ?: return
        val itemId = editor.itemId ?: return
        val planItem = state.uiState.value.items.firstOrNull { it.id == itemId } ?: return

        startSprintForItem(planItem)
        state.update { it.copy(itemEditor = null) }
    }

    fun startOngoingSprintFromEditor() {
        val editor = state.uiState.value.itemEditor ?: return
        val itemId = editor.itemId ?: return
        val planItem = state.uiState.value.items.firstOrNull { it.id == itemId } ?: return

        startOngoingSprintForItem(planItem)
        state.update { it.copy(itemEditor = null) }
    }

    fun openQuickSprint() {
        state.update { it.copy(showQuickSprintSheet = true) }
    }

    fun dismissQuickSprint() {
        state.update { it.copy(showQuickSprintSheet = false) }
    }

    fun pauseSprint() = deps.sprintManager.pauseSprint()
    fun resumeSprint() = deps.sprintManager.resumeSprint()
    fun completeSprint() = deps.sprintManager.completeSprintManually()

    fun upgradeToPomodoro() {
        scope.launch { deps.sprintTransition.upgradeToPomodoro() }
    }

    fun saveSprintAsWin() {
        scope.launch { deps.sprintTransition.saveWin() }
    }

    fun saveAndBreak() {
        scope.launch { deps.sprintTransition.saveAndBreak() }
    }

    fun continueNewPomodoro() {
        scope.launch { deps.sprintTransition.saveAndContinue() }
    }

    fun startNextPomodoro() {
        scope.launch { deps.sprintTransition.startNext() }
    }

    fun dismissFinishedSprint() = deps.sprintManager.dismissFinished()
}

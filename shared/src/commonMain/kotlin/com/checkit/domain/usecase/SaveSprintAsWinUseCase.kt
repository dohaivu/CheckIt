package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.SprintState
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class SaveSprintAsWinUseCase(
    private val repository: CheckItRepository,
    private val addTaskToDailyPlan: AddTaskToDailyPlanUseCase,
    private val addDailyPlanItem: AddDailyPlanItemUseCase,
    private val updateDailyPlanItemTime: UpdateDailyPlanItemTimeUseCase,
    private val updateDailyPlanItemStatus: UpdateDailyPlanItemStatusUseCase,
    private val syncKeyResultFromDailyPlan: SyncKeyResultFromDailyPlanUseCase
) {
    suspend operator fun invoke(finished: SprintState.Finished): Long? {
        if (finished.isBreak) return null

        val todayDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val startInstant = Instant.fromEpochMilliseconds(finished.startTimeEpochMillis)
        val startDateTime = startInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        val startMinutes = startDateTime.hour * 60 + startDateTime.minute
        val durationMinutes = (finished.elapsedSeconds / 60).coerceAtLeast(1)
        val endMinutes = startMinutes + durationMinutes

        val taskId = finished.taskId
        val dailyPlanItemId = finished.dailyPlanItemId

        return if (dailyPlanItemId != null) {
            val item = repository.getDailyPlanItem(dailyPlanItemId) ?: return null

            syncKeyResultFromDailyPlan(
                itemId = dailyPlanItemId,
                proposedStatus = DailyPlanItemStatus.Done,
                proposedStartTime = startMinutes,
                proposedEndTime = endMinutes
            )
            updateDailyPlanItemTime(dailyPlanItemId, startMinutes, endMinutes)
            updateDailyPlanItemStatus(dailyPlanItemId, DailyPlanItemStatus.Done)
            dailyPlanItemId
        } else if (taskId != null) {
            val board = repository.observeTaskBoard().first()
            val task = board.tasksById[taskId] ?: return null

            val itemId = addTaskToDailyPlan(todayDate, task)

            syncKeyResultFromDailyPlan(
                itemId = itemId,
                proposedStatus = DailyPlanItemStatus.Done,
                proposedStartTime = startMinutes,
                proposedEndTime = endMinutes
            )
            updateDailyPlanItemTime(itemId, startMinutes, endMinutes)
            updateDailyPlanItemStatus(itemId, DailyPlanItemStatus.Done)
            itemId
        } else {
            addDailyPlanItem(
                date = todayDate,
                title = finished.description,
                note = "Sprint session (${durationMinutes}m)",
                startTimeMinutes = startMinutes,
                endTimeMinutes = startMinutes + durationMinutes,
                source = DailyPlanItemSource.MyDayTask,
                status = DailyPlanItemStatus.Done,
                tagIds = finished.tagIds
            )
        }
    }
}

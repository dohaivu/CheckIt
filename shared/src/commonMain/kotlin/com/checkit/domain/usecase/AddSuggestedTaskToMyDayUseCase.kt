package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DefaultTaskDurationMinutes
import com.checkit.domain.TaskItem
import com.checkit.domain.nextAvailableTimeRange
import com.checkit.ui.currentMyDayTimeMinutes
import com.checkit.ui.today
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class AddSuggestedTaskToMyDayUseCase(
    private val repository: CheckItRepository,
    private val addTaskToDailyPlan: AddTaskToDailyPlanUseCase,
    private val updateDailyPlanItemTime: UpdateDailyPlanItemTimeUseCase
) {
    suspend operator fun invoke(
        task: TaskItem,
        suggestionStart: Int?,
        suggestionEnd: Int?
    ): Result<Long> = runCatching {
        val dailyPlans = repository.observeDailyPlans().first()
        val todayDate = today()
        val plan = dailyPlans.firstOrNull { it.date == todayDate }
        val items = plan?.items.orEmpty()

        val (startTimeMinutes, endTimeMinutes) = calculateTimeRange(
            task = task,
            items = items,
            suggestionStart = suggestionStart,
            suggestionEnd = suggestionEnd
        )

        val itemId = addTaskToDailyPlan(todayDate, task)
        if (startTimeMinutes != task.startTimeMinutes || endTimeMinutes != task.endTimeMinutes) {
            updateDailyPlanItemTime(itemId, startTimeMinutes, endTimeMinutes)
        }
        itemId
    }

    private fun calculateTimeRange(
        task: TaskItem,
        items: List<DailyPlanItem>,
        suggestionStart: Int?,
        suggestionEnd: Int?
    ): Pair<Int?, Int?> {
        val selectedDuration = suggestionStart?.let { start ->
            suggestionEnd?.let { end ->
                (end - start).takeIf { it > 0 }
            }
        }
        val durationMinutes = selectedDuration
            ?: task.duration()
            ?: DefaultTaskDurationMinutes
            
        val preferredStart = suggestionStart ?: task.preferredMyDayStartTime()
        
        return nextAvailableTimeRange(preferredStart, durationMinutes, items)
    }

    private fun TaskItem.duration(): Int? {
        val start = startTimeMinutes ?: return null
        val end = endTimeMinutes ?: return null
        return (end - start).takeIf { it > 0 }
    }

    private fun TaskItem.preferredMyDayStartTime(): Int {
        val now = currentMyDayTimeMinutes()
        val start = startTimeMinutes
        return if (start == null || start < now) {
            now
        } else {
            start
        }
    }
}

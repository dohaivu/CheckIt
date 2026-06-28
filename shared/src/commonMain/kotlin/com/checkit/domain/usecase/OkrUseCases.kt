package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.GoalWriteInput
import com.checkit.data.ObjectiveWriteInput
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.KeyResultUnit

class AddGoalUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(input: GoalWriteInput): Long = repository.addGoal(input)
}

class UpdateGoalUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(goalId: Long, input: GoalWriteInput) = repository.updateGoal(goalId, input)
}

class DeleteGoalUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(goalId: Long) = repository.deleteGoal(goalId)
}

class AddObjectiveUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(input: ObjectiveWriteInput): Long = repository.addObjective(input)
}

class UpdateObjectiveUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(objectiveId: Long, input: ObjectiveWriteInput) =
        repository.updateObjective(objectiveId, input)
}

class DeleteObjectiveUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(objectiveId: Long) = repository.deleteObjective(objectiveId)
}

class SyncKeyResultFromDailyPlanUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        itemId: Long,
        proposedStatus: DailyPlanItemStatus? = null,
        proposedStartTime: Int? = null,
        proposedEndTime: Int? = null
    ) {
        val item = repository.getDailyPlanItem(itemId) ?: return
        val taskId = item.taskId ?: return
        val keyResult = repository.getKeyResultForTask(taskId) ?: return

        val nextStatus = proposedStatus ?: item.status
        val nextStartTime = proposedStartTime ?: item.startTimeMinutes
        val nextEndTime = proposedEndTime ?: item.endTimeMinutes

        val unit = KeyResultUnit.fromString(keyResult.unit)
        val delta = calculateDelta(
            unit = unit,
            taskId = taskId,
            dateEpochDays = item.dateEpochDays,
            itemId = item.id,
            oldStatus = item.status,
            nextStatus = nextStatus,
            oldStartTime = item.startTimeMinutes,
            nextStartTime = nextStartTime,
            oldEndTime = item.endTimeMinutes,
            nextEndTime = nextEndTime
        )

        if (delta != 0.0) {
            repository.adjustKeyResultValue(keyResult.id, delta)
        }
    }

    private suspend fun calculateDelta(
        unit: KeyResultUnit,
        taskId: Long,
        dateEpochDays: Int,
        itemId: Long,
        oldStatus: DailyPlanItemStatus,
        nextStatus: DailyPlanItemStatus,
        oldStartTime: Int?,
        nextStartTime: Int?,
        oldEndTime: Int?,
        nextEndTime: Int?
    ): Double {
        return when (unit) {
            KeyResultUnit.Hours -> {
                val oldHours = if (oldStatus == DailyPlanItemStatus.Done) hoursFor(oldStartTime, oldEndTime) else 0.0
                val nextHours = if (nextStatus == DailyPlanItemStatus.Done) hoursFor(nextStartTime, nextEndTime) else 0.0
                nextHours - oldHours
            }
            KeyResultUnit.Days -> {
                val wasDone = oldStatus == DailyPlanItemStatus.Done
                val willBeDone = nextStatus == DailyPlanItemStatus.Done
                if (wasDone == willBeDone) return 0.0

                if (willBeDone) {
                    val alreadyDone = repository.countDoneDailyPlanItemsForTaskOnDate(taskId, dateEpochDays, itemId) > 0
                    if (alreadyDone) 0.0 else 1.0
                } else {
                    val stillDone = repository.countDoneDailyPlanItemsForTaskOnDate(taskId, dateEpochDays, itemId) > 0
                    if (stillDone) 0.0 else -1.0
                }
            }
            KeyResultUnit.Number -> {
                val wasDone = oldStatus == DailyPlanItemStatus.Done
                val willBeDone = nextStatus == DailyPlanItemStatus.Done
                when {
                    !wasDone && willBeDone -> 1.0
                    wasDone && !willBeDone -> -1.0
                    else -> 0.0
                }
            }
            KeyResultUnit.Binary,
            KeyResultUnit.Percentage,
            KeyResultUnit.Currency,
            KeyResultUnit.Points -> 0.0
        }
    }

    private fun hoursFor(start: Int?, end: Int?): Double {
        return if (start != null && end != null) {
            (end - start).coerceAtLeast(0) / 60.0
        } else {
            1.0
        }
    }
}

package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.GoalWriteInput
import com.checkit.data.KeyResultWriteInput
import com.checkit.data.ObjectiveWriteInput
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.KeyResultUnit
import com.checkit.domain.TaskBoard

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

class AutoUpdateKeyResultCurrentValueUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        dailyPlanItem: DailyPlanItem,
        previousStatus: DailyPlanItemStatus,
        nextStatus: DailyPlanItemStatus,
        board: TaskBoard
    ) {
        if (previousStatus == nextStatus) return

        val taskId = dailyPlanItem.taskId ?: return
        val task = board.tasksById[taskId] ?: return
        val keyResult = task.keyResult ?: return

        val delta = calculateDelta(keyResult.unit, dailyPlanItem.startTimeMinutes, dailyPlanItem.endTimeMinutes)
        if (delta == 0.0) return

        val signedDelta = when {
            previousStatus != DailyPlanItemStatus.Done && nextStatus == DailyPlanItemStatus.Done -> delta
            previousStatus == DailyPlanItemStatus.Done && nextStatus != DailyPlanItemStatus.Done -> -delta
            else -> return
        }

        val newCurrentValue = (keyResult.currentValue + signedDelta).coerceAtLeast(0.0)
        repository.updateKeyResult(
            keyResult.id,
            KeyResultWriteInput(
                objectiveId = keyResult.objectiveId,
                title = keyResult.title,
                targetValue = keyResult.targetValue,
                currentValue = newCurrentValue,
                unit = keyResult.unit
            )
        )
    }

    private fun calculateDelta(
        unit: String,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?
    ): Double {
        return when (KeyResultUnit.fromString(unit)) {
            KeyResultUnit.Hours -> {
                if (startTimeMinutes != null && endTimeMinutes != null) {
                    (endTimeMinutes - startTimeMinutes).coerceAtLeast(0) / 60.0
                } else {
                    1.0
                }
            }
            KeyResultUnit.Days -> 1.0
            KeyResultUnit.Binary -> 1.0
            KeyResultUnit.Number, KeyResultUnit.Percentage, KeyResultUnit.Currency, KeyResultUnit.Points -> 1.0
        }
    }
}
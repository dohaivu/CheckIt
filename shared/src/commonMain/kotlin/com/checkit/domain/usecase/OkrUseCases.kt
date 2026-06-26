package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.GoalWriteInput
import com.checkit.data.ObjectiveWriteInput

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
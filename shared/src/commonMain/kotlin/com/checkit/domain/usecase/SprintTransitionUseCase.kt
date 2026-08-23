package com.checkit.domain.usecase

import com.checkit.domain.SprintManager
import com.checkit.domain.SprintState

class SprintTransitionUseCase(
    private val sprintManager: SprintManager,
    private val saveSprintAsWin: SaveSprintAsWinUseCase,
    private val getTask: GetTaskUseCase
) {
    suspend fun saveWin() {
        val finished = sprintManager.takeFinished() ?: return
        saveSprintAsWin(finished)
    }

    suspend fun saveAndBreak() {
        val finished = sprintManager.takeFinished() ?: return
        saveSprintAsWin(finished)
        
        sprintManager.startSprint(
            taskId = null,
            dailyPlanItemId = null,
            description = "Short Break",
            durationSeconds = 300,
            isBreak = true
        )
    }

    suspend fun saveAndContinue() {
        val finished = sprintManager.takeFinished() ?: return
        val savedItemId = saveSprintAsWin(finished)

        val taskName = finished.taskId?.let { getTask(it)?.name }

        sprintManager.startSprint(
            taskId = finished.taskId,
            dailyPlanItemId = savedItemId ?: finished.dailyPlanItemId,
            description = taskName ?: finished.description,
            durationSeconds = finished.durationSeconds + 1500,
            isPomodoro = true,
            tagIds = finished.tagIds,
            startTimeEpochMillis = finished.startTimeEpochMillis
        )
    }

    suspend fun startNext() {
        val finished = sprintManager.takeFinished() ?: return

        val taskName = finished.taskId?.let { getTask(it)?.name }

        sprintManager.startSprint(
            taskId = finished.taskId,
            dailyPlanItemId = finished.dailyPlanItemId,
            description = taskName ?: finished.description,
            durationSeconds = 1500,
            isPomodoro = true,
            tagIds = finished.tagIds
        )
    }

    suspend fun upgradeToPomodoro() {
        val finished = sprintManager.takeFinished() ?: return
        // No saveWin yet, we are extending it
        
        sprintManager.startSprint(
            taskId = finished.taskId,
            dailyPlanItemId = finished.dailyPlanItemId,
            description = finished.description,
            durationSeconds = 1500,
            isPomodoro = true,
            tagIds = finished.tagIds,
            startTimeEpochMillis = finished.startTimeEpochMillis
        )
    }
}

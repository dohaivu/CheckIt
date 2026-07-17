package com.checkit.domain

import com.checkit.notifications.SprintNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

sealed interface SprintState {
    object Idle : SprintState

    data class Running(
        val taskId: Long?,
        val description: String,
        val totalSeconds: Int,
        val remainingSeconds: Int,
        val startTimeEpochMillis: Long,
        val isPomodoro: Boolean = false
    ) : SprintState

    data class Paused(
        val runningState: Running,
        val remainingSecondsAtPause: Int
    ) : SprintState

    data class Finished(
        val taskId: Long?,
        val description: String,
        val totalSeconds: Int,
        val isPomodoro: Boolean
    ) : SprintState
}

class SprintManager(
    private val notificationScheduler: SprintNotificationScheduler
) {
    private val _state = MutableStateFlow<SprintState>(SprintState.Idle)
    val state: StateFlow<SprintState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startSprint(taskId: Long?, description: String, durationSeconds: Int = 300, isPomodoro: Boolean = false) {
        val now = Clock.System.now().toEpochMilliseconds()
        val running = SprintState.Running(
            taskId = taskId,
            description = description.ifBlank { if (isPomodoro) "Deep Focus" else "Quick Sprint" },
            totalSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            startTimeEpochMillis = now,
            isPomodoro = isPomodoro
        )
        _state.value = running
        notificationScheduler.startPersistentNotification(running)
        startTimer()
    }

    fun pauseSprint() {
        val current = _state.value
        if (current is SprintState.Running) {
            timerJob?.cancel()
            _state.value = SprintState.Paused(current, current.remainingSeconds)
            notificationScheduler.updatePersistentNotification(current, isPaused = true)
        }
    }

    fun resumeSprint() {
        val current = _state.value
        if (current is SprintState.Paused) {
            _state.value = current.runningState.copy(remainingSeconds = current.remainingSecondsAtPause)
            startTimer()
        }
    }

    fun stopSprint() {
        timerJob?.cancel()
        _state.value = SprintState.Idle
        notificationScheduler.cancelNotification()
    }

    fun completeSprintManually() {
        val current = _state.value
        if (current is SprintState.Running) {
            finish(current)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                val current = _state.value
                if (current is SprintState.Running) {
                    val remaining = current.remainingSeconds - 1
                    if (remaining <= 0) {
                        finish(current)
                        break
                    } else {
                        val updated = current.copy(remainingSeconds = remaining)
                        _state.value = updated
                        notificationScheduler.updatePersistentNotification(updated, isPaused = false)
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun finish(running: SprintState.Running) {
        timerJob?.cancel()
        _state.value = SprintState.Finished(
            taskId = running.taskId,
            description = running.description,
            totalSeconds = running.totalSeconds,
            isPomodoro = running.isPomodoro
        )
        notificationScheduler.showFinishedNotification(running.description, running.isPomodoro)
    }

    fun dismissFinished() {
        _state.value = SprintState.Idle
    }
}

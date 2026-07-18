package com.checkit.domain

import com.checkit.notifications.SprintNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

sealed interface SprintState {
    data object Idle : SprintState

    data class Running(
        val taskId: Long?,
        val dailyPlanItemId: Long?,
        val description: String,
        val totalSeconds: Int,
        val remainingSeconds: Int,
        val startTimeEpochMillis: Long,
        /** Wall-clock deadline used to recompute remaining while running. */
        val endsAtEpochMillis: Long,
        val isPomodoro: Boolean = false
    ) : SprintState

    data class Paused(
        val runningState: Running,
        val remainingSecondsAtPause: Int
    ) : SprintState

    data class Finished(
        val taskId: Long?,
        val dailyPlanItemId: Long?,
        val description: String,
        val durationSeconds: Int,
        val elapsedSeconds: Int,
        val startTimeEpochMillis: Long,
        val isPomodoro: Boolean
    ) : SprintState
}

class SprintManager(
    private val notificationScheduler: SprintNotificationScheduler,
    private val clock: Clock = Clock.System,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _state = MutableStateFlow<SprintState>(SprintState.Idle)
    val state: StateFlow<SprintState> = _state.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Starts a sprint when idle or after a finished session.
     * Returns false if a sprint is already running or paused (does not overwrite).
     */
    fun startSprint(
        taskId: Long?,
        dailyPlanItemId: Long?,
        description: String,
        durationSeconds: Int = 300,
        isPomodoro: Boolean = false,
        startTimeEpochMillis: Long? = null
    ): Boolean {
        when (_state.value) {
            is SprintState.Running, is SprintState.Paused -> return false
            is SprintState.Idle, is SprintState.Finished -> Unit
        }

        val safeDuration = durationSeconds.coerceAtLeast(1)
        val now = clock.now().toEpochMilliseconds()
        val start = startTimeEpochMillis ?: now
        val running = SprintState.Running(
            taskId = taskId,
            dailyPlanItemId = dailyPlanItemId,
            description = description.ifBlank { if (isPomodoro) "Deep Focus" else "Quick Sprint" },
            totalSeconds = safeDuration,
            remainingSeconds = ((start + safeDuration * 1000L - now) / 1000L).toInt().coerceIn(1, safeDuration),
            startTimeEpochMillis = start,
            endsAtEpochMillis = start + safeDuration * 1000L,
            isPomodoro = isPomodoro
        )
        timerJob?.cancel()
        _state.value = running
        notificationScheduler.startPersistentNotification(running)
        startTimer()
        return true
    }

    fun pauseSprint() {
        val current = _state.value
        if (current is SprintState.Running) {
            timerJob?.cancel()
            val remaining = remainingSeconds(current)
            val frozen = current.copy(remainingSeconds = remaining)
            _state.value = SprintState.Paused(frozen, remaining)
            notificationScheduler.updatePersistentNotification(frozen, isPaused = true)
        }
    }

    fun resumeSprint() {
        val current = _state.value
        if (current is SprintState.Paused) {
            val remaining = current.remainingSecondsAtPause.coerceAtLeast(0)
            if (remaining <= 0) {
                finishWithRemaining(current.runningState, remainingSeconds = 0)
                return
            }
            val now = clock.now().toEpochMilliseconds()
            val running = current.runningState.copy(
                remainingSeconds = remaining,
                endsAtEpochMillis = now + remaining * 1000L
            )
            _state.value = running
            // Re-assert foreground service in case it was stopped while paused.
            notificationScheduler.startPersistentNotification(running)
            startTimer()
        }
    }

    fun completeSprintManually() {
        when (val current = _state.value) {
            is SprintState.Running -> finishWithRemaining(current, remainingSeconds(current))
            is SprintState.Paused -> finishWithRemaining(current.runningState, current.remainingSecondsAtPause)
            else -> Unit
        }
    }

    /**
     * Atomically takes a finished sprint (transitions to Idle) so save can only run once.
     * Cancels persistent notification if still showing.
     */
    fun takeFinished(): SprintState.Finished? {
        val current = _state.value
        if (current !is SprintState.Finished) return null
        _state.value = SprintState.Idle
        notificationScheduler.cancelNotification()
        return current
    }

    fun dismissFinished() {
        if (_state.value is SprintState.Finished) {
            _state.value = SprintState.Idle
            notificationScheduler.cancelNotification()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(200.milliseconds)
                val current = _state.value
                if (current !is SprintState.Running) break

                val remaining = remainingSeconds(current)
                if (remaining <= 0) {
                    finishWithRemaining(current, remainingSeconds = 0)
                    break
                }
                if (remaining != current.remainingSeconds) {
                    val updated = current.copy(remainingSeconds = remaining)
                    _state.value = updated
                    notificationScheduler.updatePersistentNotification(updated, isPaused = false)
                }
            }
        }
    }

    private fun remainingSeconds(running: SprintState.Running): Int {
        val now = clock.now().toEpochMilliseconds()
        val millisLeft = running.endsAtEpochMillis - now
        if (millisLeft <= 0L) return 0
        return ((millisLeft + 999L) / 1000L).toInt().coerceAtMost(running.totalSeconds)
    }

    private fun finishWithRemaining(running: SprintState.Running, remainingSeconds: Int) {
        timerJob?.cancel()
        timerJob = null
        val remaining = remainingSeconds.coerceIn(0, running.totalSeconds)
        val elapsed = (running.totalSeconds - remaining).coerceIn(0, running.totalSeconds)
        _state.value = SprintState.Finished(
            taskId = running.taskId,
            dailyPlanItemId = running.dailyPlanItemId,
            description = running.description,
            durationSeconds = running.totalSeconds,
            elapsedSeconds = elapsed,
            startTimeEpochMillis = running.startTimeEpochMillis,
            isPomodoro = running.isPomodoro
        )
        notificationScheduler.cancelNotification()
    }
}

package com.checkit.domain

import com.checkit.notifications.CountdownScheduler
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

/**
 * Generic single-shot countdown state. Reusable by any caller (QuickNote, tasks, ...):
 * the manager only knows a display [content] string plus a wall-clock deadline.
 * In-memory only; a lost process loses the timer.
 */
sealed interface CountdownState {
    data object Idle : CountdownState

    data class Running(
        val content: String,
        val totalSeconds: Int,
        val remainingSeconds: Int,
        val startTimeEpochMillis: Long,
        /** Wall-clock deadline used to recompute remaining while running. */
        val endsAtEpochMillis: Long,
    ) : CountdownState

    data class Finished(
        val content: String,
        val durationSeconds: Int,
        val elapsedSeconds: Int,
        val startTimeEpochMillis: Long,
    ) : CountdownState
}

object CountdownDisplay {
    fun formatMmSs(remainingSeconds: Int): String {
        val safe = remainingSeconds.coerceAtLeast(0)
        return "${(safe / 60).toString().padStart(2, '0')}:${(safe % 60).toString().padStart(2, '0')}"
    }
}

class CountdownManager(
    private val notificationScheduler: CountdownScheduler,
    private val clock: Clock = Clock.System,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _state = MutableStateFlow<CountdownState>(CountdownState.Idle)
    val state: StateFlow<CountdownState> = _state.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Starts a countdown when idle or after a finished session.
     * Returns false if a countdown is already running (single active only).
     */
    fun start(
        content: String,
        durationSeconds: Int,
        startTimeEpochMillis: Long? = null
    ): Boolean {
        if (_state.value is CountdownState.Running) return false

        val safeDuration = durationSeconds.coerceAtLeast(1)
        val now = clock.now().toEpochMilliseconds()
        val start = startTimeEpochMillis ?: now
        val running = CountdownState.Running(
            content = content,
            totalSeconds = safeDuration,
            remainingSeconds = ((start + safeDuration * 1000L - now) / 1000L).toInt().coerceIn(1, safeDuration),
            startTimeEpochMillis = start,
            endsAtEpochMillis = start + safeDuration * 1000L,
        )
        timerJob?.cancel()
        _state.value = running
        notificationScheduler.startPersistentNotification(running)
        startTimer()
        return true
    }

    /** Silent cancel: no finished notification, back to idle. */
    fun stop() {
        if (_state.value !is CountdownState.Running) return
        timerJob?.cancel()
        timerJob = null
        _state.value = CountdownState.Idle
        notificationScheduler.cancelNotification()
    }

    /**
     * Atomically takes a finished countdown (transitions to Idle) so finish
     * handling can only run once. Cancels persistent notification if still showing.
     */
    fun takeFinished(): CountdownState.Finished? {
        val current = _state.value
        if (current !is CountdownState.Finished) return null
        _state.value = CountdownState.Idle
        notificationScheduler.cancelNotification()
        return current
    }

    fun dismissFinished() {
        if (_state.value is CountdownState.Finished) {
            _state.value = CountdownState.Idle
            notificationScheduler.cancelNotification()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(200.milliseconds)
                val current = _state.value
                if (current !is CountdownState.Running) break

                val remaining = remainingSeconds(current)
                if (remaining <= 0) {
                    finish(current)
                    break
                }
                if (remaining != current.remainingSeconds) {
                    val updated = current.copy(remainingSeconds = remaining)
                    _state.value = updated
                    notificationScheduler.updatePersistentNotification(updated)
                }
            }
        }
    }

    private fun remainingSeconds(running: CountdownState.Running): Int {
        val now = clock.now().toEpochMilliseconds()
        val millisLeft = running.endsAtEpochMillis - now
        if (millisLeft <= 0L) return 0
        return ((millisLeft + 999L) / 1000L).toInt().coerceAtMost(running.totalSeconds)
    }

    private fun finish(running: CountdownState.Running) {
        timerJob?.cancel()
        timerJob = null
        val finished = CountdownState.Finished(
            content = running.content,
            durationSeconds = running.totalSeconds,
            elapsedSeconds = running.totalSeconds,
            startTimeEpochMillis = running.startTimeEpochMillis,
        )
        _state.value = finished
        notificationScheduler.cancelNotification()
        scope.launch {
            notificationScheduler.showFinishedNotification(finished)
        }
    }

    companion object {
        const val COUNTDOWN_5_MIN_MILLIS = 5L * 60L * 1000L
        const val COUNTDOWN_10_MIN_MILLIS = 10L * 60L * 1000L
        const val COUNTDOWN_15_MIN_MILLIS = 15L * 60L * 1000L
        const val COUNTDOWN_30_MIN_MILLIS = 30L * 60L * 1000L
        const val COUNTDOWN_1_HOUR_MILLIS = 60L * 60L * 1000L
    }
}

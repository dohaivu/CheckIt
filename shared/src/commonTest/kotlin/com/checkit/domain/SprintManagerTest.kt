package com.checkit.domain

import com.checkit.notifications.SprintNotificationScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SprintManagerTest {

    @Test
    fun startSprintIsRejectedWhileRunningOrPaused() = runTest {
        val env = createEnv()
        assertTrue(env.manager.startSprint(null, null, "A", durationSeconds = 60))
        assertFalse(env.manager.startSprint(null, null, "B", durationSeconds = 60))

        env.manager.pauseSprint()
        assertIs<SprintState.Paused>(env.manager.state.value)
        assertFalse(env.manager.startSprint(null, null, "C", durationSeconds = 60))
    }

    @Test
    fun startSprintAllowedFromFinished() = runTest {
        val env = createEnv()
        assertTrue(env.manager.startSprint(null, null, "A", durationSeconds = 10))
        env.manager.completeSprintManually()
        assertIs<SprintState.Finished>(env.manager.state.value)
        assertTrue(env.manager.startSprint(null, null, "B", durationSeconds = 10, isPomodoro = true))
        assertIs<SprintState.Running>(env.manager.state.value)
    }

    @Test
    fun elapsedExcludesPausedTime() = runTest {
        val env = createEnv()
        assertTrue(env.manager.startSprint(null, 1L, "Focus", durationSeconds = 100))

        env.clock.advanceBy(30_000)
        env.manager.pauseSprint()
        val paused = assertIs<SprintState.Paused>(env.manager.state.value)
        assertEquals(70, paused.remainingSecondsAtPause)

        // Wall clock keeps moving while paused — must not count as work.
        env.clock.advanceBy(60_000)
        env.manager.completeSprintManually()

        val finished = assertIs<SprintState.Finished>(env.manager.state.value)
        assertEquals(30, finished.elapsedSeconds)
        assertEquals(100, finished.durationSeconds)
    }

    @Test
    fun remainingUsesDeadlineNotTickCount() = runTest {
        val env = createEnv()
        assertTrue(env.manager.startSprint(null, null, "Focus", durationSeconds = 10))

        // Jump almost to the end in one wall-clock step.
        env.clock.advanceBy(9_500)
        advanceTimeBy(200)
        runCurrent()

        val running = assertIs<SprintState.Running>(env.manager.state.value)
        assertEquals(1, running.remainingSeconds)

        env.clock.advanceBy(600)
        advanceTimeBy(200)
        runCurrent()

        val finished = assertIs<SprintState.Finished>(env.manager.state.value)
        assertEquals(10, finished.elapsedSeconds)
        assertEquals(0, finished.durationSeconds - finished.elapsedSeconds)
    }

    @Test
    fun resumeReassertsPersistentNotification() = runTest {
        val env = createEnv()
        assertTrue(env.manager.startSprint(null, null, "Focus", durationSeconds = 60))
        assertEquals(1, env.scheduler.startCount)

        env.manager.pauseSprint()
        env.manager.resumeSprint()

        assertEquals(2, env.scheduler.startCount)
        assertIs<SprintState.Running>(env.manager.state.value)
    }

    @Test
    fun finishAndTakeFinishedCancelNotificationOnce() = runTest {
        val env = createEnv()
        assertTrue(env.manager.startSprint(null, null, "Focus", durationSeconds = 60))
        env.manager.completeSprintManually()
        assertEquals(1, env.scheduler.cancelCount)

        val first = env.manager.takeFinished()
        assertIs<SprintState.Finished>(first)
        assertEquals(2, env.scheduler.cancelCount)

        assertNull(env.manager.takeFinished())
        assertIs<SprintState.Idle>(env.manager.state.value)
    }

    @Test
    fun manualCompleteWhileRunningUsesRemainingFromDeadline() = runTest {
        val env = createEnv()
        assertTrue(env.manager.startSprint(null, null, "Focus", durationSeconds = 120))
        env.clock.advanceBy(45_000)
        env.manager.completeSprintManually()

        val finished = assertIs<SprintState.Finished>(env.manager.state.value)
        assertEquals(45, finished.elapsedSeconds)
    }

    private fun TestScope.createEnv(): TestEnv {
        val clock = FakeClock(Instant.fromEpochMilliseconds(1_000_000L))
        val scheduler = RecordingSprintNotificationScheduler()
        val manager = SprintManager(
            notificationScheduler = scheduler,
            clock = clock,
            scope = backgroundScope
        )
        return TestEnv(manager, clock, scheduler)
    }

    private data class TestEnv(
        val manager: SprintManager,
        val clock: FakeClock,
        val scheduler: RecordingSprintNotificationScheduler
    )

    private class FakeClock(private var now: Instant) : Clock {
        override fun now(): Instant = now
        fun advanceBy(millis: Long) {
            now = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + millis)
        }
    }

    private class RecordingSprintNotificationScheduler : SprintNotificationScheduler {
        var startCount = 0
        var cancelCount = 0
        var updateCount = 0

        override fun startPersistentNotification(running: SprintState.Running) {
            startCount++
        }

        override fun updatePersistentNotification(running: SprintState.Running, isPaused: Boolean) {
            updateCount++
        }

        override fun cancelNotification() {
            cancelCount++
        }
    }
}

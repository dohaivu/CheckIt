package com.checkit.domain

import com.checkit.notifications.CountdownScheduler
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
class CountdownManagerTest {

    @Test
    fun startIsRejectedWhileRunning() = runTest {
        val env = createEnv()
        assertTrue(env.manager.start("A", durationSeconds = 60))
        assertFalse(env.manager.start("B", durationSeconds = 60))
        assertIs<CountdownState.Running>(env.manager.state.value)
    }

    @Test
    fun startAllowedFromFinished() = runTest {
        val env = createEnv()
        assertTrue(env.manager.start("A", durationSeconds = 60))
        env.manager.stop()
        // stop() goes back to idle, so a fresh start must work
        assertTrue(env.manager.start("B", durationSeconds = 60))
        assertIs<CountdownState.Running>(env.manager.state.value)
    }

    @Test
    fun stopCancelsSilentlyWithoutFinishedNotification() = runTest {
        val env = createEnv()
        assertTrue(env.manager.start("A", durationSeconds = 60))
        env.manager.stop()
        runCurrent()
        assertIs<CountdownState.Idle>(env.manager.state.value)
        assertEquals(1, env.scheduler.cancelCount)
        assertEquals(0, env.scheduler.finishedCount)
    }

    @Test
    fun expiryTransitionsToFinishedAndNotifies() = runTest {
        val env = createEnv()
        assertTrue(env.manager.start("Focus", durationSeconds = 10))

        env.clock.advanceBy(10_000)
        advanceTimeBy(300)
        runCurrent()

        val finished = assertIs<CountdownState.Finished>(env.manager.state.value)
        assertEquals("Focus", finished.content)
        assertEquals(10, finished.elapsedSeconds)
        assertEquals(1, env.scheduler.finishedCount)
    }

    @Test
    fun remainingUsesDeadlineNotTickCount() = runTest {
        val env = createEnv()
        assertTrue(env.manager.start("Focus", durationSeconds = 10))

        // Jump almost to the end in one wall-clock step.
        env.clock.advanceBy(9_500)
        advanceTimeBy(200)
        runCurrent()

        val running = assertIs<CountdownState.Running>(env.manager.state.value)
        assertEquals(1, running.remainingSeconds)
    }

    @Test
    fun takeFinishedTransitionsToIdleOnce() = runTest {
        val env = createEnv()
        assertTrue(env.manager.start("Focus", durationSeconds = 10))
        env.clock.advanceBy(10_000)
        advanceTimeBy(300)
        runCurrent()

        val first = env.manager.takeFinished()
        assertIs<CountdownState.Finished>(first)
        assertIs<CountdownState.Idle>(env.manager.state.value)
        assertNull(env.manager.takeFinished())
    }

    @Test
    fun formatMmSsPadsAndClamps() {
        assertEquals("05:00", CountdownDisplay.formatMmSs(300))
        assertEquals("00:09", CountdownDisplay.formatMmSs(9))
        assertEquals("10:10", CountdownDisplay.formatMmSs(610))
        assertEquals("00:00", CountdownDisplay.formatMmSs(-5))
    }

    private fun TestScope.createEnv(): TestEnv {
        val clock = FakeClock(Instant.fromEpochMilliseconds(1_000_000L))
        val scheduler = RecordingCountdownScheduler()
        val manager = CountdownManager(
            notificationScheduler = scheduler,
            clock = clock,
            scope = backgroundScope
        )
        return TestEnv(manager, clock, scheduler)
    }

    private data class TestEnv(
        val manager: CountdownManager,
        val clock: FakeClock,
        val scheduler: RecordingCountdownScheduler
    )

    private class FakeClock(private var now: Instant) : Clock {
        override fun now(): Instant = now
        fun advanceBy(millis: Long) {
            now = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + millis)
        }
    }

    private class RecordingCountdownScheduler : CountdownScheduler {
        var startCount = 0
        var cancelCount = 0
        var updateCount = 0
        var finishedCount = 0

        override fun startPersistentNotification(running: CountdownState.Running) {
            startCount++
        }

        override fun updatePersistentNotification(running: CountdownState.Running) {
            updateCount++
        }

        override fun cancelNotification() {
            cancelCount++
        }

        override suspend fun showFinishedNotification(finished: CountdownState.Finished) {
            finishedCount++
        }
    }
}

package com.checkit.domain

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckInReminderPolicyTest {
    @Test
    fun shouldShowWhenNoTimedItemIsNearCurrentTime() {
        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = listOf(CheckInReminderPlanItem(startTimeMinutes = 9 * 60, endTimeMinutes = 10 * 60)),
            nowMinutes = 12 * 60,
            nowMillis = 10_000L,
            lastShownAtMillis = null
        )

        assertTrue(shouldShow)
    }

    @Test
    fun shouldNotShowWhenTimedItemIsWithinNearbyWindow() {
        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = listOf(CheckInReminderPlanItem(startTimeMinutes = 12 * 60 + 10, endTimeMinutes = 12 * 60 + 40)),
            nowMinutes = 12 * 60,
            nowMillis = 10_000L,
            lastShownAtMillis = null
        )

        assertFalse(shouldShow)
    }

    @Test
    fun shouldNotShowWhenLastReminderIsInsideCooldown() {
        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = emptyList(),
            nowMinutes = 12 * 60,
            nowMillis = CheckInReminderPolicy.MinimumRepeatIntervalMillis,
            lastShownAtMillis = CheckInReminderPolicy.MinimumRepeatIntervalMillis - 1L
        )

        assertFalse(shouldShow)
    }

    @Test
    fun shouldNotLoadItemsWhenLastReminderIsInsideCooldown() = runTest {
        var loadCount = 0

        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            nowMinutes = 12 * 60,
            nowMillis = CheckInReminderPolicy.MinimumRepeatIntervalMillis,
            lastShownAtMillis = CheckInReminderPolicy.MinimumRepeatIntervalMillis - 1L,
            loadItems = {
                loadCount += 1
                emptyList()
            }
        )

        assertFalse(shouldShow)
        assertEquals(0, loadCount)
    }

    @Test
    fun shouldNotLoadItemsDuringDoNotDisturbHours() = runTest {
        var loadCount = 0

        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            nowMinutes = 23 * 60,
            nowMillis = CheckInReminderPolicy.MinimumRepeatIntervalMillis,
            lastShownAtMillis = null,
            loadItems = {
                loadCount += 1
                emptyList()
            }
        )

        assertFalse(shouldShow)
        assertEquals(0, loadCount)
    }

    @Test
    fun shouldShowAfterCooldownExpires() {
        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = emptyList(),
            nowMinutes = 12 * 60,
            nowMillis = CheckInReminderPolicy.MinimumRepeatIntervalMillis,
            lastShownAtMillis = 0L
        )

        assertTrue(shouldShow)
    }

    @Test
    fun shouldNotShowWhenRecentDoneActivityIsWithinIdleThreshold() {
        val nowMillis = 1_000_000L
        val items = listOf(
            CheckInReminderPlanItem(
                startTimeMinutes = null,
                endTimeMinutes = null,
                isDone = true,
                completedAtMillis = nowMillis - 30L * 60_000L
            )
        )

        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = items,
            nowMinutes = 12 * 60,
            nowMillis = nowMillis,
            lastShownAtMillis = null,
            idleThresholdMinutes = 60
        )

        assertFalse(shouldShow)
    }

    @Test
    fun shouldShowWhenLastDoneActivityExceedsIdleThreshold() {
        val nowMillis = 1_000_000L
        val items = listOf(
            CheckInReminderPlanItem(
                startTimeMinutes = null,
                endTimeMinutes = null,
                isDone = true,
                completedAtMillis = nowMillis - 90L * 60_000L
            )
        )

        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = items,
            nowMinutes = 12 * 60,
            nowMillis = nowMillis,
            lastShownAtMillis = null,
            idleThresholdMinutes = 60
        )

        assertTrue(shouldShow)
    }

    @Test
    fun shouldRespectCustomIdleThreshold() {
        val nowMillis = 1_000_000L
        val items = listOf(
            CheckInReminderPlanItem(
                startTimeMinutes = null,
                endTimeMinutes = null,
                isDone = true,
                completedAtMillis = nowMillis - 40L * 60_000L
            )
        )

        assertFalse(
            CheckInReminderPolicy.shouldShowReminder(
                items = items,
                nowMinutes = 12 * 60,
                nowMillis = nowMillis,
                lastShownAtMillis = null,
                idleThresholdMinutes = 60
            )
        )
        assertTrue(
            CheckInReminderPolicy.shouldShowReminder(
                items = items,
                nowMinutes = 12 * 60,
                nowMillis = nowMillis,
                lastShownAtMillis = null,
                idleThresholdMinutes = 30
            )
        )
    }

    @Test
    fun shouldNotShowInEarlyMorningWhenNothingDoneYet() {
        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = emptyList(),
            nowMinutes = 8 * 60,
            nowMillis = 10_000L,
            lastShownAtMillis = null
        )

        assertFalse(shouldShow)
    }

    @Test
    fun shouldShowAfterMorningGuardWhenNothingDoneYet() {
        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = emptyList(),
            nowMinutes = 10 * 60,
            nowMillis = 10_000L,
            lastShownAtMillis = null
        )

        assertTrue(shouldShow)
    }

    @Test
    fun shouldNotShowWhenNearbyItemExistsEvenIfIdle() {
        val nowMillis = 1_000_000L
        val items = listOf(
            CheckInReminderPlanItem(
                startTimeMinutes = 12 * 60 + 10,
                endTimeMinutes = 12 * 60 + 40,
                isDone = true,
                completedAtMillis = nowMillis - 180L * 60_000L
            )
        )

        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = items,
            nowMinutes = 12 * 60,
            nowMillis = nowMillis,
            lastShownAtMillis = null,
            idleThresholdMinutes = 60
        )

        assertFalse(shouldShow)
    }

    @Test
    fun lastDonePrefersCompletedAtOverHandledAt() {
        val items = listOf(
            CheckInReminderPlanItem(
                startTimeMinutes = null,
                endTimeMinutes = null,
                isDone = true,
                completedAtMillis = 5_000L,
                handledAtMillis = 9_000L
            ),
            CheckInReminderPlanItem(
                startTimeMinutes = null,
                endTimeMinutes = null,
                isDone = false,
                completedAtMillis = 99_000L
            )
        )

        assertEquals(5_000L, CheckInReminderPolicy.lastDoneAtMillis(items))
        assertEquals(4L, CheckInReminderPolicy.idleMinutesSinceLastDone(items, 5_000L + 4L * 60_000L))
    }

    @Test
    fun idleCheckInMessageAppendsGap() {
        val base = NotificationMessage.idleCheckIn(null)
        assertFalse(base.body.contains("No Done"))

        val withGap = NotificationMessage.idleCheckIn(90L)
        assertTrue(withGap.body.contains("No Done in the last 1h 30m."))

        val shortGap = NotificationMessage.idleCheckIn(45L)
        assertTrue(shortGap.body.contains("No Done in the last 45m."))
    }

    @Test
    fun doNotDisturbBlocksNotificationsFromTenPmToSixAm() {
        assertTrue(NotificationDoNotDisturbPolicy.canNotifyAt(6 * 60))
        assertTrue(NotificationDoNotDisturbPolicy.canNotifyAt(21 * 60 + 59))
        assertFalse(NotificationDoNotDisturbPolicy.canNotifyAt(22 * 60))
        assertFalse(NotificationDoNotDisturbPolicy.canNotifyAt(5 * 60 + 59))
    }
}

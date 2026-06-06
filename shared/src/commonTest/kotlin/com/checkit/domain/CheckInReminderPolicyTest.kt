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
    fun doNotDisturbBlocksNotificationsFromTenPmToSixAm() {
        assertTrue(NotificationDoNotDisturbPolicy.canNotifyAt(6 * 60))
        assertTrue(NotificationDoNotDisturbPolicy.canNotifyAt(21 * 60 + 59))
        assertFalse(NotificationDoNotDisturbPolicy.canNotifyAt(22 * 60))
        assertFalse(NotificationDoNotDisturbPolicy.canNotifyAt(5 * 60 + 59))
    }
}

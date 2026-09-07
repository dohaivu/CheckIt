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
    fun shouldShowWhenTimedItemIsWithinNearbyWindowButNothingDone() {
        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = listOf(CheckInReminderPlanItem(startTimeMinutes = 12 * 60 + 10, endTimeMinutes = 12 * 60 + 40)),
            nowMinutes = 12 * 60,
            nowMillis = 10_000L,
            lastShownAtMillis = null
        )

        assertTrue(shouldShow)
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
    fun shouldNotShowWhenLastBlockEndedWithinIdleThreshold() {
        val items = listOf(
            CheckInReminderPlanItem(
                startTimeMinutes = 11 * 60,
                endTimeMinutes = 12 * 60 - 30,
                isDone = true
            )
        )

        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = items,
            nowMinutes = 12 * 60,
            nowMillis = 10_000L,
            lastShownAtMillis = null,
            idleThresholdMinutes = 60
        )

        assertFalse(shouldShow)
    }

    @Test
    fun shouldShowWhenLastBlockEndedBeyondIdleThreshold() {
        val items = listOf(
            CheckInReminderPlanItem(
                startTimeMinutes = 9 * 60,
                endTimeMinutes = 10 * 60,
                isDone = true
            )
        )

        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = items,
            nowMinutes = 12 * 60,
            nowMillis = 10_000L,
            lastShownAtMillis = null,
            idleThresholdMinutes = 60
        )

        assertTrue(shouldShow)
    }

    @Test
    fun shouldRespectCustomIdleThreshold() {
        val items = listOf(
            CheckInReminderPlanItem(
                startTimeMinutes = 11 * 60,
                endTimeMinutes = 12 * 60 - 40,
                isDone = true
            )
        )

        assertFalse(
            CheckInReminderPolicy.shouldShowReminder(
                items = items,
                nowMinutes = 12 * 60,
                nowMillis = 10_000L,
                lastShownAtMillis = null,
                idleThresholdMinutes = 60
            )
        )
        assertTrue(
            CheckInReminderPolicy.shouldShowReminder(
                items = items,
                nowMinutes = 12 * 60,
                nowMillis = 10_000L,
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
    fun shouldShowWhenNearbyItemExistsAndIdle() {
        val items = listOf(
            CheckInReminderPlanItem(
                startTimeMinutes = 12 * 60 + 10,
                endTimeMinutes = 12 * 60 + 40,
                isDone = false
            )
        )

        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = items,
            nowMinutes = 12 * 60,
            nowMillis = 10_000L,
            lastShownAtMillis = null,
            idleThresholdMinutes = 60
        )

        assertTrue(shouldShow)
    }

    @Test
    fun currentTimedItemMatchesInclusiveBounds() {
        val item = CheckInReminderPlanItem(
            startTimeMinutes = 14 * 60,
            endTimeMinutes = 15 * 60,
            title = "Write report"
        )
        val items = listOf(item)

        assertEquals(item, CheckInReminderPolicy.currentTimedItem(items, 14 * 60))
        assertEquals(item, CheckInReminderPolicy.currentTimedItem(items, 14 * 60 + 30))
        assertEquals(item, CheckInReminderPolicy.currentTimedItem(items, 15 * 60))
        assertEquals(null, CheckInReminderPolicy.currentTimedItem(items, 14 * 60 - 1))
        assertEquals(null, CheckInReminderPolicy.currentTimedItem(items, 15 * 60 + 1))
    }

    @Test
    fun currentTimedItemIgnoresUntimedAndDoneItems() {
        val done = CheckInReminderPlanItem(
            startTimeMinutes = 14 * 60,
            endTimeMinutes = 15 * 60,
            isDone = true,
            title = "Finished work"
        )
        val untimed = CheckInReminderPlanItem(
            startTimeMinutes = null,
            endTimeMinutes = null,
            title = "Loose task"
        )

        assertEquals(null, CheckInReminderPolicy.currentTimedItem(listOf(done), 14 * 60 + 30))
        assertEquals(null, CheckInReminderPolicy.currentTimedItem(listOf(untimed), 14 * 60 + 30))
        assertEquals(null, CheckInReminderPolicy.currentTimedItem(emptyList(), 14 * 60 + 30))
    }

    @Test
    fun currentTimedItemPrefersMostRecentlyStartedOnOverlap() {
        val early = CheckInReminderPlanItem(
            startTimeMinutes = 14 * 60,
            endTimeMinutes = 16 * 60,
            title = "Long block"
        )
        val late = CheckInReminderPlanItem(
            startTimeMinutes = 15 * 60,
            endTimeMinutes = 15 * 60 + 30,
            title = "Meeting"
        )

        assertEquals(
            late,
            CheckInReminderPolicy.currentTimedItem(listOf(early, late), 15 * 60 + 10)
        )
    }

    @Test
    fun decideReturnsCurrentItemOnlyWhenShowing() {
        val current = CheckInReminderPlanItem(
            startTimeMinutes = 12 * 60 - 10,
            endTimeMinutes = 12 * 60 + 50,
            isDone = false,
            title = "Deep work"
        )
        val staleDone = CheckInReminderPlanItem(
            startTimeMinutes = 9 * 60,
            endTimeMinutes = 11 * 60,
            isDone = true,
            title = "Finished block"
        )
        val recentDone = CheckInReminderPlanItem(
            startTimeMinutes = 11 * 60,
            endTimeMinutes = 12 * 60 - 10,
            isDone = true,
            title = "Just finished"
        )

        val idleDecision = CheckInReminderPolicy.decide(
            items = listOf(staleDone),
            nowMinutes = 12 * 60,
            idleThresholdMinutes = 60
        )
        assertTrue(idleDecision.shouldShow)
        assertEquals(60, idleDecision.idleMinutes)
        // Done item overlapping now is excluded from the current-item reference.
        assertEquals(null, idleDecision.currentItem)

        val activeDecision = CheckInReminderPolicy.decide(
            items = listOf(current, recentDone),
            nowMinutes = 12 * 60,
            idleThresholdMinutes = 60
        )
        assertFalse(activeDecision.shouldShow)
        assertEquals(null, activeDecision.currentItem)

        val nagDecision = CheckInReminderPolicy.decide(
            items = listOf(current, staleDone),
            nowMinutes = 12 * 60,
            idleThresholdMinutes = 60
        )
        assertTrue(nagDecision.shouldShow)
        assertEquals(current, nagDecision.currentItem)
    }

    @Test
    fun lastDoneUsesLatestPlannedEnd() {
        val items = listOf(
            CheckInReminderPlanItem(
                startTimeMinutes = 9 * 60,
                endTimeMinutes = 10 * 60,
                isDone = true
            ),
            // Point item counts by its start.
            CheckInReminderPlanItem(
                startTimeMinutes = 11 * 60,
                endTimeMinutes = null,
                isDone = true
            ),
            CheckInReminderPlanItem(
                startTimeMinutes = 8 * 60,
                endTimeMinutes = 14 * 60,
                isDone = false
            )
        )

        assertEquals(11 * 60, CheckInReminderPolicy.lastDoneEndMinutes(items))
        assertEquals(60, CheckInReminderPolicy.idleMinutesSinceLastDone(items, 12 * 60))
    }

    @Test
    fun lastDoneIgnoresUntimedDoneItems() {
        val items = listOf(
            CheckInReminderPlanItem(
                startTimeMinutes = null,
                endTimeMinutes = null,
                isDone = true
            )
        )

        assertEquals(null, CheckInReminderPolicy.lastDoneEndMinutes(items))
        assertEquals(null, CheckInReminderPolicy.idleMinutesSinceLastDone(items, 12 * 60))
    }

    @Test
    fun idleMinutesClampAtZeroWhenLastBlockEndsLater() {
        val items = listOf(
            CheckInReminderPlanItem(
                startTimeMinutes = 12 * 60,
                endTimeMinutes = 13 * 60,
                isDone = true
            )
        )

        assertEquals(0, CheckInReminderPolicy.idleMinutesSinceLastDone(items, 12 * 60))
        assertFalse(
            CheckInReminderPolicy.isIdle(
                items = items,
                nowMinutes = 12 * 60,
                idleThresholdMinutes = 60
            )
        )
    }

    @Test
    fun idleCheckInMessageReplacesTitleWithGap() {
        val base = NotificationMessage.idleCheckIn(null)
        assertFalse(base.title.contains("No Done"))

        val withGap = NotificationMessage.idleCheckIn(90)
        assertEquals("No Done in the last 1h 30m", withGap.title)
        assertFalse(withGap.body.contains("No Done"))

        val shortGap = NotificationMessage.idleCheckIn(45)
        assertEquals("No Done in the last 45m", shortGap.title)
        assertFalse(shortGap.body.contains("No Done"))
    }

    @Test
    fun currentItemCheckInMessageNamesItem() {
        val message = NotificationMessage.currentItemCheckIn("Write report", 90)

        assertTrue(message.title.contains("Write report"))
        assertFalse(message.body.contains("Write report"))
        assertTrue(message.body.contains("right now"))
        assertTrue(message.body.contains("No Done in the last 1h 30m."))
    }

    @Test
    fun currentItemCheckInMessageTrimsLongTitles() {
        val message = NotificationMessage.currentItemCheckIn(
            "A very long task title that goes on and on past forty characters",
            null
        )

        assertTrue(message.title.length <= "Still on \"\"?".length + 40)
        assertFalse(message.body.contains("No Done"))
    }

    @Test
    fun forceBypassesCooldown() {
        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = emptyList(),
            nowMinutes = 12 * 60,
            nowMillis = CheckInReminderPolicy.MinimumRepeatIntervalMillis,
            lastShownAtMillis = CheckInReminderPolicy.MinimumRepeatIntervalMillis - 1L,
            force = true
        )

        assertTrue(shouldShow)
    }

    @Test
    fun forceBypassesDoNotDisturb() {
        val shouldShow = CheckInReminderPolicy.shouldShowReminder(
            items = emptyList(),
            nowMinutes = 23 * 60,
            nowMillis = CheckInReminderPolicy.MinimumRepeatIntervalMillis,
            lastShownAtMillis = null,
            force = true
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

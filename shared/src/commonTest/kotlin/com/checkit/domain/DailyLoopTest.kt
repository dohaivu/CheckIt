package com.checkit.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyLoopTest {
    private val today = LocalDate(2026, 7, 10)
    private val yesterday = LocalDate(2026, 7, 9)

    @Test
    fun yesterdayLeftoversReturnsPlannedItemsOnly() {
        val plans = listOf(
            DailyPlan(
                date = yesterday,
                items = listOf(
                    planItem(1L, "Carry me", DailyPlanItemStatus.Planned),
                    planItem(2L, "Done", DailyPlanItemStatus.Done),
                    planItem(
                        id = 3L,
                        title = DayReviewWinNote.Title,
                        status = DailyPlanItemStatus.Planned,
                        source = DailyPlanItemSource.MyDayNote
                    )
                )
            )
        )
        val leftovers = YesterdayLeftovers.items(plans, today)
        assertEquals(listOf(1L), leftovers.map { it.id })
    }

    @Test
    fun pendingForTodaySkipsTaskAlreadyOnToday() {
        val leftovers = listOf(
            planItem(1L, "A", DailyPlanItemStatus.Planned, taskId = 10L),
            planItem(2L, "B", DailyPlanItemStatus.Planned, taskId = 20L),
            planItem(3L, "Note", DailyPlanItemStatus.Planned, source = DailyPlanItemSource.MyDayNote)
        )
        val todayPlan = DailyPlan(
            date = today,
            items = listOf(planItem(99L, "Already", DailyPlanItemStatus.Planned, taskId = 10L))
        )
        val pending = YesterdayLeftovers.pendingForToday(leftovers, todayPlan)
        assertEquals(listOf(2L, 3L), pending.map { it.id })
    }

    @Test
    fun leftoversBannerPolicy() {
        assertTrue(
            LeftoversBannerPolicy.shouldShow(
                pendingCount = 2,
                leftoversBannerDismissedEpochDay = null,
                todayEpochDay = 100
            )
        )
        assertFalse(
            LeftoversBannerPolicy.shouldShow(
                pendingCount = 2,
                leftoversBannerDismissedEpochDay = 100,
                todayEpochDay = 100
            )
        )
        assertFalse(
            LeftoversBannerPolicy.shouldShow(
                pendingCount = 0,
                leftoversBannerDismissedEpochDay = null,
                todayEpochDay = 100
            )
        )
    }

    @Test
    fun planAssistBannerPolicy() {
        assertTrue(
            PlanAssistBannerPolicy.shouldShow(
                todayPlanItemCount = 0,
                planReminderEnabled = true,
                planReminderTimeMinutes = 7 * 60,
                reviewReminderTimeMinutes = 21 * 60,
                lastDayPlanDismissedEpochDay = null,
                todayEpochDay = 100,
                nowMinutes = 8 * 60
            )
        )
        assertFalse(
            PlanAssistBannerPolicy.shouldShow(
                todayPlanItemCount = 1,
                planReminderEnabled = true,
                planReminderTimeMinutes = 7 * 60,
                reviewReminderTimeMinutes = 21 * 60,
                lastDayPlanDismissedEpochDay = null,
                todayEpochDay = 100,
                nowMinutes = 8 * 60
            )
        )
        assertFalse(
            PlanAssistBannerPolicy.shouldShow(
                todayPlanItemCount = 0,
                planReminderEnabled = true,
                planReminderTimeMinutes = 7 * 60,
                reviewReminderTimeMinutes = 21 * 60,
                lastDayPlanDismissedEpochDay = null,
                todayEpochDay = 100,
                nowMinutes = 6 * 60
            )
        )
        assertFalse(
            PlanAssistBannerPolicy.shouldShow(
                todayPlanItemCount = 0,
                planReminderEnabled = true,
                planReminderTimeMinutes = 7 * 60,
                reviewReminderTimeMinutes = 21 * 60,
                lastDayPlanDismissedEpochDay = null,
                todayEpochDay = 100,
                nowMinutes = 21 * 60
            )
        )
        assertFalse(
            PlanAssistBannerPolicy.shouldShow(
                todayPlanItemCount = 0,
                planReminderEnabled = true,
                planReminderTimeMinutes = 7 * 60,
                reviewReminderTimeMinutes = 21 * 60,
                lastDayPlanDismissedEpochDay = 100,
                todayEpochDay = 100,
                nowMinutes = 8 * 60
            )
        )
    }

    private fun planItem(
        id: Long,
        title: String,
        status: DailyPlanItemStatus,
        taskId: Long? = null,
        source: DailyPlanItemSource = if (taskId != null) {
            DailyPlanItemSource.ExistingTask
        } else {
            DailyPlanItemSource.MyDayTask
        }
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = yesterday.toEpochDays().toInt(),
        taskId = taskId,
        title = title,
        note = null,
        source = source,
        status = status,
        sortOrder = id.toInt(),
        addedAtMillis = 0L
    )
}

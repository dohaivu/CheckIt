package com.checkit.domain

import com.checkit.ui.MinutesPerDay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Shared helpers for the daily loop (plan → work → review → carry-over).
 * PR2: morning leftovers. PR3: plan assist.
 */
object YesterdayLeftovers {
    fun sourceDate(today: LocalDate): LocalDate = today.minus(1, DateTimeUnit.DAY)

    fun items(
        dailyPlans: List<DailyPlan>,
        today: LocalDate
    ): List<DailyPlanItem> {
        val yesterday = sourceDate(today)
        val plan = dailyPlans.firstOrNull { it.date == yesterday } ?: return emptyList()
        return plan.items
            .asSequence()
            .filter { it.status == DailyPlanItemStatus.Planned && it.handledAtMillis == null }
            .sortedBy { it.startTimeMinutes ?: Int.MAX_VALUE }
            .toList()
    }

    /**
     * Leftovers that still need carrying (task-linked items already on today,
     * and items already carried onto today, are omitted).
     */
    fun pendingForToday(
        leftovers: List<DailyPlanItem>,
        todayPlan: DailyPlan?
    ): List<DailyPlanItem> {
        val todayItems = todayPlan?.items.orEmpty()
        val todayTaskIds = todayItems.mapNotNull { it.taskId }.toSet()
        val todayCarriedFromIds = todayItems.mapNotNull { it.carriedFromItemId }.toSet()
        return leftovers.filter { item ->
            val taskId = item.taskId
            (taskId == null || taskId !in todayTaskIds) && item.id !in todayCarriedFromIds
        }
    }
}

object LeftoversBannerPolicy {
    fun shouldShow(
        pendingCount: Int,
        leftoversBannerDismissedEpochDay: Int?,
        todayEpochDay: Int
    ): Boolean {
        if (pendingCount <= 0) return false
        if (leftoversBannerDismissedEpochDay == todayEpochDay) return false
        return true
    }
}

object PlanAssistBannerPolicy {
    /**
     * Morning plan assist: after plan reminder time, before review time,
     * when today's plan is empty and the user has not dismissed for the day.
     */
    fun shouldShow(
        todayPlanItemCount: Int,
        planReminderEnabled: Boolean,
        planReminderTimeMinutes: Int,
        reviewReminderTimeMinutes: Int,
        lastDayPlanDismissedEpochDay: Int?,
        todayEpochDay: Int,
        nowMinutes: Int
    ): Boolean {
        if (!planReminderEnabled) return false
        if (todayPlanItemCount > 0) return false
        if (lastDayPlanDismissedEpochDay == todayEpochDay) return false
        val planAt = planReminderTimeMinutes.coerceIn(0, MinutesPerDay - 1)
        val reviewAt = reviewReminderTimeMinutes.coerceIn(0, MinutesPerDay - 1)
        val now = nowMinutes.coerceIn(0, MinutesPerDay - 1)
        if (now < planAt) return false
        // Prefer not overlapping evening review prompt window when review is later the same day.
        if (reviewAt > planAt && now >= reviewAt) return false
        return true
    }
}


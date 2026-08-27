package com.checkit.domain

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


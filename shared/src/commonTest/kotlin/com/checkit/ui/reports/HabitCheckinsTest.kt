package com.checkit.ui.reports

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals

class HabitCheckinsTest {

    private fun doneHabitItem(
        id: Long,
        taskId: Long,
        title: String
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = 0,
        taskId = taskId,
        title = title,
        source = DailyPlanItemSource.ExistingTask,
        status = DailyPlanItemStatus.Done,
        isHabit = true,
        sortOrder = 0,
        addedAtMillis = 0L,
        completedAtMillis = 1L
    )

    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val today = LocalDate(2026, 8, 2)
        val doneDates = setOf(
            today,
            today.minus(1, DateTimeUnit.DAY),
            today.minus(2, DateTimeUnit.DAY),
            today.minus(4, DateTimeUnit.DAY)
        )

        assertEquals(3, calculateStreak(doneDates, today))
    }

    @Test
    fun streakGivesGraceForTodayNotYetDone() {
        val today = LocalDate(2026, 8, 2)
        val doneDates = setOf(
            today.minus(1, DateTimeUnit.DAY),
            today.minus(2, DateTimeUnit.DAY),
            today.minus(3, DateTimeUnit.DAY)
        )

        assertEquals(3, calculateStreak(doneDates, today))
    }

    @Test
    fun streakIsZeroWhenYesterdayMissed() {
        val today = LocalDate(2026, 8, 2)
        val doneDates = setOf(
            today.minus(2, DateTimeUnit.DAY),
            today.minus(3, DateTimeUnit.DAY)
        )

        assertEquals(0, calculateStreak(doneDates, today))
    }

    @Test
    fun buildCheckinsGroupsByTaskAndCountsDistinctDoneDates() {
        val today = LocalDate(2026, 8, 2)
        val plans = listOf(
            DailyPlan(
                date = today,
                items = listOf(
                    doneHabitItem(id = 1L, taskId = 10L, title = "Meditate"),
                    doneHabitItem(id = 2L, taskId = 11L, title = "Run")
                )
            ),
            DailyPlan(
                date = today.minus(1, DateTimeUnit.DAY),
                items = listOf(
                    doneHabitItem(id = 3L, taskId = 10L, title = "Meditate")
                )
            )
        )

        val checkins = buildHabitCheckins(plans, today)

        assertEquals(2, checkins.size)
        val meditate = checkins.first { it.taskId == 10L }
        assertEquals("Meditate", meditate.title)
        assertEquals(2, meditate.totalDone)
        assertEquals(2, meditate.streak)
        assertEquals(setOf(today, today.minus(1, DateTimeUnit.DAY)), meditate.doneDates)
        val run = checkins.first { it.taskId == 11L }
        assertEquals(1, run.totalDone)
        assertEquals(1, run.streak)
    }

    @Test
    fun buildCheckinsIgnoresPlannedAndNonHabitItems() {
        val today = LocalDate(2026, 8, 2)
        val plannedHabit = doneHabitItem(id = 1L, taskId = 10L, title = "Meditate").copy(
            status = DailyPlanItemStatus.Planned,
            completedAtMillis = null
        )
        val regularTask = doneHabitItem(id = 2L, taskId = 11L, title = "Read").copy(isHabit = false)
        val plans = listOf(
            DailyPlan(
                date = today,
                items = listOf(plannedHabit, regularTask)
            )
        )

        val checkins = buildHabitCheckins(plans, today)

        assertEquals(emptyList(), checkins)
    }

    @Test
    fun buildCheckinsSortsByStreakDescending() {
        val today = LocalDate(2026, 8, 2)
        val plans = listOf(
            DailyPlan(
                date = today,
                items = listOf(
                    doneHabitItem(id = 1L, taskId = 10L, title = "Meditate"),
                    doneHabitItem(id = 2L, taskId = 11L, title = "Run")
                )
            ),
            DailyPlan(
                date = today.minus(1, DateTimeUnit.DAY),
                items = listOf(
                    doneHabitItem(id = 3L, taskId = 10L, title = "Meditate")
                )
            )
        )

        val checkins = buildHabitCheckins(plans, today)

        assertEquals(listOf("Meditate", "Run"), checkins.map { it.title })
    }

    @Test
    fun heatmapColumnsAreWeekAlignedAndFutureCellsAreNull() {
        val today = LocalDate(2026, 8, 4)
        val columns = buildHeatmapColumns(today, weekCount = 2)

        assertEquals(2, columns.size)
        assertEquals(7, columns[0].size)
        val lastColumn = columns.last()
        assertEquals(LocalDate(2026, 8, 3), lastColumn.first())
        assertEquals(today, lastColumn[1])
        assertEquals(null, lastColumn[2])
        assertEquals(null, lastColumn.last())
    }
}

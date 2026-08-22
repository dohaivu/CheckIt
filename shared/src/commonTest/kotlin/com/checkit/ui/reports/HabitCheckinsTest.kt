package com.checkit.ui.reports

import com.checkit.domain.HabitDailyRollup
import com.checkit.ui.reflect.buildHabitCheckins
import com.checkit.ui.reflect.buildHeatmapMonths
import com.checkit.ui.reflect.calculateStreak
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals

class HabitCheckinsTest {

    private fun rollup(
        date: LocalDate,
        taskId: Long,
        title: String,
        doneMinutes: Int = 0
    ) = HabitDailyRollup(
        dateEpochDays = date.toEpochDays().toInt(),
        habitKey = "task:$taskId",
        title = title,
        doneMinutes = doneMinutes
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
    fun buildCheckinsGroupsByHabitAndCountsDistinctDoneDates() {
        val today = LocalDate(2026, 8, 2)
        val rollups = listOf(
            rollup(today, taskId = 10L, title = "Meditate"),
            rollup(today, taskId = 11L, title = "Run"),
            rollup(today.minus(1, DateTimeUnit.DAY), taskId = 10L, title = "Meditate")
        )

        val checkins = buildHabitCheckins(rollups, today)

        assertEquals(2, checkins.size)
        val meditate = checkins.first { it.habitKey == "task:10" }
        assertEquals("Meditate", meditate.title)
        assertEquals(2, meditate.totalDone)
        assertEquals(2, meditate.streak)
        assertEquals(setOf(today, today.minus(1, DateTimeUnit.DAY)), meditate.doneDates)
        val run = checkins.first { it.habitKey == "task:11" }
        assertEquals(1, run.totalDone)
        assertEquals(1, run.streak)
    }

    @Test
    fun buildCheckinsSumsMinutesPerDate() {
        val today = LocalDate(2026, 8, 2)
        val rollups = listOf(
            rollup(today, taskId = 10L, title = "Meditate", doneMinutes = 60),
            rollup(today.minus(1, DateTimeUnit.DAY), taskId = 10L, title = "Meditate", doneMinutes = 60)
        )

        val checkins = buildHabitCheckins(rollups, today)

        val meditate = checkins.single { it.habitKey == "task:10" }
        assertEquals(60, meditate.doneMinutesByDate[today])
        assertEquals(60, meditate.doneMinutesByDate[today.minus(1, DateTimeUnit.DAY)])
    }

    @Test
    fun buildCheckinsEmptyWithoutRollups() {
        val today = LocalDate(2026, 8, 2)

        assertEquals(emptyList(), buildHabitCheckins(emptyList(), today))
    }

    @Test
    fun buildCheckinsSortsByStreakDescending() {
        val today = LocalDate(2026, 8, 2)
        val rollups = listOf(
            rollup(today, taskId = 10L, title = "Meditate"),
            rollup(today, taskId = 11L, title = "Run"),
            rollup(today.minus(1, DateTimeUnit.DAY), taskId = 10L, title = "Meditate")
        )

        val checkins = buildHabitCheckins(rollups, today)

        assertEquals(listOf("Meditate", "Run"), checkins.map { it.title })
    }

    @Test
    fun heatmapMonthsRenderWeeksAsHorizontalRows() {
        val today = LocalDate(2026, 8, 4)
        val months = buildHeatmapMonths(today)

        assertEquals(1, months.size)
        val weeks = months.single().weeks
        val firstWeek = weeks.first()
        assertEquals(7, firstWeek.size)
        assertEquals(null, firstWeek[0])
        assertEquals(LocalDate(2026, 8, 1), firstWeek[5])
        val lastWeek = weeks.last()
        assertEquals(LocalDate(2026, 8, 31), lastWeek.first())
        assertEquals(null, lastWeek[1])
    }

    @Test
    fun buildHeatmapMonthsReturnsRequestedCountOldestFirst() {
        val today = LocalDate(2026, 8, 4)
        val months = buildHeatmapMonths(today, monthCount = 3)

        assertEquals(3, months.size)
        assertEquals(LocalDate(2026, 6, 1), months[0].monthStart)
        assertEquals(LocalDate(2026, 7, 1), months[1].monthStart)
        assertEquals(LocalDate(2026, 8, 1), months[2].monthStart)
    }
}

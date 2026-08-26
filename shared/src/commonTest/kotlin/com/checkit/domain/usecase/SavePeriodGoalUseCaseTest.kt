package com.checkit.domain.usecase

import com.checkit.domain.FocusPeriod
import com.checkit.domain.MetricUnit
import com.checkit.domain.Period
import com.checkit.domain.PeriodMetric
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class SavePeriodGoalUseCaseTest {
    private val date = LocalDate(2026, 7, 9)

    @Test
    fun savesPeriodGoalWithBoundsAndTrimsReview() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodGoalUseCase(repository)
        val focus = FocusPeriod(Period.Week, date)

        save(focus, review = "  Solid week, shipped the PR.  ")

        val goal = repository.observePeriodGoals().first().single()
        assertEquals(Period.Week, goal.period)
        assertEquals(focus.start.toEpochDays().toInt(), goal.startEpochDays)
        assertEquals(focus.endExclusive.toEpochDays().toInt(), goal.endEpochDays)
        assertEquals("Solid week, shipped the PR.", goal.review)
        assertNull(goal.goal)
    }

    @Test
    fun savesRatingsOnCurrentPeriodGoal() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodGoalUseCase(repository)
        val focus = FocusPeriod(Period.Week, date)

        save(focus, review = "Solid week", rating = 4.5f)

        val goal = repository.observePeriodGoals().first().single()
        assertEquals(4.5f, goal.rating)
    }

    @Test
    fun savesGoalOnCurrentPeriodGoal() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodGoalUseCase(repository)
        val focus = FocusPeriod(Period.Week, date)

        save(focus, review = "Solid week", goal = "  Ship the refactor  ")

        val goal = repository.observePeriodGoals().first().single()
        assertEquals("Ship the refactor", goal.goal)
    }

    @Test
    fun savesWithoutGoalWhenBlank() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodGoalUseCase(repository)
        val focus = FocusPeriod(Period.Month, date)

        save(focus, review = "Good month", goal = "   ")

        val goal = repository.observePeriodGoals().first().single()
        assertNull(goal.goal)
    }

    @Test
    fun savingTwiceUpsertsTheSamePeriod() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodGoalUseCase(repository)
        val focus = FocusPeriod(Period.Day, date)

        save(focus, review = "First pass")
        save(focus, review = "Second pass", goal = "Keep going")

        val goals = repository.observePeriodGoals().first()
        assertEquals(1, goals.size)
        assertEquals("Second pass", goals.single().review)
        assertEquals("Keep going", goals.single().goal)
    }

    @Test
    fun savesMetricsAttachedToTheGoal() = runTest {
        val repository = FakeCheckItRepository()
        val save = SavePeriodGoalUseCase(repository)
        val focus = FocusPeriod(Period.Day, date)

        save(
            focus,
            review = "Done",
            metrics = listOf(
                PeriodMetric(
                    goalId = 0L,
                    name = "Distance",
                    value = "20",
                    unit = MetricUnit.Custom,
                    customUnit = "km"
                )
            )
        )

        val goal = repository.observePeriodGoals().first().single()
        assertEquals(1, goal.metrics.size)
        val metric = goal.metrics.single()
        assertEquals("Distance", metric.name)
        assertEquals("20", metric.value)
        assertEquals(MetricUnit.Custom, metric.unit)
        assertEquals("km", metric.customUnit)
        assertNotEquals(0L, metric.goalId)

        // Re-saving without the metric removes it.
        save(focus, review = "Done again")
        assertEquals(0, repository.observePeriodGoals().first().single().metrics.size)
    }
}

package com.checkit.domain.usecase

import com.checkit.domain.FocusPeriod
import com.checkit.domain.Period
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
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

        save(focus, review = "Solid week", ratings = 4.5f)

        val goal = repository.observePeriodGoals().first().single()
        assertEquals(4.5f, goal.ratings)
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
}

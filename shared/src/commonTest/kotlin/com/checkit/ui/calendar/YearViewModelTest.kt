package com.checkit.ui.calendar

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.Period
import com.checkit.domain.PeriodGoal
import com.checkit.domain.usecase.ObserveDailyReflectStatsUseCase
import com.checkit.domain.usecase.ObservePeriodGoalsUseCase
import com.checkit.ui.tasks.FakeCheckItRepository
import com.checkit.ui.today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class YearViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: YearViewModel

    private val year = today().year
    private val march5 = LocalDate(year, Month.MARCH, 5)
    private val march6 = LocalDate(year, Month.MARCH, 6)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository()
        repository.setDayGoals(
            listOf(
                dayGoal(march5, rating = 4.5f),
                dayGoal(march6, rating = 0f),
                weekGoal(march5, rating = 5f)
            )
        )
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = march5,
                    items = listOf(
                        doneItem(id = "1", date = march5, startMinutes = 9 * 60, endMinutes = 10 * 60)
                    )
                ),
                DailyPlan(
                    date = march6,
                    items = listOf(
                        plannedItem(id = "2", date = march6)
                    )
                )
            )
        )
        viewModel = YearViewModel(
            observePeriodGoals = ObservePeriodGoalsUseCase(repository),
            observeDailyReflectStats = ObserveDailyReflectStatsUseCase(repository)
        )
        dispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsRatingsAndDoneMinutesForYear() {
        val state = viewModel.uiState.value

        assertEquals(year, state.year)
        assertFalse(state.isLoading)
        assertEquals(4.5f, state.ratingFor(march5))
        assertNull(state.ratingFor(march6))
        assertEquals(60, state.doneMinutesFor(march5))
        assertEquals(0, state.doneMinutesFor(march6))
    }

    @Test
    fun monthsCoverAllTwelveMonthsOfYear() {
        val months = viewModel.uiState.value.months

        assertEquals(12, months.size)
        assertEquals(LocalDate(year, Month.JANUARY, 1), months.first())
        assertEquals(LocalDate(year, Month.DECEMBER, 1), months.last())
    }

    @Test
    fun setYearReloadsYearRange() {
        val nextYear = year + 1
        val june10 = LocalDate(nextYear, Month.JUNE, 10)
        repository.setDayGoals(
            listOf(
                dayGoal(march5, rating = 4.5f),
                dayGoal(june10, rating = 3f)
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.setYear(nextYear)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(nextYear, state.year)
        assertEquals(3f, state.ratingFor(june10))
        assertNull(state.ratingFor(march5))
    }

    private fun dayGoal(date: LocalDate, rating: Float) = PeriodGoal(
        period = Period.Day,
        startEpochDays = date.toEpochDays().toInt(),
        endEpochDays = date.toEpochDays().toInt() + 1,
        rating = rating
    )

    private fun weekGoal(date: LocalDate, rating: Float) = PeriodGoal(
        period = Period.Week,
        startEpochDays = date.toEpochDays().toInt(),
        endEpochDays = date.toEpochDays().toInt() + 7,
        rating = rating
    )

    private fun doneItem(
        id: String,
        date: LocalDate,
        startMinutes: Int,
        endMinutes: Int
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = date.toEpochDays().toInt(),
        title = "Item $id",
        source = DailyPlanItemSource.MyDayTask,
        status = DailyPlanItemStatus.Done,
        sortOrder = id.toInt(),
        startTimeMinutes = startMinutes,
        endTimeMinutes = endMinutes,
        addedAtMillis = 0L
    )

    private fun plannedItem(id: String, date: LocalDate) = DailyPlanItem(
        id = id,
        dateEpochDays = date.toEpochDays().toInt(),
        title = "Item $id",
        source = DailyPlanItemSource.MyDayTask,
        status = DailyPlanItemStatus.Planned,
        sortOrder = id.toInt(),
        addedAtMillis = 0L
    )
}

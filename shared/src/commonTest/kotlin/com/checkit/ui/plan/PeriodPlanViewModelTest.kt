package com.checkit.ui.plan

import com.checkit.domain.PeriodPlan
import com.checkit.domain.PlanFocus
import com.checkit.domain.PlanPeriod
import com.checkit.domain.PlanPriority
import com.checkit.domain.usecase.AddPlanPriorityUseCase
import com.checkit.domain.usecase.DeletePlanPriorityUseCase
import com.checkit.domain.usecase.LinkTaskToPlanPriorityUseCase
import com.checkit.domain.usecase.ObservePlanWorkspaceUseCase
import com.checkit.domain.usecase.TogglePlanPriorityDoneUseCase
import com.checkit.domain.usecase.UpdatePlanPriorityUseCase
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PeriodPlanViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: PeriodPlanViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository()
        viewModel = createViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(repository: FakeCheckItRepository): PeriodPlanViewModel =
        PeriodPlanViewModel(
            observePlanWorkspace = ObservePlanWorkspaceUseCase(repository),
            addPlanPriority = AddPlanPriorityUseCase(repository),
            updatePlanPriority = UpdatePlanPriorityUseCase(repository),
            deletePlanPriority = DeletePlanPriorityUseCase(repository),
            togglePlanPriorityDone = TogglePlanPriorityDoneUseCase(repository),
            linkTaskToPlanPriority = LinkTaskToPlanPriorityUseCase(repository)
        )

    @Test
    fun zoomIntoChildNavigatesToChildPeriod() = runTest(dispatcher) {
        val weekFocus = PlanFocus(PlanPeriod.Week, LocalDate(2026, 7, 6))
        val dayDate = weekFocus.start.plus(1, DateTimeUnit.DAY)
        val weekPlan = PeriodPlan(
            id = 1L,
            period = PlanPeriod.Week,
            startEpochDays = weekFocus.startEpochDays,
            endEpochDays = weekFocus.endInclusiveEpochDays
        )
        val dayPlan = PeriodPlan(
            id = 2L,
            period = PlanPeriod.Day,
            startEpochDays = dayDate.toEpochDays().toInt(),
            endEpochDays = dayDate.toEpochDays().toInt()
        )
        val root = priority(id = 1L, plan = weekPlan, parentId = null, title = "Week root")
        val child = priority(id = 2L, plan = dayPlan, parentId = 1L, title = "Day child")
        repository.setPeriodPlans(listOf(weekPlan, dayPlan))
        repository.setPlanPriorities(listOf(root, child))

        viewModel.selectFocus(weekFocus)
        advanceUntilIdle()

        assertEquals(
            child.id,
            viewModel.uiState.value.rootNodes.single().children.single().priority.id
        )

        viewModel.zoomIntoPriority(child)
        advanceUntilIdle()

        assertEquals(PlanFocus(PlanPeriod.Day, dayDate), viewModel.uiState.value.focus)
    }

    @Test
    fun zoomIntoDayChildFromDayViewJumpsToThatDay() = runTest(dispatcher) {
        val weekStart = LocalDate(2026, 7, 6)
        val dayDate = weekStart.plus(2, DateTimeUnit.DAY)
        val weekPlan = PeriodPlan(
            id = 1L,
            period = PlanPeriod.Week,
            startEpochDays = PlanFocus(PlanPeriod.Week, weekStart).startEpochDays,
            endEpochDays = PlanFocus(PlanPeriod.Week, weekStart).endInclusiveEpochDays
        )
        val dayPlan = PeriodPlan(
            id = 2L,
            period = PlanPeriod.Day,
            startEpochDays = dayDate.toEpochDays().toInt(),
            endEpochDays = dayDate.toEpochDays().toInt()
        )
        val root = priority(id = 1L, plan = weekPlan, parentId = null, title = "Week root")
        val child = priority(id = 2L, plan = dayPlan, parentId = 1L, title = "Day child")
        repository.setPeriodPlans(listOf(weekPlan, dayPlan))
        repository.setPlanPriorities(listOf(root, child))

        viewModel.selectFocus(PlanFocus(PlanPeriod.Day, weekStart))
        advanceUntilIdle()

        viewModel.zoomIntoPriority(child)
        advanceUntilIdle()

        assertEquals(PlanFocus(PlanPeriod.Day, dayDate), viewModel.uiState.value.focus)
    }

    private fun priority(id: Long, plan: PeriodPlan, parentId: Long?, title: String) = PlanPriority(
        id = id,
        periodPlan = plan,
        parentId = parentId,
        title = title,
        sortOrder = 0,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )
}
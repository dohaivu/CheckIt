package com.checkit.ui.myday

import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.usecase.AddManualDoneToDailyPlanUseCase
import com.checkit.domain.usecase.AddTaskToDailyPlanUseCase
import com.checkit.domain.usecase.DeleteDailyPlanItemUseCase
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemUseCase
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MyDayViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCheckItRepository
    private lateinit var viewModel: MyDayViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCheckItRepository()
        viewModel = MyDayViewModel(
            observeTaskBoard = ObserveTaskBoardUseCase(repository),
            observeDailyPlans = ObserveDailyPlansUseCase(repository),
            ensureDefaultTaskData = EnsureDefaultTaskDataUseCase(repository),
            addTaskToDailyPlan = AddTaskToDailyPlanUseCase(repository),
            addManualDoneToDailyPlan = AddManualDoneToDailyPlanUseCase(repository),
            updateDailyPlanItemTime = UpdateDailyPlanItemTimeUseCase(repository),
            updateDailyPlanItem = UpdateDailyPlanItemUseCase(repository),
            deleteDailyPlanItemUseCase = DeleteDailyPlanItemUseCase(repository)
        )
        dispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addCheckInPersistsSelectedStatus() = runTest(dispatcher) {
        viewModel.openCheckIn(startTimeMinutes = 9 * 60, endTimeMinutes = 10 * 60)
        viewModel.updateDoneTitle("Draft proposal")
        viewModel.updateStatus(isDone = false)

        viewModel.addCheckIn()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(DailyPlanItemStatus.Planned, repository.addedManualDailyPlanItems.single().status)
    }
}

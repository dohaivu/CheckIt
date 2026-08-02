package com.checkit.domain.usecase

import com.checkit.data.UserSettings
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.Objective
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskType
import com.checkit.ui.tasks.FakeCheckItRepository
import com.checkit.ui.tasks.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class AutoAddTodayTasksToMyDayUseCaseTest {
    @Test
    fun skipsWhenAlreadyRunToday() = runTest {
        val today = today()
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(tasks = listOf(task(id = 1L, doDate = today)))
        )
        val settingsRepository = FakeSettingsRepository(
            UserSettings(autoMyDayLastRunEpochDay = today.toEpochDays().toInt())
        )
        val useCase = AutoAddTodayTasksToMyDayUseCase(repository, settingsRepository)

        val addedCount = useCase()

        assertEquals(0, addedCount)
        assertEquals(emptyList(), repository.addedDailyPlanTasks)
    }

    @Test
    fun addsOpenTodayTasksAndUpdatesLastRunDate() = runTest {
        val today = today()
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                tasks = listOf(
                    task(id = 1L, doDate = today),
                    task(id = 2L, doDate = today)
                )
            )
        )
        val settingsRepository = FakeSettingsRepository(
            UserSettings(autoMyDayLastRunEpochDay = yesterday.toEpochDays().toInt())
        )
        val useCase = AutoAddTodayTasksToMyDayUseCase(repository, settingsRepository)

        val addedCount = useCase()

        assertEquals(2, addedCount)
        assertEquals(listOf(1L, 2L), repository.addedDailyPlanTasks.map { it.second.id })
        assertEquals(
            today.toEpochDays().toInt(),
            settingsRepository.settings.first().autoMyDayLastRunEpochDay
        )
    }

    @Test
    fun ignoresCompletedTrashedAndNonTodayTasks() = runTest {
        val today = today()
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                tasks = listOf(
                    task(id = 1L, doDate = today),
                    task(id = 2L, doDate = today, status = TaskStatus.Completed),
                    task(id = 3L, doDate = today, trashedAtMillis = 1L),
                    task(id = 4L, doDate = yesterday),
                    task(id = 5L, doDate = null)
                )
            )
        )
        val settingsRepository = FakeSettingsRepository()
        val useCase = AutoAddTodayTasksToMyDayUseCase(repository, settingsRepository)

        val addedCount = useCase()

        assertEquals(1, addedCount)
        assertEquals(listOf(1L), repository.addedDailyPlanTasks.map { it.second.id })
    }

    @Test
    fun addsOpenHabitsToMyDayWithoutDate() = runTest {
        val today = today()
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                tasks = listOf(
                    task(id = 1L, doDate = null, type = TaskType.Habit),
                    task(id = 2L, doDate = null, type = TaskType.Task)
                )
            )
        )
        val settingsRepository = FakeSettingsRepository()
        val useCase = AutoAddTodayTasksToMyDayUseCase(repository, settingsRepository)

        val addedCount = useCase()

        assertEquals(1, addedCount)
        assertEquals(listOf(1L), repository.addedDailyPlanTasks.map { it.second.id })
    }

    @Test
    fun skipsHabitsAlreadyCompleted() = runTest {
        val today = today()
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                tasks = listOf(
                    task(id = 1L, doDate = null, type = TaskType.Habit),
                    task(id = 2L, doDate = null, type = TaskType.Habit, completedDate = yesterday)
                )
            )
        )
        val settingsRepository = FakeSettingsRepository()
        val useCase = AutoAddTodayTasksToMyDayUseCase(repository, settingsRepository)

        val addedCount = useCase()

        assertEquals(1, addedCount)
        assertEquals(listOf(1L), repository.addedDailyPlanTasks.map { it.second.id })
    }

    @Test
    fun doesNotDuplicateTaskAlreadyPlannedToday() = runTest {
        val today = today()
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(tasks = listOf(task(id = 1L, doDate = today)))
        )
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = today,
                    items = listOf(
                        DailyPlanItem(
                            id = 10L,
                            dateEpochDays = today.toEpochDays().toInt(),
                            taskId = 1L,
                            title = "Task 1",
                            source = DailyPlanItemSource.ExistingTask,
                            status = DailyPlanItemStatus.Planned,
                            sortOrder = 0,
                            addedAtMillis = 0L
                        )
                    )
                )
            )
        )
        val settingsRepository = FakeSettingsRepository()
        val useCase = AutoAddTodayTasksToMyDayUseCase(repository, settingsRepository)

        val addedCount = useCase()

        assertEquals(0, addedCount)
        assertEquals(emptyList(), repository.addedDailyPlanTasks)
    }

    private fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private fun task(
        id: Long,
        doDate: LocalDate?,
        status: TaskStatus = TaskStatus.Open,
        type: TaskType = TaskType.Task,
        trashedAtMillis: Long? = null,
        completedDate: LocalDate? = null
    ) = TaskItem(
        id = id,
        objective = Objective.None,
        name = "Task $id",
        status = status,
        type = type,
        doDate = doDate,
        completedDate = completedDate,
        sortOrder = id.toInt(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
        trashedAtMillis = trashedAtMillis
    )
}

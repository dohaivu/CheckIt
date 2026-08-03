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
import com.checkit.domain.TaskTag
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
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            settingsRepository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(
                repository = repository,
                todayDate = { today },
                nowMinutes = { 0 }
            )
        )

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
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            settingsRepository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(
                repository = repository,
                todayDate = { today },
                nowMinutes = { 0 }
            )
        )

        val addedCount = useCase()

        assertEquals(2, addedCount)
        assertEquals(listOf(1L, 2L), repository.addedDailyPlanTasks.map { it.second.id })
        assertEquals(
            today.toEpochDays().toInt(),
            settingsRepository.settings.first().autoMyDayLastRunEpochDay
        )
    }

    @Test
    fun smartSchedulesTasksAddedToMyDay() = runTest {
        val today = today()
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val workTag = TaskTag(id = 1L, name = "Work", color = "#2563EB")
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                tasks = listOf(task(id = 1L, doDate = today, tags = listOf(workTag)))
            )
        )
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = yesterday,
                    items = listOf(
                        historyItem(10L, yesterday, workTag, 540, 600)
                    )
                )
            )
        )
        val settingsRepository = FakeSettingsRepository()
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            settingsRepository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(
                repository = repository,
                todayDate = { today },
                nowMinutes = { 0 }
            )
        )

        useCase()

        assertEquals(1, repository.updatedDailyPlanItemTimes.size)
        assertEquals(Triple(10_000L, 540, 600), repository.updatedDailyPlanItemTimes.single())
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
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            settingsRepository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(repository)
        )

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
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            settingsRepository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(repository)
        )

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
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            settingsRepository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(repository)
        )

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
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            settingsRepository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(repository)
        )

        val addedCount = useCase()

        assertEquals(0, addedCount)
        assertEquals(emptyList(), repository.addedDailyPlanTasks)
    }

    @Test
    fun removesIncompleteHabitsFromYesterday() = runTest {
        val today = today()
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(tasks = listOf(task(id = 1L, doDate = today)))
        )
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = yesterday,
                    items = listOf(
                        habitItem(id = 100L, taskId = 1L),
                        habitItem(id = 101L, taskId = 1L, status = DailyPlanItemStatus.Done),
                        item(id = 102L, taskId = 1L, source = DailyPlanItemSource.MyDayTask)
                    )
                )
            )
        )
        val settingsRepository = FakeSettingsRepository()
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            settingsRepository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(repository)
        )

        useCase()

        assertEquals(listOf(100L), repository.deletedDailyPlanItemIds)
        assertEquals(
            listOf(101L, 102L),
            repository.dailyPlanForDate(yesterday)?.items?.map { it.id }
        )
    }

    private fun habitItem(
        id: Long,
        taskId: Long,
        status: DailyPlanItemStatus = DailyPlanItemStatus.Planned
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = 0,
        taskId = taskId,
        title = "Habit $id",
        source = DailyPlanItemSource.ExistingTask,
        status = status,
        isHabit = true,
        sortOrder = 0,
        addedAtMillis = 0L
    )

    private fun item(
        id: Long,
        taskId: Long,
        source: DailyPlanItemSource
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = 0,
        taskId = taskId,
        title = "Item $id",
        source = source,
        status = DailyPlanItemStatus.Planned,
        sortOrder = 0,
        addedAtMillis = 0L
    )

    private fun historyItem(
        id: Long,
        date: LocalDate,
        tag: TaskTag,
        start: Int,
        end: Int
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = date.toEpochDays().toInt(),
        title = "History $id",
        source = DailyPlanItemSource.ExistingTask,
        status = DailyPlanItemStatus.Done,
        tags = listOf(tag),
        sortOrder = 0,
        startTimeMinutes = start,
        endTimeMinutes = end,
        addedAtMillis = 0L,
        completedAtMillis = 1L
    )

    private fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private fun task(
        id: Long,
        doDate: LocalDate?,
        status: TaskStatus = TaskStatus.Open,
        type: TaskType = TaskType.Task,
        trashedAtMillis: Long? = null,
        completedDate: LocalDate? = null,
        tags: List<TaskTag> = emptyList()
    ) = TaskItem(
        id = id,
        objective = Objective.None,
        name = "Task $id",
        status = status,
        type = type,
        doDate = doDate,
        completedDate = completedDate,
        tags = tags,
        sortOrder = id.toInt(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
        trashedAtMillis = trashedAtMillis
    )
}

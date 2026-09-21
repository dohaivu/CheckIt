package com.checkit.domain.usecase

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskStatus
import com.checkit.domain.TagItem
import com.checkit.domain.TaskType
import com.checkit.ui.tasks.FakeCheckItRepository
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
    fun addsOpenTodayTasks() = runTest {
        val today = today()
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                tasks = listOf(
                    task(id = "1", doDate = today),
                    task(id = "2", doDate = today)
                )
            )
        )
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(
                repository = repository,
                todayDate = { today },
                nowMinutes = { 0 }
            )
        )

        val addedCount = useCase()

        assertEquals(2, addedCount)
        assertEquals(listOf("1", "2"), repository.addedDailyPlanTasks.map { it.second.id })
    }

    @Test
    fun smartSchedulesTasksAddedToMyDay() = runTest {
        val today = today()
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val workTag = TagItem(id = "1", name = "Work", color = "#2563EB")
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                tasks = listOf(task(id = "1", doDate = today, tags = listOf(workTag)))
            )
        )
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = yesterday,
                    items = listOf(
                        historyItem("10", yesterday, workTag, 540, 600)
                    )
                )
            )
        )
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(
                repository = repository,
                todayDate = { today },
                nowMinutes = { 0 }
            )
        )

        useCase()

        assertEquals(1, repository.updatedDailyPlanItemTimes.size)
        assertEquals(Triple("400", 540, 600), repository.updatedDailyPlanItemTimes.single())
    }

    @Test
    fun ignoresCompletedTrashedAndNonTodayTasks() = runTest {
        val today = today()
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                tasks = listOf(
                    task(id = "1", doDate = today),
                    task(id = "2", doDate = today, status = TaskStatus.Completed),
                    task(id = "3", doDate = today, trashedAtMillis = 1L),
                    task(id = "4", doDate = yesterday),
                    task(id = "5", doDate = null)
                )
            )
        )
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(repository)
        )

        val addedCount = useCase()

        assertEquals(1, addedCount)
        assertEquals(listOf("1"), repository.addedDailyPlanTasks.map { it.second.id })
    }

    @Test
    fun addsOpenHabitsToMyDayWithoutDate() = runTest {
        val today = today()
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                tasks = listOf(
                    task(id = "1", doDate = null, type = TaskType.Habit),
                    task(id = "2", doDate = null, type = TaskType.Task)
                )
            )
        )
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(repository)
        )

        val addedCount = useCase()

        assertEquals(1, addedCount)
        assertEquals(listOf("1"), repository.addedDailyPlanTasks.map { it.second.id })
    }

    @Test
    fun skipsHabitsAlreadyCompleted() = runTest {
        val today = today()
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(
                tasks = listOf(
                    task(id = "1", doDate = null, type = TaskType.Habit),
                    task(id = "2", doDate = null, type = TaskType.Habit, completedDate = yesterday)
                )
            )
        )
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(repository)
        )

        val addedCount = useCase()

        assertEquals(1, addedCount)
        assertEquals(listOf("1"), repository.addedDailyPlanTasks.map { it.second.id })
    }

    @Test
    fun doesNotDuplicateTaskAlreadyPlannedToday() = runTest {
        val today = today()
        val repository = FakeCheckItRepository(
            initialBoard = TaskBoard(tasks = listOf(task(id = "1", doDate = today)))
        )
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = today,
                    items = listOf(
                        DailyPlanItem(
                            id = "10",
                            dateEpochDays = today.toEpochDays().toInt(),
                            taskId = "1",
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
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
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
            initialBoard = TaskBoard(tasks = listOf(task(id = "1", doDate = today)))
        )
        repository.setDailyPlans(
            listOf(
                DailyPlan(
                    date = yesterday,
                    items = listOf(
                        habitItem(id = "100", taskId = "1"),
                        habitItem(id = "101", taskId = "1", status = DailyPlanItemStatus.Done),
                        item(id = "102", taskId = "1", source = DailyPlanItemSource.MyDayTask)
                    )
                )
            )
        )
        val useCase = AutoAddTodayTasksToMyDayUseCase(
            repository,
            DeleteDailyPlanItemUseCase(repository),
            SmartScheduleDailyPlanUseCase(repository)
        )

        useCase()

        assertEquals(listOf("100"), repository.deletedDailyPlanItemIds)
        assertEquals(
            listOf("101", "102"),
            repository.dailyPlanForDate(yesterday)?.items?.map { it.id }
        )
    }

    private fun habitItem(
        id: String,
        taskId: String,
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
        id: String,
        taskId: String,
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
        id: String,
        date: LocalDate,
        tag: TagItem,
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
        id: String,
        doDate: LocalDate?,
        status: TaskStatus = TaskStatus.Open,
        type: TaskType = TaskType.Task,
        trashedAtMillis: Long? = null,
        completedDate: LocalDate? = null,
        tags: List<TagItem> = emptyList()
    ) = TaskItem(
        id = id,
        list = null,
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

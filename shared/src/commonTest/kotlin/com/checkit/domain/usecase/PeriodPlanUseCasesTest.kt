package com.checkit.domain.usecase

import com.checkit.data.PlanPriorityDailyPlanItemLink
import com.checkit.data.PlanPriorityTaskLink
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.ListItem
import com.checkit.domain.PlanFocus
import com.checkit.domain.PlanPeriod
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeriodPlanUseCasesTest {
    private val date = LocalDate(2026, 7, 6)

    @Test
    fun addPriorityCreatesPlanLazilyAndTrimsTitle() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)
        val focus = PlanFocus(PlanPeriod.Week, date)

        add(focus, title = "  Ship v2  ")
        add(focus, title = "Refactor")

        val plans = repository.observePeriodPlans().first()
        assertEquals(1, plans.size)
        val plan = plans.single()
        assertEquals(focus.period, plan.period)
        assertEquals(focus.startEpochDays, plan.startEpochDays)
        assertEquals(focus.endInclusiveEpochDays, plan.endEpochDays)

        val priorities = repository.observePlanPriorities().first()
        assertEquals(listOf("Ship v2", "Refactor"), priorities.map { it.title })
        assertTrue(priorities.all { it.parentId == null })
    }

    @Test
    fun addPriorityRejectsBlankTitle() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)

        assertFailsWith<IllegalArgumentException> {
            add(PlanFocus(PlanPeriod.Month, date), title = "   ")
        }
    }

    @Test
    fun addPriorityUnderParentInSamePlan() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)
        val focus = PlanFocus(PlanPeriod.Month, date)

        val parentId = add(focus, title = "Parent")
        val childId = add(focus, title = "Child", parentId = parentId)

        val child = repository.observePlanPriorities().first().first { it.id == childId }
        assertEquals(parentId, child.parentId)
    }

    @Test
    fun addWeekPriorityUnderMonthPriority() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)
        val monthFocus = PlanFocus(PlanPeriod.Month, date)
        val weekFocus = PlanFocus(PlanPeriod.Week, date)

        val monthId = add(monthFocus, title = "Month goal")
        val weekId = add(weekFocus, title = "Week step", parentId = monthId)

        val weekPriority = repository.observePlanPriorities().first().first { it.id == weekId }
        assertEquals(monthId, weekPriority.parentId)
    }

    @Test
    fun addPriorityRejectsParentOnUnrelatedPeriod() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)

        val parentId = add(PlanFocus(PlanPeriod.Year, date), title = "Year theme")

        assertFailsWith<IllegalArgumentException> {
            add(PlanFocus(PlanPeriod.Week, date), title = "Week task", parentId = parentId)
        }
    }

    @Test
    fun updateChangesTitleAndParent() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)
        val update = UpdatePlanPriorityUseCase(repository)
        val focus = PlanFocus(PlanPeriod.Quarter, date)

        val a = add(focus, title = "A")
        val b = add(focus, title = "B")

        update(b, focus, title = "  B v2  ", parentId = a)

        val updated = repository.observePlanPriorities().first().first { it.id == b }
        assertEquals("B v2", updated.title)
        assertEquals(a, updated.parentId)
    }

    @Test
    fun updateRejectsCycle() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)
        val update = UpdatePlanPriorityUseCase(repository)
        val focus = PlanFocus(PlanPeriod.Month, date)

        val a = add(focus, title = "A")
        val b = add(focus, title = "B", parentId = a)

        assertFailsWith<IllegalArgumentException> {
            update(a, focus, title = "A", parentId = b)
        }
    }

    @Test
    fun toggleDoneSetsAndClearsCompletion() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)
        val toggle = TogglePlanPriorityDoneUseCase(repository)
        val focus = PlanFocus(PlanPeriod.Day, date)

        val id = add(focus, title = "Do it")

        toggle(id, true)
        var priority = repository.observePlanPriorities().first().first { it.id == id }
        assertTrue(priority.isDone)
        assertNotNull(priority.completedAtMillis)

        toggle(id, false)
        priority = repository.observePlanPriorities().first().first { it.id == id }
        assertFalse(priority.isDone)
        assertNull(priority.completedAtMillis)
    }

    @Test
    fun deleteRemovesJoinsClearsChildrenParentsAndKeepsTasks() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)
        val delete = DeletePlanPriorityUseCase(repository)
        val addTask = AddTaskToPlanPriorityUseCase(repository)
        val focus = PlanFocus(PlanPeriod.Week, date)

        val parent = add(focus, title = "Parent")
        val child = add(focus, title = "Child", parentId = parent)
        val taskId = addTask(parent, name = "Work item")

        delete(parent)

        val priorities = repository.observePlanPriorities().first()
        assertEquals(listOf(child), priorities.map { it.id })
        assertNull(priorities.single().parentId)

        val links = repository.observePlanPriorityTaskIds().first()
        assertTrue(links.isEmpty())

        val board = repository.observeTaskBoard().first()
        assertTrue(board.tasks.any { it.id == taskId })
    }

    @Test
    fun linkTaskAllowedOnWeekAndDay() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)
        val link = LinkTaskToPlanPriorityUseCase(repository)

        val weekId = add(PlanFocus(PlanPeriod.Week, date), title = "W")
        val dayId = add(PlanFocus(PlanPeriod.Day, date), title = "D")
        link(weekId, 1L)
        link(dayId, 2L)

        val links = repository.observePlanPriorityTaskIds().first()
        assertEquals(2, links.size)
    }

    @Test
    fun linkTaskRejectedOnCoarsePeriods() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)
        val link = LinkTaskToPlanPriorityUseCase(repository)

        val monthId = add(PlanFocus(PlanPeriod.Month, date), title = "M")
        val quarterId = add(PlanFocus(PlanPeriod.Quarter, date), title = "Q")
        val yearId = add(PlanFocus(PlanPeriod.Year, date), title = "Y")

        assertFailsWith<IllegalArgumentException> { link(monthId, 1L) }
        assertFailsWith<IllegalArgumentException> { link(quarterId, 2L) }
        assertFailsWith<IllegalArgumentException> { link(yearId, 3L) }
    }

    @Test
    fun linkDailyPlanItemAllowedOnDayOnly() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)
        val link = LinkDailyPlanItemToPlanPriorityUseCase(repository)

        val dayId = add(PlanFocus(PlanPeriod.Day, date), title = "D")
        val weekId = add(PlanFocus(PlanPeriod.Week, date), title = "W")

        link(dayId, 100L)
        assertFailsWith<IllegalArgumentException> { link(weekId, 101L) }

        val links = repository.observePlanPriorityDailyPlanItemIds().first()
        assertEquals(listOf(100L), links.map { it.dailyPlanItemId })
    }

    @Test
    fun addTaskCreatesTaskLinksAndDefaultsDoDateToPeriodStart() = runTest {
        val repository = FakeCheckItRepository()
        val add = AddPlanPriorityUseCase(repository)
        val addTask = AddTaskToPlanPriorityUseCase(repository)
        val focus = PlanFocus(PlanPeriod.Day, date)

        val id = add(focus, title = "Day focus")
        val taskId = addTask(id, name = "  Call bank  ")

        val task = repository.observeTaskBoard().first().tasks.single()
        assertEquals(taskId, task.id)
        assertEquals("Call bank", task.name)
        assertEquals(focus.start, task.doDate)

        val links = repository.observePlanPriorityTaskIds().first()
        assertEquals(listOf(taskId), links.map { it.taskId })
    }

    @Test
    fun workspaceBuildsTreeAndDedupesDailyItemsBackedByLinkedTasks() = runTest {
        val task = TaskItem(
            id = 1L,
            list = ListItem.None,
            name = "T1",
            doDate = date,
            sortOrder = 0,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )
        val repository = FakeCheckItRepository(initialBoard = TaskBoard(tasks = listOf(task)))
        val add = AddPlanPriorityUseCase(repository)
        val focus = PlanFocus(PlanPeriod.Month, date)

        val parentId = add(focus, title = "Parent")
        val childId = add(focus, title = "Child", parentId = parentId)

        val duplicateDaily = DailyPlanItem(
            id = 2L,
            dateEpochDays = focus.startEpochDays,
            taskId = task.id,
            title = "T1",
            source = DailyPlanItemSource.ExistingTask,
            status = DailyPlanItemStatus.Planned,
            sortOrder = 0,
            addedAtMillis = 0L
        )
        repository.setPlanPriorityTaskLinks(listOf(PlanPriorityTaskLink(childId, task.id, 0)))
        repository.setPlanPriorityDailyPlanItemLinks(
            listOf(PlanPriorityDailyPlanItemLink(childId, duplicateDaily.id, 0))
        )
        repository.setDailyPlans(listOf(DailyPlan(focus.start, items = listOf(duplicateDaily))))

        val workspace = ObservePlanWorkspaceUseCase(repository).invoke(focus).first()

        assertEquals(parentId, workspace.rootNodes.single().priority.id)
        val childNode = workspace.rootNodes.single().children.single()
        assertEquals(childId, childNode.priority.id)
        assertEquals(listOf(task.id), childNode.tasks.map { it.id })
        assertTrue(childNode.dailyPlanItems.isEmpty())
    }

    @Test
    fun workspaceEmptyWhenPlanDoesNotExist() {
        val workspace = ObservePlanWorkspaceUseCase(FakeCheckItRepository()).build(
            focus = PlanFocus(PlanPeriod.Quarter, date),
            plans = emptyList(),
            priorities = emptyList(),
            taskLinks = emptyList(),
            dailyLinks = emptyList(),
            tasks = emptyList(),
            dailyPlans = emptyList()
        )

        assertNull(workspace.plan)
        assertTrue(workspace.rootNodes.isEmpty())
        assertTrue(workspace.parentCandidates.isEmpty())
    }
}

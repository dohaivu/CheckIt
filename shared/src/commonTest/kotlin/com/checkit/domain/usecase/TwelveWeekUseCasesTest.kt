package com.checkit.domain.usecase

import com.checkit.data.TaskWriteInput
import com.checkit.data.TwelveWeekGoalScoreWriteInput
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskType
import com.checkit.domain.TwelveWeekCycleStatus
import com.checkit.domain.TwelveWeekGoalFinalStatus
import com.checkit.domain.TwelveWeekGoalScore
import com.checkit.domain.TwelveWeekGoalTaskLink
import com.checkit.domain.TwelveWeekWorkspace
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TwelveWeekUseCasesTest {

    private fun task(
        id: Long,
        doDate: LocalDate? = null,
        type: TaskType = TaskType.Tactic
    ) = com.checkit.domain.TaskItem(
        id = id,
        list = null,
        name = "Task $id",
        tags = emptyList(),
        priority = TaskPriority.None,
        status = TaskStatus.Open,
        type = type,
        doDate = doDate,
        sortOrder = id.toInt(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
        trashedAtMillis = null
    )

    private fun tacticInput(name: String = "Tactic") = TaskWriteInput(
        name = name,
        description = "",
        subtasks = emptyList(),
        status = TaskStatus.Open,
        priority = TaskPriority.None,
        type = TaskType.Tactic,
        doDate = LocalDate(2026, 7, 6),
        startTimeMinutes = null,
        endTimeMinutes = null,
        repeatRRule = null,
        reminders = emptyList(),
        tagIds = emptyList()
    )

    @Test
    fun startCycleCreatesActiveCycleWithTrimmedGoals() = runTest {
        val repository = FakeCheckItRepository()
        val id = StartTwelveWeekCycleUseCase(repository)(
            title = " My cycle ",
            startEpochDays = 100,
            goalTitles = listOf(" Goal 1 ", "", "Goal 3")
        )

        val cycle = repository.observeTwelveWeekCycles().first().single()
        assertEquals(id, cycle.id)
        assertEquals(TwelveWeekCycleStatus.Active, cycle.status)
        assertEquals("My cycle", cycle.title)
        assertEquals(183, cycle.endEpochDays)
        assertEquals(
            listOf("Goal 1", "Goal 3"),
            repository.observeTwelveWeekGoals().first().map { it.title }
        )
    }

    @Test
    fun startCycleRejectsTooManyOrTooFewGoals() = runTest {
        val repository = FakeCheckItRepository()
        val start = StartTwelveWeekCycleUseCase(repository)

        assertFailsWith<IllegalArgumentException> {
            start("A", 0, emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            start("A", 0, listOf("1", "2", "3", "4"))
        }
    }

    @Test
    fun startCycleFailsIfActiveCycleExists() = runTest {
        val repository = FakeCheckItRepository()
        val start = StartTwelveWeekCycleUseCase(repository)
        start("A", 0, listOf("G1"))

        assertFailsWith<IllegalArgumentException> {
            start("B", 0, listOf("G2"))
        }
    }

    @Test
    fun addGoalFailsAtMaxThreeAndWhenNotActive() = runTest {
        val repository = FakeCheckItRepository()
        val start = StartTwelveWeekCycleUseCase(repository)
        val cycleId = start("A", 0, listOf("1", "2", "3"))
        val add = AddTwelveWeekGoalUseCase(repository)

        assertFailsWith<IllegalArgumentException> { add(cycleId, "4") }

        val goals = repository.observeTwelveWeekGoals().first()
        CompleteTwelveWeekCycleUseCase(repository)(
            cycleId,
            goals.associate { it.id to TwelveWeekGoalFinalStatus.Missed },
            ""
        )
        assertFailsWith<IllegalArgumentException> { add(cycleId, "new") }
    }

    @Test
    fun upsertCheckInReplacesScoresAndValidatesInput() = runTest {
        val repository = FakeCheckItRepository()
        val cycleId = StartTwelveWeekCycleUseCase(repository)("A", 0, listOf("G1"))
        val goalId = repository.observeTwelveWeekGoals().first().single().id
        val upsert = UpsertTwelveWeekCheckInUseCase(repository)

        upsert(cycleId, 0, "Good", listOf(TwelveWeekGoalScoreWriteInput(goalId, 8)))
        upsert(cycleId, 0, "Better", listOf(TwelveWeekGoalScoreWriteInput(goalId, 9)))

        val checkIns = repository.observeTwelveWeekCheckIns().first()
        assertEquals(1, checkIns.size)
        assertEquals("Better", checkIns.single().note)
        assertEquals(
            listOf(9),
            repository.observeTwelveWeekGoalScores().first().map { it.score }
        )

        assertFailsWith<IllegalArgumentException> {
            upsert(cycleId, 12, "", listOf(TwelveWeekGoalScoreWriteInput(goalId, 5)))
        }
        assertFailsWith<IllegalArgumentException> {
            upsert(cycleId, 0, "", listOf(TwelveWeekGoalScoreWriteInput(goalId, 11)))
        }
    }

    @Test
    fun completeCycleStampsFinalStatusesAndReview() = runTest {
        val repository = FakeCheckItRepository()
        val cycleId = StartTwelveWeekCycleUseCase(repository)("A", 0, listOf("G1"))
        val goalId = repository.observeTwelveWeekGoals().first().single().id

        CompleteTwelveWeekCycleUseCase(repository)(
            cycleId,
            mapOf(goalId to TwelveWeekGoalFinalStatus.Achieved),
            "Good run"
        )

        val cycle = repository.observeTwelveWeekCycles().first().single()
        assertEquals(TwelveWeekCycleStatus.Completed, cycle.status)
        assertEquals("Good run", cycle.reviewNote)
        assertEquals(goalId, repository.observeTwelveWeekGoals().first().single().id)
        assertEquals(
            TwelveWeekGoalFinalStatus.Achieved,
            repository.observeTwelveWeekGoals().first().single().finalStatus
        )
    }

    @Test
    fun abandonCycleSetsAbandonedStatus() = runTest {
        val repository = FakeCheckItRepository()
        val cycleId = StartTwelveWeekCycleUseCase(repository)("A", 0, listOf("G1"))

        AbandonTwelveWeekCycleUseCase(repository)(cycleId)

        assertEquals(
            TwelveWeekCycleStatus.Abandoned,
            repository.observeTwelveWeekCycles().first().single().status
        )
    }

    @Test
    fun addTacticCreatesLinkedTacticAndRejectsInactiveCycle() = runTest {
        val repository = FakeCheckItRepository()
        val cycleId = StartTwelveWeekCycleUseCase(repository)("A", 0, listOf("G1"))
        val goalId = repository.observeTwelveWeekGoals().first().single().id
        val addTactic = AddTacticToGoalUseCase(repository, AddTaskUseCase(repository))

        val taskId = addTactic(goalId, tacticInput())

        assertEquals(
            listOf(TwelveWeekGoalTaskLink(goalId, taskId, 0)),
            repository.observeTwelveWeekGoalTaskLinks().first()
        )
        assertEquals(
            TaskType.Tactic,
            repository.observeTaskBoard().first().tasks.single().type
        )

        CompleteTwelveWeekCycleUseCase(repository)(
            cycleId,
            mapOf(goalId to TwelveWeekGoalFinalStatus.Missed),
            ""
        )
        assertFailsWith<IllegalArgumentException> {
            addTactic(goalId, tacticInput())
        }
    }

    @Test
    fun unlinkTacticRemovesOnlyTheLink() = runTest {
        val repository = FakeCheckItRepository()
        val cycleId = StartTwelveWeekCycleUseCase(repository)("A", 0, listOf("G1"))
        val goalId = repository.observeTwelveWeekGoals().first().single().id
        val addTactic = AddTacticToGoalUseCase(repository, AddTaskUseCase(repository))
        val taskId = addTactic(goalId, tacticInput())

        UnlinkTacticFromGoalUseCase(repository)(goalId, taskId)

        assertTrue(repository.observeTwelveWeekGoalTaskLinks().first().isEmpty())
        assertEquals(1, repository.observeTaskBoard().first().tasks.size)
    }

    @Test
    fun workspaceBuildBucketsTacticsAndScores() {
        val observe = ObserveTwelveWeekWorkspaceUseCase(FakeCheckItRepository())
        val today = LocalDate(1970, 1, 10).toEpochDays().toInt()
        val cycle = com.checkit.domain.TwelveWeekCycle(
            id = 1,
            title = "A",
            startEpochDays = 0,
            endEpochDays = 83,
            status = TwelveWeekCycleStatus.Active,
            createdAtMillis = 0L
        )
        val goal = com.checkit.domain.TwelveWeekGoal(
            id = 10,
            cycleId = 1,
            title = "Ship it",
            sortOrder = 0,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )
        val checkIn = com.checkit.domain.TwelveWeekCheckIn(
            id = 100,
            cycleId = 1,
            weekIndex = 1,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )
        val inWeek = task(id = 1, doDate = LocalDate(1970, 1, 9))
        val outWeek = task(id = 2, doDate = LocalDate(1970, 1, 20))

        val workspace = observe.build(
            cycles = listOf(cycle),
            goals = listOf(goal),
            checkIns = listOf(checkIn),
            scores = listOf(
                TwelveWeekGoalScore(id = 1, checkInId = 100, goalId = 10, score = 7),
                TwelveWeekGoalScore(id = 2, checkInId = 100, goalId = 10, score = 9)
            ),
            links = listOf(
                TwelveWeekGoalTaskLink(10, 1, 0),
                TwelveWeekGoalTaskLink(10, 2, 1)
            ),
            tasks = listOf(inWeek, outWeek),
            todayEpochDays = today
        )

        assertEquals(1, workspace.currentWeekIndex)
        val card = workspace.goals.single()
        assertEquals(listOf(1L, 2L), card.tactics.map { it.id })
        assertEquals(8.0, card.averageScore)
        assertEquals(9, card.latestScore?.score)

        val cycleCard = workspace.cycleCards.single()
        assertEquals(1, cycleCard.currentWeekIndex)
        assertEquals(listOf(1L, 2L), cycleCard.goals.single().tactics.map { it.id })
    }

    @Test
    fun workspaceBuildBuildsAllCycleCardsActiveFirst() {
        val observe = ObserveTwelveWeekWorkspaceUseCase(FakeCheckItRepository())
        val active = com.checkit.domain.TwelveWeekCycle(
            id = 1,
            title = "Active",
            startEpochDays = 10,
            endEpochDays = 93,
            status = TwelveWeekCycleStatus.Active,
            createdAtMillis = 0L
        )
        val completed = com.checkit.domain.TwelveWeekCycle(
            id = 2,
            title = "Old",
            startEpochDays = 0,
            endEpochDays = 83,
            status = TwelveWeekCycleStatus.Completed,
            createdAtMillis = 0L,
            completedAtMillis = 5L
        )
        val goal = com.checkit.domain.TwelveWeekGoal(
            id = 10,
            cycleId = 1,
            title = "G",
            sortOrder = 0,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )

        val workspace = observe.build(
            cycles = listOf(completed, active),
            goals = listOf(goal),
            checkIns = emptyList(),
            scores = emptyList(),
            links = emptyList(),
            tasks = emptyList(),
            todayEpochDays = 10
        )

        assertEquals(listOf(1L, 2L), workspace.cycleCards.map { it.cycle.id })
        assertEquals(0, workspace.cycleCards[0].currentWeekIndex)
        assertNull(workspace.cycleCards[1].currentWeekIndex)
        assertEquals(1, workspace.cycleCards[0].goals.size)
        assertTrue(workspace.cycleCards[1].goals.isEmpty())
    }

    @Test
    fun workspaceBuildIsEmptyWithoutActiveCycle() {
        val observe = ObserveTwelveWeekWorkspaceUseCase(FakeCheckItRepository())
        val workspace = observe.build(
            cycles = emptyList(),
            goals = emptyList(),
            checkIns = emptyList(),
            scores = emptyList(),
            links = emptyList(),
            tasks = emptyList(),
            todayEpochDays = 10
        )
        assertNull(workspace.cycle)
        assertTrue(workspace.goals.isEmpty())
        assertTrue(workspace.cycleCards.isEmpty())
    }
}
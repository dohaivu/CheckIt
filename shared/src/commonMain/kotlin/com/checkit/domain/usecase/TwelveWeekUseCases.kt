package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.TaskWriteInput
import com.checkit.data.TwelveWeekCheckInWriteInput
import com.checkit.data.TwelveWeekCycleWriteInput
import com.checkit.data.TwelveWeekGoalScoreWriteInput
import com.checkit.data.TwelveWeekGoalWriteInput
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskType
import com.checkit.domain.TwelveWeekCheckIn
import com.checkit.domain.TwelveWeekCycle
import com.checkit.domain.TwelveWeekCycleStatus
import com.checkit.domain.TwelveWeekCycleCard
import com.checkit.domain.TwelveWeekGoal
import com.checkit.domain.TwelveWeekGoalCard
import com.checkit.domain.TwelveWeekGoalFinalStatus
import com.checkit.domain.TwelveWeekGoalScore
import com.checkit.domain.TwelveWeekGoalTaskLink
import com.checkit.domain.TwelveWeekWorkspace
import com.checkit.domain.TWELVE_WEEK_LAST_INDEX
import com.checkit.domain.TWELVE_WEEK_MAX_GOALS
import com.checkit.domain.TWELVE_WEEK_MAX_SCORE
import com.checkit.domain.TWELVE_WEEK_MIN_SCORE
import com.checkit.domain.executionScore
import com.checkit.domain.mondayOfWeek
import com.checkit.domain.twelveWeekEndEpochDays
import com.checkit.domain.weekIndexFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * Observes everything needed to render the 12-week hub: cycles, goals,
 * check-ins, scores, goal-task links and the task board. Exposes a pure
 * [build] for easy unit testing.
 */
class ObserveTwelveWeekWorkspaceUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(todayEpochDays: Int): Flow<TwelveWeekWorkspace> = combine(
        repository.observeTwelveWeekCycles(),
        repository.observeTwelveWeekGoals(),
        repository.observeTwelveWeekCheckIns(),
        repository.observeTwelveWeekGoalScores(),
        repository.observeTwelveWeekGoalTaskLinks(),
        repository.observeTaskBoard()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        build(
            cycles = values[0] as List<TwelveWeekCycle>,
            goals = values[1] as List<TwelveWeekGoal>,
            checkIns = values[2] as List<TwelveWeekCheckIn>,
            scores = values[3] as List<TwelveWeekGoalScore>,
            links = values[4] as List<TwelveWeekGoalTaskLink>,
            tasks = (values[5] as TaskBoard).tasks,
            todayEpochDays = todayEpochDays
        )
    }

    fun build(
        cycles: List<TwelveWeekCycle>,
        goals: List<TwelveWeekGoal>,
        checkIns: List<TwelveWeekCheckIn>,
        scores: List<TwelveWeekGoalScore>,
        links: List<TwelveWeekGoalTaskLink>,
        tasks: List<TaskItem>,
        todayEpochDays: Int
    ): TwelveWeekWorkspace {
        val activeCycles = cycles
            .filter { it.status == TwelveWeekCycleStatus.Active }
            .sortedBy { it.startEpochDays }
        val primaryCycle = activeCycles.firstOrNull()
        val pastCycles = cycles
            .filter { it.status != TwelveWeekCycleStatus.Active }
            .sortedByDescending { it.startEpochDays }

        val tasksById = tasks.associateBy { it.id }
        val taskIdsByGoal = links
            .groupBy({ it.goalId }, { it.taskId })

        fun buildGoalCard(goal: TwelveWeekGoal): TwelveWeekGoalCard {
            val tacticIds = taskIdsByGoal[goal.id].orEmpty()
            val tactics = tacticIds
                .mapNotNull { tasksById[it] }
                .filterNot { it.isTrashed }
            val goalScores = scores.filter { it.goalId == goal.id }
            return TwelveWeekGoalCard(
                goal = goal,
                tactics = tactics,
                latestScore = goalScores.maxByOrNull { it.id },
                averageScore = executionScore(goalScores.map { it.score })
            )
        }

        fun buildCycleCard(cycle: TwelveWeekCycle): TwelveWeekCycleCard {
            val isActive = cycle.status == TwelveWeekCycleStatus.Active
            return TwelveWeekCycleCard(
                cycle = cycle,
                goals = goals
                    .filter { it.cycleId == cycle.id }
                    .sortedBy { it.sortOrder }
                    .map(::buildGoalCard),
                currentWeekIndex = if (isActive) {
                    weekIndexFor(cycle.startEpochDays, todayEpochDays)
                } else {
                    null
                }
            )
        }

        val cycleCards = buildList {
            activeCycles.forEach { add(buildCycleCard(it)) }
            pastCycles.forEach { add(buildCycleCard(it)) }
        }

        if (primaryCycle == null) {
            return TwelveWeekWorkspace(
                cycle = null,
                cycleCards = cycleCards,
                currentWeekIndex = null,
                checkIns = checkIns,
                scores = scores,
                pastCycles = pastCycles
            )
        }

        return TwelveWeekWorkspace(
            cycle = primaryCycle,
            cycleCards = cycleCards,
            currentWeekIndex = cycleCards.first().currentWeekIndex,
            checkIns = checkIns,
            scores = scores,
            pastCycles = pastCycles
        )
    }
}

/**
 * Starts a new Active cycle. Only one active cycle is allowed at a time. The
 * cycle always starts on a Monday, so [startEpochDays] is snapped back to the
 * start of its week.
 */
class StartTwelveWeekCycleUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(title: String, startEpochDays: Int): Long {
        require(title.isNotBlank()) { "Cycle title must not be blank" }
        require(repository.countActiveTwelveWeekCycles() == 0) {
            "There is already an active 12-week cycle"
        }
        val mondayStart = mondayOfWeek(startEpochDays)
        return repository.addTwelveWeekCycle(
            TwelveWeekCycleWriteInput(
                title = title.trim(),
                startEpochDays = mondayStart,
                endEpochDays = twelveWeekEndEpochDays(mondayStart)
            )
        )
    }
}

/** Renames an Active cycle. */
class UpdateTwelveWeekCycleUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(cycleId: Long, title: String) {
        val cycle = requireActiveCycle(repository, cycleId)
        require(title.isNotBlank()) { "Cycle title must not be blank" }
        repository.updateTwelveWeekCycle(
            cycleId = cycleId,
            title = title.trim(),
            status = cycle.status,
            reviewNote = cycle.reviewNote,
            completedAtMillis = cycle.completedAtMillis
        )
    }
}

/** Adds a goal to an Active cycle; fails if not Active or at max goals. */
class AddTwelveWeekGoalUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(cycleId: Long, title: String, note: String = ""): Long {
        val cycle = requireActiveCycle(repository, cycleId)
        val existingCount = repository.observeTwelveWeekGoals()
            .first()
            .count { it.cycleId == cycle.id }
        require(existingCount < TWELVE_WEEK_MAX_GOALS) {
            "A cycle can have at most $TWELVE_WEEK_MAX_GOALS goals"
        }
        require(title.isNotBlank()) { "Goal title must not be blank" }
        return repository.addTwelveWeekGoal(
            TwelveWeekGoalWriteInput(cycleId = cycle.id, title = title.trim(), note = note.trim())
        )
    }
}

/** Edits title/note of a goal while its cycle is Active. */
class UpdateTwelveWeekGoalUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(goalId: Long, title: String, note: String) {
        val goal = repository.observeTwelveWeekGoals().first().firstOrNull { it.id == goalId }
            ?: return
        requireActiveCycle(repository, goal.cycleId)
        repository.updateTwelveWeekGoal(
            goalId = goalId,
            title = title.trim(),
            note = note.trim(),
            finalStatus = goal.finalStatus,
            updatedAtMillis = Clock.System.now().toEpochMilliseconds()
        )
    }
}

/** Deletes a goal (join rows die; tasks stay). Allowed while Active. */
class DeleteTwelveWeekGoalUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(goalId: Long) {
        val goal = repository.observeTwelveWeekGoals().first().firstOrNull { it.id == goalId }
            ?: return
        requireActiveCycle(repository, goal.cycleId)
        repository.deleteTwelveWeekGoal(goalId)
    }
}

/** Upserts the weekly check-in for [weekIndex], replacing any saved scores. */
class UpsertTwelveWeekCheckInUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        cycleId: Long,
        weekIndex: Int,
        note: String,
        scores: List<TwelveWeekGoalScoreWriteInput>
    ): Long {
        val cycle = requireActiveCycle(repository, cycleId)
        require(weekIndex in 0..TWELVE_WEEK_LAST_INDEX) {
            "Week index must be 0..$TWELVE_WEEK_LAST_INDEX"
        }
        val cycleGoals = repository.observeTwelveWeekGoals()
            .first()
            .filter { it.cycleId == cycle.id }
        val goalIds = cycleGoals.map { it.id }.toSet()
        scores.forEach { score ->
            require(score.goalId in goalIds) { "Score references a goal outside this cycle" }
            require(score.score in TWELVE_WEEK_MIN_SCORE..TWELVE_WEEK_MAX_SCORE) {
                "Score must be $TWELVE_WEEK_MIN_SCORE..$TWELVE_WEEK_MAX_SCORE"
            }
        }
        return repository.upsertTwelveWeekCheckIn(cycleId, weekIndex, note, scores)
    }
}

/** Completes the cycle, stamping final statuses. Never mutates tasks. */
class CompleteTwelveWeekCycleUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        cycleId: Long,
        finalStatuses: Map<Long, TwelveWeekGoalFinalStatus>,
        reviewNote: String
    ) {
        val cycle = requireActiveCycle(repository, cycleId)
        val now = Clock.System.now().toEpochMilliseconds()
        repository.observeTwelveWeekGoals()
            .first()
            .filter { it.cycleId == cycle.id }
            .forEach { goal ->
                val finalStatus = finalStatuses[goal.id]
                requireNotNull(finalStatus) { "Every goal needs a final status" }
                repository.updateTwelveWeekGoal(
                    goalId = goal.id,
                    title = goal.title,
                    note = goal.note,
                    finalStatus = finalStatus,
                    updatedAtMillis = now
                )
            }
        repository.updateTwelveWeekCycle(
            cycleId = cycleId,
            title = cycle.title,
            status = TwelveWeekCycleStatus.Completed,
            reviewNote = reviewNote,
            completedAtMillis = now
        )
    }
}

/** Abandons the cycle. Tasks untouched. */
class AbandonTwelveWeekCycleUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(cycleId: Long) {
        val cycle = requireActiveCycle(repository, cycleId)
        repository.updateTwelveWeekCycle(
            cycleId = cycleId,
            title = cycle.title,
            status = TwelveWeekCycleStatus.Abandoned,
            reviewNote = cycle.reviewNote,
            completedAtMillis = null
        )
    }
}

/** Creates a Tactic task linked to a goal of an Active cycle. */
class AddTacticToGoalUseCase(
    private val repository: CheckItRepository,
    private val addTask: AddTaskUseCase
) {
    suspend operator fun invoke(goalId: Long, input: TaskWriteInput): Long {
        val goal = repository.observeTwelveWeekGoals().first().firstOrNull { it.id == goalId }
            ?: error("Goal $goalId not found")
        requireActiveCycle(repository, goal.cycleId)
        return addTask(input.copy(type = TaskType.Tactic, twelveWeekGoalId = goalId))
    }
}

/** Removes the link only; the task itself stays. */
class UnlinkTacticFromGoalUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(goalId: Long, taskId: Long) {
        repository.unlinkTwelveWeekGoalTask(goalId, taskId)
    }
}

private suspend fun requireActiveCycle(repository: CheckItRepository, cycleId: Long): TwelveWeekCycle {
    val cycle = repository.observeTwelveWeekCycles().first().firstOrNull { it.id == cycleId }
        ?: error("Cycle $cycleId not found")
    require(cycle.status == TwelveWeekCycleStatus.Active) { "Cycle is not active" }
    return cycle
}
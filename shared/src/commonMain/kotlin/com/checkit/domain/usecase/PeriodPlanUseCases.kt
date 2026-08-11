package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.PlanPriorityDailyPlanItemLink
import com.checkit.data.PlanPriorityTaskLink
import com.checkit.data.PlanPriorityWriteInput
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.PeriodPlan
import com.checkit.domain.PlanFocus
import com.checkit.domain.PlanPeriod
import com.checkit.domain.PlanPriority
import com.checkit.domain.PlanPriorityNode
import com.checkit.domain.PlanWorkspace
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.wouldCreateCycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Observes everything needed to render the plan workspace for [focus]:
 * period plan rows, priorities, task/daily-plan-item links, tasks and daily
 * plans. Exposes a pure [build] for easy unit testing.
 */
class ObservePlanWorkspaceUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(focus: PlanFocus): Flow<PlanWorkspace> = combine(
        repository.observePeriodPlans(),
        repository.observePlanPriorities(),
        repository.observePlanPriorityTaskIds(),
        repository.observePlanPriorityDailyPlanItemIds(),
        repository.observeTaskBoard(),
        repository.observeDailyPlans()
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val plans = values[0] as List<PeriodPlan>
        val priorities = values[1] as List<PlanPriority>
        val taskLinks = values[2] as List<PlanPriorityTaskLink>
        val dailyLinks = values[3] as List<PlanPriorityDailyPlanItemLink>
        val board = values[4] as TaskBoard
        val dailyPlans = values[5] as List<DailyPlan>
        build(
            focus = focus,
            plans = plans,
            priorities = priorities,
            taskLinks = taskLinks,
            dailyLinks = dailyLinks,
            tasks = board.tasks,
            dailyPlans = dailyPlans
        )
    }

    /**
     * Assembles the tree for [focus].
     *
     * - All priorities whose `periodPlanId` matches the focus plan are "home".
     * - Roots are home priorities whose parent is null or lives elsewhere.
     * - Children include priorities from any plan that point at the node via
     *   `parentId`, so finer periods show up nested under their parent.
     * - Task/daily-plan-item work is attached from the join tables.
     */
    fun build(
        focus: PlanFocus,
        plans: List<PeriodPlan>,
        priorities: List<PlanPriority>,
        taskLinks: List<PlanPriorityTaskLink>,
        dailyLinks: List<PlanPriorityDailyPlanItemLink>,
        tasks: List<TaskItem>,
        dailyPlans: List<DailyPlan>
    ): PlanWorkspace {
        val plan = plans.firstOrNull {
            it.period == focus.period && it.startEpochDays == focus.startEpochDays
        }
        val homePriorities = if (plan == null) {
            emptyList()
        } else {
            priorities.filter { it.periodPlanId == plan.id }
        }
        val homeIds = homePriorities.map { it.id }.toSet()

        val tasksById = tasks.associateBy { it.id }
        val dailyItemsById = dailyPlans.flatMap { it.items }.associateBy { it.id }
        val taskIdsByPriority = taskLinks.groupBy({ it.priorityId }, { it.taskId })
        val dailyIdsByPriority = dailyLinks.groupBy({ it.priorityId }, { it.dailyPlanItemId })

        fun nodeFor(priority: PlanPriority): PlanPriorityNode {
            val children = priorities
                .filter { it.parentId == priority.id }
                .sortedBy { it.sortOrder }
                .map { nodeFor(it) }
            val linkedTaskIds = taskIdsByPriority[priority.id].orEmpty().toSet()
            val linkedTasks = taskIdsByPriority[priority.id].orEmpty()
                .mapNotNull { tasksById[it] }
                .filterNot { it.isTrashed }
                .sortedBy { it.sortOrder }
            val linkedDaily = dailyIdsByPriority[priority.id].orEmpty()
                .mapNotNull { dailyItemsById[it] }
                .filter { it.taskId == null || it.taskId !in linkedTaskIds }
                .sortedBy { it.sortOrder }
            return PlanPriorityNode(
                priority = priority,
                children = children,
                tasks = linkedTasks,
                dailyPlanItems = linkedDaily
            )
        }

        val roots = homePriorities
            .filter { it.parentId == null || it.parentId !in homeIds }
            .sortedBy { it.sortOrder }
            .map { nodeFor(it) }

        return PlanWorkspace(
            focus = focus,
            plan = plan,
            plans = plans,
            rootNodes = roots,
            parentCandidates = parentCandidates(focus, plans, priorities)
        )
    }
}

/** Creates (lazily) the period plan for [focus] and adds a priority to it. */
class AddPlanPriorityUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        focus: PlanFocus,
        title: String,
        parentId: Long? = null,
        note: String = ""
    ): Long {
        val trimmed = title.trim()
        require(trimmed.isNotBlank()) { "Priority title must not be blank" }
        repository.validateParent(focus, parentId)
        val plan = repository.getOrCreatePeriodPlan(focus.period, focus.start, focus.endInclusive)
        return repository.addPlanPriority(
            PlanPriorityWriteInput(
                periodPlanId = plan.id,
                parentId = parentId,
                title = trimmed,
                note = note
            )
        )
    }
}

/** Updates a priority's title and/or parent. Rejects cycles and invalid parents. */
class UpdatePlanPriorityUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        priorityId: Long,
        focus: PlanFocus,
        title: String,
        parentId: Long?,
        note: String = ""
    ) {
        val trimmed = title.trim()
        require(trimmed.isNotBlank()) { "Priority title must not be blank" }
        val priorities = repository.observePlanPriorities().first()
        val existing = priorities.firstOrNull { it.id == priorityId } ?: return
        require(!wouldCreateCycle(priorities, priorityId, parentId)) {
            "Parent would create a cycle"
        }
        repository.validateParent(focus, parentId)
        repository.updatePlanPriority(
            priorityId,
            PlanPriorityWriteInput(
                periodPlanId = existing.periodPlanId,
                parentId = parentId,
                title = trimmed,
                note = note,
                sortOrder = existing.sortOrder,
                isDone = existing.isDone
            )
        )
    }
}

/** Deletes a priority and its join rows; never touches linked tasks/items. */
class DeletePlanPriorityUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(priorityId: Long) =
        repository.deletePlanPriority(priorityId)
}

/** Manual done toggle: flips isDone and records completedAtMillis. */
class TogglePlanPriorityDoneUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(priorityId: Long, isDone: Boolean) =
        repository.setPlanPriorityDone(priorityId, isDone)
}

class ReorderPlanPrioritiesUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(periodPlanId: Long, orderedIds: List<Long>) =
        repository.reorderPlanPriorities(periodPlanId, orderedIds)
}

/** Links an existing task to a Week/Day priority only. */
class LinkTaskToPlanPriorityUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(priorityId: Long, taskId: Long) {
        val plan = repository.periodPlanForPriority(priorityId)
            ?: error("Plan priority $priorityId not found")
        require(plan.period == PlanPeriod.Week || plan.period == PlanPeriod.Day) {
            "Tasks can only be linked to Week or Day priorities"
        }
        repository.linkTaskToPriority(priorityId, taskId)
    }
}

/** Links a daily plan item to a Day priority only. */
class LinkDailyPlanItemToPlanPriorityUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(priorityId: Long, dailyPlanItemId: Long) {
        val plan = repository.periodPlanForPriority(priorityId)
            ?: error("Plan priority $priorityId not found")
        require(plan.period == PlanPeriod.Day) {
            "Daily plan items can only be linked to Day priorities"
        }
        repository.linkDailyPlanItemToPriority(priorityId, dailyPlanItemId)
    }
}

/**
 * Priorities that may be picked as a parent for a priority created/edited on
 * [focus]: priorities on the same plan, plus priorities on the parent period
 * plan that covers the focus (e.g. a Week priority can nest under a Month one).
 */
internal fun parentCandidates(
    focus: PlanFocus,
    plans: List<PeriodPlan>,
    priorities: List<PlanPriority>
): List<PlanPriority> {
    val plan = plans.firstOrNull {
        it.period == focus.period && it.startEpochDays == focus.startEpochDays
    }
    val parentPeriod = focus.parentPeriod()
    val parentPlan = parentPeriod?.let { period ->
        plans.firstOrNull { candidate ->
            candidate.period == period &&
                candidate.startEpochDays <= focus.startEpochDays &&
                candidate.endEpochDays >= focus.endInclusiveEpochDays
        }
    }
    return priorities
        .filter { priority ->
            (plan != null && priority.periodPlanId == plan.id) ||
                (parentPlan != null && priority.periodPlanId == parentPlan.id)
        }
        .sortedBy { it.sortOrder }
}

private suspend fun CheckItRepository.validateParent(focus: PlanFocus, parentId: Long?) {
    if (parentId == null) return
    val candidates = parentCandidates(
        focus,
        observePeriodPlans().first(),
        observePlanPriorities().first()
    )
    require(parentId in candidates.map { it.id }) { "Parent priority does not belong to this focus" }
}

private suspend fun CheckItRepository.periodPlanForPriority(priorityId: Long): PeriodPlan? {
    val priority = observePlanPriorities().first().firstOrNull { it.id == priorityId }
        ?: return null
    return observePeriodPlans().first().firstOrNull { it.id == priority.periodPlanId }
}

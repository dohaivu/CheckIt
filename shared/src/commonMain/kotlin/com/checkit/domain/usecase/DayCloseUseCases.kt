package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.SettingsRepository
import com.checkit.domain.CarryOverResult
import com.checkit.domain.CarryOverTimePolicy
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayCloseConfirmInput
import com.checkit.domain.DayCloseConfirmResult
import com.checkit.domain.DayCloseSummary
import com.checkit.domain.DayCloseTagMinutes
import com.checkit.domain.LeftoverAction
import com.checkit.domain.PeriodReview
import com.checkit.ui.myday.workMinutes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.time.Clock

/** Pure builder for evening review summary. */
class BuildDayCloseSummaryUseCase(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(date: LocalDate, plan: DailyPlan?): DayCloseSummary = withContext(dispatcher) {
        val items = plan?.items.orEmpty()
        val doneItems = items
            .filter { it.status == DailyPlanItemStatus.Done }
            .sortedBy { it.startTimeMinutes ?: Int.MAX_VALUE }
        val plannedItems = items
            .filter { it.status == DailyPlanItemStatus.Planned && it.handledAtMillis == null }
            .sortedBy { it.startTimeMinutes ?: Int.MAX_VALUE }
        val alreadyCarriedItems = items
            .filter { it.status == DailyPlanItemStatus.Planned && it.handledAtMillis != null }
            .sortedBy { it.startTimeMinutes ?: Int.MAX_VALUE }
        val doneMinutes = doneItems.sumOf { it.workMinutes() }
        val topTags = doneItems
            .asSequence()
            .flatMap { item ->
                val minutes = item.workMinutes()
                if (minutes <= 0) emptySequence()
                else item.tags.asSequence().map { tag -> tag to minutes }
            }
            .groupBy({ (tag, _) -> tag }, { (_, minutes) -> minutes })
            .map { (tag, minutes) ->
                DayCloseTagMinutes(
                    tagId = tag.id,
                    name = tag.name,
                    color = tag.color,
                    totalMinutes = minutes.sum()
                )
            }
            .sortedWith(
                compareByDescending<DayCloseTagMinutes> { it.totalMinutes }
                    .thenBy { it.name.lowercase() }
            )
            .take(TopTagLimit)

        DayCloseSummary(
            date = date,
            doneCount = doneItems.size,
            plannedCount = plannedItems.size,
            doneMinutes = doneMinutes,
            plannedItems = plannedItems,
            doneItems = doneItems,
            topTags = topTags,
            alreadyCarriedItems = alreadyCarriedItems
        )
    }

    private companion object {
        const val TopTagLimit = 5
    }
}

/**
 * Copies plan items onto a target date.
 * Shared by day review (PR1) and morning leftovers (PR2).
 */
class CarryOverDailyPlanItemsUseCase(
    private val repository: CheckItRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(
        items: List<DailyPlanItem>,
        itemIds: Set<Long>,
        toDate: LocalDate,
        timePolicy: CarryOverTimePolicy = CarryOverTimePolicy.ClearTimes
    ): CarryOverResult = withContext(dispatcher) {
        val selected = items.filter { it.id in itemIds }
        if (selected.isEmpty()) {
            return@withContext CarryOverResult(carriedCount = 0, skippedCount = 0, newItemIds = emptyList())
        }

        val clearTimes = timePolicy == CarryOverTimePolicy.ClearTimes
        val newIds = mutableListOf<Long>()
        var skipped = 0
        for (item in selected) {
            val newId = repository.copyDailyPlanItemToDate(
                source = item,
                targetDate = toDate,
                clearTimes = clearTimes
            )
            if (newId == null) skipped += 1 else newIds += newId
        }
        CarryOverResult(
            carriedCount = newIds.size,
            skippedCount = skipped,
            newItemIds = newIds
        )
    }

    /** Carry every item in [items] onto [toDate]. */
    suspend fun carryAll(
        items: List<DailyPlanItem>,
        toDate: LocalDate,
        timePolicy: CarryOverTimePolicy = CarryOverTimePolicy.ClearTimes
    ): CarryOverResult = invoke(
        items = items,
        itemIds = items.map { it.id }.toSet(),
        toDate = toDate,
        timePolicy = timePolicy
    )
}

/** Observes all persisted period reviews (day, week, month, year). */
class ObservePeriodReviewsUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(): Flow<List<PeriodReview>> = repository.observePeriodReviews()
}

/**
 * Applies leftover decisions and records the review atomically.
 * Marking done, carrying items, saving the win note/goal, and stamping items as
 * handled happen in a single database transaction, so repeated invocations for
 * the same day are idempotent.
 */
class CompleteDayCloseUseCase(
    private val repository: CheckItRepository,
    private val settingsRepository: SettingsRepository,
    private val buildSummary: BuildDayCloseSummaryUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(
        plan: DailyPlan?,
        input: DayCloseConfirmInput
    ): Result<DayCloseConfirmResult> = runCatching {
        withContext(dispatcher) {
            val summary = buildSummary(input.date, plan)
            val redecidableItems = summary.plannedItems + summary.alreadyCarriedItems
            val resolution = resolveLeftovers(redecidableItems, input.leftoverActions)
            val tomorrow = input.date.plus(1, DateTimeUnit.DAY)
            val commit = repository.completeDayClose(
                date = input.date,
                markDoneItemIds = resolution.markDoneIds,
                carryItemIds = resolution.carryIds,
                dropItemIds = resolution.dropIds,
                winNote = input.winNote,
                tomorrowGoal = input.tomorrowGoal,
                doneCount = summary.doneCount,
                plannedCount = summary.plannedCount,
                doneMinutes = summary.doneMinutes,
                targetDate = tomorrow,
                nowMillis = Clock.System.now().toEpochMilliseconds()
            )
            settingsRepository.setLastDayCloseEpochDay(input.date.toEpochDays().toInt())

            DayCloseConfirmResult(
                markedDoneCount = resolution.markDoneIds.size,
                carriedCount = commit.carriedCount,
                droppedCount = resolution.droppedCount,
                winNoteSaved = !input.winNote.isNullOrBlank()
            )
        }
    }

    /** Splits leftover decisions into the operations performed by the review. */
    private fun resolveLeftovers(
        plannedItems: List<DailyPlanItem>,
        actions: Map<Long, LeftoverAction>
    ): LeftoverResolution {
        val plannedById = plannedItems.associateBy { it.id }
        val markDoneIds = mutableListOf<Long>()
        val carryIds = mutableListOf<Long>()
        val dropIds = mutableListOf<Long>()
        for ((itemId, action) in actions) {
            val item = plannedById[itemId] ?: continue
            when (action) {
                LeftoverAction.None -> Unit // No decision yet; leave the item untouched.
                LeftoverAction.MarkDone -> markDoneIds += item.id
                LeftoverAction.CarryOver -> carryIds += item.id
                LeftoverAction.Drop -> dropIds += item.id
            }
        }
        return LeftoverResolution(
            markDoneIds = markDoneIds,
            carryIds = carryIds,
            dropIds = dropIds
        )
    }

    private data class LeftoverResolution(
        val markDoneIds: List<Long>,
        val carryIds: List<Long>,
        val dropIds: List<Long>
    ) {
        val droppedCount: Int get() = dropIds.size
    }
}

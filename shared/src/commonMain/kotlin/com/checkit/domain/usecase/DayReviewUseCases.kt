package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.SettingsRepository
import com.checkit.domain.CarryOverResult
import com.checkit.domain.CarryOverTimePolicy
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayReviewConfirmInput
import com.checkit.domain.DayReviewConfirmResult
import com.checkit.domain.DayReviewSummary
import com.checkit.domain.DayReviewTagMinutes
import com.checkit.domain.LeftoverAction
import com.checkit.domain.planWorkMinutes
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Pure builder for evening review summary (no IO). */
class BuildDayReviewSummaryUseCase {
    operator fun invoke(date: LocalDate, plan: DailyPlan?): DayReviewSummary {
        val items = plan?.items.orEmpty()
        val doneItems = items
            .filter { it.status == DailyPlanItemStatus.Done }
            .sortedBy { it.startTimeMinutes ?: Int.MAX_VALUE }
        val plannedItems = items
            .filter { it.status == DailyPlanItemStatus.Planned }
            .sortedBy { it.startTimeMinutes ?: Int.MAX_VALUE }
        val doneMinutes = doneItems.sumOf { it.planWorkMinutes() }
        val topTags = doneItems
            .asSequence()
            .flatMap { item ->
                val minutes = item.planWorkMinutes()
                if (minutes <= 0) emptySequence()
                else item.tags.asSequence().map { tag -> tag to minutes }
            }
            .groupBy({ (tag, _) -> tag }, { (_, minutes) -> minutes })
            .map { (tag, minutes) ->
                DayReviewTagMinutes(
                    tagId = tag.id,
                    name = tag.name,
                    color = tag.color,
                    totalMinutes = minutes.sum()
                )
            }
            .sortedWith(
                compareByDescending<DayReviewTagMinutes> { it.totalMinutes }
                    .thenBy { it.name.lowercase() }
            )
            .take(TopTagLimit)

        return DayReviewSummary(
            date = date,
            doneCount = doneItems.size,
            plannedCount = plannedItems.size,
            doneMinutes = doneMinutes,
            plannedItems = plannedItems,
            doneItems = doneItems,
            topTags = topTags
        )
    }

    private companion object {
        const val TopTagLimit = 5
    }
}

/**
 * Copies plan items onto a target date.
 * Shared by day review (PR1) and morning leftovers (PR2).
 *
 * Idempotent for task-linked items: skips when the same taskId already exists on [toDate].
 */
class CarryOverDailyPlanItemsUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        items: List<DailyPlanItem>,
        itemIds: Collection<Long>,
        toDate: LocalDate,
        timePolicy: CarryOverTimePolicy = CarryOverTimePolicy.ClearTimes
    ): CarryOverResult {
        val selected = items.filter { it.id in itemIds }
        if (selected.isEmpty()) {
            return CarryOverResult(carriedCount = 0, skippedCount = 0, newItemIds = emptyList())
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
        return CarryOverResult(
            carriedCount = newIds.size,
            skippedCount = skipped,
            newItemIds = newIds
        )
    }
}

/** Applies leftover decisions, optional win note, and marks the day as reviewed. */
class CompleteDayReviewUseCase(
    private val repository: CheckItRepository,
    private val settingsRepository: SettingsRepository,
    private val carryOverDailyPlanItems: CarryOverDailyPlanItemsUseCase,
    private val buildSummary: BuildDayReviewSummaryUseCase = BuildDayReviewSummaryUseCase()
) {
    suspend operator fun invoke(
        plan: DailyPlan?,
        input: DayReviewConfirmInput
    ): DayReviewConfirmResult {
        val summary = buildSummary(input.date, plan)
        val plannedById = summary.plannedItems.associateBy { it.id }

        var markedDone = 0
        var dropped = 0
        val carryIds = mutableListOf<Long>()

        for ((itemId, action) in input.leftoverActions) {
            val item = plannedById[itemId] ?: continue
            when (action) {
                LeftoverAction.MarkDone -> {
                    repository.updateDailyPlanItemStatus(itemId, DailyPlanItemStatus.Done)
                    markedDone += 1
                }
                LeftoverAction.CarryOver -> carryIds += item.id
                LeftoverAction.Drop -> dropped += 1
            }
        }

        val tomorrow = input.date.plus(1, DateTimeUnit.DAY)
        val carryResult = if (carryIds.isEmpty()) {
            CarryOverResult(carriedCount = 0, skippedCount = 0, newItemIds = emptyList())
        } else {
            carryOverDailyPlanItems(
                items = summary.plannedItems,
                itemIds = carryIds,
                toDate = tomorrow,
                timePolicy = CarryOverTimePolicy.ClearTimes
            )
        }

        val winNote = input.winNote?.trim().orEmpty()
        val winNoteAdded = if (winNote.isNotEmpty()) {
            repository.addDailyPlanItem(
                date = input.date,
                title = WinNoteTitle,
                note = winNote,
                startTimeMinutes = null,
                endTimeMinutes = null,
                source = DailyPlanItemSource.MyDayNote,
                status = DailyPlanItemStatus.Done,
                tagIds = emptyList()
            )
            true
        } else {
            false
        }

        settingsRepository.setLastDayReviewEpochDay(input.date.toEpochDays().toInt())

        return DayReviewConfirmResult(
            markedDoneCount = markedDone,
            carriedCount = carryResult.carriedCount,
            droppedCount = dropped,
            winNoteAdded = winNoteAdded
        )
    }

    private companion object {
        const val WinNoteTitle = "Win"
    }
}

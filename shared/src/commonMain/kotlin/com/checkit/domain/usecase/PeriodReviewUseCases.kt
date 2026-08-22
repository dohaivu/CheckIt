package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.FocusPeriod
import com.checkit.domain.Period
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import kotlin.time.Clock

/** Persists a period review (upsert) for the given focus. */
class SavePeriodReviewUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        focus: FocusPeriod,
        content: String,
        intentNext: String,
        source: ReviewSource = ReviewSource.Manual
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        repository.savePeriodReview(
            PeriodReview(
                period = focus.period,
                periodStartEpochDays = focus.start.toEpochDays().toInt(),
                periodEndEpochDays = focus.endExclusive.toEpochDays().toInt(),
                content = content.trim(),
                intentNext = intentNext.trim().takeIf { it.isNotEmpty() },
                source = source,
                status = ReviewStatus.Complete,
                completedAtMillis = if (content.isNotBlank()) now else null,
                generatedAtMillis = if (source == ReviewSource.Manual) null else now,
                editedAtMillis = now
            )
        )
    }
}

/**
 * Builds a narrative draft for a period focus from daily-plan activity. The
 * narrative is seeded with the content of the highest-level saved review covering
 * the focus (breadcrumb order: Year top, then Month, Week, Day), then appends an
 * activity summary built only from completed items inside the focus period.
 * Numeric stats are not embedded; they are read live from the rollup tables.
 */
class BuildPeriodReviewDraftUseCase {
    operator fun invoke(
        focus: FocusPeriod,
        dailyPlans: List<DailyPlan>,
        reviews: List<PeriodReview> = emptyList()
    ): String? {
        val plansInRange = dailyPlans.filter { plan ->
            plan.date >= focus.start && plan.date < focus.endExclusive
        }
        val items = plansInRange.flatMap { it.items }
        val doneItems = items.filter { it.status == DailyPlanItemStatus.Done }
        if (doneItems.isEmpty()) return null

        val doneMinutes = doneItems.sumOf { it.workMinutes() }
        val topTags = doneItems.asSequence()
            .flatMap { item ->
                val minutes = item.workMinutes()
                if (minutes <= 0) emptySequence() else item.tags.asSequence().map { tag -> tag to minutes }
            }
            .groupBy({ (tag, _) -> tag }, { (_, minutes) -> minutes })
            .map { (tag, minutes) -> tag.name to minutes.sum() }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.lowercase() })
            .take(TopTagLimit)
            .toList()

        val highlights = doneItems
            .sortedWith(
                compareBy<DailyPlanItem> { it.workMinutes() == 0 }
                    .thenByDescending { it.completedAtMillis }
                    .thenBy { it.sortOrder }
            )
            .take(HighlightLimit)

        val seed = highestLevelSeedReview(focus, reviews)
        return buildNarrative(seed, focus.period, doneItems.size, doneMinutes, topTags, highlights)
    }

    /**
     * Finds the saved review at the highest period level that covers [focus],
     * following the breadcrumb hierarchy (Annual top, then Month, Week, Day).
     */
    private fun highestLevelSeedReview(
        focus: FocusPeriod,
        reviews: List<PeriodReview>
    ): PeriodReview? {
        val candidates = reviews.filter { review -> review.covers(focus) }
        return candidates.minByOrNull { it.period.ordinal }
    }

    private fun PeriodReview.covers(focus: FocusPeriod): Boolean {
        val focusStart = focus.start.toEpochDays().toInt()
        val focusEnd = focus.endExclusive.toEpochDays().toInt()
        return periodStartEpochDays <= focusStart && periodEndEpochDays >= focusEnd
    }

    private fun buildNarrative(
        seed: PeriodReview?,
        period: Period,
        doneCount: Int,
        totalMinutes: Int,
        topTags: List<Pair<String, Int>>,
        highlights: List<DailyPlanItem>
    ): String = buildString {
        if (seed != null && seed.content.isNotBlank()) {
            append(seed.content.trim())
            append("\n\n")
        }
        append("Completed $doneCount item${if (doneCount == 1) "" else "s"} in ")
        append(formatMinutes(totalMinutes))
        append(" this ${period.periodWord()}.")
        if (topTags.isNotEmpty()) {
            append("\n\nTop focus: ")
            append(topTags.joinToString(", ") { "${it.first} (${formatMinutes(it.second)})" })
        }
        if (highlights.isNotEmpty()) {
            append("\n\nHighlights:\n")
            append(highlights.joinToString("\n") { "• ${it.title.ifBlank { "Completed item" }}" })
        }
    }

    private fun formatMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours == 0 -> "${minutes}m"
            minutes == 0 -> "${hours}h"
            else -> "${hours}h ${minutes}m"
        }
    }

    private fun Period.periodWord(): String = when (this) {
        Period.Day -> "day"
        Period.Week -> "week"
        Period.Month -> "month"
        Period.Quarter -> "quarter"
        Period.Year -> "year"
    }

    private companion object {
        const val TopTagLimit = 3
        const val HighlightLimit = 5
    }
}

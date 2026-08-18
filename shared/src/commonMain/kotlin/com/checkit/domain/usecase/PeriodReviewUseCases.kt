package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.PeriodFocus
import com.checkit.domain.Period
import com.checkit.domain.PeriodReview
import com.checkit.domain.PeriodReviewDraft
import com.checkit.domain.PeriodReviewDraftHighlight
import com.checkit.domain.PeriodReviewDraftStats
import com.checkit.domain.PeriodReviewDraftTag
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/** Persists a period review (upsert) for the given focus. */
class SavePeriodReviewUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        focus: PeriodFocus,
        content: String,
        intentNext: String,
        source: ReviewSource = ReviewSource.Manual,
        statsJson: String? = null,
        highlightsJson: String? = null
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
                editedAtMillis = now,
                statsJson = statsJson,
                highlightsJson = highlightsJson
            )
        )
    }
}

/** Builds a narrative + structured draft for a period focus from daily-plan activity. */
class BuildPeriodReviewDraftUseCase {
    /**
     * Builds a draft for [focus]. The narrative is seeded with the content of the
     * highest-level saved review covering the focus (breadcrumb order: Year top,
     * then Month, Week, Day), then appends an activity summary built only from
     * completed items inside the focus period (planned items are ignored).
     */
    operator fun invoke(
        focus: PeriodFocus,
        dailyPlans: List<DailyPlan>,
        reviews: List<PeriodReview> = emptyList()
    ): PeriodReviewDraft? {
        val plansInRange = dailyPlans.filter { plan ->
            plan.date >= focus.start && plan.date < focus.endExclusive
        }
        val items = plansInRange.flatMap { it.items }
        val doneItems = items.filter { it.status == DailyPlanItemStatus.Done }
        if (doneItems.isEmpty()) return null

        val doneMinutes = doneItems.sumOf { it.workMinutes() }
        val plannedCount = items.count { it.status == DailyPlanItemStatus.Planned }
        val topTags = doneItems.asSequence()
            .flatMap { item ->
                val minutes = item.workMinutes()
                if (minutes <= 0) emptySequence() else item.tags.asSequence().map { tag -> tag to minutes }
            }
            .groupBy({ (tag, _) -> tag }, { (_, minutes) -> minutes })
            .map { (tag, minutes) ->
                PeriodReviewDraftTag(
                    name = tag.name,
                    color = tag.color,
                    totalMinutes = minutes.sum()
                )
            }
            .filter { it.totalMinutes > 0 }
            .sortedWith(compareByDescending<PeriodReviewDraftTag> { it.totalMinutes }.thenBy { it.name.lowercase() })
            .take(TopTagLimit)
            .toList()

        val highlights = doneItems
            .sortedWith(
                compareBy<com.checkit.domain.DailyPlanItem> { it.workMinutes() == 0 }
                    .thenByDescending { it.completedAtMillis }
                    .thenBy { it.sortOrder }
            )
            .take(HighlightLimit)
            .map { item ->
                PeriodReviewDraftHighlight(
                    dateEpochDays = item.dateEpochDays,
                    title = item.title.ifBlank { "Completed item" },
                    minutes = item.workMinutes()
                )
            }

        val stats = PeriodReviewDraftStats(
            doneCount = doneItems.size,
            plannedCount = plannedCount,
            totalMinutes = doneMinutes,
            topTags = topTags
        )
        val seed = highestLevelSeedReview(focus, reviews)
        return PeriodReviewDraft(
            content = buildNarrative(seed, focus.period, stats, highlights),
            statsJson = draftJson.encodeToString(stats),
            highlightsJson = draftJson.encodeToString(highlights)
        )
    }

    /**
     * Finds the saved review at the highest period level that covers [focus],
     * following the breadcrumb hierarchy (Annual top, then Month, Week, Day).
     * The saved review must span the focus's date range; a narrower period
     * (e.g. Day) is never chosen over a broader one (e.g. Annual).
     */
    private fun highestLevelSeedReview(
        focus: PeriodFocus,
        reviews: List<PeriodReview>
    ): PeriodReview? {
        val candidates = reviews.filter { review ->
            review.covers(focus)
        }
        return candidates.minByOrNull { it.period.ordinal }
    }

    private fun PeriodReview.covers(focus: PeriodFocus): Boolean {
        val focusStart = focus.start.toEpochDays().toInt()
        val focusEnd = focus.endExclusive.toEpochDays().toInt()
        return periodStartEpochDays <= focusStart && periodEndEpochDays >= focusEnd
    }

    private fun buildNarrative(
        seed: PeriodReview?,
        period: Period,
        stats: PeriodReviewDraftStats,
        highlights: List<PeriodReviewDraftHighlight>
    ): String = buildString {
        if (seed != null && seed.content.isNotBlank()) {
            append(seed.content.trim())
            append("\n\n")
        }
        append("Completed ${stats.doneCount} item${if (stats.doneCount == 1) "" else "s"} in ")
        append(formatMinutes(stats.totalMinutes))
        append(" this ${period.periodWord()}.")
        if (stats.topTags.isNotEmpty()) {
            append("\n\nTop focus: ")
            append(stats.topTags.joinToString(", ") { "${it.name} (${formatMinutes(it.totalMinutes)})" })
        }
        if (highlights.isNotEmpty()) {
            append("\n\nHighlights:\n")
            append(highlights.joinToString("\n") { "• ${it.title}" })
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

private val draftJson = Json { ignoreUnknownKeys = true }

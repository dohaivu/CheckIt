package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.PeriodFocus
import com.checkit.domain.PeriodReview
import com.checkit.domain.PeriodReviewDraft
import com.checkit.domain.PeriodReviewDraftHighlight
import com.checkit.domain.PeriodReviewDraftStats
import com.checkit.domain.PeriodReviewDraftTag
import com.checkit.domain.ReviewPeriod
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import com.checkit.ui.myday.workMinutes
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
    suspend operator fun invoke(focus: PeriodFocus, dailyPlans: List<DailyPlan>): PeriodReviewDraft? {
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
        return PeriodReviewDraft(
            content = buildNarrative(focus.period, stats, highlights),
            statsJson = draftJson.encodeToString(stats),
            highlightsJson = draftJson.encodeToString(highlights)
        )
    }

    private fun buildNarrative(
        period: ReviewPeriod,
        stats: PeriodReviewDraftStats,
        highlights: List<PeriodReviewDraftHighlight>
    ): String = buildString {
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

    private fun ReviewPeriod.periodWord(): String = when (this) {
        ReviewPeriod.Day -> "day"
        ReviewPeriod.Week -> "week"
        ReviewPeriod.Month -> "month"
        ReviewPeriod.Year -> "year"
    }

    private companion object {
        const val TopTagLimit = 3
        const val HighlightLimit = 5
    }
}

private val draftJson = Json { ignoreUnknownKeys = true }

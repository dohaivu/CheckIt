package com.checkit.ui.reflect

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.JournalEntry
import com.checkit.domain.PeriodFocus
import com.checkit.domain.PeriodReview
import com.checkit.domain.ReviewPeriod
import com.checkit.domain.ReviewSource
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.myday.doneWorkMinutes
import com.checkit.ui.reports.DigestReportSummary
import com.checkit.ui.reports.buildDigestReport
import com.checkit.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class ReflectReviewEditorState(
    val focus: PeriodFocus,
    val review: PeriodReview?,
    val content: String = "",
    val intentNext: String = "",
    val source: ReviewSource = ReviewSource.Manual,
    val statsJson: String? = null,
    val highlightsJson: String? = null,
    val isDraft: Boolean = false,
    val isSaving: Boolean = false
)

/**
 * State for the Reflect tab: the unified review hub with day → week → month →
 * year zoom. The zoom level is a [ReportPeriod] (Daily/Week/Month/Annual);
 * [focus] maps it onto the canonical [ReviewPeriod] domain model.
 */
data class ReflectUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.Week,
    val selectedDate: LocalDate = today(),
    val dailyPlans: List<DailyPlan> = emptyList(),
    val journalEntries: List<JournalEntry> = emptyList(),
    val reviews: List<PeriodReview> = emptyList(),
    val isLoading: Boolean = true,
    val editor: ReflectReviewEditorState? = null
) {
    val focus: PeriodFocus get() = PeriodFocus(selectedPeriod.toReviewPeriod(), selectedDate)
    val focusStartEpochDays: Int get() = focus.start.toEpochDays().toInt()
    val focusReview: PeriodReview? get() = reviews.firstOrNull {
        it.period == focus.period && it.periodStartEpochDays == focusStartEpochDays
    }

    /** All reviews, newest period first, for the Recent Reviews section. */
    val history: List<PeriodReview> get() =
        reviews.sortedWith(
            compareByDescending<PeriodReview> { it.periodStartEpochDays }
                .thenByDescending { it.period.ordinal }
                .thenByDescending { it.id }
        )

    /** Live stats derived from plans/journal inside the focused period. */
    val stats: PeriodStats get() = buildPeriodStats(focus, dailyPlans, journalEntries)

    /** Child periods to show under the review card; tapping one zooms in. */
    val children: List<PeriodFocus> get() = childrenFor(focus)

    fun hasReview(child: PeriodFocus): Boolean = reviews.any {
        it.period == child.period && it.periodStartEpochDays == child.start.toEpochDays().toInt()
    }

    /** Digest (progress/trend/tags/highlights) for the focused period. */
    val digestReport: DigestReportSummary by lazy {
        buildDigestReport(dailyPlans, selectedPeriod, selectedDate)
    }
}

/** Aggregate statistics for a focused period, derived live from plans + journals. */
data class PeriodStats(
    val doneCount: Int = 0,
    val totalMinutes: Int = 0,
    val journalCount: Int = 0
)

fun buildPeriodStats(
    focus: PeriodFocus,
    dailyPlans: List<DailyPlan>,
    journalEntries: List<JournalEntry>
): PeriodStats {
    val startEpoch = focus.start.toEpochDays().toInt()
    val endEpoch = focus.endExclusive.toEpochDays().toInt()
    val plans = dailyPlans.filter { it.date.toEpochDays().toInt() in startEpoch until endEpoch }
    return PeriodStats(
        doneCount = plans.sumOf { plan -> plan.items.count { it.status == DailyPlanItemStatus.Done } },
        totalMinutes = plans.sumOf { it.doneWorkMinutes() },
        journalCount = journalEntries.count { it.dateEpochDays in startEpoch until endEpoch }
    )
}

/** The zoom-in targets shown beneath the review card for the current focus. */
fun childrenFor(focus: PeriodFocus): List<PeriodFocus> = when (focus.period) {
    ReviewPeriod.Day -> emptyList()
    ReviewPeriod.Week -> (0..6).map { offset ->
        PeriodFocus(ReviewPeriod.Day, focus.start.plus(offset, DateTimeUnit.DAY))
    }
    ReviewPeriod.Month -> {
        val end = focus.endExclusive
        generateSequence(focus.start.minus(focus.start.dayOfWeek.ordinal, DateTimeUnit.DAY)) { anchor ->
            anchor.plus(7, DateTimeUnit.DAY)
        }
            .takeWhile { it < end }
            .map { PeriodFocus(ReviewPeriod.Week, it) }
            .toList()
    }
    ReviewPeriod.Year -> (0..11).map { monthIndex ->
        PeriodFocus(ReviewPeriod.Month, LocalDate(focus.start.year, monthIndex + 1, 1))
    }
}

internal fun ReportPeriod.toReviewPeriod(): ReviewPeriod = when (this) {
    ReportPeriod.Daily -> ReviewPeriod.Day
    ReportPeriod.Week -> ReviewPeriod.Week
    ReportPeriod.Month -> ReviewPeriod.Month
    ReportPeriod.Annual -> ReviewPeriod.Year
    ReportPeriod.Habit -> ReviewPeriod.Week
}

internal fun ReviewPeriod.toReportPeriod(): ReportPeriod = when (this) {
    ReviewPeriod.Day -> ReportPeriod.Daily
    ReviewPeriod.Week -> ReportPeriod.Week
    ReviewPeriod.Month -> ReportPeriod.Month
    ReviewPeriod.Year -> ReportPeriod.Annual
}

internal fun ReportPeriod.zoomInPeriod(): ReportPeriod = when (this) {
    ReportPeriod.Daily -> ReportPeriod.Daily
    ReportPeriod.Week -> ReportPeriod.Daily
    ReportPeriod.Month -> ReportPeriod.Week
    ReportPeriod.Annual -> ReportPeriod.Month
    ReportPeriod.Habit -> ReportPeriod.Habit
}

internal fun ReportPeriod.zoomOutPeriod(): ReportPeriod = when (this) {
    ReportPeriod.Daily -> ReportPeriod.Week
    ReportPeriod.Week -> ReportPeriod.Month
    ReportPeriod.Month -> ReportPeriod.Annual
    ReportPeriod.Annual -> ReportPeriod.Annual
    ReportPeriod.Habit -> ReportPeriod.Habit
}

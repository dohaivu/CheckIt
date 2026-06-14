package com.checkit.ui

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.ui.components.ReportPeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class ReportUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.Week,
    val selectedDate: kotlinx.datetime.LocalDate = today(),
    val dailyPlans: List<DailyPlan> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
) {
    val tagReports: List<TagReportItem> by lazy {
        dailyPlans.toTagReports(selectedPeriod.periodStart(selectedDate), selectedPeriod.periodEndExclusive(selectedDate))
    }
    val timeReports: List<TimeReportItem> by lazy {
        dailyPlans.toTimeReports(selectedPeriod, selectedDate)
    }
    val digestReport: DigestReportSummary by lazy {
        dailyPlans.toDigest(selectedPeriod, selectedDate)
    }
}

data class TagReportItem(
    val tagId: Long,
    val name: String,
    val color: String,
    val totalMinutes: Int
)

data class TimeReportItem(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalMinutes: Int
)

data class DigestReportSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalItemCount: Int,
    val totalMinutes: Int,
    val doneItemCount: Int,
    val plannedItemCount: Int,
    val activeDayCount: Int,
    val busiestDay: TimeReportItem?,
    val topTags: List<TagReportItem>,
    val highlights: List<DigestHighlight>
)

data class DigestHighlight(
    val date: LocalDate,
    val title: String,
    val note: String?,
    val totalMinutes: Int
)

private fun DailyPlanItem.workMinutes(): Int {
    val start = startTimeMinutes ?: return 0
    val end = endTimeMinutes ?: return 0
    return (end - start).coerceAtLeast(0)
}

private fun List<DailyPlan>.doneWorkMinutesForDate(date: LocalDate): Int =
    firstOrNull { it.date == date }
        ?.items
        .orEmpty()
        .filter { it.status == DailyPlanItemStatus.Done }
        .sumOf { it.workMinutes() }

private fun List<DailyPlan>.toTagReports(startDate: LocalDate, endDateExclusive: LocalDate): List<TagReportItem> =
    asSequence()
        .filter { plan -> plan.date >= startDate && plan.date < endDateExclusive }
        .flatMap { plan -> plan.items.asSequence() }
        .filter { item -> item.status == DailyPlanItemStatus.Done }
        .flatMap { item ->
            val minutes = item.workMinutes()
            if (minutes <= 0) {
                emptySequence()
            } else {
                item.tags.asSequence().map { tag -> tag to minutes }
            }
        }
        .groupBy({ (tag, _) -> tag }, { (_, minutes) -> minutes })
        .map { (tag, minutes) ->
            TagReportItem(
                tagId = tag.id,
                name = tag.name,
                color = tag.color,
                totalMinutes = minutes.sum()
            )
        }
        .sortedWith(compareByDescending<TagReportItem> { it.totalMinutes }.thenBy { it.name.lowercase() })

private fun List<DailyPlan>.toTimeReports(period: ReportPeriod, selectedDate: LocalDate): List<TimeReportItem> =
    when (period) {
        ReportPeriod.Daily -> listOf(
            TimeReportItem(
                startDate = selectedDate,
                endDate = selectedDate,
                totalMinutes = doneWorkMinutesForDate(selectedDate)
            )
        )
        ReportPeriod.Week -> {
            val start = period.periodStart(selectedDate)
            (0 until 7).map { offset ->
                val date = start.plus(offset, DateTimeUnit.DAY)
                TimeReportItem(
                    startDate = date,
                    endDate = date,
                    totalMinutes = doneWorkMinutesForDate(date)
                )
            }
        }
        ReportPeriod.Month,
        ReportPeriod.Annual -> {
            val periodStart = period.periodStart(selectedDate)
            val periodEnd = period.periodEndExclusive(selectedDate)
            generateSequence(periodStart.firstDayOfWeek()) { it.plus(7, DateTimeUnit.DAY) }
                .takeWhile { weekStart -> weekStart < periodEnd }
                .map { weekStart ->
                    val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
                    val total = dailyPlansWorkMinutesInRange(
                        startDate = maxOf(weekStart, periodStart),
                        endDateExclusive = minOf(weekEnd.plus(1, DateTimeUnit.DAY), periodEnd)
                    )
                    TimeReportItem(
                        startDate = maxOf(weekStart, periodStart),
                        endDate = minOf(weekEnd, periodEnd.minus(1, DateTimeUnit.DAY)),
                        totalMinutes = total
                    )
                }
                .toList()
        }
    }

private fun List<DailyPlan>.toDigest(period: ReportPeriod, selectedDate: LocalDate): DigestReportSummary {
    val digestPeriod = if (period == ReportPeriod.Daily) ReportPeriod.Daily else ReportPeriod.Week
    val start = digestPeriod.periodStart(selectedDate)
    val dayCount = when (digestPeriod) {
        ReportPeriod.Daily -> 1
        else -> 7
    }
    val days = (0 until dayCount).map { offset ->
        val date = start.plus(offset, DateTimeUnit.DAY)
        TimeReportItem(
            startDate = date,
            endDate = date,
            totalMinutes = doneWorkMinutesForDate(date)
        )
    }
    val endExclusive = digestPeriod.periodEndExclusive(selectedDate)
    val periodItems = asSequence()
        .filter { plan -> plan.date >= start && plan.date < endExclusive }
        .flatMap { plan -> plan.items.asSequence().map { item -> plan.date to item } }
        .toList()
    val actionItems = periodItems.map { it.second }.filter { it.isActionableDigestItem() }
    val doneItemCount = actionItems.count { it.status == DailyPlanItemStatus.Done }
    val plannedItemCount = actionItems.count { it.status == DailyPlanItemStatus.Planned }
    val highlights = periodItems.asSequence()
        .filter { (_, item) -> item.status == DailyPlanItemStatus.Done }
        .sortedWith(
            compareBy<Pair<LocalDate, DailyPlanItem>> { (_, item) -> item.workMinutes() == 0 }
                .thenByDescending { it.first }
                .thenBy { (_, item) -> item.startTimeMinutes ?: Int.MAX_VALUE }
                .thenBy { (_, item) -> item.sortOrder }
        )
        .take(8)
        .map { (date, item) ->
            DigestHighlight(
                date = date,
                title = item.title,
                note = item.note,
                totalMinutes = item.workMinutes()
            )
        }
        .toList()

    return DigestReportSummary(
        startDate = start,
        endDate = endExclusive.minus(1, DateTimeUnit.DAY),
        totalItemCount = periodItems.size,
        totalMinutes = days.sumOf { it.totalMinutes },
        doneItemCount = doneItemCount,
        plannedItemCount = plannedItemCount,
        activeDayCount = days.count { it.totalMinutes > 0 },
        busiestDay = days.maxByOrNull { it.totalMinutes }?.takeIf { it.totalMinutes > 0 },
        topTags = toTagReports(start, endExclusive).take(3),
        highlights = highlights
    )
}

private fun DailyPlanItem.isActionableDigestItem(): Boolean =
    source == DailyPlanItemSource.CheckInManualDone || source == DailyPlanItemSource.ExistingTask

private fun List<DailyPlan>.dailyPlansWorkMinutesInRange(startDate: LocalDate, endDateExclusive: LocalDate): Int =
    asSequence()
        .filter { plan -> plan.date >= startDate && plan.date < endDateExclusive }
        .flatMap { it.items.asSequence() }
        .filter { it.status == DailyPlanItemStatus.Done }
        .sumOf { it.workMinutes() }

private fun ReportPeriod.periodStart(date: LocalDate): LocalDate = when (this) {
    ReportPeriod.Daily -> date
    ReportPeriod.Week -> date.firstDayOfWeek()
    ReportPeriod.Month -> date.firstDayOfMonth()
    ReportPeriod.Annual -> LocalDate(date.year, 1, 1)
}

private fun ReportPeriod.periodEndExclusive(date: LocalDate): LocalDate = when (this) {
    ReportPeriod.Daily -> date.plus(1, DateTimeUnit.DAY)
    ReportPeriod.Week -> periodStart(date).plus(7, DateTimeUnit.DAY)
    ReportPeriod.Month -> periodStart(date).plus(1, DateTimeUnit.MONTH)
    ReportPeriod.Annual -> periodStart(date).plus(1, DateTimeUnit.YEAR)
}

private fun LocalDate.firstDayOfWeek(): LocalDate =
    minus(dayOfWeek.ordinal, DateTimeUnit.DAY)

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
    private val reportIndex: DailyPlanReportIndex by lazy {
        DailyPlanReportIndex(dailyPlans)
    }

    val tagReports: List<TagReportItem> by lazy {
        reportIndex.toTagReports(selectedPeriod.periodStart(selectedDate), selectedPeriod.periodEndExclusive(selectedDate))
    }
    val timeReports: List<TimeReportItem> by lazy {
        reportIndex.toTimeReports(selectedPeriod, selectedDate)
    }
    val digestReport: DigestReportSummary by lazy {
        reportIndex.toDigest(selectedPeriod, selectedDate)
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
    val previousTotalMinutes: Int,
    val doneItemCount: Int,
    val plannedItemCount: Int,
    val trendItems: List<TimeReportItem>,
    val weekActivityItems: List<TimeReportItem>,
    val progressItems: List<DailyPlanItem>,
    val topTags: List<TagReportItem>,
    val highlights: List<DigestHighlight>
)

data class DigestHighlight(
    val date: LocalDate,
    val item: DailyPlanItem,
    val title: String,
    val note: String?,
    val totalMinutes: Int
)

private fun DailyPlanItem.workMinutes(): Int {
    val start = startTimeMinutes ?: return 0
    val end = endTimeMinutes ?: return 0
    return (end - start).coerceAtLeast(0)
}

private fun DailyPlan?.doneWorkMinutes(): Int =
    this
        ?.items
        .orEmpty()
        .filter { it.status == DailyPlanItemStatus.Done }
        .sumOf { it.workMinutes() }

private class DailyPlanReportIndex(
    private val plans: List<DailyPlan>
) {
    private val doneWorkMinutesByDate: Map<LocalDate, Int> = plans.associate { plan ->
        plan.date to plan.doneWorkMinutes()
    }

    fun doneWorkMinutesForDate(date: LocalDate): Int =
        doneWorkMinutesByDate[date] ?: 0

    fun doneWorkMinutesInRange(startDate: LocalDate, endDateExclusive: LocalDate): Int =
        doneWorkMinutesByDate.asSequence()
            .filter { (date, _) -> date >= startDate && date < endDateExclusive }
            .sumOf { (_, minutes) -> minutes }

    fun toTagReports(startDate: LocalDate, endDateExclusive: LocalDate): List<TagReportItem> =
        plans.asSequence()
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

    fun toTimeReports(period: ReportPeriod, selectedDate: LocalDate): List<TimeReportItem> =
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
                        val total = doneWorkMinutesInRange(
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

    fun toDigest(period: ReportPeriod, selectedDate: LocalDate): DigestReportSummary {
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
        val trendItems = when (digestPeriod) {
            ReportPeriod.Daily -> (-6..0).map { offset ->
                val date = selectedDate.plus(offset, DateTimeUnit.DAY)
                TimeReportItem(
                    startDate = date,
                    endDate = date,
                    totalMinutes = doneWorkMinutesForDate(date)
                )
            }
            else -> days
        }
        val weekStart = ReportPeriod.Week.periodStart(selectedDate)
        val weekActivityItems = (0 until 7).map { offset ->
            val date = weekStart.plus(offset, DateTimeUnit.DAY)
            TimeReportItem(
                startDate = date,
                endDate = date,
                totalMinutes = doneWorkMinutesForDate(date)
            )
        }
        val endExclusive = digestPeriod.periodEndExclusive(selectedDate)
        val previousStart = start.minus(dayCount, DateTimeUnit.DAY)
        val previousTotalMinutes = doneWorkMinutesInRange(
            startDate = previousStart,
            endDateExclusive = start
        )
        val periodItems = plans.asSequence()
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
                    item = item,
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
            previousTotalMinutes = previousTotalMinutes,
            doneItemCount = doneItemCount,
            plannedItemCount = plannedItemCount,
            trendItems = trendItems,
            weekActivityItems = weekActivityItems,
            progressItems = actionItems,
            topTags = toTagReports(start, endExclusive).take(3),
            highlights = highlights
        )
    }
}

private fun DailyPlanItem.isActionableDigestItem(): Boolean =
    source == DailyPlanItemSource.MyDayTask ||
        source == DailyPlanItemSource.MyDayReminder ||
        source == DailyPlanItemSource.ExistingTask

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

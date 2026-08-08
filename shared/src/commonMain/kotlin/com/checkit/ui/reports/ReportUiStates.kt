package com.checkit.ui.reports

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.myday.doneWorkMinutes
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.myday.workMinutes
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class HabitCheckin(
    val taskId: Long,
    val title: String,
    val doneMinutesByDate: Map<LocalDate, Int>,
    val streak: Int,
    val totalDone: Int
) {
    val doneDates: Set<LocalDate> get() = doneMinutesByDate.keys
}

internal fun buildHabitCheckins(
    dailyPlans: List<DailyPlan>,
    today: LocalDate
): List<HabitCheckin> {
    val grouped = dailyPlans.asSequence()
        .flatMap { plan -> plan.items.asSequence().map { item -> plan.date to item } }
        .filter { (_, item) -> item.isHabit && item.status == DailyPlanItemStatus.Done }
        .groupBy(
            { (_, item) -> item.taskId ?: item.id },
            { (date, item) -> date to item }
        )
    return grouped.map { (key, entries) ->
        val minutesByDate = entries
            .groupBy({ it.first }, { it.second.workMinutes() })
            .mapValues { (_, minutes) -> minutes.sum() }
        HabitCheckin(
            taskId = key,
            title = entries.first().second.title.ifBlank { "Habit" },
            doneMinutesByDate = minutesByDate,
            streak = calculateStreak(minutesByDate.keys, today),
            totalDone = minutesByDate.size
        )
    }.sortedWith(compareByDescending<HabitCheckin> { it.streak }.thenBy { it.title.lowercase() })
}

internal fun calculateStreak(doneDates: Set<LocalDate>, today: LocalDate): Int {
    var day = today
    if (day !in doneDates) {
        day = day.minus(1, DateTimeUnit.DAY)
    }
    var streak = 0
    while (day in doneDates) {
        streak += 1
        day = day.minus(1, DateTimeUnit.DAY)
    }
    return streak
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
    val activityItems: List<TimeReportItem>,
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

internal fun buildDigestReport(
    dailyPlans: List<DailyPlan>,
    period: ReportPeriod,
    selectedDate: LocalDate
): DigestReportSummary =
    DailyPlanReportIndex(dailyPlans).toDigest(period, selectedDate)


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

    private fun dayItems(startDate: LocalDate, endDateExclusive: LocalDate): List<TimeReportItem> =
        (0 until startDate.daysUntil(endDateExclusive)).map { offset ->
            val date = startDate.plus(offset, DateTimeUnit.DAY)
            TimeReportItem(
                startDate = date,
                endDate = date,
                totalMinutes = doneWorkMinutesForDate(date)
            )
        }

    private fun lastDays(selectedDate: LocalDate, count: Int): List<TimeReportItem> =
        dayItems(selectedDate.minus(count - 1, DateTimeUnit.DAY), selectedDate.plus(1, DateTimeUnit.DAY))

    private fun weekBuckets(startDate: LocalDate, endDateExclusive: LocalDate): List<TimeReportItem> =
        generateSequence(startDate.firstDayOfWeek()) { it.plus(7, DateTimeUnit.DAY) }
            .takeWhile { weekStart -> weekStart < endDateExclusive }
            .map { weekStart ->
                val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
                val bucketStart = maxOf(weekStart, startDate)
                val bucketEndExclusive = minOf(weekEnd.plus(1, DateTimeUnit.DAY), endDateExclusive)
                TimeReportItem(
                    startDate = bucketStart,
                    endDate = bucketEndExclusive.minus(1, DateTimeUnit.DAY),
                    totalMinutes = doneWorkMinutesInRange(bucketStart, bucketEndExclusive)
                )
            }
            .toList()

    private fun monthBuckets(year: Int): List<TimeReportItem> =
        (0 until 12).map { monthIndex ->
            val monthStart = LocalDate(year, monthIndex + 1, 1)
            val monthEndExclusive = monthStart.plus(1, DateTimeUnit.MONTH)
            TimeReportItem(
                startDate = monthStart,
                endDate = monthEndExclusive.minus(1, DateTimeUnit.DAY),
                totalMinutes = doneWorkMinutesInRange(monthStart, monthEndExclusive)
            )
        }

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

    fun toDigest(period: ReportPeriod, selectedDate: LocalDate): DigestReportSummary {
        val start = period.periodStart(selectedDate)
        val endExclusive = period.periodEndExclusive(selectedDate)

        val trendItems = when (period) {
            ReportPeriod.Daily -> lastDays(selectedDate, 7)
            ReportPeriod.Week -> dayItems(start, endExclusive)
            ReportPeriod.Month -> weekBuckets(start, endExclusive)
            ReportPeriod.Annual -> monthBuckets(start.year)
            ReportPeriod.Habit -> emptyList()
        }
        val activityItems = when (period) {
            ReportPeriod.Daily -> {
                val weekStart = ReportPeriod.Week.periodStart(selectedDate)
                dayItems(weekStart, weekStart.plus(7, DateTimeUnit.DAY))
            }
            ReportPeriod.Week -> dayItems(start, endExclusive)
            ReportPeriod.Month -> weekBuckets(start, endExclusive)
            ReportPeriod.Annual -> monthBuckets(start.year)
            ReportPeriod.Habit -> emptyList()
        }

        val previousStart = when (period) {
            ReportPeriod.Daily -> start.minus(1, DateTimeUnit.DAY)
            ReportPeriod.Week -> start.minus(7, DateTimeUnit.DAY)
            ReportPeriod.Month -> start.minus(1, DateTimeUnit.MONTH)
            ReportPeriod.Annual -> start.minus(1, DateTimeUnit.YEAR)
            ReportPeriod.Habit -> start
        }
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
            totalMinutes = doneWorkMinutesInRange(start, endExclusive),
            previousTotalMinutes = previousTotalMinutes,
            doneItemCount = doneItemCount,
            plannedItemCount = plannedItemCount,
            trendItems = trendItems,
            activityItems = activityItems,
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
    ReportPeriod.Habit -> date
}

private fun ReportPeriod.periodEndExclusive(date: LocalDate): LocalDate = when (this) {
    ReportPeriod.Daily -> date.plus(1, DateTimeUnit.DAY)
    ReportPeriod.Week -> periodStart(date).plus(7, DateTimeUnit.DAY)
    ReportPeriod.Month -> periodStart(date).plus(1, DateTimeUnit.MONTH)
    ReportPeriod.Annual -> periodStart(date).plus(1, DateTimeUnit.YEAR)
    ReportPeriod.Habit -> periodStart(date).plus(1, DateTimeUnit.DAY)
}

private fun LocalDate.firstDayOfWeek(): LocalDate =
    minus(dayOfWeek.ordinal, DateTimeUnit.DAY)

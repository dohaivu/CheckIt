package com.checkit.ui.calendar

import androidx.compose.ui.text.AnnotatedString
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyReflectStat
import com.checkit.domain.JournalEntry
import com.checkit.domain.NoteItem
import com.checkit.domain.Period
import com.checkit.domain.PeriodGoal
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.startOf
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.components.parseMarkdownToAnnotatedString
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.isSameMonth
import com.checkit.ui.myday.doneWorkMinutes
import com.checkit.ui.today
import kotlinx.datetime.LocalDate

data class CalendarUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.Month,
    val selectedMonth: LocalDate = today().firstDayOfMonth(),
    val selectedDate: LocalDate = today(),
    val dailyPlans: List<DailyPlan> = emptyList(),
    val dailyStatsByDate: Map<LocalDate, DailyReflectStat> = emptyMap(),
    val periodGoals: List<PeriodGoal> = emptyList(),
    val journalEntries: List<JournalEntry> = emptyList(),
    val selectedDateTasks: List<TaskItem> = emptyList(),
    val selectedDateNotes: List<NoteItem> = emptyList(),
    /** Single-day plan fetched on demand when a past date is selected. */
    val selectedDayPlan: DailyPlan? = null,
    val calendarDisplayMode: CalendarDisplayMode = CalendarDisplayMode.Week,
    val selectedTagIds: Set<Long> = emptySet(),
    val isMonthlyWinsExpanded: Boolean = false
    ) {
    val monthlyWins: List<Triple<LocalDate, AnnotatedString, Float>> by lazy {
        periodGoals
            .filter { it.period == Period.Day }
            .filter { it.startDate.isSameMonth(selectedMonth) && it.review.isNotBlank() }
            .sortedByDescending { it.startDate }
            .map { Triple(it.startDate, parseMarkdownToAnnotatedString(it.review), it.rating) }
    }

    /** Goal record (review + goal) for the currently selected date. */
    val selectedDatePeriodGoal: PeriodGoal? by lazy {
        periodGoals.firstOrNull {
            it.period == Period.Day && it.startDate == selectedDate
        }
    }

    /** Goal recorded on the week containing the selected date, if any. */
    val selectedWeekGoal: PeriodGoal? by lazy {
        periodGoalFor(Period.Week)
    }

    /** Goal recorded on the month containing the selected date, if any. */
    val selectedMonthGoal: PeriodGoal? by lazy {
        periodGoalFor(Period.Month)
    }

    private fun periodGoalFor(period: Period): PeriodGoal? =
        periodGoals
            .firstOrNull {
                it.period == period &&
                    it.startEpochDays == period.startOf(selectedDate).toEpochDays().toInt()
            }

    private val filteredDailyPlans: List<DailyPlan> by lazy {
        // Live plans cover today forward; selectedDayPlan back-fills a past
        // day's agenda when an earlier date is selected.
        val allPlans = dailyPlans + listOfNotNull(selectedDayPlan)
        if (selectedTagIds.isEmpty()) {
            allPlans
        } else {
            allPlans.mapNotNull { plan ->
                val filteredItems = plan.items.filter { item -> item.hasAnyTag(selectedTagIds) }
                if (filteredItems.isEmpty()) null else plan.copy(items = filteredItems)
            }
        }
    }

    val dailyPlanByDate: Map<LocalDate, DailyPlan> = filteredDailyPlans.associateBy { it.date }

    /**
     * Markers for past days (< today) from the precomputed stats table. When
     * tags are selected, counts come from the per-day tag rollups (done items
     * only, since rollups don't track planned items per tag).
     */
    private val pastMarkersByDate: Map<LocalDate, CalendarDateMarkers> by lazy {
        val today = today()
        dailyStatsByDate
            .filterKeys { it < today }
            .mapValues { (_, stat) ->
                if (selectedTagIds.isEmpty()) {
                    CalendarDateMarkers(totalCount = stat.doneItemCount + stat.plannedItemCount)
                } else {
                    CalendarDateMarkers(
                        totalCount = stat.tagRollups
                            .filter { it.tagId in selectedTagIds }
                            .sumOf { it.doneCount }
                    )
                }
            }
    }

    /** Markers for today and future days (>= today), taken from scheduled daily plans. */
    private val todayAndFutureMarkersByDate: Map<LocalDate, CalendarDateMarkers> by lazy {
        val today = today()
        filteredDailyPlans
            .filter { it.date >= today }
            .associate { it.date to CalendarDateMarkers(totalCount = it.items.size) }
    }

    /**
     * Done minutes per day: past days (< today) come from the precomputed
     * stats table, tag-filtered via rollups when needed; today and future
     * days (>= today) come from live daily plans.
     */
    private val doneMinutesByDate: Map<LocalDate, Int> by lazy {
        val today = today()
        val pastMinutes = dailyStatsByDate
            .filterKeys { it < today }
            .mapValues { (_, stat) ->
                if (selectedTagIds.isEmpty()) {
                    stat.doneMinutes
                } else {
                    stat.tagRollups
                        .filter { it.tagId in selectedTagIds }
                        .sumOf { it.doneMinutes }
                }
            }
        val todayAndFutureMinutes = filteredDailyPlans
            .filter { it.date >= today }
            .associate { it.date to it.doneWorkMinutes() }
        pastMinutes + todayAndFutureMinutes
    }

    fun markersForDate(board: TaskBoard, date: LocalDate): CalendarDateMarkers =
        if (date < today()) {
            pastMarkersByDate[date] ?: CalendarDateMarkers.Empty
        } else {
            val planCount = todayAndFutureMarkersByDate[date]?.totalCount ?: 0
            val boardCount = board.tasksByDate[date].orEmpty().filter { matchesSelectedTags(it.tags) }.size +
                board.notesByDate[date].orEmpty().filter { matchesSelectedTags(it.tags) }.size
            CalendarDateMarkers(totalCount = planCount + boardCount)
        }

    fun doneMinutesForDate(date: LocalDate): Int =
        doneMinutesByDate[date] ?: 0

    fun dailyPlanForDate(date: LocalDate): DailyPlan? = dailyPlanByDate[date]
}

private fun DailyPlanItem.hasAnyTag(tagIds: Set<Long>): Boolean =
    tags.any { it.id in tagIds }

data class CalendarDateMarkers(
    val totalCount: Int = 0
) {
    val hasMarkers: Boolean get() = totalCount > 0

    companion object {
        val Empty = CalendarDateMarkers()
    }
}

enum class CalendarDisplayMode {
    Month,
    Week
}
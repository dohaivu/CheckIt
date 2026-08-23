package com.checkit.ui.calendar

import androidx.compose.ui.text.AnnotatedString
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyReflectStat
import com.checkit.domain.JournalEntry
import com.checkit.domain.NoteItem
import com.checkit.domain.PeriodReview
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
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
    /** Precomputed daily aggregates (past-day markers/minutes when unfiltered). */
    val dailyStatsByDate: Map<LocalDate, DailyReflectStat> = emptyMap(),
    val dayReviews: List<PeriodReview> = emptyList(),
    val journalEntries: List<JournalEntry> = emptyList(),
    val selectedDateTasks: List<TaskItem> = emptyList(),
    val selectedDateNotes: List<NoteItem> = emptyList(),
    val calendarDisplayMode: CalendarDisplayMode = CalendarDisplayMode.Week,
    val selectedTagIds: Set<Long> = emptySet(),
    val isMonthlyWinsExpanded: Boolean = false
) {
    val monthlyWins: List<Pair<LocalDate, AnnotatedString>> by lazy {
        dayReviews
            .filter { it.periodStartDate.isSameMonth(selectedMonth) && it.content.isNotBlank() }
            .sortedByDescending { it.periodStartDate }
            .map { it.periodStartDate to parseMarkdownToAnnotatedString(it.content) }
    }

    /** Win-of-the-day note for the currently selected date, if one was recorded. */
    val selectedDateReview: String? by lazy {
        dayReviews
            .firstOrNull { it.periodStartDate == selectedDate }
            ?.content
            ?.takeIf { it.isNotBlank() }
    }

    private val filteredDailyPlans: List<DailyPlan> by lazy {
        if (selectedTagIds.isEmpty()) {
            dailyPlans
        } else {
            dailyPlans.mapNotNull { plan ->
                val filteredItems = plan.items.filter { item -> item.hasAnyTag(selectedTagIds) }
                if (filteredItems.isEmpty()) null else plan.copy(items = filteredItems)
            }
        }
    }

    val dailyPlanByDate: Map<LocalDate, DailyPlan> = filteredDailyPlans.associateBy { it.date }

    /** Markers for past days (< today): precomputed stats when unfiltered, tag-filtered live plans otherwise. */
    private val pastMarkersByDate: Map<LocalDate, CalendarDateMarkers> by lazy {
        val today = today()
        if (selectedTagIds.isEmpty()) {
            dailyStatsByDate
                .filterKeys { it < today }
                .mapValues { (_, stat) ->
                    CalendarDateMarkers(totalCount = stat.doneItemCount + stat.plannedItemCount)
                }
        } else {
            filteredDailyPlans
                .filter { it.date < today }
                .associate { it.date to CalendarDateMarkers(totalCount = it.items.size) }
        }
    }

    /** Markers for today and future days (>= today), taken from scheduled daily plans. */
    private val todayAndFutureMarkersByDate: Map<LocalDate, CalendarDateMarkers> by lazy {
        val today = today()
        filteredDailyPlans
            .filter { it.date >= today }
            .associate { it.date to CalendarDateMarkers(totalCount = it.items.size) }
    }

    private val dailyPlanWorkMinutesByDate: Map<LocalDate, Int> by lazy {
        val today = today()
        val minutes = mutableMapOf<LocalDate, Int>()
        if (selectedTagIds.isEmpty()) {
            dailyStatsByDate.forEach { (date, stat) ->
                if (date < today) {
                    minutes[date] = stat.doneMinutes
                }
            }
        }
        filteredDailyPlans.forEach { plan ->
            if (plan.date == today || selectedTagIds.isNotEmpty()) {
                minutes[plan.date] = plan.doneWorkMinutes()
            }
        }
        minutes
    }

    fun markersForDate(board: TaskBoard, date: LocalDate): CalendarDateMarkers =
        if (date < today()) {
            pastMarkersByDate[date] ?: CalendarDateMarkers.Empty
        } else {
            val planCount = todayAndFutureMarkersByDate[date]?.totalCount ?: 0
            val boardCount = board.tasksByDate[date].orEmpty().size +
                board.notesByDate[date].orEmpty().size
            CalendarDateMarkers(totalCount = planCount + boardCount)
        }

    fun dailyPlanWorkMinutesForDate(date: LocalDate): Int =
        dailyPlanWorkMinutesByDate[date] ?: 0

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
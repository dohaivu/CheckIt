package com.checkit.ui.calendar

import androidx.compose.ui.text.AnnotatedString
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
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
    val board: TaskBoard = TaskBoard(),
    val dailyPlans: List<DailyPlan> = emptyList(),
    val dayReviews: List<PeriodReview> = emptyList(),
    val journalEntries: List<JournalEntry> = emptyList(),
    val selectedDateTasks: List<TaskItem> = emptyList(),
    val selectedDateNotes: List<NoteItem> = emptyList(),
    val showDailyPlanSummary: Boolean = false,
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

    private val dailyPlanMarkersByDate: Map<LocalDate, CalendarDateMarkers> by lazy {
        val markers = mutableMapOf<LocalDate, CalendarDateMarkers>()
        
        // 1. Start with PeriodReview stats (fast, reliable snapshots)
        dayReviews.forEach { review ->
            review.dayStats?.let { stats ->
                if (selectedTagIds.isEmpty()) {
                    markers[review.periodStartDate] = CalendarDateMarkers(totalCount = stats.doneCount)
                } else {
                    // Hybrid: if filtered, calculate from daily plan instead
                }
            }
        }

        // 2. Override with live daily plans for accuracy (especially when filtered)
        filteredDailyPlans.forEach { plan ->
            markers[plan.date] = CalendarDateMarkers(totalCount = plan.items.size)
        }
        markers
    }

    private val dailyPlanWorkMinutesByDate: Map<LocalDate, Int> by lazy {
        val minutes = mutableMapOf<LocalDate, Int>()
        
        // 1. Use PeriodReview stats
        dayReviews.forEach { review ->
            review.dayStats?.let { stats ->
                if (selectedTagIds.isEmpty()) {
                    minutes[review.periodStartDate] = stats.doneMinutes
                } else if (selectedTagIds.size == 1) {
                    val tagId = selectedTagIds.first()
                    minutes[review.periodStartDate] = stats.workMinutesByTag[tagId] ?: 0
                }
            }
        }

        // 2. Override with live daily plans (handles complex filters)
        filteredDailyPlans.forEach { plan ->
            minutes[plan.date] = plan.doneWorkMinutes()
        }
        minutes
    }

    private val futureMarkersByDate: Map<LocalDate, CalendarDateMarkers> by lazy {
        val dates = board.tasksByDate.keys + board.notesByDate.keys
        dates.associateWith { date ->
            CalendarDateMarkers(
                totalCount = board.tasksByDate[date].orEmpty().size + board.notesByDate[date].orEmpty().size
            )
        }
    }

    fun tasksForDate(date: LocalDate): List<TaskItem> =
        if (date == selectedDate) selectedDateTasks else board.tasksByDate[date].orEmpty()

    fun notesForDate(date: LocalDate): List<NoteItem> =
        if (date == selectedDate) selectedDateNotes else board.notesByDate[date].orEmpty()

    fun markersForDate(date: LocalDate): CalendarDateMarkers =
        if (date <= today()) {
            dailyPlanMarkersByDate[date] ?: CalendarDateMarkers.Empty
        } else {
            futureMarkersByDate[date] ?: CalendarDateMarkers.Empty
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
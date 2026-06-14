package com.checkit.ui

import androidx.compose.ui.graphics.Color
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.theme.AppIconColorDefaults
import com.checkit.ui.theme.toColor

data class CalendarUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.Month,
    val selectedMonth: kotlinx.datetime.LocalDate = today().firstDayOfMonth(),
    val selectedDate: kotlinx.datetime.LocalDate = today(),
    val board: TaskBoard = TaskBoard(),
    val dailyPlans: List<DailyPlan> = emptyList(),
    val showDailyPlanSummary: Boolean = false,
    val calendarDisplayMode: CalendarDisplayMode = CalendarDisplayMode.Month,
    val selectedTagIds: Set<Long> = emptySet()
) {
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

    val dailyPlanByDate: Map<kotlinx.datetime.LocalDate, DailyPlan> = filteredDailyPlans.associateBy { it.date }

    private val dailyPlanMarkersByDate: Map<kotlinx.datetime.LocalDate, CalendarDateMarkers> by lazy {
        filteredDailyPlans.associate { plan -> plan.date to CalendarDateMarkers(totalCount = plan.items.size) }
    }

    private val dailyPlanWorkMinutesByDate: Map<kotlinx.datetime.LocalDate, Int> by lazy {
        filteredDailyPlans.associate { plan -> plan.date to plan.items.sumOf { it.workMinutes() } }
    }

    private val futureMarkersByDate: Map<kotlinx.datetime.LocalDate, CalendarDateMarkers> by lazy {
        val dates = board.tasksByDate.keys + board.notesByDate.keys
        dates.associateWith { date ->
            CalendarDateMarkers(
                totalCount = board.tasksByDate[date].orEmpty().size + board.notesByDate[date].orEmpty().size
            )
        }
    }

    private val listColors: Map<Long, Color> = board.lists.associateWith { list ->
        list.color.toColor()
    }.mapKeys { it.key.id }

    fun tasksForDate(date: kotlinx.datetime.LocalDate): List<TaskItem> =
        board.tasksByDate[date].orEmpty()

    fun notesForDate(date: kotlinx.datetime.LocalDate): List<NoteItem> =
        board.notesByDate[date].orEmpty()

    fun markerColorsForDate(date: kotlinx.datetime.LocalDate): List<Color> {
        if (date <= today()) {
            val dailyItems = dailyPlanByDate[date]?.items.orEmpty()
            return if (dailyItems.isNotEmpty()) {
                buildList {
                    for (item in dailyItems) {
                        add(dailyItemColor(item))
                        if (size >= MarkerCap) break
                    }
                }
            } else listOf()
        }
        val tasks = tasksForDate(date)
        val notes = notesForDate(date)
        val totalSize = tasks.size + notes.size
        return buildList {
            var i = 0
            while (i < totalSize && size < MarkerCap) {
                val color = if (i < tasks.size) {
                    listColors[tasks[i].list.id]
                } else {
                    listColors[notes[i - tasks.size].list.id]
                }
                add(color ?: DefaultMarkerColor)
                i++
            }
        }
    }

    fun markersForDate(date: kotlinx.datetime.LocalDate): CalendarDateMarkers =
        if (date <= today()) {
            dailyPlanMarkersByDate[date] ?: CalendarDateMarkers.Empty
        } else {
            futureMarkersByDate[date] ?: CalendarDateMarkers.Empty
        }

    fun dailyPlanWorkMinutesForDate(date: kotlinx.datetime.LocalDate): Int =
        dailyPlanWorkMinutesByDate[date] ?: 0

    fun dailyPlanForDate(date: kotlinx.datetime.LocalDate): DailyPlan? = dailyPlanByDate[date]

    private fun dailyItemColor(item: DailyPlanItem): Color =
        item.taskId
            ?.let { taskId -> listColors[board.tasksById[taskId]?.list?.id] }
            ?: DefaultMarkerColor

    private companion object {
        const val MarkerCap: Int = 12
        val DefaultMarkerColor: Color = AppIconColorDefaults.FallbackColor
    }
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

private fun DailyPlanItem.workMinutes(): Int {
    val start = startTimeMinutes ?: return 0
    val end = endTimeMinutes ?: return 0
    return (end - start).coerceAtLeast(0)
}

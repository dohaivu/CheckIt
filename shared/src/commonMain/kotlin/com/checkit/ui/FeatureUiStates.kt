package com.checkit.ui

import androidx.compose.ui.graphics.Color
import com.checkit.domain.ActiveTagToken
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DueDatePreset
import com.checkit.domain.NoteItem
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskFilter
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskPriority
import com.checkit.domain.TaskStatus
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.theme.AppIconColorDefaults
import com.checkit.ui.theme.parseHexColorOrNull
import com.checkit.ui.theme.toColor
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class TaskUiState(
    val activeTagToken: ActiveTagToken? = null,
    val tagSuggestions: List<String> = emptyList(),
    val board: TaskBoard = TaskBoard(),
    val selectedListId: Long? = null,
    val selectedFilterId: Long? = null,
    val selectedTagId: Long? = null,
    val selectedView: TaskWorkspaceView = TaskWorkspaceView.List,
    val listDisplayType: TaskListDisplayType = TaskListDisplayType.Standard,
    val showCompleted: Boolean = false,
    val sortOption: TaskSortOption = TaskSortOption.Custom,
    val visibleTasks: List<TaskItem> = emptyList(),
    val visibleNotes: List<NoteItem> = emptyList(),
    val editor: TaskEditorState? = null,
    val isLoading: Boolean = true,
    val message: String? = null
) {
    val selectedList: TaskList? = board.lists.firstOrNull { it.id == selectedListId }
    val selectedFilter: TaskFilter? = board.filters.firstOrNull { it.id == selectedFilterId }
    val selectedTag = board.tags.firstOrNull { it.id == selectedTagId }
    val title: String = selectedList?.name ?: selectedFilter?.name ?: selectedTag?.name ?: "Tasks"
    val dayLimit: Int? = if (selectedFilter?.dueDatePreset == DueDatePreset.Today) 1 else null
    val availableViews: List<TaskWorkspaceView> = if (dayLimit == 1) {
        TaskWorkspaceView.entries
    } else {
        TaskWorkspaceView.entries.filter { it != TaskWorkspaceView.Timeline }
    }
}

enum class TaskWorkspaceView {
    List,
    Agenda,
    Timeline;

    companion object {
        fun fromCode(code: String): TaskWorkspaceView =
            entries.firstOrNull { it.name == code } ?: List
    }
}

enum class TaskListDisplayType {
    Brief,
    Standard,
    Detail;

    companion object {
        fun fromCode(code: String): TaskListDisplayType =
            entries.firstOrNull { it.name == code } ?: Standard
    }
}

enum class TaskSortOption {
    Custom,
    Priority,
    Title,
    Date;

    companion object {
        fun fromCode(code: String): TaskSortOption =
            entries.firstOrNull { it.name == code } ?: Custom
    }
}

data class MyDayUiState(
    val board: TaskBoard = TaskBoard(),
    val dailyPlans: List<DailyPlan> = emptyList(),
    val selectedView: MyDayView = MyDayView.Timeline,
    val itemEditor: DailyPlanItemEditorState? = null,
    val showSuggestions: Boolean = false,
    val suggestionStartTimeMinutes: Int? = null,
    val suggestionEndTimeMinutes: Int? = null,
    val isLoading: Boolean = true,
    val message: String? = null
) {
    val today: LocalDate = com.checkit.ui.today()
    val plan: DailyPlan? = dailyPlans.firstOrNull { it.date == today }
    val items: List<DailyPlanItem> = plan?.items.orEmpty()
    val plannedItems: List<DailyPlanItem> = items.filter { it.status != DailyPlanItemStatus.Done }
    val doneItems: List<DailyPlanItem> = items.filter { it.status == DailyPlanItemStatus.Done }
    val suggestedTasks: List<TaskItem> = board.tasks
        .filter { task ->
            !task.isTrashed &&
                task.status != TaskStatus.Completed
        }
        .sortedWith(compareBy<TaskItem> { it.doDate ?: LocalDate.fromEpochDays(Int.MAX_VALUE) }.thenBy { it.sortOrder })
}

data class ReminderSettingsUiState(
    val planEnabled: Boolean = true,
    val planTimeMinutes: Int = 7 * 60,
    val reviewEnabled: Boolean = true,
    val reviewTimeMinutes: Int = 21 * 60,
    val checkInEnabled: Boolean = true,
    val scheduleEnabled: Boolean = true,
    val checkInLastShownAtMillis: Long? = null,
)

enum class MyDayView {
    Agenda,
    Timeline,
    Board
}

data class DailyPlanItemEditorState(
    val mode: EditorMode = EditorMode.Add,
    val itemId: Long? = null,
    val taskId: Long? = null,
    val date: LocalDate = today(),
    val source: DailyPlanItemSource = DailyPlanItemSource.CheckInManualDone,
    val title: String = "",
    val note: String = "",
    val status: DailyPlanItemStatus = DailyPlanItemStatus.Done,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val selectedTagIds: Set<Long> = emptySet()
) {
    val isAddMode: Boolean get() = mode == EditorMode.Add
    val isEditMode: Boolean get() = mode == EditorMode.Edit
    val canDelete: Boolean get() = itemId != null
}

sealed interface TaskEditorState {
    data class TaskForm(
        val mode: EditorMode,
        val taskId: Long? = null,
        val listId: Long,
        val name: String = "",
        val description: String = "",
        val doDate: LocalDate? = null,
        val startTimeMinutes: Int? = null,
        val endTimeMinutes: Int? = null,
        val repeatPreset: RepeatPreset = RepeatPreset.None,
        val subtasks: List<SubTaskEditorState> = emptyList(),
        val reminderOffsets: Set<Int> = emptySet(),
        val status: TaskStatus = TaskStatus.Open,
        val priority: TaskPriority = TaskPriority.None,
        val selectedTagIds: Set<Long> = emptySet(),
        val dailyPlanItem: DailyPlanItem? = null,
        val trashedAtMillis: Long? = null
    ) : TaskEditorState {
        val durationMinutes: Int?
            get() = calculateDurationMinutes(startTimeMinutes, endTimeMinutes)
    }

    data class NoteForm(
        val mode: EditorMode,
        val noteId: Long? = null,
        val listId: Long,
        val title: String = "",
        val content: String = "",
        val status: TaskStatus = TaskStatus.Open,
        val date: LocalDate,
        val startTimeMinutes: Int? = null,
        val selectedTagIds: Set<Long> = emptySet(),
        val trashedAtMillis: Long? = null
    ) : TaskEditorState
}

enum class EditorMode {
    Add,
    View,
    Edit
}

data class SubTaskEditorState(
    val id: Long? = null,
    val name: String,
    val isCompleted: Boolean = false,
    val editorKey: Long = nextSubTaskEditorKey()
)

fun SubTaskItem.toEditorState() = SubTaskEditorState(
    id = id,
    name = name,
    isCompleted = isCompleted
)

private var subTaskEditorKeySeed = 0L

private fun nextSubTaskEditorKey(): Long = --subTaskEditorKeySeed

data class ListEditorState(
    val mode: EditorMode,
    val listId: Long? = null,
    val name: String = "",
    val color: String = AppIconColorDefaults.ListColors.first(),
    val icon: String = AppIconColorDefaults.ListIcons.first()
)

data class TagEditorState(
    val mode: EditorMode,
    val tagId: Long? = null,
    val name: String = "",
    val color: String = AppIconColorDefaults.ListColors.first()
)

enum class RepeatPreset(
    val label: String,
    val rrule: String?
) {
    None("Does not repeat", null),
    EveryDay("Everyday", "FREQ=DAILY;INTERVAL=1"),
    EveryWeek("Weekly", "FREQ=WEEKLY;INTERVAL=1"),
    EveryMonth("Monthly", "FREQ=MONTHLY;INTERVAL=1");

    companion object {
        fun fromRRule(rrule: String?): RepeatPreset =
            entries.firstOrNull { it.rrule == rrule } ?: None
    }
}

private fun calculateDurationMinutes(startTimeMinutes: Int?, endTimeMinutes: Int?): Int? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: return null
    return if (end >= start) {
        end - start
    } else {
        24 * 60 - start + end
    }
}

data class CalendarUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.Month,
    val selectedMonth: kotlinx.datetime.LocalDate = today().firstDayOfMonth(),
    val selectedDate: kotlinx.datetime.LocalDate = today(),
    val board: TaskBoard = TaskBoard(),
    val dailyPlans: List<DailyPlan> = emptyList(),
    val showDailyPlanSummary: Boolean = false,
    val calendarDisplayMode: CalendarDisplayMode = CalendarDisplayMode.Month
) {
    val dailyPlanByDate: Map<kotlinx.datetime.LocalDate, DailyPlan> = dailyPlans.associateBy { it.date }

    private val dailyPlanMarkersByDate: Map<kotlinx.datetime.LocalDate, CalendarDateMarkers> by lazy {
        dailyPlans.associate { plan -> plan.date to CalendarDateMarkers(totalCount = plan.items.size) }
    }

    private val dailyPlanWorkMinutesByDate: Map<kotlinx.datetime.LocalDate, Int> by lazy {
        dailyPlans.associate { plan -> plan.date to plan.items.sumOf { it.workMinutes() } }
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

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.English,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val colorSchemeMode: AppColorSchemeMode = AppColorSchemeMode.Sunset,
    val reminders: ReminderSettingsUiState = ReminderSettingsUiState(),
    val tagUsageSort: TagUsageSort = TagUsageSort.MostUsed,
    val message: String? = null
)

enum class TagUsageSort {
    MostUsed,
    HighestSpending,
    RecentlyUsed,
    Alphabetical
}

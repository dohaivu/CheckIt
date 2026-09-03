package com.checkit.ui.reflect

import com.checkit.domain.MetricItem
import com.checkit.domain.DailyReflectStat
import com.checkit.domain.DailyTagRollup
import com.checkit.domain.DoneItemSummary
import com.checkit.domain.FocusPeriod
import com.checkit.domain.HabitDailyRollup
import com.checkit.domain.JournalEntry
import com.checkit.domain.Period
import com.checkit.domain.PeriodGoal

import com.checkit.domain.isGoodMood
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class ReflectGoalEditorMode {
    Full,
    GoalOnly
}

data class ReflectGoalEditorState(
    val focus: FocusPeriod,
    val mode: ReflectGoalEditorMode = ReflectGoalEditorMode.Full,
    /** The persisted goal being edited, if any. */
    val existing: PeriodGoal? = null,
    val review: String = "",
    /** This period's own goal (written while reviewing the previous period). */
    val goal: String = "",
    val rating: Float = 0f,
    val metrics: List<MetricItem> = emptyList(),
    val isSaving: Boolean = false
)

/**
 * State for the Reflect tab: the unified review hub with day → week → month →
 * year zoom. The zoom level is a [ReportPeriod] (Daily/Week/Month/Annual);
 * [focus] maps it onto the canonical [Period] domain model.
 *
 * All numeric sections are served from precomputed daily rollup tables; the
 * view model subscribes only to the windows needed for the current selection.
 */
data class ReflectUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.Week,
    val selectedDate: LocalDate = today(),
    /** Per-day aggregates covering the selection window (focus + previous + trend). */
    val dailyStats: List<DailyReflectStat> = emptyList(),
    /** Done items within the focused period (slim, for highlights). */
    val doneItems: List<DoneItemSummary> = emptyList(),
    /** Journal entries within the focused period (for highlights). */
    val journalEntries: List<JournalEntry> = emptyList(),
    /** Habit rollups in a fixed trailing window ending today. */
    val habitRollups: List<HabitDailyRollup> = emptyList(),
    val goals: List<PeriodGoal> = emptyList(),
    val isLoading: Boolean = true
) {
    val focus: FocusPeriod by lazy { FocusPeriod(selectedPeriod.toPeriod(), selectedDate) }
    val focusStartEpochDays: Int get() = focus.start.toEpochDays().toInt()
    val focusGoal: PeriodGoal? by lazy {
        goals.firstOrNull {
            it.period == focus.period && it.startEpochDays == focusStartEpochDays
        }
    }

    /**
     * Goals of the child zoom level within the current window, newest first.
     * Week shows that week's Day goals, Month shows that month's Week goals,
     * Annual shows that year's Month goals; Daily behaves like Week.
     */
    val goalsForSelectedPeriod: List<PeriodGoal> by lazy {
        val rangeFocus = if (selectedPeriod == ReportPeriod.Daily) focus.zoomOut() else focus
        val startEpoch = rangeFocus.start.toEpochDays().toInt()
        val endEpoch = rangeFocus.endExclusive.toEpochDays().toInt()
        goals
            .filter {
                it.period == selectedPeriod.childPeriod() &&
                    it.startEpochDays in startEpoch until endEpoch
            }
            .filter { it.review.isNotBlank() || !it.goal.isNullOrBlank() }
            .sortedWith(
                compareByDescending<PeriodGoal> { it.startEpochDays }
                    .thenByDescending { it.id }
            )
    }

    /** Digest (progress/trend/tags/highlights) for the focused period. */
    val digestReport: DigestReportSummary by lazy {
        buildDigestReport(
            statsByDate = dailyStats.associateBy { it.dateEpochDays },
            tagRollups = dailyStats.flatMap { it.tagRollups },
            doneItems = doneItems,
            journalEntries = journalEntries,
            period = selectedPeriod,
            selectedDate = selectedDate
        )
    }

    /** Habit check-ins for the heatmap. */
    val habitCheckins: List<HabitCheckin> by lazy {
        buildHabitCheckins(habitRollups, today())
    }
}

internal fun ReportPeriod.toPeriod(): Period = when (this) {
    ReportPeriod.Daily -> Period.Day
    ReportPeriod.Week -> Period.Week
    ReportPeriod.Month -> Period.Month
    ReportPeriod.Annual -> Period.Year
    ReportPeriod.Habit -> Period.Week
}

/** The child zoom level shown in the Reviews section for the selected period. */
internal fun ReportPeriod.childPeriod(): Period = when (this) {
    ReportPeriod.Daily -> Period.Day
    ReportPeriod.Week -> Period.Day
    ReportPeriod.Month -> Period.Week
    ReportPeriod.Annual -> Period.Month
    ReportPeriod.Habit -> Period.Day
}

internal fun Period.toReportPeriod(): ReportPeriod = when (this) {
    Period.Day -> ReportPeriod.Daily
    Period.Week -> ReportPeriod.Week
    Period.Month -> ReportPeriod.Month
    Period.Year -> ReportPeriod.Annual
    else -> ReportPeriod.Daily
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

/**
 * Inclusive daily-stats window needed for a selection: covers the focused
 * period, its previous counterpart (for comparison), and any extra lookback
 * (7-day mini trend and surrounding-week chart on Daily).
 */
internal fun ReportPeriod.statsWindow(date: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
    ReportPeriod.Daily -> {
        val weekStart = date.firstDayOfWeek()
        minOf(weekStart, date.minus(6, DateTimeUnit.DAY)) to weekStart.plus(6, DateTimeUnit.DAY)
    }
    ReportPeriod.Week -> {
        val weekStart = date.firstDayOfWeek()
        weekStart.minus(7, DateTimeUnit.DAY) to weekStart.plus(6, DateTimeUnit.DAY)
    }
    ReportPeriod.Month ->
        date.firstDayOfMonth().minus(1, DateTimeUnit.MONTH) to
            date.firstDayOfMonth().plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    ReportPeriod.Annual -> LocalDate(date.year - 1, 1, 1) to LocalDate(date.year, 12, 31)
    ReportPeriod.Habit -> date to date
}

/** Inclusive focused-period window (tags/highlights/journals are scoped here). */
internal fun ReportPeriod.focusWindow(date: LocalDate): Pair<LocalDate, LocalDate> =
    periodStart(date) to periodEndExclusive(date).minus(1, DateTimeUnit.DAY)

/** Fixed trailing window for habit rollups (heatmap + streaks). */
internal val HabitWindowDays: Int = 180

internal fun habitRollupWindow(today: LocalDate): Pair<LocalDate, LocalDate> =
    today.minus(HabitWindowDays - 1, DateTimeUnit.DAY) to today

data class HabitCheckin(
    val habitKey: String,
    val title: String,
    val doneMinutesByDate: Map<LocalDate, Int>,
    val streak: Int,
    val totalDone: Int
) {
    val doneDates: Set<LocalDate> get() = doneMinutesByDate.keys
}

internal fun buildHabitCheckins(
    rollups: List<HabitDailyRollup>,
    today: LocalDate
): List<HabitCheckin> =
    rollups.groupBy { it.habitKey }
        .map { (key, rows) ->
            val minutesByDate = rows.associate { LocalDate.fromEpochDays(it.dateEpochDays) to it.doneMinutes }
            HabitCheckin(
                habitKey = key,
                title = rows.maxBy { it.title.length }.title.ifBlank { "Habit" },
                doneMinutesByDate = minutesByDate,
                streak = calculateStreak(minutesByDate.keys, today),
                totalDone = minutesByDate.size
            )
        }
        .sortedWith(compareByDescending<HabitCheckin> { it.streak }.thenBy { it.title.lowercase() })

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
    val totalMinutes: Int,
    val doneCount: Int
)

data class TimeReportItem(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalMinutes: Int
)

data class DigestReportSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalMinutes: Int,
    val doneItemCount: Int,
    val plannedItemCount: Int,
    val journalCount: Int,
    val activityItems: List<TimeReportItem>,
    val topTags: List<TagReportItem>,
    val highlights: List<DigestHighlight>
)

data class DigestHighlight(
    val date: LocalDate,
    val title: String,
    val note: String?,
    val totalMinutes: Int,
    /** Source enum name of the underlying plan item, null for journal entries. */
    val sourceName: String? = null,
    /** First mood emoji when the highlight comes from a journal entry. */
    val moodEmoji: String? = null,
    /** Whether a journal highlight carries a positive mood (ranking only). */
    val isGoodMood: Boolean = false
) {
    val isJournal: Boolean get() = sourceName == null
}

internal fun buildDigestReport(
    statsByDate: Map<Int, DailyReflectStat>,
    tagRollups: List<DailyTagRollup>,
    doneItems: List<DoneItemSummary>,
    journalEntries: List<JournalEntry>,
    period: ReportPeriod,
    selectedDate: LocalDate
): DigestReportSummary {
    val start = period.periodStart(selectedDate)
    val endExclusive = period.periodEndExclusive(selectedDate)
    val startEpoch = start.toEpochDays().toInt()
    val endEpoch = endExclusive.toEpochDays().toInt()

    fun minutesBetween(startDate: LocalDate, endDateExclusive: LocalDate): Int =
        statsByDate.asSequence()
            .filter { (epoch, _) -> epoch >= startDate.toEpochDays().toInt() && epoch < endDateExclusive.toEpochDays().toInt() }
            .sumOf { (_, stat) -> stat.doneMinutes }

    fun dayItems(startDate: LocalDate, endDateExclusive: LocalDate): List<TimeReportItem> =
        (0 until startDate.daysUntil(endDateExclusive)).map { offset ->
            val date = startDate.plus(offset, DateTimeUnit.DAY)
            TimeReportItem(
                startDate = date,
                endDate = date,
                totalMinutes = statsByDate[date.toEpochDays().toInt()]?.doneMinutes ?: 0
            )
        }

    fun weekBuckets(startDate: LocalDate, endDateExclusive: LocalDate): List<TimeReportItem> =
        generateSequence(startDate.firstDayOfWeek()) { it.plus(7, DateTimeUnit.DAY) }
            .takeWhile { weekStart -> weekStart < endDateExclusive }
            .map { weekStart ->
                val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
                val bucketStart = maxOf(weekStart, startDate)
                val bucketEndExclusive = minOf(weekEnd.plus(1, DateTimeUnit.DAY), endDateExclusive)
                TimeReportItem(
                    startDate = bucketStart,
                    endDate = bucketEndExclusive.minus(1, DateTimeUnit.DAY),
                    totalMinutes = minutesBetween(bucketStart, bucketEndExclusive)
                )
            }
            .toList()

    fun monthBuckets(year: Int): List<TimeReportItem> =
        (0 until 12).map { monthIndex ->
            val monthStart = LocalDate(year, monthIndex + 1, 1)
            val monthEndExclusive = monthStart.plus(1, DateTimeUnit.MONTH)
            TimeReportItem(
                startDate = monthStart,
                endDate = monthEndExclusive.minus(1, DateTimeUnit.DAY),
                totalMinutes = minutesBetween(monthStart, monthEndExclusive)
            )
        }

    val inPeriodBuckets = when (period) {
        ReportPeriod.Daily -> dayItems(start, endExclusive)
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
        else -> inPeriodBuckets
    }

    val periodStats = statsByDate.asSequence()
        .filter { (epoch, _) -> epoch in startEpoch until endEpoch }
        .map { (_, stat) -> stat }
        .toList()
    val journalCount = periodStats.sumOf { it.journalCount }

    val highlights = (
        doneItems.asSequence()
            .filter { it.dateEpochDays in startEpoch until endEpoch }
            .map { item ->
                DigestHighlight(
                    date = item.date,
                    title = item.title.ifBlank { "Done item" },
                    note = item.note,
                    totalMinutes = item.minutes,
                    sourceName = item.sourceName
                )
            } +
            journalEntries.asSequence()
                .filter { entry -> entry.dateEpochDays in startEpoch until endEpoch }
                .map { entry ->
                    DigestHighlight(
                        date = LocalDate.fromEpochDays(entry.dateEpochDays),
                        title = entry.content.ifBlank { entry.label.orEmpty() },
                        note = entry.label,
                        totalMinutes = 0,
                        moodEmoji = entry.moods.firstOrNull(),
                        isGoodMood = entry.isGoodMood()
                    )
                }
        )
        .sortedWith(
            compareByDescending<DigestHighlight> { it.isGoodMood }
                .thenByDescending { it.totalMinutes }
                .thenByDescending { it.date }
        )
        .take(8)
        .toList()

    return DigestReportSummary(
        startDate = start,
        endDate = endExclusive.minus(1, DateTimeUnit.DAY),
        totalMinutes = minutesBetween(start, endExclusive),
        doneItemCount = periodStats.sumOf { it.doneItemCount },
        plannedItemCount = periodStats.sumOf { it.plannedItemCount },
        journalCount = journalCount,
        activityItems = activityItems,
        topTags = buildTopTags(tagRollups, startEpoch, endEpoch),
        highlights = highlights
    )
}

private fun buildTopTags(
    tagRollups: List<DailyTagRollup>,
    startEpoch: Int,
    endEpoch: Int
): List<TagReportItem> =
    tagRollups.asSequence()
        .filter { it.dateEpochDays in startEpoch until endEpoch }
        .groupBy { it.tagId }
        .map { (tagId, rows) ->
            TagReportItem(
                tagId = tagId,
                name = rows.first().tagName,
                color = rows.first().tagColor.orEmpty(),
                totalMinutes = rows.sumOf { it.doneMinutes },
                doneCount = rows.sumOf { it.doneCount }
            )
        }
        .filter { it.totalMinutes > 0 }
        .sortedWith(compareByDescending<TagReportItem> { it.totalMinutes }.thenBy { it.name.lowercase() })
        .take(3)
        .toList()

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

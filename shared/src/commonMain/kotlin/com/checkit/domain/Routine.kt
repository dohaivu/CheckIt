package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Percent threshold for a day to count toward a routine streak. */
const val RoutineStreakThresholdPercent = 80

/** All seven days; the default schedule for a routine. */
val AllWeekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet()

/** Monday–Friday preset. */
val WeekdayPresetWeekdays: Set<DayOfWeek> = setOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY
)

/** Saturday–Sunday preset. */
val WeekdayPresetWeekends: Set<DayOfWeek> = setOf(
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
)

/** JSON default for the routines table; an empty set means paused. */
const val ActiveWeekdaysAllJson =
    "[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"]"

/**
 * A single checklist item template inside a [Routine].
 * The id is a stable uuid so reordering/renaming the template does not
 * corrupt today's checks stored in [RoutineTodayState].
 */
@Serializable
data class RoutineStepTemplate(
    val id: String,
    val title: String,
    val description: String = "",
    val sortOrder: Int = 0
)

/**
 * A routine template. Steps are stored inline as JSON ([stepsJson]) rather
 * than a child table; history only keeps the daily percent ([RoutineLog]).
 * An empty [activeWeekdays] set means paused.
 */
data class Routine(
    val id: String,
    val title: String,
    val description: String = "",
    val reminderMinutes: Int? = null,
    val activeWeekdays: Set<DayOfWeek> = AllWeekdays,
    val sortOrder: Int = 0,
    val steps: List<RoutineStepTemplate> = emptyList(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

/** Percent-only history row for one routine on one day. Feeds the heatmap. */
data class RoutineLog(
    val routineId: String,
    val dateEpochDays: Int,
    val percent: Int
)

/** Transient today-state kept in AppDataStore (never in history). */
data class RoutineTodayState(
    val epochDay: Int?,
    val checks: Map<String, Set<String>> = emptyMap()
)

private val routineJson = Json { ignoreUnknownKeys = true }

fun encodeRoutineSteps(steps: List<RoutineStepTemplate>): String =
    routineJson.encodeToString(ListSerializer(RoutineStepTemplate.serializer()), steps)

fun decodeRoutineSteps(json: String): List<RoutineStepTemplate> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        routineJson.decodeFromString(ListSerializer(RoutineStepTemplate.serializer()), json)
    }.getOrDefault(emptyList())
}

fun encodeRoutineChecks(checks: Map<String, Set<String>>): String =
    routineJson.encodeToString(MapSerializer(String.serializer(), SetSerializer(String.serializer())), checks)

fun decodeRoutineChecks(json: String?): Map<String, Set<String>> {
    if (json.isNullOrBlank()) return emptyMap()
    return runCatching {
        routineJson.decodeFromString(MapSerializer(String.serializer(), SetSerializer(String.serializer())), json)
    }.getOrDefault(emptyMap())
}

fun encodeActiveWeekdays(days: Set<DayOfWeek>): String =
    routineJson.encodeToString(
        ListSerializer(String.serializer()),
        days.sortedBy { it.ordinal }.map { it.name }
    )

/**
 * Blank or corrupt JSON falls back to all days (e.g. rows predating the
 * column); an explicitly stored empty list stays empty (paused).
 */
fun decodeActiveWeekdays(json: String?): Set<DayOfWeek> {
    if (json.isNullOrBlank()) return AllWeekdays
    return runCatching {
        routineJson.decodeFromString(ListSerializer(String.serializer()), json)
            .mapNotNull { name -> runCatching { DayOfWeek.valueOf(name) }.getOrNull() }
            .toSet()
    }.getOrDefault(AllWeekdays)
}

/** True when [date] falls on a scheduled (non-paused) weekday. */
fun isRoutineScheduled(date: LocalDate, activeWeekdays: Set<DayOfWeek>): Boolean =
    activeWeekdays.isNotEmpty() && date.dayOfWeek in activeWeekdays

/** Next scheduled date on or after [from], or null when paused. */
fun nextScheduledDate(from: LocalDate, activeWeekdays: Set<DayOfWeek>): LocalDate? {
    if (activeWeekdays.isEmpty()) return null
    var date = from
    repeat(7) {
        if (date.dayOfWeek in activeWeekdays) return date
        date = date.plus(1, DateTimeUnit.DAY)
    }
    return null
}

/**
 * Streak over threshold days that skips off-schedule (neutral) days instead
 * of breaking on them. Terminates: with a non-empty schedule every 7-day
 * window holds a scheduled day, and [doneDates] is finite.
 */
fun calculateRoutineStreak(
    doneDates: Set<LocalDate>,
    activeWeekdays: Set<DayOfWeek>,
    today: LocalDate
): Int {
    if (activeWeekdays.isEmpty()) return 0
    var day = today
    var streak = 0
    while (true) {
        if (day.dayOfWeek in activeWeekdays) {
            if (day in doneDates) {
                streak += 1
            } else {
                break
            }
        }
        day = day.minus(1, DateTimeUnit.DAY)
    }
    return streak
}

/** A reminder fires only when a time is set and the routine is not paused. */
fun shouldScheduleRoutineReminder(reminderMinutes: Int?, activeWeekdays: Set<DayOfWeek>): Boolean =
    reminderMinutes != null && activeWeekdays.isNotEmpty()

/** Percent of [checkedStepIds] (intersected with [validStepIds]) over [totalSteps]. */
fun routinePercent(
    totalSteps: Int,
    checkedStepIds: Set<String>,
    validStepIds: Set<String>
): Int {
    if (totalSteps <= 0) return 0
    val done = checkedStepIds.count { it in validStepIds }
    return (done * 100 / totalSteps).coerceIn(0, 100)
}

fun routineMeetsStreakThreshold(percent: Int): Boolean =
    percent >= RoutineStreakThresholdPercent

/**
 * Drops stale checks when the stored day is not today.
 * Pure so the rollover rule is unit-testable without DataStore.
 */
fun resolveRoutineTodayChecks(
    storedEpochDay: Int?,
    storedChecks: Map<String, Set<String>>,
    todayEpochDay: Int
): Map<String, Set<String>> =
    if (storedEpochDay == todayEpochDay) storedChecks else emptyMap()

/** Dates with percent >= [RoutineStreakThresholdPercent], for streak math. */
fun routineStreakDates(logs: List<RoutineLog>): Set<LocalDate> =
    logs
        .filter { routineMeetsStreakThreshold(it.percent) }
        .map { LocalDate.fromEpochDays(it.dateEpochDays) }
        .toSet()

/** Heatmap intensity 0..1 per date from percent-only logs. */
fun routineIntensityByDate(logs: List<RoutineLog>): Map<LocalDate, Float> =
    logs.associate { LocalDate.fromEpochDays(it.dateEpochDays) to (it.percent / 100f).coerceIn(0f, 1f) }

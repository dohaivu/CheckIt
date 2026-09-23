package com.checkit.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Percent threshold for a day to count toward a routine streak. */
const val RoutineStreakThresholdPercent = 80

/**
 * A single checklist item template inside a [Routine].
 * The id is a stable uuid so reordering/renaming the template does not
 * corrupt today's checks stored in [RoutineTodayState].
 */
@Serializable
data class RoutineStepTemplate(
    val id: String,
    val title: String,
    val sortOrder: Int = 0
)

/**
 * A routine template. Steps are stored inline as JSON ([stepsJson]) rather
 * than a child table; history only keeps the daily percent ([RoutineLog]).
 */
data class Routine(
    val id: String,
    val title: String,
    val reminderMinutes: Int? = null,
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

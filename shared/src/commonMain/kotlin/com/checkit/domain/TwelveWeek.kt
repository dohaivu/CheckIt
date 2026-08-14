package com.checkit.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

const val TWELVE_WEEK_LENGTH_DAYS = 84
const val TWELVE_WEEK_LAST_INDEX = 11
const val TWELVE_WEEK_MAX_GOALS = 5
const val TWELVE_WEEK_MIN_SCORE = 0
const val TWELVE_WEEK_MAX_SCORE = 10

enum class TwelveWeekCycleStatus {
    Active,
    Completed,
    Abandoned
}

enum class TwelveWeekGoalFinalStatus {
    Achieved,
    Partial,
    Missed
}

enum class TwelveWeekCheckInReminderStatus {
    Normal,
    Due,
    Overdue,
    CheckedIn
}

data class TwelveWeekCheckInReminder(
    val weekIndex: Int,
    val status: TwelveWeekCheckInReminderStatus
)

data class TwelveWeekCycle(
    val id: Long = 0L,
    val title: String = "",
    val startEpochDays: Int,
    val endEpochDays: Int,
    val status: TwelveWeekCycleStatus,
    val reviewNote: String = "",
    val createdAtMillis: Long,
    val completedAtMillis: Long? = null
)

data class TwelveWeekGoal(
    val id: Long = 0L,
    val cycleId: Long,
    val title: String,
    val note: String = "",
    val sortOrder: Int,
    val finalStatus: TwelveWeekGoalFinalStatus? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class TwelveWeekCheckIn(
    val id: Long = 0L,
    val cycleId: Long,
    val weekIndex: Int,
    val note: String = "",
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class TwelveWeekGoalScore(
    val id: Long = 0L,
    val checkInId: Long,
    val goalId: Long,
    val score: Int,
    val note: String = ""
)

data class TwelveWeekGoalTaskLink(
    val goalId: Long,
    val taskId: Long,
    val sortOrder: Int
)

data class TwelveWeekWorkspace(
    val cycleCards: List<TwelveWeekCycleCard> = emptyList(),
    val checkIns: List<TwelveWeekCheckIn> = emptyList(),
    val scores: List<TwelveWeekGoalScore> = emptyList(),
    val pastCycles: List<TwelveWeekCycle> = emptyList()
)

data class TwelveWeekGoalCard(
    val goal: TwelveWeekGoal,
    val tactics: List<TaskItem> = emptyList(),
    val latestScore: TwelveWeekGoalScore? = null,
    val averageScore: Double? = null
)

data class TwelveWeekCycleCard(
    val cycle: TwelveWeekCycle,
    val goals: List<TwelveWeekGoalCard>,
    val currentWeekIndex: Int?,
    val checkInReminder: TwelveWeekCheckInReminder? = null
)

fun twelveWeekEndEpochDays(startEpochDays: Int): Int =
    startEpochDays + TWELVE_WEEK_LENGTH_DAYS - 1

fun weekIndexFor(cycleStartEpochDays: Int, dateEpochDays: Int): Int? {
    val delta = dateEpochDays - cycleStartEpochDays
    if (delta !in 0 until TWELVE_WEEK_LENGTH_DAYS) return null
    return delta / 7
}

/** Returns the Monday (ISO week start) of the week containing [epochDays]. */
fun mondayOfWeek(epochDays: Int): Int {
    val dayOfWeek = LocalDate.fromEpochDays(epochDays).dayOfWeek
    val offset = (dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal + 7) % 7
    return epochDays - offset
}

fun weekDateRange(cycleStartEpochDays: Int, weekIndex: Int): IntRange {
    val start = cycleStartEpochDays + weekIndex * 7
    return start..(start + 6)
}

fun executionScore(scores: List<Int>): Double? =
    scores.takeIf { it.isNotEmpty() }?.average()

package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** How often a [RepeatRule] fires. Mirrors the preset choices popular apps expose. */
enum class RepeatFrequency { None, Daily, Weekly, Monthly }

/** Every day of the week. */
val AllWeekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet()

/** Monday-Friday preset. */
val WeekdayPresetWeekdays: Set<DayOfWeek> = setOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY
)

/** Saturday-Sunday preset. */
val WeekdayPresetWeekends: Set<DayOfWeek> = setOf(
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
)

/**
 * The single recurrence model shared by tasks and routines.
 *
 * Only the shapes popular apps actually surface are modelled: a frequency plus,
 * for weekly rules, the weekdays it fires on, and for monthly rules the day of
 * the month. There is no interval and no iCal grammar - an interval needs a
 * hidden anchor date and a grammar needs a parser, and neither is needed today.
 * Adding an interval later is additive; see the migration note in
 * [CheckItDatabase][com.checkit.data.CheckItDatabase].
 *
 * Stored as one JSON string per row on both `tasks.repeatRuleJson` and
 * `routines.repeatRuleJson`. See [encode] / [decode].
 */
@Serializable
data class RepeatRule(
    val frequency: RepeatFrequency = RepeatFrequency.None,
    val weekdays: Set<DayOfWeek> = emptySet(),
    /** Day of month 1..31; only meaningful for [RepeatFrequency.Monthly]. Days past the end of a month clamp down. */
    val dayOfMonth: Int? = null
) {
    init {
        require(weekdays.isEmpty() || frequency == RepeatFrequency.Weekly) {
            "weekdays are only meaningful for weekly rules"
        }
        require(frequency != RepeatFrequency.Weekly || weekdays.isNotEmpty()) {
            "weekly rules must name at least one weekday"
        }
        require(dayOfMonth == null || frequency == RepeatFrequency.Monthly) {
            "dayOfMonth is only meaningful for monthly rules"
        }
        require(dayOfMonth == null || dayOfMonth in 1..31) {
            "dayOfMonth must be in 1..31"
        }
    }

    /** False while the rule is off; an off rule never fires. */
    val isActive: Boolean get() = frequency != RepeatFrequency.None

    /** True when [date] falls on an occurrence. Pure: no anchor date is needed. */
    fun isScheduled(date: LocalDate): Boolean = when (frequency) {
        RepeatFrequency.None -> false
        RepeatFrequency.Daily -> true
        RepeatFrequency.Weekly -> date.dayOfWeek in weekdays
        RepeatFrequency.Monthly -> date.dayOfMonth == resolveDayOfMonth(date)
    }

    /**
     * First occurrence on or after [from], or null for an off rule. Every
     * active frequency repeats at least once a month, so [MaxScanDays] always
     * finds a match.
     */
    fun nextOnOrAfter(from: LocalDate): LocalDate? {
        if (!isActive) return null
        var date = from
        repeat(MaxScanDays) {
            if (isScheduled(date)) return date
            date = date.plus(1, DateTimeUnit.DAY)
        }
        return null
    }

    /** Clamps [dayOfMonth] to the length of [date]'s month (e.g. 31 in February). */
    private fun resolveDayOfMonth(date: LocalDate): Int =
        (dayOfMonth ?: DefaultMonthlyDay).coerceAtMost(date.lastDayOfMonth())

    companion object {
        /** Does not repeat. */
        val None = RepeatRule()

        /** Every day. */
        val Daily = RepeatRule(RepeatFrequency.Daily)

        /** Longest gap any active frequency can have is a month; scan slightly past a year. */
        const val MaxScanDays = 400

        private const val DefaultMonthlyDay = 1
        private val repeatJson = Json { ignoreUnknownKeys = true }

        /** Weekly on [days]; an empty set yields [None] rather than an invalid rule. */
        fun weekly(days: Set<DayOfWeek>): RepeatRule =
            if (days.isEmpty()) None else RepeatRule(RepeatFrequency.Weekly, weekdays = days)

        /** Every day. */
        fun daily(): RepeatRule = Daily

        /** Monthly on [dayOfMonth]; days past the end of a month clamp down. */
        fun monthly(dayOfMonth: Int): RepeatRule =
            RepeatRule(RepeatFrequency.Monthly, dayOfMonth = dayOfMonth.coerceIn(1, 31))

        fun encode(rule: RepeatRule): String =
            repeatJson.encodeToString(serializer(), rule)

        /**
         * Decodes a stored rule. Returns null for blank or corrupt input so
         * callers can apply their own default (tasks repeat nothing, routines
         * repeat every day).
         */
        fun decode(json: String?): RepeatRule? {
            if (json.isNullOrBlank()) return null
            return runCatching { repeatJson.decodeFromString(serializer(), json) }.getOrNull()
        }
    }
}

/** Short weekday label ("MO") usable from shared domain code. */
internal fun DayOfWeek.shortLabel(): String = name.take(2)

/** Last day of this date's month, in 1..31. */
internal fun LocalDate.lastDayOfMonth(): Int {
    val firstOfNextMonth = LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH)
    return firstOfNextMonth.minus(1, DateTimeUnit.DAY).dayOfMonth
}

/** User-facing summary for pills, chips and list rows. */
fun RepeatRule.label(): String = when (frequency) {
    RepeatFrequency.None -> "Does not repeat"
    RepeatFrequency.Daily -> "Everyday"
    RepeatFrequency.Weekly -> when {
        weekdays == AllWeekdays -> "Everyday"
        weekdays == WeekdayPresetWeekdays -> "Weekdays"
        weekdays == WeekdayPresetWeekends -> "Weekends"
        weekdays.size == 1 -> "Weekly on ${weekdays.first().shortLabel()}"
        else -> weekdays.sortedBy { it.ordinal }.joinToString(", ") { it.shortLabel() }
    }
    RepeatFrequency.Monthly -> "Monthly"
}

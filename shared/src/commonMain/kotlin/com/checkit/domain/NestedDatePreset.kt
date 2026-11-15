package com.checkit.domain

import kotlinx.datetime.LocalDate

/**
 * One-tap date windows for the outline filter.
 *
 * Presets deliberately stay inside [Period] arithmetic: they resolve through
 * [FocusPeriod], so both platforms share week starts, month boundaries and
 * quarter rules. Rolling windows ("last 7 days", "overdue") are intentionally
 * not presets, because they are not calendar periods and would need their own
 * window type.
 */
enum class NestedDatePreset {
    Today,
    Yesterday,
    Tomorrow,
    ThisWeek,
    NextWeek,
    ThisMonth,
    NextMonth;

    /**
     * Equivalent focus for the device-local [today]. Keeping the preset as a
     * [FocusPeriod] means the existing filter header and prev/next stepping
     * stay the single source of truth.
     */
    fun focus(today: LocalDate): FocusPeriod = when (this) {
        Today -> FocusPeriod(Period.Day, today)
        Yesterday -> FocusPeriod(Period.Day, today).shift(-1)
        Tomorrow -> FocusPeriod(Period.Day, today).shift(1)
        ThisWeek -> FocusPeriod(Period.Week, today)
        NextWeek -> FocusPeriod(Period.Week, today).shift(1)
        ThisMonth -> FocusPeriod(Period.Month, today)
        NextMonth -> FocusPeriod(Period.Month, today).shift(1)
    }

    /** Closed, inclusive calendar window for [today]. */
    fun window(today: LocalDate): NestedDateWindow = focus(today).asDateWindow()
}

/** Closed, inclusive date window used by the outline date filter. */
data class NestedDateWindow(
    val start: LocalDate,
    val endInclusive: LocalDate
)

fun FocusPeriod.asDateWindow(): NestedDateWindow = NestedDateWindow(start, endInclusive)

/**
 * True when [focus] resolves to the same window as this preset.
 *
 * Compares resolved windows rather than [FocusPeriod.anchorDate]: any anchor
 * inside the same week/month yields the same dates, and a preset must stay
 * selected when the header was opened on a different day of that period.
 */
fun NestedDatePreset.matchesFocus(focus: FocusPeriod?, today: LocalDate): Boolean {
    if (focus == null) return false
    val window = window(today)
    return focus.start == window.start && focus.endInclusive == window.endInclusive
}

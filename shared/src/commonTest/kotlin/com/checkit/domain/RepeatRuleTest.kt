package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepeatRuleTest {

    private val monday = LocalDate(2026, 9, 21)
    private val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

    @Test
    fun offRuleIsNeverScheduled() {
        assertFalse(RepeatRule.None.isActive)
        assertFalse(RepeatRule.None.isScheduled(monday))
        assertNull(RepeatRule.None.nextOnOrAfter(monday))
    }

    @Test
    fun dailyRuleIsScheduledEveryDay() {
        val rule = RepeatRule.daily()
        assertTrue(rule.isActive)
        assertTrue(rule.isScheduled(monday))
        assertTrue(rule.isScheduled(monday.plus(30, DateTimeUnit.DAY)))
        assertEquals(monday, rule.nextOnOrAfter(monday))
    }

    @Test
    fun weeklyRuleRespectsWeekdays() {
        val rule = RepeatRule.weekly(weekdays)
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertTrue(rule.isScheduled(monday))
        assertFalse(rule.isScheduled(monday.plus(1, DateTimeUnit.DAY)))
        assertTrue(rule.isScheduled(monday.plus(2, DateTimeUnit.DAY)))

        assertEquals(monday, rule.nextOnOrAfter(monday))
        assertEquals(LocalDate(2026, 9, 23), rule.nextOnOrAfter(monday.plus(1, DateTimeUnit.DAY)))
        assertEquals(LocalDate(2026, 9, 28), rule.nextOnOrAfter(LocalDate(2026, 9, 26)))
    }

    @Test
    fun everyDayRuleIsScheduledEveryWeek() {
        val rule = RepeatRule.weekly(AllWeekdays)
        assertTrue(rule.isScheduled(monday))
        assertTrue(rule.isScheduled(monday.plus(6, DateTimeUnit.DAY)))
    }

    @Test
    fun emptyWeekdaySelectionIsOff() {
        assertFalse(RepeatRule.weekly(emptySet()).isActive)
        assertFalse(RepeatRule.weekly(emptySet()).isScheduled(monday))
    }

    @Test
    fun monthlyRuleMatchesDayOfMonthAndClamps() {
        val rule = RepeatRule.monthly(31)
        assertTrue(rule.isScheduled(LocalDate(2026, 1, 31)))
        // February has no 31st: the rule fires on the last day instead.
        assertTrue(rule.isScheduled(LocalDate(2026, 2, 28)))
        assertFalse(rule.isScheduled(LocalDate(2026, 2, 27)))
        assertFalse(rule.isScheduled(LocalDate(2026, 3, 30)))

        assertEquals(LocalDate(2026, 1, 31), rule.nextOnOrAfter(LocalDate(2026, 1, 10)))
        assertEquals(LocalDate(2026, 2, 28), rule.nextOnOrAfter(LocalDate(2026, 2, 1)))
    }

    @Test
    fun monthlyRuleOutOfRangeDayIsClamped() {
        val rule = RepeatRule.monthly(45)
        // 45 is clamped to the largest valid day, 31.
        assertTrue(rule.isScheduled(LocalDate(2026, 9, 30)))
        assertFalse(rule.isScheduled(monday)) // Sep 21 is not the 30th
        assertTrue(rule.isScheduled(LocalDate(2026, 2, 28)))
    }

    @Test
    fun nextOnOrAfterFindsEveryActiveFrequencyWithinOneYear() {
        assertTrue(RepeatRule.daily().nextOnOrAfter(monday) != null)
        assertTrue(RepeatRule.weekly(setOf(DayOfWeek.SUNDAY)).nextOnOrAfter(monday) != null)
        assertTrue(RepeatRule.monthly(31).nextOnOrAfter(monday) != null)
    }

    @Test
    fun ruleJsonRoundTrip() {
        val rules = listOf(
            RepeatRule.None,
            RepeatRule.daily(),
            RepeatRule.weekly(weekdays),
            RepeatRule.weekly(AllWeekdays),
            RepeatRule.monthly(15)
        )
        rules.forEach { rule ->
            assertEquals(rule, RepeatRule.decode(RepeatRule.encode(rule)))
        }
    }

    @Test
    fun ruleJsonReturnsNullForBlankOrCorruptInput() {
        assertNull(RepeatRule.decode(null))
        assertNull(RepeatRule.decode(""))
        assertNull(RepeatRule.decode("not-json"))
        // A weekly rule with no weekdays is invalid rather than a fixed-up value.
        assertNull(RepeatRule.decode("""{"frequency":"Weekly","weekdays":[],"dayOfMonth":null}"""))
        // A dayOfMonth on a non-monthly rule is invalid too.
        assertNull(RepeatRule.decode("""{"frequency":"Daily","weekdays":[],"dayOfMonth":15}"""))
    }

    @Test
    fun ruleJsonIgnoresUnknownKeys() {
        val decoded = RepeatRule.decode("""{"frequency":"Daily","unknownField":1}""")
        assertEquals(RepeatRule.daily(), decoded)
    }

    @Test
    fun invalidRulesAreRejected() {
        // weekdays are only meaningful for Weekly
        assertThrowsIllegalArgument { RepeatRule(RepeatFrequency.Daily, weekdays, null) }
        // Weekly must name at least one weekday
        assertThrowsIllegalArgument { RepeatRule(RepeatFrequency.Weekly, emptySet(), null) }
        // dayOfMonth is only meaningful for Monthly
        assertThrowsIllegalArgument { RepeatRule(RepeatFrequency.Daily, emptySet(), 5) }
        // dayOfMonth must be in range
        assertThrowsIllegalArgument { RepeatRule(RepeatFrequency.Monthly, emptySet(), 45) }
    }

    @Test
    fun labelsCoverEveryFrequency() {
        assertEquals("Does not repeat", RepeatRule.None.label())
        assertEquals("Everyday", RepeatRule.daily().label())
        assertEquals("Everyday", RepeatRule.weekly(AllWeekdays).label())
        assertEquals("Weekdays", RepeatRule.weekly(WeekdayPresetWeekdays).label())
        assertEquals("Weekends", RepeatRule.weekly(WeekdayPresetWeekends).label())
        assertEquals("Weekly on WE", RepeatRule.weekly(setOf(DayOfWeek.WEDNESDAY)).label())
        assertEquals("MO, WE, FR", RepeatRule.weekly(weekdays).label())
        assertEquals("Monthly", RepeatRule.monthly(5).label())
    }

    private fun assertThrowsIllegalArgument(block: () -> Unit) {
        val error = runCatching { block() }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException, "Expected IllegalArgumentException but was $error")
    }
}

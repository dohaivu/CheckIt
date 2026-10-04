package com.checkit.ui

import com.checkit.domain.MetricItem
import com.checkit.domain.MetricUnit
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MetricDateBasedTest {
    private val today = LocalDate(2026, 10, 4)
    private fun epoch(date: LocalDate): Int = date.toEpochDays().toInt()

    private fun countdown(due: LocalDate, completed: Boolean = false) = MetricItem(
        name = "Visa",
        value = "",
        unit = MetricUnit.Countdown,
        dueDateEpochDays = epoch(due),
        isCompleted = completed
    )

    private fun dueDate(due: LocalDate, time: Int? = null, completed: Boolean = false) = MetricItem(
        name = "Bill",
        value = "",
        unit = MetricUnit.DueDate,
        dueDateEpochDays = epoch(due),
        dueTimeMinutes = time,
        isCompleted = completed
    )

    @Test
    fun countdownDaysRemaining() {
        assertEquals(3, countdown(LocalDate(2026, 10, 7)).daysRemaining(today))
        assertEquals(0, countdown(today).daysRemaining(today))
        assertEquals(-2, countdown(LocalDate(2026, 10, 2)).daysRemaining(today))
        assertNull(countdown(today).copy(dueDateEpochDays = null).daysRemaining(today))
        assertNull(MetricItem(name = "x", value = "1", unit = MetricUnit.Points).daysRemaining(today))
    }

    @Test
    fun countdownLabels() {
        assertEquals("3 days left", countdown(LocalDate(2026, 10, 7)).countdownLabel(today))
        assertEquals("1 day left", countdown(LocalDate(2026, 10, 5)).countdownLabel(today))
        assertEquals("Due today", countdown(today).countdownLabel(today))
        assertEquals("Overdue by 1 day", countdown(LocalDate(2026, 10, 3)).countdownLabel(today))
        assertEquals("Overdue by 2 days", countdown(LocalDate(2026, 10, 2)).countdownLabel(today))
    }

    @Test
    fun countdownOverdueUnlessCompleted() {
        assertTrue(countdown(LocalDate(2026, 10, 2)).isMetricOverdue(today))
        assertFalse(countdown(LocalDate(2026, 10, 7)).isMetricOverdue(today))
        assertFalse(countdown(today).isMetricOverdue(today))
        assertFalse(countdown(LocalDate(2026, 10, 2), completed = true).isMetricOverdue(today))
        assertFalse(countdown(today).copy(dueDateEpochDays = null).isMetricOverdue(today))
    }

    @Test
    fun dueDateOverdueByDate() {
        assertTrue(dueDate(LocalDate(2026, 10, 2)).isMetricOverdue(today))
        assertFalse(dueDate(LocalDate(2026, 10, 7)).isMetricOverdue(today))
        assertFalse(dueDate(LocalDate(2026, 10, 2), completed = true).isMetricOverdue(today))
    }

    @Test
    fun dueDateLabelShowsDateAndOptionalTime() {
        val dateOnly = dueDate(LocalDate(2026, 10, 5)).dueDateLabel()
        val withTime = dueDate(LocalDate(2026, 10, 5), 14 * 60 + 30).dueDateLabel()
        assertFalse(dateOnly.contains(","), dateOnly)
        assertTrue(withTime.endsWith(", 2:30 PM"), withTime)
    }

    @Test
    fun dateBasedUnitsHaveNoProgress() {
        assertNull(countdown(LocalDate(2026, 10, 7)).progressRatio())
        assertNull(dueDate(LocalDate(2026, 10, 7)).progressRatio())
    }

    @Test
    fun isValidForSaveKeepsDatedMetricsDropsEmptyDrafts() {
        assertTrue(countdown(LocalDate(2026, 10, 7)).isValidForSave())
        assertFalse(countdown(today).copy(dueDateEpochDays = null).isValidForSave())
        assertTrue(dueDate(LocalDate(2026, 10, 7)).isValidForSave())
        assertFalse(dueDate(LocalDate(2026, 10, 7)).copy(dueDateEpochDays = null).isValidForSave())
        assertTrue(MetricItem(name = "Pages", value = "20").isValidForSave())
        assertFalse(MetricItem(name = "", value = "").isValidForSave())
    }

    @Test
    fun plainStringUsesDerivedDisplay() {        assertTrue(countdown(LocalDate(2026, 10, 7)).dateBasedDisplay(today).contains("3 days left"))
        assertTrue(dueDate(LocalDate(2026, 10, 5), 14 * 60 + 30).dateBasedDisplay().endsWith(", 2:30 PM"))
        assertEquals("Countdown", MetricUnit.Countdown.displayName())
        assertEquals("Due date", MetricUnit.DueDate.displayName())
    }
}

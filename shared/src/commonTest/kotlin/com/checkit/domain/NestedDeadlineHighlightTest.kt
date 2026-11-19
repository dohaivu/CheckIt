package com.checkit.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A red date label means "missed deadline", not "this range is in the past".
 * Only open deadlines (unchecked tasks or unfinished date metrics) qualify;
 * finished work and plain dated notes keep the neutral label.
 */
class NestedDeadlineHighlightTest {

    private val today = LocalDate(2026, 11, 16)
    private val yesterday = today.minus(1, DateTimeUnit.DAY)

    private fun item(
        checkboxEnabled: Boolean = false,
        checked: Boolean = false,
        startDate: LocalDate? = null,
        endDate: LocalDate? = yesterday,
        metrics: List<MetricItem> = emptyList()
    ) = NestedListItem(
        documentId = "doc",
        position = 0,
        text = "item",
        checkboxEnabled = checkboxEnabled,
        checked = checked,
        startDate = startDate,
        endDate = endDate,
        manualMetrics = metrics,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun metric(
        unit: MetricUnit,
        enabled: Boolean = true,
        completed: Boolean = false
    ) = MetricItem(
        name = "m",
        value = "1",
        unit = unit,
        enabled = enabled,
        isCompleted = completed
    )

    @Test
    fun uncheckedTaskWithPastEndHasMissedDeadline() {
        val task = item(checkboxEnabled = true)
        assertTrue(task.isOpenDeadlineItem())
        assertTrue(task.hasMissedDeadline(today))
    }

    @Test
    fun checkedTaskIsNotAnOpenDeadline() {
        val done = item(checkboxEnabled = true, checked = true)
        assertFalse(done.isOpenDeadlineItem())
        assertFalse(done.hasMissedDeadline(today))
    }

    @Test
    fun endTodayOrInTheFutureIsNotMissed() {
        val dueToday = item(checkboxEnabled = true, endDate = today)
        val future = item(checkboxEnabled = true, endDate = today.plus(2, DateTimeUnit.DAY))
        assertFalse(dueToday.hasMissedDeadline(today))
        assertFalse(future.hasMissedDeadline(today))
    }

    @Test
    fun startOnlyRangeIsNotMissed() {
        val startOnly = item(checkboxEnabled = true, startDate = yesterday, endDate = null)
        assertTrue(startOnly.isOpenDeadlineItem())
        assertFalse(startOnly.hasMissedDeadline(today))
    }

    @Test
    fun openDateMetricMakesAPastRangeADeadline() {
        val countdown = item(metrics = listOf(metric(MetricUnit.Countdown)))
        val dueDate = item(metrics = listOf(metric(MetricUnit.DueDate)))
        assertTrue(countdown.hasMissedDeadline(today))
        assertTrue(dueDate.hasMissedDeadline(today))
    }

    @Test
    fun completedOrDisabledDateMetricsDoNotQualify() {
        val completed = item(metrics = listOf(metric(MetricUnit.DueDate, completed = true)))
        val disabled = item(metrics = listOf(metric(MetricUnit.Countdown, enabled = false)))
        assertFalse(completed.hasMissedDeadline(today))
        assertFalse(disabled.hasMissedDeadline(today))
    }

    @Test
    fun nonDateMetricAndPlainDatedNoteDoNotQualify() {
        val points = item(metrics = listOf(metric(MetricUnit.Points)))
        val note = item()
        assertFalse(points.isOpenDeadlineItem())
        assertFalse(points.hasMissedDeadline(today))
        assertFalse(note.isOpenDeadlineItem())
        assertFalse(note.hasMissedDeadline(today))
    }
}

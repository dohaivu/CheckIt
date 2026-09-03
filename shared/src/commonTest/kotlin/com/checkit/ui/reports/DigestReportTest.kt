package com.checkit.ui.reports

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DailyReflectStat
import com.checkit.domain.DoneItemSummary
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.reflect.DigestReportSummary
import com.checkit.ui.reflect.buildDigestReport
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DigestReportTest {

    private fun doneTask(
        id: Long,
        title: String,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = 0,
        title = title,
        source = DailyPlanItemSource.MyDayTask,
        status = DailyPlanItemStatus.Done,
        sortOrder = 0,
        startTimeMinutes = startTimeMinutes,
        endTimeMinutes = endTimeMinutes,
        addedAtMillis = 0L,
        completedAtMillis = 1L
    )

    private fun digestFor(
        period: ReportPeriod,
        selectedDate: LocalDate,
        plans: List<DailyPlan>
    ): DigestReportSummary {
        val stats = plans.map { plan ->
            DailyReflectStat(
                dateEpochDays = plan.date.toEpochDays().toInt(),
                plannedItemCount = 0,
                doneItemCount = plan.items.count { it.status == DailyPlanItemStatus.Done },
                doneMinutes = plan.items
                    .filter { it.status == DailyPlanItemStatus.Done }
                    .sumOf { item ->
                        val start = item.startTimeMinutes ?: 0
                        val end = item.endTimeMinutes ?: 0
                        (end - start).coerceAtLeast(0)
                    },
                journalCount = 0
            )
        }
        val summaries = plans.flatMap { plan ->
            plan.items.filter { it.status == DailyPlanItemStatus.Done }.map {
                DoneItemSummary(
                    id = it.id,
                    dateEpochDays = plan.date.toEpochDays().toInt(),
                    title = it.title,
                    note = it.note,
                    sourceName = it.source.name,
                    startTimeMinutes = it.startTimeMinutes,
                    endTimeMinutes = it.endTimeMinutes,
                    completedAtMillis = it.completedAtMillis
                )
            }
        }
        return buildDigestReport(
            statsByDate = stats.associateBy { it.dateEpochDays },
            tagRollups = emptyList(),
            doneItems = summaries,
            journalEntries = emptyList(),
            period = period,
            selectedDate = selectedDate
        )
    }

    @Test
    fun monthDigestAggregatesAcrossWholeMonth() {
        val selectedDate = LocalDate(2026, 8, 15)
        val plans = listOf(
            DailyPlan(
                date = LocalDate(2026, 8, 1),
                items = listOf(doneTask(1L, "Alpha", 480, 540))
            ),
            DailyPlan(
                date = LocalDate(2026, 8, 31),
                items = listOf(doneTask(2L, "Beta", 600, 660))
            ),
            DailyPlan(
                date = LocalDate(2026, 7, 31),
                items = listOf(doneTask(3L, "Outside", 480, 480))
            )
        )

        val digest = digestFor(ReportPeriod.Month, selectedDate, plans)

        assertEquals(LocalDate(2026, 8, 1), digest.startDate)
        assertEquals(LocalDate(2026, 8, 31), digest.endDate)
        assertEquals(120, digest.totalMinutes)
        assertEquals(2, digest.doneItemCount)
        assertEquals(listOf("Beta", "Alpha"), digest.highlights.map { it.title })
        assertTrue(digest.activityItems.isNotEmpty())
        assertEquals(120, digest.activityItems.sumOf { it.totalMinutes })
    }

    @Test
    fun annualDigestAggregatesAcrossWholeYear() {
        val selectedDate = LocalDate(2026, 8, 15)
        val plans = listOf(
            DailyPlan(
                date = LocalDate(2026, 1, 5),
                items = listOf(doneTask(1L, "Jan", 480, 540))
            ),
            DailyPlan(
                date = LocalDate(2026, 12, 20),
                items = listOf(doneTask(2L, "Dec", 600, 660))
            ),
            DailyPlan(
                date = LocalDate(2025, 12, 31),
                items = listOf(doneTask(3L, "Outside", 480, 480))
            )
        )

        val digest = digestFor(ReportPeriod.Annual, selectedDate, plans)

        assertEquals(LocalDate(2026, 1, 1), digest.startDate)
        assertEquals(LocalDate(2026, 12, 31), digest.endDate)
        assertEquals(120, digest.totalMinutes)
        assertEquals(2, digest.doneItemCount)
        assertEquals(12, digest.activityItems.size)
        assertEquals(120, digest.activityItems.sumOf { it.totalMinutes })
    }

    @Test
    fun weekDigestStillCoversSevenDays() {
        val selectedDate = LocalDate(2026, 8, 5)
        val plans = listOf(
            DailyPlan(
                date = LocalDate(2026, 8, 3),
                items = listOf(doneTask(1L, "Mon", 480, 540))
            ),
            DailyPlan(
                date = LocalDate(2026, 8, 9),
                items = listOf(doneTask(2L, "Sun", 600, 660))
            )
        )

        val digest = digestFor(ReportPeriod.Week, selectedDate, plans)

        assertEquals(120, digest.totalMinutes)
        assertEquals(7, digest.activityItems.size)
        assertEquals(listOf("Sun", "Mon"), digest.highlights.map { it.title })
    }

    @Test
    fun dailyDigestUsesSingleDay() {
        val selectedDate = LocalDate(2026, 8, 5)
        val plans = listOf(
            DailyPlan(
                date = selectedDate,
                items = listOf(doneTask(1L, "Today", 480, 540))
            )
        )

        val digest = digestFor(ReportPeriod.Daily, selectedDate, plans)

        assertEquals(60, digest.totalMinutes)
        assertEquals(7, digest.activityItems.size)
        assertEquals(listOf("Today"), digest.highlights.map { it.title })
    }

    @Test
    fun digestCountsOnlyPeriodMinutes() {
        val selectedDate = LocalDate(2026, 8, 5)
        val plans = listOf(
            DailyPlan(
                date = LocalDate(2026, 8, 6),
                items = listOf(doneTask(1L, "InWeek", 480, 540))
            ),
            DailyPlan(
                date = LocalDate(2026, 8, 6).plus(7, DateTimeUnit.DAY),
                items = listOf(doneTask(2L, "NextWeek", 480, 600))
            )
        )

        val digest = digestFor(ReportPeriod.Week, selectedDate, plans)

        assertEquals(60, digest.totalMinutes)
        assertEquals(listOf("InWeek"), digest.highlights.map { it.title })
    }
}

package com.checkit.domain.usecase

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DailyPlanItemStatus.Done
import com.checkit.domain.DailyPlanItemStatus.Planned
import com.checkit.domain.PeriodFocus
import com.checkit.domain.ReviewPeriod
import com.checkit.domain.TagItem
import com.checkit.domain.ReviewSource
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuildPeriodReviewDraftUseCaseTest {
    private val build = BuildPeriodReviewDraftUseCase()
    private val date = LocalDate(2026, 7, 9)

    @Test
    fun buildsWeeklyDraftWithStatsTagsAndHighlights() = runTest {
        val focus = PeriodFocus(ReviewPeriod.Week, date)
        val work = TagItem(id = 1L, name = "Work", color = "#2563EB")
        val home = TagItem(id = 2L, name = "Home", color = "#059669")
        val plans = listOf(
            DailyPlan(
                date = date,
                items = listOf(
                    item(id = 1L, title = "Deep work", status = Done, start = 540, end = 600, tags = listOf(work)),
                    item(id = 2L, title = "Emails", status = Done, start = 600, end = 630, tags = listOf(work)),
                    item(id = 3L, title = "Chores", status = Done, start = 660, end = 720, tags = listOf(home)),
                    item(id = 4L, title = "Still open", status = Planned, start = 720)
                )
            )
        )

        val draft = assertNotNull(build(focus, plans))
        assertTrue(draft.content.contains("3 items"))
        assertTrue(draft.content.contains("2h 30m"))
        assertTrue(draft.content.contains("Work (1h 30m)"))
        assertTrue(draft.content.contains("• Deep work"))
        assertTrue(draft.statsJson.contains("\"doneCount\":3"))
        assertTrue(draft.statsJson.contains("\"totalMinutes\":150"))
        assertTrue(draft.statsJson.contains("\"plannedCount\":1"))
        assertTrue(draft.highlightsJson.contains("Emails"))
    }

    @Test
    fun sortsTopTagsByMinutesDescending() = runTest {
        val focus = PeriodFocus(ReviewPeriod.Week, date)
        val small = TagItem(id = 1L, name = "Small", color = "#111111")
        val big = TagItem(id = 2L, name = "Big", color = "#222222")
        val plans = listOf(
            DailyPlan(
                date = date,
                items = listOf(
                    item(id = 1L, title = "A", status = Done, start = 540, end = 600, tags = listOf(small)),
                    item(id = 2L, title = "B", status = Done, start = 600, end = 780, tags = listOf(big))
                )
            )
        )

        val draft = assertNotNull(build(focus, plans))
        val bigIndex = draft.content.indexOf("Big")
        val smallIndex = draft.content.indexOf("Small")
        assertTrue(bigIndex >= 0 && smallIndex > bigIndex, "Big tag should come before Small")
    }

    @Test
    fun returnsNullWhenNoDoneActivityInRange() = runTest {
        val focus = PeriodFocus(ReviewPeriod.Week, date)
        val plans = listOf(
            DailyPlan(
                date = date,
                items = listOf(
                    item(id = 1L, title = "Open", status = Planned, start = 540)
                )
            )
        )

        assertNull(build(focus, plans))
    }

    @Test
    fun ignoresPlansOutsideFocusRange() = runTest {
        val focus = PeriodFocus(ReviewPeriod.Week, date)
        val outside = date.minus(10, kotlinx.datetime.DateTimeUnit.DAY)
        val plans = listOf(
            DailyPlan(
                date = outside,
                items = listOf(item(id = 1L, title = "Old", status = Done, start = 540, end = 600))
            )
        )

        assertNull(build(focus, plans))
    }

    @Test
    fun dayDraftUsesDayWord() = runTest {
        val focus = PeriodFocus(ReviewPeriod.Day, date)
        val plans = listOf(
            DailyPlan(
                date = date,
                items = listOf(item(id = 1L, title = "Win", status = Done, start = 540, end = 600))
            )
        )

        val draft = assertNotNull(build(focus, plans))
        assertTrue(draft.content.contains("this day"))
    }

    @Test
    fun hybridReviewRoundTripsStatsThroughSave() = runTest {
        val repository = FakeCheckItRepository()
        val focus = PeriodFocus(ReviewPeriod.Week, date)
        val plans = listOf(
            DailyPlan(
                date = date,
                items = listOf(item(id = 1L, title = "Win", status = Done, start = 540, end = 600))
            )
        )
        val draft = assertNotNull(build(focus, plans))

        SavePeriodReviewUseCase(repository)(
            focus = focus,
            content = draft.content,
            intentNext = "",
            source = ReviewSource.Hybrid,
            statsJson = draft.statsJson,
            highlightsJson = draft.highlightsJson
        )

        val saved = repository.observePeriodReviews().first().single()
        assertEquals(ReviewSource.Hybrid, saved.source)
        assertEquals(draft.statsJson, saved.statsJson)
        assertEquals(draft.highlightsJson, saved.highlightsJson)
        assertNotNull(saved.generatedAtMillis)
    }

    private fun item(
        id: Long,
        title: String,
        status: DailyPlanItemStatus,
        start: Int,
        end: Int? = null,
        tags: List<TagItem> = emptyList()
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = date.toEpochDays().toInt(),
        title = title,
        source = DailyPlanItemSource.MyDayTask,
        status = status,
        tags = tags,
        sortOrder = id.toInt(),
        startTimeMinutes = start,
        endTimeMinutes = end,
        addedAtMillis = 0L,
        completedAtMillis = if (status == Done) 1L else null
    )
}

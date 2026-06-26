package com.checkit.ui

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.Objective
import com.checkit.domain.TaskTag
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarUiStateMarkersTest {
    @Test
    fun pastDateMarkersCountDailyPlanItems() {
        val date = today()
        val state = CalendarUiState(
            dailyPlans = listOf(
                dailyPlan(
                    date = date,
                    items = listOf(
                        dailyPlanItem(id = 1L, source = DailyPlanItemSource.ExistingTask),
                        dailyPlanItem(id = 2L, source = DailyPlanItemSource.ExistingTask),
                        dailyPlanItem(id = 3L, source = DailyPlanItemSource.MyDayTask),
                        dailyPlanItem(id = 4L, source = DailyPlanItemSource.MyDayNote)
                    )
                )
            )
        )

        val markers = state.markersForDate(date)

        assertEquals(CalendarDateMarkers(totalCount = 4), markers)
        assertEquals(4, markers.totalCount)
    }

    @Test
    fun futureDateMarkersCountTaskAndNoteItems() {
        val date = today().plus(1, DateTimeUnit.DAY)
        val state = CalendarUiState(
            board = TaskBoard(
                tasks = listOf(task(id = 1L, date = date), task(id = 2L, date = date)),
                notes = listOf(note(id = 3L, date = date))
            )
        )

        val markers = state.markersForDate(date)

        assertEquals(CalendarDateMarkers(totalCount = 3), markers)
        assertEquals(3, markers.totalCount)
    }

    @Test
    fun dailyPlanForDateFiltersItemsBySelectedTags() {
        val date = today()
        val tagOne = tag(id = 1L)
        val tagTwo = tag(id = 2L)
        val tagThree = tag(id = 3L)
        val state = CalendarUiState(
            dailyPlans = listOf(
                dailyPlan(
                    date = date,
                    items = listOf(
                        dailyPlanItem(id = 1L, source = DailyPlanItemSource.ExistingTask, tags = listOf(tagOne)),
                        dailyPlanItem(id = 2L, source = DailyPlanItemSource.ExistingTask, tags = listOf(tagTwo)),
                        dailyPlanItem(id = 3L, source = DailyPlanItemSource.ExistingTask, tags = listOf(tagThree)),
                        dailyPlanItem(id = 4L, source = DailyPlanItemSource.MyDayTask)
                    )
                )
            ),
            selectedTagIds = setOf(tagOne.id, tagTwo.id)
        )

        val itemIds = state.dailyPlanForDate(date)?.items.orEmpty().map { it.id }

        assertEquals(listOf(1L, 2L), itemIds)
    }

    @Test
    fun pastDateMarkersUseSelectedTagFilterAcrossDates() {
        val date = today()
        val otherDate = today().minus(1, DateTimeUnit.DAY)
        val selectedTag = tag(id = 1L)
        val otherTag = tag(id = 2L)
        val state = CalendarUiState(
            dailyPlans = listOf(
                dailyPlan(
                    date = date,
                    items = listOf(
                        dailyPlanItem(id = 1L, source = DailyPlanItemSource.ExistingTask, tags = listOf(selectedTag)),
                        dailyPlanItem(id = 2L, source = DailyPlanItemSource.ExistingTask, tags = listOf(otherTag))
                    )
                ),
                dailyPlan(
                    date = otherDate,
                    items = listOf(
                        dailyPlanItem(id = 3L, source = DailyPlanItemSource.ExistingTask, tags = listOf(selectedTag)),
                        dailyPlanItem(id = 4L, source = DailyPlanItemSource.ExistingTask)
                    )
                )
            ),
            selectedTagIds = setOf(selectedTag.id)
        )

        assertEquals(CalendarDateMarkers(totalCount = 1), state.markersForDate(date))
        assertEquals(CalendarDateMarkers(totalCount = 1), state.markersForDate(otherDate))
    }

    private fun dailyPlan(
        date: LocalDate,
        items: List<DailyPlanItem>
    ) = DailyPlan(
        id = 1L,
        date = date,
        items = items,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun dailyPlanItem(
        id: Long,
        source: DailyPlanItemSource,
        tags: List<TaskTag> = emptyList()
    ) = DailyPlanItem(
        id = id,
        dailyPlanId = 1L,
        title = "Item $id",
        source = source,
        status = DailyPlanItemStatus.Planned,
        tags = tags,
        sortOrder = id.toInt(),
        addedAtMillis = 0L
    )

    private fun tag(id: Long) = TaskTag(
        id = id,
        name = "Tag $id",
        color = "#FFFFFF"
    )

    private fun task(
        id: Long,
        date: LocalDate
    ) = TaskItem(
        id = id,
        objective = Objective.None,
        name = "Task $id",
        doDate = date,
        sortOrder = id.toInt(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun note(
        id: Long,
        date: LocalDate
    ) = NoteItem(
        id = id,
        objective = Objective.None,
        content = "Note $id",
        date = date,
        createdAtMillis = 0L,
        editedAtMillis = 0L,
        sortOrder = id.toInt()
    )
}

package com.checkit.ui

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarUiStateMarkersTest {
    @Test
    fun pastDateMarkersCountDailyPlanItemTypes() {
        val date = today()
        val state = CalendarUiState(
            dailyPlans = listOf(
                dailyPlan(
                    date = date,
                    items = listOf(
                        dailyPlanItem(id = 1L, source = DailyPlanItemSource.ExistingTask),
                        dailyPlanItem(id = 2L, source = DailyPlanItemSource.ExistingTask),
                        dailyPlanItem(id = 3L, source = DailyPlanItemSource.CheckInManualDone),
                        dailyPlanItem(id = 4L, source = DailyPlanItemSource.CheckInNote)
                    )
                )
            )
        )

        val markers = state.markersForDate(date)

        assertEquals(CalendarDateMarkers(taskCount = 2, doneCount = 1, noteCount = 1), markers)
        assertEquals(4, markers.totalCount)
    }

    @Test
    fun futureDateMarkersCountTasksAndNotes() {
        val date = today().plus(1, DateTimeUnit.DAY)
        val state = CalendarUiState(
            board = TaskBoard(
                tasks = listOf(task(id = 1L, date = date), task(id = 2L, date = date)),
                notes = listOf(note(id = 3L, date = date))
            )
        )

        val markers = state.markersForDate(date)

        assertEquals(CalendarDateMarkers(taskCount = 2, doneCount = 0, noteCount = 1), markers)
        assertEquals(3, markers.totalCount)
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
        source: DailyPlanItemSource
    ) = DailyPlanItem(
        id = id,
        dailyPlanId = 1L,
        title = "Item $id",
        source = source,
        status = DailyPlanItemStatus.Planned,
        sortOrder = id.toInt(),
        addedAtMillis = 0L
    )

    private fun task(
        id: Long,
        date: LocalDate
    ) = TaskItem(
        id = id,
        list = TaskList.None,
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
        list = TaskList.None,
        content = "Note $id",
        date = date,
        createdAtMillis = 0L,
        editedAtMillis = 0L,
        sortOrder = id.toInt()
    )
}

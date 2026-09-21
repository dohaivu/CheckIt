package com.checkit.ui

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DailyReflectStat
import com.checkit.domain.DailyTagRollup
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TagItem
import com.checkit.ui.calendar.CalendarDateMarkers
import com.checkit.ui.calendar.CalendarUiState
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
                        dailyPlanItem(id = "1", source = DailyPlanItemSource.ExistingTask),
                        dailyPlanItem(id = "2", source = DailyPlanItemSource.ExistingTask),
                        dailyPlanItem(id = "3", source = DailyPlanItemSource.MyDayTask),
                        dailyPlanItem(id = "4", source = DailyPlanItemSource.MyDayNote)
                    )
                )
            )
        )

        val markers = state.markersForDate(TaskBoard(), date)

        assertEquals(CalendarDateMarkers(totalCount = 4), markers)
        assertEquals(4, markers.totalCount)
    }

    @Test
    fun futureDateMarkersCountTaskAndNoteItems() {
        val date = today().plus(1, DateTimeUnit.DAY)
        val board = TaskBoard(
            tasks = listOf(task(id = "1", date = date), task(id = "2", date = date)),
            notes = listOf(note(id = "3", date = date))
        )
        val state = CalendarUiState()

        val markers = state.markersForDate(board, date)

        assertEquals(CalendarDateMarkers(totalCount = 3), markers)
        assertEquals(3, markers.totalCount)
    }

    @Test
    fun dailyPlanForDateFiltersItemsBySelectedTags() {
        val date = today()
        val tagOne = tag(id = "1")
        val tagTwo = tag(id = "2")
        val tagThree = tag(id = "3")
        val state = CalendarUiState(
            dailyPlans = listOf(
                dailyPlan(
                    date = date,
                    items = listOf(
                        dailyPlanItem(id = "1", source = DailyPlanItemSource.ExistingTask, tags = listOf(tagOne)),
                        dailyPlanItem(id = "2", source = DailyPlanItemSource.ExistingTask, tags = listOf(tagTwo)),
                        dailyPlanItem(id = "3", source = DailyPlanItemSource.ExistingTask, tags = listOf(tagThree)),
                        dailyPlanItem(id = "4", source = DailyPlanItemSource.MyDayTask)
                    )
                )
            ),
            selectedTagIds = setOf(tagOne.id, tagTwo.id)
        )

        val itemIds = state.dailyPlanForDate(date)?.items.orEmpty().map { it.id }

        assertEquals(listOf("1", "2"), itemIds)
    }

    @Test
    fun pastDateMarkersUseSelectedTagFilterAcrossDates() {
        val date = today()
        val otherDate = today().minus(1, DateTimeUnit.DAY)
        val selectedTag = tag(id = "1")
        val otherTag = tag(id = "2")
        val state = CalendarUiState(
            dailyPlans = listOf(
                dailyPlan(
                    date = date,
                    items = listOf(
                        dailyPlanItem(id = "1", source = DailyPlanItemSource.ExistingTask, tags = listOf(selectedTag)),
                        dailyPlanItem(id = "2", source = DailyPlanItemSource.ExistingTask, tags = listOf(otherTag))
                    )
                )
            ),
            dailyStatsByDate = mapOf(
                otherDate to DailyReflectStat(
                    dateEpochDays = otherDate.toEpochDays().toInt(),
                    plannedItemCount = 1,
                    doneItemCount = 2,
                    doneMinutes = 30,
                    journalCount = 0,
                    tagRollups = listOf(
                        dailyTagRollup(
                            dateEpochDays = otherDate.toEpochDays().toInt(),
                            tagId = selectedTag.id,
                            doneCount = 1
                        ),
                        dailyTagRollup(
                            dateEpochDays = otherDate.toEpochDays().toInt(),
                            tagId = otherTag.id,
                            doneCount = 2
                        )
                    )
                )
            ),
            selectedTagIds = setOf(selectedTag.id)
        )

        assertEquals(CalendarDateMarkers(totalCount = 1), state.markersForDate(TaskBoard(), date))
        assertEquals(CalendarDateMarkers(totalCount = 1), state.markersForDate(TaskBoard(), otherDate))
    }

    private fun dailyPlan(
        date: LocalDate,
        items: List<DailyPlanItem>
    ) = DailyPlan(
        date = date,
        items = items
    )

    private fun dailyPlanItem(
        id: String,
        source: DailyPlanItemSource,
        tags: List<TagItem> = emptyList()
    ) = DailyPlanItem(
        id = id,
        dateEpochDays = 1,
        title = "Item $id",
        source = source,
        status = DailyPlanItemStatus.Planned,
        tags = tags,
        sortOrder = id.toInt(),
        addedAtMillis = 0L
    )

    private fun tag(id: String) = TagItem(
        id = id,
        name = "Tag $id",
        color = "#FFFFFF"
    )

    private fun dailyTagRollup(
        dateEpochDays: Int,
        tagId: String,
        doneCount: Int
    ) = DailyTagRollup(
        dateEpochDays = dateEpochDays,
        tagId = tagId,
        tagName = "Tag $tagId",
        tagColor = "#FFFFFF",
        doneCount = doneCount,
        doneMinutes = 0
    )

    private fun task(
        id: String,
        date: LocalDate
    ) = TaskItem(
        id = id,
        list = null,
        name = "Task $id",
        doDate = date,
        sortOrder = id.toInt(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun note(
        id: String,
        date: LocalDate
    ) = NoteItem(
        id = id,
        list = null,
        content = "Note $id",
        date = date,
        createdAtMillis = 0L,
        editedAtMillis = 0L,
        sortOrder = id.toInt()
    )
}

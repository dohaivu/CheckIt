package com.checkit.domain.usecase

import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.SubTaskItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskList
import com.checkit.domain.TaskStatus
import com.checkit.domain.TaskTag
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildDailyPlanMarkdownSummaryUseCaseTest {
    private val buildSummary = BuildDailyPlanMarkdownSummaryUseCase()
    private val date = LocalDate(2026, 6, 13)

    @Test
    fun buildsMinimalChronologicalMarkdownForDoneItems() {
        val work = TaskTag(id = 1L, name = "Work", color = "#2563EB")
        val planning = TaskTag(id = 2L, name = "Planning", color = "#059669")
        val task = task(
            id = 10L,
            name = "Plan the day",
            description = "Review agenda, timeline, and the next task to start.",
            subtasks = listOf(
                subtask(id = 1L, taskId = 10L, name = "Check calendar", isCompleted = true),
                subtask(id = 2L, taskId = 10L, name = "Pick top priority", isCompleted = false)
            )
        )
        val plan = dailyPlan(
            items = listOf(
                item(
                    id = 1L,
                    title = "Later item",
                    status = DailyPlanItemStatus.Planned,
                    startTimeMinutes = 11 * 60
                ),
                item(
                    id = 2L,
                    title = "Capture sprint idea",
                    note = "Consider grouping review reminders with daily planning.",
                    source = DailyPlanItemSource.MyDayNote,
                    startTimeMinutes = 10 * 60 + 15,
                    endTimeMinutes = null,
                    tags = listOf(TaskTag(id = 3L, name = "Product", color = "#7C3AED"))
                ),
                item(
                    id = 3L,
                    taskId = task.id,
                    title = task.name,
                    source = DailyPlanItemSource.ExistingTask,
                    startTimeMinutes = 9 * 60,
                    endTimeMinutes = 9 * 60 + 30,
                    tags = listOf(work, planning)
                )
            )
        )

        val markdown = buildSummary(
            date = date,
            plan = plan,
            board = TaskBoard(tasks = listOf(task))
        )

        assertEquals(
            listOf(
                "- **9:00 AM - 9:30 AM**  ",
                "Plan the day #Work #Planning  ",
                "_Review agenda, timeline, and the next task to start._  ",
                "  - [x] Check calendar",
                "  - [ ] Pick top priority",
                "",
                "- **10:15 AM**  ",
                "Capture sprint idea #Product  ",
                "_Consider grouping review reminders with daily planning._"
            ).joinToString("\n"),
            markdown
        )
    }

    @Test
    fun untimedDoneItemsUseAllDayAndKeepSummaryClean() {
        val plan = dailyPlan(
            items = listOf(
                item(
                    id = 1L,
                    title = "Timed",
                    note = "Done first.",
                    startTimeMinutes = 8 * 60,
                    endTimeMinutes = 8 * 60 + 10
                ),
                item(
                    id = 2L,
                    title = "Loose win",
                    note = "Wrapped a small follow-up.",
                    startTimeMinutes = null,
                    endTimeMinutes = null
                )
            )
        )

        val markdown = buildSummary(date = date, plan = plan, board = TaskBoard())

        assertEquals(
            listOf(
                "- **8:00 AM - 8:10 AM**  ",
                "Timed  ",
                "_Done first._",
                "",
                "- **All-Day**  ",
                "Loose win  ",
                "_Wrapped a small follow-up._"
            ).joinToString("\n"),
            markdown
        )
    }

    @Test
    fun emptyDoneItemsReturnEmptyMessage() {
        val markdown = buildSummary(
            date = date,
            plan = dailyPlan(items = listOf(item(id = 1L, status = DailyPlanItemStatus.Planned))),
            board = TaskBoard()
        )

        assertEquals("No completed daily-plan items.", markdown)
    }

    @Test
    fun blankTitleDoesNotRenderFallbackTitleOrTags() {
        val plan = dailyPlan(
            items = listOf(
                item(
                    id = 1L,
                    title = "   ",
                    note = "No title needed.",
                    startTimeMinutes = 9 * 60,
                    tags = listOf(TaskTag(id = 1L, name = "Hidden", color = "#2563EB"))
                )
            )
        )

        val markdown = buildSummary(date = date, plan = plan, board = TaskBoard())

        assertEquals(
            listOf(
                "- **9:00 AM**  ",
                "_No title needed._"
            ).joinToString("\n"),
            markdown
        )
    }

    @Test
    fun multilineNoteKeepsSeparateMarkdownLines() {
        val plan = dailyPlan(
            items = listOf(
                item(
                    id = 1L,
                    title = "Wrap up",
                    note = """
                        First line
                        Second line

                        Third line
                    """.trimIndent(),
                    startTimeMinutes = 17 * 60
                )
            )
        )

        val markdown = buildSummary(date = date, plan = plan, board = TaskBoard())

        assertEquals(
            listOf(
                "- **5:00 PM**  ",
                "Wrap up  ",
                "_First line_  ",
                "_Second line_  ",
                "_Third line_"
            ).joinToString("\n"),
            markdown
        )
    }

    private fun dailyPlan(items: List<DailyPlanItem>) = DailyPlan(
        id = 1L,
        date = date,
        items = items,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun item(
        id: Long,
        taskId: Long? = null,
        title: String = "Item $id",
        note: String? = null,
        source: DailyPlanItemSource = DailyPlanItemSource.MyDayTask,
        status: DailyPlanItemStatus = DailyPlanItemStatus.Done,
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null,
        tags: List<TaskTag> = emptyList()
    ) = DailyPlanItem(
        id = id,
        dailyPlanId = 1L,
        taskId = taskId,
        title = title,
        note = note,
        source = source,
        status = status,
        tags = tags,
        sortOrder = id.toInt(),
        startTimeMinutes = startTimeMinutes,
        endTimeMinutes = endTimeMinutes,
        addedAtMillis = 0L,
        completedAtMillis = 0L
    )

    private fun task(
        id: Long,
        name: String,
        description: String,
        subtasks: List<SubTaskItem> = emptyList()
    ) = TaskItem(
        id = id,
        list = TaskList.None,
        name = name,
        description = description,
        subtasks = subtasks,
        status = TaskStatus.Open,
        sortOrder = id.toInt(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun subtask(
        id: Long,
        taskId: Long,
        name: String,
        isCompleted: Boolean
    ) = SubTaskItem(
        id = id,
        taskId = taskId,
        name = name,
        isCompleted = isCompleted,
        sortOrder = id.toInt()
    )
}

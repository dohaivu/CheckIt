package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.DailyPlanItemWriteInput
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class ObserveDailyPlansUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(): Flow<List<DailyPlan>> = repository.observeDailyPlans()
}

class BuildDailyPlanMarkdownSummaryUseCase {
    operator fun invoke(
        date: LocalDate,
        plan: DailyPlan?,
        board: TaskBoard
    ): String {
        val tasksById = board.tasksById
        val doneItems = plan
            ?.items
            .orEmpty()
            .filter { it.status == DailyPlanItemStatus.Done }
            .sortedBy { it.startTimeMinutes ?: Int.MAX_VALUE }

        return buildString {
            if (doneItems.isEmpty()) {
                appendLine("No completed daily-plan items.")
                return@buildString
            }

            doneItems.forEachIndexed { index, item ->
                if (index > 0) appendLine()
                val task = item.taskId?.let { tasksById[it] }
                val title = item.titleLine()
                val detailLines = item.summaryDetailLines(task)
                val subtasks = task?.subtasks.orEmpty()
                val hasDetailLines = detailLines.isNotEmpty()
                val hasContinuation = title != null || hasDetailLines || subtasks.isNotEmpty()

                appendLine("- **${item.timeLabel()}**${if (hasContinuation) MarkdownHardBreak else ""}")
                title?.let {
                    appendLine("$it${if (hasDetailLines || subtasks.isNotEmpty()) MarkdownHardBreak else ""}")
                }
                detailLines.forEachIndexed { detailIndex, detail ->
                    val hasNextLine = detailIndex < detailLines.lastIndex
                    appendLine("_${detail}_${if (hasNextLine || subtasks.isNotEmpty()) MarkdownHardBreak else ""}")
                }
                subtasks.forEach { subtask ->
                    appendLine("  - [${if (subtask.isCompleted) "x" else " "}] ${subtask.name.cleanMarkdownLine()}")
                }
            }
        }.trimEnd()
    }
}

class AddTaskToDailyPlanUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(date: LocalDate, task: TaskItem): Long =
        repository.addTaskToDailyPlan(date, task)
}

class AddDailyPlanItemUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(
        date: LocalDate,
        title: String,
        note: String?,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        source: DailyPlanItemSource = DailyPlanItemSource.MyDayTask,
        status: DailyPlanItemStatus = DailyPlanItemStatus.Done,
        tagIds: List<Long> = emptyList()
    ): Long =
        repository.addDailyPlanItem(date, title, note, startTimeMinutes, endTimeMinutes, source, status, tagIds)
}

class UpdateDailyPlanItemTimeUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long, startTimeMinutes: Int?, endTimeMinutes: Int?) =
        repository.updateDailyPlanItemTime(itemId, startTimeMinutes, endTimeMinutes)
}

class UpdateDailyPlanItemStatusUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long, status: DailyPlanItemStatus) =
        repository.updateDailyPlanItemStatus(itemId, status)
}

class UpdateDailyPlanItemUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long, input: DailyPlanItemWriteInput) =
        repository.updateDailyPlanItem(itemId, input)
}

class DeleteDailyPlanItemUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(itemId: Long) = repository.deleteDailyPlanItem(itemId)
}

private fun DailyPlanItem.titleLine(): String? {
    val title = title.cleanMarkdownLine().takeIf { it.isNotBlank() } ?: return null
    val tagLabel = tags
        .mapNotNull { it.name.toMarkdownTag() }
        .joinToString(separator = " ")
        .takeIf { it.isNotBlank() }
    return listOfNotNull(title, tagLabel).joinToString(separator = " ")
}

private fun DailyPlanItem.summaryDetailLines(task: TaskItem?): List<String> {
    val taskDescription = if (source == DailyPlanItemSource.ExistingTask) {
        task?.description
    } else {
        null
    }
    val detail = taskDescription?.takeIf { it.isNotBlank() } ?: note
    return detail?.cleanMarkdownLines().orEmpty()
}

private fun DailyPlanItem.timeLabel(): String {
    val start = startTimeMinutes ?: return "All-Day"
    val end = endTimeMinutes
    return if (end == null) start.toClockLabel() else "${start.toClockLabel()} - ${end.toClockLabel()}"
}

private fun String.toMarkdownTag(): String? {
    val name = trim().removePrefix("#").replace(WhitespaceRegex, "")
    return name.takeIf { it.isNotBlank() }?.let { "#$it" }
}

private fun String.cleanMarkdownLine(): String =
    trim().replace(WhitespaceRegex, " ")

private fun String.cleanMarkdownLines(): List<String> =
    lineSequence()
        .map { it.cleanMarkdownLine() }
        .filter { it.isNotBlank() }
        .toList()

private val WhitespaceRegex = Regex("\\s+")
private const val MarkdownHardBreak = "  "


internal fun Int.toClockLabel(): String {
    val hour = this / 60
    val minute = this % 60
    val suffix = if (hour >= 12) "PM" else "AM"
    val displayHour = when (val normalized = hour % 12) {
        0 -> 12
        else -> normalized
    }
    return "$displayHour:${minute.toString().padStart(2, '0')} $suffix"
}
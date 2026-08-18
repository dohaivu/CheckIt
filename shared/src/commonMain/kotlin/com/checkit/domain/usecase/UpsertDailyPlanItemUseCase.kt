package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.DailyPlanItemWriteInput
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.hasEndTime
import com.checkit.ui.myday.DailyPlanItemEditorState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class UpsertDailyPlanItemUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(editor: DailyPlanItemEditorState): Result<Long> {
        val title = editor.title.trim()
        val note = editor.note.trim()
        val source = editor.saveSource()
        val status = editor.saveStatus()

        val validationError = validate(source, title, note, editor.startTimeMinutes, editor.endTimeMinutes)
        if (validationError != null) {
            return Result.failure(Exception(validationError))
        }

        return runCatching {
            if (editor.itemId == null) {
                repository.addDailyPlanItem(
                    date = editor.date,
                    title = title,
                    note = note.takeIf { it.isNotBlank() },
                    startTimeMinutes = editor.startTimeMinutes,
                    endTimeMinutes = if (source.hasEndTime()) editor.endTimeMinutes else null,
                    source = source,
                    status = status,
                    tagIds = editor.selectedTagIds.toList(),
                    nestedListItemId = editor.nestedListItemId
                )
            } else {
                repository.updateDailyPlanItem(
                    editor.itemId,
                    DailyPlanItemWriteInput(
                        title = title,
                        note = note.takeIf { it.isNotBlank() },
                        source = source,
                        status = status,
                        startTimeMinutes = editor.startTimeMinutes,
                        endTimeMinutes = if (source.hasEndTime()) editor.endTimeMinutes else null,
                        tagIds = editor.selectedTagIds.toList(),
                        nestedListItemId = editor.nestedListItemId
                    )
                )
                editor.itemId
            }
        }
    }

    private fun validate(
        source: DailyPlanItemSource,
        title: String,
        note: String,
        start: Int?,
        end: Int?
    ): String? {
        return when (source) {
            DailyPlanItemSource.MyDayNote -> {
                if (title.isBlank() && note.isBlank()) "Add a note" else null
            }
            DailyPlanItemSource.MyDayReminder -> {
                when {
                    title.isBlank() -> "Add a reminder"
                    start == null -> "Add reminder time"
                    else -> null
                }
            }
            DailyPlanItemSource.MyDayTask -> {
                when {
                    title.isBlank() -> "Add a focus item"
                    start != null && end != null && end <= start -> "End time must be after start"
                    else -> null
                }
            }
            DailyPlanItemSource.ExistingTask -> null
        }
    }

    private fun DailyPlanItemEditorState.saveSource(): DailyPlanItemSource = source

    private fun DailyPlanItemEditorState.saveStatus(): DailyPlanItemStatus =
        if (isAddMode) {
            if (source == DailyPlanItemSource.MyDayNote) DailyPlanItemStatus.Done
            else source.inferredAddStatus(startTimeMinutes)
        } else status

    private fun DailyPlanItemSource.inferredAddStatus(startTimeMinutes: Int?): DailyPlanItemStatus =
        if (infersAddStatusFromStartTime() && startTimeMinutes != null && startTimeMinutes < currentMyDayTimeMinutes()) {
            DailyPlanItemStatus.Done
        } else {
            DailyPlanItemStatus.Planned
        }

    private fun DailyPlanItemSource.infersAddStatusFromStartTime(): Boolean =
        this == DailyPlanItemSource.MyDayTask || this == DailyPlanItemSource.MyDayReminder

    private fun currentMyDayTimeMinutes(): Int {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
        return now.hour * 60 + now.minute
    }
}

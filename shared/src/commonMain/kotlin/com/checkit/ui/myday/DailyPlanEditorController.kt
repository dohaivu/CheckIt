package com.checkit.ui.myday

import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.hasEndTime
import com.checkit.ui.UiEvent
import com.checkit.ui.tasks.EditorMode
import com.checkit.ui.today
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.milliseconds

/** Handles the daily plan item editor: open, edit fields, debounced save, delete, and time updates. */
internal class DailyPlanEditorController(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    private var pendingEditorTextSaveJob: Job? = null

    fun openDailyPlan(
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null,
        date: LocalDate = today()
    ) {
        cancelPendingEditorTextSave()
        state.update {
            it.copy(
                itemEditor = DailyPlanItemEditorState(
                    date = date,
                    source = DailyPlanItemSource.MyDayTask,
                    status = DailyPlanItemStatus.Planned,
                    startTimeMinutes = startTimeMinutes,
                    endTimeMinutes = endTimeMinutes
                )
            )
        }
    }

    fun dismissDailyPlanEditor() {
        flushPendingEditorTextSave()
        state.update { it.copy(itemEditor = null) }
    }

    fun addDailyPlan() {
        val editor = state.uiState.value.itemEditor ?: return
        if (!saveDailyPlan(editor)) return
        state.update { it.copy(itemEditor = null) }
        state.sendEvent(UiEvent.ShowSnackbar("Saved"))
    }

    fun saveDailyPlan(editor: DailyPlanItemEditorState): Boolean {
        scope.launch {
            deps.upsertDailyPlanItem(editor).onFailure { error ->
                state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to save"))
            }
        }
        return true
    }

    fun updateItemTime(item: DailyPlanItem, startTimeMinutes: Int, endTimeMinutes: Int) {
        val nextEndTime = if (item.source.hasEndTime()) endTimeMinutes else null
        scope.launch {
            deps.syncKeyResultFromDailyPlan(itemId = item.id, proposedStartTime = startTimeMinutes, proposedEndTime = nextEndTime)
            deps.updateDailyPlanItemTime(item.id, startTimeMinutes, nextEndTime)
        }
    }

    fun openItemEditor(item: DailyPlanItem, date: LocalDate) {
        cancelPendingEditorTextSave()
        state.update {
            it.copy(
                itemEditor = DailyPlanItemEditorState(
                    mode = EditorMode.Edit,
                    itemId = item.id,
                    taskId = item.taskId,
                    date = date,
                    source = item.source,
                    title = item.title,
                    note = item.note.orEmpty(),
                    status = item.status,
                    startTimeMinutes = item.startTimeMinutes,
                    endTimeMinutes = item.endTimeMinutes,
                    selectedTagIds = item.tags.map { it.id }.toSet()
                )
            )
        }
    }

    fun updateTitle(title: String) = updateItemEditor(saveImmediately = false) { it.copy(title = title) }
    fun updateNote(note: String) = updateItemEditor(saveImmediately = false) { it.copy(note = note) }
    fun updateStatus(isDone: Boolean) = updateItemEditor {
        it.copy(status = if (isDone) DailyPlanItemStatus.Done else DailyPlanItemStatus.Planned)
    }
    fun updateEditorSource(source: DailyPlanItemSource) = updateItemEditor {
        it.copy(
            source = source,
            status = if (it.isAddMode) source.inferredAddStatus(it.startTimeMinutes) else source.defaultStatus(),
            endTimeMinutes = if (source.hasEndTime()) it.endTimeMinutes else null
        )
    }
    fun updateStartTime(timeMinutes: Int?) = updateItemEditor {
        it.copy(
            startTimeMinutes = timeMinutes,
            status = if (it.isAddMode) it.source.inferredAddStatus(timeMinutes) else it.status
        )
    }
    fun updateEndTime(timeMinutes: Int?) = updateItemEditor { it.copy(endTimeMinutes = timeMinutes) }
    fun toggleTag(tagId: Long) = updateItemEditor {
        val newTagIds = if (it.selectedTagIds.contains(tagId)) {
            it.selectedTagIds - tagId
        } else {
            it.selectedTagIds + tagId
        }
        it.copy(selectedTagIds = newTagIds)
    }

    fun deleteDailyPlan() {
        cancelPendingEditorTextSave()
        val itemId = state.uiState.value.itemEditor?.itemId ?: return
        deleteDailyPlanItem(itemId) {
            it.copy(itemEditor = null)
        }
        state.sendEvent(UiEvent.ShowSnackbar("Deleted"))
    }

    fun deleteDailyPlanItem(itemId: Long) {
        deleteDailyPlanItem(itemId) { it }
        state.sendEvent(UiEvent.ShowSnackbar("Removed from My Day"))
    }

    private fun deleteDailyPlanItem(
        itemId: Long,
        updateState: (MyDayUiState) -> MyDayUiState
    ) {
        scope.launch {
            deps.deleteDailyPlanItem(itemId)
            state.update(updateState)
        }
    }

    private fun updateItemEditor(
        saveImmediately: Boolean = true,
        transform: (DailyPlanItemEditorState) -> DailyPlanItemEditorState
    ) {
        var updatedEditor: DailyPlanItemEditorState? = null
        state.update { stateValue ->
            stateValue.itemEditor?.let {
                updatedEditor = transform(it)
                stateValue.copy(itemEditor = updatedEditor)
            } ?: stateValue
        }
        val editor = updatedEditor ?: return
        if (!editor.isEditMode) return
        if (saveImmediately) {
            cancelPendingEditorTextSave()
            saveDailyPlan(editor)
        } else {
            scheduleEditorTextSave()
        }
    }

    private fun scheduleEditorTextSave() {
        pendingEditorTextSaveJob?.cancel()
        pendingEditorTextSaveJob = scope.launch {
            delay(EditorTextSaveDebounceMillis.milliseconds)
            pendingEditorTextSaveJob = null
            saveCurrentEditor()
        }
    }

    private fun flushPendingEditorTextSave() {
        val pendingSave = pendingEditorTextSaveJob ?: return
        pendingSave.cancel()
        pendingEditorTextSaveJob = null
        saveCurrentEditor()
    }

    private fun cancelPendingEditorTextSave() {
        pendingEditorTextSaveJob?.cancel()
        pendingEditorTextSaveJob = null
    }

    private fun saveCurrentEditor() {
        val editor = state.uiState.value.itemEditor?.takeIf { it.isEditMode } ?: return
        saveDailyPlan(editor)
    }

    private companion object {
        const val EditorTextSaveDebounceMillis = 600L
    }
}

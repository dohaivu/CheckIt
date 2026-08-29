package com.checkit.ui.myday

import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DefaultTaskDurationMinutes
import com.checkit.domain.hasEndTime
import com.checkit.domain.nextAvailableTimeRange
import com.checkit.ui.UiEvent
import com.checkit.ui.currentMyDayTimeMinutes
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
    fun openDailyPlan(title: String, tagIds: List<Long>, nestedListItemId: Long? = null) {
        if (title.isBlank()) return
        cancelPendingEditorTextSave()
        val current = state.uiState.value

        val (startTimeMinutes, endTimeMinutes) = if (current.suggestionStartTimeMinutes == null) {
            nextAvailableTimeRange(currentMyDayTimeMinutes(), DefaultTaskDurationMinutes, current.items)
        } else {
            current.suggestionStartTimeMinutes to current.suggestionEndTimeMinutes
        }

        state.update {
            it.copy(
                itemEditor = DailyPlanItemEditorState(
                    mode = EditorMode.Add,
                    date = today(),
                    title = title,
                    nestedListItemId = nestedListItemId,
                    source = DailyPlanItemSource.MyDayTask,
                    status = DailyPlanItemStatus.Planned,
                    startTimeMinutes = startTimeMinutes,
                    endTimeMinutes = endTimeMinutes,
                    selectedTagIds = tagIds.toSet()
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
        // Wait for upsert to finish before dismissing; optimistic close after validation passed
        scope.launch {
            val result = deps.upsertDailyPlanItem(editor.clearError())
            if (result.isSuccess) {
                if (editor.itemId == null) editor.label?.let { deps.settingsRepository.addRecentLabel(it) }
                state.update { it.copy(itemEditor = null) }
                state.sendEvent(UiEvent.ShowSnackbar("Saved"))
            } else {
                val message = result.exceptionOrNull()?.message ?: "Unable to save"
                state.update { s ->
                    val current = s.itemEditor ?: return@update s
                    s.copy(itemEditor = current.copy(error = message))
                }
            }
        }
    }

    fun saveDailyPlan(editor: DailyPlanItemEditorState): Boolean {
        val validationError = validate(editor)
        if (validationError != null) {
            state.update { s ->
                val current = s.itemEditor ?: return@update s
                // Only update if same editor (by itemId/mode) to avoid clobbering newer edits
                if (current.itemId != editor.itemId || current.mode != editor.mode) return@update s
                s.copy(itemEditor = current.copy(error = validationError))
            }
            return false
        }
        // Clear previous error if now valid
        if (editor.error != null) {
            state.update { s ->
                val current = s.itemEditor ?: return@update s
                if (current.itemId != editor.itemId || current.mode != editor.mode) return@update s
                s.copy(itemEditor = current.copy(error = null))
            }
        }
        // For Add mode, the caller (addDailyPlan) handles the actual upsert; for Edit mode we persist here
        if (editor.isEditMode) {
            scope.launch {
                if (editor.itemId == null) editor.label?.let { deps.settingsRepository.addRecentLabel(it) }
                deps.upsertDailyPlanItem(editor.clearError()).onFailure { error ->
                    val message = error.message ?: "Unable to save"
                    state.update { s ->
                        val current = s.itemEditor ?: return@update s
                        s.copy(itemEditor = current.copy(error = message))
                    }
                }
            }
        }
        return true
    }

    private fun DailyPlanItemEditorState.clearError(): DailyPlanItemEditorState =
        if (error == null) this else copy(error = null)

    private fun validate(editor: DailyPlanItemEditorState): String? {
        val title = editor.title.trim()
        val note = editor.note.trim()
        return when (editor.source) {
            DailyPlanItemSource.MyDayNote -> {
                if (title.isBlank() && note.isBlank()) "Add a note" else null
            }
            DailyPlanItemSource.MyDayReminder -> {
                when {
                    title.isBlank() -> "Add a reminder"
                    editor.startTimeMinutes == null -> "Add reminder time"
                    else -> null
                }
            }
            DailyPlanItemSource.MyDayTask -> {
                when {
                    title.isBlank() -> "Add a title"
                    editor.startTimeMinutes != null && editor.endTimeMinutes != null && editor.endTimeMinutes <= editor.startTimeMinutes -> "End time must be after start"
                    else -> null
                }
            }
            DailyPlanItemSource.ExistingTask -> null
        }
    }

    fun updateItemTime(item: DailyPlanItem, startTimeMinutes: Int, endTimeMinutes: Int) {
        val nextEndTime = if (item.source.hasEndTime()) endTimeMinutes else null
        scope.launch {
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
                    nestedListItemId = item.nestedListItemId,
                    date = date,
                    source = item.source,
                    title = item.title,
                    note = item.note.orEmpty(),
                    status = item.status,
                    label = item.label,
                    startTimeMinutes = item.startTimeMinutes,
                    endTimeMinutes = item.endTimeMinutes,
                    selectedTagIds = item.tags.map { it.id }.toSet()
                )
            )
        }
    }
    fun updateTitle(title: String) = updateItemEditor(saveImmediately = false) { it.copy(title = title, error = null) }
    fun updateNote(note: String) = updateItemEditor(saveImmediately = false) { it.copy(note = note, error = null) }
    fun updateLabel(label: String) = updateItemEditor(saveImmediately = false) { it.copy(label = label, error = null) }

    fun duplicateDailyPlanItem() {
        val current = state.uiState.value
        val editor = current.itemEditor ?: return
        cancelPendingEditorTextSave()

        val planItems = current.dailyPlans.firstOrNull { it.date == editor.date }?.items.orEmpty()
        val durationMinutes = editor.durationMinutes() ?: DefaultTaskDurationMinutes
        val (startTimeMinutes, endTimeMinutes) =
            nextAvailableTimeRange(currentMyDayTimeMinutes(), durationMinutes, planItems)

        state.update {
            it.copy(
                itemEditor = DailyPlanItemEditorState(
                    mode = EditorMode.Add,
                    taskId = editor.taskId,
                    nestedListItemId = editor.nestedListItemId,
                    date = editor.date,
                    source = editor.source,
                    title = editor.title,
                    note = editor.note,
                    label = editor.label,
                    status = if (editor.source == DailyPlanItemSource.MyDayNote) {
                        DailyPlanItemStatus.Done
                    } else {
                        editor.source.inferredAddStatus(startTimeMinutes)
                    },
                    startTimeMinutes = startTimeMinutes,
                    endTimeMinutes = endTimeMinutes,
                    selectedTagIds = editor.selectedTagIds
                )
            )
        }
    }

    fun updateStatus(isDone: Boolean) = updateItemEditor {
        it.copy(status = if (isDone) DailyPlanItemStatus.Done else DailyPlanItemStatus.Planned, error = null)
    }
    fun updateEditorSource(source: DailyPlanItemSource) = updateItemEditor {
        it.copy(
            source = source,
            status = if (it.isAddMode) source.inferredAddStatus(it.startTimeMinutes) else source.defaultStatus(),
            endTimeMinutes = if (source.hasEndTime()) it.endTimeMinutes else null,
            error = null
        )
    }
    fun updateDate(date: LocalDate?) = updateItemEditor {
        it.copy(
            date = date ?: today(),
            error = null
        )
    }
    fun updateTime(startTimeMinutes: Int?, endTimeMinutes: Int?) = updateItemEditor {
        it.copy(
            startTimeMinutes = startTimeMinutes,
            endTimeMinutes = endTimeMinutes,
            status = if (it.isAddMode) it.source.inferredAddStatus(startTimeMinutes) else it.status,
            error = null
        )
    }
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

    private fun DailyPlanItemEditorState.durationMinutes(): Int? {
        val start = startTimeMinutes ?: return null
        val end = endTimeMinutes ?: return null
        return (end - start).takeIf { it > 0 }
    }

    private companion object {
        const val EditorTextSaveDebounceMillis = 600L
    }
}

package com.checkit.ui.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.DailyPlanItemWriteInput
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.TaskItem
import com.checkit.domain.usecase.AddManualDoneToDailyPlanUseCase
import com.checkit.domain.usecase.AddTaskToDailyPlanUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.DeleteDailyPlanItemUseCase
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.ui.DailyPlanItemEditorState
import com.checkit.ui.EditorMode
import com.checkit.ui.MyDayUiState
import com.checkit.ui.MyDayView
import com.checkit.ui.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyDayViewModel(
    private val observeTaskBoard: ObserveTaskBoardUseCase,
    private val observeDailyPlans: ObserveDailyPlansUseCase,
    private val ensureDefaultTaskData: EnsureDefaultTaskDataUseCase,
    private val addTaskToDailyPlan: AddTaskToDailyPlanUseCase,
    private val addManualDoneToDailyPlan: AddManualDoneToDailyPlanUseCase,
    private val updateDailyPlanItemTime: UpdateDailyPlanItemTimeUseCase,
    private val updateDailyPlanItem: UpdateDailyPlanItemUseCase,
    private val deleteDailyPlanItem: DeleteDailyPlanItemUseCase,
    private val completeTask: CompleteTaskUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyDayUiState())
    val uiState: StateFlow<MyDayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ensureDefaultTaskData()
            combine(observeTaskBoard(), observeDailyPlans()) { board, dailyPlans ->
                board to dailyPlans
            }
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false, message = error.message ?: "Unable to load My Day")
                    }
                }
                .collect { (board, dailyPlans) ->
                    _uiState.update { it.copy(board = board, dailyPlans = dailyPlans, isLoading = false) }
                }
        }
    }

    fun selectView(view: MyDayView) {
        _uiState.update { it.copy(selectedView = view) }
    }

    fun addTask(task: TaskItem) {
        viewModelScope.launch {
            addTaskToDailyPlan(today(), task)
            _uiState.update { it.copy(showSuggestions = false) }
        }
    }

    fun updateItemTime(item: DailyPlanItem, startTimeMinutes: Int, endTimeMinutes: Int) {
        viewModelScope.launch {
            updateDailyPlanItemTime(item.id, startTimeMinutes, endTimeMinutes)
        }
    }

    fun openCheckIn() {
        _uiState.update { it.copy(itemEditor = DailyPlanItemEditorState()) }
    }

    fun openItemEditor(item: DailyPlanItem) {
        _uiState.update {
            it.copy(
                itemEditor = DailyPlanItemEditorState(
                    mode = EditorMode.View,
                    itemId = item.id,
                    taskId = item.taskId,
                    title = if (item.source == com.checkit.domain.DailyPlanItemSource.CheckInNote) {
                        item.note.orEmpty()
                    } else {
                        item.titleSnapshot
                    },
                    note = if (item.source == com.checkit.domain.DailyPlanItemSource.CheckInNote) "" else item.note.orEmpty(),
                    status = item.status,
                    startTimeMinutes = item.startTimeMinutes,
                    endTimeMinutes = item.endTimeMinutes
                )
            )
        }
    }

    fun openSuggestions() {
        _uiState.update { it.copy(showSuggestions = true) }
    }

    fun dismissSuggestions() {
        _uiState.update { it.copy(showSuggestions = false) }
    }

    fun dismissCheckIn() {
        _uiState.update { it.copy(itemEditor = null) }
    }

    fun editItemEditor() = updateItemEditor { it.copy(mode = EditorMode.Edit) }
    fun updateDoneTitle(title: String) = updateItemEditor { it.copy(title = title) }
    fun updateDoneNote(note: String) = updateItemEditor { it.copy(note = note) }
    fun updateStartTime(timeMinutes: Int?) = updateItemEditor { it.copy(startTimeMinutes = timeMinutes) }
    fun updateEndTime(timeMinutes: Int?) = updateItemEditor { it.copy(endTimeMinutes = timeMinutes) }

    fun saveCheckIn() {
        val editor = _uiState.value.itemEditor ?: return
        val title = editor.title.trim()
        val note = editor.note.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(message = "Add a done item") }
            return
        }
        viewModelScope.launch {
            if (editor.itemId == null) {
                addManualDoneToDailyPlan(
                    today(),
                    title,
                    note.takeIf { it.isNotBlank() },
                    editor.startTimeMinutes,
                    editor.endTimeMinutes
                )
            } else {
                updateDailyPlanItem(
                    editor.itemId,
                    editor.toWriteInput(editor.status)
                )
            }
            _uiState.update { it.copy(itemEditor = null, message = "Saved") }
        }
    }

    fun markEditorDone() {
        val editor = _uiState.value.itemEditor ?: return
        val title = editor.title.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(message = "Add a done item") }
            return
        }
        viewModelScope.launch {
            if (editor.itemId == null) {
                addManualDoneToDailyPlan(
                    today(),
                    title,
                    editor.note.trim().takeIf { it.isNotBlank() },
                    editor.startTimeMinutes,
                    editor.endTimeMinutes
                )
            } else {
                updateDailyPlanItem(editor.itemId, editor.toWriteInput(DailyPlanItemStatus.Done))
            }
            editor.taskId?.let { completeTask(it) }
            _uiState.update { it.copy(itemEditor = null, message = "Done") }
        }
    }

    fun deleteEditorItem() {
        val itemId = _uiState.value.itemEditor?.itemId ?: return
        viewModelScope.launch {
            deleteDailyPlanItem(itemId)
            _uiState.update { it.copy(itemEditor = null, message = "Deleted") }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun updateItemEditor(transform: (DailyPlanItemEditorState) -> DailyPlanItemEditorState) {
        _uiState.update { state ->
            state.itemEditor?.let { state.copy(itemEditor = transform(it)) } ?: state
        }
    }
}

private fun DailyPlanItemEditorState.toWriteInput(status: DailyPlanItemStatus) = DailyPlanItemWriteInput(
    title = title,
    note = note,
    status = status,
    startTimeMinutes = startTimeMinutes,
    endTimeMinutes = endTimeMinutes
)

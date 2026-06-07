package com.checkit.ui.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.DailyPlanItemWriteInput
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

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
            val state = _uiState.value
            val (startTimeMinutes, endTimeMinutes) = state.selectedSuggestionTimeRangeFor(task)
            val itemId = addTaskToDailyPlan(today(), task)
            if (startTimeMinutes != task.startTimeMinutes || endTimeMinutes != task.endTimeMinutes) {
                updateDailyPlanItemTime(itemId, startTimeMinutes, endTimeMinutes)
            }
            _uiState.update {
                it.copy(
                    showSuggestions = false,
                    suggestionStartTimeMinutes = null,
                    suggestionEndTimeMinutes = null
                )
            }
        }
    }

    fun updateItemTime(item: DailyPlanItem, startTimeMinutes: Int, endTimeMinutes: Int) {
        viewModelScope.launch {
            updateDailyPlanItemTime(item.id, startTimeMinutes, endTimeMinutes)
        }
    }

    fun openCheckIn(
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null
    ) {
        _uiState.update {
            it.copy(
                itemEditor = DailyPlanItemEditorState(
                    startTimeMinutes = startTimeMinutes,
                    endTimeMinutes = endTimeMinutes
                )
            )
        }
    }

    fun openItemEditor(item: DailyPlanItem) {
        _uiState.update {
            it.copy(
                itemEditor = DailyPlanItemEditorState(
                    mode = EditorMode.View,
                    itemId = item.id,
                    taskId = item.taskId,
                    source = item.source,
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

    fun openSuggestions(
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null
    ) {
        _uiState.update {
            it.copy(
                showSuggestions = true,
                suggestionStartTimeMinutes = startTimeMinutes,
                suggestionEndTimeMinutes = endTimeMinutes
            )
        }
    }

    fun createFromTimelineRange(startTimeMinutes: Int, endTimeMinutes: Int) {
        if (startTimeMinutes < currentMyDayTimeMinutes()) {
            openCheckIn(startTimeMinutes, endTimeMinutes)
        } else {
            openSuggestions(startTimeMinutes, endTimeMinutes)
        }
    }

    fun dismissSuggestions() {
        _uiState.update {
            it.copy(
                showSuggestions = false,
                suggestionStartTimeMinutes = null,
                suggestionEndTimeMinutes = null
            )
        }
    }

    fun dismissCheckIn() {
        _uiState.update { it.copy(itemEditor = null) }
    }

    fun editItemEditor() = updateItemEditor { it.copy(mode = EditorMode.Edit) }
    fun updateDoneTitle(title: String) = updateItemEditor { it.copy(title = title) }
    fun updateDoneNote(note: String) = updateItemEditor { it.copy(note = note) }
    fun updateEditorSource(source: DailyPlanItemSource) = updateItemEditor {
        it.copy(
            source = source,
            status = DailyPlanItemStatus.Done,
            endTimeMinutes = if (source == DailyPlanItemSource.CheckInNote) null else it.endTimeMinutes
        )
    }
    fun updateStartTime(timeMinutes: Int?) = updateItemEditor { it.copy(startTimeMinutes = timeMinutes) }
    fun updateEndTime(timeMinutes: Int?) = updateItemEditor { it.copy(endTimeMinutes = timeMinutes) }

    fun saveCheckIn() {
        val editor = _uiState.value.itemEditor ?: return
        val title = editor.title.trim()
        val note = editor.note.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(message = if (editor.source == DailyPlanItemSource.CheckInNote) "Add a note" else "Add a done item") }
            return
        }
        viewModelScope.launch {
            if (editor.itemId == null) {
                addManualDoneToDailyPlan(
                    today(),
                    title,
                    note.takeIf { it.isNotBlank() },
                    editor.startTimeMinutes,
                    editor.endTimeMinutes,
                    editor.source
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
            _uiState.update { it.copy(message = if (editor.source == DailyPlanItemSource.CheckInNote) "Add a note" else "Add a done item") }
            return
        }
        viewModelScope.launch {
            if (editor.itemId == null) {
                addManualDoneToDailyPlan(
                    today(),
                    title,
                    editor.note.trim().takeIf { it.isNotBlank() },
                    editor.startTimeMinutes,
                    editor.endTimeMinutes,
                    DailyPlanItemSource.CheckInManualDone
                )
            } else {
                updateDailyPlanItem(
                    editor.itemId,
                    editor.toWriteInput(status = DailyPlanItemStatus.Done)
                )
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

private fun MyDayUiState.selectedSuggestionTimeRangeFor(task: TaskItem): Pair<Int?, Int?> {
    val selectedStart = suggestionStartTimeMinutes
    if (selectedStart != null) {
        return selectedStart to suggestionEndTimeMinutes
    }

    val start = task.startTimeMinutes
        ?: return currentMyDayTimeMinutes().let { now ->
            now to (now + DefaultTaskDurationMinutes).coerceAtMost(MyDayMinutesPerDay)
        }
    val end = task.endTimeMinutes
    val now = currentMyDayTimeMinutes()
    return if (start < now) {
        now to (now + DefaultTaskDurationMinutes).coerceAtMost(MyDayMinutesPerDay)
    } else {
        start to end
    }
}

private fun DailyPlanItemEditorState.toWriteInput(
    status: DailyPlanItemStatus,
    source: DailyPlanItemSource = this.source
) = DailyPlanItemWriteInput(
    title = title,
    note = note,
    source = source,
    status = status,
    startTimeMinutes = startTimeMinutes,
    endTimeMinutes = endTimeMinutes
)

private fun currentMyDayTimeMinutes(): Int {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    return now.hour * 60 + now.minute
}

private const val DefaultTaskDurationMinutes = 60
private const val MyDayMinutesPerDay = 24 * 60

package com.checkit.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.DailyPlanItemWriteInput
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.usecase.AddManualDoneToDailyPlanUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.DeleteDailyPlanItemUseCase
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemUseCase
import com.checkit.ui.DailyPlanItemEditorState
import com.checkit.ui.CalendarUiState
import com.checkit.ui.EditorMode
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.firstDayOfMonth
import com.checkit.ui.isSameMonth
import com.checkit.ui.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class CalendarViewModel(
    private val observeTaskBoard: ObserveTaskBoardUseCase,
    private val observeDailyPlans: ObserveDailyPlansUseCase,
    private val ensureDefaultTaskData: EnsureDefaultTaskDataUseCase,
    private val addManualDoneToDailyPlan: AddManualDoneToDailyPlanUseCase,
    private val updateDailyPlanItem: UpdateDailyPlanItemUseCase,
    private val deleteDailyPlanItem: DeleteDailyPlanItemUseCase,
    private val completeTask: CompleteTaskUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ensureDefaultTaskData()
            combine(observeTaskBoard(), observeDailyPlans()) { board, dailyPlans ->
                board to dailyPlans
            }
                .catch { error ->
                    _uiState.update { it.copy() }
                }
                .collect { (board, dailyPlans) ->
                    _uiState.update { it.copy(board = board, dailyPlans = dailyPlans) }
                }
        }
    }

    fun selectMonth(month: LocalDate) {
        _uiState.update {  it.copy(selectedMonth = month.firstDayOfMonth()) }
    }

    fun previousMonth() {
        _uiState.update { it.copy(selectedMonth = it.selectedMonth.minus(1, DateTimeUnit.MONTH)) }
    }

    fun nextMonth() {
        _uiState.update { it.copy(selectedMonth = it.selectedMonth.plus(1, DateTimeUnit.MONTH)) }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun resetToToday() {
        val today = today()
        _uiState.update {
            it.copy(
                selectedMonth = today.firstDayOfMonth(),
                selectedDate = today
            )
        }
    }

    fun openItemEditor(item: DailyPlanItem?) {
        if (item != null) {
            _uiState.update {
                it.copy(
                    itemEditor = DailyPlanItemEditorState(
                        mode = EditorMode.View,
                        itemId = item.id,
                        taskId = item.taskId,
                        source = item.source,
                        title = if (item.source == DailyPlanItemSource.CheckInNote) item.note.orEmpty() else item.titleSnapshot,
                        note = if (item.source == DailyPlanItemSource.CheckInNote) "" else item.note.orEmpty(),
                        status = item.status,
                        startTimeMinutes = item.startTimeMinutes,
                        endTimeMinutes = item.endTimeMinutes
                    )
                )
            }
        } else {
            _uiState.update { it.copy(itemEditor = DailyPlanItemEditorState()) }
        }
    }

    fun dismissItemEditor() {
        _uiState.update { it.copy(itemEditor = null) }
    }

    fun editItemEditor() = updateItemEditor { it.copy(mode = EditorMode.Edit) }
    fun updateEditorTitle(title: String) = updateItemEditor { it.copy(title = title) }
    fun updateEditorNote(note: String) = updateItemEditor { it.copy(note = note) }
    fun updateEditorSource(source: DailyPlanItemSource) = updateItemEditor {
        it.copy(
            source = source,
            status = DailyPlanItemStatus.Done,
            endTimeMinutes = if (source == DailyPlanItemSource.CheckInNote) null else it.endTimeMinutes
        )
    }
    fun updateEditorStartTime(timeMinutes: Int?) = updateItemEditor { it.copy(startTimeMinutes = timeMinutes) }
    fun updateEditorEndTime(timeMinutes: Int?) = updateItemEditor { it.copy(endTimeMinutes = timeMinutes) }

    fun updateTags(tagIds: Set<Long>) = updateItemEditor { it.copy(selectedTagIds = tagIds) }
    fun toggleTag(tagId: Long) = updateItemEditor {
        val newTagIds = if (it.selectedTagIds.contains(tagId)) {
            it.selectedTagIds - tagId
        } else {
            it.selectedTagIds + tagId
        }
        it.copy(selectedTagIds = newTagIds)
    }

    fun saveEditorItem() {
        val editor = _uiState.value.itemEditor ?: return
        val title = editor.title.trim()
        val note = editor.note.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            if (editor.itemId == null) {
                addManualDoneToDailyPlan(
                    _uiState.value.selectedDate,
                    title,
                    note.takeIf { it.isNotBlank() },
                    editor.startTimeMinutes,
                    editor.endTimeMinutes,
                    editor.source
                )
            } else {
                updateDailyPlanItem(editor.itemId, editor.toWriteInput(editor.status))
            }
            _uiState.update { it.copy(itemEditor = null) }
        }
    }

    fun markEditorDone() {
        val editor = _uiState.value.itemEditor ?: return
        val itemId = editor.itemId ?: return
        viewModelScope.launch {
            updateDailyPlanItem(
                itemId,
                editor.toWriteInput(
                    status = DailyPlanItemStatus.Done,
                )
            )
            editor.taskId?.let { completeTask(it) }
            _uiState.update { it.copy(itemEditor = null) }
        }
    }

    fun deleteEditorItem() {
        val itemId = _uiState.value.itemEditor?.itemId ?: return
        viewModelScope.launch {
            deleteDailyPlanItem(itemId)
            _uiState.update { it.copy(itemEditor = null) }
        }
    }

    private fun updateItemEditor(transform: (DailyPlanItemEditorState) -> DailyPlanItemEditorState) {
        _uiState.update { state ->
            state.itemEditor?.let { state.copy(itemEditor = transform(it)) } ?: state
        }
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
    endTimeMinutes = endTimeMinutes,
    tagIds = selectedTagIds.toList()
)

private fun LocalDate.matchesPeriod(period: ReportPeriod, month: LocalDate): Boolean =
    when (period) {
        ReportPeriod.Month -> isSameMonth(month)
        ReportPeriod.Annual -> year == month.year
    }

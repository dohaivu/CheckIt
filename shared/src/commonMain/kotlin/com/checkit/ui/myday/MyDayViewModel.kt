package com.checkit.ui.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.TaskItem
import com.checkit.domain.usecase.AddManualDoneToDailyPlanUseCase
import com.checkit.domain.usecase.AddNoteToDailyPlanUseCase
import com.checkit.domain.usecase.AddTaskToDailyPlanUseCase
import com.checkit.domain.usecase.CompleteTaskUseCase
import com.checkit.domain.usecase.EnsureDefaultTaskDataUseCase
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemStatusUseCase
import com.checkit.domain.usecase.UpdateDailyPlanItemTimeUseCase
import com.checkit.ui.CheckInState
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
    private val addNoteToDailyPlan: AddNoteToDailyPlanUseCase,
    private val updateDailyPlanItemStatus: UpdateDailyPlanItemStatusUseCase,
    private val updateDailyPlanItemTime: UpdateDailyPlanItemTimeUseCase,
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

    fun markDone(item: DailyPlanItem) {
        viewModelScope.launch {
            updateDailyPlanItemStatus(item.id, DailyPlanItemStatus.Done)
            item.taskId?.let { completeTask(it) }
        }
    }

    fun markPlanned(item: DailyPlanItem) {
        viewModelScope.launch {
            updateDailyPlanItemStatus(item.id, DailyPlanItemStatus.Planned)
        }
    }

    fun openCheckIn() {
        _uiState.update { it.copy(checkIn = CheckInState()) }
    }

    fun openSuggestions() {
        _uiState.update { it.copy(showSuggestions = true) }
    }

    fun dismissSuggestions() {
        _uiState.update { it.copy(showSuggestions = false) }
    }

    fun dismissCheckIn() {
        _uiState.update { it.copy(checkIn = null) }
    }

    fun updateDoneTitle(title: String) = updateCheckIn { it.copy(doneTitle = title) }
    fun updateDoneNote(note: String) = updateCheckIn { it.copy(doneNote = note) }
    fun updateStatusNote(note: String) = updateCheckIn { it.copy(statusNote = note) }
    fun updateStartTime(timeMinutes: Int?) = updateCheckIn { it.copy(startTimeMinutes = timeMinutes) }
    fun updateEndTime(timeMinutes: Int?) = updateCheckIn { it.copy(endTimeMinutes = timeMinutes) }

    fun saveCheckIn() {
        val checkIn = _uiState.value.checkIn ?: return
        val doneTitle = checkIn.doneTitle.trim()
        val doneNote = checkIn.doneNote.trim()
        val statusNote = checkIn.statusNote.trim()
        if (doneTitle.isBlank() && statusNote.isBlank()) {
            _uiState.update { it.copy(message = "Add a done item or note") }
            return
        }
        viewModelScope.launch {
            if (doneTitle.isNotBlank()) {
                addManualDoneToDailyPlan(
                    today(),
                    doneTitle,
                    doneNote.takeIf { it.isNotBlank() },
                    checkIn.startTimeMinutes,
                    checkIn.endTimeMinutes
                )
            }
            if (statusNote.isNotBlank()) {
                addNoteToDailyPlan(today(), statusNote)
            }
            _uiState.update { it.copy(checkIn = null, message = "CheckIn saved") }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun updateCheckIn(transform: (CheckInState) -> CheckInState) {
        _uiState.update { state ->
            state.checkIn?.let { state.copy(checkIn = transform(it)) } ?: state
        }
    }
}

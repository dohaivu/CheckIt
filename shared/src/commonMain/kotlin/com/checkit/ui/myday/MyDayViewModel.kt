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
import kotlinx.datetime.LocalDate
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
    private val deleteDailyPlanItemUseCase: DeleteDailyPlanItemUseCase
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

    fun updateItemTime(item: DailyPlanItem, startTimeMinutes: Int, endTimeMinutes: Int) {
        viewModelScope.launch {
            updateDailyPlanItemTime(item.id, startTimeMinutes, if (item.source == DailyPlanItemSource.CheckInNote) null else endTimeMinutes)
        }
    }

    fun openCheckIn(
        startTimeMinutes: Int? = null,
        endTimeMinutes: Int? = null,
        date: LocalDate = today()
    ) {
        _uiState.update {
            it.copy(
                itemEditor = DailyPlanItemEditorState(
                    date = date,
                    startTimeMinutes = startTimeMinutes,
                    endTimeMinutes = endTimeMinutes
                )
            )
        }
    }
    fun dismissCheckIn() {
        _uiState.update { it.copy(itemEditor = null) }
    }

    fun addCheckIn() {
        val editor = _uiState.value.itemEditor ?: return

        if (!saveCheckIn(editor)) return

        _uiState.update { it.copy(itemEditor = null, message = "Saved") }
    }

    fun saveCheckIn(editor: DailyPlanItemEditorState): Boolean {
        val title = editor.title.trim()
        val note = editor.note.trim()
        when (editor.source) {
            DailyPlanItemSource.CheckInNote -> {
                if (title.isBlank() && note.isBlank()) {
                    _uiState.update { it.copy(message = "Add a note") }
                    return false
                }
            }
            DailyPlanItemSource.CheckInManualDone -> {
                val start = editor.startTimeMinutes
                val end = editor.endTimeMinutes
                when {
                    title.isBlank() -> {
                        _uiState.update { it.copy(message = "Add a done item") }
                        return false
                    }
                    start == null || end == null -> {
                        _uiState.update { it.copy(message = "Add start and end time") }
                        return false
                    }
                    end <= start -> {
                        _uiState.update { it.copy(message = "End time must be after start") }
                        return false
                    }
                }
            }
            DailyPlanItemSource.ExistingTask -> Unit
        }
        viewModelScope.launch {
            if (editor.itemId == null) {
                addManualDoneToDailyPlan(
                    editor.date,
                    title,
                    note.takeIf { it.isNotBlank() },
                    editor.startTimeMinutes,
                    editor.endTimeMinutes,
                    editor.source,
                    tagIds = editor.selectedTagIds.toList()
                )
            } else {
                updateDailyPlanItem(
                    editor.itemId,
                    editor.toWriteInput(editor.status)
                )
            }
        }
        return true
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
    fun dismissSuggestions() {
        _uiState.update {
            it.copy(
                showSuggestions = false,
                suggestionStartTimeMinutes = null,
                suggestionEndTimeMinutes = null
            )
        }
    }
    fun addTaskFromSuggestion(task: TaskItem) {
        addTaskToMyDay(task, clearSuggestions = true)
    }

    fun addTaskToMyDay(task: TaskItem) {
        addTaskToMyDay(task, clearSuggestions = false)
    }

    private fun addTaskToMyDay(
        task: TaskItem,
        clearSuggestions: Boolean
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val (startTimeMinutes, endTimeMinutes) = state.selectedSuggestionTimeRangeFor(task)
            val itemId = addTaskToDailyPlan(today(), task)
            if (startTimeMinutes != task.startTimeMinutes || endTimeMinutes != task.endTimeMinutes) {
                updateDailyPlanItemTime(itemId, startTimeMinutes, endTimeMinutes)
            }
            _uiState.update { current ->
                if (clearSuggestions) {
                    current.copy(
                        showSuggestions = false,
                        suggestionStartTimeMinutes = null,
                        suggestionEndTimeMinutes = null,
                        message = "Added to My Day"
                    )
                } else {
                    current.copy(message = "Added to My Day")
                }
            }
        }
    }

    fun createFromTimelineRange(startTimeMinutes: Int, endTimeMinutes: Int) {
        if (startTimeMinutes < currentMyDayTimeMinutes()) {
            openCheckIn(startTimeMinutes, endTimeMinutes)
        } else {
            openSuggestions(startTimeMinutes, endTimeMinutes)
        }
    }

    fun openItemEditor(item: DailyPlanItem, date: LocalDate) {
        _uiState.update {
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
    fun toggleTag(tagId: Long) = updateItemEditor {
        val newTagIds = if (it.selectedTagIds.contains(tagId)) {
            it.selectedTagIds - tagId
        } else {
            it.selectedTagIds + tagId
        }
        it.copy(selectedTagIds = newTagIds)
    }

    fun deleteEditorItem() {
        val itemId = _uiState.value.itemEditor?.itemId ?: return
        deleteDailyPlanItem(itemId) {
            it.copy(itemEditor = null, message = "Deleted")
        }
    }

    fun deleteDailyPlanItem(itemId: Long) {
        deleteDailyPlanItem(itemId) {
            it.copy(message = "Removed from My Day")
        }
    }

    private fun deleteDailyPlanItem(
        itemId: Long,
        updateState: (MyDayUiState) -> MyDayUiState
    ) {
        viewModelScope.launch {
            deleteDailyPlanItemUseCase(itemId)
            _uiState.update(updateState)
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun updateItemEditor(transform: (DailyPlanItemEditorState) -> DailyPlanItemEditorState) {
        _uiState.update { state ->
            state.itemEditor?.let {
                val updatedEditor = transform(it)
                if (updatedEditor.isEditMode) saveCheckIn(updatedEditor)
                state.copy(itemEditor = updatedEditor)
            } ?: state
        }
    }
}

private fun MyDayUiState.selectedSuggestionTimeRangeFor(task: TaskItem): Pair<Int?, Int?> {
    val selectedDuration = suggestionStartTimeMinutes?.let { selectedStart ->
        suggestionEndTimeMinutes?.let { selectedEnd ->
            (selectedEnd - selectedStart).takeIf { it > 0 }
        }
    }
    val durationMinutes = selectedDuration
        ?: task.durationMinutes()
        ?: DefaultTaskDurationMinutes
    val preferredStart = suggestionStartTimeMinutes ?: task.preferredMyDayStartTime()
    return nextAvailableTimeRange(preferredStart, durationMinutes)
}

private fun TaskItem.preferredMyDayStartTime(): Int {
    val now = currentMyDayTimeMinutes()
    val start = startTimeMinutes
    return if (start == null || start < now) {
        now
    } else {
        start
    }
}

private fun TaskItem.durationMinutes(): Int? {
    val start = startTimeMinutes ?: return null
    val end = endTimeMinutes ?: return null
    return (end - start).takeIf { it > 0 }
}

private fun MyDayUiState.nextAvailableTimeRange(
    preferredStartTimeMinutes: Int,
    durationMinutes: Int
): Pair<Int?, Int?> {
    val duration = durationMinutes.coerceIn(MinimumPlanDurationMinutes, MyDayMinutesPerDay)
    val lastStart = MyDayMinutesPerDay - duration
    val preferredStart = preferredStartTimeMinutes.coerceIn(0, lastStart)
    val occupiedRanges = items
        .mapNotNull { item -> item.occupiedTimeRange() }
        .sortedBy { it.first }

    findAvailableStart(preferredStart, duration, occupiedRanges)?.let { start ->
        return start to start + duration
    }
    findAvailableStart(0, duration, occupiedRanges)?.let { start ->
        return start to start + duration
    }
    return null to null
}

private fun DailyPlanItem.occupiedTimeRange(): Pair<Int, Int>? {
    val start = startTimeMinutes ?: return null
    val end = (endTimeMinutes ?: (start + DefaultTaskDurationMinutes)).coerceAtMost(MyDayMinutesPerDay)
    return if (end > start) start.coerceIn(0, MyDayMinutesPerDay) to end else null
}

private fun findAvailableStart(
    preferredStart: Int,
    durationMinutes: Int,
    occupiedRanges: List<Pair<Int, Int>>
): Int? {
    val lastStart = MyDayMinutesPerDay - durationMinutes
    var candidate = preferredStart.coerceIn(0, lastStart)
    occupiedRanges.forEach { (occupiedStart, occupiedEnd) ->
        if (candidate + durationMinutes <= occupiedStart) return candidate
        if (candidate < occupiedEnd && candidate + durationMinutes > occupiedStart) {
            candidate = occupiedEnd.coerceAtMost(lastStart)
        }
    }
    return candidate.takeIf { candidate + durationMinutes <= MyDayMinutesPerDay && !it.overlapsAny(durationMinutes, occupiedRanges) }
}

private fun Int.overlapsAny(durationMinutes: Int, occupiedRanges: List<Pair<Int, Int>>): Boolean =
    occupiedRanges.any { (occupiedStart, occupiedEnd) ->
        this < occupiedEnd && this + durationMinutes > occupiedStart
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

private fun currentMyDayTimeMinutes(): Int {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    return now.hour * 60 + now.minute
}

private const val DefaultTaskDurationMinutes = 60
private const val MinimumPlanDurationMinutes = 15
private const val MyDayMinutesPerDay = 24 * 60

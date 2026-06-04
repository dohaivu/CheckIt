package com.checkit.ui.tasks

import androidx.lifecycle.ViewModel
import com.checkit.data.CheckItRepository
import com.checkit.ui.TaskUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TaskViewModel(
    repository: CheckItRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

}

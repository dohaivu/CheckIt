package com.checkit.ui.tasks

import androidx.lifecycle.ViewModel
import com.checkit.data.CheckItRepository
import com.checkit.ui.ExpenseUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TaskViewModel(
    repository: CheckItRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

}

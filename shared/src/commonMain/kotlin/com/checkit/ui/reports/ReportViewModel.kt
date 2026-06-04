package com.checkit.ui.reports

import androidx.lifecycle.ViewModel
import com.checkit.data.CheckItRepository
import com.checkit.ui.ReportUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportViewModel(
    repository: CheckItRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()
}

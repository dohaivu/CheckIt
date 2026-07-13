package com.checkit.ui

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
    /** Open the Report tab after finishing day review (optional follow-up). */
    data object OpenReport : UiEvent
}

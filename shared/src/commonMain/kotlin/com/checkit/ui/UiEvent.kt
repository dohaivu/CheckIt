package com.checkit.ui

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
    /** Open the Reflect tab after finishing day review (optional follow-up). */
    data object OpenReflect : UiEvent
}

package com.checkit.ui

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
}

package com.checkit.ui.myday

import com.checkit.ui.UiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Single source of mutable My Day UI state and one-shot events shared by the feature controllers. */
internal class MyDayStateHolder(
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(MyDayUiState())
    val uiState: StateFlow<MyDayUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun update(transform: (MyDayUiState) -> MyDayUiState) {
        _uiState.update(transform)
    }

    fun sendEvent(event: UiEvent) {
        scope.launch { _events.send(event) }
    }
}

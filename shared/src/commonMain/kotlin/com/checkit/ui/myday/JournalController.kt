package com.checkit.ui.myday

import com.checkit.data.JournalEntryWriteInput
import com.checkit.domain.JournalEntry
import com.checkit.ui.UiEvent
import com.checkit.ui.today
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Handles the day journal: quick-capture, filtering, and the entry editor sheet. */
internal class JournalController(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    // Capture bar
    fun updateCaptureContext(value: String) = updateCapture { it.copy(context = value) }
    fun updateCaptureContent(value: String) = updateCapture { it.copy(content = value) }
    fun toggleCaptureMood(mood: String) = updateCapture {
        val next = if (mood in it.selectedMoods) it.selectedMoods - mood else it.selectedMoods + mood
        it.copy(selectedMoods = next)
    }
    fun toggleCaptureTag(tagId: Long) = updateCapture {
        val next = if (tagId in it.selectedTagIds) it.selectedTagIds - tagId else it.selectedTagIds + tagId
        it.copy(selectedTagIds = next)
    }
    fun useContextSuggestion(context: String) = updateCapture { it.copy(context = context) }

    fun submitCapture() {
        val capture = state.uiState.value.journalCapture
        val context = capture.context.trim()
        val content = capture.content.trim()
        if (context.isBlank() && content.isBlank()) return
        scope.launch {
            deps.addJournalEntry(
                JournalEntryWriteInput(
                    date = today(),
                    context = context.takeIf { it.isNotBlank() },
                    content = content,
                    moods = capture.selectedMoods.toList().sorted(),
                    tagIds = capture.selectedTagIds.toList()
                )
            ).onSuccess {
                state.update { current ->
                    current.copy(
                        journalCapture = current.journalCapture.copy(
                            context = "",
                            content = "",
                            selectedMoods = emptySet(),
                            selectedTagIds = emptySet()
                        )
                    )
                }
                state.sendEvent(UiEvent.ShowSnackbar("Saved"))
            }.onFailure { error ->
                state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to save"))
            }
        }
    }

    fun setJournalTagFilter(tagId: Long?) = state.update { it.copy(journalTagFilter = tagId) }

    // Editor sheet
    fun openJournalEditor(entry: JournalEntry) {
        state.update {
            it.copy(
                journalEditor = JournalEntryEditorState(
                    entryId = entry.id,
                    date = LocalDate.fromEpochDays(entry.dateEpochDays),
                    context = entry.context.orEmpty(),
                    content = entry.content,
                    moods = entry.moods,
                    selectedTagIds = entry.tags.map { it.id }.toSet()
                )
            )
        }
    }

    fun dismissJournalEditor() = state.update { it.copy(journalEditor = null) }

    fun updateJournalEditorContext(value: String) = updateEditor { it.copy(context = value) }
    fun updateJournalEditorContent(value: String) = updateEditor { it.copy(content = value) }
    fun toggleJournalEditorMood(mood: String) = updateEditor {
        val next = if (mood in it.moods) it.moods - mood else it.moods + mood
        it.copy(moods = next)
    }
    fun toggleJournalEditorTag(tagId: Long) = updateEditor {
        val next = if (tagId in it.selectedTagIds) it.selectedTagIds - tagId else it.selectedTagIds + tagId
        it.copy(selectedTagIds = next)
    }

    fun saveJournalEditor() {
        val editor = state.uiState.value.journalEditor ?: return
        scope.launch {
            deps.updateJournalEntry(
                editor.entryId ?: return@launch,
                JournalEntryWriteInput(
                    date = editor.date,
                    context = editor.context.trim().takeIf { it.isNotBlank() },
                    content = editor.content.trim(),
                    moods = editor.moods,
                    tagIds = editor.selectedTagIds.toList()
                )
            )
            state.update { it.copy(journalEditor = null) }
            state.sendEvent(UiEvent.ShowSnackbar("Saved"))
        }
    }

    fun deleteJournalEntry(entryId: Long) {
        scope.launch {
            deps.deleteJournalEntry(entryId)
            state.update { it.copy(journalEditor = null) }
            state.sendEvent(UiEvent.ShowSnackbar("Deleted"))
        }
    }

    private fun updateCapture(transform: (JournalCaptureState) -> JournalCaptureState) {
        state.update { it.copy(journalCapture = transform(it.journalCapture)) }
    }

    private fun updateEditor(transform: (JournalEntryEditorState) -> JournalEntryEditorState) {
        state.update { stateValue ->
            stateValue.journalEditor?.let { stateValue.copy(journalEditor = transform(it)) } ?: stateValue
        }
    }
}

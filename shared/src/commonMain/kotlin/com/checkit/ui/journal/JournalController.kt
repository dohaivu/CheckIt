package com.checkit.ui.journal

import com.checkit.data.JournalEntryWriteInput
import com.checkit.domain.JournalEntry
import com.checkit.ui.UiEvent
import com.checkit.ui.myday.JournalEntryEditorState
import com.checkit.ui.myday.MyDayDependencies
import com.checkit.ui.myday.MyDayStateHolder
import com.checkit.ui.today
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Handles the day journal: quick entry editing, the list sheet, and tag filtering. */
internal class JournalController(
    private val deps: MyDayDependencies,
    private val state: MyDayStateHolder,
    private val scope: CoroutineScope
) {
    fun openJournalList(date: LocalDate? = null) = state.update {
        it.copy(showJournalList = true, journalListDate = date)
    }
    fun dismissJournalList() = state.update {
        it.copy(showJournalList = false, journalListDate = null)
    }

    // Editor sheet
    fun openNewJournalEntry(date: LocalDate = today()) {
        state.update {
            it.copy(
                showJournalList = false,
                journalEditor = JournalEntryEditorState(date = date)
            )
        }
    }

    fun openJournalEditor(entry: JournalEntry) {
        state.update {
            it.copy(
                showJournalList = false,
                journalEditor = JournalEntryEditorState(
                    entryId = entry.id,
                    date = LocalDate.fromEpochDays(entry.dateEpochDays),
                    label = entry.label.orEmpty(),
                    content = entry.content,
                    moods = entry.moods,
                    selectedTagIds = entry.tags.map { it.id }.toSet()
                )
            )
        }
    }

    fun dismissJournalEditor() = state.update { it.copy(journalEditor = null) }

    fun updateJournalEditorLabel(value: String) = updateEditor { it.copy(label = value) }
    fun updateJournalEditorContent(value: String) = updateEditor { it.copy(content = value) }
    fun applyJournalLabelPreset(preset: JournalLabelPreset) = updateEditor {
        it.copy(label = preset.type, content = it.content.ifEmpty { preset.template }, prompt = preset.prompt)
    }
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
        val label = editor.label.trim().takeIf { it.isNotBlank() }
        val content = editor.content.trim()
        if (content.isBlank() && label.isNullOrBlank()) {
            state.sendEvent(UiEvent.ShowSnackbar("Add a note"))
            return
        }
        val input = JournalEntryWriteInput(
            date = editor.date,
            label = label,
            content = content,
            moods = editor.moods,
            tagIds = editor.selectedTagIds.toList()
        )
        scope.launch {
            if (editor.isEditMode) {
                deps.updateJournalEntry(editor.entryId ?: return@launch, input).onSuccess {
                    state.update { it.copy(journalEditor = null) }
                    state.sendEvent(UiEvent.ShowSnackbar("Saved"))
                }.onFailure { error ->
                    state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to save"))
                }
            } else {
                deps.addJournalEntry(input).onSuccess {
                    state.update { it.copy(journalEditor = null) }
                    state.sendEvent(UiEvent.ShowSnackbar("Saved"))
                }.onFailure { error ->
                    state.sendEvent(UiEvent.ShowSnackbar(error.message ?: "Unable to save"))
                }
            }
        }
    }

    fun deleteJournalEntry(entryId: Long) {
        scope.launch {
            deps.deleteJournalEntry(entryId)
            state.update { it.copy(journalEditor = null) }
            state.sendEvent(UiEvent.ShowSnackbar("Deleted"))
        }
    }

    private fun updateEditor(transform: (JournalEntryEditorState) -> JournalEntryEditorState) {
        state.update { stateValue ->
            stateValue.journalEditor?.let { stateValue.copy(journalEditor = transform(it)) } ?: stateValue
        }
    }
}
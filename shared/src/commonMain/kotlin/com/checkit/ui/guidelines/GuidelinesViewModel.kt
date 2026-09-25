package com.checkit.ui.guidelines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.GuidelinesDocument
import com.checkit.data.GuidelinesStorage
import com.checkit.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GuidelinesUiState(
    val folderUri: String? = null,
    val folderName: String? = null,
    val documents: List<GuidelinesDocument> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedUri: String? = null,
    val selectedName: String? = null,
    val selectedMarkdown: String? = null,
    val selectedIsLoading: Boolean = false,
    val selectedError: String? = null
) {
    val hasFolder: Boolean get() = folderUri != null
}

class GuidelinesViewModel(
    private val storage: GuidelinesStorage,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GuidelinesUiState())
    val uiState: StateFlow<GuidelinesUiState> = _uiState.asStateFlow()

    private var listJob: Job? = null
    private var readJob: Job? = null
    private var lastFolderUri: String? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.guidelinesFolderUri to it.guidelinesFolderName }
                .distinctUntilChanged()
                .collect { (folderUri, folderName) ->
                    val changed = folderUri != lastFolderUri
                    lastFolderUri = folderUri
                    _uiState.update {
                        it.copy(
                            folderUri = folderUri,
                            folderName = folderName,
                            // Drop stale selection when the folder changes.
                            selectedUri = if (changed) null else it.selectedUri,
                            selectedName = if (changed) null else it.selectedName,
                            selectedMarkdown = if (changed) null else it.selectedMarkdown,
                            selectedError = if (changed) null else it.selectedError
                        )
                    }
                    if (changed) refresh()
                }
        }
    }

    fun refresh() {
        val folderUri = _uiState.value.folderUri ?: run {
            _uiState.update { it.copy(documents = emptyList(), isLoading = false, errorMessage = null) }
            return
        }
        listJob?.cancel()
        listJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { storage.listMarkdownFiles(folderUri) }
                .onSuccess { documents ->
                    _uiState.update { it.copy(documents = documents, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            documents = emptyList(),
                            isLoading = false,
                            errorMessage = error.message ?: "Cannot list guidelines files"
                        )
                    }
                }
        }
    }

    fun openDocument(document: GuidelinesDocument) {
        readJob?.cancel()
        _uiState.update {
            it.copy(
                selectedUri = document.uri,
                selectedName = document.name,
                selectedMarkdown = null,
                selectedIsLoading = true,
                selectedError = null
            )
        }
        readJob = viewModelScope.launch {
            runCatching { storage.readMarkdownFile(document.uri) }
                .onSuccess { markdown ->
                    _uiState.update {
                        // Ignore late results after the selection moved on.
                        if (it.selectedUri != document.uri) return@update it
                        it.copy(selectedMarkdown = markdown, selectedIsLoading = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        if (it.selectedUri != document.uri) return@update it
                        it.copy(
                            selectedIsLoading = false,
                            selectedError = error.message ?: "Cannot read guidelines file"
                        )
                    }
                }
        }
    }

    fun retrySelected() {
        val uri = _uiState.value.selectedUri ?: return
        val name = _uiState.value.selectedName ?: return
        openDocument(GuidelinesDocument(uri = uri, name = name))
    }

    fun closeDocument() {
        readJob?.cancel()
        _uiState.update {
            it.copy(
                selectedUri = null,
                selectedName = null,
                selectedMarkdown = null,
                selectedIsLoading = false,
                selectedError = null
            )
        }
    }

    fun setFolder(uri: String?, name: String?) {
        viewModelScope.launch {
            settingsRepository.setGuidelinesFolder(uri, name)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, selectedError = null) }
    }
}

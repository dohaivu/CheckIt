package com.checkit.ui.nested

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.data.SettingsRepository
import com.checkit.domain.MetricRollupPolicy
import com.checkit.domain.NestedColorToken
import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedManualMetric
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.TagItem
import com.checkit.domain.TaskPriority
import com.checkit.domain.usecase.AddNestedDocumentUseCase
import com.checkit.domain.usecase.AddNestedItemUseCase
import com.checkit.domain.usecase.DeleteNestedDocumentUseCase
import com.checkit.domain.usecase.DeleteNestedItemsUseCase
import com.checkit.domain.usecase.MoveNestedItemsUseCase
import com.checkit.domain.usecase.ObserveNestedDocumentTreeUseCase
import com.checkit.domain.usecase.ObserveNestedDocumentsUseCase
import com.checkit.domain.usecase.ObserveNestedTagsUseCase
import com.checkit.domain.usecase.RenameNestedDocumentUseCase
import com.checkit.domain.usecase.ReplaceNestedManualMetricsUseCase
import com.checkit.domain.usecase.SetNestedItemCheckboxEnabledUseCase
import com.checkit.domain.usecase.SetNestedItemsCheckedUseCase
import com.checkit.domain.usecase.ToggleNestedItemCollapsedUseCase
import com.checkit.domain.usecase.UpdateNestedItemDateRangeUseCase
import com.checkit.domain.usecase.UpdateNestedItemFormattingUseCase
import com.checkit.domain.usecase.UpdateNestedItemMetricSettingsUseCase
import com.checkit.domain.usecase.UpdateNestedItemNoteUseCase
import com.checkit.domain.usecase.UpdateNestedItemPriorityUseCase
import com.checkit.domain.usecase.UpdateNestedItemTagsUseCase
import com.checkit.domain.usecase.UpdateNestedItemTextUseCase
import com.checkit.ui.UiEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import com.checkit.domain.FocusPeriod
import com.checkit.domain.Period
import com.checkit.ui.today

// --- State Hierarchy ---

data class NewItemDraft(
    val anchorId: Long? = null,
    val parentId: Long? = null,
    val depth: Int = 0,
    val text: String = ""
)

data class SelectionState(
    val isActive: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
)

data class NestedFilterState(
    val isVisible: Boolean = false,
    val focus: FocusPeriod? = null,
    val query: String = "",
    val hideChecked: Boolean = false,
    val selectedTagIds: Set<Long> = emptySet()
) {
    val isActive: Boolean get() = focus != null || query.isNotBlank() || hideChecked || selectedTagIds.isNotEmpty()
}

sealed interface NestedEditorOverlay {
    data object None : NestedEditorOverlay
    data class AddingItem(val draft: NewItemDraft) : NestedEditorOverlay
    data class EditingNote(val itemId: Long, val initialText: String) : NestedEditorOverlay
    data class ConfirmDelete(val itemIds: List<Long>) : NestedEditorOverlay
}

sealed interface NestedEditorState {
    data object Loading : NestedEditorState
    data class Error(val message: String) : NestedEditorState
    data class Active(
        val documentId: Long,
        val tree: NestedDocumentTree,
        val zoomPath: List<Long> = emptyList(),
        val selection: SelectionState = SelectionState(),
        val overlay: NestedEditorOverlay = NestedEditorOverlay.None,
        val selectedItemId: Long? = null,
        val editingTextItemId: Long? = null,
        val availableTags: List<TagItem> = emptyList(),
        val filters: NestedFilterState = NestedFilterState()
    ) : NestedEditorState {
        val focusedItem: NestedItemNode? get() = tree.nodeById[zoomPath.lastOrNull()]
    }
}

data class NestedUiState(
    val documents: List<NestedDocument> = emptyList(),
    val isListLoading: Boolean = true,
    val documentDeleting: NestedDocument? = null,
    val showNewDocumentDialog: Boolean = false,
    val newDocumentTitle: String = "",
    val editor: NestedEditorState? = null
)

class NestedListsViewModel(
    private val observeDocumentsUseCase: ObserveNestedDocumentsUseCase,
    private val observeTagsUseCase: ObserveNestedTagsUseCase,
    private val observeTreeUseCase: ObserveNestedDocumentTreeUseCase,
    private val addDocumentUseCase: AddNestedDocumentUseCase,
    private val renameDocumentUseCase: RenameNestedDocumentUseCase,
    private val deleteDocumentUseCase: DeleteNestedDocumentUseCase,
    private val addItemUseCase: AddNestedItemUseCase,
    private val updateItemTextUseCase: UpdateNestedItemTextUseCase,
    private val updateItemNoteUseCase: UpdateNestedItemNoteUseCase,
    private val updateItemFormattingUseCase: UpdateNestedItemFormattingUseCase,
    private val updateItemDateRangeUseCase: UpdateNestedItemDateRangeUseCase,
    private val updateItemPriorityUseCase: UpdateNestedItemPriorityUseCase,
    private val updateItemTagsUseCase: UpdateNestedItemTagsUseCase,
    private val updateItemMetricSettingsUseCase: UpdateNestedItemMetricSettingsUseCase,
    private val replaceNestedManualMetricsUseCase: ReplaceNestedManualMetricsUseCase,
    private val setCheckboxEnabledUseCase: SetNestedItemCheckboxEnabledUseCase,
    private val setItemsCheckedUseCase: SetNestedItemsCheckedUseCase,
    private val toggleCollapsedUseCase: ToggleNestedItemCollapsedUseCase,
    private val moveItemsUseCase: MoveNestedItemsUseCase,
    private val deleteItemsUseCase: DeleteNestedItemsUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NestedUiState())
    val uiState: StateFlow<NestedUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
    
    private var editorObservationJob: Job? = null
    private val moveMutex = Mutex()
    private var latestTags: List<TagItem> = emptyList()

    init {
        collectDocuments()
        viewModelScope.launch {
            observeTagsUseCase().collect { tags ->
                latestTags = tags
                _uiState.update { current ->
                    if (current.editor is NestedEditorState.Active) {
                        current.copy(editor = current.editor.copy(availableTags = tags))
                    } else current
                }
            }
        }
    }

    private fun collectDocuments() {
        viewModelScope.launch {
            observeDocumentsUseCase()
                .catch { error ->
                    _uiState.update { it.copy(isListLoading = false) }
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to load documents"))
                }
                .collect { documents ->
                    _uiState.update { it.copy(documents = documents, isListLoading = false) }
                }
        }
    }

    fun openDocument(documentId: Long) {
        val currentEditor = _uiState.value.editor
        if (currentEditor is NestedEditorState.Active && currentEditor.documentId == documentId) return
        
        editorObservationJob?.cancel()
        
        viewModelScope.launch {
            settingsRepository.setLastNestedDocumentId(documentId)
        }

        _uiState.update { it.copy(editor = NestedEditorState.Loading) }
        
        editorObservationJob = viewModelScope.launch {
            observeTreeUseCase(documentId)
                .catch { error ->
                    _uiState.update { it.copy(editor = NestedEditorState.Error(error.message ?: "Failed to load")) }
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to load document"))
                }
                .collectLatest { tree ->
                    _uiState.update { current ->
                        val active = (current.editor as? NestedEditorState.Active) ?: NestedEditorState.Active(
                            documentId = documentId,
                            tree = tree,
                            availableTags = latestTags
                        )
                        current.copy(editor = active.copy(tree = tree))
                    }
                }
        }
    }

    fun closeDocument() {
        editorObservationJob?.cancel()
        editorObservationJob = null
        _uiState.update { it.copy(editor = null) }
        viewModelScope.launch {
            settingsRepository.setLastNestedDocumentId(null)
        }
    }

    // --- Document Management ---

    fun startNewDocument() {
        _uiState.update { it.copy(showNewDocumentDialog = true, newDocumentTitle = "") }
    }

    fun updateNewDocumentTitle(title: String) {
        _uiState.update { it.copy(newDocumentTitle = title) }
    }

    fun cancelNewDocument() {
        _uiState.update { it.copy(showNewDocumentDialog = false) }
    }

    fun addDocument(title: String) {
        viewModelScope.launch {
            runCatching { addDocumentUseCase(title) }
                .onSuccess { id -> 
                    _uiState.update { it.copy(showNewDocumentDialog = false) }
                    openDocument(id) 
                }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to create document"))
                }
        }
    }

    fun requestDeleteDocument(document: NestedDocument) {
        _uiState.update { it.copy(documentDeleting = document) }
    }

    fun cancelDeleteDocument() {
        _uiState.update { it.copy(documentDeleting = null) }
    }

    fun confirmDeleteDocument(documentId: Long) {
        viewModelScope.launch {
            deleteDocumentUseCase(documentId)
            _uiState.update { 
                val newEditor = if (it.editor is NestedEditorState.Active && it.editor.documentId == documentId) null else it.editor
                it.copy(documentDeleting = null, editor = newEditor)
            }
            _events.tryEmit(UiEvent.ShowSnackbar("Document deleted"))
        }
    }

    // --- Zoom ---

    fun zoomInSelected() {
        updateActiveEditor { current ->
            val id = current.selectedItemId ?: return@updateActiveEditor current
            val node = current.tree.nodeById[id] ?: return@updateActiveEditor current
            if (!node.hasChildren) return@updateActiveEditor current
            
            val chain = ancestorChain(current.tree, id)
            current.copy(zoomPath = chain)
        }
    }

    fun zoomOut() {
        updateActiveEditor { it.copy(zoomPath = it.zoomPath.dropLast(1)) }
    }

    fun zoomToItem(itemId: Long) {
        updateActiveEditor { current ->
            val chain = ancestorChain(current.tree, itemId)
            if (chain.isNotEmpty()) current.copy(zoomPath = chain) else current
        }
    }

    fun zoomToRoot() = updateActiveEditor { it.copy(zoomPath = emptyList()) }

    // --- Filtering ---

    fun toggleFilterVisibility() {
        updateActiveEditor { it.copy(filters = it.filters.copy(isVisible = !it.filters.isVisible)) }
    }

    fun updateFilterFocus(focus: FocusPeriod) {
        updateActiveEditor { it.copy(filters = it.filters.copy(focus = focus)) }
    }

    fun updateFilterQuery(query: String) {
        updateActiveEditor { it.copy(filters = it.filters.copy(query = query)) }
    }

    fun toggleHideChecked() {
        updateActiveEditor { it.copy(filters = it.filters.copy(hideChecked = !it.filters.hideChecked)) }
    }

    fun updateFilterTags(tagId: Long) {
        updateActiveEditor { current ->
            val selected = current.filters.selectedTagIds
            val next = if (tagId in selected) selected - tagId else selected + tagId
            current.copy(filters = current.filters.copy(selectedTagIds = next))
        }
    }

    fun resetFilters() {
        updateActiveEditor { it.copy(filters = it.filters.copy(focus = null, query = "", hideChecked = false, selectedTagIds = emptySet())) }
    }

    fun nextFilterPeriod() {
        updateActiveEditor { current ->
            val focus = current.filters.focus ?: return@updateActiveEditor current
            current.copy(filters = current.filters.copy(focus = focus.shift(1)))
        }
    }

    fun previousFilterPeriod() {
        updateActiveEditor { current ->
            val focus = current.filters.focus ?: return@updateActiveEditor current
            current.copy(filters = current.filters.copy(focus = focus.shift(-1)))
        }
    }

    fun currentFilterPeriod() {
        updateActiveEditor { current ->
            val period = current.filters.focus?.period ?: Period.Week
            current.copy(filters = current.filters.copy(focus = FocusPeriod(period, today())))
        }
    }

    // --- Item Editing ---

    fun startAddChild(parentId: Long) {
        updateActiveEditor { current ->
            val parent = current.tree.nodeById[parentId] ?: return@updateActiveEditor current
            var anchor = parent
            while (anchor.children.isNotEmpty() && !anchor.item.collapsed) {
                anchor = anchor.children.last()
            }
            current.copy(
                overlay = NestedEditorOverlay.AddingItem(
                    NewItemDraft(
                        anchorId = anchor.item.id,
                        parentId = parentId,
                        depth = current.ancestorDepth(parentId) + 1
                    )
                )
            )
        }
    }

    fun startAddRoot() {
        updateActiveEditor { current ->
            current.copy(overlay = NestedEditorOverlay.AddingItem(NewItemDraft()))
        }
    }

    fun startAddSibling(siblingId: Long) {
        updateActiveEditor { current ->
            val item = current.tree.itemById[siblingId] ?: return@updateActiveEditor current
            current.copy(
                overlay = NestedEditorOverlay.AddingItem(
                    NewItemDraft(
                        anchorId = siblingId,
                        parentId = item.parentId,
                        depth = current.ancestorDepth(siblingId)
                    )
                )
            )
        }
    }

    fun updateNewItemText(text: String) {
        updateActiveEditor { current ->
            val overlay = current.overlay as? NestedEditorOverlay.AddingItem ?: return@updateActiveEditor current
            current.copy(overlay = overlay.copy(draft = overlay.draft.copy(text = text)))
        }
    }

    fun cancelAddItem() {
        updateActiveEditor { it.copy(overlay = NestedEditorOverlay.None) }
    }

    fun commitNewItem(thenContinue: Boolean = false) {
        val active = getActiveEditor() ?: return
        val overlay = active.overlay as? NestedEditorOverlay.AddingItem ?: return
        val text = overlay.draft.text
        if (text.isBlank()) {
            cancelAddItem()
            return
        }

        val items = active.tree.flatItems
        val anchor = items.firstOrNull { it.id == overlay.draft.anchorId }
        val isAddingChild = overlay.draft.parentId != null &&
            (anchor?.id == overlay.draft.parentId || anchor?.parentId != overlay.draft.parentId)

        val position = if (isAddingChild) {
            items.filter { it.parentId == overlay.draft.parentId }.maxOfOrNull { it.position }?.plus(1) ?: 0
        } else {
            anchor?.position?.plus(1)
        }

        viewModelScope.launch {
            runCatching { addItemUseCase(active.documentId, overlay.draft.parentId, text, position) }
                .onSuccess { itemId ->
                    if (thenContinue) {
                        selectItem(itemId)
                        // Keep the input open, re-anchored below the committed item as its sibling.
                        updateActiveEditor { current ->
                            current.copy(
                                overlay = NestedEditorOverlay.AddingItem(
                                    NewItemDraft(
                                        anchorId = itemId,
                                        parentId = overlay.draft.parentId,
                                        depth = overlay.draft.depth,
                                        text = ""
                                    )
                                )
                            )
                        }
                    } else {
                        cancelAddItem()
                        selectItem(itemId)
                    }
                }
                .onFailure { error -> _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to add item")) }
        }
    }

    fun startEditText(itemId: Long) {
        updateActiveEditor { it.copy(selectedItemId = itemId, editingTextItemId = itemId) }
    }

    fun selectItem(itemId: Long) {
        updateActiveEditor { current ->
            current.copy(
                selectedItemId = if (current.selectedItemId == itemId) null else itemId,
                editingTextItemId = null
            )
        }
    }

    fun stopEditText() {
        updateActiveEditor { it.copy(editingTextItemId = null) }
    }

    fun saveItemText(itemId: Long, text: String) {
        updateActiveEditor { it.copy(editingTextItemId = null) }
        if (text.isBlank()) return
        viewModelScope.launch {
            updateItemTextUseCase(itemId, text)
        }
    }

    fun startEditNote(itemId: Long) {
        updateActiveEditor { current ->
            val item = current.tree.itemById[itemId] ?: return@updateActiveEditor current
            current.copy(overlay = NestedEditorOverlay.EditingNote(itemId, item.note.orEmpty()))
        }
    }

    fun stopEditNote() = updateActiveEditor { it.copy(overlay = NestedEditorOverlay.None) }

    fun saveItemNote(itemId: Long, note: String?) {
        stopEditNote()
        viewModelScope.launch {
            updateItemNoteUseCase(itemId, note?.take(2_000))
        }
    }

    // --- Formatting & Metadata ---

    fun updateItemFormatting(itemId: Long, style: NestedTextStyle, text: NestedColorToken, bg: NestedColorToken) {
        viewModelScope.launch { updateItemFormattingUseCase(itemId, style, text, bg) }
    }

    fun updateItemDateRange(itemId: Long, start: LocalDate?, end: LocalDate?) {
        viewModelScope.launch { updateItemDateRangeUseCase(itemId, start, end) }
    }

    fun updateItemPriority(itemId: Long, priority: TaskPriority) {
        viewModelScope.launch { updateItemPriorityUseCase(itemId, priority) }
    }

    fun updateItemTags(itemId: Long, tagIds: List<Long>) {
        viewModelScope.launch { updateItemTagsUseCase(itemId, tagIds) }
    }

    fun updateItemMetricSettings(itemId: Long, min: Int, policy: MetricRollupPolicy, show: Boolean) {
        viewModelScope.launch { updateItemMetricSettingsUseCase(itemId, min, policy, show) }
    }

    fun replaceManualMetrics(itemId: Long, metrics: List<NestedManualMetric>) {
        viewModelScope.launch { replaceNestedManualMetricsUseCase(itemId, metrics) }
    }

    // --- Checkbox & Structure ---

    fun toggleCheckboxEnabled(itemId: Long) {
        val item = getActiveEditor()?.tree?.itemById?.get(itemId) ?: return
        viewModelScope.launch { setCheckboxEnabledUseCase(itemId, !item.checkboxEnabled) }
    }

    fun toggleChecked(itemId: Long) {
        val item = getActiveEditor()?.tree?.itemById?.get(itemId) ?: return
        if (!item.checkboxEnabled) return
        viewModelScope.launch { setItemsCheckedUseCase(listOf(itemId), !item.checked) }
    }

    fun setChecked(itemId: Long, checked: Boolean) {
        viewModelScope.launch { setItemsCheckedUseCase(listOf(itemId), checked) }
    }

    fun toggleCollapsed(itemId: Long) {
        viewModelScope.launch { toggleCollapsedUseCase(itemId) }
    }

    fun indent(itemId: Long) = applyMove { items -> moveItemsUseCase.indent(items, itemId) }
    fun outdent(itemId: Long) = applyMove { items -> moveItemsUseCase.outdent(items, itemId) }
    fun moveUp(itemId: Long) = applyMove { items -> moveItemsUseCase.moveUp(items, itemId) }
    fun moveDown(itemId: Long) = applyMove { items -> moveItemsUseCase.moveDown(items, itemId) }

    private fun applyMove(planner: (List<NestedListItem>) -> List<com.checkit.domain.NestedItemMove>) {
        val active = getActiveEditor() ?: return
        val moves = planner(active.tree.flatItems)
        if (moves.isEmpty()) return
        viewModelScope.launch {
            moveMutex.withLock { moveItemsUseCase(moves) }
        }
    }

    // --- Selection Mode ---

    fun enterSelectionMode() {
        updateActiveEditor { it.copy(selection = SelectionState(isActive = true)) }
    }

    fun exitSelectionMode() {
        updateActiveEditor { it.copy(selection = SelectionState()) }
    }

    fun toggleSelect(itemId: Long) {
        updateActiveEditor { current ->
            val selected = current.selection.selectedIds
            val next = if (itemId in selected) selected - itemId else selected + itemId
            current.copy(selection = current.selection.copy(selectedIds = next))
        }
    }

    fun selectAll() {
        updateActiveEditor { current ->
            current.copy(selection = current.selection.copy(selectedIds = current.tree.flatItems.map { it.id }.toSet()))
        }
    }

    fun batchSetChecked(checked: Boolean) {
        val ids = getActiveEditor()?.selection?.selectedIds?.toList().orEmpty()
        if (ids.isEmpty()) return
        viewModelScope.launch { setItemsCheckedUseCase(ids, checked) }
    }

    fun requestDeleteSelected() {
        updateActiveEditor { current ->
            val ids = if (current.selection.isActive) {
                current.selection.selectedIds.toList()
            } else {
                listOfNotNull(current.selectedItemId)
            }
            current.copy(overlay = NestedEditorOverlay.ConfirmDelete(ids))
        }
    }

    fun dismissDeleteConfirm() = updateActiveEditor { it.copy(overlay = NestedEditorOverlay.None) }

    fun confirmDeleteSelected() {
        val active = getActiveEditor() ?: return
        val overlay = active.overlay as? NestedEditorOverlay.ConfirmDelete ?: return
        updateActiveEditor { it.copy(overlay = NestedEditorOverlay.None, selection = SelectionState(), selectedItemId = null, editingTextItemId = null) }
        viewModelScope.launch {
            deleteItemsUseCase(overlay.itemIds)
            _events.tryEmit(UiEvent.ShowSnackbar("Items deleted"))
        }
    }

    // --- Private Helpers ---

    private fun getActiveEditor(): NestedEditorState.Active? = _uiState.value.editor as? NestedEditorState.Active

    private fun updateActiveEditor(action: (NestedEditorState.Active) -> NestedEditorState.Active) {
        _uiState.update { current ->
            val active = current.editor as? NestedEditorState.Active ?: return@update current
            current.copy(editor = action(active))
        }
    }

    private fun ancestorChain(tree: NestedDocumentTree, id: Long): List<Long> {
        val chain = ArrayDeque<Long>()
        var current = tree.itemById[id] ?: return emptyList()
        while (true) {
            chain.addFirst(current.id)
            val parentId = current.parentId ?: break
            current = tree.itemById[parentId] ?: return emptyList()
        }
        return chain.toList()
    }

    private fun NestedEditorState.Active.ancestorDepth(id: Long): Int {
        var depth = 0
        var currentId = tree.itemById[id]?.parentId
        while (currentId != null) {
            depth++
            currentId = tree.itemById[currentId]?.parentId
        }
        return depth
    }
}

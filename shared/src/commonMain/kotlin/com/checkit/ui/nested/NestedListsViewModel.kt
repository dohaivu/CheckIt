package com.checkit.ui.nested

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedItemNode
import com.checkit.domain.usecase.AddNestedDocumentUseCase
import com.checkit.domain.usecase.AddNestedItemUseCase
import com.checkit.domain.usecase.DeleteNestedDocumentUseCase
import com.checkit.domain.usecase.DeleteNestedItemsUseCase
import com.checkit.domain.usecase.MoveNestedItemsUseCase
import com.checkit.domain.usecase.ObserveNestedDocumentTreeUseCase
import com.checkit.domain.usecase.ObserveNestedDocumentsUseCase
import com.checkit.domain.usecase.RenameNestedDocumentUseCase
import com.checkit.domain.usecase.SetNestedItemCheckboxEnabledUseCase
import com.checkit.domain.usecase.SetNestedItemsCheckedUseCase
import com.checkit.domain.usecase.ToggleNestedItemCollapsedUseCase
import com.checkit.domain.usecase.UpdateNestedItemNoteUseCase
import com.checkit.domain.usecase.UpdateNestedItemTextUseCase
import com.checkit.ui.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class NestedListsUiState(
    val documents: List<NestedDocument> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Editor view state. [focusItemIds] is the zoom chain from the document root
 * down to the currently focused node (empty = document root). The visible tree
 * is the subtree of the last focused id, or all roots when empty.
 */
data class NestedListEditorUiState(
    val documentId: Long = 0L,
    val tree: NestedDocumentTree? = null,
    val focusItemIds: List<Long> = emptyList(),
    val selectedItemIds: Set<Long> = emptySet(),
    val selectionMode: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val editingItemId: Long? = null,
    val isEditingText: Boolean = false,
    val editingNoteItemId: Long? = null,
    val isAddingItem: Boolean = false,
    val addingItemAnchorId: Long? = null,
    val newItemParentId: Long? = null,
    val newItemText: String = "",
    val isLoading: Boolean = true
) {
    val focusedItem: NestedItemNode? by lazy { tree?.nodeById?.get(focusItemIds.lastOrNull()) }

    fun focusedNode(nodes: List<NestedItemNode>): NestedItemNode? {
        val last = focusItemIds.lastOrNull() ?: return null
        fun find(ns: List<NestedItemNode>): NestedItemNode? {
            for (n in ns) {
                if (n.item.id == last) return n
                find(n.children)?.let { return it }
            }
            return null
        }
        return find(nodes)
    }
}

class NestedListsViewModel(
    private val observeDocumentsUseCase: ObserveNestedDocumentsUseCase,
    private val observeTreeUseCase: ObserveNestedDocumentTreeUseCase,
    private val addDocumentUseCase: AddNestedDocumentUseCase,
    private val renameDocumentUseCase: RenameNestedDocumentUseCase,
    private val deleteDocumentUseCase: DeleteNestedDocumentUseCase,
    private val addItemUseCase: AddNestedItemUseCase,
    private val updateItemTextUseCase: UpdateNestedItemTextUseCase,
    private val updateItemNoteUseCase: UpdateNestedItemNoteUseCase,
    private val setCheckboxEnabledUseCase: SetNestedItemCheckboxEnabledUseCase,
    private val setItemsCheckedUseCase: SetNestedItemsCheckedUseCase,
    private val toggleCollapsedUseCase: ToggleNestedItemCollapsedUseCase,
    private val moveItemsUseCase: MoveNestedItemsUseCase,
    private val deleteItemsUseCase: DeleteNestedItemsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NestedListsUiState())
    val uiState: StateFlow<NestedListsUiState> = _uiState.asStateFlow()

    private val _editor = MutableStateFlow<NestedListEditorUiState?>(null)
    val editor: StateFlow<NestedListEditorUiState?> = _editor.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
    private var editorObservationJob: Job? = null
    private val moveMutex = Mutex()

    init {
        collectDocuments()
    }

    private fun collectDocuments() {
        viewModelScope.launch {
            observeDocumentsUseCase()
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to load documents"))
                }
                .collect { documents ->
                    _uiState.update { it.copy(documents = documents, isLoading = false) }
                }
        }
    }

    fun openDocument(documentId: Long) {
        val editing = _editor.value
        if (editing?.documentId == documentId) return
        editorObservationJob?.cancel()
        _editor.value = NestedListEditorUiState(documentId = documentId)
        editorObservationJob = viewModelScope.launch {
            observeTreeUseCase(documentId)
                .catch { error ->
                    _editor.update { it?.copy(isLoading = false) }
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to load document"))
                }
                .collectLatest { tree ->
                    _editor.update { it?.copy(tree = tree, isLoading = false) }
                }
        }
    }

    fun closeDocument() {
        editorObservationJob?.cancel()
        editorObservationJob = null
        _editor.value = null
    }

    fun addDocument(title: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { addDocumentUseCase(title) }
                .onSuccess { id -> onCreated(id) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to create document"))
                }
        }
    }

    fun renameDocument(documentId: Long, title: String) {
        viewModelScope.launch {
            runCatching { renameDocumentUseCase(documentId, title) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to rename document"))
                }
        }
    }

    fun deleteDocument(documentId: Long) {
        viewModelScope.launch {
            deleteDocumentUseCase(documentId)
            closeDocument()
            _events.tryEmit(UiEvent.ShowSnackbar("Document deleted"))
        }
    }

    // ---------------- zoom (focus) ----------------

    fun zoomIn(item: NestedItemNode) {
        if (!item.hasChildren) return
        val state = _editor.value ?: return
        val chain = ancestorChain(item.item.id)
        if (chain.isNotEmpty()) {
            _editor.update { it?.copy(focusItemIds = chain) }
        }
    }

    fun zoomInSelected() {
        val state = _editor.value ?: return
        val id = state.editingItemId ?: return
        val node = state.tree?.nodeById?.get(id) ?: return
        zoomIn(node)
    }

    fun zoomOut() {
        _editor.update { it?.copy(focusItemIds = it.focusItemIds.dropLast(1)) }
    }

    fun zoomOutTo(depth: Int) {
        _editor.update { state ->
            state?.copy(focusItemIds = state.focusItemIds.take(depth))
        }
    }

    /** Zooms directly to the tapped breadcrumb item, independent of display depth. */
    fun zoomToItem(itemId: Long) {
        val state = _editor.value ?: return
        val chain = ancestorChain(itemId)
        if (chain.isNotEmpty() && state.tree?.nodeById?.containsKey(itemId) == true) {
            _editor.update { it?.copy(focusItemIds = chain) }
        }
    }

    fun zoomToRoot() = zoomOutTo(0)

    // ---------------- item editing ----------------

    fun startAddChild(parentId: Long) {
        _editor.update {
            it?.copy(isAddingItem = true, addingItemAnchorId = parentId, newItemParentId = parentId, newItemText = "")
        }
    }

    fun startAddRoot() {
        _editor.update {
            it?.copy(isAddingItem = true, addingItemAnchorId = null, newItemParentId = null, newItemText = "")
        }
    }

    fun startAddSibling(siblingId: Long) {
        val state = _editor.value ?: return
        val item = state.tree?.itemById?.get(siblingId)
        _editor.update {
            it?.copy(
                isAddingItem = true,
                addingItemAnchorId = siblingId,
                newItemParentId = item?.parentId,
                newItemText = ""
            )
        }
    }

    fun updateNewItemText(text: String) {
        _editor.update { it?.copy(newItemText = text) }
    }

    fun cancelAddItem() {
        _editor.update { it?.copy(isAddingItem = false, addingItemAnchorId = null, newItemParentId = null, newItemText = "") }
    }

    fun commitNewItem() {
        val state = _editor.value ?: return
        val text = state.newItemText
        if (text.isBlank()) {
            cancelAddItem()
            return
        }
        val items = flatItems(state)
        val position = if (state.newItemParentId == state.addingItemAnchorId) {
            0
        } else {
            items.firstOrNull { it.id == state.addingItemAnchorId }?.position?.plus(1)
        }
        viewModelScope.launch {
            runCatching {
                addItemUseCase(state.documentId, state.newItemParentId, text, position)
            }
                .onSuccess {
                    cancelAddItem()
                }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to add item"))
                }
        }
    }

    fun startEditText(itemId: Long) {
        _editor.update { it?.copy(editingItemId = itemId, isEditingText = true) }
    }

    fun selectItem(itemId: Long) {
        _editor.update { state ->
            state?.copy(editingItemId = itemId, isEditingText = false)
        }
    }

    fun stopEditText() {
        _editor.update { it?.copy(editingItemId = null, isEditingText = false) }
    }

    fun saveItemText(itemId: Long, text: String) {
        stopEditText()
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching { updateItemTextUseCase(itemId, text) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to save item"))
                }
        }
    }

    fun startEditNote(itemId: Long) {
        _editor.update { it?.copy(editingNoteItemId = itemId) }
    }

    fun stopEditNote() {
        _editor.update { it?.copy(editingNoteItemId = null) }
    }

    fun saveItemNote(itemId: Long, note: String?) {
        stopEditNote()
        viewModelScope.launch {
            runCatching { updateItemNoteUseCase(itemId, note?.take(MAX_NOTE_LENGTH)) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to save note"))
                }
        }
    }

    private companion object {
        const val MAX_NOTE_LENGTH = 2_000
    }

    // ---------------- checkbox / collapse ----------------

    fun toggleCheckboxEnabled(itemId: Long) {
        val state = _editor.value ?: return
        val item = state.tree?.itemById?.get(itemId) ?: return
        viewModelScope.launch {
            runCatching { setCheckboxEnabledUseCase(itemId, !item.checkboxEnabled) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to update checkbox"))
                }
        }
    }

    fun toggleChecked(itemId: Long) {
        val state = _editor.value ?: return
        val item = state.tree?.itemById?.get(itemId) ?: return
        if (!item.checkboxEnabled) return
        viewModelScope.launch {
            runCatching { setItemsCheckedUseCase(listOf(itemId), !item.checked) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to update item"))
                }
        }
    }

    fun setChecked(itemId: Long, checked: Boolean) {
        viewModelScope.launch {
            runCatching { setItemsCheckedUseCase(listOf(itemId), checked) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to update item"))
                }
        }
    }

    fun toggleCollapsed(itemId: Long) {
        viewModelScope.launch {
            runCatching { toggleCollapsedUseCase(itemId) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to collapse"))
                }
        }
    }

    // ---------------- structure ----------------

    fun indent(itemId: Long) {
        applyMove { items -> moveItemsUseCase.indent(items, itemId) }
    }

    fun outdent(itemId: Long) {
        applyMove { items -> moveItemsUseCase.outdent(items, itemId) }
    }

    fun moveUp(itemId: Long) {
        applyMove { items -> moveItemsUseCase.moveUp(items, itemId) }
    }

    fun moveDown(itemId: Long) {
        applyMove { items -> moveItemsUseCase.moveDown(items, itemId) }
    }

    private fun applyMove(planner: (List<NestedListItem>) -> List<com.checkit.domain.NestedItemMove>) {
        val state = _editor.value ?: return
        val items = flatItems(state)
        val moves = planner(items)
        if (moves.isEmpty()) return
        viewModelScope.launch {
            runCatching { moveMutex.withLock { moveItemsUseCase(moves) } }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to move item"))
                }
        }
    }

    // ---------------- selection mode ----------------

    fun enterSelectionMode() {
        _editor.update { it?.copy(selectionMode = true) }
    }

    fun exitSelectionMode() {
        _editor.update { it?.copy(selectionMode = false, selectedItemIds = emptySet()) }
    }

    fun toggleSelect(itemId: Long) {
        _editor.update { state ->
            state?.let { current ->
                val selected = current.selectedItemIds
                current.copy(
                    selectedItemIds = if (itemId in selected) selected - itemId else selected + itemId
                )
            }
        }
    }

    fun selectAll() {
        val state = _editor.value ?: return
        _editor.update { it?.copy(selectedItemIds = flatItems(state).map { it.id }.toSet()) }
    }

    fun batchSetChecked(checked: Boolean) {
        val ids = _editor.value?.selectedItemIds.orEmpty().toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching { setItemsCheckedUseCase(ids, checked) }
                .onFailure { error ->
                    _events.tryEmit(UiEvent.ShowSnackbar(error.message ?: "Unable to update items"))
                }
        }
    }

    fun requestDeleteSelected() {
        _editor.update { it?.copy(showDeleteConfirm = true) }
    }

    fun dismissDeleteConfirm() {
        _editor.update { it?.copy(showDeleteConfirm = false) }
    }

    fun confirmDeleteSelected() {
        val ids = _editor.value?.selectedItemIds.orEmpty().toList()
        _editor.update { it?.copy(showDeleteConfirm = false, selectionMode = false, selectedItemIds = emptySet()) }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            deleteItemsUseCase(ids)
            _events.tryEmit(UiEvent.ShowSnackbar("Items deleted"))
        }
    }

    fun requestDeleteItem(itemId: Long) {
        _editor.update { it?.copy(selectedItemIds = setOf(itemId), showDeleteConfirm = true) }
    }

    // ---------------- helpers ----------------

    private fun flatItems(state: NestedListEditorUiState): List<NestedListItem> =
        state.tree?.flatItems.orEmpty()

    /** Returns the ids from a root down to [id], or empty if [id] is not found. */
    private fun ancestorChain(id: Long): List<Long> {
        val tree = _editor.value?.tree ?: return emptyList()
        val chain = ArrayDeque<Long>()
        var current = tree.itemById[id] ?: return emptyList()
        while (true) {
            chain.addFirst(current.id)
            val parentId = current.parentId ?: break
            current = tree.itemById[parentId] ?: return emptyList()
        }
        return chain.toList()
    }
}

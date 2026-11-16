package com.checkit.ui.nested

import com.checkit.domain.MetricItem
import com.checkit.domain.MetricRollupPolicy
import com.checkit.domain.NestedColorToken
import com.checkit.domain.NestedBorderShape
import com.checkit.domain.NestedBorderStyle
import com.checkit.domain.NestedBorderWidth
import com.checkit.domain.NestedCalloutStyle
import com.checkit.domain.NestedCalloutLayout
import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedSortOrder
import com.checkit.domain.MoveDestinations
import com.checkit.domain.NestedDatePreset
import com.checkit.domain.NestedDateWindow
import com.checkit.domain.NestedRollingPreset
import com.checkit.domain.moveDestinations
import com.checkit.domain.NestedMetricSummary
import com.checkit.domain.NestedOutlineProjection
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.computeNestedInsertPosition
import com.checkit.domain.filterNestedTreeWithContext
import com.checkit.domain.TagItem
import com.checkit.domain.TaskPriority
import com.checkit.ui.isValidForSave
import com.checkit.ui.tasks.PredefinedLabels
import com.checkit.domain.usecase.AddNestedDocumentUseCase
import com.checkit.domain.usecase.AddNestedItemUseCase
import com.checkit.domain.usecase.CopyNestedSubtreeUseCase
import com.checkit.domain.usecase.DuplicateNestedItemUseCase
import com.checkit.domain.usecase.DeleteNestedDocumentUseCase
import com.checkit.domain.usecase.DeleteNestedItemsUseCase
import com.checkit.domain.usecase.MoveNestedItemsUseCase
import com.checkit.domain.usecase.NestedSubtreeClipboard
import com.checkit.domain.usecase.ObserveNestedDocumentTreeUseCase
import com.checkit.domain.usecase.ObserveNestedDocumentsUseCase
import com.checkit.domain.usecase.ObserveNestedTagsUseCase
import com.checkit.domain.usecase.PasteNestedSubtreeUseCase
import com.checkit.domain.usecase.RenameNestedDocumentUseCase
import com.checkit.domain.usecase.ReorderNestedDocumentsUseCase
import com.checkit.domain.usecase.ReplaceNestedManualMetricsUseCase
import com.checkit.domain.usecase.SetNestedItemCheckboxEnabledUseCase
import com.checkit.domain.usecase.SetNestedItemsCheckedUseCase
import com.checkit.domain.usecase.SetNestedItemsCollapsedUseCase
import com.checkit.domain.usecase.ToggleNestedItemCollapsedUseCase
import com.checkit.domain.usecase.UpdateNestedItemDateRangeUseCase
import com.checkit.domain.usecase.UpdateNestedItemFormattingUseCase
import com.checkit.domain.usecase.UpdateNestedItemBorderUseCase
import com.checkit.domain.usecase.UpdateNestedItemCalloutUseCase
import com.checkit.domain.usecase.UpdateNestedItemCalloutLayoutUseCase
import com.checkit.domain.usecase.ResetNestedItemCalloutToDefaultsUseCase
import com.checkit.domain.usecase.UpdateNestedItemGuideDotUseCase
import com.checkit.domain.usecase.UpdateNestedItemGuideDotColorUseCase
import com.checkit.domain.usecase.UpdateNestedItemNumberedChildrenUseCase
import com.checkit.domain.usecase.UpdateNestedDocumentNumberRootsUseCase
import com.checkit.domain.usecase.UpdateNestedItemLabelUseCase
import com.checkit.domain.usecase.UpdateNestedItemMetricSettingsUseCase
import com.checkit.domain.usecase.UpdateNestedItemNoteUseCase
import com.checkit.domain.usecase.UpdateNestedItemPriorityUseCase
import com.checkit.domain.usecase.UpdateNestedItemProgressUseCase
import com.checkit.domain.usecase.UpdateNestedItemTagsUseCase
import com.checkit.domain.usecase.UpdateNestedItemTextUseCase
import com.checkit.domain.usecase.NestedMarkdownExportOptions
import com.checkit.domain.usecase.toMarkdown
import com.checkit.domain.usecase.ExportNestedDocumentJsonUseCase
import com.checkit.domain.usecase.ImportNestedDocumentJsonUseCase
import com.checkit.infrastructure.initKoin
import com.checkit.ui.quicknote.QuickNoteAppleBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.mp.KoinPlatform

/**
 * Swift-friendly facade over the nested-list use cases for the macOS
 * SwiftUI window. SwiftUI cannot consume suspend functions or
 * [kotlinx.coroutines.flow.Flow] directly, so all interop goes through
 * callbacks delivered on the Main thread — same pattern as
 * QuickNoteMenuHelper.
 *
 * Nullable Kotlin Int parameters surface to Swift as KotlinInt?
 * (pass nil for null). Entity ids are Strings (stable UUIDs for sync);
 * blank means null for optional parent/anchor ids.
 *
 * The helper caches the latest observed tree so draft-commit position
 * math (mirroring NestedListsViewModel.commitNewItem) and move planning
 * reuse the shared pure logic instead of reimplementing it in Swift.
 * Sync between Android and macOS falls out naturally: both go through
 * the same repository/DAO, so Room is the source of truth and the tree
 * Flow re-emits on every mutation.
 */
class NestedAppleSubscription internal constructor(
    private val job: Job,
) {
    fun cancel() {
        job.cancel()
    }
}

class NestedAppleHelper(
    private val observeDocuments: ObserveNestedDocumentsUseCase,
    private val observeTags: ObserveNestedTagsUseCase,
    private val observeTree: ObserveNestedDocumentTreeUseCase,
    private val addDocument: AddNestedDocumentUseCase,
    private val renameDocumentUseCase: RenameNestedDocumentUseCase,
    private val deleteDocument: DeleteNestedDocumentUseCase,
    private val reorderDocumentsUseCase: ReorderNestedDocumentsUseCase,
    private val addItem: AddNestedItemUseCase,
    private val updateItemText: UpdateNestedItemTextUseCase,
    private val updateItemNote: UpdateNestedItemNoteUseCase,
    private val updateItemLabel: UpdateNestedItemLabelUseCase,
    private val updateItemFormatting: UpdateNestedItemFormattingUseCase,
    private val updateItemBorder: UpdateNestedItemBorderUseCase,
    private val updateItemCallout: UpdateNestedItemCalloutUseCase,
    private val updateItemCalloutLayout: UpdateNestedItemCalloutLayoutUseCase,
    private val resetItemCalloutToDefaults: ResetNestedItemCalloutToDefaultsUseCase,
    private val updateItemDateRange: UpdateNestedItemDateRangeUseCase,
    private val updateItemPriority: UpdateNestedItemPriorityUseCase,
    private val updateItemGuideDot: UpdateNestedItemGuideDotUseCase,
    private val updateItemGuideDotColor: UpdateNestedItemGuideDotColorUseCase,
    private val updateItemNumberedChildren: UpdateNestedItemNumberedChildrenUseCase,
    private val updateDocumentNumberRoots: UpdateNestedDocumentNumberRootsUseCase,
    private val updateItemTags: UpdateNestedItemTagsUseCase,
    private val updateItemMetricSettings: UpdateNestedItemMetricSettingsUseCase,
    private val updateItemProgress: UpdateNestedItemProgressUseCase,
    private val replaceManualMetrics: ReplaceNestedManualMetricsUseCase,
    private val setCheckboxEnabled: SetNestedItemCheckboxEnabledUseCase,
    private val setItemsChecked: SetNestedItemsCheckedUseCase,
    private val toggleCollapsedUseCase: ToggleNestedItemCollapsedUseCase,
    private val setItemsCollapsed: SetNestedItemsCollapsedUseCase,
    private val moveItems: MoveNestedItemsUseCase,
    private val deleteItems: DeleteNestedItemsUseCase,
    private val copySubtreeUseCase: CopyNestedSubtreeUseCase,
    private val pasteSubtreeUseCase: PasteNestedSubtreeUseCase,
    private val duplicateItemUseCase: DuplicateNestedItemUseCase,
    private val subtreeClipboard: NestedSubtreeClipboard,
    private val exportDocumentJsonUseCase: ExportNestedDocumentJsonUseCase,
    private val importDocumentJsonUseCase: ImportNestedDocumentJsonUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val moveMutex = Mutex()

    /** Latest tree snapshot per observed document, for draft/move planning. */
    private val latestTrees = mutableMapOf<String, NestedDocumentTree>()

    fun observeDocumentList(onUpdate: (List<NestedDocument>) -> Unit): NestedAppleSubscription {
        val job = scope.launch {
            observeDocuments().collect { onUpdate(it) }
        }
        return NestedAppleSubscription(job)
    }

    fun observeTagList(onUpdate: (List<TagItem>) -> Unit): NestedAppleSubscription {
        val job = scope.launch {
            observeTags().collect { onUpdate(it) }
        }
        return NestedAppleSubscription(job)
    }

    fun observeDocumentTree(documentId: String, onUpdate: (NestedDocumentTree) -> Unit): NestedAppleSubscription {
        val job = scope.launch {
            observeTree(documentId).collect { tree ->
                latestTrees[documentId] = tree
                onUpdate(tree)
            }
        }
        return NestedAppleSubscription(job)
    }

    // --- Documents ---

    fun createDocument(title: String, onDone: (String) -> Unit) {
        scope.launch {
            val id = runCatching { addDocument(title) }.getOrNull() ?: ""
            onDone(id)
        }
    }

    fun renameDocument(documentId: String, title: String) {
        scope.launch { runCatching { renameDocumentUseCase(documentId, title) } }
    }

    fun removeDocument(documentId: String) {
        scope.launch {
            runCatching { deleteDocument(documentId) }
            latestTrees.remove(documentId)
        }
    }

    fun reorderDocuments(documentIds: List<String>) {
        scope.launch { runCatching { reorderDocumentsUseCase(documentIds) } }
    }

    // --- Items: add (mirrors NestedListsViewModel.commitNewItem position math) ---

    /**
     * Commits a new item. Blank [anchorId]/[parentId] mean null (root insert).
     * onDone receives the new item id (or blank on failure/blank).
     */
    fun commitItem(
        documentId: String,
        anchorId: String,
        parentId: String,
        text: String,
        onDone: (String) -> Unit,
    ) {
        scope.launch {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                onDone("")
                return@launch
            }
            val anchor: String? = anchorId.takeIf { it.isNotEmpty() }
            val parent: String? = parentId.takeIf { it.isNotEmpty() }
            val items = latestTrees[documentId]?.flatItems.orEmpty()
            val (resolvedParent, position) = computeNestedInsertPosition(items, anchor, parent)
            val id = runCatching { addItem(documentId, resolvedParent, trimmed, position) }.getOrNull() ?: ""
            onDone(id)
        }
    }

    /**
     * Inserts a copy of [itemId] as its next sibling, excluding descendants.
     * [onDone] receives the new item id, or blank on failure.
     */
    fun duplicateItem(
        documentId: String,
        itemId: String,
        dateOffsetDays: Int,
        clearDates: Boolean,
        onDone: (String) -> Unit
    ) {
        scope.launch {
            val items = latestTrees[documentId]?.flatItems.orEmpty()
            val newId = runCatching {
                duplicateItemUseCase(items, itemId, dateOffsetDays, clearDates)
            }.getOrNull() ?: ""
            onDone(newId)
        }
    }

    fun saveItemText(itemId: String, text: String) {
        scope.launch { runCatching { updateItemText(itemId, text) } }
    }

    fun saveItemNote(itemId: String, note: String?) {
        scope.launch { runCatching { updateItemNote(itemId, note?.take(NESTED_NOTE_MAX_LENGTH)) } }
    }

    fun saveItemLabel(itemId: String, label: String?) {
        scope.launch { runCatching { updateItemLabel(itemId, label) } }
    }

    /** Shared predefined labels so Swift and Android offer the same choices. */
    fun predefinedLabels(): List<String> = PredefinedLabels

    // --- Formatting & metadata (same use cases as Android) ---

    fun updateFormatting(itemId: String, style: NestedTextStyle, textColor: NestedColorToken, backgroundColor: NestedColorToken) {
        scope.launch { runCatching { updateItemFormatting(itemId, style, textColor, backgroundColor) } }
    }

    /** String-based overloads so Swift never touches reserved-word enum cases. */
    fun updateFormattingByName(itemId: String, styleName: String, textColorName: String, backgroundColorName: String) {
        scope.launch {
            runCatching {
                updateItemFormatting(
                    itemId,
                    NestedTextStyle.valueOf(styleName),
                    NestedColorToken.valueOf(textColorName),
                    NestedColorToken.valueOf(backgroundColorName)
                )
            }
        }
    }

    /** Border shape/style/color/width, string-based for the SwiftUI editor. Null width inherits the preset. */
    fun updateBorderByName(itemId: String, shapeName: String, styleName: String, borderColorName: String, borderWidthName: String?) {
        scope.launch {
            runCatching {
                updateItemBorder(
                    itemId,
                    NestedBorderShape.valueOf(shapeName),
                    NestedBorderStyle.valueOf(styleName),
                    NestedColorToken.valueOf(borderColorName),
                    borderWidthName?.let { NestedBorderWidth.valueOf(it) }
                )
            }
        }
    }

    /** Callout style plus optional title/icon overrides; blank title means default. */
    fun updateCalloutByName(
        itemId: String,
        calloutName: String,
        calloutTitle: String?,
        calloutIcon: String?
    ) {
        scope.launch {
            runCatching {
                val title = calloutTitle?.trim()?.takeIf { it.isNotBlank() }
                updateItemCallout(
                    itemId,
                    NestedCalloutStyle.valueOf(calloutName),
                    title,
                    calloutIcon
                )
            }
        }
    }

    /** Callout layout override; null or invalid string resets to preset default. */
    fun updateCalloutLayoutByName(itemId: String, layoutName: String?) {
        scope.launch {
            runCatching {
                val layout = layoutName?.let { runCatching { NestedCalloutLayout.valueOf(it) }.getOrNull() }
                updateItemCalloutLayout(itemId, layout)
            }
        }
    }

    /** Clears item visual overrides so the selected callout preset shows through. */
    fun resetCalloutToDefaults(itemId: String) {
        scope.launch { runCatching { resetItemCalloutToDefaults(itemId) } }
    }

    /**
     * Dates cross the bridge as epoch days (LocalDate.toEpochDays()).
     * Pass Int.MIN_VALUE to clear one side.
     */
    fun updateDateRangeDays(itemId: String, startEpochDays: Int, endEpochDays: Int) {
        scope.launch {
            runCatching {
                updateItemDateRange(
                    itemId,
                    startEpochDays.takeIf { it != Int.MIN_VALUE }?.let { LocalDate.fromEpochDays(it) },
                    endEpochDays.takeIf { it != Int.MIN_VALUE }?.let { LocalDate.fromEpochDays(it) }
                )
            }
        }
    }

    fun updatePriority(itemId: String, priority: TaskPriority) {
        scope.launch { runCatching { updateItemPriority(itemId, priority) } }
    }

    fun updatePriorityByName(itemId: String, priorityName: String) {
        scope.launch { runCatching { updateItemPriority(itemId, TaskPriority.valueOf(priorityName)) } }
    }

    /** Guide-dot emoji; pass null (or blank) to restore the default dot. */
    fun updateGuideDot(itemId: String, guideDot: String?) {
        scope.launch { runCatching { updateItemGuideDot(itemId, guideDot) } }
    }

    /** Nullable token override for the row's dot and guide segments. */
    fun updateGuideDotColorByName(itemId: String, guideDotColorName: String?) {
        scope.launch {
            runCatching {
                updateItemGuideDotColor(
                    itemId,
                    guideDotColorName?.let { NestedColorToken.valueOf(it) }?.takeIf { it != NestedColorToken.Default }
                )
            }
        }
    }

    /** Numbers the row's direct children instead of dots. */
    fun setNumberedChildren(itemId: String, numberedChildren: Boolean) {
        scope.launch { runCatching { updateItemNumberedChildren(itemId, numberedChildren) } }
    }

    /** Numbers the document's root level instead of dots. */
    fun setDocumentNumberRoots(documentId: String, numberRoots: Boolean) {
        scope.launch { runCatching { updateDocumentNumberRoots(documentId, numberRoots) } }
    }

    fun updateTags(itemId: String, tagIds: List<String>) {
        scope.launch { runCatching { updateItemTags(itemId, tagIds) } }
    }

    /** Toggles one tag using the cached tree, so Swift only passes plain ids. */
    fun setTagEnabled(itemId: String, tagId: String, enabled: Boolean) {
        scope.launch {
            val current = latestTrees.values
                .firstNotNullOfOrNull { it.itemById[itemId] }
                ?.tags.orEmpty().map { it.id }.toMutableSet()
            if (enabled) current.add(tagId) else current.remove(tagId)
            runCatching { updateItemTags(itemId, current.toList()) }
        }
    }

    fun updateMetricSettings(itemId: String, actualMinutes: Int, policy: MetricRollupPolicy, showTracked: Boolean) {
        scope.launch { runCatching { updateItemMetricSettings(itemId, actualMinutes, policy, showTracked) } }
    }

    fun updateMetricSettingsByName(itemId: String, actualMinutes: Int, policyName: String, showTracked: Boolean) {
        scope.launch {
            runCatching {
                updateItemMetricSettings(
                    itemId,
                    actualMinutes,
                    MetricRollupPolicy.valueOf(policyName),
                    showTracked
                )
            }
        }
    }

    /** Pass -1 progress to hide the progress UI. */
    fun updateProgressValue(itemId: String, progress: Int) {
        scope.launch {
            runCatching { updateItemProgress(itemId, progress.takeIf { it >= 0 }) }
        }
    }

    fun replaceMetrics(itemId: String, metrics: List<MetricItem>) {
        scope.launch { runCatching { replaceManualMetrics(itemId, metrics) } }
    }

    /**
     * JSON-based metric replace so Swift builds payloads with
     * JSONSerialization instead of Kotlin enum constructors.
     * Shape matches MetricItem: [{"name":"","value":"","targetValue":null,
     * "unit":"None","customUnit":null,"sortOrder":0,"enabled":true,
     * "isCompleted":false}]. Empty drafts are dropped (blank values, or
     * date-based units without a due date).
     */
    fun replaceMetricsJson(itemId: String, json: String) {
        scope.launch {
            runCatching {
                val decoded = Json.decodeFromString<List<MetricItem>>(json)
                replaceManualMetrics(itemId, decoded.filter { it.isValidForSave() })
            }
        }
    }

    // --- Checkbox & structure ---

    fun toggleCheckboxEnabled(itemId: String, enabled: Boolean) {
        scope.launch { runCatching { setCheckboxEnabled(itemId, enabled) } }
    }

    /** Enables checkboxes on every direct child that does not already have one. */
    fun enableCheckboxForChildren(itemId: String) {
        scope.launch {
            val ids = latestTrees.values
                .firstNotNullOfOrNull { it.nodeById[itemId] }
                ?.children
                .orEmpty()
                .map { it.item }
                .filter { !it.checkboxEnabled }
                .map { it.id }
            ids.forEach { runCatching { setCheckboxEnabled(it, true) } }
        }
    }

    fun setChecked(itemIds: List<String>, checked: Boolean) {
        scope.launch { runCatching { setItemsChecked(itemIds, checked) } }
    }

    fun toggleChecked(itemId: String, checked: Boolean) {
        scope.launch { runCatching { setItemsChecked(listOf(itemId), checked) } }
    }

    fun toggleCollapsed(itemId: String) {
        scope.launch { runCatching { toggleCollapsedUseCase(itemId) } }
    }

    /**
     * Expand/collapse-all for Swift. [scopeIds] are scope roots resolved in
     * Swift (selection, else zoom focus); empty means the whole document.
     */
    fun expandSubtrees(documentId: String, scopeIds: List<String>) {
        scope.launch {
            val items = latestTrees[documentId]?.flatItems.orEmpty()
            runCatching { setItemsCollapsed(items, scopeIds, false) }
        }
    }

    fun collapseSubtrees(documentId: String, scopeIds: List<String>) {
        scope.launch {
            val items = latestTrees[documentId]?.flatItems.orEmpty()
            runCatching { setItemsCollapsed(items, scopeIds, true) }
        }
    }

    fun indent(documentId: String, itemId: String) {
        applyMove(documentId) { items -> moveItems.indent(items, itemId) }
    }

    fun outdent(documentId: String, itemId: String) {
        applyMove(documentId) { items -> moveItems.outdent(items, itemId) }
    }

    fun moveUp(documentId: String, itemId: String) {
        applyMove(documentId) { items -> moveItems.moveUp(items, itemId) }
    }

    fun moveDown(documentId: String, itemId: String) {
        applyMove(documentId) { items -> moveItems.moveDown(items, itemId) }
    }

    /** [targetParentId] blank drops at root. [targetIndex] is gap-based within the target group. */
    fun moveTo(documentId: String, itemId: String, targetParentId: String, targetIndex: Int) {
        applyMove(documentId) { items ->
            moveItems.moveToPosition(items, itemId, targetParentId.takeIf { it.isNotEmpty() }, targetIndex)
        }
    }

    /** Sorts the children of [parentId]; [orderName] is a [NestedSortOrder] name. */
    fun sortChildren(documentId: String, parentId: String, orderName: String) {
        val order = runCatching { NestedSortOrder.valueOf(orderName) }.getOrNull() ?: return
        applyMove(documentId) { items -> moveItems.sortChildren(items, parentId, order) }
    }

    fun moveDestinationGroups(documentId: String, itemId: String): MoveDestinations {
        val items = latestTrees[documentId]?.flatItems.orEmpty()
        return moveDestinations(items, itemId)
    }

    /** Moves [itemId] under [destinationId], expanding it when collapsed. */
    fun moveUnder(documentId: String, itemId: String, destinationId: String) {
        val expand = latestTrees[documentId]?.nodeById?.get(destinationId)?.item?.collapsed == true
        applyMove(documentId) { items -> moveItems.moveUnder(items, itemId, destinationId) }
        if (expand) {
            scope.launch { runCatching { toggleCollapsedUseCase(destinationId) } }
        }
    }

    fun deleteItemList(itemIds: List<String>) {
        scope.launch { runCatching { deleteItems(itemIds) } }
    }

    fun deleteSingleItem(itemId: String) {
        scope.launch { runCatching { deleteItems(listOf(itemId)) } }
    }

    // --- Subtree clipboard ---

    /** True when the shared in-memory clipboard holds a structural subtree. */
    fun canPasteSubtree(): Boolean = subtreeClipboard.clip.value != null

    /**
     * Copies [itemId]'s subtree in [documentId] into the shared clipboard.
     * [onDone] receives the copied row count (0 on failure), on the Main thread.
     */
    fun captureSubtree(documentId: String, itemId: String, onDone: (Int) -> Unit) {
        val tree = latestTrees[documentId]
        val clip = tree?.let { copySubtreeUseCase(it, itemId) }
        onDone(clip?.itemCount ?: 0)
    }

    /**
     * Pastes the clipboard's subtree as the last child of [parentId], or at
     * the document root when [parentId] is blank. Expands a collapsed target
     * so the paste is visible. [onDone] receives the inserted row count.
     */
    fun pasteSubtree(
        documentId: String,
        parentId: String,
        dateOffsetDays: Int,
        onDone: (Int) -> Unit
    ) {
        scope.launch {
            val tree = latestTrees[documentId]
            val parent = parentId.takeIf { it.isNotEmpty() }
            val count = runCatching {
                pasteSubtreeUseCase(documentId, parent, tree?.flatItems.orEmpty(), dateOffsetDays)
            }.getOrDefault(0)
            if (count > 0 && parent != null && tree?.nodeById?.get(parent)?.item?.collapsed == true) {
                runCatching { toggleCollapsedUseCase(parent) }
            }
            onDone(count)
        }
    }

    private fun applyMove(documentId: String, planner: (List<NestedListItem>) -> List<com.checkit.domain.NestedItemMove>) {
        val items = latestTrees[documentId]?.flatItems ?: return
        val moves = planner(items)
        if (moves.isEmpty()) return
        scope.launch {
            moveMutex.withLock { moveItems(moves) }
        }
    }

    // --- Read helpers for Swift (avoid Map<String, ...> interop friction) ---

    fun summaryFor(tree: NestedDocumentTree, itemId: String): NestedMetricSummary =
        tree.metricSummaryById[itemId] ?: NestedMetricSummary()

    fun findItem(tree: NestedDocumentTree, itemId: String): NestedListItem? =
        tree.itemById[itemId]

    fun findNode(tree: NestedDocumentTree, itemId: String): NestedItemNode? =
        tree.nodeById[itemId]

    fun exportTreeToMarkdown(tree: NestedDocumentTree, includeTitle: Boolean = false): String =
        tree.toMarkdown(NestedMarkdownExportOptions(includeDocumentTitle = includeTitle))

    fun exportNodeToMarkdown(node: NestedItemNode, includeNotes: Boolean = true): String =
        node.toMarkdown(NestedMarkdownExportOptions(includeNotes = includeNotes))

    fun exportDocumentToMarkdown(documentId: String, includeTitle: Boolean = false): String {
        val tree = latestTrees[documentId] ?: return ""
        return tree.toMarkdown(NestedMarkdownExportOptions(includeDocumentTitle = includeTitle))
    }

    fun exportDocumentToJson(documentId: String, onComplete: (String?, String?) -> Unit) {
        scope.launch(Dispatchers.Default) {
            val result = runCatching { exportDocumentJsonUseCase(documentId) }
            withContext(Dispatchers.Main) {
                result.onSuccess { json ->
                    onComplete(json, null)
                }.onFailure { err ->
                    onComplete(null, err.message ?: "Export failed")
                }
            }
        }
    }

    fun importDocumentFromJson(json: String, onComplete: (String?, String?) -> Unit) {
        scope.launch(Dispatchers.Default) {
            val result = runCatching { importDocumentJsonUseCase(json) }
            withContext(Dispatchers.Main) {
                result.onSuccess { docId ->
                    onComplete(docId, null)
                }.onFailure { err ->
                    onComplete(null, err.message ?: "Import failed")
                }
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}

/**
 * Closed date window in epoch days, exposed to Swift so the UI never has to
 * reimplement Kotlin's calendar-period arithmetic.
 */
class NestedAppleDateWindow internal constructor(
    val startEpochDays: Int,
    val endEpochDays: Int
)

/**
 * Epoch-day pair produced by the shared range-ordering helpers. Swift reads
 * [Int.MIN_VALUE] from either side as "unset", matching
 * `updateDateRangeDays`.
 */
class NestedAppleDateRange internal constructor(
    val startEpochDays: Int,
    val endEpochDays: Int
)

object NestedAppleBridge {
    private var koinApp: KoinApplication? = null

    fun ensureKoin() {
        QuickNoteAppleBridge.ensureKoin()
        if (KoinPlatform.getKoinOrNull() == null) {
            koinApp = runCatching { initKoin() }.getOrNull()
        }
    }

    fun helper(): NestedAppleHelper =
        koinApp?.koin?.get() ?: KoinPlatform.getKoin().get()

    /**
     * Swift-friendly outline projection for the macOS Working/search UI:
     * title query, the Working filter and an optional closed date window, with
     * the same ancestor-context keep semantics as the Compose editor. The
     * shared filter matches item titles only (notes are excluded).
     *
     * Pass [Int.MIN_VALUE] for both window bounds to filter by text only. The
     * bounds are epoch days (`LocalDate.toEpochDays()`); a start-only or
     * end-only node participates through the shared open-ended matching rules.
     * [overdueBeforeEpochDays] carries the [NestedRollingPreset.Overdue]
     * predicate, which has no closed window of its own. [todayEpochDays] lets
     * the Working filter drop overdue items; pass [Int.MIN_VALUE] to skip that
     * check.
     */
    fun filterOutlineProjection(
        roots: List<NestedItemNode>,
        query: String = "",
        workingOnly: Boolean = false,
        windowStartEpochDays: Int = Int.MIN_VALUE,
        windowEndEpochDays: Int = Int.MIN_VALUE,
        overdueBeforeEpochDays: Int = Int.MIN_VALUE,
        todayEpochDays: Int = Int.MIN_VALUE
    ): NestedOutlineProjection {
        val hasWindow =
            windowStartEpochDays != Int.MIN_VALUE && windowEndEpochDays != Int.MIN_VALUE
        return filterNestedTreeWithContext(
            roots = roots,
            start = if (hasWindow) LocalDate.fromEpochDays(windowStartEpochDays) else null,
            end = if (hasWindow) LocalDate.fromEpochDays(windowEndEpochDays) else null,
            query = query,
            workingOnly = workingOnly,
            overdueBefore = overdueBeforeEpochDays
                .takeIf { it != Int.MIN_VALUE }
                ?.let(LocalDate::fromEpochDays),
            today = todayEpochDays
                .takeIf { it != Int.MIN_VALUE }
                ?.let(LocalDate::fromEpochDays)
        )
    }

    /**
     * Resolves a [NestedDatePreset] name for Swift using the device-local
     * [todayEpochDays]. Keeping the period math in Kotlin means macOS and
     * Android always agree on week starts and month boundaries.
     */
    fun datePresetWindow(presetName: String, todayEpochDays: Int): NestedAppleDateWindow? {
        val preset = NestedDatePreset.entries.firstOrNull { it.name == presetName } ?: return null
        val window = preset.window(LocalDate.fromEpochDays(todayEpochDays))
        return window.toAppleWindow()
    }

    /**
     * Resolves a [NestedRollingPreset] name ("Last7Days", "Next7Days", …) for
     * Swift. Rolling windows are local attention views, not calendar periods.
     */
    fun rollingWindow(presetName: String, todayEpochDays: Int): NestedAppleDateWindow? {
        val preset = NestedRollingPreset.entries.firstOrNull { it.name == presetName } ?: return null
        return preset.window(LocalDate.fromEpochDays(todayEpochDays))?.toAppleWindow()
    }

    /**
     * Orders a newly picked start against the current end for the macOS range
     * picker through `com.checkit.ui.orderedRangeForStart`, using the same
     * rule as the Compose picker: a start after the end clears the end, while
     * equal days stay valid because the end is inclusive. Pass
     * [Int.MIN_VALUE] for an unset side.
     */
    fun orderedRangeForStart(startEpochDays: Int, endEpochDays: Int): NestedAppleDateRange =
        com.checkit.ui.orderedRangeForStart(
            startEpochDays.toLocalDateOrNull(),
            endEpochDays.toLocalDateOrNull()
        ).toAppleDateRange()

    /**
     * Mirror of `orderedRangeForStart` for a newly picked end: an end before
     * the start clears the start, so the latest pick always survives.
     */
    fun orderedRangeForEnd(startEpochDays: Int, endEpochDays: Int): NestedAppleDateRange =
        com.checkit.ui.orderedRangeForEnd(
            startEpochDays.toLocalDateOrNull(),
            endEpochDays.toLocalDateOrNull()
        ).toAppleDateRange()

    private fun Int.toLocalDateOrNull(): LocalDate? =
        takeIf { it != Int.MIN_VALUE }?.let(LocalDate::fromEpochDays)

    private fun Pair<LocalDate?, LocalDate?>.toAppleDateRange() = NestedAppleDateRange(
        startEpochDays = first?.toEpochDays()?.toInt() ?: Int.MIN_VALUE,
        endEpochDays = second?.toEpochDays()?.toInt() ?: Int.MIN_VALUE
    )

    private fun NestedDateWindow.toAppleWindow() = NestedAppleDateWindow(
        startEpochDays = start.toEpochDays().toInt(),
        endEpochDays = endInclusive.toEpochDays().toInt()
    )

    fun syncBridge(): com.checkit.data.NestedSyncBridge =
        koinApp?.koin?.get() ?: KoinPlatform.getKoin().get()
}

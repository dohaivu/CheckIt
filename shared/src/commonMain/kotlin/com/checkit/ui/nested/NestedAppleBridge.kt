package com.checkit.ui.nested

import com.checkit.domain.MetricItem
import com.checkit.domain.MetricRollupPolicy
import com.checkit.domain.NestedColorToken
import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedMetricSummary
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.computeNestedInsertPosition
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
import com.checkit.domain.usecase.UpdateNestedItemProgressUseCase
import com.checkit.domain.usecase.UpdateNestedItemTagsUseCase
import com.checkit.domain.usecase.UpdateNestedItemTextUseCase
import com.checkit.infrastructure.initKoin
import com.checkit.ui.quicknote.QuickNoteAppleBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
 * Nullable Kotlin Long/Int parameters surface to Swift as KotlinLong?/
 * KotlinInt? (pass nil for null). Non-null ids map to Swift Int64.
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
    private val renameDocument: RenameNestedDocumentUseCase,
    private val deleteDocument: DeleteNestedDocumentUseCase,
    private val addItem: AddNestedItemUseCase,
    private val updateItemText: UpdateNestedItemTextUseCase,
    private val updateItemNote: UpdateNestedItemNoteUseCase,
    private val updateItemFormatting: UpdateNestedItemFormattingUseCase,
    private val updateItemDateRange: UpdateNestedItemDateRangeUseCase,
    private val updateItemPriority: UpdateNestedItemPriorityUseCase,
    private val updateItemTags: UpdateNestedItemTagsUseCase,
    private val updateItemMetricSettings: UpdateNestedItemMetricSettingsUseCase,
    private val updateItemProgress: UpdateNestedItemProgressUseCase,
    private val replaceManualMetrics: ReplaceNestedManualMetricsUseCase,
    private val setCheckboxEnabled: SetNestedItemCheckboxEnabledUseCase,
    private val setItemsChecked: SetNestedItemsCheckedUseCase,
    private val toggleCollapsed: ToggleNestedItemCollapsedUseCase,
    private val moveItems: MoveNestedItemsUseCase,
    private val deleteItems: DeleteNestedItemsUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val moveMutex = Mutex()

    /** Latest tree snapshot per observed document, for draft/move planning. */
    private val latestTrees = mutableMapOf<Long, NestedDocumentTree>()

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

    fun observeDocumentTree(documentId: Long, onUpdate: (NestedDocumentTree) -> Unit): NestedAppleSubscription {
        val job = scope.launch {
            observeTree(documentId).collect { tree ->
                latestTrees[documentId] = tree
                onUpdate(tree)
            }
        }
        return NestedAppleSubscription(job)
    }

    // --- Documents ---

    fun createDocument(title: String, onDone: (Long) -> Unit) {
        scope.launch {
            val id = runCatching { addDocument(title) }.getOrNull() ?: -1L
            onDone(id)
        }
    }

    fun renameDocument(documentId: Long, title: String) {
        scope.launch { runCatching { renameDocument(documentId, title) } }
    }

    fun removeDocument(documentId: Long) {
        scope.launch {
            runCatching { deleteDocument(documentId) }
            latestTrees.remove(documentId)
        }
    }

    // --- Items: add (mirrors NestedListsViewModel.commitNewItem position math) ---

    /**
     * Commits a new item. [anchorId]/[parentId] use -1 for null (root insert).
     * onDone receives the new item id (or -1 on failure/blank).
     */
    fun commitItem(
        documentId: Long,
        anchorId: Long,
        parentId: Long,
        text: String,
        onDone: (Long) -> Unit,
    ) {
        scope.launch {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                onDone(-1L)
                return@launch
            }
            val anchor: Long? = anchorId.takeIf { it >= 0 }
            val parent: Long? = parentId.takeIf { it >= 0 }
            val items = latestTrees[documentId]?.flatItems.orEmpty()
            val (resolvedParent, position) = computeNestedInsertPosition(items, anchor, parent)
            val id = runCatching { addItem(documentId, resolvedParent, trimmed, position) }.getOrNull() ?: -1L
            onDone(id)
        }
    }

    fun saveItemText(itemId: Long, text: String) {
        scope.launch { runCatching { updateItemText(itemId, text) } }
    }

    fun saveItemNote(itemId: Long, note: String?) {
        scope.launch { runCatching { updateItemNote(itemId, note?.take(2_000)) } }
    }

    // --- Formatting & metadata (same use cases as Android) ---

    fun updateFormatting(itemId: Long, style: NestedTextStyle, textColor: NestedColorToken, backgroundColor: NestedColorToken) {
        scope.launch { runCatching { updateItemFormatting(itemId, style, textColor, backgroundColor) } }
    }

    /** String-based overloads so Swift never touches reserved-word enum cases. */
    fun updateFormattingByName(itemId: Long, styleName: String, textColorName: String, backgroundColorName: String) {
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

    /**
     * Dates cross the bridge as epoch days (LocalDate.toEpochDays()).
     * Pass Int.MIN_VALUE to clear one side.
     */
    fun updateDateRangeDays(itemId: Long, startEpochDays: Int, endEpochDays: Int) {
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

    fun updatePriority(itemId: Long, priority: TaskPriority) {
        scope.launch { runCatching { updateItemPriority(itemId, priority) } }
    }

    fun updatePriorityByName(itemId: Long, priorityName: String) {
        scope.launch { runCatching { updateItemPriority(itemId, TaskPriority.valueOf(priorityName)) } }
    }

    fun updateTags(itemId: Long, tagIds: List<Long>) {
        scope.launch { runCatching { updateItemTags(itemId, tagIds) } }
    }

    /** Toggles one tag using the cached tree, so Swift only passes plain ids. */
    fun setTagEnabled(itemId: Long, tagId: Long, enabled: Boolean) {
        scope.launch {
            val current = latestTrees.values
                .firstNotNullOfOrNull { it.itemById[itemId] }
                ?.tags.orEmpty().map { it.id }.toMutableSet()
            if (enabled) current.add(tagId) else current.remove(tagId)
            runCatching { updateItemTags(itemId, current.toList()) }
        }
    }

    fun updateMetricSettings(itemId: Long, actualMinutes: Int, policy: MetricRollupPolicy, showTracked: Boolean) {
        scope.launch { runCatching { updateItemMetricSettings(itemId, actualMinutes, policy, showTracked) } }
    }

    fun updateMetricSettingsByName(itemId: Long, actualMinutes: Int, policyName: String, showTracked: Boolean) {
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
    fun updateProgressValue(itemId: Long, progress: Int) {
        scope.launch {
            runCatching { updateItemProgress(itemId, progress.takeIf { it >= 0 }) }
        }
    }

    fun replaceMetrics(itemId: Long, metrics: List<MetricItem>) {
        scope.launch { runCatching { replaceManualMetrics(itemId, metrics) } }
    }

    /**
     * JSON-based metric replace so Swift builds payloads with
     * JSONSerialization instead of Kotlin enum constructors.
     * Shape matches MetricItem: [{"name":"","value":"","targetValue":null,
     * "unit":"None","customUnit":null,"sortOrder":0,"enabled":true,
     * "isCompleted":false}]. Blank values are dropped.
     */
    fun replaceMetricsJson(itemId: Long, json: String) {
        scope.launch {
            runCatching {
                val decoded = Json.decodeFromString<List<MetricItem>>(json)
                replaceManualMetrics(itemId, decoded.filter { it.value.isNotBlank() })
            }
        }
    }

    // --- Checkbox & structure ---

    fun toggleCheckboxEnabled(itemId: Long, enabled: Boolean) {
        scope.launch { runCatching { setCheckboxEnabled(itemId, enabled) } }
    }

    fun setChecked(itemIds: List<Long>, checked: Boolean) {
        scope.launch { runCatching { setItemsChecked(itemIds, checked) } }
    }

    fun toggleChecked(itemId: Long, checked: Boolean) {
        scope.launch { runCatching { setItemsChecked(listOf(itemId), checked) } }
    }

    fun toggleCollapsed(itemId: Long) {
        scope.launch { runCatching { toggleCollapsed(itemId) } }
    }

    fun indent(documentId: Long, itemId: Long) {
        applyMove(documentId) { items -> moveItems.indent(items, itemId) }
    }

    fun outdent(documentId: Long, itemId: Long) {
        applyMove(documentId) { items -> moveItems.outdent(items, itemId) }
    }

    fun moveUp(documentId: Long, itemId: Long) {
        applyMove(documentId) { items -> moveItems.moveUp(items, itemId) }
    }

    fun moveDown(documentId: Long, itemId: Long) {
        applyMove(documentId) { items -> moveItems.moveDown(items, itemId) }
    }

    /** [targetParentId] -1 drops at root. [targetIndex] is gap-based within the target group. */
    fun moveTo(documentId: Long, itemId: Long, targetParentId: Long, targetIndex: Int) {
        applyMove(documentId) { items ->
            moveItems.moveToPosition(items, itemId, targetParentId.takeIf { it >= 0 }, targetIndex)
        }
    }

    fun deleteItemList(itemIds: List<Long>) {
        scope.launch { runCatching { deleteItems(itemIds) } }
    }

    fun deleteSingleItem(itemId: Long) {
        scope.launch { runCatching { deleteItems(listOf(itemId)) } }
    }

    private fun applyMove(documentId: Long, planner: (List<NestedListItem>) -> List<com.checkit.domain.NestedItemMove>) {
        val items = latestTrees[documentId]?.flatItems ?: return
        val moves = planner(items)
        if (moves.isEmpty()) return
        scope.launch {
            moveMutex.withLock { moveItems(moves) }
        }
    }

    // --- Read helpers for Swift (avoid Map<Long, ...> interop friction) ---

    fun summaryFor(tree: NestedDocumentTree, itemId: Long): NestedMetricSummary =
        tree.metricSummaryById[itemId] ?: NestedMetricSummary()

    fun findItem(tree: NestedDocumentTree, itemId: Long): NestedListItem? =
        tree.itemById[itemId]

    fun findNode(tree: NestedDocumentTree, itemId: Long): NestedItemNode? =
        tree.nodeById[itemId]

    fun close() {
        scope.cancel()
    }
}

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
}

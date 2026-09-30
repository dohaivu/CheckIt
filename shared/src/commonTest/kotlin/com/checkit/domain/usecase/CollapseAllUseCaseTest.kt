package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.NestedListItem
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollapseAllUseCaseTest {

    private fun item(
        id: String,
        parentId: String?,
        position: Int = 0,
        collapsed: Boolean = false
    ) = NestedListItem(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = position,
        text = "item $id",
        collapsed = collapsed,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    private fun tree() = listOf(
        item("root", null, 0),
        item("a", "root", 0),
        item("a1", "a", 0),
        item("b", "root", 1, collapsed = true),
        item("b1", "b", 0),
        item("leaf", null, 1),
    )

    @Test
    fun batchSelectionWinsScope() {
        val scope = resolveCollapseScopeRoots(
            items = tree(),
            selectedIds = setOf("b"),
            selectedItemId = "a",
            focusedItemId = null
        )
        assertEquals(listOf("b"), scope)
    }

    @Test
    fun selectedParentBeatsFocus() {
        val scope = resolveCollapseScopeRoots(
            items = tree(),
            selectedIds = emptySet(),
            selectedItemId = "a",
            focusedItemId = "b"
        )
        assertEquals(listOf("a"), scope)
    }

    @Test
    fun leafSelectionFallsBackToFocus() {
        val scope = resolveCollapseScopeRoots(
            items = tree(),
            selectedIds = emptySet(),
            selectedItemId = "leaf",
            focusedItemId = "b"
        )
        assertEquals(listOf("b"), scope)
    }

    @Test
    fun noSelectionOrFocusMeansWholeDocument() {
        val scope = resolveCollapseScopeRoots(
            items = tree(),
            selectedIds = emptySet(),
            selectedItemId = null,
            focusedItemId = null
        )
        assertTrue(scope.isEmpty())
    }

    @Test
    fun collapseWholeDocCollectsExpandedParentsOnly() {
        val ids = collapsibleIdsInScopes(tree(), emptyList(), collapsed = true)
        assertEquals(setOf("root", "a"), ids.toSet())
    }

    @Test
    fun expandWholeDocCollectsCollapsedParentsOnly() {
        val ids = collapsibleIdsInScopes(tree(), emptyList(), collapsed = false)
        assertEquals(listOf("b"), ids)
    }

    @Test
    fun scopedToSubtreeIncludingScopeRoot() {
        val ids = collapsibleIdsInScopes(tree(), listOf("root"), collapsed = true)
        assertEquals(setOf("root", "a"), ids.toSet())
    }

    @Test
    fun leavesNeverIncluded() {
        val ids = collapsibleIdsInScopes(
            listOf(item("lonely", null, collapsed = true)),
            emptyList(),
            collapsed = false
        )
        assertTrue(ids.isEmpty())
    }

    private class RecordingRepo(
        delegate: FakeCheckItRepository = FakeCheckItRepository()
    ) : CheckItRepository by delegate {
        val calls = mutableListOf<Pair<List<String>, Boolean>>()
        override suspend fun setNestedItemsCollapsed(itemIds: List<String>, collapsed: Boolean) {
            calls += itemIds to collapsed
        }
    }

    @Test
    fun useCaseCollapsesOnlyStaleParents() = runTest {
        val repo = RecordingRepo()
        SetNestedItemsCollapsedUseCase(repo)(tree(), listOf("root"), true)
        assertEquals(1, repo.calls.size)
        assertEquals(setOf("root", "a"), repo.calls.single().first.toSet())
        assertEquals(true, repo.calls.single().second)
    }

    @Test
    fun useCaseIsNoopWhenNothingStale() = runTest {
        val repo = RecordingRepo()
        SetNestedItemsCollapsedUseCase(repo)(tree(), listOf("leaf"), true)
        assertTrue(repo.calls.isEmpty())
    }
}

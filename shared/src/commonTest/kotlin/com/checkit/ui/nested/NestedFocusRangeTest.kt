package com.checkit.ui.nested

import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.buildNestedTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Focus selection is derived from the already-filtered pre-order rows, so a
 * subtree is one contiguous index range. These tests pin that contract and
 * the ancestor path used for the medium-emphasis tier.
 */
class NestedFocusRangeTest {

    private fun item(id: String, parentId: String? = null, position: Int = 0) = NestedListItem(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = position,
        text = id,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    private fun row(id: String, depth: Int, isVisible: Boolean = true): VisibleNestedRow =
        VisibleNestedRow(
            node = NestedItemNode(item = item(id)),
            depth = depth,
            isVisible = isVisible,
            isInCheckedBranch = false,
        )

    private fun treeOf(items: List<NestedListItem>): NestedDocumentTree =
        NestedDocumentTree(
            document = NestedDocument(
                id = "doc-1",
                title = "Doc",
                createdAtMillis = 0L,
                updatedAtMillis = 0L,
            ),
            rootNodes = buildNestedTree(items),
        )

    @Test
    fun noSelectionHasNoRange() {
        assertNull(focusRowRange(listOf(row("a", 0)), selectedId = null))
    }

    @Test
    fun selectionMissingFromProjectionHasNoRange() {
        // Filters/Working Mode/zoom can hide the selected row; focus suspends
        // rather than forcing the row back into the outline.
        assertNull(focusRowRange(listOf(row("a", 0), row("b", 0)), selectedId = "hidden"))
    }

    @Test
    fun selectedLeafCoversOnlyItsRow() {
        val rows = listOf(row("root", 0), row("leaf", 1), row("sibling", 1))
        assertEquals(1..1, focusRowRange(rows, selectedId = "leaf"))
    }

    @Test
    fun selectedParentCoversItsDescendantsOnly() {
        val rows = listOf(
            row("root", 0),
            row("branch", 1),
            row("leaf", 2),
            row("sibling", 1),
            row("other", 0),
        )
        assertEquals(1..2, focusRowRange(rows, selectedId = "branch"))
    }

    @Test
    fun collapsedRowsStillCountAsDescendants() {
        // flattenVisibleNodes keeps collapsed rows in the list with
        // isVisible=false for expand/collapse animation, so the range must
        // include them rather than stopping at the first hidden row.
        val rows = listOf(
            row("root", 0),
            row("hidden", 1, isVisible = false),
            row("hiddenLeaf", 2, isVisible = false),
            row("sibling", 0),
        )
        assertEquals(0..2, focusRowRange(rows, selectedId = "root"))
    }

    @Test
    fun tierClassifiesFocusedPathAndOutside() {
        val range = 2..4
        val ancestors = setOf("root", "branch")
        assertEquals(NestedFocusTier.Focused, focusTier(3, "leaf", range, ancestors))
        assertEquals(NestedFocusTier.Path, focusTier(0, "root", range, ancestors))
        assertEquals(NestedFocusTier.Outside, focusTier(1, "other", range, ancestors))
        assertEquals(NestedFocusTier.None, focusTier(1, "other", null, emptySet()))
    }

    @Test
    fun deepChainRangeDoesNotRecurse() {
        val depth = 10_000
        val rows = (0 until depth).map { index -> row("n$index", index) }
        assertEquals(0..(depth - 1), focusRowRange(rows, selectedId = "n0"))
        assertEquals(5_000..(depth - 1), focusRowRange(rows, selectedId = "n5000"))
    }

    @Test
    fun ancestorsOfLeafIncludeParents() {
        val tree = treeOf(
            listOf(
                item("root"),
                item("middle", parentId = "root"),
                item("leaf", parentId = "middle"),
            )
        )
        assertEquals(setOf("root", "middle"), focusAncestorIdsOf(tree, "leaf"))
    }

    @Test
    fun rootSelectionHasNoAncestors() {
        val tree = treeOf(listOf(item("root"), item("leaf", parentId = "root")))
        assertTrue(focusAncestorIdsOf(tree, "root").isEmpty())
    }

    @Test
    fun missingSelectionHasNoAncestors() {
        val tree = treeOf(listOf(item("root")))
        assertTrue(focusAncestorIdsOf(tree, "missing").isEmpty())
    }
}

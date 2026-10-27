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
 * subtree is one contiguous index range. These tests pin that contract, the
 * leaf-to-parent promotion that keeps siblings in context, the selection
 * spine, and the ancestor path used for the medium-emphasis tier.
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
        assertNull(focusRowRange(listOf(row("a", 0)), rootId = null))
    }

    @Test
    fun selectionMissingFromProjectionHasNoRange() {
        // Filters/Working Mode/zoom can hide the selected row; focus suspends
        // rather than forcing the row back into the outline.
        assertNull(focusRowRange(listOf(row("a", 0), row("b", 0)), rootId = "hidden"))
    }

    @Test
    fun collapsedHiddenSelectionHasNoRange() {
        // The row list keeps collapsed descendants with isVisible=false for
        // the animation. A hidden root must still suspend focus.
        val rows = listOf(row("root", 0), row("hidden", 1, isVisible = false))
        assertNull(focusRowRange(rows, rootId = "hidden"))
    }

    @Test
    fun rawLeafRangeCoversOnlyItsRow() {
        // Low-level range only; the editor passes the promoted parent root.
        val rows = listOf(row("root", 0), row("leaf", 1), row("sibling", 1))
        assertEquals(1..1, focusRowRange(rows, rootId = "leaf"))
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
        assertEquals(1..2, focusRowRange(rows, rootId = "branch"))
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
        assertEquals(0..2, focusRowRange(rows, rootId = "root"))
    }

    @Test
    fun leafPromotesFocusToVisibleParentSubtree() {
        val tree = treeOf(
            listOf(
                item("root"),
                item("parent", parentId = "root"),
                item("leaf", parentId = "parent"),
                item("sibling", parentId = "parent", position = 1),
            )
        )
        val rows = listOf(
            row("root", 0),
            row("parent", 1),
            row("leaf", 2),
            row("sibling", 2),
        )
        val focusRoot = selectionRootIdOf(rows, tree, "leaf")
        assertEquals("parent", focusRoot)
        // The parent's whole visible subtree is focused, not just the leaf.
        assertEquals(1..3, focusRowRange(rows, focusRoot))
        // Ancestors above the parent remain on the medium path tier.
        assertEquals(setOf("root"), focusAncestorIdsOf(tree, focusRoot))
    }

    @Test
    fun leafWithHiddenParentStaysOnLeaf() {
        val tree = treeOf(listOf(item("root"), item("leaf", parentId = "root")))
        assertEquals("leaf", selectionRootIdOf(listOf(row("leaf", 0)), tree, "leaf"))
    }

    @Test
    fun leafWithHiddenParentRowStaysOnLeaf() {
        // Android keeps collapsed descendants in the row list for animation;
        // a non-visible parent must not attract the focus root.
        val tree = treeOf(
            listOf(
                item("root"),
                item("parent", parentId = "root"),
                item("leaf", parentId = "parent"),
            )
        )
        val rows = listOf(
            row("root", 0),
            row("parent", 1, isVisible = false),
            row("leaf", 2, isVisible = false),
        )
        assertEquals("leaf", selectionRootIdOf(rows, tree, "leaf"))
    }

    @Test
    fun parentSelectionStaysOnItsOwnSubtree() {
        val tree = treeOf(
            listOf(
                item("root"),
                item("parent", parentId = "root"),
                item("leaf", parentId = "parent"),
            )
        )
        assertEquals(
            "parent",
            selectionRootIdOf(
                listOf(row("root", 0), row("parent", 1), row("leaf", 2)),
                tree,
                "parent"
            )
        )
    }

    @Test
    fun rootLeafStaysOnItself() {
        val tree = treeOf(listOf(item("root")))
        assertEquals("root", selectionRootIdOf(listOf(row("root", 0)), tree, "root"))
    }

    @Test
    fun missingSelectionHasNoFocusRoot() {
        val tree = treeOf(listOf(item("root")))
        assertNull(selectionRootIdOf(listOf(row("root", 0)), tree, "missing"))
    }

    @Test
    fun selectionBranchPromotesLeafAndCarriesRootDepth() {
        val tree = treeOf(
            listOf(
                item("root"),
                item("parent", parentId = "root"),
                item("leaf", parentId = "parent"),
                item("sibling", parentId = "parent", position = 1),
            )
        )
        val rows = listOf(
            row("root", 0),
            row("parent", 1),
            row("leaf", 2),
            row("sibling", 2),
        )
        val branch = selectionBranchOf(rows, tree, "leaf")
        // The leaf promotes to its parent; the spine rides the parent's
        // column through the parent's whole subtree.
        assertEquals("parent", branch?.rootId)
        assertEquals(1..3, branch?.range)
        assertEquals(1, branch?.depth)
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
        assertEquals(0..(depth - 1), focusRowRange(rows, rootId = "n0"))
        assertEquals(5_000..(depth - 1), focusRowRange(rows, rootId = "n5000"))
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

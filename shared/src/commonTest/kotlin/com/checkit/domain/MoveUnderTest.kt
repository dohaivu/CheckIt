package com.checkit.domain

import com.checkit.domain.usecase.MoveNestedItemsUseCase
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoveUnderTest {

    private val moves = MoveNestedItemsUseCase(FakeCheckItRepository())

    private fun item(id: String, parentId: String?, position: Int) = NestedListItem(
        id = id,
        documentId = "1",
        parentId = parentId,
        position = position,
        text = "item $id",
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    // root: p (children a, b), u (child c)
    private val items = listOf(
        item("p", null, 0),
        item("a", "p", 0),
        item("b", "p", 1),
        item("u", null, 1),
        item("c", "u", 0),
    )

    @Test
    fun destinationsListSiblingsThenParentSiblings() {
        val dest = moveDestinations(items, "a")
        assertEquals(listOf("b"), dest.siblings.map { it.id })
        assertEquals(listOf("u"), dest.parentSiblings.map { it.id })
        assertTrue(!dest.isEmpty)
    }

    @Test
    fun destinationsExcludeSelfAndParent() {
        val dest = moveDestinations(items, "p")
        assertEquals(listOf("u"), dest.siblings.map { it.id })
        assertTrue(dest.parentSiblings.isEmpty())
    }

    @Test
    fun destinationsEmptyForUnknownOrLoneRoot() {
        assertTrue(moveDestinations(items, "missing").isEmpty)
        assertTrue(moveDestinations(listOf(item("solo", null, 0)), "solo").isEmpty)
    }

    @Test
    fun moveUnderSiblingAppendsAsLastChild() {
        // c (child of u) under b: b gains c at index 0, u's group renormalizes.
        assertEquals(
            listOf(NestedItemMove("c", "b", 0)),
            moves.moveUnder(items, "c", "b")
        )
    }

    @Test
    fun moveUnderUncleAppendsAsLastChild() {
        // a (child of p) under u: u already has c at 0, so a lands at 1.
        assertEquals(
            listOf(
                NestedItemMove("b", "p", 0),
                NestedItemMove("a", "u", 1),
            ),
            moves.moveUnder(items, "a", "u")
        )
    }

    @Test
    fun alreadyLastChildIsNoop() {
        assertTrue(moves.moveUnder(items, "c", "u").isEmpty())
    }

    @Test
    fun unknownItemOrDestinationIsNoop() {
        assertTrue(moves.moveUnder(items, "missing", "u").isEmpty())
        assertTrue(moves.moveUnder(items, "a", "missing").isEmpty())
    }

    @Test
    fun destinationInsideOwnSubtreeIsNoop() {
        // a under its own (hypothetical) descendant is rejected by moveToPosition.
        val nested = items + item("a1", "a", 0)
        assertTrue(moves.moveUnder(nested, "a", "a1").isEmpty())
    }
}

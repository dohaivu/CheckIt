package com.checkit.domain.usecase

import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedItemMove
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoveToPositionTest {
    private val useCase = MoveNestedItemsUseCase(com.checkit.ui.tasks.FakeCheckItRepository())

    private fun item(id: Long, parentId: Long?, position: Int) = NestedListItem(
        id = id,
        documentId = 1L,
        parentId = parentId,
        position = position,
        text = "item-$id",
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    // Tree: 1(root), 2(root), 3(root); 2 has children 21, 22; 22 has child 221
    private val items = listOf(
        item(1L, null, 0),
        item(2L, null, 1),
        item(21L, 2L, 0),
        item(22L, 2L, 1),
        item(221L, 22L, 0),
        item(3L, null, 2)
    )

    @Test
    fun reordersWithinSameParentMovingDown() {
        val moves = useCase.moveToPosition(items, itemId = 1L, newParentId = null, newIndex = 1)
        // group excluding dragged: [2, 3]; insert at 1 -> [2, 1, 3]; only 2 and 1 change
        assertEquals(
            listOf(
                NestedItemMove(2L, null, 0),
                NestedItemMove(1L, null, 1)
            ),
            moves.sortedBy { it.position }
        )
    }

    @Test
    fun reordersWithinSameParentMovingUp() {
        val moves = useCase.moveToPosition(items, itemId = 3L, newParentId = null, newIndex = 0)
        // [3, 1, 2]; 1 and 2 shift down
        assertEquals(
            listOf(
                NestedItemMove(3L, null, 0),
                NestedItemMove(1L, null, 1),
                NestedItemMove(2L, null, 2)
            ),
            moves.sortedBy { it.position }
        )
    }

    @Test
    fun crossParentMoveAppendsAsLastChild() {
        val moves = useCase.moveToPosition(items, itemId = 3L, newParentId = 2L, newIndex = 2)
        // root loses 3 (no renormalize needed since 3 was last); children of 2 become [21, 22, 3]
        assertTrue(moves.contains(NestedItemMove(3L, 2L, 2)))
        assertTrue(moves.none { it.itemId == 21L || it.itemId == 22L })
    }

    @Test
    fun crossParentMoveInsertsAtIndexAndRenormalizesSource() {
        val moves = useCase.moveToPosition(items, itemId = 21L, newParentId = 3L, newIndex = 0)
        // source group [2] stays at position 1? After removing 21, roots are [1, 2, 3]; only child of 3 changes.
        assertTrue(moves.contains(NestedItemMove(21L, 3L, 0)))
        assertTrue(moves.none { it.itemId == 2L && it.parentId != null })
    }

    @Test
    fun rejectsDropIntoOwnDescendant() {
        val moves = useCase.moveToPosition(items, itemId = 2L, newParentId = 22L, newIndex = 0)
        assertTrue(moves.isEmpty())
        val selfMoves = useCase.moveToPosition(items, itemId = 2L, newParentId = 2L, newIndex = 0)
        assertTrue(selfMoves.isEmpty())
    }

    @Test
    fun noOpWhenAlreadyAtPlacement() {
        val moves = useCase.moveToPosition(items, itemId = 1L, newParentId = null, newIndex = 0)
        assertTrue(moves.isEmpty())
    }

    @Test
    fun unknownItemReturnsEmpty() {
        assertTrue(useCase.moveToPosition(items, itemId = 999L, newParentId = null, newIndex = 0).isEmpty())
    }
}

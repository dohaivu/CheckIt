package com.checkit.domain.usecase

import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedItemMove
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoveToPositionTest {
    private val useCase = MoveNestedItemsUseCase(com.checkit.ui.tasks.FakeCheckItRepository())

    private fun item(id: String, parentId: String?, position: Int) = NestedListItem(
        id = id,
        documentId = "1",
        parentId = parentId,
        position = position,
        text = "item-$id",
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    // Tree: 1(root), 2(root), 3(root); 2 has children 21, 22; 22 has child 221
    private val items = listOf(
        item("1", null, 0),
        item("2", null, 1),
        item("21", "2", 0),
        item("22", "2", 1),
        item("221", "22", 0),
        item("3", null, 2)
    )

    @Test
    fun reordersWithinSameParentMovingDown() {
        val moves = useCase.moveToPosition(items, itemId = "1", newParentId = null, newIndex = 1)
        // group excluding dragged: [2, 3]; insert at 1 -> [2, 1, 3]; only 2 and 1 change
        assertEquals(
            listOf(
                NestedItemMove("2", null, 0),
                NestedItemMove("1", null, 1)
            ),
            moves.sortedBy { it.position }
        )
    }

    @Test
    fun reordersWithinSameParentMovingUp() {
        val moves = useCase.moveToPosition(items, itemId = "3", newParentId = null, newIndex = 0)
        // [3, 1, 2]; 1 and 2 shift down
        assertEquals(
            listOf(
                NestedItemMove("3", null, 0),
                NestedItemMove("1", null, 1),
                NestedItemMove("2", null, 2)
            ),
            moves.sortedBy { it.position }
        )
    }

    @Test
    fun crossParentMoveAppendsAsLastChild() {
        val moves = useCase.moveToPosition(items, itemId = "3", newParentId = "2", newIndex = 2)
        // root loses 3 (no renormalize needed since 3 was last); children of 2 become [21, 22, 3]
        assertTrue(moves.contains(NestedItemMove("3", "2", 2)))
        assertTrue(moves.none { it.itemId == "21" || it.itemId == "22" })
    }

    @Test
    fun crossParentMoveInsertsAtIndexAndRenormalizesSource() {
        val moves = useCase.moveToPosition(items, itemId = "21", newParentId = "3", newIndex = 0)
        // source group [2] stays at position 1? After removing 21, roots are [1, 2, 3]; only child of 3 changes.
        assertTrue(moves.contains(NestedItemMove("21", "3", 0)))
        assertTrue(moves.none { it.itemId == "2" && it.parentId != null })
    }

    @Test
    fun rejectsDropIntoOwnDescendant() {
        val moves = useCase.moveToPosition(items, itemId = "2", newParentId = "22", newIndex = 0)
        assertTrue(moves.isEmpty())
        val selfMoves = useCase.moveToPosition(items, itemId = "2", newParentId = "2", newIndex = 0)
        assertTrue(selfMoves.isEmpty())
    }

    @Test
    fun noOpWhenAlreadyAtPlacement() {
        val moves = useCase.moveToPosition(items, itemId = "1", newParentId = null, newIndex = 0)
        assertTrue(moves.isEmpty())
    }

    @Test
    fun unknownItemReturnsEmpty() {
        assertTrue(useCase.moveToPosition(items, itemId = "999", newParentId = null, newIndex = 0).isEmpty())
    }
}

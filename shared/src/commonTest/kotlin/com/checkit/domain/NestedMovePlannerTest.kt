package com.checkit.domain

import com.checkit.domain.usecase.MoveNestedItemsUseCase
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NestedMovePlannerTest {

    private fun item(id: Long, parentId: Long?, position: Int) = NestedListItem(
        id = id,
        documentId = 1L,
        parentId = parentId,
        position = position,
        text = "item $id",
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    // root: 1, 2, 3 ; children of 2: 4, 5
    private val items = listOf(
        item(1, null, 0),
        item(2, null, 1),
        item(3, null, 2),
        item(4, 2, 0),
        item(5, 2, 1)
    )

    @Test
    fun moveDownWithinSameParentDoesNotThrow() {
        val moves = planNestedMoves(items, setOf(1L), targetParentId = null, targetIndex = 1)
        assertEquals(listOf(NestedItemMove(1L, null, 1)), moves)
    }

    @Test
    fun moveUpWithinSameParentDoesNotThrow() {
        val moves = planNestedMoves(items, setOf(2L), targetParentId = null, targetIndex = 0)
        assertEquals(listOf(NestedItemMove(2L, null, 0)), moves)
    }

    @Test
    fun moveChildWithinItsParentDoesNotThrow() {
        val moves = planNestedMoves(items, setOf(5L), targetParentId = 2L, targetIndex = 0)
        assertEquals(listOf(NestedItemMove(5L, 2L, 0)), moves)
    }

    @Test
    fun indentUnderPreviousSibling() {
        val moves = planNestedMoves(items, setOf(3L), targetParentId = 2L, targetIndex = 2)
        assertEquals(listOf(NestedItemMove(3L, 2L, 2)), moves)
    }

    @Test
    fun movingUnderOwnDescendantThrows() {
        assertFailsWith<IllegalArgumentException> {
            planNestedMoves(items, setOf(2L), targetParentId = 4L, targetIndex = 0)
        }
    }

    @Test
    fun movingIntoItselfThrows() {
        assertFailsWith<IllegalArgumentException> {
            planNestedMoves(items, setOf(2L), targetParentId = 2L, targetIndex = 0)
        }
    }

    @Test
    fun subtreeSelectionExcludesDescendants() {
        val moves = planNestedMoves(items, setOf(2L, 4L, 5L), targetParentId = 1L, targetIndex = 1)
        assertEquals(listOf(NestedItemMove(2L, 1L, 1)), moves)
    }

    @Test
    fun emptySelectionProducesNoMoves() {
        assertEquals(emptyList(), planNestedMoves(items, emptySet(), null, 0))
    }

    @Test
    fun moveUpToTopSwapsPositions() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val moves = useCase.moveUp(items, 3L)
        assertEquals(
            listOf(
                NestedItemMove(3L, null, 1),
                NestedItemMove(2L, null, 2)
            ),
            moves
        )
    }

    @Test
    fun moveDownToBottomSwapsPositions() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val moves = useCase.moveDown(items, 1L)
        assertEquals(
            listOf(
                NestedItemMove(1L, null, 1),
                NestedItemMove(2L, null, 0)
            ),
            moves
        )
    }

    @Test
    fun moveUpFirstItemIsNoOp() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        assertEquals(emptyList(), useCase.moveUp(items, 1L))
    }

    @Test
    fun moveDownLastItemIsNoOp() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        assertEquals(emptyList(), useCase.moveDown(items, 3L))
    }

    @Test
    fun moveChildWithinParentSwapsPositions() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val moves = useCase.moveUp(items, 5L)
        assertEquals(
            listOf(
                NestedItemMove(5L, 2L, 0),
                NestedItemMove(4L, 2L, 1)
            ),
            moves
        )
    }

    @Test
    fun outdentPlacesItemRightAfterParentWithRenormalizedPositions() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val moves = useCase.outdent(items, 4L)
        assertEquals(
            listOf(
                NestedItemMove(5L, 2L, 0),
                NestedItemMove(4L, null, 2),
                NestedItemMove(3L, null, 3)
            ),
            moves
        )
    }

    @Test
    fun indentAppendsAsLastChildOfPreviousSibling() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val moves = useCase.indent(items, 3L)
        assertEquals(
            listOf(NestedItemMove(3L, 2L, 2)),
            moves
        )
    }

    @Test
    fun indentRenormalizesTheSourceAndTargetSiblingGroups() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val sparse = items.map { item ->
            when (item.id) {
                2L -> item.copy(position = 3)
                3L -> item.copy(position = 8)
                4L -> item.copy(position = 4)
                5L -> item.copy(position = 9)
                else -> item
            }
        }

        assertEquals(
            listOf(
                NestedItemMove(2L, null, 1),
                NestedItemMove(4L, 2L, 0),
                NestedItemMove(5L, 2L, 1),
                NestedItemMove(3L, 2L, 2)
            ),
            useCase.indent(sparse, 3L)
        )
    }

    @Test
    fun outdentRenormalizesTheSourceSiblingGroup() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val sparse = items.map { item ->
            if (item.id == 5L) item.copy(position = 7) else item
        }

        assertEquals(
            listOf(
                NestedItemMove(5L, null, 2),
                NestedItemMove(3L, null, 3)
            ),
            useCase.outdent(sparse, 5L)
        )
    }
}

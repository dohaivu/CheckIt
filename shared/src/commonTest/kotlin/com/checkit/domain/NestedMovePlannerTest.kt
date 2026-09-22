package com.checkit.domain

import com.checkit.domain.usecase.MoveNestedItemsUseCase
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NestedMovePlannerTest {

    private fun item(id: String, parentId: String?, position: Int) = NestedListItem(
        id = id,
        documentId = "1",
        parentId = parentId,
        position = position,
        text = "item $id",
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    // root: 1, 2, 3 ; children of 2: 4, 5
    private val items = listOf(
        item("1", null, 0),
        item("2", null, 1),
        item("3", null, 2),
        item("4", "2", 0),
        item("5", "2", 1)
    )

    @Test
    fun moveDownWithinSameParentDoesNotThrow() {
        val moves = planNestedMoves(items, setOf("1"), targetParentId = null, targetIndex = 1)
        assertEquals(listOf(NestedItemMove("1", null, 1)), moves)
    }

    @Test
    fun moveUpWithinSameParentDoesNotThrow() {
        val moves = planNestedMoves(items, setOf("2"), targetParentId = null, targetIndex = 0)
        assertEquals(listOf(NestedItemMove("2", null, 0)), moves)
    }

    @Test
    fun moveChildWithinItsParentDoesNotThrow() {
        val moves = planNestedMoves(items, setOf("5"), targetParentId = "2", targetIndex = 0)
        assertEquals(listOf(NestedItemMove("5", "2", 0)), moves)
    }

    @Test
    fun indentUnderPreviousSibling() {
        val moves = planNestedMoves(items, setOf("3"), targetParentId = "2", targetIndex = 2)
        assertEquals(listOf(NestedItemMove("3", "2", 2)), moves)
    }

    @Test
    fun movingUnderOwnDescendantThrows() {
        assertFailsWith<IllegalArgumentException> {
            planNestedMoves(items, setOf("2"), targetParentId = "4", targetIndex = 0)
        }
    }

    @Test
    fun movingIntoItselfThrows() {
        assertFailsWith<IllegalArgumentException> {
            planNestedMoves(items, setOf("2"), targetParentId = "2", targetIndex = 0)
        }
    }

    @Test
    fun subtreeSelectionExcludesDescendants() {
        val moves = planNestedMoves(items, setOf("2", "4", "5"), targetParentId = "1", targetIndex = 1)
        assertEquals(listOf(NestedItemMove("2", "1", 1)), moves)
    }

    @Test
    fun emptySelectionProducesNoMoves() {
        assertEquals(emptyList(), planNestedMoves(items, emptySet(), null, 0))
    }

    @Test
    fun moveUpToTopSwapsPositions() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val moves = useCase.moveUp(items, "3")
        assertEquals(
            listOf(
                NestedItemMove("3", null, 1),
                NestedItemMove("2", null, 2)
            ),
            moves
        )
    }

    @Test
    fun moveDownToBottomSwapsPositions() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val moves = useCase.moveDown(items, "1")
        assertEquals(
            listOf(
                NestedItemMove("2", null, 0),
                NestedItemMove("1", null, 1)
            ),
            moves
        )
    }

    @Test
    fun moveUpFirstItemIsNoOp() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        assertEquals(emptyList(), useCase.moveUp(items, "1"))
    }

    @Test
    fun moveDownLastItemIsNoOp() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        assertEquals(emptyList(), useCase.moveDown(items, "3"))
    }

    @Test
    fun moveChildWithinParentSwapsPositions() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val moves = useCase.moveUp(items, "5")
        assertEquals(
            listOf(
                NestedItemMove("5", "2", 0),
                NestedItemMove("4", "2", 1)
            ),
            moves
        )
    }

    @Test
    fun moveUpRenormalizesDuplicatePositionsSoTheOrderActuallyChanges() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val duplicatePositions = items.map { item ->
            if (item.id == "2" || item.id == "3") item.copy(position = 1) else item
        }

        assertEquals(
            listOf(
                NestedItemMove("2", null, 2)
            ),
            useCase.moveUp(duplicatePositions, "3")
        )
    }

    @Test
    fun outdentPlacesItemRightAfterParentWithRenormalizedPositions() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val moves = useCase.outdent(items, "4")
        assertEquals(
            listOf(
                NestedItemMove("5", "2", 0),
                NestedItemMove("4", null, 2),
                NestedItemMove("3", null, 3)
            ),
            moves
        )
    }

    @Test
    fun indentAppendsAsLastChildOfPreviousSibling() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val moves = useCase.indent(items, "3")
        assertEquals(
            listOf(NestedItemMove("3", "2", 2)),
            moves
        )
    }

    @Test
    fun indentRenormalizesTheSourceAndTargetSiblingGroups() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val sparse = items.map { item ->
            when (item.id) {
                "2" -> item.copy(position = 3)
                "3" -> item.copy(position = 8)
                "4" -> item.copy(position = 4)
                "5" -> item.copy(position = 9)
                else -> item
            }
        }

        assertEquals(
            listOf(
                NestedItemMove("2", null, 1),
                NestedItemMove("4", "2", 0),
                NestedItemMove("5", "2", 1),
                NestedItemMove("3", "2", 2)
            ),
            useCase.indent(sparse, "3")
        )
    }

    @Test
    fun outdentRenormalizesTheSourceSiblingGroup() {
        val useCase = MoveNestedItemsUseCase(FakeCheckItRepository())
        val sparse = items.map { item ->
            if (item.id == "5") item.copy(position = 7) else item
        }

        assertEquals(
            listOf(
                NestedItemMove("5", null, 2),
                NestedItemMove("3", null, 3)
            ),
            useCase.outdent(sparse, "5")
        )
    }
}

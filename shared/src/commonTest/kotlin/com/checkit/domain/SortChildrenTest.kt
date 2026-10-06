package com.checkit.domain

import com.checkit.domain.usecase.MoveNestedItemsUseCase
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SortChildrenTest {

    private val moves = MoveNestedItemsUseCase(FakeCheckItRepository())

    private fun item(
        id: String,
        parentId: String?,
        position: Int,
        text: String = "item $id",
        createdAtMillis: Long = 0L,
        completedAtMillis: Long? = null,
        checked: Boolean = false,
        priority: TaskPriority = TaskPriority.None
    ) = NestedListItem(
        id = id,
        documentId = "1",
        parentId = parentId,
        position = position,
        text = text,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = 0L,
        completedAtMillis = completedAtMillis,
        checked = checked,
        priority = priority
    )

    @Test
    fun nameAscSortsCaseInsensitively() {
        val items = listOf(
            item("a", "p", 0, text = "Charlie"),
            item("b", "p", 1, text = "apple"),
            item("c", "p", 2, text = "Banana"),
            item("p", null, 0),
        )
        assertEquals(
            listOf(
                NestedItemMove("b", "p", 0),
                NestedItemMove("c", "p", 1),
                NestedItemMove("a", "p", 2),
            ),
            moves.sortChildren(items, "p", NestedSortOrder.NameAsc)
        )
    }

    @Test
    fun alreadySortedIsNoop() {
        val items = listOf(
            item("a", "p", 0, text = "a"),
            item("b", "p", 1, text = "b"),
            item("p", null, 0),
        )
        assertTrue(moves.sortChildren(items, "p", NestedSortOrder.NameAsc).isEmpty())
    }

    @Test
    fun addedDescPutsNewestFirst() {
        val items = listOf(
            item("a", "p", 0, createdAtMillis = 100L),
            item("b", "p", 1, createdAtMillis = 300L),
            item("c", "p", 2, createdAtMillis = 200L),
            item("p", null, 0),
        )
        assertEquals(
            listOf(
                NestedItemMove("b", "p", 0),
                NestedItemMove("c", "p", 1),
                NestedItemMove("a", "p", 2),
            ),
            moves.sortChildren(items, "p", NestedSortOrder.AddedDesc)
        )
    }

    @Test
    fun completedDescPutsLatestFirstAndNullsLast() {
        val items = listOf(
            item("a", "p", 0, completedAtMillis = null),
            item("b", "p", 1, completedAtMillis = 100L),
            item("c", "p", 2, completedAtMillis = 300L),
            item("p", null, 0),
        )
        assertEquals(
            listOf(
                NestedItemMove("c", "p", 0),
                NestedItemMove("a", "p", 2),
            ),
            moves.sortChildren(items, "p", NestedSortOrder.CompletedDesc)
        )
    }

    @Test
    fun incompleteFirstKeepsStableOrderWithinGroups() {
        val items = listOf(
            item("a", "p", 0, checked = true),
            item("b", "p", 1, checked = false),
            item("c", "p", 2, checked = true),
            item("d", "p", 3, checked = false),
            item("p", null, 0),
        )
        assertEquals(
            listOf(
                NestedItemMove("b", "p", 0),
                NestedItemMove("d", "p", 1),
                NestedItemMove("a", "p", 2),
                NestedItemMove("c", "p", 3),
            ),
            moves.sortChildren(items, "p", NestedSortOrder.IncompleteFirst)
        )
    }

    @Test
    fun priorityDescPutsHighFirstAndNoneLast() {
        val items = listOf(
            item("a", "p", 0, priority = TaskPriority.None),
            item("b", "p", 1, priority = TaskPriority.Low),
            item("c", "p", 2, priority = TaskPriority.High),
            item("d", "p", 3, priority = TaskPriority.Medium),
            item("p", null, 0),
        )
        assertEquals(
            listOf(
                NestedItemMove("c", "p", 0),
                NestedItemMove("d", "p", 1),
                NestedItemMove("b", "p", 2),
                NestedItemMove("a", "p", 3),
            ),
            moves.sortChildren(items, "p", NestedSortOrder.PriorityDesc)
        )
    }

    @Test
    fun sparsePositionsRenormalizeToContiguous() {
        val items = listOf(
            item("a", "p", 5, text = "b"),
            item("b", "p", 9, text = "a"),
            item("p", null, 0),
        )
        assertEquals(
            listOf(
                NestedItemMove("b", "p", 0),
                NestedItemMove("a", "p", 1),
            ),
            moves.sortChildren(items, "p", NestedSortOrder.NameAsc)
        )
    }

    @Test
    fun unknownChildlessOrSingleChildParentIsNoop() {
        val items = listOf(
            item("a", "p", 0),
            item("p", null, 0),
        )
        assertTrue(moves.sortChildren(items, "missing", NestedSortOrder.NameAsc).isEmpty())
        assertTrue(moves.sortChildren(items, "a", NestedSortOrder.NameAsc).isEmpty())
        assertTrue(moves.sortChildren(items, null, NestedSortOrder.NameAsc).isEmpty())
    }

    @Test
    fun nullParentSortsRoots() {
        val items = listOf(
            item("b", null, 0, text = "b"),
            item("a", null, 1, text = "a"),
        )
        assertEquals(
            listOf(
                NestedItemMove("a", null, 0),
                NestedItemMove("b", null, 1),
            ),
            moves.sortChildren(items, null, NestedSortOrder.NameAsc)
        )
    }
}

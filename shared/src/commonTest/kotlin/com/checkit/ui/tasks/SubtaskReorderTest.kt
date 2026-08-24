package com.checkit.ui.tasks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubtaskReorderTest {

    private fun itemBounds(top: Float, heightPx: Int) = SubtaskRowBounds(top, heightPx)

    @Test
    fun noDragReturnsNull() {
        val items = listOf(
            1L to itemBounds(0f, 40),
            2L to itemBounds(44f, 40),
            3L to itemBounds(88f, 40)
        )
        val target = findSubtaskReorderTarget(
            draggedKey = 1L,
            draggedDelta = 0f,
            items = items
        )
        assertNull(target)
    }

    @Test
    fun dragDownPassesCenterOfNextItemSwaps() {
        val items = listOf(
            1L to itemBounds(0f, 40), // center = 20
            2L to itemBounds(44f, 40), // center = 64
            3L to itemBounds(88f, 40)  // center = 108
        )
        // Delta = 45 -> visual center = 20 + 45 = 65 > 64
        val target = findSubtaskReorderTarget(
            draggedKey = 1L,
            draggedDelta = 45f,
            items = items
        )
        assertEquals(1, target)
    }

    @Test
    fun dragDownNotPassingCenterDoesNotSwap() {
        val items = listOf(
            1L to itemBounds(0f, 40), // center = 20
            2L to itemBounds(44f, 40), // center = 64
            3L to itemBounds(88f, 40)  // center = 108
        )
        // Delta = 30 -> visual center = 20 + 30 = 50 < 64
        val target = findSubtaskReorderTarget(
            draggedKey = 1L,
            draggedDelta = 30f,
            items = items
        )
        assertNull(target)
    }

    @Test
    fun dragUpPassesCenterOfPrevItemSwaps() {
        val items = listOf(
            1L to itemBounds(0f, 40), // center = 20
            2L to itemBounds(44f, 40), // center = 64
            3L to itemBounds(88f, 40)  // center = 108
        )
        // Item 2 is dragged up: current center = 64, delta = -50 -> visual center = 14 < 20
        val target = findSubtaskReorderTarget(
            draggedKey = 2L,
            draggedDelta = -50f,
            items = items
        )
        assertEquals(0, target)
    }

    @Test
    fun fastDragAcrossMultipleItemsStepsMultipleIndices() {
        val items = listOf(
            1L to itemBounds(0f, 40), // center = 20
            2L to itemBounds(44f, 40), // center = 64
            3L to itemBounds(88f, 40)  // center = 108
        )
        // Item 1 dragged down by 100px -> visual center = 20 + 100 = 120 > 108
        val target = findSubtaskReorderTarget(
            draggedKey = 1L,
            draggedDelta = 100f,
            items = items
        )
        assertEquals(2, target)
    }

    @Test
    fun variableHeightItemsMaintainHysteresisWithoutOscillation() {
        // Item 1: height 40, Item 2 (multiline): height 100
        val items = listOf(
            1L to itemBounds(0f, 40),   // center = 20
            2L to itemBounds(44f, 100)  // center = 94
        )
        // Moving item 1 past center of item 2 (delta = 75 -> visual center = 95 > 94)
        val target1 = findSubtaskReorderTarget(
            draggedKey = 1L,
            draggedDelta = 75f,
            items = items
        )
        assertEquals(1, target1)

        // After swap, items are [2L, 1L] with updated layout positions:
        val swappedItems = listOf(
            2L to itemBounds(0f, 100), // center = 50
            1L to itemBounds(104f, 40) // center = 124
        )
        // Dragged item is now at index 1 with draggingOffset = (initialTop(0) + dragDelta(75) - layoutTop(104)) = -29
        // Visual center is 124 + (-29) = 95.
        // Since visual center 95 is > 50 (prev item center), targetIndex stays at 1!
        val target2 = findSubtaskReorderTarget(
            draggedKey = 1L,
            draggedDelta = -29f,
            items = swappedItems
        )
        assertNull(target2) // Stays at 1, no thrashing!
    }
}

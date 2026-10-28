package com.checkit.ui.nested

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.checkit.domain.NestedBorderShape
import com.checkit.domain.NestedBorderStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the geometry the row renderer relies on. Compose happily accepts a
 * [androidx.compose.ui.geometry.RoundRect] with only one rounded corner, so a
 * misplaced named argument would silently render three square corners; these
 * checks pin the symmetry down.
 *
 * Path-based shapes are not covered: building a Compose [androidx.compose.ui.graphics.Path]
 * needs Android graphics, which the host test runtime stubs out.
 */
class NestedRowShapesTest {

    private val density = Density(1f)
    private val size = Size(200f, 40f)

    @Test
    fun rectangleHasNoCorners() {
        val outline = NestedCardShape(NestedBorderShape.Rectangle)
            .createOutline(size, LayoutDirection.Ltr, density)
        assertTrue(outline is Outline.Rectangle)
    }

    @Test
    fun roundedRoundsEveryCorner() {
        val rounded = roundedCardRect(NestedBorderShape.Rounded, size.width, size.height, 8f)
        assertEquals(8f, rounded.topLeftCornerRadius.x)
        assertEquals(rounded.topLeftCornerRadius, rounded.topRightCornerRadius)
        assertEquals(rounded.topLeftCornerRadius, rounded.bottomRightCornerRadius)
        assertEquals(rounded.topLeftCornerRadius, rounded.bottomLeftCornerRadius)
    }

    @Test
    fun pillIsCappedAtHalfTheShortestSide() {
        val rounded = roundedCardRect(NestedBorderShape.Pill, size.width, size.height, 8f)
        assertEquals(size.height / 2f, rounded.topLeftCornerRadius.x)
        assertEquals(rounded.topLeftCornerRadius, rounded.topRightCornerRadius)
        assertEquals(rounded.topLeftCornerRadius, rounded.bottomRightCornerRadius)
        assertEquals(rounded.topLeftCornerRadius, rounded.bottomLeftCornerRadius)
    }

    @Test
    fun leafBloomsTwoOppositeCorners() {
        val rounded = roundedCardRect(NestedBorderShape.Leaf, size.width, size.height, 8f)
        val bloom = rounded.topLeftCornerRadius
        val soft = rounded.topRightCornerRadius
        assertEquals(size.height / 2f, bloom.x)
        assertEquals(8f, soft.x)
        assertEquals(bloom, rounded.bottomRightCornerRadius)
        assertEquals(soft, rounded.bottomLeftCornerRadius)
    }

    @Test
    fun zeroSizeProducesAnEmptyOutline() {
        for (shape in NestedBorderShape.entries) {
            val outline = NestedCardShape(shape).createOutline(Size.Zero, LayoutDirection.Ltr, density)
            assertTrue(outline is Outline.Generic, "$shape should fall back to an empty outline")
        }
    }

    @Test
    fun borderStylesMapToExpectedStrokes() {
        assertNull(NestedBorderStyle.None.toStroke(4f))
        assertEquals(4f, NestedBorderStyle.Solid.toStroke(4f)?.width)
        assertNotNull(NestedBorderStyle.Dashed.toStroke(4f)?.pathEffect)
        assertNotNull(NestedBorderStyle.Dotted.toStroke(4f)?.pathEffect)
    }
}

package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Resetting a row to its callout defaults clears only the item-level visual
 * overrides, leaving the selected callout preset, text style and text color in
 * charge.
 */
class NestedCalloutResetTest {

    private fun overriddenItem() = NestedListItem(
        id = "item-1",
        documentId = "doc-1",
        position = 0,
        text = "Row",
        textStyle = NestedTextStyle.Subheading,
        textColor = NestedColorToken.Pink,
        backgroundColor = NestedColorToken.Red,
        borderShape = NestedBorderShape.Hexagon,
        borderStyle = NestedBorderStyle.Dotted,
        borderColor = NestedColorToken.Blue,
        borderWidth = NestedBorderWidth.ExtraThick,
        callout = NestedCalloutStyle.Warning,
        calloutTitle = "Custom title",
        calloutIcon = "heart",
        calloutLayout = NestedCalloutLayout.Rail,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    @Test
    fun resetClearsVisualOverridesButKeepsCalloutAndText() {
        val original = overriddenItem()

        val reset = original.withCalloutDefaults()

        assertEquals(NestedCalloutDefaults.backgroundColor, reset.backgroundColor)
        assertEquals(NestedCalloutDefaults.borderShape, reset.borderShape)
        assertEquals(NestedCalloutDefaults.borderStyle, reset.borderStyle)
        assertEquals(NestedCalloutDefaults.borderColor, reset.borderColor)
        assertEquals(NestedCalloutDefaults.borderWidth, reset.borderWidth)
        assertNull(reset.calloutLayout)
        assertNull(reset.calloutTitle)
        assertNull(reset.calloutIcon)

        assertEquals(original.callout, reset.callout)
        assertEquals(original.textStyle, reset.textStyle)
        assertEquals(original.textColor, reset.textColor)
        assertEquals(original.text, reset.text)
    }

    @Test
    fun resetRendersTheSelectedCalloutPreset() {
        val preset = NestedCalloutStyle.Warning.spec()!!

        val reset = overriddenItem().withCalloutDefaults()
        val visuals = reset.resolveVisuals()

        assertEquals(preset, visuals.calloutSpec)
        assertEquals(preset.layout, visuals.layout)
        assertEquals(NestedCalloutLayout.Tab.titlePlacement(), visuals.titlePlacement)
        assertEquals(preset.shape, visuals.shape)
        assertEquals(preset.borderStyle, visuals.borderStyle)
        assertEquals(preset.borderWidthDp, visuals.borderWidthDp)
        assertEquals(preset.accent, visuals.borderToken)
        assertEquals(preset.accent, visuals.bgToken)
        assertEquals(preset.iconKey, reset.effectiveCalloutIconKey())
    }

    @Test
    fun resettingAPlainRowKeepsItPlain() {
        val plain = overriddenItem()
            .copy(callout = NestedCalloutStyle.None)
            .withCalloutDefaults()

        val visuals = plain.resolveVisuals()

        assertNull(visuals.calloutSpec)
        assertNull(visuals.layout)
        assertEquals(NestedBorderShape.Rounded, visuals.shape)
        assertEquals(NestedBorderStyle.None, visuals.borderStyle)
        assertEquals(NestedColorToken.Default, visuals.bgToken)
        assertEquals(NestedColorToken.Default, visuals.borderToken)
        assertNull(plain.effectiveCalloutIconKey())
    }
}

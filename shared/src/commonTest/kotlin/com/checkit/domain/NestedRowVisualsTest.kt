package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Explicit row formatting layers over the callout preset: the preset supplies
 * defaults, the row's own background/border properties win when set. Both the
 * Compose row and the SwiftUI row read this resolution, so these cases pin the
 * frame each platform draws.
 */
class NestedRowVisualsTest {

    private fun item(
        callout: NestedCalloutStyle = NestedCalloutStyle.None,
        calloutLayout: NestedCalloutLayout? = null,
        backgroundColor: NestedColorToken = NestedColorToken.Default,
        borderColor: NestedColorToken = NestedColorToken.Default,
        borderShape: NestedBorderShape = NestedBorderShape.Rounded,
        borderStyle: NestedBorderStyle = NestedBorderStyle.None
    ) = NestedListItem(
        id = "item-1",
        documentId = "doc-1",
        position = 0,
        text = "Row",
        backgroundColor = backgroundColor,
        borderShape = borderShape,
        borderStyle = borderStyle,
        borderColor = borderColor,
        callout = callout,
        calloutLayout = calloutLayout,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    @Test
    fun plainRowHasNoCalloutFrame() {
        val visuals = item().resolveVisuals()

        assertNull(visuals.calloutSpec)
        assertNull(visuals.layout)
        assertNull(visuals.titlePlacement)
        assertEquals(NestedCalloutTitlePlacement.Eyebrow, visuals.titlePlacementOrDefault)
        assertEquals(NestedBorderShape.Rounded, visuals.shape)
        assertEquals(NestedBorderStyle.None, visuals.borderStyle)
        assertEquals(EffectiveRowVisuals.DEFAULT_BORDER_WIDTH_DP, visuals.borderWidthDp)
        assertEquals(NestedColorToken.Default, visuals.borderToken)
        assertEquals(NestedColorToken.Default, visuals.bgToken)
        assertEquals(0f, visuals.bgAlpha)
    }

    @Test
    fun presetSuppliesLayoutFrameAndWash() {
        val note = NestedCalloutStyle.Note.spec()!!
        val visuals = item(callout = NestedCalloutStyle.Note).resolveVisuals()

        assertEquals(note, visuals.calloutSpec)
        assertEquals(note.layout, visuals.layout)
        assertEquals(NestedCalloutTitlePlacement.Eyebrow, visuals.titlePlacement)
        assertEquals(note.shape, visuals.shape)
        assertEquals(note.borderStyle, visuals.borderStyle)
        assertEquals(note.borderWidthDp, visuals.borderWidthDp)
        assertEquals(note.accent, visuals.borderToken)
        assertEquals(note.accent, visuals.bgToken)
        assertEquals(EffectiveRowVisuals.CALLOUT_BACKGROUND_ALPHA, visuals.bgAlpha)
    }

    @Test
    fun layoutOverrideChangesTitleTreatmentOnly() {
        val note = NestedCalloutStyle.Note.spec()!!
        val visuals = item(
            callout = NestedCalloutStyle.Note,
            calloutLayout = NestedCalloutLayout.Tab
        ).resolveVisuals()

        assertEquals(NestedCalloutLayout.Tab, visuals.layout)
        assertEquals(NestedCalloutTitlePlacement.TopTab, visuals.titlePlacement)
        // The rest of the recipe still comes from the preset.
        assertEquals(note.shape, visuals.shape)
        assertEquals(note.borderStyle, visuals.borderStyle)
        assertEquals(note.accent, visuals.borderToken)
    }

    @Test
    fun layoutOverrideIsIgnoredWithoutACallout() {
        val visuals = item(calloutLayout = NestedCalloutLayout.Rail).resolveVisuals()

        assertNull(visuals.layout)
        assertNull(visuals.titlePlacement)
        assertEquals(NestedBorderShape.Rounded, visuals.shape)
        assertEquals(NestedBorderStyle.None, visuals.borderStyle)
    }

    @Test
    fun railLayoutForcesASquareFrame() {
        // Warning is a Tab preset with a cut-corner frame; moving it to Rail
        // must drop the card shape entirely.
        val visuals = item(
            callout = NestedCalloutStyle.Warning,
            calloutLayout = NestedCalloutLayout.Rail
        ).resolveVisuals()

        assertEquals(NestedCalloutLayout.Rail, visuals.layout)
        assertEquals(NestedCalloutTitlePlacement.Inline, visuals.titlePlacement)
        assertEquals(NestedBorderShape.Rectangle, visuals.shape)
    }

    @Test
    fun explicitBorderShapeDoesNotSurviveRail() {
        val visuals = item(
            callout = NestedCalloutStyle.Note,
            calloutLayout = NestedCalloutLayout.Rail,
            borderShape = NestedBorderShape.Cloud
        ).resolveVisuals()

        assertEquals(NestedBorderShape.Rectangle, visuals.shape)
    }

    @Test
    fun textLayoutDropsThePresetFrame() {
        // Important is a Tab preset with a cut-corner card. Text must keep the
        // accent for the eyebrow but draw no stroke and no wash.
        val important = NestedCalloutStyle.Important.spec()!!
        val visuals = item(
            callout = NestedCalloutStyle.Important,
            calloutLayout = NestedCalloutLayout.Text
        ).resolveVisuals()

        assertEquals(NestedCalloutLayout.Text, visuals.layout)
        assertEquals(NestedCalloutTitlePlacement.Eyebrow, visuals.titlePlacement)
        assertEquals(NestedBorderStyle.None, visuals.borderStyle)
        // The preset's cut-corner geometry must not leak in and clip content.
        assertEquals(NestedBorderShape.Rounded, visuals.shape)
        assertEquals(NestedColorToken.Default, visuals.bgToken)
        assertEquals(0f, visuals.bgAlpha)
        assertEquals(important.accent, visuals.borderToken)
    }

    @Test
    fun textPresetResolvesAsTextLayout() {
        val idea = NestedCalloutStyle.Idea.spec()!!
        val visuals = item(callout = NestedCalloutStyle.Idea).resolveVisuals()

        assertEquals(NestedCalloutLayout.Text, visuals.layout)
        assertEquals(NestedCalloutTitlePlacement.Eyebrow, visuals.titlePlacement)
        assertEquals(NestedBorderStyle.None, visuals.borderStyle)
        assertEquals(NestedBorderShape.Rounded, visuals.shape)
        assertEquals(idea.accent, visuals.borderToken)
        assertEquals(NestedColorToken.Default, visuals.bgToken)
        assertEquals(0f, visuals.bgAlpha)
    }

    @Test
    fun textLayoutStillHonorsExplicitFormatting() {
        val visuals = item(
            callout = NestedCalloutStyle.Note,
            calloutLayout = NestedCalloutLayout.Text,
            backgroundColor = NestedColorToken.Yellow,
            borderColor = NestedColorToken.Red,
            borderShape = NestedBorderShape.Pill,
            borderStyle = NestedBorderStyle.Dashed
        ).resolveVisuals()

        assertEquals(NestedCalloutLayout.Text, visuals.layout)
        assertEquals(NestedColorToken.Yellow, visuals.bgToken)
        assertEquals(EffectiveRowVisuals.EXPLICIT_BACKGROUND_ALPHA, visuals.bgAlpha)
        assertEquals(NestedColorToken.Red, visuals.borderToken)
        assertEquals(NestedBorderShape.Pill, visuals.shape)
        assertEquals(NestedBorderStyle.Dashed, visuals.borderStyle)
    }

    @Test
    fun explicitFormattingOverridesThePreset() {
        val note = NestedCalloutStyle.Note.spec()!!
        val visuals = item(
            callout = NestedCalloutStyle.Note,
            backgroundColor = NestedColorToken.Red,
            borderColor = NestedColorToken.Green,
            borderShape = NestedBorderShape.Pill,
            borderStyle = NestedBorderStyle.Dashed
        ).resolveVisuals()

        assertEquals(NestedBorderShape.Pill, visuals.shape)
        assertEquals(NestedBorderStyle.Dashed, visuals.borderStyle)
        assertEquals(NestedColorToken.Green, visuals.borderToken)
        assertEquals(NestedColorToken.Red, visuals.bgToken)
        // A hand-picked fill reads stronger than the preset wash.
        assertEquals(EffectiveRowVisuals.EXPLICIT_BACKGROUND_ALPHA, visuals.bgAlpha)
        // Stroke width has no row-level property, so it stays with the preset.
        assertEquals(note.borderWidthDp, visuals.borderWidthDp)
    }

    @Test
    fun explicitBackgroundWithoutACalloutStillWashes() {
        val visuals = item(backgroundColor = NestedColorToken.Yellow).resolveVisuals()

        assertEquals(NestedColorToken.Yellow, visuals.bgToken)
        assertEquals(EffectiveRowVisuals.EXPLICIT_BACKGROUND_ALPHA, visuals.bgAlpha)
        // No callout means no accent border.
        assertEquals(NestedColorToken.Default, visuals.borderToken)
        assertEquals(NestedBorderStyle.None, visuals.borderStyle)
    }

    @Test
    fun roundedAndNoBorderMeanUseThePreset() {
        // "Rounded" and "None" are defaults, not deliberate choices, so they
        // must not flatten a preset's shape or stroke.
        val tip = NestedCalloutStyle.Tip.spec()!!
        val visuals = item(
            callout = NestedCalloutStyle.Tip,
            borderShape = NestedBorderShape.Rounded,
            borderStyle = NestedBorderStyle.None
        ).resolveVisuals()

        assertEquals(tip.shape, visuals.shape)
        assertEquals(tip.borderStyle, visuals.borderStyle)
    }
}

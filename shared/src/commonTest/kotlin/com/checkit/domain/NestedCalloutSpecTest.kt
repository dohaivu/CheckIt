package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NestedCalloutSpecTest {

    @Test
    fun noneHasNoRecipe() {
        assertNull(NestedCalloutStyle.None.spec())
        assertEquals("No callout", NestedCalloutStyle.None.displayName())
    }

    @Test
    fun onlyAnAppliedCalloutKeepsATitle() {
        assertNull(NestedCalloutStyle.None.normalizedTitle("Stale title"))
        assertEquals("Read me", NestedCalloutStyle.Note.normalizedTitle("  Read me  "))
        assertNull(NestedCalloutStyle.Note.normalizedTitle("   "))
        assertNull(NestedCalloutStyle.Note.normalizedTitle(null))
    }

    @Test
    fun everyKindHasARecipeAndDefaultTitle() {
        val kinds = NestedCalloutStyle.entries.filter { it != NestedCalloutStyle.None }
        assertEquals(8, kinds.size)
        kinds.forEach { kind ->
            val spec = kind.spec()
            assertTrue(spec != null, "${kind.name} must define a recipe")
            assertEquals(kind.name, kind.displayName())
        }
    }

    @Test
    fun kindsCoverAllThreeLayouts() {
        assertEquals(
            setOf(NestedCalloutLayout.Panel, NestedCalloutLayout.Tab, NestedCalloutLayout.Rail),
            NestedCalloutStyle.entries.mapNotNull { it.spec()?.layout }.toSet()
        )
        assertEquals(
            setOf(
                NestedCalloutTitlePlacement.Eyebrow,
                NestedCalloutTitlePlacement.TopTab,
                NestedCalloutTitlePlacement.Inline
            ),
            NestedCalloutStyle.entries.mapNotNull { it.spec()?.titlePlacement }.toSet()
        )
    }

    @Test
    fun textLayoutIsAnOverrideNotAPresetDefault() {
        // Text is a frame-less treatment users can pick per row; no callout
        // kind should silently change its default look.
        assertTrue(NestedCalloutLayout.Text in NestedCalloutLayout.entries)
        assertTrue(
            NestedCalloutStyle.entries.none { it.spec()?.layout == NestedCalloutLayout.Text }
        )
    }

    @Test
    fun panelKindsAreDistinct() {
        val note = NestedCalloutStyle.Note.spec()!!
        val tip = NestedCalloutStyle.Tip.spec()!!
        val example = NestedCalloutStyle.Example.spec()!!

        assertEquals(NestedCalloutLayout.Panel, note.layout)
        assertEquals(NestedBorderShape.Rounded, note.shape)
        assertEquals(NestedColorToken.Blue, note.accent)

        assertEquals(NestedCalloutLayout.Panel, tip.layout)
        assertEquals(NestedBorderShape.Leaf, tip.shape)
        assertEquals(NestedColorToken.Green, tip.accent)

        assertEquals(NestedCalloutLayout.Panel, example.layout)
        assertEquals(NestedColorToken.Teal, example.accent)
        assertEquals(NestedBorderStyle.Dotted, example.borderStyle)

        assertEquals(3, setOf(note.iconKey, tip.iconKey, example.iconKey).size)
        assertTrue(note.iconKey.isNotBlank())
    }

    @Test
    fun tabKindsAreDistinct() {
        val important = NestedCalloutStyle.Important.spec()!!
        val warning = NestedCalloutStyle.Warning.spec()!!
        val caution = NestedCalloutStyle.Caution.spec()!!

        listOf(important, warning, caution).forEach {
            assertEquals(NestedCalloutLayout.Tab, it.layout)
            assertEquals(NestedCalloutTitlePlacement.TopTab, it.titlePlacement)
        }
        assertEquals(NestedColorToken.Purple, important.accent)
        assertEquals(NestedColorToken.Orange, warning.accent)
        assertEquals(NestedBorderStyle.Dashed, warning.borderStyle)
        assertEquals(NestedColorToken.Red, caution.accent)
        assertEquals(2f, caution.borderWidthDp)
        assertEquals(3, setOf(important.iconKey, warning.iconKey, caution.iconKey).size)
    }

    @Test
    fun railKindsHaveNoCardAndUseAwiderRail() {
        val question = NestedCalloutStyle.Question.spec()!!
        val quote = NestedCalloutStyle.Quote.spec()!!

        listOf(question, quote).forEach {
            assertEquals(NestedCalloutLayout.Rail, it.layout)
            assertEquals(NestedCalloutTitlePlacement.Inline, it.titlePlacement)
            assertNull(it.shape)
            assertEquals(3f, it.borderWidthDp)
        }
        assertEquals(NestedColorToken.Cyan, question.accent)
        assertEquals(NestedColorToken.Gray, quote.accent)
        assertEquals(2, setOf(question.iconKey, quote.iconKey).size)
    }
}

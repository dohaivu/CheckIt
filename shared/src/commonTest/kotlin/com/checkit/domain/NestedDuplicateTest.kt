package com.checkit.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NestedDuplicateTest {

    private fun item(
        id: String,
        parentId: String? = null,
        position: Int = 0,
        documentId: String = "doc-1"
    ) = NestedListItem(
        id = id,
        documentId = documentId,
        parentId = parentId,
        position = position,
        text = "item $id",
        createdAtMillis = 10L,
        updatedAtMillis = 20L
    )

    @Test
    fun unknownSourceReturnsNull() {
        assertNull(
            planNestedItemDuplicate(
                items = listOf(item("a")),
                itemId = "missing",
                newId = "new",
                nowMillis = 99L
            )
        )
    }

    @Test
    fun duplicateKeepsParentAndTakesNextSiblingPosition() {
        val source = item("source", parentId = "parent", position = 3)
        val duplicate = planNestedItemDuplicate(
            items = listOf(item("parent"), source, item("later", "parent", 4)),
            itemId = "source",
            newId = "new",
            nowMillis = 99L
        )

        assertNotNull(duplicate)
        assertEquals("new", duplicate.id)
        assertEquals("parent", duplicate.parentId)
        assertEquals("doc-1", duplicate.documentId)
        assertEquals(4, duplicate.position)
    }

    @Test
    fun duplicateOfRootStaysAtRoot() {
        val duplicate = planNestedItemDuplicate(
            items = listOf(item("root", parentId = null, position = 2)),
            itemId = "root",
            newId = "new",
            nowMillis = 99L
        )

        assertNotNull(duplicate)
        assertNull(duplicate.parentId)
        assertEquals(3, duplicate.position)
    }

    @Test
    fun duplicateResetsExecutionStateAndTimestamps() {
        val source = item("source").copy(
            checked = true,
            completedAtMillis = 123L,
            collapsed = true,
            actualMinutes = 45
        )

        val duplicate = planNestedItemDuplicate(
            items = listOf(source),
            itemId = "source",
            newId = "new",
            nowMillis = 999L
        )

        assertNotNull(duplicate)
        assertFalse(duplicate.checked)
        assertNull(duplicate.completedAtMillis)
        assertFalse(duplicate.collapsed)
        assertEquals(0, duplicate.actualMinutes)
        assertEquals(999L, duplicate.createdAtMillis)
        assertEquals(999L, duplicate.updatedAtMillis)
    }

    @Test
    fun duplicateCopiesContentFormattingMetadataAndTags() {
        val tag = TagItem(id = "tag-1", name = "work", color = "#2563EB")
        val source = item("source").copy(
            text = "Plan the week",
            note = "Keep it short",
            label = "Planning",
            checkboxEnabled = true,
            textStyle = NestedTextStyle.Heading,
            textColor = NestedColorToken.Blue,
            backgroundColor = NestedColorToken.Yellow,
            borderShape = NestedBorderShape.Pill,
            borderStyle = NestedBorderStyle.Dashed,
            borderColor = NestedColorToken.Green,
            callout = NestedCalloutStyle.Tip,
            calloutTitle = "Tip",
            guideDot = "⭐️",
            guideDotColor = NestedColorToken.Purple,
            startDate = LocalDate(2026, 11, 7),
            endDate = LocalDate(2026, 11, 8),
            priority = TaskPriority.High,
            tags = listOf(tag),
            progressPercent = 40,
            metricRollupPolicy = MetricRollupPolicy.OwnOnly,
            showTrackedMinutes = true,
            manualMetrics = listOf(MetricItem(name = "Pushups", value = "20"))
        )

        val duplicate = planNestedItemDuplicate(
            items = listOf(source),
            itemId = "source",
            newId = "new",
            nowMillis = 999L
        )

        assertNotNull(duplicate)
        assertEquals(source.text, duplicate.text)
        assertEquals(source.note, duplicate.note)
        assertEquals(source.label, duplicate.label)
        assertTrue(duplicate.checkboxEnabled)
        assertEquals(source.textStyle, duplicate.textStyle)
        assertEquals(source.textColor, duplicate.textColor)
        assertEquals(source.backgroundColor, duplicate.backgroundColor)
        assertEquals(source.borderShape, duplicate.borderShape)
        assertEquals(source.borderStyle, duplicate.borderStyle)
        assertEquals(source.borderColor, duplicate.borderColor)
        assertEquals(source.callout, duplicate.callout)
        assertEquals(source.calloutTitle, duplicate.calloutTitle)
        assertEquals(source.guideDot, duplicate.guideDot)
        assertEquals(source.guideDotColor, duplicate.guideDotColor)
        assertEquals(source.startDate, duplicate.startDate)
        assertEquals(source.endDate, duplicate.endDate)
        assertEquals(source.priority, duplicate.priority)
        assertEquals(source.tags, duplicate.tags)
        assertEquals(source.progressPercent, duplicate.progressPercent)
        assertEquals(source.metricRollupPolicy, duplicate.metricRollupPolicy)
        assertEquals(source.showTrackedMinutes, duplicate.showTrackedMinutes)
        assertEquals(source.manualMetrics, duplicate.manualMetrics)
    }

    @Test
    fun childrenAreNotPartOfThePlan() {
        val source = item("source", position = 0)
        val child = item("child", parentId = "source", position = 0)

        val duplicate = planNestedItemDuplicate(
            items = listOf(source, child),
            itemId = "source",
            newId = "new",
            nowMillis = 99L
        )

        assertNotNull(duplicate)
        assertEquals("new", duplicate.id)
        assertNull(duplicate.parentId)
        // The planner returns only the row itself; no row points at the new id.
        assertTrue(listOf(duplicate).none { it.parentId == duplicate.id })
    }
}

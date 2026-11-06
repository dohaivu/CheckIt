package com.checkit.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NestedSubtreeTest {

    private fun item(
        id: String,
        parentId: String? = null,
        position: Int = 0,
        text: String = id,
        collapsed: Boolean = false,
        tags: List<TagItem> = emptyList()
    ) = NestedListItem(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = position,
        text = text,
        note = "note-$id",
        label = "label-$id",
        checkboxEnabled = true,
        checked = true,
        completedAtMillis = 123L,
        collapsed = collapsed,
        textStyle = NestedTextStyle.Heading,
        textColor = NestedColorToken.Blue,
        backgroundColor = NestedColorToken.Yellow,
        borderShape = NestedBorderShape.Pill,
        borderStyle = NestedBorderStyle.Dotted,
        borderColor = NestedColorToken.Green,
        callout = NestedCalloutStyle.Warning,
        calloutTitle = "Custom title",
        guideDotColor = NestedColorToken.Pink,
        guideDot = "🔥",
        startDate = LocalDate(2026, 1, 1),
        endDate = LocalDate(2026, 1, 2),
        priority = TaskPriority.High,
        tags = tags,
        actualMinutes = 30,
        metricRollupPolicy = MetricRollupPolicy.OwnOnly,
        showTrackedMinutes = true,
        progressPercent = 40,
        createdAtMillis = 100L,
        updatedAtMillis = 200L
    )

    private fun tree(
        rootNodes: List<NestedItemNode>,
        documentId: String = "doc-1"
    ) = NestedDocumentTree(
        document = NestedDocument(documentId, "Doc", 1L, 2L),
        rootNodes = rootNodes
    )

    private fun sampleTree() = tree(
        listOf(
            NestedItemNode(
                item = item("a", collapsed = true, tags = listOf(TagItem("tag-1", "Work", "#fff"))),
                children = listOf(
                    NestedItemNode(
                        item = item("b", parentId = "a", position = 0),
                        children = listOf(
                            NestedItemNode(item = item("d", parentId = "b", position = 0))
                        )
                    ),
                    NestedItemNode(item = item("c", parentId = "a", position = 1))
                )
            )
        )
    )

    @Test
    fun captureKeepsCollapsedDescendantsAndCountsEveryRow() {
        val clip = captureNestedSubtree(sampleTree(), "a")

        assertEquals(4, clip?.itemCount)
        assertEquals("doc-1", clip?.sourceDocumentId)
        assertEquals(1, clip?.roots?.size)
        assertEquals("a", clip?.roots?.first()?.item?.id)
        assertEquals(2, clip?.roots?.first()?.children?.size)
    }

    @Test
    fun captureUnknownIdReturnsNull() {
        assertNull(captureNestedSubtree(sampleTree(), "missing"))
    }

    @Test
    fun pasteRemapsIdsParentsAndPositions() {
        val clip = captureNestedSubtree(sampleTree(), "a")!!
        var next = 0

        val pasted = planNestedSubtreePaste(
            clip = clip,
            documentId = "doc-2",
            parentId = "target",
            startPosition = 4,
            nowMillis = 999L,
            newId = { "new-${next++}" }
        )

        assertEquals(listOf("new-0", "new-1", "new-2", "new-3"), pasted.map { it.id })
        assertEquals(4, pasted.size)
        assertTrue(pasted.none { it.id in setOf("a", "b", "c", "d") })

        val root = pasted[0]
        val firstChild = pasted[1]
        val grandchild = pasted[2]
        val secondChild = pasted[3]

        assertEquals("target", root.parentId)
        assertEquals("doc-2", root.documentId)
        assertEquals(4, root.position)
        assertEquals("new-0", firstChild.parentId)
        assertEquals("new-0", secondChild.parentId)
        assertEquals("new-1", grandchild.parentId)
        assertEquals(0, firstChild.position)
        assertEquals(1, secondChild.position)
        assertEquals(0, grandchild.position)
    }

    @Test
    fun pastePreservesFieldsButExpandsRoot() {
        val clip = captureNestedSubtree(sampleTree(), "a")!!
        var next = 0

        val pasted = planNestedSubtreePaste(
            clip = clip,
            documentId = "doc-2",
            parentId = null,
            startPosition = 0,
            nowMillis = 999L,
            newId = { "new-${next++}" }
        )

        val root = pasted[0]
        assertFalse(root.collapsed)
        assertEquals(999L, root.createdAtMillis)
        assertEquals(999L, root.updatedAtMillis)
        assertEquals("a", root.text)
        assertEquals("note-a", root.note)
        assertEquals("label-a", root.label)
        assertEquals(NestedTextStyle.Heading, root.textStyle)
        assertEquals(NestedColorToken.Blue, root.textColor)
        assertEquals(NestedColorToken.Yellow, root.backgroundColor)
        assertEquals(NestedBorderShape.Pill, root.borderShape)
        assertEquals(NestedBorderStyle.Dotted, root.borderStyle)
        assertEquals(NestedColorToken.Green, root.borderColor)
        assertEquals(NestedCalloutStyle.Warning, root.callout)
        assertEquals("Custom title", root.calloutTitle)
        assertEquals(NestedColorToken.Pink, root.guideDotColor)
        assertEquals("🔥", root.guideDot)
        assertEquals(LocalDate(2026, 1, 1), root.startDate)
        assertEquals(LocalDate(2026, 1, 2), root.endDate)
        assertEquals(TaskPriority.High, root.priority)
        assertEquals(listOf("tag-1"), root.tags.map { it.id })
        assertEquals(30, root.actualMinutes)
        assertEquals(MetricRollupPolicy.OwnOnly, root.metricRollupPolicy)
        assertTrue(root.showTrackedMinutes)
        assertEquals(40, root.progressPercent)
        assertTrue(root.checked)
        assertEquals(123L, root.completedAtMillis)

        // Descendants keep their own collapsed state.
        val collapsedChild = item("e", parentId = "a", collapsed = true)
        val collapsedClip = captureNestedSubtree(
            tree(listOf(NestedItemNode(item("x"), listOf(NestedItemNode(collapsedChild))))),
            "x"
        )!!
        val pastedCollapsed = planNestedSubtreePaste(
            clip = collapsedClip,
            documentId = "doc-2",
            parentId = null,
            startPosition = 0,
            nowMillis = 1L,
            newId = { "id-${next++}" }
        )
        assertFalse(pastedCollapsed[0].collapsed)
        assertTrue(pastedCollapsed[1].collapsed)
    }

    @Test
    fun pasteMultipleRootsKeepsOrderAndConsecutivePositions() {
        val clip = NestedSubtreeClip(
            roots = listOf(
                NestedItemNode(item("r1")),
                NestedItemNode(item("r2"))
            ),
            sourceDocumentId = "doc-1"
        )
        var next = 0

        val pasted = planNestedSubtreePaste(
            clip = clip,
            documentId = "doc-2",
            parentId = "parent",
            startPosition = 7,
            nowMillis = 1L,
            newId = { "root-${next++}" }
        )

        assertEquals(2, pasted.size)
        assertEquals(listOf(7, 8), pasted.map { it.position })
        assertEquals(listOf("root-0", "root-1"), pasted.map { it.id })
        assertEquals(listOf("r1", "r2"), pasted.map { it.text })
        assertNotEquals(pasted[0].id, pasted[1].id)
    }

    @Test
    fun pasteEmptyClipReturnsEmptyPlan() {
        val clip = NestedSubtreeClip(roots = emptyList(), sourceDocumentId = "doc-1")

        assertTrue(
            planNestedSubtreePaste(
                clip = clip,
                documentId = "doc-2",
                parentId = null,
                startPosition = 0,
                nowMillis = 1L,
                newId = { "unused" }
            ).isEmpty()
        )
    }
}

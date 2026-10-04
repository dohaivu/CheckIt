package com.checkit.domain.usecase

import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.TagItem
import kotlin.test.Test
import kotlin.test.assertEquals

class NestedListToMarkdownTest {

    private fun item(
        id: String,
        text: String,
        checkbox: Boolean = false,
        checked: Boolean = false,
        style: NestedTextStyle = NestedTextStyle.Body,
        note: String? = null,
        tags: List<TagItem> = emptyList()
    ) = NestedListItem(
        id = id,
        documentId = "doc-1",
        position = 0,
        text = text,
        checkboxEnabled = checkbox,
        checked = checked,
        textStyle = style,
        note = note,
        tags = tags,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    @Test
    fun testSimpleListExport() {
        val nodes = listOf(
            NestedItemNode(item = item("1", "First item")),
            NestedItemNode(item = item("2", "Second item"))
        )
        val tree = NestedDocumentTree(
            document = NestedDocument("doc-1", "Test Doc", 0L, 0L),
            rootNodes = nodes
        )
        val md = tree.toMarkdown()
        assertEquals(
            """
            - First item
            - Second item
            """.trimIndent(),
            md
        )
    }

    @Test
    fun testCheckboxAndNestingExport() {
        val child1 = NestedItemNode(item = item("2", "Subtask uncompleted", checkbox = true, checked = false))
        val child2 = NestedItemNode(item = item("3", "Subtask completed", checkbox = true, checked = true))
        val root = NestedItemNode(
            item = item("1", "Parent Task", checkbox = true, checked = false, style = NestedTextStyle.Header),
            children = listOf(child1, child2)
        )
        val tree = NestedDocumentTree(
            document = NestedDocument("doc-1", "Project Plan", 0L, 0L),
            rootNodes = listOf(root)
        )
        val md = tree.toMarkdown(NestedMarkdownExportOptions(includeDocumentTitle = true))
        val expected = """
            # Project Plan

            - [ ] **Parent Task**
              - [ ] Subtask uncompleted
              - [x] Subtask completed
        """.trimIndent()
        assertEquals(expected, md)
    }

    @Test
    fun testNotesAndTagsExport() {
        val root = NestedItemNode(
            item = item(
                "1",
                "Item with notes and tags",
                note = "First line of note\nSecond line",
                tags = listOf(TagItem("t1", "work", "#FF0000"), TagItem("t2", "urgent task", "#00FF00"))
            )
        )
        val md = root.toMarkdown(NestedMarkdownExportOptions(includeTags = true, includeNotes = true))
        val expected = """
            - Item with notes and tags #work #urgent_task
              First line of note
              Second line
        """.trimIndent()
        assertEquals(expected, md)
    }

    @Test
    fun testSubtreeExport() {
        val grandchild = NestedItemNode(item = item("3", "Grandchild"))
        val child = NestedItemNode(item = item("2", "Child"), children = listOf(grandchild))
        val root = NestedItemNode(item = item("1", "Root"), children = listOf(child))

        val exporter = ExportNestedListToMarkdownUseCase()
        val childMd = exporter.exportSubtree(child)
        val expected = """
            - Child
              - Grandchild
        """.trimIndent()
        assertEquals(expected, childMd)
    }
}

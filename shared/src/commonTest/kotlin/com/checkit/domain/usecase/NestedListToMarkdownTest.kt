package com.checkit.domain.usecase

import com.checkit.domain.NestedCalloutStyle
import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedTextStyle
import com.checkit.domain.TagItem
import com.checkit.domain.buildNestedTree
import com.checkit.domain.filterNestedTree
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
    fun testSimpleListExport() {        val nodes = listOf(
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
    fun testCalloutIsOmittedFromMarkdown() {
        val item = item("1", "Heads up").copy(
            callout = NestedCalloutStyle.Warning,
            calloutTitle = "Read me before continuing"
        )
        val tree = NestedDocumentTree(
            document = NestedDocument("doc-1", "Test Doc", 0L, 0L),
            rootNodes = listOf(NestedItemNode(item = item))
        )
        val md = tree.toMarkdown()
        assertEquals("- Heads up", md)
    }

    @Test
    fun testCheckboxAndNestingExport() {
        val child1 = NestedItemNode(item = item("2", "Subtask uncompleted", checkbox = true, checked = false))
        val child2 = NestedItemNode(item = item("3", "Subtask completed", checkbox = true, checked = true))
        val root = NestedItemNode(
            item = item("1", "Parent Task", checkbox = true, checked = false, style = NestedTextStyle.Heading),
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

    @Test
    fun testTextStyleExport() {
        fun export(style: NestedTextStyle, text: String) =
            NestedItemNode(item = item("1", text, style = style)).toMarkdown()

        assertEquals("- Plain", export(NestedTextStyle.Body, "Plain"))
        assertEquals("- ***Title***", export(NestedTextStyle.Title, "Title"))
        assertEquals("- **Heading**", export(NestedTextStyle.Heading, "Heading"))
        assertEquals("- *Subheading*", export(NestedTextStyle.Subheading, "Subheading"))
        assertEquals("- `Mono`", export(NestedTextStyle.Monostyled, "Mono"))
    }

    @Test
    fun testNumberedRootsExport() {
        val roots = buildNestedTree(
            listOf(
                item("2", "Second").copy(position = 1),
                item("1", "First").copy(position = 0),
            )
        )
        val tree = NestedDocumentTree(
            document = NestedDocument("doc-1", "Doc", 0L, 0L, numberingStyle = com.checkit.domain.NestedNumberingStyle.Decimal),
            rootNodes = roots
        )

        assertEquals(
            """
            1. First
            2. Second
            """.trimIndent(),
            tree.toMarkdown()
        )
    }

    @Test
    fun testNumberedChildrenExportUsesAbsoluteOrdinalsAndCheckboxes() {
        val roots = buildNestedTree(
            listOf(
                item("parent", "Parent").copy(position = 0, numberingStyle = com.checkit.domain.NestedNumberingStyle.Decimal),
                item("child-1", "Child one").copy(
                    parentId = "parent",
                    position = 0,
                    checkboxEnabled = true,
                    checked = false
                ),
                item("child-2", "Child two").copy(
                    parentId = "parent",
                    position = 1,
                    checkboxEnabled = true,
                    checked = true
                ),
                item("sibling", "Sibling").copy(position = 1),
            )
        )
        val tree = NestedDocumentTree(
            document = NestedDocument("doc-1", "Doc", 0L, 0L),
            rootNodes = roots
        )

        assertEquals(
            """
            - Parent
              1. [ ] Child one
              2. [x] Child two
            - Sibling
            """.trimIndent(),
            tree.toMarkdown()
        )
    }

    @Test
    fun testMixedNumberingKeepsUnnumberedLevelsAsDots() {
        val roots = buildNestedTree(
            listOf(
                item("parent", "Parent").copy(position = 0),
                item("child", "Child").copy(parentId = "parent", position = 0),
            )
        )
        val tree = NestedDocumentTree(
            document = NestedDocument("doc-1", "Doc", 0L, 0L, numberingStyle = com.checkit.domain.NestedNumberingStyle.Decimal),
            rootNodes = roots
        )

        assertEquals(
            """
            1. Parent
              - Child
            """.trimIndent(),
            tree.toMarkdown()
        )
    }

    @Test
    fun testExportedFilteredSubsetKeepsAbsoluteNumber() {
        val roots = buildNestedTree(
            listOf(
                item("first", "alpha", checkbox = true).copy(position = 0),
                item("second", "beta").copy(position = 1),
                item("third", "gamma").copy(position = 2),
            )
        )
        val filtered = filterNestedTree(roots, query = "gamma")

        val exporter = ExportNestedListToMarkdownUseCase()
        assertEquals("3. gamma", exporter.exportNodes(filtered, numberingStyle = com.checkit.domain.NestedNumberingStyle.Decimal))
    }

    @Test
    fun testLegalNumberingExportsHierarchy() {
        val roots = buildNestedTree(
            listOf(
                item("root-1", "Root 1").copy(position = 0, numberingStyle = com.checkit.domain.NestedNumberingStyle.Legal),
                item("child-1", "Child 1").copy(parentId = "root-1", position = 0, numberingStyle = com.checkit.domain.NestedNumberingStyle.Legal),
                item("subchild-1", "Subchild 1").copy(parentId = "child-1", position = 0),
                item("root-2", "Root 2").copy(position = 1, numberingStyle = com.checkit.domain.NestedNumberingStyle.Legal),
                item("child-2", "Child 2").copy(parentId = "root-2", position = 1),
            )
        )
        val tree = NestedDocumentTree(
            document = NestedDocument("doc-1", "Doc", 0L, 0L, numberingStyle = com.checkit.domain.NestedNumberingStyle.Legal),
            rootNodes = roots
        )

        assertEquals(
            """
            1. Root 1
              1.1. Child 1
                1.1.1. Subchild 1
            2. Root 2
              2.1. Child 2
            """.trimIndent(),
            tree.toMarkdown()
        )
    }

    @Test
    fun testFormatNestedNumberStyles() {
        assertEquals("1", com.checkit.domain.formatNestedNumber(com.checkit.domain.NestedNumberingStyle.Decimal, 1))
        assertEquals("a", com.checkit.domain.formatNestedNumber(com.checkit.domain.NestedNumberingStyle.AlphaLower, 1))
        assertEquals("z", com.checkit.domain.formatNestedNumber(com.checkit.domain.NestedNumberingStyle.AlphaLower, 26))
        assertEquals("aa", com.checkit.domain.formatNestedNumber(com.checkit.domain.NestedNumberingStyle.AlphaLower, 27))
        assertEquals("A", com.checkit.domain.formatNestedNumber(com.checkit.domain.NestedNumberingStyle.AlphaUpper, 1))
        assertEquals("iv", com.checkit.domain.formatNestedNumber(com.checkit.domain.NestedNumberingStyle.RomanLower, 4))
        assertEquals("IX", com.checkit.domain.formatNestedNumber(com.checkit.domain.NestedNumberingStyle.RomanUpper, 9))
        assertEquals("1.2.3", com.checkit.domain.formatNestedNumber(com.checkit.domain.NestedNumberingStyle.Legal, 3, listOf(1, 2)))
        assertEquals("2.2", com.checkit.domain.formatNestedNumber(com.checkit.domain.NestedNumberingStyle.Legal, 2, listOf(2)))
    }
}

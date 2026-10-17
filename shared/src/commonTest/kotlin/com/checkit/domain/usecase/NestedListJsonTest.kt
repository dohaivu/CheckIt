package com.checkit.domain.usecase

import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NestedListJsonTest {

    @Test
    fun testJsonExportRoundTrip() {
        val rootItem = NestedListItem(
            id = "item-1",
            documentId = "doc-1",
            position = 0,
            text = "Root Item",
            note = "Some notes",
            checkboxEnabled = true,
            checked = false,
            createdAtMillis = 1000L,
            updatedAtMillis = 2000L
        )
        val childItem = NestedListItem(
            id = "item-2",
            documentId = "doc-1",
            parentId = "item-1",
            position = 0,
            text = "Child Item",
            checkboxEnabled = true,
            checked = true,
            createdAtMillis = 1100L,
            updatedAtMillis = 2100L
        )
        val tree = NestedDocumentTree(
            document = NestedDocument("doc-1", "My Document", 1000L, 2000L),
            rootNodes = listOf(
                NestedItemNode(item = rootItem, children = listOf(NestedItemNode(item = childItem)))
            )
        )

        val export = tree.toExport()
        val json = export.toJson()

        assertTrue(json.contains("\"title\": \"My Document\""))
        assertTrue(json.contains("\"text\": \"Root Item\""))
        assertTrue(json.contains("\"text\": \"Child Item\""))

        val decoded = NestedDocumentExport.fromJson(json)
        assertEquals("doc-1", decoded.document.id)
        assertEquals("My Document", decoded.document.title)
        assertEquals(1, decoded.items.size)
        assertEquals("Root Item", decoded.items[0].text)
        assertEquals(1, decoded.items[0].children.size)
        assertEquals("Child Item", decoded.items[0].children[0].text)
        assertTrue(decoded.items[0].children[0].checked)
    }

    @Test
    fun testResolveUniqueTitle() {
        val existing = setOf("Groceries", "Project X", "Project X (1)", "Work")

        // New name -> unchanged
        assertEquals("Ideas", resolveUniqueTitle("Ideas", existing))

        // Existing name -> appends (1)
        assertEquals("Groceries (1)", resolveUniqueTitle("Groceries", existing))

        // Case-insensitive existing name -> appends (1)
        assertEquals("groceries (1)", resolveUniqueTitle("groceries", existing))

        // Name with existing (1) -> increments to (2)
        assertEquals("Project X (2)", resolveUniqueTitle("Project X", existing))
        assertEquals("Project X (2)", resolveUniqueTitle("Project X (1)", existing))

        // Blank name -> Untitled Document
        assertEquals("Untitled Document", resolveUniqueTitle("   ", existing))
        assertEquals("Untitled Document (1)", resolveUniqueTitle("   ", existing + "Untitled Document"))
    }

    @Test
    fun testFlattenExportItemsFromHierarchy() {
        val child1 = NestedItemExportData(id = "c1", text = "Child 1")
        val child2 = NestedItemExportData(id = "c2", text = "Child 2")
        val root = NestedItemExportData(id = "r1", text = "Root", children = listOf(child1, child2))

        val flat = flattenExportItems(listOf(root))
        assertEquals(3, flat.size)

        val rootFlat = flat.first { it.id == "r1" }
        assertEquals(null, rootFlat.parentId)
        assertEquals(0, rootFlat.position)

        val c1Flat = flat.first { it.id == "c1" }
        assertEquals("r1", c1Flat.parentId)
        assertEquals(0, c1Flat.position)

        val c2Flat = flat.first { it.id == "c2" }
        assertEquals("r1", c2Flat.parentId)
        assertEquals(1, c2Flat.position)
    }

    @Test
    fun testRemapItemsForImport() {
        val child = NestedItemExportData(id = "item-2", parentId = "item-1", text = "Child")
        val root = NestedItemExportData(id = "item-1", parentId = null, text = "Root")
        val items = listOf(root, child)

        // Without reassigning:
        val kept = remapItemsForImport(items, targetDocumentId = "target-doc", reassignIds = false)
        assertEquals("item-1", kept[0].id)
        assertEquals("item-2", kept[1].id)
        assertEquals("item-1", kept[1].parentId)

        // With reassigning:
        val remapped = remapItemsForImport(items, targetDocumentId = "target-doc", reassignIds = true)
        val newRootId = remapped[0].id
        val newChildId = remapped[1].id

        assertNotEquals("item-1", newRootId)
        assertNotEquals("item-2", newChildId)
        assertEquals(null, remapped[0].parentId)
        assertEquals(newRootId, remapped[1].parentId)
    }
}

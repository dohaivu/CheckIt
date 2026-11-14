package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Automatic numbering is derived from the tree, never stored, so these tests
 * pin the ordinal contract that the row rendering and Markdown export rely on.
 */
class NestedNumberingTest {

    private fun item(
        id: String,
        parentId: String? = null,
        position: Int = 0,
        text: String = id
    ) = NestedListItem(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = position,
        text = text,
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    private fun ordinals(nodes: List<NestedItemNode>): Map<String, Int> =
        nodes.associate { it.item.id to it.ordinal }

    @Test
    fun ordinalsFollowPositionThenId() {
        val roots = buildNestedTree(
            listOf(
                item("a", position = 1),
                item("c", position = 0),
                item("b", position = 0),
            )
        )

        assertEquals(listOf("b", "c", "a"), roots.map { it.item.id })
        assertEquals(mapOf("b" to 1, "c" to 2, "a" to 3), ordinals(roots))
    }

    @Test
    fun ordinalsRestartPerParent() {
        val roots = buildNestedTree(
            listOf(
                item("root-2", position = 1),
                item("root-1", position = 0),
                item("child-2", parentId = "root-1", position = 1),
                item("child-1", parentId = "root-1", position = 0),
            )
        )

        assertEquals(listOf(1, 2), roots.map { it.ordinal })
        val firstRoot = roots.single { it.item.id == "root-1" }
        assertEquals(listOf("child-1", "child-2"), firstRoot.children.map { it.item.id })
        assertEquals(listOf(1, 2), firstRoot.children.map { it.ordinal })
    }

    @Test
    fun orphanKeepsOrdinalFromItsRealGroup() {
        // Two rows share a missing parent: they form one sibling group, so the
        // numbers must come from that group rather than from the extra roots.
        val roots = buildNestedTree(
            listOf(
                item("live", position = 0),
                item("orphan-2", parentId = "gone", position = 1),
                item("orphan-1", parentId = "gone", position = 0),
            )
        )

        assertEquals(listOf("live", "orphan-1", "orphan-2"), roots.map { it.item.id })
        assertEquals(1, roots.single { it.item.id == "live" }.ordinal)
        assertEquals(1, roots.single { it.item.id == "orphan-1" }.ordinal)
        assertEquals(2, roots.single { it.item.id == "orphan-2" }.ordinal)
    }

    @Test
    fun cycleMembersStillGetAnOrdinal() {
        val roots = buildNestedTree(
            listOf(
                item("a", parentId = "b"),
                item("b", parentId = "a"),
            )
        )

        val all = mutableMapOf<String, Int>()
        fun walk(nodes: List<NestedItemNode>) {
            nodes.forEach { node ->
                all[node.item.id] = node.ordinal
                walk(node.children)
            }
        }
        walk(roots)

        assertEquals(setOf("a", "b"), all.keys)
        assertEquals(listOf(1, 1), all.values.sorted())
    }

    @Test
    fun filteringDoesNotRenumber() {
        val roots = buildNestedTree(
            listOf(
                item("first", text = "alpha"),
                item("second", text = "beta"),
                item("third", text = "gamma"),
            )
        )

        val filtered = filterNestedTree(roots, query = "gamma")

        assertEquals(listOf("third"), filtered.map { it.item.id })
        assertEquals(3, filtered.single().ordinal)
    }

    @Test
    fun ancestorContextKeepsAbsoluteOrdinalOfTheMatch() {
        val roots = buildNestedTree(
            listOf(
                item("parent", text = "group"),
                item("child-1", parentId = "parent", position = 0, text = "alpha"),
                item("child-2", parentId = "parent", position = 1, text = "needle"),
            )
        )

        val filtered = filterNestedTree(roots, query = "needle")

        val parent = filtered.single()
        assertEquals("parent", parent.item.id)
        assertEquals(1, parent.ordinal)
        assertEquals(listOf("child-2"), parent.children.map { it.item.id })
        assertEquals(2, parent.children.single().ordinal)
    }

    @Test
    fun ordinalMapMatchesDepthFirstGroups() {
        val roots = buildNestedTree(
            listOf(
                item("r1", position = 0),
                item("r2", position = 1),
                item("r1-c1", parentId = "r1", position = 0),
                item("r1-c2", parentId = "r1", position = 1),
                item("r1-c1-g1", parentId = "r1-c1", position = 0),
            )
        )

        val all = mutableMapOf<String, Int>()
        fun walk(nodes: List<NestedItemNode>) {
            nodes.forEach { node ->
                all[node.item.id] = node.ordinal
                walk(node.children)
            }
        }
        walk(roots)

        assertEquals(
            mapOf("r1" to 1, "r2" to 2, "r1-c1" to 1, "r1-c2" to 2, "r1-c1-g1" to 1),
            all
        )
    }
}

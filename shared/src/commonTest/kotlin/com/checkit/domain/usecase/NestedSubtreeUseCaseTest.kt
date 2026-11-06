package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.NestedDocument
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.NestedItemNode
import com.checkit.domain.NestedListItem
import com.checkit.domain.NestedSubtreeClip
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NestedSubtreeUseCaseTest {

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

    private fun tree() = NestedDocumentTree(
        document = NestedDocument("doc-1", "Doc", 1L, 2L),
        rootNodes = listOf(
            NestedItemNode(
                item = item("a", position = 0),
                children = listOf(NestedItemNode(item("a1", "a", 0)))
            ),
            NestedItemNode(item = item("b", position = 1))
        )
    )

    private class RecordingRepo(
        delegate: FakeCheckItRepository = FakeCheckItRepository()
    ) : CheckItRepository by delegate {
        val inserted = mutableListOf<Pair<String, List<NestedListItem>>>()

        override suspend fun addNestedSubtree(documentId: String, items: List<NestedListItem>) {
            inserted += documentId to items
        }
    }

    @Test
    fun copyStoresClipAndCountsDescendants() {
        val clipboard = NestedSubtreeClipboard()
        val clip = CopyNestedSubtreeUseCase(clipboard)(tree(), "a")

        assertEquals(2, clip?.itemCount)
        assertEquals(clip, clipboard.clip.value)
    }

    @Test
    fun copyUnknownIdLeavesClipboardUntouched() {
        val clipboard = NestedSubtreeClipboard()
        val existing = NestedSubtreeClip(emptyList(), "doc-9")
        clipboard.copy(existing)

        assertNull(CopyNestedSubtreeUseCase(clipboard)(tree(), "missing"))
        assertEquals(existing, clipboard.clip.value)
    }

    @Test
    fun pasteWithoutClipIsNoOp() = runTest {
        val repo = RecordingRepo()
        val count = PasteNestedSubtreeUseCase(repo, NestedSubtreeClipboard())("doc-1", null, tree().flatItems)

        assertEquals(0, count)
        assertTrue(repo.inserted.isEmpty())
    }

    @Test
    fun pasteAppendsAfterLastChildOfTarget() = runTest {
        val repo = RecordingRepo()
        val clipboard = NestedSubtreeClipboard().apply {
            copy(NestedSubtreeClip(listOf(NestedItemNode(item("x", position = 0))), "doc-1"))
        }
        val items = listOf(
            item("target", null, 0),
            item("child-1", "target", 0),
            item("child-2", "target", 2)
        )

        val count = PasteNestedSubtreeUseCase(repo, clipboard)("doc-2", "target", items)

        assertEquals(1, count)
        val (documentId, inserted) = repo.inserted.single()
        assertEquals("doc-2", documentId)
        assertEquals("target", inserted.single().parentId)
        assertEquals(3, inserted.single().position)
        assertEquals("item x", inserted.single().text)
    }

    @Test
    fun pasteAtRootAppendsAfterLastRoot() = runTest {
        val repo = RecordingRepo()
        val clipboard = NestedSubtreeClipboard().apply {
            copy(NestedSubtreeClip(listOf(NestedItemNode(item("x"))), "doc-1"))
        }
        val items = listOf(
            item("root-a", null, 0),
            item("child", "root-a", 0),
            item("root-b", null, 5)
        )

        PasteNestedSubtreeUseCase(repo, clipboard)("doc-1", null, items)

        val inserted = repo.inserted.single().second.single()
        assertNull(inserted.parentId)
        assertEquals(6, inserted.position)
    }

    @Test
    fun pasteKeepsClipboardForRepeatedPastes() = runTest {
        val repo = RecordingRepo()
        val clipboard = NestedSubtreeClipboard().apply {
            copy(NestedSubtreeClip(listOf(NestedItemNode(item("x"))), "doc-1"))
        }
        val useCase = PasteNestedSubtreeUseCase(repo, clipboard)

        useCase("doc-1", null, emptyList())
        useCase("doc-1", null, emptyList())

        assertEquals(2, repo.inserted.size)
        assertTrue(clipboard.clip.value != null)
    }
}

package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.NestedListItem
import com.checkit.domain.TagItem
import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DuplicateNestedItemUseCaseTest {

    private fun item(
        id: String,
        parentId: String? = null,
        position: Int = 0
    ) = NestedListItem(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = position,
        text = "item $id",
        createdAtMillis = 10L,
        updatedAtMillis = 20L
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
    fun duplicatePersistsOneRowAndReturnsItsId() = runTest {
        val repo = RecordingRepo()
        val result = DuplicateNestedItemUseCase(repo)(
            items = listOf(item("source", parentId = "parent", position = 2)),
            itemId = "source"
        )

        assertTrue(result != null)
        val (documentId, inserted) = repo.inserted.single()
        assertEquals("doc-1", documentId)
        assertEquals(1, inserted.size)
        assertEquals(result, inserted.single().id)
        assertEquals("parent", inserted.single().parentId)
        assertEquals(3, inserted.single().position)
    }

    @Test
    fun duplicateDoesNotCopyChildren() = runTest {
        val repo = RecordingRepo()
        val result = DuplicateNestedItemUseCase(repo)(
            items = listOf(
                item("source"),
                item("child", parentId = "source")
            ),
            itemId = "source"
        )

        assertTrue(result != null)
        val inserted = repo.inserted.single().second
        assertEquals(1, inserted.size)
        assertTrue(inserted.none { it.parentId == result })
    }

    @Test
    fun duplicateKeepsTagsForTheRowCopy() = runTest {
        val repo = RecordingRepo()
        val tag = TagItem(id = "tag-1", name = "work", color = "#2563EB")

        DuplicateNestedItemUseCase(repo)(
            items = listOf(item("source").copy(tags = listOf(tag))),
            itemId = "source"
        )

        assertEquals(listOf(tag), repo.inserted.single().second.single().tags)
    }

    @Test
    fun unknownSourceIsNoOp() = runTest {
        val repo = RecordingRepo()

        assertNull(
            DuplicateNestedItemUseCase(repo)(
                items = listOf(item("source")),
                itemId = "missing"
            )
        )
        assertTrue(repo.inserted.isEmpty())
    }
}

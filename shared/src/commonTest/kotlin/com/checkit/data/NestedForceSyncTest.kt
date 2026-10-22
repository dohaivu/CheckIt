package com.checkit.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NestedForceSyncTest {

    private val now = 5_000L

    private fun documentEntity(
        id: String = "doc-1",
        title: String = "Plan",
        sortOrder: Int = 3,
        createdAt: Long = 1_000L,
        updatedAt: Long = 2_000L,
        dirty: Boolean = false,
        deleted: Boolean = false,
    ) = NestedDocumentEntity(
        id = id,
        title = title,
        sortOrder = sortOrder,
        createdAtMillis = createdAt,
        updatedAtMillis = updatedAt,
        dirty = dirty,
        deleted = deleted,
    )

    private fun itemEntity(
        id: String,
        parentId: String? = null,
        position: Int = 0,
        text: String = id,
        createdAt: Long = 1_100L,
        updatedAt: Long = 2_100L,
        dirty: Boolean = false,
        deleted: Boolean = false,
    ) = NestedListItemEntity(
        id = id,
        documentId = "doc-1",
        parentId = parentId,
        position = position,
        text = text,
        createdAtMillis = createdAt,
        updatedAtMillis = updatedAt,
        dirty = dirty,
        deleted = deleted,
    )

    private fun remoteItem(
        id: String,
        documentId: String = "doc-1",
        parentId: String? = null,
        position: Int = 0,
        text: String = id,
        tagIds: List<String> = emptyList(),
        createdAt: Long = 3_000L,
        updatedAt: Long = 4_000L,
        deleted: Boolean = false,
    ) = RemoteNestedItem(
        id = id,
        documentId = documentId,
        parentId = parentId,
        position = position,
        text = text,
        note = null,
        checkboxEnabled = false,
        checked = false,
        collapsed = false,
        textStyle = "Body",
        textColor = "Default",
        backgroundColor = "Default",
        startDateEpochDays = null,
        endDateEpochDays = null,
        priority = "None",
        actualMinutes = 0,
        metricRollupPolicy = "IncludeChildren",
        showTrackedMinutes = false,
        progressPercent = null,
        manualMetricsJson = "[]",
        tagIds = tagIds,
        createdAtMillis = createdAt,
        updatedAtMillis = updatedAt,
        deleted = deleted,
    )

    @Test
    fun documentMapBumpsClockAndKeepsIdentity() {
        val map = NestedForceSync.documentMap(documentEntity(), now)

        assertEquals("doc-1", map[NestedSyncDocument.FIELD_ID])
        assertEquals("Plan", map[NestedSyncDocument.FIELD_TITLE])
        assertEquals(3L, (map[NestedSyncDocument.FIELD_SORT_ORDER] as Number).toLong())
        assertEquals(1_000L, (map[NestedSyncDocument.FIELD_CREATED_AT] as Number).toLong())
        assertEquals(now, (map[NestedSyncDocument.FIELD_UPDATED_AT] as Number).toLong())
        assertEquals(false, map[NestedSyncDocument.FIELD_DELETED])
    }

    @Test
    fun itemMapBumpsClockAndKeepsContentAndTags() {
        val map = NestedForceSync.itemMap(
            item = itemEntity(id = "item-1", parentId = "parent", position = 4, text = "Buy milk"),
            tagIds = listOf("tag-a", "tag-b"),
            nowMillis = now,
        )

        assertEquals("item-1", map[NestedSyncDocument.FIELD_ID])
        assertEquals("doc-1", map[NestedSyncDocument.FIELD_DOCUMENT_ID])
        assertEquals("parent", map[NestedSyncDocument.FIELD_PARENT_ID])
        assertEquals(4L, (map[NestedSyncDocument.FIELD_POSITION] as Number).toLong())
        assertEquals("Buy milk", map[NestedSyncDocument.FIELD_TEXT])
        assertEquals(listOf("tag-a", "tag-b"), map[NestedSyncDocument.FIELD_TAG_IDS])
        assertEquals(1_100L, (map[NestedSyncDocument.FIELD_CREATED_AT] as Number).toLong())
        assertEquals(now, (map[NestedSyncDocument.FIELD_UPDATED_AT] as Number).toLong())
    }

    @Test
    fun remoteExtrasToDeleteReturnsOnlyCloudOnlyRows() {
        val extras = NestedForceSync.remoteExtrasToDelete(
            localIds = listOf("a", "b"),
            remoteIds = listOf("a", "c", "b", "d"),
        )

        assertEquals(listOf("c", "d"), extras)
    }

    @Test
    fun remoteExtrasToDeleteIsEmptyForMirrorOrEmptyCloud() {
        assertEquals(emptyList(), NestedForceSync.remoteExtrasToDelete(listOf("a"), listOf("a")))
        assertEquals(emptyList(), NestedForceSync.remoteExtrasToDelete(listOf("a"), emptyList()))
    }

    @Test
    fun pullDocumentKeepsRemoteClocksAndMarksClean() {
        val map = mapOf(
            NestedSyncDocument.FIELD_ID to "doc-1",
            NestedSyncDocument.FIELD_TITLE to "Plan",
            NestedSyncDocument.FIELD_SORT_ORDER to 3L,
            NestedSyncDocument.FIELD_CREATED_AT to 1_000L,
            NestedSyncDocument.FIELD_UPDATED_AT to 4_000L,
            NestedSyncDocument.FIELD_DELETED to false,
        )

        val pulled = NestedForceSync.pullDocument(map, "doc-1")

        assertEquals("doc-1", pulled?.id)
        assertEquals("Plan", pulled?.title)
        assertEquals(3, pulled?.sortOrder)
        assertEquals(1_000L, pulled?.createdAtMillis)
        assertEquals(4_000L, pulled?.updatedAtMillis)
        assertEquals(false, pulled?.dirty)
        assertEquals(false, pulled?.deleted)
    }

    @Test
    fun pullDocumentRejectsTombstoneAndInvalidMap() {
        val tombstone = mapOf(
            NestedSyncDocument.FIELD_ID to "doc-1",
            NestedSyncDocument.FIELD_TITLE to "Plan",
            NestedSyncDocument.FIELD_CREATED_AT to 1_000L,
            NestedSyncDocument.FIELD_UPDATED_AT to 2_000L,
            NestedSyncDocument.FIELD_DELETED to true,
        )

        assertNull(NestedForceSync.pullDocument(tombstone, "doc-1"))
        assertNull(NestedForceSync.pullDocument(mapOf("title" to "no clocks"), "doc-1"))
    }

    @Test
    fun pullItemsOrdersParentsBeforeChildren() {
        val pulled = NestedForceSync.pullItems(
            items = listOf(
                remoteItem(id = "child", parentId = "parent", position = 0),
                remoteItem(id = "parent", position = 1),
            ),
            documentId = "doc-1",
            knownTagIds = emptySet(),
        )

        assertEquals(listOf("parent", "child"), pulled.map { it.item.id })
    }

    @Test
    fun pullItemsReparentsOrphansToRoot() {
        val pulled = NestedForceSync.pullItems(
            items = listOf(remoteItem(id = "orphan", parentId = "missing")),
            documentId = "doc-1",
            knownTagIds = emptySet(),
        )

        assertNull(pulled.single().item.parentId)
    }

    @Test
    fun pullItemsDropsDeletedRowsAndUnknownTags() {
        val pulled = NestedForceSync.pullItems(
            items = listOf(
                remoteItem(id = "live", tagIds = listOf("known", "unknown", "known")),
                remoteItem(id = "gone", deleted = true),
            ),
            documentId = "doc-1",
            knownTagIds = setOf("known"),
        )

        val row = pulled.single()
        assertEquals("live", row.item.id)
        assertEquals(listOf("known"), row.tagIds)
        assertEquals(false, row.item.dirty)
        assertEquals(false, row.item.deleted)
    }

    @Test
    fun pullItemsKeepsRemoteContentAndScopesDocumentId() {
        val pulled = NestedForceSync.pullItems(
            items = listOf(
                remoteItem(
                    id = "live",
                    documentId = "stale-doc",
                    text = "Cloud text",
                    createdAt = 3_000L,
                    updatedAt = 4_000L,
                )
            ),
            documentId = "doc-1",
            knownTagIds = emptySet(),
        )

        val item = pulled.single().item
        assertEquals("doc-1", item.documentId)
        assertEquals("Cloud text", item.text)
        assertEquals(3_000L, item.createdAtMillis)
        assertEquals(4_000L, item.updatedAtMillis)
        assertTrue(!item.dirty)
    }
}

package com.checkit.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NestedSyncDocumentTest {

    @Test
    fun docRoundTripPreservesFields() {
        val map = NestedSyncDocument.docToMap(
            id = "doc-1",
            title = "Groceries",
            createdAtMillis = 100L,
            updatedAtMillis = 200L,
            deleted = false,
        )
        val json = NestedSyncDocument.toJson(map)
        val decoded = NestedSyncDocument.mapFromJson(json)
        val remote = NestedSyncDocument.docFromMap("doc-1", decoded)
        assertEquals(
            RemoteNestedDocument(
                id = "doc-1",
                title = "Groceries",
                createdAtMillis = 100L,
                updatedAtMillis = 200L,
                deleted = false,
            ),
            remote,
        )
    }

    @Test
    fun docFromMapFallsBackToPathIdAndEmptyTitle() {
        val remote = NestedSyncDocument.docFromMap(
            "doc-9",
            mapOf(
                NestedSyncDocument.FIELD_CREATED_AT to 10L,
                NestedSyncDocument.FIELD_UPDATED_AT to 20L,
            ),
        )
        assertEquals("doc-9", remote?.id)
        assertEquals("", remote?.title)
        assertFalse(remote?.deleted ?: true)
    }

    @Test
    fun docFromMapNullOnMissingClocks() {
        assertNull(NestedSyncDocument.docFromMap("d", null))
        assertNull(
            NestedSyncDocument.docFromMap(
                "d",
                mapOf(NestedSyncDocument.FIELD_UPDATED_AT to 20L),
            )
        )
    }

    @Test
    fun itemRoundTripPreservesFieldsIncludingTags() {
        val map = NestedSyncDocument.itemToMap(
            id = "item-1",
            documentId = "doc-1",
            parentId = "parent-1",
            position = 3,
            text = "Buy milk",
            note = "2%",
            checkboxEnabled = true,
            checked = false,
            collapsed = true,
            textStyle = "Header",
            textColor = "Blue",
            backgroundColor = "Default",
            startDateEpochDays = 20000,
            endDateEpochDays = 20001,
            priority = "High",
            actualMinutes = 25,
            metricRollupPolicy = "OwnOnly",
            showTrackedMinutes = true,
            progressPercent = 50,
            manualMetricsJson = """[{"name":"m"}]""",
            tagIds = listOf("tag-1", "tag-2"),
            createdAtMillis = 100L,
            updatedAtMillis = 200L,
            deleted = false,
        )
        val json = NestedSyncDocument.toJson(map)
        val remote = NestedSyncDocument.itemFromMap(
            "doc-1",
            NestedSyncDocument.mapFromJson(json),
        )
        assertEquals("item-1", remote?.id)
        assertEquals("doc-1", remote?.documentId)
        assertEquals("parent-1", remote?.parentId)
        assertEquals(3, remote?.position)
        assertEquals("Buy milk", remote?.text)
        assertEquals("2%", remote?.note)
        assertTrue(remote?.checkboxEnabled ?: false)
        assertTrue(remote?.collapsed ?: false)
        assertEquals("Header", remote?.textStyle)
        assertEquals("High", remote?.priority)
        assertEquals(25, remote?.actualMinutes)
        assertEquals(listOf("tag-1", "tag-2"), remote?.tagIds)
        assertEquals(50, remote?.progressPercent)
        assertFalse(remote?.deleted ?: true)
    }

    @Test
    fun itemFromMapNullOnMissingRequiredFields() {
        val base = NestedSyncDocument.itemToMap(
            id = "item-1",
            documentId = "doc-1",
            parentId = null,
            position = 0,
            text = "x",
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
            tagIds = emptyList(),
            createdAtMillis = 100L,
            updatedAtMillis = 200L,
            deleted = false,
        )
        assertNull(NestedSyncDocument.itemFromMap("doc-1", null))
        assertNull(
            NestedSyncDocument.itemFromMap(
                "doc-1",
                base - NestedSyncDocument.FIELD_TEXT,
            )
        )
        assertNull(
            NestedSyncDocument.itemFromMap(
                "doc-1",
                (base - NestedSyncDocument.FIELD_ID) + (NestedSyncDocument.FIELD_ID to ""),
            )
        )
    }

    @Test
    fun tombstoneFlagRoundTrips() {
        val map = NestedSyncDocument.docToMap(
            id = "doc-1",
            title = "Gone",
            createdAtMillis = 100L,
            updatedAtMillis = 300L,
            deleted = true,
        )
        val remote = NestedSyncDocument.docFromMap(
            "doc-1",
            NestedSyncDocument.mapFromJson(NestedSyncDocument.toJson(map)),
        )
        assertTrue(remote?.deleted ?: false)
    }

    @Test
    fun shouldApplyRemoteIsLastWriteWins() {
        assertTrue(NestedSyncDocument.shouldApplyRemote(null, 200L))
        assertTrue(NestedSyncDocument.shouldApplyRemote(100L, 200L))
        // Equal timestamps keep local (idempotent re-pulls).
        assertFalse(NestedSyncDocument.shouldApplyRemote(200L, 200L))
        assertFalse(NestedSyncDocument.shouldApplyRemote(300L, 200L))
    }

    @Test
    fun mapFromJsonNullOnInvalidInput() {
        assertNull(NestedSyncDocument.mapFromJson("not json"))
    }
}

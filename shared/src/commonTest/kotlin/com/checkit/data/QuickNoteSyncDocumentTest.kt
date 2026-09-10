package com.checkit.data

import com.checkit.domain.QuickNote
import com.checkit.domain.QuickNoteStatus
import com.checkit.domain.QuickNoteType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuickNoteSyncDocumentTest {
    private val now = 1_700_000_000_000L

    private fun note(
        id: String = "abc",
        updatedAt: Long = now,
        deleted: Boolean = false,
    ) = QuickNote(
        id = id,
        content = "Check weather",
        status = QuickNoteStatus.NEXT,
        createdAt = now - 1000,
        updatedAt = updatedAt,
        sortOrder = 1000.0,
        remindAt = now + 3_600_000L,
        deleteAt = null,
        deleted = deleted,
    )

    @Test
    fun roundTripPreservesAllFields() {
        val original = note()
        val restored = QuickNoteSyncDocument.fromMap(original.id, QuickNoteSyncDocument.toMap(original))
        assertEquals(original, restored)
    }

    @Test
    fun roundTripPreservesNullsAndTombstone() {
        val original = note().copy(
            status = QuickNoteStatus.TO_BE_DELETED,
            remindAt = null,
            deleteAt = now + 86_400_000L,
            deleted = true,
        )
        val restored = QuickNoteSyncDocument.fromMap(original.id, QuickNoteSyncDocument.toMap(original))
        assertEquals(original, restored)
    }

    @Test
    fun roundTripPreservesTypeAndAttachmentUrl() {
        val original = note().copy(
            type = QuickNoteType.IMAGE,
            attachmentLocalPath = "/data/local/photo.webp",
            attachmentUrl = "https://example.com/photo.webp",
        )
        val restored = QuickNoteSyncDocument.fromMap(original.id, QuickNoteSyncDocument.toMap(original))
        assertEquals(QuickNoteType.IMAGE, restored?.type)
        assertEquals("https://example.com/photo.webp", restored?.attachmentUrl)
        // Local paths never leave the device.
        assertNull(restored?.attachmentLocalPath)
    }

    @Test
    fun fromMapFallsBackToTextForUnknownType() {
        val map = QuickNoteSyncDocument.toMap(note()) + (QuickNoteSyncDocument.FIELD_TYPE to "HOLOGRAM")
        assertEquals(QuickNoteType.TEXT, QuickNoteSyncDocument.fromMap("x", map)?.type)
    }

    @Test
    fun fromMapFallsBackToDocumentId() {
        val map = QuickNoteSyncDocument.toMap(note()).minus(QuickNoteSyncDocument.FIELD_ID)
        val restored = QuickNoteSyncDocument.fromMap("doc-id", map)
        assertEquals("doc-id", restored?.id)
    }

    @Test
    fun fromMapRejectsInvalidDocuments() {
        assertNull(QuickNoteSyncDocument.fromMap("x", null))
        assertNull(QuickNoteSyncDocument.fromMap("x", emptyMap()))
        assertNull(
            QuickNoteSyncDocument.fromMap(
                "x",
                QuickNoteSyncDocument.toMap(note()).minus(QuickNoteSyncDocument.FIELD_CONTENT),
            ),
        )
    }

    @Test
    fun fromMapAcceptsIntNumbers() {
        // Firestore may decode small longs as Int/Long and doubles as Double.
        val map = mapOf(
            QuickNoteSyncDocument.FIELD_CONTENT to "hi",
            QuickNoteSyncDocument.FIELD_CREATED_AT to 100,
            QuickNoteSyncDocument.FIELD_UPDATED_AT to 200,
            QuickNoteSyncDocument.FIELD_SORT_ORDER to 1000,
            QuickNoteSyncDocument.FIELD_DELETED to false,
        )
        val restored = QuickNoteSyncDocument.fromMap("x", map)
        assertEquals(100L, restored?.createdAt)
        assertEquals(200L, restored?.updatedAt)
        assertEquals(1000.0, restored?.sortOrder)
        assertEquals(QuickNoteStatus.NEXT, restored?.status)
    }

    @Test
    fun lwwAcceptsRemoteWhenMissingOrNewer() {
        val remote = note(updatedAt = 200)
        assertEquals(remote, QuickNoteSyncDocument.resolveLocal(null, remote))
        assertEquals(remote, QuickNoteSyncDocument.resolveLocal(note(updatedAt = 100), remote))
    }

    @Test
    fun lwwKeepsLocalWhenNewerOrEqual() {
        val remote = note(updatedAt = 100)
        assertNull(QuickNoteSyncDocument.resolveLocal(note(updatedAt = 200), remote))
        assertNull(QuickNoteSyncDocument.resolveLocal(note(updatedAt = 100), remote))
    }

    @Test
    fun lwwPropagatesNewerTombstone() {
        val tombstone = note(updatedAt = 300, deleted = true)
        val applied = QuickNoteSyncDocument.resolveLocal(note(updatedAt = 100), tombstone)
        assertTrue(applied?.deleted == true)
    }
}

package com.checkit.data

import com.checkit.domain.QuickNote
import com.checkit.domain.QuickNoteStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickNoteDirtyFlagTest {
    private fun note() = QuickNote(
        id = "x",
        content = "hi",
        status = QuickNoteStatus.NEXT,
        createdAt = 100L,
        updatedAt = 200L,
        sortOrder = 1000.0,
        remindAt = null,
        deleteAt = null,
        deleted = false,
    )

    @Test
    fun localMutationsDefaultToDirty() {
        assertTrue(note().toEntity().dirty)
    }

    @Test
    fun mergedRemoteRowsCanBeStoredClean() {
        val merged = QuickNoteSyncDocument.resolveLocal(null, note())
        assertTrue(merged?.toEntity(dirty = false)?.dirty == false)
    }

    @Test
    fun entityDefaultsToDirty() {
        assertFalse(
            QuickNoteEntity(
                id = "x",
                content = "hi",
                status = "NEXT",
                createdAt = 100L,
                updatedAt = 200L,
                sortOrder = 1000.0,
                remindAt = null,
                deleteAt = null,
                deleted = false,
                dirty = false,
            ).dirty
        )
    }
}

package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuickNoteRulesTest {
    private val now = 1_700_000_000_000L

    private fun note(
        id: String = "1",
        status: QuickNoteStatus = QuickNoteStatus.NEXT,
        sortOrder: Double = 1000.0,
        remindAt: Long? = null,
        deleteAt: Long? = null,
        deleted: Boolean = false,
        updatedAt: Long = now,
    ) = QuickNote(
        id = id,
        content = "hello",
        status = status,
        createdAt = now,
        updatedAt = updatedAt,
        sortOrder = sortOrder,
        remindAt = remindAt,
        deleteAt = deleteAt,
        deleted = deleted,
    )

    @Test
    fun newNoteDefaultsToNext() {
        val created = QuickNoteRules.newNote("  Check weather ", now, null, id = "fixed-id")
        assertEquals("fixed-id", created.id)
        assertEquals("Check weather", created.content)
        assertEquals(QuickNoteStatus.NEXT, created.status)
        assertEquals(now, created.createdAt)
        assertEquals(now, created.updatedAt)
        assertEquals(1000.0, created.sortOrder)
        assertNull(created.remindAt)
        assertNull(created.deleteAt)
        assertFalse(created.deleted)
    }

    @Test
    fun newNoteAppendsToBottom() {
        val created = QuickNoteRules.newNote("b", now, 2000.0, id = "x")
        assertEquals(3000.0, created.sortOrder)
    }

    @Test
    fun blankTextIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            QuickNoteRules.newNote("   ", now, null, id = "x")
        }
    }

    @Test
    fun swipeRightSetsToBeDeleted() {
        val moved = QuickNoteRules.moveToBeDeleted(note(remindAt = now + 1000), now)
        assertEquals(QuickNoteStatus.TO_BE_DELETED, moved.status)
        assertEquals(now + QuickNoteRules.DELETE_AFTER_MILLIS, moved.deleteAt)
        assertNull(moved.remindAt)
        assertEquals(now, moved.updatedAt)
    }

    @Test
    fun reminder30MinSetsRemindAt() {
        val at = QuickNoteRules.reminderAt(now, QuickNoteRules.REMINDER_30_MIN_MILLIS)
        assertEquals(now + 30L * 60L * 1000L, at)
        val updated = QuickNoteRules.setReminder(note(), at, now)
        assertEquals(at, updated.remindAt)
        assertEquals(QuickNoteStatus.NEXT, updated.status)
    }

    @Test
    fun reminder1HourSetsRemindAt() {
        val at = QuickNoteRules.reminderAt(now, QuickNoteRules.REMINDER_1_HOUR_MILLIS)
        val updated = QuickNoteRules.setReminder(note(), at, now)
        assertEquals(now + 3_600_000L, updated.remindAt)
        assertEquals(QuickNoteStatus.NEXT, updated.status)
    }

    @Test
    fun reminderDoesNotChangeStatus() {
        val deleted = note(status = QuickNoteStatus.TO_BE_DELETED)
        val updated = QuickNoteRules.setReminder(deleted, now + 1000, now)
        assertEquals(QuickNoteStatus.TO_BE_DELETED, updated.status)
    }

    @Test
    fun expirationMarksDeletedAfter24hButNotBefore() {
        val moved = QuickNoteRules.moveToBeDeleted(note(), now)
        assertFalse(QuickNoteRules.isExpired(moved, now + QuickNoteRules.DELETE_AFTER_MILLIS - 1))
        assertTrue(QuickNoteRules.isExpired(moved, now + QuickNoteRules.DELETE_AFTER_MILLIS))
        assertTrue(QuickNoteRules.isExpired(moved, now + QuickNoteRules.DELETE_AFTER_MILLIS + 1))
        val marked = QuickNoteRules.markDeleted(moved, now + QuickNoteRules.DELETE_AFTER_MILLIS)
        assertTrue(marked.deleted)
    }

    @Test
    fun nextNotesNeverExpire() {
        assertFalse(QuickNoteRules.isExpired(note(), now + QuickNoteRules.DELETE_AFTER_MILLIS * 2))
    }

    @Test
    fun sortOrderMiddleFirstLast() {
        assertEquals(1500.0, QuickNoteRules.sortOrderBetween(1000.0, 2000.0))
        assertEquals(0.0, QuickNoteRules.sortOrderBetween(null, 1000.0))
        assertEquals(3000.0, QuickNoteRules.sortOrderBetween(2000.0, null))
        assertEquals(1000.0, QuickNoteRules.sortOrderBetween(null, null))
    }

    @Test
    fun normalizationTriggersWhenTooClose() {
        val notes = listOf(note("a", sortOrder = 1.0), note("b", sortOrder = 1.0 + 1e-9))
        assertTrue(QuickNoteRules.needsNormalization(notes))
        val ok = listOf(note("a", sortOrder = 1000.0), note("b", sortOrder = 2000.0))
        assertFalse(QuickNoteRules.needsNormalization(ok))
        val normalized = QuickNoteRules.normalizedOrders(ok)
        assertEquals(1000.0, normalized["a"])
        assertEquals(2000.0, normalized["b"])
    }

    @Test
    fun lwwConflictResolution() {
        val local = note("1", updatedAt = 100)
        val remoteNewer = note("1", updatedAt = 200)
        val remoteOlder = note("1", updatedAt = 50)
        assertTrue(QuickNoteRules.shouldAcceptRemote(local, remoteNewer))
        assertFalse(QuickNoteRules.shouldAcceptRemote(local, remoteOlder))
        assertFalse(QuickNoteRules.shouldAcceptRemote(local, local))
    }
}

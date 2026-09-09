package com.checkit.data

import com.checkit.domain.QuickNote
import com.checkit.domain.QuickNoteRules
import com.checkit.domain.QuickNoteStatus
import com.checkit.notifications.QuickNoteReminderScheduler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

private class RecordingScheduler : QuickNoteReminderScheduler {
    val scheduled = mutableListOf<QuickNote>()
    val cancelled = mutableListOf<String>()
    override suspend fun schedule(note: QuickNote) {
        scheduled.add(note)
    }

    override suspend fun cancel(noteId: String) {
        cancelled.add(noteId)
    }
}

private class InMemoryQuickNoteRepository(
    private val scheduler: RecordingScheduler = RecordingScheduler(),
) {
    private val notes = mutableMapOf<String, QuickNote>()
    val syncRequests = mutableListOf<Unit>()

    fun snapshot(): List<QuickNote> = notes.values.toList()

    suspend fun create(content: String): QuickNote? {
        if (content.isBlank()) return null
        val now = Clock.System.now().toEpochMilliseconds()
        val bottom = notes.values.filter { it.status == QuickNoteStatus.NEXT && !it.deleted }.maxOfOrNull { it.sortOrder } ?: 0.0
        val note = QuickNoteRules.newNote(content, now, bottom)
        notes[note.id] = note
        syncRequests.add(Unit)
        return note
    }

    suspend fun moveToBeDeleted(id: String) {
        val existing = notes[id] ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        notes[id] = QuickNoteRules.moveToBeDeleted(existing, now)
        scheduler.cancel(id)
        syncRequests.add(Unit)
    }

    suspend fun setReminder(id: String, remindAt: Long?) {
        val existing = notes[id] ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        if (remindAt != null && remindAt <= now) return
        notes[id] = QuickNoteRules.setReminder(existing, remindAt, now)
        if (remindAt != null) scheduler.schedule(notes[id]!!) else scheduler.cancel(id)
        syncRequests.add(Unit)
    }

    suspend fun processExpired(now: Long): Int {
        var count = 0
        notes.forEach { (id, note) ->
            if (QuickNoteRules.isExpired(note, now)) {
                notes[id] = QuickNoteRules.markDeleted(note, now)
                scheduler.cancel(id)
                count++
            }
        }
        if (count > 0) syncRequests.add(Unit)
        return count
    }
}

class QuickNoteRepositoryTest {
    @Test
    fun captureAppearsImmediatelyWithoutNetwork() = runTest {
        val repo = InMemoryQuickNoteRepository()
        val created = repo.create("  Buy milk ")
        assertEquals("Buy milk", created?.content)
        assertEquals(1, repo.snapshot().size)
        assertEquals(1, repo.syncRequests.size)
    }

    @Test
    fun blankCaptureIsIgnored() = runTest {
        val repo = InMemoryQuickNoteRepository()
        assertNull(repo.create("   "))
        assertTrue(repo.snapshot().isEmpty())
        assertTrue(repo.syncRequests.isEmpty())
    }

    @Test
    fun swipeActionsUpdateImmediatelyAndCancelReminder() = runTest {
        val scheduler = RecordingScheduler()
        val repo = InMemoryQuickNoteRepository(scheduler)
        val created = repo.create("task")!!
        val future = Clock.System.now().toEpochMilliseconds() + 3_600_000L
        repo.setReminder(created.id, future)
        assertEquals(1, scheduler.scheduled.size)
        repo.moveToBeDeleted(created.id)
        val moved = repo.snapshot().first()
        assertEquals(QuickNoteStatus.TO_BE_DELETED, moved.status)
        assertNull(moved.remindAt)
        assertTrue(scheduler.cancelled.contains(created.id))
    }

    @Test
    fun expiredItemsDisappearAfterResume() = runTest {
        val repo = InMemoryQuickNoteRepository()
        val created = repo.create("old")!!
        repo.moveToBeDeleted(created.id)
        val moved = repo.snapshot().first()
        val before = repo.processExpired(moved.deleteAt!! - 1)
        assertEquals(0, before)
        assertEquals(1, repo.snapshot().count { !it.deleted })
        val after = repo.processExpired(moved.deleteAt + 1)
        assertEquals(1, after)
        assertEquals(0, repo.snapshot().count { !it.deleted })
    }
}

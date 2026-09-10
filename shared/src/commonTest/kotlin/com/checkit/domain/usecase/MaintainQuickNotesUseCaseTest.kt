package com.checkit.domain.usecase

import com.checkit.data.NoOpQuickNoteSyncManager
import com.checkit.data.QuickNoteRepository
import com.checkit.domain.QuickNote
import com.checkit.domain.QuickNoteStatus
import com.checkit.domain.QuickNoteType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock

private class FakeQuickNoteRepository : QuickNoteRepository {
    private val notes = mutableMapOf<String, QuickNote>()
    private val flow = MutableStateFlow(0)
    var reconciled = 0
        private set

    fun put(note: QuickNote) {
        notes[note.id] = note
    }

    fun get(id: String): QuickNote? = notes[id]

    private fun active() = notes.values.filter { !it.deleted }

    override fun observeNext(): Flow<List<QuickNote>> =
        flow.map { active().filter { it.status == QuickNoteStatus.NEXT } }

    override fun observeToBeDeleted(): Flow<List<QuickNote>> =
        flow.map { active().filter { it.status == QuickNoteStatus.TO_BE_DELETED } }

    override suspend fun create(content: String): QuickNote? = null

    override suspend fun create(
        content: String,
        type: QuickNoteType,
        attachmentLocalPath: String?,
    ): QuickNote? = null

    override suspend fun moveToBeDeleted(id: String) = Unit

    override suspend fun deletePermanently(id: String) {
        notes[id]?.let { notes[id] = it.copy(deleted = true) }
    }

    override suspend fun restore(id: String) = Unit

    override suspend fun setReminder(id: String, remindAt: Long?) {
        notes[id]?.let { notes[id] = it.copy(remindAt = remindAt) }
    }

    override suspend fun clearReminder(id: String) {
        notes[id]?.let { notes[id] = it.copy(remindAt = null) }
    }

    override suspend fun clearExpiredReminders(): Int {
        val now = Clock.System.now().toEpochMilliseconds()
        var count = 0
        notes.forEach { (id, note) ->
            if (!note.deleted && note.remindAt != null && note.remindAt <= now) {
                notes[id] = note.copy(remindAt = null)
                count++
            }
        }
        return count
    }

    override suspend fun move(id: String, beforeId: String?, afterId: String?) = Unit

    override suspend fun reorder(fromIndex: Int, toIndex: Int) = Unit

    override suspend fun processExpired(): Int = 0

    override suspend fun reconcileReminders() {
        reconciled++
    }
}

class MaintainQuickNotesUseCaseTest {
    private val now = Clock.System.now().toEpochMilliseconds()

    private fun note(id: String, remindAt: Long?) = QuickNote(
        id = id,
        content = id,
        status = QuickNoteStatus.NEXT,
        createdAt = now - 1000,
        updatedAt = now - 1000,
        sortOrder = 1000.0,
        remindAt = remindAt,
        deleteAt = null,
        deleted = false,
    )

    @Test
    fun clearsPastDueRemindersButKeepsFutureOnes() = runTest {
        val repo = FakeQuickNoteRepository().apply {
            put(note("past", now - 60_000L))
            put(note("future", now + 3_600_000L))
            put(note("none", null))
        }
        MaintainQuickNotesUseCase(repo, NoOpQuickNoteSyncManager())()

        assertNull(repo.get("past")?.remindAt)
        assertEquals(now + 3_600_000L, repo.get("future")?.remindAt)
        assertNull(repo.get("none")?.remindAt)
        assertEquals(1, repo.reconciled)
    }
}

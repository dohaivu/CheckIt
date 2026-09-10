package com.checkit.data

import com.checkit.domain.QuickNote
import com.checkit.domain.QuickNoteRules
import com.checkit.domain.QuickNoteStatus
import com.checkit.domain.QuickNoteType
import com.checkit.notifications.NoOpQuickNoteReminderScheduler
import com.checkit.notifications.QuickNoteReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

interface QuickNoteRepository {
    fun observeNext(): Flow<List<QuickNote>>
    fun observeToBeDeleted(): Flow<List<QuickNote>>

    suspend fun create(content: String): QuickNote?
    suspend fun create(
        content: String,
        type: QuickNoteType,
        attachmentLocalPath: String?,
    ): QuickNote?
    suspend fun moveToBeDeleted(id: String)
    suspend fun deletePermanently(id: String)
    suspend fun restore(id: String)
    suspend fun setReminder(id: String, remindAt: Long?)
    suspend fun clearReminder(id: String)
    suspend fun clearExpiredReminders(): Int
    suspend fun autoTrashInactive(): Int
    suspend fun move(id: String, beforeId: String?, afterId: String?)
    suspend fun reorder(fromIndex: Int, toIndex: Int)
    suspend fun processExpired(): Int
    suspend fun reconcileReminders()
}

class RoomQuickNoteRepository(
    private val dao: QuickNoteDao,
    private val reminderScheduler: QuickNoteReminderScheduler = NoOpQuickNoteReminderScheduler(),
    private val syncManager: QuickNoteSyncManager = NoOpQuickNoteSyncManager(),
) : QuickNoteRepository {

    override fun observeNext(): Flow<List<QuickNote>> =
        dao.observeNext().map { rows -> rows.map { it.toDomain() } }

    override fun observeToBeDeleted(): Flow<List<QuickNote>> =
        dao.observeToBeDeleted().map { rows -> rows.map { it.toDomain() } }

    override suspend fun create(content: String): QuickNote? =
        create(content, QuickNoteType.TEXT, null)

    override suspend fun create(
        content: String,
        type: QuickNoteType,
        attachmentLocalPath: String?,
    ): QuickNote? {
        if (content.isBlank()) return null
        if (type != QuickNoteType.TEXT && attachmentLocalPath.isNullOrBlank()) return null
        val now = Clock.System.now().toEpochMilliseconds()
        val bottom = dao.maxNextSortOrder()
        val note = QuickNoteRules.newNote(content, now, bottom, type = type, attachmentLocalPath = attachmentLocalPath)
        dao.upsert(note.toEntity())
        syncManager.requestSync()
        return note
    }

    override suspend fun moveToBeDeleted(id: String) {
        val entity = dao.getById(id) ?: return
        if (entity.deleted) return
        val now = Clock.System.now().toEpochMilliseconds()
        val moved = QuickNoteRules.moveToBeDeleted(entity.toDomain(), now)
        dao.moveToBeDeleted(moved.id, moved.deleteAt ?: (now + QuickNoteRules.DELETE_AFTER_MILLIS), now)
        reminderScheduler.cancel(id)
        syncManager.requestSync()
    }

    override suspend fun deletePermanently(id: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.markDeleted(id, now)
        reminderScheduler.cancel(id)
        syncManager.requestSync()
    }

    override suspend fun restore(id: String) {
        val entity = dao.getById(id) ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        val bottom = dao.maxNextSortOrder()
        val restored = QuickNoteRules.restore(entity.toDomain(), now, bottom)
        dao.restore(restored.id, restored.sortOrder, now)
        syncManager.requestSync()
    }

    override suspend fun setReminder(id: String, remindAt: Long?) {
        val entity = dao.getById(id) ?: return
        if (entity.deleted) return
        val now = Clock.System.now().toEpochMilliseconds()
        if (remindAt != null && remindAt <= now) return
        val updated = QuickNoteRules.setReminder(entity.toDomain(), remindAt, now)
        dao.setReminder(id, updated.remindAt, now)
        if (updated.remindAt != null) {
            reminderScheduler.schedule(updated)
        } else {
            reminderScheduler.cancel(id)
        }
        syncManager.requestSync()
    }

    override suspend fun clearReminder(id: String) {
        val entity = dao.getById(id) ?: return
        if (entity.remindAt == null) return
        val now = Clock.System.now().toEpochMilliseconds()
        dao.setReminder(id, null, now)
        syncManager.requestSync()
    }

    override suspend fun clearExpiredReminders(): Int {
        val now = Clock.System.now().toEpochMilliseconds()
        val expired = dao.getDueReminders(now)
        if (expired.isEmpty()) return 0
        expired.forEach { reminderScheduler.cancel(it.id) }
        dao.clearExpiredReminders(now)
        syncManager.requestSync()
        return expired.size
    }

    override suspend fun autoTrashInactive(): Int {
        val now = Clock.System.now().toEpochMilliseconds()
        val inactive = dao.getInactiveNext(now - QuickNoteRules.INACTIVITY_AFTER_MILLIS)
        inactive.forEach { entity ->
            dao.moveToBeDeleted(
                entity.id,
                now + QuickNoteRules.DELETE_AFTER_MILLIS,
                now,
            )
            reminderScheduler.cancel(entity.id)
        }
        if (inactive.isNotEmpty()) syncManager.requestSync()
        return inactive.size
    }

    override suspend fun move(id: String, beforeId: String?, afterId: String?) {
        val active = dao.getAllActive()
            .map { it.toDomain() }
            .filter { it.status == QuickNoteStatus.NEXT }
            .sortedBy { it.sortOrder }
        val dragged = active.firstOrNull { it.id == id } ?: return
        if (beforeId == null && afterId == null) return
        val before = beforeId?.let { bid -> active.firstOrNull { it.id == bid }?.sortOrder }
        val after = afterId?.let { aid -> active.firstOrNull { it.id == aid }?.sortOrder }
        if (beforeId != null && before == null) return
        if (afterId != null && after == null) return
        val now = Clock.System.now().toEpochMilliseconds()
        val newOrder = QuickNoteRules.sortOrderBetween(before, after)
        dao.updateSortOrder(dragged.id, newOrder, now)
        maybeNormalize(active, newOrder, dragged.id, now)
        syncManager.requestSync()
    }

    override suspend fun reorder(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val active = dao.getAllActive()
            .map { it.toDomain() }
            .filter { it.status == QuickNoteStatus.NEXT }
            .sortedBy { it.sortOrder }
        if (fromIndex !in active.indices || toIndex !in active.indices) return
        val reordered = active.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        val moved = reordered[toIndex]
        val before = reordered.getOrNull(toIndex - 1)?.takeIf { it.id != moved.id }?.sortOrder
        val after = reordered.getOrNull(toIndex + 1)?.takeIf { it.id != moved.id }?.sortOrder
        val now = Clock.System.now().toEpochMilliseconds()
        val newOrder = QuickNoteRules.sortOrderBetween(before, after)
        dao.updateSortOrder(moved.id, newOrder, now)
        maybeNormalize(reordered, newOrder, moved.id, now)
        syncManager.requestSync()
    }

    override suspend fun processExpired(): Int {
        val now = Clock.System.now().toEpochMilliseconds()
        val expired = dao.getExpired(now)
        expired.forEach { entity ->
            dao.markDeleted(entity.id, now)
            reminderScheduler.cancel(entity.id)
        }
        if (expired.isNotEmpty()) syncManager.requestSync()
        return expired.size
    }

    override suspend fun reconcileReminders() {
        val scheduled = dao.getScheduledReminders().map { it.toDomain() }
        val now = Clock.System.now().toEpochMilliseconds()
        scheduled.filter { it.remindAt != null && it.remindAt > now }.forEach {
            reminderScheduler.schedule(it)
        }
        scheduled.filter { it.deleted || it.remindAt == null }.forEach {
            reminderScheduler.cancel(it.id)
        }
    }

    private suspend fun maybeNormalize(
        ordered: List<QuickNote>,
        justWritten: Double,
        justWrittenId: String,
        now: Long,
    ) {
        val withNew = ordered.map { if (it.id == justWrittenId) it.copy(sortOrder = justWritten) else it }
        if (!QuickNoteRules.needsNormalization(withNew)) return
        QuickNoteRules.normalizedOrders(withNew).forEach { (noteId, order) ->
            dao.updateSortOrder(noteId, order, now)
        }
    }
}

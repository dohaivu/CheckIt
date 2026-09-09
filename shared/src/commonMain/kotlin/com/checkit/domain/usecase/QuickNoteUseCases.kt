package com.checkit.domain.usecase

import com.checkit.data.QuickNoteRepository
import com.checkit.data.QuickNoteSyncManager
import com.checkit.domain.QuickNote
import com.checkit.domain.QuickNoteRules
import kotlinx.coroutines.flow.Flow

class ObserveQuickNextUseCase(
    private val repository: QuickNoteRepository,
) {
    operator fun invoke(): Flow<List<QuickNote>> = repository.observeNext()
}

class ObserveQuickToBeDeletedUseCase(
    private val repository: QuickNoteRepository,
) {
    operator fun invoke(): Flow<List<QuickNote>> = repository.observeToBeDeleted()
}

class CreateQuickNoteUseCase(
    private val repository: QuickNoteRepository,
) {
    suspend operator fun invoke(content: String): QuickNote? {
        if (content.isBlank()) return null
        return repository.create(content)
    }
}

class MoveQuickNoteToBeDeletedUseCase(
    private val repository: QuickNoteRepository,
) {
    suspend operator fun invoke(id: String) = repository.moveToBeDeleted(id)
}

class SetQuickNoteReminderUseCase(
    private val repository: QuickNoteRepository,
) {
    suspend operator fun invoke(id: String, durationMillis: Long) {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        repository.setReminder(id, QuickNoteRules.reminderAt(now, durationMillis))
    }

    suspend fun clear(id: String) = repository.setReminder(id, null)
}

class MoveQuickNoteUseCase(
    private val repository: QuickNoteRepository,
) {
    suspend operator fun invoke(id: String, beforeId: String?, afterId: String?) =
        repository.move(id, beforeId, afterId)

    suspend fun reorder(fromIndex: Int, toIndex: Int) = repository.reorder(fromIndex, toIndex)
}

class ProcessExpiredQuickNotesUseCase(
    private val repository: QuickNoteRepository,
) {
    suspend operator fun invoke(): Int = repository.processExpired()
}

class ReconcileQuickNoteRemindersUseCase(
    private val repository: QuickNoteRepository,
) {
    suspend operator fun invoke() = repository.reconcileReminders()
}

/**
 * Periodic maintenance previously done by a WorkManager worker: expire
 * 24h notes, reconcile alarms after restarts, and retry pending sync.
 * Called from the app's startup/resume maintenance path.
 */
class MaintainQuickNotesUseCase(
    private val repository: QuickNoteRepository,
    private val syncManager: QuickNoteSyncManager,
) {
    suspend operator fun invoke(): Int {
        val expiredCount = repository.processExpired()
        repository.reconcileReminders()
        runCatching { syncManager.sync() }
        return expiredCount
    }
}

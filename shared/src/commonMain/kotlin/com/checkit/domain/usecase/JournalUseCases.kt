package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.JournalEntryWriteInput
import com.checkit.domain.JournalEntry
import kotlinx.coroutines.flow.Flow

class ObserveJournalEntriesUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(): Flow<List<JournalEntry>> = repository.observeJournalEntries()
}

class AddJournalEntryUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(input: JournalEntryWriteInput): Result<Long> {
        if (input.content.trim().isBlank() && input.label.isNullOrBlank()) {
            return Result.failure(Exception("Add a note"))
        }
        return runCatching { repository.addJournalEntry(input) }
    }
}

class UpdateJournalEntryUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(entryId: Long, input: JournalEntryWriteInput): Result<Unit> =
        runCatching { repository.updateJournalEntry(entryId, input) }
}

class DeleteJournalEntryUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(entryId: Long) = repository.deleteJournalEntry(entryId)
}

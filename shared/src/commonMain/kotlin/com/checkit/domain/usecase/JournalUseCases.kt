package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.JournalEntryWriteInput
import com.checkit.domain.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Observes journal entries, optionally limited to an inclusive date window. */
class ObserveJournalEntriesUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(
        startDate: LocalDate? = null,
        endDateInclusive: LocalDate? = null
    ): Flow<List<JournalEntry>> =
        if (startDate != null && endDateInclusive != null) {
            repository.observeJournalEntriesInRange(startDate, endDateInclusive)
        } else {
            repository.observeJournalEntries()
        }
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

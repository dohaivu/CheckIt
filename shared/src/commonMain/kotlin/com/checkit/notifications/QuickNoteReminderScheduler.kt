package com.checkit.notifications

import com.checkit.domain.QuickNote

interface QuickNoteReminderScheduler {
    suspend fun schedule(note: QuickNote)
    suspend fun cancel(noteId: String)
    suspend fun reconcile(notes: List<QuickNote>) {
        notes.forEach { note ->
            if (note.remindAt != null && !note.deleted) schedule(note) else cancel(note.id)
        }
    }
}

class NoOpQuickNoteReminderScheduler : QuickNoteReminderScheduler {
    override suspend fun schedule(note: QuickNote) = Unit
    override suspend fun cancel(noteId: String) = Unit
}

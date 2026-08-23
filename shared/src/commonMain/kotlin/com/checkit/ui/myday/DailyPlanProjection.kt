package com.checkit.ui.myday

import com.checkit.domain.DailyPlanItem
import com.checkit.domain.JournalEntry
import com.checkit.domain.NoteItem

data class DayViewProjection(
    val items: List<DailyPlanItem>,
    val notes: List<NoteItem>,
    val journalEntries: List<JournalEntry>,
)

fun List<DailyPlanItem>.toDayViewProjection(
    notes: List<NoteItem>,
    journalEntries: List<JournalEntry>
): DayViewProjection {
    return DayViewProjection(
        items = this,
        notes = notes,
        journalEntries = journalEntries
    )
}

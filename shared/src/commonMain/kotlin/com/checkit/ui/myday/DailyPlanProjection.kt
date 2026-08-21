package com.checkit.ui.myday

import com.checkit.domain.DailyPlanItem
import com.checkit.domain.JournalEntry
import com.checkit.domain.NoteItem

data class MyDayTaskViewProjection(
    val items: List<DailyPlanItem>,
    val notes: List<NoteItem>,
    val journalEntries: List<JournalEntry>,
)

fun List<DailyPlanItem>.toTaskViewProjection(
    notes: List<NoteItem>,
    journalEntries: List<JournalEntry>
): MyDayTaskViewProjection {
    return MyDayTaskViewProjection(
        items = this,
        notes = notes,
        journalEntries = journalEntries
    )
}

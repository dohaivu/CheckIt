package com.checkit.data

import kotlinx.serialization.Serializable

/**
 * Full JSON backup of the local Room database plus user settings.
 * Stored as a single `.json` file in the app-local backups directory (Android only).
 */
@Serializable
data class CheckItBackup(
    val version: Int = BACKUP_VERSION,
    val exportedAtMillis: Long = 0L,
    val settings: UserSettings = UserSettings(),
    val lists: List<ListEntity> = emptyList(),
    val listSections: List<ListSectionEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val subTasks: List<SubTaskEntity> = emptyList(),
    val taskReminders: List<TaskReminderEntity> = emptyList(),
    val taskTags: List<TaskTagEntity> = emptyList(),
    val taskLists: List<TaskListEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val noteTags: List<NoteTagEntity> = emptyList(),
    val noteLists: List<NoteListEntity> = emptyList(),
    val dailyPlanItems: List<DailyPlanItemEntity> = emptyList(),
    val dailyPlanItemTags: List<DailyPlanItemTagEntity> = emptyList(),
    val journalEntries: List<JournalEntryEntity> = emptyList(),
    val journalEntryTags: List<JournalEntryTagEntity> = emptyList(),
    val taskFilters: List<TaskFilterEntity> = emptyList(),
    val periodGoals: List<PeriodGoalEntity> = emptyList(),
    val nestedDocuments: List<NestedDocumentEntity> = emptyList(),
    val nestedListItems: List<NestedListItemEntity> = emptyList(),
    val nestedItemTags: List<NestedItemTagEntity> = emptyList(),
    val routines: List<RoutineEntity> = emptyList(),
    val routineLogs: List<RoutineLogEntity> = emptyList(),
    val quickNotes: List<QuickNoteEntity> = emptyList(),
) {
    companion object {
        const val BACKUP_VERSION = 2
    }
}

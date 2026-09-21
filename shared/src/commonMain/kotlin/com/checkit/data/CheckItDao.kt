package com.checkit.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.RoomRawQuery
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayCloseCommitResult
import com.checkit.domain.Period
import com.checkit.domain.TaskReminderWriteInput
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface CheckItDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPlanItem(item: DailyPlanItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: TaskReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListSection(section: ListSectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskList(taskList: TaskListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteList(noteList: NoteListEntity)

    @Query("DELETE FROM task_list WHERE taskId = :taskId")
    suspend fun deleteTaskList(taskId: String)

    @Query("DELETE FROM note_list WHERE noteId = :noteId")
    suspend fun deleteNoteList(noteId: String)

    @Query("SELECT * FROM task_list")
    fun observeTaskLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM note_list")
    fun observeNoteLists(): Flow<List<NoteListEntity>>

    @Query("SELECT * FROM task_list WHERE taskId = :taskId LIMIT 1")
    suspend fun taskListByTaskId(taskId: String): TaskListEntity?

    @Query("SELECT * FROM note_list WHERE noteId = :noteId LIMIT 1")
    suspend fun noteListByNoteId(noteId: String): NoteListEntity?

    @Query("SELECT * FROM lists WHERE id = :listId LIMIT 1")
    suspend fun listById(listId: String): ListEntity?

    @Query("SELECT * FROM list_sections WHERE id = :sectionId LIMIT 1")
    suspend fun sectionById(sectionId: String): ListSectionEntity?

    @Query("SELECT * FROM lists WHERE deleted = 0 ORDER BY sortOrder ASC, title ASC")
    fun observeLists(): Flow<List<ListEntity>>

    @Query("SELECT * FROM list_sections WHERE deleted = 0 ORDER BY listId ASC, sortOrder ASC, title ASC")
    fun observeListSections(): Flow<List<ListSectionEntity>>

    @Query("SELECT * FROM list_sections WHERE deleted = 0 AND listId = :listId ORDER BY sortOrder ASC, title ASC")
    fun observeSectionsForList(listId: String): Flow<List<ListSectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity)

    @Query(
        """
        INSERT OR IGNORE INTO journal_entry_tags(entryId, tagId)
        SELECT :entryId, :tagId
        WHERE EXISTS(SELECT 1 FROM journal_entries WHERE id = :entryId)
          AND EXISTS(SELECT 1 FROM tags WHERE id = :tagId)
        """
    )
    suspend fun insertJournalEntryTagIfParentsExist(entryId: String, tagId: String)

    @Query("DELETE FROM journal_entry_tags WHERE entryId = :entryId")
    suspend fun deleteJournalEntryTags(entryId: String)

    @Query("SELECT COUNT(*) FROM journal_entries WHERE deleted = 0 AND dateEpochDays = :dateEpochDays")
    suspend fun journalEntryCountForDate(dateEpochDays: Int): Int

    @Query("SELECT * FROM journal_entries WHERE deleted = 0 ORDER BY createdTimeMinutes ASC")
    fun observeJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE deleted = 0 AND dateEpochDays = :dateEpochDays ORDER BY createdTimeMinutes ASC")
    fun observeJournalEntriesForDate(dateEpochDays: Int): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entry_tags")
    fun observeJournalEntryTags(): Flow<List<JournalEntryTagEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :entryId LIMIT 1")
    suspend fun journalEntryById(entryId: String): JournalEntryEntity?

    @Query(
        """
        UPDATE journal_entries
        SET label = :label,
            content = :content,
            moods = :moods,
            attachments = :attachments,
            updatedAtMillis = :updatedAtMillis,
            dirty = 1
        WHERE id = :entryId
        """
    )
    suspend fun updateJournalEntry(
        entryId: String,
        label: String?,
        content: String,
        moods: String,
        attachments: String,
        updatedAtMillis: Long
    )

    /**
     * Tombstone instead of hard delete so the deletion syncs; the row is
     * hard-deleted later via [getPurgeableJournalTombstones] once uploaded.
     */
    @Query("UPDATE journal_entries SET deleted = 1, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :entryId")
    suspend fun deleteJournalEntry(entryId: String, updatedAtMillis: Long)

    @Query("DELETE FROM task_tags WHERE taskId = :taskId")
    suspend fun deleteTaskTags(taskId: String)

    @Query("DELETE FROM sub_tasks WHERE taskId = :taskId")
    suspend fun deleteSubTasks(taskId: String)

    @Query("DELETE FROM task_reminders WHERE taskId = :taskId")
    suspend fun deleteTaskReminders(taskId: String)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteNoteTags(noteId: String)

    @Query("DELETE FROM daily_plan_item_tags WHERE itemId = :itemId")
    suspend fun deleteDailyPlanItemTags(itemId: String)

    @Query("SELECT * FROM tags WHERE deleted = 0 ORDER BY sortOrder ASC, lastUsedAtMillis DESC, name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM task_filters ORDER BY sortOrder ASC, name ASC")
    fun observeFilters(): Flow<List<TaskFilterEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'Open' AND deleted = 0 ORDER BY createdAtMillis DESC")
    fun observeTasksOpen(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY createdAtMillis DESC")
    fun observeTasksAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun taskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun noteById(noteId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE status = 'Open' AND deleted = 0 ORDER BY editedAtMillis DESC")
    fun observeNotesOpen(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deleted = 0 ORDER BY editedAtMillis DESC")
    fun observeNotesAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM daily_plan_items WHERE deleted = 0 ORDER BY sortOrder ASC, addedAtMillis ASC")
    fun observeDailyPlanItems(): Flow<List<DailyPlanItemEntity>>

    @Query("SELECT * FROM daily_plan_items WHERE deleted = 0 AND dateEpochDays BETWEEN :startEpochDays AND :endEpochDays ORDER BY sortOrder ASC, addedAtMillis ASC")
    fun observeDailyPlanItemsInRange(startEpochDays: Int, endEpochDays: Int): Flow<List<DailyPlanItemEntity>>

    @Query("SELECT * FROM daily_plan_items WHERE id = :itemId LIMIT 1")
    suspend fun dailyPlanItemById(itemId: String): DailyPlanItemEntity?

    @Query("SELECT * FROM daily_plan_items WHERE deleted = 0 AND dateEpochDays = :dateEpochDays ORDER BY sortOrder ASC, addedAtMillis ASC")
    suspend fun dailyPlanItemsForDate(dateEpochDays: Int): List<DailyPlanItemEntity>

    @Query("SELECT COUNT(*) FROM daily_plan_items WHERE deleted = 0 AND dateEpochDays = :dateEpochDays AND carriedFromItemId = :sourceItemId")
    suspend fun carriedFromCountOnDate(dateEpochDays: Int, sourceItemId: String): Int

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND doDateEpochDays = :dateEpochDays AND trashedAtMillis IS NULL AND status != 'Completed' ORDER BY createdAtMillis DESC")
    fun observeTasksForDate(dateEpochDays: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND doDateEpochDays BETWEEN :startEpochDays AND :endEpochDays AND trashedAtMillis IS NULL AND status != 'Completed' ORDER BY createdAtMillis DESC")
    fun observeTasksForDateRange(startEpochDays: Int, endEpochDays: Int): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE deleted = 0 AND trashedAtMillis IS NULL 
          AND (doDateEpochDays = :dateEpochDays OR status = 'Open' OR completedDateEpochDays = :dateEpochDays)
        ORDER BY createdAtMillis DESC
    """)
    fun observeWorkingTasks(dateEpochDays: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM notes WHERE deleted = 0 AND dateEpochDays = :dateEpochDays AND trashedAtMillis IS NULL AND status != 'Completed' ORDER BY editedAtMillis DESC")
    fun observeNotesForDate(dateEpochDays: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deleted = 0 AND dateEpochDays BETWEEN :startEpochDays AND :endEpochDays AND trashedAtMillis IS NULL AND status != 'Completed' ORDER BY editedAtMillis DESC")
    fun observeNotesForDateRange(startEpochDays: Int, endEpochDays: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM sub_tasks ORDER BY sortOrder ASC, id ASC")
    fun observeSubTasks(): Flow<List<SubTaskEntity>>

    @Query("SELECT * FROM task_reminders ORDER BY remindAtMillis ASC")
    fun observeReminders(): Flow<List<TaskReminderEntity>>

    @Query("SELECT * FROM task_tags")
    fun observeTaskTags(): Flow<List<TaskTagEntity>>

    @Query(
        """
        SELECT tagId, COUNT(*) AS usageCount FROM (
            SELECT tt.tagId AS tagId
            FROM task_tags tt INNER JOIN tasks t ON t.id = tt.taskId
            WHERE t.trashedAtMillis IS NULL AND t.deleted = 0
            UNION ALL
            SELECT nt.tagId AS tagId
            FROM note_tags nt INNER JOIN notes n ON n.id = nt.noteId
            WHERE n.trashedAtMillis IS NULL AND n.deleted = 0
            UNION ALL
            SELECT pt.tagId AS tagId
            FROM daily_plan_item_tags pt INNER JOIN daily_plan_items i ON i.id = pt.itemId
            WHERE i.deleted = 0
            UNION ALL
            SELECT jt.tagId AS tagId
            FROM journal_entry_tags jt INNER JOIN journal_entries j ON j.id = jt.entryId
            WHERE j.deleted = 0
        )
        GROUP BY tagId
        """
    )
    fun observeTagUsageCounts(): Flow<List<TagUsageCountEntity>>

    @Query("SELECT * FROM note_tags")
    fun observeNoteTags(): Flow<List<NoteTagEntity>>

    @Query("SELECT * FROM daily_plan_item_tags")
    fun observeDailyPlanItemTags(): Flow<List<DailyPlanItemTagEntity>>

    @Query("SELECT isPinned FROM task_list WHERE taskId = :taskId AND listId = :listId LIMIT 1")
    suspend fun taskIsPinnedInList(taskId: String, listId: String): Boolean

    @Query("SELECT isPinned FROM note_list WHERE noteId = :noteId AND listId = :listId LIMIT 1")
    suspend fun noteIsPinnedInList(noteId: String, listId: String): Boolean

    @Query("SELECT COALESCE(MAX(tl.sortOrder), -1) + 1 FROM task_list tl WHERE tl.listId = :listId")
    suspend fun nextTaskSortOrder(listId: String): Int

    @Query("SELECT COALESCE(MAX(nl.sortOrder), -1) + 1 FROM note_list nl WHERE nl.listId = :listId")
    suspend fun nextNoteSortOrder(listId: String): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM daily_plan_items WHERE dateEpochDays = :dateEpochDays")
    suspend fun nextDailyPlanItemSortOrder(dateEpochDays: Int): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM lists")
    suspend fun nextListSortOrder(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tags")
    suspend fun nextTagSortOrder(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM list_sections WHERE listId = :listId")
    suspend fun nextSectionSortOrder(listId: String): Int

    @Query("UPDATE tags SET sortOrder = :sortOrder, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :tagId")
    suspend fun updateTagSortOrder(tagId: String, sortOrder: Int, updatedAtMillis: Long)

    @Query("UPDATE tags SET lastUsedAtMillis = :lastUsedAtMillis WHERE id = :tagId")
    suspend fun updateTagLastUsedAtMillis(tagId: String, lastUsedAtMillis: Long)

    @Query("SELECT COUNT(*) FROM daily_plan_items WHERE deleted = 0 AND taskId = :taskId AND dateEpochDays = :dateEpochDays AND status = 'Done' AND id != :excludeItemId")
    suspend fun countDoneDailyPlanItemsForTaskOnDate(taskId: String, dateEpochDays: Int, excludeItemId: String): Int

    @Query("SELECT tagId FROM daily_plan_item_tags WHERE itemId = :itemId")
    suspend fun tagIdsForItem(itemId: String): List<String>

    @Query("SELECT tagId FROM task_tags WHERE taskId = :taskId")
    suspend fun tagIdsForTask(taskId: String): List<String>

    @Query("SELECT tagId FROM note_tags WHERE noteId = :noteId")
    suspend fun tagIdsForNote(noteId: String): List<String>

    @Query("SELECT * FROM tags WHERE id IN (:tagIds)")
    suspend fun tagsByIds(tagIds: List<String>): List<TagEntity>

    @Query("SELECT * FROM sub_tasks WHERE taskId = :taskId ORDER BY sortOrder ASC")
    suspend fun subTasksForTask(taskId: String): List<SubTaskEntity>

    @Query("SELECT * FROM task_reminders WHERE taskId = :taskId ORDER BY remindAtMillis ASC")
    suspend fun remindersForTask(taskId: String): List<TaskReminderEntity>

    @Query("SELECT id FROM lists WHERE title = 'Inbox' ORDER BY sortOrder ASC, id ASC LIMIT 1")
    suspend fun inboxListId(): String?

    @Query("UPDATE lists SET title = :title, icon = :icon, color = :color, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :listId")
    suspend fun updateList(listId: String, title: String, icon: String, color: String, updatedAtMillis: Long)

    /**
     * Tombstone instead of hard delete so the deletion syncs; the row is
     * hard-deleted later via [getPurgeableListTombstones] once uploaded.
     */
    @Query("UPDATE lists SET deleted = 1, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :listId")
    suspend fun deleteList(listId: String, updatedAtMillis: Long)

    @Query("UPDATE list_sections SET title = :title, color = :color, sortOrder = :sortOrder, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :sectionId")
    suspend fun updateSection(sectionId: String, title: String, color: String, sortOrder: Int, updatedAtMillis: Long)

    /**
     * Tombstone instead of hard delete so the deletion syncs; the row is
     * hard-deleted later via [getPurgeableListSectionTombstones] once uploaded.
     */
    @Query("UPDATE list_sections SET deleted = 1, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :sectionId")
    suspend fun deleteSection(sectionId: String, updatedAtMillis: Long)

    @Query("UPDATE task_list SET listId = :toListId WHERE listId = :fromListId")
    suspend fun moveTasksToList(fromListId: String, toListId: String)

    @Query("UPDATE note_list SET listId = :toListId WHERE listId = :fromListId")
    suspend fun moveNotesToList(fromListId: String, toListId: String)

    @Query("UPDATE tasks SET dirty = 1, updatedAtMillis = :nowMillis WHERE id IN (SELECT taskId FROM task_list WHERE listId = :listId)")
    suspend fun markTasksInListDirty(listId: String, nowMillis: Long)

    @Query("UPDATE notes SET dirty = 1, editedAtMillis = :nowMillis WHERE id IN (SELECT noteId FROM note_list WHERE listId = :listId)")
    suspend fun markNotesInListDirty(listId: String, nowMillis: Long)

    @Transaction
    suspend fun deleteListMovingContents(listId: String, targetListId: String, nowMillis: Long) {
        // Membership syncs embedded in the task/note documents, so the moved
        // rows must be marked dirty before the joins are rewritten.
        markTasksInListDirty(listId, nowMillis)
        markNotesInListDirty(listId, nowMillis)
        moveTasksToList(fromListId = listId, toListId = targetListId)
        moveNotesToList(fromListId = listId, toListId = targetListId)
        deleteList(listId, nowMillis)
    }

    @Query(
        """
        INSERT OR IGNORE INTO task_tags(taskId, tagId)
        SELECT :taskId, :tagId
        WHERE EXISTS(SELECT 1 FROM tasks WHERE id = :taskId)
          AND EXISTS(SELECT 1 FROM tags WHERE id = :tagId)
        """
    )
    suspend fun insertTaskTagIfParentsExist(taskId: String, tagId: String)

    @Query(
        """
        INSERT OR IGNORE INTO note_tags(noteId, tagId)
        SELECT :noteId, :tagId
        WHERE EXISTS(SELECT 1 FROM notes WHERE id = :noteId)
          AND EXISTS(SELECT 1 FROM tags WHERE id = :tagId)
        """
    )
    suspend fun insertNoteTagIfParentsExist(noteId: String, tagId: String)

    @Query(
        """
        INSERT OR IGNORE INTO daily_plan_item_tags(itemId, tagId)
        SELECT :itemId, :tagId
        WHERE EXISTS(SELECT 1 FROM daily_plan_items WHERE id = :itemId)
          AND EXISTS(SELECT 1 FROM tags WHERE id = :tagId)
        """
    )
    suspend fun insertDailyPlanItemTagIfParentsExist(itemId: String, tagId: String)

    @Query("UPDATE tags SET name = :name, color = :color, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :tagId")
    suspend fun updateTag(tagId: String, name: String, color: String, updatedAtMillis: Long)

    /**
     * Tombstone instead of hard delete so the deletion syncs; the row is
     * hard-deleted later via [getPurgeableTagTombstones] once uploaded.
     */
    @Query("UPDATE tags SET deleted = 1, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :tagId")
    suspend fun deleteTag(tagId: String, updatedAtMillis: Long)

    @Query("SELECT COUNT(*) FROM tags WHERE name = :name AND id != :excludeId")
    suspend fun tagNameInUseExcept(name: String, excludeId: String): Int

    @Query(
        """
        UPDATE tasks
        SET name = :name,
            description = :description,
            status = :status,
            priority = :priority,
            type = :type,
            doDateEpochDays = :doDateEpochDays,
            startTimeMinutes = :startTimeMinutes,
            endTimeMinutes = :endTimeMinutes,
            repeatRRule = :repeatRRule,
            label = :label,
            updatedAtMillis = :updatedAtMillis,
            dirty = 1
        WHERE id = :taskId
        """
    )
    suspend fun updateTask(
        taskId: String,
        name: String,
        description: String,
        status: String,
        priority: String,
        type: String,
        doDateEpochDays: Int?,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        repeatRRule: String?,
        label: String?,
        updatedAtMillis: Long
    )

    @Query("UPDATE tasks SET trashedAtMillis = :trashedAtMillis, updatedAtMillis = :trashedAtMillis, dirty = 1 WHERE id = :taskId")
    suspend fun trashTask(taskId: String, trashedAtMillis: Long)

    @Query("UPDATE tasks SET trashedAtMillis = NULL, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :taskId")
    suspend fun restoreTask(taskId: String, updatedAtMillis: Long)

    @Transaction
    suspend fun replaceTaskSubTasks(taskId: String, subtasks: List<SubTaskWriteInput>) {
        deleteSubTasks(taskId)
        subtasks.forEachIndexed { index, subtask ->
            insertSubTask(
                SubTaskEntity(
                    id = Uuid.random().toString(),
                    taskId = taskId,
                    name = subtask.name,
                    isCompleted = subtask.isCompleted,
                    sortOrder = index
                )
            )
        }
    }

    @Transaction
    suspend fun replaceTaskReminders(taskId: String, reminders: List<TaskReminderWriteInput>) {
        deleteTaskReminders(taskId)
        reminders.forEach { reminder ->
            insertReminder(
                TaskReminderEntity(
                    id = Uuid.random().toString(),
                    taskId = taskId,
                    remindAtMillis = reminder.remindAtMillis,
                    label = reminder.label
                )
            )
        }
    }

    @Query(
        """
        UPDATE tasks
        SET status = :status,
            completedDateEpochDays = :completedDateEpochDays,
            updatedAtMillis = :updatedAtMillis,
            dirty = 1
        WHERE id = :taskId
        """
    )
    suspend fun completeTask(
        taskId: String,
        status: String,
        completedDateEpochDays: Int,
        updatedAtMillis: Long
    )

    @Query(
        """
        UPDATE tasks
        SET status = :status,
            completedDateEpochDays = CASE WHEN :status != 'Completed' THEN NULL ELSE completedDateEpochDays END,
            updatedAtMillis = :updatedAtMillis,
            dirty = 1
        WHERE id = :taskId
        """
    )
    suspend fun updateTaskStatus(
        taskId: String,
        status: String,
        updatedAtMillis: Long
    )

    @Query(
        """
        UPDATE daily_plan_items
        SET deleted = 1,
            updatedAtMillis = :nowMillis,
            dirty = 1
        WHERE taskId = :taskId
          AND status = 'Planned'
          AND deleted = 0
        """
    )
    suspend fun deletePlannedDailyPlanItemsForTask(taskId: String, nowMillis: Long)

    @Query(
        """
        UPDATE daily_plan_items
        SET startTimeMinutes = :startTimeMinutes,
            endTimeMinutes = :endTimeMinutes,
            updatedAtMillis = :nowMillis,
            dirty = 1
        WHERE id = :itemId
        """
    )
    suspend fun updateDailyPlanItemTime(
        itemId: String,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        nowMillis: Long
    )

    @Transaction
    suspend fun updateDailyPlanItemTimes(updates: List<DailyPlanItemTimeUpdate>, nowMillis: Long) {
        updates.forEach { update ->
            updateDailyPlanItemTime(
                itemId = update.itemId,
                startTimeMinutes = update.startTimeMinutes,
                endTimeMinutes = update.endTimeMinutes,
                nowMillis = nowMillis
            )
        }
    }

    @Query(
        """
        UPDATE daily_plan_items
        SET status = :status,
            completedAtMillis = :completedAtMillis,
            updatedAtMillis = :nowMillis,
            dirty = 1
        WHERE id = :itemId
        """
    )
    suspend fun updateDailyPlanItemStatus(
        itemId: String,
        status: String,
        completedAtMillis: Long?,
        nowMillis: Long
    )

    @Query(
        """
        UPDATE daily_plan_items
        SET status = :status,
            completedAtMillis = :completedAtMillis,
            updatedAtMillis = :nowMillis,
            dirty = 1
        WHERE id IN (:itemIds)
        """
    )
    suspend fun updateDailyPlanItemsStatus(
        itemIds: List<String>,
        status: String,
        completedAtMillis: Long?,
        nowMillis: Long
    )

    @Query("UPDATE daily_plan_items SET handledAtMillis = :handledAtMillis, updatedAtMillis = :handledAtMillis, dirty = 1 WHERE id IN (:itemIds)")
    suspend fun markDailyPlanItemsHandled(itemIds: List<String>, handledAtMillis: Long)

    @Query("SELECT * FROM period_goals WHERE deleted = 0 ORDER BY startEpochDays ASC")
    fun observePeriodGoals(): Flow<List<PeriodGoalEntity>>

    @Query(
        """
        SELECT * FROM period_goals
        WHERE deleted = 0
          AND (:startEpochDays IS NULL OR startEpochDays >= :startEpochDays)
          AND (:endEpochDays IS NULL OR startEpochDays <= :endEpochDays)
        ORDER BY startEpochDays ASC
        """
    )
    fun observePeriodGoalsBetween(startEpochDays: Int?, endEpochDays: Int?): Flow<List<PeriodGoalEntity>>

    /**
     * One page of the unlimited newest-first history of one period type before
     * [beforeEpochDays] (the current focus start is excluded by the caller).
     * No blank filtering: rows are shown with any data. Backs the Paging 3
     * history source built in the repository.
     */
    @Query(
        """
        SELECT * FROM period_goals
        WHERE deleted = 0 AND periodType = :periodType AND startEpochDays < :beforeEpochDays
        ORDER BY startEpochDays DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun goalHistoryPage(
        periodType: String,
        beforeEpochDays: Int,
        limit: Int,
        offset: Int
    ): List<PeriodGoalEntity>

    /** Tracked minutes summed from daily rollups over a goal's date range. */
    @Query(
        """
        SELECT COALESCE(SUM(doneMinutes), 0) FROM daily_reflect_stats
        WHERE dateEpochDays >= :startEpochDays AND dateEpochDays < :endEpochDays
        """
    )
    suspend fun sumDoneMinutesBetween(startEpochDays: Int, endEpochDays: Int): Int

    // ---------------- Reflect rollups (precomputed daily aggregates) ----------------

    @Query(
        "SELECT * FROM daily_reflect_stats WHERE dateEpochDays BETWEEN :startEpochDays AND :endEpochDays ORDER BY dateEpochDays ASC"
    )
    fun observeDailyReflectStats(startEpochDays: Int, endEpochDays: Int): Flow<List<DailyReflectStatsEntity>>

    @Query(
        """
        SELECT r.dateEpochDays AS dateEpochDays,
               r.tagId AS tagId,
               t.name AS tagName,
               t.color AS tagColor,
               r.doneCount AS doneCount,
               r.doneMinutes AS doneMinutes
        FROM daily_tag_rollups AS r
        JOIN tags AS t ON t.id = r.tagId
        WHERE r.dateEpochDays BETWEEN :startEpochDays AND :endEpochDays
        ORDER BY r.dateEpochDays ASC
        """
    )
    fun observeDailyTagRollups(startEpochDays: Int, endEpochDays: Int): Flow<List<DailyTagRollupWithMeta>>

    @Query(
        "SELECT * FROM habit_daily_rollups WHERE dateEpochDays BETWEEN :startEpochDays AND :endEpochDays ORDER BY dateEpochDays ASC"
    )
    fun observeHabitDailyRollups(startEpochDays: Int, endEpochDays: Int): Flow<List<HabitDailyRollupEntity>>

    @Query("DELETE FROM daily_reflect_stats")
    suspend fun clearDailyReflectStats()

    @Query("DELETE FROM daily_tag_rollups")
    suspend fun clearDailyTagRollups()

    @Query("DELETE FROM habit_daily_rollups")
    suspend fun clearHabitDailyRollups()

    @Query(
        """
        INSERT OR REPLACE INTO daily_reflect_stats(dateEpochDays, plannedItemCount, doneItemCount, doneMinutes, journalCount, computedAtMillis)
        SELECT i.dateEpochDays,
               SUM(CASE WHEN i.status = 'Planned' AND i.source IN ('MyDayTask', 'MyDayReminder', 'ExistingTask') THEN 1 ELSE 0 END),
               SUM(CASE WHEN i.status = 'Done' AND i.source IN ('MyDayTask', 'MyDayReminder', 'ExistingTask') THEN 1 ELSE 0 END),
               SUM(
                   CASE
                       WHEN i.status = 'Done' AND i.startTimeMinutes IS NOT NULL AND i.endTimeMinutes IS NOT NULL
                       THEN MAX(i.endTimeMinutes - i.startTimeMinutes, 0)
                       ELSE 0
                   END
               ),
               0,
               :computedAtMillis
        FROM daily_plan_items AS i
        WHERE i.deleted = 0
        GROUP BY i.dateEpochDays
        """
    )
    suspend fun insertDailyReflectStatsFromItems(computedAtMillis: Long)

    @Query(
        """
        INSERT OR IGNORE INTO daily_reflect_stats(dateEpochDays, plannedItemCount, doneItemCount, doneMinutes, journalCount, computedAtMillis)
        SELECT j.dateEpochDays, 0, 0, 0, 0, :computedAtMillis
        FROM journal_entries AS j
        WHERE j.deleted = 0
        GROUP BY j.dateEpochDays
        """
    )
    suspend fun insertDailyReflectStatsForJournalOnlyDays(computedAtMillis: Long)

    @Query(
        """
        UPDATE daily_reflect_stats
        SET journalCount = (
            SELECT COUNT(*) FROM journal_entries AS j WHERE j.dateEpochDays = daily_reflect_stats.dateEpochDays
        )
        """
    )
    suspend fun updateDailyReflectStatsJournalCounts()

    @Query(
        """
        INSERT OR REPLACE INTO daily_tag_rollups(dateEpochDays, tagId, doneCount, doneMinutes)
        SELECT i.dateEpochDays,
               it.tagId,
               COUNT(*),
               SUM(
                   CASE
                       WHEN i.startTimeMinutes IS NOT NULL AND i.endTimeMinutes IS NOT NULL
                       THEN MAX(i.endTimeMinutes - i.startTimeMinutes, 0)
                       ELSE 0
                   END
               )
        FROM daily_plan_items AS i
        JOIN daily_plan_item_tags AS it ON it.itemId = i.id
        WHERE i.status = 'Done' AND i.deleted = 0
        GROUP BY i.dateEpochDays, it.tagId
        """
    )
    suspend fun insertDailyTagRollups()

    @Query(
        """
        INSERT OR REPLACE INTO habit_daily_rollups(dateEpochDays, habitKey, title, doneMinutes)
        SELECT i.dateEpochDays,
               CASE WHEN i.taskId IS NOT NULL THEN 'task:' || i.taskId ELSE 'title:' || LOWER(TRIM(i.title)) END,
               MAX(TRIM(i.title)),
               SUM(
                   CASE
                       WHEN i.startTimeMinutes IS NOT NULL AND i.endTimeMinutes IS NOT NULL
                       THEN MAX(i.endTimeMinutes - i.startTimeMinutes, 0)
                       ELSE 0
                   END
               )
        FROM daily_plan_items AS i
        WHERE i.isHabit = 1 AND i.status = 'Done' AND i.deleted = 0
        GROUP BY i.dateEpochDays,
                 CASE WHEN i.taskId IS NOT NULL THEN 'task:' || i.taskId ELSE 'title:' || LOWER(TRIM(i.title)) END
        """
    )
    suspend fun insertHabitDailyRollups()

    /**
     * Rebuilds all Reflect rollups from source tables in one set-based pass.
     * Cheap enough to run as a whole; called once a day by [com.checkit.domain.usecase.RebuildReflectStatsUseCase].
     */
    @Transaction
    suspend fun rebuildReflectStats(computedAtMillis: Long) {
        clearDailyReflectStats()
        clearDailyTagRollups()
        clearHabitDailyRollups()
        insertDailyReflectStatsFromItems(computedAtMillis)
        insertDailyReflectStatsForJournalOnlyDays(computedAtMillis)
        updateDailyReflectStatsJournalCounts()
        insertDailyTagRollups()
        insertHabitDailyRollups()
    }

    /** Slim projection of done items for highlights; avoids hydrating tags/labels. */
    @Query(
        """
        SELECT id, dateEpochDays, title, note, source, startTimeMinutes, endTimeMinutes, completedAtMillis
        FROM daily_plan_items
        WHERE deleted = 0 AND status = 'Done' AND dateEpochDays BETWEEN :startEpochDays AND :endEpochDays
        ORDER BY completedAtMillis DESC
        """
    )
    fun observeDoneItemSummaries(startEpochDays: Int, endEpochDays: Int): Flow<List<DoneItemSummaryEntity>>

    @Query(
        "SELECT * FROM journal_entries WHERE deleted = 0 AND dateEpochDays BETWEEN :startEpochDays AND :endEpochDays ORDER BY createdTimeMinutes ASC"
    )
    fun observeJournalEntriesInRange(startEpochDays: Int, endEpochDays: Int): Flow<List<JournalEntryEntity>>

    @RawQuery(observedEntities = [JournalEntryEntity::class, JournalEntryTagEntity::class])
    fun observeJournalEntriesFiltered(query: RoomRawQuery): Flow<List<JournalEntryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM journal_entries WHERE deleted = 0 AND dateEpochDays < :epochDays)")
    fun observeJournalEntryExistsBefore(epochDays: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM period_goals WHERE deleted = 0 AND periodType = 'Day' AND startEpochDays < :epochDays)")
    fun observeDayGoalExistsBefore(epochDays: Int): Flow<Boolean>


    @Query(
        "SELECT * FROM period_goals WHERE periodType = :periodType AND startEpochDays = :startEpochDays LIMIT 1"
    )
    suspend fun periodGoalFor(periodType: String, startEpochDays: Int): PeriodGoalEntity?

    @Query(
        "SELECT * FROM period_goals WHERE deleted = 0 AND periodType = :periodType AND startEpochDays >= :startEpochDays AND startEpochDays < :endEpochDays ORDER BY startEpochDays ASC"
    )
    fun observePeriodGoalsInRange(
        periodType: String,
        startEpochDays: Int,
        endEpochDays: Int
    ): Flow<List<PeriodGoalEntity>>

    /**
     * Insert-or-update without delete: keeps the existing primary key on
     * conflicts so a goal's inline metrics survive. Callers must resolve
     * [PeriodGoalEntity.id] for existing rows first — the fallback update
     * matches by primary key only. The entity carries its own
     * [PeriodGoalEntity.updatedAtMillis]/dirty/deleted sync fields.
     */
    @Upsert
    suspend fun upsertPeriodGoal(goal: PeriodGoalEntity)

    /**
     * Applies a complete evening review atomically: marks done items, carries
     * leftovers onto the next day, writes tomorrow's goal note, and records the
     * review. Idempotent: already-carried items are skipped and every resolved
     * item is stamped as handled.
     */
    @Transaction
    suspend fun completeDayClose(
        dateEpochDays: Int,
        markDoneItemIds: List<String>,
        carryItemIds: List<String>,
        dropItemIds: List<String>,
        winNote: String?,
        tomorrowGoal: String?,
        rating: Float = 0f,
        doneCount: Int,
        plannedCount: Int,
        doneMinutes: Int,
        targetDateEpochDays: Int,
        nowMillis: Long
    ): DayCloseCommitResult {
        markDoneItemIds.forEach { itemId ->
            val source = dailyPlanItemById(itemId) ?: return@forEach
            if (source.nestedListItemId != null) {
                val start = source.startTimeMinutes
                val end = source.endTimeMinutes
                if (start != null && end != null) {
                    val minutes = (end - start).coerceAtLeast(0)
                    if (minutes > 0) {
                        updateNestedItemActualMinutesDelta(source.nestedListItemId, minutes, nowMillis)
                    }
                }
            }
        }
        updateDailyPlanItemsStatus(markDoneItemIds, DailyPlanItemStatus.Done.name, nowMillis, nowMillis)
        markDailyPlanItemsHandled(markDoneItemIds, nowMillis)
        deleteDailyPlanItems(dropItemIds, nowMillis)

        var carriedCount = 0
        var skippedCount = 0
        carryItemIds.forEach { itemId ->
            val source = dailyPlanItemById(itemId) ?: return@forEach
            val alreadyCarried = carriedFromCountOnDate(targetDateEpochDays, source.id) > 0
            if (!alreadyCarried) {
                val newItemId = Uuid.random().toString()
                insertDailyPlanItem(
                    DailyPlanItemEntity(
                        id = newItemId,
                        dateEpochDays = targetDateEpochDays,
                        taskId = source.taskId,
                        nestedListItemId = source.nestedListItemId,
                        title = source.title,
                        note = source.note,
                        source = source.source,
                        status = DailyPlanItemStatus.Planned.name,
                        sortOrder = nextDailyPlanItemSortOrder(targetDateEpochDays),
                        label = source.label,
                        startTimeMinutes = null,
                        endTimeMinutes = null,
                        isHabit = source.isHabit,
                        addedAtMillis = nowMillis,
                        completedAtMillis = null,
                        carriedFromItemId = source.id,
                        updatedAtMillis = nowMillis
                    )
                )
                tagIdsForItem(source.id).forEach { tagId ->
                    insertDailyPlanItemTagIfParentsExist(newItemId, tagId)
                }
                carriedCount += 1
            } else {
                skippedCount += 1
            }
            markDailyPlanItemsHandled(listOf(source.id), nowMillis)
        }

        // Merge the win note and rating into any existing record so its goal
        // text and metrics are preserved.
        val existingToday = periodGoalFor(Period.Day.name, dateEpochDays)
        upsertPeriodGoal(
            (existingToday ?: PeriodGoalEntity(
                id = Uuid.random().toString(),
                periodType = Period.Day.name,
                startEpochDays = dateEpochDays,
                endEpochDays = dateEpochDays + 1,
                updatedAtMillis = nowMillis
            )).copy(
                review = winNote?.trim().orEmpty(),
                rating = rating.coerceIn(0f, 5f),
                completedAtMillis = nowMillis,
                editedAtMillis = nowMillis,
                updatedAtMillis = nowMillis,
                dirty = true,
                deleted = false
            )
        )

        // The tomorrow goal describes the next day's period, so it is stored
        // as the next day's goal instead of on this record.
        tomorrowGoal?.trim()?.takeIf { it.isNotEmpty() }?.let { goal ->
            val nextStartEpochDays = dateEpochDays + 1
            val existing = periodGoalFor(Period.Day.name, nextStartEpochDays)
            upsertPeriodGoal(
                (existing ?: PeriodGoalEntity(
                    id = Uuid.random().toString(),
                    periodType = Period.Day.name,
                    startEpochDays = nextStartEpochDays,
                    endEpochDays = nextStartEpochDays + 1,
                    updatedAtMillis = nowMillis
                )).copy(
                    goal = goal,
                    editedAtMillis = nowMillis,
                    updatedAtMillis = nowMillis,
                    dirty = true,
                    deleted = false
                )
            )
        }

        return DayCloseCommitResult(
            carriedCount = carriedCount,
            skippedCount = skippedCount
        )
    }

    @Transaction
    suspend fun updateDailyPlanItemWithTags(
        itemId: String,
        dateEpochDays: Int,
        title: String,
        note: String?,
        source: String,
        status: String,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        completedAtMillis: Long?,
        label: String?,
        nestedListItemId: String?,
        tagIds: List<String>,
        nowMillis: Long
    ) {
        val oldEntity = dailyPlanItemById(itemId) ?: return
        
        updateDailyPlanItem(
            itemId, dateEpochDays, title, note, source, status, 
            startTimeMinutes, endTimeMinutes, completedAtMillis, label, nestedListItemId, nowMillis
        )

        if (oldEntity.nestedListItemId != null) {
            val oldStatus = enumValueOf<DailyPlanItemStatus>(oldEntity.status)
            val oldMinutes = if (oldStatus == DailyPlanItemStatus.Done) {
                (oldEntity.endTimeMinutes ?: 0) - (oldEntity.startTimeMinutes ?: 0)
            } else 0
            val newMinutes = if (status == DailyPlanItemStatus.Done.name) {
                (endTimeMinutes ?: 0) - (startTimeMinutes ?: 0)
            } else 0
            val delta = newMinutes.coerceAtLeast(0) - oldMinutes.coerceAtLeast(0)
            if (delta != 0) {
                updateNestedItemActualMinutesDelta(oldEntity.nestedListItemId, delta, nowMillis)
            }
        }

        deleteDailyPlanItemTags(itemId)
        tagIds.distinct().forEach { tagId ->
            insertDailyPlanItemTagIfParentsExist(itemId, tagId)
            updateTagLastUsedAtMillis(tagId, nowMillis)
        }
    }

    @Transaction
    suspend fun updateDailyPlanItemStatusWithMinutes(
        itemId: String,
        status: String,
        completedAtMillis: Long?,
        nowMillis: Long
    ) {
        val oldEntity = dailyPlanItemById(itemId) ?: return
        if (oldEntity.status == status) return

        updateDailyPlanItemStatus(itemId, status, completedAtMillis, nowMillis)

        if (oldEntity.nestedListItemId != null) {
            val start = oldEntity.startTimeMinutes
            val end = oldEntity.endTimeMinutes
            if (start != null && end != null) {
                val minutes = (end - start).coerceAtLeast(0)
                if (minutes > 0) {
                    val delta = if (status == DailyPlanItemStatus.Done.name) minutes else -minutes
                    updateNestedItemActualMinutesDelta(oldEntity.nestedListItemId, delta, nowMillis)
                }
            }
        }
    }

    @Query(
        """
        UPDATE daily_plan_items
        SET dateEpochDays = :dateEpochDays,
            title = :title,
            note = :note,
            source = :source,
            status = :status,
            startTimeMinutes = :startTimeMinutes,
            endTimeMinutes = :endTimeMinutes,
            completedAtMillis = :completedAtMillis,
            label = :label,
            nestedListItemId = :nestedListItemId,
            updatedAtMillis = :nowMillis,
            dirty = 1
        WHERE id = :itemId
        """
    )
    suspend fun updateDailyPlanItem(
        itemId: String,
        dateEpochDays: Int,
        title: String,
        note: String?,
        source: String,
        status: String,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        completedAtMillis: Long?,
        label: String?,
        nestedListItemId: String?,
        nowMillis: Long
    )

    /**
     * Tombstone instead of hard delete so the deletion syncs; the row is
     * hard-deleted later via [getPurgeableDailyPlanItemTombstones] once uploaded.
     */
    @Query("UPDATE daily_plan_items SET deleted = 1, updatedAtMillis = :nowMillis, dirty = 1 WHERE id = :itemId")
    suspend fun deleteDailyPlanItem(itemId: String, nowMillis: Long)

    @Query("UPDATE daily_plan_items SET deleted = 1, updatedAtMillis = :nowMillis, dirty = 1 WHERE id IN (:itemIds)")
    suspend fun deleteDailyPlanItems(itemIds: List<String>, nowMillis: Long)

    @Query(
        """
        UPDATE daily_plan_items
        SET taskId = :taskId,
            source = :source,
            updatedAtMillis = :nowMillis,
            dirty = 1
        WHERE id = :itemId
        """
    )
    suspend fun linkDailyPlanItemToTask(itemId: String, taskId: String, source: String, nowMillis: Long)

    @Query(
        """
        UPDATE tasks
        SET startTimeMinutes = NULL,
            endTimeMinutes = NULL,
            updatedAtMillis = :updatedAtMillis,
            dirty = 1
        WHERE id = :taskId
        """
    )
    suspend fun clearTaskTime(taskId: String, updatedAtMillis: Long)

    @Query("UPDATE notes SET title = :title, content = :content, status = :status, dateEpochDays = :dateEpochDays, startTimeMinutes = :startTimeMinutes, label = :label, editedAtMillis = :editedAtMillis, dirty = 1 WHERE id = :noteId")
    suspend fun updateNote(
        noteId: String,
        title: String,
        content: String,
        status: String,
        dateEpochDays: Int?,
        startTimeMinutes: Int?,
        label: String?,
        editedAtMillis: Long
    )

    @Query("UPDATE notes SET status = :status, editedAtMillis = :editedAtMillis, dirty = 1 WHERE id = :noteId")
    suspend fun updateNoteStatus(noteId: String, status: String, editedAtMillis: Long)

    @Query("UPDATE notes SET trashedAtMillis = :trashedAtMillis, editedAtMillis = :trashedAtMillis, dirty = 1 WHERE id = :noteId")
    suspend fun trashNote(noteId: String, trashedAtMillis: Long)

    @Query("UPDATE notes SET trashedAtMillis = NULL, editedAtMillis = :editedAtMillis, dirty = 1 WHERE id = :noteId")
    suspend fun restoreNote(noteId: String, editedAtMillis: Long)

    // ---------------- Nested Documents ----------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNestedDocument(document: NestedDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNestedListItem(item: NestedListItemEntity)

    @Transaction
    suspend fun insertNestedDocumentWithRoot(
        document: NestedDocumentEntity,
        rootItem: NestedListItemEntity
    ): String {
        insertNestedDocument(document)
        insertNestedListItem(rootItem.copy(documentId = document.id))
        return document.id
    }

    @Query("SELECT * FROM nested_documents WHERE deleted = 0 ORDER BY updatedAtMillis DESC, id ASC")
    fun observeNestedDocuments(): Flow<List<NestedDocumentEntity>>

    @Query("SELECT * FROM nested_list_items WHERE deleted = 0 AND documentId = :documentId ORDER BY position ASC, id ASC")
    fun observeNestedItems(documentId: String): Flow<List<NestedListItemEntity>>

    @Query("SELECT * FROM nested_item_tags WHERE itemId IN (SELECT id FROM nested_list_items WHERE documentId = :documentId)")
    fun observeNestedItemTags(documentId: String): Flow<List<NestedItemTagEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM nested_list_items WHERE documentId = :documentId AND parentId IS :parentId")
    suspend fun nextNestedItemPosition(documentId: String, parentId: String?): Int

    @Query("UPDATE nested_documents SET title = :title, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :documentId")
    suspend fun updateNestedDocumentTitle(documentId: String, title: String, updatedAtMillis: Long)

    /**
     * Tombstones the document and all of its items so deletions sync; rows
     * are hard-deleted later via the purge queries once uploaded.
     */
    @Transaction
    suspend fun deleteNestedDocument(documentId: String, nowMillis: Long) {
        markNestedDocumentDeleted(documentId, nowMillis)
        markNestedItemsDeletedForDocument(documentId, nowMillis)
    }

    @Query("UPDATE nested_documents SET deleted = 1, updatedAtMillis = :nowMillis, dirty = 1 WHERE id = :documentId")
    suspend fun markNestedDocumentDeleted(documentId: String, nowMillis: Long)

    @Query("UPDATE nested_list_items SET deleted = 1, updatedAtMillis = :nowMillis, dirty = 1 WHERE documentId = :documentId AND deleted = 0")
    suspend fun markNestedItemsDeletedForDocument(documentId: String, nowMillis: Long)

    @Query("UPDATE nested_list_items SET text = :text, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun updateNestedItemText(itemId: String, text: String, updatedAtMillis: Long)

    @Query("UPDATE nested_list_items SET note = :note, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun updateNestedItemNote(itemId: String, note: String?, updatedAtMillis: Long)

    @Query("UPDATE nested_list_items SET textStyle = :textStyle, textColor = :textColor, backgroundColor = :backgroundColor, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun updateNestedItemFormatting(
        itemId: String,
        textStyle: String,
        textColor: String,
        backgroundColor: String,
        updatedAtMillis: Long
    )

    @Query("UPDATE nested_list_items SET priority = :priority, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun updateNestedItemPriority(
        itemId: String,
        priority: String,
        updatedAtMillis: Long
    )

    @Query("UPDATE nested_list_items SET startDateEpochDays = :startDateEpochDays, endDateEpochDays = :endDateEpochDays, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun updateNestedItemDateRange(
        itemId: String,
        startDateEpochDays: Int?,
        endDateEpochDays: Int?,
        updatedAtMillis: Long
    )

    @Query("UPDATE nested_list_items SET actualMinutes = :actualMinutes, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun updateNestedItemActualMinutes(itemId: String, actualMinutes: Int, updatedAtMillis: Long)

    @Query("UPDATE nested_list_items SET metricRollupPolicy = :policy, showTrackedMinutes = :showTrackedMinutes, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun updateNestedItemMetricSettings(
        itemId: String,
        policy: String,
        showTrackedMinutes: Boolean,
        updatedAtMillis: Long
    )

    @Query("UPDATE nested_list_items SET progressPercent = :progressPercent, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun updateNestedItemProgress(
        itemId: String,
        progressPercent: Int?,
        updatedAtMillis: Long
    )

    @Query("UPDATE nested_list_items SET checkboxEnabled = :checkboxEnabled, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun setNestedItemCheckboxEnabled(itemId: String, checkboxEnabled: Boolean, updatedAtMillis: Long)

    @Query("UPDATE nested_list_items SET checked = :checked, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id IN (:itemIds)")
    suspend fun setNestedItemsChecked(itemIds: List<String>, checked: Boolean, updatedAtMillis: Long)

    @Query("UPDATE nested_list_items SET collapsed = :collapsed, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun setNestedItemCollapsed(itemId: String, collapsed: Boolean, updatedAtMillis: Long)

    @Query("UPDATE nested_list_items SET collapsed = NOT collapsed, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun toggleNestedItemCollapsed(itemId: String, updatedAtMillis: Long)

    @Query("UPDATE nested_list_items SET parentId = :parentId, position = :position, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun updateNestedItemPosition(
        itemId: String,
        parentId: String?,
        position: Int,
        updatedAtMillis: Long
    )

    /**
     * Tombstones instead of hard delete so deletions sync; rows are
     * hard-deleted later via [getPurgeableNestedItemTombstones] once uploaded.
     */
    @Query("UPDATE nested_list_items SET deleted = 1, updatedAtMillis = :nowMillis, dirty = 1 WHERE id IN (:itemIds)")
    suspend fun deleteNestedItems(itemIds: List<String>, nowMillis: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNestedItemTag(link: NestedItemTagEntity)

    @Query("DELETE FROM nested_item_tags WHERE itemId = :itemId")
    suspend fun deleteNestedItemTags(itemId: String)

    @Transaction
    suspend fun replaceNestedItemTags(itemId: String, tagIds: List<String>) {
        deleteNestedItemTags(itemId)
        tagIds.distinct().forEach { tagId -> insertNestedItemTag(NestedItemTagEntity(itemId, tagId)) }
    }

    @Query(
        "UPDATE nested_list_items SET manualMetricsJson = :metricsJson, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId"
    )
    suspend fun updateNestedItemManualMetrics(itemId: String, metricsJson: String, updatedAtMillis: Long)

    @Query("UPDATE nested_list_items SET actualMinutes = actualMinutes + :delta, updatedAtMillis = :updatedAtMillis, dirty = 1 WHERE id = :itemId")
    suspend fun updateNestedItemActualMinutesDelta(itemId: String, delta: Int, updatedAtMillis: Long)

    @Transaction
    suspend fun applyNestedMoves(moves: List<NestedMoveRow>) {
        moves.forEach { move ->
            updateNestedItemPosition(
                itemId = move.itemId,
                parentId = move.parentId,
                position = move.position,
                updatedAtMillis = move.updatedAtMillis
            )
        }
    }

    @Query("UPDATE tasks SET dirty = 1, updatedAtMillis = :nowMillis WHERE id = :taskId")
    suspend fun markTaskDirty(taskId: String, nowMillis: Long)

    @Query("UPDATE notes SET dirty = 1, editedAtMillis = :nowMillis WHERE id = :noteId")
    suspend fun markNoteDirty(noteId: String, nowMillis: Long)

    @Query("UPDATE daily_plan_items SET dirty = 1, updatedAtMillis = :nowMillis WHERE id = :itemId")
    suspend fun markDailyPlanItemDirty(itemId: String, nowMillis: Long)

    @Query("UPDATE nested_list_items SET dirty = 1, updatedAtMillis = :nowMillis WHERE id = :itemId")
    suspend fun markNestedItemDirty(itemId: String, nowMillis: Long)

    @Query("SELECT * FROM nested_documents WHERE id = :documentId LIMIT 1")
    suspend fun nestedDocumentById(documentId: String): NestedDocumentEntity?

    @Query("SELECT * FROM nested_list_items WHERE id = :itemId LIMIT 1")
    suspend fun nestedItemById(itemId: String): NestedListItemEntity?

    @Query("SELECT * FROM nested_item_tags WHERE itemId IN (:itemIds)")
    suspend fun nestedItemTagsForItems(itemIds: List<String>): List<NestedItemTagEntity>

    @Query("SELECT * FROM nested_list_items WHERE documentId = :documentId AND dirty = 1 ORDER BY updatedAtMillis ASC")
    suspend fun getDirtyNestedItemsForDocument(documentId: String): List<NestedListItemEntity>

    @Query("SELECT id FROM nested_list_items WHERE documentId = :documentId AND deleted = 1 AND dirty = 0 AND updatedAtMillis <= :cutoff ORDER BY updatedAtMillis ASC")
    suspend fun getPurgeableNestedItemIdsForDocument(documentId: String, cutoff: Long): List<String>

    // ---------------- Sync (dirty tracking, tombstones, purge) ----------------
    //
    // One query group per top-level sync table. Children and membership joins
    // (sub-tasks, reminders, tag/list links) sync embedded in the parent
    // document and need no sync columns of their own.
    //
    // Lifecycle per row: local writes set dirty = 1; upload clears it with
    // markClean* (guarded by the read watermark so mid-push edits stay dirty);
    // deletes write tombstones (deleted = 1, dirty = 1); purge hard-deletes
    // tombstones only after they uploaded (dirty = 0) and aged past the
    // retention cutoff so every device had a chance to sync them.

    @Query("SELECT * FROM tasks WHERE dirty = 1 ORDER BY updatedAtMillis ASC")
    suspend fun getDirtyTasks(): List<TaskEntity>

    @Query("UPDATE tasks SET dirty = 0 WHERE id IN (:ids) AND updatedAtMillis <= :maxUpdatedAt")
    suspend fun markTasksClean(ids: List<String>, maxUpdatedAt: Long)

    @Query("SELECT * FROM tasks WHERE deleted = 1 AND dirty = 0 AND updatedAtMillis <= :cutoff ORDER BY updatedAtMillis ASC")
    suspend fun getPurgeableTaskTombstones(cutoff: Long): List<TaskEntity>

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun hardDeleteTasks(ids: List<String>)

    @Query("SELECT * FROM notes WHERE dirty = 1 ORDER BY editedAtMillis ASC")
    suspend fun getDirtyNotes(): List<NoteEntity>

    @Query("UPDATE notes SET dirty = 0 WHERE id IN (:ids) AND editedAtMillis <= :maxUpdatedAt")
    suspend fun markNotesClean(ids: List<String>, maxUpdatedAt: Long)

    @Query("SELECT * FROM notes WHERE deleted = 1 AND dirty = 0 AND editedAtMillis <= :cutoff ORDER BY editedAtMillis ASC")
    suspend fun getPurgeableNoteTombstones(cutoff: Long): List<NoteEntity>

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun hardDeleteNotes(ids: List<String>)

    @Query("SELECT * FROM tags WHERE dirty = 1 ORDER BY updatedAtMillis ASC")
    suspend fun getDirtyTags(): List<TagEntity>

    @Query("UPDATE tags SET dirty = 0 WHERE id IN (:ids) AND updatedAtMillis <= :maxUpdatedAt")
    suspend fun markTagsClean(ids: List<String>, maxUpdatedAt: Long)

    @Query("SELECT * FROM tags WHERE deleted = 1 AND dirty = 0 AND updatedAtMillis <= :cutoff ORDER BY updatedAtMillis ASC")
    suspend fun getPurgeableTagTombstones(cutoff: Long): List<TagEntity>

    @Query("DELETE FROM tags WHERE id IN (:ids)")
    suspend fun hardDeleteTags(ids: List<String>)

    @Query("SELECT * FROM lists WHERE dirty = 1 ORDER BY updatedAtMillis ASC")
    suspend fun getDirtyLists(): List<ListEntity>

    @Query("UPDATE lists SET dirty = 0 WHERE id IN (:ids) AND updatedAtMillis <= :maxUpdatedAt")
    suspend fun markListsClean(ids: List<String>, maxUpdatedAt: Long)

    @Query("SELECT * FROM lists WHERE deleted = 1 AND dirty = 0 AND updatedAtMillis <= :cutoff ORDER BY updatedAtMillis ASC")
    suspend fun getPurgeableListTombstones(cutoff: Long): List<ListEntity>

    @Query("DELETE FROM lists WHERE id IN (:ids)")
    suspend fun hardDeleteLists(ids: List<String>)

    @Query("SELECT * FROM list_sections WHERE dirty = 1 ORDER BY updatedAtMillis ASC")
    suspend fun getDirtyListSections(): List<ListSectionEntity>

    @Query("UPDATE list_sections SET dirty = 0 WHERE id IN (:ids) AND updatedAtMillis <= :maxUpdatedAt")
    suspend fun markListSectionsClean(ids: List<String>, maxUpdatedAt: Long)

    @Query("SELECT * FROM list_sections WHERE deleted = 1 AND dirty = 0 AND updatedAtMillis <= :cutoff ORDER BY updatedAtMillis ASC")
    suspend fun getPurgeableListSectionTombstones(cutoff: Long): List<ListSectionEntity>

    @Query("DELETE FROM list_sections WHERE id IN (:ids)")
    suspend fun hardDeleteListSections(ids: List<String>)

    @Query("SELECT * FROM journal_entries WHERE dirty = 1 ORDER BY updatedAtMillis ASC")
    suspend fun getDirtyJournalEntries(): List<JournalEntryEntity>

    @Query("UPDATE journal_entries SET dirty = 0 WHERE id IN (:ids) AND updatedAtMillis <= :maxUpdatedAt")
    suspend fun markJournalEntriesClean(ids: List<String>, maxUpdatedAt: Long)

    @Query("SELECT * FROM journal_entries WHERE deleted = 1 AND dirty = 0 AND updatedAtMillis <= :cutoff ORDER BY updatedAtMillis ASC")
    suspend fun getPurgeableJournalTombstones(cutoff: Long): List<JournalEntryEntity>

    @Query("DELETE FROM journal_entries WHERE id IN (:ids)")
    suspend fun hardDeleteJournalEntries(ids: List<String>)

    @Query("SELECT * FROM daily_plan_items WHERE dirty = 1 ORDER BY updatedAtMillis ASC")
    suspend fun getDirtyDailyPlanItems(): List<DailyPlanItemEntity>

    @Query("UPDATE daily_plan_items SET dirty = 0 WHERE id IN (:ids) AND updatedAtMillis <= :maxUpdatedAt")
    suspend fun markDailyPlanItemsClean(ids: List<String>, maxUpdatedAt: Long)

    @Query("SELECT * FROM daily_plan_items WHERE deleted = 1 AND dirty = 0 AND updatedAtMillis <= :cutoff ORDER BY updatedAtMillis ASC")
    suspend fun getPurgeableDailyPlanItemTombstones(cutoff: Long): List<DailyPlanItemEntity>

    @Query("DELETE FROM daily_plan_items WHERE id IN (:ids)")
    suspend fun hardDeleteDailyPlanItems(ids: List<String>)

    @Query("SELECT * FROM period_goals WHERE dirty = 1 ORDER BY updatedAtMillis ASC")
    suspend fun getDirtyPeriodGoals(): List<PeriodGoalEntity>

    @Query("UPDATE period_goals SET dirty = 0 WHERE id IN (:ids) AND updatedAtMillis <= :maxUpdatedAt")
    suspend fun markPeriodGoalsClean(ids: List<String>, maxUpdatedAt: Long)

    @Query("SELECT * FROM period_goals WHERE deleted = 1 AND dirty = 0 AND updatedAtMillis <= :cutoff ORDER BY updatedAtMillis ASC")
    suspend fun getPurgeablePeriodGoalTombstones(cutoff: Long): List<PeriodGoalEntity>

    @Query("DELETE FROM period_goals WHERE id IN (:ids)")
    suspend fun hardDeletePeriodGoals(ids: List<String>)

    @Query("SELECT * FROM nested_documents WHERE dirty = 1 ORDER BY updatedAtMillis ASC")
    suspend fun getDirtyNestedDocuments(): List<NestedDocumentEntity>

    @Query("UPDATE nested_documents SET dirty = 0 WHERE id IN (:ids) AND updatedAtMillis <= :maxUpdatedAt")
    suspend fun markNestedDocumentsClean(ids: List<String>, maxUpdatedAt: Long)

    @Query("SELECT * FROM nested_documents WHERE deleted = 1 AND dirty = 0 AND updatedAtMillis <= :cutoff ORDER BY updatedAtMillis ASC")
    suspend fun getPurgeableNestedDocumentTombstones(cutoff: Long): List<NestedDocumentEntity>

    @Query("DELETE FROM nested_documents WHERE id IN (:ids)")
    suspend fun hardDeleteNestedDocuments(ids: List<String>)

    @Query("SELECT * FROM nested_list_items WHERE dirty = 1 ORDER BY updatedAtMillis ASC")
    suspend fun getDirtyNestedItems(): List<NestedListItemEntity>

    @Query("UPDATE nested_list_items SET dirty = 0 WHERE id IN (:ids) AND updatedAtMillis <= :maxUpdatedAt")
    suspend fun markNestedItemsClean(ids: List<String>, maxUpdatedAt: Long)

    @Query("SELECT * FROM nested_list_items WHERE deleted = 1 AND dirty = 0 AND updatedAtMillis <= :cutoff ORDER BY updatedAtMillis ASC")
    suspend fun getPurgeableNestedItemTombstones(cutoff: Long): List<NestedListItemEntity>

    @Query("DELETE FROM nested_list_items WHERE id IN (:ids)")
    suspend fun hardDeleteNestedItems(ids: List<String>)

    // ---------------- JSON backup / restore ----------------

    @Query("SELECT * FROM lists")
    suspend fun getAllListsOnce(): List<ListEntity>

    @Query("SELECT * FROM list_sections")
    suspend fun getAllListSectionsOnce(): List<ListSectionEntity>

    @Query("SELECT * FROM tags")
    suspend fun getAllTagsOnce(): List<TagEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM sub_tasks")
    suspend fun getAllSubTasksOnce(): List<SubTaskEntity>

    @Query("SELECT * FROM task_reminders")
    suspend fun getAllTaskRemindersOnce(): List<TaskReminderEntity>

    @Query("SELECT * FROM task_tags")
    suspend fun getAllTaskTagsOnce(): List<TaskTagEntity>

    @Query("SELECT * FROM task_list")
    suspend fun getAllTaskListsOnce(): List<TaskListEntity>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesOnce(): List<NoteEntity>

    @Query("SELECT * FROM note_tags")
    suspend fun getAllNoteTagsOnce(): List<NoteTagEntity>

    @Query("SELECT * FROM note_list")
    suspend fun getAllNoteListsOnce(): List<NoteListEntity>

    @Query("SELECT * FROM daily_plan_items")
    suspend fun getAllDailyPlanItemsOnce(): List<DailyPlanItemEntity>

    @Query("SELECT * FROM daily_plan_item_tags")
    suspend fun getAllDailyPlanItemTagsOnce(): List<DailyPlanItemTagEntity>

    @Query("SELECT * FROM journal_entries")
    suspend fun getAllJournalEntriesOnce(): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entry_tags")
    suspend fun getAllJournalEntryTagsOnce(): List<JournalEntryTagEntity>

    @Query("SELECT * FROM task_filters")
    suspend fun getAllTaskFiltersOnce(): List<TaskFilterEntity>

    @Query("SELECT * FROM period_goals")
    suspend fun getAllPeriodGoalsOnce(): List<PeriodGoalEntity>

    @Query("SELECT * FROM nested_documents")
    suspend fun getAllNestedDocumentsOnce(): List<NestedDocumentEntity>

    @Query("SELECT * FROM nested_list_items")
    suspend fun getAllNestedListItemsOnce(): List<NestedListItemEntity>

    @Query("SELECT * FROM nested_item_tags")
    suspend fun getAllNestedItemTagsOnce(): List<NestedItemTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTags(rows: List<TaskTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTags(rows: List<NoteTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPlanItemTags(rows: List<DailyPlanItemTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntryTags(rows: List<JournalEntryTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskFilters(rows: List<TaskFilterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriodGoals(rows: List<PeriodGoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNestedItemTags(rows: List<NestedItemTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLists(rows: List<ListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListSections(rows: List<ListSectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(rows: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(rows: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTasks(rows: List<SubTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskReminders(rows: List<TaskReminderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(rows: List<NoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPlanItems(rows: List<DailyPlanItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntries(rows: List<JournalEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNestedDocuments(rows: List<NestedDocumentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNestedListItems(rows: List<NestedListItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskLists(rows: List<TaskListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteLists(rows: List<NoteListEntity>)

    @Query("DELETE FROM nested_item_tags")
    suspend fun clearNestedItemTags()

    @Query("DELETE FROM daily_plan_item_tags")
    suspend fun clearDailyPlanItemTags()

    @Query("DELETE FROM note_tags")
    suspend fun clearNoteTags()

    @Query("DELETE FROM task_tags")
    suspend fun clearTaskTags()

    @Query("DELETE FROM journal_entry_tags")
    suspend fun clearJournalEntryTags()

    @Query("DELETE FROM task_list")
    suspend fun clearTaskLists()

    @Query("DELETE FROM note_list")
    suspend fun clearNoteLists()

    @Query("DELETE FROM task_reminders")
    suspend fun clearTaskReminders()

    @Query("DELETE FROM sub_tasks")
    suspend fun clearSubTasks()

    @Query("DELETE FROM daily_plan_items")
    suspend fun clearDailyPlanItems()

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM list_sections")
    suspend fun clearListSections()

    @Query("DELETE FROM task_filters")
    suspend fun clearTaskFilters()

    @Query("DELETE FROM journal_entries")
    suspend fun clearJournalEntries()

    @Query("DELETE FROM period_goals")
    suspend fun clearPeriodGoals()

    @Query("DELETE FROM nested_list_items")
    suspend fun clearNestedListItems()

    @Query("DELETE FROM nested_documents")
    suspend fun clearNestedDocuments()

    @Query("DELETE FROM lists")
    suspend fun clearLists()

    @Query("DELETE FROM tags")
    suspend fun clearTags()

    @Transaction
    suspend fun restoreBackup(backup: CheckItBackup) {
        clearNestedItemTags()
        clearDailyPlanItemTags()
        clearNoteTags()
        clearTaskTags()
        clearJournalEntryTags()
        clearTaskLists()
        clearNoteLists()
        clearTaskReminders()
        clearSubTasks()
        clearDailyPlanItems()
        clearNotes()
        clearTasks()
        clearListSections()
        clearTaskFilters()
        clearJournalEntries()
        clearPeriodGoals()
        clearNestedListItems()
        clearNestedDocuments()
        clearLists()
        clearTags()

        if (backup.lists.isNotEmpty()) insertLists(backup.lists)
        if (backup.tags.isNotEmpty()) insertTags(backup.tags)
        if (backup.listSections.isNotEmpty()) insertListSections(backup.listSections)
        if (backup.tasks.isNotEmpty()) insertTasks(backup.tasks)
        if (backup.notes.isNotEmpty()) insertNotes(backup.notes)
        if (backup.subTasks.isNotEmpty()) insertSubTasks(backup.subTasks)
        if (backup.taskReminders.isNotEmpty()) insertTaskReminders(backup.taskReminders)
        if (backup.taskFilters.isNotEmpty()) insertTaskFilters(backup.taskFilters)
        if (backup.journalEntries.isNotEmpty()) insertJournalEntries(backup.journalEntries)
        if (backup.periodGoals.isNotEmpty()) insertPeriodGoals(backup.periodGoals)
        if (backup.nestedDocuments.isNotEmpty()) insertNestedDocuments(backup.nestedDocuments)
        if (backup.nestedListItems.isNotEmpty()) insertNestedListItems(backup.nestedListItems)
        if (backup.dailyPlanItems.isNotEmpty()) insertDailyPlanItems(backup.dailyPlanItems)
        if (backup.taskTags.isNotEmpty()) insertTaskTags(backup.taskTags)
        if (backup.noteTags.isNotEmpty()) insertNoteTags(backup.noteTags)
        if (backup.dailyPlanItemTags.isNotEmpty()) insertDailyPlanItemTags(backup.dailyPlanItemTags)
        if (backup.journalEntryTags.isNotEmpty()) insertJournalEntryTags(backup.journalEntryTags)
        if (backup.taskLists.isNotEmpty()) insertTaskLists(backup.taskLists)
        if (backup.noteLists.isNotEmpty()) insertNoteLists(backup.noteLists)
        if (backup.nestedItemTags.isNotEmpty()) insertNestedItemTags(backup.nestedItemTags)
    }
}

data class NestedMoveRow(
    val itemId: String,
    val parentId: String?,
    val position: Int,
    val updatedAtMillis: Long
)

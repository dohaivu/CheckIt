package com.checkit.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.RoomRawQuery
import androidx.room3.Transaction
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayCloseCommitResult
import com.checkit.domain.Period
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
import com.checkit.domain.TaskReminderWriteInput
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckItDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPlanItem(item: DailyPlanItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: TaskReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ListEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListSection(section: ListSectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskList(taskList: TaskListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteList(noteList: NoteListEntity)

    @Query("DELETE FROM task_list WHERE taskId = :taskId")
    suspend fun deleteTaskList(taskId: Long)

    @Query("DELETE FROM note_list WHERE noteId = :noteId")
    suspend fun deleteNoteList(noteId: Long)

    @Query("SELECT * FROM task_list")
    fun observeTaskLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM note_list")
    fun observeNoteLists(): Flow<List<NoteListEntity>>

    @Query("SELECT * FROM task_list WHERE taskId = :taskId LIMIT 1")
    suspend fun taskListByTaskId(taskId: Long): TaskListEntity?

    @Query("SELECT * FROM note_list WHERE noteId = :noteId LIMIT 1")
    suspend fun noteListByNoteId(noteId: Long): NoteListEntity?

    @Query("SELECT * FROM lists WHERE id = :listId LIMIT 1")
    suspend fun listById(listId: Long): ListEntity?

    @Query("SELECT * FROM list_sections WHERE id = :sectionId LIMIT 1")
    suspend fun sectionById(sectionId: Long): ListSectionEntity?

    @Query("SELECT * FROM lists ORDER BY sortOrder ASC, title ASC")
    fun observeLists(): Flow<List<ListEntity>>

    @Query("SELECT * FROM list_sections ORDER BY listId ASC, sortOrder ASC, title ASC")
    fun observeListSections(): Flow<List<ListSectionEntity>>

    @Query("SELECT * FROM list_sections WHERE listId = :listId ORDER BY sortOrder ASC, title ASC")
    fun observeSectionsForList(listId: Long): Flow<List<ListSectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    @Query(
        """
        INSERT OR IGNORE INTO journal_entry_tags(entryId, tagId)
        SELECT :entryId, :tagId
        WHERE EXISTS(SELECT 1 FROM journal_entries WHERE id = :entryId)
          AND EXISTS(SELECT 1 FROM tags WHERE id = :tagId)
        """
    )
    suspend fun insertJournalEntryTagIfParentsExist(entryId: Long, tagId: Long)

    @Query("DELETE FROM journal_entry_tags WHERE entryId = :entryId")
    suspend fun deleteJournalEntryTags(entryId: Long)

    @Query("SELECT COUNT(*) FROM journal_entries WHERE dateEpochDays = :dateEpochDays")
    suspend fun journalEntryCountForDate(dateEpochDays: Int): Int

    @Query("SELECT * FROM journal_entries ORDER BY createdTimeMinutes ASC")
    fun observeJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE dateEpochDays = :dateEpochDays ORDER BY createdTimeMinutes ASC")
    fun observeJournalEntriesForDate(dateEpochDays: Int): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entry_tags")
    fun observeJournalEntryTags(): Flow<List<JournalEntryTagEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :entryId LIMIT 1")
    suspend fun journalEntryById(entryId: Long): JournalEntryEntity?

    @Query(
        """
        UPDATE journal_entries
        SET label = :label,
            content = :content,
            moods = :moods,
            attachments = :attachments
        WHERE id = :entryId
        """
    )
    suspend fun updateJournalEntry(entryId: Long, label: String?, content: String, moods: String, attachments: String)

    @Query("DELETE FROM journal_entries WHERE id = :entryId")
    suspend fun deleteJournalEntry(entryId: Long)

    @Query("DELETE FROM task_tags WHERE taskId = :taskId")
    suspend fun deleteTaskTags(taskId: Long)

    @Query("DELETE FROM sub_tasks WHERE taskId = :taskId")
    suspend fun deleteSubTasks(taskId: Long)

    @Query("DELETE FROM task_reminders WHERE taskId = :taskId")
    suspend fun deleteTaskReminders(taskId: Long)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteNoteTags(noteId: Long)

    @Query("DELETE FROM daily_plan_item_tags WHERE itemId = :itemId")
    suspend fun deleteDailyPlanItemTags(itemId: Long)

    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, lastUsedAtMillis DESC, name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM task_filters ORDER BY sortOrder ASC, name ASC")
    fun observeFilters(): Flow<List<TaskFilterEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'Open' ORDER BY createdAtMillis DESC")
    fun observeTasksOpen(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAtMillis DESC")
    fun observeTasksAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun taskById(taskId: Long): TaskEntity?

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun noteById(noteId: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE status = 'Open' ORDER BY editedAtMillis DESC")
    fun observeNotesOpen(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY editedAtMillis DESC")
    fun observeNotesAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM daily_plan_items ORDER BY sortOrder ASC, addedAtMillis ASC")
    fun observeDailyPlanItems(): Flow<List<DailyPlanItemEntity>>

    @Query("SELECT * FROM daily_plan_items WHERE dateEpochDays BETWEEN :startEpochDays AND :endEpochDays ORDER BY sortOrder ASC, addedAtMillis ASC")
    fun observeDailyPlanItemsInRange(startEpochDays: Int, endEpochDays: Int): Flow<List<DailyPlanItemEntity>>

    @Query("SELECT * FROM daily_plan_items WHERE id = :itemId LIMIT 1")
    suspend fun dailyPlanItemById(itemId: Long): DailyPlanItemEntity?

    @Query("SELECT * FROM daily_plan_items WHERE dateEpochDays = :dateEpochDays ORDER BY sortOrder ASC, addedAtMillis ASC")
    suspend fun dailyPlanItemsForDate(dateEpochDays: Int): List<DailyPlanItemEntity>

    @Query("SELECT COUNT(*) FROM daily_plan_items WHERE dateEpochDays = :dateEpochDays AND carriedFromItemId = :sourceItemId")
    suspend fun carriedFromCountOnDate(dateEpochDays: Int, sourceItemId: Long): Int

    @Query("SELECT * FROM tasks WHERE doDateEpochDays = :dateEpochDays AND trashedAtMillis IS NULL AND status != 'Completed' ORDER BY createdAtMillis DESC")
    fun observeTasksForDate(dateEpochDays: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE doDateEpochDays BETWEEN :startEpochDays AND :endEpochDays AND trashedAtMillis IS NULL AND status != 'Completed' ORDER BY createdAtMillis DESC")
    fun observeTasksForDateRange(startEpochDays: Int, endEpochDays: Int): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE trashedAtMillis IS NULL 
          AND (doDateEpochDays = :dateEpochDays OR status = 'Open' OR completedDateEpochDays = :dateEpochDays)
        ORDER BY createdAtMillis DESC
    """)
    fun observeWorkingTasks(dateEpochDays: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM notes WHERE dateEpochDays = :dateEpochDays AND trashedAtMillis IS NULL AND status != 'Completed' ORDER BY editedAtMillis DESC")
    fun observeNotesForDate(dateEpochDays: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE dateEpochDays BETWEEN :startEpochDays AND :endEpochDays AND trashedAtMillis IS NULL AND status != 'Completed' ORDER BY editedAtMillis DESC")
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
            WHERE t.trashedAtMillis IS NULL
            UNION ALL
            SELECT nt.tagId AS tagId
            FROM note_tags nt INNER JOIN notes n ON n.id = nt.noteId
            WHERE n.trashedAtMillis IS NULL
            UNION ALL
            SELECT pt.tagId AS tagId FROM daily_plan_item_tags pt
            UNION ALL
            SELECT jt.tagId AS tagId FROM journal_entry_tags jt
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
    suspend fun taskIsPinnedInList(taskId: Long, listId: Long): Boolean

    @Query("SELECT isPinned FROM note_list WHERE noteId = :noteId AND listId = :listId LIMIT 1")
    suspend fun noteIsPinnedInList(noteId: Long, listId: Long): Boolean

    @Query("SELECT COALESCE(MAX(tl.sortOrder), -1) + 1 FROM task_list tl WHERE tl.listId = :listId")
    suspend fun nextTaskSortOrder(listId: Long): Int

    @Query("SELECT COALESCE(MAX(nl.sortOrder), -1) + 1 FROM note_list nl WHERE nl.listId = :listId")
    suspend fun nextNoteSortOrder(listId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM daily_plan_items WHERE dateEpochDays = :dateEpochDays")
    suspend fun nextDailyPlanItemSortOrder(dateEpochDays: Int): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM lists")
    suspend fun nextListSortOrder(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tags")
    suspend fun nextTagSortOrder(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM list_sections WHERE listId = :listId")
    suspend fun nextSectionSortOrder(listId: Long): Int

    @Query("UPDATE tags SET sortOrder = :sortOrder WHERE id = :tagId")
    suspend fun updateTagSortOrder(tagId: Long, sortOrder: Int)

    @Query("UPDATE tags SET lastUsedAtMillis = :lastUsedAtMillis WHERE id = :tagId")
    suspend fun updateTagLastUsedAtMillis(tagId: Long, lastUsedAtMillis: Long)

    @Query("SELECT COUNT(*) FROM daily_plan_items WHERE taskId = :taskId AND dateEpochDays = :dateEpochDays AND status = 'Done' AND id != :excludeItemId")
    suspend fun countDoneDailyPlanItemsForTaskOnDate(taskId: Long, dateEpochDays: Int, excludeItemId: Long): Int

    @Query("SELECT tagId FROM daily_plan_item_tags WHERE itemId = :itemId")
    suspend fun tagIdsForItem(itemId: Long): List<Long>

    @Query("SELECT tagId FROM task_tags WHERE taskId = :taskId")
    suspend fun tagIdsForTask(taskId: Long): List<Long>

    @Query("SELECT tagId FROM note_tags WHERE noteId = :noteId")
    suspend fun tagIdsForNote(noteId: Long): List<Long>

    @Query("SELECT * FROM tags WHERE id IN (:tagIds)")
    suspend fun tagsByIds(tagIds: List<Long>): List<TagEntity>

    @Query("SELECT * FROM sub_tasks WHERE taskId = :taskId ORDER BY sortOrder ASC")
    suspend fun subTasksForTask(taskId: Long): List<SubTaskEntity>

    @Query("SELECT * FROM task_reminders WHERE taskId = :taskId ORDER BY remindAtMillis ASC")
    suspend fun remindersForTask(taskId: Long): List<TaskReminderEntity>

    @Query("SELECT id FROM lists WHERE title = 'Inbox' ORDER BY sortOrder ASC, id ASC LIMIT 1")
    suspend fun inboxListId(): Long?

    @Query("UPDATE lists SET title = :title, icon = :icon, color = :color WHERE id = :listId")
    suspend fun updateList(listId: Long, title: String, icon: String, color: String)

    @Query("DELETE FROM lists WHERE id = :listId")
    suspend fun deleteList(listId: Long)

    @Query("DELETE FROM list_sections WHERE id = :sectionId")
    suspend fun deleteSection(sectionId: Long)

    @Query("UPDATE list_sections SET title = :title, color = :color, sortOrder = :sortOrder WHERE id = :sectionId")
    suspend fun updateSection(sectionId: Long, title: String, color: String, sortOrder: Int)

    @Query("UPDATE task_list SET listId = :toListId WHERE listId = :fromListId")
    suspend fun moveTasksToList(fromListId: Long, toListId: Long)

    @Query("UPDATE note_list SET listId = :toListId WHERE listId = :fromListId")
    suspend fun moveNotesToList(fromListId: Long, toListId: Long)

    @Transaction
    suspend fun deleteListMovingContents(listId: Long, targetListId: Long) {
        moveTasksToList(fromListId = listId, toListId = targetListId)
        moveNotesToList(fromListId = listId, toListId = targetListId)
        deleteList(listId)
    }

    @Query(
        """
        INSERT OR IGNORE INTO task_tags(taskId, tagId)
        SELECT :taskId, :tagId
        WHERE EXISTS(SELECT 1 FROM tasks WHERE id = :taskId)
          AND EXISTS(SELECT 1 FROM tags WHERE id = :tagId)
        """
    )
    suspend fun insertTaskTagIfParentsExist(taskId: Long, tagId: Long)

    @Query(
        """
        INSERT OR IGNORE INTO note_tags(noteId, tagId)
        SELECT :noteId, :tagId
        WHERE EXISTS(SELECT 1 FROM notes WHERE id = :noteId)
          AND EXISTS(SELECT 1 FROM tags WHERE id = :tagId)
        """
    )
    suspend fun insertNoteTagIfParentsExist(noteId: Long, tagId: Long)

    @Query(
        """
        INSERT OR IGNORE INTO daily_plan_item_tags(itemId, tagId)
        SELECT :itemId, :tagId
        WHERE EXISTS(SELECT 1 FROM daily_plan_items WHERE id = :itemId)
          AND EXISTS(SELECT 1 FROM tags WHERE id = :tagId)
        """
    )
    suspend fun insertDailyPlanItemTagIfParentsExist(itemId: Long, tagId: Long)

    @Query("UPDATE tags SET name = :name, color = :color WHERE id = :tagId")
    suspend fun updateTag(tagId: Long, name: String, color: String)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)

    @Query("SELECT COUNT(*) FROM tags WHERE name = :name AND id != :excludeId")
    suspend fun tagNameInUseExcept(name: String, excludeId: Long): Int

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
            updatedAtMillis = :updatedAtMillis
        WHERE id = :taskId
        """
    )
    suspend fun updateTask(
        taskId: Long,
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

    @Query("UPDATE tasks SET trashedAtMillis = :trashedAtMillis, updatedAtMillis = :trashedAtMillis WHERE id = :taskId")
    suspend fun trashTask(taskId: Long, trashedAtMillis: Long)

    @Query("UPDATE tasks SET trashedAtMillis = NULL, updatedAtMillis = :updatedAtMillis WHERE id = :taskId")
    suspend fun restoreTask(taskId: Long, updatedAtMillis: Long)

    @Transaction
    suspend fun replaceTaskSubTasks(taskId: Long, subtasks: List<SubTaskWriteInput>) {
        deleteSubTasks(taskId)
        subtasks.forEachIndexed { index, subtask ->
            insertSubTask(
                SubTaskEntity(
                    taskId = taskId,
                    name = subtask.name,
                    isCompleted = subtask.isCompleted,
                    sortOrder = index
                )
            )
        }
    }

    @Transaction
    suspend fun replaceTaskReminders(taskId: Long, reminders: List<TaskReminderWriteInput>) {
        deleteTaskReminders(taskId)
        reminders.forEach { reminder ->
            insertReminder(
                TaskReminderEntity(
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
            updatedAtMillis = :updatedAtMillis
        WHERE id = :taskId
        """
    )
    suspend fun completeTask(
        taskId: Long,
        status: String,
        completedDateEpochDays: Int,
        updatedAtMillis: Long
    )

    @Query(
        """
        UPDATE tasks
        SET status = :status,
            completedDateEpochDays = CASE WHEN :status != 'Completed' THEN NULL ELSE completedDateEpochDays END,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :taskId
        """
    )
    suspend fun updateTaskStatus(
        taskId: Long,
        status: String,
        updatedAtMillis: Long
    )

    @Query(
        """
        UPDATE daily_plan_items
        SET status = 'Done',
            completedAtMillis = :completedAtMillis
        WHERE taskId = :taskId
          AND status = 'Planned'
        """
    )
    suspend fun completePlannedDailyPlanItemsForTask(
        taskId: Long,
        completedAtMillis: Long
    )

    @Query(
        """
        DELETE FROM daily_plan_items
        WHERE taskId = :taskId
          AND status = 'Planned'
        """
    )
    suspend fun deletePlannedDailyPlanItemsForTask(taskId: Long)

    @Query(
        """
        UPDATE daily_plan_items
        SET startTimeMinutes = :startTimeMinutes,
            endTimeMinutes = :endTimeMinutes
        WHERE id = :itemId
        """
    )
    suspend fun updateDailyPlanItemTime(
        itemId: Long,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?
    )

    @Transaction
    suspend fun updateDailyPlanItemTimes(updates: List<DailyPlanItemTimeUpdate>) {
        updates.forEach { update ->
            updateDailyPlanItemTime(
                itemId = update.itemId,
                startTimeMinutes = update.startTimeMinutes,
                endTimeMinutes = update.endTimeMinutes
            )
        }
    }

    @Query(
        """
        UPDATE daily_plan_items
        SET status = :status,
            completedAtMillis = :completedAtMillis
        WHERE id = :itemId
        """
    )
    suspend fun updateDailyPlanItemStatus(
        itemId: Long,
        status: String,
        completedAtMillis: Long?
    )

    @Query(
        """
        UPDATE daily_plan_items
        SET status = :status,
            completedAtMillis = :completedAtMillis
        WHERE id IN (:itemIds)
        """
    )
    suspend fun updateDailyPlanItemsStatus(
        itemIds: List<Long>,
        status: String,
        completedAtMillis: Long?
    )

    @Query("UPDATE daily_plan_items SET handledAtMillis = :handledAtMillis WHERE id IN (:itemIds)")
    suspend fun markDailyPlanItemsHandled(itemIds: List<Long>, handledAtMillis: Long)

    @Query("SELECT * FROM period_reviews ORDER BY periodStartEpochDays ASC")
    fun observePeriodReviews(): Flow<List<PeriodReviewEntity>>

    @Query(
        """
        SELECT * FROM period_reviews
        WHERE (:startEpochDays IS NULL OR periodStartEpochDays >= :startEpochDays)
          AND (:endEpochDays IS NULL OR periodStartEpochDays <= :endEpochDays)
        ORDER BY periodStartEpochDays ASC
        """
    )
    fun observePeriodReviewsBetween(startEpochDays: Int?, endEpochDays: Int?): Flow<List<PeriodReviewEntity>>

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
        GROUP BY i.dateEpochDays
        """
    )
    suspend fun insertDailyReflectStatsFromItems(computedAtMillis: Long)

    @Query(
        """
        INSERT OR IGNORE INTO daily_reflect_stats(dateEpochDays, plannedItemCount, doneItemCount, doneMinutes, journalCount, computedAtMillis)
        SELECT j.dateEpochDays, 0, 0, 0, 0, :computedAtMillis
        FROM journal_entries AS j
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
        WHERE i.status = 'Done'
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
        WHERE i.isHabit = 1 AND i.status = 'Done'
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
        WHERE status = 'Done' AND dateEpochDays BETWEEN :startEpochDays AND :endEpochDays
        ORDER BY completedAtMillis DESC
        """
    )
    fun observeDoneItemSummaries(startEpochDays: Int, endEpochDays: Int): Flow<List<DoneItemSummaryEntity>>

    @Query(
        "SELECT * FROM journal_entries WHERE dateEpochDays BETWEEN :startEpochDays AND :endEpochDays ORDER BY createdTimeMinutes ASC"
    )
    fun observeJournalEntriesInRange(startEpochDays: Int, endEpochDays: Int): Flow<List<JournalEntryEntity>>

    @RawQuery(observedEntities = [JournalEntryEntity::class, JournalEntryTagEntity::class])
    fun observeJournalEntriesFiltered(query: RoomRawQuery): Flow<List<JournalEntryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM journal_entries WHERE dateEpochDays < :epochDays)")
    fun observeJournalEntryExistsBefore(epochDays: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM period_reviews WHERE periodType = 'Day' AND periodStartEpochDays < :epochDays)")
    fun observeDayReviewExistsBefore(epochDays: Int): Flow<Boolean>


    @Query(
        "SELECT * FROM period_reviews WHERE periodType = :periodType AND periodStartEpochDays = :periodStartEpochDays LIMIT 1"
    )
    suspend fun periodReviewFor(periodType: String, periodStartEpochDays: Int): PeriodReviewEntity?

    @Query(
        "SELECT * FROM period_reviews WHERE periodType = :periodType AND periodStartEpochDays >= :startEpochDays AND periodStartEpochDays < :endEpochDays ORDER BY periodStartEpochDays ASC"
    )
    fun observePeriodReviewsInRange(
        periodType: String,
        startEpochDays: Int,
        endEpochDays: Int
    ): Flow<List<PeriodReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeriodReview(review: PeriodReviewEntity)

    /**
     * Applies a complete evening review atomically: marks done items, carries
     * leftovers onto the next day, writes tomorrow's goal note, and records the
     * review. Idempotent: already-carried items are skipped and every resolved
     * item is stamped as handled.
     */
    @Transaction
    suspend fun completeDayClose(
        dateEpochDays: Int,
        markDoneItemIds: List<Long>,
        carryItemIds: List<Long>,
        dropItemIds: List<Long>,
        winNote: String?,
        tomorrowGoal: String?,
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
        updateDailyPlanItemsStatus(markDoneItemIds, DailyPlanItemStatus.Done.name, nowMillis)
        markDailyPlanItemsHandled(markDoneItemIds, nowMillis)
        markDailyPlanItemsHandled(dropItemIds, nowMillis)

        var carriedCount = 0
        var skippedCount = 0
        carryItemIds.forEach { itemId ->
            val source = dailyPlanItemById(itemId) ?: return@forEach
            val alreadyCarried = carriedFromCountOnDate(targetDateEpochDays, source.id) > 0
            if (!alreadyCarried) {
                val newItemId = insertDailyPlanItem(
                    DailyPlanItemEntity(
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
                        carriedFromItemId = source.id
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

        upsertPeriodReview(
            PeriodReviewEntity(
                periodType = Period.Day.name,
                periodStartEpochDays = dateEpochDays,
                periodEndEpochDays = dateEpochDays + 1,
                content = winNote?.trim().orEmpty(),
                source = ReviewSource.Manual.name,
                status = ReviewStatus.Complete.name,
                completedAtMillis = nowMillis,
                editedAtMillis = nowMillis
            )
        )

        // The tomorrow goal describes the next day's period, so it is stored
        // as the next day's period intent instead of on this record.
        tomorrowGoal?.trim()?.takeIf { it.isNotEmpty() }?.let { goal ->
            val nextStartEpochDays = dateEpochDays + 1
            val existing = periodReviewFor(Period.Day.name, nextStartEpochDays)
            upsertPeriodReview(
                (existing ?: PeriodReviewEntity(
                    periodType = Period.Day.name,
                    periodStartEpochDays = nextStartEpochDays,
                    periodEndEpochDays = nextStartEpochDays + 1
                )).copy(periodIntent = goal, editedAtMillis = nowMillis)
            )
        }

        return DayCloseCommitResult(
            carriedCount = carriedCount,
            skippedCount = skippedCount
        )
    }

    @Transaction
    suspend fun updateDailyPlanItemWithTags(
        itemId: Long,
        title: String,
        note: String?,
        source: String,
        status: String,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        completedAtMillis: Long?,
        label: String?,
        nestedListItemId: Long?,
        tagIds: List<Long>,
        nowMillis: Long
    ) {
        val oldEntity = dailyPlanItemById(itemId) ?: return
        
        updateDailyPlanItem(
            itemId, title, note, source, status, 
            startTimeMinutes, endTimeMinutes, completedAtMillis, label, nestedListItemId
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
        itemId: Long,
        status: String,
        completedAtMillis: Long?,
        nowMillis: Long
    ) {
        val oldEntity = dailyPlanItemById(itemId) ?: return
        if (oldEntity.status == status) return

        updateDailyPlanItemStatus(itemId, status, completedAtMillis)

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
        SET title = :title,
            note = :note,
            source = :source,
            status = :status,
            startTimeMinutes = :startTimeMinutes,
            endTimeMinutes = :endTimeMinutes,
            completedAtMillis = :completedAtMillis,
            label = :label,
            nestedListItemId = :nestedListItemId
        WHERE id = :itemId
        """
    )
    suspend fun updateDailyPlanItem(
        itemId: Long,
        title: String,
        note: String?,
        source: String,
        status: String,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        completedAtMillis: Long?,
        label: String?,
        nestedListItemId: Long?
    )

    @Query("DELETE FROM daily_plan_items WHERE id = :itemId")
    suspend fun deleteDailyPlanItem(itemId: Long)

    @Query(
        """
        UPDATE daily_plan_items
        SET taskId = :taskId,
            source = :source
        WHERE id = :itemId
        """
    )
    suspend fun linkDailyPlanItemToTask(itemId: Long, taskId: Long, source: String)

    @Query(
        """
        UPDATE tasks
        SET startTimeMinutes = NULL,
            endTimeMinutes = NULL,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :taskId
        """
    )
    suspend fun clearTaskTime(taskId: Long, updatedAtMillis: Long)

    @Query("UPDATE notes SET title = :title, content = :content, status = :status, dateEpochDays = :dateEpochDays, startTimeMinutes = :startTimeMinutes, label = :label, editedAtMillis = :editedAtMillis WHERE id = :noteId")
    suspend fun updateNote(
        noteId: Long,
        title: String,
        content: String,
        status: String,
        dateEpochDays: Int?,
        startTimeMinutes: Int?,
        label: String?,
        editedAtMillis: Long
    )

    @Query("UPDATE notes SET status = :status, editedAtMillis = :editedAtMillis WHERE id = :noteId")
    suspend fun updateNoteStatus(noteId: Long, status: String, editedAtMillis: Long)

    @Query("UPDATE notes SET trashedAtMillis = :trashedAtMillis, editedAtMillis = :trashedAtMillis WHERE id = :noteId")
    suspend fun trashNote(noteId: Long, trashedAtMillis: Long)

    @Query("UPDATE notes SET trashedAtMillis = NULL, editedAtMillis = :editedAtMillis WHERE id = :noteId")
    suspend fun restoreNote(noteId: Long, editedAtMillis: Long)

    // ---------------- Nested Documents ----------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNestedDocument(document: NestedDocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNestedListItem(item: NestedListItemEntity): Long

    @Transaction
    suspend fun insertNestedDocumentWithRoot(
        document: NestedDocumentEntity,
        rootItem: NestedListItemEntity
    ): Long {
        val documentId = insertNestedDocument(document)
        insertNestedListItem(rootItem.copy(documentId = documentId))
        return documentId
    }

    @Query("SELECT * FROM nested_documents ORDER BY updatedAtMillis DESC, id ASC")
    fun observeNestedDocuments(): Flow<List<NestedDocumentEntity>>

    @Query("SELECT * FROM nested_list_items WHERE documentId = :documentId ORDER BY position ASC, id ASC")
    fun observeNestedItems(documentId: Long): Flow<List<NestedListItemEntity>>

    @Query("SELECT * FROM nested_item_tags WHERE itemId IN (SELECT id FROM nested_list_items WHERE documentId = :documentId)")
    fun observeNestedItemTags(documentId: Long): Flow<List<NestedItemTagEntity>>

    @Query("SELECT * FROM nested_manual_metrics WHERE itemId IN (SELECT id FROM nested_list_items WHERE documentId = :documentId) ORDER BY itemId ASC, sortOrder ASC, id ASC")
    fun observeNestedManualMetrics(documentId: Long): Flow<List<NestedManualMetricEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM nested_list_items WHERE documentId = :documentId AND parentId IS :parentId")
    suspend fun nextNestedItemPosition(documentId: Long, parentId: Long?): Int

    @Query("UPDATE nested_documents SET title = :title, updatedAtMillis = :updatedAtMillis WHERE id = :documentId")
    suspend fun updateNestedDocumentTitle(documentId: Long, title: String, updatedAtMillis: Long)

    @Query("DELETE FROM nested_documents WHERE id = :documentId")
    suspend fun deleteNestedDocument(documentId: Long)

    @Query("UPDATE nested_list_items SET text = :text, updatedAtMillis = :updatedAtMillis WHERE id = :itemId")
    suspend fun updateNestedItemText(itemId: Long, text: String, updatedAtMillis: Long)

    @Query("UPDATE nested_list_items SET note = :note, updatedAtMillis = :updatedAtMillis WHERE id = :itemId")
    suspend fun updateNestedItemNote(itemId: Long, note: String?, updatedAtMillis: Long)

    @Query("UPDATE nested_list_items SET textStyle = :textStyle, textColor = :textColor, backgroundColor = :backgroundColor, updatedAtMillis = :updatedAtMillis WHERE id = :itemId")
    suspend fun updateNestedItemFormatting(
        itemId: Long,
        textStyle: String,
        textColor: String,
        backgroundColor: String,
        updatedAtMillis: Long
    )

    @Query("UPDATE nested_list_items SET priority = :priority, updatedAtMillis = :updatedAtMillis WHERE id = :itemId")
    suspend fun updateNestedItemPriority(
        itemId: Long,
        priority: String,
        updatedAtMillis: Long
    )

    @Query("UPDATE nested_list_items SET startDateEpochDays = :startDateEpochDays, endDateEpochDays = :endDateEpochDays, updatedAtMillis = :updatedAtMillis WHERE id = :itemId")
    suspend fun updateNestedItemDateRange(
        itemId: Long,
        startDateEpochDays: Int?,
        endDateEpochDays: Int?,
        updatedAtMillis: Long
    )

    @Query("UPDATE nested_list_items SET actualMinutes = :actualMinutes, updatedAtMillis = :updatedAtMillis WHERE id = :itemId")
    suspend fun updateNestedItemActualMinutes(itemId: Long, actualMinutes: Int, updatedAtMillis: Long)

    @Query("UPDATE nested_list_items SET metricRollupPolicy = :policy, showTrackedMinutes = :showTrackedMinutes, updatedAtMillis = :updatedAtMillis WHERE id = :itemId")
    suspend fun updateNestedItemMetricSettings(
        itemId: Long,
        policy: String,
        showTrackedMinutes: Boolean,
        updatedAtMillis: Long
    )

    @Query("UPDATE nested_list_items SET checkboxEnabled = :checkboxEnabled WHERE id = :itemId")
    suspend fun setNestedItemCheckboxEnabled(itemId: Long, checkboxEnabled: Boolean)

    @Query("UPDATE nested_list_items SET checked = :checked WHERE id IN (:itemIds)")
    suspend fun setNestedItemsChecked(itemIds: List<Long>, checked: Boolean)

    @Query("UPDATE nested_list_items SET collapsed = :collapsed WHERE id = :itemId")
    suspend fun setNestedItemCollapsed(itemId: Long, collapsed: Boolean)

    @Query("UPDATE nested_list_items SET collapsed = NOT collapsed WHERE id = :itemId")
    suspend fun toggleNestedItemCollapsed(itemId: Long)

    @Query("UPDATE nested_list_items SET parentId = :parentId, position = :position, updatedAtMillis = :updatedAtMillis WHERE id = :itemId")
    suspend fun updateNestedItemPosition(
        itemId: Long,
        parentId: Long?,
        position: Int,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM nested_list_items WHERE id IN (:itemIds)")
    suspend fun deleteNestedItems(itemIds: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNestedItemTag(link: NestedItemTagEntity)

    @Query("DELETE FROM nested_item_tags WHERE itemId = :itemId")
    suspend fun deleteNestedItemTags(itemId: Long)

    @Transaction
    suspend fun replaceNestedItemTags(itemId: Long, tagIds: List<Long>) {
        deleteNestedItemTags(itemId)
        tagIds.distinct().forEach { tagId -> insertNestedItemTag(NestedItemTagEntity(itemId, tagId)) }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNestedManualMetric(metric: NestedManualMetricEntity): Long

    @Query("DELETE FROM nested_manual_metrics WHERE id = :metricId")
    suspend fun deleteNestedManualMetric(metricId: Long)

    @Query("DELETE FROM nested_manual_metrics WHERE itemId = :itemId")
    suspend fun deleteNestedManualMetrics(itemId: Long)

    @Transaction
    suspend fun replaceNestedManualMetrics(itemId: Long, metrics: List<NestedManualMetricEntity>) {
        deleteNestedManualMetrics(itemId)
        metrics.forEach { insertNestedManualMetric(it.copy(id = 0L, itemId = itemId)) }
    }

    @Query("UPDATE nested_list_items SET actualMinutes = actualMinutes + :delta, updatedAtMillis = :updatedAtMillis WHERE id = :itemId")
    suspend fun updateNestedItemActualMinutesDelta(itemId: Long, delta: Int, updatedAtMillis: Long)

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
}

data class NestedMoveRow(
    val itemId: Long,
    val parentId: Long?,
    val position: Int,
    val updatedAtMillis: Long
)

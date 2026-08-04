package com.checkit.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayReviewCommitResult
import com.checkit.domain.TaskReminderWriteInput
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckItDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjective(objective: ObjectiveEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyResult(keyResult: KeyResultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPlanItem(item: DailyPlanItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: TaskReminderEntity): Long

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

    @Query("SELECT * FROM journal_entries ORDER BY createdAtMillis ASC")
    fun observeJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE dateEpochDays = :dateEpochDays ORDER BY createdAtMillis ASC")
    fun observeJournalEntriesForDate(dateEpochDays: Int): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entry_tags")
    fun observeJournalEntryTags(): Flow<List<JournalEntryTagEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :entryId LIMIT 1")
    suspend fun journalEntryById(entryId: Long): JournalEntryEntity?

    @Query(
        """
        UPDATE journal_entries
        SET context = :context,
            content = :content,
            moods = :moods
        WHERE id = :entryId
        """
    )
    suspend fun updateJournalEntry(entryId: Long, context: String?, content: String, moods: String)

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

    @Query("SELECT * FROM goals ORDER BY sortOrder ASC, title ASC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM objectives ORDER BY sortOrder ASC, title ASC")
    fun observeObjectives(): Flow<List<ObjectiveEntity>>

    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, lastUsedAtMillis DESC, name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM task_filters ORDER BY sortOrder ASC, name ASC")
    fun observeFilters(): Flow<List<TaskFilterEntity>>

    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, createdAtMillis DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM key_results ORDER BY sortOrder ASC, id ASC")
    fun observeKeyResults(): Flow<List<KeyResultEntity>>

    @Query("SELECT * FROM key_results WHERE id = :keyResultId LIMIT 1")
    suspend fun keyResultById(keyResultId: Long): KeyResultEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun taskById(taskId: Long): TaskEntity?

    @Query("SELECT * FROM notes ORDER BY sortOrder ASC, editedAtMillis DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM daily_plan_items ORDER BY sortOrder ASC, addedAtMillis ASC")
    fun observeDailyPlanItems(): Flow<List<DailyPlanItemEntity>>

    @Query("SELECT * FROM daily_plan_items WHERE id = :itemId LIMIT 1")
    suspend fun dailyPlanItemById(itemId: Long): DailyPlanItemEntity?

    @Query("SELECT * FROM daily_plan_items WHERE dateEpochDays = :dateEpochDays ORDER BY sortOrder ASC, addedAtMillis ASC")
    suspend fun dailyPlanItemsForDate(dateEpochDays: Int): List<DailyPlanItemEntity>

    @Query("SELECT COUNT(*) FROM daily_plan_items WHERE dateEpochDays = :dateEpochDays AND carriedFromItemId = :sourceItemId")
    suspend fun carriedFromCountOnDate(dateEpochDays: Int, sourceItemId: Long): Int

    @Query("SELECT * FROM sub_tasks ORDER BY sortOrder ASC, id ASC")
    fun observeSubTasks(): Flow<List<SubTaskEntity>>

    @Query("SELECT * FROM task_reminders ORDER BY remindAtMillis ASC")
    fun observeReminders(): Flow<List<TaskReminderEntity>>

    @Query("SELECT * FROM task_tags")
    fun observeTaskTags(): Flow<List<TaskTagEntity>>

    @Query("SELECT * FROM note_tags")
    fun observeNoteTags(): Flow<List<NoteTagEntity>>

    @Query("SELECT * FROM daily_plan_item_tags")
    fun observeDailyPlanItemTags(): Flow<List<DailyPlanItemTagEntity>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tasks WHERE objectiveId = :objectiveId")
    suspend fun nextTaskSortOrder(objectiveId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM notes WHERE objectiveId = :objectiveId")
    suspend fun nextNoteSortOrder(objectiveId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM daily_plan_items WHERE dateEpochDays = :dateEpochDays")
    suspend fun nextDailyPlanItemSortOrder(dateEpochDays: Int): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM goals")
    suspend fun nextGoalSortOrder(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM objectives")
    suspend fun nextObjectiveSortOrder(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM key_results WHERE objectiveId = :objectiveId")
    suspend fun nextKeyResultSortOrder(objectiveId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tags")
    suspend fun nextTagSortOrder(): Int

    @Query("UPDATE tags SET sortOrder = :sortOrder WHERE id = :tagId")
    suspend fun updateTagSortOrder(tagId: Long, sortOrder: Int)

    @Query("UPDATE tags SET lastUsedAtMillis = :lastUsedAtMillis WHERE id = :tagId")
    suspend fun updateTagLastUsedAtMillis(tagId: Long, lastUsedAtMillis: Long)

    @Query("UPDATE key_results SET currentValue = currentValue + :delta WHERE id = :keyResultId")
    suspend fun adjustKeyResultValue(keyResultId: Long, delta: Double)

    @Query("SELECT COUNT(*) FROM daily_plan_items WHERE taskId = :taskId AND dateEpochDays = :dateEpochDays AND status = 'Done' AND id != :excludeItemId")
    suspend fun countDoneDailyPlanItemsForTaskOnDate(taskId: Long, dateEpochDays: Int, excludeItemId: Long): Int

    @Query("SELECT tagId FROM daily_plan_item_tags WHERE itemId = :itemId")
    suspend fun tagIdsForItem(itemId: Long): List<Long>

    @Query("SELECT tagId FROM task_tags WHERE taskId = :taskId")
    suspend fun tagIdsForTask(taskId: Long): List<Long>

    @Query("SELECT * FROM tags WHERE id IN (:tagIds)")
    suspend fun tagsByIds(tagIds: List<Long>): List<TagEntity>

    @Query("SELECT * FROM sub_tasks WHERE taskId = :taskId ORDER BY sortOrder ASC")
    suspend fun subTasksForTask(taskId: Long): List<SubTaskEntity>

    @Query("SELECT * FROM task_reminders WHERE taskId = :taskId ORDER BY remindAtMillis ASC")
    suspend fun remindersForTask(taskId: Long): List<TaskReminderEntity>

    @Query("SELECT * FROM objectives WHERE id = :id LIMIT 1")
    suspend fun objectiveById(id: Long): ObjectiveEntity?

    @Query("SELECT kr.* FROM key_results kr JOIN tasks t ON kr.id = t.keyResultId WHERE t.id = :taskId LIMIT 1")
    suspend fun keyResultByTaskId(taskId: Long): KeyResultEntity?

    @Query("SELECT id FROM objectives WHERE title = 'Inbox' ORDER BY sortOrder ASC, id ASC LIMIT 1")
    suspend fun inboxObjectiveId(): Long?

    @Query(
        """
        UPDATE objectives
        SET title = :name,
            goalId = :goalId,
            startDateEpochDays = :startDateEpochDays,
            endDateEpochDays = :endDateEpochDays,
            color = :color,
            icon = :icon
        WHERE id = :objectiveId
        """
    )
    suspend fun updateObjective(
        objectiveId: Long,
        name: String,
        goalId: Long?,
        startDateEpochDays: Int?,
        endDateEpochDays: Int?,
        color: String,
        icon: String
    )

    @Query("UPDATE goals SET title = :title, color = :color, icon = :icon WHERE id = :goalId")
    suspend fun updateGoal(goalId: Long, title: String, color: String, icon: String)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: Long)

    @Query(
        """
        UPDATE key_results
        SET objectiveId = :objectiveId,
            title = :title,
            targetValue = :targetValue,
            currentValue = :currentValue,
            unit = :unit
        WHERE id = :keyResultId
        """
    )
    suspend fun updateKeyResult(
        keyResultId: Long,
        objectiveId: Long,
        title: String,
        targetValue: Double,
        currentValue: Double,
        unit: String
    )

    @Query("DELETE FROM key_results WHERE id = :keyResultId")
    suspend fun deleteKeyResult(keyResultId: Long)

    @Query("UPDATE tasks SET objectiveId = :toObjectiveId, keyResultId = NULL, updatedAtMillis = :updatedAtMillis WHERE objectiveId = :fromObjectiveId")
    suspend fun moveTasksToObjective(fromObjectiveId: Long, toObjectiveId: Long, updatedAtMillis: Long)

    @Query("UPDATE notes SET objectiveId = :toObjectiveId, editedAtMillis = :editedAtMillis WHERE objectiveId = :fromObjectiveId")
    suspend fun moveNotesToObjective(fromObjectiveId: Long, toObjectiveId: Long, editedAtMillis: Long)

    @Query("DELETE FROM objectives WHERE id = :objectiveId")
    suspend fun deleteObjective(objectiveId: Long)

    @Transaction
    suspend fun deleteObjectiveMovingContents(objectiveId: Long, targetObjectiveId: Long, timestampMillis: Long) {
        moveTasksToObjective(fromObjectiveId = objectiveId, toObjectiveId = targetObjectiveId, updatedAtMillis = timestampMillis)
        moveNotesToObjective(fromObjectiveId = objectiveId, toObjectiveId = targetObjectiveId, editedAtMillis = timestampMillis)
        deleteObjective(objectiveId)
    }

    @Query("UPDATE tags SET name = :name, color = :color WHERE id = :tagId")
    suspend fun updateTag(tagId: Long, name: String, color: String)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)

    @Query("SELECT COUNT(*) FROM tags WHERE name = :name AND id != :excludeId")
    suspend fun tagNameInUseExcept(name: String, excludeId: Long): Int

    @Query(
        """
        UPDATE tasks
        SET objectiveId = :objectiveId,
            keyResultId = :keyResultId,
            name = :name,
            description = :description,
            status = :status,
            priority = :priority,
            type = :type,
            doDateEpochDays = :doDateEpochDays,
            startTimeMinutes = :startTimeMinutes,
            endTimeMinutes = :endTimeMinutes,
            repeatRRule = :repeatRRule,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :taskId
        """
    )
    suspend fun updateTask(
        taskId: Long,
        objectiveId: Long,
        keyResultId: Long?,
        name: String,
        description: String,
        status: String,
        priority: String,
        type: String,
        doDateEpochDays: Int?,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        repeatRRule: String?,
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
            completedDateEpochDays = NULL,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :taskId
        """
    )
    suspend fun updateTaskStatusOpen(
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

    @Query("SELECT * FROM day_reviews ORDER BY dateEpochDays ASC")
    fun observeDayReviews(): Flow<List<DayReviewEntity>>

    @Query("SELECT * FROM day_reviews WHERE dateEpochDays = :dateEpochDays LIMIT 1")
    suspend fun dayReviewForDate(dateEpochDays: Int): DayReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDayReview(review: DayReviewEntity)

    /**
     * Applies a complete evening review atomically: marks done items, carries
     * leftovers onto the next day, writes tomorrow's goal note, and records the
     * review. Idempotent: already-carried items are skipped and every resolved
     * item is stamped as handled.
     */
    @Transaction
    suspend fun completeDayReview(
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
    ): DayReviewCommitResult {
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
                        title = source.title,
                        note = source.note,
                        source = source.source,
                        status = DailyPlanItemStatus.Planned.name,
                        sortOrder = nextDailyPlanItemSortOrder(targetDateEpochDays),
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

        upsertDayReview(
            DayReviewEntity(
                dateEpochDays = dateEpochDays,
                doneCount = doneCount,
                plannedCount = plannedCount,
                doneMinutes = doneMinutes,
                winNote = winNote?.trim()?.takeIf { it.isNotEmpty() },
                tomorrowGoal = tomorrowGoal?.trim()?.takeIf { it.isNotEmpty() },
                completedAtMillis = nowMillis
            )
        )

        return DayReviewCommitResult(
            carriedCount = carriedCount,
            skippedCount = skippedCount
        )
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
            completedAtMillis = :completedAtMillis
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
        completedAtMillis: Long?
    )

    @Query("DELETE FROM daily_plan_items WHERE id = :itemId")
    suspend fun deleteDailyPlanItem(itemId: Long)

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

    @Query("UPDATE notes SET objectiveId = :objectiveId, title = :title, content = :content, status = :status, dateEpochDays = :dateEpochDays, startTimeMinutes = :startTimeMinutes, editedAtMillis = :editedAtMillis WHERE id = :noteId")
    suspend fun updateNote(
        noteId: Long,
        objectiveId: Long,
        title: String,
        content: String,
        status: String,
        dateEpochDays: Int?,
        startTimeMinutes: Int?,
        editedAtMillis: Long
    )

    @Query("UPDATE notes SET status = :status, editedAtMillis = :editedAtMillis WHERE id = :noteId")
    suspend fun updateNoteStatus(noteId: Long, status: String, editedAtMillis: Long)

    @Query("UPDATE notes SET trashedAtMillis = :trashedAtMillis, editedAtMillis = :trashedAtMillis WHERE id = :noteId")
    suspend fun trashNote(noteId: Long, trashedAtMillis: Long)

    @Query("UPDATE notes SET trashedAtMillis = NULL, editedAtMillis = :editedAtMillis WHERE id = :noteId")
    suspend fun restoreNote(noteId: Long, editedAtMillis: Long)
}

package com.checkit.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DayCloseCommitResult
import com.checkit.domain.ReviewPeriod
import com.checkit.domain.ReviewSource
import com.checkit.domain.ReviewStatus
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ListEntity): Long

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

    @Query("SELECT * FROM lists ORDER BY sortOrder ASC, title ASC")
    fun observeLists(): Flow<List<ListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskKeyResult(taskKeyResult: TaskKeyResultEntity)

    @Query("DELETE FROM task_key_result WHERE taskId = :taskId")
    suspend fun deleteTaskKeyResult(taskId: Long)

    @Query("SELECT * FROM task_key_result")
    fun observeTaskKeyResults(): Flow<List<TaskKeyResultEntity>>

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
        SET context = :context,
            content = :content,
            moods = :moods,
            attachments = :attachments
        WHERE id = :entryId
        """
    )
    suspend fun updateJournalEntry(entryId: Long, context: String?, content: String, moods: String, attachments: String)

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

    @Query("SELECT COALESCE(MAX(t.sortOrder), -1) + 1 FROM tasks t JOIN task_list tl ON t.id = tl.taskId WHERE tl.listId = :listId")
    suspend fun nextTaskSortOrder(listId: Long): Int

    @Query("SELECT COALESCE(MAX(n.sortOrder), -1) + 1 FROM notes n JOIN note_list nl ON n.id = nl.noteId WHERE nl.listId = :listId")
    suspend fun nextNoteSortOrder(listId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM daily_plan_items WHERE dateEpochDays = :dateEpochDays")
    suspend fun nextDailyPlanItemSortOrder(dateEpochDays: Int): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM goals")
    suspend fun nextGoalSortOrder(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM lists")
    suspend fun nextListSortOrder(): Int

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

    @Query("SELECT kr.* FROM key_results kr JOIN task_key_result tkr ON kr.id = tkr.keyResultId WHERE tkr.taskId = :taskId LIMIT 1")
    suspend fun keyResultByTaskId(taskId: Long): KeyResultEntity?

    @Query("SELECT id FROM lists WHERE title = 'Inbox' ORDER BY sortOrder ASC, id ASC LIMIT 1")
    suspend fun inboxListId(): Long?

    @Query("UPDATE lists SET title = :title, icon = :icon, color = :color WHERE id = :listId")
    suspend fun updateList(listId: Long, title: String, icon: String, color: String)

    @Query("DELETE FROM lists WHERE id = :listId")
    suspend fun deleteList(listId: Long)

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

    @Query("UPDATE goals SET title = :title, color = :color, icon = :icon WHERE id = :goalId")
    suspend fun updateGoal(goalId: Long, title: String, color: String, icon: String)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoal(goalId: Long)

    @Query(
        """
        UPDATE objectives
        SET title = :title,
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
        title: String,
        goalId: Long,
        startDateEpochDays: Int?,
        endDateEpochDays: Int?,
        color: String,
        icon: String
    )

    @Query("DELETE FROM objectives WHERE id = :objectiveId")
    suspend fun deleteObjective(objectiveId: Long)

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

    @Query("SELECT * FROM period_reviews ORDER BY periodStartEpochDays ASC")
    fun observePeriodReviews(): Flow<List<PeriodReviewEntity>>

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

        upsertPeriodReview(
            PeriodReviewEntity(
                periodType = ReviewPeriod.Day.name,
                periodStartEpochDays = dateEpochDays,
                periodEndEpochDays = dateEpochDays + 1,
                content = winNote?.trim().orEmpty(),
                intentNext = tomorrowGoal?.trim()?.takeIf { it.isNotEmpty() },
                source = ReviewSource.Manual.name,
                status = ReviewStatus.Complete.name,
                completedAtMillis = nowMillis,
                editedAtMillis = nowMillis
            )
        )

        return DayCloseCommitResult(
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

    @Query("UPDATE notes SET title = :title, content = :content, status = :status, dateEpochDays = :dateEpochDays, startTimeMinutes = :startTimeMinutes, editedAtMillis = :editedAtMillis WHERE id = :noteId")
    suspend fun updateNote(
        noteId: Long,
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

    // ---------------- Period Plan ----------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriodPlan(plan: PeriodPlanEntity): Long

    @Query("SELECT * FROM period_plans ORDER BY startEpochDays ASC, id ASC")
    fun observePeriodPlans(): Flow<List<PeriodPlanEntity>>

    @Query(
        "SELECT * FROM period_plans WHERE periodType = :periodType AND startEpochDays = :startEpochDays LIMIT 1"
    )
    suspend fun periodPlanFor(periodType: String, startEpochDays: Int): PeriodPlanEntity?

    @Transaction
    suspend fun getOrCreatePeriodPlan(
        periodType: String,
        startEpochDays: Int,
        endEpochDays: Int
    ): Long {
        val existing = periodPlanFor(periodType, startEpochDays)
        if (existing != null) return existing.id
        return insertPeriodPlan(
            PeriodPlanEntity(
                periodType = periodType,
                startEpochDays = startEpochDays,
                endEpochDays = endEpochDays
            )
        )
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanPriority(priority: PlanPriorityEntity): Long

    @Query(
        """
        UPDATE plan_priorities
        SET title = :title,
            note = :note,
            parentId = :parentId,
            sortOrder = :sortOrder,
            isDone = :isDone,
            completedAtMillis = :completedAtMillis,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :priorityId
        """
    )
    suspend fun updatePlanPriority(
        priorityId: Long,
        title: String,
        note: String,
        parentId: Long?,
        sortOrder: Int,
        isDone: Boolean,
        completedAtMillis: Long?,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM plan_priorities WHERE id = :priorityId")
    suspend fun deletePlanPriority(priorityId: Long)

    @Transaction
    suspend fun deletePlanPriorityWithJoins(priorityId: Long) {
        deletePlanPriorityTasksForPriority(priorityId)
        deletePlanPriorityDailyPlanItemsForPriority(priorityId)
        deletePlanPriority(priorityId)
    }

    @Query("SELECT * FROM plan_priorities ORDER BY sortOrder ASC, id ASC")
    fun observePlanPriorities(): Flow<List<PlanPriorityEntity>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM plan_priorities WHERE periodPlanId = :periodPlanId")
    suspend fun nextPlanPrioritySortOrder(periodPlanId: Long): Int

    @Query("UPDATE plan_priorities SET sortOrder = :sortOrder, updatedAtMillis = :updatedAtMillis WHERE id = :priorityId")
    suspend fun updatePlanPrioritySortOrder(priorityId: Long, sortOrder: Int, updatedAtMillis: Long)

    @Query(
        """
        UPDATE plan_priorities
        SET isDone = :isDone,
            completedAtMillis = :completedAtMillis,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :priorityId
        """
    )
    suspend fun setPlanPriorityDone(
        priorityId: Long,
        isDone: Boolean,
        completedAtMillis: Long?,
        updatedAtMillis: Long
    )

    @Query(
        "UPDATE plan_priorities SET parentId = :parentId, updatedAtMillis = :updatedAtMillis WHERE id = :priorityId"
    )
    suspend fun setPlanPriorityParent(priorityId: Long, parentId: Long?, updatedAtMillis: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanPriorityTask(link: PlanPriorityTaskEntity)

    @Query("DELETE FROM plan_priority_tasks WHERE priorityId = :priorityId AND taskId = :taskId")
    suspend fun deletePlanPriorityTask(priorityId: Long, taskId: Long)

    @Query("DELETE FROM plan_priority_tasks WHERE priorityId = :priorityId")
    suspend fun deletePlanPriorityTasksForPriority(priorityId: Long)

    @Query("DELETE FROM plan_priority_tasks WHERE taskId = :taskId")
    suspend fun deletePlanPriorityTasksForTask(taskId: Long)

    @Query("SELECT * FROM plan_priority_tasks")
    fun observePlanPriorityTasks(): Flow<List<PlanPriorityTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanPriorityDailyPlanItem(link: PlanPriorityDailyPlanItemEntity)

    @Query(
        "DELETE FROM plan_priority_daily_plan_items WHERE priorityId = :priorityId AND dailyPlanItemId = :dailyPlanItemId"
    )
    suspend fun deletePlanPriorityDailyPlanItem(priorityId: Long, dailyPlanItemId: Long)

    @Query("DELETE FROM plan_priority_daily_plan_items WHERE priorityId = :priorityId")
    suspend fun deletePlanPriorityDailyPlanItemsForPriority(priorityId: Long)

    @Query("SELECT * FROM plan_priority_daily_plan_items")
    fun observePlanPriorityDailyPlanItems(): Flow<List<PlanPriorityDailyPlanItemEntity>>
}

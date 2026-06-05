package com.checkit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.checkit.domain.TaskReminderWriteInput
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckItDao {
    @Query("SELECT COUNT(*) FROM task_lists")
    suspend fun listCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: TaskListEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPlan(plan: DailyPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPlanItem(item: DailyPlanItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(filter: TaskFilterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: TaskReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskTag(taskTag: TaskTagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTag(noteTag: NoteTagEntity)

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

    @Query("DELETE FROM task_tags WHERE taskId = :taskId")
    suspend fun deleteTaskTags(taskId: Long)

    @Query("DELETE FROM sub_tasks WHERE taskId = :taskId")
    suspend fun deleteSubTasks(taskId: Long)

    @Query("DELETE FROM task_reminders WHERE taskId = :taskId")
    suspend fun deleteTaskReminders(taskId: Long)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteNoteTags(noteId: Long)

    @Query(
        """
        DELETE FROM tasks
        WHERE name = 'Plan the day'
          AND description = 'Review agenda, timeline, and the next task to start.'
          AND id NOT IN (
              SELECT MIN(id)
              FROM tasks
              WHERE name = 'Plan the day'
                AND description = 'Review agenda, timeline, and the next task to start.'
          )
        """
    )
    suspend fun deleteDuplicateSeedTasks()

    @Query(
        """
        DELETE FROM notes
        WHERE content = 'Ideas, meeting notes, and loose thoughts live beside tasks in each list.'
          AND id NOT IN (
              SELECT MIN(id)
              FROM notes
              WHERE content = 'Ideas, meeting notes, and loose thoughts live beside tasks in each list.'
          )
        """
    )
    suspend fun deleteDuplicateSeedNotes()

    @Query(
        """
        DELETE FROM task_filters
        WHERE name IN ('All', 'Today', 'Completed', 'High priority', 'Trashed')
          AND id NOT IN (
              SELECT MIN(id)
              FROM task_filters
              WHERE name IN ('All', 'Today', 'Completed', 'High priority', 'Trashed')
              GROUP BY name
          )
        """
    )
    suspend fun deleteDuplicateSeedFilters()

    @Query(
        """
        DELETE FROM tags
        WHERE name IN ('Work', 'Home')
          AND id NOT IN (
              SELECT MIN(id)
              FROM tags
              WHERE name IN ('Work', 'Home')
              GROUP BY name
          )
        """
    )
    suspend fun deleteDuplicateSeedTags()

    @Query(
        """
        DELETE FROM task_lists
        WHERE name = 'Inbox'
          AND id NOT IN (
              SELECT MIN(id)
              FROM task_lists
              WHERE name = 'Inbox'
          )
          AND id NOT IN (SELECT DISTINCT listId FROM tasks)
          AND id NOT IN (SELECT DISTINCT listId FROM notes)
        """
    )
    suspend fun deleteDuplicateEmptySeedLists()

    @Query("SELECT * FROM task_lists ORDER BY sortOrder ASC, name ASC")
    fun observeLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM task_filters ORDER BY sortOrder ASC, name ASC")
    fun observeFilters(): Flow<List<TaskFilterEntity>>

    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, createdAtMillis DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM notes ORDER BY sortOrder ASC, editedAtMillis DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM daily_plans ORDER BY dateEpochDays DESC")
    fun observeDailyPlans(): Flow<List<DailyPlanEntity>>

    @Query("SELECT * FROM daily_plan_items ORDER BY sortOrder ASC, addedAtMillis ASC")
    fun observeDailyPlanItems(): Flow<List<DailyPlanItemEntity>>

    @Query("SELECT * FROM daily_plans WHERE dateEpochDays = :dateEpochDays LIMIT 1")
    suspend fun dailyPlanForDate(dateEpochDays: Int): DailyPlanEntity?

    @Query("SELECT * FROM sub_tasks ORDER BY sortOrder ASC, id ASC")
    fun observeSubTasks(): Flow<List<SubTaskEntity>>

    @Query("SELECT * FROM task_reminders ORDER BY remindAtMillis ASC")
    fun observeReminders(): Flow<List<TaskReminderEntity>>

    @Query("SELECT * FROM task_tags")
    fun observeTaskTags(): Flow<List<TaskTagEntity>>

    @Query("SELECT * FROM note_tags")
    fun observeNoteTags(): Flow<List<NoteTagEntity>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tasks WHERE listId = :listId")
    suspend fun nextTaskSortOrder(listId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM notes WHERE listId = :listId")
    suspend fun nextNoteSortOrder(listId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM daily_plan_items WHERE dailyPlanId = :dailyPlanId")
    suspend fun nextDailyPlanItemSortOrder(dailyPlanId: Long): Int

    @Query("SELECT COUNT(*) FROM daily_plan_items WHERE dailyPlanId = :dailyPlanId AND taskId = :taskId")
    suspend fun dailyPlanTaskItemCount(dailyPlanId: Long, taskId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM task_lists")
    suspend fun nextListSortOrder(): Int

    @Query("UPDATE task_lists SET name = :name, color = :color, icon = :icon WHERE id = :listId")
    suspend fun updateList(listId: Long, name: String, color: String, icon: String)

    @Query("UPDATE tags SET name = :name, color = :color WHERE id = :tagId")
    suspend fun updateTag(tagId: Long, name: String, color: String)

    @Query("SELECT COUNT(*) FROM tags WHERE name = :name AND id != :excludeId")
    suspend fun tagNameInUseExcept(name: String, excludeId: Long): Int

    @Query(
        """
        UPDATE tasks
        SET name = :name,
            description = :description,
            status = :status,
            priority = :priority,
            dueDateEpochDays = :dueDateEpochDays,
            startTimeMinutes = :startTimeMinutes,
            endTimeMinutes = :endTimeMinutes,
            durationMinutes = :durationMinutes,
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
        dueDateEpochDays: Int?,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?,
        durationMinutes: Int?,
        repeatRRule: String?,
        updatedAtMillis: Long
    )

    @Query("UPDATE tasks SET trashedAtMillis = :trashedAtMillis, updatedAtMillis = :trashedAtMillis WHERE id = :taskId")
    suspend fun trashTask(taskId: Long, trashedAtMillis: Long)

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
        WHERE taskId = :taskId
          AND status != :status
        """
    )
    suspend fun updateDailyPlanItemsForTaskStatus(
        taskId: Long,
        status: String,
        completedAtMillis: Long?
    )

    @Query(
        """
        UPDATE daily_plan_items
        SET plannedStartTimeMinutes = :startTimeMinutes,
            plannedEndTimeMinutes = :endTimeMinutes
        WHERE id = :itemId
        """
    )
    suspend fun updateDailyPlanItemTime(
        itemId: Long,
        startTimeMinutes: Int?,
        endTimeMinutes: Int?
    )

    @Query("UPDATE notes SET content = :content, status = :status, dateEpochDays = :dateEpochDays, editedAtMillis = :editedAtMillis WHERE id = :noteId")
    suspend fun updateNote(noteId: Long, content: String, status: String, dateEpochDays: Int, editedAtMillis: Long)

    @Query("UPDATE notes SET status = :status, editedAtMillis = :editedAtMillis WHERE id = :noteId")
    suspend fun updateNoteStatus(noteId: Long, status: String, editedAtMillis: Long)

    @Query("UPDATE notes SET trashedAtMillis = :trashedAtMillis, editedAtMillis = :trashedAtMillis WHERE id = :noteId")
    suspend fun trashNote(noteId: Long, trashedAtMillis: Long)
}

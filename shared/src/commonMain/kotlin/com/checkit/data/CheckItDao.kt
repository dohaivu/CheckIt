package com.checkit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
    suspend fun insertFilter(filter: TaskFilterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: TaskReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskTag(taskTag: TaskTagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTag(noteTag: NoteTagEntity)

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

    @Query("SELECT * FROM sub_tasks ORDER BY sortOrder ASC, id ASC")
    fun observeSubTasks(): Flow<List<SubTaskEntity>>

    @Query("SELECT * FROM task_reminders ORDER BY remindAtMillis ASC")
    fun observeReminders(): Flow<List<TaskReminderEntity>>

    @Query("SELECT * FROM task_tags")
    fun observeTaskTags(): Flow<List<TaskTagEntity>>

    @Query("SELECT * FROM note_tags")
    fun observeNoteTags(): Flow<List<NoteTagEntity>>
}

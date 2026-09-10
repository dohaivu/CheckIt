package com.checkit.data

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert
import com.checkit.domain.QuickNote
import com.checkit.domain.QuickNoteStatus
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "quick_notes",
    indices = [
        Index("status"),
        Index("updatedAt"),
        Index(value = ["status", "sortOrder"]),
    ]
)
data class QuickNoteEntity(
    @PrimaryKey val id: String,
    val content: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Double,
    val remindAt: Long?,
    val deleteAt: Long?,
    val deleted: Boolean,
)

fun QuickNoteEntity.toDomain(): QuickNote = QuickNote(
    id = id,
    content = content,
    status = runCatching { QuickNoteStatus.valueOf(status) }.getOrDefault(QuickNoteStatus.NEXT),
    createdAt = createdAt,
    updatedAt = updatedAt,
    sortOrder = sortOrder,
    remindAt = remindAt,
    deleteAt = deleteAt,
    deleted = deleted,
)

fun QuickNote.toEntity(): QuickNoteEntity = QuickNoteEntity(
    id = id,
    content = content,
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sortOrder = sortOrder,
    remindAt = remindAt,
    deleteAt = deleteAt,
    deleted = deleted,
)

@Dao
interface QuickNoteDao {
    @Query("SELECT * FROM quick_notes WHERE status = 'NEXT' AND deleted = 0 ORDER BY sortOrder ASC")
    fun observeNext(): Flow<List<QuickNoteEntity>>

    @Query("SELECT * FROM quick_notes WHERE status = 'TO_BE_DELETED' AND deleted = 0 ORDER BY deleteAt ASC")
    fun observeToBeDeleted(): Flow<List<QuickNoteEntity>>

    @Query("SELECT * FROM quick_notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): QuickNoteEntity?

    @Upsert
    suspend fun upsert(note: QuickNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: QuickNoteEntity)

    @Query("UPDATE quick_notes SET status = 'TO_BE_DELETED', deleteAt = :deleteAt, remindAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun moveToBeDeleted(id: String, deleteAt: Long, updatedAt: Long)

    @Query("UPDATE quick_notes SET remindAt = :remindAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setReminder(id: String, remindAt: Long?, updatedAt: Long)

    @Query("UPDATE quick_notes SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Double, updatedAt: Long)

    @Query("UPDATE quick_notes SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: String, updatedAt: Long)

    @Query("UPDATE quick_notes SET status = 'NEXT', deleteAt = NULL, remindAt = NULL, sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restore(id: String, sortOrder: Double, updatedAt: Long)

    @Query("SELECT * FROM quick_notes WHERE status = 'TO_BE_DELETED' AND deleted = 0 AND deleteAt IS NOT NULL AND deleteAt <= :now")
    suspend fun getExpired(now: Long): List<QuickNoteEntity>

    @Query("SELECT * FROM quick_notes WHERE deleted = 0 AND remindAt IS NOT NULL AND remindAt <= :now")
    suspend fun getDueReminders(now: Long): List<QuickNoteEntity>

    @Query("SELECT * FROM quick_notes WHERE deleted = 0 AND remindAt IS NOT NULL")
    suspend fun getScheduledReminders(): List<QuickNoteEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), 0.0) FROM quick_notes WHERE status = 'NEXT' AND deleted = 0")
    suspend fun maxNextSortOrder(): Double

    @Query("SELECT * FROM quick_notes WHERE deleted = 0 ORDER BY updatedAt DESC")
    suspend fun getAllActive(): List<QuickNoteEntity>

    @Query("SELECT * FROM quick_notes ORDER BY updatedAt DESC")
    suspend fun getAllForSync(): List<QuickNoteEntity>
}

package com.checkit.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val icon: String,
    val color: String,
    val sortOrder: Int,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "objectives",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("goalId")]
)
data class ObjectiveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val goalId: Long? = null,
    val startDateEpochDays: Int? = null,
    val endDateEpochDays: Int? = null,
    val color: String? = null,
    val icon: String? = null,
    val sortOrder: Int,
    val isArchived: Boolean = false
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val color: String
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ObjectiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = KeyResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["keyResultId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("listId"),
        Index("keyResultId"),
        Index("status"),
        Index("priority"),
        Index("doDateEpochDays")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val listId: Long,
    val keyResultId: Long? = null,
    val name: String,
    val description: String = "",
    val status: String,
    val priority: String,
    val doDateEpochDays: Int? = null,
    val completedDateEpochDays: Int? = null,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val durationMinutes: Int? = null,
    val repeatRRule: String? = null,
    val sortOrder: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val trashedAtMillis: Long? = null
)

@Entity(
    tableName = "key_results",
    foreignKeys = [
        ForeignKey(
            entity = ObjectiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["objectiveId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("objectiveId")]
)
data class KeyResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val objectiveId: Long,
    val title: String,
    val targetValue: Double,
    val currentValue: Double = 0.0,
    val unit: String,
    val sortOrder: Int
)

@Entity(
    tableName = "sub_tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class SubTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val taskId: Long,
    val name: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = ObjectiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val listId: Long,
    val title: String = "",
    val content: String,
    val status: String = "Open",
    val dateEpochDays: Int,
    val startTimeMinutes: Int? = null,
    val createdAtMillis: Long,
    val editedAtMillis: Long,
    val sortOrder: Int,
    val trashedAtMillis: Long? = null
)

@Entity(
    tableName = "daily_plans",
    indices = [Index(value = ["dateEpochDays"], unique = true)]
)
data class DailyPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateEpochDays: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(
    tableName = "daily_plan_items",
    foreignKeys = [
        ForeignKey(
            entity = DailyPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["dailyPlanId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("dailyPlanId"), Index("taskId"), Index("status")]
)
data class DailyPlanItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dailyPlanId: Long,
    val taskId: Long? = null,
    val title: String,
    val note: String? = null,
    val source: String,
    val status: String,
    val sortOrder: Int,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val addedAtMillis: Long,
    val completedAtMillis: Long? = null
)

@Entity(
    tableName = "task_tags",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("tagId")]
)
data class TaskTagEntity(
    val taskId: Long,
    val tagId: Long
)

@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId"), Index("tagId")]
)
data class NoteTagEntity(
    val noteId: Long,
    val tagId: Long
)

@Entity(
    tableName = "daily_plan_item_tags",
    primaryKeys = ["itemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = DailyPlanItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index("tagId")]
)
data class DailyPlanItemTagEntity(
    val itemId: Long,
    val tagId: Long
)

@Entity(
    tableName = "task_reminders",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("remindAtMillis")]
)
data class TaskReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val taskId: Long,
    val remindAtMillis: Long,
    val label: String = ""
)

@Entity(
    tableName = "task_filters",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("tagId")]
)
data class TaskFilterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val icon: String,
    val color: String,
    val tagId: Long? = null,
    val dueDatePreset: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val includeTrashed: Boolean = false,
    val sortOrder: Int
)

@Database(
    entities = [
        GoalEntity::class,
        ObjectiveEntity::class,
        KeyResultEntity::class,
        TaskEntity::class,
        SubTaskEntity::class,
        NoteEntity::class,
        DailyPlanEntity::class,
        DailyPlanItemEntity::class,
        TagEntity::class,
        TaskTagEntity::class,
        NoteTagEntity::class,
        DailyPlanItemTagEntity::class,
        TaskReminderEntity::class,
        TaskFilterEntity::class
    ],
    version = 2,
    exportSchema = false
)
@ConstructedBy(CheckItDatabaseConstructor::class)
abstract class CheckItDatabase : RoomDatabase() {
    abstract fun checkItDao(): CheckItDao
}

@Suppress("KotlinNoActualForExpect")
expect object CheckItDatabaseConstructor : RoomDatabaseConstructor<CheckItDatabase> {
    override fun initialize(): CheckItDatabase
}

fun buildCheckItDatabase(
    builder: RoomDatabase.Builder<CheckItDatabase>
): CheckItDatabase {
    return builder
        .fallbackToDestructiveMigration(false)
        .fallbackToDestructiveMigrationOnDowngrade(false)
        .addMigrations()
        .setQueryCoroutineContext(Dispatchers.IO)
        .setDriver(BundledSQLiteDriver())
        .build()
}
